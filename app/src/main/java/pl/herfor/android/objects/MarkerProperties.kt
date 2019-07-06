package pl.herfor.android.objects

import lombok.Data
import java.util.*

@Data
class MarkerProperties(
    val creationDate: Date,
    val accidentType: AccidentType,
    val severityType: SeverityType
) {

}
