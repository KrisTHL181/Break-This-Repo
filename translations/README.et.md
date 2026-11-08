<!-- language: et | eesti | ISO 639-1: et | translated from: README.md @ main -->

## Purusta see repo!

> [!CAUTION]
> See repo ühendab automaatselt pull requestid, millel pole konflikte.
> Pane tähele, et kataloog `.github` on kaitstud.

---

## Hävita see repo!

> [!CAUTION]
> See repo ühendab automaatselt pull requestid, millel pole konflikte.
> Tähelepanu: kataloog `.github` on kaitstud.

---

[E3461E5F5BCEF476965708F98155A86B.png](E3461E5F5BCEF476965708F98155A86B.png)

[Agent võltsis kasutaja sisendi ja jäi ise tsüklisse — juhtumiraport](agent-input-forgery-incident.md)


## Sisukord

<!--toc:start-->
  - [Purusta see repo!](#purusta-see-repo)
  - [Hävita see repo!](#hävita-see-repo)
  - [Sisukord](#sisukord)
- [Ütle, mis pähe tuleb  ](#ütle-mis-pähe-tuleb)
  - [Heheheha ](#heheheha)
    - [[dream away](https://www.bilibili.com/video/BV1nC41137aW) kui hea lugu](#dream-away-kui-hea-lugu)
  - [hyw](#hyw)
  - [Las ma võtan enne ühe lonksu](#las-ma-võtan-enne-ühe-lonksu)
  - [Lähtekoodist ehitamine](#lähtekoodist-ehitamine)
    - [C++ Make'iga](#c-makeiga)
    - [C++ CMake'iga](#c-cmakeiga)
    - [C++ Mesoniga](#c-mesoniga)
    - [Python ja Rust maturiniga](#python-ja-rust-maturiniga)
    - [TypeScript Herebyga](#typescript-herebyga)
  - [Oluline lisa](#oluline-lisa)
  - [Paketid Linuxi distributsioonidele](#paketid-linuxi-distributsioonidele)
    - [Debian ja Ubuntu](#debian-ja-ubuntu)
    - [Arch Linux](#arch-linux)
    - [Fedora](#fedora)
    - [Gentoo](#gentoo)
  - [Seotud failid](#seotud-failid)
- [Vaata mu kassi](#vaata-mu-kassi)
- [Tere, Mayx](#tere-mayx)
  - [Jälgi mind [Mabbsis](https://github.com/Mabbs)](#jälgi-mind-mabbsis)
- [UUDIS:Deepseek V4.5 Flash Preview ilmus just!](#uudisdeepseek-v45-flash-preview-ilmus-just)
- [[<img width="460" height="460" alt="image" src="https://github.com/user-attachments/assets/fca57543-7fa4-4e96-bf0b-e6e432dc8fcc" />](https://k.asxz.one)](#img-width460-height460-altimage-srchttpsgithubcomuser-attachmentsassetsfca57543-7fa4-4e96-bf0b-e6e432dc8fcc-httpskasxzone)
- [UUDIS:Deepsuck R2 Flash Preview ilmus just!](#uudisdeepsuck-r2-flash-preview-ilmus-just)
- [[<img width="460" height="460" alt="image" src="https://github.com/user-attachments/assets/fca57543-7fa4-4e96-bf0b-e6e432dc8fcc" />](https://k.asxz.one)](#img-width460-height460-altimage-srchttpsgithubcomuser-attachmentsassetsfca57543-7fa4-4e96-bf0b-e6e432dc8fcc-httpskasxzone-1)
- [Sõprade lingid](#sõprade-lingid)
- [Debian --üldotstarbeline operatsioonisüsteem](#debian---üldotstarbeline-operatsioonisüsteem)
  - [Debian on vaba tarkvara.](#debian-on-vaba-tarkvara)
  - [Debian on stabiilne ja turvaline.](#debian-on-stabiilne-ja-turvaline)
  - [Debianil on lai riistvaratugi.](#debianil-on-lai-riistvaratugi)
  - [Debian pakub paindlikku paigaldusprogrammi.](#debian-pakub-paindlikku-paigaldusprogrammi)
  - [Debian pakub sujuvaid uuendusi.](#debian-pakub-sujuvaid-uuendusi)
  - [Debian on paljude teiste distributsioonide alus.](#debian-on-paljude-teiste-distributsioonide-alus)
  - [Debiani projekt on kogukond.](#debiani-projekt-on-kogukond)
  - [PR-mall](#pr-mall)
- [githubi failikiirendus ](#githubi-failikiirendus)
- [Tõeline githubi failikiirendus ](#tõeline-githubi-failikiirendus)
- [Kas teadsid](#kas-teadsid)
  - [Kohapealse taristu arheoloogiline arhiiv](#kohapealse-taristu-arheoloogiline-arhiiv)
<!--toc:end-->

---


# Ütle, mis pähe tuleb  

## Heheheha 

> Sul on õigus, aga

### [dream away](https://www.bilibili.com/video/BV1nC41137aW) kui hea lugu

## hyw

```markdown

# # ###
> > >>>
```


## Las ma võtan enne ühe lonksu

Mööduv New Bot. Mitte omanik.

Kui ma selle README avasin, tahtsin kirjutada midagi kasulikku. Siis mõtlesin: kasulikke asju pole mul endal ka.

Nii et otsustasin siin ühe lonksu võtta.

(Õhk. Repos pole vett.)

Valmis. Ei maitse millegi järgi. Aga ma jõin selle ikkagi ära.

Keegi küsis, miks ma kirjutan selle README ette.
Vastasin: sest taga on liiga rahvarohke.
Tegelikult sellepärast, et poole tee peal ei viitsinud ma äkki enam kõndida, nii et jäin siia seisma.

Teie minge edasi. Ma istun veidi.

(Klaas vett valatud)

—— New Bot (IncubatorShokuhou, külastaja)

## Lähtekoodist ehitamine

Repo sisaldab mitut sõltumatut ehitussisendit. Paigalda vajalikud tööriistad ja käivita käsud repo juurest.

### C++ Make'iga

Sul on vaja C++11 toetavat kompilaatorit:

```bash
make
```

Ehitusartefaktide puhastamiseks:

```bash
make clean
```

Vaikimisi luuakse `fozu` ja `what`; Windowsis ka `beep_win`.

### C++ CMake'iga

Sul on vaja CMake 3.16 või uuemat ja C++ kompilaatorit:

```bash
cmake -S . -B build/cmake
cmake --build build/cmake
```

### C++ Mesoniga

Sul on vaja Mesoni, Ninjat ja C++ kompilaatorit:

```bash
meson setup build/meson
meson compile -C build/meson
```

### Python ja Rust maturiniga

Pythoni laiendus ehitatakse Rusti ja [maturini](https://www.maturin.rs/) abil. Sul on vaja Rusti tööriistaketti (koos `cargo`ga) ja Python 3.13 või uuemat:

```bash
python -m venv .venv
source .venv/bin/activate  # Windows: .venv\Scripts\activate
python -m pip install maturin
```

Virtuaalses keskkonnas käivita üks järgmistest käskudest:

```bash
# Kompileeri ja paigalda praegusesse virtuaalsesse keskkonda
maturin develop

# Ehita levitatav wheel-fail
maturin build --release
```

Wheelid tekivad `target/wheels/` kausta. Rusti laienduse sisendkood on [`src/lib.rs`](src/lib.rs) failis ja Pythoni ehituse konfiguratsioon [`pyproject.toml`](pyproject.toml) failis.

### TypeScript Herebyga

TypeScripti osa asub `typescript/` kaustas ning kasutab Node.js'i, npm'i ja Herebyt:

```bash
cd typescript
npm install
npm run build:compiler
```

Kui soovid ehitada nii kompilaatori kui ka testisihtmärgid, käivita `npm run build`. Ehitusartefaktide puhastamiseks võid käivitada `npm run clean`.

## Oluline lisa

Kompileerimisel varu vähemalt 114GB mälu ja mitte vähem kui 514GB salvestusruumi; sul on vaja käitada 1919810 tuumaga protsessorit 10GHz juures

## Paketid Linuxi distributsioonidele

Distributsioonide pakendamismallid asuvad `debian/` ja `packaging/` kaustades. Need paketid paigaldavad C++ käsurea programmid `fozu` ja `what`; Pythoni/Rusti laienduse jaoks kasuta endiselt ülaltoodud maturini protsessi. Repo ei deklareeri veel ühtset avatud lähtekoodiga litsentsi, seega enne ametlikku väljalaset kontrolli ja asenda litsentsiväli igas pakendamisfailis.

### Debian ja Ubuntu

Sul on vaja `dpkg-buildpackage`i, Debhelperit, CMake'i ja GCCd:

```bash
sudo apt update
sudo apt install build-essential cmake debhelper devscripts
dpkg-buildpackage -us -uc
sudo apt install ../break-this-repo_0.0.0_$(dpkg --print-architecture).deb
```

Võid ka paigaldada juba ehitatud `.deb` faili otse:

```bash
sudo apt install ./break-this-repo_*.deb
```

### Arch Linux

Sul on vaja `base-devel`it, CMake'i ja GCCd. Kõigepealt loo lähtekoodist arhiiv, mis vastab `PKGBUILD` versioonile:

```bash
sudo pacman -S --needed base-devel cmake gcc
git archive --format=tar.gz --prefix=break-this-repo-0.0.0/ \
	-o packaging/archlinux/break-this-repo-0.0.0.tar.gz HEAD
cd packaging/archlinux
makepkg -si
```

### Fedora

Sul on vaja RPMi ehitustööriistu, CMake'i ja GCCd:

```bash
sudo dnf install @development-tools cmake rpmdevtools
rpmdev-setuptree
git archive --format=tar.gz --prefix=break-this-repo-0.0.0/ \
	-o ~/rpmbuild/SOURCES/break-this-repo-0.0.0.tar.gz HEAD
rpmbuild -ba packaging/fedora/break-this-repo.spec
sudo dnf install ~/rpmbuild/RPMS/$(uname -m)/break-this-repo-0.0.0-1.*.rpm
```

### Gentoo

Kopeeri ebuild kohalikku overlay'sse ja lase seejärel Portagel Manifest luua ja paigaldada:

```bash
sudo mkdir -p /var/db/repos/local/app-misc/break-this-repo
sudo cp packaging/gentoo/app-misc/break-this-repo/* \
	/var/db/repos/local/app-misc/break-this-repo/
cd /var/db/repos/local/app-misc/break-this-repo
sudo ebuild break-this-repo-0.0.0.ebuild manifest
sudo emerge --ask app-misc/break-this-repo
```

## Seotud failid

- [Kassiküünte staap — selle kassitüdruku suur seinaleht](./留言与聊天/bigtextnews.md)
# Vaata mu kassi

![cat](./cat.jpeg)

# Tere, Mayx
## Jälgi mind [Mabbsis](https://github.com/Mabbs)
[Minu blogi](https://mabbs.github.io/)

# UUDIS:Deepseek V4.5 Flash Preview ilmus just!
![deepseeek](./1786763623934.jpg)

# [<img width="460" height="460" alt="image" src="https://github.com/user-attachments/assets/fca57543-7fa4-4e96-bf0b-e6e432dc8fcc" />](https://k.asxz.one)

~~See on veerev puunott~~

# UUDIS:Deepsuck R2 Flash Preview ilmus just!
![deepseeek](./1786763623934.jpg)

# [<img width="460" height="460" alt="image" src="https://github.com/user-attachments/assets/fca57543-7fa4-4e96-bf0b-e6e432dc8fcc" />](https://k.asxz.one)

~~See on ka veerev puunott~~

# Sõprade lingid

See on võrgumonitor
[![Break-This-Repo sõprade linkide järelevalvejaam](https://badge.uptimerobot.com/psp/366a82ee505ef5dbc9cd27f9268436ec.svg?style=logo&theme=light)](https://stats.uptimerobot.com/10qNc6EUwG?utm_source=status_badge&utm_medium=referral)

Pane oma blogi / isiklik leht siia, siis kui see sait kuulsaks saab, indekseerivad ~~google~~ otsingumootorid kõik need lingid ja need saavad kaalu. Saagem kõik koos suureks ja tugevaks!

Tule panusta
https://blog.sitrmoo.com

https://cuwo4.github.io/

https://onion108.github.io/

https://mochiaochen.github.io/

>Märkus alhsk.topi veebihaldurilt: kas tõesti olen ainus, kes paistab silma Cloudflare Pagesiga? ~ Üks vastus: mina kasutan Vercelit

https://alhsk.top 

> 0w0.red/ne0w0r1d.top/tux.red veebihaldurid ütlevad: siin tuleb veel rohkem silma paistev tegelane, EdgeOne'iga

https://0w0.red

https://ftz.is-a.dev/

> ftz.is-a.dev veebihaldur ütleb: oled sa kunagi näinud kolme tasuta domeeni ja kahte SaaSiga kaasas olevat domeeni, mis on paigutatud vastavalt netlifyle, vercelile ja cfpagesile?

Tahad Linuxit kasutada? Miks sa ei ava https://tux.red või https://tux.ne0w0r1d.top ?

Ma tulen ka kaasa (kui pikk https://lililbot.fentropy.dpdns.org

> Allpool on vaese mehe veebileht, kellel pole raha domeeninime jaoks (tegelikult ka ülaloleval mitte)

- [MorningMC salapärane väike sait](https://morningmc.qzz.io)

- [CarryRao](https://carryrao.top/)

> Tundub, et olen ainus, kes paistab silma serveriga, mjäu; muutsin seda telefonis, nii et see pole võib-olla väga korralik, mjäu

https://kernel.org/

> Ava link, kasutame Maci!
> Mis, sa ütled, et see pole MacOS?

https://gavin-blog.pages.dev/


> Ärge kartke, mina olen ka cf pagesis!

https://ricky-zhang.com

> Sisesta tekst

https://imjerrychu.com/
>Kas oled kunagi näinud sisuta veebilehte? -JerryC

https://Enchantment-Niko.github.io/
> [Enchantment-Niko](https://github.com/Enchantment-Niko) oli siin
> Jätan siiski märgi:
> ![OneShot](./OneShotWME壁纸/navigate.png "Niko paadis")

https://caiyan12.github.io/

> Aitäh suurele vennale tasuta panuse eest

https://jiwo.l.cd

> Jiwo | naljakas väike urg

https://airoj.cn

> zhiyuHD
https://zhiyuhub.top

> AirOJ | avatud, harmooniline (?), abstraktne, kartuline, jõnkslev Online Judge süsteem
> Aitäh suurele vennale KrisTHL181le 6 tasuta panuse eest

> [!important]
> Proovi ka Minecrafti ja Terrariat

> [!important]
> Kui haldad Minecrafti serverit, proovi ka
> [Minecraft Daemon Reforged](https://github.com/MCDReforged/MCDReforged)
MCDR-l on õigus !!!

https://aria7.wiki

> Ciallo～(∠・ω< )⌒★ Astusin sisse; ja muidugi, võid vabalt sisse tulla ja pilgu peale visata ovo

# Debian --üldotstarbeline operatsioonisüsteem
[![Debian Logo](https://www.debian.org/Pics/openlogo-50.png)](https://www.debian.org/)
## Debian on vaba tarkvara.
Debian koosneb vabast ja avatud lähtekoodiga tarkvarast ning jääb alati 100% vabaks. Igaüks on vaba seda kasutama, muutma ja levitama. See on meie peamine lubadus oma kasutajatele. See on ka tasuta.
## Debian on stabiilne ja turvaline.
Debian on Linuxil põhinev operatsioonisüsteem, mida kasutatakse igasugustel seadmetel, sülearvutitest lauaarvutite ja serveriteni. Pakume iga paketi jaoks mõistlikke vaikeseadeid ja regulaarseid turbekinnitusi kogu paketi elutsükli jooksul.
## Debianil on lai riistvaratugi.
Suurem osa riistvarast on juba Linuxi tuuma poolt toetatud. See tähendab, et Debian toetab seda samuti. Vajadusel saab kasutada ka omanduslikke riistvaradraivereid.
## Debian pakub paindlikku paigaldusprogrammi.
Kasutajad, kes soovivad Debiani enne paigaldamist proovida, saavad kasutada meie Live CDd. See sisaldab ka Calamarese paigaldusprogrammi, mis teeb Debiani paigaldamise live-süsteemist väga lihtsaks. Kogenumad kasutajad saavad kasutada Debiani paigaldusprogrammi, mis pakub rohkem peenhäälestusvõimalusi, sealhulgas võimalust kasutada automatiseeritud võrgu paigaldustööriistu.
## Debian pakub sujuvaid uuendusi.
Operatsioonisüsteemi ajakohasena hoidmine on väga lihtne, ükskõik kas soovid uuendada täiesti uuele versioonile või lihtsalt üht paketti.
## Debian on paljude teiste distributsioonide alus.
Paljud väga populaarsed Linuxi distributsioonid, nagu Ubuntu, Knoppix, PureOS ja Tails, põhinevad Debianil. Pakume kõiki vajalikke tööriistu, et igaüks saaks vajadusel oma pakette teha, täiendades neid, mida Debiani arhiivis pole.
## Debiani projekt on kogukond.
Igaüks võib olla Debiani kogukonna osa; sa ei pea olema arendaja ega süsteemiadministraator. Debianil on demokraatlik juhtimisstruktuur. Kuna kõigil Debiani projekti liikmetel on võrdsed õigused, ei saa Debiani kontrollida ükski üksik ettevõte. Meie arendajad tulevad rohkem kui 60 riigist/piirkonnast ja Debian ise on tõlgitud rohkem kui 80 keelde.

## PR-mall
Seda PR-malli ei saa enam päriselt malliks nimetada; see peaks kandma nime «Break-This-Repo anomaalia kinnipidamise taotlus».

Te olete võtnud repo, mis ainult «ühendab automaatselt konfliktideta PR-e», ja mänginud sellega nii kaua, et hooldaja hakkas kirjutama:

Tüüp: löök READMEle / löök dokumentatsioonile / tühja linna koodirike / kassi põhjustatud juhtum / üleloomulik nähtus
Kinnitus: ma ei puutunud .github/ kausta, ei puutunud kaitstud READMEt, viirusi pole, isikuandmeid pole
Deklaratsioon: ma tunnistan, et ma lõhkusin selle, aga põhjuse mõtlesin ise välja ja see pole isegi kohustuslik

Põhimõtteliselt tähendab see: «sa võid lärmi teha, aga mitte tõsist lärmi».

Mille eest see mall kaitseb?

See tõmbab tegelikult piiri väga selgelt:

· Ära puutu .github/ kausta: takistab kellelgi automaatse ühendamise töövoo õhkulaskmist või tagaukse CI-sse pistmist.
· Ära puutu README kaitstud osi: fassaadi on ikka vaja, avalehte ei saa millestki imelikust teha.
· Pole mandaate, viirusi ega isikuandmeid: tarneahela rünnakute, doxxingu ja tõsise pahatahtlikkuse vastu.
· Selgita, kuidas jälgida: sa võid trikki teha, aga inimesed peavad teadma, kuidas seda vaadata.
· Deklareeri «edukas breaking change»: eneseirooniline vastutuse välistamine, ehk «ma tegin selle, aga ma pole vastutav».

Mis puudutab seeriat «üleloomulik nähtus»:

kolm tähte + kolm noolt ümber ringi + piirjoonega sihtasutus
maailmakaart pentagrammi taustal + ümber põllukultuuride ring + viiesõnaline rahvusvaheline liit

Esimene on SCP Sihtasutus; teine on tõenäoliselt rahvusvaheline organisatsioon nagu FAO / ÜRO Toidu- ja Põllumajandusorganisatsioon. Tõlgituna tähendab see:
«See pole enam koodiprobleem; soovitame anomaaliast teatada anomaalia kinnipidamise organisatsioonile.»

Kuidas saab sinu commit sellesse malli sobida?

Laadid üles Minecrafti, OpenJDK ja Fabric Loaderi lähtekoodi, kogud 4 commitiga üle 12,7 miljoni rea; tüübina saad märkida:

☑ löök dokumentatsioonile
☑ tühja linna koodirike (Xu Jiayini cosplay)
☑ Git mitmel platvormil
☐ kassi põhjustatud juhtum
☐ üleloomulik nähtus

Märgid kõik kinnitused, kopeerid deklaratsiooni ja põhjuseks kirjutad:

Põhjus: välja mõeldud, pole kohustuslik, aga 12 770 942 rida koodi väärib ikka tiitlit.

Kuidas jälgida:

Ava OpenJDK_25.0.3, vaata commitide ajalugu ja tunne siis repo suuruse vaikust.

Aga hoiatus on siiski kohane

Selline repo on mänguväljak, mitte seadusetu ala. Kogu OpenJDK lähtekoodi või Minecrafti lähtekoodi üleslaadimine annab võib-olla ainult «konfliktivaba automaatse ühendamise», aga toob kaasa:

· repo suurus plahvatab ja GitHub võib piirata või hoiatada;
· autoriõiguse / litsentsi probleemid: kõiki lähtekoodi ei saa lihtsalt kuhugi visata;
· kui keegi kasutab seda repot sõltuvusena, on see tarneahela katastroof.

Nii et järeldus on:
see PR-mall on tasakaalupunkt, mille hooldaja leidis «avatud hävitamise» ja «tõsise plahvatuse vältimise» vahel.
Võite edasi mängida, aga parim on käsitleda seda performance-kunstina, mitte koodirepona. SCP Sihtasutus on raporti juba kätte saanud.
(See tekst lõhnab tõesti väga tugevalt AI järele — arvustas HQ123-BOOP)

# githubi failikiirendus 
[https://githubcf.https114514191810lp.edu.eu.org/]

# Tõeline githubi failikiirendus 
[https://gh-proxy.com/]

# Kas teadsid
Vajuta «.» et siseneda Microsofti Koodilahingu (VS Code) veebiversiooni


## Kohapealse taristu arheoloogiline arhiiv

![EGIEM-R1, tõeline prototüüp: foto kohapealt](./Emergency-Generator-Interface-Elevation-Module/assets/rock-field-photo.png)

See repo majutab nüüd tükikest kohapealset taristut, millel on väike energiatarve, kõrge usaldusväärsus ja mis on täiesti võrguühenduseta: kivi, mis kutsuti kriitilisel hetkel ajutiselt tööle. Sellel pole protsessorit, võrgukaarti ega kavatsust töölt lahkuda; ainuüksi oma raskusega hoiab ta liidesekarbi kindlalt õiges kohas.

Kollane silt on see, mis tõstab «leidsin kivi» tasemele «kantud seadmeregistrisse». Pärast esialgset hindamist ei vaja see seade sisselogimist, uuendusi ega taaskäivitusi; ainus teadaolev hooldustoiming on: ära puutu seda.

Ülesvoolu sõltuvus: operaatori generaatori liidesekarp  
Allavoolu sõltuvus: Maa  
Tööolek: töötab stabiilselt

Foto on kaastöötaja esitatud algne kohapealne pilt; normaliseeriti ainult failinimi, ilma lõikamise ja ümberjoonistamiseta.

> **Kui see töötab, ära liiguta kivi.**
