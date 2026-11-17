/**
 * Behaviour tests for the in-process terminal's state machine.
 *
 * The terminal loop needs a 0.2.x harness *in the process*, so it cannot be
 * driven end to end here. What can be driven is everything above that: the
 * composer behind the command palette, the keys that edit it, the transcript a
 * resume rebuilds, and the order a session switch does its work in. That is
 * exactly the layer the defects were in — the harness is only where the events
 * come from, and the tests feed those in directly.
 *
 * `Screen` is replaced so nothing is written to the test runner's stdout.
 */

import { test } from 'node:test'
import assert from 'node:assert/strict'
import { mkdtempSync, mkdirSync, rmSync, utimesSync, writeFileSync } from 'node:fs'
import { tmpdir } from 'node:os'
import { join } from 'node:path'

import { ChatSession } from '../src/agent.js'
import { TerminalApp } from '../src/app.js'

/** A terminal app with a screen that writes nowhere and a session the test supplies. */
function makeApp(services = {}) {
  const app = new TerminalApp({ get: (name) => services[name] })
  app.screen = { out: null, columns: 80, rows: 24, clear() {}, enter() {}, leave() {} }
  app.render = () => {}
  return app
}

/** The session stand-in the terminal talks to; `calls` is the order it was used in. */
function fakeSession(overrides = {}) {
  const calls = []
  const sent = []
  const steered = []
  const session = {
    calls,
    sent,
    steered,
    ready: true,
    busy: false,
    resumeFailure: undefined,
    cancel() {
      calls.push('cancel')
    },
    async settle() {
      calls.push('settle')
      return true
    },
    async close() {
      calls.push('close')
    },
    async open() {
      calls.push('open')
    },
    async resume(id) {
      calls.push(`resume:${id}`)
      return { resumed: true }
    },
    currentModel: () => null,
    usage: () => ({ used: 0, size: 0 }),
    history: () => [],
    send(text) {
      sent.push(text)
    },
    steer(text) {
      steered.push(text)
    },
    ...overrides,
  }
  return session
}

const text = (value) => ({ type: 'text', text: value })

test('a bracketed paste reaches the composer, one event and CRLF normalised', () => {
  const app = makeApp()
  app.onKeyEvent({ name: 'paste', text: '第一行\r\n第二行' })
  assert.equal(app.composer.text, '第一行\n第二行')
  assert.equal(app.blocks.filter((block) => block.type === 'notice' && block.level === 'error').length, 0)
})

test('typing after a slash keeps the line, and the inline menu follows it', () => {
  const app = makeApp()
  for (const letter of ['/', 'h', 'e']) app.onKeyEvent({ name: 'char', text: letter })
  assert.equal(app.composer.text, '/he')
  assert.equal(app.overlay, null, 'the command list is not a dialog')
  assert.deepEqual(app.palette.items.map((item) => item.label), ['/help'])

  app.onKeyEvent({ name: 'backspace' })
  assert.equal(app.composer.text, '/h')
  app.onKeyEvent({ name: 'char', text: 'x' })
  assert.equal(app.composer.text, '/hx', 'a prefix nothing matches still reaches the line')
  assert.equal(app.palette, null, 'and the menu gets out of the way')
})

test('the inline menu runs the highlighted command and esc closes it without clearing', () => {
  const app = makeApp()
  app.onKeyEvent({ name: 'char', text: '/' })
  app.onKeyEvent({ name: 'enter' })
  assert.equal(app.palette, null, 'enter consumed the menu')
  assert.equal(app.overlay?.kind, 'help', 'the first command (/help) ran')

  app.overlay = null
  app.onKeyEvent({ name: 'char', text: '/' })
  app.onKeyEvent({ name: 'escape' })
  assert.equal(app.palette, null)
  assert.equal(app.composer.text, '/', 'esc closes the list, not the line')
})

test('ctrl+enter is a line break rather than a send', () => {
  const app = makeApp()
  app.session = fakeSession()
  app.composer.setText('first')
  // A raw newline is decoded as enter+ctrl: `\n` is a control byte, not a key
  // name every terminal agrees on.
  app.onKeyEvent({ name: 'enter', ctrl: true })
  assert.equal(app.composer.text, 'first\n')
  assert.equal(app.blocks.filter((block) => block.type === 'user').length, 0)
})

test('a tool card leaves the expansion decision to the global toggle', () => {
  const app = makeApp()
  app.onSessionEvent({ type: 'tool/call', data: { callId: 'c1', name: 'read', arguments: '{"path":"a"}' } })
  const block = app.blocks.find((candidate) => candidate.type === 'tool')
  assert.equal(block.expanded, undefined, 'a card that pinned itself closed would ignore ctrl+o')
  assert.equal(app.expandTools, false)
  app.onKeyEvent({ name: 'ctrl+o' })
  assert.equal(app.expandTools, true)
})

