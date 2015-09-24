package org.koherent.kotlinet

import android.os.Handler
import java.io.*
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLConnection
import java.net.URLEncoder
import java.nio.charset.Charset
import java.util.*
import kotlin.concurrent.thread

public class Request(val method: Method, val urlString: String, val parameters: Map<String, Object>?, val encoding: ParameterEncoding, val headers: Map<String, String>?) {
    private var completed: Boolean = false

    private var urlOrNull: URL? = null
    private var urlConnectionOrNull: URLConnection? = null
    private var bytes: ByteArray? = null
    private var exception: Exception? = null

    private var completionHandlers: MutableList<(URL?, URLConnection?, ByteArray?, Exception?) -> Unit> = ArrayList()

    init {
        try {
            val parametersString = when (encoding) {
                ParameterEncoding.URL -> parameters?.entrySet()?.fold("") { result, entry ->
                    result + if (result.length() == 0) {
                        "?"
                    } else {
                        "&"
                    } + URLEncoder.encode(entry.getKey(), "UTF-8") + "=" + URLEncoder.encode(entry.getValue().toString())
                } ?: ""
            }

            val urlStringWithParameters = when (method) {
                Method.GET, Method.HEAD -> urlString + parametersString
                else -> urlString
            }

            val url = URL(urlStringWithParameters)
            urlOrNull = url

            val urlConnection = url.openConnection()
            urlConnectionOrNull = urlConnection

            if (!(urlConnection is HttpURLConnection)) {
                throw IOException("Unsupported URL connection: " + urlConnection.javaClass.name)
            }

            urlConnection.setRequestMethod(method.rawValue)

            headers?.entrySet()?.forEach { urlConnection.setRequestProperty(it.getKey(), it.getValue()) }

            when (method) {
                Method.POST -> {
                    urlConnection.doOutput = true
                    BufferedWriter(OutputStreamWriter(urlConnection.getOutputStream(), "UTF-8")).use {
                        it.write(parametersString)
                    }
                }
            }

            val handler = Handler()
            thread {
                try {
                    urlConnection.connect()
                    val out = ByteArrayOutputStream()
                    bytes = BufferedInputStream(urlConnection.inputStream).use {
                        it.readBytes()
                    }
                    complete()
                } catch(e: Exception) {
                    exception = e
                    complete()
                }
            }
        } catch(e: Exception) {
            exception = e
            complete()
        }
    }

    public fun response(completionHandler: (URL?, URLConnection?, ByteArray?, Exception?) -> Unit): Request {
        synchronized(this) {
            if (completed) {
                callCompletionHandler(completionHandler)
            } else {
                completionHandlers.add(completionHandler)
            }
        }
        return this
    }

    private fun callCompletionHandler(completionHandler: (URL?, URLConnection?, ByteArray?, Exception?) -> Unit) {
        completionHandler(urlOrNull, urlConnectionOrNull, bytes, exception)
    }

    Synchronized private fun complete() {
        completionHandlers.forEach { callCompletionHandler(it) }
        completionHandlers.clear()
        completed = true
    }

    public fun responseString(charset: Charset? = null, completionHandler: (URL?, URLConnection?, Result<String>) -> Unit): Request {
        return response { url, urlConnection, bytes, exception ->
            bytes?.let { String(it, charset ?: Charsets.UTF_8) }?.let { Result.Success<String>(it) } ?: Result.Failure(bytes, exception!!)
        }
    }
}