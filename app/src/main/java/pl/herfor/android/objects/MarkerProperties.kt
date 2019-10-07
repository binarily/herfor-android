package pl.herfor.android.objects

import androidx.room.Ignore
import java.util.*

data class MarkerProperties(
    val creationDate: Date,
    val accidentType: AccidentType,
    val severityType: SeverityType
) {
    @Ignore
    constructor(accidentType: AccidentType) : this(Date(System.currentTimeMillis()), accidentType, SeverityType.GREEN)

    fun getGlyph(): Int {
        return getResId(
            "ic_${accidentType.toString().toLowerCase(Locale.ROOT)}_${severityType.toString().toLowerCase(
                Locale.ROOT
            )}",
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
