<script>
  /** @type {{ activePage: 'market' | 'shard' | 'history' }} */
  let { activePage } = $props()
</script>

<nav class="navbar navbar-expand-lg sticky-top">
  <div class="container-fluid">
    <a class="navbar-brand" href="/"><i class="bi bi-shop me-2"></i>Visotaris OPMod</a>
    <div class="navbar-nav ms-auto">
      <a class="nav-link" class:active={activePage === 'market'} href="/">
        <i class="bi bi-table me-1"></i>Markt
      </a>
      <a class="nav-link" class:active={activePage === 'shard'} href="/shard">
        <i class="bi bi-gem me-1"></i>Shards
      </a>
      <a class="nav-link" class:active={activePage === 'history'} href="/history">
        <i class="bi bi-graph-up me-1"></i>Verlauf
      </a>
    </div>
  </div>
</nav>
