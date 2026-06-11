<script>
  import { flip }     from 'svelte/animate'
  import { fade }     from 'svelte/transition'
  import { cubicOut } from 'svelte/easing'
  import Icon from '@iconify/svelte'
  import Navbar       from '../../components/Navbar.svelte'
  import { fmtItem, fmt, fmtInt, spreadClass, itemIcon, hideOnError } from '../../lib/utils.js'

  const LS_VIEW = 'visotaris_market_view'

  // ── State ──────────────────────────────────────────────────────────────────
  let items     = $state([])
  let loading   = $state(false)
  let error     = $state(null)
  let search    = $state('')
  let sortKey   = $state('item')
  let sortDir   = $state('asc')
  let lastFetch = $state(null)
  let category  = $state('')        // '' = alle Kategorien
  let viewMode  = $state('grid')    // 'list' | 'grid'

  // Preise der vorherigen Ladung – für Flash-Erkennung
  let prevPrices = {}
  let flashKeys  = $state(new Set())

  // ── Derived ────────────────────────────────────────────────────────────────
  const statusBadgeClass = $derived(
    loading            ? 'badge-secondary' :
    error              ? 'badge-stale'     :
    items.length === 0 ? 'badge-empty'     : 'badge-fresh'
  )

  const statusText = $derived(
    loading          ? 'Laden…'             :
    error            ? 'Fehler'             :
    items.length === 0 ? 'Leer'             :
    items.length + ' Items'
  )

  const categories = $derived.by(() => {
    const set = new Set()
    for (const i of items) if (i.category) set.add(i.category)
    return [...set].sort((a, b) => a.localeCompare(b, 'de'))
  })

  const filteredItems = $derived.by(() => {
    let list = items
    if (category) list = list.filter(i => i.category === category)
    const q = search.toLowerCase().trim()
    if (q) list = list.filter(i => i.itemKey.toLowerCase().includes(q))
    const dir = sortDir === 'asc' ? 1 : -1
    return [...list].sort((a, b) => {
      if (sortKey === 'item')   return dir * a.itemKey.localeCompare(b.itemKey)
      if (sortKey === 'buy')    return dir * (a.buy  - b.buy)
      if (sortKey === 'sell')   return dir * (a.sell - b.sell)
      if (sortKey === 'orders') return dir * (((a.buyOrders ?? 0) + (a.sellOrders ?? 0)) - ((b.buyOrders ?? 0) + (b.sellOrders ?? 0)))
      return 0
    })
  })

  // ── Sort/View-Helpers ───────────────────────────────────────────────────────
  function setSort(key) {
    if (sortKey === key) sortDir = sortDir === 'asc' ? 'desc' : 'asc'
    else { sortKey = key; sortDir = 'asc' }
  }
  function sortCls(key) {
    if (sortKey !== key) return ''
    return sortDir === 'asc' ? 'sort-asc' : 'sort-desc'
  }
  function setView(mode) {
    viewMode = mode
    try { localStorage.setItem(LS_VIEW, mode) } catch(_) {}
  }

  // ── Daten laden ─────────────────────────────────────────────────────────────
  async function loadData() {
    loading = true
    error   = null
    try {
      const res = await fetch('/api/market')
      if (!res.ok) throw new Error('HTTP ' + res.status)
      const data     = await res.json()
      const newItems = Array.isArray(data) ? data : Object.values(data)

      // Flash-Erkennung: Zeilen deren Preis sich geändert hat kurz aufleuchten
      const changed = new Set()
      for (const item of newItems) {
        const prev = prevPrices[item.itemKey]
        if (prev !== undefined && (prev.buy !== item.buy || prev.sell !== item.sell)) {
          changed.add(item.itemKey)
        }
      }
      if (changed.size > 0) {
        flashKeys = changed
        setTimeout(() => { flashKeys = new Set() }, 1000)
      }

      // Preise für nächsten Vergleich speichern
      const pp = {}
      for (const it of newItems) pp[it.itemKey] = { buy: it.buy, sell: it.sell }
      prevPrices = pp

      items     = newItems
      lastFetch = new Date().toLocaleTimeString('de-DE')
    } catch (e) {
      error = 'Fehler beim Laden: ' + e.message
    } finally {
      loading = false
    }
  }

  // Einmalig beim Mount laden
  $effect.root(() => {
    try { const v = localStorage.getItem(LS_VIEW); if (v === 'grid' || v === 'list') viewMode = v } catch(_) {}
    loadData()
  })
