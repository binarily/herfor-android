package pl.herfor.android.services

import android.app.IntentService
import android.content.Intent
import pl.herfor.android.contexts.AppContext
import pl.herfor.android.objects.enums.NotificationStatus
import pl.herfor.android.utils.Constants

class NotificationDeletedService : IntentService("NotificationDeletedService") {

    override fun onHandleIntent(intent: Intent?) {
        val context = AppContext(applicationContext)
        val markerId = intent?.extras?.getString(Constants.NOTIFICATION_MESSAGE_ID_KEY) ?: return
        context.getDatabase().reportDao()
            .updateNotificationStatus(NotificationStatus.Dismissed, markerId)
    }

}
