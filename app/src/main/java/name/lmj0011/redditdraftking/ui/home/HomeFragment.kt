package name.lmj0011.redditdraftking.ui.home

import android.os.Bundle
import android.util.JsonToken
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import com.squareup.moshi.*
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectLatest
import name.lmj0011.redditdraftking.App
import name.lmj0011.redditdraftking.R
import name.lmj0011.redditdraftking.database.AppDatabase
import name.lmj0011.redditdraftking.databinding.FragmentHomeBinding
import name.lmj0011.redditdraftking.helpers.NotificationHelper
import name.lmj0011.redditdraftking.helpers.RedditAuthHelper
import name.lmj0011.redditdraftking.helpers.adapters.JSONObjectAdapter
import name.lmj0011.redditdraftking.helpers.adapters.SubredditListAdapter
import name.lmj0011.redditdraftking.helpers.data.DraftsJsonResponse
import name.lmj0011.redditdraftking.helpers.factories.ViewModelFactory
import name.lmj0011.redditdraftking.helpers.util.launchIO
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okio.Buffer
import okio.IOException
import org.json.JSONException
import org.json.JSONObject
import org.kodein.di.instance
import timber.log.Timber

class HomeFragment : Fragment(R.layout.fragment_home) {

    private lateinit var binding: FragmentHomeBinding
    private val  homeViewModel by viewModels<HomeViewModel> {
        ViewModelFactory(AppDatabase.getInstance(requireActivity().application).sharedDao,
        requireActivity().application)
    }
    private lateinit var redditAuthHelper: RedditAuthHelper
    private lateinit var listAdapter: SubredditListAdapter

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
        listAdapter = SubredditListAdapter(
            SubredditListAdapter.SubredditClickListener  { sub ->
                val action = HomeFragmentDirections.actionNavigationHomeToSubredditDraftsFragment(sub.uuid)
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
            getDrafts()
        }
    }

    private fun setupObservers() {
        homeViewModel.subredditsWithDrafts.observe(viewLifecycleOwner, {
            listAdapter.submitList(it)
            listAdapter.notifyDataSetChanged()
        })
    }

    fun getDrafts() {
        val client = OkHttpClient()
        val moshi = Moshi.Builder()
            .add(JSONObjectAdapter)
            .add(KotlinJsonAdapterFactory())
            .build()

        val draftsJsonAdapter = moshi.adapter(DraftsJsonResponse::class.java)


        if(redditAuthHelper.authClient().hasSavedBearer()) {
            //refresh token
            redditAuthHelper.authClient().getSavedBearer().renewToken()
        } else return

        val request = Request.Builder()
            .url("https://oauth.reddit.com/api/v1/drafts.json?raw_json=1")
            .header("Authorization", "Bearer ${redditAuthHelper.authClient().getSavedBearer().getAccessToken()}")
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