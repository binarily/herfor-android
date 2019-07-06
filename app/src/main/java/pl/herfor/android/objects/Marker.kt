package pl.herfor.android.objects


data class Marker constructor(var location: Point = Point(0.0, 0.0), val properties: MarkerProperties) {
    val type = "Marker"
    var id: String? = null

    constructor(latitude: Double, longitude: Double, properties: MarkerProperties) : this(Point(latitude, longitude), properties) {
    }

    fun constructTitle(): String {
        return "${properties.severityType} ${properties.accidentType}".capitalize()
    }

}
