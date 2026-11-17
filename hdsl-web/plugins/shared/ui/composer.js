/**
 * The message composer.
 *
 * A multi-line editor that wraps to the pane and keeps a caret index into the
 * plain text, so movement, deletion and history all work on one coordinate
 * system while rendering works on another. It owns no colours and no layout
 * beyond its own text: the renderer asks for wrapped lines and the caret
 * position and paints them.
 *
 * @module ui/composer
 */

import { codePointWidth, plainWidth } from '../term/text.js'

/** Longest history the composer keeps for one run. */
const HISTORY_LIMIT = 200

/** One wrapped display line and the text range it covers. */
class Line {
  constructor(start, end, display) {
    this.start = start
    this.end = end
    this.display = display
  }
}

/**
 * The character ending at `index`, as a string.
 *
 * `codePointAt(index - 1)` looks *forward*, so on an astral character it returns the low
 * half of the surrogate pair and deleting "one character" removes half of it — that leaves
 * a lone surrogate in the text, which the agent then receives as a replacement character.
 * Walking the pair together is the whole job of this helper.
 *
 * @param {string} text
 * @param {number} index
 * @returns {string} one character (one or two code units), or '' at the start
 */
function charBefore(text, index) {
  if (index <= 0) return ''
  const low = text.charCodeAt(index - 1)
  if (low >= 0xdc00 && low <= 0xdfff && index >= 2) {
    const high = text.charCodeAt(index - 2)
    if (high >= 0xd800 && high <= 0xdbff) return text.slice(index - 2, index)
  }
  return text.slice(index - 1, index)
}

/**
 * Wraps text to `width` cells for display, remembering source offsets.
 *
 * @param {string} text
 * @param {number} width
 * @returns {Line[]}
 */
export function layoutText(text, width) {
  const limit = Math.max(1, width)
  const lines = []
  let start = 0
  let index = 0
  let used = 0
  let lastSpace = -1
  while (index <= text.length) {
    if (index === text.length) {
      lines.push(new Line(start, index, text.slice(start, index)))
      break
    }
    const ch = String.fromCodePoint(text.codePointAt(index))
    if (ch === '\n') {
      lines.push(new Line(start, index, text.slice(start, index)))
      index += 1
      start = index
      used = 0
      lastSpace = -1
      continue
    }
    const cell = codePointWidth(ch.codePointAt(0))
    if (used + cell > limit && index > start) {
      const breakAt = lastSpace > start ? lastSpace : index
      lines.push(new Line(start, breakAt, text.slice(start, breakAt)))
      start = lastSpace > start ? breakAt + 1 : breakAt
      index = start
      used = 0
      lastSpace = -1
      continue
    }
    if (ch === ' ') lastSpace = index
    used += cell
    index += ch.length
  }
  return lines.length === 0 ? [new Line(0, 0, '')] : lines
}

/** The composer's editable buffer. */
export class Composer {
  constructor(options = {}) {
    this.text = options.text ?? ''
    this.caret = this.text.length
    this.history = []
    this.historyIndex = -1
    this.draft = ''
    this.maxLines = options.maxLines ?? 8
  }

  /** Replaces the whole buffer (used by history and by tests). */
  setText(text) {
    this.text = String(text)
    this.caret = this.text.length
  }

  /** Whether there is nothing to send. */
  get empty() {
    return this.text.trim() === ''
  }

  /** Inserts typed text at the caret. */
  insert(text) {
    if (text === '') return
    this.text = this.text.slice(0, this.caret) + text + this.text.slice(this.caret)
    this.caret += text.length
  }

  /**
   * Inserts pasted text at the caret.
   *
   * Pasted content arrives as whatever the terminal sent, which includes the CRLF a
   * copied line ends with; a composer that kept them would send `\r` to the agent. Line
   * endings are normalised, everything else is inserted verbatim — a paste is the user's
   * text, not something to trim or reformat.
   *
   * @param {string} text the pasted text
   * @returns {boolean} whether anything was inserted
   */
  paste(text) {
    const normalised = String(text ?? '').replace(/\r\n?/g, '\n')
    if (normalised === '') return false
    this.insert(normalised)
    return true
  }

  /** Inserts a line break. */
  newline() {
    this.insert('\n')
  }

  /** Removes the character before the caret. */
  backspace() {
    if (this.caret === 0) return
    const before = this.text.slice(0, this.caret)
    const removed = charBefore(this.text, this.caret)
    this.text = before.slice(0, before.length - removed.length) + this.text.slice(this.caret)
    this.caret -= removed.length
  }

