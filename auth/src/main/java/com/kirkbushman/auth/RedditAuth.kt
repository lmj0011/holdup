package com.kirkbushman.auth

import com.kirkbushman.auth.errors.AccessDeniedException
import com.kirkbushman.auth.errors.InvalidRequestException
import com.kirkbushman.auth.errors.InvalidScopesException
import com.kirkbushman.auth.errors.OAuth2Exception
import com.kirkbushman.auth.errors.RefreshTokenMissingException
import com.kirkbushman.auth.errors.UnsupportedResponseTypeException
import com.kirkbushman.auth.http.RedditService
import com.kirkbushman.auth.managers.StorageManager
import com.kirkbushman.auth.models.creds.ApplicationCredentials
import com.kirkbushman.auth.models.AuthType
import com.kirkbushman.auth.models.Scope
import com.kirkbushman.auth.models.ScopesEnvelope
import com.kirkbushman.auth.models.creds.ScriptCredentials
import com.kirkbushman.auth.models.Token
import com.kirkbushman.auth.models.TokenBearer
import com.kirkbushman.auth.models.creds.UserlessCredentials
import com.kirkbushman.auth.models.base.Credentials
import com.kirkbushman.auth.utils.Utils
import com.kirkbushman.auth.utils.Utils.addParamsToUrl
import com.kirkbushman.auth.utils.Utils.buildRetrofit
import com.kirkbushman.auth.utils.Utils.generateRandomString
import com.kirkbushman.auth.utils.toHeaderString
import com.kirkbushman.auth.utils.toSeparatedString
import retrofit2.Call
import retrofit2.Retrofit

/**
 * Class that is needed to interact with reddit authentication using a webView in
 * an installed application or using id and secret from a script
 */
