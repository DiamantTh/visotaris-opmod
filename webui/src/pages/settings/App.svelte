<script>
  import { onMount } from 'svelte'
  import Navbar from '../../components/Navbar.svelte'
  import SystemSubnav from '../../components/SystemSubnav.svelte'
  import { fmtItem } from '../../lib/utils.js'
  const alertPage = window.location.pathname === '/system/price-alerts'

  let configured = $state(null)
  let authenticated = $state(false)
  let password = $state('')
  let confirmation = $state('')
  let alerts = $state([])
  let enabled = $state(true)
  let items = $state([])
  let itemsLoaded = false
  let events = $state([])
  let tooltip = $state({ showMarketPrices: true, showBuyPrice: true, showSellPrice: true, showMerchantRates: true, showShardRates: true, showDataAge: false, showStaleData: true, maxAgeSeconds: 900 })
  let busy = $state(false)
  let error = $state('')
  let notice = $state('')
  let editingId = $state('')
  let form = $state({ itemKey: '', condition: 'BUY_ABOVE', threshold: '', enabled: true, repeat: false, cooldownSeconds: 300, rearmOnExit: true, notification: 'HUD' })
  const conditions = [
    ['BUY_ABOVE', 'Kaufpreis steigt über'], ['BUY_BELOW', 'Kaufpreis fällt unter'],
    ['SELL_ABOVE', 'Verkaufspreis steigt über'], ['SELL_BELOW', 'Verkaufspreis fällt unter'],
    ['SPREAD_ABOVE', 'Spanne überschreitet'], ['SPREAD_BELOW', 'Spanne unterschreitet']
  ]
  const labels = Object.fromEntries(conditions)
  const numberText = value => value == null || !Number.isFinite(value) ? '–' : new Intl.NumberFormat('de-DE', { minimumFractionDigits: 2, maximumFractionDigits: 2 }).format(value)
  const stateLabel = { watching: 'Beobachtet', paused: 'Pausiert', globally_paused: 'Global pausiert', waiting_for_data: 'Wartet auf Marktdaten', triggered: 'Ausgelöst', rearm_pending: 'Wartet auf Rückkehr', cooldown: 'Cooldown' }
  const channelLabel = { HUD: 'Minecraft-HUD', WEB: 'Web-UI (solange offen)', HUD_WEB: 'Minecraft-HUD und Web-UI' }
  let timer

  async function request(url, options = {}) {
    const response = await fetch(url, { credentials: 'same-origin', ...options })
    if (response.status === 401 || response.status === 428) { authenticated = false; return null }
    if (!response.ok) throw new Error((await response.text()) || `HTTP ${response.status}`)
    return response.status === 204 ? true : response.json()
  }
  async function load() {
    error = ''
    try {
      if (alertPage) {
        const [a, ev] = await Promise.all([request('/api/system/price-alerts'), request('/api/system/price-alerts/events')])
        if (!a || !ev) return
        authenticated = true; enabled = a.enabled; alerts = a.rules; events = ev
        if (!itemsLoaded) {
          const response = await fetch('/api/market')
          const market = response.ok ? await response.json() : {}
          items = Object.keys(market).sort((x, y) => fmtItem(x).localeCompare(fmtItem(y), 'de'))
          itemsLoaded = true
        }
        if (!form.itemKey && items.length) form.itemKey = items[0]
        clearInterval(timer); timer = setInterval(refreshLive, 5000)
      } else {
        const t = await request('/api/system/tooltips')
        if (!t) return
        authenticated = true; tooltip = t
      }
    } catch (e) { error = e.message }
  }
  async function refreshLive() {
    try {
      const [a, ev] = await Promise.all([request('/api/system/price-alerts'), request('/api/system/price-alerts/events')])
      if (a && ev) { enabled = a.enabled; alerts = a.rules; events = ev }
    } catch { /* retain last good local snapshot */ }
  }
  async function loadAuth() {
    try { const result = await fetch('/api/system/status').then(r => r.json()); configured = result.configured } catch { error = 'Lokales Webinterface nicht erreichbar.' }
  }
  async function authenticate() {
    busy = true; error = ''
    try {
      const setup = !configured
      const body = new URLSearchParams({ password, ...(setup ? { confirmation } : {}) })
      const response = await fetch(setup ? '/api/system/setup' : '/api/system/login', { method: 'POST', headers: { 'Content-Type': 'application/x-www-form-urlencoded' }, body })
      if (!response.ok) throw new Error(await response.text())
      password = ''; confirmation = ''; configured = true; await load()
    } catch (e) { error = e.message }
    finally { busy = false }
  }
  async function saveTooltip() {
    busy = true; error = ''; notice = ''
    try { await request('/api/system/tooltips', { method: 'PUT', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(tooltip) }); notice = 'Tooltip-Einstellungen gespeichert.' }
    catch (e) { error = e.message } finally { busy = false }
  }
  function edit(row) {
    const r = row.rule; editingId = r.id
    form = { itemKey: r.itemKey, condition: r.condition, threshold: r.threshold, enabled: r.enabled, repeat: r.repeat, cooldownSeconds: r.cooldownSeconds, rearmOnExit: r.rearmOnExit, notification: r.notification === 'CHAT' ? 'HUD' : r.notification === 'BOTH' ? 'HUD_WEB' : r.notification }
    document.getElementById('alert-form')?.scrollIntoView({ behavior: 'smooth', block: 'center' })
  }
  function resetForm() { editingId = ''; form = { itemKey: items[0] || '', condition: 'BUY_ABOVE', threshold: '', enabled: true, repeat: false, cooldownSeconds: 300, rearmOnExit: true, notification: 'HUD' } }
  async function saveAlert(event) {
    event.preventDefault(); error = ''; notice = ''
    const threshold = Number(form.threshold)
    if (!form.itemKey || !Number.isFinite(threshold) || Math.abs(threshold) > 1e15) { error = 'Bitte Item und einen gültigen Schwellenwert angeben.'; return }
    const body = { ...form, threshold, cooldownSeconds: Number(form.cooldownSeconds) }
    busy = true
    try {
      await request(editingId ? `/api/system/price-alerts/${encodeURIComponent(editingId)}` : '/api/system/price-alerts', { method: editingId ? 'PUT' : 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(body) })
      notice = editingId ? 'Alarmregel aktualisiert.' : 'Alarmregel erstellt.'; resetForm(); await load()
    } catch (e) { error = e.message } finally { busy = false }
  }
  async function toggleGlobal() {
    enabled = !enabled
    try { await request('/api/system/price-alerts/global', { method: 'PUT', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ enabled }) }); await load() }
    catch (e) { enabled = !enabled; error = e.message }
  }
  async function toggle(row) {
    busy = true; error = ''; notice = ''
    try {
      await request(`/api/system/price-alerts/${encodeURIComponent(row.rule.id)}`, { method: 'PUT', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ ...row.rule, enabled: !row.rule.enabled }) })
      notice = row.rule.enabled ? 'Alarmregel pausiert.' : 'Alarmregel aktiviert.'
      await load()
    } catch (e) { error = e.message } finally { busy = false }
  }
  async function remove(row) {
    if (!confirm(`Alarm für ${fmtItem(row.rule.itemKey)} löschen?`)) return
    try { await request(`/api/system/price-alerts/${encodeURIComponent(row.rule.id)}`, { method: 'DELETE' }); notice = 'Alarmregel gelöscht.'; await load() } catch (e) { error = e.message }
  }
  onMount(() => {
    document.title = alertPage ? 'Visotaris – Preisalarme' : 'Visotaris – Einstellungen'
    loadAuth().then(load)
    return () => clearInterval(timer)
  })
