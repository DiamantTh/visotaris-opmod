<script>
  import { onMount } from 'svelte'
  import Icon from '@iconify/svelte'
  import Navbar from '../../components/Navbar.svelte'
  import { fmt, fmtInt, itemIcon } from '../../lib/utils.js'

  let auctions = $state([])
  let categories = $state([])
  let meta = $state(null)
  let search = $state('')
  let category = $state('')
  let sortKey = $state('ending')
  let descending = $state(false)
  let selectedUid = $state('')
  let profileNames = $state({})
  const profileLookupAt = new Map()
  let loading = $state(false)
  let refreshing = $state(false)
  let error = $state('')
  let notice = $state('')
  let timer

  const selected = $derived(auctions.find(auction => auction.uid === selectedUid) || null)
  const filtered = $derived.by(() => {
    const query = search.trim().toLocaleLowerCase('de')
    const values = auctions.filter(auction => {
      const matchesCategory = !category || auction.category === category
      const matchesText = !query || String(auction.searchText || '').toLocaleLowerCase('de').includes(query)
      return matchesCategory && matchesText
    })
    const direction = descending ? -1 : 1
    return values.sort((a, b) => {
      if (sortKey === 'name') return direction * itemName(a).localeCompare(itemName(b), 'de')
      if (sortKey === 'current') return direction * (a.currentBid - b.currentBid)
      if (sortKey === 'instant') {
        if (a.instantBuyPrice == null || b.instantBuyPrice == null) return a.instantBuyPrice == null ? (b.instantBuyPrice == null ? 0 : 1) : -1
        return direction * (a.instantBuyPrice - b.instantBuyPrice)
      }
      const aEnd = Date.parse(a.endTime || '')
      const bEnd = Date.parse(b.endTime || '')
      if (!Number.isFinite(aEnd) || !Number.isFinite(bEnd)) return !Number.isFinite(aEnd) ? (!Number.isFinite(bEnd) ? 0 : 1) : -1
      return direction * (aEnd - bEnd)
    })
  })

  function itemName(auction) { return auction?.item?.visibleName || auction?.item?.displayName || auction?.item?.material || 'Unbekanntes Item' }
  function sellerName(auction) { return auction?.sellerName || playerName(auction?.seller) }
  function fallbackMaterial(auction) {
    const item = auction?.item || {}
    const category = String(auction?.category || '').toLowerCase()
    if (category.startsWith('custom_') || category.startsWith('op_') || /items\.opsucht\.net/i.test(item.icon || '')) return 'barrier'
    return item.material || 'barrier'
  }
  function auctionIconLoaded(event) {
    const fallback = event.currentTarget.previousElementSibling
    if (fallback) fallback.hidden = true
  }
  function hideFallbackIcon(event) { event.currentTarget.hidden = true }
  function shortId(value) { return value ? `${value.slice(0, 8)}…${value.slice(-4)}` : 'unbekannt' }
  function canonicalUuid(value) {
    const compact = String(value || '').replaceAll('-', '').toLowerCase()
    return /^[0-9a-f]{32}$/.test(compact) ? `${compact.slice(0, 8)}-${compact.slice(8, 12)}-${compact.slice(12, 16)}-${compact.slice(16, 20)}-${compact.slice(20)}` : value
  }
  function playerName(uuid) { return uuid ? (profileNames[canonicalUuid(uuid)] || shortId(uuid)) : '–' }
  function date(value) { return value ? new Date(value).toLocaleString('de-DE') : '–' }
  function remaining(value) {
    const seconds = Math.max(0, Math.floor((Date.parse(value || '') - Date.now()) / 1000))
    if (!Number.isFinite(seconds) || !value) return '–'
    const hours = Math.floor(seconds / 3600)
    const minutes = Math.floor(seconds % 3600 / 60)
    return hours ? `${hours} h ${minutes} min` : `${minutes} min`
  }
  function bidCount(auction) { return Object.keys(auction.bids || {}).length }
  function cycleSort(value) { if (sortKey === value) descending = !descending; else { sortKey = value; descending = false } }
  function sortLabel() { return ({ ending: 'Ablauf', name: 'Name', current: 'Gebot', instant: 'Sofortkauf' })[sortKey] }
  function retryAuctionIcon(event) {
    const image = event.currentTarget
    const attempts = Number(image.dataset.retries || 0)
    if (attempts >= 8) { image.hidden = true; return }
    image.dataset.retries = String(attempts + 1)
    setTimeout(() => {
      if (!image.isConnected) return
      image.src = `/api/auctions/${encodeURIComponent(image.dataset.uid)}/icon?retry=${Date.now()}`
    }, 1200)
  }

  async function load() {
    if (loading) return
    loading = true
    try {
      const [auctionResponse, categoryResponse, metaResponse] = await Promise.all([
        fetch('/api/auctions'), fetch('/api/auctions/categories'), fetch('/api/meta')
      ])
      if (!auctionResponse.ok) throw new Error(`Auktionscache: HTTP ${auctionResponse.status}`)
      auctions = await auctionResponse.json()
      categories = categoryResponse.ok ? await categoryResponse.json() : []
      meta = metaResponse.ok ? (await metaResponse.json()).auctions : null
      if (selectedUid && !auctions.some(auction => auction.uid === selectedUid)) selectedUid = ''
      error = ''
    } catch (exception) { error = exception.message || 'Lokaler AuctionCache nicht erreichbar.' }
    finally { loading = false }
  }

  async function resolveProfiles(rows, includeBids = false) {
    const ids = new Set()
    for (const auction of rows) {
      if (includeBids) {
        if (auction.highestBidder) ids.add(auction.highestBidder)
        Object.keys(auction.bids || {}).slice(0, 50).forEach(id => ids.add(id))
      }
    }
    for (const raw of ids) {
      const last = profileLookupAt.get(raw) || 0
      if (Date.now() - last < 4000) continue
      profileLookupAt.set(raw, Date.now())
      fetch(`/api/profiles/${encodeURIComponent(raw)}`).then(response => response.ok ? response.json() : null)
        .then(value => { if (value?.resolved) profileNames[value.uuid] = value.name })
        .catch(() => {})
    }
  }

  async function refresh() {
    refreshing = true; notice = ''; error = ''
    try {
      const response = await fetch('/api/auctions/refresh', { method: 'POST', credentials: 'same-origin' })
      if (!response.ok && response.status !== 202) throw new Error(`Aktualisierung: HTTP ${response.status}`)
      notice = 'Auktions-Snapshot angefragt. Die Liste zeigt den lokalen Cache.'
      await load()
    } catch (exception) { error = exception.message || 'Snapshot konnte nicht angefragt werden.' }
    finally { refreshing = false }
  }

  onMount(() => {
    document.title = 'Visotaris – Auktionshaus'
    load()
    timer = setInterval(load, 4000)
    return () => clearInterval(timer)
  })
