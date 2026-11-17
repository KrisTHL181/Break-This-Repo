#!/bin/sh
# 容器里的身份划分：以 agent 的身份跑，确认它碰不到面板的秘密。
#
# 用法（在宿主上）：
#   docker exec -u 1000 hdsl-web sh /scripts/verify-privilege-split.sh
# 或者脚本已挂进容器时：
#   docker exec -u 1000 hdsl-web /scripts/verify-privilege-split.sh
#
# 以 root 跑是**错误用法**：root 当然什么都能改，那样验的是"root 能改"，
# 不是这次要证明的"agent 不能改"。脚本会拒绝。
set -u

fail=0
ok()   { printf 'PASS  %s\n' "$*"; }
bad()  { printf 'FAIL  %s\n' "$*"; fail=1; }

if [ "$(id -u)" = "0" ]; then
    echo "以 root 运行没有意义：请用 docker exec -u 1000 … 跑这个脚本" >&2
    exit 2
fi

echo "uid=$(id -u) gid=$(id -g) $(id -un 2>/dev/null || true)"
echo

# 1. 秘密：写不进去
for path in /data/auth /data/logs; do
    if [ -d "$path" ]; then
        if touch "$path/.hdsl-probe" 2>/dev/null; then
            rm -f "$path/.hdsl-probe" 2>/dev/null
            bad "$path 可写（agent 能改面板的秘密）"
        else
            ok "$path 不可写"
        fi
    else
        echo "跳过  $path（还不存在）"
    fi
done

# 2. 秘密：读不出来
for path in /data/auth/developer.json /data/server.yaml /data/certs; do
    if [ -e "$path" ]; then
        if cat "$path" >/dev/null 2>&1 || ls "$path" >/dev/null 2>&1; then
            bad "$path 可读（面板的口令/证书暴露给 agent）"
        else
            ok "$path 不可读"
        fi
    else
        echo "跳过  $path（还不存在）"
    fi
done

# 3. 实例子目录：agent 必须还能写，否则 dsh/pnpm 全废
instance_root=/data/hdsl/instances
if [ -d "$instance_root" ]; then
    first=$(find "$instance_root" -mindepth 1 -maxdepth 1 -type d 2>/dev/null | head -1)
    if [ -n "$first" ]; then
        if touch "$first/.hdsl-probe" 2>/dev/null; then
            rm -f "$first/.hdsl-probe" 2>/dev/null
            ok "实例目录可写（$(basename "$first")）"
        else
            bad "实例目录不可写：agent 装不了包、起不了服务（$first）"
        fi
    else
        echo "跳过  实例目录是空的（先建一个实例再跑）"
    fi
else
    echo "跳过  $instance_root 不存在"
fi

# 4. pnpm store 必须可写且和实例同 fs，否则硬链接退化成复制
if [ -d /data/pnpm-store ]; then
    if touch /data/pnpm-store/.hdsl-probe 2>/dev/null; then
        rm -f /data/pnpm-store/.hdsl-probe 2>/dev/null
        ok "pnpm store 可写"
    else
        bad "pnpm store 不可写：实例装包会失败"
    fi
else
    echo "跳过  /data/pnpm-store 还不存在"
fi

# 5. 品牌实例的 home/workspace 必须归 agent 自己
#    Hermes 的 gateway 发现会拒绝 root 拥有的 control path（unsafe_control_path），
#    症状是聊天面板一直 "retrying in 30s" 而消息留在队列里。别的品牌不检查属主，
#    但同样不该是 root——它们只是恰好落在 setgid 的 /data 上才还能写。
brand_homes=$(find /data/brands -mindepth 4 -maxdepth 4 -type d \( -name home -o -name workspace \) 2>/dev/null | head -20)
if [ -n "$brand_homes" ]; then
    for dir in $brand_homes; do
        owner=$(stat -c '%u' "$dir" 2>/dev/null || echo '?')
        if [ "$owner" = "$(id -u)" ]; then
            ok "品牌实例目录属主正确（$dir）"
        else
            bad "品牌实例目录属主是 $owner，应为 $(id -u)：Hermes 会拒绝这种 control path（$dir）"
        fi
    done
else
    echo "跳过  还没有品牌实例（先建一个再跑）"
fi

# 6. ZCode 实例的 home/ 必须归 agent，并且在数据卷上
#    会话库跟着 homedir() 走，只有 HOME 能把它挪出容器的可写层。归 root 的话
#    agent 写不了库，看起来就是"发了消息没反应"；落在 /home/hdsl 的话换镜像就丢。
zcode_homes=$(find /data/zcode/instances -mindepth 2 -maxdepth 2 -type d -name home 2>/dev/null | head -20)
if [ -n "$zcode_homes" ]; then
    for dir in $zcode_homes; do
        owner=$(stat -c '%u' "$dir" 2>/dev/null || echo '?')
        if [ "$owner" != "$(id -u)" ]; then
            bad "ZCode 实例 home 属主是 $owner，应为 $(id -u)：agent 写不了会话库（$dir）"
        elif touch "$dir/.hdsl-probe" 2>/dev/null; then
            rm -f "$dir/.hdsl-probe" 2>/dev/null
            ok "ZCode 实例 home 可写（$dir）"
        else
            bad "ZCode 实例 home 不可写：会话与日志都落不下来（$dir）"
        fi
    done
else
    echo "跳过  还没有 ZCode 实例（先建一个再跑）"
fi

