package pl.herfor.android.objects

import lombok.Data
import java.util.*

@Data
class MarkerProperties(
    val creationDate: Date,
    val accidentType: AccidentType,
    val severityType: SeverityType
) {
    fun getGlyph(): Int {
        return getResId(
            "ic_${accidentType.toString().toLowerCase()}_${severityType.toString().toLowerCase()}",
            pl.herfor.android.R.drawable::class.java
        )

    }

    private fun getResId(resName: String, c: Class<*>): Int {
        try {
            val idField = c.getDeclaredField(resName)
            return idField.getInt(idField)
        } catch (e: Exception) {
            e.printStackTrace()
            return -1
        }

    }
}
