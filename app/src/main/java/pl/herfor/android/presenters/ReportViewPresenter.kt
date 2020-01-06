package pl.herfor.android.presenters

import android.annotation.SuppressLint
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.Observer
import com.crashlytics.android.Crashlytics
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLngBounds
import org.koin.core.KoinComponent
import org.koin.core.inject
import pl.herfor.android.R
import pl.herfor.android.interfaces.AppContract
import pl.herfor.android.interfaces.ContextRepository
import pl.herfor.android.modules.*
import pl.herfor.android.objects.Point
import pl.herfor.android.objects.Report
import pl.herfor.android.objects.ReportProperties
import pl.herfor.android.objects.enums.*
import pl.herfor.android.objects.requests.ReportAddRequest
import pl.herfor.android.objects.requests.ReportGradeRequest
import pl.herfor.android.objects.requests.ReportSearchRequest
import pl.herfor.android.objects.viewmodels.ReportViewModel
import pl.herfor.android.retrofits.RetrofitRepository
import pl.herfor.android.utils.Constants
import pl.herfor.android.utils.toLatLng
import pl.herfor.android.utils.toPoint
import kotlin.concurrent.thread

class ReportViewPresenter(
    private val view: AppContract.View,
    private val model: ReportViewModel,
    private val context: ContextRepository
) : AppContract.Presenter, KoinComponent {
    private val repository: RetrofitRepository by inject()
    private val notificationGeofence: NotificationGeofenceModule by inject()
    private val silentZoneGeofence: SilentZoneGeofenceModule by inject()
    private val preferences: PreferencesModule by inject()
    private val liveData: LiveDataModule by inject()
    private val location: LocationModule by inject()
    private val database: DatabaseModule by inject()
    private val notification: NotificationModule by inject()

    override fun initializeObservers() {
        model.filteredReports.observe(
            context.getLifecycleOwner(),
            Observer { newReportList -> handleChangesInReports(newReportList) })
        model.silentZoneToggled.observe(context.getLifecycleOwner(),
            Observer { zone -> handleSilentZoneChange(zone) })
        model.currentlyShownReport.observe(context.getLifecycleOwner(),
            Observer { report -> handleShownReport(report) })
        model.severityFilterChanged.observe(context.getLifecycleOwner(),
            Observer { severityType -> severityFilterObserver(severityType) })
        model.accidentFilterChanged.observe(context.getLifecycleOwner(),
            Observer { accidentType -> accidentFilterObserver(accidentType) })

        liveData.addReportToMap.observe(
            context.getLifecycleOwner(),
            Observer { report -> addReportToMapObserver(report) })
        liveData.removeReportFromMap.observe(
            context.getLifecycleOwner(),
            Observer { report -> removeReportFromMapObserver(report) })
        liveData.updateReportOnMap.observe(
            context.getLifecycleOwner(),
            Observer { report -> updateReportOnMapObserver(report) })
        liveData.submittingReportStatus.observe(
            context.getLifecycleOwner(),
            Observer { status -> submittingReportObserver(status) })
        liveData.connectionStatus.observe(
            context.getLifecycleOwner(),
            Observer { status -> connectionStatusObserver(status) })
        liveData.reportFromNotificationStatus.observe(
            context.getLifecycleOwner(),
            Observer { status -> reportFromNotificationObserver(status) }
        )
        liveData.gradeSubmissionStatus.observe(
            context.getLifecycleOwner(),
            Observer { status -> handleGradeSubmission(status) }
        )
        liveData.registrationId.observe(context.getLifecycleOwner(),
            Observer { registrationId -> preferences.setUserId(registrationId) })

        notification.createNotificationChannel()
        notification.registerToNotifications()
        notification.receiveGeofenceRadiusUpdates()
        rebuildGeofences()

        var userId = preferences.getUserId()
        if (userId == null) {
            repository.register()
        }

        if (Constants.DEV_MODE) {
            //Clean up cache of the DB - we don't need it as we restart database
            thread {
                database.getGradeDao().deleteAll()
                database.getReportDao().deleteAll()
            }
        }
    }

    @SuppressLint("MissingPermission")
    override fun submitReport(reportProperties: ReportProperties) {
        var userId = preferences.getUserId()
        if (userId == null) {
            repository.register()
            userId = preferences.getUserId()
            if (userId == null) {
                view.showSubmitReportFailure()
                return
            }
        }

        if (context.getLocationPermissionState()) {
            thread {
                val currentLocation = location.getCurrentLocation()
                if (currentLocation == null) {
                    view.showSubmitReportFailure()
                    return@thread
                }
                val report = ReportAddRequest(
                    userId,
                    currentLocation.toPoint(),
                    reportProperties
                )
                repository.submitReport(report)
            }
        } else {
            seekPermissions(true)
        }
    }

    override fun submitGrade(grade: Grade) {
        var userId = preferences.getUserId()
        if (userId == null) {
            repository.register()
            userId = preferences.getUserId()
            if (userId == null) {
                view.showSubmitReportFailure()
                return
            }
        }

        if (context.getLocationPermissionState()) {
            thread {
                val currentLocation = location.getCurrentLocation()
                if (currentLocation == null) {
                    liveData.gradeSubmissionStatus.value = false
                    return@thread
                }
                val request =
                    ReportGradeRequest(
                        userId,
                        model.currentlyShownReport.value!!.id,
                        currentLocation.toPoint(),
                        grade
                    )
                repository.submitGrade(request)
            }
        } else {
            seekPermissions(true)
        }
    }

    override fun loadReportsToMap(northEast: Point, southWest: Point) {
        val currentReports = database.getReportDao().getFromLocation(
            southWest.longitude, northEast.longitude,
            southWest.latitude, northEast.latitude
        )
        currentReports.observe(context.getLifecycleOwner(), Observer {
            if (it.isEmpty()) {
                val request = ReportSearchRequest(
                    northEast,
                    southWest
                )
                repository.loadVisibleReportsChangedSince(request)
            } else {
                val earliestModificationDate =
                    it.minBy { report -> report.properties.modificationDate }!!
                val request = ReportSearchRequest(
                    northEast,
                    southWest,
                    earliestModificationDate.properties.modificationDate
                )
                repository.loadVisibleReportsChangedSince(request)
            }
            currentReports.removeObservers(context.getLifecycleOwner())
        })
    }

    override fun handleLocationBeingEnabled() {
        this.handleLocationPermissionChange(true)
    }

    override fun handleLocationBeingDisabled() {
        this.handleLocationPermissionChange(false)
    }

    @SuppressLint("MissingPermission")
    override fun zoomToCurrentLocation() {
        this.showCurrentLocation(animate = true)
    }

    @SuppressLint("MissingPermission")
    override fun showCurrentLocation() {
        this.showCurrentLocation(animate = false)
    }

    override fun displayReportAdd() {
        this.zoomToCurrentLocation()
        view.showAddSheet()
    }

    override fun seekPermissions(checkLocation: Boolean) {
        if (!context.checkForPlayServices()) {
            GoogleApiAvailability.getInstance()
                .makeGooglePlayServicesAvailable(context.getActivity())
        }
        if (checkLocation) {
            view.getPermissionForLocation()
        }
    }

    @SuppressLint("MissingPermission")
    override fun setRightButtonMode(bounds: LatLngBounds) {
        if (context.getLocationPermissionState()) {
            thread {
                val currentLocation = location.getCurrentLocation()
                if (currentLocation == null) {
                    view.showToast(R.string.location_unavailable_error, Toast.LENGTH_SHORT)
                    view.setRightButton(RightButtonMode.DISABLED, false)
                    return@thread
                }

                val transition =
                    bounds.contains(currentLocation.toLatLng()) xor model.insideLocationArea
                if (transition) {
                    model.insideLocationArea = !model.insideLocationArea
                }
                val buttonMode =
                    if (model.insideLocationArea) RightButtonMode.ADD_REPORT else RightButtonMode.SHOW_LOCATION
                view.setRightButton(buttonMode, transition)
            }
        } else {
            view.setRightButton(RightButtonMode.DISABLED, false)
        }
    }

    override fun displayReportFromNotifications(id: String) {
        repository.loadReport(id)
    }

    fun showCurrentLocation(animate: Boolean) {
        if (context.getLocationPermissionState()) {
            thread {
                val currentLocation = location.getCurrentLocation()
                if (currentLocation == null) {
                    view.showToast(R.string.location_unavailable_error, Toast.LENGTH_SHORT)
                }
                view.moveCamera(currentLocation.toLatLng(), animate)
            }
        } else {
            seekPermissions(true)
        }
    }

    fun handleLocationPermissionChange(permissionEnabled: Boolean) {
        view.setLocationStateForMap(permissionEnabled)

        val buttonMode =
            if (permissionEnabled) RightButtonMode.ADD_REPORT else RightButtonMode.DISABLED
        //TODO: replace with LiveData + observer in MapsActivity
        view.setRightButton(buttonMode, false)
        if (permissionEnabled) {
            showCurrentLocation()
        }
    }

    fun rebuildGeofences() {
        for (silentZone in SilentZone.values()) {
            when (silentZone) {
                SilentZone.HOME ->
                    silentZoneGeofence.reregisterZone(silentZone, model.homeSilentZoneName)
                SilentZone.WORK ->
                    silentZoneGeofence.reregisterZone(silentZone, model.workSilentZoneName)
            }
        }

        notificationGeofence.registerInitialGeofence()
    }

    //OBSERVERS
    fun reportFromNotificationObserver(status: String?) {
        when (status) {
            null -> {
                view.showToast(R.string.report_notification_unavailable, Toast.LENGTH_SHORT)
            }
            else -> {
                if (model.mapMarkers[status] != null) {
                    model.currentlyShownReport.value = model.mapMarkers[status]?.tag as Report
                }
            }
        }
    }

    fun addReportToMapObserver(report: Report) {
        val mapsMarker = view.addReportToMap(report)
        mapsMarker.tag = report
        model.mapMarkers[report.id] = mapsMarker
    }

    fun removeReportFromMapObserver(reportId: String) {
        model.mapMarkers[reportId]?.remove()
        model.mapMarkers.remove(reportId)
    }

    fun updateReportOnMapObserver(report: Report) {
        val mapMarker = model.mapMarkers[report.id] ?: return

        if ((mapMarker.tag as Report).properties != report.properties) {
            mapMarker.tag = report
            mapMarker.setIcon(BitmapDescriptorFactory.fromResource(report.properties.getGlyph()))
        }
    }

    fun submittingReportObserver(status: Boolean) {
        when (status) {
            true -> {
                view.dismissAddSheet()
            }
            false -> {
                view.showSubmitReportFailure()
                Crashlytics.log(Log.DEBUG, "Presenter", "Report submission failure")
            }
        }
    }

    fun connectionStatusObserver(status: Boolean?) {
        when (status) {
            true -> {
                view.dismissConnectionError()
            }
            false -> {
                view.showConnectionError()
                liveData.connectionStatus.value = null
            }
        }
    }

    fun severityFilterObserver(severity: Severity) {
        when (model.visibleSeverities.value?.contains(severity)) {
            true -> {
                preferences.setSeverity(severity, false)
                model.visibleSeverities.value?.remove(severity)
                model.visibleSeverities.value = model.visibleSeverities.value
            }
            false -> {
                preferences.setSeverity(severity, true)
                model.visibleSeverities.value?.add(severity)
                model.visibleSeverities.value = model.visibleSeverities.value
            }
        }
    }

    fun accidentFilterObserver(accident: Accident) {
        when (model.visibleAccidents.value?.contains(accident)) {
            true -> {
                preferences.setAccident(accident, false)
                model.visibleAccidents.value?.remove(accident)
                model.visibleAccidents.value = model.visibleAccidents.value
            }
            false -> {
                preferences.setAccident(accident, true)
                model.visibleAccidents.value?.add(accident)
                model.visibleAccidents.value = model.visibleAccidents.value
            }
        }
    }

    fun handleChangesInReports(newReportList: List<Report>) {
        model.mapMarkers.keys.filterNot { id -> newReportList.any { report -> report.id == id } }
            .forEach { id -> liveData.removeReportFromMap.value = id }

        newReportList.filter { report -> model.mapMarkers.containsKey(report.id) }
            .forEach { report -> liveData.updateReportOnMap.value = report }

        newReportList.filterNot { report -> model.mapMarkers.containsKey(report.id) }
            .forEach { report -> liveData.addReportToMap.value = report }
    }

    fun handleShownReport(report: Report) {
        val grade = database.getGradeDao().getGradesByReportIdAsync(report.id)
        grade.observe(context.getLifecycleOwner(), Observer { grades ->
            if (grades.isEmpty()) {
                model.currentlyShownGrade.value = Grade.UNGRADED
            } else {
                model.currentlyShownGrade.value = grades[0].grade
            }
            grade.removeObservers(context.getLifecycleOwner())
        })
    }

    fun handleGradeSubmission(status: Boolean) {
        when (status) {
            true -> {
                if (model.currentlyShownReport.value == null) {
                    return
                }
                val grade = database.getGradeDao()
                    .getGradesByReportIdAsync(model.currentlyShownReport.value!!.id)
                grade.observe(context.getLifecycleOwner(), Observer { grades ->
                    if (grades.isEmpty()) {
                        model.currentlyShownGrade.value = Grade.UNGRADED
                        liveData.gradeSubmissionStatus.value = false
                        context.showToast(R.string.grade_error_toast, Toast.LENGTH_SHORT)
                        Crashlytics.log(Log.DEBUG, "Presenter", "Grade not saved")
                    } else {
                        model.currentlyShownGrade.value = grades[0].grade
                        context.showToast(R.string.grade_thanks_toast, Toast.LENGTH_SHORT)
                    }
                    grade.removeObservers(context.getLifecycleOwner())
                })
            }
            false -> {
                context.showToast(R.string.grade_error_toast, Toast.LENGTH_SHORT)
            }
        }
    }

    fun handleSilentZoneChange(silentZone: SilentZone) {
        if (silentZoneGeofence.isRunning(silentZone)) {
            silentZoneGeofence.disableZone(silentZone)
            when (silentZone) {
                SilentZone.HOME ->
                    model.homeSilentZoneName.value = ""
                SilentZone.WORK ->
                    model.workSilentZoneName.value = ""
            }
        } else {
            when (silentZone) {
                SilentZone.HOME ->
                    silentZoneGeofence.enableZone(silentZone, model.homeSilentZoneName)
                SilentZone.WORK ->
                    silentZoneGeofence.enableZone(silentZone, model.workSilentZoneName)
            }
        }
    }
}