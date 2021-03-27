package com.zzy.common.util

import android.net.wifi.ScanResult
import android.util.Log
import androidx.annotation.WorkerThread
import com.zzy.common.bean.*
import java.lang.RuntimeException
import java.util.*
import kotlin.math.roundToInt
import kotlin.math.sqrt

/**
 * 帮助处理一些RSSI数据
 * create by zuyuan on 2021/3/13
 */
object WifiRSSIUtil {

    const val INVALID_LEVEL = -999

    private const val TAG = "rssi_util"

    //一个有效点
    private const val ONE_VALID_POINT = Int.MAX_VALUE - 1
    //两个有效点
    private const val TWO_VALID_POINT = Int.MAX_VALUE - 2
    //三个有效点
    private const val THREE_VALID_POINT = Int.MAX_VALUE - 3

    /**
     * 转换rssi指纹库数据
     */
    fun parseDataOfRSSI(bean: RSSITaskBean): List<RSSIPointBean> {
        //转换数据
        val map = mutableMapOf<String, MutableList<RSSIData>>()
        bean.rssi_data.forEach {
            val xyName = "${it.x}*${it.y}"
            if (map[xyName] == null) {
                map[xyName] = mutableListOf()
            }
            val list = map[xyName]!!
            list.add(it)
        }
        val beans = mutableListOf<RSSIPointBean>()
        map.forEach { entry ->
            val oneBean = entry.value[0]
            val rssiPointBean =
                    RSSIPointBean(oneBean.x, oneBean.y)
            entry.value.forEach { data ->
                var level = 0
                var count = 0
                data.levels.forEach {
                    //抛去无效值
                    if (it != INVALID_LEVEL) {
                        level += it
                        ++count
                    }
                }
                if (count == 0) {
                    return emptyList()
                }
                level = (level.toFloat() / count).roundToInt()
                val wifiBean = WifiBean(data.wifi_ssid, data.wifi_bssid, level)
                rssiPointBean.wifiList.add(wifiBean)
            }
            beans.add(rssiPointBean)
        }
        return beans
    }

    /**
     * 解析扫描结果，转换数据
     */
    fun parseScanResult(targetWifiList: List<WifiTag>, data: List<List<ScanResult>>): List<List<WifiBean>> {
        val targetResult: MutableList<List<WifiBean>> = mutableListOf()
        data.forEach { list ->
            val onceList = list.filter { result ->
                targetWifiList.contains(WifiTag(result.SSID, result.BSSID))
            }.sortedBy { result ->
                //根据名字简单排个序
                result.SSID
            }.map { WifiBean.toWifiBean(it)
            }.toMutableList()
            //有的wifi没扫描到
            if (onceList.size != targetWifiList.size) {
                val copyTargetWifiList = targetWifiList.toMutableList()
                onceList.forEach {
                    val tag = WifiTag(it.ssid, it.bssid)
                    if (copyTargetWifiList.contains(tag)) {
                        copyTargetWifiList.remove(tag)
                    }
                }
                copyTargetWifiList.forEach {
                    onceList.add(WifiBean(it.ssid, it.bssid, INVALID_LEVEL))
                }
            }
            targetResult.add(onceList)
        }
        return targetResult
    }

