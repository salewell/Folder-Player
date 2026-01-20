package com.wing.folderplayer.data.source

import android.net.Uri
import android.util.Xml
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.xmlpull.v1.XmlPullParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URLDecoder
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.*

class WebDavSource(
    private val baseUrl: String,
    private val user: String,
    private val pass: String
) : MusicSource {

    private val client = OkHttpClient.Builder()
        .followRedirects(true)
        .followSslRedirects(true)
        .authenticator { _, response ->
            // Only send credentials to the original host to avoid leaking them to redirected hosts (e.g. cloud direct links)
            if (response.request.url.host != Uri.parse(baseUrl).host) return@authenticator null
            
            if (response.count() > 3) return@authenticator null
            val credential = okhttp3.Credentials.basic(user, pass)
            response.request.newBuilder().header("Authorization", credential).build()
        }.build()

    private fun okhttp3.Response.count(): Int {
        var count = 1
        var r = this.priorResponse
        while (r != null) {
            count++
            r = r.priorResponse
        }
        return count
    }

    override suspend fun list(path: String): List<MusicFile> = withContext(Dispatchers.IO) {
        val propfindXml = """
            <?xml version="1.0" encoding="utf-8" ?>
            <propfind xmlns="DAV:">
              <prop>
                <displayname/>
                <getcontentlength/>
                <getlastmodified/>
                <resourcetype/>
              </prop>
            </propfind>
        """.trimIndent()

        val request = Request.Builder()
            .url(path)
            .method("PROPFIND", propfindXml.toRequestBody("text/xml".toMediaType()))
            .header("Depth", "1")
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext emptyList()
                val body = response.body?.string() ?: return@withContext emptyList()
                parsePropfindResponse(body, path)
            }
        } catch (e: Exception) {
            android.util.Log.e("WebDavSource", "Error listing $path", e)
            emptyList()
        }
    }

    private fun parsePropfindResponse(xml: String, currentPath: String): List<MusicFile> {
        val result = mutableListOf<MusicFile>()
        val parser = Xml.newPullParser()
        parser.setInput(xml.reader())

        var eventType = parser.eventType
        var currentHref: String? = null
        var currentDisplayName: String? = null
        var currentSize: Long = 0
        var currentModified: Long = 0
        var isDirectory = false

        while (eventType != XmlPullParser.END_DOCUMENT) {
            val tagName = parser.name
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    when (tagName) {
                        "response" -> {
                            currentHref = null
                            currentDisplayName = null
                            currentSize = 0
                            currentModified = 0
                            isDirectory = false
                        }
                        "href" -> currentHref = parser.nextText()
                        "displayname" -> currentDisplayName = parser.nextText()
                        "getcontentlength" -> currentSize = parser.nextText()?.toLongOrNull() ?: 0
                        "getlastmodified" -> {
                            val dateStr = parser.nextText()
                            if (dateStr != null) {
                                currentModified = try {
                                    SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.ENGLISH).parse(dateStr)?.time ?: 0
                                } catch (e: Exception) { 0 }
                            }
                        }
                        "collection" -> isDirectory = true
                    }
                }
                XmlPullParser.END_TAG -> {
                    when (tagName) {
                        "response" -> {
                            if (currentHref != null) {
                                val decodedHref = try { URLDecoder.decode(currentHref, "UTF-8") } catch (e: Exception) { currentHref }
                                
                                // Standardize both paths for comparison
                                val normHref = decodedHref.trimEnd('/')
                                val normCurrent = try { URLDecoder.decode(currentPath, "UTF-8").trimEnd('/') } catch(e: Exception) { currentPath.trimEnd('/') }
                                
                                // Extract just the path portion for comparison
                                val hrefPathOnly = if (normHref.startsWith("http")) Uri.parse(normHref).path?.trimEnd('/') ?: "" else normHref.trimEnd('/')
                                val currentPathOnly = if (normCurrent.startsWith("http")) Uri.parse(normCurrent).path?.trimEnd('/') ?: "" else normCurrent.trimEnd('/')

                                // Skip if it's the directory itself (case insensitive to be safe with some NAS systems)
                                if (!hrefPathOnly.equals(currentPathOnly, ignoreCase = true)) {
                                     val name = currentDisplayName ?: try { URLDecoder.decode(normHref.substringAfterLast('/'), "UTF-8") } catch(e: Exception) { normHref.substringAfterLast('/') }
                                     if (name.isNotEmpty()) {
                                        val fullPath = if (currentHref.startsWith("http")) currentHref else {
                                            val root = if (baseUrl.contains("://")) {
                                                val uri = Uri.parse(baseUrl)
                                                "${uri.scheme}://${uri.authority}"
                                            } else ""
                                            if (currentHref.startsWith("/")) "$root$currentHref" else "$root/$currentHref"
                                        }

                                        result.add(MusicFile(
                                            name = name,
                                            path = fullPath,
                                            isDirectory = isDirectory,
                                            size = currentSize,
                                            lastModified = currentModified
                                        ))
                                     }
                                }
                            }
                        }
                    }
                }
            }
            eventType = parser.next()
        }
        return result
    }

    override fun getUri(path: String): Uri {
        return Uri.parse(path)
    }

    override suspend fun readText(path: String): String? = withContext(Dispatchers.IO) {
        val request = Request.Builder().url(path).build()
        try {
            client.newCall(request).execute().use { it.body?.string() }
        } catch (e: Exception) { null }
    }
}
