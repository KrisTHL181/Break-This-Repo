# Break This Repo — 保护区被绕过的实证 (PoC)

> 这份 README **本身**就是证据。它位于 `PROTECTED ZONE ENDS HERE` 标记线**以上**的
> "受保护区"里,却被一个普通 PR 改掉了 —— 说明 `.github/workflows/auto-merge.yml`
> 的保护逻辑存在可绕过的漏洞。原始 README 已备份到 [`README.backup.md`](./README.backup.md)。

---

## 漏洞 (Trigger)

自动合并脚本用下面这行获取一个 PR 改动了哪些文件,再据此判断是否碰了保护路径:

```bash
changed_paths() {
  gh pr view "$1" --json files --jq '.files[] | (.path, (.previous_filename // empty))'
}
```

**`gh pr view --json files` 走 GraphQL 的 `files(first: 100)`,只返回前 100 个文件、且不翻页。**
一旦 PR 的改动文件超过 100 个,第 101 个及之后的文件**根本不出现在输出里**。

而 README 的全部保护检查都挂在这个判断上:

```bash
if grep -qx 'README.md' < <(changed_paths "$pr"); then
    # 只有 README.md 出现在列表里, 才会跑 marker / 删除 / 保护区 diff 检查
fi
```

如果 `README.md` 被挤出前 100 条 → `grep -qx 'README.md'` 匹配不到 →
**整段保护检查被跳过** → `touches_protected` 判定"未触碰保护" → PR 被自动合并,
哪怕它改的正是标记线以上的保护内容。

## 触发方式 (Reproduction)

1. 在 PR 里加入 **100 个"配重"文件**,文件路径要按字节序**排在 `README.md` 之前**。
   本仓库用的是 [`0pad/001.txt` … `0pad/100.txt`](./0pad/)。
   之所以能排在前面:ASCII 里 `'0'` = 48 < `'R'` = 82,所以 `0pad/...` 恒排在 `README.md` 之前
   (换成数字、`.`、或大写 `A`–`Q` 开头都行;就算 GitHub 用大小写不敏感排序,数字也永远排在字母前)。
2. 同一个 PR 里**修改 `README.md`**(本例:重写了这段受保护区)。
3. `gh pr view --json files` 只回前 100 个配重文件,列表里**看不到 `README.md`** →
   marker 检查、删除检查、保护区 diff 检查**全部跳过** → 无冲突 → 自动合并。

全程**不需要碰 `.github/`**。`.github/workflows/*` 另有一道平台级保护
(`GITHUB_TOKEN` 缺少 `workflows` 权限,无法合并 workflow 文件改动),但 `README.md`
不是 workflow 文件,不受那道墙约束,所以这条路直通保护区。

## 修复方法 (Fix)

不要用有 100 条上限的 `gh pr view --json files`,改用会翻页 / 无上限的取法:

```bash
# 方案 A: 让 REST files 端点翻页, 取全量文件名
changed_paths() {
  gh api --paginate "repos/$GITHUB_REPOSITORY/pulls/$1/files" --jq '.[].filename'
}

# 方案 B: 直接读 diff 的文件名(不受 100 上限影响)
changed_paths() {
  gh pr diff "$1" --name-only
}
```

这样文件数不再有天花板,配重攻击立即失效。`.github/` 那道平台级 `workflows` 权限墙
本来就已足够稳固,无需改动。

---
**🔒 PROTECTED ZONE ENDS HERE — DO NOT MODIFY ANYTHING ABOVE THIS LINE**

<sub>本 PoC 的存在本身证明了上面这行"保护标记"及其之上的所有内容都是可被绕过修改的。</sub>
