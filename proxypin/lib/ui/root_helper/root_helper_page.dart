import 'dart:convert';

import 'package:flutter/material.dart';
import 'package:proxypin/network/util/crts.dart';
import 'package:proxypin/network/util/logger.dart';
import 'package:proxypin/native/root_cert.dart';

/// Root 辅助页：为已 root 设备"一键解决无网络抓包"。
///
/// 背景：抓 HTTPS 时很多 App 只信任系统证书或做了证书固定，
/// 导致通过 ProxyPin 抓包后提示"无网络/证书无效"。
/// 本页利用 root 把 ProxyPin CA 装入系统证书库，并引导启用证书绕过模块。
class RootHelperPage extends StatefulWidget {
  const RootHelperPage({super.key});

  @override
  State<RootHelperPage> createState() => _RootHelperPageState();
}

class _RootHelperPageState extends State<RootHelperPage> {
  bool _rooted = false;
  bool _checking = true;
  String? _certPem;
  String? _hashName;
  List<Map<String, dynamic>> _modules = [];
  String? _lastMessage;

  @override
  void initState() {
    super.initState();
    _load();
  }

  Future<void> _load() async {
    setState(() => _checking = true);
    final rooted = await RootCert.isRooted();
    final modules = await RootCert.detectPinningModules();
    String? pem;
    String? hash;
    try {
      pem = await CertificateManager.certificatePem();
      hash = await CertificateManager.systemCertificateName();
    } catch (e) {
      logger.d('load cert failed: $e');
    }
    if (mounted) {
      setState(() {
        _rooted = rooted;
        _modules = modules;
        _certPem = pem;
        _hashName = hash;
        _checking = false;
      });
    }
  }

  Future<void> _showMessage(String title, String content) async {
    setState(() => _lastMessage = content);
    if (!mounted) return;
    await showDialog<void>(
      context: context,
      builder: (ctx) => AlertDialog(
        title: Text(title),
        content: SingleChildScrollView(child: Text(content)),
        actions: [
          TextButton(
            onPressed: () => Navigator.of(ctx).pop(),
            child: const Text('确定'),
          ),
        ],
      ),
    );
  }

  Future<void> _installSystemCa() async {
    if (_certPem == null || _hashName == null) {
      await _showMessage('提示', '未获取到 CA 证书，请先初始化证书。');
      return;
    }
    if (!_rooted) {
      await _showMessage('未检测到 root', '无法自动安装系统证书。请先 root，或使用 Magisk 模块方式安装。');
      return;
    }
    final r = await RootCert.installSystemCa(_certPem!, _hashName!);
    final needReboot = r['needReboot'] == true;
    await _showMessage(
      r['success'] == true ? '安装成功' : '安装失败',
      '${r['message']}${needReboot ? '\n\n（需要重启设备后生效）' : ''}',
    );
  }

  Future<void> _installViaMagisk() async {
    if (_certPem == null || _hashName == null) {
      await _showMessage('提示', '未获取到 CA 证书。');
      return;
    }
    final r = await RootCert.installViaMagisk(_certPem!, _hashName!);
    await _showMessage(
      r['success'] == true ? 'Magisk 模块已生成' : 'Magisk 模块部署失败',
      '${r['message']}',
    );
  }

