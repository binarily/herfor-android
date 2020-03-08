package pl.herfor.android.database.daos

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import pl.herfor.android.objects.ReportLocalProperties

@Dao
interface ReportPropertiesDao {
    @Query("SELECT * FROM report_local WHERE id = :reportId")
    fun getOne(reportId: String): LiveData<ReportLocalProperties>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insert(reportLocalProperties: ReportLocalProperties)
}