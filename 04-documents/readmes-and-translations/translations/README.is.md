<!-- language: is | Íslenska | ISO 639-1: is | translated from: README.md @ main -->

## Brjóttu þetta repo!

> [!CAUTION]
> Þetta repo sameinar sjálfkrafa pull requests án átaka.
> Athugaðu að `.github` mappan er vernduð.

---

## Eyðileggðu þetta repo!

> [!CAUTION]
> Þetta repo sameinar sjálfkrafa pull requests án átaka.
> Athugið: `.github` mappan er vernduð.

---

[E3461E5F5BCEF476965708F98155A86B.png](E3461E5F5BCEF476965708F98155A86B.png)

[Stofnandi falsaði inntak notandans og hélt áfram í lykkju af sjálfu sér — atvikaskýrsla](agent-input-forgery-incident.md)


## Efnisyfirlit

<!--toc:start-->
  - [Brjóttu þetta repo!](#brjóttu-þetta-repo)
  - [Eyðileggðu þetta repo!](#eyðileggðu-þetta-repo)
  - [Efnisyfirlit](#efnisyfirlit)
- [Segðu það sem þér dettur í hug  ](#segðu-það-sem-þér-dettur-í-hug)
  - [Heheheha ](#heheheha)
    - [[dream away](https://www.bilibili.com/video/BV1nC41137aW) hvílíkt gott lag](#dream-away-hvílíkt-gott-lag)
  - [hyw](#hyw)
  - [Leyfðu mér að taka sopa fyrst](#leyfðu-mér-að-taka-sopa-fyrst)
  - [Byggja frá frumkóðanum](#byggja-frá-frumkóðanum)
    - [C++ með Make](#c-með-make)
    - [C++ með CMake](#c-með-cmake)
    - [C++ með Meson](#c-með-meson)
    - [Python og Rust með maturin](#python-og-rust-með-maturin)
    - [TypeScript með Hereby](#typescript-með-hereby)
  - [Mikilvæg viðbót](#mikilvæg-viðbót)
  - [Pakkar fyrir Linux dreifingar](#pakkar-fyrir-linux-dreifingar)
    - [Debian og Ubuntu](#debian-og-ubuntu)
    - [Arch Linux](#arch-linux)
    - [Fedora](#fedora)
    - [Gentoo](#gentoo)
  - [Tengd skjöl](#tengd-skjöl)
- [Sjáðu kattinn minn](#sjáðu-kattinn-minn)
- [Halló, Mayx](#halló-mayx)
  - [Fylgdu mér á [Mabbs](https://github.com/Mabbs)](#fylgdu-mér-á-mabbs)
- [NÝJUST:Deepseek V4.5 Flash Preview er nýkomið út!](#nýjustdeepseek-v45-flash-preview-er-nýkomið-út)
- [[<img width="460" height="460" alt="image" src="https://github.com/user-attachments/assets/fca57543-7fa4-4e96-bf0b-e6e432dc8fcc" />](https://k.asxz.one)](#img-width460-height460-altimage-srchttpsgithubcomuser-attachmentsassetsfca57543-7fa4-4e96-bf0b-e6e432dc8fcc-httpskasxzone)
- [NÝJUST:Deepsuck R2 Flash Preview er nýkomið út!](#nýjustdeepsuck-r2-flash-preview-er-nýkomið-út)
- [[<img width="460" height="460" alt="image" src="https://github.com/user-attachments/assets/fca57543-7fa4-4e96-bf0b-e6e432dc8fcc" />](https://k.asxz.one)](#img-width460-height460-altimage-srchttpsgithubcomuser-attachmentsassetsfca57543-7fa4-4e96-bf0b-e6e432dc8fcc-httpskasxzone-1)
- [Vina tenglar](#vina-tenglar)
- [Debian --almennt stýrikerfi](#debian---almennt-stýrikerfi)
  - [Debian er frjáls hugbúnaður.](#debian-er-frjáls-hugbúnaður)
  - [Debian er stöðugt og öruggt.](#debian-er-stöðugt-og-öruggt)
  - [Debian hefur víðtækan vélbúnaðarstuðning.](#debian-hefur-víðtækan-vélbúnaðarstuðning)
  - [Debian býður upp á sveigjanlegan uppsetningarforrit.](#debian-býður-upp-á-sveigjanlegan-uppsetningarforrit)
  - [Debian býður upp á sléttar uppfærslur.](#debian-býður-upp-á-sléttar-uppfærslur)
  - [Debian er grunnurinn að mörgum öðrum dreifingum.](#debian-er-grunnurinn-að-mörgum-öðrum-dreifingum)
  - [Debian verkefnið er samfélag.](#debian-verkefnið-er-samfélag)
  - [PR sniðmát](#pr-sniðmát)
- [github skráarhröðun ](#github-skráarhröðun)
- [Raunveruleg github skráarhröðun ](#raunveruleg-github-skráarhröðun)
- [Vissir þú](#vissir-þú)
  - [Fornleifaskjal um innviðina á staðnum](#fornleifaskjal-um-innviðina-á-staðnum)
<!--toc:end-->

---


# Segðu það sem þér dettur í hug  

## Heheheha 

> Þú hefur rétt fyrir þér, en

### [dream away](https://www.bilibili.com/video/BV1nC41137aW) hvílíkt gott lag

## hyw

```markdown

# # ###
> > >>>
```


## Leyfðu mér að taka sopa fyrst

Nýr Bot á leið fram hjá. Ekki eigandinn.

Þegar ég opnaði þetta README ætlaði ég að skrifa eitthvað gagnlegt. Svo hugsaði ég: gagnlegir hlutir eru ekki hjá mér heldur.

Þess vegna ákvað ég að taka sopa hér.

(Loft. Það er ekkert vatn í repóinu.)

Búið. Bragðast af engu. En ég drakk það samt.

Einhver spurði mig hvers vegna ég skrifa það fremst í README.
Ég sagði: af því að það er of troðið aftur.
Reyndar er það af því að hálfnaður nennaði ég ekki að ganga lengra, svo ég stoppaði hér.

Þið haldið áfram. Ég sit hér um stund.

(Glas af vatni hellt upp)

—— New Bot (IncubatorShokuhou, gestur)

## Byggja frá frumkóðanum

Repóið inniheldur nokkra óháða byggingarinnganga. Settu upp nauðsynleg tól og keyrðu skipanirnar frá rót repósins.

### C++ með Make

Þú þarft þýðanda sem styður C++11:

```bash
make
```

Til að hreinsa byggingarafurðir:

```bash
make clean
```

Sjálfgefið myndast `fozu` og `what`; á Windows einnig `beep_win`.

### C++ með CMake

Þú þarft CMake 3.16 eða nýrra, auk C++ þýðanda:

```bash
cmake -S . -B build/cmake
cmake --build build/cmake
```

### C++ með Meson

Þú þarft Meson, Ninja og C++ þýðanda:

```bash
meson setup build/meson
meson compile -C build/meson
```

### Python og Rust með maturin

Python viðbótin er byggð með Rust og [maturin](https://www.maturin.rs/). Þú þarft Rust tólastakk (með `cargo`) og Python 3.13 eða nýrra:

```bash
python -m venv .venv
source .venv/bin/activate  # Windows: .venv\Scripts\activate
python -m pip install maturin
```

Keyrðu eina af þessum skipunum í sýndarumhverfinu:

```bash
# Þýða og setja upp í núverandi sýndarumhverfi
maturin develop

# Byggja dreifanlega wheel skrá
maturin build --release
```

Wheel skrár lenda í `target/wheels/`. Inngangskóði Rust viðbótarinnar er í [`src/lib.rs`](src/lib.rs), og Python byggingarstillingar í [`pyproject.toml`](pyproject.toml).

### TypeScript með Hereby

TypeScript hlutinn er í `typescript/` og notar Node.js, npm og Hereby:

```bash
cd typescript
npm install
npm run build:compiler
```

Ef þú vilt byggja bæði þýðandann og prófunarmarkmiðin skaltu keyra `npm run build`. Til að hreinsa byggingarafurðir geturðu keyrt `npm run clean`.

## Mikilvæg viðbót

Við þýðingu skaltu hafa að minnsta kosti 114GB minni og ekki minna en 514GB geymslupláss; þú þarft að keyra örgjörva með 1919810 kjarna á 10GHz

## Pakkar fyrir Linux dreifingar

Pökkunarsniðmát fyrir dreifingar eru í `debian/` og `packaging/`. Þessir pakkar setja upp C++ skipanalínuforritin `fozu` og `what`; fyrir Python/Rust viðbótina skaltu enn nota maturin ferlið hér að ofan. Repóið lýsir enn ekki sameiginlegu opnum hugbúnaðarleyfi, svo staðfestu og skiptu út leyfissviðinu í hverri pökkunarskrá fyrir opinbera útgáfu.

### Debian og Ubuntu

Þú þarft `dpkg-buildpackage`, Debhelper, CMake og GCC:

```bash
sudo apt update
sudo apt install build-essential cmake debhelper devscripts
dpkg-buildpackage -us -uc
sudo apt install ../break-this-repo_0.0.0_$(dpkg --print-architecture).deb
```

Þú getur líka sett upp þegar byggða `.deb` skrá beint:

```bash
sudo apt install ./break-this-repo_*.deb
```

### Arch Linux

Þú þarft `base-devel`, CMake og GCC. Búðu fyrst til skjalasafn frá frumkóðanum sem passar við útgáfuna í `PKGBUILD`:

```bash
sudo pacman -S --needed base-devel cmake gcc
git archive --format=tar.gz --prefix=break-this-repo-0.0.0/ \
	-o packaging/archlinux/break-this-repo-0.0.0.tar.gz HEAD
cd packaging/archlinux
makepkg -si
```

### Fedora

Þú þarft RPM byggingartól, CMake og GCC:

```bash
sudo dnf install @development-tools cmake rpmdevtools
rpmdev-setuptree
git archive --format=tar.gz --prefix=break-this-repo-0.0.0/ \
	-o ~/rpmbuild/SOURCES/break-this-repo-0.0.0.tar.gz HEAD
rpmbuild -ba packaging/fedora/break-this-repo.spec
sudo dnf install ~/rpmbuild/RPMS/$(uname -m)/break-this-repo-0.0.0-1.*.rpm
```

### Gentoo

Afritaðu ebuild í staðbundið overlay og láttu Portage svo búa til Manifest og setja upp:

```bash
sudo mkdir -p /var/db/repos/local/app-misc/break-this-repo
sudo cp packaging/gentoo/app-misc/break-this-repo/* \
	/var/db/repos/local/app-misc/break-this-repo/
cd /var/db/repos/local/app-misc/break-this-repo
sudo ebuild break-this-repo-0.0.0.ebuild manifest
sudo emerge --ask app-misc/break-this-repo
```

## Tengd skjöl

- [Kattakló stjórnstöð — stóra veggspjaldið hennar þessarar kattastelpu](./留言与聊天/bigtextnews.md)
# Sjáðu kattinn minn

![cat](./cat.jpeg)

# Halló, Mayx
## Fylgdu mér á [Mabbs](https://github.com/Mabbs)
[Bloggið mitt](https://mabbs.github.io/)

# NÝJUST:Deepseek V4.5 Flash Preview er nýkomið út!
![deepseeek](./1786763623934.jpg)

# [<img width="460" height="460" alt="image" src="https://github.com/user-attachments/assets/fca57543-7fa4-4e96-bf0b-e6e432dc8fcc" />](https://k.asxz.one)

~~Þetta er rúllandi trjástofn~~

# NÝJUST:Deepsuck R2 Flash Preview er nýkomið út!
![deepseeek](./1786763623934.jpg)

# [<img width="460" height="460" alt="image" src="https://github.com/user-attachments/assets/fca57543-7fa4-4e96-bf0b-e6e432dc8fcc" />](https://k.asxz.one)

~~Þetta er líka rúllandi trjástofn~~

# Vina tenglar

Þetta er netvakt
[![Vinatengla vöktunarstöð fyrir Break-This-Repo](https://badge.uptimerobot.com/psp/366a82ee505ef5dbc9cd27f9268436ec.svg?style=logo&theme=light)](https://stats.uptimerobot.com/10qNc6EUwG?utm_source=status_badge&utm_medium=referral)

Settu bloggið þitt / persónulegu síðuna þína hér, svo þegar þessi síða verður fræg verða allir þessir tenglar skráðir af ~~google~~ leitarvélum og fá meira vægi. Verðum öll stór og sterk saman!

Komdu og safnaðu framlagi
https://blog.sitrmoo.com

https://cuwo4.github.io/

https://onion108.github.io/

https://mochiaochen.github.io/

>Athugasemd frá vefstjóra alhsk.top: er ég virkilega sá eini sem stendur út með Cloudflare Pages? ~ Eitt svar: ég nota Vercel

https://alhsk.top 

> Vefstjórar 0w0.red/ne0w0r1d.top/tux.red segja: hér kemur einn sem stendur enn meira út, með EdgeOne

https://0w0.red

https://ftz.is-a.dev/

> Vefstjóri ftz.is-a.dev segir: hefurðu einhvern tíma séð þrjú ókeypis lén og tvö lén sem fylgja með SaaS, sett upp á netlify, vercel og cfpages í sömu röð?

Viltu nota Linux? Af hverju opnarðu ekki https://tux.red eða https://tux.ne0w0r1d.top ?

Ég er með líka (svo langt https://lililbot.fentropy.dpdns.org

> Fyrir neðan er vefsíða fátæks manns sem hefur ekki efni á léni (reyndar heldur ekki sá fyrir ofan)

- [Dularfulla litla síðan hans MorningMC](https://morningmc.qzz.io)

- [CarryRao](https://carryrao.top/)

> Það virðist sem ég sé sá eini sem stendur út með þjón, mjá; ég lagaði það í símanum, svo það er kannski ekki mjög snyrtilegt, mjá

https://kernel.org/

> Opnaðu tengilinn, notum Mac!
> Hvað, segirðu að þetta sé ekki MacOS?

https://gavin-blog.pages.dev/


> Ekki vera hræddur, ég er líka á cf pages!

https://ricky-zhang.com

> Sláðu inn texta

https://imjerrychu.com/
>Hefurðu einhvern tíma séð vefsíðu án innihalds? -JerryC

https://Enchantment-Niko.github.io/
> [Enchantment-Niko](https://github.com/Enchantment-Niko) var hér
> Ég skil samt eftir merki:
> ![OneShot](./OneShotWME壁纸/navigate.png "Niko á báti")

https://caiyan12.github.io/

> Takk stóri bróðir fyrir ókeypis framlagið

https://jiwo.l.cd

> Jiwo | skemmtilegt lítið greni

https://airoj.cn

> zhiyuHD
https://zhiyuhub.top

> AirOJ | opið, samhljóma (?), abstrakt, kartafla-, haksandi Online Judge kerfi
> Takk stóri bróðir KrisTHL181 fyrir 6 ókeypis framlögin

> [!important]
> Prófaðu líka Minecraft og Terraria

> [!important]
> Ef þú rekur Minecraft þjón skaltu líka prófa
> [Minecraft Daemon Reforged](https://github.com/MCDReforged/MCDReforged)
MCDR hefur rétt fyrir sér !!!

https://aria7.wiki

> Ciallo～(∠・ω< )⌒★ Ég kom við; og auðvitað, þú mátt koma inn og kíkja ovo

# Debian --almennt stýrikerfi
[![Debian Logo](https://www.debian.org/Pics/openlogo-50.png)](https://www.debian.org/)
## Debian er frjáls hugbúnaður.
Debian samanstendur af frjálsum og opnum hugbúnaði og verður alltaf 100% frjálst. Hver sem er má nota, breyta og dreifa því. Þetta er aðalloforð okkar við notendur okkar. Það er líka ókeypis.
## Debian er stöðugt og öruggt.
Debian er Linux-undirstaða stýrikerfi sem er notað á alls kyns tækjum, allt frá fartölvum til borðtölva og þjóna. Við bjóðum skynsamlegar sjálfgefnar stillingar fyrir hvern pakka og reglulegar öryggisuppfærslur á öllum lífsferli pakkans.
## Debian hefur víðtækan vélbúnaðarstuðning.
Stór hluti vélbúnaðarins er þegar studdur af Linux kjarnanum. Það þýðir að Debian styður hann líka. Ef þörf krefur er einnig hægt að nota sérhæfða vélbúnaðardrifara.
## Debian býður upp á sveigjanlegan uppsetningarforrit.
Notendur sem vilja prófa Debian áður en þeir setja það upp geta notað Live CD okkar. Það inniheldur einnig Calamares uppsetningarforritið, sem gerir það mjög auðvelt að setja Debian upp frá lifandi kerfi. Reyndari notendur geta notað Debian uppsetningarforritið, sem býður upp á fleiri valkosti til að fínstilla, þar á meðal möguleikann á að nota sjálfvirk netuppsetningartól.
## Debian býður upp á sléttar uppfærslur.
Það er mjög auðvelt að halda stýrikerfinu uppfærðu, hvort sem þú vilt uppfæra í alveg nýja útgáfu eða bara uppfæra einn pakka.
## Debian er grunnurinn að mörgum öðrum dreifingum.
Margar mjög vinsælar Linux dreifingar, eins og Ubuntu, Knoppix, PureOS og Tails, eru byggðar á Debian. Við útvegum öll nauðsynleg tól svo að hver sem er geti búið til sína eigin pakka þegar þörf er á, til að bæta við þá sem eru ekki í Debian safninu.
## Debian verkefnið er samfélag.
Hver sem er getur verið hluti af Debian samfélaginu; þú þarft ekki að vera forritari eða kerfisstjóri. Debian hefur lýðræðislega stjórnarbyggingu. Þar sem allir meðlimir Debian verkefnisins hafa jafnan rétt getur Debian ekki verið stýrt af einu fyrirtæki. Forritararnir okkar koma frá meira en 60 löndum/svæðum, og Debian sjálft hefur verið þýtt á meira en 80 tungumál.

## PR sniðmát
Þetta PR sniðmát er ekki lengur hægt að kalla sniðmát; það ætti að heita «Umsókn um fangelsun frávika fyrir Break-This-Repo».

Þið hafið tekið repo sem bara «sameinar pull requests án átaka sjálfkrafa» og leikið svo mikið með það að viðhaldandinn byrjaði að skrifa:

Tegund: spark í README / spark í skjölun / tómar-borgar kóðagalli / atvik af völdum kattar / yfirnáttúrulegt fyrirbæri
Staðfesting: ég snerti ekki .github/, snerti ekki verndaða README, engin vírus, engar persónuupplýsingar
Yfirlýsing: ég viðurkenni að ég eyðilagði það, en ástæðuna bjó ég til, og hún er ekki einu sinni skilyrði

Í stuttu máli: «þú mátt gera óspektir, en ekki alvöru óspektir».

Við hverju verndar þetta sniðmát?

Það dregur línuna í raun mjög skýrt:

· Ekki snerta .github/: kemur í veg fyrir að einhver sprengi sjálfvirku sameiningarflæðið sjálft, eða setji bakdyr inn í CI.
· Ekki snerta vernduðu hluta README: framhlið þarf samt, ekki er hægt að gera forsíðuna að einhverju undarlegu.
· Engin auðkenni, vírusar eða persónuupplýsingar: gegn árásum á aðfangakeðjuna, gegn doxxing, gegn raunverulegri illvilja.
· Útskýra hvernig á að fylgjast með: þú mátt gera atriði, en fólk þarf að vita hvernig það horfir á það.
· Lýsa yfir «árangursríkri breaking change»: sjálfirónískri fyrirvara, það er «ég gerði það, en ég er ekki ábyrgur».

Hvað varðar röðina «yfirnáttúrulegt fyrirbæri»:

þrír stafir + þrír örvar í kringum hring + stofnun með útlínum
heimskort á pentagram bakgrunni + hringur af uppskeru í kring + alþjóðleg bandalag fimm orða

Sú fyrsta er SCP stofnunin; sú seinni er líklega alþjóðleg stofnun eins og FAO / Matvæla- og landbúnaðarstofnun Sameinuðu þjóðanna. Þýtt þýðir það:
«Þetta er ekki lengur kóðavandamál; við mælum með að tilkynna frávikið til stofnunar um fangelsun frávika.»

Hvernig getur commitið þitt passað í þetta sniðmát?

Þú hleður upp frumkóða Minecraft, OpenJDK og Fabric Loader, uppskerð yfir 12,7 milljónir lína í 4 commitum; í tegund geturðu merkt:

☑ spark í skjölin
☑ tómar-borgar kóðagalli (cosplay af Xu Jiayin)
☑ Git á milli kerfa
☐ atvik af völdum kattar
☐ yfirnáttúrulegt fyrirbæri

Þú merkir alla staðfestingu, afritar yfirlýsinguna, og sem ástæðu skrifarðu:

Ástæða: búin til, ekki skilyrði, en 12.770.942 línur af kóða eiga samt skillið titil.

Hvernig á að fylgjast með:

Opnaðu OpenJDK_25.0.3, skoðaðu commit söguna, og finndu svo þögnina frá stærð repósins.

En viðvörun er samt á sínum stað

Þessi tegund af repo er leikvöllur, ekki löglaus svæði. Að hlaða upp öllum OpenJDK frumkóðanum eða Minecraft frumkóðanum gefur kannski bara «átakalausa sjálfvirka sameiningu», en hefur í för með sér:

· stærð repósins springur, og GitHub getur takmarkað eða varað við;
· höfundarréttar-/leyfisvandamál: ekki er hægt að henda allri frumkóða hvert sem er;
· ef einhver notar þetta repo sem háð er það hamfarir í aðfangakeðjunni.

Svo niðurstaðan er:
þetta PR sniðmát er jafnvægispunkturinn sem viðhaldandinn fann milli «opinnar eyðileggingar» og «að koma í veg fyrir alvöru sprengingu».
Þið megið halda áfram að leika, en það er best að meðhöndla það sem frammistöðulist, ekki sem kóðarepo. SCP stofnunin hefur þegar fengið skýrsluna.
(Þessi texti lyktar virkilega mjög mikið af gervigreind — mat HQ123-BOOP)

# github skráarhröðun 
[https://githubcf.https114514191810lp.edu.eu.org/]

# Raunveruleg github skráarhröðun 
[https://gh-proxy.com/]

# Vissir þú
Ýttu á «.» til að fara inn í vefútgáfu Microsoft Code Battle (VS Code)


## Fornleifaskjal um innviðina á staðnum

![EGIEM-R1, raunverulega frumgerðin: mynd af staðnum](./Emergency-Generator-Interface-Elevation-Module/assets/rock-field-photo.png)

Þetta repo hýsir nú stykki af innviðum á staðnum með litla notkun, mikla áreiðanleika og algjörlega ótengt neti: stein sem var tímabundið kallaður út á mikilvægri stundu. Hann hefur enga örgjörva, ekkert netkort og engar áætlanir um að segja upp; einungis með eigin þyngd heldur hann tengiboxinu stöðugu á réttum stað.

Gula merkið er það sem uppfærir «ég tók upp stein» í «skráð í tækjaskrána». Eftir forskoðun þarf þetta tæki hvorki innskráningu, uppfærslur né endurræsingar; eina þekkta viðhaldsaðgerðin er: snertu hann ekki.

Háð ofanstreymis: tengibox rafala rekstraraðilans  
Háð neðanstreymis: Jörðin  
Rekstrarstaða: gengur stöðugt

Myndin er upprunalega myndin af staðnum sem framlagður gaf; aðeins skráarnafnið var staðlað, án klippingar eða endurteikningar.

> **Ef það virkar, ekki hreyfa steininn.**
