package name.lmj0011.holdup.ui.submission.bottomsheet

import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import name.lmj0011.holdup.App
import name.lmj0011.holdup.R
import name.lmj0011.holdup.databinding.BottomsheetFragmentSubmissionsScheduleOptionsBinding
import name.lmj0011.holdup.helpers.DataStoreHelper
import name.lmj0011.holdup.helpers.DateTimeHelper
import org.kodein.di.instance
import timber.log.Timber
import java.util.*

class BottomSheetSubmissionsScheduleOptionsFragment(
    private val onPreSelectedDateAndTimeCallback: (millis: Long) -> Unit,
    private val onPickDateAndTimeCallback: () -> Unit,
    private val onPostNowCallback: () -> Unit,
    private val onDismissCallback: () -> Unit
): BottomSheetDialogFragment() {
    private lateinit var binding: BottomsheetFragmentSubmissionsScheduleOptionsBinding
    private lateinit var dataStoreHelper: DataStoreHelper

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        dataStoreHelper = (requireContext().applicationContext as App).kodein.instance()

        return inflater.inflate(R.layout.bottomsheet_fragment_submissions_schedule_options, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupBinding(view)
        setupObservers()
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        onDismissCallback()
    }

    private fun setupBinding(view: View) {
        binding = BottomsheetFragmentSubmissionsScheduleOptionsBinding.bind(view)
        binding.lifecycleOwner = viewLifecycleOwner

        val timesMap = DateTimeHelper.getPreScheduledPostTimes(Calendar.getInstance(), Calendar.getInstance())

        binding.dateAndTimeOption1DescriptionTextView.text = timesMap["option1"]?.first
        binding.dateAndTimeOption1DateTextView.text = timesMap["option1"]?.second

        binding.dateAndTimeOption2DescriptionTextView.text = timesMap["option2"]?.first
        binding.dateAndTimeOption2DateTextView.text = timesMap["option2"]?.second

        binding.dateAndTimeOption3DescriptionTextView.text = timesMap["option3"]?.first
        binding.dateAndTimeOption3DateTextView.text = timesMap["option3"]?.second

        binding.dateAndTimeOption1TableRow.setOnClickListener {
            onPreSelectedDateAndTimeCallback(timesMap["option1"]!!.third)
        }

        binding.dateAndTimeOption2TableRow.setOnClickListener {
            onPreSelectedDateAndTimeCallback(timesMap["option2"]!!.third)
        }

        binding.dateAndTimeOption3TableRow.setOnClickListener {
            onPreSelectedDateAndTimeCallback(timesMap["option3"]!!.third)
        }

        binding.pickDateAndTimeIconTableRow.setOnClickListener {
            onPickDateAndTimeCallback()
        }

        binding.postNowTableRow.setOnClickListener {
            onPostNowCallback()
        }
    }

    private fun setupObservers() {

    }
}