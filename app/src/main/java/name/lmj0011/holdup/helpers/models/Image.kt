package name.lmj0011.holdup.helpers.models

import android.os.Parcelable
import androidx.annotation.Keep
import kotlinx.parcelize.Parcelize

@Keep
@Parcelize
data class Image (
    val sourceUri: String, // uri to this image file (on device location)
    var mediaId: String, // the id of this image
    var url: String, // raw url to this image after it has been uploaded to Reddit
    val caption: String, // some info about this image
    val outboundUrl: String, //  arbitrary link to go with [caption]
) : Parcelable