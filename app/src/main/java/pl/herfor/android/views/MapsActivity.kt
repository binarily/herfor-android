package pl.herfor.android.views

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
import org.koin.android.ext.android.inject
import org.koin.android.scope.currentScope
import org.koin.android.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf
import pl.herfor.android.R
import pl.herfor.android.interfaces.AppContract
import pl.herfor.android.interfaces.ContextRepository
import pl.herfor.android.modules.LocationModule
import pl.herfor.android.objects.Report
import pl.herfor.android.objects.ReportProperties
import pl.herfor.android.objects.enums.Accident
import pl.herfor.android.objects.enums.Grade
import pl.herfor.android.objects.enums.RightButtonMode
import pl.herfor.android.objects.enums.SheetVisibility
import pl.herfor.android.utils.Constants
import pl.herfor.android.utils.Constants.Companion.BUTTON_ANIMATION_DURATION
import pl.herfor.android.utils.Constants.Companion.CHIP_ID_KEY
import pl.herfor.android.utils.Constants.Companion.RIGHT_BUTTON_STATE_KEY
import pl.herfor.android.utils.Constants.Companion.ZOOM_LEVEL
import pl.herfor.android.utils.toPoint
import pl.herfor.android.utils.toRelativeDateString
import pl.herfor.android.viewmodels.ReportViewModel
import kotlin.concurrent.thread


class MapsActivity : AppCompatActivity(), AppContract.View {
    private lateinit var mMap: GoogleMap
    private lateinit var activityView: View
    private lateinit var detailsSheet: BottomSheetBehavior<ConstraintLayout>
    private lateinit var addSheet: BottomSheetBehavior<ConstraintLayout>
    private lateinit var snackbar: Snackbar
    private val presenter: AppContract.Presenter by inject { parametersOf(this, model, context) }
    private val model: ReportViewModel by viewModel()
    private val context: ContextRepository by currentScope.inject { parametersOf(this) }
    private val location: LocationModule by inject()
    private lateinit var filterSheet: FilterSheetFragment

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AndroidThreeTen.init(this)

        setContentView(R.layout.activity_main)
        activityView = findViewById(R.id.activity_main)

        model.currentlyShownReport.observe(this, Observer { report -> handleShowReport(report) })
        model.currentlyShownGrade.observe(this, Observer { grade -> handleGrade(grade) })

        detailsSheet = BottomSheetBehavior.from(details_sheet)
        detailsSheet.state = BottomSheetBehavior.STATE_HIDDEN

        addSheet = BottomSheetBehavior.from(add_sheet)
        addSheet.state = BottomSheetBehavior.STATE_HIDDEN

