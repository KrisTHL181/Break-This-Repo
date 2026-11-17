/**
 * Application-state tests.
 *
 * The app is driven by its own event methods rather than by a terminal: paste
 * goes through `onKeyEvent`, permissions through `onRequest`, child death through
 * `onExit`, and `/new` through `startNewSession` with a stub client. One test
 * spawns a real (exiting) child to prove the startup-failure route releases the
 * terminal.
 */

import { test } from 'node:test'
import assert from 'node:assert/strict'
import { mkdtempSync, rmSync, writeFileSync } from 'node:fs'
import { tmpdir } from 'node:os'
import { join } from 'node:path'
import { PassThrough, Writable } from 'node:stream'

import { App } from '../src/ui/app.js'

/** Rejects if `promise` has not settled within `ms`; a hung regression must fail, not hang. */
function deadline(promise, ms = 1500, label = 'timed out') {
  let timer = null
  const guard = new Promise((_resolve, reject) => {
    timer = setTimeout(() => reject(new Error(label)), ms)
  })
  return Promise.race([promise, guard]).finally(() => clearTimeout(timer))
}

/** A terminal-width stdout that keeps what was written. */
function outputSink() {
  const chunks = []
  const out = new Writable({
    write(chunk, _encoding, callback) {
      chunks.push(chunk.toString())
      callback()
    },
  })
  out.columns = 80
  out.rows = 24
  out.text = () => chunks.join('')
  return out
}

/** A stdin stand-in that records raw-mode changes instead of touching a TTY. */
function inputSource() {
  const stdin = new PassThrough()
  stdin.isTTY = true
  stdin.rawModes = []
  stdin.setRawMode = (value) => {
    stdin.rawModes.push(value)
  }
  return stdin
}

/** An `App` that has not been started; the test decides what `client` and `harness` are. */
function makeApp(extra = {}) {
  const stdout = outputSink()
  const stdin = inputSource()
  const app = new App({
    env: { HSDL_TUI_LANG: 'en' },
    stdout,
    stdin,
    cwd: process.cwd(),
    harness: { command: ['dsh'], source: 'test', profile: 'acp' },
    resume: null,
    ...extra,
  })
  return { app, stdout, stdin }
}

test('paste inserts at the caret and normalises CRLF', () => {
  const { app } = makeApp()
  app.composer.setText('hello world')
  app.composer.caret = 5
  app.onKeyEvent({ name: 'paste', text: 'a\r\nb\rc' })
  assert.equal(app.composer.text, 'helloa\nb\nc world')
  assert.equal(app.composer.caret, 'helloa\nb\nc'.length)
  assert.equal(app.dirty, true)
  assert.equal(app.scroll.follow, true)
})

test('a paste while a dialog is open leaves the composer untouched', () => {
  const { app } = makeApp()
  app.composer.setText('/he')
  const overlay = { kind: 'picker', items: [{ label: '/help' }], index: 0 }
  app.overlay = overlay
  app.onKeyEvent({ name: 'paste', text: 'lp\r\nme' })
  assert.equal(app.composer.text, '/he')
  assert.equal(app.composer.caret, 3)
  assert.equal(app.overlay, overlay)
})

test('the inline command menu follows the text instead of swallowing it', () => {
  const { app } = makeApp()
  app.onKeyEvent({ name: 'char', text: '/' })
  assert.equal(app.overlay, null, 'the command list is not a dialog')
  app.onKeyEvent({ name: 'char', text: 'h' })
  app.onKeyEvent({ name: 'char', text: 'e' })
  assert.equal(app.composer.text, '/he', 'the letters typed after / reach the composer')
  assert.deepEqual(app.palette.items.map((item) => item.label), ['/help'])

  // The menu still navigates: the arrows move the selection, they do not move
  // the caret.
  app.palette.items = [{ name: 'help', label: '/help' }, { name: 'hello', label: '/hello' }]
  app.onKeyEvent({ name: 'down' })
  assert.equal(app.palette.index, 1)
  assert.equal(app.composer.caret, 3)

  app.onKeyEvent({ name: 'backspace' })
  assert.equal(app.composer.text, '/h')

  // A paste is text too, and a space ends the command prefix.
  app.onKeyEvent({ name: 'paste', text: 'elp ' })
  assert.equal(app.composer.text, '/help ')
  assert.equal(app.palette, null, 'the menu closes once the line is no longer a command')

  // Nothing matches `/hx`, so the menu closes and typing simply continues.
  app.composer.setText('/hx')
  app.onKeyEvent({ name: 'char', text: 'y' })
  assert.equal(app.palette, null)
  assert.equal(app.composer.text, '/hxy')
})

