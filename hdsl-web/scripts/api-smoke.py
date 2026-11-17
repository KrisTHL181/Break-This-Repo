#!/usr/bin/env python3
"""API 级冒烟：不点浏览器、不装包，只调接口把"一个人会点到的路径"跑一遍。

为什么要它：这一轮 Windows 裸部署的 bug 大多**不用点浏览器就能发现**——`/api/node` 少了
`system` 键、品牌版本因为证书失败静默变空、ZCode 启动写裸 `node`、dsh 的半安装树被当成
已安装…… 这些在接口层面各有一次明确的回答，只是以前没人把它们逐个念一遍。

它自己起一个临时面板（临时数据目录、随机端口、HTTP），做完删掉，不碰任何真实数据：

    python scripts/api-smoke.py [jar路径]

不给 jar 就取 `build/libs/` 里最新的那个（先 `./gradlew shadowJar`）。

`HDSL_TRUST_FORWARDED_PROTO=true` 让它相信 `X-Forwarded-Proto: https`，于是终端那四道
闸（会话 / HTTPS / 开发者密码 / 提权）可以在 HTTP 上一路验到底，不必真配证书。

退出码非 0 = 有检查失败。脚本末尾会列出失败项。
"""
import glob
import json
import os
import re
import shutil
import socket
import subprocess
import sys
import tempfile
import time
import urllib.error
import urllib.request
import http.cookiejar

PASSWORD = "api-smoke-pass-1"
DEV_PASSWORD = "api-smoke-dev-1"
ACK = "我确认这是我自己的网络代理"

results = []


def free_port():
    with socket.socket() as sock:
        sock.bind(("127.0.0.1", 0))
        return sock.getsockname()[1]


def newest_jar():
    candidates = sorted(glob.glob(os.path.join("build", "libs", "hdsl-web-*.jar")),
                        key=os.path.getmtime, reverse=True)
    return candidates[0] if candidates else None


class Panel:
    def __init__(self, jar):
        self.jar = jar
        self.data = tempfile.mkdtemp(prefix="hdsl-smoke-data-")
        self.port = free_port()
        self.log = os.path.join(self.data, "panel.log")
        self.base = "http://127.0.0.1:%d" % self.port
        self.process = None
        self.jar_handle = open(self.log, "wb")

    def start(self):
        environment = dict(os.environ)
        environment.update({
            "HDSL_DATA": self.data,
            "HDSL_PORT": str(self.port),
            "HDSL_ADMIN_PASSWORD": PASSWORD,
            "HDSL_TRUST_FORWARDED_PROTO": "true",
        })
        self.process = subprocess.Popen(["java", "-jar", self.jar], env=environment,
                                        stdout=self.jar_handle, stderr=subprocess.STDOUT)
        deadline = time.time() + 90
        while time.time() < deadline:
            try:
                with urllib.request.urlopen(self.base + "/api/health", timeout=2) as response:
                    if response.status == 200:
                        return
            except Exception:
                pass
            if self.process.poll() is not None:
                raise SystemExit("the panel exited during startup; see " + self.log)
            time.sleep(0.5)
        raise SystemExit("the panel did not answer /api/health within 90s; see " + self.log)

    def stop(self):
        if self.process and self.process.poll() is None:
            self.process.terminate()
            try:
                self.process.wait(timeout=20)
            except subprocess.TimeoutExpired:
                self.process.kill()
        self.jar_handle.close()
        # A failed check is worth a log tail; a clean run deletes the directory.
        if any(not ok for _, ok, _ in results):
            print("\n--- panel log tail ---")
            with open(self.log, encoding="utf-8", errors="replace") as log:
                for line in log.readlines()[-15:]:
                    print(line.rstrip())
            print("(full log: %s)" % self.log)
        else:
            shutil.rmtree(self.data, ignore_errors=True)


def main():
    jar = sys.argv[1] if len(sys.argv) > 1 else newest_jar()
    if not jar or not os.path.isfile(jar):
        raise SystemExit("no jar: build one with `./gradlew shadowJar`")

    panel = Panel(jar)
    panel.start()
    print("panel: %s  jar: %s\n" % (panel.base, jar))
    try:
        run(panel)
    finally:
        panel.stop()

    failed = [name for name, ok, _ in results if not ok]
    print("\n%d checks, %d failed" % (len(results), len(failed)))
    for name in failed:
        print("  FAILED: " + name)
    return 1 if failed else 0


