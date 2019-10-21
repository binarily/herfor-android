package pl.herfor.android.activities

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.chip.Chip
import kotlinx.android.synthetic.main.sheet_filter.*
import pl.herfor.android.R
import pl.herfor.android.objects.Accident
import pl.herfor.android.objects.Severity
import pl.herfor.android.viewmodels.MarkerViewModel

class FilterSheetFragment(
    private val model: MarkerViewModel
) : BottomSheetDialogFragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.sheet_filter, container)
    }

    override fun onStart() {
        super.onStart()

        for (severityType in model.visibleSeverities) {
            (filterSeverityChipGroup.getChildAt(severityType.ordinal) as Chip).isChecked = true
        }
        for (accidentType in model.visibleAccidentTypes) {
            (filterTypeChipGroup.getChildAt(accidentType.ordinal) as Chip).isChecked = true
        }

        for (i in 0 until filterSeverityChipGroup.childCount) {
            (filterSeverityChipGroup.getChildAt(i) as Chip).setOnCheckedChangeListener { compoundButton, b ->
                model.severityFilterChanged.value = Severity.values()[i]
            }
        }

        for (i in 0 until filterTypeChipGroup.childCount) {
            (filterTypeChipGroup.getChildAt(i) as Chip).setOnCheckedChangeListener { compoundButton, b ->
                model.accidentFilterChanged.value = Accident.values()[i]
            }
        }
    }
}