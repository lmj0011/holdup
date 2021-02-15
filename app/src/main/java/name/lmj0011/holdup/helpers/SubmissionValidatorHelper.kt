package name.lmj0011.holdup.helpers

import android.content.Context
import android.util.Patterns
import name.lmj0011.holdup.helpers.enums.SubmissionKind
import name.lmj0011.holdup.helpers.models.Image
import name.lmj0011.holdup.helpers.models.PostRequirements
import org.json.JSONArray
import timber.log.Timber

class SubmissionValidatorHelper(val context: Context) {
    companion object {
        const val MAX_TITLE_LENGTH = 300
        const val MAX_FLAIR_ID_LENGTH = 36
        const val MAX_FLAIR_TEXT_LENGTH = 64
        const val MAX_IMAGE_CAPTION_LENGTH = 180
        const val MIN_GALLERY_LENGTH = 1
        const val MIN_POLL_OPTIONS = 2
        const val MAX_POLL_OPTIONS = 6
        const val MIN_POLL_DURATION = 1 // days
        const val MAX_POLL_DURATION = 7
    }

    /**
     * NOTE: this data class does not correspond 1:1 to the "api/submit" json payload
     * some property value(s) are exclusive to SubmissionViewModel (ie. [images])
     */
    data class SubmissionForm (
        var api_type: String = "json",
        var extension: String = "json",
        var flair_id: String = "",
        var flair_text: String = "",
        var kind: String = "",
        var nsfw: Boolean = false,
        var resubmit: Boolean = true,
        var sendreplies: Boolean = true,
        var spoiler: Boolean = false,
        var sr: String = "",
        var text: String = "",
        var title: String = "",
        var url: String = "",
        var video_poster_url: String = "",
        var images: List<Image> = listOf(),
        var submit_type: String = "subreddit",
        var items: JSONArray = JSONArray(),
        var pollOptions: List<String> = listOf(),
        var options: JSONArray = JSONArray(),
        var duration: Int = 3
    )

    /** TODO - return error message when validation is false
     *  basically return something like Pair<validated: Boolean, message: String>
     */
    fun validate(form: SubmissionForm, reqs: PostRequirements): Boolean {
        Timber.d("PostRequirements for r/${form.sr}: $reqs")

        // title validation
        if(
            form.title.isEmpty() ||
            reqs.title_text_min_length != null &&
            form.title.length < reqs.title_text_min_length
        ) return false

        if(
            reqs.title_text_max_length != null &&
            form.title.length > reqs.title_text_max_length ||
            form.title.length > MAX_TITLE_LENGTH
        ) return false

        // post flair validation
        if(
            reqs.is_flair_required &&
            form.flair_id.isNotBlank() &&
            form.flair_id.length > MAX_FLAIR_ID_LENGTH
        ) return false

        if(
            reqs.is_flair_required &&
            form.flair_text.isNotBlank() &&
            form.flair_text.length > MAX_FLAIR_TEXT_LENGTH
        ) return false


        // handle specific validation here
        return when(form.kind) {
            SubmissionKind.Link.kind -> {
                linkValidate(form, reqs)
            }
            SubmissionKind.Self.kind -> {
                selfValidate(form, reqs)
            }
            SubmissionKind.Image.kind -> {
                imageValidate(form, reqs)
            }
            SubmissionKind.Video.kind -> {
                videoValidate(form, reqs)
            }
            SubmissionKind.Poll.kind -> {
                pollValidate(form, reqs)
            }
            SubmissionKind.VideoGif.kind -> {
                true
            }
            else -> false
        }
    }

    private fun selfTextValidate(form: SubmissionForm, reqs: PostRequirements): Boolean {
        // TODO - needs completing
        return true
    }

    private fun linkValidate(form: SubmissionForm, reqs: PostRequirements): Boolean {
        return Patterns.WEB_URL.matcher(form.url).matches()
    }

    private fun selfValidate(form: SubmissionForm, reqs: PostRequirements): Boolean {
        // TODO - needs completing
        return true
    }

    private fun imageValidate(form: SubmissionForm, reqs: PostRequirements): Boolean {
        val reqMinItems = reqs.gallery_min_items
        val reqMaxItems = reqs.gallery_max_items

        return when {
            form.images.size < MIN_GALLERY_LENGTH -> false
            reqMinItems != null && form.images.size < reqMinItems -> false
            reqMaxItems != null && form.images.size > reqMaxItems -> false
            else -> true
        }
    }

    private fun pollValidate(form: SubmissionForm, reqs: PostRequirements): Boolean {
        return when {
            form.pollOptions.size < MIN_POLL_OPTIONS -> false
            form.pollOptions.size > MAX_POLL_OPTIONS -> false
            form.duration < MIN_POLL_DURATION -> false
            form.duration > MAX_POLL_DURATION -> false
            else -> true
        }
    }

    private fun videoValidate(form: SubmissionForm, reqs: PostRequirements): Boolean {
        return when {
            form.url.isBlank() || !Patterns.WEB_URL.matcher(form.url).matches() -> false
            form.video_poster_url.isNotBlank() && !Patterns.WEB_URL.matcher(form.video_poster_url).matches() -> false
            else -> true
        }
    }
}