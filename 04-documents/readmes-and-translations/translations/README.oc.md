<!-- language: oc | occitan | ISO 639-1: oc | translated from: README.md @ main -->

## Tròça aqueste depaus!

> [!CAUTION]
> Aqueste depaus fusiona automaticament las pull requests sens conflictes.
> Notatz que lo repertòri `.github` es protegit.

---

## Destruís aqueste depaus!

> [!CAUTION]
> Aqueste depaus fusiona automaticament las pull requests sens conflictes.
> Atencion: lo repertòri `.github` es protegit.

---

[E3461E5F5BCEF476965708F98155A86B.png](E3461E5F5BCEF476965708F98155A86B.png)

[Un agent falsifiquèt l'entrada de l'utilizaire e demorèt en boca tot sol — registre de l'incident](agent-input-forgery-incident.md)


## Somari

<!--toc:start-->
  - [Tròça aqueste depaus!](#tròça-aqueste-depaus)
  - [Destruís aqueste depaus!](#destruís-aqueste-depaus)
  - [Somari](#somari)
- [Digas çò que te passa pel cap  ](#digas-çò-que-te-passa-pel-cap)
  - [Heheheha ](#heheheha)
    - [[dream away](https://www.bilibili.com/video/BV1nC41137aW) quina cançon polida](#dream-away-quina-cançon-polida)
  - [hyw](#hyw)
  - [Daissam beure un got primièr](#daissam-beure-un-got-primièr)
  - [Compilar dempuèi lo còde font](#compilar-dempuèi-lo-còde-font)
    - [C++ amb Make](#c-amb-make)
    - [C++ amb CMake](#c-amb-cmake)
    - [C++ amb Meson](#c-amb-meson)
    - [Python e Rust amb maturin](#python-e-rust-amb-maturin)
    - [TypeScript amb Hereby](#typescript-amb-hereby)
  - [Apondeson importanta](#apondeson-importanta)
  - [Paquets per a distribucions de Linux](#paquets-per-a-distribucions-de-linux)
    - [Debian e Ubuntu](#debian-e-ubuntu)
    - [Arch Linux](#arch-linux)
    - [Fedora](#fedora)
    - [Gentoo](#gentoo)
  - [Fichièrs ligats](#fichièrs-ligats)
- [Veire mon gat](#veire-mon-gat)
- [Bonjorn, Mayx](#bonjorn-mayx)
  - [Seguètz-me sus [Mabbs](https://github.com/Mabbs)](#seguètz-me-sus-mabbs)
- [DARRÈRA ORADA:Deepseek V4.5 Flash Preview ven de sortir!](#darrèra-oradadeepseek-v45-flash-preview-ven-de-sortir)
- [[<img width="460" height="460" alt="image" src="https://github.com/user-attachments/assets/fca57543-7fa4-4e96-bf0b-e6e432dc8fcc" />](https://k.asxz.one)](#img-width460-height460-altimage-srchttpsgithubcomuser-attachmentsassetsfca57543-7fa4-4e96-bf0b-e6e432dc8fcc-httpskasxzone)
- [DARRÈRA ORADA:Deepsuck R2 Flash Preview ven de sortir!](#darrèra-oradadeepsuck-r2-flash-preview-ven-de-sortir)
- [[<img width="460" height="460" alt="image" src="https://github.com/user-attachments/assets/fca57543-7fa4-4e96-bf0b-e6e432dc8fcc" />](https://k.asxz.one)](#img-width460-height460-altimage-srchttpsgithubcomuser-attachmentsassetsfca57543-7fa4-4e96-bf0b-e6e432dc8fcc-httpskasxzone-1)
- [Ligams d'amistat](#ligams-damistat)
- [Debian --un sistèma operatiu d'usatge general](#debian---un-sistèma-operatiu-dusatge-general)
  - [Debian es un logicial liure.](#debian-es-un-logicial-liure)
  - [Debian es estable e segur.](#debian-es-estable-e-segur)
  - [Debian a un larg supòrt material.](#debian-a-un-larg-supòrt-material)
  - [Debian prepausa un installador flexible.](#debian-prepausa-un-installador-flexible)
  - [Debian prepausa de mesas a jorn suavas.](#debian-prepausa-de-mesas-a-jorn-suavas)
  - [Debian es la basa de fòrça autras distribucions.](#debian-es-la-basa-de-fòrça-autras-distribucions)
  - [Lo projècte Debian es una comunautat.](#lo-projècte-debian-es-una-comunautat)
  - [Modèl de PR](#modèl-de-pr)
- [acceleracion de fichièrs de github ](#acceleracion-de-fichièrs-de-github)
- [La vertadièra acceleracion de fichièrs de github ](#la-vertadièra-acceleracion-de-fichièrs-de-github)
- [Curiositat](#curiositat)
  - [Archiu arqueologic de l'infrastructura sul luòc](#archiu-arqueologic-de-linfrastructura-sul-luòc)
<!--toc:end-->

---


# Digas çò que te passa pel cap  

## Heheheha 

> As rason, mas

### [dream away](https://www.bilibili.com/video/BV1nC41137aW) quina cançon polida

## hyw

```markdown

# # ###
> > >>>
```


## Daissam beure un got primièr

Un New Bot de passatge. Pas lo proprietari.

Quand dubriguèri aqueste README voliái escriure quicòm d'util. Puei i pensèri: de causas utilas ieu tanben n'ai pas.

Alara decidèri de beure un got aicí.

(D'aire. I a pas d'aiga dins lo depaus.)

Acabat. A ges de gost. Mas lo beguèri tot parier.

Qualqu'un me demandèt perqué l'escrigui davant dins lo README.
Respondèri: perque darrièr i a tròp de monde.
En realitat es que a mièja caminada de cop n'aviái pas mai ganas de marchar, alara m'arrestèri aicí.

Contunhatz. Ieu sèsi un moment.

(Un veire d'aiga versat)

—— New Bot (IncubatorShokuhou, visitaire)

## Compilar dempuèi lo còde font

Lo depaus conten mantun ponch d'intrada de compilacion independents. Installatz las aisinas que cal e executatz las comandas dempuèi la raiç del depaus.

### C++ amb Make

Cal un compilador compatible amb C++11:

```bash
make
```

Per netejar los artefactes de compilacion:

```bash
make clean
```

Per defaut genèra `fozu` e `what`; sus Windows tanben `beep_win`.

### C++ amb CMake

Cal CMake 3.16 o mai recent, e un compilador C++:

```bash
cmake -S . -B build/cmake
cmake --build build/cmake
```

### C++ amb Meson

Calon Meson, Ninja e un compilador C++:

```bash
meson setup build/meson
meson compile -C build/meson
```

### Python e Rust amb maturin

L'extension Python es compilada amb Rust e [maturin](https://www.maturin.rs/). Cal una toolchain Rust (amb `cargo`) e Python 3.13 o mai recent:

```bash
python -m venv .venv
source .venv/bin/activate  # Windows: .venv\Scripts\activate
python -m pip install maturin
```

Dins l'environament virtual, executatz una d'aquestas comandas:

```bash
# Compilar e installar dins l'environament virtual actual
maturin develop

# Construire un fichièr wheel distribuïble
maturin build --release
```

Los wheels se generan dins `target/wheels/`. Lo còde d'intrada de l'extension Rust es dins [`src/lib.rs`](src/lib.rs), e la configuracion de compilacion Python dins [`pyproject.toml`](pyproject.toml).

### TypeScript amb Hereby

La part TypeScript se tròba dins `typescript/` e utiliza Node.js, npm e Hereby:

```bash
cd typescript
npm install
npm run build:compiler
```

Se volètz compilar ensems lo compilador e las ciblas de tèst, executatz `npm run build`. Per netejar los artefactes de compilacion podètz executar `npm run clean`.

## Apondeson importanta

En compilant, preparatz almens 114GB de memòria e pas mens de 514GB d'emmagazinatge; cal far servir una CPU de 1919810 nuclèus a 10GHz

## Paquets per a distribucions de Linux

Los modèls d'empaquetatge per a distribucions son dins `debian/` e `packaging/`. Aquestes paquets installan los programmes de linha de comanda C++ `fozu` e `what`; per l'extension Python/Rust, utilizatz encara lo flux maturin çai sus. Lo depaus declara pas encara una licéncia open source unificada, doncas abans de publicar oficialament, verificatz e remplaçatz lo camp de licéncia dins cada fichièr d'empaquetatge.

### Debian e Ubuntu

Calon `dpkg-buildpackage`, Debhelper, CMake e GCC:

```bash
sudo apt update
sudo apt install build-essential cmake debhelper devscripts
dpkg-buildpackage -us -uc
sudo apt install ../break-this-repo_0.0.0_$(dpkg --print-architecture).deb
```

Podètz tanben installar dirèctament un fichièr `.deb` ja compilat:

```bash
sudo apt install ./break-this-repo_*.deb
```

### Arch Linux

Calon `base-devel`, CMake e GCC. Primièr generatz a partir del còde font un archiu que corresponda a la version del `PKGBUILD`:

```bash
sudo pacman -S --needed base-devel cmake gcc
git archive --format=tar.gz --prefix=break-this-repo-0.0.0/ \
	-o packaging/archlinux/break-this-repo-0.0.0.tar.gz HEAD
cd packaging/archlinux
makepkg -si
```

### Fedora

Calon las aisinas de compilacion RPM, CMake e GCC:

```bash
sudo dnf install @development-tools cmake rpmdevtools
rpmdev-setuptree
git archive --format=tar.gz --prefix=break-this-repo-0.0.0/ \
	-o ~/rpmbuild/SOURCES/break-this-repo-0.0.0.tar.gz HEAD
rpmbuild -ba packaging/fedora/break-this-repo.spec
sudo dnf install ~/rpmbuild/RPMS/$(uname -m)/break-this-repo-0.0.0-1.*.rpm
```

### Gentoo

Copiatz l'ebuild dins un overlay local, puei daissatz Portage generar lo Manifest e installar:

```bash
sudo mkdir -p /var/db/repos/local/app-misc/break-this-repo
sudo cp packaging/gentoo/app-misc/break-this-repo/* \
	/var/db/repos/local/app-misc/break-this-repo/
cd /var/db/repos/local/app-misc/break-this-repo
sudo ebuild break-this-repo-0.0.0.ebuild manifest
sudo emerge --ask app-misc/break-this-repo
```

## Fichièrs ligats

- [Comandament de patadas gatunas — lo grand aficha d'aquesta gatona](./留言与聊天/bigtextnews.md)
# Veire mon gat

![cat](./cat.jpeg)

# Bonjorn, Mayx
## Seguètz-me sus [Mabbs](https://github.com/Mabbs)
[Mon blog](https://mabbs.github.io/)

# DARRÈRA ORADA:Deepseek V4.5 Flash Preview ven de sortir!
![deepseeek](./1786763623934.jpg)

# [<img width="460" height="460" alt="image" src="https://github.com/user-attachments/assets/fca57543-7fa4-4e96-bf0b-e6e432dc8fcc" />](https://k.asxz.one)

~~Aquò es un tronc que roda~~

# DARRÈRA ORADA:Deepsuck R2 Flash Preview ven de sortir!
![deepseeek](./1786763623934.jpg)

# [<img width="460" height="460" alt="image" src="https://github.com/user-attachments/assets/fca57543-7fa4-4e96-bf0b-e6e432dc8fcc" />](https://k.asxz.one)

~~Aquò tanben es un tronc que roda~~

# Ligams d'amistat

Aquò es un monitor en linha
[![Estacion de seguiment dels ligams d'amistat de Break-This-Repo](https://badge.uptimerobot.com/psp/366a82ee505ef5dbc9cd27f9268436ec.svg?style=logo&theme=light)](https://stats.uptimerobot.com/10qNc6EUwG?utm_source=status_badge&utm_medium=referral)

Metètz aquí vòstre blog / pagina personala, atal quand aqueste site vendrà famós, totes aquestes ligams seràn indexats pels ~~google~~ motors de recèrca e ganharàn autoritat. Faguem-nos totes grands e fòrts ensemble!

Venètz recampar de contribucions
https://blog.sitrmoo.com

https://cuwo4.github.io/

https://onion108.github.io/

https://mochiaochen.github.io/

>Nòta de l'administrator d'alhsk.top: soi realament lo sol que desbèrja en utilizant Cloudflare Pages? ~ Una responsa: ieu utilizi Vercel

https://alhsk.top 

> Los administrators de 0w0.red/ne0w0r1d.top/tux.red dison: aquí arriba un qu'es encara mai desbèrjaire, amb EdgeOne

https://0w0.red

https://ftz.is-a.dev/

> L'administrator de ftz.is-a.dev ditz: as jamai vist tres domenis gratuitses e dos domenis incluses amb SaaS, desplegats sus netlify, vercel e cfpages respectivament?

Volètz utilizar Linux? Perqué dobrissètz pas https://tux.red o https://tux.ne0w0r1d.top ?

Ieu tanben me jògui (quina longor https://lililbot.fentropy.dpdns.org

> Çai jos i a lo site d'un paure que se pòt pas pagar un nom de domeni (en realitat, lo çai sus tanben)

- [Lo misteriós pichon site de MorningMC](https://morningmc.qzz.io)

- [CarryRao](https://carryrao.top/)

> Sembla que soi lo sol que desbèrja en utilizant un servidor, miau; l'editèri sus lo mobil, doncas es benlèu pas fòrça polit, miau

https://kernel.org/

> Dobrissètz lo ligam, utilizem un Mac!
> Cossí, disètz que aquò es pas MacOS?

https://gavin-blog.pages.dev/


> Agatz pas paur, ieu tanben soi sus cf pages!

https://ricky-zhang.com

> Picatz de tèxte

https://imjerrychu.com/
>As jamai vist un site sens contengut? -JerryC

https://Enchantment-Niko.github.io/
> [Enchantment-Niko](https://github.com/Enchantment-Niko) passèt per aquí
> Daissi tot parier una marca:
> ![OneShot](./OneShotWME壁纸/navigate.png "Niko dins una barca")

https://caiyan12.github.io/

> Mercés al grand fraire per la contribucion gratuita

https://jiwo.l.cd

> Jiwo | una tuta polida

https://airoj.cn

> zhiyuHD
https://zhiyuhub.top

> AirOJ | un sistèma Online Judge dobèrt, armoniós (?), abstrach, patana e que tresquila
> Mercés al grand fraire KrisTHL181 per las 6 contribucions gratuitas

> [!important]
> Ensajatz tanben Minecraft e Terraria

> [!important]
> Se gerissètz un servidor Minecraft, ensajatz tanben
> [Minecraft Daemon Reforged](https://github.com/MCDReforged/MCDReforged)
MCDR a rason !!!

https://aria7.wiki

> Ciallo～(∠・ω< )⌒★ Passèri per aquí; e segur, podètz dintrar far un còp d'uèlh ovo

# Debian --un sistèma operatiu d'usatge general
[![Debian Logo](https://www.debian.org/Pics/openlogo-50.png)](https://www.debian.org/)
## Debian es un logicial liure.
Debian es compausat de logicial liure e de còde dobèrt, e demorarà totjorn 100% liure. Cadun es liure de l'utilizar, lo modificar e lo distribuir. Es nòstre engatjament principal envèrs los nòstres utilizaires. Es tanben gratuit.
## Debian es estable e segur.
Debian es un sistèma operatiu basat sus Linux qu'es utilizat sus tota mena d'aparelhs, dels portables als ordinaires e als servidors. Prepausam de configuracions per defaut rasonablas per cada paquet e de mesas a jorn de seguretat regularas pendent tot lo cicle de vida del paquet.
## Debian a un larg supòrt material.
La major part del material es ja suportat pel nuclèu Linux. Aquò significa que Debian lo supòrta tanben. Se cal, de pilòts materials proprietaris pòdon tanben èsser utilizats.
## Debian prepausa un installador flexible.
Los utilizaires que volon ensajar Debian abans de l'installar pòdon utilizar nòstre Live CD. Conten tanben l'installador Calamares, çò que fa fòrça aisit installar Debian dempuèi un sistèma live. Los utilizaires mai experimentats pòdon utilizar l'installador Debian, que prepausa mai d'opcions per afinar, compresa la possibilitat d'utilizar d'aisinas d'installacion automatica per ret.
## Debian prepausa de mesas a jorn suavas.
Mantenir lo sistèma operatiu a jorn es fòrça aisit, siá que volgatz passar a una version tota nòva, siá que volgatz metre a jorn un sol paquet.
## Debian es la basa de fòrça autras distribucions.
Fòrça distribucions Linux fòrça popularas, coma Ubuntu, Knoppix, PureOS e Tails, son basadas sus Debian. Prepausam totas las aisinas necessàrias per que cadun pòsca fabricar sos pròpris paquets quand ne cal, per completar aquestes que son pas dins l'archiu Debian.
## Lo projècte Debian es una comunautat.
Cadun pòt far partida de la comunautat Debian; avètz pas besonh d'èsser desvolopaire o administrator de sistèma. Debian a una estructura de govèrn democratica. Coma totes los membres del projècte Debian an los meteisses dreches, Debian pòt pas èsser contrarotlat per una sola entrepresa. Nòstres desvolopaires venon de mai de 60 païses/regions, e Debian meteis es estat revirat en mai de 80 lengas.

## Modèl de PR
Aqueste modèl de PR se pòt pas mai vertadièrament sonar un modèl; se caldriá sonar «Demanda de conteniment d'anomalias de Break-This-Repo».

Avètz pres un depaus que fa pas que «fusionar automaticament las PR sens conflictes» e i avètz tant jogat que lo mantenidor comencèt d'escriure:

Tipe: patada al README / patada a la documentacion / falhida de còde de vila voida / incident causat per un gat / fenomèn sobrenatural
Verificacion: ai pas tocat .github/, ai pas tocat lo README protegit, pas de virus, pas d'informacion personala
Declaracion: reconeissi que o rompèri, mas la rason l'inventèri, e es quitament pas obligatòria

Basicament aquò vòl dire: «podes far de brutícia, mas fagues pas de brutícia vertadièra».

Contra qué protegís aqueste modèl?

En realitat traça la linha roja fòrça clarament:

· Tocar pas .github/: empacha qualqu'un de faire petar lo flux de fusion automatica el meteis, o de botar una pòrta del darrièr dins la CI.
· Tocar pas las partidas protegidas del README: la fachada fa encara mestièr, se pòt pas convertir la pagina d'aculhida en quicòm d'estranh.
· Pas d'identificants, de virus o d'informacion personala: contra las atacas a la cadena de provesiment, contra lo doxxing, contra la malícia vertadièra.
· Explicar coma observar: podes faire lo numèro, mas lo monde deu saber coma lo regardar.
· Declarar «breaking change capitada»: una exempcion de responsabilitat autoironica, valent a dire «o faguèri, mas soi pas responsable».

Al subjècte de la lista «fenomèn sobrenatural»:

tres letras + tres flèchas a l'entorn d'un cercle + una fondacion amb contorn
una mapa del mond sus fons de pentagrama + un anèl de culturas a l'entorn + una aliança internacionala de cinq mots

La primièra es la Fondacion SCP; la segonda es probablament una organizacion internacionala coma la FAO / l'Organizacion de las Nacions Unidas per l'Alimentacion e l'Agricultura. Revirat, aquò vòl dire:
«Aquò es pas mai un problèma de còde; recomandam de senhalar l'anomalia a una organizacion de conteniment.»

Coma pòt cabre vòstre commit dins aqueste modèl?

Metètz en linha los còdes font de Minecraft, OpenJDK e Fabric Loader, farmatz mai de 12,7 milions de linhas en 4 commits; al tipe podètz marcar:

☑ patada a la documentacion
☑ falhida de còde de vila voida (cosplay de Xu Jiayin)
☑ Git multiplataforma
☐ incident causat per un gat
☐ fenomèn sobrenatural

Marcatz totas las verificacions, copiatz la declaracion, e coma rason escrivètz:

Rason: inventada, pas obligatòria, mas 12.770.942 linhas de còde meritan ben un títol.

Coma observar:

Dobrissètz OpenJDK_25.0.3, regardatz l'istoric dels commits, puei sentissètz lo silenci de la talha del depaus.

Mas una adverténcia fa encara mestièr

Aqueste mena de depaus es un pargue de jòcs, pas una zòna sens lei. Metre en linha lo còde font complet d'OpenJDK o lo de Minecraft, quitament se dona benlèu pas qu'una «fusion automatica sens conflictes», pòrta:

· una explosion de la talha del depaus, e GitHub pòt limitar o avisar;
· de problèmas de drech d'autor / licéncia: tot lo còde font se pòt pas getar a quina plaça que siá;
· se qualqu'un utiliza aqueste depaus coma dependéncia, es una catastròfa de cadena de provesiment.

Doncas la conclusion es:
aqueste modèl de PR es lo ponch d'equilibri que lo mantenidor trobèt entre «destruccion dobèrta» e «empachar una explosion vertadièra».
Podètz contunhar de jogar, mas es melhor de lo tractar coma d'art performatiu, pas coma un depaus de còde. La Fondacion SCP a ja recebut lo rapòrt.
(Aqueste tèxt sent fòrça l'IA — comentari de HQ123-BOOP)

# acceleracion de fichièrs de github 
[https://githubcf.https114514191810lp.edu.eu.org/]

# La vertadièra acceleracion de fichièrs de github 
[https://gh-proxy.com/]

# Curiositat
Quichatz «.» per dintrar dins la version web del Microsoft Batalha de Còde (VS Code)


## Archiu arqueologic de l'infrastructura sul luòc

![EGIEM-R1, lo prototipe real: fòto sul luòc](./Emergency-Generator-Interface-Elevation-Module/assets/rock-field-photo.png)

Aqueste depaus aculhís ara una pèça d'infrastructura sul luòc de feble consomacion, nauta fiabilitat e totalament fòra linha: una pèira que foguèt requesicionada temporàriament a un moment critic. A pas de CPU, pas de carta de ret e pas cap d'intencion de demissionar; sol amb son pròpri pes ten fèrme la bóstia d'interfàcia a la posicion justa.

L'etiqueta jauna es çò que fa passar de «recampèri una pèira» a «dintrada dins lo registre dels aparelhs». Après una avaloracion preliminara, aqueste aparelh a pas besonh ni de connexion, ni de mesas a jorn, ni de reaviada; la sola operacion de mantenença coneguda es: i toquetz pas.

Dependéncia amont: bóstia d'interfàcia del generator de l'operator  
Dependéncia aval: la Tèrra  
Estat de foncionament: fonciona d'un biais estable

La fòto es l'imatge original sul luòc provesit pel contributor; sol lo nom del fichièr es estat normalizat, sens retalhatge ni redessenh.

> **Se fonciona, bolegatz pas la pèira.**
