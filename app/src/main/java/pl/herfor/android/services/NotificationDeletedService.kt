package pl.herfor.android.services

import android.app.IntentService
import android.content.Intent
import pl.herfor.android.contexts.MarkerContext
import pl.herfor.android.objects.NotificationStatus
import pl.herfor.android.utils.Constants

class NotificationDeletedService : IntentService("NotificationDeletedService") {

    override fun onHandleIntent(intent: Intent?) {
        val context = MarkerContext(applicationContext)
        val markerId = intent?.extras?.getString(Constants.NOTIFICATION_MESSAGE_ID_KEY) ?: return
        context.getDatabase().markerDao()
            .updateNotificationStatus(NotificationStatus.Dismissed, markerId)
    }

}
