<template>
  <AuthShell variant="login">
    <template #header>
      <header class="proto-header proto-header--simple">
        <div class="proto-header__inner">
          <RouterLink class="proto-brand proto-brand--simple" :to="{ name: 'login' }">
            食品安全监管
          </RouterLink>
        </div>
      </header>
    </template>

    <section class="login-proto">
      <div class="login-proto__orb login-proto__orb--left"></div>
      <div class="login-proto__orb login-proto__orb--right"></div>

      <div class="login-proto__inner">
        <div class="login-proto__card">
          <div class="login-proto__accent"></div>

          <div class="login-proto__hero">
            <div class="login-proto__icon">
              <span class="material-symbols-outlined">password</span>
            </div>
            <h1>忘记密码</h1>
            <p>当前仓库未扫描到重置密码接口，本页先补齐链路与页面样式，便于后续正式接入。</p>
          </div>

          <form class="proto-form" @submit.prevent="handleSubmit">
            <label class="proto-field">
              <span class="proto-field__label">用户名</span>
              <span class="proto-input">
                <span class="material-symbols-outlined proto-input__icon">person</span>
                <input
                  v-model.trim="form.username"
                  class="proto-input__control"
                  type="text"
                  placeholder="请输入需要找回的用户名"
                  autocomplete="username"
                  required
                />
              </span>
            </label>

            <label class="proto-field">
              <span class="proto-field__label">手机号</span>
              <span class="proto-input">
                <span class="material-symbols-outlined proto-input__icon">phone_iphone</span>
                <input
                  v-model.trim="form.phone"
                  class="proto-input__control"
                  type="tel"
                  placeholder="请输入注册时使用的手机号"
                  autocomplete="tel"
                  required
                />
              </span>
            </label>

            <label class="proto-field">
              <span class="proto-field__label">补充说明</span>
              <textarea
                v-model.trim="form.note"
                class="proto-textarea"
                rows="4"
                placeholder="可选填写，例如最近一次登录时间或账号异常情况。"
              />
            </label>

            <AuthStatusAlert :message="status.message" :type="status.type" />

            <button class="proto-primary-button" type="submit">
              <span>提交找回申请</span>
              <span class="material-symbols-outlined">arrow_forward</span>
            </button>
          </form>

          <div class="register-proto__footer register-proto__footer--single">
            <RouterLink class="proto-link" :to="{ name: 'login' }">返回登录</RouterLink>
          </div>
        </div>
      </div>
    </section>
  </AuthShell>
</template>

<script setup>
import { reactive } from "vue";
import AuthShell from "../../components/auth/AuthShell.vue";
import AuthStatusAlert from "../../components/auth/AuthStatusAlert.vue";

const form = reactive({
  username: "",
  phone: "",
  note: ""
});

const status = reactive({
  message: "",
  type: "info"
});

function handleSubmit() {
  status.message = "当前仓库未发现忘记密码接口。本页已完成路由闭环，后续确认接口契约后再接入提交能力。";
  status.type = "warning";
}
</script>
