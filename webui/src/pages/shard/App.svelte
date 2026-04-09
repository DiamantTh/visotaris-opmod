<script>
  import { flip }     from 'svelte/animate'
  import { fade }     from 'svelte/transition'
  import { cubicOut } from 'svelte/easing'
  import Navbar       from '../../components/Navbar.svelte'
  import { fmtItem }  from '../../lib/utils.js'

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
    loading            ? 'bg-secondary' :
    error              ? 'badge-stale'  :
    items.length === 0 ? 'badge-empty'  : 'badge-fresh'
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
    if (q) list = list.filter(i => i.source.toLowerCase().includes(q))
    const dir = sortDir === 'asc' ? 1 : -1
    return [...list].sort((a, b) => {
      if (sortKey === 'item') return dir * a.source.localeCompare(b.source)
      if (sortKey === 'rate') return dir * (a.exchangeRate - b.exchangeRate)
      return 0
    })
  })

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
      const res  = await fetch('/api/shard')
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

<Navbar activePage="shard" />

<div class="container-fluid py-3">

  <!-- ── Kopfzeile ────────────────────────────────────────────────────────── -->
  <div class="d-flex align-items-center gap-3 mb-3 flex-wrap">
    <h5 class="mb-0"><i class="bi bi-gem me-2 text-info"></i>Shardkurse</h5>
    <span class="badge rounded-pill {statusBadgeClass}">{statusText}</span>
    <div class="ms-auto d-flex gap-2">
      <input
        type="text"
        class="form-control form-control-sm search-input"
        placeholder="Suchen…"
        bind:value={search}
        style="width:200px"
      >
      <button class="btn btn-sm btn-outline-secondary" onclick={loadData} title="Aktualisieren">
        <i class="bi bi-arrow-clockwise" class:spin={loading}></i>
      </button>
    </div>
  </div>

  <!-- ── Lade-Spinner ──────────────────────────────────────────────────────── -->
  {#if loading && items.length === 0}
    <div class="loading-overlay" transition:fade={{ duration: 150 }}>
      <div class="text-center">
        <div class="spinner-border text-info mb-2" role="status"></div>
        <div>Lade Shardkurse…</div>
      </div>
    </div>
  {/if}

  <!-- ── Fehler ────────────────────────────────────────────────────────────── -->
  {#if error && !loading}
    <div class="alert alert-danger" transition:fade>{error}</div>
  {/if}

  <!-- ── Tabelle ───────────────────────────────────────────────────────────── -->
  {#if !loading || items.length > 0}
    <div class="card" transition:fade={{ duration: 200 }}>
      <div class="table-responsive">
        <table class="table table-hover table-sm mb-0">
          <thead>
            <tr>
              <th onclick={() => setSort('item')} class={sortCls('item')}>Material</th>
              <th onclick={() => setSort('rate')} class="text-end {sortCls('rate')}">
                Shards / Einheit
              </th>
            </tr>
          </thead>
          <tbody>
            {#each filteredItems as item (item.source)}
              <tr animate:flip={{ duration: 280, easing: cubicOut }}>
                <td>
                  <div class="d-flex align-items-center gap-2">
                    <img
                      src="/api/icon/{item.source}"
                      class="item-icon" alt=""
                      onerror={(e) => e.currentTarget.style.display = 'none'}
                    >
                    <span class="fw-medium">{fmtItem(item.source)}</span>
                  </div>
                </td>
                <td class="text-end">
                  <span class="price-buy">{fmtRate(item.exchangeRate)}</span>
                  <span class="text-muted ms-1 small">OPS</span>
                </td>
              </tr>
            {/each}
          </tbody>
        </table>
      </div>
      <div class="card-footer text-muted small d-flex justify-content-between">
        <span>{filteredItems.length} / {items.length} Einträge</span>
        {#if lastFetch}<span>Stand: {lastFetch}</span>{/if}
      </div>
    </div>
  {/if}

</div>
