package name.lmj0011.holdup.ui.submission

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.AppCompatCheckedTextView
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.tabs.TabLayoutMediator
import kotlinx.coroutines.flow.collectLatest
import name.lmj0011.holdup.App
import name.lmj0011.holdup.BaseFragment
import name.lmj0011.holdup.R
import name.lmj0011.holdup.database.AppDatabase
import name.lmj0011.holdup.databinding.FragmentSubmissionBinding
import name.lmj0011.holdup.helpers.DataStoreHelper
import name.lmj0011.holdup.helpers.DateTimeHelper.getElapsedTimeUntilFutureTime
import name.lmj0011.holdup.helpers.DateTimeHelper.getLocalDateFromUtcMillis
import name.lmj0011.holdup.helpers.UniqueRuntimeNumberHelper
import name.lmj0011.holdup.helpers.adapters.SubredditFlairListAdapter
import name.lmj0011.holdup.helpers.adapters.SubredditSearchListAdapter
import name.lmj0011.holdup.helpers.enums.SubmissionKind
import name.lmj0011.holdup.helpers.interfaces.BaseFragmentInterface
import name.lmj0011.holdup.helpers.models.Subreddit
import name.lmj0011.holdup.helpers.receivers.PublishScheduledSubmissionReceiver
import name.lmj0011.holdup.helpers.util.buildOneColorStateList
import name.lmj0011.holdup.helpers.util.launchIO
import name.lmj0011.holdup.helpers.util.launchUI
import name.lmj0011.holdup.helpers.util.showSnackBar
import name.lmj0011.holdup.helpers.util.withUIContext
import name.lmj0011.holdup.ui.submission.bottomsheet.BottomSheetAccountsFragment
import name.lmj0011.holdup.ui.submission.bottomsheet.BottomSheetSubredditFlairFragment
import name.lmj0011.holdup.ui.submission.bottomsheet.BottomSheetSubredditSearchFragment
import org.kodein.di.instance
import timber.log.Timber

/**
 * Serves as the ParentFragment for other *SubmissionFragment
 */
class SubmissionFragment: BaseFragment(R.layout.fragment_submission), BaseFragmentInterface {
    private lateinit var binding: FragmentSubmissionBinding
    private lateinit var tabLayoutMediator: TabLayoutMediator
    private lateinit var viewModel: SubmissionViewModel
    private lateinit var dataStoreHelper: DataStoreHelper
    private lateinit var alarmMgr: AlarmManager
    private lateinit var requestCodeHelper: UniqueRuntimeNumberHelper

    lateinit var bottomSheetAccountsFragment: BottomSheetAccountsFragment
    lateinit var bottomSheetSubredditSearchFragment: BottomSheetSubredditSearchFragment
    lateinit var bottomSheetSubredditFlairFragment: BottomSheetSubredditFlairFragment
    lateinit var enableInboxReplies: AppCompatCheckedTextView

    private var optionsMenu: Menu? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = SubmissionViewModel.getInstance(
            AppDatabase.getInstance(requireActivity().application).sharedDao,
            requireActivity().application
        )

        dataStoreHelper = (requireContext().applicationContext as App).kodein.instance()
        requestCodeHelper = (requireContext().applicationContext as App).kodein.instance()
        alarmMgr = requireContext().getSystemService(Context.ALARM_SERVICE) as AlarmManager

