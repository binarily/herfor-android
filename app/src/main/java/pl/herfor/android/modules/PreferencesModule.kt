package pl.herfor.android.modules

import android.location.Location
import com.google.android.gms.location.DetectedActivity
import org.koin.core.KoinComponent
import pl.herfor.android.interfaces.ContextRepository
import pl.herfor.android.objects.SilentZoneData
import pl.herfor.android.objects.enums.Accident
import pl.herfor.android.objects.enums.Severity
import pl.herfor.android.objects.enums.SilentZone

class PreferencesModule(context: ContextRepository) : KoinComponent {

    private val sharedPreferences = context.getSharedPreferences()

    fun getUserId(): String? {
        return sharedPreferences.getString("registrationId", null)
    }

    fun setUserId(id: String) {
        sharedPreferences.edit()
            .putString("registrationId", id)
            .apply()
    }

    fun getSeverities(): MutableList<Severity> {
        val severities = arrayOf(
            if (sharedPreferences.getBoolean(
                    "severity.GREEN",
                    false
                )
            ) Severity.GREEN else null,
            if (sharedPreferences.getBoolean(
                    "severity.YELLOW",
                    true
                )
            ) Severity.YELLOW else null,
            if (sharedPreferences.getBoolean("severity.RED", true)) Severity.RED else null
        )
        return severities.toList().filterNotNull() as MutableList<Severity>
    }

    fun setSeverity(severity: Severity, result: Boolean) {
        sharedPreferences.edit()
            .putBoolean("severity.${severity.name}", result)
            .apply()
    }

    fun getAccidents(): MutableList<Accident> {
        val accidents = mutableListOf<Accident?>()

        for (accidentType in Accident.values()) {
            accidents.add(
                if (sharedPreferences.getBoolean(
                        "accident.${accidentType.name}",
                        true
                    )
                ) accidentType else null
            )
        }
        return accidents.filterNotNull() as MutableList<Accident>

    }

    fun setAccident(accident: Accident, result: Boolean) {
        sharedPreferences.edit()
            .putBoolean("accident.${accident.name}", result)
            .apply()
    }

    fun getSilentZoneNotificationCondition(): Boolean {
        return sharedPreferences.getBoolean("displayNotifications", true)
    }

    fun setSilentZoneNotificationCondition(result: Boolean) {
        sharedPreferences.edit()
            .putBoolean("displayNotifications", result)
            .apply()
    }

    fun getCurrentActivity(): Int {
        return sharedPreferences.getInt("currentActivity", DetectedActivity.STILL)
    }

    fun setCurrentActivity(activity: Int) {
        sharedPreferences.edit()
            .putInt("currentActivity", activity)
            .apply()
    }

    fun getSilentZoneData(silentZone: SilentZone): SilentZoneData {
        val location = Location("dummyprovider")
        location.latitude =
            sharedPreferences.getFloat("silentZone.${silentZone.name}.location.latitude", 0.0f)
                .toDouble()
        location.longitude =
            sharedPreferences.getFloat("silentZone.${silentZone.name}.location.longitude", 0.0f)
                .toDouble()

        return SilentZoneData(
            sharedPreferences.getBoolean("silentZone.${silentZone.name}", false),
            location,
            sharedPreferences
                .getString("silentZone.${silentZone.name}.locationName", "")!!
        )
    }

    fun setSilentZoneData(silentZone: SilentZone, silentZoneData: SilentZoneData) {
        sharedPreferences.edit()
            .putBoolean("silentZone.${silentZone.name}", silentZoneData.enabled)
            .putFloat(
                "silentZone.${silentZone.name}.location.latitude",
                silentZoneData.location?.latitude?.toFloat() ?: 0.0f
            )
            .putFloat(
                "silentZone.${silentZone.name}.location.longitude",
                silentZoneData.location?.longitude?.toFloat() ?: 0.0f
            )
            .putString("silentZone.${silentZone.name}.locationName", silentZoneData.locationName)
            .apply()
    }
}