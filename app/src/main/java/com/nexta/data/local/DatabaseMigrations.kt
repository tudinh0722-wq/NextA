package com.nexta.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object DatabaseMigrations {

    /**
     * v1 → v2: schema v1 dùng startTime/endTime/occurrencesJson/notifyBeforeMinutes —
     * khác hoàn toàn với schema hiện tại. Không thể ALTER, phải drop và tạo lại.
     * Data cũ bị mất (chấp nhận được vì chưa release chính thức).
     */
    val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("DROP TABLE IF EXISTS events")
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS events (
                    id TEXT NOT NULL PRIMARY KEY,
                    title TEXT NOT NULL,
                    type TEXT NOT NULL,
                    startDateTime TEXT NOT NULL,
                    endDateTime TEXT NOT NULL,
                    location TEXT NOT NULL,
                    note TEXT NOT NULL,
                    priority INTEGER NOT NULL DEFAULT 0
                )
                """.trimIndent()
            )
        }
    }

    /**
     * v2 → v3: tạo bảng event_alarms.
     */
    val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS event_alarms (
                    eventId TEXT NOT NULL,
                    enabled INTEGER NOT NULL,
                    leadTimeMinutes INTEGER NOT NULL,
                    repeatEnabled INTEGER NOT NULL,
                    repeatIntervalMinutes INTEGER NOT NULL,
                    maxRepeats INTEGER NOT NULL,
                    acknowledged INTEGER NOT NULL,
                    PRIMARY KEY(eventId),
                    FOREIGN KEY(eventId) REFERENCES events(id) ON UPDATE CASCADE ON DELETE CASCADE
                )
                """.trimIndent()
            )
            db.execSQL(
                "CREATE UNIQUE INDEX IF NOT EXISTS index_event_alarms_eventId ON event_alarms(eventId)"
            )
        }
    }

    /**
     * v1 → v3: shortcut — drop schema cũ, tạo cả hai table mới trong 1 bước.
     */
    val MIGRATION_1_3 = object : Migration(1, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("DROP TABLE IF EXISTS events")
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS events (
                    id TEXT NOT NULL PRIMARY KEY,
                    title TEXT NOT NULL,
                    type TEXT NOT NULL,
                    startDateTime TEXT NOT NULL,
                    endDateTime TEXT NOT NULL,
                    location TEXT NOT NULL,
                    note TEXT NOT NULL,
                    priority INTEGER NOT NULL DEFAULT 0
                )
                """.trimIndent()
            )
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS event_alarms (
                    eventId TEXT NOT NULL,
                    enabled INTEGER NOT NULL,
                    leadTimeMinutes INTEGER NOT NULL,
                    repeatEnabled INTEGER NOT NULL,
                    repeatIntervalMinutes INTEGER NOT NULL,
                    maxRepeats INTEGER NOT NULL,
                    acknowledged INTEGER NOT NULL,
                    PRIMARY KEY(eventId),
                    FOREIGN KEY(eventId) REFERENCES events(id) ON UPDATE CASCADE ON DELETE CASCADE
                )
                """.trimIndent()
            )
            db.execSQL(
                "CREATE UNIQUE INDEX IF NOT EXISTS index_event_alarms_eventId ON event_alarms(eventId)"
            )
        }
    }
}
