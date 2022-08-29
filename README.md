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

熟悉 java 字节码的同学可能知道，这是指这是一个匿名内部类，但这个匿名内部类哪来的呢？我也不知道，当然是学来的~

书中描述是编译器根据我们的协程体，就是那个lambda表达式生成的。这个类继承自**SuspendLambda**类，而这个类又是**Continuation**接口的实现类。通过打印出的东西，我们还可以看到另一个东西，**invokeSuspend**， 这个就可以解释这个Suspend Lambda是如何编译的了，这个函数的实现就是协程体。正是因为调用了协程体的 **resume()** 函数才让协程得以执行。

除此之外，在源码中，还有一组创建和启动的API。这一组API可以为协程体提供一个作用域，在这个作用域内可以直接使用作用域内定义的函数或者状态。

```kotlin
public fun <R, T> (suspend R.() -> T).createCoroutine(
    receiver: R,
    completion: Continuation<T>
): Continuation<Unit> =
    SafeContinuation(createCoroutineUnintercepted(receiver, completion).intercepted(), COROUTINE_SUSPENDED)

public fun <R, T> (suspend R.() -> T).startCoroutine(
    receiver: R,
    completion: Continuation<T>
) {
    createCoroutineUnintercepted(receiver, completion).intercepted().resume(Unit)
}
```

通过对比可以发现，这一组仅仅只多了一个 Receiver 的类型R，这个就是为协程提供作用域的。我们首先来封装一个启动协程的函数。

```kotlin
fun <R, T> launchCoroutine(receiver: R, block: suspend R.() -> T) {
    block.startCoroutine(receiver, object : Continuation<T> {
        override val context: CoroutineContext
            get() = EmptyCoroutineContext

        override fun resumeWith(result: Result<T>) {
            println("Coroutine End: $result")
        }
    })
}
```

然后自定义一个协程作用域。

```kotlin
// 定义一个协程作用域
class ProducerScope<T> {
    fun produce(value: T) {
        println("produce $value")
    }
}
```

最后调用这个函数。

```kotlin
fun callLaunchCoroutine(){
    launchCoroutine(ProducerScope<Int>()) {
        println("In Coroutine.")
        // 可以直接使用作用域内的函数 produce()
        produce(1024)
        delay(1000)
        produce(2048)
    }
}
```

可以看到，我们在 **launchCoroutine()** 中调用了 **ProducerScope** 中的函数，结果如下：

