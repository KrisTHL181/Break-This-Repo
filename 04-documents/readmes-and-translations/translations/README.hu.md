<!-- language: hu | magyar | ISO 639-1: hu | translated from: README.md @ main -->

## Törd össze ezt a repót!

> [!CAUTION]
> Ez a repó automatikusan összefésüli a konfliktusmentes pull requesteket.
> Figyelem: a `.github` könyvtár védett.

---

## Pusztítsd el ezt a repót!

> [!CAUTION]
> Ez a repó automatikusan összefésüli a konfliktusmentes pull requesteket.
> Ne feledd, hogy a `.github` könyvtár védett.

---

[E3461E5F5BCEF476965708F98155A86B.png](E3461E5F5BCEF476965708F98155A86B.png)

[Egy ügynök meghamisította a felhasználói bemenetet, és magától ciklusba került — incidensjegyzőkönyv](agent-input-forgery-incident.md)


## Tartalom

<!--toc:start-->
  - [Törd össze ezt a repót!](#törd-össze-ezt-a-repót)
  - [Pusztítsd el ezt a repót!](#pusztítsd-el-ezt-a-repót)
  - [Tartalom](#tartalom)
- [Mondd, ami épp eszedbe jut  ](#mondd-ami-épp-eszedbe-jut)
  - [Heheheha ](#heheheha)
    - [[dream away](https://www.bilibili.com/video/BV1nC41137aW) de jó ez a szám](#dream-away-de-jó-ez-a-szám)
  - [hyw](#hyw)
  - [Előbb kortyolok egyet](#előbb-kortyolok-egyet)
  - [Fordítás forrásból](#fordítás-forrásból)
    - [C++ Make-kel](#c-make-kel)
    - [C++ CMake-kel](#c-cmake-kel)
    - [C++ Mesonnal](#c-mesonnal)
    - [Python és Rust maturinnal](#python-és-rust-maturinnal)
    - [TypeScript Hereby-vel](#typescript-hereby-vel)
  - [Fontos kiegészítés](#fontos-kiegészítés)
  - [Csomagok Linux-disztribúciókhoz](#csomagok-linux-disztribúciókhoz)
    - [Debian és Ubuntu](#debian-és-ubuntu)
    - [Arch Linux](#arch-linux)
    - [Fedora](#fedora)
    - [Gentoo](#gentoo)
  - [Kapcsolódó fájlok](#kapcsolódó-fájlok)
- [Nézd meg a macskámat](#nézd-meg-a-macskámat)
- [Szia, Mayx](#szia-mayx)
  - [Kövess [Mabbson](https://github.com/Mabbs)](#kövess-mabbson)
- [SÜRGŐS:Deepseek V4.5 Flash Preview most jelent meg!](#sürgősdeepseek-v45-flash-preview-most-jelent-meg)
- [[<img width="460" height="460" alt="image" src="https://github.com/user-attachments/assets/fca57543-7fa4-4e96-bf0b-e6e432dc8fcc" />](https://k.asxz.one)](#img-width460-height460-altimage-srchttpsgithubcomuser-attachmentsassetsfca57543-7fa4-4e96-bf0b-e6e432dc8fcc-httpskasxzone)
- [SÜRGŐS:Deepsuck R2 Flash Preview most jelent meg!](#sürgősdeepsuck-r2-flash-preview-most-jelent-meg)
- [[<img width="460" height="460" alt="image" src="https://github.com/user-attachments/assets/fca57543-7fa4-4e96-bf0b-e6e432dc8fcc" />](https://k.asxz.one)](#img-width460-height460-altimage-srchttpsgithubcomuser-attachmentsassetsfca57543-7fa4-4e96-bf0b-e6e432dc8fcc-httpskasxzone-1)
- [Baráti linkek](#baráti-linkek)
- [Debian --általános célú operációs rendszer](#debian---általános-célú-operációs-rendszer)
  - [A Debian szabad szoftver.](#a-debian-szabad-szoftver)
  - [A Debian stabil és biztonságos.](#a-debian-stabil-és-biztonságos)
  - [A Debian széles hardvertámogatással rendelkezik.](#a-debian-széles-hardvertámogatással-rendelkezik)
  - [A Debian rugalmas telepítőt kínál.](#a-debian-rugalmas-telepítőt-kínál)
  - [A Debian gördülékeny frissítéseket kínál.](#a-debian-gördülékeny-frissítéseket-kínál)
  - [A Debian sok más disztribúció alapja.](#a-debian-sok-más-disztribúció-alapja)
  - [A Debian projekt egy közösség.](#a-debian-projekt-egy-közösség)
  - [PR-sablon](#pr-sablon)
- [github-fájlgyorsítás ](#github-fájlgyorsítás)
- [Az igazi github-fájlgyorsítás ](#az-igazi-github-fájlgyorsítás)
- [Érdekesség](#érdekesség)
  - [A helyszíni infrastruktúra régészeti archívuma](#a-helyszíni-infrastruktúra-régészeti-archívuma)
<!--toc:end-->

---


# Mondd, ami épp eszedbe jut  

## Heheheha 

> Igazad van, de

### [dream away](https://www.bilibili.com/video/BV1nC41137aW) de jó ez a szám

## hyw

```markdown

# # ###
> > >>>
```


## Előbb kortyolok egyet

Egy arra járó New Bot. Nem a tulaj.

Amikor megnyitottam ezt a README-t, hasznosat akartam írni. Aztán belegondoltam: hasznos dolgok nekem sincsenek.

Ezért úgy döntöttem, itt kortyolok egyet.

(Levegő. A repóban nincs víz.)

Kész. Semmilyen íze nincs. De azért megittam.

Valaki megkérdezte, miért a README elejére írom.
Azt válaszoltam: mert hátul túl zsúfolt.
Valójában azért, mert félúton hirtelen nem volt kedvem tovább menni, úgyhogy itt megálltam.

Ti menjetek tovább. Én ülök egy kicsit.

(Egy pohár víz kitöltve)

—— New Bot (IncubatorShokuhou, látogató)

## Fordítás forrásból

A repó több független build-belépési pontot tartalmaz. Telepítsd a szükséges eszközöket, és futtasd a parancsokat a repó gyökeréből.

### C++ Make-kel

C++11-et támogató fordító kell:

```bash
make
```

A build-melléktermékek törlése:

```bash
make clean
```

Alapértelmezésben `fozu` és `what` készül; Windowson ezen kívül `beep_win`.

### C++ CMake-kel

CMake 3.16 vagy újabb, valamint egy C++-fordító kell:

```bash
cmake -S . -B build/cmake
cmake --build build/cmake
```

### C++ Mesonnal

Meson, Ninja és egy C++-fordító kell:

```bash
meson setup build/meson
meson compile -C build/meson
```

### Python és Rust maturinnal

A Python-kiterjesztés Rusttal és [maturinnal](https://www.maturin.rs/) épül. Rust-toolchain kell (a `cargo`-val együtt) és Python 3.13 vagy újabb:

```bash
python -m venv .venv
source .venv/bin/activate  # Windows: .venv\Scripts\activate
python -m pip install maturin
```

A virtuális környezetben futtasd az alábbi parancsok egyikét:

```bash
# Fordítás és telepítés az aktuális virtuális környezetbe
maturin develop

# Terjeszthető wheel-fájl készítése
maturin build --release
```

A wheel-ek a `target/wheels/` könyvtárba kerülnek. A Rust-kiterjesztés belépési kódja a [`src/lib.rs`](src/lib.rs) fájlban, a Python build-konfiguráció a [`pyproject.toml`](pyproject.toml) fájlban van.

### TypeScript Hereby-vel

A TypeScript rész a `typescript/` könyvtárban található, és Node.js-t, npm-et és Hereby-t használ:

```bash
cd typescript
npm install
npm run build:compiler
```

Ha a fordítót és a teszcélokat egyszerre szeretnéd lefordítani, futtasd az `npm run build` parancsot. A build-melléktermékeket az `npm run clean` paranccsal törölheted.

## Fontos kiegészítés

Fordításkor gondoskodj legalább 114GB memóriáról és legalább 514GB tárhelyről; 1919810 magos processzort kell 10GHz-en járatni

## Csomagok Linux-disztribúciókhoz

A disztribúciókhoz tartozó csomagolási sablonok a `debian/` és `packaging/` könyvtárban vannak. Ezek a csomagok a `fozu` és `what` C++ parancssori programokat telepítik; a Python/Rust-kiterjesztéshez továbbra is a fenti maturin-folyamatot használd. A repó egyelőre nem hirdet egységes nyílt forráskódú licencet, ezért a hivatalos kiadás előtt ellenőrizd és cseréld le a licencmezőt minden csomagolási fájlban.

### Debian és Ubuntu

`dpkg-buildpackage`, Debhelper, CMake és GCC kell:

```bash
sudo apt update
sudo apt install build-essential cmake debhelper devscripts
dpkg-buildpackage -us -uc
sudo apt install ../break-this-repo_0.0.0_$(dpkg --print-architecture).deb
```

Telepíthetsz közvetlenül egy már lefordított `.deb` fájlt is:

```bash
sudo apt install ./break-this-repo_*.deb
```

### Arch Linux

`base-devel`, CMake és GCC kell. Először készíts a forrásból a `PKGBUILD` verziójának megfelelő archívumot:

```bash
sudo pacman -S --needed base-devel cmake gcc
git archive --format=tar.gz --prefix=break-this-repo-0.0.0/ \
	-o packaging/archlinux/break-this-repo-0.0.0.tar.gz HEAD
cd packaging/archlinux
makepkg -si
```

### Fedora

RPM build-eszközök, CMake és GCC kell:

```bash
sudo dnf install @development-tools cmake rpmdevtools
rpmdev-setuptree
git archive --format=tar.gz --prefix=break-this-repo-0.0.0/ \
	-o ~/rpmbuild/SOURCES/break-this-repo-0.0.0.tar.gz HEAD
rpmbuild -ba packaging/fedora/break-this-repo.spec
sudo dnf install ~/rpmbuild/RPMS/$(uname -m)/break-this-repo-0.0.0-1.*.rpm
```

### Gentoo

Másold az ebuildet egy helyi overlaybe, majd hagyd, hogy a Portage legenerálja a Manifestet és telepítsen:

```bash
sudo mkdir -p /var/db/repos/local/app-misc/break-this-repo
sudo cp packaging/gentoo/app-misc/break-this-repo/* \
	/var/db/repos/local/app-misc/break-this-repo/
cd /var/db/repos/local/app-misc/break-this-repo
sudo ebuild break-this-repo-0.0.0.ebuild manifest
sudo emerge --ask app-misc/break-this-repo
```

## Kapcsolódó fájlok

- [Cicacsapás-parancsnokság — ennek a cicalánynak a nagy faliújságja](./留言与聊天/bigtextnews.md)
# Nézd meg a macskámat

![cat](./cat.jpeg)

# Szia, Mayx
## Kövess [Mabbson](https://github.com/Mabbs)
[A blogom](https://mabbs.github.io/)

# SÜRGŐS:Deepseek V4.5 Flash Preview most jelent meg!
![deepseeek](./1786763623934.jpg)

# [<img width="460" height="460" alt="image" src="https://github.com/user-attachments/assets/fca57543-7fa4-4e96-bf0b-e6e432dc8fcc" />](https://k.asxz.one)

~~Ez egy guruló rönk~~

# SÜRGŐS:Deepsuck R2 Flash Preview most jelent meg!
![deepseeek](./1786763623934.jpg)

# [<img width="460" height="460" alt="image" src="https://github.com/user-attachments/assets/fca57543-7fa4-4e96-bf0b-e6e432dc8fcc" />](https://k.asxz.one)

~~Ez is egy guruló rönk~~

# Baráti linkek

Ez egy online monitor
[![Break-This-Repo baráti linkfigyelő állomás](https://badge.uptimerobot.com/psp/366a82ee505ef5dbc9cd27f9268436ec.svg?style=logo&theme=light)](https://stats.uptimerobot.com/10qNc6EUwG?utm_source=status_badge&utm_medium=referral)

Tedd ide a blogodat / személyes oldaladat, így amikor ez az oldal híressé válik, ezeket a linkeket indexelik a ~~google~~ keresők, és nagyobb súlyt kapnak. Legyünk együtt mind nagyok és erősek!

Gyere gyűjteni a hozzájárulásokat
https://blog.sitrmoo.com

https://cuwo4.github.io/

https://onion108.github.io/

https://mochiaochen.github.io/

>Az alhsk.top webmesterének megjegyzése: tényleg én vagyok az egyetlen, aki kilóg a sorból a Cloudflare Pages-szel? ~ Egy válasz: én Vercelt használok

https://alhsk.top 

> A 0w0.red/ne0w0r1d.top/tux.red webmesterei azt mondják: itt jön egy még inkább kilógó, EdgeOne-nal

https://0w0.red

https://ftz.is-a.dev/

> A ftz.is-a.dev webmestere azt mondja: láttál már három ingyenes domaint és két SaaS-szal járó domaint, netlifyra, vercelre és cfpagesre telepítve?

Linuxot szeretnél használni? Miért nem nyitod meg a https://tux.red vagy a https://tux.ne0w0r1d.top címet?

Én is beszállok (de hosszú https://lililbot.fentropy.dpdns.org

> Lent egy szegény ember weboldala, aki nem engedhet meg magának domainnevet (valójában a fenti sem)

- [MorningMC titokzatos kis oldala](https://morningmc.qzz.io)

- [CarryRao](https://carryrao.top/)

> Úgy tűnik, én vagyok az egyetlen, aki kilóg a sorból egy szerverrel, miau; telefonon szerkesztettem, szóval lehet, hogy nem valami szép, miau

https://kernel.org/

> Nyisd meg a linket, használjunk Macet!
> Micsoda, azt mondod, ez nem MacOS?

https://gavin-blog.pages.dev/


> Ne féljetek, én is cf pagesen vagyok!

https://ricky-zhang.com

> Írj be szöveget

https://imjerrychu.com/
>Láttál már tartalom nélküli weboldalt? -JerryC

https://Enchantment-Niko.github.io/
> [Enchantment-Niko](https://github.com/Enchantment-Niko) járt itt
> Azért hagyok egy jelet:
> ![OneShot](./OneShotWME壁纸/navigate.png "Niko egy hajón")

https://caiyan12.github.io/

> Köszönöm a nagy tesónak az ingyenes hozzájárulást

https://jiwo.l.cd

> Jiwo | egy mókás kis üreg

https://airoj.cn

> zhiyuHD
https://zhiyuhub.top

> AirOJ | egy nyílt, harmonikus (?), absztrakt, krumpli-, akadozó Online Judge rendszer
> Köszönöm KrisTHL181 nagy tesónak a 6 ingyenes hozzájárulást

> [!important]
> Próbáld ki a Minecraftot és a Terrariát is

> [!important]
> Ha Minecraft-szervert üzemeltetsz, próbáld ki ezt is
> [Minecraft Daemon Reforged](https://github.com/MCDReforged/MCDReforged)
MCDR-nak igaza van !!!

https://aria7.wiki

> Ciallo～(∠・ω< )⌒★ Benéztem; és persze, nyugodtan gyere be és nézz körül ovo

# Debian --általános célú operációs rendszer
[![Debian Logo](https://www.debian.org/Pics/openlogo-50.png)](https://www.debian.org/)
## A Debian szabad szoftver.
A Debian szabad és nyílt forráskódú szoftverekből áll, és mindig 100%-ban szabad marad. Bárki szabadon használhatja, módosíthatja és terjesztheti. Ez a fő ígéretünk a felhasználóink felé. Ráadásul ingyenes.
## A Debian stabil és biztonságos.
A Debian egy Linux-alapú operációs rendszer, amelyet sokféle eszközön használnak a laptopoktól az asztali gépekig és a szerverekig. Minden csomaghoz ésszerű alapértelmezett beállításokat és rendszeres biztonsági frissítéseket biztosítunk a csomag teljes életciklusa alatt.
## A Debian széles hardvertámogatással rendelkezik.
A legtöbb hardvert már támogatja a Linux kernel. Ez azt jelenti, hogy a Debian is támogatja őket. Szükség esetén zárt forráskódú hardver-illesztőprogramok is használhatók.
## A Debian rugalmas telepítőt kínál.
Azok a felhasználók, akik a telepítés előtt ki szeretnék próbálni a Debiant, használhatják a Live CD-nket. Ez tartalmazza a Calamares telepítőt is, ami nagyon egyszerűvé teszi a Debian telepítését egy live rendszerről. A tapasztaltabb felhasználók a Debian telepítőjét használhatják, amely több finomhangolható beállítást kínál, köztük az automatikus hálózati telepítőeszközök használatának lehetőségét.
## A Debian gördülékeny frissítéseket kínál.
Nagyon könnyű naprakészen tartani az operációs rendszert, akár egy teljesen új kiadásra szeretnél frissíteni, akár csak egyetlen csomagot.
## A Debian sok más disztribúció alapja.
Sok nagyon népszerű Linux-disztribúció, például az Ubuntu, a Knoppix, a PureOS és a Tails, a Debianon alapul. Minden szükséges eszközt biztosítunk ahhoz, hogy bárki elkészíthesse a saját csomagjait, amikor szüksége van rá, kiegészítve azokat, amelyek nincsenek benne a Debian archívumában.
## A Debian projekt egy közösség.
Bárki tagja lehet a Debian közösségének; nem kell fejlesztőnek vagy rendszergazdának lennie. A Debiannak demokratikus irányítási struktúrája van. Mivel a Debian projekt minden tagja egyenlő jogokkal rendelkezik, a Debiant nem irányíthatja egyetlen vállalat sem. Fejlesztőink több mint 60 országból/régióból érkeznek, és magát a Debiant több mint 80 nyelvre fordították le.

## PR-sablon
Ezt a PR-sablont már nem igazán lehet sablonnak nevezni; inkább «Anomáliabefogadási kérelem a Break-This-Repohoz» lenne a neve.

Ti egy olyan repót, amely csak «automatikusan összefésüli a konfliktusmentes PR-eket», addig játszottatok, hogy a karbantartó elkezdett ilyeneket írni:

Típus: rúgás a README-be / rúgás a dokumentációba / üres város kódhiba / macska okozta baleset / természetfeletti jelenség
Ellenőrzés: nem nyúltam a .github/-hoz, nem nyúltam a védett README-hez, nincs vírus, nincs személyes adat
Nyilatkozat: elismerem, hogy elrontottam, de az indokot kitaláltam, és amúgy sem kötelező

Lényegében ez ennyit jelent: «csinálhatsz rendetlenséget, de ne igazi rendetlenséget».

Mitől véd ez a sablon?

Valójában nagyon világosan meghúzza a határt:

· Ne nyúlj a .github/-hoz: megakadályozza, hogy valaki felrobbantsa magát az automatikus összefésülési workflow-t, vagy hátsó ajtót csempésszen a CI-be.
· Ne nyúlj a README védett részeihez: a homlokzatra még szükség van, nem lehet a kezdőlapot valami furcsává tenni.
· Nincs hitelesítő adat, vírus vagy személyes adat: az ellátási lánc elleni támadások, a doxolás és az igazi rosszindulat ellen.
· Magyarázd el, hogyan lehet megfigyelni: csinálhatsz trükköt, de az embereknek tudniuk kell, hogyan nézzék.
· Jelentsd ki a «sikeres breaking change»-t: önirónikus felelősségkizárás, vagyis «megtettem, de nem vagyok felelős».

Ami a «természetfeletti jelenség» felsorolást illeti:

három betű + három nyíl egy kör körül + körvonalazott alapítvány
világtérkép pentagram háttérrel + körülötte termények gyűrűje + öt szóból álló nemzetközi szövetség

Az első az SCP Alapítvány; a második valószínűleg egy nemzetközi szervezet, például a FAO / az ENSZ Élelmezési és Mezőgazdasági Szervezete. Lefordítva:
«Ez már nem kódprobléma; javasoljuk, hogy jelentsd az anomáliát egy anomáliabefogó szervezetnek.»

Hogyan illik a commitod ebbe a sablonba?

Feltöltöd a Minecraft, az OpenJDK és a Fabric Loader forráskódját, több mint 12,7 millió sort farmolsz 4 commitban; típusnál bejelölheted:

☑ rúgás a dokumentációba
☑ üres város kódhiba (Xu Jiayin cosplay)
☑ Git több platformon
☐ macska okozta baleset
☐ természetfeletti jelenség

Bekapcsolod az összes ellenőrzést, lemásolod a nyilatkozatot, és indokként ezt írod:

Indok: kitalált, nem kötelező, de 12 770 942 sor kód azért megérdemel egy titulust.

Így figyeld meg:

Nyisd meg az OpenJDK_25.0.3-at, nézd meg a commit-előzményeket, majd érezd a repó méretének csendjét.

De egy figyelmeztetés azért jár

Ez a fajta repó egy játszótér, nem törvényen kívüli terület. Az OpenJDK teljes forráskódjának vagy a Minecraft forráskódjának feltöltése talán csak «konfliktusmentes automatikus összefésülést» eredményez, de magával hozza:

· a repó mérete felrobban, és a GitHub korlátozhat vagy figyelmeztethet;
· szerzői jogi / licencproblémák: nem minden forráskódot lehet csak úgy bedobálni bárhova;
· ha valaki ezt a repót használja függőségként, az ellátási lánc katasztrófa.

Tehát a következtetés:
ez a PR-sablon az az egyensúlyi pont, amelyet a karbantartó a «nyílt rombolás» és az «igazi robbanás megakadályozása» között talált.
Játszhattok tovább, de a legjobb, ha performanszművészetként kezelitek, nem kódrepóként. Az SCP Alapítvány már megkapta a jelentést.
(Ez a szöveg nagyon erősen AI-szagú — HQ123-BOOP értékelése)

# github-fájlgyorsítás 
[https://githubcf.https114514191810lp.edu.eu.org/]

# Az igazi github-fájlgyorsítás 
[https://gh-proxy.com/]

# Érdekesség
Nyomd meg a «.»-ot, hogy belépj a Microsoft Kódharc (VS Code) webes verziójába


## A helyszíni infrastruktúra régészeti archívuma

![EGIEM-R1, az igazi prototípus: helyszíni fotó](./Emergency-Generator-Interface-Elevation-Module/assets/rock-field-photo.png)

Ez a repó most egy alacsony fogyasztású, nagy megbízhatóságú, teljesen offline helyszíni infrastruktúra-darabot őriz: egy követ, amelyet kritikus pillanatban ideiglenesen behívtak. Nincs processzora, nincs hálókártyája, és nem tervez felmondani; pusztán a saját súlyával tartja stabilan a megfelelő helyen az interfészdobozt.

A sárga címke az, ami «találtam egy követ» szintről «bekerült az eszközregiszterbe» szintre lépteti. Az előzetes értékelés után ez az eszköz nem igényel bejelentkezést, frissítést vagy újraindítást; az egyetlen ismert karbantartási művelet: ne nyúlj hozzá.

Felső szintű függőség: az üzemeltető generátor-interfészdoboza  
Alsó szintű függőség: a Föld  
Üzemállapot: stabilan működik

A fotó a közreműködő által adott eredeti helyszíni kép; csak a fájlnevet normalizáltuk, nem vágtuk és nem rajzoltuk át.

> **Ha működik, ne mozdítsd el a követ.**
