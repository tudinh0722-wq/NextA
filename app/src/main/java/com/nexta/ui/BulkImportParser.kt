package com.nexta.ui

import com.nexta.data.model.Event
import com.nexta.data.model.EventType
import java.text.Normalizer
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.UUID

data class ImportRow(val event: Event?, val errors: List<String>, val rawIndex: Int)

object BulkImportParser {
    private val dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
    private const val MAX_TITLE = 47
    private const val MAX_LOCATION = 30
    private const val MAX_NOTE = 30

    fun parse(input: String): List<ImportRow> {
        val cleaned = input
            .replace(Regex("```(?:text)?"), "")
            .trim()

        // Keep NGÀY: inside each block. The old split() removed the delimiter,
        // which made every parsed event fail with "Thiếu ngày".
        val blocks = Regex(
            "(?ims)^\\s*NGÀY\\s*:.*?(?=^\\s*NGÀY\\s*:|\\z)"
        ).findAll(cleaned).map { it.value.trim() }.toList()

        return blocks.mapIndexed { index, block -> parseBlock(block, index + 1) }
    }

    private fun parseBlock(block: String, index: Int): ImportRow {
        val fields = mutableMapOf<String, String>()
        block.lineSequence().forEach { line ->
            Regex("^\\s*([^:]+)\\s*:\\s*(.*)$").find(line)?.let {
                fields[normalizeKey(it.groupValues[1])] = it.groupValues[2].trim()
            }
        }

        val errors = mutableListOf<String>()
        val date = fields["NGAY"].orEmpty()
        val title = fields["TEN"].orEmpty()
        val start = fields["BATDAU"].orEmpty()
        val end = fields["KETTHUC"].orEmpty()

        if (date.isBlank()) errors += "Thiếu ngày"
        if (title.isBlank()) errors += "Thiếu tên"
        if (start.isBlank()) errors += "Thiếu giờ bắt đầu"
        if (end.isBlank()) errors += "Thiếu giờ kết thúc"
        if (title.length > MAX_TITLE) errors += "Tên vượt 47 ký tự"
        if (fields["DIADIEM"].orEmpty().length > MAX_LOCATION) errors += "Địa điểm vượt 30 ký tự"
        if (fields["GHICHU"].orEmpty().length > MAX_NOTE) errors += "Ghi chú vượt 30 ký tự"

        try {
            val d = LocalDate.parse(date, dateFormatter)
            val s = LocalDateTime.of(d, parseTime(start))
            val e = LocalDateTime.of(d, parseTime(end))

            if (!e.isAfter(s)) errors += "Giờ kết thúc phải sau giờ bắt đầu"
            if (errors.isNotEmpty()) return ImportRow(null, errors, index)

            return ImportRow(
                Event(
                    UUID.randomUUID().toString(),
                    title,
                    EventType.CLASS_OFFLINE,
                    s,
                    e,
                    fields["DIADIEM"].orEmpty(),
                    fields["GHICHU"].orEmpty(),
                    0
                ),
                emptyList(),
                index
            )
        } catch (_: DateTimeParseException) {
            errors += "Ngày/giờ không đúng định dạng"
        } catch (_: Exception) {
            errors += "Ngày/giờ không hợp lệ"
        }

        return ImportRow(null, errors.distinct(), index)
    }

    private fun parseTime(value: String): LocalTime {
        val normalized = value.trim().lowercase().replace('h', ':')
        val parts = normalized.split(':')
        return if (parts.size == 1) {
            LocalTime.parse(normalized)
        } else {
            LocalTime.of(parts[0].toInt(), parts[1].toInt())
        }
    }

    private fun normalizeKey(value: String): String = Normalizer
        .normalize(value.trim().uppercase(), Normalizer.Form.NFD)
        .replace(Regex("\\p{Mn}+"), "")
        .replace("Đ", "D")
        .replace(" ", "")
}
