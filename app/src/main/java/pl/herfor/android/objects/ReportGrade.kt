package pl.herfor.android.objects

import androidx.room.*
import org.threeten.bp.OffsetDateTime
import pl.herfor.android.objects.enums.Grade

@Entity(
    foreignKeys = [ForeignKey(
        entity = Report::class,
        parentColumns = arrayOf("id"),
        childColumns = arrayOf("report")
    )],
    indices = [Index("id"), Index("report")],
    tableName = "grades"
)
data class ReportGrade(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "report")
    val reportId: String,
    val submissionDate: OffsetDateTime,
    val grade: Grade
)