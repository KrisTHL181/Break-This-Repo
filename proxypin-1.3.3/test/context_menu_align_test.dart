import 'package:flutter/gestures.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:proxypin/ui/component/context_menu.dart';

/// Verifies that labels of every item type share one left edge, which is the
/// visual property being asked for (checkmark in its own gutter, text aligned).
void main() {
  const hostKey = Key('host');

  testWidgets('all label types share the same left edge', (tester) async {
    await tester.pumpWidget(MaterialApp(
      home: Scaffold(
        body: Center(
          child: Builder(builder: (context) {
            return GestureDetector(
              behavior: HitTestBehavior.opaque,
              onSecondaryTapDown: (d) => showCustomContextMenu(context, d.globalPosition, const [
                ContextMenuItem.normal(label: 'Plain'),
                ContextMenuItem.separator(),
                ContextMenuItem.submenu(label: 'HasSubmenu', submenu: [
                  ContextMenuItem.normal(label: 'Nested'),
                ]),
                ContextMenuItem.checkbox(label: 'CheckedOn', checked: true),
                ContextMenuItem.checkbox(label: 'CheckedOff', checked: false),
              ]),
              child: Container(key: hostKey, width: 200, height: 200, color: Colors.blue),
            );
          }),
        ),
      ),
    ));

    await tester.tap(find.byKey(hostKey), buttons: kSecondaryMouseButton);
    await tester.pumpAndSettle();

    double leftOf(String label) => tester.getTopLeft(find.text(label)).dx;

    final plain = leftOf('Plain');
    final submenu = leftOf('HasSubmenu');
    final on = leftOf('CheckedOn');
    final off = leftOf('CheckedOff');

    debugPrint('LEFT plain=$plain submenu=$submenu checkedOn=$on checkedOff=$off');

    expect(submenu, closeTo(plain, 0.5), reason: 'submenu label misaligned');
    expect(on, closeTo(plain, 0.5), reason: 'checked label misaligned');
    expect(off, closeTo(plain, 0.5), reason: 'unchecked label misaligned');

    // The checkmark must sit left of the shared text edge, in its own gutter.
    final checkRight = tester.getBottomRight(find.byIcon(Icons.check)).dx;
    debugPrint('CHECK right edge=$checkRight vs text left=$plain');
    expect(checkRight, lessThanOrEqualTo(plain + 0.5));
  });

  testWidgets('no checkmark gutter is reserved when no item is a checkbox', (tester) async {
    // A menu with nothing to check (e.g. the domain menu) must not leave an
    // empty icon column on the left.
    Future<double> leftEdgeFor(List<ContextMenuItem> items) async {
      await tester.pumpWidget(MaterialApp(
        home: Scaffold(
          body: Center(
            child: Builder(builder: (context) {
              return GestureDetector(
                behavior: HitTestBehavior.opaque,
                onSecondaryTapDown: (d) => showCustomContextMenu(context, d.globalPosition, items),
                child: Container(key: hostKey, width: 200, height: 200, color: Colors.blue),
              );
            }),
          ),
        ),
      ));
      await tester.tap(find.byKey(hostKey), buttons: kSecondaryMouseButton);
      await tester.pumpAndSettle();
      final x = tester.getTopLeft(find.text('Copy host')).dx;
      await tester.sendKeyEvent(LogicalKeyboardKey.escape);
      await tester.pumpAndSettle();
      return x;
    }

    final withoutCheckbox = await leftEdgeFor(const [
      ContextMenuItem.normal(label: 'Copy host'),
      ContextMenuItem.separator(),
      ContextMenuItem.normal(label: 'Delete'),
    ]);
    final withCheckbox = await leftEdgeFor(const [
      ContextMenuItem.normal(label: 'Copy host'),
      ContextMenuItem.checkbox(label: 'Auto read', checked: false),
    ]);

    debugPrint('LEFT without-checkbox=$withoutCheckbox with-checkbox=$withCheckbox');
    // No gutter reserved => the label sits further left than in a menu that
    // does reserve one.
    expect(withoutCheckbox, lessThan(withCheckbox));
  });

  testWidgets('every item row is the same fixed height', (tester) async {
    // Desktop VisualDensity otherwise shrinks rows (measured 24dp) or leaves
    // them oversized (40dp); pin them at the Windows menu height.
    await tester.pumpWidget(MaterialApp(
      home: Scaffold(
        body: Center(
          child: Builder(builder: (context) {
            return GestureDetector(
              behavior: HitTestBehavior.opaque,
              onSecondaryTapDown: (d) => showCustomContextMenu(context, d.globalPosition, const [
                ContextMenuItem.normal(label: 'Plain'),
                ContextMenuItem.submenu(label: 'HasSubmenu', submenu: [
                  ContextMenuItem.normal(label: 'Nested'),
                ]),
                ContextMenuItem.checkbox(label: 'Checked', checked: true),
                ContextMenuItem.normal(label: 'Disabled', disabled: true),
              ]),
              child: Container(key: hostKey, width: 200, height: 200, color: Colors.blue),
            );
          }),
        ),
      ),
    ));

    await tester.tap(find.byKey(hostKey), buttons: kSecondaryMouseButton);
    await tester.pumpAndSettle();

    double heightOf(String label) {
      final row = find.ancestor(of: find.text(label), matching: find.byType(Material)).first;
      return tester.getSize(row).height;
    }

    for (final label in ['Plain', 'HasSubmenu', 'Checked', 'Disabled']) {
      expect(heightOf(label), 28.0, reason: '$label row height drifted');
    }

    // Row *pitch* matters as much as row height: Material's default
    // MaterialTapTargetSize.padded wraps each row in a 48dp tap target, which
    // spaced rows 48dp apart while each row still measured 28dp. Assert the
    // spacing between adjacent rows, not just their size.
    final plainTop = tester.getTopLeft(find.text('Plain')).dy;
    final submenuTop = tester.getTopLeft(find.text('HasSubmenu')).dy;
    expect(submenuTop - plainTop, 28.0, reason: 'row pitch drifted (tap target padding?)');
  });
}
