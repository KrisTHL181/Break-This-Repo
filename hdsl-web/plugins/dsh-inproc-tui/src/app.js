/**
 * The interactive terminal, running inside the harness process.
 *
 * This is the 0.2.x counterpart of the ACP terminal: the bundle patch mounts this
 * plugin over `dsh-base`, so instead of a protocol it gets the agent registry,
 * the live session log and the streaming assistant feed directly. That is what
 * makes it the fuller interface — token-level text, live reasoning, approval
 * prompts, questions, real compaction — and it is also why it is pinned to the
 * 0.2.x line: those services are internal seams, not a contract.
 *
 * Rendering, key decoding, the composer and the dialogs come from
 * `plugins/shared`, the same toolkit the ACP terminal uses.
 *
 * @module app
 */

import { readFileSync, readdirSync } from 'node:fs'
import { spawn } from 'node:child_process'
import { isAbsolute, relative as relativePath, resolve as resolvePath } from 'node:path'

import { createI18n, detectLanguage, textOf } from '../../shared/i18n.js'
import { createTheme, detectMode } from '../../shared/term/theme.js'
import { Screen } from '../../shared/term/screen.js'
import { createKeyDecoder } from '../../shared/term/keys.js'
import { Composer } from '../../shared/ui/composer.js'
import { applyEditKey } from '../../shared/ui/edit.js'
import { applyCompletion, completionFor, nextCandidate } from '../../shared/ui/complete.js'
import {
  APPROVAL_MODES,
  DEFAULT_APPROVAL_MODE,
  allowKey,
  allowlistEligible,
  decide,
  isPlanCall,
} from '../../shared/approval/policy.js'
import { loadRules, ruleFor, saveRules, withRule, emptyRules } from '../../shared/approval/rules.js'
import { renderFrame } from '../../shared/ui/render.js'
import { ageLabel, shortId } from '../../shared/ui/blocks.js'
import { ChatSession } from './agent.js'
import {
  argsFromProcess,
  isUserQuestionError,
  ownsRequest,
  parseArgs,
  questionInterrupted,
  scopedWaterfall,
  usage,
} from './protocol.js'
import { VERSION } from './version.js'

const TICK_MS = 33
const ESC_DELAY_MS = 25
/** How long `quit()` waits for the agent to detach before ending the process. */
const QUIT_CLOSE_MS = 3_000
/** At most one error notice per window, so a broken frame cannot flood the screen. */
const ERROR_NOTICE_MS = 2_000
/** How much of a resumed session log is put back on screen, newest last. */
const MAX_REPLAY_EVENTS = 400

const EFFORTS = [
  { value: 'off', name: 'off' },
  { value: 'low', name: 'low' },
  { value: 'high', name: 'high' },
  { value: 'max', name: 'max' },
]

/** Directories never worth walking for `@` completion. */
const SKIP_DIRS = new Set(['.git', 'node_modules', 'dist', 'build', '.gradle', '.cache', 'target', 'vendor'])

/**
 * Commands this terminal owns.
 *
 * `/permission`, `/goal` and `/feedback` are the harness's own and are dispatched
 * to `ctx.commands`, so they stay in step with the version the instance runs.
 * `/plan` is local because the terminal has to draw the plan-mode badge and the
 * Shift-Tab shortcut, and `/allow` `/deny` `/rules` are local because they edit
 * a file this terminal owns.
 */
const COMMANDS = [
  { name: 'help', key: 'cmd.help' },
  { name: 'model', key: 'cmd.model' },
  { name: 'effort', key: 'cmd.effort' },
  { name: 'yolo', key: 'cmd.yolo' },
  { name: 'permission', key: 'cmd.permission' },
  { name: 'plan', key: 'cmd.plan' },
  { name: 'resume', key: 'cmd.resume' },
  { name: 'sessions', key: 'cmd.sessions' },
  { name: 'new', key: 'cmd.new' },
  { name: 'compact', key: 'cmd.compact' },
  { name: 'clear', key: 'cmd.clear' },
  { name: 'status', key: 'cmd.status' },
  { name: 'provider', key: 'cmd.provider' },
  { name: 'allow', key: 'cmd.allow' },
  { name: 'deny', key: 'cmd.deny' },
  { name: 'rules', key: 'cmd.rules' },
  { name: 'quit', key: 'cmd.quit' },
]

/** The terminal application. */
export class TerminalApp {
  /**
   * @param {object} ctx the Cordis context of this plugin
   */
  constructor(ctx) {
    this.ctx = ctx
    this.env = process.env
    this.language = detectLanguage(this.env)
    this.i18n = createI18n(this.language)
    this.theme = createTheme({ mode: detectMode(this.env), color: this.env.NO_COLOR === undefined })
    this.screen = new Screen(process.stdout, process.stdin)

    this.cwd = process.cwd()
    this.session = new ChatSession(ctx, this.cwd)
    this.blocks = []
    this.transcriptCache = new WeakMap()
    this.composer = new Composer({ maxLines: 8 })
    this.queue = []
    this.scroll = { offset: 0, follow: true }
    this.overlay = null
    /** The inline `/` menu, or `null`; it is not an overlay and never owns the keyboard. */
    this.palette = null
    this.interactions = []
    /** Live question batches, so `quit()` can reject them as interrupted. */
    this.pendingQuestions = new Set()
    this.activeInteraction = null
    /**
     * How much this session asks before running a tool. The harness has no such
     * switch (its own policy is ask-or-reject), so the decision is this app's —
     * see `plugins/shared/approval/policy.js`.
     */
    this.approvalMode = DEFAULT_APPROVAL_MODE
    /** Exact `(tool + command/path)` keys said "run for this session". */
    this.approvedTools = new Set()
    /** Persistent allow/deny rules, loaded once per terminal. */
    this.rules = loadRules()
    /** Whether Plan mode is on (in-process line; toggled with shift+tab). */
    this.planMode = false
    /** How many requests this session ran without a dialog, for the status row. */
    this.autoApproved = 0
    /** Arguments of tool calls in flight, by callId: the approval request itself has none. */
    this.toolCalls = new Map()
    /** Local `!` shell commands still running; quit must not leave them behind. */
    this.shellProcesses = new Set()
    /** The rejection reason being typed, or `null` when that line is closed. */
    this.reasonDraft = null
    /** The Tab completion being cycled, or `null`. */
    this.completion = null
    /** Model routes/ids seen by the last model picker, for `/model` completion. */
    this.modelOptions = []
    /** Sessions seen by the last resume picker, for `/resume` completion. */
    this.sessionCache = []
    this.focus = 'composer'
    this.expandTools = false
    this.status = 'connecting'
    this.activity = { tool: null, startedAt: null }
    this.usage = { used: 0, size: 0 }
    this.model = { current: null, currentLabel: null, options: [] }
    this.effort = { current: null, options: [] }
    this.lastTurnSeconds = null
    this.turnEndedAt = undefined
    this.turnStartedAt = null
    this.gitBranch = detectGitBranch(this.cwd)
    this.clock = 0
    this.dirty = true
    this.timer = null
    this.escTimer = null
    this.quitArmedAt = null
    this.stopping = false
    /** True before the alternate screen is taken: `--help`, or no TTY. */
    this.headless = false
    /** When the last error notice was pushed (rate limit for `reportError`). */
    this.lastErrorAt = 0
    this.transcriptTotal = 0
    this.disposers = []
    this.streaming = { text: null, reasoning: null }
    this.deliveredStreams = 0
    this.exitCode = 0
    this.exitPromise = new Promise((resolve) => {
      this.resolveExit = resolve
    })
    this.codec = createKeyDecoder((event) => this.onKeyEvent(event))
  }

  /** The live session id: the status row and the banner both show it. */
  get sessionId() {
    return this.session?.sessionId ?? null
  }

  /** What the renderer reads for the approval badge. */
  get approval() {
    return { mode: this.approvalMode, autoApproved: this.autoApproved }
  }

  // ── lifecycle ───────────────────────────────────────────────────────────

  /** Boots the terminal and the agent. */
  async start() {
    const args = this.readArgs()
    if (args.help) {
      // Nothing has taken the terminal, so this path ends the app here: a
      // profile that only printed its help must not keep the process alive.
      this.headless = true
      process.stdout.write(usage())
      await this.quit(0)
      return this
    }
    if (process.stdin.isTTY !== true || process.stdout.isTTY !== true) {
      // Writing alt-screen and raw mode into a pipe corrupts whatever reads it;
      // the ACP terminal refuses the same way.
      this.headless = true
      process.stdout.write('dsh-inproc-tui: this interface needs a terminal; run it in a PTY.\n')
      await this.quit(1)
      return this
    }
    this.screen.enter()
    this.screen.resized = () => {
      this.dirty = true
      this.render()
    }
    this.installInput()
    this.installSignalHandlers()
    this.pushBanner()
    this.render()
    this.timer = setInterval(() => this.tick(), TICK_MS)

    this.wireHarnessEvents()
    try {
      const { resumed } = await this.session.open({ resume: args.resume })
      if (this.session.resumeFailure !== undefined) {
        this.pushNotice(`${this.failureText(this.session.resumeFailure)} — starting a new session`, 'error')
      }
      this.syncSelection()
      // The banner was painted before the session existed; it now has an id.
      this.refreshBanner()
      // A launch that was asked to resume has to show what it resumed: the log is
      // the only record of those turns, and a banner alone reads as an empty
      // conversation. `/resume` does the same; this is the way in from the panel.
      if (resumed) this.replayHistory(this.session.history())
      this.pushNotice(resumed
        ? this.i18n.t('banner.resume', { id: shortId(this.session.sessionId) })
        : this.i18n.t('notice.ready', { version: process.env.HDSL_INSTANCE_VERSION ?? '0.2.x' }), 'info')
      this.status = 'ready'
      this.pumpQueue()
    } catch (error) {
      this.status = 'disconnected'
      this.pushNotice(this.failureText(error), 'error')
    }
    this.dirty = true
    this.render()
    return this
  }

  /** @private */
  readArgs() {
    let list = []
    try {
      list = this.ctx.get('cmdlineArgs')?.get() ?? []
    } catch {
      list = []
    }
    if (Array.isArray(list) && list.length > 0) return parseArgs(list)
    // Fall back to the process arguments after the profile name; a launcher
    // that did not provide cmdlineArgs still gets --resume working.
    return argsFromProcess(process.argv)
  }

  /** @private */
  installInput() {
    if (typeof process.stdin.setRawMode === 'function') process.stdin.setRawMode(true)
    process.stdin.resume()
    process.stdin.setEncoding('utf8')
    process.stdin.on('data', (chunk) => {
      try {
        this.codec.feed(chunk)
        if (this.codec.pending) {
          if (this.escTimer !== null) clearTimeout(this.escTimer)
          this.escTimer = setTimeout(() => {
            this.escTimer = null
            try {
              this.codec.flush()
            } catch (error) {
              this.reportError(error, 'keys')
            }
          }, ESC_DELAY_MS)
        }
      } catch (error) {
        this.reportError(error, 'keys')
      }
    })
    process.stdin.on('end', () => this.quit())
  }

