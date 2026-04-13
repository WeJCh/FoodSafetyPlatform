<template>
  <section class="enterprise-filter-bar">
    <div v-if="title || description" class="enterprise-filter-bar__intro">
      <strong v-if="title">{{ title }}</strong>
      <span v-if="description">{{ description }}</span>
    </div>
    <div class="enterprise-filter-bar__content">
      <slot />
    </div>
  </section>
</template>

<script setup>
defineProps({
  title: {
    type: String,
    default: ""
  },
  description: {
    type: String,
    default: ""
  }
});
</script>
