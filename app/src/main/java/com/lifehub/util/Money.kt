package com.lifehub.util

import java.util.Locale

/**
 * 金额格式化工具
 */
fun money2(v: Double): String {
    return String.format(Locale.CHINA, "%,.2f", v)
}

fun money0(v: Double): String {
    return String.format(Locale.CHINA, "%,.0f", v)
}

/** 千分位 + 货币符号 */
fun yuan(v: Double): String = "¥${money2(v)}"
