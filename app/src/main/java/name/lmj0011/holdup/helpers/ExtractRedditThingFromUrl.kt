package name.lmj0011.holdup.helpers

import android.os.Parcelable
import androidx.annotation.Keep
import kotlinx.parcelize.Parcelize
import timber.log.Timber
import java.net.URL

/**
 * Helper class ported from src/RedditThing.ts
 * ref: https://github.com/lmj0011/patton-server
 */
object ExtractRedditThingFromUrl {
    abstract class ExtractedRedditThing {
        abstract val id: String
        abstract val url: URL
        abstract val kind: String
        abstract val fullName: String
        abstract val subredditNamePrefixed: String
    }

    @Keep
    @Parcelize
    data class Post (
        override val id: String,
        override val url: URL,
        override val kind: String = "t3",
        override val fullName: String,
        override val subredditNamePrefixed: String
    ) : ExtractedRedditThing(), Parcelable

    @Keep
    @Parcelize
    data class Comment(
        override val id: String,
        override val url: URL,
        override val kind: String = "t1",
        override val fullName: String,
        val fullNameOfParent: String,
        override val subredditNamePrefixed: String
    ) : ExtractedRedditThing(), Parcelable

    /**
     * Returns an object representing a Reddit Post or Comment
     */
    fun extract(url: String): ExtractedRedditThing {
        val redditUrl = URL(url)
        var id = ""
        var kind = ""
        var fullName = ""
        var fullNameOfParent = ""
        var subredditNamePrefixed = ""

        if (listOf("www.reddit.com", "reddit.com").contains(redditUrl.host).not()) {
            throw Error("Unaccepted url: $url")
        }

        val pathList = redditUrl.path.split("/").toMutableList()
        val pathListCount = pathList.size

        Timber.d(redditUrl.path)

        // we'll assume the share url is from the Reddit official mobile app
        if(pathListCount == 7) {
            while (pathList.isNotEmpty()) {
                val ele = pathList.removeFirst()

                /**
                 * example payload for this case:
                 *
                 * pathArray: [ '', 'r', 'dankvideos', 'comments', 'vnz6e0', 'time', '' ]
                 * thing.url: https://www.reddit.com/r/dankvideos/comments/vnz6e0/time/
                 */
                if(pathList.size > 1 && ele == "r") {
                    // next element is the subreddit name
                    val ele = pathList.removeFirst()
                    subredditNamePrefixed = "r/$ele"
                }

                /**
                 * example payload for this case:
                 *
                 * pathArray: [ '', 'r', 'dankvideos', 'comments', 'vnz6e0', 'time', '' ]
                 * thing.url: https://www.reddit.com/r/dankvideos/comments/vnz6e0/time/
                 */
                if(ele == "comments") {
                    //next element is the Thing id for Post
                    id = pathList.removeFirst()
                    kind = "t3"
                    fullName = "t3_$id"
                    fullNameOfParent = "t3_$id"
                }

                /**
                 * example payload for this case:
                 *
                 * pathArray: [ '', 'r', 'dankvideos', 'comments', 'vnz6e0', 'time', 'ieagig6' ]
                 * thing.url: https://www.reddit.com/r/dankvideos/comments/vnz6e0/time/ieagig6
                 */
                // we're at the last element
                if(pathList.size == 1 && pathList[0].isNotBlank()){
                    //next element is thing id for Comment
                    id = pathList.removeFirst()
                    kind = "t1"
                    fullName = "t1_$id"
                }
            }
        } else { // we'll assume the share url is from the Reddit web app
            while (pathList.isNotEmpty()) {
                val ele = pathList.removeFirst()

                /**
                 * example payload for this case:
                 *
                 * pathArray: [ '', 'r', 'privacy', 'comments', 'om9h4p', 'piped_the_privacyfriendly_youtube', '' ]
                 * thing.url: https://www.reddit.com/r/privacy/comments/om9h4p/piped_the_privacyfriendly_youtube/
                 */
                if(pathList.size > 1 && ele == "r") {
                    // next element is the subreddit name
                    val ele = pathList.removeFirst()
                    subredditNamePrefixed = "r/$ele"
                }

                /**
                 * example payload for this case:
                 *
                 * pathArray: [ '', 'r', 'privacy', 'comments', 'om9h4p', 'piped_the_privacyfriendly_youtube', '' ]
                 * thing.url: https://www.reddit.com/r/privacy/comments/om9h4p/piped_the_privacyfriendly_youtube/
                 */
                if(ele == "comments") {
                    //next element is thing id for Post
                    id = pathList.removeFirst()
                    kind = "t3"
                    fullName = "t3_$id"
                    fullNameOfParent = "t3_$id"
                }

                /**
                 * example payload for this case:
                 *
                 * pathArray: [ '', 'r', 'privacy', 'comments', 'om9h4p', 'comment', 'h5kk2mh', '' ]
                 * thing.url: https://www.reddit.com/r/privacy/comments/om9h4p/comment/h5kk2mh/
                 */
                if(ele == "comment") {
                    //next element is thing id for Comment
                    id = pathList.removeFirst()
                    kind = "t1"
                    fullName = "t1_$id"
                }
            }
        }

        return when(kind) {
            "t1" -> Comment(id = id, url = redditUrl, fullName = fullName, fullNameOfParent = fullNameOfParent, subredditNamePrefixed = subredditNamePrefixed)
            "t3" -> Post(id = id, url = redditUrl, fullName = fullName, subredditNamePrefixed = subredditNamePrefixed)
            else -> throw Error("invalid kind of Thing")
        }
    }
}