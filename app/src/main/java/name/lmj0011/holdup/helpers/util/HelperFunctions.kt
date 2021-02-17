package name.lmj0011.holdup.helpers.util

import android.R
import android.content.Context
import android.content.res.ColorStateList
import android.os.Build
import android.os.PowerManager
import android.view.View
import android.view.ViewGroup
import androidx.core.view.children
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayout
import timber.log.Timber


/**
 * return true if in App's Battery settings "Not optimized" and false if "Optimizing battery use"
 * big Up! https://stackoverflow.com/a/49098293/2445763
 */
fun isIgnoringBatteryOptimizations(context: Context): Boolean {
    val pwrm = context.applicationContext.getSystemService(Context.POWER_SERVICE) as PowerManager
    val name = context.applicationContext.packageName
    Timber.d("context.applicationContext.packageName: $name")
    Timber.d("pwrm.isIgnoringBatteryOptimizations: ${pwrm.isIgnoringBatteryOptimizations(name)}")
    return pwrm.isIgnoringBatteryOptimizations(name)
}


fun disableTabItemAt(tabLayout: TabLayout?, tabText: String) {
    (tabLayout?.getChildAt(0) as? ViewGroup)?.children?.iterator()?.forEach {
            if((it as TabLayout.TabView).tab?.text == tabText) {
                it.isEnabled = false
                it.alpha = 0.5f
            }
    }
}

fun enableTabItemAt(tabLayout: TabLayout?, tabText: String) {
    (tabLayout?.getChildAt(0) as? ViewGroup)?.children?.iterator()?.forEach {
        if((it as TabLayout.TabView).tab?.text == tabText) {
            it.isEnabled = true
            it.alpha = 1f
        }
    }
}


/**
 * returns a ColorStateList that uses only 1 Color
 */
fun buildOneColorStateList(color: Int /* Color.parseColor("#FFFF") */): ColorStateList? {
    return ColorStateList(
        arrayOf(intArrayOf(R.attr.state_checked), intArrayOf()), intArrayOf(
            color,
            color
        )
    )
}

/**
 * displays a vanilla snackBar
 */
fun showSnackBar(view: View, message: String) {
    val snackBar = Snackbar.make(view, message, Snackbar.LENGTH_LONG)
    snackBar.show()
}