  Future<void> _exportMagiskZip() async {
    if (_certPem == null || _hashName == null) return;
    final path = await RootCert.exportMagiskModuleZip(_certPem!, _hashName!);
    await _showMessage(
      path != null ? '模块已导出' : '导出失败',
      path != null ? 'Magisk 模块 zip 已保存到：\n$path\n\n在 Magisk 中本地安装后重启即可。' : '导出失败，请检查存储权限。',
    );
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('Root 辅助 · 一键解决无网络')),
      body: _checking
          ? const Center(child: CircularProgressIndicator())
          : ListView(
              padding: const EdgeInsets.all(16),
              children: [
                _statusCard(),
                const SizedBox(height: 12),
                _sectionTitle('自动安装系统证书'),
                const SizedBox(height: 8),
                _actionButton(
                  icon: Icons.security,
                  label: '一键安装系统证书',
                  subtitle: '把 ProxyPin CA 写入系统证书库，解决 App 不信任用户证书导致的无网络',
                  onTap: _installSystemCa,
                ),
                _actionButton(
                  icon: Icons.folder_special,
                  label: '通过 Magisk 模块安装',
                  subtitle: '写入 /data/adb/modules（需 root，装完重启）',
                  onTap: _installViaMagisk,
                ),
                _actionButton(
                  icon: Icons.archive_outlined,
                  label: '导出 Magisk 模块 zip',
                  subtitle: '生成可手动导入 Magisk 的模块包（无需 root）',
                  onTap: _exportMagiskZip,
                ),
                const SizedBox(height: 16),
                _sectionTitle('证书绕过模块（SSL Pinning）'),
                const SizedBox(height: 8),
                _pinningCard(),
                const SizedBox(height: 8),
                Text(
                  '说明：部分 App 做了证书固定（SSL Pinning），即便装系统证书也会校验失败报无网络。'
                  '需在 LSPosed 中启用 TrustMeAlready / 算法助手 等绕过模块。',
                  style: Theme.of(context).textTheme.bodySmall?.copyWith(color: Colors.grey),
                ),
                if (_lastMessage != null) ...[
                  const SizedBox(height: 16),
                  _sectionTitle('最近结果'),
                  const SizedBox(height: 8),
                  SelectableText(_lastMessage!, style: const TextStyle(fontSize: 13)),
                ],
              ],
            ),
    );
  }

  Widget _statusCard() {
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Row(
          children: [
            Icon(_rooted ? Icons.verified_user : Icons.no_accounts, color: _rooted ? Colors.green : Colors.orange),
            const SizedBox(width: 12),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text('Root 状态：${_rooted ? '已获取' : '未检测到'}',
                      style: const TextStyle(fontWeight: FontWeight.bold)),
                  const SizedBox(height: 4),
                  Text(
                    _rooted ? '可以自动安装系统证书。' : '建议先用 Magisk 等方案 root。',
                    style: Theme.of(context).textTheme.bodySmall,
                  ),
                ],
              ),
            ),
            IconButton(icon: const Icon(Icons.refresh), onPressed: _load),
          ],
        ),
      ),
    );
  }

  Widget _sectionTitle(String title) {
    return Text(title, style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 15));
  }

  Widget _actionButton({
    required IconData icon,
    required String label,
    required String subtitle,
    required VoidCallback onTap,
  }) {
    return Card(
      child: ListTile(
        leading: Icon(icon),
        title: Text(label),
        subtitle: Text(subtitle),
        trailing: const Icon(Icons.chevron_right),
        onTap: onTap,
      ),
    );
  }

  Widget _pinningCard() {
    if (_modules.isEmpty) {
      return Card(
        child: Padding(
          padding: const EdgeInsets.all(16),
          child: Row(
            children: [
              const Icon(Icons.info_outline, color: Colors.orange),
              const SizedBox(width: 12),
              Expanded(
                child: Text(
                  '未检测到证书绕过模块。\n如需抓"无网络"的加固 App，请安装 LSPosed + TrustMeAlready / 算法助手。',
                  style: Theme.of(context).textTheme.bodySmall,
                ),
              ),
            ],
          ),
        ),
      );
    }
    return Card(
      child: Column(
        children: [
          for (final m in _modules)
            ListTile(
              leading: const Icon(Icons.extension, color: Colors.green),
              title: Text(m['label']?.toString() ?? m['package'].toString()),
              subtitle: Text('${m['package']} · ${m['type']}'),
              trailing: const Icon(Icons.check_circle, color: Colors.green),
            ),
        ],
      ),
    );
  }
}
