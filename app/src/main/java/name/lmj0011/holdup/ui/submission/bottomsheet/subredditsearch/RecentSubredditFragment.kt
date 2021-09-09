package name.lmj0011.holdup.ui.submission.bottomsheet.subredditsearch

import android.os.Bundle
import android.view.View
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.ConcatAdapter
import androidx.viewpager2.widget.ViewPager2
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.collectLatest
import name.lmj0011.holdup.R
import name.lmj0011.holdup.databinding.FragmentRecentSubredditBinding
import name.lmj0011.holdup.helpers.adapters.JoinedSubsHeaderListAdapter
import name.lmj0011.holdup.helpers.adapters.RecentSubsHeaderListAdapter
import name.lmj0011.holdup.helpers.adapters.SubredditSearchListAdapter
import name.lmj0011.holdup.helpers.models.Subreddit
import name.lmj0011.holdup.helpers.util.launchIO
import name.lmj0011.holdup.helpers.util.withUIContext

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
        binding.lifecycleOwner = viewLifecycleOwner
    }

    private fun setupRecyclerView() {
        val concatAdapter = ConcatAdapter()
        val recentSubsHeaderListAdapter = RecentSubsHeaderListAdapter()
        val joinedSubsHeaderListAdapter = JoinedSubsHeaderListAdapter()
        recentsListAdapter = SubredditSearchListAdapter(setSubredditForSubmission)
        joinedListAdapter = SubredditSearchListAdapter(setSubredditForSubmission)

        concatAdapter.apply {
            addAdapter(recentSubsHeaderListAdapter)
            addAdapter(recentsListAdapter)
            addAdapter(joinedSubsHeaderListAdapter)
            addAdapter(joinedListAdapter)
        }

        binding.recentAndJoinedSubredditList.adapter = concatAdapter

        launchIO {
            recentAndJoinedSubredditPair.first.collectLatest { list ->
                withUIContext {
                    if(list.isEmpty()) {
                        concatAdapter.apply {
                            concatAdapter.removeAdapter(recentSubsHeaderListAdapter)
                            concatAdapter.removeAdapter(recentsListAdapter)
                        }
                    }  else recentsListAdapter.submitList(list)
                }
            }
        }

        launchIO {
            recentAndJoinedSubredditPair.second.collectLatest { list ->
                withUIContext {
                    if(list.isEmpty()) {
                        concatAdapter.apply {
                            removeAdapter(joinedSubsHeaderListAdapter)
                            removeAdapter(joinedListAdapter)
                        }
                    }  else joinedListAdapter.submitList(list)
                }
            }
        }
    }

    private fun setupObservers() {}

}