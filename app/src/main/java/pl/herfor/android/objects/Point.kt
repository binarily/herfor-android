package pl.herfor.android.objects

import com.google.android.gms.maps.model.LatLng

data class Point(var latitude: Double = 0.0, var longitude: Double = 0.0) {

    fun toLatLng(): LatLng {
        return LatLng(this.latitude, this.longitude)
    }
}