package pl.herfor.android.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import pl.herfor.android.interfaces.MarkerDataDao
import pl.herfor.android.objects.MarkerData


@Database(entities = [MarkerData::class], version = 1)
@TypeConverters(DatabaseConverters::class)
abstract class MarkerDatabase : RoomDatabase() {
    companion object {
        var INSTANCE: MarkerDatabase? = null

        fun getDatabase(context: Context): MarkerDatabase {
            if (INSTANCE == null) {
                INSTANCE = Room.databaseBuilder(
                    context.applicationContext,
                    MarkerDatabase::class.java, "markers.db"
                )
                    .enableMultiInstanceInvalidation()
                    .build()
            }
            return INSTANCE as MarkerDatabase
        }
    }

    abstract fun markerDao(): MarkerDataDao

}