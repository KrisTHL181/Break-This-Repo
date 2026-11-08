/*
 * Copyright 2026 Hongen Wang All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 */

import 'package:flutter/material.dart';
import 'package:proxypin/network/components/manager/environment_manager.dart';

/// 匹配 EnvironmentManager 支持的 `{{name}}` 语法（与 env_manager 内部正则保持一致）。
final RegExp _envTokenRe = RegExp(r'\{\{\s*([\w.\-]+)\s*\}\}');

/// 判断字符串是否引用了任何环境变量。
bool containsEnvToken(String? text) {
  if (text == null || text.isEmpty) return false;
  if (!text.contains('{{')) return false;
  return _envTokenRe.hasMatch(text);
}

/// 环境变量高亮控制器：在请求编辑器的普通 [TextField] 中原地高亮 `{{name}}` 引用。
/// - 已定义的变量渲染为绿色。
/// - 未定义/未启用的变量渲染为红色 + 波浪下划线，便于及早发现拼写错误。
///
/// 控制器会订阅 [EnvironmentManager]，环境切换 / 变量变化时自动重绘。
///
/// @author wanghongen
class EnvHighlightTextEditingController extends TextEditingController {
  EnvHighlightTextEditingController({super.text}) {
    _attach();
  }

  EnvironmentManager? _manager;

  void _attach() {
    final mgr = EnvironmentManager.instanceOrNull;
    if (mgr != null) {
      _manager = mgr;
      mgr.addListener(_onEnvChanged);
    } else {
      // 首次进入编辑器时 manager 可能尚未加载完毕；懒加载后再挂钩。
      EnvironmentManager.instance.then((mgr) {
        if (_disposed) return;
        _manager = mgr;
        mgr.addListener(_onEnvChanged);
        _onEnvChanged();
      });
    }
  }

  bool _disposed = false;

  void _onEnvChanged() {
    if (_disposed) return;
    // 强制 TextField 重建。
    notifyListeners();
  }

  @override
  void dispose() {
    _disposed = true;
    _manager?.removeListener(_onEnvChanged);
    super.dispose();
  }

  @override
  TextSpan buildTextSpan({required BuildContext context, TextStyle? style, required bool withComposing}) {
    final text = this.text;
    if (text.isEmpty || !text.contains('{{')) {
      return super.buildTextSpan(context: context, style: style, withComposing: withComposing);
    }

    final matches = _envTokenRe.allMatches(text).toList();
    if (matches.isEmpty) {
      return super.buildTextSpan(context: context, style: style, withComposing: withComposing);
    }

    final isDark = Theme.brightnessOf(context) == Brightness.dark;
    final definedStyle = (style ?? const TextStyle()).copyWith(
      color: isDark ? const Color(0xFF7EE787) : const Color(0xFF1B7F3A),
      fontWeight: FontWeight.w600,
    );
    final undefinedStyle = (style ?? const TextStyle()).copyWith(
      color: isDark ? const Color(0xFFFF7B72) : const Color(0xFFD32F2F),
      fontWeight: FontWeight.w600,
      decoration: TextDecoration.underline,
      decorationStyle: TextDecorationStyle.wavy,
      decorationColor: isDark ? const Color(0xFFFF7B72) : const Color(0xFFD32F2F),
    );

    final spans = <TextSpan>[];
    int cursor = 0;
    for (final m in matches) {
      if (m.start > cursor) {
        spans.add(TextSpan(text: text.substring(cursor, m.start), style: style));
      }
      final name = m.group(1)!;
      final resolved = _manager?.resolve(name);
      spans.add(TextSpan(
        text: text.substring(m.start, m.end),
        style: resolved == null ? undefinedStyle : definedStyle,
      ));
      cursor = m.end;
    }
    if (cursor < text.length) {
      spans.add(TextSpan(text: text.substring(cursor), style: style));
    }

    return TextSpan(style: style, children: spans);
  }
}

/// 只读展示（`Text.rich`）版本的高亮构建器。用于 mobile header 列表等
/// 不使用 [TextField] 的场景。
///
/// - 命中变量渲染为绿色 / 红色；
/// - 未包含 `{{` 时直接返回 [Text]，零成本回退。
Widget buildEnvHighlightText(
  BuildContext context,
  String? text, {
  TextStyle? style,
  int? maxLines,
  TextOverflow? overflow,
}) {
  if (text == null || text.isEmpty) {
    return Text(text ?? '', style: style, maxLines: maxLines, overflow: overflow ?? TextOverflow.clip);
  }
  if (!text.contains('{{')) {
    return Text(text, style: style, maxLines: maxLines, overflow: overflow ?? TextOverflow.clip);
  }
  final matches = _envTokenRe.allMatches(text).toList();
  if (matches.isEmpty) {
    return Text(text, style: style, maxLines: maxLines, overflow: overflow ?? TextOverflow.clip);
  }

  final isDark = Theme.brightnessOf(context) == Brightness.dark;
  final definedStyle = (style ?? const TextStyle()).copyWith(
    color: isDark ? const Color(0xFF7EE787) : const Color(0xFF1B7F3A),
    fontWeight: FontWeight.w600,
  );
  final undefinedStyle = (style ?? const TextStyle()).copyWith(
    color: isDark ? const Color(0xFFFF7B72) : const Color(0xFFD32F2F),
    fontWeight: FontWeight.w600,
    decoration: TextDecoration.underline,
    decorationStyle: TextDecorationStyle.wavy,
    decorationColor: isDark ? const Color(0xFFFF7B72) : const Color(0xFFD32F2F),
  );

  final mgr = EnvironmentManager.instanceOrNull;
  final spans = <TextSpan>[];
  int cursor = 0;
  for (final m in matches) {
    if (m.start > cursor) {
      spans.add(TextSpan(text: text.substring(cursor, m.start), style: style));
    }
    final name = m.group(1)!;
    final resolved = mgr?.resolve(name);
    spans.add(TextSpan(
      text: text.substring(m.start, m.end),
      style: resolved == null ? undefinedStyle : definedStyle,
    ));
    cursor = m.end;
  }
  if (cursor < text.length) {
    spans.add(TextSpan(text: text.substring(cursor), style: style));
  }

  return Text.rich(
    TextSpan(style: style, children: spans),
    maxLines: maxLines,
    overflow: overflow ?? TextOverflow.clip,
  );
}
