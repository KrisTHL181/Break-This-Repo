/**
 * Deciding whether an approval has to be shown.
 *
 * The harness asks the terminal to approve a tool call (or a plan) and waits for
 * one of `allowed-once` / `rejected`. It has no notion of "run everything" — its
 * own policy switch only offers "ask" and "never", and `never` **rejects** — so a
 * mode that stops asking is a decision this terminal makes, per request, before
 * the dialog is drawn. That decision lives here: one pure function, one list of
 * rules, and a test file next to the terminal that uses it.
 *
 * The modes mirror the three Kimi Code permission modes:
 *
 * - `ask` (default): read-only tools run, everything with a side effect asks;
 * - `critical`: routine edits and commands run, dangerous/sensitive ones ask;
 * - `full`: everything runs except plan/question decisions.
 *
 * The bias is deliberate: anything not recognised is asked about. A wrong "ask"
 * costs one keystroke; a wrong "allow" runs `rm -rf` without one.
 *
 * @module approval/policy
 */

import { isAbsolute, relative as relativePath, resolve as resolvePath } from 'node:path'

/** The three modes, in the order `/yolo` cycles them. */
export const APPROVAL_MODES = ['ask', 'critical', 'full']

/** The mode a fresh session starts in — Kimi Code's "Always Ask" default. */
export const DEFAULT_APPROVAL_MODE = 'ask'

/** Shells a command may arrive through, by tool name. */
const SHELL_TOOLS = /bash|pwsh|powershell|shell|exec|terminal|run_command|run\b/iu

/** File-writing tools, by tool name. */
const WRITE_TOOLS = /write|edit|replace|patch|create|delete|remove|move|rename|copy|mkdir|touch|apply|insert|append/iu

/**
 * Tools that only read: these are the ones Always Ask does not interrupt.
 *
 * Network reads are deliberately **not** here: `web_fetch` is a read of a URL
 * that the model composes, so it can carry the workspace out in a query string.
 * It costs one dialog and stays a read in every other respect (the sandbox does
 * not apply to it, which is exactly why the dialog does).
 */
const READ_TOOLS = /read|view|list|search|grep|glob|find|stat|ls\b|cat\b|head\b|tail\b|wc\b|diff\b|show\b/iu

/** Tools whose call is a decision about what to do, never an automated action. */
const PLAN_TOOLS = /plan|present|goal|question|ask_user/iu

/** Tools that carry a plan to review, as opposed to a question. */
const PLAN_CARD = /present_plan|exit_plan_mode|plan_mode|(^|_)plan($|_)/iu

/** @param {string} toolName @returns {boolean} */
export function isPlanCall(toolName) {
  return PLAN_CARD.test(String(toolName ?? ''))
}

/** Commands that need a human even in `critical` mode. */
const DANGEROUS_COMMANDS = [
  // Privilege and account changes.
  /(^|[\s/])(sudo|doas|su|runas)\s/u,
  // Deletion, whatever the flags.
  /(^|[\s/])(rm|rmdir|shred|unlink|srm)\s/u,
  // Whole-disk and partition work.
  /(^|[\s/])(mkfs[.\w]*|fdisk|parted|dd|wipefs|mkswap|losetup)\s/u,
  // Power and init.
  /(^|[\s/])(shutdown|reboot|poweroff|halt|systemctl\s+(poweroff|reboot|halt))\b/u,
  // Killing everything the account can see.
  /(^|[\s/])kill\s+(-9|-KILL|-s\s*9)\s+-1\b/u,
  // Recursive permission changes.
  /(^|[\s/])(chmod|chown|chgrp)\s+(-\w*R|--recursive)/u,
  // History the person did not ask to lose.
  /git\s+push\b[^\n]*(--force\b|--force-with-lease\b|(^|\s)-f(\s|$))/u,
  /git\s+reset\s+--hard\b/u,
  /git\s+clean\s+[^\n]*-[a-z]*[fdx]/u,
  // Publishing to a registry.
  /(npm|pnpm|yarn)\s+publish\b/u,
  // Downloading something and running it.
  /(curl|wget)[^\n|]*\|\s*(sudo\s+)?(ba|z|da|k)?sh\b/u,
  // Writing to the machine's own directories.
  /(^|[\s/])>{1,2}\s*(\/etc|\/usr|\/bin|\/sbin|\/boot|\/dev\/(sd|nvme|hd)|\/var\/(lib|spool))\b/u,
  // The classic fork bomb shape.
  /:\s*\(\s*\)\s*\{/u,
]

/** Paths that belong to the machine rather than the workspace. */
const SYSTEM_PATHS = /^\/(etc|usr|bin|sbin|boot|dev|proc|sys|root|var\/(lib|spool))(\/|$)/u

/** A file whose contents are a credential, not source. */
const SENSITIVE_BASENAME = /^(\.env(\..+)?|id_(rsa|dsa|ecdsa|ed25519)(\.pub)?|known_hosts|authorized_keys|\.npmrc|\.pypirc|\.netrc|\.git-credentials|credentials(\.json)?|secrets?(\.json)?|tokens?(\.json)?|auth\.json|.*\.(pem|key|p12|pfx))$/iu

/** A directory whose contents are credentials. */
const SENSITIVE_SEGMENTS = new Set(['.ssh', '.gnupg', '.aws', '.azure', '.kube', '.docker'])

/** A reference to a credential in a shell command. */
const SENSITIVE_TOKEN = /(^|[\s/'"=:<>,;|&(])(\.env(\.[\w.-]+)?|\.ssh\/[^\s'"|&;]*|id_(rsa|dsa|ecdsa|ed25519)(\.pub)?|\.npmrc|\.pypirc|\.netrc|\.git-credentials|credentials(\.json)?|secrets?(\.json)?|tokens?(\.json)?|.*\.(pem|key|p12|pfx))(?=$|[\s/'"|&;),])/iu

