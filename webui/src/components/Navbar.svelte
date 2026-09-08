<script>
  import Icon from '@iconify/svelte'
  /** @type {{ activePage: 'market' | 'shard' | 'redcoins' | 'merchant' | 'history' }} */
  let { activePage } = $props()
</script>

<nav class="vi-navbar sticky top-0 z-50">
  <a href="/" class="vi-navbar-brand">
    <span class="vi-navbar-mark"><Icon icon="lucide:store" width={13} /></span>Visotaris
  </a>
  <div class="vi-navbar-links">
    <a href="/" class:active={activePage === 'market'}>
      <Icon icon="lucide:table" width={13} />Markt
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
  </div>
</nav>
