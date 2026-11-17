/**
 * Transcript blocks.
 *
 * One function per kind of transcript entry, each turning a block of plain data
 * into styled rows of exactly `width` cells. Keeping them separate from the app
 * state is what lets the renderer cache a block's rows until its content or the
 * width changes — the difference between a smooth transcript and one that
 * re-wraps the whole conversation on every spinner frame.
 *
 * @module ui/blocks
 */

import { padStyled, spread, stripAnsi, truncateStyled, visibleWidth, wrapStyled } from '../term/text.js'
import { renderDiff, renderMarkdown } from '../term/markdown.js'

const BODY_INDENT = '  '
const PREVIEW_LINES = 6
const EXPANDED_LINES = 120

/** Formats a duration the way a person reads it: 120ms, 3.2s, 1m04s. */
export function formatDuration(ms) {
  if (ms === null || ms === undefined || !Number.isFinite(ms)) return ''
  if (ms < 950) return `${Math.max(0, Math.round(ms))}ms`
  const seconds = ms / 1000
  if (seconds < 60) return `${seconds.toFixed(1)}s`
  const minutes = Math.floor(seconds / 60)
  const rest = Math.floor(seconds % 60)
  return `${minutes}m${String(rest).padStart(2, '0')}s`
}

/** Removes terminal escapes and control bytes a tool result may contain. */
export function sanitizeOutput(value) {
  return stripAnsi(String(value ?? ''))
    .replace(/\r\n/g, '\n')
    .replace(/\r/g, '\n')
    .replace(/[\u0000-\u0008\u000b\u000c\u000e-\u001f\u007f]/g, '')
}

/**
 * Paints one block.
 *
 * @param {object} block
 * @param {number} width
 * @param {object} theme
 * @param {object} i18n
 * @param {{spinner: string, expandTools: boolean}} options
 * @returns {string[]}
 */
export function renderBlock(block, width, theme, i18n, options) {
  switch (block.type) {
    case 'banner':
      return banner(block, width, theme, i18n)
    case 'user':
      return user(block, width, theme)
    case 'assistant':
      return assistant(block, width, theme)
    case 'thought':
      return thought(block, width, theme, i18n, options)
    case 'tool':
      return tool(block, width, theme, i18n, options)
    case 'notice':
      return notice(block, width, theme)
    case 'divider':
      return ['', theme.fg('border', '─'.repeat(Math.max(1, width))), '']
    default:
      return [theme.fg('text', String(block.text ?? ''))]
  }
}

const WHALE_ROWS = [
  '                     A          ',
  '                    ABA       A ',
  '                    ABBA     ABA',
  '                    ABBBA  AABBA',
  '                    ABBBBAABBBBA',
  '    AAAAAAAAA        ABBBBBBBBA ',
  '   ABBBBBBBBBAA       ABBBBBBBA ',
  '  ABBBBBBBBBBBBAA     ABBBBBAA  ',
  ' ABBBBBBBBBBBBBBBAA    ABBBA    ',
  'AABBBBBBBBBBBBBBBBBA  ABBBBA    ',
  'ABBBBBBBBBBBBBBBBBBBAABBBBBA    ',
  'ABBBABBBBBBABBBBBBBBBBBBBBBA    ',
  'ABBBABBBBBBABBBBBBBBBBBBBBA     ',
  'ABBBBBBBBBBBBBBBBBBBBBBBBBA     ',
  'ABBBBDDDDDDDBBBBBBBBABBBBA      ',
  'AABDDDDDDDDDDDDBBBBBBABBBA      ',
  ' ACCDDDDDDDDDDDDABBBBAABA       ',
  '  ACCCDDDDDDDDDDABBBBBAA        ',
  '   AACCCDDDDDDCCCABBBBBAA       ',
  '     ACCCCCCCCCCCAABBBBBBA      ',
  '      AAAAAAAAAAA  AAAAAAA      ',
]

/**
 * Frame 0 of the dsh-TUI pixel whale, with the SVG's own colours.
 *
 * ccch1mneyyy/dsh-TUI (MIT License) → lhh010/dsh-ui-whale (BSD-3-Clause) for the
 * original 22 frames. Each plugin's tarball carries a NOTICE with this
 * attribution, because this module ships inside it.
 */
const WHALE_PALETTE = {
  A: '#142660',
  B: '#4e6fff',
  C: '#bee1ff',
  D: '#ffffff',
}

