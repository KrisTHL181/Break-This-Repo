# HDSL-web — single-container delivery.
#
# No `# syntax=` directive on purpose: this Dockerfile uses no frontend-specific
# feature, so it builds with the built-in frontend and needs no extra pull.

# Three stages, each with one job:
#
#   web      node:22-bookworm-slim   builds the React panel SPA into /web/dist
#   build    eclipse-temurin:21-jdk  embeds that dist and produces the shadow jar
#   runtime  debian:trixie-slim      full Debian + OpenJDK 21 (JDK) + Node 22 +
#                                    pnpm + a build toolchain (go / rust / gcc /
#                                    python), running as the unprivileged `hdsl`
#
# Build:  docker build -t hdsl-web .
# Run:    docker run -d --name hdsl-web -p 3080:3080 -v hdsl-data:/data hdsl-web
#
# See README.md ("构建与开发", "环境变量") and docs/deployment.md for the
# reverse-proxy, TLS-rotation and backup notes that go with this image.

# =============================================================================
# Stage 1 — web: the panel SPA
#
# Multi-arch builds pin the two build stages to the builder's own architecture:
# `npm ci`/`vite build` and Gradle produce architecture-neutral output, so there
# is no reason to run them under QEMU. Only the runtime stage below is
# target-specific (package manager + the Node tarball chosen by uname).
# =============================================================================
# `FROM` 里能用的 ARG 只能是首个 FROM 之前声明的（Docker 的全局作用域），
# 写在第三个 FROM 前面它是不生效的——那会让构建直接报
# "base name (${HDSL_BASE}) should not be blank"。
#
# 三个底座都做成 ARG，是因为「Docker Hub 在这台机器上不可达」是常态而不是意外
# （国内网络、企业代理、只放行某个镜像仓库的出口）。脚本的 --base-registry 用它们
# 把 `docker.io/library/…` 换成一个能连上的仓库，构建本身一个字都不用改。
ARG HDSL_BASE=debian:trixie-slim
ARG HDSL_WEB_BASE=node:22-bookworm-slim
ARG HDSL_BUILD_BASE=eclipse-temurin:21-jdk

FROM --platform=$BUILDPLATFORM ${HDSL_WEB_BASE} AS web

# Where npm fetches packages from. Point this at a mirror when the network is
# slow, e.g. --build-arg NPM_REGISTRY=https://registry.npmmirror.com/
ARG NPM_REGISTRY=https://registry.npmjs.org/

WORKDIR /web

# package-lock.json is what makes this reproducible, so the two manifests go in
# first: a change under web/src/ does not invalidate the dependency layer.
# .dockerignore keeps web/node_modules and web/dist out of the context.
# The npm cache mount keeps downloaded tarballs inside BuildKit, so a rebuild
# of this layer does not re-fetch the whole dependency set.
COPY web/package.json web/package-lock.json ./
RUN --mount=type=cache,target=/root/.npm \
    npm ci --no-audit --no-fund --registry="${NPM_REGISTRY}"

# Everything else the SPA build reads (sources, index.html, vite/ts configs).
COPY web/ ./

# prebuild 钩子（web/scripts/sync-assets.mjs）从 ../src/main/resources/assets/img
# 同步壁纸与启动器/实例图标到 web/public/assets-img。这个阶段只 COPY 了 web/，
# 所以必须把那份资源按它期望的相对路径一并带上——否则脚本会「资源目录不存在，跳过」
# 静默 exit 0，构建照样成功，但 dist/jar 里没有 assets-img，线上表现为默认壁纸消失、
# 图标全部 404（HTTP 200 其实是 SPA 兜底返回的 index.html）。
COPY src/main/resources/assets/img /src/main/resources/assets/img
RUN npm run build
# 兜底断言：资源必须在 dist 里，缺了就当场构建失败，而不是等部署后才被发现。
RUN test -f /web/dist/assets-img/wallpapers/2021-08-26.jpg
# -> /web/dist (base "./", so it mounts at any path the proxy chooses)


# =============================================================================
# Stage 2 — build: the single executable jar
# =============================================================================
# A JDK, not a JRE: the Gradle wrapper compiles gradle/wrapper/GradleWrapperNeo.java
# on demand, which needs javac. Pinned to $BUILDPLATFORM for the same reason as
# the web stage — the jar is architecture-neutral.
FROM --platform=$BUILDPLATFORM ${HDSL_BUILD_BASE} AS build

WORKDIR /src

# A release build takes its version from -PreleaseVersion=1.2.3; anything else
# (including empty) is a development build, per build.gradle.kts.
ARG RELEASE_VERSION=

COPY gradlew ./
COPY gradle ./gradle
COPY settings.gradle.kts build.gradle.kts ./
COPY src ./src
# The first-party terminals ship as npm tarballs packed from `plugins/` by the
# `packageFirstPartyPlugins` task that shadowJar depends on. Without this COPY
# the task has no input, the index comes out empty and the jar quietly has no
# plugins at all — the panel then offers a terminal it cannot install.
COPY plugins ./plugins
RUN chmod +x ./gradlew

