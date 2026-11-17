#!/usr/bin/env bash
#
# HDSL-web 本地构建/启动向导（非交付物：不进 Release，也不参与 CI）。
#
#   git clone … && cd HDSL-web && ./docker-install.sh
#
# 问四件事——数据放哪、工作区挂不挂、端口、要不要国内镜像源——然后在**当前仓库**
# 里 docker build，再 docker run 起容器，等健康检查通过后打印地址。
#
# 为什么用 docker run 而不是 compose：NAS 上常常只有 docker、没有 compose 插件，
# 而这条路径用不到 compose 的任何特性。docker-compose.yml 仍然有效，两条路互不干扰。
#
# 同名容器已存在时不会擅自覆盖：先问「重启 / 重建容器 / 重新构建镜像」还是退出。
#
# 非交互（宿主 agent、批量部署用）：
#   ./docker-install.sh --yes --data /srv/hdsl-data --workspace /srv/dsh-ws --port 3080
#   ./docker-install.sh --yes --mc-ports 25565-25575   # 顺带把 MC 端口映射到宿主
#   ./docker-install.sh --yes --restart     # 只重启现有容器，不碰镜像
#   ./docker-install.sh --yes --recreate    # 删容器重建（用现有镜像，不重新编译）
#   ./docker-install.sh --yes --rebuild     # 重新构建镜像后重建容器
#   ./docker-install.sh --dry-run           # 只打印会执行什么
#   ./docker-install.sh --doctor            # 只体检：依赖、docker、buildx、端口、磁盘
#
# 连 docker 都没装（绝对冷启动）：脚本会说明装法；加 --install-docker 则由它代装
# （需要 root 或免密 sudo）。缺 buildx 时给的是**按发行版区分的完整命令**——Ubuntu
# 的 docker.io 包常常不带它，这正是「照着提示做完仍然构建不了」的来源。
#
set -euo pipefail

# 原始参数：加完 docker 组后要用 `sg docker -c` 原样重跑本脚本（见 fix_docker_access），
# 那时 $@ 已经被下面的解析循环吃光了。
ORIG_ARGS=("$@")

REPO_DIR=$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)
STATE_FILE="$REPO_DIR/.docker-install.state"
ENV_FILE="$REPO_DIR/.docker-install.env"

DEFAULT_NAME="hdsl-web"
DEFAULT_PORT="3080"
DEFAULT_BIND="0.0.0.0"
DEFAULT_JAVA_OPTS="-Xmx1g"
NPM_MIRROR="https://registry.npmmirror.com/"
NODE_MIRROR_CN="https://mirrors.cloud.tencent.com/nodejs-release"
NODE_MIRROR_OFFICIAL="https://nodejs.org/dist"
# 底座镜像默认走哪个仓库。Docker Hub 在国内网络下经常整个连不上（`registry-1.docker.io`
# 直接 connection refused），而底座拉不到就一步都走不下去；DaoCloud 的镜像站实测三个底座
# （node / eclipse-temurin / debian）的多架构 manifest 都在。要回官方用 `--no-mirror`
# 或 `--base-registry docker.io`。
DEFAULT_BASE_REGISTRY="docker.m.daocloud.io"
BASE_REGISTRY_ALT="docker.1ms.run"
# 系统包管理器的下载源（只对 Debian/Ubuntu 底座生效）。底座镜像换了仓库，`apt` 仍然
# 直连 `deb.debian.org`：`apt-get update` 要拉 ~10 MB 索引、装包再拉几百 MB，这是本地
# 构建里最慢的一段，也是「改一行 Dockerfile 等五分钟」的主因之一。
APT_MIRROR_CN="http://mirrors.tuna.tsinghua.edu.cn"

# ── 命令行参数 ────────────────────────────────────────────────────────────────

ASSUME_YES=0
DRY_RUN=0
INSTALL_DOCKER=0
ACTION=""
NO_BUILD=0
RELEASE=0
RELEASE_SET=0        # 命令行显式给了 --release / --no-release 时置 1
MIRROR=""            # yes / no，空 = 交互时问
NAME=""
PORT=""
BIND=""
DATA=""
WORKSPACE=""
WORKSPACE_SET=0      # 交互里明确选了「不挂」时置 1，避免被状态文件里的旧值顶掉
MC=""
MC_SET=0
VIDEO=""
VIDEO_SET=0
HW=""
HW_SET=0
MC_PORTS=""          # 25565 / 25565-25575 / 25565,25566；空 = 不把 MC 端口映射到宿主
MC_PORTS_SET=0       # 命令行或交互里明确给过
DOCTOR=0             # --doctor：只体检，不构建、不起容器、不写状态
PASSWORD=""
PASSWORD_SET=0
IMAGE=""
BASE=""              # debian / fedora / opensuse，空 = 沿用上次或 debian
BASE_REGISTRY=""     # 底座镜像的仓库前缀，空 = Docker Hub（docker.io）
BASE_REGISTRY_SET=0  # 命令行显式给了 --base-registry 时置 1（否则沿用状态文件里的）
APT_MIRROR=""        # apt 下载源（scheme+host），空 = 用发行版默认的 deb.debian.org
APT_MIRROR_SET=0     # 命令行显式给了 --apt-mirror 时置 1
PREBUILT=""          # 非空 = 不构建，直接拉这个已发布的镜像

usage() {
    cat <<'EOF'
用法：docker-install.sh [选项]

问什么（交互，回车取默认）：
  --data <路径|volume:名称>       面板数据（默认 ./hdsl-web-data；volume: 走命名卷）
  --workspace <路径|volume:名称>  工作区挂到 /home/hdsl/workspace（默认不挂）
  --no-workspace                  明确不挂工作区
  --mc <路径|volume:名称|none>    MC 服务器数据挂到 /home/hdsl/mc
                                  （默认 volume:hdsl-mc；none = 不挂，重建容器会丢存档）
  --no-mc                         同 --mc none
  --video <路径|volume:名称|none> 转码媒体库挂到 /home/hdsl/video
                                  （默认 volume:hdsl-video；NAS 建议指到真实视频目录）
  --no-video                      同 --video none
  --hw <auto|vaapi|nvidia|none>   硬件编码直通（默认 auto：有 /dev/dri 挂 VAAPI/QSV，
                                  nvidia-container-toolkit 可用时挂 --gpus all）
  --mc-ports <规格|none>          把 MC 游戏端口映射到宿主：`25565`、`25565-25575`、
                                  `25565,25566`（最多 128 个）。默认 `none`：不对外开服。
                                  交互运行时会在「配置」里问一次，回车 = 不开。
                                  注意容器内端口必须等于该实例的 `server-port`
                                  （面板分配范围见 mc.port_start / mc.port_end）。
  --no-mc-ports                   同 --mc-ports none
  --port <端口>                   面板端口（默认 3080，容器内外同号）
  --bind <地址>                   监听地址（默认 0.0.0.0；只本机用 127.0.0.1）
  --name <容器名>                 默认 hdsl-web
  --password <口令>               首启管理员口令（默认留空 = 网页初始化引导）
  --mirror | --no-mirror          镜像源：国内（npm→npmmirror、Node→腾讯、底座→docker.m.daocloud.io）
                                  或官方（npmjs / nodejs.org / Docker Hub）。
                                  不给这个参数时会弹一个上下键菜单，一次把三者都定下来。
  --base-registry <前缀>          底座镜像从哪个仓库拉，默认 docker.m.daocloud.io。
                                  给了它就只改底座仓库（npm/Node 那半仍按上次的镜像源）；
                                  想回 Docker Hub 用 --base-registry docker.io 或 --no-mirror。
                                  构建本身仍在本地做，只是三个 FROM 换了仓库。
  --apt-mirror <scheme://host>    Debian/Ubuntu 底座的 apt 下载源，例如
                                  http://mirrors.tuna.tsinghua.edu.cn 。
                                  这是本地构建最大的一块（apt-get update 的索引 +
                                  几百 MB 包体），和底座仓库是两回事：底座走镜像站时
                                  apt 仍然直连 deb.debian.org。--mirror 会在国内模式下
                                  自动带上它，--no-mirror 会清掉。
  --no-apt-mirror                 显式用发行版默认源。
  --pull <版本|镜像>              不构建，直接拉已发布的镜像
                                  （ghcr.io/nas200-timmy/hdsl-web:<版本>）。
                                  Docker Hub 完全不可达、ghcr.io 能连时走这条。

动作（同名容器已存在时）：
  --restart     只重启现有容器
  --recreate    删容器重建，用现有镜像（不重新编译）
  --rebuild     重新构建镜像后重建容器
  --no-build    起容器时复用记录里的镜像，不重新构建

其它：
  --release     把最后的版本标签当 RELEASE_VERSION 打进 jar（否则是 0.1.0-dev）
  --no-release  关掉上次记住的 --release
  --image <tag> 指定镜像名（默认 <最后标签>-build-<底座>-<时间戳>）
  --base <底座> 运行时底座：debian（默认）/ fedora / opensuse。
                都是原地构建，镜像里跑的是同一颗 jar；Fedora/openSUSE 用各自的
                包管理器和官方仓库，ffmpeg 只有 patent-free 版本（构建期告警，不失败）。
  --install-docker  没有 docker 时代为安装（需要 root 或免密 sudo）；缺 buildx 时也一并装
  --doctor      只做体检，什么都不改：docker / buildx / 端口 / 磁盘 / 发行版提示
  --dry-run     只打印会执行什么，不构建、不起容器
  --yes         不问，全部用参数或默认值
  -h, --help    本帮助
EOF
    exit "${1:-0}"
}

need_value() {
    [ "$#" -ge 2 ] && [ -n "$2" ] || { echo "参数 $1 需要一个值" >&2; exit 2; }
}

