package pl.herfor.android.components

import pl.herfor.android.activities.MapsActivity
import pl.herfor.android.contexts.MarkerContext
import pl.herfor.android.objects.MarkerData
import javax.inject.Inject

class GoogleMapComponent @Inject constructor(mapsActivity: MapsActivity, context: MarkerContext) {
    //TODO: from presenter: logic to database, from view: interaction with the map

    fun addToMap(marker: MarkerData) {

    }

    fun removeFromMap(marker: MarkerData) {

    }
}