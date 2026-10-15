import { defineConfig } from 'vite'
import { svelte } from '@sveltejs/vite-plugin-svelte'
import tailwindcss from '@tailwindcss/vite'
import { extname, resolve } from 'path'
import { brotliCompressSync, constants as zlibConstants } from 'node:zlib'
import { readFile, readdir, unlink, writeFile } from 'node:fs/promises'
import { fileURLToPath } from 'url'
import { Zstd } from '@hpcc-js/wasm-zstd'

const __dirname = fileURLToPath(new URL('.', import.meta.url))
const resDir    = resolve(__dirname, '../src/main/resources/assets/webui')
const compressedExtensions = new Set(['.js', '.css', '.html', '.json', '.svg'])
const compressionVariants = ['.br', '.zst']
const generatedHtml = new Set([
  'index.html', 'auctions.html', 'history.html', 'shard.html',
  'redcoins.html', 'merchant.html', 'system.html', 'settings.html'
])

/**
 * emptyOutDir bleibt aus, damit die mitgelieferten Schriftdateien erhalten
 * bleiben. Nach einem erfolgreichen Build erzeugen wir für textbasierte
 * WebUX-Dateien Brotli- und Zstd-Varianten und räumen nur veraltete, von Vite
 * erzeugte JS-/CSS-/HTML-Dateien mitsamt ihrer Varianten weg.
 */
function webAssetCompression() {
  let generated = new Set()
  return {
    name: 'visotaris-precompress-webui-assets',
    generateBundle(_, bundle) {
      // Vite emits HTML outside Rollup's bundle map, so list the known entry
      // documents explicitly alongside Rollup's JS/CSS assets.
      generated = new Set([...Object.keys(bundle), ...generatedHtml])
    },
    async writeBundle() {
      const zstd = await Zstd.load()
      const compressedAssets = [...generated].filter(isCompressibleAsset)
      for (const asset of compressedAssets) {
        const rawPath = resolve(resDir, asset)
        const raw = normalizeGeneratedSvelteWhitespace(asset, await readFile(rawPath))
        await writeFile(rawPath, raw)
        const brotli = brotliCompressSync(raw, {
          params: {
            [zlibConstants.BROTLI_PARAM_MODE]: zlibConstants.BROTLI_MODE_TEXT,
            [zlibConstants.BROTLI_PARAM_QUALITY]: 9
          }
        })
        const zstandard = zstd.compress(raw, 12)
        await Promise.all([
          writeFile(resolve(resDir, `${asset}.br`), brotli),
          writeFile(resolve(resDir, `${asset}.zst`), zstandard)
        ])
      }

      const kept = new Set(generated)
      for (const asset of compressedAssets) {
        for (const variant of compressionVariants) kept.add(`${asset}${variant}`)
      }
      await cleanStaleAssets(resDir, '', kept)
      await cleanStaleAssets(resolve(resDir, 'static/app'), 'static/app', kept)
    }
  }
}

function normalizeGeneratedSvelteWhitespace(fileName, content) {
  if (fileName !== 'static/app/Navbar.js') return content
  // Svelte currently emits one trailing space/tab inside this generated template
  // literal. Keep the tracked stable bundle whitespace-clean before compressing.
  return Buffer.from(content.toString().replace(/(var wi=\[\.\.\.`) \t\n/, '$1\n'))
}

function isCompressibleAsset(fileName) {
  return isGeneratedWebAsset(fileName) && compressedExtensions.has(extname(fileName))
}

function isGeneratedWebAsset(fileName) {
  const logicalName = compressionVariants.reduce(
    (name, variant) => name.endsWith(variant) ? name.slice(0, -variant.length) : name,
    fileName
  )
  return logicalName.startsWith('static/app/')
    ? ['.js', '.css'].includes(extname(logicalName))
    : !logicalName.includes('/') && extname(logicalName) === '.html'
}

async function cleanStaleAssets(directory, prefix, kept) {
  for (const entry of await readdir(directory, { withFileTypes: true })) {
    if (!entry.isFile()) continue
    const outputName = prefix ? `${prefix}/${entry.name}` : entry.name
    if (isGeneratedWebAsset(outputName) && !kept.has(outputName)) {
      await unlink(resolve(directory, entry.name))
    }
  }
}

export default defineConfig({
  plugins: [tailwindcss(), svelte(), webAssetCompression()],
  base: '/',
  publicDir: false,

  build: {
    outDir:       resDir,
    emptyOutDir:  false,   // Zusätzliche Fonts in static/ behalten
    // Eigene JS/CSS-Bundles nach static/app/ – kein Hash-Suffix für stabile Namen
    rollupOptions: {
      input: {
        market:  resolve(__dirname, 'index.html'),
        auctions: resolve(__dirname, 'auctions.html'),
        history: resolve(__dirname, 'history.html'),
        shard:   resolve(__dirname, 'shard.html'),
        redcoins: resolve(__dirname, 'redcoins.html'),
        merchant: resolve(__dirname, 'merchant.html'),
        system: resolve(__dirname, 'system.html'),
        settings: resolve(__dirname, 'settings.html'),
      },
      output: {
        entryFileNames: 'static/app/[name].js',
        chunkFileNames: 'static/app/[name].js',
        assetFileNames: 'static/app/[name][extname]'
      }
    }
  }
})
