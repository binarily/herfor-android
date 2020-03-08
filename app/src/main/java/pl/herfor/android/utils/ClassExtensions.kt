package pl.herfor.android.utils

import android.location.Location
import android.text.format.DateUtils
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.SphericalUtil
import org.threeten.bp.OffsetDateTime
import pl.herfor.android.objects.Point
import pl.herfor.android.objects.Report
import pl.herfor.android.objects.enums.Accident
import pl.herfor.android.objects.enums.Severity

internal fun LatLng?.toPoint(): Point {
    return Point(this?.latitude ?: 0.0, this?.longitude ?: 0.0)
}

internal fun Location?.toLatLng(): LatLng {
    return LatLng(this?.latitude ?: 0.0, this?.longitude ?: 0.0)
}

internal fun Location.toPoint(): Point {
    return Point(latitude, longitude)
}

internal fun Point?.toLocation(): Location {
    val location = Location("")
    location.latitude = this?.latitude ?: 0.0
    location.longitude = this?.longitude ?: 0.0
    return location
}

internal fun String.toRelativeDateString(date: OffsetDateTime): String {
    return this.format(
        DateUtils.getRelativeTimeSpanString(
            date.toEpochSecond(),
            OffsetDateTime.now().toEpochSecond(),
            DateUtils.MINUTE_IN_MILLIS,
            DateUtils.FORMAT_ABBREV_RELATIVE
        ).toString()
    )
}

internal fun Report.isVisible(
    allowedSeverities: List<Severity>,
    allowedAccidents: List<Accident>
): Boolean {
    return allowedSeverities.contains(this.properties.severity)
            && allowedAccidents.contains(this.properties.accident)
}

internal class DoubleTrigger<A, B>(a: LiveData<A>, b: LiveData<B>) :
    MediatorLiveData<Pair<A?, B?>>() {
    init {
        addSource(a) { value = it to b.value }
        addSource(b) { value = a.value to it }
    }
}

internal fun Location.toNorthEast(away: Double): Point {
    val north = SphericalUtil.computeOffset(this.toLatLng(), away, 0.0)
    val northEast = SphericalUtil.computeOffset(north, away, 270.0).toPoint()
    return northEast
}

internal fun Location.toSouthWest(away: Double): Point {
    val south = SphericalUtil.computeOffset(this.toLatLng(), away, 180.0)
    val southWest = SphericalUtil.computeOffset(south, away, 90.0).toPoint()
    return southWest
}