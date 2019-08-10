package pl.herfor.android.objects

import java.util.*

data class MarkerProperties(
    val creationDate: Date,
    val accidentType: AccidentType,
    val severityType: SeverityType
) {
    constructor(accidentType: AccidentType) : this(Date(System.currentTimeMillis()), accidentType, SeverityType.GREEN)

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
