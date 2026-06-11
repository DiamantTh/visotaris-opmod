<script>
  import { onMount, onDestroy } from 'svelte'
  import { fade } from 'svelte/transition'
  import Icon from '@iconify/svelte'
  import Navbar from '../../components/Navbar.svelte'
  import { fmtItem, fmtInt, fmt, fmtCompact, itemIcon, hideOnError } from '../../lib/utils.js'

  // ECharts wird als globale Variable aus /static/js/echarts.min.js geladen
  /** @type {typeof import('echarts')} */
  const echarts = /** @type {any} */ (window).echarts

  const LS_RECENT = 'visotaris_history_recent'

  // ── State ──────────────────────────────────────────────────────────────────
  let materialInput   = $state('')
  let currentMaterial = $state('')
  let history         = $state(null)
  let live            = $state(null)   // Live-Preis aus /api/market/{material}
  let granularity     = $state('DAILY')
  let loading         = $state(false)
  let error           = $state(null)
  let recent          = $state([])

  // bind:this – Chart-Container-Referenzen
  let chartPriceEl = $state(null)
  let chartTxEl    = $state(null)
  let chartItemsEl = $state(null)

  let chartInstances = {}

  // ── Derived ────────────────────────────────────────────────────────────────
  const currentPoints = $derived(history ? (history[granularity] ?? []) : [])
  // Aggregierte Kennzahlen über die aktuelle Granularität (für Stat-Karten)
  const stats = $derived.by(() => {
    const pts = currentPoints
    if (!pts.length) return null
    let maxPrice = 0, minPrice = Infinity, totalTx = 0, totalItems = 0
    for (const p of pts) {
      if (p.maxPrice > maxPrice) maxPrice = p.maxPrice
      if (p.minPrice > 0 && p.minPrice < minPrice) minPrice = p.minPrice
      totalTx    += p.transactions ?? 0
      totalItems += p.items ?? 0
    }
    return {
      maxPrice,
      minPrice: minPrice === Infinity ? 0 : minPrice,
      totalTx,
      totalItems
    }
  })
  const dateRange = $derived.by(() => {
    const pts = currentPoints
    if (pts.length < 2) return '–'
    const d = ts => new Date(ts).toLocaleDateString('de-DE', { day:'2-digit', month:'2-digit', year:'2-digit' })
    return d(pts[0].timestamp) + ' – ' + d(pts[pts.length - 1].timestamp)
  })

  // ── Chart-Lifecycle ────────────────────────────────────────────────────────
  function destroyCharts() {
    Object.values(chartInstances).forEach(c => { try { c?.dispose() } catch(_) {} })
    chartInstances = {}
  }

  // Neuzeichnen wenn Datenpunkte, Granularität oder DOM-Referenzen sich ändern
  $effect(() => {
    const pts  = currentPoints   // reaktive Abhängigkeit
    const gran = granularity     // reaktive Abhängigkeit
    const e1   = chartPriceEl    // reaktive Abhängigkeit (bind:this)
    const e2   = chartTxEl
    const e3   = chartItemsEl

    if (!pts.length || !e1 || !e2 || !e3) { destroyCharts(); return }
    // requestAnimationFrame stellt sicher, dass der Container korrekte Breite hat
    requestAnimationFrame(() => renderCharts(pts, gran))

    return destroyCharts   // Cleanup beim Unmount / nächstem Re-run
  })

  // ── Daten laden ─────────────────────────────────────────────────────────────
  async function loadHistory() {
    const mat = materialInput.trim().toLowerCase()
    if (!mat) return
    loading         = true
    error           = null
    currentMaterial = mat
    history         = null
    live            = null
    destroyCharts()
    try {
      const [histRes, liveRes] = await Promise.all([
        fetch('/api/history/' + encodeURIComponent(mat)),
        fetch('/api/market/' + encodeURIComponent(mat))
      ])
      if (!histRes.ok) throw new Error('HTTP ' + histRes.status)
      history = await histRes.json()
      live    = liveRes.ok ? await liveRes.json() : null
      if (!recent.includes(mat)) {
        recent = [mat, ...recent].slice(0, 8)
        try { localStorage.setItem(LS_RECENT, JSON.stringify(recent)) } catch(_) {}
      }
    } catch (e) {
      error = 'Fehler beim Laden: ' + e.message
    } finally {
      loading = false
    }
  }

  // ── Charts rendern ──────────────────────────────────────────────────────────
  function renderCharts(points, gran) {
    destroyCharts()
    const isHourly = gran === 'HOURLY'
    const fmtTs = ts => {
      const d = new Date(ts)
      return isHourly
        ? d.toLocaleString('de-DE', { day:'2-digit', month:'2-digit', hour:'2-digit', minute:'2-digit' })
        : d.toLocaleDateString('de-DE', { day:'2-digit', month:'2-digit', year:'2-digit' })
    }
    const fmtNum = v => new Intl.NumberFormat('de-DE', { minimumFractionDigits:2, maximumFractionDigits:2 }).format(v)
    const axFmt  = v => v >= 1_000_000 ? (v/1_000_000).toFixed(1)+'M' : v >= 1_000 ? (v/1_000).toFixed(1)+'k' : v

    const labels = points.map(p => fmtTs(p.timestamp))
    const GRID   = { left:64, right:20, top:30, bottom:40 }
    const xAxis  = {
      type: 'category', data: labels,
      axisLabel: { color:'#94a3b8', fontSize:10 },
      axisLine:  { lineStyle: { color:'#334155' } },
      splitLine: { show: false }
    }
    const yLbl = { color:'#94a3b8', fontSize:10, formatter: axFmt }

    // ── Preisentwicklung ──
    chartInstances.price = echarts.init(chartPriceEl, 'dark', { backgroundColor:'transparent' })
    chartInstances.price.setOption({
      tooltip: {
        trigger:'axis', backgroundColor:'#1e293b', borderColor:'#334155',
        textStyle: { color:'#e2e8f0' },
        formatter(params) {
          const p = points[params[0].dataIndex]
          return `<b>${params[0].axisValueLabel}</b><br>`
            + `Ø Preis: <b>${fmtNum(p.avgPrice)}</b><br>`
            + `↓ Min: <b>${fmtNum(p.minPrice)}</b>&nbsp;&nbsp;↑ Max: <b>${fmtNum(p.maxPrice)}</b>`
        }
      },
      grid: GRID, xAxis,
      yAxis: { type:'value', axisLabel: yLbl, splitLine: { lineStyle:{ color:'#1e293b' } } },
      series: [
        {
          name:'Max', type:'line',
          data: points.map(p => p.maxPrice > 0 ? +p.maxPrice.toFixed(2) : null),
          smooth:true, symbol:'none', lineStyle:{ width:0 },
          areaStyle: { color:'rgba(78,158,247,0.06)' }
        },
        {
          name:'Ø Preis', type:'line',
          data: points.map(p => p.avgPrice > 0 ? +p.avgPrice.toFixed(2) : null),
          smooth:true, symbol:'none',
          lineStyle: { color:'#4e9ef7', width:2 },
          areaStyle: { color: { type:'linear', x:0, y:0, x2:0, y2:1,
            colorStops: [{ offset:0, color:'rgba(78,158,247,0.28)' }, { offset:1, color:'rgba(78,158,247,0)' }] } }
        },
        {
          name:'Min', type:'line',
          data: points.map(p => p.minPrice > 0 ? +p.minPrice.toFixed(2) : null),
          smooth:true, symbol:'none',
          lineStyle: { color:'rgba(148,163,184,0.35)', width:1, type:'dashed' }
        }
      ]
    })

    // ── Transaktionen ──
    chartInstances.tx = echarts.init(chartTxEl, 'dark', { backgroundColor:'transparent' })
    chartInstances.tx.setOption({
      tooltip: {
        trigger:'axis', backgroundColor:'#1e293b', borderColor:'#334155',
        textStyle: { color:'#e2e8f0' },
        formatter: p => `<b>${p[0].axisValueLabel}</b><br>Transaktionen: <b>${fmtInt(p[0].value ?? 0)}</b>`
      },
      grid: GRID, xAxis,
      yAxis: { type:'value', axisLabel: yLbl, splitLine: { lineStyle:{ color:'#1e293b' } } },
      series: [{
        name:'Transaktionen', type:'line',
        data: points.map(p => p.transactions),
        smooth:true, symbol:'none',
        lineStyle: { color:'#4e9ef7', width:2 },
        areaStyle: { color: { type:'linear', x:0, y:0, x2:0, y2:1,
          colorStops: [{ offset:0, color:'rgba(78,158,247,0.22)' }, { offset:1, color:'rgba(78,158,247,0)' }] } }
      }]
    })

    // ── Gehandelte Items (Balken) ──
    chartInstances.items = echarts.init(chartItemsEl, 'dark', { backgroundColor:'transparent' })
    chartInstances.items.setOption({
      tooltip: {
        trigger:'axis', backgroundColor:'#1e293b', borderColor:'#334155',
        textStyle: { color:'#e2e8f0' },
        formatter: p => `<b>${p[0].axisValueLabel}</b><br>Items: <b>${fmtInt(p[0].value ?? 0)}</b>`
      },
      grid: GRID, xAxis,
      yAxis: { type:'value', axisLabel: yLbl, splitLine: { lineStyle:{ color:'#1e293b' } } },
      series: [{
        name:'Items', type:'bar',
        data: points.map(p => p.items),
        itemStyle: { color:'#e8b030' },
        barMaxWidth: 24,
        emphasis: { itemStyle: { color:'#f5c842' } }
      }]
    })
  }

  // ── Init + Resize ──────────────────────────────────────────────────────────
  function handleResize() {
    Object.values(chartInstances).forEach(c => c?.resize())
  }

  onMount(() => {
    try { recent = JSON.parse(localStorage.getItem(LS_RECENT) || '[]') } catch(_) {}
    const m = new URLSearchParams(window.location.search).get('m')
    if (m) { materialInput = m; loadHistory() }
    window.addEventListener('resize', handleResize)
  })

  onDestroy(() => {
    window.removeEventListener('resize', handleResize)
    destroyCharts()
  })