/** The logo wordmark/descriptor/rule colours from the same SVG. */
const LOGO_INK = {
  dark: { wordmark: '#e8e6e0', descriptor: '#abc2ec', rule: '#5e88cc' },
  light: { wordmark: '#263146', descriptor: '#687386', rule: '#abc2ec' },
}

function pixelCell(theme, top, bottom) {
  const topColor = WHALE_PALETTE[top] ?? null
  const bottomColor = WHALE_PALETTE[bottom] ?? null
  if (!theme.color) {
    if (topColor !== null && bottomColor !== null) return '█'
    if (topColor !== null) return '▀'
    if (bottomColor !== null) return '▄'
    return ' '
  }
  if (topColor !== null && bottomColor !== null) {
    if (topColor === bottomColor) return theme.fg(topColor, '█')
    return theme.fgBg(topColor, bottomColor, '▀')
  }
  if (topColor !== null) return theme.fg(topColor, '▀')
  if (bottomColor !== null) return theme.fg(bottomColor, '▄')
  return ' '
}

function whaleLines(theme) {
  const lines = []
  for (let row = 0; row < WHALE_ROWS.length; row += 2) {
    let line = ''
    const top = WHALE_ROWS[row]
    const bottom = WHALE_ROWS[row + 1] ?? ''
    for (let column = 0; column < top.length; column += 1) {
      line += pixelCell(theme, top[column] ?? ' ', bottom[column] ?? ' ')
    }
    lines.push(line.replace(/\s+$/u, ''))
  }
  return lines
}

function banner(block, width, theme, i18n) {
  const art = whaleLines(theme)
  const artWidth = 32
  const ink = theme.mode === 'dark' ? LOGO_INK.dark : LOGO_INK.light
  const info = []
  const tag = theme.fg(ink.wordmark, block.tag ?? 'HDSL-web')
  const version = block.version ? ` ${theme.fg(ink.descriptor, block.version)}` : ''
  info.push(`${tag}${version}`)
  if (block.subtitle) info.push(theme.fg(ink.descriptor, block.subtitle))
  const facts = []
  if (block.model) facts.push(`${i18n.t('banner.model')} ${block.model}`)
  if (block.mode) facts.push(`${i18n.t('banner.mode')} ${i18n.t(`status.mode.${block.mode}`)}`)
  if (block.planMode) facts.push(theme.fg('#e0a63c', i18n.t('banner.plan')))
  if (block.cwd) facts.push(block.cwd)
  if (facts.length > 0) info.push(theme.fg(ink.descriptor, facts.join(theme.fg(ink.rule, ' · '))))
  if (block.sessionId) {
    info.push(theme.fg(ink.descriptor, i18n.t('hint.session', { id: shortId(block.sessionId) })))
  }
  info.push(theme.fg(ink.rule, '─'.repeat(Math.max(8, Math.min(46, width - 4)))))
  info.push(theme.fg(ink.descriptor, block.hint ?? i18n.t('banner.hint')))
  info.push(theme.fg(ink.descriptor, i18n.t('banner.keys')))

  const rows = ['']
  const wide = width >= artWidth + 34
  if (wide) {
    const count = Math.max(art.length, info.length)
    for (let index = 0; index < count; index += 1) {
      const left = art[index] === undefined
        ? ' '.repeat(artWidth)
        : padStyled(art[index], artWidth)
      const right = info[index] ?? ''
      rows.push(`  ${left}  ${right}`)
    }
  } else {
    for (const line of art) rows.push(`  ${line}`)
    rows.push('')
    for (const line of info) rows.push(`  ${line}`)
  }
  rows.push('')
  return rows.map((row) => truncateStyled(row, width))
}

function user(block, width, theme) {
  const bullet = theme.fg('roleUser', '❯ ')
  const body = wrapStyled(theme.fg('text', block.text), Math.max(1, width - 2))
  const rows = ['']
  body.forEach((line, index) => {
    rows.push(index === 0 ? bullet + line : '  ' + line)
  })
  return rows
}

function assistant(block, width, theme) {
  const body = renderMarkdown(block.text, Math.max(1, width - 2), theme)
  const bullet = theme.fg('textDim', '● ')
  const rows = ['']
  body.forEach((line, index) => {
    rows.push(index === 0 ? bullet + line : BODY_INDENT + line)
  })
  if (block.streaming) {
    rows[rows.length - 1] += theme.fg('primary', '▊')
  }
  return rows
}

