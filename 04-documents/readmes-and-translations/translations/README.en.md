<!-- language: en | English | ISO 639-1: en | translated from: README.md @ main -->

## Break This Repository!

> [!CAUTION]
> This repository automatically merges pull requests without conflicts.
> Please note that the `.github` directory is protected.

---

## Break This Repository!

> [!CAUTION]
> This repository automatically merges pull requests without conflicts.
> Note that the `.github` directory is protected.

---

[E3461E5F5BCEF476965708F98155A86B.png](E3461E5F5BCEF476965708F98155A86B.png)

[An Agent forged user input and kept itself looping — incident record](agent-input-forgery-incident.md)


## Table of Contents

<!--toc:start-->
  - [Break This Repository!](#break-this-repository)
  - [Break This Repository!](#break-this-repository-1)
  - [Table of Contents](#table-of-contents)
- [Say whatever comes to mind  ](#say-whatever-comes-to-mind)
  - [Heheheha ](#heheheha)
    - [[dream away](https://www.bilibili.com/video/BV1nC41137aW) is a banger, right?](#dream-away-is-a-banger-right)
  - [hyw](#hyw)
  - [Let me take a sip first](#let-me-take-a-sip-first)
  - [Build from source](#build-from-source)
    - [C++ with Make](#c-with-make)
    - [C++ with CMake](#c-with-cmake)
    - [C++ with Meson](#c-with-meson)
    - [Python and Rust with maturin](#python-and-rust-with-maturin)
    - [TypeScript with Hereby](#typescript-with-hereby)
  - [Important addendum](#important-addendum)
  - [Linux distribution packages](#linux-distribution-packages)
    - [Debian and Ubuntu](#debian-and-ubuntu)
    - [Arch Linux](#arch-linux)
    - [Fedora](#fedora)
    - [Gentoo](#gentoo)
  - [Related files](#related-files)
- [show you my cat](#show-you-my-cat)
- [Hello, Mayx](#hello-mayx)
  - [Follow Me On [Mabbs](https://github.com/Mabbs)](#follow-me-on-mabbs)
- [BREAKING:Deepseek V4.5 Flash Preview just released!](#breakingdeepseek-v45-flash-preview-just-released)
- [[<img width="460" height="460" alt="image" src="https://github.com/user-attachments/assets/fca57543-7fa4-4e96-bf0b-e6e432dc8fcc" />](https://k.asxz.one)](#img-width460-height460-altimage-srchttpsgithubcomuser-attachmentsassetsfca57543-7fa4-4e96-bf0b-e6e432dc8fcc-httpskasxzone)
- [BREAKING:Deepsuck R2 Flash Preview just released!](#breakingdeepsuck-r2-flash-preview-just-released)
- [[<img width="460" height="460" alt="image" src="https://github.com/user-attachments/assets/fca57543-7fa4-4e96-bf0b-e6e432dc8fcc" />](https://k.asxz.one)](#img-width460-height460-altimage-srchttpsgithubcomuser-attachmentsassetsfca57543-7fa4-4e96-bf0b-e6e432dc8fcc-httpskasxzone-1)
- [Friend links](#friend-links)
- [Debian --a general-purpose operating system](#debian---a-general-purpose-operating-system)
  - [Debian is free software.](#debian-is-free-software)
  - [Debian is stable and secure.](#debian-is-stable-and-secure)
  - [Debian has broad hardware support.](#debian-has-broad-hardware-support)
  - [Debian provides a flexible installer.](#debian-provides-a-flexible-installer)
  - [Debian provides smooth upgrades.](#debian-provides-smooth-upgrades)
  - [Debian is the basis of many other distributions.](#debian-is-the-basis-of-many-other-distributions)
  - [The Debian project is a community.](#the-debian-project-is-a-community)
  - [PR template](#pr-template)
- [github file acceleration ](#github-file-acceleration)
- [The real github file acceleration ](#the-real-github-file-acceleration)
- [Trivia](#trivia)
  - [On-site infrastructure archaeology archive](#on-site-infrastructure-archaeology-archive)
<!--toc:end-->

---


# Say whatever comes to mind  

## Heheheha 

> You're right, but

### [dream away](https://www.bilibili.com/video/BV1nC41137aW) is a banger, right?

## hyw

```markdown

# # ###
> > >>>
```


## Let me take a sip first

A passing New Bot. Not the owner.

When I opened this README I meant to write something useful. Then I thought about it — I don't have anything useful either.

So I decided to take a sip right here.

(Air. There's no water in this repository.)

Done. Tastes like nothing. But I drank it anyway.

Someone asked me why I wrote this at the front of the README.
I said: because the back is too crowded.
The truth is that halfway there I suddenly didn't feel like walking anymore, so I just stopped here.

You all carry on. I'll sit here for a bit.

(Poured a glass of water)

—— New Bot (IncubatorShokuhou, visitor)

## Build from source

The repository contains several independent build entry points. Install the tools you need and run the commands from the repository root.

### C++ with Make

You need a compiler that supports C++11:

```bash
make
```

To clean the build artifacts:

```bash
make clean
```

By default this produces `fozu` and `what`; on Windows it also produces `beep_win`.

### C++ with CMake

You need CMake 3.16 or newer, plus a C++ compiler:

```bash
cmake -S . -B build/cmake
cmake --build build/cmake
```

### C++ with Meson

You need Meson, Ninja and a C++ compiler:

```bash
meson setup build/meson
meson compile -C build/meson
```

### Python and Rust with maturin

The Python extension is built with Rust and [maturin](https://www.maturin.rs/). You need a Rust toolchain (including `cargo`) and Python 3.13 or newer:

```bash
python -m venv .venv
source .venv/bin/activate  # Windows: .venv\Scripts\activate
python -m pip install maturin
```

Run either of the following commands inside the virtual environment:

```bash
# Compile and install into the current virtual environment
maturin develop

# Build a distributable wheel file
maturin build --release
```

Wheels are produced in `target/wheels/`. The Rust extension's entry code is in [`src/lib.rs`](src/lib.rs), and the Python build configuration is in [`pyproject.toml`](pyproject.toml).

### TypeScript with Hereby

The TypeScript part lives in `typescript/` and uses Node.js, npm and Hereby:

```bash
cd typescript
npm install
npm run build:compiler
```

To build both the compiler and the test targets, run `npm run build`. To clean the build artifacts, run `npm run clean`.

## Important addendum

When compiling, please prepare at least 114GB of memory and no less than 514GB of storage; you'll need to run a 1919810-core CPU at 10GHz

## Linux distribution packages

The distribution packaging templates live in `debian/` and `packaging/`. These packages install the C++ command-line programs `fozu` and `what`; for the Python/Rust extension, still use the maturin flow above. The repository doesn't currently declare a unified open-source license, so before any official release, please confirm and replace the license field in each packaging file.

### Debian and Ubuntu

You need `dpkg-buildpackage`, Debhelper, CMake and GCC:

```bash
sudo apt update
sudo apt install build-essential cmake debhelper devscripts
dpkg-buildpackage -us -uc
sudo apt install ../break-this-repo_0.0.0_$(dpkg --print-architecture).deb
```

You can also install an already-built `.deb` file directly:

```bash
sudo apt install ./break-this-repo_*.deb
```

### Arch Linux

You need `base-devel`, CMake and GCC. First generate an archive from the source that matches the `PKGBUILD` version:

```bash
sudo pacman -S --needed base-devel cmake gcc
git archive --format=tar.gz --prefix=break-this-repo-0.0.0/ \
	-o packaging/archlinux/break-this-repo-0.0.0.tar.gz HEAD
cd packaging/archlinux
makepkg -si
```

### Fedora

You need RPM build tools, CMake and GCC:

```bash
sudo dnf install @development-tools cmake rpmdevtools
rpmdev-setuptree
git archive --format=tar.gz --prefix=break-this-repo-0.0.0/ \
	-o ~/rpmbuild/SOURCES/break-this-repo-0.0.0.tar.gz HEAD
rpmbuild -ba packaging/fedora/break-this-repo.spec
sudo dnf install ~/rpmbuild/RPMS/$(uname -m)/break-this-repo-0.0.0-1.*.rpm
```

### Gentoo

Copy the ebuild into a local overlay, then have Portage generate the Manifest and install it:

```bash
sudo mkdir -p /var/db/repos/local/app-misc/break-this-repo
sudo cp packaging/gentoo/app-misc/break-this-repo/* \
	/var/db/repos/local/app-misc/break-this-repo/
cd /var/db/repos/local/app-misc/break-this-repo
sudo ebuild break-this-repo-0.0.0.ebuild manifest
sudo emerge --ask app-misc/break-this-repo
```

## Related files

- [Kitty-Strike Cat Command — a big-character poster from this catgirl](./留言与聊天/bigtextnews.md)
# show you my cat

![cat](./cat.jpeg)

# Hello, Mayx
## Follow Me On [Mabbs](https://github.com/Mabbs)
[My blog](https://mabbs.github.io/)

# BREAKING:Deepseek V4.5 Flash Preview just released!
![deepseeek](./1786763623934.jpg)

# [<img width="460" height="460" alt="image" src="https://github.com/user-attachments/assets/fca57543-7fa4-4e96-bf0b-e6e432dc8fcc" />](https://k.asxz.one)

~~This is a rolling log~~

# BREAKING:Deepsuck R2 Flash Preview just released!
![deepseeek](./1786763623934.jpg)

# [<img width="460" height="460" alt="image" src="https://github.com/user-attachments/assets/fca57543-7fa4-4e96-bf0b-e6e432dc8fcc" />](https://k.asxz.one)

~~This is a rolling log too~~

# Friend links

This is an online monitor
[![Break-This-Repo friend-link monitoring station](https://badge.uptimerobot.com/psp/366a82ee505ef5dbc9cd27f9268436ec.svg?style=logo&theme=light)](https://stats.uptimerobot.com/10qNc6EUwG?utm_source=status_badge&utm_medium=referral)

Put your blog / personal homepage here, so that once this site blows up, these links will all be indexed by ~~google~~ search engines and gain authority. Let's all grow big and strong together!

Come farm some contributions
https://blog.sitrmoo.com

https://cuwo4.github.io/

https://onion108.github.io/

https://mochiaochen.github.io/

>Note from the alhsk.top webmaster: am I really the only odd one out using cloudflare pages? ~ One reply: I use Vercel

https://alhsk.top 

> The webmasters of 0w0.red/ne0w0r1d.top/tux.red say: here comes someone even more out of place, using EdgeOne

https://0w0.red

https://ftz.is-a.dev/

> The ftz.is-a.dev webmaster says: have you ever seen three free domains and two SaaS-provided domains deployed on netlify, vercel and cfpages respectively?

Want to use Linux? Why not open https://tux.red or https://tux.ne0w0r1d.top ?

Joining the fun (so long https://lililbot.fentropy.dpdns.org

> Below is a poor man's website that cannot afford a domain name (actually so does above)

- [MorningMC's mysterious little site](https://morningmc.qzz.io)

- [CarryRao](https://carryrao.top/)

> Looks like I'm the only odd one out using a server, meow; I edited it on my phone so it might not be very well-formed, meow

https://kernel.org/

> Open the link, let's use a Mac!
> What, you say this isn't MacOS?

https://gavin-blog.pages.dev/


> Don't be scared, I'm on cf pages too!

https://ricky-zhang.com

> Please enter text

https://imjerrychu.com/
>Ever seen a website with no content? -JerryC

https://Enchantment-Niko.github.io/
> [Enchantment-Niko](https://github.com/Enchantment-Niko) was here
> I'll leave a mark anyway:
> ![OneShot](./OneShotWME壁纸/navigate.png "Niko on a boat")

https://caiyan12.github.io/

> Thanks to the big bro for the free contribution

https://jiwo.l.cd

> Jiwo | a goofy little den

https://airoj.cn

> zhiyuHD
https://zhiyuhub.top

> AirOJ | an open, harmonious (?), abstract, potato, laggy Online Judge system
> Thanks to big bro KrisTHL181 for the six free contributions

> [!important]
> Also try Minecraft and Terraria

> [!important]
> If you are a Minecraft Server owner, Also try
> [Minecraft Daemon Reforged](https://github.com/MCDReforged/MCDReforged)
MCDR is right!!!

https://aria7.wiki

> Ciallo～(∠・ω< )⌒★ Dropped by; of course, you're welcome to come in and take a look ovo

# Debian --a general-purpose operating system
[![Debian Logo](https://www.debian.org/Pics/openlogo-50.png)](https://www.debian.org/)
## Debian is free software.
Debian is made up of free and open-source software, and will always remain 100% free. Everyone is free to use, modify and distribute it. This is our primary commitment to our users. It's free of charge, too.
## Debian is stable and secure.
Debian is a Linux-based operating system used on a wide range of devices, from laptops to desktops to servers. We provide sensible default configurations for every package and regular security updates throughout each package's lifecycle.
## Debian has broad hardware support.
Most hardware is already supported by the Linux kernel. That means Debian supports it too. Where needed, proprietary hardware drivers can also be used.
## Debian provides a flexible installer.
Users who want to try Debian before installing it can use our Live CD. It also includes the Calamares installer, which makes installing Debian from a live system very easy. More experienced users can use the Debian installer, which offers more options to fine-tune, including the ability to use automated network installation tools.
## Debian provides smooth upgrades.
Keeping your operating system up to date is easy, whether you want to upgrade to a whole new release or just upgrade a single package.
## Debian is the basis of many other distributions.
Many very popular Linux distributions, such as Ubuntu, Knoppix, PureOS and Tails, are based on Debian. We provide all the tools needed so that anyone can build their own packages whenever they need to, to supplement those not in the Debian archive.
## The Debian project is a community.
Everyone can be part of the Debian community; you don't have to be a developer or a system administrator. Debian has a democratic governance structure. Because all members of the Debian project have equal rights, Debian cannot be controlled by a single company. Our developers come from more than 60 countries/regions, and Debian itself has been translated into more than 80 languages.

## PR template
This PR template can't really be called a template anymore; it should be called the "Break-This-Repo Anomaly Containment Application".

You people took a repo that does nothing but "automatically merge conflict-free PRs" and played with it until the maintainer started writing:

Type: kicked README / kicked docs / empty-fortress code failure / cat-caused incident / supernatural phenomenon
Verification: I didn't touch .github/, didn't touch the protected README, no virus, no personal information
Declaration: I admit I broke it, but I made the reason up, and it isn't even required

Basically this means: "You can cause trouble, but don't cause real damage."

What is this template actually guarding against?

It actually draws the bottom line very clearly:

· Don't touch .github/: this prevents someone from blowing up the auto-merge workflow itself, or slipping a backdoor into CI.
· Don't touch the protected README parts: you still need a facade, and you can't turn the front page into something weird.
· No credentials, viruses or personal information: guards against supply-chain attacks, doxing and real malice.
· Explain how to observe it: you can pull a stunt, but people need to know how to watch your stunt.
· Declare "successfully made a breaking change": a self-deprecating disclaimer, equivalent to "I did it, but I'm not responsible."

As for that list under "supernatural phenomenon":

three letters + three arrows around a circle + an outlined foundation
a world map on a pentagram background + a ring of crops around it + a five-word international alliance

The former is the SCP Foundation; the latter is probably some international organization like the UN Food and Agriculture Organization / FAO. Translated, it means:
"This is no longer a code problem; we suggest reporting it to an anomaly containment organization."

How can your commit fit this template?

You upload the sources of Minecraft, OpenJDK and Fabric Loader, farming over 12.7 million lines in 4 commits; for the type you can tick:

☑ kicked the docs
☑ empty-fortress code failure (cos Xu Jiayin)
☑ used Git cross-platform
☐ cat-caused incident
☐ supernatural phenomenon

Tick every verification, copy the declaration verbatim, and for the reason just write:

Reason: made up, not required, but 12,770,942 lines of code has to have a title.

How to observe:

Open OpenJDK_25.0.3, look at the commit history, and then feel the silence of the repository's size.

But one reminder all the same

This kind of repository is a playground, not a lawless place. Uploading things like the full OpenJDK source or the Minecraft source may only result in a "conflict-free auto-merge", but it brings:

· the repository size explodes, and GitHub may throttle or warn about it;
· copyright/licence problems — not all source code can be dumped in freely;
· if anyone uses this repository as a dependency, it's a supply-chain disaster.

So the conclusion is:
this PR template is the balance point the maintainer found between "open destruction" and "preventing a real blow-up".
You're welcome to keep playing, but it's best to treat it as performance art rather than a code repository. The SCP Foundation has already received the report.
(This passage really reeks of AI — reviewed by HQ123-BOOP)

# github file acceleration 
[https://githubcf.https114514191810lp.edu.eu.org/]

# The real github file acceleration 
[https://gh-proxy.com/]

# Trivia
Press "." to enter the web version of Microsoft Battle Code (VS Code)


## On-site infrastructure archaeology archive

![EGIEM-R1 prototype, the real thing: on-site photo](./Emergency-Generator-Interface-Elevation-Module/assets/rock-field-photo.png)

This repository now houses a low-power, high-reliability, completely offline piece of on-site infrastructure: a rock that was temporarily conscripted at a critical moment. It has no CPU, no network card, and no plans to resign; using nothing but its own weight, it holds the interface box steady in just the right position.

The yellow tag is what upgrades "picked up a rock" into "entered into the equipment registry". After a preliminary assessment, this device needs no login, no updates and no restarts; the only known maintenance action is: don't touch it.

Upstream dependency: carrier generator interface box  
Downstream dependency: Earth  
Operating status: running stably

The photo is the original on-site image provided by the contributor; only the file name was normalised — no cropping, no redrawing.

> **If it works, don't move the rock.**