test('ctrl+enter is a line break, not a send', () => {
  const { app } = makeApp()
  app.composer.setText('first')
  // A raw newline arrives as enter+ctrl: `\n` is a control byte, not a key name
  // every terminal agrees on.
  app.onKeyEvent({ name: 'enter', ctrl: true })
  assert.equal(app.composer.text, 'first\n')
  assert.equal(app.blocks.filter((block) => block.type === 'user').length, 0)
  app.onKeyEvent({ name: 'enter', shift: true })
  assert.equal(app.composer.text, 'first\n\n')
})

test('an update from the session that was replaced is ignored', () => {
  const { app } = makeApp()
  app.sessionId = 'session-new'
  app.status = 'ready'
  const chunk = (sessionId, text) => ({
    method: 'session/update',
    params: { sessionId, update: { sessionUpdate: 'agent_message_chunk', content: { type: 'text', text } } },
  })
  app.onNotification(chunk('session-old', 'late answer'))
  assert.equal(app.blocks.filter((block) => block.type === 'assistant').length, 0)
  app.onNotification(chunk('session-new', 'current answer'))
  assert.equal(app.blocks.filter((block) => block.type === 'assistant').length, 1)
})

test('/new cancels the running turn before it swaps the session', async () => {
  const { app } = makeApp()
  const calls = []
  app.client = {
    sessions: { close: true },
    request(method) {
      calls.push(method)
      if (method === 'session/new') return Promise.resolve({ sessionId: 'session-next', configOptions: [] })
      return Promise.resolve({})
    },
    notify(method) {
      calls.push(method)
    },
    respond() {},
  }
  app.sessionId = 'session-live'
  app.status = 'working'
  app.queue = ['a queued prompt']
  await app.startNewSession()
  assert.deepEqual(calls.slice(0, 3), ['session/cancel', 'session/close', 'session/new'])
  assert.deepEqual(app.queue, [], 'the queue belonged to the session that was left')
  assert.equal(app.sessionId, 'session-next')
})

test('/new waits for the turn in flight instead of leaving it running', async () => {
  const { app } = makeApp()
  let answer = null
  const calls = []
  app.client = {
    sessions: { close: true },
    request(method) {
      calls.push(method)
      if (method === 'session/prompt') {
        return new Promise((resolve) => {
          answer = resolve
        })
      }
      if (method === 'session/new') return Promise.resolve({ sessionId: 'session-next', configOptions: [] })
      return Promise.resolve({})
    },
    notify(method) {
      calls.push(method)
    },
    respond() {},
  }
  app.sessionId = 'session-live'
  const turn = app.runPrompt('slow question')
  assert.equal(app.status, 'working')
  const switching = app.startNewSession()
  // Give the switch every chance to race ahead: it must be parked on the turn.
  await new Promise((resolve) => setTimeout(resolve, 20))
  assert.equal(calls.includes('session/cancel'), true, 'the turn was asked to stop')
  assert.equal(calls.includes('session/new'), false, 'the new session waits for the old turn to answer')

  answer({ stopReason: 'cancelled' })
  await turn
  await switching
  assert.equal(app.sessionId, 'session-next')
  assert.equal(app.status, 'ready')
  assert.deepEqual(calls.slice(0, 3), ['session/prompt', 'session/cancel', 'session/close'])
})

test('a turn that outlives its session cannot touch the next one', async () => {
  const { app } = makeApp({ turnSettleMs: 20 })
  let answer = null
  app.client = {
    sessions: { close: true },
    request(method) {
      if (method === 'session/prompt') {
        return new Promise((resolve) => {
          answer = resolve
        })
      }
      if (method === 'session/new') return Promise.resolve({ sessionId: 'session-next', configOptions: [] })
      return Promise.resolve({})
    },
    notify() {},
    respond() {},
  }
  app.sessionId = 'session-live'
  const turn = app.runPrompt('slow question')
  // The switch gives up waiting after `turnSettleMs` and moves on.
  await app.startNewSession()
  assert.equal(app.sessionId, 'session-next')

  // The new session starts its own turn, and is mid-answer when the old one
  // finally clears — the shape of the defect: the late reply used to finish the
  // streaming block, set the status and drain the queue of a conversation it
  // does not belong to.
  const streaming = app.makeBlock({ type: 'assistant', text: '', full: 'new answer', streaming: true })
  app.status = 'working'
  answer({ stopReason: 'end_turn' })
  await turn
  assert.equal(app.status, 'working', 'the late turn did not mark the new session idle')
  assert.equal(streaming.streaming, true, 'the late turn did not finish the new session\'s block')
  assert.equal(app.blocks.filter((block) => block.type === 'notice' && block.level === 'error').length, 0)
})

