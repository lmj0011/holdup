package name.lmj0011.redditdraftking.ui.submission

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.AppCompatCheckedTextView
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import name.lmj0011.redditdraftking.App
import name.lmj0011.redditdraftking.BaseFragment
import name.lmj0011.redditdraftking.R
import name.lmj0011.redditdraftking.database.AppDatabase
import name.lmj0011.redditdraftking.database.models.Submission
import name.lmj0011.redditdraftking.databinding.FragmentEditSubmissionBinding
import name.lmj0011.redditdraftking.helpers.DataStoreHelper
import name.lmj0011.redditdraftking.helpers.DateTimeHelper.getElapsedTimeUntilFutureTime
import name.lmj0011.redditdraftking.helpers.DateTimeHelper.getLocalDateFromUtcMillis
import name.lmj0011.redditdraftking.helpers.UniqueRuntimeNumberHelper
import name.lmj0011.redditdraftking.helpers.adapters.SubredditFlairListAdapter
import name.lmj0011.redditdraftking.helpers.adapters.SubredditSearchListAdapter
import name.lmj0011.redditdraftking.helpers.enums.SubmissionKind
import name.lmj0011.redditdraftking.helpers.interfaces.BaseFragmentInterface
import name.lmj0011.redditdraftking.helpers.models.SubredditFlair
import name.lmj0011.redditdraftking.helpers.receivers.PublishScheduledSubmissionReceiver
import name.lmj0011.redditdraftking.helpers.util.buildOneColorStateList
import name.lmj0011.redditdraftking.helpers.util.launchIO
import name.lmj0011.redditdraftking.helpers.util.launchUI
import name.lmj0011.redditdraftking.helpers.util.showSnackBar
import name.lmj0011.redditdraftking.helpers.util.withUIContext
import name.lmj0011.redditdraftking.ui.submission.bottomsheet.BottomSheetAccountsFragment
import name.lmj0011.redditdraftking.ui.submission.bottomsheet.BottomSheetSubredditFlairFragment
import name.lmj0011.redditdraftking.ui.submission.bottomsheet.BottomSheetSubredditSearchFragment
import org.kodein.di.instance
import timber.log.Timber
import java.lang.Exception

/**
 * Serves as the ParentFragment for other *SubmissionFragment
 */
class EditSubmissionFragment: BaseFragment(R.layout.fragment_edit_submission), BaseFragmentInterface {
    private lateinit var binding: FragmentEditSubmissionBinding
    private lateinit var viewModel: SubmissionViewModel
    private lateinit var dataStoreHelper: DataStoreHelper
    private lateinit var alarmMgr: AlarmManager
    private lateinit var requestCodeHelper: UniqueRuntimeNumberHelper
    private val args: EditSubmissionFragmentArgs by navArgs()

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
        launchIO {
            viewModel.populateFromSubmissionThenPost(args.submission, false)

            withUIContext {
                delay(1500)
                viewModel.validateSubmission(args.submission.kind!!)
            }
        }
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
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        optionsMenu = menu
        optionsMenu?.clear()
        inflater.inflate(R.menu.edit_submission, menu)
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        super.onPrepareOptionsMenu(menu)
        optionsMenu = menu
        viewModel.readyToPost().value!!.let {
            menu.getItem(1).isEnabled = it
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.action_delete_submission -> {
                showDeleteConfirmationDialog()
                true
            }
            R.id.action_post_submission -> {
                showPostConfirmationDialog()
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

        viewModel.getSubmissionLinkText().observe(viewLifecycleOwner, {
            viewModel.validateSubmission(args.submission.kind!!)
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
        binding.lifecycleOwner = this
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
        catch (ex: java.lang.Exception) { /* flair.backGroundColor was either null or not a recognizable color */}

        viewModel.subredditFlair.postValue(null)
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
        val checkedItem = if(viewModel.sendReplies.value!!) 0 else -1

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.when_would_you_like_to_post))
            .setSingleChoiceItems(arrayOf(getString(R.string.enable_inbox_replies)), checkedItem) { dialog, which ->
                enableInboxReplies = (dialog as AlertDialog).listView.getChildAt(which) as AppCompatCheckedTextView
                viewModel.toggleSendReplies()
            }
            .setNeutralButton("Now") {_, _ ->
                launchIO {
                    val responsePair = viewModel.postSubmission(args.submission.kind!!)

                    responsePair.first?.let { _ ->
                        viewModel.deleteSubmission(args.submission)
                    }

                    withUIContext {
                        responsePair.first?.let { _ ->
                            findNavController().navigateUp()
                        }

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
                        val sub = args.submission

                        withUIContext {
                            val alarmIntent = Intent(context, PublishScheduledSubmissionReceiver::class.java).let { intent ->
                                intent.putExtra("alarmRequestCode", sub.alarmRequestCode)
                                PendingIntent.getBroadcast(context, sub.alarmRequestCode, intent, 0)
                            }

                            val futureElapsedDate = getLocalDateFromUtcMillis(
                                getElapsedTimeUntilFutureTime(
                                    cal.timeInMillis
                                )
                            )

                            if (futureElapsedDate != null) {
                                alarmMgr.setExactAndAllowWhileIdle(
                                    AlarmManager.ELAPSED_REALTIME_WAKEUP,
                                    futureElapsedDate.time,
                                    alarmIntent
                                )
                                Timber.d("alarm set for Submission")
                            } else {
                                Timber.e("failed to set alarm!")
                            }
                        }

                        sub.postAtMillis = cal.timeInMillis
                        viewModel.updateSubmission(sub)

                        withUIContext { findNavController().navigateUp() }
                    }
                }
            }
            .show()
    }

    inner class TabCollectionAdapterDefault(fragment: Fragment) : FragmentStateAdapter(fragment) {
        override fun getItemCount(): Int = 5

        override fun createFragment(position: Int): Fragment {
            // Return a NEW fragment instance
            return when ((args.submission as Submission).kind) {
                SubmissionKind.Link -> LinkSubmissionFragment(viewModel, args.submission, null)
                SubmissionKind.Image -> ImageSubmissionFragment(viewModel, args.submission, null)
                SubmissionKind.Video ->  VideoSubmissionFragment(viewModel, args.submission, null)
                SubmissionKind.VideoGif -> VideoSubmissionFragment(viewModel, args.submission, null)
                SubmissionKind.Self -> TextSubmissionFragment(viewModel, args.submission, null)
                SubmissionKind.Poll -> PollSubmissionFragment(viewModel, args.submission, null)
                else ->  TextSubmissionFragment(viewModel, args.submission, null)
            }
        }
    }
}