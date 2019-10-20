package pl.herfor.android.interfaces

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import pl.herfor.android.objects.MarkerGrade

@Dao
interface MarkerGradeDao {
    @Query("SELECT * FROM grades WHERE marker = :markerId")
    fun getGradesByMarkerId(markerId: String): LiveData<List<MarkerGrade>>

    @Insert
    fun insert(markerGrade: MarkerGrade)

    @Query("DELETE FROM grades WHERE marker = :markerId")
    fun deleteByMarkerId(markerId: String)
}