  /** @private */
  installSignalHandlers() {
    this.signalHandlers = ['SIGTERM', 'SIGHUP'].map((signal) => {
      const handler = () => {
        void this.quit()
      }
      process.on(signal, handler)
      return [signal, handler]
    })
  }

  /** @private */
  wireHarnessEvents() {
    const ctx = this.ctx
    const mine = (agent) => agent === this.session.live
    // A malformed event or a render bug must become a notice, not an uncaught
    // exception in the host process. Waterfall listeners are wrapped too: the
    // answer is their return value, so it is passed through untouched.
    const guard = (label, fn) => (...args) => {
      try {
        const result = fn(...args)
        if (result !== null && typeof result?.catch === 'function') {
          result.catch((error) => this.reportError(error, label))
        }
        return result
      } catch (error) {
        this.reportError(error, label)
        return undefined
      }
    }
    const isMine = (request) => ownsRequest(request, this.session.live)
    this.disposers.push(
      ctx.on('agent/assistant-stream', guard('stream', (payload) => {
        if (mine(payload.agent)) this.onStreamFrame(payload.frame)
      })),
      ctx.on('session/event', guard('session', (session, event) => {
        if (session === this.session.live?.session) this.onSessionEvent(event)
      })),
      ctx.on('agent/status', guard('status', (payload) => {
        if (!mine(payload.agent)) return
        this.status = payload.status === 'running' ? 'working' : 'ready'
        if (payload.status === 'running' && this.activity.startedAt === null) {
          this.activity.startedAt = this.clock
        }
        this.dirty = true
      })),
      ctx.on('agent/error', guard('agent', (payload) => {
        if (!mine(payload.agent)) return
        this.debug(payload.error)
        this.pushNotice(this.failureText(payload.error), 'error')
      })),
      // 0.2.x approval and question requests are waterfalls: answer only the
      // agent this terminal drives and delegate everyone else's down the chain
      // (a sub-agent's approval, the host's /auth wizard). An agentless request
      // is not ours either — dsh-tui's own /auth questions belong to whoever
      // dispatched them.
      ctx.on('approval/request', guard('approval', scopedWaterfall({
        isMine,
        handle: (request) => this.requestApproval(request),
        onError: (error) => this.reportError(error, 'approval'),
      }))),
      ctx.on('user-questions/request', guard('questions', scopedWaterfall({
        isMine,
        handle: (request) => this.requestAnswers(request),
        onError: (error) => this.reportError(error, 'questions'),
      }))),
    )
  }

  // ── harness events ──────────────────────────────────────────────────────

  /** @private */
  onStreamFrame(frame) {
    if (frame === null || frame === undefined) return
    if (frame.type === 'end') {
      this.finishStreaming()
      return
    }
    if (frame.type !== 'chunk') return
    const chunk = frame.chunk
    if (chunk === undefined) return
    switch (chunk.type) {
      case 'block-start':
        if (chunk.blockType === 'reasoning') this.startBlock('reasoning')
        else if (chunk.blockType === 'text') this.startBlock('text')
        return
      case 'text-delta': {
        if (chunk.text === '') return
        const block = this.streamBlock('text')
        block.full = `${block.full ?? ''}${chunk.text}`
        block.text = block.full
        this.bump(block)
        return
      }
      case 'reasoning-delta': {
        if (chunk.text === '') return
        const block = this.streamBlock('reasoning')
        block.full = `${block.full ?? ''}${chunk.text}`
        block.text = block.full
        this.bump(block)
        return
      }
      case 'block-end':
        if (chunk.block?.type === 'reasoning') this.finishOne('reasoning')
        else if (chunk.block?.type === 'text') {
          this.finishOne('text')
          this.deliveredStreams += 1
        }
        return
      case 'usage':
        return
      case 'finish':
        this.finishStreaming()
        return
      default:
        // `tool-call-delta` is presentation the durable tool events already give.
        return
    }
  }

  /** @private */
  startBlock(kind) {
    this.streamBlock(kind)
  }

  /** @private */
  streamBlock(kind) {
    const existing = this.streaming[kind]
    if (existing !== null) return existing
    const block = this.makeBlock({
      type: kind === 'reasoning' ? 'thought' : 'assistant',
      full: '',
      text: '',
      streaming: true,
      expanded: false,
    })
    this.streaming[kind] = block
    return block
  }

  /** @private */
  finishOne(kind) {
    const block = this.streaming[kind]
    if (block === null || block === undefined) return
    block.streaming = false
    block.text = block.full ?? block.text
    this.streaming[kind] = null
    this.bump(block)
  }

  /** @private */
  finishStreaming() {
    this.finishOne('text')
    this.finishOne('reasoning')
  }

  /** @private */
  onSessionEvent(event) {
    switch (event.type) {
      case 'turn/start':
        this.status = 'working'
        this.activity = { tool: null, startedAt: this.clock }
        this.turnStartedAt = this.clock
        this.lastTurnSeconds = null
        this.turnEndedAt = undefined
        this.deliveredStreams = 0
        this.dirty = true
        return
      case 'turn/end': {
        this.finishStreaming()
        this.deliveredStreams = 0
        this.status = this.session.busy ? 'working' : 'ready'
        if (this.turnStartedAt !== null) {
          this.lastTurnSeconds = (this.clock - this.turnStartedAt) / 1000
          this.turnEndedAt = this.clock
          this.turnStartedAt = null
        }
        this.activity = { tool: null, startedAt: null }
        this.usage = this.session.usage()
        void this.session.flush()
        this.dirty = true
        this.pumpQueue()
        return
      }
      case 'tool/call': {
        const input = parseArguments(event.data.arguments)
        // Kept for the approval that follows: the request names the tool and the
        // call, never the arguments the decision is about.
        if (event.data.callId !== undefined) {
          this.toolCalls.set(event.data.callId, { name: event.data.name ?? 'tool', input })
        }
        // No `expanded` field: the global toggle (ctrl+o / shift+tab) decides,
        // and a card that pinned itself closed would ignore it.
        const block = this.makeBlock({
          type: 'tool',
          callId: event.data.callId,
          name: event.data.name ?? 'tool',
          input,
          status: 'in_progress',
          startedAt: this.clock,
          output: null,
          plan: isPlanCall(event.data.name),
        })
        this.activity.tool = block.name
        this.dirty = true
        return
      }
      case 'tool/result': {
        const block = this.findTool(event.data.message?.toolCallId)
        if (event.data.message?.toolCallId !== undefined) {
          this.toolCalls.delete(event.data.message.toolCallId)
        }
        if (block === null) return
        block.output = textOfMessage(event.data.message)
        block.status = event.data.message?.isError === true ? 'failed' : 'completed'
        block.endedAt = this.clock
        this.activity.tool = null
        this.bump(block)
        return
      }
      case 'assistant/message': {
        // A provider that emits no deltas still has to show its answer: one
        // committed message per streamed block means "already on screen".
        if (this.deliveredStreams > 0) {
          this.deliveredStreams -= 1
          return
        }
        const text = textOfMessage(event.data.message)
        if (text.trim() === '') return
        const block = this.makeBlock({ type: 'assistant', text, streaming: false, expanded: false })
        this.bump(block)
        return
      }
      default:
        return
    }
  }

  // ── interactions ────────────────────────────────────────────────────────

  /** @private */
  requestApproval(request) {
    return new Promise((resolve) => {
      if (request.signal?.aborted === true) {
        resolve('cancelled')
        return
      }
      // The request names the tool and (when the asker had one) the call, not its
      // arguments — those arrived on `tool/call`, which this app already stored.
      const call = request.callId === undefined ? undefined : this.toolCalls.get(request.callId)
      const toolName = request.toolName ?? call?.name ?? 'tool'
      const key = allowKey(toolName, call?.input)
      const verdict = decide({ mode: this.approvalMode, toolName, input: call?.input, cwd: this.cwd })

      // A permanent rule is explicit configuration and wins over the heuristic.
      // A deny is final; an allow still records the harness audit because the
      // answer is `allowed-once`, not a policy change in the harness.
      const rule = ruleFor(this.rules, key)
      if (rule === 'deny') {
        this.pushNotice(this.i18n.t('permission.rule.denied', { key }), 'warning')
        resolve('rejected')
        this.dirty = true
        return
      }
      if (rule === 'allow') {
        this.autoApproved += 1
        resolve('allowed-once')
        this.dirty = true
        return
      }
      // "Run for this session" is a promise about ordinary work. It must not
      // swallow a dangerous command, a credential file, or a plan: those still
      // draw the dialog, so the person sees what changed.
      if (this.approvedTools.has(key) && allowlistEligible(verdict)) {
        this.autoApproved += 1
        resolve('allowed-once')
        this.dirty = true
        return
      }
      if (verdict.verdict === 'allow') {
        this.autoApproved += 1
        resolve('allowed-once')
        this.dirty = true
        return
      }
      const item = {
        kind: 'approval',
        request,
        key,
        toolName,
        // `displayReason` is a localized object on 0.2.x (`{ en, zh, … }`), not a string:
        // rendering it raw is where `[object Object]` came from.
        detail: textOf(request.displayReason ?? request.reason, this.language),
        /** Why the dialog is up, so the footer can say "critical 模式下需要确认". */
        why: verdict.reason,
        plan: verdict.severity === 'plan',
        settle: (outcome) => {
          request.signal?.removeEventListener('abort', onAbort)
          retire(this, item)
          resolve(outcome)
        },
        render: () => this.approvalOverlay(item),
      }
      const onAbort = () => item.settle('cancelled')
      request.signal?.addEventListener('abort', onAbort, { once: true })
      enqueue(this, item)
    })
  }

  /**
   * The approval dialog: four numbered choices.
   *
   * The numbers are the shortcuts (1-9 already selected rows); printing them is
   * what makes that discoverable. The fourth choice opens a reason line inside
   * the dialog — the composer behind it is not visible, so typing there would
   * edit unseen text.
   *
   * @private
   * @param {object} item the interaction being rendered
   * @returns {object} the overlay
   */
  approvalOverlay(item) {
    const overlay = {
      kind: 'permission',
      title: item.plan ? this.i18n.t('permission.plan.title') : this.i18n.t('permission.title'),
      toolName: item.toolName,
      detail: item.detail,
      plan: item.plan === true,
      options: [
        { label: this.i18n.t('permission.allow-once'), value: 'allowed-once' },
        { label: this.i18n.t('permission.allow-always'), value: 'session-allow' },
        { label: this.i18n.t('permission.reject'), value: 'rejected' },
        { label: this.i18n.t('permission.reject-reason'), value: 'reject-reason' },
      ],
      index: 0,
      detailOffset: 0,
      hint: this.i18n.t('permission.hint'),
    }
    // A reason line that was already open survives a re-render (it redraws on
    // every keystroke and on resize).
    if (this.reasonDraft !== null) overlay.reason = { text: this.reasonDraft }
    return overlay
  }

