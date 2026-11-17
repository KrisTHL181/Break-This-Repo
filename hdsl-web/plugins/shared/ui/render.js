/**
 * Frame composition.
 *
 * The screen is laid out from the bottom: a status row, the composer box, an
 * activity row, and the transcript taking whatever is left. Every row is exactly
 * the terminal width, so the painter can diff and anchor rows without guessing.
 * Dialogs are composited over the transcript — their rows replace the middle of
 * the covered lines — which keeps the conversation visible while a picker is
 * open.
 *
 * @module ui/render
 */

import { padStyled, spread, truncateStyled, visibleWidth } from '../term/text.js'
import { formatDuration, renderBlock, shortId } from './blocks.js'
import { center, renderOverlay } from './dialogs.js'
import { describeCandidates } from './complete.js'

const SPINNER = ['⠋', '⠙', '⠹', '⠸', '⠼', '⠴', '⠦', '⠧', '⠇', '⠏']
const MIN_TRANSCRIPT_ROWS = 2
/** How many rows the inline `/` menu may take before it starts scrolling. */
const MAX_MENU_ROWS = 7

/** The spinner frame for a moment in time. */
export function spinnerFrame(clock) {
  return SPINNER[Math.floor(clock / 80) % SPINNER.length]
}

/**
 * Renders the whole frame.
 *
 * @param {object} state
 * @param {{width: number, height: number, theme: object, i18n: object, clock: number}} context
 * @returns {{rows: string[], cursor: {row: number, column: number} | null, transcript: {total: number}}}
 */
export function renderFrame(state, context) {
  const { width, height, theme, i18n, clock } = context
  const spinner = spinnerFrame(clock)
  const scope = { theme, i18n, spinner }

  const statusRow = renderStatus(state, width, theme, i18n)
  const inner = Math.max(1, width - 4)
  const composerLayout = layoutComposer(state, inner)
  const composerRows = paintComposer(state, width, composerLayout, theme, i18n)
  const activityRow = renderActivity(state, width, theme, i18n, clock, spinner)
  // The `/` menu is part of the bottom block, not a dialog: it starts directly
  // under the input box and pushes the transcript up, the way Kimi Code draws
  // its command list. Its height is bounded by the screen, because the composer
  // still has to fit.
  const menuBudget = Math.max(1, Math.min(MAX_MENU_ROWS,
    height - composerRows.length - 2 - (activityRow === null ? 0 : 1) - 1 - MIN_TRANSCRIPT_ROWS))
  const menuRows = state.overlay === null ? renderCommandMenu(state, width, theme, menuBudget) : []
  // The hint line sits **below** the input box, the way Kimi Code draws it: the
  // thing you are looking at while you type should be next to what you type.
  const hintRow = renderHint(state, width, theme, i18n, clock)
  const bottomRows = composerRows.length + 2 + (activityRow === null ? 0 : 1) + 1 + menuRows.length
  const transcriptHeight = Math.max(MIN_TRANSCRIPT_ROWS, height - bottomRows)
  const transcript = renderTranscript(state, width, transcriptHeight, scope)

  const rows = new Array(height).fill('')
  for (let index = 0; index < transcript.rows.length; index += 1) rows[index] = transcript.rows[index]

  // A short terminal (or a composer that has grown to several lines) can make the bottom
  // block taller than the screen: `top` went negative and the rows were written at negative
  // indices, which shift the frame and pull the cursor back to row 1. Clamp where the block
  // starts and refuse writes outside the screen — the composer is what the user is typing
  // into, so it wins over the transcript that is being clipped anyway.
  const top = Math.max(0, height - bottomRows)
  let row = top
  const put = (value) => {
    if (row >= 0 && row < height) rows[row] = value
    row += 1
  }
  put('')
  if (activityRow !== null) put(activityRow)
  for (const composerRow of composerRows) put(composerRow)
  for (const menuRow of menuRows) put(menuRow)
  put(hintRow)
  rows[height - 1] = statusRow

  let cursor = null
  if (state.overlay !== null) {
    paintOverlay(rows, state, { width, height, theme, i18n })
  } else if (state.focus === 'composer') {
    cursor = {
      row: top + 1 + (activityRow === null ? 0 : 1) + 1 + composerLayout.caretRow,
      // `│ ` + `❯ ` is four cells before the first content cell. Three put the
      // caret on the last typed character (or the right half of a wide CJK
      // glyph), which is exactly the "cursor sits in the middle of the text"
      // defect seen in the Go client.
      column: 4 + composerLayout.caretColumn,
    }
  }

  return {
    rows: rows.map((line) => padStyled(line, width)),
    cursor,
    transcript: { total: transcript.total },
  }
}

