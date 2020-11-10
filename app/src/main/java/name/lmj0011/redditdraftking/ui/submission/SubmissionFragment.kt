package name.lmj0011.redditdraftking.ui.submission

import android.annotation.SuppressLint
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.google.android.material.tabs.TabLayoutMediator
import kotlinx.coroutines.flow.SharedFlow
import name.lmj0011.redditdraftking.App
import name.lmj0011.redditdraftking.R
import name.lmj0011.redditdraftking.database.AppDatabase
import name.lmj0011.redditdraftking.helpers.models.Subreddit
import name.lmj0011.redditdraftking.databinding.FragmentSubmissionBinding
import name.lmj0011.redditdraftking.helpers.RedditApiHelper
import name.lmj0011.redditdraftking.helpers.RedditAuthHelper
import name.lmj0011.redditdraftking.helpers.adapters.SubredditSearchListAdapter
import name.lmj0011.redditdraftking.helpers.factories.ViewModelFactory
import name.lmj0011.redditdraftking.ui.submission.bottomsheet.BottomSheetAccountsFragment
import name.lmj0011.redditdraftking.ui.submission.bottomsheet.BottomSheetSubredditSearchFragment
import org.kodein.di.instance

/**
 * Serves as the ParentFragment for other *SubmissionFragment
 */
class SubmissionFragment: Fragment(R.layout.fragment_submission) {
    private lateinit var binding: FragmentSubmissionBinding
    private lateinit var redditAuthHelper: RedditAuthHelper
    private lateinit var redditApiHelper: RedditApiHelper
    private lateinit var tabLayoutMediator: TabLayoutMediator

    private var recentAndJoinedSubredditPair: Pair<SharedFlow<List<Subreddit>>, SharedFlow<List<Subreddit>>>? = null
    private val  viewModel by viewModels<SubmissionViewModel> {
        ViewModelFactory(
            AppDatabase.getInstance(requireActivity().application).sharedDao,
            requireActivity().application
        )
    }

