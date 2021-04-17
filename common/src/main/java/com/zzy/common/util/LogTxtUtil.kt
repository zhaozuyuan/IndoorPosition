package com.zzy.common.util

import android.annotation.SuppressLint
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.io.OutputStreamWriter
import java.nio.charset.Charset
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

/**
 * create by zuyuan on 2021/4/3
 *
 * 打印日志帮助器，打印到txt中
 */
object LogTxtUtil {

    var openLog = false

    private val txtQueue: Queue<String> = LinkedList<String>()

    private var canSet = AtomicBoolean(true)

    private var curFile: File? = null

    /**
     * 在android 10之前是可以直接使用 /sdcard/ 路径来储存到SD卡根目录
     * Context.getExternalFilesDir(null) 获取当前app的SD卡的储存路径
     * android 11 新安装的app只能使用SD卡下的storage/emulated/0/android/data/packageName目录
     * android 11 可以使用preserveLegacyExternalStorage=true标记在app覆盖安装时使用SD卡下的已有的文件夹
     * android 10 可以使用requestLegacyExternalStorage=true标记直接使用SD卡根目录
     *
     * 这么做的原因:
     * 保护用户隐私和维护app的数据安全
     * storage/emulated/0/android/data/目录是一个安全目录，普通app无法直接访问到其它app的SD卡目录
     */
    @SuppressLint("SdCardPath")
    private val rootDirPath = "${Global.app.getExternalFilesDir(null)!!.absolutePath}/${Global.APP_DIR_NAME}/"

    private const val TAG = "logTxt"

    private val lock: Object = Object()

    private const val TIME_OUT = 2000L

    private var txtLineCount = AtomicInteger(0)

    init {
        Log.d(TAG, "rootDir=$rootDirPath")
        File(rootDirPath).mkdirs()

        //守护线程
        ioSync {
            while (true) {
                synchronized(lock) {
                    lock.wait(TIME_OUT)
                }
                writeQueue()
            }
        }
    }

    fun getTxtLineCount() = txtLineCount.get()

    @SuppressLint("SimpleDateFormat")
    fun saveLine(txt: String): Boolean {
        if (!openLog) {
            return false
        }

        val target = "${getTimeString()} : $txt \n"

        lock()
        txtLineCount.getAndIncrement()
        txtQueue.offer(target)
        if (txtQueue.size >= 5) {
            notifySave()
        }
        unlock()
        return true
    }

    fun forceFlush() {
        notifySave()
    }

    private fun notifySave() {
        synchronized(lock) {
            lock.notify()
        }
    }

    private fun lock() {
        while (!canSet.compareAndSet(true, false)) { }
    }

    private fun unlock() {
        canSet.set(true)
    }

    private fun writeQueue() {
        if (txtQueue.isEmpty()) {
            return
        }

        lock()
        val targetBuilder = StringBuilder()
        while (txtQueue.isNotEmpty()) {
            targetBuilder.append(txtQueue.poll())
        }
        val target = targetBuilder.toString()
        unlock()

        if (curFile == null) {
            curFile = File("$rootDirPath/${getTimeString()}.txt")
            curFile!!.createNewFile()
        }

        OutputStreamWriter(FileOutputStream(curFile, true)).use {
            it.write(target)
        }
    }

    @SuppressLint("SimpleDateFormat")
    private fun getTimeString(): String {
        val formatter = SimpleDateFormat("MM-dd HH:mm:ss")
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = System.currentTimeMillis()
        return formatter.format(calendar.time)
    }
}