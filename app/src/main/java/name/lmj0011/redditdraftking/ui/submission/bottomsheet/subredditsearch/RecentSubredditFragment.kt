package name.lmj0011.redditdraftking.ui.submission.bottomsheet.subredditsearch

import android.os.Bundle
import android.view.View
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.LiveData
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.viewpager2.widget.ViewPager2
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.withContext
import name.lmj0011.redditdraftking.App
import name.lmj0011.redditdraftking.R
import name.lmj0011.redditdraftking.database.AppDatabase
import name.lmj0011.redditdraftking.database.models.Account
import name.lmj0011.redditdraftking.databinding.FragmentRecentSubredditBinding
import name.lmj0011.redditdraftking.databinding.FragmentSearchResultSubredditBinding
import name.lmj0011.redditdraftking.helpers.RedditApiHelper
import name.lmj0011.redditdraftking.helpers.RedditAuthHelper
import name.lmj0011.redditdraftking.helpers.adapters.SubredditSearchListAdapter
import name.lmj0011.redditdraftking.helpers.factories.ViewModelFactory
import name.lmj0011.redditdraftking.helpers.models.Subreddit
import name.lmj0011.redditdraftking.helpers.util.launchIO
import name.lmj0011.redditdraftking.helpers.util.launchNow
import name.lmj0011.redditdraftking.helpers.util.launchUI
import name.lmj0011.redditdraftking.ui.submission.SubmissionViewModel
import name.lmj0011.redditdraftking.ui.submission.bottomsheet.BottomSheetViewModel
import org.kodein.di.instance
import timber.log.Timber

class RecentSubredditFragment(val searchView: SearchView,
                              val viewPager: ViewPager2,
                              val setSubredditForSubmission: SubredditSearchListAdapter.SubredditSearchClickListener,
                              val recentAndJoinedSubredditPair: Pair<SharedFlow<List<Subreddit>>, SharedFlow<List<Subreddit>>>):
    Fragment(R.layout.fragment_recent_subreddit) {
    private lateinit var binding: FragmentRecentSubredditBinding
    private lateinit var recentsListAdapter: SubredditSearchListAdapter
    private lateinit var joinedListAdapter: SubredditSearchListAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupBinding(view)
        setupRecyclerView()
        setupObservers()
    }

    override fun onResume() {
        super.onResume()
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                if(newText == null) return false

                if (newText.isNotBlank()) {
                    viewPager.setCurrentItem(1, true)
                }
                return false
            }
        })
    }

    private fun setupBinding(view: View) {
        binding = FragmentRecentSubredditBinding.bind(view)
        binding.lifecycleOwner = this
    }

    private fun setupRecyclerView() {
        recentsListAdapter = SubredditSearchListAdapter(setSubredditForSubmission)
        joinedListAdapter = SubredditSearchListAdapter(setSubredditForSubmission)

        val decor = DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL)
        binding.recentSubredditList.addItemDecoration(decor)
        binding.joinedSubredditList.addItemDecoration(decor)

        binding.recentSubredditList.adapter = recentsListAdapter
        binding.joinedSubredditList.adapter = joinedListAdapter

        launchUI {
            recentAndJoinedSubredditPair.first.collectLatest {
                recentsListAdapter.submitList(it)
                recentsListAdapter.notifyDataSetChanged()
            }
        }

        launchUI {
            recentAndJoinedSubredditPair.second.collectLatest {
                joinedListAdapter.submitList(it)
                joinedListAdapter.notifyDataSetChanged()
            }
        }
    }

    private fun setupObservers() {}

}