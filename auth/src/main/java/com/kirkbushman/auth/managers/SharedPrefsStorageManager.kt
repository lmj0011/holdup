package com.kirkbushman.auth.managers

import android.content.Context
import androidx.core.content.edit
import com.kirkbushman.auth.models.AuthType
import com.kirkbushman.auth.models.Token

/**
 * Implementation of StorageManager using SharedPreferences
 */
class SharedPrefsStorageManager(private val context: Context, private val tag: String? = null) : StorageManager {

    // TODO - commit note: make shared preference file name unique
    override fun internalSharedPrefsKey(): String = "${context.packageName}_preferences"

    private val prefs by lazy { context.getSharedPreferences(internalSharedPrefsKey(), Context.MODE_PRIVATE) }

    override fun isAuthPrefsKey(): String {
        val key = "android_reddit_oauth2_is_authed_first_time"
        return if(tag != null) "${key}_${tag}"
        else key
    }

    override fun authTypePrefsKey(): String {
        val key = "android_reddit_oauth2_authentication_type"
        return if(tag != null) "${key}_${tag}"
        else key
    }

    override fun lastAccessTokenPrefsKey(): String {
        val key = "android_reddit_oauth2_current_access_token"
        return if(tag != null) "${key}_${tag}"
        else key
    }

    override fun lastRefreshTokenPrefsKey(): String {
        val key = "android_reddit_oauth2_current_refresh_token"
        return if(tag != null) "${key}_${tag}"
        else key
    }

    override fun lastTokenTypePrefsKey(): String {
        val key = "android_reddit_oauth2_current_token_type"
        return if(tag != null) "${key}_${tag}"
        else key
    }

    override fun lastExpiresInPrefsKey(): String {
        val key = "android_reddit_oauth2_current_expires_in"
        return if(tag != null) "${key}_${tag}"
        else key
    }

    override fun lastCreatedTimePrefsKey(): String {
        val key = "android_reddit_oauth2_current_created_time"
        return if(tag != null) "${key}_${tag}"
        else key
    }

    override fun lastScopesPrefsKey(): String {
        val key = "android_reddit_oauth2_current_scopes"
        return if(tag != null) "${key}_${tag}"
        else key
    }

    override fun isAuthed(): Boolean {
        return prefs.getBoolean(isAuthPrefsKey(), false)
    }

    override fun authType(): AuthType {
        return AuthType.valueOf(prefs.getString(authTypePrefsKey(), "NONE") ?: "NONE")
    }

    override fun hasToken(): Boolean {
        return prefs.contains(lastAccessTokenPrefsKey()) &&
            prefs.contains(lastTokenTypePrefsKey()) &&
            prefs.contains(lastExpiresInPrefsKey()) &&
            prefs.contains(lastCreatedTimePrefsKey()) &&
            prefs.contains(lastScopesPrefsKey())
    }

    override fun getToken(): Token {
        if (
            !prefs.contains(lastAccessTokenPrefsKey()) ||
            !prefs.contains(lastTokenTypePrefsKey()) ||
            !prefs.contains(lastExpiresInPrefsKey()) ||
            !prefs.contains(lastCreatedTimePrefsKey()) ||
            !prefs.contains(lastScopesPrefsKey())
        ) {
            throw IllegalStateException("Token not found in store! did you ever saved one?")
        }

        val accessToken = prefs.getString(lastAccessTokenPrefsKey(), "") as String
        val refreshToken = prefs.getString(lastRefreshTokenPrefsKey(), null)
        val tokenType = prefs.getString(lastTokenTypePrefsKey(), "") as String
        val expiresInSecs = prefs.getInt(lastExpiresInPrefsKey(), 0)
        val createdTime = prefs.getLong(lastCreatedTimePrefsKey(), 0L)
        val scopes = prefs.getString(lastScopesPrefsKey(), "") as String

        return Token(
            accessToken = accessToken,
            refreshToken = refreshToken,
            tokenType = tokenType,
            expiresInSecs = expiresInSecs,
            createdTime = createdTime,
            scopes = scopes
        )
    }

    override fun saveToken(token: Token, authType: AuthType) {

        prefs.edit {

            if (!prefs.contains(isAuthPrefsKey()) || !prefs.getBoolean(isAuthPrefsKey(), false)) {
                putBoolean(isAuthPrefsKey(), true)
            }

            putString(authTypePrefsKey(), authType.toString())

            putString(lastAccessTokenPrefsKey(), token.accessToken)
            putString(lastRefreshTokenPrefsKey(), token.refreshToken)
            putString(lastTokenTypePrefsKey(), token.tokenType)
            putInt(lastExpiresInPrefsKey(), token.expiresInSecs)
            putLong(lastCreatedTimePrefsKey(), token.createdTime)
            putString(lastScopesPrefsKey(), token.scopes)
        }
    }

    override fun clearAll() {
        prefs.edit {

            if (prefs.contains(isAuthPrefsKey()))
                this.remove(isAuthPrefsKey())

            if (prefs.contains(authTypePrefsKey()))
                this.remove(authTypePrefsKey())

            if (prefs.contains(lastAccessTokenPrefsKey()))
                this.remove(lastAccessTokenPrefsKey())
            if (prefs.contains(lastRefreshTokenPrefsKey()))
                this.remove(lastRefreshTokenPrefsKey())
            if (prefs.contains(lastTokenTypePrefsKey()))
                this.remove(lastTokenTypePrefsKey())
            if (prefs.contains(lastExpiresInPrefsKey()))
                this.remove(lastExpiresInPrefsKey())
            if (prefs.contains(lastCreatedTimePrefsKey()))
                this.remove(lastCreatedTimePrefsKey())
            if (prefs.contains(lastScopesPrefsKey()))
                this.remove(lastScopesPrefsKey())
        }
    }
}
