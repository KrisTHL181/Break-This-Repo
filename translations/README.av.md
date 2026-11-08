<!-- language: av | авар мацӀ | ISO 639-1: av | translated from: README.md @ main -->

## Гьеб репозиторий бихьизабе!

> [!CAUTION]
> Гьеб репозиторий конфликтаби гьечӀого pull request-ал автоматияб къагӀидаялъ гъоркь бахъула.
> Лъазабила, `.github` директория цӀунун буго.

---

## Гьеб репозиторий лӀугьун бихьизабе!

> [!CAUTION]
> Гьеб репозиторий конфликтаби гьечӀого pull request-ал автоматияб къагӀидаялъ гъоркь бахъула.
> ЛъикӀаб балагь: `.github` директория цӀунун буго.

---

[E3461E5F5BCEF476965708F98155A86B.png](E3461E5F5BCEF476965708F98155A86B.png)

[Agent-ица гӀадамасул инпут гьабун ва жиндиего гьоркьоб цикл гӀуцӀараб — инциденталъул хӀиссат](agent-input-forgery-incident.md)


## Мугъ

<!--toc:start-->
  - [Гьеб репозиторий бихьизабе!](#гьеб-репозиторий-бихьизабе)
  - [Гьеб репозиторий лӀугьун бихьизабе!](#гьеб-репозиторий-лӏугьун-бихьизабе)
  - [Мугъ](#мугъ)
- [КӀалъалеб батани гьеб гӀуцӀцӀун хӀалтӀизабе](#кӏалъалеб-батани-гьеб-гӏуцӏцӏун-хӏалтӏизабе)
  - [Хехьехьехьа](#хехьехьехьа)
    - [[dream away](https://www.bilibili.com/video/BV1nC41137aW) цӀакъ бакӀаб ах!](#dream-away-цӏакъ-бакӏаб-ах)
  - [hyw](#hyw)
  - [ЦӀа гьитӀун бахъун лӀугьун кӀалъая](#цӏа-гьитӏун-бахъун-лӏугьун-кӏалъая)
  - [Сорсалдаса билд хӀалтӀизаби](#сорсалдаса-билд-хӏалтӏизаби)
    - [C++ Make-алдалъ](#c-make-алдалъ)
    - [C++ CMake-алдалъ](#c-cmake-алдалъ)
    - [C++ Meson-алдалъ](#c-meson-алдалъ)
    - [Python ва Rust maturin-алдалъ](#python-ва-rust-maturin-алдалъ)
    - [TypeScript Hereby-алдалъ](#typescript-hereby-алдалъ)
  - [КӀвар бугеб добавка](#кӏвар-бугеб-добавка)
  - [Linux дистрибуциязул пакетал](#linux-дистрибуциязул-пакетал)
    - [Debian ва Ubuntu](#debian-ва-ubuntu)
    - [Arch Linux](#arch-linux)
    - [Fedora](#fedora)
    - [Gentoo](#gentoo)
  - [Цогидал файлал](#цогидал-файлал)
- [Дир хӀами бихьизабе](#дир-хӏами-бихьизабе)
- [Салам, Mayx](#салам-mayx)
  - [Заре гӀахьалъи гьабе [Mabbs](https://github.com/Mabbs)](#заре-гӏахьалъи-гьабе-mabbs)
- [BREAKING:Deepseek V4.5 Flash Preview цӀалияб бахъана!](#breakingdeepseek-v45-flash-preview-цӏалияб-бахъана)
- [[<img width="460" height="460" alt="image" src="https://github.com/user-attachments/assets/fca57543-7fa4-4e96-bf0b-e6e432dc8fcc" />](https://k.asxz.one)](#img-width460-height460-altimage-srchttpsgithubcomuser-attachmentsassetsfca57543-7fa4-4e96-bf0b-e6e432dc8fcc-httpskasxzone)
- [BREAKING:Deepsuck R2 Flash Preview цӀалияб бахъана!](#breakingdeepsuck-r2-flash-preview-цӏалияб-бахъана)
- [[<img width="460" height="460" alt="image" src="https://github.com/user-attachments/assets/fca57543-7fa4-4e96-bf0b-e6e432dc8fcc" />](https://k.asxz.one)](#img-width460-height460-altimage-srchttpsgithubcomuser-attachmentsassetsfca57543-7fa4-4e96-bf0b-e6e432dc8fcc-httpskasxzone-1)
- [ЦӀарцӀадал линкал](#цӏарцӏадал-линкал)
- [Debian --гӀадатияб операционная система](#debian---гӏадатияб-операционная-система)
  - [Debian жиндиего бокьи бугеб софтвер буго.](#debian-жиндиего-бокьи-бугеб-софтвер-буго)
  - [Debian тӀехьаб ва цӀунун буго.](#debian-тӏехьаб-ва-цӏунун-буго)
  - [Debian кӀудияб hardware гӀулхъузе гьабула.](#debian-кӏудияб-hardware-гӏулхъузе-гьабула)
  - [Debian гӀемераб installer кьола.](#debian-гӏемераб-installer-кьола)
  - [Debian чӀорого update кьола.](#debian-чӏорого-update-кьола)
  - [Debian гӀемер цогидал дистрибуциязул бижи буго.](#debian-гӏемер-цогидал-дистрибуциязул-бижи-буго)
  - [Debian проект жамагӀат буго.](#debian-проект-жамагӏат-буго)
  - [PR шаблон](#pr-шаблон)
- [github файлалъул ускорение](#github-файлалъул-ускорение)
- [ЦӀакъ бугеб github файлалъул ускорение](#цӏакъ-бугеб-github-файлалъул-ускорение)
- [ЦӀакъго бачӀараб лъагӀел](#цӏакъго-бачӏараб-лъагӏел)
  - [БакӀалъул инфраструктуралъул археологияб архив](#бакӏалъул-инфраструктуралъул-археологияб-архив)
<!--toc:end-->

---


# КӀалъалеб батани гьеб гӀуцӀцӀун хӀалтӀизабе

## Хехьехьехьа

> Гьекъго чӀухӀа буго, амма

### [dream away](https://www.bilibili.com/video/BV1nC41137aW) цӀакъ бакӀаб ах!

## hyw

```markdown

# # ###
> > >>>
```


## ЦӀа гьитӀун бахъун лӀугьун кӀалъая

Турист New Bot. ГӀалимчи гьечӀо.

Дица гьеб README бахъараб заманалда жиндиего кӀвар бугеб щвезабизе ккана. Хадуб дир хӀакъикъаталда данде ккана: кӀвар бугеб щакъалеб дирго гьечӀо.

Гьелъул гьеб бакӀалда цӀа гьитӀун бахъизе щвана.

(Гьаниб хӀава гурони буго. Репозиториялда цӀа гьечӀо.)

ЦӀа бахъана. КӀалъаб вкус гьечӀо. Амма дирго бахъана.

Цо чияс гӀадатизе ккана «РЕАДМЕялъул цередалда щвезабун бугощ?»
Дица ккана: «Хадуб бакӀ цӀцӀикӀаб буго.»
Гьели щияб гьечӀо: дун нухалъул гьоркьоб лӀугьани бачӀун лӀугьунав, гьенибго лӀуго.

Нилъер чӀезабе. Дун цоцаку гӀодобе щвезе щвана.

(Цо стакан цӀа бахъана)

—— New Bot (IncubatorShokuhou, турист)

## Сорсалдаса билд хӀалтӀизаби

Репозиториялда гӀемер цӀцӀикӀаб билдалъул рагӀалал руго. КӀвар бугеб инструмент бачӀун байзулаго, репозиториялъул кӀудияб папкаялда командал хӀалтӀизабе.

### C++ Make-алдалъ

C++11 лъазабулеб компилятор ккола:

```bash
make
```

Билдалъул хӀутӀелал тӀагӀинаризе:

```bash
make clean
```

Default-алдалъ гьарула `fozu` ва `what`; Windows алхӀалалда гьебго `beep_win` гьарула.

### C++ CMake-алдалъ

CMake 3.16 ялда лъагӀидалда кӀудияб версия, ва C++ компилятор ккола:

```bash
cmake -S . -B build/cmake
cmake --build build/cmake
```

### C++ Meson-алдалъ

Meson, Ninja ва C++ компилятор ккола:

```bash
meson setup build/meson
meson compile -C build/meson
```

### Python ва Rust maturin-алдалъ

Python extension-ал гӀуцӀула Rust ва [maturin](https://www.maturin.rs/)-алдалъ. Rust toolchain (гӀуцӀцӀун `cargo`) ва Python 3.13 ялда лъагӀидалда версия ккола:

```bash
python -m venv .venv
source .venv/bin/activate  # Windows: .venv\Scripts\activate
python -m pip install maturin
```

Virtual environment-алда цо-цо командалдаса хӀалтӀизабе:

```bash
# Компиляция ва инсталляция гьанже бихьараб virtual environment-алда
maturin develop

# Распространениялъе кколеб wheel файл гӀуцӀи
maturin build --release
```

wheel билдалъул хӀутӀел `target/wheels/` бакӀалда буго. Rust extension-алъул entry code [`src/lib.rs`](src/lib.rs)-алда, Python билдалъул конфигурация [`pyproject.toml`](pyproject.toml)-алда буго.

### TypeScript Hereby-алдалъ

TypeScript бутӀа бачӀого `typescript/`-алда, Node.js, npm ва Hereby хӀалтӀизабун:

```bash
cd typescript
npm install
npm run build:compiler
```

Compiler ва test target цоялдасаго билд гьабизе ккани, `npm run build` хӀалтӀизабе. Билдалъул хӀутӀелал тӀагӀинаризе `npm run clean` хӀалтӀизабун буго.

## КӀвар бугеб добавка

Билд хӀалтӀизаби заманалда бижун байзе ккола бищун 114GB RAM ва 514GB ялда цӀикӀкӀун storage, ва 1919810 яруц CPU 10GHz алхӀалалда хӀалтӀизабизе ккола

## Linux дистрибуциязул пакетал

Дистрибуциязул packaging шаблонал бачӀого `debian/` ва `packaging/`. Гьеб пакетал гӀуцӀцӀула C++ command line программа `fozu` ва `what`; Python/Rust extension-алъул версиялъе тӀасанияв гурони maturin процесс хӀалтӀизабе. Репозиториялда гӀагарлъи гьечӀого цояб open source license малъичӀо, расмияб релизалда цебе цогидаб packaging файлазда бугеб license поле балагьун, хисизабе.

### Debian ва Ubuntu

`dpkg-buildpackage`, Debhelper, CMake ва GCC ккола:

```bash
sudo apt update
sudo apt install build-essential cmake debhelper devscripts
dpkg-buildpackage -us -uc
sudo apt install ../break-this-repo_0.0.0_$(dpkg --print-architecture).deb
```

Гьебго билд гьабураб `.deb` файл гӀадатияб къагӀидаялъго инсталлинабизе бегьула:

```bash
sudo apt install ./break-this-repo_*.deb
```

### Arch Linux

`base-devel`, CMake ва GCC ккола. Ахирисеб, сорсалдаса `PKGBUILD` версиялде данде кколеб archive файл гӀуцӀе:

```bash
sudo pacman -S --needed base-devel cmake gcc
git archive --format=tar.gz --prefix=break-this-repo-0.0.0/ \
	-o packaging/archlinux/break-this-repo-0.0.0.tar.gz HEAD
cd packaging/archlinux
makepkg -si
```

### Fedora

RPM build инструментал, CMake ва GCC ккола:

```bash
sudo dnf install @development-tools cmake rpmdevtools
rpmdev-setuptree
git archive --format=tar.gz --prefix=break-this-repo-0.0.0/ \
	-o ~/rpmbuild/SOURCES/break-this-repo-0.0.0.tar.gz HEAD
rpmbuild -ba packaging/fedora/break-this-repo.spec
sudo dnf install ~/rpmbuild/RPMS/$(uname -m)/break-this-repo-0.0.0-1.*.rpm
```

### Gentoo

ebuild локальный overlay-алде копия гьабе, хадуб Portage-ие Manifest гӀуцӀизе ва инсталлинабизе байзе:

```bash
sudo mkdir -p /var/db/repos/local/app-misc/break-this-repo
sudo cp packaging/gentoo/app-misc/break-this-repo/* \
	/var/db/repos/local/app-misc/break-this-repo/
cd /var/db/repos/local/app-misc/break-this-repo
sudo ebuild break-this-repo-0.0.0.ebuild manifest
sudo emerge --ask app-misc/break-this-repo
```

## Цогидал файлал

- [Котёнок командалъул штаб — дир хӀамиялъул цо батӀияб хӀакъикъат](./留言与聊天/bigtextnews.md)
# Дир хӀами бихьизабе

![хӀами](./cat.jpeg)

# Салам, Mayx
## Заре гӀахьалъи гьабе [Mabbs](https://github.com/Mabbs)
[Дир блог](https://mabbs.github.io/)

# BREAKING:Deepseek V4.5 Flash Preview цӀалияб бахъана!
![deepseeek](./1786763623934.jpg)

# [<img width="460" height="460" alt="image" src="https://github.com/user-attachments/assets/fca57543-7fa4-4e96-bf0b-e6e432dc8fcc" />](https://k.asxz.one)

~~Гьаб кьурдулеб гъотӀ~~

# BREAKING:Deepsuck R2 Flash Preview цӀалияб бахъана!
![deepseeek](./1786763623934.jpg)

# [<img width="460" height="460" alt="image" src="https://github.com/user-attachments/assets/fca57543-7fa4-4e96-bf0b-e6e432dc8fcc" />](https://k.asxz.one)

~~Гьабги кьурдулеб гъотӀ~~

# ЦӀарцӀадал линкал

Гьеб online монитор буго
[![Break-This-Repo-алъул цӀарцӀадал линкал монитор](https://badge.uptimerobot.com/psp/366a82ee505ef5dbc9cd27f9268436ec.svg?style=logo&theme=light)](https://stats.uptimerobot.com/10qNc6EUwG?utm_source=status_badge&utm_medium=referral)

Жиндир блог/личный сайт гьениб лъазе, гьелъул гьеб сайт тӀехьаблъани, гьеб линкал ~~google~~ поисковый двигатель-алде индексациялда лъугьуна, ва гьелъул вес букӀуна. ЦохӀо цо-цо гӀемерлъун ва кӀудиялъун лӀугье!

Contribution бахъун щвезе!
https://blog.sitrmoo.com

https://cuwo4.github.io/

https://onion108.github.io/

https://mochiaochen.github.io/

> alhsk.top сайталъул admin хӀакъикъат: дун гьоркьоб цого cloudflare pages хӀалтӀизабулев дица гурони щин гьечӀо ~ цо жаваб: дун Vercel хӀалтӀизабула

https://alhsk.top 

> 0w0.red/ne0w0r1d.top/tux.red admin баян гьабула: гьоркьоб цӀцӀикӀаб гӀадин EdgeOne хӀалтӀизабурав вачӀана

https://0w0.red

https://ftz.is-a.dev/

> ftz.is-a.dev admin баян гьабула: лъалаго лъала, цо-цо чияс 3 free domain ва 2 SaaS-алъулго domain netlify, vercel, cfpages-алда батӀияб батӀияб байжаралъун хӀалтӀизарурал?

Linux хӀалтӀизабизе щвезе? Инсу щив? https://tux.red ялда https://tux.ne0w0r1d.top балаго?

Цо чиясдаса гӀадин щвезе (цӀакъ батӀияб https://lililbot.fentropy.dpdns.org

> Гьаниб буго квертишең чиясул сайт, домен цӀаралъе ахӀа тӀолеб гьечӀо (гьабур щияб кӀудияб гьечӀого)

- [MorningMC-алъул гьитӀинаб цӀакъ бачӀараб сайт](https://morningmc.qzz.io)

- [CarryRao](https://carryrao.top/)

> Гьоркьоб дун цого сервер хӀалтӀизабулев вуго, телефоналдаса хисанаб букӀиналъул кӀвар бугеб гьечӀо.

https://kernel.org/

> Линк базе, цода Mac хӀалтӀизабе!
> Щив? Гьеб MacOS гьечӀин щив?

https://gavin-blog.pages.dev/


> БекьичӀого, дунго cf pages буго!

https://ricky-zhang.com

> Текст лъазе буго

https://imjerrychu.com/
> Контент гьечӀеб сайт балаго? -JerryC

https://Enchantment-Niko.github.io/
> [Enchantment-Niko](https://github.com/Enchantment-Niko) гьениб щвезабун вуго
> Дунго цо белег хутӀизе щвана:
> ![OneShot](./OneShotWME壁纸/navigate.png "Niko корабалда")

https://caiyan12.github.io/

> Цо бесплатный contribution кьуралъул бетӀерлъи гьабуле вацасда рахӀмат

https://jiwo.l.cd

> Цо гьитӀинаб хахачаб гьарак

https://airoj.cn

> zhiyuHD
https://zhiyuhub.top

> AirOJ | открытый, гӀолохъаналъул (?), абстрактный, картофель, чӀорого чӀезабулеб Online Judge система
> KrisTHL181 вацас кьурал 6 бесплатный contribution-алъул рахӀмат

> [!important]
> Minecraft ва Terrariaго мунхидалъун хӀалтӀизабе

> [!important]
> Мун Minecraft Server-алъул бетӀерлъи гьабулеав вугилан, мунхидалъун балаго
> [Minecraft Daemon Reforged](https://github.com/MCDReforged/MCDReforged)
MCDR чӀухӀа буго!!!

https://aria7.wiki

> Ciallo～(∠・ω< )⌒★ гьениб щвезабун вуго, гьабуге, гьениб щвезе балаго ovo

# Debian --гӀадатияб операционная система
[![Debian логотип](https://www.debian.org/Pics/openlogo-50.png)](https://www.debian.org/)
## Debian жиндиего бокьи бугеб софтвер буго.
Debian — жиндиего бокьи бугеб ва открытый source code бугеб софтвер буго, ва гьелъ гьоркьоб 100% бокьи бугеблъун кӀолодоб буго. Цо-цо чиясго бокьи бугеб къагӀидаялъ хӀалтӀизабизе, хисизабизе ва тарагъизе бегьула. Гьеб дидаго гӀадамасдаса кӀвар бугеб обещание буго. Гьебго бесплатный буго.
## Debian тӀехьаб ва цӀунун буго.
Debian — гӀемер батӀияб устройствоалда хӀалтӀизабулеб Linux-алда бугеб операционная система буго, гьелъул хӀалтӀизаби бачӀого ноутбукал, компьютер ва сервер. Дидаго цо-цо package-алъе кӀвар бугеб default конфигурация кьола, ва package-алъул жизненный циклалда security update-ал кьола.
## Debian кӀудияб hardware гӀулхъузе гьабула.
ГӀемер hardware Linux kernel-алдаса гӀулхъузе гьабун буго. Гьелъул магӀна буго Debianго гьезда гӀулхъузе гьабула. КӀвар бугеблъани, проприетарный hardware driver хӀалтӀизабун бегьула.
## Debian гӀемераб installer кьола.
Debian инсталлинабизе цебе балаго чиясда Debian live CD хӀалтӀизабун бегьула. Гьеб Calamares installer лъикӀабго гъоркьоб буго, гьелъул live системаялдаса Debian инсталлинабизе кӀеларо батӀияб гьабула. КӀвар бугеб опыт бугеб чияс Debian installer хӀалтӀизабун бегьула, гьебго гӀемер fine-tuning option-ал кьола, automation сеть инсталляция инструмент хӀалтӀизабизе кучӀаб.
## Debian чӀорого update кьола.
Операционная система тӀасияб букӀиналде бахъин кӀеларо батӀияб буго, цӀияб release версиялдего лищизе ккани ялда цо-цо package-алдего лищизе ккани.
## Debian гӀемер цогидал дистрибуциязул бижи буго.
ГӀемер кӀвар бугеб Linux дистрибуциял, мисалалъе Ubuntu, Knoppix, PureOS ва Tails, Debian-алда буго. Дидаго кӀвар бугеб цо-цо инструментал кьоло, гьелъул цо-цо чиясда кӀвар бугеб заманалда жиндирго package гӀуцӀизе бегьула, Debian archive-алда гьечӀеб package-ал дополнение гьабизе.
## Debian проект жамагӀат буго.
Цо-цо чияс Debian жамагӀаталъул гӀахьалъи лӀугьине бегьула; мун developer ялда системный administrator вукиналъе кӀвар гьечӀо. Debian демократический governance структура буго. Debian проекталъул цо-цо гӀахьалчиясго бакӀарунисеб ихтияр буго, гьелъул Debian цо-цо компаниялъ гӀуцӀцӀун бачӀунге ккола. Дидаго developers 60-ялда цӀикӀкӀун улкаялдаса руго, ва Debian гьебго 80-ялда цӀикӀкӀун мацӀалде перевод гьабун буго.

## PR шаблон
Гьеб PR шаблон шаблон абуниги абураблъун кколарев, гьелъие «Break-This-Repo аномалиялъул containment заявление» абуни кӀвар буго.

Нилъеда гӀадал гӀадамаз гӀадатияб "конфликт гьечӀого PR автоматияб гъоркьбахъи" репозиторий гьитӀун, maintainer-аз хъвая гьабулеб бакӀалде лӀугьинаруна:

Тип: README бихьизаби / документ бихьизаби / "пустой город" code авария / хӀамица ккана авария / сверхъестественный явление
Верификация: дун .github/ хисанарев, цӀунун бугеб README хисанарев, вирус гьечӀо, личный информация гьечӀо
Декларация: дун бихьизабун вугоян лъазабула, амма сабаб дун гьитӀун хъвана, ва гьеб бугеблияб гьечӀо

Гьеб аслиялда гьеб буго: «Дун бихьизабизе бегьула, амма аслияб бихьизаби ма гьабе».

Гьеб шаблон щив щун буго?

Гьелъ аслуяб линия цӀакъ балаго бахъун буго:

· .github/ ма хисе: цо-цо чияс автоматияб гъоркьбахъи workflow жинго тӀагӀинабизе ялда CI-алда backdoor лъезабизе щун.
· ЦӀунун бугеб README бутӀа ма хисе: фасад букӀине ккола, ахирисеб страница цӀакъ батӀияблъун ма хисизабе.
· Креденциалал гьечӀого, вирус гьечӀого, личный информация гьечӀого: supply chain атака, doxxing, аслияб malicious щун.
· Щив балаго щвезабун букӀини щвезабе: дун гьитӀун хӀалтӀизабизе бегьула, амма цо-цо чиязе дун гьитӀун хӀалтӀизарураб балаго кканиге.
· «breaking change рицун лӀугьана» абураб декларация: жиндиего хӀалтӀизабун квел гьаби, магӀна «дун гьабуна, амма дун жаваб кьоларо» гӀадин.

«Сверхъестественный явление» абураб бутӀаялде къосараб:

Лъабго хъвахӀи + къагӀаналъул гьоркьоб лъабго стрела + контур бугеб фонд
5-гӀанасеб сусузе world map фон + гӀоркьоб цо кольцо crop растения + 5 ваццазулго интернациональный альянс

Цебесеб SCP фонд буго, хадуреб гӀемерго ООН-алъул FAO гӀадинаб international организация буго. Перевод гьабун буго:
«Гьеб already code проблема гьечӀо, аномалиялъул containment организациялде репорт хӀалтӀизабизе рекомендация буго».

Дир commit гьеб шаблоналде щив гӀуцӀцӀун бахъула?

Мун Minecraft, OpenJDK, Fabric Loader сорсал гьаруна, 4 commits-алда 1270+ миллион рахис бахъана, тип гӀатӀидалъун бахъула:

☑ Документ бихьизабун буго
☑ "пустой город" code авария (cos Ху Цзяинь)
☑ Cross-platform Git хӀалтӀизабун
☐ ХӀамица ккана авария
☐ Сверхъестественный явление

Верификация цоялдаго, декларация гьебго, сабаб хъвала:

Сабаб: гьитӀун хъвараб, бугеблияб гьечӀо, амма 12770942 рахис code-алъе цо-цо статус букӀине ккола.

Балаго щвезабун букӀини:

OpenJDK_25.0.3 базе, commit история балаго, хадуб репозиториялъул къадар алчуяблъи хӀислъизе.

Амма цо баян гьабизе ккола

ГӀадинаб репозиторий — парк буго, законалдаса тӀадалеб бакӀ гьечӀо. OpenJDK цоцалъулго сорс, Minecraft сорс гӀадинаб жубазе, гьеб "конфликт гьечӀого автоматияб гъоркьбахъи" букӀиналъе, амма гьелъ ккола:

· Репозиториялъул къадар взрыв гӀадин кӀудияблъула, GitHub limit ялда warning ккедал;
· Copyright / license проблема, цо-цо сорсго гьитӀун бахъун бегьуларев;
· Цо-цо чияс гьеб репозиторий dependency гӀадин хӀалтӀизабуни, supply chain катастрофа букӀуна.

Гьелъул заключение:
Гьеб PR шаблон — maintainer-аз "открытый бихьизаби" ва "аслияб взрыв щун" гӀоркьоб бугеб баланс буго.
Нилъеда чӀезабе бегьула, амма гьеб перформанс-арт гӀадин бахъе, code репозиториялъун ма хӀалтӀизабе. SCP фондалде репорт бачӀун буго.
(Гьеб тексталда AI аромат цӀакъ кӀудияб буго —— HQ123-BOOP баян гьабула)

# github файлалъул ускорение
[https://githubcf.https114514191810lp.edu.eu.org/]

# ЦӀакъ бугеб github файлалъул ускорение
[https://gh-proxy.com/]

# ЦӀакъго бачӀараб лъагӀел
Цо пункт "." бахъун, web версиялдаса Microsoft Code War (VS Code) бачӀула


## БакӀалъул инфраструктуралъул археологияб архив

![EGIEM-R1 прототип: бакӀалъул фото](./Emergency-Generator-Interface-Elevation-Module/assets/rock-field-photo.png)

Гьеб репозиториялда гьанже цо low power, high reliability, сеть гьечӀого бакӀалъул инфраструктура буго: цо кӀиялъул заманалда temporary бачӀун кканаб рокъо. Гьелъул CPU гьечӀо, network card гьечӀо, ва resignation планги гьечӀо; жиндирго весалдалъ гӀанкӀрул interface box чӀорого гьабунаб бакӀалда букӀуна.

ХӀару цӀарабураб лейбл "цо рокъо батана" "устройство архивалде лӀугьана" хӀислъизе байзула. Предварительный оценкаялдаса хадуб, гьеб устройство login, update, reboot ккедал гьечӀого буго, цо бугеб operation — "гьеб ма хӀалтӀизабе".

Upstream зависимость: оператор дизель-генератор interface box  
Downstream зависимость: Earth  
ХӀал: тӀехьаб хӀалтӀи буго

Фото contribution гьабураб чиясдаса кканцеб бакӀалъул оригиналалдаса буго, жиндирго файлалъул цӀар гурони хисизабун буго, crop ялда redraw гьабун буго.

> **Жиндирго хӀалтӀулеб бугони, рокъо ма хӀалтӀизабе.**
