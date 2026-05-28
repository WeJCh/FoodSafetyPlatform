<template>
    <PublicWorkspacePage
    page-class="public-account-page"
    active-key="account"
  >
    <main class="public-account-page__main">
      <section class="account-layout">
        <div class="account-layout__main">
          <section class="account-card account-card--hero">
            <div class="hero-profile">
              <div class="hero-profile__avatar">
                <span class="material-symbols-outlined">account_circle</span>
              </div>
              <div class="hero-profile__body">
                <div class="hero-profile__headline">
                  <h1>{{ displayName }}</h1>
                  <span class="hero-badge">{{ roleLabel }}</span>
                  <span class="hero-badge hero-badge--status" :class="`is-${userStatusTone}`">
                    账号状态：{{ userStatusLabel }}
                  </span>
                </div>
                <div class="hero-profile__meta">
                  <span>
                    <span class="material-symbols-outlined">person</span>
                    {{ currentUser.username || publicUser.username || "-" }}
                  </span>
                  <span>
                    <span class="material-symbols-outlined">schedule</span>
                    最近更新时间：{{ accountUpdatedAt }}
                  </span>
                </div>
              </div>
            </div>
          </section>

          <div class="account-layout__overview">
            <section class="account-card">
              <div class="section-head">
                <div class="section-head__title">
                  <span class="material-symbols-outlined">person</span>
                  <div>
                    <h2>基本信息</h2>
                    <p>这里仅维护当前公众账号的基础资料。</p>
                  </div>
                </div>
                <div class="section-head__actions">
                  <button
                    v-if="!editingBasic"
                    type="button"
                    class="ghost-btn"
                    :disabled="loading"
                    @click="startBasicEdit"
                  >
                    编辑资料
                  </button>
                  <template v-else>
                    <button type="button" class="ghost-btn" :disabled="savingBasic" @click="cancelBasicEdit">取消</button>
                    <button type="button" class="primary-btn" :disabled="savingBasic || loading" @click="handleBasicSave">
                      {{ savingBasic ? "保存中..." : "保存基本信息" }}
                    </button>
                  </template>
                </div>
              </div>

              <div class="info-panel">
                <div v-if="!editingBasic" class="field-grid">
                  <article class="field-card">
                    <span>真实姓名</span>
                    <strong>{{ currentUser.realName || "-" }}</strong>
                  </article>
                  <article class="field-card">
                    <span>账号类型</span>
                    <strong>{{ roleLabel }}</strong>
                  </article>
                  <article class="field-card">
                    <span>手机号</span>
                    <strong>{{ currentUser.phone || "-" }}</strong>
                  </article>
                  <article class="field-card">
                    <span>登录账号</span>
                    <strong>{{ currentUser.username || publicUser.username || "-" }}</strong>
                  </article>
                  <article class="field-card">
                    <span>账号状态</span>
                    <strong>{{ userStatusLabel }}</strong>
                  </article>
                  <article class="field-card">
                    <span>最近更新时间</span>
                    <strong>{{ accountUpdatedAt }}</strong>
                  </article>
                </div>

                <div v-else class="field-grid">
                  <label class="field">
                    <span>真实姓名</span>
                    <input v-model="basicForm.realName" type="text" :disabled="savingBasic" />
                  </label>
                  <label class="field">
                    <span>账号类型</span>
                    <input :value="roleLabel" type="text" readonly />
                  </label>
                  <label class="field">
                    <span>手机号</span>
                    <input
                      v-model="basicForm.phone"
                      type="tel"
                      inputmode="numeric"
                      maxlength="11"
                      :disabled="savingBasic"
                    />
                  </label>
                  <label class="field">
                    <span>登录账号</span>
                    <input :value="currentUser.username || publicUser.username || '-'" type="text" readonly />
                  </label>
                  <label class="field">
                    <span>账号状态</span>
                    <input :value="userStatusLabel" type="text" readonly />
                  </label>
                  <label class="field">
                    <span>最近更新时间</span>
                    <input :value="accountUpdatedAt" type="text" readonly />
                  </label>
                </div>
              </div>

              <div v-if="basicMessage.message" class="inline-feedback" :class="`is-${basicMessage.type}`">
                {{ basicMessage.message }}
              </div>
            </section>

            <section class="account-card account-card--log-side">
              <div class="section-head">
                <div class="section-head__title">
                  <span class="material-symbols-outlined">history</span>
                  <div>
                    <h2>账户变更记录</h2>
                    <p>展示当前账号最近的开通、资料与密码变更记录。</p>
                  </div>
                </div>
              </div>

              <div class="log-list">
                <article v-if="loadingLogs" class="log-record">
                  <div class="log-record__head">
                    <strong>正在加载账户变更记录...</strong>
                  </div>
                </article>
                <article v-for="item in accountLogs" :key="item.id || item.createTime || item.actionType" class="log-record">
                  <div class="log-record__head">
                    <strong>{{ resolveAuditLogTitle(item) }}</strong>
                    <span>{{ formatTime(item.createTime) || "-" }}</span>
                  </div>
                  <p v-if="resolveAuditLogDetail(item)" class="log-record__detail">
                    {{ resolveAuditLogDetail(item) }}
                  </p>
                  <div class="log-record__meta">
                    <span>操作人</span>
                    <strong>{{ item.operatorName || "系统" }}</strong>
                  </div>
                </article>
                <article v-if="!loadingLogs && !accountLogs.length" class="log-record">
                  <div class="log-record__head">
                    <strong>当前暂无账户变更记录。</strong>
                  </div>
                </article>
              </div>
            </section>

          <section class="account-card">
            <div class="section-head">
              <div class="section-head__title">
                <span class="material-symbols-outlined">lock</span>
                <div>
                  <h2>安全与密码</h2>
                  <p>密码修改成功后会退出当前登录，请重新进入公众门户。</p>
                </div>
              </div>
            </div>

            <div class="security-panel">
              <div class="security-panel__form">
                <label class="field security-field">
                  <span>当前密码</span>
                  <input v-model="passwordForm.oldPassword" type="password" autocomplete="current-password" :disabled="savingPassword" />
                </label>
                <label class="field security-field">
                  <span>新密码</span>
                  <input v-model="passwordForm.newPassword" type="password" autocomplete="new-password" :disabled="savingPassword" />
                </label>
                <label class="field security-field">
                  <span>确认新密码</span>
                  <input
                    v-model="passwordForm.confirmPassword"
                    type="password"
                    autocomplete="new-password"
                    :disabled="savingPassword"
                  />
                </label>
              </div>

              <aside class="security-panel__tips">
                <h3>密码安全要求</h3>
                <ul>
                  <li>必须包含至少 8 个字符。</li>
                  <li>建议包含大小写字母和数字。</li>
                  <li>不要与近期使用过的密码相同。</li>
                  <li>建议定期更新密码。</li>
                </ul>
              </aside>
            </div>

            <div v-if="passwordMessage.message" class="inline-feedback" :class="`is-${passwordMessage.type}`">
              {{ passwordMessage.message }}
            </div>

            <div class="section-foot">
              <button type="button" class="security-submit-btn" :disabled="savingPassword" @click="handlePasswordSave">
                {{ savingPassword ? "更新中..." : "更新密码" }}
              </button>
            </div>
          </section>
          </div>

        </div>
      </section>
    </main>
    </PublicWorkspacePage>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from "vue";
