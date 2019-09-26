package pl.herfor.android.presenters

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.Marker
import com.google.firebase.messaging.FirebaseMessaging
import pl.herfor.android.R
import pl.herfor.android.interfaces.ContextRepository
import pl.herfor.android.interfaces.MarkerContract
import pl.herfor.android.objects.*
import pl.herfor.android.utils.Constants.Companion.NOTIFICATION_CHANNEL_ID
import pl.herfor.android.utils.toLatLng
import pl.herfor.android.utils.toPoint
import pl.herfor.android.viewmodels.MarkerViewModel
import kotlin.concurrent.thread

class MarkerViewPresenter(
    private val model: MarkerViewModel, private val view: MarkerContract.View,
    private val context: ContextRepository
) : MarkerContract.Presenter {

    override fun start() {
        if (model.started) {
            return
        }

        model.addMarkerToMap.observe(context.getLifecycleOwner(), Observer<MarkerData> { marker ->
            if (marker.id != null) {
                val mapsMarker = view.addMarker(marker)
                mapsMarker.tag = marker
                model.mapMarkers[marker.id!!] = mapsMarker
            }
        })
        model.removeMarkerFromMap.observe(
            context.getLifecycleOwner(),
            Observer<MarkerData> { marker ->
                model.mapMarkers[marker.id]?.remove()
                model.mapMarkers.remove(marker.id)
            })
        model.submittingMarkerStatus.observe(
            context.getLifecycleOwner(),
            Observer<Boolean> { status ->
                when (status) {
                    true -> {
                        view.dismissAddSheet()
                    }
                    false -> {
                        view.showSubmitMarkerFailure()
                    }
                }
            })
        model.connectionStatus.observe(context.getLifecycleOwner(), Observer<Boolean> { status ->
            when (status) {
                true -> {
                    view.dismissConnectionError()
                }
                false -> {
                    view.showConnectionError()
                    model.connectionStatus.value = null
                }
            }
        })

        model.severityFilterChanged.observe(
            context.getLifecycleOwner(),
            Observer<SeverityType> { severityType ->
                when (model.visibleSeverities.contains(severityType)) {
                    true -> {
                        model.visibleSeverities.remove(severityType)
                        model.markersByPoint.filter { marker -> marker.value.properties.severityType == severityType }
                            .forEach { marker -> model.removeMarkerFromMap.value = marker.value }
                    }
                    false -> {
                        model.visibleSeverities.add(severityType)
                        model.markersByPoint.filter { marker -> marker.value.properties.severityType == severityType }
                            .forEach { marker -> model.addMarkerToMap.value = marker.value }
                    }
                }
            })

        model.accidentFilterChanged.observe(
            context.getLifecycleOwner(),
            Observer<AccidentType> { accidentType ->
                when (model.visibleAccidentTypes.contains(accidentType)) {
                    true -> {
                        model.visibleAccidentTypes.remove(accidentType)
                        model.markersByPoint.filter { marker -> marker.value.properties.accidentType == accidentType }
                            .forEach { marker -> model.removeMarkerFromMap.value = marker.value }
                    }
                    false -> {
                        model.visibleAccidentTypes.add(accidentType)
                        model.markersByPoint.filter { marker -> marker.value.properties.accidentType == accidentType }
                            .forEach { marker -> model.addMarkerToMap.value = marker.value }
                    }
                }
            })

        model.markerFromNotificationStatus.observe(
            context.getLifecycleOwner(),
            Observer<String> { status ->
                when (status) {
                    null -> {
                        Toast.makeText(
                            context.getContext(),
                            context.getContext().getString(R.string.marker_notification_unavailable),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    else -> {
                        if (model.mapMarkers[status] != null) {
                            displayMarkerDetails(model.mapMarkers[status]!!)
                        }
                    }
                }
            }
        )

        view.setSeverityTypeFilters(model.visibleSeverities)
        view.setAccidentTypeFilters(model.visibleAccidentTypes)

        createNotificationChannel()
        FirebaseMessaging.getInstance().subscribeToTopic("marker")

        model.started = true
    }

    override fun stop() {
        model.addMarkerToMap.removeObservers(context.getLifecycleOwner())
        model.removeMarkerFromMap.removeObservers(context.getLifecycleOwner())
        model.submittingMarkerStatus.removeObservers(context.getLifecycleOwner())
        model.connectionStatus.removeObservers(context.getLifecycleOwner())
        model.accidentFilterChanged.removeObservers(context.getLifecycleOwner())
        model.severityFilterChanged.removeObservers(context.getLifecycleOwner())

        model.started = false
    }

    override fun displayMarkerDetails(marker: Marker) {
        view.moveCamera(marker.position, animate = true)
        view.showDetailsSheet(marker.tag as MarkerData)
        val position = marker.position
        thread {
            view.showLocationOnDetailsSheet(
                context.getLocationName(
                    position.latitude,
                    position.longitude
                )
            )
        }
    }

    @SuppressLint("MissingPermission")
    override fun submitMarker(markerProperties: MarkerProperties) {
        if (locationPermissionAvailable()) {
            context.getCurrentLocation().addOnSuccessListener { location: Location? ->
                if (location != null) {
                    val marker = MarkerData(location.toPoint(), markerProperties)
                    model.addMarker(marker)
                } else {
                    view.showSubmitMarkerFailure()
                }
            }
                .addOnFailureListener {
                    view.showSubmitMarkerFailure()
                }
        } else {
            askForLocationPermission()
        }
    }

    override fun loadVisibleMarkers(northEast: Point, southWest: Point) {
        val request = MarkersLookupRequest(northEast, southWest)
        model.loadAllVisibleMarkers(request)
    }

    override fun handleLocationBeingEnabled() {
        handleLocationPermissionChange(true)
    }

    override fun handleLocationBeingDisabled() {
        handleLocationPermissionChange(false)
    }

    @SuppressLint("MissingPermission")
    override fun zoomToCurrentLocation() {
        showCurrentLocation(animate = true)
    }

    @SuppressLint("MissingPermission")
    override fun showCurrentLocation() {
        showCurrentLocation(animate = false)
    }

    override fun displayMarkerAdd() {
        zoomToCurrentLocation()
        view.showAddSheet()
    }

    override fun askForLocationPermission() {
        view.getPermissionForLocation()
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
                    //TODO("No location available message")
                    view.setRightButton(RightButtonMode.DISABLED, false)
                }
            }
        } else {
            view.setRightButton(RightButtonMode.DISABLED, false)
        }
    }

    override fun checkForPlayServices() {
        val playServicesCode =
            GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(context.getContext())
        if (playServicesCode != ConnectionResult.SUCCESS) {
            GoogleApiAvailability.getInstance()
                .makeGooglePlayServicesAvailable(context.getActivity())
        }
    }

    override fun toggleSeverityType(severityType: SeverityType) {
        model.severityFilterChanged.value = severityType
    }

    override fun toggleAccidentType(accidentType: AccidentType) {
        model.accidentFilterChanged.value = accidentType
    }

    override fun displayMarkerFromNotifications(id: String) {
        model.loadSingleMarkerForNotification(id)
    }

    private fun locationPermissionAvailable(): Boolean {
        if (ContextCompat.checkSelfPermission(
                context.getContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            )
            != PackageManager.PERMISSION_GRANTED
        ) {
            return false
        }
        return true
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
        }
    }

    private fun showCurrentLocation(animate: Boolean) {
        if (locationPermissionAvailable()) {
            context.getCurrentLocation().addOnSuccessListener { location: Location? ->
                if (location != null) {
                    view.moveCamera(location.toLatLng(), animate)
                } else {
                    TODO("Show error here because no location available right now.")
                }
            }
        } else {
            askForLocationPermission()
        }
    }

    private fun handleLocationPermissionChange(result: Boolean) {
        val buttonMode = if (result) RightButtonMode.ADD_MARKER else RightButtonMode.DISABLED
        model.locationEnabled = result
        view.setLocationStateForMap(result)
        view.setRightButton(buttonMode, false)
        if (result) {
            showCurrentLocation()
        }
    }

}