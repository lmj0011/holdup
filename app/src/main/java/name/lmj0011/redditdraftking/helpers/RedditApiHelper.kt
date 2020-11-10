package name.lmj0011.redditdraftking.helpers

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.text.format.Formatter
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import name.lmj0011.redditdraftking.database.models.Account
import name.lmj0011.redditdraftking.helpers.adapters.JSONObjectAdapter
import name.lmj0011.redditdraftking.helpers.models.ImageSubmission
import name.lmj0011.redditdraftking.helpers.models.MediaAssetUploadResponse
import name.lmj0011.redditdraftking.helpers.models.Subreddit
import name.lmj0011.redditdraftking.helpers.util.InputStreamRequestBody
import name.lmj0011.redditdraftking.helpers.util.apiPath
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okio.IOException
import org.json.JSONException
import org.json.JSONObject
import timber.log.Timber
import java.util.concurrent.TimeUnit

class RedditApiHelper(val context: Context) {
    companion object {
        const val BASE_URL = "https://oauth.reddit.com/"
        
        val client = OkHttpClient.Builder()
            .readTimeout(0,  TimeUnit.MILLISECONDS)
            .build()

        val acceptedMimeTypes = listOf(
            "image/png", "video/quicktime", "video/mp4", "image/jpeg", "image/gif"
        )
    }

    fun get(apiPath: String, oauthToken: String): Response {
        val request = Request.Builder()
            .url("${BASE_URL}${apiPath}")
            .header("Authorization", "Bearer $oauthToken")
            .build()

        val response = client.newCall(request).execute()

        return if (!response.isSuccessful) throw IOException("Unexpected code $response")
        else response
    }

    fun post(apiPath: String, oauthToken: String, formBody: FormBody): Response {
        val request = Request.Builder()
            .url("${BASE_URL}${apiPath}")
            .header("Authorization", "Bearer $oauthToken")
            .post(formBody)
            .build()

        val response = client.newCall(request).execute()

        return if (!response.isSuccessful) throw IOException("Unexpected code $response")
        else response
    }


    fun parseSubredditListingResponse(json: JSONObject): List<Subreddit> {
        var subredditSet = mutableSetOf<Subreddit>()

        val redditMixedEntityArray =
            json.getJSONObject("data").getJSONArray("children")

        for (i in 0 until redditMixedEntityArray.length()) {
            val obj = redditMixedEntityArray.getJSONObject(i)

            /**
             * t5 represents a Subreddit type
             * refer to the "overview" section https://www.reddit.com/dev/api
             */
            if (obj.getString("kind") == "t5") {
                try {

                    var iconImgUrl = if (obj.getJSONObject("data").getString("icon_img").isNullOrBlank()) {
                        obj.getJSONObject("data").getString("community_icon")
                    } else obj.getJSONObject("data").getString("icon_img")

                    iconImgUrl = iconImgUrl.split("?").first()
                    val sub = Subreddit(
                        displayName = obj.getJSONObject("data").getString("display_name"),
                        displayNamePrefixed = obj.getJSONObject("data").getString("display_name_prefixed"),
                        iconImgUrl = iconImgUrl,
                        subscribers = obj.getJSONObject("data").getInt("subscribers")
                    )
                    subredditSet.add(sub)
                } catch(ex: JSONException) {
                    // fahgettaboudit
                }

            }
        }

        return subredditSet.toList()
    }

    /**
     * Get a Listing of Subreddits recently contributed to
     */
    fun getRecentSubreddits(acct: Account, oauthToken: String): List<Subreddit> {
        val apiPath = "user/${acct.name.substring(2)}/submitted?sort=new"
        val res = get(apiPath, oauthToken)

        val data = JSONObject(res.body!!.source().readUtf8())

        val redditMixedEntityArray =
            data.getJSONObject("data").getJSONArray("children")

        val subredditIdSet = mutableSetOf<String>()
        for (i in 0 until redditMixedEntityArray.length()) {
            val obj = redditMixedEntityArray.getJSONObject(i)
            subredditIdSet.add(obj.getJSONObject("data").getString("subreddit_id"))
        }

        val res2 = get("api/info?id=${subredditIdSet.take(5).joinToString(",")}", oauthToken)
        return parseSubredditListingResponse(JSONObject(res2.body!!.source().readUtf8()))
    }

    fun getSubscribedSubreddits(oauthToken: String): List<Subreddit> {
        val apiPath = "subreddits/mine/subscriber?limit=100"
        val res = get(apiPath, oauthToken)

        return parseSubredditListingResponse(JSONObject(res.body!!.source().readUtf8()))
    }

