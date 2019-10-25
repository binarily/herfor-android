package pl.herfor.android.interfaces

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import pl.herfor.android.objects.MarkerGrade

@Dao
interface MarkerGradeDao {
    @Query("SELECT * FROM grades WHERE marker = :markerId")
    fun getGradesByMarkerIdAsync(markerId: String): LiveData<List<MarkerGrade>>

    @Query("SELECT * FROM grades WHERE marker = :markerId")
    fun getGradesByMarkerIdSync(markerId: String): List<MarkerGrade>

    @Insert
    fun insert(markerGrade: MarkerGrade)

    @Query("DELETE FROM grades WHERE marker = :markerId")
    fun deleteByMarkerId(markerId: String)
}