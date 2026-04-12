<template>
  <div class="auth-page">
    <slot name="header" />
    <main :class="layoutClass">
      <template v-if="mode === 'centered'">
        <section :class="panelClass">
          <slot />
        </section>
      </template>
      <template v-else>
        <section class="hero-panel auth-shell__hero">
          <slot name="hero" />
        </section>
        <section class="form-panel auth-shell__form">
          <slot />
        </section>
      </template>
    </main>
    <slot name="footer" />
  </div>
</template>

<script setup>
import { computed } from "vue";

const props = defineProps({
  mode: {
    type: String,
    default: "split"
  },
  panelWidth: {
    type: String,
    default: "narrow"
  }
});

const layoutClass = computed(() =>
  props.mode === "centered" ? "auth-shell auth-shell--centered" : "app-shell auth-shell"
);

const panelClass = computed(() =>
  props.panelWidth === "wide" ? "auth-centered-panel auth-centered-panel--wide" : "auth-centered-panel"
);
</script>
