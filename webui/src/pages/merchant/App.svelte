<script>
  import { fade } from 'svelte/transition'
  import Icon from '@iconify/svelte'
  import Navbar from '../../components/Navbar.svelte'
  import { fmtItem, itemIcon, hideOnError, currencyDisplayName } from '../../lib/utils.js'

  let ratesByTarget = $state({})
  let activeTarget = $state('')
  let loading = $state(false)
  let error = $state(null)
  let search = $state('')
  let lastUpdated = $state(null)

  const targets = $derived(Object.keys(ratesByTarget).sort((a, b) => a.localeCompare(b, 'de')))
  const rates = $derived(activeTarget ? (ratesByTarget[activeTarget] ?? []) : Object.values(ratesByTarget).flat())
  const filteredRates = $derived.by(() => {
    const query = search.trim().toLowerCase()
    return [...rates]
      .filter(rate => !query || displayName(rate).toLocaleLowerCase().includes(query))
      .sort((a, b) => displayName(a).localeCompare(displayName(b), 'de'))
  })

  function displayName(rate) { return rate.visibleName || rate.displayName || fmtItem(rate.source) }
  function targetLabel(target) {
    const rate = ratesByTarget[target]?.[0]
    const canonical = currencyDisplayName(target)
    return canonical === 'Unbekannte Währung' ? currencyDisplayName(rate?.targetDisplayUnit) : canonical
  }
  function fmtRate(value) { return new Intl.NumberFormat('de-DE', { minimumFractionDigits: 2, maximumFractionDigits: 2 }).format(value ?? 0) }
  function diff(rate) { return rate.base > 0 ? ((rate.exchangeRate - rate.base) / rate.base) * 100 : null }
  function fmtTime(timestamp) { return timestamp ? new Date(timestamp).toLocaleString('de-DE', { hour: '2-digit', minute: '2-digit', day: '2-digit', month: '2-digit' }) : '–' }

  function selectTarget(target) {
    activeTarget = target
    const url = new URL(window.location.href)
    if (target) url.searchParams.set('target', target)
    else url.searchParams.delete('target')
    window.history.replaceState({}, '', url)
  }

  async function loadData() {
    loading = true
    error = null
    try {
      const [ratesResponse, metaResponse] = await Promise.all([fetch('/api/merchant'), fetch('/api/meta')])
      if (!ratesResponse.ok) throw new Error('HTTP ' + ratesResponse.status)
      const data = await ratesResponse.json()
      ratesByTarget = data && typeof data === 'object' ? data : {}
      const requested = new URLSearchParams(window.location.search).get('target')?.toLowerCase()
      if (requested && ratesByTarget[requested]) activeTarget = requested
      else if (activeTarget && !ratesByTarget[activeTarget]) activeTarget = ''
      if (metaResponse.ok) lastUpdated = (await metaResponse.json())?.merchant?.updatedAtMs ?? null
    } catch (e) {
      error = 'Fehler beim Laden: ' + e.message
    } finally {
      loading = false
    }
  }

  $effect.root(() => { loadData() })
</script>

<div class="vi-viewport-shell">
<Navbar activePage="merchant" />

