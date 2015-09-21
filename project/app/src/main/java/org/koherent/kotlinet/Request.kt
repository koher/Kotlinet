package org.koherent.kotlinet

import android.os.Handler
import java.io.*
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLConnection
import java.net.URLEncoder
import java.nio.charset.Charset
import kotlin.concurrent.thread

public class Request(val method: Method, val urlString: String, val parameters: Map<String, Object>?, val encoding: ParameterEncoding, val headers: Map<String, String>?) {
    public fun response(completionHandler: (URL?, URLConnection?, ByteArray?, Exception?) -> Unit): Request {
        var urlOrNull: URL? = null
        var urlConnectionOrNull: URLConnection? = null
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

            thread {
                urlConnection.connect()
                val out = ByteArrayOutputStream()
                val bytes: ByteArray = BufferedInputStream(urlConnection.inputStream).use {
                    it.readBytes()
                }

                Handler().post { completionHandler(url, urlConnection, bytes, null) }
            }
        } catch(e: Exception) {
            completionHandler(urlOrNull, urlConnectionOrNull, null, e)
        }

        return this
    }

    public fun responseString(charset: Charset? = null, completionHandler: (URL?, URLConnection?, Result<String>) -> Unit): Request {
        return response { url, urlConnection, bytes, exception ->
            bytes?.let { String(it, charset ?: Charsets.UTF_8) }?.let { Result.Success<String>(it) } ?: Result.Failure(bytes, exception!!)
        }
    }
}