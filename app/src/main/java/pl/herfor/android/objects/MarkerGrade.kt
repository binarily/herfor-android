package pl.herfor.android.objects

import androidx.room.*
import org.threeten.bp.OffsetDateTime
import pl.herfor.android.objects.enums.Grade

@Entity(
    foreignKeys = [ForeignKey(
        entity = MarkerData::class,
        parentColumns = arrayOf("id"),
        childColumns = arrayOf("marker")
    )],
    indices = [Index("id"), Index("marker")],
    tableName = "grades"
)
data class MarkerGrade(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "marker")
    val markerId: String,
    val submissionDate: OffsetDateTime,
    @Embedded val gradeLocation: Point,
    val grade: Grade
)