    /**
     * 根据RSSI指纹库和扫描结果，得到当前的坐标
     */
    @WorkerThread
    fun getCurrentXY(rssiPointBeans: List<RSSIPointBean>, scanResult: List<WifiBean>): Pair<Float, Float> {
        val startTime = System.currentTimeMillis()

        var minPointDiffLevel = Int.MAX_VALUE
        var minLevelBean =  RSSIPointBean(0,0)
        val levelMap = mutableMapOf<String, Int>()
        rssiPointBeans.forEach { rssiPointBean ->
            if (scanResult.size != rssiPointBean.wifiList.size) {
                throw RuntimeException()
            }
            var level2 = 0
            scanResult.forEach { wifiBean ->
                val findBean = rssiPointBean.findWifiBeanNoNull(wifiBean.bssid)
                //无效值不计算
                if (wifiBean.level != INVALID_LEVEL) {
                    val diffLevel = findBean.level - wifiBean.level
                    level2 += (diffLevel * diffLevel)
                }
            }
            levelMap[getXYKey(rssiPointBean.x, rssiPointBean.y)] = level2
            if (minPointDiffLevel >= level2) {
                minPointDiffLevel = level2
                minLevelBean = rssiPointBean
            }
        }
        Log.d(TAG, "min diff = ${minPointDiffLevel}, min xy = $minLevelBean")

        //按照5个wifi, 每个差值在3，=45，认为在当前点
        if (minPointDiffLevel < 45) {
            return Pair(minLevelBean.x.toFloat(), minLevelBean.y.toFloat())
        }

        //找到了rssi diff最小坐标，那么它的周围可以形成四个矩形
        //例如rssi diff最小坐标(0,0)，可形成四个矩形(如下)，真实坐标一定在其中一个矩形以内
        /**
            (0,0)  (1,0)   (1,1)    (0,1)
            (0,0)  (-1,0)  (-1,1)   (0,1)
            (0,0)  (1,0)   (1,-1)   (0,-1)
            (0,0)  (-1,0)  (-1,-1)  (0,-1)
        **/

        //转map，方便拿出
        val rssiMap = mutableMapOf<String, RSSIPointBean>()
        rssiPointBeans.forEach {
            rssiMap[getXYKey(it.x, it.y)] = it
        }

        val minX = minLevelBean.x
        val minY = minLevelBean.y

        //使用三个点即可，因为有一个是相同的
        //左上矩形
        val leftTopRect = getRect(rssiMap, Pair(minX - 1, minY), Pair(minX - 1, minY + 1), Pair(minX, minY + 1))
        //右上矩形
        val rightTopRect = getRect(rssiMap, Pair(minX + 1, minY), Pair(minX + 1, minY + 1), Pair(minX, minY + 1))
        //左下矩形
        val leftBottomRect = getRect(rssiMap, Pair(minX - 1, minY), Pair(minX - 1, minY - 1), Pair(minX, minY - 1))
        //右下矩形
        val rightBottomRect = getRect(rssiMap, Pair(minX + 1, minY), Pair(minX + 1, minY - 1), Pair(minX, minY - 1))

        val leftTopRectPair = getAndFillRectLevel(leftTopRect, levelMap)
        val rightTopRectPair = getAndFillRectLevel(rightTopRect, levelMap)
        val leftBottomRectPair = getAndFillRectLevel(leftBottomRect, levelMap)
        val rightBottomRectPair = getAndFillRectLevel(rightBottomRect, levelMap)

        var targetRect: Array<RectPointBean>? = null
        var minRectDiffLevel = Int.MAX_VALUE
        //三个有效点和两个有效点具有横向可比性，查找diff level最小的
        if (leftTopRectPair.first == THREE_VALID_POINT || leftTopRectPair.first == TWO_VALID_POINT) {
            minRectDiffLevel = leftTopRectPair.second
            targetRect = leftTopRect
        }
        if (leftBottomRectPair.first == THREE_VALID_POINT || leftBottomRectPair.first == TWO_VALID_POINT) {
            if (minRectDiffLevel > leftBottomRectPair.second) {
                minRectDiffLevel = leftBottomRectPair.second
                targetRect = leftBottomRect
            }
        }
        if (rightTopRectPair.first == THREE_VALID_POINT || rightTopRectPair.first == TWO_VALID_POINT) {
            if (minRectDiffLevel > rightTopRectPair.second) {
                minRectDiffLevel = rightTopRectPair.second
                targetRect = rightTopRect
            }
        }
        if (rightBottomRectPair.first == THREE_VALID_POINT || rightBottomRectPair.first == TWO_VALID_POINT) {
            if (minRectDiffLevel > rightBottomRectPair.second) {
                minRectDiffLevel = rightBottomRectPair.second
                targetRect = rightBottomRect
            }
        }
        if (targetRect == null) {
            //剩下就是一个有效点，找diff level最小的即可
            targetRect = leftTopRect
            minRectDiffLevel = leftTopRectPair.second
            if (minRectDiffLevel > leftBottomRectPair.second) {
                minRectDiffLevel = leftBottomRectPair.second
                targetRect = leftBottomRect
            }
            if (minRectDiffLevel > rightTopRectPair.second) {
                minRectDiffLevel = rightTopRectPair.second
                targetRect = rightTopRect
            }
            if (minRectDiffLevel > rightBottomRectPair.second) {
                //minDiffLevel = rightBottomRectPair.second
                targetRect = rightBottomRect
            }
        }

        if (targetRect.size != 3) error("size != 3")
        Log.d(TAG, "targetRect = ${Arrays.toString(targetRect)}")

        //找到了想要的矩形，将diff level最小的点填写进去
        val targetRect4 = arrayOf(targetRect[0], targetRect[1], targetRect[2], RectPointBean(minLevelBean.x, minLevelBean.y, minLevelBean, minPointDiffLevel))
        var allDiffLevel = 0f
        var x = 0f
        var y = 0f
        //diff level越小，占比应该越大
        targetRect4.forEach {
            if (it.rssiPointBean != null) {
                allDiffLevel += it.getSquareRootLevel()
            }
        }
        var allDiffLevelB = 0f
        targetRect4.forEach {
            if (it.rssiPointBean != null) {
                allDiffLevelB += allDiffLevel / it.getSquareRootLevel()
            }
        }
        targetRect4.forEach {
            if (it.rssiPointBean != null) {
                val scale = allDiffLevel / it.getSquareRootLevel() / allDiffLevelB
                x += (scale * it.x)
                y += (scale * it.y)
            }
        }

        Log.d(TAG, "result: x=$x, y=$y")

        val diffTime = System.currentTimeMillis() - startTime
        Log.d(TAG, "getCurrentXY() duration = ${diffTime}ms" )
        return Pair((x * 100).roundToInt() / 100f, (y * 100).roundToInt() / 100f)
    }

