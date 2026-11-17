/**
 * The agent this terminal drives.
 *
 * Unlike the ACP terminal, this one lives inside the harness process: the bundle
 * patch mounts it over `dsh-base`, so it talks to the agent registry and the
 * session log directly. That is what buys token-level streaming, live reasoning,
 * approval prompts and model switching without a protocol in between — at the
 * price of only working on the 0.2.x harness line, which is the trade this
 * plugin deliberately makes.
 *
 * @module agent
 */

import { randomUUID } from 'node:crypto'
import { readdirSync, statSync } from 'node:fs'
import { homedir } from 'node:os'
import { basename, join } from 'node:path'

// The harness packages are imported when a session is opened, not when this
// module loads. They are peers: a terminal that never reaches `open()` — the
// `--help` path, a profile whose registry is missing — must still be loadable,
// and the two error messages below are the ones that say so. It is also what
// lets this module be exercised without a harness installed.

/** How long a detach waits for the agent to go idle before giving up on it. */
const SETTLE_TIMEOUT_MS = 2_000

/** How many events one session-index read asks for. */
const SESSION_INDEX_READ = 64
/** Stop looking for the first prompt after this many events. */
const SESSION_INDEX_LIMIT = 2_048
/** How many characters of the first prompt the picker shows. */
const SESSION_TITLE_CHARS = 40
/** Newest artifacts read when building the `/sessions` index. */
const SESSION_INDEX_MAX = 200
/** Read handles opened at once; a large store must not open hundreds in parallel. */
const SESSION_INDEX_CONCURRENCY = 8
/** Resolved first prompts kept in memory; a title never changes once found. */
const PROMPT_CACHE_MAX = 1_000

/** A model choice as the harness records it. */
export class ChatSession {
  /**
   * @param {object} ctx the Cordis context of the plugin
   * @param {string} cwd the workspace the session is opened in
   */
  constructor(ctx, cwd) {
    this.ctx = ctx
    this.cwd = cwd
    this.agent = null
    this.dispose = null
    this.sessionId = null
    /** The state object the harness assembles prompts from; see {@link open}. */
    this.modelState = null
    /** Set by {@link open}: the harness's own user-message constructor. */
    this.createUserMessage = undefined
  }

  /** The live agent, or `null` before {@link open}. */
  get live() {
    return this.agent
  }

  /**
   * Creates a fresh agent, or resumes one by id.
   *
   * @param {{resume?: string | null}} [options]
   * @returns {Promise<{resumed: boolean}>}
   */
  async open(options = {}) {
    const agents = this.ctx.get('agents')
    const defaultModel = this.ctx.get('agentDefaultModel')
    if (agents === undefined) {
      throw new Error('dsh-inproc-tui: this profile does not mount the agent registry (dsh-base does)')
    }
    if (defaultModel === undefined) {
      throw new Error('dsh-inproc-tui: this harness exposes no agentDefaultModel; the profile has no model route to start')
    }
    const selection = defaultModel.currentSelection()
    const agentOptions = { ...selection }
    // Loaded here rather than at module scope, so this module can be read without
    // a harness: both are peers, and one of them (`dsh-llm`) is only needed once a
    // prompt is actually sent.
    const { installModelSelection } = await import('@deepseek-ai/dsh-agent')
    const { createUserMessage } = await import('@deepseek-ai/dsh-llm')
    this.createUserMessage = createUserMessage
    // `installModelSelection` takes the *state object* the prompt assembly reads
    // (`{current, assembled}`), not just the context: without it the harness's
    // assemble hook dereferences undefined on the first step.
    //
    // The object is kept: `current` is what the harness's assemble hook reads on
    // **every** step, so a later `/model` has to write here — appending the
    // durable event alone only changed what the log said.
    const setup = (agentCtx, agent) => {
      const state = {
        current: agent?.model?.current?.() ?? selection,
        assembled: undefined,
      }
      this.modelState = state
      installModelSelection(agentCtx, state)
    }
    this.modelState = null
    this.resumeFailure = undefined

    let resumed = false
    if (options.resume) {
      try {
        const result = await agents.resume({ resumeSessionId: options.resume, agentOptions, setup })
        this.agent = result.agent
        this.dispose = result.dispose
        resumed = true
      } catch (error) {
        // A typo or a session from another workspace falls back to a new
        // conversation, and the app tells the person what happened.
        this.resumeFailure = error
      }
    }
    if (this.agent === null) {
      const result = await agents.create({
        sessionId: `session-${randomUUID()}`,
        meta: { cwd: this.cwd },
        agentOptions,
        setup,
      })
      this.agent = result.agent
      this.dispose = result.dispose
    }
    this.sessionId = this.agent.session.id
    await this.agent.whenIdle()
    return { resumed }
  }

