![LunarCore](https://socialify.git.ci/Melledy/LunarCore/image?description=1&descriptionEditable=A%20game%20server%20reimplementation%20for%20version%202.2.0%20of%20a%20certain%20turn-based%20anime%20game%20for%20educational%20purposes.%20&font=Inter&forks=1&issues=1&language=1&name=1&owner=1&pulls=1&stargazers=1&theme=Light)
<div align="center"><img alt="GitHub release (latest by date)" src="https://img.shields.io/github/v/release/Melledy/LunarCore?logo=java&style=for-the-badge"> <img alt="GitHub" src="https://img.shields.io/github/license/Melledy/LunarCore?style=for-the-badge"> <img alt="GitHub last commit" src="https://img.shields.io/github/last-commit/Melledy/LunarCore?style=for-the-badge"> <img alt="GitHub Workflow Status" src="https://img.shields.io/github/actions/workflow/status/Melledy/LunarCore/build.yml?branch=development&logo=github&style=for-the-badge"></div>

<div align="center"><a href="https://discord.gg/cfPKJ6N5hw"><img alt="Discord - LunarCore" src="https://img.shields.io/discord/1163718404067303444?label=Discord&logo=discord&style=for-the-badge"></a></div>

[EN](README.md) | [ID](docs/README_id-ID.md) | [简中](docs/README_zh-CN.md) | [繁中](docs/README_zh-TW.md) | [JP](docs/README_ja-JP.md) | [RU](docs/README_ru-RU.md) | [FR](docs/README_fr-FR.md) | [KR](docs/README_ko-KR.md) | [VI](docs/README_vi-VI.md)

**Attention:** 追加のサポート、質問、または議論がある場合は、 [Discord](https://discord.gg/cfPKJ6N5hw).

### 注目すべき機能
- 基本ゲーム機能：ログイン、チームのセットアップ、バッグ、基本的なシーン/エンティティの管理
- モンスター戦闘
- オーバーワールドのモンスター/プロップ/NPCのスポーン
- キャラクターの秘技
- 消耗品
- NPCショップ
- ガチャシステム
- メールシステム
- フレンドシステム（アシストはまだ機能していません）
- 忘却の庭
- 虚構叙事
- 模擬宇宙（実行はできますが、多くの機能が不足しています）

# サーバーとクライアントの実行

### 必須
* [Java 17 JDK](https://www.oracle.com/java/technologies/javase/jdk17-archive-downloads.html)

### 推奨
* [MongoDB 4.0+](https://www.mongodb.com/try/download/community)

### サーバーのコンパイル
1. システムターミナルを開き、`./gradlew jar` でサーバーをコンパイル。
2. サーバーのディレクトリに `resources` という名前のフォルダを作成。
3. [https://github.com/Dimbreath/StarRailData](https://github.com/Dimbreath/StarRailData) から `Config`、`TextMap`、および `ExcelBin` フォルダをダウンロードし、それらを`resources`に配置。
4. `/resources/Config/LevelOutput`を削除。
5. [https://gitlab.com/Melledy/LunarCore-Configs](https://gitlab.com/Melledy/LunarCore-Configs)から`Config`をダウンロードし、`resources`に配置。※これらはワールドの生成に関するもので、非常に重要です。
6. システムターミナルから `java -jar LunarCore.jar` を使用してサーバーを実行。Lunar Coreにはデータベースのための組み込みの内部MongoDBサーバーが付属しているため、MongoDBのインストールは必要ありません。ただし、MongoDBのインストールを強くお勧めします。

### クライアントとの接続（Fiddler）
1. **同じクライアントで公式サーバーとHoyoverseアカウントに少なくとも一度ログインしてゲームデータをダウンロードしてください。**
2. [Fiddler Classic](https://www.telerik.com/fiddler) をインストールし、実行します。
3. Fiddlerをhttpsトラフィックを復号化するように設定します（ツール -> オプション -> HTTPS -> HTTPSトラフィックを復号化）。 `サーバー証明書のエラーを無視する` がチェックされていることを確認してください。
4. Fiddler ClassicのFiddlerscriptタブに以下のコードをコピーして貼り付けます：

```javascript
import System;
import System.Windows.Forms;
import Fiddler;
import System.Text.RegularExpressions;

class Handlers
{
    static function OnBeforeRequest(oS: Session) {
        if (oS.host.EndsWith(".starrails.com") || oS.host.EndsWith(".hoyoverse.com") || oS.host.EndsWith(".mihoyo.com") || oS.host.EndsWith(".bhsr.com")) {
            oS.host = "localhost"; // これは別のIPアドレスに置き換えることもできます。
        }
    }
};
```

5. configで`autoCreateAccount`をtrueにした場合、このステップはスキップされます。 そうでない場合、サーバ＾コンソールに`/account create [account name]`と入力しアカウントを作成します。
6. 作成したアカウント名と任意のパスワードでログインします。(パスワードはサーバーで無視されます。)

### サーバーコマンド
サーバーコマンドはサーバーコンソールまたはゲーム内で実行できます。各プレイヤーのフレンドリストには、ゲーム内でコマンドを使用するための "Server" という名前のユーザーがいます。

```
/account {create | delete} [username] (uidに紐付けされている). アカウントを作成または削除します。
/avatar lv(level) p(ascension) r(eidolon) s(skill levels). 現在のアバターのプロパティを設定します。
/clear {relics | lightcones | materials | items}. プレイヤーのインベントリから指定したアイテムを削除します。
/gender {male | female}. プレイヤーの性別を設定します。
/give [item id] x[amount] lv[number]. ターゲットのプレイヤーにアイテムを与えます。
/giveall {materials | avatars}. ターゲットのプレイヤーにアイテムを与えます。
/heal. プレイヤーのキャラクターを回復します。
/help. 利用可能なコマンドの一覧を表示します。
/kick @[player id]. サーバーからプレイヤーをキックする。
/mail [content]. ターゲットのプレイヤーにシステムメールを送信します。
/permission {add | remove | clear} [permission]. ターゲットのプレイヤーから権限を付与/削除します。
/refill. SPを回復します。
/reload. サーバーコンフィギュレーションを再読み込みします。
/scene [scene id] [floor id]. プレイヤーを指定したシーンにテレポートします。
/spawn [monster/prop id] x[amount] s[stage id]. ターゲットのプレイヤーの近くにモンスターまたはプロップを生成します。
/stop. サーバーを終了します。
/unstuck @[player id]. オフラインプレイヤーが読み込み不可のシーンにいる場合、スタックを解除します。
/worldlevel [world level]. ターゲットのプレイヤーの均衡レベルを設定します。
```
