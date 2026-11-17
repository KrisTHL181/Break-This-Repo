/**
 * Offline source check.
 *
 * The plugin must build with nothing but Node: no bundler, no transpiler, no
 * registry. This script is that promise, executed — it syntax-checks every source
 * file, proves the manifest declares no dependencies, and proves both locale
 * tables define the same keys. It is what CI runs in place of a build.
 */

import { execFileSync } from 'node:child_process'
import { existsSync, readdirSync, readFileSync, statSync } from 'node:fs'
import { join, dirname } from 'node:path'
import { fileURLToPath } from 'node:url'

const root = dirname(dirname(fileURLToPath(import.meta.url)))
const failures = []

/** Every `.js` file under `dir`, recursively. */
function sources(dir, out = []) {
  for (const entry of readdirSync(dir)) {
    if (entry === 'node_modules' || entry === 'test') continue
    const full = join(dir, entry)
    const info = statSync(full)
    if (info.isDirectory()) sources(full, out)
    else if (entry.endsWith('.js')) out.push(full)
  }
  return out
}

// The plugin carries the shared terminal toolkit inside its tarball, so the
// check covers it from the repository layout: a syntax error there breaks the
// installed program just as surely as one under src/.
const sharedRoot = join(root, '..', 'shared')
const files = [
  ...sources(join(root, 'src')),
  ...sources(join(root, 'bin')),
  ...(existsSync(sharedRoot) ? sources(sharedRoot) : []),
  join(root, 'scripts', 'check.mjs'),
]
for (const file of files) {
  try {
    execFileSync(process.execPath, ['--check', file], { stdio: 'pipe' })
  } catch (error) {
    failures.push(`${file}: ${String(error.stderr ?? error.message).trim()}`)
  }
}

const manifest = JSON.parse(readFileSync(join(root, 'package.json'), 'utf8'))
for (const field of ['dependencies', 'optionalDependencies', 'peerDependencies']) {
  const value = manifest[field]
  if (value !== undefined && Object.keys(value).length > 0) {
    failures.push(`package.json declares ${field}: the plugin has no external dependencies by design`)
  }
}
if (!manifest.bin || manifest.bin['dsh-acp-tui'] === undefined) {
  failures.push('package.json must declare the dsh-acp-tui bin')
}
if (manifest.type !== 'module') {
  failures.push('package.json must be an ES module package')
}
// The tarball has to be installable on its own: the terminal toolkit is staged
// into `package/shared`, the licence must travel with it, and every export must
// name a file that exists after staging.
const manifestFiles = manifest.files ?? []
for (const required of ['bin', 'src', 'shared', 'LICENSE']) {
  if (!manifestFiles.includes(required)) failures.push(`package.json files[] must include ${required}`)
}
if (!existsSync(join(root, 'LICENSE'))) {
  failures.push('LICENSE must sit beside package.json so the tarball ships it')
}
for (const [subpath, target] of Object.entries(manifest.exports ?? {})) {
  if (typeof target !== 'string') continue
  const staged = target.startsWith('./shared/')
    ? join(sharedRoot, target.slice('./shared/'.length))
    : join(root, target.startsWith('./') ? target.slice(2) : target)
  if (!existsSync(staged)) failures.push(`exports["${subpath}"] points at ${target}, which is not present when staged`)
}

const i18n = await import(join(sharedRoot, 'i18n.js'))
const english = Object.keys(i18n.TABLES.en)
const chinese = Object.keys(i18n.TABLES.zh)
for (const key of english) {
  if (!chinese.includes(key)) failures.push(`i18n: zh is missing ${key}`)
}
for (const key of chinese) {
  if (!english.includes(key)) failures.push(`i18n: en is missing ${key}`)
}

if (failures.length > 0) {
  process.stderr.write(`dsh-acp-tui check failed:\n${failures.map((line) => `  - ${line}`).join('\n')}\n`)
  process.exit(1)
}
process.stdout.write(`dsh-acp-tui check passed: ${files.length} files, no external dependencies\n`)
