/**
 * The terminal itself: alternate screen, frame painting, resizes.
 *
 * Painting is line-diffed. A frame is an array of already-styled rows of exactly
 * the terminal width; only rows that differ from the previous frame are written,
 * each one anchored with an absolute cursor position so a wrapped or wide
 * character can never desynchronise the grid. That keeps a 40-row frame over a
 * WebSocket terminal to the handful of rows that actually changed, which matters
 * for the panel's web-shell where every byte is a socket frame.
 *
 * @module term/screen
 */

const ESC = '\x1b'
const SYNC_ON = `${ESC}[?2026h`
const SYNC_OFF = `${ESC}[?2026l`

const ALT_ON = `${ESC}[?1049h`
const ALT_OFF = `${ESC}[?1049l`
const CURSOR_HIDE = `${ESC}[?25l`
const CURSOR_SHOW = `${ESC}[?25h`
const PASTE_ON = `${ESC}[?2004h`
const PASTE_OFF = `${ESC}[?2004l`

/** A terminal screen that can be painted frame by frame. */
export class Screen {
  /**
   * @param {import('node:stream').Writable} out
   * @param {import('node:stream').Readable} input
   */
  constructor(out, input) {
    this.out = out
    this.input = input
    this.previous = []
    this.lastCursorMove = ''
    this.entered = false
    this.columns = 80
    this.rows = 24
    this.resized = () => {}
    this.onResize = () => {
      this.measure()
      this.previous = []
      this.resized()
    }
  }

  /** Reads the current size from the tty, falling back to the environment. */
  measure() {
    const columns = Number(this.out.columns ?? this.measureEnv('COLUMNS') ?? 80)
    const rows = Number(this.out.rows ?? this.measureEnv('LINES') ?? 24)
    this.columns = Number.isFinite(columns) && columns > 0 ? Math.trunc(columns) : 80
    this.rows = Number.isFinite(rows) && rows > 0 ? Math.trunc(rows) : 24
    return { columns: this.columns, rows: this.rows }
  }

  /** @private */
  measureEnv(name) {
    const value = Number(process.env[name])
    return Number.isFinite(value) && value > 0 ? value : undefined
  }

  /** Enters the alternate screen and starts listening for resizes. */
  enter() {
    if (this.entered) return
    this.entered = true
    this.measure()
    this.out.write(ALT_ON + CURSOR_HIDE + PASTE_ON)
    process.on('SIGWINCH', this.onResize)
  }

  /** Restores the terminal exactly as it was found. */
  leave() {
    if (!this.entered) return
    this.entered = false
    process.removeListener('SIGWINCH', this.onResize)
    this.out.write(`${ESC}[0m` + PASTE_OFF + CURSOR_SHOW + ALT_OFF)
  }

  /**
   * Paints one frame.
   *
   * @param {string[]} rows styled rows, one per terminal line
   * @param {{row: number, column: number} | null} [cursor] 0-based caret position
   */
  paint(rows, cursor = null) {
    if (!this.entered) return
    const height = this.rows
    const width = this.columns

    // Nothing changed: write nothing at all. Besides being free, this is what
    // keeps an input method alive. Every frame used to end with an absolute
    // cursor move, and an idle screen still painted once a second — a browser
    // terminal repositions its IME helper on every write, so a Chinese
    // composition could be dropped before it was ever committed.
    let cursorMove = ''
    if (cursor !== null) {
      const row = Math.max(1, Math.min(height, cursor.row + 1))
      const column = Math.max(1, Math.min(width, cursor.column + 1))
      cursorMove = `${ESC}[${row};${column}H`
    }
    const shown = rows.slice(0, height)
    const unchanged = this.previous.length === shown.length
      && shown.every((row, index) => this.previous[index] === row)
      && cursorMove === this.lastCursorMove
    if (unchanged) return

    let frame = SYNC_ON
    for (let index = 0; index < height; index += 1) {
      const row = shown[index] ?? ''
      if (this.previous[index] === row) continue
      frame += `${ESC}[${index + 1};1H` + row + `${ESC}[K`
    }
    if (this.previous.length > height) {
      for (let index = height; index < this.previous.length; index += 1) {
        frame += `${ESC}[${index + 1};1H${ESC}[K`
      }
    }
    if (cursor !== null) {
      frame += cursorMove + CURSOR_SHOW
    } else {
      frame += CURSOR_HIDE
    }
    frame += SYNC_OFF
    this.previous = shown
    this.lastCursorMove = cursorMove
    this.out.write(frame)
  }

  /** Clears everything the previous frame drew. */
  clear() {
    this.previous = []
    this.lastCursorMove = ''
    if (this.entered) this.out.write(`${ESC}[2J${ESC}[H`)
  }
}
