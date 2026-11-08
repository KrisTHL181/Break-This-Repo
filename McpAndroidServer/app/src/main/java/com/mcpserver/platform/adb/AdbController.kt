package com.mcpserver.platform.adb

import com.mcpserver.platform.shell.PrivilegedShell

/**
 * Wireless debugging control that actually runs on the device.
 *
 * The bundled WirelessAdb class shells out to the `adb` binary, which does not
 * exist on Android, so it can never work. This uses real device commands
 * through the privileged shell instead.
 */
object AdbController {

    data class Status(
        val tcpPort: Int,
        val wifiAddress: String?,
        val elevated: Boolean,
        val rawPortProperty: String,
    ) {
        val wirelessEnabled: Boolean get() = tcpPort > 0
    }

    suspend fun status(): Status {
        val portProperty = PrivilegedShell.exec("getprop service.adb.tcp.port").stdout.trim()
        val address = PrivilegedShell.exec(
            "ip -4 addr show wlan0 2>/dev/null | grep -o 'inet [0-9.]*' | head -1 | cut -d' ' -f2"
        ).stdout.trim()
        return Status(
            tcpPort = portProperty.toIntOrNull() ?: 0,
            wifiAddress = address.ifBlank { null },
            elevated = PrivilegedShell.isShizukuAvailable(),
            rawPortProperty = portProperty,
        )
    }

    /** Turn on ADB over TCP and report the resulting property. */
    suspend fun enableTcpIp(port: Int = 5555): PrivilegedShell.Result =
        PrivilegedShell.exec(
            "setprop service.adb.tcp.port $port; stop adbd; sleep 1; start adbd; sleep 1; getprop service.adb.tcp.port",
            30_000L,
        )

    /** Turn ADB over TCP back off. */
    suspend fun disableTcpIp(): PrivilegedShell.Result =
        PrivilegedShell.exec(
            "setprop service.adb.tcp.port -1; stop adbd; sleep 1; start adbd",
            30_000L,
        )
}
