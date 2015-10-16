package org.koherent.kotlinet

import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

public enum class Method(val rawValue: String) {
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

public enum class ParameterEncoding {
    URL
}

public fun request(method: Method, urlString: String, parameters: Map<String, Any>? = null, encoding: ParameterEncoding = ParameterEncoding.URL, headers: Map<String, String>? = null): Request {
    return Request(method, urlString, parameters, encoding, headers)
}

public fun download(method: Method, urlString: String, destination: File): Request {
    return download(method, urlString, null, destination)
}

public fun download(method: Method, urlString: String, parameters: Map<String, Any>?, destination: File): Request {
    return download(method, urlString, parameters, ParameterEncoding.URL, destination)
}

public fun download(method: Method, urlString: String, parameters: Map<String, Any>?, encoding: ParameterEncoding, destination: File): Request {
    return download(method, urlString, parameters, encoding, null, destination)
}

public fun download(method: Method, urlString: String, parameters: Map<String, Any>?, encoding: ParameterEncoding, headers: Map<String, String>?, destination: File): Request {
    val request = request(method, urlString, parameters, encoding, headers)

    val temporaryDestination = File.createTempFile("__kotlinet__", null, destination.parentFile)

    val out = BufferedOutputStream(FileOutputStream(temporaryDestination))

    return request.stream { bytes ->
        out.write(bytes)
    }.response { url, urlConnection, bytes, exception ->
        out.close()

        if (!temporaryDestination.renameTo(destination)) {
            temporaryDestination.delete()
            throw IOException("Failed to rename a temporary file to " + destination.absolutePath)
        }
    }
}
