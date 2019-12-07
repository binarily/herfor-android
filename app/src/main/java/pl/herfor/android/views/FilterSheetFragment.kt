package pl.herfor.android.views

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.chip.Chip
import kotlinx.android.synthetic.main.sheet_filter.*
import pl.herfor.android.R
import pl.herfor.android.objects.enums.Accident
import pl.herfor.android.objects.enums.Severity
import pl.herfor.android.objects.enums.SilentZone
import pl.herfor.android.viewmodels.ReportViewModel

class FilterSheetFragment(
    private val model: ReportViewModel
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

        for (severityType in model.visibleSeverities.value!!) {
            (filterSeverityChipGroup.getChildAt(severityType.ordinal) as Chip).isChecked = true
        }
        for (accidentType in model.visibleAccidents.value!!) {
            (filterTypeChipGroup.getChildAt(accidentType.ordinal) as Chip).isChecked = true
        }

        model.homeSilentZoneName.observe(viewLifecycleOwner, Observer {
            if (it.isNotEmpty()) {
                homeSilentZoneTextView.text =
                    context?.getText(R.string.silent_zones_set).toString().format(it)
            } else {
                homeSilentZoneTextView.text = context?.getText(R.string.silent_zone_initial)
            }
        })

        model.workSilentZoneName.observe(viewLifecycleOwner, Observer {
            if (it.isNotEmpty()) {
                workSilentZoneTextView.text =
                    context?.getText(R.string.silent_zones_set).toString().format(it)
            } else {
                workSilentZoneTextView.text = context?.getText(R.string.silent_zone_initial)
            }
        })


        for (i in 0 until filterSeverityChipGroup.childCount) {
            (filterSeverityChipGroup.getChildAt(i) as Chip).setOnCheckedChangeListener { _, _ ->
                model.severityFilterChanged.value = Severity.values()[i]
            }
        }

        for (i in 0 until filterTypeChipGroup.childCount) {
            (filterTypeChipGroup.getChildAt(i) as Chip).setOnCheckedChangeListener { _, _ ->
                model.accidentFilterChanged.value = Accident.values()[i]
            }
        }

        homeSilentZoneTextView.setOnClickListener {
            model.silentZoneToggled.value = SilentZone.HOME
        }

        workSilentZoneTextView.setOnClickListener {
            model.silentZoneToggled.value = SilentZone.WORK
        }

        privacyPolicyButton.setOnClickListener {
            AlertDialog.Builder(this.context)
                .setTitle(R.string.privacy_policy_button)
                .setMessage(R.string.privacy_policy)
                .setPositiveButton("OK") { dialog, _ ->
                    dialog.dismiss()
                }
                .show()
        }
    }

    override fun onStop() {
        super.onStop()
        model.homeSilentZoneName.removeObservers(viewLifecycleOwner)
        model.workSilentZoneName.removeObservers(viewLifecycleOwner)
    }
}