  /** @private */
  requestAnswers(request) {
    return new Promise((resolve, reject) => {
      if (request.signal?.aborted === true) {
        reject(questionInterrupted())
        return
      }
      const answers = []
      let index = 0
      let active = null
      let settled = false
      // Registered so `quit()` can reject this ask instead of waiting for a
      // dialogue nobody is left to answer.
      const batch = {
        cancel: () => fail(),
      }
      const remove = () => {
        this.pendingQuestions.delete(batch)
        request.signal?.removeEventListener('abort', fail)
      }
      const fail = () => {
        if (settled) return
        settled = true
        remove()
        const current = active
        active = null
        if (current !== null) retire(this, current)
        reject(questionInterrupted())
      }
      this.pendingQuestions.add(batch)
      request.signal?.addEventListener('abort', fail, { once: true })
      const askNext = () => {
        const question = request.questions[index]
        if (question === undefined) {
          if (settled) return
          settled = true
          remove()
          resolve({ answers })
          return
        }
        const options = (question.options ?? []).map((option) => ({
          label: option.label,
          value: option.label,
          description: option.description,
        }))
        const item = {
          kind: 'question',
          settle: (selected) => {
            active = null
            retire(this, item)
            answers.push({ id: question.id, selected: selected === null ? [] : [selected] })
            index += 1
            askNext()
          },
          render: () => ({
            kind: 'permission',
            title: question.header ?? this.i18n.t('question.title'),
            detail: [textOf(question.question, this.language),
              textOf(question.detail, this.language)].filter(Boolean).join('\n'),
            options: options.length > 0
              ? options
              : [{ label: this.i18n.t('question.no-options'), value: null }],
            index: 0,
          }),
        }
        active = item
        enqueue(this, item)
      }
      askNext()
    })
  }

  /**
   * Rejects every waiting question (as interrupted) and answers every waiting
   * approval as cancelled. Called from `quit()` before the agent is detached,
   * so a tool blocked on a dialogue can unwind instead of holding `whenIdle()`.
   *
   * @private
   */
  dismissInteractions() {
    this.queue = []
    for (const batch of [...this.pendingQuestions]) {
      try {
        batch.cancel()
      } catch {
        // A batch that already settled has nothing left to reject.
      }
    }
    this.pendingQuestions.clear()
    const pending = [this.activeInteraction, ...this.interactions].filter((item) => item !== null)
    this.interactions = []
    this.activeInteraction = null
    this.overlay = null
    for (const item of pending) {
      if (item.kind !== 'approval') continue
      try {
        item.settle('cancelled')
      } catch {
        // The dialogue may already have been retired by its abort listener.
      }
    }
    this.dirty = true
  }

  /** @private */
  showNextInteraction() {
    // Quitting retires interactions on purpose: do not pop the next dialogue
    // while the terminal is being restored.
    if (this.stopping) {
      this.activeInteraction = null
      this.overlay = null
      return
    }
    if (this.activeInteraction !== null) return
    const next = this.interactions.shift()
    this.activeInteraction = next ?? null
    if (next === undefined) {
      if (this.overlay?.kind === 'permission') this.overlay = null
      this.dirty = true
      return
    }
    this.overlay = next.render()
    this.dirty = true
    this.render()
  }

  /** @private */
  resolveInteraction(value) {
    // Two of the four approval choices are the app's own, not outcomes the
    // harness knows: "run for this session" records the key and runs this one,
    // and "reject with a reason" opens the reason line without settling yet.
    if (value === 'session-allow') {
      const key = this.activeInteraction?.key
      if (key !== undefined) this.approvedTools.add(key)
      value = 'allowed-once'
    }
    if (value === 'reject-reason') {
      if (this.overlay?.kind === 'permission' && this.overlay.reason === undefined) {
        this.reasonDraft = ''
        this.overlay = { ...this.overlay, reason: { text: '' } }
        this.dirty = true
        this.render()
      }
      return
    }
    this.reasonDraft = null
    const active = this.activeInteraction
    this.activeInteraction = null
    this.overlay = null
    active?.settle(value)
    this.dirty = true
    this.showNextInteraction()
    this.render()
  }

  /**
   * Settles the open approval as rejected and sends the reason to the model.
   *
   * The harness's rejection carries no text, so the reason only reaches the
   * model as a message of its own. `followup` is queued for the next step, which
   * is exactly where the model learns why and can change course — and it does
   * not interrupt the turn that just lost its tool.
   *
   * @private
   */
  submitReason() {
    const reason = String(this.reasonDraft ?? '').trim()
    const toolName = this.activeInteraction?.toolName ?? 'tool'
    this.resolveInteraction('rejected')
    if (reason === '') return
    const text = this.i18n.t('permission.reason.message', { tool: toolName, reason })
    this.makeBlock({ type: 'user', text })
    try {
      this.session.send(text)
    } catch (error) {
      this.pushNotice(this.failureText(error), 'error')
    }
    this.dirty = true
  }

  // ── blocks ──────────────────────────────────────────────────────────────

  /** @private */
  makeBlock(block) {
    const created = { ...block, rev: 0 }
    this.blocks.push(created)
    this.scroll.follow = true
    this.scroll.offset = 0
    this.dirty = true
    return created
  }

  /** @private */
  bump(block) {
    block.rev += 1
    this.dirty = true
  }

  /** @private */
  /**
   * Points the banner at the live session.
   *
   * The banner is painted before the session exists (the screen needs something
   * on it while `open()` runs), so its id has to be filled in afterwards — and
   * again after a resume, where the banner is the one thing kept.
   *
   * @private
   */
  refreshBanner() {
    const banner = this.blocks.find((block) => block.type === 'banner')
    if (banner === undefined) return
    banner.sessionId = this.sessionId
    banner.model = this.model.currentLabel ?? this.model.current ?? null
    banner.mode = this.approvalMode
    banner.planMode = this.planMode
    banner.rev += 1
    this.dirty = true
  }

  /** @private */
  pushBanner() {
    this.makeBlock({
      type: 'banner',
      tag: 'HDSL-web',
      version: `v${VERSION} · dsh-inproc-tui`,
      subtitle: this.i18n.t('banner.subtitle'),
      hint: this.i18n.t('banner.hint'),
      sessionId: this.sessionId,
      cwd: this.cwd,
      model: this.model.currentLabel ?? this.model.current ?? null,
      mode: this.approvalMode,
      planMode: this.planMode,
    })
  }

  /** @private */
  pushNotice(text, level = 'info') {
    this.makeBlock({ type: 'notice', text, level })
  }

  /** @private */
  findTool(callId) {
    if (callId === undefined) return null
    for (let index = this.blocks.length - 1; index >= 0; index -= 1) {
      const block = this.blocks[index]
      if (block.type === 'tool' && block.callId === callId) return block
    }
    return null
  }

  // ── keys ────────────────────────────────────────────────────────────────

  /** @private */
  onKeyEvent(event) {
    // Any key but Tab ends the cycle: the next Tab starts from the fresh token.
    if (event.name !== 'tab') this.completion = null
    try {
      this.dispatchKey(event)
    } catch (error) {
      this.debug(error)
      this.pushNotice(this.failureText(error), 'error')
      this.render()
    }
  }

  /** @private */
  dispatchKey(event) {
    if (this.overlay !== null) {
      this.handleOverlayKey(event)
      return
    }
    // The `/` menu is part of the composer, not a dialog: it gets first refusal
    // on the keys it owns, then lets everything else edit the line underneath.
    if (this.palette !== null && this.handlePaletteKey(event)) return
    const { name } = event
    if (name === 'paste') {
      // The terminal sends the whole clipboard as one event; it is the user's
      // text, so it goes in at the caret with CRLF normalised by the composer.
      this.composer.paste(event.text ?? '')
      this.syncCommandPalette()
      this.scrollToBottom()
      return
    }
    if (name === 'char') {
      this.composer.insert(event.text ?? '')
      this.syncCommandPalette()
      this.scrollToBottom()
      return
    }
    if (name === 'enter') {
      // `ctrl+j` arrives as enter+ctrl from the decoder (a raw newline is a
      // control byte, not a key name every terminal agrees on), and it means
      // the same thing shift+enter and alt+enter do: a line break, not a send.
      if (event.shift || event.alt || event.ctrl) {
        this.composer.newline()
        this.dirty = true
        return
      }
      this.submitComposer()
      return
    }
    if (name === 'backspace') {
      this.composer.backspace()
      this.dirty = true
      return
    }
    if (name === 'delete') {
      this.composer.deleteForward()
      this.dirty = true
      return
    }
    if (name === 'left' || name === 'right' || name === 'up' || name === 'down') {
      const width = Math.max(1, this.screen.columns - 4)
      if ((name === 'up' || name === 'down') && !this.composer.text.includes('\n')
        && (name === 'up' || this.composer.historyIndex !== -1)) {
        this.composer.recall(name)
        this.dirty = true
        return
      }
      this.composer.move(name, width)
      this.dirty = true
      return
    }
    if (name === 'home' || name === 'end') {
      this.composer.moveLineEdge(name, Math.max(1, this.screen.columns - 4))
      this.dirty = true
      return
    }
    if (name === 'page-up' || name === 'page-down') {
      this.scrollBy(name === 'page-up' ? this.screen.rows - 4 : -(this.screen.rows - 4))
      return
    }
    if (name === 'escape') {
      if (this.status === 'working') this.cancelTurn()
      else this.scrollToBottom()
      return
    }
    if (name === 'ctrl+c') {
      this.handleInterrupt()
      return
    }
    if (name === 'ctrl+d') {
      if (this.composer.empty) void this.quit()
      else this.composer.deleteForward()
      this.dirty = true
      return
    }
    if (name === 'ctrl+j') {
      this.composer.newline()
      this.dirty = true
      return
    }
    if (name === 'ctrl+s') {
      this.steerComposer()
      return
    }
    if (name === 'ctrl+a') {
      this.composer.moveLineEdge('home', Math.max(1, this.screen.columns - 4))
      this.dirty = true
      return
    }
    if (name === 'ctrl+e') {
      this.composer.moveLineEdge('end', Math.max(1, this.screen.columns - 4))
      this.dirty = true
      return
    }
    if (name === 'ctrl+u') {
      this.composer.deleteToLineStart(Math.max(1, this.screen.columns - 4))
      this.dirty = true
      return
    }
    if (name === 'ctrl+k') {
      this.composer.deleteToEnd()
      this.dirty = true
      return
    }
    if (name === 'ctrl+w') {
      this.composer.deleteWord()
      this.dirty = true
      return
    }
    if (name === 'ctrl+o') {
      this.expandTools = !this.expandTools
      this.transcriptCache = new WeakMap()
      this.dirty = true
      return
    }
    if (name === 'ctrl+r') {
      void this.openResumePicker()
      return
    }
    if (name === 'ctrl+l') {
      this.blocks = []
      this.transcriptCache = new WeakMap()
      this.screen.clear()
      this.pushBanner()
      this.dirty = true
      return
    }
    if (name === 'tab') {
      this.complete()
      return
    }
    if (name === 'shift+tab') {
      // Kimi Code's muscle memory: Shift-Tab is Plan mode, not the tool
      // expansion toggle (that one stays on ctrl+o).
      void this.togglePlan('')
    }
  }

