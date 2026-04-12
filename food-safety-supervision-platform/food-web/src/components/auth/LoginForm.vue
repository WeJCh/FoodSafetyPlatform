<template>
  <form class="auth-form auth-form--login" @submit.prevent="$emit('submit')">
    <div class="auth-form__field">
      <label class="auth-form__label" for="login-username">用户名</label>
      <div class="auth-input">
        <span class="auth-input__icon" aria-hidden="true">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8">
            <path d="M12 12a4 4 0 1 0-4-4 4 4 0 0 0 4 4Z" />
            <path d="M4 20a8 8 0 0 1 16 0" />
          </svg>
        </span>
        <input
          id="login-username"
          :value="modelValue.username"
          required
          placeholder="请输入账号"
          @input="updateField('username', $event.target.value)"
        />
      </div>
    </div>

    <div class="auth-form__field">
      <div class="auth-form__label-row">
        <label class="auth-form__label" for="login-password">密码</label>
        <button class="auth-form__link" type="button">忘记密码？</button>
      </div>
      <div class="auth-input">
        <span class="auth-input__icon" aria-hidden="true">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8">
            <rect x="5" y="11" width="14" height="10" rx="2" />
            <path d="M8 11V8a4 4 0 1 1 8 0v3" />
          </svg>
        </span>
        <input
          id="login-password"
          :value="modelValue.password"
          type="password"
          required
          placeholder="请输入密码"
          @input="updateField('password', $event.target.value)"
        />
      </div>
    </div>

    <label class="auth-checkbox">
      <input type="checkbox" />
      <span>在此设备上记住我</span>
    </label>

    <button class="primary auth-form__submit" type="submit" :disabled="loading">
      {{ loading ? "登录中..." : "立即登录" }}
    </button>
  </form>
</template>

<script setup>
const props = defineProps({
  loading: {
    type: Boolean,
    default: false
  },
  modelValue: {
    type: Object,
    required: true
  }
});

const emit = defineEmits(["submit", "update:modelValue"]);

function updateField(field, value) {
  emit("update:modelValue", {
    ...props.modelValue,
    [field]: typeof value === "string" ? value.trim() : value
  });
}
</script>
