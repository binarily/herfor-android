package pl.herfor.android.objects

import android.content.Context
import android.content.res.ColorStateList
import androidx.core.content.ContextCompat
import pl.herfor.android.R

enum class SeverityType {
    GREEN, YELLOW, RED, NONE;

    fun toHumanReadableString(): String {
        return when (this) {
            GREEN -> "Zielony"
            YELLOW -> "Żółty"
            RED -> "Czerwony"
            NONE -> "Nieważny"
        }
    }

    fun toColor(context: Context): ColorStateList? {
        return when (this) {
            GREEN -> ContextCompat.getColorStateList(context, R.color.colorGreenSeverity)
            YELLOW -> ContextCompat.getColorStateList(context, R.color.colorYellowSeverity)
            RED -> ContextCompat.getColorStateList(context, R.color.colorRedSeverity)
            NONE -> ContextCompat.getColorStateList(context, R.color.colorGrey)
        }
    }
}
