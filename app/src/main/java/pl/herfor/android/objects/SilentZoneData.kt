package pl.herfor.android.objects

import android.location.Location

data class SilentZoneData(val enabled: Boolean, val location: Location?, val locationName: String) {
    companion object {
        val DISABLED = SilentZoneData(false, null, "")
    }
}