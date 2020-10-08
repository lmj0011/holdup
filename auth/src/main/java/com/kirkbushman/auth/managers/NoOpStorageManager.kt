package com.kirkbushman.auth.managers

import com.kirkbushman.auth.models.AuthType
import com.kirkbushman.auth.models.Token

class NoOpStorageManager : StorageManager {
    override fun internalSharedPrefsKey(): String {
        TODO("Not yet implemented")
    }

    override fun isAuthPrefsKey(): String {
        TODO("Not yet implemented")
    }

    override fun authTypePrefsKey(): String {
        TODO("Not yet implemented")
    }

    override fun lastAccessTokenPrefsKey(): String {
        TODO("Not yet implemented")
    }

    override fun lastRefreshTokenPrefsKey(): String {
        TODO("Not yet implemented")
    }

    override fun lastTokenTypePrefsKey(): String {
        TODO("Not yet implemented")
    }

    override fun lastExpiresInPrefsKey(): String {
        TODO("Not yet implemented")
    }

    override fun lastCreatedTimePrefsKey(): String {
        TODO("Not yet implemented")
    }

    override fun lastScopesPrefsKey(): String {
        TODO("Not yet implemented")
    }

    override fun isAuthed(): Boolean {
        return false
    }

    override fun authType(): AuthType {
        return AuthType.NONE
    }

    override fun hasToken(): Boolean {
        return false
    }

    override fun getToken(): Token? {
        return null
    }

    override fun saveToken(token: Token, authType: AuthType) {}
    override fun clearAll() {}
}
