package pl.herfor.android.modules

import org.koin.core.KoinComponent
import org.koin.core.inject
import pl.herfor.android.objects.Report
import pl.herfor.android.objects.enums.NotificationStatus
import pl.herfor.android.objects.enums.Severity
import pl.herfor.android.utils.Constants
import pl.herfor.android.utils.isVisible
import kotlin.random.Random

class BusinessLogicModule : KoinComponent {
    val preferences: PreferencesModule by inject()
    val database: DatabaseModule by inject()

    fun showNotificationByFilter(report: Report): Boolean {
        if (Constants.DEV_MODE) {
            return true
        }
        val severities = preferences.getSeverities()
        val accidentTypes = preferences.getAccidents()
        val displayNotifications = preferences.getSilentZoneNotificationCondition()

        if (!displayNotifications || !report.isVisible(severities, accidentTypes)) {
            return false
        }

        when {
            severities.contains(Severity.GREEN) -> when (report.properties.severity) {
                Severity.GREEN -> if (Random.nextInt(0, 100) < 50) return false
                Severity.YELLOW -> if (Random.nextInt(0, 100) < 100) return false
                Severity.RED -> if (Random.nextInt(0, 100) < 100) return false
                Severity.NONE -> return false
            }
            severities.contains(Severity.YELLOW) -> when (report.properties.severity) {
                Severity.GREEN -> if (Random.nextInt(0, 100) < 30) return false
                Severity.YELLOW -> if (Random.nextInt(0, 100) < 80) return false
                Severity.RED -> if (Random.nextInt(0, 100) < 100) return false
                Severity.NONE -> return false
            }
            severities.contains(Severity.RED) -> when (report.properties.severity) {
                Severity.GREEN -> if (Random.nextInt(0, 100) < 15) return false
                Severity.YELLOW -> if (Random.nextInt(0, 100) < 50) return false
                Severity.RED -> if (Random.nextInt(0, 100) < 100) return false
                Severity.NONE -> return false
            }
        }
        return true
    }

    fun alreadyShown(id: String): Boolean {
        val marker = database.getReportDao().getOne(id).value
            ?: return false

        return marker.properties.notificationStatus != NotificationStatus.NotShown
    }

    fun alreadyGraded(id: String): Boolean {
        val grades = database.getGradeDao().getGradesByReportIdSync(id)
        return grades.isNotEmpty()
    }
}