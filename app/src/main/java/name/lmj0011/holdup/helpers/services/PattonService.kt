package name.lmj0011.holdup.helpers.services

import android.app.PendingIntent
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.*
import io.socket.client.Ack
import io.socket.client.Socket
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import name.lmj0011.holdup.App
import name.lmj0011.holdup.MainActivity
import name.lmj0011.holdup.R
import name.lmj0011.holdup.database.models.Account
import name.lmj0011.holdup.helpers.NotificationHelper
import name.lmj0011.holdup.helpers.PattonConnectivityHelper
import name.lmj0011.holdup.helpers.RedditApiHelper
import name.lmj0011.holdup.helpers.RedditAuthHelper
import name.lmj0011.holdup.helpers.models.Thing1
import name.lmj0011.holdup.helpers.models.Thing3
import name.lmj0011.holdup.helpers.util.launchIO
import okhttp3.Response
import org.json.JSONException
import org.json.JSONObject
import org.kodein.di.instance
import timber.log.Timber
import java.util.*

/**
 * A Bounded Service used to keep one ongoing websocket connection
 * ref: https://developer.android.com/guide/components/bound-services
 */
class PattonService : LifecycleService() {

    companion object {
        const val ACTION_NAV_TO_PATTON_SERVICE = "name.lmj0011.holdup.helpers.services.PattonService.ACTION_NAV_TO_PATTON_SERVICE"
        const val ACTION_STOP_SERVICE = "name.lmj0011.holdup.helpers.services.PattonService.ACTION_STOP_SERVICE"
        var hasActiveSocketConnection = false
         private set
    }

    inner class PattonServiceBinder : Binder() {
        // Return this instance of LocalService so clients can call public methods
        fun getService(): PattonService = this@PattonService
    }

    private val binder = PattonServiceBinder()
    private lateinit var pattonConnectivityHelper: PattonConnectivityHelper
    private lateinit var redditAuthHelper: RedditAuthHelper
    private lateinit var redditApiHelper: RedditApiHelper
    private lateinit var socket: Socket
    lateinit var foregroundNotificationBuilder: NotificationCompat.Builder
        private set

    private lateinit var thingsUpvotedJob: Job
    private lateinit var hasActiveSocketConnectionJob: Job
    private var _messages = MutableSharedFlow<String>(replay = 200)
    private var _usersOnline = MutableStateFlow(0)
    private var _thingsUpvoted = MutableStateFlow(0)
    private var _socketConnected = MutableStateFlow(false)
    private val pendingIntentFlags = PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_CANCEL_CURRENT

    /**
     * methods/properties available to bounded clients
     */
    val account = MutableStateFlow<Account?>(null)

    val socketConnected: SharedFlow<Boolean>
        get() = _socketConnected

    val usersOnline: SharedFlow<Int>
        get() = _usersOnline

    val thingsUpvoted: SharedFlow<Int>
        get() = _thingsUpvoted

    val messages: SharedFlow<String>
        get() = _messages
    /***/

