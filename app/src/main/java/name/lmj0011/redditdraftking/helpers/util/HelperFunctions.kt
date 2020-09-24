package name.lmj0011.redditdraftking.helpers.util

import android.content.Context
import android.os.Build
import android.os.PowerManager

/**
 * return true if in App's Battery settings "Not optimized" and false if "Optimizing battery use"
 * big Up! https://stackoverflow.com/a/49098293/2445763
 */
fun isIgnoringBatteryOptimizations(context: Context): Boolean {
    val pwrm = context.applicationContext.getSystemService(Context.POWER_SERVICE) as PowerManager
    val name = context.applicationContext.packageName
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        return pwrm.isIgnoringBatteryOptimizations(name)
    }
    return true
}