test('a resumed conversation is rebuilt from its session log', () => {
  const app = makeApp()
  app.replayHistory([
    { type: 'user/message', data: { source: { kind: 'user' }, content: [text('what is here')] } },
    { type: 'assistant/message', data: { message: { content: [text('one file')] } } },
    { type: 'tool/call', data: { callId: 'c1', name: 'read', arguments: '{"path":"a.txt"}' } },
    { type: 'tool/result', data: { message: { toolCallId: 'c1', content: [text('file body')], isError: false } } },
  ])
  assert.deepEqual(app.blocks.map((block) => block.type), ['user', 'assistant', 'tool'])
  assert.equal(app.blocks[0].text, 'what is here')
  assert.equal(app.blocks[1].text, 'one file')
  const tool = app.blocks[2]
  assert.equal(tool.output, 'file body')
  assert.equal(tool.status, 'completed')
})

test('/resume reports a refused id instead of claiming it resumed', async () => {
  const app = makeApp()
  app.session = fakeSession({
    async resume(id) {
      app.session.resumeFailure = new Error('no such session')
      return { resumed: false }
    },
  })
  await app.resumeSession('nope')
  const errors = app.blocks.filter((block) => block.type === 'notice' && block.level === 'error')
  assert.equal(errors.length, 1)
  assert.match(errors[0].text, /no such session/u)
  assert.equal(app.blocks.filter((block) => block.type === 'banner').length, 1, 'a fresh banner, not a resumed one')
})

test('/resume puts the resumed transcript on screen', async () => {
  const app = makeApp()
  app.session = fakeSession({
    history: () => [
      { type: 'user/message', data: { source: { kind: 'user' }, content: [text('earlier question')] } },
      { type: 'assistant/message', data: { message: { content: [text('earlier answer')] } } },
    ],
  })
  await app.resumeSession('session-old')
  assert.deepEqual(app.session.calls, ['resume:session-old'])
  assert.deepEqual(app.blocks.filter((block) => block.type === 'user').map((block) => block.text), ['earlier question'])
  assert.deepEqual(app.blocks.filter((block) => block.type === 'assistant').map((block) => block.text), ['earlier answer'])
})

/// A persistence double for the session-index tests. `fails` makes `open` throw.
function sessionPersistence(entries) {
  const opened = []
  return {
    opened,
    async list() {
      return entries.map(({ id, at, eventCount }) => ({
        header: { id, cwd: '/workspace', createdAt: at },
        eventCount,
      }))
    },
    async open(id) {
      opened.push(id)
      const entry = entries.find((candidate) => candidate.id === id)
      if (entry?.fails === true) throw new Error('unreadable')
      return {
        async read(offset = 0, length = 100) {
          return { events: (entry?.events ?? []).slice(offset, offset + length) }
        },
        async close() {},
      }
    },
  }
}

test('stored sessions without a human prompt are not listed', async () => {
  const persistence = sessionPersistence([
    { id: 'session-empty', at: 300, eventCount: 0, events: [] },
    { id: 'session-tool-only', at: 200, eventCount: 1, events: [
      { type: 'tool/call', data: { callId: 'c1', name: 'read', arguments: '{}' } },
    ] },
    { id: 'session-prompt', at: 100, eventCount: 1, events: [
      { type: 'user/message', data: { source: { kind: 'user' }, content: [text('Fix the build, then run the tests')] } },
    ] },
  ])
  const session = new ChatSession({ get: (name) => (name === 'sessionPersistence' ? persistence : undefined) }, '/workspace')
  const rows = await session.listSessions()
  assert.deepEqual(rows.map((row) => row.id), ['session-prompt'])
  assert.equal(rows[0].title, 'Fix the build, then run the tests')
  assert.equal(rows[0].unreadable, false)
  assert.equal(rows[0].at, 100)
  assert.deepEqual(
    persistence.opened.sort(),
    ['session-empty', 'session-prompt', 'session-tool-only'],
    'every artifact is read; emptiness is decided by content, not eventCount',
  )
})

