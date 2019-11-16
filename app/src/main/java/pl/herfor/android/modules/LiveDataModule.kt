package pl.herfor.android.modules

import androidx.lifecycle.MutableLiveData
import org.koin.core.KoinComponent
import pl.herfor.android.objects.Report

class LiveDataModule : KoinComponent {
    var started = false

    //Observables
    val addReportToMap: MutableLiveData<Report> by lazy {
        MutableLiveData<Report>()
    }
    val removeReportFromMap: MutableLiveData<String> by lazy {
        MutableLiveData<String>()
    }
    val updateReportOnMap: MutableLiveData<Report> by lazy {
        MutableLiveData<Report>()
    }

    val connectionStatus: MutableLiveData<Boolean?> by lazy {
        MutableLiveData<Boolean?>()
    }
    val submittingReportStatus: MutableLiveData<Boolean> by lazy {
        MutableLiveData<Boolean>()
    }
    val reportFromNotificationStatus: MutableLiveData<String?> by lazy {
        MutableLiveData<String?>()
    }
    val gradeSubmissionStatus: MutableLiveData<Boolean> by lazy {
        MutableLiveData<Boolean>()
    }
    val registrationId: MutableLiveData<String> by lazy {
        MutableLiveData<String>()
    }

}