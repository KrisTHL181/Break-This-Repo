#!/usr/bin/env python3
"""移动端信息架构验收（headless Chromium 390x844）+ 桌面版式回归。

钉死的规则（回归时直接跑）：
  ① 实例列表页首屏可见 dsh 实例行 >= 3
  ② 第一个实例行的启动按钮零滚动可达（在视口内）
  ③ 品牌区是实例列表页内的**原地切换**（不是第二层页面）：点左侧「品牌实例」后
     URL 仍是 /instances（只换查询串）、`page-root` 不重挂载、内容区换成品牌视图；
     左下没有旧版挤在列表底部的品牌残留（无折叠摘要卡、无品牌安装行）
  ④ 品牌视图：页签行 4 个品牌（带实例数）、版本卡折叠 <= 90px、实例卡吃掉剩余高度
     （>= 400px）、首行实例的启动按钮零滚动可达——手机上一屏能完成「看实例 + 启动」
  ⑤ `/instances?brand=x` 与 `/brands/<品牌>` 旧链接都落到同一处的品牌视图
另有：行内版本选择器（已装/未装标记）、长说明一行降级、插件弹窗（真调后端）、
主页品牌卡跟随启动目标与其「管理」跳转、桌面端与桌面缩窄（细指针）两种路径。

跑法（需要本机 python playwright 与一个可登录的实例）：
  pip install playwright && playwright install chromium
  # 起一个有数据的 jar（建议 >=4 个 dsh 实例 + 1 个 kimi 实例）：
  HDSL_DATA=/tmp/data HDSL_PORT=3920 java -jar build/libs/hdsl-web-<ver>.jar &
  HDSL_BASE=http://127.0.0.1:3920 HDSL_USER=admin HDSL_PASS=*** HDSL_KIMI_ID=<kimi 实例 id> \
      python3 scripts/mobile-acceptance.py

不设 `HDSL_KIMI_ID` 就跳过所有与品牌实例相关的检查（6/6a/7/7b/7c）；真拉起实例的 7/7a
在**跑脚本的机器自己就是 kimi 进程**时需要留意（面板实例与 agent 同名）。

脚本用 web/dist 起静态服务并把 /api 反代到 HDSL_BASE——所以它验的是**当前工作区
的 dist**：先 `cd web && ./node_modules/.bin/vite build`（或整包 gradle build）。
静态服务必须像真服务器那样注入 `<base href="/">`（见 SpaHandler）：前端用相对路径发
API 请求，缺了它两级路径上的相对地址会解析到错误的父路径。
"""
import json
import os
import threading
import http.server
import socketserver

from playwright.sync_api import sync_playwright

DIST = os.path.join(os.path.dirname(__file__), "..", "web", "dist")
API = os.environ.get("HDSL_BASE", "http://127.0.0.1:3920")
USER = os.environ.get("HDSL_USER", "admin")
PASS = os.environ.get("HDSL_PASS", "testpass123")
KIMI_ID = os.environ.get("HDSL_KIMI_ID", "")  # 预建 kimi 实例 id；为空则跳过实例相关检查
BASE = "http://127.0.0.1:3910"


class SpaHandler(http.server.SimpleHTTPRequestHandler):
    """web/dist 的静态服务，只为验收脚本自足。

    必须像真服务器（`StaticServlet.indexHtml`）那样往 index.html 里注入
    `<base href="/">` + `window.__HDSL_BASE__`：前端用**相对**路径发 API 请求
    （见 `web/src/base.ts`），没有 base 标签时两级路径上的相对地址会解析到
    错误的父路径，页面看起来「一个实例都没有」。"""

    def __init__(self, *a, **kw):
        super().__init__(*a, directory=DIST, **kw)

    def send_head(self):
        import os.path
        p = os.path.join(DIST, self.path.lstrip("/").split("?")[0])
        if self.path != "/" and not os.path.isfile(p):
            self.path = "/index.html"
        if self.path.split("?")[0] in ("/", "/index.html"):
            with open(os.path.join(DIST, "index.html"), encoding="utf-8") as f:
                html = f.read()
            html = html.replace(
                "<head>",
                '<head>\n  <base href="/">\n  <script>window.__HDSL_BASE__="";</script>',
                1,
            )
            body = html.encode("utf-8")
            self.send_response(200)
            self.send_header("Content-Type", "text/html; charset=utf-8")
            self.send_header("Content-Length", str(len(body)))
            self.end_headers()
            self.wfile.write(body)
            return None
        return super().send_head()

    def log_message(self, *a):
        pass


