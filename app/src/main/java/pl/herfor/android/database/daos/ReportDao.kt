package pl.herfor.android.database.daos

import androidx.lifecycle.LiveData
import androidx.room.*
import pl.herfor.android.objects.Report
import pl.herfor.android.objects.enums.Accident
import pl.herfor.android.objects.enums.NotificationStatus
import pl.herfor.android.objects.enums.Severity

@Dao
interface ReportDao {
    //For showing and removing markers on screen
    @Query("SELECT * FROM reports")
    fun getAll(): LiveData<List<Report>>

    //Use for data binding in details sheet and eventual changes in notification
    @Query("SELECT * FROM reports where id = :id")
    fun getOne(id: String): LiveData<Report>

    //Use for keeping track of changes of what is visible
    @Query(
        "SELECT * FROM reports m WHERE m.latitude BETWEEN :north AND :south " +
                "AND m.longitude BETWEEN :west and :east"
    )
    fun getFromLocation(
        north: Double,
        south: Double,
        east: Double,
        west: Double
    ): LiveData<List<Report>>

    @Query("SELECT * FROM reports WHERE severity = :severity")
    fun getBySeverity(severity: Severity): LiveData<List<Report>>

    @Query("SELECT * FROM reports WHERE accident = :accident")
    fun getByAccidentType(accident: Accident): LiveData<List<Report>>

    //Add and update reports
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(vararg markers: Report)

    //Delete no longer existing reports
    @Delete
    fun delete(marker: Report)

    @Query("DELETE FROM reports WHERE id=:id")
    fun deleteById(id: String)

    @Query("UPDATE reports SET severity = :severity WHERE id = :id")
    fun updateSeverity(severity: Severity, id: String)

    @Query("UPDATE reports SET notificationStatus = :notificationStatus WHERE id = :id")
    fun updateNotificationStatus(notificationStatus: NotificationStatus, id: String)

    @Query("SELECT * FROM reports WHERE severity IN (:severities) AND accident IN (:accidents)")
    fun getFiltered(
        severities: List<Severity>?,
        accidents: List<Accident>?
    ): LiveData<List<Report>>
}