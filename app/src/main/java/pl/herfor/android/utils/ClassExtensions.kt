package pl.herfor.android.utils

import android.location.Location
import com.google.android.gms.maps.model.LatLng
import pl.herfor.android.objects.Point

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