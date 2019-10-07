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
import com.google.android.gms.maps.model.BitmapDescriptorFactory
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

        model.addMarkerToMap.observe(context.getLifecycleOwner(),
            Observer { marker -> addMarkerToMapObserver(marker) })
        model.removeMarkerFromMap.observe(context.getLifecycleOwner(),
            Observer { marker -> removeMarkerFromMapObserver(marker) })
        model.updateMarkerOnMap.observe(context.getLifecycleOwner(),
            Observer { marker -> updateMarkerOnMapObserver(marker) })
        model.existingMarkers.observe(context.getLifecycleOwner(),
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

        initializeSeverityTypes()
        initializeAccidentTypes()

        view.setSeverityTypeFilters(model.visibleSeverities)
        view.setAccidentTypeFilters(model.visibleAccidentTypes)

        createNotificationChannel()
        FirebaseMessaging.getInstance().subscribeToTopic("marker")

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
                    val marker = MarkerAddRequest(location.toPoint(), markerProperties)
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
                    context.showToast(R.string.location_unavailable_error, Toast.LENGTH_SHORT)
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

    override fun displayMarkerFromNotifications(id: String?) {
        if (id != null) {
            model.loadSingleMarkerForNotification(id)
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
            //TODO: log.d - do nothing
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
            askForLocationPermission()
        }
    }

    private fun handleLocationPermissionChange(permissionEnabled: Boolean) {
        val buttonMode =
            if (permissionEnabled) RightButtonMode.ADD_MARKER else RightButtonMode.DISABLED
        model.locationEnabled = permissionEnabled
        view.setLocationStateForMap(permissionEnabled)
        view.setRightButton(buttonMode, false)
        if (permissionEnabled) {
            showCurrentLocation()
        }
    }

    private fun initializeSeverityTypes() {
        val sharedPreferences = context.getSharedPreferences("preferences", Context.MODE_PRIVATE)
        val severities = arrayOf(
            if (sharedPreferences.getBoolean(
                    "severityType.GREEN",
                    false
                )
            ) SeverityType.GREEN else null,
            if (sharedPreferences.getBoolean(
                    "severityType.YELLOW",
                    true
                )
            ) SeverityType.YELLOW else null,
            if (sharedPreferences.getBoolean("severityType.RED", true)) SeverityType.RED else null
        )

        model.visibleSeverities.clear()
        model.visibleSeverities.addAll(severities.filterNotNull())
    }

    private fun initializeAccidentTypes() {
        val sharedPreferences = context.getSharedPreferences("preferences", Context.MODE_PRIVATE)
        val accidentTypes = mutableListOf<AccidentType?>()

        for (accidentType in AccidentType.values()) {
            accidentTypes.add(
                if (sharedPreferences.getBoolean(
                        "accidentType.${accidentType.name}",
                        true
                    )
                ) accidentType else null
            )
        }

        model.visibleAccidentTypes.clear()
        model.visibleAccidentTypes.addAll(accidentTypes.filterNotNull())
    }

    //OBSERVERS

    private fun markerFromNotificationObserver(status: String?) {
        when (status) {
            null -> {
                context.showToast(R.string.marker_notification_unavailable, Toast.LENGTH_SHORT)
            }
            else -> {
                if (model.mapMarkers[status] != null) {
                    displayMarkerDetails(model.mapMarkers[status]!!)
                }
            }
        }
    }

    private fun addMarkerToMapObserver(marker: MarkerData) {
        if (model.visibleSeverities.contains(marker.properties.severityType) && model.visibleAccidentTypes.contains(
                marker.properties.accidentType
            )
        ) {
            val mapsMarker = view.addMarker(marker)
            mapsMarker.tag = marker
            model.mapMarkers[marker.id] = mapsMarker
        }
    }

    private fun removeMarkerFromMapObserver(markerId: String) {
        model.mapMarkers[markerId]?.remove()
        model.mapMarkers.remove(markerId)
    }

    private fun updateMarkerOnMapObserver(marker: MarkerData) {
        val mapMarker = model.mapMarkers[marker.id]

        if ((mapMarker?.tag as MarkerData).properties == marker.properties) {
            return
        } else {
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

    private fun severityFilterObserver(severityType: SeverityType) {
        val sharedPreferences = context.getSharedPreferences("preferences", Context.MODE_PRIVATE)

        when (model.visibleSeverities.contains(severityType)) {
            true -> {
                sharedPreferences.edit().putBoolean("severityType.${severityType.name}", true)
                    .apply()
                model.visibleSeverities.remove(severityType)
                model.markerDatabase.getBySeverity(severityType)
                    .observe(context.getLifecycleOwner(),
                        Observer { markers ->
                            markers.forEach { marker ->
                                model.removeMarkerFromMap.value = marker.id
                                model.markerDatabase.getBySeverity(severityType)
                                    .removeObservers(context.getLifecycleOwner())
                            }
                        })
            }
            false -> {
                sharedPreferences.edit().putBoolean("severityType.${severityType.name}", false)
                    .apply()
                model.visibleSeverities.add(severityType)
                model.markerDatabase.getBySeverity(severityType)
                    .observe(context.getLifecycleOwner(),
                        Observer { markers ->
                            markers.forEach { marker ->
                                model.addMarkerToMap.value = marker
                                model.markerDatabase.getBySeverity(severityType)
                                    .removeObservers(context.getLifecycleOwner())
                            }
                        })
            }
        }
    }

    private fun accidentFilterObserver(accidentType: AccidentType) {
        val sharedPreferences = context.getSharedPreferences("preferences", Context.MODE_PRIVATE)

        when (model.visibleAccidentTypes.contains(accidentType)) {
            true -> {
                sharedPreferences.edit().putBoolean("accidentType.${accidentType.name}", true)
                    .apply()
                model.visibleAccidentTypes.remove(accidentType)
                model.markerDatabase.getByAccidentType(accidentType)
                    .observe(context.getLifecycleOwner(),
                        Observer { markers ->
                            markers.forEach { marker ->
                                model.removeMarkerFromMap.value = marker.id
                                model.markerDatabase.getByAccidentType(accidentType)
                                    .removeObservers(context.getLifecycleOwner())
                            }
                        })
            }
            false -> {
                sharedPreferences.edit().putBoolean("accidentType.${accidentType.name}", false)
                    .apply()
                model.visibleAccidentTypes.add(accidentType)
                model.markerDatabase.getByAccidentType(accidentType)
                    .observe(context.getLifecycleOwner(),
                        Observer { markers ->
                            markers.forEach { marker ->
                                model.addMarkerToMap.value = marker
                                model.markerDatabase.getByAccidentType(accidentType)
                                    .removeObservers(context.getLifecycleOwner())
                            }
                        })
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
}