  /** @private */
  handleInterrupt() {
    if (this.status === 'working') {
      this.cancelTurn()
      return
    }
    if (!this.composer.empty) {
      this.composer.setText('')
      this.palette = null
      this.dirty = true
      return
    }
    const now = Date.now()
    if (this.quitArmedAt !== null && now - this.quitArmedAt < 2000) {
      void this.quit()
      return
    }
    this.quitArmedAt = now
    this.pushNotice(this.i18n.t('quit.confirm'), 'info')
    this.dirty = true
  }

  /**
   * Keys while the inline `/` menu is open.
   *
   * The menu is not a dialog: typing keeps editing the command and the menu
   * filters itself. Arrows move the selection, enter runs it, esc closes it
   * without clearing the line. `false` means the app still owns the key, so
   * ctrl+c, ctrl+o and the like keep their normal meaning.
   *
   * @private
   * @param {{name: string, text?: string}} event the decoded key
   * @returns {boolean} whether the menu consumed the key
   */
  handlePaletteKey(event) {
    const width = Math.max(1, this.screen.columns - 4)
    const { name } = event
    const items = this.palette.items ?? []
    if (name === 'up' || name === 'down') {
      if (items.length === 0) return true
      const step = name === 'up' ? -1 : 1
      this.palette.index = (this.palette.index + step + items.length) % items.length
      this.dirty = true
      return true
    }
    if (name === 'page-up' || name === 'page-down') {
      if (items.length === 0) return true
      const step = name === 'page-up' ? -5 : 5
      this.palette.index = Math.max(0, Math.min(items.length - 1, this.palette.index + step))
      this.dirty = true
      return true
    }
    if (name === 'enter') {
      if (event.shift || event.alt || event.ctrl) {
        this.composer.newline()
        this.syncCommandPalette()
        this.dirty = true
        return true
      }
      this.choosePaletteItem(items[this.palette.index] ?? items[0])
      return true
    }
    if (name === 'ctrl+j') {
      this.composer.newline()
      this.syncCommandPalette()
      this.dirty = true
      return true
    }
    if (name === 'escape') {
      this.palette = null
      this.dirty = true
      return true
    }
    if (name === 'tab') {
      this.complete()
      return true
    }
    if (name === 'char' && /^[1-9]$/u.test(event.text ?? '')) {
      const item = items[Number(event.text) - 1]
      if (item !== undefined) this.choosePaletteItem(item)
      return true
    }
    if (name === 'left' || name === 'right') {
      this.composer.move(name, width)
      this.syncCommandPalette()
      this.dirty = true
      return true
    }
    if (!applyEditKey(this.composer, event, width)) return false
    this.syncCommandPalette()
    this.scrollToBottom()
    this.dirty = true
    return true
  }

  /** @private */
  choosePaletteItem(item) {
    if (item === undefined) return
    this.palette = null
    this.composer.setText('')
    this.dirty = true
    this.runCommand(item.name)
  }

  /** @private */
  handleOverlayKey(event) {
    const overlay = this.overlay
    if (overlay === null) return
    const { name } = event
    if (overlay.reason !== undefined) {
      this.editReason(event)
      return
    }
    if (name === 'paste') {
      // A dialog owns the keyboard while it is up; the composer behind it is not
      // visible, so inserting there would edit text the person cannot see.
      return
    }
    if (name === 'escape') {
      if (overlay.kind === 'permission') this.resolveInteraction(null)
      else this.overlay = null
      this.dirty = true
      return
    }
    if (name === 'ctrl+c') {
      if (overlay.kind === 'permission') this.resolveInteraction(null)
      else this.overlay = null
      this.dirty = true
      return
    }
    if (name === 'up' || name === 'down') {
      const length = overlay.items?.length ?? overlay.options?.length ?? 0
      if (length === 0) return
      overlay.index = (overlay.index ?? 0) + (name === 'up' ? -1 : 1)
      if (overlay.index < 0) overlay.index = length - 1
      if (overlay.index >= length) overlay.index = 0
      this.dirty = true
      return
    }
    if (name === 'page-up' || name === 'page-down') {
      if (overlay.kind === 'permission' && overlay.detail) {
        // Page keys scroll the detail: a long command or patch must be readable
        // without pushing the choices off the dialog.
        const step = name === 'page-up' ? -6 : 6
        overlay.detailOffset = Math.max(0, (overlay.detailOffset ?? 0) + step)
        this.dirty = true
        return
      }
      const length = overlay.items?.length ?? overlay.options?.length ?? 0
      if (length === 0) return
      overlay.index = Math.max(0, Math.min(length - 1, (overlay.index ?? 0) + (name === 'page-up' ? -6 : 6)))
      this.dirty = true
      return
    }
    if (name === 'char' && /^[1-9]$/u.test(event.text ?? '')) {
      const index = Number(event.text) - 1
      if (overlay.kind === 'permission') {
        const option = overlay.options?.[index]
        if (option !== undefined) this.resolveInteraction(option.value)
      } else if (overlay.kind === 'picker') {
        const item = overlay.items?.[index]
        if (item !== undefined) this.chooseOverlayItem(item)
      }
      return
    }
    if (name === 'enter') {
      if (overlay.kind === 'permission') {
        const option = overlay.options?.[overlay.index ?? 0]
        this.resolveInteraction(option?.value ?? null)
        return
      }
      if (overlay.kind === 'picker') {
        const item = overlay.items?.[overlay.index ?? 0]
        if (item !== undefined) this.chooseOverlayItem(item)
        return
      }
      this.overlay = null
      this.dirty = true
    }
  }

  /**
   * Edits the rejection reason while its line is open.
   *
   * Deliberately small: one line, append and backspace, esc to go back to the
   * choices. It is a note to the model, not a prompt editor — the composer is
   * still there for a real message once the dialog is gone.
   *
   * @private
   * @param {{name: string, text?: string}} event the decoded key
   */
  editReason(event) {
    const draft = this.reasonDraft ?? ''
    const { name } = event
    if (name === 'char') {
      this.reasonDraft = draft + (event.text ?? '')
    } else if (name === 'paste') {
      this.reasonDraft = draft + String(event.text ?? '').replace(/\r\n?/gu, '\n')
    } else if (name === 'backspace') {
      this.reasonDraft = Array.from(draft).slice(0, -1).join('')
    } else if (name === 'enter') {
      this.submitReason()
      return
    } else if (name === 'escape' || name === 'ctrl+c') {
      // Back to the four choices, nothing settled.
      this.reasonDraft = null
      if (this.overlay !== null) {
        const { reason: _dropped, ...rest } = this.overlay
        this.overlay = rest
      }
      this.dirty = true
      this.render()
      return
    } else {
      return
    }
    if (this.overlay !== null) this.overlay.reason = { text: this.reasonDraft }
    this.dirty = true
    this.render()
  }

  /** @private */
  chooseOverlayItem(item) {
    this.overlay = null
    this.dirty = true
    if (typeof item.onSelect === 'function') item.onSelect()
  }

  // ── commands and turns ──────────────────────────────────────────────────

  /** @private */
  syncCommandPalette() {
    const text = this.composer.text
    if (!text.startsWith('/') || /[\s\n]/u.test(text)) {
      // The text stopped being a command prefix (`/help args`, a newline, a
      // paste that is not a command): the menu goes away instead of staying up
      // over a line it no longer describes.
      if (this.palette !== null) {
        this.palette = null
        this.dirty = true
      }
      return
    }
    const prefix = text.slice(1).toLowerCase()
    const items = this.commandList()
      .filter((command) => command.name.startsWith(prefix))
      .map((command) => ({
        name: command.name,
        label: `/${command.name}`,
        description: command.description,
      }))
    if (items.length === 0) {
      if (this.palette !== null) {
        this.palette = null
        this.dirty = true
      }
      return
    }
    const previous = this.palette
    const same = previous !== null
      && previous.items.length === items.length
      && previous.items.every((item, index) => item.name === items[index].name)
    if (same) {
      // Moving the caret does not change the list; keep the highlighted row.
      previous.index = Math.min(previous.index, items.length - 1)
      return
    }
    this.palette = { items, index: 0 }
    this.dirty = true
  }

  /**
   * Completes the token under the caret, cycling on repeated Tab.
   *
   * `completionFor` decides what the token even is (command, argument, path);
   * this method only applies the result, so the rules stay testable without a
   * terminal.
   *
   * @private
   */
  complete() {
    const caret = this.composer.caret
    // Still cycling the same candidate: the text is exactly the option applied
    // last time and the caret sits right after it, so the next Tab moves on
    // instead of re-filtering from what was just filled in.
    const current = this.completion
    if (current !== null && current !== undefined
      && caret === current.start + current.option.length
      && this.composer.text.slice(current.start, caret) === current.option) {
      const index = (current.index + 1) % current.options.length
      const option = current.options[index]
      const next = applyCompletion(
        { text: this.composer.text, caret },
        { start: current.start, end: caret },
        option,
      )
      this.composer.setText(next.text)
      this.composer.caret = next.caret
      this.completion = { ...current, index, option, end: next.caret }
      this.syncCommandPalette()
      this.dirty = true
      return
    }
    const fresh = completionFor({
      text: this.composer.text,
      caret: this.composer.caret,
      resolve: (request) => this.completionsFor(request),
    })
    if (fresh === null) {
      this.completion = null
      this.dirty = true
      return
    }
    const { option, index } = nextCandidate(this.completion, fresh)
    const applied = applyCompletion(
      { text: this.composer.text, caret: this.composer.caret },
      fresh,
      option,
    )
    this.composer.setText(applied.text)
    this.composer.caret = applied.caret
    this.completion = { ...fresh, index, option }
    this.syncCommandPalette()
    this.dirty = true
  }

  /**
   * The candidate source: commands, their arguments, and `@` paths.
   *
   * @private
   * @param {{token: string, index: number, command: string}} request what is being completed
   * @returns {string[]} candidates
   */
  completionsFor(request) {
    const { token, index, command } = request
    if (index === 0) return this.commandList().map((entry) => `/${entry.name}`)
    if (token.startsWith('@')) return this.completePaths(token.slice(1)).map((path) => `@${path}`)
    switch (command) {
      case 'yolo':
        return ['ask', 'critical', 'full', 'status', 'forget']
      case 'plan':
        return ['on', 'off']
      case 'effort':
        return EFFORTS.map((effort) => effort.value)
      case 'permission':
      case 'preset': {
        const presets = this.ctx.get('permissionPresets')
        return presets === undefined ? [] : [...(presets.names ?? [])]
      }
      case 'model':
        return this.modelOptions
      case 'resume':
        return this.sessionCache
      case 'provider':
        return ['list', 'add', 'remove', 'test']
      default:
        return []
    }
  }

