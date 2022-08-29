package com.tongji.basic

import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.coroutines.*


val delayExecutor = Executors.newScheduledThreadPool(1)

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

    println(continuation.resume(Unit))

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

    callLaunchCoroutine()
}

suspend fun delay(delay: Long) = suspendCoroutine {
    delayExecutor.schedule({
        it.resume(Unit)
    }, delay, TimeUnit.MILLISECONDS)
}

fun <R, T> launchCoroutine(receiver: R, block: suspend R.() -> T) {
    block.startCoroutine(receiver, object : Continuation<T> {
        override val context: CoroutineContext
            get() = EmptyCoroutineContext

        override fun resumeWith(result: Result<T>) {
            println("Coroutine End: $result")
        }
    })
}

// 定义一个协程作用域
@RestrictsSuspension
class ProducerScope<T> {
    fun produce(value: T) {
        println("produce $value")
    }
}

fun callLaunchCoroutine(){
    launchCoroutine(ProducerScope<Int>()) {
        println("In Coroutine.")
        // 可以直接使用作用域内的函数 produce()
        produce(1024)
        // delay(1000) // 在注解的作用下无法调用外部的挂起函数
        produce(2048)
    }
}