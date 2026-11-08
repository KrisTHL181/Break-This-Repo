# 廉中自然科学局第207研究所官网

> 深耕数理化生、电子及信息学等理工科领域，致力于推动基础科学发展。

廉中自然科学局第207研究所（2606班）官方网站，基于 Markdown + Python 静态生成，支持论文展示、搜索、引用、数学公式、浅色/深色模式切换。

## 功能特性

- **Markdown 驱动**：所有内容以 Markdown 格式撰写，自动转换为静态 HTML
- **数学公式**：支持 LaTeX 语法（MathJax 渲染），行内 `$...$` 与块级 `$$...$$`
- **GitHub Flavored Markdown**：表格、任务列表、删除线、代码高亮等
- **全站搜索**：客户端实时搜索，支持 `Ctrl+K` 快捷键
- **浅色/深色模式**：一键切换，支持跟随系统
- **n级导航菜单**：可配置的多级下拉导航
- **响应式设计**：适配桌面、平板、手机
- **莫奈配色**：低饱和和谐配色，扁平简约现代化风格
- **静态部署**：生成纯静态文件，可直接部署到 Gitee Pages / EdgeOne 等平台
- **PDF 导出**：构建时自动为每个页面生成 PDF（Playwright + Chromium），支持数学公式渲染
- **公开财务**：支持年/月财务视图，自动计算收支、净利润、预算执行
- **发展历程**：S 形时间线展示重要节点
- **在线游戏**：内置 3D 拍照游戏《拍哇哇》，端口 4319

## 项目结构

```
Apart207/
├── build.py                  # 静态构建脚本 (本地使用，已 git 屏蔽)
├── run.bat                   # Windows 双击运行
├── run.sh                    # macOS / Linux 运行
├── site.config.json          # 站点配置 (标题、导航、构建参数)
├── ico.svg                   # 官方图标
├── content/                  # Markdown 内容源文件
│   ├── index.md              # 首页
│   ├── about.md              # 关于我们
│   ├── history.md            # 历史趣事
│   ├── team.md               # 团队成员
│   ├── contact.md            # 联系我们
│   ├── finance.md            # 公开财务（占位符，数据由脚本生成）
│   ├── timeline.md           # 发展历程
│   ├── credits.md            # 特别鸣谢
│   ├── finance/              # 财务数据目录
│   │   └── 2026/
│   │       └── 8.md          # 2026年8月财务数据
│   └── papers/               # 学术论文
│       ├── index.md          # 论文列表
│       └── template.md       # 论文模板
├── templates/                # HTML 模板
│   └── page.html             # 页面模板
├── assets/                   # 静态资源
│   ├── css/style.css         # 主样式 (莫奈配色)
│   └── js/                   # 交互脚本
├── public/                   # 构建输出 (部署目录)
├── functions/                # EdgeOne Pages Functions (API)
├── scripts/                  # 工具脚本
├── games/
│   └── paiwawa-main/         # 拍哇哇 3D 拍照游戏 (引用自 bramblex/paiwawa)
├── data/                     # JSON 数据存储 (gitignore)
└── .gitignore
```

## 快速开始

### 环境要求

- Python 3.8+
- pip 包：`markdown`、`Jinja2`、`PyYAML`、`Pygments`
- 可选：`pymdown-extensions`（增强 GFM 和数学公式支持）、`watchdog`（文件监听模式）
- PDF 生成：`playwright`（需运行 `playwright install chromium` 下载浏览器）

### 一键运行

**Windows**：双击 `run.bat`，在菜单中选择操作。

**macOS / Linux**：

```bash
chmod +x run.sh
./run.sh
```

### 命令行使用

```bash
# 增量构建 (仅重新生成变更的文件)
python build.py

# 全量重建 (清除输出目录后重新构建)
python build.py --clean

# 构建并启动本地预览服务器 (http://localhost:8000)
python build.py --serve

# 监听文件变化，自动重建
python build.py --watch
```

> 首次运行时，脚本会自动检查并安装缺失的 Python 依赖包。

### 在线游戏

本项目内置 3D 拍照游戏《拍哇哇》，基于 Three.js 开发。

**部署后访问**：`https://apart207.top/game/`

**游戏构建**（游戏源码修改后需重新构建并提交 dist/）：
```bash
cd games/paiwawa-main
npm run build
# 构建产物在 games/paiwawa-main/dist/，会被 build.py 复制到 public/game/
```

**本地开发**（热重载）：
```bash
npm run game
# 访问 http://127.0.0.1:4319/
```

**操作**：WASD/方向键移动，鼠标转向，Space 拍照，Shift 加速，R 回到起点，M 开关声音。

