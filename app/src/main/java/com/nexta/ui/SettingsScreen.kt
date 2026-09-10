@file:OptIn(ExperimentalMaterial3Api::class)
package com.nexta.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nexta.R

@Composable
fun SettingsScreen(
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Cài đặt", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(painterResource(R.drawable.ic_back), contentDescription = "Back")
                    }
                }
            )
        },
        containerColor = Color.White
    ) { innerPadding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SettingItem("Cá nhân hóa", R.drawable.ic_settings)
            SettingItem("Thông báo", R.drawable.ic_settings)
            SettingItem("Đồng bộ lịch", R.drawable.ic_settings)
            SettingItem("Dữ liệu và bộ nhớ", R.drawable.ic_settings)
            SettingItem("Trợ giúp", R.drawable.ic_settings)
            SettingItem("Về NextA", R.drawable.ic_settings)
        }
    }
}

@Composable
private fun SettingItem(title: String, iconRes: Int) {
    Surface(
        onClick = {},
        shape = MaterialTheme.shapes.medium,
        color = Color(0xFFF7F7F7),
        modifier = Modifier.fillMaxWidth()
    ) {
        ListItem(
            headlineContent = { Text(title) },
            leadingContent = { Icon(painterResource(iconRes), null, Modifier.size(24.dp)) },
            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
        )
    }
}
