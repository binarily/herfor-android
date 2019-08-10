package pl.herfor.android.objects


data class MarkerData constructor(var location: Point = Point(0.0, 0.0), val properties: MarkerProperties) {
    var id: String? = null
}