  /** Whether an agent is open and can take a prompt. */
  get ready() {
    return this.agent !== null
  }

  /** Whether the agent is currently running a turn. */
  get busy() {
    return this.agent !== null && this.agent.status === 'running'
  }

  /**
   * Queues one user prompt.
   * @param {string} text
   */
  send(text) {
    if (this.agent === null) throw new Error('dsh-inproc-tui: no agent is open')
    if (this.createUserMessage === undefined) throw new Error('dsh-inproc-tui: the session was never opened')
    this.agent.followup(this.createUserMessage({
      content: [{ type: 'text', text }],
      source: { kind: 'user' },
    }))
  }

  /**
   * Feeds text to the step that is running, instead of queuing the next turn.
   *
   * This is Kimi Code's `Ctrl-S`: the agent sees the message in the current
   * turn's next step. An idle agent starts a turn, so the caller may use it
   * without checking status.
   *
   * @param {string} text
   */
  steer(text) {
    if (this.agent === null) throw new Error('dsh-inproc-tui: no agent is open')
    if (this.createUserMessage === undefined) throw new Error('dsh-inproc-tui: the session was never opened')
    this.agent.steer(this.createUserMessage({
      content: [{ type: 'text', text }],
      source: { kind: 'user' },
    }))
  }

  /** Interrupts whatever the agent is doing. */
  cancel() {
    this.agent?.cancel({ kind: 'user' })
  }

  /**
   * Waits until the agent is quiet, but never forever.
   *
   * A tool parked on an approval or a question keeps the agent non-idle; a
   * quit that waits for idle without a bound hangs the harness process.
   *
   * @param {number} [timeoutMs]
   */
  async settle(timeoutMs = SETTLE_TIMEOUT_MS) {
    const agent = this.agent
    if (agent === null) return
    let timer = null
    try {
      await Promise.race([
        Promise.resolve(agent.whenIdle()).catch(() => {}),
        // Not unref'd: the wait *is* the work here, and a quit that vanishes
        // into an empty event loop would leave the terminal unrestored.
        new Promise((resolve) => {
          timer = setTimeout(resolve, timeoutMs)
        }),
      ])
    } finally {
      if (timer !== null) clearTimeout(timer)
    }
  }

  /** Persists the session log, when the profile mounts persistence. */
  async flush() {
    if (this.agent === null) return
    try {
      await this.ctx.get('sessions')?.flush(this.agent.session)
    } catch {
      // A flush failure must not keep the terminal alive.
    }
  }

  /** The selection the next step will use, when known. */
  /**
   * The selection the next step will use.
   *
   * Read from the durable `model/selection` events rather than `agent.model`:
   * that accessor is not present on every 0.2.x build, while the event is the
   * projection both the harness and this terminal read anyway.
   *
   * @returns {{provider: string, model: string, reasoningEffort?: string} | null}
   */
  currentModel() {
    const session = this.agent?.session
    if (session === undefined) return null
    try {
      const events = session.snapshotEvents()
      for (let index = events.length - 1; index >= 0; index -= 1) {
        const event = events[index]
        if (event?.type === 'model/selection' && typeof event.data?.provider === 'string' && typeof event.data?.model === 'string') {
          return event.data
        }
      }
    } catch {
      // A session that cannot be read falls through to the options below.
    }
    const options = this.agent?.options
    if (typeof options?.provider === 'string' && typeof options?.model === 'string' && options.provider !== '' && options.model !== '') {
      return {
        provider: options.provider,
        model: options.model,
        ...(options.reasoningEffort === undefined ? {} : { reasoningEffort: options.reasoningEffort }),
      }
    }
    return null
  }

  /**
   * Switches the route and/or effort for the next step.
   * @param {{provider?: string, model?: string, reasoningEffort?: string}} selection
   */
  select(selection) {
    if (this.agent === null) return
    const current = this.currentModel()
    const effort = selection.reasoningEffort ?? current?.reasoningEffort
    const chosen = {
      provider: selection.provider ?? current?.provider ?? '',
      model: selection.model ?? current?.model ?? '',
      ...(effort === undefined ? {} : { reasoningEffort: effort }),
    }
    // What the next step actually assembles with. The harness's assemble hook
    // reads this object's `current` on every step, so writing only the event
    // below left the request on the old route while the UI said otherwise.
    if (this.modelState !== null && this.modelState !== undefined) {
      this.modelState.current = { ...chosen }
    }
    // The same durable event `agent.model.select` writes: a later step reads the
    // newest selection, and the log is what survives a resume.
    this.agent.session.append('model/selection', chosen)
  }