  /** Removes the character after the caret. */
  deleteForward() {
    if (this.caret >= this.text.length) return
    const after = this.text.slice(this.caret)
    const removed = String.fromCodePoint(after.codePointAt(0))
    this.text = this.text.slice(0, this.caret) + after.slice(removed.length)
  }

  /**
   * Moves the caret one cell, keeping a column when moving vertically.
   * @param {'left'|'right'|'up'|'down'} direction
   * @param {number} width
   */
  move(direction, width) {
    const lines = layoutText(this.text, width)
    const position = this.positionOf(lines)
    if (direction === 'left') {
      if (this.caret === 0) return
      this.caret -= charBefore(this.text, this.caret).length
      return
    }
    if (direction === 'right') {
      if (this.caret >= this.text.length) return
      const after = this.text.slice(this.caret)
      const removed = String.fromCodePoint(after.codePointAt(0))
      this.caret += removed.length
      return
    }
    const target = direction === 'up' ? position.line - 1 : position.line + 1
    if (target < 0 || target >= lines.length) return
    const line = lines[target]
    let index = line.start
    let used = 0
    while (index < line.end && used < position.column) {
      const ch = String.fromCodePoint(this.text.codePointAt(index))
      const cell = codePointWidth(ch.codePointAt(0))
      if (used + cell > position.column) break
      used += cell
      index += ch.length
    }
    this.caret = index
  }

  /** Moves the caret to the start or end of the visual line. */
  moveLineEdge(edge, width) {
    const lines = layoutText(this.text, width)
    const position = this.positionOf(lines)
    this.caret = edge === 'home' ? lines[position.line].start : lines[position.line].end
  }

  /** Deletes from the caret back to the start of the visual line. */
  deleteToLineStart(width) {
    const lines = layoutText(this.text, width)
    const position = this.positionOf(lines)
    const from = lines[position.line].start
    this.text = this.text.slice(0, from) + this.text.slice(this.caret)
    this.caret = from
  }

  /** Deletes from the caret to the end of the buffer. */
  deleteToEnd() {
    this.text = this.text.slice(0, this.caret)
  }

  /** Deletes the word before the caret. */
  deleteWord() {
    const before = this.text.slice(0, this.caret)
    const trimmed = before.replace(/\s*\S*$/, '')
    this.text = trimmed + this.text.slice(this.caret)
    this.caret = trimmed.length
  }

  /** Prepends a `!` shell prompt or refuses when the buffer already has text. */
  startShellMode() {
    if (this.text.trim() !== '') return false
    this.text = '! '
    this.caret = this.text.length
    return true
  }

  /**
   * Recalls an earlier submission. Only meaningful while editing the first line.
   * @param {'up'|'down'} direction
   */
  recall(direction) {
    if (this.history.length === 0) return
    if (direction === 'up') {
      if (this.historyIndex === -1) {
        this.draft = this.text
        this.historyIndex = this.history.length - 1
      } else if (this.historyIndex > 0) {
        this.historyIndex -= 1
      }
      this.setText(this.history[this.historyIndex] ?? '')
      return
    }
    if (this.historyIndex === -1) return
    if (this.historyIndex < this.history.length - 1) {
      this.historyIndex += 1
      this.setText(this.history[this.historyIndex] ?? '')
      return
    }
    this.historyIndex = -1
    this.setText(this.draft)
    this.draft = ''
  }

  /** Records the current text as sent and clears the buffer. */
  submit() {
    const value = this.text
    const trimmed = value.trim()
    if (trimmed !== '') {
      this.history.push(value)
      if (this.history.length > HISTORY_LIMIT) this.history.shift()
    }
    this.text = ''
    this.caret = 0
    this.historyIndex = -1
    this.draft = ''
    return value
  }

  /**
   * Where the caret sits in the wrapped layout.
   * @private
   */
  positionOf(lines) {
    for (let index = 0; index < lines.length; index += 1) {
      const line = lines[index]
      if (this.caret <= line.end) {
        return { line: index, column: plainWidth(this.text.slice(line.start, this.caret)) }
      }
    }
    const last = lines[lines.length - 1]
    return { line: lines.length - 1, column: plainWidth(this.text.slice(last.start, this.caret)) }
  }

  /**
   * The display lines and caret for painting.
   *
   * @param {number} width content width in cells
   * @returns {{lines: string[], caret: {row: number, column: number}, total: number}}
   */
  view(width) {
    const lines = layoutText(this.text, width)
    const position = this.positionOf(lines)
    return {
      lines: lines.map((line) => line.display),
      caret: { row: position.line, column: position.column },
      total: lines.length,
    }
  }

  /** Height the composer needs at this width, capped at `maxLines`. */
  height(width) {
    return Math.min(this.maxLines, layoutText(this.text, width).length)
  }
}
