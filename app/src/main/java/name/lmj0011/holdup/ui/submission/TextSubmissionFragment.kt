package name.lmj0011.holdup.ui.submission

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import name.lmj0011.holdup.FullscreenTextEntryActivity
import name.lmj0011.holdup.R
import name.lmj0011.holdup.database.models.Submission
import name.lmj0011.holdup.databinding.FragmentTextSubmissionBinding
import name.lmj0011.holdup.helpers.enums.SubmissionKind
import name.lmj0011.holdup.helpers.interfaces.BaseFragmentInterface
import name.lmj0011.holdup.helpers.interfaces.SubmissionFragmentChild
import name.lmj0011.holdup.helpers.util.launchUI

class TextSubmissionFragment(
    override var viewModel:  SubmissionViewModel,
    override val submission: Submission? = null,
    override val actionBarTitle: String? = "Self Submission"
): Fragment(R.layout.fragment_text_submission),
    BaseFragmentInterface, SubmissionFragmentChild {
    private lateinit var binding: FragmentTextSubmissionBinding

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupBinding(view)
        setupObservers()

        submission?.let {
            binding.textEditTextTextMultiLine.setText(it.body)
        }
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
                text?.let{
                    viewModel.submissionSelfText.postValue(it)
                    binding.textEditTextTextMultiLine.setText(it)
                }
            }
        }
    }

    override fun setupBinding(view: View) {
        binding = FragmentTextSubmissionBinding.bind(view)
        binding.lifecycleOwner = this
    }

    override fun setupObservers() {
        binding.textEditTextTextMultiLine.setOnClickListener {
            val intent = Intent(requireContext(), FullscreenTextEntryActivity::class.java)
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

    override fun updateActionBarTitle() {
        actionBarTitle?.let {
            launchUI {
                (requireActivity() as AppCompatActivity).supportActionBar?.title = it
            }
        }
    }
}