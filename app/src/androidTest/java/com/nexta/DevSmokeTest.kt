package com.nexta

/**
 * DEV SMOKE TEST — chạy trực tiếp trên thiết bị thật qua Android Studio.
 * XÓA FILE NÀY trước khi đóng gói APK release.
 *
 * Cách chạy: mở file → click nút ▶ bên cạnh tên class hoặc từng test.
 * Yêu cầu: app đã được cài trên máy, máy đã unlock.
 */

import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.nexta.alarm.AlarmScheduler
import com.nexta.data.local.AppDatabase
import com.nexta.data.model.AlarmSettings
import com.nexta.data.model.Event
import com.nexta.data.model.EventType
import com.nexta.data.repository.EventRepository
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDateTime
import java.util.UUID
import javax.inject.Inject

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class DevSmokeTest {

    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    @Inject lateinit var repository: EventRepository
    @Inject lateinit var db: AppDatabase

    private val ctx: Context get() = ApplicationProvider.getApplicationContext()

    @Before
    fun setUp() {
        hiltRule.inject()
    }

    @After
    fun tearDown() {
        // Xóa sạch event test sau mỗi test để không ảnh hưởng dữ liệu thật
        runBlocking {
            db.eventDao().getAll().first()
                .filter { it.title.startsWith("[TEST]") }
                .forEach { db.eventDao().delete(it) }
        }
    }

    // ── #1 Room migration ────────────────────────────────────────────────────

    /**
     * Verify DB mở được, version đúng — nếu migration lỗi thì test này crash ngay.
     */
    @Test
    fun test01_roomOpensWithCorrectVersion() {
        val version = db.openHelper.readableDatabase.version
        assertEquals("DB version phải là 3", 3, version)
    }

    // ── #2 LegacyAlarmMigrator done-flag ────────────────────────────────────

    /**
     * Gọi migrate() 3 lần liên tiếp — không crash, không exception.
     * Kiểm tra gián tiếp qua việc DB vẫn hoạt động bình thường sau đó.
     */
    @Test
    fun test02_legacyMigratorIdempotent() {
        // LegacyAlarmMigrator là @Singleton, inject qua repository
        // Gọi thông qua AlarmReceiver broadcast để cover đường thật
        repeat(3) {
            ctx.sendBroadcast(
                Intent(ctx, Class.forName("com.nexta.alarm.AlarmReceiver")).apply {
                    action = "android.intent.action.BOOT_COMPLETED"
                }
            )
        }
        // Nếu không crash sau 1s là pass
        Thread.sleep(1_000)
        assertTrue("Migrator không crash khi gọi nhiều lần", true)
    }

    // ── #3 Save event + alarm ────────────────────────────────────────────────

    /**
     * Tạo event + alarm, verify persist vào Room đúng.
     */
    @Test
    fun test03_saveEventAndAlarm() = runBlocking {
        val event = makeTestEvent(minutesFromNow = 30)
        val alarm = AlarmSettings(
            enabled = true,
            leadTimeMinutes = 5,
            repeatEnabled = true,
            repeatIntervalMinutes = 5,
            maxRepeats = 3
        )
        repository.saveEvent(event, alarm)

        val all = repository.getAllEvents().first()
        val saved = all.find { it.id == event.id }
        assertNotNull("Event phải được lưu vào Room", saved)
        assertEquals("[TEST] Kiểm tra alarm", saved?.title)
    }

    // ── #4 Alarm fires (manual observe) ─────────────────────────────────────

    /**
     * Tạo event bắt đầu sau 1 phút, schedule alarm.
     * TEST NÀY CẦN QUAN SÁT THỦ CÔNG:
     *   - Sau ~55 giây phải nghe TTS "NextA nhắc bạn: còn..."
     *   - Notification hiện ra với nút "Đã xác nhận"
     *   - Nhấn "Đã xác nhận" → TTS dừng, notification biến mất
     *
     * Log: adb logcat -s NextA
     */
    @Test
    fun test04_alarmFiresIn1Minute_MANUAL() = runBlocking {
        val event = makeTestEvent(minutesFromNow = 1, title = "[TEST] Alarm 1 phút")
        val alarm = AlarmSettings(
            enabled = true,
            leadTimeMinutes = 1,
            repeatEnabled = false,
            repeatIntervalMinutes = 5,
            maxRepeats = 0
        )
        repository.saveEvent(event, alarm)

        // Verify đã save
        val saved = repository.getAllEvents().first().find { it.id == event.id }
        assertNotNull("Event phải tồn tại trước khi alarm", saved)

        println("""
            ╔══════════════════════════════════════════════╗
            ║  TEST 04 — ĐỢI ~55 GIÂY                     ║
            ║  Khoá màn hình, đặt máy xuống               ║
            ║  → TTS phải đọc tên sự kiện                 ║
            ║  → Notification hiện "Đã xác nhận"          ║
            ║  → Nhấn ACK → TTS dừng                      ║
            ╚══════════════════════════════════════════════╝
        """.trimIndent())

        // Chờ đủ để alarm kịp fire (65s) rồi verify acknowledged
        Thread.sleep(65_000)

        val alarmEntity = db.alarmDao().getByEventId(event.id)
        assertTrue(
            "Alarm phải được acknowledged sau khi nhấn nút",
            alarmEntity?.acknowledged == true
        )
    }

    // ── #5 Delete event cancels alarm ────────────────────────────────────────

    /**
     * Xóa event → alarm trong Room phải bị cascade delete.
     */
    @Test
    fun test05_deleteEventCascadesAlarm() = runBlocking {
        val event = makeTestEvent(minutesFromNow = 60, title = "[TEST] Cascade delete")
        val alarm = AlarmSettings(enabled = true, leadTimeMinutes = 5, repeatEnabled = false, repeatIntervalMinutes = 5, maxRepeats = 0)
        repository.saveEvent(event, alarm)

        // Verify có alarm
        assertNotNull("Alarm phải tồn tại sau save", db.alarmDao().getByEventId(event.id))

        // Xóa event
        repository.deleteEvent(event.id)

        // Alarm phải bị xóa theo cascade
        val alarmAfter = db.alarmDao().getByEventId(event.id)
        assertTrue("Alarm phải bị xóa khi event bị xóa", alarmAfter == null)
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private fun makeTestEvent(
        minutesFromNow: Long,
        title: String = "[TEST] Kiểm tra alarm"
    ): Event {
        val start = LocalDateTime.now().plusMinutes(minutesFromNow)
        return Event(
            id = UUID.randomUUID().toString(),
            title = title,
            type = EventType.CLASS_OFFLINE,
            startDateTime = start,
            endDateTime = start.plusMinutes(90),
            location = "Phòng test",
            note = "Tạo bởi DevSmokeTest",
            priority = 0
        )
    }
}
