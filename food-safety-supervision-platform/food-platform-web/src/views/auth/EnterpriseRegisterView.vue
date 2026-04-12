<template>
  <AuthShell variant="enterprise">
    <template #header>
      <header class="proto-header proto-header--enterprise">
        <div class="proto-header__inner">
          <RouterLink class="proto-brand proto-brand--simple" :to="{ name: 'login' }">
            食品安全监管
          </RouterLink>

          <div class="proto-header__actions">
            <RouterLink class="proto-header__ghost" :to="{ name: 'login' }">登录</RouterLink>
            <span class="proto-header__active">企业注册</span>
          </div>
        </div>
      </header>
    </template>

    <section class="enterprise-proto">
      <div class="enterprise-proto__frame">
        <aside class="enterprise-proto__aside">
          <article class="enterprise-proto__hero-card">
            <div class="enterprise-proto__hero-content">
              <h1>企业准入与数字账本注册</h1>
              <p>
                欢迎加入食品安全数字监管体系。注册企业账号将为您提供实时合规性分析、追溯链条管理以及官方数字证书签发。
              </p>

              <div class="enterprise-proto__feature-list">
                <div>
                  <span class="material-symbols-outlined">verified_user</span>
                  <div>
                    <strong>法定权威认证</strong>
                    <span>对接国家食品安全监管数据库</span>
                  </div>
                </div>
                <div>
                  <span class="material-symbols-outlined">account_tree</span>
                  <div>
                    <strong>追溯链管理</strong>
                    <span>全环节、全要素、可追溯数字化管理</span>
                  </div>
                </div>
              </div>
            </div>
          </article>

          <article class="enterprise-proto__notice-card">
            <h3>注册须知</h3>
            <ul>
              <li>准备有效的社会统一信用代码</li>
              <li>需上传法定代表人身份证明文件</li>
              <li>完成注册后需进行为期1-3个工作日的资质审核</li>
            </ul>
          </article>
        </aside>

        <section class="enterprise-proto__panel">
          <form class="enterprise-form" @submit.prevent="handleSubmit">
            <section class="enterprise-section">
              <div class="enterprise-section__header">
                <span class="material-symbols-outlined">business</span>
                <h2>企业主体信息</h2>
              </div>

              <p class="enterprise-section__note">本阶段仅保留展示，不随注册接口提交。</p>

              <div class="enterprise-form__grid">
                <label class="proto-field">
                  <span class="proto-field__label">企业官方名称</span>
                  <input
                    v-model.trim="form.enterpriseName"
                    class="proto-input__control proto-input__control--plain"
                    type="text"
                    placeholder="请按营业执照填写全称"
                  />
                </label>

                <div class="enterprise-form__grid enterprise-form__grid--two">
                  <label class="proto-field">
                    <span class="proto-field__label">统一社会信用代码</span>
                    <input
                      v-model.trim="form.creditCode"
                      class="proto-input__control proto-input__control--plain"
                      type="text"
                      placeholder="18位统一社会信用代码"
                    />
                  </label>

                  <label class="proto-field">
                    <span class="proto-field__label">企业类型</span>
                    <select v-model="form.enterpriseType" class="proto-input__control proto-input__control--plain">
                      <option value="MANUFACTURING">生产企业</option>
                      <option value="DISTRIBUTION">流通环节</option>
                      <option value="CATERING">餐饮服务</option>
                      <option value="IMPORT_EXPORT">进出口贸易</option>
                    </select>
                  </label>
                </div>
              </div>
            </section>

            <section class="enterprise-section">
              <div class="enterprise-section__header">
                <span class="material-symbols-outlined">account_circle</span>
                <h2>负责人信息</h2>
              </div>

              <p class="enterprise-section__note">本次只将负责人姓名映射到现有接口的 `realName`，联系电话映射到 `phone`。</p>

              <div class="enterprise-form__grid enterprise-form__grid--two">
                <label class="proto-field">
                  <span class="proto-field__label">主要负责人姓名</span>
                  <input
                    v-model.trim="form.principalName"
                    class="proto-input__control proto-input__control--plain"
                    type="text"
                    placeholder="法定代表人或项目负责人"
                    required
                  />
                </label>

                <label class="proto-field">
                  <span class="proto-field__label">联系电话</span>
                  <input
                    v-model.trim="form.phone"
                    class="proto-input__control proto-input__control--plain"
                    type="tel"
                    placeholder="11位手机号码"
                    required
                  />
                </label>

                <label class="proto-field enterprise-form__span-2">
                  <span class="proto-field__label">官方电子邮箱</span>
                  <input
                    v-model.trim="form.email"
                    class="proto-input__control proto-input__control--plain"
                    type="email"
                    placeholder="用于接收监管通知与公文"
                  />
                </label>
              </div>
            </section>

            <section class="enterprise-section">
              <div class="enterprise-section__header">
                <span class="material-symbols-outlined">shield</span>
                <h2>安全凭证</h2>
              </div>

              <p class="enterprise-section__note">为对齐现有企业注册接口，本页增加“登录账号”字段作为最小前端适配。</p>

              <div class="enterprise-form__grid enterprise-form__grid--two">
                <label class="proto-field enterprise-form__span-2">
                  <span class="proto-field__label">登录账号</span>
                  <input
                    v-model.trim="form.username"
                    class="proto-input__control proto-input__control--plain"
                    type="text"
                    placeholder="请输入企业登录账号"
                    autocomplete="username"
                    required
                  />
                </label>

                <label class="proto-field">
                  <span class="proto-field__label">设置登录密码</span>
                  <input
                    v-model.trim="form.password"
                    class="proto-input__control proto-input__control--plain"
                    type="password"
                    placeholder="包含字母、数字与符号"
                    autocomplete="new-password"
                    required
                  />
                </label>

                <label class="proto-field">
                  <span class="proto-field__label">确认登录密码</span>
                  <input
                    v-model.trim="form.confirmPassword"
                    class="proto-input__control proto-input__control--plain"
                    type="password"
                    placeholder="请再次输入以确认"
                    autocomplete="new-password"
                    required
                  />
                </label>
              </div>

              <div class="enterprise-upload">
                <span class="material-symbols-outlined enterprise-upload__icon">upload_file</span>
                <strong>上传营业执照彩色扫描件</strong>
                <span>支持 PDF, JPG, PNG 格式 (最大 10MB)</span>
                <input ref="licenseInputRef" class="enterprise-upload__input" type="file" @change="handleFileChange" />
                <button class="enterprise-upload__button" type="button" @click="openFilePicker">选择文件</button>
                <em>{{ selectedFileLabel }}</em>
              </div>
            </section>

            <label class="proto-checkbox proto-checkbox--stacked">
              <input v-model="form.agreed" type="checkbox" />
              <span>
                我已阅读并同意
                <a href="#">《食品安全数字监管平台企业服务协议》</a>
                以及
                <a href="#">《隐私保护政策》</a>
                。我承诺所填信息真实有效，并承担相应的法律责任。
              </span>
            </label>

            <AuthStatusAlert :message="status.message" :type="status.type" />

            <button class="proto-primary-button" type="submit" :disabled="loading">
              <span>{{ loading ? "提交中..." : "提交注册申请" }}</span>
              <span class="material-symbols-outlined">arrow_forward</span>
            </button>

            <p class="enterprise-proto__login-link">
              已有企业账号？<RouterLink class="proto-link" :to="{ name: 'login' }">立即登录</RouterLink>
            </p>
          </form>
        </section>
      </div>
    </section>
  </AuthShell>
