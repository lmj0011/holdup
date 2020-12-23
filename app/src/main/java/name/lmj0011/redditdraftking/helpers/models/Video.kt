package name.lmj0011.redditdraftking.helpers.models

data class Video (
    val sourceUri: String, // uri to this video file (on device location)
    val mediaId: String, // the id of this video
    var url: String, // raw url to this video after it has been uploaded to Reddit

    /**
     * raw url to image used as this video's thumbnail
     *
     * TODO - change default posterUrl, currently is a solid black image, would ideally be the
     * logo of this app in the future
     */
    var posterUrl: String = "https://reddit-uploaded-media.s3-accelerate.amazonaws.com/rte_images/6kc2nljukn661",
)