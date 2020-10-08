package com.kirkbushman.auth.managers

import com.kirkbushman.auth.models.AuthType
import com.kirkbushman.auth.models.Token

/**
 * Interface for persisting auth data on device,
 * can be extended with your preferred method
 */
interface StorageManager {

    fun internalSharedPrefsKey(): String
    fun isAuthPrefsKey(): String
    fun authTypePrefsKey(): String
    fun lastAccessTokenPrefsKey(): String
    fun lastRefreshTokenPrefsKey(): String
    fun lastTokenTypePrefsKey(): String
    fun lastExpiresInPrefsKey(): String
    fun lastCreatedTimePrefsKey(): String
    fun lastScopesPrefsKey(): String

    fun isAuthed(): Boolean

    fun authType(): AuthType

    fun hasToken(): Boolean
    fun getToken(): Token?

    fun saveToken(token: Token, authType: AuthType)

    fun clearAll()
}
