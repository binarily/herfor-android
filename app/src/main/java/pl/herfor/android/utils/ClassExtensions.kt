package pl.herfor.android.utils

import android.content.SharedPreferences
import android.location.Location
import android.text.format.DateUtils
import com.google.android.gms.maps.model.LatLng
import pl.herfor.android.objects.AccidentType
import pl.herfor.android.objects.Point
import pl.herfor.android.objects.SeverityType
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

internal fun SharedPreferences.getSeverities(): List<SeverityType?> {
    val severities = arrayOf(
        if (this.getBoolean(
                "severityType.GREEN",
                false
            )
        ) SeverityType.GREEN else null,
        if (this.getBoolean(
                "severityType.YELLOW",
                true
            )
        ) SeverityType.YELLOW else null,
        if (this.getBoolean("severityType.RED", true)) SeverityType.RED else null
    )
    return severities.toList()

}

internal fun SharedPreferences.getAccidentTypes(): List<AccidentType?> {
    val accidentTypes = mutableListOf<AccidentType?>()

    for (accidentType in AccidentType.values()) {
        accidentTypes.add(
            if (this.getBoolean(
                    "accidentType.${accidentType.name}",
                    true
                )
            ) accidentType else null
        )
    }
    return accidentTypes
}