</script>

<Navbar activePage="market" />

<div class="w-full px-4 py-3">

  <!-- ── Kopfzeile ────────────────────────────────────────────────────────── -->
  <div class="flex items-center gap-3 mb-3 flex-wrap">
    <h5 class="m-0 flex items-center gap-2 font-semibold text-base">
      <Icon icon="lucide:table" width={15} style="color:var(--vi-accent)" />Marktpreise
    </h5>
    <span class={statusBadgeClass}>{statusText}</span>
    <div class="ml-auto flex gap-2 items-center">
      <div class="flex">
        <button class="btn-seg" class:active={viewMode === 'list'} onclick={() => setView('list')} title="Listenansicht">
          <Icon icon="lucide:list" width={14} />
        </button>
        <button class="btn-seg" class:active={viewMode === 'grid'} onclick={() => setView('grid')} title="Kachelansicht">
          <Icon icon="lucide:layout-grid" width={14} />
        </button>
      </div>
      <input
        type="text"
        class="search-input"
        style="width:220px"
        placeholder="Suchen…"
        bind:value={search}
      >
      <button class="btn-icon" onclick={loadData} title="Aktualisieren">
        <Icon icon="lucide:refresh-cw" width={14} class={loading ? 'spin' : ''} />
      </button>
    </div>
  </div>

  <!-- ── Kategorie-Filter ──────────────────────────────────────────────────── -->
  {#if categories.length > 0}
    <div class="flex gap-2 mb-3 flex-wrap items-center" transition:fade>
      <button class="chip" class:active={category === ''} onclick={() => category = ''}>
        Alle
      </button>
      {#each categories as cat (cat)}
        <button class="chip" class:active={category === cat} onclick={() => category = cat}>
          {cat}
        </button>
      {/each}
    </div>
  {/if}

  <!-- ── Lade-Spinner ──────────────────────────────────────────────────────── -->
  {#if loading && items.length === 0}
    <div class="loading-overlay" transition:fade={{ duration: 150 }}>
      <div class="text-center">
        <span class="inline-block w-6 h-6 border-2 rounded-full animate-spin mb-2 mx-auto block"
              style="border-color:var(--vi-accent); border-top-color:transparent"></span>
        <div>Lade Marktpreise…</div>
      </div>
    </div>
  {/if}

  <!-- ── Fehler ────────────────────────────────────────────────────────────── -->
  {#if error && !loading}
    <div class="rounded p-3 mb-3 text-sm"
         style="background:#450a0a; border:1px solid #7f1d1d; color:#fca5a5"
         transition:fade>{error}</div>
  {/if}

  <!-- ── Grid-Ansicht ──────────────────────────────────────────────────────── -->
  {#if viewMode === 'grid' && (!loading || items.length > 0)}
    <div class="market-grid mb-3" transition:fade={{ duration: 200 }}>
      {#each filteredItems as item (item.itemKey)}
        <a
          href="/history?m={encodeURIComponent(item.itemKey)}"
          class="market-card"
          class:row-flash={flashKeys.has(item.itemKey)}
          animate:flip={{ duration: 280, easing: cubicOut }}
        >
          <div class="mc-head">
            <img src={itemIcon(item.itemKey)} class="mc-icon" alt="" loading="lazy" onerror={hideOnError}>
            <div>
              <div class="mc-name">{fmtItem(item.itemKey)}</div>
              {#if item.category}<div class="mc-cat">{item.category}</div>{/if}
            </div>
          </div>
          <div class="mc-prices">
            <div>
              <span class="mc-label">Kauf</span>
              {#if item.buy > 0}<span class="price-buy">{fmt(item.buy)}</span>{:else}<span class="price-na">–</span>{/if}
            </div>
            <div class="text-right">
              <span class="mc-label">Verkauf</span>
              {#if item.sell > 0}<span class="price-sell">{fmt(item.sell)}</span>{:else}<span class="price-na">–</span>{/if}
            </div>
          </div>
          <div class="mc-orders">
            <span title="Aktive Kauf-Aufträge">
              <Icon icon="lucide:arrow-down-circle" width={11} class="inline" /> {fmtInt(item.buyOrders ?? 0)}
            </span>
            <span title="Aktive Verkauf-Aufträge">
              <Icon icon="lucide:arrow-up-circle" width={11} class="inline" /> {fmtInt(item.sellOrders ?? 0)}
            </span>
          </div>
        </a>
      {/each}
    </div>
    <div class="vi-card-footer flex justify-between rounded"
         style="border:1px solid var(--vi-border); background:var(--vi-bg-card)">
      <span>{filteredItems.length} / {items.length} Einträge</span>
      {#if lastFetch}<span>Stand: {lastFetch}</span>{/if}
    </div>
  {/if}

  <!-- ── Listen-Ansicht ────────────────────────────────────────────────────── -->
  {#if viewMode === 'list' && (!loading || items.length > 0)}
    <div class="vi-card" transition:fade={{ duration: 200 }}>
      <div class="overflow-x-auto">
        <table class="vi-table">
          <thead>
            <tr>
              <th onclick={() => setSort('item')} class={sortCls('item')}>Item</th>
              <th class="hide-sm">Kategorie</th>
              <th onclick={() => setSort('buy')}  class="text-right {sortCls('buy')}">Kaufpreis</th>
              <th onclick={() => setSort('sell')} class="text-right {sortCls('sell')}">Verkaufspreis</th>
              <th class="text-right">Spanne</th>
              <th onclick={() => setSort('orders')} class="text-right {sortCls('orders')}">Aufträge (K / V)</th>
            </tr>
          </thead>
          <tbody>
            {#each filteredItems as item (item.itemKey)}
              <tr
                animate:flip={{ duration: 280, easing: cubicOut }}
                class:row-flash={flashKeys.has(item.itemKey)}
              >
                <td>
                  <div class="flex items-center gap-2">
                    <img src={itemIcon(item.itemKey)} class="item-icon" alt="" loading="lazy" onerror={hideOnError}>
                    <a href="/history?m={encodeURIComponent(item.itemKey)}"
                       class="font-medium no-underline transition-colors"
                       style="color:var(--vi-text)">
                      {fmtItem(item.itemKey)}
                    </a>
                  </div>
                </td>
                <td class="hide-sm" style="color:var(--vi-text-muted)">{item.category ?? '–'}</td>
                <td class="text-right">
                  {#if item.buy > 0}
                    <span class="price-buy">{fmt(item.buy)}</span>
                  {:else}
                    <span class="price-na">–</span>
                  {/if}
                </td>
                <td class="text-right">
                  {#if item.sell > 0}
                    <span class="price-sell">{fmt(item.sell)}</span>
                  {:else}
                    <span class="price-na">–</span>
                  {/if}
                </td>
                <td class="text-right">
                  {#if item.buy > 0 && item.sell > 0}
                    <span class="{spreadClass(item.buy, item.sell)} tabular">
                      {fmt(item.buy - item.sell)}
                    </span>
                  {:else}
                    <span class="price-na">–</span>
                  {/if}
                </td>
                <td class="text-right tabular" style="color:var(--vi-text-muted)">
                  <span class="price-buy">{fmtInt(item.buyOrders ?? 0)}</span>
                  /
                  <span class="price-sell">{fmtInt(item.sellOrders ?? 0)}</span>
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

<style>
  .tabular { font-variant-numeric: tabular-nums; }
  @media (max-width: 720px) {
    .hide-sm { display: none; }
  }
</style>
