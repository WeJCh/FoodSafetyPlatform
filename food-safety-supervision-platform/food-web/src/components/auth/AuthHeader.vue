<template>
  <header class="auth-header">
    <div class="auth-header__brand">
      <span v-if="badge" class="auth-header__badge">{{ badge }}</span>
      <strong class="brand-font">{{ title }}</strong>
    </div>
    <nav v-if="actions.length" class="auth-header__links" aria-label="认证导航">
      <button
        v-for="action in actions"
        :key="action.target"
        class="ghost"
        type="button"
        @click="$emit('navigate', action.target)"
      >
        {{ action.label }}
      </button>
    </nav>
  </header>
</template>

<script setup>
defineProps({
  badge: {
    type: String,
    default: ""
  },
  actions: {
    type: Array,
    default: () => []
  },
  title: {
    type: String,
    default: "The Digital Ledger"
  }
});

defineEmits(["navigate"]);
</script>
