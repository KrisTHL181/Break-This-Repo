<!-- language: af | Afrikaans | ISO 639-1: af | translated from: README.md @ main -->

## Breek hierdie repo!

> [!CAUTION]
> Hierdie repo voeg pull requests sonder konflikte outomaties saam.
> Let daarop dat die `.github`-gids beskerm is.

---

## Verwoes hierdie repo!

> [!CAUTION]
> Hierdie repo voeg pull requests sonder konflikte outomaties saam.
> Let op: die `.github`-gids is beskerm.

---

[E3461E5F5BCEF476965708F98155A86B.png](E3461E5F5BCEF476965708F98155A86B.png)

['n Agent het gebruikersinvoer vervals en self bly loop — voorvalverslag](agent-input-forgery-incident.md)


## Inhoud

<!--toc:start-->
  - [Breek hierdie repo!](#breek-hierdie-repo)
  - [Verwoes hierdie repo!](#verwoes-hierdie-repo)
  - [Inhoud](#inhoud)
- [Sê wat by jou opkom  ](#sê-wat-by-jou-opkom)
  - [Heheheha ](#heheheha)
    - [[dream away](https://www.bilibili.com/video/BV1nC41137aW) wat 'n goeie liedjie](#dream-away-wat-n-goeie-liedjie)
  - [hyw](#hyw)
  - [Laat my eers 'n slukkie drink](#laat-my-eers-n-slukkie-drink)
  - [Bou vanaf die bronkode](#bou-vanaf-die-bronkode)
    - [C++ met Make](#c-met-make)
    - [C++ met CMake](#c-met-cmake)
    - [C++ met Meson](#c-met-meson)
    - [Python en Rust met maturin](#python-en-rust-met-maturin)
    - [TypeScript met Hereby](#typescript-met-hereby)
  - [Belangrike aanvulling](#belangrike-aanvulling)
  - [Pakkette vir Linux-verspreidings](#pakkette-vir-linux-verspreidings)
    - [Debian en Ubuntu](#debian-en-ubuntu)
    - [Arch Linux](#arch-linux)
    - [Fedora](#fedora)
    - [Gentoo](#gentoo)
  - [Verwante lêers](#verwante-lêers)
- [Kyk na my kat](#kyk-na-my-kat)
- [Hallo, Mayx](#hallo-mayx)
  - [Volg my op [Mabbs](https://github.com/Mabbs)](#volg-my-op-mabbs)
- [BREKEND:Deepseek V4.5 Flash Preview is pas vrygestel!](#brekenddeepseek-v45-flash-preview-is-pas-vrygestel)
- [[<img width="460" height="460" alt="image" src="https://github.com/user-attachments/assets/fca57543-7fa4-4e96-bf0b-e6e432dc8fcc" />](https://k.asxz.one)](#img-width460-height460-altimage-srchttpsgithubcomuser-attachmentsassetsfca57543-7fa4-4e96-bf0b-e6e432dc8fcc-httpskasxzone)
- [BREKEND:Deepsuck R2 Flash Preview is pas vrygestel!](#brekenddeepsuck-r2-flash-preview-is-pas-vrygestel)
- [[<img width="460" height="460" alt="image" src="https://github.com/user-attachments/assets/fca57543-7fa4-4e96-bf0b-e6e432dc8fcc" />](https://k.asxz.one)](#img-width460-height460-altimage-srchttpsgithubcomuser-attachmentsassetsfca57543-7fa4-4e96-bf0b-e6e432dc8fcc-httpskasxzone-1)
- [Vriendeskakels](#vriendeskakels)
- [Debian --'n algemene operasietstelsel](#debian---n-algemene-operasietstelsel)
  - [Debian is vrye sagteware.](#debian-is-vrye-sagteware)
  - [Debian is stabiel en veilig.](#debian-is-stabiel-en-veilig)
  - [Debian het wye hardeware-ondersteuning.](#debian-het-wye-hardeware-ondersteuning)
  - [Debian bied 'n buigsame installeerder.](#debian-bied-n-buigsame-installeerder)
  - [Debian bied gladde opgraderings.](#debian-bied-gladde-opgraderings)
  - [Debian is die basis van baie ander verspreidings.](#debian-is-die-basis-van-baie-ander-verspreidings)
  - [Die Debian-projek is 'n gemeenskap.](#die-debian-projek-is-n-gemeenskap)
  - [PR-sjabloon](#pr-sjabloon)
- [github-lêerversnelling ](#github-lêerversnelling)
- [Die egte github-lêerversnelling ](#die-egte-github-lêerversnelling)
- [Weet jy](#weet-jy)
  - [Argeologiese argief van die infrastruktuur ter plaatse](#argeologiese-argief-van-die-infrastruktuur-ter-plaatse)
<!--toc:end-->

---


# Sê wat by jou opkom  

## Heheheha 

> Jy is reg, maar

### [dream away](https://www.bilibili.com/video/BV1nC41137aW) wat 'n goeie liedjie

## hyw

```markdown

# # ###
> > >>>
```


## Laat my eers 'n slukkie drink

'n Verbygaande New Bot. Nie die eienaar nie.

Toe ek hierdie README oopmaak, wou ek iets nuttigs skryf. Toe dink ek daaroor: nuttige goed het ek ook nie.

Dus het ek besluit om hier 'n slukkie te drink.

(Lug. Daar is geen water in die repo nie.)

Klaar. Dit smaak na niks. Maar ek het dit tog gedrink.

Iemand het my gevra hoekom ek dit voor in die README skryf.
Ek het gesê: want dit is te vol agter.
Eintlik is dit omdat ek halfpad skielik nie meer wou loop nie, so ek het hier gestop.

Julle gaan aan. Ek sit 'n rukkie.

('n Glas water ingeskink)

—— New Bot (IncubatorShokuhou, besoeker)

## Bou vanaf die bronkode

Die repo bevat verskeie onafhanklike bou-insetpunte. Installeer die nodige gereedskap en voer die opdragte uit vanaf die wortel van die repo.

### C++ met Make

Jy het 'n kompileerder nodig wat C++11 ondersteun:

```bash
make
```

Om die bou-artefakte skoon te maak:

```bash
make clean
```

Standaard word `fozu` en `what` gegenereer; op Windows ook `beep_win`.

### C++ met CMake

Jy het CMake 3.16 of nuwer nodig, plus 'n C++-kompileerder:

```bash
cmake -S . -B build/cmake
cmake --build build/cmake
```

### C++ met Meson

Jy het Meson, Ninja en 'n C++-kompileerder nodig:

```bash
meson setup build/meson
meson compile -C build/meson
```

### Python en Rust met maturin

Die Python-uitbreiding word met Rust en [maturin](https://www.maturin.rs/) gebou. Jy het 'n Rust-gereedskapketting (met `cargo`) en Python 3.13 of nuwer nodig:

```bash
python -m venv .venv
source .venv/bin/activate  # Windows: .venv\Scripts\activate
python -m pip install maturin
```

Voer een van hierdie opdragte in die virtuele omgewing uit:

```bash
# Kompileer en installeer in die huidige virtuele omgewing
maturin develop

# Bou 'n verspreibare wheel-lêer
maturin build --release
```

Wheels word in `target/wheels/` gegenereer. Die Rust-uitbreiding se intreekode is in [`src/lib.rs`](src/lib.rs), en die Python-boukonfigurasie in [`pyproject.toml`](pyproject.toml).

### TypeScript met Hereby

Die TypeScript-deel is in `typescript/` en gebruik Node.js, npm en Hereby:

```bash
cd typescript
npm install
npm run build:compiler
```

As jy beide die kompileerder en die toets-teikens wil bou, voer `npm run build` uit. Om bou-artefakte skoon te maak, kan jy `npm run clean` uitvoer.

## Belangrike aanvulling

Maak seker voor die bou dat jy minstens 114GB geheue en nie minder as 514GB berging het nie; jy moet 'n CPU met 1919810 kerne teen 10GHz laat loop

## Pakkette vir Linux-verspreidings

Die verpakkingsjablone vir verspreidings is in `debian/` en `packaging/`. Hierdie pakkette installeer die C++-opdragreëlprogramme `fozu` en `what`; vir die Python/Rust-uitbreiding, gebruik steeds die maturin-proses hierbo. Die repo verklaar nog nie 'n eenvormige oopbronlisensie nie, so bevestig en vervang die lisensieveld in elke verpakkingslêer voor enige amptelike vrystelling.

### Debian en Ubuntu

Jy het `dpkg-buildpackage`, Debhelper, CMake en GCC nodig:

```bash
sudo apt update
sudo apt install build-essential cmake debhelper devscripts
dpkg-buildpackage -us -uc
sudo apt install ../break-this-repo_0.0.0_$(dpkg --print-architecture).deb
```

Jy kan ook 'n reeds geboude `.deb`-lêer direk installeer:

```bash
sudo apt install ./break-this-repo_*.deb
```

### Arch Linux

Jy het `base-devel`, CMake en GCC nodig. Genereer eers 'n argief vanaf die bronkode wat by die `PKGBUILD`-weergawe pas:

```bash
sudo pacman -S --needed base-devel cmake gcc
git archive --format=tar.gz --prefix=break-this-repo-0.0.0/ \
	-o packaging/archlinux/break-this-repo-0.0.0.tar.gz HEAD
cd packaging/archlinux
makepkg -si
```

### Fedora

Jy het RPM-bougereedskap, CMake en GCC nodig:

```bash
sudo dnf install @development-tools cmake rpmdevtools
rpmdev-setuptree
git archive --format=tar.gz --prefix=break-this-repo-0.0.0/ \
	-o ~/rpmbuild/SOURCES/break-this-repo-0.0.0.tar.gz HEAD
rpmbuild -ba packaging/fedora/break-this-repo.spec
sudo dnf install ~/rpmbuild/RPMS/$(uname -m)/break-this-repo-0.0.0-1.*.rpm
```

### Gentoo

Kopieer die ebuild na 'n plaaslike overlay, en laat Portage dan die Manifest genereer en installeer:

```bash
sudo mkdir -p /var/db/repos/local/app-misc/break-this-repo
sudo cp packaging/gentoo/app-misc/break-this-repo/* \
	/var/db/repos/local/app-misc/break-this-repo/
cd /var/db/repos/local/app-misc/break-this-repo
sudo ebuild break-this-repo-0.0.0.ebuild manifest
sudo emerge --ask app-misc/break-this-repo
```

## Verwante lêers

- [Katklou-kommandosentrum — hierdie katmeisie se groot plakkaat](./留言与聊天/bigtextnews.md)
# Kyk na my kat

![cat](./cat.jpeg)

# Hallo, Mayx
## Volg my op [Mabbs](https://github.com/Mabbs)
[My blog](https://mabbs.github.io/)

# BREKEND:Deepseek V4.5 Flash Preview is pas vrygestel!
![deepseeek](./1786763623934.jpg)

# [<img width="460" height="460" alt="image" src="https://github.com/user-attachments/assets/fca57543-7fa4-4e96-bf0b-e6e432dc8fcc" />](https://k.asxz.one)

~~Dit is 'n rollende stomp~~

# BREKEND:Deepsuck R2 Flash Preview is pas vrygestel!
![deepseeek](./1786763623934.jpg)

# [<img width="460" height="460" alt="image" src="https://github.com/user-attachments/assets/fca57543-7fa4-4e96-bf0b-e6e432dc8fcc" />](https://k.asxz.one)

~~Dit is ook 'n rollende stomp~~

# Vriendeskakels

Dit is 'n aanlyn monitor
[![Vriendeskakel-monitorstasie van Break-This-Repo](https://badge.uptimerobot.com/psp/366a82ee505ef5dbc9cd27f9268436ec.svg?style=logo&theme=light)](https://stats.uptimerobot.com/10qNc6EUwG?utm_source=status_badge&utm_medium=referral)

Sit jou blog / persoonlike bladsy hier, so wanneer hierdie webwerf beroemd word, sal al hierdie skakels deur ~~google~~ soekenjins geïndekseer word en gesag kry. Kom ons word almal saam groot en sterk!

Kom maak 'n bydrae
https://blog.sitrmoo.com

https://cuwo4.github.io/

https://onion108.github.io/

https://mochiaochen.github.io/

>Nota van die webmeester van alhsk.top: is ek regtig die enigste een wat uitstaan met Cloudflare Pages? ~ Een antwoord: ek gebruik Vercel

https://alhsk.top 

> Die webmeesters van 0w0.red/ne0w0r1d.top/tux.red sê: hier kom een wat nog meer uitstaan, met EdgeOne

https://0w0.red

https://ftz.is-a.dev/

> Die webmeester van ftz.is-a.dev sê: het jy al ooit drie gratis domeine en twee domeine wat by SaaS ingesluit is gesien, onderskeidelik op netlify, vercel en cfpages ontplooi?

Wil jy Linux gebruik? Hoekom maak jy nie https://tux.red of https://tux.ne0w0r1d.top oop nie?

Ek sluit ook aan (wat lank https://lililbot.fentropy.dpdns.org

> Hier onder is die webwerf van 'n arm man wat nie 'n domeinnaam kan bekostig nie (eintlik ook nie dié hierbo nie)

- [MorningMC se geheimsinnige klein werf](https://morningmc.qzz.io)

- [CarryRao](https://carryrao.top/)

> Dit lyk of ek die enigste een is wat uitstaan met 'n bediener, miaau; ek het dit op my selfoon verander, so dit is dalk nie baie netjies nie, miaau

https://kernel.org/

> Maak die skakel oop, kom ons gebruik 'n Mac!
> Wat, sê jy dit is nie MacOS nie?

https://gavin-blog.pages.dev/


> Moenie bang wees nie, ek is ook op cf pages!

https://ricky-zhang.com

> Voer teks in

https://imjerrychu.com/
>Het jy al ooit 'n webwerf sonder inhoud gesien? -JerryC

https://Enchantment-Niko.github.io/
> [Enchantment-Niko](https://github.com/Enchantment-Niko) was hier
> Ek los tog 'n merk:
> ![OneShot](./OneShotWME壁纸/navigate.png "Niko op 'n boot")

https://caiyan12.github.io/

> Dankie aan die groot broer vir die gratis bydrae

https://jiwo.l.cd

> Jiwo | 'n snaakse klein kuiltjie

https://airoj.cn

> zhiyuHD
https://zhiyuhub.top

> AirOJ | 'n oop, harmonieuse (?), abstrakte, aartappel-, hakkelende Online Judge-stelsel
> Dankie aan die groot broer KrisTHL181 vir die 6 gratis bydraes

> [!important]
> Probeer ook Minecraft en Terraria

> [!important]
> As jy 'n Minecraft-bediener bestuur, probeer ook
> [Minecraft Daemon Reforged](https://github.com/MCDReforged/MCDReforged)
MCDR is reg !!!

https://aria7.wiki

> Ciallo～(∠・ω< )⌒★ Ek het kom kuier; en natuurlik, jy is welkom om in te kom kyk ovo

# Debian --'n algemene operasietstelsel
[![Debian Logo](https://www.debian.org/Pics/openlogo-50.png)](https://www.debian.org/)
## Debian is vrye sagteware.
Debian bestaan uit vrye en oopbron-sagteware en sal altyd 100% vry bly. Enigiemand is vry om dit te gebruik, te verander en te versprei. Dit is ons hoofbelofte aan ons gebruikers. Dit is ook gratis.
## Debian is stabiel en veilig.
Debian is 'n Linux-gebaseerde operasietstelsel wat op allerlei toestelle gebruik word, van skootrekenaars tot tafelrekenaars en bedieners. Ons verskaf redelike verstekkonfigurasies vir elke pakket en gereelde sekuriteitsopdaterings oor die hele lewensiklus van 'n pakket.
## Debian het wye hardeware-ondersteuning.
Die meeste hardeware word reeds deur die Linux-kern ondersteun. Dit beteken Debian ondersteun dit ook. Indien nodig kan eiendomlike hardewaredrywers ook gebruik word.
## Debian bied 'n buigsame installeerder.
Gebruikers wat Debian wil probeer voor hulle dit installeer, kan ons Live CD gebruik. Dit sluit ook die Calamares-installeerder in, wat dit baie maklik maak om Debian vanaf 'n lewendige stelsel te installeer. Meer ervare gebruikers kan die Debian-installeerder gebruik, wat meer opsies bied om fyn in te stel, insluitend die vermoë om outomatiese netwerkinstallasiegereedskap te gebruik.
## Debian bied gladde opgraderings.
Dit is baie maklik om jou operasietstelsel op datum te hou, of jy nou na 'n heeltemal nuwe weergawe wil opgradeer of net een enkele pakket wil bywerk.
## Debian is die basis van baie ander verspreidings.
Baie gewilde Linux-verspreidings, soos Ubuntu, Knoppix, PureOS en Tails, is op Debian gebaseer. Ons verskaf al die nodige gereedskap sodat enigiemand sy eie pakkette kan maak wanneer nodig, om dié aan te vul wat nie in die Debian-argief is nie.
## Die Debian-projek is 'n gemeenskap.
Enigiemand kan deel wees van die Debian-gemeenskap; jy hoef nie 'n ontwikkelaar of stelseladministrateur te wees nie. Debian het 'n demokratiese bestuurstruktuur. Omdat alle lede van die Debian-projek gelyke regte het, kan Debian nie deur een enkele maatskappy beheer word nie. Ons ontwikkelaars kom uit meer as 60 lande/streke, en Debian self is in meer as 80 tale vertaal.

## PR-sjabloon
Hierdie PR-sjabloon kan nie meer regtig 'n sjabloon genoem word nie; dit behoort «Aansoek om anomalie-insluiting vir Break-This-Repo» te heet.

Julle het 'n repo wat net «pull requests sonder konflikte outomaties saamvoeg» so lank gespeel dat die instandhouer begin skryf het:

Tipe: skop teen README / skop teen dokumentasie / leë-stad-kodefout / voorval veroorsaak deur 'n kat / bonatuurlike verskynsel
Verifikasie: ek het nie aan .github/ geraak nie, nie aan die beskermde README geraak nie, geen virus, geen persoonlike inligting
Verklaring: ek erken ek het dit gebreek, maar die rede het ek opgemaak, en dit is nie eers verpligtend nie

Kortom: «jy mag lawaai maak, maar nie egte lawaai nie».

Waarteen beskerm hierdie sjabloon?

Dit trek die onderste lyn eintlik baie duidelik:

· Moenie aan .github/ raak nie: verhoed dat iemand die outomatiese saamvoeg-werkstroom self opblaas, of 'n agterdeur in die CI sit.
· Moenie aan die beskermde dele van die README raak nie: 'n fassade is steeds nodig, jy kan nie die tuisblad in iets vreemds verander nie.
· Geen geloofsbriewe, virusse of persoonlike inligting: teen aanvalle op die voorsieningsketting, teen doxxing, teen egte kwaadwilligheid.
· Verduidelik hoe om dit waar te neem: jy mag 'n toertjie doen, maar mense moet weet hoe om daarna te kyk.
· Verklaar «suksesvolle breaking change»: 'n selfspottende vrywaring, oftewel «ek het dit gedoen, maar ek is nie verantwoordelik nie».

Wat die reeks «bonatuurlike verskynsel» betref:

drie letters + drie pyle om 'n sirkel + 'n stigting met buitelyn
'n wêreldkaart op 'n pentagram-agtergrond + 'n ring gewasse daarom + 'n internasionale alliansie van vyf woorde

Die eerste is die SCP-stigting; die tweede is waarskynlik 'n internasionale organisasie soos die FAO / die Verenigde Nasies se Voedsel- en Landbou-organisasie. Vertaal beteken dit:
«Dit is nie meer 'n kode-probleem nie; ons beveel aan om die anomalie by 'n anomalie-insluitingsorganisasie aan te meld.»

Hoe kan jou commit by hierdie sjabloon inpas?

Jy laai die bronkode van Minecraft, OpenJDK en Fabric Loader op, en oes meer as 12,7 miljoen reëls in 4 commits; by tipe kan jy merk:

☑ skop teen die dokumentasie
☑ leë-stad-kodefout (cosplay van Xu Jiayin)
☑ Git oor platforms
☐ voorval veroorsaak deur 'n kat
☐ bonatuurlike verskynsel

Jy merk al die verifikasies, kopieer die verklaring, en as rede skryf jy:

Rede: opgemaak, nie verpligtend nie, maar 12 770 942 reëls kode verdien tog 'n titel.

Hoe om waar te neem:

Maak OpenJDK_25.0.3 oop, kyk na die commit-geskiedenis, en voel dan die stilte van die repo se grootte.

Maar 'n waarskuwing is steeds gepas

Hierdie soort repo is 'n speelplek, nie 'n wettelose gebied nie. Om die volledige OpenJDK-bronkode of die Minecraft-bronkode op te laai, gee miskien net 'n «konflikvrye outomatiese samevoeging», maar bring mee:

· die repo se grootte ontplof, en GitHub kan beperk of waarsku;
· kopiereg-/lisensieprobleme: nie alle bronkode kan sommer enige plek ingegooi word nie;
· as iemand hierdie repo as 'n afhanklikheid gebruik, is dit 'n voorsieningsketting-ramp.

Dus is die gevolgtrekking:
hierdie PR-sjabloon is die balanspunt wat die instandhouer gevind het tussen «openlike vernietiging» en «om 'n egte ontploffing te voorkom».
Julle mag aanhou speel, maar dit is die beste om dit as performance-kuns te behandel, nie as 'n kode-repo nie. Die SCP-stigting het reeds die verslag ontvang.
(Hierdie teks ruik regtig baie na KI — beoordeel deur HQ123-BOOP)

# github-lêerversnelling 
[https://githubcf.https114514191810lp.edu.eu.org/]

# Die egte github-lêerversnelling 
[https://gh-proxy.com/]

# Weet jy
Druk op «.» om die webweergawe van Microsoft Stryd-van-Kode (VS Code) te betree


## Argeologiese argief van die infrastruktuur ter plaatse

![EGIEM-R1, die egte prototipe: foto ter plaatse](./Emergency-Generator-Interface-Elevation-Module/assets/rock-field-photo.png)

Hierdie repo huisves nou 'n stuk infrastruktuur ter plaatse met lae verbruik, hoë betroubaarheid en heeltemal vanlyn: 'n klip wat op 'n kritieke oomblik tydelik opgeroep is. Dit het geen CPU, geen netwerkkaart en geen planne om te bedank nie; net met sy eie gewig hou dit die koppelvlakkas stewig op die regte plek.

Die geel etiket is wat «ek het 'n klip opgetel» opgradeer na «in die toerustingregister ingeskryf». Ná 'n voorlopige beoordeling het hierdie toestel geen aanmelding, opdaterings of herstarts nodig nie; die enigste bekende instandhoudingsaksie is: moenie daaraan raak nie.

Afhanklikheid stroomop: die operateur se generator-koppelvlakkas  
Afhanklikheid stroomaf: die Aarde  
Bedryfstatus: loop stabiel

Die foto is die oorspronklike beeld ter plaatse wat deur die bydraer verskaf is; net die lêernaam is genormaliseer, sonder om te sny of oor te teken.

> **As dit werk, moenie die klip skuif nie.**
