/**
 * An Agent Client Protocol client over a child process.
 *
 * The terminal does not import any harness package: it starts `dsh --profile acp`
 * as a child and speaks ACP v1 to it on newline-delimited JSON-RPC. That is the
 * one stable seam the harness offers a front end — a documented protocol rather
 * than an in-process service graph — so a new dsh release changes nothing here,
 * and the terminal keeps working while the harness's internals keep moving.
 *
 * The failure paths are as much a part of that seam as the happy one: a pipe that
 * dies under a write (EPIPE after the child is gone) must not surface as an
 * uncaught exception, a child that never answers the handshake must not keep the
 * terminal alive forever, and a line that never ends must not grow without bound.
 * All three end in the same `exit` event the UI already listens for.
 *
 * @module acp
 */

import { spawn } from 'node:child_process'
import { EventEmitter } from 'node:events'

const PROTOCOL_VERSION = 1

/** Default cap on the stderr kept for diagnostics. */
const STDERR_LINES = 80

/** How long the handshake may take before the client gives up on it. */
export const HANDSHAKE_TIMEOUT_MS = 30_000

/**
 * Longest stdout line the reader buffers.
 *
 * ACP updates can carry a whole tool output, so the cap is generous; its job is
 * to bound a line that never ends. Past the cap the rest of that line is dropped
 * up to the next newline, so one bad message costs a notice instead of a hang.
 */
export const MAX_LINE_LENGTH = 16 * 1024 * 1024

/** A JSON-RPC error surfaced as a rejected request. */
export class AcpError extends Error {
  constructor(message, code, data) {
    super(message)
    this.name = 'AcpError'
    this.code = code
    this.data = data
  }
}

/**
 * The ACP client.
 *
 * Events: `update` (session/update params), `request` (an agent-to-client request
 * the UI must answer through {@link respond}), `protocolError` (a stdout line the
 * client could not accept), `stderr`, `exit`.
 */
export class AcpClient extends EventEmitter {
  /**
   * @param {{
   *   command: string[],
   *   cwd: string,
   *   env: Record<string, string | undefined>,
   *   log?: (line: string) => void,
   *   maxLineLength?: number,
   * }} options
   */
  constructor(options) {
    super()
    this.command = options.command
    this.cwd = options.cwd
    this.env = options.env
    this.log = options.log ?? (() => {})
    this.maxLineLength = options.maxLineLength ?? MAX_LINE_LENGTH
    this.child = null
    this.pending = new Map()
    this.nextId = 1
    this.buffer = ''
    this.discarding = false
    this.stderrLines = []
    this.closed = false
    this.finalized = false
    this.closing = null
    this.agentInfo = null
    this.capabilities = {}
  }

  /** Starts the child process and begins parsing its stdout. */
  start() {
    const [file, ...args] = this.command
    this.attach(spawn(file, args, {
      cwd: this.cwd,
      env: this.env,
      stdio: ['pipe', 'pipe', 'pipe'],
    }))
    return this
  }

  /**
   * Wires a child that is already running.
   *
   * `start` uses it for the real process; tests use it to drive the protocol over
   * `PassThrough` streams instead of a harness.
   *
   * @param {import('node:child_process').ChildProcess} child
   */
  attach(child) {
    this.child = child
    child.stdout.setEncoding('utf8')
    child.stderr.setEncoding('utf8')
    child.stdout.on('data', (chunk) => this.onData(chunk))
    child.stderr.on('data', (chunk) => this.onStderr(chunk))
    // A write after the child dies arrives here as EPIPE/ERR_STREAM_DESTROYED.
    // With no listener Node raises it as an uncaught exception; the pipe error is
    // simply the child being gone, which `finalize` already knows how to handle.
    child.stdin.on('error', (error) => this.onStdinError(error))
    child.on('error', (error) => this.finalize({ code: null, signal: null, error }))
    child.on('exit', (code, signal) => this.finalize({ code, signal }))
    return this
  }