    fun submitSubredditQuery(query: String, oauthToken: String): List<Subreddit> {
        val url = "api/subreddit_autocomplete_v2.json"
        val res = get("${url}?query=${query}&include_profiles=false&typeahead_active=true&raw_json=1&gilding_detail=1",
            oauthToken)

        return parseSubredditListingResponse(JSONObject(res.body!!.source().readUtf8()))
    }

    /**
     * Add a selftext or Link submission to the subreddit.
     */
    fun submit() {}

    /**
     * Add an image submission to the subreddit.
     */
    fun submitImage(submission: ImageSubmission, oauthToken: String) {
        val formBodyBuilder = FormBody.Builder()

        formBodyBuilder.add("sr", submission.subreddit)
        formBodyBuilder.add("api_type", submission.apiType)
        formBodyBuilder.add("show_error_list", submission.showErrorList.toString())
        formBodyBuilder.add("title", submission.title)
        formBodyBuilder.add("spoiler", submission.spoiler.toString())
        formBodyBuilder.add("nsfw", submission.nsfw.toString())
        formBodyBuilder.add("kind", submission.kind)
        formBodyBuilder.add("original_content", submission.originalContent.toString())
        formBodyBuilder.add("submit_type", submission.submitType)
        formBodyBuilder.add("post_to_twitter", submission.postToTwitter.toString())
        formBodyBuilder.add("sendreplies", submission.sendReplies.toString())
        formBodyBuilder.add("url", uploadMedia(submission.image, oauthToken))
        formBodyBuilder.add("validate_on_submit", submission.validateOnSubmit.toString())

        submitMedia(formBodyBuilder.build(), oauthToken)
    }

    /**
     * for submitting posts that are not link posts or self
     * posts.
     */
    private fun submitMedia(formBody: FormBody, oauthToken: String, withWebsockets: Boolean = false) {
        if (withWebsockets) {

        } else {
            val url = apiPath["submit"] ?: error("api path not found!")
            post("${url}?resubmit=true&raw_json=1&gilding_detail=1", oauthToken, formBody)
        }
    }

    /**
     * uploads a media file to Reddit and returns the url link
     */
    private fun uploadMedia(uri: Uri, oauthToken: String): String {
        var mediaUrl = ""
        val contentResolver = context.contentResolver

        val mimeType = contentResolver.getType(uri).toString()
        Timber.d("mimetype: $mimeType")

        // TODO - throw Exception if not an accepted mime type
        val accepted = acceptedMimeTypes.contains(contentResolver.getType(uri))
        Timber.d("acceptedMimetype: $accepted")

        // ref: https://developer.android.com/training/secure-file-sharing/retrieve-info#RetrieveFileInfo
        contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
            cursor.moveToFirst()
            Timber.d("fileName: ${cursor.getString(nameIndex)}")
            Timber.d("fileSize: ${Formatter.formatShortFileSize(context, cursor.getLong(sizeIndex))}")

            val fileName = cursor.getString(nameIndex)
            val fileSize = cursor.getLong(sizeIndex)

            val formBody = FormBody.Builder()
                .add("filepath", fileName)
                .add("mimetype", mimeType)
                .build()

            val url = apiPath["media_asset"] ?: error("api path not found!")
            val mediaAssetApiResponse = post("${url}?raw_json=1&gilding_detail=1", oauthToken, formBody)

            val moshi = Moshi.Builder()
                .add(JSONObjectAdapter)
                .add(KotlinJsonAdapterFactory())
                .build()

            val mediaAssetUploadResponseAdapter = moshi.adapter(MediaAssetUploadResponse::class.java)
            val mediaAssetUploadResponse = mediaAssetUploadResponseAdapter.fromJson(mediaAssetApiResponse.body!!.source())
            //Timber.d("mediaAssetUploadResponse: $mediaAssetUploadResponse")

            val requestBodyBuilder = MultipartBody.Builder().setType(MultipartBody.FORM)
            var uploadFileKey = ""

            mediaAssetUploadResponse?.args?.fields?.onEach { pair ->
                if(pair.name == "key") uploadFileKey = pair.value
                requestBodyBuilder.addFormDataPart(pair.name, pair.value)
            }.also {_ ->
                val contentPart = InputStreamRequestBody(mimeType.toMediaType(), fileSize, contentResolver, uri)
                requestBodyBuilder.addFormDataPart("file", fileName, contentPart)
            }

            val requestBody = requestBodyBuilder.build()
            val uploadUrl = "https:${mediaAssetUploadResponse?.args?.action}"
            val request = Request.Builder()
                .url(uploadUrl)
                .post(requestBody)
                .build()

            val uploadResponse = client.newCall(request).execute()

            when {
                uploadResponse.isSuccessful -> mediaUrl = "${uploadUrl}/${uploadFileKey}"
                else -> throw IOException("Unexpected code: $uploadResponse")
            }

        }
        return mediaUrl
    }
}
