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
import pl.herfor.android.R
import pl.herfor.android.interfaces.AppContract
import pl.herfor.android.interfaces.ContextRepository
import pl.herfor.android.objects.Point
import pl.herfor.android.objects.Report
import pl.herfor.android.objects.ReportProperties
import pl.herfor.android.objects.enums.Accident
import pl.herfor.android.objects.enums.Grade
import pl.herfor.android.objects.enums.RightButtonMode
import pl.herfor.android.objects.enums.Severity
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
    private val model: ReportViewModel, private val view: AppContract.View,
    private val context: ContextRepository, private val repository: RetrofitRepository
) : AppContract.Presenter {
    override fun start() {
        if (model.started) {
            return
        }

        model.addReportToMap.observe(context.getLifecycleOwner(),
            Observer { report -> addReportToMapObserver(report) })
        model.removeReportFromMap.observe(context.getLifecycleOwner(),
            Observer { report -> removeReportFromMapObserver(report) })
        model.updateReportOnMap.observe(context.getLifecycleOwner(),
            Observer { report -> updateReportOnMapObserver(report) })
        model.filteredReports.observe(
            context.getLifecycleOwner(),
            Observer { newReportList -> handleChangesInReports(newReportList) })
        model.submittingReportStatus.observe(context.getLifecycleOwner(),
            Observer { status -> submittingReportObserver(status) })
        model.connectionStatus.observe(context.getLifecycleOwner(),
            Observer { status -> connectionStatusObserver(status) })
        model.severityFilterChanged.observe(context.getLifecycleOwner(),
            Observer { severityType -> severityFilterObserver(severityType) })
        model.accidentFilterChanged.observe(context.getLifecycleOwner(),
            Observer { accidentType -> accidentFilterObserver(accidentType) })
        model.reportFromNotificationStatus.observe(
            context.getLifecycleOwner(),
            Observer { status -> reportFromNotificationObserver(status) }
        )
        model.currentlyShownReport.observe(context.getLifecycleOwner(),
            Observer { report -> handleShownReport(report) })

        createNotificationChannel()
        FirebaseMessaging.getInstance().subscribeToTopic("report-new")
        FirebaseMessaging.getInstance().subscribeToTopic("report-update")
        FirebaseMessaging.getInstance().subscribeToTopic("report-remove")

        createActivityRequest()

        model.started = true
    }

    override fun stop() {
        model.addReportToMap.removeObservers(context.getLifecycleOwner())
        model.removeReportFromMap.removeObservers(context.getLifecycleOwner())
        model.updateReportOnMap.removeObservers(context.getLifecycleOwner())
        model.submittingReportStatus.removeObservers(context.getLifecycleOwner())
        model.connectionStatus.removeObservers(context.getLifecycleOwner())
        model.accidentFilterChanged.removeObservers(context.getLifecycleOwner())
        model.severityFilterChanged.removeObservers(context.getLifecycleOwner())
        model.reportFromNotificationStatus.removeObservers(context.getLifecycleOwner())
        model.currentlyShownReport.removeObservers(context.getLifecycleOwner())

        model.started = false
    }

    @SuppressLint("MissingPermission")
    override fun submitReport(reportProperties: ReportProperties) {
        if (checkForLocationPermission()) {
            context.getCurrentLocation().addOnSuccessListener { location: Location? ->
                if (location != null) {
                    val report = ReportAddRequest(
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
        if (checkForLocationPermission()) {
            context.getCurrentLocation().addOnSuccessListener {
                val request =
                    ReportGradeRequest(
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
        val currentReports = model.reportDao.getFromLocation(
            northEast.longitude, southWest.longitude,
            northEast.latitude, southWest.latitude
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

    override fun displayReportFromNotifications(id: String?) {
        if (id != null) {
            repository.loadReport(id)
        }
    }

    fun checkForPlayServices() {
        val playServicesCode =
            GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(context.getContext())
        if (playServicesCode != ConnectionResult.SUCCESS) {
            GoogleApiAvailability.getInstance()
                .makeGooglePlayServicesAvailable(context.getActivity())
        }
    }

    fun checkForLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context.getContext(),
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

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

    fun createActivityRequest() {
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
                model.connectionStatus.value = null
            }
        }
    }

    fun severityFilterObserver(severity: Severity) {
        val sharedPreferences = context.getSharedPreferences("preferences", Context.MODE_PRIVATE)

        when (model.visibleSeverities.value?.contains(severity)) {
            true -> {
                sharedPreferences.edit().putBoolean("severity.${severity.name}", false)
                    .apply()
                model.visibleSeverities.value?.remove(severity)
            }
            false -> {
                sharedPreferences.edit().putBoolean("severity.${severity.name}", true)
                    .apply()
                model.visibleSeverities.value?.add(severity)
            }
        }
    }

    fun accidentFilterObserver(accident: Accident) {
        val sharedPreferences = context.getSharedPreferences("preferences", Context.MODE_PRIVATE)

        when (model.visibleAccidentTypes.value?.contains(accident)) {
            true -> {
                sharedPreferences.edit().putBoolean("accident.${accident.name}", false)
                    .apply()
                model.visibleAccidentTypes.value?.remove(accident)
            }
            false -> {
                sharedPreferences.edit().putBoolean("accident.${accident.name}", true)
                    .apply()
                model.visibleAccidentTypes.value?.add(accident)
            }
        }
    }

    fun handleChangesInReports(newReportList: List<Report>) {
        model.mapMarkers.keys.filterNot { id -> newReportList.any { report -> report.id == id } }
            .forEach { id -> model.removeReportFromMap.value = id }

        newReportList.filter { report -> model.mapMarkers.containsKey(report.id) }
            .forEach { report -> model.updateReportOnMap.value = report }

        newReportList.filterNot { report -> model.mapMarkers.containsKey(report.id) }
            .forEach { report -> model.addReportToMap.value = report }
    }

    fun handleShownReport(report: Report) {
        val liveData = model.gradeDao.getGradesByReportIdAsync(report.id)
        liveData.observe(context.getLifecycleOwner(), Observer { grades ->
            if (grades.isEmpty()) {
                model.currentlyShownGrade.value = Grade.UNGRADED
            } else {
                model.currentlyShownGrade.value = grades[0].grade
            }
            liveData.removeObservers(context.getLifecycleOwner())
        })
    }
}