class Q(socketserver.TCPServer):
    allow_reuse_address = True


srv = Q(("127.0.0.1", 3910), SpaHandler)
threading.Thread(target=srv.serve_forever, daemon=True).start()

results = []


def check(name, cond, detail=""):
    results.append((name, bool(cond)))
    print(("PASS " if cond else "FAIL ") + name + (f"  [{detail}]" if detail else ""))


def route_api(route, request):
    if request.url.startswith(BASE + "/api/"):
        route.continue_(url=request.url.replace(BASE, API, 1))
    else:
        route.continue_()


def h(pg, selector):
    return pg.evaluate(
        """(s) => { const e = document.querySelector(s); if (!e) return null;
             const r = e.getBoundingClientRect(); return {h: Math.round(r.height),
             top: Math.round(r.top), bottom: Math.round(r.bottom)}; }""",
        selector,
    )


# 品牌视图的体积：版本卡 = 实例卡的前一张卡（品牌是「版本」卡，zcode 是「发行包」卡，
# 两者都在实例卡之前）；页签行 = 内容区第一张 toolbar 卡。用结构找卡而不是写死第几个孩子，
# 也不靠按钮文案（zcode 没有「安装新版本」），版式微调时断言不会假红。
BRAND_BOXES = """() => {
  const body = document.querySelector('.list-body');
  const instancesCard = body ? body.closest('.card') : null;
  const prev = instancesCard ? instancesCard.previousElementSibling : null;
  const versionCard = prev && prev.classList.contains('card') ? prev : null;
  const tabRow = document.querySelector('.page-content .card.toolbar-row');
  return {
    version: versionCard ? Math.round(versionCard.getBoundingClientRect().height) : null,
    instances: instancesCard ? Math.round(instancesCard.getBoundingClientRect().height) : null,
    list: body ? Math.round(body.getBoundingClientRect().height) : null,
    tabs: tabRow ? [...tabRow.querySelectorAll('button')].map(b => b.textContent.replace(/\\s+/g, ' ').trim()) : [],
    warn: (document.querySelector('.hint.warning')?.textContent ?? '').replace(/\\s+/g, ' ').trim().slice(0, 24),
  };
}"""


