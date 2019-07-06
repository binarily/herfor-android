package pl.herfor.android.objects

import lombok.AllArgsConstructor
import lombok.Data
import lombok.NoArgsConstructor

@Data
@AllArgsConstructor
@NoArgsConstructor
class MarkersLookupRequest {
    var northEast: Point? = null
    var southWest: Point? = null
}
