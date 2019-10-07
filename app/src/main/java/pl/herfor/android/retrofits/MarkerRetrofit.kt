package pl.herfor.android.retrofits

import pl.herfor.android.objects.MarkerAddRequest
import pl.herfor.android.objects.MarkerData
import pl.herfor.android.objects.MarkersLookupRequest
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path


interface MarkerRetrofit {
    @GET("markers")
    fun listMarkers(): Call<List<MarkerData>>

    @POST("markers")
    fun listMarkersNearby(@Body request: MarkersLookupRequest): Call<List<MarkerData>>

    @POST("markers")
    fun listMarkersNearbySince(@Body request: MarkersLookupRequest): Call<List<MarkerData>>

    @GET("markers/{id}")
    fun getMarker(@Path("id") id: String): Call<MarkerData>

    @POST("markers/create")
    fun addMarker(@Body request: MarkerAddRequest): Call<MarkerData>
}