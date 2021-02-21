package name.lmj0011.holdup.helpers.util

import name.lmj0011.holdup.R
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.net.Uri
import android.os.Build
import androidx.datastore.preferences.core.Preferences
import android.os.PowerManager
import android.view.View
import android.view.ViewGroup
import androidx.core.view.children
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayout
import name.lmj0011.holdup.BuildConfig
import name.lmj0011.holdup.database.SharedDao
import java.util.UUID


/**
 * return true if in App's Battery settings "Not optimized" and false if "Optimizing battery use"
 * big Up! https://stackoverflow.com/a/49098293/2445763
 */
fun isIgnoringBatteryOptimizations(context: Context): Boolean {
    val pwrm = context.applicationContext.getSystemService(Context.POWER_SERVICE) as PowerManager
    val name = context.applicationContext.packageName

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
        arrayOf(intArrayOf(android.R.attr.state_checked), intArrayOf()), intArrayOf(
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

fun sendGeneralFeedback(context: Context) {
    val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:"))
    intent.putExtra(Intent.EXTRA_EMAIL, arrayOf(context.getString(name.lmj0011.holdup.R.string.dev_email)))
    intent.putExtra(Intent.EXTRA_SUBJECT, "General Feedback")
    intent.putExtra(Intent.EXTRA_TEXT, "")

    context.startActivity(intent)
}

fun sendBugReport(context: Context, template: String) {
    val uniqueID = UUID.randomUUID().toString().take(5)
    val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:"))
    intent.putExtra(Intent.EXTRA_EMAIL, arrayOf(context.getString(name.lmj0011.holdup.R.string.dev_email)))
    intent.putExtra(Intent.EXTRA_SUBJECT, "Bug Report #$uniqueID")
    intent.putExtra(Intent.EXTRA_TEXT, template)

    context.startActivity(intent)
}

fun getDebugDump(context: Context, prefs: Preferences, dao: SharedDao): String{
    val text = """


== enter message above this line ==

deviceOem: ${Build.MANUFACTURER}
deviceModel: ${Build.MODEL}
osVersion: Android ${Build.VERSION.RELEASE}
apiLevel: ${Build.VERSION.SDK_INT}
=
versionName: ${BuildConfig.VERSION_NAME}
buildType: ${BuildConfig.BUILD_TYPE}
versionCode: ${BuildConfig.VERSION_CODE}
gitCommitCount: ${context.getString(R.string.git_commit_count)}
gitCommitSha: ${context.getString(R.string.git_commit_sha)}
appBuildTime: ${context.getString(R.string.app_buildtime)}
=
preferences: $prefs
isIgnoringBatteryOptimizations: ${isIgnoringBatteryOptimizations(context)}
accounts: ${dao.accountsRowCount()}
submissions: ${dao.submissionsRowCount()}

====
""".trimIndent()

    return text
}