    /**
     * 检查速度是否正常
     */
    fun checkNormalSpeed(x: Float, y: Float, preX: Float, preY: Float, unit: Int, preMillTime: Long): Pair<Float, Float> {
        val length = sqrt((preY - y) * (preY - y) + (preX - x) * (preX - x)) * unit
        //多少cm/ms
        val speed = length / (System.currentTimeMillis() - preMillTime)
        //20km/h = 0.5556cm/s
        val maxSpeed = 0.5556f
        return if (speed > maxSpeed) {
            //超速了
            val maxX = preX + (x - preX) * maxSpeed / speed
            val maxY = preY + (x - preY) * maxSpeed / speed
            Log.d(TAG, "超速 $speed, max=$maxSpeed")
            Pair(maxX, maxY)
        } else {
            Pair(x, y)
        }
    }

    /**
     * 得到一个矩形
     */
    private fun getRect(map: Map<String, RSSIPointBean>, vararg xy: Pair<Int, Int>): Array<RectPointBean> {
        val array = Array<RectPointBean?>(xy.size) { null }
        xy.forEachIndexed { index, pair ->
            array[index] = RectPointBean(pair.first, pair.second, map[getXYKey(pair.first, pair.second)])
        }
        return array.requireNoNulls()
    }

    private fun getXYKey(x: Int, y: Int): String = "$x*$y"

    /**
     * 填充矩形所有的点的level
     * @return first 有效个数 second level平方和
     */
    private fun getAndFillRectLevel(rect: Array<RectPointBean>, levelMap: Map<String, Int>): Pair<Int, Int> {
        var target = 0
        var max = Int.MAX_VALUE
        var validCount = Int.MAX_VALUE
        rect.forEach {
            if (it.rssiPointBean != null) {
                val level2 = levelMap[getXYKey(it.x, it.y)] ?: error("level2 = null !!!")
                it.level2 = level2
                target += level2
                --validCount
                if (max > level2) {
                    max = level2
                }
            }
        }
        //没有这个点的话，就拿level2最大值来填充
        rect.forEach {
            if (it.rssiPointBean == null) {
                it.level2 = max
            }
            target += max
        }
        return Pair(validCount, target)
    }

    class RectPointBean(val x: Int, val y: Int, val rssiPointBean: RSSIPointBean?, var level2: Int = 0) {

        fun getSquareRootLevel() : Float  {
            //return sqrt(level2.toFloat())
            //应该扩大邻近点的优势
            return level2.toFloat()
        }

        override fun toString(): String = "(x=${x},y=${y},level2=$level2)"
    }
}