test('adoptSession tolerates an empty transcript', () => {
  const { app } = makeApp()
  app.blocks = []
  assert.doesNotThrow(() => app.adoptSession({ sessionId: 'session-empty', configOptions: [] }, 'session-empty'))
  assert.equal(app.sessionId, 'session-empty')
  assert.equal(app.blocks.length, 0)
})

test('/new with an empty transcript creates the session without a bogus error', async () => {
  const { app } = makeApp()
  const calls = []
  app.client = {
    sessions: { close: true },
    request(method) {
      calls.push(method)
      if (method === 'session/new') {
        return Promise.resolve({ sessionId: 'session-new', configOptions: [] })
      }
      return Promise.resolve({})
    },
    respond() {},
  }
  app.sessionId = 'session-old'
  app.blocks = []
  await app.startNewSession()
  assert.equal(app.sessionId, 'session-new')
  assert.deepEqual(calls, ['session/close', 'session/new'])
  assert.equal(app.blocks[0].type, 'banner')
  assert.equal(app.blocks[0].sessionId, 'session-new')
  assert.deepEqual(
    app.blocks.filter((block) => block.type === 'notice' && block.level === 'error'),
    [],
  )
})

test('a second permission request waits its turn instead of replacing the first', () => {
  const { app } = makeApp()
  const answered = []
  app.client = {
    respond(id, outcome) {
      answered.push({ id, outcome })
    },
  }
  const request = (id) => ({
    id,
    method: 'session/request_permission',
    params: {
      toolCall: { title: `tool-${id}` },
      options: [{ optionId: `allow-${id}`, kind: 'allow_once' }],
    },
  })

  app.onRequest(request(1))
  app.onRequest(request(2))
  assert.equal(app.pendingPermission.id, 1, 'the first request keeps the dialog')
  assert.equal(app.overlay.toolName, 'tool-1')
  assert.equal(answered.length, 0)

  app.answerPermission({ value: 'allow-1' })
  assert.deepEqual(answered, [
    { id: 1, outcome: { result: { outcome: { outcome: 'selected', optionId: 'allow-1' } } } },
  ])
  assert.equal(app.pendingPermission.id, 2, 'the queued request is shown next')
  assert.equal(app.overlay.toolName, 'tool-2')

  app.answerPermission({ value: 'allow-2' })
  assert.equal(answered.length, 2)
  assert.equal(answered[1].id, 2)
  assert.equal(app.pendingPermission, null)
  assert.equal(app.overlay, null)
})

/** One permission request shaped the way the harness sends it. */
function permissionRequest(id, { command = 'npm test', options } = {}) {
  return {
    id,
    method: 'session/request_permission',
    params: {
      toolCall: { title: 'bash', kind: 'bash', rawInput: { command } },
      options: options ?? [
        { optionId: 'allow-once', kind: 'allow_once' },
        { optionId: 'allow-always', kind: 'allow_always' },
        { optionId: 'reject-once', kind: 'reject_once' },
      ],
    },
  }
}

/** A client that records what the terminal answered and asked. */
function recordingClient() {
  const answered = []
  const prompts = []
  return {
    answered,
    prompts,
    respond(id, payload) {
      answered.push({ id, payload })
    },
    request(method, params) {
      if (method === 'session/prompt') prompts.push(params.prompt?.[0]?.text ?? '')
      return Promise.resolve({ stopReason: 'end_turn' })
    },
    notify() {},
    close: async () => {},
  }
}

test('an approval asks with the same four numbered choices as the in-process terminal', () => {
  const { app } = makeApp()
  app.client = recordingClient()
  app.approvalMode = 'ask'
  app.onRequest(permissionRequest(1))
  assert.deepEqual(app.overlay.options.map((option) => option.value),
    ['allowed-once', 'session-allow', 'rejected', 'reject-reason'])
})

