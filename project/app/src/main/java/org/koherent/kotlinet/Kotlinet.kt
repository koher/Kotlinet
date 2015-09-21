package org.koherent.kotlinet

import java.net.URL

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

public fun request(method: Method, urlString: String, parameters: Map<String, Object>? = null, encoding: ParameterEncoding = ParameterEncoding.URL, headers: Map<String, String>? = null): Request {
    return Request(method, urlString, parameters, encoding, headers)
}