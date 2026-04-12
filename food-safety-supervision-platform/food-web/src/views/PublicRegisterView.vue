<template>
  <AuthLayout mode="centered" panelWidth="wide">
    <template #header>
      <AuthHeader
        class="auth-header--register"
        :actions="headerActions"
        title="食品安全监管"
        @navigate="navigateTo"
      />
    </template>

    <div class="auth-composite-card auth-composite-card--public">
      <section class="auth-composite-card__aside">
        <div class="auth-register-hero auth-register-hero--public">
          <div class="auth-shield-badge auth-shield-badge--inverse" aria-hidden="true">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8">
              <path d="M12 3 5 6v5c0 5 3.5 8.2 7 10 3.5-1.8 7-5 7-10V6Z" />
              <path d="m9.5 12 1.7 1.7 3.3-3.4" />
            </svg>
          </div>

          <div class="auth-register-hero__copy">
            <h1>监管权威与<br />食品安全保障</h1>
            <p>欢迎加入数字化食品安全监管系统。通过建立真实、透明的身份档案，我们共同维护全行业的合规与安全。</p>
          </div>

          <div class="auth-register-hero__points">
            <div>
              <span class="auth-register-hero__check"></span>
              <span>实名身份认证</span>
            </div>
            <div>
              <span class="auth-register-hero__check"></span>
              <span>全程数字化审计</span>
            </div>
            <div>
              <span class="auth-register-hero__check"></span>
              <span>监管政策实时同步</span>
            </div>
          </div>
        </div>
      </section>

      <section class="auth-composite-card__body">
        <div class="auth-card__header auth-card__header--register">
          <h2 class="auth-card__title">创建新账户</h2>
          <p class="auth-card__subtitle">请填写以下基础信息以完成注册程序</p>
        </div>

        <PublicRegisterForm
          v-model="publicForm"
          v-model:agreed="agreed"
          :loading="loading"
          @submit="handlePublicRegister"
          @back="navigateTo('login')"
        />

        <div v-if="status.message" class="status" :class="status.type">
          {{ status.message }}
        </div>

        <p class="auth-register-login-link">
          已有系统账号？
          <button class="auth-form__link" type="button" @click="navigateTo('login')">立即登录</button>
        </p>
      </section>
    </div>

    <template #footer>
      <AuthFooter />
    </template>
  </AuthLayout>
</template>

<script setup>
import { reactive, ref } from "vue";
import { useRouter } from "vue-router";
import { register } from "../api/auth";
import AuthFooter from "../components/auth/AuthFooter.vue";
import AuthHeader from "../components/auth/AuthHeader.vue";
import AuthLayout from "../components/auth/AuthLayout.vue";
import PublicRegisterForm from "../components/auth/PublicRegisterForm.vue";

const router = useRouter();

const headerActions = [{ label: "返回登录", target: "login" }];

const loading = ref(false);
const agreed = ref(false);
const status = reactive({ message: "", type: "" });
const publicForm = ref({
  username: "",
  password: "",
  realName: "",
  phone: ""
});

function setStatus(message, type = "info") {
  status.message = message;
  status.type = type;
}

function navigateTo(target) {
  router.push({ name: target }).catch(() => {});
}

async function handlePublicRegister() {
  if (!agreed.value) {
    setStatus("请先阅读并同意用户服务协议与隐私权政策。", "error");
    return;
  }

  loading.value = true;
  setStatus("");
  try {
    await register(publicForm.value);
    setStatus("注册完成，现在可以登录。", "success");
    setTimeout(() => {
      navigateTo("login");
    }, 200);
  } catch (error) {
    setStatus(error.message || "注册失败", "error");
  } finally {
    loading.value = false;
  }
}
</script>
