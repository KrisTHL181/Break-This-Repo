import 'package:flutter/gestures.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:proxypin/ui/component/context_menu.dart';

void main() {
  const hostKey = Key('host');

  /// Pumps a host widget whose right-click opens a context menu built from
  /// [items]. Click labels are appended to [clicks].
  Future<void> pumpMenu(
    WidgetTester tester,
    List<ContextMenuItem> items,
  ) async {
    await tester.pumpWidget(MaterialApp(
      home: Scaffold(
        body: Center(
          child: Builder(builder: (context) {
            return GestureDetector(
              behavior: HitTestBehavior.opaque,
              onSecondaryTapDown: (details) =>
                  showCustomContextMenu(context, details.globalPosition, items),
              child: Container(key: hostKey, width: 200, height: 200, color: Colors.blue),
            );
          }),
        ),
      ),
    ));
  }

  Future<void> openMenu(WidgetTester tester) async {
    await tester.tap(find.byKey(hostKey), buttons: kSecondaryMouseButton);
    await tester.pumpAndSettle();
  }

  /// Moves a mouse pointer over [finder] so hover-driven behaviour triggers.
  /// Reuses a single pointer per test: concurrent live mouse pointers trip an
  /// assertion inside MouseTracker.
  TestGesture? mouse;
  Future<void> hoverOver(WidgetTester tester, Finder finder) async {
    if (mouse == null) {
      mouse = await tester.createGesture(kind: PointerDeviceKind.mouse);
      await mouse!.addPointer(location: Offset.zero);
      addTearDown(() => mouse?.removePointer());
    }
    await mouse!.moveTo(tester.getCenter(finder));
    await tester.pumpAndSettle();
  }

  testWidgets('renders normal items and separators', (tester) async {
    await pumpMenu(tester, const [
      ContextMenuItem.normal(label: 'Copy'),
      ContextMenuItem.separator(),
      ContextMenuItem.normal(label: 'Delete'),
    ]);
    await openMenu(tester);

    expect(find.text('Copy'), findsOneWidget);
    expect(find.text('Delete'), findsOneWidget);
    expect(find.byType(Divider), findsOneWidget);
  });

  testWidgets('separator divider fills the menu width instead of collapsing', (tester) async {
    await pumpMenu(tester, const [
      ContextMenuItem.normal(label: 'A fairly long menu label'),
      ContextMenuItem.separator(),
    ]);
    await openMenu(tester);

    // A bare Divider would be laid out at zero width inside the menu panel,
    // making it invisible.
    expect(tester.getSize(find.byType(Divider)).width, greaterThan(100));
  });

  testWidgets('submenu opens automatically on hover, without a click', (tester) async {
    await pumpMenu(tester, const [
      ContextMenuItem.normal(label: 'Top'),
      ContextMenuItem.submenu(label: 'Export', submenu: [
        ContextMenuItem.normal(label: 'HAR'),
      ]),
    ]);
    await openMenu(tester);

    expect(find.text('HAR'), findsNothing);

    await hoverOver(tester, find.text('Export'));

    expect(find.text('HAR'), findsOneWidget);
  });

  testWidgets('clicking an item fires onClick and dismisses the menu', (tester) async {
    final clicks = <String>[];
    await pumpMenu(tester, [
      ContextMenuItem.normal(label: 'Repeat', onClick: () => clicks.add('Repeat')),
    ]);
    await openMenu(tester);

    await tester.tap(find.text('Repeat'));
    await tester.pumpAndSettle();

    expect(clicks, ['Repeat']);
    expect(find.text('Repeat'), findsNothing);
  });

  testWidgets('clicking a nested submenu item fires onClick and closes everything', (tester) async {
    final clicks = <String>[];
    await pumpMenu(tester, [
      ContextMenuItem.submenu(label: 'Copy', submenu: [
        ContextMenuItem.normal(label: 'Copy URL', onClick: () => clicks.add('Copy URL')),
      ]),
    ]);
    await openMenu(tester);
    await hoverOver(tester, find.text('Copy'));

    await tester.tap(find.text('Copy URL'));
    await tester.pumpAndSettle();

    expect(clicks, ['Copy URL']);
    expect(find.text('Copy URL'), findsNothing);
    expect(find.text('Copy'), findsNothing);
  });

  testWidgets('checkbox item shows a check only when checked', (tester) async {
    await pumpMenu(tester, const [
      ContextMenuItem.checkbox(label: 'Auto read', checked: true),
    ]);
    await openMenu(tester);
    expect(find.byIcon(Icons.check), findsOneWidget);

    await tester.sendKeyEvent(LogicalKeyboardKey.escape);
    await tester.pumpAndSettle();

    await pumpMenu(tester, const [
      ContextMenuItem.checkbox(label: 'Auto read', checked: false),
    ]);
    await openMenu(tester);
    expect(find.text('Auto read'), findsOneWidget);
    expect(find.byIcon(Icons.check), findsNothing);
  });

  testWidgets('disabled item is not clickable', (tester) async {
    final clicks = <String>[];
    await pumpMenu(tester, [
      ContextMenuItem.normal(
        label: 'Nope',
        disabled: true,
        onClick: () => clicks.add('Nope'),
      ),
    ]);
    await openMenu(tester);

    await tester.tap(find.text('Nope'));
    await tester.pumpAndSettle();
    expect(clicks, isEmpty);
  });

  testWidgets('Escape dismisses the menu', (tester) async {
    await pumpMenu(tester, const [ContextMenuItem.normal(label: 'Copy')]);
    await openMenu(tester);
    expect(find.text('Copy'), findsOneWidget);

    await tester.sendKeyEvent(LogicalKeyboardKey.escape);
    await tester.pumpAndSettle();
    expect(find.text('Copy'), findsNothing);
  });

  testWidgets('tapping outside dismisses the menu', (tester) async {
    await pumpMenu(tester, const [ContextMenuItem.normal(label: 'Copy')]);
    await openMenu(tester);
    expect(find.text('Copy'), findsOneWidget);

    await tester.tapAt(const Offset(5, 5));
    await tester.pumpAndSettle();
    expect(find.text('Copy'), findsNothing);
  });

  testWidgets('menu anchor has a non-empty size', (tester) async {
    // A zero-area anchor makes the Windows engine's incremental accessibility
    // merge fail on every open ("Failed to update ui::AXTree ... will not be in
    // the tree and is not the new root"). Verified empirically: zero-area
    // anchor produced 16+ engine errors per 4 open/close cycles, a sized anchor
    // produced none. Keep the anchor non-empty.
    await pumpMenu(tester, const [ContextMenuItem.normal(label: 'Copy')]);
    await openMenu(tester);

    final anchor = find.byType(MenuAnchor);
    expect(anchor, findsOneWidget);
    final anchorSize = tester.getSize(anchor);
    expect(anchorSize.width, greaterThan(0));
    expect(anchorSize.height, greaterThan(0));
  });

  testWidgets('reopening repeatedly does not throw (issue #878 regression)', (tester) async {
    await pumpMenu(tester, const [
      ContextMenuItem.normal(label: 'Copy'),
      ContextMenuItem.submenu(label: 'Export', submenu: [
        ContextMenuItem.normal(label: 'HAR'),
      ]),
    ]);

    for (var i = 0; i < 3; i++) {
      await openMenu(tester);
      expect(find.text('Copy'), findsOneWidget);
      await hoverOver(tester, find.text('Export'));
      expect(find.text('HAR'), findsOneWidget);
      await tester.sendKeyEvent(LogicalKeyboardKey.escape);
      await tester.pumpAndSettle();
      expect(find.text('Copy'), findsNothing);
    }
  });
}
