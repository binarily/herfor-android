package pl.herfor.android.objects


data class MarkerData constructor(
    var location: Point = Point(0.0, 0.0),
    val properties: MarkerProperties = MarkerProperties(AccidentType.PEDESTRIAN)
) {
    var id: String? = null
}
