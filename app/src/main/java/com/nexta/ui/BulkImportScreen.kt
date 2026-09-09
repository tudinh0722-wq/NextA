package com.nexta.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nexta.data.model.AlarmSettings
import com.nexta.data.model.Event

private const val NEXTA_PROMPT = """Đọc ảnh thời khóa biểu và chuyển tất cả sự kiện thành dữ liệu NextA.

Mỗi sự kiện dùng đúng 6 dòng sau:
NGÀY: dd/MM/yyyy
TÊN: ...
BẮT ĐẦU: HH:mm
KẾT THÚC: HH:mm
ĐỊA ĐIỂM: ...
GHI CHÚ: ...

Quy tắc:
1. Mỗi sự kiện bắt đầu bằng NGÀY:.
2. Không bỏ sót và không tự thêm thông tin không có trong ảnh.
3. Không đọc được trường nào thì để trống trường đó.
4. Giữ nguyên tên môn học, địa điểm và ghi chú theo ảnh.
5. Ngày dùng dd/MM/yyyy, giờ dùng HH:mm.
6. Chỉ trả về các dòng dữ liệu ở trên, không giải thích, không markdown.

Ví dụ:
NGÀY: 15/09/2026
TÊN: Toán rời rạc
BẮT ĐẦU: 08:00
KẾT THÚC: 09:30
ĐỊA ĐIỂM: A101
GHI CHÚ: ..."""

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BulkImportScreen(onBack: () -> Unit, onImport: (List<Event>, AlarmSettings) -> Unit) {
    var text by remember { mutableStateOf("") }
    var rows by remember { mutableStateOf(emptyList<ImportRow>()) }
    var showPrompt by remember { mutableStateOf(false) }
    var alarmEnabled by remember { mutableStateOf(true) }
    var leadTime by remember { mutableIntStateOf(15) }
    var repeatEnabled by remember { mutableStateOf(true) }
    var repeatInterval by remember { mutableIntStateOf(5) }
    var maxRepeats by remember { mutableIntStateOf(3) }
    val context = LocalContext.current
    val validEvents = rows.mapNotNull { it.event }
    val invalidCount = rows.count { it.event == null }

    fun copyPrompt() {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("NextA prompt", NEXTA_PROMPT))
    }

    val alarmSettings = AlarmSettings(
        enabled = alarmEnabled,
        leadTimeMinutes = leadTime,
        repeatEnabled = repeatEnabled,
        repeatIntervalMinutes = repeatInterval,
        maxRepeats = maxRepeats
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Nhập nhiều sự kiện") },
                navigationIcon = { TextButton(onClick = onBack) { Text("Quay lại") } }
            )
        }
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = { showPrompt = true },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Xem mẫu prompt") }
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier.fillMaxWidth().weight(1f),
                label = { Text("Dán dữ liệu từ AI") },
                placeholder = { Text("NGÀY: 15/09/2026\nTÊN: ...") }
            )
            if (rows.isNotEmpty()) {
                Text(
                    "✓ ${validEvents.size} hợp lệ" + if (invalidCount > 0) "   ⚠ $invalidCount lỗi" else "",
                    style = MaterialTheme.typography.labelLarge
                )
                LazyColumn(Modifier.weight(.8f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(rows) { row ->
                        Card(Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(12.dp)) {
                                Text(
                                    row.event?.title ?: "Sự kiện ${row.rawIndex}",
                                    fontWeight = FontWeight.Bold
                                )
                                if (row.event != null) {
                                    Text(
                                        "${row.event.startDateTime} → ${row.event.endDateTime}\n${row.event.location}",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                } else {
                                    Text(
                                        row.errors.joinToString(" • "),
                                        color = MaterialTheme.colorScheme.error,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                        }
                    }
                }
            }
            if (validEvents.isNotEmpty() && invalidCount == 0) {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Nhắc trước cho các sự kiện", fontWeight = FontWeight.SemiBold)
                        Row(Modifier.fillMaxWidth().height(38.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                            Text(if (alarmEnabled) "Có báo" else "Không báo", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                            if (alarmEnabled) AlarmChoice("${leadTime} phút", listOf(5, 10, 15, 30, 60), leadTime) { leadTime = it }
                            Switch(checked = alarmEnabled, onCheckedChange = { alarmEnabled = it }, modifier = Modifier.padding(start = 6.dp))
                        }
                        if (alarmEnabled) {
                            Row(Modifier.fillMaxWidth().height(36.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                                Text("Lặp nếu chưa xác nhận", style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                                if (repeatEnabled) {
                                    AlarmChoice("Mỗi $repeatInterval phút", listOf(5, 10, 15), repeatInterval) { repeatInterval = it }
                                    Spacer(Modifier.width(4.dp))
                                    AlarmChoice("$maxRepeats lần", listOf(1, 2, 3, 4), maxRepeats) { maxRepeats = it }
                                }
                                Switch(checked = repeatEnabled, onCheckedChange = { repeatEnabled = it }, modifier = Modifier.padding(start = 6.dp))
                            }
                        }
                    }
                }
            }
            Button(
                onClick = { rows = BulkImportParser.parse(text) },
                enabled = text.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            ) { Text("Kiểm tra dữ liệu") }
            Button(
                onClick = { onImport(validEvents, alarmSettings) },
                enabled = validEvents.isNotEmpty() && invalidCount == 0,
                modifier = Modifier.fillMaxWidth()
            ) { Text("Thêm ${validEvents.size} sự kiện") }
        }
    }
    if (showPrompt) {
        AlertDialog(
            onDismissRequest = { showPrompt = false },
            title = { Text("Prompt cho AI") },
            text = { Text(NEXTA_PROMPT) },
            confirmButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = { copyPrompt() }) { Text("Copy prompt") }
                    TextButton(onClick = { showPrompt = false }) { Text("Đóng") }
                }
            }
        )
    }
}

@Composable
private fun AlarmChoice(label: String, options: List<Int>, selected: Int, onSelected: (Int) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        OutlinedButton(onClick = { expanded = true }, contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp), modifier = Modifier.height(34.dp)) { Text(label, style = MaterialTheme.typography.labelSmall) }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(if (options == listOf(1, 2, 3, 4)) "$option lần" else "$option phút") },
                    onClick = { onSelected(option); expanded = false }
                )
            }
        }
    }
}
