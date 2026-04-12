<template>
  <AuthShell variant="register">
    <template #header>
      <header class="proto-header proto-header--register">
        <div class="proto-header__inner">
          <div class="proto-brand-group">
            <span class="material-symbols-outlined proto-brand__icon">shield_person</span>
            <RouterLink class="proto-brand" :to="{ name: 'login' }">食品安全监管</RouterLink>
          </div>

          <RouterLink class="proto-return-link" :to="{ name: 'login' }">
            <span class="material-symbols-outlined">login</span>
            返回登录
          </RouterLink>
        </div>
      </header>
    </template>

    <section class="register-proto register-proto--public">
      <div class="register-proto__frame">
        <aside class="register-proto__hero register-proto__hero--public">
          <div class="register-proto__hero-image"></div>

          <div class="register-proto__hero-content">
            <div class="register-proto__badge">
              <span class="material-symbols-outlined">verified_user</span>
            </div>

            <h1>监管权威与<br />食品安全保障</h1>
            <p>
              欢迎加入数字化食品安全监管系统。通过建立真实、透明的身份档案，我们共同维护全行业的合规与安全。
            </p>

            <div class="register-proto__hero-list">
              <div>
                <span class="material-symbols-outlined">check_circle</span>
                <span>实名身份认证</span>
              </div>
              <div>
                <span class="material-symbols-outlined">check_circle</span>
                <span>全程数字化审计</span>
              </div>
              <div>
                <span class="material-symbols-outlined">check_circle</span>
                <span>监管政策实时同步</span>
              </div>
            </div>
          </div>
        </aside>

        <section class="register-proto__panel">
          <div class="register-proto__panel-header">
            <h2>创建新账户</h2>
            <p>请填写以下基础信息以完成注册程序</p>
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
                  placeholder="请输入您的唯一识别名"
                  autocomplete="username"
                  required
                />
              </span>
            </label>

            <label class="proto-field">
              <span class="proto-field__label">手机号码</span>
              <span class="proto-input">
                <span class="material-symbols-outlined proto-input__icon">phone_iphone</span>
                <input
                  v-model.trim="form.phone"
                  class="proto-input__control"
                  type="tel"
                  placeholder="请输入11位手机号"
                  autocomplete="tel"
                  required
                />
              </span>
            </label>

            <label class="proto-field">
              <span class="proto-field__label">登录密码</span>
              <span class="proto-input">
                <span class="material-symbols-outlined proto-input__icon">lock</span>
                <input
                  v-model.trim="form.password"
                  class="proto-input__control"
                  type="password"
                  placeholder="设置强密码（至少8位）"
                  autocomplete="new-password"
                  required
                />
              </span>
            </label>

            <label class="proto-checkbox proto-checkbox--stacked">
              <input v-model="form.agreed" type="checkbox" />
              <span>
                我已阅读并同意
                <a href="#">《用户服务协议》</a>
                与
                <a href="#">《隐私权政策》</a>
                ，并承诺提供真实合法的备案信息。
              </span>
            </label>

            <AuthStatusAlert :message="status.message" :type="status.type" />

            <button class="proto-primary-button" type="submit" :disabled="loading">
              <span>{{ loading ? "提交中..." : "提交注册申请" }}</span>
              <span class="material-symbols-outlined">arrow_forward</span>
            </button>
          </form>

          <div class="register-proto__footer">
            <span>已有系统账号？</span>
            <RouterLink class="proto-link" :to="{ name: 'login' }">立即登录</RouterLink>
          </div>
        </section>
      </div>
    </section>
  </AuthShell>
</template>

<script setup>
import { reactive, ref } from "vue";
import { useRouter } from "vue-router";
import { register } from "../../api/auth";
import AuthShell from "../../components/auth/AuthShell.vue";
import AuthStatusAlert from "../../components/auth/AuthStatusAlert.vue";

const router = useRouter();
const form = reactive({
  username: "",
  phone: "",
  password: "",
  agreed: false
});

const status = reactive({
  message: "",
  type: "info"
});

const loading = ref(false);

function setStatus(message, type = "info") {
  status.message = message;
  status.type = type;
}

function isValidPhone(phone) {
  return /^1\d{10}$/.test(phone);
}

async function handleSubmit() {
  const payload = {
    username: form.username,
    phone: form.phone,
    password: form.password
  };

  if (!isValidPhone(payload.phone)) {
    setStatus("请输入 11 位手机号。", "warning");
    return;
  }

  if (payload.password.length < 8) {
    setStatus("密码长度至少为 8 位。", "warning");
    return;
  }

  if (!form.agreed) {
    setStatus("请先阅读并同意用户协议。", "warning");
    return;
  }

  loading.value = true;
  setStatus("");

  try {
    await register(payload);
    await router.replace({
      name: "login",
      query: {
        registered: "public"
      }
    });
  } catch (error) {
    setStatus(error.message || "注册失败，请稍后重试。", "error");
  } finally {
    loading.value = false;
  }
}
</script>
