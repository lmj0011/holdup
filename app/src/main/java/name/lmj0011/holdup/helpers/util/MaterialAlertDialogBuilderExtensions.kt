package name.lmj0011.holdup.helpers.util

import android.app.Dialog
import android.content.DialogInterface
import android.view.LayoutInflater
import androidx.appcompat.app.AlertDialog
import androidx.core.widget.doOnTextChanged
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputLayout

fun MaterialAlertDialogBuilder.showInput(
    layout: Int,
    tilId: Int,
    hintRes: String = "",
    counterMaxLength: Int = 0,
    prefilled: String = ""
): Dialog {
    this.setView(layout)
    val dialog = this.show()
    val til = dialog.findViewById<TextInputLayout>(tilId)
    til?.let {
        til.hint = hintRes
        if (counterMaxLength > 0) {
            til.counterMaxLength = counterMaxLength
            til.isCounterEnabled = true
        }
        til.editText?.doOnTextChanged { text, start, before, count ->
            dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .isEnabled = !text.isNullOrBlank() && (counterMaxLength == 0 || text.length <= counterMaxLength)
        }
        til.editText?.append(prefilled)
        dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            .isEnabled = !prefilled.isBlank()
    }
    return dialog
}

fun DialogInterface.inputText(tilId: Int): String {
    return (this as AlertDialog).findViewById<TextInputLayout>(tilId)?.editText?.text?.toString().orEmpty()
}