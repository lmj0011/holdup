package name.lmj0011.redditdraftking.database

import androidx.room.TypeConverter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import name.lmj0011.redditdraftking.database.models.Account
import name.lmj0011.redditdraftking.helpers.enums.SubmissionKind
import name.lmj0011.redditdraftking.helpers.models.Subreddit
import name.lmj0011.redditdraftking.helpers.models.Image
import name.lmj0011.redditdraftking.helpers.models.SubredditFlair
import name.lmj0011.redditdraftking.helpers.models.Video

class DataConverters {
    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val subredditJsonAdapter = moshi.adapter(Subreddit::class.java)

    private val accountJsonAdapter = moshi.adapter(Account::class.java)

    private val subredditFlairJsonAdapter = moshi.adapter(SubredditFlair::class.java)

    private val imgGalleryJsonAdapter = moshi.adapter<MutableList<Image>>(
        Types.newParameterizedType(MutableList::class.java, Image::class.java)
    )
    private val videoJsonAdapter = moshi.adapter(Video::class.java)

    private val mutableListOfStringsJsonAdapter = moshi.adapter<MutableList<String>>(
        Types.newParameterizedType(MutableList::class.java, String::class.java)
    )

    private val submissionKindJsonAdapter = moshi.adapter(SubmissionKind::class.java)

    @TypeConverter
    fun fromSubreddit(value: Subreddit?): String {
        return subredditJsonAdapter.toJson(value)
    }

    @TypeConverter
    fun toSubreddit(value: String): Subreddit? {
        return subredditJsonAdapter.fromJson(value)
    }

    @TypeConverter
    fun fromAccount(value: Account?): String {
        return accountJsonAdapter.toJson(value)
    }

    @TypeConverter
    fun toAccount(value: String): Account? {
        return accountJsonAdapter.fromJson(value)
    }

    @TypeConverter
    fun fromSubredditFlair(value: SubredditFlair?): String {
        return subredditFlairJsonAdapter.toJson(value)
    }

    @TypeConverter
    fun toSubredditFlair(value: String): SubredditFlair? {
        return subredditFlairJsonAdapter.fromJson(value)
    }

    @TypeConverter
    fun fromImgGallery(value: MutableList<Image>): String {
        return imgGalleryJsonAdapter.toJson(value)
    }

    @TypeConverter
    fun toImgGallery(value: String): MutableList<Image>? {
        return imgGalleryJsonAdapter.fromJson(value)
    }

    @TypeConverter
    fun fromVideo(value: Video?): String {
        return videoJsonAdapter.toJson(value)
    }

    @TypeConverter
    fun toVideo(value: String): Video? {
        return videoJsonAdapter.fromJson(value)

    }

    @TypeConverter
    fun fromMutableStringList(value: MutableList<String>): String {
        return mutableListOfStringsJsonAdapter.toJson(value)
    }

    @TypeConverter
    fun toMutableStringList(value: String): MutableList<String>? {
        return mutableListOfStringsJsonAdapter.fromJson(value)

    }

    @TypeConverter
    fun fromSubmissionKind(value: SubmissionKind?): String {
        return submissionKindJsonAdapter.toJson(value)
    }

    @TypeConverter
    fun toSubmissionKind(value: String): SubmissionKind? {
        return submissionKindJsonAdapter.fromJson(value)
    }
}