        //TODO: move to separate function (maybe like with onClicks?)
        addChipGroup.setOnCheckedChangeListener { _, checkedId ->
            submitReportButton.isEnabled = checkedId != -1
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
            RightButtonMode.ADD_REPORT -> {
                addButton.alpha = 1f
                if (transition) {
                    animateAddButton(R.drawable.location_to_add)
                }
            }
            RightButtonMode.SHOW_LOCATION -> {
                addButton.alpha = 1f
                if (transition) {
                    animateAddButton(R.drawable.add_to_location)
                }
            }
            RightButtonMode.DISABLED -> {
                addButton.alpha = 0.8f
            }
        }
    }

    override fun showToast(textId: Int, length: Int) {
        runOnUiThread {
            context.showToast(textId, length)
        }
    }

    override fun moveCamera(position: LatLng, animate: Boolean) {
        runOnUiThread {
            if (animate) {
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(position, ZOOM_LEVEL))
            } else {
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(position, ZOOM_LEVEL))
            }
            presenter.setRightButtonMode(mMap.projection.visibleRegion.latLngBounds)
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

        val presenterListener = object : PermissionListener {
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
            .withListener(CompositePermissionListener(snackBarListener, presenterListener))
            .check()
    }

    override fun showSubmitReportFailure() {
        Toast.makeText(this, getString(R.string.report_submit_error), Toast.LENGTH_SHORT).show()
        submitReportButton.isEnabled = true
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
        submitReportButton.isEnabled = false
        addChipGroup.clearCheck()
        showSheet(SheetVisibility.NONE)
    }

    override fun addReportToMap(report: Report): Marker {
        val mapsReport = mMap.addMarker(
            MarkerOptions()
                .position(report.location.toLatLng())
                .icon(BitmapDescriptorFactory.fromResource(report.properties.getGlyph()))
                .anchor(0.5F, 0.5F)
        )
        mapsReport.tag = report
        return mapsReport
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
        model.currentlyShownReport.removeObservers(this)
        model.currentlyShownGrade.removeObservers(this)
    }

    //Private functions
    private fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        mMap.uiSettings.isCompassEnabled = false
        mMap.uiSettings.isMapToolbarEnabled = false
        mMap.uiSettings.isMyLocationButtonEnabled = false

        mMap.setOnMarkerClickListener {
            model.currentlyShownReport.value = (it.tag as Report)
            return@setOnMarkerClickListener true
        }
        mMap.setOnMapClickListener {
            showSheet(SheetVisibility.NONE)
        }
        mMap.setOnMyLocationClickListener { presenter.displayReportAdd() }
        mMap.setOnCameraIdleListener(::handleIdleMap)

        presenter.start()
        presenter.seekPermissions(true)

        if (intent.extras != null && intent.extras.containsKey(Constants.INTENT_REPORT_ID_KEY)) {
            intent?.extras?.remove(Constants.INTENT_REPORT_ID_KEY)
            presenter.displayReportFromNotifications(intent.extras.getString(Constants.INTENT_REPORT_ID_KEY))
        }
    }

    private fun showSheet(sheetVisibility: SheetVisibility) {
        detailsSheet.state = if (sheetVisibility == SheetVisibility.DETAILS_SHEET)
            BottomSheetBehavior.STATE_COLLAPSED
        else BottomSheetBehavior.STATE_HIDDEN
        addSheet.state = if (sheetVisibility == SheetVisibility.ADD_SHEET)
            BottomSheetBehavior.STATE_COLLAPSED
        else BottomSheetBehavior.STATE_HIDDEN
    }

    private fun fillDetailsSheet(report: Report) {
        runOnUiThread {
            detailsTypeChip.text = report.properties.accident.toHumanReadableString(this)
            detailsSeverityChip.text =
                report.properties.severity.toHumanReadableString(this)
            detailsSeverityChip.chipBackgroundColor = report.properties.severity.toColor(this)
            detailsTimeTextView.text =
                getString(R.string.time_details_description).toRelativeDateString(report.properties.creationDate)

            //This will be filled in showLocationOnDetailsSheet()
            detailsPlaceTextView.text = "..."
        }
    }

    private fun handleIdleMap() {
        val bounds = mMap.projection.visibleRegion.latLngBounds
        presenter.setRightButtonMode(bounds)
        presenter.loadReportsToMap(bounds.northeast.toPoint(), bounds.southwest.toPoint())
    }

    private fun animateAddButton(animationDrawable: Int) {
        addButton.setImageDrawable(ContextCompat.getDrawable(this, animationDrawable))
        (addButton.drawable as TransitionDrawable).isCrossFadeEnabled = true
        (addButton.drawable as TransitionDrawable).startTransition(
            BUTTON_ANIMATION_DURATION
        )
    }

    //Button functions
    fun onLeftButton(view: View) {
        filterSheet.show(supportFragmentManager, filterSheet.tag)
    }

    fun onRightButton(view: View) {
        when (model.buttonState) {
            RightButtonMode.ADD_REPORT -> presenter.displayReportAdd()
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

    fun onReportAddButton(view: View) {
        submitReportButton.isEnabled = false
        val reportProperties =
            ReportProperties(Accident.values()[(addChipGroup.checkedChipId - 1) % 6])
        presenter.submitReport(reportProperties)
    }

    //Observers
    private fun handleShowReport(report: Report) {
        moveCamera(report.location.toLatLng(), animate = true)
        fillDetailsSheet(report)
        showSheet(SheetVisibility.DETAILS_SHEET)
        val position = report.location
        thread {
            //TODO: this should not be here, like at all - do it in presenter
            showLocationOnDetailsSheet(
                location.getLocationName(
                    position.latitude,
                    position.longitude
                )
            )
        }
    }

    private fun handleGrade(grade: Grade) {
        relevantGradeButton.isEnabled = grade == Grade.UNGRADED || grade == Grade.RELEVANT
        irrelevantGradeButton.isEnabled = grade == Grade.UNGRADED || grade == Grade.NOT_RELEVANT

        relevantGradeButton.isClickable = grade == Grade.UNGRADED
        irrelevantGradeButton.isClickable = grade == Grade.UNGRADED
    }

}
