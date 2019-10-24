package pl.herfor.android.retrofits

import pl.herfor.android.objects.MarkerGrade
import pl.herfor.android.objects.MarkerGradeRequest
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST

interface GradeRetrofit {
    @POST("grades/add")
    fun create(@Body markerGradeRequest: MarkerGradeRequest): Call<MarkerGrade>
}