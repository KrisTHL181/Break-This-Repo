/**
 * Version reported by the profile's `--help` and the banner.
 *
 * @module version
 */

import { readFileSync } from 'node:fs'

const manifest = JSON.parse(readFileSync(new URL('../package.json', import.meta.url), 'utf8'))

/** The package version. */
export const VERSION = String(manifest.version ?? '0.0.0')
