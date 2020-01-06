package pl.herfor.android.objects

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import pl.herfor.android.objects.enums.NotificationStatus

@Entity(tableName = "report_local")
data class ReportLocalProperties(
    @PrimaryKey
    var id: String = "",
    @ColumnInfo(defaultValue = "NotShown")
    var notificationStatus: NotificationStatus = NotificationStatus.NotShown,
    @ColumnInfo(defaultValue = "false")
    var userMade: Boolean = false
)