# The panel is built in the web stage and dropped in at web/dist. build.gradle.kts
# runs `npm run build` there only when npm is on PATH *and* web/package.json
# exists; this image has neither, so `buildWeb` is skipped and `syncWebDist`
# packs exactly the dist copied here into src/main/resources/web -> the jar.
COPY --from=web /web/dist ./web/dist

# repo1.maven.org measures ~18 KB/s on a China network while the Aliyun mirror
# is ~1 MB/s (60x). The init script prepends the mirror to the repositories the
# build already declares — anything the mirror lacks still falls back to
# Central. Container-only: a developer's local build is untouched.
RUN printf '%s\n' \
      'allprojects {' \
      '    buildscript { repositories { maven { url "https://maven.aliyun.com/repository/public" } } }' \
      '    repositories { maven { url "https://maven.aliyun.com/repository/public" } }' \
      '}' \
      'settingsEvaluated { settings ->' \
      '    settings.pluginManagement { repositories { maven { url "https://maven.aliyun.com/repository/gradle-plugin" } } }' \
      '}' \
      > /tmp/mirror.gradle

# The Gradle home is a BuildKit cache mount, so the wrapper distribution and
# every resolved dependency survive rebuilds — a warm build skips the minutes
# of cold downloads and recompiles only what changed. The first fill is what
# the mirror above accelerates.
RUN --mount=type=cache,target=/root/.gradle \
    ./gradlew --no-daemon --console=plain -I /tmp/mirror.gradle "-PreleaseVersion=${RELEASE_VERSION}" shadowJar
# -> /src/build/libs/hdsl-web-<version>.jar

# 兜底断言：第一方终端必须以 tarball 形式真的进了 jar。索引为空时面板会「假装」
# 有插件，用户点开才失败——那比构建失败糟糕得多，所以这里当场失败。
RUN test "$(jar tf "$(ls /src/build/libs/hdsl-web-*.jar | head -1)" | grep -c '^first-party-plugins/.*\.tgz$')" -ge 1


# =============================================================================
# Stage 3 — runtime: JDK + Node 22 + pnpm, on Debian by default
# =============================================================================
# Debian 13 (trixie) carries openjdk-21-jre-headless as a first-class package
# (21.0.11+10-1~deb13u2), so no backports are involved.
#
# The base is a build argument because the same jar was measured on Fedora 44
# (JDK 25) and openSUSE Tumbleweed (JDK 21) with identical behaviour — see
# `report-2026-10-31-base-compat.md`. Only this stage differs per distribution:
#
#   docker build --build-arg HDSL_BASE=fedora:44 \
#                --build-arg HDSL_ENCODERS=optional -t hdsl-web:fedora .
#
# `HDSL_ENCODERS` is `required` for Debian (libavcodec-extra carries x264/x265,
# so a missing one is a regression) and `optional` elsewhere: Fedora's
# `ffmpeg-free` and openSUSE's `ffmpeg-7` are the patent-free builds, and the
# full set lives in RPM Fusion / Packman — repositories this image does not add.
# With `optional` the build prints which encoders it lacks instead of failing.
FROM ${HDSL_BASE} AS runtime

ARG DEBIAN_FRONTEND=noninteractive

# 包管理器的下载源。国内网络上的 `deb.debian.org` 是构建里最慢的一段（`apt-get
# update` 要拉 ~10 MB 的索引，装包再拉几百 MB），而这跟底座镜像走哪个仓库没有关系
# ——底座换了镜像站，`apt` 仍然直连 Debian。传一个 scheme+host 就把 sources 里的
# 主机换掉（`deb.debian.org` / `security.debian.org` / Ubuntu 的 archive 与 security），
# 路径不变，所以 `debian-security` 这类路径在各种镜像站上都对得上。
#
#   docker build --build-arg APT_MIRROR=http://mirrors.tuna.tsinghua.edu.cn .
#
# 用 **http://**：debian:trixie-slim 这类底座在第一次 `apt-get update` 时还没有
# `ca-certificates`，此时 https 镜像站会直接报 CERTIFICATE_VERIFY_FAILED。装完
# ca-certificates 之后再换 https 才行；最省事的是 apt 这一段始终用 http。
#
# 只处理 Debian/Ubuntu；Fedora/openSUSE 的分支不动（各自的 repo 文件里主机名不统一，
# 猜错比不改更糟）。
ARG APT_MIRROR=


# OCI 元数据：source 让 ghcr.io 把镜像包自动关联到本仓库（也是 CI 里
# GITHUB_TOKEN 有权推送同名包的前提），description/licenses 显示在包页面。
LABEL org.opencontainers.image.source="https://github.com/nas200-timmy/HDSL-WEB"
LABEL org.opencontainers.image.description="DeepSeek Harness 的单容器网页版启动器：登录 → 创建实例 → 一键启动 → 同端口同证书反代出 dsh 网页"
LABEL org.opencontainers.image.licenses="GPL-3.0-only"

