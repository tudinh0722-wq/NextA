package com.nexta.data.sample

import com.nexta.data.model.Event
import com.nexta.data.model.EventType
import java.time.LocalDateTime

object SampleEvents {

    val events = listOf(

        Event(
            id = "sample-001",
            title = "Thiết kế phần mềm",
            type = EventType.CLASS_OFFLINE,
            startDateTime = LocalDateTime.of(2026, 9, 7, 9, 40),
            endDateTime = LocalDateTime.of(2026, 9, 7, 11, 25),
            location = "302 - A9 - Cơ sở 1 - Khu A",
            note = "",
            priority = 0
        ),

        Event(
            id = "sample-002",
            title = "Tiếng Anh Công nghệ thông tin 1",
            type = EventType.CLASS_OFFLINE,
            startDateTime = LocalDateTime.of(2026, 9, 7, 12, 30),
            endDateTime = LocalDateTime.of(2026, 9, 7, 14, 10),
            location = "308 - A9 - Cơ sở 1 - Khu A",
            note = "",
            priority = 0
        ),

        Event(
            id = "sample-003",
            title = "Phát triển ứng dụng thương mại điện tử",
            type = EventType.CLASS_OFFLINE,
            startDateTime = LocalDateTime.of(2026, 9, 8, 15, 10),
            endDateTime = LocalDateTime.of(2026, 9, 8, 17, 45),
            location = "402 - A9 - Cơ sở 1 - Khu A",
            note = "",
            priority = 0
        ),

        Event(
            id = "sample-004",
            title = "Cơ sở dữ liệu",
            type = EventType.CLASS_OFFLINE,
            startDateTime = LocalDateTime.of(2026, 9, 9, 8, 45),
            endDateTime = LocalDateTime.of(2026, 9, 9, 10, 30),
            location = "205 - A9 - Cơ sở 1 - Khu A",
            note = "",
            priority = 0
        ),

        Event(
            id = "sample-005",
            title = "Tiếng Anh Công nghệ thông tin 1",
            type = EventType.CLASS_OFFLINE,
            startDateTime = LocalDateTime.of(2026, 9, 10, 12, 30),
            endDateTime = LocalDateTime.of(2026, 9, 10, 14, 10),
            location = "308 - A9 - Cơ sở 1 - Khu A",
            note = "",
            priority = 0
        ),

        Event(
            id = "sample-006",
            title = "Kiểm thử phần mềm",
            type = EventType.CLASS_ONLINE,
            startDateTime = LocalDateTime.of(2026, 9, 12, 7, 0),
            endDateTime = LocalDateTime.of(2026, 9, 12, 9, 35),
            location = "Khu A_PH Online 05 - Khu A_Online - Cơ sở 1 - Khu A",
            note = "",
            priority = 0
        ),

        Event(
            id = "sample-007",
            title = "Thiết kế phần mềm",
            type = EventType.CLASS_OFFLINE,
            startDateTime = LocalDateTime.of(2026, 9, 14, 9, 40),
            endDateTime = LocalDateTime.of(2026, 9, 14, 11, 25),
            location = "302 - A9 - Cơ sở 1 - Khu A",
            note = "",
            priority = 0
        )
    )
}