test('a first prompt tolerates missing sources, splices and string content', async () => {
  const persistence = sessionPersistence([
    { id: 'session-old-source', at: 3, events: [
      // Old logs predate `source`; they are still human input.
      { type: 'user/message', data: { content: [text('old log without source')] } },
    ] },
    { id: 'session-string', at: 2, events: [
      { type: 'user/message', data: { source: { kind: 'user' }, content: 'string prompt' } },
    ] },
    { id: 'session-splice', at: 1, events: [
      // A tool notice is not a prompt; the spliced human message after it is.
      { type: 'agent/inbox/spliced', data: { inserted: [
        { content: [text('job notice')], source: { kind: 'tool-jobs' } },
      ] } },
      { type: 'agent/inbox/spliced', data: { inserted: [
        { content: [text('spliced prompt')], source: { kind: 'user' } },
      ] } },
    ] },
  ])
  const session = new ChatSession({ get: (name) => (name === 'sessionPersistence' ? persistence : undefined) }, '/workspace')
  const rows = await session.listSessions()
  assert.deepEqual(rows.map((row) => row.id), ['session-old-source', 'session-string', 'session-splice'])
  assert.deepEqual(rows.map((row) => row.title), ['old log without source', 'string prompt', 'spliced prompt'])
})

test('a session whose log cannot be read stays visible', async () => {
  const persistence = sessionPersistence([
    { id: 'session-broken', at: 10, fails: true },
  ])
  const session = new ChatSession({ get: (name) => (name === 'sessionPersistence' ? persistence : undefined) }, '/workspace')
  const rows = await session.listSessions()
  assert.equal(rows.length, 1, 'an unreadable real session is not deleted')
  assert.equal(rows[0].id, 'session-broken')
  assert.equal(rows[0].title, '')
  assert.equal(rows[0].unreadable, true)

  const app = makeApp({ sessionPersistence: persistence })
  app.session = session
  await app.openResumePicker()
  assert.equal(app.overlay.items.length, 1)
  // The interface language follows the locale, so the label is asserted in
  // either table rather than in the one this machine happens to run.
  assert.match(app.overlay.items[0].label, /title unavailable|标题未读出/u)
})

test('sessions are ordered by artifact mtime, not creation time', async () => {
  const home = mkdtempSync(join(tmpdir(), 'dsh-session-mtime-'))
  const previous = process.env.DSH_HOME
  try {
    process.env.DSH_HOME = home
    const group = join(home, 'sessions', '--workspace--')
    for (const [id, mtime] of [['session-old', 5_000], ['session-new', 1_000]]) {
      const directory = join(group, id)
      mkdirSync(directory, { recursive: true })
      const file = join(directory, 'session.v4.jsonl.zstd')
      writeFileSync(file, '')
      utimesSync(file, new Date(mtime), new Date(mtime))
    }
    const persistence = sessionPersistence([
      { id: 'session-old', at: 100, events: [
        { type: 'user/message', data: { source: { kind: 'user' }, content: [text('old but active')] } },
      ] },
      { id: 'session-new', at: 200, events: [
        { type: 'user/message', data: { source: { kind: 'user' }, content: [text('new but idle')] } },
      ] },
    ])
    const session = new ChatSession({ get: (name) => (name === 'sessionPersistence' ? persistence : undefined) }, '/workspace')
    const rows = await session.listSessions()
    assert.deepEqual(rows.map((row) => row.id), ['session-old', 'session-new'],
      'the session written most recently sorts first even though it was created first')
  } finally {
    if (previous === undefined) delete process.env.DSH_HOME
    else process.env.DSH_HOME = previous
    rmSync(home, { recursive: true, force: true })
  }
})

test('the newest log generation decides the order, not the listing order', async () => {
  const home = mkdtempSync(join(tmpdir(), 'dsh-session-generation-'))
  const previous = process.env.DSH_HOME
  try {
    process.env.DSH_HOME = home
    // `session.v10` sorts before `session.v2` in a directory listing, so a
    // walk that keeps the last entry would order by the stale generation.
    const rewritten = join(home, 'sessions', '--workspace--', 'session-rewritten')
    const created = join(home, 'sessions', '--workspace--', 'session-created')
    mkdirSync(rewritten, { recursive: true })
    mkdirSync(created, { recursive: true })
    for (const [name, mtime] of [['session.v2.jsonl', 1_000], ['session.v10.jsonl', 9_000]]) {
      const file = join(rewritten, name)
      writeFileSync(file, '')
      utimesSync(file, new Date(mtime), new Date(mtime))
    }
    const otherFile = join(created, 'session.jsonl')
    writeFileSync(otherFile, '')
    utimesSync(otherFile, new Date(5_000), new Date(5_000))
    const persistence = sessionPersistence([
      { id: 'session-rewritten', at: 100, events: [
        { type: 'user/message', data: { source: { kind: 'user' }, content: [text('written again today')] } },
      ] },
      { id: 'session-created', at: 200, events: [
        { type: 'user/message', data: { source: { kind: 'user' }, content: [text('created later')] } },
      ] },
    ])
    const session = new ChatSession({ get: (name) => (name === 'sessionPersistence' ? persistence : undefined) }, '/workspace')
    const rows = await session.listSessions()
    assert.deepEqual(rows.map((row) => row.id), ['session-rewritten', 'session-created'],
      'a session whose newest generation was just written sorts above an older one')
  } finally {
    if (previous === undefined) delete process.env.DSH_HOME
    else process.env.DSH_HOME = previous
    rmSync(home, { recursive: true, force: true })
  }
})

