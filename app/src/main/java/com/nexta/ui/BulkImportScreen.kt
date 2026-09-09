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
import com.nexta.data.model.Event

private const val NEXTA_PROMPT = """Hãy đọc ảnh thời khóa biểu tôi cung cấp và chuyển tất cả các sự kiện trong ảnh thành đúng định dạng dưới đây.

QUY TẮC:
- Mỗi sự kiện bắt đầu bằng một dòng NGÀY:
- NGÀY là khóa chính để phân biệt các sự kiện.
- Không bỏ sót sự kiện nào.
- Không tự thêm thông tin không có trong ảnh.
- Nếu không đọc được thông tin, để trống.
- Giữ nguyên tên môn học.
- Ngày dùng định dạng dd/MM/yyyy.
- Giờ dùng định dạng HH:mm.
- Chỉ trả về dữ liệu, không giải thích, không dùng markdown.

MẪU:
NGÀY: dd/MM/yyyy
TÊN: ...
BẮT ĐẦU: HH:mm
KẾT THÚC: HH:mm
ĐỊA ĐIỂM: ...
GHI CHÚ: ..."""

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BulkImportScreen(onBack: () -> Unit, onImport: (List<Event>) -> Unit) {
    var text by remember { mutableStateOf("") }
    var rows by remember { mutableStateOf(emptyList<ImportRow>()) }
    var showPrompt by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val validEvents = rows.mapNotNull { it.event }
    val invalidCount = rows.count { it.event == null }

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
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { showPrompt = true }, modifier = Modifier.weight(1f)) { Text("Xem mẫu") }
                OutlinedButton(onClick = {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(ClipData.newPlainText("NextA prompt", NEXTA_PROMPT))
                }, modifier = Modifier.weight(1f)) { Text("Copy prompt") }
            }
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
            Button(
                onClick = { rows = BulkImportParser.parse(text) },
                enabled = text.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            ) { Text("Kiểm tra dữ liệu") }
            Button(
                onClick = { onImport(validEvents) },
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
            confirmButton = { TextButton(onClick = { showPrompt = false }) { Text("Đóng") } }
        )
    }
}