function thought(block, width, theme, i18n, options) {
  const expanded = block.expanded === true || (options.expandTools && !block.collapsed)
  const label = block.streaming ? i18n.t('thinking.live') : i18n.t('thinking.done')
  const glyph = theme.fg('textDim', block.streaming ? options.spinner : '✻')
  const characters = visibleWidth(stripAnsi(block.text ?? ''))
  const meta = block.streaming
    ? theme.fg('textMuted', `${characters}`)
    : theme.fg('textMuted', i18n.t('thinking.characters', { count: characters }))
  const rows = ['', `${BODY_INDENT}${glyph} ${theme.fg('textDim', label)} ${meta}`]
  const text = block.text ?? ''
  if (text.trim() !== '') {
    const rendered = wrapStyled(theme.italic(theme.fg('textDim', text)), Math.max(1, width - 4))
    const shown = expanded ? rendered.slice(0, EXPANDED_LINES) : rendered.slice(Math.max(0, rendered.length - 3))
    for (const line of shown) rows.push(`${BODY_INDENT}  ${line}`)
    if (!expanded && rendered.length > shown.length) {
      rows.push(`${BODY_INDENT}  ${theme.fg('textMuted', i18n.t('tool.expand'))}`)
    }
  }
  return rows
}

function tool(block, width, theme, i18n, options) {
  const expanded = block.expanded === true || (options.expandTools && block.expanded !== false)
  const rows = ['']
  if (block.plan === true) {
    rows.push(`${BODY_INDENT}${theme.fg('warning', `▣ ${i18n.t('plan.card')}`)}`)
  }
  const name = theme.strong('textStrong', block.name ?? 'tool')
  const args = summary(block.input)
  const left = `${BODY_INDENT}${statusGlyph(block.status, theme, options.spinner)} ${name}${args === '' ? '' : '  ' + theme.fg('textDim', args)}`
  const right = statusText(block, theme, i18n)
  rows.push(spread(left, right, width, 2))
  if (block.input !== undefined && block.input !== null) {
    rows.push(...detailLines(block, width, theme, expanded))
  }
  if (block.output !== undefined && block.output !== null && block.output !== '') {
    rows.push(...outputLines(block, width, theme, i18n, expanded))
  }
  return rows
}

function statusGlyph(status, theme, spinner) {
  switch (status) {
    case 'completed':
      return theme.fg('success', '✓')
    case 'failed':
      return theme.fg('error', '✗')
    case 'in_progress':
      return theme.fg('primary', spinner)
    case 'cancelled':
      return theme.fg('textMuted', '⊘')
    default:
      return theme.fg('textMuted', '·')
  }
}

function statusText(block, theme, i18n) {
  const parts = []
  const label = block.status === 'completed'
    ? i18n.t('tool.done')
    : block.status === 'failed'
      ? i18n.t('tool.failed')
      : block.status === 'in_progress'
        ? i18n.t('tool.running')
        : block.status === 'cancelled'
          ? i18n.t('notice.cancelled')
          : i18n.t('tool.queued')
  parts.push(block.status === 'failed' ? theme.fg('error', label) : theme.fg('textMuted', label))
  const duration = block.endedAt !== undefined && block.startedAt !== undefined
    ? formatDuration(block.endedAt - block.startedAt)
    : ''
  if (duration !== '') parts.push(theme.fg('textMuted', duration))
  return parts.join(theme.fg('textMuted', ' · '))
}

/** A one-line argument summary for the card header. */
function summary(input) {
  if (input === undefined || input === null) return ''
  if (typeof input === 'string') return oneLine(input, 60)
  const record = input
  if (typeof record !== 'object') return ''
  const preferred = ['command', 'description', 'path', 'file_path', 'pattern', 'query', 'url', 'name', 'prompt']
  for (const key of preferred) {
    const value = record[key]
    if (typeof value === 'string' && value.trim() !== '') return oneLine(value, 60)
  }
  return ''
}

function oneLine(value, limit) {
  const text = String(value).replace(/\s+/g, ' ').trim()
  return text.length > limit ? `${text.slice(0, limit - 1)}…` : text
}

