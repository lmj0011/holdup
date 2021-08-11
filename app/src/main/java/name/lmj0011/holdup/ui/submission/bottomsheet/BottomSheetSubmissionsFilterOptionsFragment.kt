package name.lmj0011.holdup.ui.submission.bottomsheet

import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.coroutines.flow.first
import name.lmj0011.holdup.App
import name.lmj0011.holdup.R
import name.lmj0011.holdup.databinding.BottomsheetFragmentSubmissionsFilterOptionsBinding
import name.lmj0011.holdup.helpers.DataStoreHelper
import name.lmj0011.holdup.helpers.util.launchUI
import org.kodein.di.instance

class BottomSheetSubmissionsFilterOptionsFragment(private val onDismissCallback: () -> Unit): BottomSheetDialogFragment() {
    private lateinit var binding: BottomsheetFragmentSubmissionsFilterOptionsBinding
    private lateinit var dataStoreHelper: DataStoreHelper

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        dataStoreHelper = (requireContext().applicationContext as App).kodein.instance()

        return inflater.inflate(R.layout.bottomsheet_fragment_submissions_filter_options, container, false)
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
        binding = BottomsheetFragmentSubmissionsFilterOptionsBinding.bind(view)
        binding.lifecycleOwner = viewLifecycleOwner

        launchUI {
            when(dataStoreHelper.getSubmissionsDisplayOption().first()) {
                resources.getString(R.string.submissions_display_option_compact_list) -> {
                    binding.displayRadioGroup.check(R.id.compactListRadioOption)
                }
                resources.getString(R.string.submissions_display_option_full_list) -> {
                    binding.displayRadioGroup.check(R.id.fullListRadioOption)
                }
                else -> binding.displayRadioGroup.check(R.id.fullListRadioOption)
            }
        }

        binding.compactListRadioOption.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                launchUI { dataStoreHelper
                    .setSubmissionsDisplayOption(resources.getString(R.string.submissions_display_option_compact_list))
                }
            }
        }

        binding.fullListRadioOption.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                launchUI { dataStoreHelper
                    .setSubmissionsDisplayOption(resources.getString(R.string.submissions_display_option_full_list))
                }
            }
        }
    }

    private fun setupObservers() {

    }
}