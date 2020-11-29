package name.lmj0011.redditdraftking.helpers.enums

enum class SubmissionKind(val kind: String) {
    Link("link"),
    Self("self"),
    Image("image"),
    Video("video"),
    Poll("poll"),
    VideoGif("videogif");

    // :prayer_hands: https://stackoverflow.com/a/34625163/2445763
    companion object {
        fun from(findKind: String): SubmissionKind = values().first { it.kind == findKind }
    }
}