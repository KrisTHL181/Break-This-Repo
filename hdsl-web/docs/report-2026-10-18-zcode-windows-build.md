# ZCode 在 Windows 上构建：到底缺什么（2026-10-18 实测报告）

> 结论先行：**v3.14.3 的 ZCode 构建在 Windows 上不需要 C++ 编译链**。没有 MSVC、没有  
> Visual Studio Build Tools、没有 `make`，`pnpm install` 与 `pnpm build:zcode` 都能跑完。  
> 真正会挡住构建的是三件**非编译器**的东西：Node 版本、pnpm 的供给、`tar` 的解析。  
> 本文给出证据链、失败原文，以及「把工具链放进数据目录」的最小方案。
>
> 实测环境：Windows 10 19045 / x64，JDK 21.0.12，系统 Node 22.22.2，pnpm 11.28.6，  
> 面板以 `java -jar` 裸部署，数据目录在纯 ASCII 路径下。ZCode 上游 tag `v3.14.3`。

## 0. 一张表看完

| 构建需要的东西               | Windows 上有吗          | 面板能提供吗                                     | 实测结论                          |
| --------------------- | -------------------- | ------------------------------------------ | ----------------------------- |
| Node.js **24.14+**    | 系统装的是 22（dsh 支持的线）   | ✅ 设置 → Node.js 一键装到 `<data>/hdsl/runtimes` | **必须**；22 不够                  |
| pnpm                  | 全局装过就有，没有就无          | ⚠️ 一键装的 runtime 里**不含** pnpm               | **必须**；要么 PATH 有，要么装进 runtime |
| `tar`（bsdtar）         | ✅ `System32\tar.exe` | ⚠️ 但 PATH 上常先命中 Git 的 GNU tar              | **必须**；GNU tar 不认 `C:\`       |
| MSVC / VS Build Tools | ❌ 没有                 | ➖ 不需要                                      | **用不到**（node-pty 有预编译）        |
| Python                | ✅ 3.13 在             | ➖ 不需要                                      | 只有走 node-gyp 时才需要             |
| git                   | ✅ 有                  | ➖ 不需要                                      | 下载走 codeload zip/tar，不碰 git   |
| 7z / make             | ❌ 没有                 | ➖ 不需要                                      | 上游用 `tar` 与 Node 自带工具         |

## 1. 为什么“不缺编译器”——证据

### 1.1 node-pty 在 Windows 用预编译产物

`pnpm install` 的日志里，desktop 包的 postinstall 明确跳过重建：

```
packages/desktop postinstall: [node-pty] skip electron-rebuild (windows-prebuild-available):
  <data>/zcode/build/src/node_modules/node-pty/prebuilds/win32-x64/pty.node
packages/desktop postinstall: Done
Done in 1m 44.3s using pnpm v10.33.2
```

`node_modules/node-pty/prebuilds/` 下随包发布了 `win32-x64/pty.node`、`conpty.node`、  
`winpty.dll`、`winpty-agent.exe`（本机另用 kimi 的安装树核对过同一份文件）。node-pty 的  
`install` 脚本是 `node scripts/prebuild.js || node-gyp rebuild` —— 预编译命中就不会落到  
node-gyp，也就不会去要 Python / MSVC。

### 1.2 上游的架构清单覆盖了 win32

`pnpm-workspace.yaml`：

```yaml
supportedArchitectures:
  os: [current, darwin, linux, win32]
  cpu: [current, x64, arm64]
  libc: [current, glibc]
allowBuilds:
  cpu-features: true
