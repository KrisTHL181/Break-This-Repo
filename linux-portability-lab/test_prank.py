"""Exercise timer and exit behavior without opening actual windows."""

import unittest
from types import SimpleNamespace

from prank import MAX_WINDOWS, Prank


class Widget:
    def __init__(self, *args, **kwargs):
        self.handlers = {}

    def pack(self, **kwargs):
        pass

    def configure(self, **kwargs):
        pass

    def title(self, value):
        pass

    def geometry(self, value):
        pass

    def protocol(self, name, handler):
        self.handlers[name] = handler

    def bind(self, name, handler):
        self.handlers[name] = handler


class Root(Widget):
    def __init__(self):
        super().__init__()
        self.now = 0
        self.callbacks = {}
        self.counter = 0
        self.destroyed = False

    def after(self, milliseconds, callback):
        self.counter += 1
        self.callbacks[self.counter] = (self.now + milliseconds / 1000, callback)
        return self.counter

    def after_cancel(self, timer):
        self.callbacks.pop(timer, None)

    def destroy(self):
        self.destroyed = True

    def advance(self, seconds):
        target = self.now + seconds
        while self.callbacks:
            timer = min(self.callbacks, key=lambda key: self.callbacks[key][0])
            due, callback = self.callbacks[timer]
            if due > target:
                break
            del self.callbacks[timer]
            self.now = due
            callback()
        self.now = target


class PrankTests(unittest.TestCase):
    def make_prank(self):
        root = Root()
        tk = SimpleNamespace(Label=Widget, Button=Widget, Toplevel=Widget)
        return root, Prank(root, tk, clock=lambda: root.now)

    def test_bounded_windows_and_automatic_exit(self):
        root, prank = self.make_prank()
        root.advance(6)
        self.assertEqual(len(prank.windows), MAX_WINDOWS)
        prank.popup(1)
        self.assertEqual(len(prank.windows), MAX_WINDOWS)
        root.advance(25)
        self.assertTrue(root.destroyed)
        self.assertFalse(root.callbacks)

    def test_escape_cancels_future_popups(self):
        root, prank = self.make_prank()
        root.handlers["<Escape>"]()
        root.advance(60)
        self.assertEqual(len(prank.windows), 1)
        self.assertTrue(root.destroyed)
        self.assertFalse(root.callbacks)

    def test_closing_any_popup_closes_everything(self):
        root, prank = self.make_prank()
        root.advance(2)
        prank.windows[1].handlers["WM_DELETE_WINDOW"]()
        prank.stop()
        root.advance(60)
        self.assertEqual(len(prank.windows), 2)
        self.assertTrue(root.destroyed)
        self.assertFalse(root.callbacks)


if __name__ == "__main__":
    unittest.main()
