package com.nexta.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object DatabaseMigrations {

    /**
     * v2 → v3: tạo bảng event_alarms.
     * Đây là migration duy nhất cần giữ — v1 không còn tồn tại ngoài thực tế.
     * Mọi install mới đều bắt đầu từ v3 trở lên.
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
}
