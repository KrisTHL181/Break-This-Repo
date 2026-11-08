import os
import sys
import json
import shutil
import hashlib
import argparse
import subprocess
from pathlib import Path
from datetime import datetime

ROOT = Path(__file__).resolve().parent

REQUIRED_PACKAGES = {
    "markdown": "markdown",
    "jinja2": "Jinja2",
    "pyyaml": "PyYAML",
    "pygments": "Pygments",
}

OPTIONAL_PACKAGES = {
    "pymdownx": "pymdown-extensions",
}

def check_dependencies():
    missing = []
    for module, pip_name in REQUIRED_PACKAGES.items():
        try:
            __import__(module)
        except ImportError:
            missing.append(pip_name)
    if missing:
        print("[依赖检查] 缺少以下包，正在尝试自动安装...")
        try:
            subprocess.check_call(
                [sys.executable, "-m", "pip", "install", *missing],
                stdout=subprocess.DEVNULL,
                stderr=subprocess.DEVNULL,
            )
            print("[依赖检查] 安装完成。")
        except Exception:
            print(f"[依赖检查] 自动安装失败，请手动执行:")
            print(f"  pip install {' '.join(missing)}")
            sys.exit(1)
    try:
        import pymdownx
    except ImportError:
        print("[依赖检查] 可选包 pymdown-extensions 未安装，将使用基础 Markdown 扩展。")

check_dependencies()

import markdown
from jinja2 import Environment, FileSystemLoader
import yaml
from pygments.formatters import HtmlFormatter

CONFIG_PATH = ROOT / "site.config.json"

def load_config():
    with open(CONFIG_PATH, "r", encoding="utf-8") as f:
        return json.load(f)

CONFIG = load_config()
SITE = CONFIG["site"]
BUILD = CONFIG["build"]
NAVIGATION = CONFIG.get("navigation", [])

CONTENT_DIR = ROOT / BUILD["content_dir"]
OUTPUT_DIR = ROOT / BUILD["output_dir"]
TEMPLATE_DIR = ROOT / BUILD["template_dir"]
ASSETS_DIR = ROOT / BUILD["assets_dir"]
MANIFEST_PATH = OUTPUT_DIR / ".build_manifest.json"

def parse_frontmatter(text):
    if text.startswith("---"):
        end = text.find("---", 3)
        if end != -1:
            fm_text = text[3:end].strip()
            body = text[end+3:].strip()
            try:
                meta = yaml.safe_load(fm_text) or {}
            except Exception:
                meta = {}
            return meta, body
    return {}, text

def mermaid_fence_format(source, language, css_class, options, md, **kwargs):
    return f'<div class="{css_class}">{source}</div>'

def get_markdown_instance():
    extensions = [
        "extra",
        "abbr",
        "attr_list",
        "def_list",
        "fenced_code",
        "tables",
        "admonition",
        "toc",
        "sane_lists",
        "smarty",
    ]
    extension_configs = {
        "toc": {"permalink": False},
    }
    try:
        import pymdownx
        extensions.extend([
            "pymdownx.superfences",
            "pymdownx.highlight",
            "pymdownx.inlinehilite",
            "pymdownx.tasklist",
            "pymdownx.tilde",
            "pymdownx.caret",
            "pymdownx.mark",
            "pymdownx.arithmatex",
            "pymdownx.details",
            "pymdownx.keys",
        ])
        extension_configs["pymdownx.highlight"] = {
            "use_pygments": True,
            "linenums": False,
            "css_class": "codehilite",
        }
        extension_configs["pymdownx.superfences"] = {
            "custom_fences": [
                {
                    "name": "mermaid",
                    "class": "mermaid",
                    "format": mermaid_fence_format,
                },
            ],
        }
        extension_configs["pymdownx.arithmatex"] = {
            "generic": True,
        }
        extension_configs["pymdownx.tasklist"] = {
            "custom_checkbox": True,
        }
    except ImportError:
        pass
    return markdown.Markdown(extensions=extensions, extension_configs=extension_configs, output_format="html5")

def md_path_to_html_path(md_rel_path):
    return Path(str(md_rel_path).replace(".md", ".html"))

def file_hash(filepath):
    h = hashlib.md5()
    with open(filepath, "rb") as f:
        h.update(f.read())
    return h.hexdigest()

def collect_papers():
    papers = []
    papers_dir = CONTENT_DIR / "papers"
    if not papers_dir.exists():
        return papers
    for md_file in sorted(papers_dir.glob("*.md")):
        if md_file.name == "index.md":
            continue
        with open(md_file, "r", encoding="utf-8") as f:
            raw = f.read()
        meta, _ = parse_frontmatter(raw)
        if meta.get("hidden", False):
            continue
        rel_path = md_file.relative_to(CONTENT_DIR)
        html_path = str(md_path_to_html_path(rel_path)).replace("\\", "/")
        papers.append({
            "title": meta.get("title", md_file.stem),
            "date": str(meta.get("date", "")),
            "tags": meta.get("tags", []),
            "description": meta.get("description", ""),
            "author": meta.get("author", ""),
            "weight": int(meta.get("weight", 100)),
            "path": html_path,
            "filename": md_file.stem,
        })
    pinned = [p for p in papers if p["weight"] < 10]
    regular = [p for p in papers if p["weight"] >= 10]
    pinned.sort(key=lambda p: p["date"] or "0000-00-00", reverse=True)
    pinned.sort(key=lambda p: p["weight"])
    regular.sort(key=lambda p: (p["date"] or "0000-00-00", p["filename"]), reverse=True)
    return pinned + regular

