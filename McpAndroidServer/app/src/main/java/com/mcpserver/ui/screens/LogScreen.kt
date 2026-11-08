package com.mcpserver.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mcpserver.ui.viewmodel.LogViewModel

@Composable
fun LogScreen(viewModel: LogViewModel = androidx.lifecycle.viewmodel.compose.viewModel()) {
    val uiState by viewModel.uiState.collectAsState()

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        OutlinedTextField(
            value = uiState.searchQuery,
            onValueChange = { viewModel.setSearch(it) },
            label = { Text("搜索日志") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Spacer(Modifier.height(8.dp))
        LazyColumn(Modifier.weight(1f)) {
            items(uiState.entries) { entry ->
                Card(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Column(Modifier.padding(12.dp)) {
                        Text("${entry.timestamp} [${entry.level}] ${entry.module}", style = MaterialTheme.typography.labelSmall)
                        Text(entry.message, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
        Button(onClick = { viewModel.clearLogs() }, Modifier.fillMaxWidth()) { Text("清空日志") }
    }
}
