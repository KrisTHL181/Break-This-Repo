/**
 * Cordis entry for the in-process terminal.
 *
 * The bundle patch lists this module as a profile row; mounting it takes over the
 * terminal and drives one agent. It is deliberately thin: the plugin contract is
 * `name` / `inject` / `apply`, and everything the person sees lives in `app.js`.
 *
 * @module index
 */

import { TerminalApp } from './app.js'

/** Stable Cordis plugin name. */
export const name = 'dsh-inproc-tui'

/// The only service a turn cannot start without. `agentDefaultModel` and
/// `sessions` are optional in 0.2.x, so they belong to the row's `inject` in
/// `cordis.patch.yml`: a code-level inject of a missing service parks the
/// plugin forever, with no error and no terminal.
export const inject = ['agents']

/**
 * Mounts the terminal for this profile.
 *
 * @param {object} ctx the plugin context
 */
export function apply(ctx) {
  const app = new TerminalApp(ctx)
  // The profile tree may be disposed by the launcher while the terminal owns the
  // screen; restoring it is the plugin's own job.
  ctx.effect(() => () => {
    if (!app.stopping) void app.quit()
  })
  void boot(app)
}

/** @param {TerminalApp} app */
async function boot(app) {
  try {
    await app.start()
  } catch (error) {
    try {
      app.screen.leave()
    } catch {
      // the terminal may already be restored
    }
    process.stderr.write(`dsh-inproc-tui: ${error?.stack ?? error}\n`)
    await app.quit(1)
    return
  }
  // Stay mounted until the terminal really ends: `--help` and a non-TTY launch
  // resolve `start()` immediately, and an interactive run ends at `/quit`.
  await app.waitForExit()
}
