import { cp, mkdtemp, readdir, readFile, realpath, rm, writeFile } from 'node:fs/promises'
import { tmpdir } from 'node:os'
import { extname, join, resolve } from 'node:path'
import { minify } from 'terser'
import { build as viteBuild } from 'vite'

const frontendRoot = resolve(import.meta.dirname, '..')
const publicRoot = resolve(frontendRoot, 'public')
const outputRoot = resolve(frontendRoot, '../src/main/resources/static')
const temporaryRoot = await realpath(await mkdtemp(join(tmpdir(), 'photos-review-vite-')))
const temporaryPublicRoot = resolve(temporaryRoot, 'public')
const temporaryOutputRoot = resolve(temporaryRoot, 'dist')
const temporaryEntry = resolve(temporaryRoot, 'vite-entry.html')

async function findJavaScriptFiles(directory) {
  const entries = await readdir(directory, { withFileTypes: true })
  const files = await Promise.all(entries.map(async (entry) => {
    const path = resolve(directory, entry.name)
    return entry.isDirectory() ? findJavaScriptFiles(path) : [path]
  }))

  return files.flat().filter((path) => extname(path) === '.js')
}

try {
  await cp(publicRoot, temporaryPublicRoot, { recursive: true })
  await writeFile(temporaryEntry, '<!doctype html><html><body></body></html>', 'utf8')

  await viteBuild({
    configFile: false,
    root: temporaryRoot,
    publicDir: temporaryPublicRoot,
    build: {
      outDir: temporaryOutputRoot,
      emptyOutDir: true,
      sourcemap: false,
      minify: true,
      rollupOptions: {
        input: temporaryEntry,
      },
    },
  })

  await rm(resolve(temporaryOutputRoot, 'vite-entry.html'), { force: true })

  const scripts = await findJavaScriptFiles(resolve(temporaryOutputRoot, 'js'))
  await Promise.all(scripts.map(async (path) => {
    const source = await readFile(path, 'utf8')
    const result = await minify(source, {
      compress: {
        passes: 2,
        toplevel: false,
      },
      mangle: {
        toplevel: false,
      },
      format: {
        comments: false,
      },
    })

    if (typeof result.code !== 'string') {
      throw new Error(`Failed to minify ${path}`)
    }

    await writeFile(path, result.code, 'utf8')
  }))

  await rm(outputRoot, { recursive: true, force: true })
  await cp(temporaryOutputRoot, outputRoot, { recursive: true })
  console.log(`Built 8 pages and minified ${scripts.length} JavaScript files.`)
} finally {
  await rm(temporaryRoot, { recursive: true, force: true })
}
