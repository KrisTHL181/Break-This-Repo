/**
 * Finding the harness this terminal drives.
 *
 * The panel always knows which dsh an instance runs and now hands the path over
 * in `HSDL_DSH_ENTRY`, so the common case is one environment lookup. The rest is
 * for the other ways a person starts the terminal — a checkout, a global `dsh`, a
 * hand-written command — where guessing from `DSH_HOME` or `PATH` is what turns
 * "it cannot find the harness" into "it just works".
 *
 * @module dsh
 */

import { existsSync } from 'node:fs'
import { delimiter, dirname, join, resolve } from 'node:path'
import { fileURLToPath } from 'node:url'

/** Environment variables that may name the harness entry, in precedence order. */
const ENTRY_VARIABLES = ['HSDL_DSH_ENTRY', 'DSH_ENTRY', 'DSH_BIN']

/** The entry file inside the published CLI package. */
const CLI_ENTRY = join('node_modules', '@deepseek-ai', 'dsh', 'lib', 'bin.js')

/**
 * Resolves the command that boots the harness.
 *
 * @param {{env?: Record<string, string | undefined>, moduleUrl?: string}} [options]
 * @returns {{command: string[], source: string, profile: string} | {error: string}}
 */
export function locateHarness(options = {}) {
  const env = options.env ?? process.env
  const moduleUrl = options.moduleUrl ?? import.meta.url
  const profile = (env.HSDL_TUI_ACP_PROFILE ?? '').trim() || 'acp'

  for (const name of ENTRY_VARIABLES) {
    const value = (env[name] ?? '').trim()
    if (value === '') continue
    if (!existsSync(value)) {
      return { error: `${name}=${value} does not exist` }
    }
    return { command: [process.execPath, resolve(value)], source: name, profile }
  }

  const home = (env.DSH_HOME ?? '').trim()
  if (home !== '') {
    const base = resolve(home)
    const candidates = [
      resolve(base, '..', 'dsh', CLI_ENTRY),
      resolve(base, CLI_ENTRY),
      resolve(base, '..', CLI_ENTRY),
    ]
    for (const candidate of candidates) {
      if (existsSync(candidate)) {
        return { command: [process.execPath, candidate], source: `DSH_HOME→${candidate}`, profile }
      }
    }
  }

  // A checkout: walk up from this package looking for the CLI in a sibling
  // node_modules, which is how a workspace install lays it out.
  let directory = dirname(fileURLToPath(moduleUrl))
  for (let depth = 0; depth < 8; depth += 1) {
    const candidate = join(directory, CLI_ENTRY)
    if (existsSync(candidate)) {
      return { command: [process.execPath, candidate], source: `sibling→${candidate}`, profile }
    }
    const parent = dirname(directory)
    if (parent === directory) break
    directory = parent
  }

  const pathValue = env.PATH ?? ''
  for (const entry of pathValue.split(delimiter)) {
    if (entry === '') continue
    const candidate = join(entry, 'dsh')
    if (existsSync(candidate)) {
      return { command: [candidate], source: 'PATH', profile }
    }
  }

  return {
    error:
      'Cannot find the dsh entry point. Set HSDL_DSH_ENTRY (or DSH_ENTRY) to the harness lib/bin.js, '
      + 'or put `dsh` on PATH.',
  }
}

/** A one-line command a person can paste to resume this session. */
export function resumeHint(entry, sessionId) {
  if (entry === undefined || entry === null) return `dsh-acp-tui --resume ${sessionId}`
  const file = entry.command[entry.command.length - 1]
  const base = file === 'dsh' ? 'dsh' : `node ${file}`
  return `HSDL_DSH_ENTRY=${file} ${base === 'dsh' ? 'dsh' : 'node'} --profile ${entry.profile} # then dsh-acp-tui --resume ${sessionId}`
}
