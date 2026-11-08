package com.mcpserver.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mcpserver.ui.theme.*
import com.mcpserver.ui.viewmodel.HomeViewModel

@Composable
fun HomeScreen(viewModel: HomeViewModel = androidx.lifecycle.viewmodel.compose.viewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            ServerStatusCard(uiState.serverRunning, uiState.serverAddress) { viewModel.toggleServer() }
        }
        item {
            PermissionStatusCard(
                shizukuState = uiState.shizukuState,
                adbMode = uiState.adbMode,
                adbStatus = uiState.adbStatus,
                onReconnect = { viewModel.reconnectShizuku() },
                onRequestPermission = { viewModel.requestShizukuPermission() },
                onEnableAdb = { viewModel.enableWirelessAdb() },
                onDisableAdb = { viewModel.disableWirelessAdb() },
            )
        }
        item {
            KeepAliveCard { viewModel.requestIgnoreBatteryOptimizations() }
        }
        item {
            DeviceInfoCard(
                uiState.deviceModel,
                uiState.androidVersion,
                uiState.apiLevel,
                uiState.batteryLevel,
                uiState.wifiConnected,
            )
        }
        item { QuickActionsCard(uiState.lastResult) { viewModel.testCommand(it) } }
    }
}

@Composable
private fun ServerStatusCard(running: Boolean, address: String, onToggle: () -> Unit) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                Text("MCP Server", style = MaterialTheme.typography.titleLarge)
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = if (running) StatusGreen else MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(12.dp)
                ) {}
            }
            Spacer(Modifier.height(8.dp))
            Text(if (running) "运行中" else "已停止", color = if (running) StatusGreen else MaterialTheme.colorScheme.error)
            if (running && address.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(address, style = MaterialTheme.typography.bodySmall)
            }
            Spacer(Modifier.height(16.dp))
            Button(onClick = onToggle, Modifier.fillMaxWidth()) {
                Text(if (running) "停止服务" else "启动服务")
            }
        }
    }
}

@Composable
private fun PermissionStatusCard(
    shizukuState: String,
    adbMode: String,
    adbStatus: String,
    onReconnect: () -> Unit,
    onRequestPermission: () -> Unit,
    onEnableAdb: () -> Unit,
    onDisableAdb: () -> Unit,
) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text("权限通道", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) { Text("Shizuku"); Text(shizukuState) }
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) { Text("执行方式"); Text(adbMode) }
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) { Text("无线 ADB"); Text(adbStatus) }
            Spacer(Modifier.height(12.dp))
            Button(onClick = onRequestPermission, Modifier.fillMaxWidth()) { Text("申请 Shizuku 权限") }
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth()) {
                OutlinedButton(onClick = onReconnect, Modifier.weight(1f).padding(end = 8.dp)) { Text("重新检测") }
                OutlinedButton(onClick = onEnableAdb, Modifier.weight(1f).padding(end = 8.dp)) { Text("开 ADB") }
                OutlinedButton(onClick = onDisableAdb, Modifier.weight(1f)) { Text("关 ADB") }
            }
        }
    }
}

@Composable
private fun KeepAliveCard(onRequest: () -> Unit) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text("后台保活", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            Text("前台服务已开启；再加入电池优化白名单可防止系统在休眠时冻结进程。", style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(12.dp))
            OutlinedButton(onClick = onRequest, Modifier.fillMaxWidth()) { Text("加入电池优化白名单") }
        }
    }
}

@Composable
private fun DeviceInfoCard(model: String, version: String, api: Int, battery: Int, wifi: Boolean) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text("设备信息", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceEvenly) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) { Text("型号"); Text(model) }
                Column(horizontalAlignment = Alignment.CenterHorizontally) { Text("Android"); Text(version) }
                Column(horizontalAlignment = Alignment.CenterHorizontally) { Text("API"); Text("$api") }
            }
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceEvenly) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) { Text("电池"); Text("$battery%") }
                Column(horizontalAlignment = Alignment.CenterHorizontally) { Text("WiFi"); Text(if (wifi) "已连接" else "未连接") }
            }
        }
    }
}

@Composable
private fun QuickActionsCard(lastResult: String, onExecute: (String) -> Unit) {
    var command by remember { mutableStateOf("") }
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp)) {
            Text("快速操作", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = command,
                onValueChange = { command = it },
                label = { Text("Shell 命令") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = { if (command.isNotBlank()) { onExecute(command); command = "" } },
                Modifier.fillMaxWidth(),
                enabled = command.isNotBlank()
            ) { Text("执行") }
            if (lastResult.isNotBlank()) {
                Spacer(Modifier.height(12.dp))
                Text(lastResult, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
