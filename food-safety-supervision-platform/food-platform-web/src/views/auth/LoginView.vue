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
              <span class="material-symbols-outlined">verified_user</span>
            </div>
            <h1>食品安全监管平台</h1>
            <p>欢迎回来，请验证您的凭据以进入数字账本工作区。</p>
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
                  placeholder="输入您的电子邮箱或工号"
                  autocomplete="username"
                  required
                />
              </span>
            </label>

            <label class="proto-field">
              <span class="proto-field__row">
                <span class="proto-field__label">密码</span>
                <RouterLink class="proto-link" :to="{ name: 'forgot-password' }">忘记密码？</RouterLink>
              </span>
              <span class="proto-input">
                <span class="material-symbols-outlined proto-input__icon">lock</span>
                <input
                  v-model.trim="form.password"
                  class="proto-input__control"
                  type="password"
                  placeholder="••••••••"
                  autocomplete="current-password"
                  required
                />
              </span>
            </label>

            <label class="proto-checkbox">
              <input v-model="form.remember" type="checkbox" />
              <span>在此设备上记住我</span>
            </label>

            <AuthStatusAlert :message="status.message" :type="status.type" />

            <button class="proto-primary-button" type="submit" :disabled="loading">
              <span>{{ loading ? "登录中..." : "立即登录" }}</span>
              <span class="material-symbols-outlined">login</span>
            </button>
          </form>

          <div class="proto-divider">
            <span>尚未拥有账户？</span>
          </div>

          <div class="login-proto__actions">
            <RouterLink class="proto-action-card" :to="{ name: 'public-register' }">
              <span class="material-symbols-outlined">person_add</span>
              <strong>公众注册</strong>
            </RouterLink>

            <RouterLink class="proto-action-card" :to="{ name: 'enterprise-register' }">
              <span class="material-symbols-outlined">domain_add</span>
              <strong>企业入驻</strong>
            </RouterLink>
          </div>
        </div>
      </div>
    </section>
  </AuthShell>
</template>

<script setup>
import { reactive, ref, watch } from "vue";
import { useRoute, useRouter } from "vue-router";
import { login } from "../../api/auth";
import AuthShell from "../../components/auth/AuthShell.vue";
import AuthStatusAlert from "../../components/auth/AuthStatusAlert.vue";
import { commitResolvedSession } from "../../session/authRuntime";
import { isEnterpriseIdentity } from "../../session/authSession";

const router = useRouter();
const route = useRoute();
const form = reactive({ username: "", password: "", remember: false });
const status = reactive({ message: "", type: "info" });
const loading = ref(false);

function setStatus(message, type = "info") {
  status.message = message;
  status.type = type;
}

watch(
  () => [route.query.registered, route.query.reason],
  ([registered, reason]) => {
    if (registered === "public") {
      setStatus("公众账号注册成功，请使用新账号登录。", "success");
      return;
    }
    if (registered === "enterprise") {
      setStatus("企业账号注册成功，请使用企业账号登录。", "success");
      return;
    }
    if (reason === "expired") {
      setStatus("登录状态已失效，请重新登录。", "warning");
    }
  },
  { immediate: true }
);

async function handleSubmit() {
  loading.value = true;
  setStatus("");

  try {
    const result = await login({ username: form.username, password: form.password });

    if (!isEnterpriseIdentity(result)) {
      setStatus("当前版本只接入了认证页与企业端工作区，请使用企业账号登录。", "error");
      return;
    }

    commitResolvedSession(result, form.remember);
    const redirect = typeof route.query.redirect === "string" ? route.query.redirect : "";
    router.replace(redirect || { name: "enterprise-dashboard" });
  } catch (error) {
    setStatus(error.message || "登录失败，请稍后重试。", "error");
  } finally {
    loading.value = false;
  }
}
</script>