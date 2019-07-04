package pl.herfor.android.retrofits

import pl.herfor.android.objects.Marker
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Path


interface MarkerRetrofit {
    @GET("markers")
    fun listMarkers(): Call<List<Marker>>
}