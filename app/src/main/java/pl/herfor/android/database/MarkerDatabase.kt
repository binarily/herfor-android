package pl.herfor.android.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import pl.herfor.android.interfaces.MarkerDataDao
import pl.herfor.android.interfaces.MarkerGradeDao
import pl.herfor.android.objects.MarkerData
import pl.herfor.android.objects.MarkerGrade


@Database(entities = [MarkerData::class, MarkerGrade::class], version = 1)
@TypeConverters(DatabaseConverters::class)
abstract class MarkerDatabase : RoomDatabase() {
    companion object {
        private var INSTANCE: MarkerDatabase? = null

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

    abstract fun gradeDao(): MarkerGradeDao

}