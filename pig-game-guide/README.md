# 猪棋盘助手 · Pig Game Guide

在 Mac 上识别 iPhone 镜像里的猪棋盘，计算完整解法，显示下一步绿点，也可以按指定的最短间隔自动点击。计算窗口可查看动物占格、朝向、数字锁、搜索过程和完整回放。

当前版本面向已验证的粉色猪、上下左右朝向、小鸡／小鸭、长猪、小象和数字锁外观。识别不确定、镜像断开或移动偏离预期时保留方案并停止发出新点击。它不是适配所有游戏和皮肤的通用识别器。

## 运行条件

| 项目 | 要求 |
| --- | --- |
| 电脑 | macOS 15 或更新系统；支持 iPhone 镜像的 Apple 芯片 Mac 或配备 T2 芯片的 Intel Mac |
| 手机 | iOS 18 或更新系统，能与这台 Mac 正常使用 iPhone 镜像 |
| Python | 3.12、3.13 或 3.14；完整回归测试使用 Python 3.12 |
| 权限 | 显示绿点需要屏幕录制权限；自动点击还需要辅助功能权限 |
| 网络 | 首次安装用于下载 Python 依赖；识别、计算和点击在本机运行 |

Python 可从 [Python 官方 macOS 下载页](https://www.python.org/downloads/macos/)安装。手机和 Mac 的 Apple 账户、蓝牙、Wi-Fi、设备及可用地区要求，以 [Apple 的 iPhone 镜像说明](https://support.apple.com/zh-cn/120421)为准。

本发布版仅支持 macOS + iPhone 镜像。无需 Codex、ChatGPT、API Key 或手机越狱。

## 安装

仓库体积较大，建议用 Git 只取这个目录：

```sh
git clone --depth 1 --filter=blob:none --no-checkout https://github.com/KrisTHL181/Break-This-Repo.git
cd Break-This-Repo
git sparse-checkout set --no-cone '/pig-game-guide/'
git checkout
cd pig-game-guide
sh install.command
```

如果已经下载了完整的 `pig-game-guide` 文件夹，可以直接双击 `install.command`。它会在当前目录创建 `.venv` 并安装固定版本的依赖，不使用 `sudo`，也不修改系统 Python。重复运行可修复缺少的依赖。

安装器会寻找受支持的 Python。需要指定解释器时：

```sh
PIG_GUIDE_PYTHON="/path/to/python3.12" sh install.command
```

不要复制别人的 `.venv`。移动整个文件夹后若虚拟环境失效，将 `.venv` 改名为 `.venv.old`，再运行安装器。

## 先运行离线示例

```sh
sh start.command --demo
```

此命令分析随附的、已遮去棋盘外区域的游戏样例，不连接或点击手机。成功后会在 `analysis/` 中生成：

- `board.json`：识别到的动物、格子坐标和锁计数。
- `plan.json`：完整步骤以及每一步后的棋盘状态。
- `next.png`：标出第一步绿点的图片。

随附示例为 92 只猪、数字锁 20 和 35，预期得到 120 步并模拟清空。模拟成功不代表手机上已经通关。

也可以分析自己的完整游戏窗口图片：

```sh
sh start.command --analyze "/path/to/game.png" --out analysis/my-level
```

图片需保持竖屏游戏窗口的完整比例，不能包含整张 Mac 桌面。锁数字识别使用本机 Apple Vision。

## 连接手机并使用

1. 在 Mac 打开 **iPhone 镜像**，进入游戏关卡。实体 iPhone 保持锁屏，镜像窗口保持可见。
2. 运行 `sh check.command`，根据结果配置 **系统设置 → 隐私与安全性 → 屏幕与系统音频录制**及**辅助功能**。系统列表中的进程可能显示为 Terminal、iTerm 或 Python，按实际启动方式授权；修改权限后退出并重新启动工具。
3. 双击 `start.command`，或在终端运行 `sh start.command`。识别成功后，点击猪身上的绿点即可；绿点不拦截鼠标事件。
4. 需要自动执行时，在菜单栏 **猪 → 点击间隔**设置秒数，再选择 **开始自动点击**。默认只提示，不会自动操作手机。

菜单栏还提供停止自动点击、重新识别、暂停提示、显示计算过程和退出。**Esc 停止发出新点击并取消正在进行的求解**；已经跑动的动物仍会由游戏继续移动。

最短间隔默认为 **1 秒**，可设置为 **0.1–3600 秒**。必要时工具会等待在途动物离场、冲突路线清空、数字锁消失或视频帧更新，因此设为 0.1 秒不等于每秒必定点击十次。设置间隔时会暂停点击并保留方案，保存后需要再次开始。

以下参数用于已有环境：

```sh
sh start.command --interval 0.5
sh start.command --marker box
sh start.command --no-visualization
sh start.command --auto-start --interval 0.5
sh start.command --doctor
```

`--auto-start` 是明确开启自动点击的选项，只用于实时模式。工具完成当前方案后停止自动点击，不点击“下一关”、奖励或道具按钮。新关卡会重新识别并显示提示，需要自动执行时再从菜单开始。

## 规则和工作方式

- **网格与方向**：先根据本关身体宽度和排列间距估算格距，在格内比较猪头、尾部特征，再组合成长短不同的动物。
- **移动**：动物沿朝向前进，遇到动物就停在障碍前；完全没有阻挡时离场。
- **小鸡／小鸭**：作为临时占格，通路打开后自动找路离开，不安排点击。
- **小象**：当前按两格宽、三格长处理，前进至障碍前，不推开其他动物。
- **数字锁**：锁猪在归零前不移动，并继续占格。每有一只猪离场，剩余锁计数减一；滑动、小鸡／小鸭离场不减。小象是否计数尚未验证，当前保守地不计数。解锁后的猪离场也能帮助打开其他锁。
- **求解**：搜索结果还需用独立几何模拟器逐步回放并清空棋盘。求解在可取消的子进程运行，默认限时 20 秒，外层另有 25 秒看门狗。
- **执行**：沿已算好的顺序点击，并持续跟踪位置、估算速度、预测路径冲突。最多同时跟踪三只动物。原本带锁的猪还要满足实际离场数量，并确认锁身已消失。

画面来自 ScreenCaptureKit 持续视频流，不循环调用单次截图接口，不采集音频或麦克风。视频帧保留在内存；实时模式会在本机保存棋盘、方案和状态 JSON，不上传它们。

默认运行记录目录为 `~/Library/Application Support/Pig Game Guide/`，也可用 `--state-dir /path/to/state` 指定。它不依赖当前终端目录，不读取任何已有 Codex 工作目录。

## 常见问题

| 现象 | 处理方法 |
| --- | --- |
| 找不到 Python、缺依赖 | 安装受支持的 Python，重跑 `install.command`；使用 `start.command` 启动，避免误用系统 Python。 |
| 没有绿点、提示连接镜像 | 确认实体 iPhone 已锁屏、镜像已连接且游戏窗口可见；运行 `check.command` 查看权限。 |
| 自动点击没有开始 | 默认是手动提示模式。检查是否已有完整方案，再选择菜单栏“开始自动点击”；Esc 和设置间隔都会停止点击。 |
| 显示画面不一致或头尾不清 | 提示动画或遮挡可能影响部分帧。恢复完整棋盘；实际手动改动了布局时使用“重新识别”。 |
| 搜索达到上限 | 目前没有完整解法，工具不会继续点击。核对朝向、占格和特殊规则，修正识别后再重新计算。 |
| 锁数字读取超时 | OCR 子进程单次上限 4 秒。实时模式会跳过该帧并保留方案；离线模式会报错，可以再次运行同一分析命令。 |
| 点击间隔比设置值长 | 间隔只是下限；查看状态栏是在等待移动、碰撞区域、视频更新还是锁消失。 |
| 提示工具已经运行 | 使用已有菜单栏“猪”，或先退出原实例；同一用户仅允许一个实时实例。 |

目前不支持斜向箭头、炸弹次数和未见过的特殊外观。小象实机外观验证过朝下和朝左；锁猪外观验证过朝上及数字 20、35，其他情况仍需积累样例。辨识通过也不保证所有关卡都能通关。

## 开发与验证

在本目录运行：

```sh
.venv/bin/python -m unittest discover -s tests -p 'test_*.py' -q
.venv/bin/python doctor.py --offline
.venv/bin/python guide.py --demo --out analysis/demo
```

测试使用随附棋盘或生成的简单图形，自动点击回调已替换为模拟函数，不控制真实手机。`tests/` 覆盖识别、独立模拟、长猪、小象、数字锁、动画追踪、短间隔点击及部署入口。

| 文件 | 作用 |
| --- | --- |
| `install.command` / `start.command` / `check.command` | 安装、启动、检查 |
| `guide.py` / `runtime.py` / `doctor.py` | 命令行入口、环境与路径、诊断 |
| `grid_recognition.py` / `lock_vision.py` / `lock_ocr.py` | 网格、方向和数字锁识别 |
| `solver.py` / `search_engine.py` / `solve_service.py` | 独立模拟、搜索及可取消进程 |
| `controller.py` / `auto_click.py` / `tracking.py` / `motion_guard.py` | 方案跟随、点击和动态碰撞检查 |
| `mac_capture.py` / `visualization.py` | 持续视频帧与计算窗口 |
| `templates.npz` / `long_templates.npz` / `elephant_templates.npz` | 随程序提供的识别参考数据 |
| `recognition.py` | 保留用于历史回归对照的旧识别器；实时入口使用网格识别 |
| `examples/` / `tests/` | 棋盘样例和回归测试 |

卸载时先从菜单退出工具，再删除这个目录及不再需要的运行记录目录即可。没有安装后台服务。

## 许可

原创代码使用 [MIT License](LICENSE)。识别模板和游戏画面样例包含第三方游戏美术，来源与许可范围见 [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md)。