def generate_paper_item_html(paper, relative_root="", show_pinned=True):
    tags = paper["tags"]
    tags_str = ", ".join(tags) if isinstance(tags, list) else str(tags)
    author = paper.get("author", "") or "207研究所"
    date = paper.get("date", "")
    weight = paper.get("weight", 100)
    is_pinned = show_pinned and weight < 10
    meta_parts = []
    if is_pinned:
        meta_parts.append('<span class="paper-badge paper-badge-pinned">置顶</span>')
    meta_parts.append(f'<span>作者：{author}</span>')
    if date:
        meta_parts.append(f'<span>日期：{date}</span>')
    if tags_str:
        meta_parts.append(f'<span>标签：{tags_str}</span>')
    link = relative_root + paper["path"]
    pdf_path = relative_root + "downloads/" + paper["path"].replace(".html", ".pdf")
    pinned_class = " paper-item-pinned" if is_pinned else ""
    return (
        f'  <div class="paper-item{pinned_class}">\n'
        f'    <div class="paper-info">\n'
        f'      <div class="paper-title"><a href="{link}">{paper["title"]}</a></div>\n'
        f'      <div class="paper-meta">\n'
        f'        {"".join(meta_parts)}\n'
        f'      </div>\n'
        f'    </div>\n'
        f'    <div class="paper-actions">\n'
        f'      <a href="{link}" class="btn btn-primary">阅读</a>\n'
        f'      <a href="{pdf_path}" class="btn" download>PDF</a>\n'
        f'    </div>\n'
        f'  </div>'
    )

def generate_pagination_html(current_page, total_pages, base_path=""):
    if total_pages <= 1:
        return ""
    pages = []
    page_prefix = base_path + "papers/page/"
    if current_page > 1:
        prev_path = f"{base_path}papers/index.html" if current_page == 2 else f"{page_prefix}{current_page - 1}.html"
        pages.append(f'<a href="{prev_path}" class="pagination-btn" aria-label="上一页">上一页</a>')
    else:
        pages.append('<span class="pagination-btn disabled" aria-disabled="true">上一页</span>')
    if total_pages <= 7:
        for i in range(1, total_pages + 1):
            if i == current_page:
                pages.append(f'<span class="pagination-btn active" aria-current="page">{i}</span>')
            else:
                p = f"{base_path}papers/index.html" if i == 1 else f"{page_prefix}{i}.html"
                pages.append(f'<a href="{p}" class="pagination-btn">{i}</a>')
    else:
        pages.append(f'<a href="{base_path}papers/index.html" class="pagination-btn">1</a>')
        if current_page > 3:
            pages.append('<span class="pagination-ellipsis">...</span>')
        start = max(2, current_page - 1)
        end = min(total_pages - 1, current_page + 1)
        for i in range(start, end + 1):
            if i == current_page:
                pages.append(f'<span class="pagination-btn active" aria-current="page">{i}</span>')
            else:
                pages.append(f'<a href="{page_prefix}{i}.html" class="pagination-btn">{i}</a>')
        if current_page < total_pages - 2:
            pages.append('<span class="pagination-ellipsis">...</span>')
        pages.append(f'<a href="{page_prefix}{total_pages}.html" class="pagination-btn">{total_pages}</a>')
    if current_page < total_pages:
        pages.append(f'<a href="{page_prefix}{current_page + 1}.html" class="pagination-btn" aria-label="下一页">下一页</a>')
    else:
        pages.append('<span class="pagination-btn disabled" aria-disabled="true">下一页</span>')
    return f'<nav class="pagination" aria-label="分页导航">{"".join(pages)}</nav>'

def generate_paper_index_html(papers, page=1, per_page=10, relative_root=""):
    pinned = [p for p in papers if p.get("weight", 100) < 10]
    regular = [p for p in papers if p.get("weight", 100) >= 10]
    regular_on_page1 = max(0, per_page - len(pinned))
    if page == 1:
        page_papers = pinned + regular[:regular_on_page1]
        remaining_regular = regular[regular_on_page1:]
        total_pages = 1 + (len(remaining_regular) + per_page - 1) // per_page if remaining_regular else 1
    else:
        start = (page - 2) * per_page
        end = start + per_page
        page_papers = regular[regular_on_page1 + start:regular_on_page1 + end]
        remaining_regular = regular[regular_on_page1:]
        total_pages = 1 + (len(remaining_regular) + per_page - 1) // per_page if remaining_regular else 1
    page = max(1, min(page, total_pages))
    if not page_papers:
        return '<div class="paper-list"><div class="paper-item"><div class="paper-info"><div class="paper-title">暂无论文</div></div></div></div>'
    items = "\n".join(generate_paper_item_html(p, relative_root, show_pinned=(page == 1)) for p in page_papers)
    pagination = generate_pagination_html(page, total_pages, relative_root)
    return f'<div class="paper-list">\n{items}\n</div>\n{pagination}'

def generate_latest_news_html(papers, count=5, relative_root=""):
    latest = papers[:count]
    if not latest:
        return '<div class="paper-list"><div class="paper-item"><div class="paper-info"><div class="paper-title">暂无动态</div></div></div></div>'
    items = "\n".join(generate_paper_item_html(p, relative_root) for p in latest)
    return f'<div class="paper-list">\n{items}\n</div>'

def apply_placeholders(body, papers=None, finance_data=None, relative_root=""):
    import re
    if papers is not None:
        paper_index_html = generate_paper_index_html(papers, page=1, per_page=10, relative_root=relative_root)
        body = re.sub(r'<!--\s*PAPER_INDEX\s*-->', paper_index_html, body)
        latest_news_html = generate_latest_news_html(papers, count=5, relative_root=relative_root)
        body = re.sub(r'<!--\s*LATEST_NEWS\s*-->', latest_news_html, body)
    if finance_data is not None:
        finance_html = generate_finance_html(finance_data)
        body = re.sub(r'<!--\s*FINANCE_DATA\s*-->', finance_html, body)
    return body

def generate_pagination_pages(papers, jinja_env):
    per_page = 10
    pinned = [p for p in papers if p.get("weight", 100) < 10]
    regular = [p for p in papers if p.get("weight", 100) >= 10]
    regular_on_page1 = max(0, per_page - len(pinned))
    remaining_regular = regular[regular_on_page1:]
    total_pages = 1 + (len(remaining_regular) + per_page - 1) // per_page if remaining_regular else 1
    if total_pages <= 1:
        return
    page_dir = OUTPUT_DIR / "papers" / "page"
    page_dir.mkdir(parents=True, exist_ok=True)
    for page_num in range(2, total_pages + 1):
        page_relative_root = "../../"
        paper_index_html = generate_paper_index_html(
            papers, page=page_num, per_page=per_page, relative_root=page_relative_root
        )
        content_html = (
            f'<h2>论文列表 — 第 {page_num} 页</h2>\n'
            f'{paper_index_html}\n'
            f'<p style="margin-top:16px;"><a href="../index.html">← 返回论文列表首页</a></p>'
        )
        template = jinja_env.get_template(BUILD["default_template"])
        rendered = template.render(
            title=f"学术论文 · 第{page_num}页",
            content=content_html,
            page_date="",
            tags=[],
            description=f"207研究所学术论文列表，第{page_num}页",
            current_path=f"papers/page/{page_num}.html",
            relative_root=page_relative_root,
            mathjax=True,
        )
        out_path = page_dir / f"{page_num}.html"
        with open(out_path, "w", encoding="utf-8") as f:
            f.write(rendered)

