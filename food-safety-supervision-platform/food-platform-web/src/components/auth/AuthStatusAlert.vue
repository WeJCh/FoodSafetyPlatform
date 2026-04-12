<template>
  <div v-if="message" :class="['status-alert', `status-alert--${type}`]">
    {{ message }}
  </div>
</template>

<script setup>
defineProps({
  message: {
    type: String,
    default: ""
  },
  type: {
    type: String,
    default: "info"
  }
});
</script>