    lateinit var bottomSheetAccountsFragment: BottomSheetAccountsFragment
    lateinit var bottomSheetSubredditSearchFragment: BottomSheetSubredditSearchFragment

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        redditAuthHelper = (requireContext().applicationContext as App).kodein.instance()
        redditApiHelper = (requireContext().applicationContext as App).kodein.instance()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupBinding(view)
        setupObservers()
    }

    private fun setupObservers() {
        viewModel.getSubreddit().observe(viewLifecycleOwner, {
            binding.chooseSubredditTextView.text = it.displayNamePrefixed
            Glide
                .with(this)
                .load(it.iconImgUrl)
                .apply(RequestOptions().override(100))
                .circleCrop()
                .error(R.mipmap.ic_default_subreddit_icon_round)
                .into(binding.chooseSubredditImageView)

            reattachTabLayoutMediator(it)
        })

        viewModel.getAccount().observe(viewLifecycleOwner, {
            binding.chooseAccountTextView.text = it.name
            Glide
                .with(this)
                .load(it.iconImage)
                .apply(RequestOptions().override(100))
                .circleCrop()
                .error(R.drawable.ic_baseline_image_24)
                .into(binding.chooseAccountImageView)
        })
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    private fun setupBinding(view: View) {
        binding = FragmentSubmissionBinding.bind(view)
        binding.lifecycleOwner = this
        binding.submissionPager.adapter = TabCollectionAdapterDefault(this)
        binding.submissionPager.isUserInputEnabled = false // prevent swiping navigation ref: https://stackoverflow.com/a/55193815/2445763
        tabLayoutMediator = TabLayoutMediator(binding.submissionTabLayout, binding.submissionPager) { tab, position ->
            when(position) {
                0 -> {
                    tab.text = "Link"
                    tab.icon = requireContext().getDrawable(R.drawable.ic_baseline_link_24)
                }
                1 -> {
                    tab.text = "Image"
                    tab.icon = requireContext().getDrawable(R.drawable.ic_baseline_image_24)
                }
                2 -> {
                    tab.text = "Video"
                    tab.icon = requireContext().getDrawable(R.drawable.ic_baseline_videocam_24)
                }
                3 -> {
                    tab.text = "Text"
                    tab.icon = requireContext().getDrawable(R.drawable.ic_baseline_text_snippet_24)
                }
                4 -> {
                    tab.text = "Poll"
                    tab.icon = requireContext().getDrawable(R.drawable.ic_baseline_poll_24)
                }
            }
        }

        tabLayoutMediator.attach()

        binding.chooseAccountLinearLayout.setOnClickListener {
            bottomSheetAccountsFragment = BottomSheetAccountsFragment { acct ->
                viewModel.setAccount(acct)
                recentAndJoinedSubredditPair = Pair(viewModel.getRecentSubredditListFlow(acct), viewModel.getJoinedSubredditListFlow(acct))
                bottomSheetAccountsFragment.dismiss()
            }

            bottomSheetAccountsFragment.show(childFragmentManager, "BottomSheetAccountsFragment")
        }

        binding.chooseSubredditLinearLayout.setOnClickListener {_ ->
            if(viewModel.getAccount().value != null && recentAndJoinedSubredditPair != null) {
                viewModel.getAccount().value?.let {acct ->
                    bottomSheetSubredditSearchFragment = BottomSheetSubredditSearchFragment(
                        SubredditSearchListAdapter.SubredditSearchClickListener { sub ->
                            viewModel.setSubreddit(sub)
                            bottomSheetSubredditSearchFragment.dismiss()
                        },
                        recentAndJoinedSubredditPair!!
                    )
                    bottomSheetSubredditSearchFragment.show(childFragmentManager, "BottomSheetSubredditSearchFragment")
                }
            } else {
                // TODO show toast asking user to select an account first
            }
        }
    }

    /**
     * Reconfigures the TabLayout based on this Subreddit allowed Post types
     */
    private fun reattachTabLayoutMediator(subreddit: Subreddit) {
        val tabCollection = getTabCollectionTriples(subreddit)

        tabLayoutMediator.detach()
        binding.submissionPager.adapter = TabCollectionAdapterDynamic(this, tabCollection)

        tabLayoutMediator = TabLayoutMediator(binding.submissionTabLayout, binding.submissionPager) { tab, position ->
            tab.text = tabCollection[position].first
            tab.icon = tabCollection[position].second
        }

        tabLayoutMediator.attach()
    }

    private fun getTabCollectionTriples(subreddit: Subreddit): List<Triple<String, Drawable, Fragment>> {
        var listOfTriples = mutableListOf<Triple<String, Drawable, Fragment>>()

        listOfTriples.add(
            Triple(
            "Link",
            requireContext().getDrawable(R.drawable.ic_baseline_link_24)!!,
            LinkSubmissionFragment()
            ),
        )

        if (subreddit.allowImages) {
            listOfTriples.add(
                Triple(
                    "Image",
                    requireContext().getDrawable(R.drawable.ic_baseline_image_24)!!,
                    ImageSubmissionFragment()
                ),
            )

        }

        if (subreddit.allowVideos || subreddit.allowVideoGifs) {
            listOfTriples.add(
                Triple(
                    "Video",
                    requireContext().getDrawable(R.drawable.ic_baseline_videocam_24)!!,
                    VideoSubmissionFragment()
                ),
            )
        }

        listOfTriples.add(
            Triple(
                "Text",
                requireContext().getDrawable(R.drawable.ic_baseline_text_snippet_24)!!,
                TextSubmissionFragment()
            ),
        )

        if (subreddit.allowPolls) {
            listOfTriples.add(
                Triple(
                    "Poll",
                    requireContext().getDrawable(R.drawable.ic_baseline_poll_24)!!,
                    PollSubmissionFragment()
                ),
            )
        }

        return listOfTriples.toList()
    }

    inner class TabCollectionAdapterDefault(fragment: Fragment) : FragmentStateAdapter(fragment) {
        override fun getItemCount(): Int = 5

        override fun createFragment(position: Int): Fragment {
            // Return a NEW fragment instance
            return when(position) {
                0 -> LinkSubmissionFragment()
                1 -> ImageSubmissionFragment()
                2 -> VideoSubmissionFragment()
                3 -> TextSubmissionFragment()
                4 -> PollSubmissionFragment()
                else -> throw Exception("Unknown Tab Position!")
            }
        }
    }

    inner class TabCollectionAdapterDynamic(fragment: Fragment, val tabCollection: List<Triple<String, Drawable, Fragment>>) : FragmentStateAdapter(fragment) {

        override fun getItemCount(): Int = tabCollection.size

        override fun createFragment(position: Int): Fragment {
            // Return a NEW fragment instance
            return tabCollection[position].third
        }
    }
}