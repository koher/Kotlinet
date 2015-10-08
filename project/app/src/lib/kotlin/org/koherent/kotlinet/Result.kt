package org.koherent.kotlinet

public open class Result<out Value> private constructor() {
    public class Success<out Value>(val value: Value) : Result<Value>() {}
    public class Failure<out Value>(val bytes: ByteArray?, val exception: Exception) : Result<Value>() {}
}