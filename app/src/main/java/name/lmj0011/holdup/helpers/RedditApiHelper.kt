package name.lmj0011.holdup.helpers

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.text.format.Formatter
import androidx.core.net.toUri
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import name.lmj0011.holdup.database.models.Account
import name.lmj0011.holdup.helpers.adapters.JSONObjectAdapter
import name.lmj0011.holdup.helpers.enums.SubmissionKind
import name.lmj0011.holdup.helpers.models.*
import name.lmj0011.holdup.helpers.util.InputStreamRequestBody
import name.lmj0011.holdup.helpers.util.apiPath
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import org.jsoup.HttpStatusException
import timber.log.Timber
import java.io.File
import java.net.URL
import java.util.concurrent.TimeUnit
import java.util.logging.Level
import java.util.logging.Logger

class RedditApiHelper(val context: Context) {
    companion object {
        const val BASE_URL = "https://oauth.reddit.com/"

        private val okHttpLogging = HttpLoggingInterceptor { message ->
            Timber.d(message)
        }

        init {
            Logger.getLogger(OkHttpClient::class.java.name).level = Level.FINE
            okHttpLogging.setLevel(HttpLoggingInterceptor.Level.BASIC)
            okHttpLogging.redactHeader("Authorization")
        }

        val client = OkHttpClient.Builder()
            .readTimeout(0,  TimeUnit.MILLISECONDS)
            .addInterceptor(okHttpLogging)
            .build()

        val acceptedMimeTypes = listOf(
            "image/png", "video/quicktime", "video/mp4", "image/jpeg", "image/gif"
        )

        /**
         * This function can be called to parse responses from SubmissionViewModel.postSubmission
         *
         * returns a Pair<jsonData, errorMessage>
         */
        fun parseSubmitResponse(json: JSONObject): Pair<JSONObject?, String?> {
            var data: JSONObject? = null
            var errorMessage: String? = null

            try {
                data = json.getJSONObject("json").getJSONObject("data")

            } catch(ex: JSONException) {
                Timber.e(json.toString(2))
                val errors = json.getJSONObject("json").getJSONArray("errors")

                if (errors.length() > 0) {
                    val arr = errors.getJSONArray(0)
                    errorMessage = "error: ${arr.join(" ").replace("\"", "")}"
                } else throw ex
            }

