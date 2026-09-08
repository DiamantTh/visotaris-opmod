import { defineConfig } from 'vite'
import { svelte } from '@sveltejs/vite-plugin-svelte'
import tailwindcss from '@tailwindcss/vite'
import { resolve } from 'path'
import { fileURLToPath } from 'url'

const __dirname = fileURLToPath(new URL('.', import.meta.url))
const resDir    = resolve(__dirname, '../src/main/resources/assets/webui')

export default defineConfig({
  plugins: [tailwindcss(), svelte()],
  base: '/',
  publicDir: false,

  build: {
    outDir:       resDir,
    emptyOutDir:  false,   // Zusätzliche Fonts in static/ behalten
    // Eigene JS/CSS-Bundles nach static/app/ – kein Hash-Suffix für stabile Namen
    rollupOptions: {
      input: {
        market:  resolve(__dirname, 'index.html'),
        history: resolve(__dirname, 'history.html'),
        shard:   resolve(__dirname, 'shard.html'),
        redcoins: resolve(__dirname, 'redcoins.html'),
        merchant: resolve(__dirname, 'merchant.html'),
        system: resolve(__dirname, 'system.html'),
      },
      output: {
        entryFileNames: 'static/app/[name].js',
        chunkFileNames: 'static/app/[name].js',
        assetFileNames: 'static/app/[name][extname]'
      }
    }
  }
})
