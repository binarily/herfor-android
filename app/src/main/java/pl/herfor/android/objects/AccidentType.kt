package pl.herfor.android.objects

import android.content.Context
import pl.herfor.android.R

enum class AccidentType {
    BUS, TRAM, RAIL, METRO, BIKE, PEDESTRIAN;

    fun toHumanReadableString(context: Context): String {
        return when (this) {
            BUS -> context.getString(R.string.bus)
            TRAM -> context.getString(R.string.tram)
            RAIL -> context.getString(R.string.train)
            METRO -> context.getString(R.string.metro)
            BIKE -> context.getString(R.string.bike)
            PEDESTRIAN -> context.getString(R.string.pedestrian)
        }
    }

}
