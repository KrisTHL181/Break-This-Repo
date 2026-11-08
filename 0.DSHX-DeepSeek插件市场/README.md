<div align="center">

# DSHX

**Build, check, and ship typed DeepSeek Harness plugins.**

为 [DeepSeek Harness](https://github.com/deepseek-ai/DeepSeek-Harness) 插件开发准备的类型化工具链

[🌐 官网 / 插件 Hub](https://dshx.io) · [💻 源码](https://github.com/liyown/dshx) · [📦 npm](https://www.npmjs.com/package/@becomeopc/dshx) · [📚 文档](https://github.com/liyown/dshx/blob/main/docs/index.md)

</div>

---

## 这是什么

DSHX 是一个面向 **out-of-tree（仓库外）DeepSeek Harness 插件** 的构建时工具链。

它负责「写代码」这一段：类型化的 Host / Client 编写、有边界的 Vite 构建、离线诊断、Profile 感知的开发模式，以及可复现的项目模板。

至于插件如何执行、注册、通信、持久化、装配、HMR 与销毁 —— 仍然交给官方的 DSH / Cordis 服务。

> DSHX 管开发体验，Harness 管运行时。

---

## 快速开始

```bash
pnpm create dshx@preview my-plugin
cd my-plugin
pnpm check
pnpm dev
```

默认生成一个最小可用项目（`starter` + CSS Modules），包含一个 Host Tool 和一个可见的 Client Slot。

需要完整示例？用 `showcase` 模板并加上 Tailwind：

```bash
pnpm create dshx@preview my-plugin --template showcase --style tailwind
```

| 选项 | 可选值 |
| --- | --- |
| `--template` | `starter` \| `showcase` |
| `--style` | `css-modules` \| `tailwind` \| `none` |

---

## 核心能力

- **类型化 Host / Client**：`defineHost`、`defineApi`、`defineClient`、`defineSlot`、`defineLocale`，端到端类型推导
- **有边界的 Vite 构建**：类型声明走 Vite 管线编译，产物遵循官方加载器约定
- **Client HMR + Host 重启编排**：改前端立即热更新，改宿主自动重启
- **Profile 感知开发**：用真实的 DSH Profile 调试插件，而不是模拟环境
- **离线诊断**：`pnpm check` 在不启动 Harness 的情况下检查配置、API 与插槽
- **插件 Hub**：在 [dshx.io](https://dshx.io) 浏览带版本、兼容性、维护者、指标与评价的插件目录

---

## 写一个插件长什么样

```ts
// src/api/status.ts
import { defineApi, method } from "@becomeopc/dshx/api";

export const statusApi = defineApi({
  id: "status",
  version: 1,
  methods: {
    get: method<void, { readonly ready: boolean }>(),
  },
});
```

```ts
// src/host.ts
import { defineHost } from "@becomeopc/dshx/host";
import { statusApi } from "./api/status.js";

export default defineHost({
  apis: [statusApi.host({ get: () => ({ ready: true }) })],
});
```

```tsx
// src/client.tsx
import {
  defineClient,
  defineLocale,
  defineSlot,
  useApiQuery,
  type PropsLocaleOf,
} from "@becomeopc/dshx/client";

const copy = defineLocale("my-plugin.status", {
  zh: { ready: "就绪", unavailable: "不可用" },
  en: { ready: "Ready", unavailable: "Unavailable" },
});

function Status({ t }: PropsLocaleOf<typeof copy>) {
  const query = useApiQuery(statusApi, "get", { enabled: true });
  if (query.status === "pending") return <p>{query.fetchStatus}</p>;
  if (query.status === "error") return <button onClick={query.refetch}>Retry</button>;
  return <p>{t(query.data.ready ? "ready" : "unavailable")}</p>;
}
```

---

## 参考实现

[`@becomeopc/dshx-plugin-marketplace`](https://github.com/liyown/dshx/tree/main/packages/plugin-marketplace) 是官方的自举参考插件 —— 把 **设置 → 插件 → 插件市场** 当作一个普通 DSH bundle 挂载，一次性用上了 `defineHost`、`defineSettings`、`defineApi`、`defineClient`、`defineLocale`、`defineSlot`、标准 Schema 校验、`useApiQuery`、CSS Modules、Profile 安装与 Client HMR。

---

<div align="center">

**用 TypeScript 写 DeepSeek Harness 插件，从 DSHX 开始。**

[dshx.io](https://dshx.io) · [GitHub](https://github.com/liyown/dshx) · [npm](https://www.npmjs.com/package/@becomeopc/dshx)

<sub>Made by <a href="https://liuyaowen.cn">Evan / liyown</a></sub>

</div>
