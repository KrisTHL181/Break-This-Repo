"""A bounded, manual desktop prank. No real scan or system modification."""

import time

LIFETIME_SECONDS = 30
MAX_WINDOWS = 4
FAKE_MESSAGES = (
    "PRANK / SIMULATION\n\nPenguin takeover: successful.\nYour computer now says quack.\n\nNo files were scanned or changed.",
    "PRANK / SIMULATION\n\nFAKE THREAT DETECTED\nUnquoted shell variables found in your imagination.\n\nThis is a joke, not a security alert.",
    "PRANK / SIMULATION\n\nFAKE ENCRYPTION COMPLETE\n0 real files affected.\nThe penguin has encrypted its own lunch.",
    "PRANK / SIMULATION\n\nFAKE RANSOM DEMAND\nPayment required: one imaginary fish.\n\nPress Escape to fire the penguin.",
)


class Prank:
    def __init__(self, root, tk, clock=time.monotonic):
        self.root = root
        self.tk = tk
        self.clock = clock
        self.started = clock()
        self.closed = False
        self.windows = [root]
        self.pending = set()
        self.configure(root, FAKE_MESSAGES[0], 0)
        self.status = tk.Label(root, text="Auto-close in 30 seconds", bg="#181923", fg="#facc15")
        self.status.pack(pady=8)
        for index in range(1, MAX_WINDOWS):
            self.schedule(index * 1800, lambda index=index: self.popup(index))
        self.tick()

    def schedule(self, delay, callback):
        if self.closed:
            return

        def run():
            self.pending.discard(timer)
            if not self.closed:
                callback()

        timer = self.root.after(delay, run)
        self.pending.add(timer)

    def configure(self, window, message, index):
        window.title("PRANK / SIMULATION — Penguin takeover")
        window.configure(bg="#181923")
        window.geometry(f"520x300+{70 + index * 65}+{70 + index * 45}")
        window.protocol("WM_DELETE_WINDOW", self.stop)
        window.bind("<Escape>", self.stop)
        self.tk.Label(
            window, text=message, bg="#181923", fg="#fb7185",
            font=("sans-serif", 12), wraplength=480, justify="center",
        ).pack(padx=16, pady=20)
        self.tk.Button(window, text="Stop prank / Close all windows (Esc)", command=self.stop).pack(pady=6)

    def popup(self, index):
        if self.closed or len(self.windows) >= MAX_WINDOWS:
            return
        window = self.tk.Toplevel(self.root)
        self.windows.append(window)
        self.configure(window, FAKE_MESSAGES[index], index)

    def tick(self):
        if self.closed:
            return
        remaining = LIFETIME_SECONDS - (self.clock() - self.started)
        if remaining <= 0:
            self.stop()
            return
        self.status.configure(text=f"SIMULATION — auto-close in {remaining:.0f} seconds")
        self.schedule(200, self.tick)

    def stop(self, _event=None):
        if self.closed:
            return
        self.closed = True
        for timer in tuple(self.pending):
            self.root.after_cancel(timer)
        self.pending.clear()
        self.root.destroy()


def main():
    try:
        import tkinter as tk
    except ImportError:
        print("This optional prank needs Python Tkinter and a graphical desktop.")
        return 1
    try:
        root = tk.Tk()
    except tk.TclError:
        print("No usable graphical desktop. The prank did not start.")
        return 1
    Prank(root, tk)
    root.mainloop()
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
