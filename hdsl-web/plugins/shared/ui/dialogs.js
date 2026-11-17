/**
 * Dialogs drawn over the transcript.
 *
 * Every dialog is a box of styled rows the renderer centres; this module only
 * decides what is inside and how tall it wants to be. Pickers share one list
 * widget, so a model list, an effort list and a session list scroll and
 * highlight identically. A dialog sizes itself to its content — an empty list
 * should be two rows, not a wall of padding.
 *
 * @module ui/dialogs
 */

import { padStyled, truncateStyled, visibleWidth, wrapStyled } from '../term/text.js'
import { renderDiff } from '../term/markdown.js'

const POINTER = '❯'
const MAX_DESCRIPTION_LINES = 2

/** How many rows a permission's detail may take before it is elided. */
const MAX_PERMISSION_DETAIL_LINES = 8

/** Draws a bordered box around already-styled lines. */
export function box({ title, lines, width, height, theme, footer, titleRight }) {
  const inner = Math.max(4, width - 4)
  const body = []
  for (const line of lines) {
    for (const row of wrapStyled(line, inner)) body.push(row)
  }
  const limit = Math.max(1, height - 2)
  const shown = body.length > limit ? body.slice(0, limit) : body
  const rows = []
  rows.push(headRow(title, titleRight, width, theme))
  for (const line of shown) {
    rows.push(theme.fg('border', '│') + ' ' + padStyled(line, inner) + ' ' + theme.fg('border', '│'))
  }
  rows.push(footRow(footer, width, theme))
  return rows
}

function headRow(title, titleRight, width, theme) {
  if (title === undefined || title === '') {
    return theme.fg('border', '╭' + '─'.repeat(width - 2) + '╮')
  }
  const right = titleRight === undefined ? '' : titleRight
  const used = visibleWidth(` ${title} `) + visibleWidth(right)
  const rest = Math.max(0, width - 2 - used)
  return theme.fg('border', '╭')
    + theme.strong('textStrong', ` ${title} `)
    + theme.fg('border', '─'.repeat(rest))
    + theme.fg('textMuted', right)
    + theme.fg('border', '─╮')
}

function footRow(footer, width, theme) {
  if (footer === undefined || footer === '') {
    return theme.fg('border', '╰' + '─'.repeat(width - 2) + '╯')
  }
  const text = truncateStyled(footer, Math.max(4, width - 6))
  const gap = Math.max(0, width - 6 - visibleWidth(text))
  return theme.fg('border', '╰─ ') + theme.fg('textMuted', text) + theme.fg('border', ` ${'─'.repeat(gap)}╯`)
}

/**
 * Renders whichever overlay is open.
 *
 * @param {object} overlay
 * @param {{width: number, height: number, theme: object, i18n: object}} context
 * @returns {string[] | null}
 */
export function renderOverlay(overlay, context) {
  if (overlay === null || overlay === undefined) return null
  const { width: screenWidth, height: screenHeight, theme, i18n } = context
  const width = Math.max(28, Math.min(screenWidth - 4, overlay.width ?? 62))
  // `maxHeight` is the newer spelling; `height` was what the ACP app sets on every
  // overlay it opens, and reading only one of them silently ignored the other's intent.
  const maxHeight = Math.max(5, Math.min(screenHeight - 4, overlay.maxHeight ?? overlay.height ?? 20))
  let lines
  let footer
  if (overlay.kind === 'picker') {
    lines = pickerLines(overlay, width, maxHeight - 2, theme, i18n)
    footer = overlay.hint ?? i18n.t('model.hint')
  } else if (overlay.kind === 'permission') {
    lines = permissionLines(overlay, width, theme, i18n)
    footer = overlay.hint ?? i18n.t('permission.hint')
  } else if (overlay.kind === 'help') {
    lines = helpLines(overlay, theme)
    footer = overlay.hint ?? i18n.t('help.hint')
  } else {
    lines = overlay.lines ?? []
    footer = overlay.hint
  }
  const height = Math.min(maxHeight, Math.max(4, lines.length + 2))
  return box({
    title: overlay.title,
    titleRight: overlay.count,
    lines,
    width,
    height,
    theme,
    footer,
  })
}

