package pl.herfor.android.objects

import androidx.room.ColumnInfo
import androidx.room.Ignore
import org.threeten.bp.OffsetDateTime
import pl.herfor.android.objects.enums.Accident
import pl.herfor.android.objects.enums.NotificationStatus
import pl.herfor.android.objects.enums.Severity
import java.util.*

data class ReportProperties(
    val creationDate: OffsetDateTime = OffsetDateTime.now(),
    val modificationDate: OffsetDateTime = OffsetDateTime.now(),
    val accident: Accident,
    val severity: Severity,
    @ColumnInfo(defaultValue = "NotShown")
    var notificationStatus: NotificationStatus = NotificationStatus.NotShown,
    @ColumnInfo(defaultValue = "false")
    var userMade: Boolean = false
) {
    @Ignore
    constructor(accident: Accident) : this(
        OffsetDateTime.now(), OffsetDateTime.now(), accident, Severity.GREEN
    )

    fun getGlyph(): Int {
        return getResId(
            "ic_${accident.toString().toLowerCase(Locale.ROOT)}" +
                    "_${severity.toString().toLowerCase(Locale.ROOT)}",
            pl.herfor.android.R.drawable::class.java
        )
    }

    private fun getResId(resName: String, c: Class<*>): Int {
        return try {
            val idField = c.getDeclaredField(resName)
            idField.getInt(idField)
        } catch (e: Exception) {
            e.printStackTrace()
            -1
        }
    }
}
