package name.lmj0011.holdup.ui.submission

import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.asFlow
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.work.*
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import name.lmj0011.holdup.App
import name.lmj0011.holdup.R
import name.lmj0011.holdup.database.AppDatabase
import name.lmj0011.holdup.database.models.Account
import name.lmj0011.holdup.databinding.FragmentScheduleCommentBinding
import name.lmj0011.holdup.helpers.DataStoreHelper
import name.lmj0011.holdup.helpers.interfaces.BaseFragmentInterface
import name.lmj0011.holdup.helpers.models.Thing1
import name.lmj0011.holdup.helpers.models.Thing3
import name.lmj0011.holdup.helpers.util.*
import name.lmj0011.holdup.helpers.workers.CommentDelayWorker
import name.lmj0011.holdup.ui.submission.bottomsheet.BottomSheetAccountsFragment
import org.kodein.di.instance
import timber.log.Timber
import java.util.concurrent.TimeUnit

class ScheduleCommentFragment : Fragment(R.layout.fragment_schedule_comment), BaseFragmentInterface {
    private lateinit var dataStoreHelper: DataStoreHelper
    private lateinit var viewModel: ScheduleCommentViewModel
    private lateinit var binding: FragmentScheduleCommentBinding
    lateinit var bottomSheetAccountsFragment: BottomSheetAccountsFragment

    private var optionsMenu: Menu? = null
    private lateinit var account: Account
    private lateinit var post: Thing3
    private var comment: Thing1? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ScheduleCommentViewModel.getInstance(
                AppDatabase.getInstance(requireActivity().application).sharedDao,
                requireActivity().application
            )

        dataStoreHelper = (requireContext().applicationContext as App).kodein.instance()

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.CREATED) {
                launchIO {
                    viewModel.post.collectLatest { thing3 ->
                        post = thing3
                        thing3.let { thing ->
                            withUIContext {
                                binding.SubredditPostConstraintLayout.visibility = View.VISIBLE
                                binding.submissionTitleTextView.text = thing.title
                                binding.subredditDisplayNameTextView.text = thing.subredditNamePrefixed
                            }
                        }
                    }
                }

                launchIO {
                    viewModel.postSubredditImgUrl.collectLatest { url ->
                        withUIContext {
                            Glide
                                .with(this@ScheduleCommentFragment)
                                .load(url)
                                .apply(RequestOptions().override(100))
                                .circleCrop()
                                .error(R.drawable.ic_baseline_image_24)
                                .into(binding.subredditIconImageView)
                        }
                    }
                }

                launchIO {
                    viewModel.commenterComment.collectLatest { thing1 ->
                        comment = thing1
                        thing1.let { thing ->
                            withUIContext {
                                binding.CommenterConstraintLayout.visibility = View.VISIBLE
                                binding.commenterDisplayNameTextView.text = "u/${thing.author}"
                                binding.commenterCommentTextView.text = thing.body
                            }
                        }
                    }
                }

                launchIO {
                    viewModel.commenterImgUrl.collectLatest { url ->
                        withUIContext {
                            Glide
                                .with(this@ScheduleCommentFragment)
                                .load(url)
                                .apply(RequestOptions().override(100))
                                .circleCrop()
                                .error(R.drawable.ic_baseline_image_24)
                                .into(binding.commenterIconImageView)
                        }
                    }
                }

                launchIO {
                    viewModel.account.collectLatest { acct ->
                        account = acct
                        acct.let {
                            withUIContext {
                                binding.chooseAccountTextView.text = it.name

                                Glide
                                    .with(this@ScheduleCommentFragment)
                                    .load(it.iconImage)
                                    .apply(RequestOptions().override(100))
                                    .circleCrop()
                                    .error(R.drawable.ic_baseline_image_24)
                                    .into(binding.chooseAccountImageView)
                            }
                        }
                    }
                }
            }
        }
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
                menuInflater.inflate(R.menu.schedule_comment, menu)
            }

            override fun onPrepareMenu(menu: Menu) {
                super.onPrepareMenu(menu)
                optionsMenu = menu
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                return when (menuItem.itemId) {
                    R.id.action_comment_submission -> {
                        showPostConfirmationDialog()
                        true
                    }
                    else -> false
                }
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }

    override fun onPause() {
        super.onPause()
        binding.SubredditPostConstraintLayout.visibility = View.GONE
        binding.CommenterConstraintLayout.visibility = View.GONE
        launchIO { viewModel.resetFlows() }
    }

    override fun setupBinding(view: View) {
        binding = FragmentScheduleCommentBinding.bind(view)
        binding.lifecycleOwner = viewLifecycleOwner

        binding.chooseAccountLinearLayout.setOnClickListener {
            bottomSheetAccountsFragment = BottomSheetAccountsFragment { acct ->
                viewModel.setAccount(acct)
                bottomSheetAccountsFragment.dismiss()
            }

            bottomSheetAccountsFragment.show(childFragmentManager, "BottomSheetAccountsFragment")
        }
    }

    override fun setupObservers() {}

    override fun setupRecyclerView() {}

    /**
     *  clear any User input data from this Fragment
     */
    override fun clearUserInputViews() {}

    private fun showPostConfirmationDialog() {
        if(!binding.textEditTextTextMultiLine.text.isNullOrBlank()) {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle(getString(R.string.submit_this_reply))
                .setNeutralButton("Cancel") {_, _ ->

                }
                .setNegativeButton("") {_, _ -> }
                .setPositiveButton("Yes") { _, _ ->
                    val workData = if(comment is Thing1) {
                        workDataOf(
                            CommentDelayWorker.IN_KEY_ACCOUNT_ID to account.id,
                            CommentDelayWorker.IN_KEY_TEXT to binding.textEditTextTextMultiLine.text.toString(),
                            CommentDelayWorker.IN_KEY_THING_ID to comment!!.name
                        )
                    } else {
                        workDataOf(
                            CommentDelayWorker.IN_KEY_ACCOUNT_ID to account.id,
                            CommentDelayWorker.IN_KEY_TEXT to binding.textEditTextTextMultiLine.text.toString(),
                            CommentDelayWorker.IN_KEY_THING_ID to post.name
                        )
                    }

                    val commentDelayWork = OneTimeWorkRequestBuilder<CommentDelayWorker>()
                        .setInputData(workData)
                        .addTag(CommentDelayWorker.GROUP_TAG)
                        .setBackoffCriteria(
                            BackoffPolicy.LINEAR,
                            CommentDelayWorker.BACKOFF_DELAY_MINUTES,
                            TimeUnit.MINUTES)
                        .build()

                    WorkManager.getInstance(requireContext()).enqueue(commentDelayWork)

                    Toast.makeText(requireActivity(), getString(R.string.reply_submitted), Toast.LENGTH_LONG).show()

                    requireActivity().finish()

                }

                .show()
        } else {
            showSnackBar(binding.root, "Reply is blank..")
        }
    }
}