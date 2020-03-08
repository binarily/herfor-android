package pl.herfor.android.retrofits

import pl.herfor.android.objects.ReportGrade
import pl.herfor.android.objects.requests.ReportGradeRequest
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST

interface GradeRetrofit {
    @POST("grades/add")
    fun create(@Body reportGradeRequest: ReportGradeRequest): Call<ReportGrade>
}