  /** @private */
  onStderr(chunk) {
    for (const line of chunk.split('\n')) {
      const trimmed = line.trim()
      if (trimmed === '') continue
      this.stderrLines.push(trimmed)
      if (this.stderrLines.length > STDERR_LINES) this.stderrLines.shift()
      this.emit('stderr', trimmed)
    }
  }

  /** @private */
  onStdinError(error) {
    this.log(`stdin error: ${error?.message ?? error}`)
    this.finalize({
      code: null,
      signal: null,
      error: error instanceof Error ? error : new AcpError(String(error), -32000),
    })
  }

  /** @private */
  onData(chunk) {
    if (this.finalized) return
    if (this.discarding) {
      // Still inside a line that was already too long: drop it up to its end.
      const newline = chunk.indexOf('\n')
      if (newline < 0) return
      this.discarding = false
      chunk = chunk.slice(newline + 1)
    }
    this.buffer += chunk
    let index = this.buffer.indexOf('\n')
    while (index >= 0) {
      const line = this.buffer.slice(0, index).trim()
      this.buffer = this.buffer.slice(index + 1)
      index = this.buffer.indexOf('\n')
      if (line !== '') this.acceptLine(line)
    }
    if (this.buffer.length > this.maxLineLength) {
      this.buffer = ''
      this.discarding = true
      const error = new AcpError(
        `the harness sent a line longer than ${this.maxLineLength} characters`,
        -32700,
      )
      this.log(error.message)
      this.emit('protocolError', error)
    }
  }

  /** @private */
  acceptLine(line) {
    let message
    try {
      message = JSON.parse(line)
    } catch {
      this.log(`non-json stdout: ${line.slice(0, 400)}`)
      this.emit('protocolError', new AcpError(
        `the harness wrote a line that is not JSON: ${line.slice(0, 120)}`,
        -32700,
      ))
      return
    }
    this.dispatch(message)
  }

  /** @private */
  dispatch(message) {
    if (typeof message !== 'object' || message === null) return
    if (message.method !== undefined) {
      if (message.id !== undefined) {
        this.emit('request', {
          id: message.id,
          method: message.method,
          params: message.params ?? {},
        })
      } else {
        this.emit('notification', { method: message.method, params: message.params ?? {} })
      }
      return
    }
    if (message.id === undefined) return
    const entry = this.pending.get(message.id)
    // A late answer to a request that already timed out (or an id from another
    // peer) settles nothing; dropping it here is what keeps that safe.
    if (entry === undefined) return
    this.pending.delete(message.id)
    if (entry.timer !== null) {
      clearTimeout(entry.timer)
      entry.timer = null
    }
    if (message.error !== undefined) {
      entry.reject(new AcpError(message.error.message ?? 'request failed', message.error.code, message.error.data))
    } else {
      entry.resolve(message.result ?? {})
    }
  }

  /**
   * Sends a request and resolves with its result.
   * @param {string} method
   * @param {unknown} params
   * @param {{timeoutMs?: number}} [options]
   * @returns {Promise<any>}
   */
  request(method, params, options = {}) {
    if (this.child === null || this.closed) {
      return Promise.reject(new AcpError('the harness is not running', -32000))
    }
    const timeoutMs = Number(options.timeoutMs ?? 0)
    const id = this.nextId
    this.nextId += 1
    const payload = JSON.stringify({ jsonrpc: '2.0', id, method, params }) + '\n'
    return new Promise((resolve, reject) => {
      const entry = { method, resolve, reject, timer: null }
      this.pending.set(id, entry)
      if (Number.isFinite(timeoutMs) && timeoutMs > 0) {
        entry.timer = setTimeout(() => {
          if (this.pending.get(id) !== entry) return
          this.pending.delete(id)
          entry.timer = null
          reject(new AcpError(`the harness did not answer ${method} within ${timeoutMs} ms`, -32000))
        }, timeoutMs)
        // A pending answer is not a reason to keep the process alive.
        entry.timer.unref?.()
      }
      const onWritten = (error) => {
        if (error === undefined || error === null) return
        if (this.pending.get(id) !== entry) return
        this.pending.delete(id)
        if (entry.timer !== null) {
          clearTimeout(entry.timer)
          entry.timer = null
        }
        reject(new AcpError(error.message, -32000))
      }
      try {
        this.child.stdin.write(payload, onWritten)
      } catch (error) {
        this.pending.delete(id)
        if (entry.timer !== null) clearTimeout(entry.timer)
        entry.timer = null
        reject(new AcpError(error?.message ?? String(error), -32000))
      }
    })
  }