</template>

<script setup>
import { computed, reactive, ref } from "vue";
import { useRouter } from "vue-router";
import { registerEnterprise } from "../../api/auth";
import AuthShell from "../../components/auth/AuthShell.vue";
import AuthStatusAlert from "../../components/auth/AuthStatusAlert.vue";

const router = useRouter();
const form = reactive({
  enterpriseName: "",
  creditCode: "",
  enterpriseType: "MANUFACTURING",
  principalName: "",
  phone: "",
  email: "",
  username: "",
  password: "",
  confirmPassword: "",
  agreed: false
});

const status = reactive({
  message: "",
  type: "info"
});

const loading = ref(false);
const licenseInputRef = ref(null);
const selectedFileName = ref("");

const selectedFileLabel = computed(() => selectedFileName.value || "尚未选择文件");

function setStatus(message, type = "info") {
  status.message = message;
  status.type = type;
}

function isValidPhone(phone) {
  return /^1\d{10}$/.test(phone);
}

function openFilePicker() {
  licenseInputRef.value?.click();
}

function handleFileChange(event) {
  const file = event.target.files?.[0] || null;
  selectedFileName.value = file ? file.name : "";
  if (file) {
    setStatus("已选择营业执照文件，但当前阶段不会上传到后端。", "info");
  }
}

async function handleSubmit() {
  if (!isValidPhone(form.phone)) {
    setStatus("请输入 11 位手机号。", "warning");
    return;
  }

  if (form.password.length < 8) {
    setStatus("密码长度至少为 8 位。", "warning");
    return;
  }

  if (form.password !== form.confirmPassword) {
    setStatus("两次输入的密码不一致。", "warning");
    return;
  }

  if (!form.agreed) {
    setStatus("请先阅读并同意企业服务协议。", "warning");
    return;
  }

  const payload = {
    username: form.username,
    password: form.password,
    realName: form.principalName,
    phone: form.phone
  };

  loading.value = true;
  setStatus("");

  try {
    await registerEnterprise(payload);
    await router.replace({
      name: "login",
      query: {
        registered: "enterprise"
      }
    });
  } catch (error) {
    setStatus(error.message || "企业注册失败，请稍后重试。", "error");
  } finally {
    loading.value = false;
  }
}
</script>
