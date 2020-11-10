package name.lmj0011.redditdraftking.helpers.models

/**
 * For moshi.adapter
 */
data class MediaAssetUploadResponse(
    val args: MediaAssetUploadArgs,
    val asset: MediaAssetUploadAsset
)

data class MediaAssetUploadArgs(
    val action: String,
    val fields: List<MediaAssetUploadFieldPairs>
)

data class MediaAssetUploadFieldPairs(
    val name: String,
    val value: String
)

data class MediaAssetUploadAsset(
    val asset_id: String,
    val processing_state: String,
    val payload: MediaAssetUploadAssetPayload,
    val websocket_url: String
)

data class MediaAssetUploadAssetPayload(
    val filepath: String
)



