#! /usr/bin/env python3

package: dict = {
    "ID": "thon-code-libs",
    "Name": "Thon Code Font Loader",
    "Path": ".main.libs.font_loader",
    "Entrance": "main.py"
}

import os
import sys
import tkinter.font as tkfont

# Private font registration flag (Windows): keeps the font visible only to
# this process without installing it system-wide.
_FR_PRIVATE = 0x10

# Cached set of already-registered font file paths to avoid re-registration.
_registered_files = set()


def _bundled_fira_code_path():
    """Resolve the absolute path to the bundled FiraCode ttf.

    Locates assets/fonts relative to this module so the font works regardless
    of the current working directory (important for packaged builds).

    Returns:
        str or None: Absolute path to FiraCode-Regular.ttf, or None if absent.
    """
    candidates = []
    if getattr(sys, 'frozen', False):
        exe_dir = os.path.dirname(sys.executable)
        candidates.append(os.path.join(exe_dir, 'assets', 'fonts', 'FiraCode-Regular.ttf'))
        meipass = getattr(sys, '_MEIPASS', None)
        if meipass:
            candidates.append(os.path.join(meipass, 'assets', 'fonts', 'FiraCode-Regular.ttf'))
    else:
        base_dir = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
        candidates.append(os.path.join(base_dir, 'assets', 'fonts', 'FiraCode-Regular.ttf'))

    for path in candidates:
        if path and os.path.exists(path):
            return path
    return None


def _register_private_font(file_path):
    """Register a font file for this process only (Windows implementation).

    Uses AddFontResourceExW with FR_PRIVATE so the font becomes available to
    Tk without modifying system-wide font state. On non-Windows platforms this
    is a no-op (system font lookup is used instead).

    Args:
        file_path: Absolute path to a .ttf font file

    Returns:
        bool: True if registration succeeded (or already registered)
    """
    if file_path in _registered_files:
        return True
    if sys.platform != 'win32':
        return False
    try:
        import ctypes
        gdi32 = ctypes.windll.gdi32  # type: ignore[attr-defined]
        result = gdi32.AddFontResourceExW(file_path, _FR_PRIVATE, 0)
        if result > 0:
            _registered_files.add(file_path)
            # Broadcast WM_FONTCHANGE so Tk picks up the new font.
            HWND_BROADCAST = 0xFFFF
            WM_FONTCHANGE = 0x001D
            user32 = ctypes.windll.user32  # type: ignore[attr-defined]
            user32.PostMessageW(HWND_BROADCAST, WM_FONTCHANGE, 0, 0)
            return True
        return False
    except (OSError, AttributeError):
        return False


def _font_available(family):
    """Check whether a font family is actually resolved by Tk.

    Args:
        family: Font family name to probe

    Returns:
        bool: True if Tk resolved the family name to itself
    """
    try:
        f = tkfont.Font(family=family, size=12)
        return family.lower() in f.actual()["family"].lower()
    except tkfont.TclError:
        return False


def load_fira_code_font(master=None, size=12, ligatures=True):
    """Load the Fira Code font for use in the editor.

    Resolution order:
    1. System-installed "Fira Code" (already available to Tk).
    2. Bundled FiraCode-Regular.ttf registered privately (Windows only).
    3. Fallback to Consolas, then Courier New.

    Args:
        master: Optional Tk master (unused, kept for backward compatibility)
        size: Font size in points
        ligatures: Whether ligatures are intended (kept for API compatibility)

    Returns:
        tkfont.Font: A usable font object for the editor.
    """
    # 1. System-installed Fira Code.
    if _font_available("Fira Code"):
        return tkfont.Font(family="Fira Code", size=size)

    # 2. Bundled ttf registered privately (Windows).
    ttf_path = _bundled_fira_code_path()
    if ttf_path and _register_private_font(ttf_path):
        # After registration the family name "Fira Code" should resolve.
        if _font_available("Fira Code"):
            return tkfont.Font(family="Fira Code", size=size)
        # Some builds expose it under the full name.
        if _font_available("Fira Code Regular"):
            return tkfont.Font(family="Fira Code Regular", size=size)

    # 3. Fallback chain.
    for fallback in ("Consolas", "Courier New"):
        if _font_available(fallback):
            return tkfont.Font(family=fallback, size=size)
    # Final guarantee: Courier New is available on every Tk port.
    return tkfont.Font(family="Courier New", size=size)
