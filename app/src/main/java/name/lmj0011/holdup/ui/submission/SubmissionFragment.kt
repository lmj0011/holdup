package name.lmj0011.holdup.ui.submission

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatCheckedTextView
import androidx.core.view.MenuProvider
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.navigation.fragment.findNavController
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.tabs.TabLayoutMediator
import kotlinx.coroutines.flow.collectLatest
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.analytics.ktx.logEvent
import com.google.firebase.ktx.Firebase
import name.lmj0011.holdup.App
import name.lmj0011.holdup.BaseFragment
import name.lmj0011.holdup.Keys
import name.lmj0011.holdup.MainActivity
import name.lmj0011.holdup.R
import name.lmj0011.holdup.database.AppDatabase
import name.lmj0011.holdup.databinding.FragmentSubmissionBinding
import name.lmj0011.holdup.helpers.DataStoreHelper
import name.lmj0011.holdup.helpers.DateTimeHelper.getElapsedTimeUntilFutureTime
import name.lmj0011.holdup.helpers.NotificationHelper
import name.lmj0011.holdup.helpers.UniqueRuntimeNumberHelper
import name.lmj0011.holdup.helpers.adapters.SubredditFlairListAdapter
import name.lmj0011.holdup.helpers.adapters.SubredditSearchListAdapter
import name.lmj0011.holdup.helpers.enums.SubmissionKind
import name.lmj0011.holdup.helpers.interfaces.BaseFragmentInterface
import name.lmj0011.holdup.helpers.interfaces.SubmissionFragmentChild
import name.lmj0011.holdup.helpers.models.Subreddit
import name.lmj0011.holdup.helpers.receivers.PublishScheduledSubmissionReceiver
import name.lmj0011.holdup.helpers.util.buildOneColorStateList
import name.lmj0011.holdup.helpers.util.extractOpenGraphImageFromUrl
import name.lmj0011.holdup.helpers.util.extractTitleFromUrl
import name.lmj0011.holdup.helpers.util.isIgnoringBatteryOptimizations
import name.lmj0011.holdup.helpers.util.launchIO
import name.lmj0011.holdup.helpers.util.launchUI
import name.lmj0011.holdup.helpers.util.showSnackBar
import name.lmj0011.holdup.helpers.util.withUIContext
import name.lmj0011.holdup.ui.submission.bottomsheet.BottomSheetAccountsFragment
import name.lmj0011.holdup.ui.submission.bottomsheet.BottomSheetSubredditFlairFragment
import name.lmj0011.holdup.ui.submission.bottomsheet.BottomSheetSubredditSearchFragment
import org.jsoup.HttpStatusException
import org.kodein.di.instance
import timber.log.Timber

/**
 * Serves as the ParentFragment for other *SubmissionFragment
 */
class SubmissionFragment: BaseFragment(R.layout.fragment_submission), BaseFragmentInterface {
    private lateinit var binding: FragmentSubmissionBinding
    private lateinit var tabLayoutMediator: TabLayoutMediator
    private lateinit var viewModel: SubmissionViewModel
    override lateinit var dataStoreHelper: DataStoreHelper
    private lateinit var alarmMgr: AlarmManager
    private lateinit var requestCodeHelper: UniqueRuntimeNumberHelper
    private val firebaseAnalytics: FirebaseAnalytics = Firebase.analytics

    lateinit var bottomSheetAccountsFragment: BottomSheetAccountsFragment
    lateinit var bottomSheetSubredditSearchFragment: BottomSheetSubredditSearchFragment
    lateinit var bottomSheetSubredditFlairFragment: BottomSheetSubredditFlairFragment
    lateinit var enableInboxReplies: AppCompatCheckedTextView