  /**
   * The live agent's session log, oldest first.
   *
   * Used to put a resumed conversation back on screen: the log is the only
   * place the earlier turns exist once the previous agent is gone.
   *
   * @returns {object[]} the events, or an empty list when none can be read
   */
  history() {
    try {
      return this.agent?.session?.snapshotEvents() ?? []
    } catch {
      return []
    }
  }

  /**
   * Every provider the profile mounted, with its catalogue.
   * @returns {Promise<{provider: string, displayName: string, models: {id: string, name: string}[]}[]>}
   */
  async modelCatalog() {
    const llm = this.ctx.get('llm')
    if (llm === undefined) return []
    const providers = llm.listProviders()
    const rows = []
    for (const provider of providers) {
      let models = []
      try {
        models = await llm.listModels(provider.provider ?? provider.id)
      } catch {
        models = []
      }
      rows.push({
        provider: provider.provider ?? provider.id,
        displayName: provider.displayName ?? provider.provider ?? provider.id,
        models: models.map((model) => ({ id: model.id ?? model.model, name: model.name ?? model.id ?? model.model })),
      })
    }
    return rows
  }

  /** Context occupancy, from the token meter when the profile mounts one. */
  usage() {
    const session = this.agent?.session
    if (session === undefined) return { used: 0, size: 0 }
    let used = 0
    try {
      const meter = this.ctx.get('tokenMeter')
      used = meter?.measure(session)?.totalTokens ?? 0
    } catch {
      used = 0
    }
    let size = 0
    try {
      size = session.requestContext()?.contextWindow ?? 0
    } catch {
      size = 0
    }
    return { used, size }
  }

  /**
   * Stored sessions that have a human prompt, newest activity first.
   *
   * Persistence lists every artifact it has, including the empty session an
   * accidental `/new`, a failed resume or a killed start leaves behind. Those
   * have no first prompt and are not useful in `/sessions`, so their first few
   * events are read and the ones that really are empty are dropped.
   *
   * A read that fails or stops at the scan cap is **kept**, with an empty title.
   * An unreadable real conversation is worse to hide than to show with its id:
   * upstream's digest keeps uncertain reads visible for the same reason.
   *
   * @returns {Promise<{id: string, cwd: string, at: number, title: string, unreadable: boolean}[]>}
   */
  async listSessions() {
    const persistence = this.ctx.get('sessionPersistence')
    if (persistence === undefined) return []
    let snapshots = []
    try {
      snapshots = await persistence.list()
    } catch {
      return []
    }
    // `list()` promises no order and a store can be large. Work newest-first so
    // the picker's 40 rows are populated from the useful end, and read with a
    // small pool: one read handle per session, but not hundreds at once.
    const mtimes = sessionFileMtimes()
    const ordered = [...snapshots]
      .sort((left, right) => sessionTime(right, mtimes) - sessionTime(left, mtimes))
      .slice(0, SESSION_INDEX_MAX)
    const rows = []
    let next = 0
    const worker = async () => {
      while (next < ordered.length) {
        const snapshot = ordered[next]
        next += 1
        const id = snapshot.header?.id ?? ''
        if (id === '') continue
        const prompt = await readSessionPrompt(persistence, id)
        if (prompt.state === 'empty') continue
        rows.push({
          id,
          cwd: snapshot.header?.cwd ?? '',
          at: sessionTime(snapshot, mtimes),
          title: prompt.title ?? '',
          unreadable: prompt.state !== 'prompt',
        })
      }
    }
    await Promise.all(Array.from(
      { length: Math.min(SESSION_INDEX_CONCURRENCY, ordered.length) },
      worker,
    ))
    return rows.sort((left, right) => Number(right.at) - Number(left.at))
  }

  /**
   * Resumes a stored session, replacing the live one.
   * @param {string} sessionId
   */
  async resume(sessionId) {
    await this.close()
    return this.open({ resume: sessionId })
  }

  /**
   * Runs the profile's compaction over this agent.
   * @returns {Promise<boolean>} whether a compaction service was available
   */
  async compact() {
    const compaction = this.ctx.get('compaction')
    if (compaction === undefined || this.agent === null) return false
    await compaction.compactNow(this.agent, new AbortController().signal)
    return true
  }

