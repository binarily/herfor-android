package pl.herfor.android.activities

import android.Manifest
import android.annotation.SuppressLint
import android.graphics.drawable.TransitionDrawable
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
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
import com.jakewharton.threetenabp.AndroidThreeTen
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
import pl.herfor.android.interfaces.MarkerContract
import pl.herfor.android.objects.MarkerData
import pl.herfor.android.objects.MarkerProperties
import pl.herfor.android.objects.enums.Accident
import pl.herfor.android.objects.enums.Grade
import pl.herfor.android.objects.enums.RightButtonMode
import pl.herfor.android.objects.enums.SheetVisibility
import pl.herfor.android.presenters.MarkerViewPresenter
import pl.herfor.android.retrofits.RetrofitRepository
import pl.herfor.android.utils.Constants
import pl.herfor.android.utils.Constants.Companion.BUTTON_ANIMATION_DURATION
import pl.herfor.android.utils.Constants.Companion.CHIP_ID_KEY
import pl.herfor.android.utils.Constants.Companion.RIGHT_BUTTON_STATE_KEY
import pl.herfor.android.utils.Constants.Companion.ZOOM_LEVEL
import pl.herfor.android.utils.toPoint
import pl.herfor.android.utils.toRelativeDateString
import pl.herfor.android.viewmodels.MarkerViewModel
import kotlin.concurrent.thread


class MapsActivity : AppCompatActivity(), MarkerContract.View {
    private lateinit var mMap: GoogleMap
    private lateinit var activityView: View
    private lateinit var detailsSheet: BottomSheetBehavior<ConstraintLayout>
    private lateinit var addSheet: BottomSheetBehavior<ConstraintLayout>
    private lateinit var snackbar: Snackbar
    private lateinit var presenter: MarkerContract.Presenter
    private lateinit var model: MarkerViewModel
    private lateinit var context: MarkerContext
    private lateinit var filterSheet: FilterSheetFragment

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AndroidThreeTen.init(this)

        setContentView(R.layout.activity_main)
        activityView = findViewById(R.id.activity_main)
        context = MarkerContext(this)

        model = ViewModelProvider(this)[MarkerViewModel::class.java]
        presenter = MarkerViewPresenter(
            model,
            this,
            MarkerContext(this),
            RetrofitRepository(model)
        )

        model.currentlyShownMarker.observe(this, Observer { marker -> handleShowMarker(marker) })
        model.currentlyShownGrade.observe(this, Observer { grade -> handleGrade(grade) })

        detailsSheet = BottomSheetBehavior.from(details_sheet)
        detailsSheet.state = BottomSheetBehavior.STATE_HIDDEN

        addSheet = BottomSheetBehavior.from(add_sheet)
        addSheet.state = BottomSheetBehavior.STATE_HIDDEN

        addChipGroup.setOnCheckedChangeListener { _, checkedId ->
            submitMarkerButton.isEnabled = checkedId != -1
        }

        snackbar = Snackbar.make(
            activityView,
            getString(R.string.connection_loss_explanation),
            Snackbar.LENGTH_INDEFINITE
        )

