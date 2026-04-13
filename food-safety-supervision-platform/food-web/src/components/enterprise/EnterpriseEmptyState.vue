<template>
  <div class="enterprise-empty-state">
    <div class="enterprise-empty-state__icon">!</div>
    <strong class="enterprise-empty-state__title">{{ title }}</strong>
    <p v-if="description" class="enterprise-empty-state__description">{{ description }}</p>
    <slot />
  </div>
</template>

<script setup>
defineProps({
  title: {
    type: String,
    default: "暂无数据"
  },
  description: {
    type: String,
    default: ""
  }
});
</script>
