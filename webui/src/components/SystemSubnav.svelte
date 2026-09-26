<script>
  import Icon from '@iconify/svelte'

  /** @type {{ activePage: 'overview' | 'mcinfo' | 'settings' }} */
  let { activePage } = $props()
</script>

<nav class="system-subnav" aria-label="Systembereiche">
  <a href="/system" aria-current={activePage === 'overview' ? 'page' : undefined} class:active={activePage === 'overview'}>
    <Icon icon="lucide:layout-dashboard" width={14} />Übersicht
  </a>
  <a href="/system/mcinfo" aria-current={activePage === 'mcinfo' ? 'page' : undefined} class:active={activePage === 'mcinfo'}>
    <Icon icon="lucide:cpu" width={14} />Minecraft & F3
  </a>
  <a href="/system/settings" aria-current={activePage === 'settings' ? 'page' : undefined} class:active={activePage === 'settings'}>
    <Icon icon="lucide:bell" width={14} />Alarme & Tooltips
  </a>
</nav>

<style>
  .system-subnav{display:flex;flex-wrap:wrap;gap:.35rem;margin:-.45rem 0 1rem;padding:.3rem;border:1px solid var(--vi-border);border-radius:.55rem;background:color-mix(in srgb,var(--vi-surface,#182016) 76%,transparent)}
  .system-subnav a{display:inline-flex;align-items:center;gap:.4rem;padding:.48rem .7rem;border-radius:.38rem;color:var(--vi-text-muted);font-size:.8rem;text-decoration:none;transition:background-color .15s,color .15s}
  .system-subnav a:hover{color:var(--vi-text);background:color-mix(in srgb,var(--vi-accent) 10%,transparent)}
  .system-subnav a.active{color:var(--vi-text);background:color-mix(in srgb,var(--vi-accent) 17%,transparent);box-shadow:inset 0 0 0 1px color-mix(in srgb,var(--vi-accent) 38%,transparent)}
  @media(max-width:480px){.system-subnav{display:grid;grid-template-columns:repeat(2,minmax(0,1fr))}.system-subnav a{justify-content:center;padding:.55rem .35rem}}
</style>