test('/sessions resolves each first prompt once and remembers it', async () => {
  const persistence = sessionPersistence([
    { id: 'session-cached', at: 10, events: [
      { type: 'user/message', data: { source: { kind: 'user' }, content: [text('only read once')] } },
    ] },
  ])
  const session = new ChatSession({ get: (name) => (name === 'sessionPersistence' ? persistence : undefined) }, '/workspace')
  const first = await session.listSessions()
  const second = await session.listSessions()
  assert.deepEqual(second, first, 'the second listing answers the same way')
  assert.deepEqual(persistence.opened, ['session-cached'],
    'a resolved title is kept, so reopening the picker does not read the log again')
})

test('/sessions labels a row with its first prompt instead of the id', async () => {
  const persistence = sessionPersistence([
    { id: 'session-123456', at: 100, events: [
      { type: 'user/message', data: { source: { kind: 'user' }, content: [text('给 /sessions 加首条提示词')] } },
    ] },
  ])
  const app = makeApp({ sessionPersistence: persistence })
  app.session = new ChatSession({ get: (name) => (name === 'sessionPersistence' ? persistence : undefined) }, '/workspace')
  await app.openResumePicker()
  assert.equal(app.overlay.kind, 'picker')
  assert.equal(app.overlay.items.length, 1)
  assert.match(app.overlay.items[0].label, /给 \/sessions 加首条提示词/u)
  assert.match(app.overlay.items[0].tail, /123456/u, 'the id stays as secondary metadata')
})

test('/new ends the turn in flight before it swaps the agent', async () => {
  const app = makeApp()
  app.session = fakeSession({ busy: true })
  app.status = 'working'
  app.queue = ['a queued prompt']
  await app.startNew()
  assert.deepEqual(app.session.calls, ['cancel', 'settle', 'close', 'open'])
  assert.deepEqual(app.queue, [], 'the queue belonged to the session that was left')
  assert.equal(app.status, 'ready')
})

/** Presses one key on the open dialog. */
function press(app, name, text) {
  app.onKeyEvent(text === undefined ? { name } : { name, text })
}

/**
 * Starts one approval and returns its promise plus the dialog item.
 *
 * `tool/call` arrives before the ask, which is where the arguments come from —
 * the request itself carries only the tool name and the call id.
 */
function ask(app, { callId = 'c1', name = 'bash', command = 'npm test', reason = 'needs permission' } = {}) {
  app.onSessionEvent({ type: 'tool/call', data: { callId, name, arguments: JSON.stringify({ command }) } })
  const promise = app.requestApproval({ toolName: name, callId, reason })
  return promise
}

test('an approval asks with four numbered choices and answers by number', async () => {
  const app = makeApp()
  app.approvalMode = 'ask'
  const decided = ask(app)
  assert.deepEqual(app.overlay.options.map((option) => option.value),
    ['allowed-once', 'session-allow', 'rejected', 'reject-reason'])

  press(app, 'char', '1')
  assert.equal(await decided, 'allowed-once')
  assert.equal(app.overlay, null)
})

test('number 2 runs this command for the rest of the session', async () => {
  const app = makeApp()
  app.approvalMode = 'ask'
  const first = ask(app)
  press(app, 'char', '2')
  assert.equal(await first, 'allowed-once')
  assert.equal(app.approvedTools.has('bash npm test'), true)

  // The same command again does not draw a dialog at all.
  const second = ask(app, { callId: 'c2' })
  assert.equal(await second, 'allowed-once')
  assert.equal(app.overlay, null)
})

test('number 4 settles as rejected and tells the model why', async () => {
  const app = makeApp()
  app.approvalMode = 'ask'
  app.session = fakeSession()
  const decided = ask(app)

  press(app, 'char', '4')
  assert.ok(app.overlay.reason !== undefined, 'the reason line opens inside the dialog')
  for (const letter of ['别', '删']) press(app, 'char', letter)
  press(app, 'enter')

  assert.equal(await decided, 'rejected')
  assert.deepEqual(app.session.sent, [
    app.i18n.t('permission.reason.message', { tool: 'bash', reason: '别删' }),
  ])
  assert.match(app.session.sent[0], /别删/u, 'the reason itself is in the message')
  assert.equal(app.blocks.filter((block) => block.type === 'user').length, 1, 'the reason is visible too')
})

