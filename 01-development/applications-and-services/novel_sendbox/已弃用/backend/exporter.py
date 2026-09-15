"""阶段 4 完本与导出：完结总结生成 + TXT/Markdown/EPUB 导出 + 工程文件 zip。
函数式风格，单文件。EPUB 用 zipfile 标准库生成（不引入 ebooklib）。"""
import io
import os
import re
import zipfile
import json
from datetime import datetime

import storage
import agents


# ---------- HTML/XHTML 转义 ----------

def _escape_html(text):
    """HTML 转义 5 个特殊字符。"""
    s = str(text) if text is not None else ""
    return (s.replace("&", "&amp;")
             .replace("<", "&lt;")
             .replace(">", "&gt;")
             .replace('"', "&quot;")
             .replace("'", "&apos;"))


def _sanitize_filename(name):
    """清理 Windows 文件名非法字符 < > : " / \\ | ? *"""
    return re.sub(r'[<>:"/\\|?*]', "_", str(name or "")).strip() or "untitled"


# ---------- 完结总结 ----------

def generate_finale_summary(pid):
    """调 agents.narrator_finale 生成完结总结。
    输入：chapters.json 摘要列表 + characters.json + events.json + foreshadows.json。
    LLM 失败/未配置时返回降级版本（代码统计字数 + 简单角色命运列表）。"""
    meta = storage.get_project(pid) or {}
    chapters = storage.load_data(pid, "chapters.json") or []
    characters = storage.load_data(pid, "characters.json") or []
    events = storage.load_data(pid, "events.json") or []
    foreshadows = storage.load_data(pid, "foreshadows.json") or []

    chapters_summary = [
        {"chapter": c.get("chapter"), "title": c.get("title", ""), "summary": c.get("summary", "")}
        for c in chapters
    ]

    cfg = storage.load_llm_config().get("narrator") or {}
    if cfg.get("model") and cfg.get("api_key"):
        try:
            return agents.narrator_finale(cfg, chapters_summary, characters, events, foreshadows, storage.load_prompt(pid, "narrator_finale"))
        except Exception as e:
            print(f"[WARNING] 叙事者完结总结失败，降级为代码统计: {e}")

    # 降级版本：代码统计 + 简单列表
    total = sum(c.get("word_count") or len(c.get("text", "")) for c in chapters)
    return {
        "total_word_count": total,
        "character_fates": [
            {"name": ch.get("name", ""), "fate": ch.get("current_emotion", "") or "未交代"}
            for ch in characters
        ],
        "foreshadow_recap": [
            {"foreshadow": f.get("foreshadow") or f.get("content") or f.get("description", ""),
             "status": f.get("status", "未回收")}
            for f in foreshadows
        ],
        "timeline": [
            {"chapter": ev.get("chapter"), "event": ev.get("description", "")}
            for ev in events
        ],
    }


# ---------- TXT ----------

def export_txt(pid):
    """导出 TXT：标题 + 完结总结（如有）+ 各章正文。返回 bytes。"""
    meta = storage.get_project(pid) or {}
    chapters = storage.load_data(pid, "chapters.json") or []
    summary = meta.get("finale_summary")

    parts = [f"《{meta.get('title', '')}》"]
    if summary:
        parts.append("")
        parts.append("【完结总结】")
        parts.append(f"全书字数：{summary.get('total_word_count', 0)}")
        fates = "；".join(f"{f.get('name', '')}：{f.get('fate', '')}"
                          for f in summary.get("character_fates", []))
        if fates:
            parts.append(f"角色命运：{fates}")
        recap = "；".join(f"{r.get('foreshadow', '')}（{r.get('status', '')}）"
                          for r in summary.get("foreshadow_recap", []))
        if recap:
            parts.append(f"伏笔回收：{recap}")

    for c in chapters:
        parts.append("")
        ch_title = c.get("title") or f"第{c.get('chapter', '?')}章"
        parts.append(ch_title)
        parts.append(c.get("text", ""))

    return "\n".join(parts).encode("utf-8")


# ---------- Markdown ----------

