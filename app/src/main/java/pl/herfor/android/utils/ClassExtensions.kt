package pl.herfor.android.utils

import android.location.Location
import android.text.format.DateUtils
import com.google.android.gms.maps.model.LatLng
import pl.herfor.android.objects.Point
import java.util.*

internal fun LatLng?.toPoint(): Point {
    return Point(this?.latitude ?: 0.0, this?.longitude ?: 0.0)
}

internal fun Location?.toLatLng(): LatLng {
    return LatLng(this?.latitude ?: 0.0, this?.longitude ?: 0.0)
}

internal fun Location.toPoint(): Point {
    return Point(latitude, longitude)
}

internal fun Point?.toLatLng(): LatLng {
    return LatLng(this?.latitude ?: 0.0, this?.longitude ?: 0.0)
}

internal fun Point?.toLocation(): Location {
    val location = Location("")
    location.latitude = this?.latitude ?: 0.0
    location.longitude = this?.longitude ?: 0.0
    return location
}

internal fun String.toRelativeDateString(date: Date): String {
    return this.format(
        DateUtils.getRelativeTimeSpanString(
            date.time,
            System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS, DateUtils.FORMAT_ABBREV_RELATIVE
        ).toString()
    )
}