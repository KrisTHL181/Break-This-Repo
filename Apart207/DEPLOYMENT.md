# EdgeOne 部署指南

## 1. 快速开始（本地开发）

```bash
# 安装依赖
npm install
pip install -r requirements.txt

# 构建网站
npm run build

# 本地预览（含 API 模拟）
npm run dev

# 或仅预览已构建的静态文件
npm run preview
```

访问 `http://localhost:8080`

## 2. 初始化创始人账号

```bash
npm run setup:founders
```

6位创始人账号自动创建，默认密码 `20766643`，登录后请及时修改密码并绑定邮箱。

## 3. 存储方案

### 默认：JSON 文件存储

数据存储在项目根目录 `data/` 文件夹，每个实体一个 JSON 文件：

```
data/
├── user/
│   ├── uid/
│   │   └── <uid>.json
│   ├── email/
│   └── username/
├── session/
│   └── <token>.json
├── forum/
│   └── post/
│       └── <id>.json
└── paper/
    └── draft/
        └── <slug>.json
```

敏感字段（密码哈希、令牌等）使用 AES-256-CBC 加密存储。

### 切换到 KV 存储（预留接口）

如需使用 EdgeOne KV 存储，设置环境变量：

```bash
export STORAGE_BACKEND=kv
export KV_NAMESPACE=apart207
```

然后在 EdgeOne 控制台创建 KV 命名空间并绑定到 Pages 项目。

> 注意：当前默认使用 JSON 文件存储，KV 为预留接口，需手动切换。

## 4. EdgeOne Pages 部署

### 4.1 创建项目

1. 登录 [EdgeOne 控制台](https://console.cloud.tencent.com/edgeone)
2. 进入「Pages」→「新建项目」
3. 关联 Gitee 仓库 `buelierm/Apart207`
4. 构建配置：
   - 安装命令：`python3 -m pip install -r requirements.txt`
   - 构建命令：`python3 build.py --clean`
   - 输出目录：`public`

### 4.2 环境变量

在 EdgeOne 控制台 → 项目设置 → 环境变量：

| 变量名 | 说明 | 必填 |
|--------|------|------|
| `GITEE_TOKEN` | Gitee 访问令牌（用于 git push 同步） | 是 |
| `GITEE_OWNER` | Gitee 仓库所有者（默认 buelierm） | 否 |
| `GITEE_REPO` | Gitee 仓库名（默认 Apart207） | 否 |
| `DATA_ENCRYPTION_KEY` | 数据加密密钥（生产环境务必修改） | 是 |
| `STORAGE_BACKEND` | 存储后端：`json`（默认）或 `kv` | 否 |
| `KV_NAMESPACE` | KV 命名空间（仅 STORAGE_BACKEND=kv 时需要） | 否 |

### 4.3 数据同步到 Gitee

上线后数据以 JSON 文件存储在 EdgeOne 运行时，需定时同步到 Gitee 仓库：

```bash
npm run sync
```

该命令执行：
1. `git pull` 拉取最新代码
2. `git add -A` 添加所有变更
3. `git commit -m "杂物：自动构建&修复了一些已知问题"`
4. `git push` 推送到 Gitee

建议配置定时任务（如每小时执行一次）。

Gitee 限流：从响应头 `X-RateLimit-*` 自动分析，分析失败时默认 0.5 QPS。

## 5. API 端点

### 认证
| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/auth/register` | 注册 |
| POST | `/api/auth/login` | 登录 |
| POST | `/api/auth/logout` | 登出 |

### 用户
| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/user/profile` | 获取个人信息 |
| PUT | `/api/user/profile` | 更新个人信息 |
| POST | `/api/user/verify` | 实名认证 |
| GET | `/api/user/notifications` | 通知列表 |
| POST | `/api/user/notifications?action=readAll` | 全部已读 |

### 论文
| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/paper/submit?slug=xxx` | 获取草稿 |
| POST | `/api/paper/submit` | 提交/发表论文（自动同步Gitee） |

### 论坛
| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/forum/post?page=1` | 帖子列表 |
| GET | `/api/forum/post?id=xxx` | 帖子详情 |
| POST | `/api/forum/post` | 发帖 |
| DELETE | `/api/forum/post?id=xxx` | 删除帖子 |
| GET | `/api/forum/comment?postId=xxx` | 评论列表 |
| POST | `/api/forum/comment?postId=xxx` | 发表评论 |
| POST | `/api/forum/like?type=post&id=xxx` | 点赞 |

### 管理（需管理员权限）
| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/admin?action=users` | 用户列表 |
| POST | `/api/admin?action=ban` | 封禁用户 |
| POST | `/api/admin?action=unban` | 解封用户 |
| POST | `/api/admin?action=promote` | 提升管理员（仅创始人） |
| POST | `/api/admin?action=demote` | 撤销管理员（仅创始人） |

## 6. 项目结构

```
Apart207/
├── node-functions/         # EdgeOne Node Functions (API, ESM)
│   ├── _lib/               # 共享库
│   │   ├── storage.js      # 存储抽象层（JSON/KV）
│   │   ├── common.js       # 通用工具
│   │   └── gitee.js        # Gitee 同步
│   └── api/                # API 端点
├── functions/              # 备用 Pages Functions (CommonJS)
├── content/                # 内容源文件
│   ├── papers/             # 学术论文
│   ├── finance/            # 财务数据
│   └── static/             # 动态页面（登录/注册/论坛等）
├── public/                 # 构建输出（部署目录）
├── data/                   # JSON 数据存储（gitignore）
├── scripts/                # 工具脚本
│   ├── dev-server.js       # 本地开发服务器
│   ├── sync-data.js        # 数据同步到 Gitee
│   └── setup-founders.js   # 创始人账号初始化
├── build.py                # 静态站点构建脚本
├── package.json            # Node 依赖和脚本
└── edgeone.config.js       # EdgeOne 配置
```

## 7. 注意事项

- `data/` 目录已加入 .gitignore，不会提交到仓库
- 密码使用 PBKDF2-SHA512 哈希，存储时额外 AES 加密
- 超过3年未登录的普通用户自动注销（创始人/no_time标签除外）
- 论坛 HTML/JS/CSS 自动转义，防止 XSS
- 论文发表后自动同步到 Gitee，下次构建生成静态页面
