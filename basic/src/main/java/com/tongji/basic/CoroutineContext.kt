package com.tongji.basic

import kotlin.coroutines.*

class CoroutineName(private val name: String): AbstractCoroutineContextElement(Key){
    companion object Key: CoroutineContext.Key<CoroutineName>

    override fun toString() = name
}

class CoroutineExceptionHandler(val onErrorAction: (Throwable) -> Unit)
    : AbstractCoroutineContextElement(Key){
    companion object Key: CoroutineContext.Key<CoroutineExceptionHandler>

    fun onError(error: Throwable){
        error.printStackTrace()
        onErrorAction(error)
    }
}

fun main() {
    var coroutineContext: CoroutineContext = EmptyCoroutineContext
    coroutineContext += CoroutineName("test1")
    coroutineContext += CoroutineExceptionHandler {
        println(it)
    }

//    第二种写法
//    coroutineContext += CoroutineName("test1") + CoroutineExceptionHandler {
//        println(it)
//    }

    suspend {
        println("In Coroutine [${coroutineContext[CoroutineName]}].")
        // throw ArithmeticException()
        100
    }.startCoroutine(object : Continuation<Int> {

        override val context = coroutineContext

        override fun resumeWith(result: Result<Int>) {
            result.onFailure {
                context[CoroutineExceptionHandler]?.onError(it)
            }.onSuccess {
                println("Result $it")
            }
        }
    })
}