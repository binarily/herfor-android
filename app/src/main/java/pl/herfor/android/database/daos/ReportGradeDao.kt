package pl.herfor.android.database.daos

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import pl.herfor.android.objects.ReportGrade

@Dao
interface ReportGradeDao {
    @Query("SELECT * FROM grades WHERE report = :reportId")
    fun getGradesByReportIdAsync(reportId: String): LiveData<List<ReportGrade>>

    @Query("SELECT * FROM grades WHERE report = :reportId")
    fun getGradesByReportIdSync(reportId: String): List<ReportGrade>

    @Insert
    fun insert(reportGrade: ReportGrade)

    @Query("DELETE FROM grades WHERE report = :reportId")
    fun deleteByMarkerId(reportId: String)
}