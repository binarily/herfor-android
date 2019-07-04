package pl.herfor.android.objects

import lombok.Data
import lombok.Getter


data class Marker constructor(var geometry: Point = Point(0.0,0.0), val properties: MarkerProperties) {
    val type = "Marker"
    var id: String? = null

    constructor(latitude: Double, longitude: Double, properties: MarkerProperties) : this(Point(latitude, longitude), properties) {
    }

}