def export_markdown(pid):
    """导出 Markdown：# 标题 + 题材引用 + 完结总结 + 各章 ## 二级标题。返回 bytes。"""
    meta = storage.get_project(pid) or {}
    chapters = storage.load_data(pid, "chapters.json") or []
    summary = meta.get("finale_summary")

    parts = [f"# {meta.get('title', '')}", ""]
    if meta.get("genre"):
        parts.append(f"> 题材：{meta['genre']}")
        parts.append("")

    if summary:
        parts.append("## 完结总结")
        parts.append("")
        parts.append(f"- 全书字数：{summary.get('total_word_count', 0)}")
        for f in summary.get("character_fates", []):
            parts.append(f"- 角色命运 · {f.get('name', '')}：{f.get('fate', '')}")
        for r in summary.get("foreshadow_recap", []):
            parts.append(f"- 伏笔 · {r.get('foreshadow', '')}：{r.get('status', '')}")
        if summary.get("timeline"):
            parts.append("")
            parts.append("### 时间线")
            for t in summary.get("timeline", []):
                parts.append(f"- 第{t.get('chapter', '?')}章：{t.get('event', '')}")
        parts.append("")

    for c in chapters:
        ch_title = c.get("title") or f"第{c.get('chapter', '?')}章"
        parts.append(f"## {ch_title}")
        parts.append("")
        parts.append(c.get("text", ""))
        parts.append("")

    return "\n".join(parts).encode("utf-8")


# ---------- EPUB ----------

_EPUB_CONTAINER_XML = """<?xml version="1.0" encoding="utf-8"?>
<container version="1.0" xmlns="urn:oasis:names:tc:opendocument:xmlns:container">
  <rootfiles>
    <rootfile full-path="OEBPS/content.opf" media-type="application/oebps-package+xml"/>
  </rootfiles>
</container>
"""


def _xhtml_page(title, body_html):
    return (
        '<?xml version="1.0" encoding="utf-8"?>\n'
        '<!DOCTYPE html>\n'
        '<html xmlns="http://www.w3.org/1999/xhtml">\n'
        f'<head><title>{_escape_html(title)}</title>'
        '<meta charset="utf-8"/></head>\n'
        f'<body>\n{body_html}\n</body>\n</html>\n'
    )


def _paragraphs(text):
    """按空行切段为 <p>。"""
    paras = [p.strip() for p in re.split(r"\n\s*\n", str(text or "")) if p.strip()]
    if not paras and (text or "").strip():
        paras = [str(text).strip()]
    return "".join(f"<p>{_escape_html(p)}</p>\n" for p in paras)


def _build_opf(pid, meta, chapters):
    """content.opf 清单 + spine。"""
    title = _escape_html(meta.get("title", ""))
    items = ['<item id="title" href="title.xhtml" media-type="application/xhtml+xml"/>']
    spine = ['<itemref idref="title"/>']
    for i, c in enumerate(chapters, 1):
        items.append(f'<item id="ch{i}" href="chapter{i}.xhtml" media-type="application/xhtml+xml"/>')
        spine.append(f'<itemref idref="ch{i}"/>')
    items_xml = "\n    ".join(items)
    spine_xml = "\n    ".join(spine)
    uid = f"urn:uuid:{pid}"
    return f"""<?xml version="1.0" encoding="utf-8"?>
<package xmlns="http://www.idpf.org/2007/opf" version="2.0" unique-id="bookid">
  <metadata xmlns:dc="http://purl.org/dc/elements/1.1/" xmlns:opf="http://www.idpf.org/2007/opf">
    <dc:identifier id="bookid">{_escape_html(uid)}</dc:identifier>
    <dc:title>{title}</dc:title>
    <dc:language>zh-CN</dc:language>
  </metadata>
  <manifest>
    {items_xml}
    <item id="ncx" href="toc.ncx" media-type="application/x-dtbncx+xml"/>
  </manifest>
  <spine toc="ncx">
    {spine_xml}
  </spine>
</package>
"""


