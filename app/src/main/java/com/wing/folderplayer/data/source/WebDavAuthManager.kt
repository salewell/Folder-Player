package com.wing.folderplayer.data.source

object WebDavAuthManager {
    private var username: String? = null
    private var password: String? = null

    fun setCredentials(user: String, pass: String) {
        username = user
        password = pass
    }

    fun clear() {
        username = null
        password = null
    }

    fun getUsername(): String? = username
    fun getPassword(): String? = password

    val authHeader: String?
        get() {
            val u = username ?: return null
            val p = password ?: return null
            val bytes = "$u:$p".toByteArray(Charsets.UTF_8)
            val base64 = android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
            return "Basic $base64"
        }
}
