/**
 * The version this build reports.
 *
 * Read from the package manifest rather than written twice: a release bumps
 * `package.json`, and the banner, `--version` and the ACP handshake all follow.
 *
 * @module version
 */

import { readFileSync } from 'node:fs'

const manifest = JSON.parse(readFileSync(new URL('../package.json', import.meta.url), 'utf8'))

/** The package version. */
export const VERSION = String(manifest.version ?? '0.0.0')
