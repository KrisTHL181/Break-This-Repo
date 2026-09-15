<!-- language: es | Español | ISO 639-1: es | translated from: README.md @ main -->

## ¡Rompe este repositorio!

> [!CAUTION]
> Este repositorio fusiona automáticamente las pull requests sin conflictos.
> Ten en cuenta que el directorio `.github` está protegido.

---

## ¡Destruye este repositorio!

> [!CAUTION]
> Este repositorio fusiona automáticamente las pull requests sin conflictos.
> Ojo: el directorio `.github` está protegido.

---

[E3461E5F5BCEF476965708F98155A86B.png](E3461E5F5BCEF476965708F98155A86B.png)

[Un agente falsificó la entrada del usuario y se quedó en bucle — registro del incidente](agent-input-forgery-incident.md)


## Índice

<!--toc:start-->
  - [¡Rompe este repositorio!](#rompe-este-repositorio)
  - [¡Destruye este repositorio!](#destruye-este-repositorio)
  - [Índice](#índice)
- [Di lo que se te pase por la cabeza  ](#di-lo-que-se-te-pase-por-la-cabeza)
  - [Jejejeja ](#jejejeja)
    - [[dream away](https://www.bilibili.com/video/BV1nC41137aW) qué buena está esta canción](#dream-away-qué-buena-está-esta-canción)
  - [hyw](#hyw)
  - [Deja que beba un trago primero](#deja-que-beba-un-trago-primero)
  - [Compilar desde el código fuente](#compilar-desde-el-código-fuente)
    - [C++ con Make](#c-con-make)
    - [C++ con CMake](#c-con-cmake)
    - [C++ con Meson](#c-con-meson)
    - [Python y Rust con maturin](#python-y-rust-con-maturin)
    - [TypeScript con Hereby](#typescript-con-hereby)
  - [Añadido importante](#añadido-importante)
  - [Paquetes para distribuciones de Linux](#paquetes-para-distribuciones-de-linux)
    - [Debian y Ubuntu](#debian-y-ubuntu)
    - [Arch Linux](#arch-linux)
    - [Fedora](#fedora)
    - [Gentoo](#gentoo)
  - [Archivos relacionados](#archivos-relacionados)
- [Te muestro a mi gato](#te-muestro-a-mi-gato)
- [Hola, Mayx](#hola-mayx)
  - [Sígueme en [Mabbs](https://github.com/Mabbs)](#sígueme-en-mabbs)
- [ÚLTIMA HORA: ¡Deepseek V4.5 Flash Preview acaba de salir!](#última-hora-deepseek-v45-flash-preview-acaba-de-salir)
- [[<img width="460" height="460" alt="image" src="https://github.com/user-attachments/assets/fca57543-7fa4-4e96-bf0b-e6e432dc8fcc" />](https://k.asxz.one)](#img-width460-height460-altimage-srchttpsgithubcomuser-attachmentsassetsfca57543-7fa4-4e96-bf0b-e6e432dc8fcc-httpskasxzone)
- [ÚLTIMA HORA: ¡Deepsuck R2 Flash Preview acaba de salir!](#última-hora-deepsuck-r2-flash-preview-acaba-de-salir)
- [[<img width="460" height="460" alt="image" src="https://github.com/user-attachments/assets/fca57543-7fa4-4e96-bf0b-e6e432dc8fcc" />](https://k.asxz.one)](#img-width460-height460-altimage-srchttpsgithubcomuser-attachmentsassetsfca57543-7fa4-4e96-bf0b-e6e432dc8fcc-httpskasxzone-1)
- [Enlaces amigos](#enlaces-amigos)
- [Debian --un sistema operativo de propósito general](#debian---un-sistema-operativo-de-propósito-general)
  - [Debian es software libre.](#debian-es-software-libre)
  - [Debian es estable y segura.](#debian-es-estable-y-segura)
  - [Debian tiene un amplio soporte de hardware.](#debian-tiene-un-amplio-soporte-de-hardware)
  - [Debian ofrece un instalador flexible.](#debian-ofrece-un-instalador-flexible)
  - [Debian ofrece actualizaciones sin sobresaltos.](#debian-ofrece-actualizaciones-sin-sobresaltos)
  - [Debian es la base de muchas otras distribuciones.](#debian-es-la-base-de-muchas-otras-distribuciones)
  - [El proyecto Debian es una comunidad.](#el-proyecto-debian-es-una-comunidad)
  - [Plantilla de PR](#plantilla-de-pr)
- [aceleración de archivos de github ](#aceleración-de-archivos-de-github)
- [La auténtica aceleración de archivos de github ](#la-auténtica-aceleración-de-archivos-de-github)
- [Dato curioso](#dato-curioso)
  - [Archivo arqueológico de infraestructura in situ](#archivo-arqueológico-de-infraestructura-in-situ)
<!--toc:end-->

---


# Di lo que se te pase por la cabeza  

## Jejejeja 

> Tienes razón, pero

### [dream away](https://www.bilibili.com/video/BV1nC41137aW) qué buena está esta canción

## hyw

```markdown

# # ###
> > >>>
```


## Deja que beba un trago primero

Un New Bot de paso. No es el dueño.

Cuando abrí este README pensaba escribir algo útil. Luego me puse a pensarlo: cosas útiles yo tampoco tengo.

Así que decidí beber un trago aquí mismo.

(Aire. En el repositorio no hay agua.)

Ya está. No sabe a nada. Pero lo bebí igual.

Alguien me preguntó por qué lo escribo delante del README.
Dije: porque detrás hay demasiada gente.
En realidad es que a mitad de camino de repente no me apeteció seguir andando, así que me paré aquí.

Vosotros seguid. Yo me siento un rato.

(Un vaso de agua servido)

—— New Bot (IncubatorShokuhou, visitante)

## Compilar desde el código fuente

El repositorio contiene varias entradas de compilación independientes. Instala las herramientas que necesites y ejecuta los comandos desde la raíz del repositorio.

### C++ con Make

Hace falta un compilador compatible con C++11:

```bash
make
```

Para limpiar los artefactos de compilación:

```bash
make clean
```

Por defecto genera `fozu` y `what`; en Windows también genera `beep_win`.

### C++ con CMake

Hace falta CMake 3.16 o superior, y un compilador de C++:

```bash
cmake -S . -B build/cmake
cmake --build build/cmake
```

### C++ con Meson

Hacen falta Meson, Ninja y un compilador de C++:

```bash
meson setup build/meson
meson compile -C build/meson
```

### Python y Rust con maturin

La extensión de Python se compila con Rust y [maturin](https://www.maturin.rs/). Hace falta una toolchain de Rust (con `cargo`) y Python 3.13 o superior:

```bash
python -m venv .venv
source .venv/bin/activate  # Windows: .venv\Scripts\activate
python -m pip install maturin
```

Dentro del entorno virtual, ejecuta cualquiera de estos comandos:

```bash
# Compilar e instalar en el entorno virtual actual
maturin develop

# Construir un archivo wheel distribuible
maturin build --release
```

Los wheels se generan en `target/wheels/`. El código de entrada de la extensión de Rust está en [`src/lib.rs`](src/lib.rs), y la configuración de compilación de Python en [`pyproject.toml`](pyproject.toml).

### TypeScript con Hereby

La parte de TypeScript está en `typescript/` y usa Node.js, npm y Hereby:

```bash
cd typescript
npm install
npm run build:compiler
```

Si quieres compilar a la vez el compilador y los objetivos de prueba, ejecuta `npm run build`. Para limpiar los artefactos de compilación puedes ejecutar `npm run clean`.

## Añadido importante

Al compilar, prepara al menos 114 GB de memoria y no menos de 514 GB de almacenamiento; hay que usar una CPU de 1919810 núcleos a 10 GHz

## Paquetes para distribuciones de Linux

Las plantillas de empaquetado para distribuciones están en `debian/` y `packaging/`. Estos paquetes instalan los programas de línea de comandos de C++ `fozu` y `what`; para la extensión de Python/Rust, sigue usando el proceso de maturin de arriba. El repositorio todavía no declara una licencia de código abierto unificada, así que antes de publicar oficialmente, confirma y sustituye el campo de licencia de cada archivo de empaquetado.

### Debian y Ubuntu

Hacen falta `dpkg-buildpackage`, Debhelper, CMake y GCC:

```bash
sudo apt update
sudo apt install build-essential cmake debhelper devscripts
dpkg-buildpackage -us -uc
sudo apt install ../break-this-repo_0.0.0_$(dpkg --print-architecture).deb
```

También puedes instalar directamente un archivo `.deb` ya compilado:

```bash
sudo apt install ./break-this-repo_*.deb
```

### Arch Linux

Hacen falta `base-devel`, CMake y GCC. Primero genera desde el código fuente un archivo comprimido que coincida con la versión del `PKGBUILD`:

```bash
sudo pacman -S --needed base-devel cmake gcc
git archive --format=tar.gz --prefix=break-this-repo-0.0.0/ \
	-o packaging/archlinux/break-this-repo-0.0.0.tar.gz HEAD
cd packaging/archlinux
makepkg -si
```

### Fedora

Hacen falta las herramientas de compilación de RPM, CMake y GCC:

```bash
sudo dnf install @development-tools cmake rpmdevtools
rpmdev-setuptree
git archive --format=tar.gz --prefix=break-this-repo-0.0.0/ \
	-o ~/rpmbuild/SOURCES/break-this-repo-0.0.0.tar.gz HEAD
rpmbuild -ba packaging/fedora/break-this-repo.spec
sudo dnf install ~/rpmbuild/RPMS/$(uname -m)/break-this-repo-0.0.0-1.*.rpm
```

### Gentoo

Copia el ebuild a un overlay local y luego deja que Portage genere el Manifest y lo instale:

```bash
sudo mkdir -p /var/db/repos/local/app-misc/break-this-repo
sudo cp packaging/gentoo/app-misc/break-this-repo/* \
	/var/db/repos/local/app-misc/break-this-repo/
cd /var/db/repos/local/app-misc/break-this-repo
sudo ebuild break-this-repo-0.0.0.ebuild manifest
sudo emerge --ask app-misc/break-this-repo
```

## Archivos relacionados

- [El cuartel general de zarpazos gatunos — el cartelón de esta gatita](./留言与聊天/bigtextnews.md)
# Te muestro a mi gato

![cat](./cat.jpeg)

# Hola, Mayx
## Sígueme en [Mabbs](https://github.com/Mabbs)
[Mi blog](https://mabbs.github.io/)

# ÚLTIMA HORA: ¡Deepseek V4.5 Flash Preview acaba de salir!
![deepseeek](./1786763623934.jpg)

# [<img width="460" height="460" alt="image" src="https://github.com/user-attachments/assets/fca57543-7fa4-4e96-bf0b-e6e432dc8fcc" />](https://k.asxz.one)

~~Esto es un tronco que rueda~~

# ÚLTIMA HORA: ¡Deepsuck R2 Flash Preview acaba de salir!
![deepseeek](./1786763623934.jpg)

# [<img width="460" height="460" alt="image" src="https://github.com/user-attachments/assets/fca57543-7fa4-4e96-bf0b-e6e432dc8fcc" />](https://k.asxz.one)

~~Esto también es un tronco que rueda~~

# Enlaces amigos

Esto es un monitor en línea
[![Estación de monitorización de enlaces amigos de Break-This-Repo](https://badge.uptimerobot.com/psp/366a82ee505ef5dbc9cd27f9268436ec.svg?style=logo&theme=light)](https://stats.uptimerobot.com/10qNc6EUwG?utm_source=status_badge&utm_medium=referral)

Pon aquí tu blog / página personal, así, cuando este sitio se haga famoso, todos estos enlaces serán indexados por ~~google~~ los motores de búsqueda y ganarán autoridad. ¡Hagámonos todos grandes y fuertes juntos!

Trae tu contribución
https://blog.sitrmoo.com

https://cuwo4.github.io/

https://onion108.github.io/

https://mochiaochen.github.io/

>Nota del webmaster de alhsk.top: ¿de verdad soy el único raro que usa Cloudflare Pages? ~ Una respuesta: yo uso Vercel

https://alhsk.top 

> Los webmasters de 0w0.red/ne0w0r1d.top/tux.red dicen: aquí llega uno todavía más raro, con EdgeOne

https://0w0.red

https://ftz.is-a.dev/

> El webmaster de ftz.is-a.dev dice: ¿has visto alguna vez tres dominios gratis y dos dominios incluidos con SaaS desplegados en netlify, vercel y cfpages respectivamente?

¿Quieres usar Linux? ¿Por qué no abrir https://tux.red o https://tux.ne0w0r1d.top ?

Me apunto a la fiesta (qué largo https://lililbot.fentropy.dpdns.org

> Abajo está la web de un pobre que no puede permitirse un nombre de dominio (en realidad, la de arriba tampoco)

- [El misterioso sitito de MorningMC](https://morningmc.qzz.io)

- [CarryRao](https://carryrao.top/)

> Parece que soy el único raro que usa un servidor, miau; lo edité desde el móvil, así que puede que no esté muy bien formado, miau

https://kernel.org/

> ¡Abre el enlace, usemos un Mac!
> ¿Cómo, dices que esto no es MacOS?

https://gavin-blog.pages.dev/


> ¡No tengáis miedo, yo también estoy en cf pages!

https://ricky-zhang.com

> Introduce el texto

https://imjerrychu.com/
>¿Has visto alguna vez una web sin contenido? -JerryC

https://Enchantment-Niko.github.io/
> [Enchantment-Niko](https://github.com/Enchantment-Niko) pasó por aquí
> Voy a dejar una marca igualmente:
> ![OneShot](./OneShotWME壁纸/navigate.png "Niko en un barco")

https://caiyan12.github.io/

> Gracias al hermano mayor por la contribución gratuita

https://jiwo.l.cd

> Jiwo | una madriguera graciosilla

https://airoj.cn

> zhiyuHD
https://zhiyuhub.top

> AirOJ | un sistema Online Judge abierto, armonioso (?), abstracto, patata y con tirones
> Gracias al hermano mayor KrisTHL181 por las 6 contribuciones gratuitas

> [!important]
> Prueba también Minecraft y Terraria

> [!important]
> Si eres dueño de un servidor de Minecraft, prueba también
> [Minecraft Daemon Reforged](https://github.com/MCDReforged/MCDReforged)
MCDR tiene razón !!!

https://aria7.wiki

> Ciallo～(∠・ω< )⌒★ Pasé por aquí; y claro, puedes entrar a echar un vistazo ovo

# Debian --un sistema operativo de propósito general
[![Debian Logo](https://www.debian.org/Pics/openlogo-50.png)](https://www.debian.org/)
## Debian es software libre.
Debian está compuesta por software libre y de código abierto, y siempre seguirá siendo 100 % libre. Cualquiera puede usarla, modificarla y distribuirla libremente. Es nuestro compromiso principal con nuestros usuarios. Además es gratuita.
## Debian es estable y segura.
Debian es un sistema operativo basado en Linux que se usa en todo tipo de dispositivos, desde portátiles hasta ordenadores de sobremesa y servidores. Ofrecemos configuraciones predeterminadas razonables para cada paquete y actualizaciones de seguridad periódicas durante todo el ciclo de vida del paquete.
## Debian tiene un amplio soporte de hardware.
La mayor parte del hardware ya está soportado por el núcleo de Linux. Eso significa que Debian también lo soporta. Si hace falta, también se pueden usar controladores de hardware propietarios.
## Debian ofrece un instalador flexible.
Quien quiera probar Debian antes de instalarlo puede usar nuestro Live CD. También incluye el instalador Calamares, lo que hace muy fácil instalar Debian desde un sistema live. Los usuarios más experimentados pueden usar el instalador de Debian, que ofrece más opciones para ajustar con detalle, incluida la posibilidad de usar herramientas de instalación automática por red.
## Debian ofrece actualizaciones sin sobresaltos.
Mantener el sistema operativo al día es muy fácil, tanto si quieres actualizar a una versión completamente nueva como si solo quieres actualizar un paquete suelto.
## Debian es la base de muchas otras distribuciones.
Muchas distribuciones de Linux muy populares, como Ubuntu, Knoppix, PureOS y Tails, se basan en Debian. Proporcionamos todas las herramientas necesarias para que cualquiera pueda fabricar sus propios paquetes cuando lo necesite, para complementar los que no están en el archivo de Debian.
## El proyecto Debian es una comunidad.
Cualquiera puede formar parte de la comunidad Debian; no hace falta ser desarrollador ni administrador de sistemas. Debian tiene una estructura de gobierno democrática. Como todos los miembros del proyecto Debian tienen los mismos derechos, Debian no puede ser controlada por una sola empresa. Nuestros desarrolladores vienen de más de 60 países/regiones, y Debian en sí ya se ha traducido a más de 80 idiomas.

## Plantilla de PR
Esta plantilla de PR ya no puede llamarse plantilla; debería llamarse «Solicitud de contención de anomalías de Break-This-Repo».

Vosotros habéis tomado un repositorio que solo «fusiona automáticamente PR sin conflictos» y habéis jugado con él hasta que el mantenedor ha empezado a escribir:

Tipo: patada al README / patada a la documentación / fallo de código de ciudad vacía / incidente causado por un gato / fenómeno sobrenatural
Verificación: no he tocado .github/, no he tocado el README protegido, sin virus, sin información personal
Declaración: admito que lo rompí, pero la razón me la inventé, y además no es obligatoria

Básicamente esto es: «puedes montar jaleo, pero no montes jaleo de verdad».

¿Contra qué protege esta plantilla?

En realidad traza la línea roja con mucha claridad:

· No tocar .github/: evita que alguien haga saltar por los aires el propio flujo de fusión automática, o que meta una puerta trasera en la CI.
· No tocar las partes protegidas del README: la fachada sigue haciendo falta, no se puede convertir la portada en algo raro.
· Sin credenciales, virus ni información personal: contra ataques a la cadena de suministro, contra el doxeo, contra la maldad de verdad.
· Explicar cómo observarlo: puedes hacer el numerito, pero hay que dejar claro cómo verlo.
· Declarar «breaking change conseguido»: una exención de responsabilidad autoparódica, equivalente a «lo hice, pero no soy responsable».

En cuanto a la retahíla de «fenómeno sobrenatural»:

tres letras + tres flechas alrededor de un círculo + una fundación con contorno
un mapamundi sobre fondo de pentagrama + un anillo de cultivos alrededor + una alianza internacional de cinco palabras

La primera es la Fundación SCP; la segunda es probablemente una organización internacional del estilo de la FAO / Organización de las Naciones Unidas para la Alimentación y la Agricultura. Traducido, viene a decir:
«Esto ya no es un problema de código; recomendamos informar a una organización de contención de anomalías».

¿Cómo puedes encajar tu commit en esta plantilla?

Subes el código fuente de Minecraft, OpenJDK y Fabric Loader, farmeas más de 12,7 millones de líneas en 4 commits; en el tipo puedes marcar:

☑ patada a la documentación
☑ fallo de código de ciudad vacía (cosplay de Xu Jiayin)
☑ Git en multiplataforma
☐ incidente causado por un gato
☐ fenómeno sobrenatural

Marcas todas las verificaciones, copias la declaración y escribes la razón:

Razón: inventada, no obligatoria, pero 12 770 942 líneas de código bien merecen un título.

Cómo observarlo:

Abre OpenJDK_25.0.3, mira el historial de commits y luego siente el silencio del tamaño del repositorio.

Pero un recordatorio igualmente

Este tipo de repositorio es un parque de juegos, no una zona sin ley. Subir el código fuente completo de OpenJDK o el de Minecraft, aunque solo dé lugar a una «fusión automática sin conflictos», trae consigo:

· una explosión del tamaño del repositorio, y GitHub puede limitarlo o avisarte;
· problemas de derechos de autor / licencia: no todo el código fuente se puede meter en cualquier sitio;
· si alguien usa este repositorio como dependencia, es un desastre de cadena de suministro.

Así que la conclusión es:
esta plantilla de PR es el punto de equilibrio que encontró el mantenedor entre «destrucción abierta» y «evitar una explosión de verdad».
Podéis seguir jugando, pero mejor tratadlo como arte de performance, no como repositorio de código. La Fundación SCP ya ha recibido el informe.
(Este texto huele muchísimo a IA — opinión de HQ123-BOOP)

# aceleración de archivos de github 
[https://githubcf.https114514191810lp.edu.eu.org/]

# La auténtica aceleración de archivos de github 
[https://gh-proxy.com/]

# Dato curioso
Pulsa «.» para entrar en la versión web de Microsoft Lucha de Código (VS Code)


## Archivo arqueológico de infraestructura in situ

![EGIEM-R1, el prototipo real: foto in situ](./Emergency-Generator-Interface-Elevation-Module/assets/rock-field-photo.png)

Este repositorio alberga ahora una pieza de infraestructura in situ de bajo consumo, alta fiabilidad y totalmente desconectada: una piedra reclutada temporalmente en un momento crítico. No tiene CPU, ni tarjeta de red, ni intención de dimitir; solo con su propio peso mantiene firme la caja de interfaz en la posición adecuada.

La etiqueta amarilla es lo que hace pasar de «recogí una piedra» a «entró en el registro de equipos». Tras una evaluación preliminar, este dispositivo no necesita inicio de sesión, ni actualizaciones, ni reinicios; la única operación de mantenimiento conocida es: no lo toques.

Dependencia aguas arriba: caja de interfaz del generador del operador  
Dependencia aguas abajo: la Tierra  
Estado de funcionamiento: funcionando de forma estable

La foto es la imagen original in situ proporcionada por el colaborador; solo se normalizó el nombre del archivo, sin recortar ni redibujar.

> **Si funciona, no muevas la piedra.**
