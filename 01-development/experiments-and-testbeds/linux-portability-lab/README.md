# Linux 用户也别想太舒服

欢迎来到文件名沼泽。这里的文件名在常见 Linux 文件系统上合法，但会让按空格拆文件名、解析 `ls` 输出、漏加引号的脚本出丑。

`fixtures/` 包含换行、制表符、前导连字符、通配符、尾随空格、大小写、视觉近似 Unicode，以及看起来像 Shell 语法的普通文本文件。它们没有可执行权限，内容只是说明。Git 不会执行文件名；正确传参的程序也不会执行它们。Windows 无法完整检出这一组样本，可从 Git 对象查看。

## 玩法

1. 在终端手动补全这些名字，感受一下生活。
2. 运行 `python3 linux-portability-lab/audit.py`，看带转义的真实文件名。
3. 挑战：写一个脚本，准确枚举每个文件而不把换行或空格误当分隔符。

适用于 Linux 上 Bash 的只读参考：

```bash
while IFS= read -r -d '' file; do
  printf '%q\n' "$file"
done < <(find linux-portability-lab/fixtures -type f -print0)
```

## 假病毒弹窗

手动运行 `python3 linux-portability-lab/prank.py`。需要图形桌面和 Python Tkinter；脚本不会自动安装依赖。

你会看到“企鹅接管电脑”的假警报，窗口持续标注 **PRANK / SIMULATION**。所有进度、感染数量和文件名都是虚构的。最多四个窗口，30 秒自动结束；任何窗口按 Esc，或关闭任何一个窗口，都会结束整个演示。退出后不会重开窗口。

演示不读写用户文件、不联网、不播放声音、不锁屏、不置顶、不修改系统配置，也没有启动钩子。请在自己的桌面上运行。

`python3 -m unittest discover -s linux-portability-lab -p 'test_*.py'` 可在没有桌面的环境验证窗口数量、计时与退出逻辑。
