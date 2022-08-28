package com.tongji.basic

import kotlin.coroutines.*

fun main() {
    val continuation = suspend {
        println("In Coroutine.")
        5
    }.createCoroutine(object : Continuation<Int> {
        override fun resumeWith(result: Result<Int>) {
            println("Coroutine End: $result")
        }

        override val context = EmptyCoroutineContext
    })

    println(continuation)

    val continuation1 = suspend {
        println("In Coroutine.")
        5
    }.startCoroutine(object : Continuation<Int> {
        override val context: CoroutineContext
            get() = EmptyCoroutineContext

        override fun resumeWith(result: Result<Int>) {
            println("Coroutine End: $result")
        }

    })

    println(continuation1)
}