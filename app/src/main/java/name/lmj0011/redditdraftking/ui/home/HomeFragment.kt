package name.lmj0011.redditdraftking.ui.home

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.observe
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import com.squareup.moshi.*
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import name.lmj0011.redditdraftking.App
import name.lmj0011.redditdraftking.R
import name.lmj0011.redditdraftking.database.AppDatabase
import name.lmj0011.redditdraftking.database.models.Submission
import name.lmj0011.redditdraftking.databinding.FragmentHomeBinding
import name.lmj0011.redditdraftking.helpers.RedditAuthHelper
import name.lmj0011.redditdraftking.helpers.adapters.JSONObjectAdapter
import name.lmj0011.redditdraftking.helpers.adapters.SubmissionListAdapter
import name.lmj0011.redditdraftking.helpers.adapters.SubredditListAdapter
import name.lmj0011.redditdraftking.helpers.models.DraftsJsonResponse
import name.lmj0011.redditdraftking.helpers.factories.ViewModelFactory
import name.lmj0011.redditdraftking.helpers.util.launchIO
import name.lmj0011.redditdraftking.helpers.util.withUIContext
import name.lmj0011.redditdraftking.ui.submission.EditSubmissionFragment
import okhttp3.OkHttpClient
import okhttp3.Request
import okio.IOException
import org.kodein.di.instance

class HomeFragment : Fragment(R.layout.fragment_home) {

    private lateinit var binding: FragmentHomeBinding
    private val  homeViewModel by viewModels<HomeViewModel> {
        ViewModelFactory(AppDatabase.getInstance(requireActivity().application).sharedDao,
        requireActivity().application)
    }
    private lateinit var redditAuthHelper: RedditAuthHelper
    private lateinit var listAdapter: SubmissionListAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        redditAuthHelper = (requireContext().applicationContext as App).kodein.instance()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupBinding(view)
        setupRecyclerView()
        setupObservers()
        setupSwipeToRefresh()
        refreshRecyclerView()
    }

    override fun onResume() {
        super.onResume()
        refreshRecyclerView()
    }

    private fun setupBinding(view: View) {
        binding = FragmentHomeBinding.bind(view)
        binding.lifecycleOwner = this
        binding.homeViewModel = homeViewModel
    }

    private fun setupRecyclerView() {
        listAdapter = SubmissionListAdapter(
            SubmissionListAdapter.ClickListener  {
                val action = HomeFragmentDirections.actionHomeFragmentToEditSubmissionFragment(it)
                findNavController().navigate(action)
            }
        )

        val decor = DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL)
        binding.subredditList.addItemDecoration(decor)
        binding.subredditList.adapter = listAdapter
    }

    private fun setupSwipeToRefresh() {
        binding.swipeRefresh.setOnRefreshListener {
            refreshRecyclerView()
            binding.swipeRefresh.isRefreshing = false
        }
    }

    private fun refreshRecyclerView() {
        launchIO {
           val list = homeViewModel.getSubmissions()

            withUIContext {
                listAdapter.submitList(list)
                listAdapter.notifyDataSetChanged()
            }
        }
    }

    private fun setupObservers() {
        homeViewModel.submissions.observe(viewLifecycleOwner) {
            listAdapter.submitList(it)
            listAdapter.notifyDataSetChanged()
        }
    }

    fun getDrafts() {
        val client = OkHttpClient()
        val moshi = Moshi.Builder()
            .add(JSONObjectAdapter)
            .add(KotlinJsonAdapterFactory())
            .build()

        val draftsJsonAdapter = moshi.adapter(DraftsJsonResponse::class.java)

        homeViewModel.database.getAllAccounts().forEach { acct ->
            if(redditAuthHelper.authClient(acct).hasSavedBearer()) {
                //refresh token
                redditAuthHelper.authClient(acct).getSavedBearer().renewToken()
            } else return

            val request = Request.Builder()
                .url("https://oauth.reddit.com/api/v1/drafts.json?raw_json=1")
                .header("Authorization", "Bearer ${redditAuthHelper.authClient(acct).getSavedBearer().getAccessToken()}")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) throw IOException("Unexpected code $response")

                val res = draftsJsonAdapter.fromJson(response.body!!.source())

                res!!.subreddits.forEach {
                    homeViewModel.insertSubreddit(it)
                }

                res!!.drafts.forEach {
                    homeViewModel.insertDraft(it)
                }
            }
        }
    }
}