function pickerLines(overlay, width, budget, theme, i18n) {
  const items = overlay.items ?? []
  const inner = Math.max(4, width - 4)
  const selected = Math.max(0, Math.min(items.length - 1, overlay.index ?? 0))

  // Size the window by rendered rows, not by item count: a description that
  // wraps to two lines is a taller row than a bare label.
  const rendered = items.map((item, index) => {
    const rows = []
    const selectedItem = item === items[selected]
    const pointer = selectedItem ? theme.fg('primary', POINTER) : ' '
    // The number is the shortcut (1-9 pick a row), and printing it is what lets a person
    // discover that — the same reason the permission dialog numbers its options.
    const number = index < 9 ? theme.fg(selectedItem ? 'primary' : 'textMuted', `${index + 1}.`) : '  '
    const label = selectedItem ? theme.strong('textStrong', item.label) : theme.fg('text', item.label)
    const tail = item.tail ? theme.fg('textMuted', `  ${item.tail}`) : ''
    rows.push(`${pointer} ${number} ${label}${tail}`)
    if (item.description) {
      const wrapped = wrapStyled(theme.fg('textMuted', item.description), Math.max(8, inner - 2))
      for (const row of wrapped.slice(0, MAX_DESCRIPTION_LINES)) rows.push(`  ${row}`)
    }
    return { item, rows }
  })

  // Convert a row budget into an item range around the selection.
  const cost = (index) => (rendered[index]?.rows.length ?? 0) + (items[index]?.group !== undefined ? 1 : 0)
  let start = selected
  let end = selected + 1
  let used = cost(selected)
  while (true) {
    const before = start - 1
    const after = end
    const beforeCost = before >= 0 ? cost(before) : Infinity
    const afterCost = after < items.length ? cost(after) : Infinity
    const fitsBefore = before >= 0 && used + beforeCost <= budget
    const fitsAfter = after < items.length && used + afterCost <= budget
    if (!fitsBefore && !fitsAfter) break
    if (fitsBefore && (!fitsAfter || selected - before <= after - selected)) {
      start = before
      used += beforeCost
    } else {
      end = after + 1
      used += afterCost
    }
  }

  const lines = []
  if (start > 0) lines.push(theme.fg('textMuted', `  ↑ ${start} more`))
  let lastGroup
  for (let index = start; index < end; index += 1) {
    const item = items[index]
    if (item.group !== undefined && item.group !== lastGroup) {
      lines.push(theme.fg('textMuted', item.group))
      lastGroup = item.group
    }
    lines.push(...rendered[index].rows)
  }
  if (end < items.length) lines.push(theme.fg('textMuted', `  ↓ ${items.length - end} more`))
  if (items.length === 0) lines.push(theme.fg('textDim', overlay.emptyText ?? ''))
  return lines
}

function permissionLines(overlay, width, theme, i18n) {
  const lines = []
  const tool = overlay.toolName ? theme.strong('textStrong', overlay.toolName) : theme.fg('text', 'tool')
  lines.push(`${theme.fg('warning', '⚑')} ${tool}`)
  if (overlay.detail) {
    // The detail is a command or a patch: potentially hundreds of rows. The box keeps a
    // budgeted window and the rest is reachable with page-up/page-down — the options stay
    // on screen, because answering without seeing what is being answered is the defect
    // this dialog exists to avoid.
    const inner = Math.max(8, width - 6)
    const detail = String(overlay.detail)
    const isDiff = /(^|\n)(diff --git|@@ )/u.test(detail)
    const source = isDiff
      ? renderDiff(detail, inner, theme, { maxLines: 400 })
      : [theme.fg('textDim', detail)]
    const wrapped = source.flatMap((line) => wrapStyled(line, inner))
    const budget = MAX_PERMISSION_DETAIL_LINES
    const maxOffset = Math.max(0, wrapped.length - budget)
    const offset = Math.max(0, Math.min(maxOffset, overlay.detailOffset ?? 0))
    lines.push(...wrapped.slice(offset, offset + budget))
    if (maxOffset > 0) {
      const hidden = wrapped.length - budget
      const where = offset === 0 ? `↓ ${hidden}` : offset >= maxOffset ? `↑ ${hidden}` : `↑ ${offset} · ↓ ${hidden - offset}`
      lines.push(theme.fg('textMuted', `  … ${i18n.t('permission.detail.more', { where, count: hidden })}`))
    }
  }
  lines.push('')
  const options = overlay.options ?? []
  options.forEach((option, index) => {
    const active = index === (overlay.index ?? 0)
    const mark = active ? theme.fg('primary', POINTER) : ' '
    // The number is the shortcut: 1-9 pick the row, and seeing it is what makes the
    // shortcut discoverable (the keys worked before, but nothing said so).
    const number = index < 9 ? theme.fg(active ? 'primary' : 'textMuted', `${index + 1}.`) : '  '
    const label = active ? theme.strong('textStrong', option.label) : theme.fg('text', option.label)
    const hint = active && option.hint ? theme.fg('textMuted', `  ${option.hint}`) : ''
    lines.push(`${mark} ${number} ${label}${hint}`)
  })
  if (overlay.reason !== null && overlay.reason !== undefined) {
    // A rejection can carry a reason the model reads. The line lives inside the dialog:
    // the composer behind it is not visible, so typing there would edit unseen text.
    const draft = overlay.reason.text ?? ''
    const caret = theme.fg('primary', '▏')
    lines.push('')
    lines.push(`${theme.fg('textMuted', `  ${i18n.t('permission.reason.label')}`)}${theme.fg('text', draft)}${caret}`)
    lines.push(theme.fg('textMuted', `  ${i18n.t('permission.reason.hint')}`))
  }
  return lines
}

function helpLines(overlay, theme) {
  const lines = []
  for (const section of overlay.sections ?? []) {
    lines.push(theme.strong('textStrong', section.title))
    for (const row of section.rows) {
      const [key, text] = row
      lines.push(`  ${theme.fg('primary', padStyled(key, 14))}${theme.fg('textDim', text)}`)
    }
    lines.push('')
  }
  if (lines.length > 0) lines.pop()
  return lines
}

/** Centres a dialog block inside the screen. */
export function center(rows, screenWidth, screenHeight) {
  const dialogWidth = Math.max(...rows.map((row) => visibleWidth(row)))
  const left = Math.max(0, Math.floor((screenWidth - dialogWidth) / 2))
  const top = Math.max(0, Math.floor((screenHeight - rows.length) / 2))
  return { rows, left, top, width: dialogWidth, height: rows.length }
}
