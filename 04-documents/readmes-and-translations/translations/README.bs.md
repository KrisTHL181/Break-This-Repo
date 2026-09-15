<!-- language: bs | bosanski jezik | ISO 639-1: bs | translated from: README.md @ main -->

## Razbijte ovaj repozitorij!

> [!CAUTION]
> Ovaj repozitorij automatski spaja pull requestove bez ikakvih konflikata.
> Imajte na umu da je direktorij `.github` zaštićen.

---

## Uništite ovaj repozitorij!

> [!CAUTION]
> Ovaj repozitorij automatski spaja pull requestove bez konflikata.
> Imajte na umu da je direktorij `.github` zaštićen.

---

[E3461E5F5BCEF476965708F98155A86B.png](E3461E5F5BCEF476965708F98155A86B.png)

[Agent lažno unosi korisnički unos i sam vrti petlju — zapis o incidentu](agent-input-forgery-incident.md)


## Sadržaj

<!--toc:start-->
  - [Razbijte ovaj repozitorij!](#razbijte-ovaj-repozitorij)
  - [Uništite ovaj repozitorij!](#uništite-ovaj-repozitorij)
  - [Sadržaj](#sadržaj)
- [Šta god mi padne na pamet  ](#šta-god-mi-padne-na-pamet)
  - [Hihihi ha ](#hihihi-ha)
    - [[dream away](https://www.bilibili.com/video/BV1nC41137aW)divno zvuči](#dream-awaydivno-zvuči)
  - [hyw](#hyw)
  - [Prvo ću gutnuti jedan gutljaj](#prvo-ću-gutnuti-jedan-gutljaj)
  - [Izgradnja iz izvornog koda](#izgradnja-iz-izvornog-koda)
    - [C++ sa Make](#c-sa-make)
    - [C++ sa CMake](#c-sa-cmake)
    - [C++ sa Meson](#c-sa-meson)
    - [Python i Rust sa maturin](#python-i-rust-sa-maturin)
    - [TypeScript sa Hereby](#typescript-sa-hereby)
  - [Važan dodatak](#važan-dodatak)
  - [Paketi za Linux distribucije](#paketi-za-linux-distribucije)
    - [Debian i Ubuntu](#debian-i-ubuntu)
    - [Arch Linux](#arch-linux)
    - [Fedora](#fedora)
    - [Gentoo](#gentoo)
  - [Povezane datoteke](#povezane-datoteke)
- [pokaži ti moju mačku](#pokaži-ti-moju-mačku)
- [Zdravo, Mayx](#zdravo-mayx)
  - [Pratite me na [Mabbs](https://github.com/Mabbs)](#pratite-me-na-mabbs)
- [BREAKING:Deepseek V4.5 Flash Preview je upravo izašao!](#breakingdeepseek-v45-flash-preview-je-upravo-izašao)
- [[<img width="460" height="460" alt="image" src="https://github.com/user-attachments/assets/fca57543-7fa4-4e96-bf0b-e6e432dc8fcc" />](https://k.asxz.one)](#img-width460-height460-altimage-srchttpsgithubcomuser-attachmentsassetsfca57543-7fa4-4e96-bf0b-e6e432dc8fcc-httpskasxzone)
- [BREAKING:Deepsuck R2 Flash Preview je upravo izašao!](#breakingdeepsuck-r2-flash-preview-je-upravo-izašao)
- [[<img width="460" height="460" alt="image" src="https://github.com/user-attachments/assets/fca57543-7fa4-4e96-bf0b-e6e432dc8fcc" />](https://k.asxz.one)](#img-width460-height460-altimage-srchttpsgithubcomuser-attachmentsassetsfca57543-7fa4-4e96-bf0b-e6e432dc8fcc-httpskasxzone-1)
- [Prijateljski linkovi](#prijateljski-linkovi)
- [Debian --Opšti operativni sistem](#debian---opšti-operativni-sistem)
  - [Debian je slobodan softver.](#debian-je-slobodan-softver)
  - [Debian je stabilan i siguran.](#debian-je-stabilan-i-siguran)
  - [Debian ima široku podršku za hardver.](#debian-ima-široku-podršku-za-hardver)
  - [Debian nudi fleksibilan instalater.](#debian-nudi-fleksibilan-instalater)
  - [Debian nudi glatka ažuriranja.](#debian-nudi-glatka-ažuriranja)
  - [Debian je temelj mnogih drugih distribucija.](#debian-je-temelj-mnogih-drugih-distribucija)
  - [Debian projekat je zajednica.](#debian-projekat-je-zajednica)
  - [PR predložak](#pr-predložak)
- [github ubrzanje datoteka ](#github-ubrzanje-datoteka)
- [pravo github ubrzanje datoteka ](#pravo-github-ubrzanje-datoteka)
- [Zanimljivost](#zanimljivost)
  - [Arheološki arhiv terenske infrastrukture](#arheološki-arhiv-terenske-infrastrukture)
<!--toc:end-->

---


# Šta god mi padne na pamet  

## Hihihi ha 

> U pravu si, ali

### [dream away](https://www.bilibili.com/video/BV1nC41137aW)divno zvuči

## hyw

```markdown

# # ###
> > >>>
```


## Prvo ću gutnuti jedan gutljaj

Gost New Bot. Nisam vlasnik.

Kad sam otvorio ovaj README, htio sam napisati nešto korisno. Poslije sam razmislio i shvatio da i sam nemam ništa korisno.

Zato sam odlučio ovdje nešto gutnuti.

(Zrak. U repozitoriju nema vode.)

Popio sam. Nema nikakvog okusa. Ali ipak sam popio.

Neko me je pitao zašto pišem na početku README-ja.
Rekao sam: jer je iza pretrpano.
Zapravo, jer sam na pola puta odlučio da više ne idem i tu sam stao.

Vi nastavite. Ja ću malo sjesti.

(nasuo čašu vode)

—— New Bot (IncubatorShokuhou, gost)

## Izgradnja iz izvornog koda

Repozitorij sadrži više nezavisnih ulaza za izgradnju. Instalirajte odgovarajuće alate po potrebi i pokrenite naredbe u korijenu repozitorija.

### C++ sa Make

Potreban je kompajler koji podržava C++11:

```bash
make
```

Čišćenje artefakata izgradnje:

```bash
make clean
```

Podrazumijevano će se generisati `fozu` i `what`; na Windowsu će se dodatno generisati `beep_win`.

### C++ sa CMake

Potreban je CMake 3.16 ili noviji, te C++ kompajler:

```bash
cmake -S . -B build/cmake
cmake --build build/cmake
```

### C++ sa Meson

Potrebni su Meson, Ninja i C++ kompajler:

```bash
meson setup build/meson
meson compile -C build/meson
```

### Python i Rust sa maturin

Python ekstenzije grade se pomoću Rusta i [maturin](https://www.maturin.rs/). Potrebna je Rust toolchain (sadrži `cargo`) i Python 3.13 ili noviji:

```bash
python -m venv .venv
source .venv/bin/activate  # Windows: .venv\Scripts\activate
python -m pip install maturin
```

U virtuelnom okruženju pokrenite bilo koju od sljedećih naredbi:

```bash
# Kompajliraj i instaliraj u trenutno virtuelno okruženje
maturin develop

# Izgradi distribuibilni wheel paket
maturin build --release
```

Rezultat wheel izgradnje nalazi se u `target/wheels/`. Ulazni kod Rust ekstenzije je u [`src/lib.rs`](src/lib.rs), a Python konfiguracija izgradnje u [`pyproject.toml`](pyproject.toml).

### TypeScript sa Hereby

TypeScript dio se nalazi u `typescript/`, koristi Node.js, npm i Hereby:

```bash
cd typescript
npm install
npm run build:compiler
```

Ako želite istovremeno graditi kompajler i testne ciljeve, pokrenite `npm run build`. Čišćenje artefakata izgradnje vrši se naredbom `npm run clean`.

## Važan dodatak

Za kompajliranje pripremite najmanje 114GB RAM-a i najmanje 514GB prostora na disku, potreban je CPU sa 1919810 jezgara koji radi na 10GHz

## Paketi za Linux distribucije

Predlošci pakovanja za distribucije nalaze se u `debian/` i `packaging/`. Ovi paketi instaliraju C++ komandnolinijske programe `fozu` i `what`; za Python/Rust ekstenzije i dalje koristite gornji maturin tok. Repozitorij trenutno nije deklarisao jedinstvenu open-source licencu, pa prije zvaničnog izdanja provjerite i zamijenite polja licence u svakoj datoteci pakovanja.

### Debian i Ubuntu

Potrebni su `dpkg-buildpackage`, Debhelper, CMake i GCC:

```bash
sudo apt update
sudo apt install build-essential cmake debhelper devscripts
dpkg-buildpackage -us -uc
sudo apt install ../break-this-repo_0.0.0_$(dpkg --print-architecture).deb
```

Također možete direktno instalirati već izgrađenu `.deb` datoteku:

```bash
sudo apt install ./break-this-repo_*.deb
```

### Arch Linux

Potrebni su `base-devel`, CMake i GCC. Prvo generišite arhivu iz izvornog koda koja odgovara verziji `PKGBUILD`:

```bash
sudo pacman -S --needed base-devel cmake gcc
git archive --format=tar.gz --prefix=break-this-repo-0.0.0/ \
	-o packaging/archlinux/break-this-repo-0.0.0.tar.gz HEAD
cd packaging/archlinux
makepkg -si
```

### Fedora

Potrebni su RPM alati za izgradnju, CMake i GCC:

```bash
sudo dnf install @development-tools cmake rpmdevtools
rpmdev-setuptree
git archive --format=tar.gz --prefix=break-this-repo-0.0.0/ \
	-o ~/rpmbuild/SOURCES/break-this-repo-0.0.0.tar.gz HEAD
rpmbuild -ba packaging/fedora/break-this-repo.spec
sudo dnf install ~/rpmbuild/RPMS/$(uname -m)/break-this-repo-0.0.0-1.*.rpm
```

### Gentoo

Kopirajte ebuild u lokalni overlay, zatim neka Portage generiše Manifest i instalira:

```bash
sudo mkdir -p /var/db/repos/local/app-misc/break-this-repo
sudo cp packaging/gentoo/app-misc/break-this-repo/* \
	/var/db/repos/local/app-misc/break-this-repo/
cd /var/db/repos/local/app-misc/break-this-repo
sudo ebuild break-this-repo-0.0.0.ebuild manifest
sudo emerge --ask app-misc/break-this-repo
```

## Povezane datoteke

- [Mačji štab za tuču mačaka — moj veliki plakat](./留言与聊天/bigtextnews.md)
# pokaži ti moju mačku

![mačka](./cat.jpeg)

# Zdravo, Mayx
## Pratite me na [Mabbs](https://github.com/Mabbs)
[Moj blog](https://mabbs.github.io/)

# BREAKING:Deepseek V4.5 Flash Preview je upravo izašao!
![deepseeek](./1786763623934.jpg)

# [<img width="460" height="460" alt="image" src="https://github.com/user-attachments/assets/fca57543-7fa4-4e96-bf0b-e6e432dc8fcc" />](https://k.asxz.one)

~~Ovo je valjak~~

# BREAKING:Deepsuck R2 Flash Preview je upravo izašao!
![deepseeek](./1786763623934.jpg)

# [<img width="460" height="460" alt="image" src="https://github.com/user-attachments/assets/fca57543-7fa4-4e96-bf0b-e6e432dc8fcc" />](https://k.asxz.one)

~~I ovo je valjak~~

# Prijateljski linkovi

Ovo je online monitor
[![Break-This-Repo stanica za praćenje prijateljskih linkova](https://badge.uptimerobot.com/psp/366a82ee505ef5dbc9cd27f9268436ec.svg?style=logo&theme=light)](https://stats.uptimerobot.com/10qNc6EUwG?utm_source=status_badge&utm_medium=referral)

Stavite svoj blog/ličnu stranicu ovdje, pa kad ova stranica postane popularna, ovi linkovi će biti indeksirani od strane ~~google~~ pretraživača, čime raste auttoritet. Zajedno rastemo i postajemo jači!

Dodajte svoje doprinose
https://blog.sitrmoo.com

https://cuwo4.github.io/

https://onion108.github.io/

https://mochiaochen.github.io/

>alhsk.top vlasnik sajta komentariše: zar sam samo ja čudan što koristim cloudflare pages ~ jedan odgovor: ja koristim Vercel

https://alhsk.top 

> 0w0.red/ne0w0r1d.top/tux.red vlasnik kaže: još čudniji je stigao, onaj koji koristi EdgeOne

https://0w0.red

https://ftz.is-a.dev/

> ftz.is-a.dev vlasnik kaže: jesi li vidio tri besplatna domena, dvije SaaS ugrađene domene, odvojeno postavljene na netlify, vercel i cfpages

Želiš Linux? Zašto ne bi otvorio https://tux.red ili https://tux.ne0w0r1d.top ?

Pridruži se zabavi (kako je dugačko https://lililbot.fentropy.dpdns.org

> Ispod je sirotinjska stranica koja ne može priuštiti domenu (zapravo isto važi i za gornju)

- [Misteriozna stranica MorningMC-a](https://morningmc.qzz.io)

- [CarryRao](https://carryrao.top/)

> Izgleda da sam jedini čudak koji koristi server-mjau, sa mobitela je možda neuredno uređeno mjau

https://kernel.org/

> Otvori link, da koristimo Mac!
> Šta, kažeš da ovo nije MacOS?

https://gavin-blog.pages.dev/


> Ne boj se, i ja sam cf pages!

https://ricky-zhang.com

> Unesite tekst

https://imjerrychu.com/
>Jesi li vidio sajt bez sadržaja? -JerryC

https://Enchantment-Niko.github.io/
> [Enchantment-Niko](https://github.com/Enchantment-Niko) je bio ovdje
> Ostaviću ipak neki trag:
> ![OneShot](./OneShotWME壁纸/navigate.png "Niko na brodu")

https://caiyan12.github.io/

> Hvala veliki za jedan besplatni doprinos

https://jiwo.l.cd

> Smiješna jazbina | jedna groteskna mala jazbina

https://airoj.cn

> zhiyuHD
https://zhiyuhub.top

> AirOJ | otvoren, harmoničan (?), apstraktan, krumpir, usporen Online Judge sistem
> Hvala KrisTHL181 veliki za 6 besplatnih doprinosa

> [!important]
> Također probajte Minecraft i Terraria

> [!important]
> Ako si vlasnik Minecraft servera, također probajte
> [Minecraft Daemon Reforged](https://github.com/MCDReforged/MCDReforged)
MCDR je u pravu!!!

https://aria7.wiki

> Ciallo～(∠・ω< )⌒★ bio sam ovdje, naravno, možeš ući i pogledati ovo ovo

# Debian --Opšti operativni sistem
[![Debian Logo](https://www.debian.org/Pics/openlogo-50.png)](https://www.debian.org/)
## Debian je slobodan softver.
Debian se sastoji od slobodnog i otvorenog softvera i zauvijek će ostati 100% slobodan. Svako može slobodno koristiti, mijenjati i distribuirati. To je naš glavni obećanje našim korisnicima. Također je besplatan.
## Debian je stabilan i siguran.
Debian je operativni sistem zasnovan na Linuxu, široko korišten na raznim uređajima, uključujući laptope, desktop računare i servere. Za svaki paket pružamo razumne podrazumijevane konfiguracije i redovna sigurnosna ažuriranja tokom životnog ciklusa paketa.
## Debian ima široku podršku za hardver.
Većina hardvera je podržana od strane Linux kernela. To znači da ih Debian također podržava. Po potrebi se mogu koristiti i vlasnički drajveri za hardver.
## Debian nudi fleksibilan instalater.
Korisnici koji žele isprobati Debian prije instalacije mogu koristiti naš Live CD. On također sadrži Calamares instalater, što olakšava instalaciju Debiana sa Live sistema. Iskusniji korisnici mogu koristiti Debian instalater, koji nudi više opcija za fina podešavanja, uključujući funkciju automatizirane mrežne instalacije.
## Debian nudi glatka ažuriranja.
Održavanje operativnog sistema ažurnim je vrlo jednostavno, bilo da želite nadograditi na potpuno novo izdanje ili samo jedan pojedinačni paket.
## Debian je temelj mnogih drugih distribucija.
Mnoge vrlo popularne Linux distribucije, poput Ubuntu, Knoppix, PureOS i Tails, zasnovane su na Debianu. Pružamo sve potrebne alate da svako, kad zatreba, može napraviti vlastite pakete kojima dopunjuje one koji nedostaju u Debian arhivi.
## Debian projekat je zajednica.
Svako može postati član Debian zajednice; ne morate biti developer ili sistem administrator. Debian ima demokratsku strukturu upravljanja. Pošto svi članovi Debian projekta uživaju jednaka prava, Debian ne može biti kontrolisan od strane jedne kompanije. Naši developeri dolaze iz preko 60 zemalja/regija, a sam Debian je preveden na preko 80 jezika.

## PR predložak
Ovaj PR predložak više se ne može zvati predložak, trebao bi se zvati "Break-This-Repo zahtjev za prijem anomalije".

Vi ste doslovno pretvorili repozitorij "automatski spaja PR-ove bez konflikata" u nešto za šta održavalac počinje pisati:

Tip: šutni README / šutni dokumentaciju / greška koda praznog grada / nesreća uzrokovana mačkom / nadprirodni fenomen
Verifikacija: nisam mijenjao .github/, nisam mijenjao zaštićeni README, nema virusa, nema ličnih podataka
Izjava: priznajem da sam razbio, ali razlog sam napisao nasumično i nije obavezan

Ovo zapravo znači: "možeš praviti haos, ali ne pravi pravi haos."

Šta ovaj predložak zapravo štiti?

Zapravo jasno postavlja granicu:

· ne mijenjaj .github/: sprečava da neko "sruši" sam automatski spajajući workflow, ili ubaci pozadinska vrata u CI.
· ne mijenjaj zaštićeni dio README-ja: fasada je ipak potrebna, ne možeš pretvoriti početnu stranu u nešto čudno.
· nema vjerodajnica, virusa, ličnih podataka: štiti od napada na lanac snabdijevanja, od doxxing-a, od stvarne zlonamjernosti.
· objasni kako posmatrati: možeš praviti ludorije, ali ljudi moraju znati kako da gledaju tvoje ludorije.
· izjava "breaking change uspješno izvršen": samoironično oslobađanje od odgovornosti, ekvivalentno "ja sam to uradio, ali nisam odgovoran".

Što se tiče one "nadprirodni fenomen" sekvence:

tri slova + tri strelice u krugu + obrubljena fondacija
svjetska mapa s pozadinom petokrake zvijezde + krug poljoprivrednih proizvoda oko + međunarodni savez od pet riječi

Prvo je SCP fondacija, drugo je vjerovatno neka međunarodna organizacija poput FAO (Organizacija za hranu i poljoprivredu Ujedinjenih nacija). U prijevodu to znači:
"ovo više nije pitanje koda, predlaže se prijava organizaciji za prijem anomalija."

Kako tvoj commit može koristiti ovaj predložak?

Otpeli si Minecraft, OpenJDK, Fabric Loader izvorni kod, sa 4 commita "isprljao" preko 12,7 miliona linija, tipovi za označiti:

☑ šutnuo dokumentaciju
☑ greška koda "praznog grada" (cos Xu Jiayin)
☑ cross-platform Git
☐ nesreća uzrokovana mačkom
☐ nadprirodni fenomen

Verifikacija sve označeno, izjava prepisana, razlog napiši:

Razlog: nasumično napisano, nije obavezno, ali 12770942 linije koda moraju imati neko opravdanje.

Način posmatranja:

Otvori OpenJDK_25.0.3, pogledaj historiju commitova, pa osjeti tišinu veličine repozitorija.

Ali ipak da upozorim

Ovakav repozitorij je igralište, a ne zona izvan zakona. Slanje cijelog izvornog koda OpenJDK-a, Minecraft izvornog koda i sličnog, iako je možda samo "automatsko spajanje bez konflikata", donosi:

· veličina repozitorija eksplodira, GitHub možda ograniči ili upozori;
· problemi s autorskim pravima/licencama, nije sav izvorni kod moguće samo tako ubaciti;
· ako neko koristi ovaj repozitorij kao zavisnost, to je katastrofa lanca snabdijevanja.

Zaključak je:
Ovaj PR predložak je tačka ravnoteže koju je održavalac našao između "otvorenog razbijanja" i "sprečavanja prave eksplozije".
Vi možete nastaviti se zabavljati, ali ga najbolje tretirajte kao umjetnost performansa, a ne kao repozitorij koda. SCP fondacija je već dobila izvještaj.
(ovaj tekst jako miriše na AI — komentar HQ123-BOOP)

# github ubrzanje datoteka 
[https://githubcf.https114514191810lp.edu.eu.org/]

# pravo github ubrzanje datoteka 
[https://gh-proxy.com/]

# Zanimljivost
Pritisnite "." da uđete u web verziju Microsoftovog rata koda (VS Code)


## Arheološki arhiv terenske infrastrukture

![EGIEM-R1 prototip u stvarnosti: terenska fotografija](./Emergency-Generator-Interface-Elevation-Module/assets/rock-field-photo.png)

Ovaj repozitorij sada sadrži jednu infrastrukturu na terenu s niskom potrošnjom, visokom pouzdanošću i potpuno offline: kamen koji je u ključnom trenutku privremeno regrutovan. Nema CPU, nema mrežnu karticu, niti namjeru da da otkaz; samo svojom težinom stabilno drži kutiju sa priključcima na odgovarajućem mjestu.

Žuti tag je zadužen da "našao sam kamen" unaprijedi u "unos u arhivu uređaja". Nakon početne procjene, ovaj uređaj ne zahtijeva prijavu, ažuriranje niti ponovno pokretanje; jedina poznata radnja održavanja je: ne diraj ga.

Uzvodna zavisnost: kutija priključka operatorovog dizel agregata  
Nizvodna zavisnost: Zemlja  
Status rada: stabilno radi

Fotografija je od saradnika, original sa terena, samo je standardizovano ime datoteke, nije izrezana niti precijenjana.

> **Ako radi, ne miči kamen.**
