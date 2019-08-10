package pl.herfor.android.objects

import java.util.*

data class MarkersLookupRequest constructor(
    var northEast: Point? = null,
    var southWest: Point? = null,
    var date: Date? = null
)