</script>

<Navbar activePage="auctions" />
<main class="vi-page auction-page">
  <header class="auction-heading">
    <div><p class="auction-kicker">OPSUCHT · READ-ONLY · LOKALER CACHE</p><h1>Auktionshaus</h1><p>Aktive Angebote und Live-Änderungen aus derselben AuctionCache-Instanz wie im Minecraft-Menü.</p></div>
    <button class="btn-primary" onclick={refresh} disabled={refreshing}><Icon icon="lucide:refresh-cw" width={15} />{refreshing ? 'Angefragt …' : 'Auktionen aktualisieren'}</button>
  </header>

  {#if error}<div class="vi-alert-error" role="alert">{error}</div>{/if}
  {#if notice}<div class="auction-notice" role="status">{notice}</div>{/if}
  <section class="auction-toolbar vi-card">
    <label class="auction-search"><Icon icon="lucide:search" width={15} /><input bind:value={search} placeholder="Item oder Verkäufer suchen …" /></label>
    <select bind:value={category} aria-label="Kategorie"><option value="">Alle Kategorien</option>{#each categories as entry}<option value={entry.name}>{entry.displayName || entry.name}</option>{/each}</select>
    <button class="btn-outline" onclick={() => cycleSort(sortKey)} aria-label="Sortierrichtung umschalten">Sortierung: {sortLabel()} {descending ? '↓' : '↑'}</button>
    <select bind:value={sortKey} aria-label="Sortieren nach"><option value="ending">Ablauf</option><option value="name">Name</option><option value="current">Gebot</option><option value="instant">Sofortkauf</option></select>
  </section>
  <p class="auction-meta">{auctions.length.toLocaleString('de-DE')} aktiv · Cache: {meta?.updatedAt ? date(meta.updatedAt) : 'noch nicht synchronisiert'} · {meta?.streamActive ? 'Live verbunden' : meta?.streamEnabled ? (meta?.devOverride && !meta?.liveUpdatesEnabled ? 'Entwicklungsstream verbindet …' : 'Liveupdates verbinden …') : 'manueller Abruf'}</p>

  {#if loading && !auctions.length}
    <div class="auction-empty">Lokalen Auktionscache laden …</div>
  {:else if !filtered.length}
    <div class="auction-empty"><strong>{auctions.length ? 'Keine passenden Auktionen' : 'Noch keine Auktionen synchronisiert'}</strong><span>{auctions.length ? 'Suche oder Kategorie anpassen.' : '„Auktionen aktualisieren“ lädt /auctions/active. Das Öffnen dieser Seite fragt keine externe Auktionsliste ab.'}</span></div>
  {:else}
    <div class="auction-layout">
      <section class="auction-list" aria-label="Aktive Auktionen">
        {#each filtered as auction (auction.uid)}
          <button class="auction-row" class:selected={selectedUid === auction.uid} onclick={() => { selectedUid = auction.uid; resolveProfiles([auction], true) }}>
            <span class="auction-icons" aria-hidden="true">
              <img class="auction-icon-fallback" src={itemIcon(fallbackMaterial(auction))} alt="" loading="lazy" onerror={hideFallbackIcon} />
              <img class="auction-icon-api" src={`/api/auctions/${encodeURIComponent(auction.uid)}/icon`} data-uid={auction.uid} alt="" loading="lazy" onload={auctionIconLoaded} onerror={retryAuctionIcon} />
            </span>
            <span class="auction-row-main"><strong>{itemName(auction)}</strong><small>{auction.item?.amount || 1}× · {auction.category || 'Ohne Kategorie'} · Verkäufer {sellerName(auction)}</small></span>
            <span class="auction-price"><strong>{fmt(auction.currentBid)} OPS</strong><small>{auction.instantBuyPrice == null ? 'kein Sofortkauf' : `Sofort ${fmt(auction.instantBuyPrice)} OPS`}</small></span>
            <span class="auction-time"><strong>{remaining(auction.endTime)}</strong><small>Restzeit</small></span>
          </button>
        {/each}
      </section>

      {#if selected}
        <aside class="auction-detail vi-card">
          <div class="auction-detail-head">
            <span class="auction-icons auction-detail-icons" aria-hidden="true">
              <img class="auction-icon-fallback" src={itemIcon(fallbackMaterial(selected))} alt="" loading="lazy" onerror={hideFallbackIcon} />
              <img class="auction-icon-api" src={`/api/auctions/${encodeURIComponent(selected.uid)}/icon`} data-uid={selected.uid} alt="" loading="lazy" onload={auctionIconLoaded} onerror={retryAuctionIcon} />
            </span>
            <div><p class="auction-kicker">ANGEBOTSDETAILS</p><h2>{itemName(selected)}</h2></div><button class="btn-outline" onclick={() => selectedUid = ''} aria-label="Details schließen">×</button>
          </div>
          <dl>
            <div><dt>Menge</dt><dd>{fmtInt(selected.item?.amount || 1)}</dd></div>
            <div><dt>Kategorie</dt><dd>{selected.category || '–'}</dd></div>
            <div><dt>Status</dt><dd>{selected.state || '–'}</dd></div>
            <div><dt>Startgebot</dt><dd>{fmt(selected.startBid)} OPS</dd></div>
            <div><dt>Aktuelles Gebot</dt><dd>{fmt(selected.currentBid)} OPS</dd></div>
            <div><dt>Sofortkauf</dt><dd>{selected.instantBuyPrice == null ? '–' : `${fmt(selected.instantBuyPrice)} OPS`}</dd></div>
            <div><dt>Verkäufer</dt><dd>{sellerName(selected)}</dd></div>
            <div><dt>Höchstbietender</dt><dd>{playerName(selected.highestBidder)}</dd></div>
            <div><dt>Gebote</dt><dd>{bidCount(selected)}</dd></div>
            <div><dt>Startzeit</dt><dd>{date(selected.startTime)}</dd></div>
            <div><dt>Endzeit</dt><dd>{date(selected.endTime)} · {remaining(selected.endTime)}</dd></div>
            <div><dt>Auktions-UID</dt><dd class="mono">{selected.uid}</dd></div>
          </dl>
          {#if selected.item?.lore?.length}<section><h3>Beschreibung</h3><ul>{#each selected.item.lore as line}<li>{line}</li>{/each}</ul></section>{/if}
          {#if Object.keys(selected.item?.enchantments || {}).length}<section><h3>Verzauberungen</h3><ul>{#each Object.entries(selected.item.enchantments) as [name, level]}<li>{name} {level}</li>{/each}</ul></section>{/if}
          {#if Object.keys(selected.bids || {}).length}<section><h3>Gebotsverlauf aus dem aktuellen API-Datensatz</h3><ul>{#each Object.entries(selected.bids) as [uuid, amount]}<li>{playerName(uuid)} — {fmt(amount)} OPS</li>{/each}</ul></section>{/if}
          <small class="auction-readonly">Nur Anzeige. Keine Gebots-, Kauf- oder Serveraktion.</small>
        </aside>
      {/if}
    </div>
  {/if}
</main>

<style>
  .auction-page{max-width:1440px}.auction-heading{display:flex;justify-content:space-between;align-items:end;gap:1rem;margin:.6rem 0 1.2rem}.auction-heading h1{margin:.12rem 0;font-size:clamp(1.7rem,4vw,2.25rem)}.auction-heading p{color:var(--vi-text-muted);max-width:46rem}.auction-kicker{color:var(--vi-accent)!important;font:600 .68rem var(--vi-font-data);letter-spacing:.09em;margin:0}.auction-heading .btn-primary{display:flex;align-items:center;gap:.45rem;white-space:nowrap}.auction-toolbar{display:flex;align-items:center;gap:.55rem;padding:.65rem;margin:.8rem 0}.auction-search{display:flex;align-items:center;gap:.5rem;flex:1;min-width:12rem;color:var(--vi-text-muted)}.auction-search input,.auction-toolbar select{border:1px solid var(--vi-border);background:var(--vi-bg-input);color:var(--vi-text);border-radius:.35rem;padding:.5rem .6rem;min-width:0}.auction-search input{width:100%}.auction-meta{color:var(--vi-text-muted);font-size:.78rem;margin:.5rem .1rem}.auction-layout{display:grid;grid-template-columns:minmax(0,1.1fr) minmax(18rem,.9fr);gap:.8rem;align-items:start}.auction-list{display:grid;gap:.4rem}.auction-row{display:grid;grid-template-columns:2rem minmax(0,1fr) minmax(8rem,auto) minmax(4.4rem,auto);align-items:center;gap:.6rem;width:100%;text-align:left;padding:.55rem .65rem;border:1px solid var(--vi-border);border-radius:.45rem;background:var(--vi-bg-card);color:var(--vi-text);cursor:pointer}.auction-row:hover,.auction-row.selected{border-color:var(--vi-accent);background:var(--vi-bg-elevated)}.auction-row img{width:2rem;height:2rem;object-fit:contain;image-rendering:pixelated}.auction-row-main,.auction-price,.auction-time{display:grid;gap:.12rem;min-width:0}.auction-row-main strong{overflow:hidden;text-overflow:ellipsis;white-space:nowrap}.auction-row small,.auction-price small,.auction-time small{font-size:.7rem;color:var(--vi-text-muted)}.auction-price{text-align:right}.auction-time{text-align:right}.auction-detail{padding:1rem;position:sticky;top:5rem}.auction-detail-head{display:flex;align-items:start;justify-content:space-between;gap:.8rem}.auction-detail h2{font-size:1.2rem;margin:.15rem 0 .8rem;overflow-wrap:anywhere}.auction-detail dl{display:grid;grid-template-columns:repeat(2,minmax(0,1fr));gap:.55rem .8rem}.auction-detail dl div{min-width:0}.auction-detail dt{font-size:.68rem;color:var(--vi-text-muted)}.auction-detail dd{margin:.1rem 0 0;overflow-wrap:anywhere;font-size:.82rem}.auction-detail .mono{font-family:var(--vi-font-data);font-size:.7rem}.auction-detail h3{font-size:.82rem;margin:.9rem 0 .3rem}.auction-detail ul{padding-left:1rem;margin:.25rem 0;color:var(--vi-text-muted);font-size:.78rem}.auction-readonly{display:block;margin-top:1rem;color:var(--vi-text-muted)}.auction-empty{display:grid;gap:.35rem;padding:2rem;text-align:center;border:1px dashed var(--vi-border);border-radius:.5rem;color:var(--vi-text-muted)}.auction-empty strong{color:var(--vi-text)}.auction-notice{padding:.65rem .8rem;border:1px solid var(--vi-accent);border-radius:.4rem;color:var(--vi-text)}@media(max-width:850px){.auction-layout{grid-template-columns:1fr}.auction-detail{position:static}}@media(max-width:650px){.auction-heading{align-items:start;flex-direction:column}.auction-toolbar{align-items:stretch;flex-direction:column}.auction-row{grid-template-columns:2rem minmax(0,1fr) auto}.auction-time{display:none}}
  .auction-icons{position:relative;display:block;width:2rem;height:2rem;flex:none}.auction-icons img{position:absolute;inset:0;width:100%;height:100%;object-fit:contain;image-rendering:pixelated}.auction-icon-api{z-index:1}.auction-detail-icons{width:3rem;height:3rem}
</style>
