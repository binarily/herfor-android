package pl.herfor.android.modules

import android.app.PendingIntent
import android.content.Intent
import org.koin.core.KoinComponent
import org.koin.core.inject
import pl.herfor.android.interfaces.ContextRepository
import pl.herfor.android.services.NotificationGradingService
import pl.herfor.android.utils.Constants
import pl.herfor.android.views.MapsActivity
import kotlin.random.Random

class IntentModule : KoinComponent {
    private val appContext: ContextRepository by inject()

    fun openAppIntent(reportId: String): PendingIntent {
        return PendingIntent.getActivity(
            appContext.getContext(),
            Random.nextInt(),
            Intent(appContext.getContext(), MapsActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                putExtra(
                    Constants.INTENT_REPORT_ID_KEY,
                    reportId
                )
            },
            PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    fun relevantGradeIntent(reportId: String): PendingIntent {
        return PendingIntent.getService(
            appContext.getContext(),
            Random.nextInt(),
            Intent(appContext.getContext(), NotificationGradingService::class.java).apply {
                putExtra(
                    NotificationGradingService.REPORT_ID_PARAM,
                    reportId
                )
                action = NotificationGradingService.GRADE_RELEVANT_ACTION
            },
            PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    fun notRelevantGradeIntent(reportId: String): PendingIntent {
        return PendingIntent.getService(
            appContext.getContext(),
            Random.nextInt(),
            Intent(appContext.getContext(), NotificationGradingService::class.java).apply {
                putExtra(
                    NotificationGradingService.REPORT_ID_PARAM,
                    reportId
                )
                action = NotificationGradingService.GRADE_NOT_RELEVANT_ACTION
            },
            PendingIntent.FLAG_UPDATE_CURRENT
        )
    }
}