def render_page(md_file, rel_path, jinja_env, papers=None, finance_data=None):
    with open(md_file, "r", encoding="utf-8") as f:
        raw_content = f.read()
    meta, body = parse_frontmatter(raw_content)
    html_rel_path = md_path_to_html_path(rel_path)
    depth = len(html_rel_path.parts) - 1
    relative_root = "../" * depth if depth > 0 else ""
    if papers is not None or finance_data is not None:
        body = apply_placeholders(body, papers=papers, finance_data=finance_data, relative_root=relative_root)
    md = get_markdown_instance()
    html_content = md.convert(body)
    title = meta.get("title", md_file.stem)
    page_date = str(meta.get("date", ""))
    tags = meta.get("tags", [])
    author = meta.get("author", "")
    description = meta.get("description", "")
    mathjax = meta.get("mathjax", True)
    template_name = meta.get("template", BUILD["default_template"])
    try:
        template = jinja_env.get_template(template_name)
    except Exception:
        template = jinja_env.get_template(BUILD["default_template"])

    is_paper = str(rel_path).replace("\\", "/").startswith("papers/")
    enable_pagination = meta.get("pagination", is_paper)
    page_contents = [html_content]
    if enable_pagination and len(html_content) > 12000:
        page_contents = paginate_article_html(html_content, max_chars=12000)

    total_pages = len(page_contents)
    base_name = html_rel_path.stem
    page_dir = html_rel_path.parent

    for page_idx, page_content in enumerate(page_contents):
        if total_pages > 1:
            nav = build_pagination_nav(page_idx + 1, total_pages, base_name)
            page_content = page_content + "\n" + nav
        if page_idx == 0:
            out_path = OUTPUT_DIR / html_rel_path
            current_path_str = str(html_rel_path).replace("\\", "/")
        else:
            out_filename = f"{base_name}_page{page_idx + 1}.html"
            out_path = OUTPUT_DIR / page_dir / out_filename
            current_path_str = str((page_dir / out_filename)).replace("\\", "/")
        out_path.parent.mkdir(parents=True, exist_ok=True)
        rendered = template.render(
            title=title if page_idx == 0 else f"{title} (第{page_idx + 1}页)",
            content=page_content,
            page_date=page_date,
            tags=tags,
            author=author,
            description=description,
            current_path=current_path_str,
            relative_root=relative_root,
            mathjax=mathjax,
        )
        with open(out_path, "w", encoding="utf-8") as f:
            f.write(rendered)

    return {
        "title": title,
        "path": str(html_rel_path).replace("\\", "/"),
        "search_text": body,
        "tags": tags,
        "date": page_date,
    }

def find_safe_split_points(html_content):
    import re
    split_points = []
    protected_ranges = []
    for pattern in [r'<pre\b[^>]*>.*?</pre>', r'<table\b[^>]*>.*?</table>',
                    r'<script[^>]*type="math/tex"[^>]*>.*?</script>',
                    r'\$\$.*?\$\$', r'\\\[.*?\\\]']:
        for m in re.finditer(pattern, html_content, re.DOTALL):
            protected_ranges.append((m.start(), m.end()))
    for m in re.finditer(r'</h[23]>', html_content):
        pos = m.end()
        is_protected = any(start <= pos < end for start, end in protected_ranges)
        if not is_protected:
            split_points.append(pos)
    return split_points

def paginate_article_html(html_content, max_chars=12000):
    if len(html_content) <= max_chars:
        return [html_content]
    split_points = find_safe_split_points(html_content)
    if not split_points:
        return [html_content]
    pages = []
    current_start = 0
    last_split = 0
    for sp in split_points:
        if sp - current_start >= max_chars and last_split > current_start:
            pages.append(html_content[current_start:last_split])
            current_start = last_split
        last_split = sp
    if current_start < len(html_content):
        pages.append(html_content[current_start:])
    if len(pages) <= 1:
        return [html_content]
    return pages

def build_pagination_nav(current_page, total_pages, base_path):
    if total_pages <= 1:
        return ""
    items = ['<nav class="article-pagination" aria-label="文章分页">']
    if current_page > 1:
        prev_path = base_path if current_page == 2 else f"{base_path}_page{current_page - 1}.html"
        items.append(f'<a href="{prev_path}" class="btn btn-primary">← 上一页</a>')
    else:
        items.append('<span class="btn disabled">← 上一页</span>')
    items.append(f'<span class="article-page-info">第 {current_page} / {total_pages} 页</span>')
    if current_page < total_pages:
        next_path = f"{base_path}_page{current_page + 1}.html"
        items.append(f'<a href="{next_path}" class="btn btn-primary">下一页 →</a>')
    else:
        items.append('<span class="btn disabled">下一页 →</span>')
    items.append('</nav>')
    return "\n".join(items)

def collect_markdown_files():
    files = []
    for md_file in CONTENT_DIR.rglob("*.md"):
        rel_path = md_file.relative_to(CONTENT_DIR)
        if str(rel_path).startswith("finance" + os.sep):
            continue
        files.append((md_file, rel_path))
    return sorted(files, key=lambda x: str(x[1]))

def copy_assets():
    dest = OUTPUT_DIR / "assets"
    if dest.exists():
        shutil.rmtree(dest)
    shutil.copytree(ASSETS_DIR, dest)
    ico_src = ROOT / "ico.svg"
    if ico_src.exists():
        shutil.copy2(ico_src, OUTPUT_DIR / "assets" / "ico.svg")
    static_dir = CONTENT_DIR / "static"
    if static_dir.exists():
        for html_file in static_dir.glob("*.html"):
            shutil.copy2(html_file, OUTPUT_DIR / html_file.name)
            print(f"  [静态] {html_file.name}")