import { useRouter } from "vue-router";
import PublicWorkspacePage from "../../components/public/PublicWorkspacePage.vue";
import { changeCurrentUserPassword, fetchCurrentUser, fetchCurrentUserAuditLogs, updateCurrentUser } from "../../api/auth";
import { getActiveSession, performLogout } from "../../session/authRuntime";
import { formatTime } from "../../utils/formatters";
import { resolveErrorMessage } from "../../utils/uiFeedback";

const router = useRouter();
const publicUser = computed(() => getActiveSession() || {});
const publicToken = computed(() => getActiveSession()?.token || "");

const loading = ref(false);
const loadingLogs = ref(false);
const savingBasic = ref(false);
const savingPassword = ref(false);
const editingBasic = ref(false);
const accountLogs = ref([]);

const currentUser = reactive({
  username: "",
  realName: "",
  phone: "",
  status: 1,
  updateTime: ""
});

const basicForm = reactive({
  realName: "",
  phone: ""
});

const passwordForm = reactive({
  oldPassword: "",
  newPassword: "",
  confirmPassword: ""
});

const basicSnapshot = reactive({
  realName: "",
  phone: ""
});

const basicMessage = reactive({ message: "", type: "neutral" });
const passwordMessage = reactive({ message: "", type: "neutral" });