  /** @private */
  writeLine(payload) {
    if (this.child === null || this.closed) return false
    try {
      this.child.stdin.write(payload, (error) => {
        if (error) this.log(`stdin write failed: ${error.message}`)
      })
      return true
    } catch (error) {
      this.log(`stdin write failed: ${error?.message ?? error}`)
      return false
    }
  }

  /** Sends a notification (no response expected). */
  notify(method, params) {
    this.writeLine(JSON.stringify({ jsonrpc: '2.0', method, params }) + '\n')
  }

  /**
   * Answers an agent-to-client request.
   * @param {number} id
   * @param {{result?: unknown, error?: {code: number, message: string}}} outcome
   */
  respond(id, outcome) {
    const body = outcome.error !== undefined
      ? { jsonrpc: '2.0', id, error: outcome.error }
      : { jsonrpc: '2.0', id, result: outcome.result ?? {} }
    this.writeLine(JSON.stringify(body) + '\n')
  }

  /**
   * Runs the ACP handshake.
   * @param {{name: string, version: string}} clientInfo
   * @param {{timeoutMs?: number}} [options]
   * @returns {Promise<{protocolVersion: number, agentInfo: unknown, agentCapabilities: object}>}
   */
  async initialize(clientInfo, options = {}) {
    const result = await this.request('initialize', {
      protocolVersion: PROTOCOL_VERSION,
      clientCapabilities: {
        fs: { readTextFile: false, writeTextFile: false },
        terminal: false,
      },
      clientInfo,
    }, { timeoutMs: options.timeoutMs ?? HANDSHAKE_TIMEOUT_MS })
    this.agentInfo = result.agentInfo ?? null
    this.capabilities = result.agentCapabilities ?? {}
    this.protocolVersion = result.protocolVersion ?? PROTOCOL_VERSION
    return result
  }

  get sessions() {
    return this.capabilities.sessionCapabilities ?? {}
  }

  /**
   * Marks the client dead exactly once, rejects what was pending and tells the UI.
   * Both the child's `exit` and a dead stdin funnel through here, so `closed` is
   * true whichever way the process went — which is what lets `close()` return at
   * once instead of waiting out its grace period for an exit that already happened.
   *
   * @private
   */
  finalize(info) {
    if (this.finalized) return
    this.finalized = true
    this.closed = true
    const failure = info?.error ?? new AcpError('the harness exited before answering', -32000)
    for (const [, entry] of this.pending) {
      if (entry.timer !== null) clearTimeout(entry.timer)
      entry.reject(failure)
    }
    this.pending.clear()
    this.buffer = ''
    this.discarding = false
    this.emit('exit', {
      code: info?.code ?? null,
      signal: info?.signal ?? null,
      error: info?.error,
    })
  }

  /** Asks the harness to stop and waits briefly for the child to go. */
  async close() {
    if (this.child === null) return
    if (this.closed) return
    if (this.closing === null) this.closing = this.shutdown()
    return this.closing
  }

  /** @private */
  async shutdown() {
    const child = this.child
    try {
      child.stdin.end()
    } catch {
      // stdin may already be gone.
    }
    if (this.closed) return
    await new Promise((resolve) => {
      let settled = false
      let killTimer = null
      const done = () => {
        if (settled) return
        settled = true
        clearTimeout(timer)
        if (killTimer !== null) clearTimeout(killTimer)
        this.removeListener('exit', done)
        resolve()
      }
      const timer = setTimeout(() => {
        try {
          child.kill('SIGTERM')
        } catch {
          // already exited
        }
        killTimer = setTimeout(() => {
          try {
            child.kill('SIGKILL')
          } catch {
            // already exited
          }
          done()
        }, 1500)
      }, 1200)
      this.once('exit', done)
      if (this.closed) done()
    })
  }
}
