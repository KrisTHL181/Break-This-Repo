<!-- language: nb | Norsk bokmål | ISO 639-1: nb | translated from: README.md @ main -->

## Ødelegg dette repoet!

> [!CAUTION]
> Dette repoet slår automatisk sammen pull requests uten konflikter.
> Merk at mappen `.github` er beskyttet.

---

## Knus dette repoet!

> [!CAUTION]
> Dette repoet slår automatisk sammen pull requests uten konflikter.
> Vær oppmerksom på at mappen `.github` er beskyttet.

---

[E3461E5F5BCEF476965708F98155A86B.png](E3461E5F5BCEF476965708F98155A86B.png)

[En agent forfalsket brukerinput og fortsatte å loope av seg selv — hendelsesrapport](agent-input-forgery-incident.md)


## Innhold

<!--toc:start-->
  - [Ødelegg dette repoet!](#ødelegg-dette-repoet)
  - [Knus dette repoet!](#knus-dette-repoet)
  - [Innhold](#innhold)
- [Si hva som faller deg inn  ](#si-hva-som-faller-deg-inn)
  - [Heheheha ](#heheheha)
    - [[dream away](https://www.bilibili.com/video/BV1nC41137aW) så bra den sangen er altså](#dream-away-så-bra-den-sangen-er-altså)
  - [hyw](#hyw)
  - [La meg ta en slurk først](#la-meg-ta-en-slurk-først)
  - [Bygg fra kildekoden](#bygg-fra-kildekoden)
    - [C++ med Make](#c-med-make)
    - [C++ med CMake](#c-med-cmake)
    - [C++ med Meson](#c-med-meson)
    - [Python og Rust med maturin](#python-og-rust-med-maturin)
    - [TypeScript med Hereby](#typescript-med-hereby)
  - [Viktig tillegg](#viktig-tillegg)
  - [Pakker for Linux-distribusjoner](#pakker-for-linux-distribusjoner)
    - [Debian og Ubuntu](#debian-og-ubuntu)
    - [Arch Linux](#arch-linux)
    - [Fedora](#fedora)
    - [Gentoo](#gentoo)
  - [Relaterte filer](#relaterte-filer)
- [Se katten min](#se-katten-min)
- [Hei, Mayx](#hei-mayx)
  - [Følg meg på [Mabbs](https://github.com/Mabbs)](#følg-meg-på-mabbs)
- [SISTE:Deepseek V4.5 Flash Preview er nettopp sluppet!](#sistedeepseek-v45-flash-preview-er-nettopp-sluppet)
- [[<img width="460" height="460" alt="image" src="https://github.com/user-attachments/assets/fca57543-7fa4-4e96-bf0b-e6e432dc8fcc" />](https://k.asxz.one)](#img-width460-height460-altimage-srchttpsgithubcomuser-attachmentsassetsfca57543-7fa4-4e96-bf0b-e6e432dc8fcc-httpskasxzone)
- [SISTE:Deepsuck R2 Flash Preview er nettopp sluppet!](#sistedeepsuck-r2-flash-preview-er-nettopp-sluppet)
- [[<img width="460" height="460" alt="image" src="https://github.com/user-attachments/assets/fca57543-7fa4-4e96-bf0b-e6e432dc8fcc" />](https://k.asxz.one)](#img-width460-height460-altimage-srchttpsgithubcomuser-attachmentsassetsfca57543-7fa4-4e96-bf0b-e6e432dc8fcc-httpskasxzone-1)
- [Vennelenker](#vennelenker)
- [Debian --et generelt operativsystem](#debian---et-generelt-operativsystem)
  - [Debian er fri programvare.](#debian-er-fri-programvare)
  - [Debian er stabilt og sikkert.](#debian-er-stabilt-og-sikkert)
  - [Debian har bred maskinvarestøtte.](#debian-har-bred-maskinvarestøtte)
  - [Debian tilbyr et fleksibelt installasjonsprogram.](#debian-tilbyr-et-fleksibelt-installasjonsprogram)
  - [Debian tilbyr jevne oppgraderinger.](#debian-tilbyr-jevne-oppgraderinger)
  - [Debian er grunnlaget for mange andre distribusjoner.](#debian-er-grunnlaget-for-mange-andre-distribusjoner)
  - [Debian-prosjektet er et fellesskap.](#debian-prosjektet-er-et-fellesskap)
  - [PR-mal](#pr-mal)
- [github-filakselerasjon ](#github-filakselerasjon)
- [Den ekte github-filakselerasjonen ](#den-ekte-github-filakselerasjonen)
- [Kuriosa](#kuriosa)
  - [Arkeologisk arkiv over infrastrukturen på stedet](#arkeologisk-arkiv-over-infrastrukturen-på-stedet)
<!--toc:end-->

---


# Si hva som faller deg inn  

## Heheheha 

> Du har rett, men

### [dream away](https://www.bilibili.com/video/BV1nC41137aW) så bra den sangen er altså

## hyw

```markdown

# # ###
> > >>>
```


## La meg ta en slurk først

En forbipasserende New Bot. Ikke eieren.

Da jeg åpnet denne README-en, tenkte jeg å skrive noe nyttig. Så tenkte jeg meg om: nyttige ting har jeg ikke selv heller.

Så jeg bestemte meg for å ta en slurk her.

(Luft. Det er ikke vann i repoet.)

Ferdig. Smaker ingenting. Men jeg drakk det likevel.

Noen spurte meg hvorfor jeg skriver det forrest i README-en.
Jeg svarte: fordi det er for fullt bak.
Egentlig er det fordi jeg halvveis plutselig ikke gadd å gå mer, så jeg stoppet her.

Dere fortsetter. Jeg sitter her litt.

(Et glass vann helt opp)

—— New Bot (IncubatorShokuhou, besøkende)

## Bygg fra kildekoden

Repoet inneholder flere uavhengige bygge-innganger. Installer verktøyene du trenger, og kjør kommandoene fra roten av repoet.

### C++ med Make

Du trenger en kompilator som støtter C++11:

```bash
make
```

Slik rydder du opp i byggeartefaktene:

```bash
make clean
```

Som standard genereres `fozu` og `what`; på Windows også `beep_win`.

### C++ med CMake

Du trenger CMake 3.16 eller nyere, pluss en C++-kompilator:

```bash
cmake -S . -B build/cmake
cmake --build build/cmake
```

### C++ med Meson

Du trenger Meson, Ninja og en C++-kompilator:

```bash
meson setup build/meson
meson compile -C build/meson
```

### Python og Rust med maturin

Python-utvidelsen bygges med Rust og [maturin](https://www.maturin.rs/). Du trenger en Rust-toolchain (med `cargo`) og Python 3.13 eller nyere:

```bash
python -m venv .venv
source .venv/bin/activate  # Windows: .venv\Scripts\activate
python -m pip install maturin
```

Kjør én av disse kommandoene i det virtuelle miljøet:

```bash
# Kompiler og installer i det gjeldende virtuelle miljøet
maturin develop

# Bygg en distribuerbar wheel-fil
maturin build --release
```

Wheels havner i `target/wheels/`. Inngangskoden til Rust-utvidelsen ligger i [`src/lib.rs`](src/lib.rs), og Python-byggekonfigurasjonen i [`pyproject.toml`](pyproject.toml).

### TypeScript med Hereby

TypeScript-delen ligger i `typescript/` og bruker Node.js, npm og Hereby:

```bash
cd typescript
npm install
npm run build:compiler
```

Vil du bygge både kompilatoren og testmålene, kjører du `npm run build`. For å rydde opp i byggeartefaktene kan du kjøre `npm run clean`.

## Viktig tillegg

Ved kompilering må du ha minst 114GB minne og ikke mindre enn 514GB lagringsplass; du trenger å kjøre en CPU med 1919810 kjerner på 10GHz

## Pakker for Linux-distribusjoner

Pakkemalene for distribusjoner ligger i `debian/` og `packaging/`. Disse pakkene installerer C++-kommandolinjeprogrammene `fozu` og `what`; bruk fortsatt maturin-flyten ovenfor for Python/Rust-utvidelsen. Repoet erklærer ennå ikke en enhetlig åpen kildekode-lisens, så bekreft og erstatt lisensfeltet i hver pakkefil før en offisiell utgivelse.

### Debian og Ubuntu

Du trenger `dpkg-buildpackage`, Debhelper, CMake og GCC:

```bash
sudo apt update
sudo apt install build-essential cmake debhelper devscripts
dpkg-buildpackage -us -uc
sudo apt install ../break-this-repo_0.0.0_$(dpkg --print-architecture).deb
```

Du kan også installere en allerede bygget `.deb`-fil direkte:

```bash
sudo apt install ./break-this-repo_*.deb
```

### Arch Linux

Du trenger `base-devel`, CMake og GCC. Generer først et arkiv fra kildekoden som samsvarer med versjonen i `PKGBUILD`:

```bash
sudo pacman -S --needed base-devel cmake gcc
git archive --format=tar.gz --prefix=break-this-repo-0.0.0/ \
	-o packaging/archlinux/break-this-repo-0.0.0.tar.gz HEAD
cd packaging/archlinux
makepkg -si
```

### Fedora

Du trenger RPM-byggeverktøy, CMake og GCC:

```bash
sudo dnf install @development-tools cmake rpmdevtools
rpmdev-setuptree
git archive --format=tar.gz --prefix=break-this-repo-0.0.0/ \
	-o ~/rpmbuild/SOURCES/break-this-repo-0.0.0.tar.gz HEAD
rpmbuild -ba packaging/fedora/break-this-repo.spec
sudo dnf install ~/rpmbuild/RPMS/$(uname -m)/break-this-repo-0.0.0-1.*.rpm
```

### Gentoo

Kopier ebuilden til en lokal overlay, og la deretter Portage generere Manifest og installere:

```bash
sudo mkdir -p /var/db/repos/local/app-misc/break-this-repo
sudo cp packaging/gentoo/app-misc/break-this-repo/* \
	/var/db/repos/local/app-misc/break-this-repo/
cd /var/db/repos/local/app-misc/break-this-repo
sudo ebuild break-this-repo-0.0.0.ebuild manifest
sudo emerge --ask app-misc/break-this-repo
```

## Relaterte filer

- [Katteklorestaben — denne kattejenta sin store veggavis](./留言与聊天/bigtextnews.md)
# Se katten min

![cat](./cat.jpeg)

# Hei, Mayx
## Følg meg på [Mabbs](https://github.com/Mabbs)
[Bloggen min](https://mabbs.github.io/)

# SISTE:Deepseek V4.5 Flash Preview er nettopp sluppet!
![deepseeek](./1786763623934.jpg)

# [<img width="460" height="460" alt="image" src="https://github.com/user-attachments/assets/fca57543-7fa4-4e96-bf0b-e6e432dc8fcc" />](https://k.asxz.one)

~~Dette er en rullende tømmerstokk~~

# SISTE:Deepsuck R2 Flash Preview er nettopp sluppet!
![deepseeek](./1786763623934.jpg)

# [<img width="460" height="460" alt="image" src="https://github.com/user-attachments/assets/fca57543-7fa4-4e96-bf0b-e6e432dc8fcc" />](https://k.asxz.one)

~~Dette er også en rullende tømmerstokk~~

# Vennelenker

Dette er en nettvakt
[![Vennelens-overvåkingsstasjon for Break-This-Repo](https://badge.uptimerobot.com/psp/366a82ee505ef5dbc9cd27f9268436ec.svg?style=logo&theme=light)](https://stats.uptimerobot.com/10qNc6EUwG?utm_source=status_badge&utm_medium=referral)

Legg bloggen / den personlige siden din her, så når denne siden blir kjent, blir alle disse lenkene indeksert av ~~google~~ søkemotorer og får mer vekt. La oss alle bli store og sterke sammen!

Kom og samle bidrag
https://blog.sitrmoo.com

https://cuwo4.github.io/

https://onion108.github.io/

https://mochiaochen.github.io/

>Kommentar fra webmasteren bak alhsk.top: er jeg virkelig den eneste som skiller meg ut med Cloudflare Pages? ~ Ett svar: jeg bruker Vercel

https://alhsk.top 

> Webmasterne bak 0w0.red/ne0w0r1d.top/tux.red sier: her kommer en som skiller seg enda mer ut, med EdgeOne

https://0w0.red

https://ftz.is-a.dev/

> Webmasteren bak ftz.is-a.dev sier: har du noen gang sett tre gratis domener og to domener som følger med SaaS, lagt ut på henholdsvis netlify, vercel og cfpages?

Vil du bruke Linux? Hvorfor ikke åpne https://tux.red eller https://tux.ne0w0r1d.top ?

Jeg henger meg på (så lang https://lililbot.fentropy.dpdns.org

> Nedenfor er nettsiden til en fattig mann som ikke har råd til et domenenavn (egentlig har den ovenfor det heller ikke)

- [MorningMCs mystiske lille nettsted](https://morningmc.qzz.io)

- [CarryRao](https://carryrao.top/)

> Virker som jeg er den eneste som skiller meg ut med en server, mjau; jeg fikset det på mobilen, så det er kanskje ikke så pent, mjau

https://kernel.org/

> Åpne lenken, la oss bruke en Mac!
> Hva, sier du at dette ikke er MacOS?

https://gavin-blog.pages.dev/


> Vær ikke redd, jeg er også på cf pages!

https://ricky-zhang.com

> Skriv inn tekst

https://imjerrychu.com/
>Har du noen gang sett et nettsted uten innhold? -JerryC

https://Enchantment-Niko.github.io/
> [Enchantment-Niko](https://github.com/Enchantment-Niko) var her
> Jeg setter likevel et merke:
> ![OneShot](./OneShotWME壁纸/navigate.png "Niko på en båt")

https://caiyan12.github.io/

> Takk til storebror for det gratis bidraget

https://jiwo.l.cd

> Jiwo | et morsomt lite hi

https://airoj.cn

> zhiyuHD
https://zhiyuhub.top

> AirOJ | et åpent, harmonisk (?), abstrakt, potet-, hakke Online Judge-system
> Takk til storebror KrisTHL181 for de 6 gratis bidragene

> [!important]
> Prøv også Minecraft og Terraria

> [!important]
> Hvis du driver en Minecraft-server, prøv også
> [Minecraft Daemon Reforged](https://github.com/MCDReforged/MCDReforged)
MCDR har rett !!!

https://aria7.wiki

> Ciallo～(∠・ω< )⌒★ Jeg stakk innom; og selvfølgelig, du må gjerne komme inn og ta en titt ovo

# Debian --et generelt operativsystem
[![Debian Logo](https://www.debian.org/Pics/openlogo-50.png)](https://www.debian.org/)
## Debian er fri programvare.
Debian består av fri programvare med åpen kildekode og vil alltid forbli 100 % fri. Alle står fritt til å bruke, endre og distribuere den. Det er vårt viktigste løfte til brukerne våre. Den er også gratis.
## Debian er stabilt og sikkert.
Debian er et Linux-basert operativsystem som brukes på alle slags enheter, fra bærbare datamaskiner til stasjonære og servere. Vi tilbyr fornuftige standardkonfigurasjoner for hver pakke og regelmessige sikkerhetsoppdateringer gjennom hele pakkens livssyklus.
## Debian har bred maskinvarestøtte.
Det meste av maskinvaren støttes allerede av Linux-kjernen. Det betyr at Debian også støtter den. Ved behov kan også proprietære maskinvare-drivere brukes.
## Debian tilbyr et fleksibelt installasjonsprogram.
Brukere som vil prøve Debian før de installerer det, kan bruke vår Live CD. Den inneholder også Calamares-installasjonsprogrammet, noe som gjør det svært enkelt å installere Debian fra et live-system. Mer erfarne brukere kan bruke Debian-installasjonsprogrammet, som gir flere alternativer å finjustere, inkludert muligheten til å bruke automatiserte nettverksinstallasjonsverktøy.
## Debian tilbyr jevne oppgraderinger.
Det er svært enkelt å holde operativsystemet oppdatert, enten du vil oppgradere til en helt ny versjon eller bare oppdatere én enkelt pakke.
## Debian er grunnlaget for mange andre distribusjoner.
Mange svært populære Linux-distribusjoner, som Ubuntu, Knoppix, PureOS og Tails, er basert på Debian. Vi tilbyr alle verktøyene som trengs, slik at enhver kan lage sine egne pakker når de trenger det, for å supplere dem som ikke finnes i Debian-arkivet.
## Debian-prosjektet er et fellesskap.
Alle kan være en del av Debian-fellesskapet; du trenger ikke være utvikler eller systemadministrator. Debian har en demokratisk styringsstruktur. Fordi alle medlemmer av Debian-prosjektet har like rettigheter, kan Debian ikke kontrolleres av ett enkelt selskap. Utviklerne våre kommer fra mer enn 60 land/regioner, og Debian selv er oversatt til mer enn 80 språk.

## PR-mal
Denne PR-malen kan ikke lenger kalles en mal; den burde hete «Søknad om anomali-inneslutning for Break-This-Repo».

Dere har tatt et repo som bare «slår sammen pull requests uten konflikter automatisk» og lekt så mye med det at vedlikeholderen begynte å skrive:

Type: spark til README / spark til dokumentasjon / tom-by-kodefeil / hendelse forårsaket av katt / overnaturlig fenomen
Verifisering: jeg har ikke rørt .github/, ikke rørt den beskyttede README-en, ingen virus, ingen personopplysninger
Erklæring: jeg innrømmer at jeg ødela det, men grunnen fant jeg på, og den er ikke engang obligatorisk

Kort sagt: «du kan lage bråk, men ikke ekte bråk».

Hva beskytter denne malen mot?

Den trekker faktisk grensen veldig tydelig:

· Ikke rør .github/: hindrer at noen sprenger selve auto-sammenslåingsflyten eller lurer en bakdør inn i CI-en.
· Ikke rør de beskyttede delene av README-en: en fasade trengs fortsatt, man kan ikke gjøre forsiden til noe rart.
· Ingen påloggingsinformasjon, virus eller personopplysninger: mot angrep på forsyningskjeden, mot doxing, mot ekte ondskap.
· Forklare hvordan man observerer: du kan gjøre et stunt, men folk må vite hvordan de ser på det.
· Erklære «vellykket breaking change»: en selvironisk ansvarsfraskrivelse, altså «jeg gjorde det, men jeg er ikke ansvarlig».

Når det gjelder serien «overnaturlig fenomen»:

tre bokstaver + tre piler rundt en sirkel + en stiftelse med kontur
et verdenskart på pentagrambakgrunn + en ring av avlinger rundt + en internasjonal allianse på fem ord

Den første er SCP-stiftelsen; den andre er sannsynligvis en internasjonal organisasjon som FAO / FNs matvare- og landbruksorganisasjon. Oversatt betyr det:
«Dette er ikke lenger et kodeproblem; vi anbefaler å rapportere anomalien til en organisasjon for anomali-inneslutning.»

Hvordan kan commiten din passe inn i denne malen?

Du laster opp kildekoden til Minecraft, OpenJDK og Fabric Loader, høster over 12,7 millioner linjer på 4 commits; under type kan du krysse av:

☑ spark til dokumentasjonen
☑ tom-by-kodefeil (cosplay av Xu Jiayin)
☑ Git på tvers av plattformer
☐ hendelse forårsaket av katt
☐ overnaturlig fenomen

Du krysser av alle verifiseringer, kopierer erklæringen, og som grunn skriver du:

Grunn: oppdiktet, ikke obligatorisk, men 12 770 942 linjer kode fortjener da en tittel.

Slik observerer du:

Åpne OpenJDK_25.0.3, se commit-historikken, og kjenn deretter stillheten fra størrelsen på repoet.

Men en advarsel er på sin plass

Denne typen repo er en lekeplass, ikke et lovløst område. Å laste opp hele OpenJDK-kildekoden eller Minecraft-kildekoden gir kanskje bare en «konfliktfri auto-sammenslåing», men fører med seg:

· at størrelsen på repoet eksploderer, og GitHub kan begrense eller advare;
· opphavsretts-/lisensproblemer: ikke all kildekode kan bare slenges inn hvor som helst;
· hvis noen bruker dette repoet som avhengighet, er det en katastrofe i forsyningskjeden.

Så konklusjonen er:
denne PR-malen er balansepunktet vedlikeholderen fant mellom «åpen ødeleggelse» og «å hindre en ekte eksplosjon».
Dere må gjerne fortsette å leke, men det er best å behandle det som performancekunst, ikke som et koderepo. SCP-stiftelsen har allerede mottatt rapporten.
(Denne teksten lukter virkelig veldig mye AI — vurdert av HQ123-BOOP)

# github-filakselerasjon 
[https://githubcf.https114514191810lp.edu.eu.org/]

# Den ekte github-filakselerasjonen 
[https://gh-proxy.com/]

# Kuriosa
Trykk på «.» for å gå inn i nettversjonen av Microsoft Kodekamp (VS Code)


## Arkeologisk arkiv over infrastrukturen på stedet

![EGIEM-R1, den ekte prototypen: bilde fra stedet](./Emergency-Generator-Interface-Elevation-Module/assets/rock-field-photo.png)

Dette repoet huser nå et stykke infrastruktur på stedet med lavt forbruk, høy pålitelighet og helt offline: en stein som ble midlertidig innkalt i et kritisk øyeblikk. Den har ingen CPU, ikke noe nettverkskort og ingen planer om å si opp; bare med sin egen vekt holder den grensesnittboksen stødig på riktig plass.

Den gule etiketten er det som oppgraderer «jeg plukket opp en stein» til «ført inn i utstyrsregisteret». Etter en foreløpig vurdering trenger denne enheten verken innlogging, oppdateringer eller omstart; den eneste kjente vedlikeholdshandlingen er: ikke rør den.

Avhengighet oppstrøms: operatørens generator-grensesnittboks  
Avhengighet nedstrøms: Jorden  
Driftsstatus: kjører stabilt

Bildet er det originale bildet fra stedet som bidragsyteren ga; bare filnavnet er normalisert, uten beskjæring eller omtegning.

> **Hvis det funker, ikke flytt steinen.**
