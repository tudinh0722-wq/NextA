package com.nexta

/**
 * DEV SMOKE TEST — chạy trực tiếp trên thiết bị thật qua Android Studio.
 * XÓA FILE NÀY (hoặc cả folder androidTest/) trước khi đóng gói APK release.
 *
 * Cách chạy: mở file → click ▶ cạnh tên class để chạy tất cả,
 * hoặc click ▶ cạnh từng @Test để chạy riêng.
 *
 * Không dùng Hilt testing — dùng Room trực tiếp để không cần thêm dependency.
 */

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.nexta.data.local.AppDatabase
import com.nexta.data.local.DatabaseMigrations
import com.nexta.data.local.EventAlarmEntity
import com.nexta.data.local.EventEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class DevSmokeTest {

    private lateinit var db: AppDatabase

    @Before
    fun setUp() {
        val ctx: Context = ApplicationProvider.getApplicationContext()
        // Dùng in-memory DB để test không ảnh hưởng dữ liệu thật trên máy
        db = Room.inMemoryDatabaseBuilder(ctx, AppDatabase::class.java)
            .addMigrations(
                DatabaseMigrations.MIGRATION_1_2,
                DatabaseMigrations.MIGRATION_1_3,
                DatabaseMigrations.MIGRATION_2_3,
            )
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() {
        db.close()
    }

    // ── #1 Room migration / version ──────────────────────────────────────────

    /**
     * Verify DB mở được và version đúng.
     * Nếu migration lỗi thì setUp() crash trước khi vào đây.
     */
    @Test
    fun test01_roomVersionIs3() {
        val version = db.openHelper.readableDatabase.version
        assertEquals("DB version phải là 3", 3, version)
    }

    // ── #3 Save event + alarm ────────────────────────────────────────────────

    /**
     * Insert event + alarm, verify đọc lại được.
     */
    @Test
    fun test03_saveEventAndAlarm() = runBlocking {
        val id = UUID.randomUUID().toString()
        db.eventDao().insert(makeEventEntity(id))
        db.alarmDao().upsert(makeAlarmEntity(id))

        val event = db.eventDao().getById(id)
        val alarm = db.alarmDao().getByEventId(id)

        assertNotNull("Event phải tồn tại sau insert", event)
        assertEquals("[TEST] Kiểm tra alarm", event?.title)
        assertNotNull("Alarm phải tồn tại sau insert", alarm)
        assertEquals(5, alarm?.leadTimeMinutes)
    }

    // ── #4 Alarm acknowledge ─────────────────────────────────────────────────

    /**
     * Verify acknowledge() đổi flag trong DB.
     */
    @Test
    fun test04_acknowledgeAlarm() = runBlocking {
        val id = UUID.randomUUID().toString()
        db.eventDao().insert(makeEventEntity(id))
        db.alarmDao().upsert(makeAlarmEntity(id, acknowledged = false))

        db.alarmDao().acknowledge(id)

        val alarm = db.alarmDao().getByEventId(id)
        assertEquals("Alarm phải acknowledged sau khi gọi acknowledge()", true, alarm?.acknowledged)
    }

    // ── #5 Delete event cascades alarm ───────────────────────────────────────

    /**
     * Xóa event → alarm phải bị cascade delete theo foreign key.
     */
    @Test
    fun test05_deleteEventCascadesAlarm() = runBlocking {
        val id = UUID.randomUUID().toString()
        db.eventDao().insert(makeEventEntity(id, title = "[TEST] Cascade delete"))
        db.alarmDao().upsert(makeAlarmEntity(id))

        assertNotNull("Alarm phải tồn tại trước khi xóa event", db.alarmDao().getByEventId(id))

        db.eventDao().delete(id)

        assertNull("Alarm phải bị xóa theo cascade khi event bị xóa", db.alarmDao().getByEventId(id))
    }

    // ── #6 getAll flow ───────────────────────────────────────────────────────

    /**
     * Verify Flow từ getAll() emit đúng số event.
     */
    @Test
    fun test06_getAllEventsFlow() = runBlocking {
        val id1 = UUID.randomUUID().toString()
        val id2 = UUID.randomUUID().toString()
        db.eventDao().insert(makeEventEntity(id1, title = "[TEST] Event 1"))
        db.eventDao().insert(makeEventEntity(id2, title = "[TEST] Event 2"))

        val all = db.eventDao().getAll().first()
        assertEquals("Phải có đúng 2 event", 2, all.size)
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private fun makeEventEntity(
        id: String,
        title: String = "[TEST] Kiểm tra alarm"
    ) = EventEntity(
        id = id,
        title = title,
        type = "CLASS_OFFLINE",
        startDateTime = "2026-09-10T12:30:00",
        endDateTime = "2026-09-10T14:10:00",
        location = "Phòng test",
        note = "Tạo bởi DevSmokeTest",
        priority = 0
    )

    private fun makeAlarmEntity(
        eventId: String,
        acknowledged: Boolean = false
    ) = EventAlarmEntity(
        eventId = eventId,
        enabled = true,
        leadTimeMinutes = 5,
        repeatEnabled = true,
        repeatIntervalMinutes = 5,
        maxRepeats = 3,
        acknowledged = acknowledged
    )
}