while [ "$#" -gt 0 ]; do
    case "$1" in
        --data) need_value "$@"; DATA="$2"; shift 2 ;;
        --workspace) need_value "$@"; WORKSPACE="$2"; WORKSPACE_SET=1; shift 2 ;;
        --no-workspace) WORKSPACE=""; WORKSPACE_SET=1; shift ;;
        --mc) need_value "$@"; MC="$2"; MC_SET=1; shift 2 ;;
        --no-mc) MC=""; MC_SET=1; shift ;;
        --video) need_value "$@"; VIDEO="$2"; VIDEO_SET=1; shift 2 ;;
        --no-video) VIDEO=""; VIDEO_SET=1; shift ;;
        --hw) need_value "$@"; HW="$2"; HW_SET=1; shift 2 ;;
        --mc-ports) need_value "$@"; MC_PORTS="$2"; MC_PORTS_SET=1; shift 2 ;;
        --no-mc-ports) MC_PORTS=""; MC_PORTS_SET=1; shift ;;
        --port) need_value "$@"; PORT="$2"; shift 2 ;;
        --bind) need_value "$@"; BIND="$2"; shift 2 ;;
        --name) need_value "$@"; NAME="$2"; shift 2 ;;
        --password) need_value "$@"; PASSWORD="$2"; PASSWORD_SET=1; shift 2 ;;
        --mirror) MIRROR=yes; shift ;;
        --no-mirror) MIRROR=no; shift ;;
        --restart) ACTION=restart; shift ;;
        --recreate) ACTION=recreate; shift ;;
        --rebuild) ACTION=rebuild; shift ;;
        --no-build) NO_BUILD=1; shift ;;
        --image) need_value "$@"; IMAGE="$2"; shift 2 ;;
        --base) need_value "$@"; BASE="$2"; shift 2 ;;
        --base-registry) need_value "$@"; BASE_REGISTRY="$2"; BASE_REGISTRY_SET=1; shift 2 ;;
        --apt-mirror) need_value "$@"; APT_MIRROR="${2%/}"; APT_MIRROR_SET=1; shift 2 ;;
        --no-apt-mirror) APT_MIRROR=""; APT_MIRROR_SET=1; shift ;;
        --pull) need_value "$@"; PREBUILT="$2"; shift 2 ;;
        --release) RELEASE=1; RELEASE_SET=1; shift ;;
        --no-release) RELEASE=0; RELEASE_SET=1; shift ;;
        --install-docker) INSTALL_DOCKER=1; shift ;;
        --doctor) DOCTOR=1; shift ;;
        --dry-run) DRY_RUN=1; shift ;;
        --yes|-y) ASSUME_YES=1; shift ;;
        -h|--help) usage 0 ;;
        *) echo "未知参数：$1" >&2; usage 2 ;;
    esac
done

# ── 底座 ─────────────────────────────────────────────────────────────────────
# 同一颗 jar 在三个底座上行为一致（见 docs/deployment.md §4.1 与底座兼容性报告），
# 差别只在包管理器、tini 路径和 ffmpeg 的编码器集合——那些由 Dockerfile 的
# runtime 阶段按 /etc/os-release 分派。
BASE_IMAGE=""
BASE_CODE=""
BASE_ENCODERS="required"
resolve_base() {
    case "$1" in
        debian|"")  BASE_IMAGE="debian:trixie-slim";           BASE_CODE=debian; BASE_ENCODERS=required ;;
        fedora)     BASE_IMAGE="fedora:44";                     BASE_CODE=fedora; BASE_ENCODERS=optional ;;
        opensuse|suse) BASE_IMAGE="opensuse/tumbleweed:latest"; BASE_CODE=suse;   BASE_ENCODERS=optional ;;
        *) die "--base 只认 debian / fedora / opensuse（收到：$1）" ;;
    esac
}

# ── 输出 ──────────────────────────────────────────────────────────────────────

say()  { printf '%s\n' "$*"; }
step() { printf '\n==> %s\n' "$*"; }
warn() { printf '注意：%s\n' "$*" >&2; }
die()  { printf '错误：%s\n' "$*" >&2; exit 1; }

# 交互提问；--yes 时直接取默认值并把选择打印到 stderr（stdout 只留给答案），
# 这样 $(ask …) 拿到的始终是干净的值。
ask() {
    local prompt="$1" default="${2-}" answer=""
    if [ "$ASSUME_YES" = 1 ]; then
        printf '  %s → %s\n' "$prompt" "${default:-（空）}" >&2
        printf '%s' "$default"
        return 0
    fi
    if [ -n "$default" ]; then
        read -r -p "  $prompt [$default]: " answer || true
    else
        read -r -p "  $prompt: " answer || true
    fi
    [ -n "$answer" ] || answer="$default"
    printf '%s' "$answer"
}

ask_password() {
    local answer=""
    if [ "$ASSUME_YES" = 1 ]; then
        printf '%s' ""
        return 0
    fi
    read -r -s -p "  管理员口令（留空 = 打开网页走初始化引导）: " answer || true
    printf '\n' >&2
    printf '%s' "$answer"
}

# 上下键单选。stdout 只输出选中项的序号（从 1 起），标题和列表画到 stderr——
# 和 ask 一样的约定，`$(select_one …)` 拿到的永远是一个数字。
#
# 每一项的写法是「名字｜说明」，屏幕上整体显示，返回序号；说明里不要放换行。
#
# --yes、或 stdin 不是终端（宿主 agent、管道、CI）时直接取 SELECT_DEFAULT，并把选了哪一项
# 打到 stderr：非交互的日志里也要看得出用的是哪一套源。
select_one() {
    local title=$1
    shift
    local -a items=("$@")
    local count=${#items[@]}
    local sel=$(( ${SELECT_DEFAULT:-1} - 1 ))
    if [ "$sel" -lt 0 ] || [ "$sel" -ge "$count" ]; then sel=0; fi

    if [ "$ASSUME_YES" = 1 ] || [ ! -t 0 ]; then
        printf '  %s → %s\n' "$title" "${items[$sel]%%｜*}" >&2
        printf '%s' "$((sel + 1))"
        return 0
    fi

    local i key rest line
    printf '  %s（↑/↓ 选择，回车确认）\n' "$title" >&2
    # 先占住位置：之后每一轮都是「上移 count 行、整块重画」，屏幕不会越滚越长。
    for i in "${!items[@]}"; do printf '\n' >&2; done
    while :; do
        printf '\033[%dA' "$count" >&2
        for i in "${!items[@]}"; do
            if [ "$i" = "$sel" ]; then
                line="  ❯ ${items[$i]}"
            else
                line="    ${items[$i]}"
            fi
            printf '\r\033[K%s\n' "$line" >&2
        done
        IFS= read -rsn1 key || true
        case "$key" in
            '') break ;;                      # 回车
            $'\x03') printf '\n' >&2; exit 130 ;;   # Ctrl-C
            $'\x1b')
                IFS= read -rsn2 rest || true
                case "$rest" in
                    '[A') sel=$(( (sel - 1 + count) % count )) ;;
                    '[B') sel=$(( (sel + 1) % count )) ;;
                esac
                ;;
        esac
    done
    printf '%s' "$((sel + 1))"
}

confirm() {
    local prompt="$1" answer=""
    if [ "$ASSUME_YES" = 1 ]; then
        return 0
    fi
    read -r -p "  $prompt [y/N]: " answer || true
    case "$answer" in
        y|Y|yes|YES) return 0 ;;
        *) return 1 ;;
    esac
}

# ── docker 存在性 / 冷启动 ────────────────────────────────────────────────────

docker_present() { command -v docker >/dev/null 2>&1; }
docker_ready()   { docker info >/dev/null 2>&1; }

# 本地构建要 buildx：Dockerfile 里的 `RUN --mount=type=cache` 是 BuildKit 语法，而
# Docker 23 起 `docker build` 本身就是 buildx——插件不在，命令直接报
# "BuildKit is enabled but the buildx component is missing or broken"。
# Ubuntu 的 docker.io 包并不总是把插件带上（独立包 `docker-buildx`），所以先探一次。
buildx_ready()   { docker buildx version >/dev/null 2>&1; }

show_buildx_hint() {
    say ""
    say "构建需要 buildx，但这台机器上没有（或插件坏了）。"
    say "Dockerfile 用了 BuildKit 的缓存挂载，没有 buildx 就连第一步都过不去。"
    say "Ubuntu 的 docker.io 包经常**不带**它——这就是「照着提示装完还是构建不了」的常见来源。"
    say ""
    say "  1) 按这台机器的包管理器装："
    buildx_install_hint
    say ""
    say "  2) 不想装也行——不构建，直接拉已发布的镜像："
    say "     ./docker-install.sh --pull $(last_tag | sed 's/^v//')"
    say "     （多架构 manifest，amd64/arm64 自动挑；不需要 buildx）"
}

# ── 发行版 / 包管理器 ─────────────────────────────────────────────────────────

# 提示里给出的必须是在**这台机器上真能装上**的那一条：包名各发行版不一样，
# 写错一个就让人白跑一趟。
detect_pm() {
    local cand
    for cand in apt-get dnf yum zypper pacman apk; do
        if command -v "$cand" >/dev/null 2>&1; then printf '%s' "$cand"; return 0; fi
    done
    printf ''
}

pm_family() {
    case "$(detect_pm)" in
        apt-get) printf 'apt' ;;
        dnf|yum) printf 'dnf' ;;
        zypper)  printf 'zypper' ;;
        pacman)  printf 'pacman' ;;
        apk)     printf 'apk' ;;
        *)       printf 'unknown' ;;
    esac
}

os_pretty_name() {
    if [ -r /etc/os-release ]; then
        # shellcheck disable=SC1091
        . /etc/os-release
        printf '%s' "${PRETTY_NAME:-${NAME:-unknown}}"
    else
        printf '%s' "$(uname -srm)"
    fi
}

docker_install_hint() {
    case "$(pm_family)" in
        apt)    printf 'sudo apt-get update && sudo apt-get install -y docker.io' ;;
        dnf)    printf 'sudo dnf install -y moby-engine' ;;
        zypper) printf 'sudo zypper --non-interactive install docker' ;;
        pacman) printf 'sudo pacman -S --needed docker' ;;
        apk)    printf 'sudo apk add docker && sudo rc-update add docker default && sudo service docker start' ;;
        *)      printf 'curl -fsSL https://get.docker.com | sudo sh' ;;
    esac
}

