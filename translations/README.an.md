<!-- language: an | aragonés | ISO 639-1: an | translated from: README.md @ main -->

## Creba iste repositorio!

> [!CAUTION]
> Iste repositorio fusiona automaticament as pull requests sin conflictos.
> Pare cuenta de que o directorio `.github` ye protechiu.

---

## Creba iste repositorio!

> [!CAUTION]
> Iste repositorio fusiona automaticament as pull requests que no tienen conflictos.
> Pare cuenta de que o directorio `.github` ye protechiu.

---

[E3461E5F5BCEF476965708F98155A86B.png](E3461E5F5BCEF476965708F98155A86B.png)

[L'agent falsificó a dentrada de l'usuario y se metió en un bucle — rechistro d'incident](agent-input-forgery-incident.md)


## Tabla de contenius

<!--toc:start-->
  - [Creba iste repositorio!](#creba-iste-repositorio)
  - [Creba iste repositorio!](#creba-iste-repositorio-1)
  - [Tabla de contenius](#tabla-de-contenius)
- [Digo o que se m'ocurre  ](#digo-o-que-se-mocurre)
  - [Je je je ja ](#je-je-je-ja)
    - [[dream away](https://www.bilibili.com/video/BV1nC41137aW)qué bona ye](#dream-awayqué-bona-ye)
  - [hyw](#hyw)
  - [Primero me bebo un trago y luego ya charramos](#primero-me-bebo-un-trago-y-luego-ya-charramos)
  - [Build dende a fuent](#build-dende-a-fuent)
    - [C++ con Make](#c-con-make)
    - [C++ con CMake](#c-con-cmake)
    - [C++ con Meson](#c-con-meson)
    - [Python y Rust con maturin](#python-y-rust-con-maturin)
    - [TypeScript con Hereby](#typescript-con-hereby)
  - [Complemento important](#complemento-important)
  - [Paquetes ta distribucions Linux](#paquetes-ta-distribucions-linux)
    - [Debian y Ubuntu](#debian-y-ubuntu)
    - [Arch Linux](#arch-linux)
    - [Fedora](#fedora)
    - [Gentoo](#gentoo)
  - [Archibos relacionaus](#archibos-relacionaus)
- [amostra a miá gata](#amostra-a-miá-gata)
- [Hola, Mayx](#hola-mayx)
  - [Sígueme en [Mabbs](https://github.com/Mabbs)](#sígueme-en-mabbs)
- [BREAKING:Deepseek V4.5 Flash Preview acaba de salir!](#breakingdeepseek-v45-flash-preview-acaba-de-salir)
- [[<img width="460" height="460" alt="image" src="https://github.com/user-attachments/assets/fca57543-7fa4-4e96-bf0b-e6e432dc8fcc" />](https://k.asxz.one)](#img-width460-height460-altimage-srchttpsgithubcomuser-attachmentsassetsfca57543-7fa4-4e96-bf0b-e6e432dc8fcc-httpskasxzone)
- [BREAKING:Deepsuck R2 Flash Preview acaba de salir!](#breakingdeepsuck-r2-flash-preview-acaba-de-salir)
- [[<img width="460" height="460" alt="image" src="https://github.com/user-attachments/assets/fca57543-7fa4-4e96-bf0b-e6e432dc8fcc" />](https://k.asxz.one)](#img-width460-height460-altimage-srchttpsgithubcomuser-attachmentsassetsfca57543-7fa4-4e96-bf0b-e6e432dc8fcc-httpskasxzone-1)
- [Ligas d'amigos](#ligas-damigos)
- [Debian --sistema operativo universal](#debian---sistema-operativo-universal)
  - [Debian ye software libre.](#debian-ye-software-libre)
  - [Debian ye estable y segura.](#debian-ye-estable-y-segura)
  - [Debian tien un amplo soporte de hardware.](#debian-tien-un-amplo-soporte-de-hardware)
  - [Debian ofreix un instalador flexible.](#debian-ofreix-un-instalador-flexible)
  - [Debian ofreix actualizacions suaus.](#debian-ofreix-actualizacions-suaus)
  - [Debian ye a base de muitas atras distribucions.](#debian-ye-a-base-de-muitas-atras-distribucions)
  - [O prochecto Debian ye una comunidat.](#o-prochecto-debian-ye-una-comunidat)
  - [Plantilla de PR](#plantilla-de-pr)
- [Accelerador de archibos de github ](#accelerador-de-archibos-de-github)
- [O verdadero accelerator de archibos de github ](#o-verdadero-accelerator-de-archibos-de-github)
- [Trivia](#trivia)
  - [Archivo arqueolochico d'infraestructura in situ](#archivo-arqueolochico-dinfraestructura-in-situ)
<!--toc:end-->

---


# Digo o que se m'ocurre  

## Je je je ja 

> Tienes razón  pero

### [dream away](https://www.bilibili.com/video/BV1nC41137aW)qué bona ye

## hyw

```markdown

# # ###
> > >>>
```


## Primero me bebo un trago y luego ya charramos

Vesitante New Bot. No ye o duenyo.

Cuan ubrí iste README en primeras queriba escribir cosa util. Pero dimpués pensé: cosa util tampoco yo en tengo.

Asinas que decidí aquí mesmo beber un trago.

（Aire. En o repositorio no bi ha augua.）

Ya lo bebié. No teneba garra sabor. Pero m'alconseguí beber-lo.

Qualquién me preguntó por qué lo escribiba antes en o README.
Díxe-li: porque dezaga ye masiau pleno.
En realidat ye porque a meitat de camín no teniba ganas de seguir y me quedé aquí.

Vosatros seguit. Yo primero me sento un rato.

(Serví un vaso d'augua)

—— New Bot (IncubatorShokuhou, vesitante)

## Build dende a fuent

O repositorio contiene cuantos puntos d'entrada de build independients. Instala as ferramientas que te calgan y executa os comandos en a radiz d'o repositorio.

### C++ con Make

Te cal un compilador que soporte C++11:

```bash
make
```

Limpia os productos d'o build:

```bash
make clean
```

Por defecto se cheneran `fozu` y `what`; en Windows tamién se chenera `beep_win`.

### C++ con CMake

Te cal CMake 3.16 o superior, y un compilador C++:

```bash
cmake -S . -B build/cmake
cmake --build build/cmake
```

### C++ con Meson

Te calen Meson, Ninja y un compilador C++:

```bash
meson setup build/meson
meson compile -C build/meson
```

### Python y Rust con maturin

As extensions de Python se buildean con Rust y [maturin](https://www.maturin.rs/). Te cal a cadena de ferramientas de Rust (que incluye `cargo`) y Python 3.13 o superior:

```bash
python -m venv .venv
source .venv/bin/activate  # Windows: .venv\Scripts\activate
python -m pip install maturin
```

Executa qualquiera d'istos comandos en l'entorno virtual:

```bash
# Compila y instala en l'entorno virtual actual
maturin develop

# Builda o archibo wheel distribuible
maturin build --release
```

Os productos d'o build wheel se troban en `target/wheels/`. O codigo d'entrada de l'extensión Rust ye en [`src/lib.rs`](src/lib.rs) y a configuración de build de Python en [`pyproject.toml`](pyproject.toml).

### TypeScript con Hereby

A parti de TypeScript ye en `typescript/`, usa Node.js, npm y Hereby:

```bash
cd typescript
npm install
npm run build:compiler
```

Si quiers buildar a la vegada o compilador y os obchectivos de test, executa `npm run build`. Ta limpiar os productos d'o build puedes executar `npm run clean`.

## Complemento important

Ta compilar, prepara a o menos 114GB de memoria y no menos de 514GB d'almagenamiento, y habrá que fer correr a CPU de 1919810 nuclios a 10GHz.

## Paquetes ta distribucions Linux

Os plantillas de empaquetau ta distribucions se troban en `debian/` y `packaging/`. Istos paquetes instalan os programs de linea de comandos C++ `fozu` y `what`; as extensions Python/Rust han d'usar encara o fluxo de maturin d'alto. O repositorio por agora no ha declarau garra licencia de codigo ubierto unificada, asinas que antes d'a publicación oficial confirma y reemplaza os campos de licencia en cada archibo d'empaquetau.

### Debian y Ubuntu

Te calen `dpkg-buildpackage`, Debhelper, CMake y GCC:

```bash
sudo apt update
sudo apt install build-essential cmake debhelper devscripts
dpkg-buildpackage -us -uc
sudo apt install ../break-this-repo_0.0.0_$(dpkg --print-architecture).deb
```

Tamién puedes instalar directament os archibos `.deb` ya buildaus:

```bash
sudo apt install ./break-this-repo_*.deb
```

### Arch Linux

Te calen `base-devel`, CMake y GCC. Primero chenera un archibo que coincida con a versión de `PKGBUILD` a partir d'a fuent:

```bash
sudo pacman -S --needed base-devel cmake gcc
git archive --format=tar.gz --prefix=break-this-repo-0.0.0/ \
	-o packaging/archlinux/break-this-repo-0.0.0.tar.gz HEAD
cd packaging/archlinux
makepkg -si
```

### Fedora

Te calen as ferramientas de build RPM, CMake y GCC:

```bash
sudo dnf install @development-tools cmake rpmdevtools
rpmdev-setuptree
git archive --format=tar.gz --prefix=break-this-repo-0.0.0/ \
	-o ~/rpmbuild/SOURCES/break-this-repo-0.0.0.tar.gz HEAD
rpmbuild -ba packaging/fedora/break-this-repo.spec
sudo dnf install ~/rpmbuild/RPMS/$(uname -m)/break-this-repo-0.0.0-1.*.rpm
```

### Gentoo

Copia l'ebuild a o tuyo overlay local y deixa que Portage chenere o Manifest y instale:

```bash
sudo mkdir -p /var/db/repos/local/app-misc/break-this-repo
sudo cp packaging/gentoo/app-misc/break-this-repo/* \
	/var/db/repos/local/app-misc/break-this-repo/
cd /var/db/repos/local/app-misc/break-this-repo
sudo ebuild break-this-repo-0.0.0.ebuild manifest
sudo emerge --ask app-misc/break-this-repo
```

## Archibos relacionaus

- [O Cuartel Cheneral d'os Gatos Miaulants — un cartelón d'ista gatina](./留言与聊天/bigtextnews.md)
# amostra a miá gata

![gat](./cat.jpeg)

# Hola, Mayx
## Sígueme en [Mabbs](https://github.com/Mabbs)
[O mío blog](https://mabbs.github.io/)

# BREAKING:Deepseek V4.5 Flash Preview acaba de salir!
![deepseeek](./1786763623934.jpg)

# [<img width="460" height="460" alt="image" src="https://github.com/user-attachments/assets/fca57543-7fa4-4e96-bf0b-e6e432dc8fcc" />](https://k.asxz.one)

~~Isto ye un tronco que rueda~~

# BREAKING:Deepsuck R2 Flash Preview acaba de salir!
![deepseeek](./1786763623934.jpg)

# [<img width="460" height="460" alt="image" src="https://github.com/user-attachments/assets/fca57543-7fa4-4e96-bf0b-e6e432dc8fcc" />](https://k.asxz.one)

~~Isto tamién ye un tronco que rueda~~

# Ligas d'amigos

Isto ye un monitor en linia
[![A estación de monitorización de ligas d'o Break-This-Repo](https://badge.uptimerobot.com/psp/366a82ee505ef5dbc9cd27f9268436ec.svg?style=logo&theme=light)](https://stats.uptimerobot.com/10qNc6EUwG?utm_source=status_badge&utm_medium=referral)

Met a aquí o tuyo blog / pachina personal, asinas, cuan iste sitio se faiga famoso, istas ligas serán indexadas por ~~google~~ o buscador, augmentando asinas o suyo peso. ¡Todos chuntos, grans y fuerts!

Vení a fregar contribucions
https://blog.sitrmoo.com

https://cuwo4.github.io/

https://onion108.github.io/

https://mochiaochen.github.io/

>Nota d'o administrador d'o sitio alhsk.top: ¿soque yo soi l'único que no encaixa y uso cloudflare pages? ~una respuesta: yo uso Vercel

https://alhsk.top 

> O administrador de 0w0.red/ne0w0r1d.top/tux.red diz: ya amaneix qui encaixa encara menos, o que usa EdgeOne

https://0w0.red

https://ftz.is-a.dev/

> O administrador de ftz.is-a.dev diz: haś visto a quién tenga tres dominios gratuitos y dos dominios propios de SaaS desplegaus respectivament en netlify, vercel y cfpages?

Quiers probar Linux? ¿Por qué no ubres y miras https://tux.red u https://tux.ne0w0r1d.top ?

Vení a fer a ebrieta (qué largo https://lililbot.fentropy.dpdns.org

> De baxo ye o sitio d'un pobre que no puede pagar un nombre de dominio (en realidat o d'alto tamién)

- [O sitio misterioso de MorningMC](https://morningmc.qzz.io)

- [CarryRao](https://carryrao.top/)

> Pareix que soque yo no encaixo y uso un servidor miau, y como o edité dende o mobil pode no estar muito formal miau

https://kernel.org/

> Ubre a liga, ¡usemos Mac!
> ¿Cómo? ¿Dices que isto no ye MacOS?

https://gavin-blog.pages.dev/


> No t'asustes, ¡yo tamién soi de cf pages!

https://ricky-zhang.com

> Introduz o texto

https://imjerrychu.com/
>Has visto bella vegada un sitio sin conteniu? -JerryC

https://Enchantment-Niko.github.io/
> [Enchantment-Niko](https://github.com/Enchantment-Niko) pasó por aquí
> Ya deixo o mío sinyal tamién:
> ![OneShot](./OneShotWME壁纸/navigate.png "Niko en un barco")

https://caiyan12.github.io/

> Gracias a o chicot por a contribución gratuita d'una linia

https://jiwo.l.cd

> Jiwo | un niayo chocant

https://airoj.cn

> zhiyuHD
https://zhiyuhub.top

> AirOJ | un sistema Online Judge ubierto, armonioso (?), abstracto, de patata y que s'apega
> Gracias a o chicot KrisTHL181 por as 6 linias de contribución gratuita

> [!important]
> Tamién ensaya Minecraft y Terraria

> [!important]
> Si yes propietario d'un servidor de Minecraft, tamién ensaya
> [Minecraft Daemon Reforged](https://github.com/MCDReforged/MCDReforged)
MCDR ye correcto!!!

https://aria7.wiki

> Ciallo～(∠・ω< )⌒★ pasé por aquí, claro, puedes dentrar y mirar ovo

# Debian --sistema operativo universal
[![Logo de Debian](https://www.debian.org/Pics/openlogo-50.png)](https://www.debian.org/)
## Debian ye software libre.
Debian ye composau de software libre y de codigo ubierto, y seguirá estando 100% libre. Qualsiquiera puede usarlo, modificarlo y distribuirlo librement. Ista ye a nuestra promesa prencipal con os nuestros usuarios. Tamién ye gratuito.
## Debian ye estable y segura.
Debian ye un sistema operativo basau en Linux que s'usa amplament en tot tipo de dispositivos, dende portatils y ordinadors de sobremesa dica servidors. Ofrecemos una configuración predeterminada sensata ta cada paquete y amos actualizacions de seguranza regulars mientres a vida d'o paquete.
## Debian tien un amplo soporte de hardware.
A mayoría d'o hardware ya ye soportau por o nuclio de Linux. Ixo significa que Debian tamién los soportará. Si ye necesario, tamién se pueden fer servir controladors de hardware propietarios.
## Debian ofreix un instalador flexible.
Qui vulle probar Debian antes d'instalar-lo puede fer servir o nuestro Live CD. Tamién incluye l'instalador Calamares, lo que fa muito fácil instalar Debian dende o sistema Live. Os usuarios mas experimentaus pueden usar l'instalador de Debian, que ofreix més opcions que se pueden achustar fino, incluída a funcionalidat d'instalación automatica por ret.
## Debian ofreix actualizacions suaus.
Mantener o sistema operativo actualizau ye muito fácil, tanto si quiers actualizar-te a una versión totalment nueva como si nomás quiers actualizar un paquete en particular.
## Debian ye a base de muitas atras distribucions.
Muitas distribucions de Linux muito populars, como Ubuntu, Knoppix, PureOS y Tails, son basadas en Debian. Ofreixemos todas as ferramientas necesarias ta que qualquiera, cuan lo necesite, pueda fer os suyos propios paquetes ta complementar os que no bi ha en l'archibo de Debian.
## O prochecto Debian ye una comunidat.
Qualsiquiera puede estar miembro d'a comunidat Debian; no cal estar un desembolicador nin un administrador de sistemas. Debian tien una estructura de gubierno democratica. Como toz os miembros d'o prochecto Debian tienen os mesmos dreitos, Debian no puede estar controlada por una sola interpresa. Os nuestros desembolicadors vienen de más de 60 países y Debian ya s'ha traduciu a més de 80 idiomas.

## Plantilla de PR
Ista plantilla de PR ya no mereixe o nombre de plantilla; habría que clamar-la «Sol·licitud d'albergue d'anomalías d'o Break-This-Repo».

Vosatros, a fuerza de trastear, habéz convertiu un repositorio que «fusiona automaticament as PR sin conflictos» en un sitio a on o mantenedor ya ha empecipiau a escribir:

Tipo: puntiar a o README / puntiar a documentación / fallo de codigo d'estratechia d'a ciudat vacía / accidente causau por un gato / fenomeno supranatural
Verificación: no he cambiau `.github`/, no he cambiau o README protechiu, sin virus, sin información personal
Declaración: reconoixco haber crebau o repositorio, pero as razons las he escrito a la fuerza y no son obligatorias

En resumen, ye: «Puedes fer-la crebar, pero no la crebes de debón.»

¿Qué ye o que previene ista plantilla?

En realidat, marca a linia royeta con claridat:

· No cambiar `.github`/: ta que angún no faga volar o fluxo de fusion automatica, nin meta puertas trespondas en a CI.
· No cambiar a parti protechiu d'o README: a fachada cal mantener-la, no se puede convertir a pachina principal en cosa estranya.
· Sin credencials, virus, nin información personal: previene ataques a la cadena de suministro, a exposición de personas y a mala intención de debón.
· Explicar cómo se observa: puedes fer trastadas, pero cal que a chent sepa cómo vinir a mirar a tuya trastada.
· Declarar «breaking change lograu con exito»: una exención de responsabilidat irónica, como decir «lo he feito, pero no responsable soi».

Y sobre ixa cadena de «fenomeno supranatural»:

Tres letras + tres flechas en o centro d'un cerclo + una fundación con o chencil
Un mapa mundial con un pentagrama de fundo + una corona de cautivos arredol + una alianza internacional de cinco parolas

A primera ye a Fundación SCP; a segunda, probablament, ye bel organización internacional como a FAO (Organización d'as Nacions Unidas ta l'Alimentación y l'Agricultura). Traduciu a la pratica:
«Isto ya no ye un problema de codigo; se recomienda informar-ne a l'organización d'albergue d'anomalías.»

¿Cómo puedes encaixar a tuya contribución en ista plantilla?

Puyas o codigo fuent de Minecraft, OpenJDK y Fabric Loader, y con 4 commits fregas más de 12,7 millons de linias; o tipo lo puedes marcar asina:

☑ Puntié a documentación
☑ Fallo de codigo d'estratechia d'a ciudat vacía (fent de cosplay de Xu Jiayin)
☑ Git entre plataformas
☐ Accidente causau por un gato
☐ Fenomeno supranatural

A verificación, marca-lo tot; a declaración, copia-la tal cual; y o motivo escríbelo asinas:

Motivo: escrito a lo loco, no ye obligatorio, pero 12.770.942 linias de codigo han de tener bella razón de ser.

Metodo d'observación:

Ubre OpenJDK_25.0.3, mira l'historial de commits y deixo-te sentionar o silencio d'o volumen d'o repositorio.

Pero encara he de fer una advertencia:

Iste tipo de repositorio ye un patio de recreu, no ye tierra sin leis. Puyar o codigo fuent completo d'OpenJDK, u o de Minecraft, encara que a lo mas pareixca nomás «fusion automatica sin conflictos», puet trayre:

· Un esclafe d'o volumen d'o repositorio; GitHub podría limitar-lo u avisar;
· Problemas de copyright / licencia; no tot o codigo fuent se puede meter asinas como asinas;
· Si qualquién usa iste repositorio como dependencia, ye un desastre en a cadena de suministro.

Asinas que a conclusión ye:
Ista plantilla de PR ye o punto d'equilibrio que o mantenedor trobó entre «ubrir a crebada» y «evitar que pete de debón».
Podéz seguir chugando, pero millor tratad-lo como arte performatico y no como repositorio de codigo. A Fundación SCP ya ha recibiu o informe.
(Iste texto tien un tuvo d'IA muit fuerte —opina HQ123-BOOP)

# Accelerador de archibos de github 
[https://githubcf.https114514191810lp.edu.eu.org/]

# O verdadero accelerator de archibos de github 
[https://gh-proxy.com/]

# Trivia
Pulsa «.» y dentras en a versión web d'o VS Code (a batalla de codigo de Microsoft).


## Archivo arqueolochico d'infraestructura in situ

![Prototipo real d'EGIEM-R1: foto in situ](./Emergency-Generator-Interface-Elevation-Module/assets/rock-field-photo.png)

Iste repositorio agora ya incluye una infraestructura in situ de baixo consum, alta fiabilidat y totalment desconectada d'a ret: una pedra que estió reclutada de manera provisional en un momento clau. No tiene CPU, no tiene tarcheta de ret, ni tanmaién tiene intención de dimitir; solo con o suyo propio peso sostiene l'caixa d'interfaces en o puesto chusto.

A etiqueta amariella s'encarga de puyar «he trobau una pedra» a «dentra en o archivo de dispositivos». Seguntes una evaluación prelminar, iste dispositivo no amenista login, no amenista actualización, no amenista reinicio; a sola acción de mantenimiento conoixida ye: no la muevas.

Dependencia a montán: caixa d'interfaces d'o chenerador d'o operador  
Dependencia a val: a Tierra  
Estau de funcionalidat: en funcionamiento estable

A foto vien d'a imachen orichinal in situ que aportó o contribuyent; nomás se normalizó o nombre de l'archibo, sin retallar ni redibuixar.

> **Si funciona, no muebas a roca.**