def _build_ncx(meta, chapters):
    """toc.ncx 目录。"""
    title = _escape_html(meta.get("title", ""))
    nav_points = [
        '<navPoint id="navtitle" playOrder="1">',
        f'<navLabel><text>{title}</text></navLabel>',
        '<content src="title.xhtml"/>',
        '</navPoint>',
    ]
    for i, c in enumerate(chapters, 1):
        ch_title = _escape_html(c.get("title", f"第{i}章"))
        nav_points.append(f'<navPoint id="navch{i}" playOrder="{i+1}">')
        nav_points.append(f'<navLabel><text>{ch_title}</text></navLabel>')
        nav_points.append(f'<content src="chapter{i}.xhtml"/>')
        nav_points.append('</navPoint>')
    nav_xml = "\n    ".join(nav_points)
    return f"""<?xml version="1.0" encoding="utf-8"?>
<ncx xmlns="http://www.daisy.org/z3986/2005/ncx/" version="2005-1">
  <head>
    <meta name="dtb:uid" content="{_escape_html(meta.get('id', ''))}"/>
  </head>
  <docTitle><text>{title}</text></docTitle>
  <navMap>
    {nav_xml}
  </navMap>
</ncx>
"""


def _build_title_page(meta, summary):
    """封面页：标题 + 题材 + 完结总结。"""
    body = f"<h1>{_escape_html(meta.get('title', ''))}</h1>"
    if meta.get("genre"):
        body += f"<p><em>题材：{_escape_html(meta['genre'])}</em></p>"
    if summary:
        body += f"<h2>完结总结</h2>"
        body += f"<p>全书字数：{summary.get('total_word_count', 0)}</p>"
        if summary.get("character_fates"):
            body += "<h3>角色命运</h3>"
            for f in summary.get("character_fates", []):
                body += f"<p>{_escape_html(f.get('name', ''))}：{_escape_html(f.get('fate', ''))}</p>"
        if summary.get("foreshadow_recap"):
            body += "<h3>伏笔回收</h3>"
            for r in summary.get("foreshadow_recap", []):
                body += f"<p>{_escape_html(r.get('foreshadow', ''))}（{_escape_html(r.get('status', ''))}）</p>"
        if summary.get("timeline"):
            body += "<h3>时间线</h3>"
            for t in summary.get("timeline", []):
                body += f"<p>第{_escape_html(t.get('chapter', '?'))}章：{_escape_html(t.get('event', ''))}</p>"
    return _xhtml_page(meta.get("title", ""), body)


def _build_chapter_xhtml(chapter):
    """单章 XHTML。"""
    title = chapter.get("title", f"第{chapter.get('chapter', '?')}章")
    body = f"<h1>{_escape_html(title)}</h1>"
    body += _paragraphs(chapter.get("text", ""))
    return _xhtml_page(title, body)


def export_epub(pid):
    """导出 EPUB：用 zipfile 标准库。mimetype 不压缩，其余 ZIP_DEFLATED。返回 bytes。"""
    meta = storage.get_project(pid) or {}
    chapters = storage.load_data(pid, "chapters.json") or []
    summary = meta.get("finale_summary")

    buf = io.BytesIO()
    with zipfile.ZipFile(buf, 'w', zipfile.ZIP_DEFLATED) as zf:
        # mimetype 必须第一个且不压缩
        zi = zipfile.ZipInfo("mimetype")
        zi.compress_type = zipfile.ZIP_STORED
        zf.writestr(zi, "application/epub+zip")
        zf.writestr("META-INF/container.xml", _EPUB_CONTAINER_XML)
        zf.writestr("OEBPS/content.opf", _build_opf(pid, meta, chapters))
        zf.writestr("OEBPS/toc.ncx", _build_ncx(meta, chapters))
        zf.writestr("OEBPS/title.xhtml", _build_title_page(meta, summary))
        for i, c in enumerate(chapters, 1):
            zf.writestr(f"OEBPS/chapter{i}.xhtml", _build_chapter_xhtml(c))
    return buf.getvalue()


# ---------- 工程文件 zip ----------

