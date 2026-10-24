<script>
  import { onMount } from 'svelte'
  import Icon from '@iconify/svelte'
  import Navbar from '../../components/Navbar.svelte'
  import { fmtMoney, fmtInt, itemIcon } from '../../lib/utils.js'

  let auctions = $state([])
  let categories = $state([])
  let meta = $state(null)
  let search = $state('')
  let category = $state('')
  let sortKey = $state('ending')
  let descending = $state(false)
  let selectedUid = $state('')
  let profileNames = $state({})
  let profileResolution = $state({})
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
  function canonicalUuid(value) {
    const compact = String(value || '').replaceAll('-', '').toLowerCase()
    return /^[0-9a-f]{32}$/.test(compact) ? `${compact.slice(0, 8)}-${compact.slice(8, 12)}-${compact.slice(12, 16)}-${compact.slice(16, 20)}-${compact.slice(20)}` : value
  }
  function playerName(uuid) { return uuid ? (profileNames[canonicalUuid(uuid)] || 'Spielername nicht verfügbar') : 'Anonymer Verkäufer' }
  function sellerResolution(auction) {
    const seller = String(auction?.seller || '').trim()
    if (!seller) return 'anonymous'
    const uuid = canonicalUuid(seller)
    if (profileNames[uuid] || auction?.sellerName) return 'resolved'
    return profileResolution[uuid] || auction?.sellerResolution || 'unavailable'
  }
  function sellerName(auction) {
    const state = sellerResolution(auction)
    if (state === 'anonymous') return 'Anonymer Verkäufer'
    if (state === 'resolving') return 'Spielername wird aufgelöst …'
    return profileNames[canonicalUuid(auction?.seller)] || auction?.sellerName || 'Spielername nicht verfügbar'
  }
  function fallbackMaterial(auction) {
    const item = auction?.item || {}
    const category = String(auction?.category || '').toLowerCase()
    if (category.startsWith('custom_') || category.startsWith('op_') || /items\.opsucht\.net/i.test(item.icon || '')) return 'barrier'
    return item.material || 'barrier'
  }
  function categoryLabel(raw) {
    if (!raw) return 'Ohne Kategorie'
    const categoryEntry = categories.find(entry => entry.name === raw)
    if (categoryEntry?.displayName) return categoryEntry.displayName
    return String(raw).replace(/^minecraft:/, '').replaceAll(/[_-]+/g, ' ').replace(/\b\w/g, char => char.toLocaleUpperCase('de'))
  }
  function statusLabel(raw) {
    const key = String(raw || '').trim().toLowerCase().replaceAll('-', '_').replaceAll(' ', '_')
    return ({ active: 'Aktiv', open: 'Aktiv', sold: 'Verkauft', instant_bought: 'Sofort gekauft', bought: 'Sofort gekauft', expired: 'Abgelaufen', cancelled: 'Abgebrochen', canceled: 'Abgebrochen', removed: 'Entfernt', pending: 'Ausstehend' })[key]
      || (key ? key.replaceAll('_', ' ').replace(/\b\w/g, char => char.toLocaleUpperCase('de')) : 'Unbekannt')
  }
  function date(value) { return value ? new Date(value).toLocaleString('de-DE') : '–' }
  function remaining(value) {
    const seconds = Math.floor((Date.parse(value || '') - Date.now()) / 1000)
    if (!Number.isFinite(seconds) || !value) return '–'
    if (seconds <= 0) return 'Abgelaufen'
    const days = Math.floor(seconds / 86400)
    const hours = Math.floor(seconds % 86400 / 3600)
    const minutes = Math.floor(seconds % 3600 / 60)
    if (days) return `${days} T ${hours} h`
    if (hours) return `${hours} h ${minutes} min`
    return `${minutes} min`
  }
  function bidCount(auction) { return Object.keys(auction.bids || {}).length }
  function bidEntries(auction) {
    return Object.entries(auction?.bids || {}).sort((a, b) => b[1] - a[1])
  }
  function enchantments(auction) { return auction?.item?.visibleEnchantments || [] }
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
  function auctionIconLoaded(event) {
    const fallback = event.currentTarget.previousElementSibling
    if (fallback) fallback.hidden = true
  }
  function hideFallbackIcon(event) { event.currentTarget.hidden = true }

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

  async function resolveProfiles(rows) {
    const ids = new Set()
    for (const auction of rows) {
      if (auction.seller) ids.add(auction.seller)
      if (auction.highestBidder) ids.add(auction.highestBidder)
      Object.keys(auction.bids || {}).slice(0, 50).forEach(id => ids.add(id))
    }
    for (const raw of ids) {
      const last = profileLookupAt.get(raw) || 0
      if (Date.now() - last < 4000) continue
      profileLookupAt.set(raw, Date.now())
      fetch(`/api/profiles/${encodeURIComponent(raw)}`).then(response => response.ok ? response.json() : null)
        .then(value => {
          if (!value?.uuid) return
          if (value.resolved) profileNames[value.uuid] = value.name
          profileResolution[value.uuid] = value.resolution || (value.resolved ? 'resolved' : 'unavailable')
        })
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

<div class="vi-viewport-shell auction-shell">
  <Navbar activePage="auctions" />
  <main class="vi-page vi-viewport-page auction-page">
    <header class="auction-heading vi-page-toolbar">
      <div><p class="auction-kicker">OPSUCHT · READ-ONLY · LOKALER CACHE</p><h1>Auktionshaus</h1><p>Aktive Angebote und Live-Änderungen aus derselben AuctionCache-Instanz wie im Minecraft-Menü.</p></div>
      <button class="btn-primary" onclick={refresh} disabled={refreshing}><Icon icon="lucide:refresh-cw" width={15} />{refreshing ? 'Angefragt …' : 'Auktionen aktualisieren'}</button>
    </header>

    {#if error}<div class="vi-alert-error auction-notice" role="alert">{error}</div>{/if}
    {#if notice}<div class="auction-notice" role="status">{notice}</div>{/if}
    <section class="auction-toolbar vi-card vi-page-toolbar">
      <label class="auction-search"><Icon icon="lucide:search" width={15} /><input bind:value={search} placeholder="Item oder Verkäufer suchen …" /></label>
      <select bind:value={category} aria-label="Kategorie"><option value="">Alle Kategorien</option>{#each categories as entry}<option value={entry.name}>{entry.displayName || categoryLabel(entry.name)}</option>{/each}</select>
      <button class="btn-outline auction-sort-direction" onclick={() => cycleSort(sortKey)} aria-label="Sortierrichtung umschalten">{descending ? '↓' : '↑'}</button>
      <select bind:value={sortKey} aria-label="Sortieren nach"><option value="ending">Ablauf</option><option value="name">Name</option><option value="current">Gebot</option><option value="instant">Sofortkauf</option></select>
    </section>

    <div class="auction-layout vi-data-workspace" class:has-selection={Boolean(selected)}>
      <section class="auction-results vi-data-surface" aria-label="Aktive Auktionen">
        <div class="auction-results-heading"><strong>{filtered.length.toLocaleString('de-DE')} Angebote</strong><span>{meta?.updatedAt ? `Cache ${date(meta.updatedAt)}` : 'Cache nicht synchronisiert'} · {meta?.streamActive ? 'Liveupdates aktiv' : meta?.streamEnabled ? (meta?.devOverride && !meta?.liveUpdatesEnabled ? 'Entwicklungsstream verbindet …' : 'Liveupdates verbinden …') : 'manueller Abruf'}</span></div>
        {#if loading && !auctions.length}
          <div class="auction-empty auction-empty-scroll">Lokalen Auktionscache laden …</div>
        {:else if !filtered.length}
          <div class="auction-empty auction-empty-scroll"><strong>{auctions.length ? 'Keine passenden Auktionen' : 'Noch keine Auktionen synchronisiert'}</strong><span>{auctions.length ? 'Suche oder Kategorie anpassen.' : '„Auktionen aktualisieren“ lädt /auctions/active. Das Öffnen der Seite fragt keine externe Auktionsliste ab.'}</span></div>
        {:else}
          <div class="auction-grid vi-scroll-region">
            {#each filtered as auction (auction.uid)}
              <button type="button" class="auction-tile" class:selected={selectedUid === auction.uid} aria-pressed={selectedUid === auction.uid} onclick={() => { selectedUid = auction.uid; resolveProfiles([auction]) }}>
                <span class="auction-tile-head">
                  <span class="auction-icons auction-tile-icons" aria-hidden="true">
                    <img class="auction-icon-fallback" src={itemIcon(fallbackMaterial(auction))} alt="" loading="lazy" onerror={hideFallbackIcon} />
                    <img class="auction-icon-api" src={`/api/auctions/${encodeURIComponent(auction.uid)}/icon`} data-uid={auction.uid} alt="" loading="lazy" onload={auctionIconLoaded} onerror={retryAuctionIcon} />
                  </span>
                  <span class="auction-tile-name"><strong>{itemName(auction)}</strong><small>{fmtInt(auction.item?.amount || 1)} Stück</small></span>
                </span>
                <span class="auction-tile-seller"><span>Verkäufer</span><strong class:auction-seller-resolving={sellerResolution(auction) === 'resolving'}>{sellerName(auction)}</strong></span>
                <span class="auction-tile-prices">
                  <span><small>Aktuelles Gebot</small><strong>{fmtMoney(auction.currentBid)}</strong></span>
                  {#if auction.instantBuyPrice != null}<span><small>Sofortkauf</small><strong>{fmtMoney(auction.instantBuyPrice)}</strong></span>{/if}
                </span>
                <span class="auction-tile-bottom"><span class="auction-category">{categoryLabel(auction.category)}</span><span class="auction-time"><small>Restzeit</small><strong>{remaining(auction.endTime)}</strong></span></span>
              </button>
            {/each}
          </div>
        {/if}
        <footer class="vi-card-footer vi-data-footer auction-results-footer">
          <span>{filtered.length.toLocaleString('de-DE')} / {auctions.length.toLocaleString('de-DE')} Angebote</span>
          <span>{descending ? 'Absteigend' : 'Aufsteigend'} · {sortLabel()}</span>
        </footer>
      </section>

      <aside class="auction-detail vi-card vi-data-surface" aria-label="Auktionsdetails">
        {#if selected}
          <div class="auction-detail-header">
            <button type="button" class="btn-outline auction-back-button" onclick={() => selectedUid = ''}><Icon icon="lucide:arrow-left" width={14} />Zurück zur Liste</button>
            <div class="auction-detail-title">
              <span class="auction-icons auction-detail-icons" aria-hidden="true">
                <img class="auction-icon-fallback" src={itemIcon(fallbackMaterial(selected))} alt="" loading="lazy" onerror={hideFallbackIcon} />
                <img class="auction-icon-api" src={`/api/auctions/${encodeURIComponent(selected.uid)}/icon`} data-uid={selected.uid} alt="" loading="lazy" onload={auctionIconLoaded} onerror={retryAuctionIcon} />
              </span>
              <div><p class="auction-kicker">ANGEBOTSDETAILS</p><h2>{itemName(selected)}</h2><small>{fmtInt(selected.item?.amount || 1)} Stück · {categoryLabel(selected.category)}</small></div>
            </div>
          </div>
          <div class="auction-detail-scroll vi-scroll-region">
            <section class="auction-detail-section">
              <h3><Icon icon="lucide:tag" width={14} />Auktion</h3>
              <dl class="auction-detail-grid">
                <div><dt>Verkäufer</dt><dd class:auction-seller-resolving={sellerResolution(selected) === 'resolving'}>{sellerName(selected)}</dd></div>
                <div><dt>Status</dt><dd>{statusLabel(selected.state)}</dd></div>
                <div><dt>Gebote</dt><dd>{fmtInt(bidCount(selected))}</dd></div>
                <div><dt>Restzeit</dt><dd>{remaining(selected.endTime)}</dd></div>
              </dl>
            </section>

            <section class="auction-detail-section">
              <h3><Icon icon="lucide:coins" width={14} />Preise</h3>
              <dl class="auction-detail-grid auction-price-grid">
                <div><dt>Startgebot</dt><dd>{fmtMoney(selected.startBid)}</dd></div>
                <div><dt>Aktuelles Gebot</dt><dd>{fmtMoney(selected.currentBid)}</dd></div>
                <div><dt>Sofortkauf</dt><dd>{selected.instantBuyPrice == null ? '–' : fmtMoney(selected.instantBuyPrice)}</dd></div>
                <div><dt>Höchstbietender</dt><dd>{playerName(selected.highestBidder)}</dd></div>
              </dl>
            </section>

            {#if selected.item?.lore?.length}
              <section class="auction-detail-section auction-long-section">
                <h3><Icon icon="lucide:align-left" width={14} />Beschreibung</h3>
                <div class="auction-lore">{#each selected.item.lore as line}{#if line?.trim()}<p>{line}</p>{:else}<div class="auction-lore-break" aria-hidden="true"></div>{/if}{/each}</div>
              </section>
            {/if}

            {#if enchantments(selected).length}
              <section class="auction-detail-section">
                <h3><Icon icon="lucide:sparkles" width={14} />Verzauberungen</h3>
                <ul class="auction-enchantments">{#each enchantments(selected) as name}<li>{name}</li>{/each}</ul>
              </section>
            {/if}

            {#if bidEntries(selected).length}
              <section class="auction-detail-section">
                <h3><Icon icon="lucide:history" width={14} />Gebote</h3>
                <ul class="auction-bids">{#each bidEntries(selected) as [uuid, amount]}<li><span>{playerName(uuid)}</span><strong>{fmtMoney(amount)}</strong></li>{/each}</ul>
                <small>Gebotsbeträge aus dem aktuellen API-Datensatz; Zeitpunkte werden dort nicht mitgeliefert.</small>
              </section>
            {/if}

            <section class="auction-detail-section">
              <h3><Icon icon="lucide:clock-3" width={14} />Zeiten und Kennung</h3>
              <dl class="auction-detail-grid auction-time-grid">
                <div><dt>Start</dt><dd>{date(selected.startTime)}</dd></div>
                <div><dt>Ende</dt><dd>{date(selected.endTime)}</dd></div>
                <div class="auction-uid"><dt>Auktions-ID</dt><dd>{selected.uid}</dd></div>
              </dl>
            </section>
            <small class="auction-readonly">Nur Anzeige. Keine Gebots-, Kauf- oder Serveraktion.</small>
          </div>
        {:else}
          <div class="auction-detail-empty"><Icon icon="lucide:mouse-pointer-click" width={26} /><strong>Auktion auswählen</strong><span>Hier erscheinen alle verfügbaren Details des Angebots.</span></div>
        {/if}
      </aside>
    </div>
  </main>
</div>

<style>
  .auction-page { max-width: 1600px; }
  .auction-heading { display:flex; align-items:end; justify-content:space-between; gap:1rem; }
  .auction-heading h1 { margin:.12rem 0; font-size:clamp(1.55rem,3vw,2rem); }
  .auction-heading p { max-width:46rem; margin:.25rem 0 0; color:var(--vi-text-muted); font-size:.82rem; }
  .auction-kicker { color:var(--vi-accent); font:600 .66rem var(--vi-font-data); letter-spacing:.09em; margin:0; }
  .auction-heading .btn-primary { display:flex; align-items:center; gap:.45rem; white-space:nowrap; }
  .auction-toolbar { display:flex; align-items:center; gap:.5rem; padding:.55rem; margin:0; }
  .auction-search { display:flex; align-items:center; gap:.5rem; flex:1; min-width:12rem; color:var(--vi-text-muted); }
  .auction-search input,.auction-toolbar select { min-width:0; border:1px solid var(--vi-border); background:var(--vi-bg-input); color:var(--vi-text); border-radius:.35rem; padding:.45rem .55rem; }
  .auction-search input { width:100%; }
  .auction-sort-direction { font-weight:700; }
  .auction-notice { flex:0 0 auto; padding:.55rem .75rem; border:1px solid var(--vi-accent); border-radius:.4rem; color:var(--vi-text); }
  .auction-results { border:1px solid var(--vi-border); border-radius:.55rem; background:color-mix(in srgb,var(--vi-bg-card) 88%,transparent); }
  .auction-results-heading { flex:0 0 auto; display:flex; justify-content:space-between; align-items:center; gap:.75rem; padding:.55rem .75rem; border-bottom:1px solid var(--vi-border); }
  .auction-results-heading strong { font-size:.82rem; }
  .auction-results-heading span { color:var(--vi-text-muted); font-size:.7rem; text-align:right; }
  .auction-grid { flex:1 1 auto; display:grid; grid-template-columns:repeat(auto-fill,minmax(min(100%,24rem),1fr)); align-content:start; gap:.7rem; padding:.75rem; }
  .auction-tile { display:flex; flex-direction:column; gap:.75rem; min-width:0; min-height:16.5rem; padding:1.05rem; border:1px solid color-mix(in srgb,var(--vi-border) 72%,transparent); border-radius:.6rem; background:color-mix(in srgb,var(--vi-bg-card) 96%,transparent); color:var(--vi-text); text-align:left; cursor:pointer; transition:border-color .15s,background-color .15s,box-shadow .15s; }
  .auction-tile:hover { border-color:color-mix(in srgb,var(--vi-accent) 58%,var(--vi-border)); background:var(--vi-bg-elevated,var(--vi-hover)); box-shadow:0 4px 14px #0003; }
  .auction-tile.selected { border-color:color-mix(in srgb,var(--vi-accent) 72%,var(--vi-border)); background:color-mix(in srgb,var(--vi-bg-elevated,var(--vi-hover)) 90%,var(--vi-accent)); box-shadow:inset 3px 0 0 var(--vi-accent),0 4px 14px #0003; }
  .auction-tile:focus-visible { outline:2px solid var(--vi-accent); outline-offset:2px; }
  .auction-tile-head { display:flex; align-items:center; gap:.75rem; min-width:0; }
  .auction-icons { position:relative; display:block; width:2.2rem; height:2.2rem; flex:0 0 auto; }
  .auction-icons img { position:absolute; inset:0; width:100%; height:100%; object-fit:contain; image-rendering:pixelated; }
  .auction-icon-api { z-index:1; }
  .auction-tile-icons { width:3.2rem; height:3.2rem; padding:.2rem; border:1px solid color-mix(in srgb,var(--vi-accent) 24%,var(--vi-border)); border-radius:.45rem; background:var(--vi-bg-input); }
  .auction-detail-icons { width:3.5rem; height:3.5rem; padding:.2rem; border:1px solid var(--vi-border); border-radius:.45rem; background:var(--vi-bg-input); }
  .auction-tile-name { display:grid; gap:.22rem; min-width:0; }
  .auction-tile-name strong { display:-webkit-box; overflow:hidden; overflow-wrap:anywhere; -webkit-box-orient:vertical; -webkit-line-clamp:2; font-size:.98rem; line-height:1.25; }
  .auction-tile-name small,.auction-tile-seller span,.auction-tile-prices small,.auction-time small { color:var(--vi-text-muted); font-size:.7rem; }
  .auction-tile-seller { display:grid; grid-template-columns:5rem minmax(0,1fr); align-items:baseline; gap:.5rem; min-width:0; }
  .auction-tile-seller strong { overflow:hidden; text-overflow:ellipsis; white-space:nowrap; font-size:.8rem; }
  .auction-tile-seller strong.auction-seller-resolving { color:var(--vi-text-muted); display:inline-flex; align-items:center; gap:.45rem; }
  .auction-tile-seller strong.auction-seller-resolving::after { content:''; flex:0 0 2.4rem; height:.18rem; border-radius:999px; background:linear-gradient(90deg, var(--vi-line) 0 35%, var(--vi-cyan) 35% 60%, var(--vi-line) 60%); background-size:200% 100%; animation:auction-seller-loading 1s linear infinite; }
  .auction-detail-grid dd.auction-seller-resolving { color:var(--vi-text-muted); }
  @keyframes auction-seller-loading { to { background-position:-200% 0; } }
  .auction-tile-prices { display:grid; grid-template-columns:repeat(2,minmax(0,1fr)); gap:.65rem; padding:.7rem 0 .75rem; border-block:1px solid color-mix(in srgb,var(--vi-border) 64%,transparent); }
  .auction-tile-prices > span { display:grid; gap:.2rem; min-width:0; }
  .auction-tile-prices > span:last-child:nth-child(1) { grid-column:1 / -1; }
  .auction-tile-prices small { line-height:1.2; }
  .auction-tile-prices strong { overflow-wrap:anywhere; color:var(--vi-accent); font:600 .88rem var(--vi-font-data); font-variant-numeric:tabular-nums; }
  .auction-tile-prices > span:nth-child(2) strong { color:var(--vi-buy); }
  .auction-tile-bottom { display:grid; grid-template-columns:minmax(0,1fr) auto; flex:0 0 auto; align-items:center; gap:.75rem; min-width:0; margin-top:auto; padding:.15rem .1rem .2rem; }
  .auction-category { display:block; width:fit-content; max-width:100%; min-width:0; overflow:hidden; padding:.32rem .6rem; border:1px solid color-mix(in srgb,var(--vi-border) 66%,transparent); border-radius:999px; background:color-mix(in srgb,var(--vi-bg-input) 55%,transparent); color:var(--vi-text-muted); font-size:.68rem; text-overflow:ellipsis; white-space:nowrap; }
  .auction-time { display:grid; justify-items:end; gap:.18rem; min-width:0; padding:.1rem .15rem; text-align:right; }
  .auction-time strong { max-width:100%; overflow-wrap:anywhere; font:500 .77rem var(--vi-font-data); font-variant-numeric:tabular-nums; }
  .auction-results-footer { display:flex; justify-content:space-between; gap:.75rem; padding:.45rem .75rem; font-size:.72rem; }
  .auction-empty { display:grid; align-content:center; justify-items:center; gap:.35rem; min-height:8rem; padding:1.5rem; color:var(--vi-text-muted); text-align:center; }
  .auction-empty strong { color:var(--vi-text); }
  .auction-detail { border:1px solid var(--vi-border); border-radius:.55rem; background:color-mix(in srgb,var(--vi-bg-card) 94%,transparent); }
  .auction-detail-header { flex:0 0 auto; padding:1rem 1.05rem; border-bottom:1px solid var(--vi-border); }
  .auction-detail-title { display:flex; align-items:center; gap:.75rem; min-width:0; }
  .auction-detail-title > div { min-width:0; }
  .auction-detail-title h2 { margin:.16rem 0 .2rem; overflow-wrap:anywhere; font-size:1.12rem; line-height:1.2; }
  .auction-detail-title small { color:var(--vi-text-muted); font-size:.72rem; }
  .auction-detail-scroll { flex:1 1 auto; padding:.35rem 1.05rem 1rem; }
  .auction-detail-section { padding:.9rem .15rem 1rem; border-bottom:1px solid color-mix(in srgb,var(--vi-border) 64%,transparent); }
  .auction-detail-section h3 { display:flex; align-items:center; gap:.4rem; margin:0 0 .7rem; color:var(--vi-text); font-size:.82rem; }
  .auction-detail-section h3 :global(svg) { color:var(--vi-accent); }
  .auction-detail-grid { display:grid; grid-template-columns:repeat(2,minmax(0,1fr)); gap:.65rem 1rem; margin:0; }
  .auction-detail-grid > div { min-width:0; }
  .auction-detail-grid dt { color:var(--vi-text-muted); font-size:.68rem; }
  .auction-detail-grid dd { margin:.12rem 0 0; overflow-wrap:anywhere; font-size:.8rem; font-variant-numeric:tabular-nums; }
  .auction-price-grid dd { color:var(--vi-accent); font-family:var(--vi-font-data); font-size:.82rem; }
  .auction-lore { color:var(--vi-text-on-muted); font-size:.8rem; line-height:1.5; overflow-wrap:anywhere; }
  .auction-lore p { margin:.35rem 0; white-space:pre-wrap; }
  .auction-lore-break { height:.42rem; }
  .auction-enchantments { display:flex; flex-wrap:wrap; gap:.5rem .6rem; margin:0; padding:0; list-style:none; }
  .auction-enchantments li { padding:.42rem .72rem; border:1px solid color-mix(in srgb,var(--vi-border) 38%,transparent); border-radius:999px; background:color-mix(in srgb,var(--vi-accent) 8%,var(--vi-bg-input)); color:var(--vi-text-on-muted); font-size:.77rem; line-height:1.25; }
  .auction-bids { display:grid; gap:.25rem; max-height:15rem; overflow:auto; margin:.2rem 0 .4rem; padding:0; list-style:none; }
  .auction-bids li { display:flex; justify-content:space-between; gap:.8rem; padding:.3rem .45rem; border-radius:.3rem; background:color-mix(in srgb,var(--vi-bg-input) 68%,transparent); }
  .auction-bids li span { min-width:0; overflow-wrap:anywhere; color:var(--vi-text-on-muted); }
  .auction-bids li strong { flex:none; color:var(--vi-accent); font:500 .75rem var(--vi-font-data); }
  .auction-detail-section > small { color:var(--vi-text-muted); font-size:.68rem; }
  .auction-uid { grid-column:1 / -1; }
  .auction-uid dd { font-family:var(--vi-font-data); font-size:.68rem; }
  .auction-readonly { display:block; padding:.9rem .15rem .2rem; color:var(--vi-text-muted); font-size:.68rem; }
  .auction-detail-empty { display:grid; align-content:center; justify-items:center; gap:.5rem; flex:1; padding:2rem; color:var(--vi-text-muted); text-align:center; }
  .auction-detail-empty :global(svg) { color:var(--vi-accent); }
  .auction-detail-empty strong { color:var(--vi-text); }
  .auction-detail-empty span { max-width:18rem; font-size:.78rem; }
  .auction-back-button { display:none; margin:0 0 .7rem; }
  @media (min-width: 921px) {
    .auction-layout { display:grid; grid-template-columns:minmax(0,1.7fr) minmax(20rem,1fr); grid-template-rows:minmax(0,1fr); gap:.75rem; }
  }
  @media (max-width: 920px) {
    .auction-heading { align-items:start; flex-direction:column; }
    .auction-toolbar { flex-wrap:wrap; }
    .auction-search { flex-basis:100%; }
    .auction-detail { display:none; }
    .auction-layout.has-selection .auction-results { display:none; }
    .auction-layout.has-selection .auction-detail { display:flex; }
    .auction-back-button { display:inline-flex; align-items:center; gap:.35rem; }
  }
  @media (max-width: 520px) {
    .auction-heading .btn-primary { width:100%; justify-content:center; }
    .auction-toolbar { display:grid; grid-template-columns:minmax(0,1fr) auto; }
    .auction-search { grid-column:1 / -1; }
    .auction-sort-direction { justify-self:start; min-width:2rem; justify-content:center; }
    .auction-toolbar select:last-child { grid-column:1 / -1; }
    .auction-results-heading { align-items:start; flex-direction:column; gap:.15rem; }
    .auction-results-heading span { text-align:left; }
    .auction-grid { grid-template-columns:minmax(0,1fr); gap:.55rem; padding:.55rem; }
    .auction-tile { min-height:18.5rem; padding:.9rem; gap:.65rem; }
    .auction-tile-bottom { grid-template-columns:minmax(0,1fr); align-items:start; gap:.45rem; padding:.25rem .1rem .15rem; }
    .auction-time { justify-items:start; padding:.1rem .15rem; text-align:left; }
    .auction-detail-title h2 { font-size:1rem; }
    .auction-detail-grid { gap:.45rem .55rem; }
  }
</style>
