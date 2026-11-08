#! /usr/bin/env python3

"""Simple singleton I18N helper for GUI modules.

The rest of the codebase uses :mod:`libs.langs_handle` directly, but some
modules (navigation, tab manager) want a dict-style ``.get(key, default)``
convenience. This small wrapper exists so those modules don't need to know
about ``langs_handle`` status dictionaries.
"""

import threading
from typing import Any, Dict, Optional

from libs.langs_handle import langs_handle


class I18N:
    """Singleton translation provider.

    Usage::

        i18n = I18N.get_instance()
        msg = i18n.get("nav.definition_not_found", "Definition not found")
    """

    _instance: Optional["I18N"] = None
    _lock = threading.Lock()

    def __init__(self) -> None:
        self._handle = langs_handle()
        self._cache: Dict[str, Any] = {}

    @classmethod
    def get_instance(cls) -> "I18N":
        if cls._instance is None:
            with cls._lock:
                if cls._instance is None:
                    cls._instance = cls()
        return cls._instance

    def get(self, key: str, default: Optional[str] = None) -> Any:
        if key in self._cache:
            return self._cache[key]
        try:
            result = self._handle.key_languages(key)
            if result.get("status") == "success":
                data = result["data"]
                self._cache[key] = data
                return data
        except Exception:
            pass
        return default if default is not None else key

    def invalidate(self) -> None:
        """Drop cached translations (e.g. after language change)."""
        self._cache.clear()
