/**
 * The persistent allow/deny rules: where the file lives, and what it may say.
 *
 * The location matters as much as the matching: a rules file outside the
 * instance home is shared by every instance of the same OS user and disappears
 * with the container image.
 */

import { test } from 'node:test'
import assert from 'node:assert/strict'
import { mkdtempSync, rmSync, writeFileSync } from 'node:fs'
import { homedir, tmpdir } from 'node:os'
import { join } from 'node:path'

import {
  emptyRules,
  loadRules,
  ruleFor,
  rulesFile,
  saveRules,
  withRule,
  withoutRule,
} from '../../shared/approval/rules.js'

test('the rules file follows the instance home, not the OS user', () => {
  assert.equal(rulesFile({ DSH_HOME: '/srv/hdsl/instances/a/home' }),
    join('/srv/hdsl/instances/a/home', '.hdsl-tui', 'permissions.json'))
  assert.equal(rulesFile({}), join(homedir(), '.hdsl-tui', 'permissions.json'),
    'a terminal started by hand has no instance home')
  assert.equal(rulesFile({ DSH_HOME: '/x', HDSL_TUI_RULES: '/tmp/r.json' }), '/tmp/r.json',
    'the override wins, for tests and for anyone who wants it elsewhere')
})

test('a rule set round-trips, and deny wins over allow', () => {
  const directory = mkdtempSync(join(tmpdir(), 'dsh-rules-'))
  const file = join(directory, 'permissions.json')
  try {
    assert.deepEqual(loadRules(file), emptyRules(), 'a missing file grants nothing')

    let rules = withRule(emptyRules(), 'allow', 'bash git status')
    rules = withRule(rules, 'allow', 'read /workspace/a')
    saveRules(rules, file)
    assert.deepEqual(loadRules(file), { allow: ['bash git status', 'read /workspace/a'], deny: [] })

    rules = withRule(rules, 'deny', 'bash git status')
    saveRules(rules, file)
    assert.equal(ruleFor(loadRules(file), 'bash git status'), 'deny', 'a key is in one list only')
    assert.equal(ruleFor(loadRules(file), 'read /workspace/a'), 'allow')
    assert.equal(ruleFor(loadRules(file), 'bash rm -rf /'), '')
  } finally {
    rmSync(directory, { recursive: true, force: true })
  }
})

test('a malformed file is an empty rule set, never a grant', () => {
  const directory = mkdtempSync(join(tmpdir(), 'dsh-rules-'))
  const file = join(directory, 'permissions.json')
  try {
    saveRules({ allow: [], deny: [] }, file)
    writeFileSync(file, 'not json')
    assert.deepEqual(loadRules(file), emptyRules())
  } finally {
    rmSync(directory, { recursive: true, force: true })
  }
})

test('removing a rule leaves the other list alone', () => {
  const rules = { allow: ['a', 'b'], deny: ['c'] }
  assert.deepEqual(withoutRule(rules, 'b'), { allow: ['a'], deny: ['c'] })
  const again = withRule(rules, 'allow', 'a')
  assert.deepEqual([...again.allow].sort(), ['a', 'b'], 'the key stays in exactly one list')
  assert.deepEqual(again.deny, ['c'])
})
