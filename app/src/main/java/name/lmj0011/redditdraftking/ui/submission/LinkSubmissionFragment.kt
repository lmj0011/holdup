package name.lmj0011.redditdraftking.ui.submission

import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import androidx.fragment.app.Fragment
import kotlinx.coroutines.flow.collectLatest
import name.lmj0011.redditdraftking.App
import name.lmj0011.redditdraftking.R
import name.lmj0011.redditdraftking.database.AppDatabase
import name.lmj0011.redditdraftking.databinding.FragmentLinkSubmissionBinding
import name.lmj0011.redditdraftking.helpers.RedditApiHelper
import name.lmj0011.redditdraftking.helpers.RedditAuthHelper
import name.lmj0011.redditdraftking.helpers.adapters.SubredditFlairListAdapter
import name.lmj0011.redditdraftking.helpers.enums.SubmissionKind
import name.lmj0011.redditdraftking.helpers.util.buildOneColorStateList
import name.lmj0011.redditdraftking.helpers.util.launchUI
import name.lmj0011.redditdraftking.ui.submission.bottomsheet.BottomSheetSubredditFlairFragment
import org.kodein.di.instance
import timber.log.Timber
import java.lang.Exception


class LinkSubmissionFragment(
    val setFlairItemForSubmission: SubredditFlairListAdapter.FlairItemClickListener,
    val removeFlairClickListener: (v: View) -> Unit,
): Fragment(R.layout.fragment_link_submission) {
    private lateinit var binding: FragmentLinkSubmissionBinding
    private lateinit var bottomSheetSubredditFlairFragment: BottomSheetSubredditFlairFragment
    private lateinit var redditAuthHelper: RedditAuthHelper
    private lateinit var redditApiHelper: RedditApiHelper
    private lateinit var  viewModel: SubmissionViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        redditAuthHelper = (requireContext().applicationContext as App).kodein.instance()
        redditApiHelper = (requireContext().applicationContext as App).kodein.instance()
        viewModel = SubmissionViewModel.getInstance(
            AppDatabase.getInstance(requireActivity().application).sharedDao,
            requireActivity().application
        )
    }

    override fun onResume() {
        super.onResume()
        viewModel.validateSubmission(SubmissionKind.Link)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupBinding(view)
        setupObservers()

        resetFlairToDefaultState()
    }

    private fun setupBinding(view: View) {
        binding = FragmentLinkSubmissionBinding.bind(view)
        binding.lifecycleOwner = this
    }

    private fun setupObservers() {
        viewModel.getSubreddit().observe(viewLifecycleOwner, {
            val listFlow = viewModel.getSubredditFlairListFlow()

            bottomSheetSubredditFlairFragment = BottomSheetSubredditFlairFragment(SubredditFlairListAdapter.FlairItemClickListener { flair ->
                binding.addFlairChip.text = flair.text
                try {
                    buildOneColorStateList(Color.parseColor(flair.backGroundColor))?.let {
                        binding.addFlairChip.chipBackgroundColor = it
                    }
                }
                catch (ex: Exception) { /* flair.backGroundColor was either null or not a recognizable color */}

                when(flair.textColor) {
                    "light" -> binding.addFlairChip.setTextColor(Color.WHITE)
                    else -> binding.addFlairChip.setTextColor(Color.DKGRAY)
                }

                setFlairItemForSubmission.clickListener(flair)
                viewModel.validateSubmission(SubmissionKind.Link)
                bottomSheetSubredditFlairFragment.dismiss()
            },
                { v: View ->
                    removeFlairClickListener(v)
                    resetFlairToDefaultState()
                    viewModel.validateSubmission(SubmissionKind.Link)
                    bottomSheetSubredditFlairFragment.dismiss()
                },
                listFlow)

            launchUI {
                listFlow.collectLatest {
                    if(it.isNotEmpty()) {
                        binding.addFlairChip.visibility = View.VISIBLE

                        binding.addFlairChip.setOnClickListener {
                            bottomSheetSubredditFlairFragment.show(childFragmentManager, "BottomSheetSubredditFlairFragment")
                        }
                    } else {
                        binding.addFlairChip.visibility = View.GONE
                    }
                }
            }
        })

        binding.linkTitleTextView.addTextChangedListener( object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {}

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                viewModel.setSubmissionLinkTitle(s.toString())
            }
        })

        binding.linkTextView.addTextChangedListener( object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {}

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                viewModel.setSubmissionLinkText(s.toString())
            }
        })


        viewModel.isLinkSubmissionSuccessful.observe(viewLifecycleOwner, {
            if (it) {
                clearUserInputViews()
                resetFlairToDefaultState()
            }
        })
    }

    private fun clearUserInputViews() {
        binding.linkTitleTextView.text.clear()
        binding.linkTextView.text.clear()
    }

    private fun resetFlairToDefaultState() {
        try {
            binding.addFlairChip.text = "+ Add Flair"
            buildOneColorStateList(Color.LTGRAY)?.let {
                binding.addFlairChip.chipBackgroundColor = it
            }
            binding.addFlairChip.setTextColor(Color.BLACK)
        }
        catch (ex: Exception) { /* flair.backGroundColor was either null or not a recognizable color */}
    }
}