test('escape goes back to the choices without rejecting', async () => {
  const app = makeApp()
  app.approvalMode = 'ask'
  app.session = fakeSession()
  const decided = ask(app)

  press(app, 'char', '4')
  press(app, 'escape')
  assert.equal(app.overlay.reason, undefined)
  assert.equal(app.session.sent.length, 0)

  press(app, 'char', '3')
  assert.equal(await decided, 'rejected')
})

test('critical mode runs a safe command and still asks about a dangerous one', async () => {
  const app = makeApp()
  assert.equal(app.approvalMode, 'ask', 'the Kimi-style default is Always Ask')
  app.approvalMode = 'critical'

  assert.equal(await ask(app), 'allowed-once')
  assert.equal(app.overlay, null, 'a safe command does not interrupt')

  const dangerous = ask(app, { callId: 'c2', command: 'rm -rf /' })
  assert.notEqual(app.overlay, null, 'a dangerous command still asks')
  press(app, 'char', '3')
  assert.equal(await dangerous, 'rejected')
})

test('full mode runs everything except a plan decision', async () => {
  const app = makeApp()
  app.approvalMode = 'full'
  assert.equal(await ask(app, { command: 'sudo rm -rf /' }), 'allowed-once')

  const plan = ask(app, { callId: 'c2', name: 'present_plan', command: '' })
  assert.notEqual(app.overlay, null, 'accepting a plan is still a decision')
  press(app, 'char', '3')
  assert.equal(await plan, 'rejected')
})

test('a new session forgets the session allow-list', async () => {
  const app = makeApp()
  app.approvalMode = 'ask'
  app.session = fakeSession()
  const decided = ask(app)
  press(app, 'char', '2')
  await decided
  assert.equal(app.approvedTools.size, 1)

  app.approvalMode = 'full'
  await app.startNew()
  assert.equal(app.approvedTools.size, 0)
  assert.equal(app.autoApproved, 0)
  assert.equal(app.approvalMode, 'ask', 'the next session starts at Always Ask, even after full')
})

test('a session grant does not swallow a dangerous command', async () => {
  const app = makeApp()
  app.approvalMode = 'ask'
  app.session = fakeSession()
  const first = ask(app, { command: 'rm -rf build' })
  press(app, 'char', '2')
  assert.equal(await first, 'allowed-once')
  assert.equal(app.approvedTools.has('bash rm -rf build'), true)

  const dangerous = ask(app, { callId: 'c2', command: 'rm -rf /' })
  assert.notEqual(app.overlay, null, 'a dangerous command still draws the dialog')
  press(app, 'char', '3')
  assert.equal(await dangerous, 'rejected')
})

test('ctrl+s steers instead of queueing while a turn is running', () => {
  const app = makeApp()
  app.session = fakeSession()
  app.status = 'working'
  app.composer.setText('use the other fixture')
  app.onKeyEvent({ name: 'ctrl+s' })
  assert.deepEqual(app.session.steered, ['use the other fixture'])
  assert.deepEqual(app.session.sent, [])
  assert.deepEqual(app.queue, [])
  assert.equal(app.composer.text, '')
})

test('! runs a workspace shell command and renders its output', async () => {
  const app = makeApp()
  app.runShellCommand('printf hello')
  const block = app.blocks.find((candidate) => candidate.type === 'tool' && candidate.name === 'shell')
  assert.ok(block, 'the command gets a card')
  for (let attempt = 0; attempt < 100 && block.status === 'in_progress'; attempt += 1) {
    await new Promise((resolve) => setTimeout(resolve, 10))
  }
  assert.equal(block.status, 'completed')
  assert.match(block.output, /hello/u)
})

test('/plan and shift+tab toggle the harness plan mode', async () => {
  const lines = []
  const app = makeApp({
    commands: {
      execute: async (_agent, line) => {
        lines.push(line)
        return { result: { text: 'plan mode changed' } }
      },
    },
  })
  app.session = fakeSession({ live: {} })
  await app.togglePlan('on')
  assert.equal(app.planMode, true)
  // Bare `/plan`: the harness steers any argument into the conversation, so
  // `/plan on` would send the word "on" to the model.
  assert.deepEqual(lines, ['/plan'])
  await app.togglePlan('')
  assert.equal(app.planMode, false)
  assert.deepEqual(lines, ['/plan', '/plan off'])
})

