package com.serein.day

import android.icu.util.ChineseCalendar
import android.icu.util.TimeZone
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

/** 农历日期：month 为 1-12，isLeapMonth 表示闰月。 */
data class LunarDate(
    val year: Int,
    val month: Int,
    val isLeapMonth: Boolean,
    val day: Int
) {
    val yearName: String
        get() {
            val gan = "甲乙丙丁戊己庚辛壬癸"
            val zhi = "子丑寅卯辰巳午未申酉戌亥"
            val zodiac = "鼠牛虎兔龙蛇马羊猴鸡狗猪"
            val g = (year - 4).mod(10)
            val z = (year - 4).mod(12)
            return "${gan[g]}${zhi[z]}${zodiac[z]}"
        }

    val monthName: String
        get() = (if (isLeapMonth) "闰" else "") + CHINESE_MONTHS[month - 1] + "月"

    val dayName: String
        get() = CHINESE_DAYS[day - 1]

    /** 形如「丙午马年 闰六月初五」。 */
    fun formatFull(): String = "${yearName}年 $monthName$dayName"

    companion object {
        val CHINESE_MONTHS = listOf("正", "二", "三", "四", "五", "六", "七", "八", "九", "十", "冬", "腊")
        val CHINESE_DAYS = listOf(
            "初一", "初二", "初三", "初四", "初五", "初六", "初七", "初八", "初九", "初十",
            "十一", "十二", "十三", "十四", "十五", "十六", "十七", "十八", "十九", "二十",
            "廿一", "廿二", "廿三", "廿四", "廿五", "廿六", "廿七", "廿八", "廿九", "三十"
        )
    }
}

/** 公历 ↔ 农历转换，基于系统 ICU4J 的 ChineseCalendar（UTC 午间取值，避免时区边界）。 */
object LunarCalendar {
    /**
     * 扩展年 → 农历年号的偏移，用已知锚点（2024-06-01 必在甲辰年）动态校准，
     * 以兼容不同 ICU 实现对 EXTENDED_YEAR 的定义差异。
     */
    private val extYearOffset: Int by lazy {
        val cc = ChineseCalendar(TimeZone.getTimeZone("UTC"))
        cc.timeInMillis = LocalDate.of(2024, 6, 1).atTime(12, 0).toInstant(ZoneOffset.UTC).toEpochMilli()
        cc.get(ChineseCalendar.EXTENDED_YEAR) - 2024
    }

    // ICU ChineseCalendar 构造很重；列表行渲染与排序会高频换算同一批日期，进程内缓存换算结果。
    private val fromSolarCache = androidx.collection.LruCache<Long, LunarDate>(512)
    private val toSolarCache = androidx.collection.LruCache<Int, LocalDate>(256)

    private fun atUtc(date: LocalDate): Long = date.atTime(12, 0).toInstant(ZoneOffset.UTC).toEpochMilli()

    private fun newCalendar(): ChineseCalendar = ChineseCalendar(TimeZone.getTimeZone("UTC"))

    /** 公历 → 农历。 */
    fun fromSolar(solar: LocalDate): LunarDate {
        val key = solar.toEpochDay()
        fromSolarCache.get(key)?.let { return it }
        val cc = newCalendar()
        cc.timeInMillis = atUtc(solar)
        val result = LunarDate(
            year = cc.get(ChineseCalendar.EXTENDED_YEAR) - extYearOffset,
            month = cc.get(ChineseCalendar.MONTH) + 1,
            isLeapMonth = cc.get(ChineseCalendar.IS_LEAP_MONTH) == 1,
            day = cc.get(ChineseCalendar.DAY_OF_MONTH)
        )
        fromSolarCache.put(key, result)
        return result
    }

    /** 农历 → 公历。 */
    fun toSolar(year: Int, month: Int, isLeapMonth: Boolean, day: Int): LocalDate {
        val key = toSolarKey(year, month, isLeapMonth, day)
        toSolarCache.get(key)?.let { return it }
        val cc = newCalendar()
        cc.clear()
        cc.set(ChineseCalendar.EXTENDED_YEAR, year + extYearOffset)
        cc.set(ChineseCalendar.MONTH, month - 1)
        cc.set(ChineseCalendar.IS_LEAP_MONTH, if (isLeapMonth) 1 else 0)
        cc.set(ChineseCalendar.DAY_OF_MONTH, day)
        val result = Instant.ofEpochMilli(cc.timeInMillis).atZone(ZoneOffset.UTC).toLocalDate()
        toSolarCache.put(key, result)
        return result
    }

    /** 打包 (year, month, 闰月标记, day) 为缓存键；month+闰位最多 24，day 最多 30。 */
    private fun toSolarKey(year: Int, month: Int, isLeapMonth: Boolean, day: Int): Int =
        ((year - 1900) shl 10) or ((month + (if (isLeapMonth) 12 else 0)) shl 5) or day

    /** 某农历年的闰月月份（1-12），无闰月返回 0。从正月初一逐日扫描，一年至多 ~390 次转换。 */
    fun leapMonthOf(year: Int): Int {
        var d = runCatching { toSolar(year, 1, false, 1) }.getOrNull() ?: return 0
        repeat(400) {
            val lunar = fromSolar(d)
            if (lunar.year > year) return 0
            if (lunar.isLeapMonth && lunar.year == year) return lunar.month
            d = d.plusDays(1)
        }
        return 0
    }

    private fun maxDayOfMonth(year: Int, month: Int): Int {
        val cc = newCalendar()
        cc.clear()
        cc.set(ChineseCalendar.EXTENDED_YEAR, year + extYearOffset)
        cc.set(ChineseCalendar.MONTH, month - 1)
        cc.set(ChineseCalendar.IS_LEAP_MONTH, 0)
        cc.set(ChineseCalendar.DAY_OF_MONTH, 1)
        return cc.getActualMaximum(ChineseCalendar.DAY_OF_MONTH)
    }

    /**
     * 「每年重复」的农历纪念日：year 年该农历月日的公历日期。
     * 闰月生日在无闰年份过平月；三十遇小月退到廿九。
     */
    fun solarOfLunarAnniversary(anchor: LunarDate, year: Int): LocalDate? {
        val day = minOf(anchor.day, maxDayOfMonth(year, anchor.month))
        return runCatching { toSolar(year, anchor.month, false, day) }.getOrNull()
    }
}