    private var optionsMenu: Menu? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = SubmissionViewModel.getNewInstance(
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
         *
         * Creates 10 scheduled Self Submissions, the first posting X time from the current time
         * and each sequential Submission X time after the previous one.
         */
//        val dao = AppDatabase.getInstance(requireActivity().application).sharedDao
//        val tHelper = TestHelper(requireContext())
//
//        launchIO {
//            dao.deleteAllSubmissionId() // clear submissions table
//
//            val currentMillis = System.currentTimeMillis()
//            val offsetInMillis = 300000L // 5 minutes
//
//            val submissions = tHelper.generateSubmissions(
//                listOf(
//                    Pair(SubmissionKind.Self, (currentMillis + (offsetInMillis * 1))),
//                    Pair(SubmissionKind.Self, (currentMillis + (offsetInMillis * 2))),
//                    Pair(SubmissionKind.Self, (currentMillis + (offsetInMillis * 3))),
//                    Pair(SubmissionKind.Self, (currentMillis + (offsetInMillis * 4))),
//                    Pair(SubmissionKind.Self, (currentMillis + (offsetInMillis * 5))),
//                    Pair(SubmissionKind.Self, (currentMillis + (offsetInMillis * 6))),
//                    Pair(SubmissionKind.Self, (currentMillis + (offsetInMillis * 7))),
//                    Pair(SubmissionKind.Self, (currentMillis + (offsetInMillis * 8))),
//                    Pair(SubmissionKind.Self, (currentMillis + (offsetInMillis * 9))),
//                    Pair(SubmissionKind.Self, (currentMillis + (offsetInMillis * 10))),
//                )
//            )
//
//            dao.insertAllSubmissions(submissions)
//
//            withUIContext {
//
//                submissions.forEach { sub ->
//                    val alarmIntent = Intent(context, PublishScheduledSubmissionReceiver::class.java).let { intent ->
//                        intent.action = sub.alarmRequestCode.toString()
//                        intent.putExtra("alarmRequestCode", sub.alarmRequestCode)
//                        PendingIntent.getBroadcast(
//                            context,
//                            sub.alarmRequestCode,
//                            intent,
//                            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
//                    }
//
//
//                val futureElapsedTime = getElapsedTimeUntilFutureTime(sub.postAtMillis)
//
//                    alarmMgr.setExactAndAllowWhileIdle(
//                        AlarmManager.ELAPSED_REALTIME_WAKEUP,
//                        futureElapsedTime,
//                        alarmIntent
//                    )
//
//                  Timber.d("alarm set for ${sub.kind?.name} Submission \"${sub.title}\" @ ${DateTimeHelper.getPostAtDateForListLayout(sub)}")
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

        /**
         * The new way of creating and handling menus
         * ref: https://developer.android.com/jetpack/androidx/releases/activity#1.4.0-alpha01
         */
        requireActivity().addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                optionsMenu = menu
                optionsMenu?.clear()
                menuInflater.inflate(R.menu.submission, menu)
            }

            override fun onPrepareMenu(menu: Menu) {
                super.onPrepareMenu(menu)
                optionsMenu = menu
                viewModel.readyToPost().value!!.let {
                    menu.getItem(0).isEnabled = it
                }
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                return when (menuItem.itemId) {
                    R.id.action_post_submission -> {
                        val layout = binding.submissionTabLayout

                        layout.getTabAt(layout.selectedTabPosition)?.let {
                            showPostConfirmationDialog(it.text.toString())
                        }
                        true
                    }
                    else -> false
                }
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }

