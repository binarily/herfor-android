package pl.herfor.android.services

import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import pl.herfor.android.utils.Constants
import pl.herfor.android.workers.NotificationWorker

class NotificationService : FirebaseMessagingService() {
    override fun onMessageReceived(p0: RemoteMessage) {


        val workData = workDataOf(*p0.data.toList().toTypedArray())
        val notificationWorkRequest = OneTimeWorkRequestBuilder<NotificationWorker>()
            .setInputData(workData)
            .addTag(Constants.NOTIFICATION_WORKER_TAG)
            .build()
        WorkManager.getInstance(this).enqueue(notificationWorkRequest)
    }

    override fun onDeletedMessages() {
        super.onDeletedMessages()
    }
}