const roleLabel = "公众账号";
const displayName = computed(() => currentUser.realName || publicUser.value.username || "公众用户");
const userStatusLabel = computed(() => (Number(currentUser.status) === 1 ? "已启用" : "已停用"));
const userStatusTone = computed(() => (Number(currentUser.status) === 1 ? "success" : "danger"));
const accountUpdatedAt = computed(() => formatTime(currentUser.updateTime || ""));

function fillCurrentUser(data) {
  currentUser.username = data?.username || publicUser.value.username || "";
  currentUser.realName = data?.realName || "";
  currentUser.phone = data?.phone || "";
  currentUser.status = data?.status ?? 1;
  currentUser.updateTime = data?.updateTime || "";
}

function syncBasicForm() {
  basicForm.realName = currentUser.realName || "";
  basicForm.phone = currentUser.phone || "";
  basicSnapshot.realName = basicForm.realName;
  basicSnapshot.phone = basicForm.phone;
}

function resolveAuditLogTitle(item) {
  return item?.actionName || item?.summary || item?.remark || "账户变更";
}

function resolveAuditLogDetail(item) {
  const detail = String(item?.summary || item?.remark || "").trim();
  const title = String(resolveAuditLogTitle(item) || "").trim();
  return detail && detail !== title ? detail : "";
}

function startBasicEdit() {
  editingBasic.value = true;
  basicMessage.message = "";
}

function cancelBasicEdit() {
  editingBasic.value = false;
  basicForm.realName = basicSnapshot.realName;
  basicForm.phone = basicSnapshot.phone;
  basicMessage.message = "";
}

async function handleLogout() {
  await performLogout();
  router.replace({ name: "login" }).catch(() => {});
}

async function loadAccountLogs() {
  loadingLogs.value = true;
  try {
    const logData = await fetchCurrentUserAuditLogs(publicToken.value, 6);
    accountLogs.value = Array.isArray(logData) ? logData : [];
  } catch (error) {
    accountLogs.value = [];
  } finally {
    loadingLogs.value = false;
  }
}

async function loadPageData() {
  loading.value = true;
  basicMessage.message = "";
  passwordMessage.message = "";
  try {
    const [userData] = await Promise.all([
      fetchCurrentUser(publicToken.value),
      loadAccountLogs()
    ]);
    fillCurrentUser(userData || {});
    syncBasicForm();
  } catch (error) {
    basicMessage.message = resolveErrorMessage(error, "加载个人信息失败");
    basicMessage.type = "danger";
  } finally {
    loading.value = false;
  }
}

async function handleBasicSave() {
  savingBasic.value = true;
  basicMessage.message = "";
  try {
    const payload = {
      realName: String(basicForm.realName || "").trim(),
      phone: String(basicForm.phone || "").trim()
    };
    const userData = await updateCurrentUser(publicToken.value, payload);
    fillCurrentUser(userData || {});
    syncBasicForm();
    await loadAccountLogs();
    editingBasic.value = false;
    basicMessage.message = "基本信息已更新。";
    basicMessage.type = "success";
  } catch (error) {
    basicMessage.message = resolveErrorMessage(error, "保存基本信息失败");
    basicMessage.type = "danger";
  } finally {
    savingBasic.value = false;
  }
}