buildx_install_hint() {
    case "$(pm_family)" in
        apt)
            say "  sudo apt-get update && sudo apt-get install -y docker-buildx"
            say "    # Ubuntu 23.10+ / Debian 12+ 有这个包；更老的发行版没有这个包名，"
            say "    # 改用 Docker 官方源里的 docker-buildx-plugin（见 docs.docker.com/engine/install）"
            ;;
        dnf)
            say "  sudo dnf install -y docker-buildx-plugin     # Docker 官方源里的名字"
            say "  sudo dnf install -y docker-buildx           # 有的版本用这个包名"
            ;;
        zypper) say "  sudo zypper install -y docker-buildx" ;;
        pacman) say "  sudo pacman -S --needed docker-buildx" ;;
        apk)    say "  sudo apk add docker-cli-buildx" ;;
        *)      say "  # 没有认出来的包管理器：用下面的通用办法" ;;
    esac
    say "  通用兜底（任何发行版，不需要包管理器；docker 已经在时可用）："
    say "    docker create --name buildx-tmp docker/buildx-bin:latest \\"
    say "      && docker cp buildx-tmp:/buildx ~/.docker/cli-plugins/docker-buildx \\"
    say "      && docker rm buildx-tmp && chmod +x ~/.docker/cli-plugins/docker-buildx"
    say "    docker buildx version"
}

as_root() {
    if [ "$(id -u)" = 0 ]; then
        "$@"
    elif command -v sudo >/dev/null 2>&1 && sudo -n true 2>/dev/null; then
        sudo "$@"
    else
        return 127
    fi
}

# 当前用户与 docker 组的关系，分两处查：
#   group_db_has_user  /etc/group（或 NSS）里有没有点名——usermod 生效了没有
#   group_active_now   当前进程的补充组里有没有 docker——当前会话拿到了没有
#
# 主组也算：有人把 `docker` 设成了主组（或在容器/镜像里就是这么配的），只看
# `/etc/group` 的成员列表会漏掉，脚本就会说「没加组」——这正是被抱怨过的
# 「明明加了，判定没过」。getent 不在时退回直接读 /etc/group。
group_db_has_user() {
    local user="$1" members=""
    if [ "$(id -gn "$user" 2>/dev/null)" = docker ]; then
        return 0
    fi
    if command -v getent >/dev/null 2>&1; then
        members=$(getent group docker 2>/dev/null | awk -F: '{print $4}')
    elif [ -r /etc/group ]; then
        members=$(awk -F: '$1=="docker" {print $4}' /etc/group)
    fi
    printf '%s' "$members" | tr ',' '\n' | grep -qx "$user"
}

# 注意：这里不能写 `id -nG 用户名`。带用户名查的是 NSS/数据库里的组，
# usermod 之后立刻就会显示 docker——那正是「加了组但当前会话没拿到」的误判来源。
# 不带参数查的才是当前进程实际持有的补充组。
group_active_now() {
    id -nG 2>/dev/null | tr ' ' '\n' | grep -qx docker
}

# 出问题时把三件事一起摊开：你是谁、这个会话有什么组、数据库里 docker 组有谁。
# 以前只给一句结论，用户看不出「加到了 root 身上」或者「加的是另一个账号」。
show_group_diagnosis() {
    local members=""
    if command -v getent >/dev/null 2>&1; then
        members=$(getent group docker 2>/dev/null | awk -F: '{print $4}')
    elif [ -r /etc/group ]; then
        members=$(awk -F: '$1=="docker" {print $4}' /etc/group)
    fi
    say "  当前用户：$(id -un)（uid $(id -u)）"
    say "  这个会话的组：$(id -nG 2>/dev/null)"
    say "  /etc/group 里的 docker 成员：${members:-（空）}"
    case ",${members}," in
        *,root,*) say "  注意：docker 组里加的是 root，不是 $(id -un)——用 sudo 跑过 usermod 时很容易这样。" ;;
    esac
}

# docker info 报 permission denied 时走这里。
#
# 只查 /etc/group 会把「刚 usermod、还没重新登录」误报成没问题；只查 id -nG 又
# 分不清「没加组」和「加了但会话旧」。两处都查，结论才是确定的：
#   都没有 → 代加组；加了但会话没有 → 用 sg 带上组重跑，不必重新登录。
fix_docker_access() {
    local user self quoted
    user=$(id -un)
    self="$REPO_DIR/$(basename "${BASH_SOURCE[0]}")"

    show_group_diagnosis

    if [ "$(id -u)" = 0 ]; then
        say ""
        say "以 root 运行仍被拒绝，不是组权限问题。检查 socket 和守护进程："
        say "  ls -l ${DOCKER_HOST:-/var/run/docker.sock}"
        say "  systemctl status docker"
        exit 1
    fi

    if group_active_now; then
        say ""
        say "$user 已经在当前会话的 docker 组里，但访问 docker 仍被拒绝。"
        say "这不是「没加组」，检查 socket 的属组和权限："
        say "  ls -l ${DOCKER_HOST:-/var/run/docker.sock}"
        say "  期望：srw-rw---- root docker"
        exit 1
    fi

    if group_db_has_user "$user"; then
        say ""
        say "组已经加好了（/etc/group 里有 $user）——这一步不是失败。"
        say "只是当前登录会话是加组之前启动的，进程还拿不到它；"
        say "重开终端没用，要用 sg 进组，或者整个会话重新登录。"
    else
        if ! getent group docker >/dev/null 2>&1 && [ ! -r /etc/group ]; then
            say ""
            say "读不到 docker 组（rootless docker 的权限不在 /etc/group）。"
            say "检查：ls -l ${DOCKER_HOST:-/var/run/docker.sock}"
            exit 1
        fi
        say ""
        say "$user 不在 docker 组里，正在加入（需要 root 或免密 sudo）……"
        if ! do_root usermod -aG docker "$user"; then
            say ""
            say "加组失败。手动执行后重跑本脚本："
            say "  sudo usermod -aG docker $user"
            say "  # 如果刚才那条 sudo 是别的用户在跑，确认加的是 $(id -un)，不是 root："
            say "  getent group docker"
            exit 1
        fi
        if [ "$DRY_RUN" = 1 ]; then
            return 0
        fi
        # 加完立刻复核：写进去了就明说，没写进去就别再往下走。
        if group_db_has_user "$user"; then
            say "  已写入 /etc/group：$(getent group docker 2>/dev/null || awk -F: '$1=="docker"' /etc/group)"
        else
            say ""
            say "usermod 跑完了，但 /etc/group 里仍然没有 $user。"
            say "常见原因：sudoers 里限定、NSS 用了 LDAP/sssd、或者根文件系统只读。"
            say "请手动确认后重跑：getent group docker"
            exit 1
        fi
    fi

    # 已经用 sg 重跑过一次还是不行：别再来一次，直接说清楚要重新登录。
    if [ "${HDSL_DOCKER_GROUP_REEXEC:-}" = 1 ]; then
        say ""
        say "已经用 sg 重跑过一次，当前会话仍然拿不到 docker 组。"
        say "请注销后重新登录（GUI 要整个会话重登）。"
        exit 1
    fi

    # 不用重新登录：sg 在子 shell 里带上 docker 组，把本脚本原样重跑。
    # 先试一下 sg 能不能真的进组，免得它在这里要密码、把脚本挂住。
    if command -v sg >/dev/null 2>&1 && sg docker -c true </dev/null >/dev/null 2>&1; then
        say "  用 sg 带上 docker 组重新执行本脚本（不需要重新登录）……"
        quoted=$(printf '%q ' "$self" "${ORIG_ARGS[@]}")
        exec env HDSL_DOCKER_GROUP_REEXEC=1 sg docker -c "$quoted"
    fi

    say ""
    say "组是真的加上了，只是这个会话拿不到它——不算失败。任选其一："
    say "  1) 注销后重新登录（GUI 也要整个会话重登，新开终端不算）"
    say "  2) 在当前终端里进组（之后的命令都有效）：newgrp docker"
    say "  3) 一条命令搞定，不用重登："
    say "     sg docker -c '$(printf '%q ' "$self" "${ORIG_ARGS[@]}")'"
    say ""
    say "验证加成功了没有：getent group docker    ← 里面应当有 $(id -un)"
    exit 1
}

show_install_hint() {
    local pm
    pm=$(detect_pm)
    say ""
    say "这台机器上没有 docker（$(os_pretty_name)）。"
    say ""
    say "  1) 装 docker："
    say "     $(docker_install_hint)"
    [ -n "$pm" ] || say "     （没认出包管理器，用上面的通用安装脚本）"
    say ""
    say "  2) 装完确认守护进程在跑："
    say "     docker info        ← 报 permission denied 就继续第 3 步"
    say "     # 没有 systemd（WSL/容器里）时手动起：sudo dockerd &"
    say ""
    say "  3) 把当前用户加进 docker 组（否则每条命令都要 sudo）："
    say "     sudo usermod -aG docker $(id -un)"
    say "     getent group docker      ← 这里应当出现 $(id -un)"
    say "     sg docker -c './docker-install.sh'   # 不想重新登录就先用 sg 进组"
    say "     # 或者注销后重新登录（GUI 要整个会话重登，新开终端不算）"
    say ""
    say "  4) 构建还要 buildx（Ubuntu 的 docker.io 常常不带它）："
    buildx_install_hint
    say ""
    say "或者让本脚本代装 docker + buildx：./docker-install.sh --install-docker"
    say "只想先看看缺什么：./docker-install.sh --doctor"
}

# 以 root 跑一条安装命令；--dry-run 时只打印。
do_root() {
    if [ "$DRY_RUN" = 1 ]; then
        say "  [dry-run] $*"
    else
        as_root "$@"
    fi
}

