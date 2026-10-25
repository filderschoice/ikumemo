package com.ikumemo.mobile.data.db

import androidx.room.TypeConverter
import java.time.Instant
import java.time.LocalDate

/** 日付は epochDay、日時は epoch ミリ秒で保存する（タイムゾーンに依存しない値で持つため）。 */
class Converters {
    @TypeConverter
    fun localDateToEpochDay(value: LocalDate?): Long? = value?.toEpochDay()

    @TypeConverter
    fun epochDayToLocalDate(value: Long?): LocalDate? = value?.let(LocalDate::ofEpochDay)

    @TypeConverter
    fun instantToEpochMilli(value: Instant?): Long? = value?.toEpochMilli()

    @TypeConverter
    fun epochMilliToInstant(value: Long?): Instant? = value?.let(Instant::ofEpochMilli)

    @TypeConverter
    fun reminderStatusToName(value: ReminderStatus): String = value.name

    @TypeConverter
    fun nameToReminderStatus(value: String): ReminderStatus =
        ReminderStatus.entries.firstOrNull { it.name == value } ?: ReminderStatus.PENDING
}
