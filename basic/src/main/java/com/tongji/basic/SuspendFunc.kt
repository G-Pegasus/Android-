package com.tongji.basic

import kotlin.concurrent.thread
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

// 挂起函数可以像普通函数一样返回
suspend fun suspendFunc01(a: Int){
    return
}

// 挂起函数也可以处理异步逻辑
suspend fun suspendFunc02(a: String, b: String)
        = suspendCoroutine { continuation ->
    thread {
        continuation.resumeWith(Result.success(5))
    }
}

// 没有被挂起的挂起函数，容易混淆
suspend fun notSuspend() = suspendCoroutine { continuation ->
    continuation.resume(100)
}