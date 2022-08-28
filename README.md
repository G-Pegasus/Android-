# Kotlin协程

## 一、Kotlin协程简介

### 1. 什么是协程？

> **2022.08.28 起 -> 预计于 2022.09.04 结束**
>
> 协程是什么，这个问题在我刚开始学习`Kotlin`的时候其实很迷茫，最初的理解就是可以实现异步的一段程序，在安卓中可以结合`Retrofit`用来网络请求。但很显然，这是非常肤浅的。所以，此篇文章用来深入理解一下**Kotlin**中的协程部分。

其实简单来说，协程最核心的就是函数或者一段程序能够被挂起，稍后可以在挂起的位置恢复。根据维基百科的说法：

> **协程**（英语：coroutine）是计算机程序的一类组件，推广了[协作式多任务](https://zh.m.wikipedia.org/wiki/协作式多任务)的[子例程](https://zh.m.wikipedia.org/wiki/子例程)，允许执行被挂起与被恢复。相对子例程而言，协程更为一般和灵活，但在实践中使用没有子例程那样广泛。协程更适合于用来实现彼此熟悉的程序组件，如[协作式多任务](https://zh.m.wikipedia.org/wiki/协作式多任务)、[异常处理](https://zh.m.wikipedia.org/wiki/异常处理)、[事件循环](https://zh.m.wikipedia.org/wiki/事件循环)、[迭代器](https://zh.m.wikipedia.org/wiki/迭代器)、[无限列表](https://zh.m.wikipedia.org/wiki/惰性求值)和[管道](https://zh.m.wikipedia.org/wiki/管道_(软件))。

协程的挂起和恢复是程序逻辑自己控制的，协程是通过主动挂起出让运行权来实现协作的，所以从本质上来讲讨论协程就是在讨论程序的控制流程。探讨协程最核心的点就是对**挂起**和**恢复**的研究。

### 2. 相比于线程

协程有些类似于线程，但线程是**抢占式多任务**的，而协程是**协作式多任务**的。这两者有什么区别呢？线程一旦开始执行就不能暂停，直到该任务结束，这个过程都是连续的，不存在协作问题。而协程可以实现任务执行流程的协作调度。而且线程的调度需要借助操作系统，由操作系统进行控制，协程则不需要。同线程相比，协程也更加轻量，不太占用系统资源。

### 3. 协程的分类

#### （1）根据是否有调用栈来分类

+ 有栈协程（Stackful Coroutine）：每一个协程都有自己的调用栈，有点类似于线程的调用栈，这种情况下的协程实现很大程度上接近线程，主要的不同体现在调度上。
+ 无栈线程（Stackless Coroutine）：协程没有自己的调用栈，挂起点的状态通过状态机或者闭包等语法实现。

有栈协程的有点是可以在任意函数调用层级的任意位置挂起，并转移调度权，但有栈协程总是会给协程开辟一块栈内存，因此内存开销也大大增加，而无栈协程在内存方面就比较有优势。

那`Kotlin`的协程是否有栈呢？如果我们狭义地认为调用栈就只是类似于线程为函数提供的调用栈的话，Kotlin既然无法在任意层次普通函数调用内实现挂起，那么可以将**Kotlin协程**认为是**无栈协程**。（Kotlin的挂起函数，即suspend关键字声明的函数，只能在协程体内或者其他挂起函数内调用，而不能在普通函数内调用，但挂起函数可以随意调用普通函数）。但如果从挂起函数可以实现任意层次嵌套调用内挂起的效果来讲，也可以将**Kotlin协程**视为一种**有栈协程**的实现。但我们大可不必纠结于Kotlin协程是属于哪一类。

#### （2）根据调度方式分类

+ 对称协程（Symmetric Coroutine）：任何一个协程都是相互独立并且平等的，调度权可以在任意协程之间转移。
+ 非对称协程（Asymmetric Coroutine）：协程出让调度权的目标只能是它的调用者，即协程之间存在调用和被调用关系。

对称协程实际上非常接近线程了，而非对称协程的调用则更接近我们的思维方式，常见语言对协程的实现也基本上都是非对称协程。例如**async/await**，await 时将调度权转移到异步调用中，异步调用返回的结果或抛出异常时总是将调度权转移回 await 的位置。这就是典型的调用于非调用关系。非对称协程在实现上也更加自然，相对容易，而且只要对非对称协程稍作修改就可实现对称协程的能力。

Kotlin 的挂起函数就是非对称协程的例子，调用者与非调用者的关系是固定的，被调用者运行完毕后只能返回到调用者，而不能返回到其他协程。当然，Kotlin 也有自己的对称协程的实现。还是用几张图来理解一下吧：

这是对称协程，每个协程调用之后，都将调度权返回给调度器，各协程间是平等的关系。

![对称协程](https://pic1.zhimg.com/v2-1b5386fdeae8480977e7f9670c819ea0_r.jpg)

这就是非对称协程，只存在调用者和被调用者的关系。

![非对称协程](https://pic3.zhimg.com/80/v2-170ad18468256f3c05afb93895036fae_1440w.jpg)

当然，不管 Kotlin 到底是属于哪一种协程，我们最终都以讨论其函数的挂起和恢复为主。 

## 二、Kotlin协程基础

### 1. 协程的构造

### （1）协程的创建与启动

```kotlin
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
}
```

我们通过`createCoroutine()`来创建了一个协程，点进去看看它的源码是咋写的

```kotlin
public fun <T> (suspend () -> T).createCoroutine(
    completion: Continuation<T>
): Continuation<Unit> =
    SafeContinuation(createCoroutineUnintercepted(completion).intercepted(), COROUTINE_SUSPENDED)
```

+ Receiver 是一个被 suspend 修饰的挂起函数，也是协程的执行体，称他为**协程体**。
+ 参数 completion 会在协程执行完成后调用，实际上就是协程的完成回调。
+ 返回值是一个 Continuation 对象，之后会通过这个值启动该协程。

创建之后，我们在代码中通过调用`startCoroutine()`函数来启动协程

```kotlin
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
```

和创建的过程大差不差，源码也差不多，我们也来分析一下。

```kotlin
public fun <T> (suspend () -> T).startCoroutine(
    completion: Continuation<T>
) {
    createCoroutineUnintercepted(completion).intercepted().resume(Unit)
}
```

可以看到，最后通过 **resume() **函数来启动这个协程。但是为什么通过这个函数就能启动协程呢？由于我们的刨根问底的精神，我们继续深入研究。我们通过 **println(continuation)** 来看看打印出什么。

![结果](https://img-blog.csdnimg.cn/3390acb176d942a482cd59ce24fbcd37.png)

第一行就是打印出的东西，这是个啥？

熟悉 java 字节码中匿名内部类的同学可能知道，这是指这是一个匿名内部类
