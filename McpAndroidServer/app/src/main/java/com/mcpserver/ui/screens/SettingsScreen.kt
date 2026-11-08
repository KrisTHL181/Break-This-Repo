package com.mcpserver.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.mcpserver.ui.viewmodel.SettingsViewModel

private fun copyToClipboard(context: Context, label: String, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return
    clipboard.setPrimaryClip(ClipData.newPlainText(label, text))
}

@Composable
fun SettingsScreen(viewModel: SettingsViewModel = androidx.lifecycle.viewmodel.compose.viewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(Unit) { viewModel.refresh() }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(20.dp)) {
                    Text("服务器", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(12.dp))

                    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                        Text("传输模式")
                        Row {
                            Button(
                                onClick = { viewModel.setUseSse(true) },
                                enabled = !uiState.useSse,
                                modifier = Modifier.padding(end = 8.dp)
                            ) { Text("SSE") }
                            Button(onClick = { viewModel.setUseSse(false) }, enabled = uiState.useSse) { Text("Stdio") }
                        }
                    }

                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = uiState.port,
                        onValueChange = { viewModel.setPort(it) },
                        label = { Text("端口") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(Modifier.height(12.dp))
                    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                        Text("开机自动启动服务")
                        Switch(checked = uiState.autoStart, onCheckedChange = { viewModel.setAutoStart(it) })
                    }
                    Text(
                        if (uiState.serverRunning) "服务运行中，修改端口/传输模式需重启服务生效" else "服务未运行",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }

        item {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(20.dp)) {
                    Text("访问密钥", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    Text("客户端需带 Authorization: Bearer <密钥> 或 ?key=<密钥>", style = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.height(12.dp))

                    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                        Text("启用密钥校验")
                        Switch(checked = uiState.authEnabled, onCheckedChange = { viewModel.setAuthEnabled(it) })
                    }

                    Spacer(Modifier.height(8.dp))
                    Text(
                        if (uiState.apiKey.isBlank()) "尚未生成密钥" else uiState.apiKey,
                        style = MaterialTheme.typography.bodyMedium
                    )

                    Spacer(Modifier.height(12.dp))
                    Row {
                        Button(onClick = { viewModel.generateApiKey() }, modifier = Modifier.padding(end = 8.dp)) {
                            Text(if (uiState.apiKey.isBlank()) "生成密钥" else "重新生成")
                        }
                        OutlinedButton(
                            onClick = { copyToClipboard(context, "MCP API Key", uiState.apiKey) },
                            enabled = uiState.apiKey.isNotBlank(),
                            modifier = Modifier.padding(end = 8.dp)
                        ) { Text("复制") }
                        OutlinedButton(onClick = { viewModel.revokeApiKey() }, enabled = uiState.apiKey.isNotBlank()) {
                            Text("吊销")
                        }
                    }
                }
            }
        }

        item {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(20.dp)) {
                    Text("安全设置", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(12.dp))
                    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                        Text("危险命令拦截")
                        Switch(
                            checked = uiState.blockDangerousCommands,
                            onCheckedChange = { viewModel.setBlockDangerousCommands(it) }
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    Text("权限等级: ${uiState.permissionLevel}")
                    Spacer(Modifier.height(8.dp))
                    Row {
                        listOf("READ_ONLY", "NORMAL", "ELEVATED").forEach { level ->
                            Button(
                                onClick = { viewModel.setPermissionLevel(level) },
                                enabled = uiState.permissionLevel != level,
                                modifier = Modifier.padding(end = 8.dp)
                            ) { Text(level, style = MaterialTheme.typography.labelSmall) }
                        }
                    }
                }
            }
        }

        item {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(20.dp)) {
                    Text("高级设置", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(12.dp))
                    Text("命令超时: ${uiState.commandTimeout.toInt()} s")
                    Slider(
                        value = uiState.commandTimeout,
                        onValueChange = { viewModel.setCommandTimeout(it) },
                        valueRange = 5f..120f,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text("日志级别: ${uiState.logLevel}")
                    Spacer(Modifier.height(8.dp))
                    Row {
                        listOf("DEBUG", "INFO", "WARN", "ERROR").forEach { level ->
                            Button(
                                onClick = { viewModel.setLogLevel(level) },
                                enabled = uiState.logLevel != level,
                                modifier = Modifier.padding(end = 8.dp)
                            ) { Text(level, style = MaterialTheme.typography.labelSmall) }
                        }
                    }
                }
            }
        }
    }
}
