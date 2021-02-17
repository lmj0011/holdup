package name.lmj0011.holdup.helpers.models

import android.os.Parcelable
import androidx.annotation.Keep
import kotlinx.parcelize.Parcelize

/**
 * For moshi.adapter
 */
@Keep
@Parcelize
data class MediaAssetUploadResponse(
    val args: MediaAssetUploadArgs,
    val asset: MediaAssetUploadAsset
) : Parcelable

@Keep
@Parcelize
data class MediaAssetUploadArgs(
    val action: String,
    val fields: List<MediaAssetUploadFieldPairs>
) : Parcelable

@Keep
@Parcelize
data class MediaAssetUploadFieldPairs(
    val name: String,
    val value: String
) : Parcelable

@Keep
@Parcelize
data class MediaAssetUploadAsset(
    val asset_id: String,
    val processing_state: String,
    val payload: MediaAssetUploadAssetPayload,
    val websocket_url: String
) : Parcelable

@Keep
@Parcelize
data class MediaAssetUploadAssetPayload(
    val filepath: String
) : Parcelable