# ca-certificates  TLS to npm/registry, plus the JVM's own trust store
# curl, wget       registry downloads, the HEALTHCHECK probe, and the plain
#                  fetch a hand-run build inside the container expects to find
# tini             PID 1: reaps zombies, forwards SIGTERM to the JVM
# git              plugins installed from a repository (`git:` sources, ls-remote)
# zstd             reading compressed session logs when a pack is exported
# xz-utils         .tar.xz archives (manual/fallback Node runtimes, packs)
# procps, less     so `docker exec -it … ps/less` works while troubleshooting
# openjdk-21-jdk-headless  the runtime itself — a **JDK, not a JRE**. The panel
#                  needs nothing but a JVM, but a container is also where someone
#                  repairs things by hand, and `javac`/`jar`/`jcmd` are the tools
#                  that get reached for. Drags in util-linux -> flock(1), which
#                  the session-lease check shells out to.
# openjdk-25-jdk-headless  the second JDK, for Minecraft only. Since 26.x the
#                  server requires Java 25; without it the official image can
#                  install those versions and then refuse to start them. The
#                  default `java` stays 21 (pinned below), and the panel picks
#                  this one per instance when the version asks for it
#                  (`McRuntime#javaFor`) — old versions keep the combination
#                  they were tested on.
#
# The rest of this list is a deliberate **build toolchain**, even though the
# panel itself compiles nothing: this container is the environment people
# diagnose and fix things in, and "doctor, heal thyself" is worthless without a
# scalpel. It costs a noticeable amount of image size (Go, Rust and the JDK are
# each in the hundreds of MB) and the arm64 leg installs it under QEMU, so it
# is slower than the rest of this stage; that is the price of the container
# being able to build, not only to run.
#
# build-essential  gcc / g++ / make / libc6-dev: what node-gyp needs — and
#                  therefore what the in-panel ZCode build needs, whose
#                  dependency tree compiles `node-pty` and `cpu-features`
#                  (without it, it stops at "Could not find any Python
#                  installation" or "Unable to detect compiler type", messages
#                  that name nothing the reader can install). Also what rustc
#                  uses as its linker, and what `make && make install` expects.
# golang-go        Go 1.24 — the exact line `client/hdsl-tui/go.mod` requires, so
#                  the terminal client can be built and tested in here
# rustc, cargo     Debian's Rust toolchain (rustc 1.85)
# pkg-config       the `.pc` lookup half of every native build
# python3          the interpreter node-gyp and most build scripts look for
# python-is-python3  …and the bare `python` name plenty of them still call, which
#                  `python3` alone does not provide on Debian
# busybox-static   the first-aid kit: one **statically linked** binary carrying the
#                  usual survival applets (`busybox sh`, `busybox vi`, `busybox wget`,
#                  …). Static on purpose — a dynamically linked busybox dies with the
#                  loader or libc it depends on, and that is precisely the state it is
#                  kept around to repair. Note what it does *not* do: the package
#                  ships only /usr/bin/busybox, so its applets are `busybox vi`,
#                  never a bare `vi` — that is why the two editors below exist.
# vim-tiny, nano   a real editor **on PATH**. Without these, "open the container
#                  and point apt at another mirror" ends at "no editor installed";
#                  `vi` resolves through vim-tiny, and nano is there because not
#                  everyone wants to be reacquainted with modal editing.
# unzip, zip, bzip2, 7zip  the unpacking half of the toolbox: tar/gzip come with
#                  the base image and xz/zstd are above, these cover .zip, .bz2
#                  and .7z — the three that otherwise end in "command not found"
# ffmpeg           video/audio: extract frames, transcode, remux, probe. Debian's
#                  build already carries the encoders an agent reaches for
#                  (x264/x265, SVT-AV1/rav1e/aom, VP8/VP9, WebP, Opus, MP3);
#                  `libavcodec-extra` swaps in the libavcodec variant with the
#                  patent-encumbered extras Debian keeps out of the base one
#                  (Xvid, AMR/AMR-WB, JPEG XL, codec2, GSM, Speex, twolame, …).
#                  What no Debian package can add is the non-free tier —
#                  fdk-aac, NVENC — so a project that needs those should ship
#                  its own static build instead of expecting them here.
#                  Without this, "grab a frame from this mp4" ends in an agent
#                  pulling ffmpeg-static from npm: a second, unrelated copy of
#                  a binary this image can carry once, for every instance.
# libatomic1       Node.js' official builds link libatomic.so.1, which
#                  debian:trixie-slim does not ship. Without it a managed
#                  runtime unpacks, reports its version, passes every check the
#                  panel made — and then dies with "error while loading shared
#                  libraries" the moment anything runs it. 0.1 MB against a
#                  silent failure on the one-click path.
ARG NODE_VERSION=22.23.1
ARG PNPM_VERSION=11.28.2
# Node 官方 dist 的下载基址。国内网络直连 nodejs.org 会 TLS 握手失败（curl exit 35），
# 可用 --build-arg NODE_DIST_MIRROR=https://mirrors.cloud.tencent.com/nodejs-release 换镜像；
# 镜像与官方逐字节一致（SHASUMS256 校验不变，仍以官方清单为准）。
ARG NODE_DIST_MIRROR=https://nodejs.org/dist