test('permanent rules deny and allow without a dialog', async () => {
  const dir = mkdtempSync(join(tmpdir(), 'hdsl-rules-'))
  const file = join(dir, 'permissions.json')
  const previous = process.env.HDSL_TUI_RULES
  process.env.HDSL_TUI_RULES = file
  try {
    const app = makeApp()
    app.session = fakeSession()
    app.runRuleCommand('deny', 'bash rm -rf /')
    const denied = ask(app, { command: 'rm -rf /' })
    assert.equal(await denied, 'rejected')
    assert.equal(app.overlay, null, 'a deny rule settles before the dialog')

    app.runRuleCommand('allow', 'bash npm test')
    const allowed = ask(app, { callId: 'c2', command: 'npm test' })
    assert.equal(await allowed, 'allowed-once')
    assert.equal(app.overlay, null, 'an allow rule settles before the dialog')
  } finally {
    if (previous === undefined) delete process.env.HDSL_TUI_RULES
    else process.env.HDSL_TUI_RULES = previous
    rmSync(dir, { recursive: true, force: true })
  }
})

test('the banner shows the live session id', async () => {
  const app = makeApp()
  app.session = fakeSession({ sessionId: 'abcdef123456' })
  app.pushBanner()
  app.refreshBanner()
  const banner = app.blocks.find((block) => block.type === 'banner')
  assert.equal(banner.sessionId, 'abcdef123456')
})

const tick = () => new Promise((resolve) => setTimeout(resolve, 0))

test('a harness command is executed by the harness, not re-implemented here', async () => {
  const executed = []
  const app = makeApp({
    commands: {
      list: () => [{ name: 'goal', description: 'Set or view the goal' }],
      execute: async (agent, line) => {
        executed.push(line)
        return { result: { kind: 'success', text: 'goal set' } }
      },
    },
  })
  app.session = fakeSession({ live: {} })
  app.runCommand('goal', 'ship it')
  await tick()
  assert.deepEqual(executed, ['/goal ship it'])
  assert.equal(app.blocks.filter((block) => block.type === 'notice' && block.text === 'goal set').length, 1)
})

test('the palette and the help list the harness commands too', async () => {
  const app = makeApp({
    commands: { list: () => [{ name: 'permission', description: 'Switch the permission preset' }], execute: async () => undefined },
  })
  app.session = fakeSession({ live: {} })
  app.onKeyEvent({ name: 'char', text: '/' })
  const labels = app.palette.items.map((item) => item.label)
  assert.ok(labels.includes('/goal') === false)
  assert.ok(labels.includes('/permission'), labels.join(','))
  assert.ok(labels.includes('/yolo'), 'the local commands stay')
})

test('a name nobody knows is refused only after the harness was asked', async () => {
  const asked = []
  const app = makeApp({
    commands: {
      list: () => [],
      execute: async (agent, line) => {
        asked.push(line)
        return undefined
      },
    },
  })
  app.session = fakeSession({ live: {} })
  app.runCommand('nope', '')
  await tick()
  assert.deepEqual(asked, ['/nope'])
  const errors = app.blocks.filter((block) => block.type === 'notice' && block.level === 'error')
  assert.equal(errors.length, 1)
  assert.match(errors[0].text, /nope/u)

  // Without the service at all the answer is the same, and it is immediate.
  const bare = makeApp({})
  bare.session = fakeSession({ live: {} })
  bare.runCommand('goal', '')
  await tick()
  assert.equal(bare.blocks.filter((block) => block.type === 'notice' && block.level === 'error').length, 1)
})

test('/yolo opens a list of the three modes and takes an explicit one', () => {
  const app = makeApp()
  assert.equal(app.approvalMode, 'ask', 'the default is Always Ask')

  // No argument: a picker, not a cycle. The current level is marked.
  app.runCommand('yolo')
  assert.equal(app.overlay.kind, 'picker')
  assert.deepEqual(app.overlay.items.map((item) => item.tail !== ''), [true, false, false])
  app.overlay.items[2].onSelect()
  assert.equal(app.approvalMode, 'full')

  app.runCommand('yolo', 'critical')
  assert.equal(app.approvalMode, 'critical')

  app.runCommand('yolo', 'nonsense')
  assert.equal(app.approvalMode, 'critical', 'an unknown mode changes nothing')
  const errors = app.blocks.filter((block) => block.type === 'notice' && block.level === 'error')
  assert.match(errors.at(-1).text, /nonsense/u)
})

test('/permission opens the harness preset list and applies one', () => {
  const applied = []
  const app = makeApp({
    permissionPresets: {
      catalog: () => ({
        options: [
          { value: 'read-only', name: '只读' },
          { value: 'workspace-write', name: '工作区可写' },
          { value: 'danger-full-access', name: '完全放开' },
        ],
      }),
      current: () => 'workspace-write',
      set: (_session, name) => applied.push(name),
    },
  })
  app.session = fakeSession({ live: { session: { id: 's' } } })
  app.runCommand('permission')
  assert.equal(app.overlay.kind, 'picker')
  assert.equal(app.overlay.items.length, 3)
  assert.equal(app.overlay.index, 1, 'the current preset is highlighted')
  app.overlay.items[2].onSelect()
  assert.deepEqual(applied, ['danger-full-access'])
})

