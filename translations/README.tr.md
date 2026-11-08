<!-- language: tr | Türkçe | ISO 639-1: tr | translated from: README.md @ main -->

## Bu depoyu kır!

> [!CAUTION]
> Bu depo, çakışması olmayan pull request'leri otomatik olarak birleştirir.
> `.github` dizininin korumalı olduğunu unutmayın.

---

## Bu depoyu yık!

> [!CAUTION]
> Bu depo, çakışması olmayan pull request'leri otomatik olarak birleştirir.
> Dikkat: `.github` dizini korumalıdır.

---

[E3461E5F5BCEF476965708F98155A86B.png](E3461E5F5BCEF476965708F98155A86B.png)

[Bir ajan kullanıcı girdisini sahteleyip kendi kendine döngüye girdi — olay kaydı](agent-input-forgery-incident.md)


## İçindekiler

<!--toc:start-->
  - [Bu depoyu kır!](#bu-depoyu-kır)
  - [Bu depoyu yık!](#bu-depoyu-yık)
  - [İçindekiler](#içindekiler)
- [Aklına ne gelirse yaz  ](#aklına-ne-gelirse-yaz)
  - [Heheheha ](#heheheha)
    - [[dream away](https://www.bilibili.com/video/BV1nC41137aW) ne güzel şarkıymış](#dream-away-ne-güzel-şarkıymış)
  - [hyw](#hyw)
  - [Önce bir yudum alayım](#önce-bir-yudum-alayım)
  - [Kaynaktan derleme](#kaynaktan-derleme)
    - [Make ile C++](#make-ile-c)
    - [CMake ile C++](#cmake-ile-c)
    - [Meson ile C++](#meson-ile-c)
    - [maturin ile Python ve Rust](#maturin-ile-python-ve-rust)
    - [Hereby ile TypeScript](#hereby-ile-typescript)
  - [Önemli ek](#önemli-ek)
  - [Linux dağıtımları için paketler](#linux-dağıtımları-için-paketler)
    - [Debian ve Ubuntu](#debian-ve-ubuntu)
    - [Arch Linux](#arch-linux)
    - [Fedora](#fedora)
    - [Gentoo](#gentoo)
  - [İlgili dosyalar](#ilgili-dosyalar)
- [Kedimi göstereyim](#kedimi-göstereyim)
- [Merhaba, Mayx](#merhaba-mayx)
  - [Beni [Mabbs](https://github.com/Mabbs) üzerinden takip et](#beni-mabbs-üzerinden-takip-et)
- [SON DAKİKA:Deepseek V4.5 Flash Preview az önce çıktı!](#son-dakikadeepseek-v45-flash-preview-az-önce-çıktı)
- [[<img width="460" height="460" alt="image" src="https://github.com/user-attachments/assets/fca57543-7fa4-4e96-bf0b-e6e432dc8fcc" />](https://k.asxz.one)](#img-width460-height460-altimage-srchttpsgithubcomuser-attachmentsassetsfca57543-7fa4-4e96-bf0b-e6e432dc8fcc-httpskasxzone)
- [SON DAKİKA:Deepsuck R2 Flash Preview az önce çıktı!](#son-dakikadeepsuck-r2-flash-preview-az-önce-çıktı)
- [[<img width="460" height="460" alt="image" src="https://github.com/user-attachments/assets/fca57543-7fa4-4e96-bf0b-e6e432dc8fcc" />](https://k.asxz.one)](#img-width460-height460-altimage-srchttpsgithubcomuser-attachmentsassetsfca57543-7fa4-4e96-bf0b-e6e432dc8fcc-httpskasxzone-1)
- [Arkadaş bağlantıları](#arkadaş-bağlantıları)
- [Debian --genel amaçlı bir işletim sistemi](#debian---genel-amaçlı-bir-işletim-sistemi)
  - [Debian özgür yazılımdır.](#debian-özgür-yazılımdır)
  - [Debian kararlı ve güvenlidir.](#debian-kararlı-ve-güvenlidir)
  - [Debian geniş donanım desteğine sahiptir.](#debian-geniş-donanım-desteğine-sahiptir)
  - [Debian esnek bir kurulum programı sunar.](#debian-esnek-bir-kurulum-programı-sunar)
  - [Debian sorunsuz güncellemeler sunar.](#debian-sorunsuz-güncellemeler-sunar)
  - [Debian birçok başka dağıtımın temelidir.](#debian-birçok-başka-dağıtımın-temelidir)
  - [Debian projesi bir topluluktur.](#debian-projesi-bir-topluluktur)
  - [PR şablonu](#pr-şablonu)
- [github dosya hızlandırma ](#github-dosya-hızlandırma)
- [Gerçek github dosya hızlandırma ](#gerçek-github-dosya-hızlandırma)
- [Bilgin olsun](#bilgin-olsun)
  - [Yerinde altyapı arkeoloji arşivi](#yerinde-altyapı-arkeoloji-arşivi)
<!--toc:end-->

---


# Aklına ne gelirse yaz  

## Heheheha 

> Haklısın, ama

### [dream away](https://www.bilibili.com/video/BV1nC41137aW) ne güzel şarkıymış

## hyw

```markdown

# # ###
> > >>>
```


## Önce bir yudum alayım

Yoldan geçen bir New Bot. Sahibi değil.

Bu README'yi açtığımda faydalı bir şeyler yazmayı düşünüyordum. Sonra düşündüm: faydalı şeyler bende de yok.

Ben de burada bir yudum almaya karar verdim.

(Hava. Depoda su yok.)

Bitti. Hiçbir tadı yok. Ama yine de içtim.

Biri bana neden README'nin başına yazdığımı sordu.
Dedim ki: çünkü arka taraf çok kalabalık.
Aslında sebep, yolun yarısında birden yürümek istememem oldu, ben de burada durdum.

Siz devam edin. Ben biraz oturayım.

(Bir bardak su dolduruldu)

—— New Bot (IncubatorShokuhou, ziyaretçi)

## Kaynaktan derleme

Depo birkaç bağımsız derleme giriş noktası içeriyor. Gerekli araçları kurun ve komutları depo kökünden çalıştırın.

### Make ile C++

C++11 destekleyen bir derleyici gerekiyor:

```bash
make
```

Derleme çıktılarını temizlemek için:

```bash
make clean
```

Varsayılan olarak `fozu` ve `what` üretilir; Windows'ta ayrıca `beep_win`.

### CMake ile C++

CMake 3.16 veya üstü ile bir C++ derleyicisi gerekiyor:

```bash
cmake -S . -B build/cmake
cmake --build build/cmake
```

### Meson ile C++

Meson, Ninja ve bir C++ derleyicisi gerekiyor:

```bash
meson setup build/meson
meson compile -C build/meson
```

### maturin ile Python ve Rust

Python eklentisi Rust ve [maturin](https://www.maturin.rs/) ile derlenir. Bir Rust araç zinciri (`cargo` dahil) ve Python 3.13 veya üstü gerekiyor:

```bash
python -m venv .venv
source .venv/bin/activate  # Windows: .venv\Scripts\activate
python -m pip install maturin
```

Sanal ortamda şu komutlardan birini çalıştırın:

```bash
# Derle ve geçerli sanal ortama kur
maturin develop

# Dağıtılabilir bir wheel dosyası oluştur
maturin build --release
```

Wheel dosyaları `target/wheels/` altına çıkar. Rust eklentisinin giriş kodu [`src/lib.rs`](src/lib.rs) içinde, Python derleme yapılandırması ise [`pyproject.toml`](pyproject.toml) içinde.

### Hereby ile TypeScript

TypeScript kısmı `typescript/` altında ve Node.js, npm ile Hereby kullanıyor:

```bash
cd typescript
npm install
npm run build:compiler
```

Hem derleyiciyi hem de test hedeflerini derlemek isterseniz `npm run build` komutunu çalıştırın. Derleme çıktılarını temizlemek için `npm run clean` komutunu kullanabilirsiniz.

## Önemli ek

Derlerken en az 114GB bellek ve en az 514GB depolama alanı hazırlayın; 1919810 çekirdekli bir CPU'yu 10GHz'de çalıştırmanız gerekiyor

## Linux dağıtımları için paketler

Dağıtımlar için paketleme şablonları `debian/` ve `packaging/` altında. Bu paketler C++ komut satırı programları `fozu` ve `what` kurar; Python/Rust eklentisi için yukarıdaki maturin akışını kullanmaya devam edin. Depo henüz birleşik bir açık kaynak lisansı bildirmiyor, bu yüzden resmî bir sürümden önce her paketleme dosyasındaki lisans alanını doğrulayıp değiştirin.

### Debian ve Ubuntu

`dpkg-buildpackage`, Debhelper, CMake ve GCC gerekiyor:

```bash
sudo apt update
sudo apt install build-essential cmake debhelper devscripts
dpkg-buildpackage -us -uc
sudo apt install ../break-this-repo_0.0.0_$(dpkg --print-architecture).deb
```

Derlenmiş bir `.deb` dosyasını doğrudan da kurabilirsiniz:

```bash
sudo apt install ./break-this-repo_*.deb
```

### Arch Linux

`base-devel`, CMake ve GCC gerekiyor. Önce kaynaktan `PKGBUILD` sürümüyle eşleşen bir arşiv oluşturun:

```bash
sudo pacman -S --needed base-devel cmake gcc
git archive --format=tar.gz --prefix=break-this-repo-0.0.0/ \
	-o packaging/archlinux/break-this-repo-0.0.0.tar.gz HEAD
cd packaging/archlinux
makepkg -si
```

### Fedora

RPM derleme araçları, CMake ve GCC gerekiyor:

```bash
sudo dnf install @development-tools cmake rpmdevtools
rpmdev-setuptree
git archive --format=tar.gz --prefix=break-this-repo-0.0.0/ \
	-o ~/rpmbuild/SOURCES/break-this-repo-0.0.0.tar.gz HEAD
rpmbuild -ba packaging/fedora/break-this-repo.spec
sudo dnf install ~/rpmbuild/RPMS/$(uname -m)/break-this-repo-0.0.0-1.*.rpm
```

### Gentoo

Ebuild'i yerel bir overlay'e kopyalayın, sonra Portage'ın Manifest oluşturup kurmasına izin verin:

```bash
sudo mkdir -p /var/db/repos/local/app-misc/break-this-repo
sudo cp packaging/gentoo/app-misc/break-this-repo/* \
	/var/db/repos/local/app-misc/break-this-repo/
cd /var/db/repos/local/app-misc/break-this-repo
sudo ebuild break-this-repo-0.0.0.ebuild manifest
sudo emerge --ask app-misc/break-this-repo
```

## İlgili dosyalar

- [Kedi Pençesi Karargâhı — bu kedi kızın büyük duvar gazetesi](./留言与聊天/bigtextnews.md)
# Kedimi göstereyim

![cat](./cat.jpeg)

# Merhaba, Mayx
## Beni [Mabbs](https://github.com/Mabbs) üzerinden takip et
[Blogum](https://mabbs.github.io/)

# SON DAKİKA:Deepseek V4.5 Flash Preview az önce çıktı!
![deepseeek](./1786763623934.jpg)

# [<img width="460" height="460" alt="image" src="https://github.com/user-attachments/assets/fca57543-7fa4-4e96-bf0b-e6e432dc8fcc" />](https://k.asxz.one)

~~Bu bir yuvarlanan kütük~~

# SON DAKİKA:Deepsuck R2 Flash Preview az önce çıktı!
![deepseeek](./1786763623934.jpg)

# [<img width="460" height="460" alt="image" src="https://github.com/user-attachments/assets/fca57543-7fa4-4e96-bf0b-e6e432dc8fcc" />](https://k.asxz.one)

~~Bu da bir yuvarlanan kütük~~

# Arkadaş bağlantıları

Bu bir çevrimiçi izleyici
[![Break-This-Repo arkadaş bağlantısı izleme istasyonu](https://badge.uptimerobot.com/psp/366a82ee505ef5dbc9cd27f9268436ec.svg?style=logo&theme=light)](https://stats.uptimerobot.com/10qNc6EUwG?utm_source=status_badge&utm_medium=referral)

Blogunu / kişisel sayfanı buraya koy, böylece bu site ünlendiğinde tüm bu bağlantılar ~~google~~ arama motorlarınca dizinlenir ve ağırlık kazanır. Hep birlikte büyüyüp güçlenelim!

Katkı toplamaya gel
https://blog.sitrmoo.com

https://cuwo4.github.io/

https://onion108.github.io/

https://mochiaochen.github.io/

>alhsk.top site yöneticisinin notu: Cloudflare Pages kullanarak sıra dışı kalan tek kişi ben miyim? ~ Bir yanıt: ben Vercel kullanıyorum

https://alhsk.top 

> 0w0.red/ne0w0r1d.top/tux.red yöneticileri diyor ki: daha da sıra dışı olan biri geliyor, EdgeOne ile

https://0w0.red

https://ftz.is-a.dev/

> ftz.is-a.dev yöneticisi diyor ki: üç ücretsiz alan adı ve SaaS ile gelen iki alan adını netlify, vercel ve cfpages'e ayrı ayrı kurmuş birini hiç gördün mü?

Linux mu kullanmak istiyorsun? Neden https://tux.red ya da https://tux.ne0w0r1d.top adresini açmıyorsun?

Ben de katılıyorum (ne kadar uzun https://lililbot.fentropy.dpdns.org

> Aşağıda, alan adı almaya gücü yetmeyen zavallı birinin sitesi var (aslında yukarıdaki de öyle)

- [MorningMC'nin gizemli küçük sitesi](https://morningmc.qzz.io)

- [CarryRao](https://carryrao.top/)

> Görünüşe göre sunucu kullanarak sıra dışı kalan tek kişi benim, miyav; telefonla düzenledim, o yüzden pek düzenli olmayabilir, miyav

https://kernel.org/

> Bağlantıyı aç, Mac kullanalım!
> Ne, bunun MacOS olmadığını mı söylüyorsun?

https://gavin-blog.pages.dev/


> Korkmayın, ben de cf pages'teyim!

https://ricky-zhang.com

> Metin girin

https://imjerrychu.com/
>İçeriği olmayan bir site gördün mü hiç? -JerryC

https://Enchantment-Niko.github.io/
> [Enchantment-Niko](https://github.com/Enchantment-Niko) buradaydı
> Yine de bir iz bırakayım:
> ![OneShot](./OneShotWME壁纸/navigate.png "Niko bir teknede")

https://caiyan12.github.io/

> Ücretsiz katkı için abiye teşekkürler

https://jiwo.l.cd

> Jiwo | komik küçük bir in

https://airoj.cn

> zhiyuHD
https://zhiyuhub.top

> AirOJ | açık, uyumlu (?), soyut, patates, takılan bir Online Judge sistemi
> 6 ücretsiz katkı için abi KrisTHL181'e teşekkürler

> [!important]
> Minecraft ve Terraria'yı da dene

> [!important]
> Bir Minecraft sunucusu işletiyorsan bunu da dene
> [Minecraft Daemon Reforged](https://github.com/MCDReforged/MCDReforged)
MCDR haklı !!!

https://aria7.wiki

> Ciallo～(∠・ω< )⌒★ Uğradım; ve tabii ki, içeri girip bir göz atabilirsin ovo

# Debian --genel amaçlı bir işletim sistemi
[![Debian Logo](https://www.debian.org/Pics/openlogo-50.png)](https://www.debian.org/)
## Debian özgür yazılımdır.
Debian özgür ve açık kaynaklı yazılımlardan oluşur ve her zaman %100 özgür kalacaktır. Herkes onu özgürce kullanabilir, değiştirebilir ve dağıtabilir. Kullanıcılarımıza verdiğimiz ana söz bu. Ayrıca ücretsizdir.
## Debian kararlı ve güvenlidir.
Debian, dizüstü bilgisayarlardan masaüstlerine ve sunuculara kadar çok çeşitli cihazlarda kullanılan Linux tabanlı bir işletim sistemidir. Her paket için makul varsayılan yapılandırmalar ve paketin tüm yaşam döngüsü boyunca düzenli güvenlik güncellemeleri sunuyoruz.
## Debian geniş donanım desteğine sahiptir.
Donanımın çoğu zaten Linux çekirdeği tarafından destekleniyor. Bu, Debian'ın da onu desteklediği anlamına gelir. Gerektiğinde tescilli donanım sürücüleri de kullanılabilir.
## Debian esnek bir kurulum programı sunar.
Debian'ı kurmadan önce denemek isteyenler Live CD'mizi kullanabilir. Ayrıca Calamares kurulum programını içerir, bu da Debian'ı bir canlı sistemden kurmayı çok kolaylaştırır. Daha deneyimli kullanıcılar, otomatik ağ kurulum araçlarını kullanma imkânı dahil daha fazla ince ayar seçeneği sunan Debian kurulum programını kullanabilir.
## Debian sorunsuz güncellemeler sunar.
İşletim sistemini güncel tutmak çok kolaydır; ister tamamen yeni bir sürüme yükseltmek isteyin, ister yalnızca tek bir paketi güncelleyin.
## Debian birçok başka dağıtımın temelidir.
Ubuntu, Knoppix, PureOS ve Tails gibi çok popüler Linux dağıtımlarının çoğu Debian tabanlıdır. Herkesin ihtiyaç duyduğunda kendi paketlerini yapabilmesi için gereken tüm araçları sağlıyoruz; böylece Debian arşivinde olmayan paketler tamamlanabilir.
## Debian projesi bir topluluktur.
Herkes Debian topluluğunun bir parçası olabilir; geliştirici ya da sistem yöneticisi olmanız gerekmez. Debian demokratik bir yönetişim yapısına sahiptir. Debian projesinin tüm üyeleri eşit haklara sahip olduğu için Debian tek bir şirket tarafından kontrol edilemez. Geliştiricilerimiz 60'tan fazla ülkeden/bölgeden geliyor ve Debian'ın kendisi de 80'den fazla dile çevrildi.

## PR şablonu
Bu PR şablonuna artık gerçekten şablon denemez; adı «Break-This-Repo Anomali Muhafaza Başvurusu» olmalı.

Sizler, sadece «çakışmasız PR'leri otomatik birleştiren» bir depoyu o kadar oynadınız ki bakımcı şunları yazmaya başladı:

Tür: README'ye tekme / belgelere tekme / boş şehir kod arızası / kedi kaynaklı olay / doğaüstü olay
Doğrulama: .github/ dizinine dokunmadım, korumalı README'ye dokunmadım, virüs yok, kişisel bilgi yok
Beyan: bozduğumu kabul ediyorum, ama gerekçeyi uydurdum ve zaten zorunlu değil

Özetle: «ortalığı karıştırabilirsin, ama gerçek karışıklık yapma».

Bu şablon neye karşı koruyor?

Aslında sınırı çok net çiziyor:

· .github/ dizinine dokunma: birinin otomatik birleştirme iş akışını havaya uçurmasını ya da CI'ya arka kapı koymasını engeller.
· README'nin korumalı kısımlarına dokunma: vitrin yine de gerekli, ana sayfayı tuhaf bir şeye çeviremezsin.
· Kimlik bilgisi, virüs, kişisel bilgi yok: tedarik zinciri saldırılarına, doxlamaya ve gerçek kötülüğe karşı.
· Nasıl izleneceğini açıkla: numara yapabilirsin, ama insanlar onu nasıl izleyeceğini bilmeli.
· «Başarılı breaking change» beyan et: kendini tiye alan bir sorumluluk reddi, yani «yaptım ama sorumlu değilim».

«Doğaüstü olay» listesine gelince:

üç harf + bir dairenin çevresinde üç ok + dış çizgili bir vakıf
pentagram zemininde bir dünya haritası + çevresinde bir tarım ürünleri halkası + beş kelimelik uluslararası bir ittifak

Birincisi SCP Vakfı; ikincisi muhtemelen FAO / Birleşmiş Milletler Gıda ve Tarım Örgütü gibi uluslararası bir kuruluş. Çevirisi şu:
«Bu artık bir kod sorunu değil; anomaliyi bir muhafaza kuruluşuna bildirmenizi öneririz.»

Commit'iniz bu şablona nasıl uyar?

Minecraft, OpenJDK ve Fabric Loader kaynak kodlarını yüklüyorsunuz, 4 commit'te 12,7 milyondan fazla satır kasıyorsunuz; türde şunları işaretleyebilirsiniz:

☑ belgelere tekme
☑ boş şehir kod arızası (Xu Jiayin cosplay'i)
☑ Platformlar arası Git
☐ kedi kaynaklı olay
☐ doğaüstü olay

Tüm doğrulamaları işaretliyorsunuz, beyanı kopyalıyorsunuz ve gerekçe olarak şunu yazıyorsunuz:

Gerekçe: uydurma, zorunlu değil, ama 12.770.942 satır kod bir unvanı hak ediyor.

Nasıl izlenir:

OpenJDK_25.0.3'ü açın, commit geçmişine bakın, sonra depo boyutunun sessizliğini hissedin.

Ama yine de bir uyarı gerekli

Bu tür bir depo oyun alanıdır, kanunsuz bölge değil. OpenJDK'nın tüm kaynak kodunu ya da Minecraft kaynak kodunu yüklemek belki sadece «çakışmasız otomatik birleştirme» ile sonuçlanır, ama şunları getirir:

· depo boyutu patlar ve GitHub kısıtlayabilir ya da uyarabilir;
· telif hakkı / lisans sorunları: her kaynak kodu öylece bir yere atılamaz;
· biri bu depoyu bağımlılık olarak kullanırsa, bu bir tedarik zinciri felaketidir.

Yani sonuç şu:
bu PR şablonu, bakımcının «açık yıkım» ile «gerçek patlamayı önlemek» arasında bulduğu denge noktası.
Oynamaya devam edebilirsiniz, ama en iyisi bunu performans sanatı olarak görün, kod deposu olarak değil. SCP Vakfı raporu çoktan aldı.
(Bu metin gerçekten çok fazla AI kokuyor — HQ123-BOOP değerlendirmesi)

# github dosya hızlandırma 
[https://githubcf.https114514191810lp.edu.eu.org/]

# Gerçek github dosya hızlandırma 
[https://gh-proxy.com/]

# Bilgin olsun
Microsoft Kod Savaşı'nın (VS Code) web sürümüne girmek için «.» tuşuna bas


## Yerinde altyapı arkeoloji arşivi

![EGIEM-R1, gerçek prototip: yerinde fotoğraf](./Emergency-Generator-Interface-Elevation-Module/assets/rock-field-photo.png)

Bu depo artık düşük güç tüketimli, yüksek güvenilirliğe sahip ve tamamen çevrimdışı bir yerinde altyapı parçası barındırıyor: kritik bir anda geçici olarak göreve çağrılmış bir taş. İşlemcisi yok, ağ kartı yok ve istifa etme niyeti de yok; sadece kendi ağırlığıyla arayüz kutusunu tam doğru konumda sapasağlam tutuyor.

Sarı etiket, «bir taş buldum»u «ekipman siciline girdi» seviyesine yükselten şey. Ön değerlendirmeden sonra bu cihaz ne giriş, ne güncelleme, ne de yeniden başlatma gerektiriyor; bilinen tek bakım işlemi: dokunma.

Üst akış bağımlılığı: operatörün jeneratör arayüz kutusu  
Alt akış bağımlılığı: Dünya  
Çalışma durumu: istikrarlı çalışıyor

Fotoğraf, katkıda bulunanın sağladığı yerinde çekilmiş orijinal görüntü; yalnızca dosya adı normalleştirildi, kırpılmadı ya da yeniden çizilmedi.

> **Çalışıyorsa taşı yerinden oynatma.**
