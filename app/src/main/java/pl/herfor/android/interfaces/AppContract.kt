package pl.herfor.android.interfaces

import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.Marker
import pl.herfor.android.objects.Point
import pl.herfor.android.objects.Report
import pl.herfor.android.objects.ReportProperties
import pl.herfor.android.objects.enums.Grade
import pl.herfor.android.objects.enums.RightButtonMode

interface AppContract {
    interface View {
        fun showSubmitReportFailure()
        fun showConnectionError()
        fun dismissConnectionError()
        fun getPermissionForLocation()
        fun setLocationStateForMap(state: Boolean)
        fun showAddSheet()
        fun dismissAddSheet()
        fun showLocationOnDetailsSheet(location: String)
        fun moveCamera(position: LatLng, animate: Boolean)
        fun setRightButton(rightButtonMode: RightButtonMode, transition: Boolean)
        fun showToast(textId: Int, length: Int)

        fun addReportToMap(report: Report): Marker
    }

    interface Presenter {
        fun initializeObservers()

        fun displayReportAdd()
        fun submitReport(reportProperties: ReportProperties)
        fun submitGrade(grade: Grade)
        fun loadReportsToMap(northEast: Point, southWest: Point)
        fun handleLocationBeingEnabled()
        fun handleLocationBeingDisabled()
        fun zoomToCurrentLocation()
        fun showCurrentLocation()
        fun setRightButtonMode(bounds: LatLngBounds)

        fun seekPermissions(checkLocation: Boolean)

        fun displayReportFromNotifications(id: String)
    }
}