def export_project_zip(pid, include_snapshots=True, include_logs=True, include_workspace=False):
    """打包项目文件夹为 zip。
    可勾选项：snapshots/ / logs/ / chapter_workspace.json。
    其余项目文件（meta + 全部 DATA_FILES）总是包含。
    空目录也写入目录条目以保留项目结构。
    返回 bytes。"""
    pdir = storage.project_dir(pid)
    buf = io.BytesIO()
    excluded_files = {"chapter_workspace.json"}

    with zipfile.ZipFile(buf, 'w', zipfile.ZIP_DEFLATED) as zf:
        for root, dirs, files in os.walk(pdir):
            rel_root = os.path.relpath(root, pdir)
            # 调整子目录列表（按 include 标志过滤）
            if rel_root == ".":
                if not include_snapshots and "snapshots" in dirs:
                    dirs.remove("snapshots")
                if not include_logs and "logs" in dirs:
                    dirs.remove("logs")
            # 写目录条目（保留空目录结构，arcname 以 / 结尾表示目录）
            if rel_root == ".":
                arc_dir = ""
            else:
                # 规范化路径分隔符为正斜杠（zip 标准）
                arc_dir = rel_root.replace(os.sep, "/") + "/"
            zf.writestr(arc_dir, "") if arc_dir else None
            for fname in sorted(files):
                if fname in excluded_files and not include_workspace:
                    continue
                fpath = os.path.join(root, fname)
                arcname = arc_dir + fname
                zf.write(fpath, arcname)
    return buf.getvalue()


def import_project_zip(zip_bytes):
    """从 zip bytes 导入项目。保留原 pid，冲突报错。返回 meta。
    校验：meta.json 必须在根目录；所有条目路径必须在项目目录内（防 zip slip）。"""
    with zipfile.ZipFile(io.BytesIO(zip_bytes), 'r') as zf:
        names = zf.namelist()
        if "meta.json" not in names:
            raise ValueError("无效的工程文件：缺少 meta.json")
        meta = json.loads(zf.read("meta.json"))
        pid = meta.get("id")
        if not pid:
            raise ValueError("无效的工程文件：meta.json 缺少 id")
        pdir = storage.project_dir(pid)
        if os.path.exists(pdir):
            raise ValueError(f"项目 {pid} 已存在，拒绝覆盖")
        # 防 zip slip：校验所有条目路径在 pdir 内
        base = os.path.normpath(pdir)
        for name in names:
            target = os.path.normpath(os.path.join(pdir, name))
            if not (target == base or target.startswith(base + os.sep)):
                raise ValueError(f"非法路径: {name}")
        os.makedirs(pdir, exist_ok=True)
        zf.extractall(pdir)
        return meta


# ---------- 自检：临时项目 + mock LLM，无框架 ----------

