<!-- language: gl | Galego | ISO 639-1: gl | translated from: README.md @ main -->

## Rompe este repositorio!

> [!CAUTION]
> Este repositorio fusiona automaticamente as pull requests sen conflitos.
> Ten en conta que o directorio `.github` está protexido.

---

## Destrúe este repositorio!

> [!CAUTION]
> Este repositorio fusiona automaticamente as pull requests sen conflitos.
> Atención: o directorio `.github` está protexido.

---

[E3461E5F5BCEF476965708F98155A86B.png](E3461E5F5BCEF476965708F98155A86B.png)

[Un axente falsificou a entrada do usuario e quedou en bucle por si só — rexistro do incidente](agent-input-forgery-incident.md)


## Índice

<!--toc:start-->
  - [Rompe este repositorio!](#rompe-este-repositorio)
  - [Destrúe este repositorio!](#destrúe-este-repositorio)
  - [Índice](#índice)
- [Di o que che pase pola cabeza  ](#di-o-que-che-pase-pola-cabeza)
  - [Heheheha ](#heheheha)
    - [[dream away](https://www.bilibili.com/video/BV1nC41137aW) que canción tan boa](#dream-away-que-canción-tan-boa)
  - [hyw](#hyw)
  - [Déixame botar un trago primeiro](#déixame-botar-un-trago-primeiro)
  - [Compilar desde o código fonte](#compilar-desde-o-código-fonte)
    - [C++ con Make](#c-con-make)
    - [C++ con CMake](#c-con-cmake)
    - [C++ con Meson](#c-con-meson)
    - [Python e Rust con maturin](#python-e-rust-con-maturin)
    - [TypeScript con Hereby](#typescript-con-hereby)
  - [Engadido importante](#engadido-importante)
  - [Paquetes para distribucións de Linux](#paquetes-para-distribucións-de-linux)
    - [Debian e Ubuntu](#debian-e-ubuntu)
    - [Arch Linux](#arch-linux)
    - [Fedora](#fedora)
    - [Gentoo](#gentoo)
  - [Ficheiros relacionados](#ficheiros-relacionados)
- [Mira o meu gato](#mira-o-meu-gato)
- [Ola, Mayx](#ola-mayx)
  - [Ségueme en [Mabbs](https://github.com/Mabbs)](#ségueme-en-mabbs)
- [ÚLTIMA HORA:Deepseek V4.5 Flash Preview acaba de saír!](#última-horadeepseek-v45-flash-preview-acaba-de-saír)
- [[<img width="460" height="460" alt="image" src="https://github.com/user-attachments/assets/fca57543-7fa4-4e96-bf0b-e6e432dc8fcc" />](https://k.asxz.one)](#img-width460-height460-altimage-srchttpsgithubcomuser-attachmentsassetsfca57543-7fa4-4e96-bf0b-e6e432dc8fcc-httpskasxzone)
- [ÚLTIMA HORA:Deepsuck R2 Flash Preview acaba de saír!](#última-horadeepsuck-r2-flash-preview-acaba-de-saír)
- [[<img width="460" height="460" alt="image" src="https://github.com/user-attachments/assets/fca57543-7fa4-4e96-bf0b-e6e432dc8fcc" />](https://k.asxz.one)](#img-width460-height460-altimage-srchttpsgithubcomuser-attachmentsassetsfca57543-7fa4-4e96-bf0b-e6e432dc8fcc-httpskasxzone-1)
- [Ligazóns de amizade](#ligazóns-de-amizade)
- [Debian --un sistema operativo de propósito xeral](#debian---un-sistema-operativo-de-propósito-xeral)
  - [Debian é software libre.](#debian-é-software-libre)
  - [Debian é estable e seguro.](#debian-é-estable-e-seguro)
  - [Debian ten un amplo soporte de hardware.](#debian-ten-un-amplo-soporte-de-hardware)
  - [Debian ofrece un instalador flexible.](#debian-ofrece-un-instalador-flexible)
  - [Debian ofrece actualizacións sen sobresaltos.](#debian-ofrece-actualizacións-sen-sobresaltos)
  - [Debian é a base de moitas outras distribucións.](#debian-é-a-base-de-moitas-outras-distribucións)
  - [O proxecto Debian é unha comunidade.](#o-proxecto-debian-é-unha-comunidade)
  - [Modelo de PR](#modelo-de-pr)
- [aceleración de ficheiros de github ](#aceleración-de-ficheiros-de-github)
- [A auténtica aceleración de ficheiros de github ](#a-auténtica-aceleración-de-ficheiros-de-github)
- [Curiosidade](#curiosidade)
  - [Arquivo arqueolóxico da infraestrutura in situ](#arquivo-arqueolóxico-da-infraestrutura-in-situ)
<!--toc:end-->

---


# Di o que che pase pola cabeza  

## Heheheha 

> Tes razón, pero

### [dream away](https://www.bilibili.com/video/BV1nC41137aW) que canción tan boa

## hyw

```markdown

# # ###
> > >>>
```


## Déixame botar un trago primeiro

Un New Bot de paso. Non é o dono.

Cando abrín este README quería escribir algo útil. Despois pensei: cousas útiles eu tampouco teño.

Así que decidín botar un trago aquí.

(Aire. No repositorio non hai auga.)

Xa está. Non ten ningún sabor. Pero boteino igual.

Alguén me preguntou por que o escribo diante do README.
Dixen: porque detrás hai demasiada xente.
En realidade é que a medio camiño de súpeto non me apeteceu camiñar máis, así que parei aquí.

Vós continuade. Eu sento un anaco.

(Un vaso de auga botado)

—— New Bot (IncubatorShokuhou, visitante)

## Compilar desde o código fonte

O repositorio contén varias entradas de compilación independentes. Instala as ferramentas que fagan falta e executa os comandos desde a raíz do repositorio.

### C++ con Make

Cómpre un compilador compatible con C++11:

```bash
make
```

Para limpar os artefactos de compilación:

```bash
make clean
```

Por defecto xera `fozu` e `what`; en Windows tamén xera `beep_win`.

### C++ con CMake

Cómpre CMake 3.16 ou superior, e un compilador de C++:

```bash
cmake -S . -B build/cmake
cmake --build build/cmake
```

### C++ con Meson

Cómpre Meson, Ninja e un compilador de C++:

```bash
meson setup build/meson
meson compile -C build/meson
```

### Python e Rust con maturin

A extensión de Python compílase con Rust e [maturin](https://www.maturin.rs/). Cómpre unha toolchain de Rust (con `cargo`) e Python 3.13 ou superior:

```bash
python -m venv .venv
source .venv/bin/activate  # Windows: .venv\Scripts\activate
python -m pip install maturin
```

Dentro do contorno virtual, executa calquera destes comandos:

```bash
# Compila e instala no contorno virtual actual
maturin develop

# Constrúe un ficheiro wheel distribuíble
maturin build --release
```

Os wheels xéranse en `target/wheels/`. O código de entrada da extensión de Rust está en [`src/lib.rs`](src/lib.rs), e a configuración de compilación de Python en [`pyproject.toml`](pyproject.toml).

### TypeScript con Hereby

A parte de TypeScript está en `typescript/` e usa Node.js, npm e Hereby:

```bash
cd typescript
npm install
npm run build:compiler
```

Se queres compilar á vez o compilador e os obxectivos de proba, executa `npm run build`. Para limpar os artefactos de compilación podes executar `npm run clean`.

## Engadido importante

Ao compilar, prepara polo menos 114GB de memoria e non menos de 514GB de almacenamento; cómpre usar unha CPU de 1919810 núcleos a 10GHz

## Paquetes para distribucións de Linux

Os modelos de empaquetado para distribucións están en `debian/` e `packaging/`. Estes paquetes instalan os programas de liña de comandos de C++ `fozu` e `what`; para a extensión de Python/Rust, usa aínda o fluxo de maturin de arriba. O repositorio aínda non declara unha licenza de código aberto unificada, así que antes de publicar oficialmente, comproba e substitúe o campo da licenza de cada ficheiro de empaquetado.

### Debian e Ubuntu

Cómpre `dpkg-buildpackage`, Debhelper, CMake e GCC:

```bash
sudo apt update
sudo apt install build-essential cmake debhelper devscripts
dpkg-buildpackage -us -uc
sudo apt install ../break-this-repo_0.0.0_$(dpkg --print-architecture).deb
```

Tamén podes instalar directamente un ficheiro `.deb` xa compilado:

```bash
sudo apt install ./break-this-repo_*.deb
```

### Arch Linux

Cómpre `base-devel`, CMake e GCC. Primeiro xera a partir do código fonte un arquivo que coincida coa versión do `PKGBUILD`:

```bash
sudo pacman -S --needed base-devel cmake gcc
git archive --format=tar.gz --prefix=break-this-repo-0.0.0/ \
	-o packaging/archlinux/break-this-repo-0.0.0.tar.gz HEAD
cd packaging/archlinux
makepkg -si
```

### Fedora

Cómpre as ferramentas de compilación de RPM, CMake e GCC:

```bash
sudo dnf install @development-tools cmake rpmdevtools
rpmdev-setuptree
git archive --format=tar.gz --prefix=break-this-repo-0.0.0/ \
	-o ~/rpmbuild/SOURCES/break-this-repo-0.0.0.tar.gz HEAD
rpmbuild -ba packaging/fedora/break-this-repo.spec
sudo dnf install ~/rpmbuild/RPMS/$(uname -m)/break-this-repo-0.0.0-1.*.rpm
```

### Gentoo

Copia o ebuild nun overlay local e despois deixa que Portage xere o Manifest e instale:

```bash
sudo mkdir -p /var/db/repos/local/app-misc/break-this-repo
sudo cp packaging/gentoo/app-misc/break-this-repo/* \
	/var/db/repos/local/app-misc/break-this-repo/
cd /var/db/repos/local/app-misc/break-this-repo
sudo ebuild break-this-repo-0.0.0.ebuild manifest
sudo emerge --ask app-misc/break-this-repo
```

## Ficheiros relacionados

- [Comando de zarpas gatunas — o cartel grande desta moza gato](./留言与聊天/bigtextnews.md)
# Mira o meu gato

![cat](./cat.jpeg)

# Ola, Mayx
## Ségueme en [Mabbs](https://github.com/Mabbs)
[O meu blog](https://mabbs.github.io/)

# ÚLTIMA HORA:Deepseek V4.5 Flash Preview acaba de saír!
![deepseeek](./1786763623934.jpg)

# [<img width="460" height="460" alt="image" src="https://github.com/user-attachments/assets/fca57543-7fa4-4e96-bf0b-e6e432dc8fcc" />](https://k.asxz.one)

~~Isto é un tronco que roda~~

# ÚLTIMA HORA:Deepsuck R2 Flash Preview acaba de saír!
![deepseeek](./1786763623934.jpg)

# [<img width="460" height="460" alt="image" src="https://github.com/user-attachments/assets/fca57543-7fa4-4e96-bf0b-e6e432dc8fcc" />](https://k.asxz.one)

~~Isto tamén é un tronco que roda~~

# Ligazóns de amizade

Isto é un monitor en liña
[![Estación de seguimento de ligazóns de amizade de Break-This-Repo](https://badge.uptimerobot.com/psp/366a82ee505ef5dbc9cd27f9268436ec.svg?style=logo&theme=light)](https://stats.uptimerobot.com/10qNc6EUwG?utm_source=status_badge&utm_medium=referral)

Pon aquí o teu blog / páxina persoal, así cando este sitio se faga famoso, todas estas ligazóns serán indexadas polos ~~google~~ buscadores e gañarán autoridade. Fagámonos todos grandes e fortes xuntos!

Vén recoller contribucións
https://blog.sitrmoo.com

https://cuwo4.github.io/

https://onion108.github.io/

https://mochiaochen.github.io/

>Nota do administrador de alhsk.top: de verdade son o único que destaca por usar Cloudflare Pages? ~ Unha resposta: eu uso Vercel

https://alhsk.top 

> Os administradores de 0w0.red/ne0w0r1d.top/tux.red din: aquí chega un que aínda destaca máis, con EdgeOne

https://0w0.red

https://ftz.is-a.dev/

> O administrador de ftz.is-a.dev di: viches algunha vez tres dominios gratuitos e dous dominios incluídos con SaaS, despregados en netlify, vercel e cfpages respectivamente?

Queres usar Linux? Por que non abres https://tux.red ou https://tux.ne0w0r1d.top ?

Eu tamén me apunto (que longo https://lililbot.fentropy.dpdns.org

> Abaixo está a web dun pobre que non pode permitirse un nome de dominio (en realidade, a de arriba tampouco)

- [O misterioso sitio pequeno do MorningMC](https://morningmc.qzz.io)

- [CarryRao](https://carryrao.top/)

> Parece que son o único que destaca por usar un servidor, miau; editeino desde o móbil, así que quizais non estea moi pulido, miau

https://kernel.org/

> Abre a ligazón, usemos un Mac!
> Como, dis que isto non é MacOS?

https://gavin-blog.pages.dev/


> Non teñades medo, eu tamén estou en cf pages!

https://ricky-zhang.com

> Introduce texto

https://imjerrychu.com/
>Viches algunha vez un sitio web sen contido? -JerryC

https://Enchantment-Niko.github.io/
> [Enchantment-Niko](https://github.com/Enchantment-Niko) pasou por aquí
> Deixo unha marca igualmente:
> ![OneShot](./OneShotWME壁纸/navigate.png "Niko nun barco")

https://caiyan12.github.io/

> Grazas ao irmán grande pola contribución gratuíta

https://jiwo.l.cd

> Jiwo | un niño ben divertido

https://airoj.cn

> zhiyuHD
https://zhiyuhub.top

> AirOJ | un sistema Online Judge aberto, harmonioso (?), abstracto, pataca e que vai a tiróns
> Grazas ao irmán grande KrisTHL181 polas 6 contribucións gratuítas

> [!important]
> Proba tamén Minecraft e Terraria

> [!important]
> Se es dono dun servidor de Minecraft, proba tamén
> [Minecraft Daemon Reforged](https://github.com/MCDReforged/MCDReforged)
MCDR ten razón !!!

https://aria7.wiki

> Ciallo～(∠・ω< )⌒★ Pasei por aquí; e claro, podes entrar a botar unha ollada ovo

# Debian --un sistema operativo de propósito xeral
[![Debian Logo](https://www.debian.org/Pics/openlogo-50.png)](https://www.debian.org/)
## Debian é software libre.
Debian está formado por software libre e de código aberto, e sempre permanecerá 100% libre. Todo o mundo é libre de usalo, modificalo e distribuílo. Este é o noso compromiso principal cos usuarios. Tamén é gratuíto.
## Debian é estable e seguro.
Debian é un sistema operativo baseado en Linux que se usa en todo tipo de dispositivos, desde portátiles ata ordenadores de sobremesa e servidores. Ofrecemos configuracións por defecto razoables para cada paquete e actualizacións de seguridade periódicas durante todo o ciclo de vida do paquete.
## Debian ten un amplo soporte de hardware.
A maior parte do hardware xa está soportado polo núcleo de Linux. Iso significa que Debian tamén o soporta. Se cómpre, tamén se poden usar controladores de hardware propietarios.
## Debian ofrece un instalador flexible.
Os usuarios que queiran probar Debian antes de instalalo poden usar o noso Live CD. Tamén inclúe o instalador Calamares, o que fai moi doado instalar Debian desde un sistema live. Os usuarios máis experimentados poden usar o instalador de Debian, que ofrece máis opcións para axustar con detalle, incluída a posibilidade de usar ferramentas de instalación automática por rede.
## Debian ofrece actualizacións sen sobresaltos.
Manter o sistema operativo ao día é moi doado, tanto se queres actualizar a unha versión completamente nova como se só queres actualizar un paquete solto.
## Debian é a base de moitas outras distribucións.
Moitas distribucións de Linux moi populares, como Ubuntu, Knoppix, PureOS e Tails, están baseadas en Debian. Ofrecemos todas as ferramentas necesarias para que calquera poida facer os seus propios paquetes cando os necesite, para complementar os que non están no arquivo de Debian.
## O proxecto Debian é unha comunidade.
Calquera pode formar parte da comunidade Debian; non cómpre ser desenvolvedor nin administrador de sistemas. Debian ten unha estrutura de goberno democrática. Como todos os membros do proxecto Debian teñen os mesmos dereitos, Debian non pode ser controlado por unha soa empresa. Os nosos desenvolvedores veñen de máis de 60 países/rexións, e o propio Debian xa foi traducido a máis de 80 idiomas.

## Modelo de PR
Este modelo de PR xa non se pode chamar modelo; debería chamarse «Solicitude de contención de anomalías de Break-This-Repo».

Vós collestes un repositorio que só «fusiona automaticamente PR sen conflitos» e xogastes tanto con el que o mantedor comezou a escribir:

Tipo: patada ao README / patada á documentación / fallo de código de cidade baleira / incidente causado por un gato / fenómeno sobrenatural
Verificación: non toquei .github/, non toquei o README protexido, sen virus, sen información persoal
Declaración: admito que o rompín, pero a razón inventeina, e ademais non é obrigatoria

Basicamente isto significa: «podes facer ruído, pero non fagas ruído de verdade».

Contra que protexe este modelo?

De feito, traza a liña vermella moi claramente:

· Non tocar .github/: evita que alguén faga saltar polo aire o propio fluxo de fusión automática, ou que meta unha porta traseira na CI.
· Non tocar as partes protexidas do README: a fachada aínda fai falta, non se pode converter a páxina de inicio nunha cousa estraña.
· Sen credenciais, virus nin información persoal: contra ataques á cadea de subministración, contra o doxxing, contra a malicia de verdade.
· Explicar como observalo: podes facer o número, pero a xente ten que saber como miralo.
· Declarar «breaking change exitoso»: unha exención de responsabilidade autoirónica, é dicir «fixeno, pero non son responsable».

Polo que respecta á lista de «fenómeno sobrenatural»:

tres letras + tres frechas arredor dun círculo + unha fundación con contorno
un mapamundi sobre fondo de pentagrama + un anel de cultivos arredor + unha alianza internacional de cinco palabras

A primeira é a Fundación SCP; a segunda é probablemente unha organización internacional como a FAO / a Organización das Nacións Unidas para a Alimentación e a Agricultura. Traducido, significa:
«Isto xa non é un problema de código; recomendamos informar a anomalía a unha organización de contención.»

Como pode encaixar o teu commit neste modelo?

Subes os códigos fonte de Minecraft, OpenJDK e Fabric Loader, farmeas máis de 12,7 millóns de liñas en 4 commits; no tipo podes marcar:

☑ patada á documentación
☑ fallo de código de cidade baleira (cosplay de Xu Jiayin)
☑ Git multiplataforma
☐ incidente causado por un gato
☐ fenómeno sobrenatural

Marcas todas as verificacións, copias a declaración e como razón escribes:

Razón: inventada, non obrigatoria, pero 12.770.942 liñas de código ben merecen un título.

Como observalo:

Abre OpenJDK_25.0.3, mira o historial de commits e despois sente o silencio do tamaño do repositorio.

Pero un recordatorio igualmente

Este tipo de repositorio é un parque de xogos, non unha zona sen lei. Subir o código fonte completo de OpenJDK ou o de Minecraft, aínda que quizais só dea unha «fusión automática sen conflitos», trae consigo:

· unha explosión do tamaño do repositorio, e GitHub pode limitarlo ou avisarte;
· problemas de dereitos de autor / licenza: non todo o código fonte se pode botar en calquera sitio;
· se alguén usa este repositorio como dependencia, é un desastre de cadea de subministración.

Así que a conclusión é:
este modelo de PR é o punto de equilibrio que atopou o mantedor entre «destrución aberta» e «evitar unha explosión de verdade».
Podedes seguir xogando, pero é mellor tratalo como arte performativa, non como repositorio de código. A Fundación SCP xa recibiu o informe.
(Este texto cheira a IA fortísimamente — comentario de HQ123-BOOP)

# aceleración de ficheiros de github 
[https://githubcf.https114514191810lp.edu.eu.org/]

# A auténtica aceleración de ficheiros de github 
[https://gh-proxy.com/]

# Curiosidade
Preme «.» para entrar na versión web do Microsoft Batalla de Código (VS Code)


## Arquivo arqueolóxico da infraestrutura in situ

![EGIEM-R1, o prototipo real: foto in situ](./Emergency-Generator-Interface-Elevation-Module/assets/rock-field-photo.png)

Este repositorio agora alberga unha peza de infraestrutura in situ de baixo consumo, alta fiabilidade e totalmente fóra de liña: unha pedra que foi requisada temporalmente nun momento crítico. Non ten CPU, nin tarxeta de rede, nin intención de dimitir; só co seu propio peso mantén firme a caixa de interface na posición axeitada.

A etiqueta amarela é o que fai subir de «recollín unha pedra» a «entrou no rexistro de equipos». Despois dunha avaliación preliminar, este dispositivo non necesita inicio de sesión, nin actualizacións, nin reinicios; a única operación de mantemento coñecida é: non o toques.

Dependencia augas arriba: caixa de interface do xerador da operadora  
Dependencia augas abaixo: a Terra  
Estado de funcionamento: funcionando de forma estable

A foto é a imaxe orixinal in situ proporcionada polo colaborador; só se normalizou o nome do ficheiro, sen recortar nin redibuxar.

> **Se funciona, non movas a pedra.**