async function handlePasswordSave() {
  passwordMessage.message = "";
  if (!passwordForm.oldPassword || !passwordForm.newPassword || !passwordForm.confirmPassword) {
    passwordMessage.message = "请完整填写密码信息。";
    passwordMessage.type = "danger";
    return;
  }
  if (passwordForm.newPassword.length < 8) {
    passwordMessage.message = "新密码长度不能少于 8 位。";
    passwordMessage.type = "danger";
    return;
  }
  if (passwordForm.newPassword !== passwordForm.confirmPassword) {
    passwordMessage.message = "两次输入的新密码不一致。";
    passwordMessage.type = "danger";
    return;
  }

  savingPassword.value = true;
  try {
    await changeCurrentUserPassword(publicToken.value, {
      oldPassword: passwordForm.oldPassword,
      newPassword: passwordForm.newPassword
    });
    passwordMessage.message = "密码已更新，正在退出当前登录。";
    passwordMessage.type = "success";
    passwordForm.oldPassword = "";
    passwordForm.newPassword = "";
    passwordForm.confirmPassword = "";
    await handleLogout();
  } catch (error) {
    passwordMessage.message = resolveErrorMessage(error, "更新密码失败");
    passwordMessage.type = "danger";
  } finally {
    savingPassword.value = false;
  }
}

onMounted(() => {
  loadPageData();
});
</script>

<style scoped>
.public-account-page {
  min-height: 100vh;
  background: var(--surface);
}

.public-account-page__topbar {
  position: sticky;
  top: 0;
  z-index: 40;
  border-bottom: 1px solid rgba(195, 198, 211, 0.4);
  background: rgba(248, 250, 253, 0.84);
}

.public-account-page__topbar-inner {
  max-width: 1680px;
  margin: 0 auto;
  min-height: var(--public-topbar-min-h);
  padding: 0 16px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 24px;
}

.public-account-page__brand-nav {
  display: flex;
  align-items: center;
  gap: 24px;
}

.public-account-page__brand {
  font-family: var(--font-display);
  font-size: var(--public-brand-size);
  font-weight: 800;
  color: var(--primary);
}

.public-account-page__nav {
  display: flex;
  gap: 18px;
}

.public-account-page__nav-item {
  border: none;
  background: transparent;
  min-height: var(--public-topbar-min-h);
  color: var(--on-surface-variant);
  font-size: var(--public-nav-size);
  font-weight: 700;
  border-bottom: 2px solid transparent;
  cursor: pointer;
}

.public-account-page__toolbar {
  display: flex;
  align-items: center;
  gap: 10px;
}

.public-account-page__account-btn {
  min-width: var(--public-toolbar-min-h);
  min-height: var(--public-toolbar-min-h);
  padding: 0 12px;
}

.public-account-page__account-btn.is-active {
  color: var(--primary);
  border-color: rgba(70, 89, 231, 0.25);
  background: rgba(70, 89, 231, 0.08);
}

.public-account-page__logout {
  min-height: var(--public-toolbar-min-h);
  font-size: var(--public-logout-font-size);
  margin: 0;
}

.public-account-page__main {
  max-width: 1120px;
  margin: 0 auto;
  padding: 24px 16px 48px;
}

.account-layout {
  display: flex;
  justify-content: center;
}

.account-layout__main {
  width: 100%;
  display: grid;
  gap: 20px;
}

.account-layout__overview {
  display: grid;
  grid-template-columns: minmax(0, 1.85fr) minmax(320px, 0.95fr);
  gap: 20px;
  align-items: start;
}

.account-layout__overview > :first-child {
  grid-column: 1 / -1;
}

.account-layout__overview > :nth-child(2) {
  grid-column: 2;
  grid-row: 2;
}

