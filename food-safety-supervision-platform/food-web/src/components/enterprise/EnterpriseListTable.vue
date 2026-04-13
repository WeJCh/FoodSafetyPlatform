<template>
  <section class="enterprise-list-table">
    <div class="enterprise-list-table__header">
      <slot name="header" />
    </div>
    <div v-if="hasData" class="enterprise-list-table__body">
      <slot />
    </div>
    <slot v-else name="empty">
      <EnterpriseEmptyState :title="emptyTitle" :description="emptyDescription" />
    </slot>
  </section>
</template>

<script setup>
import EnterpriseEmptyState from "./EnterpriseEmptyState.vue";

defineProps({
  hasData: {
    type: Boolean,
    default: false
  },
  emptyTitle: {
    type: String,
    default: "暂无数据"
  },
  emptyDescription: {
    type: String,
    default: ""
  }
});
</script>
