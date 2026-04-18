<template>
  <AuthLayout>
    <template #header>
      <AuthHeader @navigate="switchView" />
    </template>

    <template #hero>
      <div class="hero-content">
        <span class="badge">食品安全平台</span>
        <h1>用户中心登录与注册</h1>
        <p>
          已有账号请登录。注册时请选择公众或企业用户类型，企业注册信息与公众一致。
        </p>
        <div class="hero-highlights">
          <div>
            <strong>第一步</strong>
            <span>选择用户类型</span>
          </div>
          <div>
            <strong>第二步</strong>
            <span>填写基础资料</span>
          </div>
          <div>
            <strong>第三步</strong>
            <span>注册完成即可使用</span>
          </div>
        </div>
      </div>
    </template>

    <div class="card">
      <div class="tabs">
        <button :class="{ active: view === 'login' }" @click="switchView('login')">登录</button>
        <button :class="{ active: view !== 'login' }" @click="switchView('select')">注册</button>
      </div>

      <LoginForm v-if="view === 'login'" v-model="loginForm" :loading="loading" @submit="handleLogin" />

      <div v-else-if="view === 'select'" class="type-select">
        <div class="type-card" @click="switchView('public')">
          <strong>公众用户</strong>
          <span>提交投诉与查询信息</span>
        </div>
        <div class="type-card" @click="switchView('enterprise')">
          <strong>企业用户</strong>
          <span>企业账号注册</span>
        </div>
        <div class="type-card note">
          <strong>监管人员</strong>
          <span>由系统管理员统一添加</span>
        </div>
      </div>

      <PublicRegisterForm
        v-else-if="view === 'public'"
        v-model="publicForm"
        :loading="loading"
        @submit="handlePublicRegister"
        @back="switchView('select')"
      />

      <EnterpriseRegisterForm
        v-else-if="view === 'enterprise'"
        v-model="enterpriseForm"
        :loading="loading"
        @submit="handleEnterpriseRegister"
        @back="switchView('select')"
      />

      <div v-if="status.message" class="status" :class="status.type">
        {{ status.message }}
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
import { reactive, ref, watch } from "vue";
import { useRoute, useRouter } from "vue-router";
import AuthFooter from "../components/auth/AuthFooter.vue";
import AuthHeader from "../components/auth/AuthHeader.vue";
import AuthLayout from "../components/auth/AuthLayout.vue";
import EnterpriseRegisterForm from "../components/auth/EnterpriseRegisterForm.vue";
import LoginForm from "../components/auth/LoginForm.vue";
import PublicRegisterForm from "../components/auth/PublicRegisterForm.vue";
import { login, register, registerEnterprise, verify } from "../api/auth";
import { fetchRegulatorProfile } from "../api/regulation";
import { getDefaultRouteLocation } from "../router";
import { resolveRegulatorRoleType } from "../session/authSession";
import { commitResolvedSession } from "../session/authRuntime";

const router = useRouter();
const route = useRoute();

function normalizeView(target) {
  if (target === "public" || target === "enterprise" || target === "select") {
    return target;
  }
  return "login";
}

function resolveViewFromRoute(name) {
  if (name === "public-register") {
    return "public";
  }
  if (name === "enterprise-register") {
    return "enterprise";
  }
  return "login";
}

const view = ref(normalizeView(resolveViewFromRoute(route.name)));
const loading = ref(false);
const token = ref("");
const status = reactive({ message: "", type: "" });

const loginForm = ref({
  username: "",
  password: ""
});

const publicForm = ref({
  username: "",
  password: "",
  realName: "",
  phone: ""
});

const enterpriseForm = ref({
  username: "",
  password: "",
  realName: "",
  phone: ""
});

function setStatus(message, type = "info") {
  status.message = message;
  status.type = type;
}

function switchView(target) {
  const nextView = normalizeView(target);
  view.value = nextView;
  setStatus("");

  if (nextView === "login" && route.name !== "login") {
    router.push({ name: "login" }).catch(() => {});
    return;
  }

  if (nextView === "public" && route.name !== "public-register") {
    router.push({ name: "public-register" }).catch(() => {});
    return;
  }

  if (nextView === "enterprise" && route.name !== "enterprise-register") {
    router.push({ name: "enterprise-register" }).catch(() => {});
  }
}

watch(
  () => route.name,
  (nextRouteName) => {
    view.value = normalizeView(resolveViewFromRoute(nextRouteName));
    setStatus("");
  }
);

async function handleLogin() {
  loading.value = true;
  setStatus("");
  try {
    const result = await login(loginForm.value);
    if (!result || typeof result !== "object") {
      throw new Error("登录接口返回为空或格式不正确");
    }
    if (!result.token) {
      throw new Error("登录接口未返回 token");
    }
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

async function handlePublicRegister() {
  loading.value = true;
  setStatus("");
  try {
    await register(publicForm.value);
    setStatus("注册完成，现在可以登录。", "success");
    switchView("login");
  } catch (error) {
    setStatus(error.message || "注册失败", "error");
  } finally {
    loading.value = false;
  }
}

async function handleEnterpriseRegister() {
  loading.value = true;
  setStatus("");
  try {
    await registerEnterprise(enterpriseForm.value);
    setStatus("注册完成，现在可以登录。", "success");
    switchView("login");
  } catch (error) {
    setStatus(error.message || "注册失败", "error");
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
