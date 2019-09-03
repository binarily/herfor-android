package pl.herfor.android.objects

enum class AccidentType {
    BUS, TRAM, RAIL, METRO, BIKE, PEDESTRIAN;

    fun toHumanReadableString(): String {
        return when (this) {
            BUS -> "Autobus"
            TRAM -> "Tramwaj"
            RAIL -> "Pociąg"
            METRO -> "Metro"
            BIKE -> "Rower"
            PEDESTRIAN -> "Pieszo"
        }
    }

}
