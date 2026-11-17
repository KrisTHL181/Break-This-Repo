/**
 * Markdown to styled terminal lines.
 *
 * The renderer is written for transcripts that are still arriving: it never
 * needs a closing fence to draw a block, it re-renders cheaply from the raw
 * string, and it keeps each construct inside the pane width. It covers what a
 * coding agent actually emits — headings, emphasis, inline code, fenced code,
 * lists, quotes, links, rules — and deliberately leaves out anything that would
 * need a layout engine.
 *
 * @module term/markdown
 */

import { indentBlock, visibleWidth, wrapStyled } from './text.js'

const HEADING_RE = /^(#{1,6})\s+(.*)$/
const FENCE_RE = /^\s*(```+|~~~+)\s*([\w+-]*)\s*$/
const LIST_RE = /^(\s*)([-*+]|\d{1,3}[.)])\s+(.*)$/
const QUOTE_RE = /^\s*>\s?(.*)$/
const RULE_RE = /^\s*(?:-{3,}|\*{3,}|_{3,})\s*$/
const TABLE_SEPARATOR = /^\s*\|?\s*:?-{2,}:?\s*(\|\s*:?-{2,}:?\s*)+\|?\s*$/u

const KEYWORDS = new Set([
  'const', 'let', 'var', 'function', 'return', 'if', 'else', 'for', 'while', 'class', 'new',
  'import', 'from', 'export', 'default', 'async', 'await', 'try', 'catch', 'finally', 'throw',
  'typeof', 'instanceof', 'interface', 'type', 'enum', 'extends', 'implements', 'public',
  'private', 'protected', 'static', 'final', 'void', 'true', 'false', 'null', 'undefined',
  'package', 'public', 'def', 'elif', 'in', 'not', 'and', 'or', 'with', 'as', 'lambda', 'yield',
  'switch', 'case', 'break', 'continue', 'struct', 'impl', 'fn', 'mut', 'pub', 'use', 'match',
])

/**
 * Renders markdown into styled, width-wrapped lines.
 *
 * @param {string} source raw markdown
 * @param {number} width pane width in cells
 * @param {object} theme theme from {@link createTheme}
 * @param {{invertCode?: boolean, compact?: boolean}} [options]
 * @returns {string[]}
 */
export function renderMarkdown(source, width, theme, options = {}) {
  const lines = []
  const text = String(source ?? '')
  const rows = text.split('\n')
  let fence = null
  let paragraph = []

  const flushParagraph = () => {
    if (paragraph.length === 0) return
    const joined = paragraph.join(' ')
    for (const line of wrapStyled(inline(joined, theme), width)) lines.push(line)
    paragraph = []
  }

  for (let index = 0; index < rows.length; index += 1) {
    const row = rows[index]
    const fenceMatch = FENCE_RE.exec(row)
    if (fenceMatch) {
      if (fence === null) {
        flushParagraph()
        fence = { marker: fenceMatch[1], language: fenceMatch[2] || '' }
        lines.push(theme.fg('textDim', `  ${fence.language || 'text'}`))
      } else {
        fence = null
        lines.push('')
      }
      continue
    }
    if (fence !== null) {
      lines.push(...codeLine(row, fence.language, width, theme))
      continue
    }
    if (row.trim() === '') {
      flushParagraph()
      if (lines.length > 0 && lines[lines.length - 1] !== '') lines.push('')
      continue
    }
    // A table is a header row followed by a separator row. Without the
    // separator it is just prose (or a pipe in a shell snippet).
    if (row.includes('|') && index + 1 < rows.length && TABLE_SEPARATOR.test(rows[index + 1])) {
      flushParagraph()
      const table = [tableCells(row)]
      index += 2
      while (index < rows.length && rows[index].includes('|') && rows[index].trim() !== '') {
        table.push(tableCells(rows[index]))
        index += 1
      }
      index -= 1
      const rendered = renderTable(table, width, theme)
      if (lines.length > 0 && lines[lines.length - 1] !== '') lines.push('')
      lines.push(...rendered)
      continue
    }
    if (RULE_RE.test(row)) {
      flushParagraph()
      lines.push(theme.fg('border', '─'.repeat(Math.max(1, width))))
      continue
    }
    const heading = HEADING_RE.exec(row)
    if (heading) {
      flushParagraph()
      const level = heading[1].length
      const title = inline(heading[2], theme)
      const styled = level <= 2 ? theme.strong('textStrong', title) : theme.strong('text', title)
      if (lines.length > 0 && lines[lines.length - 1] !== '') lines.push('')
      for (const line of wrapStyled(styled, width)) lines.push(line)
      continue
    }
    const quote = QUOTE_RE.exec(row)
    if (quote) {
      flushParagraph()
      const body = wrapStyled(inline(quote[1], theme), Math.max(1, width - 2))
      for (const line of body) lines.push(theme.fg('textMuted', '│ ') + theme.fg('textDim', line))
      continue
    }
    const list = LIST_RE.exec(row)
    if (list) {
      flushParagraph()
      const marker = list[2]
      const bullet = /^\d/.test(marker) ? `${marker} ` : '• '
      const body = wrapStyled(inline(list[3], theme), Math.max(1, width - 2))
      body.forEach((line, index) => {
        lines.push(index === 0 ? theme.fg('primary', bullet) + line : '  ' + line)
      })
      continue
    }
    paragraph.push(row.trim())
  }
  flushParagraph()
  while (lines.length > 0 && lines[lines.length - 1] === '') lines.pop()
  return options.compact ? lines : lines
}

/**
 * Styled body of one fenced-code line.
 * @private
 */
function codeLine(row, language, width, theme) {
  const body = row.replace(/\t/g, '  ')
  const highlighted = highlight(body, language, theme)
  const gutter = theme.fg('border', '│ ')
  return wrapStyled(gutter + highlighted, width).map((line, index) =>
    index === 0 ? line : theme.fg('border', '│ ') + line.slice(0),
  )
}

const STRING_RE = /("(?:\\.|[^"\\])*"|'(?:\\.|[^'\\])*'|`(?:\\.|[^`\\])*`)/
const COMMENT_RE = /(\/\/.*$|#.*$)/

/**
 * A deliberately small highlighter: strings, comments, numbers and keywords.
 * It is not a parser, and it never needs one for a transcript pane.
 * @private
 */
function highlight(line, language, theme) {
  if (line.trim() === '') return line
  if (language === '' || language === 'text' || language === 'plain') return theme.fg('text', line)
  const parts = []
  let rest = line
  const commentAt = findComment(rest, language)
  if (commentAt >= 0) {
    parts.push(tokenize(rest.slice(0, commentAt), theme))
    parts.push(theme.fg('textMuted', rest.slice(commentAt)))
    return parts.join('')
  }
  return tokenize(rest, theme)
}

function findComment(line, language) {
  if (language === 'python' || language === 'py' || language === 'sh' || language === 'bash' || language === 'yaml' || language === 'yml' || language === 'toml' || language === 'makefile') {
    const index = line.indexOf('#')
    if (index < 0) return -1
    if (index > 0 && line[index - 1] === '\\') return -1
    const before = line.slice(0, index)
    // Only treat it as a comment when not inside a string.
    if ((before.match(/"/g) ?? []).length % 2 === 1) return -1
    if ((before.match(/'/g) ?? []).length % 2 === 1) return -1
    return index
  }
  const index = line.indexOf('//')
  return index < 0 ? -1 : index
}

function tokenize(source, theme) {
  let out = ''
  let index = 0
  while (index < source.length) {
    const rest = source.slice(index)
    const string = STRING_RE.exec(rest)
    if (string && string.index === 0) {
      out += theme.fg('diffAdded', string[0])
      index += string[0].length
      continue
    }
    const word = /^[A-Za-z_$][\w$]*/.exec(rest)
    if (word) {
      const value = word[0]
      out += KEYWORDS.has(value) ? theme.fg('primary', value) : theme.fg('text', value)
      index += value.length
      continue
    }
    const number = /^(?:0x[\da-f]+|\d+(?:\.\d+)?)/i.exec(rest)
    if (number) {
      out += theme.fg('accent', number[0])
      index += number[0].length
      continue
    }
    out += theme.fg('text', source[index])
    index += 1
  }
  return out
}

/**
 * Inline markdown: code spans, bold, italic, strikethrough and links.
 *
 * Applied with one left-to-right scan so nested markers cannot fight over the
 * same characters, which is the failure mode of chained `replace` calls.
 *
 * @param {string} source
 * @param {object} theme
 * @returns {string}
 */
export function inline(source, theme) {
  let out = ''
  let index = 0
  const text = String(source)
  while (index < text.length) {
    const rest = text.slice(index)
    const code = /^`([^`]+)`/.exec(rest)
    if (code) {
      out += theme.fg('accent', code[1])
      index += code[0].length
      continue
    }
    const bold = /^\*\*([\s\S]+?)\*\*/.exec(rest) ?? /^__([\s\S]+?)__/.exec(rest)
    if (bold) {
      out += theme.strong('textStrong', inline(bold[1], theme))
      index += bold[0].length
      continue
    }
    const italic = /^\*([^*\n]+)\*/.exec(rest) ?? /^_([^_\n]+)_/.exec(rest)
    if (italic) {
      out += theme.italic(inline(italic[1], theme))
      index += italic[0].length
      continue
    }
    const strike = /^~~([\s\S]+?)~~/.exec(rest)
    if (strike) {
      out += theme.dim(inline(strike[1], theme))
      index += strike[0].length
      continue
    }
    const link = /^\[([^\]]+)\]\(([^)\s]+)(?:\s+"[^"]*")?\)/.exec(rest)
    if (link) {
      out += theme.underline(theme.fg('primary', link[1]))
      if (link[2] !== link[1]) out += theme.fg('textMuted', ` (${link[2]})`)
      index += link[0].length
      continue
    }
    const autolink = /^<(https?:\/\/[^>\s]+)>/.exec(rest)
    if (autolink) {
      out += theme.underline(theme.fg('primary', autolink[1]))
      index += autolink[0].length
      continue
    }
    const ch = String.fromCodePoint(text.codePointAt(index))
    out += theme.fg('text', ch)
    index += ch.length
  }
  return out
}

/**
 * Renders a unified diff with gutters and colours.
 *
 * @param {string} patch unified diff text
 * @param {number} width
 * @param {object} theme
 * @param {{maxLines?: number}} [options]
 * @returns {string[]}
 */
export function renderDiff(patch, width, theme, options = {}) {
  const limit = options.maxLines ?? 60
  const rows = String(patch ?? '').split('\n')
  const out = []
  for (const row of rows) {
    if (out.length >= limit) {
      out.push(theme.fg('textMuted', `… ${rows.length - limit} more lines`))
      break
    }
    if (row.startsWith('+++') || row.startsWith('---') || row.startsWith('@@') || row.startsWith('diff ') || row.startsWith('index ')) {
      out.push(theme.fg('diffMeta', row))
      continue
    }
    if (row.startsWith('+')) {
      out.push(theme.fg('diffAdded', row))
      continue
    }
    if (row.startsWith('-')) {
      out.push(theme.fg('diffRemoved', row))
      continue
    }
    out.push(theme.fg('textDim', row))
  }
  return out.map((line) => line.length === 0 ? line : line)
}

/** Splits one pipe-table row into trimmed cells. */
function tableCells(row) {
  let text = String(row).trim()
  if (text.startsWith('|')) text = text.slice(1)
  if (text.endsWith('|')) text = text.slice(0, -1)
  return text.split('|').map((cell) => cell.trim())
}

/**
 * Lays out `rows` of two-column cells into a table, honouring wide characters.
 *
 * @param {string[][]} rows first row is the header
 * @param {number} width
 * @param {object} theme
 * @returns {string[]}
 */
export function renderTable(rows, width, theme) {
  if (rows.length === 0) return []
  const columns = Math.max(...rows.map((row) => row.length))
  const widths = new Array(columns).fill(0)
  for (const row of rows) {
    row.forEach((cell, index) => {
      widths[index] = Math.max(widths[index], visibleWidth(cell))
    })
  }
  const budget = Math.max(8, width)
  const total = widths.reduce((sum, value) => sum + value + 2, 0)
  const scale = total > budget ? budget / total : 1
  const lines = rows.map((row, rowIndex) => {
    const cells = []
    for (let index = 0; index < columns; index += 1) {
      const value = row[index] ?? ''
      const budgetCell = Math.max(2, Math.floor(widths[index] * scale))
      const style = rowIndex === 0 ? (text) => theme.strong('textStrong', text) : (text) => theme.fg('text', text)
      cells.push(pad(value, budgetCell, style))
    }
    return cells.join('  ')
  })
  return lines
}

function pad(value, width, style) {
  const text = String(value)
  const room = Math.max(0, width - visibleWidth(text))
  return style(text) + ' '.repeat(room)
}

/** Indents markdown lines by `spaces`, keeping blank lines empty. */
export function indentMarkdown(lines, spaces) {
  return indentBlock(lines, ' '.repeat(spaces))
}
