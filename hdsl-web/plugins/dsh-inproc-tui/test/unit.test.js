/**
 * Unit tests for the decisions that need no terminal and no harness.
 *
 * The terminal loop itself needs a 0.2.x harness; these pin the parts a
 * regression can hide in without one: the shape of the optional peer's error,
 * the waterfall scope that keeps this terminal from answering someone else's
 * approval or question, and the `--help` / argument path that used to hang.
 * Everything here is offline and hermetic.
 */

import { test } from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'

import { textOf } from '../../shared/i18n.js'
import {
  argsFromProcess,
  isUserQuestionError,
  ownsRequest,
  parseArgs,
  questionInterrupted,
  scopedWaterfall,
  usage,
} from '../src/protocol.js'

const AGENT = { id: 'session-own' }
const OTHER_AGENT = { id: 'session-subagent' }

test('the optional question-error peer is rebuilt by shape', () => {
  const error = questionInterrupted()
  assert.equal(error.name, 'UserQuestionError')
  assert.equal(error.code, 'ASK_ABORTED')
  assert.equal(error.message, 'ask_user_question was interrupted')
  assert.ok(error instanceof Error)

  // A user cancel carries the other code dsh-plan-mode reads.
  const cancelled = Object.assign(new Error('cancelled'), { name: 'UserQuestionError', code: 'ASK_CANCELLED' })
  assert.equal(isUserQuestionError(cancelled), true)
  // A plain object of the same shape is still the protocol's error.
  assert.equal(isUserQuestionError({ name: 'UserQuestionError', code: 'ASK_ABORTED' }), true)

  assert.equal(isUserQuestionError(new Error('boom')), false, 'a plain error is not the protocol error')
  assert.equal(isUserQuestionError({ code: 'ASK_ABORTED' }), false, 'the name is part of the shape')
  assert.equal(isUserQuestionError({ name: 'UserQuestionError' }), false, 'the code is part of the shape')
  assert.equal(isUserQuestionError({ name: 'UserQuestionError', code: 'OTHER' }), false)
  assert.equal(isUserQuestionError('ASK_ABORTED'), false)
  assert.equal(isUserQuestionError(null), false)
  assert.equal(isUserQuestionError(undefined), false)
})

test('only this terminal\'s agent owns a request', () => {
  assert.equal(ownsRequest({ agent: AGENT }, AGENT), true)
  // The wrapper nuance the reference handles by id: the request may carry a
  // re-wrapped handle whose session id is this terminal's agent.
  assert.equal(ownsRequest({ agent: { id: 'session-own' } }, AGENT), true)
  assert.equal(ownsRequest({ agent: { session: { id: 'session-own' } } }, AGENT), true)
  assert.equal(ownsRequest({ agent: OTHER_AGENT }, AGENT), false, 'a sub-agent\'s request is not ours')
  assert.equal(ownsRequest({ agent: { id: 'session-subagent' } }, AGENT), false)
  assert.equal(ownsRequest({}, AGENT), false, 'an agentless request (/auth) is not ours')
  assert.equal(ownsRequest({ agent: undefined }, AGENT), false)
  assert.equal(ownsRequest({ agent: null }, AGENT), false)
  assert.equal(ownsRequest({ agent: AGENT }, null), false, 'no open agent owns nothing')
  assert.equal(ownsRequest(null, AGENT), false)
})

test('a waterfall answers its own agent and delegates the rest', () => {
  const handled = []
  const listen = scopedWaterfall({
    isMine: (request) => ownsRequest(request, AGENT),
    handle: (request) => {
      handled.push(request)
      return 'answered'
    },
  })
  const next = () => ({ delegated: true })

  // This terminal's agent: answer it, do not call next().
  assert.equal(listen({ agent: AGENT, toolName: 'bash' }, next), 'answered')
  assert.equal(handled.length, 1)

  // Another agent: delegate, do not answer it.
  assert.deepEqual(listen({ agent: OTHER_AGENT, toolName: 'bash' }, next), { delegated: true })
  assert.equal(handled.length, 1)

  // Agentless (the host's /auth wizard): not stolen.
  assert.deepEqual(listen({ questions: [] }, next), { delegated: true })
  assert.equal(handled.length, 1)
})

