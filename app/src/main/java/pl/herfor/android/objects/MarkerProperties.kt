package pl.herfor.android.objects

import androidx.room.Ignore
import org.threeten.bp.OffsetDateTime
import java.util.*

data class MarkerProperties(
    val creationDate: OffsetDateTime = OffsetDateTime.now(),
    val modificationDate: OffsetDateTime = OffsetDateTime.now(),
    val localModificationDate: OffsetDateTime = OffsetDateTime.now(),
    val accident: Accident,
    val severity: Severity,
    val notificationStatus: NotificationStatus = NotificationStatus.NotShown
) {
    @Ignore
    constructor(accident: Accident) : this(
        OffsetDateTime.now(), OffsetDateTime.now(),
        OffsetDateTime.now(), accident, Severity.GREEN
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
