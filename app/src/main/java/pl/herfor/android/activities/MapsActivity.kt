package pl.herfor.android.activities

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.drawable.TransitionDrawable
import android.location.Geocoder
import android.location.Location
import android.os.Bundle
import android.text.format.DateUtils
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.snackbar.Snackbar
import com.karumi.dexter.Dexter
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionDeniedResponse
import com.karumi.dexter.listener.PermissionGrantedResponse
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.single.CompositePermissionListener
import com.karumi.dexter.listener.single.PermissionListener
import com.karumi.dexter.listener.single.SnackbarOnDeniedPermissionListener
import kotlinx.android.synthetic.main.fragment_maps.*
import kotlinx.android.synthetic.main.sheet_add.*
import kotlinx.android.synthetic.main.sheet_details.*
import pl.herfor.android.R
import pl.herfor.android.objects.AccidentType
import pl.herfor.android.objects.MarkerData
import pl.herfor.android.objects.MarkerProperties
import pl.herfor.android.objects.MarkersLookupRequest
import pl.herfor.android.utils.toColor
import pl.herfor.android.utils.toHumanReadableString
import pl.herfor.android.utils.toLatLng
import pl.herfor.android.utils.toPoint
import pl.herfor.android.viewmodels.MarkerViewModel
import kotlin.concurrent.thread


class MapsActivity : AppCompatActivity() {

    private lateinit var mMap: GoogleMap
    private lateinit var activityView: View
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var detailsSheet: BottomSheetBehavior<ConstraintLayout>
    private lateinit var addSheet: BottomSheetBehavior<ConstraintLayout>
    private lateinit var model: MarkerViewModel

    private val zoomLevel = 15.0F
    private val buttonAnimationDuration = 200

    private val markers = HashMap<String, Marker>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        activityView = findViewById(android.R.id.content)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        model = ViewModelProviders.of(this).get(MarkerViewModel::class.java)

        detailsSheet = BottomSheetBehavior.from(details_sheet)
        detailsSheet.state = BottomSheetBehavior.STATE_HIDDEN

        addSheet = BottomSheetBehavior.from(add_sheet)
        addSheet.state = BottomSheetBehavior.STATE_HIDDEN

