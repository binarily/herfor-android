package pl.herfor.android.activities

import android.Manifest
import android.annotation.SuppressLint
import android.graphics.drawable.TransitionDrawable
import android.os.Bundle
import android.text.format.DateUtils
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.chip.Chip
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
import pl.herfor.android.contexts.MarkerContext
import pl.herfor.android.contracts.MarkerContract
import pl.herfor.android.objects.*
import pl.herfor.android.presenters.MarkerViewPresenter
import pl.herfor.android.utils.toColor
import pl.herfor.android.utils.toHumanReadableString
import pl.herfor.android.utils.toPoint
import pl.herfor.android.viewmodels.MarkerViewModel
import kotlin.concurrent.thread


class MapsActivity : AppCompatActivity(), MarkerContract.View {
    private lateinit var mMap: GoogleMap
    private lateinit var activityView: View
    private lateinit var detailsSheet: BottomSheetBehavior<ConstraintLayout>
    private lateinit var addSheet: BottomSheetBehavior<ConstraintLayout>
    private lateinit var snackbar: Snackbar
    private lateinit var presenter: MarkerContract.Presenter

    private var buttonState = RightButtonMode.DISABLED

    private val ZOOM_LEVEL = 15.0F
    private val BUTTON_ANIMATION_DURATION = 200

    //TODO: internationalization
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        activityView = findViewById(R.id.activity_main)

        presenter = MarkerViewPresenter(ViewModelProvider(this)[MarkerViewModel::class.java], this, MarkerContext(this))

        detailsSheet = BottomSheetBehavior.from(details_sheet)
        detailsSheet.state = BottomSheetBehavior.STATE_HIDDEN

        addSheet = BottomSheetBehavior.from(add_sheet)
        addSheet.state = BottomSheetBehavior.STATE_HIDDEN

        submitMarkerButton.setOnClickListener { prepareMarkerForSubmission() }
        addChipGroup.setOnCheckedChangeListener { _, checkedId -> submitMarkerButton.isEnabled = checkedId != -1 }

