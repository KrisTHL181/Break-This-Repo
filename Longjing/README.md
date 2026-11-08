# 龙井 - TeaCon Continuous Delivery Service

We leverage GitHub Actions to provide continuous integration and delivery of all TeaCon 2026 participant mods.

## 工作流程 Workflow

  1. 使用 GitHub Action 的 Cron Trigger，每半小时触发一次轮询。
     轮询的 Workflow 由 `.github/workflows/query.yaml` 描述。
  2. 轮询的过程为访问碧螺春的 API，获取当前有效报名的列表。
     通过 API 拿到报名信息中的队伍名、模组名和 Git Repo 地址，生成对应的信息。
     对于新报名的队伍，将会在 `.github/workflows/` 目录下创建新的 Workflow，并在 `mods` 下创建对应的目录。
     对于已有信息的队伍，将会更新其在 `mods` 目录下的信息（Git Repo 地址、HEAD shasum 等）
  3. 将新建或更新的构建信息推送回本仓库，触发构建和发布。
     构建时，`setup.gradle` 会作为参赛项目的 [Init Script][ref-2] 注入到构建流程中。
  4. 构建成功后，龙井会找出正确的构建产物，并从碧螺春获取 Mod 前置，并将这些模组一并传入 
     [Dedicated Server Launch Test][ref-3]（下简称 DSLT）中，在 CI 环境下启动一次服务器，
     以确认其可正常在服务器环境中启动。
  5. 通过 DSLT 测试后，龙井会执行 `publish.sh` 脚本，将构建产物上传至 TeaCon 归档服务，
     并通知碧螺春有新构建产生。
  6. 对于未能完整通过构建的模组，龙井会执行 `failure-callback.sh` 脚本，透过碧螺春向对应报名队伍的队长发送电邮通知，
     告知其构建失败。

[ref-2]: https://docs.gradle.org/current/userguide/init_scripts.html
[ref-3]: https://github.com/teaconmc/dedicated-server-launch-test

## 项目结构 Structure

### `fetch/query.py`

轮询脚本的主体。

Main/entry script of the scheduled query.

### `fetch/workflow_template.yaml`

轮询时自动创建的新 GitHub Action Workflow 的模板，基于 Python 的 [`string.Template`][ref-3]。

Template used for automatically creating new GitHub Action Workflows duringa query.
Based on Python's [`string.Template`][ref-3].

[ref-3]: https://docs.python.org/3/library/string.html#string.Template

### `fetch/auth.sh`

用于 GitHub App 的 Authentication。
在 GitHub Action 中，为防止意外触发无限递归的 Workflow Run，GitHub 默认提供的 `${{ github.TOKEN }}` 不能用于创建新的 Workflow
（即没有 `workflow:write` 权限）。
为此我们需要使用其他身份来推送自动创建的 Workflow，这里我们使用的是我们自己的 GitHub App。

Used for GitHub App Authentication.
In GitHub Action, the default, GitHub-provided `${{ github.TOKEN }}` cannot be used for creating new workflows
(i.e. does not have `workflow:write` permission), for sake of avoiding accidential recursive workflow runs.
To automatically create new workflows, we need to autheticate as other identities; here, we authenticate as
our GitHub App.

### `fetch/commit_info.sh`

设定创建并推送新提交时所用的作者信息。

Set author info used for creating and pushing new commits.

### `mods/teacon-2026-[team-id]/remote`

指定队伍的 Git 仓库的地址。

Address (URI) pointed to the git repo of the designated team.

### `mods/teacon-2026-[team-id]/HEAD`

指定队伍的 Git 仓库的最新提交的校验和。

Checksum of the git repo of the designated team.

### `build/build.sh`

启动 Gradle 构建所需的核心脚本。

Core script used for authoring a Gradle build.

### `build/setup.gradle`

构建参赛项目时使用的 [Gradle Init Script][ref-2]，用于：

  - 确定输出任务
  - 检查输出文件尺寸是否合规

[Gradle Init Script][ref-2] used for:

  - Locating the output task
  - Validate that built artifact size is in compliance.

### `build/mods_toml.py`

检查构建产物的 Mod ID 是否与碧螺春中登记的一致的脚本。

Script that checks whether the mod ID extracted from build artifact is the same as what we record in Biluochun.

### `build/pre-server-test.sh`

在 DSLT 启动之前的脚本，用于下载所有必须前置模组。

Script run before DSLT, for fetching necessary dependencies.

### `build/publish.sh`

参赛项目构建成功后使用的脚本，用于：

  - 上传构建产物至 TeaCon 归档服务
  - 通知碧螺春新构建已上传
  - 将下载链接展示在 Workflow Run 中的 Annotation 中方便有兴趣的玩家下载试玩。

Script used after successfully building participating projects. It is responible of 

  - Upload build artifact to TeaCon Archive Service
  - Notify Biluochun that new build is available
  - Displaying download link as an annotation in the corresponding workflow run, for those who are interested.

## 问题反馈 Reporting issue

构建失败不一定意味着被构建的项目有问题，还有可能是龙井系统本身的问题。这样的情况已经出现过不止一次了。
如果你确定龙井本身有问题，欢迎在我们的问题追踪器内反馈。或是直接向我们发起 PR。

A build failure does not implies that the project being built has issue(s); it could very well be an issue inside Longjing.
There have been cases where issues from Longjing itself cause build failures.
If you believe that Longjing itself is causing issues, feel free to report in our issue tracker.
Pull requests are more than welcome.

## 授权许可 License

[BSD-3-Clause](./LICENSE)
