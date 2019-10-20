package pl.herfor.android.utils

import android.content.SharedPreferences
import android.location.Location
import android.text.format.DateUtils
import com.google.android.gms.location.DetectedActivity
import com.google.android.gms.maps.model.LatLng
import org.threeten.bp.OffsetDateTime
import pl.herfor.android.objects.Accident
import pl.herfor.android.objects.MarkerData
import pl.herfor.android.objects.Point
import pl.herfor.android.objects.Severity

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

internal fun SharedPreferences.getSeverities(): MutableList<Severity> {
    val severities = arrayOf(
        if (this.getBoolean(
                "severity.GREEN",
                false
            )
        ) Severity.GREEN else null,
        if (this.getBoolean(
                "severity.YELLOW",
                true
            )
        ) Severity.YELLOW else null,
        if (this.getBoolean("severity.RED", true)) Severity.RED else null
    )
    return severities.toList().filterNotNull() as MutableList<Severity>

}

internal fun SharedPreferences.getAccidentTypes(): MutableList<Accident> {
    val accidentTypes = mutableListOf<Accident?>()

    for (accidentType in Accident.values()) {
        accidentTypes.add(
            if (this.getBoolean(
                    "accident.${accidentType.name}",
                    true
                )
            ) accidentType else null
        )
    }
    return accidentTypes.filterNotNull() as MutableList<Accident>
}

internal fun MarkerData.isVisible(
    allowedSeverities: List<Severity>,
    allowedAccidents: List<Accident>
): Boolean {
    return allowedSeverities.contains(this.properties.severity)
            && allowedAccidents.contains(this.properties.accident)
}

internal fun Int.toDetectedActivityDistance(): Long {
    when (this) {
        DetectedActivity.STILL -> {
            return 100
        }
        DetectedActivity.ON_FOOT, DetectedActivity.WALKING, DetectedActivity.RUNNING -> {
            return 200
        }
        DetectedActivity.ON_BICYCLE -> {
            return 400
        }
        DetectedActivity.IN_VEHICLE -> {
            return 800
        }
        else -> {
            return 100
        }
    }
}