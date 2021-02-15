package name.lmj0011.holdup.ui.home

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.observe
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import name.lmj0011.holdup.App
import name.lmj0011.holdup.R
import name.lmj0011.holdup.database.AppDatabase
import name.lmj0011.holdup.databinding.FragmentHomeBinding
import name.lmj0011.holdup.helpers.RedditAuthHelper
import name.lmj0011.holdup.helpers.adapters.SubmissionListAdapter
import name.lmj0011.holdup.helpers.factories.ViewModelFactory
import name.lmj0011.holdup.helpers.util.launchIO
import name.lmj0011.holdup.helpers.util.withUIContext
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
}