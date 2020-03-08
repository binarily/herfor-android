package pl.herfor.android.objects

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "reports")
data class Report(
    @PrimaryKey var id: String,
    @Embedded var location: Point,
    @Embedded var properties: ReportProperties
)