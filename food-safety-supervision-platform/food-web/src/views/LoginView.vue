<template>
  <AuthLayout mode="centered">
    <template #header>
      <AuthHeader class="auth-header--login" title="食品安全监管" @navigate="navigateTo" />
    </template>

    <div class="card auth-card auth-card--login">
      <div class="auth-login-brand">
        <div class="auth-shield-badge auth-shield-badge--surface" aria-hidden="true">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8">
            <path d="M12 3 5 6v5c0 5 3.5 8.2 7 10 3.5-1.8 7-5 7-10V6Z" />
            <path d="m9.5 12 1.7 1.7 3.3-3.4" />
          </svg>
        </div>
        <h1 class="auth-login-brand__title">食品安全监管平台</h1>
        <p class="auth-login-brand__subtitle">欢迎回来，请验证您的凭据以访问数字台账。</p>
      </div>

      <LoginForm v-model="loginForm" :loading="loading" @submit="handleLogin" />

      <div v-if="status.message" class="status" :class="status.type">
        {{ status.message }}
      </div>

      <div class="auth-divider">
        <span>还没有账号？</span>
      </div>

      <div class="auth-link-grid auth-link-grid--login">
        <button class="type-card auth-entry-card" type="button" @click="navigateTo('public-register')">
          <span class="auth-entry-card__icon" aria-hidden="true">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8">
              <path d="M12 12a4 4 0 1 0-4-4 4 4 0 0 0 4 4Z" />
              <path d="M4 20a8 8 0 0 1 16 0" />
              <path d="M19 8h4" />
              <path d="M21 6v4" />
            </svg>
          </span>
          <strong>公众注册</strong>
          <span>新建公众账号</span>
        </button>
        <button class="type-card auth-entry-card" type="button" @click="navigateTo('enterprise-register')">
          <span class="auth-entry-card__icon" aria-hidden="true">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8">
              <path d="M3 21h18" />
              <path d="M5 21V7l7-4 7 4v14" />
              <path d="M9 10h1" />
              <path d="M14 10h1" />
              <path d="M9 14h1" />
              <path d="M14 14h1" />
            </svg>
          </span>
          <strong>企业入驻</strong>
          <span>创建企业账号</span>
        </button>
      </div>

      <div v-if="token" class="token-box">
        <div class="token-header">
          <span>令牌</span>
          <button class="ghost" type="button" @click="handleVerify">验证</button>
        </div>
        <textarea readonly rows="3">{{ token }}</textarea>
      </div>
    </div>

    <template #footer>
      <AuthFooter />
    </template>
  </AuthLayout>
</template>

<script setup>
import { reactive, ref } from "vue";
import { useRouter } from "vue-router";
import { login, verify } from "../api/auth";
import { fetchRegulatorProfile } from "../api/regulation";
import AuthFooter from "../components/auth/AuthFooter.vue";
import AuthHeader from "../components/auth/AuthHeader.vue";
import AuthLayout from "../components/auth/AuthLayout.vue";
import LoginForm from "../components/auth/LoginForm.vue";
import { getDefaultRouteLocation } from "../router";
import { resolveRegulatorRoleType } from "../session/authSession";
import { commitResolvedSession } from "../session/authRuntime";

const router = useRouter();

const loading = ref(false);
const token = ref("");
const status = reactive({ message: "", type: "" });
const loginForm = ref({
  username: "",
  password: ""
});

function setStatus(message, type = "info") {
  status.message = message;
  status.type = type;
}

function navigateTo(target) {
  router.push({ name: target }).catch(() => {});
}

async function handleLogin() {
  loading.value = true;
  setStatus("");
  try {
    const result = await login(loginForm.value);
    const roles = Array.isArray(result.roles) ? result.roles : [];
    const sessionPayload = {
      token: result.token,
      userId: result.userId ?? null,
      username: result.username,
      userType: result.userType,
      roles
    };

    if (roles.includes("REGULATOR_ADMIN") || roles.includes("REGULATOR_ENFORCER") || result.userType === "REGULATOR") {
      let roleType = resolveRegulatorRoleType(sessionPayload);
      if (!roleType) {
        const profile = await fetchRegulatorProfile(result.token).catch(() => null);
        roleType = resolveRegulatorRoleType(profile);
      }
      if (!roleType) {
        token.value = result.token;
        setStatus("登录成功，但未能确认监管角色，请稍后重试或检查监管员资料接口。", "error");
        return;
      }
      sessionPayload.roleType = roleType;
    }

    const session = commitResolvedSession(sessionPayload);
    const targetLocation = getDefaultRouteLocation(session);

    if (targetLocation.name === "login") {
      token.value = result.token;
      setStatus("登录成功，但未识别到可用角色。", "error");
      return;
    }

    setStatus("");
    await router.replace(targetLocation);
  } catch (error) {
    setStatus(error.message || "登录失败", "error");
  } finally {
    loading.value = false;
  }
}

async function handleVerify() {
  if (!token.value) {
    setStatus("请先登录。", "error");
    return;
  }
  try {
    const result = await verify(token.value);
    setStatus(result.valid ? "令牌有效" : "令牌无效", result.valid ? "success" : "error");
  } catch (error) {
    setStatus(error.message || "验证失败", "error");
  }
}
</script>
