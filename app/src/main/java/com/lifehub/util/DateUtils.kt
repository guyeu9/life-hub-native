package com.lifehub.util

import java.text.SimpleDateFormat
import java.util.*

/**
 * 日期工具 — 整个 App 共用
 */

private val sdfDateKey = SimpleDateFormat("yyyy-MM-dd", Locale.CHINA)
private val sdfMonthDay = SimpleDateFormat("MM/dd", Locale.CHINA)
private val sdfFull = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.CHINA)
private val sdfCnDate = SimpleDateFormat("M月d日", Locale.CHINA)

fun dateKey(ts: Long): String = sdfDateKey.format(Date(ts))
fun todayKey(): String = dateKey(System.currentTimeMillis())
fun dayOffsetKey(offset: Int): String {
    val cal = Calendar.getInstance()
    cal.add(Calendar.DAY_OF_YEAR, offset)
    return dateKey(cal.timeInMillis)
}
fun monthDay(ts: Long): String = sdfMonthDay.format(Date(ts))
fun fullTime(ts: Long): String = sdfFull.format(Date(ts))
fun cnDate(ts: Long): String = sdfCnDate.format(Date(ts))

/** 把日期键 yyyy-MM-dd 转成「M月d日」 */
fun cnDateKey(key: String): String {
    val p = key.split("-")
    if (p.size < 3) return key
    return "${p[1].toInt()}月${p[2].toInt()}日"
}

/** 返回本周周一的日期键 yyyy-MM-dd */
fun weekMondayKey(): String {
    val cal = Calendar.getInstance()
    cal.set(Calendar.HOUR_OF_DAY, 0)
    cal.set(Calendar.MINUTE, 0)
    cal.set(Calendar.SECOND, 0)
    cal.set(Calendar.MILLISECOND, 0)
    val w = (cal.get(Calendar.DAY_OF_WEEK) + 5) % 7 // 周一=0 ... 周日=6
    cal.add(Calendar.DAY_OF_YEAR, -w)
    return dateKey(cal.timeInMillis)
}

/** 两个日期键相差的天数（b - a），a<b 为正 */
fun diffDaysKey(a: String, b: String): Int {
    val pa = a.split("-")
    val pb = b.split("-")
    if (pa.size < 3 || pb.size < 3) return 0
    val ca = Calendar.getInstance().apply { set(pa[0].toInt(), pa[1].toInt() - 1, pa[2].toInt(), 0, 0, 0) }
    val cb = Calendar.getInstance().apply { set(pb[0].toInt(), pb[1].toInt() - 1, pb[2].toInt(), 0, 0, 0) }
    return ((cb.timeInMillis - ca.timeInMillis) / 86_400_000L).toInt()
}

/** 当天 0 点时间戳 */
fun startOfToday(): Long {
    val cal = Calendar.getInstance()
    cal.set(Calendar.HOUR_OF_DAY, 0)
    cal.set(Calendar.MINUTE, 0)
    cal.set(Calendar.SECOND, 0)
    cal.set(Calendar.MILLISECOND, 0)
    return cal.timeInMillis
}

/** 当天 23:59:59 */
fun endOfToday(): Long {
    val cal = Calendar.getInstance()
    cal.set(Calendar.HOUR_OF_DAY, 23)
    cal.set(Calendar.MINUTE, 59)
    cal.set(Calendar.SECOND, 59)
    cal.set(Calendar.MILLISECOND, 999)
    return cal.timeInMillis
}

/** 本月 1 号 0 点 */
fun startOfMonth(): Long {
    val cal = Calendar.getInstance()
    cal.set(Calendar.DAY_OF_MONTH, 1)
    cal.set(Calendar.HOUR_OF_DAY, 0)
    cal.set(Calendar.MINUTE, 0)
    cal.set(Calendar.SECOND, 0)
    cal.set(Calendar.MILLISECOND, 0)
    return cal.timeInMillis
}

/** 最近 N 天日期键（含今天），从旧到新 */
fun recentDateKeys(n: Int): List<String> {
    val cal = Calendar.getInstance()
    cal.add(Calendar.DAY_OF_YEAR, -(n - 1))
    val result = mutableListOf<String>()
    for (i in 0 until n) {
        result.add(dateKey(cal.timeInMillis))
        cal.add(Calendar.DAY_OF_YEAR, 1)
    }
    return result
}

fun isOverdue(due: Long, now: Long = System.currentTimeMillis()): Boolean {
    return due > 0 && due < now
}
