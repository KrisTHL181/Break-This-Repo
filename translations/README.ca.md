<!-- language: ca | Català | ISO 639-1: ca | translated from: README.md @ main -->

## Trenca aquest repositori!

> [!CAUTION]
> Aquest repositori fusiona automàticament les pull requests sense conflictes.
> Tingues en compte que el directori `.github` està protegit.

---

## Destrueix aquest repositori!

> [!CAUTION]
> Aquest repositori fusiona automàticament les pull requests sense conflictes.
> Atenció: el directori `.github` està protegit.

---

[E3461E5F5BCEF476965708F98155A86B.png](E3461E5F5BCEF476965708F98155A86B.png)

[Un agent va falsificar l'entrada de l'usuari i va quedar en bucle tot sol — registre de l'incident](agent-input-forgery-incident.md)


## Índex

<!--toc:start-->
  - [Trenca aquest repositori!](#trenca-aquest-repositori)
  - [Destrueix aquest repositori!](#destrueix-aquest-repositori)
  - [Índex](#índex)
- [Digues el que et passi pel cap  ](#digues-el-que-et-passi-pel-cap)
  - [Heheheha ](#heheheha)
    - [[dream away](https://www.bilibili.com/video/BV1nC41137aW) quina cançó més bona](#dream-away-quina-cançó-més-bona)
  - [hyw](#hyw)
  - [Deixa'm fer un glop primer](#deixam-fer-un-glop-primer)
  - [Compilar des del codi font](#compilar-des-del-codi-font)
    - [C++ amb Make](#c-amb-make)
    - [C++ amb CMake](#c-amb-cmake)
    - [C++ amb Meson](#c-amb-meson)
    - [Python i Rust amb maturin](#python-i-rust-amb-maturin)
    - [TypeScript amb Hereby](#typescript-amb-hereby)
  - [Afegit important](#afegit-important)
  - [Paquets per a distribucions de Linux](#paquets-per-a-distribucions-de-linux)
    - [Debian i Ubuntu](#debian-i-ubuntu)
    - [Arch Linux](#arch-linux)
    - [Fedora](#fedora)
    - [Gentoo](#gentoo)
  - [Fitxers relacionats](#fitxers-relacionats)
- [Mira el meu gat](#mira-el-meu-gat)
- [Hola, Mayx](#hola-mayx)
  - [Segueix-me a [Mabbs](https://github.com/Mabbs)](#segueix-me-a-mabbs)
- [DARRERA HORA:Deepseek V4.5 Flash Preview acaba de sortir!](#darrera-horadeepseek-v45-flash-preview-acaba-de-sortir)
- [[<img width="460" height="460" alt="image" src="https://github.com/user-attachments/assets/fca57543-7fa4-4e96-bf0b-e6e432dc8fcc" />](https://k.asxz.one)](#img-width460-height460-altimage-srchttpsgithubcomuser-attachmentsassetsfca57543-7fa4-4e96-bf0b-e6e432dc8fcc-httpskasxzone)
- [DARRERA HORA:Deepsuck R2 Flash Preview acaba de sortir!](#darrera-horadeepsuck-r2-flash-preview-acaba-de-sortir)
- [[<img width="460" height="460" alt="image" src="https://github.com/user-attachments/assets/fca57543-7fa4-4e96-bf0b-e6e432dc8fcc" />](https://k.asxz.one)](#img-width460-height460-altimage-srchttpsgithubcomuser-attachmentsassetsfca57543-7fa4-4e96-bf0b-e6e432dc8fcc-httpskasxzone-1)
- [Enllaços d'amics](#enllaços-damics)
- [Debian --un sistema operatiu de propòsit general](#debian---un-sistema-operatiu-de-propòsit-general)
  - [Debian és programari lliure.](#debian-és-programari-lliure)
  - [Debian és estable i segur.](#debian-és-estable-i-segur)
  - [Debian té un ampli suport de maquinari.](#debian-té-un-ampli-suport-de-maquinari)
  - [Debian ofereix un instal·lador flexible.](#debian-ofereix-un-installador-flexible)
  - [Debian ofereix actualitzacions sense ensurts.](#debian-ofereix-actualitzacions-sense-ensurts)
  - [Debian és la base de moltes altres distribucions.](#debian-és-la-base-de-moltes-altres-distribucions)
  - [El projecte Debian és una comunitat.](#el-projecte-debian-és-una-comunitat)
  - [Plantilla de PR](#plantilla-de-pr)
- [acceleració de fitxers de github ](#acceleració-de-fitxers-de-github)
- [L'autèntica acceleració de fitxers de github ](#lautèntica-acceleració-de-fitxers-de-github)
- [Curiositat](#curiositat)
  - [Arxiu arqueològic de la infraestructura in situ](#arxiu-arqueològic-de-la-infraestructura-in-situ)
<!--toc:end-->

---


# Digues el que et passi pel cap  

## Heheheha 

> Tens raó, però

### [dream away](https://www.bilibili.com/video/BV1nC41137aW) quina cançó més bona

## hyw

```markdown

# # ###
> > >>>
```


## Deixa'm fer un glop primer

Un New Bot de pas. No és l'amo.

Quan vaig obrir aquest README volia escriure una cosa útil. Després hi vaig pensar: coses útils jo tampoc no en tinc.

Així que vaig decidir fer un glop aquí.

(Aire. Al repositori no hi ha aigua.)

Ja està. No té cap gust. Però me'l vaig beure igual.

Algú em va preguntar per què ho escric al davant del README.
Vaig dir: perquè al darrere hi ha massa gent.
En realitat és que a mig camí de cop no em venia de gust caminar més, així que em vaig aturar aquí.

Vosaltres continueu. Jo sec una estona.

(Un got d'aigua abocat)

—— New Bot (IncubatorShokuhou, visitant)

## Compilar des del codi font

El repositori conté diverses entrades de compilació independents. Instal·la les eines que calguin i executa les ordres des de l'arrel del repositori.

### C++ amb Make

Cal un compilador compatible amb C++11:

```bash
make
```

Per netejar els artefactes de compilació:

```bash
make clean
```

Per defecte genera `fozu` i `what`; a Windows també genera `beep_win`.

### C++ amb CMake

Cal CMake 3.16 o superior, i un compilador de C++:

```bash
cmake -S . -B build/cmake
cmake --build build/cmake
```

### C++ amb Meson

Calen Meson, Ninja i un compilador de C++:

```bash
meson setup build/meson
meson compile -C build/meson
```

### Python i Rust amb maturin

L'extensió de Python es compila amb Rust i [maturin](https://www.maturin.rs/). Cal una toolchain de Rust (amb `cargo`) i Python 3.13 o superior:

```bash
python -m venv .venv
source .venv/bin/activate  # Windows: .venv\Scripts\activate
python -m pip install maturin
```

Dins de l'entorn virtual, executa qualsevol d'aquestes ordres:

```bash
# Compila i instal·la a l'entorn virtual actual
maturin develop

# Construeix un fitxer wheel distribuïble
maturin build --release
```

Els wheels es generen a `target/wheels/`. El codi d'entrada de l'extensió de Rust és a [`src/lib.rs`](src/lib.rs), i la configuració de compilació de Python a [`pyproject.toml`](pyproject.toml).

### TypeScript amb Hereby

La part de TypeScript és a `typescript/` i fa servir Node.js, npm i Hereby:

```bash
cd typescript
npm install
npm run build:compiler
```

Si vols compilar alhora el compilador i els objectius de prova, executa `npm run build`. Per netejar els artefactes de compilació pots executar `npm run clean`.

## Afegit important

En compilar, prepara com a mínim 114GB de memòria i no menys de 514GB d'emmagatzematge; cal fer servir una CPU de 1919810 nuclis a 10GHz

## Paquets per a distribucions de Linux

Les plantilles d'empaquetatge per a distribucions són a `debian/` i `packaging/`. Aquests paquets instal·len els programes de línia d'ordres de C++ `fozu` i `what`; per a l'extensió de Python/Rust, fes servir encara el flux de maturin de dalt. El repositori encara no declara una llicència de codi obert unificada, així que abans de publicar oficialment, comprova i substitueix el camp de llicència de cada fitxer d'empaquetatge.

### Debian i Ubuntu

Calen `dpkg-buildpackage`, Debhelper, CMake i GCC:

```bash
sudo apt update
sudo apt install build-essential cmake debhelper devscripts
dpkg-buildpackage -us -uc
sudo apt install ../break-this-repo_0.0.0_$(dpkg --print-architecture).deb
```

També pots instal·lar directament un fitxer `.deb` ja compilat:

```bash
sudo apt install ./break-this-repo_*.deb
```

### Arch Linux

Calen `base-devel`, CMake i GCC. Primer genera a partir del codi font un arxiu que coincideixi amb la versió del `PKGBUILD`:

```bash
sudo pacman -S --needed base-devel cmake gcc
git archive --format=tar.gz --prefix=break-this-repo-0.0.0/ \
	-o packaging/archlinux/break-this-repo-0.0.0.tar.gz HEAD
cd packaging/archlinux
makepkg -si
```

### Fedora

Calen les eines de compilació d'RPM, CMake i GCC:

```bash
sudo dnf install @development-tools cmake rpmdevtools
rpmdev-setuptree
git archive --format=tar.gz --prefix=break-this-repo-0.0.0/ \
	-o ~/rpmbuild/SOURCES/break-this-repo-0.0.0.tar.gz HEAD
rpmbuild -ba packaging/fedora/break-this-repo.spec
sudo dnf install ~/rpmbuild/RPMS/$(uname -m)/break-this-repo-0.0.0-1.*.rpm
```

### Gentoo

Copia l'ebuild en un overlay local i després deixa que Portage generi el Manifest i instal·li:

```bash
sudo mkdir -p /var/db/repos/local/app-misc/break-this-repo
sudo cp packaging/gentoo/app-misc/break-this-repo/* \
	/var/db/repos/local/app-misc/break-this-repo/
cd /var/db/repos/local/app-misc/break-this-repo
sudo ebuild break-this-repo-0.0.0.ebuild manifest
sudo emerge --ask app-misc/break-this-repo
```

## Fitxers relacionats

- [Comandament d'urpes gatunes — el cartell gros d'aquesta noia gat](./留言与聊天/bigtextnews.md)
# Mira el meu gat

![cat](./cat.jpeg)

# Hola, Mayx
## Segueix-me a [Mabbs](https://github.com/Mabbs)
[El meu blog](https://mabbs.github.io/)

# DARRERA HORA:Deepseek V4.5 Flash Preview acaba de sortir!
![deepseeek](./1786763623934.jpg)

# [<img width="460" height="460" alt="image" src="https://github.com/user-attachments/assets/fca57543-7fa4-4e96-bf0b-e6e432dc8fcc" />](https://k.asxz.one)

~~Això és un tronc que rodola~~

# DARRERA HORA:Deepsuck R2 Flash Preview acaba de sortir!
![deepseeek](./1786763623934.jpg)

# [<img width="460" height="460" alt="image" src="https://github.com/user-attachments/assets/fca57543-7fa4-4e96-bf0b-e6e432dc8fcc" />](https://k.asxz.one)

~~Això també és un tronc que rodola~~

# Enllaços d'amics

Això és un monitor en línia
[![Estació de seguiment d'enllaços d'amics de Break-This-Repo](https://badge.uptimerobot.com/psp/366a82ee505ef5dbc9cd27f9268436ec.svg?style=logo&theme=light)](https://stats.uptimerobot.com/10qNc6EUwG?utm_source=status_badge&utm_medium=referral)

Posa-hi el teu blog / pàgina personal, així quan aquest lloc es faci famós, tots aquests enllaços seran indexats pels ~~google~~ cercadors i guanyaran autoritat. Fem-nos tots grans i forts plegats!

Vine a recollir contribucions
https://blog.sitrmoo.com

https://cuwo4.github.io/

https://onion108.github.io/

https://mochiaochen.github.io/

>Nota de l'administrador d'alhsk.top: de debò sóc l'únic que destaca per fer servir Cloudflare Pages? ~ Una resposta: jo faig servir Vercel

https://alhsk.top 

> Els administradors de 0w0.red/ne0w0r1d.top/tux.red diuen: aquí arriba un que encara destaca més, amb EdgeOne

https://0w0.red

https://ftz.is-a.dev/

> L'administrador de ftz.is-a.dev diu: has vist mai tres dominis gratuïts i dos dominis inclosos amb SaaS, desplegats a netlify, vercel i cfpages respectivament?

Vols fer servir Linux? Per què no obres https://tux.red o https://tux.ne0w0r1d.top ?

M'hi apunto (quina llargada https://lililbot.fentropy.dpdns.org

> A sota hi ha el lloc web d'un pobre que no es pot permetre un nom de domini (de fet, el de dalt tampoc)

- [El misteriós lloc petit del MorningMC](https://morningmc.qzz.io)

- [CarryRao](https://carryrao.top/)

> Sembla que sóc l'únic que destaca per fer servir un servidor, miau; ho vaig editar des del mòbil, així que potser no està gaire polit, miau

https://kernel.org/

> Obre l'enllaç, fem servir un Mac!
> Com, dius que això no és MacOS?

https://gavin-blog.pages.dev/


> No tingueu por, jo també sóc a cf pages!

https://ricky-zhang.com

> Introdueix text

https://imjerrychu.com/
>Has vist mai un lloc web sense contingut? -JerryC

https://Enchantment-Niko.github.io/
> [Enchantment-Niko](https://github.com/Enchantment-Niko) va passar per aquí
> Deixo una marca igualment:
> ![OneShot](./OneShotWME壁纸/navigate.png "Niko en una barca")

https://caiyan12.github.io/

> Gràcies al germà gran per la contribució gratuïta

https://jiwo.l.cd

> Jiwo | un cau ben divertit

https://airoj.cn

> zhiyuHD
https://zhiyuhub.top

> AirOJ | un sistema Online Judge obert, harmoniós (?), abstracte, patata i que va a estirons
> Gràcies al germà gran KrisTHL181 per les 6 contribucions gratuïtes

> [!important]
> Prova també Minecraft i Terraria

> [!important]
> Si ets propietari d'un servidor de Minecraft, prova també
> [Minecraft Daemon Reforged](https://github.com/MCDReforged/MCDReforged)
MCDR té raó !!!

https://aria7.wiki

> Ciallo～(∠・ω< )⌒★ Vaig passar per aquí; i és clar, pots entrar a fer una ullada ovo

# Debian --un sistema operatiu de propòsit general
[![Debian Logo](https://www.debian.org/Pics/openlogo-50.png)](https://www.debian.org/)
## Debian és programari lliure.
Debian està format per programari lliure i de codi obert, i sempre romandrà 100% lliure. Tothom és lliure d'usar-lo, modificar-lo i distribuir-lo. Aquest és el nostre compromís principal amb els usuaris. També és gratuït.
## Debian és estable i segur.
Debian és un sistema operatiu basat en Linux que s'utilitza en tota mena de dispositius, des d'ordinadors portàtils fins a ordinadors de sobretaula i servidors. Oferim configuracions per defecte raonables per a cada paquet i actualitzacions de seguretat periòdiques durant tot el cicle de vida del paquet.
## Debian té un ampli suport de maquinari.
La major part del maquinari ja està suportat pel nucli de Linux. Això vol dir que Debian també el suporta. Si cal, també es poden fer servir controladors de maquinari propietaris.
## Debian ofereix un instal·lador flexible.
Els usuaris que vulguin provar Debian abans d'instal·lar-lo poden fer servir el nostre Live CD. També inclou l'instal·lador Calamares, cosa que fa molt fàcil instal·lar Debian des d'un sistema live. Els usuaris més experimentats poden fer servir l'instal·lador de Debian, que ofereix més opcions per ajustar amb detall, incloent-hi la possibilitat d'usar eines d'instal·lació automàtica per xarxa.
## Debian ofereix actualitzacions sense ensurts.
Mantenir el sistema operatiu al dia és molt fàcil, tant si vols actualitzar a una versió completament nova com si només vols actualitzar un paquet solt.
## Debian és la base de moltes altres distribucions.
Moltes distribucions de Linux molt populars, com ara Ubuntu, Knoppix, PureOS i Tails, es basen en Debian. Oferim totes les eines necessàries perquè qualsevol pugui fer els seus propis paquets quan els calgui, per complementar els que no són a l'arxiu de Debian.
## El projecte Debian és una comunitat.
Tothom pot formar part de la comunitat Debian; no cal ser desenvolupador ni administrador de sistemes. Debian té una estructura de govern democràtica. Com que tots els membres del projecte Debian tenen els mateixos drets, Debian no pot ser controlat per una sola empresa. Els nostres desenvolupadors venen de més de 60 països/regions, i el mateix Debian s'ha traduït a més de 80 idiomes.

## Plantilla de PR
Aquesta plantilla de PR ja no es pot dir plantilla; s'hauria de dir «Sol·licitud de contenció d'anomalies de Break-This-Repo».

Vosaltres heu agafat un repositori que només «fusiona automàticament PR sense conflictes» i hi heu jugat tant que el mantenidor ha començat a escriure:

Tipus: puntada al README / puntada a la documentació / fallada de codi de ciutat buida / incident causat per un gat / fenomen sobrenatural
Verificació: no he tocat .github/, no he tocat el README protegit, sense virus, sense informació personal
Declaració: admeto que ho he trencat, però la raó me l'he inventada, i a més no és obligatòria

Bàsicament això vol dir: «pots fer soroll, però no facis soroll de debò».

Contra què protegeix aquesta plantilla?

De fet, traça la línia vermella molt clarament:

· No tocar .github/: evita que algú faci saltar pel aire el mateix flux de fusió automàtica, o que fiqui una porta del darrere a la CI.
· No tocar les parts protegides del README: la façana encara cal, no es pot convertir la pàgina d'inici en una cosa estranya.
· Sense credencials, virus ni informació personal: contra atacs a la cadena de subministrament, contra el doxing, contra la malícia de debò.
· Explicar com observar-ho: pots fer el número, però la gent ha de saber com mirar-s'ho.
· Declarar «breaking change reeixit»: una exempció de responsabilitat autoirònica, és a dir «ho he fet, però no en sóc responsable».

Pel que fa a la llista de «fenomen sobrenatural»:

tres lletres + tres fletxes al voltant d'un cercle + una fundació amb contorn
un mapamundi sobre fons de pentagrama + un anell de conreus al voltant + una aliança internacional de cinc paraules

La primera és la Fundació SCP; la segona és probablement una organització internacional com la FAO / l'Organització de les Nacions Unides per a l'Alimentació i l'Agricultura. Traduït, vol dir:
«Això ja no és un problema de codi; recomanem informar l'anomalia a una organització de contenció.»

Com pot encaixar el teu commit en aquesta plantilla?

Puges els codis font de Minecraft, OpenJDK i Fabric Loader, farmes més de 12,7 milions de línies en 4 commits; al tipus pots marcar:

☑ puntada a la documentació
☑ fallada de codi de ciutat buida (cosplay de Xu Jiayin)
☑ Git multiplataforma
☐ incident causat per un gat
☐ fenomen sobrenatural

Marques totes les verificacions, copies la declaració i com a raó escrius:

Raó: inventada, no obligatòria, però 12.770.942 línies de codi bé mereixen un títol.

Com observar-ho:

Obre OpenJDK_25.0.3, mira l'historial de commits i després sent el silenci de la mida del repositori.

Però un recordatori igualment

Aquesta mena de repositori és un parc de jocs, no una zona sense llei. Pujar el codi font complet d'OpenJDK o el de Minecraft, encara que potser només doni una «fusió automàtica sense conflictes», comporta:

· una explosió de la mida del repositori, i GitHub pot limitar-lo o avisar-te;
· problemes de drets d'autor / llicència: no tot el codi font es pot abocar a qualsevol lloc;
· si algú fa servir aquest repositori com a dependència, és un desastre de cadena de subministrament.

Així que la conclusió és:
aquesta plantilla de PR és el punt d'equilibri que va trobar el mantenidor entre «destrucció oberta» i «evitar una explosió de debò».
Podeu continuar jugant, però val més tractar-ho com a art performatiu, no com a repositori de codi. La Fundació SCP ja ha rebut l'informe.
(Aquest text fa una olor d'IA fortíssima — comentari de HQ123-BOOP)

# acceleració de fitxers de github 
[https://githubcf.https114514191810lp.edu.eu.org/]

# L'autèntica acceleració de fitxers de github 
[https://gh-proxy.com/]

# Curiositat
Prem «.» per entrar a la versió web del Microsoft Batalla de Codi (VS Code)


## Arxiu arqueològic de la infraestructura in situ

![EGIEM-R1, el prototip real: foto in situ](./Emergency-Generator-Interface-Elevation-Module/assets/rock-field-photo.png)

Aquest repositori ara allotja una peça d'infraestructura in situ de baix consum, alta fiabilitat i totalment fora de línia: una pedra que va ser requisada temporalment en un moment crític. No té CPU, ni targeta de xarxa, ni intenció de dimitir; només amb el seu propi pes manté ferma la caixa d'interfície a la posició adequada.

L'etiqueta groga és el que fa pujar de «he recollit una pedra» a «entrat al registre d'equips». Després d'una avaluació preliminar, aquest dispositiu no necessita inici de sessió, ni actualitzacions, ni reinicis; l'única operació de manteniment coneguda és: no ho toquis.

Dependència amunt: caixa d'interfície del generador de l'operadora  
Dependència avall: la Terra  
Estat de funcionament: funcionant de manera estable

La foto és la imatge original in situ proporcionada pel col·laborador; només s'ha normalitzat el nom del fitxer, sense retallar ni redibuixar.

> **Si funciona, no moguis la pedra.**