![作用域结果](https://img-blog.csdnimg.cn/60027ccab39a4202bf00bb11d3e040bb.png)

作用域可以用来提供函数支持，当然也可以增加限制。这个时候我们就要用到一个注解 **RestrictsSuspension** ，当作用域在这个注解作用下，在其内部就无法调用外部的挂起函数，就比如 delay()。

![baocuo](https://img-blog.csdnimg.cn/f7a52253da67494da2452f021544d2e2.png)

可以看到，编译器报错了，这个注解在某些特定的场景下可以避免无效甚至危险的挂起函数的调用。当然我还没遇到这样的场景（汗.)

### （2）协程的挂起

我们已经知道在 Kotlin 中使用 **suspend** 关键字修饰的函数叫做挂起函数，而挂起函数只能在其他挂起函数或者协程体内调用。接下里我们来看两段挂起函数：

```kotlin
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
```

这两个都是被 suspend 修饰的挂起函数，但是他们真的都会处于挂起状态吗？答案是否。挂起函数不一定会真的挂起，只有当这个挂起函数处理异步调用时，这个协程才会被挂起。通过编译器我们也可以发现这两个函数哪一个被挂起，哪一个没被挂起。

![ 是否挂起](https://img-blog.csdnimg.cn/0da302ec5a53401aa0b0d76bd6846e80.png)

很明显，suspendFunc01() 没有被挂起。因为它没有出现异步调用，只相当于一个普通函数。

通过前面的介绍我们知道，suspendCoroutine() 函数可以返回一个 Continuation 实例，这部分也就是协程体。在协程的内部挂起函数的调用处被称为挂起点，挂起点如果出现了异步调用，那么当前的协程就会被挂起，直到对应的 Continuation 的 resume 函数被调用才会恢复执行。而 **suspendFunc02()** 挂起点处就有异步调用，所以它是被挂起的。

所以为什么普通函数不能调用挂起函数呢？原因就是普通函数中没有 Continuation 实例，而挂起函数要被挂起 **Continuation 实例是必需的**，编译器能够对这个实例进行正确传递。

### （3）协程的上下文

在前面讲到协程的创建与启动时，可以看到，除了 resumeWith() 函数，还有一个 context，也就是上下文。熟悉安卓开发的肯定对这个都不陌生，它承载了**资源获取、配置管理**等重要工作，在很多控件中，这个 context 都必不可少，那它在协程中有什么用呢？协程的上下文也是大同小异，它的数据结构特征更加的显著，比较类似于List，Map等经典数据结构。先来看看它的主要源码叭~

```kotlin
public interface Key<E : Element>

public interface Element : CoroutineContext {
    
    /**
     * A key of this coroutine context element.
     */
    public val key: Key<*>
    
    // 通过该方法根据 key 返回一个Element(一个CoroutineContext类型的元素)
    public override operator fun <E : Element> get(key: Key<E>): E? =
        @Suppress("UNCHECKED_CAST")
        if (this.key == key) this as E else null
    
    public override fun <R> fold(initial: R, operation: (R, Element) -> R): R =
        operation(initial, this)
    
    public override fun minusKey(key: Key<*>): CoroutineContext =
        if (this.key == key) EmptyCoroutineContext else this
}
```

Element 接口中有一个属性 **key**，这个 key 就是协程上下文这个集合中元素的索引，有点像 List 的索引 index ，但不同的是这个 key “长”在了数据的里面，这就意味着协程上下文一创建就找到了自己的位置。下面我们就来为协程上下文添加一些简单元素。

```kotlin
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
```

这两个就是要添加到上下文中的类，都继承自抽象类 **AbstractCoroutineContextElement()**，而这个类是 Element 类型的。究其根本还是一个 ContinuationContext 对象。第一个类是为协程实现一个名字，第二个类是协程异常处理器的实现。他们是如何添加到上下文中的呢？莫急，且看：

```kotlin
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

        // 将定义好的 continuationContext 赋值给 context
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
```

是不是很像集合元素的添加？没错，就是这么简单。这样我们就知道协程上下文的设置和获取的方法了。当然，上下文不止有这么点作用，通过我对官方文档的调查，协程上下文还包含一个 *协程调度器* （参见 [CoroutineDispatcher](https://kotlin.github.io/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines/-coroutine-dispatcher/index.html)）它确定了相关的协程在哪个线程或哪些线程上执行。协程调度器可以将协程限制在一个特定的线程执行，或将它分派到一个线程池，亦或是让它不受限地运行。

所有的协程构建器诸如 [launch](https://kotlin.github.io/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines/launch.html) 和 [async](https://kotlin.github.io/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines/async.html) 接收一个可选的 [CoroutineContext](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.coroutines/-coroutine-context/) 参数，它可以被用来显式的为一个新协程或其它上下文元素指定一个调度器。这些只作为了解，深入研究还待学习。

### （4）协程的拦截器

在协程上下文的基础上，标准库中还提供了另一个组件，就是拦截器，它允许我们拦截协程异步回调时的恢复调用，简单来说就是在异步回调的过程中为这个回调添加一些东西，比如添加个日志，甚至还可以在这个过程中控制线程的切换。

拦截器其实也是上下文的一种实现，我们定义好一个拦截器之后赋值给 context 就行。

```kotlin
class LogInterceptor : ContinuationInterceptor {
    // 为 key 赋值
    override val key = ContinuationInterceptor

    override fun <T> interceptContinuation(continuation: Continuation<T>)
            = LogContinuation(continuation)
}

class LogContinuation<T>(private val continuation: Continuation<T>)
    : Continuation<T> by continuation {
    override fun resumeWith(result: Result<T>) {
        println("before resumeWith: $result")
        continuation.resumeWith(result)
        println("after resumeWith.")
    }
}

fun main() {
    suspend {
        suspendFunc02("Hello", "Kotlin")
        suspendFunc02("Hello", "Coroutine")
    }.startCoroutine(object : Continuation<Int> {
        // 将拦截器赋值给context
        override val context = LogInterceptor()

        override fun resumeWith(result: Result<Int>) {
            result.getOrThrow()
        }
    })
}
```

