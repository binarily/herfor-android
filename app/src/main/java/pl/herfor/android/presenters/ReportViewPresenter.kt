package pl.herfor.android.presenters

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.location.ActivityRecognition
import com.google.android.gms.location.ActivityTransitionRequest
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLngBounds
import com.google.firebase.messaging.FirebaseMessaging
import org.koin.core.KoinComponent
import org.koin.core.inject
import pl.herfor.android.R
import pl.herfor.android.interfaces.AppContract
import pl.herfor.android.interfaces.ContextRepository
import pl.herfor.android.modules.LiveDataModule
import pl.herfor.android.modules.NotificationGeofenceModule
import pl.herfor.android.modules.PreferencesModule
import pl.herfor.android.modules.SilentZoneGeofenceModule
import pl.herfor.android.objects.Point
import pl.herfor.android.objects.Report
import pl.herfor.android.objects.ReportProperties
import pl.herfor.android.objects.enums.*
import pl.herfor.android.objects.requests.ReportAddRequest
import pl.herfor.android.objects.requests.ReportGradeRequest
import pl.herfor.android.objects.requests.ReportSearchRequest
import pl.herfor.android.retrofits.RetrofitRepository
import pl.herfor.android.services.ActivityRecognitionService
import pl.herfor.android.utils.Constants
import pl.herfor.android.utils.Constants.Companion.NOTIFICATION_CHANNEL_ID
import pl.herfor.android.utils.toLatLng
import pl.herfor.android.utils.toPoint
import pl.herfor.android.viewmodels.ReportViewModel

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

    override fun start() {
        if (liveData.started) {
            return
        }
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
        liveData.registrationId.observe(context.getLifecycleOwner(),
            Observer { registrationId -> preferences.setUserId(registrationId) })

        createNotificationChannel()
        registerToNotifications()
        requestActivityUpdates()
        rebuildGeofences()

        liveData.started = true
    }

    override fun stop() {
        model.filteredReports.removeObservers(context.getLifecycleOwner())
        model.accidentFilterChanged.removeObservers(context.getLifecycleOwner())
        model.severityFilterChanged.removeObservers(context.getLifecycleOwner())
        model.currentlyShownReport.removeObservers(context.getLifecycleOwner())
        model.silentZoneToggled.removeObservers(context.getLifecycleOwner())

        liveData.addReportToMap.removeObservers(context.getLifecycleOwner())
        liveData.removeReportFromMap.removeObservers(context.getLifecycleOwner())
        liveData.updateReportOnMap.removeObservers(context.getLifecycleOwner())
        liveData.submittingReportStatus.removeObservers(context.getLifecycleOwner())
        liveData.connectionStatus.removeObservers(context.getLifecycleOwner())
        liveData.reportFromNotificationStatus.removeObservers(context.getLifecycleOwner())
        liveData.registrationId.removeObservers(context.getLifecycleOwner())

        liveData.started = false
    }

    @SuppressLint("MissingPermission")
    override fun submitReport(reportProperties: ReportProperties) {
        val userId = preferences.getUserId()
        if (userId == null) {
            repository.register()
            view.showSubmitReportFailure()
            return
        }

        if (checkForLocationPermission()) {
            context.getCurrentLocation().addOnSuccessListener { location: Location? ->
                if (location != null) {
                    val report = ReportAddRequest(
                        userId,
                        location.toPoint(),
                        reportProperties
                    )
                    repository.submitReport(report)
                } else {
                    view.showSubmitReportFailure()
                }
            }
                .addOnFailureListener {
                    view.showSubmitReportFailure()
                }
        } else {
            seekPermissions(true)
        }
    }

    override fun submitGrade(grade: Grade) {
        val userId = preferences.getUserId()
        if (userId == null) {
            repository.register()
            liveData.gradeSubmissionStatus.value = false
            return
        }

        if (checkForLocationPermission()) {
            context.getCurrentLocation().addOnSuccessListener {
                val request =
                    ReportGradeRequest(
                        userId,
                        model.currentlyShownReport.value!!.id,
                        it.toPoint(),
                        grade
                    )
                repository.submitGrade(request)
            }
        } else {
            seekPermissions(true)
        }
    }

    override fun loadReportsToMap(northEast: Point, southWest: Point) {
        val currentReports = context.getReportDao().getFromLocation(
            northEast.longitude, southWest.longitude,
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
        this.checkForPlayServices()
        if (checkLocation) {
            view.getPermissionForLocation()
        }
    }

    @SuppressLint("MissingPermission")
    override fun setRightButtonMode(bounds: LatLngBounds) {
        if (checkForLocationPermission()) {
            context.getCurrentLocation().addOnSuccessListener { location: Location? ->
                if (location != null) {
                    val transition =
                        bounds.contains(location.toLatLng()) xor model.insideLocationArea
                    if (transition) {
                        model.insideLocationArea = !model.insideLocationArea
                    }
                    val buttonMode =
                        if (model.insideLocationArea) RightButtonMode.ADD_REPORT else RightButtonMode.SHOW_LOCATION
                    view.setRightButton(buttonMode, transition)
                } else {
                    context.showToast(R.string.location_unavailable_error, Toast.LENGTH_SHORT)
                    view.setRightButton(RightButtonMode.DISABLED, false)
                }
            }
        } else {
            view.setRightButton(RightButtonMode.DISABLED, false)
        }
    }

    override fun displayReportFromNotifications(id: String) {
        repository.loadReport(id)
    }

    //TODO: to context
    fun checkForPlayServices() {
        val playServicesCode =
            GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(context.getContext())
        if (playServicesCode != ConnectionResult.SUCCESS) {
            GoogleApiAvailability.getInstance()
                .makeGooglePlayServicesAvailable(context.getActivity())
        }
    }

    //TODO: to context
    fun checkForLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context.getContext(),
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    //TODO: to notification module
    fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = context.getContext().getString(R.string.channel_name)
            val descriptionText = context.getContext().getString(R.string.channel_description)
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(NOTIFICATION_CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            // Register the channel with the system
            val notificationManager: NotificationManager =
                context.getContext().getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        } else {
            Log.d(this.javaClass.name, "Notification channel not necessary on this platform.")
        }
    }

    fun showCurrentLocation(animate: Boolean) {
        if (checkForLocationPermission()) {
            context.getCurrentLocation().addOnSuccessListener { location: Location? ->
                if (location != null) {
                    view.moveCamera(location.toLatLng(), animate)
                } else {
                    context.showToast(R.string.location_unavailable_error, Toast.LENGTH_SHORT)
                }
            }
        } else {
            seekPermissions(true)
        }
    }

    fun handleLocationPermissionChange(permissionEnabled: Boolean) {
        val buttonMode =
            if (permissionEnabled) RightButtonMode.ADD_REPORT else RightButtonMode.DISABLED
        //TODO: replace with LiveData + observer in MapsActivity
        view.setLocationStateForMap(permissionEnabled)
        view.setRightButton(buttonMode, false)
        if (permissionEnabled) {
            showCurrentLocation()
        }
    }

    fun requestActivityUpdates() {
        val pendingIntent = PendingIntent.getService(
            context.getContext(),
            0,
            Intent(context.getContext(), ActivityRecognitionService::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT
        )

        ActivityRecognition.getClient(context.getContext())
            .requestActivityTransitionUpdates(
                ActivityTransitionRequest(Constants.TRANSITIONS),
                pendingIntent
            )
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

    //TODO: to notification module
    fun registerToNotifications() {
        FirebaseMessaging.getInstance().subscribeToTopic("report-new")
        FirebaseMessaging.getInstance().subscribeToTopic("report-update")
        FirebaseMessaging.getInstance().subscribeToTopic("report-remove")
    }

    //OBSERVERS
    //TODO: move to separate class, aka ObserverCollection
    fun reportFromNotificationObserver(status: String?) {
        when (status) {
            null -> {
                context.showToast(R.string.report_notification_unavailable, Toast.LENGTH_SHORT)
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
            }
            false -> {
                preferences.setSeverity(severity, true)
                model.visibleSeverities.value?.add(severity)
            }
        }
    }

    fun accidentFilterObserver(accident: Accident) {
        when (model.visibleAccidents.value?.contains(accident)) {
            true -> {
                preferences.setAccident(accident, false)
                model.visibleAccidents.value?.remove(accident)
            }
            false -> {
                preferences.setAccident(accident, true)
                model.visibleAccidents.value?.add(accident)
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
        val grade = context.getGradeDao().getGradesByReportIdAsync(report.id)
        grade.observe(context.getLifecycleOwner(), Observer { grades ->
            if (grades.isEmpty()) {
                model.currentlyShownGrade.value = Grade.UNGRADED
            } else {
                model.currentlyShownGrade.value = grades[0].grade
            }
            grade.removeObservers(context.getLifecycleOwner())
        })
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