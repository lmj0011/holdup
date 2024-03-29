package name.lmj0011.holdup.ui.submission

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.*
import androidx.appcompat.widget.AppCompatCheckedTextView
import androidx.core.view.MenuProvider
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.analytics.ktx.logEvent
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import name.lmj0011.holdup.App
import name.lmj0011.holdup.BaseFragment
import name.lmj0011.holdup.Keys
import name.lmj0011.holdup.MainActivity
import name.lmj0011.holdup.R
import name.lmj0011.holdup.database.AppDatabase
import name.lmj0011.holdup.databinding.FragmentEditSubmissionBinding
import name.lmj0011.holdup.helpers.DataStoreHelper
import name.lmj0011.holdup.helpers.DateTimeHelper.getElapsedTimeUntilFutureTime
import name.lmj0011.holdup.helpers.FirebaseAnalyticsHelper
import name.lmj0011.holdup.helpers.NotificationHelper
import name.lmj0011.holdup.helpers.UniqueRuntimeNumberHelper
import name.lmj0011.holdup.helpers.adapters.SubredditFlairListAdapter
import name.lmj0011.holdup.helpers.adapters.SubredditSearchListAdapter
import name.lmj0011.holdup.helpers.enums.SubmissionKind
import name.lmj0011.holdup.helpers.interfaces.BaseFragmentInterface
import name.lmj0011.holdup.helpers.interfaces.SubmissionFragmentChildInterface
import name.lmj0011.holdup.helpers.models.SubredditFlair
import name.lmj0011.holdup.helpers.receivers.PublishScheduledSubmissionReceiver
import name.lmj0011.holdup.helpers.util.buildOneColorStateList
import name.lmj0011.holdup.helpers.util.extractOpenGraphImageFromUrl
import name.lmj0011.holdup.helpers.util.isIgnoringBatteryOptimizations
import name.lmj0011.holdup.helpers.util.launchIO
import name.lmj0011.holdup.helpers.util.launchUI
import name.lmj0011.holdup.helpers.util.showSnackBar
import name.lmj0011.holdup.helpers.util.withUIContext
import name.lmj0011.holdup.ui.submission.bottomsheet.BottomSheetAccountsFragment
import name.lmj0011.holdup.ui.submission.bottomsheet.BottomSheetSubmissionsScheduleOptionsFragment
import name.lmj0011.holdup.ui.submission.bottomsheet.BottomSheetSubredditFlairFragment
import name.lmj0011.holdup.ui.submission.bottomsheet.BottomSheetSubredditSearchFragment
import org.jsoup.HttpStatusException
import org.kodein.di.instance
import timber.log.Timber
import java.lang.Exception

/**
 * Serves as the ParentFragment for other *SubmissionFragment
 */
class EditSubmissionFragment: BaseFragment(R.layout.fragment_edit_submission), BaseFragmentInterface {
    private lateinit var binding: FragmentEditSubmissionBinding
    private lateinit var viewModel: SubmissionViewModel
    override lateinit var dataStoreHelper: DataStoreHelper
    private lateinit var alarmMgr: AlarmManager
    private lateinit var requestCodeHelper: UniqueRuntimeNumberHelper
    private val args: EditSubmissionFragmentArgs by navArgs()
    lateinit var firebaseAnalyticsHelper: FirebaseAnalyticsHelper

    lateinit var bottomSheetSubmissionsScheduleOptionsFragment: BottomSheetSubmissionsScheduleOptionsFragment
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
        firebaseAnalyticsHelper = (requireContext().applicationContext as App).kodein.instance()
        alarmMgr = requireContext().getSystemService(Context.ALARM_SERVICE) as AlarmManager

