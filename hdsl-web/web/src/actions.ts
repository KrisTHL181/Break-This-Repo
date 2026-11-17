import { api } from "./api";
import { beginLaunch, consumeLaunchPopup, forgetLaunch } from "./launch";
import { requestDeveloperPassword, isElevationRequired } from "./elevation";
import { I18N } from "./i18n";
import { patchInstanceLocal, refreshExternal, seedTasks, toast } from "./store";
import type { Instance } from "./types";
import { errMsg } from "./utils";

/**
 * 跑一个可能被开发者守卫挡住的请求：被挡住时弹一次开发者密码框，成功后重试一次。
 *
 * 只对 `developer_elevation_required` / `developer_password_not_set` 弹框——别的拒绝
 * （没有 HTTPS、终端已关闭、正在紧急停机）弹密码框没有意义，照旧把错误交给调用方。
 */
async function withElevation<T>(call: () => Promise<T>): Promise<T> {
  try {
    return await call();
  } catch (e) {
    if (!isElevationRequired(e)) throw e;
    const elevated = await requestDeveloperPassword();
    if (!elevated) throw e;
    return await call();
  }
}

/**
 * 启动流程：
 * 1. 点击瞬间同步 window.open("about:blank")（popup blocker 豁免）并保存引用；
 * 2. POST launch；成功后实例状态乐观置为 STARTING（后端回答 installing 时说明
 *    该实例还没装：它会先装再启动，这里就置 INSTALLING，弹窗继续等 RUNNING）；
 * 3. store 收到 RUNNING + url 的 WS 事件后自动把弹窗导航到 /i/<id>/?token=…。
 */
export async function launchInstance(inst: Instance, onSubmitted?: () => void): Promise<boolean> {
  const win = beginLaunch(inst.id);
  try {
    const r = await withElevation(() => api.launch(inst.id));
    const installing = r.state === "installing";
    patchInstanceLocal(inst.id, { state: installing ? "INSTALLING" : "STARTING" });
    toast(
      "info",
      installing
        ? `「${inst.name}」尚未安装 dsh，正在自动安装，装好后会直接启动`
        : win
          ? `正在启动「${inst.name}」，就绪后将自动打开 dsh 标签页`
          : "正在启动…（请允许浏览器弹窗，以便就绪后自动打开 dsh）",
    );
    onSubmitted?.();
    return true;
  } catch (e) {
    if (win) win.close();
    forgetLaunch(inst.id);
    toast("error", `启动失败：${errMsg(e)}`);
    return false;
  }
}

export async function stopInstance(inst: Instance): Promise<boolean> {
  try {
    await withElevation(() => api.stop(inst.id));
    patchInstanceLocal(inst.id, { state: "STOPPING" });
    toast("info", "已发送停止指令");
    return true;
  } catch (e) {
    toast("error", `停止失败：${errMsg(e)}`);
    return false;
  }
}

export async function installVersion(inst: Instance, version: string): Promise<boolean> {
  try {
    await withElevation(() => api.install(inst.id, version));
    patchInstanceLocal(inst.id, { state: "INSTALLING" });
    void seedTasks();
    toast("info", `已开始安装 ${version}`);
    return true;
  } catch (e) {
    toast("error", `安装失败：${errMsg(e)}`);
    return false;
  }
}

/** [打开 dsh]：同步开 about:blank 规避弹窗拦截，拿到 url 后导航。 */
export function openDsh(instanceId: string): void {
  let win: Window | null = null;
  try {
    win = window.open("about:blank", "_blank");
  } catch {
    win = null;
  }
  // `/api/instances/<id>/open` 是写操作（它会把实例拉起来），守卫开着时要提权：
  // 弹一次密码框，成功后重试，而不是把"打开失败"丢给用户。
  withElevation(() => api.open(instanceId))
    .then((r) => {
      // r.url 已由后端带挂载点前缀（open 端点在 REST 边界加 base）
      if (win) {
        win.location.href = r.url;
      } else {
        window.open(r.url, "_blank");
      }
    })
    .catch((e) => {
      if (win) win.close();
      toast("error", `打开失败：${errMsg(e)}`);
    });
}

/**
 * 启动第三方品牌 / zcode 实例：
 * 1. 点击瞬间同步 beginLaunch(id) 拿 about:blank 弹窗（popup blocker 豁免）；
 * 2. launch 同步阻塞至就绪（最多 60s），返回 running 后取 open url 导航弹窗；
 * 3. 失败关闭弹窗并提示；结束后立即刷新跨品牌实例列表。
 */
export async function launchBrandInstance(
  brand: "kimi" | "opencode" | "claude-web" | "octop" | "pi-web" | "qwen" | "mimo" | "minimax" | "zcode" | "hermes",
  entry: { id: string; name: string },
): Promise<boolean> {
  beginLaunch(entry.id);
  try {
    let state: string;
    // 品牌 / ZCode 的启动与打开同样是写操作：守卫开着时会要开发者密码，
    // 这里弹一次、成功后重试，别把"启动失败"直接甩给用户。
    if (brand === "zcode") {
      const r = await withElevation(() => api.launchZcode(entry.id));
      state = r.state;
      if (state === "error") throw new Error(r.error ?? "未知原因");
    } else {
      const r = await withElevation(() => api.launchBrand(brand, entry.id));
      state = r.state;
      if (state === "error") throw new Error(r.error ?? "未知原因");
    }
    const target =
      brand === "zcode"
        ? (await withElevation(() => api.zcodeOpen(entry.id))).url
        : (await withElevation(() => api.openBrand(brand, entry.id))).url;
    const popup = consumeLaunchPopup(entry.id);
    toast("success", I18N["dsh.launch.ready"].replace("%s", entry.name));
    if (popup && !popup.closed) {
      popup.location.href = target;
    } else {
      window.open(target, "_blank", "noopener");
    }
    return true;
  } catch (e) {
    const popup = consumeLaunchPopup(entry.id);
    if (popup && !popup.closed) popup.close();
    forgetLaunch(entry.id);
    toast("error", `启动失败：${errMsg(e)}`);
    return false;
  } finally {
    void refreshExternal();
  }
}

export async function stopBrandInstance(
  brand: "kimi" | "opencode" | "claude-web" | "octop" | "pi-web" | "qwen" | "mimo" | "minimax" | "zcode" | "hermes",
  id: string,
): Promise<boolean> {
  try {
    if (brand === "zcode") await api.stopZcode(id);
    else await api.stopBrand(brand, id);
    toast("info", "已发送停止指令");
    void refreshExternal();
    return true;
  } catch (e) {
    toast("error", `停止失败：${errMsg(e)}`);
    return false;
  }
}
