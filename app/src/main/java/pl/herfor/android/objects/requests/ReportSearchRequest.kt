package pl.herfor.android.objects.requests

import org.threeten.bp.OffsetDateTime
import pl.herfor.android.objects.Point

data class ReportSearchRequest constructor(
    var northEast: Point? = null,
    var southWest: Point? = null,
    var date: OffsetDateTime? = null
)