<script>
  import { onMount } from 'svelte'
  import Icon from '@iconify/svelte'
  import { fmtItem } from '../lib/utils.js'
/** @type {{ activePage: 'market' | 'auctions' | 'shard' | 'redcoins' | 'merchant' | 'history' | 'system' }} */
  let { activePage } = $props()
  let alert = $state(null)
  let initialized = false
  let seen = new Set()
  let pollTimer
  let hideTimer
  const labels = { BUY_ABOVE: 'Kaufpreis über', BUY_BELOW: 'Kaufpreis unter', SELL_ABOVE: 'Verkaufspreis über', SELL_BELOW: 'Verkaufspreis unter', SPREAD_ABOVE: 'Spanne über', SPREAD_BELOW: 'Spanne unter' }
  const amount = value => new Intl.NumberFormat('de-DE', { maximumFractionDigits: 2 }).format(value)

  async function pollAlerts() {
    try {
      const response = await fetch('/api/system/price-alerts/events', { credentials: 'same-origin' })
      if (!response.ok) return
      const events = await response.json()
      if (!initialized) {
        events.forEach(event => seen.add(`${event.id}:${event.timestampMs}`))
        initialized = true
        return
      }
      const fresh = events.filter(event => {
        const key = `${event.id}:${event.timestampMs}`
        if (seen.has(key)) return false
        seen.add(key)
        return event.notification === 'WEB' || event.notification === 'HUD_WEB'
      })
      if (fresh.length) {
        alert = fresh[0]
        clearTimeout(hideTimer)
        hideTimer = setTimeout(() => { alert = null }, 8000)
      }
    } catch { /* local web server may stop while a page is open */ }
  }
  onMount(() => {
    pollAlerts()
    pollTimer = setInterval(pollAlerts, 5000)
    return () => { clearInterval(pollTimer); clearTimeout(hideTimer) }
  })
</script>

<nav class="vi-navbar sticky top-0 z-50">
  <a href="/" class="vi-navbar-brand">
    <span class="vi-navbar-mark"><Icon icon="lucide:store" width={13} /></span>Visotaris
  </a>
  <div class="vi-navbar-links">
    <a href="/" class:active={activePage === 'market'}>
      <Icon icon="lucide:table" width={13} />Markt
    </a>
    <a href="/auctions" class:active={activePage === 'auctions'}>
      <Icon icon="lucide:gavel" width={13} />Auktionshaus
    </a>
    <a href="/shard" class:active={activePage === 'shard'}>
      <Icon icon="lucide:gem" width={13} />Shards
    </a>
    <a href="/redcoins" class:active={activePage === 'redcoins'}>
      <Icon icon="lucide:circle-dollar-sign" width={13} />Redcoins
    </a>
    <a href="/merchant" class:active={activePage === 'merchant'}>
      <Icon icon="lucide:handshake" width={13} />Händler
    </a>
    <a href="/history" class:active={activePage === 'history'}>
      <Icon icon="lucide:trending-up" width={13} />Verlauf
    </a>
    <a href="/system" class:active={activePage === 'system'}>
      <Icon icon="lucide:settings-2" width={13} />System
    </a>
  </div>
</nav>
{#if alert}
  <div class="vi-price-alert" role="status">
    <strong>Preisalarm · {fmtItem(alert.itemKey)}</strong>
    <span>{labels[alert.condition] || alert.condition} {amount(alert.threshold)} · Aktuell: {amount(alert.currentValue)}</span>
    <button type="button" aria-label="Benachrichtigung schließen" onclick={() => { alert = null; clearTimeout(hideTimer) }}>×</button>
  </div>
{/if}

<style>
  .vi-price-alert{position:fixed;z-index:60;right:1rem;bottom:1rem;display:grid;gap:.2rem;max-width:min(26rem,calc(100vw - 2rem));padding:.8rem 2.3rem .8rem 1rem;color:var(--vi-text);background:var(--vi-bg-card);border:1px solid var(--vi-accent);border-radius:.5rem;box-shadow:0 .5rem 2rem #0008}
  .vi-price-alert span{font-size:.82rem;color:var(--vi-text-muted)}
  .vi-price-alert button{position:absolute;right:.5rem;top:.35rem;background:none;border:0;color:var(--vi-text);font-size:1.3rem;cursor:pointer}
</style>
