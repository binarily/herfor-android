package pl.herfor.android.services

import android.app.IntentService
import android.content.Intent
import org.koin.android.ext.android.inject
import pl.herfor.android.modules.DatabaseModule
import pl.herfor.android.objects.enums.NotificationStatus
import pl.herfor.android.utils.Constants

class NotificationDismissedService : IntentService("NotificationDeletedService") {

    private val database: DatabaseModule by inject()

    override fun onHandleIntent(intent: Intent?) {
        val markerId = intent?.extras?.getString(Constants.NOTIFICATION_MESSAGE_ID_KEY) ?: return
        database.getReportDao()
            .updateNotificationStatus(NotificationStatus.Dismissed, markerId)
    }

}
