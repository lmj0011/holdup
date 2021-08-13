package name.lmj0011.holdup.ui.submission

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.AdapterView
import android.widget.EditText
import android.widget.ImageView
import android.widget.TableRow
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import name.lmj0011.holdup.FullscreenTextEntryActivity
import name.lmj0011.holdup.R
import name.lmj0011.holdup.database.AppDatabase
import name.lmj0011.holdup.database.models.Submission
import name.lmj0011.holdup.databinding.FragmentPollSubmissionBinding
import name.lmj0011.holdup.helpers.SubmissionValidatorHelper.Companion.MAX_POLL_OPTIONS
import name.lmj0011.holdup.helpers.enums.SubmissionKind
import name.lmj0011.holdup.helpers.interfaces.BaseFragmentInterface
import name.lmj0011.holdup.helpers.interfaces.SubmissionFragmentChild

class PollSubmissionFragment: Fragment(R.layout.fragment_poll_submission),
    BaseFragmentInterface, SubmissionFragmentChild {
    override lateinit var viewModel: SubmissionViewModel
    override var submission: Submission? = null
    override val actionBarTitle: String = "Poll Submission"
    override var mode: Int = SubmissionFragmentChild.CREATE_AND_EDIT_MODE

    private lateinit var binding: FragmentPollSubmissionBinding
    private val defaultSpinnerPosition = 2

    companion object {
        fun newInstance(submission: Submission?, mode: Int): PollSubmissionFragment {
            val fragment = PollSubmissionFragment()

            val args = Bundle().apply {
                putParcelable("submission", submission)
                putInt("mode", mode)
            }

            fragment.arguments = args

            return fragment
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = SubmissionViewModel.getInstance(
            AppDatabase.getInstance(requireActivity().application).sharedDao,
            requireActivity().application
        )

        submission = requireArguments().getParcelable("submission") as? Submission
        mode = requireArguments().getInt("mode")

        setupBinding(view)
        setupObservers()

        submission?.let {
            binding.pollEditTextTextMultiLine.setText(it.body)
            restoreOptions(it)
            binding.pollDurationSpinner.setSelection(it.pollDuration - 1)
        }
    }

    override fun onResume() {
        super.onResume()
        updateActionBarTitle()
        viewModel.validateSubmission(SubmissionKind.Poll)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when(requestCode) {
            FullscreenTextEntryActivity.FULLSCREEN_TEXT_ENTRY_REQUEST_CODE -> {
                val text = data?.getStringExtra(FullscreenTextEntryActivity.RESULT_OUTPUT_TEXT)
                text?.let{
                    viewModel.submissionPollBodyText.postValue(it)
                    binding.pollEditTextTextMultiLine.setText(it)
                }
            }
        }
    }

    override fun setupBinding(view: View) {
        binding = FragmentPollSubmissionBinding.bind(view)
        binding.lifecycleOwner = viewLifecycleOwner


        binding.pollDurationSpinner.setSelection(defaultSpinnerPosition) // 3 Days

        if (mode == SubmissionFragmentChild.VIEW_MODE) {
            binding.pollEditTextTextMultiLine
                .setTextColor(ContextCompat.getColorStateList(requireContext(), R.color.edit_text_no_disabled_selector))
            binding.pollEditTextTextMultiLine.isEnabled = false

            binding.pollDurationSpinner.isEnabled = false

            binding.addOptionImageButton.visibility = View.GONE
        }
    }

    override fun setupObservers() {
        binding.addOptionImageButton.setOnClickListener { addOption() }

        binding.pollEditTextTextMultiLine.setOnClickListener {
            val intent = Intent(requireContext(), FullscreenTextEntryActivity::class.java)
            intent.putExtra("start_text",  binding.pollEditTextTextMultiLine.text.toString());
            intent.putExtra("start_position",  binding.pollEditTextTextMultiLine.selectionStart);
            startActivityForResult(intent, FullscreenTextEntryActivity.FULLSCREEN_TEXT_ENTRY_REQUEST_CODE)
        }

        pollOptionsTableRefresh()

        binding.pollDurationSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {

            override fun onItemSelected(
                parent: AdapterView<*>,
                view: View?,
                position: Int,
                id: Long
            ) {
                val item = parent.getItemAtPosition(position) as String
                val duration = item.split(" ")[0].toInt()

                (parent.getChildAt(0) as? TextView)
                    ?.setTextColor(ContextCompat.getColorStateList(requireContext(), R.color.edit_text_no_disabled_selector))

                viewModel.submissionPollDuration.postValue(duration)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) { }

        }


        viewModel.isSubmissionSuccessful.observe(viewLifecycleOwner, {
            if (it) {
                clearUserInputViews()
            }
        })
    }

    override fun setupRecyclerView() {}

    override fun clearUserInputViews() {
        binding.pollEditTextTextMultiLine.text.clear()
        viewModel.submissionPollBodyText.postValue(null)

        pollOptionsTableRefresh(clearInput = true)
        viewModel.submissionPollOptions.postValue(listOf())

        binding.pollDurationSpinner.setSelection(defaultSpinnerPosition) // 3 Days
        viewModel.submissionPollDuration.postValue(null)
    }

    override fun updateActionBarTitle() {}

    /**
     * add a new TableRow in the TableLayout containing poll options
     */
    private fun addOption() {
        if (binding.pollOptionsTableLayout.childCount < MAX_POLL_OPTIONS) {
            val tableRow = layoutInflater.inflate(R.layout.poll_option_tablerow, null)

            tableRow.findViewWithTag<ImageView?>("removeOptionImageView")?.let { imgView ->
                imgView.setOnClickListener {
                    binding.pollOptionsTableLayout.removeView(tableRow)
                    pollOptionsTableRefresh()
                }
            }

            binding.pollOptionsTableLayout.addView(tableRow)
            pollOptionsTableRefresh()
        }
    }

    /**
     * restore the poll options
     */
    private fun restoreOptions(submission: Submission) {
        submission.pollOptions.take(2).forEachIndexed { idx, option ->
            val tableRowView = binding.pollOptionsTableLayout.getChildAt(idx)

            if(tableRowView is TableRow) {
                val editText = tableRowView.findViewWithTag<EditText>("optionEditText")
                editText.setText(option)

                if (mode == SubmissionFragmentChild.VIEW_MODE) {
                    editText
                        .setTextColor(ContextCompat.getColorStateList(requireContext(), R.color.edit_text_no_disabled_selector))
                    editText.isEnabled = false
                }
            }
        }

        submission.pollOptions.drop(2).forEach { option ->
            val tableRowView = layoutInflater.inflate(R.layout.poll_option_tablerow, null)

            val editText = tableRowView.findViewWithTag<EditText>("optionEditText")
            editText.setText(option)

            if (mode == SubmissionFragmentChild.VIEW_MODE) {
                editText
                    .setTextColor(ContextCompat.getColorStateList(requireContext(), R.color.edit_text_no_disabled_selector))
                editText.isEnabled = false
                tableRowView.findViewWithTag<ImageView?>("removeOptionImageView")?.let { imgView ->
                    imgView.visibility = View.GONE
                }
            } else {
                tableRowView.findViewWithTag<ImageView?>("removeOptionImageView")?.let { imgView ->
                    imgView.setOnClickListener {
                        binding.pollOptionsTableLayout.removeView(tableRowView)
                        pollOptionsTableRefresh()
                    }
                }
            }

            binding.pollOptionsTableLayout.addView(tableRowView)
            pollOptionsTableRefresh()
        }

    }

    /**
     * - keeps the EditText hints accurate based on the poll option's position
     * - hides the addOption button when max options are shown
     * - saves options changes to the viewModel
     * - if [clearInput] is true, then options' text values will be cleared
     */
    private fun pollOptionsTableRefresh(clearInput: Boolean = false) {
        var idx = 0
        var view = binding.pollOptionsTableLayout.getChildAt(idx)

        var position = 1

        while (view != null) {

            if(view is TableRow) {
                val editText = view.findViewWithTag<EditText>("optionEditText")

                editText.addTextChangedListener( object : TextWatcher {
                    override fun afterTextChanged(s: Editable?) { saveOptions() }

                    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

                    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                })


                if(editText.isVisible) {
                    editText.hint = "Option $position"
                    position++
                }

                if(clearInput) {
                    editText.setText("")
                }

            }

            idx++
            view = binding.pollOptionsTableLayout.getChildAt(idx)
        }

        if(position > MAX_POLL_OPTIONS || mode == SubmissionFragmentChild.VIEW_MODE) {
            binding.addOptionImageButton.visibility = View.GONE
        } else binding.addOptionImageButton.visibility = View.VISIBLE
    }

    private fun saveOptions() {
        var idx = 0
        var view = binding.pollOptionsTableLayout.getChildAt(idx)
        var options = mutableListOf<String>()

        while (view != null) {

            if(view is TableRow) {
                val editText = view.findViewWithTag<EditText>("optionEditText")
                val option = editText.text.toString()

                if (option.isNotBlank()) options.add(option)
            }

            idx++
            view = binding.pollOptionsTableLayout.getChildAt(idx)
        }

        viewModel.submissionPollOptions.postValue(options.toList())
    }
}
