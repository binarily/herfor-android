package pl.herfor.android.objects

import org.threeten.bp.OffsetDateTime

data class MarkersLookupRequest constructor(
    var northEast: Point? = null,
    var southWest: Point? = null,
    var date: OffsetDateTime? = null
)