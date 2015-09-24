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

public class Request(val method: Method, val urlString: String, val parameters: Map<String, Any>?, val encoding: ParameterEncoding, val headers: Map<String, String>?) {
    private var completed: Boolean = false

    private var urlOrNull: URL? = null
    private var urlConnectionOrNull: URLConnection? = null
    private var bytes: ByteArray? = null
    private var exception: Exception? = null

    private var totalBytesWritten: Long = 0L
    private var totalBytesExpectedToWrite: Long = -1L

    private var progressHandlers: MutableList<(Long, Long, Long) -> Unit> = ArrayList()
    private var streamHandlers: MutableList<(ByteArray) -> Unit> = ArrayList()
    private var completionHandlers: MutableList<(URL?, URLConnection?, ByteArray?, Exception?) -> Unit> = ArrayList()

    init {
        try {
            val parametersString = when (encoding) {
                ParameterEncoding.URL -> parameters?.entrySet()?.fold("") { result, entry ->
                    result + if (result.length() == 0) {
                        "?"
                    } else {
                        "&"
                    } + URLEncoder.encode(entry.getKey(), Charsets.UTF_8.name()) + "=" + URLEncoder.encode(entry.getValue().toString(), Charsets.UTF_8.name())
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

            urlConnection.requestMethod = method.rawValue

            headers?.entrySet()?.forEach { urlConnection.setRequestProperty(it.getKey(), it.getValue()) }

            when (method) {
                Method.POST -> {
                    urlConnection.doOutput = true
                    BufferedWriter(OutputStreamWriter(urlConnection.outputStream, "UTF-8")).use {
                        it.write(parametersString)
                    }
                }
                else -> {
                }
            }

            val handler = Handler()
            thread {
                try {
                    urlConnection.connect()

                    try {
                        totalBytesExpectedToWrite = urlConnection.getHeaderField("Content-Length").toLong()
                    } catch(e: NumberFormatException) {
                    }

                    val out = ByteArrayOutputStream()
                    val bufferLength = Math.min(0x10000, if (totalBytesExpectedToWrite < 0L || totalBytesExpectedToWrite > Int.MAX_VALUE) {
                        Int.MAX_VALUE
                    } else {
                        totalBytesExpectedToWrite.toInt()
                    }) // to prevent unnecessary copies for the stream handlers
                    BufferedInputStream(urlConnection.inputStream, bufferLength).use {
                        val buffer = ByteArray(bufferLength)
                        while (true) {
                            val length = it.read(buffer)
                            if (length == -1) {
                                synchronized(this) {
                                    callProgressHandlers(0L)
                                    callStreamHandlers(ByteArray(0))
                                }
                                break
                            }

                            totalBytesWritten += length

                            out.write(buffer, 0, length)

                            synchronized(this) {
                                callProgressHandlers(length.toLong())
                                callStreamHandlers(if (length == buffer.size()) {
                                    buffer
                                } else {
                                    buffer.copyOf(length)
                                })
                            }
                        }
                    }
                    bytes = out.toByteArray()

                    complete(handler)
                } catch(e: Exception) {
                    exception = e
                    complete(handler)
                }
            }
        } catch(e: Exception) {
            exception = e
            complete(null)
        }
    }

    public fun progress(progressHandler: ((Long, Long, Long) -> Unit)?): Request {
        if (progressHandler != null) {
            synchronized(this) {
                if (completed) {
                    callProgressHandler(progressHandler, 0L)
                } else {
                    progressHandlers.add(progressHandler)
                }
            }
        }
        return this
    }

    public fun stream(streamHandler: ((ByteArray) -> Unit)?): Request {
        if (streamHandler != null) {
            synchronized(this) {
                if (completed) {
                    callStreamHandler(streamHandler, ByteArray(0))
                } else {
                    streamHandlers.add(streamHandler)
                }
            }
        }
        return this
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

    private fun callProgressHandler(progressHandler: (Long, Long, Long) -> Unit, bytesWritten: Long) {
        progressHandler(bytesWritten, totalBytesWritten, totalBytesExpectedToWrite)
    }

    private fun callProgressHandlers(bytesWritten: Long) {
        progressHandlers.forEach { callProgressHandler(it, bytesWritten) }
    }

    private fun callStreamHandler(streamHandler: (ByteArray) -> Unit, readBytes: ByteArray) {
        streamHandler(readBytes)
    }

    private fun callStreamHandlers(readBytes: ByteArray) {
        streamHandlers.forEach { callStreamHandler(it, readBytes) }
    }

    private fun callCompletionHandler(completionHandler: (URL?, URLConnection?, ByteArray?, Exception?) -> Unit) {
        completionHandler(urlOrNull, urlConnectionOrNull, bytes, exception)
    }

    private fun callCompletionHandlers() {
        completionHandlers.forEach { callCompletionHandler(it) }
    }

    @Synchronized private fun complete(handler: Handler?) {
        if (handler != null) {
            handler.post {
                callCompletionHandlers()
            }
        } else {
            callCompletionHandlers()
        }
        progressHandlers.clear()
        streamHandlers.clear()
        completionHandlers.clear()
        completed = true
    }

    public fun responseString(charset: Charset? = null, completionHandler: (URL?, URLConnection?, Result<String>) -> Unit): Request {
        return response { url, urlConnection, bytes, exception ->
            val result = bytes?.let { String(it, charset ?: Charsets.UTF_8) }?.let { Result.Success(it) } ?: Result.Failure<String>(bytes, exception!!)
            completionHandler(url, urlConnection, result)
        }
    }
}