```

所以组包阶段为六个平台收集原生资源时，Windows 这一份本来就在清单里，不会因“缺一个平台”  
而失败。

### 1.3 整个构建里没有 node-gyp / MSBuild 调用

构建日志里没有出现 `gyp`、`MSBuild`、`cl.exe`、`Visual Studio` 任何一行；`pnpm build:zcode`  
的失败点也与编译无关（见下一节）。真正用到的外部程序只有 `pnpm` 与 `tar`。

## 2. 真正挡住构建的三件事（附原始报错）

### 2.1 Node 版本：系统 22 不够，上游要 24.14+

- 面板设置 → Node.js 一键装 `26.12.0` 到 `<data>/hdsl/runtimes/26.12.0`，Windows 布局是  
  根目录 `node.exe`、`npm.cmd`（POSIX 才是 `bin/node`）。
- 但**装好不等于构建会用**：ZCode 构建的 `buildBin` 只从 `HDSL_ZCODE_BUILD_BIN` 读，裸部署  
  默认为空，于是构建仍用 PATH 上的 22。
- 本次验证：把 `buildBin` 回退到「托管 runtime 中 major ≥ 24 的最新一个」后，构建进程树里  
  出现了 `...\runtimes\26.12.0\node.exe`，安装与构建均正常。

### 2.2 pnpm：一键装的 runtime 里没有它

面板的一键 Node 是**官方发行包**，只有 `npm`。构建命令是 `pnpm install`，裸机上没装 pnpm  
就直接 `ENOENT`。

实测的补齐方式（也是本次验证过的路径）：用 runtime 自带的 `npm` 装进同一个目录——

```
npm install --global pnpm@11 --prefix <data>\hdsl\runtimes\26.12.0
```

结果是该目录下出现 `pnpm.cmd`（本机已核对：删除 runtime 后重新触发构建，`26.12.0` 目录里  
`node.exe` 与 `pnpm.cmd` 同时到位）。**这一步应该由面板做，而不是让用户自己开命令行。**

### 2.3 `tar` 解析：PATH 上的 GNU tar 不认盘符

`pnpm build:zcode` 的最后一步是把发行目录打成 tarball，上游脚本直接 `spawnSync("tar", ...)`：

```
[zcode] tar -czf <data>\zcode\build\out\releases\3.14.3\zcode-3.14.3.tar.gz
        -C <data>\zcode\build\out\.work zcode
tar (child): Cannot connect to C: resolve failed
tar: ... Cannot write: Broken pipe
tar: Error is not recoverable: exiting now
```

原因：PATH 上先命中的是 Git for Windows 的 GNU tar，它把 `C:\…` 当成名为 `C` 的**远程主机**。  
Windows 自带的 `System32\tar.exe` 是 bsdtar，处理 `C:\` 正常。

实测解法：在数据目录里放一个 shim，并把它排在 PATH 最前：

```
<data>/zcode/build/bin/tar.bat
    @echo off
    "%SystemRoot%\System32\tar.exe" %*
```

打完补丁后同一个构建的 tar 步骤正常产出 `zcode-3.14.3.tar.gz`（约 80 MB），并写出 `sha256.txt`。

## 3. 还有两个“Windows 约定”问题（不是编译链）

### 3.1 上游脚本裸调 `pnpm`，Windows 需要 shell

```
Error: pnpm --filter @zcode/cli... build failed: spawnSync pnpm ENOENT
  errno: -4058, code: 'ENOENT', syscall: 'spawnSync pnpm', path: 'pnpm'
```

`build-zcode.mjs` 里 `spawnSync(command, args, {cwd, stdio})` 没有 `shell`。Windows 上没有名为  
`pnpm` 的可执行文件（只有 `pnpm.cmd`），CreateProcess 不会按 `PATHEXT` 自动补全。给这一处  
`spawnSync` 加 `shell: process.platform === "win32"` 后，构建继续走到 vite（58s 完成）。

### 3.2 pnpm 的 isolated 布局会让品牌工具启动即崩

这一条属于品牌区，但同一台机器上验证过，记录在此：

```
Error [ERR_MODULE_NOT_FOUND]: Cannot find package 'ws' imported from
  .../@moonshot-ai/kimi-code/dist/main.mjs