install_docker_now() {
    local pm
    pm=$(detect_pm)
    [ -n "$pm" ] || die "认不出包管理器，请照上面的提示手动安装"

    if [ "$DRY_RUN" = 0 ]; then
        as_root true || die "需要 root 或免密 sudo 才能代装 docker（否则请手动装）"
    fi

    step "安装 docker（$pm）"
    case "$pm" in
        apt-get)
            do_root apt-get update
            do_root sh -c 'DEBIAN_FRONTEND=noninteractive apt-get install -y docker.io'
            do_root systemctl enable --now docker
            ;;
        dnf)
            do_root dnf install -y moby-engine || do_root dnf install -y docker
            do_root systemctl enable --now docker
            ;;
        yum)
            do_root yum install -y docker
            do_root systemctl enable --now docker
            ;;
        apk)
            do_root apk add --no-cache docker
            do_root rc-update add docker default
            do_root service docker start
            ;;
        pacman)
            do_root pacman -Sy --noconfirm docker
            do_root systemctl enable --now docker
            ;;
        zypper)
            do_root zypper --non-interactive install docker
            do_root systemctl enable --now docker
            ;;
    esac

    if [ "$DRY_RUN" = 1 ]; then
        say "  （dry-run：不会真的安装）"
        return 0
    fi

    command -v docker >/dev/null 2>&1 || die "安装命令跑完了，但 PATH 里仍然没有 docker"

    # 顺手把 buildx 装上：Dockerfile 用了 BuildKit 的缓存挂载，缺它构建一步都走不了，
    # 而 Ubuntu 的 docker.io 正好常常不带。装不上不算致命（--pull 不需要它），给提示。
    if ! buildx_ready; then
        step "补装 buildx"
        case "$pm" in
            apt-get) do_root sh -c 'DEBIAN_FRONTEND=noninteractive apt-get install -y docker-buildx' || true ;;
            dnf)     do_root dnf install -y docker-buildx-plugin || do_root dnf install -y docker-buildx || true ;;
            yum)     do_root yum install -y docker-buildx-plugin || true ;;
            zypper)  do_root zypper --non-interactive install docker-buildx || true ;;
            pacman)  do_root pacman -S --needed --noconfirm docker-buildx || true ;;
            apk)     do_root apk add --no-cache docker-cli-buildx || true ;;
        esac
        if ! buildx_ready; then
            warn "buildx 仍然没装上；本地构建会失败（--pull 不受影响）"
            show_buildx_hint
        fi
    fi

    # 权限/守护进程的问题统一交给 ensure_docker：那里会分辨「没加组」「加了但会话旧」。
    ensure_docker
}

ensure_docker() {
    if docker_present; then
        if docker_ready; then
            return 0
        fi
        # 有 CLI 但连不上：多半是没启动，或没进 docker 组
        local err
        err=$(docker info 2>&1 || true)
        case "$err" in
            *"Permission denied"*|*"permission denied"*)
                fix_docker_access
                ;;
        esac
        if command -v systemctl >/dev/null 2>&1; then
            warn "docker 守护进程没在跑，尝试启动"
            as_root systemctl start docker >/dev/null 2>&1 || true
            docker_ready && return 0
        fi
        if [ "$DRY_RUN" = 1 ]; then
            warn "docker 不可用，dry-run 继续打印计划"
            return 0
        fi
        die "docker 装好了但守护进程不可用（试过启动）。请检查：systemctl status docker"
    fi

    # 完全没有 docker：绝对冷启动
    show_install_hint
    if [ "$INSTALL_DOCKER" = 1 ]; then
        install_docker_now
        return 0
    fi
    if [ "$DRY_RUN" = 1 ]; then
        say "  （dry-run：假装 docker 可用，继续打印计划）"
        return 0
    fi
    exit 2
}

# ── 版本号、端口、目录 ────────────────────────────────────────────────────────

last_tag() {
    local tag=""
    tag=$(git -C "$REPO_DIR" describe --tags --abbrev=0 2>/dev/null || true)
    [ -n "$tag" ] || tag="v0.0.0"
    printf '%s' "$tag"
}


sanitize_tag() {
    printf '%s' "$1" | tr '[:upper:]' '[:lower:]' | sed 's/[^a-z0-9_.-]/-/g'
}

# 本地构建的镜像名：<最后一个版本标签>-build-<系统版本>-<日期时间>
# 例：hdsl-web:v0.6.34-build-debian13-20261030-202516
default_image() {
    local tail_part
    # 从前这里是 <宿主系统版本>；底座能选之后那个名字会读成 "fedora-debian13"，而宿主
    # 版本对镜像内容没有意义（jar 是同一颗），时间戳已经足够区分重建。
    tail_part=$(sanitize_tag "$(last_tag)-build-${BASE_CODE}-$(date +%Y%m%d-%H%M%S)")
    printf 'hdsl-web:%s' "$tail_part"
}

port_in_use() {
    local p="$1"
    if command -v python3 >/dev/null 2>&1; then
        python3 - "$p" <<'PY'
import socket, sys
port = int(sys.argv[1])
s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
s.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
try:
    s.bind(("0.0.0.0", port))
except OSError:
    sys.exit(0)          # 0 = 被占用
finally:
    s.close()
sys.exit(1)
PY
    elif command -v ss >/dev/null 2>&1; then
        ss -ltn 2>/dev/null | awk '{print $4}' | grep -qE "[:.]${p}$"
    else
        return 1             # 没有探测手段就当它空闲
    fi
}

# ── MC 端口映射 ───────────────────────────────────────────────────────────────

# 规格展开成「一行一个端口」：`25565` / `25565-25575` / `25565,25566`。
#
# 失败用退出码表达，不在管道里 die：管道里的 exit 只结束子 shell，脚本会带着一个
# 半截列表继续往下跑——那正是「校验看着有、实际没拦住」的来源。
#   2 = 不是数字；3 = 范围写反；4 = 超过 128 个（输入错的典型形状）
mc_ports_expand() {
    local spec="$1" item from to p
    local -a out=()
    case "$spec" in
        ""|none|no|-|NONE|None) return 0 ;;
    esac
    local IFS=','
    for item in $spec; do
        [ -n "$item" ] || continue
        case "$item" in
            *-*) from=${item%%-*}; to=${item##*-} ;;
            *)   from=$item; to=$item ;;
        esac
        case "$from" in ''|*[!0-9]*) return 2 ;; esac
        case "$to"   in ''|*[!0-9]*) return 2 ;; esac
        [ "$from" -le "$to" ] || return 3
        p=$from
        while [ "$p" -le "$to" ]; do
            out+=("$p")
            p=$((p + 1))
            [ "${#out[@]}" -le 128 ] || return 4
        done
    done
    [ "${#out[@]}" -gt 0 ] || return 0
    printf '%s\n' "${out[@]}" | awk 'NF && !seen[$0]++'
}

# 校验并展开；不合格就带着原因退出。
mc_ports_checked() {
    local spec="$1" out="" rc=0
    out=$(mc_ports_expand "$spec") && rc=0 || rc=$?
    case "$rc" in
        0) printf '%s' "$out" ;;
        2) die "MC 端口必须是数字（收到：$spec）；例：25565 或 25565-25575" ;;
        3) die "MC 端口范围写反了（收到：$spec）；例：25565-25575" ;;
        4) die "MC 端口一次最多映射 128 个（收到：$spec）" ;;
        *) die "MC 端口看不懂（收到：$spec）" ;;
    esac
}

mc_ports_summary() {
    local list count
    list=$(mc_ports_checked "$MC_PORTS")
    if [ -z "$list" ]; then
        printf '不对外开服（MC 端口没有映射到宿主）'
        return 0
    fi
    count=$(printf '%s\n' "$list" | wc -l | tr -d ' ')
    printf '%s 个端口：%s' "$count" "$(printf '%s' "$list" | paste -sd, -)"
}

# ── 状态文件 ──────────────────────────────────────────────────────────────────

load_state() {
    [ -f "$STATE_FILE" ] || return 0
    # shellcheck disable=SC1090
    . "$STATE_FILE"
    PREV_NAME=${INSTALL_NAME:-}
    PREV_PORT=${INSTALL_PORT:-}
    PREV_BIND=${INSTALL_BIND:-}
    PREV_DATA=${INSTALL_DATA:-}
    PREV_WORKSPACE=${INSTALL_WORKSPACE:-}
    PREV_MC=${INSTALL_MC:-}
    PREV_VIDEO=${INSTALL_VIDEO:-}
    PREV_HW=${INSTALL_HW:-}
    PREV_MC_PORTS=${INSTALL_MC_PORTS:-}
    PREV_MIRROR=${INSTALL_MIRROR:-}
    PREV_RELEASE=${INSTALL_RELEASE:-}
    PREV_IMAGE=${INSTALL_IMAGE:-}
    PREV_BASE=${INSTALL_BASE:-}
    PREV_BASE_REGISTRY=${INSTALL_BASE_REGISTRY:-}
    PREV_APT_MIRROR=${INSTALL_APT_MIRROR:-}
}

save_state() {
    {
        printf 'INSTALL_NAME=%q\n' "$NAME"
        printf 'INSTALL_PORT=%q\n' "$PORT"
        printf 'INSTALL_BIND=%q\n' "$BIND"
        printf 'INSTALL_DATA=%q\n' "$DATA"
        printf 'INSTALL_WORKSPACE=%q\n' "$WORKSPACE"
        printf 'INSTALL_MC=%q\n' "$MC"
        printf 'INSTALL_VIDEO=%q\n' "$VIDEO"
        printf 'INSTALL_HW=%q\n' "$HW"
        printf 'INSTALL_MC_PORTS=%q\n' "$MC_PORTS"
        printf 'INSTALL_MIRROR=%q\n' "$MIRROR_MODE"
        printf 'INSTALL_RELEASE=%q\n' "$([ "$RELEASE" = 1 ] && echo yes || echo no)"
        printf 'INSTALL_IMAGE=%q\n' "$IMAGE"
        printf 'INSTALL_BASE=%q\n' "$BASE_CODE"
        # 记住底座仓库：Docker Hub 不通的机器上，不记的话每次重跑都得再补一次
        # --base-registry（报告里那次"跑不通"有一半是这么来的）。
        printf 'INSTALL_BASE_REGISTRY=%q\n' "$BASE_REGISTRY"
        # apt 源同理：国内机器上它决定了每次重建要不要再等十分钟。
        printf 'INSTALL_APT_MIRROR=%q\n' "$APT_MIRROR"
    } > "$STATE_FILE"
    chmod 600 "$STATE_FILE"
}

