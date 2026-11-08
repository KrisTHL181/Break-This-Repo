import 'dart:io';

import 'package:flutter/material.dart';
import 'package:proxypin/ui/configuration.dart';
import 'package:proxypin/ui/desktop/window_listener.dart';
import 'package:screen_retriever/screen_retriever.dart';
import 'package:window_manager/window_manager.dart';

import '../network/util/logger.dart';
import '../ui/component/multi_window.dart';

class DesktopSupport {
  static const _minimumWindowSize = Size(1000, 600);

  static Future<void> initialize(AppConfiguration appConfiguration) async {
    try {
      await windowManager.ensureInitialized();

      final defaultWindowSize = Platform.isMacOS ? const Size(1230, 750) : const Size(1100, 650);
      final windowSize = _sanitizeWindowSize(appConfiguration.windowSize, defaultWindowSize);
      appConfiguration.windowSize = windowSize;

      final windowOptions = WindowOptions(
        minimumSize: _minimumWindowSize,
        size: windowSize,
        titleBarStyle: TitleBarStyle.hidden,
      );
      if (appConfiguration.themeMode != ThemeMode.system) {
        await windowManager
            .setBrightness(appConfiguration.themeMode == ThemeMode.dark ? Brightness.dark : Brightness.light);
      }

      await windowManager.waitUntilReadyToShow(windowOptions, () async {
        final position = appConfiguration.windowPosition;
        if (position != null && await _isPositionVisible(position, windowSize)) {
          await windowManager.setPosition(position);
        } else {
          if (position != null) {
            appConfiguration.windowPosition = null;
          }
          //位置无效(如显示器已断开)时居中显示，避免窗口跑到屏幕外不可见
          await windowManager.center();
        }

        await windowManager.show();
        await windowManager.focus();
      });

      windowManager.addListener(WindowChangeListener(appConfiguration));
      registerMethodHandler();
    } catch (e) {
      logger.e('Error during desktop initialization: $e');
    }
  }

  /// 修复配置文件中可能残留的无效窗口尺寸。
  ///
  /// 旧配置可能包含 0、负数、NaN、Infinity 或远大于当前显示器的尺寸。
  /// 这些值会被 window_manager 直接用于创建窗口，Windows 上可能表现为
  /// 白屏、窗口不可见或 Flutter surface 无法正常布局。
  static Size _sanitizeWindowSize(Size? savedSize, Size defaultSize) {
    if (savedSize == null || !savedSize.width.isFinite || !savedSize.height.isFinite) {
      return defaultSize;
    }

    final width = savedSize.width.clamp(_minimumWindowSize.width, 3840.0);
    final height = savedSize.height.clamp(_minimumWindowSize.height, 2160.0);
    final sanitized = Size(width.toDouble(), height.toDouble());

    if (sanitized != savedSize) {
      logger.w('Invalid saved window size $savedSize, using $sanitized');
    }
    return sanitized;
  }

  /// 校验保存的窗口位置是否落在某个显示器的可见范围内。
  /// 显示器断开或分辨率变化后，旧位置可能在所有屏幕之外，导致窗口不可见。
  static Future<bool> _isPositionVisible(Offset position, Size windowSize) async {
    try {
      final displays = await screenRetriever.getAllDisplays();
      if (displays.isEmpty || !position.dx.isFinite || !position.dy.isFinite) return false;

      // 至少要有一部分标题栏在某个显示器内，才认为位置可用。
      const minVisible = 100.0;
      for (final display in displays) {
        final origin = display.visiblePosition ?? Offset.zero;
        final size = display.visibleSize ?? display.size;
        final left = origin.dx;
        final top = origin.dy;
        final right = left + size.width;
        final bottom = top + size.height;

        final overlapLeft = position.dx > left - windowSize.width + minVisible;
        final overlapRight = position.dx < right - minVisible;
        final overlapTop = position.dy >= top;
        final overlapBottom = position.dy < bottom - minVisible;

        if (overlapLeft && overlapRight && overlapTop && overlapBottom) {
          return true;
        }
      }
      return false;
    } catch (e) {
      logger.e("Error validating window position: $e");
      // 校验失败时不使用保存的位置，回退到居中显示。
      return false;
    }
  }
}
