package com.serein.day

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
class LunarCalendarTest {
    private fun lunarText(date: String): String {
        val lunar = LunarCalendar.fromSolar(LocalDate.parse(date))
        return lunar.monthName + lunar.dayName
    }

    @Test fun springFestivalAnchors() {
        assertEquals("正月初一", lunarText("2024-02-10"))
        assertEquals("正月初一", lunarText("2025-01-29"))
        assertEquals("正月初一", lunarText("2026-02-17"))
    }

    @Test fun festivals() {
        assertEquals("五月初五", lunarText("2025-05-31"))
        assertEquals("八月十五", lunarText("2025-10-06"))
        assertEquals("八月十五", lunarText("2026-09-25"))
    }

    @Test fun leapMonth() {
        assertEquals(2, LunarCalendar.leapMonthOf(2023))
        assertEquals(6, LunarCalendar.leapMonthOf(2025))
        assertEquals(4, LunarCalendar.leapMonthOf(2020))
        assertEquals(0, LunarCalendar.leapMonthOf(2024))
        val lunar = LunarCalendar.fromSolar(LocalDate.parse("2023-03-22"))
        assertTrue(lunar.isLeapMonth)
        assertEquals("闰二月初一", lunar.monthName + lunar.dayName)
    }

    @Test fun roundTrip() {
        listOf("2024-06-15", "2025-12-31", "2026-09-19", "2023-04-05", "2020-02-29", "2026-02-17", "1912-02-18").forEach { date ->
            val solar = LocalDate.parse(date)
            val lunar = LunarCalendar.fromSolar(solar)
            val back = LunarCalendar.toSolar(lunar.year, lunar.month, lunar.isLeapMonth, lunar.day)
            assertEquals(date, back.toString())
        }
    }

    @Test fun yearGanZhi() {
        assertEquals("甲辰", LunarCalendar.fromSolar(LocalDate.parse("2024-06-01")).yearName.substring(0, 2))
        assertEquals("乙巳", LunarCalendar.fromSolar(LocalDate.parse("2025-06-01")).yearName.substring(0, 2))
        assertEquals("丙午", LunarCalendar.fromSolar(LocalDate.parse("2026-06-01")).yearName.substring(0, 2))
    }

    @Test fun lunarAnniversaryKeepsLunarDate() {
        val anchor = LunarCalendar.fromSolar(LocalDate.parse("2026-09-19"))
        val next = LunarCalendar.solarOfLunarAnniversary(anchor, 2027)!!
        val lunarNext = LunarCalendar.fromSolar(next)
        assertEquals(anchor.month, lunarNext.month)
        assertEquals(anchor.day, lunarNext.day)
        assertEquals(2027, lunarNext.year)
    }
}