def build_and_copy_game():
    game_dir = ROOT / "games" / "paiwawa-main"
    game_dist = game_dir / "dist"
    dest = OUTPUT_DIR / "game"
    if not game_dist.exists():
        print("[游戏] 跳过: 未找到预构建的 dist/ 目录 (请先在 games/paiwawa-main 执行 npm run build)")
        return
    try:
        if dest.exists():
            shutil.rmtree(dest)
        shutil.copytree(game_dist, dest)
        print(f"[游戏] 已复制预构建产物到 /game/")
    except Exception as e:
        print(f"[游戏] 复制失败: {e}")

def copy_markdown_sources():
    for md_file, rel_path in collect_markdown_files():
        dest = OUTPUT_DIR / "downloads" / rel_path
        dest.parent.mkdir(parents=True, exist_ok=True)
        shutil.copy2(md_file, dest)

def build_search_index(pages):
    index = []
    for page in pages:
        index.append({
            "title": page["title"],
            "path": page["path"],
            "content": page.get("search_text", "")[:5000],
            "tags": page.get("tags", []),
            "date": page.get("date", ""),
        })
    return index

def parse_md_table(lines, start_idx):
    rows = []
    i = start_idx
    while i < len(lines) and lines[i].strip().startswith("|"):
        row = [c.strip() for c in lines[i].strip().strip("|").split("|")]
        if not all(set(c) <= set("-: ") for c in row):
            rows.append(row)
        i += 1
    return rows, i

def extract_number(s):
    import re
    m = re.search(r'[\d.]+', str(s).replace(",", ""))
    return float(m.group()) if m else 0.0

def parse_finance_month(md_path):
    with open(md_path, "r", encoding="utf-8") as f:
        content = f.read()
    lines = content.split("\n")
    data = {"expenses": {}, "income": {}, "budget": {}, "expense_details": {}, "note": ""}
    current_section = None
    i = 0
    while i < len(lines):
        line = lines[i].strip()
        if line.startswith("## "):
            current_section = line[3:].strip()
            i += 1
            continue
        if line.startswith("|") and current_section:
            rows, i = parse_md_table(lines, i)
            if len(rows) >= 2:
                headers = rows[0]
                for row in rows[1:]:
                    if len(row) >= 2:
                        key = row[0]
                        val = extract_number(row[1])
                        if current_section == "支出":
                            data["expenses"][key] = val
                        elif current_section == "收入":
                            data["income"][key] = val
                        elif current_section == "原始预算":
                            data["budget"][key] = val
                        elif current_section == "内务费用明细" and len(row) >= 5:
                            try:
                                data["expense_details"][key] = {
                                    "qty": row[1], "unit": row[2],
                                    "price": extract_number(row[3]),
                                    "total": extract_number(row[4])
                                }
                            except (ValueError, IndexError):
                                pass
            continue
        if current_section == "备注" and line and not line.startswith("#"):
            data["note"] += line + "\n"
        i += 1
    return data

def cleanup_old_finance(max_years=5):
    finance_dir = CONTENT_DIR / "finance"
    if not finance_dir.exists():
        return 0
    current_year = datetime.now().year
    removed = 0
    for year_dir in finance_dir.iterdir():
        if not year_dir.is_dir():
            continue
        try:
            year = int(year_dir.name)
            if current_year - year > max_years:
                import shutil
                shutil.rmtree(year_dir)
                removed += 1
                print(f"[财务] 已清理超过{max_years}年的旧数据: {year_dir.name}")
        except ValueError:
            pass
    return removed

def load_finance_data():
    finance_dir = CONTENT_DIR / "finance"
    if not finance_dir.exists():
        return {}
    years = {}
    for year_dir in sorted(finance_dir.iterdir()):
        if not year_dir.is_dir():
            continue
        year = year_dir.name
        months = {}
        for md_file in sorted(year_dir.glob("*.md")):
            if md_file.stem == "total":
                continue
            try:
                month = int(md_file.stem)
                months[month] = parse_finance_month(md_file)
            except ValueError:
                pass
        if months:
            years[year] = months
    return years

def calc_finance_totals(month_data):
    total_exp = sum(month_data.get("expenses", {}).values())
    total_inc = sum(month_data.get("income", {}).values())
    total_budget = sum(month_data.get("budget", {}).values())
    return {
        "total_expense": total_exp,
        "total_income": total_inc,
        "net_profit": total_inc - total_exp,
        "budget": total_budget,
        "over_budget": total_exp - total_budget,
        "budget_remaining": total_budget - total_exp,
    }

def compute_finance_hashes(finance_data):
    hashes = {}
    finance_dir = CONTENT_DIR / "finance"
    for year, months in finance_data.items():
        for month in months:
            md_path = finance_dir / year / f"{month}.md"
            if md_path.exists():
                with open(md_path, "rb") as f:
                    h = hashlib.sha256(f.read()).hexdigest()
                hashes[f"{year}-{month}"] = h
    return hashes

def fmt_cny(v):
    return f"{v:.2f} CNY"

def fmt_num(v):
    return f"{v:.2f}"

