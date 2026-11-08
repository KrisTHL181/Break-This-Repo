# forbric-installer
[English](README.md) | 简体中文

一个独立、无依赖的安装器，让 Forbric 能从现成的 Minecraft 启动器（PCL2、HMCL，或其他任何读取原版 `versions/` 目录结构的启动器）启动。它只用 JDK，不依赖任何第三方库，所以构建产出的是一个可直接运行的 jar。

它做两件事：

1. 把 Forbric 的 jar 按常规 Maven 目录结构暂存到启动器的 `libraries/` 目录树中，复制后逐个校验文件的 SHA-1。
2. 在 `versions/<id>/<id>.json` 写入一份版本配置，其中 `inheritsFrom` 设为基础 Minecraft 版本，`mainClass` 设为 `net.forbric.loader.impl.launch.ForbricClient`。之后在启动器里选择 `<id>`，就会在启动器已经解析好的原版基础版本上引导 Forbric。

现有的 Fabric 或 Forge 安装不会有任何改动：这份版本配置是新建的、独立的。

加载器清单里的两类 jar 处理方式不同，而这个差别很关键。`classpath` 条目（加载器核心，加上它复用的底座依赖）既会暂存，也会列入版本配置的 `libraries`，因此启动器会把它们放到 `-cp` 上。`knot-addmods` 条目（`forbricruntime.jar`）会暂存，但刻意*不*列入——放在 `-cp` 上时，它那些用 intermediary 命名的游戏引用无法解析——而是通过版本配置里的 `-Dfabric.addMods=${library_directory}/…` JVM 参数交给 Forbric 的 Knot 类加载器。

重复运行安装器是幂等的：它会覆盖版本配置、重新暂存 jar，并复用已经构建好的产物。

## 安装模式

`Installer.MODE_INTERMEDIARY_V1` = `intermediary-v1`（默认），基础版本 `1.21.11`。
运行在 Fabric 底座上的原版 Minecraft。生成 `versions/forbric-<mc>/forbric-<mc>.json`，其中 `-Dfabric.addMods=…` 指向暂存好的 `forbricruntime` jar，并把 `net.fabricmc:intermediary:<mc>`（来自 `https://maven.fabricmc.net/`）加入版本配置的 libraries。

`Installer.MODE_FULL_FORGE_26_2` = `full-forge-26.2`，基础版本 `26.2`，Forge `26.2-65.0.1`。
驱动真正的 Forge 生命周期，所以直接丢进该版本配置 `mods/` 的原始 Forge mod jar 也能加载。这个模式会在本机构建两个大型产物（见下文），写出的版本配置通过 `-Dfabric.gameJarPath.client` 选用打过补丁的游戏 jar，并带上 `-Dforbric.runtimeNamespace=named` 和 `-Dforbric.fabricMainDeferred=true`。由于 Forge 生命周期是通过扫描 `gameDir/mods` 发现的，三个基础设施 jar（`forbricruntime.jar`、`forge-runtime.jar`、`forbric-bridge.jar`）也会复制到 `versions/forbric-forge-<mc>/mods/`，和用户自己的 mod 放在一起。

只有在加载器的 `generateInstallerManifest` 运行时 `forbric-loader/run/forge-runtime/forbric-bridge.jar` 已经存在，`forbric-bridge.jar` 才会进入清单；这个文件由 `forbric-loader/run/assemble-minecraftforge-runtime.sh` 生成。缺了它，`full-forge-26.2` 安装会失败，并给出明确的报错。

## 运行方式

不带参数且有图形显示环境时，`Main` 会打开 Swing GUI（`InstallerGui`）：一个 Minecraft 文件夹输入框、一个模式下拉框、一个基础版本下拉框（选项来自 `versions/` 下已安装的版本）、一个“缺少基础版本时下载”复选框，以及一个日志面板。安装在后台线程上运行，实时输出与 CLI 相同的日志。GUI 只是同一个 `Installer` 外面的一层薄壳。

```
java -jar forbric-installer.jar                      # GUI
java -jar forbric-installer.jar --headless [options] # no GUI
```

