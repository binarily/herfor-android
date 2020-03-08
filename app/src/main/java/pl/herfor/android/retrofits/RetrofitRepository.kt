package pl.herfor.android.retrofits

import android.util.Log
import com.crashlytics.android.Crashlytics
import org.koin.core.KoinComponent
import org.koin.core.inject
import pl.herfor.android.modules.DatabaseModule
import pl.herfor.android.modules.LiveDataModule
import pl.herfor.android.objects.Report
import pl.herfor.android.objects.ReportGrade
import pl.herfor.android.objects.ReportLocalProperties
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
        .baseUrl(Constants.SERVER_LOCATION)
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
                if (response?.isSuccessful == true) {
                    liveData.connectionStatus.value = true
                    response.body()?.forEach { report ->
                        when (report.properties.severity) {
                            Severity.NONE -> {
                                database.threadSafeDelete(report)
                            }
                            else -> {
                                database.threadSafeInsert(report)
                            }
                        }
                    }
                } else {
                    liveData.connectionStatus.value = false
                }
            }
        }
    }

    private fun reportAddCallback(): Callback<Report> {
        return object : Callback<Report> {
            override fun onFailure(call: Call<Report>, t: Throwable) {
                Crashlytics.log(Log.DEBUG, "Retrofit", "Could not add marker")
                liveData.submittingReportStatus.value = false
            }

            override fun onResponse(call: Call<Report>, response: Response<Report>) {
                if (response.isSuccessful) {
                    val report = response.body()
                    if (report?.id != null) {
                        liveData.submittingReportStatus.value = true
                        database.threadSafeInsert(report)
                        val localProperties =
                            ReportLocalProperties(report.id, NotificationStatus.Dismissed, true)
                        database.threadSafeInsert(localProperties)
                    } else {
                        Crashlytics.log(Log.DEBUG, "Retrofit", "Add marker response with no return")
                        liveData.submittingReportStatus.value = false
                    }
                } else {
                    Crashlytics.log(
                        Log.ERROR,
                        "Retrofit",
                        "Add marker failed (HTTP code: %s)".format(response.code())
                    )
                    liveData.submittingReportStatus.value = false
                }
            }

        }
    }

    private fun singleReportForNotificationCallback(): Callback<Report> {
        return object : Callback<Report> {
            override fun onFailure(call: Call<Report>, t: Throwable) {
                liveData.connectionStatus.value = false
                liveData.reportFromNotificationStatus.value = null
                Crashlytics.log(Log.ERROR, "Retrofit", "Could not load marker from notification")
            }

            override fun onResponse(call: Call<Report>, response: Response<Report>) {
                if (response.isSuccessful) {
                    val report = response.body()
                    when (report?.properties?.severity) {
                        Severity.NONE -> {
                            database.threadSafeDelete(report)
                            liveData.reportFromNotificationStatus.value = null
                        }
                        null -> {
                            Crashlytics.log(
                                Log.ERROR,
                                "Retrofit",
                                "Received marker from notification with no severity, showing error"
                            )
                            liveData.reportFromNotificationStatus.value = null
                        }
                        else -> {
                            database.threadSafeInsert(report)
                            liveData.reportFromNotificationStatus.value = report.id
                        }
                    }
                } else {
                    liveData.connectionStatus.value = false
                    liveData.reportFromNotificationStatus.value = null
                    Crashlytics.log(
                        Log.ERROR,
                        "Retrofit",
                        "Could not load marker from notification (HTTP code: %s)".format(response.code())
                    )
                }
            }

        }
    }

    private fun gradeCallback(): Callback<ReportGrade> {
        return object : Callback<ReportGrade> {
            override fun onFailure(call: Call<ReportGrade>, t: Throwable) {
                liveData.gradeSubmissionStatus.value = false
                Crashlytics.log(Log.DEBUG, "Retrofit", "Grade submission failed")
            }

            override fun onResponse(call: Call<ReportGrade>, response: Response<ReportGrade>) {
                if (response.isSuccessful) {
                    val grade = response.body()
                    if (grade != null) {
                        database.threadSafeInsert(grade)
                        liveData.gradeSubmissionStatus.value = true
                    } else {
                        liveData.gradeSubmissionStatus.value = false
                        Crashlytics.log(Log.DEBUG, "Retrofit", "Submission with no grade")
                    }
                } else {
                    liveData.gradeSubmissionStatus.value = false
                    Crashlytics.log(
                        Log.DEBUG,
                        "Retrofit",
                        "Submission with no grade (HTTP code: %s)".format(response.code())
                    )
                }
            }

        }
    }

    private fun registerCalback(): Callback<User> {
        return object : Callback<User> {
            override fun onFailure(call: Call<User>, t: Throwable) {
                Crashlytics.log(Log.DEBUG, "Retrofit", "Could not register the user")
            }

            override fun onResponse(call: Call<User>, response: Response<User>) {
                if (response.isSuccessful) {
                    liveData.registrationId.value = response.body()?.id
                } else {
                    Crashlytics.log(
                        Log.DEBUG,
                        "Retrofit",
                        "Could not register the user (HTTP code: %s)".format(response.code())
                    )
                }
            }

        }
    }

}