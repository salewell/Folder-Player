package com.wing.folderplayer.data.prefs

import android.content.Context
import android.content.SharedPreferences

class LyricPreferences(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("lyric_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_LYRIC_API_URL = "lyric_api_url"
        private const val DEFAULT_LYRIC_API_URL = "https://api.lrc.cx/lyrics"
    }

    fun getLyricApiUrl(): String {
        return prefs.getString(KEY_LYRIC_API_URL, DEFAULT_LYRIC_API_URL) ?: DEFAULT_LYRIC_API_URL
    }

    fun setLyricApiUrl(url: String) {
        prefs.edit().putString(KEY_LYRIC_API_URL, url).apply()
    }

    fun getGeminiApiKey(): String {
        return prefs.getString("gemini_api_key", "") ?: ""
    }

    fun setGeminiApiKey(key: String) {
        prefs.edit().putString("gemini_api_key", key).apply()
    }

    fun getAiBaseUrl(): String {
        return prefs.getString("ai_base_url", "https://api.openai.com/v1") ?: "https://api.openai.com/v1"
    }

    fun setAiBaseUrl(url: String) {
        prefs.edit().putString("ai_base_url", url).apply()
    }

    fun getAiApiKey(): String {
        return prefs.getString("ai_api_key", "") ?: ""
    }

    fun setAiApiKey(key: String) {
        prefs.edit().putString("ai_api_key", key).apply()
    }

    fun getAiModel(): String {
        return prefs.getString("ai_model", "gpt-3.5-turbo") ?: "gpt-3.5-turbo"
    }

    fun setAiModel(model: String) {
        prefs.edit().putString("ai_model", model).apply()
    }

    fun clear() {
        prefs.edit().clear().apply()
    }
}