def generate_finance_html(finance_data):
    if not finance_data:
        return "<p>暂无数据</p>"
    years = sorted(finance_data.keys(), reverse=True)
    latest_year = years[0]
    latest_months = sorted(finance_data[latest_year].keys(), reverse=True)
    latest_month = latest_months[0] if latest_months else "total"

    html = '<div class="finance-controls"><div class="finance-select-group">'
    html += '<label for="financeYearSelect">年份：</label>'
    html += '<select id="financeYearSelect" class="finance-select" onchange="switchFinanceView()">'
    for y in years:
        sel = " selected" if y == latest_year else ""
        html += f'<option value="{y}"{sel}>{y}年</option>'
    html += '</select>'
    html += '<label for="financeMonthSelect">月份：</label>'
    html += '<select id="financeMonthSelect" class="finance-select" onchange="switchFinanceView()">'
    html += '<option value="total">全年汇总</option>'
    for m in latest_months:
        sel = " selected" if m == latest_month else ""
        html += f'<option value="{m}"{sel}>{m}月</option>'
    html += '</select></div></div>'

    html += '<div id="financeContent">'

    for year in years:
        months = finance_data[year]
        year_exp = sum(calc_finance_totals(m)["total_expense"] for m in months.values())
        year_inc = sum(calc_finance_totals(m)["total_income"] for m in months.values())
        year_budget = sum(calc_finance_totals(m)["budget"] for m in months.values())
        year_net = year_inc - year_exp
        net_class = "finance-positive" if year_net >= 0 else "finance-negative"

        panel_display = "" if (year == latest_year and latest_month == "total") else ' style="display:none"'
        html += f'<div class="finance-panel" data-year="{year}" data-month="total"{panel_display}>'
        html += '<div class="finance-cards">'
        html += f'<div class="finance-card"><div class="finance-card-label">全年总支出 (CNY)</div><div class="finance-card-value">{fmt_num(year_exp)}</div></div>'
        html += f'<div class="finance-card"><div class="finance-card-label">全年总收入 (CNY)</div><div class="finance-card-value">{fmt_num(year_inc)}</div></div>'
        html += f'<div class="finance-card"><div class="finance-card-label">全年净利润 (CNY)</div><div class="finance-card-value {net_class}">{fmt_num(year_net)}</div></div>'
        html += f'<div class="finance-card"><div class="finance-card-label">全年预算 (CNY)</div><div class="finance-card-value">{fmt_num(year_budget)}</div></div>'
        html += '</div>'
        html += '<h3 class="finance-section-title">月度汇总</h3>'
        html += '<table class="finance-table"><thead><tr><th>月份</th><th>总支出 (CNY)</th><th>总收入 (CNY)</th><th>净利润 (CNY)</th><th>预算 (CNY)</th><th>预算执行</th></tr></thead><tbody>'
        for m in sorted(months.keys()):
            t = calc_finance_totals(months[m])
            exec_pct = f"{t['total_expense']/t['budget']*100:.1f}%" if t["budget"] > 0 else "-/-"
            net_cls = "finance-positive" if t["net_profit"] >= 0 else "finance-negative"
            html += f'<tr><td><a href="javascript:selectFinanceMonth({m})">{year}年{m}月</a></td><td>{fmt_num(t["total_expense"])}</td><td>{fmt_num(t["total_income"])}</td><td class="{net_cls}">{fmt_num(t["net_profit"])}</td><td>{fmt_num(t["budget"])}</td><td>{exec_pct}</td></tr>'
        html += '</tbody></table>'
        html += f'<p style="margin-top:16px"><a href="downloads/finance/total_{year}.pdf" class="btn btn-primary" download>下载 {year}年 全年PDF</a></p>'
        html += '</div>'

        for m, md in months.items():
            t = calc_finance_totals(md)
            net_cls = "finance-positive" if t["net_profit"] >= 0 else "finance-negative"
            panel_display = "" if (year == latest_year and m == latest_month) else ' style="display:none"'
            html += f'<div class="finance-panel" data-year="{year}" data-month="{m}"{panel_display}>'
            html += '<div class="finance-cards">'
            html += f'<div class="finance-card"><div class="finance-card-label">总支出 (CNY)</div><div class="finance-card-value">{fmt_num(t["total_expense"])}</div></div>'
            html += f'<div class="finance-card"><div class="finance-card-label">总收入 (CNY)</div><div class="finance-card-value">{fmt_num(t["total_income"])}</div></div>'
            html += f'<div class="finance-card"><div class="finance-card-label">净利润 (CNY)</div><div class="finance-card-value {net_cls}">{fmt_num(t["net_profit"])}</div></div>'
            html += f'<div class="finance-card"><div class="finance-card-label">预算 (CNY)</div><div class="finance-card-value">{fmt_num(t["budget"])}</div></div>'
            html += '</div>'
            html += '<h3 class="finance-section-title">支出明细</h3><table class="finance-table"><thead><tr><th>项目</th><th>金额 (CNY)</th></tr></thead><tbody>'
            for k, v in md.get("expenses", {}).items():
                html += f'<tr><td>{k}</td><td>{fmt_num(v)}</td></tr>'
            html += f'<tr class="finance-total-row"><td>支出合计</td><td>{fmt_num(t["total_expense"])}</td></tr></tbody></table>'
            if md.get("expense_details"):
                html += '<h4 class="finance-subtitle">内务费用明细</h4>'
                html += '<table class="finance-table finance-table-sm"><thead><tr><th>项目</th><th>数量</th><th>单价</th><th>小计 (CNY)</th></tr></thead><tbody>'
                for k, d in md["expense_details"].items():
                    html += f'<tr><td>{k}</td><td>{d["qty"]}{d["unit"]}</td><td>{d["price"]:.2f} CNY/{d["unit"]}</td><td>{fmt_num(d["total"])}</td></tr>'
                html += '</tbody></table>'
            html += '<h3 class="finance-section-title">收入明细</h3><table class="finance-table"><thead><tr><th>项目</th><th>金额 (CNY)</th></tr></thead><tbody>'
            for k, v in md.get("income", {}).items():
                html += f'<tr><td>{k}</td><td>{fmt_num(v)}</td></tr>'
            html += f'<tr class="finance-total-row"><td>收入合计</td><td>{fmt_num(t["total_income"])}</td></tr></tbody></table>'
            if md.get("budget"):
                html += '<h3 class="finance-section-title">原始预算</h3><table class="finance-table finance-table-sm"><thead><tr><th>项目</th><th>金额 (CNY)</th></tr></thead><tbody>'
                for k, v in md["budget"].items():
                    html += f'<tr><td>{k}</td><td>{fmt_num(v)}</td></tr>'
                html += f'<tr class="finance-total-row"><td>预算合计</td><td>{fmt_num(t["budget"])}</td></tr></tbody></table>'
            if md.get("note", "").strip():
                html += f'<p class="finance-note">注：{md["note"].strip()}</p>'
            html += f'<p style="margin-top:16px"><a href="downloads/finance/month_{year}_{m}.pdf" class="btn btn-primary" download>下载 {year}年{m}月 PDF</a></p>'
            html += '</div>'

    html += '</div>'

    html += '<script>'
    html += 'var financeMonths=' + json.dumps({y: sorted(m.keys(), reverse=True) for y, m in finance_data.items()}, ensure_ascii=False) + ';'
    html += """
function switchFinanceView(){
  var y=document.getElementById("financeYearSelect").value;
  var m=document.getElementById("financeMonthSelect").value;
  var sel=document.getElementById("financeMonthSelect");
  var ms=financeMonths[y]||[];
  if(sel.options.length<=1||String(ms[0])!==sel.options[1].value){
    sel.innerHTML='<option value="total">全年汇总</option>';
    ms.forEach(function(mm){var o=document.createElement("option");o.value=String(mm);o.textContent=mm+"月";sel.appendChild(o);});
    sel.value=m;
  }
  document.querySelectorAll(".finance-panel").forEach(function(p){
    if(p.dataset.year===y&&p.dataset.month===m){p.style.display="";}
    else{p.style.display="none";}
  });
}
function selectFinanceMonth(m){
  document.getElementById("financeMonthSelect").value=String(m);
  switchFinanceView();
}
"""
    html += '</script>'

    hashes = compute_finance_hashes(finance_data)
    html += '<div class="finance-integrity">'
    html += '<h3 class="finance-section-title">数据完整性验证</h3>'
    html += '<p class="finance-note">为防止前端篡改，以下为各月份原始数据文件的 SHA-256 哈希值。可与 <a href="https://gitee.com/buelierm/Apart207/" target="_blank" rel="noopener">Gitee 仓库</a> 中 <code>content/finance/</code> 目录下的源文件比对验证。</p>'
    html += '<table class="finance-table finance-table-sm"><thead><tr><th>数据文件</th><th>SHA-256</th></tr></thead><tbody>'
    for key in sorted(hashes.keys(), reverse=True):
        year, month = key.split("-")
        html += f'<tr><td><code>{year}/{month}.md</code></td><td><code style="font-size:0.75rem;word-break:break-all">{hashes[key]}</code></td></tr>'
    html += '</tbody></table></div>'

    return html


