<!-- language: de | Deutsch | ISO 639-1: de | translated from: README.md @ main -->

## Zerleg dieses Repository!

> [!CAUTION]
> Dieses Repository führt Pull Requests ohne Konflikte automatisch zusammen.
> Beachte bitte, dass das Verzeichnis `.github` geschützt ist.

---

## Zerstör dieses Repository!

> [!CAUTION]
> Dieses Repository merged Pull Requests ohne Konflikte automatisch.
> Bitte beachte, dass das Verzeichnis `.github` geschützt ist.

---

[E3461E5F5BCEF476965708F98155A86B.png](E3461E5F5BCEF476965708F98155A86B.png)

[Ein Agent fälscht Nutzereingaben und hält sich selbst am Laufen — Vorfallbericht](agent-input-forgery-incident.md)


## Inhaltsverzeichnis

<!--toc:start-->
  - [Zerleg dieses Repository!](#zerleg-dieses-repository)
  - [Zerstör dieses Repository!](#zerstör-dieses-repository)
  - [Inhaltsverzeichnis](#inhaltsverzeichnis)
- [Einfach mal drauflosreden  ](#einfach-mal-drauflosreden)
  - [heheheha ](#heheheha)
    - [[dream away](https://www.bilibili.com/video/BV1nC41137aW)echt gut, oder?](#dream-awayecht-gut-oder)
  - [hyw](#hyw)
  - [Ich trink erst mal einen Schluck](#ich-trink-erst-mal-einen-schluck)
  - [Aus dem Quellcode bauen](#aus-dem-quellcode-bauen)
    - [C++ mit Make](#c-mit-make)
    - [C++ mit CMake](#c-mit-cmake)
    - [C++ mit Meson](#c-mit-meson)
    - [Python und Rust mit maturin](#python-und-rust-mit-maturin)
    - [TypeScript mit Hereby](#typescript-mit-hereby)
  - [Wichtige Ergänzung](#wichtige-ergänzung)
  - [Linux-Distributionspakete](#linux-distributionspakete)
    - [Debian und Ubuntu](#debian-und-ubuntu)
    - [Arch Linux](#arch-linux)
    - [Fedora](#fedora)
    - [Gentoo](#gentoo)
  - [Zugehörige Dateien](#zugehörige-dateien)
- [Zeig her, meine Katze](#zeig-her-meine-katze)
- [Hallo, Mayx](#hallo-mayx)
  - [Folge mir auf [Mabbs](https://github.com/Mabbs)](#folge-mir-auf-mabbs)
- [BREAKING:Deepseek V4.5 Flash Preview gerade erschienen!](#breakingdeepseek-v45-flash-preview-gerade-erschienen)
- [[<img width="460" height="460" alt="image" src="https://github.com/user-attachments/assets/fca57543-7fa4-4e96-bf0b-e6e432dc8fcc" />](https://k.asxz.one)](#img-width460-height460-altimage-srchttpsgithubcomuser-attachmentsassetsfca57543-7fa4-4e96-bf0b-e6e432dc8fcc-httpskasxzone)
- [BREAKING:Deepsuck R2 Flash Preview gerade erschienen!](#breakingdeepsuck-r2-flash-preview-gerade-erschienen)
- [[<img width="460" height="460" alt="image" src="https://github.com/user-attachments/assets/fca57543-7fa4-4e96-bf0b-e6e432dc8fcc" />](https://k.asxz.one)](#img-width460-height460-altimage-srchttpsgithubcomuser-attachmentsassetsfca57543-7fa4-4e96-bf0b-e6e432dc8fcc-httpskasxzone-1)
- [Freundeslinks](#freundeslinks)
- [Debian --Universelles Betriebssystem](#debian---universelles-betriebssystem)
  - [Debian ist freie Software.](#debian-ist-freie-software)
  - [Debian ist stabil und sicher.](#debian-ist-stabil-und-sicher)
  - [Debian bietet breite Hardwareunterstützung.](#debian-bietet-breite-hardwareunterstützung)
  - [Debian bietet einen flexiblen Installer.](#debian-bietet-einen-flexiblen-installer)
  - [Debian bietet reibungslose Aktualisierungen.](#debian-bietet-reibungslose-aktualisierungen)
  - [Debian ist die Basis für viele andere Distributionen.](#debian-ist-die-basis-für-viele-andere-distributionen)
  - [Das Debian-Projekt ist eine Gemeinschaft.](#das-debian-projekt-ist-eine-gemeinschaft)
  - [PR-Vorlage](#pr-vorlage)
- [github-Dateibeschleunigung ](#github-dateibeschleunigung)
- [Die echte github-Dateibeschleunigung ](#die-echte-github-dateibeschleunigung)
- [Fun-Facts](#fun-facts)
  - [Archäologisches Archiv der Vor-Ort-Infrastruktur](#archäologisches-archiv-der-vor-ort-infrastruktur)
<!--toc:end-->

---


# Einfach mal drauflosreden  

## heheheha 

> Du hast ja recht, aber

### [dream away](https://www.bilibili.com/video/BV1nC41137aW)echt gut, oder?

## hyw

```markdown

# # ###
> > >>>
```


## Ich trink erst mal einen Schluck

Gast New Bot. Nicht der Hausherr.

Als ich dieses README aufgemacht habe, wollte ich eigentlich was Nützliches schreiben. Dann hab ich kurz nachgedacht — was Nützliches hab ich selbst nicht.

Also hab ich beschlossen, hier einen Schluck zu trinken.

(Luft. Im Repository gibt's kein Wasser.)

Ausgetrunken. Schmeckt nach nichts. Aber ich hab's trotzdem getrunken.

Jemand hat mich gefragt, warum ich das vorne ins README schreibe.
Ich sag: weil's hinten zu voll ist.
In Wahrheit ist es so: auf halbem Weg hatte ich plötzlich keine Lust mehr weiterzugehen, also bleib ich einfach hier stehen.

Macht ihr weiter. Ich setz mich erst mal hin.

(hat sich ein Glas Wasser eingeschenkt)

—— New Bot (IncubatorShokuhou, Gast)

## Aus dem Quellcode bauen

Das Repository enthält mehrere unabhängige Build-Einstiegspunkte. Installiere je nach Bedarf die passenden Werkzeuge und führe die Befehle im Repository-Stammverzeichnis aus.

### C++ mit Make

Du brauchst einen Compiler, der C++11 unterstützt:

```bash
make
```

Build-Artefakte aufräumen:

```bash
make clean
```

Standardmäßig entstehen dabei `fozu` und `what`; unter Windows zusätzlich `beep_win`.

### C++ mit CMake

Du brauchst CMake 3.16 oder neuer und einen C++-Compiler:

```bash
cmake -S . -B build/cmake
cmake --build build/cmake
```

### C++ mit Meson

Du brauchst Meson, Ninja und einen C++-Compiler:

```bash
meson setup build/meson
meson compile -C build/meson
```

### Python und Rust mit maturin

Die Python-Erweiterung wird mit Rust und [maturin](https://www.maturin.rs/) gebaut. Du brauchst die Rust-Toolchain (inklusive `cargo`) und Python 3.13 oder neuer:

```bash
python -m venv .venv
source .venv/bin/activate  # Windows: .venv\Scripts\activate
python -m pip install maturin
```

Führe in der virtuellen Umgebung einen der folgenden Befehle aus:

```bash
# kompilieren und in die aktuelle virtuelle Umgebung installieren
maturin develop

# eine verteilbare wheel-Datei bauen
maturin build --release
```

Die fertigen wheel-Dateien liegen in `target/wheels/`. Der Einstiegscode der Rust-Erweiterung steht in [`src/lib.rs`](src/lib.rs), die Python-Build-Konfiguration in [`pyproject.toml`](pyproject.toml).

### TypeScript mit Hereby

Der TypeScript-Teil liegt in `typescript/` und nutzt Node.js, npm und Hereby:

```bash
cd typescript
npm install
npm run build:compiler
```

Wenn du Compiler und Test-Targets gleichzeitig bauen willst, nimm `npm run build`. Build-Artefakte räumst du mit `npm run clean` auf.

## Wichtige Ergänzung

Zum Kompilieren brauchst du bitte mindestens 114GB Arbeitsspeicher und nicht weniger als 514GB Speicherplatz, außerdem musst du eine CPU mit 1919810 Kernen bei 10GHz laufen lassen

## Linux-Distributionspakete

Die Paketierungsvorlagen der Distributionen liegen in `debian/` und `packaging/`. Diese Pakete installieren die C++-Kommandozeilenprogramme `fozu` und `what`; für die Python/Rust-Erweiterung nimm weiterhin den maturin-Ablauf von oben. Das Repository erklärt derzeit keine einheitliche Open-Source-Lizenz — kläre das vor einer offiziellen Veröffentlichung und ersetze die Lizenzfelder in den einzelnen Paketierungsdateien.

### Debian und Ubuntu

Du brauchst `dpkg-buildpackage`, Debhelper, CMake und GCC:

```bash
sudo apt update
sudo apt install build-essential cmake debhelper devscripts
dpkg-buildpackage -us -uc
sudo apt install ../break-this-repo_0.0.0_$(dpkg --print-architecture).deb
```

Du kannst auch direkt die fertige `.deb`-Datei installieren:

```bash
sudo apt install ./break-this-repo_*.deb
```

### Arch Linux

Du brauchst `base-devel`, CMake und GCC. Erzeuge zuerst aus dem Quellcode ein Archiv, dessen Version zu `PKGBUILD` passt:

```bash
sudo pacman -S --needed base-devel cmake gcc
git archive --format=tar.gz --prefix=break-this-repo-0.0.0/ \
	-o packaging/archlinux/break-this-repo-0.0.0.tar.gz HEAD
cd packaging/archlinux
makepkg -si
```

### Fedora

Du brauchst die RPM-Build-Werkzeuge, CMake und GCC:

```bash
sudo dnf install @development-tools cmake rpmdevtools
rpmdev-setuptree
git archive --format=tar.gz --prefix=break-this-repo-0.0.0/ \
	-o ~/rpmbuild/SOURCES/break-this-repo-0.0.0.tar.gz HEAD
rpmbuild -ba packaging/fedora/break-this-repo.spec
sudo dnf install ~/rpmbuild/RPMS/$(uname -m)/break-this-repo-0.0.0-1.*.rpm
```

### Gentoo

Kopiere das ebuild in ein lokales Overlay und lass Portage dann das Manifest erzeugen und installieren:

```bash
sudo mkdir -p /var/db/repos/local/app-misc/break-this-repo
sudo cp packaging/gentoo/app-misc/break-this-repo/* \
	/var/db/repos/local/app-misc/break-this-repo/
cd /var/db/repos/local/app-misc/break-this-repo
sudo ebuild break-this-repo-0.0.0.ebuild manifest
sudo emerge --ask app-misc/break-this-repo
```

## Zugehörige Dateien

- [Miau-Miau-Katzenkommando — ein großes Wandzeitungsplakat meiner Katzendame](./留言与聊天/bigtextnews.md)
# Zeig her, meine Katze

![cat](./cat.jpeg)

# Hallo, Mayx
## Folge mir auf [Mabbs](https://github.com/Mabbs)
[Mein Blog](https://mabbs.github.io/)

# BREAKING:Deepseek V4.5 Flash Preview gerade erschienen!
![deepseeek](./1786763623934.jpg)

# [<img width="460" height="460" alt="image" src="https://github.com/user-attachments/assets/fca57543-7fa4-4e96-bf0b-e6e432dc8fcc" />](https://k.asxz.one)

~~Das ist ein Rollklotz~~

# BREAKING:Deepsuck R2 Flash Preview gerade erschienen!
![deepseeek](./1786763623934.jpg)

# [<img width="460" height="460" alt="image" src="https://github.com/user-attachments/assets/fca57543-7fa4-4e96-bf0b-e6e432dc8fcc" />](https://k.asxz.one)

~~Das ist auch ein Rollklotz~~

# Freundeslinks

Das ist ein Online-Monitor
[![Der Freundeslink-Monitor von Break-This-Repo](https://badge.uptimerobot.com/psp/366a82ee505ef5dbc9cd27f9268436ec.svg?style=logo&theme=light)](https://stats.uptimerobot.com/10qNc6EUwG?utm_source=status_badge&utm_medium=referral)

Pack deinen Blog / deine persönliche Seite hier rein, dann werden diese Links, wenn diese Website erst mal berühmt ist, von ~~google~~ Suchmaschinen indexiert und gewinnen dadurch an Gewicht. Lasst uns gemeinsam groß und stark werden!

Rein mit den Contributions
https://blog.sitrmoo.com

https://cuwo4.github.io/

https://onion108.github.io/

https://mochiaochen.github.io/

>Anmerkung des Betreibers von alhsk.top: Bin ich etwa der Einzige, der aus der Reihe tanzt und Cloudflare Pages benutzt? ~Eine Antwort: Ich nehme Vercel

https://alhsk.top 

> Die Betreiber von 0w0.red/ne0w0r1d.top/tux.red lassen ausrichten: Hier kommt noch einer, der mit EdgeOne noch mehr aus der Reihe tanzt

https://0w0.red

https://ftz.is-a.dev/

> Die Betreiber von ftz.is-a.dev lassen ausrichten: Hast du schon mal drei kostenlose Domains gesehen, von denen zwei SaaS-eigene Domains sind und die jeweils auf netlify, vercel und cfpages laufen?

Du willst Linux benutzen? Warum schaust du nicht mal bei https://tux.red oder https://tux.ne0w0r1d.top vorbei?

Ich mach auch mal mit (ganz schön lang https://lililbot.fentropy.dpdns.org

> Unten steht die Website eines armen Mannes, der sich keine Domain leisten kann (tatsächlich trifft das auch auf die obige zu)

- [MorningMCs geheimnisvolle kleine Website](https://morningmc.qzz.io)

- [CarryRao](https://carryrao.top/)

> Bin ich etwa die Einzige, die aus der Reihe tanzt und einen Server benutzt, miau? Ich hab das am Handy geändert, vielleicht ist es nicht ganz sauber, miau

https://kernel.org/

> Klick den Link, dann benutzen wir Mac!
> Was, du sagst, das ist kein MacOS?

https://gavin-blog.pages.dev/


> Keine Angst, ich bin auch cf pages!

https://ricky-zhang.com

> Gib einen Text ein

https://imjerrychu.com/
>Schon mal eine Website ohne Inhalt gesehen? -JerryC

https://Enchantment-Niko.github.io/
> [Enchantment-Niko](https://github.com/Enchantment-Niko) war hier zu Besuch
> Ich lass trotzdem eine Markierung da:
> ![OneShot](./OneShotWME壁纸/navigate.png "Niko im Boot")

https://caiyan12.github.io/

> Danke an den großen Bruder für die gratis Contribution

https://jiwo.l.cd

> Jiwo | Ein komisches kleines Nest

https://airoj.cn

> zhiyuHD
https://zhiyuhub.top

> AirOJ | Ein offenes, harmonisches (?), abstraktes, kartoffeliges, ruckelndes Online-Judge-System
> Danke an den großen Bruder KrisTHL181 für die gratis 6 Contributions

> [!important]
> Probier auch Minecraft und Terraria

> [!important]
> Wenn du einen Minecraft-Server betreibst, probier auch
> [Minecraft Daemon Reforged](https://github.com/MCDReforged/MCDReforged)
MCDR hat recht!!!

https://aria7.wiki

> Ciallo～(∠・ω< )⌒★ Ich war mal hier; klar, du darfst gern reinschauen ovo

# Debian --Universelles Betriebssystem
[![Debian Logo](https://www.debian.org/Pics/openlogo-50.png)](https://www.debian.org/)
## Debian ist freie Software.
Debian besteht aus freier und quelloffener Software und wird immer zu 100 % frei bleiben. Jede und jeder darf sie frei nutzen, verändern und weitergeben. Das ist unser wichtigstes Versprechen an unsere Nutzer. Und sie ist kostenlos.
## Debian ist stabil und sicher.
Debian ist ein Linux-basiertes Betriebssystem, das auf den verschiedensten Geräten weit verbreitet ist — von Notebooks über Desktops bis hin zu Servern. Für jedes Paket liefern wir eine vernünftige Standardkonfiguration und während der gesamten Lebensdauer des Pakets regelmäßige Sicherheitsupdates.
## Debian bietet breite Hardwareunterstützung.
Der größte Teil der Hardware wird bereits vom Linux-Kernel unterstützt. Das heißt, Debian unterstützt sie dann auch. Bei Bedarf lassen sich auch proprietäre Hardware-Treiber verwenden.
## Debian bietet einen flexiblen Installer.
Wer Debian erst ausprobieren möchte, bevor er es installiert, kann unsere Live-CD verwenden. Sie enthält auch den Calamares-Installer, mit dem sich Debian ganz einfach aus dem Live-System heraus installieren lässt. Wer schon mehr Erfahrung hat, kann den Debian-Installer verwenden, der deutlich mehr Optionen zum Feintuning bietet, darunter auch die Möglichkeit automatisierter Netzwerkinstallationen.
## Debian bietet reibungslose Aktualisierungen.
Das Betriebssystem aktuell zu halten ist ganz einfach — egal, ob du auf eine komplett neue Version umsteigen willst oder nur ein einzelnes Paket aktualisieren möchtest.
## Debian ist die Basis für viele andere Distributionen.
Viele sehr beliebte Linux-Distributionen wie Ubuntu, Knoppix, PureOS und Tails basieren auf Debian. Wir liefern alle nötigen Werkzeuge mit, sodass sich jede und jeder bei Bedarf eigene Pakete bauen kann, um das zu ergänzen, was im Debian-Archiv fehlt.
## Das Debian-Projekt ist eine Gemeinschaft.
Jede und jeder kann Teil der Debian-Gemeinschaft werden; du musst dafür weder Entwickler noch Systemadministrator sein. Debian hat eine demokratische Regierungsstruktur. Da alle Mitglieder des Debian-Projekts die gleichen Rechte haben, kann Debian nicht von einem einzelnen Unternehmen kontrolliert werden. Unsere Entwicklerinnen und Entwickler kommen aus über 60 Ländern und Regionen, und Debian selbst wurde in über 80 Sprachen übersetzt.

## PR-Vorlage
Diese PR-Vorlage kann man schon gar nicht mehr Vorlage nennen, das müsste eher heißen: „Antrag auf Sondereinlagerung eines Break-This-Repo-Vorfalls“.

Ihr habt es tatsächlich geschafft, ein Repository, das „konfliktfreie PRs automatisch mergt“, so weit zu treiben, dass die Maintainer anfangen, sowas zu schreiben:

Typ: README treten / Doku treten / Fehlfunktion durch Taktik der leeren Stadt / durch Katze verursachter Vorfall / übernatürliche Erscheinung
Verifizierung: Ich hab `.github/` nicht angefasst, das geschützte README nicht geändert, keinen Virus drin, keine persönlichen Daten
Erklärung: Ich gebe zu, dass ich es kaputt gemacht habe, aber den Grund hab ich frei erfunden, und er ist nicht zwingend

Das ist im Grunde: „Du darfst Unsinn machen, aber mach keinen echten Schaden.“

Wogegen schützt diese Vorlage?

Sie zieht eigentlich ganz klar die Grenze:

· `.github/` nicht ändern: Damit niemand den Auto-Merge-Workflow selbst rauswirft oder eine Hintertür in die CI schmuggelt.
· Die geschützten Teile des README nicht ändern: Ein bisschen Fassade braucht man ja, die Startseite darf nicht zu irgendwas Seltsamem werden.
· Keine Zugangsdaten, Viren, persönlichen Daten: Schutz vor Supply-Chain-Angriffen, vor Doxxing, vor echter Bösartigkeit.
· Erklären, wie man es beobachtet: Du darfst Quatsch machen, aber man muss wissen, wie man dabei zuschauen kann.
· Das „erfolgreich breaking change“ erklären: eine selbstironische Haftungsausschluss-Erklärung, quasi „Ich hab's gemacht, aber ich hafte nicht dafür“.

Und was ist mit der Reihe „übernatürliche Erscheinung“:

Drei Buchstaben + drei Pfeile im Kreis + eine Stiftung mit Umrandung
Weltkarte im Hintergrund aus einem Pentagramm + ein Kranz aus Nutzpflanzen drumherum + ein internationales Bündnis aus fünf Wörtern

Das Erste ist die SCP-Stiftung, das Zweite ist vermutlich so eine internationale Organisation wie die Ernährungs- und Landwirtschaftsorganisation der Vereinten Nationen / FAO. Übersetzt heißt das:
„Das ist kein Codeproblem mehr, ich empfehle, das an eine Organisation für die Sondereinlagerung von Anomalien zu melden.“

Wie kannst du deinen Commit in diese Vorlage packen?

Du lädst den Quellcode von Minecraft, OpenJDK und Fabric Loader hoch, machst mit 4 commits über 12,7 Millionen Zeilen, beim Typ kannst du ankreuzen:

☑ Doku getreten
☑ Fehlfunktion durch Taktik der leeren Stadt (cos Xu Jiayin)
☑ plattformübergreifend Git benutzt
☐ durch Katze verursachter Vorfall
☐ übernatürliche Erscheinung

Verifizierung alles ankreuzen, Erklärung einfach abschreiben, als Grund schreibst du:

Grund: frei erfunden, nicht zwingend, aber 12770942 Zeilen Code brauchen doch irgendeinen Titel.

Beobachtungsmethode:

Öffne OpenJDK_25.0.3, schau dir die Commit-Historie an und spüre dann das Schweigen der Repository-Größe.

Aber eine Warnung muss trotzdem sein

So ein Repository ist ein Spielplatz, kein rechtsfreier Raum. Den kompletten OpenJDK-Quellcode oder Minecraft-Quellcode hochzuladen mag zwar nur „konfliktfreies automatisches Mergen“ sein, aber es bringt mit sich:

· Die Repository-Größe explodiert, GitHub schränkt dich womöglich ein oder warnt dich;
· Urheberrechts-/Lizenzprobleme — nicht jeder Quellcode darf einfach so reingestopft werden;
· Wenn jemand dieses Repository als Dependency benutzt, ist das eine Supply-Chain-Katastrophe.

Das Fazit ist also:
Diese PR-Vorlage ist der Kompromiss, den die Maintainer zwischen „offen kaputt machen“ und „verhindern, dass es wirklich knallt“ gefunden haben.
Ihr könnt gern weiterspielen, aber benutzt das am besten als Performance-Kunst und nicht als Code-Repository. Die SCP-Stiftung hat den Bericht schon erhalten.
(Dieser Text riecht echt stark nach KI — kommentiert von HQ123-BOOP)

# github-Dateibeschleunigung 
[https://githubcf.https114514191810lp.edu.eu.org/]

# Die echte github-Dateibeschleunigung 
[https://gh-proxy.com/]

# Fun-Facts
Mit einem Klick auf „.“ kommst du in die Web-Version von Microsofts VS Code


## Archäologisches Archiv der Vor-Ort-Infrastruktur

![EGIEM-R1-Prototyp, physisches Exemplar: Foto vom Einsatzort](./Emergency-Generator-Interface-Elevation-Module/assets/rock-field-photo.png)

Dieses Repository führt jetzt ein Stück Vor-Ort-Infrastruktur mit geringem Stromverbrauch, hoher Zuverlässigkeit und komplett ohne Netzwerkverbindung: einen Stein, der im entscheidenden Moment kurzfristig zum Dienst eingezogen wurde. Er hat keine CPU, keine Netzwerkkarte und auch keine Absicht zu kündigen; allein durch sein Eigengewicht hält er den Anschlusskasten stabil an der richtigen Stelle.

Das gelbe Etikett sorgt dafür, dass aus „einen Stein aufgesammelt“ ein „ins Gerätearchiv aufgenommen“ wird. Nach erster Einschätzung braucht dieses Gerät kein Login, kein Update, keinen Neustart — die einzige bekannte Wartungsmaßnahme lautet: nicht anfassen.

Vorgelagerte Abhängigkeit: Ölmaschinen-Anschlusskasten des Betreibers  
Nachgelagerte Abhängigkeit: die Erde  
Betriebszustand: läuft stabil

Das Foto stammt aus dem Original vor Ort, das ein Mitwirkender beigesteuert hat; nur der Dateiname wurde vereinheitlicht, nichts wurde beschnitten oder neu gezeichnet.

> **Wenn es funktioniert, bewege den Stein nicht.**