        snackbar = Snackbar.make(activityView, "We have lost access to the internet.", Snackbar.LENGTH_INDEFINITE)
            .setAction("Reload") { handleIdleMap() }


        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync { map -> onMapReady(map) }
    }

    private fun prepareMarkerForSubmission() {
        val markerProperties = MarkerProperties(AccidentType.values()[(addChipGroup.checkedChipId - 1) % 6])
        presenter.submitMarker(markerProperties)
    }

    private fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        mMap.uiSettings.isCompassEnabled = false
        mMap.uiSettings.isMapToolbarEnabled = false
        mMap.uiSettings.isMyLocationButtonEnabled = false

        mMap.setOnMarkerClickListener {
            presenter.displayMarkerDetails(it)
            return@setOnMarkerClickListener true
        }
        mMap.setOnMapClickListener {
            dismissSheet()
        }
        mMap.setOnMyLocationClickListener { presenter.displayMarkerAdd() }
        mMap.setOnCameraIdleListener(::handleIdleMap)

        presenter.start()
        presenter.askForLocationPermission()
    }

    private fun showSheet(sheetVisibility: SheetVisibility) {
        when (sheetVisibility) {
            SheetVisibility.DETAILS_SHEET -> {
                detailsSheet.state = BottomSheetBehavior.STATE_COLLAPSED
                addSheet.state = BottomSheetBehavior.STATE_HIDDEN
            }
            SheetVisibility.ADD_SHEET -> {
                detailsSheet.state = BottomSheetBehavior.STATE_HIDDEN
                addSheet.state = BottomSheetBehavior.STATE_COLLAPSED
            }
            SheetVisibility.NONE -> {
                detailsSheet.state = BottomSheetBehavior.STATE_HIDDEN
                addSheet.state = BottomSheetBehavior.STATE_HIDDEN
            }
        }
    }

    override fun dismissSheet() {
        showSheet(SheetVisibility.NONE)
    }

    override fun setRightButton(rightButtonMode: RightButtonMode, transition: Boolean) {
        when (rightButtonMode) {
            RightButtonMode.ADD_MARKER -> {
                addButton.backgroundTintList = ContextCompat.getColorStateList(this, R.color.colorSecondary)
                if (transition) {
                    val animationDrawable = R.drawable.location_to_add
                    addButton.setImageDrawable(ContextCompat.getDrawable(this, animationDrawable))
                    (addButton.drawable as TransitionDrawable).isCrossFadeEnabled = true
                    (addButton.drawable as TransitionDrawable).startTransition(BUTTON_ANIMATION_DURATION)
                    addButton.setOnClickListener { presenter.displayMarkerAdd() }
                }
            }
            RightButtonMode.SHOW_LOCATION -> {
                addButton.backgroundTintList = ContextCompat.getColorStateList(this, R.color.colorSecondary)
                if (transition) {
                    val animationDrawable = R.drawable.add_to_location
                    addButton.setImageDrawable(ContextCompat.getDrawable(this, animationDrawable))
                    (addButton.drawable as TransitionDrawable).isCrossFadeEnabled = true
                    (addButton.drawable as TransitionDrawable).startTransition(BUTTON_ANIMATION_DURATION)
                    addButton.setOnClickListener { presenter.zoomToCurrentLocation() }
                }
            }
            RightButtonMode.DISABLED -> {
                addButton.backgroundTintList = ContextCompat.getColorStateList(this, R.color.colorSecondaryTranslucent)
                addButton.setOnClickListener { presenter.askForLocationPermission() }
            }
        }
        buttonState = rightButtonMode
    }

    override fun moveCamera(position: LatLng, animate: Boolean) {
        if (animate) {
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(position, ZOOM_LEVEL))
        } else {
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(position, ZOOM_LEVEL))
        }
        presenter.setRightButtonMode(mMap.projection.visibleRegion.latLngBounds)
    }

    //TODO: replace with data binding
    override fun showDetailsSheet(markerData: MarkerData) {
        runOnUiThread {
            detailsTypeChip.text = markerData.properties.accidentType.toHumanReadableString()
            detailsSeverityChip.text = markerData.properties.severityType.toHumanReadableString()
            detailsSeverityChip.chipBackgroundColor = markerData.properties.severityType.toColor(this)
            detailsTimeTextView.text = "Dodano ${DateUtils.getRelativeTimeSpanString(
                markerData.properties.creationDate.time,
                System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS, DateUtils.FORMAT_ABBREV_RELATIVE
            ).toString().toLowerCase()}"

            //This will be filled in showLocationOnDetailsSheet()
            detailsPlaceTextView.text = "..."
            showSheet(SheetVisibility.DETAILS_SHEET)
        }
    }

    override fun showLocationOnDetailsSheet(location: String) {
        runOnUiThread {
            detailsPlaceTextView.text = location
        }
    }

    override fun showAddSheet() {
        addChipGroup.clearCheck()
        showSheet(SheetVisibility.ADD_SHEET)
    }

    override fun getPermissionForLocation() {
        val snackBarListener = SnackbarOnDeniedPermissionListener.Builder
            .with(activityView, "Location is needed to report incidents and see location on the map.")
            .withOpenSettingsButton("Settings")
            .build()
        val overallListener = object : PermissionListener {
            override fun onPermissionGranted(response: PermissionGrantedResponse) {
                presenter.handleLocationBeingEnabled()
            }

            override fun onPermissionDenied(response: PermissionDeniedResponse) {
                presenter.handleLocationBeingDisabled()
            }

            override fun onPermissionRationaleShouldBeShown(permission: PermissionRequest, token: PermissionToken) {
                presenter.handleLocationBeingDisabled()
            }
        }
        Dexter.withActivity(this)
            .withPermission(Manifest.permission.ACCESS_FINE_LOCATION)
            .withListener(CompositePermissionListener(snackBarListener, overallListener))
            .check()
    }

    private fun handleIdleMap() {
        val bounds = mMap.projection.visibleRegion.latLngBounds
        thread {
            presenter.setRightButtonMode(bounds)
            presenter.loadVisibleMarkers(bounds.northeast.toPoint(), bounds.southwest.toPoint())
        }
    }

    override fun changeRightButtonState(rightButtonMode: RightButtonMode, transition: Boolean) {
        setRightButton(rightButtonMode, transition)
    }

    override fun showSubmitMarkerFailure() {
        Toast.makeText(this, "Nie udało się wysłać zgłoszenia. Spróbuj ponownie", Toast.LENGTH_SHORT).show()
        submitMarkerButton.isEnabled = true
    }

    override fun showConnectionError() {
        snackbar.show()
    }

    override fun dismissConnectionError() {
        snackbar.dismiss()
    }

    @SuppressLint("MissingPermission")
    override fun setLocationStateForMap(state: Boolean) {
        mMap.isMyLocationEnabled = state
        if (state) {
            presenter.setRightButtonMode(mMap.projection.visibleRegion.latLngBounds)
        }
    }

    override fun dismissAddSheet() {
        submitMarkerButton.isEnabled = true
        showSheet(SheetVisibility.NONE)
    }

    override fun addMarker(marker: MarkerData): Marker {
        val mapsMarker = mMap.addMarker(
            MarkerOptions()
                .position(marker.location.toLatLng())
                .icon(BitmapDescriptorFactory.fromResource(marker.properties.getGlyph()))
                .anchor(0.5F, 0.5F)
        )
        mapsMarker.tag = marker
        return mapsMarker
    }

    override fun removeMarker(marker: Marker) {
        marker.remove()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt("chipId", addChipGroup.checkedChipId)
        outState.putString("rightButtonState", buttonState.toString())
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle?) {
        super.onRestoreInstanceState(savedInstanceState)
        addChipGroup.check(savedInstanceState?.getInt("chipId") ?: Chip.NO_ID)
        setRightButton(RightButtonMode.valueOf(savedInstanceState?.getString("rightButtonState") ?: "DISABLED"), true)
    }

    override fun onDestroy() {
        super.onDestroy()
        presenter.stop()
    }
}
