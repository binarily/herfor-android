package pl.herfor.android.presenters

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.location.Location
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.Marker
import pl.herfor.android.contracts.MarkerContract
import pl.herfor.android.objects.*
import pl.herfor.android.utils.toLatLng
import pl.herfor.android.utils.toPoint
import pl.herfor.android.viewmodels.MarkerViewModel
import kotlin.concurrent.thread

class MarkerViewPresenter(
    private val model: MarkerViewModel, private val view: MarkerContract.View,
    private val context: MarkerContract.Context
) : MarkerContract.Presenter {
    override fun start() {
        model.newMarkerObservable.observe(context.getAppContext(), Observer<MarkerData> { marker ->
            if (marker.id != null) {
                val mapsMarker = view.addMarker(marker)
                mapsMarker.tag = marker
                model.mapMarkers[marker.id!!] = mapsMarker
            }
        })
        model.removeMarkerObservable.observe(context.getAppContext(), Observer<MarkerData> { marker ->
            model.mapMarkers[marker.id]?.remove()
        })
        model.addMarkerStatusObservable.observe(context.getAppContext(), Observer<Boolean> { status ->
            when (status) {
                true -> {
                    view.dismissAddSheet()
                }
                false -> {
                    view.showSubmitMarkerFailure()
                }
            }
        })
        model.connectionStatusObservable.observe(context.getAppContext(), Observer<Boolean> { status ->
            when (status) {
                true -> {
                    view.dismissConnectionError()
                }
                false -> {
                    view.showConnectionError()
                    model.connectionStatusObservable.value = null
                }
            }
        })
    }

    override fun stop() {
        model.newMarkerObservable.removeObservers(context.getAppContext())
        model.removeMarkerObservable.removeObservers(context.getAppContext())
        model.addMarkerStatusObservable.removeObservers(context.getAppContext())
        model.connectionStatusObservable.removeObservers(context.getAppContext())
    }

    override fun displayMarkerDetails(marker: Marker) {
        view.showDetailsSheet(marker.tag as MarkerData)
        val position = marker.position
        thread {
            val geoCoder = context.getGeocoder()
            val matches = geoCoder.getFromLocation(position.latitude, position.longitude, 1)
            val bestMatch = if (matches.isEmpty()) null else matches[0]
            view.showLocationOnDetailsSheet(bestMatch?.thoroughfare ?: "Unknown location")
        }

    }

    @SuppressLint("MissingPermission")
    override fun submitMarker(markerProperties: MarkerProperties) {
        if (permissionCheck()) {
            context.getLocationProvider().lastLocation.addOnSuccessListener { location: Location? ->
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
        }
    }

    override fun loadVisibleMarkers(northEast: Point, southWest: Point) {
        val request = MarkersLookupRequest(northEast, southWest, null)
        model.loadAllVisibleMarkers(request)
    }

    override fun handleLocationBeingEnabled() {
        model.locationEnabled = true
        view.setLocationStateForMap(true)
        view.setRightButton(RightButtonMode.ADD_MARKER, false)
        showCurrentLocation()
    }

    override fun handleLocationBeingDisabled() {
        model.locationEnabled = false
        view.setLocationStateForMap(false)
    }

    @SuppressLint("MissingPermission")
    override fun zoomToCurrentLocation() {
        if (permissionCheck()) {
            context.getLocationProvider().lastLocation.addOnSuccessListener { location: Location? ->
                if (location != null) {
                    view.moveCamera(location.toLatLng(), true)
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    override fun showCurrentLocation() {
        if (permissionCheck()) {
            context.getLocationProvider().lastLocation.addOnSuccessListener { location: Location? ->
                if (location != null) {
                    view.moveCamera(location.toLatLng(), false)
                }
            }
        }
    }

    override fun displayMarkerAdd() {
        showCurrentLocation()
        view.showAddSheet()
    }

    override fun askForLocationPermission() {
        view.getPermissionForLocation()
    }

    @SuppressLint("MissingPermission")
    override fun setRightButtonMode(bounds: LatLngBounds) {
        if (permissionCheck()) {
            context.getLocationProvider().lastLocation.addOnSuccessListener { location: Location? ->
                if (location != null) {
                    val transition = bounds.contains(location.toLatLng()) xor model.insideLocationArea
                    if (transition) {
                        model.insideLocationArea = !model.insideLocationArea
                    }
                    val buttonMode =
                        if (model.insideLocationArea) RightButtonMode.ADD_MARKER else RightButtonMode.SHOW_LOCATION
                    view.setRightButton(buttonMode, transition)
                }
            }
        } else {
            view.setRightButton(RightButtonMode.DISABLED, false)
        }
    }

    private fun permissionCheck(): Boolean {
        if (ContextCompat.checkSelfPermission(context.getAppContext(), Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            askForLocationPermission()
            return false
        }
        return true
    }
}