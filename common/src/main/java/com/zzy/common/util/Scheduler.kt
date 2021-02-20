package com.zzy.common.util

import android.os.Handler
import android.os.Looper
import java.util.concurrent.*

val mainHandler = Handler(Looper.getMainLooper())

val singleExecutor: ExecutorService by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
    Executors.newSingleThreadExecutor()
}

val ioExecutor: ExecutorService by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
    val maxCount = Runtime.getRuntime().availableProcessors() * 2 + 1
    ThreadPoolExecutor(1, maxCount, 1000L, TimeUnit.MILLISECONDS, LinkedBlockingQueue()) { task ->
        Thread(task).apply {
            priority = Thread.NORM_PRIORITY
            isDaemon = true
        }
    }
}

val cpuExecutor :ExecutorService by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
    val count = Runtime.getRuntime().availableProcessors() + 1
    ThreadPoolExecutor(count, count, 0L, TimeUnit.MILLISECONDS, LinkedBlockingQueue()) { task ->
        Thread(task).apply {
            priority = Thread.MAX_PRIORITY
        }
    }
}

fun runUIThread(task: () -> Unit) {
    if (Looper.myLooper() == mainHandler.looper) {
        task.invoke()
    } else {
        mainHandler.post(task)
    }
}

fun postUIThread(task: () -> Unit) {
    mainHandler.post(task)
}

fun postUIThreadDelay(delay: Long, task: () -> Unit) {
    mainHandler.postDelayed(task, delay)
}

fun singleExecutor(task: () -> Unit) {
    singleExecutor.submit(task)
}

fun ioSync(task: () -> Unit) {
    ioExecutor.submit(task)
}

fun cpuSync(task: () -> Unit) {
    cpuExecutor.submit(task)
}