save_env() {
    (
        umask 077
        {
            printf 'HDSL_PORT=%s\n' "$PORT"
            printf 'HDSL_MEDIA_ROOT=%s\n' "/home/hdsl/video"
            printf 'NPM_CONFIG_REGISTRY=%s\n' "$NPM_REGISTRY"
            printf 'JAVA_OPTS=%s\n' "$JAVA_OPTS"
            if [ -n "$PASSWORD" ]; then
                printf 'HDSL_ADMIN_PASSWORD=%s\n' "$PASSWORD"
            fi
        } > "$ENV_FILE"
    )
    chmod 600 "$ENV_FILE"
}

# ── docker 动作 ───────────────────────────────────────────────────────────────

container_status() {
    # 容器不存在时，`docker inspect` 会先往 stdout 写一个空行再以非零退出；
    # `|| printf missing` 于是拼出 "\nmissing"，任何 `!= missing` 判断都会把
    # 「不存在」读成「存在」——全新安装直接卡在「已经存在，状态：missing」。
    # 所以取出结果后去掉空白，只有真拿不到状态才算 missing。
    local status
    status=$(docker inspect -f '{{.State.Status}}' "$1" 2>/dev/null || true)
    status=$(printf '%s' "$status" | tr -d '[:space:]')
    printf '%s' "${status:-missing}"
}

# 把底座镜像名换到 --base-registry 指的那个仓库。
#
# Docker Hub 上的官方镜像在 API 里都带 `library/` 前缀，`docker pull debian:…` 只是
# 客户端替你补的；换成别的仓库时必须把这一节写全，否则镜像是
# `docker.m.daocloud.io/debian:…`——那里没有这个仓库，报的错反而更难懂。
with_registry() {
    if [ -z "$BASE_REGISTRY" ]; then
        printf '%s' "$1"
        return
    fi
    local prefix="${BASE_REGISTRY%/}"
    case "$prefix" in
        */library) printf '%s/%s' "$prefix" "$1" ;;
        *)         printf '%s/library/%s' "$prefix" "$1" ;;
    esac
}