test('the number keys answer: 1 runs and 3 rejects', () => {
  const { app } = makeApp()
  const client = recordingClient()
  app.client = client
  app.approvalMode = 'ask'

  app.onRequest(permissionRequest(1))
  app.onKeyEvent({ name: 'char', text: '1' })
  assert.deepEqual(client.answered[0].payload.result.outcome, { outcome: 'selected', optionId: 'allow-once' })

  app.onRequest(permissionRequest(2))
  app.onKeyEvent({ name: 'char', text: '3' })
  assert.deepEqual(client.answered[1].payload.result.outcome, { outcome: 'selected', optionId: 'reject-once' })
})

test('"run for this session" answers the first request and silences the next one', () => {
  const { app } = makeApp()
  const client = recordingClient()
  app.client = client
  app.approvalMode = 'ask'

  app.onRequest(permissionRequest(1))
  app.onKeyEvent({ name: 'char', text: '2' })
  assert.deepEqual(client.answered[0].payload.result.outcome, { outcome: 'selected', optionId: 'allow-always' })

  // The same command again: answered without a dialog, and the overlay stays closed.
  app.onRequest(permissionRequest(2))
  assert.equal(app.overlay, null)
  assert.equal(client.answered.length, 2)
  assert.deepEqual(client.answered[1].payload.result.outcome, { outcome: 'selected', optionId: 'allow-once' })
})

test('"reject with a reason" sends the reason to the model after the rejection', () => {
  const { app } = makeApp()
  const client = recordingClient()
  app.client = client
  app.approvalMode = 'ask'
  app.sessionId = 'session-live'
  app.status = 'ready'

  app.onRequest(permissionRequest(1))
  app.onKeyEvent({ name: 'char', text: '4' })
  assert.ok(app.overlay.reason !== undefined, 'the reason line opens inside the dialog')
  assert.equal(client.answered.length, 0, 'nothing is settled while the reason is being typed')

  for (const letter of ['太', '危', '险']) app.onKeyEvent({ name: 'char', text: letter })
  app.onKeyEvent({ name: 'enter' })

  assert.deepEqual(client.answered[0].payload.result.outcome, { outcome: 'selected', optionId: 'reject-once' })
  assert.equal(client.prompts.length, 1, 'the reason is one prompt of its own')
  assert.match(client.prompts[0], /太危险/u)
  assert.match(client.prompts[0], /bash/u)
})

test('escape returns to the choices without rejecting', () => {
  const { app } = makeApp()
  const client = recordingClient()
  app.client = client
  app.approvalMode = 'ask'

  app.onRequest(permissionRequest(1))
  app.onKeyEvent({ name: 'char', text: '4' })
  app.onKeyEvent({ name: 'escape' })
  assert.equal(app.overlay.reason, undefined)
  assert.equal(client.answered.length, 0, 'back to the four choices, nothing decided')

  app.onKeyEvent({ name: 'char', text: '1' })
  assert.equal(client.answered.length, 1)
})

test('critical mode answers a safe command and still asks about a dangerous one', () => {
  const { app } = makeApp()
  const client = recordingClient()
  app.client = client
  assert.equal(app.approvalMode, 'ask', 'the Kimi-style default is Always Ask')
  app.approvalMode = 'critical'

  app.onRequest(permissionRequest(1, { command: 'npm test' }))
  assert.equal(app.overlay, null, 'a safe command does not interrupt')
  assert.equal(client.answered.length, 1)

  app.onRequest(permissionRequest(2, { command: 'rm -rf /' }))
  assert.notEqual(app.overlay, null, 'a dangerous command still asks')
})

test('a session grant does not swallow a dangerous command', () => {
  const { app } = makeApp()
  const client = recordingClient()
  app.client = client
  app.approvalMode = 'ask'

  app.onRequest(permissionRequest(1, { command: 'rm -rf build' }))
  app.onKeyEvent({ name: 'char', text: '2' })
  assert.equal(app.allowedTools.has('bash rm -rf build'), true)

  app.onRequest(permissionRequest(2, { command: 'rm -rf /' }))
  assert.notEqual(app.overlay, null, 'a dangerous command still draws the dialog')
  app.onKeyEvent({ name: 'char', text: '3' })
  assert.deepEqual(client.answered.at(-1).payload.result.outcome,
    { outcome: 'selected', optionId: 'reject-once' })
})

test('a session switch resets the approval mode and counter', async () => {
  const { app } = makeApp()
  app.approvalMode = 'full'
  app.autoApproved = 4
  await app.detachTurn()
  assert.equal(app.approvalMode, 'ask')
  assert.equal(app.autoApproved, 0)
})

