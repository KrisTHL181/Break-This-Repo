#! /usr/bin/env python3

package: dict = {
    "ID": "thon-code",
    "Name": "Thon Code Log Manager",
    "Path": ".main.libs.log_manager",
    "Entrance": "main.py"
}

"""Centralized logging with rotation to prevent disk overflow.

Provides get_logger(name) returning a logger with:
  - RotatingFileHandler: max 5 MB per file, 3 backups
  - StreamHandler: INFO+ to console for developer visibility

Log directory is resolved relative to the project root in development
or next to the executable in frozen (PyInstaller) mode.
"""

import os
import sys
import logging
from logging.handlers import RotatingFileHandler
from typing import Optional

# Rotation: 5 MB per file, keep 3 historical backups (max ~20 MB total)
MAX_BYTES: int = 5 * 1024 * 1024
BACKUP_COUNT: int = 3
DEFAULT_LEVEL: int = logging.DEBUG

_log_dir: str = ""
_initialized: bool = False


def _resolve_log_dir() -> str:
    """Determine the log directory based on execution mode.

    Returns:
        str: Absolute path to the log directory
    """
    if getattr(sys, 'frozen', False):
        # PyInstaller: logs next to the executable
        base = os.path.dirname(sys.executable)
    else:
        # Development: logs in <project_root>/logs
        base = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
    return os.path.join(base, "logs")


def init_logging(log_dir: Optional[str] = None, level: int = DEFAULT_LEVEL) -> str:
    """Initialize the global logging configuration.

    Creates the log directory if missing and configures the root logger
    with a RotatingFileHandler and a console StreamHandler. Safe to call
    multiple times; subsequent calls are no-ops.

    Args:
        log_dir: Override log directory; None auto-resolves
        level: Minimum log level for file output

    Returns:
        str: The resolved log directory path
    """
    global _log_dir, _initialized
    if _initialized:
        return _log_dir

    _log_dir = log_dir or _resolve_log_dir()
    os.makedirs(_log_dir, exist_ok=True)
    _initialized = True

    # Configure root logger so all child loggers inherit handlers
    root = logging.getLogger()
    root.setLevel(level)

    # Rotating file handler: captures everything (DEBUG+)
    log_file = os.path.join(_log_dir, "thoncode.log")
    file_handler = RotatingFileHandler(
        log_file, maxBytes=MAX_BYTES, backupCount=BACKUP_COUNT,
        encoding="utf-8")
    file_handler.setLevel(level)
    file_handler.setFormatter(logging.Formatter(
        "%(asctime)s [%(levelname)s] %(name)s: %(message)s"
    ))
    root.addHandler(file_handler)

    # Console handler: INFO+ for real-time developer feedback
    console_handler = logging.StreamHandler(sys.stdout)
    console_handler.setLevel(logging.INFO)
    console_handler.setFormatter(logging.Formatter(
        "[%(levelname)s] %(name)s: %(message)s"
    ))
    root.addHandler(console_handler)

    logging.info("Logging initialized (dir=%s)", _log_dir)
    return _log_dir


def get_logger(name: str, level: Optional[int] = None) -> logging.Logger:
    """Get a configured logger by name.

    If init_logging() has not been called yet, initializes with defaults
    so early-stage modules can still log without explicit setup.

    Args:
        name: Logger name (typically __name__ or module identifier)
        level: Optional level override for this logger

    Returns:
        logging.Logger: Configured logger instance
    """
    if not _initialized:
        init_logging()
    logger = logging.getLogger(name)
    if level is not None:
        logger.setLevel(level)
    return logger


def get_log_dir() -> str:
    """Return the current log directory path.

    Returns:
        str: Log directory path, or empty string if not yet initialized
    """
    return _log_dir
