<script>
  import { flip }     from 'svelte/animate'
  import { fade }     from 'svelte/transition'
  import { cubicOut } from 'svelte/easing'
  import Icon from '@iconify/svelte'
  import Navbar       from '../../components/Navbar.svelte'
  import { fmtItem, itemIcon, hideOnError } from '../../lib/utils.js'

  /** @type {{ currency: 'opshards' | 'redcoins' }} */
  let { currency = 'opshards' } = $props()
  const isShard = $derived(currency === 'opshards')
  const pageTitle = $derived(isShard ? 'Shardkurse' : 'Redcoin-Kurse')
  const apiPath = $derived(isShard ? '/api/shard' : '/api/redcoins')
  const unitLabel = $derived(isShard ? 'Shards / Einheit' : 'Redcoins / Einheit')

  // ── State ──────────────────────────────────────────────────────────────────
  let items     = $state([])
  let loading   = $state(false)
  let error     = $state(null)
  let search    = $state('')
  let sortKey   = $state('item')
  let sortDir   = $state('asc')
  let lastFetch = $state(null)

  // ── Derived ────────────────────────────────────────────────────────────────
  const statusBadgeClass = $derived(
    loading            ? 'badge-secondary' :
    error              ? 'badge-stale'     :
    items.length === 0 ? 'badge-empty'     : 'badge-fresh'
  )

  const statusText = $derived(
    loading            ? 'Laden…'             :
    error              ? 'Fehler'             :
    items.length === 0 ? 'Leer'               :
    items.length + ' Kurse'
  )

  const filteredItems = $derived.by(() => {
    let list = items
    const q  = search.toLowerCase().trim()
    if (q) list = list.filter(i =>
      i.source.toLowerCase().includes(q) ||
      (i.displayName ?? '').toLowerCase().includes(q)
    )
    const dir = sortDir === 'asc' ? 1 : -1
    return [...list].sort((a, b) => {
      if (sortKey === 'item') return dir * shardName(a).localeCompare(shardName(b), 'de')
      if (sortKey === 'rate') return dir * (a.exchangeRate - b.exchangeRate)
      if (sortKey === 'base') return dir * ((a.base ?? 0) - (b.base ?? 0))
      if (sortKey === 'diff') return dir * (diffPct(a) - diffPct(b))
      return 0
    })
  })

  /** Anzeigename: Custom-Name ("Gräbergemisch") falls vorhanden, sonst formatierter Key. */
  function shardName(item) {
    return item.displayName || fmtItem(item.source)
  }

  /** Abweichung des aktuellen Kurses vom Basiskurs in Prozent. */
  function diffPct(item) {
    if (!item.base || item.base <= 0) return 0
    return ((item.exchangeRate - item.base) / item.base) * 100
  }

  function fmtPct(v) {
    const s = new Intl.NumberFormat('de-DE', { minimumFractionDigits:1, maximumFractionDigits:1 }).format(v)
    return (v > 0 ? '+' : '') + s + ' %'
  }

  // ── Sort-Helpers ────────────────────────────────────────────────────────────
  function setSort(key) {
    if (sortKey === key) sortDir = sortDir === 'asc' ? 'desc' : 'asc'
    else { sortKey = key; sortDir = 'asc' }
  }
  function sortCls(key) {
    if (sortKey !== key) return ''
    return sortDir === 'asc' ? 'sort-asc' : 'sort-desc'
  }

  function fmtRate(v) {
    if (!v) return '–'
    return new Intl.NumberFormat('de-DE', { minimumFractionDigits:2, maximumFractionDigits:2 }).format(v)
  }

  // ── Daten laden ─────────────────────────────────────────────────────────────
  async function loadData() {
    loading = true
    error   = null
    try {
      const res  = await fetch(apiPath)
      if (!res.ok) throw new Error('HTTP ' + res.status)
      const data = await res.json()
      items     = Array.isArray(data) ? data : Object.values(data)
      lastFetch = new Date().toLocaleTimeString('de-DE')
    } catch (e) {
      error = 'Fehler beim Laden: ' + e.message
    } finally {
      loading = false
    }
  }

  $effect.root(() => { loadData() })
</script>

<Navbar activePage={isShard ? 'shard' : 'redcoins'} />

