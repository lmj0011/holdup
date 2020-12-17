package name.lmj0011.redditdraftking.ui.submission

import android.annotation.SuppressLint
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.*
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.google.android.material.tabs.TabLayoutMediator
import name.lmj0011.redditdraftking.R
import name.lmj0011.redditdraftking.database.AppDatabase
import name.lmj0011.redditdraftking.helpers.models.Subreddit
import name.lmj0011.redditdraftking.databinding.FragmentSubmissionBinding
import name.lmj0011.redditdraftking.helpers.adapters.SubredditFlairListAdapter
import name.lmj0011.redditdraftking.helpers.adapters.SubredditSearchListAdapter
import name.lmj0011.redditdraftking.helpers.enums.SubmissionKind
import name.lmj0011.redditdraftking.ui.submission.bottomsheet.BottomSheetAccountsFragment
import name.lmj0011.redditdraftking.ui.submission.bottomsheet.BottomSheetSubredditSearchFragment

/**
 * Serves as the ParentFragment for other *SubmissionFragment
 */
class SubmissionFragment: Fragment(R.layout.fragment_submission) {
    private lateinit var binding: FragmentSubmissionBinding
    private lateinit var tabLayoutMediator: TabLayoutMediator
    private lateinit var viewModel: SubmissionViewModel
    private lateinit var optionsMenu: Menu

    lateinit var bottomSheetAccountsFragment: BottomSheetAccountsFragment
    lateinit var bottomSheetSubredditSearchFragment: BottomSheetSubredditSearchFragment

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = SubmissionViewModel.getInstance(
            AppDatabase.getInstance(requireActivity().application).sharedDao,
            requireActivity().application
        )

        setHasOptionsMenu(true)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupBinding(view)
        setupObservers()

        // TODO - select last Account or first in db and then call viewModel.setAccount()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        optionsMenu = menu
        optionsMenu.clear()
        inflater.inflate(R.menu.submission, optionsMenu)
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        super.onPrepareOptionsMenu(menu)
        viewModel.readyToPost().value!!.let {
            menu.getItem(0).isEnabled = it
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.action_post_submission -> {
                val layout = binding.submissionTabLayout
                optionsMenu.getItem(0).isEnabled = false

                layout.getTabAt(layout.selectedTabPosition)?.let {
                    when(it.text) {
                        "Link" -> {
                            viewModel.postSubmission(SubmissionKind.Link)
                        }
                        "Image" -> {
                            viewModel.postSubmission(SubmissionKind.Image)
                        }
                        "Video" -> {

                        }
                        "Text" -> {
                            viewModel.postSubmission(SubmissionKind.Self)
                        }
                        "Poll" -> {
                            viewModel.postSubmission(SubmissionKind.Poll)
                        }
                        else -> { optionsMenu.getItem(0).isEnabled = true }
                    }
                }
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
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

            viewModel.setSubredditPostRequirements(it)
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

            binding.chooseSubredditLinearLayout.visibility = View.VISIBLE

            viewModel.recentAndJoinedSubredditPair.postValue(Pair(viewModel.getRecentSubredditListFlow(), viewModel.getJoinedSubredditListFlow()))
        })

        viewModel.getSubmissionLinkTitle().observe(viewLifecycleOwner, {
            viewModel.validateSubmission(SubmissionKind.Link)
        })

        viewModel.getSubmissionLinkText().observe(viewLifecycleOwner, {
            viewModel.validateSubmission(SubmissionKind.Link)
        })

        viewModel.submissionSelfTitle.observe(viewLifecycleOwner, {
            viewModel.validateSubmission(SubmissionKind.Self)
        })

        viewModel.submissionSelfText.observe(viewLifecycleOwner, {
            viewModel.validateSubmission(SubmissionKind.Self)
        })

        viewModel.submissionImageTitle.observe(viewLifecycleOwner, {
            viewModel.validateSubmission(SubmissionKind.Image)
        })

        viewModel.submissionImageGallery.observe(viewLifecycleOwner, {
            viewModel.validateSubmission(SubmissionKind.Image)
        })

        viewModel.submissionPollTitle.observe(viewLifecycleOwner, {
            viewModel.validateSubmission(SubmissionKind.Poll)
        })

        viewModel.submissionPollBodyText.observe(viewLifecycleOwner, {
            viewModel.validateSubmission(SubmissionKind.Poll)
        })

        viewModel.submissionPollOptions.observe(viewLifecycleOwner, {
            viewModel.validateSubmission(SubmissionKind.Poll)
        })

