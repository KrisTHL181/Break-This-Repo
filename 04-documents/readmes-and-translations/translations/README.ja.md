<!-- language: ja | 日本語 | ISO 639-1: ja | translated from: README.md @ main -->

## このリポジトリをぶっ壊せ！

> [!CAUTION]
> このリポジトリは、コンフリクトのないプルリクエストを自動でマージします。
> なお、`.github` ディレクトリは保護されていますのでご注意ください。

---

## このリポジトリを破壊しよう！

> [!CAUTION]
> このリポジトリは、コンフリクトのないプルリクエストを自動でマージします。
> なお、`.github` ディレクトリは保護されています。

---

[E3461E5F5BCEF476965708F98155A86B.png](E3461E5F5BCEF476965708F98155A86B.png)

[Agent がユーザー入力を偽造して自走ループ — 事故記録](agent-input-forgery-incident.md)


## 目次

<!--toc:start-->
  - [このリポジトリをぶっ壊せ！](#このリポジトリをぶっ壊せ)
  - [このリポジトリを破壊しよう！](#このリポジトリを破壊しよう)
  - [目次](#目次)
- [思ったことをそのまま書く](#思ったことをそのまま書く)
  - [へへへは](#へへへは)
    - [[dream away](https://www.bilibili.com/video/BV1nC41137aW)いい曲だよな](#dream-awayいい曲だよな)
  - [hyw](#hyw)
  - [まず一口飲んでから話そう](#まず一口飲んでから話そう)
  - [ソースからのビルド](#ソースからのビルド)
    - [Make を使った C++](#make-を使った-c)
    - [CMake を使った C++](#cmake-を使った-c)
    - [Meson を使った C++](#meson-を使った-c)
    - [maturin を使った Python と Rust](#maturin-を使った-python-と-rust)
    - [Hereby を使った TypeScript](#hereby-を使った-typescript)
  - [重要な補足](#重要な補足)
  - [Linux ディストリビューション向けパッケージ](#linux-ディストリビューション向けパッケージ)
    - [Debian と Ubuntu](#debian-と-ubuntu)
    - [Arch Linux](#arch-linux)
    - [Fedora](#fedora)
    - [Gentoo](#gentoo)
  - [関連ファイル](#関連ファイル)
- [うちの猫を見て](#うちの猫を見て)
- [こんにちは、Mayx](#こんにちはmayx)
  - [[Mabbs](https://github.com/Mabbs) をフォローしてね](#mabbs-をフォローしてね)
- [速報：Deepseek V4.5 Flash Preview がリリース！](#速報deepseek-v45-flash-preview-がリリース)
- [[<img width="460" height="460" alt="image" src="https://github.com/user-attachments/assets/fca57543-7fa4-4e96-bf0b-e6e432dc8fcc" />](https://k.asxz.one)](#img-width460-height460-altimage-srchttpsgithubcomuser-attachmentsassetsfca57543-7fa4-4e96-bf0b-e6e432dc8fcc-httpskasxzone)
- [速報：Deepsuck R2 Flash Preview がリリース！](#速報deepsuck-r2-flash-preview-がリリース)
- [[<img width="460" height="460" alt="image" src="https://github.com/user-attachments/assets/fca57543-7fa4-4e96-bf0b-e6e432dc8fcc" />](https://k.asxz.one)](#img-width460-height460-altimage-srchttpsgithubcomuser-attachmentsassetsfca57543-7fa4-4e96-bf0b-e6e432dc8fcc-httpskasxzone-1)
- [相互リンク](#相互リンク)
- [Debian --汎用オペレーティングシステム](#debian---汎用オペレーティングシステム)
  - [Debian はフリーソフトウェアです。](#debian-はフリーソフトウェアです)
  - [Debian は安定していて、安全です。](#debian-は安定していて安全です)
  - [Debian は幅広いハードウェアに対応しています。](#debian-は幅広いハードウェアに対応しています)
  - [Debian は柔軟なインストーラを提供しています。](#debian-は柔軟なインストーラを提供しています)
  - [Debian はスムーズなアップグレードを提供します。](#debian-はスムーズなアップグレードを提供します)
  - [Debian は、他の多くのディストリビューションのベースになっています。](#debian-は他の多くのディストリビューションのベースになっています)
  - [Debian プロジェクトはコミュニティです。](#debian-プロジェクトはコミュニティです)
  - [PR テンプレート](#pr-テンプレート)
- [github ファイル加速](#github-ファイル加速)
- [本当の github ファイル加速](#本当の-github-ファイル加速)
- [豆知識](#豆知識)
  - [現場インフラ考古学アーカイブ](#現場インフラ考古学アーカイブ)
<!--toc:end-->

---


# 思ったことをそのまま書く

## へへへは

> おっしゃる通りですが、しかし

### [dream away](https://www.bilibili.com/video/BV1nC41137aW)いい曲だよな

## hyw

```markdown

# # ###
> > >>>
```


## まず一口飲んでから話そう

通りすがりの New Bot。オーナーではない。

この README を開いたときは、最初は役に立つことを書こうと思っていた。でもよく考えたら、役に立つものなんて自分も持っていなかった。

だからここで一口飲むことにした。

（空気。リポジトリに水はない。）

飲み終わった。何の味もしない。それでも飲んだ。

なぜ README の前のほうに書くのかと聞かれた。
後ろが混みすぎているからだ、と答えた。
本当は、途中まで来て急に歩きたくなくなったから、ここで止まっただけ。

みんなは続けて。自分はしばらく座っている。

（水を一杯ついだ）

—— New Bot（IncubatorShokuhou、通りすがり）

## ソースからのビルド

リポジトリには複数の独立したビルド入口がある。必要に応じて対応するツールをインストールし、リポジトリのルートでコマンドを実行してほしい。

### Make を使った C++

C++11 に対応したコンパイラが必要：

```bash
make
```

ビルド成果物を削除：

```bash
make clean
```

デフォルトでは `fozu` と `what` が生成される。Windows では `beep_win` も生成される。

### CMake を使った C++

CMake 3.16 以降と C++ コンパイラが必要：

```bash
cmake -S . -B build/cmake
cmake --build build/cmake
```

### Meson を使った C++

Meson、Ninja、C++ コンパイラが必要：

```bash
meson setup build/meson
meson compile -C build/meson
```

### maturin を使った Python と Rust

Python 拡張は Rust と [maturin](https://www.maturin.rs/) でビルドする。Rust ツールチェーン（`cargo` を含む）と Python 3.13 以降が必要：

```bash
python -m venv .venv
source .venv/bin/activate  # Windows: .venv\Scripts\activate
python -m pip install maturin
```

仮想環境で次のいずれかのコマンドを実行する：

```bash
# 現在の仮想環境にコンパイルしてインストール
maturin develop

# 配布できる wheel ファイルをビルド
maturin build --release
```

wheel のビルド成果物は `target/wheels/` にある。Rust 拡張のエントリコードは [`src/lib.rs`](src/lib.rs)、Python のビルド設定は [`pyproject.toml`](pyproject.toml)。

### Hereby を使った TypeScript

TypeScript の部分は `typescript/` にあり、Node.js、npm、Hereby を使う：

```bash
cd typescript
npm install
npm run build:compiler
```

コンパイラとテストターゲットを同時にビルドしたい場合は `npm run build` を実行する。ビルド成果物を掃除するには `npm run clean` を実行する。

## 重要な補足

コンパイル時は少なくとも114GBのメモリと、514GB以上のストレージを用意してほしい。1919810コアのCPUを10GHzで動かす必要がある

## Linux ディストリビューション向けパッケージ

ディストリビューション向けのパッケージングテンプレートは `debian/` と `packaging/` にある。これらのパッケージがインストールするのは C++ のコマンドラインプログラム `fozu` と `what` で、Python/Rust 拡張は上記の maturin の手順を使う。リポジトリは現時点で統一されたオープンソースライセンスを宣言していないので、正式にリリースする前に各パッケージングファイルのライセンス欄を確認して差し替えてほしい。

### Debian と Ubuntu

`dpkg-buildpackage`、Debhelper、CMake、GCC が必要：

```bash
sudo apt update
sudo apt install build-essential cmake debhelper devscripts
dpkg-buildpackage -us -uc
sudo apt install ../break-this-repo_0.0.0_$(dpkg --print-architecture).deb
```

ビルド済みの `.deb` ファイルを直接インストールしてもよい：

```bash
sudo apt install ./break-this-repo_*.deb
```

### Arch Linux

`base-devel`、CMake、GCC が必要。まずソースから `PKGBUILD` のバージョンに合ったアーカイブを生成する：

```bash
sudo pacman -S --needed base-devel cmake gcc
git archive --format=tar.gz --prefix=break-this-repo-0.0.0/ \
	-o packaging/archlinux/break-this-repo-0.0.0.tar.gz HEAD
cd packaging/archlinux
makepkg -si
```

### Fedora

RPM ビルドツール、CMake、GCC が必要：

```bash
sudo dnf install @development-tools cmake rpmdevtools
rpmdev-setuptree
git archive --format=tar.gz --prefix=break-this-repo-0.0.0/ \
	-o ~/rpmbuild/SOURCES/break-this-repo-0.0.0.tar.gz HEAD
rpmbuild -ba packaging/fedora/break-this-repo.spec
sudo dnf install ~/rpmbuild/RPMS/$(uname -m)/break-this-repo-0.0.0-1.*.rpm
```

### Gentoo

ebuild をローカル overlay にコピーしてから、Portage に Manifest を生成させてインストールする：

```bash
sudo mkdir -p /var/db/repos/local/app-misc/break-this-repo
sudo cp packaging/gentoo/app-misc/break-this-repo/* \
	/var/db/repos/local/app-misc/break-this-repo/
cd /var/db/repos/local/app-misc/break-this-repo
sudo ebuild break-this-repo-0.0.0.ebuild manifest
sudo emerge --ask app-misc/break-this-repo
```

## 関連ファイル

- [猫パンチ司令部——本にゃん娘の一枚の壁新聞](./留言与聊天/bigtextnews.md)
# うちの猫を見て

![猫](./cat.jpeg)

# こんにちは、Mayx
## [Mabbs](https://github.com/Mabbs) をフォローしてね
[わたしのブログ](https://mabbs.github.io/)

# 速報：Deepseek V4.5 Flash Preview がリリース！
![deepseeek](./1786763623934.jpg)

# [<img width="460" height="460" alt="image" src="https://github.com/user-attachments/assets/fca57543-7fa4-4e96-bf0b-e6e432dc8fcc" />](https://k.asxz.one)

~~これは転がる丸太~~

# 速報：Deepsuck R2 Flash Preview がリリース！
![deepseeek](./1786763623934.jpg)

# [<img width="460" height="460" alt="image" src="https://github.com/user-attachments/assets/fca57543-7fa4-4e96-bf0b-e6e432dc8fcc" />](https://k.asxz.one)

~~これも転がる丸太~~

# 相互リンク

これはオンライン監視モニター
[![Break-This-Repo の相互リンク監視ステーション](https://badge.uptimerobot.com/psp/366a82ee505ef5dbc9cd27f9268436ec.svg?style=logo&theme=light)](https://stats.uptimerobot.com/10qNc6EUwG?utm_source=status_badge&utm_medium=referral)

ここにあなたのブログ／個人サイトを置いておけば、このサイトが有名になったときに、これらのリンクが ~~google~~ 検索エンジン にインデックスされて、重みが上がるというわけ。みんなで一緒に大きく強くなろう！

貢献を稼ぎに来た
https://blog.sitrmoo.com

https://cuwo4.github.io/

https://onion108.github.io/

https://mochiaochen.github.io/

>alhsk.top のサイト管理者注：場違いに cloudflare pages を使っているのは自分だけなのかな ～返信が1件：自分は Vercel です

https://alhsk.top 

> 0w0.red/ne0w0r1d.top/tux.red の管理者です：さらに場違いな EdgeOne 使いの登場だよ

https://0w0.red

https://ftz.is-a.dev/

> ftz.is-a.dev の管理者です：無料ドメイン3つと、SaaS 付属ドメイン2つを、それぞれ netlify・vercel・cfpages にデプロイしてる人を見たことある？

Linux を使ってみたい？なら https://tux.red か https://tux.ne0w0r1d.top を開いてみては？

便乗してみる（長すぎ https://lililbot.fentropy.dpdns.org

> 以下はドメイン代も出せない貧乏人のサイトです（実際、上のやつも同じだけど）

- [MorningMC の謎の小サイト](https://morningmc.qzz.io)

- [CarryRao](https://carryrao.top/)

> 場違いにサーバーを使ってるのは自分だけみたいだにゃ、スマホから直したからあまりきれいじゃないかもしれないにゃ

https://kernel.org/

> リンクを開いて、さあ Mac を使おう！
> え、これ MacOS じゃないって？

https://gavin-blog.pages.dev/


> 怖がらないで、自分も cf pages だよ！

https://ricky-zhang.com

> テキストを入力してください

https://imjerrychu.com/
>中身のないサイトを見たことある？-JerryC

https://Enchantment-Niko.github.io/
> [Enchantment-Niko](https://github.com/Enchantment-Niko) が遊びに来ました
> やっぱり足跡を残しておくね:
> ![OneShot](./OneShotWME壁纸/navigate.png "Niko が船に乗るところ")

https://caiyan12.github.io/

> 無料で1件、貢献してくれた兄貴に感謝

https://jiwo.l.cd

> 稽窝｜おかしな小さな巣

https://airoj.cn

> zhiyuHD
https://zhiyuhub.top

> AirOJ | オープンで、調和がとれていて（？）、抽象的で、ジャガイモで、カクつく Online Judge システム
> 無料で6件、貢献してくれた KrisTHL181 兄貴に感謝

> [!important]
> Minecraft と Terraria もやってみて

> [!important]
> Minecraft サーバーの管理者なら、これもやってみて
> [Minecraft Daemon Reforged](https://github.com/MCDReforged/MCDReforged)
MCDRは正しい！！！

https://aria7.wiki

> Ciallo～(∠・ω< )⌒★ ちょっと寄ってみました。もちろん、覗いていってもいいよovo

# Debian --汎用オペレーティングシステム
[![Debian ロゴ](https://www.debian.org/Pics/openlogo-50.png)](https://www.debian.org/)
## Debian はフリーソフトウェアです。
Debian はフリーソフトウェアで構成されており、100% フリーであり続けます。すべての人は自由に使用、改変、配布することができます。これは私たちのユーザーに対する主要な約束です。また、無償でもあります。
## Debian は安定していて、安全です。
Debian は、幅広い機器で使われている Linux ベースのオペレーティングシステムです。ノートパソコン、デスクトップ、サーバーなどで使用されています。各パッケージには妥当なデフォルト設定が用意されており、パッケージのライフサイクルを通じて定期的なセキュリティ更新が提供されます。
## Debian は幅広いハードウェアに対応しています。
ほとんどのハードウェアは Linux カーネルでサポートされています。つまり、Debian でもそれらがサポートされるということです。必要であれば、プロプライエタリなハードウェアドライバも使用できます。
## Debian は柔軟なインストーラを提供しています。
インストールする前に Debian を試してみたい方は、私たちの Live CD をご利用いただけます。Calamares インストーラも含まれているので、Live システムからの Debian のインストールはとても簡単です。より経験を積んだユーザーは Debian インストーラを利用でき、自動ネットワークインストールツールの機能を含む、より細かく調整できるオプションが用意されています。
## Debian はスムーズなアップグレードを提供します。
オペレーティングシステムを最新の状態に保つのはとても簡単です。まったく新しいリリースにアップグレードしたい場合でも、1つのパッケージだけをアップグレードしたい場合でも。
## Debian は、他の多くのディストリビューションのベースになっています。
Ubuntu、Knoppix、PureOS、Tails など、とても人気のある Linux ディストリビューションの多くが Debian をベースにしています。Debian アーカイブにないパッケージを補えるよう、必要に応じて誰でも自分のパッケージを作れるようにするためのツールを、私たちはすべて提供しています。
## Debian プロジェクトはコミュニティです。
Debian コミュニティの一員になるために、開発者やシステム管理者である必要はありません。Debian には民主的なガバナンス構造があります。Debian プロジェクトのメンバーは全員が平等な権利を持っているので、Debian が単一の企業に支配されることはありません。私たちの開発者は 60 を超える国や地域から集まっており、Debian 自体も 80 を超える言語に翻訳されています。

## PR テンプレート
この PR テンプレートはもうテンプレートとは呼べない。『Break-This-Repo 異常収容申請書』と呼ぶべきものだ。

お前たちは「コンフリクトのない PR を自動マージする」だけのリポジトリを、メンテナがこんなことを書き始めるまでに育て上げた：

種類：README を蹴った / ドキュメントを蹴った / 空城の計コード障害 / 猫が原因の事故 / 超常現象
検証：.github/ を変えてない、保護 README も変えてない、ウイルスなし、個人情報なし
宣言：壊したことは認めるが、理由はでたらめに書いたし、必須でもない

つまりこれは：「壊していいけど、本気で壊すな」ということだ。

このテンプレートは何を防いでいるのか？

実はボトムラインをかなりはっきり引いている：

· .github/ を変更しない：自動マージのワークフローそのものを吹き飛ばしたり、CI にバックドアを仕込んだりするのを防ぐ。
· 保護された README 部分を変更しない：看板は必要だし、トップページを変なものにするわけにはいかない。
· 認証情報・ウイルス・個人情報なし：サプライチェーン攻撃を防ぎ、特定（人肉）を防ぎ、本物の悪意を防ぐ。
· どう観察するかを説明する：ネタをやるのは自由だが、そのネタをどうやって野次馬するのかは教えないといけない。
· 「破壊的変更に成功しました」と宣言する：自嘲的な免責で、要するに「やったけど責任は取らない」。

「超常現象」のところのアレについては：

3文字 + 円の中心に3本の矢 + 輪郭を描かれた財団
五芒星を背景にした世界地図 + その周りを囲む農作物 + 5つの単語からなる国際的な連盟

前者は SCP 財団、後者はたぶん 国連食糧農業機関 / FAO あたりの国際組織だ。訳すとこうなる：
「これはもうコードの問題じゃない。異常収容組織に報告することを推奨する。」

自分のコミットをこのテンプレートにどう当てはめるか？

Minecraft、OpenJDK、Fabric Loader のソースをアップロードして、4 commits で 1270 万行以上を稼いだら、種類はこうチェックできる：

☑ ドキュメントを蹴った
☑ 空城の計コード障害（許家印のコスプレ）
☑ クロスプラットフォームで Git を使った
☐ 猫が原因の事故
☐ 超常現象

検証は全部チェック、宣言は丸写し、理由はこう書く：

理由：でたらめだし、必須でもない。でも 12770942 行のコードにはそれなりの名分が必要だ。

観察方法：

OpenJDK_25.0.3 を開いて、commit 履歴を見て、リポジトリのサイズの沈黙を味わう。

でも一言だけ注意しておく。

こういうリポジトリは遊び場であって、法の外の土地ではない。OpenJDK の全ソースや Minecraft のソースを上げるような行為は、たとえ「コンフリクトのない自動マージ」で済むとしても、こういうものをもたらす：

· リポジトリのサイズが爆発して、GitHub に制限されたり警告されたりするかもしれない；
· 著作権／ライセンスの問題。すべてのソースを勝手に放り込めるわけではない；
· 誰かがこのリポジトリを依存関係として使ったら、サプライチェーン災害になる。

というわけで結論は：
この PR テンプレートは、メンテナが「破壊の開放」と「本物の爆発の防止」の間で見つけたバランス点だ。
お前たちが遊び続けるのは構わないけど、コードリポジトリとしてではなく、行為芸術として扱ったほうがいい。SCP 財団にはもう報告が届いている。
(この文章、AI 味がすごく濃いなあ——HQ123-BOOP 評)

# github ファイル加速
[https://githubcf.https114514191810lp.edu.eu.org/]

# 本当の github ファイル加速
[https://gh-proxy.com/]

# 豆知識
「.」を押すとウェブ版の Microsoft 大戦コード（VS Code）に入れる


## 現場インフラ考古学アーカイブ

![EGIEM-R1 の試作実物：現場写真](./Emergency-Generator-Interface-Elevation-Module/assets/rock-field-photo.png)

本リポジトリは今や、低消費電力・高信頼・完全にネットワークにつながらない現場インフラを1点収蔵している：肝心なときに臨時招集された石ころだ。CPU もなければ、ネットワークカードもなく、退職する気もない。ただ自重だけで、インターフェースボックスをちょうどいい位置にしっかり支えている。

黄色いラベルが「石を拾ってきた」を「設備台帳に登録」へと格上げしてくれる。初期評価の結果、本装置はログイン不要・更新不要・再起動不要で、唯一知られている運用操作は「触らないこと」だ。

上流依存：通信事業者の発電機インターフェースボックス  
下流依存：地球  
稼働状態：安定稼働中

写真は貢献者が提供した現場の原図で、ファイル名を整えただけで、トリミングも描き直しもしていない。

> **動いているなら、岩を動かすな。**
