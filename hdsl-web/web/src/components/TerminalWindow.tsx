import { useCallback, useEffect, useRef, useState } from "react";
import type { Terminal as XTerm } from "@xterm/xterm";
import type { FitAddon as XFitAddon } from "@xterm/addon-fit";
import { api, ApiError } from "../api";
import { withBase } from "../base";
import type { TerminalOpen } from "../api";
import type { BrandId, TerminalKernel } from "../types";
import { I18N } from "../i18n";
import { Dialog } from "./Dialog";
import "@xterm/xterm/css/xterm.css";

/** 终端窗口的目标：一个品牌实例、一个 dsh 实例，或一个 ZCode 实例（实验性品类）。 */
export interface TerminalTargetInfo {
  kind: "brand" | "dsh" | "zcode" | "mc";
  brand?: BrandId;
  id: string;
  name: string;
}

/// 打开终端的前置步骤都画成对话框，只有真正拿到票据之后才换成终端窗口：
/// `opening`（申请中）、`password`（开发者密码）、`setup`（首次设置）、
/// `kernel`（Claude Web 选内核）；`connecting` 起才是终端。
type Phase =
  | "opening"
  | "password"
  | "setup"
  | "kernel"
  | "connecting"
  | "running"
  | "ended"
  | "detached"
  | "desktop"
  | "error";

/** 前置步骤：画对话框而不是终端窗口。 */
function isDialogPhase(phase: Phase): boolean {
  return phase === "opening" || phase === "password" || phase === "setup" || phase === "kernel";
}

/// 手机形态的功能键条（CSS 按屏幕宽度/指针类型决定可见性，桌面不占高度）。
/// 手机软键盘发不出控制键与方向键，按一下这里就直接发对应的 PTY 字节——协议里
/// WS 二进制帧就是原始字节（docs/terminal-protocol.md §3），所以不需要动服务端。
const MOBILE_KEYS: { label: string; seq: string; aria: string }[] = [
  { label: "Esc", seq: "\x1b", aria: "Escape" },
  { label: "Tab", seq: "\t", aria: "Tab" },
  { label: "Ctrl+C", seq: "\x03", aria: "Ctrl+C（中断当前程序）" },
  { label: "Ctrl+D", seq: "\x04", aria: "Ctrl+D（EOF / 退出会话）" },
  { label: "Ctrl+Z", seq: "\x1a", aria: "Ctrl+Z（挂起当前程序）" },
  { label: "↑", seq: "\x1b[A", aria: "上箭头" },
  { label: "↓", seq: "\x1b[B", aria: "下箭头" },
  { label: "←", seq: "\x1b[D", aria: "左箭头" },
  { label: "→", seq: "\x1b[C", aria: "右箭头" },
];

/** 原地终端（docs/web-shell.md）：xterm 覆盖层 + 服务端 PTY。
 *
 * 关窗只 detach，agent 继续跑；底栏「停止」才杀进程。刷新后重新打开会接回
 * 同一个会话并回放最近的输出（后端对运行中的会话直接发票据）。
 * 打开需要开发者密码提权：403 时先在这里输入一次。 */
