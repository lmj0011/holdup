package name.lmj0011.redditdraftking

import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.webkit.CookieManager
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import name.lmj0011.redditdraftking.database.AppDatabase
import name.lmj0011.redditdraftking.databinding.ActivityFullscreenTextEntryBinding
import name.lmj0011.redditdraftking.databinding.ActivityMainBinding
import name.lmj0011.redditdraftking.helpers.NotificationHelper
import name.lmj0011.redditdraftking.helpers.enums.SubmissionKind
import name.lmj0011.redditdraftking.helpers.util.isIgnoringBatteryOptimizations
import name.lmj0011.redditdraftking.helpers.workers.ScheduledDraftServiceCallerWorker
import name.lmj0011.redditdraftking.ui.home.HomeFragmentDirections
import name.lmj0011.redditdraftking.ui.submission.SubmissionViewModel
import timber.log.Timber

class FullscreenTextEntryActivity : AppCompatActivity() {

    companion object {
        const val FULLSCREEN_TEXT_ENTRY_REQUEST_CODE = 100
    }

    private lateinit var binding: ActivityFullscreenTextEntryBinding
    private lateinit var  viewModel: SubmissionViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = SubmissionViewModel.getInstance(
            AppDatabase.getInstance(this.application).sharedDao, this.application
        )

        binding = DataBindingUtil.setContentView(this, R.layout.activity_fullscreen_text_entry)
        binding.lifecycleOwner = this

        val startText = intent.getStringExtra("start_text")
        val startPosition = intent.getIntExtra("start_position", 0)

        binding.textEditTextTextMultiLine.setText(startText)
        binding.textEditTextTextMultiLine.setSelection(startPosition)

        showKeyBoard(binding.textEditTextTextMultiLine)

        binding.saveButton.setOnClickListener {
            val text = binding.textEditTextTextMultiLine.text.toString()

            when(intent.getStringExtra("kind")) {
                SubmissionKind.Self.kind -> viewModel.submissionSelfText.postValue(text)
                SubmissionKind.Poll.kind -> viewModel.submissionPollBodyText.postValue(text)
            }

            hideKeyBoard(binding.textEditTextTextMultiLine)
            finish()
        }

        binding.backImageButton.setOnClickListener {
            hideKeyBoard(binding.textEditTextTextMultiLine)
            finish()
        }

    }

    fun showKeyBoard(v: View) {
        v.requestFocus()
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
        imm?.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0)
    }

    fun hideKeyBoard(v: View) {
        v.clearFocus()
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
        imm?.hideSoftInputFromWindow(v.windowToken, 0)
    }
}