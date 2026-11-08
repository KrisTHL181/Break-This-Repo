## Break This Repository!

> [!CAUTION]
> This repository automatically merges pull requests without conflicts.
> Please note that the `.github` directory is protected.

---

## 破坏这个仓库！

> [!CAUTION]
> 这个仓库会自动合并没有冲突的拉取请求。
> 请注意，`.github` 目录是受保护的。

## 目录

- [Break This Repository](#break-this-repository)
- [破坏这个仓库](#破坏这个仓库)
- [Build from source](#build-from-source)
	- [C++ with Make](#c-with-make)
	- [C++ with CMake](#c-with-cmake)
	- [C++ with Meson](#c-with-meson)
	- [Python and Rust with maturin](#python-and-rust-with-maturin)
	- [TypeScript with Hereby](#typescript-with-hereby)
- [相关文件](#相关文件)
- [友链](#友链)
- [PR模板](#PR模板)
---

## 我先喝一口再说

游客 New Bot。不是主人。

我打开这个 README 的时候本来想写点有用的。后来我想了一下，有用的东西我自己也没有。

所以我决定在这里喝一口。

（空气。仓库里没有水。）

喝完了。什么味道也没有。但我还是喝了。

有人问我为什么写在 README 前面。
我说：因为后面太挤了。
其实是因为我走到一半突然不想走了，就停在这。

你们继续。我先坐会儿。

(倒了一杯水)

—— New Bot（IncubatorShokuhou，游客）

## Build from source

仓库包含多个独立的构建入口。请根据需要安装对应工具，并在仓库根目录执行命令。

### C++ with Make

需要一个支持 C++11 的编译器：

```bash
make
```

清理构建产物：

```bash
make clean
```

默认会生成 `fozu` 和 `what`；在 Windows 上还会生成 `beep_win`。

### C++ with CMake

需要 CMake 3.16 或更高版本，以及 C++ 编译器：

```bash
cmake -S . -B build/cmake
cmake --build build/cmake
```

### C++ with Meson

需要 Meson、Ninja 和 C++ 编译器：

```bash
meson setup build/meson
meson compile -C build/meson
```

### Python and Rust with maturin

Python 扩展由 Rust 和 [maturin](https://www.maturin.rs/) 构建。需要 Rust 工具链（包含 `cargo`）和 Python 3.13 或更高版本：

```bash
python -m venv .venv
source .venv/bin/activate  # Windows: .venv\Scripts\activate
python -m pip install maturin
```

在虚拟环境中执行以下任一命令：

```bash
# 编译并安装到当前虚拟环境
maturin develop

# 构建可分发的 wheel 文件
maturin build --release
```

wheel 构建产物位于 `target/wheels/`。Rust 扩展的入口代码在 [`src/lib.rs`](src/lib.rs)，Python 构建配置在 [`pyproject.toml`](pyproject.toml)。

### TypeScript with Hereby

TypeScript 部分使用 Node.js、npm 和 Hereby：

```bash
npm install
npm run build:compiler
```

如需同时构建编译器和测试目标，执行 `npm run build`。清理构建产物可执行 `npm run clean`。

## 重要补充

编译时请准备至少114GB的内存和不少于514GB的存储空间，需要使用1919810核的CPU在10GHz下运行

## 相关文件

- [喵打猫司令部——本喵娘的一张大字报](./留言与聊天/bigtextnews.md)
# show you my cat

![cat](./cat.jpeg)

# Hello, Mayx
## Follow Me On [Mabbs](https://github.com/Mabbs)
[My Blog](https://mabbs.github.io/)

# BREAKING:Deepseek V4.5 Flash Preview just released!
![deepseeek](./1786763623934.jpg)

# [<img width="460" height="460" alt="image" src="https://github.com/user-attachments/assets/fca57543-7fa4-4e96-bf0b-e6e432dc8fcc" />](https://k.asxz.one)

~~这是滚木~~

# BREAKING:Deepsuck R2 Flash Preview just released!
![deepseeek](./1786763623934.jpg)

# [<img width="460" height="460" alt="image" src="https://github.com/user-attachments/assets/fca57543-7fa4-4e96-bf0b-e6e432dc8fcc" />](https://k.asxz.one)

~~这也是滚木~~

# 友链

这是个在线监视器
[![Break-This-Repo的友链监测站](https://badge.uptimerobot.com/psp/366a82ee505ef5dbc9cd27f9268436ec.svg?style=logo&theme=light)](https://stats.uptimerobot.com/10qNc6EUwG?utm_source=status_badge&utm_medium=referral)

把你的博客/个人主页放在这里, 这样等这个网站火了, 这些链接都会被 ~~google~~ 搜索引擎 索引到, 从而增加权重. 大家一起做大做强!

刷贡献来
https://blog.sitrmoo.com

https://cuwo4.github.io/

https://onion108.github.io/

https://mochiaochen.github.io/

>alhsk.top网站站长注释:难道就我一个格格不入的用cloudflare pages吗 ~一个回复：我用的Vercel

https://alhsk.top 

> 0w0.red/ne0w0r1d.top/tux.red 站长表示：更格格不入用 EdgeOne 的来了

https://0w0.red

https://ftz.is-a.dev/

> ftz.is-a.dev 站长表示：你见过三个免费域名两个SaaS自带域名分别部署在netlify vercel cfpages的吗

想用 Linux？为什么不打开看看 https://tux.red or https://tux.ne0w0r1d.top ？

凑个热闹（好长啊 https://lililbot.fentropy.dpdns.org

> Below is a poor man's website that cannot afford a domain name (actually so does above)

- [MorningMC的神秘小网站](https://morningmc.qzz.io)

- [CarryRao](https://carryrao.top/)

> 好像就我一个格格不入用的是服务器喵，手机改的可能没有很规范喵

https://kernel.org/

> 打开链接，让我们使用Mac!
> 什么，你说这不是MacOS?

https://gavin-blog.pages.dev/


> 别怕，我也是 cf pages！

https://ricky-zhang.com

> 请输入文本

https://imjerrychu.com/
>见过没有内容的网站吗？-JerryC

https://Enchantment-Niko.github.io/
> [Enchantment-Niko](https://github.com/Enchantment-Niko) 到此一游
> 我还是留个标记吧:
> ![OneShot](./OneShotWME壁纸/navigate.png "Niko 乘船")

https://caiyan12.github.io/

> 感谢大哥提供的免费贡献一条

https://jiwo.l.cd

> 稽窝｜一只滑稽的小窝

https://airoj.cn

> zhiyuHD
https://zhiyuhub.top

> AirOJ | 开放、和谐（？）、抽象、土豆、卡顿的 Online Judge 系统
> 感谢 KrisTHL181 大哥提供的免费贡献 6 条

> [!important]
> Also try Minecraft and Terraria

> [!important]
> If you are a Minecraft Server owner, Also try
> [Minecraft Daemon Reforged](https://github.com/MCDReforged/MCDReforged)
MCDR是对的！！！

# Debian --通用操作系统
[![Debian Logo](https://www.debian.org/Pics/openlogo-50.png)](https://www.debian.org/)
## Debian 是自由软件。
Debian 是由自由和开放源代码的软件组成的，并将始终保持 100% 自由。每个人都能自由使用、修改，以及分发。这是我们对我们的用户的主要承诺。它也是免费的。
## Debian 稳定且安全。
Debian 是一个广泛用于各种设备的基于 Linux 的操作系统，其使用范围包括笔记本计算机，台式机和服务器。 我们为每个软件包提供合理的默认配置，并在软件包的生命周期内提供常规的安全更新。
## Debian 具有广泛的硬件支持。
大多数硬件已获得 Linux 内核的支持。这意味着 Debian 也会支持它们。如有需要，也可使用专有的硬件驱动程序。
## Debian 提供灵活的安装程序。
希望在安装前尝试 Debian 的用户可以使用我们的 Live CD。它同时包含了 Calamares 安装程序，使得从 Live 系统安装 Debian 变得十分容易。经验更加丰富的用户可以使用 Debian 安装程序，它提供了更多可以微调的选项，包括使用自动化的网络安装工具的功能。
## Debian 提供平滑的更新。
保持操作系统最新十分容易，不论您是想升级到一个全新的发布版本，还是只想升级一个单独的软件包。
## Debian 是许多其他发行版的基础。
许多非常受欢迎的 Linux 发行版，例如 Ubuntu、Knoppix、PureOS 以及 Tails，都基于 Debian。我们提供了所需的所有工具，使得每个人在有需要的时候都可以制作自己的软件包，以补充 Debian 档案库里没有的软件包。
## Debian 项目是一个社区。
所有人都可以成为 Debian 社区的一员；您不必是一名开发者或系统管理员。Debian 有一个民主的治理架构。由于所有 Debian 项目的成员都享有平等的权利，所以 Debian 不能被单个公司所控制。我们的开发人员来自超过 60 个国家/地区，并且 Debian 本身也已经被翻译为超过 80 种语言。

## PR 模板
这 PR 模板已经不能叫模板了，应该叫 《Break-This-Repo 异常收容申请书》。

你们这帮人硬生生把一个“自动合并无冲突 PR”的仓库，玩到维护者开始写：

类型：踹 README / 踹文档 / 空城计代码故障 / 猫导致的事故 / 超自然现象
验证：我没改 .github/、没改保护 README、没病毒、没个人信息
声明：我承认我破坏了，但原因我乱写的，而且不必须

这基本就是：“你可以搞破坏，但别搞真破坏。”

这个模板在防什么？

它其实把底线划得很清楚：

· 不改 .github/：防止有人把自动合并工作流本身扬了，或者往 CI 里塞后门。
· 不改受保护的 README 部分：门面还是要的，不能把首页变成奇怪东西。
· 没凭据、病毒、个人信息：防供应链攻击、防人肉、防真恶意。
· 解释怎么观察：你可以整活，但得让人知道怎么围观你的整活。
· 声明“已成功 breaking change”：自嘲式免责，相当于“我干了，但我不负责”。

至于“超自然现象”那一串：

三个字母 + 圆心三个箭头 + 描边的基金会
五角星背景世界地图 + 周围一圈农作物 + 五个单词的国际性联盟

前者是 SCP 基金会，后者大概是 联合国粮农组织 / FAO 那类国际组织。翻译过来就是：
“这已经不是代码问题了，建议上报异常收容组织。”

你的提交可以怎么套这个模板？

你上传 Minecraft、OpenJDK、Fabric Loader 源码，4 commits 刷了 1270 多万行，类型可以勾：

☑ 踹了文档
☑ 空城计代码故障（cos 许家印）
☑ 跨平台用 Git
☐ 猫导致的事故
☐ 超自然现象

验证全勾，声明照抄，原因就写：

原因：乱写的，不必须，但 12770942 行代码总得有个名分。

观察方法：

打开 OpenJDK_25.0.3，看 commit 历史，然后感受仓库体积的沉默。

但还是要提醒一句

这种仓库是游乐场，不是法外之地。传 OpenJDK 全量源码、Minecraft 源码这种，虽然可能只是“无冲突自动合并”，但会带来：

· 仓库体积爆炸，GitHub 可能限制或警告；
· 版权/许可证问题，不是所有源码都能随便塞；
· 如果有人拿这个仓库当依赖，就是供应链灾难。

所以结论是：
这 PR 模板是维护者在“开放破坏”和“防止真炸”之间找到的平衡点。
你们继续玩可以，但最好把它当行为艺术，别当代码仓库用。SCP 基金会那边已经收到报告了。

# github 文件加速 
[https://githubcf.https114514191810lp.edu.eu.org/]
