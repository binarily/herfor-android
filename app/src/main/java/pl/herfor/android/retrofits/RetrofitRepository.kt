package pl.herfor.android.retrofits

import android.util.Log
import org.koin.core.KoinComponent
import org.koin.core.inject
import pl.herfor.android.modules.DatabaseModule
import pl.herfor.android.modules.LiveDataModule
import pl.herfor.android.objects.Report
import pl.herfor.android.objects.ReportGrade
import pl.herfor.android.objects.User
import pl.herfor.android.objects.enums.NotificationStatus
import pl.herfor.android.objects.enums.Severity
import pl.herfor.android.objects.requests.ReportAddRequest
import pl.herfor.android.objects.requests.ReportGradeRequest
import pl.herfor.android.objects.requests.ReportSearchRequest
import pl.herfor.android.utils.Constants
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class RetrofitRepository : KoinComponent {
    //Retrofit
    private val retrofit = Retrofit.Builder()
        .baseUrl("http://10.0.2.2:8080/")
        .addConverterFactory(GsonConverterFactory.create(Constants.GSON))
        .build()
    private val reportRetrofit = retrofit.create(ReportRetrofit::class.java)
    private val gradeRetrofit = retrofit.create(GradeRetrofit::class.java)
    private val userRetrofit = retrofit.create(UserRetrofit::class.java)
    private val liveData: LiveDataModule by inject()
    private val database: DatabaseModule by inject()

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

    fun register() {
        userRetrofit.register().enqueue(registerCalback())
    }

    private fun reportsCallback(): Callback<List<Report>> {
        return object : Callback<List<Report>> {
            override fun onFailure(call: Call<List<Report>>?, t: Throwable?) {
                liveData.connectionStatus.value = false
            }

            override fun onResponse(
                call: Call<List<Report>>?,
                response: Response<List<Report>>?
            ) {
                liveData.connectionStatus.value = true
                response?.body()?.forEach { report ->
                    when (report.properties.severity) {
                        Severity.NONE -> {
                            database.threadSafeDelete(report)
                        }
                        else -> {
                            database.threadSafeInsert(report)
                        }
                    }
                }
            }
        }
    }

    private fun reportAddCallback(): Callback<Report> {
        return object : Callback<Report> {
            override fun onFailure(call: Call<Report>, t: Throwable) {
                liveData.submittingReportStatus.value = false
            }

            override fun onResponse(call: Call<Report>, response: Response<Report>) {
                val report = response.body()
                if (report?.id != null) {
                    liveData.submittingReportStatus.value = true
                    report.properties.notificationStatus = NotificationStatus.Dismissed
                    database.threadSafeInsert(report)
                }
                //TODO: error handling here
                else {

                }
            }

        }
    }

    private fun singleReportForNotificationCallback(): Callback<Report> {
        return object : Callback<Report> {
            override fun onFailure(call: Call<Report>, t: Throwable) {
                liveData.connectionStatus.value = false
                liveData.reportFromNotificationStatus.value = null
            }

            override fun onResponse(call: Call<Report>, response: Response<Report>) {
                val report = response.body()
                when (report?.properties?.severity) {
                    Severity.NONE -> {
                        database.threadSafeDelete(report)
                        liveData.reportFromNotificationStatus.value = null
                    }
                    null -> {
                        Log.e(
                            this.javaClass.name,
                            "Received marker with no severity, showing error"
                        )
                        liveData.reportFromNotificationStatus.value = null
                    }
                    else -> {
                        database.threadSafeInsert(report)
                        liveData.reportFromNotificationStatus.value = report.id
                    }
                }
            }

        }
    }

    private fun gradeCallback(): Callback<ReportGrade> {
        return object : Callback<ReportGrade> {
            override fun onFailure(call: Call<ReportGrade>, t: Throwable) {
                liveData.gradeSubmissionStatus.value = false
            }

            override fun onResponse(call: Call<ReportGrade>, response: Response<ReportGrade>) {
                val grade = response.body()
                if (grade != null) {
                    database.threadSafeInsert(grade)
                    liveData.gradeSubmissionStatus.value = true
                } else {
                    liveData.gradeSubmissionStatus.value = false
                }
            }

        }
    }

    private fun registerCalback(): Callback<User> {
        return object : Callback<User> {
            override fun onFailure(call: Call<User>, t: Throwable) {
                //Do nothing - it'll be empty
            }

            override fun onResponse(call: Call<User>, response: Response<User>) {
                liveData.registrationId.value = response.body()?.id
            }

        }
    }

}