def run(panel):
    jar = http.cookiejar.CookieJar()
    opener = urllib.request.build_opener(urllib.request.HTTPCookieProcessor(jar))

    def call(method, path, body=None, https=False, raw=False):
        data = None if body is None else json.dumps(body).encode("utf-8")
        headers = {"Content-Type": "application/json"}
        if https:
            headers["X-Forwarded-Proto"] = "https"
        request = urllib.request.Request(panel.base + path, data=data, method=method, headers=headers)
        try:
            with opener.open(request, timeout=120) as response:
                payload = response.read()
                return response.status, payload if raw else json.loads(payload or b"{}")
        except urllib.error.HTTPError as error:
            payload = error.read()
            if raw:
                return error.code, payload
            try:
                return error.code, json.loads(payload or b"{}")
            except Exception:
                return error.code, {"error": payload.decode("utf-8", "replace")[:300]}

    def check(name, ok, detail=""):
        results.append((name, bool(ok), detail))
        print(("PASS  " if ok else "FAIL  ") + name + ("" if ok else "   <- " + str(detail)[:400]))

    # ---- 会话 ------------------------------------------------------------
    status, body = call("POST", "/api/auth/login", {"username": "admin", "password": PASSWORD})
    check("auth: login with the seeded admin password", status == 200, (status, body))
    status, body = call("GET", "/api/health")
    check("health", status == 200 and body.get("status") == "ok", (status, body))

    # ---- 无 JS 保底页 ----------------------------------------------------
    status, body = call("GET", "/plain", raw=True)
    check("plain: the no-JavaScript surface renders forms",
          status == 200 and b"<h1>" in body and b"<form" in body, (status, body[:200]))
    status, body = call("GET", "/plain/old", raw=True)
    check("plain: the old-browser page explains the fallback",
          status == 200 and "浏览器太旧".encode("utf-8") in body and b"/plain" in body,
          (status, body[:200]))

    # ---- 平台声明 --------------------------------------------------------
    status, body = call("GET", "/api/system/platform")
    level = body.get("level")
    check("platform: os + level + items",
          status == 200 and body.get("os") and level in ("supported", "limited", "unsupported")
          and isinstance(body.get("items"), list), (status, body))
    if level == "supported":
        check("platform: a supported platform carries no gap notes", not body.get("items"), body.get("items"))
    else:
        check("platform: a non-supported platform names its gaps",
              len(body.get("items") or []) >= 2, body.get("items"))

    # ---- 工具链 ----------------------------------------------------------
    status, body = call("GET", "/api/node")
    check("node: `system` is present even when null (a dropped key whitened the page)",
          status == 200 and "system" in body and "managed" in body, (status, body))
    check("node: the managed runtimes are a list", isinstance(body.get("managed"), list), body.get("managed"))

    for brand in ("claude-web", "octop"):
        status, body = call("GET", "/api/brands/%s/versions" % brand)
        versions = body.get("versions") or []
        # Either the list arrives, or the answer explains why it cannot (a proxy's
        # certificate). An empty list with no reason is the failure this pins.
        explained = status == 409 and "证书" in (body.get("error") or "")
        check("brands: %s versions, or a reason" % brand,
              (status == 200 and versions) or explained, (status, body.get("error"), versions[:3]))
        if status == 200 and versions:
            # Only the shape is the contract: pinning a major here means every upstream
            # release turns this check red for no reason (claude-web 0.5.0 did).
            check("brands: %s versions look like versions" % brand,
                  re.match(r"^\d+\.\d+", versions[0]) is not None, versions[:3])

    status, body = call("GET", "/api/doctor")
    checks = {c.get("id"): c for c in body.get("checks", [])}
    windows = (checks.get("windows") or {}).get("detail", "")
    check("doctor: the instance terminal section matches the platform",
          ("instance terminal" in windows) == (os.name == "nt"), windows[-160:])

    # ---- 出站证书的确认（一次性窗口） ------------------------------------
    status, body = call("GET", "/api/network/trust")
    check("trust: strict by default", status == 200 and body.get("mode") == "strict", (status, body))
    status, _ = call("POST", "/api/network/trust", {"minutes": 5, "acknowledge": "yes"})
    check("trust: a wrong acknowledgement is refused", status == 400, status)
    status, body = call("POST", "/api/network/trust", {"minutes": 5, "acknowledge": ACK})
    check("trust: a typed acknowledgement opens the window",
          status == 200 and body.get("mode") == "accept_unverified", (status, body))
    status, body = call("DELETE", "/api/network/trust")
    check("trust: revoking closes it", status == 200 and body.get("mode") == "strict", (status, body))

    # ---- ZCode：工作区 ---------------------------------------------------
    project = os.path.join(panel.data, "my-project")
    status, body = call("POST", "/api/zcode/instances", {"name": "smoke-zcode", "workspacePath": project})
    instance = body.get("instance") or {}
    zcode_id = instance.get("id")
    check("zcode: create with a chosen workspace", status == 201 and zcode_id, (status, body))
    check("zcode: the workspace is recorded and created",
          instance.get("workspacePath") == project and os.path.isdir(project), instance)
    status, _ = call("POST", "/api/zcode/instances", {"name": "bad", "workspacePath": "relative/x"})
    check("zcode: a relative workspace is refused", status == 400, status)

    # 创建时不填工作区：面板自己在 <数据目录>/zcode/workspaces 下建好一个并报回来
    workspaces_root = os.path.join(panel.data, "zcode", "workspaces")
    status, body = call("POST", "/api/zcode/instances", {"name": "smoke-default-ws"})
    default_instance = body.get("instance") or {}
    default_ws = default_instance.get("workspacePath") or ""
    check("zcode: creating without a path makes a workspace",
          status == 201 and os.path.dirname(default_ws) == workspaces_root
          and os.path.isdir(default_ws), (status, default_ws))
    status, body = call("GET", "/api/zcode/instances")
    check("zcode: the listing advertises the workspaces root",
          status == 200 and body.get("workspacesRoot") == workspaces_root,
          (status, body.get("workspacesRoot")))
    if default_instance.get("id"):
        status, _ = call("DELETE", "/api/zcode/instances/%s" % default_instance["id"])
        # 那是项目目录，不是实例状态：删实例不该把 agent 干过活的目录一起删掉
        check("zcode: deleting the instance keeps its default workspace",
              status in (200, 204) and os.path.isdir(default_ws),
              (status, os.path.isdir(default_ws)))
    if zcode_id:
        status, body = call("POST", "/api/zcode/instances/%s/launch" % zcode_id)
        text = json.dumps(body, ensure_ascii=False)
        # v0.6.88 起提示语会指明是哪个版本没构建；两种措辞都算“说清楚了”。
        check("zcode: launching without a distribution says so",
              status in (400, 409) and ("distribution" in text or "还没有构建" in text),
              (status, text))
        status, _ = call("DELETE", "/api/zcode/instances/%s" % zcode_id)
        check("zcode: delete", status in (200, 204), status)

    # ---- dsh：半安装的树 -------------------------------------------------
    instance_id = "smoke-dsh"
    root = os.path.join(panel.data, "hdsl", "instances", instance_id)
    tree = os.path.join(root, "dsh")
    package = os.path.join(tree, "node_modules", "@deepseek-ai", "dsh", "lib")
    os.makedirs(package, exist_ok=True)
    open(os.path.join(package, "bin.js"), "w").close()
    os.makedirs(os.path.join(tree, "node_modules", ".pnpm", "zod@9.9.9", "node_modules", "zod"),
                exist_ok=True)
    with open(os.path.join(tree, "package.json"), "w", encoding="utf-8") as out:
        out.write('{"private":true,"dependencies":{"@deepseek-ai/dsh":"9.9.9"}}')
    with open(os.path.join(root, "instance.json"), "w", encoding="utf-8") as out:
        json.dump({"id": instance_id, "name": "smoke dsh", "version": "9.9.9", "profile": "web",
                   "workspace": os.path.join(root, "workspace"), "nodeRuntime": "system",
                   "homeMode": "isolated", "arguments": [], "environment": {}, "icon": "default",
                   "portMode": "auto", "port": 4999, "createdAt": int(time.time() * 1000)},
                  out, ensure_ascii=False)

    # 半安装的树不再"拒绝后干等"：启动请求应当落到既有的"先装再启动"那条路（202 + taskId）。
    # 用 9.9.9 这个不存在的版本，安装本身会失败——这里验的是**它去装了**，不是装成功。
    status, body = call("POST", "/api/instances/%s/launch" % instance_id, {})
    check("dsh: a half-installed tree triggers a reinstall instead of a refusal",
          status == 202 and body.get("state") == "installing" and body.get("taskId"),
          (status, body))

    status, body = call("GET", "/api/instances/%s/tui-plugins" % instance_id)
    plugins = body.get("plugins") or []
    check("dsh: the curated terminal plugins are listed", status == 200 and plugins, plugins[:1])
    inproc = next((p for p in plugins if p.get("packageName") == "@nas200-timmy/dsh-inproc-tui"), None)
    check("dsh: a harness version outside the plugin's range is not called a fit",
          inproc is not None and inproc.get("fitsHost") is False, inproc)
    status, body = call("POST", "/api/instances/%s/tui-plugins/install" % instance_id,
                        {"package": "@nas200-timmy/dsh-inproc-tui"})
    text = json.dumps(body, ensure_ascii=False)
    check("dsh: the bundled plugin tarball is readable", "读取自带插件" not in text, (status, text))

    # ---- 终端四道闸（用可信的 X-Forwarded-Proto 模拟 TLS） -----------------
    status, body = call("GET", "/api/developer/status", https=True)
    check("terminal: the panel sees TLS behind the trusted header",
          status == 200 and body.get("https") is True, (status, body))
    status, body = call("POST", "/api/instances/%s/terminal" % instance_id, {}, https=True)
    text = json.dumps(body, ensure_ascii=False)
    check("terminal: a developer password is demanded first",
          status in (403, 409) and ("developer_password_not_set" in text
                                    or "developer_elevation_required" in text), (status, text))
    status, _ = call("POST", "/api/developer/password",
                     {"adminPassword": PASSWORD, "password": DEV_PASSWORD, "confirm": DEV_PASSWORD},
                     https=True)
    check("developer: the password can be set", status == 200, status)
    status, body = call("POST", "/api/instances/%s/terminal" % instance_id, {}, https=True)
    check("terminal: elevation is demanded next",
          status == 403 and "developer_elevation_required" in json.dumps(body), (status, body))
    status, body = call("POST", "/api/developer/elevate", {"password": DEV_PASSWORD}, https=True)
    check("developer: elevate", status == 200 and body.get("elevated") is True, (status, body))
    status, body = call("POST", "/api/instances/%s/terminal" % instance_id, {}, https=True)
    text = json.dumps(body, ensure_ascii=False)
    check("terminal: with every gate passed the refusal is about the instance",
          status == 409 and ("TUI" in text or "terminal_failed" in text), (status, text))
    status, body = call("POST", "/api/instances/%s/terminal" % instance_id, {})
    check("terminal: plain HTTP is refused before anything else",
          status == 403 and "insecure_transport" in json.dumps(body), (status, body))

    # ---- 其余表面 --------------------------------------------------------
    status, body = call("GET", "/api/instances")
    check("instances: list", status == 200 and isinstance(body.get("instances"), list), (status, body))
    status, body = call("POST", "/api/brands/kimi/instances/no-such-instance/launch", {})
    check("brands: launching an unknown instance is a 404", status == 404, (status, body))
    status, body = call("GET", "/api/settings/registry")
    check("settings: the download source is readable", status == 200, (status, body))
    status, _ = call("GET", "/api/settings/logs")
    check("settings: log retention is readable", status == 200, status)
    status, body = call("GET", "/api/developer/status")
    check("developer: status", status == 200 and "webShellEnabled" in body, (status, body))

    status, payload = call("GET", "/", raw=True)
    page = payload.decode("utf-8", "replace")
    check("spa: index.html references a built bundle", status == 200 and "/assets/index-" in page, status)
    if "/assets/" in page:
        bundle = page.split("/assets/")[1].split('"')[0]
        status, _ = call("GET", "/assets/" + bundle, raw=True)
        check("spa: the bundle is served", status == 200, (status, bundle))


if __name__ == "__main__":
    sys.exit(main())
