package pl.herfor.android.interfaces

import androidx.lifecycle.LiveData
import androidx.room.*
import pl.herfor.android.objects.Accident
import pl.herfor.android.objects.MarkerData
import pl.herfor.android.objects.NotificationStatus
import pl.herfor.android.objects.Severity

@Dao
interface MarkerDataDao {
    //For showing and removing markers on screen
    @Query("SELECT * FROM markers")
    fun getAll(): LiveData<List<MarkerData>>

    //Use for data binding in details sheet and eventual changes in notification
    @Query("SELECT * FROM markers where id = :id")
    fun getOne(id: String): LiveData<MarkerData>

    //Use for keeping track of changes of what is visible
    @Query(
        "SELECT * FROM markers m WHERE m.latitude BETWEEN :north AND :south " +
                "AND m.longitude BETWEEN :west and :east"
    )
    fun getFromLocation(
        north: Double,
        south: Double,
        east: Double,
        west: Double
    ): LiveData<List<MarkerData>>

    @Query("SELECT * FROM markers WHERE severity = :severity")
    fun getBySeverity(severity: Severity): LiveData<List<MarkerData>>

    @Query("SELECT * FROM markers WHERE accident = :accident")
    fun getByAccidentType(accident: Accident): LiveData<List<MarkerData>>

    //Add and update reports
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(vararg markers: MarkerData)

    //Delete no longer existing reports
    @Delete
    fun delete(marker: MarkerData)

    @Query("DELETE FROM markers WHERE id=:id")
    fun deleteById(id: String)

    @Query("UPDATE markers SET severity = :severity WHERE id = :id")
    fun updateSeverity(severity: Severity, id: String)

    @Query("UPDATE markers SET notificationStatus = :notificationStatus WHERE id = :id")
    fun updateNotificationStatus(notificationStatus: NotificationStatus, id: String)
}