> 游戏引用自 [bramblex/paiwawa](https://github.com/bramblex/paiwawa)，基于 MIT 协议使用。游戏采用预构建方式部署，修改源码后需执行 `npm run build` 并提交 `dist/` 目录。

## 内容编写

### 新建页面

在 `content/` 目录下创建 `.md` 文件，文件路径对应网站 URL 路径。

例如：`content/papers/my-paper.md` → `public/papers/my-paper.html`

### Frontmatter

每个 Markdown 文件顶部可添加 YAML frontmatter：

```yaml
---
title: 论文标题
date: 2026-08-26
tags: [标签1, 标签2]
description: 页面描述，用于 SEO 和搜索摘要
mathjax: true  # 是否启用数学公式，默认 true
---
```

### 数学公式

```markdown
行内公式：$E = mc^2$

块级公式：
$$
\int_{-\infty}^{\infty} e^{-x^2} dx = \sqrt{\pi}
$$
```

### 代码块

    ```python
    def hello():
        print("Hello, 207!")
    ```

## 导航配置

编辑 `site.config.json` 中的 `navigation` 字段，支持多级菜单：

```json
{
  "navigation": [
    {
      "title": "学术论文",
      "path": "papers/index.html",
      "children": [
        { "title": "论文列表", "path": "papers/index.html" },
        { "title": "论文模板", "path": "papers/template.html" }
      ]
    }
  ]
}
```

## 公开财务

财务数据以 Markdown 表格格式存储在 `content/finance/<年份>/<月份>.md` 文件中，构建脚本自动解析并生成统计页面。

### 目录结构

```
content/finance/
└── 2026/
    ├── 8.md      # 2026年8月数据
    └── 9.md      # 2026年9月数据（新增月份时创建）
```

### 添加月度财务数据

在对应年份目录下创建 `<月份>.md` 文件，包含以下表格：

```markdown
# 2026年8月财务

## 支出

| 项目 | 金额（元） |
|------|-----------|
| 域名费用 | 48.00 |
| 内务费用 | 6.00 |

## 收入

| 项目 | 金额（元） |
|------|-----------|
| 预算收入 | 100.00 |

## 原始预算

| 项目 | 金额（元） |
|------|-----------|
| 域名 | 50.00 |
| 自由支配 | 50.00 |

## 内务费用明细

| 项目 | 数量 | 单位 | 单价（元） | 小计（元） |
|------|------|------|-----------|-----------|
| 垃圾桶 | 1 | 个 | 6.00 | 6.00 |

## 备注

工程项目费用包含：科学实验费用、实践工程费用等。
```

**自动计算项**：总支出、总收入、净利润、预算执行率、超支/结余金额。全年汇总由脚本自动计算。

**支出类别**（固定）：域名费用、服务器费用、公关费用、AI相关费用、仓库维护费用、成员工资、工程项目费用、内务费用、其他费用。

**收入类别**（固定）：一手工程承接、外包承接、工程售卖、收费咨询、代办理费用、内容授权费用、预算收入、其他收入。

**PDF 导出**：财务页面支持按年/月分别下载 PDF：
- 全年汇总：`downloads/finance/total_<年份>.pdf`
- 单月明细：`downloads/finance/month_<年份>_<月份>.pdf`

**数据保留**：构建时自动清理超过 5 年的旧财务数据。

**数据完整性**：页面底部显示各月份原始数据文件的 SHA-256 哈希值，可与 Gitee 仓库源文件比对，防止前端篡改。

## 发展历程

时间线数据在 `content/timeline.md` 中，以 HTML 结构编写，采用横向 S 形布局。

### 添加新节点

在 `.timeline-track` 内添加：

```html
<div class="timeline-item" id="tl-2026-09-01">
  <div class="timeline-dot"></div>
  <div class="timeline-card">
    <div class="timeline-date">2026.09.01</div>
    <div class="timeline-title">事件标题</div>
    <div class="timeline-desc">事件描述</div>
  </div>
</div>
```

同时在顶部 `.timeline-nav` 中添加快速跳转链接：

```html
<a href="#tl-2026-09">2026.09</a>
```

时间线自动交替上下排列（横向 S 形），支持横向滚动，移动端可滑动查看。

## 部署

### Gitee Pages

1. 运行 `python build.py --clean` 生成最新静态文件到 `public/` 目录
2. 将 `public/` 目录的内容推送到 Gitee 仓库
3. 在 Gitee 仓库的「服务」→「Gitee Pages」中配置部署

### EdgeOne（腾讯云）

1. 运行 `python build.py --clean` 生成静态文件
2. 将 `public/` 目录内容上传至 EdgeOne Pages
3. 确保 `index.html` 在部署根目录

## 团队成员

| 职位 | 姓名 |
|------|------|
| 第一技术及工程指导长官 | 陈益达 |
| 首席技术设计及工程师 | 罗逸琳 |
| 对外及对内协调总长 | 范凌 |
| 首席理论验证工程师 | 张铭业 |
| 交涉及工程资源管理总长 | 赖世博 |
| 首席技术及工程审查长 | 刘家成 |

## 联系方式

- 邮箱（推荐）：liymio@outlook.com
- 邮箱（备用）：yydshmcl@outlook.com
- Gitee：https://gitee.com/buelierm/Apart207/
- GitHub 组织：https://github.com/DevApotheosis/ 、https://github.com/thundercoolstudio
- 班级：廉中 2606班

## License

本项目采用 MIT License。