  /**
   * Workspace paths for `@` completion.
   *
   * A bare fragment searches the workspace (two levels, bounded) instead of only
   * the top directory; a fragment with a slash lists that directory. The result
   * never leaves the workspace: `@../` is not a way to browse the machine. A
   * trailing slash keeps the completion open for the next level.
   *
   * @private
   * @param {string} fragment what was typed after `@`
   * @returns {string[]} paths relative to the workspace
   */
  completePaths(fragment) {
    const text = String(fragment ?? '')
    if (text.startsWith('/') || text.startsWith('~')) return []
    const slash = text.lastIndexOf('/')
    const directory = slash === -1 ? '' : text.slice(0, slash + 1)
    const leaf = slash === -1 ? text : text.slice(slash + 1)
    const base = directory === '' ? this.cwd : resolvePath(this.cwd, directory)
    if (!this.insideWorkspace(base)) return []
    const paths = []
    if (directory === '') {
      this.walkWorkspace(base, '', leaf, paths, 0)
    } else {
      let entries = []
      try {
        entries = readdirSync(base, { withFileTypes: true })
      } catch {
        return []
      }
      for (const entry of entries) {
        if (!entry.name.startsWith(leaf)) continue
        if (entry.name.startsWith('.') && !leaf.startsWith('.')) continue
        paths.push(`${directory}${entry.name}${entry.isDirectory() ? '/' : ''}`)
      }
    }
    return paths.sort()
  }

  /** @private */
  insideWorkspace(target) {
    const relative = relativePath(this.cwd, target)
    return relative === '' || (!relative.startsWith('..') && !isAbsolute(relative))
  }

  /** @private */
  walkWorkspace(directory, prefix, leaf, paths, depth) {
    if (depth > 2 || paths.length >= 200) return
    let entries = []
    try {
      entries = readdirSync(directory, { withFileTypes: true })
    } catch {
      return
    }
    for (const entry of entries) {
      if (paths.length >= 200) return
      if (entry.isDirectory() && SKIP_DIRS.has(entry.name)) continue
      if (entry.name.startsWith('.') && !leaf.startsWith('.')) continue
      const relative = `${prefix}${entry.name}`
      if (entry.isDirectory()) {
        if (entry.name.startsWith(leaf)) paths.push(`${relative}/`)
        this.walkWorkspace(resolvePath(directory, entry.name), `${relative}/`, leaf, paths, depth + 1)
      } else if (entry.name.startsWith(leaf)) {
        paths.push(relative)
      }
    }
  }

  /** @private */
  submitComposer() {
    const value = this.composer.submit()
    this.overlay = null
    this.palette = null
    if (value.trim() === '') {
      this.dirty = true
      return
    }
    if (value.startsWith('!')) {
      this.runShellCommand(value.slice(1).trim())
      return
    }
    if (value.startsWith('/')) {
      const [head, ...rest] = value.trim().slice(1).split(/\s+/u)
      this.runCommand(head, rest.join(' '))
      return
    }
    this.sendPrompt(value)
  }

  /**
   * `!command`: run it in the workspace without going through the agent.
   *
   * This is Kimi Code's shell mode in its smallest useful form. The output is a
   * tool card, not agent context: the terminal cannot inject an arbitrary tool
   * result into the harness's history without pretending the agent called it.
   * The process is tracked so quit does not leave a shell behind.
   *
   * @private
   * @param {string} command
   */
  runShellCommand(command) {
    const text = String(command ?? '').trim()
    if (text === '') return
    const block = this.makeBlock({
      type: 'tool',
      name: 'shell',
      shell: true,
      input: { command: text },
      status: 'in_progress',
      startedAt: this.clock,
      output: '',
    })
    this.activity.tool = 'shell'
    this.activity.startedAt = this.clock
    this.scrollToBottom()
    this.dirty = true
    let output = ''
    let settled = false
    let child
    try {
      child = spawn(text, {
        shell: true,
        cwd: this.cwd,
        env: process.env,
      })
    } catch (error) {
      block.status = 'failed'
      block.output = this.failureText(error)
      block.endedAt = this.clock
      this.activity.tool = null
      this.pushNotice(this.failureText(error), 'error')
      this.dirty = true
      return
    }
    this.shellProcesses.add(child)
    const append = (chunk) => {
      if (output.length >= 64 * 1024) return
      output += String(chunk).slice(0, 64 * 1024 - output.length)
      block.output = output
      this.bump(block)
    }
    child.stdout?.on('data', append)
    child.stderr?.on('data', append)
    const finish = (code, error) => {
      if (settled) return
      settled = true
      this.shellProcesses.delete(child)
      if (error !== undefined) {
        block.status = 'failed'
        block.output = `${output}${this.failureText(error)}`
        this.pushNotice(this.failureText(error), 'error')
      } else {
        block.status = code === 0 ? 'completed' : 'failed'
        block.output = output
        this.pushNotice(this.i18n.t(code === 0 ? 'shell.done' : 'shell.failed', {
          code: String(code ?? '?'),
        }), code === 0 ? 'success' : 'error')
      }
      block.endedAt = this.clock
      this.activity.tool = null
      this.dirty = true
    }
    child.on('error', (error) => finish(undefined, error))
    child.on('close', (code) => finish(code))
  }

  /**
   * `Ctrl-S`: push the composer into the turn that is running.
   *
   * This is the difference between "queue the next message" and "tell the agent
   * now"; Kimi Code has had it for a while and a long tool loop is where it
   * matters most. The in-process line can steer; the ACP protocol cannot, so
   * that line says so instead of silently queuing.
   *
   * @private
   */
  steerComposer() {
    const text = this.composer.text.trim()
    if (text === '') return
    this.composer.setText('')
    this.palette = null
    if (this.status !== 'working' && !this.session.busy) {
      this.sendPrompt(text)
      return
    }
    if (typeof this.session.steer !== 'function') {
      this.pushNotice(this.i18n.t('steer.unavailable'), 'warning')
      this.queue.push(text)
      this.dirty = true
      return
    }
    try {
      this.session.steer(text)
      this.makeBlock({ type: 'user', text, steer: true })
      this.pushNotice(this.i18n.t('steer.sent'), 'success')
      this.scrollToBottom()
      this.dirty = true
    } catch (error) {
      this.pushNotice(this.failureText(error), 'error')
      this.queue.push(text)
      this.dirty = true
    }
  }

  /** @private */
  sendPrompt(text) {
    this.makeBlock({ type: 'user', text })
    this.scrollToBottom()
    if (this.status === 'working' || !this.session.ready || this.session.busy) {
      this.queue.push(text)
      this.dirty = true
      return
    }
    this.deliver(text)
  }

  /** @private */
  deliver(text) {
    try {
      this.session.send(text)
      this.status = 'working'
      this.activity = { tool: null, startedAt: this.clock }
      this.turnStartedAt = this.clock
      this.lastTurnSeconds = null
      this.turnEndedAt = undefined
      this.dirty = true
    } catch (error) {
      this.pushNotice(this.failureText(error), 'error')
    }
  }

  /** @private */
  pumpQueue() {
    if (this.queue.length === 0 || this.status === 'working' || !this.session.ready) return
    const next = this.queue.shift()
    this.deliver(next)
  }

  /** @private */
  cancelTurn() {
    this.session.cancel()
    this.queue = []
    this.dirty = true
  }

  /** @private */
  runCommand(name, argument = '') {
    switch (name) {
      case 'help':
        this.openHelp()
        return
      case 'clear':
        this.blocks = []
        this.transcriptCache = new WeakMap()
        this.screen.clear()
        this.pushBanner()
        this.dirty = true
        return
      case 'model':
        void this.openModelPicker()
        return
      case 'effort':
        this.openEffortPicker()
        return
      case 'yolo':
        this.runYolo(argument)
        return
      case 'permission':
        this.openPermissionPicker(argument)
        return
      case 'plan':
        void this.togglePlan(argument)
        return
      case 'status':
        this.openStatus()
        return
      case 'provider':
        void this.runProvider(argument)
        return
      case 'resume':
      case 'sessions':
        void this.openResumePicker()
        return
      case 'new':
        void this.startNew()
        return
      case 'compact':
        void this.compact()
        return
      case 'allow':
      case 'deny':
      case 'rules':
        this.runRuleCommand(name, argument)
        return
      case 'quit':
      case 'exit':
        void this.quit()
        return
      default:
        // Not ours: the harness registers `/permission`, `/goal`, `/feedback`
        // and `/export`. Asking it is what keeps this terminal honest about what
        // the running version can do.
        void this.runHarnessCommand(name, argument)
    }
  }

  /**
   * Runs one of the harness's own slash commands.
   *
   * The handler decides what it means and answers with text, so the terminal
   * only has to show that text. An unknown name comes back as `undefined`, which
   * is the moment to say "not a command" — after asking, not before.
   *
   * @private
   * @param {string} name the command, without the slash
   * @param {string} argument everything after it
   */
  async runHarnessCommand(name, argument) {
    const commands = this.ctx.get('commands')
    const agent = this.session.live
    if (commands === undefined || agent === null) {
      this.pushNotice(this.i18n.t('cmd.unknown', { name }), 'error')
      return false
    }
    const line = argument === '' ? `/${name}` : `/${name} ${argument}`
    try {
      const execution = await commands.execute(agent, line, [], new AbortController().signal)
      if (execution === undefined) {
        this.pushNotice(this.i18n.t('cmd.unknown', { name }), 'error')
        return false
      }
      const result = execution.result ?? {}
      const text = typeof result.text === 'string' ? result.text : ''
      if (text !== '') this.pushNotice(text, result.kind === 'error' ? 'error' : 'info')
      return result.kind !== 'error'
    } catch (error) {
      this.pushNotice(this.failureText(error), 'error')
      return false
    } finally {
      this.dirty = true
    }
  }

  /**
   * `/yolo` — how much this session asks before running a tool.
   *
   * The harness's own approval policy is ask-or-reject, so the middle and top
   * modes are this terminal's decision, made per request in `requestApproval`.
   * It is a session setting, not a global one: a new session starts at the
   * default again.
   *
   * @private
   * @param {string} argument the argument, if any
   */
  runYolo(argument) {
    const arg = String(argument ?? '').trim().toLowerCase()
    if (arg === 'status') {
      this.pushNotice(this.i18n.t('yolo.status', {
        mode: this.i18n.t(`status.mode.${this.approvalMode}`),
        count: String(this.autoApproved),
        tools: String(this.approvedTools.size),
      }), 'info')
      this.dirty = true
      return
    }
    if (arg === 'forget') {
      this.approvedTools.clear()
      this.pushNotice(this.i18n.t('yolo.forgotten'), 'success')
      this.dirty = true
      return
    }
    if (arg === '') {
      // No argument means "let me pick", not "cycle and hope": a mode is a
      // decision with three names, and the same list the model picker uses makes
      // it one keystroke instead of three.
      this.openYoloPicker()
      return
    }
    if (APPROVAL_MODES.includes(arg)) {
      this.setApprovalMode(arg)
      return
    }
    this.pushNotice(this.i18n.t('yolo.unknown', { mode: arg }), 'error')
    this.dirty = true
  }

