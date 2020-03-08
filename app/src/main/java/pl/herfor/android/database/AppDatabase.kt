package pl.herfor.android.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import pl.herfor.android.database.daos.ReportDao
import pl.herfor.android.database.daos.ReportGradeDao
import pl.herfor.android.database.daos.ReportPropertiesDao
import pl.herfor.android.objects.Report
import pl.herfor.android.objects.ReportGrade
import pl.herfor.android.objects.ReportLocalProperties


@Database(entities = [Report::class, ReportGrade::class, ReportLocalProperties::class], version = 2)
@TypeConverters(DatabaseConverters::class)
abstract class AppDatabase : RoomDatabase() {
    companion object {
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            if (INSTANCE == null) {
                INSTANCE = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java, "reports.db"
                )
                    .enableMultiInstanceInvalidation()
                    .build()
            }
            return INSTANCE as AppDatabase
        }
    }

    abstract fun reportDao(): ReportDao

    abstract fun gradeDao(): ReportGradeDao

    abstract fun reportPropertiesDao(): ReportPropertiesDao
}