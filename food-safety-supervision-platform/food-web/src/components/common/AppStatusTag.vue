<template>
  <span class="app-status-tag" :class="`is-${tone}`">
    <slot>{{ label }}</slot>
  </span>
</template>

<script setup>
defineProps({
  label: {
    type: String,
    default: "-"
  },
  tone: {
    type: String,
    default: "neutral"
  }
});
</script>

<style scoped>
.app-status-tag {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: 24px;
  padding: 0 10px;
  border-radius: 999px;
  border: 1px solid transparent;
  font-size: 11px;
  font-weight: 800;
  white-space: nowrap;
}

.app-status-tag.is-success {
  background: rgba(33, 156, 84, 0.1);
  color: #1f6e45;
  border-color: rgba(33, 156, 84, 0.2);
}

.app-status-tag.is-danger {
  background: rgba(186, 26, 26, 0.12);
  color: #93000a;
  border-color: rgba(186, 26, 26, 0.22);
}

.app-status-tag.is-warning {
  background: rgba(209, 122, 0, 0.12);
  color: #9a6700;
  border-color: rgba(209, 122, 0, 0.2);
}

.app-status-tag.is-info {
  background: rgba(0, 96, 160, 0.12);
  color: #005a9c;
  border-color: rgba(0, 96, 160, 0.18);
}

.app-status-tag.is-neutral {
  background: rgba(94, 104, 128, 0.1);
  color: #56607a;
  border-color: rgba(94, 104, 128, 0.18);
}
</style>