  /**
   * Detaches and forgets the current agent.
   *
   * @param {{settleTimeoutMs?: number}} [options]
   */
  async close(options = {}) {
    await this.settle(options.settleTimeoutMs)
    await this.flush()
    try {
      this.dispose?.()
    } catch {
      // Disposal is best effort; the registry drops the agent either way.
    }
    this.dispose = null
    this.agent = null
    this.sessionId = null
    this.modelState = null
  }
}

/**
 * Resolved first prompts, one map per persistence service.
 *
 * Keyed by the service so a remounted or replaced store cannot answer from the
 * previous one's titles; each map is dropped with its service.
 */
const promptCaches = new WeakMap()

/** @param {object} persistence the session persistence service @returns {Map<string, object>} */
function promptCacheFor(persistence) {
  let cache = promptCaches.get(persistence)
  if (cache === undefined) {
    cache = new Map()
    promptCaches.set(persistence, cache)
  }
  return cache
}

/**
 * The first prompt of one stored session, resolved at most once.
 *
 * Only a resolved prompt is cached: it is the head of an append-only log and
 * never changes. Empty and inconclusive answers can still turn into a
 * conversation, so they are re-read the next time `/sessions` opens.
 *
 * @param {object} persistence the session persistence service
 * @param {string} id the stored session id
 * @returns {Promise<{state: 'prompt'|'empty'|'uncertain'|'unreadable', title?: string}>}
 */
async function readSessionPrompt(persistence, id) {
  const cache = promptCacheFor(persistence)
  const cached = cache.get(id)
  if (cached !== undefined) return cached
  const result = await readPromptFromStore(persistence, id)
  if (result.state === 'prompt') {
    if (cache.size >= PROMPT_CACHE_MAX) cache.clear()
    cache.set(id, result)
  }
  return result
}

/**
 * Read enough of one session to tell an empty artifact from a real conversation.
 *
 * `user/message` is the normal shape, but an early prompt can still live in an
 * `agent/inbox/spliced` event: the inbox is written before the turn claims it.
 * Both are checked. A source that is missing or null counts as human (old logs
 * predate the source field); runtime-context and tool notices do not.
 *
 * @param {object} persistence the session persistence service
 * @param {string} id the stored session id
 * @returns {Promise<{state: 'prompt'|'empty'|'uncertain'|'unreadable', title?: string}>}
 */
async function readPromptFromStore(persistence, id) {
  let handle = null
  let reachedEnd = false
  try {
    handle = await persistence.open(id, 'read')
    let offset = 0
    while (offset < SESSION_INDEX_LIMIT) {
      const result = await handle.read(offset, SESSION_INDEX_READ)
      const events = Array.isArray(result?.events) ? result.events : []
      // A short read is not an EOF promise: the contract says `length` is a
      // maximum, not that a backend must fill it. Only an empty slice proves the
      // log ended; stopping at a short slice could delete a real conversation.
      if (events.length === 0) {
        reachedEnd = true
        break
      }
      for (const event of events) {
        const text = promptFromEvent(event)
        if (text.trim() !== '') return { state: 'prompt', title: shortenPrompt(text) }
      }
      offset += events.length
    }
    // Hitting the cap without finding a prompt is not proof of an empty session:
    // keep it visible and let the person decide, rather than deleting a real one.
    return reachedEnd ? { state: 'empty' } : { state: 'uncertain' }
  } catch {
    return { state: 'unreadable' }
  } finally {
    try {
      await handle?.close()
    } catch {
      // Closing a read handle is best effort.
    }
  }
}

/** The first human text in one stored event, or ''. */
function promptFromEvent(event) {
  if (event?.type === 'user/message') {
    return isHumanSource(event.data?.source) ? messageText(event.data) : ''
  }
  if (event?.type === 'agent/inbox/spliced') {
    const inserted = event.data?.inserted
    if (!Array.isArray(inserted)) return ''
    for (const message of inserted) {
      if (!isHumanSource(message?.source)) continue
      const text = messageText(message)
      if (text.trim() !== '') return text
    }
  }
  return ''
}

/**
 * Whether a stored message came from the person.
 *
 * Older logs do not carry `source` at all; those are human by default. The
 * runtime context and tool-job notices are typed, so they are not.
 */
function isHumanSource(source) {
  if (source === null || source === undefined) return true
  if (typeof source === 'string') return source === 'user'
  return typeof source === 'object' && source.kind === 'user'
}

