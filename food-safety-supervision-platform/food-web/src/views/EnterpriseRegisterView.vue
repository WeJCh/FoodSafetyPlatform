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

    <div class="auth-composite-card auth-composite-card--enterprise">
      <section class="auth-composite-card__aside auth-composite-card__aside--gradient">
        <div class="auth-register-hero auth-register-hero--enterprise">
          <div class="auth-shield-badge auth-shield-badge--inverse" aria-hidden="true">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8">
              <path d="M12 3 5 6v5c0 5 3.5 8.2 7 10 3.5-1.8 7-5 7-10V6Z" />
              <path d="m9.5 12 1.7 1.7 3.3-3.4" />
            </svg>
          </div>

          <div class="auth-register-hero__copy">
            <h1>企业准入与<br />数字台账接入</h1>
            <p>欢迎加入食品安全数字监管体系。当前先完成企业账号创建，登录后再继续完善企业主体资料与经营档案。</p>
          </div>

          <div class="auth-register-panel-note">
            <h3>阶段 A 注册须知</h3>
            <ul>
              <li>当前页面仅提交账号创建所需的最小字段</li>
              <li>企业名称、信用代码、证照材料不在本页直接提交</li>
              <li>注册完成后进入企业档案流程补齐完整入驻资料</li>
            </ul>
          </div>
        </div>
      </section>

      <section class="auth-composite-card__body">
        <div class="auth-card__header auth-card__header--register">
          <h2 class="auth-card__title">企业用户注册</h2>
          <p class="auth-card__subtitle">先创建企业账号，后续再进入企业资料完善与附件上传流程。</p>
        </div>

        <EnterpriseRegisterForm
          v-model="enterpriseForm"
          v-model:agreed="agreed"
          :loading="loading"
          @submit="handleEnterpriseRegister"
          @back="navigateTo('login')"
        />

        <div v-if="status.message" class="status" :class="status.type">
          {{ status.message }}
        </div>

        <p class="auth-register-login-link">
          已有企业账号？
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
import { registerEnterprise } from "../api/auth";
import AuthFooter from "../components/auth/AuthFooter.vue";
import AuthHeader from "../components/auth/AuthHeader.vue";
import AuthLayout from "../components/auth/AuthLayout.vue";
import EnterpriseRegisterForm from "../components/auth/EnterpriseRegisterForm.vue";

const router = useRouter();

const headerActions = [{ label: "返回登录", target: "login" }];

const loading = ref(false);
const agreed = ref(false);
const status = reactive({ message: "", type: "" });
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

function navigateTo(target) {
  router.push({ name: target }).catch(() => {});
}

async function handleEnterpriseRegister() {
  if (!agreed.value) {
    setStatus("请先阅读并同意企业服务协议与隐私权政策。", "error");
    return;
  }

  loading.value = true;
  setStatus("");
  try {
    await registerEnterprise(enterpriseForm.value);
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
