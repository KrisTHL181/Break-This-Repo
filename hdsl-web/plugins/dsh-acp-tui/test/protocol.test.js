/**
 * Protocol-layer tests.
 *
 * The unit tests cover the pure parts; these drive `AcpClient` itself over fake
 * pipes, so framing, timeouts and the two ways a child dies are exercised
 * without a harness.
 */

import { test } from 'node:test'
import assert from 'node:assert/strict'
import { EventEmitter } from 'node:events'
import { mkdtempSync, rmSync, writeFileSync } from 'node:fs'
import { tmpdir } from 'node:os'
import { join } from 'node:path'
import { PassThrough } from 'node:stream'

import { AcpClient } from '../src/acp.js'

/** Rejects if `promise` has not settled within `ms`; a hung regression must fail, not hang. */
function deadline(promise, ms = 1500, label = 'timed out') {
  let timer = null
  const guard = new Promise((_resolve, reject) => {
    timer = setTimeout(() => reject(new Error(label)), ms)
  })
  return Promise.race([promise, guard]).finally(() => clearTimeout(timer))
}

/** A child process stand-in: pipes and events, with `kill` recorded instead of real. */
function fakeChild() {
  const child = new EventEmitter()
  child.stdout = new PassThrough()
  child.stderr = new PassThrough()
  child.stdin = new PassThrough()
  child.signals = []
  child.kill = (signal) => {
    child.signals.push(signal)
    return true
  }
  return child
}

/** An `AcpClient` wired to a fake child, without spawning anything. */
function fakeClient(options = {}) {
  const child = fakeChild()
  const client = new AcpClient({
    command: ['dsh'],
    cwd: process.cwd(),
    env: {},
    ...options,
  })
  client.attach(child)
  return { client, child }
}

/** Lets a `PassThrough` write reach its reader. */
function tick() {
  return new Promise((resolve) => setImmediate(resolve))
}

test('two replies arriving in one read both settle', async () => {
  const { client, child } = fakeClient()
  const first = client.request('session/prompt', {})
  const second = client.request('session/prompt', {})
  child.stdout.write(
    '{"jsonrpc":"2.0","id":1,"result":{"n":1}}\n'
    + '{"jsonrpc":"2.0","id":2,"result":{"n":2}}\n',
  )
  assert.deepEqual(await deadline(first), { n: 1 })
  assert.deepEqual(await deadline(second), { n: 2 })
  assert.equal(client.pending.size, 0)
})

test('a line split across two reads is parsed once it is complete', async () => {
  const { client, child } = fakeClient()
  const reply = client.request('session/new', {})
  child.stdout.write('{"jsonrpc":"2.0","id":1,"res')
  await tick()
  assert.equal(client.pending.size, 1, 'half a line settles nothing')
  child.stdout.write('ult":{"sessionId":"s-1"}}\n')
  assert.deepEqual(await deadline(reply), { sessionId: 's-1' })
})

test('a non-JSON line is reported and the reader keeps going', async () => {
  const { client, child } = fakeClient()
  const errors = []
  client.on('protocolError', (error) => errors.push(error))
  const reply = client.request('session/prompt', {})
  child.stdout.write(
    'harness: a stray log line\n'
    + '{"jsonrpc":"2.0","id":1,"result":{"ok":true}}\n',
  )
  assert.deepEqual(await deadline(reply), { ok: true })
  assert.equal(errors.length, 1)
  assert.match(errors[0].message, /not JSON/u)
  assert.equal(client.closed, false, 'one bad line does not end the session')
})

test('a line longer than the cap is dropped and the next line still parses', async () => {
  const { client, child } = fakeClient({ maxLineLength: 64 })
  const errors = []
  client.on('protocolError', (error) => errors.push(error))
  const reply = client.request('session/prompt', {})
  child.stdout.write('x'.repeat(200))
  await tick()
  assert.equal(errors.length, 1)
  assert.match(errors[0].message, /longer than 64/u)
  assert.equal(client.buffer, '', 'the oversized line is not kept')
  child.stdout.write('tail-of-the-long-line\n{"jsonrpc":"2.0","id":1,"result":{"ok":true}}\n')
  assert.deepEqual(await deadline(reply), { ok: true })
})

