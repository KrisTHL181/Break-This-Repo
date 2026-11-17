/**
 * Tab completion: the token rules, the candidate source, and the cycling.
 *
 * The module is deliberately free of `fs` and of any app state, so the tests
 * here pin the arrangement and let the two terminals be tested against their own
 * sources (in `app.test.js`).
 */

import { test } from 'node:test'
import assert from 'node:assert/strict'

import {
  applyCompletion,
  completionFor,
  describeCandidates,
  nextCandidate,
  tokenAt,
} from '../../shared/ui/complete.js'

test('the token under the caret is the word being typed', () => {
  assert.deepEqual(tokenAt('/mod', 4), { token: '/mod', start: 0, end: 4, index: 0, words: [] })
  const argument = tokenAt('/effort hi', 10)
  assert.equal(argument.token, 'hi')
  assert.equal(argument.index, 1)
  assert.deepEqual(argument.words, ['/effort'])
  const middle = tokenAt('/model ac/sonnet rest', 9)
  assert.equal(middle.token, 'ac', 'the caret decides, not the end of the line')
})

test('commands complete from the first word only', () => {
  const resolve = ({ index }) => (index === 0 ? ['/model', '/effort', '/yolo'] : ['should-not-be-used'])
  const completion = completionFor({ text: '/mo', caret: 3, resolve })
  assert.deepEqual(completion.options, ['/model'])
  assert.deepEqual({ start: completion.start, end: completion.end }, { start: 0, end: 3 })
})

test('arguments complete from the second word on', () => {
  const seen = []
  const completion = completionFor({
    text: '/yolo cr',
    caret: 8,
    resolve: (request) => {
      seen.push(request)
      return ['ask', 'critical', 'full']
    },
  })
  assert.deepEqual(completion.options, ['critical'])
  assert.equal(seen[0].index, 1)
  assert.equal(seen[0].command, 'yolo')
})

test('the prefix is respected and the exact text is not offered back', () => {
  const resolve = () => ['/model', '/models', '/effort']
  assert.deepEqual(completionFor({ text: '/mod', caret: 4, resolve }).options, ['/model', '/models'])
  assert.deepEqual(completionFor({ text: '/model', caret: 6, resolve }).options, ['/models'],
    'the exact text is not offered back, a longer name still is')
  assert.equal(completionFor({ text: '/effort', caret: 7, resolve: () => ['/effort'] }), null,
    'with only the exact match there is nothing left to add')
})

test('a source that throws is "no candidates", not an error', () => {
  const resolve = () => {
    throw new Error('no persistence mounted')
  }
  assert.equal(completionFor({ text: '/resume a', caret: 9, resolve }), null)
})

test('applying a candidate replaces the token and moves the caret', () => {
  const applied = applyCompletion({ text: '/mod is good', caret: 4 }, { start: 0, end: 4 }, '/model')
  assert.deepEqual(applied, { text: '/model is good', caret: 6 })
})

test('repeated Tab cycles, and a new token starts over', () => {
  const fresh = { start: 0, end: 3, options: ['/model', '/models'] }
  const first = nextCandidate(null, fresh)
  assert.deepEqual(first, { option: '/model', index: 0 })
  const second = nextCandidate({ ...fresh, index: first.index }, fresh)
  assert.deepEqual(second, { option: '/models', index: 1 })
  const wrapped = nextCandidate({ ...fresh, index: second.index }, fresh)
  assert.deepEqual(wrapped, { option: '/model', index: 0 })

  const moved = nextCandidate({ ...fresh, index: 1 }, { start: 0, end: 4, options: ['/model'] })
  assert.equal(moved.index, 0, 'a different token is a different cycle')
})

test('the candidate line lists at most six and fits the width', () => {
  assert.equal(describeCandidates(null, 40), '')
  const line = describeCandidates({ options: ['a', 'b', 'c'], index: 0 }, 80)
  assert.match(line, /1\. a/u)
  assert.match(line, /3\. c/u)
  const narrow = describeCandidates({ options: ['abcdefghij', 'klmnopqrst', 'uvwxyz'], index: 0 }, 12)
  assert.equal(narrow.length <= 12, true, narrow)
})
