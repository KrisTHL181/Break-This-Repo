"""Installation checks and paths; importing this module needs only Python."""
import argparse
import importlib.util
import math
from pathlib import Path
import platform
import sys

ROOT=Path(__file__).resolve().parent
DEPENDENCIES={'numpy':'numpy','PIL':'Pillow','cv2':'opencv-python-headless',
              'objc':'pyobjc-core','AppKit':'pyobjc-framework-Cocoa',
              'Quartz':'pyobjc-framework-Quartz','CoreMedia':'pyobjc-framework-CoreMedia',
              'ScreenCaptureKit':'pyobjc-framework-ScreenCaptureKit','Vision':'pyobjc-framework-Vision'}
ASSETS=('templates.npz','long_templates.npz','elephant_templates.npz')


def interval_seconds(value):
    try:seconds=float(value)
    except (TypeError,ValueError):raise argparse.ArgumentTypeError('点击间隔必须是数字，例如 0.5') from None
    if not math.isfinite(seconds) or not .1<=seconds<=3600:
        raise argparse.ArgumentTypeError('点击间隔必须在 0.1–3600 秒之间')
    return seconds


def platform_error():
    if sys.platform!='darwin':return '实时工具需要 macOS 15 或更新系统及 iPhone 镜像；不支持 Windows / Linux。'
    if not (3,12)<=sys.version_info[:2]<(3,15):return '请安装 Python 3.12、3.13 或 3.14，然后重新运行 install.command。'
    version=platform.mac_ver()[0]
    if not version or int(version.split('.')[0])<15:return 'iPhone 镜像需要 macOS 15 或更新系统。'
    return None


def installation_errors():
    errors=[]
    problem=platform_error()
    if problem:errors.append(problem)
    missing=[package for module,package in DEPENDENCIES.items() if importlib.util.find_spec(module) is None]
    if missing:errors.append('缺少依赖：'+', '.join(missing)+'。请运行 install.command，并使用 .venv 中的 Python。')
    missing_assets=[name for name in ASSETS if not (ROOT/name).is_file()]
    if missing_assets:errors.append('缺少识别模板：'+', '.join(missing_assets)+'。请下载完整的 pig-game-guide 文件夹。')
    return errors


def require_installation():
    errors=installation_errors()
    if errors:raise RuntimeError('\n'.join(errors))


def state_directory():
    return Path.home()/'Library'/'Application Support'/'Pig Game Guide'
