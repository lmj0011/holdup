package name.lmj0011.holdup.ui.submission.bottomsheet

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.coroutines.flow.SharedFlow
import name.lmj0011.holdup.App
import name.lmj0011.holdup.R
import name.lmj0011.holdup.databinding.BottomsheetFragmentSubredditSearchBinding
import name.lmj0011.holdup.helpers.RedditApiHelper
import name.lmj0011.holdup.helpers.RedditAuthHelper
import name.lmj0011.holdup.helpers.adapters.SubredditSearchListAdapter
import name.lmj0011.holdup.helpers.models.Subreddit
import name.lmj0011.holdup.ui.submission.bottomsheet.subredditsearch.RecentSubredditFragment
import name.lmj0011.holdup.ui.submission.bottomsheet.subredditsearch.SearchResultSubredditFragment
import org.kodein.di.instance

class BottomSheetSubredditSearchFragment(
    val setSubredditForSubmission: SubredditSearchListAdapter.SubredditSearchClickListener,
    val recentAndJoinedSubredditPair: Pair<SharedFlow<List<Subreddit>>, SharedFlow<List<Subreddit>>>):
    BottomSheetDialogFragment() {
    private lateinit var binding: BottomsheetFragmentSubredditSearchBinding
    private lateinit var redditAuthHelper: RedditAuthHelper
    private lateinit var redditApiHelper: RedditApiHelper

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        redditAuthHelper = (requireContext().applicationContext as App).kodein.instance()
        redditApiHelper = (requireContext().applicationContext as App).kodein.instance()
        return inflater.inflate(R.layout.bottomsheet_fragment_subreddit_search, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupBinding(view)
        setupObservers()
    }

    override fun onStart() {
        super.onStart()
        val sheetContainer = requireView().parent as? ViewGroup ?: return
        sheetContainer.layoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return BottomSheetDialog(requireContext(), theme).apply {
            behavior.state = BottomSheetBehavior.STATE_EXPANDED
            behavior.isDraggable = false
        }
    }

    private fun setupObservers() {}

    private fun setupBinding(view: View) {
        binding = BottomsheetFragmentSubredditSearchBinding.bind(view)
        binding.lifecycleOwner = this
        binding.subredditListPager.adapter = PagerAdapter(this)
        binding.subredditListPager.isUserInputEnabled = false // prevent swiping navigation ref: https://stackoverflow.com/a/55193815/2445763
    }

    inner class PagerAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {
        override fun getItemCount(): Int = 2

        override fun createFragment(position: Int): Fragment {
            // Return a NEW fragment instance
            return when (position) {
                0 -> RecentSubredditFragment(binding.subredditSearchView, binding.subredditListPager, setSubredditForSubmission, recentAndJoinedSubredditPair)
                1 -> SearchResultSubredditFragment(binding.subredditSearchView, binding.subredditListPager, setSubredditForSubmission)
                else -> error("invalid position!")
            }
        }
    }
}