<template>
  <article class="enterprise-summary-card" :class="tone ? `enterprise-summary-card--${tone}` : ''">
    <div class="enterprise-summary-card__head">
      <span class="enterprise-summary-card__eyebrow">{{ eyebrow }}</span>
      <slot name="meta" />
    </div>
    <strong class="enterprise-summary-card__value">{{ value }}</strong>
    <div class="enterprise-summary-card__title">{{ title }}</div>
    <p v-if="description" class="enterprise-summary-card__description">{{ description }}</p>
  </article>
</template>

<script setup>
defineProps({
  eyebrow: {
    type: String,
    default: ""
  },
  value: {
    type: String,
    default: ""
  },
  title: {
    type: String,
    default: ""
  },
  description: {
    type: String,
    default: ""
  },
  tone: {
    type: String,
    default: ""
  }
});
</script>
