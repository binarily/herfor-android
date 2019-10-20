package pl.herfor.android.database

import androidx.room.TypeConverter
import org.threeten.bp.OffsetDateTime
import org.threeten.bp.format.DateTimeFormatter
import pl.herfor.android.objects.Accident
import pl.herfor.android.objects.Grade
import pl.herfor.android.objects.NotificationStatus
import pl.herfor.android.objects.Severity

class DatabaseConverters {
    private val formatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME

    @TypeConverter
    fun toOffsetDateTime(value: String?): OffsetDateTime? {
        return value?.let {
            return formatter.parse(value, OffsetDateTime::from)
        }
    }

    @TypeConverter
    fun fromOffsetDateTime(date: OffsetDateTime?): String? {
        return date?.format(formatter)
    }

    @TypeConverter
    fun severityTypeToString(severity: Severity?): String? {
        return severity?.name
    }

    @TypeConverter
    fun stringToSeverityType(string: String?): Severity? {
        return Severity.valueOf(string.toString())
    }

    @TypeConverter
    fun accidentTypeToString(accident: Accident?): String? {
        return accident?.name
    }

    @TypeConverter
    fun stringToAccidentType(string: String?): Accident? {
        return Accident.valueOf(string.toString())
    }

    @TypeConverter
    fun notificationStatusToString(notificationStatus: NotificationStatus?): String? {
        return notificationStatus?.name
    }

    @TypeConverter
    fun stringToNotificationStatus(string: String?): NotificationStatus? {
        return NotificationStatus.valueOf(string.toString())
    }

    @TypeConverter
    fun gradeToString(grade: Grade?): String? {
        return grade?.name
    }

    @TypeConverter
    fun stringToName(string: String?): Grade? {
        return Grade.valueOf(string.toString())
    }
}