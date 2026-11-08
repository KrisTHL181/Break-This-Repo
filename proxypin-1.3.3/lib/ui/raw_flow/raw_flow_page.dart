import 'package:flutter/material.dart';
import 'package:proxypin/network/raw_flow.dart';
import 'package:proxypin/utils/listenable_list.dart';

/// 原始流抓包页：查看 VPN 捕获到的 UDP / 非 HTTP TCP 流量，并支持重放。
///
/// 使用方式（Android）：
///  1. 开启 VPN 抓包后，本页实时显示捕获到的 UDP / TCP 原始流；
///  2. 点一条查看报文（Hex + ASCII）；
///  3. 可"重放"该条报文（UDP 重发、TCP 重建连接发送）。
class RawFlowPage extends StatefulWidget {
  const RawFlowPage({super.key});

  @override
  State<RawFlowPage> createState() => _RawFlowPageState();
}

class _RawFlowPageState extends State<RawFlowPage> {
  late OnchangeListEvent<RawFlow> _listEvent;
  final RawFlowStore _store = RawFlowStore.instance;

  @override
  void initState() {
    super.initState();
    RawFlowChannel.start();
    _listEvent = OnchangeListEvent<RawFlow>(() {
      if (mounted) setState(() {});
    });
    _store.flows.addListener(_listEvent);
    RawFlowChannel.pullBuffered();
  }

  @override
  void dispose() {
    _store.flows.removeListener(_listEvent);
    super.dispose();
  }

  Future<void> _openDetail(RawFlow flow) async {
    final replaySent = await showModalBottomSheet<bool>(
      context: context,
      isScrollControlled: true,
      builder: (ctx) => _RawFlowDetailSheet(flow: flow),
    );
    if (replaySent == true && mounted) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('已发送重放请求（TCP 可能因协议/端口关闭而失败）'), duration: Duration(seconds: 2)),
      );
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('UDP / TCP 原始流'),
        actions: [
          IconButton(
            tooltip: '拉取缓冲',
            icon: const Icon(Icons.sync),
            onPressed: () => RawFlowChannel.pullBuffered(),
          ),
          IconButton(
            tooltip: '清空',
            icon: const Icon(Icons.delete_sweep),
            onPressed: () {
              _store.clear();
              RawFlowChannel.clearNative();
            },
          ),
        ],
      ),
      body: _store.flows.isEmpty
          ? const Center(
              child: Text('暂无原始流\n请开启 VPN 抓包后，访问使用 UDP/TCP 的应用（游戏/音视频/DNS 等）',
                  textAlign: TextAlign.center),
            )
          : ListView.builder(
              itemCount: _store.flows.length,
              itemBuilder: (context, index) {
                final flow = _store.flows.elementAt(_store.flows.length - 1 - index);
                return _flowTile(flow);
              },
            ),
    );
  }

  Widget _flowTile(RawFlow flow) {
    final isUp = flow.direction == 'up';
    final color = flow.protocol == 'UDP' ? Colors.blue : Colors.teal;
    return ListTile(
      leading: CircleAvatar(
        radius: 16,
        backgroundColor: color.withValues(alpha: 0.15),
        child: Text(flow.protocol, style: TextStyle(fontSize: 10, color: color, fontWeight: FontWeight.bold)),
      ),
      title: Text(flow.endpoint, style: const TextStyle(fontSize: 13)),
      subtitle: Text(
        '${isUp ? '↑' : '↓'} ${flow.length} 字节 · ${_time(flow.timestamp)}',
        style: const TextStyle(fontSize: 12),
      ),
      trailing: Icon(isUp ? Icons.arrow_upward : Icons.arrow_downward, size: 18, color: Colors.grey),
      onTap: () => _openDetail(flow),
    );
  }

  String _time(int ms) {
    final t = DateTime.fromMillisecondsSinceEpoch(ms);
    String two(int v) => v.toString().padLeft(2, '0');
    return '${two(t.hour)}:${two(t.minute)}:${two(t.second)}';
  }
}

/// 报文详情底部弹层：Hex + ASCII 视图 + 重放按钮。
class _RawFlowDetailSheet extends StatelessWidget {
  final RawFlow flow;

  const _RawFlowDetailSheet({required this.flow});

  @override
  Widget build(BuildContext context) {
    return SafeArea(
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              children: [
                Text('${flow.protocol} ${flow.endpoint}',
                    style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 15)),
                const Spacer(),
                Text(flow.direction == 'up' ? '客户端→服务器' : '服务器→客户端',
                    style: TextStyle(fontSize: 12, color: Colors.grey.shade600)),
              ],
            ),
            const SizedBox(height: 4),
            Text('载荷 ${flow.length} 字节（显示前 ${flow.payload.length}）', style: TextStyle(fontSize: 12, color: Colors.grey)),
            const SizedBox(height: 12),
            Flexible(
              child: SingleChildScrollView(
                child: Container(
                  width: double.infinity,
                  padding: const EdgeInsets.all(10),
                  decoration: BoxDecoration(
                    color: Colors.grey.shade100,
                    borderRadius: BorderRadius.circular(8),
                  ),
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      SelectableText(flow.hexPayload,
                          style: const TextStyle(fontFamily: 'monospace', fontSize: 12)),
                      const Divider(height: 16),
                      SelectableText(flow.asciiPayload,
                          style: const TextStyle(fontFamily: 'monospace', fontSize: 12)),
                    ],
                  ),
                ),
              ),
            ),
            const SizedBox(height: 12),
            Row(
              mainAxisAlignment: MainAxisAlignment.end,
              children: [
                TextButton(
                  onPressed: () => Navigator.of(context).pop(),
                  child: const Text('关闭'),
                ),
                const SizedBox(width: 8),
                ElevatedButton.icon(
                  icon: const Icon(Icons.replay, size: 18),
                  label: const Text('重放'),
                  onPressed: () async {
                    final ok = flow.protocol == 'UDP'
                        ? await RawFlowChannel.replayUdp(flow.dstIp, flow.dstPort, flow.payload)
                        : await RawFlowChannel.replayTcp(flow.dstIp, flow.dstPort, flow.payload);
                    if (context.mounted) {
                      ScaffoldMessenger.of(context).showSnackBar(SnackBar(
                        content: Text(ok ? '已重放到 ${flow.dstIp}:${flow.dstPort}' : '重放失败，请检查目标可达'),
                        duration: const Duration(seconds: 2),
                      ));
                    }
                  },
                ),
              ],
            ),
          ],
        ),
      ),
    );
  }
}
