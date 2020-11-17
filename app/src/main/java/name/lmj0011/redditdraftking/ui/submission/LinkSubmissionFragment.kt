package name.lmj0011.redditdraftking.ui.submission

import android.graphics.Color
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import name.lmj0011.redditdraftking.App
import name.lmj0011.redditdraftking.R
import name.lmj0011.redditdraftking.database.AppDatabase
import name.lmj0011.redditdraftking.database.models.Account
import name.lmj0011.redditdraftking.databinding.FragmentLinkSubmissionBinding
import name.lmj0011.redditdraftking.helpers.RedditApiHelper
import name.lmj0011.redditdraftking.helpers.RedditAuthHelper
import name.lmj0011.redditdraftking.helpers.adapters.SubredditFlairListAdapter
import name.lmj0011.redditdraftking.helpers.factories.ViewModelFactory
import name.lmj0011.redditdraftking.helpers.models.Subreddit
import name.lmj0011.redditdraftking.helpers.util.buildOneColorStateList
import name.lmj0011.redditdraftking.ui.submission.bottomsheet.BottomSheetSubredditFlairFragment
import org.kodein.di.instance
import java.lang.Exception


class LinkSubmissionFragment(
    val setFlairItemForSubmission: SubredditFlairListAdapter.FlairItemClickListener,
    val removeFlairClickListener: (v: View) -> Unit,
    val subredditAndAccountPair: Pair<Subreddit, Account>?,
): Fragment(R.layout.fragment_link_submission) {
    private lateinit var binding: FragmentLinkSubmissionBinding
    private lateinit var bottomSheetSubredditFlairFragment: BottomSheetSubredditFlairFragment
    private lateinit var redditAuthHelper: RedditAuthHelper
    private lateinit var redditApiHelper: RedditApiHelper

    private val  viewModel by viewModels<SubmissionViewModel> {
        ViewModelFactory(
            AppDatabase.getInstance(requireActivity().application).sharedDao,
            requireActivity().application
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        redditAuthHelper = (requireContext().applicationContext as App).kodein.instance()
        redditApiHelper = (requireContext().applicationContext as App).kodein.instance()
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
        if(subredditAndAccountPair != null && subredditAndAccountPair.first.linkFlairEnabled) {
            val listFlow = viewModel.getSubredditFlairListFlow(subredditAndAccountPair)

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
                bottomSheetSubredditFlairFragment.dismiss()
            },
            { v: View ->
                removeFlairClickListener(v)
                resetFlairToDefaultState()
                bottomSheetSubredditFlairFragment.dismiss()
            },
            listFlow)

            binding.addFlairChip.visibility = View.VISIBLE

            binding.addFlairChip.setOnClickListener {
                bottomSheetSubredditFlairFragment.show(childFragmentManager, "BottomSheetSubredditFlairFragment")
            }
        }
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