test('/yolo status reports the mode and what it has allowed', async () => {
  const app = makeApp()
  app.approvalMode = 'critical'
  await ask(app)
  app.runCommand('yolo', 'status')
  const text = app.blocks.filter((block) => block.type === 'notice').at(-1).text
  assert.match(text, /critical|关键确认/u)
  assert.match(text, /1/u, 'one request ran without asking')
})

test('/status shows the facts a person checks', () => {
  const app = makeApp()
  app.approvalMode = 'critical'
  app.session = fakeSession({ sessionId: 'abcdef123456' })
  app.runCommand('status')
  assert.equal(app.overlay.kind, 'help')
  const rows = app.overlay.sections[0].rows.map((row) => row[1]).join('\n')
  assert.match(rows, /abcdef123456/u)
  assert.match(rows, /critical|关键确认/u)
})

test('Tab completes a harness command as readily as a local one', () => {
  const app = makeApp({
    commands: { list: () => [{ name: 'goal', description: 'Set the goal' }], execute: async () => undefined },
  })
  app.session = fakeSession({ live: {} })
  app.composer.setText('/go')
  app.composer.caret = 3
  app.onKeyEvent({ name: 'tab' })
  assert.equal(app.composer.text, '/goal')
})

test('/effort completes its values and Tab cycles through them', () => {
  const app = makeApp()
  app.composer.setText('/effort ')
  app.composer.caret = 8
  app.onKeyEvent({ name: 'tab' })
  assert.equal(app.composer.text, '/effort off')
  app.onKeyEvent({ name: 'tab' })
  assert.equal(app.composer.text, '/effort low', 'the next Tab moves to the next value')
  app.onKeyEvent({ name: 'tab' })
  assert.equal(app.composer.text, '/effort high')
})

test('@ completes workspace paths', () => {
  const directory = mkdtempSync(join(tmpdir(), 'dsh-complete-'))
  try {
    mkdirSync(join(directory, 'src'))
    writeFileSync(join(directory, 'src', 'index.js'), '')
    writeFileSync(join(directory, 'src', 'main.js'), '')
    writeFileSync(join(directory, 'README.md'), '')
    const app = makeApp()
    app.cwd = directory

    app.composer.setText('/model @src/')
    app.composer.caret = app.composer.text.length
    app.onKeyEvent({ name: 'tab' })
    assert.match(app.composer.text, /^\/model @src\/(index\.js|main\.js)$/u)

    app.composer.setText('/model @RE')
    app.composer.caret = app.composer.text.length
    app.onKeyEvent({ name: 'tab' })
    assert.equal(app.composer.text, '/model @README.md')

    assert.ok(app.completePaths('inde').includes('src/index.js'), 'a bare fragment searches the workspace')
    assert.deepEqual(app.completePaths('../'), [], 'completion never leaves the workspace')
  } finally {
    rmSync(directory, { recursive: true, force: true })
  }
})

/** The services `/provider` writes through, recording what it asked for. */
function providerServices({ routes = {} } = {}) {
  const writes = []
  return {
    writes,
    settings: {
      describe: () => [{ ns: 'llm-pi-ai', value: { providers: routes }, revision: 7 }],
      mutate: (ns, ops, revision) => {
        writes.push({ ns, ops, revision })
        return Promise.resolve()
      },
    },
    credentials: {
      describe: async (name) => ({ present: name === 'ACME_API_KEY' }),
      set: async (name, value) => {
        writes.push({ key: name, value })
      },
    },
    llm: {
      listProviders: () => [{ provider: 'acme', displayName: 'Acme' }],
      listModels: async () => [{ id: 'acme-1' }, { id: 'acme-2' }],
    },
  }
}

test('/provider add writes nothing until --yes, then the path-addressed section', async () => {
  const services = providerServices()
  const app = makeApp(services)

  await app.runProvider('add acme openai-completions https://api.example.test/v1 --model acme-1,acme-2')
  assert.equal(services.writes.length, 0, 'the exact block is shown first')
  const confirm = app.blocks.filter((block) => block.type === 'notice').at(-1).text
  assert.match(confirm, /acme/u)
  assert.match(confirm, /--yes/u)

  await app.runProvider('add acme openai-completions https://api.example.test/v1 --model acme-1 --yes')
  assert.equal(services.writes.length, 1)
  const write = services.writes[0]
  assert.equal(write.ns, 'llm-pi-ai')
  assert.equal(write.revision, 7, 'the revision guards a concurrent edit')
  assert.deepEqual(write.ops[0].op, 'set')
  assert.deepEqual(write.ops[0].path, ['providers', 'acme'])
  assert.equal(write.ops[0].value.baseURL, 'https://api.example.test/v1')
  assert.equal(write.ops[0].value.api, 'openai-completions')
  assert.equal(write.ops[0].value.apiKeyEnv, 'ACME_API_KEY', 'the default variable name')
})

