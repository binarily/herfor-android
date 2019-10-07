package pl.herfor.android.database

import androidx.room.TypeConverter
import pl.herfor.android.objects.AccidentType
import pl.herfor.android.objects.SeverityType
import java.util.*

class DatabaseConverters {
    @TypeConverter
    fun fromTimestamp(value: Long?): Date? {
        return value?.let { Date(it) }
    }

    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? {
        return date?.time
    }

    @TypeConverter
    fun severityTypeToString(severityType: SeverityType?): String? {
        return severityType?.name
    }

    @TypeConverter
    fun stringToSeverityType(string: String?): SeverityType? {
        return SeverityType.valueOf(string.toString())
    }

    @TypeConverter
    fun accidentTypeToString(accidentType: AccidentType?): String? {
        return accidentType?.name
    }

    @TypeConverter
    fun stringToAccidentType(string: String?): AccidentType? {
        return AccidentType.valueOf(string.toString())
    }
}