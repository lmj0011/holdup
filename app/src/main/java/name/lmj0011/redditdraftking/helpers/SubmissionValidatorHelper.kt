package name.lmj0011.redditdraftking.helpers

import android.content.Context
import android.util.Patterns
import name.lmj0011.redditdraftking.helpers.enums.SubmissionKind
import name.lmj0011.redditdraftking.helpers.models.PostRequirements
import timber.log.Timber

class SubmissionValidatorHelper(val context: Context) {
    companion object {
        const val MAX_TITLE_LENGTH = 300
        const val MAX_FLAIR_ID_LENGTH = 36
        const val MAX_FLAIR_TEXT_LENGTH = 64
    }

    data class SubmissionForm (
        var api_type: String = "json",
        var extension: String = "json",
        var flair_id: String = "",
        var flair_text: String = "",
        var kind: String = "",
        var nsfw: Boolean = false,
        var resubmit: Boolean = true,
        var spoiler: Boolean = false,
        var sr: String = "",
        var text: String = "",
        var title: String = "",
        var url: String = "",
        var video_poster_url: String = "",
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
                true
            }
            SubmissionKind.Video.kind -> {
                true
            }
            SubmissionKind.Poll.kind -> {
                true
            }
            SubmissionKind.VideoGif.kind -> {
                true
            }
            else -> false
        }
    }

    private fun linkValidate(form: SubmissionForm, reqs: PostRequirements): Boolean {
        return Patterns.WEB_URL.matcher(form.url).matches()
    }

    private fun selfValidate(form: SubmissionForm, reqs: PostRequirements): Boolean {
        // TODO - needs completing
        return true
    }
}