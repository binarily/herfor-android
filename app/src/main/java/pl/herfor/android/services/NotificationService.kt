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
        //TODO: read action tag here
        //Depending on it, push stuff towards a proper worker
        //(will be used in geofencing broadcast receiver, where we'll push stuff towards proper worker)
        val notificationWorkRequest = OneTimeWorkRequestBuilder<NotificationWorker>()
            .setInputData(workData)
            .addTag(Constants.NOTIFICATION_WORKER_TAG)
            .build()
        WorkManager.getInstance(this).enqueue(notificationWorkRequest)
    }

}