test('a stdin error marks the client closed instead of throwing', async () => {
  const { client, child } = fakeClient()
  const exits = []
  client.on('exit', (info) => exits.push(info))
  const pending = client.request('session/prompt', {})
  assert.doesNotThrow(() => child.stdin.emit('error', new Error('write EPIPE')))
  assert.equal(client.closed, true)
  assert.equal(exits.length, 1)
  await assert.rejects(deadline(pending, 500), /EPIPE/u)
  // The grace period is for a child that is still alive; this one is not.
  await deadline(client.close(), 500)
  await assert.rejects(client.request('session/prompt', {}), /not running/u)
})

test('a child error finalises the client once and unblocks close', async () => {
  const { client, child } = fakeClient()
  const exits = []
  client.on('exit', (info) => exits.push(info))
  assert.doesNotThrow(() => child.emit('error', new Error('spawn ENOENT')))
  assert.equal(client.closed, true)
  assert.equal(exits.length, 1)
  assert.match(exits[0].error.message, /ENOENT/u)
  await deadline(client.close(), 500)
})

test('a handshake that never answers times out', async () => {
  const { client } = fakeClient()
  await assert.rejects(
    deadline(client.initialize({ name: 'dsh-acp-tui', version: '0.0.0' }, { timeoutMs: 25 }), 1000),
    /did not answer initialize/u,
  )
  assert.equal(client.pending.size, 0)
  assert.equal(client.closed, false, 'a slow answer is not a dead child')
})

test('a reply for a request that already timed out is ignored', async () => {
  const { client, child } = fakeClient()
  const reply = client.request('session/new', {}, { timeoutMs: 20 })
  await assert.rejects(deadline(reply, 1000), /did not answer session\/new/u)
  assert.equal(client.pending.size, 0)
  assert.doesNotThrow(() => client.dispatch({ jsonrpc: '2.0', id: 1, result: { sessionId: 'late' } }))
  child.stdout.write('{"jsonrpc":"2.0","id":1,"result":{"sessionId":"late"}}\n')
  await tick()
  assert.equal(client.pending.size, 0)
  assert.equal(client.closed, false)
})

test('a real child process completes the handshake over pipes', async () => {
  const directory = mkdtempSync(join(tmpdir(), 'dsh-acp-protocol-'))
  const script = join(directory, 'harness.mjs')
  // Stands in for `dsh --profile acp`: newline-delimited JSON-RPC on stdio,
  // exiting when its stdin closes.
  writeFileSync(script, `
import { stdin, stdout } from 'node:process'
stdin.setEncoding('utf8')
let buffer = ''
stdin.on('data', (chunk) => {
  buffer += chunk
  let index = buffer.indexOf('\\n')
  while (index >= 0) {
    const line = buffer.slice(0, index).trim()
    buffer = buffer.slice(index + 1)
    index = buffer.indexOf('\\n')
    if (line === '') continue
    const message = JSON.parse(line)
    if (message.method === 'initialize') {
      stdout.write(JSON.stringify({ jsonrpc: '2.0', id: message.id, result: { protocolVersion: 1, agentCapabilities: {} } }) + '\\n')
    } else if (message.method === 'session/new') {
      stdout.write(JSON.stringify({ jsonrpc: '2.0', id: message.id, result: { sessionId: 's-live' } }) + '\\n')
    }
  }
})
`)
  const client = new AcpClient({
    command: [process.execPath, script, '--profile', 'acp'],
    cwd: process.cwd(),
    env: process.env,
  })
  try {
    client.start()
    const handshake = await deadline(client.initialize({ name: 'dsh-acp-tui', version: '0.0.0' }), 5000)
    assert.equal(handshake.protocolVersion, 1)
    const session = await deadline(client.request('session/new', { cwd: process.cwd(), mcpServers: [] }), 5000)
    assert.equal(session.sessionId, 's-live')
  } finally {
    await deadline(client.close(), 5000)
    rmSync(directory, { recursive: true, force: true })
  }
  assert.equal(client.closed, true)
})
