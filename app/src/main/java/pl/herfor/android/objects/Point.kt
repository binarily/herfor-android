package pl.herfor.android.objects

import com.google.android.gms.maps.model.LatLng

data class Point (var x: Double = 0.0, var y: Double = 0.0){

    fun toLatLng(): LatLng {
        return LatLng(this.x, this.y)
    }
}