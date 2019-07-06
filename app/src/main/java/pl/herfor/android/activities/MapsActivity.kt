package pl.herfor.android.activities

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import pl.herfor.android.R
import pl.herfor.android.objects.Marker
import pl.herfor.android.objects.MarkersLookupRequest
import pl.herfor.android.objects.Point
import pl.herfor.android.viewmodels.MarkerViewModel

class MapsActivity : AppCompatActivity(), OnMapReadyCallback, GoogleMap.OnCameraIdleListener {

    private lateinit var mMap: GoogleMap

    override fun onCreate(savedInstanceState: Bundle?) {
        // Create a ViewModel the first time the system calls an activity's onCreate() method.
        // Re-created activities receive the same MyViewModel instance created by the first activity.
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
                .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        val model = ViewModelProviders.of(this).get(MarkerViewModel::class.java)
        model.getMarker().observe(this, Observer<Marker> { marker ->
            mMap.addMarker(MarkerOptions().position(marker.location.toLatLng()).title(marker.constructTitle()))
        })
        mMap.setOnCameraIdleListener(this)
    }

    override fun onCameraIdle() {
        val bounds = mMap.projection.visibleRegion.latLngBounds
        val request = MarkersLookupRequest()
        request.northEast = bounds.northeast.toPoint()
        request.southWest = bounds.southwest.toPoint()
        val model = ViewModelProviders.of(this).get(MarkerViewModel::class.java)
        model.loadMarkersVisibleOnCamera(request)
    }

    private fun LatLng.toPoint(): Point {
        return Point(latitude, longitude)
    }
}