test('a throwing waterfall handler delegates instead of escaping', () => {
  const reported = []
  const listen = scopedWaterfall({
    isMine: () => true,
    handle: () => {
      throw new Error('broken handler')
    },
    onError: (error) => reported.push(error),
  })
  assert.equal(listen({ agent: AGENT }, () => 'delegated'), 'delegated')
  assert.equal(reported.length, 1)
  assert.match(reported[0].message, /broken handler/u)

  // A scope predicate that throws also delegates rather than killing dispatch.
  const reportedScope = []
  const scoped = scopedWaterfall({
    isMine: () => {
      throw new Error('broken predicate')
    },
    handle: () => 'answered',
    onError: (error) => reportedScope.push(error),
  })
  assert.equal(scoped({ agent: AGENT }, () => 'delegated'), 'delegated')
  assert.equal(reportedScope.length, 1)
})

test('argv parsing accepts --help and --resume in every spelling', () => {
  assert.deepEqual(parseArgs([]), { resume: null, help: false })
  assert.equal(parseArgs(['--help']).help, true)
  assert.equal(parseArgs(['-h']).help, true)
  assert.deepEqual(parseArgs(['--resume', 'session-1']), { resume: 'session-1', help: false })
  assert.deepEqual(parseArgs(['-r', 'session-2']), { resume: 'session-2', help: false })
  assert.deepEqual(parseArgs(['--resume=session-3']), { resume: 'session-3', help: false })
  assert.deepEqual(parseArgs(['--help', '--resume', 'session-4']), { resume: 'session-4', help: true })
  assert.deepEqual(parseArgs(['--unknown', 'prompt text']), { resume: null, help: false })
  assert.deepEqual(parseArgs(undefined), { resume: null, help: false })
})

test('the launcher argv skips the profile selector', () => {
  assert.deepEqual(
    argsFromProcess(['node', 'bin.js', '--profile', 'dsh-inproc-tui', '--help']),
    { resume: null, help: true },
    '--help after the profile name reaches the terminal',
  )
  assert.deepEqual(
    argsFromProcess(['node', 'bin.js', '--profile', 'dsh-inproc-tui', '--resume', 'session-9']),
    { resume: 'session-9', help: false },
  )
  assert.deepEqual(
    argsFromProcess(['node', 'bin.js', '--resume', 'session-8']),
    { resume: 'session-8', help: false },
    'a launcher without --profile still gets --resume',
  )
})

test('usage documents the arguments that end the app', () => {
  const text = usage()
  assert.match(text, /--help/u)
  assert.match(text, /--resume <id>/u)
  assert.match(text, /dsh --profile dsh-inproc-tui/u)
})

test('the optional services live on the patch row, not the code-level inject', () => {
  const index = readFileSync(new URL('../src/index.js', import.meta.url), 'utf8')
  assert.match(index, /export const inject = \['agents'\]/u, 'the code-level inject is the required service only')

  const patch = readFileSync(new URL('../cordis.patch.yml', import.meta.url), 'utf8')
  assert.match(
    patch,
    /inject:\s*\[agentDefaultModel,\s*sessions\]/u,
    'agentDefaultModel and sessions are row-level (ordering only)',
  )
})

test('the patch does not override a row the composition lacks', () => {
  const patch = readFileSync(new URL('../cordis.patch.yml', import.meta.url), 'utf8')
  assert.doesNotMatch(patch, /^-\s*id:\s*hmr\s*$/mu, 'the 0.2.x composition has no hmr row')
  assert.match(patch, /name: '@nas200-timmy\/dsh-inproc-tui'/u)
})

// `start()` cannot run here: it needs a TTY on both ends and a harness in the
// process. Everything it does after opening the session is exercised in
// `test/app.test.js`; this is only the one line that connects the two — a launch
// asked to resume must put the log back on screen, not just print a banner.
test('a launch asked to resume replays the log it resumed', () => {
  const app = readFileSync(new URL('../src/app.js', import.meta.url), 'utf8')
  const start = /async start\(\) \{[\s\S]*?\n  \}/u.exec(app)
  assert.ok(start !== null, 'the startup path is still there')
  assert.match(start[0], /if \(resumed\) this\.replayHistory\(this\.session\.history\(\)\)/u)
})

test('a localized harness text is one language, never an object', () => {
  const reason = { en: 'Bash needs approval', zh: 'bash 需要确认' }
  assert.equal(textOf(reason, 'zh'), 'bash 需要确认')
  assert.equal(textOf(reason, 'en'), 'Bash needs approval')
  assert.equal(textOf(reason, 'fr'), 'Bash needs approval', 'unknown language falls back to a translation')
  assert.equal(textOf('plain', 'zh'), 'plain')
  assert.equal(textOf(null, 'zh'), '')
  assert.equal(textOf({}, 'zh'), '')
  assert.equal(textOf({ zh: '  ', en: 'only english' }, 'zh'), 'only english',
    'a blank translation is not a translation')
})