<div class="w-full px-4 py-3">

  <!-- ── Kopfzeile ────────────────────────────────────────────────────────── -->
  <div class="flex items-center gap-3 mb-3 flex-wrap">
    <h5 class="m-0 flex items-center gap-2 font-semibold text-base">
      <Icon icon={isShard ? 'lucide:gem' : 'lucide:circle-dollar-sign'} width={15} style="color:var(--vi-accent)" />{pageTitle}
    </h5>
    <span class={statusBadgeClass}>{statusText}</span>
    <div class="ml-auto flex gap-2">
      <input
        type="text"
        class="search-input"
        style="width:200px"
        placeholder="Suchen…"
        bind:value={search}
      >
      <button class="btn-icon" onclick={loadData} title="Aktualisieren">
        <Icon icon="lucide:refresh-cw" width={14} class={loading ? 'spin' : ''} />
      </button>
    </div>
  </div>

  <!-- ── Lade-Spinner ──────────────────────────────────────────────────────── -->
  {#if loading && items.length === 0}
    <div class="loading-overlay" transition:fade={{ duration: 150 }}>
      <div class="text-center">
        <span class="inline-block w-6 h-6 border-2 rounded-full animate-spin mb-2 mx-auto block"
              style="border-color:var(--vi-accent); border-top-color:transparent"></span>
        <div>Lade {pageTitle}…</div>
      </div>
    </div>
  {/if}

  <!-- ── Fehler ────────────────────────────────────────────────────────────── -->
  {#if error && !loading}
    <div class="rounded p-3 mb-3 text-sm"
         style="background:#450a0a; border:1px solid #7f1d1d; color:#fca5a5"
         transition:fade>{error}</div>
  {/if}

  <!-- ── Tabelle ───────────────────────────────────────────────────────────── -->
  {#if !loading || items.length > 0}
    <div class="vi-card" transition:fade={{ duration: 200 }}>
      <div class="overflow-x-auto">
        <table class="vi-table">
          <thead>
            <tr>
              <th onclick={() => setSort('item')} class={sortCls('item')}>Material</th>
              <th onclick={() => setSort('base')} class="text-right {sortCls('base')}">Basiskurs</th>
              <th onclick={() => setSort('rate')} class="text-right {sortCls('rate')}">
                {unitLabel}
              </th>
              <th onclick={() => setSort('diff')} class="text-right {sortCls('diff')}">Trend</th>
            </tr>
          </thead>
          <tbody>
            {#each filteredItems as item (item.source)}
              <tr animate:flip={{ duration: 280, easing: cubicOut }}>
                <td>
                  <div class="flex items-center gap-2">
                    <img
                      src={itemIcon(item.source)}
                      class="item-icon" alt="" loading="lazy"
                      onerror={hideOnError}
                    >
                    <span class="font-medium">{shardName(item)}</span>
                    {#if item.displayName}
                      <span class="text-xs" style="color:var(--vi-text-muted)" title="Custom-Item">
                        <Icon icon="lucide:sparkles" width={11} class="inline" />
                      </span>
                    {/if}
                  </div>
                </td>
                <td class="text-right">
                  {#if item.base > 0}
                    <span style="color:var(--vi-text-muted); font-variant-numeric:tabular-nums">{fmtRate(item.base)}</span>
                  {:else}
                    <span class="price-na">–</span>
                  {/if}
                </td>
                <td class="text-right">
                  <span class="price-buy">{fmtRate(item.exchangeRate)}</span>
                  <span class="text-sm ml-1" style="color:var(--vi-text-muted)">{(item.target ?? 'OPS').toUpperCase() === 'OPSHARDS' ? 'OPS' : (item.target ?? 'OPS')}</span>
                </td>
                <td class="text-right" style="font-variant-numeric:tabular-nums">
                  {#if item.base > 0}
                    {@const d = diffPct(item)}
                    <span class={d > 0.05 ? 'price-buy' : d < -0.05 ? 'price-sell' : ''}
                          style={Math.abs(d) <= 0.05 ? 'color:var(--vi-text-muted)' : ''}>
                      {#if d > 0.05}<Icon icon="lucide:trending-up" width={12} class="inline" />{/if}
                      {#if d < -0.05}<Icon icon="lucide:trending-down" width={12} class="inline" />{/if}
                      {fmtPct(d)}
                    </span>
                  {:else}
                    <span class="price-na">–</span>
                  {/if}
                </td>
              </tr>
            {/each}
          </tbody>
        </table>
      </div>
      <div class="vi-card-footer flex justify-between">
        <span>{filteredItems.length} / {items.length} Einträge</span>
        {#if lastFetch}<span>Stand: {lastFetch}</span>{/if}
      </div>
    </div>
  {/if}

</div>