.account-layout__overview > :nth-child(3) {
  grid-column: 1;
  grid-row: 2;
}

.account-card {
  border: 1px solid rgba(195, 198, 211, 0.34);
  border-radius: 10px;
  background: var(--surface-container-lowest);
  box-shadow: 0 12px 28px rgba(15, 23, 42, 0.05);
  overflow: hidden;
}

.account-card--hero {
  padding: 14px 18px;
}

.account-card--log-side {
  min-height: 100%;
}

.hero-profile {
  display: grid;
  grid-template-columns: 84px minmax(0, 1fr);
  gap: 16px;
  align-items: center;
}

.hero-profile__avatar {
  width: 84px;
  height: 84px;
  border: 1px solid rgba(195, 198, 211, 0.4);
  background: linear-gradient(180deg, #f6f8fb 0%, #eef3f8 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--primary);
}

.hero-profile__avatar .material-symbols-outlined {
  font-size: 38px;
}

.hero-profile__headline {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
}

.hero-profile__headline h1 {
  margin: 0;
  color: var(--on-surface);
  font-size: 34px;
  line-height: 1.1;
}

.hero-badge {
  display: inline-flex;
  align-items: center;
  min-height: 24px;
  padding: 0 8px;
  border: 1px solid rgba(70, 89, 231, 0.2);
  background: rgba(70, 89, 231, 0.08);
  color: var(--primary);
  font-size: 11px;
  font-weight: 700;
}

.hero-badge--status.is-success {
  border-color: rgba(33, 156, 84, 0.3);
  background: rgba(33, 156, 84, 0.08);
  color: #177245;
}

.hero-badge--status.is-danger {
  border-color: rgba(180, 35, 24, 0.25);
  background: rgba(180, 35, 24, 0.08);
  color: #b42318;
}

.hero-profile__meta {
  display: flex;
  flex-wrap: wrap;
  gap: 12px 18px;
  margin-top: 10px;
  color: var(--on-surface-variant);
  font-size: 13px;
}

.hero-profile__meta span {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.hero-profile__meta .material-symbols-outlined {
  font-size: 15px;
}

.section-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  padding: 18px 18px 0;
}

.section-head__title {
  display: flex;
  align-items: flex-start;
  gap: 10px;
}

.section-head__title .material-symbols-outlined {
  margin-top: 1px;
  font-size: 18px;
  color: var(--primary);
}

.section-head__title h2 {
  margin: 0;
  color: var(--on-surface);
  font-size: 22px;
  line-height: 1.2;
}

.section-head__title p {
  margin: 8px 0 0;
  color: var(--on-surface-variant);
  font-size: 13px;
  line-height: 1.6;
}

.section-head__actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.info-panel,
.log-list {
  margin: 16px 18px 18px;
  padding: 18px;
  background: var(--surface-container-low);
}

.field-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px 18px;
}

.field-card,
.field {
  display: grid;
  gap: 8px;
}

.field-card span,
.field span {
  color: #5b6472;
  font-size: 11px;
  font-weight: 700;
}

.field-card strong {
  min-height: 44px;
  padding: 12px 14px;
  border: 1px solid rgba(195, 198, 211, 0.5);
  background: #e9edf2;
  color: var(--on-surface);
  font-size: 15px;
  line-height: 1.35;
}

.field input {
  min-height: 44px;
  padding: 0 14px;
  border: 1px solid rgba(195, 198, 211, 0.6);
  border-radius: 0;
  background: #ffffff;
  color: var(--on-surface);
  font-size: 14px;
}

.field input[readonly] {
  background: #e9edf2;
  color: var(--on-surface-variant);
}

.security-panel {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 168px;
  align-items: start;
  gap: 14px;
  margin: 0 18px;
  padding: 16px 14px 14px;
  background: #f4f7fb;
}

.security-panel__form {
  display: grid;
  gap: 12px;
}

.security-field {
  gap: 6px;
}