        viewModel.submissionPollDuration.observe(viewLifecycleOwner, {
            viewModel.validateSubmission(SubmissionKind.Poll)
        })

        viewModel.readyToPost().observe(viewLifecycleOwner, {
            requireActivity().invalidateOptionsMenu()
        })

        viewModel.isSubmissionSuccessful.observe(viewLifecycleOwner, {
            if (it) {
                resetFlagsState()
            }
        })
    }

    private fun resetFlagsState() {
        binding.nsfwChip.isChecked = false
        binding.spoilerChip.isChecked = false
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
                bottomSheetAccountsFragment.dismiss()
            }

            bottomSheetAccountsFragment.show(childFragmentManager, "BottomSheetAccountsFragment")
        }

        binding.chooseSubredditLinearLayout.setOnClickListener {_ ->
            viewModel.getAccount().value?.let {
                bottomSheetSubredditSearchFragment = BottomSheetSubredditSearchFragment(
                    SubredditSearchListAdapter.SubredditSearchClickListener { sub ->
                        viewModel.setSubreddit(sub)
                        bottomSheetSubredditSearchFragment.dismiss()
                    },
                    viewModel.recentAndJoinedSubredditPair.value!!
                )
                bottomSheetSubredditSearchFragment.show(childFragmentManager, "BottomSheetSubredditSearchFragment")
            }
        }

        binding.toggleFlagsImageView.setOnClickListener {
            if(!binding.flagsChipGroup.isVisible) {
                resetFlagsState()
            }

            binding.flagsChipGroup.isVisible = !binding.flagsChipGroup.isVisible
        }
        
        binding.nsfwChip.setOnCheckedChangeListener { _, isChecked -> viewModel.setNsfwFlag(isChecked) }

        binding.spoilerChip.setOnCheckedChangeListener { _, isChecked ->  viewModel.setSpoilerFlag(isChecked) }
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
            LinkSubmissionFragment(
                SubredditFlairListAdapter.FlairItemClickListener { flair -> viewModel.subredditFlair.postValue(flair)}
            ) {
                viewModel.subredditFlair.postValue(null)
            })
        )

        if (subreddit.allowImages) {
            listOfTriples.add(
                Triple(
                    "Image",
                    requireContext().getDrawable(R.drawable.ic_baseline_image_24)!!,
                    ImageSubmissionFragment(
                        SubredditFlairListAdapter.FlairItemClickListener { flair -> viewModel.subredditFlair.postValue(flair)}
                    ) {
                        viewModel.subredditFlair.postValue(null)
                    }
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
                TextSubmissionFragment(
                    SubredditFlairListAdapter.FlairItemClickListener { flair ->
                        viewModel.subredditFlair.postValue(flair)
                    }
                ) {
                    viewModel.subredditFlair.postValue(null)
                }
            ),
        )

        if (subreddit.allowPolls) {
            listOfTriples.add(
                Triple(
                    "Poll",
                    requireContext().getDrawable(R.drawable.ic_baseline_poll_24)!!,
                    PollSubmissionFragment(
                        SubredditFlairListAdapter.FlairItemClickListener { flair ->
                            viewModel.subredditFlair.postValue(flair)
                        }
                    ) {
                        viewModel.subredditFlair.postValue(null)
                    }
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
                0 -> {
                    LinkSubmissionFragment(
                        SubredditFlairListAdapter.FlairItemClickListener { flair ->
                            viewModel.subredditFlair.postValue(flair)
                        }
                    ) {
                        viewModel.subredditFlair.postValue(null)
                    }
                }
                1 -> {
                    ImageSubmissionFragment(
                        SubredditFlairListAdapter.FlairItemClickListener { flair ->
                            viewModel.subredditFlair.postValue(flair)
                        }
                    ) {
                        viewModel.subredditFlair.postValue(null)
                    }
                }
                2 -> VideoSubmissionFragment()
                3 -> TextSubmissionFragment(
                    SubredditFlairListAdapter.FlairItemClickListener { flair ->
                        viewModel.subredditFlair.postValue(flair)
                    }
                ) {
                    viewModel.subredditFlair.postValue(null)
                }
                4 -> PollSubmissionFragment(
                    SubredditFlairListAdapter.FlairItemClickListener { flair ->
                        viewModel.subredditFlair.postValue(flair)
                    }
                ) {
                    viewModel.subredditFlair.postValue(null)
                }
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