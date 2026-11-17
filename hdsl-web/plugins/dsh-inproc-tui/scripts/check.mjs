/**
 * Offline source check for the in-process terminal.
 *
 * Same promise as the ACP plugin's check, plus the two things a *bundle* adds:
 * the patch file the profile loader reads must exist and mention this package,
 * and every peer dependency must be a harness package — a first-party bundle
 * rides on the harness it is installed into, not on npm.
 */

import { execFileSync } from 'node:child_process'
import { existsSync, readdirSync, readFileSync, statSync } from 'node:fs'
import { dirname, join } from 'node:path'
import { fileURLToPath } from 'node:url'

const root = dirname(dirname(fileURLToPath(import.meta.url)))
const failures = []

function sources(dir, out = []) {
  if (!existsSync(dir)) return out
  for (const entry of readdirSync(dir)) {
    if (entry === 'node_modules' || entry === 'test') continue
    const full = join(dir, entry)
    const info = statSync(full)
    if (info.isDirectory()) sources(full, out)
    else if (entry.endsWith('.js')) out.push(full)
  }
  return out
}

const sharedRoot = join(root, '..', 'shared')
const files = [...sources(join(root, 'src')), ...sources(sharedRoot), join(root, 'scripts', 'check.mjs')]
for (const file of files) {
  try {
    execFileSync(process.execPath, ['--check', file], { stdio: 'pipe' })
  } catch (error) {
    failures.push(`${file}: ${String(error.stderr ?? error.message).trim()}`)
  }
}

const manifest = JSON.parse(readFileSync(join(root, 'package.json'), 'utf8'))
if (manifest.dsh?.bundle?.patch === undefined) {
  failures.push('package.json must declare dsh.bundle.patch: this plugin is a profile bundle')
}
const patchFile = join(root, manifest.dsh?.bundle?.patch ?? 'cordis.patch.yml')
if (!existsSync(patchFile)) {
  failures.push(`dsh.bundle.patch points at a missing file: ${patchFile}`)
} else if (!readFileSync(patchFile, 'utf8').includes(manifest.name)) {
  failures.push(`cordis.patch.yml does not list ${manifest.name}`)
}
if (manifest.bin !== undefined) {
  failures.push('an in-process bundle must not ship a bin: the profile boots the app')
}
for (const field of ['dependencies', 'optionalDependencies']) {
  const value = manifest[field]
  if (value !== undefined && Object.keys(value).length > 0) {
    failures.push(`package.json declares ${field}: a first-party bundle depends on the harness only`)
  }
}
for (const [name, range] of Object.entries(manifest.peerDependencies ?? {})) {
  if (!name.startsWith('@deepseek-ai/')) {
    failures.push(`peer dependency ${name} is not a harness package`)
  }
  if (String(range).trim() === '') failures.push(`peer dependency ${name} has an empty range`)
}
if (!existsSync(join(root, 'LICENSE'))) {
  failures.push('LICENSE is missing: the package is GPL-3.0-or-later and the tarball carries the text')
}

// The question service is an *optional* peer. A top-level import of it makes
// the whole terminal fail to load on a harness that does not ship it, which is
// exactly the boot the shape-matched error in `src/protocol.js` avoids.
for (const file of files) {
  if (!file.endsWith('.js')) continue
  const text = readFileSync(file, 'utf8')
  if (/(?:from|import)\s*\(?\s*['"]@deepseek-ai\/dsh-user-questions['"]/u.test(text)) {
    failures.push(`${file}: imports the optional dsh-user-questions peer at module load`)
  }
}

const i18n = await import(join(sharedRoot, 'i18n.js'))
const english = Object.keys(i18n.TABLES.en)
const chinese = Object.keys(i18n.TABLES.zh)
for (const key of english) if (!chinese.includes(key)) failures.push(`i18n: zh is missing ${key}`)
for (const key of chinese) if (!english.includes(key)) failures.push(`i18n: en is missing ${key}`)

if (failures.length > 0) {
  process.stderr.write(`dsh-inproc-tui check failed:\n${failures.map((line) => `  - ${line}`).join('\n')}\n`)
  process.exit(1)
}
process.stdout.write(`dsh-inproc-tui check passed: ${files.length} files, bundle patch present, harness-only peers\n`)