</script>

<Navbar activePage="system" />
<SystemSubnav activePage={alertPage ? 'alerts' : 'settings'} />
<main class="vi-page settings-page">
  <header class="settings-heading"><div><p class="settings-kicker">{alertPage ? 'LOKALE MARKTBEOBACHTUNG' : 'SYSTEM · CLIENT-OPTIONEN'}</p><h1>{alertPage ? 'Preisalarme' : 'Einstellungen'}</h1><p>{alertPage ? 'Regeln werten ausschließlich synchronisierte Cache-Daten aus. Kein Kauf, Verkauf oder Minecraft-Serveraufruf.' : 'Lege fest, welche Informationen in Minecraft-Item-Tooltips erscheinen und wann Cache-Daten als veraltet gelten.'}</p></div></header>

  {#if error}<div class="vi-alert-error" role="alert">{error}</div>{/if}
  {#if notice}<div class="settings-notice" role="status">{notice}</div>{/if}
  {#if !authenticated}
    <section class="vi-card settings-auth"><div class="vi-card-header">Systemzugang</div><div class="vi-card-body"><p>{configured ? 'Melde dich mit dem lokalen Systempasswort an.' : 'Lege zuerst ein starkes Passwort für die lokale Systemoberfläche fest.'}</p>
      <form class="settings-form" onsubmit={(e) => { e.preventDefault(); authenticate() }}>
        <label>Passwort<input type="password" bind:value={password} autocomplete={configured ? 'current-password' : 'new-password'} required minlength="12" /></label>
        {#if !configured}<label>Passwort bestätigen<input type="password" bind:value={confirmation} autocomplete="new-password" required minlength="12" /></label>{/if}
        <button class="btn-primary" disabled={busy}>{configured ? 'Anmelden' : 'Passwort einrichten'}</button>
      </form>
      <small>Die Konfiguration ist nur über den lokalen Systemzugang erreichbar. Falls bereits angemeldet: Seite neu laden.</small>
    </div></section>
  {:else}
    {#if alertPage}
    <section class="settings-section" aria-labelledby="alerts-title">
      <div class="settings-section-head"><div><p class="settings-kicker">MARKT-CACHE · KEINE EXTRA-ABFRAGEN</p><h2 id="alerts-title">Preisalarme</h2></div><label class="settings-toggle"><input type="checkbox" checked={enabled} onchange={toggleGlobal} />Alle Alarme aktiv</label></div>
      {#if alerts.length}
        <div class="settings-rule-list">
          {#each alerts as row (row.rule.id)}
            <article class="vi-card alert-card">
              <div class="alert-card-main"><div><h3>{fmtItem(row.rule.itemKey)}</h3><p>{labels[row.rule.condition]} <strong>{numberText(row.rule.threshold)}</strong></p></div><span class="alert-state" data-state={row.state}>{stateLabel[row.state] || row.state}</span></div>
              <div class="alert-meta"><span>Zuletzt bekannter Wert: {row.currentValue == null ? 'keine Daten' : numberText(row.currentValue)}</span><span>Datenstand: {row.lastUpdatedAtMs ? new Date(row.lastUpdatedAtMs).toLocaleString('de-DE') : 'noch nie synchronisiert'}</span><span>{row.rule.repeat ? `Wiederholung · ${row.rule.cooldownSeconds}s Cooldown` : 'Einmalig'}</span><span>Benachrichtigung: {channelLabel[row.rule.notification] || row.rule.notification}</span></div>
              <div class="alert-actions"><button class="btn-outline" onclick={() => toggle(row)}>{row.rule.enabled ? 'Pausieren' : 'Aktivieren'}</button><button class="btn-outline" onclick={() => edit(row)}>Bearbeiten</button><button class="btn-outline danger" onclick={() => remove(row)}>Entfernen</button></div>
            </article>
          {/each}
        </div>
      {:else}<div class="empty-state"><strong>Noch keine Preisalarme</strong><small>Lege eine Regel an, um einen Kauf-, Verkaufs- oder Spannenwert zu beobachten.</small></div>{/if}

      <form id="alert-form" class="vi-card settings-form-card" onsubmit={saveAlert}>
        <div class="vi-card-header">{editingId ? 'Alarmregel bearbeiten' : 'Neuen Alarm anlegen'}</div>
        <div class="settings-form-grid">
          <label>Marktitem<select bind:value={form.itemKey} required>{#each items as key}<option value={key}>{fmtItem(key)}</option>{/each}</select></label>
          <label>Bedingung<select bind:value={form.condition}>{#each conditions as [value, label]}<option {value}>{label}</option>{/each}</select></label>
          <label>Schwellenwert<input type="number" step="any" bind:value={form.threshold} required /></label>
          <label>Benachrichtigung<select bind:value={form.notification}><option value="HUD">Minecraft-HUD</option><option value="WEB">Web-UI (solange offen)</option><option value="HUD_WEB">Minecraft-HUD und Web-UI</option></select></label>
          <label class="settings-check"><input type="checkbox" bind:checked={form.repeat} />Wiederholt benachrichtigen</label>
          <label>Cooldown (Sekunden)<input type="number" min="10" max="86400" step="1" bind:value={form.cooldownSeconds} disabled={!form.repeat} /></label>
          <label class="settings-check"><input type="checkbox" bind:checked={form.rearmOnExit} disabled={!form.repeat} />Nach Verlassen des Schwellenbereichs wieder scharf schalten</label>
          <label class="settings-check"><input type="checkbox" bind:checked={form.enabled} />Regel direkt aktivieren</label>
        </div>
        {#if !items.length}<p class="settings-help">Der Marktcache ist noch leer. Items sind nach der nächsten regulären Synchronisierung auswählbar.</p>{/if}
        <div class="settings-form-actions"><button class="btn-primary" disabled={busy || !items.length}>{editingId ? 'Änderungen speichern' : 'Alarm erstellen'}</button>{#if editingId}<button type="button" class="btn-outline" onclick={resetForm}>Abbrechen</button>{/if}</div>
      </form>
      {#if events.length}<div class="settings-events"><h3>Zuletzt ausgelöst</h3>{#each events.slice(0, 8) as event}<p><time>{new Date(event.timestampMs).toLocaleTimeString('de-DE')}</time> · {fmtItem(event.itemKey)} — {labels[event.condition]} {numberText(event.currentValue)}</p>{/each}</div>{/if}
    </section>
    {:else}
    <section class="settings-section" aria-labelledby="tooltip-title">
      <div class="settings-section-head"><div><p class="settings-kicker">CLIENT-SEITIG · CACHE-ONLY</p><h2 id="tooltip-title">Item-Tooltips</h2></div><button class="btn-primary" onclick={saveTooltip} disabled={busy}>Einstellungen speichern</button></div>
      <p class="settings-help">Diese Zusätze erscheinen im normalen Minecraft-Tooltip, wenn du im Inventar, einer Kiste oder einem Menü mit der Maus über einen Item-Slot fährst. Sie ergänzen die üblichen Item-Eigenschaften und Verzauberungen. Off-Hand, dauerhaftes HUD und Container-Gesamtwert sind getrennte Anzeigen.</p>
      <div class="vi-card tooltip-options">
        <label class="settings-check"><input type="checkbox" bind:checked={tooltip.showMarketPrices} />Marktpreise im Tooltip aktivieren</label>
        <div class="settings-nested">
          <label class="settings-check"><input type="checkbox" bind:checked={tooltip.showBuyPrice} disabled={!tooltip.showMarketPrices} />Kaufpreis anzeigen</label>
          <label class="settings-check"><input type="checkbox" bind:checked={tooltip.showSellPrice} disabled={!tooltip.showMarketPrices} />Verkaufspreis anzeigen</label>
        </div>
        <label class="settings-check"><input type="checkbox" bind:checked={tooltip.showMerchantRates} />Andere Händlerkurse (z. B. Redcoins) anzeigen</label>
        <label class="settings-check"><input type="checkbox" bind:checked={tooltip.showShardRates} />Shard-Kurse anzeigen</label>
        <label class="settings-check"><input type="checkbox" bind:checked={tooltip.showDataAge} />Alter der verwendeten Cache-Daten anzeigen</label>
        <label class="settings-check"><input type="checkbox" bind:checked={tooltip.showStaleData} />Veraltete Daten weiterhin anzeigen</label>
        <label class="settings-age">Daten ab diesem Alter als veraltet markieren (Sekunden)<input type="number" min="60" max="86400" step="60" bind:value={tooltip.maxAgeSeconds} /></label>
        <p class="settings-help">Tooltip-Aufrufe lesen nur lokale Caches. Das Datenalter bezieht sich auf die letzte erfolgreiche Synchronisierung; es werden dabei weder Netzwerkabfragen noch Spielbefehle ausgelöst.</p>
      </div>
    </section>
    {/if}
  {/if}
</main>

<style>
  .settings-page{max-width:1100px}.settings-heading{margin:.5rem 0 1.5rem}.settings-heading h1{margin:.1rem 0;font-size:clamp(1.6rem,4vw,2.2rem)}.settings-heading p{color:var(--vi-text-muted);max-width:48rem}.settings-kicker{color:var(--vi-accent)!important;font:600 .68rem var(--vi-font-data);letter-spacing:.09em;margin:0}.settings-section{margin:2rem 0 2.5rem}.settings-section-head{display:flex;align-items:end;justify-content:space-between;gap:1rem;margin-bottom:.8rem}.settings-section h2{margin:.15rem 0;font-size:1.35rem}.settings-rule-list{display:grid;gap:.65rem}.alert-card{padding:.8rem 1rem}.alert-card-main{display:flex;justify-content:space-between;gap:.8rem;align-items:center}.alert-card h3{font-size:1rem;margin:0}.alert-card p{margin:.2rem 0;color:var(--vi-text-muted);font-size:.85rem}.alert-card p strong{color:var(--vi-text);font-family:var(--vi-font-data)}.alert-state{font-size:.72rem;padding:.2rem .5rem;border:1px solid var(--vi-border);border-radius:999px;white-space:nowrap}.alert-state[data-state="triggered"]{color:var(--vi-buy)}.alert-state[data-state="paused"],.alert-state[data-state="globally_paused"]{color:var(--vi-text-muted)}.alert-meta{display:flex;flex-wrap:wrap;gap:.35rem 1rem;margin:.6rem 0;color:var(--vi-text-muted);font-size:.72rem}.alert-actions{display:flex;gap:.4rem;flex-wrap:wrap}.danger{color:#f87171}.settings-form-card{margin-top:.8rem}.settings-form-grid{display:grid;grid-template-columns:repeat(2,minmax(0,1fr));gap:.75rem;padding:1rem}.settings-form label,.settings-form-card label,.settings-auth label,.settings-age{display:grid;gap:.35rem;color:var(--vi-text-muted);font-size:.8rem}.settings-form input:not([type=checkbox]),.settings-form-card input:not([type=checkbox]),:global(.settings-form-card select),.settings-auth input,.settings-age input{width:100%;min-width:0;padding:.55rem .65rem;border-radius:.35rem;border:1px solid var(--vi-border);background:var(--vi-bg-input);color:var(--vi-text)}.settings-check{display:flex!important;align-items:center;gap:.5rem;min-height:2.3rem}.settings-check input{accent-color:var(--vi-accent)}.settings-form-actions{display:flex;gap:.5rem;padding:0 1rem 1rem}.settings-auth{max-width:35rem}.settings-auth .vi-card-body{display:grid;gap:.8rem}.settings-auth p,.settings-auth small,.settings-help{color:var(--vi-text-muted);font-size:.82rem}.settings-form{display:grid;gap:.7rem}.settings-notice{margin:.5rem 0;padding:.65rem .8rem;border:1px solid #4ade8059;color:#86efac;background:#14532d30;border-radius:.4rem}.tooltip-options{padding:1rem;display:grid;gap:.7rem}.settings-nested{margin-left:1.3rem;border-left:1px solid var(--vi-border);padding-left:.8rem;display:grid;gap:.35rem}.settings-age{max-width:24rem}.settings-events{margin-top:1rem;padding:.6rem .8rem;border:1px solid var(--vi-border);border-radius:.45rem}.settings-events h3{font-size:.9rem;margin:.2rem 0 .5rem}.settings-events p{font-size:.78rem;color:var(--vi-text-muted);margin:.25rem 0}.settings-events time{font-family:var(--vi-font-data);color:var(--vi-accent)}@media(max-width:650px){.settings-section-head{align-items:flex-start;flex-direction:column}.settings-form-grid{grid-template-columns:1fr}.alert-card-main{align-items:flex-start;flex-direction:column}.settings-age{max-width:100%}}
</style>
