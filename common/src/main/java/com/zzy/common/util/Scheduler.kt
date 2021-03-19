package com.zzy.common.util

import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.OnLifecycleEvent
import java.lang.Exception
import java.lang.ref.WeakReference
import java.util.concurrent.*

val mainHandler = Handler(Looper.getMainLooper())

val singleExecutor: ExecutorService by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
    Executors.newSingleThreadExecutor()
}

val ioExecutor: ExecutorService by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
    val maxCount = Runtime.getRuntime().availableProcessors() * 2 + 1
    ThreadPoolExecutor(2, maxCount, 1000L, TimeUnit.MILLISECONDS, LinkedBlockingQueue()) { task ->
        Thread(task).apply {
            priority = Thread.NORM_PRIORITY
            isDaemon = true
        }
    }
}

val cpuExecutor :ExecutorService by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
    val count = Runtime.getRuntime().availableProcessors() + 1
    ThreadPoolExecutor(count, count, 0L, TimeUnit.MILLISECONDS, LinkedBlockingQueue()) { task ->
        Thread {
            task.run()
        }.apply {
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
    singleExecutor.execute(task)
}

fun ioSync(task: () -> Unit) {
    ioExecutor.execute(task)
}

fun cpuSync(task: () -> Unit) {
    cpuExecutor.execute(task)
}