package pl.herfor.android.objects

import lombok.Data

import java.time.LocalDateTime
import java.util.*

@Data
class MarkerProperties(
    private val creationDate: Date,
    private val accidentType: AccidentType,
    private val severityType: SeverityType
) {

}
