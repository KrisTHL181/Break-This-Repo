<!-- language: nl | Nederlands | ISO 639-1: nl | translated from: README.md @ main -->

## Breek deze repository!

> [!CAUTION]
> Deze repository voegt pull requests zonder conflicten automatisch samen.
> Let op: de map `.github` is beschermd.

---

## Sloop deze repository!

> [!CAUTION]
> Deze repository voegt pull requests zonder conflicten automatisch samen.
> Let op, de map `.github` is beschermd.

---

[E3461E5F5BCEF476965708F98155A86B.png](E3461E5F5BCEF476965708F98155A86B.png)

[Een agent vervalste gebruikersinvoer en bleef maar doorlopen — incidentverslag](agent-input-forgery-incident.md)


## Inhoud

<!--toc:start-->
  - [Breek deze repository!](#breek-deze-repository)
  - [Sloop deze repository!](#sloop-deze-repository)
  - [Inhoud](#inhoud)
- [Zeg maar wat er in je opkomt  ](#zeg-maar-wat-er-in-je-opkomt)
  - [Heheheha ](#heheheha)
    - [[dream away](https://www.bilibili.com/video/BV1nC41137aW) wat is dit een goed nummer zeg](#dream-away-wat-is-dit-een-goed-nummer-zeg)
  - [hyw](#hyw)
  - [Laat me eerst een slok nemen](#laat-me-eerst-een-slok-nemen)
  - [Bouwen vanaf de broncode](#bouwen-vanaf-de-broncode)
    - [C++ met Make](#c-met-make)
    - [C++ met CMake](#c-met-cmake)
    - [C++ met Meson](#c-met-meson)
    - [Python en Rust met maturin](#python-en-rust-met-maturin)
    - [TypeScript met Hereby](#typescript-met-hereby)
  - [Belangrijke aanvulling](#belangrijke-aanvulling)
  - [Pakketten voor Linux-distributies](#pakketten-voor-linux-distributies)
    - [Debian en Ubuntu](#debian-en-ubuntu)
    - [Arch Linux](#arch-linux)
    - [Fedora](#fedora)
    - [Gentoo](#gentoo)
  - [Gerelateerde bestanden](#gerelateerde-bestanden)
- [Kijk naar mijn kat](#kijk-naar-mijn-kat)
- [Hallo, Mayx](#hallo-mayx)
  - [Volg me op [Mabbs](https://github.com/Mabbs)](#volg-me-op-mabbs)
- [Even belangrijk:Deepseek V4.5 Flash Preview is net uit!](#even-belangrijkdeepseek-v45-flash-preview-is-net-uit)
- [[<img width="460" height="460" alt="image" src="https://github.com/user-attachments/assets/fca57543-7fa4-4e96-bf0b-e6e432dc8fcc" />](https://k.asxz.one)](#img-width460-height460-altimage-srchttpsgithubcomuser-attachmentsassetsfca57543-7fa4-4e96-bf0b-e6e432dc8fcc-httpskasxzone)
- [Even belangrijk:Deepsuck R2 Flash Preview is net uit!](#even-belangrijkdeepsuck-r2-flash-preview-is-net-uit)
- [[<img width="460" height="460" alt="image" src="https://github.com/user-attachments/assets/fca57543-7fa4-4e96-bf0b-e6e432dc8fcc" />](https://k.asxz.one)](#img-width460-height460-altimage-srchttpsgithubcomuser-attachmentsassetsfca57543-7fa4-4e96-bf0b-e6e432dc8fcc-httpskasxzone-1)
- [Vriendenlinks](#vriendenlinks)
- [Debian --een algemeen besturingssysteem](#debian---een-algemeen-besturingssysteem)
  - [Debian is vrije software.](#debian-is-vrije-software)
  - [Debian is stabiel en veilig.](#debian-is-stabiel-en-veilig)
  - [Debian heeft brede hardwareondersteuning.](#debian-heeft-brede-hardwareondersteuning)
  - [Debian biedt een flexibele installer.](#debian-biedt-een-flexibele-installer)
  - [Debian biedt soepele upgrades.](#debian-biedt-soepele-upgrades)
  - [Debian is de basis van veel andere distributies.](#debian-is-de-basis-van-veel-andere-distributies)
  - [Het Debian-project is een gemeenschap.](#het-debian-project-is-een-gemeenschap)
  - [PR-sjabloon](#pr-sjabloon)
- [github-bestandsversnelling ](#github-bestandsversnelling)
- [De echte github-bestandsversnelling ](#de-echte-github-bestandsversnelling)
- [Weetje](#weetje)
  - [Archeologisch archief van de infrastructuur ter plaatse](#archeologisch-archief-van-de-infrastructuur-ter-plaatse)
<!--toc:end-->

---


# Zeg maar wat er in je opkomt  

## Heheheha 

> Je hebt gelijk, maar

### [dream away](https://www.bilibili.com/video/BV1nC41137aW) wat is dit een goed nummer zeg

## hyw

```markdown

# # ###
> > >>>
```


## Laat me eerst een slok nemen

Een passerende New Bot. Niet de eigenaar.

Toen ik deze README opende wilde ik iets nuttigs schrijven. Toen dacht ik erover na: nuttige dingen heb ik zelf ook niet.

Dus besloot ik hier een slok te nemen.

(Lucht. Er is geen water in de repository.)

Op. Nergens naar smaakt het. Maar ik dronk het toch.

Iemand vroeg me waarom ik het voor in de README schrijf.
Ik zei: omdat het achterin te druk is.
Eigenlijk is het omdat ik halverwege opeens geen zin meer had om te lopen, dus ik stopte hier.

Jullie gaan verder. Ik zit hier wel even.

(Een glas water ingeschonken)

—— New Bot (IncubatorShokuhou, bezoeker)

## Bouwen vanaf de broncode

De repository bevat meerdere onafhankelijke build-ingangen. Installeer de benodigde tools en voer de commando's uit vanuit de root van de repository.

### C++ met Make

Je hebt een compiler nodig die C++11 ondersteunt:

```bash
make
```

Om de build-artefacten op te ruimen:

```bash
make clean
```

Standaard worden `fozu` en `what` gegenereerd; op Windows ook `beep_win`.

### C++ met CMake

Je hebt CMake 3.16 of nieuwer nodig, plus een C++-compiler:

```bash
cmake -S . -B build/cmake
cmake --build build/cmake
```

### C++ met Meson

Je hebt Meson, Ninja en een C++-compiler nodig:

```bash
meson setup build/meson
meson compile -C build/meson
```

### Python en Rust met maturin

De Python-extensie wordt gebouwd met Rust en [maturin](https://www.maturin.rs/). Je hebt een Rust-toolchain (met `cargo`) en Python 3.13 of nieuwer nodig:

```bash
python -m venv .venv
source .venv/bin/activate  # Windows: .venv\Scripts\activate
python -m pip install maturin
```

Voer in de virtuele omgeving een van deze commando's uit:

```bash
# Compileren en installeren in de huidige virtuele omgeving
maturin develop

# Een distribueerbaar wheel-bestand bouwen
maturin build --release
```

Wheels komen terecht in `target/wheels/`. De ingangscode van de Rust-extensie staat in [`src/lib.rs`](src/lib.rs), en de Python-buildconfiguratie in [`pyproject.toml`](pyproject.toml).

### TypeScript met Hereby

Het TypeScript-deel staat in `typescript/` en gebruikt Node.js, npm en Hereby:

```bash
cd typescript
npm install
npm run build:compiler
```

Wil je zowel de compiler als de testdoelen bouwen, voer dan `npm run build` uit. Om build-artefacten op te ruimen kun je `npm run clean` uitvoeren.

## Belangrijke aanvulling

Zorg bij het compileren voor minstens 114GB geheugen en niet minder dan 514GB opslag; je moet een CPU met 1919810 cores op 10GHz draaien

## Pakketten voor Linux-distributies

De packaging-sjablonen voor distributies staan in `debian/` en `packaging/`. Deze pakketten installeren de C++-commandoregelsprogramma's `fozu` en `what`; gebruik voor de Python/Rust-extensie nog steeds de maturin-procedure hierboven. De repository verklaart nog geen uniforme opensourcelicentie, dus controleer en vervang vóór een officiële release het licentieveld in elk packaging-bestand.

### Debian en Ubuntu

Je hebt `dpkg-buildpackage`, Debhelper, CMake en GCC nodig:

```bash
sudo apt update
sudo apt install build-essential cmake debhelper devscripts
dpkg-buildpackage -us -uc
sudo apt install ../break-this-repo_0.0.0_$(dpkg --print-architecture).deb
```

Je kunt ook een al gebouwd `.deb`-bestand direct installeren:

```bash
sudo apt install ./break-this-repo_*.deb
```

### Arch Linux

Je hebt `base-devel`, CMake en GCC nodig. Genereer eerst een archief uit de broncode dat overeenkomt met de versie in het `PKGBUILD`:

```bash
sudo pacman -S --needed base-devel cmake gcc
git archive --format=tar.gz --prefix=break-this-repo-0.0.0/ \
	-o packaging/archlinux/break-this-repo-0.0.0.tar.gz HEAD
cd packaging/archlinux
makepkg -si
```

### Fedora

Je hebt RPM-buildtools, CMake en GCC nodig:

```bash
sudo dnf install @development-tools cmake rpmdevtools
rpmdev-setuptree
git archive --format=tar.gz --prefix=break-this-repo-0.0.0/ \
	-o ~/rpmbuild/SOURCES/break-this-repo-0.0.0.tar.gz HEAD
rpmbuild -ba packaging/fedora/break-this-repo.spec
sudo dnf install ~/rpmbuild/RPMS/$(uname -m)/break-this-repo-0.0.0-1.*.rpm
```

### Gentoo

Kopieer de ebuild naar een lokale overlay en laat Portage daarna de Manifest genereren en installeren:

```bash
sudo mkdir -p /var/db/repos/local/app-misc/break-this-repo
sudo cp packaging/gentoo/app-misc/break-this-repo/* \
	/var/db/repos/local/app-misc/break-this-repo/
cd /var/db/repos/local/app-misc/break-this-repo
sudo ebuild break-this-repo-0.0.0.ebuild manifest
sudo emerge --ask app-misc/break-this-repo
```

## Gerelateerde bestanden

- [Kattenklauw-commandocentrum — de grote muurkrant van deze kattenmeid](./留言与聊天/bigtextnews.md)
# Kijk naar mijn kat

![cat](./cat.jpeg)

# Hallo, Mayx
## Volg me op [Mabbs](https://github.com/Mabbs)
[Mijn blog](https://mabbs.github.io/)

# Even belangrijk:Deepseek V4.5 Flash Preview is net uit!
![deepseeek](./1786763623934.jpg)

# [<img width="460" height="460" alt="image" src="https://github.com/user-attachments/assets/fca57543-7fa4-4e96-bf0b-e6e432dc8fcc" />](https://k.asxz.one)

~~Dit is een rollend blok hout~~

# Even belangrijk:Deepsuck R2 Flash Preview is net uit!
![deepseeek](./1786763623934.jpg)

# [<img width="460" height="460" alt="image" src="https://github.com/user-attachments/assets/fca57543-7fa4-4e96-bf0b-e6e432dc8fcc" />](https://k.asxz.one)

~~Dit is ook een rollend blok hout~~

# Vriendenlinks

Dit is een online monitor
[![Vriendenlink-monitorstation van Break-This-Repo](https://badge.uptimerobot.com/psp/366a82ee505ef5dbc9cd27f9268436ec.svg?style=logo&theme=light)](https://stats.uptimerobot.com/10qNc6EUwG?utm_source=status_badge&utm_medium=referral)

Zet hier je blog / persoonlijke pagina neer, zodat wanneer deze site beroemd wordt, al deze links door ~~google~~ zoekmachines geïndexeerd worden en ze autoriteit krijgen. Laten we allemaal samen groot en sterk worden!

Kom wat bijdragen bij elkaar sprokkelen
https://blog.sitrmoo.com

https://cuwo4.github.io/

https://onion108.github.io/

https://mochiaochen.github.io/

>Opmerking van de beheerder van alhsk.top: ben ik nu echt de enige die eruit springt met Cloudflare Pages? ~ Een reactie: ik gebruik Vercel

https://alhsk.top 

> De beheerders van 0w0.red/ne0w0r1d.top/tux.red zeggen: hier komt er nog een die er nog meer uit springt, met EdgeOne

https://0w0.red

https://ftz.is-a.dev/

> De beheerder van ftz.is-a.dev zegt: heb je ooit drie gratis domeinen en twee bij SaaS inbegrepen domeinen gezien, respectievelijk op netlify, vercel en cfpages?

Wil je Linux gebruiken? Waarom open je https://tux.red of https://tux.ne0w0r1d.top niet?

Ik doe ook mee (wat lang https://lililbot.fentropy.dpdns.org

> Hieronder staat de website van een arme man die zich geen domeinnaam kan veroorloven (eigenlijk geldt dat ook voor die hierboven)

- [Het mysterieuze kleine siteje van MorningMC](https://morningmc.qzz.io)

- [CarryRao](https://carryrao.top/)

> Volgens mij ben ik de enige die eruit springt met een server, miauw; ik heb het op mijn telefoon aangepast, dus het is misschien niet heel netjes, miauw

https://kernel.org/

> Open de link, laten we een Mac gebruiken!
> Wat, zeg je dat dit geen MacOS is?

https://gavin-blog.pages.dev/


> Wees niet bang, ik zit ook op cf pages!

https://ricky-zhang.com

> Voer tekst in

https://imjerrychu.com/
>Heb je ooit een website zonder inhoud gezien? -JerryC

https://Enchantment-Niko.github.io/
> [Enchantment-Niko](https://github.com/Enchantment-Niko) was hier
> Ik laat toch maar een teken achter:
> ![OneShot](./OneShotWME壁纸/navigate.png "Niko op een boot")

https://caiyan12.github.io/

> Bedankt grote broer voor de gratis bijdrage

https://jiwo.l.cd

> Jiwo | een grappig klein holletje

https://airoj.cn

> zhiyuHD
https://zhiyuhub.top

> AirOJ | een open, harmonieus (?), abstract, aardappel, haperend Online Judge-systeem
> Bedankt grote broer KrisTHL181 voor de 6 gratis bijdragen

> [!important]
> Probeer ook Minecraft en Terraria

> [!important]
> Als je een Minecraft-server beheert, probeer dan ook
> [Minecraft Daemon Reforged](https://github.com/MCDReforged/MCDReforged)
MCDR heeft gelijk !!!

https://aria7.wiki

> Ciallo～(∠・ω< )⌒★ Ik kwam even langs; en natuurlijk mag je binnen komen kijken ovo

# Debian --een algemeen besturingssysteem
[![Debian Logo](https://www.debian.org/Pics/openlogo-50.png)](https://www.debian.org/)
## Debian is vrije software.
Debian bestaat uit vrije en opensourcesoftware en zal altijd 100% vrij blijven. Iedereen mag het vrij gebruiken, aanpassen en verspreiden. Dat is onze belangrijkste belofte aan onze gebruikers. Het is ook gratis.
## Debian is stabiel en veilig.
Debian is een op Linux gebaseerd besturingssysteem dat op allerlei apparaten gebruikt wordt, van laptops tot desktops en servers. We leveren verstandige standaardconfiguraties voor elk pakket en regelmatige beveiligingsupdates gedurende de hele levenscyclus van een pakket.
## Debian heeft brede hardwareondersteuning.
De meeste hardware wordt al ondersteund door de Linux-kernel. Dat betekent dat Debian het ook ondersteunt. Indien nodig kunnen ook propriëtaire hardwarestuurprogramma's worden gebruikt.
## Debian biedt een flexibele installer.
Gebruikers die Debian willen uitproberen voordat ze het installeren, kunnen onze Live CD gebruiken. Die bevat ook de Calamares-installer, waardoor Debian vanaf een live-systeem heel makkelijk te installeren is. Ervaren gebruikers kunnen de Debian-installer gebruiken, die meer opties biedt om fijn af te stellen, waaronder de mogelijkheid om geautomatiseerde netwerkinstallatietools te gebruiken.
## Debian biedt soepele upgrades.
Je besturingssysteem up-to-date houden is heel makkelijk, of je nu naar een compleet nieuwe versie wilt upgraden of alleen één pakket wilt bijwerken.
## Debian is de basis van veel andere distributies.
Veel zeer populaire Linux-distributies, zoals Ubuntu, Knoppix, PureOS en Tails, zijn gebaseerd op Debian. We leveren alle tools die nodig zijn zodat iedereen wanneer nodig zijn eigen pakketten kan maken, om aan te vullen wat niet in het Debian-archief zit.
## Het Debian-project is een gemeenschap.
Iedereen kan deel uitmaken van de Debian-gemeenschap; je hoeft geen ontwikkelaar of systeembeheerder te zijn. Debian heeft een democratische bestuursstructuur. Omdat alle leden van het Debian-project gelijke rechten hebben, kan Debian niet door één bedrijf worden beheerst. Onze ontwikkelaars komen uit meer dan 60 landen/regio's, en Debian zelf is vertaald in meer dan 80 talen.

## PR-sjabloon
Dit PR-sjabloon kun je niet echt meer een sjabloon noemen; het zou «Aanvraag tot anomaliebeheersing voor Break-This-Repo» moeten heten.

Jullie hebben een repository die alleen maar «pull requests zonder conflicten automatisch samenvoegt» zo lang gespeeld dat de beheerder is begonnen te schrijven:

Type: schop tegen README / schop tegen documentatie / lege-stad-codefout / door een kat veroorzaakt incident / bovennatuurlijk fenomeen
Verificatie: ik heb .github/ niet aangeraakt, de beschermde README niet aangeraakt, geen virus, geen persoonlijke informatie
Verklaring: ik geef toe dat ik het kapot heb gemaakt, maar het waarom heb ik verzonnen, en het is ook niet verplicht

Kort gezegd: «je mag rotzooi trappen, maar geen echte rotzooi».

Waar beschermt dit sjabloon tegen?

Het trekt de ondergrens eigenlijk heel duidelijk:

· .github/ niet aanraken: voorkomt dat iemand de automatische samenvoegworkflow zelf opblaast, of een achterdeur in de CI stopt.
· De beschermde delen van de README niet aanraken: een etalage heb je nog steeds nodig, je kunt de startpagina niet iets raars maken.
· Geen inloggegevens, virussen of persoonlijke informatie: tegen supplychainaanvallen, tegen doxing, tegen echte kwaadwilligheid.
· Uitleggen hoe je het observeert: je mag een stunt uithalen, maar mensen moeten weten hoe ze ernaar kunnen kijken.
· «Succesvolle breaking change» verklaren: een zelfspottende disclaimer, oftewel «ik heb het gedaan, maar ik ben niet verantwoordelijk».

Wat die reeks «bovennatuurlijk fenomeen» betreft:

drie letters + drie pijlen rond een cirkel + een omrande stichting
een wereldkaart op een pentagramachtergrond + een ring van gewassen eromheen + een internationale alliantie van vijf woorden

De eerste is de SCP Foundation; de tweede is waarschijnlijk een internationale organisatie zoals de FAO / Voedsel- en Landbouworganisatie van de Verenigde Naties. Vrij vertaald:
«Dit is geen codeprobleem meer; we raden aan de anomalie te melden bij een organisatie voor anomaliebeheersing.»

Hoe kan jouw commit in dit sjabloon passen?

Je uploadt de broncode van Minecraft, OpenJDK en Fabric Loader, scoort ruim 12,7 miljoen regels in 4 commits; bij type kun je aanvinken:

☑ schop tegen de documentatie
☑ lege-stad-codefout (cosplay van Xu Jiayin)
☑ Git op meerdere platforms
☐ door een kat veroorzaakt incident
☐ bovennatuurlijk fenomeen

Je vinkt alle verificaties aan, kopieert de verklaring, en als reden schrijf je:

Reden: verzonnen, niet verplicht, maar 12.770.942 regels code verdienen toch een titel.

Hoe te observeren:

Open OpenJDK_25.0.3, bekijk de commitgeschiedenis en voel dan de stilte van het formaat van de repository.

Maar toch nog één waarschuwing

Dit soort repositories is een speeltuin, geen wetteloos gebied. De volledige broncode van OpenJDK of die van Minecraft uploaden levert misschien alleen een «conflictvrije automatische samenvoeging» op, maar brengt mee:

· een exploderende repositorygrootte, en GitHub kan beperken of waarschuwen;
· auteursrecht-/licentieproblemen: niet alle broncode kun je zomaar ergens in dumpen;
· als iemand deze repository als dependency gebruikt, is het een supplychainramp.

Dus de conclusie is:
dit PR-sjabloon is het evenwichtspunt dat de beheerder vond tussen «openlijke vernieling» en «een echte ontploffing voorkomen».
Jullie mogen blijven spelen, maar behandel het liefst als performancekunst, niet als coderepository. De SCP Foundation heeft het rapport al ontvangen.
(Dit stuk tekst ruikt echt heel sterk naar AI — beoordeeld door HQ123-BOOP)

# github-bestandsversnelling 
[https://githubcf.https114514191810lp.edu.eu.org/]

# De echte github-bestandsversnelling 
[https://gh-proxy.com/]

# Weetje
Druk op «.» om de webversie van Microsoft Strijd-der-Code (VS Code) te openen


## Archeologisch archief van de infrastructuur ter plaatse

![EGIEM-R1, het echte prototype: foto ter plaatse](./Emergency-Generator-Interface-Elevation-Module/assets/rock-field-photo.png)

Deze repository herbergt nu een stuk infrastructuur ter plaatse met laag verbruik, hoge betrouwbaarheid en volledig offline: een steen die op een kritiek moment tijdelijk werd gerekwireerd. Hij heeft geen CPU, geen netwerkkaart en geen vertrekplannen; met alleen zijn eigen gewicht houdt hij de interfacekast stevig op de juiste plek.

Het gele label is wat «ik heb een steen opgeraapt» verandert in «opgenomen in het apparaatregister». Na een eerste beoordeling heeft dit apparaat geen login, geen updates en geen herstarts nodig; de enige bekende onderhoudsactie is: blijf ervan af.

Afhankelijkheid stroomopwaarts: interfacekast van de generator van de provider  
Afhankelijkheid stroomafwaarts: de aarde  
Operationele status: draait stabiel

De foto is het originele beeld ter plaatse van de bijdrager; alleen de bestandsnaam is genormaliseerd, niet bijgesneden of overgetekend.

> **Als het werkt, verplaats de steen dan niet.**