| 选项 | 含义 |
| --- | --- |
| `--mc-dir <path>` | Minecraft 目录。默认是各操作系统的启动器目录（`%APPDATA%\.minecraft`、`~/Library/Application Support/minecraft` 或 `~/.minecraft`）。开头的 `~/` 会自动展开。 |
| `--mode <mode>` | `intermediary-v1`（默认）或 `full-forge-26.2`。 |
| `--game-version <id>` | 基础 Minecraft 版本。默认为 `1.21.11`；模式为 `full-forge-26.2` 时默认为 `26.2`。 |
| `--no-download-mc` | 基础版本缺失时，不从 Mojang 获取。默认会下载。 |
| `--manifest <path>` | 开发用的覆盖选项：读取外部的 `forbric-libraries.json`，而不是安装器 jar 里自带的清单。 |
| `--remote` | 即使本安装器自带了 Forbric 的 jar，也从 GitHub 上的发布版下载。 |
| `--release <tag>` | 安装指定标签的发布版，而不是本安装器构建时对应的那个。传入此选项会丢弃编译进来的清单摘要，因为那个摘要描述的是另一个发布版。 |
| `--mirror <url-prefix>` | 在每个 github.com 请求前加一层中转，适用于 github.com 访问缓慢或被屏蔽的网络，例如 `--mirror https://your-relay.example/`。Maven URL 不改写。 |
| `--offline` | 从不下载 Forbric 的 jar。遇到本地无法满足的项会失败，并把它们列出来。 |
| `--headless` | 不打开 GUI。JVM 报告当前是无界面图形环境时，也等同于指定了此选项。 |
| `--help`, `-h` | 打印用法。 |

### jar 从哪里来

依次为：本安装器内自带的 jar，然后是 `--manifest` 指定的本地构建，最后是 GitHub 发布版。精简版安装器（`./gradlew jar -Pslim`）完全不带载荷，总是下载，所以它只有 70 KB，而不是 5 MB。

Forbric 自己的 jar 来自发布版。第三方库来自各自的官方 Maven 仓库（`net.fabricmc:*` 来自 `maven.fabricmc.net`，其余来自 Maven Central），只有这些仓库连不上时才回退到发布版。

下载内容有两层校验。每个 jar 都要对照清单声明的 SHA-1 检查；已存在的文件只有摘要匹配才算命中缓存，所以下载不完整的文件会自行修复，而不会一直残留。清单本身则要对照发布时编译进安装器的 SHA-256 检查：要是摘要写在与 jar 来自同一发布版的清单里，它只能证明传输没有出错，因为能替换 jar 的人同样能替换清单。不匹配是致命错误，不是警告。在发布流程之外构建的安装器不带这种摘要，并会明确说明这一点。

## 不用终端启动

`packaging/Forbric-Installer.bat`（Windows）和 `packaging/Forbric-Installer.command`（macOS）可以双击运行安装器。每个脚本都要和 jar 放在同一个文件夹里。它们依次在 `JAVA_HOME`、`PATH`、Minecraft 启动器自己的 `runtime` 文件夹中寻找 Java 运行时——Minecraft 玩家常常没有全系统安装的 JDK，启动器自带的运行时就是机器上唯一的 Java。

### Windows 的 .jar 文件关联

在 Windows 上，只有 `.jar` 文件关联正确，双击 jar 才能正常运行；而这个关联存在注册表里，不在 jar 里。本项目分发的任何东西都改变不了结果：关联不对的话，安装器一个字节都还没跑，JVM 就已经失败了。

JDK 安装器写入的关联是 `javaw.exe -jar "%1" %*`，这样可以正常工作。Windows 的 **“打开方式”**（open with）对话框写入的则是 `<whatever.exe> "%1"`——**没有 `-jar`**——因为这个对话框根本不知道 jar 需要它。于是 Java 把 jar 的路径当成*类名*读取，抛出 `ClassNotFoundException` 后退出。用 `java.exe` 时，用户会看到一个控制台窗口一闪而过，快得看不清；用 `javaw.exe` 时，则什么都看不到。

有两点会让那些以为自己装了 Java 的人栽在这上面：

- Minecraft 启动器自带的运行时（`.minecraft/runtime/...`）不是安装好的 JDK，它不注册任何文件关联。在“打开方式”里指向它的 `java.exe`，得到的正是上面那种坏掉的关联。
- `HKCU\...\Explorer\FileExts\.jar\UserChoice` 会**覆盖** JDK 安装器之后写入的任何设置。所以装一个真正的 JDK 不一定能修好已经手动设置过的关联——过时的用户选择会一直占上风。真正能把它切换过来的，是清除这个键，或者在“打开方式”里重新选择 JDK 自己注册的 Java 条目。

