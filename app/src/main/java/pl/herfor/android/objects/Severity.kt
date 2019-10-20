package pl.herfor.android.objects

import android.content.Context
import android.content.res.ColorStateList
import androidx.core.content.ContextCompat
import pl.herfor.android.R

enum class Severity {
    GREEN, YELLOW, RED, NONE;

    fun toHumanReadableString(context: Context): String {
        return when (this) {
            GREEN -> context.getString(R.string.green)
            YELLOW -> context.getString(R.string.yellow)
            RED -> context.getString(R.string.red)
            NONE -> context.getString(R.string.none_severity)
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
