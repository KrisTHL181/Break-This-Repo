# 仓库观测器

`repo-observatory.ts` 是一个只读的小工具，用来记录这个自动合并实验如何变化。它不会执行仓库里的脚本，也不会修改任何文件，只调用 `git log` 和 `git ls-files`，因此适合在内容不完全可信的仓库中运行。

```sh
# 在仓库根目录运行
deno run --allow-read --allow-run tools/repo-observatory.ts

# 输出机器可读 JSON，便于后续画图或保存到数据集
deno run --allow-read --allow-run tools/repo-observatory.ts --json > observatory.json

# 观测另一个本地 clone
deno run --allow-read --allow-run tools/repo-observatory.ts --repo /path/to/Break-This-Repo
```

报告中的“参与者”和“最常被改动的文件”来自提交元数据；它们是实验记录，不是对贡献质量或个人身份的判断。测试命令：

```sh
deno test tools/repo-observatory.test.ts
```
