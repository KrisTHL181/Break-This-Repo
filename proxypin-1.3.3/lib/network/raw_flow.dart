import 'dart:async';
import 'dart:convert';
import 'dart:io';
import 'dart:typed_data';

import 'package:flutter/services.dart';
import 'package:proxypin/utils/listenable_list.dart';

/// 一条原始流（UDP 报文 / 非 HTTP 的 TCP 数据段）抓取记录。
/// 由 Android 原生 VPN 层上报（见 com.network.proxy.vpn.RawFlowReporter）。
class RawFlow {
  final String protocol; // "UDP" | "TCP"
  final String srcIp;
  final int srcPort;
  final String dstIp;
  final int dstPort;
  final String direction; // "up" 客户端->服务器 | "down" 服务器->客户端
  final int length; // 完整载荷长度
  final int timestamp;
  final Uint8List payload; // 截断后的载荷（最多 1024 字节）

  RawFlow({
    required this.protocol,
    required this.srcIp,
    required this.srcPort,
    required this.dstIp,
    required this.dstPort,
    required this.direction,
    required this.length,
    required this.timestamp,
    required this.payload,
  });

  factory RawFlow.fromMap(Map<dynamic, dynamic> m) {
    final payloadB64 = m['payload']?.toString() ?? '';
    Uint8List payload;
    try {
      payload = base64Decode(payloadB64);
    } catch (_) {
      payload = Uint8List(0);
    }
    return RawFlow(
      protocol: m['protocol']?.toString() ?? 'UDP',
      srcIp: m['srcIp']?.toString() ?? '',
      srcPort: (m['srcPort'] as num?)?.toInt() ?? 0,
      dstIp: m['dstIp']?.toString() ?? '',
      dstPort: (m['dstPort'] as num?)?.toInt() ?? 0,
      direction: m['direction']?.toString() ?? 'up',
      length: (m['length'] as num?)?.toInt() ?? payload.length,
      timestamp: (m['timestamp'] as num?)?.toInt() ?? DateTime.now().millisecondsSinceEpoch,
      payload: payload,
    );
  }

  /// 展示用的端点描述：src -> dst
  String get endpoint => '$srcIp:$srcPort -> $dstIp:$dstPort';

  /// 载荷的十六进制文本
  String get hexPayload {
    final sb = StringBuffer();
    for (var i = 0; i < payload.length; i++) {
      sb.write(payload[i].toRadixString(16).padLeft(2, '0'));
      if (i % 16 == 15 && i != payload.length - 1) sb.write('\n');
      else if (i != payload.length - 1) sb.write(' ');
    }
    return sb.toString();
  }

  /// 载荷的可打印 ASCII 文本（不可打印字符替换为 .）
  String get asciiPayload {
    final sb = StringBuffer();
    for (var b in payload) {
      if (b >= 32 && b <= 126) {
        sb.writeCharCode(b);
      } else {
        sb.write('.');
      }
    }
    return sb.toString();
  }
}

/// 原始流抓取存储：集中管理捕获到的 UDP/TCP 原始流。
class RawFlowStore {
  RawFlowStore._();

  static final RawFlowStore instance = RawFlowStore._();

  /// 最大保留条数（超出丢弃最旧的）
  static const int maxEntries = 2000;

  final ListenableList<RawFlow> flows = ListenableList<RawFlow>();

  /// 从原生缓冲主动拉取（用于推送可能丢失的场景）
  void add(RawFlow flow) {
    if (flows.length >= maxEntries) {
      flows.removeAt(0);
    }
    flows.add(flow);
  }

  void clear() {
    flows.clear();
  }
}

/// 原始流通道：订阅原生 EventChannel 推送，并调用原生重放/拉取/清空。
class RawFlowChannel {
  static const EventChannel _event = EventChannel('com.proxy/rawFlowEvents');
  static const MethodChannel _ctrl = MethodChannel('com.proxy/rawFlowCtrl');

  static StreamSubscription<dynamic>? _sub;
  static bool _started = false;

  /// 启动订阅（Android 且开启抓包时才有原生上报；其它平台静默跳过）。
  /// 幂等：多次调用只订阅一次。
  static void start() {
    if (_started) return;
    if (!Platform.isAndroid) return;
    _started = true;
    _sub = _event.receiveBroadcastStream().listen(
      (event) {
        if (event is Map) {
          RawFlowStore.instance.add(RawFlow.fromMap(event));
        }
      },
      onError: (Object e) {
        // 订阅断开属正常（例如 App 进入后台）；尝试下次拉取兜底。
      },
    );
  }

  static void stop() {
    _sub?.cancel();
    _sub = null;
    _started = false;
  }

  /// 主动拉取原生缓冲中的原始流（补漏）。
  static Future<void> pullBuffered() async {
    if (!Platform.isAndroid) return;
    try {
      final list = await _ctrl.invokeMethod<List<dynamic>>('getBufferedFlows');
      if (list == null) return;
      for (final e in list) {
        if (e is Map) {
          RawFlowStore.instance.add(RawFlow.fromMap(e));
        }
      }
    } catch (_) {}
  }

  /// 重放一条 UDP 报文。
  static Future<bool> replayUdp(String dstIp, int dstPort, Uint8List payload) async {
    if (!Platform.isAndroid) return false;
    try {
      return await _ctrl.invokeMethod<bool>('replayUdp', {
        'dstIp': dstIp,
        'dstPort': dstPort,
        'payloadB64': base64Encode(payload),
      }) ?? false;
    } catch (_) {
      return false;
    }
  }

  /// 重放一条 TCP 连接（建立连接并发送载荷）。
  static Future<bool> replayTcp(String dstIp, int dstPort, Uint8List payload) async {
    if (!Platform.isAndroid) return false;
    try {
      return await _ctrl.invokeMethod<bool>('replayTcp', {
        'dstIp': dstIp,
        'dstPort': dstPort,
        'payloadB64': base64Encode(payload),
      }) ?? false;
    } catch (_) {
      return false;
    }
  }

  /// 清空原生缓冲。
  static Future<void> clearNative() async {
    if (!Platform.isAndroid) return;
    try {
      await _ctrl.invokeMethod<void>('clearFlows');
    } catch (_) {}
  }
}
