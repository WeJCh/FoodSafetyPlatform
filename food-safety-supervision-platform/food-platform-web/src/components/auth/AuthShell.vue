<template>
  <div :class="['auth-page', `auth-page--${variant}`]">
    <slot name="header" />

    <main class="auth-page__main">
      <slot />
    </main>

    <slot name="footer">
      <footer class="proto-footer">
        <div class="proto-footer__inner">
          <span>© 2024 Regulatory Authority. All rights reserved.</span>
          <div class="proto-footer__links">
            <a href="#">Privacy Policy</a>
            <a href="#">Terms of Service</a>
            <a href="#">Accessibility</a>
            <a href="#">Contact Support</a>
          </div>
        </div>
      </footer>
    </slot>
  </div>
</template>

<script setup>
defineProps({
  variant: {
    type: String,
    default: "login"
  }
});
</script>