if __name__ == "__main__":
    # 1. _escape_html 正确转义 5 个特殊字符
    s = _escape_html('<a href="x">&\'\'</a>')
    assert "&lt;" in s and "&gt;" in s and "&quot;" in s and "&apos;" in s and "&amp;" in s, s
    assert _escape_html(None) == ""

    # 2. _sanitize_filename 清理非法字符
    assert _sanitize_filename('a<b>c:"d/e\\f|g?h*i') == "a_b_c__d_e_f_g_h_i", _sanitize_filename('a<b>c:"d/e\\f|g?h*i')
    assert _sanitize_filename("") == "untitled"

    # 构造临时项目
    meta = storage.create_project("自检小说", "测试")
    pid = meta["id"]
    try:
        # 写入 2 章带正文和摘要 + 角色 + 事件 + 伏笔
        storage.save_data(pid, "chapters.json", [
            {"chapter": 1, "title": "第1章 启程", "text": "主角踏上旅程。\n\n清晨，他走出客栈。", "summary": "主角启程", "word_count": 20},
            {"chapter": 2, "title": "第2章 邂逅", "text": "主角遇见了伙伴。\n\n两人结伴同行。", "summary": "主角相遇", "word_count": 22},
        ])
        storage.save_data(pid, "characters.json", [
            {"name": "张三", "current_emotion": "坚定"},
            {"name": "李四", "current_emotion": "好奇"},
        ])
        storage.save_data(pid, "events.json", [
            {"chapter": 1, "type": "启程", "description": "张三离开家乡"},
            {"chapter": 2, "type": "相遇", "description": "张三遇见李四"},
        ])
        storage.save_data(pid, "foreshadows.json", [
            {"foreshadow": "神秘信物", "status": "未回收"},
        ])
        # 保存一个快照和一条日志，测试 zip 勾选项
        storage.save_snapshot(pid, "v1")
        log_dir = os.path.join(storage.project_dir(pid), "logs")
        with open(os.path.join(log_dir, "test.log"), "w", encoding="utf-8") as f:
            f.write("test log content")

        # 3. generate_finale_summary：LLM 未配置 → 降级版本
        summary = generate_finale_summary(pid)
        assert summary["total_word_count"] == 42, summary
        assert len(summary["character_fates"]) == 2, summary
        assert summary["character_fates"][0]["name"] == "张三", summary
        assert len(summary["foreshadow_recap"]) == 1, summary
        assert summary["foreshadow_recap"][0]["foreshadow"] == "神秘信物", summary
        assert len(summary["timeline"]) == 2, summary

        # 4. generate_finale_summary：mock narrator_finale 抛异常 → 降级
        original_finale = agents.narrator_finale
        original_load_cfg = storage.load_llm_config
        def mock_finale_raise(cfg, cs, chs, ev, fs):
            raise RuntimeError("LLM 炸了")
        _mock_cfg = {k: {"provider": "openai", "model": "test", "api_key": "test", "base_url": ""}
                     for k in ("dialogue", "character", "gm", "inspector", "narrator")}
        agents.narrator_finale = mock_finale_raise
        storage.load_llm_config = lambda: _mock_cfg
        try:
            summary2 = generate_finale_summary(pid)
            assert summary2["total_word_count"] == 42, summary2  # 仍降级
        finally:
            agents.narrator_finale = original_finale
            storage.load_llm_config = original_load_cfg

        # 5. generate_finale_summary：mock 成功路径
        def mock_finale_ok(cfg, cs, chs, ev, fs):
            return {"total_word_count": 999,
                    "character_fates": [{"name": "测试", "fate": "通关"}],
                    "foreshadow_recap": [],
                    "timeline": []}
        agents.narrator_finale = mock_finale_ok
        storage.load_llm_config = lambda: _mock_cfg
        try:
            summary3 = generate_finale_summary(pid)
            assert summary3["total_word_count"] == 999, summary3
        finally:
            agents.narrator_finale = original_finale
            storage.load_llm_config = original_load_cfg

        # 6. export_txt 返回 bytes，含标题和章节正文
        meta_now = storage.get_project(pid)
        meta_now["finale_summary"] = summary
        storage.update_project(pid, meta_now)

        txt = export_txt(pid)
        assert isinstance(txt, bytes), type(txt)
        txt_str = txt.decode("utf-8")
        assert "《自检小说》" in txt_str, txt_str
        assert "第1章 启程" in txt_str, txt_str
        assert "主角踏上旅程" in txt_str, txt_str
        assert "第2章 邂逅" in txt_str, txt_str
        assert "全书字数" in txt_str, txt_str  # 完结总结

        # 7. export_markdown 返回 bytes，含 # 标题和 ## 章节
        md = export_markdown(pid)
        assert isinstance(md, bytes)
        md_str = md.decode("utf-8")
        assert md_str.startswith("# 自检小说"), md_str[:30]
        assert "## 第1章 启程" in md_str, md_str
        assert "## 第2章 邂逅" in md_str, md_str
        assert "## 完结总结" in md_str, md_str
        assert "> 题材：" in md_str, md_str

        # 8. export_epub 返回 bytes，是合法 zip，含必需文件
        epub = export_epub(pid)
        assert isinstance(epub, bytes)
        epub_buf = io.BytesIO(epub)
        with zipfile.ZipFile(epub_buf, 'r') as zf:
            names = zf.namelist()
            assert "mimetype" in names, names
            assert "META-INF/container.xml" in names, names
            assert "OEBPS/content.opf" in names, names
            assert "OEBPS/toc.ncx" in names, names
            assert "OEBPS/title.xhtml" in names, names
            assert "OEBPS/chapter1.xhtml" in names, names
            assert "OEBPS/chapter2.xhtml" in names, names
            # mimetype 内容正确，且未压缩
            assert zf.read("mimetype").decode() == "application/epub+zip"
            mi = zf.getinfo("mimetype")
            assert mi.compress_type == zipfile.ZIP_STORED, mi.compress_type
            # content.opf 含 manifest + spine 引用
            opf = zf.read("OEBPS/content.opf").decode("utf-8")
            assert 'href="title.xhtml"' in opf, opf
            assert 'href="chapter1.xhtml"' in opf, opf
            assert 'href="chapter2.xhtml"' in opf, opf
            # chapter1.xhtml 含正文（已转义）
            ch1 = zf.read("OEBPS/chapter1.xhtml").decode("utf-8")
            assert "主角踏上旅程" in ch1, ch1
            assert "<h1>" in ch1, ch1
            assert "<p>" in ch1, ch1

        # 9. export_project_zip 默认含 snapshots + logs，不含 workspace
        zip_bytes = export_project_zip(pid)
        assert isinstance(zip_bytes, bytes)
        with zipfile.ZipFile(io.BytesIO(zip_bytes), 'r') as zf:
            names = zf.namelist()
            assert "meta.json" in names, names
            assert "chapters.json" in names, names
            assert "characters.json" in names, names
            assert any(n.startswith("snapshots/v1/") for n in names), names
            assert any(n.startswith("logs/") for n in names), names

        # 10. export_project_zip include_snapshots=False 不含 snapshots
        zip_bytes2 = export_project_zip(pid, include_snapshots=False, include_logs=False)
        with zipfile.ZipFile(io.BytesIO(zip_bytes2), 'r') as zf:
            names = zf.namelist()
            assert "meta.json" in names, names
            assert not any(n.startswith("snapshots/") for n in names), names
            assert not any(n.startswith("logs/") for n in names), names

        # 11. export_project_zip include_workspace=True 包含 chapter_workspace.json（需先创建）
        ws_path = os.path.join(storage.project_dir(pid), "chapter_workspace.json")
        with open(ws_path, "w", encoding="utf-8") as f:
            f.write('{"chapter_num":99}')
        zip_bytes3 = export_project_zip(pid, include_workspace=True)
        with zipfile.ZipFile(io.BytesIO(zip_bytes3), 'r') as zf:
            names = zf.namelist()
            assert "chapter_workspace.json" in names, names
        # 默认 include_workspace=False → 不含
        zip_bytes4 = export_project_zip(pid)
        with zipfile.ZipFile(io.BytesIO(zip_bytes4), 'r') as zf:
            names = zf.namelist()
            assert "chapter_workspace.json" not in names, names

        # 12. import_project_zip：导出 → 删除 → 导入 → 校验恢复
        zip_for_import = export_project_zip(pid)
        storage.delete_project(pid)
        assert storage.get_project(pid) is None, "删除后应不存在"
        meta_restored = import_project_zip(zip_for_import)
        assert meta_restored["id"] == pid, meta_restored
        meta_now2 = storage.get_project(pid)
        assert meta_now2 is not None and meta_now2["title"] == "自检小说", meta_now2
        # 章节数据也恢复
        chs = storage.load_data(pid, "chapters.json") or []
        assert len(chs) == 2 and chs[0]["title"] == "第1章 启程", chs

        # 13. import_project_zip：项目已存在 → 报错（拒绝覆盖）
        try:
            import_project_zip(zip_for_import)
            raise AssertionError("应报错：项目已存在")
        except ValueError as e:
            assert "已存在" in str(e), e

        # 14. import_project_zip：缺少 meta.json → 报错
        bad_buf = io.BytesIO()
        with zipfile.ZipFile(bad_buf, 'w') as zf:
            zf.writestr("chapters.json", "[]")
        try:
            import_project_zip(bad_buf.getvalue())
            raise AssertionError("应报错：缺少 meta.json")
        except ValueError as e:
            assert "meta.json" in str(e), e

        print("ALL CHECKS PASSED")
    finally:
        storage.delete_project(pid)
