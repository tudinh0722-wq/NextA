package com.nexta.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object DatabaseMigrations {

    /**
     * v1 → v2: thêm cột priority vào events.
     * (events ban đầu không có priority)
     */
    val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE events ADD COLUMN priority INTEGER NOT NULL DEFAULT 0")
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
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_event_alarms_eventId ON event_alarms(eventId)")
        }
    }

    /**
     * v1 → v3: shortcut cho clean install từ version 1 thẳng lên 3.
     * Tránh Room phải chain 1→2→3 qua 2 bước.
     */
    val MIGRATION_1_3 = object : Migration(1, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE events ADD COLUMN priority INTEGER NOT NULL DEFAULT 0")
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
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_event_alarms_eventId ON event_alarms(eventId)")
        }
    }
}
