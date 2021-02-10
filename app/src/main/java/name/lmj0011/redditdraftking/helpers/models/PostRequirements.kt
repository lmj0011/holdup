package name.lmj0011.redditdraftking.helpers.models

import android.os.Parcelable
import androidx.annotation.Keep
import kotlinx.parcelize.Parcelize


/**
 * ref: https://www.reddit.com/dev/api#GET_api_v1_{subreddit}_post_requirements
 */
@Keep
@Parcelize
data class PostRequirements (
    val title_regexes: List<String>,
    val body_blacklisted_strings: List<String>,
    val title_blacklisted_strings: List<String>,
    val body_text_max_length: String?,
    val title_required_strings: List<String>,
    val guidelines_text: String?,
    val gallery_min_items: Int?,
    val domain_blacklist: List<String>,
    val domain_whitelist: List<String>,
    val title_text_max_length: Int?,

    /**
     * String. One of "required", "notAllowed", or "none",
     * meaning that a self-post body is required, not allowed, or optional, respectively.
     */
    val body_restriction_policy: String?,
    /*****/

    val link_restriction_policy: String?,
    val guidelines_display_policy: String?,
    val body_required_strings: List<String>,
    val title_text_min_length: Int?,
    val gallery_captions_requirement: String,
    val is_flair_required: Boolean,
    val gallery_max_items: Int?,
    val gallery_urls_requirement: String,
    val body_regexes: List<String>,
    val link_repost_age: Int?,
    val body_text_min_length: Int?

) : Parcelable