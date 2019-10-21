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
        fun getPermissionForLocation()
        fun setLocationStateForMap(state: Boolean)
        fun showAddSheet()
        fun dismissAddSheet()
        fun showDetailsSheet(marker: MarkerData)
        fun showLocationOnDetailsSheet(location: String)
        fun moveCamera(position: LatLng, animate: Boolean)
        fun setRightButton(rightButtonMode: RightButtonMode, transition: Boolean)

        fun addMarkerToMap(marker: MarkerData): Marker
    }

    interface Presenter {
        fun start()
        fun stop()

        fun displayMarkerAdd()
        fun submitMarker(markerProperties: MarkerProperties)
        fun submitGrade(grade: Grade)
        fun loadMarkersToMap(northEast: Point, southWest: Point)
        fun handleLocationBeingEnabled()
        fun handleLocationBeingDisabled()
        fun zoomToCurrentLocation()
        fun showCurrentLocation()
        fun setRightButtonMode(bounds: LatLngBounds)

        fun seekPermissions(checkLocation: Boolean)

        fun displayMarkerFromNotifications(id: String?)
    }
}