```

kimi-code 自带一个空 `node_modules` 目录，pnpm 11 在 Windows 上因此没有为它建立依赖链接；  
安装报 done，启动即崩。opencode 的表现是 postinstall 里 `require.resolve` 找不到平台包，回退  
`spawnSync("npm")` 也 ENOENT。把 pnpm 改成 `--config.node-linker=hoisted` 后两者都恢复  
（kimi 输出 `2.2.0`，opencode 输出 `1.19.3`，反代 `/i/<id>/` 均 200）。

## 4. 面板现状与“一键”的差距

| 环节        | 现状                          | 一键应有的行为                      |
| --------- | --------------------------- | ---------------------------- |
| Node 24+  | 设置页可装，但 ZCode 构建不消费         | 构建前自动选/装，写进构建日志              |
| pnpm      | runtime 不含，构建依赖 PATH        | 缺则用 runtime 的 npm 装进 runtime |
| tar       | 直接 `tar`，Windows 命中 GNU tar | 在 `<data>` 写 shim 并前置 PATH   |
| spawnSync | 无 shell                     | Windows 加 shell（补丁层可做）       |
| 编译链       | 无                           | **不装**；只在真缺时给出明确提示           |

## 5. 建议的最小方案（数据目录内，全部可删可重建）

```
<data>/
├─ hdsl/runtimes/26.12.0/     # 面板一键装；构建用 Node 24+
│  ├─ node.exe
│  ├─ npm.cmd
│  └─ pnpm.cmd                # 由 runtime 的 npm 装进来（新增）
└─ zcode/build/
   ├─ bin/tar.bat             # → %SystemRoot%\System32\tar.exe（新增）
   ├─ src/                    # 源码树
   └─ out/                    # 构建产物
