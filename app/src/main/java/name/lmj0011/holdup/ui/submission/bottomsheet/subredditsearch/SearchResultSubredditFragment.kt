package name.lmj0011.holdup.ui.submission.bottomsheet.subredditsearch

import android.os.Bundle
import android.view.View
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.viewpager2.widget.ViewPager2
import name.lmj0011.holdup.App
import name.lmj0011.holdup.R
import name.lmj0011.holdup.database.AppDatabase
import name.lmj0011.holdup.databinding.FragmentSearchResultSubredditBinding
import name.lmj0011.holdup.helpers.RedditApiHelper
import name.lmj0011.holdup.helpers.RedditAuthHelper
import name.lmj0011.holdup.helpers.adapters.SubredditSearchListAdapter
import name.lmj0011.holdup.helpers.factories.ViewModelFactory
import name.lmj0011.holdup.helpers.util.launchIO
import name.lmj0011.holdup.helpers.util.withUIContext
import name.lmj0011.holdup.ui.submission.SubmissionViewModel
import org.kodein.di.instance
import timber.log.Timber

class SearchResultSubredditFragment(val searchView: SearchView,
                                    val viewPager: ViewPager2,
                                    val setSubredditForSubmission: SubredditSearchListAdapter.SubredditSearchClickListener):
    Fragment(R.layout.fragment_search_result_subreddit) {
    private lateinit var binding: FragmentSearchResultSubredditBinding
    private lateinit var redditAuthHelper: RedditAuthHelper
    private lateinit var redditApiHelper: RedditApiHelper
    private lateinit var listAdapter: SubredditSearchListAdapter
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
        Timber.d("${viewModel.getAccount()}")
        setupBinding(view)
        setupRecyclerView()
        setupObservers()
    }

    override fun onResume() {
        super.onResume()

        fetchSearchResults(searchView.query.toString())
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                fetchSearchResults(newText)
                return false
            }
        })
    }

    private fun setupBinding(view: View) {
        binding = FragmentSearchResultSubredditBinding.bind(view)
        binding.lifecycleOwner = viewLifecycleOwner
    }

    private fun setupObservers() {}

    private fun setupRecyclerView() {
        listAdapter = SubredditSearchListAdapter(setSubredditForSubmission)

        val decor = DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL)
        binding.searchResultSubredditList.addItemDecoration(decor)
        binding.searchResultSubredditList.adapter = listAdapter
        fetchSearchResults(searchView.query.toString())
    }

    private fun fetchSearchResults(queryText: String?) {
        if(queryText != null && queryText.isNotBlank()) {
            launchIO {

                // clear list
                withUIContext {
                    listAdapter.submitList(mutableListOf())
                    listAdapter.notifyDataSetChanged()
                }

                // populate list
                viewModel.getAccounts().first()?.let {
                    val list = redditApiHelper.submitSubredditQuery(queryText, redditAuthHelper.authClient(it).getSavedBearer().getAccessToken()!!)

                    withUIContext {
                        listAdapter.submitList(list)
                        listAdapter.notifyDataSetChanged()
                    }
                }
            }
        } else viewPager.setCurrentItem(0, true)
    }

}