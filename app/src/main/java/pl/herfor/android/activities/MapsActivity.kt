package pl.herfor.android.activities

import android.Manifest
import android.annotation.SuppressLint
import android.content.res.ColorStateList
import android.graphics.drawable.TransitionDrawable
import android.location.Location
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.karumi.dexter.Dexter
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionDeniedResponse
import com.karumi.dexter.listener.PermissionGrantedResponse
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.single.CompositePermissionListener
import com.karumi.dexter.listener.single.PermissionListener
import com.karumi.dexter.listener.single.SnackbarOnDeniedPermissionListener
import kotlinx.android.synthetic.main.activity_maps.*
import kotlinx.android.synthetic.main.activity_sheet.*
import pl.herfor.android.R
import pl.herfor.android.objects.Marker
import pl.herfor.android.objects.MarkersLookupRequest
import pl.herfor.android.objects.Point
import pl.herfor.android.viewmodels.MarkerViewModel


class MapsActivity : AppCompatActivity(), OnMapReadyCallback, GoogleMap.OnCameraIdleListener {

    private lateinit var mMap: GoogleMap
    private lateinit var activityView: View
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var bottomSheetBehavior: BottomSheetBehavior<ConstraintLayout>

    private val zoomLevel = 15.0F
    private val buttonAnimationDuration = 200

    private var locationEnabled = false
    private var insideLocationArea = true

    private val markers = HashMap<String, com.google.android.gms.maps.model.Marker>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        activityView = findViewById(android.R.id.content)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        bottomSheetBehavior = BottomSheetBehavior.from(bottom_sheet)
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
        bottomSheetBehavior.isFitToContents = true

        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        mMap.uiSettings.isCompassEnabled = false
        mMap.uiSettings.isMapToolbarEnabled = false
        mMap.uiSettings.isMyLocationButtonEnabled = false
        mMap.setOnMarkerClickListener {
            bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
            marker_name.text = (it?.tag as Marker).constructTitle()
            return@setOnMarkerClickListener true
        }
        mMap.setOnMapClickListener {
            bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
        }

        val model = ViewModelProviders.of(this).get(MarkerViewModel::class.java)
        model.getMarker().observe(this, Observer<Marker> { marker ->
            val mapsMarker = mMap.addMarker(
                MarkerOptions()
                    .position(marker.location.toLatLng())
                    .icon(BitmapDescriptorFactory.fromResource(marker.properties.getGlyph()))
                    .anchor(0.5F, 0.5F)
            )
            mapsMarker.tag = marker
            markers[marker.id!!] = mapsMarker
        })
        mMap.setOnCameraIdleListener(this)
        enableLocation()
    }

    private fun enableLocation() {
        val snackBarListener = SnackbarOnDeniedPermissionListener.Builder
            .with(activityView, "Location is needed to report incidents and see location on the map.")
            .withOpenSettingsButton("Settings")
            .build()
        @SuppressLint("MissingPermission")
        val overallListener = object : PermissionListener {
            override fun onPermissionGranted(response: PermissionGrantedResponse) {
                locationEnabled = true
                mMap.isMyLocationEnabled = true
                mMap.setOnMyLocationClickListener { location: Location ->
                    addButton.isEnabled = true
                    //For triggering adding a marker?
                }
                zoomToCurrentLocation()
            }

            override fun onPermissionDenied(response: PermissionDeniedResponse) {
                locationEnabled = false
                mMap.isMyLocationEnabled = false
            }

            override fun onPermissionRationaleShouldBeShown(permission: PermissionRequest, token: PermissionToken) {
                locationEnabled = false
                mMap.isMyLocationEnabled = false
            }
        }
        Dexter.withActivity(this)
            .withPermission(Manifest.permission.ACCESS_FINE_LOCATION)
            .withListener(CompositePermissionListener(snackBarListener, overallListener))
            .check()
    }

    @SuppressLint("MissingPermission")
    override fun onCameraIdle() {
        if (locationEnabled) {
            fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
                if (location != null) {
                    val currentLocation = location.toLatLng()
                    if (mMap.projection.visibleRegion.latLngBounds.contains(currentLocation)) {
                        if (!insideLocationArea) {
                            insideLocationArea = true
                            addButton.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.location_to_add))
                            (addButton.drawable as TransitionDrawable).isCrossFadeEnabled = true
                            (addButton.drawable as TransitionDrawable).startTransition(buttonAnimationDuration)
                        }
                    } else {
                        if (insideLocationArea) {
                            insideLocationArea = false
                            addButton.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.add_to_location))
                            (addButton.drawable as TransitionDrawable).isCrossFadeEnabled = true
                            (addButton.drawable as TransitionDrawable).startTransition(buttonAnimationDuration)
                            addButton.setOnClickListener { zoomToCurrentLocation(animate = true) }
                        }
                    }
                    addButton.backgroundTintList =
                        ColorStateList.valueOf(ContextCompat.getColor(this, R.color.colorAccent))
                }
            }
        } else {
            addButton.backgroundTintList =
                ColorStateList.valueOf(ContextCompat.getColor(this, R.color.colorAccentTranslucent))
        }
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

    private fun Location.toLatLng(): LatLng {
        return LatLng(this.latitude, this.longitude)
    }

    @SuppressLint("MissingPermission")
    private fun zoomToCurrentLocation(animate: Boolean = false) {
        fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
            if (location != null) {
                if (animate) {
                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(location.toLatLng(), zoomLevel))
                } else {
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(location.toLatLng(), zoomLevel))
                }
            }
        }
    }
}
