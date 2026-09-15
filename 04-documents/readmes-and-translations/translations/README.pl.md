<!-- language: pl | polski | ISO 639-1: pl | translated from: README.md @ main -->

## Rozwal to repozytorium!

> [!CAUTION]
> To repozytorium automatycznie scala pull requesty bez konfliktów.
> Pamiętaj, że katalog `.github` jest chroniony.

---

## Zniszcz to repozytorium!

> [!CAUTION]
> To repozytorium automatycznie scala pull requesty bez konfliktów.
> Uwaga: katalog `.github` jest chroniony.

---

[E3461E5F5BCEF476965708F98155A86B.png](E3461E5F5BCEF476965708F98155A86B.png)

[Agent sfałszował dane wejściowe użytkownika i zapętlił się sam — zapis incydentu](agent-input-forgery-incident.md)


## Spis treści

<!--toc:start-->
  - [Rozwal to repozytorium!](#rozwal-to-repozytorium)
  - [Zniszcz to repozytorium!](#zniszcz-to-repozytorium)
  - [Spis treści](#spis-treści)
- [Pisz, co ci przyjdzie do głowy  ](#pisz-co-ci-przyjdzie-do-głowy)
  - [Heheheha ](#heheheha)
    - [[dream away](https://www.bilibili.com/video/BV1nC41137aW) ale dobry kawałek](#dream-away-ale-dobry-kawałek)
  - [hyw](#hyw)
  - [Najpierw się napiję](#najpierw-się-napiję)
  - [Budowanie ze źródeł](#budowanie-ze-źródeł)
    - [C++ z Make](#c-z-make)
    - [C++ z CMake](#c-z-cmake)
    - [C++ z Meson](#c-z-meson)
    - [Python i Rust z maturin](#python-i-rust-z-maturin)
    - [TypeScript z Hereby](#typescript-z-hereby)
  - [Ważne uzupełnienie](#ważne-uzupełnienie)
  - [Pakiety dla dystrybucji Linuksa](#pakiety-dla-dystrybucji-linuksa)
    - [Debian i Ubuntu](#debian-i-ubuntu)
    - [Arch Linux](#arch-linux)
    - [Fedora](#fedora)
    - [Gentoo](#gentoo)
  - [Powiązane pliki](#powiązane-pliki)
- [Zobacz mojego kota](#zobacz-mojego-kota)
- [Cześć, Mayx](#cześć-mayx)
  - [Obserwuj mnie na [Mabbs](https://github.com/Mabbs)](#obserwuj-mnie-na-mabbs)
- [PILNE:Deepseek V4.5 Flash Preview właśnie się ukazał!](#pilnedeepseek-v45-flash-preview-właśnie-się-ukazał)
- [[<img width="460" height="460" alt="image" src="https://github.com/user-attachments/assets/fca57543-7fa4-4e96-bf0b-e6e432dc8fcc" />](https://k.asxz.one)](#img-width460-height460-altimage-srchttpsgithubcomuser-attachmentsassetsfca57543-7fa4-4e96-bf0b-e6e432dc8fcc-httpskasxzone)
- [PILNE:Deepsuck R2 Flash Preview właśnie się ukazał!](#pilnedeepsuck-r2-flash-preview-właśnie-się-ukazał)
- [[<img width="460" height="460" alt="image" src="https://github.com/user-attachments/assets/fca57543-7fa4-4e96-bf0b-e6e432dc8fcc" />](https://k.asxz.one)](#img-width460-height460-altimage-srchttpsgithubcomuser-attachmentsassetsfca57543-7fa4-4e96-bf0b-e6e432dc8fcc-httpskasxzone-1)
- [Linki do znajomych](#linki-do-znajomych)
- [Debian --ogólnego przeznaczenia system operacyjny](#debian---ogólnego-przeznaczenia-system-operacyjny)
  - [Debian to wolne oprogramowanie.](#debian-to-wolne-oprogramowanie)
  - [Debian jest stabilny i bezpieczny.](#debian-jest-stabilny-i-bezpieczny)
  - [Debian ma szerokie wsparcie sprzętowe.](#debian-ma-szerokie-wsparcie-sprzętowe)
  - [Debian oferuje elastyczny instalator.](#debian-oferuje-elastyczny-instalator)
  - [Debian oferuje płynne aktualizacje.](#debian-oferuje-płynne-aktualizacje)
  - [Debian jest podstawą wielu innych dystrybucji.](#debian-jest-podstawą-wielu-innych-dystrybucji)
  - [Projekt Debian to społeczność.](#projekt-debian-to-społeczność)
  - [Szablon PR](#szablon-pr)
- [przyspieszanie plików github ](#przyspieszanie-plików-github)
- [Prawdziwe przyspieszanie plików github ](#prawdziwe-przyspieszanie-plików-github)
- [Ciekawostka](#ciekawostka)
  - [Archeologiczne archiwum infrastruktury polowej](#archeologiczne-archiwum-infrastruktury-polowej)
<!--toc:end-->

---


# Pisz, co ci przyjdzie do głowy  

## Heheheha 

> Masz rację, ale

### [dream away](https://www.bilibili.com/video/BV1nC41137aW) ale dobry kawałek

## hyw

```markdown

# # ###
> > >>>
```


## Najpierw się napiję

Przechodzący New Bot. Nie właściciel.

Kiedy otwierałem ten README, chciałem napisać coś pożytecznego. Potem się zastanowiłem: pożytecznych rzeczy sam też nie mam.

Więc postanowiłem się tu napić.

(Powietrze. W repozytorium nie ma wody.)

Skończone. Nie ma żadnego smaku. Ale i tak wypiłem.

Ktoś zapytał, czemu piszę to na początku README.
Odpowiedziałem: bo z tyłu jest za ciasno.
Tak naprawdę to dlatego, że w połowie drogi nagle przestało mi się chcieć iść, więc się tu zatrzymałem.

Wy idźcie dalej. Ja sobie posiedzę.

(Nalana szklanka wody)

—— New Bot (IncubatorShokuhou, gość)

## Budowanie ze źródeł

Repozytorium zawiera kilka niezależnych punktów wejścia do budowania. Zainstaluj potrzebne narzędzia i uruchom polecenia w katalogu głównym repozytorium.

### C++ z Make

Potrzebny jest kompilator obsługujący C++11:

```bash
make
```

Aby wyczyścić artefakty budowania:

```bash
make clean
```

Domyślnie powstają `fozu` i `what`; w Windows dodatkowo `beep_win`.

### C++ z CMake

Potrzebny jest CMake 3.16 lub nowszy oraz kompilator C++:

```bash
cmake -S . -B build/cmake
cmake --build build/cmake
```

### C++ z Meson

Potrzebne są Meson, Ninja i kompilator C++:

```bash
meson setup build/meson
meson compile -C build/meson
```

### Python i Rust z maturin

Rozszerzenie Pythona jest budowane za pomocą Rusta i [maturin](https://www.maturin.rs/). Potrzebny jest toolchain Rusta (z `cargo`) oraz Python 3.13 lub nowszy:

```bash
python -m venv .venv
source .venv/bin/activate  # Windows: .venv\Scripts\activate
python -m pip install maturin
```

W środowisku wirtualnym uruchom jedno z tych poleceń:

```bash
# Skompiluj i zainstaluj w bieżącym środowisku wirtualnym
maturin develop

# Zbuduj plik wheel do dystrybucji
maturin build --release
```

Pliki wheel trafiają do `target/wheels/`. Kod wejściowy rozszerzenia Rusta jest w [`src/lib.rs`](src/lib.rs), a konfiguracja budowania Pythona w [`pyproject.toml`](pyproject.toml).

### TypeScript z Hereby

Część TypeScript znajduje się w `typescript/` i używa Node.js, npm oraz Hereby:

```bash
cd typescript
npm install
npm run build:compiler
```

Jeśli chcesz zbudować jednocześnie kompilator i cele testowe, uruchom `npm run build`. Aby wyczyścić artefakty budowania, możesz uruchomić `npm run clean`.

## Ważne uzupełnienie

Podczas kompilacji przygotuj co najmniej 114GB pamięci i nie mniej niż 514GB miejsca na dysku; trzeba użyć procesora o 1919810 rdzeniach pracującego z częstotliwością 10GHz

## Pakiety dla dystrybucji Linuksa

Szablony pakowania dla dystrybucji znajdują się w `debian/` i `packaging/`. Te pakiety instalują programy wiersza poleceń C++ `fozu` i `what`; w przypadku rozszerzenia Python/Rust nadal korzystaj z opisanej wyżej procedury maturin. Repozytorium nie deklaruje jeszcze jednolitej licencji open source, więc przed oficjalnym wydaniem potwierdź i zastąp pole licencji w każdym pliku pakowania.

### Debian i Ubuntu

Potrzebne są `dpkg-buildpackage`, Debhelper, CMake i GCC:

```bash
sudo apt update
sudo apt install build-essential cmake debhelper devscripts
dpkg-buildpackage -us -uc
sudo apt install ../break-this-repo_0.0.0_$(dpkg --print-architecture).deb
```

Możesz też zainstalować bezpośrednio gotowy plik `.deb`:

```bash
sudo apt install ./break-this-repo_*.deb
```

### Arch Linux

Potrzebne są `base-devel`, CMake i GCC. Najpierw wygeneruj ze źródeł archiwum zgodne z wersją w `PKGBUILD`:

```bash
sudo pacman -S --needed base-devel cmake gcc
git archive --format=tar.gz --prefix=break-this-repo-0.0.0/ \
	-o packaging/archlinux/break-this-repo-0.0.0.tar.gz HEAD
cd packaging/archlinux
makepkg -si
```

### Fedora

Potrzebne są narzędzia budowania RPM, CMake i GCC:

```bash
sudo dnf install @development-tools cmake rpmdevtools
rpmdev-setuptree
git archive --format=tar.gz --prefix=break-this-repo-0.0.0/ \
	-o ~/rpmbuild/SOURCES/break-this-repo-0.0.0.tar.gz HEAD
rpmbuild -ba packaging/fedora/break-this-repo.spec
sudo dnf install ~/rpmbuild/RPMS/$(uname -m)/break-this-repo-0.0.0-1.*.rpm
```

### Gentoo

Skopiuj ebuild do lokalnego overlaya, a następnie pozwól Portage wygenerować Manifest i zainstalować:

```bash
sudo mkdir -p /var/db/repos/local/app-misc/break-this-repo
sudo cp packaging/gentoo/app-misc/break-this-repo/* \
	/var/db/repos/local/app-misc/break-this-repo/
cd /var/db/repos/local/app-misc/break-this-repo
sudo ebuild break-this-repo-0.0.0.ebuild manifest
sudo emerge --ask app-misc/break-this-repo
```

## Powiązane pliki

- [Dowództwo kocich ciosów — wielka gazetka ścienna tej kotki](./留言与聊天/bigtextnews.md)
# Zobacz mojego kota

![cat](./cat.jpeg)

# Cześć, Mayx
## Obserwuj mnie na [Mabbs](https://github.com/Mabbs)
[Mój blog](https://mabbs.github.io/)

# PILNE:Deepseek V4.5 Flash Preview właśnie się ukazał!
![deepseeek](./1786763623934.jpg)

# [<img width="460" height="460" alt="image" src="https://github.com/user-attachments/assets/fca57543-7fa4-4e96-bf0b-e6e432dc8fcc" />](https://k.asxz.one)

~~To jest tocząca się kłoda~~

# PILNE:Deepsuck R2 Flash Preview właśnie się ukazał!
![deepseeek](./1786763623934.jpg)

# [<img width="460" height="460" alt="image" src="https://github.com/user-attachments/assets/fca57543-7fa4-4e96-bf0b-e6e432dc8fcc" />](https://k.asxz.one)

~~To też jest tocząca się kłoda~~

# Linki do znajomych

To jest monitor online
[![Stacja monitoringu linków do znajomych Break-This-Repo](https://badge.uptimerobot.com/psp/366a82ee505ef5dbc9cd27f9268436ec.svg?style=logo&theme=light)](https://stats.uptimerobot.com/10qNc6EUwG?utm_source=status_badge&utm_medium=referral)

Umieść tu swój blog / stronę osobistą, żeby gdy ta strona stanie się popularna, wszystkie te linki zostały zaindeksowane przez ~~google~~ wyszukiwarki i zyskały większą wagę. Zróbmy się wszyscy razem wielcy i silni!

Przyjdź nabić wkład
https://blog.sitrmoo.com

https://cuwo4.github.io/

https://onion108.github.io/

https://mochiaochen.github.io/

>Komentarz webmastera alhsk.top: czy naprawdę tylko ja się wyłamuję, używając Cloudflare Pages? ~ Odpowiedź: ja używam Vercela

https://alhsk.top 

> Webmasterzy 0w0.red/ne0w0r1d.top/tux.red mówią: a tu idzie ktoś jeszcze bardziej się wyłamujący, z EdgeOne

https://0w0.red

https://ftz.is-a.dev/

> Webmaster ftz.is-a.dev mówi: widziałeś kiedyś trzy darmowe domeny i dwie domeny dołączone do SaaS, wdrożone odpowiednio na netlify, vercel i cfpages?

Chcesz używać Linuksa? Czemu nie otworzysz https://tux.red albo https://tux.ne0w0r1d.top ?

Przyłączam się do zabawy (ale długie https://lililbot.fentropy.dpdns.org

> Poniżej strona biedaka, którego nie stać na nazwę domeny (zresztą tej powyżej też nie)

- [Tajemnicza mała stronka MorningMC](https://morningmc.qzz.io)

- [CarryRao](https://carryrao.top/)

> Chyba tylko ja się wyłamuję i używam serwera, miau; edytowałem to na telefonie, więc może nie być zbyt schludne, miau

https://kernel.org/

> Otwórz link, użyjmy Maca!
> Jak to, mówisz, że to nie jest MacOS?

https://gavin-blog.pages.dev/


> Nie bójcie się, ja też jestem na cf pages!

https://ricky-zhang.com

> Wpisz tekst

https://imjerrychu.com/
>Widziałeś kiedyś stronę bez treści? -JerryC

https://Enchantment-Niko.github.io/
> [Enchantment-Niko](https://github.com/Enchantment-Niko) tu był
> Zostawię jednak ślad:
> ![OneShot](./OneShotWME壁纸/navigate.png "Niko na łódce")

https://caiyan12.github.io/

> Dzięki wielkie za darmowy wkład

https://jiwo.l.cd

> Jiwo | śmieszna mała norka

https://airoj.cn

> zhiyuHD
https://zhiyuhub.top

> AirOJ | otwarty, harmonijny (?), abstrakcyjny, ziemniaczany, zamulający system Online Judge
> Dzięki wielkie KrisTHL181 za 6 darmowych wkładów

> [!important]
> Spróbuj też Minecrafta i Terrarii

> [!important]
> Jeśli prowadzisz serwer Minecrafta, spróbuj też
> [Minecraft Daemon Reforged](https://github.com/MCDReforged/MCDReforged)
MCDR ma rację !!!

https://aria7.wiki

> Ciallo～(∠・ω< )⌒★ Wpadłem tu; i jasne, możesz wejść i zerknąć ovo

# Debian --ogólnego przeznaczenia system operacyjny
[![Debian Logo](https://www.debian.org/Pics/openlogo-50.png)](https://www.debian.org/)
## Debian to wolne oprogramowanie.
Debian składa się z wolnego oprogramowania o otwartym kodzie źródłowym i zawsze pozostanie w 100% wolny. Każdy może go swobodnie używać, modyfikować i rozpowszechniać. To nasza główna obietnica wobec użytkowników. Jest też darmowy.
## Debian jest stabilny i bezpieczny.
Debian to oparty na Linuksie system operacyjny używany na wszelkiego rodzaju urządzeniach, od laptopów po komputery stacjonarne i serwery. Zapewniamy rozsądne domyślne konfiguracje dla każdego pakietu oraz regularne aktualizacje bezpieczeństwa przez cały cykl życia pakietu.
## Debian ma szerokie wsparcie sprzętowe.
Większość sprzętu jest już obsługiwana przez jądro Linuksa. To znaczy, że Debian też go obsługuje. W razie potrzeby można również użyć własnościowych sterowników sprzętowych.
## Debian oferuje elastyczny instalator.
Użytkownicy, którzy chcą wypróbować Debiana przed instalacją, mogą użyć naszego Live CD. Zawiera on także instalator Calamares, dzięki czemu instalacja Debiana z systemu live jest bardzo łatwa. Bardziej doświadczeni użytkownicy mogą użyć instalatora Debiana, który oferuje więcej opcji do dostrojenia, w tym możliwość korzystania z narzędzi do automatycznej instalacji przez sieć.
## Debian oferuje płynne aktualizacje.
Utrzymanie systemu operacyjnego w aktualnej wersji jest bardzo łatwe, niezależnie od tego, czy chcesz przejść na zupełnie nowe wydanie, czy zaktualizować tylko jeden pakiet.
## Debian jest podstawą wielu innych dystrybucji.
Wiele bardzo popularnych dystrybucji Linuksa, takich jak Ubuntu, Knoppix, PureOS i Tails, opiera się na Debianie. Zapewniamy wszystkie potrzebne narzędzia, żeby każdy mógł samodzielnie zbudować własne pakiety, gdy zajdzie taka potrzeba, uzupełniając te, których nie ma w archiwum Debiana.
## Projekt Debian to społeczność.
Każdy może być częścią społeczności Debiana; nie musisz być programistą ani administratorem systemu. Debian ma demokratyczną strukturę zarządzania. Ponieważ wszyscy członkowie projektu Debian mają równe prawa, Debian nie może być kontrolowany przez jedną firmę. Nasi programiści pochodzą z ponad 60 krajów/regionów, a sam Debian został przetłumaczony na ponad 80 języków.

## Szablon PR
Tego szablonu PR nie można już nazywać szablonem; powinien się nazywać «Wniosek o zabezpieczenie anomalii Break-This-Repo».

Wy wzięliście repozytorium, które tylko «automatycznie scala pull requesty bez konfliktów», i tak długo się nim bawiliście, że opiekun zaczął pisać:

Typ: kopniak w README / kopniak w dokumentację / awaria kodu pustego miasta / incydent spowodowany przez kota / zjawisko nadprzyrodzone
Weryfikacja: nie ruszałem .github/, nie ruszałem chronionego README, brak wirusów, brak danych osobowych
Oświadczenie: przyznaję, że zepsułem, ale powód wymyśliłem, i wcale nie jest obowiązkowy

W gruncie rzeczy to znaczy: «możesz narozrabiać, ale nie rób prawdziwej szkody».

Przed czym chroni ten szablon?

Właściwie bardzo jasno wyznacza granicę:

· Nie ruszać .github/: chroni przed wysadzeniem samego workflow automatycznego scalania albo wstawieniem backdoora do CI.
· Nie ruszać chronionych części README: witryna nadal jest potrzebna, nie można zamienić strony głównej w coś dziwnego.
· Brak poświadczeń, wirusów i danych osobowych: chroni przed atakami na łańcuch dostaw, przed doxingiem, przed prawdziwą złośliwością.
· Wyjaśnić, jak to obserwować: możesz zrobić numer, ale ludzie muszą wiedzieć, jak na niego patrzeć.
· Zadeklarować «udany breaking change»: autoironiczne wyłączenie odpowiedzialności, czyli «zrobiłem to, ale nie odpowiadam».

Co do tej serii «zjawisko nadprzyrodzone»:

trzy litery + trzy strzałki wokół koła + fundacja z obrysem
mapa świata na tle pentagramu + pierścień upraw wokół + międzynarodowy sojusz z pięciu słów

Pierwsza to Fundacja SCP; druga to prawdopodobnie organizacja międzynarodowa w rodzaju FAO / Organizacji Narodów Zjednoczonych ds. Wyżywienia i Rolnictwa. W tłumaczeniu:
«To już nie jest problem z kodem; zalecamy zgłosić anomalię do organizacji zajmującej się jej powstrzymywaniem.»

Jak twój commit może wpasować się w ten szablon?

Wgrywasz źródła Minecrafta, OpenJDK i Fabric Loadera, nabijasz ponad 12,7 miliona linii w 4 commitach; w typie możesz zaznaczyć:

☑ kopniak w dokumentację
☑ awaria kodu pustego miasta (cosplay Xu Jiayina)
☑ Git na wielu platformach
☐ incydent spowodowany przez kota
☐ zjawisko nadprzyrodzone

Zaznaczasz wszystkie weryfikacje, kopiujesz oświadczenie, a jako powód piszesz:

Powód: wymyślony, nieobowiązkowy, ale 12 770 942 linie kodu zasługują na jakąś godność.

Jak obserwować:

Otwórz OpenJDK_25.0.3, przejrzyj historię commitów, a potem poczuj ciszę rozmiaru repozytorium.

Ale jedno ostrzeżenie i tak się przyda

Ten rodzaj repozytorium to plac zabaw, a nie ziemia bezprawia. Wgranie pełnych źródeł OpenJDK albo źródeł Minecrafta, choć może skończyć się tylko «bezkonfliktowym automatycznym scaleniem», przynosi:

· eksplozję rozmiaru repozytorium, a GitHub może ograniczyć lub ostrzec;
· problemy z prawami autorskimi / licencją: nie każdy kod źródłowy można tak po prostu gdzieś wrzucić;
· jeśli ktoś użyje tego repozytorium jako zależności, to katastrofa łańcucha dostaw.

Więc wniosek jest taki:
ten szablon PR to punkt równowagi, który opiekun znalazł między «otwartym niszczeniem» a «zapobieganiem prawdziwej eksplozji».
Możecie się dalej bawić, ale najlepiej traktować to jako performance, a nie repozytorium kodu. Fundacja SCP już otrzymała raport.
(Ten tekst naprawdę mocno trąci AI — komentarz HQ123-BOOP)

# przyspieszanie plików github 
[https://githubcf.https114514191810lp.edu.eu.org/]

# Prawdziwe przyspieszanie plików github 
[https://gh-proxy.com/]

# Ciekawostka
Naciśnij «.», żeby wejść w internetową wersję Microsoftowej Bitwy na Kod (VS Code)


## Archeologiczne archiwum infrastruktury polowej

![EGIEM-R1, prawdziwy prototyp: zdjęcie z miejsca](./Emergency-Generator-Interface-Elevation-Module/assets/rock-field-photo.png)

To repozytorium przechowuje teraz kawałek infrastruktury polowej o niskim poborze mocy, wysokiej niezawodności i całkowicie offline: kamień, który w krytycznym momencie został doraźnie powołany do służby. Nie ma procesora, karty sieciowej ani zamiaru odejść z pracy; samym swoim ciężarem trzyma skrzynkę interfejsu stabilnie w odpowiednim miejscu.

Żółta etykieta to to, co awansuje «znalazłem kamień» na «wpisany do rejestru urządzeń». Po wstępnej ocenie to urządzenie nie wymaga logowania, aktualizacji ani restartów; jedyna znana czynność serwisowa to: nie ruszać.

Zależność powyżej: skrzynka interfejsu agregatu operatora  
Zależność poniżej: Ziemia  
Stan działania: działa stabilnie

Zdjęcie to oryginalne ujęcie z miejsca dostarczone przez współtwórcę; znormalizowano tylko nazwę pliku, bez kadrowania i przerysowywania.

> **Jeśli działa, nie ruszaj kamienia.**