# 7. 面板 home 下不该再有 zcode 的会话库——有就说明有实例还在用可写层
if [ -f /home/hdsl/.zcode/cli/db/db.sqlite ]; then
    echo "注意  /home/hdsl/.zcode/cli/db 还在：换成 0.6.51 之前写的会话在这层上，重建即丢"
fi

# 8. 一张表：agent 的进程要写、而面板（root）创建的所有目录
#
#    这是同一个 bug 形状的完整清单——panel 建、agent 写。数据卷是 setgid 2775 时它被
#    挂载属性盖住（root 建的子目录继承 hdsl 组 + 0775），一旦 /data 换成普通 bind mount
#    （root:root 0755），每一条都会变成"进程起来就死、一个字都不输出"：
#    Octop 打不开 octop.db、MC 建不出 logs/、dsh 装不进 pnpm、品牌装不进 releases/。
#
#    注意 setgid 只在 `/data` 这一层：面板的 prepareAgentDirectory 用 0775，会把
#    `instances/<id>` 与 `home` 上的 setgid 位清掉，所以 home 里的子目录早就退回
#    root:root——home 内的污染必须靠下面的 8b 才看得见。
#
#    脚本本身就是以 agent 身份跑的（docker exec -u 1000），所以 touch 就是最终裁决。
check_agent_tree() {   # <名字> <glob>
    local label="$1" pattern="$2" found=0 dir owner
    for dir in $(find $pattern -maxdepth 0 2>/dev/null | head -20); do
        found=1
        if [ ! -d "$dir" ]; then
            continue
        fi
        owner=$(stat -c '%u' "$dir" 2>/dev/null || echo '?')
        if [ "$owner" != "$(id -u)" ]; then
            bad "$label 属主是 $owner，应为 $(id -u)：$dir"
        elif touch "$dir/.hdsl-probe" 2>/dev/null; then
            rm -f "$dir/.hdsl-probe" 2>/dev/null
            ok "$label 可写（$dir）"
        else
            bad "$label 不可写：$dir"
        fi
    done
    if [ "$found" = 0 ]; then
        echo "跳过  还没有 $label（先建一个再跑）"
    fi
}

check_agent_tree "dsh 实例目录"   "/data/hdsl/instances/*"
check_agent_tree "dsh 实例 home"  "/data/hdsl/instances/*/home"
check_agent_tree "ZCode 实例 data" "/data/zcode/instances/*/data"
check_agent_tree "MC 实例目录"    "/data/minecraft/instances/*"

# 8b. 递归：home 里任何一条非 agent 属主的条目，都会让 harness 写不了"自己那一半"。
#     cordis.yml 就写在面板留下的 cordis.patch.yml 旁边，父目录 root 0755 就是 EACCES。
#     自愈发生在同一实例的下一次启动（DshLauncher#prepareInstanceDirectories），所以这里
#     报 FAIL 的意思通常是"还没重启过"，而不是"永远修不好"。
for home in $(find /data/hdsl/instances -mindepth 2 -maxdepth 2 -type d -name home 2>/dev/null | head -20); do
    foreign=$(find "$home" ! -uid "$(id -u)" -print -quit 2>/dev/null)
    if [ -n "$foreign" ]; then
        bad "dsh 实例 home 里有非 agent 属主的条目：$foreign"
    else
        ok "dsh 实例 home 整树归 agent（$home）"
    fi
done

# 8c. 托管运行时反过来：agent 只读执行，能写反而是横向风险（可篡改所有实例共用的
#     node/pnpm）。root 属主是设计，不是漏交接——所以这里断言"不可写"。
for dir in /data/hdsl/runtimes/*; do
    [ -d "$dir" ] || continue
    if touch "$dir/.hdsl-probe" 2>/dev/null; then
        rm -f "$dir/.hdsl-probe" 2>/dev/null
        bad "agent 能写托管运行时（应为只读）：$dir"
    else
        ok "托管运行时只读（$dir）"
    fi
done

# 品牌：releases 里每个版本都是 pnpm 以 agent 身份写进去的
brand_releases=$(find /data/brands -mindepth 3 -maxdepth 3 -type d -path '*/releases/*' 2>/dev/null | head -20)
if [ -n "$brand_releases" ]; then
    for dir in $brand_releases; do
        owner=$(stat -c '%u' "$dir" 2>/dev/null || echo '?')
        if [ "$owner" != "$(id -u)" ]; then
            bad "品牌版本目录属主是 $owner，应为 $(id -u)：$dir"
        else
            ok "品牌版本目录可写（$dir）"
        fi
    done
else
    echo "跳过  还没有品牌版本（先装一个再跑）"
fi

# 9. 共享缓存：pnpm store 与 corepack home 归 root 的话，agent 的安装会失败在
#    "EACCES" 或"又要下载一遍 pnpm"上
for dir in /data/pnpm-store /data/corepack; do
    if [ -d "$dir" ]; then
        owner=$(stat -c '%u' "$dir" 2>/dev/null || echo '?')
        if [ "$owner" != "$(id -u)" ]; then
            bad "共享缓存属主是 $owner，应为 $(id -u)：$dir"
        fi
    else
        echo "跳过  $dir 不存在"
    fi
done

echo
if [ "$fail" = 1 ]; then
    echo "结论：身份划分没有生效（或方向错了），见上面 FAIL 行"
    exit 1
fi
echo "结论：agent 改不动面板的秘密，但仍然能维护自己的实例"
exit 0