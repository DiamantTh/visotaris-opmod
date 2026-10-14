import { defineConfig } from 'vite'
import { svelte } from '@sveltejs/vite-plugin-svelte'
import tailwindcss from '@tailwindcss/vite'
import { extname, resolve } from 'path'
import { readdirSync, unlinkSync } from 'fs'
import { fileURLToPath } from 'url'

const __dirname = fileURLToPath(new URL('.', import.meta.url))
const resDir    = resolve(__dirname, '../src/main/resources/assets/webui')

/**
 * emptyOutDir bleibt aus, damit die mitgelieferten Schriftdateien erhalten
 * bleiben. Nach einem erfolgreichen Build räumen wir daher ausschließlich
 * überholte, von Vite erzeugte JS-/CSS-Bundles weg.
 */
function cleanStaleBundles() {
  let generated = new Set()
  return {
    name: 'visotaris-clean-stale-webui-bundles',
    generateBundle(_, bundle) {
      generated = new Set(Object.keys(bundle)
        .filter(fileName => fileName.startsWith('static/app/')))
    },
    writeBundle() {
      const appDir = resolve(resDir, 'static/app')
      for (const entry of readdirSync(appDir, { withFileTypes: true })) {
        if (!entry.isFile() || !['.js', '.css'].includes(extname(entry.name))) continue
        const outputName = `static/app/${entry.name}`
        if (!generated.has(outputName)) unlinkSync(resolve(appDir, entry.name))
      }
    }
  }
}

export default defineConfig({
  plugins: [tailwindcss(), svelte(), cleanStaleBundles()],
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