def generate_finance_pdf_html(finance_data, year=None):
    if not finance_data:
        return "<p>暂无财务数据</p>"
    years = sorted(finance_data.keys(), reverse=True)
    target_year = year if year and year in finance_data else years[0]
    months = finance_data[target_year]
    year_exp = sum(calc_finance_totals(m)["total_expense"] for m in months.values())
    year_inc = sum(calc_finance_totals(m)["total_income"] for m in months.values())
    year_budget = sum(calc_finance_totals(m)["budget"] for m in months.values())
    year_net = year_inc - year_exp
    html = f"<h2>{target_year}年 财务年报</h2>"
    html += f"<p>单位：CNY | 总支出：{year_exp:.2f} | 总收入：{year_inc:.2f} | 净利润：{year_net:.2f} | 总预算：{year_budget:.2f}</p>"
    html += '<table border="1" cellpadding="6" style="border-collapse:collapse;width:100%"><tr><th>月份</th><th>支出 (CNY)</th><th>收入 (CNY)</th><th>净利润 (CNY)</th><th>预算 (CNY)</th></tr>'
    for m in sorted(months.keys()):
        t = calc_finance_totals(months[m])
        html += f'<tr><td>{m}月</td><td>{t["total_expense"]:.2f}</td><td>{t["total_income"]:.2f}</td><td>{t["net_profit"]:.2f}</td><td>{t["budget"]:.2f}</td></tr>'
    html += "</table>"
    return html

def generate_finance_pdfs(finance_data, engine="playwright"):
    if not finance_data:
        return 0, 0
    from pathlib import Path
    pdf_dir = OUTPUT_DIR / "downloads" / "finance"
    pdf_dir.mkdir(parents=True, exist_ok=True)
    count = 0
    errors = 0

    def make_pdf(html_content, pdf_path):
        nonlocal count, errors
        tmp_path = OUTPUT_DIR / "finance-pdf-temp.html"
        full_html = f"<!DOCTYPE html><html><head><meta charset='UTF-8'><link rel='stylesheet' href='../assets/css/style.css'></head><body><div class='container' style='max-width:800px;margin:0 auto;padding:20px'>{html_content}</div></body></html>"
        with open(tmp_path, "w", encoding="utf-8") as f:
            f.write(full_html)
        try:
            if engine == "playwright":
                from playwright.sync_api import sync_playwright
                with sync_playwright() as p:
                    browser = p.chromium.launch(headless=True)
                    page = browser.new_page()
                    for attempt in range(3):
                        try:
                            page.goto(tmp_path.resolve().as_uri(), wait_until="domcontentloaded", timeout=60000)
                            page.wait_for_timeout(1000)
                            break
                        except Exception:
                            if attempt == 2:
                                raise
                            page.wait_for_timeout(2000)
                    page.pdf(path=str(pdf_path), format="A4", print_background=True,
                             margin={"top": "15mm", "bottom": "15mm", "left": "12mm", "right": "12mm"})
                    browser.close()
            else:
                from weasyprint import HTML
                HTML(filename=str(tmp_path), base_url=str(OUTPUT_DIR)).write_pdf(target=str(pdf_path))
            count += 1
        except Exception as e:
            print(f"  [PDF] 财务PDF失败: {pdf_path.name} - {e}")
            errors += 1
        if tmp_path.exists():
            tmp_path.unlink()

    for year, months in finance_data.items():
        year_exp = sum(calc_finance_totals(m)["total_expense"] for m in months.values())
        year_inc = sum(calc_finance_totals(m)["total_income"] for m in months.values())
        year_budget = sum(calc_finance_totals(m)["budget"] for m in months.values())
        year_html = f"<h2>{year}年 财务年报</h2>"
        year_html += f"<p>总支出：￥{year_exp:.2f} | 总收入：￥{year_inc:.2f} | 净利润：￥{year_inc-year_exp:.2f} | 总预算：￥{year_budget:.2f}</p>"
        year_html += '<table border="1" cellpadding="6" style="border-collapse:collapse;width:100%"><tr><th>月份</th><th>支出</th><th>收入</th><th>净利润</th><th>预算</th></tr>'
        for m in sorted(months.keys()):
            t = calc_finance_totals(months[m])
            year_html += f'<tr><td>{m}月</td><td>￥{t["total_expense"]:.2f}</td><td>￥{t["total_income"]:.2f}</td><td>￥{t["net_profit"]:.2f}</td><td>￥{t["budget"]:.2f}</td></tr>'
        year_html += "</table>"
        make_pdf(year_html, pdf_dir / f"total_{year}.pdf")

        for m, md in months.items():
            t = calc_finance_totals(md)
            m_html = f"<h2>{year}年{m}月 财务明细</h2>"
            m_html += f"<p>总支出：￥{t['total_expense']:.2f} | 总收入：￥{t['total_income']:.2f} | 净利润：￥{t['net_profit']:.2f} | 预算：￥{t['budget']:.2f}</p>"
            m_html += "<h3>支出</h3><table border='1' cellpadding='6' style='border-collapse:collapse;width:100%'>"
            for k, v in md.get("expenses", {}).items():
                m_html += f"<tr><td>{k}</td><td>￥{v:.2f}</td></tr>"
            m_html += f"<tr><td><b>合计</b></td><td><b>￥{t['total_expense']:.2f}</b></td></tr></table>"
            m_html += "<h3>收入</h3><table border='1' cellpadding='6' style='border-collapse:collapse;width:100%'>"
            for k, v in md.get("income", {}).items():
                m_html += f"<tr><td>{k}</td><td>￥{v:.2f}</td></tr>"
            m_html += f"<tr><td><b>合计</b></td><td><b>￥{t['total_income']:.2f}</b></td></tr></table>"
            if md.get("note", "").strip():
                m_html += f"<p>注：{md['note'].strip()}</p>"
            make_pdf(m_html, pdf_dir / f"month_{year}_{m}.pdf")

    return count, errors

