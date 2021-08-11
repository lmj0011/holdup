package name.lmj0011.holdup.ui.submission

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.EditText
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import name.lmj0011.holdup.FullscreenTextEntryActivity
import name.lmj0011.holdup.R
import name.lmj0011.holdup.database.AppDatabase
import name.lmj0011.holdup.database.models.Submission
import name.lmj0011.holdup.databinding.FragmentTextSubmissionBinding
import name.lmj0011.holdup.helpers.enums.SubmissionKind
import name.lmj0011.holdup.helpers.interfaces.BaseFragmentInterface
import name.lmj0011.holdup.helpers.interfaces.SubmissionFragmentChild
import name.lmj0011.holdup.helpers.util.launchIO
import timber.log.Timber

class TextSubmissionFragment: Fragment(R.layout.fragment_text_submission),
    BaseFragmentInterface, SubmissionFragmentChild {
    override lateinit var parentContext: Context
    override lateinit var viewModel: SubmissionViewModel
    override var submission: Submission? = null
    override val actionBarTitle: String = "Self Submission"
    override var mode: Int = SubmissionFragmentChild.CREATE_AND_EDIT_MODE

    private lateinit var binding: FragmentTextSubmissionBinding

    companion object {
        fun newInstance(submission: Submission?, mode: Int): TextSubmissionFragment {
            val fragment = TextSubmissionFragment()

            val args = Bundle().apply {
                putParcelable("submission", submission)
                putInt("mode", mode)
            }

            fragment.arguments = args

            return fragment
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        parentContext = context
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = SubmissionViewModel.getInstance(
            AppDatabase.getInstance(requireActivity().application).sharedDao,
            requireActivity().application
        )

        submission = requireArguments().getParcelable("submission") as? Submission
        mode = requireArguments().getInt("mode")
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view.findViewById<EditText>(R.id.textEditTextTextMultiLine).setText(submission?.body)
        setupBinding(view)
        setupObservers()
    }

    override fun onResume() {
        super.onResume()
        updateActionBarTitle()
        viewModel.validateSubmission(SubmissionKind.Self)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when(requestCode) {
            FullscreenTextEntryActivity.FULLSCREEN_TEXT_ENTRY_REQUEST_CODE -> {
                val text = data?.getStringExtra(FullscreenTextEntryActivity.RESULT_OUTPUT_TEXT)
                text?.let{ text ->
                    viewModel.submissionSelfText.postValue(text)
                    binding.textEditTextTextMultiLine.setText(text)

                    submission?.apply {
                        body = text
                        Timber.d("body: $body")
                        launchIO { viewModel.updateSubmission(this@apply) }
                    }
                }
            }
        }
    }

    override fun setupBinding(view: View) {
        binding = FragmentTextSubmissionBinding.bind(view)
        binding.lifecycleOwner = viewLifecycleOwner

        if (mode == SubmissionFragmentChild.VIEW_MODE) {
            binding.textEditTextTextMultiLine
                .setTextColor(ContextCompat.getColorStateList(parentContext, R.color.edit_text_no_disabled_selector))
            binding.textEditTextTextMultiLine.isEnabled = false
        }
    }

    override fun setupObservers() {
        binding.textEditTextTextMultiLine.setOnClickListener {
            val intent = Intent(parentContext, FullscreenTextEntryActivity::class.java)
            intent.putExtra("start_text", binding.textEditTextTextMultiLine.text.toString())
            intent.putExtra("start_position", binding.textEditTextTextMultiLine.selectionStart)
            startActivityForResult(intent, FullscreenTextEntryActivity.FULLSCREEN_TEXT_ENTRY_REQUEST_CODE)
        }


        viewModel.isSubmissionSuccessful.observe(viewLifecycleOwner, {
            if (it) {
                clearUserInputViews()
            }
        })
    }
    override fun setupRecyclerView() {}

    override fun clearUserInputViews() {
        binding.textEditTextTextMultiLine.text.clear()
    }

    override fun updateActionBarTitle() {}
}