package name.lmj0011.holdup.ui.home

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.work.WorkInfo
import androidx.work.WorkManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import name.lmj0011.holdup.App
import name.lmj0011.holdup.R
import name.lmj0011.holdup.database.AppDatabase
import name.lmj0011.holdup.databinding.FragmentHomeBinding
import name.lmj0011.holdup.helpers.DataStoreHelper
import name.lmj0011.holdup.helpers.RedditAuthHelper
import name.lmj0011.holdup.helpers.adapters.SubmissionListAdapter
import name.lmj0011.holdup.helpers.util.launchIO
import name.lmj0011.holdup.helpers.util.withUIContext
import name.lmj0011.holdup.ui.submission.SubmissionViewModel
import name.lmj0011.holdup.ui.submission.bottomsheet.BottomSheetSubmissionsFilterOptionsFragment
import org.kodein.di.instance
import java.lang.IllegalArgumentException
import java.util.*

class HomeFragment : Fragment(R.layout.fragment_home) {

    private lateinit var binding: FragmentHomeBinding
    private lateinit var homeViewModel: HomeViewModel
    private lateinit var redditAuthHelper: RedditAuthHelper
    private lateinit var dataStoreHelper: DataStoreHelper
    private lateinit var listAdapter: SubmissionListAdapter
    private lateinit var bottomSheetSubmissionsFilterOptionsFragment: BottomSheetSubmissionsFilterOptionsFragment
    private var publishSubmissionObserverJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        homeViewModel = HomeViewModel(
            AppDatabase.getInstance(requireActivity().application).sharedDao,
            requireActivity().application
        )
        redditAuthHelper = (requireContext().applicationContext as App).kodein.instance()
        dataStoreHelper = (requireContext().applicationContext as App).kodein.instance()

        setHasOptionsMenu(true)
    }

    override fun onResume() {
        super.onResume()

        refreshRecyclerView()

        publishSubmissionObserverJob = launchIO {
            val id = dataStoreHelper.getPublishScheduledSubmissionWorkerId().first()

            withUIContext {
                try{
                    WorkManager.getInstance(requireContext())
                        .getWorkInfoByIdLiveData(UUID.fromString(id))
                        .observe(viewLifecycleOwner) { workInfo: WorkInfo ->
                            if (workInfo.state.isFinished) {
                                refreshRecyclerView()
                            }
                        }
                } catch(ex: IllegalArgumentException) {
                    // do nothing
                }
            }
        }
    }

    override fun onStop() {
        super.onStop()
        publishSubmissionObserverJob?.cancel()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupBinding(view)
        setupRecyclerView()
        setupObservers()
        setupSwipeToRefresh()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.home, menu)
    }


    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.action_filer_posts -> {
                bottomSheetSubmissionsFilterOptionsFragment
                    .show(childFragmentManager, "BottomSheetSubmissionsFilterOptionsFragment")
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun setupBinding(view: View) {
        binding = FragmentHomeBinding.bind(view)
        binding.lifecycleOwner = viewLifecycleOwner

        bottomSheetSubmissionsFilterOptionsFragment = BottomSheetSubmissionsFilterOptionsFragment {
            findNavController().navigate(R.id.homeFragment)
        }
    }

    private fun setupRecyclerView() {
        listAdapter = SubmissionListAdapter(
            SubmissionListAdapter.ClickListener  {
                val action = HomeFragmentDirections.actionHomeFragmentToEditSubmissionFragment(it)
                findNavController().navigate(action)
            },
            this
        )

        val decor = DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL)
        binding.subredditList.addItemDecoration(decor)

        refreshRecyclerView()

        binding.subredditList.adapter = listAdapter
    }

    private fun setupSwipeToRefresh() {
        binding.swipeRefresh.setOnRefreshListener {
            refreshRecyclerView()
            binding.swipeRefresh.isRefreshing = false
        }
    }

    private fun refreshRecyclerView() {
        /**
         * Haven't figured out why yet, but calling a new instance of this viewModel prevents the
         * recyclerview's viewholders from showing an empty ViewPager where there should be content
         * whenever the user
         */
        SubmissionViewModel.getNewInstance(
            AppDatabase.getInstance(requireActivity().application).sharedDao,
            requireActivity().application
        )
        /**
         *
         */

        homeViewModel.submissions.refresh()
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun setupObservers() {
        homeViewModel.submissions.observe(viewLifecycleOwner, { submissions ->
            listAdapter.submitList(submissions)
            listAdapter.notifyDataSetChanged()
        })
    }
}