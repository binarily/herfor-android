package pl.herfor.android.retrofits

import pl.herfor.android.objects.Report
import pl.herfor.android.objects.requests.ReportAddRequest
import pl.herfor.android.objects.requests.ReportSearchRequest
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path


interface ReportRetrofit {
    @POST("reports")
    fun listMarkersNearbySince(@Body request: ReportSearchRequest): Call<List<Report>>

    @GET("reports/{id}")
    fun getMarker(@Path("id") id: String): Call<Report>

    @POST("reports/create")
    fun addMarker(@Body request: ReportAddRequest): Call<Report>
}