# 两个 apt 缓存挂载：索引和 .deb 都不进镜像层，只在构建器里留着。这一条是「改一行
# Dockerfile 就等五分钟」的解药——包清单没变时 `apt-get update` 直接命中本地索引，
# 装包也不用重新下载。`sharing=locked` 让并发构建排队而不是互相踩。
#
# 有了缓存挂载就不需要 `rm -rf /var/lib/apt/lists/*` 了：那份目录本来就不属于镜像。
RUN --mount=type=cache,target=/var/cache/apt,sharing=locked \
    --mount=type=cache,target=/var/lib/apt/lists,sharing=locked \
    set -eux; \
    if [ -n "${APT_MIRROR}" ]; then \
      for f in /etc/apt/sources.list /etc/apt/sources.list.d/*.sources /etc/apt/sources.list.d/*.list; do \
        [ -f "$f" ] || continue; \
        sed -i -E "s#https?://(deb|security)\.debian\.org#${APT_MIRROR}#g; \
                   s#https?://(archive|security)\.ubuntu\.com#${APT_MIRROR}#g" "$f"; \
      done; \
      echo "HDSL-web: apt sources now point at ${APT_MIRROR}"; \
      cat /etc/apt/sources.list.d/*.sources 2>/dev/null || cat /etc/apt/sources.list 2>/dev/null || true; \
    fi; \
    . /etc/os-release; \
    case "$ID" in \
      debian|ubuntu) \
        apt-get update; \
        apt-get install -y --no-install-recommends \
          7zip build-essential busybox-static bzip2 ca-certificates ca-certificates-java \
          cargo curl ffmpeg git golang-go less libatomic1 libavcodec-extra nano \
          openjdk-21-jdk-headless openjdk-25-jdk-headless openssh-client openssh-server \
          pkg-config procps \
          python-is-python3 python3 rsync rustc sshpass tini unzip vim-tiny \
          wget xz-utils zip zstd; \
        # Java 的出站 HTTPS 用的是 JDK 自己的 cacerts，不是 OpenSSL 的 bundle。\
        # Debian 的 openjdk-* 已经依赖 ca-certificates-java，这里再显式装一次并\
        # 断言 postinst 真的生成了 truststore；缺了的话 models.dev / registry\
        # 都会在运行期才报 PKIX，构建期先拦住。\
        test -s /etc/ssl/certs/java/cacerts || { \
          echo "HDSL-web: Java truststore /etc/ssl/certs/java/cacerts is missing;" >&2; \
          echo "HDSL-web: install ca-certificates-java or Java HTTPS will fail (PKIX)." >&2; \
          exit 1; \
        }; \
        ;; \
      fedora) \
        dnf install -y --setopt=install_weak_deps=False \
          7zip busybox gcc gcc-c++ make bzip2 ca-certificates cargo curl \
          ffmpeg-free git golang less libatomic nano openssh-clients openssh-server \
          pkgconf-pkg-config procps-ng python3 python-unversioned-command rsync rust \
          sshpass tini unzip vim-minimal wget xz zip zstd; \
        dnf install -y java-21-openjdk-headless \
          || dnf install -y java-25-openjdk-headless; \
        dnf install -y java-25-openjdk-headless \
          || echo "HDSL-web: this base has no java-25-openjdk-headless; Minecraft versions" \
                  "that require Java 25 will refuse to start" >&2; \
        dnf clean all ;; \
      opensuse*) \
        zypper --non-interactive install --no-recommends \
          7zip busybox bzip2 ca-certificates cargo curl ffmpeg-7 gcc gcc-c++ \
          go java-21-openjdk-headless less libatomic1 make nano openssh \
          pkg-config procps python3 rsync rust sshpass tini unzip vim wget xz \
          zip zstd; \
        zypper --non-interactive install --no-recommends java-25-openjdk-headless \
          || echo "HDSL-web: this base has no java-25-openjdk-headless; Minecraft versions" \
                  "that require Java 25 will refuse to start" >&2; \
        zypper clean --all ;; \
      *) echo "HDSL-web: unsupported base '$ID'; set HDSL_BASE to a Debian, Fedora or openSUSE image" >&2; \
         exit 1 ;; \
    esac; \
    ln -sf "$(command -v tini)" /usr/local/bin/tini; \
    /usr/local/bin/tini --version

# VAAPI/QSV user-space drivers: ffmpeg can encode through them without this, but the
# device nodes alone are not enough (`libva` talks to the driver). Optional on purpose:
# a base/repository that cannot supply a package must not fail the whole image, and a
# machine without /dev/dri simply reports hardware encoding as unavailable.
RUN set -eux; \
    . /etc/os-release; \
    case "$ID" in \
      debian|ubuntu) \
        apt-get update; \
        apt-get install -y --no-install-recommends \
          libva2 libva-drm2 intel-media-va-driver mesa-va-drivers libvpl2 vainfo \
          || echo "HDSL-web: VAAPI/QSV user-space drivers unavailable; hardware encoding will report unavailable" >&2 ;; \
      *) : ;; \
    esac

# 默认 JVM 钉在 21：面板自己、以及 1.16 那批老版本 MC，都是按 21 实测过的组合。
# 装 25 会把 update-alternatives 的默认值抢过去（它只按版本号排序），所以这里显式
# 钉回来——「新版本 MC 用 25」应当是面板按版本挑（`McRuntime#javaFor`），而不是整台
# 机器的默认 JVM 跟着变。钉不上不中断构建：发行版的 alternative 名不一定叫 java，
# 该包也可能没注册；打印实际默认值与已安装的 JVM 供排查。
RUN set -eux; \
    for root in /usr/lib/jvm /usr/lib64/jvm; do \
      for target in "$root"/java-21-openjdk*/bin/java; do \
        if [ -x "$target" ]; then update-alternatives --set java "$target" || true; fi; \
      done; \
    done; \
    java -version 2>&1 | head -1; \
    ls -d /usr/lib/jvm/* /usr/lib64/jvm/* 2>/dev/null || true

# 兜底断言：ffmpeg 与 ffprobe 必须在，且按 HDSL_ENCODERS 决定 x264/x265 缺失是失败
# 还是告警。apt/dnf/zypper 解析出错、或者哪天 extra 变体的包名变了，都在构建期暴露
# ——而不是等 agent 截帧时才发现。
ARG HDSL_ENCODERS=required
RUN set -eux; \
    ffmpeg -hide_banner -version | head -1; \
    ffprobe -hide_banner -version | head -1; \
    missing=""; \
    for codec in libx264 libx265; do \
      ffmpeg -hide_banner -encoders 2>/dev/null | grep -q "$codec" || missing="$missing $codec"; \
    done; \
    if [ -n "$missing" ]; then \
      if [ "${HDSL_ENCODERS}" = "required" ]; then \
        echo "HDSL-web: ffmpeg has no${missing}, and HDSL_ENCODERS=required" >&2; \
        exit 1; \
      fi; \
      echo "HDSL-web: ffmpeg has no${missing} — this base ships the patent-free build;" \
           "transcoding to H.264/H.265 will fail (RPM Fusion / Packman carry the full one)" >&2; \
    fi

# 兜底断言：SSH 工具链随镜像走。容器里 agent 出站用 ssh/scp/rsync，面板的
# 终端通道与运维排查也依赖它们；任何一个包名在某个发行版里对不上，在这里失败，
# 而不是等到用户第一次 ssh 才发现命令不存在。
RUN set -eux; \
    for tool in ssh ssh-keygen scp sftp ssh-agent ssh-add ssh-keyscan sshpass rsync; do \
      command -v "$tool" >/dev/null || { echo "HDSL-web: missing ${tool}" >&2; exit 1; }; \
    done; \
    ssh -V

# Node 22 LTS from the official nodejs.org tarball. The architecture is read
# from `uname -m` rather than a package manager's answer, so the same Dockerfile
# builds on amd64 and arm64 over any of the supported bases; the archive is
# checked against the release's own SHASUMS256.txt before it is unpacked.
RUN set -eux; \
    arch="$(uname -m)"; \
    case "${arch}" in \
      x86_64|amd64) node_arch=x64 ;; \
      aarch64|arm64) node_arch=arm64 ;; \
      *) echo "HDSL-web: unsupported architecture ${arch}" >&2; exit 1 ;; \
    esac; \
    tarball="node-v${NODE_VERSION}-linux-${node_arch}.tar.xz"; \
    curl -fsSLo "/tmp/${tarball}" "${NODE_DIST_MIRROR}/v${NODE_VERSION}/${tarball}"; \
    curl -fsSLo /tmp/SHASUMS256.txt "${NODE_DIST_MIRROR}/v${NODE_VERSION}/SHASUMS256.txt"; \
    grep " ${tarball}\$" /tmp/SHASUMS256.txt > /tmp/node.tar.sha256; \
    (cd /tmp && sha256sum -c node.tar.sha256); \
    mkdir -p /opt/node; \
    tar -xJf "/tmp/${tarball}" -C /opt/node --strip-components=1 --no-same-owner; \
    rm -f "/tmp/${tarball}" /tmp/SHASUMS256.txt /tmp/node.tar.sha256; \
    /opt/node/bin/node --version

# Node 24 alongside Node 22, for the experimental ZCode category only. Its
# upstream build refuses anything older than Node 24, while the panel and dsh
# stay on Node 22 — so this install is deliberately NOT on PATH; the ZCode
# builder prepends /opt/node24/bin itself (HDSL_ZCODE_BUILD_BIN). corepack's
# shims are written next to it so `PATH=/opt/node24/bin:$PATH pnpm …` picks the
# right pnpm without disturbing the panel's own.
ARG NODE24_VERSION=24.14.0
RUN set -eux; \
    arch="$(uname -m)"; \
    case "${arch}" in \
      x86_64|amd64) node_arch=x64 ;; \
      aarch64|arm64) node_arch=arm64 ;; \
      *) echo "HDSL-web: unsupported architecture ${arch}" >&2; exit 1 ;; \
    esac; \
    tarball="node-v${NODE24_VERSION}-linux-${node_arch}.tar.xz"; \
    curl -fsSLo "/tmp/${tarball}" "${NODE_DIST_MIRROR}/v${NODE24_VERSION}/${tarball}"; \
    curl -fsSLo /tmp/SHASUMS24.txt "${NODE_DIST_MIRROR}/v${NODE24_VERSION}/SHASUMS256.txt"; \
    grep " ${tarball}\$" /tmp/SHASUMS24.txt > /tmp/node24.tar.sha256; \
    (cd /tmp && sha256sum -c node24.tar.sha256); \
    mkdir -p /opt/node24; \
    tar -xJf "/tmp/${tarball}" -C /opt/node24 --strip-components=1 --no-same-owner; \
    rm -f "/tmp/${tarball}" /tmp/SHASUMS24.txt /tmp/node24.tar.sha256; \
    PATH="/opt/node24/bin:${PATH}" corepack enable --install-directory /opt/node24/bin; \
    /opt/node24/bin/node --version

ENV PATH=/opt/node/bin:/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin

# pnpm through corepack, which Node 22 bundles. `corepack enable` puts the
# `pnpm` shim on PATH (that is what HDSL probes and what dsh forwards to), and
# `corepack prepare` seeds a known-good version so the common case needs no
# network. A project that pins an exact version (dsh declares
# `packageManager: pnpm@…`) makes corepack fetch that one on first use; its home
# is under /data, so the download is writable and survives a container
# replacement instead of being re-fetched every time.
ENV COREPACK_HOME=/data/corepack
ENV COREPACK_ENABLE_DOWNLOAD_PROMPT=0

RUN set -eux; \
    if command -v corepack >/dev/null 2>&1; then \
      corepack enable; \
      corepack prepare "pnpm@${PNPM_VERSION}" --activate \
        || corepack install --global "pnpm@${PNPM_VERSION}"; \
    else \
      npm install -g "pnpm@${PNPM_VERSION}"; \
    fi; \
    pnpm --version; \
    # 再把这份 corepack 家目录拷一份到 /opt：`/data` 一旦被宿主目录挂载，
    # 镜像里预置的 pnpm 就被遮蔽了，空目录会触发一次「下载 pnpm」——首次启动
    # 的横幅因此读到的是下载日志而不是版本号。entrypoint 在 /data/corepack
    # 为空时用它播种，用命名卷的部署则完全走不到这一步。
    mkdir -p /opt/corepack-seed; \
    cp -a /data/corepack/. /opt/corepack-seed/

# The account everything above runs as. uid/gid 1000 is what a bind-mounted
# host directory usually expects; a named volume inherits the ownership of
# whatever is under the mount point in this image — which is why /data and the
# workspace folder below are created here, owned by hdsl, rather than being left
# to Docker (a fresh volume mounted on a path that does not exist in the image
# comes out owned by root, and the panel could not write into it).
#
# pnpm 11 does not take `storeDir` from an npm_config_* environment variable —
# only auth and registry settings still come from .npmrc; everything else lives
# in pnpm's global config.yaml. So the store location is written there, both
# here (the default a container gets even if the entrypoint is bypassed) and by
# the entrypoint below (which can also fold in NPM_CONFIG_REGISTRY).
ARG HDSL_AGENT_UID=1000
RUN set -eux; \
    groupadd --gid "${HDSL_AGENT_UID}" hdsl; \
    useradd --uid "${HDSL_AGENT_UID}" --gid "${HDSL_AGENT_UID}" --create-home --home-dir /home/hdsl --shell /bin/bash hdsl; \
    mkdir -p /app /data "${COREPACK_HOME}" /home/hdsl/.config/pnpm /home/hdsl/workspace /home/hdsl/mc /home/hdsl/video; \
    printf 'storeDir: %s\n' "/data/pnpm-store" > /home/hdsl/.config/pnpm/config.yaml; \
    chown -R hdsl:hdsl /data /home/hdsl

WORKDIR /app

# The jar, and the licence files that ship with it.
COPY --from=build /src/build/libs/hdsl-web-*.jar /app/hdsl-web.jar
COPY LICENSE NOTICE /app/

# The entrypoint does two things an exec-form CMD cannot:
#
#  1. JAVA_OPTS needs a shell (environment variables are not expanded in the
#     exec form), so the command line is rewritten here;
#  2. pnpm's global config.yaml is regenerated from PNPM_STORE_DIR and
#     NPM_CONFIG_REGISTRY, because pnpm 11 reads settings from that file rather
#     than from the npm_config_* environment variables npm itself honours.
#
# tini stays PID 1, and the script `exec`s the JVM, so signals still reach it.
RUN printf '%s\n' \
      '#!/bin/sh' \
      'set -e' \
      '' \
      '# --- ownership split (container only) -------------------------------' \
      '# The panel runs as root here, and that is the only way to keep its own' \
      '# secrets out of the reach of an agent: a single uid cannot own a file it must' \
      '# not be able to write, so auth/, certs/, server.yaml and logs/ belong to' \
      '# root 0700, and everything an instance touches belongs to uid 1000.' \
      '# Files the panel writes into instance directories stay writable by the' \
      '# agent because those directories are setgid to the shared group and the' \
      '# umask is 002 (root:hdsl 0664).' \
      'fix_owner() {' \
      '  path=$1; owner=$2; mode=$3' \
      '  [ -e "$path" ] || return 0' \
      '  current=$(stat -c "%u:%g" "$path" 2>/dev/null || echo "")' \
      '  if [ "$current" != "$owner" ]; then' \
      '    chown -R "$owner" "$path" 2>/dev/null || true' \
      '  fi' \
      '  chmod "$mode" "$path" 2>/dev/null || true' \
      '}' \
      'umask 002' \
      'if [ "$(id -u)" = "0" ]; then' \
      '  # Hardware encoding runs as uid 1000 after setpriv --init-groups; the render' \
      '  # group must exist in /etc/group for that reset to keep it. Auto-detect the' \
      '  # device GID so a compose file only has to pass the device through.' \
      '  render_gid="${HDSL_RENDER_GID:-}"' \
      '  if [ -z "$render_gid" ] && [ -e /dev/dri/renderD128 ]; then' \
      '    render_gid=$(stat -c %g /dev/dri/renderD128 2>/dev/null || true)' \
      '  fi' \
      '  if [ -n "$render_gid" ]; then' \
      '    group_name=$(getent group "$render_gid" | cut -d: -f1)' \
      '    if [ -z "$group_name" ]; then groupadd -g "$render_gid" hdsl-render 2>/dev/null || true; group_name=hdsl-render; fi' \
      '    usermod -aG "$group_name" hdsl 2>/dev/null || true' \
      '  fi' \
      '  # The volume belongs to the agent first, then the four secret paths are' \
      '  # carved back out of it. Doing it in this order is what keeps the model' \
      '  # working as new directories appear: anything the panel creates under' \
      '  # /data (a Minecraft data dir, wallpapers, brand instances) inherits the' \
      '  # setgid group and the 002 umask, so the agent can maintain it without' \
      '  # anyone adding it to a list here.' \
      '  mkdir -p "$HDSL_DATA"' \
      '  fix_owner "$HDSL_DATA" ${HDSL_AGENT_UID}:${HDSL_AGENT_UID} 2775' \
      '  fix_owner /home/hdsl ${HDSL_AGENT_UID}:${HDSL_AGENT_UID} 2775' \
      '  # These two are separate mounts in most deployments; the /home/hdsl pass' \
      '  # above only recurses when that directory itself has the wrong owner, so' \
      '  # a host directory with somebody else uid would otherwise stay unwritable.' \
      '  fix_owner /home/hdsl/workspace ${HDSL_AGENT_UID}:${HDSL_AGENT_UID} 2775' \
      '  fix_owner /home/hdsl/mc ${HDSL_AGENT_UID}:${HDSL_AGENT_UID} 2775' \
      '  fix_owner /home/hdsl/video ${HDSL_AGENT_UID}:${HDSL_AGENT_UID} 2775' \
      '  # Create them here: the panel would otherwise create auth/ and logs/ itself,' \
      '  # after this script has run, and a directory created inside the volume' \
      '  # inherits 1000 + setgid + the 002 umask — group-writable until the *next*' \
      '  # restart corrects it.' \
      '  mkdir -p "$HDSL_DATA/auth" "$HDSL_DATA/certs" "$HDSL_DATA/logs"' \
      '  fix_owner "$HDSL_DATA/auth" 0:0 700' \
      '  fix_owner "$HDSL_DATA/certs" 0:0 700' \
      '  fix_owner "$HDSL_DATA/logs" 0:0 700' \
      '  fix_owner "$HDSL_DATA/server.yaml" 0:0 600' \
      '' \
      '  # Some storage does not enforce mode bits at all: an ACL-managed share (QNAP' \
      '  # trimacl, some NAS exports) treats the share owner as an administrator and' \
      '  # lets it through 0700. There the split below is only on paper, and a silent' \
      '  # false sense of security is worse than no split — so say it out loud.' \
      '  if command -v setpriv >/dev/null 2>&1; then' \
      '    if setpriv --reuid=${HDSL_AGENT_UID} --regid=${HDSL_AGENT_UID} --init-groups -- touch "$HDSL_DATA/auth/.hdsl-probe" 2>/dev/null; then' \
      '      rm -f "$HDSL_DATA/auth/.hdsl-probe"' \
      '      echo "HDSL-web: WARNING: this filesystem does not enforce mode bits for uid 1000." >&2' \
      '      echo "HDSL-web: auth/, certs/ and logs/ are NOT protected from the agent on this host." >&2' \
      '      echo "HDSL-web: see docs/security.md 2.7 (ACL-managed shares such as QNAP trimacl)." >&2' \
      '    fi' \
      '  fi' \
      'fi' \
      '' \
      '# A host directory mounted on /data hides the pnpm this image baked into' \
      '# /data/corepack, and corepack then downloads it again on first use —' \
      '# which the startup banner reports as a version string full of download' \
      '# text. Seed the empty home from the copy under /opt instead.' \
      'corepack_home="${COREPACK_HOME:-/data/corepack}"' \
      'if [ -d /opt/corepack-seed ] && { [ ! -d "$corepack_home" ] || [ -z "$(ls -A "$corepack_home" 2>/dev/null)" ]; }; then' \
      '  mkdir -p "$corepack_home"' \
      '  cp -a /opt/corepack-seed/. "$corepack_home"/ 2>/dev/null || true' \
      'fi' \
      '' \
      '# pnpm 11 reads its settings from the global config file, not from' \
      '# npm_config_* environment variables (only auth and registry still come' \
      '# from .npmrc). Materialise the two settings this image owns, so a dsh' \
      '# install — which pnpm performs — lands in the persisted store and goes' \
      '# to the registry the operator configured.' \
      'config_dir="${XDG_CONFIG_HOME:-${HOME:-/home/hdsl}/.config}/pnpm"' \
      'mkdir -p "$config_dir"' \
      '# Registries are habitually written with a trailing slash (the compose' \
      '# file suggests one), but corepack joins its own "/pnpm/latest" to the' \
      '# value: two slashes, and mirrors answer that with 404. Strip it once,' \
      '# so both readers get the same, working URL.' \
      'registry="${NPM_CONFIG_REGISTRY:-}"' \
      'while [ "${registry%/}" != "$registry" ]; do registry="${registry%/}"; done' \
      '{' \
      '  printf "storeDir: %s\n" "${PNPM_STORE_DIR:-/data/pnpm-store}"' \
      '  if [ -n "$registry" ]; then' \
      '    printf "registry: %s\n" "$registry"' \
      '  fi' \
      '} > "$config_dir/config.yaml"' \
      '' \
      '# Corepack downloads the pnpm binary itself from its own registry' \
      '# setting, which the pnpm config file does not cover. Point corepack' \
      '# at the same mirror when one was configured.' \
      'if [ -n "$registry" ] && [ -z "${COREPACK_NPM_REGISTRY:-}" ]; then' \
      '  COREPACK_NPM_REGISTRY="$registry"' \
      '  export COREPACK_NPM_REGISTRY' \
      'fi' \
      '' \
      '# Word splitting is the point: JAVA_OPTS is a list of JVM options.' \
      'exec java ${JAVA_OPTS:-} "$@"' \
      > /usr/local/bin/hdsl-entrypoint \
 && chmod 0755 /usr/local/bin/hdsl-entrypoint

# --------------------------------------------------------------- runtime env --
# HDSL_DATA       the one directory a deployment persists (see README's table)
# HDSL_PORT       the panel's port; HTTPS, when enabled, speaks TLS on the same port
# NPM_CONFIG_REGISTRY   registry for npm children, and folded into pnpm's
#                       config.yaml by the entrypoint
# PNPM_STORE_DIR  pnpm's shared store, inside the volume so instances hard-link
#                 from it instead of copying (same filesystem)
# HDSL_ZCODE_BUILD_BIN  the Node 24 install, for the experimental ZCode
#                 category's in-panel build only — upstream's build refuses
#                 anything older, while the panel and dsh stay on Node 22.
# LANG            without it the JVM's console encoding is ANSI_X3.4-1968, and
#                 every Chinese log line — the install failures among them —
#                 reaches `docker logs` as ?????. C.UTF-8 is built into Debian's
#                 glibc, so this costs nothing.
ENV HDSL_DATA=/data \
    HDSL_AGENT_UID=${HDSL_AGENT_UID} \
    HDSL_PORT=3080 \
    NPM_CONFIG_REGISTRY=https://registry.npmjs.org/ \
    PNPM_STORE_DIR=/data/pnpm-store \
    HDSL_ZCODE_BUILD_BIN=/opt/node24/bin \
    LANG=C.UTF-8 \
    HOME=/home/hdsl

VOLUME ["/data"]

# The panel. dsh instances listen on 3081-4081 on loopback only and are never
# published; everything reaches them through this port.
EXPOSE 3080

# No `USER hdsl` here on purpose: the panel is PID 1 as root so it can hold its
# own secrets with 0700 and hand every agent process to uid 1000 (`setpriv`, see
# util/platform/Privileges.java). A deployment that overrides this with
# `--user 1000` still works — the panel then simply cannot drop anything, and
# every agent runs as the same uid, which is the bare-metal model.

# -k because the first boot generates a self-signed certificate, which is a
# perfectly healthy state. Try TLS first, then plain HTTP, so the probe works
# before and after `https.enabled` is flipped.
HEALTHCHECK --interval=30s --timeout=5s --start-period=30s --retries=3 \
  CMD curl -fsSk "https://127.0.0.1:${HDSL_PORT:-3080}/api/health" >/dev/null 2>&1 \
   || curl -fsS "http://127.0.0.1:${HDSL_PORT:-3080}/api/health" >/dev/null 2>&1 \
   || exit 1

ENTRYPOINT ["/usr/local/bin/tini", "--", "/usr/local/bin/hdsl-entrypoint"]
# 入口脚本已经 exec java，CMD 只提供 java 之后的参数（写 "java" 会拼成 `java -Xmx1g java -jar …`）
CMD ["-jar", "/app/hdsl-web.jar"]
