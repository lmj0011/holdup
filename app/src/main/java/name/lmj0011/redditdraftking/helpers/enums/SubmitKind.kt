package name.lmj0011.redditdraftking.helpers.enums

enum class SubmitKind(val kind: String) {
    Link("link"),
    Self("self"),
    Image("image"),
    Video("video"),
    VideoGif("videogif");

    // :prayer_hands: https://stackoverflow.com/a/34625163/2445763
    companion object {
        fun from(findKind: String): SubmitKind = values().first { it.kind == findKind }
    }
}