/**
 * Commands that print the environment wholesale.
 *
 * `printenv` dumps whatever it is given, so it always counts. Bare `env` does
 * too — but `env FOO=bar cmd` is how you run a command with one variable set,
 * which is ordinary work and must not be treated as reading the environment.
 */
const ENVIRONMENT_DUMP = /(^|[\s;&|(])(printenv\b|env(?=\s*($|[|>;&)])))/u

/**
 * Reads the command text out of a tool call's arguments.
 *
 * Tools name it differently (`command`, `cmd`, `script`), and a call that
 * carries none is not a shell call at all.
 *
 * @param {object | undefined} input the call's arguments, already parsed
 * @returns {string} the command, or `''`
 */
export function commandOf(input) {
  if (input === null || input === undefined || typeof input !== 'object') return ''
  for (const key of ['command', 'cmd', 'script', 'commandLine']) {
    const value = input[key]
    if (typeof value === 'string') return value
    if (Array.isArray(value) && value.every((part) => typeof part === 'string')) return value.join(' ')
  }
  return ''
}

/**
 * Reads the file path out of a tool call's arguments.
 *
 * @param {object | undefined} input the call's arguments, already parsed
 * @returns {string} the path, or `''`
 */
export function pathOf(input) {
  if (input === null || input === undefined || typeof input !== 'object') return ''
  for (const key of ['path', 'file_path', 'filePath', 'file', 'target', 'destination']) {
    const value = input[key]
    if (typeof value === 'string' && value !== '') return value
  }
  return ''
}

/**
 * Whether a path names a credential file.
 *
 * @param {string} target the path
 * @returns {string} why it is sensitive, or `''`
 */
export function sensitivePath(target) {
  const value = String(target ?? '').replace(/\\/gu, '/').trim()
  if (value === '') return ''
  const segments = value.split('/').filter((segment) => segment !== '' && segment !== '.')
  for (const segment of segments) {
    if (SENSITIVE_SEGMENTS.has(segment)) return `敏感目录 ${segment}`
  }
  const base = segments[segments.length - 1] ?? ''
  if (SENSITIVE_BASENAME.test(base)) return `敏感文件 ${base}`
  return ''
}

/**
 * Whether a shell command references a credential file or dumps the environment.
 *
 * @param {string} command the command text
 * @returns {string} why it is sensitive, or `''`
 */
export function sensitiveCommand(command) {
  const text = String(command ?? '')
  if (text.trim() === '') return ''
  if (ENVIRONMENT_DUMP.test(text)) return '读取环境变量'
  const match = SENSITIVE_TOKEN.exec(text)
  if (match !== null) return `敏感文件 ${match[2]}`
  return ''
}

/**
 * Whether a shell command does something a person should see first.
 *
 * @param {string} command the command text
 * @returns {string} why it is dangerous, or `''` when it is not
 */
export function dangerousCommand(command) {
  const text = String(command ?? '')
  if (text.trim() === '') return ''
  for (const pattern of DANGEROUS_COMMANDS) {
    if (pattern.test(text)) return `匹配危险规则 ${pattern.source}`
  }
  return ''
}

/**
 * Whether a file write leaves the workspace or touches a credential.
 *
 * The path is resolved against `cwd` before it is compared, so `..`, a relative
 * path, or `/workspace/../etc/passwd` cannot be used to escape the check.
 *
 * @param {string} target the path the call names
 * @param {string} cwd the workspace root
 * @returns {string} why it is dangerous, or `''`
 */
