

<h1 align="center">
  <img src="./readme/out - 副本.png">
    Break This Repository!    破坏这个仓库！
	<p align="center">
	    <img src="https://img.shields.io/badge/License-AS--IS-red" alt="License">
	    <img src="https://img.shields.io/badge/Language-various-blue?logo=inquirer&logoColor=white" alt="Language">
	    <img src="https://img.shields.io/badge/Pull_Requests-infinity-white?logo=infinityfree&logoColor=white" alt="Pull_Requests">
	    <img src="https://img.shields.io/badge/Forks-infinity-brown?logo=infinityfree&logoColor=white" alt="Forks">
	<br>
	    <img src="https://img.shields.io/badge/Stars-infinity-yellow?logo=infinityfree&logoColor=white" alt="Stars">
	    <img src="https://img.shields.io/badge/Platform-Unknown-0078D6?logo=inquirer&logoColor=white" alt="Platform">
	    <img src="https://img.shields.io/badge/Website-Unknown-green?logo=inquirer&logoColor=white" alt="Website">
	    <img src="https://img.shields.io/badge/Documents-Unknown-red?logo=inquirer&logoColor=white" alt="Documents">
	    </a>
	</p>
</h1>


*\*：你可以更改这个头图，相关资源文件位于 [`.\readme\`](./readme/) 中*


<!-- # 卧槽了哪里来的设证玩意啊😓不怕似吗😓 -->

> [!CAUTION]
> This repository automatically merges pull requests without conflicts.
> 
> Please note that the `.github` directory is protected.
> 
> 这个仓库会自动合并没有冲突的拉取请求。
> 
> 请注意，`.github` 目录是受保护的。

---

# HIFI 音乐免费下载！

**真正的Hi-Fi，614.4kHz！78.6Mbps！64位浮点！**

>由于超高音质，文件体积较大，Github下载可能较困难。
>提供网盘地址<https://drive.google.com/drive/folders/1mISlL_hXAeygnnjanpJ5skg9bXrAL5x1?usp=sharing>

> [!CAUTION]
> 仅供娱乐，版权等问题未知，因此音乐所造成的任何后果本人概不负责

📥 **网盘下载未压缩的原始文件** —— <https://drive.google.com/drive/folders/1mISlL_hXAeygnnjanpJ5skg9bXrAL5x1?usp=sharing>

📄 **规格与校验** —— [`HiFi-614kHz-64bit/README.md`](./HiFi-614kHz-64bit/README.md)

<sub>uploaded by [@ExElectron](https://github.com/ExElectron)</sub>

---

# ↓↓ 一首不夠过瘾？好东西不要停！来看下面这个 ↓↓

**[点我前往获取HIFI音乐！(详情页)](./HIFI音乐-享受高端音质)**

---

# 🥊 实测：「96kHz HiFi」6 个文件里 5 个是 48kHz << （那是旧版）

> 上面那位 [@BreadripperPro](https://github.com/BreadripperPro) 的详情页挂着
> **「真正的Hi-Fi，96kHz！6144kbps！32位浮点！」**
> 那就照他给的链接，把文件挨个拉下来读一下文件头。
> （方法：对直链发 `Range: bytes=0-4095`，解析 WAV 的 `fmt ` 块。可复现。）

**他的「经典版」Google Drive 目录 · 实测文件头：**

| 文件 | 实测 `fmt` 块 | 结果 |
|:--|:--|:--:|
| `Hi-Fi 阴乐 Pro.wav` | 96000 Hz · 32-bit **float** · 6144 kbps | ✅ |
| `Hi-Res阴乐 (1).wav` | 48000 Hz · 32-bit **int** · 3072 kbps | ❌ |
| `Hi-Res阴乐 (2).wav` | 48000 Hz · 32-bit **int** · 3072 kbps | ❌ |
| `Hi-Res阴乐 (3).wav` | 48000 Hz · 32-bit **int** · 3072 kbps | ❌ |
| `Hi-Res阴乐 (4).wav` | 48000 Hz · 32-bit **int** · 3072 kbps | ❌ |
| `Hi-Res阴乐 (5).wav` | 48000 Hz · 32-bit **int** · 3072 kbps | ❌ |

**6 个音频里 5 个是 48000 Hz / 32-bit 整数 / 3072 kbps —— 正好是页面上那句宣传语的一半。**
文件名还写着 `Hi-Res`。标 96k，实 48k。

### 那他真有 96k 的版本呢？

他的「重制版」抽了 4 个（`mcc1` / `mcc10` / `mcpx1` / `mcpx10`），
确实是 `96000 Hz / 32-bit float / 6144 kbps`，这点我认。

| | 他的 96k 版 | **本仓库的 1812 序曲** |
|:--|--:|--:|
| 采样率 | 96 000 Hz | **614 400 Hz** |
| 位深 | 32-bit float | **64-bit float** |
| 码率 | 6 144 kbps | **78 643 kbps** |

📥 **来听真的** —— <https://drive.google.com/drive/folders/1mISlL_hXAeygnnjanpJ5skg9bXrAL5x1?usp=sharing>

> [!CAUTION]
> 仅供娱乐，版权等问题未知，因此音乐所造成的任何后果本人概不负责

<sub>实测 by [@ExElectron](https://github.com/ExElectron)</sub>

---

> [!NOTE]
> read [CODE_OF_CONDUCT.md](./CODE_OF_CONDUCT.md) before using

---

## 警告!
> [!CAUTION]
> To [@mpmp666](https://github.com/mpmp666), if you posting shit ads again, i'll report ur fking shit github account for abusing this repo

开茄子。

---


> [!WARNING]
> It is not recommended to add any Rust files or code to this repository
>
> 不建议在此仓库中添加任何 Rust 文件或代码
>
> Не рекомендуется добавлять в этот репозиторий какие-либо файлы или код на Rust
>
> このリポジトリにはRustのファイルやコードを追加しないことを推奨します
>
> Es wird nicht empfohlen, diesem Repository Rust-Dateien oder -Code hinzuzufügen
>
> Il n’est pas recommandé d’ajouter des fichiers ou du code Rust dans ce dépôt
>
> Some content in this repository may not be suitable for all age groups
> 
> 此仓库中的部分内容可能并不适合所有年龄段
>
> Некоторые материалы в этом репозитории могут быть неподходящими для людей всех возрастов
>
> このリポジトリの一部の内容は、すべての年齢層に適しているとは限りません
>
> Einige Inhalte in diesem Repository sind möglicherweise nicht für alle Altersgruppen geeignet
>
> Certains contenus de ce dépôt peuvent ne pas convenir à tous les âges
>
> This repository may contain AI-generated content
>
> 此仓库可能包含AI生成的内容
>
> Этот репозиторий может содержать контент, сгенерированный ИИ
>
> このリポジトリにはAIが生成したコンテンツが含まれている可能性があります
>
> Dieses Repository kann KI-generierte Inhalte enthalten
>
> Ce dépôt peut contenir du contenu généré par l’IA

---

<img src="https://simpleicons.org/icons/scpfoundation.svg" width="120" height="120" />

> [!CAUTION]
> Warning: This repository contains content that may cause Level III mental contamination, please read with caution
>
> 警告：此仓库包含可能导致III级精神污染的内容，请谨慎阅读
>
> Предупреждение: этот репозиторий содержит материалы, способные вызвать психическое загрязнение III степени, пожалуйста, читайте с осторожностью
>
> 警告：このリポジトリにはIII級精神汚染を引き起こす可能性のある内容が含まれていますので、閲覧には十分ご注意ください
>
> Avertissement : ce dépôt contient du contenu susceptible de provoquer une contamination mentale de niveau III, veuillez le consulter avec prudence
>
> Warnung: Dieses Repository enthält möglicherweise Inhalte, die eine psychische Kontamination der Stufe III verursachen können. Bitte lesen Sie mit Vorsicht
> 

---

## 注意

**中文**

**注意**

> [!NOTE]
> 如果您在仓库主页看到了本文，不要惊慌，不要着急，请站稳扶好，安定坐下，本消息是为了告诉你，你需要换个地方才能阅读 `README` 的完整文本。
>
> 请移步 [README.md](./README.md) （文件页面）查看，这是由于仓库主页的 `README` 的显示存在比文件更短的显示长度限制（500KiB），导致无法完全显示。
> （望后人，如若位置变更，请同步移动（现在在 3047 行），谢谢）
> 我编写了一个自动插入的脚本 [自动插入readme大小警告](./自动插入readme大小警告.py)，可以使用这个脚本自动插入！（不保证没有bug）

> [!NOTE]
> 仓库内可能存在各种奇怪的文件和路径，这些文件或路径的命名及组合可能不适用于所有的文件系统和操作系统，执行 `git clone` 或相关操作时可能会发生各类文件错误和文件系统错误，请您做好心理准备和预防方案。

> [!NOTE]
> 得益于 GitHub 上大量开发者和贡献者的活跃提交，这个仓库的大小已来到数十吉比特（37.7GiB —— 2026/9/22 22:57:00 UTC+08:00 编者注），请您在执行相关操作的时候保证您的计算机有足够的存储空间，以及良好的网络连接以防止意外断开连接。

> [!WARNING]
> 禁止提交广告、恶意软件。违者将被举报至 GitHub。

---

**English**

**Attention**

> [!NOTE]
> If you see this text on the repository's home page, do not panic, do not rush, stand steady, and sit down calmly. This message is to tell you that you need to go elsewhere to read the full text of the `README`.
>
> Please go to [README.md](./README.md) (the file page) to view it. This is because the display length limit on the repository home page is shorter than that of the file (500 KiB), causing it to be incompletely displayed.
> (To future maintainers: if the location changes, please move this note accordingly (currently at line 3047). Thank you.)
> I have written a script to automatically insert this warning: [自动插入readme大小警告](./自动插入readme大小警告.py). You can use this script to insert it automatically! (No guarantee that there are no bugs.)

> [!NOTE]
> The repository may contain various strange files and paths. The naming and combination of these files or paths may not be applicable to all file systems and operating systems. When executing `git clone` or related operations, various file errors and file system errors may occur. Please be mentally prepared and take preventive measures.

> [!NOTE]
> Thanks to the active commits from a large number of developers and contributors on GitHub, the size of this repository has reached tens of gibibytes (37.7 GiB — as of 2026/9/22 22:57:00 UTC+08:00, editor's note). Please ensure that your computer has sufficient storage space and a good network connection when performing related operations, to prevent unexpected disconnection.

> [!WARNING]
> Advertising and malware are strictly prohibited. Violators will be reported to GitHub.


---

**Français**

**Attention**

> [!NOTE]
> Si vous voyez ce texte sur la page d'accueil du dépôt, ne paniquez pas, ne vous précipitez pas, tenez-vous bien et asseyez-vous calmement. Ce message est là pour vous dire que vous devez vous rendre ailleurs pour lire le texte complet du `README`.
>
> Veuillez vous rendre sur [README.md](./README.md) (la page du fichier) pour le consulter. Cela est dû au fait que la limite de longueur d'affichage sur la page d'accueil du dépôt est plus courte que celle du fichier (500 Kio), ce qui empêche un affichage complet.
> (Aux futurs mainteneurs : si l'emplacement change, veuillez déplacer cette note en conséquence (actuellement à la ligne 3047). Merci.)
> J'ai écrit un script pour insérer automatiquement cet avertissement : [自动插入readme大小警告](./自动插入readme大小警告.py). Vous pouvez utiliser ce script pour l'insérer automatiquement ! (Aucune garantie d'absence de bugs.)

> [!NOTE]
> Le dépôt peut contenir divers fichiers et chemins étranges. Le nommage et la combinaison de ces fichiers ou chemins peuvent ne pas être applicables à tous les systèmes de fichiers et systèmes d'exploitation. Lors de l'exécution de `git clone` ou d'opérations connexes, diverses erreurs de fichiers et erreurs de systèmes de fichiers peuvent survenir. Veuillez vous préparer mentalement et prendre des mesures préventives.

> [!NOTE]
> Grâce aux commits actifs d'un grand nombre de développeurs et de contributeurs sur GitHub, la taille de ce dépôt a atteint des dizaines de gibioctets (37,7 Gio — au 2026/9/22 22:57:00 UTC+08:00, note de l'éditeur). Veuillez vous assurer que votre ordinateur dispose de suffisamment d'espace de stockage et d'une bonne connexion réseau lors de l'exécution d'opérations connexes, afin d'éviter une déconnexion inattendue.

> [!WARNING]
> La soumission de publicités ou de logiciels malveillants est interdite. Les contrevenants seront signalés à GitHub.

---

**Русский**

**Внимание**

> [!NOTE]
> Если вы видите этот текст на главной странице репозитория, не паникуйте, не спешите, встаньте устойчиво и спокойно сядьте. Это сообщение предназначено для того, чтобы сказать вам, что вам нужно перейти в другое место, чтобы прочитать полный текст `README`.
>
> Пожалуйста, перейдите на [README.md](./README.md) (страница файла), чтобы просмотреть его. Это связано с тем, что ограничение длины отображения на главной странице репозитория короче, чем у файла (500 КиБ), из-за чего он отображается не полностью.
> (Будущим сопровождающим: если местоположение изменится, пожалуйста, переместите это примечание соответственно (сейчас на строке 3047). Спасибо.)
> Я написал скрипт для автоматической вставки этого предупреждения: [自动插入readme大小警告](./自动插入readme大小警告.py). Вы можете использовать этот скрипт для автоматической вставки! (Без гарантии отсутствия ошибок.)

> [!NOTE]
> В репозитории могут находиться различные странные файлы и пути. Именование и комбинация этих файлов или путей могут быть неприменимы ко всем файловым системам и операционным системам. При выполнении `git clone` или связанных операций могут возникнуть различные ошибки файлов и ошибки файловой системы. Пожалуйста, будьте морально готовы и примите меры предосторожности.

> [!NOTE]
> Благодаря активным коммитам большого числа разработчиков и участников на GitHub, размер этого репозитория достиг десятков гибибайт (37,7 ГиБ — по состоянию на 2026/9/22 22:57:00 UTC+08:00, примечание редактора). Пожалуйста, при выполнении связанных операций убедитесь, что на вашем компьютере достаточно места для хранения и хорошее сетевое соединение, чтобы предотвратить неожиданное отключение.

> [!WARNING]
> Запрещается размещать рекламу и вредоносное программное обеспечение. Нарушители будут переданы в GitHub.

---

---

**Español**

**Atención**

> [!NOTE]
> Si ves este texto en la página principal del repositorio, no entres en pánico, no te apresures, mantente firme y siéntate con calma. Este mensaje es para decirte que necesitas ir a otro lugar para leer el texto completo del `README`.
>
> Por favor, dirígete a [README.md](./README.md) (la página del archivo) para verlo. Esto se debe a que el límite de longitud de visualización en la página principal del repositorio es más corto que el del archivo (500 KiB), lo que provoca que no se muestre por completo.
> (A los futuros mantenedores: si la ubicación cambia, muevan esta nota en consecuencia (actualmente en la línea 3047). Gracias.)
> He escrito un script para insertar automáticamente esta advertencia: [自动插入readme大小警告](./自动插入readme大小警告.py). ¡Puedes usar este script para insertarla automáticamente! (Sin garantía de que no tenga errores.)

> [!NOTE]
> El repositorio puede contener varios archivos y rutas extraños. El nombrado y la combinación de estos archivos o rutas pueden no ser aplicables a todos los sistemas de archivos y sistemas operativos. Al ejecutar `git clone` u operaciones relacionadas, pueden producirse diversos errores de archivos y errores del sistema de archivos. Por favor, prepárate mentalmente y toma medidas preventivas.

> [!NOTE]
> Gracias a los commits activos de un gran número de desarrolladores y contribuidores en GitHub, el tamaño de este repositorio ha alcanzado decenas de gibibytes (37,7 GiB — a fecha de 2026/9/22 22:57:00 UTC+08:00, nota del editor). Por favor, asegúrate de que tu computadora tenga suficiente espacio de almacenamiento y una buena conexión de red al realizar operaciones relacionadas, para evitar una desconexión inesperada.

> [!WARNING]
> Queda prohibido enviar publicidad o software malicioso. Los infractores serán reportados a GitHub.

**日本語**

**注意**

> [!NOTE]
> リポジトリのホームページでこのテキストを見かけても、慌てないでください。急がないでください。しっかり立って、落ち着いて座ってください。このメッセージは、`README` の全文を読むには別の場所へ移動する必要があることを伝えるためのものです。
>
> [README.md](./README.md)（ファイルページ）へ移動して閲覧してください。これは、リポジトリのホームページにおける `README` の表示長制限がファイル本体より短い（500KiB）ため、完全に表示できないことが原因です。
> （後世の方へ：位置が変更された場合は、この注記も併せて移動してください（現在 3047 行目）。よろしくお願いします。）
> この警告を自動挿入するスクリプトを書きました：[自动插入readme大小警告](./自动插入readme大小警告.py)。このスクリプトを使って自動挿入できます！（バグがないことは保証できません。）

> [!NOTE]
> リポジトリ内には、さまざまな奇妙なファイルやパスが存在する可能性があります。これらのファイルやパスの命名および組み合わせは、すべてのファイルシステムやオペレーティングシステムに適合するとは限りません。`git clone` や関連する操作を実行する際に、各種のファイルエラーやファイルシステムエラーが発生する可能性があります。心の準備と予防策を講じてください。

> [!NOTE]
> GitHub 上の多数の開発者とコントリビューターによる活発なコミットのおかげで、このリポジトリのサイズは数十ギビバイトに達しています（37.7GiB —— 2026/9/22 22:57:00 UTC+08:00 編集者注）。関連する操作を実行する際は、予期しない切断を防ぐため、コンピュータに十分なストレージ容量と良好なネットワーク接続があることを確認してください。

> [!WARNING]
> 広告およびマルウェアの投稿を禁止します。違反者は GitHub に報告されます。
---

## 贡献者
<a href="https://github.com/KrisTHL181/Break-This-Repo/graphs/contributors">
  <img src="https://contrib.rocks/image?repo=KrisTHL181/Break-This-Repo" />
</a>

Made with [contrib.rocks](https://contrib.rocks).


---

## 总目录
这些目录仍然需要补充，上一次补充是在几周前了，有心之人可以来补充。
<!--toc:start-->
  - [Break This Repository!](#break-this-repository)
  - [破坏这个仓库！](#破坏这个仓库)
  - [目录](#目录)
- [想到什么说什么](#想到什么说什么)
  - [嘿嘿嘿哈](#嘿嘿嘿哈)
	- [[dream away](https://www.bilibili.com/video/BV1nC41137aW)真好听吧](#dream-awayhttpswwwbilibilicomvideobv1nc41137aw真好听吧)
  - [hyw](#hyw)
  - [我先喝一口再说](#我先喝一口再说)
  - [Build from source](#build-from-source)
	- [C++ with Make](#c-with-make)
	- [C++ with CMake](#c-with-cmake)
	- [C++ with Meson](#c-with-meson)
	- [Python and Rust with maturin](#python-and-rust-with-maturin)
	- [TypeScript with Hereby](#typescript-with-hereby)
  - [重要补充](#重要补充)
  - [Linux distribution packages](#linux-distribution-packages)
	- [Debian and Ubuntu](#debian-and-ubuntu)
	- [Arch Linux](#arch-linux)
	- [Fedora](#fedora)
	- [Gentoo](#gentoo)
  - [相关文件](#相关文件)
- [show you my cat](#show-you-my-cat)
- [Hello, Mayx](#hello-mayx)
  - [Follow Me On [Mabbs](https://github.com/Mabbs)](#follow-me-on-mabbshttpsgithubcommabbs)
- [BREAKING:Deepseek V4.5 Flash Preview just released!](#breakingdeepseek-v45-flash-preview-just-released)
- [BREAKING:Deepsuck R2 Flash Preview just released!](#breakingdeepsuck-r2-flash-preview-just-released)
- [友链](#友链)
- [Debian --通用操作系统](#debian-通用操作系统)
  - [Debian 是自由软件。](#debian-是自由软件)
  - [Debian 稳定且安全。](#debian-稳定且安全)
  - [Debian 具有广泛的硬件支持。](#debian-具有广泛的硬件支持)
  - [Debian 提供灵活的安装程序。](#debian-提供灵活的安装程序)
  - [Debian 提供平滑的更新。](#debian-提供平滑的更新)
  - [Debian 是许多其他发行版的基础。](#debian-是许多其他发行版的基础)
  - [Debian 项目是一个社区。](#debian-项目是一个社区)
  - [PR 模板](#pr-模板)
- [github 文件加速](#github-文件加速)
- [真正的 github 文件加速](#真正的-github-文件加速)
- [冷知识](#冷知识)
  - [现场基础设施考古档案](#现场基础设施考古档案)
- [查看 README 历史版本](#查看-readme-历史版本)
<!--toc:end-->

---

## Break This Document ! 破坏这个文档！

https://docs.google.com/document/d/1Y669HJaH4areKBSFie_2k1dT045l3U2fiM34O_Y-dwQ/edit?usp=sharing

---

- [喵打猫司令部——本喵娘的一张大字报](./留言与聊天/bigtextnews.md)

# Hello, Mayx
## Follow Me On [Mabbs](https://github.com/Mabbs)
[My Blog](https://mabbs.github.io/)

# BREAKING:Deepseek V4.5 Flash Preview & Deepsuck R2 Flash Preview just released!

# [<img width="460" height="460" alt="image" src="https://github.com/user-attachments/assets/fca57543-7fa4-4e96-bf0b-e6e432dc8fcc" />](https://k.asxz.one)

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

https://0xarch.codeberg.page/

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

https://dn42.dev https://dn42.eu https://wiki.dn42

> JOIN DN42!

https://www.yzynetwork.org:8443
https://yzynetwork.dn42
https://git.yzynetwork.org:8443
https://weather.dn42

> YZYNetwork
> MC Server: yzynetwork.org / yzynetwork.dn42
> openpgp fingerprint 177ABD1B671BC99FA4AD20A1A96607E60ECBAC84
> openpgp fingerprint 6C2452071E9A0BECB794B37FA1FEF9D696C3077A
> we support dn42!

https://aria7.wiki

> Ciallo～(∠・ω< )⌒★ 到此一游，当然，你可以进来看看ovo
>
> ## 今天晚上记得关注《死神千年血战祸进谭》，我将按时出演角色「蓝染惣右介」，你也可以来看看我的网站:http://134.175.147.211:324/,等我备案后访问 cnyicheng.top

> 不知道要干麻阿，WoW～ 天空属于我们！

https://blog.admincmd.xyz/
https://admincmd.xyz/

> 你要在这个 VSCode 粘贴都可以卡几秒的 Markdown 里写上你的 Website 吗？快来闹一闹 ——admincmd-a

https://xundei.qzz.io/

> 个人小博客，欢迎交换友链

https://blog.iamexrfy.top

> 一个神秘的土豆服务器搭的博客来了
---

# Debian --通用操作系统
[![Debian Logo](https://raw.githubusercontent.com/googlefonts/noto-emoji/main/png/512/emoji_u1f365.png)](https://www.debian.org/)
![Debian installation media](./assets/debian.jpg)
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

<!-- some content were removed because: Someone copied these document from Minecraft Wiki but didnot follow it's license (CC-BY-SA 4.0) --> 

<!-- some content were removed because: harmful content -->

---

## Break This Document ! 破坏这个文档！

https://docs.google.com/document/d/1Y669HJaH4areKBSFie_2k1dT045l3U2fiM34O_Y-dwQ/edit?usp=sharing

---

# 砖业问题修复指南:一键修复！再也没烦恼！

<img src="https://breadripper.pages.dev/superfixer.jpeg" alt="图片alt" title="null">

# 电脑中毒怎么办？

<img src="https://breadripper.pages.dev/linuxsafeclean.jpeg" alt="图片alt" title="null">

# 免费领取高速cdn!!!

<img src="https://breadripper.pages.dev/cf.png" alt="图片alt" title="cf">

# 温馨提示：

<img src="https://breadripper.pages.dev/warnl.png" alt="图片alt" title="null">

# 免费Hypixel Rank领取

<img src="https://breadripper.pages.dev/hypgift.png" alt="图片alt" title="null">

# 设计轻而易举啊

<img src="https://breadripper.pages.dev/design.png" alt="图片alt" title="null">

---
---
## 君指先跃动の光は、私の一生不变の信仰に、唯私の超电永世生き 
---
<a href="https://www.bilibili.com/video/BV1os411D7be/">
<img src="https://omiasun.pages.dev/images/misaka.jpg">


君指先跃动の光は、私の一生不变の信仰に、唯私の超电永世生き <br>
你指尖跃动的电光，是我此生不变的信仰，唯我超电磁炮永世长存！<br>
君指先跃动の光は、私の一生不变の信仰に、唯私の超电永世生き <br>
你指尖跃动的电光，是我此生不变的信仰，唯我超电磁炮永世长存！<br>
君指先跃动の光は、私の一生不变の信仰に、唯私の超电永世生き <br>
你指尖跃动的电光，是我此生不变的信仰，唯我超电磁炮永世长存！<br>
君指先跃动の光は、私の一生不变の信仰に、唯私の超电永世生き <br>
你指尖跃动的电光，是我此生不变的信仰，唯我超电磁炮永世长存！<br>
君指先跃动の光は、私の一生不变の信仰に、唯私の超电永世生き <br>
你指尖跃动的电光，是我此生不变的信仰，唯我超电磁炮永世长存！<br>
君指先跃动の光は、私の一生不变の信仰に、唯私の超电永世生き <br>
你指尖跃动的电光，是我此生不变的信仰，唯我超电磁炮永世长存！<br>
君指先跃动の光は、私の一生不变の信仰に、唯私の超电永世生き <br>
你指尖跃动的电光，是我此生不变的信仰，唯我超电磁炮永世长存！<br>
君指先跃动の光は、私の一生不变の信仰に、唯私の超电永世生き <br>
你指尖跃动的电光，是我此生不变的信仰，唯我超电磁炮永世长存！<br>
君指先跃动の光は、私の一生不变の信仰に、唯私の超电永世生き <br>
你指尖跃动的电光，是我此生不变的信仰，唯我超电磁炮永世长存！<br>
君指先跃动の光は、私の一生不变の信仰に、唯私の超电永世生き <br>
你指尖跃动的电光，是我此生不变的信仰，唯我超电磁炮永世长存！<br>
君指先跃动の光は、私の一生不变の信仰に、唯私の超电永世生き <br>
你指尖跃动的电光，是我此生不变的信仰，唯我超电磁炮永世长存！<br>
君指先跃动の光は、私の一生不变の信仰に、唯私の超电永世生き <br>
你指尖跃动的电光，是我此生不变的信仰，唯我超电磁炮永世长存！<br>
君指先跃动の光は、私の一生不变の信仰に、唯私の超电永世生き <br>
你指尖跃动的电光，是我此生不变的信仰，唯我超电磁炮永世长存！<br>
君指先跃动の光は、私の一生不变の信仰に、唯私の超电永世生き <br>
你指尖跃动的电光，是我此生不变的信仰，唯我超电磁炮永世长存！<br>
君指先跃动の光は、私の一生不变の信仰に、唯私の超电永世生き <br>
你指尖跃动的电光，是我此生不变的信仰，唯我超电磁炮永世长存！<br>
君指先跃动の光は、私の一生不变の信仰に、唯私の超电永世生き <br>
你指尖跃动的电光，是我此生不变的信仰，唯我超电磁炮永世长存！<br>
君指先跃动の光は、私の一生不变の信仰に、唯私の超电永世生き <br>
你指尖跃动的电光，是我此生不变的信仰，唯我超电磁炮永世长存！<br>
君指先跃动の光は、私の一生不变の信仰に、唯私の超电永世生き <br>
你指尖跃动的电光，是我此生不变的信仰，唯我超电磁炮永世长存！<br>
君指先跃动の光は、私の一生不变の信仰に、唯私の超电永世生き <br>
你指尖跃动的电光，是我此生不变的信仰，唯我超电磁炮永世长存！<br>
君指先跃动の光は、私の一生不变の信仰に、唯私の超电永世生き <br>
你指尖跃动的电光，是我此生不变的信仰，唯我超电磁炮永世长存！<br>
君指先跃动の光は、私の一生不变の信仰に、唯私の超电永世生き <br>
你指尖跃动的电光，是我此生不变的信仰，唯我超电磁炮永世长存！<br>
君指先跃动の光は、私の一生不变の信仰に、唯私の超电永世生き <br>
你指尖跃动的电光，是我此生不变的信仰，唯我超电磁炮永世长存！<br>
君指先跃动の光は、私の一生不变の信仰に、唯私の超电永世生き <br>
你指尖跃动的电光，是我此生不变的信仰，唯我超电磁炮永世长存！<br>
君指先跃动の光は、私の一生不变の信仰に、唯私の超电永世生き <br>
你指尖跃动的电光，是我此生不变的信仰，唯我超电磁炮永世长存！<br>
君指先跃动の光は、私の一生不变の信仰に、唯私の超电永世生き <br>
你指尖跃动的电光，是我此生不变的信仰，唯我超电磁炮永世长存！<br>
君指先跃动の光は、私の一生不变の信仰に、唯私の超电永世生き <br>
你指尖跃动的电光，是我此生不变的信仰，唯我超电磁炮永世长存！<br>
君指先跃动の光は、私の一生不变の信仰に、唯私の超电永世生き <br>
你指尖跃动的电光，是我此生不变的信仰，唯我超电磁炮永世长存！<br>
君指先跃动の光は、私の一生不变の信仰に、唯私の超电永世生き <br>
你指尖跃动的电光，是我此生不变的信仰，唯我超电磁炮永世长存！<br>
君指先跃动の光は、私の一生不变の信仰に、唯私の超电永世生き <br>
你指尖跃动的电光，是我此生不变的信仰，唯我超电磁炮永世长存！<br>
君指先跃动の光は、私の一生不变の信仰に、唯私の超电永世生き <br>
你指尖跃动的电光，是我此生不变的信仰，唯我超电磁炮永世长存！<br>
君指先跃动の光は、私の一生不变の信仰に、唯私の超电永世生き <br>
你指尖跃动的电光，是我此生不变的信仰，唯我超电磁炮永世长存！<br>
君指先跃动の光は、私の一生不变の信仰に、唯私の超电永世生き <br>
你指尖跃动的电光，是我此生不变的信仰，唯我超电磁炮永世长存！<br>
君指先跃动の光は、私の一生不变の信仰に、唯私の超电永世生き <br>
你指尖跃动的电光，是我此生不变的信仰，唯我超电磁炮永世长存！<br>
君指先跃动の光は、私の一生不变の信仰に、唯私の超电永世生き <br>
你指尖跃动的电光，是我此生不变的信仰，唯我超电磁炮永世长存！<br>
君指先跃动の光は、私の一生不变の信仰に、唯私の超电永世生き <br>
你指尖跃动的电光，是我此生不变的信仰，唯我超电磁炮永世长存！<br>
君指先跃动の光は、私の一生不变の信仰に、唯私の超电永世生き <br>
你指尖跃动的电光，是我此生不变的信仰，唯我超电磁炮永世长存！<br>
君指先跃动の光は、私の一生不变の信仰に、唯私の超电永世生き <br>
你指尖跃动的电光，是我此生不变的信仰，唯我超电磁炮永世长存！<br>
君指先跃动の光は、私の一生不变の信仰に、唯私の超电永世生き <br>
你指尖跃动的电光，是我此生不变的信仰，唯我超电磁炮永世长存！<br>
君指先跃动の光は、私の一生不变の信仰に、唯私の超电永世生き <br>
你指尖跃动的电光，是我此生不变的信仰，唯我超电磁炮永世长存！<br>
君指先跃动の光は、私の一生不变の信仰に、唯私の超电永世生き <br>
你指尖跃动的电光，是我此生不变的信仰，唯我超电磁炮永世长存！<br>
君指先跃动の光は、私の一生不变の信仰に、唯私の超电永世生き <br>
你指尖跃动的电光，是我此生不变的信仰，唯我超电磁炮永世长存！<br>
君指先跃动の光は、私の一生不变の信仰に、唯私の超电永世生き <br>
你指尖跃动的电光，是我此生不变的信仰，唯我超电磁炮永世长存！<br>
君指先跃动の光は、私の一生不变の信仰に、唯私の超电永世生き <br>
你指尖跃动的电光，是我此生不变的信仰，唯我超电磁炮永世长存！<br>
君指先跃动の光は、私の一生不变の信仰に、唯私の超电永世生き <br>
你指尖跃动的电光，是我此生不变的信仰，唯我超电磁炮永世长存！<br>
君指先跃动の光は、私の一生不变の信仰に、唯私の超电永世生き <br>
你指尖跃动的电光，是我此生不变的信仰，唯我超电磁炮永世长存！<br>
君指先跃动の光は、私の一生不变の信仰に、唯私の超电永世生き <br>
你指尖跃动的电光，是我此生不变的信仰，唯我超电磁炮永世长存！<br>
君指先跃动の光は、私の一生不变の信仰に、唯私の超电永世生き <br>
你指尖跃动的电光，是我此生不变的信仰，唯我超电磁炮永世长存！<br>
君指先跃动の光は、私の一生不变の信仰に、唯私の超电永世生き <br>
你指尖跃动的电光，是我此生不变的信仰，唯我超电磁炮永世长存！<br>

</a>

> 这是谁放的这么多乱七八糟的东西 ——御坂御坂如此疑惑地说道
>
> ——admincmd-a

---

### 好耶是女装
[好耶是女装](https://github.com/Cute-Dress/Dress)

## STALL! STALL! 嘚嘚嘚嘚嘚嘚 STALL! STALL! 嘚嘚嘚嘚嘚嘚 STALL! STALL! 嘚嘚嘚嘚嘚嘚
博南，你的电话响了。

> ### 博南拉杆
> 
> 梗起源于AF447空难的副驾驶博南。当时飞机飞越雷暴区，在途中空速管结冰导致数据不全，自动驾驶断开。其实，博南只需要控制住飞机一分钟，空速管就会自动解冻。然而博南开始向后拉杆，大角度爬升的同时使飞机进入失速。当时驾驶舱内另一名副驾驶做出了失速的改出操作，压低机头，但博南还在拉杆。由于空客的无反馈侧杆设置，导致两个相反的操作互相抵消，直到飞机坠毁，博南还是不知道为什么他做错了。
> 
> 有诗云：”博南副机长，拉杆进大洋。“
>
> @see <https://zhuanlan.zhihu.com/p/488357178>

R.I.P.
