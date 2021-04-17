package com.zzy.common.util

import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.lifecycle.Lifecycle
import java.util.concurrent.*

val singleExecutor: ExecutorService by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
    Executors.newSingleThreadExecutor()
}

val ioExecutor: ExecutorService by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
    //多搞点，防止被用完了
    val maxCount = Runtime.getRuntime().availableProcessors() * 3 + 1
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
        Thread(task).apply {
            priority = Thread.MAX_PRIORITY
        }
    }
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

val mainHandler = Handler(Looper.getMainLooper())

fun runOnUIThread(lifecycle: Lifecycle? = null, task: () -> Unit) {
    if (Looper.myLooper() == mainHandler.looper) {
        if (lifecycle == null || LifecycleTask.stateIsSafe(lifecycle)) {
            task.invoke()
        }
    } else {
        val delay = 0L
        postToUIThread(delay, lifecycle, task)
    }
}

fun postToUIThread(delay: Long = 0L, lifecycle: Lifecycle? = null, task: () -> Unit) {
    if (lifecycle == null) {
        mainHandler.postDelayed(task, delay)
    } else {
        mainHandler.postDelayed(LifecycleTask(task, lifecycle), delay)
    }
}

class LifecycleTask(private val task: () -> Unit, private val lifecycle: Lifecycle) : Runnable {

    companion object {

        fun stateIsSafe(lifecycle: Lifecycle) =
            when(lifecycle.currentState) {
                //fragment: onAttach->onCreate->onCreateView->onActivityCreated->onStart->onResume...
                Lifecycle.State.STARTED, Lifecycle.State.RESUMED -> {
                    true
                }
                else -> false
            }
    }

    override fun run() {
        if (stateIsSafe(lifecycle)) {
            task.invoke()
        } else {
            Log.i("LifecycleTask", "current state=${lifecycle.currentState}, state must be STARTED|RESUMED")
        }
    }
}