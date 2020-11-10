package name.lmj0011.redditdraftking.ui.subredditdrafts

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.format.DateFormat
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.DividerItemDecoration
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.DateValidatorPointForward
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import kotlinx.coroutines.*
import name.lmj0011.redditdraftking.App
import name.lmj0011.redditdraftking.MainActivity
import name.lmj0011.redditdraftking.R
import name.lmj0011.redditdraftking.database.AppDatabase
import name.lmj0011.redditdraftking.databinding.FragmentSubredditDraftsBinding
import name.lmj0011.redditdraftking.helpers.UniqueRuntimeNumberHelper
import name.lmj0011.redditdraftking.helpers.DateTimeHelper.getLocalDateFromUtcMillis
import name.lmj0011.redditdraftking.helpers.adapters.DraftListAdapter
import name.lmj0011.redditdraftking.helpers.factories.ViewModelFactory
import name.lmj0011.redditdraftking.helpers.receivers.PublishScheduledDraftReceiver
import name.lmj0011.redditdraftking.helpers.util.launchIO
import org.kodein.di.instance
import timber.log.Timber
import java.util.*

class SubredditDraftsFragment : Fragment(R.layout.fragment_subreddit_drafts) {
    private lateinit var binding: FragmentSubredditDraftsBinding
    private val args: SubredditDraftsFragmentArgs by navArgs()
    private val  subredditDraftsViewModel by viewModels<SubredditDraftsViewModel> {
        ViewModelFactory(
            AppDatabase.getInstance(requireActivity().application).sharedDao,
            requireActivity().application)
    }
    private lateinit var listAdapter: DraftListAdapter
    private lateinit var alarmMgr: AlarmManager
    private lateinit var requestCodeHelper: UniqueRuntimeNumberHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        alarmMgr = requireContext().getSystemService(Context.ALARM_SERVICE) as AlarmManager
        requestCodeHelper = (requireContext().applicationContext as App).kodein.instance()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupBinding(view)
        setupRecyclerView()
        setupObservers()
        setupSwipeToRefresh()
        refreshRecyclerView()

        launchIO {
            val subreddit = subredditDraftsViewModel.getSubreddit(args.subredditUuid)
            withContext(Dispatchers.Main) {
                subreddit?.let { (requireActivity() as MainActivity).supportActionBar?.subtitle = it.displayNamePrefixed }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        refreshRecyclerView()
    }

    private fun setupBinding(view: View) {
        binding = FragmentSubredditDraftsBinding.bind(view)
        binding.lifecycleOwner = this
        binding.subredditDraftsViewModel = subredditDraftsViewModel
    }

    private fun setupRecyclerView() {
        listAdapter = DraftListAdapter(
            DraftListAdapter.AlarmIconClickListener {draft ->
                Timber.d("draft: $draft")
                // see https://github.com/material-components/material-components-android/issues/882#issuecomment-638983598
                val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
                // ^ because the datepicker give us UTC time to convert ourselves

                // big Up! https://stackoverflow.com/a/62080582/2445763
                val constraintBuilder = CalendarConstraints.Builder()
                constraintBuilder.setValidator(DateValidatorPointForward.now())

                val dateBuilder = MaterialDatePicker.Builder.datePicker()
                dateBuilder.setCalendarConstraints(constraintBuilder.build())
                val datePicker = dateBuilder.build()

                @TimeFormat val clockFormat = when(DateFormat.is24HourFormat(context)) {
                    true -> TimeFormat.CLOCK_24H
                    else -> TimeFormat.CLOCK_12H
                }

                val timePicker = MaterialTimePicker.Builder()
                    .setTimeFormat(clockFormat)
                    .build()

                datePicker.addOnPositiveButtonClickListener { millis ->
                    cal.timeInMillis = millis
                    timePicker.addOnPositiveButtonClickListener {
                        cal.set(Calendar.HOUR_OF_DAY, timePicker.hour)
                        cal.set(Calendar.MINUTE, timePicker.minute)

                        draft.requestCode = requestCodeHelper.nextInt()
                        val alarmIntent = Intent(context, PublishScheduledDraftReceiver::class.java).let { intent ->
                            intent.putExtra("draftUuid", draft.uuid)
                            PendingIntent.getBroadcast(context, draft.requestCode, intent, 0)
                        }

                        draft.postAtMillis = cal.timeInMillis
                        GlobalScope.launch(Dispatchers.IO) {
                            subredditDraftsViewModel.updateDraft(draft)

                            withContext(Dispatchers.Main) {
                                val localDate = getLocalDateFromUtcMillis(draft.postAtMillis)
                                if(localDate != null) {
                                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                                        alarmMgr.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, localDate.time, alarmIntent)
                                    } else {
                                        alarmMgr.set(AlarmManager.RTC_WAKEUP, localDate.time, alarmIntent)
                                    }
                                    refreshRecyclerView()
                                } else {
                                    // TODO Show ERROR "Unable to `getLocalDateFromUtcMillis`"
                                    // reset draft.postAtMillis = Const.UNIX_EPOCH
                                }
                            }
                        }
                    }

                    timePicker.show(parentFragmentManager, "time_picker_tag")
                }


                datePicker.show(parentFragmentManager, "date_picker_tag")
            }
        )

        val decor = DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL)
        binding.draftList.addItemDecoration(decor)
        binding.draftList.adapter = listAdapter
    }

    private fun setupSwipeToRefresh() {
        binding.swipeRefresh.setOnRefreshListener {
            refreshRecyclerView()
            binding.swipeRefresh.isRefreshing = false
        }
    }

    private fun refreshRecyclerView() {
        launchIO {
            val drafts = subredditDraftsViewModel.getDrafts(args.subredditUuid)

            withContext(Dispatchers.Main) {
                listAdapter.submitList(drafts)
                listAdapter.notifyDataSetChanged()
            }
        }
    }

    private fun setupObservers() {}

    override fun onDestroyView() {
        super.onDestroyView()
        (requireActivity() as MainActivity).supportActionBar?.subtitle = null
    }

}