function layoutComposer(state, inner) {
  const view = state.composer.view(inner)
  const maxLines = state.composer.maxLines
  const rows = Math.min(maxLines, Math.max(1, view.total))
  // Scroll the visible window so the caret line stays on screen.
  const first = Math.max(0, Math.min(view.caret.row - rows + 1, view.total - rows))
  return {
    lines: view.lines.slice(first, first + rows),
    caretRow: view.caret.row - first,
    caretColumn: view.caret.column,
    rows,
  }
}

function paintComposer(state, width, layout, theme, i18n) {
  const inner = Math.max(1, width - 4)
  const borderColor = state.overlay !== null ? 'border' : 'primary'
  const rows = [theme.fg(borderColor, '╭' + '─'.repeat(Math.max(0, width - 2)) + '╮')]
  const placeholder = state.composer.empty
    ? (state.status === 'working' ? i18n.t('composer.placeholder.busy') : i18n.t('composer.placeholder'))
    : null
  for (let index = 0; index < layout.rows; index += 1) {
    const content = layout.lines[index] ?? ''
    const bullet = index === 0 ? theme.fg('primary', '❯ ') : '  '
    const painted = index === 0 && placeholder !== null
      ? bullet + theme.fg('textMuted', placeholder)
      : bullet + theme.fg('text', content)
    rows.push(theme.fg(borderColor, '│') + ' ' + padStyled(painted, inner) + ' ' + theme.fg(borderColor, '│'))
  }
  rows.push(theme.fg(borderColor, '╰' + '─'.repeat(Math.max(0, width - 2)) + '╯'))
  return rows
}

function paintOverlay(rows, state, context) {
  const { width, height, theme, i18n } = context
  const dialogRows = renderOverlay(state.overlay, context)
  if (dialogRows === null) return
  const placed = center(dialogRows, width, height)
  for (let index = 0; index < placed.rows.length; index += 1) {
    const target = placed.top + index
    if (target < 0 || target >= height) continue
    const base = rows[target] ?? ''
    const left = padStyled(truncateStyled(base, placed.left), placed.left)
    const dialogWidth = visibleWidth(placed.rows[index])
    const right = padStyled('', Math.max(0, width - placed.left - dialogWidth))
    rows[target] = left + placed.rows[index] + right
  }
}

function renderStatus(state, width, theme, i18n) {
  const busy = state.status === 'working'
  const dot = state.status === 'ready' || busy
    ? theme.fg('success', '●')
    : state.status === 'connecting'
      ? theme.fg('warning', '◌')
      : theme.fg('error', '✕')
  const model = theme.strong('text', state.model.currentLabel ?? state.model.current ?? 'dsh')
  const effortLabel = state.effort.current
    ? (i18n.has(`effort.${state.effort.current}`) ? i18n.t(`effort.${state.effort.current}`) : state.effort.current)
    : ''
  const effort = effortLabel === '' ? '' : theme.fg('textMuted', effortLabel)
  const git = state.gitBranch ? theme.fg('textMuted', ` ${state.gitBranch}`) : ''
  const cwd = theme.fg('textDim', compactPath(state.cwd, Math.max(10, Math.floor(width / 4)))) + git
  const usage = state.usage.size > 0
    ? theme.fg('textMuted', width >= 72
      ? `${i18n.t('status.context')} ${percent(state.usage)}% · ${formatTokens(state.usage.used)}/${formatTokens(state.usage.size)}`
      : `${i18n.t('status.context')} ${percent(state.usage)}%`)
    : ''
  const scroll = state.scroll.offset > 0 ? theme.fg('warning', `↑ ${state.scroll.offset}`) : ''
  // The session id and the approval mode are the two facts a person checks when
  // they are not sure which conversation or which "will it ask me" they are in.
  // `shortId` is what the notices already use: a harness session id is
  // `session-<hex>`, so slicing the first characters showed the useless prefix
  // ("#sessio") instead of anything that identifies the conversation.
  const session = typeof state.sessionId === 'string' && state.sessionId !== ''
    ? theme.fg('textMuted', `#${shortId(state.sessionId)}`)
    : ''
  const plan = state.planMode === true ? theme.fg('warning', `▣${i18n.t('banner.plan')}`) : ''
  const right = [scroll, session, usage].filter(Boolean).join('  ')
  const left = [dot, model, effort, renderMode(state, theme, i18n), plan, cwd].filter((part) => part !== '').join(' ')
  return spread(left, right, width, 2)
}