def get_pdf_html_path(page_info, finance_data):
    if page_info["path"] == "finance.html":
        return None
    return OUTPUT_DIR / page_info["path"]

def generate_pdfs_with_playwright(pages, finance_data=None):
    from playwright.sync_api import sync_playwright
    pdf_count = 0
    pdf_errors = 0
    with sync_playwright() as p:
        browser = p.chromium.launch(headless=True)
        context = browser.new_context()
        for page_info in pages:
            html_path = get_pdf_html_path(page_info, finance_data)
            if html_path is None or not html_path.exists():
                continue
            pdf_rel = Path("downloads") / page_info["path"].replace(".html", ".pdf")
            pdf_path = OUTPUT_DIR / pdf_rel
            pdf_path.parent.mkdir(parents=True, exist_ok=True)
            try:
                page = context.new_page()
                file_url = html_path.resolve().as_uri()
                for attempt in range(3):
                    try:
                        page.goto(file_url, wait_until="domcontentloaded", timeout=60000)
                        page.wait_for_timeout(2000)
                        break
                    except Exception:
                        if attempt == 2:
                            raise
                        page.wait_for_timeout(2000)
                page.pdf(
                    path=str(pdf_path),
                    format="A4",
                    print_background=True,
                    margin={"top": "20mm", "bottom": "20mm", "left": "15mm", "right": "15mm"},
                    prefer_css_page_size=True,
                )
                page.close()
                pdf_count += 1
            except Exception as e:
                print(f"  [PDF] 失败: {page_info['path']} - {e}")
                try:
                    page.close()
                except Exception:
                    pass
                pdf_errors += 1
        browser.close()
    tmp = OUTPUT_DIR / "finance-pdf-temp.html"
    if tmp.exists():
        tmp.unlink()
    return pdf_count, pdf_errors

def generate_pdfs_with_weasyprint(pages, finance_data=None):
    from weasyprint import HTML
    pdf_count = 0
    pdf_errors = 0
    base_url = str(OUTPUT_DIR)
    for page_info in pages:
        html_path = get_pdf_html_path(page_info, finance_data)
        if not html_path.exists():
            continue
        pdf_rel = Path("downloads") / page_info["path"].replace(".html", ".pdf")
        pdf_path = OUTPUT_DIR / pdf_rel
        pdf_path.parent.mkdir(parents=True, exist_ok=True)
        try:
            HTML(filename=str(html_path), base_url=base_url).write_pdf(target=str(pdf_path))
            pdf_count += 1
        except Exception as e:
            print(f"  [PDF] 失败: {page_info['path']} - {e}")
            pdf_errors += 1
    tmp = OUTPUT_DIR / "finance-pdf-temp.html"
    if tmp.exists():
        tmp.unlink()
    return pdf_count, pdf_errors

def generate_pdfs(pages, finance_data=None):
    playwright_available = False
    weasyprint_available = False
    try:
        import playwright
        playwright_available = True
    except Exception:
        pass
    try:
        import weasyprint
        weasyprint_available = True
    except Exception:
        pass

    if not playwright_available and not weasyprint_available:
        print("[PDF] 跳过: 未安装 playwright 或 weasyprint (或缺少运行库)")
        print("       推荐安装 playwright: pip install playwright && playwright install chromium")
        print("       或安装 weasyprint + GTK3 运行库: pip install weasyprint")
        return

    engine = "playwright" if playwright_available else "weasyprint"
    if playwright_available:
        print("[PDF] 使用 Playwright 引擎生成 PDF...")
    else:
        print("[PDF] 使用 WeasyPrint 引擎生成 PDF...")
        print("       注意: WeasyPrint 不执行 JavaScript，数学公式可能显示为 LaTeX 源码")

    if engine == "playwright":
        try:
            pdf_count, pdf_errors = generate_pdfs_with_playwright(pages, finance_data)
        except Exception as e:
            print(f"[PDF] Playwright 启动失败: {e}")
            if weasyprint_available:
                print("[PDF] 回退到 WeasyPrint...")
                engine = "weasyprint"
                pdf_count, pdf_errors = generate_pdfs_with_weasyprint(pages, finance_data)
            else:
                pdf_count, pdf_errors = 0, 0
    else:
        try:
            pdf_count, pdf_errors = generate_pdfs_with_weasyprint(pages, finance_data)
        except Exception as e:
            print(f"[PDF] WeasyPrint 失败: {e}")
            pdf_count, pdf_errors = 0, 0

    if finance_data:
        print("[PDF] 生成财务 PDF (按年/月)...")
        fin_count, fin_errors = generate_finance_pdfs(finance_data, engine=engine)
        pdf_count += fin_count
        pdf_errors += fin_errors

    if pdf_count > 0:
        print(f"[PDF] 已生成 {pdf_count} 个 PDF 文件" + (f" ({pdf_errors} 个失败)" if pdf_errors else ""))
    else:
        print("[PDF] PDF 生成跳过，网站其他功能不受影响")