```

构建时的 PATH 顺序：`<data>/zcode/build/bin` → `<data>/hdsl/runtimes/<ver>` → 原 PATH。

**不要**把 MSVC / VS Build Tools 塞进数据目录：官方安装器要管理员权限、体积 2–6 GB，而当前  
上游根本不需要它。真需要的那天（上游引入必须现场编译的原生模块），正确做法是**预检 + 明确  
报错**（指出缺 `cl.exe` / Python），而不是默默下载几个 GB。

## 6. 验收清单（Windows 裸机，从零开始）

1. 只装 JDK 21，`java -jar hdsl-web.jar` 起面板，数据目录为纯 ASCII 路径。
2. 设置 → Node.js 一键装 24+；确认 `<data>/hdsl/runtimes/<ver>/node.exe`。
3. ZCode → 构建 `v3.14.3`，观察构建日志依次出现：
   - `=== 构建工具链：…node.exe（26.x），pnpm …pnpm.cmd`
   - `pnpm install` 走所选 registry 并 `Done`
   - `[node-pty] skip electron-rebuild (windows-prebuild-available)`
   - `✓ built in …`（vite）
   - `tar -czf …` 无 `Cannot connect to C`
   - `=== 安装发行包` → `state: done`
4. 建实例并启动 → `/i/<id>/` 返回 200；页面带 `__hdsl-shim.js`。
5. 停止实例 → 端口释放、无残留 `node.exe`。

## 7. 已知限制与风险

- **每次构建都清空源码树**：pnpm 树有几十万文件、上千 junction。`Files.walk` 会因 junction  
  目标先被删而抛 `NoSuchFileException`（实测：重建直接失败在“准备构建目录”）；`rmdir /s`  
  在 2 GB 树上十分钟仍未结束；`robocopy /MIR /XJ` 可用，但仍以分钟计。**建议后续做增量复用**  
  （ref 相同则保留 src 与 node_modules），这是体验上最值得的一刀。
- **上游周更**：`spawnSync` 与 `tar` 的调法、node-pty 是否继续发预编译，都可能变。补丁与  
  shim 都要按“锚点找不到就明确失败”处理，不要静默产出错包。
- **pnpm 版本**：上游 `packageManager` 指定 10.33.2，pnpm 11 会自动切换；自行装 pnpm 时不必  
  钉版本，但日志里要能看出实际用的是哪个。
- **路径长度**：数据目录建议短且纯 ASCII；本机实测路径未触及 260 字符上限，但深层  
  `node_modules` 接近时需要留意。
- **构建成功 ≠ 可用**：本次已验证实例启动与反代 200，但登录、模型供应商、会话行为属上游，  
  面板不作担保（与 `docs/zcode-experimental.md` 一致）。

## 8. 落地状态与复核

改动已移植到 `feat/zcode-windows-toolchain`（基于 main `d0c1709`）：

| 文件                | 改动                                                | 目的                      |
| ----------------- | ------------------------------------------------- | ----------------------- |
| `ZcodeBuilder`    | Windows `tar.bat` shim + PATH 前置                  | 修 `Cannot connect to C` |
| `ZcodeBuilder`    | `pnpm install` 加 `node-linker=hoisted`（仅 Windows） | 修依赖解析                   |
| `ZcodeBuilder`    | 构建前 `withToolchain`：选/装 Node 24+、缺 pnpm 则装        | 一键自供                    |
| `ZcodeBuilder`    | 清理改 `robocopy /MIR /XJ` + 容错 walk                 | 修 junction 删除失败         |
| `ZcodePatch`      | `spawnSync` 加 `shell`（仅 Windows）                  | 修 `ENOENT pnpm`         |
| `BrandInstaller`  | Windows `node-linker=hoisted`                     | 修 kimi/opencode 启动崩     |
| `BrandInstaller`  | 安装日志真正写入 `install.log`                            | 修空日志                    |
| `ZcodeApiServlet` | `buildBin` 只认显式配置与镜像路径                            | 把供给逻辑收口到 builder        |

复核时补了四处：`samePath` 改用 `Files.isSameFile`（Windows 的大小写/8.3 短名/`\\?\`
前缀不再影响 `current` 判定）；`deleteRecursively` 用 `NOFOLLOW` 判存在、robocopy 的
`.empty` 用完即删、删不干净时明确失败；工具链来源写进构建日志；并发安装 Node 的
「already installed」转为复用。

Linux 全量测试 **584 用例 / 0 失败 / 0 错误 / 0 跳过**。

### 8.1 真机复跑（2026-10-18，移植后的分支）

在同一台 Windows 10 上从**空数据目录**跑了两遍（全新构建 + 同版本重建），都 `done`：

- 工具链自供：日志先出现
  `=== 数据目录里没有 Node 24+，自动安装一个：26.12.0`，随后
  `=== 构建工具链：…\runtimes\26.12.0\node.exe（26.12.0），pnpm …\runtimes\26.12.0\pnpm.cmd`；
  重建时不再下载，直接复用。运行时目录里 `node.exe`/`npm.cmd`/`pnpm.cmd` 齐全。
- `tar`：解包走 `C:\Windows\System32\tar.exe`；构建脚本的裸 `tar -czf …` 由
  `build/bin/tar.bat` shim 命中，**没有** `Cannot connect to C`。
- `pnpm install` 带 `--config.node-linker=hoisted`；`node-pty` 走
  `skip electron-rebuild (windows-prebuild-available)`。
- **编译器**：`cpu-features`（`ssh2` 的可选依赖）确实尝试了 `node-gyp rebuild`，
  因没有 MSVC 报 `Unable to detect compiler type`——pnpm 把它当可选依赖继续，
  构建成功，`cpufeatures.node` 不存在，`ssh2` 按设计降级。也就是说：
  **不需要编译器即可构建**；报告里"没有 gyp 调用"的说法不准确，但结论不受影响。
- 重建：清理旧 `src`/`out`/`current` 指针后重新安装，`current` 仍指向
  `releases/3.14.3`，没有残留 `.empty`；报告里怀疑的 `current` 断言问题在实践中未复现。
- 实例：启动后 `/i/<id>/` 返回 **200**（`<title>ZCode</title>`），停止后端口无监听。
- 已知小噪音：`apps/zcode-cli` 的 `engines` 钉了 `24.14.0`，用 26.12.0 会有一条
  `Unsupported engine` 警告（pnpm 不拦截）；启动瞬间若立刻访问反代可能短暂 502，
  就绪后正常。
- **编译链预检（最终选择）**：构建前用 `vswhere` 查 `cl.exe`。找到就记一行
  `=== C++ 编译链：…`；找不到就明确写「cpu-features 是 ssh2 的可选依赖，会被跳过，
  不影响本次构建」，并给出最小 Build Tools 命令。面板**不自动下载编译器**——那需要
  管理员权限和 2.5–3 GB，不能藏在一次构建后面。


