package name.lmj0011.holdup.helpers.models

import android.os.Parcelable
import androidx.annotation.Keep
import kotlinx.parcelize.Parcelize

@Keep
@Parcelize
data class Video (
    val sourceUri: String, // uri to this video file (device location)
    var mediaId: String, // the id of this video
    var url: String, // raw url to this video after it has been uploaded to Reddit
    var posterUrl: String // video thumbnail file
) : Parcelable