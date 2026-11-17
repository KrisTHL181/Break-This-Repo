import { useEffect, useState } from "react";
import { api, type NodeStatus } from "../api";
import { toast } from "../store";
import { errMsg } from "../utils";
import { WarningIcon } from "./icons";

/** 裸部署（只用 jar）时最常缺的东西：Node.js，或者 Node.js 有但 pnpm 没有。
 *
 *  面板能自己装（托管运行时 + pnpm），但入口原来只藏在「设置 → Node.js」里，
 *  用户在实例页点「创建」只会拿到一条安装失败的错误。这条提示把两件事说清楚：
 *  缺什么，以及一键装完就是好的。
 *
 *  只在**真的缺**时出现：系统 PATH 里有可用的 node + pnpm，或者已装好一个带
 *  pnpm 的托管运行时，都不显示。 */
export function ToolchainBanner() {
  const [status, setStatus] = useState<NodeStatus | null>(null);
  const [installing, setInstalling] = useState<string | null>(null);
  const [msg, setMsg] = useState("");
  const [dismissed, setDismissed] = useState(false);

  const reload = () => void api.nodeStatus().then(setStatus).catch(() => setStatus(null));

  useEffect(() => {
    reload();
  }, []);

  // 任务轮询：node-install 走 TaskService，完成后把状态刷新一遍（提示条自己会消失）
  useEffect(() => {
    if (!installing) return;
    const timer = setInterval(() => {
      void api
        .tasks()
        .then((tasks) => {
          const t = tasks.find((x) => x.id === installing);
          if (!t) return;
          setMsg(t.message ?? t.state);
          if (t.state === "done" || t.state === "failed") {
            setInstalling(null);
            setMsg("");
            toast(t.state === "done" ? "success" : "error", t.message ?? t.state);
            reload();
          }
        })
        .catch(() => undefined);
    }, 1000);
    return () => clearInterval(timer);
  }, [installing]);

  const install = async (source: "mirror" | "official") => {
    try {
      const r = await api.nodeInstall({ source });
      setInstalling(r.taskId);
      setMsg("正在下载…");
    } catch (e) {
      toast("error", `启动安装失败：${errMsg(e)}`);
    }
  };

  if (dismissed || status === null) return null;

  // `system` is spelled `null` by the contract, but a panel from before that was
  // fixed (or any proxy that eats nulls) can omit the key — and an omitted key
  // is `undefined`, which slips past a `!== null` test and crashes the page on
  // `system.pnpm`. Normalise once, here.
  const system = status.system ?? null;
  const systemOk = system !== null && system.pnpm !== undefined && supports(system.nodeVersion);
  const managedOk = (status.managed ?? []).some((r) => r.suitable && r.pnpm);
  if (systemOk || managedOk) return null;

  let reason: string;
  if (system === null) {
    reason = "这台机器上没有 Node.js。安装 dsh 版本、插件与品牌工具都要它（node + npm + pnpm）。";
  } else if (system.pnpm === undefined) {
    reason = `有 Node.js ${system.nodeVersion}，但没有 pnpm —— 安装 dsh 版本需要 pnpm。`;
  } else {
    reason = `系统 Node.js ${system.nodeVersion} 不在 dsh 要求的范围内（${status.requirement}）。`;
  }

  return (
    <div className="hint warning" style={{ alignItems: "center", flexWrap: "wrap", gap: 8 }}>
      <WarningIcon size={16} />
      <span style={{ flex: 1, minWidth: 200 }}>
        {reason} 面板可以自己装一份（含 pnpm，存在数据目录里，不动系统）：
      </span>
      <button
        className="btn btn-raised ripple-host"
        style={{ height: 28 }}
        disabled={installing !== null}
        onClick={() => void install("mirror")}
      >
        {installing ? "安装中…" : "一键安装（国内镜像）"}
      </button>
      <button
        className="btn btn-text ripple-host"
        style={{ height: 28 }}
        disabled={installing !== null}
        onClick={() => void install("official")}
      >
        官方源
      </button>
      <button className="btn btn-text ripple-host" style={{ height: 28 }} onClick={() => setDismissed(true)}>
        关闭
      </button>
      {msg && <span style={{ width: "100%", fontSize: 12, opacity: 0.85 }}>{msg}</span>}
    </div>
  );
}

/** dsh 的 Node 要求：`^22.19.0 || >=24.0.0`。
 *
 *  只解析 major/minor 就够：这个判断只决定提示条要不要出现，真正的把关在服务端。 */
function supports(version: string): boolean {
  const m = /^v?(\d+)\.(\d+)/.exec(version.trim());
  if (!m) return false;
  const major = Number(m[1]);
  const minor = Number(m[2]);
  if (major >= 24) return true;
  return major === 22 && minor >= 19;
}