        setHasOptionsMenu(true)
    }

    override fun onResume() {
        super.onResume()
        viewModel.setAccount()

        /**
         * TEST CODE
         */
//        val dao = AppDatabase.getInstance(requireActivity().application).sharedDao
//        val tHelper = TestHelper(requireContext())
//
//        launchIO {
//            dao.deleteAllSubmissionId() // clear submissions table
//
//            val postAtUtcMillis = 1612916400602
//            val submissions = tHelper.generateSubmissions(
//                listOf(
//                    Pair(SubmissionKind.Self, postAtUtcMillis),
//                    Pair(SubmissionKind.Self, postAtUtcMillis),
//                    Pair(SubmissionKind.Self, postAtUtcMillis),
//                    Pair(SubmissionKind.Self, postAtUtcMillis),
//                    Pair(SubmissionKind.Self, postAtUtcMillis),
//                    Pair(SubmissionKind.Self, postAtUtcMillis),
//                    Pair(SubmissionKind.Self, postAtUtcMillis),
//                    Pair(SubmissionKind.Self, postAtUtcMillis),
//                    Pair(SubmissionKind.Self, postAtUtcMillis),
//                    Pair(SubmissionKind.Self, postAtUtcMillis),
//                )
//            )
//
//            dao.insertAllSubmissions(submissions)
//
//            withUIContext {
//
//                submissions.forEach { sub ->
//                    val alarmIntent = Intent(context, PublishScheduledSubmissionReceiver::class.java).let { intent ->
//                        intent.putExtra("alarmRequestCode", sub.alarmRequestCode)
//                        PendingIntent.getBroadcast(context, sub.alarmRequestCode, intent, 0)
//                    }
//
//                    val futureElapsedDate = getLocalDateFromUtcMillis(
//                        getElapsedTimeUntilFutureTime(
//                            sub.postAtMillis
//                        )
//                    )
//
//                    if (futureElapsedDate != null) {
//                        alarmMgr.setExactAndAllowWhileIdle(
//                            AlarmManager.ELAPSED_REALTIME_WAKEUP,
//                            futureElapsedDate.time,
//                            alarmIntent
//                        )
//                        Timber.d("alarm set for Submission: ${getPostAtDateForListLayout(sub)}")
//                    } else {
//                        Timber.e("failed to set alarm!")
//                    }
//                }
//
//            }
//        }
        /**
         *
         */
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupBinding(view)
        setupObservers()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        optionsMenu = menu
        optionsMenu?.clear()
        inflater.inflate(R.menu.submission, menu)
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        super.onPrepareOptionsMenu(menu)
        optionsMenu = menu
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

                layout.getTabAt(layout.selectedTabPosition)?.let {
                    showPostConfirmationDialog(it.text.toString())
                }
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun setupObservers() {
        viewModel.sendReplies.observe(viewLifecycleOwner, {
            if(::enableInboxReplies.isInitialized) enableInboxReplies.isChecked = it
        })

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

            val listFlow = viewModel.getSubredditFlairListFlow()

            bottomSheetSubredditFlairFragment = BottomSheetSubredditFlairFragment(
                SubredditFlairListAdapter.FlairItemClickListener { flair ->
                    binding.addFlairChip.text = flair.text
                    try {
                        buildOneColorStateList(Color.parseColor(flair.backGroundColor))?.let {
                            binding.addFlairChip.chipBackgroundColor = it
                        }
                    } catch (ex: java.lang.Exception) { /* flair.backGroundColor was either null or not a recognizable color */
                    }

                    when (flair.textColor) {
                        "light" -> binding.addFlairChip.setTextColor(Color.WHITE)
                        else -> binding.addFlairChip.setTextColor(Color.DKGRAY)
                    }

                    viewModel.subredditFlair.postValue(flair)
                    viewModel.validateSubmission(getSelectedTabPositionSubmissionType())
                    bottomSheetSubredditFlairFragment.dismiss()
                },
                { v: View ->
                    viewModel.subredditFlair.postValue(null)
                    resetFlairToDefaultState()
                    viewModel.validateSubmission(getSelectedTabPositionSubmissionType())
                    bottomSheetSubredditFlairFragment.dismiss()
                },
                listFlow
            )

            launchUI {
                listFlow.collectLatest { list ->
                    Timber.d("list: $list")

                    if (list.isNotEmpty()) {
                        binding.addFlairChip.visibility = View.VISIBLE

                        binding.addFlairChip.setOnClickListener {
                            bottomSheetSubredditFlairFragment.show(
                                childFragmentManager,
                                "BottomSheetSubredditFlairFragment"
                            )
                        }
                    } else {
                        binding.addFlairChip.visibility = View.GONE
                    }
                }
            }
        })


        viewModel.getAccount().observe(viewLifecycleOwner, {
            binding.chooseAccountTextView.text = it.name
            launchIO { dataStoreHelper.setSelectedAccountUsername(it.name) }

            Glide
                .with(this)
                .load(it.iconImage)
                .apply(RequestOptions().override(100))
                .circleCrop()
                .error(R.drawable.ic_baseline_image_24)
                .into(binding.chooseAccountImageView)

            binding.chooseSubredditLinearLayout.visibility = View.VISIBLE

            viewModel.recentAndJoinedSubredditPair.postValue(
                Pair(
                    viewModel.getRecentSubredditListFlow(),
                    viewModel.getJoinedSubredditListFlow()
                )
            )
        })


        binding.titleEditTextView.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {}

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                viewModel.submissionTitle.postValue(s.toString())
            }
        })

        viewModel.submissionTitle.observe(viewLifecycleOwner, {
            viewModel.validateSubmission(getSelectedTabPositionSubmissionType())
        })

        viewModel.getSubmissionLinkText().observe(viewLifecycleOwner, {
            viewModel.validateSubmission(getSelectedTabPositionSubmissionType())
        })

        viewModel.submissionSelfText.observe(viewLifecycleOwner, {
            viewModel.validateSubmission(getSelectedTabPositionSubmissionType())
        })

        viewModel.submissionImageGallery.observe(viewLifecycleOwner, {
            viewModel.validateSubmission(getSelectedTabPositionSubmissionType())
        })

        viewModel.submissionPollBodyText.observe(viewLifecycleOwner, {
            viewModel.validateSubmission(getSelectedTabPositionSubmissionType())
        })

        viewModel.submissionPollOptions.observe(viewLifecycleOwner, {
            viewModel.validateSubmission(getSelectedTabPositionSubmissionType())
        })

        viewModel.submissionPollDuration.observe(viewLifecycleOwner, {
            viewModel.validateSubmission(getSelectedTabPositionSubmissionType())
        })

        viewModel.submissionVideo.observe(viewLifecycleOwner, {
            viewModel.validateSubmission(getSelectedTabPositionSubmissionType())
        })

        viewModel.readyToPost().observe(viewLifecycleOwner, {
            requireActivity().invalidateOptionsMenu()
        })

        viewModel.isSubmissionSuccessful.observe(viewLifecycleOwner, {
            if (it) {
                optionsMenu?.getItem(0)?.isEnabled = false
                clearUserInputViews()
                resetFlairToDefaultState()
                resetFlagsState()
            }
        })
    }

    override fun setupRecyclerView() {}

    private fun resetFlagsState() {
        binding.nsfwChip.isChecked = false
        binding.spoilerChip.isChecked = false
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    override fun setupBinding(view: View) {
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

        binding.chooseSubredditLinearLayout.setOnClickListener { _ ->
            viewModel.getAccount().value?.let {
                bottomSheetSubredditSearchFragment = BottomSheetSubredditSearchFragment(
                    SubredditSearchListAdapter.SubredditSearchClickListener { sub ->
                        viewModel.setSubreddit(sub)
                        bottomSheetSubredditSearchFragment.dismiss()
                    },
                    viewModel.recentAndJoinedSubredditPair.value!!
                )
                bottomSheetSubredditSearchFragment.show(
                    childFragmentManager,
                    "BottomSheetSubredditSearchFragment"
                )
            }
        }

        binding.toggleFlagsImageView.setOnClickListener {
            if(!binding.flagsChipGroup.isVisible) {
                resetFlagsState()
            }

            binding.flagsChipGroup.isVisible = !binding.flagsChipGroup.isVisible
        }
        
        binding.nsfwChip.setOnCheckedChangeListener { _, isChecked -> viewModel.setNsfwFlag(
            isChecked
        ) }

        binding.spoilerChip.setOnCheckedChangeListener { _, isChecked ->  viewModel.setSpoilerFlag(
            isChecked
        ) }
    }


    /**
     * Reconfigures the TabLayout based on this Subreddit allowed Post types
     * - retains the selected Tab, if it's still available
     */
    private fun reattachTabLayoutMediator(subreddit: Subreddit) {
        val oldTabLayout = binding.submissionTabLayout
        val tabCollection = getTabCollectionTriples(subreddit)
        val oldSelectedTabText = oldTabLayout.getTabAt(oldTabLayout.selectedTabPosition)?.text

        tabLayoutMediator.detach()
        binding.submissionPager.adapter = TabCollectionAdapterDynamic(this, tabCollection)

        tabLayoutMediator = TabLayoutMediator(oldTabLayout, binding.submissionPager) { tab, position ->
            tab.text = tabCollection[position].first
            tab.icon = tabCollection[position].second
        }

        tabLayoutMediator.attach()

        // try to retain the selected Tab, if it's still available
        val newTabLayout = binding.submissionTabLayout
        for (i in 0 until newTabLayout.tabCount) {
            val tab = newTabLayout.getTabAt(i)

            if (tab?.text == oldSelectedTabText) {
                newTabLayout.selectTab(tab)
                break
            }
        }
    }

    private fun getTabCollectionTriples(subreddit: Subreddit): List<Triple<String, Drawable, Fragment>> {
        var listOfTriples = mutableListOf<Triple<String, Drawable, Fragment>>()

        listOfTriples.add(
            Triple(
                "Link",
                requireContext().getDrawable(R.drawable.ic_baseline_link_24)!!,
                LinkSubmissionFragment(viewModel)
            )
        )

        if (subreddit.allowImages) {
            listOfTriples.add(
                Triple(
                    "Image",
                    requireContext().getDrawable(R.drawable.ic_baseline_image_24)!!,
                    ImageSubmissionFragment(viewModel)
                )
            )

        }

        if (subreddit.allowVideos) {
            listOfTriples.add(
                Triple(
                    "Video",
                    requireContext().getDrawable(R.drawable.ic_baseline_videocam_24)!!,
                    VideoSubmissionFragment(viewModel)
                ),
            )
        }

        listOfTriples.add(
            Triple(
                "Text",
                requireContext().getDrawable(R.drawable.ic_baseline_text_snippet_24)!!,
                TextSubmissionFragment(viewModel)
            ),
        )

        if (subreddit.allowPolls) {
            listOfTriples.add(
                Triple(
                    "Poll",
                    requireContext().getDrawable(R.drawable.ic_baseline_poll_24)!!,
                    PollSubmissionFragment(viewModel)
                ),
            )
        }

        return listOfTriples.toList()
    }

    override fun clearUserInputViews() {
        binding.titleEditTextView.text.clear()
        viewModel.submissionTitle.postValue(null)
    }

    private fun resetFlairToDefaultState() {
        try {
            binding.addFlairChip.text = "+ Add Flair"
            buildOneColorStateList(Color.LTGRAY)?.let {
                binding.addFlairChip.chipBackgroundColor = it
            }
            binding.addFlairChip.setTextColor(Color.BLACK)
        }
        catch (ex: java.lang.Exception) { /* flair.backGroundColor was either null or not a recognizable color */}

        viewModel.subredditFlair.postValue(null)
    }

    private fun getSelectedTabPositionSubmissionType(): SubmissionKind {
        val layout = binding.submissionTabLayout
        val tab = layout.getTabAt(layout.selectedTabPosition)

        return when(tab?.text) {
            "Link" -> {
                SubmissionKind.Link
            }
            "Image" -> {
                SubmissionKind.Image
            }
            "Video" -> {
                SubmissionKind.Video
            }
            "Text" -> {
                SubmissionKind.Self
            }
            "Poll" -> {
                SubmissionKind.Poll
            }
            else -> throw Exception("Invalid SubmissionKind!")
        }
    }

    private fun showPostConfirmationDialog(kind: String) {
        val checkedItem = if(viewModel.sendReplies.value!!) 0 else -1

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.when_would_you_like_to_post))
            .setSingleChoiceItems(arrayOf(getString(R.string.enable_inbox_replies)), checkedItem) { dialog, which ->
                enableInboxReplies = (dialog as AlertDialog).listView.getChildAt(which) as AppCompatCheckedTextView
                viewModel.toggleSendReplies()
            }
            .setNeutralButton("Now") {_, _ ->
                launchIO {
                    val responsePair = when (kind) {
                        "Link" -> {
                            viewModel.postSubmission(SubmissionKind.Link)
                        }
                        "Image" -> {
                            viewModel.postSubmission(SubmissionKind.Image)
                        }
                        "Video" -> {
                            viewModel.postSubmission(SubmissionKind.Video)
                        }
                        "Text" -> {
                            viewModel.postSubmission(SubmissionKind.Self)
                        }
                        "Poll" -> {
                            viewModel.postSubmission(SubmissionKind.Poll)
                        }
                        else -> Pair(null, null)
                    }

                    withUIContext {
                        // Post was successful
                        responsePair.first?.let { _ ->
                            findNavController().navigateUp()
                        }

                        // Post failed
                        responsePair.second?.let { msg ->
                            showSnackBar(binding.root, msg)
                        }
                    }

                }

            }
            .setNegativeButton("") {_, _ -> }
            .setPositiveButton("Later") { _, _ ->
                pickDateAndTime { cal ->
                    launchIO {
                        val alarmRequestCode = requestCodeHelper.nextInt()

                        withUIContext {
                            val alarmIntent = Intent(context, PublishScheduledSubmissionReceiver::class.java).let { intent ->
                                intent.putExtra("alarmRequestCode", alarmRequestCode)
                                PendingIntent.getBroadcast(context, alarmRequestCode, intent, 0)
                            }

                            val futureElapsedDate = getLocalDateFromUtcMillis(getElapsedTimeUntilFutureTime(cal.timeInMillis))

                            if (futureElapsedDate != null) {
                                alarmMgr.setExactAndAllowWhileIdle(
                                    AlarmManager.ELAPSED_REALTIME_WAKEUP,
                                    futureElapsedDate.time,
                                    alarmIntent
                                )
                                Timber.d("alarm set for new Submission")
                            } else {
                                Timber.e("failed to set alarm!")
                            }
                        }

                        when (kind) {
                            "Link" -> {
                                viewModel.saveSubmission(SubmissionKind.Link, cal.timeInMillis, alarmRequestCode)
                            }
                            "Image" -> {
                                viewModel.saveSubmission(SubmissionKind.Image, cal.timeInMillis, alarmRequestCode)
                            }
                            "Video" -> {
                                viewModel.saveSubmission(SubmissionKind.Video, cal.timeInMillis, alarmRequestCode)
                            }
                            "Text" -> {
                                viewModel.saveSubmission(SubmissionKind.Self, cal.timeInMillis, alarmRequestCode)
                            }
                            "Poll" -> {
                                viewModel.saveSubmission(SubmissionKind.Poll, cal.timeInMillis, alarmRequestCode)
                            }
                        }

                        launchUI { findNavController().navigateUp() }
                    }
                }
            }
            .show()
    }

    inner class TabCollectionAdapterDefault(fragment: Fragment) : FragmentStateAdapter(fragment) {
        override fun getItemCount(): Int = 5

        override fun createFragment(position: Int): Fragment {
            // Return a NEW fragment instance
            return when(position) {
                0 -> LinkSubmissionFragment(viewModel)
                1 -> ImageSubmissionFragment(viewModel)
                2 -> VideoSubmissionFragment(viewModel)
                3 -> TextSubmissionFragment(viewModel)
                4 -> PollSubmissionFragment(viewModel)
                else -> throw Exception("Unknown Tab Position!")
            }
        }
    }

    inner class TabCollectionAdapterDynamic(
        fragment: Fragment,
        val tabCollection: List<Triple<String, Drawable, Fragment>>
    ) : FragmentStateAdapter(fragment) {

        override fun getItemCount(): Int = tabCollection.size

        override fun createFragment(position: Int): Fragment {
            // Return a NEW fragment instance
            return tabCollection[position].third
        }
    }
}