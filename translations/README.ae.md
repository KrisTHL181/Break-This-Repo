<!-- language: ae | avesta | ISO 639-1: ae | translated from: README.md @ main -->

## vi-družaya imam repository!

> [!CAUTION]
> ima repository automat-ā merge-ayeiti pull requests a-čašma-.
> fra-yazəδa tū, ima `.github` directory-š fra-yaštō.

---

## ham-vi-drušaya imąm repository!

> [!CAUTION]
> imąm repository automat-ā merge-ayeiti a-čašma- pull requests.
> fra-yazəδa, ima `.github` directory-š fra-yaštō.

---

[E3461E5F5BCEF476965708F98155A86B.png](E3461E5F5BCEF476965708F98155A86B.png)

[Agent fra-uxta user-input aθra-šnāθra — incident-record](agent-input-forgery-incident.md)


## fradaxšta

<!--toc:start-->
  - [vi-družaya imam repository!](#vi-družaya-imam-repository)
  - [ham-vi-drušaya imąm repository!](#ham-vi-drušaya-imąm-repository)
  - [fradaxšta](#fradaxšta)
- [mrūδa yaδa-cit manō-š](#mrūδa-yaδa-cit-manō-š)
  - [heheheha](#heheheha)
    - [[dream away](https://www.bilibili.com/video/BV1nC41137aW) husta-š sraošō](#dream-away-husta-š-sraošō)
  - [hyw](#hyw)
  - [azəm fra-piθa āt mrūδa](#azəm-fra-piθa-āt-mrūδa)
  - [Source-š fra-build](#source-š-fra-build)
    - [C++ āt Make](#c-āt-make)
    - [C++ āt CMake](#c-āt-cmake)
    - [C++ āt Meson](#c-āt-meson)
    - [Python āt Rust āt maturin](#python-āt-rust-āt-maturin)
    - [TypeScript āt Hereby](#typescript-āt-hereby)
  - [important-addendum](#important-addendum)
  - [Linux distribution package-š](#linux-distribution-package-š)
    - [Debian āt Ubuntu](#debian-āt-ubuntu)
    - [Arch Linux](#arch-linux)
    - [Fedora](#fedora)
    - [Gentoo](#gentoo)
  - [related-files](#related-files)
- [mā gō reet](#mā-gō-reet)
- [namō, Mayx](#namō-mayx)
  - [mā raac [Mabbs](https://github.com/Mabbs)](#mā-raac-mabbs)
- [BREAKING:Deepseek V4.5 Flash Preview fra-vīda!](#breakingdeepseek-v45-flash-preview-fra-vīda)
- [[<img width="460" height="460" alt="image" src="https://github.com/user-attachments/assets/fca57543-7fa4-4e96-bf0b-e6e432dc8fcc" />](https://k.asxz.one)](#img-width460-height460-altimage-srchttpsgithubcomuser-attachmentsassetsfca57543-7fa4-4e96-bf0b-e6e432dc8fcc-httpskasxzone)
- [BREAKING:Deepsuck R2 Flash Preview fra-vīda!](#breakingdeepsuck-r2-flash-preview-fra-vīda)
- [[<img width="460" height="460" alt="image" src="https://github.com/user-attachments/assets/fca57543-7fa4-4e96-bf0b-e6e432dc8fcc" />](https://k.asxz.one)](#img-width460-height460-altimage-srchttpsgithubcomuser-attachmentsassetsfca57543-7fa4-4e96-bf0b-e6e432dc8fcc-httpskasxzone-1)
- [friend-links](#friend-links)
- [Debian --vispa-kāra operating system](#debian---vispa-kāra-operating-system)
  - [Debian asti azāta-software.](#debian-asti-azāta-software)
  - [Debian stāra āt aṣ̌a-raθβa.](#debian-stāra-āt-aṣa-raθβa)
  - [Debian asti broad hardware-support.](#debian-asti-broad-hardware-support)
  - [Debian asti flexible installer dāštayaēti.](#debian-asti-flexible-installer-dāštayaēti)
  - [Debian asti smooth upgrade dāštayaēti.](#debian-asti-smooth-upgrade-dāštayaēti)
  - [Debian asti many other distributions-š bunya-.](#debian-asti-many-other-distributions-š-bunya-)
  - [Debian project asti a community.](#debian-project-asti-a-community)
  - [PR template](#pr-template)
- [github file-accelerate](#github-file-accelerate)
- [real github file-accelerate](#real-github-file-accelerate)
- [cold-knowledge](#cold-knowledge)
  - [field-infrastructure-archaeology-archive](#field-infrastructure-archaeology-archive)
<!--toc:end-->

---


# mrūδa yaδa-cit manō-š

## heheheha

> tū rāstō, at

### [dream away](https://www.bilibili.com/video/BV1nC41137aW) husta-š sraošō

## hyw

```markdown

# # ###
> > >>>
```


## azəm fra-piθa āt mrūδa

New Bot visitor. nōit xvaēta.

azəm ima README frā-vərəzayaθa yaδa-cit upayō-š mrūδayaē-ča. pasća azəm aš-taša: upayō-š azəm-ča nōit asti.

təm-pāδī azəm ima-sthāne fra-piθa.

(āyō. repository-š ap-š nōit asti.)

fra-piθa. tauma-š nōit. at azəm fra-piθa.

kaš-cit mąm parsaθa yaδa ča ima README pərəsa-š mrūδayaēmi.
azəm mrūδayaēmi: pascā-tara ati-gaθa.
haθa-vaēša, azəm madhya-š ā-gaθa āt nōit kāma ā-gaθaya, ima-sthāne stop-aya.

yūšmākəm aθravaēnaθa. azəm kaś-cit ni-šadaya.

(ap-š fra-piθa)

—— New Bot (IncubatorShokuhou, visitor)

## Source-š fra-build

ima repository vasati many independent build-entry. yūš fra-yazəδa ima upayō anušaya āt ima repository root-š command-š frā-yāršta.

### C++ āt Make

C++11 support-š compiler-š kāma:

```bash
make
```

clean build artifact:

```bash
make clean
```

default-š fra-kaṇta `fozu` āt `what`; Windows-š api fra-kaṇta `beep_win`.

### C++ āt CMake

CMake 3.16 or up-š version-š āt C++ compiler-š kāma:

```bash
cmake -S . -B build/cmake
cmake --build build/cmake
```

### C++ āt Meson

Meson, Ninja āt C++ compiler-š kāma:

```bash
meson setup build/meson
meson compile -C build/meson
```

### Python āt Rust āt maturin

Python extension-š Rust āt [maturin](https://www.maturin.rs/) fra-kaṇta. Rust toolchain (`cargo` āt) āt Python 3.13 or up-š kāma:

```bash
python -m venv .venv
source .venv/bin/activate  # Windows: .venv\Scripts\activate
python -m pip install maturin
```

virtual environment-š asti command-š frā-yāršta:

```bash
# fra-kaṇta āt install-aya imąm virtual environment-š
maturin develop

# fra-kaṇta distributable wheel file-š
maturin build --release
```

wheel build artifact-š `target/wheels/`-š asti. Rust extension-š entry code-š [`src/lib.rs`](src/lib.rs), Python build config-š [`pyproject.toml`](pyproject.toml).

### TypeScript āt Hereby

TypeScript bhāga-š `typescript/`-š asti, Node.js, npm āt Hereby fra-yāršta:

```bash
cd typescript
npm install
npm run build:compiler
```

compiler āt test target saθa build-š kāma, `npm run build` frā-yāršta. clean build artifact-š `npm run clean` frā-yāršta.

## important-addendum

compile-time-š kamu at least 114GB memory āt 514GB up-š storage, 1919810-core CPU 10GHz-š frā-yāršta

## Linux distribution package-š

distribution packaging template-š `debian/` āt `packaging/`-š asti. ima package-š install-ayeiti C++ command-line program `fozu` āt `what`; Python/Rust extension-š upari maturin flow fra-yāršta. repository-š aθravaēnaθa unified open-source license declare-š nōit asti, release pərəsa ima packaging file-š license field confirm āt replace fra-yazəδa.

### Debian āt Ubuntu

`dpkg-buildpackage`, Debhelper, CMake āt GCC kāma:

```bash
sudo apt update
sudo apt install build-essential cmake debhelper devscripts
dpkg-buildpackage -us -uc
sudo apt install ../break-this-repo_0.0.0_$(dpkg --print-architecture).deb
```

api directly install build-š `.deb` file-š:

```bash
sudo apt install ./break-this-repo_*.deb
```

### Arch Linux

`base-devel`, CMake āt GCC kāma. prāma source-š `PKGBUILD` version-š match-š archive file fra-kaṇta:

```bash
sudo pacman -S --needed base-devel cmake gcc
git archive --format=tar.gz --prefix=break-this-repo-0.0.0/ \
	-o packaging/archlinux/break-this-repo-0.0.0.tar.gz HEAD
cd packaging/archlinux
makepkg -si
```

### Fedora

RPM build tool, CMake āt GCC kāma:

```bash
sudo dnf install @development-tools cmake rpmdevtools
rpmdev-setuptree
git archive --format=tar.gz --prefix=break-this-repo-0.0.0/ \
	-o ~/rpmbuild/SOURCES/break-this-repo-0.0.0.tar.gz HEAD
rpmbuild -ba packaging/fedora/break-this-repo.spec
sudo dnf install ~/rpmbuild/RPMS/$(uname -m)/break-this-repo-0.0.0-1.*.rpm
```

### Gentoo

ebuild-š local overlay-š copy āt Portage-š Manifest fra-kaṇta āt install:

```bash
sudo mkdir -p /var/db/repos/local/app-misc/break-this-repo
sudo cp packaging/gentoo/app-misc/break-this-repo/* \
	/var/db/repos/local/app-misc/break-this-repo/
cd /var/db/repos/local/app-misc/break-this-repo
sudo ebuild break-this-repo-0.0.0.ebuild manifest
sudo emerge --ask app-misc/break-this-repo
```

## related-files

- [Cat-punch HQ — ima cat-girl-š bṛhat-banner](./留言与聊天/bigtextnews.md)
# mā gō reet

![būša](./cat.jpeg)

# namō, Mayx
## mā raac [Mabbs](https://github.com/Mabbs)
[mā blog](https://mabbs.github.io/)

# BREAKING:Deepseek V4.5 Flash Preview fra-vīda!
![deepseeek](./1786763623934.jpg)

# [<img width="460" height="460" alt="image" src="https://github.com/user-attachments/assets/fca57543-7fa4-4e96-bf0b-e6e432dc8fcc" />](https://k.asxz.one)

~~ima asti rolling-log~~

# BREAKING:Deepsuck R2 Flash Preview fra-vīda!
![deepseeek](./1786763623934.jpg)

# [<img width="460" height="460" alt="image" src="https://github.com/user-attachments/assets/fca57543-7fa4-4e96-bf0b-e6e432dc8fcc" />](https://k.asxz.one)

~~ima api rolling-log~~

# friend-links

ima asti online monitor
[![Break-This-Repo-š friend-link monitor-station](https://badge.uptimerobot.com/psp/366a82ee505ef5dbc9cd27f9268436ec.svg?style=logo&theme=light)](https://stats.uptimerobot.com/10qNc6EUwG?utm_source=status_badge&utm_medium=referral)

yūšmākəm blog/personal-homepage ima-sthāne ni-dhā, təm-pāδī ima site ātaṣ-š, ima link-š ~~google~~ search-engine index-š bavainti, weight-š vardhaya. vispa saθa vṛddha āt śakta bhavantu!

contribution-š ā-ganaya
https://blog.sitrmoo.com

https://cuwo4.github.io/

https://onion108.github.io/

https://mochiaochen.github.io/

> alhsk.top site-owner note: nōit azəm-ča eka-š garala cloudflare pages fra-yāršta ~eka reply: azəm Vercel fra-yāršta

https://alhsk.top

> 0w0.red/ne0w0r1d.top/tux.red owner mrūδayaēti: api garala EdgeOne fra-yāršta ā-gata

https://0w0.red

https://ftz.is-a.dev/

> ftz.is-a.dev owner mrūδayaēti: tū drṣṭa three free-domain two SaaS own-domain respectively deploy-š netlify vercel cfpages-š

Linux fra-yāršta kāma? yaδa nōit open kṛṇuṣva https://tux.red or https://tux.ne0w0r1d.top?

juṇa-š ā-ganaya (ati-dīrgha https://lililbot.fentropy.dpdns.org

> adho asti poor man-š website yat domain-price nōit śaknuṣti (haθa-vaēša upari api tathā)

- [MorningMC-š mysterious small-site](https://morningmc.qzz.io)

- [CarryRao](https://carryrao.top/)

> iva azəm eka-š garala server-cat fra-yāršta, phone-š modify-š maybe nōit standard cat

https://kernel.org/

> link fra-vāršta, vaēm Mac fra-yāršta!
> ka, tū mrūδayaēti ima nōit MacOS?

https://gavin-blog.pages.dev/


> mā bīša, azəm api cf pages!

https://ricky-zhang.com

> input text prayuja

https://imjerrychu.com/
> drṣṭa content-nāśa site? -JerryC

https://Enchantment-Niko.github.io/
> [Enchantment-Niko](https://github.com/Enchantment-Niko) ima-sthāne ā-gata
> azəm api mark ni-dhā:
> ![OneShot](./OneShotWME壁纸/navigate.png "Niko nāva-š sāda")

https://caiyan12.github.io/

> thanks big-brother-š free contribution eka

https://jiwo.l.cd

> jiwo | eka comical small-nest

https://airoj.cn

> zhiyuHD
https://zhiyuhub.top

> AirOJ | open, harmonious (?), abstract, potato, lag-š Online Judge system
> thanks KrisTHL181 big-brother-š free contribution 6

> [!important]
> api try Minecraft āt Terraria

> [!important]
> Minecraft Server owner yadi tū, api try
> [Minecraft Daemon Reforged](https://github.com/MCDReforged/MCDReforged)
MCDR asti rāstō!!!

https://aria7.wiki

> Ciallo～(∠・ω< )⌒★ ima-sthāne ā-gata, of course, tū andar vaēdaya ovo

# Debian --vispa-kāra operating system
[![Debian Logo](https://www.debian.org/Pics/openlogo-50.png)](https://www.debian.org/)
## Debian asti azāta-software.
Debian asti azāta āt open-source software-š saṃnāta, āt 100% azāta remain-ayeiti. vispa jan-š azāta use, modify, āt distribute-š śaknuṣti. ima asti vaēm-š user-š pərəsa main promise. ima api free asti.
## Debian stāra āt aṣ̌a-raθβa.
Debian asti vāsati many device-š Linux-based operating system, yat range-š laptop, desktop āt server. vaēm each package-š reasonable default-config dāštayaēma, āt package lifecycle-š regular security-update dāštayaēma.
## Debian asti broad hardware-support.
most hardware-š Linux kernel-š support-š bavainti. ima asti Debian api tān support-ayeiti. yadi kāma, api proprietary hardware-driver fra-yāršta.
## Debian asti flexible installer dāštayaēti.
install pərəsa Debian try-š kāma user-š vaēm-š Live CD fra-yāršta. ima api Calamares installer include-ayeiti, yat Live system-š Debian install-š ati āsya bavainti. experienced user-š Debian installer fra-yāršta śaknuṣti, yat more fine-tune option-š dāštayaēti, including automated network-install tool-š function.
## Debian asti smooth upgrade dāštayaēti.
operating system-š latest remain-ayeiti ati āsya. yadi tū brand-new release-š upgrade kāma, yadā eka single package-š upgrade kāma.
## Debian asti many other distributions-š bunya-.
many ati-popular Linux distribution-š, example Ubuntu, Knoppix, PureOS, Tails, Debian base-š asti. vaēm dāštayaēma required all tool, yat each jan-š kāma-sthāne own package fra-kaṇta, Debian archive-š nōit-asti package supplement.
## Debian project asti a community.
vispa jan Debian community-š member bavainti; tū nōit developer eka system-administrator bavainti. Debian-š asti democratic governance-structure. yat Debian project-š member vispa equal right-š, təm-pāδī Debian single company-š control nōit śaknuṣti. vaēm-š developer 60 up country-š ā-ganainti, āt Debian-š api 80 up language-š translate-š bavainti.

## PR template
ima PR template nōit template nāma-š arhayaēti, ima `Break-This-Repo Anomaly-Containment-Application` nāma arhayaēti.

yūš-š ima repo, eka "conflict-free PR auto-merge" repo, maintainer-š mrūδayaēti:

type: kick README / kick doc / empty-city-strategy code-fault / cat-caused accident / supernatural-phenomenon
verify: azəm nōit modify .github/, nōit modify protected README, nōit virus, nōit personal-info
declare: azəm acknowledge azəm broke, at reason-š azəm random-write, āt nōit mandatory

ima asti base: "tū śaknuṣti break, at mā real-break."

ima template-š protect-š ka?

ima actually bottom-line-š ati clear cut-ayeiti:

· nōit modify .github/: prevent kaš-ci auto-merge workflow-š self blow-up, eka CI-š backdoor insert.
· nōit modify protected README part: facade-š required, nōit home-page-š weird-š bavaya.
· nōit credential, virus, personal-info: prevent supply-chain attack, prevent dox, prevent real-malice.
· explain ka observe: tū śaknuṣti prank, at kaś-ci know ka observe tū-š prank.
· declare "successfully breaking-change": self-mock disclaimer, equivalent "azəm did, at azəm nōit responsible."

təm-pāδī "supernatural-phenomenon" series:

three-letter + circle-center three-arrow + outlined foundation
pentagram-background world-map + around crops + five-word international-alliance

former asti SCP Foundation, latter maybe FAO-š international-organization. translate-š:
"ima nōit code problem, suggest report anomaly-containment-org."

tū-š commit ka ima template apply?

tū upload Minecraft, OpenJDK, Fabric Loader source, 4 commits-š 12.7M+ line-š, type check-š:

☑ kick doc
☑ empty-city-strategy code-fault (cos Xu-Jiayin)
☑ cross-platform Git fra-yāršta
☐ cat-caused accident
☐ supernatural-phenomenon

verify all-check, declare copy, reason-š mrūδayaēti:

reason: random-write, nōit mandatory, at 12770942 line code-š must asti a name.

observe-method:

open OpenJDK_25.0.3, see commit history, āt feel repo-size-š silence.

at azəm api remind eka:

ima repo asti playground, nōit law-outside-land. upload OpenJDK full-source, Minecraft source ima-š, although maybe "conflict-free auto-merge", at bring-š:

· repo-size explosion, GitHub maybe limit eka warn;
· copyright/license problem, nōit all source arbitrary insert;
· yadi kaś-ci ima repo-š dependency fra-yāršta, asti supply-chain disaster.

təm-pāδī conclusion:
ima PR template asti maintainer-š "open-break" āt "prevent-real-explosion" madhya find-š balance-point.
yūš continue-play OK, at better treat-š performance-art, mā code-repo fra-yāršta. SCP Foundation-š already report receive.
(ima text AI-taste ati-strong —HQ123-BOOP comment)

# github file-accelerate
[https://githubcf.https114514191810lp.edu.eu.org/]

# real github file-accelerate
[https://gh-proxy.com/]

# cold-knowledge
press eka "."-š enter web-version Microsoft big-war-code (VS Code)


## field-infrastructure-archaeology-archive

![EGIEM-R1 prototype real-object: field-photo](./Emergency-Generator-Interface-Elevation-Module/assets/rock-field-photo.png)

ima repository-š now include eka low-power, high-reliability, completely offline field-infrastructure: eka key-moment-š temporary-conscription-š stone. ima nōit CPU, nōit network-card, āt nōit resign-intent; only self-weight-š, interface-box-š stable-š support-š proper-position-š.

yellow-label-š "pick-up eka stone" upgrade-š "enter device-archive". preliminary-eval-š, ima device-š nōit login, nōit update, nōit reboot required, only known ops-action asti: mā move-š.

upstream-dependency: carrier oil-machine interface-box
downstream-dependency: earth
running-status: stable-running

photo-š contributor-š provide-š field-original-image, only filename standardize, nōit crop, nōit redraw.

> **yaδa-š work-ayeiti, mā move-š ima rock.**