@Suppress("unused")
class RedditAuth private constructor(

    retrofit: Retrofit,

    private val credentials: Credentials,

    private val scopes: String,

    private val storManager: StorageManager,

    private val logging: Boolean = false,

) {

    companion object {
        private const val BASE_URL = "https://www.reddit.com"
        private const val BASE_AUTH_URL = "$BASE_URL/api/v1/authorize.compact"

        private val codeRegex = Regex("(?<=(code=))([a-zA-Z0-9]|-|_)+(?=(&|\\s|))")
        private val stateRegex = Regex("(?<=(state=))([a-zA-Z0-9]|-|_)+(?=(&|\\s|))")
        private val errorRegex = Regex("(?<=(error=))([a-zA-Z0-9]|-|_)+(?=(&|\\s|))")

        // TODO - commit note: make `state` global in RedditAuth
        private val state: String = generateRandomString()

        @Volatile
        private var retrofit: Retrofit? = null
        @Volatile
        private var api: RedditService? = null

        @Synchronized
        fun setRetrofit(retrofit: Retrofit) {
            synchronized(this) {
                this.retrofit = retrofit
            }
        }

        @Synchronized
        fun getRetrofit(logging: Boolean = false): Retrofit {
            return synchronized(this) {

                if (retrofit == null) {
                    retrofit = buildRetrofit(BASE_URL, logging)
                }

                retrofit!!
            }
        }

        @Synchronized
        fun getApi(logging: Boolean = false): RedditService {
            return synchronized(this) {

                if (api == null) {
                    api = getRetrofit(logging).create(RedditService::class.java)
                }

                api!!
            }
        }

        fun getScopes(logging: Boolean = false): ScopesEnvelope? {
            val api = getApi(logging)

            val req = api.getScopes()
            val res = req.execute()

            if (res.isSuccessful) {
                return res.body()
            } else {
                throw Exception(res.errorBody().toString())
            }
        }

        fun renewTokenReq(

            token: Token,
            authType: AuthType,
            credentials: Credentials,
            logging: Boolean = false
        ): Call<Token>? {

            if (authType == AuthType.INSTALLED_APP && credentials is ApplicationCredentials) {

                if (token.refreshToken == null) {

                    throw RefreshTokenMissingException()
                }

                val api = getApi(logging)
                return api.renewToken(
                    header = "${credentials.clientId}:".toHeaderString(),

                    refreshToken = token.refreshToken
                )
            }

            if (authType == AuthType.USERLESS && credentials is UserlessCredentials) {

                val api = getApi(logging)
                return api.getAccessToken(
                    header = "${credentials.clientId}:".toHeaderString(),
                    grantType = "https://oauth.reddit.com/grants/installed_client",
                    deviceId = Utils.getDeviceUUID()
                )
            }

            if (authType == AuthType.SCRIPT && credentials is ScriptCredentials) {

                val api = getApi(logging)
                return api.getAccessToken(
                    header = "${credentials.clientId}:${credentials.clientSecret}".toHeaderString(),
                    grantType = "password",
                    username = credentials.username,
                    password = credentials.password
                )
            }

            return null
        }

        fun revokeTokenReq(

            token: Token,
            authType: AuthType,
            credentials: Credentials,
            logging: Boolean = false
        ): Call<Any>? {

            if (authType == AuthType.INSTALLED_APP && credentials is ApplicationCredentials) {

                val api = getApi(logging)
                return api.revoke(
                    header = "${credentials.clientId}:".toHeaderString(),

                    token = token.refreshToken!!,
                    tokenTypeHint = "refresh_token"
                )
            }

            if (authType == AuthType.USERLESS && credentials is UserlessCredentials) {

                val api = getApi(logging)
                return api.revoke(
                    header = "${credentials.clientId}:".toHeaderString(),

                    token = token.accessToken,
                    tokenTypeHint = "access_token"
                )
            }

            if (authType == AuthType.SCRIPT && credentials is ScriptCredentials) {

                val api = getApi(logging)
                return api.revoke(
                    header = "${credentials.clientId}:${credentials.clientSecret}".toHeaderString(),

                    token = token.accessToken,
                    tokenTypeHint = "access_token"
                )
            }

            return null
        }
    }

    private val authType: AuthType = when (credentials) {
        is ApplicationCredentials -> AuthType.INSTALLED_APP
        is UserlessCredentials -> AuthType.USERLESS
        is ScriptCredentials -> AuthType.SCRIPT

        else -> AuthType.NONE
    }

    // App Authentication without user-context
    constructor(

        /**
         * Singleton that can be passed externally or created by the builder object
         */
        retrofit: Retrofit,

        /**
         * Contains
         *
         * clientId:
         * Given by creating an app on reddit console, at https://www.reddit.com/prefs/apps,
         * Click the "create an app" button,
         *
         * deviceId:
         * A unique string that identified a device, needed since there is no user reference.
         * If this is null, it will be generated for you.
         */
        credentials: UserlessCredentials,

        /**
         * Permissions the client will have, should be as tight as possible.
         * Can be retrieved at https://www.reddit.com/api/v1/scopes
         */
        scopes: String,

        /**
         * Interface instance that is used to persist token to memory.
         * Can be extended using the method you prefer, this lib provides a working
         * example with SharedPreferences
         */
        storManager: StorageManager,

        logging: Boolean

    ) : this (retrofit, credentials as Credentials, scopes, storManager, logging)

    // Script App Authentication
    constructor(

        /**
         * Singleton that can be passed externally or created by the builder object
         */
        retrofit: Retrofit,

        /**
         * Contains
         *
         * username, password:
         * Login info of the account.
         *
         * clientId:
         * Given by creating an app on reddit console, at https://www.reddit.com/prefs/apps,
         * Click the "create an app" button
         *
         * clientSecret:
         * Given by creating an app on reddit console, at https://www.reddit.com/prefs/apps,
         * Click the "create an app" button
         *
         */
        credentials: ScriptCredentials,

        /**
         * Permissions the client will have, should be as tight as possible.
         * Can be retrieved at https://www.reddit.com/api/v1/scopes
         */
        scopes: String,

        /**
         * Interface instance that is used to persist token to memory.
         * Can be extended using the method you prefer, this lib provides a working
         * example with SharedPreferences
         */
        storManager: StorageManager,

        logging: Boolean

    ) : this (retrofit, credentials as Credentials, scopes, storManager, logging)

    init {
        ::setRetrofit.invoke(retrofit)
    }

    private val renewToken: (Token) -> Token? = { token: Token ->

        try {

            val req = renewTokenReq(token, authType, credentials, logging)
            val res = req?.execute()

            if (res?.isSuccessful == true) {

                res.body()
            } else {
                null
            }
        } catch (ex: Exception) {
            ex.printStackTrace()

            null
        }
    }

    private val revokeToken: (Token) -> Boolean = { token: Token ->

        try {

            val req = revokeTokenReq(token, authType, credentials, logging)
            val res = req?.execute()

            res?.isSuccessful == true
        } catch (ex: Exception) {
            ex.printStackTrace()

            false
        }
    }

    fun getAuthType(): AuthType {
        return authType
    }

    fun provideAuthorizeUrl(): String {

        if (authType == AuthType.INSTALLED_APP && credentials is ApplicationCredentials) {

            val params = arrayOf(
                "client_id=${credentials.clientId}",
                "response_type=code",
                "state=$state",
                "redirect_uri=${credentials.redirectUrl}",
                "duration=permanent",
                "scope=$scopes"
            )

            return addParamsToUrl(BASE_AUTH_URL, params)
        }

        return ""
    }

    fun isRedirectedUrl(url: String?): Boolean {

        if (authType == AuthType.INSTALLED_APP && credentials is ApplicationCredentials) {

            if (url == null) {
                throw IllegalStateException("Provided url is null!")
            }

            if (credentials.redirectUrl.isEmpty()) {
                throw IllegalStateException("Redirect Url was not provided or invalid!")
            }

            return url.startsWith(credentials.redirectUrl)
        }

        return false
    }

    fun getTokenBearer(): TokenBearer? {

        if (authType == AuthType.SCRIPT && credentials is ScriptCredentials) {

            try {

                val api = getApi(logging)
                val req = api.getAccessToken(
                    header = "${credentials.clientId}:${credentials.clientSecret}".toHeaderString(),
                    grantType = "password",
                    username = credentials.username,
                    password = credentials.password
                )

                val res = req.execute()

                return if (res.isSuccessful) {
                    val token = res.body() as Token
                    TokenBearer(
                        storManager = storManager,
                        token = token,
                        authType = AuthType.SCRIPT,
                        renewToken = renewToken,
                        revokeToken = revokeToken
                    )
                } else {
                    null
                }
            } catch (ex: Exception) {
                ex.printStackTrace()

                return null
            }
        }

        if (authType == AuthType.USERLESS && credentials is UserlessCredentials) {

            try {

                val api = getApi(logging)
                val req = api.getAccessToken(
                    header = "${credentials.clientId}:".toHeaderString(),
                    grantType = "https://oauth.reddit.com/grants/installed_client",
                    deviceId = Utils.getDeviceUUID()
                )

                val res = req.execute()

                return if (res.isSuccessful) {
                    val token = res.body() as Token
                    TokenBearer(
                        storManager = storManager,
                        token = token,
                        authType = AuthType.USERLESS,
                        renewToken = renewToken,
                        revokeToken = revokeToken
                    )
                } else {
                    null
                }
            } catch (ex: Exception) {
                ex.printStackTrace()

                return null
            }
        }

        return null
    }

    fun getTokenBearer(url: String?): TokenBearer? {

        if (authType == AuthType.INSTALLED_APP && credentials is ApplicationCredentials) {

            if (url == null) {
                throw IllegalStateException("Provided url is null!")
            }

            if (url.contains("error")) {
                val errorStr = errorRegex.find(url)?.value ?: ""

                if (errorStr == "access_denied") {
                    throw AccessDeniedException("User chose not to grant your app permissions!")
                }

                if (errorStr == "unsupported_response_type") {
                    throw UnsupportedResponseTypeException("Invalid response_type parameter in initial Authorization!")
                }

                if (errorStr == "invalid_scope") {
                    throw InvalidScopesException("Invalid scope parameter in initial Authorization!")
                }

                if (errorStr == "invalid_request") {
                    throw InvalidRequestException("There was an issue with the request sent to /api/v1/authorize!")
                }

                throw OAuth2Exception("The Reddit API returned the error: $errorStr")
            }

            if (!url.contains("state")) {
                throw OAuth2Exception("State param is missing from response url!")
            }

            val stateParam = stateRegex.find(url)?.value ?: ""
            if (stateParam == "") {
                throw IllegalStateException("Could not retrieve state param value!")
            }
            if (stateParam != state) {
                throw IllegalStateException("Saved State and retrieved one are different!")
            }

            val authCode = codeRegex.find(url)?.value ?: ""
            if (authCode == "") {
                throw IllegalStateException("Could not retrieve code param value!")
            }

            return requestToken(authCode)
        }

        return null
    }

    private fun requestToken(authCode: String): TokenBearer? {

        if (authType == AuthType.INSTALLED_APP &&
            credentials is ApplicationCredentials
        ) {

            try {

                val api = getApi(logging)
                val req = api.getAccessToken(
                    header = "${credentials.clientId}:".toHeaderString(),
                    grantType = "authorization_code",
                    code = authCode,
                    redirectUrl = credentials.redirectUrl
                )

                val res = req.execute()

                return if (res.isSuccessful) {
                    val token = res.body() as Token
                    TokenBearer(
                        storManager = storManager,
                        token = token,
                        authType = AuthType.INSTALLED_APP,
                        renewToken = renewToken,
                        revokeToken = revokeToken
                    )
                } else {
                    null
                }
            } catch (ex: Exception) {
                ex.printStackTrace()

                return null
            }
        }

        return null
    }

    fun hasSavedBearer(): Boolean {
        return storManager.isAuthed() && storManager.hasToken()
    }

    fun getSavedBearerType(): AuthType {
        return storManager.authType()
    }

    fun getSavedBearer(): TokenBearer {
        return TokenBearer(
            storManager = storManager,
            token = storManager.getToken(),
            authType = storManager.authType(),
            renewToken = renewToken,
            revokeToken = revokeToken
        )
    }

    abstract class AuthBuilder {

        protected var retrofit: Retrofit? = null

        protected var scopes: String = ""

        protected var storManager: StorageManager? = null
        protected var logging: Boolean = false

        open fun setRetrofit(retrofit: Retrofit?): AuthBuilder {
            this.retrofit = retrofit
            return this
        }

        open fun setScopes(scopes: Array<Scope>): AuthBuilder {
            this.scopes = scopes.toSeparatedString()
            return this
        }

        open fun setScopes(scopes: String): AuthBuilder {
            this.scopes = scopes
            return this
        }

        open fun setScopes(scopes: List<Scope>): AuthBuilder {
            this.scopes = scopes.toSeparatedString()
            return this
        }

        open fun setScopes(scopes: Array<String>): AuthBuilder {
            this.scopes = scopes.joinToString(separator = " ")
            return this
        }

        open fun setStorageManager(storManager: StorageManager?): AuthBuilder {
            this.storManager = storManager
            return this
        }

        open fun setLogging(logging: Boolean): AuthBuilder {
            this.logging = logging
            return this
        }
    }

    class Builder : AuthBuilder() {

        override fun setRetrofit(retrofit: Retrofit?): Builder {
            return super.setRetrofit(retrofit) as Builder
        }

        override fun setScopes(scopes: String): Builder {
            return super.setScopes(scopes) as Builder
        }

        override fun setScopes(scopes: Array<Scope>): Builder {
            return super.setScopes(scopes) as Builder
        }

        override fun setScopes(scopes: List<Scope>): Builder {
            return super.setScopes(scopes) as Builder
        }

        override fun setScopes(scopes: Array<String>): Builder {
            return super.setScopes(scopes) as Builder
        }

        override fun setStorageManager(storManager: StorageManager?): Builder {
            return super.setStorageManager(storManager) as Builder
        }

        override fun setLogging(logging: Boolean): Builder {
            return super.setLogging(logging) as Builder
        }

        fun setApplicationCredentials(credentials: ApplicationCredentials): AppAuthBuilder {
            val builder = AppAuthBuilder(credentials)
            builder.setRetrofit(retrofit)
            builder.setScopes(scopes)
            builder.setStorageManager(storManager)
            builder.setLogging(logging)
            return builder
        }

        fun setApplicationCredentials(clientId: String, redirectUrl: String): AppAuthBuilder {
            val builder = AppAuthBuilder(
                ApplicationCredentials(
                    clientId = clientId,
                    redirectUrl = redirectUrl
                )
            )

            builder.setRetrofit(retrofit)
            builder.setScopes(scopes)
            builder.setStorageManager(storManager)
            builder.setLogging(logging)
            return builder
        }

        fun setUserlessCredentials(credentials: UserlessCredentials): UserlessAuthBuilder {
            val builder = UserlessAuthBuilder(credentials)
            builder.setRetrofit(retrofit)
            builder.setScopes(scopes)
            builder.setStorageManager(storManager)
            builder.setLogging(logging)
            return builder
        }

        fun setUserlessCredentials(clientId: String, deviceId: String? = null): UserlessAuthBuilder {
            val builder = UserlessAuthBuilder(UserlessCredentials(clientId = clientId, deviceId = deviceId))
            builder.setRetrofit(retrofit)
            builder.setScopes(scopes)
            builder.setStorageManager(storManager)
            builder.setLogging(logging)
            return builder
        }

        fun setScriptAuthCredentials(credentials: ScriptCredentials): ScriptAuthBuilder {
            val builder = ScriptAuthBuilder(credentials)
            builder.setRetrofit(retrofit)
            builder.setScopes(scopes)
            builder.setStorageManager(storManager)
            builder.setLogging(logging)
            return builder
        }

        fun setScriptAuthCredentials(username: String, password: String, clientId: String, clientSecret: String): ScriptAuthBuilder {
            val builder = ScriptAuthBuilder(
                ScriptCredentials(
                    username = username,
                    password = password,
                    clientId = clientId,
                    clientSecret = clientSecret
                )
            )

            builder.setRetrofit(retrofit)
            builder.setScopes(scopes)
            builder.setStorageManager(storManager)
            builder.setLogging(logging)
            return builder
        }
    }

    class AppAuthBuilder(private var credentials: ApplicationCredentials) : AuthBuilder() {

        override fun setRetrofit(retrofit: Retrofit?): AppAuthBuilder {
            return super.setRetrofit(retrofit) as AppAuthBuilder
        }

        override fun setScopes(scopes: String): AppAuthBuilder {
            return super.setScopes(scopes) as AppAuthBuilder
        }

        override fun setScopes(scopes: Array<Scope>): AppAuthBuilder {
            return super.setScopes(scopes) as AppAuthBuilder
        }

        override fun setScopes(scopes: List<Scope>): AppAuthBuilder {
            return super.setScopes(scopes) as AppAuthBuilder
        }

        override fun setScopes(scopes: Array<String>): AppAuthBuilder {
            return super.setScopes(scopes) as AppAuthBuilder
        }

        override fun setStorageManager(storManager: StorageManager?): AppAuthBuilder {
            return super.setStorageManager(storManager) as AppAuthBuilder
        }

        override fun setLogging(logging: Boolean): AppAuthBuilder {
            return super.setLogging(logging) as AppAuthBuilder
        }

        fun build(): RedditAuth {

            return RedditAuth(
                retrofit = retrofit ?: buildRetrofit(BASE_URL, logging),
                credentials = credentials,
                scopes = scopes,
                storManager = storManager
                    ?: throw IllegalArgumentException("StorageManager must not be null!"),
                logging = logging
            )
        }
    }

    class UserlessAuthBuilder(private var credentials: UserlessCredentials) : AuthBuilder() {

        override fun setRetrofit(retrofit: Retrofit?): UserlessAuthBuilder {
            return super.setRetrofit(retrofit) as UserlessAuthBuilder
        }

        override fun setScopes(scopes: String): UserlessAuthBuilder {
            return super.setScopes(scopes) as UserlessAuthBuilder
        }

        override fun setScopes(scopes: Array<Scope>): UserlessAuthBuilder {
            return super.setScopes(scopes) as UserlessAuthBuilder
        }

        override fun setScopes(scopes: List<Scope>): UserlessAuthBuilder {
            return super.setScopes(scopes) as UserlessAuthBuilder
        }

        override fun setScopes(scopes: Array<String>): UserlessAuthBuilder {
            return super.setScopes(scopes) as UserlessAuthBuilder
        }

        override fun setStorageManager(storManager: StorageManager?): UserlessAuthBuilder {
            return super.setStorageManager(storManager) as UserlessAuthBuilder
        }

        override fun setLogging(logging: Boolean): UserlessAuthBuilder {
            return super.setLogging(logging) as UserlessAuthBuilder
        }

        fun build(): RedditAuth {

            return RedditAuth(
                retrofit = retrofit ?: buildRetrofit(BASE_URL, logging),
                credentials = credentials,
                scopes = scopes,
                storManager = storManager
                    ?: throw IllegalArgumentException("StorageManager must not be null!"),
                logging = logging
            )
        }
    }

    class ScriptAuthBuilder(private var credentials: ScriptCredentials) : AuthBuilder() {

        override fun setRetrofit(retrofit: Retrofit?): ScriptAuthBuilder {
            return super.setRetrofit(retrofit) as ScriptAuthBuilder
        }

        override fun setScopes(scopes: String): ScriptAuthBuilder {
            return super.setScopes(scopes) as ScriptAuthBuilder
        }

        override fun setScopes(scopes: Array<Scope>): ScriptAuthBuilder {
            return super.setScopes(scopes) as ScriptAuthBuilder
        }

        override fun setScopes(scopes: List<Scope>): ScriptAuthBuilder {
            return super.setScopes(scopes) as ScriptAuthBuilder
        }

        override fun setScopes(scopes: Array<String>): ScriptAuthBuilder {
            return super.setScopes(scopes) as ScriptAuthBuilder
        }

        override fun setStorageManager(storManager: StorageManager?): ScriptAuthBuilder {
            return super.setStorageManager(storManager) as ScriptAuthBuilder
        }

        override fun setLogging(logging: Boolean): ScriptAuthBuilder {
            return super.setLogging(logging) as ScriptAuthBuilder
        }

        fun build(): RedditAuth {

            return RedditAuth(
                retrofit = retrofit ?: buildRetrofit(BASE_URL, logging),
                credentials = credentials,
                scopes = scopes,
                storManager = storManager
                    ?: throw IllegalArgumentException("StorageManager must not be null!"),
                logging = logging
            )
        }
    }
}
