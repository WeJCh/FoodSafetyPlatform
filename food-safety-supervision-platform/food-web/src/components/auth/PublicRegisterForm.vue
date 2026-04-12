<template>
  <form class="auth-form auth-form--register" @submit.prevent="$emit('submit')">
    <div class="auth-form__field">
      <label class="auth-form__label" for="public-username">用户名</label>
      <div class="auth-input">
        <span class="auth-input__icon" aria-hidden="true">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8">
            <path d="M12 12a4 4 0 1 0-4-4 4 4 0 0 0 4 4Z" />
            <path d="M4 20a8 8 0 0 1 16 0" />
          </svg>
        </span>
        <input
          id="public-username"
          :value="modelValue.username"
          required
          placeholder="请输入您的唯一识别名"
          @input="updateField('username', $event.target.value)"
        />
      </div>
    </div>

    <div class="auth-form__field">
      <label class="auth-form__label" for="public-real-name">真实姓名</label>
      <div class="auth-input">
        <span class="auth-input__icon" aria-hidden="true">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8">
            <path d="M12 12a4 4 0 1 0-4-4 4 4 0 0 0 4 4Z" />
            <path d="M4 20a8 8 0 0 1 16 0" />
          </svg>
        </span>
        <input
          id="public-real-name"
          :value="modelValue.realName"
          placeholder="请输入真实姓名"
          @input="updateField('realName', $event.target.value)"
        />
      </div>
    </div>

    <div class="auth-form__field">
      <label class="auth-form__label" for="public-phone">手机号码</label>
      <div class="auth-input">
        <span class="auth-input__icon" aria-hidden="true">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8">
            <rect x="7" y="2.5" width="10" height="19" rx="2" />
            <path d="M10 5.5h4" />
            <path d="M11.5 18.5h1" />
          </svg>
        </span>
        <input
          id="public-phone"
          :value="modelValue.phone"
          placeholder="请输入11位手机号"
          @input="updateField('phone', $event.target.value)"
        />
      </div>
    </div>

    <div class="auth-form__field">
      <label class="auth-form__label" for="public-password">登录密码</label>
      <div class="auth-input auth-input--with-action">
        <span class="auth-input__icon" aria-hidden="true">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8">
            <rect x="5" y="11" width="14" height="10" rx="2" />
            <path d="M8 11V8a4 4 0 1 1 8 0v3" />
          </svg>
        </span>
        <input
          id="public-password"
          :value="modelValue.password"
          type="password"
          required
          placeholder="设置强密码（至少8位）"
          @input="updateField('password', $event.target.value)"
        />
        <button class="auth-input__action" type="button" aria-label="显示或隐藏密码">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8">
            <path d="M3 3 21 21" />
            <path d="M10.6 10.7a3 3 0 0 0 4 4" />
            <path d="M9.9 5.1A10.9 10.9 0 0 1 12 5c5.2 0 8.8 4.2 9.8 6-0.5 0.8-1.8 2.7-3.9 4.2" />
            <path d="M6.2 6.3C3.7 8 2.3 10.4 2 11c1 1.8 4.6 6 10 6 1 0 2-.2 2.9-.4" />
          </svg>
        </button>
      </div>
    </div>

    <label class="auth-agreement auth-agreement--register">
      <input :checked="agreed" type="checkbox" @change="$emit('update:agreed', $event.target.checked)" />
      <span>
        我已阅读并同意
        <button class="auth-form__link" type="button">《用户服务协议》</button>
        与
        <button class="auth-form__link" type="button">《隐私权政策》</button>
        ，并承诺提供真实合法的备案信息。
      </span>
    </label>

    <button
      class="primary auth-form__submit auth-form__submit--register"
      type="submit"
      :disabled="loading || !agreed"
    >
      {{ loading ? "提交中..." : "提交注册申请" }}
      <span class="auth-form__submit-arrow" aria-hidden="true">→</span>
    </button>
  </form>
</template>

<script setup>
const props = defineProps({
  loading: {
    type: Boolean,
    default: false
  },
  agreed: {
    type: Boolean,
    default: false
  },
  modelValue: {
    type: Object,
    required: true
  }
});

const emit = defineEmits(["submit", "back", "update:agreed", "update:modelValue"]);

function updateField(field, value) {
  emit("update:modelValue", {
    ...props.modelValue,
    [field]: typeof value === "string" ? value.trim() : value
  });
}
</script>