  /** @private */
  openYoloPicker() {
    const items = APPROVAL_MODES.map((mode) => ({
      label: this.i18n.t(`status.mode.${mode}`),
      description: this.i18n.t(`yolo.desc.${mode}`),
      tail: mode === this.approvalMode ? this.i18n.t('yolo.current') : '',
      onSelect: () => this.setApprovalMode(mode),
    }))
    this.overlay = {
      kind: 'picker',
      title: this.i18n.t('yolo.title'),
      width: 66,
      maxHeight: 14,
      items,
      index: Math.max(0, APPROVAL_MODES.indexOf(this.approvalMode)),
      hint: this.i18n.t('model.hint'),
    }
    this.dirty = true
  }

  /**
   * Sets the approval level and says what changed.
   *
   * @private
   * @param {'ask'|'critical'|'full'} mode the new level
   */
  setApprovalMode(mode) {
    this.approvalMode = mode
    this.pushNotice(this.i18n.t(`yolo.set.${mode}`), mode === 'full' ? 'warning' : 'success')
    this.refreshBanner()
    this.dirty = true
  }

  /**
   * `/permission` — the harness's sandbox+approval presets, pickable.
   *
   * The harness owns the presets, so the list comes from it; an argument still
   * goes through the harness command, which is the authoritative setter.
   *
   * @private
   * @param {string} argument the preset name, or empty to open the list
   */
  openPermissionPicker(argument) {
    const text = String(argument ?? '').trim()
    const presets = this.ctx.get('permissionPresets')
    const agent = this.session.live
    if (text !== '' || presets === undefined || agent === null) {
      void this.runHarnessCommand('permission', text)
      return
    }
    const catalog = presets.catalog?.() ?? { options: [] }
    const current = presets.current?.(agent.session)
    const items = (catalog.options ?? []).map((option) => ({
      label: option.name ?? option.value,
      description: option.description,
      tail: option.value === current ? this.i18n.t('yolo.current') : '',
      onSelect: () => {
        try {
          presets.set(agent.session, option.value)
          this.pushNotice(this.i18n.t('permission.preset.set', { name: option.name ?? option.value }), 'success')
          this.refreshBanner()
        } catch (error) {
          this.pushNotice(this.failureText(error), 'error')
        }
        this.dirty = true
      },
    }))
    if (items.length === 0) {
      void this.runHarnessCommand('permission', '')
      return
    }
    this.overlay = {
      kind: 'picker',
      title: this.i18n.t('permission.preset.title'),
      width: 66,
      maxHeight: 14,
      items,
      index: Math.max(0, (catalog.options ?? []).findIndex((option) => option.value === current)),
      hint: this.i18n.t('model.hint'),
    }
    this.dirty = true
  }

  /**
   * `/plan` and Shift-Tab: ask the harness to enter or leave Plan mode.
   *
   * The harness owns the actual mode; this app keeps only the badge and the
   * shortcut. If the running harness has no `/plan` command, the toggle is
   * refused instead of pretending the mode changed.
   *
   * @private
   * @param {string} [argument] `on`, `off`, or empty to flip
   */
  async togglePlan(argument = '') {
    const arg = String(argument ?? '').trim().toLowerCase()
    if (arg !== '' && arg !== 'on' && arg !== 'off') {
      this.pushNotice(this.i18n.t('plan.usage'), 'info')
      return
    }
    const next = arg === 'on' ? true : arg === 'off' ? false : !this.planMode
    if (next === this.planMode) {
      this.pushNotice(this.i18n.t(next ? 'plan.on' : 'plan.off'), 'info')
      return
    }
    // Entering is `/plan` **with no argument**: the harness steers whatever text
    // it is given into the running conversation, so `/plan on` would put the word
    // "on" in front of the model. Leaving is `/plan off`, which it reads as a verb.
    const ok = await this.runHarnessCommand('plan', next ? '' : 'off')
    if (!ok) return
    this.planMode = next
    this.refreshBanner()
    this.pushNotice(this.i18n.t(next ? 'plan.on' : 'plan.off'), 'success')
    this.dirty = true
  }

  /**
   * `/allow`, `/deny`, `/rules`: the persistent half of the approval seam.
   *
   * Keys are the exact strings `allowKey()` produces (`<tool> <command>` or
   * `<tool> <path>`), so a rule cannot accidentally become a regex.
   *
   * @private
   * @param {string} name `allow`, `deny`, or `rules`
   * @param {string} argument the rest of the line
   */
  runRuleCommand(name, argument) {
    if (name === 'rules') {
      const arg = String(argument ?? '').trim().toLowerCase()
      if (arg === 'clear') {
        this.rules = { allow: [], deny: [] }
        try {
          saveRules(this.rules)
        } catch (error) {
          this.pushNotice(this.failureText(error), 'error')
          return
        }
        this.pushNotice(this.i18n.t('rule.cleared'), 'success')
        this.dirty = true
        return
      }
      const lines = [
        ...this.rules.allow.map((key) => this.i18n.t('rule.line.allow', { key })),
        ...this.rules.deny.map((key) => this.i18n.t('rule.line.deny', { key })),
      ]
      this.pushNotice(lines.length === 0 ? this.i18n.t('rule.none') : lines.join('\n'), 'info')
      this.dirty = true
      return
    }
    const text = String(argument ?? '').trim()
    const [tool, ...rest] = text.split(/\s+/u).filter((word) => word !== '')
    if (tool === undefined) {
      this.pushNotice(this.i18n.t('rule.usage'), 'info')
      return
    }
    const key = rest.length === 0 ? tool : `${tool} ${rest.join(' ')}`
    this.rules = withRule(this.rules, name === 'deny' ? 'deny' : 'allow', key)
    try {
      saveRules(this.rules)
    } catch (error) {
      this.pushNotice(this.failureText(error), 'error')
      return
    }
    this.pushNotice(this.i18n.t(name === 'deny' ? 'rule.added.deny' : 'rule.added.allow', { key }), 'success')
    this.dirty = true
  }

  /**
   * `/provider` — the model suppliers this terminal can add and remove.
   *
   * The routes live in the `llm-pi-ai` settings section (the same one the web
   * Models page writes); their keys live in the credentials service, addressed by
   * the `apiKeyEnv` name each route carries. Both are touched only after `--yes`:
   * this is a persistent change to the instance's profile, not a session setting.
   *
   * @private
   * @param {string} argument the sub-command and its arguments
   */
  async runProvider(argument) {
    const words = String(argument ?? '').trim().split(/\s+/u).filter((word) => word !== '')
    const action = words[0] ?? 'list'
    const settings = this.ctx.get('settings')
    const llm = this.ctx.get('llm')
    if (settings === undefined || llm === undefined) {
      this.pushNotice(this.i18n.t('provider.needs-services'), 'error')
      return
    }
    try {
      if (action === 'list') {
        await this.listProviders(settings, llm)
        return
      }
      if (action === 'add') {
        await this.addProvider(settings, words.slice(1))
        return
      }
      if (action === 'remove') {
        await this.removeProvider(settings, words.slice(1))
        return
      }
      if (action === 'test') {
        await this.testProvider(llm, words[1])
        return
      }
      this.pushNotice(this.i18n.t('provider.usage'), 'info')
    } catch (error) {
      this.pushNotice(this.failureText(error), 'error')
    }
    this.dirty = true
  }

  /** @private */
  async listProviders(settings, llm) {
    const providers = llm.listProviders()
    if (providers.length === 0) {
      this.pushNotice(this.i18n.t('provider.none'), 'info')
      return
    }
    const credentials = this.ctx.get('credentials')
    const section = this.providerSection(settings)
    const routes = section?.value?.providers ?? {}
    for (const provider of providers) {
      const route = provider.provider ?? provider.id
      const profile = routes[route] ?? {}
      const keyName = typeof profile.apiKeyEnv === 'string' ? profile.apiKeyEnv : ''
      let keyState = this.i18n.t('provider.key.none')
      if (keyName !== '' && credentials !== undefined) {
        const described = await credentials.describe(keyName).catch(() => undefined)
        keyState = described?.present === true
          ? this.i18n.t('provider.key.set', { name: keyName })
          : this.i18n.t('provider.key.missing', { name: keyName })
      }
      const models = Array.isArray(profile.models) ? profile.models.length : 0
      this.pushNotice(this.i18n.t('provider.row', {
        route,
        base: profile.baseURL ?? provider.displayName ?? '',
        api: profile.api ?? '',
        models: String(models),
        key: keyState,
      }), 'info')
    }
  }

  /** @private */
  async addProvider(settings, words) {
    const [route, api, baseURL] = words
    if (route === undefined || api === undefined) {
      this.pushNotice(this.i18n.t('provider.usage'), 'info')
      return
    }
    const flags = this.providerFlags(words.slice(3))
    // Every route names the variable its key resolves from: the one given, or a
    // default derived from the route. A route without a name would resolve no key.
    const keyEnv = flags['key-env'] ?? `${route.toUpperCase().replace(/[^A-Z0-9]+/gu, '_')}_API_KEY`
    const profile = {
      api,
      ...(baseURL === undefined ? {} : { baseURL }),
      ...(flags.display === undefined ? {} : { displayName: flags.display }),
      ...(keyEnv === undefined ? {} : { apiKeyEnv: keyEnv }),
      ...(flags.model === undefined ? {} : {
        models: flags.model.split(',').map((id) => ({ id: id.trim() })).filter((model) => model.id !== ''),
      }),
    }
    const section = this.providerSection(settings)
    if (section === null) {
      this.pushNotice(this.i18n.t('provider.no-section'), 'error')
      return
    }
    if (flags.yes === undefined) {
      // Persistent configuration: show the exact block first, write on `--yes`.
      this.pushNotice(this.i18n.t('provider.confirm', {
        route,
        block: JSON.stringify({ [route]: profile }, null, 2),
      }), 'info')
      return
    }
    await settings.mutate('llm-pi-ai', [{ op: 'set', path: ['providers', route], value: profile }], section.revision)
    if (flags.key !== undefined) {
      const credentials = this.ctx.get('credentials')
      if (credentials === undefined) {
        this.pushNotice(this.i18n.t('provider.no-credentials'), 'error')
        return
      }
      await credentials.set(keyEnv, flags.key)
    }
    this.pushNotice(this.i18n.t('provider.added', { route }), 'success')
    this.modelOptions = []
    this.dirty = true
  }

  /** @private */
  async removeProvider(settings, words) {
    const [route] = words
    if (route === undefined) {
      this.pushNotice(this.i18n.t('provider.usage'), 'info')
      return
    }
    const section = this.providerSection(settings)
    if (section === null) {
      this.pushNotice(this.i18n.t('provider.no-section'), 'error')
      return
    }
    if (!words.includes('--yes')) {
      this.pushNotice(this.i18n.t('provider.confirm-remove', { route }), 'info')
      return
    }
    await settings.mutate('llm-pi-ai', [{ op: 'unset', path: ['providers', route] }], section.revision)
    this.pushNotice(this.i18n.t('provider.removed', { route }), 'success')
    this.modelOptions = []
    this.dirty = true
  }

  /** @private */
  async testProvider(llm, route) {
    if (route === undefined) {
      this.pushNotice(this.i18n.t('provider.usage'), 'info')
      return
    }
    const models = await llm.listModels(route)
    this.pushNotice(this.i18n.t('provider.tested', { route, count: String(models.length) }), 'success')
    this.dirty = true
  }