        setHasOptionsMenu(true)
    }

    override fun onResume() {
        super.onResume()
        viewModel.setAccount()

        launchIO {
            viewModel.populateFromSubmissionThenPost(args.submission, false)

            withUIContext {
                delay(1500)
                viewModel.validateSubmission(args.submission.kind!!)
            }
        }

        firebaseAnalyticsHelper.logScreenView(this)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupBinding(view)
        setupObservers()

        binding.titleEditTextView.setText(args.submission.title)
        args.submission.subredditFlair?.let{ updateFlair(it) }

        binding.flagsChipGroup.isVisible = args.submission.isNsfw || args.submission.isSpoiler
        binding.nsfwChip.isChecked = args.submission.isNsfw
        binding.spoilerChip.isChecked = args.submission.isSpoiler

        (requireActivity() as MainActivity).hideFab()

        /**
         * The new way of creating and handling menus
         * ref: https://developer.android.com/jetpack/androidx/releases/activity#1.4.0-alpha01
         */
        requireActivity().addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                optionsMenu = menu
                optionsMenu?.clear()
                menuInflater.inflate(R.menu.edit_submission, menu)
            }

            override fun onPrepareMenu(menu: Menu) {
                super.onPrepareMenu(menu)

                optionsMenu = menu
                viewModel.readyToPost().value!!.let {
                    menu.getItem(2).isEnabled = it
                }


                // Only show option to save Submission if it's been scheduled
                if(args.submission.postAtMillis == Keys.UNIX_EPOCH_MILLIS) {
                    menu.getItem(0).isEnabled = false
                    menu.getItem(0).isVisible = false
                }
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                return when (menuItem.itemId) {
                    R.id.action_delete_submission -> {
                        showDeleteConfirmationDialog()
                        true
                    }
                    R.id.action_save_submission -> {
                        saveSubmission()
                        true
                    }
                    R.id.action_post_submission -> {
                        showPostConfirmationDialog()
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
            viewModel.validateSubmission(args.submission.kind!!)
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

            viewModel.setSubredditPostRequirements(it)

            val listFlow = viewModel.getSubredditFlairListFlow()

            bottomSheetSubredditFlairFragment = BottomSheetSubredditFlairFragment(
                SubredditFlairListAdapter.FlairItemClickListener { flair ->
                    updateFlair(flair)

                    viewModel.subredditFlair.postValue(flair)
                    viewModel.validateSubmission(args.submission.kind!!)
                    bottomSheetSubredditFlairFragment.dismiss()
                },
                { v: View ->
                    viewModel.subredditFlair.postValue(null)
                    resetFlairToDefaultState()
                    viewModel.validateSubmission(args.submission.kind!!)
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
            viewModel.validateSubmission(args.submission.kind!!)
        })

        viewModel.getSubmissionLinkText().observe(viewLifecycleOwner, { url ->
            launchIO {
                try {
                    // update og:image
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
                        viewModel.validateSubmission(args.submission.kind!!)
                    }
                }
            }

        })

        viewModel.submissionSelfText.observe(viewLifecycleOwner, {
            viewModel.validateSubmission(args.submission.kind!!)
        })

        viewModel.submissionImageGallery.observe(viewLifecycleOwner, {
            viewModel.validateSubmission(args.submission.kind!!)
        })

        viewModel.submissionPollBodyText.observe(viewLifecycleOwner, {
            viewModel.validateSubmission(args.submission.kind!!)
        })

        viewModel.submissionPollOptions.observe(viewLifecycleOwner, {
            viewModel.validateSubmission(args.submission.kind!!)
        })

        viewModel.submissionPollDuration.observe(viewLifecycleOwner, {
            viewModel.validateSubmission(args.submission.kind!!)
        })

        viewModel.submissionVideo.observe(viewLifecycleOwner, {
            viewModel.validateSubmission(args.submission.kind!!)
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

        launchUI {
            dataStoreHelper.getLastSelectedDateTimeFromCalendar().collectLatest { millis ->
                previousCalMillis = millis
            }
        }
    }

    private fun updateFlair(flair: SubredditFlair) {
        binding.addFlairChip.text = flair.text
        try {
            buildOneColorStateList(Color.parseColor(flair.backGroundColor))?.let {
                binding.addFlairChip.chipBackgroundColor = it
            }
        } catch (ex: Exception) { /* flair.backGroundColor was either null or not a recognizable color */
        }

        when (flair.textColor) {
            "light" -> binding.addFlairChip.setTextColor(Color.WHITE)
            else -> binding.addFlairChip.setTextColor(Color.DKGRAY)
        }
    }

    override fun setupRecyclerView() {}

    private fun resetFlagsState() {
        binding.nsfwChip.isChecked = false
        binding.spoilerChip.isChecked = false
    }

    override fun setupBinding(view: View) {
        binding = FragmentEditSubmissionBinding.bind(view)
        binding.lifecycleOwner = viewLifecycleOwner
        binding.submissionPager.adapter = TabCollectionAdapterDefault(this)
        binding.submissionPager.isUserInputEnabled = false // prevent swiping navigation ref: https://stackoverflow.com/a/55193815/2445763

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
        catch (ex: Exception) { /* flair.backGroundColor was either null or not a recognizable color */}

        viewModel.subredditFlair.postValue(null)
    }

    private fun saveSubmission() {
        launchIO {
            viewModel.updateSubmission(args.submission)
            withUIContext { showSnackBar(binding.root, "Submission saved.") }
        }
    }

    private fun showDeleteConfirmationDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Delete this submission?")
            .setNeutralButton("Yes") {_, _ ->
                launchIO {
                    viewModel.deleteSubmission(args.submission)
                    withUIContext { findNavController().navigateUp() }
                }
            }
            .setNegativeButton("") {_, _ -> }
            .setPositiveButton("No") { dialog,_ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun showPostConfirmationDialog() {
        viewModel.sendReplies.postValue(true) // setting this to always be true since it's not being added as an option to the bottomSheet

        bottomSheetSubmissionsScheduleOptionsFragment = BottomSheetSubmissionsScheduleOptionsFragment(
            { millis ->
                launchIO {
                    val sub = args.submission

                    withUIContext {
                        val alarmIntent = Intent(context, PublishScheduledSubmissionReceiver::class.java).let { intent ->
                            intent.action = sub.alarmRequestCode.toString()
                            intent.putExtra("alarmRequestCode", sub.alarmRequestCode)
                            PendingIntent.getBroadcast(
                                context,
                                sub.alarmRequestCode,
                                intent,
                                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
                        }

                        val futureElapsedTime = getElapsedTimeUntilFutureTime(millis)

                        Timber.d("futureElapsedTime: $futureElapsedTime")

                        alarmMgr.setExactAndAllowWhileIdle(
                            AlarmManager.ELAPSED_REALTIME_WAKEUP,
                            futureElapsedTime,
                            alarmIntent
                        )
                        Timber.d("alarm set for Submission")
                    }

                    sub.postAtMillis = millis
                    viewModel.updateSubmission(sub)

                    enqueueUploadSubmissionMediaWorker()
                    launchUI {
                        if(!isIgnoringBatteryOptimizations(requireContext())) {
                            NotificationHelper.showBatteryOptimizationInfoNotification()
                        }

                        findNavController().navigateUp()
                    }
                }
            },
            {
                pickDateAndTime(previousCalMillis) { cal ->
                    launchIO {
                        val sub = args.submission

                        withUIContext {
                            val alarmIntent = Intent(context, PublishScheduledSubmissionReceiver::class.java).let { intent ->
                                intent.action = sub.alarmRequestCode.toString()
                                intent.putExtra("alarmRequestCode", sub.alarmRequestCode)
                                PendingIntent.getBroadcast(
                                    context,
                                    sub.alarmRequestCode,
                                    intent,
                                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
                            }

                            val futureElapsedTime = getElapsedTimeUntilFutureTime(cal.timeInMillis)

                            Timber.d("futureElapsedTime: $futureElapsedTime")

                            alarmMgr.setExactAndAllowWhileIdle(
                                AlarmManager.ELAPSED_REALTIME_WAKEUP,
                                futureElapsedTime,
                                alarmIntent
                            )
                            Timber.d("alarm set for Submission")
                        }

                        sub.postAtMillis = cal.timeInMillis
                        viewModel.updateSubmission(sub)

                        dataStoreHelper.setLastSelectedDateTimeFromCalendar(sub.postAtMillis)
                        enqueueUploadSubmissionMediaWorker()
                        launchUI {
                            if(!isIgnoringBatteryOptimizations(requireContext())) {
                                NotificationHelper.showBatteryOptimizationInfoNotification()
                            }

                            findNavController().navigateUp()
                        }
                    }
                }
            },
            {
                launchIO {
                    try {

                        when (val kind = args.submission.kind!!) {
                            SubmissionKind.Image, SubmissionKind.Video, SubmissionKind.VideoGif -> {
                                args.submission.postAtMillis = System.currentTimeMillis()
                                AppDatabase.getInstance(requireActivity().application).sharedDao.update(args.submission)
                                enqueueUploadSubmissionMediaWorkerThenPublish(args.submission.alarmRequestCode)
                                withUIContext { findNavController().navigateUp() }
                            } else -> {
                            val responsePair = viewModel.postSubmission(kind)

                            // Post was successful
                            responsePair.first?.let { _ ->
                                viewModel.deleteSubmission(args.submission)
                                withUIContext { findNavController().navigateUp() }
                            }

                            responsePair.second?.let { msg ->
                                withUIContext { showSnackBar(binding.root, msg) }
                            }
                        }
                        }
                    } catch(ex: HttpStatusException) {
                        bottomSheetSubmissionsScheduleOptionsFragment.dismiss()
                        showSnackBar(binding.root, requireContext().getString(R.string.reddit_api_http_error_msg, ex.statusCode, ex.message))
                    }
                }
            },
            {

            }
        )

        bottomSheetSubmissionsScheduleOptionsFragment.show(childFragmentManager, "bottomSheetSubmissionsScheduleOptionsFragment")
    }

    inner class TabCollectionAdapterDefault(fragment: Fragment) : FragmentStateAdapter(fragment) {
        override fun getItemCount(): Int = 5

        override fun createFragment(position: Int): Fragment {
            // Return a NEW fragment instance
            return when (args.submission.kind) {
                SubmissionKind.Link -> LinkSubmissionFragment.newInstance(args.submission, SubmissionFragmentChildInterface.CREATE_AND_EDIT_MODE)
                SubmissionKind.Image -> ImageSubmissionFragment.newInstance(args.submission, SubmissionFragmentChildInterface.CREATE_AND_EDIT_MODE)
                SubmissionKind.Video, SubmissionKind.VideoGif ->  VideoSubmissionFragment.newInstance(args.submission, SubmissionFragmentChildInterface.CREATE_AND_EDIT_MODE, (requireActivity() as MainActivity).mediaPlayer)
                SubmissionKind.Self -> TextSubmissionFragment.newInstance(args.submission, SubmissionFragmentChildInterface.CREATE_AND_EDIT_MODE)
                SubmissionKind.Poll -> PollSubmissionFragment.newInstance(args.submission, SubmissionFragmentChildInterface.CREATE_AND_EDIT_MODE)
                else ->  TextSubmissionFragment.newInstance(args.submission, SubmissionFragmentChildInterface.CREATE_AND_EDIT_MODE)
            }
        }
    }
}