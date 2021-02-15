package name.lmj0011.holdup

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import name.lmj0011.holdup.databinding.ActivityFullscreenTextEntryBinding

class FullscreenTextEntryActivity : AppCompatActivity() {

    companion object {
        const val FULLSCREEN_TEXT_ENTRY_REQUEST_CODE = 100
        const val RESULT_OUTPUT_TEXT = "RESULT_OUTPUT_TEXT"
    }

    private lateinit var binding: ActivityFullscreenTextEntryBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = DataBindingUtil.setContentView(this, R.layout.activity_fullscreen_text_entry)
        binding.lifecycleOwner = this

        val startText = intent.getStringExtra("start_text")
        val startPosition = intent.getIntExtra("start_position", 0)

        binding.textEditTextTextMultiLine.setText(startText)
        binding.textEditTextTextMultiLine.setSelection(startPosition)

        showKeyBoard(binding.textEditTextTextMultiLine)

        binding.saveButton.setOnClickListener {
            val intent = Intent()
            intent.putExtra(RESULT_OUTPUT_TEXT, binding.textEditTextTextMultiLine.text.toString())
            setResult(0, intent)
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