  /**
   * The `llm-pi-ai` settings section, with the revision a write has to carry.
   *
   * @private
   * @param {object} settings the settings service
   * @returns {{value: object, revision: number} | null} the section, or `null`
   */
  providerSection(settings) {
    let described = []
    try {
      described = settings.describe({ redactSecrets: true }) ?? []
    } catch {
      described = []
    }
    const section = described.find((entry) => entry?.ns === 'llm-pi-ai')
    if (section === undefined) return null
    return { value: section.value ?? {}, revision: section.revision ?? 0 }
  }

  /**
   * `--flag value` pairs from a command line.
   *
   * @private
   * @param {string[]} words the words after the positional ones
   * @returns {Record<string, string>} the flags, `--yes` mapped to `'true'`
   */
  providerFlags(words) {
    const flags = {}
    for (let index = 0; index < words.length; index += 1) {
      const word = words[index]
      if (!word.startsWith('--')) continue
      const name = word.slice(2)
      const next = words[index + 1]
      if (next === undefined || next.startsWith('--')) {
        flags[name] = 'true'
        continue
      }
      flags[name] = next
      index += 1
    }
    return flags
  }

  /**
   * `/status` — the facts a person checks when something feels off.
   *
   * @private
   */
  openStatus() {
    const usage = this.usage
    const rows = [
      [this.i18n.t('status.mode'), this.i18n.t(`status.mode.${this.approvalMode}`)
        + (this.autoApproved > 0 ? ` · ${this.i18n.t('yolo.auto', { count: String(this.autoApproved) })}` : '')],
      [this.i18n.t('banner.plan'), this.planMode ? 'on' : 'off'],
      [this.i18n.t('hint.session').replace(' {id}', ''), this.sessionId === null ? '—' : this.sessionId],
      [this.i18n.t('status.model'), `${this.model.currentLabel ?? this.model.current ?? '—'}`
        + (this.effort.current === null ? '' : ` · ${this.effort.current}`)],
      [this.i18n.t('status.context'), usage.size > 0 ? `${usage.used}/${usage.size}` : '—'],
      [this.i18n.t('status.dsh'), this.env.HDSL_INSTANCE_VERSION ?? '—'],
      [this.i18n.t('status.plugins'), `dsh-inproc-tui v${VERSION}`],
      ['cwd', this.cwd],
    ]
    this.overlay = {
      kind: 'help',
      title: this.i18n.t('status.title'),
      width: 72,
      maxHeight: 20,
      sections: [{ title: this.i18n.t('status.title'), rows }],
    }
    this.dirty = true
  }

  /**
   * The commands offered by `/` and by the help dialog.
   *
   * The terminal's own first, then whatever the harness registered — with the
   * harness's description when both know the name, because that text follows the
   * version the instance runs. A profile without the `commands` service (an
   * older harness, a minimal composition) simply lists the local ones.
   *
   * @private
   * @returns {{name: string, description: string}[]} the merged list
   */
  commandList() {
    const rows = COMMANDS.map((command) => ({ name: command.name, description: this.i18n.t(command.key) }))
    const commands = this.ctx.get('commands')
    const agent = this.session.live
    if (commands === undefined || agent === null) return rows
    let listed = []
    try {
      listed = commands.list(agent) ?? []
    } catch {
      listed = []
    }
    for (const descriptor of listed) {
      if (descriptor === null || typeof descriptor.name !== 'string' || descriptor.name === '') continue
      const existing = rows.find((row) => row.name === descriptor.name)
      if (existing !== undefined) {
        if (typeof descriptor.description === 'string' && descriptor.description !== '') {
          existing.description = descriptor.description
        }
        continue
      }
      rows.push({ name: descriptor.name, description: descriptor.description ?? '' })
    }
    return rows
  }

  /** @private */
  openHelp() {
    this.overlay = {
      kind: 'help',
      title: this.i18n.t('help.title'),
      width: 64,
      maxHeight: 24,
      sections: [
        { title: this.i18n.t('help.commands'), rows: this.commandList().map((command) => [`/${command.name}`, command.description]) },
        {
          title: this.i18n.t('help.keys'),
          rows: [
            ['enter', this.i18n.t('composer.hint.send')],
            ['ctrl+j', this.i18n.t('composer.hint.newline')],
            ['esc', this.i18n.t('hint.esc')],
            ['ctrl+o', this.i18n.t('hint.ctrl-o')],
            ['pgup/pgdn', this.i18n.t('hint.scroll')],
            ['ctrl+c', this.i18n.t('hint.ctrl-c')],
          ],
        },
      ],
    }
    this.dirty = true
  }

  /** @private */
  async openModelPicker() {
    const catalog = await this.session.modelCatalog()
    const current = this.session.currentModel()
    const items = []
    // What `/model` completes from: `provider/model`, the same pair the picker
    // writes into the session.
    this.modelOptions = catalog.flatMap((provider) =>
      provider.models.map((model) => `${provider.provider}/${model.id}`))
    for (const provider of catalog) {
      for (const model of provider.models) {
        items.push({
          label: model.name,
          group: provider.displayName,
          description: model.name === provider.displayName ? undefined : undefined,
          tail: current?.provider === provider.provider && current?.model === model.id
            ? this.i18n.t('model.current')
            : provider.provider,
          onSelect: () => this.selectModel({ provider: provider.provider, model: model.id }),
        })
      }
    }
    if (items.length === 0) {
      this.pushNotice(this.i18n.t('notice.error') + ': the profile lists no model', 'error')
      return
    }
    this.overlay = { kind: 'picker', title: this.i18n.t('model.title'), width: 62, maxHeight: 20, items, index: 0 }
    this.dirty = true
  }

  /** @private */
  selectModel(selection) {
    this.session.select(selection)
    this.syncSelection()
    this.pushNotice(`${this.i18n.t('model.title')} → ${selection.model}`, 'success')
  }

  /** @private */
  openEffortPicker() {
    const current = this.session.currentModel()?.reasoningEffort
    const items = EFFORTS.map((effort) => ({
      label: this.i18n.t(`effort.${effort.value}`) === `effort.${effort.value}` ? effort.value : this.i18n.t(`effort.${effort.value}`),
      tail: effort.value === current ? this.i18n.t('model.current') : undefined,
      onSelect: () => {
        this.session.select({ reasoningEffort: effort.value })
        this.syncSelection()
        this.pushNotice(`${this.i18n.t('effort.title')} → ${effort.value}`, 'success')
      },
    }))
    this.overlay = { kind: 'picker', title: this.i18n.t('effort.title'), width: 52, maxHeight: 12, items, index: 0 }
    this.dirty = true
  }

  /** @private */
  async openResumePicker() {
    if (this.ctx.get('sessionPersistence') === undefined) {
      // An empty list would read as "you have no sessions"; the truth is that this
      // profile cannot store them at all.
      this.pushNotice(this.i18n.t('resume.no-persistence'), 'info')
      return
    }
    const sessions = await this.session.listSessions()
    this.sessionCache = sessions.map((session) => session.id)
    const now = Date.now()
    const items = sessions.slice(0, 40).map((session) => {
      const current = session.id === this.sessionId
      const where = session.cwd === this.cwd
        ? this.i18n.t('resume.here')
        : (session.cwd !== '' ? session.cwd : this.i18n.t('resume.other'))
      const age = Number.isFinite(session.at) && session.at > 0 ? ` · ${ageLabel(now - session.at)}` : ''
      const title = typeof session.title === 'string' && session.title !== ''
        ? session.title
        : shortId(session.id)
      // A session whose first prompt could not be read is still listed: hiding a
      // real conversation is worse than showing its id with a warning.
      const unreadable = session.unreadable === true
        ? ` · ${this.i18n.t('resume.unreadable')}`
        : ''
      return {
        // The prompt is what identifies the conversation; the id is metadata,
        // not a label. Empty artifacts never reach this point (listSessions drops
        // them), so every row answers "which conversation is this?".
        label: `${title}${unreadable}${current ? ` · ${this.i18n.t('resume.current')}` : ''}`,
        tail: `${shortId(session.id)} · ${where}${age}`,
        onSelect: () => void this.resumeSession(session.id),
      }
    })
    if (items.length === 0) {
      this.pushNotice(this.i18n.t('resume.empty'), 'info')
      return
    }
    this.overlay = {
      kind: 'picker',
      title: this.i18n.t('resume.title'),
      width: 76,
      maxHeight: 20,
      items,
      index: 0,
      emptyText: this.i18n.t('resume.empty'),
      hint: this.i18n.t('resume.hint'),
    }
    this.dirty = true
  }

  /**
   * Ends the current turn before the session under it is replaced.
   *
   * `/new` and `/resume` used to clear the transcript and swap the agent while
   * a turn was still running: the running turn's events then landed on the new
   * session's transcript, and a queued message could be delivered to it. What
   * has to be true before the swap is that nothing is running, nothing is
   * queued, and no streaming block is left holding a half-answer.
   *
   * @private
   */
  async detachTurn() {
    this.queue = []
    this.activity = { tool: null, startedAt: null }
    this.turnStartedAt = null
    this.streaming = { text: null, reasoning: null }
    // The approval state belongs to the conversation being left: "run for this
    // session" is a promise about *this* session, and a tool call remembered from
    // it has no meaning in the next one.
    this.approvedTools.clear()
    this.autoApproved = 0
    // The mode was chosen for the conversation being left. A new or resumed
    // session starts at the Kimi-style default again, rather than inheriting a
    // `full` that was meant for the previous task.
    this.approvalMode = DEFAULT_APPROVAL_MODE
    this.toolCalls.clear()
    this.reasonDraft = null
    const running = this.status === 'working' || this.session.busy
    if (this.session.ready && running) {
      this.session.cancel()
      await this.session.settle()
    }
    this.status = 'ready'
    this.dirty = true
  }

  /**
   * Puts a resumed conversation's earlier turns back on screen.
   *
   * The session log is the only record of them: the agent that produced them is
   * gone. Read defensively — a log that cannot be projected is not a reason to
   * lose the resume, so a failure is a debug line, not a notice.
   *
   * @private
   * @param {object[]} events the session log, oldest first
   */
  replayHistory(events) {
    const start = Math.max(0, events.length - MAX_REPLAY_EVENTS)
    for (let index = start; index < events.length; index += 1) {
      const event = events[index]
      try {
        if (event?.type === 'user/message' && event.data?.source?.kind === 'user') {
          const text = textOfMessage(event.data)
          if (text.trim() !== '') this.makeBlock({ type: 'user', text })
          continue
        }
        if (event?.type === 'assistant/message') {
          const text = textOfMessage(event.data.message)
          if (text.trim() !== '') this.makeBlock({ type: 'assistant', text, streaming: false })
          continue
        }
        if (event?.type === 'tool/call') {
          this.makeBlock({
            type: 'tool',
            callId: event.data.callId,
            name: event.data.name ?? 'tool',
            input: parseArguments(event.data.arguments),
            status: 'in_progress',
            startedAt: this.clock,
            output: null,
          })
          continue
        }
        if (event?.type === 'tool/result') {
          const block = this.findTool(event.data.message?.toolCallId)
          if (block === null) continue
          block.output = textOfMessage(event.data.message)
          block.status = event.data.message?.isError === true ? 'failed' : 'completed'
          block.endedAt = this.clock
        }
      } catch (error) {
        this.debug(error)
      }
    }
    this.transcriptCache = new WeakMap()
    this.dirty = true
  }

