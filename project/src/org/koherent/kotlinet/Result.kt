package org.koherent.kotlinet

sealed class Result<out Value> private constructor() {
    class Success<out Value>(val value: Value) : Result<Value>() {}
    class Failure<out Value>(val bytes: ByteArray?, val exception: Exception) : Result<Value>() {}
}