.security-field span {
  color: #1e293b;
  font-size: 11px;
  font-weight: 700;
  line-height: 1.5;
}

.security-field input {
  min-height: 40px;
  padding: 0 12px;
  border: 1px solid #d8e0ea;
  border-radius: 0;
  background: #dfe5eb;
  color: var(--on-surface);
  font-size: 14px;
}

.security-field input:focus {
  outline: none;
  border-color: #9fb2ca;
  background: #e6ebf1;
}

.security-panel__tips {
  padding: 12px 14px;
  border: 1px solid #d9e2ec;
  background: #eef3f8;
}

.security-panel__tips h3 {
  margin: 0;
  color: #0f2b57;
  font-size: 12px;
  font-weight: 700;
}

.security-panel__tips ul {
  margin: 8px 0 0;
  padding-left: 16px;
  color: #334155;
  font-size: 11px;
  line-height: 1.7;
}

.section-foot {
  display: flex;
  justify-content: flex-start;
  padding: 14px 18px 18px;
}

.log-record {
  display: grid;
  gap: 8px;
  padding: 12px;
  border: 1px solid rgba(195, 198, 211, 0.5);
  background: #ffffff;
}

.log-record + .log-record {
  margin-top: 14px;
}

.log-record__head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.log-record__head strong {
  color: var(--on-surface);
  font-size: 14px;
  line-height: 1.5;
}

.log-record__head span {
  color: var(--on-surface-variant);
  font-size: 11px;
  font-weight: 700;
  white-space: nowrap;
}

.log-record__detail {
  margin: 0;
  color: #334155;
  font-size: 13px;
  line-height: 1.6;
}

.log-record__meta {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  color: var(--on-surface-variant);
  font-size: 11px;
  font-weight: 700;
}

.log-record__meta strong {
  color: #334155;
  font-size: 11px;
  font-weight: 700;
}

.security-submit-btn,
.ghost-btn,
.primary-btn {
  min-height: 34px;
  border-radius: 0;
  cursor: pointer;
}

.security-submit-btn {
  min-width: 84px;
  padding: 0 14px;
  border: 1px solid #cfd7e3;
  background: #ffffff;
  color: var(--on-surface);
  font-size: 13px;
  font-weight: 700;
}

.security-submit-btn:disabled {
  cursor: not-allowed;
  opacity: 0.7;
}

.ghost-btn,
.primary-btn {
  min-width: 108px;
  padding: 0 14px;
  border: 1px solid #cad4e0;
  background: #ffffff;
  color: #334155;
  font-size: 13px;
  font-weight: 700;
}

.primary-btn {
  border-color: var(--primary);
  background: var(--primary);
  color: #ffffff;
}

.inline-feedback {
  margin: 0 18px 18px;
  padding: 12px 14px;
  font-size: 13px;
}

.inline-feedback.is-success {
  background: #eefbf3;
  color: #117a43;
}

.inline-feedback.is-danger {
  background: #fff1f1;
  color: #b42318;
}

@media (max-width: 1180px) {
  .account-layout__overview,
  .security-panel {
    grid-template-columns: 1fr;
  }

  .account-layout__overview > :first-child,
  .account-layout__overview > :nth-child(2),
  .account-layout__overview > :nth-child(3) {
    grid-column: auto;
    grid-row: auto;
  }
}

@media (max-width: 1100px) {
  .public-account-page__nav {
    display: none;
  }
}

@media (max-width: 760px) {
  .public-account-page__toolbar {
    display: none;
  }

  .hero-profile,
  .section-head {
    grid-template-columns: 1fr;
    flex-direction: column;
  }

  .hero-profile__headline h1 {
    font-size: 28px;
  }

  .field-grid {
    grid-template-columns: 1fr;
  }

  .info-panel,
  .security-panel,
  .log-list {
    padding: 14px;
  }

  .log-record__head {
    flex-direction: column;
  }
}
</style>




