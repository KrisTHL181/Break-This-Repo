# Windows 裸部署：排查与适配方案

> 裸部署 = 不跑 Docker，直接在宿主机 `java -jar hdsl-web.jar`（Windows 上装 JDK 即可）。
> 本文是 2026-10-16 的一次只读排查：把「Windows 上到底坏在哪、坏在代码的哪一层、怎么修」
> 钉到 `file:line`，并给出「一个 jar 万能」的实现路径。
>
> **当前状态：已实现**（2026-10-16，分支 `feat/windows-bare-deploy`，见 CHANGELOG v0.4.2）。
> 本文最初是一份只读排查，落笔时**代码没有任何一处针对 Windows 的平台分支**（`OperatingSystem.WINDOWS`
> 枚举存在，但除几处 `cmd ver`/`chcp` 探测与启动横幅提示外无人使用）。README / CHANGELOG 当时把
> Windows 裸部署当作「已加固」场景，而 v0.3.6 的实测记录只覆盖**清空 PATH 的容器内**——用户预期
> 被抬高了，那是本文要修的第一件事。
>
> 落地过程中，本文几条**推测**被真机实测**修正**（§9.1），§3 的问题清单已按处理结果重新标注。
> 实例终端在 2026-10-31 改成了 ConPTY（§9.5），不再走桌面窗口。

## 0. 摘要

两个具体疑问的答复（都可由代码直接证实，不需要 Windows 机器）：

| 疑问 | 答复 |
|---|---|
| 传闻「npm 在 Windows 上装了 Linux 版」 | **属实**，但根因不在 npm：面板托管运行时的 `platformTag()` 只特判 macOS，Windows 一律返回 `linux-x64`，`archiveExtension()` 同样只特判 macOS → `.tar.xz`。于是它从 nodejs.org 拉的是 `node-v<ver>-linux-x64.tar.xz`。 |
| 「添加 Node.js 运行环境」在 Windows 有用吗 | **不但没用，还是「可用性假象」**：下载与解包都会成功（解包走 Java 库），Linux 二进制甚至会被登记为「可用」并被自动选中；`bin/npm` 装 pnpm 必然失败。**而且即便用户自己装了官方 Node，面板也探测不到**（`which()` 不认 `.exe`/`.cmd`）——所以「指引用户自行安装」这条路现在也是假的。 |

| | 现状 |
|---|---|
| Windows 上能用的 | 面板本体（Jetty/HTTPS/登录门/静态资源）、纯 Java 的功能（实例台账、日志、反代、证书、导出） |
| Windows 上不能用的 | 托管 Node 运行时一键安装、**探测已有的系统 Node**（连手动装也白装）、dsh 版本安装与插件、三个品牌工具（kimi/opencode/claude-web）的安装与启动、ZCode 面板内构建、实例的自定义命令（`/bin/sh`） |

## 1. 排查：证据链

### 1.1 托管 Node 运行时在 Windows 上下载的是 Linux 版

`src/main/java/org/jackhuang/hmcl/dsh/NodeRuntimeManager.java`

```java
// :73-88  platformTag()
if (OperatingSystem.CURRENT_OS == OperatingSystem.MACOS) {
    return switch (Architecture.SYSTEM_ARCH) {
        case X86_64 -> "darwin-x64";
        case ARM64  -> "darwin-arm64";
        ...
    };
}
return switch (Architecture.SYSTEM_ARCH) {   // ← Windows 落到这里
    case X86_64 -> "linux-x64";
    case ARM64  -> "linux-arm64";
    ...
};

// :113-115  archiveExtension()
return OperatingSystem.CURRENT_OS == OperatingSystem.MACOS ? ".tar.gz" : ".tar.xz";

// :122-124  archiveFileName()  → node-v<ver>-linux-x64.tar.xz
```

证据链（每一步都成立，所以**不报错**）：

1. URL 由 `NodeSource.archiveUrl`（`NodeSource.java:87-89`）拼成 `…/v22.19.0/node-v22.19.0-linux-x64.tar.xz`；
2. 该文件在镜像源上**真实存在**，HTTP 200；
3. 解包用 Java 库（`NodeRuntimeManager.java:527-585`：`TarFileTree` + `org.tukaani.xz.XInputStream`，依赖见 `build.gradle.kts`），**不依赖外部 `tar`**，于是解包也成功；
4. `NodeRuntime.isUsable()`（`NodeRuntime.java:62-63`）只查 `bin/node` **是否存在**——Linux 包解出来正好有这个文件，于是这套 ELF 二进制被登记为「已安装、可用」；
5. `DshNodeRuntime.detectManaged()`（`DshNodeRuntime.java:139-144`）随后自动优先选中它；
6. `provisionPnpm()`（`NodeRuntimeManager.java:322-341`）用 `bin/npm` 去 `npm install --global pnpm@11` —— 在 Windows 上这一步才炸，而报错与「平台装错」毫无关系。

顺带：版本候选列表也只会问 Linux 标签（`indexTag()` `:98-104` 只把 darwin 映射成 `osx-*-tar`；`hasPlatform()` `:216-218` 精确匹配），所以 Windows 用户永远看不到 `win-x64` 构建。

代码里**不存在**任何 `npm_config_platform` / `--os` / `--cpu` / `optionalDependencies` 平台解析 /
`supportedArchitectures` 逻辑（`ZcodeBuilder.java:237-242` 的注释还说明曾经那个 `.npmrc` 平台开关
已被删除，因为 pnpm 不吃）。**「装上 Linux 版」的唯一来源就是上面这三行平台分支的缺失。**

### 1.2 「可用性假象」还有第二层：探测不到用户自己装的 Node

`DshNodeRuntime.java:321-334`

```java
public static Optional<Path> which(String name) {
    for (String entry : path.split(java.io.File.pathSeparator)) {
        Path candidate = Path.of(entry).resolve(name);
        if (Files.isRegularFile(candidate) && Files.isExecutable(candidate)) {   // ← 没有 .exe / .cmd / PATHEXT
            return Optional.of(candidate);
        }
    }
    return Optional.empty();
}
```

Windows 上：`which("node")` 找的是 `…\node`（真实文件是 `node.exe`），`which("npm")` 找的是 `npm`
（真实是 `npm.cmd`）——**两个都返回空**。于是面板判定「本机没有 Node」，把用户推向那个坏掉的托管
运行时；而托管运行时又是 Linux 版。这不是「体验问题」，是**闭环堵死**。

### 1.3 运行时布局：三处硬编码 POSIX 形状

`NodeRuntime.java:38-56`（托管运行时）

```java
public Path node() { return directory.resolve("bin/node"); }
public Path npm()  { return directory.resolve("bin/npm"); }
public Path npx()  { return directory.resolve("bin/npx"); }
```