/** The approval mode badge: how much this session asks before running a tool. */
function renderMode(state, theme, i18n) {
  const mode = state.approval?.mode
  if (typeof mode !== 'string' || mode === '') return ''
  const key = `status.mode.${mode}`
  const label = i18n.has(key) ? i18n.t(key) : mode
  if (mode === 'full') return theme.fg('warning', `⚡${label}`)
  if (mode === 'ask') return theme.fg('textMuted', `◇${label}`)
  return theme.fg('primary', `◆${label}`)
}

function percent(usage) {
  if (!Number.isFinite(usage.used) || !Number.isFinite(usage.size) || usage.size <= 0) return 0
  return Math.min(100, Math.round((usage.used / usage.size) * 100))
}

function compactPath(path, budget) {
  const text = String(path ?? '')
  if (visibleWidth(text) <= budget) return text
  const parts = text.split(/[\\/]/).filter(Boolean)
  const tail = []
  let used = 0
  for (let index = parts.length - 1; index >= 0; index -= 1) {
    const part = parts[index]
    const width = visibleWidth(part) + 1
    if (used + width > budget - 2) break
    tail.unshift(part)
    used += width
  }
  return tail.length === 0 ? text.slice(-budget) : `…/${tail.join('/')}`
}

function renderActivity(state, width, theme, i18n, clock, spinner) {
  if (state.status === 'working') {
    const label = state.activity.tool
      ? i18n.t('working.tool', { tool: state.activity.tool })
      : i18n.t('working')
    const elapsed = state.activity.startedAt === null ? 0 : clock - state.activity.startedAt
    // A live size next to the clock: waiting for a long answer without any sign of
    // progress reads as a hang, and the token count is the number that moves.
    const live = liveCharacters(state)
    const size = live > 0 ? theme.fg('textMuted', ` · ~${formatTokens(Math.round(live / 4))}`) : ''
    return ` ${theme.fg('primary', spinner)} ${theme.fg('textDim', label)}${theme.fg('textMuted', `  ${formatDuration(elapsed)}`)}${size}`
  }
  if (state.status === 'connecting') {
    return ` ${theme.fg('warning', spinner)} ${theme.fg('textDim', i18n.t('status.polling'))}`
  }
  return null
}

/**
 * The `/` command list, drawn inline under the composer.
 *
 * It is deliberately not a bordered dialog: a box centred over the transcript
 * is what made `/` feel like a pop-up window instead of part of the input.
 * One row per command, one dial: `↑`/`↓` move, enter runs, esc closes. The
 * labels are numbered because the number is a shortcut, and the window keeps
 * the selection in view when the list is longer than the budget.
 */
