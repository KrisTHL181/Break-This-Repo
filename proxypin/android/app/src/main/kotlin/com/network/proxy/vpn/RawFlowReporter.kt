package com.network.proxy.vpn

import android.os.Handler
import android.os.Looper
import android.util.Base64
import android.util.Log
import io.flutter.plugin.common.EventChannel
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * 原始流抓取上报器：把 VPN 捕获到的非 HTTP TCP 与 UDP 流量上报给 Flutter 侧。
 *
 * 数据流：原生 VPN 线程捕获 → 本对象上报到 EventChannel('com.proxy/rawFlowEvents')
 *          → Dart 侧 [RawFlowChannel] 接收并存入抓包历史。
 *
 * 线程安全：VPN/NIO 线程可任意调用 [report]；内部把上报投递到主线程再交给 EventSink。
 * 同时维护一个有界环形缓冲，供 Dart 侧在丢失推送时主动拉取。
 *
 * @author ProxyBird
 */
object RawFlowReporter {
    private const val TAG = "RawFlowReporter"

    /** 单条载荷最多保留的字节数，避免单条事件过大 */
    const val MAX_PAYLOAD_BYTES = 1024

    /** 缓冲的最大条数 */
    private const val MAX_BUFFER = 500

    /** 方向常量 */
    const val DIR_UP = "up"      // 客户端 -> 服务器
    const val DIR_DOWN = "down"  // 服务器 -> 客户端

    @Volatile
    private var sink: EventChannel.EventSink? = null

    private val handler = Handler(Looper.getMainLooper())

    // 有界环形缓冲：推送丢失时 Dart 侧可主动拉取
    private val buffer: ConcurrentLinkedQueue<Map<String, Any?>> = ConcurrentLinkedQueue()

    /**
     * 由 RawFlowPlugin 在 Dart 订阅时设置。
     */
    fun setSink(eventSink: EventChannel.EventSink?) {
        sink = eventSink
    }

    /**
     * 上报一条原始流事件。
     *
     * @param protocol "UDP" 或 "TCP"
     * @param srcIp 源 IP（字符串）
     * @param srcPort 源端口
     * @param dstIp 目的 IP
     * @param dstPort 目的端口
     * @param direction [DIR_UP] / [DIR_DOWN]
     * @param payload 载荷字节（内部截断到 [MAX_PAYLOAD_BYTES] 并 base64）
     */
    fun report(
        protocol: String,
        srcIp: String, srcPort: Int,
        dstIp: String, dstPort: Int,
        direction: String,
        payload: ByteArray
    ) {
        try {
            val len = payload.size
            val sample = if (len <= MAX_PAYLOAD_BYTES) payload else payload.copyOf(MAX_PAYLOAD_BYTES)
            val event: Map<String, Any?> = mapOf(
                "protocol" to protocol,
                "srcIp" to srcIp,
                "srcPort" to srcPort,
                "dstIp" to dstIp,
                "dstPort" to dstPort,
                "direction" to direction,
                "length" to len,
                "timestamp" to System.currentTimeMillis(),
                "payload" to Base64.encodeToString(sample, Base64.NO_WRAP)
            )

            // 写入缓冲（有界）
            if (buffer.size >= MAX_BUFFER) {
                buffer.poll()
            }
            buffer.offer(event)

            // 投递到主线程交给 EventSink
            handler.post {
                try {
                    sink?.success(event)
                } catch (e: Exception) {
                    Log.w(TAG, "push event failed", e)
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "report failed", e)
        }
    }

    /**
     * 取出缓冲中全部事件（Dart 侧主动拉取用）。
     */
    fun drainBuffer(): List<Map<String, Any?>> {
        val list = mutableListOf<Map<String, Any?>>()
        while (true) {
            val e = buffer.poll() ?: break
            list.add(e)
        }
        return list
    }

    /**
     * 清空缓冲。
     */
    fun clear() {
        buffer.clear()
    }
}
