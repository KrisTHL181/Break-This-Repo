![LunarCore](https://socialify.git.ci/Melledy/LunarCore/image?description=1&descriptionEditable=A%20game%20server%20reimplementation%20for%20version%202.4.0%20of%20a%20certain%20turn-based%20anime%20game%20for%20educational%20purposes.%20&font=Inter&forks=1&issues=1&language=1&name=1&owner=1&pulls=1&stargazers=1&theme=Light)
<div align="center"><img alt="GitHub release (latest by date)" src="https://img.shields.io/github/v/release/Melledy/LunarCore?logo=java&style=for-the-badge"> <img alt="GitHub" src="https://img.shields.io/github/license/Melledy/LunarCore?style=for-the-badge"> <img alt="GitHub last commit" src="https://img.shields.io/github/last-commit/Melledy/LunarCore?style=for-the-badge"> <img alt="GitHub Workflow Status" src="https://img.shields.io/github/actions/workflow/status/Melledy/LunarCore/build.yml?branch=development&logo=github&style=for-the-badge"></div>

<div align="center"><a href="https://discord.gg/cfPKJ6N5hw"><img alt="Discord - LunarCore" src="https://img.shields.io/discord/1163718404067303444?label=Discord&logo=discord&style=for-the-badge"></a></div>

[EN](README.md) | [ID](docs/README_id-ID.md) | [简中](docs/README_zh-CN.md) | [繁中](docs/README_zh-TW.md) | [JP](docs/README_ja-JP.md) | [RU](docs/README_ru-RU.md) | [FR](docs/README_fr-FR.md) | [KR](docs/README_ko-KR.md) | [VI](docs/README_vi-VI.md)

**Perhatian:** Untuk dukungan, pertanyaan, atau diskusi tambahan, lihat di [Discord](https://discord.gg/cfPKJ6N5hw).

### Fitur penting
- Fitur game umum: Log in, setup team, inventory, scene/menegemen entitas
- Pertarungan monster
- Natural world monster/prop/NPC spawns
- Teknik char(trace)
- Crafting/Consumables working
- NPC shops handled
- Sistem Gacha
- Sistem Pesan
- Friendlist (assist masih belum berfungsi)
- Forgotten hall
- Pure Fiction
- Simulated universe (Beberapa fitur mungkin akan bug)

# Menjalankan server dan client

### Prasyarat
* [Java 17 JDK](https://www.oracle.com/java/technologies/javase/jdk17-archive-downloads.html)

### Recommended
* [MongoDB 4.0+](https://www.mongodb.com/try/download/community)

### Kompilasi servernya
1. Buka sistem terminal-mu, dan kompilasi server dengan `./gradlew jar`
2. Buat folder dengan nama `resources` di server ditactorymu
3. Download `Config`, `TextMap`, dan `ExcelBin` folders dari [https://github.com/Dimbreath/StarRailData](https://github.com/Dimbreath/StarRailData) dan taruh di dalam folder resource mu
4. Run server dengan `java -jar LunarCore.jar` dari sistem terminal mu. Lunar Core dilengkapi dengan server MongoDB internal bawaan untuk databasenya, jadi tidak diperlukan instalasi Mongodb. Namun, sangat disarankan untuk tetap menginstal Mongodb.

### Konek dengan client (metode Fiddler)
1. **Masuk dengan klien ke server resmi dan akun Hoyoverse setidaknya sekali untuk mengunduh data game.**
2. Install dan dapatkan [Fiddler Classic](https://www.telerik.com/fiddler) berjalan.
3. Salin dan tempel kode berikut ke tab Fiddlerscript di Fiddler Classic. Ingatlah untuk menyimpan skrip fiddler setelah Anda menyalin dan menempelkannya:

```
import System;
import System.Windows.Forms;
import Fiddler;
import System.Text.RegularExpressions;

class Handlers
{
    static function OnBeforeRequest(oS: Session) {
        if (oS.host.EndsWith(".starrails.com") || oS.host.EndsWith(".hoyoverse.com") || oS.host.EndsWith(".mihoyo.com") || oS.host.EndsWith(".bhsr.com")) {
            oS.oRequest.headers.UriScheme = "http";
            oS.host = "localhost"; // This can also be replaced with another IP address.
        }
    }
};
```

4. Jika `autoCreateAccount` disetel ke true di konfigurasi, maka Anda dapat melewati langkah ini. Jika tidak, ketik `/account create [nama akun]` di konsol server untuk membuat akun.
5. Login dengan nama akun Anda, kolom kata sandi diabaikan oleh server dan dapat diatur ke apa saja(bebas isi apa saja).

### Server commands
Perintah server dapat dijalankan di konsol server atau dalam game. Ada pengguna tiruan bernama "Server" di setiap daftar teman pemain yang dapat Anda kirimi pesan(Command) untuk menggunakan perintah dalam game.

```
/account {create | delete} [nama pengguna] (uid pemain yang dipesan). Membuat atau menghapus akun.
/avatar lv(level) p(ascension) r(eidolon) s(skill levels). Mengatur properti avatar saat ini.
/clear {relics | lightcones | materials | items}. Menghapus item yang difilter dari inventaris pemain.
/gender {male | female}. Menetapkan jenis kelamin Trailblazer.
/give [item id] x[amount] lv[number]. Memberi pemain target sebuah item.
/giveall {materials | avatars | lightcones | relics}. Memberikan item pemain yang ditargetkan.(tapi semua item)
/heal. Heals.
/help. Menampilkan daftar command yang tersedia.
/kick @[player id]. Kicks a player dari server.
/mail [content]. Mengirimkan email sistem kepada pemain yang ditargetkan.
/permission {add | remove | clear} [permission]. Memberi/menghapus izin dari pemain yang ditargetkan.
/refill. Isi ulang skill point Anda di dunia terbuka.
/reload. Muat ulang konfigurasi server.
/scene [scene id] [floor id]. Teleportasi pemain ke adegan yang ditentukan.
/spawn [npc monster id/prop id] s[stage id] x[amount] lv[level] r[radius] <battle monster ids...>. Memunculkan monster atau prop di dekat pemain yang ditargetkan.
/stop. Stop server
/unstuck @[player id]. Unstuck pemain offline jika mereka berada dalam adegan yang tidak dapat dimuat.
/worldlevel [world level]. Menetapkan tingkat keseimbangan pemain yang ditargetkan.
```
