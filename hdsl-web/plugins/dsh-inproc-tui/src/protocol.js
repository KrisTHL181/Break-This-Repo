/**
 * The decisions that need no terminal and no harness.
 *
 * These are the parts a regression can hide in: which argv this profile accepts,
 * which requests are this terminal's to answer, and the shape of the
 * ask-user-question interruption error. Nothing here imports a `@deepseek-ai/*`
 * package — `dsh-user-questions` is an *optional* peer, and a top-level import
 * of it keeps the whole terminal from loading on a harness that omits it.
 *
 * @module protocol
 */

import { VERSION } from './version.js'

/** The `dsh-user-questions` error class name, matched by shape. */
const QUESTION_ERROR_NAME = 'UserQuestionError'

/** The code dsh-plan-mode reads to tell an abort from a user cancel. */
const QUESTION_ABORTED = 'ASK_ABORTED'

/**
 * Builds the error an interrupted question rejects with.
 *
 * The protocol's class comes from an optional peer, so this rebuilds its shape
 * (`name` plus the `code` that dsh-plan-mode keys on) instead of importing it.
 *
 * @param {string} [message]
 * @returns {Error & {code: string}}
 */
export function questionInterrupted(message = 'ask_user_question was interrupted') {
  const error = new Error(message)
  error.name = QUESTION_ERROR_NAME
  error.code = QUESTION_ABORTED
  return error
}

/**
 * Whether `error` is the protocol's question error, by shape.
 *
 * @param {unknown} error
 * @returns {boolean}
 */
export function isUserQuestionError(error) {
  if (error === null || typeof error !== 'object') return false
  const { name, code } = error
  if (name !== QUESTION_ERROR_NAME) return false
  return typeof code === 'string' && code.startsWith('ASK_')
}

/**
 * Whether a waterfall request belongs to the agent this terminal drives.
 *
 * A request with no agent (`/auth`, host wizards) is not ours: answering it
 * would steal a question meant for whoever opened it.
 *
 * @param {{agent?: unknown} | null} request
 * @param {object | null} live the open agent
 * @returns {boolean}
 */
export function ownsRequest(request, live) {
  if (request === null || typeof request !== 'object') return false
  const agent = request.agent
  if (agent === undefined || agent === null) return false
  if (live === undefined || live === null) return false
  if (agent === live) return true
  // The host may hand the request a re-wrapped handle rather than the object
  // the registry returned. dsh-tui compares ids for exactly this reason; DSH
  // shares the agent/session identity, so a matching id is the same agent.
  const agentId = agentIdOf(agent)
  return agentId !== undefined && agentId === agentIdOf(live)
}

/**
 * The identity a DSH agent exposes, however it was wrapped.
 *
 * @param {object} value
 * @returns {string | undefined}
 */
function agentIdOf(value) {
  const id = value?.session?.id ?? value?.id
  return typeof id === 'string' && id !== '' ? id : undefined
}

/**
 * Wraps a waterfall handler so only this terminal's agent is answered.
 *
 * In 0.2.x `approval/request` and `user-questions/request` are waterfalls: a
 * listener that returns claims the request, and one that calls `next()` lets
 * the next listener (or the host default) answer it. A throw from a handler
 * must not escape into the host's dispatch, so it delegates instead.
 *
 * @param {{
 *   isMine: (request: object) => boolean,
 *   handle: (request: object) => unknown,
 *   onError?: (error: unknown) => void,
 * }} options
 * @returns {(request: object, next: () => unknown) => unknown}
 */
export function scopedWaterfall({ isMine, handle, onError }) {
  return (request, next) => {
    let mine = false
    try {
      mine = isMine(request) === true
    } catch (error) {
      onError?.(error)
    }
    if (!mine) return typeof next === 'function' ? next() : undefined
    try {
      return handle(request)
    } catch (error) {
      onError?.(error)
      return typeof next === 'function' ? next() : undefined
    }
  }
}

/**
 * Parses this profile's arguments.
 *
 * @param {string[]} argv
 * @returns {{resume: string | null, help: boolean}}
 */
export function parseArgs(argv) {
  const result = { resume: null, help: false }
  const list = Array.isArray(argv) ? argv : []
  for (let index = 0; index < list.length; index += 1) {
    const argument = list[index]
    if (typeof argument !== 'string') continue
    if (argument === '--help' || argument === '-h') result.help = true
    else if (argument === '--resume' || argument === '-r') {
      result.resume = list[index + 1] ?? null
      index += 1
    } else if (argument.startsWith('--resume=')) {
      result.resume = argument.slice('--resume='.length)
    }
  }
  return result
}

/**
 * Parses a launcher's `process.argv`, skipping the profile selector.
 *
 * @param {string[]} argv
 * @returns {{resume: string | null, help: boolean}}
 */
export function argsFromProcess(argv) {
  const list = Array.isArray(argv) ? argv : []
  const index = list.indexOf('--profile')
  return parseArgs(index >= 0 ? list.slice(index + 2) : list.slice(2))
}

/** The `--help` text this profile prints. */
export function usage() {
  return `dsh-inproc-tui ${VERSION} — HDSL-web's terminal interface for DeepSeek Harness 0.2.x

Usage:
  dsh --profile dsh-inproc-tui                 start a new session in the current directory
  dsh --profile dsh-inproc-tui --resume <id>   resume a stored session
  dsh --profile dsh-inproc-tui --help          print this help

The panel starts this for you (instance terminal). It runs in-process over
dsh-base, so it needs a 0.2.x harness: the agent, streaming, approval and
question services are the harness's own.
`
}
