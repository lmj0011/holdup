package name.lmj0011.holdup.ui.home

import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import name.lmj0011.holdup.App
import name.lmj0011.holdup.R
import name.lmj0011.holdup.database.AppDatabase
import name.lmj0011.holdup.databinding.FragmentHomeBinding
import name.lmj0011.holdup.helpers.RedditAuthHelper
import name.lmj0011.holdup.helpers.adapters.SubmissionListAdapter
import name.lmj0011.holdup.helpers.factories.ViewModelFactory
import name.lmj0011.holdup.ui.submission.SubmissionViewModel
import name.lmj0011.holdup.ui.submission.bottomsheet.BottomSheetSubmissionsFilterOptionsFragment
import org.kodein.di.instance

// extension function for LiveData
// ref: https://stackoverflow.com/a/54969114/2445763
fun <T> LiveData<T>.observeOnce(observer: Observer<T>) {
    observeForever(object : Observer<T> {
        override fun onChanged(t: T?) {
            observer.onChanged(t)
            removeObserver(this)
        }
    })
}

class HomeFragment : Fragment(R.layout.fragment_home) {

    private lateinit var binding: FragmentHomeBinding
    private val  homeViewModel by viewModels<HomeViewModel> {
        ViewModelFactory(AppDatabase.getInstance(requireActivity().application).sharedDao,
        requireActivity().application)
    }
    private lateinit var redditAuthHelper: RedditAuthHelper
    private lateinit var listAdapter: SubmissionListAdapter
    private lateinit var bottomSheetSubmissionsFilterOptionsFragment: BottomSheetSubmissionsFilterOptionsFragment

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        redditAuthHelper = (requireContext().applicationContext as App).kodein.instance()

        setHasOptionsMenu(true)
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
        binding.homeViewModel = homeViewModel

        bottomSheetSubmissionsFilterOptionsFragment = BottomSheetSubmissionsFilterOptionsFragment {
            findNavController().navigate(R.id.homeFragment)
        }
    }

    private fun setupRecyclerView() {
        val submissionViewModel = SubmissionViewModel.getNewInstance(
            AppDatabase.getInstance(requireActivity().application).sharedDao,
            requireActivity().application
        )

        listAdapter = SubmissionListAdapter(
            SubmissionListAdapter.ClickListener  {
                val action = HomeFragmentDirections.actionHomeFragmentToEditSubmissionFragment(it)
                findNavController().navigate(action)
            },
            submissionViewModel,
            this
        )

        val decor = DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL)
        binding.subredditList.addItemDecoration(decor)

        homeViewModel.submissions.observeOnce{ submissions ->
            listAdapter.submitList(submissions)
            listAdapter.notifyItemRangeChanged(0,100)
        }

        binding.subredditList.adapter = listAdapter
    }

    private fun setupSwipeToRefresh() {
        binding.swipeRefresh.setOnRefreshListener {
            findNavController().navigate(R.id.homeFragment)
            binding.swipeRefresh.isRefreshing = false
        }
    }

    private fun setupObservers() {}
}