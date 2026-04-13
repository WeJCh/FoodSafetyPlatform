<template>
  <span class="enterprise-status-chip" :class="[`enterprise-status-chip--${tone}`, { 'is-small': small }]">
    <i class="enterprise-status-chip__dot" />
    <span>{{ label }}</span>
  </span>
</template>

<script setup>
defineProps({
  label: {
    type: String,
    default: ""
  },
  tone: {
    type: String,
    default: "neutral"
  },
  small: {
    type: Boolean,
    default: false
  }
});
</script>
