package pl.herfor.android.retrofits

import android.util.Log
import pl.herfor.android.objects.Report
import pl.herfor.android.objects.ReportGrade
import pl.herfor.android.objects.enums.NotificationStatus
import pl.herfor.android.objects.enums.Severity
import pl.herfor.android.objects.requests.ReportAddRequest
import pl.herfor.android.objects.requests.ReportGradeRequest
import pl.herfor.android.objects.requests.ReportSearchRequest
import pl.herfor.android.utils.Constants
import pl.herfor.android.viewmodels.ReportViewModel
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class RetrofitRepository(val model: ReportViewModel) {
    //Retrofit
    private val retrofit = Retrofit.Builder()
        .baseUrl("http://192.168.0.127:8080/")
        .addConverterFactory(GsonConverterFactory.create(Constants.GSON))
        .build()
    private val reportRetrofit = retrofit.create(ReportRetrofit::class.java)
    private val gradeRetrofit = retrofit.create(GradeRetrofit::class.java)

    fun loadReport(
        id: String,
        callback: Callback<Report> = singleReportForNotificationCallback()
    ) {
        reportRetrofit.getMarker(id).enqueue(callback)
    }

    fun loadVisibleReportsChangedSince(request: ReportSearchRequest) {
        reportRetrofit.listMarkersNearbySince(request).enqueue(reportsCallback())
    }

    fun submitReport(request: ReportAddRequest) {
        reportRetrofit.addMarker(request).enqueue(reportAddCallback())
    }

    fun submitGrade(
        request: ReportGradeRequest,
        callback: Callback<ReportGrade> = gradeCallback()
    ) {
        gradeRetrofit.create(request).enqueue(callback)
    }

    private fun reportsCallback(): Callback<List<Report>> {
        return object : Callback<List<Report>> {
            override fun onFailure(call: Call<List<Report>>?, t: Throwable?) {
                model.connectionStatus.value = false
            }

            override fun onResponse(
                call: Call<List<Report>>?,
                response: Response<List<Report>>?
            ) {
                model.connectionStatus.value = true
                response?.body()?.forEach { report ->
                    when (report.properties.severity) {
                        Severity.NONE -> {
                            model.threadSafeDelete(report)
                        }
                        else -> {
                            model.threadSafeInsert(report)
                        }
                    }
                }
            }
        }
    }

    private fun reportAddCallback(): Callback<Report> {
        return object : Callback<Report> {
            override fun onFailure(call: Call<Report>, t: Throwable) {
                model.submittingReportStatus.value = false
            }

            override fun onResponse(call: Call<Report>, response: Response<Report>) {
                val report = response.body()
                if (report?.id != null) {
                    model.submittingReportStatus.value = true
                    report.properties.notificationStatus = NotificationStatus.Dismissed
                    model.threadSafeInsert(report)
                }
            }

        }
    }

    private fun singleReportForNotificationCallback(): Callback<Report> {
        return object : Callback<Report> {
            override fun onFailure(call: Call<Report>, t: Throwable) {
                model.connectionStatus.value = false
                model.reportFromNotificationStatus.value = null
            }

            override fun onResponse(call: Call<Report>, response: Response<Report>) {
                val report = response.body()
                when (report?.properties?.severity) {
                    Severity.NONE -> {
                        model.threadSafeDelete(report)
                        model.reportFromNotificationStatus.value = null
                    }
                    null -> {
                        Log.e(
                            this.javaClass.name,
                            "Received marker with no severity, showing error"
                        )
                        model.reportFromNotificationStatus.value = null
                    }
                    else -> {
                        model.threadSafeInsert(report)
                        model.reportFromNotificationStatus.value = report.id
                    }
                }
            }

        }
    }

    private fun gradeCallback(): Callback<ReportGrade> {
        return object : Callback<ReportGrade> {
            override fun onFailure(call: Call<ReportGrade>, t: Throwable) {
                model.gradeSubmissionStatus.value = false
            }

            override fun onResponse(call: Call<ReportGrade>, response: Response<ReportGrade>) {
                val grade = response.body()
                if (grade != null) {
                    model.threadSafeInsert(grade)
                    model.gradeSubmissionStatus.value = true
                    model.currentlyShownGrade.value = grade.grade
                } else {
                    model.gradeSubmissionStatus.value = false
                }
            }

        }
    }

}