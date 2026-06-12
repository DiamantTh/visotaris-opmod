import { defineConfig } from 'vite'
import { svelte } from '@sveltejs/vite-plugin-svelte'
import tailwindcss from '@tailwindcss/vite'
import { resolve } from 'path'
import { fileURLToPath } from 'url'

const __dirname = fileURLToPath(new URL('.', import.meta.url))
const resDir    = resolve(__dirname, '../src/main/resources/assets/webui')

/** Injiziert externe (nicht zu bundelnde) Scripts nach dem HTML-Transform,
 *  damit Vite sie beim Parsen nicht sieht und keine Warnung ausgibt. */
const injectExternalScripts = {
  name: 'inject-external-scripts',
  transformIndexHtml: {
    order: 'post',
    handler(html, ctx) {
      if (!ctx.filename.endsWith('history.html')) return html
      return html.replace(
        '</body>',
        '  <!-- ECharts: lokale Datei, kein CDN, extern zu Rollup -->\n  <script src="/static/js/echarts.min.js"><\/script>\n</body>'
      )
    }
  }
}

export default defineConfig({
  plugins: [tailwindcss(), svelte(), injectExternalScripts],
  base: '/',
  publicDir: false,

  build: {
    outDir:       resDir,
    emptyOutDir:  false,   // Bootstrap/ECharts/Fonts in static/ behalten
    // Eigene JS/CSS-Bundles nach static/app/ – kein Hash-Suffix für stabile Namen
    rollupOptions: {
      // ECharts bleibt extern (existing static/js/echarts.min.js)
      external: ['echarts'],
      input: {
        market:  resolve(__dirname, 'index.html'),
        history: resolve(__dirname, 'history.html'),
        shard:   resolve(__dirname, 'shard.html'),
      },
      output: {
        globals:        { echarts: 'echarts' },
        entryFileNames: 'static/app/[name].js',
        chunkFileNames: 'static/app/[name].js',
        assetFileNames: 'static/app/[name][extname]'
      }
    }
  }
})
