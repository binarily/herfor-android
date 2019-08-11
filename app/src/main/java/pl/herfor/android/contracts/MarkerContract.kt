package pl.herfor.android.contracts

import android.location.Geocoder
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.Marker
import pl.herfor.android.objects.MarkerData
import pl.herfor.android.objects.MarkerProperties
import pl.herfor.android.objects.Point
import pl.herfor.android.objects.RightButtonMode

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
    }

    interface Presenter {
        fun start()
        fun stop()

        fun displayMarkerDetails(marker: Marker)
        fun displayMarkerAdd()
        fun submitMarker(markerProperties: MarkerProperties)
        fun loadVisibleMarkers(northWest: Point, southEast: Point)
        fun handleLocationBeingEnabled()
        fun handleLocationBeingDisabled()
        fun zoomToCurrentLocation()
        fun showCurrentLocation()
        fun askForLocationPermission()
        fun setRightButtonMode(bounds: LatLngBounds)
    }

    interface Context {
        fun getLocationProvider(): FusedLocationProviderClient
        fun getGeocoder(): Geocoder
        fun getAppContext(): AppCompatActivity
    }
}