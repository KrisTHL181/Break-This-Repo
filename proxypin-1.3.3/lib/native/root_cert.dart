import 'package:flutter/services.dart';
import 'package:proxypin/network/util/logger.dart';

/// Root 辅助通道：已 root 设备"一键解决无网络抓包"。
/// 对应原生 com.network.proxy.plugin.RootCertPlugin
class RootCert {
  static const MethodChannel _channel = MethodChannel('com.proxy/rootCert');

  /// 设备是否已 root
  static Future<bool> isRooted() async {
    try {
      return await _channel.invokeMethod<bool>('isRooted') ?? false;
    } catch (e) {
      logger.d('isRooted error: $e');
      return false;
    }
  }

  /// 一键安装系统证书（需 root）。返回 {success, message, needReboot}
  static Future<Map<String, dynamic>> installSystemCa(String certPem, String hashName) async {
    try {
      final r = await _channel.invokeMethod<Map<dynamic, dynamic>>(
        'installSystemCa',
        {'certPem': certPem, 'hashName': hashName},
      );
      return (r ?? {}).map((k, v) => MapEntry(k.toString(), v));
    } catch (e) {
      logger.d('installSystemCa error: $e');
      return {'success': false, 'message': '调用失败: $e', 'needReboot': false};
    }
  }

  /// 通过 Magisk 模块安装（需 root，装完重启）。返回 {success, message, needReboot}
  static Future<Map<String, dynamic>> installViaMagisk(String certPem, String hashName) async {
    try {
      final r = await _channel.invokeMethod<Map<dynamic, dynamic>>(
        'installViaMagisk',
        {'certPem': certPem, 'hashName': hashName},
      );
      return (r ?? {}).map((k, v) => MapEntry(k.toString(), v));
    } catch (e) {
      logger.d('installViaMagisk error: $e');
      return {'success': false, 'message': '调用失败: $e', 'needReboot': false};
    }
  }

  /// 导出可手动安装的 Magisk 模块 zip，返回保存路径（null 表示失败）
  static Future<String?> exportMagiskModuleZip(String certPem, String hashName) async {
    try {
      return await _channel.invokeMethod<String>(
        'exportMagiskModuleZip',
        {'certPem': certPem, 'hashName': hashName},
      );
    } catch (e) {
      logger.d('exportMagiskModuleZip error: $e');
      return null;
    }
  }

  /// 检测已安装的证书绕过模块，返回 [{package,label,type}]
  static Future<List<Map<String, dynamic>>> detectPinningModules() async {
    try {
      final list = await _channel.invokeMethod<List<dynamic>>('detectPinningModules');
      return (list ?? [])
          .whereType<Map<dynamic, dynamic>>()
          .map((m) => m.map((k, v) => MapEntry(k.toString(), v)))
          .toList();
    } catch (e) {
      logger.d('detectPinningModules error: $e');
      return [];
    }
  }

  /// 是否已装 LSPosed/Xposed 框架
  static Future<bool> hasLsposed() async {
    try {
      return await _channel.invokeMethod<bool>('hasLsposed') ?? false;
    } catch (e) {
      logger.d('hasLsposed error: $e');
      return false;
    }
  }
}
