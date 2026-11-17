/**
 * Input decoding.
 *
 * A terminal hands over one byte stream in which a typed letter, a pasted
 * document and a cursor key can all arrive in the same chunk. This module turns
 * that stream into named key events, and it is deliberately tolerant: an escape
 * sequence it does not know is dropped rather than echoed, and a CSI-u key report
 * (what kitty-protocol terminals send for things like Shift+Enter) is decoded
 * alongside the legacy `ESC [ 13 ; 2 u`-less form.
 *
 * @module term/keys
 */

/** Named keys the UI understands. */
export const KEYS = {
  ENTER: 'enter',
  TAB: 'tab',
  SHIFT_TAB: 'shift+tab',
  BACKSPACE: 'backspace',
  DELETE: 'delete',
  LEFT: 'left',
  RIGHT: 'right',
  UP: 'up',
  DOWN: 'down',
  HOME: 'home',
  END: 'end',
  PAGE_UP: 'page-up',
  PAGE_DOWN: 'page-down',
  ESCAPE: 'escape',
  PASTE_START: 'paste-start',
  PASTE_END: 'paste-end',
}

/** Control characters that map straight onto a semantic action. */
const CONTROL = new Map([
  // Tab is a control byte like the others. Without this entry the decoder fell
  // through to "not a known control character" and dropped the key, so Tab never
  // reached either terminal — completion looked broken because it never ran.
  [0x09, 'tab'],
  [0x03, 'ctrl+c'],
  [0x04, 'ctrl+d'],
  [0x06, 'ctrl+f'],
  [0x07, 'ctrl+g'],
  [0x08, 'backspace'],
  [0x0a, 'ctrl+j'],
  [0x0d, 'enter'],
  [0x0b, 'ctrl+k'],
  [0x0c, 'ctrl+l'],
  [0x0e, 'ctrl+n'],
  [0x0f, 'ctrl+o'],
  [0x10, 'ctrl+p'],
  [0x12, 'ctrl+r'],
  [0x15, 'ctrl+u'],
  [0x17, 'ctrl+w'],
  [0x19, 'ctrl+y'],
  [0x1a, 'ctrl+z'],
  [0x7f, 'backspace'],
])

const ARROWS = new Map([
  ['A', KEYS.UP],
  ['B', KEYS.DOWN],
  ['C', KEYS.RIGHT],
  ['D', KEYS.LEFT],
  ['H', KEYS.HOME],
  ['F', KEYS.END],
])

const TILDE = new Map([
  ['1', KEYS.HOME],
  ['2', 'insert'],
  ['3', KEYS.DELETE],
  ['4', KEYS.END],
  ['5', KEYS.PAGE_UP],
  ['6', KEYS.PAGE_DOWN],
  ['7', KEYS.HOME],
  ['8', KEYS.END],
  ['200', KEYS.PASTE_START],
  ['201', KEYS.PASTE_END],
  ['Z', KEYS.SHIFT_TAB],
])

/**
 * Builds the incremental decoder.
 *
 * @param {(event: {name: string, text?: string, shift?: boolean, alt?: boolean, ctrl?: boolean}) => void} onKey
 */