with sync_playwright() as p:
    # --------------------------------------------- 移动端：实例列表页 + 原地切换 --
    b = p.chromium.launch()
    ctx = b.new_context(viewport={"width": 390, "height": 844}, has_touch=True)
    pg = ctx.new_page()
    pg.route("**/*", route_api)
    ctx.request.post(API + "/api/auth/login", data={"username": USER, "password": PASS})

    pg.goto(BASE + "/instances")
    pg.wait_for_timeout(1800)

    vis = pg.evaluate("""() => {
      const vh = window.innerHeight;
      const rows = [...document.querySelectorAll('.list-body')][0]?.querySelectorAll('.tlli') ?? [];
      return [...rows].filter(el => { const r = el.getBoundingClientRect(); return r.bottom > 0 && r.top < vh; }).length;
    }""")
    check("1 首屏可见 dsh 实例行 >= 3", vis >= 3, f"visible={vis}")

    btn = pg.evaluate("""() => {
      const row = [...document.querySelectorAll('.list-body')][0]?.querySelector('.tlli');
      if (!row) return null;
      const b = row.querySelector('.row-actions button').getBoundingClientRect();
      return {top: b.top, bottom: b.bottom, vh: window.innerHeight};
    }""")
    check("2 首行启动按钮零滚动可达", btn and btn["top"] >= 0 and btn["bottom"] <= btn["vh"], json.dumps(btn))

    # --------------------------------------------- 原地切换（核心）：不跳页、不重挂载 --
    pg.evaluate("() => { document.querySelector('.page-root').dataset.probe = 'alive'; }")
    pg.get_by_role("button", name="品牌实例").first.click()
    pg.wait_for_timeout(1600)
    inplace = pg.evaluate("""() => ({
      probe: document.querySelector('.page-root')?.dataset.probe ?? null,
      summary: !!document.querySelector('.brand-summary'),
      installRow: !!document.querySelector('.filter-bar'),
    })""")
    check("3 点「品牌实例」在原地换视图（URL 仍 /instances、页面不重挂载）",
          "/instances" in pg.url and "view=brands" in pg.url and inplace["probe"] == "alive",
          f"{pg.url} probe={inplace['probe']}")
    check("3a 没有旧版挤在列表底部的品牌残留（摘要卡/安装行）",
          not inplace["summary"] and not inplace["installRow"], json.dumps(inplace))

    boxes = pg.evaluate(BRAND_BOXES)
    check("4 品牌视图：页签行 4 个品牌 + 警示条",
          len(boxes["tabs"]) == 4 and len(boxes["warn"]) > 0, json.dumps(boxes, ensure_ascii=False))
    check("4a 版本卡折叠 <= 90px", boxes["version"] is not None and 0 < boxes["version"] <= 90,
          f"version={boxes['version']}")
    check("4b 实例卡吃剩余高度 >= 400px", boxes["instances"] is not None and boxes["instances"] >= 400,
          f"instances={boxes['instances']}")

    inst_btn = pg.evaluate("""() => {
      const row = document.querySelector('.list-body .tlli');
      if (!row) return null;
      const b = [...row.querySelectorAll('button')][0].getBoundingClientRect();
      return {top: Math.round(b.top), bottom: Math.round(b.bottom), vh: window.innerHeight};
    }""")
    check("4c 品牌实例行首个动作按钮零滚动可达",
          inst_btn is None or (inst_btn["top"] >= 0 and inst_btn["bottom"] <= inst_btn["vh"]),
          json.dumps(inst_btn))
    no_page_scroll = pg.evaluate("() => document.documentElement.scrollHeight <= window.innerHeight + 1")
    check("4d 品牌视图无页面级滚动（内部滚动）", no_page_scroll)
    pg.screenshot(path="/tmp/acc-mobile-brands.png")

    # 换品牌页签：只换内容区
    pg.get_by_role("button", name="Kimi Code").first.click()
    pg.wait_for_timeout(1500)
    check("5 换品牌页签仍在原地（查询串变 brand=kimi）",
          "brand=kimi" in pg.url and "view=brands" in pg.url and
          pg.evaluate("() => document.querySelector('.page-root')?.dataset.probe") == "alive", pg.url)
    check("5a 内容跟着换（警示条说的是 Kimi Code）",
          "Kimi Code" in pg.evaluate("() => document.querySelector('.hint.warning')?.textContent ?? ''"))

    # 切回 dsh 视图
    pg.get_by_role("button", name="instances").first.click()
    pg.wait_for_timeout(1500)
    back = pg.evaluate("""() => ({
      probe: document.querySelector('.page-root')?.dataset.probe ?? null,
      rows: document.querySelectorAll('.list-body .tlli').length,
    })""")
    check("5b 切回 dsh 实例列表（同页、行数不变）",
          "view=" not in pg.url and back["probe"] == "alive" and back["rows"] >= 3,
          f"{pg.url} {json.dumps(back)}")

    # 深链与旧链接
    pg.goto(BASE + "/instances?view=brands&brand=nope")
    pg.wait_for_timeout(1600)
    check("5c 非法品牌落到有实例的品牌（写进 URL）",
          "brand=nope" not in pg.url and "view=brands" in pg.url, pg.url)
    pg.goto(BASE + "/brands/kimi")
    pg.wait_for_timeout(1600)
    check("5d /brands/kimi 旧深链重定向到原地视图",
          "/instances" in pg.url and "view=brands" in pg.url and "brand=kimi" in pg.url, pg.url)

    # zcode 分支：长说明降级 + 两卡同样的高度规格
    pg.goto(BASE + "/instances?view=brands&brand=zcode")
    pg.wait_for_timeout(1800)
    note = pg.evaluate("""() => {
      const nc = document.querySelector('.note-collapse');
      return nc ? {h: Math.round(nc.getBoundingClientRect().height)} : null;
    }""")
    check("6 长说明折叠为一行 + 详情按钮（zcode 分支）", note and note["h"] <= 48, json.dumps(note))
    zbox = pg.evaluate(BRAND_BOXES)
    check("6a zcode 分支同为「版本卡折叠 + 实例卡吃高度」",
          zbox["version"] is not None and zbox["version"] <= 90
          and zbox["instances"] is not None and zbox["instances"] >= 400,
          json.dumps(zbox))

    if KIMI_ID:
        pg.goto(BASE + "/instances?view=brands&brand=kimi")
        pg.wait_for_timeout(1800)

        # 插件弹窗：真调后端（通道预留，supported=false）——只读，不碰进程
        pg.evaluate("""() => { const r = document.querySelector('.list-body .tlli');
            const btns = [...r.querySelectorAll('button')]; btns[btns.length - 1].click(); }""")
        pg.wait_for_timeout(600)
        pg.get_by_text("插件", exact=True).last.click()
        pg.wait_for_timeout(1200)
        dlg = pg.evaluate("""() => { const d = [...document.querySelectorAll('[role=dialog]')].pop();
            return d ? d.textContent.replace(/\\s+/g, ' ').trim() : null; }""")
        check("7 插件弹窗按后端响应渲染（暂不支持 + 通道预留）",
              dlg and ("暂不支持插件" in dlg) and ("预留" in dlg), str(dlg)[:60])
        pg.keyboard.press("Escape")
        pg.wait_for_timeout(400)

        # 行内版本选择器（全量目录 + 已装/未装标记）：只读
        ver_opts = pg.evaluate("""() => {
          const sel = document.querySelector('.list-body .tlli select');
          return sel ? [...sel.options].slice(0, 4).map(o => o.textContent.trim()) : null;
        }""")
        check("7a 行内版本选择器列出全量目录并标记已装/未装",
              ver_opts is not None
              and any("已装" in o for o in ver_opts) and any("未装" in o for o in ver_opts),
              json.dumps(ver_opts, ensure_ascii=False))

        # 主页品牌卡跟随启动目标 + 「管理」跳转（只读导航）
        pg.evaluate(f"() => localStorage.setItem('hdsl.instance.selected', '{KIMI_ID}')")
        pg.goto(BASE + "/")
        pg.wait_for_timeout(2000)
        card = pg.evaluate("""() => document.querySelector('.mobile-home')?.textContent.replace(/\\s+/g, ' ') ?? null""")
        check("7b 主页品牌卡跟随启动目标", card and "Kimi Code" in card, str(card)[:80])
        pg.get_by_role("button", name="管理").first.click()
        pg.wait_for_timeout(1500)
        check("7c 主页品牌卡「管理」落到实例列表页的品牌视图",
              "/instances" in pg.url and "view=brands" in pg.url and "brand=kimi" in pg.url, pg.url)

        # 行内启动/停止：真拉起实例。
        # 注意：如果跑本脚本的机器上 **agent 自己就是 `kimi` 进程**（真机验证时的常见情形），
        # 别用它跑这一条——面板拉起的实例与 agent 同名，排查成本远大于收益。
        pg.goto(BASE + "/instances?view=brands&brand=kimi")
        pg.wait_for_timeout(1500)
        pg.evaluate("""() => { document.querySelector('.list-body .tlli button').click(); }""")
        try:
            pg.wait_for_function(
                """() => /运行中/.test(document.querySelector('.list-body .tlli')?.textContent ?? '')""",
                timeout=90000)
            launched = True
        except Exception:
            launched = False
        check("8 品牌实例行内启动真实拉起实例", launched)
        pg.evaluate("""() => { document.querySelector('.list-body .tlli button').click(); }""")
        try:
            pg.wait_for_function(
                """() => !/运行中|启动中/.test(document.querySelector('.list-body .tlli')?.textContent ?? '')""",
                timeout=90000)
            stopped = True
        except Exception:
            stopped = False
        check("8a 品牌实例行内停止真实停掉实例", stopped)
    else:
        print("SKIP 7-8（HDSL_KIMI_ID 未设置）")

    b.close()

    # ------------------------------------------------- 桌面：列表整屏 / 品牌视图 --
    b = p.chromium.launch()
    ctx = b.new_context(viewport={"width": 1440, "height": 900})
    pg = ctx.new_page()
    pg.route("**/*", route_api)
    ctx.request.post(API + "/api/auth/login", data={"username": USER, "password": PASS})

    pg.goto(BASE + "/instances")
    pg.wait_for_timeout(1800)
    desk = pg.evaluate("""() => ({ list: Math.round(document.querySelector('.list-body').getBoundingClientRect().height),
      summary: !!document.querySelector('.brand-summary'), filterRow: !!document.querySelector('.filter-bar'),
      cats: [...document.querySelectorAll('.sidebar-sub .side-category')].map(e => e.textContent.trim()),
      divider: (() => { const c = document.querySelector('.sidebar-sub .side-category');
        return c ? getComputedStyle(c).borderBottomWidth : null; })() })""")
    check("9 桌面实例列表吃整屏（>= 600px）且无品牌残留",
          desk["list"] >= 600 and not desk["summary"] and not desk["filterRow"], json.dumps(desk))
    check("9a 子侧栏两个分区（dsh 实例 / 第三方品牌）且带 1px 分割线",
          desk["cats"] == ["dsh 实例", "第三方品牌"] and desk["divider"] == "1px",
          json.dumps(desk, ensure_ascii=False))

    pg.goto(BASE + "/instances?view=brands&brand=kimi")
    pg.wait_for_timeout(1800)
    bk = pg.evaluate(BRAND_BOXES)
    bk_note = pg.evaluate("() => !!document.querySelector('.note-collapse')")
    check("10 桌面品牌视图：版本卡 <= 90px、实例列表 >= 300px、长说明原样",
          bk["version"] is not None and bk["version"] <= 90
          and bk["list"] is not None and bk["list"] >= 300 and not bk_note,
          json.dumps(bk, ensure_ascii=False))
    pg.screenshot(path="/tmp/acc-desktop-brands.png")
    b.close()

    # --------------------- 回归：桌面缩窄触发移动版式（细指针，非 coarse） ----
    # v0.3.3 的教训：移动样式曾误放进 (hover:none)+(pointer:coarse) 查询里，真机正常、
    # 桌面缩窄时退化。两种指针路径都要钉：这里钉子侧栏横排与品牌视图的两卡高度。
    b = p.chromium.launch()
    ctx = b.new_context(viewport={"width": 500, "height": 900}, has_touch=False)
    pg = ctx.new_page()
    pg.route("**/*", route_api)
    ctx.request.post(API + "/api/auth/login", data={"username": USER, "password": PASS})
    pg.goto(BASE + "/instances?view=brands&brand=kimi")
    pg.wait_for_timeout(1800)
    fine = pg.evaluate("""() => {
      const nav = document.querySelector('.sidebar-sub');
      const btn = [...document.querySelectorAll('button')].find(b => b.textContent.includes('安装新版本'));
      const card = btn ? btn.closest('.card') : null;
      const sel = document.querySelector('.list-body .tlli select');
      return { dir: getComputedStyle(nav).flexDirection,
               version: card ? Math.round(card.getBoundingClientRect().height) : null,
               hasSelect: !!sel };
    }""")
    check("11 桌面缩窄（细指针）：子侧栏仍横排、版本卡折叠单行、行内选择器在",
          fine["dir"] == "row" and fine["version"] is not None and fine["version"] <= 90 and fine["hasSelect"],
          json.dumps(fine))
    b.close()

srv.shutdown()
fails = [n for n, ok in results if not ok]
print("\n==>", "ALL PASS" if not fails else f"FAILED: {fails}")
raise SystemExit(1 if fails else 0)