Windows 官方 zip 解出来的形状是**根目录** `node.exe` / `npm.cmd` / `npx.cmd`，**没有 `bin/`**。
同样的假设还在：`NodeRuntimeManager.adopt()`（`:400-434`，采用用户已有的 Node 安装）、
`DshNodeRuntime.fromManaged()`（`:90-97`，`bin/pnpm` + `Files.isExecutable`）。

### 1.4 子进程派生：`.cmd` 不会被 Windows 自动补上

Java 在 Windows 上通过 `CreateProcess` 派生，只自动补 `.exe`，**不会**补 `.cmd`/`.bat`：
裸 `npm` 报 `CreateProcess error=2`（找不到文件），而面板里所有派生点都是裸 `ProcessBuilder`，
**没有任何一处**做 shell 包装，它们全都直接失败。

> **实测修正（2026-10-16，Windows 10 + JDK 21）**：原文推论「`.cmd` 必须由 `cmd.exe` 执行」
> 是**错的**。`CreateProcess` 在命令行里点了名的批处理会由它自己交给 `cmd.exe`，所以：
>
> | 形式 | 结果 |
> |---|---|
> | 裸 `node` | ✅ 能跑（`CreateProcess` 自己补 `.exe`）|
> | 裸 `npm` / `npx` | ❌ `error=2` 找不到文件 |
> | 裸 `npm.cmd` | ✅ **能跑** |
> | 全路径 `npm.cmd` | ✅ **能跑** |
> | 全路径无扩展名 `npm` | ❌ `error=193` 不是有效的 Win32 应用程序 |
>
> 于是正确修法是**把扩展名补对**，而不是套 `cmd.exe /c` —— 后者会引入一层参数二次转义
> （那正是 util 层 `CommandBuilder.toBatchStringLiteral` 存在的理由），前者参数可以原样透传。
> 实现在 `util/platform/Executables`（`resolve()` 按 `PATH` + `PATHEXT` 补全，Windows 上优先
> `.cmd`、无扩展名的文件退到最后）。

`CommandBuilder` 里那个带 Windows 批处理转义/引号的实现**当前仍然没有任何调用方**——
本轮没有用它，也不需要它。

| 派生点 | 代码 | Windows 上的结局 |
|---|---|---|
| 品牌安装（Claude Web 的 npm 步骤） | `BrandInstaller.java:261-263`、`:280-281` | `List.of("npm", "ci", …)` → `IOException: Cannot run program "npm"` |
| 品牌安装（npm 品牌的 pnpm 回退） | `BrandInstaller.java:134-136` | 同上（`pnpm` 是 `pnpm.cmd`） |
| 品牌实例启动 | `BrandRuntime.java:404-408`（kimi）、`:429-433`（opencode）、`:447-456`（claude-web 的 `.bin/tsx`） | 直接执行 `node_modules/.bin/<tool>`。Windows 上那份**无扩展名**的文件是 POSIX sh 脚本，真正的入口是 `<tool>.cmd`/`.ps1` → `WinError 193 不是有效的 Win32 应用程序` |
| 托管运行时装 pnpm | `NodeRuntimeManager.java:323, 334-341` | `bin/npm` 不存在（布局）＋ `.cmd` 不认（派生） |
| 启动横幅探测 | `Main.java:152-159, 185-193` | `pnpm` 误报 `MISSING`（`node` 能侥幸找到，因为补 `.exe`） |
| 实例自定义命令 | `DshCustomCommands.java:74` `List.of("/bin/sh", "-c", command)` | 无 `/bin/sh` → **启动前命令失败会阻止实例启动** |
| 实例终端 | `DshInstanceTerminal.java:70-72`（x-terminal-emulator / gnome-terminal…）、`:210, 251`（`#!/usr/bin/env bash`）、`:271`（`/bin/bash`） | 找不到终端模拟器 → 「No terminal emulator was found」 |

### 1.5 文件系统语义：符号链接 / 硬链接 / 被占用文件 / 长路径

- **符号链接**（Windows 需 `SeCreateSymbolicLinkPrivilege`：管理员或开发者模式）：
  - `NodeRuntimeManager.java:570-585` 解包 Node 包时**无条件**重建 tar 里的软链（`bin/npm → ../lib/node_modules/npm/bin/npm-cli.js`）→ 非开发者模式下抛 `FileSystemException`，被 `install()` 的 catch（`:273-277`）转成一句「Failed to install Node.js …」；
  - `NodeRuntimeManager.java:447-452`（`adopt()` 复制树）同样；
  - `ZcodeBuilder.java:387-389` 的 `releases/current` 链**没有兜底** → 几十分钟的 `pnpm install + build` 跑完之后，最后一步「安装发行包」整体报错；
  - `BrandInstaller.java:459-468` 的 `current` 链**有** try/catch 兜底（`:428-435` 会退回「最新 release」）——这是可照抄的写法。
