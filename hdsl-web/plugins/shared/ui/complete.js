/**
 * Tab completion for the composer.
 *
 * Two levels, because a terminal has two kinds of things to complete: the
 * command itself (`/per…` → `/permission`) and whatever that command takes
 * (`/effort h…` → `high`, `/model acme/…` → the models of that route). `@`
 * starts a third: a path in the workspace.
 *
 * Everything that knows the candidates is a callback the terminal supplies, so
 * this module stays free of `fs`, of the harness, and of either app's state —
 * and can be tested without a terminal.
 *
 * @module ui/complete
 */

/**
 * Splits the text before the caret into the token being completed and the words
 * before it.
 *
 * @param {string} text the whole composer text
 * @param {number} caret the caret position
 * @returns {{token: string, start: number, index: number, words: string[]}} the token, where it starts, which word it is, and the words before it
 */
export function tokenAt(text, caret) {
  const before = String(text ?? '').slice(0, Math.max(0, Math.min(caret, String(text ?? '').length)))
  const match = /(\S*)$/u.exec(before)
  const token = match === null ? '' : match[1]
  const start = before.length - token.length
  const words = before.slice(0, start).trim().split(/\s+/u).filter((word) => word !== '')
  return { token, start, end: before.length, index: words.length, words }
}

/**
 * Finds the candidates for the token under the caret.
 *
 * @param {{
 *   text: string,
 *   caret: number,
 *   resolve: (request: {token: string, index: number, words: string[], command: string}) => string[],
 * }} request the composer state and the terminal's candidate source
 * @returns {{start: number, end: number, options: string[]} | null} the completion, or `null` when there is nothing to offer
 */
export function completionFor(request) {
  const { token, start, end, index, words } = tokenAt(request.text, request.caret)
  const command = words.length > 0 && words[0].startsWith('/') ? words[0].slice(1) : ''
  let options = []
  try {
    options = request.resolve({ token, index, words, command }) ?? []
  } catch {
    // A source that cannot read its candidates (no persistence, no service) is
    // not an error: there is simply nothing to complete right now.
    options = []
  }
  const unique = [...new Set(options.filter((option) => typeof option === 'string' && option !== ''))]
  // The prefix is what the person already typed; an option that does not share it
  // would replace text rather than extend it.
  const matching = unique.filter((option) => option.startsWith(token) && option !== token)
  if (matching.length === 0) return null
  return { start, end, options: matching }
}

/**
 * Applies one candidate to the composer text.
 *
 * @param {{text: string, caret: number}} composer the current composer state
 * @param {{start: number, end: number}} range where the token is
 * @param {string} value the candidate
 * @returns {{text: string, caret: number}} the new state
 */
export function applyCompletion(composer, range, value) {
  const text = `${composer.text.slice(0, range.start)}${value}${composer.text.slice(range.end)}`
  return { text, caret: range.start + value.length }
}

/**
 * The next candidate to try, cycling through the list.
 *
 * A first Tab takes the first candidate (so one press is never a no-op); each
 * further Tab on the same range moves on.
 *
 * @param {{options: string[], index?: number} | null} previous the state from the last Tab
 * @param {{start: number, end: number, options: string[]}} fresh the candidates now
 * @returns {{option: string, index: number}} the candidate to apply
 */
export function nextCandidate(previous, fresh) {
  const same = previous !== null
    && previous !== undefined
    && previous.start === fresh.start
    && previous.end === fresh.end
    && previous.options.length === fresh.options.length
    && previous.options.every((option, index) => option === fresh.options[index])
  const index = same ? ((previous.index ?? 0) + 1) % fresh.options.length : 0
  return { option: fresh.options[index], index }
}

/**
 * The idle line that advertises the candidates.
 *
 * @param {{options: string[], index?: number}} completion the current completion
 * @param {number} width how much room there is
 * @returns {string} a one-line list, or `''` when there is nothing to show
 */
export function describeCandidates(completion, width) {
  if (completion === null || completion === undefined || completion.options.length === 0) return ''
  const parts = completion.options.slice(0, 6).map((option, index) => `${index + 1}. ${option}`)
  const text = parts.join('   ')
  return text.length <= width ? text : `${text.slice(0, Math.max(0, width - 1))}…`
}
