<script>
  import { onMount, onDestroy } from 'svelte'
  import { fade } from 'svelte/transition'
  import Navbar from '../../components/Navbar.svelte'
  import { fmtItem, fmtInt, fmt } from '../../lib/utils.js'

  // ECharts wird als globale Variable aus /static/js/echarts.min.js geladen
  /** @type {typeof import('echarts')} */
  const echarts = /** @type {any} */ (window).echarts

  const LS_RECENT = 'visotaris_history_recent'

  // ── State ──────────────────────────────────────────────────────────────────
  let materialInput   = $state('')
  let currentMaterial = $state('')
  let history         = $state(null)
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
    destroyCharts()
    try {
      const res = await fetch('/api/history/' + encodeURIComponent(mat))
      if (!res.ok) throw new Error('HTTP ' + res.status)
      history = await res.json()
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

<div class="container-fluid py-3">

  <!-- ── Kopfzeile ────────────────────────────────────────────────────────── -->
  <div class="d-flex align-items-center gap-3 mb-3 flex-wrap">
    <h5 class="mb-0"><i class="bi bi-graph-up me-2 text-info"></i>Preisverlauf</h5>
    {#if currentMaterial}
      <div class="d-flex align-items-center gap-2" transition:fade>
        <img
          src="/api/icon/{currentMaterial}"
          class="item-icon-lg" alt=""
          onerror={(e) => e.currentTarget.style.display = 'none'}
        >
        <span class="fw-semibold fs-6">{fmtItem(currentMaterial)}</span>
      </div>
    {/if}
    <div class="ms-auto d-flex gap-2 align-items-center">
      <input
        type="text"
        class="form-control form-control-sm search-input"
        placeholder="Material (z.B. diamond)"
        bind:value={materialInput}
        onkeydown={(e) => e.key === 'Enter' && loadHistory()}
        style="width:220px"
      >
      <button class="btn btn-sm btn-info" onclick={loadHistory} disabled={loading}>
        <i class="bi bi-search me-1"></i>Laden
      </button>
    </div>
  </div>

  <!-- ── Schnellauswahl ────────────────────────────────────────────────────── -->
  {#if recent.length > 0}
    <div class="d-flex gap-2 mb-3 flex-wrap" transition:fade>
      <span class="text-muted small align-self-center">Zuletzt:</span>
      {#each recent as m (m)}
        <button
          class="btn btn-xs btn-outline-secondary"
          onclick={() => { materialInput = m; loadHistory() }}
        >
          {fmtItem(m)}
        </button>
      {/each}
    </div>
  {/if}

  <!-- ── Lade-Spinner ──────────────────────────────────────────────────────── -->
  {#if loading}
    <div class="loading-overlay" transition:fade>
      <div class="text-center">
        <div class="spinner-border text-info mb-2" role="status"></div>
        <div>Lade Verlauf für <strong>{currentMaterial}</strong>…</div>
      </div>
    </div>
  {/if}

  <!-- ── Fehler ────────────────────────────────────────────────────────────── -->
  {#if error && !loading}
    <div class="alert alert-danger" transition:fade>{error}</div>
  {/if}

  <!-- ── Leerer Zustand ────────────────────────────────────────────────────── -->
  {#if !loading && !error && !history && !currentMaterial}
    <div class="loading-overlay text-muted">
      <div class="text-center">
        <i class="bi bi-graph-up-arrow fs-1 mb-2 d-block opacity-25"></i>
        <div>Material eingeben und <strong>Laden</strong> klicken</div>
      </div>
    </div>
  {/if}

  <!-- ── Keine Daten ───────────────────────────────────────────────────────── -->
  {#if !loading && !error && history && currentPoints.length === 0 && currentMaterial}
    <div class="alert alert-secondary" transition:fade>
      Keine Verlaufsdaten für <strong>{currentMaterial}</strong> in dieser Granularität verfügbar.
    </div>
  {/if}

  <!-- ── Charts ────────────────────────────────────────────────────────────── -->
  {#if !loading && history && currentPoints.length > 0}

    <!-- Preisentwicklung -->
    <div class="card mb-3" transition:fade>
      <div class="card-header d-flex justify-content-between align-items-center flex-wrap gap-2">
        <span>
          <i class="bi bi-graph-up me-2 text-info"></i>Preisentwicklung
          <small class="text-muted ms-2">{currentMaterial}</small>
        </span>
        <div class="btn-group btn-group-sm" role="group">
          {#each [['HOURLY','Stündlich'],['DAILY','Täglich'],['WEEKLY','Wöchentlich'],['MONTHLY','Monatlich']] as [key, label] (key)}
            <button
              type="button"
              class="btn"
              class:btn-info={granularity === key}
              class:btn-outline-secondary={granularity !== key}
              onclick={() => granularity = key}
            >{label}</button>
          {/each}
        </div>
      </div>
      <div class="card-body p-2">
        <div class="chart-container" bind:this={chartPriceEl}></div>
      </div>
    </div>

    <!-- Transaktionen -->
    <div class="card mb-3" transition:fade>
      <div class="card-header">
        <i class="bi bi-arrow-left-right me-2 text-info"></i>Transaktionen
      </div>
      <div class="card-body p-2">
        <div class="chart-container" bind:this={chartTxEl}></div>
      </div>
    </div>

    <!-- Gehandelte Items -->
    <div class="card" transition:fade>
      <div class="card-header">
        <i class="bi bi-boxes me-2 text-warning"></i>Gehandelte Items
      </div>
      <div class="card-body p-2">
        <div class="chart-container" bind:this={chartItemsEl}></div>
      </div>
      <div class="card-footer text-muted small d-flex justify-content-between">
        <span>{currentPoints.length} Datenpunkte</span>
        {#if currentPoints.length > 0}<span>Zeitraum: {dateRange}</span>{/if}
      </div>
    </div>

  {/if}

</div>