/** The text blocks of a stored message; old logs may store content as a string. */
function messageText(message) {
  const content = message?.content
  if (typeof content === 'string') return content
  if (Array.isArray(content)) {
    return content
      .map((block) => (block?.type === 'text' ? String(block.text ?? '') : ''))
      .join('')
  }
  if (content !== null && typeof content === 'object' && content.type === 'text') {
    return String(content.text ?? '')
  }
  return ''
}

/** Collapse whitespace and cap a prompt at a readable picker label. */
function shortenPrompt(text) {
  const flat = String(text ?? '')
    .replace(/[\u0000-\u001f\u007f]+/gu, ' ')
    .replace(/\s+/gu, ' ')
    .trim()
  if (flat === '') return ''
  const chars = Array.from(flat)
  return chars.length <= SESSION_TITLE_CHARS
    ? flat
    : `${chars.slice(0, SESSION_TITLE_CHARS - 1).join('')}…`
}

/**
 * Last-write time per stored session, from the JSONL artifacts under the
 * session roots.
 *
 * The persistence snapshot has `createdAt` but no `updatedAt`, so sorting by the
 * header alone puts a months-old session that is used every day below a session
 * created yesterday and never opened. The file mtime is the cheapest signal
 * that actually follows writes; when the layout is not a JSONL store (or no
 * root exists), callers fall back to `createdAt`.
 *
 * The roots and the `sessions/<workspace>/<session>/session[.vN].jsonl[.zstd]`
 * layout mirror the `dsh-session` filesystem backend, so the same store the
 * harness resumes from is the one ordered here.
 *
 * @returns {Map<string, number>} session id → mtime in epoch milliseconds
 */
function sessionFileMtimes() {
  const mtimes = new Map()
  const walk = (directory, depth) => {
    if (depth > 4) return
    let entries
    try {
      entries = readdirSync(directory, { withFileTypes: true })
    } catch {
      return
    }
    for (const entry of entries) {
      const full = join(directory, entry.name)
      if (entry.isDirectory()) {
        walk(full, depth + 1)
        continue
      }
      if (!entry.isFile()) continue
      if (!entry.name.endsWith('.jsonl') && !entry.name.endsWith('.jsonl.zstd')) continue
      try {
        // A directory holds one generation per format version. Taking the newest
        // rather than the last one `readdir` returned keeps a session that just
        // wrote a v4 log from sorting by its stale v3 mtime.
        const id = basename(directory)
        const stamp = statSync(full).mtimeMs
        const seen = mtimes.get(id)
        if (seen === undefined || stamp > seen) mtimes.set(id, stamp)
      } catch {
        // A file that vanished between readdir and stat is not a session we can
        // order; the createdAt fallback still gives it a stable position.
      }
    }
  }
  for (const root of sessionRoots()) walk(root, 0)
  return mtimes
}

/**
 * Candidate session-log roots, in the order the harness resolves them.
 *
 * `DSH_TUI_SESSION_ROOT` is an explicit override; otherwise the store lives in
 * `$DSH_HOME/sessions`, and a bare dsh install without `DSH_HOME` keeps it in
 * `~/.dsh/sessions` (the legacy base config falls back to `~/.dsh-tui/sessions`).
 * Scanning every candidate costs one failed `readdir` at most.
 *
 * @returns {string[]} absolute root paths, deduplicated
 */
function sessionRoots() {
  const roots = []
  const override = process.env.DSH_TUI_SESSION_ROOT
  if (override !== undefined && override.trim() !== '') roots.push(override)
  const home = process.env.DSH_HOME
  const base = home !== undefined && home.trim() !== '' ? home : join(homedir(), '.dsh')
  roots.push(join(base, 'sessions'))
  roots.push(join(homedir(), '.dsh-tui', 'sessions'))
  return [...new Set(roots)]
}


/** The header time, with the artifact mtime and createdAt as fallbacks. */
function sessionTime(snapshot, mtimes) {
  return timestamp(snapshot?.header?.updatedAt)
    || mtimes.get(snapshot?.header?.id)
    || timestamp(snapshot?.header?.createdAt)
    || 0
}

/** A number or ISO timestamp as epoch milliseconds, or 0. */
function timestamp(value) {
  if (typeof value === 'number' && Number.isFinite(value)) return value
  if (typeof value === 'string' && value !== '') {
    const numeric = Number(value)
    if (Number.isFinite(numeric)) return numeric
    const parsed = Date.parse(value)
    if (Number.isFinite(parsed)) return parsed
  }
  return 0
}