export function TerminalWindow({ target, onClose }: { target: TerminalTargetInfo | null; onClose: () => void }) {
  const hostRef = useRef<HTMLDivElement>(null);
  /** 终端窗口容器：手机形态下把高度钉在 visualViewport 上（软键盘起落时跟随）。 */
  const winRef = useRef<HTMLDivElement>(null);
  const termRef = useRef<XTerm | null>(null);
  /** IME guards installed on the current terminal; teardown runs this before dispose. */
  const cleanupImeRef = useRef<(() => void) | null>(null);
  const fitRef = useRef<XFitAddon | null>(null);
  const wsRef = useRef<WebSocket | null>(null);
  const disposedRef = useRef(false);
  const phaseRef = useRef<Phase>("opening");
  const closeTimerRef = useRef<number | null>(null);
  const observerRef = useRef<ResizeObserver | null>(null);
  /** visualViewport 的 resize 监听句柄，teardown 时摘下。 */
  const viewportHandlerRef = useRef<(() => void) | null>(null);
  /** 父组件每次渲染都会传入新的 onClose；用 ref 兜住，挂载 effect 才不会跟着重连。 */
  const onCloseRef = useRef(onClose);
  /** 每次 openSession 自增；慢 POST 回来时若已过期，丢弃结果。 */
  const generationRef = useRef(0);
  /** 这轮选过的内核；断线重连时沿用它，不再问一遍。 */
  const kernelRef = useRef<string>("");

  useEffect(() => {
    onCloseRef.current = onClose;
  });

  const [phase, setPhaseState] = useState<Phase>("opening");
  const [message, setMessage] = useState("");
  const [exitCode, setExitCode] = useState<number | null>(null);
  const [password, setPassword] = useState("");
  const [setupAdmin, setSetupAdmin] = useState("");
  const [setupPassword, setSetupPassword] = useState("");
  const [setupConfirm, setSetupConfirm] = useState("");
  const [busy, setBusy] = useState(false);
  /** 已从服务端收到的终端字节数（KB），用于区分“没收到”和“收到但没渲染”。 */
  const [rxKb, setRxKb] = useState(0);
  const rxRef = useRef(0);
  /** 提权到期时间（epoch ms），null = 面板没报（未提权或读不到）。头部倒计时用。 */
  const [elevationExpiry, setElevationExpiry] = useState<number | null>(null);
  /** 每秒走一格的时钟，只驱动倒计时渲染。 */
  const [nowMs, setNowMs] = useState(() => Date.now());
  /** 票据到手、等终端窗口挂载完成后再接 WebSocket（帧要画进 xterm）。 */
  const [pending, setPending] = useState<TerminalOpen | null>(null);
  /** Claude Web 的两个内核（社区引擎 / 官方），只在需要选择时非空。 */
  const [kernels, setKernels] = useState<TerminalKernel[]>([]);
  const [kernelChoice, setKernelChoice] = useState<string>("");

  const setPhase = useCallback((next: Phase) => {
    phaseRef.current = next;
    setPhaseState(next);
  }, []);

  const teardown = useCallback(() => {
    if (closeTimerRef.current !== null) {
      window.clearTimeout(closeTimerRef.current);
      closeTimerRef.current = null;
    }
    observerRef.current?.disconnect();
    observerRef.current = null;
    if (viewportHandlerRef.current) {
      window.visualViewport?.removeEventListener("resize", viewportHandlerRef.current);
      viewportHandlerRef.current = null;
    }
    cleanupImeRef.current?.();
    cleanupImeRef.current = null;
    wsRef.current?.close();
    wsRef.current = null;
    termRef.current?.dispose();
    termRef.current = null;
    fitRef.current = null;
  }, []);

  /** 字节进 PTY：物理键盘经 xterm 的 onData，功能按钮经这里。都只发送、不在本地
   *  回显——画面全部来自远端 PTY，本地多写一次就会双倍字符。 */
  const sendText = useCallback((text: string) => {
    const ws = wsRef.current;
    if (ws && ws.readyState === WebSocket.OPEN) {
      ws.send(new TextEncoder().encode(text));
    }
  }, []);

  const connect = useCallback(async (data: TerminalOpen) => {
    const { Terminal } = await import("@xterm/xterm");
    const { FitAddon } = await import("@xterm/addon-fit");
    if (disposedRef.current || !hostRef.current || !data.ws) return;
    // A reconnect replaces the view: drop the old terminal/socket first so a
    // second canvas does not pile up in the same host.
    teardown();

    rxRef.current = 0;
    setRxKb(0);

    const term = new Terminal({
      cursorBlink: true,
      fontSize: 13,
      // A monospace stack that actually has CJK glyphs. Without one the browser
      // falls back to a proportional font for 中文 and its punctuation, whose
      // glyphs are wider than the two cells the terminal grid gives them — which
      // reads as clipped/overlapping characters. These families are monospaced
      // for CJK too, so a full-width glyph is exactly two ASCII cells.
      fontFamily:
        '"JetBrains Mono", "Cascadia Mono", "SFMono-Regular", Menlo, Consolas, '
        + '"Sarasa Mono SC", "Noto Sans Mono CJK SC", "Source Han Mono SC", '
        + '"Microsoft YaHei Mono", "WenQuanYi Micro Hei Mono", monospace',
      scrollback: 3000,
      theme: {
        background: "#1b1b1f",
        foreground: "#e6e1e5",
        cursor: "#d0bcff",
        selectionBackground: "#4a4458",
      },
    });
    const fit = new FitAddon();
    term.loadAddon(fit);
    term.open(hostRef.current);
    fit.fit();
    termRef.current = term;
    fitRef.current = fit;

    // ── IME guard ──────────────────────────────────────────────────────────
    // xterm 6 keeps a hidden <textarea> for input. Two upstream defects show up
    // through it, and both are worse in a full-screen TUI that writes frames
    // continuously:
    //  1. every terminal write while a composition is active re-anchors the
    //     composition/candidate box to the buffer cursor (xterm's CompositionHelper
    //     runs updateCompositionElements on every render). A streaming turn or a
    //     spinner therefore makes the Chinese candidate window drift;
    //  2. uppercase letters and spaces leave their character in that textarea; a
    //     later keyCode-229 key can re-emit the whole accumulated value through
    //     onData (xtermjs/xterm.js#6078). In the TUI that reads as an earlier
    //     block of text being pasted into the composer again.
    // Buffer PTY output while composing, and sweep the textarea once keys settle.
    // screenReaderMode is skipped: that mode deliberately keeps the textarea value
    // readable by assistive technology.
    let composing = false;
    /** PTY bytes received while the IME is composing, flushed on compositionend. */
    let pendingOutput: Uint8Array[] = [];
    let pendingBytes = 0;
    const MAX_PENDING_BYTES = 512 * 1024;
    let sweepTimer: number | undefined;
    const textarea = term.textarea;

    const flushOutput = () => {
      if (pendingOutput.length === 0) return;
      const chunks = pendingOutput;
      pendingOutput = [];
      pendingBytes = 0;
      for (const chunk of chunks) term.write(chunk);
    };
    const sweepTextarea = () => {
      if (term.options.screenReaderMode === true || composing) return;
      if (sweepTimer !== undefined) window.clearTimeout(sweepTimer);
      // xterm reads the textarea value in a setTimeout(0) when a composition
      // finalizes; clearing before that would eat the just-committed text.
      sweepTimer = window.setTimeout(() => {
        sweepTimer = undefined;
        if (composing || termRef.current !== term) return;
        const input = term.textarea;
        if (input === undefined || input.value === "") return;
        input.value = "";
        input.setSelectionRange(0, 0);
      }, 50);
    };
    const onCompositionStart = () => {
      composing = true;
      if (sweepTimer !== undefined) {
        window.clearTimeout(sweepTimer);
        sweepTimer = undefined;
      }
    };
    const onCompositionEnd = () => {
      composing = false;
      // xterm schedules its own commit in setTimeout(0); flush after it.
      window.setTimeout(() => {
        if (!composing && termRef.current === term) flushOutput();
      }, 0);
      sweepTextarea();
    };
    const onKeyUp = () => sweepTextarea();
    const onBlur = () => {
      if (!composing) return;
      composing = false;
      flushOutput();
    };
    textarea?.addEventListener("compositionstart", onCompositionStart);
    textarea?.addEventListener("compositionend", onCompositionEnd);
    textarea?.addEventListener("keyup", onKeyUp);
    textarea?.addEventListener("blur", onBlur);
    cleanupImeRef.current = () => {
      if (sweepTimer !== undefined) {
        window.clearTimeout(sweepTimer);
        sweepTimer = undefined;
      }
      textarea?.removeEventListener("compositionstart", onCompositionStart);
      textarea?.removeEventListener("compositionend", onCompositionEnd);
      textarea?.removeEventListener("keyup", onKeyUp);
      textarea?.removeEventListener("blur", onBlur);
      pendingOutput = [];
      pendingBytes = 0;
    };

    const protocol = window.location.protocol === "https:" ? "wss" : "ws";
    // The server returns a panel-internal path ("/ws/terminal/{ticket}").
    // withBase adds the public mount point ("/hdsl"), the same way every other
    // API and the status socket do; without it a sub-path deployment opens the
    // ticket successfully and then dials the wrong URL. The protocol allows an
    // absolute ws:// or wss:// answer too — prefixing that would produce
    // garbage, so it is used as it came.
    const wsUrl = /^wss?:\/\//i.test(data.ws)
      ? data.ws
      : `${protocol}://${window.location.host}${withBase(data.ws)}`;
    const ws = new WebSocket(wsUrl);
    ws.binaryType = "arraybuffer";
    wsRef.current = ws;

    term.onData((chunk) => sendText(chunk));
    term.onResize(({ cols, rows }) => {
      if (ws.readyState === WebSocket.OPEN) {
        ws.send(JSON.stringify({ type: "resize", cols, rows }));
      }
    });

    // fit() ran before the socket existed, so its resize was dropped. Send
    // the real size once the handshake completes; the PTY starts at 80x24.
    ws.onopen = () => {
      if (ws.readyState === WebSocket.OPEN) {
        ws.send(JSON.stringify({ type: "resize", cols: term.cols, rows: term.rows }));
      }
    };

    /** 收到二进制帧：计数并渲染。「已接收 N KB」用来区分「没收到字节」和
     *  「收到了但 xterm 没画出来」——这次空白屏就是后者。 */
    const acceptBytes = (bytes: Uint8Array) => {
      rxRef.current += bytes.byteLength;
      setRxKb(Math.floor(rxRef.current / 1024));
      if (composing) {
        pendingOutput.push(bytes);
        pendingBytes += bytes.byteLength;
        // A pathological stream must not grow without bound; flush at the cap
        // even mid-composition rather than losing output entirely.
        if (pendingBytes >= MAX_PENDING_BYTES) flushOutput();
        return;
      }
      term.write(bytes);
    };

    ws.onmessage = (event) => {
      if (typeof event.data === "string") {
        try {
          const control = JSON.parse(event.data) as {
            type?: string;
            code?: number;
            failedWithoutOutput?: boolean;
          };
          if (control.type === "ready") {
            setPhase("running");
            term.focus();
          } else if (control.type === "exit") {
            setExitCode(control.code ?? 0);
            term.write(`\r\n\x1b[90m${I18N["terminal.ended"]} (${control.code ?? 0})\x1b[0m\r\n`);
            if (control.failedWithoutOutput) {
              // The child never printed anything, so the empty screen is the failure and
              // not a viewer that missed the output — say what to do about it instead of
              // leaving a bare exit code.
              term.write(`\x1b[90m${I18N["terminal.failedWithoutOutput"]}\x1b[0m\r\n`);
            }
            setPhase("ended");
            ws.close();
            closeTimerRef.current = window.setTimeout(() => onCloseRef.current(), 3000);
          }
        } catch {
          term.write(event.data);
        }
      } else if (event.data instanceof ArrayBuffer) {
        acceptBytes(new Uint8Array(event.data));
      } else if (event.data instanceof Blob) {
        // binaryType is "arraybuffer", but a browser that ignored it must not
        // leave a silent black screen.
        void event.data.arrayBuffer().then((buffer) => acceptBytes(new Uint8Array(buffer)));
      }
    };
    ws.onerror = () => {
      if (disposedRef.current) {
        return;
      }
      // During "opening" the close handler below carries the stage-specific
      // explanation; flipping the phase here would hide it.
      if (phaseRef.current === "opening") {
        return;
      }
      setPhase("error");
      setMessage(I18N["terminal.error"]);
    };
    ws.onclose = (event) => {
      if (disposedRef.current) {
        return;
      }
      if (phaseRef.current === "running") {
        setPhase("detached");
      } else if (phaseRef.current === "opening") {
        // The ticket is already issued at this point, so a close before
        // "ready" is a channel problem, not a password one. Say which stage
        // failed: a sub-path proxy that drops the WebSocket prefix looks
        // exactly like this.
        setPhase("error");
        setMessage(
          "终端票据已获取，但 WebSocket 通道连接失败" +
            (event.code ? `（${event.code}${event.reason ? `：${event.reason}` : ""}）` : "") +
            "。请检查反向代理的子路径与 WebSocket Upgrade 配置。",
        );
      }
    };

    // 软键盘起落只改 visualViewport、不改 layout viewport（iOS 尤其如此），只看
    // 元素尺寸会漏掉。手机形态把窗口高度直接钉在可视高度上，fit() 随之重算行列；
    // 宽屏不动内联样式，高度交给 CSS 的 min(72vh, 760px)。
    const applyViewport = () => {
      const win = winRef.current;
      const vv = window.visualViewport;
      if (!win || !vv) return;
      if (window.matchMedia("(max-width: 720px)").matches) {
        win.style.height = `${Math.round(vv.height)}px`;
      } else {
        win.style.height = "";
      }
      fit.fit();
    };
    applyViewport();
    if (window.visualViewport) {
      viewportHandlerRef.current = applyViewport;
      window.visualViewport.addEventListener("resize", applyViewport);
    }

    const observer = new ResizeObserver(() => fit.fit());
    observer.observe(hostRef.current);
    observerRef.current = observer;
  }, [teardown, sendText, setPhase]);

  /// 申请票据并进入终端。前置步骤（密码 / 内核）已经走完时才调用。
  const openSession = useCallback(async (kernel?: string) => {
    if (!target) return;
    const generation = ++generationRef.current;
    if (kernel) kernelRef.current = kernel;
    setPhase("opening");
    setMessage("");
    try {
      const data =
        target.kind === "zcode"
          ? await api.openZcodeTerminal(target.id)
          : target.kind === "mc"
            ? await api.mc.consoleTicket(target.id)
            : target.kind === "brand"
              ? await api.openBrandTerminal(target.brand as BrandId, target.id, undefined, kernel)
              : await api.openDshTerminal(target.id);
      if (disposedRef.current || generation !== generationRef.current) return;
      if (data.mode === "desktop") {
        setPhase("desktop");
        return;
      }
      // 票据到手，但终端窗口还没挂载（对话框要换成终端），等 effect 接。
      setPending(data);
      setPhase("connecting");
    } catch (e) {
      if (disposedRef.current || generation !== generationRef.current) return;
      if (e instanceof ApiError && e.status === 403 && e.message.includes("web_shell_disabled")) {
        setPhase("error");
        setMessage("Web Shell 已在「设置 → 开发者模式」里关闭");
        return;
      }
      if (e instanceof ApiError && e.status === 403 && e.message.includes("insecure_transport")) {
        setPhase("error");
        setMessage("Web Shell 需要 HTTPS，请先在「设置 → HTTPS 证书」里启用");
        return;
      }
      if (e instanceof ApiError && e.status === 403) {
        setPhase("password");
        return;
      }
      if (e instanceof ApiError && e.status === 409 && e.message.includes("developer_password_not_set")) {
        setPhase("setup");
        return;
      }
      setPhase("error");
      setMessage(e instanceof Error ? e.message : String(e));
    }
  }, [target, setPhase]);

  /// Claude Web 有社区引擎和官方两个内核：提权之后让用户选一个，其余品牌
  /// 直接进终端。只有一个可用内核、或者查询失败时按服务端默认走，不挡路。
  const askKernel = useCallback(async (): Promise<boolean> => {
    if (!target || target.kind !== "brand" || target.brand !== "claude-web") return false;
    try {
      const info = await api.brandTerminalKernels(target.brand, target.id);
      const usable = info.kernels.filter((kernel) => kernel.available);
      if (usable.length < 2) return false;
      setKernels(usable);
      setKernelChoice(info.default ?? usable[0].id);
      setPhase("kernel");
      return true;
    } catch {
      return false;
    }
  }, [target, setPhase]);

  /// 挂载后第一步：先确认开发者密码的状态，再决定画哪一页。
  const begin = useCallback(async () => {
    if (!target) return;
    setPhase("opening");
    setMessage("");
    try {
      const status = await api.developerStatus();
      setElevationExpiry(status.expiresAt ? Date.parse(status.expiresAt) : null);
      if (!status.webShellEnabled) {
        setPhase("error");
        setMessage("Web Shell 已在「设置 → 开发者模式」里关闭");
        return;
      }
      if (!status.https) {
        setPhase("error");
        setMessage("Web Shell 需要 HTTPS，请先在「设置 → HTTPS 证书」里启用");
        return;
      }
      if (!status.devPasswordSet) {
        setPhase("setup");
        return;
      }
      if (!status.elevated) {
        setPhase("password");
        return;
      }
      if (await askKernel()) return;
      await openSession();
    } catch (e) {
      setPhase("error");
      setMessage(e instanceof Error ? e.message : String(e));
    }
  }, [target, askKernel, openSession, setPhase]);

  /// 提权成功之后：先问内核，再开终端。
  const afterElevation = useCallback(async () => {
    if (await askKernel()) return;
    await openSession();
  }, [askKernel, openSession]);

  /// 终端窗口挂载完成后再接 WebSocket——票据在对话框阶段就拿好了。
  useEffect(() => {
    if (phase !== "connecting" || !pending || !hostRef.current) return;
    const data = pending;
    setPending(null);
    void connect(data);
  }, [phase, pending, connect, setPhase]);

  useEffect(() => {
    if (!target) return;
    disposedRef.current = false;
    void begin();
    return () => {
      disposedRef.current = true;
      teardown();
    };
  }, [target, begin, teardown]);

  /// 每 30 秒对一次表、每秒走一格：提权会被其它页面延长、被使用滑动、
  /// 或悄悄到期，头部倒计时始终要以面板为准；读失败就保持现状。
  useEffect(() => {
    if (!target) return;
    const poll = window.setInterval(() => {
      api
        .developerStatus()
        .then((s) => setElevationExpiry(s.expiresAt ? Date.parse(s.expiresAt) : null))
        .catch(() => {});
    }, 30000);
    const tick = window.setInterval(() => setNowMs(Date.now()), 1000);
    return () => {
      window.clearInterval(poll);
      window.clearInterval(tick);
    };
  }, [target]);

  const submitPassword = async () => {
    if (!password) return;
    setBusy(true);
    setMessage("");
    try {
      const elevation = await api.elevateDeveloper(password);
      setElevationExpiry(elevation.expiresAt ? Date.parse(elevation.expiresAt) : null);
      setPassword("");
      await afterElevation();
    } catch (e) {
      setMessage(e instanceof ApiError && e.status === 429 ? I18N["terminal.password.locked"] : e instanceof Error ? e.message : String(e));
    } finally {
      setBusy(false);
    }
  };

  const submitSetup = async () => {
    if (!setupAdmin || !setupPassword || setupPassword !== setupConfirm) {
      setMessage(I18N["terminal.setup.mismatch"]);
      return;
    }
    setBusy(true);
    setMessage("");
    try {
      await api.setDeveloperPassword(setupAdmin, setupPassword, setupConfirm);
      const elevation = await api.elevateDeveloper(setupPassword);
      setElevationExpiry(elevation.expiresAt ? Date.parse(elevation.expiresAt) : null);
      setSetupAdmin("");
      setSetupPassword("");
      setSetupConfirm("");
      await afterElevation();
    } catch (e) {
      setMessage(e instanceof Error ? e.message : String(e));
    } finally {
      setBusy(false);
    }
  };

  const stopSession = async () => {
    if (!target) return;
    if (target.kind === "mc") {
      // MC 的「停止」是关服，属于功能页的动作（要提权、要停所有服），
      // 不能藏在控制台底栏里误点一次。这里只关窗口。
      onClose();
      return;
    }
    setBusy(true);
    try {
      if (target.kind === "zcode") {
        await api.closeZcodeTerminal(target.id);
      } else if (target.kind === "brand") {
        await api.closeBrandTerminal(target.brand as BrandId, target.id);
      } else {
        await api.closeDshTerminal(target.id);
      }
    } catch {
      // 进程可能已经退出；关闭窗口即可。
    } finally {
      setBusy(false);
      teardown();
      onCloseRef.current();
    }
  };

  if (!target) return null;

  /** 头部提权倒计时。提权过期只影响开新终端/重连，不动已连接的终端——
   *  agent 继续跑，这里只负责让用户心里有数，到期后重连时再验证即可。 */
  let elevationCountdown: { text: string; tone: "ok" | "soon" | "expired" } | null = null;
  if (elevationExpiry != null) {
    const left = elevationExpiry - nowMs;
    if (left <= 0) {
      elevationCountdown = { text: "提权已过期", tone: "expired" };
    } else {
      const totalSec = Math.floor(left / 1000);
      const h = Math.floor(totalSec / 3600);
      const m = Math.floor((totalSec % 3600) / 60);
      const s = totalSec % 60;
      const mm = m.toString().padStart(2, "0");
      const ss = s.toString().padStart(2, "0");
      elevationCountdown = {
        text: h > 0 ? `提权剩余 ${h}:${mm}:${ss}` : `提权剩余 ${m}:${ss}`,
        tone: left < 5 * 60_000 ? "soon" : "ok",
      };
    }
  }

  // 前置步骤画对话框：先密码（或首次设置）、必要时选内核，然后才换成终端窗口。
  if (isDialogPhase(phase)) {
    return (
      <Dialog
        open
        title={`${target.name} · ${I18N["terminal.title"]}`}
        onClose={busy ? undefined : onClose}
        closeOnBackdrop={!busy}
      >
        {phase === "opening" && (
          <div className="hint" style={{ display: "flex", alignItems: "center", gap: 10 }}>
            <span className="spinner" />
            <span>正在申请终端…</span>
          </div>
        )}

        {phase === "password" && (
          <div style={{ display: "flex", flexDirection: "column", gap: 8 }}>
            <div>{I18N["terminal.password.prompt"]}</div>
            <input
              className="input"
              type="password"
              autoFocus
              value={password}
              placeholder={I18N["terminal.password.placeholder"]}
              onChange={(e) => setPassword(e.target.value)}
              onKeyDown={(e) => {
                if (e.key === "Enter") void submitPassword();
              }}
            />
            {message && <div className="hint error">{message}</div>}
            <div className="dialog-actions">
              <button className="btn btn-text dim" disabled={busy} onClick={onClose}>
                {I18N["button.cancel"]}
              </button>
              <button className="btn btn-raised ripple-host" disabled={busy || !password} onClick={() => void submitPassword()}>
                {I18N["terminal.password.submit"]}
              </button>
            </div>
          </div>
        )}

        {phase === "setup" && (
          <div style={{ display: "flex", flexDirection: "column", gap: 8 }}>
            <div>{I18N["terminal.setup.prompt"]}</div>
            <input
              className="input"
              type="password"
              autoFocus
              placeholder={I18N["terminal.setup.panel"]}
              value={setupAdmin}
              onChange={(e) => setSetupAdmin(e.target.value)}
            />
            <input
              className="input"
              type="password"
              placeholder={I18N["terminal.setup.password"]}
              value={setupPassword}
              onChange={(e) => setSetupPassword(e.target.value)}
            />
            <input
              className="input"
              type="password"
              placeholder={I18N["terminal.setup.confirm"]}
              value={setupConfirm}
              onChange={(e) => setSetupConfirm(e.target.value)}
              onKeyDown={(e) => {
                if (e.key === "Enter") void submitSetup();
              }}
            />
            {message && <div className="hint error">{message}</div>}
            <div className="dialog-actions">
              <button className="btn btn-text dim" disabled={busy} onClick={onClose}>
                {I18N["button.cancel"]}
              </button>
              <button
                className="btn btn-raised ripple-host"
                disabled={busy || !setupAdmin || !setupPassword || !setupConfirm}
                onClick={() => void submitSetup()}
              >
                {I18N["terminal.setup.submit"]}
              </button>
            </div>
          </div>
        )}

        {phase === "kernel" && (
          <div style={{ display: "flex", flexDirection: "column", gap: 10 }}>
            <div>选择这次要进入的 Claude Code 内核：</div>
            {kernels.map((kernel) => (
              <label
                key={kernel.id}
                style={{ display: "flex", alignItems: "flex-start", gap: 8, cursor: "pointer" }}
              >
                <input
                  type="radio"
                  name="terminal-kernel"
                  checked={kernelChoice === kernel.id}
                  onChange={() => setKernelChoice(kernel.id)}
                />
                <span>
                  <span style={{ display: "block" }}>{kernel.label}</span>
                  {kernel.detail && (
                    <span style={{ display: "block", fontSize: 12, color: "var(--monet-on-surface-variant)" }}>
                      {kernel.detail}
                    </span>
                  )}
                </span>
              </label>
            ))}
            <div className="hint">
              网页里配置的第三方供应商只对社区引擎生效；官方内核只认 Anthropic 账号。
            </div>
            <div className="dialog-actions">
              <button className="btn btn-text dim" onClick={onClose}>
                {I18N["button.cancel"]}
              </button>
              <button
                className="btn btn-raised ripple-host"
                disabled={!kernelChoice}
                onClick={() => void openSession(kernelChoice)}
              >
                {I18N["terminal.open"]}
              </button>
            </div>
          </div>
        )}
      </Dialog>
    );
  }

  return (
    <div className="terminal-overlay" role="dialog" aria-label={`${target.name} ${I18N["terminal.title"]}`}>
      <div className="terminal-window" ref={winRef}>
        <div className="terminal-head">
          <span className="terminal-title">
            {target.name} · {I18N["terminal.title"]}
          </span>
          <span className={`terminal-state terminal-state-${phase}`}>
            {phaseText(phase)}
            {rxKb > 0 ? ` · 已接收 ${rxKb} KB` : ""}
          </span>
          {elevationCountdown && (
            <span
              className={`terminal-elevation terminal-elevation-${elevationCountdown.tone}`}
              title="提权只影响开新终端与重连；到期不会断开当前终端，重连时再验证一次即可"
            >
              {elevationCountdown.text}
            </span>
          )}
        </div>

        <div className="terminal-body" ref={hostRef} />

        <div className="terminal-keys" role="toolbar" aria-label="终端功能键">
          {MOBILE_KEYS.map((key) => (
            <button
              key={key.label}
              type="button"
              className="terminal-key"
              aria-label={key.aria}
              title={key.aria}
              // 不让按钮抢走 xterm 隐藏输入框的焦点（手机软键盘不会因此收起）；
              // 少数仍抢走焦点的情况在 click 里把焦点还回去。
              onPointerDown={(e) => e.preventDefault()}
              onClick={() => {
                sendText(key.seq);
                termRef.current?.focus();
              }}
            >
              {key.label}
            </button>
          ))}
        </div>

        {phase === "connecting" && <div className="terminal-note">{I18N["terminal.state.opening"]}</div>}
        {phase === "desktop" && <div className="terminal-note">{I18N["terminal.desktop"]}</div>}
        {phase === "error" && <div className="terminal-note error">{message || I18N["terminal.error"]}</div>}
        {phase === "detached" && <div className="terminal-note">{I18N["terminal.detached"]}</div>}
        {phase === "ended" && <div className="terminal-note">{I18N["terminal.ended"]} ({exitCode ?? 0})</div>}

        <div className="terminal-footer">
          {(target.kind !== "mc") && (phase === "running" || phase === "detached") && (
            <button
              className="btn btn-raised danger ripple-host"
              disabled={busy}
              onClick={() => void stopSession()}
            >
              {I18N["terminal.stop"]}
            </button>
          )}
          {(phase === "detached" || phase === "error") && (
            <button className="btn btn-text ripple-host" onClick={() => void openSession(kernelRef.current || undefined)}>
              {I18N["terminal.reconnect"]}
            </button>
          )}
          <span className="spacer" />
          <button className="btn btn-text ripple-host" onClick={onClose}>
            {I18N["terminal.close"]}
          </button>
        </div>
      </div>
    </div>
  );
}

function phaseText(phase: Phase): string {
  switch (phase) {
    case "opening":
      return I18N["terminal.state.opening"];
    case "password":
    case "setup":
      return I18N["terminal.state.locked"];
    case "kernel":
      return I18N["terminal.state.kernel"];
    case "connecting":
      return I18N["terminal.state.opening"];
    case "running":
      return I18N["terminal.state.running"];
    case "ended":
      return I18N["terminal.state.ended"];
    case "detached":
      return I18N["terminal.state.detached"];
    case "desktop":
      return I18N["terminal.state.desktop"];
    default:
      return I18N["terminal.state.error"];
  }
}
