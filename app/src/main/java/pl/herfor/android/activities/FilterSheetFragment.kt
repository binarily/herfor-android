package pl.herfor.android.activities

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.chip.Chip
import kotlinx.android.synthetic.main.sheet_filter.*
import pl.herfor.android.R
import pl.herfor.android.objects.AccidentType
import pl.herfor.android.objects.SeverityType

interface FilterSheetFragmentInterface {
    fun toggleSeverityType(severityType: SeverityType)

    fun toggleAccidentType(accidentType: AccidentType)
}

class FilterSheetFragment : BottomSheetDialogFragment() {

    private lateinit var activity: FilterSheetFragmentInterface
    private lateinit var severityTypes: List<SeverityType>
    private lateinit var accidentTypes: List<AccidentType>

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.sheet_filter, container)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        try {
            activity = context as FilterSheetFragmentInterface
        } catch (e: ClassCastException) {
            throw ClassCastException("Fragment needs access to activity that extends FilterSheetFragmentInterface")
        }
    }

    override fun onStart() {
        super.onStart()

        for (severityType in severityTypes) {
            (filterSeverityChipGroup.getChildAt(severityType.ordinal) as Chip).isChecked = true
        }
        for (accidentType in accidentTypes) {
            (filterTypeChipGroup.getChildAt(accidentType.ordinal) as Chip).isChecked = true
        }

        for (i in 0 until filterSeverityChipGroup.childCount) {
            (filterSeverityChipGroup.getChildAt(i) as Chip).setOnCheckedChangeListener { compoundButton, b ->
                activity.toggleSeverityType(
                    SeverityType.values()[i]
                )
            }
        }

        for (i in 0 until filterTypeChipGroup.childCount) {
            (filterTypeChipGroup.getChildAt(i) as Chip).setOnCheckedChangeListener { compoundButton, b ->
                activity.toggleAccidentType(
                    AccidentType.values()[i]
                )
            }
        }
    }

    fun setSeverityTypes(checkedSeverityType: List<SeverityType>) {
        severityTypes = checkedSeverityType
    }

    fun setAccidentTypes(checkedAccidentType: List<AccidentType>) {
        accidentTypes = checkedAccidentType
    }
}