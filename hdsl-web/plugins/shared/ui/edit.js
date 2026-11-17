/**
 * The keys a text field owns, applied to a composer.
 *
 * Both terminals keep a composer under the transcript, and the same handful of
 * keys edits it. The mapping lives here rather than twice in the two apps
 * because one caller cannot afford to drift: the inline `/` command menu. While
 * that menu is open the line must keep growing so the menu can filter itself;
 * arrows and enter still drive the list, and everything else edits the text.
 *
 * @module ui/edit
 */

/**
 * Applies one editing key to the composer.
 *
 * Only keys that edit or move within the text are handled; navigation keys the
 * two apps treat differently (arrows, page-up/page-down) are left to the caller,
 * and so is anything that is not text editing at all.
 *
 * @param {import('./composer.js').Composer} composer the composer to edit
 * @param {{name: string, text?: string}} event the decoded key
 * @param {number} width the composer's text width, for line-edge moves
 * @returns {boolean} whether the key was an editing key
 */
export function applyEditKey(composer, event, width) {
  const { name } = event
  if (name === 'char') {
    composer.insert(event.text ?? '')
    return true
  }
  if (name === 'paste') {
    // Pasted text is the user's text: CRLF is normalised by the composer, and
    // nothing else is trimmed. An empty paste is still a paste (it is handled,
    // not passed on to a dialog that would do something else with the key).
    composer.paste(event.text ?? '')
    return true
  }
  if (name === 'backspace') {
    composer.backspace()
    return true
  }
  if (name === 'delete') {
    composer.deleteForward()
    return true
  }
  if (name === 'home' || name === 'end') {
    composer.moveLineEdge(name, width)
    return true
  }
  if (name === 'ctrl+a') {
    composer.moveLineEdge('home', width)
    return true
  }
  if (name === 'ctrl+e') {
    composer.moveLineEdge('end', width)
    return true
  }
  if (name === 'ctrl+u') {
    composer.deleteToLineStart(width)
    return true
  }
  if (name === 'ctrl+k') {
    composer.deleteToEnd()
    return true
  }
  if (name === 'ctrl+w') {
    composer.deleteWord()
    return true
  }
  return false
}
