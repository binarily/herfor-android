package pl.herfor.android.utils

import android.content.Context
import android.content.res.ColorStateList
import android.location.Location
import androidx.core.content.ContextCompat
import com.google.android.gms.maps.model.LatLng
import pl.herfor.android.R
import pl.herfor.android.objects.AccidentType
import pl.herfor.android.objects.MarkerProperties
import pl.herfor.android.objects.Point
import pl.herfor.android.objects.SeverityType

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

internal fun MarkerProperties.getGlyph(): Int {
    return getResId(
        "ic_${this.accidentType.toString().toLowerCase()}_${severityType.toString().toLowerCase()}",
        pl.herfor.android.R.drawable::class.java
    )
}

internal fun AccidentType.toHumanReadableString(): String {
    return when (this) {
        AccidentType.BUS -> "Autobus"
        AccidentType.TRAM -> "Tramwaj"
        AccidentType.RAIL -> "Pociąg"
        AccidentType.METRO -> "Metro"
        AccidentType.BIKE -> "Rower"
        AccidentType.PEDESTRIAN -> "Pieszo"
    }
}

internal fun SeverityType.toHumanReadableString(): String {
    return when (this) {
        SeverityType.GREEN -> "Zielony"
        SeverityType.YELLOW -> "Żółty"
        SeverityType.RED -> "Czerwony"
        SeverityType.NONE -> "Nieważny"
    }
}

internal fun SeverityType.toColor(context: Context): ColorStateList? {
    return when (this) {
        SeverityType.GREEN -> ContextCompat.getColorStateList(context, R.color.colorGreenSeverity)
        SeverityType.YELLOW -> ContextCompat.getColorStateList(context, R.color.colorYellowSeverity)
        SeverityType.RED -> ContextCompat.getColorStateList(context, R.color.colorRedSeverity)
        SeverityType.NONE -> ContextCompat.getColorStateList(context, R.color.colorGrey)
    }
}

private fun getResId(resName: String, c: Class<*>): Int {
    try {
        val idField = c.getDeclaredField(resName)
        return idField.getInt(idField)
    } catch (e: Exception) {
        e.printStackTrace()
        return -1
    }

}