test('permanent rules deny and allow without a dialog', () => {
  const dir = mkdtempSync(join(tmpdir(), 'hdsl-acp-rules-'))
  const file = join(dir, 'permissions.json')
  const previous = process.env.HDSL_TUI_RULES
  process.env.HDSL_TUI_RULES = file
  try {
    const { app } = makeApp()
    const client = recordingClient()
    app.client = client
    app.runRuleCommand('deny', 'bash rm -rf /')
    app.onRequest(permissionRequest(1, { command: 'rm -rf /' }))
    assert.equal(app.overlay, null)
    assert.deepEqual(client.answered[0].payload.result.outcome,
      { outcome: 'selected', optionId: 'reject-once' })

    app.runRuleCommand('allow', 'bash npm test')
    app.onRequest(permissionRequest(2, { command: 'npm test' }))
    assert.equal(app.overlay, null)
    assert.deepEqual(client.answered[1].payload.result.outcome,
      { outcome: 'selected', optionId: 'allow-once' })
  } finally {
    if (previous === undefined) delete process.env.HDSL_TUI_RULES
    else process.env.HDSL_TUI_RULES = previous
    rmSync(dir, { recursive: true, force: true })
  }
})

test('the four choices are answered even when the server offers no reject option', () => {
  const { app } = makeApp()
  const client = recordingClient()
  app.client = client
  app.approvalMode = 'ask'
  app.onRequest(permissionRequest(1, { options: [{ optionId: 'allow-once', kind: 'allow_once' }] }))
  app.onKeyEvent({ name: 'char', text: '3' })
  assert.deepEqual(client.answered[0].payload.result.outcome, { outcome: 'cancelled' },
    'no reject option means cancelling, not inventing a decision')
})

test('Tab completes a command and its argument, cycling on repeat', () => {
  const { app } = makeApp()
  app.composer.setText('/yo')
  app.composer.caret = 3
  app.onKeyEvent({ name: 'tab' })
  assert.equal(app.composer.text, '/yolo', 'the command is completed')

  app.onKeyEvent({ name: 'char', text: ' ' })
  app.onKeyEvent({ name: 'tab' })
  assert.match(app.composer.text, /^\/yolo (ask|critical|full)/u)
  const first = app.composer.text
  app.onKeyEvent({ name: 'tab' })
  assert.notEqual(app.composer.text, first, 'a second Tab moves on')
})

test('a model argument completes from what the session advertised', () => {
  const { app } = makeApp()
  app.model = { current: 'sonnet', currentLabel: 'sonnet', options: [{ value: 'sonnet' }, { value: 'sonnet-long' }] }
  app.composer.setText('/model so')
  app.composer.caret = 9
  app.onKeyEvent({ name: 'tab' })
  assert.equal(app.composer.text, '/model sonnet')
})

test('the candidate line appears while there is something to cycle', () => {
  const { app } = makeApp()
  app.composer.setText('/yo')
  app.composer.caret = 3
  app.onKeyEvent({ name: 'tab' })
  assert.ok(app.completion !== null, 'the completion state is kept for cycling')
  assert.ok(app.completion.options.length >= 1)
  app.onKeyEvent({ name: 'char', text: 'x' })
  assert.equal(app.completion, null, 'typing ends the cycle')
})

test('"/permission" says which line can run it instead of pretending', () => {
  const { app } = makeApp()
  app.runCommand('permission', '')
  const notice = app.blocks.filter((block) => block.type === 'notice').at(-1)
  assert.match(notice.text, /permission/u)
  assert.match(notice.text, /0\.2|in-process|进程内/u)
})

test('/yolo full answers even a dangerous permission request', () => {
  const { app } = makeApp()
  const client = recordingClient()
  app.client = client
  app.runCommand('yolo', 'full')
  app.onRequest(permissionRequest(1, { command: 'rm -rf /' }))
  assert.equal(app.overlay, null, 'full mode does not interrupt')
  assert.equal(client.answered.length, 1)
})

test('/resume closes the session it is leaving', async () => {
  const { app } = makeApp()
  const calls = []
  app.client = {
    sessions: { close: true },
    request(method, params) {
      calls.push(`${method}:${params.sessionId ?? ''}`)
      if (method === 'session/resume') return Promise.resolve({ configOptions: [] })
      return Promise.resolve({})
    },
    notify() {},
    respond() {},
  }
  app.sessionId = 'session-old'
  await app.resumeSession('session-new')
  assert.deepEqual(calls, ['session/close:session-old', 'session/resume:session-new'])
  assert.equal(app.sessionId, 'session-new')
})

