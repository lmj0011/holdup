package name.lmj0011.redditdraftking.ui.testing

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.google.android.material.tabs.TabLayoutMediator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import name.lmj0011.redditdraftking.App
import name.lmj0011.redditdraftking.R
import name.lmj0011.redditdraftking.database.models.Account
import name.lmj0011.redditdraftking.database.AppDatabase
import name.lmj0011.redditdraftking.databinding.FragmentTestingBinding
import name.lmj0011.redditdraftking.helpers.RedditApiHelper
import name.lmj0011.redditdraftking.helpers.RedditAuthHelper
import name.lmj0011.redditdraftking.helpers.UniqueRuntimeNumberHelper
import name.lmj0011.redditdraftking.helpers.factories.ViewModelFactory
import name.lmj0011.redditdraftking.helpers.models.ImageSubmission
import name.lmj0011.redditdraftking.helpers.util.disableTabItemAt
import name.lmj0011.redditdraftking.helpers.util.enableTabItemAt
import name.lmj0011.redditdraftking.helpers.util.launchIO
import name.lmj0011.redditdraftking.ui.submission.*
import name.lmj0011.redditdraftking.ui.submission.bottomsheet.BottomSheetAccountsFragment
import org.kodein.di.instance
import timber.log.Timber

class TestingFragment: Fragment(R.layout.fragment_testing) {
    private lateinit var binding: FragmentTestingBinding
    private lateinit var requestCodeHelper: UniqueRuntimeNumberHelper
    private lateinit var redditAuthHelper: RedditAuthHelper
    private lateinit var redditApiHelper: RedditApiHelper
    private lateinit var testAccount: Account

    private val  testingViewModel by viewModels<TestingViewModel> {
        ViewModelFactory(
            AppDatabase.getInstance(requireActivity().application).sharedDao,
            requireActivity().application)
    }

    private val MEDIA_PICK_REQUEST_CODE = 1000


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        redditAuthHelper = (requireContext().applicationContext as App).kodein.instance()
        requestCodeHelper = (requireContext().applicationContext as App).kodein.instance()
        redditApiHelper = (requireContext().applicationContext as App).kodein.instance()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupBinding(view)
        setupObservers()

        launchIO {
            testingViewModel.getFirstAccount()?.let {
                testAccount = it
            }

            redditAuthHelper.authClient(testAccount).getSavedBearer().renewToken()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, intent: Intent?) {
        super.onActivityResult(requestCode, resultCode, intent)

        if (resultCode != Activity.RESULT_OK) {
            Timber.i("Intent failed! [requestCode: $resultCode, resultCode: $resultCode]")
            return
        }

        when(requestCode) {
            MEDIA_PICK_REQUEST_CODE -> {
                val token = redditAuthHelper.authClient(testAccount).getSavedBearer().getAccessToken()!!
                launchIO { intent?.data?.let {
                        try {
                            val submission = ImageSubmission(
                                title = "YoshiPoshi",
                                subreddit = "draftkingtesting",
                                image = it
                            )
                            redditApiHelper.submitImage(submission, token)
                            Timber.d("Image submission \"${submission.title}\" is live!")
                        } catch (ex: Exception) {
                            Timber.e(ex)
                        }

                    }
                }
            }
        }
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    private fun setupBinding(view: View) {
        binding = FragmentTestingBinding.bind(view)
        binding.lifecycleOwner = this
        binding.pager.adapter = TabCollectionAdapter(this)
        binding.pager.isUserInputEnabled = false // prevent swiping navigation ref: https://stackoverflow.com/a/55193815/2445763
        TabLayoutMediator(binding.submissionTabLayout, binding.pager) { tab, position ->
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
        }.attach()
    }

    private fun setupObservers() {
//        binding.onessumbitImagePostButton.setOnClickListener {
//            val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
//                // Filter to only show results that can be "opened", such as
//                // a file (as opposed to a list of contacts or timezones).
//                addCategory(Intent.CATEGORY_OPENABLE)
//
//                type = "image/*"
//            }
//
//            startActivityForResult(intent, MEDIA_PICK_REQUEST_CODE)
//        }
    }

    inner class TabCollectionAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {
        override fun getItemCount(): Int = 5

        override fun createFragment(position: Int): Fragment {
            // Return a NEW fragment instance in createFragment(int)
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
}