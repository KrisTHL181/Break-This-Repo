/**
 * Unit tests for the pieces that are pure functions: text geometry, the composer,
 * key decoding, markdown and the i18n tables. The terminal loop itself is covered
 * by the protocol probe and the PTY runs; these are the parts a regression can
 * hide in, and they run offline with `node --test`.
 */

import { test } from 'node:test'
import assert from 'node:assert/strict'

import { plainWidth, stripAnsi, truncateStyled, visibleWidth, wrapStyled } from '../../shared/term/text.js'
import { Composer, layoutText } from '../../shared/ui/composer.js'
import { createKeyDecoder } from '../../shared/term/keys.js'
import { Screen } from '../../shared/term/screen.js'
import { renderMarkdown } from '../../shared/term/markdown.js'
import { createTheme } from '../../shared/term/theme.js'
import { applyEditKey } from '../../shared/ui/edit.js'
import { renderBlock } from '../../shared/ui/blocks.js'
import { createI18n, detectLanguage, TABLES } from '../../shared/i18n.js'
import { renderFrame } from '../../shared/ui/render.js'
import { createInitialState } from '../src/ui/app.js'

const theme = createTheme({ color: false })

test('wide characters count as two cells', () => {
  assert.equal(plainWidth('中文'), 4)
  assert.equal(plainWidth('a中b'), 4)
  assert.equal(plainWidth('😀'), 2)
  assert.equal(visibleWidth('\x1b[31m红\x1b[0m'), 2)
})

test('wrapping never exceeds the width and keeps words', () => {
  const lines = wrapStyled('the quick brown fox jumps over the lazy dog', 12)
  for (const line of lines) assert.ok(visibleWidth(line) <= 12, line)
  assert.equal(lines.join(' ').replace(/\s+/g, ' '), 'the quick brown fox jumps over the lazy dog')
})

test('wrapping carries colour across the break', () => {
  const styled = '\x1b[31m' + 'aaaa '.repeat(6) + '\x1b[0m'
  const lines = wrapStyled(styled, 10)
  assert.ok(lines.length > 1)
  assert.ok(lines[1].startsWith('\x1b[31m'), 'continuation keeps the colour')
  for (const line of lines) assert.ok(visibleWidth(line) <= 10)
})

test('truncation respects the cell budget', () => {
  assert.equal(truncateStyled('abcdef', 4), 'abc…')
  assert.equal(truncateStyled('中文中文', 5), '中文…')
  assert.equal(truncateStyled('abc', 10), 'abc')
})

test('the composer inserts, deletes and moves by cells', () => {
  const composer = new Composer()
  composer.insert('hello 中文')
  assert.equal(composer.text, 'hello 中文')
  composer.move('left', 40)
  composer.backspace()
  assert.equal(composer.text, 'hello 文')
  composer.moveLineEdge('home', 40)
  assert.equal(composer.caret, 0)
  composer.deleteToEnd()
  assert.equal(composer.text, '')
})

test('the composer wraps long lines and keeps the caret inside', () => {
  const composer = new Composer({ text: 'x'.repeat(50) })
  const view = composer.view(20)
  assert.ok(view.total >= 3)
  assert.equal(view.caret.row, view.total - 1)
  assert.ok(view.caret.column <= 20)
})

test('layoutText breaks at spaces and keeps offsets', () => {
  const lines = layoutText('one two three', 8)
  assert.equal(lines[0].display, 'one two')
  assert.equal(lines[1].display, 'three')
  assert.equal('one two three'.slice(lines[1].start), 'three')
})

test('history recalls and restores the draft', () => {
  const composer = new Composer()
  composer.setText('first')
  composer.submit()
  composer.setText('draft')
  composer.recall('up')
  assert.equal(composer.text, 'first')
  composer.recall('down')
  assert.equal(composer.text, 'draft')
})

function decodeAll(chunks) {
  const events = []
  const decoder = createKeyDecoder((event) => events.push(event))
  for (const chunk of chunks) decoder.feed(chunk)
  return events
}

test('enter is carriage return in raw mode', () => {
  // A run of printable bytes arrives as one `char` event; the UI inserts the text.
  const events = decodeAll(['hi\r'])
  assert.deepEqual(events.map((event) => event.name), ['char', 'enter'])
  assert.equal(events[0].text, 'hi')
})

test('ctrl+j is a newline and arrows are named keys', () => {
  const events = decodeAll(['\n\x1b[A\x1b[B\x1b[C\x1b[D'])
  assert.deepEqual(events.map((event) => event.name), ['enter', 'up', 'down', 'right', 'left'])
  assert.equal(events[0].ctrl, true)
})

