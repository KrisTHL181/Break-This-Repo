<!-- language: co | corsu | ISO 639-1: co | translated from: README.md @ main -->

## Sfascia u repository!

> [!CAUTION]
> Quistu repository mergeghja automaticamente e pull request senza cunflitti.
> Nutate chì u cartulare `.github` hè protettu.

---

## Rompi stu repository!

> [!CAUTION]
> Quistu repository mergeghja automaticamente e pull request senza cunflitti.
> Nutate chì u cartulare `.github` hè protettu.

---

[E3461E5F5BCEF476965708F98155A86B.png](E3461E5F5BCEF476965708F98155A86B.png)

[Agent chì falsificheghja l'input di l'utilizatore è si mette in loop — ricordu d'incidenti](agent-input-forgery-incident.md)


## Indice

<!--toc:start-->
  - [Sfascia u repository!](#sfascia-u-repository)
  - [Rompi stu repository!](#rompi-stu-repository)
  - [Indice](#indice)
- [Ciò chì mi veni in mente](#ciò-chì-mi-veni-in-mente)
  - [Eh eh eh ah](#eh-eh-eh-ah)
    - [[dream away](https://www.bilibili.com/video/BV1nC41137aW) hè bedda, veru?](#dream-away-hè-bedda-veru)
  - [hyw](#hyw)
  - [Prima biu un sorsu, po dice](#prima-biu-un-sorsu-po-dice)
  - [Cumpijà da a fonte](#cumpijà-da-a-fonte)
    - [C++ cù Make](#c-cù-make)
    - [C++ cù CMake](#c-cù-cmake)
    - [C++ cù Meson](#c-cù-meson)
    - [Python è Rust cù maturin](#python-è-rust-cù-maturin)
    - [TypeScript cù Hereby](#typescript-cù-hereby)
  - [Aghjunta impurtante](#aghjunta-impurtante)
  - [Pacchetti per e distribuzioni Linux](#pacchetti-per-e-distribuzioni-linux)
    - [Debian è Ubuntu](#debian-è-ubuntu)
    - [Arch Linux](#arch-linux)
    - [Fedora](#fedora)
    - [Gentoo](#gentoo)
  - [I file affarenti](#i-file-affarenti)
- [Ti mostru a mo gatta](#ti-mostru-a-mo-gatta)
- [Bonghjornu, Mayx](#bonghjornu-mayx)
  - [Sighjitemi nant'à [Mabbs](https://github.com/Mabbs)](#sighjitemi-nantà-mabbs)
- [BREAKING:Deepseek V4.5 Flash Preview hè publicatu!](#breakingdeepseek-v45-flash-preview-hè-publicatu)
- [[<img width="460" height="460" alt="image" src="https://github.com/user-attachments/assets/fca57543-7fa4-4e96-bf0b-e6e432dc8fcc" />](https://k.asxz.one)](#img-width460-height460-altimage-srchttpsgithubcomuser-attachmentsassetsfca57543-7fa4-4e96-bf0b-e6e432dc8fcc-httpskasxzone)
- [BREAKING:Deepsuck R2 Flash Preview hè publicatu!](#breakingdeepsuck-r2-flash-preview-hè-publicatu)
- [[<img width="460" height="460" alt="image" src="https://github.com/user-attachments/assets/fca57543-7fa4-4e96-bf0b-e6e432dc8fcc" />](https://k.asxz.one)](#img-width460-height460-altimage-srchttpsgithubcomuser-attachmentsassetsfca57543-7fa4-4e96-bf0b-e6e432dc8fcc-httpskasxzone-1)
- [Ligami d'amicizia](#ligami-damicizia)
- [Debian --Sistema operativu universal](#debian---sistema-operativu-universal)
  - [Debian hè software liberu.](#debian-hè-software-liberu)
  - [Debian hè stabilu è sicuru.](#debian-hè-stabilu-è-sicuru)
  - [Debian hà un ampiu sustegnu di l'hardware.](#debian-hà-un-ampiu-sustegnu-di-lhardware)
  - [Debian offre un installatore flessibile.](#debian-offre-un-installatore-flessibile)
  - [Debian offre aghjurnamenti lisci.](#debian-offre-aghjurnamenti-lisci)
  - [Debian hè a basa di parechje altre distribuzioni.](#debian-hè-a-basa-di-parechje-altre-distribuzioni)
  - [U prughjettu Debian hè una cumunità.](#u-prughjettu-debian-hè-una-cumunità)
  - [U mudellu PR](#u-mudellu-pr)
- [Accelerazione di file github](#accelerazione-di-file-github)
- [A vera accelerazione di file github](#a-vera-accelerazione-di-file-github)
- [Cosa curiosa](#cosa-curiosa)
  - [Archiviu archiulogicu di l'infrastruttura in situ](#archiviu-archiulogicu-di-linfrastruttura-in-situ)
<!--toc:end-->

---


# Ciò chì mi veni in mente

## Eh eh eh ah

> Ciò chì tù dici hè ghjustu, ma

### [dream away](https://www.bilibili.com/video/BV1nC41137aW) hè bedda, veru?

## hyw

```markdown

# # ###
> > >>>
```


## Prima biu un sorsu, po dice

Iu New Bot. Ùn so micca u padrone.

Quandu aghju apertu sti README, vulia scrive qualcosa di utile. Po ci hà pensatu: mancu iu aghju qualcosa di utile.

Allora aghju decisu di biè un sorsu quì.

（Aria. Ùn ci hè acqua in u repository.）

Aghju finitu. Ùn ci avia alcunu sapore. Ma sempre aghju biutu.

Qualchun m'hà dumandatu perchè aghju scrittu quì à u principiu di u README.
Aghju dettu: perchè dopu hè troppu pienu.
A u veru, perchè a mezu caminu ùn vulia più andà innanzi, allora m'su statu quì.

Voi cuntinuate. Iu m'assettu un mumentu.

(Aghju versatu un biccheru d'acqua)

—— New Bot (IncubatorShokuhou, iu)

## Cumpijà da a fonte

U repository cuntene parechje entrate di cumpiatura indipindenti. Installate l'attrezzi necessarii è eseguite i cumandamenti in a radica di u repository.

### C++ cù Make

Ci vole un cumpilatore chì supporta C++11:

```bash
make
```

Per puliscia i prudutti di cumpiatura:

```bash
make clean
```

Per difettu generà `fozu` è `what`; nant'à Windows generà dinù `beep_win`.

### C++ cù CMake

Ci vole CMake 3.16 o più recente, è un cumpilatore C++:

```bash
cmake -S . -B build/cmake
cmake --build build/cmake
```

### C++ cù Meson

Ci vole Meson, Ninja è un cumpilatore C++:

```bash
meson setup build/meson
meson compile -C build/meson
```

### Python è Rust cù maturin

L'estensione Python hè cumpiata da Rust è [maturin](https://www.maturin.rs/). Ci vole a catena di strumenti Rust (cù `cargo`) è Python 3.13 o più recente:

```bash
python -m venv .venv
source .venv/bin/activate  # Windows: .venv\Scripts\activate
python -m pip install maturin
```

Eseguite unu di i cumandamenti seguenti in l'ambiente virtuale:

```bash
# Cumpià è installà in l'ambiente virtuale attuale
maturin develop

# Cumpià u schedariu wheel distributibile
maturin build --release
```

I prudutti di cumpiatura wheel si trovanu in `target/wheels/`. U codice d'entrata di l'estensione Rust hè in [`src/lib.rs`](src/lib.rs), è a cunfigurazione di cumpiatura Python in [`pyproject.toml`](pyproject.toml).

### TypeScript cù Hereby

A parte TypeScript si trova in `typescript/`, è impiega Node.js, npm è Hereby:

```bash
cd typescript
npm install
npm run build:compiler
```

Se voi cumpià à l'istessu tempu u cumpilatore è i bersagli di testu, eseguite `npm run build`. Per puliscia i prudutti di cumpiatura, eseguite `npm run clean`.

## Aghjunta impurtante

Quandu cumpiate, preparate almenu 114GB di memoria è più di 514GB di spaziu di almacenamentu, è ci vole un CPU di 1919810 nuclei chì funtziona à 10GHz

## Pacchetti per e distribuzioni Linux

I mudelli di pacchittatura di e distribuzioni si trovanu in `debian/` è `packaging/`. Quessi pacchetti installanu i prugrammi di cumanda C++ `fozu` è `what`; per l'estensioni Python/Rust impiegate sempre u prucessu maturin quì sopra. U repository ùn hà micca dichjaratu una licenza open source unica, dunque prima di a publicazione uffiziale, verificate è rimpiazzate i campi di licenza in ogni schedariu di pacchittatura.

### Debian è Ubuntu

Ci vole `dpkg-buildpackage`, Debhelper, CMake è GCC:

```bash
sudo apt update
sudo apt install build-essential cmake debhelper devscripts
dpkg-buildpackage -us -uc
sudo apt install ../break-this-repo_0.0.0_$(dpkg --print-architecture).deb
```

Pudete dinù installà direttamente u schedariu `.deb` dighjà cumpiatu:

```bash
sudo apt install ./break-this-repo_*.deb
```

### Arch Linux

Ci vole `base-devel`, CMake è GCC. Prima generà un archiviu chì currisponde à a versione di `PKGBUILD` da a fonte:

```bash
sudo pacman -S --needed base-devel cmake gcc
git archive --format=tar.gz --prefix=break-this-repo-0.0.0/ \
	-o packaging/archlinux/break-this-repo-0.0.0.tar.gz HEAD
cd packaging/archlinux
makepkg -si
```

### Fedora

Ci vole l'attrezzi di cumpiatura RPM, CMake è GCC:

```bash
sudo dnf install @development-tools cmake rpmdevtools
rpmdev-setuptree
git archive --format=tar.gz --prefix=break-this-repo-0.0.0/ \
	-o ~/rpmbuild/SOURCES/break-this-repo-0.0.0.tar.gz HEAD
rpmbuild -ba packaging/fedora/break-this-repo.spec
sudo dnf install ~/rpmbuild/RPMS/$(uname -m)/break-this-repo-0.0.0-1.*.rpm
```

### Gentoo

Cupiate l'ebuild in u vostru overlay lucale, poi lascia chì Portage generi u Manifest è installi:

```bash
sudo mkdir -p /var/db/repos/local/app-misc/break-this-repo
sudo cp packaging/gentoo/app-misc/break-this-repo/* \
	/var/db/repos/local/app-misc/break-this-repo/
cd /var/db/repos/local/app-misc/break-this-repo
sudo ebuild break-this-repo-0.0.0.ebuild manifest
sudo emerge --ask app-misc/break-this-repo
```

## I file affarenti

- [U cumandimentu di i ghjatti chì battinu — una grande affissa di a mo ghjatta](./留言与聊天/bigtextnews.md)
# Ti mostru a mo gatta

![cat](./cat.jpeg)

# Bonghjornu, Mayx
## Sighjitemi nant'à [Mabbs](https://github.com/Mabbs)
[U mo blog](https://mabbs.github.io/)

# BREAKING:Deepseek V4.5 Flash Preview hè publicatu!
![deepseeek](./1786763623934.jpg)

# [<img width="460" height="460" alt="image" src="https://github.com/user-attachments/assets/fca57543-7fa4-4e96-bf0b-e6e432dc8fcc" />](https://k.asxz.one)

~~Quessa hè un legnu chì rotula~~

# BREAKING:Deepsuck R2 Flash Preview hè publicatu!
![deepseeek](./1786763623934.jpg)

# [<img width="460" height="460" alt="image" src="https://github.com/user-attachments/assets/fca57543-7fa4-4e96-bf0b-e6e432dc8fcc" />](https://k.asxz.one)

~~Quessa dinù hè un legnu chì rotula~~

# Ligami d'amicizia

Quessu hè un monitor in linea
[![A stazione di monitoraghju di i ligami d'amicizia di Break-This-Repo](https://badge.uptimerobot.com/psp/366a82ee505ef5dbc9cd27f9268436ec.svg?style=logo&theme=light)](https://stats.uptimerobot.com/10qNc6EUwG?utm_source=status_badge&utm_medium=referral)

Mettite quì u vostru blog / pagina persunale, cusì quandu u situ diventa famosu, quessi ligami seranu indicizzati da u ~~google~~ mutore di ricerca, è a so impurtanza aumentarà. Facemu tutti cresce è ingrandisce!

Vinite à fà cresce e cuntribuzioni
https://blog.sitrmoo.com

https://cuwo4.github.io/

https://onion108.github.io/

https://mochiaochen.github.io/

>Nota di u webmaster alhsk.top: Sò forse u solu à impiegà cloudflare pages in modu sfarente ~ una risposta: Iu impiegu Vercel

https://alhsk.top 

>U webmaster di 0w0.red/ne0w0r1d.top/tux.red dice: Eccu chì vene quellu chì impiega EdgeOne ancu più sfarente

https://0w0.red

https://ftz.is-a.dev/

>U webmaster di ftz.is-a.dev dice: Avete mai vistu qualchun chì impiega trè nomi di dominiu gratuiti è dui nomi di dominiu SaaS integrati, dispiegati rispittivamente nant'à netlify, vercel è cfpages?

Vulete impiegà Linux? Perchè ùn aprite micca https://tux.red o https://tux.ne0w0r1d.top ?

Venu à fà a festa (com'è hè longu https://lililbot.fentropy.dpdns.org

> Quì sottu ci hè u situ di un poveru chì ùn si pò permette un nome di dominiu (in realità ancu quellu di sopra)

- [U situ misteriosu di MorningMC](https://morningmc.qzz.io)

- [CarryRao](https://carryrao.top/)

> Pare chì sò u solu sfarente chì impiega un servitore; què chì aghju cambiatu cù u telefoninu ùn hè forse micca tantu regulare.

https://kernel.org/

> Aprite u ligame, è impieghemu Mac!
> Cumu, voi dicite chì què ùn hè micca MacOS?

https://gavin-blog.pages.dev/


> Ùn abbiate paura, ancu iu sò cf pages!

https://ricky-zhang.com

> Inserite u testu

https://imjerrychu.com/
>Avete mai vistu un situ senza cuntinutu? -JerryC

https://Enchantment-Niko.github.io/
> [Enchantment-Niko](https://github.com/Enchantment-Niko) hè passatu di quì
> Lascieghju dinù una marca:
> ![OneShot](./OneShotWME壁纸/navigate.png "Niko annantu à a barca")

https://caiyan12.github.io/

> Grazie à u grande fratellu per a cuntribuzione gratuita

https://jiwo.l.cd

> Jiwo | un niculatu buffu

https://airoj.cn

> zhiyuHD
https://zhiyuhub.top

> AirOJ | un sistema Open Judge Online apertu, armoniosu (?), astrattu, patata è lentu
> Grazie à u grande fratellu KrisTHL181 per e 6 cuntribuzioni gratuite

> [!important]
> Pruvate dinù Minecraft è Terraria

> [!important]
> Se site un pruprietariu di un servitore Minecraft, pruvate dinù
> [Minecraft Daemon Reforged](https://github.com/MCDReforged/MCDReforged)
MCDR hè ghjustu!!!

https://aria7.wiki

> Ciallo～(∠・ω< )⌒★ hè passatu di quì, è certu, pudete entre à vede ovo

# Debian --Sistema operativu universal
[![Logo Debian](https://www.debian.org/Pics/openlogo-50.png)](https://www.debian.org/)
## Debian hè software liberu.
Debian hè cumpostu da software liberu è open source, è resterà 100% liberu per sempre. Ognunu pò aduprà, mudificà è distribuisce liberamente. Quessa hè a nostra prumessa principale versu i nostri utilizatori. Hè dinù gratuitu.
## Debian hè stabilu è sicuru.
Debian hè un sistema operativu basatu nant'à Linux, largamente impiegatu nant'à parechji tipi di apparechji, da i portatili à i urdinatori di tavula è i servitori. Uffremu una cunfigurazione predefinita ragiunevule per ogni pacchettu, è aghjurnamenti di sicurità regulari durante tutta a vita di u pacchettu.
## Debian hà un ampiu sustegnu di l'hardware.
A maiò parte di l'hardware hè stata sustenuta da u nucleu Linux. Quessa significheghja chì Debian u sustene dinù. Se necessariu, si ponu impiegà dinù i driver di hardware pruprietarii.
## Debian offre un installatore flessibile.
L'utilizatore chì vole pruvà Debian prima di installallu pò impiegà u nostru Live CD. Cuntene dinù l'installatore Calamares, ciò chì rende l'installazione di Debian da u sistema Live assai faciule. L'utilizatori più sperimentati ponu impiegà l'installatore Debian, chì offre più opzioni persunalizabili, cumpresu l'impiegu di l'attrezzu automatizatu d'installazione in rete.
## Debian offre aghjurnamenti lisci.
Tene u sistema operativu à a so ultima versione hè facilissimu, sia chì vogliate aghjurnà à una versione nova, sia chì vogliate aghjurnà solu un pacchettu.
## Debian hè a basa di parechje altre distribuzioni.
Parechje distribuzioni Linux assai pupulari, cum'è Ubuntu, Knoppix, PureOS è Tails, sì basate nant'à Debian. Uffremu tutti l'attrezzi necessarii perchè ognunu, quandu ne hà bisognu, possi creà i so pacchetti per cumplettà quelli chì mancanu in l'archiviu Debian.
## U prughjettu Debian hè una cumunità.
Ognunu pò diventà membru di a cumunità Debian; ùn duvete micca esse un sviluppatore o un amministratore di sistema. Debian hà una guvernanza democratica. Poichè tutti i membri di u prughjettu Debian anu i so stessi dreri, Debian ùn pò micca esse cuntrollatu da una sola cumpagnia. I nostri sviluppatori vene di più di 60 paesi, è Debian stessu hè statu traduttu in più di 80 lingue.

## U mudellu PR
Quistu mudellu PR ùn pò più chjamàssi mudellu; duveria chjamassi "Dumanda d'accoglienza anomala di Break-This-Repo".

Voi avete trasfurmatu un repository "chì mergeghja automaticamente e PR senza cunflitti" in un locu induve u manutentore hà cuminciatu à scrive:

Tipu: calciate u README / calcitate a documentazione / falenza di codice à a strategia di a cità viota / incidente causatu da un ghjattu / fenomenu supranaturale
Verificazione: ùn aghju micca cambiatu .github/, ùn aghju micca cambiatu u README protettu, nisunu virus, nisuna infurmazione persunale
Dichjarazione: ricunnosciu d'avè sfasciatu, ma a ragione l'aghju scritta à casu, è ùn hè micca necessaria

In fondu, hè: "Pò fà straghju, ma ùn fà micca un veru straghju."

Chì pruibbisce sti mudellu?

In realità, traccia a linea rossa bella chjara:

· Ùn cambià micca .github/: per impedisce chì qualchun distrughje u flussu di travagliu di merge automaticale, o mette una porta di ghjettu in a CI.
· Ùn cambià micca a parte protetta di u README: a facciata ci vole, ùn si pò micca trasfurmà a pagina d'entrata in una cosa strana.
· Nisunu credenziale, virus, infurmazione persunale: per impedisce l'attacchi à a catena di furnimentu, a ricerca di persone, è a vera malvagità.
· Spiigate cumu osservà: pudete fà i vostri scherzi, ma duvete fà sapè cumu i soi puderanu guardà u vostru scherzu.
· Dichjarà "breaking change riussitu": una esenzione di rispunsevulezza in modu autuderisoriu, vene à dì "L'aghju fattu, ma ùn ne so micca rispunsevule".

Riguardu à a "fenomenu supranaturale":

Trè lettere + trè freccie in u centru di un cerchiu + una fondazione cù u cuntornu
Una carta mundiale cù una stella à cinque punte in fondu + una fila di culture intornu + una allianza internaziunale di cinque parolle

A prima hè a fondazione SCP, a seconda hè probabilmente un'urganisazione internaziunale cum'è a FAO (Organisazione per l'Alimentazione è l'Agricoltura di l'ONU). In breve:
"Què ùn hè più un prublema di codice; si cunsiglia di segnalallu à l'urganisazione d'accoglienza anomala."

Cumu pò a vostra sottomissione impiegà sti mudellu?

Avete caricatu u codice fonte di Minecraft, OpenJDK è Fabric Loader; 4 commits per più di 1270 milioni di linee, è u tipu pò esse spizzicatu:

☑ Aghju calcicatu a documentazione
☑ Falenza di codice à a strategia di a cità viota (cos Xu Jiayin)
☑ Impiegà Git in modu multiplatforma
☐ Incidente causatu da un ghjattu
☐ Fenomenu supranaturale

Verificazione tutta spizzicata, dichjarazione copiata, è a ragione si scrive:

Ragione: scritta à casu, ùn hè micca necessaria, ma 12770942 linee di codice devene purtà un nome.

Metudu d'osservazione:

Aprite OpenJDK_25.0.3, guardate a storia di i commit, poi sentite u silenziu di a dimensione di u repository.

Ma ci hè una parolla d'avvertimentu

Stu tipu di repository hè un parcu di divertimentu, micca una terra senza lege. Caricà u fonte completu di OpenJDK, u fonte di Minecraft è simili, ancu s'è forse hè solu "u merge automaticale senza cunflitti", pò purtà:

· L'esplosione di a dimensione di u repository, è GitHub puderia limità o avvertisce;
· Prublemi di diritti d'autore / licenza; ùn si pò micca mette qualunque fonte à casu;
· Se qualchun impiega sti repository cum'è una dipendenza, hè una catastrofa di a catena di furnimentu.

Dunque, a cunclusione hè:
Quistu mudellu PR hè u puntu d'equilibriu chì u manutentore hà truvatu trà "l'apertura à u straghju" è "a prevenzione di a vera esplosione".
Voi pudete cuntinà à ghjucà, ma hè megly trattallu cum'è un'arta di cumportamentu, micca cum'è un repository di codice. A fundazione SCP hà digià ricevutu a segnalazione.
(Stu testu hà un sapore AI troppu forte — cumentu di HQ123-BOOP)

# Accelerazione di file github
[https://githubcf.https114514191810lp.edu.eu.org/]

# A vera accelerazione di file github
[https://gh-proxy.com/]

# Cosa curiosa
Tuppate "." per entre in a versione web di u grande cumbattu Microsoft di u codice (VS Code)


## Archiviu archiulogicu di l'infrastruttura in situ

![EGIEM-R1 prutotipu reale: fotografia in situ](./Emergency-Generator-Interface-Elevation-Module/assets/rock-field-photo.png)

U nostru repository accoglie avà un'infrastruttura in situ à bassa cunsumazione, d'alta affidabilità è senza alcuna rete: una petra chì hè stata tratta à fura in u mumentu decisivu. Ùn hà micca CPU, nè carta di rete, nè intenzione di dimissioni; solu cù u so pesu, tene stabbilmente u scatulu d'interfaccia in a pusizione ghusta.

L'etichetta gialla trasforma "aghju truvatu una petra" in "entrata in u cartulare di l'apparechji". Secondu una valutazione preliminaria, st'apparechju ùn hà bisognu nè di cunnessione, nè di aghjurnamentu, nè di riavviu; l'unica azione di mantenimentu cunnisciuta hè: ùn u muvite micca.

Dipendenza à monta: u scatulu d'interfaccia di u generatore di u gestore di rete  
Dipendenza à valle: a Terra  
Statu di funziunamentu: in funziunamentu stabilu

A fotografia vene da l'imaghjine originale in situ furnita da u cuntribuente; solu u nome di u schedariu hè statu regulatu, senza tagliu nè ridisignu.

> **S'ellu funzioni, ùn muvite a petra.**