<main class="vi-page vi-viewport-page merchant-page">
  <div class="merchant-toolbar vi-page-toolbar flex items-center gap-3 flex-wrap">
    <h5 class="vi-page-heading m-0 flex items-center gap-2 font-semibold text-base"><Icon icon="lucide:handshake" width={15} style="color:var(--vi-accent)" />Händlerkurse</h5>
    <span class={loading ? 'badge-secondary' : error ? 'badge-stale' : targets.length ? 'badge-fresh' : 'badge-empty'}>{loading ? 'Laden…' : targets.length ? targets.length + ' Währungen' : 'Leer'}</span>
    <div class="ml-auto flex gap-2 items-center"><input class="search-input" style="width:210px;max-width:100%" placeholder="Material suchen…" bind:value={search}><button class="btn-icon" onclick={loadData} title="Aktualisieren"><Icon icon="lucide:refresh-cw" width={14} class={loading ? 'spin' : ''} /></button></div>
  </div>

  {#if targets.length > 0}
    <div class="vi-summary vi-page-toolbar" transition:fade={{ duration: 150 }}>
      <div class="vi-metric"><span class="vi-metric-label">Zielwährungen</span><span class="vi-metric-value">{targets.length}</span></div>
      <div class="vi-metric"><span class="vi-metric-label">Kurse in Auswahl</span><span class="vi-metric-value">{rates.length}</span></div>
      <div class="vi-metric"><span class="vi-metric-label">API-Stand</span><span class="vi-metric-value fresh">{fmtTime(lastUpdated)}</span></div>
    </div>
    <div class="merchant-targets vi-page-toolbar">
      <button class="chip" class:active={activeTarget === ''} onclick={() => selectTarget('')}>Alle</button>
      {#each targets as target (target)}<button class="chip" class:active={activeTarget === target} onclick={() => selectTarget(target)}>{targetLabel(target)}</button>{/each}
    </div>
  {/if}

  {#if error && !loading}<div class="vi-alert-error merchant-error">{error}</div>{/if}
  {#if loading && !targets.length}<div class="loading-overlay merchant-loading"><div>Lade Händlerkurse…</div></div>{/if}

  {#if !loading || rates.length > 0}
    <div class="vi-card vi-data-surface merchant-table-card" transition:fade={{ duration: 150 }}>
      <div class="vi-table-region"><table class="vi-table"><thead><tr><th>Material</th><th>Zielwährung</th><th class="text-right hide-xs">Basis</th><th class="text-right">Kurs</th><th class="text-right hide-xs">Abweichung</th></tr></thead><tbody>
        {#each filteredRates as rate (rate.target + ':' + rate.source)}
          {@const delta = diff(rate)}
          <tr><td><div class="flex items-center gap-2"><img src={itemIcon(rate.source)} class="item-icon" alt="" loading="lazy" onerror={hideOnError}><span class="font-medium">{displayName(rate)}</span></div></td><td><span class="merchant-target">{targetLabel(rate.target ?? 'unknown')}</span></td><td class="text-right hide-xs">{rate.base > 0 ? fmtRate(rate.base) : '–'}</td><td class="text-right"><span class={rate.target === 'redcoins' ? 'price-redcoin' : 'price-buy'}>{fmtRate(rate.exchangeRate)}</span></td><td class="text-right hide-xs">{#if delta !== null}<span class={delta > 0.05 ? 'price-buy' : delta < -0.05 ? 'price-sell' : ''}>{delta > 0 ? '+' : ''}{fmtRate(delta)} %</span>{:else}–{/if}</td></tr>
        {/each}
      </tbody></table></div>
      {#if !filteredRates.length}<div class="empty-state"><Icon icon="lucide:search-x" width={30} /><div>Keine Händlerkurse für diese Auswahl.</div></div>{/if}
      <div class="vi-card-footer vi-data-footer merchant-footer"><span>{filteredRates.length} / {rates.length} Einträge</span><span>API-Stand: {fmtTime(lastUpdated)}</span></div>
    </div>
  {/if}
</main>
</div>

<style>
  .merchant-toolbar { margin:0; }
  .merchant-targets { flex:0 0 auto; margin:0; }
  .merchant-loading { flex:1 1 auto; min-height:0; }
  .merchant-error { flex:0 0 auto; }
  .merchant-table-card { flex:1 1 auto; }
  .merchant-table-card .vi-table-region { flex:1 1 auto; }
  .merchant-footer { display:flex; justify-content:space-between; gap:.8rem; }
  @media(max-width:520px) {
    .merchant-toolbar { gap:.5rem; }
    .merchant-toolbar > div:last-child { width:100%; }
    .merchant-toolbar .search-input { flex:1; width:auto!important; }
    .merchant-table-card .vi-table th,.merchant-table-card .vi-table td { padding:.4rem .42rem; font-size:.68rem; }
    .merchant-table-card .vi-table th { white-space:normal; }
    .merchant-table-card .vi-table .hide-xs { display:none; }
    .merchant-footer { font-size:.68rem; }
  }
</style>
