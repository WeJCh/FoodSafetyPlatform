<template>
  <div class="enterprise-shell-layout">
    <aside class="enterprise-shell-layout__sidebar">
      <slot name="sidebar" />
    </aside>
    <div class="enterprise-shell-layout__main">
      <header class="enterprise-shell-layout__topbar">
        <slot name="topbar" />
      </header>
      <main class="enterprise-shell-layout__content">
        <slot />
      </main>
    </div>
  </div>
</template>
