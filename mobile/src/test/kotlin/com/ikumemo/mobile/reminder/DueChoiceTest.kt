package com.ikumemo.mobile.reminder

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

/** 期限の候補のチップに添える日付（BL-132）。月・日・曜日を作る処理は純粋な関数で、端末の言語によらない。 */
class DueChoiceTest {
    @Test
    fun datePartsCountFromToday() {
        val today = LocalDate.of(2026, 11, 13) // 金曜日
        assertEquals(DueDateParts(11, 16, "月"), DueChoice.THREE_DAYS.dateParts(today))
        assertEquals(DueDateParts(11, 20, "金"), DueChoice.ONE_WEEK.dateParts(today))
        assertEquals(DueDateParts(11, 27, "金"), DueChoice.TWO_WEEKS.dateParts(today))
        assertEquals(DueDateParts(12, 13, "日"), DueChoice.ONE_MONTH.dateParts(today))
    }

    @Test
    fun datePartsCrossMonthAndYear() {
        // 1 月 30 日の 1 か月後は 2 月の末日（2026 年は 28 日）に丸める
        assertEquals(DueDateParts(2, 28, "土"), DueChoice.ONE_MONTH.dateParts(LocalDate.of(2026, 1, 30)))
        // 年をまたぐ
        assertEquals(DueDateParts(1, 3, "日"), DueChoice.ONE_WEEK.dateParts(LocalDate.of(2026, 12, 27)))
        assertEquals(DueDateParts(2, 2, "月"), DueChoice.THREE_DAYS.dateParts(LocalDate.of(2026, 1, 30)))
    }
}
