<script>
  import { fade } from 'svelte/transition'
  import Icon from '@iconify/svelte'
  import Navbar from '../../components/Navbar.svelte'

  let system = $state(null)
  let meta = $state(null)
  let loading = $state(false)
  let error = $state(null)

  const memoryPercent = $derived.by(() => {
    if (!system?.runtime?.heapMaxBytes) return 0
    return Math.round(system.runtime.heapUsedBytes / system.runtime.heapMaxBytes * 100)
  })
  const options = $derived(system?.options ?? {})

  const bytes = value => {
    if (!Number.isFinite(value)) return '–'
    const units = ['B', 'KiB', 'MiB', 'GiB']
    let number = value
    let index = 0
    while (number >= 1024 && index < units.length - 1) { number /= 1024; index++ }
    return new Intl.NumberFormat('de-DE', { maximumFractionDigits: 1 }).format(number) + ' ' + units[index]
  }
  const age = value => value == null ? 'noch nicht geladen' : value < 60 ? value + ' s' : Math.floor(value / 60) + ' min'
  const optionState = value => value ? 'AN' : 'AUS'

  async function loadData() {
    loading = true
    error = null
    try {
      const [systemResponse, metaResponse] = await Promise.all([fetch('/api/system'), fetch('/api/meta')])
      if (!systemResponse.ok) throw new Error('HTTP ' + systemResponse.status)
      system = await systemResponse.json()
      meta = metaResponse.ok ? await metaResponse.json() : null
    } catch (e) {
      error = 'Systemdaten konnten nicht geladen werden: ' + e.message
    } finally {
      loading = false
    }
  }

  $effect.root(() => { loadData() })
</script>

<Navbar activePage="system" />

<main class="vi-page">
  <div class="flex items-center gap-3 mb-3 flex-wrap">
    <h5 class="vi-page-heading m-0 flex items-center gap-2 font-semibold text-base"><Icon icon="lucide:settings-2" width={15} style="color:var(--vi-accent)" />Optionen & System</h5>
    <span class={loading ? 'badge-secondary' : error ? 'badge-stale' : 'badge-fresh'}>{loading ? 'Laden…' : error ? 'Fehler' : 'Lokal'}</span>
    <button class="btn-icon ml-auto" onclick={loadData} title="Status aktualisieren"><Icon icon="lucide:refresh-cw" width={14} class={loading ? 'spin' : ''} /></button>
  </div>

  <div class="system-note mb-3"><Icon icon="lucide:shield-check" width={15} />Diese Seite ist absichtlich schreibgeschützt. Optionen werden sicher im ModMenu oder über die Visotaris-Keybinds geändert.</div>
  {#if error}<div class="vi-alert-error mb-3">{error}</div>{/if}

  {#if system}
    <section class="system-grid" transition:fade={{ duration: 150 }}>
      <article class="vi-card system-card">
        <div class="vi-card-header"><span><Icon icon="lucide:server" width={14} /> Laufzeit</span><span class="system-muted">v{system.application.modVersion}</span></div>
        <dl class="system-list"><div><dt>Betriebssystem</dt><dd>{system.runtime.os} {system.runtime.osVersion}</dd></div><div><dt>Architektur</dt><dd>{system.runtime.architecture}</dd></div><div><dt>Java</dt><dd>{system.runtime.java}</dd></div><div><dt>Prozessoren</dt><dd>{system.runtime.processors}</dd></div></dl>
      </article>

      <article class="vi-card system-card">
        <div class="vi-card-header"><span><Icon icon="lucide:memory-stick" width={14} /> Speicher</span><span class="system-muted">{memoryPercent} %</span></div>
        <div class="system-memory"><div class="system-progress"><span style={`width:${memoryPercent}%`}></span></div><div class="system-memory-values"><span>{bytes(system.runtime.heapUsedBytes)} genutzt</span><span>{bytes(system.runtime.heapMaxBytes)} Maximum</span></div></div>
      </article>

      <article class="vi-card system-card">
        <div class="vi-card-header"><span><Icon icon="lucide:database" width={14} /> Datenquellen</span><span class="system-muted">lokaler Cache</span></div>
        <dl class="system-list"><div><dt>Marktpreise</dt><dd>{age(meta?.market?.ageSeconds)}</dd></div><div><dt>Händlerkurse</dt><dd>{age(meta?.merchant?.ageSeconds)}</dd></div><div><dt>Web-Interface</dt><dd>127.0.0.1:{system.application.webUiPort}</dd></div></dl>
      </article>

      <article class="vi-card system-card">
        <div class="vi-card-header"><span><Icon icon="lucide:sliders-horizontal" width={14} /> Aktive Optionen</span><span class="system-muted">nur Anzeige</span></div>
        <dl class="system-list compact"><div><dt>Observer-Modus</dt><dd class:option-on={options.observerMode} class:option-off={!options.observerMode}>{optionState(options.observerMode)}</dd></div><div><dt>Tooltips / HUD</dt><dd>{optionState(options.marketTooltips)} / {optionState(options.hud)}</dd></div><div><dt>Container / Schnellzugriff</dt><dd>{optionState(options.containerOverlay)} / {optionState(options.quickButtons)}</dd></div><div><dt>Markt / Händler-Refresh</dt><dd>{Math.round((options.marketRefreshSeconds ?? 0) / 60)} / {Math.round((options.merchantRefreshSeconds ?? 0) / 60)} min</dd></div></dl>
      </article>
    </section>
  {:else if loading}
    <div class="loading-overlay">Systemdaten werden geladen…</div>
  {/if}
</main>
