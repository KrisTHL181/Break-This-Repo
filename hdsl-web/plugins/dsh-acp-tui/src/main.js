/**
 * Process entry: arguments, the harness lookup, and the one terminal session.
 *
 * @module main
 */

import { stdin, stdout } from 'node:process'

import { App } from './ui/app.js'
import { locateHarness } from './dsh.js'
import { VERSION } from './version.js'

/** Printed by `--help`. Kept short: it is read in a terminal, not a manual. */
const USAGE = `dsh-acp-tui ${VERSION} — a terminal interface for DeepSeek Harness

Usage:
  dsh-acp-tui                     start a new session in the current directory
  dsh-acp-tui --resume <id>       resume a session by id
  dsh-acp-tui --version           print the version
  dsh-acp-tui --help              print this help

Environment:
  HSDL_DSH_ENTRY=<path>        the harness entry (defaults to DSH_HOME/dsh or PATH)
  HSDL_TUI_LANG=zh|en          interface language (defaults to the locale)
  HSDL_TUI_THEME=dark|light    palette (defaults to the terminal's background)
  HSDL_TUI_ACP_PROFILE=acp     the harness profile that speaks ACP

The terminal finds the harness the panel launched it for; when it is started by
hand, point HSDL_DSH_ENTRY at that instance's @deepseek-ai/dsh/lib/bin.js.
`

/**
 * Runs one terminal session and resolves with its exit code.
 *
 * @param {{argv?: string[], env?: Record<string, string | undefined>, out?: import('node:stream').Writable, input?: import('node:stream').Readable}} [options]
 * @returns {Promise<number>}
 */
export async function main(options = {}) {
  const argv = options.argv ?? process.argv.slice(2)
  const env = options.env ?? process.env
  const out = options.out ?? stdout
  const input = options.input ?? stdin

  const parsed = parseArguments(argv)
  if (parsed.error !== undefined) {
    out.write(`dsh-acp-tui: ${parsed.error}\n\n${USAGE}`)
    return 2
  }
  if (parsed.help) {
    out.write(USAGE)
    return 0
  }
  if (parsed.version) {
    out.write(`dsh-acp-tui ${VERSION}\n`)
    return 0
  }

  const harness = locateHarness({ env })
  if (harness.error !== undefined) {
    out.write(`dsh-acp-tui: ${harness.error}\n`)
    return 1
  }

  if (!input.isTTY || !out.isTTY) {
    out.write('dsh-acp-tui: this interface needs a terminal; pipe it through a PTY.\n')
    return 1
  }

  const app = new App({
    env,
    stdout: out,
    stdin: input,
    cwd: process.cwd(),
    harness,
    resume: parsed.resume,
  })
  app.start().catch((error) => {
    try {
      app.screen.leave()
    } catch {
      // the terminal is already gone
    }
    out.write(`dsh-acp-tui: ${error?.message ?? error}\n`)
    app.exitCode = 1
    app.resolveExit()
  })

  // SIGINT only reaches us when the terminal is not in raw mode yet (or the
  // signal comes from outside); in raw mode Ctrl+C is an input byte the UI reads.
  const signals = ['SIGTERM', 'SIGHUP', 'SIGINT']
  const onSignal = () => {
    app.quit(0)
  }
  for (const signal of signals) process.on(signal, onSignal)
  // Last-resort restore: nothing should reach here without `stop()` having left
  // the alternate screen, but a stray `process.exit()` must not strand a person
  // in it. `Screen.leave` is idempotent.
  const onProcessExit = () => {
    try {
      app.screen.leave()
    } catch {
      // already restored
    }
  }
  process.on('exit', onProcessExit)
  process.on('uncaughtException', (error) => {
    try {
      app.screen.leave()
    } catch {
      // already restored
    }
    out.write(`dsh-acp-tui: unexpected error: ${error?.stack ?? error}\n`)
    process.exit(1)
  })

  const code = await app.waitForExit()
  for (const signal of signals) process.removeListener(signal, onSignal)
  process.removeListener('exit', onProcessExit)
  return code
}

/**
 * Parses the command line.
 *
 * @param {string[]} argv
 * @returns {{resume: string | null, help?: boolean, version?: boolean, error?: string}}
 */
export function parseArguments(argv) {
  const result = { resume: null, help: false, version: false }
  for (let index = 0; index < argv.length; index += 1) {
    const argument = argv[index]
    if (argument === '--help' || argument === '-h') {
      result.help = true
      continue
    }
    if (argument === '--version' || argument === '-v') {
      result.version = true
      continue
    }
    if (argument === '--resume' || argument === '-r') {
      const value = argv[index + 1]
      if (value === undefined || value.startsWith('-')) {
        result.error = '--resume needs a session id'
        return result
      }
      result.resume = value
      index += 1
      continue
    }
    if (argument.startsWith('--resume=')) {
      result.resume = argument.slice('--resume='.length)
      continue
    }
    result.error = `unknown argument: ${argument}`
    return result
  }
  return result
}

/** Starts the process the way the executable entry does. */
export async function run() {
  const code = await main()
  process.exitCode = code
}
