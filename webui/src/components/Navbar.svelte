<script>
  import Icon from '@iconify/svelte'
  /** @type {{ activePage: 'market' | 'shard' | 'redcoins' | 'history' }} */
  let { activePage } = $props()
</script>

<nav class="sticky top-0 z-50 flex items-center gap-4 px-4 py-2 border-b"
     style="background-color:var(--vi-bg-card); border-color:var(--vi-border)">
  <a href="/" class="flex items-center gap-2 font-bold tracking-wide no-underline"
     style="color:var(--vi-accent)">
    <Icon icon="lucide:store" width={15} />Visotaris OPMod
  </a>
  <div class="flex items-center gap-4 ml-auto">
    <a href="/" class="flex items-center gap-1 text-sm no-underline transition-colors"
       style="color:{activePage === 'market' ? 'var(--vi-text)' : 'var(--vi-text-muted)'}">
      <Icon icon="lucide:table" width={13} />Markt
    </a>
    <a href="/shard" class="flex items-center gap-1 text-sm no-underline transition-colors"
       style="color:{activePage === 'shard' ? 'var(--vi-text)' : 'var(--vi-text-muted)'}">
      <Icon icon="lucide:gem" width={13} />Shards
    </a>
    <a href="/redcoins" class="flex items-center gap-1 text-sm no-underline transition-colors"
       style="color:{activePage === 'redcoins' ? 'var(--vi-text)' : 'var(--vi-text-muted)'}">
      <Icon icon="lucide:circle-dollar-sign" width={13} />Redcoins
    </a>
    <a href="/history" class="flex items-center gap-1 text-sm no-underline transition-colors"
       style="color:{activePage === 'history' ? 'var(--vi-text)' : 'var(--vi-text-muted)'}">
      <Icon icon="lucide:trending-up" width={13} />Verlauf
    </a>
  </div>
</nav>
