package pl.herfor.android.services

import android.app.IntentService
import android.content.Intent
import android.widget.Toast
import androidx.core.app.NotificationManagerCompat
import com.google.android.gms.tasks.Tasks
import org.koin.core.KoinComponent
import org.koin.core.inject
import org.koin.core.parameter.parametersOf
import pl.herfor.android.R
import pl.herfor.android.contexts.AppContext
import pl.herfor.android.modules.PreferencesModule
import pl.herfor.android.objects.ReportGrade
import pl.herfor.android.objects.enums.Grade
import pl.herfor.android.objects.requests.ReportGradeRequest
import pl.herfor.android.retrofits.RetrofitRepository
import pl.herfor.android.utils.toPoint
import pl.herfor.android.viewmodels.ReportViewModel
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response


class NotificationGradingService : IntentService("NotificationGradingService"), KoinComponent {
    private val context: AppContext by inject { parametersOf(this) }
    private val model = ReportViewModel(application)
    val repository: RetrofitRepository by inject()
    private val preferences: PreferencesModule by inject { parametersOf(context) }

    companion object {
        const val GRADE_RELEVANT_ACTION = "pl.herfor.android.services.action.GRADE_RELEVANT"
        const val GRADE_NOT_RELEVANT_ACTION = "pl.herfor.android.services.action.GRADE_NOT_RELEVANT"

        const val REPORT_ID_PARAM = "pl.herfor.android.services.extra.MARKER_ID"
    }

    override fun onHandleIntent(intent: Intent?) {
        when (intent?.action) {
            GRADE_RELEVANT_ACTION -> {
                val markerId = intent.getStringExtra(REPORT_ID_PARAM)
                handleGrade(markerId, Grade.RELEVANT)
            }
            GRADE_NOT_RELEVANT_ACTION -> {
                val markerId = intent.getStringExtra(REPORT_ID_PARAM)
                handleGrade(markerId, Grade.NOT_RELEVANT)
            }
        }
    }

    private fun handleGrade(reportId: String, grade: Grade) {
        val location = Tasks.await(context.getCurrentLocation()).toPoint()
        val userId = preferences.getUserId()
        if (userId == null) {
            repository.register()
            context.showToast(R.string.grade_error_toast, Toast.LENGTH_SHORT)
            return
        }

        val request =
            ReportGradeRequest(userId, reportId, location, grade)
        repository.submitGrade(request, gradeNotificationCallback())
    }


    private fun gradeNotificationCallback(): Callback<ReportGrade> {
        return object : Callback<ReportGrade> {
            override fun onFailure(call: Call<ReportGrade>, t: Throwable) {
                context.showToast(R.string.grade_error_toast, Toast.LENGTH_SHORT)
            }

            override fun onResponse(call: Call<ReportGrade>, response: Response<ReportGrade>) {
                val grade = response.body()
                if (grade != null) {
                    model.threadSafeInsert(grade)
                    NotificationManagerCompat.from(context.getContext())
                        .cancel(grade.reportId, 0)
                    context.showToast(R.string.grade_thanks_toast, Toast.LENGTH_SHORT)
                } else {
                    context.showToast(R.string.grade_error_toast, Toast.LENGTH_SHORT)
                }
            }

        }
    }

}