function renderCommandMenu(state, width, theme, budget) {
  const menu = state.palette
  if (menu === null || menu === undefined) return []
  const items = menu.items ?? []
  if (items.length === 0) return []
  const selected = Math.max(0, Math.min(items.length - 1, menu.index ?? 0))
  const rowsBudget = Math.max(1, Math.min(MAX_MENU_ROWS, budget))
  // Two of the budget rows may be "more" markers; on a screen too short for
  // those, show the selected item alone rather than overflowing the block.
  const showMore = rowsBudget >= 3
  let start = 0
  let end = items.length
  if (items.length > rowsBudget) {
    // Reserve one row for each "more" marker; the item window then follows the
    // selection instead of always showing the first page.
    const capacity = Math.max(1, showMore ? rowsBudget - 2 : 1)
    start = Math.max(0, Math.min(selected - Math.floor(capacity / 2), items.length - capacity))
    end = Math.min(items.length, start + capacity)
  }
  const labelWidth = Math.max(8, Math.min(30, Math.floor(width * 0.32)))
  const descriptionWidth = width >= 56 ? Math.max(0, width - 9 - labelWidth) : 0
  const rows = []
  if (showMore && start > 0) rows.push(theme.fg('textMuted', `  ↑ ${start} more`))
  for (let index = start; index < end; index += 1) {
    const item = items[index]
    const active = index === selected
    const pointer = active ? theme.fg('primary', '❯') : ' '
    const number = index < 9 ? theme.fg(active ? 'primary' : 'textMuted', `${index + 1}.`) : '  '
    const label = active ? theme.strong('textStrong', item.label) : theme.fg('text', item.label)
    const description = descriptionWidth > 0 && item.description
      ? '  ' + truncateStyled(theme.fg('textMuted', item.description), descriptionWidth)
      : ''
    rows.push(`  ${pointer} ${number} ${padStyled(label, labelWidth)}${description}`)
  }
  if (showMore && end < items.length) rows.push(theme.fg('textMuted', `  ↓ ${items.length - end} more`))
  return rows
}

/**
 * The line under the input box.
 *
 * In order of who needs it: the candidates a Tab is cycling, then what the keys
 * do while a turn runs, then the envelope after it ends, and otherwise the one
 * line that does not rotate — `/` commands, `@` files, `ctrl+j`, `ctrl+o`, `esc`.
 * A rotating tip hid two of those at any moment, which is the opposite of
 * discoverable.
 */
function renderHint(state, width, theme, i18n, clock) {
  if (state.overlay === null && state.palette !== null && state.palette !== undefined) {
    return ` ${theme.fg('textMuted', i18n.t('palette.hint'))}`
  }
  const candidates = describeCandidates(state.completion, Math.max(10, width - 4))
  if (candidates !== '') return ` ${theme.fg('textMuted', `${candidates}  ⇥`)}`
  if (state.status === 'working') {
    const queued = state.queue.length > 0 ? `${i18n.t('hint.queued', { count: String(state.queue.length) })} · ` : ''
    return ` ${theme.fg('textMuted', `${queued}${i18n.t('hint.esc')} · ${i18n.t('hint.ctrl-c')}`)}`
  }
  const recent = state.lastTurnSeconds !== null && state.turnEndedAt !== undefined
    ? clock - state.turnEndedAt < 6000
    : false
  if (recent) {
    return ` ${theme.fg('textMuted', `· ${i18n.t('notice.turn-end', { seconds: state.lastTurnSeconds.toFixed(1) })}`)}`
  }
  return ` ${theme.fg('textMuted', i18n.t('hint.line'))}`
}

/** Characters currently streaming, for the live token estimate. */
function liveCharacters(state) {
  let total = 0
  for (const block of state.blocks) {
    if (block.streaming !== true) continue
    total += String(block.full ?? block.text ?? '').length
  }
  return total
}

function renderTranscript(state, width, height, scope) {
  const cache = state.transcriptCache
  const lines = []
  for (const block of state.blocks) {
    const cached = cache.get(block)
    if (cached !== undefined && cached.width === width && cached.rev === block.rev) {
      lines.push(...cached.lines)
      continue
    }
    const rendered = renderBlock(block, width, scope.theme, scope.i18n, { spinner: scope.spinner, expandTools: state.expandTools })
    cache.set(block, { width, rev: block.rev, lines: rendered })
    lines.push(...rendered)
    if (cache.size > 400) {
      const first = cache.keys().next()
      if (!first.done) cache.delete(first.value)
    }
  }
  const total = lines.length
  const maxOffset = Math.max(0, total - height)
  const offset = state.scroll.follow ? 0 : Math.min(state.scroll.offset, maxOffset)
  const start = Math.max(0, total - height - offset)
  const visible = lines.slice(start, start + height)
  while (visible.length < height) visible.push('')
  return { rows: visible, total, maxOffset }
}

/** Formats a token count for the status row (kept exported for tests). */
export function formatTokens(value) {
  if (!Number.isFinite(value)) return '0'
  if (value >= 1_000_000) return `${(value / 1_000_000).toFixed(1)}m`
  if (value >= 1_000) return `${(value / 1_000).toFixed(1)}k`
  return String(value)
}