export function dangerousPath(target, cwd) {
  const raw = String(target ?? '').trim()
  if (raw === '') return ''
  const base = resolvePath(cwd ?? process.cwd())
  const resolved = resolvePath(base, raw)
  const sensitive = sensitivePath(raw) || sensitivePath(resolved)
  if (sensitive !== '') return sensitive
  if (SYSTEM_PATHS.test(resolved)) return `写入系统路径 ${resolved}`
  const relative = relativePath(base, resolved)
  if (relative === '' || (!relative.startsWith('..') && !isAbsolute(relative))) return ''
  return `写入工作区之外 ${resolved}`
}

/**
 * Whether a session allow-list entry may answer this request without a dialog.
 *
 * A person saying "run this for the session" is a promise about ordinary work,
 * not about a dangerous command, a credential file, or a plan.
 *
 * @param {{verdict: string, severity?: string}} verdict the decision
 * @returns {boolean}
 */
export function allowlistEligible(verdict) {
  return verdict.verdict === 'ask'
    && verdict.severity !== 'danger'
    && verdict.severity !== 'sensitive'
    && verdict.severity !== 'plan'
}

/**
 * Decides whether one request needs a human.
 *
 * @param {{
 *   mode: string,
 *   toolName: string,
 *   input?: object,
 *   cwd?: string,
 * }} request what the harness is asking about
 * @returns {{verdict: 'allow' | 'ask', reason: string, severity: string}} the decision and why
 */
export function decide(request) {
  const mode = APPROVAL_MODES.includes(request.mode) ? request.mode : DEFAULT_APPROVAL_MODE
  const toolName = String(request.toolName ?? '')
  const input = request.input
  const cwd = request.cwd ?? process.cwd()

  // A plan is a decision about what to do next, not a permission: it is asked
  // about in every mode, including `full`.
  if (PLAN_TOOLS.test(toolName)) {
    return { verdict: 'ask', reason: '计划/提问类调用', severity: 'plan' }
  }

  // Credentials are checked **before** the mode, so `full` does not become
  // "print the keys and write them out quietly". Everything else about `full` is
  // unchanged: no dialogs for ordinary work.
  if (SHELL_TOOLS.test(toolName)) {
    const sensitive = sensitiveCommand(commandOf(input))
    if (sensitive !== '') return { verdict: 'ask', reason: sensitive, severity: 'sensitive' }
  } else if (WRITE_TOOLS.test(toolName) || READ_TOOLS.test(toolName)) {
    const sensitive = sensitivePath(pathOf(input))
    if (sensitive !== '') return { verdict: 'ask', reason: sensitive, severity: 'sensitive' }
  }

  if (mode === 'full') return { verdict: 'allow', reason: 'full 模式', severity: 'mode' }

  if (SHELL_TOOLS.test(toolName)) {
    const command = commandOf(input)
    if (command === '') return { verdict: 'ask', reason: '有命令但读不出内容', severity: 'unknown' }
    const dangerous = dangerousCommand(command)
    if (dangerous !== '') return { verdict: 'ask', reason: dangerous, severity: 'danger' }
    if (mode === 'ask') return { verdict: 'ask', reason: 'ask 模式：命令需要确认', severity: 'mode' }
    return { verdict: 'allow', reason: '未命中危险规则', severity: 'command' }
  }

  if (WRITE_TOOLS.test(toolName)) {
    const target = pathOf(input)
    if (target === '') return { verdict: 'ask', reason: '写操作没有可读路径', severity: 'unknown' }
    const dangerous = dangerousPath(target, cwd)
    if (dangerous !== '') return { verdict: 'ask', reason: dangerous, severity: 'danger' }
    if (mode === 'ask') return { verdict: 'ask', reason: 'ask 模式：写入需要确认', severity: 'mode' }
    return { verdict: 'allow', reason: '工作区内的写入', severity: 'write' }
  }

  if (READ_TOOLS.test(toolName)) {
    return { verdict: 'allow', reason: '只读操作', severity: 'read' }
  }

  // Anything else — an unknown tool, a subagent, a web fetch — is asked about.
  return { verdict: 'ask', reason: '未知工具', severity: 'unknown' }
}

/**
 * The allow-list key for "run this for the rest of the session".
 *
 * The full command (not just its first word) and the path a write names: a
 * session grant for `git status` must not silently cover `git push`, and a
 * grant for one file must not cover every write.
 *
 * @param {string} toolName the tool
 * @param {object | undefined} input the call's arguments
 * @returns {string} the key
 */
export function allowKey(toolName, input) {
  const command = commandOf(input).trim()
  if (command !== '') return `${toolName} ${command}`
  const target = pathOf(input).trim()
  if (target !== '') return `${toolName} ${target}`
  return String(toolName ?? '')
}
