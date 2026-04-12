<template>
  <form class="auth-form auth-form--register auth-form--enterprise" @submit.prevent="$emit('submit')">
    <div class="auth-form__field">
      <label class="auth-form__label" for="enterprise-username">用户名</label>
      <div class="auth-input">
        <span class="auth-input__icon" aria-hidden="true">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8">
            <path d="M12 12a4 4 0 1 0-4-4 4 4 0 0 0 4 4Z" />
            <path d="M4 20a8 8 0 0 1 16 0" />
          </svg>
        </span>
        <input
          id="enterprise-username"
          :value="modelValue.username"
          required
          placeholder="请输入企业账号用户名"
          @input="updateField('username', $event.target.value)"
        />
      </div>
    </div>

    <div class="auth-form__field">
      <label class="auth-form__label" for="enterprise-real-name">联系人姓名</label>
      <div class="auth-input">
        <span class="auth-input__icon" aria-hidden="true">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8">
            <path d="M12 12a4 4 0 1 0-4-4 4 4 0 0 0 4 4Z" />
            <path d="M4 20a8 8 0 0 1 16 0" />
          </svg>
        </span>
        <input
          id="enterprise-real-name"
          :value="modelValue.realName"
          placeholder="请输入联系人姓名"
          @input="updateField('realName', $event.target.value)"
        />
      </div>
    </div>

    <div class="auth-form__field">
      <label class="auth-form__label" for="enterprise-phone">联系电话</label>
      <div class="auth-input">
        <span class="auth-input__icon" aria-hidden="true">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8">
            <rect x="7" y="2.5" width="10" height="19" rx="2" />
            <path d="M10 5.5h4" />
            <path d="M11.5 18.5h1" />
          </svg>
        </span>
        <input
          id="enterprise-phone"
          :value="modelValue.phone"
          placeholder="请输入11位手机号码"
          @input="updateField('phone', $event.target.value)"
        />
      </div>
    </div>

    <div class="auth-form__field">
      <label class="auth-form__label" for="enterprise-password">登录密码</label>
      <div class="auth-input auth-input--with-action">
        <span class="auth-input__icon" aria-hidden="true">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8">
            <rect x="5" y="11" width="14" height="10" rx="2" />
            <path d="M8 11V8a4 4 0 1 1 8 0v3" />
          </svg>
        </span>
        <input
          id="enterprise-password"
          :value="modelValue.password"
          type="password"
          required
          placeholder="设置企业账号登录密码"
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

    <div class="auth-register-phase-note">
      <strong>阶段 A：最小注册</strong>
      <span>当前先创建企业账号。企业名称、统一社会信用代码、营业执照等资料将在登录后进入企业档案流程继续完善。</span>
    </div>

    <label class="auth-agreement auth-agreement--register">
      <input :checked="agreed" type="checkbox" @change="$emit('update:agreed', $event.target.checked)" />
      <span>
        我已阅读并同意
        <button class="auth-form__link" type="button">《企业服务协议》</button>
        与
        <button class="auth-form__link" type="button">《隐私权政策》</button>
        ，并知悉完整企业资料将在后续流程中补充提交。
      </span>
    </label>

    <button
      class="primary auth-form__submit auth-form__submit--register"
      type="submit"
      :disabled="loading || !agreed"
    >
      {{ loading ? "提交中..." : "提交入驻申请" }}
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
