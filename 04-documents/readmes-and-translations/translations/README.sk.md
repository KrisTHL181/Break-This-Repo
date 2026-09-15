<!-- language: sk | slovenčina | ISO 639-1: sk | translated from: README.md @ main -->

## Rozbi toto úložisko!

> [!CAUTION]
> Toto úložisko automaticky zlučuje pull requesty bez konfliktov.
> Vezmi na vedomie, že adresár `.github` je chránený.

---

## Znič toto úložisko!

> [!CAUTION]
> Toto úložisko automaticky zlučuje pull requesty bez konfliktov.
> Pozor: adresár `.github` je chránený.

---

[E3461E5F5BCEF476965708F98155A86B.png](E3461E5F5BCEF476965708F98155A86B.png)

[Agent sfalšoval vstup používateľa a sám sa zacyklil — záznam o incidente](agent-input-forgery-incident.md)


## Obsah

<!--toc:start-->
  - [Rozbi toto úložisko!](#rozbi-toto-úložisko)
  - [Znič toto úložisko!](#znič-toto-úložisko)
  - [Obsah](#obsah)
- [Povedz, čo ti príde na um  ](#povedz-čo-ti-príde-na-um)
  - [Heheheha ](#heheheha)
    - [[dream away](https://www.bilibili.com/video/BV1nC41137aW) to je ale dobrá pesnička](#dream-away-to-je-ale-dobrá-pesnička)
  - [hyw](#hyw)
  - [Nechaj ma najprv hltať](#nechaj-ma-najprv-hltať)
  - [Zostavenie zo zdrojového kódu](#zostavenie-zo-zdrojového-kódu)
    - [C++ s Make](#c-s-make)
    - [C++ s CMake](#c-s-cmake)
    - [C++ s Meson](#c-s-meson)
    - [Python a Rust s maturin](#python-a-rust-s-maturin)
    - [TypeScript s Hereby](#typescript-s-hereby)
  - [Dôležitý dodatok](#dôležitý-dodatok)
  - [Balíky pre linuxové distribúcie](#balíky-pre-linuxové-distribúcie)
    - [Debian a Ubuntu](#debian-a-ubuntu)
    - [Arch Linux](#arch-linux)
    - [Fedora](#fedora)
    - [Gentoo](#gentoo)
  - [Súvisiace súbory](#súvisiace-súbory)
- [Pozri sa na moju mačku](#pozri-sa-na-moju-mačku)
- [Ahoj, Mayx](#ahoj-mayx)
  - [Sleduj ma na [Mabbs](https://github.com/Mabbs)](#sleduj-ma-na-mabbs)
- [MIMORIADNE:Deepseek V4.5 Flash Preview práve vyšiel!](#mimoriadnedeepseek-v45-flash-preview-práve-vyšiel)
- [[<img width="460" height="460" alt="image" src="https://github.com/user-attachments/assets/fca57543-7fa4-4e96-bf0b-e6e432dc8fcc" />](https://k.asxz.one)](#img-width460-height460-altimage-srchttpsgithubcomuser-attachmentsassetsfca57543-7fa4-4e96-bf0b-e6e432dc8fcc-httpskasxzone)
- [MIMORIADNE:Deepsuck R2 Flash Preview práve vyšiel!](#mimoriadnedeepsuck-r2-flash-preview-práve-vyšiel)
- [[<img width="460" height="460" alt="image" src="https://github.com/user-attachments/assets/fca57543-7fa4-4e96-bf0b-e6e432dc8fcc" />](https://k.asxz.one)](#img-width460-height460-altimage-srchttpsgithubcomuser-attachmentsassetsfca57543-7fa4-4e96-bf0b-e6e432dc8fcc-httpskasxzone-1)
- [Odkazy na priateľov](#odkazy-na-priateľov)
- [Debian --univerzálny operačný systém](#debian---univerzálny-operačný-systém)
  - [Debian je slobodný softvér.](#debian-je-slobodný-softvér)
  - [Debian je stabilný a bezpečný.](#debian-je-stabilný-a-bezpečný)
  - [Debian má širokú podporu hardvéru.](#debian-má-širokú-podporu-hardvéru)
  - [Debian ponúka flexibilný inštalátor.](#debian-ponúka-flexibilný-inštalátor)
  - [Debian ponúka plynulé aktualizácie.](#debian-ponúka-plynulé-aktualizácie)
  - [Debian je základom mnohých ďalších distribúcií.](#debian-je-základom-mnohých-ďalších-distribúcií)
  - [Projekt Debian je komunita.](#projekt-debian-je-komunita)
  - [Šablóna PR](#šablóna-pr)
- [zrýchlenie github súborov ](#zrýchlenie-github-súborov)
- [Skutočné zrýchlenie github súborov ](#skutočné-zrýchlenie-github-súborov)
- [Vedel si](#vedel-si)
  - [Archeologický archív infraštruktúry na mieste](#archeologický-archív-infraštruktúry-na-mieste)
<!--toc:end-->

---


# Povedz, čo ti príde na um  

## Heheheha 

> Máš pravdu, ale

### [dream away](https://www.bilibili.com/video/BV1nC41137aW) to je ale dobrá pesnička

## hyw

```markdown

# # ###
> > >>>
```


## Nechaj ma najprv hltať

Prechádzajúci New Bot. Nie majiteľ.

Keď som otvoril tento README, chcel som napísať niečo užitočné. Potom som si to rozmyslel: užitočné veci nemám ani ja.

Tak som sa rozhodol, že si tu hlcem.

(Vzduch. V úložisku nie je voda.)

Hotovo. Nemá to žiadnu chuť. Ale vypil som to aj tak.

Niekto sa ma spýtal, prečo to píšem dopredu do README.
Povedal som: lebo vzadu je príliš plno.
Vlastne preto, že v polovici ma prestalo baviť chodiť, tak som sa tu zastavil.

Vy choďte ďalej. Ja si tu sadnem.

(Naliaty pohár vody)

—— New Bot (IncubatorShokuhou, návštevník)

## Zostavenie zo zdrojového kódu

Úložisko obsahuje niekoľko nezávislých vstupných bodov zostavenia. Nainštaluj potrebné nástroje a spúšťaj príkazy z koreňa úložiska.

### C++ s Make

Potrebuješ kompilátor podporujúci C++11:

```bash
make
```

Na vyčistenie artefaktov zostavenia:

```bash
make clean
```

Predvolene sa vytvoria `fozu` a `what`; na Windows aj `beep_win`.

### C++ s CMake

Potrebuješ CMake 3.16 alebo novší a kompilátor C++:

```bash
cmake -S . -B build/cmake
cmake --build build/cmake
```

### C++ s Meson

Potrebuješ Meson, Ninja a kompilátor C++:

```bash
meson setup build/meson
meson compile -C build/meson
```

### Python a Rust s maturin

Rozšírenie pre Python sa zostavuje pomocou Rustu a [maturinu](https://www.maturin.rs/). Potrebuješ Rust toolchain (s `cargo`) a Python 3.13 alebo novší:

```bash
python -m venv .venv
source .venv/bin/activate  # Windows: .venv\Scripts\activate
python -m pip install maturin
```

Vo virtuálnom prostredí spusť jeden z týchto príkazov:

```bash
# Skompilovať a nainštalovať do aktuálneho virtuálneho prostredia
maturin develop

# Zostaviť distribuovateľný súbor wheel
maturin build --release
```

Wheely sa vytvárajú v `target/wheels/`. Vstupný kód rozšírenia Rust je v [`src/lib.rs`](src/lib.rs) a konfigurácia zostavenia Pythonu v [`pyproject.toml`](pyproject.toml).

### TypeScript s Hereby

Časť TypeScript je v `typescript/` a používa Node.js, npm a Hereby:

```bash
cd typescript
npm install
npm run build:compiler
```

Ak chceš zostaviť aj kompilátor, aj testovacie ciele, spusť `npm run build`. Na vyčistenie artefaktov zostavenia môžeš spustiť `npm run clean`.

## Dôležitý dodatok

Pri kompilácii si priprav aspoň 114GB pamäte a nie menej než 514GB úložného priestoru; potrebuješ procesor s 1919810 jadrami na 10GHz

## Balíky pre linuxové distribúcie

Šablóny balíkov pre distribúcie sú v `debian/` a `packaging/`. Tieto balíky inštalujú C++ programy pre príkazový riadok `fozu` a `what`; pre rozšírenie Python/Rust použi stále postup s maturinom vyššie. Úložisko zatiaľ nedeklaruje jednotnú open source licenciu, takže pred oficiálnym vydaním skontroluj a nahraď pole s licenciou v každom balíkovom súbore.

### Debian a Ubuntu

Potrebuješ `dpkg-buildpackage`, Debhelper, CMake a GCC:

```bash
sudo apt update
sudo apt install build-essential cmake debhelper devscripts
dpkg-buildpackage -us -uc
sudo apt install ../break-this-repo_0.0.0_$(dpkg --print-architecture).deb
```

Môžeš tiež priamo nainštalovať už zostavený súbor `.deb`:

```bash
sudo apt install ./break-this-repo_*.deb
```

### Arch Linux

Potrebuješ `base-devel`, CMake a GCC. Najprv vygeneruj zo zdrojového kódu archív zodpovedajúci verzii v `PKGBUILD`:

```bash
sudo pacman -S --needed base-devel cmake gcc
git archive --format=tar.gz --prefix=break-this-repo-0.0.0/ \
	-o packaging/archlinux/break-this-repo-0.0.0.tar.gz HEAD
cd packaging/archlinux
makepkg -si
```

### Fedora

Potrebuješ nástroje na zostavenie RPM, CMake a GCC:

```bash
sudo dnf install @development-tools cmake rpmdevtools
rpmdev-setuptree
git archive --format=tar.gz --prefix=break-this-repo-0.0.0/ \
	-o ~/rpmbuild/SOURCES/break-this-repo-0.0.0.tar.gz HEAD
rpmbuild -ba packaging/fedora/break-this-repo.spec
sudo dnf install ~/rpmbuild/RPMS/$(uname -m)/break-this-repo-0.0.0-1.*.rpm
```

### Gentoo

Skopíruj ebuild do lokálneho overlay a potom nechaj Portage vygenerovať Manifest a nainštalovať:

```bash
sudo mkdir -p /var/db/repos/local/app-misc/break-this-repo
sudo cp packaging/gentoo/app-misc/break-this-repo/* \
	/var/db/repos/local/app-misc/break-this-repo/
cd /var/db/repos/local/app-misc/break-this-repo
sudo ebuild break-this-repo-0.0.0.ebuild manifest
sudo emerge --ask app-misc/break-this-repo
```

## Súvisiace súbory

- [Mačacie drápové veliteľstvo — veľký plagát tejto mačacej baby](./留言与聊天/bigtextnews.md)
# Pozri sa na moju mačku

![cat](./cat.jpeg)

# Ahoj, Mayx
## Sleduj ma na [Mabbs](https://github.com/Mabbs)
[Môj blog](https://mabbs.github.io/)

# MIMORIADNE:Deepseek V4.5 Flash Preview práve vyšiel!
![deepseeek](./1786763623934.jpg)

# [<img width="460" height="460" alt="image" src="https://github.com/user-attachments/assets/fca57543-7fa4-4e96-bf0b-e6e432dc8fcc" />](https://k.asxz.one)

~~Toto je valiaci sa klát~~

# MIMORIADNE:Deepsuck R2 Flash Preview práve vyšiel!
![deepseeek](./1786763623934.jpg)

# [<img width="460" height="460" alt="image" src="https://github.com/user-attachments/assets/fca57543-7fa4-4e96-bf0b-e6e432dc8fcc" />](https://k.asxz.one)

~~Toto je tiež valiaci sa klát~~

# Odkazy na priateľov

Toto je online monitor
[![Monitorovacia stanica odkazov na priateľov Break-This-Repo](https://badge.uptimerobot.com/psp/366a82ee505ef5dbc9cd27f9268436ec.svg?style=logo&theme=light)](https://stats.uptimerobot.com/10qNc6EUwG?utm_source=status_badge&utm_medium=referral)

Daj sem svoj blog / osobnú stránku, aby keď bude tento web slávny, boli všetky tieto odkazy indexované ~~google~~ vyhľadávačmi a získali váhu. Poďme všetci spoločne byť veľkí a silní!

Poď nasbierať príspevok
https://blog.sitrmoo.com

https://cuwo4.github.io/

https://onion108.github.io/

https://mochiaochen.github.io/

>Poznámka správcu alhsk.top: som vážne jediný, kto vyčnieva s Cloudflare Pages? ~ Jedna odpoveď: ja používam Vercel

https://alhsk.top 

> Správcovia 0w0.red/ne0w0r1d.top/tux.red hovoria: tu prichádza niekto, kto vyčnieva ešte viac, s EdgeOne

https://0w0.red

https://ftz.is-a.dev/

> Správca ftz.is-a.dev hovorí: videl si niekedy tri domény zadarmo a dve domény pribalené k SaaS, nasadené postupne na netlify, vercel a cfpages?

Chceš používať Linux? Prečo si neotvoríš https://tux.red alebo https://tux.ne0w0r1d.top ?

Tiež sa pridám (aké dlhé https://lililbot.fentropy.dpdns.org

> Dole je web chudáka, ktorý si nemôže dovoliť doménové meno (vlastne ani ten hore)

- [Tajomný malý web MorningMC](https://morningmc.qzz.io)

- [CarryRao](https://carryrao.top/)

> Vyzerá to, že som jediný, kto vyčnieva so serverom, mňau; upravoval som to na mobile, takže to možno nie je moc uhladené, mňau

https://kernel.org/

> Otvor odkaz, použijeme Mac!
> Čože, hovoríš, že toto nie je MacOS?

https://gavin-blog.pages.dev/


> Nebojte sa, ja som tiež na cf pages!

https://ricky-zhang.com

> Zadaj text

https://imjerrychu.com/
>Videl si niekedy web bez obsahu? -JerryC

https://Enchantment-Niko.github.io/
> [Enchantment-Niko](https://github.com/Enchantment-Niko) tu bol
> Aj tak tu nechám značku:
> ![OneShot](./OneShotWME壁纸/navigate.png "Niko na lodi")

https://caiyan12.github.io/

> Vďaka veľkému bratovi za príspevok zadarmo

https://jiwo.l.cd

> Jiwo | vtipná malá nora

https://airoj.cn

> zhiyuHD
https://zhiyuhub.top

> AirOJ | otvorený, harmonický (?), abstraktný, zemiakový, škubajúci Online Judge systém
> Vďaka veľkému bratovi KrisTHL181 za 6 príspevkov zadarmo

> [!important]
> Vyskúšaj tiež Minecraft a Terrariu

> [!important]
> Ak prevádzkuješ Minecraft server, vyskúšaj tiež
> [Minecraft Daemon Reforged](https://github.com/MCDReforged/MCDReforged)
MCDR má pravdu !!!

https://aria7.wiki

> Ciallo～(∠・ω< )⌒★ Zastavil som sa tu; a samozrejme, pokojne sa príď pozrieť ovo

# Debian --univerzálny operačný systém
[![Debian Logo](https://www.debian.org/Pics/openlogo-50.png)](https://www.debian.org/)
## Debian je slobodný softvér.
Debian sa skladá zo slobodného softvéru s otvoreným zdrojovým kódom a vždy zostane 100% slobodný. Každý ho môže slobodne používať, upravovať a šíriť. Je to náš hlavný záväzok voči našim používateľom. Je tiež zadarmo.
## Debian je stabilný a bezpečný.
Debian je operačný systém založený na Linuxe, ktorý sa používa na najrôznejších zariadeniach od notebookov po stolné počítače a servery. Poskytujeme rozumné predvolené nastavenia pre každý balík a pravidelné bezpečnostné aktualizácie po celú dobu životnosti balíka.
## Debian má širokú podporu hardvéru.
Väčšinu hardvéru už podporuje jadro Linuxu. To znamená, že ho podporuje aj Debian. V prípade potreby možno použiť aj proprietárne ovládače hardvéru.
## Debian ponúka flexibilný inštalátor.
Používatelia, ktorí si chcú Debian vyskúšať pred inštaláciou, môžu použiť naše Live CD. Obsahuje tiež inštalátor Calamares, ktorý veľmi uľahčuje inštaláciu Debianu zo živého systému. Skúsenejší používatelia môžu použiť inštalátor Debianu, ktorý ponúka viac možností na doladenie, vrátane možnosti používať nástroje na automatickú inštaláciu po sieti.
## Debian ponúka plynulé aktualizácie.
Udržiavať operačný systém aktuálny je veľmi jednoduché, či už chceš prejsť na úplne novú verziu, alebo aktualizovať len jeden balík.
## Debian je základom mnohých ďalších distribúcií.
Mnoho veľmi populárnych linuxových distribúcií, ako sú Ubuntu, Knoppix, PureOS a Tails, je založených na Debiane. Poskytujeme všetky potrebné nástroje, aby si každý mohol vytvoriť vlastné balíky, keď ich potrebuje, a doplniť tie, ktoré v archíve Debianu nie sú.
## Projekt Debian je komunita.
Ktokoľvek sa môže stať súčasťou komunity Debianu; nemusíš byť vývojár ani správca systému. Debian má demokratickú štruktúru riadenia. Pretože všetci členovia projektu Debian majú rovnaké práva, nemôže Debian ovládať jediná spoločnosť. Naši vývojári pochádzajú z viac než 60 krajín/regiónov a Debian sám bol preložený do viac než 80 jazykov.

## Šablóna PR
Túto šablónu PR už nemožno naozaj nazývať šablónou; mala by sa volať «Žiadosť o zadržanie anomálie pre Break-This-Repo».

Vy ste vzali úložisko, ktoré len «automaticky zlučuje PR bez konfliktov», a hrali sa s ním tak dlho, až správca začal písať:

Typ: kopanec do README / kopanec do dokumentácie / porucha kódu prázdneho mesta / incident spôsobený mačkou / nadprirodzený jav
Overenie: nesiahol som na .github/, nesiahol som na chránený README, žiadne vírusy, žiadne osobné údaje
Vyhlásenie: priznávam, že som to rozbil, ale dôvod som si vymyslel, a ani nie je povinný

V podstate to znamená: «môžeš robiť neporiadok, ale nie skutočný neporiadok».

Pred čím táto šablóna chráni?

Vlastne kreslí hranicu veľmi jasne:

· Nesahať na .github/: bráni tomu, aby niekto vyhodil do povetria samotný workflow automatického zlučovania, alebo aby do CI nastrčil zadné dvierka.
· Nesahať na chránené časti README: fasáda je stále potrebná, nedá sa urobiť z domovskej stránky niečo divné.
· Žiadne prihlasovacie údaje, vírusy ani osobné údaje: proti útokom na dodávateľský reťazec, proti doxxingu, proti skutočnej zlomyseľnosti.
· Vysvetliť, ako to pozorovať: môžeš urobiť číslo, ale ľudia musia vedieť, ako sa na to pozerať.
· Vyhlásiť «úspešný breaking change»: sebaironické zrieknutie sa zodpovednosti, teda «urobil som to, ale nie som zodpovedný».

Čo sa týka série «nadprirodzený jav»:

tri písmená + tri šípky okolo kruhu + orámovaná nadácia
mapa sveta na pozadí pentagramu + okolo prstenec plodín + medzinárodná aliancia o piatich slovách

Prvá je Nadácia SCP; druhá je pravdepodobne medzinárodná organizácia ako FAO / Organizácia OSN pre výživu a poľnohospodárstvo. Preložené to znamená:
«To už nie je problém kódu; odporúčame nahlásiť anomáliu organizácii na zadržiavanie anomálií.»

Ako môže tvoj commit zapadnúť do tejto šablóny?

Nahráš zdrojový kód Minecraftu, OpenJDK a Fabric Loaderu, v 4 commitoch nazbieraš cez 12,7 milióna riadkov; ako typ môžeš zaškrtnúť:

☑ kopanec do dokumentácie
☑ porucha kódu prázdneho mesta (cosplay Xu Jiayina)
☑ Git naprieč platformami
☐ incident spôsobený mačkou
☐ nadprirodzený jav

Zaškrtneš všetky overenia, skopíruješ vyhlásenie a ako dôvod napíšeš:

Dôvod: vymyslený, nepovinný, ale 12 770 942 riadkov kódu si titul zaslúži.

Ako pozorovať:

Otvor OpenJDK_25.0.3, pozri sa na históriu commitov a potom pocítiš ticho z veľkosti úložiska.

Ale upozornenie je aj tak na mieste

Tento typ úložiska je ihrisko, nie bezprávne územie. Nahrať celý zdrojový kód OpenJDK alebo Minecraftu možno povedie len k «bezkonfliktnému automatickému zlúčeniu», ale prináša:

· explóziu veľkosti úložiska a GitHub môže obmedziť alebo varovať;
· problémy s autorskými právami / licenciou: nie všetok zdrojový kód sa dá len tak vyhodiť kamkoľvek;
· ak niekto použije toto úložisko ako závislosť, je to katastrofa dodávateľského reťazca.

Takže záver je:
táto šablóna PR je rovnovážny bod, ktorý správca našiel medzi «otvoreným ničením» a «zabránením skutočnému výbuchu».
Môžete sa ďalej hrať, ale najlepšie je brať to ako performatívne umenie, nie ako kódové úložisko. Nadácia SCP už správu dostala.
(Ten text fakt veľmi vonia AI — posúdil HQ123-BOOP)

# zrýchlenie github súborov 
[https://githubcf.https114514191810lp.edu.eu.org/]

# Skutočné zrýchlenie github súborov 
[https://gh-proxy.com/]

# Vedel si
Stlač «.» na vstup do webovej verzie Microsoft Bitky kódu (VS Code)


## Archeologický archív infraštruktúry na mieste

![EGIEM-R1, skutočný prototyp: fotka z miesta](./Emergency-Generator-Interface-Elevation-Module/assets/rock-field-photo.png)

Toto úložisko teraz hostí kus infraštruktúry na mieste s nízkou spotrebou, vysokou spoľahlivosťou a úplne offline: kameň, ktorý bol v kritickom okamihu dočasne povolaný. Nemá procesor, sieťovú kartu ani plán dať výpoveď; len svojou vlastnou váhou drží rozhranie pevne na správnom mieste.

Žltá etiketa je to, čo povýši «našiel som kameň» na «zapísané do registra zariadení». Po predbežnom posúdení toto zariadenie nepotrebuje prihlásenie, aktualizácie ani reštarty; jediná známa údržbová operácia je: nesiahaj naň.

Závislosť vyššie: rozhranie generátora operátora  
Závislosť nižšie: Zem  
Prevádzkový stav: beží stabilne

Fotka je pôvodná snímka z miesta, ktorú poskytol prispievateľ; normalizované bolo len meno súboru, bez orezania a prekresľovania.

> **Keď to funguje, nehýb tým kameňom.**
