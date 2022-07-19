package name.lmj0011.holdup.helpers

import android.content.Context
import com.github.javafaker.Faker
import name.lmj0011.holdup.App
import name.lmj0011.holdup.database.models.Account
import name.lmj0011.holdup.database.models.Submission
import name.lmj0011.holdup.helpers.enums.SubmissionKind
import name.lmj0011.holdup.helpers.models.Subreddit
import name.lmj0011.holdup.helpers.models.SubredditFlair
import org.kodein.di.instance

class TestHelper(val context: Context) {
    private val faker = Faker()
    private val requestCodeHelper: UniqueRuntimeNumberHelper = (context.applicationContext as App).kodein.instance()

    /**
     * Takes a list of SubmissionKind to utcMillis pairs and
     * returns a list of Submissions.
     *
     * utsMillis is the Submission.postAtMillis
     *
     */
    suspend fun generateSubmissions(listOfPairs: List<Pair<SubmissionKind, Long>>): MutableList<Submission> {
        val submissions = mutableListOf<Submission>()

        val account = Account(
            id=7,
            createdAt="2021-08-17T01:31:08.882Z",
            updatedAt="2021-08-17T01:31:09.332Z",
            name="u/0x69733",
            iconImage="https://www.redditstatic.com/avatars/avatar_default_08_D4E815.png"
         )

        val subreddit = Subreddit(
            guid="t5_357j7j",
            displayName="DraftKingTesting",
            displayNamePrefixed="r/DraftKingTesting",
            iconImgUrl="https://styles.redditmedia.com/t5_357j7j/styles/communityIcon_2kz8l0ekx5o51.png",
            subscribers=3,
            allowGalleries=true,
            allowImages=true,
            allowVideos=true,
            allowVideoGifs=true,
            allowPolls=true,
            linkFlairEnabled=true,
            over18 = false
        )

        val listOfFlairs = listOf(
            SubredditFlair(
                id="270abb2e-2f9b-11eb-a46f-0e596405ba0f",
                text="test1",
                textColor="dark",
                backGroundColor="#ea0027"
            ),
            SubredditFlair(
                id="2bb3ea2e-2f9b-11eb-8d62-0e528cea0d1b",
                text="test2",
                textColor="dark",
                backGroundColor="#ff4500"
            ),
            SubredditFlair(
                id="33c75688-2f9b-11eb-82ea-0ee65bd9a375",
                text="test3",
                textColor="dark",
                backGroundColor="#ffb000"
            )
        )

        listOfPairs. forEach { pair ->
            when(pair.first) {
                SubmissionKind.Self -> {
                    val sub = Submission(
                        postAtMillis = pair.second,
                        title = faker.zelda().character(),
                        kind = pair.first,
                        isNsfw = false,
                        isSpoiler = false,
                        alarmRequestCode = requestCodeHelper.nextInt(),
                        subredditFlair = listOfFlairs.random(),
                        body = faker.shakespeare().hamletQuote(),
                        url = "",
                        subreddit = subreddit,
                        account = account
                    )

                    submissions.add(sub)
                }
                else -> {}
            }
        }

        return submissions
    }
}