- **硬链接**：`CacheRepository.java:166`（缓存去重）；pnpm store 在 Docker 里靠同卷硬链接共享（`docker-compose.yml`），Windows 裸部署默认不设 `PNPM_STORE_DIR`，跨卷/FAT/网络盘会失败。
- **被占用文件**：Windows 不允许删除/改名被进程打开的目录，而多处是 `deleteQuietly(target)` 后紧跟 move（`NodeRuntimeManager.java:288-289`）、卸载走 `FileUtils.deleteDirectory`（`:465-477`）——没有重试。
- **`ATOMIC_MOVE` 无降级**：`NodeRuntimeManager.java:289`、`:422`（其余调用点基本都有「失败退回普通 move」的写法，是正面的对照）。
- **长路径**：数据目录在 `%APPDATA%\.hdsl\instances\<id>\dsh\node_modules\…`，pnpm 嵌套极易越过 Windows 默认 260 字符 `MAX_PATH`；代码里没有 `\\?\` 前缀处理。

### 1.6 路径与环境层

- `ZcodeBuilder.java:423`：`settings.buildBin() + ":" + path` —— **硬编码冒号**，Windows 需要 `;`（`File.pathSeparator`）。其余 PATH 拼接点（`DshNodeRuntime:103/228`、`DshVersionManager:846`、`NodeRuntimeManager:339`、`DshInstanceTerminal:206`）都用对了。
- `PnpmConfigFile.java:73-83`：pnpm 全局配置写在 `${XDG_CONFIG_HOME:-~/.config}/pnpm/config.yaml`。
  **Windows 的 pnpm 不读这个文件**。多数调用点显式传了 `--registry=` 尚可，但 `dsh plugin`
  内部自己转发的 pnpm（`DshPluginInstaller.java:40` 注释明确说了这条路径）会走错源。

  > **实测修正（Windows 10 + pnpm 11.28.6）**：原文猜的是 `%LOCALAPPDATA%\pnpm\config\rc`，
  > 那个也不对。用 `pnpm config set` 之后看哪个文件变了：
  >
  > | 内容 | pnpm 真正读的位置 |
  > |---|---|
  > | registry | `%LOCALAPPDATA%\pnpm\config\auth.ini`，形式 `registry=<地址>` |
  > | 其余（`storeDir`） | `%LOCALAPPDATA%\pnpm\config\config.yaml` |
  >
  > `rc` 与 `~/.config/pnpm/config.yaml` 都**不读**。所以面板写进后者的国内镜像被静默忽略、
  > 一路打到 npmjs.org。`PnpmConfigFile` 现在按平台分派，语法由文件名决定
  > （`.yaml` → `registry: "…"`，`.ini` → `registry=…`）。
- **编码**：子进程输出有的按 `NATIVE_CHARSET` 解（`DshCommand.java:129`、`ManagedProcess.java:155-161`），有的按 UTF-8 解（`BrandInstaller.java:493`、`BrandRuntime.java:538`、`ZcodeRuntime.java:306`）。Windows 中文代码页（936）下二者混用会出现 `?????` 或乱码。
- 数据目录默认 `./data`（相对当前工作目录，`ServerConfigLoader.java:62`）——Windows 上取决于「从哪双击启动」，属体验问题。

### 1.7 进程生命周期

- 只有 dsh 侧枚举子孙进程（`DshProcess.java:361, 373`）；
- `BrandRuntime.stop()`（`:357-365`）与 `ZcodeRuntime.stop()`（`:256-263`）原本**只 `destroy()+destroyForcibly()`**。Claude Web 是 `tsx` 派生 node，Windows 上强杀 tsx 会留下真正占着端口的 node——而 Windows 的 `destroy()` 本身就是 `TerminateProcess`，没有「优雅退出」可言。
  **v0.4.2 已改为终止整棵进程树**（`util/ProcessTree`，见 §9.2.2 的真机复验）。

### 1.8 做对了的地方（改动时别弄坏）

- 解包不依赖外部 `tar`（Java 库）；
- POSIX 权限设置普遍捕获了 `UnsupportedOperationException`（`PnpmConfigFile:168-170`、`setting/SettingsManager.java:115-122`、`UserStore:306-307`、`CertificateManager:652-655`、`PackExports:57-72`、`DshLocalPluginPaths:289-293`），`Unzipper.java:148` 有 `!= WINDOWS` 守卫；
- `ATOMIC_MOVE` 的调用点大多有普通 move 兜底；
- 图标/清单里已经打印了「Windows 上 0600 无 NTFS 语义」的提示（`Main.java:164-172`，与 `README.md:343-346` 一致）。

## 2. 涉及的逻辑层面

适配不是「打几个补丁」，而是**七个层次各自都有平台假设**。按依赖顺序：

| 层 | 负责什么 | 现状 | Windows 症状 | 修法取向 |
|---|---|---|---|---|
| **L1 平台识别** | 回答「我是什么平台/架构」 | `OperatingSystem` 枚举齐备（含 `WINDOWS`），`Architecture` 在 Windows 走 `PROCESSOR_IDENTIFIER`（`Architecture.java:201-208`） | 无 | 已有，直接用；**新增一处「平台能力」查询**（是否 POSIX shell、是否支持符号链接、是否区分 `.cmd`）供其余层使用 |
| **L2 发行物解析** | 平台 → 文件名 / URL / 索引标签 / 解包布局 | 只认 `linux-*` / `darwin-*` | 下载 Linux 包 | 补 `win-x64/win-arm64` + `.zip` + 索引标签 `win-x64-zip`；**纯函数化**便于在 Linux 上测 |
| **L3 运行时布局** | node/npm/npx 在哪 | 硬编码 `bin/*` | 找不到可执行文件 | 抽 `RuntimeLayout`：POSIX(`bin/node`,`bin/npm`) vs Windows(`node.exe`,`npm.cmd` 在根) |
| **L4 子进程派生** | 怎么启动 `.cmd`/脚本/原生二进制 | 全部裸 `ProcessBuilder` | `.cmd` 不可执行 / shim 报 193 | 统一入口：**补出扩展名、不套 shell**（见 §9.1 实测）；裸名走 `PATH`+`PATHEXT`；`.bin/*` 优先 `<tool>.cmd` |
| **L5 文件系统语义** | 符号链接/硬链接/权限/占用/原子替换/长路径 | POSIX 语义 | 建链失败、删不动、超 260 | 能力探测 + 降级策略（junction/copy/逻辑指针）+ 占用重试 + 长路径前缀 |
| **L6 路径与环境** | PATH 拼装、配置目录、编码、HOME | 多数正确，个别硬编码 | PATH 拼坏、pnpm 配置不被读、乱码 | 集中到一处 helper（`pathSeparator`、配置目录按平台、统一 charset） |
| **L7 声明层** | UI/文档/doctor 承诺什么 | 把 Windows 当已加固 | 用户踩空 | 修好前明确标注不支持 + doctor 加 Windows 专检 |

## 3. 问题清单（按严重度）

| 严重度 | 问题 | 位置 | 置信度 |
|---|---|---|---|
| 致命 | 平台标签/扩展名把 Windows 当 Linux | `NodeRuntimeManager.java:73-88, 113-115, 98-104` | 代码证实 |
| 致命 | `which()` 不认 `.exe`/`.cmd`（探不到系统 Node） | `DshNodeRuntime.java:321-334` | 代码证实 |
| 致命 | 裸名派生 `npm`/`pnpm` | `BrandInstaller.java:261-263, 280-281, 134-136` | 代码证实 |
| 致命 | 直接执行 `node_modules/.bin/<tool>` | `BrandRuntime.java:404-408, 429-433, 447-456` | 代码证实 |
| 致命 | 运行时布局硬编码 `bin/*` | `NodeRuntime.java:38-56`、`NodeRuntimeManager.java:400-434`、`DshNodeRuntime.java:90-97` | 代码证实 |
| 致命 | 解包/复制 Node 包无条件建符号链接 | `NodeRuntimeManager.java:570-585, 447-452` | 代码证实（是否失败取决于开发者模式） |
| 致命 | `/bin/sh -c` 硬编码 | `DshCustomCommands.java:74` | 代码证实 |
| 致命 | 实例终端整体 Linux 设计 | `DshInstanceTerminal.java:70-72, 210, 251, 271` | 代码证实 |
| 严重 | ZCode `current` 链失败毁掉整次构建 | `ZcodeBuilder.java:387-389` | 代码证实 |
| 严重 | ZCode PATH 硬编码 `":"` | `ZcodeBuilder.java:423` | 代码证实 |
| 严重 | pnpm 全局配置路径 POSIX | `PnpmConfigFile.java:73-83` | 代码证实（pnpm 在 Windows 的读取路径需实测确认） |
| 严重 | 品牌/ZCode 进程不枚举子孙即强杀 | `BrandRuntime.java:357-365`、`ZcodeRuntime.java:256-263` | 代码证实；**v0.4.2 已修**（`util/ProcessTree`，实测见 §9.2.2） |
| 严重 | 长路径 / 非 ASCII 路径 | 全局无处理 | **需实测** |
| 严重 | 被占用文件删/改无重试 | `NodeRuntimeManager.java:288-289, 465-477` | **需实测** |
| 严重 | pnpm 硬链接 store 跨卷 | `CacheRepository.java:166` + 部署约定 | **需实测** |
| 严重 | 子进程输出编码混用 | `DshCommand.java:129` vs `BrandInstaller.java:493` 等 | **需实测** |
| 严重 | ZCode 依赖外部 `tar` | `ZcodeBuilder.java:224, 376` | **需实测**（Win10 1803+ 自带 bsdtar，大概率可用） |
| 次要 | `ATOMIC_MOVE` 无降级 | `NodeRuntimeManager.java:289, 422` | 代码证实 |
| 次要 | doctor 打印 inotify/容器文案 | `DshDoctor.java:159-173` | 代码证实 |
| 次要 | 横幅把 `pnpm.cmd` 误报 MISSING | `Main.java:152-159, 185-193` | 代码证实 |
| 次要 | 品牌 `current` 链无意义地告警（有兜底） | `BrandInstaller.java:459-468` | 代码证实 |
| 次要 | 数据目录相对 CWD | `ServerConfigLoader.java:62` | 代码证实 |
| 次要 | 默认主目录 `%APPDATA%\.hdsl`（带前导点） | `OperatingSystem.java:263-265` | 代码证实 |

## 4. 适配方法（具体改法）

### 4.1 L2 发行物解析

| | Linux | macOS | Windows |
|---|---|---|---|
| 平台标签 | `linux-x64` / `linux-arm64` | `darwin-x64` / `darwin-arm64` | **`win-x64` / `win-arm64`** |
| 归档扩展名 | `.tar.xz` | `.tar.gz` | **`.zip`**（Node 从不为 Windows 发 tar.xz；也有 `.7z`，不采用） |
| 索引 `files[]` 标签 | `linux-x64` | `osx-x64-tar` / `osx-arm64-tar` | **`win-x64-zip` / `win-arm64-zip`** |
| 解包后根 | `<dir>/bin/…` | `<dir>/bin/…` | **`<dir>/node.exe`、`npm.cmd`、`npx.cmd`、`node_modules/`** |

要点：
- 三个 switch（`platformTag` / `archiveExtension` / `indexTag`）都补 Windows 分支，**并做成纯函数**
  （输入：OS、架构；输出：标签/扩展名）——这样在 Linux 上就能钉住 Windows 的产物名，不需要 Windows CI；
- Windows 走 `.zip` 时解包器要从「TarFileTree + XZ」换成 zip 分支（项目里 `ZipInputStream` 已在
  `BrandInstaller.extractZip` 用过，可抽公共）；
- 顶层目录名固定为 `node-v<ver>-win-x64`（与 Linux 一致），`install()` 里那条「移动顶层目录」的
  逻辑可复用。

### 4.2 L3 运行时布局抽象

引入一个布局描述，而不是在三处各写一遍：

```
RuntimeLayout                              // 由平台决定
  Path nodeExecutable(Path runtimeDir)     // POSIX: bin/node      | Win: node.exe
  Path npmExecutable(Path runtimeDir)      // POSIX: bin/npm       | Win: npm.cmd
  Path npxExecutable(Path runtimeDir)      // POSIX: bin/npx       | Win: npx.cmd
  boolean isUsable(Path runtimeDir)        // 存在的判据（Win 查 node.exe）
  Path binDirectory(Path runtimeDir)       // 塞进 PATH 的目录：POSIX bin/ | Win 根目录
```

改动点：`NodeRuntime.node()/npm()/npx()/isUsable()`、`NodeRuntimeManager.adopt()` 的校验、
`DshNodeRuntime.fromManaged()` 的 `pnpm` 解析、`NodeRuntimeManager.provisionPnpm()` 的 `npm` 解析。
注意 Windows 上 `pnpm` 由 npm 全局装完落在**运行时根目录**（`pnpm.cmd`），不是 `bin/`。

### 4.3 L4 子进程派生

统一到一个派生 helper（或复活 util 层那个 `CommandBuilder`，它已有 Windows 引号/转义实现）：

- `.cmd` / `.bat` → **补出扩展名后直接交给 `ProcessBuilder`**，不带 shell：`CreateProcess` 会把点名
  的批处理自己转给 `cmd.exe`，参数原样透传（套 `cmd /c` 反而多一层转义）。实测见 §9.1；
- **裸名解析**要按 `PATHEXT` 逐个扩展名试（`node` → `node.exe`；`npm` → `npm.cmd`）；
- `node_modules/.bin/<tool>`：Windows 上优先 `<tool>.cmd`（其次 `.ps1`），**绝不要执行无扩展名那份**
  （它是 POSIX sh 脚本）；
- `/bin/sh -c <cmd>` → Windows 上 `cmd /c <cmd>`（`DshCustomCommands`）；
- 实例终端：Windows 上不找 `x-terminal-emulator`；要么走 `wt`/`powershell`，要么明确「本平台暂不支持」。

**这一层是 Windows 三条链路（装 dsh / 装品牌 / ZCode 构建）共同的第一失败点**，优先级最高。

### 4.4 L5 文件系统语义

- **符号链接**：先探测能力（试建一次临时链接）；不支持时按用途降级：
  - 解包 Node 包里的 `bin/npm` 这类软链 → **复制**成真文件（或只对 npm/npx 两个入口做复制）；
  - `releases/current` 这类「指针」→ 写一个文本指针文件 + 读侧解析（比 junction 更省事且不需要权限），
    或直接退回「最新 release」（`BrandInstaller` 已有这个兜底，照抄给 ZCode）；
- **被占用文件**：删除/改名加「重试 + 短暂延迟」，失败信息写清「文件被占用」；
- **`ATOMIC_MOVE`**：补普通 move 兜底（照抄其余调用点的写法）；
- **长路径**：数据目录给出「短路径」建议写在 doctor 里；必要时对超长路径加 `\\?\` 前缀（Java 侧
  需谨慎，仅在确有必要处引入）。

### 4.5 L6 路径与环境

- `ZcodeBuilder.java:423` 的 `":"` → `File.pathSeparator`；
- pnpm 全局配置路径按平台（Windows：registry 写 `%LOCALAPPDATA%\pnpm\config\auth.ini`，`storeDir`
  写同目录 `config.yaml`；`rc` 不被读取——实测见 §9.1），或**不写文件**、把 registry 一律作为命令行
  参数传（`--registry=` 已经覆盖多数点，剩下 `dsh plugin` 内部那条只能靠文件）；
- 子进程输出解码统一到一处（按平台选 UTF-8 或 NATIVE_CHARSET），别一半一半；
- 启动横幅的 `toolVersion` 走 4.3 的 helper（否则 Windows 永远误报 `pnpm MISSING`）。

### 4.6 L7 声明层（先做，成本最低）

- 设置页「Node.js」：Windows 上在托管运行时安装按钮旁**明确**写明当前不可用，并给出替代；
- `DshDoctor`：加 Windows 专检（符号链接权限、启动器用户目录长度、控制台代码页、实例终端不可用），
  把「为什么装不了」直接说出来；
- README/CHANGELOG：把「Windows 裸部署已加固」的口径改成实际覆盖范围（Linux/macOS 容器外裸机），
  或注明「Windows 见 docs/windows-bare-deploy.md 的当前限制」。

## 5. 「一个 jar 万能」怎么实现

用户倾向单 jar，这也是**技术上正确的选择**。三种做法的对比：

| 做法 | 评价 |
|---|---|
| **构建期自动适配**（按构建机平台打包） | ❌ 解决不了问题：Windows 用户拿到的是「同一个 jar」，平台差异是**运行时事实**（同一个 jar 在 Windows 上跑就是 Windows 行为）。构建期唯一真正需要适配的是**打包产物**（启动器脚本等），不是代码路径。 |
| **分支改进**（Windows 版单独维护） | ❌ 两套代码分叉：每个 bug 修两遍，测试两份，且差异全在底层工具（正是最容易踩雷的地方）。历史上这类分叉都退化成一个分支长期腐烂。 |
| **单 jar + 运行时分派**（推荐） | ✅ 平台差异集中在 L1–L6 六层的少数几个纯函数与 helper 里；运行时读 `OperatingSystem.CURRENT_OS` 分派；**测试用注入**（在 Linux CI 上钉住 Windows 的产物名/布局/命令行形状），真机只验证「那些纯函数说不清的事」（权限、占用、长路径、编码、bsdtar）。 |

单 jar 的**天然边界**（哪些做不到，只能声明不支持或降级）：

- 实例终端依赖本机终端模拟器与 bash 脚本 → Windows 上要么换成 `wt`/`powershell`，要么标记不支持；
- POSIX 专用外部命令（`flock`，已有降级）与 `/bin/sh` 脚本内容 → 用户写的自定义命令本身可能就不是跨平台的；
- 原生二进制（各 CLI 的 `node-pty` 等）由 npm 按平台解析，属上游行为——只要 L4 的派生正确，它们会自己选对平台。

## 6. 测试与验收

### 6.1 单元层（在 Linux 上就能钉住，无需 Windows）

已落地（都在 `src/test/java`，本机即可跑）：

1. `NodeArtifactNamingTest`：`platformTag/indexTag/archiveExtension/archiveFileName` 注入
   `Platform.WINDOWS_X86_64`/`WINDOWS_ARM64`，断言 `win-x64`/`win-x64-zip`/`.zip`/
   `node-v22.19.0-win-x64.zip`；Linux/macOS 的旧名字、以及「Node 不发布的平台要拒绝」同样钉住；
2. `RuntimeLayoutTest`：假的 Windows 运行时段（`node.exe`/`npm.cmd`/`npx.cmd`/`pnpm.cmd`）断言
   `node()/npm()/npx()/pnpm()/binDirectory()/isUsable()`；POSIX 形态，以及「拿 Windows 布局看
   POSIX 目录必须不认」的反例同样钉住；
3. `ExecutablesTest`：Windows 规则下 `.cmd` 优先于无扩展名脚本、只有无扩展名时的兜底、`PATH`
   逐项搜索、带目录的名字不当裸名搜索；**POSIX 规则下执行位必需、答案是 realpath、不会凭空补
   扩展名**——POSIX 的语义就是「不能因为 Windows 适配而改变」的那一半；
4. `DshCustomCommandsTest`：`cmd /c` 与 `/bin/sh -c` 两种包装（整行作为单个参数）；
5. `DshVersionManagerRollbackTest`：安装失败/中断时旧版本被挪回原位、已发布时清掉备份、
   只剩半成品时删干净。

只能在真机验的（§6.2）：`PATH` 分隔符与目录拼装的**平台值**、`.cmd`/原生二进制的实际启动、
文件占用重试、长路径、控制台编码、bsdtar。

### 6.2 真机验收清单（Windows）

按顺序做，每步记结果：

| # | 场景 | 期望 |
|---|---|---|
| 1 | 未开开发者模式、未装 Node：设置页点「安装托管运行时」 | 装出 **win-x64** 运行时（`node.exe` 在根），装完版本列表可用；或明确报错说清权限原因 |
| 2 | 开开发者模式重做 1 | 同上（符号链接路径也能过） |
| 3 | 用官方 Windows 安装包装了 Node 22：重启面板 | 横幅显示 `node x.y.z, pnpm x.y.z`（不再误报 MISSING），设置页显示系统运行时 |
| 4 | 装一个 dsh 实例（新版本 + 插件） | 安装成功，插件安装走对 registry |
| 5 | 装 kimi / opencode / claude-web 各一个实例并启动 | 启动成功、`/i/<id>/` 打开可用（claude-web 还要能连上 WS、项目菜单里有工作区） |
| 6 | ZCode 面板内构建一次 | 构建成功；`current` 指针可用 |
| 7 | 数据目录放在含中文/空格的路径；再放一个深目录（路径 > 200 字符） | 上述功能仍可用，或给出可诊断的报错 |
| 8 | 中文代码页（936）下看安装日志 | 中文不乱码 |
| 9 | 实例终端 | 明确可用或明确「本平台不支持」（不能只是报错） |
| 10 | 进程停止：启动品牌实例后点停止，看端口是否释放 | 无残留 node 占用端口 |

2026-10-17 的复验（§9.2.2）覆盖了 3、4、5（opencode）、8、9、10 与 7 的中文/空格部分；
1/2 的托管运行时下载路径已验证（该机另有开发者模式权限，未单独测「无权限」分支）。
6（ZCode 构建）于 2026-10-18 在同一类真机上跑通（v3.14.3），细节见
[`report-2026-10-18-zcode-windows-build.md`](report-2026-10-18-zcode-windows-build.md)：
构建工具链由面板自供（Node 24+、pnpm、`tar.bat` shim），不需要 MSVC；
7 的 >260 字符深路径仍未跑。

### 6.3 仍需实测的点（本文无法在 Linux 上定论）

`bsdtar` 是否可用（ZCode 解包）、pnpm 硬链接 store 跨卷、被占用文件的删改、控制台编码、
长路径阈值、符号链接在目标机器的权限——这些只能按 6.2 的真机清单逐条定性。

## 7. 落地计划

| 阶段 | 内容 | 工作量 | 完成判据 |
|---|---|---|---|
| **0 · 诚实 + 探测/派生** | L7 声明层（UI/doctor/文档说清 Windows 现状）＋ 4.2 布局抽象 ＋ 4.3 派生 helper（探测与派生是「自己装 Node」这条路的前提） | ~1 天 | 真机清单 3/4 通过：官方 Node 能被识别，dsh 实例能装 |
| **1 · 托管运行时可用** | 4.1 发行物解析（win-x64/.zip/索引标签）＋ zip 解包分支 ＋ 4.4 符号链接降级 | ~2–3 天 | 真机清单 1/2/5 通过 |
| **2 · 长尾** | 进程树终止（`taskkill /T /F`）、占用重试、长路径、编码统一、ZCode 的 PATH 与 `current`、终端声明 | ~1–2 天 | 真机清单 6–10 通过 |

阶段 0 之后，「Windows 上自己装官方 Node + 装 dsh 实例」应当**真的能用**——这是当前最刺眼的
前后不一致（文档说可以，代码做不到）。阶段 1 之后托管运行时与品牌工具才谈得上可用。

## 8. 相关代码一览

| 文件 | 角色 |
|---|---|
| `src/main/java/org/jackhuang/hmcl/dsh/NodeRuntimeManager.java` | 托管 Node 运行时的下载/解包/安装/pnpm 供给（**问题最集中**） |
| `src/main/java/org/jackhuang/hmcl/dsh/NodeRuntime.java` | 运行时目录的可执行文件布局 |
| `src/main/java/org/jackhuang/hmcl/dsh/DshNodeRuntime.java` | 系统/托管运行时探测、PATH 注入、`which()` |
| `src/main/java/org/jackhuang/hmcl/dsh/{NodeSource,NodeRelease}.java` | 下载源与版本元数据 |
| `src/main/java/org/jackhuang/hmcl/util/platform/{OperatingSystem,Architecture,CommandBuilder}.java` | 平台识别与（当前无人使用的）Windows 命令行处理 |
| `src/main/java/org/jackhuang/hmcl/web/brand/{BrandInstaller,BrandRuntime}.java` | 品牌工具安装与启动 |
| `src/main/java/org/jackhuang/hmcl/web/zcode/{ZcodeBuilder,ZcodeRuntime}.java` | 面板内构建与启动 |
| `src/main/java/org/jackhuang/hmcl/dsh/{DshCustomCommands,DshInstanceTerminal,DshProcess}.java` | 自定义命令、终端、进程生命周期 |
| `src/main/java/org/jackhuang/hmcl/dsh/PnpmConfigFile.java` | pnpm 全局配置写入位置 |
| `src/main/java/org/jackhuang/hmcl/web/Main.java`、`dsh/DshDoctor.java` | 启动横幅体检、doctor |

## 9. 落地与实测（2026-10-16）

环境：真 Windows 10（19045）、JDK 21.0.12、Node v24.22.0、npm 11.19.1、pnpm 11.28.6（实测时装的）。

### 9.1 被实测推翻/修正的推测

| 本文原推测 | 实测结果 |
|---|---|
| §1.4：`.cmd`/`.bat` 必须由 `cmd.exe` 执行 → 要套 `cmd /c` | **错**。`CreateProcess` 会自己把点了名的批处理交给 `cmd.exe`：裸 `npm.cmd`、全路径 `npm.cmd` 都能跑；裸 `npm` 是 `error=2`，无扩展名全路径是 `error=193`。修法是补扩展名，不是包 shell。 |
| §3：`PnpmConfigFile` 在 Windows 应写 `%LOCALAPPDATA%\pnpm\config\rc` | **也不对**。registry 写在 `%LOCALAPPDATA%\pnpm\config\auth.ini`（`registry=<地址>`），`storeDir` 在 `%LOCALAPPDATA%\pnpm\config\config.yaml`；`rc` 和 `~/.config/pnpm/config.yaml` 都不读。 |
| §3：ZCode 依赖外部 `tar`「需实测」 | **可用，但有陷阱**。Win10 自带 `System32\tar.exe`（bsdtar 3.5.2）支持本处用到的全部参数；但装了 Git for Windows 时 `usr\bin` 里的 **GNU tar** 会排在前面，而 GNU tar 把 `C:\…` 当成**远程主机 `C`**，报 `Cannot connect to C: resolve failed`。所以按路径点名 `System32\tar.exe`。 |
| §1.5：符号链接失败「取决于开发者模式」 | **确认**。未开开发者模式的普通账户下，建链抛 `FileSystemException: ……current: 客户端没有所需的特权`。 |
| §3 次要：`ATOMIC_MOVE` 无降级 | **确认**，且跨卷时 Windows 直接拒绝。 |

### 9.2 真机验证通过的部分

按 §6.2 的清单逐条跑（`acc-dsh` = 一个真的 dsh 实例，经面板 `/i/<id>/` 反代）：

| §6.2 | 结果 |
|---|---|
| 3 · 识别系统装的 Node/pnpm | ✅ 横幅从 `pnpm MISSING` 变成 `pnpm 11.28.6`；`GET /api/node` 报出 `node.exe`/`npm.cmd`/`pnpm.cmd` 三条完整路径（改之前三条全空——这是 31 个失败里 15 个的同一个根因）|
| 1 · 一键装托管运行时 | ✅ 装出 `runtimes/26.12.0`：`node.exe` 打 `v26.12.0`、`pnpm.cmd` 打 `11.28.6`，**根目录没有 `bin/`**；`/api/node` 的 `managed[].pnpm` 为 `true` |
| 4 · 装一个 dsh 实例 | ✅ 601 个包装完、`Installed 0.2.0-rc.2`；`node_modules/@deepseek-ai/dsh` 的链接指向最终路径，`package.json` 读回 `0.2.0-rc.2` |
| 4 · 启动并使用 | ✅ `state=RUNNING`，经 `/i/acc-dsh/?token=…` 得到 `200`、34782 字节、`<title>DeepSeek Harness</title>`，页面带服务端注入的 `<base href="./">` |
| 9 · 实例终端 | ✅ 明确不支持：doctor 打印「not available on this platform (it opens a POSIX shell through a Linux terminal emulator)」，启动横幅同款提示 |
| 10 · 停止后释放端口 | ✅ 停止前 `127.0.0.1:3937` LISTENING，停止后 `state=STOPPED` **且该端口无监听**。（`/i/acc-dsh/` 答 **502** 而非 404，这是设计如此——`ProxyBehaviorTest` 的用例名就写着 `StoppedInstanceIs502`；plan.md 里的「即刻 404」是旧口径。）|
| 8 · 中文代码页下的日志 | ✅ 控制台编码 `GB18030`，进度「已写入 529 / 601 个包」无乱码 |
| — · 全量测试 | ✅ **559 用例 / 0 失败 / 0 错误 / 6 跳过**（改之前 31 失败；6 个跳过里 5 个原本就有）|
| 6 · ZCode 面板内构建 | ✅ 2026-10-18 真机跑通 v3.14.3（约 80 MB 发行包）。缺的不是编译器，而是 Node 24+/pnpm/bsdtar——面板已改为自供，见 [`report-2026-10-18-zcode-windows-build.md`](report-2026-10-18-zcode-windows-build.md) |
| 5 · 品牌工具（kimi/opencode/claude-web） | ⬜ 未跑（几百 MB 下载）。依赖机制已验：`node_modules/.bin/<tool>` 补全到 `.cmd`（`BrandRuntime.entryPoint`）、`npm`/`pnpm` 走统一派生、`HOME`+`USERPROFILE` 双设 |
| 7 · 含中文/空格、超深的数据目录 | ⬜ 未跑（本次数据目录是纯 ASCII 短路径）|

### 9.2.1 真机挖出的新问题：安装「先暂存再搬」在 Windows 上必然坏

这是本文原本**没有**列出的项，只有真机真的装一次实例才会暴露。`DshVersionManager.install()`
原来让 pnpm 在 `dsh.installing/` 里安装，成功后把整棵树 `Files.move` 到 `dsh/`。POSIX 上 pnpm
写的是**相对**软链，搬了照样能用；**Windows 上 pnpm 用 junction，目标必须是绝对路径**，
于是搬过去的树里每条链接都还指着刚被删掉的 `dsh.installing/`。现象极具误导性：pnpm 退出码 0、
601 个包全下完、`.bin/dsh.CMD` 也在，只有 `node_modules/@deepseek-ai/dsh` 是条死链——
`package.json` 于是读不出来，面板报
「pnpm reported success, but the installed version could not be read back」。

已修：pnpm 直接写在实例最终要用的路径上；`dsh.installing` 降级为「还在写」的旗标，
`isInstalled()` 见到它就答「没装」，`discardPartial()` 连同半成品目录一起清掉——中断的安装
仍然不会被误当成可用版本。修完复测：`Installed 0.2.0-rc.2`，链接指向最终路径。

> 教训：这条**不可能**靠读 Linux 上的代码发现（形状完全一样、Linux 上就是对的）。
> 它是「真机跑一遍」相对「静态排查」多买到的东西。

### 9.2.2 v0.4.2 候选 jar 的复验（2026-10-17）

同一台机器（Windows 10 19045、JDK 21.0.12、系统 Node 24.22.0、pnpm 11.28.6），用
`feat/windows-bare-deploy` 构建出的 jar 从**空数据目录**重跑；数据目录分别放在纯 ASCII
与含中文/空格的两处。

| 场景 | 结果 |
|---|---|
| 首启 | ✅ `HDSL_ADMIN_PASSWORD` 建号、登录、`/api/doctor` 全绿；横幅 `runtime: node v24.22.0, pnpm 11.28.6` |
| 托管运行时（win-x64） | ✅ 装出 `26.12.0`：根目录 `node.exe`/`npm.cmd`/`pnpm.cmd`（无 `bin/`），`node -v`、`pnpm -v` 可用 |
| dsh 实例 | ✅ `0.2.1-rc.1`、626 个包**就地**装完，`dsh.installing` 装完即消失；`/i/acc-win/?token=…` 反代 **200**（37023 字节、`<base href="./">`）；停止后端口无监听 |
| 品牌实例（opencode 1.19.3） | ✅ 装、启、反代 200（`<title>OpenCode</title>`）；停止后状态 `stopped`、端口释放、`opencode.exe` 无残留 |
| 中文 + 空格路径 | ✅ `C:\Users\timmy\hdsl-测试 空格` 下同样装、启、反代通过（链接为相对软链，`dsh.CMD` 就位）|
| HTTPS + 裸 IP | ✅ 自签证书：`https://192.0.2.10:3080/` → 200；`http://192.0.2.10:3080/`（含 `/api/health`）→ **308** 到 https |
| 启动前自定义命令 | ✅ `cmd /c` 执行 `echo … > pre-win.txt`，文件落在工作目录，实例正常进入 RUNNING |
| ZCode 面板内构建（v3.14.3） | ✅ 2026-10-18 从空数据目录连跑两次（全新 + 重建）都成功（约 80 MB 发行包）：面板自供 Node 26.12.0、pnpm 与 bsdtar shim；`cpu-features` 无 MSVC 时编译失败但按可选依赖继续，`node-pty` 用预编译——**不需要编译器**。详见 [`report-2026-10-18-zcode-windows-build.md`](report-2026-10-18-zcode-windows-build.md) §8.1 |
| 全量测试（Linux 侧） | ✅ **584 用例 / 0 失败 / 0 错误 / 0 跳过** |

未复跑：kimi 品牌（需要账号）、claude-web（GitHub 下载 + 构建）、>260 字符的长路径。

### 9.3 仍未做 / 明确不支持

| 项 | 处置 |
|---|---|
| 实例终端 | **Win10 1809+ 可用（ConPTY，§9.5）**；8.1 及以下、以及更早的 Win10 明确不支持，终端接口 409 `terminal_unsupported_platform` 并带实际 build 号。 |
| `flock` 会话租约 | Windows 直接答「观察不到」，不再每次去 spawn 一个必然失败的进程。 |
| 关闭实例后端口/进程是否真的释放 | ✅ dsh 实例实测：停止后端口即刻无监听。**品牌/ZCode 在 v0.4.2 改为终止整棵进程树**（`util/ProcessTree`）；opencode 复验：停止后 `listeners=0`、`opencode=0`，状态保持 `stopped`（不再被输出泵误报成「意外退出」）。 |
| 子进程输出编码混用 | ⬜ 未逐处实测；控制台为 GB18030 时中文进度无乱码（§9.2 第 7 行）。 |
| pnpm 硬链接 store 跨卷 | ⬜ 未实测（需要一个跨卷的数据目录）。 |
| 长路径（>260） | 未实测；doctor 打印数据目录长度并在 >120 字符时给出提示。 |

### 9.4 复现方法（真机）

```bash
# 全量测试（Windows）
gradlew.bat test

# 单跑某一类
gradlew.bat test --tests "org.jackhuang.hmcl.dsh.PnpmConfigFileTest"

# 裸部署 + 体检
set HDSL_DATA=%CD%\data
set HDSL_PORT=3080
java -jar build\libs\hdsl-web-<版本>.jar
java -jar build\libs\hdsl-web-<版本>.jar -doctor
```

派生结论的复现：写一个 `ProcessBuilder` 小程序，分别跑裸 `node`、裸 `npm`、裸 `npm.cmd`、
全路径 `npm.cmd`、无扩展名全路径 `npm`，看 error code（§1.4 的表就是这样做出来的）。

**别在 SSH 会话里 `Start-Process` 常驻面板。** Windows 自带的 OpenSSH 把登录 shell 放进一个
job object，会话一断就把整棵树带走：面板和你从它拉起的实例一起消失，日志最后一行还停在
"listening"，看着像自己退了。要常驻就用**任务计划程序**（触发器 "计算机启动时"，勾选
"不管用户是否登录都运行"）或 nssm 注册成服务，两者都不挂在任何交互式会话的 job 上。
临时排查时才用交互式会话，且那条会话断之前别指望它活着。

### 9.5 实例终端：ConPTY 落地记录（2026-10-31）

Windows 上的实例终端从「桌面控制台窗口」换成浏览器 PTY，实现是系统自带的
`CreatePseudoConsole`（`web/terminal/pty/ConPty.java`）。桌面终端（`DesktopTerminal`、
`{"mode":"desktop"}`、`open-desktop-terminal` 确认短语）整类删除。

- **门槛**：build ≥ 17763（Win10 1809），并且 `kernel32` 真有 `CreatePseudoConsole` 符号。
  `-doctor` 与启动横幅都直接打印这一行：`ConPTY (Windows 10.0 build 19045) [ok]`，
  或者 `unsupported: needs build 17763, this is <n>`。
- **验证过的**（本机 Win10 19045 / JDK 21）：`cmd.exe /c echo` 往返、UTF-8 输出、
  `mode con` 前后的 `Columns` 随 `ResizePseudoConsole` 变化、退出码与 EOF、
  `cmd /c ping` 的**整棵树**随 `terminate` 消失、4 KB 输入分块写入不报错。
  用例在 `ConPtyWindowsTest`，其它平台自动 skip。
- **服务形态**：不再依赖桌面（`DesktopIntegration.supported()` 与终端无关了）。session 0 下
  也能起 ConPTY——这是本次换掉桌面终端的直接原因。
- **已知差异**：Windows 没有能打到伪控制台子进程的礼貌信号，`terminate` 是「宽限后
  `TerminateJobObject`」；一个实例终端 = 一个 `conhost.exe`；只承诺 64 位 JVM。
- **未验证**：arm64 Windows；真机中文输入法（候选框/回显）；真实品牌的 TUI 长时间运行。
  这几条没有一手证据，按「未验证」对待，不要写成已支持。

### 9.6 MC 服务端（2026-11-02）

MC 在 Windows 上走 headless `pipes`（日志 + 命令框），不走 ConPTY。本轮补的是这条路径的
生命周期与可解释性，**还没有真机 Windows 验收**；Linux 侧的 `McRuntimePipeTest` 用
`ptySupported=false` 覆盖同一段代码。

- 面板退出时 `McRuntime.stopAll()` 会先写 `stop`，超时才 `ProcessTree.terminate`；
  之前 pipe 进程不在 `terminals` 里，关面板后会留下占端口的 `java.exe`。
- 启动前 `McPorts.isFree()` 做真实 bind 探测；删除实例前 `ProcessTree.killReferencing`
  收掉命令行里带该实例目录的残留进程（`server.jar` 已改为绝对路径）。
- `mc.java_path` 默认解析到面板自己的 JVM；`GET /api/mc/feature` 暴露路径/版本/来源，
  设置页在功能关闭时也能看到。配置了不存在的 Java 会在启动前明确报错。
- 子进程固定 UTF-8；`-Xms` 不再固定 512M（创建表单允许 256M）。
- 存储根默认 `%USERPROFILE%\mc`，`HDSL_MC_DIR` / `mc.dir` 可改；SYSTEM 账户、超长路径、
  不可写目录会在启动时警告。

真机验收顺序见 `docs/minecraft.md` §15.2 与 §6.2；2026-11-02 已按该顺序在真机跑通一次
（§9.7），`1.21.11` 起服到 `Done`、控制台有回显、停止后端口释放。版本与 Java 的对应见 §9.7。

### 9.7 全功能裸部署冒烟（2026-11-02）

环境：Windows 10 `19045.6466`、JDK 21.0.12、**PATH 里没有任何 Node**（模拟只装了 JDK 的
干净机器），面板用发布 jar 起在 `127.0.0.1:3080`（HTTP），由宿主机经 SSH 隧道驱动。

| 面 | 结果 |
|---|---|
| 接口 42 项（登录 / health / node / doctor / 品牌版本 / 证书窗口 / ZCode / 半安装 / 终端四道闸 / SPA） | ✅ 除「干净机器没有 JS 工具链」按设计报 fail 外全过 |
| 一键装托管运行时 | ✅ `26.13.0`（`win-x64.zip`，根目录 `node.exe`）+ `pnpm 11.28.10` |
| dsh `0.2.2-alpha.2` | ✅ 装、启、`/i/<id>/` 200；ACP 控制台收到回复 |
| kimi `2.4.1` / opencode `1.19.5` | ✅ 装、启、反代 200；各自 `acp` 子命令握手 + 问候 |
| MC | ✅ enable → 下 jar → `Done (2.646s)` → 25565 监听 → 控制台 `list` 有回显 → stop 后端口释放 |
| 实例终端（ConPTY） | ✅ ticket → WS `ready` → 3986 字节 ANSI（kimi TUI）→ `resize` 无报错 → `DELETE` 收进程 |

**冒烟抓到的缺陷：ACP 中文被按控制台代码页解码**

`ManagedProcess.pumpInputStream` / `pumpErrorStream` 原来固定用
`OperatingSystem.NATIVE_CHARSET`；中文 Windows 上是 GBK，而 ACP 的 JSON-RPC 行是 UTF-8。
结果是 ACP 控制台里的中文回复变成「浣犲ソ」这类乱码，个别字节还会直接丢失（实测回复里的
「一」就丢了）。

已修：`ManagedProcess` 增加带 `Charset` 的重载，`DshAcpClient` 的 stdout / stderr 明确用
UTF-8。复验：同一句问候返回「你好，很高兴见到你！😊」。全量测试 1182 项 / 2 失败
（`BrandTuiTest`，宿主机没 node）/ 74 跳过，与基线一致。

**MC 的版本—Java 对应（真机结论）**

`26.3` 要求 Java 25+，面板在启动前明确拒绝（"needs Java 25 or newer, but `java` is Java
21"）；只装了 JDK 21 的机器应选 `1.21.x`（本次用 `1.21.11` 跑通）。这条错误信息本身是
可行动的，不是「启动失败」。
