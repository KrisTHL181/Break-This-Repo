# 大一班会 · 文字冒险游戏

面向大一班会的 Streamlit 文字冒险互动网页，适合教室投影使用。AI 担任游戏主持人（GM），围绕**期末诚信考试**与**正确成长观**推进剧情，全班可通过选项或自定义输入共同决策。

## 快速开始

```bash
pip install -r requirements.txt
streamlit run app.py
```

浏览器打开 `http://127.10.8.1:8501`，在侧边栏填写接口密钥即可开始。

## 上传到 GitHub

本地 Git 仓库已初始化并完成首次提交。按以下步骤推送到 GitHub：

### 方式一：使用 GitHub CLI

在终端进入项目目录，依次执行：

```bash
gh auth login
```

按提示选择 GitHub.com → HTTPS → 浏览器登录授权。

登录成功后创建仓库并推送：

```bash
cd "%USERPROFILE%\Desktop\新建文件夹\团会"
gh repo create 团会 --public --source=. --remote=origin --push --description "大一班会文字冒险 Streamlit 互动游戏"
```

> 仓库名为 `团会`；若 GitHub 上已有同名仓库，换一个即可。

### 方式二：在网页 GUI 创建（推荐）

1. 打开 [github.com/new](https://github.com/new)，新建空仓库（不要勾选 README）
2. 在终端执行（将 `你的用户名` 和 `仓库名` 替换为实际值）：

```bash
cd "%USERPROFILE%\Desktop\新建文件夹\团会"（或%HOMEPATH%）
git remote add origin https://github.com/keil510086-commits/-.git
git push -u origin main
```

## 在线部署（Streamlit Cloud）

1. 将本仓库推送到 GitHub
2. 打开 [share.streamlit.io](https://share.streamlit.io)，用 GitHub 登录
3. 选择本仓库，主文件填 `app.py`，点击 Deploy
4. 部署完成后会获得 `https://xxx.streamlit.app` 网址，可直接分享

> 接口密钥建议在 Streamlit Cloud 的 `Secrets` 中配置，**不要写入代码**。

## 详细说明

请参阅 [操作指南](./操作指南.md)。

## 文件说明

| 文件 | 说明 |
|------|------|
| `app.py` | 主程序 |
| `requirements.txt` | Python 依赖 |
| `操作指南.md` | 完整操作指南 |
