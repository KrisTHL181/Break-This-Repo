/**
 * Colours and text styles.
 *
 * The palette is semantic rather than literal: components ask for `primary`,
 * `textDim`, `diffAdded` and never for a hex value, so a light terminal or a
 * monochrome one is a palette change and not a component change. The token set
 * follows the visual language the terminal UI aims for — one accent hue for
 * anything interactive, muted greys for chrome, green/red reserved for diffs and
 * state — which is what makes a transcript readable at a glance.
 *
 * @module term/theme
 */

const ESC = '\x1b'

/** Dark palette: tuned for a near-black terminal background. */
export const DARK = {
  primary: '#5aa9ff',
  accent: '#5bc8c0',
  text: '#dcdcdc',
  textStrong: '#f4f4f4',
  textDim: '#8a8a8a',
  textMuted: '#6b6b6b',
  border: '#565656',
  borderFocus: '#e0a63c',
  success: '#4ec87e',
  warning: '#e0a63c',
  error: '#e85a5a',
  diffAdded: '#4ec87e',
  diffRemoved: '#e85a5a',
  diffAddedStrong: '#7ad99b',
  diffRemovedStrong: '#f08a8a',
  diffGutter: '#6b6b6b',
  diffMeta: '#8a8a8a',
  roleUser: '#f2c46b',
  shellMode: '#b48ce8',
  banner: '#7fb8ff',
}

/** Light palette: the same roles, re-tuned for a white background. */
export const LIGHT = {
  primary: '#1565c0',
  accent: '#00787f',
  text: '#1c1c1c',
  textStrong: '#0d0d0d',
  textDim: '#4a4a4a',
  textMuted: '#606060',
  border: '#8a8a8a',
  borderFocus: '#8a5c00',
  success: '#1f7a44',
  warning: '#8a5c00',
  error: '#b3261e',
  diffAdded: '#1f7a44',
  diffRemoved: '#b3261e',
  diffAddedStrong: '#146232',
  diffRemovedStrong: '#8c1c17',
  diffGutter: '#606060',
  diffMeta: '#4a4a4a',
  roleUser: '#8a5c00',
  shellMode: '#5b2d8e',
  banner: '#1565c0',
}

function hexToRgb(hex) {
  const value = hex.replace('#', '')
  return [parseInt(value.slice(0, 2), 16), parseInt(value.slice(2, 4), 16), parseInt(value.slice(4, 6), 16)]
}

/**
 * Builds the theme object the UI styles with.
 *
 * @param {object} [options]
 * @param {'dark'|'light'} [options.mode] palette to start from
 * @param {boolean} [options.color] whether to emit escapes at all
 * @param {boolean} [options.unicode] whether decorative glyphs may be used
 */
export function createTheme(options = {}) {
  const state = {
    mode: options.mode === 'light' ? 'light' : 'dark',
    color: options.color !== false,
    unicode: options.unicode !== false,
    palette: options.mode === 'light' ? LIGHT : DARK,
  }

  const fg = (token, text) => {
    if (!state.color) return String(text)
    const hex = state.palette[token] ?? token
    const [r, g, b] = hexToRgb(hex)
    return `${ESC}[38;2;${r};${g};${b}m${text}${ESC}[0m`
  }
  const bg = (token, text) => {
    if (!state.color) return String(text)
    const hex = state.palette[token] ?? token
    const [r, g, b] = hexToRgb(hex)
    return `${ESC}[48;2;${r};${g};${b}m${text}${ESC}[0m`
  }
  const fgBg = (foreground, background, text) => {
    if (!state.color) return String(text)
    const fgHex = state.palette[foreground] ?? foreground
    const bgHex = state.palette[background] ?? background
    const [fr, fg, fb] = hexToRgb(fgHex)
    const [br, bgG, bb] = hexToRgb(bgHex)
    return `${ESC}[38;2;${fr};${fg};${fb};48;2;${br};${bgG};${bb}m${text}${ESC}[0m`
  }
  const sgr = (code) => (text) => (state.color ? `${ESC}[${code}m${text}${ESC}[0m` : String(text))

  const theme = {
    get mode() {
      return state.mode
    },
    get color() {
      return state.color
    },
    get palette() {
      return state.palette
    },
    setPalette(next) {
      state.palette = next
    },
    setColor(on) {
      state.color = Boolean(on)
    },
    fg,
    bg,
    fgBg,
    bold: sgr(1),
    dim: sgr(2),
    italic: sgr(3),
    underline: sgr(4),
    inverse: sgr(7),
    /** Bold and coloured in one escape pair, so the result nests cleanly. */
    strong(token, text) {
      if (!state.color) return String(text)
      const hex = state.palette[token] ?? token
      const [r, g, b] = hexToRgb(hex)
      return `${ESC}[1;38;2;${r};${g};${b}m${text}${ESC}[0m`
    },
  }
  return theme
}

/**
 * Picks the palette a launch should start with.
 *
 * `HSDL_TUI_THEME=light|dark` wins; otherwise a terminal that reports a light
 * background through `COLORFGBG` gets the light palette. Everything else is
 * dark, which is the common case.
 *
 * @param {Record<string, string | undefined>} env
 * @returns {'dark'|'light'}
 */
export function detectMode(env) {
  const chosen = (env.HSDL_TUI_THEME ?? '').trim().toLowerCase()
  if (chosen === 'light' || chosen === 'dark') return chosen
  const fgbg = (env.COLORFGBG ?? '').trim()
  const last = fgbg.split(';').pop()
  if (last === '15' || last === '7') return 'light'
  return 'dark'
}
