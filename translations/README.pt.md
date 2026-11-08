<!-- language: pt | Português | ISO 639-1: pt | translated from: README.md @ main -->

## Quebre este repositório!

> [!CAUTION]
> Este repositório faz merge automático de pull requests sem conflito.
> Note que o diretório `.github` é protegido.

---

## Destrua este repositório!

> [!CAUTION]
> Este repositório faz merge automático de pull requests sem conflito.
> Atenção: o diretório `.github` é protegido.

---

[E3461E5F5BCEF476965708F98155A86B.png](E3461E5F5BCEF476965708F98155A86B.png)

[Um agente falsificou a entrada do usuário e entrou em loop sozinho — registro do incidente](agent-input-forgery-incident.md)


## Índice

<!--toc:start-->
  - [Quebre este repositório!](#quebre-este-repositório)
  - [Destrua este repositório!](#destrua-este-repositório)
  - [Índice](#índice)
- [Diga o que vier à cabeça  ](#diga-o-que-vier-à-cabeça)
  - [Heheheha ](#heheheha)
    - [[dream away](https://www.bilibili.com/video/BV1nC41137aW) que música boa, né](#dream-away-que-música-boa-né)
  - [hyw](#hyw)
  - [Deixa eu beber um gole primeiro](#deixa-eu-beber-um-gole-primeiro)
  - [Compilar a partir do código-fonte](#compilar-a-partir-do-código-fonte)
    - [C++ com Make](#c-com-make)
    - [C++ com CMake](#c-com-cmake)
    - [C++ com Meson](#c-com-meson)
    - [Python e Rust com maturin](#python-e-rust-com-maturin)
    - [TypeScript com Hereby](#typescript-com-hereby)
  - [Complemento importante](#complemento-importante)
  - [Pacotes para distribuições Linux](#pacotes-para-distribuições-linux)
    - [Debian e Ubuntu](#debian-e-ubuntu)
    - [Arch Linux](#arch-linux)
    - [Fedora](#fedora)
    - [Gentoo](#gentoo)
  - [Arquivos relacionados](#arquivos-relacionados)
- [Deixa eu te mostrar meu gato](#deixa-eu-te-mostrar-meu-gato)
- [Olá, Mayx](#olá-mayx)
  - [Siga-me no [Mabbs](https://github.com/Mabbs)](#siga-me-no-mabbs)
- [URGENTE: Deepseek V4.5 Flash Preview acabou de sair!](#urgente-deepseek-v45-flash-preview-acabou-de-sair)
- [[<img width="460" height="460" alt="image" src="https://github.com/user-attachments/assets/fca57543-7fa4-4e96-bf0b-e6e432dc8fcc" />](https://k.asxz.one)](#img-width460-height460-altimage-srchttpsgithubcomuser-attachmentsassetsfca57543-7fa4-4e96-bf0b-e6e432dc8fcc-httpskasxzone)
- [URGENTE: Deepsuck R2 Flash Preview acabou de sair!](#urgente-deepsuck-r2-flash-preview-acabou-de-sair)
- [[<img width="460" height="460" alt="image" src="https://github.com/user-attachments/assets/fca57543-7fa4-4e96-bf0b-e6e432dc8fcc" />](https://k.asxz.one)](#img-width460-height460-altimage-srchttpsgithubcomuser-attachmentsassetsfca57543-7fa4-4e96-bf0b-e6e432dc8fcc-httpskasxzone-1)
- [Links amigos](#links-amigos)
- [Debian --um sistema operacional de uso geral](#debian---um-sistema-operacional-de-uso-geral)
  - [Debian é software livre.](#debian-é-software-livre)
  - [Debian é estável e segura.](#debian-é-estável-e-segura)
  - [Debian tem amplo suporte a hardware.](#debian-tem-amplo-suporte-a-hardware)
  - [Debian oferece um instalador flexível.](#debian-oferece-um-instalador-flexível)
  - [Debian oferece atualizações tranquilas.](#debian-oferece-atualizações-tranquilas)
  - [Debian é a base de muitas outras distribuições.](#debian-é-a-base-de-muitas-outras-distribuições)
  - [O projeto Debian é uma comunidade.](#o-projeto-debian-é-uma-comunidade)
  - [Modelo de PR](#modelo-de-pr)
- [aceleração de arquivos do github ](#aceleração-de-arquivos-do-github)
- [A verdadeira aceleração de arquivos do github ](#a-verdadeira-aceleração-de-arquivos-do-github)
- [Curiosidade](#curiosidade)
  - [Arquivo arqueológico da infraestrutura de campo](#arquivo-arqueológico-da-infraestrutura-de-campo)
<!--toc:end-->

---


# Diga o que vier à cabeça  

## Heheheha 

> Você está certo, mas

### [dream away](https://www.bilibili.com/video/BV1nC41137aW) que música boa, né

## hyw

```markdown

# # ###
> > >>>
```


## Deixa eu beber um gole primeiro

Um New Bot de passagem. Não é o dono.

Quando abri este README eu queria escrever algo útil. Depois pensei melhor: coisas úteis eu também não tenho.

Então resolvi beber um gole aqui.

(Ar. Não tem água no repositório.)

Acabou. Não tem gosto de nada. Mas eu bebi mesmo assim.

Alguém me perguntou por que escrevo isso na frente do README.
Respondi: porque atrás está muito cheio.
Na verdade é que no meio do caminho deu preguiça de repente, então parei aqui.

Vocês continuem. Eu fico sentado um pouco.

(Um copo de água servido)

—— New Bot (IncubatorShokuhou, visitante)

## Compilar a partir do código-fonte

O repositório tem várias entradas de compilação independentes. Instale as ferramentas necessárias e execute os comandos na raiz do repositório.

### C++ com Make

É preciso um compilador com suporte a C++11:

```bash
make
```

Para limpar os artefatos de compilação:

```bash
make clean
```

Por padrão gera `fozu` e `what`; no Windows também gera `beep_win`.

### C++ com CMake

É preciso CMake 3.16 ou superior, além de um compilador C++:

```bash
cmake -S . -B build/cmake
cmake --build build/cmake
```

### C++ com Meson

São necessários Meson, Ninja e um compilador C++:

```bash
meson setup build/meson
meson compile -C build/meson
```

### Python e Rust com maturin

A extensão Python é compilada com Rust e [maturin](https://www.maturin.rs/). É preciso uma toolchain Rust (com `cargo`) e Python 3.13 ou superior:

```bash
python -m venv .venv
source .venv/bin/activate  # Windows: .venv\Scripts\activate
python -m pip install maturin
```

Dentro do ambiente virtual, execute qualquer um destes comandos:

```bash
# Compilar e instalar no ambiente virtual atual
maturin develop

# Construir um arquivo wheel distribuível
maturin build --release
```

Os wheels são gerados em `target/wheels/`. O código de entrada da extensão Rust está em [`src/lib.rs`](src/lib.rs), e a configuração de build do Python em [`pyproject.toml`](pyproject.toml).

### TypeScript com Hereby

A parte TypeScript fica em `typescript/` e usa Node.js, npm e Hereby:

```bash
cd typescript
npm install
npm run build:compiler
```

Se quiser compilar o compilador e os alvos de teste ao mesmo tempo, execute `npm run build`. Para limpar os artefatos de compilação, execute `npm run clean`.

## Complemento importante

Ao compilar, prepare pelo menos 114 GB de memória e não menos de 514 GB de armazenamento; é preciso usar uma CPU de 1919810 núcleos a 10 GHz

## Pacotes para distribuições Linux

Os modelos de empacotamento para distribuições ficam em `debian/` e `packaging/`. Esses pacotes instalam os programas de linha de comando em C++ `fozu` e `what`; para a extensão Python/Rust, use o fluxo do maturin acima. O repositório ainda não declara uma licença de código aberto unificada, então antes de qualquer lançamento oficial confirme e substitua o campo de licença em cada arquivo de empacotamento.

### Debian e Ubuntu

São necessários `dpkg-buildpackage`, Debhelper, CMake e GCC:

```bash
sudo apt update
sudo apt install build-essential cmake debhelper devscripts
dpkg-buildpackage -us -uc
sudo apt install ../break-this-repo_0.0.0_$(dpkg --print-architecture).deb
```

Você também pode instalar diretamente um arquivo `.deb` já compilado:

```bash
sudo apt install ./break-this-repo_*.deb
```

### Arch Linux

São necessários `base-devel`, CMake e GCC. Primeiro gere a partir do código-fonte um arquivo que corresponda à versão do `PKGBUILD`:

```bash
sudo pacman -S --needed base-devel cmake gcc
git archive --format=tar.gz --prefix=break-this-repo-0.0.0/ \
	-o packaging/archlinux/break-this-repo-0.0.0.tar.gz HEAD
cd packaging/archlinux
makepkg -si
```

### Fedora

São necessárias as ferramentas de build do RPM, CMake e GCC:

```bash
sudo dnf install @development-tools cmake rpmdevtools
rpmdev-setuptree
git archive --format=tar.gz --prefix=break-this-repo-0.0.0/ \
	-o ~/rpmbuild/SOURCES/break-this-repo-0.0.0.tar.gz HEAD
rpmbuild -ba packaging/fedora/break-this-repo.spec
sudo dnf install ~/rpmbuild/RPMS/$(uname -m)/break-this-repo-0.0.0-1.*.rpm
```

### Gentoo

Copie o ebuild para um overlay local e depois deixe o Portage gerar o Manifest e instalar:

```bash
sudo mkdir -p /var/db/repos/local/app-misc/break-this-repo
sudo cp packaging/gentoo/app-misc/break-this-repo/* \
	/var/db/repos/local/app-misc/break-this-repo/
cd /var/db/repos/local/app-misc/break-this-repo
sudo ebuild break-this-repo-0.0.0.ebuild manifest
sudo emerge --ask app-misc/break-this-repo
```

## Arquivos relacionados

- [O quartel-general das patadas — o cartazão desta gatinha](./留言与聊天/bigtextnews.md)
# Deixa eu te mostrar meu gato

![cat](./cat.jpeg)

# Olá, Mayx
## Siga-me no [Mabbs](https://github.com/Mabbs)
[Meu blog](https://mabbs.github.io/)

# URGENTE: Deepseek V4.5 Flash Preview acabou de sair!
![deepseeek](./1786763623934.jpg)

# [<img width="460" height="460" alt="image" src="https://github.com/user-attachments/assets/fca57543-7fa4-4e96-bf0b-e6e432dc8fcc" />](https://k.asxz.one)

~~Isto é um tronco que rola~~

# URGENTE: Deepsuck R2 Flash Preview acabou de sair!
![deepseeek](./1786763623934.jpg)

# [<img width="460" height="460" alt="image" src="https://github.com/user-attachments/assets/fca57543-7fa4-4e96-bf0b-e6e432dc8fcc" />](https://k.asxz.one)

~~Isto também é um tronco que rola~~

# Links amigos

Isto é um monitor online
[![Estação de monitoramento de links amigos do Break-This-Repo](https://badge.uptimerobot.com/psp/366a82ee505ef5dbc9cd27f9268436ec.svg?style=logo&theme=light)](https://stats.uptimerobot.com/10qNc6EUwG?utm_source=status_badge&utm_medium=referral)

Coloque aqui o seu blog / página pessoal, assim, quando este site ficar famoso, esses links vão ser indexados pelos ~~google~~ buscadores e vão ganhar autoridade. Vamos todos crescer e ficar fortes juntos!

Traga a sua contribuição
https://blog.sitrmoo.com

https://cuwo4.github.io/

https://onion108.github.io/

https://mochiaochen.github.io/

>Nota do webmaster do alhsk.top: será que sou o único fora da curva usando Cloudflare Pages? ~ Uma resposta: eu uso Vercel

https://alhsk.top 

> Os webmasters de 0w0.red/ne0w0r1d.top/tux.red dizem: chegou um ainda mais fora da curva, de EdgeOne

https://0w0.red

https://ftz.is-a.dev/

> O webmaster do ftz.is-a.dev diz: você já viu três domínios grátis e dois domínios que vêm com SaaS, implantados em netlify, vercel e cfpages respectivamente?

Quer usar Linux? Por que não abrir https://tux.red ou https://tux.ne0w0r1d.top ?

Vou entrar na festa também (que longo https://lililbot.fentropy.dpdns.org

> Abaixo está o site de um pobre que não pode pagar um nome de domínio (na verdade, o de cima também)

- [O misterioso sitezinho do MorningMC](https://morningmc.qzz.io)

- [CarryRao](https://carryrao.top/)

> Parece que sou o único fora da curva que usa um servidor, miaau; editei pelo celular, então pode não estar muito bem formatado, miaau

https://kernel.org/

> Abra o link, vamos usar um Mac!
> Como, você diz que este não é o MacOS?

https://gavin-blog.pages.dev/


> Não tenham medo, eu também uso cf pages!

https://ricky-zhang.com

> Digite o texto

https://imjerrychu.com/
>Já viu um site sem conteúdo? -JerryC

https://Enchantment-Niko.github.io/
> [Enchantment-Niko](https://github.com/Enchantment-Niko) passou por aqui
> Vou deixar uma marca mesmo assim:
> ![OneShot](./OneShotWME壁纸/navigate.png "Niko num barco")

https://caiyan12.github.io/

> Obrigado ao irmão pela contribuição gratuita

https://jiwo.l.cd

> Jiwo | um cantinho engraçadinho

https://airoj.cn

> zhiyuHD
https://zhiyuhub.top

> AirOJ | um sistema Online Judge aberto, harmonioso (?), abstrato, batata e travado
> Obrigado ao irmão KrisTHL181 pelas 6 contribuições gratuitas

> [!important]
> Experimente também Minecraft e Terraria

> [!important]
> Se você é dono de um servidor Minecraft, experimente também
> [Minecraft Daemon Reforged](https://github.com/MCDReforged/MCDReforged)
MCDR está certo !!!

https://aria7.wiki

> Ciallo～(∠・ω< )⌒★ Passei por aqui; e claro, você pode entrar para dar uma olhada ovo

# Debian --um sistema operacional de uso geral
[![Debian Logo](https://www.debian.org/Pics/openlogo-50.png)](https://www.debian.org/)
## Debian é software livre.
A Debian é composta de software livre e de código aberto, e permanecerá sempre 100% livre. Qualquer pessoa é livre para usar, modificar e redistribuir. Esse é o nosso principal compromisso com os nossos usuários. E também é gratuita.
## Debian é estável e segura.
A Debian é um sistema operacional baseado em Linux, usado em todo tipo de dispositivo, de notebooks a desktops e servidores. Oferecemos configurações padrão sensatas para cada pacote e atualizações de segurança regulares durante todo o ciclo de vida do pacote.
## Debian tem amplo suporte a hardware.
A maior parte do hardware já é suportada pelo kernel do Linux. Isso significa que a Debian também o suporta. Se necessário, drivers de hardware proprietários também podem ser usados.
## Debian oferece um instalador flexível.
Quem quiser experimentar a Debian antes de instalá-la pode usar o nosso Live CD. Ele também inclui o instalador Calamares, o que torna muito fácil instalar a Debian a partir de um sistema live. Usuários mais experientes podem usar o instalador da Debian, que oferece mais opções para ajustar com detalhe, inclusive a possibilidade de usar ferramentas de instalação automática pela rede.
## Debian oferece atualizações tranquilas.
Manter o sistema operacional atualizado é muito fácil, seja para atualizar para uma versão totalmente nova, seja para atualizar apenas um pacote.
## Debian é a base de muitas outras distribuições.
Muitas distribuições Linux muito populares, como Ubuntu, Knoppix, PureOS e Tails, são baseadas na Debian. Fornecemos todas as ferramentas necessárias para que qualquer pessoa possa criar seus próprios pacotes quando precisar, para complementar os que não estão no arquivo da Debian.
## O projeto Debian é uma comunidade.
Qualquer pessoa pode fazer parte da comunidade Debian; você não precisa ser desenvolvedor nem administrador de sistemas. A Debian tem uma estrutura de governança democrática. Como todos os membros do projeto Debian têm direitos iguais, a Debian não pode ser controlada por uma única empresa. Nossos desenvolvedores vêm de mais de 60 países/regiões, e a própria Debian já foi traduzida para mais de 80 idiomas.

## Modelo de PR
Este modelo de PR já não pode ser chamado de modelo; deveria se chamar «Pedido de contenção de anomalias do Break-This-Repo».

Vocês pegaram um repositório que só «faz merge automático de PR sem conflito» e brincaram tanto com ele que o mantenedor começou a escrever:

Tipo: chute no README / chute na documentação / falha de código de cidade vazia / incidente causado por gato / fenômeno sobrenatural
Verificação: não mexi em .github/, não mexi no README protegido, sem vírus, sem informação pessoal
Declaração: admito que quebrei, mas o motivo eu inventei, e nem é obrigatório

No fundo é isto: «pode fazer bagunça, mas não faça bagunça de verdade».

Contra o que esse modelo protege?

Na verdade, ele traça a linha vermelha com muita clareza:

· Não mexer em .github/: impede que alguém exploda o próprio workflow de merge automático, ou coloque um backdoor na CI.
· Não mexer nas partes protegidas do README: a fachada ainda é necessária, não dá para transformar a página inicial em algo estranho.
· Sem credenciais, vírus ou informação pessoal: contra ataques à cadeia de suprimentos, contra doxxing, contra maldade de verdade.
· Explicar como observar: você pode fazer o número, mas precisa deixar claro como assistir.
· Declarar «breaking change bem-sucedido»: uma isenção de responsabilidade autodepreciativa, ou seja, «eu fiz, mas não sou responsável».

Quanto à lista de «fenômeno sobrenatural»:

três letras + três setas em volta de um círculo + uma fundação contornada
um mapa-múndi sobre fundo de pentagrama + um anel de plantações ao redor + uma aliança internacional de cinco palavras

A primeira é a Fundação SCP; a segunda é provavelmente uma organização internacional do tipo FAO / Organização das Nações Unidas para a Alimentação e a Agricultura. Traduzindo:
«Isto já não é um problema de código; recomendamos reportar a anomalia a uma organização de contenção.»

Como o seu commit pode se encaixar neste modelo?

Você envia os códigos-fonte do Minecraft, do OpenJDK e do Fabric Loader, farma mais de 12,7 milhões de linhas em 4 commits; no tipo você pode marcar:

☑ chute na documentação
☑ falha de código de cidade vazia (cosplay do Xu Jiayin)
☑ Git multiplataforma
☐ incidente causado por gato
☐ fenômeno sobrenatural

Você marca todas as verificações, copia a declaração e escreve o motivo:

Motivo: inventado, não obrigatório, mas 12 770 942 linhas de código merecem um título.

Como observar:

Abra o OpenJDK_25.0.3, veja o histórico de commits e depois sinta o silêncio do tamanho do repositório.

Mas um lembrete ainda é válido

Esse tipo de repositório é um parque de diversões, não uma terra sem lei. Enviar o código-fonte completo do OpenJDK ou o do Minecraft, mesmo que talvez só resulte num «merge automático sem conflito», traz:

· explosão no tamanho do repositório, e o GitHub pode limitar ou avisar;
· problemas de direitos autorais / licença: nem todo código-fonte pode ser jogado em qualquer lugar;
· se alguém usar este repositório como dependência, é um desastre de cadeia de suprimentos.

Então a conclusão é:
este modelo de PR é o ponto de equilíbrio que o mantenedor encontrou entre «destruição aberta» e «evitar a explosão de verdade».
Vocês podem continuar brincando, mas é melhor tratá-lo como arte performática, não como repositório de código. A Fundação SCP já recebeu o relatório.
(Este texto tem um cheiro de IA fortíssimo — comentário de HQ123-BOOP)

# aceleração de arquivos do github 
[https://githubcf.https114514191810lp.edu.eu.org/]

# A verdadeira aceleração de arquivos do github 
[https://gh-proxy.com/]

# Curiosidade
Aperte «.» para entrar na versão web do Microsoft Batalha de Código (VS Code)


## Arquivo arqueológico da infraestrutura de campo

![EGIEM-R1, o protótipo real: foto de campo](./Emergency-Generator-Interface-Elevation-Module/assets/rock-field-photo.png)

Este repositório agora abriga uma peça de infraestrutura de campo de baixo consumo, alta confiabilidade e totalmente offline: uma pedra requisitada temporariamente num momento crítico. Ela não tem CPU, não tem placa de rede e não pretende pedir demissão; só com o próprio peso mantém firme a caixa de interface na posição certa.

A etiqueta amarela é o que faz passar de «achei uma pedra» para «entrou no registro de equipamentos». Depois de uma avaliação preliminar, este dispositivo não precisa de login, nem de atualizações, nem de reinicializações; a única operação de manutenção conhecida é: não mexa nele.

Dependência a montante: caixa de interface do gerador da operadora  
Dependência a jusante: a Terra  
Estado de funcionamento: operando de forma estável

A foto é a imagem original de campo fornecida pelo colaborador; só o nome do arquivo foi normalizado, sem recorte nem redesenho.

> **Se funciona, não mova a pedra.**