`packaging/Forbric-Installer.bat` 自己调用 Java，绕开了上面所有问题；在文件关联状态不明的机器上，用它启动安装器最可靠。

解析器（`Main.parseOpts`）允许任何选项以一个或两个短横线开头。选项会把下一个参数当作自己的值，除非这个参数本身以 `-` 开头，此时该选项取值 `true`。既不以短横线开头、又没被当作值的参数一律忽略。

## 哪些在本地构建，哪些从不分发

安装器 jar 里带的是 Forbric 自己的 jar：加载器（其中包含编译好的、Apache-2.0 许可的 fabric-loader 底座）、它复用的依赖、`forbricruntime`，以及净室实现的 `forbric-bridge` `@Mod`。它不带任何 Minecraft 字节码，也不带任何 Forge 字节码。`full-forge-26.2` 那些会包含此类字节码的产物，安装时才在用户机器上构建，所用文件也是在那里下载的：

- `ForgeRuntimeBuilder` 下载 Forge 的 `-universal` jar，以及 userdev `config.json` 里列出的 FML/ModLauncher/eventbus 库（从 `https://maven.minecraftforge.net` 的 Forge Maven 下载，失败时回退到 Maven Central），再把它们合并成一个 `forge-runtime.jar`，里面带一份合成的 `fabric.mod.json`，让 Knot 在自己的转换型类加载器里加载 `net.minecraftforge.*` 类。其中排除了 Mixin，因为 Forge 自带的那份会和 Knot classpath 上已有的 Fabric sponge-mixin 分支冲突。
- `PatchedMcBuilder` 生成打过 Forge 补丁、使用 Mojmap 命名的游戏 jar：用 `installertools BUNDLER_EXTRACT` 解出 Mojang 服务端 jar，通过 `mergetool` 把它和用户自己已安装的客户端 jar 合并，用 `binarypatcher` 应用 Forge 的 `joined.lzma` 二进制补丁，覆盖上打过补丁的类并去掉 Mojang 的 jar 签名，再通过 Forge 自己的 `AccessTransformerEngine` 应用 Forge 的访问转换器。`ForgeTool` 把这些 fatjar 工具作为子进程运行（它们会调用 `System.exit`），用的是安装器当前所在 JVM 的 `java`，所以不需要另装 JDK。
- 基础版本缺失且没有传 `--no-download-mc` 时，`MojangDownloader` 会获取原版客户端（版本清单 → 各版本 JSON → 客户端 jar）。每个文件都先写入 `.part` 临时文件，校验大小和 SHA-1，再原子地移动到最终位置。

构建出的两个产物都以 `net.forbric` 坐标放进 `libraries/`，中间产物缓存在 `<mc-dir>/.forbric-build/<mc>-<forge>` 下。第一次 `full-forge-26.2` 安装要花几分钟；之后的安装会复用磁盘上已有的内容。

## 构建

```
cd <repo>            && ./bootstrap.sh        # the loader needs its substrate checkout first
cd forbric-installer && ./gradlew jar         # -> build/libs/forbric-installer-0.1.0.jar
```

`jar` 依赖同级目录 `../forbric-loader`。`generateLoaderArtifacts` 会另起进程，调用加载器自己的 Gradle wrapper 执行 `jar runtimeJar generateInstallerManifest`（让两边的构建保持解耦，安装器始终只用 JDK），然后 `bundleForbric` 把 `build/forbric-libraries.json` 列出的每个 jar 复制到本项目生成资源目录的 `/forbric/libs/<maven-path>` 下，并把一份去掉了构建机绝对路径的可移植清单写到 classpath 根目录。正因如此，发布版的 jar 才是自包含的：最终用户只需下载一个文件，既不用从源码构建，也不用指向外部清单。

`./gradlew releaseZip` 会打包出 `build/dist/forbric-installer-<version>.zip`，其中包含 jar 以及 `packaging/` 里的双击启动脚本（macOS 用 `Forbric-Installer.command`，Windows 用 `Forbric-Installer.bat`）。两者都需要 `PATH` 上有 Java 运行时。