test('shift+enter arrives as CSI-u and as alt+enter', () => {
  const viaCsiU = decodeAll(['\x1b[13;2u'])
  assert.equal(viaCsiU[0].name, 'enter')
  assert.equal(viaCsiU[0].shift, true)
  const viaAlt = decodeAll(['\x1b\r'])
  assert.equal(viaAlt[0].name, 'enter')
  assert.equal(viaAlt[0].alt, true)
})

test('bracketed paste stays literal text', () => {
  const events = decodeAll(['\x1b[200~a\rb\x1b[201~'])
  assert.equal(events.length, 1)
  assert.equal(events[0].name, 'paste')
  assert.equal(events[0].text, 'a\rb')
})

test('markdown renders headings, code fences and lists', () => {
  const lines = renderMarkdown('# Title\n\n- one\n- two\n\n```js\nconst x = 1\n```', 40, theme)
  const text = lines.map(stripAnsi).join('\n')
  assert.match(text, /Title/)
  assert.match(text, /• one/)
  assert.match(text, /js/)
  assert.match(text, /const x = 1/)
})

test('an unclosed fence still renders as code', () => {
  const lines = renderMarkdown('```sh\necho hi', 30, theme)
  assert.ok(lines.some((line) => stripAnsi(line).includes('echo hi')))
})

test('both language tables define the same keys', () => {
  assert.deepEqual(Object.keys(TABLES.en).sort(), Object.keys(TABLES.zh).sort())
})

test('language detection follows the locale and the override', () => {
  assert.equal(detectLanguage({ LANG: 'zh_CN.UTF-8' }), 'zh')
  assert.equal(detectLanguage({ LANG: 'en_US.UTF-8' }), 'en')
  assert.equal(detectLanguage({ LANG: 'zh_CN.UTF-8', HSDL_TUI_LANG: 'en' }), 'en')
})

test('a frame is exactly the requested size', () => {
  const state = createInitialState({
    cwd: '/workspace',
    composerText: 'hello',
    blocks: [{ type: 'user', text: 'hi', rev: 0 }, { type: 'assistant', text: 'hello there', rev: 0 }],
  })
  const frame = renderFrame(state, { width: 80, height: 24, theme, i18n: createI18n('en'), clock: 0 })
  assert.equal(frame.rows.length, 24)
  for (const row of frame.rows) assert.equal(visibleWidth(row), 80, row)
})

test('the caret sits inside the composer border', () => {
  const state = createInitialState({ cwd: '/workspace', composerText: 'abc' })
  const frame = renderFrame(state, { width: 60, height: 20, theme, i18n: createI18n('en'), clock: 0 })
  assert.ok(frame.cursor !== null)
  assert.ok(frame.cursor.row < 20)
  assert.ok(frame.cursor.column < 60)
})

test('the caret sits after the last character, including wide CJK', () => {
  for (const text of ['', 'abc', '中文', 'ab中cd']) {
    const state = createInitialState({ cwd: '/workspace', composerText: text })
    const frame = renderFrame(state, { width: 60, height: 20, theme, i18n: createI18n('en'), clock: 0 })
    assert.ok(frame.cursor !== null)
    // `│ ` + `❯ ` is four cells; the caret follows the text by its display width.
    assert.equal(frame.cursor.column, 4 + plainWidth(text), `text ${JSON.stringify(text)}`)
  }
})

test('a dialog is composited without changing the frame size', () => {
  const state = createInitialState({ cwd: '/workspace' })
  state.overlay = {
    kind: 'picker',
    title: 'models',
    items: [{ label: 'a' }, { label: 'b', description: 'second' }],
    index: 0,
    hint: 'ok',
  }
  const frame = renderFrame(state, { width: 70, height: 22, theme, i18n: createI18n('en'), clock: 0 })
  assert.equal(frame.rows.length, 22)
  for (const row of frame.rows) assert.equal(visibleWidth(row), 70)
  assert.equal(frame.cursor, null, 'no caret while a dialog is open')
  assert.ok(frame.rows.some((row) => stripAnsi(row).includes('models')))
})