def clean_output():
    if OUTPUT_DIR.exists():
        shutil.rmtree(OUTPUT_DIR)
        print(f"[清理] 已清除输出目录: {OUTPUT_DIR}")

def load_manifest():
    if MANIFEST_PATH.exists():
        try:
            with open(MANIFEST_PATH, "r", encoding="utf-8") as f:
                return json.load(f)
        except Exception:
            return {}
    return {}

def save_manifest(manifest):
    OUTPUT_DIR.mkdir(parents=True, exist_ok=True)
    with open(MANIFEST_PATH, "w", encoding="utf-8") as f:
        json.dump(manifest, f, ensure_ascii=False, indent=2)

def serve():
    import http.server
    import socketserver
    os.chdir(OUTPUT_DIR)
    port = 8000
    with socketserver.TCPServer(("", port), http.server.SimpleHTTPRequestHandler) as httpd:
        print(f"[预览] 本地服务器已启动: http://localhost:{port}")
        print("[预览] 按 Ctrl+C 停止")
        try:
            httpd.serve_forever()
        except KeyboardInterrupt:
            print("\n[预览] 已停止")

def watch():
    try:
        from watchdog.observers import Observer
        from watchdog.events import FileSystemEventHandler
    except ImportError:
        print("[监听] watchdog 未安装，无法使用监听模式。安装: pip install watchdog")
        return
    class Handler(FileSystemEventHandler):
        def on_modified(self, event):
            if event.is_directory:
                return
            if event.src_path.endswith(".md") or event.src_path.endswith(".html") or event.src_path.endswith(".css") or event.src_path.endswith(".js"):
                print(f"[变更] {event.src_path}")
                build(clean=False)
    observer = Observer()
    observer.schedule(Handler(), str(CONTENT_DIR), recursive=True)
    observer.schedule(Handler(), str(TEMPLATE_DIR), recursive=True)
    observer.schedule(Handler(), str(ASSETS_DIR), recursive=True)
    observer.start()
    print("[监听] 已启动文件监听，按 Ctrl+C 停止")
    try:
        while True:
            import time
            time.sleep(1)
    except KeyboardInterrupt:
        observer.stop()
    observer.join()

def build(clean=False):
    start_time = datetime.now()
    if clean:
        clean_output()
    OUTPUT_DIR.mkdir(parents=True, exist_ok=True)
    manifest = load_manifest()
    jinja_env = create_jinja_env()
    papers = collect_papers()
    print(f"[论文] 发现 {len(papers)} 篇论文")
    paper_signature = hashlib.md5(json.dumps([{"path": p["path"], "date": p["date"], "title": p["title"], "weight": p["weight"]} for p in papers], ensure_ascii=False).encode()).hexdigest()
    force_rebuild_paths = set()
    if not clean and manifest.get("_paper_signature") != paper_signature:
        force_rebuild_paths.add("papers/index.md")
        force_rebuild_paths.add("index.md")
        print("[论文] 论文列表变化，强制重建索引页和首页")
    manifest["_paper_signature"] = paper_signature
    cleanup_old_finance()
    finance_data = load_finance_data()
    if finance_data:
        print(f"[财务] 发现 {len(finance_data)} 个年度数据")
    md_files = collect_markdown_files()
    print(f"[扫描] 发现 {len(md_files)} 个 Markdown 文件")
    pages = []
    built_count = 0
    skipped_count = 0
    for md_file, rel_path in md_files:
        rel_str = str(rel_path).replace("\\", "/")
        current_hash = file_hash(md_file)
        if not clean and manifest.get(rel_str) == current_hash and rel_str not in force_rebuild_paths:
            html_rel = md_path_to_html_path(rel_path)
            if (OUTPUT_DIR / html_rel).exists():
                skipped_count += 1
                pages.append({
                    "title": md_file.stem,
                    "path": str(html_rel).replace("\\", "/"),
                    "search_text": "",
                    "tags": [],
                    "date": "",
                })
                continue
        page_info = render_page(md_file, rel_path, jinja_env, papers=papers, finance_data=finance_data)
        pages.append(page_info)
        manifest[rel_str] = current_hash
        built_count += 1
        print(f"  [构建] {rel_str} -> {page_info['path']}")
    generate_pagination_pages(papers, jinja_env)
    copy_assets()
    print(f"[资源] 静态资源已复制")
    build_and_copy_game()
    copy_markdown_sources()
    search_index = build_search_index(pages)
    index_path = OUTPUT_DIR / "search-index.json"
    with open(index_path, "w", encoding="utf-8") as f:
        json.dump(search_index, f, ensure_ascii=False, indent=2)
    print(f"[索引] 搜索索引已生成 ({len(search_index)} 条记录)")
    save_manifest(manifest)
    generate_pdfs(pages, finance_data=finance_data)
    elapsed = (datetime.now() - start_time).total_seconds()
    print(f"\n[完成] 构建成功! 新建/更新 {built_count} 个页面, 跳过 {skipped_count} 个, 耗时 {elapsed:.2f}s")
    print(f"       输出目录: {OUTPUT_DIR}")
    return True

def create_jinja_env():
    env = Environment(
        loader=FileSystemLoader(str(TEMPLATE_DIR)),
        autoescape=False,
        trim_blocks=True,
        lstrip_blocks=True,
    )
    env.globals["site"] = SITE
    env.globals["navigation"] = NAVIGATION
    env.globals["build"] = BUILD
    return env

def main():
    parser = argparse.ArgumentParser(
        description="207研究所官网静态构建脚本",
        formatter_class=argparse.RawDescriptionHelpFormatter,
    )
    parser.add_argument("--clean", action="store_true", help="全量重建")
    parser.add_argument("--serve", action="store_true", help="构建后启动本地预览服务器")
    parser.add_argument("--watch", action="store_true", help="监听文件变化自动重建")
    args = parser.parse_args()
    print("=" * 60)
    print(f"  {SITE['title']} - 静态站点构建器")
    print("=" * 60)
    success = build(clean=args.clean)
    if success and args.serve:
        serve()
    elif success and args.watch:
        watch()

if __name__ == "__main__":
    main()