test('/provider add --key stores the key through the credentials service, never in the settings write', async () => {
  const services = providerServices()
  const app = makeApp(services)
  await app.runProvider('add relay openai-completions https://relay.example.test/v1 --key sk-secret --yes')
  const settingsWrite = services.writes.find((write) => write.ns === 'llm-pi-ai')
  assert.equal(JSON.stringify(settingsWrite).includes('sk-secret'), false,
    'the key is not part of the profile')
  const keyWrite = services.writes.find((write) => write.key !== undefined)
  assert.deepEqual(keyWrite, { key: 'RELAY_API_KEY', value: 'sk-secret' })
  assert.equal(JSON.stringify(app.blocks).includes('sk-secret'), false, 'and it is never printed')
})

test('/provider remove needs --yes and unsets the route', async () => {
  const services = providerServices()
  const app = makeApp(services)
  await app.runProvider('remove acme')
  assert.equal(services.writes.length, 0)
  await app.runProvider('remove acme --yes')
  assert.deepEqual(services.writes[0].ops, [{ op: 'unset', path: ['providers', 'acme'] }])
})

test('/provider list shows the route and whether its key resolves', async () => {
  const services = providerServices({
    routes: { acme: { api: 'openai-completions', baseURL: 'https://api.example.test/v1', apiKeyEnv: 'ACME_API_KEY' } },
  })
  const app = makeApp(services)
  await app.runProvider('list')
  const text = app.blocks.filter((block) => block.type === 'notice').at(-1).text
  assert.match(text, /acme/u)
  assert.match(text, /key set|已配置/u)
  assert.equal(text.includes('ACME_API_KEY'), true, 'the variable name is shown')
})

test('/provider says so when the profile has no llm-pi-ai section', async () => {
  const services = providerServices()
  services.settings.describe = () => []
  const app = makeApp(services)
  await app.runProvider('add acme openai-completions https://x.test/v1 --yes')
  const notice = app.blocks.filter((block) => block.type === 'notice').at(-1)
  assert.equal(notice.level, 'error')
  assert.match(notice.text, /llm-pi-ai/u)
})

test('/provider test asks the provider for its models', async () => {
  const services = providerServices()
  const app = makeApp(services)
  await app.runProvider('test acme')
  const text = app.blocks.filter((block) => block.type === 'notice').at(-1).text
  assert.match(text, /acme/u)
  assert.match(text, /2/u)
})

test('/provider without the services says which version has them', async () => {
  const app = makeApp({})
  await app.runProvider('list')
  const notice = app.blocks.filter((block) => block.type === 'notice').at(-1)
  assert.equal(notice.level, 'error')
  assert.match(notice.text, /0\.2|settings|挂载|mount/u)
})

test('/resume says why there is nothing to resume when persistence is off', async () => {
  const app = makeApp({})
  await app.openResumePicker()
  const notice = app.blocks.filter((block) => block.type === 'notice').at(-1)
  assert.match(notice.text, /persistence|持久化/u)
  assert.equal(app.overlay, null, 'no empty picker')
})

test('a quiet session is not cancelled on the way to a new one', async () => {
  const app = makeApp()
  app.session = fakeSession()
  app.status = 'ready'
  await app.startNew()
  assert.deepEqual(app.session.calls, ['close', 'open'], 'nothing to cancel means nothing to wait for')
})

test('/model writes what the next step assembles with, not just the log', () => {
  const session = new ChatSession({ get: () => undefined }, '/workspace')
  const appended = []
  session.agent = {
    session: {
      id: 'session-x',
      append(type, data) {
        appended.push([type, data])
      },
      snapshotEvents: () => [
        { type: 'model/selection', data: { provider: 'acme', model: 'one', reasoningEffort: 'high' } },
      ],
    },
  }
  session.modelState = { current: { provider: 'acme', model: 'one', reasoningEffort: 'high' }, assembled: undefined }

  session.select({ model: 'two' })

  const expected = { provider: 'acme', model: 'two', reasoningEffort: 'high' }
  assert.deepEqual(session.modelState.current, expected, 'the harness reads this on its next step')
  assert.deepEqual(appended, [['model/selection', expected]], 'and the log still says the same thing')
})
