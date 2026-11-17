/**
 * Text geometry for a terminal cell grid.
 *
 * Everything here is ANSI-aware: a styled string is measured, wrapped and cut by
 * its *visible* cells, while the colour escapes travel with the text they style.
 * The renderer builds frames out of these helpers, so they are the one place that
 * has to know about East Asian wide characters, combining marks and control
 * sequences.
 *
 * @module term/text
 */

const ESC = '\x1b'
const SGR_RE = /\x1b\[([0-9;:]*)m/gu
const ANSI_RE = /\x1b\[[0-9;:?]*[ -/]*[@-~]/gu
const OSC_RE = /\x1b\][\s\S]*?(?:\x07|\x1b\\)/gu
const TAB_RE = /\t/gu

/** Control sequences that occupy no cells. */
export function stripAnsi(value) {
  return String(value).replace(OSC_RE, '').replace(ANSI_RE, '')
}

/** Display width of one code point. */
export function codePointWidth(cp) {
  if (cp === 0x200d) return 0 // zero width joiner
  if (cp === 0xfe0f || cp === 0xfe0e) return 0 // variation selectors
  if (cp >= 0x0300 && cp <= 0x036f) return 0 // combining diacritics
  if (cp >= 0x1ab0 && cp <= 0x1aff) return 0
  if (cp >= 0x20d0 && cp <= 0x20ff) return 0
  if (cp >= 0xfe20 && cp <= 0xfe2f) return 0
  if (cp === 0x200b || cp === 0x200c || cp === 0x200e || cp === 0x200f) return 0
  if (cp < 32 || (cp >= 0x7f && cp < 0xa0)) return 0
  // East Asian Wide / Fullwidth, plus the emoji planes terminals draw double.
  if (
    (cp >= 0x1100 && cp <= 0x115f) ||
    (cp >= 0x2e80 && cp <= 0x303e) ||
    (cp >= 0x3041 && cp <= 0x33ff) ||
    (cp >= 0x3400 && cp <= 0x4dbf) ||
    (cp >= 0x4e00 && cp <= 0x9fff) ||
    (cp >= 0xa000 && cp <= 0xa4cf) ||
    (cp >= 0xa960 && cp <= 0xa97f) ||
    (cp >= 0xac00 && cp <= 0xd7a3) ||
    (cp >= 0xf900 && cp <= 0xfaff) ||
    (cp >= 0xfe10 && cp <= 0xfe19) ||
    (cp >= 0xfe30 && cp <= 0xfe6f) ||
    (cp >= 0xff00 && cp <= 0xff60) ||
    (cp >= 0xffe0 && cp <= 0xffe6) ||
    (cp >= 0x1f300 && cp <= 0x1f64f) ||
    (cp >= 0x1f900 && cp <= 0x1f9ff) ||
    (cp >= 0x1fa70 && cp <= 0x1faff) ||
    (cp >= 0x1f000 && cp <= 0x1f02f) ||
    (cp >= 0x20000 && cp <= 0x3fffd)
  ) {
    return 2
  }
  return 1
}

/** Display width of a plain (escape-free) string. */
export function plainWidth(value) {
  let width = 0
  for (const ch of value) width += codePointWidth(ch.codePointAt(0))
  return width
}

/** Display width of a possibly styled string. */
export function visibleWidth(value) {
  return plainWidth(stripAnsi(String(value)))
}

/**
 * Re-apply the styling that is still open at a wrap point.
 *
 * A styled string is cut into runs of SGR escapes and text. Wrapping has to
 * remember every code seen since the last reset so the continuation line starts
 * in the same colour, and close the line before the newline.
 */
class StyleCarry {
  constructor() {
    this.codes = []
  }

  /** Record the SGR parameter list and return the escape itself. */
  push(params) {
    if (params === '' || params === '0') {
      this.codes = []
    } else {
      this.codes.push(params)
      // `0;xx` resets first; keep only what follows.
      const flat = params.split(';')
      if (flat[0] === '0') this.codes = flat.slice(1).filter(Boolean)
    }
    return `${ESC}[${params}m`
  }

  get prefix() {
    return this.codes.length === 0 ? '' : `${ESC}[${this.codes.join(';')}m`
  }
}

/**
 * Split a styled string into visible cells, each carrying the style prefix that
 * applies to it. Trailing combining marks join the previous cell.
 * @param {string} value styled text
 * @returns {{ch: string, width: number, style: string}[]}
 */
export function cells(value) {
  const text = String(value).replace(TAB_RE, '   ')
  const out = []
  const carry = new StyleCarry()
  let i = 0
  while (i < text.length) {
    if (text[i] === ESC) {
      const rest = text.slice(i)
      const match = /^\x1b\[([0-9;:]*)m/u.exec(rest)
      if (match) {
        carry.push(match[1])
        i += match[0].length
        continue
      }
      const other = /^\x1b\[[0-9;:?]*[ -/]*[@-~]/u.exec(rest) ?? /^\x1b\][\s\S]*?(?:\x07|\x1b\\)/u.exec(rest)
      if (other) {
        i += other[0].length
        continue
      }
      i += 1
      continue
    }
    const cp = text.codePointAt(i)
    const ch = String.fromCodePoint(cp)
    i += ch.length
    const width = codePointWidth(cp)
    if (width === 0 && out.length > 0) {
      out[out.length - 1].ch += ch
      continue
    }
    out.push({ ch, width, style: carry.prefix })
  }
  return out
}

/**
 * Wrap a styled string to a cell width.
 *
 * Breaks on spaces when one is available in the line so far, otherwise between
 * cells — the same greedy behaviour a terminal editor uses, and the one that
 * keeps long paths and URLs from overflowing the pane.
 *
 * @param {string} value styled text, `\n` respected as a hard break
 * @param {number} width cell budget per line
 * @returns {string[]} styled lines, never wider than `width`
 */
export function wrapStyled(value, width) {
  const limit = Math.max(1, Math.trunc(width))
  const lines = []
  for (const hard of String(value).split('\n')) {
    const list = cells(hard)
    if (list.length === 0) {
      lines.push('')
      continue
    }
    let line = []
    let used = 0
    let lastSpace = -1
    const flush = () => {
      lines.push(renderCells(line))
      line = []
      used = 0
      lastSpace = -1
    }
    for (const cell of list) {
      if (used + cell.width > limit && line.length > 0) {
        if (lastSpace > 0) {
          const head = line.slice(0, lastSpace)
          const tail = line.slice(lastSpace + 1)
          lines.push(renderCells(head))
          line = tail
          used = tail.reduce((sum, c) => sum + c.width, 0)
          lastSpace = -1
        } else {
          flush()
        }
      }
      if (cell.width === 0 && line.length === 0) continue
      line.push(cell)
      used += cell.width
      if (cell.ch === ' ') lastSpace = line.length - 1
    }
    if (line.length > 0) lines.push(renderCells(line))
  }
  return lines.length === 0 ? [''] : lines
}

function renderCells(list) {
  let out = ''
  let style = ''
  for (const cell of list) {
    if (cell.style !== style) {
      out += cell.style === '' ? `${ESC}[0m` : cell.style
      style = cell.style
    }
    out += cell.ch
  }
  if (style !== '') out += `${ESC}[0m`
  return out
}

/** Cut a styled string to at most `width` cells, appending `ellipsis` when cut. */
export function truncateStyled(value, width, ellipsis = '…') {
  const limit = Math.max(0, Math.trunc(width))
  const text = String(value)
  if (visibleWidth(text) <= limit) return text
  const tail = visibleWidth(ellipsis)
  if (limit <= tail) return plainWidth(ellipsis) > limit ? '' : ellipsis
  const out = cells(text)
  let used = 0
  let kept = ''
  let style = ''
  for (const cell of out) {
    if (used + cell.width > limit - tail) break
    if (cell.style !== style) {
      kept += cell.style === '' ? `${ESC}[0m` : cell.style
      style = cell.style
    }
    kept += cell.ch
    used += cell.width
  }
  if (style !== '') kept += `${ESC}[0m`
  return kept + ellipsis
}

/** Pad a styled string with spaces to exactly `width` cells (truncating first). */
export function padStyled(value, width, align = 'left') {
  const text = truncateStyled(value, width)
  const gap = Math.max(0, width - visibleWidth(text))
  if (align === 'right') return ' '.repeat(gap) + text
  if (align === 'center') {
    const left = Math.floor(gap / 2)
    return ' '.repeat(left) + text + ' '.repeat(gap - left)
  }
  return text + ' '.repeat(gap)
}

/** Left and right halves of one row, with the right one dropped when it cannot fit. */
export function spread(left, right, width, gap = 2) {
  const leftWidth = visibleWidth(left)
  const rightWidth = visibleWidth(right)
  if (leftWidth + gap + rightWidth <= width) return left + ' '.repeat(width - leftWidth - rightWidth) + right
  if (rightWidth >= width) return truncateStyled(right, width)
  const room = width - rightWidth - gap
  if (room <= 0) return truncateStyled(right, width)
  return truncateStyled(left, room) + ' '.repeat(width - Math.min(leftWidth, room) - rightWidth) + right
}

/** Indent every line of a styled block. */
export function indentBlock(lines, prefix) {
  const pad = visibleWidth(prefix) === 0 ? prefix : prefix
  return lines.map((line) => (line === '' ? line : pad + line))
}

/** A horizontal rule of `width` cells in the given style. */
export function rule(width, style, ch = '─') {
  const count = Math.max(0, Math.trunc(width))
  if (count === 0) return ''
  return style(`${ch.repeat(count)}`)
}
