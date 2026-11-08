# -*- mode: python ; coding: utf-8 -*-

import os

block_cipher = None

# 添加 web 目录作为数据文件
added_files = [
    ('web', 'web'),
]

a = Analysis(
    ['main.py'],
    pathex=[],
    binaries=[],
    datas=added_files,
    hiddenimports=[
        'webview',
        'PIL',
        'PIL.Image',
        'clr',
        'cffi',
        '_cffi_backend',
        'clr_loader',
        'clr_loader.ffi',
        'pythonnet',
        'pycparser',
        'pycparser.lextab',
        'pycparser.yacctab',
    ],
    hookspath=[],
    hooksconfig={},
    runtime_hooks=[],
    excludes=[
        # 排除 PyQt - webview 可能自动检测并引入
        'PyQt6',
        'PyQt6.QtCore',
        'PyQt6.QtGui',
        'PyQt6.QtWidgets',
        'PyQt6.QtNetwork',
        'PyQt6.QtOpenGL',
        'PyQt6.QtOpenGLWidgets',
        'PyQt6.QtWebChannel',
        'PyQt5',
        'PySide6',
        'PySide2',
        # 排除 numpy
        'numpy',
        'numpy.core',
        # 排除其他大型库
        'cryptography',
        'charset_normalizer',
        'jinja2',
        'markupsafe',
        # 排除测试和文档
        'unittest',
        'test',
        'pydoc',
        'pydoc_data',
        # 排除 tkinter
        'tkinter',
        'Tkinter',
        # 排除其他 GUI 框架
        'gtk',
        'gi',
        'wx',
        'wxPython',
        'curses',
        # 排除开发工具
        'setuptools',
        'pkg_resources',
        'wheel',
        'pip',
        # 排除网络库（除必要的）
        'requests',
        'urllib3',
        'certifi',
        'idna',
        # 注意：cffi 和 pycparser 不能排除，pythonnet 需要它们
    ],
    win_no_prefer_redirects=False,
    win_private_assemblies=False,
    cipher=block_cipher,
    noarchive=False,
)

# 过滤掉不需要的二进制文件
# 注意：libcrypto 和 libssl 不能排除，webview 的 http 模块需要 ssl
binaries_to_exclude = [
    'Qt6',
    'Qt5',
    'numpy',
]

a.binaries = [b for b in a.binaries if not any(excl in b[0] for excl in binaries_to_exclude)]

pyz = PYZ(a.pure, a.zipped_data, cipher=block_cipher)

exe = EXE(
    pyz,
    a.scripts,
    a.binaries,
    a.zipfiles,
    a.datas,
    [],
    name='ICO生成器',
    debug=False,
    bootloader_ignore_signals=False,
    strip=False,
    upx=True,
    upx_exclude=[],
    runtime_tmpdir=None,
    console=False,
    disable_windowed_traceback=False,
    target_arch=None,
    codesign_identity=None,
    entitlements_file=None,
    icon='icon.ico' if os.path.exists('icon.ico') else None,
)