# 已发布的镜像（--pull）。本地名不变，所以状态文件、--no-build、容器重建都照旧认它。
pull_image() {
    local source
    case "$PREBUILT" in
        */*|*:*) source="$PREBUILT" ;;
        *)       source="ghcr.io/nas200-timmy/hdsl-web:$PREBUILT" ;;
    esac
    step "拉取镜像：$source"
    if [ "$DRY_RUN" = 1 ]; then
        say "  [dry-run] docker pull $source"
        say "  [dry-run] docker tag $source $IMAGE"
        return 0
    fi
    if ! docker pull "$source"; then
        die "拉不到 $source
   ghcr.io 也不通的话，只能本地构建；那时用 --base-registry 把底座换到一个能连的仓库。"
    fi
    docker tag "$source" "$IMAGE"
}

build_image() {
    if [ -n "$PREBUILT" ]; then
        pull_image
        return 0
    fi
    if [ "$DRY_RUN" != 1 ] && ! buildx_ready; then
        show_buildx_hint
        die "镜像构建失败：缺少 buildx"
    fi
    step "构建镜像：$IMAGE"
    local web_base build_base runtime_base
    web_base=$(with_registry "node:22-bookworm-slim")
    build_base=$(with_registry "eclipse-temurin:21-jdk")
    runtime_base=$(with_registry "$BASE_IMAGE")
    if [ "$DRY_RUN" = 1 ]; then
        say "  [dry-run] docker build -t $IMAGE \\"
        say "      --build-arg NPM_REGISTRY=$NPM_REGISTRY \\"
        say "      --build-arg NODE_DIST_MIRROR=$NODE_DIST_MIRROR \\"
        say "      --build-arg APT_MIRROR=$APT_MIRROR \\"
        say "      --build-arg HDSL_BASE=$runtime_base --build-arg HDSL_ENCODERS=$BASE_ENCODERS \\"
        say "      --build-arg HDSL_WEB_BASE=$web_base --build-arg HDSL_BUILD_BASE=$build_base \\"
        say "      --build-arg RELEASE_VERSION=$RELEASE_VERSION -f Dockerfile ."
        return 0
    fi
    # Dockerfile 里的 `RUN --mount=type=cache` 要 BuildKit；老引擎（NAS 上的 20.10）
    # 默认关着，不显式打开会在这一步报 "the --mount option requires BuildKit"。
    #
    # 底座那三个 ARG 以前只在 dry-run 里出现，真构建时一个都没传——`--base fedora`
    # 于是静默构建出了 Debian 的镜像。现在它们和仓库前缀走同一条路。
    local log
    log=$(mktemp)
    if ! DOCKER_BUILDKIT=1 docker build \
        -t "$IMAGE" \
        --build-arg "NPM_REGISTRY=$NPM_REGISTRY" \
        --build-arg "NODE_DIST_MIRROR=$NODE_DIST_MIRROR" \
        --build-arg "APT_MIRROR=$APT_MIRROR" \
        --build-arg "HDSL_BASE=$runtime_base" \
        --build-arg "HDSL_ENCODERS=$BASE_ENCODERS" \
        --build-arg "HDSL_WEB_BASE=$web_base" \
        --build-arg "HDSL_BUILD_BASE=$build_base" \
        --build-arg "RELEASE_VERSION=$RELEASE_VERSION" \
        -f "$REPO_DIR/Dockerfile" "$REPO_DIR" 2>&1 | tee "$log"; then
        # 失败有两种，报错长得很像：拉不到底座镜像，和构建**里面**的包管理器连不上。
        # 以前只认前者，于是 `apt-get update` 卡在 deb.debian.org 时给出的建议是
        # "换个镜像仓库"——照做没用，因为 apt 根本不走那个仓库。
        local pull=0 apt=0
        if grep -qiE "load metadata|failed to resolve source metadata|registry-1\.docker\.io|pull access denied" "$log"; then
            pull=1
        fi
        if grep -qiE "deb\.debian\.org|security\.debian\.org|archive\.ubuntu\.com|Could not connect to|Temporary failure resolving|^Err:|E: Failed to fetch" "$log"; then
            apt=1
        fi
        if [ "$apt" = 1 ] && [ "$pull" = 0 ]; then
            say ""
            say "  !! 构建卡在容器里的 apt 下载这一步（${APT_MIRROR:-发行版默认源 deb.debian.org}）。"
            say "  !! apt 的源和底座镜像的仓库是两回事，换仓库不影响它："
            say "  !!   ./docker-install.sh --mirror …                                  # 国内镜像站（一并带上 apt 源）"
            say "  !!   ./docker-install.sh --apt-mirror http://mirrors.tuna.tsinghua.edu.cn …"
            say "  !!   ./docker-install.sh --pull $(last_tag | sed 's/^v//') …                 # 不构建，直接拉已发布的镜像"
            say "  !! 中断不会白等：已经建好的层和 apt 缓存都留着，重跑从断点继续。"
        elif [ "$pull" = 1 ]; then
            say ""
            if [ -n "$BASE_REGISTRY" ]; then
                say "  !! 构建卡在拉底座镜像这一步，而底座已经走的是 ${BASE_REGISTRY}。"
                say "  !! 换一个仓库，或者直接拉已发布的镜像："
                say "  !!   ./docker-install.sh --base-registry ${BASE_REGISTRY_ALT} …   # 换备选镜像站"
                say "  !!   ./docker-install.sh --pull $(last_tag | sed 's/^v//') …        # 不构建"
            else
                say "  !! 构建卡在拉底座镜像这一步（Docker Hub 不可达或限流），而底座走的是 Docker Hub。"
                say "  !! 换一个能连的仓库，或者直接拉已发布的镜像："
                say "  !!   ./docker-install.sh --mirror …                        # 国内镜像站"
                say "  !!   ./docker-install.sh --base-registry ${BASE_REGISTRY_ALT} …   # 换这个镜像站"
                say "  !!   ./docker-install.sh --pull $(last_tag | sed 's/^v//') …        # 不构建"
            fi
            say "  !! 也可以在 /etc/docker/daemon.json 配 registry-mirrors，那是全局设置（配一次，"
            say "  !! 所有 docker pull 都受益，包括构建时的底座）。"
        elif grep -qi "dial tcp\|connection refused\|i/o timeout" "$log"; then
            say ""
            say "  !! 构建里有连接超时。先分清是哪一段：翻上面的日志，`load metadata` 是拉底座，"
            say "  !! `apt-get` / `npm` / `gradlew` 是构建内部。前者换 --base-registry，后两者"
            say "  !! 用 --mirror（apt / npm / Node 一起换）。"
        fi
        rm -f "$log"
        die "镜像构建失败"
    fi
    rm -f "$log"
}

ensure_owner() {   # 宿主目录：只建出来，属主交给容器入口去分
    # 以前这里是 `docker run -u 0 … chown -R 1000:1000`，把整卷交给 hdsl。现在不行了：
    # 面板在容器里以 root 跑，auth/、certs/、server.yaml、logs/ 必须是 root 0700，
    # 整卷 chown 等于每次启动把秘密又交还给 agent 的 uid。
    # 入口脚本（root）每次启动自己校正属主，所以这里只需要保证目录存在。
    local path="$1"
    if [ "$DRY_RUN" = 1 ]; then
        say "  [dry-run] mkdir -p $path   # 属主由容器入口校正（面板 root，秘密 0700）"
        return 0
    fi
    mkdir -p "$path"
}

nvidia_docker_ready() {
    command -v nvidia-smi >/dev/null 2>&1 || return 1
    docker info --format '{{json .Runtimes}}' 2>/dev/null | grep -q '"nvidia"'
}

run_container() {
    local mounts=()
    case "$DATA" in
        volume:*) mounts+=(-v "${DATA#volume:}:/data") ;;
        *)        ensure_owner "$DATA"; mounts+=(-v "$DATA:/data") ;;
    esac
    if [ -n "$WORKSPACE" ]; then
        case "$WORKSPACE" in
            volume:*) mounts+=(-v "${WORKSPACE#volume:}:/home/hdsl/workspace") ;;
            *)        ensure_owner "$WORKSPACE"; mounts+=(-v "$WORKSPACE:/home/hdsl/workspace") ;;
        esac
    fi
    if [ -n "$MC" ]; then
        case "$MC" in
            volume:*) mounts+=(-v "${MC#volume:}:/home/hdsl/mc") ;;
            *)        ensure_owner "$MC"; mounts+=(-v "$MC:/home/hdsl/mc") ;;
        esac
    fi
    if [ -n "$VIDEO" ] && [ "$VIDEO" != none ]; then
        case "$VIDEO" in
            volume:*) mounts+=(-v "${VIDEO#volume:}:/home/hdsl/video") ;;
            *)        ensure_owner "$VIDEO"; mounts+=(-v "$VIDEO:/home/hdsl/video") ;;
        esac
    fi

    # Hardware encoding: only add flags the host can actually satisfy. A
    # `--device /dev/dri` on a machine without it makes Docker refuse to start
    # the container, which is worse than falling back to CPU.
    local hw_args=()
    case "$HW" in
        auto)
            if [ -e /dev/dri ]; then hw_args+=(--device /dev/dri); fi
            if nvidia_docker_ready; then hw_args+=(--gpus all); fi
            ;;
        vaapi)
            if [ -e /dev/dri ]; then hw_args+=(--device /dev/dri)
            else say "  [·] 没有 /dev/dri：VAAPI/QSV 直通跳过"; fi
            ;;
        nvidia)
            if nvidia_docker_ready; then hw_args+=(--gpus all)
            else say "  [·] nvidia-container-toolkit 不可用：NVENC 直通跳过"; fi
            ;;
        none|"") ;;
        *) die "未知的 --hw 值：$HW（auto / vaapi / nvidia / none）" ;;
    esac

    step "启动容器：$NAME"
    # MC 端口一行一个 `-p 宿主:容器`，两边同号：容器里的服务端监听的就是实例的
    # `server-port`，宿主端口跟它不一致只会让人对不上号。
    local mc_mappings=() p
    while read -r p; do
        [ -n "$p" ] || continue
        mc_mappings+=(-p "${BIND}:${p}:${p}")
    done <<< "${MC_PORT_LIST:-}"
    local cmd=(docker run -d
        --name "$NAME"
        --restart unless-stopped
        --stop-timeout 40
        -p "${BIND}:${PORT}:${PORT}"
        ${mc_mappings[@]+"${mc_mappings[@]}"}
        --env-file "$ENV_FILE"
        "${mounts[@]}"
        ${hw_args[@]+"${hw_args[@]}"}
        "$IMAGE")

    if [ "$DRY_RUN" = 1 ]; then
        say "  [dry-run] ${cmd[*]}"
        return 0
    fi
    save_env
    "${cmd[@]}" >/dev/null
}

wait_healthy() {
    local name="$1" waited=0 limit=180 status
    step "等待健康检查（首次启动要生成自签证书、建 pnpm store，给足两分钟）"
    if [ "$DRY_RUN" = 1 ]; then
        say "  [dry-run] 不等待"
        return 0
    fi
    while [ "$waited" -lt "$limit" ]; do
        status=$(docker inspect -f '{{if .State.Health}}{{.State.Health.Status}}{{else}}none{{end}}' "$name" 2>/dev/null || true)
        status=$(printf '%s' "$status" | tr -d '[:space:]')
        [ -n "$status" ] || status=missing
        case "$status" in
            healthy)
                say "  健康（等了 ${waited}s）"
                return 0
                ;;
            unhealthy|missing|exited|dead)
                say ""
                say "容器没有起来（状态：$status）。最近 100 行日志："
                docker logs --tail 100 "$name" 2>&1 || true
                case "$status" in
                    unhealthy)
                        say ""
                        say "常见原因：$DATA 不可写（属主不是 1000）、端口冲突、首次下载超时。"
                        ;;
                esac
                return 1
                ;;
        esac
        sleep 3
        waited=$((waited + 3))
        printf '.'
    done
    printf '\n'
    say "180 秒还没到 healthy。日志："
    docker logs --tail 100 "$name" 2>&1 || true
    return 1
}

stop_container() {
    local status
    status=$(container_status "$NAME")
    [ "$status" = missing ] && return 0
    if [ "$status" = running ]; then
        step "停止容器（最多等 40 秒让它把 dsh 实例收干净）"
        if [ "$DRY_RUN" = 1 ]; then
            say "  [dry-run] docker stop -t 40 $NAME"
        else
            docker stop -t 40 "$NAME" >/dev/null
        fi
    fi
}

remove_container() {
    [ "$(container_status "$NAME")" = missing ] && return 0
    if [ "$DRY_RUN" = 1 ]; then
        say "  [dry-run] docker rm $NAME"
    else
        docker rm "$NAME" >/dev/null
    fi
}

image_exists() { docker image inspect "$1" >/dev/null 2>&1; }

# ── 体检（--doctor）───────────────────────────────────────────────────────────
#
# 只读：不装包、不构建、不起容器、不写状态。每一条都给「现在是什么」与「怎么修」——
# 「照着提示做完仍然跑不通」几乎都出在提示缺了一步（Ubuntu 的 docker.io 不带 buildx
# 就是典型），所以这里的提示按本机包管理器现算，不再是一段万能文案。
doctor() {
    local failures=0 warnings=0 err avail p busy
    step "体检（只读：不装包、不构建、不起容器、不写状态）"
    say "  系统：$(os_pretty_name)"
    say "  包管理器：$(detect_pm || true)"

    if docker_present; then
        say "  [✓] docker：$(docker --version 2>/dev/null)"
    else
        say "  [✗] docker：没有"
        say "      装法：$(docker_install_hint)"
        failures=$((failures + 1))
    fi

    if docker_present; then
        if docker_ready; then
            say "  [✓] docker 守护进程：可用"
        else
            err=$(docker info 2>&1 || true)
            case "$err" in
                *[Pp]ermission\ denied*)
                    say "  [✗] docker 守护进程：在跑，但当前用户没有权限"
                    show_group_diagnosis
                    say "      修：sudo usermod -aG docker $(id -un)，然后 newgrp docker（或重新登录）"
                    ;;
                *)
                    say "  [✗] docker 守护进程：连不上"
                    say "      修：systemctl start docker（没有 systemd 时：sudo dockerd &）"
                    ;;
            esac
            failures=$((failures + 1))
        fi
    fi

    if docker_present; then
        if buildx_ready; then
            say "  [✓] buildx：$(docker buildx version 2>/dev/null | head -1)"
        else
            say "  [!] buildx：缺失（只有本地构建要用；--pull 不需要）"
            buildx_install_hint
            warnings=$((warnings + 1))
        fi
        if docker compose version >/dev/null 2>&1; then
            say "  [✓] docker compose：可用（可选）"
        else
            say "  [·] docker compose：没有（可选：本脚本用 docker run）"
        fi
    fi

    if command -v df >/dev/null 2>&1; then
        avail=$(df -Pk "$REPO_DIR" 2>/dev/null | awk 'NR==2{print int($4/1024/1024)}')
        if [ -n "${avail:-}" ] && [ "$avail" -ge 4 ]; then
            say "  [✓] 磁盘：${avail} GiB 可用（构建约需 3–4 GiB）"
        else
            say "  [!] 磁盘：只有 ${avail:-?} GiB 可用（构建约需 3–4 GiB）"
            warnings=$((warnings + 1))
        fi
    fi

    if port_in_use "$PORT"; then
        if docker port "$NAME" 2>/dev/null | grep -qE "[:.]${PORT}\$"; then
            say "  [✓] 端口 $PORT（面板）：由本脚本的容器 $NAME 占着（升级时正常）"
        else
            say "  [!] 端口 $PORT（面板）：已被占用——换 --port，或先停掉占用它的进程"
            warnings=$((warnings + 1))
        fi
    else
        say "  [✓] 端口 $PORT（面板）：空闲"
    fi

    if [ -n "$(mc_ports_expand "$MC_PORTS" 2>/dev/null)" ]; then
        busy=""
        while read -r p; do
            [ -n "$p" ] || continue
            port_in_use "$p" && busy="$busy $p"
        done <<< "$(mc_ports_expand "$MC_PORTS")"
        if [ -n "$busy" ]; then
            say "  [!] MC 端口被占用：$busy"
            warnings=$((warnings + 1))
        else
            say "  [✓] MC 端口：$(mc_ports_summary)"
        fi
    else
        say "  [·] MC 端口：没有映射（默认不开服；要开用 --mc-ports 25565）"
    fi

    say ""
    if [ "$failures" -gt 0 ]; then
        say "结论：$failures 项必须先修好（见上面的「装法」「修」）。"
        exit 1
    fi
    if [ "$warnings" -gt 0 ]; then
        say "结论：必需项齐全，$warnings 项提醒看一眼。可以 ./docker-install.sh 开始了。"
    else
        say "结论：齐全。可以 ./docker-install.sh 开始了。"
    fi
}

# ── 主流程 ────────────────────────────────────────────────────────────────────

[ -f "$REPO_DIR/Dockerfile" ] || die "这个目录里没有 Dockerfile，请在仓库根目录运行"

load_state

if [ "$DOCTOR" = 1 ]; then
    # 体检用的是「这次会用的配置」：命令行 > 状态文件 > 默认值。它不能走 ensure_docker
    # ——那条路径会试图装包、加组，而 --doctor 的承诺是「什么都不改」。
    [ -n "$NAME" ] || NAME="${PREV_NAME:-$DEFAULT_NAME}"
    [ -n "$PORT" ] || PORT="${PREV_PORT:-$DEFAULT_PORT}"
    if [ "$MC_PORTS_SET" = 0 ]; then
        MC_PORTS="${PREV_MC_PORTS:-}"
    fi
    doctor
    exit 0
fi

step "检查 docker"
ensure_docker
say "  docker：$(docker --version 2>/dev/null || echo '不可用（dry-run）')"
# buildx 只有本地构建才需要；--pull 走的是 docker pull/tag/run，一样能起。所以这里只探
# 不说死，真到要构建时再拦（见 build_image）。
if [ "$DRY_RUN" != 1 ] && docker_present; then
    if buildx_ready; then
        say "  buildx：$(docker buildx version 2>/dev/null | head -1)"
    else
        say "  buildx：缺失（本地构建需要它；--pull 不需要）"
    fi
fi

# 已有状态作为默认值：重跑时只按回车就是上次的配置
[ -n "$NAME" ] || NAME="${PREV_NAME:-$DEFAULT_NAME}"
[ -n "$PORT" ] || PORT="${PREV_PORT:-$DEFAULT_PORT}"
[ -n "$BIND" ] || BIND="${PREV_BIND:-$DEFAULT_BIND}"
[ -n "$DATA" ] || DATA="${PREV_DATA:-$REPO_DIR/hdsl-web-data}"
if [ "$WORKSPACE_SET" = 0 ] && [ -z "$WORKSPACE" ]; then
    WORKSPACE="${PREV_WORKSPACE:-}"
fi
if [ "$MC_SET" = 0 ] && [ -z "$MC" ]; then
    MC="${PREV_MC:-volume:hdsl-mc}"
fi
if [ "$VIDEO_SET" = 0 ] && [ -z "$VIDEO" ]; then
    VIDEO="${PREV_VIDEO:-volume:hdsl-video}"
fi
if [ "$HW_SET" = 0 ] && [ -z "$HW" ]; then
    HW="${PREV_HW:-auto}"
fi
if [ "$MC_PORTS_SET" = 0 ]; then
    # 默认不映射：开服是暴露面，不能因为重跑一次脚本就悄悄多开一组端口。
    MC_PORTS="${PREV_MC_PORTS:-}"
fi
if [ -z "$MIRROR" ]; then
    MIRROR="${PREV_MIRROR:-}"
fi

step "配置"
if [ "$ASSUME_YES" = 0 ]; then
    say "  回车表示括号里的默认值。"
fi

NAME=$(ask "容器名" "$NAME")
DATA=$(ask "面板数据（/data；宿主路径或 volume:名称）" "$DATA")
WORKSPACE=$(ask "工作区（挂到 /home/hdsl/workspace；留空或 none = 不挂，重建即丢）" "$WORKSPACE")
MC=$(ask "MC 服务器数据（挂到 /home/hdsl/mc；volume:名称 或宿主路径；none = 不挂）" "$MC")
VIDEO=$(ask "转码媒体库（挂到 /home/hdsl/video；volume:名称或宿主路径；none = 不挂）" "$VIDEO")
HW=$(ask "硬件编码直通（auto / vaapi / nvidia / none）" "$HW")
say "  MC 端口：容器里的服务端要局域网能连，就得在这里映射到宿主。"
say "  格式 25565 / 25565-25575 / 25565,25566；留空 = 不对外开服（默认）。"
MC_PORTS=$(ask "MC 游戏端口（映射到宿主）" "$MC_PORTS")
PORT=$(ask "面板端口（容器内外同号）" "$PORT")
BIND=$(ask "监听地址（0.0.0.0=局域网，127.0.0.1=只本机）" "$BIND")
if [ "$PASSWORD_SET" = 0 ]; then
    PASSWORD=$(ask_password)
fi
if [ -z "$MIRROR" ] && [ "$BASE_REGISTRY_SET" = 0 ]; then
    # 一个菜单同时决定 npm/Node 和底座仓库：这三个来源本来就是一件事（能不能连上），
    # 分成两个问题问，答案还可能互相矛盾。
    # 默认项跟着上次走：上次选了官方源 → 停在"官方源"；上次记着备选仓库 → 停在备选。
    if [ "${PREV_MIRROR:-yes}" = no ]; then
        SELECT_DEFAULT=3
    elif [ "${PREV_BASE_REGISTRY:-}" = "$BASE_REGISTRY_ALT" ]; then
        SELECT_DEFAULT=2
    else
        SELECT_DEFAULT=1
    fi
    mirror_choice=$(select_one "镜像源" \
        "国内镜像（推荐）｜npm→npmmirror，Node→腾讯，apt→TUNA，底座→${DEFAULT_BASE_REGISTRY}" \
        "国内镜像（备选）｜npm→npmmirror，Node→腾讯，apt→TUNA，底座→${BASE_REGISTRY_ALT}" \
        "官方源｜npm→npmjs.org，Node→nodejs.org，apt→deb.debian.org，底座→Docker Hub")
    case "$mirror_choice" in
        1) MIRROR=yes; BASE_REGISTRY="$DEFAULT_BASE_REGISTRY"; BASE_REGISTRY_SET=1 ;;
        2) MIRROR=yes; BASE_REGISTRY="$BASE_REGISTRY_ALT";     BASE_REGISTRY_SET=1 ;;
        *) MIRROR=no ;;
    esac
elif [ -z "$MIRROR" ]; then
    # 仓库已经用 --base-registry 点名了，菜单里那三套会把答案盖掉；只问 npm/Node 这半。
    MIRROR="${PREV_MIRROR:-yes}"
fi

case "$MIRROR" in
    yes|y|Y|Yes|YES) MIRROR_MODE=yes; NPM_REGISTRY="$NPM_MIRROR"; NODE_DIST_MIRROR="$NODE_MIRROR_CN" ;;
    *)               MIRROR_MODE=no;  NPM_REGISTRY="https://registry.npmjs.org/"; NODE_DIST_MIRROR="$NODE_MIRROR_OFFICIAL" ;;
esac

# apt 源跟着镜像源走：国内模式带上 TUNA（`apt-get update` 那 ~10 MB 索引 + 几百 MB
# 包体全在那一步），官方模式清空、用发行版默认。显式给了 --apt-mirror / --no-apt-mirror
# 就不动它。上次记住的值在重建时优先，省得每次--rebuild 都要再写一遍。
if [ "$APT_MIRROR_SET" = 0 ]; then
    if [ "$MIRROR_MODE" = yes ]; then
        APT_MIRROR="${PREV_APT_MIRROR:-$APT_MIRROR_CN}"
    else
        APT_MIRROR=""
    fi
fi
APT_MIRROR="${APT_MIRROR%/}"

# 底座仓库的默认值：命令行 > 上次记住的 > 默认镜像站。
# 走官方源（--no-mirror）时把上次记住的仓库清掉，除非这次又显式给了——"官方源"就该
# 三个来源全是官方的，只清 npm 和 Node 会留下一个别人看不懂的不一致。
if [ "$BASE_REGISTRY_SET" = 0 ]; then
    if [ "$MIRROR_MODE" = yes ]; then
        BASE_REGISTRY="${PREV_BASE_REGISTRY:-$DEFAULT_BASE_REGISTRY}"
    else
        BASE_REGISTRY=""
    fi
fi

JAVA_OPTS="${JAVA_OPTS:-$DEFAULT_JAVA_OPTS}"
# 上次用过 --release，这次（比如 --rebuild）默认沿用：不沿用的话会静默掉回
# 0.1.0-dev，「同一套配置重建」就变成了「换了个版本」。--no-release 显式关掉。
# 底座：命令行 > 状态文件 > debian。空串也算"没指定"，这样 --rebuild 沿用上次的底座。
[ -n "$BASE" ] || BASE="${PREV_BASE:-debian}"
resolve_base "$BASE"

if [ "$RELEASE_SET" = 0 ] && [ "${PREV_RELEASE:-}" = "yes" ]; then
    RELEASE=1
fi
RELEASE_VERSION=""
if [ "$RELEASE" = 1 ]; then
    RELEASE_VERSION=$(last_tag)
    RELEASE_VERSION=${RELEASE_VERSION#v}
fi

# 路径归一：宿主路径去掉尾斜杠；none / - 表示不挂
case "$WORKSPACE" in
    none|no|-|NONE|None) WORKSPACE="" ;;
esac
case "$MC" in
    none|no|-|NONE|None) MC="" ;;
esac
case "$DATA" in
    volume:*) ;;
    *)
        while [ "$DATA" != "/" ] && [ "${DATA%/}" != "$DATA" ]; do DATA="${DATA%/}"; done
        ;;
esac
if [ -n "$WORKSPACE" ]; then
    case "$WORKSPACE" in
        volume:*) ;;
        *) while [ "$WORKSPACE" != "/" ] && [ "${WORKSPACE%/}" != "$WORKSPACE" ]; do WORKSPACE="${WORKSPACE%/}"; done ;;
    esac
fi
if [ -n "$MC" ]; then
    case "$MC" in
        volume:*) ;;
        *) while [ "$MC" != "/" ] && [ "${MC%/}" != "$MC" ]; do MC="${MC%/}"; done ;;
    esac
fi
[ -n "$DATA" ] || die "数据目录不能为空"
[ -n "$NAME" ] || die "容器名不能为空"

# MC 端口：现在校验并归一成逗号列表。不合格在这里停，别等 docker run 吐英文。
MC_PORT_LIST=$(mc_ports_checked "$MC_PORTS")
if [ -n "$MC_PORT_LIST" ]; then
    MC_PORTS=$(printf '%s\n' "$MC_PORT_LIST" | paste -sd, -)
fi

# docker 的容器名/卷名只允许这些字符；提前拦，别等 docker 报一串英文
case "$NAME" in
    *[!a-zA-Z0-9_.-]*) die "容器名只能用字母、数字、下划线、点、短横线：$NAME" ;;
esac
case "$DATA" in
    volume:*)
        vol="${DATA#volume:}"
        case "$vol" in
            ''|*[!a-zA-Z0-9_.-]*) die "卷名只能用字母、数字、下划线、点、短横线：$vol" ;;
        esac
        ;;
esac
case "$WORKSPACE" in
    volume:*)
        vol="${WORKSPACE#volume:}"
        case "$vol" in
            ''|*[!a-zA-Z0-9_.-]*) die "卷名只能用字母、数字、下划线、点、短横线：$vol" ;;
        esac
        ;;
esac
case "$MC" in
    volume:*)
        vol="${MC#volume:}"
        case "$vol" in
            ''|*[!a-zA-Z0-9_.-]*) die "卷名只能用字母、数字、下划线、点、短横线：$vol" ;;
        esac
        ;;
esac

case "$PORT" in
    ''|*[!0-9]*) die "端口必须是数字：$PORT" ;;
esac
[ "$PORT" -ge 1 ] && [ "$PORT" -le 65535 ] || die "端口超出范围：$PORT"

if [ -z "$IMAGE" ]; then
    # --no-build / --recreate 想要的是「上次那个镜像」；没有记录时才落回新时间戳的默认名
    if [ "$NO_BUILD" = 1 ] && [ -n "${PREV_IMAGE:-}" ]; then
        IMAGE="$PREV_IMAGE"
    else
        IMAGE=$(default_image)
    fi
fi

# 同名容器的状态要在**打印摘要之前**拿到：`--restart` 不动镜像，而上面刚按时间戳
# 算出了一个并不存在的标签——摘要、完成信息和状态文件都得用容器实际在跑的那个，
# 否则下一次 `--no-build` 会拿着这个空名字去重建。
STATUS=$(container_status "$NAME")
if [ "$STATUS" != "missing" ] && [ "$ACTION" = "restart" ]; then
    running_image=$(docker inspect -f '{{.Config.Image}}' "$NAME" 2>/dev/null || true)
    running_image=$(printf '%s' "$running_image" | tr -d '[:space:]')
    [ -n "$running_image" ] && IMAGE="$running_image"
fi

# 数据路径：宿主路径要建出来；volume: 交给 docker
case "$DATA" in
    volume:*) say "  数据：命名卷 ${DATA#volume:}" ;;
    *)
        [ "$DRY_RUN" = 1 ] || mkdir -p "$DATA"
        say "  数据：$DATA"
        ;;
esac
if [ -n "$WORKSPACE" ]; then
    case "$WORKSPACE" in
        volume:*) say "  工作区：命名卷 ${WORKSPACE#volume:}" ;;
        *)
            [ "$DRY_RUN" = 1 ] || mkdir -p "$WORKSPACE"
            say "  工作区：$WORKSPACE"
            ;;
    esac
else
    say "  工作区：不挂（容器内，重建容器会丢）"
fi
if [ -n "$MC" ]; then
    case "$MC" in
        volume:*) say "  MC 服务器数据：命名卷 ${MC#volume:} → /home/hdsl/mc" ;;
        *)
            [ "$DRY_RUN" = 1 ] || mkdir -p "$MC"
            say "  MC 服务器数据：$MC → /home/hdsl/mc"
            ;;
    esac
else
    say "  MC 服务器数据：不挂（容器内，重建容器会丢存档）"
fi
if [ -n "$VIDEO" ] && [ "$VIDEO" != none ]; then
    case "$VIDEO" in
        volume:*) say "  转码媒体库：命名卷 ${VIDEO#volume:} → /home/hdsl/video" ;;
        *) say "  转码媒体库：$VIDEO → /home/hdsl/video" ;;
    esac
else
    say "  转码媒体库：不挂（容器内，重建容器会丢）"
fi
say "  硬件编码直通：$HW"
say "  端口：${BIND}:${PORT} → 容器 ${PORT}"
say "  MC 端口：$(mc_ports_summary)"
say "  底座：$BASE_IMAGE（$BASE_CODE）"
say "  镜像：$IMAGE"
if [ -n "$PREBUILT" ]; then
    say "  来源：拉取已发布的镜像（不本地构建）"
elif [ -n "$BASE_REGISTRY" ]; then
    say "  来源：本地构建，底座从 ${BASE_REGISTRY%/}/library 拉"
else
    say "  来源：本地构建"
fi
say "  镜像源：$([ "$MIRROR_MODE" = yes ] && echo '国内（npm→npmmirror，Node→腾讯）' || echo '官方（npmjs / nodejs.org）')"
if [ -n "$PREBUILT" ]; then
    :
elif [ -n "$APT_MIRROR" ]; then
    say "  apt 源：$APT_MIRROR（Debian/Ubuntu 底座）"
else
    say "  apt 源：发行版默认（deb.debian.org）$([ "$MIRROR_MODE" = yes ] && echo ' — 国内网络下这里最慢，用 --apt-mirror 换' || true)"
fi
say "  jar 版本：$([ "$RELEASE" = 1 ] && echo "发布 $RELEASE_VERSION" || echo '开发版 0.1.0-dev（--release 用最后的标签）')"
say "  口令：$([ -n "$PASSWORD" ] && echo '已设置' || echo '留空，打开网页走初始化引导')"

# 同名容器：没有指定动作时先问，别擅自覆盖
if [ "$STATUS" != "missing" ] && [ -z "$ACTION" ]; then
    say ""
    say "容器 $NAME 已经存在，状态：$STATUS"
    if [ "$ASSUME_YES" = 1 ]; then
        say "没有指定动作，什么都没做。要动它请加 --restart / --recreate / --rebuild。"
        exit 0
    fi
    say "  1) 只重启（不动镜像）"
    say "  2) 删掉重建容器（用现有镜像，不重新编译）"
    say "  3) 重新构建镜像后重建容器（会花几分钟）"
    say "  4) 退出"
    choice=$(ask "选一个" "4")
    case "$choice" in
        1) ACTION=restart ;;
        2) ACTION=recreate ;;
        3) ACTION=rebuild ;;
        *) say "退出，什么都没改。"; exit 0 ;;
    esac
fi

# 端口占用：只有真的要新建容器时才查；而且占着它的是自己的容器就放行
if [ "$ACTION" != "restart" ]; then
    port_ours=0
    if [ "$STATUS" = "running" ] && docker port "$NAME" 2>/dev/null | grep -qE "[:.]${PORT}\$"; then
        port_ours=1
    fi
    if [ "${port_ours:-0}" = 0 ] && port_in_use "$PORT"; then
        die "端口 $PORT 已被占用（换一个，或先停掉占用的进程）"
    fi
fi

if [ "$ASSUME_YES" = 0 ] && [ "$STATUS" = "missing" ]; then
    confirm "按上面的配置开始构建并启动？" || { say "已取消。"; exit 0; }
fi

case "$ACTION" in
    restart)
        if [ "$STATUS" = "missing" ]; then
            die "没有名为 $NAME 的容器，无法 --restart"
        fi
        if [ "$STATUS" != "running" ]; then
            step "启动现有容器"
            if [ "$DRY_RUN" = 1 ]; then say "  [dry-run] docker start $NAME"; else docker start "$NAME" >/dev/null; fi
        fi
        wait_healthy "$NAME"
        ;;
    recreate|rebuild)
        if [ "$ACTION" = "rebuild" ]; then
            build_image
        else
            # --recreate 的承诺是「用现有镜像，不重新编译」。容器还在就用它实际在跑的
            # 那个；容器被手动删了，就退回状态文件里记下的那个（仍在本地才算数）。
            reusable=""
            if [ "$STATUS" != "missing" ]; then
                reusable=$(docker inspect -f '{{.Config.Image}}' "$NAME" 2>/dev/null || true)
                reusable=$(printf '%s' "$reusable" | tr -d '[:space:]')
            fi
            if [ -z "$reusable" ] && [ -n "${PREV_IMAGE:-}" ] && image_exists "$PREV_IMAGE"; then
                reusable="$PREV_IMAGE"
            fi
            if [ -n "$reusable" ] && image_exists "$reusable"; then
                IMAGE="$reusable"
                say "  复用现有镜像：$IMAGE"
            else
                build_image
            fi
        fi
        stop_container
        remove_container
        run_container
        wait_healthy "$NAME"
        ;;
    *)
        if [ "$NO_BUILD" = 1 ] && image_exists "$IMAGE"; then
            say "  复用现有镜像：$IMAGE"
        else
            build_image
        fi
        if [ "$STATUS" = "missing" ]; then
            run_container
        fi
        wait_healthy "$NAME"
        ;;
esac

[ "$DRY_RUN" = 1 ] || save_state

scheme="http"
step "完成"
say "  容器：$NAME（$IMAGE）"
say "  地址：$scheme://127.0.0.1:$PORT/   局域网：$scheme://<本机IP>:$PORT/"
if [ -n "$MC" ]; then
    case "$MC" in
        volume:*) say "  MC 数据：卷 ${MC#volume:} → /home/hdsl/mc（存档与服务端程序都在这里）" ;;
        *) say "  MC 数据：$MC → /home/hdsl/mc（存档与服务端程序都在这里）" ;;
    esac
else
    say "  MC 数据：没挂（存档在容器内，重建容器会丢）"
fi
if [ -n "$VIDEO" ] && [ "$VIDEO" != none ]; then
    case "$VIDEO" in
        volume:*) say "  媒体库：卷 ${VIDEO#volume:} → /home/hdsl/video" ;;
        *) say "  媒体库：$VIDEO → /home/hdsl/video" ;;
    esac
else
    say "  媒体库：没挂（容器内，重建容器会丢）"
fi
say "  硬件编码：$HW"
say "  MC 端口：$(mc_ports_summary)"
if [ -n "$MC_PORT_LIST" ]; then
    say "            （宿主与容器同号；实例的 server-port 要与之一致，多实例就多开几个）"
fi
if [ -z "$PASSWORD" ]; then
    say "  首次打开会进入初始化引导，创建第一个管理员账号。"
fi
say ""
say "  常用操作："
say "    docker logs -f $NAME                      # 看日志"
say "    ./docker-install.sh --yes --restart       # 重启"
say "    ./docker-install.sh --yes --rebuild       # 拉了新代码后重建"
say "    docker stop $NAME && docker rm $NAME      # 拆掉（数据在 $DATA，不会删）"
say ""
if [ "$DRY_RUN" = 1 ]; then
    say "  （dry-run：没有构建、没有起容器，也没有写状态文件）"
else
    say "  配置记在 $STATE_FILE（口令在 $ENV_FILE，权限 600），重跑时就是默认值。"
fi