package pl.herfor.android.objects

data class MarkerAddRequest(
    var location: Point,
    var properties: MarkerProperties
)