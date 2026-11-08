#!/usr/bin/env python3
"""Read-only installation/permission checks; never captures a frame or clicks."""
import argparse
from pathlib import Path
import sys
from runtime import ROOT,installation_errors,state_directory


def check(offline=False):
    print('Python:',sys.version.split()[0])
    print('解释器:',sys.executable)
    print('程序目录:',ROOT)
    print('运行记录:',state_directory())
    errors=installation_errors()
    for error in errors:print('[需要处理]',error)
    if errors:return 1
    print('[正常] 系统、依赖和识别模板')
    if offline:
        print('离线检查通过。此检查不读取画面，也不要求屏幕录制或辅助功能权限。')
        return 0
    import Quartz as Q
    import AppKit as AK
    checks=[('屏幕录制',bool(Q.CGPreflightScreenCaptureAccess()),'系统设置 → 隐私与安全性 → 屏幕与系统音频录制'),
            ('辅助功能',bool(Q.CGPreflightPostEventAccess()),'系统设置 → 隐私与安全性 → 辅助功能（仅自动点击需要）')]
    mirror=bool(AK.NSRunningApplication.runningApplicationsWithBundleIdentifier_('com.apple.ScreenContinuity'))
    checks.append(('iPhone 镜像进程',mirror,'打开 iPhone 镜像，并让实体手机保持锁屏'))
    for name,ok,tip in checks:
        print(('[正常] ' if ok else '[需要处理] ')+name+(('；'+tip) if not ok else ''))
    print('镜像进程存在不代表已连接；请在窗口中确认游戏画面。此命令没有截屏或点击。')
    return 0 if all(ok for _,ok,_ in checks) else 1


def main(argv=None):
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--offline',action='store_true',help='只检查安装，不检查实时权限和镜像')
    args=parser.parse_args(argv)
    return check(args.offline)


if __name__=='__main__':sys.exit(main())
