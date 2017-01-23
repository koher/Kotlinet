package org.koherent.kotlinet

import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

enum class Method(val rawValue: String) {
    OPTIONS("OPTIONS"),
    GET("GET"),
    HEAD("HEAD"),
    POST("POST"),
    PUT("PUT"),
    PATCH("PATCH"),
    DELETE("DELETE"),
    TRACE("TRACE"),
    CONNECT("CONNECT")
}

enum class ParameterEncoding {
    URL
}

private val defaultMaxBytesOnMemory = 0x100000

fun request(method: Method, urlString: String, parameters: Map<String, Any>? = null, encoding: ParameterEncoding = ParameterEncoding.URL, headers: Map<String, String>? = null, maxBytesOnMemory: Int = defaultMaxBytesOnMemory): Request {
    return Request(method, urlString, parameters, encoding, headers, maxBytesOnMemory)
}

fun download(method: Method, urlString: String, destination: File, maxBytesOnMemory: Int = defaultMaxBytesOnMemory): Request {
    return download(method, urlString, null, destination, maxBytesOnMemory)
}

fun download(method: Method, urlString: String, parameters: Map<String, Any>?, destination: File, maxBytesOnMemory: Int = defaultMaxBytesOnMemory): Request {
    return download(method, urlString, parameters, ParameterEncoding.URL, destination, maxBytesOnMemory)
}

fun download(method: Method, urlString: String, parameters: Map<String, Any>?, encoding: ParameterEncoding, destination: File, maxBytesOnMemory: Int = defaultMaxBytesOnMemory): Request {
    return download(method, urlString, parameters, encoding, null, destination, maxBytesOnMemory)
}

fun download(method: Method, urlString: String, parameters: Map<String, Any>?, encoding: ParameterEncoding, headers: Map<String, String>?, destination: File, maxBytesOnMemory: Int = defaultMaxBytesOnMemory): Request {
    val request = request(method, urlString, parameters, encoding, headers, maxBytesOnMemory)

    val temporaryDestination = File.createTempFile("__kotlinet__", null, destination.parentFile)

    val out = BufferedOutputStream(FileOutputStream(temporaryDestination))

    return request.stream { bytes ->
        out.write(bytes)
    }.response { url, urlConnection, bytes, exception ->
        out.close()

        if (exception == null && !temporaryDestination.renameTo(destination)) {
            temporaryDestination.delete()
            throw IOException("Failed to rename a temporary file to " + destination.absolutePath)
        }
    }
}