</script>

<Navbar activePage="history" />

<div class="w-full px-4 py-3">

  <!-- ── Kopfzeile ────────────────────────────────────────────────────────── -->
  <div class="flex items-center gap-3 mb-3 flex-wrap">
    <h5 class="m-0 flex items-center gap-2 font-semibold text-base">
      <Icon icon="lucide:trending-up" width={15} style="color:var(--vi-accent)" />Preisverlauf
    </h5>
    <div class="ml-auto flex gap-2 items-center">
      <input
        type="text"
        class="search-input"
        style="width:220px"
        placeholder="Material (z.B. diamond)"
        bind:value={materialInput}
        onkeydown={(e) => e.key === 'Enter' && loadHistory()}
      >
      <button class="btn-primary" onclick={loadHistory} disabled={loading}>
        <Icon icon="lucide:search" width={13} />Laden
      </button>
    </div>
  </div>

  <!-- ── Schnellauswahl ────────────────────────────────────────────────────── -->
  {#if recent.length > 0}
    <div class="flex gap-2 mb-3 flex-wrap items-center" transition:fade>
      <span class="text-sm" style="color:var(--vi-text-muted)">Zuletzt:</span>
      {#each recent as m (m)}
        <button
          class="btn-outline btn-xs"
          onclick={() => { materialInput = m; loadHistory() }}
        >
          {fmtItem(m)}
        </button>
      {/each}
    </div>
  {/if}

  <!-- ── Analytics-Item-Header ─────────────────────────────────────────────── -->
  {#if currentMaterial}
    <div class="vi-card mb-3" transition:fade={{ duration: 150 }}>
      <div class="vi-card-header">
        <div class="flex items-center gap-3">
          <img src={itemIcon(currentMaterial)} class="item-icon-lg" alt="" onerror={hideOnError}>
          <div>
            <div class="font-bold" style="font-size:1.15rem;line-height:1.2">{fmtItem(currentMaterial)}</div>
            <div class="text-xs mt-1" style="color:var(--vi-text-muted)">Historische Preis- und Transaktionsdaten</div>
          </div>
        </div>
        <div class="flex items-center gap-2 flex-wrap">
          <div class="flex">
            {#each [['HOURLY','Stündlich'],['DAILY','Täglich'],['WEEKLY','Wöchentlich'],['MONTHLY','Monatlich']] as [key, label] (key)}
              <button type="button" class="btn-seg" class:active={granularity === key} onclick={() => granularity = key}>{label}</button>
            {/each}
          </div>
          <a href="/" class="btn-outline" style="display:inline-flex;align-items:center;gap:0.25rem;font-size:0.78rem;white-space:nowrap">
            <Icon icon="lucide:arrow-left" width={12} />Zum Markt
          </a>
        </div>
      </div>
    </div>
  {/if}

  <!-- ── Lade-Spinner ──────────────────────────────────────────────────────── -->
  {#if loading}
    <div class="loading-overlay" transition:fade>
      <div class="text-center">
        <span class="inline-block w-6 h-6 border-2 rounded-full animate-spin mb-2 mx-auto block"
              style="border-color:var(--vi-accent); border-top-color:transparent"></span>
        <div>Lade Verlauf für <strong>{currentMaterial}</strong>…</div>
      </div>
    </div>
  {/if}

  <!-- ── Fehler ────────────────────────────────────────────────────────────── -->
  {#if error && !loading}
    <div class="rounded p-3 mb-3 text-sm"
         style="background:#450a0a; border:1px solid #7f1d1d; color:#fca5a5"
         transition:fade>{error}</div>
  {/if}

  <!-- ── Leerer Zustand ────────────────────────────────────────────────────── -->
  {#if !loading && !error && !history && !currentMaterial}
    <div class="loading-overlay" style="color:var(--vi-text-muted)">
      <div class="text-center">
        <Icon icon="lucide:trending-up" width={48} class="mb-2 mx-auto block opacity-25" />
        <div>Material eingeben und <strong>Laden</strong> klicken</div>
      </div>
    </div>
  {/if}

  <!-- ── Keine Daten ───────────────────────────────────────────────────────── -->
  {#if !loading && !error && history && currentPoints.length === 0 && currentMaterial}
    <div class="rounded p-3 mb-3 text-sm"
         style="background:var(--vi-bg-card); border:1px solid var(--vi-border); color:var(--vi-text-muted)"
         transition:fade>
      Keine Verlaufsdaten für <strong>{currentMaterial}</strong> in dieser Granularität verfügbar.
    </div>
  {/if}

  <!-- ── Charts ────────────────────────────────────────────────────────────── -->
  {#if !loading && history && currentPoints.length > 0}
    <!-- Kennzahlen -->
    <div class="stat-grid mb-3" transition:fade>
      {#if live}
        <div class="stat-card live" title="Aktueller Kaufpreis">
          <div class="stat-value">{fmt(live.buy)}</div>
          <div class="stat-label">Live Kaufpreis</div>
        </div>
        <div class="stat-card live" title="Aktueller Verkaufspreis">
          <div class="stat-value">{fmt(live.sell)}</div>
          <div class="stat-label">Live Verkaufspreis</div>
        </div>
        <div class="stat-card live" title="Aktive Kauf-Aufträge">
          <div class="stat-value">{fmtInt(live.buyOrders ?? 0)}</div>
          <div class="stat-label">Kauf-Aufträge</div>
        </div>
        <div class="stat-card live" title="Aktive Verkauf-Aufträge">
          <div class="stat-value">{fmtInt(live.sellOrders ?? 0)}</div>
          <div class="stat-label">Verkauf-Aufträge</div>
        </div>
      {/if}
      {#if stats}
        <div class="stat-card" title="Höchster Preis im Zeitraum">
          <div class="stat-value">{fmt(stats.maxPrice)}</div>
          <div class="stat-label">Höchster Preis</div>
        </div>
        <div class="stat-card" title="Niedrigster Preis im Zeitraum">
          <div class="stat-value">{fmt(stats.minPrice)}</div>
          <div class="stat-label">Niedrigster Preis</div>
        </div>
        <div class="stat-card" title="Summe aller Transaktionen im Zeitraum">
          <div class="stat-value">{fmtCompact(stats.totalTx)}</div>
          <div class="stat-label">Gesamte Transaktionen</div>
        </div>
        <div class="stat-card" title="Summe aller gehandelten Items im Zeitraum">
          <div class="stat-value">{fmtCompact(stats.totalItems)}</div>
          <div class="stat-label">Gehandelte Items</div>
        </div>
      {/if}
    </div>
    <!-- Preisentwicklung -->
    <div class="vi-card mb-3" transition:fade>
      <div class="vi-card-header">
        <span class="flex items-center gap-2">
          <Icon icon="lucide:trending-up" width={14} style="color:var(--vi-accent)" />Preisentwicklung
          <small style="color:var(--vi-text-muted)">{dateRange}</small>
        </span>
      </div>
      <div class="vi-card-body">
        <div class="chart-container" bind:this={chartPriceEl}></div>
      </div>
    </div>

    <!-- Transaktionen -->
    <div class="vi-card mb-3" transition:fade>
      <div class="vi-card-header">
        <span class="flex items-center gap-2">
          <Icon icon="lucide:arrow-left-right" width={14} style="color:var(--vi-accent)" />Transaktionen
        </span>
      </div>
      <div class="vi-card-body">
        <div class="chart-container" bind:this={chartTxEl}></div>
      </div>
    </div>

    <!-- Gehandelte Items -->
    <div class="vi-card" transition:fade>
      <div class="vi-card-header">
        <span class="flex items-center gap-2">
          <Icon icon="lucide:boxes" width={14} style="color:#fbbf24" />Gehandelte Items
        </span>
      </div>
      <div class="vi-card-body">
        <div class="chart-container" bind:this={chartItemsEl}></div>
      </div>
      <div class="vi-card-footer flex justify-between">
        <span>{currentPoints.length} Datenpunkte</span>
        {#if currentPoints.length > 0}<span>Zeitraum: {dateRange}</span>{/if}
      </div>
    </div>

  {/if}

</div>