        addButton.setOnClickListener { onRightButtonClick() }
        submitMarkerButton.setOnClickListener { submitMarker() }
        addChipGroup.setOnCheckedChangeListener { _, checkedId -> submitMarkerButton.isEnabled = checkedId != -1 }

        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync { map -> onMapReady(map) }
    }

    private fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        mMap.uiSettings.isCompassEnabled = false
        mMap.uiSettings.isMapToolbarEnabled = false
        mMap.uiSettings.isMyLocationButtonEnabled = false
        mMap.setOnMarkerClickListener {
            setUpDetailsSheet(it)
            return@setOnMarkerClickListener true
        }
        mMap.setOnMapClickListener {
            detailsSheet.state = BottomSheetBehavior.STATE_HIDDEN
            addSheet.state = BottomSheetBehavior.STATE_HIDDEN
        }

        this.setUpObservers()
        mMap.setOnCameraIdleListener(::loadVisibleMarkers)
        enableLocation(::locationEnabled, ::locationDisabled)
    }

    private fun setUpDetailsSheet(it: Marker) {
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(it.position, zoomLevel))
        val markerData = (it.tag as MarkerData)
        detailsTypeChip.text = markerData.properties.accidentType.toHumanReadableString()
        detailsSeverityChip.text = markerData.properties.severityType.toHumanReadableString()
        detailsSeverityChip.chipBackgroundColor = markerData.properties.severityType.toColor(this)
        detailsTimeTextView.text = "Dodano ${DateUtils.getRelativeTimeSpanString(
            markerData.properties.creationDate.time,
            System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS, DateUtils.FORMAT_ABBREV_RELATIVE
        ).toString().toLowerCase()}"
        addSheet.state = BottomSheetBehavior.STATE_HIDDEN
        detailsSheet.state = BottomSheetBehavior.STATE_COLLAPSED

        //This may take some while
        detailsPlaceTextView.text = "..."
        val position = it.position
        thread {
            val geoCoder = Geocoder(this)
            val matches = geoCoder.getFromLocation(position.latitude, position.longitude, 1)
            val bestMatch = if (matches.isEmpty()) null else matches[0]
            runOnUiThread {
                detailsPlaceTextView.text = bestMatch?.thoroughfare
            }
        }
    }

    private fun setUpAddSheet() {
        if (checkLocationPermission { setUpAddSheet() }) {
            zoomToCurrentLocation(true)
            addChipGroup.clearCheck()
            detailsSheet.state = BottomSheetBehavior.STATE_HIDDEN
            addSheet.state = BottomSheetBehavior.STATE_COLLAPSED
        }
    }

    private fun enableLocation(
        success: (PermissionGrantedResponse) -> Unit,
        failure: (PermissionDeniedResponse?) -> Unit = { }
    ) {
        val snackBarListener = SnackbarOnDeniedPermissionListener.Builder
            .with(activityView, "Location is needed to report incidents and see location on the map.")
            .withOpenSettingsButton("Settings")
            .build()
        @SuppressLint("MissingPermission")
        val overallListener = object : PermissionListener {
            override fun onPermissionGranted(response: PermissionGrantedResponse) {
                success(response)
            }

            override fun onPermissionDenied(response: PermissionDeniedResponse) {
                failure(response)
            }

            override fun onPermissionRationaleShouldBeShown(permission: PermissionRequest, token: PermissionToken) {
                failure(null)
            }
        }
        Dexter.withActivity(this)
            .withPermission(Manifest.permission.ACCESS_FINE_LOCATION)
            .withListener(CompositePermissionListener(snackBarListener, overallListener))
            .check()
    }

    @SuppressLint("MissingPermission")
    private fun locationEnabled(response: PermissionGrantedResponse) {
        model.locationEnabled = true
        mMap.isMyLocationEnabled = true
        mMap.setOnMyLocationClickListener { setUpAddSheet() }
        zoomToCurrentLocation()
    }

    @SuppressLint("MissingPermission")
    private fun locationDisabled(response: PermissionDeniedResponse?) {
        model.locationEnabled = false
        mMap.isMyLocationEnabled = false
    }

    private fun loadVisibleMarkers() {
        rightButtonModify()
        val bounds = mMap.projection.visibleRegion.latLngBounds
        val request = MarkersLookupRequest(bounds.northeast.toPoint(), bounds.southwest.toPoint(), null)
        model.loadAllVisibleMarkers(request)
    }

    private fun rightButtonModify() {
        if (checkLocationPermission { rightButtonModify() }) {
            fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
                if (location != null) {
                    addButton.backgroundTintList =
                        ColorStateList.valueOf(ContextCompat.getColor(this, R.color.colorSecondary))
                    addButton.setOnClickListener { onRightButtonClick() }
                    //Area type changes, but not acted on yet
                    if (mMap.projection.visibleRegion.latLngBounds.contains(location.toLatLng()) xor model.insideLocationArea) {
                        model.insideLocationArea = !model.insideLocationArea
                        val animationDrawable =
                            if (model.insideLocationArea) R.drawable.location_to_add else R.drawable.add_to_location
                        addButton.setImageDrawable(ContextCompat.getDrawable(this, animationDrawable))
                        (addButton.drawable as TransitionDrawable).isCrossFadeEnabled = true
                        (addButton.drawable as TransitionDrawable).startTransition(buttonAnimationDuration)
                    }
                }
            }
        } else {
            addButton.backgroundTintList =
                ColorStateList.valueOf(ContextCompat.getColor(this, R.color.colorSecondaryTranslucent))
            addButton.setOnClickListener { enableLocation(::locationEnabled, ::locationDisabled) }
        }
    }

    private fun onRightButtonClick() {
        if (model.insideLocationArea) {
            setUpAddSheet()
        } else {
            zoomToCurrentLocation(animate = true)
        }
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

    private fun setUpObservers() {
        var snackbar: Snackbar? = null
        model.newMarkerObservable.observe(this, Observer<MarkerData> { marker ->
            val mapsMarker = mMap.addMarker(
                MarkerOptions()
                    .position(marker.location.toLatLng())
                    .icon(BitmapDescriptorFactory.fromResource(marker.properties.getGlyph()))
                    .anchor(0.5F, 0.5F)
            )
            mapsMarker.tag = marker
            markers[marker?.id!!] = mapsMarker
        })
        model.removeMarkerObservable.observe(this, Observer<MarkerData> { marker ->
            markers[marker.id!!]?.remove()
        })
        model.addMarkerStatusObservable.observe(this, Observer<Boolean> { status ->
            when (status) {
                true -> {
                    addSheet.state = BottomSheetBehavior.STATE_HIDDEN
                    submitMarkerButton.isEnabled = true
                }
                false -> {
                    submitMarkerFailure()
                }
            }
        })
        model.connectionStatusObservable.observe(this, Observer<Boolean> { status ->
            when (status) {
                true -> {
                    snackbar?.dismiss()
                    snackbar = null
                }
                false -> {
                    snackbar =
                        Snackbar.make(activityView, "We have lost access to the internet.", Snackbar.LENGTH_INDEFINITE)
                            .setAction("Reload") { loadVisibleMarkers() }
                    snackbar!!.show()
                }
            }
        })
        //TODO: observers for connection errors, removing markers
    }

    @SuppressLint("MissingPermission")
    private fun submitMarker() {
        if (checkLocationPermission { submitMarker() }) {
            submitMarkerButton.isEnabled = false
            fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
                if (location != null) {
                    val markerProperties = MarkerProperties(AccidentType.values()[addChipGroup.checkedChipId - 1])
                    val marker = MarkerData(location.toPoint(), markerProperties)
                    model.addMarker(marker)
                } else {
                    submitMarkerFailure()
                }
            }
                .addOnFailureListener {
                    submitMarkerFailure()
                }
        }
    }

    private fun submitMarkerFailure() {
        Toast.makeText(this, "Nie udało się wysłać zgłoszenia. Spróbuj ponownie", Toast.LENGTH_SHORT).show()
        submitMarkerButton.isEnabled = true
    }

    private fun checkLocationPermission(successFunction: () -> Unit): Boolean {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            enableLocation({ successFunction() })
            return false
        }
        return true
    }
}
