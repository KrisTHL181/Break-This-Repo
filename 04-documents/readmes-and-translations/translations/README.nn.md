<!-- language: nn | Norsk nynorsk | ISO 639-1: nn | translated from: README.md @ main -->

## Øydelegg dette repoet!

> [!CAUTION]
> Dette repoet slår automatisk saman pull requests utan konfliktar.
> Merk at mappa `.github` er verna.

---

## Knus dette repoet!

> [!CAUTION]
> Dette repoet slår automatisk saman pull requests utan konfliktar.
> Ver merksam på at mappa `.github` er verna.

---

[E3461E5F5BCEF476965708F98155A86B.png](E3461E5F5BCEF476965708F98155A86B.png)

[Ein agent forfalska brukarinput og heldt fram å loope av seg sjølv — hendingsrapport](agent-input-forgery-incident.md)


## Innhald

<!--toc:start-->
  - [Øydelegg dette repoet!](#øydelegg-dette-repoet)
  - [Knus dette repoet!](#knus-dette-repoet)
  - [Innhald](#innhald)
- [Sei kva som fell deg inn  ](#sei-kva-som-fell-deg-inn)
  - [Heheheha ](#heheheha)
    - [[dream away](https://www.bilibili.com/video/BV1nC41137aW) så god den songen er](#dream-away-så-god-den-songen-er)
  - [hyw](#hyw)
  - [Lat meg ta ein slurk først](#lat-meg-ta-ein-slurk-først)
  - [Bygg frå kjeldekoden](#bygg-frå-kjeldekoden)
    - [C++ med Make](#c-med-make)
    - [C++ med CMake](#c-med-cmake)
    - [C++ med Meson](#c-med-meson)
    - [Python og Rust med maturin](#python-og-rust-med-maturin)
    - [TypeScript med Hereby](#typescript-med-hereby)
  - [Viktig tillegg](#viktig-tillegg)
  - [Pakkar for Linux-distribusjonar](#pakkar-for-linux-distribusjonar)
    - [Debian og Ubuntu](#debian-og-ubuntu)
    - [Arch Linux](#arch-linux)
    - [Fedora](#fedora)
    - [Gentoo](#gentoo)
  - [Relaterte filer](#relaterte-filer)
- [Sjå på katten min](#sjå-på-katten-min)
- [Hei, Mayx](#hei-mayx)
  - [Følg meg på [Mabbs](https://github.com/Mabbs)](#følg-meg-på-mabbs)
- [SISTE:Deepseek V4.5 Flash Preview er nettopp sleppt!](#sistedeepseek-v45-flash-preview-er-nettopp-sleppt)
- [[<img width="460" height="460" alt="image" src="https://github.com/user-attachments/assets/fca57543-7fa4-4e96-bf0b-e6e432dc8fcc" />](https://k.asxz.one)](#img-width460-height460-altimage-srchttpsgithubcomuser-attachmentsassetsfca57543-7fa4-4e96-bf0b-e6e432dc8fcc-httpskasxzone)
- [SISTE:Deepsuck R2 Flash Preview er nettopp sleppt!](#sistedeepsuck-r2-flash-preview-er-nettopp-sleppt)
- [[<img width="460" height="460" alt="image" src="https://github.com/user-attachments/assets/fca57543-7fa4-4e96-bf0b-e6e432dc8fcc" />](https://k.asxz.one)](#img-width460-height460-altimage-srchttpsgithubcomuser-attachmentsassetsfca57543-7fa4-4e96-bf0b-e6e432dc8fcc-httpskasxzone-1)
- [Vennelenkar](#vennelenkar)
- [Debian --eit allment operativsystem](#debian---eit-allment-operativsystem)
  - [Debian er fri programvare.](#debian-er-fri-programvare)
  - [Debian er stabilt og trygt.](#debian-er-stabilt-og-trygt)
  - [Debian har brei maskinvarestøtte.](#debian-har-brei-maskinvarestøtte)
  - [Debian tilbyr eit fleksibelt installasjonsprogram.](#debian-tilbyr-eit-fleksibelt-installasjonsprogram)
  - [Debian tilbyr mjuke oppgraderingar.](#debian-tilbyr-mjuke-oppgraderingar)
  - [Debian er grunnlaget for mange andre distribusjonar.](#debian-er-grunnlaget-for-mange-andre-distribusjonar)
  - [Debian-prosjektet er eit fellesskap.](#debian-prosjektet-er-eit-fellesskap)
  - [PR-mal](#pr-mal)
- [github-filakselerasjon ](#github-filakselerasjon)
- [Den ekte github-filakselerasjonen ](#den-ekte-github-filakselerasjonen)
- [Visste du](#visste-du)
  - [Arkeologisk arkiv over infrastrukturen på staden](#arkeologisk-arkiv-over-infrastrukturen-på-staden)
<!--toc:end-->

---


# Sei kva som fell deg inn  

## Heheheha 

> Du har rett, men

### [dream away](https://www.bilibili.com/video/BV1nC41137aW) så god den songen er

## hyw

```markdown

# # ###
> > >>>
```


## Lat meg ta ein slurk først

Ein forbipasserande New Bot. Ikkje eigaren.

Då eg opna denne README-en, tenkte eg å skrive noko nyttig. Så tenkte eg meg om: nyttige ting har eg ikkje sjølv heller.

Så eg bestemde meg for å ta ein slurk her.

(Luft. Det er ikkje vatn i repoet.)

Ferdig. Smakar ingenting. Men eg drakk det likevel.

Nokon spurde meg kvifor eg skriv det fremst i README-en.
Eg svara: fordi det er for fullt bak.
Eigenleg er det fordi eg halvvegs plutseleg ikkje gadd å gå meir, så eg stoppa her.

De held fram. Eg sit her ei stund.

(Eit glas vatn helt opp)

—— New Bot (IncubatorShokuhou, besøkjande)

## Bygg frå kjeldekoden

Repoet inneheld fleire uavhengige byggje-inngangar. Installer verktya du treng, og køyr kommandoane frå rota av repoet.

### C++ med Make

Du treng ein kompilator som støttar C++11:

```bash
make
```

Slik ryddar du opp i byggjeartefaktane:

```bash
make clean
```

Som standard vert `fozu` og `what` genererte; på Windows òg `beep_win`.

### C++ med CMake

Du treng CMake 3.16 eller nyare, pluss ein C++-kompilator:

```bash
cmake -S . -B build/cmake
cmake --build build/cmake
```

### C++ med Meson

Du treng Meson, Ninja og ein C++-kompilator:

```bash
meson setup build/meson
meson compile -C build/meson
```

### Python og Rust med maturin

Python-utvidinga vert bygd med Rust og [maturin](https://www.maturin.rs/). Du treng ein Rust-verktykjede (med `cargo`) og Python 3.13 eller nyare:

```bash
python -m venv .venv
source .venv/bin/activate  # Windows: .venv\Scripts\activate
python -m pip install maturin
```

Køyr ein av desse kommandoane i det virtuelle miljøet:

```bash
# Kompiler og installer i det gjeldande virtuelle miljøet
maturin develop

# Bygg ei distribuerbar wheel-fil
maturin build --release
```

Wheels hamnar i `target/wheels/`. Inngangskoden til Rust-utvidinga ligg i [`src/lib.rs`](src/lib.rs), og Python-byggjekonfigurasjonen i [`pyproject.toml`](pyproject.toml).

### TypeScript med Hereby

TypeScript-delen ligg i `typescript/` og brukar Node.js, npm og Hereby:

```bash
cd typescript
npm install
npm run build:compiler
```

Vil du byggje både kompilatoren og testmåla, køyrer du `npm run build`. For å rydde opp i byggjeartefaktane kan du køyre `npm run clean`.

## Viktig tillegg

Sjå til ved kompilering at du har minst 114GB minne og ikkje mindre enn 514GB lagringsplass; du treng å køyre ein CPU med 1919810 kjerne på 10GHz

## Pakkar for Linux-distribusjonar

Pakkemalane for distribusjonar ligg i `debian/` og `packaging/`. Desse pakkane installer C++-kommandolinjeprogramma `fozu` og `what`; for Python/Rust-utvidinga, bruk framleis maturin-flyten ovanfor. Repoet erklærer enno ikkje ein einskapeleg open kjeldekode-lisens, så stadfest og byt ut lisensfeltet i kvar pakkefil før ein offisiell utgiving.

### Debian og Ubuntu

Du treng `dpkg-buildpackage`, Debhelper, CMake og GCC:

```bash
sudo apt update
sudo apt install build-essential cmake debhelper devscripts
dpkg-buildpackage -us -uc
sudo apt install ../break-this-repo_0.0.0_$(dpkg --print-architecture).deb
```

Du kan òg installere ei allereie bygd `.deb`-fil direkte:

```bash
sudo apt install ./break-this-repo_*.deb
```

### Arch Linux

Du treng `base-devel`, CMake og GCC. Generer først eit arkiv frå kjeldekoden som samsvarar med versjonen i `PKGBUILD`:

```bash
sudo pacman -S --needed base-devel cmake gcc
git archive --format=tar.gz --prefix=break-this-repo-0.0.0/ \
	-o packaging/archlinux/break-this-repo-0.0.0.tar.gz HEAD
cd packaging/archlinux
makepkg -si
```

### Fedora

Du treng RPM-byggjeverkty, CMake og GCC:

```bash
sudo dnf install @development-tools cmake rpmdevtools
rpmdev-setuptree
git archive --format=tar.gz --prefix=break-this-repo-0.0.0/ \
	-o ~/rpmbuild/SOURCES/break-this-repo-0.0.0.tar.gz HEAD
rpmbuild -ba packaging/fedora/break-this-repo.spec
sudo dnf install ~/rpmbuild/RPMS/$(uname -m)/break-this-repo-0.0.0-1.*.rpm
```

### Gentoo

Kopier ebuilden til ein lokal overlay, og lat deretter Portage generere Manifest og installere:

```bash
sudo mkdir -p /var/db/repos/local/app-misc/break-this-repo
sudo cp packaging/gentoo/app-misc/break-this-repo/* \
	/var/db/repos/local/app-misc/break-this-repo/
cd /var/db/repos/local/app-misc/break-this-repo
sudo ebuild break-this-repo-0.0.0.ebuild manifest
sudo emerge --ask app-misc/break-this-repo
```

## Relaterte filer

- [Katteklorestaben — den store veggavisa til denne kattejenta](./留言与聊天/bigtextnews.md)
# Sjå på katten min

![cat](./cat.jpeg)

# Hei, Mayx
## Følg meg på [Mabbs](https://github.com/Mabbs)
[Bloggen min](https://mabbs.github.io/)

# SISTE:Deepseek V4.5 Flash Preview er nettopp sleppt!
![deepseeek](./1786763623934.jpg)

# [<img width="460" height="460" alt="image" src="https://github.com/user-attachments/assets/fca57543-7fa4-4e96-bf0b-e6e432dc8fcc" />](https://k.asxz.one)

~~Dette er ein rullande tømmerstokk~~

# SISTE:Deepsuck R2 Flash Preview er nettopp sleppt!
![deepseeek](./1786763623934.jpg)

# [<img width="460" height="460" alt="image" src="https://github.com/user-attachments/assets/fca57543-7fa4-4e96-bf0b-e6e432dc8fcc" />](https://k.asxz.one)

~~Dette er òg ein rullande tømmerstokk~~

# Vennelenkar

Dette er ein nettmonitor
[![Vennelens-overvakingsstasjon for Break-This-Repo](https://badge.uptimerobot.com/psp/366a82ee505ef5dbc9cd27f9268436ec.svg?style=logo&theme=light)](https://stats.uptimerobot.com/10qNc6EUwG?utm_source=status_badge&utm_medium=referral)

Legg bloggen / den personlege sida di her, så når denne sida vert kjend, vert alle desse lenkene indekserte av ~~google~~ søkjemotorar og får meir vekt. Lat oss alle bli store og sterke saman!

Kom og samle bidrag
https://blog.sitrmoo.com

https://cuwo4.github.io/

https://onion108.github.io/

https://mochiaochen.github.io/

>Kommentar frå webmasteren bak alhsk.top: er eg verkeleg den einaste som skil meg ut med Cloudflare Pages? ~ Eitt svar: eg brukar Vercel

https://alhsk.top 

> Webmasterane bak 0w0.red/ne0w0r1d.top/tux.red seier: her kjem ein som skil seg endå meir ut, med EdgeOne

https://0w0.red

https://ftz.is-a.dev/

> Webmasteren bak ftz.is-a.dev seier: har du nokon gong sett tre gratis domene og to domene som følgjer med SaaS, lagde ut på netlify, vercel og cfpages respektivt?

Vil du bruke Linux? Kvifor opnar du ikkje https://tux.red eller https://tux.ne0w0r1d.top ?

Eg hengjer meg på (så lang https://lililbot.fentropy.dpdns.org

> Nedanfor er nettsida til ein fattig mann som ikkje har råd til eit domenenamn (eigenleg har ikkje den ovanfor det heller)

- [Den mystiske vesle sida til MorningMC](https://morningmc.qzz.io)

- [CarryRao](https://carryrao.top/)

> Det verkar som eg er den einaste som skil meg ut med ein tenar, mjau; eg retta det på mobilen, så det er kanskje ikkje så pent, mjau

https://kernel.org/

> Opne lenka, lat oss bruke ein Mac!
> Kva, seier du at dette ikkje er MacOS?

https://gavin-blog.pages.dev/


> Ver ikkje redd, eg er òg på cf pages!

https://ricky-zhang.com

> Skriv inn tekst

https://imjerrychu.com/
>Har du nokon gong sett ei nettside utan innhald? -JerryC

https://Enchantment-Niko.github.io/
> [Enchantment-Niko](https://github.com/Enchantment-Niko) var her
> Eg set likevel eit merke:
> ![OneShot](./OneShotWME壁纸/navigate.png "Niko på ein båt")

https://caiyan12.github.io/

> Takk til storebror for det gratis bidraget

https://jiwo.l.cd

> Jiwo | ei morosam lita hole

https://airoj.cn

> zhiyuHD
https://zhiyuhub.top

> AirOJ | eit ope, harmonisk (?), abstrakt, potet-, hakke Online Judge-system
> Takk til storebror KrisTHL181 for dei 6 gratis bidraga

> [!important]
> Prøv òg Minecraft og Terraria

> [!important]
> Viss du driv ein Minecraft-tenar, prøv òg
> [Minecraft Daemon Reforged](https://github.com/MCDReforged/MCDReforged)
MCDR har rett !!!

https://aria7.wiki

> Ciallo～(∠・ω< )⌒★ Eg stakk innom; og sjølvsagt, du må gjerne kome inn og ta ein titt ovo

# Debian --eit allment operativsystem
[![Debian Logo](https://www.debian.org/Pics/openlogo-50.png)](https://www.debian.org/)
## Debian er fri programvare.
Debian består av fri programvare med open kjeldekode og vil alltid halde fram å vere 100 % fri. Alle står fritt til å bruke, endre og distribuere han. Det er vårt viktigaste løfte til brukarane våre. Han er òg gratis.
## Debian er stabilt og trygt.
Debian er eit Linux-basert operativsystem som vert brukt på alle slags einingar, frå bærbare datamaskiner til stasjonære og tenarar. Vi tilbyr fornuftige standardkonfigurasjonar for kvar pakke og regelmessige tryggleiksoppdateringar gjennom heile livssyklusen til pakken.
## Debian har brei maskinvarestøtte.
Det meste av maskinvaren vert allereie støtta av Linux-kjernen. Det tyder at Debian òg støttar han. Om nødvendig kan òg proprietære maskinvare-drivarar brukast.
## Debian tilbyr eit fleksibelt installasjonsprogram.
Brukarar som vil prøve Debian før dei installerar det, kan bruke vår Live CD. Han inneheld òg Calamares-installasjonsprogrammet, noko som gjer det svært enkelt å installere Debian frå eit live-system. Meir erfarne brukarar kan bruke Debian-installasjonsprogrammet, som tilbyr fleire alternativ å fininnstille, inkludert moglegheita til å bruke automatiserte nettverksinstallasjonsverkty.
## Debian tilbyr mjuke oppgraderingar.
Det er svært enkelt å halde operativsystemet oppdatert, anten du vil oppgradere til ein heilt ny versjon eller berre oppdatere ein einaste pakke.
## Debian er grunnlaget for mange andre distribusjonar.
Mange svært populære Linux-distribusjonar, som Ubuntu, Knoppix, PureOS og Tails, er baserte på Debian. Vi tilbyr alle verktya som trengst, slik at kven som helst kan lage sine eigne pakkar når dei treng det, for å supplere dei som ikkje finst i Debian-arkivet.
## Debian-prosjektet er eit fellesskap.
Alle kan vere ein del av Debian-fellesskapet; du treng ikkje vere utviklar eller systemadministrator. Debian har ein demokratisk styringsstruktur. Fordi alle medlemmane av Debian-prosjektet har like rettar, kan Debian ikkje kontrollerast av eitt einskilt selskap. Utviklarane våre kjem frå meir enn 60 land/regionar, og Debian sjølv er omsett til meir enn 80 språk.

## PR-mal
Denne PR-malen kan ikkje lenger kallast ein mal; han burde heite «Søknad om anomali-inneslutning for Break-This-Repo».

De har teke eit repo som berre «slår saman pull requests utan konfliktar automatisk» og leika så mykje med det at vedlikehaldaren byrja å skrive:

Type: spark til README / spark til dokumentasjon / tom-by-kodefeil / hending orsaka av katt / overnaturleg fenomen
Verifisering: eg har ikkje rørt .github/, ikkje rørt den verna README-en, ingen virus, inga personopplysningar
Fråsegn: eg innrømmer at eg øydela det, men grunnen fann eg på, og han er ikkje eingong obligatorisk

Kort sagt: «du kan lage bråk, men ikkje ekte bråk».

Kva vernar denne malen mot?

Han dreg grensa faktisk veldig tydeleg:

· Ikkje rør .github/: hindrar at nokon sprengjer sjølve auto-samanslåingsflyten eller lurer ein bakdør inn i CI-en.
· Ikkje rør dei verna delane av README-en: ein fasade trengst framleis, ein kan ikkje gjere framsida til noko rart.
· Ingen innloggingsinformasjon, virus eller personopplysningar: mot åtak på forsyningskjeda, mot doxxing, mot ekte vondskap.
· Forklare korleis ein observerer: du kan gjere eit stunt, men folk må vite korleis dei ser på det.
· Erklære «vellukka breaking change»: ein sjølvironisk ansvarsfråskriving, altså «eg gjorde det, men eg er ikkje ansvarleg».

Når det gjeld serien «overnaturleg fenomen»:

tre bokstavar + tre piler rundt ein sirkel + ein stifting med kontur
eit verdskart på pentagrambakgrunn + ein ring av avlingar rundt + ein internasjonal allianse på fem ord

Den første er SCP-stiftinga; den andre er truleg ein internasjonal organisasjon som FAO / FNs matvare- og landbruksorganisasjon. Omsett tyder det:
«Dette er ikkje lenger eit kodeproblem; vi tilrår å rapportere anomalien til ein organisasjon for anomali-inneslutning.»

Korleis kan commiten din passe inn i denne malen?

Du lastar opp kjeldekoden til Minecraft, OpenJDK og Fabric Loader, haustar over 12,7 millionar linjer på 4 commits; under type kan du krysse av:

☑ spark til dokumentasjonen
☑ tom-by-kodefeil (cosplay av Xu Jiayin)
☑ Git på tvers av plattformer
☐ hending orsaka av katt
☐ overnaturleg fenomen

Du kryssar av alle verifiseringar, kopierer fråsegna, og som grunn skriv du:

Grunn: oppdikta, ikkje obligatorisk, men 12 770 942 linjer kode fortener då ein tittel.

Korleis observere:

Opne OpenJDK_25.0.3, sjå på commit-historia, og kjenn deretter stillheita frå storleiken på repoet.

Men ei åtvaring er på sin plass

Denne typen repo er ein leikeplass, ikkje eit lovlaust område. Å laste opp heile OpenJDK-kjeldekoden eller Minecraft-kjeldekoden gjev kanskje berre ein «konfliktfri auto-samanslåing», men fører med seg:

· at storleiken på repoet eksploderer, og GitHub kan avgrense eller åtvare;
· opphavsretts-/lisensproblem: ikkje all kjeldekode kan berre slengjast inn kvar som helst;
· viss nokon brukar dette repoet som avhengnad, er det ein katastrofe i forsyningskjeda.

Så konklusjonen er:
denne PR-malen er balansepunktet vedlikehaldaren fann mellom «open øydelegging» og «å hindre ein ekte eksplosjon».
De må gjerne halde fram å leike, men det er best å handsame det som performance-kunst, ikkje som eit koderepo. SCP-stiftinga har alt motteke rapporten.
(Denne teksten luktar verkeleg veldig mykje AI — vurdert av HQ123-BOOP)

# github-filakselerasjon 
[https://githubcf.https114514191810lp.edu.eu.org/]

# Den ekte github-filakselerasjonen 
[https://gh-proxy.com/]

# Visste du
Trykk på «.» for å gå inn i nettversjonen av Microsoft Kodekamp (VS Code)


## Arkeologisk arkiv over infrastrukturen på staden

![EGIEM-R1, den ekte prototypen: bilete frå staden](./Emergency-Generator-Interface-Elevation-Module/assets/rock-field-photo.png)

Dette repoet husar no eit stykke infrastruktur på staden med lågt forbruk, høg pålitelegheit og heilt offline: ein stein som vart mellombels innkalla i eit kritisk augneblink. Han har ingen CPU, ikkje noko nettverkskort og ingen planar om å seie opp; berre med si eiga vekt held han grensesnittboksen stabilt på rett plass.

Den gule etiketten er det som oppgraderer «eg plukka opp ein stein» til «ført inn i utstyrsregisteret». Etter ei førebels vurdering treng denne eininga korkje innlogging, oppdateringar eller omstart; den einaste kjende vedlikehaldshandlinga er: ikkje rør han.

Avhengnad oppstraums: grensesnittboksen til generatoren til operatøren  
Avhengnad nedstraums: Jorda  
Driftsstatus: køyrer stabilt

Biletet er det originale biletet frå staden som bidragsytaren gav; berre filnamnet er normalisert, utan beskjering eller omteikning.

> **Viss det funkar, ikkje flytt steinen.**