test('the status row carries the approval mode and the session id', () => {
  const state = createInitialState({ cwd: '/workspace', sessionId: 'session-1234abcd' })
  state.approval = { mode: 'full', autoApproved: 3 }
  const frame = renderFrame(state, { width: 90, height: 24, theme, i18n: createI18n('en'), clock: 0 })
  const bottom = stripAnsi(frame.rows.at(-1))
  assert.match(bottom, /#1234abcd/u, 'the id without its useless session- prefix')
  assert.doesNotMatch(bottom, /#sessio/u)
  assert.match(bottom, /yolo/u, 'and so is the mode that decides whether it asks')
})

test('a working turn shows the live size, not just the clock', () => {
  const state = createInitialState({ cwd: '/workspace' })
  state.status = 'working'
  state.activity = { tool: null, startedAt: 0 }
  state.blocks = [{ type: 'assistant', text: '', full: 'x'.repeat(4000), streaming: true, rev: 0 }]
  const frame = renderFrame(state, { width: 90, height: 24, theme, i18n: createI18n('en'), clock: 1500 })
  const body = frame.rows.map(stripAnsi).join('\n')
  assert.match(body, /~1\.0k/u, 'a rough token count while the answer streams')
})

test('the idle hint line names the keys a person actually needs', () => {
  const state = createInitialState({ cwd: '/workspace' })
  const frame = renderFrame(state, { width: 96, height: 24, theme, i18n: createI18n('en'), clock: 0 })
  const body = frame.rows.map(stripAnsi).join('\n')
  assert.match(body, /\/ commands/u)
  assert.match(body, /ctrl\+j newline/u)
})

test('a numbered dialog prints the shortcut numbers', () => {
  const state = createInitialState({ cwd: '/workspace' })
  state.overlay = {
    kind: 'permission',
    title: 'permission',
    toolName: 'bash',
    detail: 'rm -rf build',
    options: [
      { label: 'Run', value: 'allowed-once' },
      { label: 'Run for this session', value: 'session-allow' },
      { label: 'Reject', value: 'rejected' },
      { label: 'Reject with a reason', value: 'reject-reason' },
    ],
    index: 0,
  }
  const frame = renderFrame(state, { width: 90, height: 24, theme, i18n: createI18n('en'), clock: 0 })
  const body = frame.rows.map(stripAnsi)
  const numbered = body.filter((row) => /[1-4]\. /u.test(row))
  assert.equal(numbered.length, 4, 'every choice is numbered')
  assert.ok(body.some((row) => /1\. Run/u.test(row)), 'the number sits in front of the label')
})

test('Tab is a key, not a dropped control byte', () => {
  // The decoder used to have no entry for 0x09, so Tab fell through the
  // "unknown control character" branch and completion never ran at all.
  const events = []
  const decoder = createKeyDecoder((event) => events.push(event.name))
  decoder.feed('/mo\t')
  assert.deepEqual(events, ['char', 'tab'])
})

test('the completion candidates sit below the input box', () => {
  const state = createInitialState({ cwd: '/workspace', composerText: '/yo' })
  state.completion = { start: 0, end: 3, options: ['/yolo'], index: 0 }
  const frame = renderFrame(state, { width: 70, height: 16, theme, i18n: createI18n('en'), clock: 0 })
  const rows = frame.rows.map(stripAnsi)
  const bottom = rows.findIndex((row) => row.startsWith('╰'))
  const candidates = rows.findIndex((row) => /1\. \/yolo/u.test(row))
  assert.ok(bottom >= 0 && candidates > bottom, `box bottom ${bottom}, candidates ${candidates}`)
  assert.ok(candidates < rows.length - 1, 'and above the status row')
})

test('the command menu sits below the input, not in a dialog', () => {
  const state = createInitialState({ cwd: '/workspace', composerText: '/' })
  state.palette = {
    items: [
      { name: 'help', label: '/help', description: 'list commands and keys' },
      { name: 'model', label: '/model', description: 'choose this session model' },
    ],
    index: 0,
  }
  const frame = renderFrame(state, { width: 80, height: 16, theme, i18n: createI18n('en'), clock: 0 })
  const rows = frame.rows.map(stripAnsi)
  const bottom = rows.findIndex((row) => row.startsWith('╰'))
  const menu = rows.findIndex((row) => /1\. \/help/u.test(row))
  assert.ok(bottom >= 0 && menu > bottom, `box bottom ${bottom}, menu ${menu}`)
  assert.ok(menu < rows.length - 1, 'and above the status row')
  assert.ok(rows[menu].startsWith('  ❯ 1. /help'), rows[menu])
  assert.ok(rows.some((row) => row.includes('enter run')), 'the menu has its own key hint')
  assert.ok(frame.cursor !== null, 'the composer keeps the caret while the menu is open')
})

test('an idle frame writes nothing the second time', () => {
  // A no-op paint is what keeps a browser terminal's input method alive: every
  // write repositions its composition helper.
  const writes = []
  const screen = new Screen({ write: (text) => writes.push(text), columns: 60, rows: 8 }, {})
  screen.enter()
  const rows = ['a'.repeat(60), ...Array.from({ length: 7 }, () => ' '.repeat(60))]
  screen.paint(rows, { row: 0, column: 1 })
  const afterFirst = writes.length
  screen.paint(rows, { row: 0, column: 1 })
  assert.equal(writes.length, afterFirst, 'an identical frame is not written again')
})

test('the editing keys the inline menu passes on are exactly the text ones', () => {
  const composer = new Composer({ maxLines: 8 })
  composer.setText('/he')
  // The palette owns arrows, page keys, enter and escape; this is everything
  // else it must hand to the composer.
  assert.equal(applyEditKey(composer, { name: 'char', text: 'l' }, 40), true)
  assert.equal(composer.text, '/hel')
  assert.equal(applyEditKey(composer, { name: 'backspace' }, 40), true)
  assert.equal(composer.text, '/he')
  assert.equal(applyEditKey(composer, { name: 'paste', text: 'lp\r\nme' }, 40), true)
  assert.equal(composer.text, '/help\nme')
  assert.equal(applyEditKey(composer, { name: 'ctrl+u' }, 40), true)
  assert.equal(composer.text.length < '/help\nme'.length, true)
  assert.equal(applyEditKey(composer, { name: 'enter' }, 40), false)
  assert.equal(applyEditKey(composer, { name: 'down' }, 40), false)

})

test('the startup banner has the whale and the Kimi-style keys', () => {
  const i18n = createI18n('en')
  const rows = renderBlock({
    type: 'banner',
    tag: 'dsh-inproc-tui',
    version: 'v0.9.0',
    subtitle: 'DeepSeek Harness in your terminal',
    hint: 'Type a message to start · /help for commands',
    cwd: '/workspace',
    model: 'kimi-k2',
    mode: 'ask',
    planMode: true,
    sessionId: 'abcdef123456',
  }, 82, theme, i18n, { expandTools: false, spinner: '.' })
  const text = rows.join('\n')
  assert.match(text, /[▄▀█]/u, 'the pixel whale is drawn')
  assert.match(text, /kimi-k2/u, 'the model is in the banner')
  assert.match(text, /plan/u, 'plan mode is in the banner')
  assert.match(text, /shift\+tab plan/u, 'the keyboard hint is in the banner')
  assert.match(text, /dsh-inproc-tui/u, 'the plugin name is in the banner')
})

test('the startup whale uses the SVG palette', () => {
  const colorTheme = createTheme({ color: true, mode: 'dark' })
  const rows = renderBlock({
    type: 'banner',
    tag: 'HDSL-web',
    version: 'v0.2.0',
    subtitle: 'DeepSeek Harness in your terminal',
    mode: 'ask',
  }, 82, colorTheme, createI18n('en'), { expandTools: false, spinner: '.' })
  const text = rows.join('\n')
  assert.match(text, /38;2;20;38;96/u, 'outline #142660')
  assert.match(text, /38;2;78;111;255/u, 'body #4e6fff')
  assert.match(text, /38;2;190;225;255/u, 'highlight #bee1ff')
})

test('a pipe table becomes columns instead of a paragraph', () => {
  const lines = renderMarkdown('| file | state |\n| --- | --- |\n| a.js | ok |\n| b.js | bad |', 50, theme)
  const text = lines.join('\n')
  assert.match(text, /file/u)
  assert.match(text, /state/u)
  assert.match(text, /a\.js/u)
  assert.match(text, /bad/u)
  assert.equal(text.includes('| --- |'), false, 'the separator row is not printed')
})

test('a tool card follows the global expand toggle', () => {  const i18n = createI18n('en')
  const render = (options) => renderBlock({
    type: 'tool',
    name: 'read',
    input: { path: 'src/index.js' },
    output: Array.from({ length: 40 }, (_unused, index) => `line ${index}`).join('\n'),
    status: 'completed',
  }, 60, theme, i18n, options)
  const collapsed = render({ expandTools: false, spinner: '.' })
  const expanded = render({ expandTools: true, spinner: '.' })
  assert.ok(expanded.length > collapsed.length, 'the toggle shows more of the output')
  // A card that pinned itself closed used to ignore the toggle; that field is
  // what the terminals no longer set. The output has to be longer than the
  // collapsed preview for the difference to show at all.
  const long = Array.from({ length: 40 }, (_unused, index) => `line ${index}`).join('\n')
  const renderOne = (extra) => renderBlock({
    type: 'tool', name: 'read', input: { path: 'x' }, output: long, status: 'completed', ...extra,
  }, 60, theme, i18n, { expandTools: true, spinner: '.' })
  assert.ok(renderOne({}).length > renderOne({ expanded: false }).length, 'expanded:false overrides the toggle')
})
