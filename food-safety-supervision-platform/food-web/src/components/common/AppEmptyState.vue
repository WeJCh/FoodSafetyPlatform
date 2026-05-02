<template>
  <div class="app-empty-state">
    <span class="material-symbols-outlined" aria-hidden="true">inbox</span>
    <strong>{{ title }}</strong>
    <p v-if="description">{{ description }}</p>
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

<style scoped>
.app-empty-state {
  min-height: 120px;
  border: 1px dashed rgba(195, 198, 211, 0.7);
  border-radius: 10px;
  background: rgba(248, 250, 253, 0.78);
  padding: 24px 16px;
  display: grid;
  place-items: center;
  text-align: center;
  gap: 6px;
}

.app-empty-state .material-symbols-outlined {
  font-size: 28px;
  color: #64749a;
}

.app-empty-state strong {
  color: var(--primary, #002660);
  font-size: 15px;
}

.app-empty-state p {
  margin: 0;
  color: var(--on-surface-variant, #5e6880);
  font-size: 13px;
  line-height: 1.55;
}
</style>