export function createKeyDecoder(onKey) {
  let buffer = ''
  let pasting = false
  let pasteBuffer = ''

  const emit = (name, extra) => {
    if (pasting) {
      // While a paste is open, named keys other than its end are literal text.
      return
    }
    onKey({ name, ...extra })
  }

  const decodeCsiU = (params) => {
    // 13;2u = Shift+Enter, 13;5u = Ctrl+Enter, 9 = Tab, 27 = Escape.
    const [codeText, modText] = params.split(';')
    const code = Number(codeText)
    const mod = Number(modText ?? '1')
    // xterm 的修饰位：bit0=Shift、bit1=Alt、bit2=Ctrl（相对是 mod-1）。用位运算，别用取模——
    // 原来的 `(mod-1)%8>=1 && (mod-1)%4>=1` 会把 Ctrl（4）也判成 Shift，把 Ctrl+Shift（5）判成
    // 没有 Shift，于是 Ctrl+Enter 变成换行而不是发送。
    const bits = Number.isFinite(mod) ? mod - 1 : 0
    const shift = (bits & 1) === 1
    const alt = ((bits >> 1) & 1) === 1
    const ctrl = ((bits >> 2) & 1) === 1
    if (code === 13) return { name: KEYS.ENTER, shift, alt, ctrl }
    if (code === 9) return { name: shift ? KEYS.SHIFT_TAB : KEYS.TAB, shift, alt, ctrl }
    if (code === 27) return { name: KEYS.ESCAPE, shift, alt, ctrl }
    if (code === 127) return { name: KEYS.BACKSPACE, shift, alt, ctrl }
    if (Number.isFinite(code) && code >= 32) {
      return { name: 'char', text: String.fromCodePoint(code), shift, alt, ctrl }
    }
    return null
  }

  const consume = () => {
    while (buffer.length > 0) {
      const head = buffer[0]

      if (pasting) {
        const end = buffer.indexOf('\x1b[201~')
        if (end < 0) {
          // Keep a possible partial terminator in the buffer.
          const keep = Math.min(buffer.length, 6)
          pasteBuffer += buffer.slice(0, buffer.length - keep)
          buffer = buffer.slice(buffer.length - keep)
          return
        }
        pasteBuffer += buffer.slice(0, end)
        buffer = buffer.slice(end + 6)
        pasting = false
        const text = pasteBuffer
        pasteBuffer = ''
        onKey({ name: 'paste', text })
        continue
      }

      if (head === '\x1b') {
        const rest = buffer
        // Lone escape: give the terminal a moment to complete a sequence.
        if (rest.length === 1) return
        if (rest[1] === '[') {
          const match = /^\x1b\[([0-9;:?]*)([ -/]*)([@-~])/u.exec(rest)
          if (!match) {
            if (rest.length < 8) return
            buffer = rest.slice(1)
            continue
          }
          buffer = rest.slice(match[0].length)
          const [, params, , final] = match
          if (final === 'u') {
            const event = decodeCsiU(params)
            if (event) emit(event.name, event)
            continue
          }
          if (final === '~') {
            const name = TILDE.get(params.split(';')[0])
            if (name === KEYS.PASTE_START) {
              pasting = true
              pasteBuffer = ''
              continue
            }
            if (name) emit(name, { shift: /;2$/.test(params) })
            continue
          }
          if (final === 'A' || final === 'B' || final === 'C' || final === 'D' || final === 'H' || final === 'F') {
            const name = ARROWS.get(final)
            emit(name, { shift: /;2$/.test(params) })
            continue
          }
          if (final === 'Z') {
            emit(KEYS.SHIFT_TAB, {})
            continue
          }
          // Mouse reports, focus events, terminal replies: not for the UI.
          continue
        }
        if (rest[1] === 'O') {
          if (rest.length < 3) return
          const final = rest[2]
          buffer = rest.slice(3)
          const name = ARROWS.get(final) ?? (final === 'P' ? 'f1' : undefined)
          if (name) emit(name, {})
          continue
        }
        if (rest.length < 2) return
        // ESC followed by a printable byte is Alt+<byte>; ESC+CR is Alt+Enter.
        const next = rest[1]
        buffer = rest.slice(2)
        if (next === '\r') {
          emit(KEYS.ENTER, { alt: true })
          continue
        }
        if (next === '\x7f' || next === '\x08') {
          emit(KEYS.BACKSPACE, { alt: true })
          continue
        }
        onKey({ name: 'char', text: next, alt: true })
        continue
      }

      const code = head.codePointAt(0)
      if (code < 0x20 || code === 0x7f) {
        buffer = buffer.slice(head.length)
        const name = CONTROL.get(code)
        if (name === 'ctrl+j') {
          emit(KEYS.ENTER, { ctrl: true })
          continue
        }
        if (name === 'enter') {
          emit(KEYS.ENTER, {})
          continue
        }
        if (name) emit(name, { ctrl: true })
        continue
      }

      // Printable text: take the whole run up to the next control byte.
      let end = 0
      while (end < buffer.length) {
        const cp = buffer.codePointAt(end)
        if (cp < 0x20 || cp === 0x7f || cp === 0x1b) break
        end += cp > 0xffff ? 2 : 1
      }
      const text = buffer.slice(0, end)
      buffer = buffer.slice(end)
      onKey({ name: 'char', text })
    }
  }

  return {
    /**
     * Feeds one decoded chunk of stdin.
     * @param {string} chunk
     */
    feed(chunk) {
      buffer += chunk
      consume()
    },
    /** Whether a partial escape sequence is still pending. */
    get pending() {
      return buffer.length > 0
    },
    /** Emits a lone Escape once the terminal's ambiguity window has passed. */
    flush() {
      if (buffer === '\x1b') {
        buffer = ''
        onKey({ name: KEYS.ESCAPE })
      }
    },
  }
}