        filterSheet = FilterSheetFragment(model)

        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync { map -> onMapReady(map) }

    }

    override fun setRightButton(rightButtonMode: RightButtonMode, transition: Boolean) {
        model.buttonState = rightButtonMode
        when (rightButtonMode) {
            RightButtonMode.ADD_MARKER -> {
                addButton.alpha = 1f
                if (transition) {
                    val animationDrawable = R.drawable.location_to_add
                    addButton.setImageDrawable(ContextCompat.getDrawable(this, animationDrawable))
                    (addButton.drawable as TransitionDrawable).isCrossFadeEnabled = true
                    (addButton.drawable as TransitionDrawable).startTransition(
                        BUTTON_ANIMATION_DURATION
                    )
                }
            }
            RightButtonMode.SHOW_LOCATION -> {
                addButton.alpha = 1f
                if (transition) {
                    val animationDrawable = R.drawable.add_to_location
                    addButton.setImageDrawable(ContextCompat.getDrawable(this, animationDrawable))
                    (addButton.drawable as TransitionDrawable).isCrossFadeEnabled = true
                    (addButton.drawable as TransitionDrawable).startTransition(
                        BUTTON_ANIMATION_DURATION
                    )
                }
            }
            RightButtonMode.DISABLED -> {
                addButton.alpha = 0.8f
            }
        }
    }

    override fun moveCamera(position: LatLng, animate: Boolean) {
        if (animate) {
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(position, ZOOM_LEVEL))
        } else {
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(position, ZOOM_LEVEL))
        }
        presenter.setRightButtonMode(mMap.projection.visibleRegion.latLngBounds)
    }

    override fun showDetailsSheet(marker: MarkerData) {
        runOnUiThread {
            detailsTypeChip.text = marker.properties.accident.toHumanReadableString(this)
            detailsSeverityChip.text =
                marker.properties.severity.toHumanReadableString(this)
            detailsSeverityChip.chipBackgroundColor = marker.properties.severity.toColor(this)
            detailsTimeTextView.text =
                getString(R.string.time_details_description).toRelativeDateString(marker.properties.creationDate)

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
            .with(activityView, getString(R.string.location_explanation))
            .withOpenSettingsButton(getString(R.string.settings_button))
            .build()
        val overallListener = object : PermissionListener {
            override fun onPermissionGranted(response: PermissionGrantedResponse) {
                presenter.handleLocationBeingEnabled()
            }

            override fun onPermissionDenied(response: PermissionDeniedResponse) {
                presenter.handleLocationBeingDisabled()
            }

            override fun onPermissionRationaleShouldBeShown(
                permission: PermissionRequest,
                token: PermissionToken
            ) {
                presenter.handleLocationBeingDisabled()
            }
        }
        Dexter.withActivity(this)
            .withPermission(Manifest.permission.ACCESS_FINE_LOCATION)
            .withListener(CompositePermissionListener(snackBarListener, overallListener))
            .check()
    }

    override fun showSubmitMarkerFailure() {
        Toast.makeText(this, getString(R.string.marker_submit_error), Toast.LENGTH_SHORT).show()
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
    }

    override fun dismissAddSheet() {
        submitMarkerButton.isEnabled = false
        addChipGroup.clearCheck()
        showSheet(SheetVisibility.NONE)
    }

    override fun addMarkerToMap(marker: MarkerData): Marker {
        val mapsMarker = mMap.addMarker(
            MarkerOptions()
                .position(marker.location.toLatLng())
                .icon(BitmapDescriptorFactory.fromResource(marker.properties.getGlyph()))
                .anchor(0.5F, 0.5F)
        )
        mapsMarker.tag = marker
        return mapsMarker
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(CHIP_ID_KEY, addChipGroup.checkedChipId)
        outState.putString(RIGHT_BUTTON_STATE_KEY, model.buttonState.toString())
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle?) {
        super.onRestoreInstanceState(savedInstanceState)
        addChipGroup.check(savedInstanceState?.getInt(CHIP_ID_KEY) ?: Chip.NO_ID)
        setRightButton(
            RightButtonMode.valueOf(
                (savedInstanceState?.getString(RIGHT_BUTTON_STATE_KEY)
                    ?: RightButtonMode.DISABLED).toString()
            ), true
        )
    }

    override fun onResume() {
        super.onResume()
        presenter.seekPermissions(false)
    }

    override fun onDestroy() {
        super.onDestroy()
        presenter.stop()
        model.currentlyShownMarker.removeObservers(this)
    }

    //Private functions
    private fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        mMap.uiSettings.isCompassEnabled = false
        mMap.uiSettings.isMapToolbarEnabled = false
        mMap.uiSettings.isMyLocationButtonEnabled = false

        mMap.setOnMarkerClickListener {
            model.currentlyShownMarker.value = (it.tag as MarkerData)
            return@setOnMarkerClickListener true
        }
        mMap.setOnMapClickListener {
            showSheet(SheetVisibility.NONE)
        }
        mMap.setOnMyLocationClickListener { presenter.displayMarkerAdd() }
        mMap.setOnCameraIdleListener(::handleIdleMap)

        presenter.start()
        presenter.seekPermissions(true)

        if (intent.extras != null && intent.extras.containsKey(Constants.INTENT_MARKER_ID_KEY)) {
            intent?.extras?.remove(Constants.INTENT_MARKER_ID_KEY)
            presenter.displayMarkerFromNotifications(intent?.extras?.getString(Constants.INTENT_MARKER_ID_KEY))
        }
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

    private fun handleIdleMap() {
        val bounds = mMap.projection.visibleRegion.latLngBounds
        presenter.setRightButtonMode(bounds)
        presenter.loadMarkersToMap(bounds.northeast.toPoint(), bounds.southwest.toPoint())
    }

    //Button functions
    fun onLeftButton(view: View) {
        filterSheet.show(supportFragmentManager, filterSheet.tag)
    }

    fun onRightButton(view: View) {
        when (model.buttonState) {
            RightButtonMode.ADD_MARKER -> presenter.displayMarkerAdd()
            RightButtonMode.SHOW_LOCATION -> presenter.zoomToCurrentLocation()
            RightButtonMode.DISABLED -> presenter.seekPermissions(true)
        }
    }

    fun onRelevantButton(view: View) {
        presenter.submitGrade(Grade.RELEVANT)
    }

    fun onIrrelevantButton(view: View) {
        presenter.submitGrade(Grade.NOT_RELEVANT)
    }

    fun onMarkerAddButton(view: View) {
        submitMarkerButton.isEnabled = false
        val markerProperties =
            MarkerProperties(Accident.values()[(addChipGroup.checkedChipId - 1) % 6])
        presenter.submitMarker(markerProperties)
    }

    //Observers
    private fun handleShowMarker(marker: MarkerData) {
        moveCamera(marker.location.toLatLng(), animate = true)
        showDetailsSheet(marker)
        val position = marker.location
        thread {
            showLocationOnDetailsSheet(
                context.getLocationName(
                    position.latitude,
                    position.longitude
                )
            )
        }
    }

    private fun handleGrade(grade: Grade) {
        when (grade) {
            Grade.UNGRADED -> {
                relevantGradeButton.isEnabled = true
                irrelevantGradeButton.isEnabled = true
            }
            Grade.RELEVANT -> {
                relevantGradeButton.isEnabled = true
                irrelevantGradeButton.isEnabled = false
            }
            Grade.NOT_RELEVANT -> {
                relevantGradeButton.isEnabled = false
                irrelevantGradeButton.isEnabled = true
            }
        }
    }

}
