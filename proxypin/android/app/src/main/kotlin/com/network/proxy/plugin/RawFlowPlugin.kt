package com.network.proxy.plugin

import android.util.Base64
import com.network.proxy.vpn.RawFlowReporter
import com.network.proxy.vpn.socket.ProtectSocketHolder
import io.flutter.plugin.common.EventChannel
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket

/**
 * 原始流（UDP / 非 HTTP TCP）抓取通道。
 *
 * - EventChannel('com.proxy/rawFlowEvents')：原生 -> Dart 推送捕获到的原始流。
 * - MethodChannel('com.proxy/rawFlowCtrl')：Dart -> 原生，重放原始流 / 拉取缓冲 / 清空。
 *
 * 重放：
 *  - replayUdp(dstIp, dstPort, payloadB64)：通过真实网络（protect）重发一条 UDP 报文。
 *  - replayTcp(dstIp, dstPort, payloadB64)：通过真实网络（protect）建立 TCP 连接并发送载荷。
 */
class RawFlowPlugin : AndroidFlutterPlugin() {

    companion object {
        const val EVENT_CHANNEL = "com.proxy/rawFlowEvents"
        const val CTRL_CHANNEL = "com.proxy/rawFlowCtrl"
    }

    override fun onAttachedToEngine(binding: io.flutter.embedding.engine.plugins.FlutterPlugin.FlutterPluginBinding) {
        // 原生 -> Dart 推送
        val eventChannel = EventChannel(binding.binaryMessenger, EVENT_CHANNEL)
        eventChannel.setStreamHandler(object : EventChannel.StreamHandler {
            override fun onListen(arguments: Any?, events: EventChannel.EventSink?) {
                RawFlowReporter.setSink(events)
            }

            override fun onCancel(arguments: Any?) {
                RawFlowReporter.setSink(null)
            }
        })

        // Dart -> 原生控制（重放/拉取/清空）
        val ctrl = MethodChannel(binding.binaryMessenger, CTRL_CHANNEL)
        ctrl.setMethodCallHandler { call: MethodCall, result: MethodChannel.Result ->
            when (call.method) {
                "replayUdp" -> {
                    val dstIp = call.argument<String>("dstIp") ?: ""
                    val dstPort = call.argument<Int>("dstPort") ?: 0
                    val payload = decode(call.argument<String>("payloadB64"))
                    if (dstIp.isBlank() || dstPort <= 0 || payload.isEmpty()) {
                        result.error("bad_args", "dstIp/dstPort/payloadB64 不能为空", null)
                        return@setMethodCallHandler
                    }
                    result.success(replayUdp(dstIp, dstPort, payload))
                }
                "replayTcp" -> {
                    val dstIp = call.argument<String>("dstIp") ?: ""
                    val dstPort = call.argument<Int>("dstPort") ?: 0
                    val payload = decode(call.argument<String>("payloadB64"))
                    if (dstIp.isBlank() || dstPort <= 0) {
                        result.error("bad_args", "dstIp/dstPort 不能为空", null)
                        return@setMethodCallHandler
                    }
                    result.success(replayTcp(dstIp, dstPort, payload))
                }
                "getBufferedFlows" -> {
                    result.success(RawFlowReporter.drainBuffer())
                }
                "clearFlows" -> {
                    RawFlowReporter.clear()
                    result.success(null)
                }
                else -> result.notImplemented()
            }
        }
    }

    override fun onDetachedFromEngine(binding: io.flutter.embedding.engine.plugins.FlutterPlugin.FlutterPluginBinding) {
    }

    private fun decode(b64: String?): ByteArray {
        if (b64.isNullOrBlank()) return ByteArray(0)
        return try {
            Base64.decode(b64, Base64.NO_WRAP)
        } catch (e: Exception) {
            ByteArray(0)
        }
    }

    /**
     * 通过真实网络重发一条 UDP 报文（protect 防止回环进 VPN）。
     */
    private fun replayUdp(dstIp: String, dstPort: Int, payload: ByteArray): Boolean {
        return try {
            val socket = DatagramSocket()
            socket.soTimeout = 3000
            ProtectSocketHolder.protect(socket)
            val packet = DatagramPacket(payload, payload.size, InetAddress.getByName(dstIp), dstPort)
            socket.send(packet)
            socket.close()
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 通过真实网络建立 TCP 连接并发送载荷（protect 防止回环进 VPN）。
     * 发送后保持读取一小段时间以拿到可能的回包并立即关闭。
     */
    private fun replayTcp(dstIp: String, dstPort: Int, payload: ByteArray): Boolean {
        return try {
            val socket = Socket()
            ProtectSocketHolder.protect(socket)
            socket.connect(InetSocketAddress(dstIp, dstPort), 5000)
            socket.soTimeout = 3000
            if (payload.isNotEmpty()) {
                socket.getOutputStream().write(payload)
                socket.getOutputStream().flush()
            }
            // 尝试读一点回包（不阻塞失败）
            try {
                val buf = ByteArray(4096)
                socket.getInputStream().read(buf)
            } catch (ignore: Exception) {
            }
            socket.close()
            true
        } catch (e: Exception) {
            false
        }
    }
}
