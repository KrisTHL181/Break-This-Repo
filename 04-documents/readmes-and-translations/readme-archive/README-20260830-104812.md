## Break This Repository!

This repository automatically merges pull requests without conflicts.

Please note that the `.github` directory and the `README` file (specifically, the content preceding the WARNING line) are protected.

> [!WARNING]
> **From the next line onward, ALL TEXT IS SUBJECT TO EDITS VIA PULL REQUESTS AND SHOULD NOT be trusted.**

---
**🔒 PROTECTED ZONE ENDS HERE — DO NOT MODIFY ANYTHING ABOVE THIS LINE**

# Everything below is fair game!

关注[Kris谭](https://www.zhihu.com/people/KrisTHL181)喵，关注[Kris谭](https://www.zhihu.com/people/KrisTHL181)谢谢喵！

关注[淼畔](https://space.bilibili.com/550997092)喵！
关注[Silvaire](https://github.com/silvaire-qwq)喵！关注[Miracle](https://github.com/miralous/miracle)喵！关注[Lankshare](https://github.com/Lankshare/Data/issues/new?template=create-link.yaml)喵！

---
## 如何通过 GitHub 提交一个 Pull Request（PR）喵

下面是一份完整的文字教程喵，带你走完 `fork - clone - modify - submit` 的全流程喵。

### 1. Fork（分叉）原仓库喵

打开本仓库的 GitHub 页面喵，点击右上方灰色的 **Fork** 按钮喵，将仓库复制到你自己的 GitHub 账号名下喵。这样你就有了一份可以自由修改的副本，且不会影响原仓库喵。

### 2. Clone（克隆你的分叉）喵

在你自己账号的仓库页，点击绿色的 **Code** 按钮喵，复制 HTTPS 地址喵，然后在本地终端执行喵：

```bash
git clone https://github.com/<你的用户名喵>/<仓库名喵>.git
cd <仓库名喵>
```

> 建议先为克隆下来的仓库配置一个指向原仓库的 `upstream`喵，方便后续同步喵：
>
> ```bash
> git remote add upstream https://github.com/<原仓库作者喵>/<原仓库名喵>.git
> ```

### 3. 创建分支并 Modify（修改）喵

永远不要在 `main` 分支上直接修改，先创建一个新分支喵：

```bash
git checkout -b my-feature-branch
```

然后在本地编辑、添加或删除文件喵。修改完成后，把改动加到暂存区并提交喵：

```bash
git add .
git commit -m "描述这次修改的内容喵"
```

### 4. 同步上游（可选但推荐）喵

如果原仓库在你修改期间有新提交，先同步喵，避免 PR 出现冲突喵：

```bash
git fetch upstream
git rebase upstream/main   # 或者 git merge upstream/main 喵
git push
```

### 5. Submit（提交 Pull Request）喵

将你的分支推送到远端你 fork 的仓库喵：

```bash
git push origin my-feature-branch
```

回到 GitHub 上**你 fork 的仓库**页面喵，通常会看到一条提示喵，点击 **Compare & pull request**喵。确认比较的分别是原仓库的 `main` 与你的分支喵，填写标题和描述喵，点击 **Create pull request**喵。如果原仓库有 CI（比如本仓库的自动合并工作流），提交后等待检查即可喵。

### 6. 注意喵

- 每个 PR 只聚焦一个目的喵，方便维护者评审喵。
- 留意 `.github` 目录和 README 中受保护的部分（本仓库中 WARNING 行以上的内容）不要随意改动喵。
- 修改完记得 `git add` 并 `git commit`，否则改动不会进入 PR喵。

---

> 📝 本教程由 Deepseek V4 Flash Vision Exp 编写喵。

![L9](./L9.mp4)