test('a failed resume says the terminal stayed where it was', async () => {
  const { app } = makeApp()
  app.client = {
    sessions: { close: true },
    request(method) {
      if (method === 'session/resume') return Promise.reject(new Error('no such session'))
      return Promise.resolve({})
    },
    notify() {},
    respond() {},
  }
  app.sessionId = 'session-old'
  await app.resumeSession('missing')
  const notice = app.blocks.filter((block) => block.type === 'notice' && block.level === 'error').at(-1)
  assert.match(notice.text, /no such session/u)
  assert.match(notice.text, /session-old|still|仍在/u)
  assert.equal(app.sessionId, 'session-old', 'the terminal did not adopt the failed session')
})

test('a protocol error becomes one notice, not a flood', () => {
  const { app } = makeApp()
  app.onProtocolError(new Error('the harness wrote a line that is not JSON'))
  app.onProtocolError(new Error('another bad line'))
  const notices = app.blocks.filter((block) => block.type === 'notice')
  assert.equal(notices.length, 1)
  assert.equal(notices[0].level, 'error')
  assert.match(notices[0].text, /not JSON/u)
})

test('stop restores the terminal, clears timers and runs once', async () => {
  const { app, stdout, stdin } = makeApp()
  let closes = 0
  app.client = {
    close: async () => {
      closes += 1
    },
  }
  app.screen.enter()
  app.installInput()
  app.timer = setInterval(() => {}, 1000)
  app.escTimer = setTimeout(() => {}, 1000)
  app.sessionId = 'session-1'
  const exited = app.waitForExit()

  await app.stop('quit')
  await app.stop('quit')

  assert.equal(closes, 1, 'the child is closed once')
  assert.equal(app.timer, null)
  assert.equal(app.escTimer, null)
  assert.equal(app.stopped, true)
  assert.equal(stdin.listenerCount('data'), 0)
  assert.equal(stdin.listenerCount('end'), 0)
  assert.deepEqual(stdin.rawModes, [true, false])
  assert.ok(stdout.text().includes('\x1b[?1049l'), 'the alternate screen is left')
  assert.equal(stdout.text().match(/session session-1/gu)?.length, 1, 'the resume hint is printed once')
  assert.equal(await deadline(exited), 0)
})

test('stop still resolves the exit when the child cannot be closed', async () => {
  const { app } = makeApp()
  app.client = {
    close: async () => {
      throw new Error('close failed')
    },
  }
  app.timer = setInterval(() => {}, 1000)
  const exited = app.waitForExit()

  await app.stop('quit')

  assert.equal(app.timer, null)
  assert.equal(await deadline(exited), 0)
})

test('the child dying exits the terminal instead of leaving it running', async () => {
  const { app } = makeApp()
  app.status = 'ready'
  app.client = { close: async () => {}, stderrLines: ['boom'] }
  app.screen.enter()
  app.timer = setInterval(() => {}, 1000)
  app.sessionId = 'session-live'
  const exited = app.waitForExit()

  app.onExit({ code: 7, signal: null })

  assert.equal(await deadline(exited), 7)
  assert.equal(app.timer, null)
  assert.equal(app.status, 'disconnected')
  assert.equal(app.stopped, true)
})

test('a failed handshake releases the terminal and exits with an error', async () => {
  const directory = mkdtempSync(join(tmpdir(), 'dsh-acp-tui-'))
  const script = join(directory, 'harness.mjs')
  // The terminal appends `--profile acp`, so the stand-in harness is a file, the
  // way the real `lib/bin.js` is; it simply exits without speaking ACP.
  writeFileSync(script, 'process.exit(3)\n')
  const { app, stdout, stdin } = makeApp()
  app.harness = { command: [process.execPath, script], source: 'test', profile: 'acp' }
  try {
    await assert.rejects(deadline(app.start(), 5000), /harness|exited|running/u)
    assert.equal(app.timer, null)
    assert.equal(app.stopped, true)
    assert.equal(await deadline(app.waitForExit(), 1000), 1)
    assert.deepEqual(stdin.rawModes, [true, false])
    assert.ok(stdout.text().includes('\x1b[?1049l'), 'the alternate screen is left on failure')
  } finally {
    await app.stop('test')
    rmSync(directory, { recursive: true, force: true })
  }
})
