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
import pl.herfor.android.interfaces.ContextRepository
import pl.herfor.android.interfaces.MarkerContract
import pl.herfor.android.objects.*
import pl.herfor.android.objects.enums.Accident
import pl.herfor.android.objects.enums.Grade
import pl.herfor.android.objects.enums.RightButtonMode
import pl.herfor.android.objects.enums.Severity
import pl.herfor.android.retrofits.RetrofitRepository
import pl.herfor.android.services.ActivityRecognitionService
import pl.herfor.android.utils.Constants
import pl.herfor.android.utils.Constants.Companion.NOTIFICATION_CHANNEL_ID
import pl.herfor.android.utils.toLatLng
import pl.herfor.android.utils.toPoint
import pl.herfor.android.viewmodels.MarkerViewModel

class MarkerViewPresenter(
    private val model: MarkerViewModel, private val view: MarkerContract.View,
    private val context: ContextRepository, private val repository: RetrofitRepository
) : MarkerContract.Presenter {
    override fun start() {
        if (model.started) {
            return
        }

        model.addMarkerToMap.observe(context.getLifecycleOwner(),
            Observer { marker -> addMarkerToMapObserver(marker) })
        model.removeMarkerFromMap.observe(context.getLifecycleOwner(),
            Observer { marker -> removeMarkerFromMapObserver(marker) })
        model.updateMarkerOnMap.observe(context.getLifecycleOwner(),
            Observer { marker -> updateMarkerOnMapObserver(marker) })
        model.filteredMarkers.observe(
            context.getLifecycleOwner(),
            Observer { newMarkerList -> handleChangesInMarkers(newMarkerList) })
        model.submittingMarkerStatus.observe(context.getLifecycleOwner(),
            Observer { status -> submittingMarkerObserver(status) })
        model.connectionStatus.observe(context.getLifecycleOwner(),
            Observer { status -> connectionStatusObserver(status) })
        model.severityFilterChanged.observe(context.getLifecycleOwner(),
            Observer { severityType -> severityFilterObserver(severityType) })
        model.accidentFilterChanged.observe(context.getLifecycleOwner(),
            Observer { accidentType -> accidentFilterObserver(accidentType) })
        model.markerFromNotificationStatus.observe(
            context.getLifecycleOwner(),
            Observer { status -> markerFromNotificationObserver(status) }
        )
        model.currentlyShownMarker.observe(context.getLifecycleOwner(),
            Observer { marker -> handleShownMarker(marker) })

        createNotificationChannel()
        FirebaseMessaging.getInstance().subscribeToTopic("marker-new")
        FirebaseMessaging.getInstance().subscribeToTopic("marker-remove")

        createActivityRequest()

        model.started = true
    }

    override fun stop() {
        model.addMarkerToMap.removeObservers(context.getLifecycleOwner())
        model.removeMarkerFromMap.removeObservers(context.getLifecycleOwner())
        model.updateMarkerOnMap.removeObservers(context.getLifecycleOwner())
        model.submittingMarkerStatus.removeObservers(context.getLifecycleOwner())
        model.connectionStatus.removeObservers(context.getLifecycleOwner())
        model.accidentFilterChanged.removeObservers(context.getLifecycleOwner())
        model.severityFilterChanged.removeObservers(context.getLifecycleOwner())
        model.markerFromNotificationStatus.removeObservers(context.getLifecycleOwner())
        model.currentlyShownMarker.removeObservers(context.getLifecycleOwner())

        model.started = false
    }

    @SuppressLint("MissingPermission")
    override fun submitMarker(markerProperties: MarkerProperties) {
        if (locationPermissionAvailable()) {
            context.getCurrentLocation().addOnSuccessListener { location: Location? ->
                if (location != null) {
                    val marker = MarkerAddRequest(location.toPoint(), markerProperties)
                    repository.submitMarker(marker)
                } else {
                    view.showSubmitMarkerFailure()
                }
            }
                .addOnFailureListener {
                    view.showSubmitMarkerFailure()
                }
        } else {
            seekPermissions(true)
        }
    }

    override fun submitGrade(grade: Grade) {
        if (locationPermissionAvailable()) {
            context.getCurrentLocation().addOnSuccessListener {
                val request =
                    MarkerGradeRequest(model.currentlyShownMarker.value!!.id, it.toPoint(), grade)
                repository.submitGrade(request)
            }
        } else {
            seekPermissions(true)
        }
    }

    override fun loadMarkersToMap(northEast: Point, southWest: Point) {
        val currentMarkers = model.markerDao.getFromLocation(
            northEast.longitude, southWest.longitude,
            northEast.latitude, southWest.latitude
        )
        currentMarkers.observe(context.getLifecycleOwner(), Observer {
            if (it.isEmpty()) {
                val request = MarkersLookupRequest(
                    northEast,
                    southWest
                )
                repository.loadVisibleMarkersChangedSince(request)
            } else {
                val earliestModificationDate =
                    it.maxBy { marker -> marker.properties.modificationDate }!!
                val request = MarkersLookupRequest(
                    northEast,
                    southWest,
                    earliestModificationDate.properties.modificationDate
                )
                repository.loadVisibleMarkersChangedSince(request)
            }
            currentMarkers.removeObservers(context.getLifecycleOwner())
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

    override fun displayMarkerAdd() {
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
        if (locationPermissionAvailable()) {
            context.getCurrentLocation().addOnSuccessListener { location: Location? ->
                if (location != null) {
                    val transition =
                        bounds.contains(location.toLatLng()) xor model.insideLocationArea
                    if (transition) {
                        model.insideLocationArea = !model.insideLocationArea
                    }
                    val buttonMode =
                        if (model.insideLocationArea) RightButtonMode.ADD_MARKER else RightButtonMode.SHOW_LOCATION
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

    override fun displayMarkerFromNotifications(id: String?) {
        if (id != null) {
            repository.loadSingleMarkerForNotification(id)
        }
    }

    private fun checkForPlayServices() {
        val playServicesCode =
            GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(context.getContext())
        if (playServicesCode != ConnectionResult.SUCCESS) {
            GoogleApiAvailability.getInstance()
                .makeGooglePlayServicesAvailable(context.getActivity())
        }
    }

    private fun locationPermissionAvailable(): Boolean {
        return ContextCompat.checkSelfPermission(
            context.getContext(),
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun createNotificationChannel() {
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

    private fun showCurrentLocation(animate: Boolean) {
        if (locationPermissionAvailable()) {
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

    private fun handleLocationPermissionChange(permissionEnabled: Boolean) {
        val buttonMode =
            if (permissionEnabled) RightButtonMode.ADD_MARKER else RightButtonMode.DISABLED
        //TODO: replace with LiveData + observer in MapsActivity
        view.setLocationStateForMap(permissionEnabled)
        view.setRightButton(buttonMode, false)
        if (permissionEnabled) {
            showCurrentLocation()
        }
    }

    private fun createActivityRequest() {
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
    private fun markerFromNotificationObserver(status: String?) {
        when (status) {
            null -> {
                context.showToast(R.string.marker_notification_unavailable, Toast.LENGTH_SHORT)
            }
            else -> {
                if (model.mapMarkers[status] != null) {
                    model.currentlyShownMarker.value = model.mapMarkers[status]?.tag as MarkerData
                }
            }
        }
    }

    private fun addMarkerToMapObserver(marker: MarkerData) {
        val mapsMarker = view.addMarkerToMap(marker)
        mapsMarker.tag = marker
        model.mapMarkers[marker.id] = mapsMarker
    }

    private fun removeMarkerFromMapObserver(markerId: String) {
        model.mapMarkers[markerId]?.remove()
        model.mapMarkers.remove(markerId)
    }

    private fun updateMarkerOnMapObserver(marker: MarkerData) {
        val mapMarker = model.mapMarkers[marker.id] ?: return

        if ((mapMarker.tag as MarkerData).properties != marker.properties) {
            mapMarker.tag = marker
            mapMarker.setIcon(BitmapDescriptorFactory.fromResource(marker.properties.getGlyph()))
        }
    }

    private fun submittingMarkerObserver(status: Boolean) {
        when (status) {
            true -> {
                view.dismissAddSheet()
            }
            false -> {
                view.showSubmitMarkerFailure()
            }
        }
    }

    private fun connectionStatusObserver(status: Boolean?) {
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

    private fun severityFilterObserver(severity: Severity) {
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

    private fun accidentFilterObserver(accident: Accident) {
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

    private fun handleChangesInMarkers(newMarkerList: List<MarkerData>) {
        model.mapMarkers.keys.filterNot { id -> newMarkerList.any { markerData -> markerData.id == id } }
            .forEach { id -> model.removeMarkerFromMap.value = id }

        newMarkerList.filter { markerData -> model.mapMarkers.containsKey(markerData.id) }
            .forEach { markerData -> model.updateMarkerOnMap.value = markerData }

        newMarkerList.filterNot { markerData -> model.mapMarkers.containsKey(markerData.id) }
            .forEach { markerData -> model.addMarkerToMap.value = markerData }
    }

    private fun handleShownMarker(marker: MarkerData) {
        val liveData = model.gradeDao.getGradesByMarkerId(marker.id)
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