  /** @private */
  async resumeSession(sessionId) {
    try {
      await this.detachTurn()
      this.blocks = this.blocks.filter((block) => block.type === 'banner')
      this.transcriptCache = new WeakMap()
      const result = await this.session.resume(sessionId)
      this.syncSelection()
      if (result?.resumed !== true) {
        // The registry refused the id (a typo, another workspace's session) and
        // the agent fell back to a fresh one. Saying "resumed" here would be a
        // lie the person only notices once the history is missing.
        const reason = this.session.resumeFailure
        this.blocks = []
        this.pushBanner()
        this.pushNotice(this.i18n.t('resume.failed', {
          id: shortId(sessionId),
          error: reason === undefined ? '' : this.failureText(reason),
        }), 'error')
      } else {
        this.refreshBanner()
        this.replayHistory(this.session.history())
        this.pushNotice(this.i18n.t('banner.resume', { id: shortId(sessionId) }), 'info')
      }
    } catch (error) {
      this.pushNotice(this.failureText(error), 'error')
    }
    this.dirty = true
  }

  /** @private */
  async startNew() {
    try {
      await this.detachTurn()
      await this.session.close()
      await this.session.open({})
      this.blocks = []
      this.transcriptCache = new WeakMap()
      this.screen.clear()
      this.pushBanner()
      this.syncSelection()
      this.pushNotice(this.i18n.t('banner.new'), 'success')
    } catch (error) {
      this.pushNotice(this.failureText(error), 'error')
    }
    this.dirty = true
  }

  /** @private */
  async compact() {
    try {
      const available = await this.session.compact()
      if (!available) {
        this.pushNotice(this.i18n.t('cmd.unknown', { name: 'compact' }), 'error')
        return
      }
      this.pushNotice(this.i18n.t('notice.compacted'), 'success')
      this.usage = this.session.usage()
      this.dirty = true
    } catch (error) {
      this.pushNotice(this.failureText(error), 'error')
    }
  }

  /** @private */
  syncSelection() {
    const current = this.session.currentModel()
    this.model = {
      current: current?.model ?? null,
      currentLabel: current?.model ?? null,
      options: [],
    }
    this.effort = { current: current?.reasoningEffort ?? null, options: [] }
    this.usage = this.session.usage()
    this.refreshBanner()
    this.dirty = true
  }

  // ── ticks, scrolling, painting ──────────────────────────────────────────

  /** @private */
  scrollBy(lines) {
    const total = this.transcriptTotal ?? 0
    const height = Math.max(1, this.screen.rows - 8)
    const max = Math.max(0, total - height)
    const base = this.scroll.follow ? 0 : this.scroll.offset
    this.scroll.offset = Math.max(0, Math.min(max, base + lines))
    this.scroll.follow = this.scroll.offset === 0
    this.dirty = true
  }

  /** @private */
  scrollToBottom() {
    this.scroll.follow = true
    this.scroll.offset = 0
    this.dirty = true
  }

  /** @private */
  tick() {
    try {
      this.clock = Date.now()
      if (this.status === 'working') this.dirty = true
      // The harness shares this process and may write a log line to stderr at any
      // moment (a provider catalogue refresh does exactly that). Repainting every
      // row now and then is what removes such a line: a paint that only diffs
      // against the previous frame would keep it there. The cadence is the
      // compromise — often enough to clean up, rare enough not to churn a
      // browser terminal's input method (`Screen#paint` writes nothing when the
      // frame is identical, so this is the only forced repaint left).
      if (this.clock - (this.lastFullPaint ?? 0) > 2000) {
        this.lastFullPaint = this.clock
        this.screen.previous = []
        this.dirty = true
      }
      if (this.dirty) this.render()
    } catch (error) {
      this.reportError(error, 'tick')
    }
  }

  /** Paints one frame; a broken frame must not escape into the host's timers. */
  render() {
    this.dirty = false
    try {
      const { columns, rows } = this.screen.measure()
      if (columns < 20 || rows < 6) {
        this.screen.paint([], null)
        return
      }
      const frame = renderFrame(this, {
        width: columns,
        height: rows,
        theme: this.theme,
        i18n: this.i18n,
        clock: this.clock || Date.now(),
      })
      this.transcriptTotal = frame.transcript.total
      this.screen.paint(frame.rows, frame.cursor)
    } catch (error) {
      this.reportError(error, 'render')
      // A minimal row is the only thing safe to draw from a broken frame.
      try {
        this.screen.paint([this.failureText(error)], null)
      } catch {
        // The terminal itself may be gone.
      }
    }
  }

  /**
   * Turns a failure into a notice (rate-limited) and a debug line.
   *
   * @param {unknown} error
   * @param {string} where the callback that failed
   * @private
   */
  reportError(error, where) {
    this.debug(error)
    // An interrupted question is an outcome the asker asked for, not a fault.
    if (isUserQuestionError(error)) return
    const now = Date.now()
    if (now - this.lastErrorAt < ERROR_NOTICE_MS) return
    this.lastErrorAt = now
    const message = error?.message ?? String(error)
    this.pushNotice(`${this.i18n.t('notice.error')} (${where}): ${message}`, 'error')
    this.dirty = true
  }

  /** @private */
  debug(error) {
    // The ACP terminal reads HSDL_TUI_DEBUG; both names are honoured here so one
    // variable debugs either program.
    if (this.env.HSDL_TUI_DEBUG === undefined && this.env.HSDL_UI_DEBUG === undefined) return
    try {
      process.stderr.write(`dsh-inproc-tui: ${error?.stack ?? error}\n`)
    } catch {
      // the terminal may already be gone
    }
  }

  /** @private */
  failureText(error) {
    if (error === null || error === undefined) return this.i18n.t('error.acp', { message: 'unknown' })
    return this.i18n.t('error.acp', { message: error.message ?? String(error) })
  }

  // ── exit ────────────────────────────────────────────────────────────────

  /**
   * Restores the terminal and asks the launcher to end this process.
   *
   * Quitting must never outlive the wait for an agent: a blocked approval or a
   * running turn would keep the harness process alive forever, so the waiting
   * dialogues are settled, the turn is cancelled and the detach is bounded.
   *
   * @param {number} [code] the exit code
   */
  async quit(code) {
    if (this.stopping) return
    this.stopping = true
    if (code !== undefined) this.exitCode = code
    if (this.timer !== null) clearInterval(this.timer)
    this.timer = null
    if (this.escTimer !== null) clearTimeout(this.escTimer)
    this.escTimer = null
    for (const child of this.shellProcesses) {
      try {
        child.kill('SIGTERM')
      } catch {
        // The command may already have exited.
      }
    }
    this.shellProcesses.clear()
    for (const dispose of this.disposers) {
      try {
        dispose()
      } catch {
        // A disposed plugin scope may already be gone.
      }
    }
    this.disposers = []
    // Unblock the tools that are waiting on a dialogue, then stop the turn they
    // belong to; only then is waiting for idle a wait that can end.
    this.dismissInteractions()
    try {
      if (this.session.busy) this.session.cancel()
    } catch (error) {
      this.debug(error)
    }
    try {
      this.screen.leave()
    } catch (error) {
      this.debug(error)
    }
    try {
      await withTimeout(this.session.close(), QUIT_CLOSE_MS)
    } catch (error) {
      this.debug(error)
    }
    if (typeof process.stdin.setRawMode === 'function') {
      try {
        process.stdin.setRawMode(false)
      } catch {
        // the PTY may already be gone
      }
    }
    try {
      process.stdin.removeAllListeners('data')
      process.stdin.pause()
    } catch (error) {
      this.debug(error)
    }
    for (const [signal, handler] of this.signalHandlers ?? []) process.removeListener(signal, handler)
    this.resolveExit(this.exitCode)
    // The exit seam is read last and defensively: a throw here would leave the
    // process running after the screen was already restored — and callers of
    // `quit()` do not await it, so a rejection would be an uncaught one.
    let exit
    try {
      exit = this.ctx.get('appExit')
    } catch {
      exit = undefined
    }
    if (typeof exit === 'function') {
      try {
        exit(this.exitCode)
      } catch (error) {
        this.debug(error)
        process.exitCode = this.exitCode
      }
    } else if (this.headless) {
      // `--help` / no TTY: nothing else owns this process, so end it here
      // instead of hanging behind the harness's event loop.
      process.exit(this.exitCode)
    } else {
      process.exitCode = this.exitCode
    }
  }

  /** Resolves after the terminal has been restored. */
  waitForExit() {
    return this.exitPromise
  }
}

/** Queues one interaction and shows it when the overlay is free. */
function enqueue(app, item) {
  app.interactions.push(item)
  app.showNextInteraction()
}

/** Drops an interaction that is still queued, or advances past the active one. */
function retire(app, item) {
  const index = app.interactions.indexOf(item)
  if (index !== -1) {
    app.interactions.splice(index, 1)
    return
  }
  if (app.activeInteraction === item) {
    app.activeInteraction = null
    app.showNextInteraction()
  }
}

function parseArguments(value) {
  if (typeof value !== 'string') return value ?? null
  try {
    return JSON.parse(value)
  } catch {
    return value
  }
}

function textOfMessage(message) {
  if (message === undefined || message === null) return ''
  const content = message.content
  if (!Array.isArray(content)) return ''
  return content
    .map((block) => {
      if (block?.type === 'text') return String(block.text ?? '')
      if (block?.type === 'reasoning') return ''
      return ''
    })
    .join('')
}

/** Reads the checked-out branch name, for the status row. */
function detectGitBranch(cwd) {
  let directory = cwd
  for (let depth = 0; depth < 12; depth += 1) {
    try {
      const text = readFileSync(`${directory}/.git/HEAD`, 'utf8').trim()
      const match = /^ref:\s*refs\/heads\/(.+)$/u.exec(text)
      return match === null ? text.slice(0, 7) : match[1]
    } catch {
      const parent = directory.replace(/\/[^/]*$/u, '')
      if (parent === directory || parent === '') return ''
      directory = parent
    }
  }
  return ''
}

/**
 * Settles after `promise` or after `ms`, whichever comes first.
 *
 * Quitting is not a place to wait for a harness that may never answer. The
 * timer stays referenced: a quit that exits into an empty event loop would
 * leave the process without restoring the terminal or reporting its code.
 */
function withTimeout(promise, ms) {
  return new Promise((resolve) => {
    const timer = setTimeout(resolve, ms)
    Promise.resolve(promise).then(
      (value) => {
        clearTimeout(timer)
        resolve(value)
      },
      () => {
        clearTimeout(timer)
        resolve(undefined)
      },
    )
  })
}
