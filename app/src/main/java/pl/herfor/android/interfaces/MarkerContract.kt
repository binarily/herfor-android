package pl.herfor.android.interfaces

import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.Marker
import pl.herfor.android.objects.*

interface MarkerContract {
    interface View {
        fun showSubmitMarkerFailure()
        fun showConnectionError()
        fun dismissConnectionError()
        fun changeRightButtonState(rightButtonMode: RightButtonMode, transition: Boolean)
        fun getPermissionForLocation()
        fun setLocationStateForMap(state: Boolean)
        fun showAddSheet()
        fun showDetailsSheet(marker: MarkerData)
        fun showLocationOnDetailsSheet(location: String)
        fun dismissSheet()
        fun dismissAddSheet()
        fun moveCamera(position: LatLng, animate: Boolean)
        fun setRightButton(rightButtonMode: RightButtonMode, transition: Boolean)

        fun addMarker(marker: MarkerData): Marker
        fun removeMarker(marker: Marker)
        fun removeAllMarkers()

        fun setSeverityTypeFilters(severityTypes: List<SeverityType>)
        fun setAccidentTypeFilters(accidentTypes: List<AccidentType>)
    }

    interface Presenter {
        fun start()
        fun stop()

        fun displayMarkerDetails(marker: Marker)
        fun displayMarkerAdd()
        fun submitMarker(markerProperties: MarkerProperties)
        fun loadVisibleMarkers(northEast: Point, southWest: Point)
        fun handleLocationBeingEnabled()
        fun handleLocationBeingDisabled()
        fun zoomToCurrentLocation()
        fun showCurrentLocation()
        fun setRightButtonMode(bounds: LatLngBounds)

        fun askForLocationPermission()
        fun checkForPlayServices()

        fun toggleSeverityType(severityType: SeverityType)
        fun toggleAccidentType(accidentType: AccidentType)

        fun displayMarkerFromNotifications(id: String?)
    }
}