    override fun onCreate() {
        super.onCreate()
        pattonConnectivityHelper = (this.applicationContext as App).kodein.instance()
        redditAuthHelper = (this.applicationContext as App).kodein.instance()
        redditApiHelper = (this.applicationContext as App).kodein.instance()
        socket = pattonConnectivityHelper.socket

        val contentPendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).apply
            {
                action = ACTION_NAV_TO_PATTON_SERVICE
                flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
            },
            pendingIntentFlags
        )

        val closeActionPendingIntent = PendingIntent.getForegroundService(
            this,
            0,
            Intent(this, PattonService::class.java).apply
            {
                action = ACTION_STOP_SERVICE
            },
            pendingIntentFlags
        )

        foregroundNotificationBuilder = NotificationCompat.Builder(this, NotificationHelper.PATTON_SERVICE_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_app_notification_icon)
            .setShowWhen(false)
            .setContentTitle("Patton Service")
            .setContentText("Upvoted: ${_thingsUpvoted.value}")
            .addAction(0, "Disconnect", closeActionPendingIntent)
            .setContentIntent(contentPendingIntent)
    }

    /**
     * starts this foreground service
     */
    fun start(acct: Account) {
        account.tryEmit(acct)

        socket.off() // remove all previous listeners, if any.
        setUpSocketEventListeners()
        socket.connect()

        val notification = foregroundNotificationBuilder.build()
        startForeground(NotificationHelper.PATTON_SERVICE_NOTIFICATION_ID, notification)

        thingsUpvotedJob = launchIO {
            thingsUpvoted.collect { cnt ->
                val notif = foregroundNotificationBuilder
                    .setContentText("Upvoted: $cnt")
                    .build()

                NotificationManagerCompat.from(application)
                    .notify(NotificationHelper.PATTON_SERVICE_NOTIFICATION_ID, notif)
            }
        }

        hasActiveSocketConnectionJob = launchIO {
            socketConnected.collect { connected ->
                hasActiveSocketConnection = connected
            }
        }


        Timber.d("start called")
    }

    /**
     * stops this foreground service
     */
    fun stop() {
        socket.disconnect()

        stopForeground(true)
        stopSelf()

        launchIO {
            delay(2000)
            NotificationManagerCompat.from(this@PattonService).apply {
                cancel(NotificationHelper.PATTON_SERVICE_NOTIFICATION_ID)
            }
        }

        Timber.d("stop called")
    }

    /**
     * Event Listeners for this Service
     */
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when(intent?.action) {
            ACTION_STOP_SERVICE -> {
                stop()
            }
            else -> {
                Timber.e("No matching intent.action")
            }
        }

        return super.onStartCommand(intent, flags, startId)
    }

    override fun onDestroy() {
        super.onDestroy()
        socket.off() // calling here so Socket.EVENT_DISCONNECT has time to trigger Flow.emit
        thingsUpvotedJob.cancel()
        hasActiveSocketConnectionJob.cancel()
        Timber.d("onDestroy called")
    }

    override fun onBind(intent: Intent): IBinder {
        super.onBind(intent)
        Timber.d("onBind called")
        return binder
    }
    /** */


    /**
     * reddit API functions
     */
    private fun getPostById(fullName: String): Thing3 {
        return redditApiHelper.getPostById(
            fullName,
            redditAuthHelper.authClient(account.value!!).getSavedBearer().getAccessToken()!!
        )
    }

    private fun getCommentById(fullName: String, subredditNamePrefixed: String): Thing1 {
        return redditApiHelper.getCommentById(
            fullName,
            subredditNamePrefixed,
            redditAuthHelper.authClient(account.value!!).getSavedBearer().getAccessToken()!!
        )
    }

    private fun upvoteThing(fullName: String): Response {
        return redditApiHelper.castVote(
            fullName,
            1,
            redditAuthHelper.authClient(account.value!!).getSavedBearer().getAccessToken()!!
        )
    }
    /** */

    /**
     * websocket Event Listeners
     */
    private fun setUpSocketEventListeners() {
        socket.on(Socket.EVENT_CONNECT) {
            _messages.tryEmit("Successfully connected!")
            _socketConnected.tryEmit(socket.connected())
        }

        socket.on(Socket.EVENT_DISCONNECT) {
            _messages.tryEmit("Disconnected..")
            _usersOnline.tryEmit(0)
            _thingsUpvoted.tryEmit(0)
            _socketConnected.tryEmit(socket.connected())
        }

        socket.on(Socket.EVENT_CONNECT_ERROR) {
            _messages.tryEmit(Socket.EVENT_CONNECT_ERROR)
        }

        socket.on("ackConnection") { args ->
            (args[0] as? String)?.let { _messages.tryEmit(it) }

            launchIO {
                if (args.size > 1 && args[1] is Ack) {
                    (args[1] as Ack).call("acknowledged connection")
                }
            }
        }

        socket.on("upvoteThisThing") { args ->
            val ack = if (args.size > 1 && args[1] is Ack) { (args[1] as Ack) } else null

            launchIO {
                try {
                    /**
                     *
                     * the JSON structure of [data]
                     *
                     * { id: "vqfm29", kind: "t3", name: "t3_vqfm29", subredditNamePrefixed: "r/MadeMeSmile" }
                     */
                    val data = JSONObject(args[0].toString())
                    Timber.d("data: $data")

                    when (data.getString("kind")) {
                        "t1" -> { // this is a Comment
                            val thingFullName = data.getString("name")
                            val subredditNamePrefixed = data.getString("subredditNamePrefixed")
                            val comment = getCommentById(thingFullName, subredditNamePrefixed)

                            // check if user has already upvote Thing
                            if(!comment.likes) {
                                try{
                                    upvoteThing(thingFullName)

                                    val msg = "Successfully upvoted $thingFullName"
                                    _messages.tryEmit(msg)
                                    _thingsUpvoted.tryEmit(1 + _thingsUpvoted.value)

                                    ack?.call(msg)

                                } catch (ex: org.jsoup.HttpStatusException) {
                                    val msg = "HTTP Error ${ex.statusCode}; POST api/vote; $thingFullName"
                                    _messages.tryEmit(msg)
                                    ack?.call(msg)
                                }
                            } else {
                                val msg = "Already upvoted $thingFullName"
                                _messages.tryEmit(msg)
                                ack?.call(msg)
                            }

                        }
                        "t3" -> { // This is a Post
                            val thingFullName = data.getString("name")
                            val post = getPostById(thingFullName)

                            // check if user has already upvote Thing
                            if(!post.likes) {
                                try{
                                    upvoteThing(thingFullName)

                                    val msg = "Successfully upvoted $thingFullName"
                                    _messages.tryEmit(msg)
                                    _thingsUpvoted.tryEmit(1 + _thingsUpvoted.value)

                                    ack?.call(msg)

                                } catch (ex: org.jsoup.HttpStatusException) {
                                    val msg = "HTTP Error ${ex.statusCode}; POST api/vote; $thingFullName"
                                    _messages.tryEmit(msg)
                                    ack?.call(msg)
                                }
                            } else {
                                val msg = "Already upvoted $thingFullName"
                                _messages.tryEmit(msg)
                                ack?.call(msg)
                            }
                        }
                        else -> {
                            // ref: https://www.reddit.com/dev/api/#fullnames
                            val msg = "Cannot upvote a ${data.getString("kind")} Thing"
                            Timber.e(msg)
                            ack?.call(msg)
                        }
                    }
                } catch (ex: JSONException) {
                    ex.message?.let { _messages.tryEmit(it) }
                    Timber.e(ex)
                }
            }
        }

        socket.on("numberOfClientsOnline") { args ->

            try {
                /**
                 *
                 * the JSON structure of [data]
                 *
                 * { numOnline: 37 }
                 */
                val data = JSONObject(args[0].toString())
                _usersOnline.tryEmit(data.getInt("numOnline"))
            } catch(ex: JSONException) {
                ex.message?.let { _messages.tryEmit(it) }
                Timber.e(ex)
            }
        }

        socket.on("log") { args ->
            (args[0] as? String)?.let { _messages.tryEmit(it) }
        }
    }
    /** */

    /**
     * Events to send to the server
     */

    fun emitUpvoteSubmission(payload: JSONObject) {
        socket.emit("upvoteSubmission", payload)
    }

    /** */


}