    override fun setupObservers() {
        viewModel.sendReplies.observe(viewLifecycleOwner, {
            if(::enableInboxReplies.isInitialized) enableInboxReplies.isChecked = it
        })

        viewModel.subredditPostRequirements.observe(viewLifecycleOwner, {
            viewModel.validateSubmission(getSelectedTabPositionSubmissionType())
        })

        viewModel.subreddit.observe(viewLifecycleOwner, {
            binding.chooseSubredditTextView.text = it.displayNamePrefixed
            Glide
                .with(this)
                .load(it.iconImgUrl)
                .apply(RequestOptions().override(100))
                .circleCrop()
                .error(R.mipmap.ic_default_subreddit_icon_round)
                .into(binding.chooseSubredditImageView)

            reConfigureTabLayoutMediator(it)

            viewModel.setSubredditPostRequirements(it)

            val listFlow = viewModel.getSubredditFlairListFlow()

            bottomSheetSubredditFlairFragment = BottomSheetSubredditFlairFragment(
                SubredditFlairListAdapter.FlairItemClickListener { flair ->
                    binding.addFlairChip.text = flair.text

                    try {
                        if (flair.backGroundColor.isNotBlank() && flair.textColor.isNotBlank()) {
                            buildOneColorStateList(Color.parseColor(flair.backGroundColor))?.let {
                                binding.addFlairChip.chipBackgroundColor = it
                            }

                            when(flair.textColor) {
                                "light" ->  binding.addFlairChip.setTextColor(Color.WHITE)
                                "dark" ->  binding.addFlairChip.setTextColor(Color.DKGRAY)
                            }
                        }
                    } catch (ex: java.lang.Exception) {/* flair.backGroundColor was either null or not a recognizable color */}

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


        viewModel.getAccount().observe(viewLifecycleOwner) {
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
        }


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

        viewModel.getSubmissionLinkText().observe(viewLifecycleOwner, { url ->
            launchIO {
                try {
                    // get og:title
                    val title = extractTitleFromUrl(url.toString())

                    if (title.isNotBlank()) {
                        withUIContext {
                            binding.titleEditTextView.setText(title)
                            viewModel.submissionTitle.postValue(title)
                        }
                    }

                    // get og:image
                    val imageUrl = extractOpenGraphImageFromUrl(url.toString())

                    if (imageUrl.isNotBlank()) {
                        withUIContext {
                            viewModel.submissionLinkImageUrl.postValue(imageUrl)
                        }
                    }
                } catch (ex: Exception) {
                    // If there's an Exception, we'll ignore it.
                } finally {
                    withUIContext {
                        viewModel.validateSubmission(getSelectedTabPositionSubmissionType())
                    }
                }
            }
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
        binding.lifecycleOwner = viewLifecycleOwner
        binding.submissionPager.adapter = TabCollectionAdapter(this)
        binding.submissionPager.isUserInputEnabled = false // prevent swiping navigation ref: https://stackoverflow.com/a/55193815/2445763
        binding.submissionPager.offscreenPageLimit = 4
        tabLayoutMediator = TabLayoutMediator(binding.submissionTabLayout, binding.submissionPager, true, false) { tab, position ->
            when(position) {
                Keys.LINK_TAB_POSITION -> {
                    tab.text = "Link"
                    tab.icon = requireContext().getDrawable(R.drawable.ic_baseline_link_24)
                }
                Keys.IMAGE_TAB_POSITION -> {
                    tab.text = "Image"
                    tab.icon = requireContext().getDrawable(R.drawable.ic_baseline_image_24)
                }
                Keys.VIDEO_TAB_POSITION -> {
                    tab.text = "Video"
                    tab.icon = requireContext().getDrawable(R.drawable.ic_baseline_videocam_24)
                }
                Keys.SELF_TAB_POSITION -> {
                    tab.text = "Text"
                    tab.icon = requireContext().getDrawable(R.drawable.ic_baseline_text_snippet_24)
                }
                Keys.POLL_TAB_POSITION -> {
                    tab.text = "Poll"
                    tab.icon = requireContext().getDrawable(R.drawable.ic_baseline_poll_24)
                }
            }
        }

        binding.submissionPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                val actionBarTitle = when(position) {
                    Keys.LINK_TAB_POSITION -> "Link Submission"
                    Keys.IMAGE_TAB_POSITION -> "Image Submission"
                    Keys.VIDEO_TAB_POSITION -> "Video Submission"
                    Keys.SELF_TAB_POSITION -> "Text Submission"
                    Keys.POLL_TAB_POSITION -> "Poll Submission"
                    else -> ""
                }
                (requireActivity() as AppCompatActivity).supportActionBar?.title = actionBarTitle

                super.onPageSelected(position)
            }
        })

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
    private fun reConfigureTabLayoutMediator(subreddit: Subreddit) {
        val tabLayout = binding.submissionTabLayout
        val allowedSubmissionKinds = getAllowedSubmissionKinds(subreddit)
        val selectedTab = tabLayout.getTabAt(tabLayout.selectedTabPosition)

        // hide all tabs
        for (i in 0..tabLayout.tabCount) {
            tabLayout.getTabAt(i)?.view?.isVisible = false
        }

        // shows tabs that should be shown for this subreddit
        allowedSubmissionKinds.forEach { kind ->
            when (kind) {
                SubmissionKind.Link -> {
                    tabLayout.getTabAt(Keys.LINK_TAB_POSITION)?.view?.isVisible = true
            }
                SubmissionKind.Image -> {
                    tabLayout.getTabAt(Keys.IMAGE_TAB_POSITION)?.view?.isVisible = true
            }
                SubmissionKind.Video, SubmissionKind.VideoGif -> {
                    tabLayout.getTabAt(Keys.VIDEO_TAB_POSITION)?.view?.isVisible = true
            }
                SubmissionKind.Self -> {
                    tabLayout.getTabAt(Keys.SELF_TAB_POSITION)?.view?.isVisible = true
            }
                SubmissionKind.Poll -> {
                    tabLayout.getTabAt(Keys.POLL_TAB_POSITION)?.view?.isVisible = true
            }
            }
        }

        // set the selected Tab to the previously selected or the first available one
        val kind = allowedSubmissionKinds.firstOrNull { kind ->
            kind.kind == selectedTab?.text.toString().lowercase()
        }

        if (kind != null) {
            when (kind) {
                SubmissionKind.Link -> {
                    tabLayout.selectTab(tabLayout.getTabAt(Keys.LINK_TAB_POSITION), true)
                }
                SubmissionKind.Image -> {
                    tabLayout.selectTab(tabLayout.getTabAt(Keys.IMAGE_TAB_POSITION), true)
                }
                SubmissionKind.Video, SubmissionKind.VideoGif -> {
                    tabLayout.selectTab(tabLayout.getTabAt(Keys.VIDEO_TAB_POSITION), true)
                }
                SubmissionKind.Self -> {
                    tabLayout.selectTab(tabLayout.getTabAt(Keys.SELF_TAB_POSITION), true)
                }
                SubmissionKind.Poll -> {
                    tabLayout.selectTab(tabLayout.getTabAt(Keys.POLL_TAB_POSITION), true)
                }
            }
        } else {
            tabLayout.selectTab(tabLayout.getTabAt(0), true)
        }
    }

    /**
     * Returns a list of [SubmissionKind] that reflects the allowed posts for the given [Subreddit]
     *
     */
    private fun getAllowedSubmissionKinds(subreddit: Subreddit): List<SubmissionKind> {
        val listOfKinds = mutableListOf<SubmissionKind>()

        listOfKinds.add(SubmissionKind.Link)

        /**
         * NSFW subs only allow media hosted from 3rd party sources apparently
         *
         * ref: https://www.reddit.com/r/ModSupport/comments/i3dcqr/cant_post_images_to_my_nsfw_subreddit_from_reddit/
         * ref: https://www.reddit.com/r/help/comments/b0sd0x/posting_nsfw_images/
         */
        if (subreddit.allowImages && !subreddit.over18) {
            listOfKinds.add(SubmissionKind.Image)

        }

        if (subreddit.allowVideos && !subreddit.over18) {
            listOfKinds.add(SubmissionKind.Video)
            listOfKinds.add(SubmissionKind.VideoGif)
        }

        listOfKinds.add(SubmissionKind.Self)

        if (subreddit.allowPolls) {
            listOfKinds.add(SubmissionKind.Poll)
        }

        return listOfKinds
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
                    try {
                        when (SubmissionKind.from(kind)) {
                            SubmissionKind.Image, SubmissionKind.Video, SubmissionKind.VideoGif -> {
                                val alarmRequestCode = requestCodeHelper.nextInt()
                                viewModel.saveSubmission(SubmissionKind.from(kind), System.currentTimeMillis(), alarmRequestCode)
                                enqueueUploadSubmissionMediaWorkerThenPublish(alarmRequestCode)
                                withUIContext { findNavController().navigateUp() }
                            } else -> {
                                val responsePair = viewModel.postSubmission(SubmissionKind.from(kind))

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
                    } catch(ex: HttpStatusException) {
                        withUIContext {
                            showSnackBar(binding.root, requireContext().getString(R.string.reddit_api_http_error_msg, ex.statusCode, ex.message))
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
                                intent.action = alarmRequestCode.toString()
                                intent.putExtra("alarmRequestCode", alarmRequestCode)
                                PendingIntent.getBroadcast(
                                    context,
                                    alarmRequestCode,
                                    intent,
                                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
                            }

                            val futureElapsedTime = getElapsedTimeUntilFutureTime(cal.timeInMillis)

                            alarmMgr.setExactAndAllowWhileIdle(
                                AlarmManager.ELAPSED_REALTIME_WAKEUP,
                                futureElapsedTime,
                                alarmIntent
                            )

                            Timber.d("alarm set for new $kind Submission")
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

                        enqueueUploadSubmissionMediaWorker()

                        // [START custom_event]
                        firebaseAnalytics.logEvent("hol_post_scheduled") {
                            param("sr", viewModel.subreddit.value?.displayName.toString())
                            param("post_type", kind)
                        }
                        // [END custom_event]

                        launchUI {
                            if(!isIgnoringBatteryOptimizations(requireContext())) {
                                NotificationHelper.showBatteryOptimizationInfoNotification()
                            }

                            findNavController().navigateUp()
                        }
                    }
                }
            }
            .show()
    }

    inner class TabCollectionAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {
        private val linkFrag = LinkSubmissionFragment.newInstance(null, SubmissionFragmentChild.CREATE_AND_EDIT_MODE)
        private val imageFrag = ImageSubmissionFragment.newInstance(null, SubmissionFragmentChild.CREATE_AND_EDIT_MODE)
        private val videoFrag = VideoSubmissionFragment.newInstance(null, SubmissionFragmentChild.CREATE_AND_EDIT_MODE, (requireActivity() as MainActivity).mediaPlayer)
        private val textFrag = TextSubmissionFragment.newInstance(null, SubmissionFragmentChild.CREATE_AND_EDIT_MODE)
        private val pollFrag = PollSubmissionFragment.newInstance(null, SubmissionFragmentChild.CREATE_AND_EDIT_MODE)

        override fun getItemCount(): Int = 5

        override fun createFragment(position: Int): Fragment {
            // Return a NEW fragment instance
            return when(position) {
                Keys.LINK_TAB_POSITION -> linkFrag
                Keys.IMAGE_TAB_POSITION -> imageFrag
                Keys.VIDEO_TAB_POSITION -> videoFrag
                Keys.SELF_TAB_POSITION -> textFrag
                Keys.POLL_TAB_POSITION -> pollFrag
                else -> throw Exception("Unknown Tab Position!")
            }
        }
    }
}