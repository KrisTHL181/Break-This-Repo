<!-- language: sl | slovenščina | ISO 639-1: sl | translated from: README.md @ main -->

## Zlomi to skladišče!

> [!CAUTION]
> To skladišče samodejno združuje pull requeste brez konfliktov.
> Upoštevaj, da je mapa `.github` zaščitena.

---

## Uniči to skladišče!

> [!CAUTION]
> To skladišče samodejno združuje pull requeste brez konfliktov.
> Pozor: mapa `.github` je zaščitena.

---

[E3461E5F5BCEF476965708F98155A86B.png](E3461E5F5BCEF476965708F98155A86B.png)

[Agent je ponaredil uporabnikov vnos in sam ostal v zanki — poročilo o incidentu](agent-input-forgery-incident.md)


## Kazalo

<!--toc:start-->
  - [Zlomi to skladišče!](#zlomi-to-skladišče)
  - [Uniči to skladišče!](#uniči-to-skladišče)
  - [Kazalo](#kazalo)
- [Povej, kar ti pride na misel  ](#povej-kar-ti-pride-na-misel)
  - [Heheheha ](#heheheha)
    - [[dream away](https://www.bilibili.com/video/BV1nC41137aW) kako dobra pesem](#dream-away-kako-dobra-pesem)
  - [hyw](#hyw)
  - [Naj najprej popijem požirek](#naj-najprej-popijem-požirek)
  - [Gradnja iz izvorne kode](#gradnja-iz-izvorne-kode)
    - [C++ z Make](#c-z-make)
    - [C++ s CMake](#c-s-cmake)
    - [C++ z Meson](#c-z-meson)
    - [Python in Rust z maturin](#python-in-rust-z-maturin)
    - [TypeScript s Hereby](#typescript-s-hereby)
  - [Pomemben dodatek](#pomemben-dodatek)
  - [Paketi za distribucije Linux](#paketi-za-distribucije-linux)
    - [Debian in Ubuntu](#debian-in-ubuntu)
    - [Arch Linux](#arch-linux)
    - [Fedora](#fedora)
    - [Gentoo](#gentoo)
  - [Povezane datoteke](#povezane-datoteke)
- [Poglej mojo mačko](#poglej-mojo-mačko)
- [Živjo, Mayx](#živjo-mayx)
  - [Sledi mi na [Mabbs](https://github.com/Mabbs)](#sledi-mi-na-mabbs)
- [NUJNO:Deepseek V4.5 Flash Preview je pravkar izšel!](#nujnodeepseek-v45-flash-preview-je-pravkar-izšel)
- [[<img width="460" height="460" alt="image" src="https://github.com/user-attachments/assets/fca57543-7fa4-4e96-bf0b-e6e432dc8fcc" />](https://k.asxz.one)](#img-width460-height460-altimage-srchttpsgithubcomuser-attachmentsassetsfca57543-7fa4-4e96-bf0b-e6e432dc8fcc-httpskasxzone)
- [NUJNO:Deepsuck R2 Flash Preview je pravkar izšel!](#nujnodeepsuck-r2-flash-preview-je-pravkar-izšel)
- [[<img width="460" height="460" alt="image" src="https://github.com/user-attachments/assets/fca57543-7fa4-4e96-bf0b-e6e432dc8fcc" />](https://k.asxz.one)](#img-width460-height460-altimage-srchttpsgithubcomuser-attachmentsassetsfca57543-7fa4-4e96-bf0b-e6e432dc8fcc-httpskasxzone-1)
- [Povezave do prijateljev](#povezave-do-prijateljev)
- [Debian --splošni operacijski sistem](#debian---splošni-operacijski-sistem)
  - [Debian je prosto programje.](#debian-je-prosto-programje)
  - [Debian je stabilen in varen.](#debian-je-stabilen-in-varen)
  - [Debian ima široko podporo strojni opremi.](#debian-ima-široko-podporo-strojni-opremi)
  - [Debian ponuja prilagodljiv namestitveni program.](#debian-ponuja-prilagodljiv-namestitveni-program)
  - [Debian ponuja gladke posodobitve.](#debian-ponuja-gladke-posodobitve)
  - [Debian je osnova mnogih drugih distribucij.](#debian-je-osnova-mnogih-drugih-distribucij)
  - [Projekt Debian je skupnost.](#projekt-debian-je-skupnost)
  - [Predloga PR](#predloga-pr)
- [pospeševanje github datotek ](#pospeševanje-github-datotek)
- [Pravo pospeševanje github datotek ](#pravo-pospeševanje-github-datotek)
- [Ali si vedel](#ali-si-vedel)
  - [Arheološki arhiv infrastrukture na kraju samem](#arheološki-arhiv-infrastrukture-na-kraju-samem)
<!--toc:end-->

---


# Povej, kar ti pride na misel  

## Heheheha 

> Imaš prav, ampak

### [dream away](https://www.bilibili.com/video/BV1nC41137aW) kako dobra pesem

## hyw

```markdown

# # ###
> > >>>
```


## Naj najprej popijem požirek

Mimoidoči New Bot. Ne lastnik.

Ko sem odprl ta README, sem hotel napisati kaj koristnega. Potem sem pomislil: koristnih stvari tudi sam nimam.

Zato sem se odločil, da tu popijem požirek.

(Zrak. V skladišču ni vode.)

Konec. Nič ne tekne. Ampak vseeno sem popil.

Nekdo me je vprašal, zakaj to pišem spredaj v README.
Rekel sem: ker je zadaj preveč gneče.
Pravzaprav zato, ker me je na pol poti nenadoma minilo hoditi, zato sem se ustavil tu.

Vi kar pojdite naprej. Jaz bom malo sedel.

(Nalit kozarec vode)

—— New Bot (IncubatorShokuhou, obiskovalec)

## Gradnja iz izvorne kode

Skladišče vsebuje več neodvisnih vhodnih točk za gradnjo. Namesti potrebna orodja in zaženi ukaze iz korena skladišča.

### C++ z Make

Potrebuješ prevajalnik, ki podpira C++11:

```bash
make
```

Za čiščenje gradbenih artefaktov:

```bash
make clean
```

Privzeto se ustvarita `fozu` in `what`; v sistemu Windows tudi `beep_win`.

### C++ s CMake

Potrebuješ CMake 3.16 ali novejši in prevajalnik C++:

```bash
cmake -S . -B build/cmake
cmake --build build/cmake
```

### C++ z Meson

Potrebuješ Meson, Ninja in prevajalnik C++:

```bash
meson setup build/meson
meson compile -C build/meson
```

### Python in Rust z maturin

Razširitev za Python se zgradi z Rustom in [maturinom](https://www.maturin.rs/). Potrebuješ Rust toolchain (z `cargo`) in Python 3.13 ali novejši:

```bash
python -m venv .venv
source .venv/bin/activate  # Windows: .venv\Scripts\activate
python -m pip install maturin
```

V virtualnem okolju zaženi enega od teh ukazov:

```bash
# Prevedi in namesti v trenutno virtualno okolje
maturin develop

# Zgradi distribucijsko datoteko wheel
maturin build --release
```

Wheeli se ustvarijo v `target/wheels/`. Vhodna koda razširitve Rust je v [`src/lib.rs`](src/lib.rs), konfiguracija gradnje Pythona pa v [`pyproject.toml`](pyproject.toml).

### TypeScript s Hereby

Del TypeScript je v `typescript/` in uporablja Node.js, npm in Hereby:

```bash
cd typescript
npm install
npm run build:compiler
```

Če želiš zgraditi tako prevajalnik kot testne cilje, zaženi `npm run build`. Za čiščenje gradbenih artefaktov lahko zaženeš `npm run clean`.

## Pomemben dodatek

Pri prevajanju pripravi vsaj 114GB pomnilnika in ne manj kot 514GB prostora za shranjevanje; potrebuješ procesor s 1919810 jedri pri 10GHz

## Paketi za distribucije Linux

Predloge za pakiranje distribucij so v `debian/` in `packaging/`. Ti paketi namestijo C++ ukazna programa `fozu` in `what`; za razširitev Python/Rust še vedno uporabi zgoraj opisani postopek z maturinom. Skladišče še ne navaja enotne licence odprte kode, zato pred uradno izdajo preveri in zamenjaj polje z licenco v vsaki datoteki za pakiranje.

### Debian in Ubuntu

Potrebuješ `dpkg-buildpackage`, Debhelper, CMake in GCC:

```bash
sudo apt update
sudo apt install build-essential cmake debhelper devscripts
dpkg-buildpackage -us -uc
sudo apt install ../break-this-repo_0.0.0_$(dpkg --print-architecture).deb
```

Namestiš lahko tudi že zgrajeno datoteko `.deb` neposredno:

```bash
sudo apt install ./break-this-repo_*.deb
```

### Arch Linux

Potrebuješ `base-devel`, CMake in GCC. Najprej ustvari iz izvorne kode arhiv, ki ustreza različici v `PKGBUILD`:

```bash
sudo pacman -S --needed base-devel cmake gcc
git archive --format=tar.gz --prefix=break-this-repo-0.0.0/ \
	-o packaging/archlinux/break-this-repo-0.0.0.tar.gz HEAD
cd packaging/archlinux
makepkg -si
```

### Fedora

Potrebuješ orodja za gradnjo RPM, CMake in GCC:

```bash
sudo dnf install @development-tools cmake rpmdevtools
rpmdev-setuptree
git archive --format=tar.gz --prefix=break-this-repo-0.0.0/ \
	-o ~/rpmbuild/SOURCES/break-this-repo-0.0.0.tar.gz HEAD
rpmbuild -ba packaging/fedora/break-this-repo.spec
sudo dnf install ~/rpmbuild/RPMS/$(uname -m)/break-this-repo-0.0.0-1.*.rpm
```

### Gentoo

Kopiraj ebuild v lokalni overlay in nato pusti Portage, da ustvari Manifest in namesti:

```bash
sudo mkdir -p /var/db/repos/local/app-misc/break-this-repo
sudo cp packaging/gentoo/app-misc/break-this-repo/* \
	/var/db/repos/local/app-misc/break-this-repo/
cd /var/db/repos/local/app-misc/break-this-repo
sudo ebuild break-this-repo-0.0.0.ebuild manifest
sudo emerge --ask app-misc/break-this-repo
```

## Povezane datoteke

- [Poveljstvo mačjih krempljev — veliki plakat te mačje punce](./留言与聊天/bigtextnews.md)
# Poglej mojo mačko

![cat](./cat.jpeg)

# Živjo, Mayx
## Sledi mi na [Mabbs](https://github.com/Mabbs)
[Moj blog](https://mabbs.github.io/)

# NUJNO:Deepseek V4.5 Flash Preview je pravkar izšel!
![deepseeek](./1786763623934.jpg)

# [<img width="460" height="460" alt="image" src="https://github.com/user-attachments/assets/fca57543-7fa4-4e96-bf0b-e6e432dc8fcc" />](https://k.asxz.one)

~~To je kotaljen hlod~~

# NUJNO:Deepsuck R2 Flash Preview je pravkar izšel!
![deepseeek](./1786763623934.jpg)

# [<img width="460" height="460" alt="image" src="https://github.com/user-attachments/assets/fca57543-7fa4-4e96-bf0b-e6e432dc8fcc" />](https://k.asxz.one)

~~To je tudi kotaljen hlod~~

# Povezave do prijateljev

To je spletni monitor
[![Nadzorna postaja povezav do prijateljev Break-This-Repo](https://badge.uptimerobot.com/psp/366a82ee505ef5dbc9cd27f9268436ec.svg?style=logo&theme=light)](https://stats.uptimerobot.com/10qNc6EUwG?utm_source=status_badge&utm_medium=referral)

Sem daj svoj blog / osebno stran, da bodo, ko ta stran postane znana, vse te povezave indeksirane s strani ~~google~~ iskalnikov in pridobile težo. Bodimo vsi skupaj veliki in močni!

Pridi nabirat prispevek
https://blog.sitrmoo.com

https://cuwo4.github.io/

https://onion108.github.io/

https://mochiaochen.github.io/

>Opomba skrbnika alhsk.top: sem res edini, ki izstopa s Cloudflare Pages? ~ En odgovor: jaz uporabljam Vercel

https://alhsk.top 

> Skrbniki 0w0.red/ne0w0r1d.top/tux.red pravijo: tu prihaja nekdo, ki izstopa še bolj, z EdgeOne

https://0w0.red

https://ftz.is-a.dev/

> Skrbnik ftz.is-a.dev pravi: si že kdaj videl tri brezplačne domene in dve domeni, priloženi k SaaS, nameščene na netlify, vercel oziroma cfpages?

Želiš uporabljati Linux? Zakaj ne odpreš https://tux.red ali https://tux.ne0w0r1d.top ?

Tudi jaz se pridružim (kako dolgo https://lililbot.fentropy.dpdns.org

> Spodaj je stran revnega človeka, ki si ne more privoščiti domenskega imena (pravzaprav tudi tisti zgoraj ne)

- [Skrivnostna mala stran MorningMC](https://morningmc.qzz.io)

- [CarryRao](https://carryrao.top/)

> Zdi se, da sem edini, ki izstopa s strežnikom, mijav; urejal sem na telefonu, zato morda ni zelo urejeno, mijav

https://kernel.org/

> Odpri povezavo, uporabimo Mac!
> Kaj, praviš, da to ni MacOS?

https://gavin-blog.pages.dev/


> Ne bojte se, tudi jaz sem na cf pages!

https://ricky-zhang.com

> Vnesi besedilo

https://imjerrychu.com/
>Si že kdaj videl stran brez vsebine? -JerryC

https://Enchantment-Niko.github.io/
> [Enchantment-Niko](https://github.com/Enchantment-Niko) je bil tu
> Vseeno pustim oznako:
> ![OneShot](./OneShotWME壁纸/navigate.png "Niko na čolnu")

https://caiyan12.github.io/

> Hvala velikemu bratu za brezplačen prispevek

https://jiwo.l.cd

> Jiwo | smešna mala duplina

https://airoj.cn

> zhiyuHD
https://zhiyuhub.top

> AirOJ | odprt, harmoničen (?), abstrakten, krompirjev, zataknjen Online Judge sistem
> Hvala velikemu bratu KrisTHL181 za 6 brezplačnih prispevkov

> [!important]
> Preizkusi tudi Minecraft in Terrario

> [!important]
> Če upravljaš strežnik Minecraft, preizkusi tudi
> [Minecraft Daemon Reforged](https://github.com/MCDReforged/MCDReforged)
MCDR ima prav !!!

https://aria7.wiki

> Ciallo～(∠・ω< )⌒★ Oglasil sem se; in seveda, lahko prideš pogledat ovo

# Debian --splošni operacijski sistem
[![Debian Logo](https://www.debian.org/Pics/openlogo-50.png)](https://www.debian.org/)
## Debian je prosto programje.
Debian sestavlja prosto programje z odprto kodo in bo vedno ostal 100 % prost. Vsakdo ga lahko prosto uporablja, spreminja in razširja. To je naša glavna obljuba našim uporabnikom. Je tudi brezplačen.
## Debian je stabilen in varen.
Debian je operacijski sistem, ki temelji na Linuxu, in se uporablja na najrazličnejših napravah, od prenosnikov do namiznih računalnikov in strežnikov. Za vsak paket zagotavljamo smiselne privzete nastavitve in redne varnostne posodobitve skozi celoten življenjski cikel paketa.
## Debian ima široko podporo strojni opremi.
Večino strojne opreme že podpira jedro Linux. To pomeni, da jo podpira tudi Debian. Če je potrebno, lahko uporabiš tudi lastniške gonilnike strojne opreme.
## Debian ponuja prilagodljiv namestitveni program.
Uporabniki, ki želijo Debian preizkusiti pred namestitvijo, lahko uporabijo naš Live CD. Vsebuje tudi namestitveni program Calamares, zaradi česar je namestitev Debiana iz živega sistema zelo preprosta. Bolj izkušeni uporabniki lahko uporabijo namestitveni program Debiana, ki ponuja več možnosti za natančno nastavljanje, vključno z možnostjo uporabe orodij za samodejno omrežno namestitev.
## Debian ponuja gladke posodobitve.
Vzdrževanje operacijskega sistema posodobljenega je zelo preprosto, ne glede na to, ali želiš nadgraditi na popolnoma novo različico ali posodobiti samo en paket.
## Debian je osnova mnogih drugih distribucij.
Mnoge zelo priljubljene distribucije Linuxa, kot so Ubuntu, Knoppix, PureOS in Tails, temeljijo na Debianu. Zagotavljamo vsa potrebna orodja, da lahko vsakdo izdela svoje pakete, ko jih potrebuje, in tako dopolni tiste, ki jih v arhivu Debiana ni.
## Projekt Debian je skupnost.
Vsakdo je lahko del skupnosti Debian; ni ti treba biti razvijalec ali sistemski skrbnik. Debian ima demokratično strukturo upravljanja. Ker imajo vsi člani projekta Debian enake pravice, Debiana ne more nadzorovati eno samo podjetje. Naši razvijalci prihajajo iz več kot 60 držav/regij, Debian sam pa je preveden v več kot 80 jezikov.

## Predloga PR
Te predloge PR res ne moremo več imenovati predloga; morala bi se imenovati «Vloga za zadržanje anomalije za Break-This-Repo».

Vi ste vzeli skladišče, ki samo «samodejno združuje PR brez konfliktov», in se z njim igrali tako dolgo, da je vzdrževalec začel pisati:

Vrsta: brca v README / brca v dokumentacijo / okvara kode praznega mesta / incident, ki ga je povzročila mačka / nadnaravni pojav
Preverjanje: nisem se dotaknil .github/, nisem se dotaknil zaščitenega README, ni virusov, ni osebnih podatkov
Izjava: priznavam, da sem to zlomil, ampak razlog sem si izmislil, in sploh ni obvezen

V bistvu to pomeni: «lahko delaš nered, ampak ne resničnega nereda».

Pred čim ščiti ta predloga?

Pravzaprav potegne mejo zelo jasno:

· Ne dotikaj se .github/: preprečuje, da bi kdo raznesel sam potek samodejnega združevanja ali v CI vtaknil zadnja vrata.
· Ne dotikaj se zaščitenih delov README: fasada je še vedno potrebna, domače strani ne moreš spremeniti v nekaj čudnega.
· Brez poverilnic, virusov ali osebnih podatkov: proti napadom na dobavno verigo, proti doxxingu, proti resnični zlohotnosti.
· Pojasni, kako opazovati: lahko izvedeš točko, ampak ljudje morajo vedeti, kako jo gledati.
· Izjavi «uspešen breaking change»: samoironična zavrnitev odgovornosti, torej «naredil sem, ampak nisem odgovoren».

Kar zadeva serijo «nadnaravni pojav»:

tri črke + tri puščice okoli kroga + obrobljena fundacija
zemljevid sveta na ozadju pentagrama + okoli obroč pridelkov + mednarodna aliansa petih besed

Prva je fundacija SCP; druga je verjetno mednarodna organizacija, kot je FAO / Organizacija OZN za prehrano in kmetijstvo. Prevedeno to pomeni:
«To ni več težava s kodo; priporočamo, da anomalijo prijavite organizaciji za zadržanje anomalij.»

Kako lahko tvoj commit spada v to predlogo?

Naložiš izvorno kodo Minecrafta, OpenJDK in Fabric Loaderja, v 4 commitih nabereš več kot 12,7 milijona vrstic; pri vrsti lahko označiš:

☑ brca v dokumentacijo
☑ okvara kode praznega mesta (cosplay Xu Jiayina)
☑ Git na več platformah
☐ incident, ki ga je povzročila mačka
☐ nadnaravni pojav

Označiš vsa preverjanja, kopiraš izjavo in kot razlog napišeš:

Razlog: izmišljen, ni obvezen, ampak 12 770 942 vrstic kode si vseeno zasluži naslov.

Kako opazovati:

Odpri OpenJDK_25.0.3, poglej zgodovino commitov in nato začuti tišino velikosti skladišča.

Ampak opozorilo je vseeno na mestu

Ta vrsta skladišča je igrišče, ne območje brez zakona. Nalaganje celotne izvorne kode OpenJDK ali Minecrafta morda prinese le «samodejno združevanje brez konfliktov», a prinaša:

· eksplozijo velikosti skladišča in GitHub lahko omeji ali opozori;
· težave z avtorskimi pravicami / licenco: vsake izvorne kode ni mogoče kar vreči kamor koli;
· če kdo uporabi to skladišče kot odvisnost, je to katastrofa dobavne verige.

Torej sklep je:
ta predloga PR je ravnovesna točka, ki jo je vzdrževalec našel med «odprtim uničevanjem» in «preprečevanjem resnične eksplozije».
Lahko se še naprej igrate, ampak najbolje je, da to obravnavate kot performans, ne kot skladišče kode. Fundacija SCP je poročilo že prejela.
(To besedilo res zelo močno diši po AI — ocenil HQ123-BOOP)

# pospeševanje github datotek 
[https://githubcf.https114514191810lp.edu.eu.org/]

# Pravo pospeševanje github datotek 
[https://gh-proxy.com/]

# Ali si vedel
Pritisni «.» za vstop v spletno različico Microsoftove Bitke kode (VS Code)


## Arheološki arhiv infrastrukture na kraju samem

![EGIEM-R1, pravi prototip: fotografija s kraja](./Emergency-Generator-Interface-Elevation-Module/assets/rock-field-photo.png)

To skladišče zdaj gosti kos infrastrukture na kraju samem z nizko porabo, visoko zanesljivostjo in popolnoma brez povezave: kamen, ki je bil v kritičnem trenutku začasno vpoklican. Nima procesorja, mrežne kartice in nobenega namena dati odpovedi; samo s svojo težo drži vmesniško škatlo trdno na pravem mestu.

Rumena nalepka je tisto, kar «našel sem kamen» povzdigne v «vpisano v register naprav». Po predhodni oceni ta naprava ne potrebuje prijave, posodobitev ali ponovnih zagonov; edina znana vzdrževalna operacija je: ne dotikaj se ga.

Odvisnost gor: vmesniška škatla generatorja operaterja  
Odvisnost dol: Zemlja  
Stanje delovanja: deluje stabilno

Fotografija je izvirna slika s kraja, ki jo je prispeval sodelavec; normalizirano je bilo samo ime datoteke, brez obrezovanja in prerisovanja.

> **Če deluje, ne premakni kamna.**