            return Pair(data, errorMessage)
        }
    }

    fun get(apiPath: String, oauthToken: String): Response {
        val request = Request.Builder()
            .url("${BASE_URL}${apiPath}")
            .header("Authorization", "Bearer $oauthToken")
            .build()

        val response = client.newCall(request).execute()

        return if (!response.isSuccessful) throw HttpStatusException(response.message, response.code, response.request.url.toString())
        else response
    }

    fun post(apiPath: String, oauthToken: String, formBody: FormBody): Response {
        val request = Request.Builder()
            .url("${BASE_URL}${apiPath}")
            .header("Authorization", "Bearer $oauthToken")
            .post(formBody)
            .build()

        val response = client.newCall(request).execute()

        return if (!response.isSuccessful) throw HttpStatusException(response.message, response.code, response.request.url.toString())
        else response
    }

    fun post(apiPath: String, oauthToken: String, requestBody: RequestBody): Response {
        val request = Request.Builder()
            .url("${BASE_URL}${apiPath}")
            .header("Authorization", "Bearer $oauthToken")
            .post(requestBody)
            .build()

        val response = client.newCall(request).execute()

        return if (!response.isSuccessful) throw HttpStatusException(response.message, response.code, response.request.url.toString())
        else response
    }


    private fun parseSubredditListingResponse(json: JSONObject): List<Subreddit> {
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

                    val dataObj = obj.getJSONObject("data")
                    var iconImgUrl = if (dataObj.getString("icon_img").isNullOrBlank()) {
                        dataObj.getString("community_icon")
                    } else dataObj.getString("icon_img")

                    iconImgUrl = iconImgUrl.split("?").first()
                    val sub = Subreddit(
                        guid = dataObj.getString("name"),
                        displayName = dataObj.getString("display_name"),
                        displayNamePrefixed = dataObj.getString("display_name_prefixed"),
                        iconImgUrl = iconImgUrl,
                        subscribers = dataObj.getInt("subscribers"),
                        allowGalleries = dataObj.getBoolean("allow_galleries"),
                        allowImages = dataObj.getBoolean("allow_images"),
                        allowVideos = dataObj.getBoolean("allow_videos"),
                        allowVideoGifs = dataObj.getBoolean("allow_videogifs"),
                        allowPolls = dataObj.getBoolean("allow_polls"),
                        linkFlairEnabled = dataObj.getBoolean("link_flair_enabled"),
                        over18 = dataObj.getBoolean("over18")
                    )
                    subredditSet.add(sub)
                } catch(ex: JSONException) {
                    Timber.e(ex)
                }

            }
        }

        return subredditSet.toList()
    }

    private fun parseThing3ListingResponse(json: JSONObject): List<Thing3> {
        var thing3Set = mutableSetOf<Thing3>()

        val redditMixedEntityArray =
            json.getJSONObject("data").getJSONArray("children")

        for (i in 0 until redditMixedEntityArray.length()) {
            val obj = redditMixedEntityArray.getJSONObject(i)

            /**
             * t3 represents a Link/Post type
             * refer to the "overview" section https://www.reddit.com/dev/api
             */
            if (obj.getString("kind") == "t3") {
                try {

                    val dataObj = obj.getJSONObject("data")

                    val sub = Thing3(
                        subreddit = dataObj.getString("subreddit"),
                        likes = if(dataObj.isNull("likes")) false else dataObj.getBoolean("likes"),
                        authorFullname =  if(dataObj.isNull("author_fullname"))  "" else dataObj.getString("author_fullname"),
                        title = dataObj.getString("title"),
                        subredditNamePrefixed = dataObj.getString("subreddit_name_prefixed"),
                        downs = dataObj.getInt("downs"),
                        name = dataObj.getString("name"),
                        upvoteRatio = dataObj.getDouble("upvote_ratio"),
                        ups = dataObj.getInt("ups"),
                        score = dataObj.getInt("score"),
                        thumbnail = dataObj.getString("thumbnail"),
                        isSelf = dataObj.getBoolean("is_self"),
                        subredditType = dataObj.getString("subreddit_type"),
                        over18 = dataObj.getBoolean("over_18"),
                        subredditId = dataObj.getString("subreddit_id"),
                        author = dataObj.getString("author"),
                        permalink = dataObj.getString("permalink")
                    )
                    thing3Set.add(sub)
                } catch(ex: JSONException) {
                    Timber.e(ex)
                    Timber.d(obj.getJSONObject("data").toString())
                }

            }
        }

        return thing3Set.toList()
    }

    private fun parseThing1ListingResponse(json: JSONObject): List<Thing1> {
        var thing1Set = mutableSetOf<Thing1>()

        val redditMixedEntityArray =
            json.getJSONObject("data").getJSONArray("children")

        for (i in 0 until redditMixedEntityArray.length()) {
            val obj = redditMixedEntityArray.getJSONObject(i)

            /**
             * t1 represents a Comment type
             * refer to the "overview" section https://www.reddit.com/dev/api
             */
            if (obj.getString("kind") == "t1") {
                try {

                    val dataObj = obj.getJSONObject("data")

                    val thing = Thing1(
                        subreddit = dataObj.getString("subreddit"),
                        likes = if(dataObj.isNull("likes")) false else dataObj.getBoolean("likes"),
                        authorFullname = if(dataObj.isNull("author_fullname"))  "" else dataObj.getString("author_fullname"),
                        subredditNamePrefixed = dataObj.getString("subreddit_name_prefixed"),
                        downs = dataObj.getInt("downs"),
                        name = dataObj.getString("name"),
                        ups = dataObj.getInt("ups"),
                        score = dataObj.getInt("score"),
                        subredditType = dataObj.getString("subreddit_type"),
                        subredditId = dataObj.getString("subreddit_id"),
                        author = dataObj.getString("author"),
                        permalink = dataObj.getString("permalink")
                    )
                    thing1Set.add(thing)
                } catch(ex: JSONException) {
                    Timber.e(ex)
                    Timber.d(obj.getJSONObject("data").toString())
                }

            }
        }

        return thing1Set.toList()
    }

    fun getPostRequirements(subreddit: Subreddit, oauthToken: String): PostRequirements? {
        val apiPath = "api/v1/${subreddit.displayName}/post_requirements"
        val res = get(apiPath, oauthToken)

        val moshi = Moshi.Builder()
            .add(JSONObjectAdapter)
            .add(KotlinJsonAdapterFactory())
            .build()

        val postRequirementsJsonAdapter = moshi.adapter(PostRequirements::class.java)

        return try {
            val postRequirements = postRequirementsJsonAdapter.fromJson(res.body!!.source())
            postRequirements
        } catch(ex: Exception) {
            Timber.e(ex)
            null
        }
    }

    /**
     * Fetches a JSONArray of flairs belonging to the given Subreddit
     *
     * notes:
     * - if the Subreddit doesn't allow flairs (ie. r/funny), a 403 response is returned by the server.
     */
    fun getSubredditFlairList(subreddit: Subreddit, oauthToken: String): List<SubredditFlair> {
        val apiPath = "${subreddit.displayNamePrefixed}/api/link_flair_v2"
        var flairList = mutableListOf<SubredditFlair>()

        try {
            val res = get(apiPath, oauthToken)

            val flairArray = JSONArray(res.body!!.source().readUtf8())

            for (i in 0 until flairArray.length()) {
                val flairObj = flairArray.getJSONObject(i)

                flairList.add(
                    SubredditFlair(
                        id = flairObj.getString("id"),
                        text = flairObj.getString("text"),
                        textColor = flairObj.getString("text_color"),
                        backGroundColor = flairObj.getString("background_color"),
                    )
                )
            }
        } catch (ex: java.io.IOException) {
            Timber.w("${subreddit.displayNamePrefixed} has no flairs?")
        } finally {
            return flairList.toList()
        }
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

        val res2 = get("api/info?id=${subredditIdSet.joinToString(",")}", oauthToken)
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
     * fetches the icon_img from `/api/v1/me`
     *
     */
    fun getAccountImageUrl(oauthToken: String): String {
        val apiPath = "api/v1/me"
        val res = get(apiPath, oauthToken)

        val data = JSONObject(res.body!!.source().readUtf8())

        return when {
            data.getString("icon_img").isNullOrBlank().not() -> {
                val img = data.getString("icon_img").split("?")[0]
                URL(img).toString()
            }
            data.getString("snoovatar_img").isNullOrBlank().not() -> {
                val img = data.getString("snoovatar_img").split("?")[0]
                URL(img).toString()
            }
            else -> ""
        }
    }

    /**
     * fetches a Post by id
     *
     * [fullName]: t3_vqfm29
     * [oauthToken]: 1234585724870-velC4Oe6RURtyhmUIPrCWER-o6Y_rF
     */
    fun getPostById(fullName: String, oauthToken: String): Thing3 {
        val apiPath = "by_id/${fullName}"
        val res = get(apiPath, oauthToken)

        return parseThing3ListingResponse(JSONObject(res.body!!.source().readUtf8())).first()
    }

    /**
     * fetches a Comment by id
     *
     * [fullName]: t1_ieoxp6v
     * [subredditNamePrefixed]: r/webdev
     * [oauthToken]: 1234585724870-velC4Oe6RURtyhmUIPrCWER-o6Y_rF
     */
    fun getCommentById(fullName: String, subredditNamePrefixed: String, oauthToken: String): Thing1 {
        val apiPath = "${subredditNamePrefixed}/api/info?id=${fullName}"
        val res = get(apiPath, oauthToken)

        return parseThing1ListingResponse(JSONObject(res.body!!.source().readUtf8())).first()
    }


    /**
     * Cast a vote on a thing.
     * ref: https://www.reddit.com/dev/api/#POST_api_vote
     *
     * [fullName]: t3_vqfm29
     * [dir]: 1
     * [oauthToken]: 1234585724870-velC4Oe6RURtyhmUIPrCWER-o6Y_rF
     */
    fun castVote(fullName: String, dir: Int, oauthToken: String): Response {
        val apiPath = "api/vote"
        val builder = FormBody.Builder()

        builder.add("id", fullName)
        builder.add("dir", dir.toString())

        val formBody = builder.build()

        return post(apiPath, oauthToken, formBody)
    }

    /**
     * Add a selftext or Link submission to the subreddit.
     */
    fun submit(form: SubmissionValidatorHelper.SubmissionForm, oauthToken: String): Response {
        var url = "api/submit?resubmit=true&raw_json=1&gilding_detail=1"
        val builder = FormBody.Builder()

        builder.add("kind", form.kind)
        builder.add("sr", form.sr)
        builder.add("api_type", form.api_type)
        builder.add("title", form.title)
        builder.add("flair_id", form.flair_id)
        builder.add("flair_text", form.flair_text)
        builder.add("sendreplies", form.sendreplies.toString())
        builder.add("show_error_list", true.toString())
        builder.add("validate_on_submit", true.toString())
        builder.add("nsfw", form.nsfw.toString())
        builder.add("spoiler", form.spoiler.toString())

        when(form.kind) {
            SubmissionKind.Link.kind -> {
                builder.add("url", form.url)
            }
            SubmissionKind.Self.kind -> {
                builder.add("text", form.text)
            }
            SubmissionKind.Image.kind -> {
                builder.add("url", form.url)

                if (form.images.size > 1) { // is a Gallery submission
                    url = "api/submit_gallery_post.json?resubmit=true&raw_json=1&gilding_detail=1"

                    val payload = JSONObject()

                    payload.put("sr", form.sr)
                    payload.put("api_type", form.api_type)
                    payload.put("title", form.title)
                    payload.put("flair_id", form.flair_id)
                    payload.put("flair_text", form.flair_text)
                    payload.put("sendreplies", form.sendreplies.toString())
                    payload.put("show_error_list", true.toString())
                    payload.put("validate_on_submit", true.toString())
                    payload.put("nsfw", form.nsfw.toString())
                    payload.put("spoiler", form.spoiler.toString())
                    payload.put("kind", "self")
                    payload.put("submit_type", "subreddit")
                    payload.put("items", form.items)

                    val mediaType = String.format("application/json; charset=utf-8").toMediaType()
                    return post(url, oauthToken, payload.toString().toRequestBody(mediaType))
                }
            }
            SubmissionKind.Poll.kind -> {
                url = "api/submit_poll_post?resubmit=true&raw_json=1&gilding_detail=1"

                val payload = JSONObject()

                payload.put("sr", form.sr)
                payload.put("api_type", form.api_type)
                payload.put("title", form.title)
                payload.put("text", form.text)
                payload.put("flair_id", form.flair_id)
                payload.put("flair_text", form.flair_text)
                payload.put("sendreplies", form.sendreplies.toString())
                payload.put("show_error_list", true.toString())
                payload.put("validate_on_submit", true.toString())
                payload.put("nsfw", form.nsfw.toString())
                payload.put("spoiler", form.spoiler.toString())
                payload.put("kind", "self")
                payload.put("submit_type", "subreddit")
                payload.put("duration", form.duration)
                payload.put("options", form.options)


                val mediaType = String.format("application/json; charset=utf-8").toMediaType()
                return post(url, oauthToken, payload.toString().toRequestBody(mediaType))
            }
            SubmissionKind.Video.kind -> {
                builder.add("url", form.url)
                builder.add("video_poster_url", form.video_poster_url)
            }
            else -> {
            /**
             * TODO - throw custom Exception
             */
                Timber.w("Submission Not Implemented!")
            }
        }

        val formBody = builder.build()

        return post(url, oauthToken, formBody)
    }

    /**
     * uploads a media file to Reddit and returns the url link
     */
   fun uploadMedia(uri: Uri, oauthToken: String): Pair<String, String> {
        var mediaUrl = ""
        var mediaId = ""
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
            Timber.d("mediaAssetUploadResponse: $mediaAssetUploadResponse")

            val requestBodyBuilder = MultipartBody.Builder().setType(MultipartBody.FORM)
            var uploadFileKey = ""

            Timber.d("mediaAssetUploadResponse: $mediaAssetUploadResponse")
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
                uploadResponse.isSuccessful -> {
                    mediaUrl = "${uploadUrl}/${uploadFileKey}"
                    mediaAssetUploadResponse?.asset?.asset_id?.let { id ->  mediaId = id}

                    Timber.d("mediaUrl: $mediaUrl")
                    Timber.d("mediaId: $mediaId")
                }
                else -> throw HttpStatusException(uploadResponse.message, uploadResponse.code, uploadResponse.request.url.toString())
            }

        }
        return Pair(mediaId, mediaUrl)
    }

    /**
     * just like uploadMedia(Uri, String) but strictly for uploading .png images
     */
    fun uploadMedia(file: File, mimeType: String, oauthToken: String): Pair<String, String> {
            var mediaUrl = ""
            var mediaId = ""
            val formBody = FormBody.Builder()
                .add("filepath", file.name)
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
            Timber.d("mediaAssetUploadResponse: $mediaAssetUploadResponse")

            val requestBodyBuilder = MultipartBody.Builder().setType(MultipartBody.FORM)
            var uploadFileKey = ""

            Timber.d("mediaAssetUploadResponse: $mediaAssetUploadResponse")
            mediaAssetUploadResponse?.args?.fields?.onEach { pair ->
                if(pair.name == "key") uploadFileKey = pair.value
                requestBodyBuilder.addFormDataPart(pair.name, pair.value)
            }.also {_ ->
                val contentPart = InputStreamRequestBody(mimeType.toMediaType(), file.length(), context.contentResolver, file.toUri())
                requestBodyBuilder.addFormDataPart("file", file.name, contentPart)
            }

            val requestBody = requestBodyBuilder.build()
            val uploadUrl = "https:${mediaAssetUploadResponse?.args?.action}"
            val request = Request.Builder()
                .url(uploadUrl)
                .post(requestBody)
                .build()

            val uploadResponse = client.newCall(request).execute()

            when {
                uploadResponse.isSuccessful -> {
                    mediaUrl = "${uploadUrl}/${uploadFileKey}"
                    mediaAssetUploadResponse?.asset?.asset_id?.let { id ->  mediaId = id}

                    Timber.d("mediaUrl: $mediaUrl")
                    Timber.d("mediaId: $mediaId")
                }
                else -> throw HttpStatusException(uploadResponse.message, uploadResponse.code, uploadResponse.request.url.toString())
            }
        return Pair(mediaId, mediaUrl)
    }
}