function detailLines(block, width, theme, expanded) {
  const rows = []
  const input = block.input
  const inner = Math.max(1, width - 6)
  if (typeof input !== 'object' || input === null) return rows
  // The header already shows the one field that identifies the call; the body
  // carries only what a person would otherwise have to expand to see — long or
  // multi-line payloads such as file content and patches.
  const headerFields = new Set(['command', 'description', 'path', 'file_path', 'pattern', 'query', 'url', 'name', 'prompt'])
  const keys = Object.keys(input).filter((key) => {
    const value = input[key]
    if (value === undefined || value === null) return false
    if (headerFields.has(key) && typeof value === 'string' && !value.includes('\n')) return false
    if (!expanded && typeof value === 'string' && value.length < 40 && !value.includes('\n')) return false
    if (!expanded && typeof value !== 'string') return false
    return true
  })
  const shown = expanded ? keys : keys.slice(0, 4)
  for (const key of shown) {
    const value = input[key]
    if (typeof value === 'string') {
      const label = theme.fg('textMuted', `${key}:`)
      const lines = wrapStyled(theme.fg('textDim', value), Math.max(8, inner - key.length - 2))
      lines.forEach((line, index) => {
        rows.push(BODY_INDENT + '  ' + (index === 0 ? `${label} ${line}` : ' '.repeat(key.length + 1) + line))
      })
      continue
    }
    if (typeof value === 'object') {
      let json = ''
      try {
        json = JSON.stringify(value)
      } catch {
        json = String(value)
      }
      rows.push(BODY_INDENT + '  ' + theme.fg('textMuted', `${key}:`) + ' ' + theme.fg('textDim', oneLine(json, Math.max(20, inner - key.length - 2))))
    }
  }
  if (!expanded && keys.length > shown.length) {
    rows.push(BODY_INDENT + '  ' + theme.fg('textMuted', `… ${keys.length - shown.length} more (ctrl+o)`))
  }
  return rows
}

function outputLines(block, width, theme, i18n, expanded) {
  const text = sanitizeOutput(block.output)
  const limit = expanded ? EXPANDED_LINES : PREVIEW_LINES
  const inner = Math.max(1, width - 6)
  const all = trimBlankEdges(text.split('\n'))
  const rows = []
  if (all.length === 0) return rows
  const interestingDiff = all.some((line) => /^[-+]{3} |^@@ /u.test(line)) && all.length > 3
  const renderer = interestingDiff
    ? renderDiff(text, inner, theme, { maxLines: limit })
    : all.slice(0, limit).flatMap((line) => wrapStyled(theme.fg(diffTint(line, block.status), line), inner))
  for (const line of renderer) rows.push(BODY_INDENT + '  ' + theme.fg('textMuted', '│') + ' ' + line)
  if (!expanded && all.length > limit) {
    rows.push(BODY_INDENT + '  ' + theme.fg('textMuted', '│') + ' ' + theme.fg('textMuted', i18n.t('tool.more-lines', { count: all.length - limit })))
  }
  return rows
}

function trimBlankEdges(lines) {
  let start = 0
  let end = lines.length
  while (start < end && lines[start].trim() === '') start += 1
  while (end > start && lines[end - 1].trim() === '') end -= 1
  return lines.slice(start, end)
}

function diffTint(line, status) {
  if (status === 'failed') return 'error'
  if (/^\s*\+\s/u.test(line)) return 'diffAdded'
  if (/^\s*-\s/u.test(line)) return 'diffRemoved'
  if (/^\s*\^\s*$/u.test(line)) return 'diffMeta'
  return 'textDim'
}

function notice(block, width, theme) {
  const level = block.level ?? 'info'
  const glyph = level === 'error' ? theme.fg('error', '✗') : level === 'success' ? theme.fg('success', '✓') : theme.fg('textMuted', '·')
  const color = level === 'error' ? 'error' : level === 'success' ? 'success' : 'textDim'
  const body = wrapStyled(theme.fg(color, block.text ?? ''), Math.max(1, width - 4))
  return body.map((line, index) => (index === 0 ? `  ${glyph} ${line}` : `    ${line}`))
}

/** Shortens a session id to the tail people quote to each other. */
export function shortId(id) {
  const text = String(id ?? '')
  const match = /session-([0-9a-f-]+)$/u.exec(text)
  const value = match === null ? text : match[1]
  return value.length > 8 ? `${value.slice(0, 4)}…${value.slice(-4)}` : value
}

/** Right-aligned timestamp used by list dialogs. */
export function ageLabel(ms) {
  if (!Number.isFinite(ms) || ms < 0) return ''
  const minutes = Math.floor(ms / 60000)
  if (minutes < 1) return 'now'
  if (minutes < 60) return `${minutes}m`
  const hours = Math.floor(minutes / 60)
  if (hours < 24) return `${hours}h`
  return `${Math.floor(hours / 24)}d`
}

/** Pads `text` to `width` cells with `fill`. */
export function fillTo(text, width, fill = ' ', align = 'left') {
  return padStyled(text, width, align).replace(/ +$/u, (match) => fill.repeat(match.length))
}
