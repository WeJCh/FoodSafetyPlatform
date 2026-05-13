<template>
  <RegulatorEnforcerWorkspacePage
    active-key=""
    :username="enforcerUser.username || currentUser.username || ''"
    search-placeholder="搜索企业、任务或待办事项"
    @navigate="handleSidebarNavigate"
    @logout="handleLogout"
  >
    <section class="account-layout">
      <div class="account-layout__main">
        <section class="account-card account-card--hero">
          <div class="hero-profile">
            <div class="hero-profile__avatar">
              <span class="material-symbols-outlined">local_police</span>
            </div>
            <div class="hero-profile__body">
              <div class="hero-profile__headline">
                <h1>{{ displayName }}</h1>
                <span class="hero-badge">{{ roleLabel }}</span>
                <span class="hero-badge hero-badge--status" :class="`is-${profileStatusTone}`">
                  账号状态：{{ profileStatusLabel }}
                </span>
              </div>
              <div class="hero-profile__meta">
                <span>
                  <span class="material-symbols-outlined">schedule</span>
                  建档时间：{{ profileCreatedAt }}
                </span>
                <span>
                  <span class="material-symbols-outlined">person</span>
                  {{ currentUser.username || enforcerUser.username || "-" }}
                </span>
              </div>
            </div>
          </div>
        </section>

        <section class="account-card">
          <div class="section-head">
            <div class="section-head__title">
              <span class="material-symbols-outlined">person</span>
              <div>
                <h2>基本信息</h2>
                <p>姓名和手机号支持自助维护，登录账号、状态和时间信息保持只读。</p>
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
                <strong>{{ currentUser.realName || profile.name || "-" }}</strong>
              </article>
              <article class="field-card">
                <span>建档时间</span>
                <strong>{{ profileCreatedAt }}</strong>
              </article>
              <article class="field-card">
                <span>手机号</span>
                <strong>{{ currentUser.phone || profile.phone || "-" }}</strong>
              </article>
              <article class="field-card">
                <span>登录账号</span>
                <strong>{{ currentUser.username || enforcerUser.username || "-" }}</strong>
              </article>
              <article class="field-card">
                <span>账号状态</span>
                <strong>{{ profileStatusLabel }}</strong>
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
                <span>建档时间</span>
                <input :value="profileCreatedAt" type="text" readonly />
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
                <input :value="currentUser.username || enforcerUser.username || '-'" type="text" readonly />
              </label>
              <label class="field">
                <span>账号状态</span>
                <input :value="profileStatusLabel" type="text" readonly />
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

        <section class="account-card">
          <div class="section-head">
            <div class="section-head__title">
              <span class="material-symbols-outlined">lock</span>
              <div>
                <h2>安全与密码</h2>
                <p>密码修改独立处理，提交成功后会退出当前登录。</p>
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
                <li>建议包含大写字母、小写字母和数字。</li>
                <li>不要与近期使用的密码重复。</li>
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

      <aside class="account-layout__side">
        <section class="account-card side-card">
          <div class="section-head section-head--stack">
            <div class="section-head__title">
              <span class="material-symbols-outlined">map</span>
              <div>
                <h2>管辖范围</h2>
              </div>
            </div>
          </div>
          <div class="side-fields">
            <article class="side-field">
              <span>省份</span>
              <strong>{{ regionDisplay.province }}</strong>
            </article>
            <article class="side-field">
              <span>城市</span>
              <strong>{{ regionDisplay.city }}</strong>
            </article>
            <article class="side-field">
              <span>区县</span>
              <strong>{{ regionDisplay.district }}</strong>
            </article>
            <article class="side-field">
              <span>街道</span>
              <strong>{{ regionDisplay.street }}</strong>
            </article>
          </div>
        </section>

        <section class="account-card side-card">
          <div class="section-head section-head--stack">
            <div class="section-head__title">
              <span class="material-symbols-outlined">history</span>
              <div>
                <h2>系统活动日志</h2>
              </div>
            </div>
          </div>
          <div class="log-list">
            <article class="log-item">
              <span>上次登录时间</span>
              <strong>{{ accountUpdatedAt }}</strong>
            </article>
            <article class="log-item">
              <span>登录设备与环境</span>
              <strong>{{ deviceSummary }}</strong>
            </article>
            <article class="log-item">
              <span>会话标识 (Session ID)</span>
              <strong class="is-mono">{{ sessionLabel }}</strong>
            </article>
          </div>
        </section>
      </aside>
    </section>
  </RegulatorEnforcerWorkspacePage>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from "vue";
import { changeCurrentUserPassword, fetchCurrentUser, updateCurrentUser } from "../../api/auth";
import { fetchRegionPath, fetchRegulatorProfile, updateMyRegulatorProfile } from "../../api/regulation";
import RegulatorEnforcerWorkspacePage from "../../components/regulatorEnforcer/RegulatorEnforcerWorkspacePage.vue";
import { formatTime } from "../../utils/formatters";
import { resolveErrorMessage } from "../../utils/uiFeedback";
import { useRegulatorEnforcerShellSession } from "./regulatorEnforcerShared";

const { enforcerUser, token, handleSidebarNavigate, handleLogout } = useRegulatorEnforcerShellSession();

const loading = ref(false);
const savingBasic = ref(false);
const savingPassword = ref(false);
const editingBasic = ref(false);

const currentUser = reactive({
  username: "",
  realName: "",
  phone: "",
  status: 1,
  updateTime: ""
});

const profile = reactive({
  id: null,
  name: "",
  phone: "",
  status: 1,
  regionIds: [],
  updateTime: "",
  createTime: ""
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
const regionPath = ref([]);
const deviceSummary = ref("-");

const roleLabel = "执法人员";
const displayName = computed(() => currentUser.realName || profile.name || enforcerUser.value.username || "监管员");
const profileStatusLabel = computed(() => (Number(profile.status) === 1 ? "活跃正常" : "已停用"));
const profileStatusTone = computed(() => (Number(profile.status) === 1 ? "success" : "danger"));
const profileCreatedAt = computed(() => formatTime(profile.createTime));
const accountUpdatedAt = computed(() => formatTime(currentUser.updateTime || ""));
const sessionLabel = computed(() => {
  const raw = token.value || "";
  if (!raw) return "-";
  return raw.length > 26 ? `${raw.slice(0, 10)}...${raw.slice(-10)}` : raw;
});
const regionDisplay = computed(() => ({
  province: regionPath.value[0]?.name || "-",
  city: regionPath.value[1]?.name || "-",
  district: regionPath.value[2]?.name || "-",
  street: regionPath.value[3]?.name || "-"
}));

function fillCurrentUser(data) {
  currentUser.username = data?.username || enforcerUser.value.username || "";
  currentUser.realName = data?.realName || "";
  currentUser.phone = data?.phone || "";
  currentUser.status = data?.status ?? 1;
  currentUser.updateTime = data?.updateTime || "";
}

function fillProfile(data) {
  profile.id = data?.id ?? null;
  profile.name = data?.name || "";
  profile.phone = data?.phone || "";
  profile.status = data?.status ?? 1;
  profile.regionIds = Array.isArray(data?.regionIds) ? data.regionIds : [];
  profile.updateTime = data?.updateTime || "";
  profile.createTime = data?.createTime || "";
}

function syncBasicForm() {
  basicForm.realName = currentUser.realName || profile.name || "";
  basicForm.phone = currentUser.phone || profile.phone || "";
  basicSnapshot.realName = basicForm.realName;
  basicSnapshot.phone = basicForm.phone;
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

async function loadProfile() {
  const profileData = await fetchRegulatorProfile(token.value);
  fillProfile(profileData || {});
  const regionId = Number(profile.regionIds[0] || 0);
  regionPath.value = regionId ? await fetchRegionPath(token.value, regionId).catch(() => []) : [];
}

async function loadPageData() {
  loading.value = true;
  basicMessage.message = "";
  passwordMessage.message = "";
  try {
    const user = await fetchCurrentUser(token.value);
    fillCurrentUser(user || {});
    await loadProfile();
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
    const userData = await updateCurrentUser(token.value, payload);
    fillCurrentUser(userData || {});
    const profileData = await updateMyRegulatorProfile(token.value, {
      name: payload.realName,
      phone: payload.phone
    });
    fillProfile(profileData || {});
    syncBasicForm();
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
    await changeCurrentUserPassword(token.value, {
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
    passwordMessage.message = resolveErrorMessage(error, "修改密码失败");
    passwordMessage.type = "danger";
  } finally {
    savingPassword.value = false;
  }
}

function resolveDeviceSummary() {
  if (typeof navigator === "undefined") {
    deviceSummary.value = "-";
    return;
  }
  const ua = navigator.userAgent || "";
  let browser = "Unknown Browser";
  if (ua.includes("Edg/")) browser = "Edge";
  else if (ua.includes("Chrome/")) browser = "Chrome";
  else if (ua.includes("Firefox/")) browser = "Firefox";
  else if (ua.includes("Safari/")) browser = "Safari";
  deviceSummary.value = `${navigator.platform || "Unknown OS"} / ${browser}`;
}

onMounted(() => {
  resolveDeviceSummary();
  loadPageData();
});
</script>

<style scoped>
.account-layout {
  display: grid;
  grid-template-columns: minmax(0, 2fr) minmax(360px, 1fr);
  gap: 20px;
  align-items: start;
}

.account-layout__main,
.account-layout__side {
  display: grid;
  gap: 20px;
}

.account-card {
  border: 1px solid #e4e9f1;
  border-radius: 0;
  background: #ffffff;
  box-shadow: 0 10px 26px rgba(15, 23, 42, 0.05);
  overflow: hidden;
}

.account-card--hero {
  padding: 14px 18px;
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
  border: 1px solid #d9e1ed;
  background: linear-gradient(180deg, #f6f8fb 0%, #eef3f8 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  color: #15396f;
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
  color: #0f172a;
  font-size: 34px;
  line-height: 1.1;
}

.hero-badge {
  display: inline-flex;
  align-items: center;
  min-height: 24px;
  padding: 0 8px;
  border: 1px solid #cbd8eb;
  background: #f1f5fb;
  color: #214a86;
  font-size: 11px;
  font-weight: 700;
}

.hero-badge--status.is-success {
  border-color: #c8e6d3;
  background: #eefbf3;
  color: #0f8a4b;
}

.hero-badge--status.is-danger {
  border-color: #f2c7c4;
  background: #fff1f1;
  color: #b42318;
}

.hero-profile__meta {
  display: flex;
  flex-wrap: wrap;
  gap: 12px 18px;
  margin-top: 10px;
  color: #475569;
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

.section-head--stack {
  padding-bottom: 12px;
}

.section-head__title {
  display: flex;
  align-items: flex-start;
  gap: 10px;
}

.section-head__title .material-symbols-outlined {
  margin-top: 1px;
  font-size: 18px;
  color: #15396f;
}

.section-head__title h2 {
  margin: 0;
  color: #0f172a;
  font-size: 22px;
  line-height: 1.2;
}

.section-head__title p {
  margin: 8px 0 0;
  color: #64748b;
  font-size: 13px;
  line-height: 1.6;
}

.section-head__actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.info-panel,
.security-panel,
.side-fields,
.log-list {
  margin: 16px 18px 18px;
  padding: 18px;
  background: #f4f6f9;
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
.field span,
.side-field span,
.log-item span {
  color: #5b6472;
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0;
}

.field-card strong {
  min-height: 44px;
  padding: 12px 14px;
  border: 1px solid #e0e5ec;
  background: #e9edf2;
  color: #0f172a;
  font-size: 15px;
  line-height: 1.35;
}

.field input {
  min-height: 44px;
  padding: 0 14px;
  border: 1px solid #d7dee8;
  background: #ffffff;
  color: #0f172a;
  font-size: 14px;
}

.field input[readonly] {
  background: #e9edf2;
  color: #475569;
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
  color: #0f172a;
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

.security-submit-btn {
  min-width: 84px;
  min-height: 34px;
  padding: 0 14px;
  border: 1px solid #cfd7e3;
  border-radius: 0;
  background: #ffffff;
  color: #0f172a;
  font-size: 13px;
  font-weight: 700;
  cursor: pointer;
}

.security-submit-btn:disabled {
  cursor: not-allowed;
  opacity: 0.7;
}

.side-field,
.log-item {
  display: grid;
  gap: 8px;
}

.side-field + .side-field,
.log-item + .log-item {
  margin-top: 14px;
}

.side-field strong,
.log-item strong {
  min-height: 42px;
  padding: 11px 12px;
  border: 1px solid #e0e5ec;
  background: #ffffff;
  color: #0f172a;
  font-size: 14px;
  line-height: 1.4;
}

.is-mono {
  font-family: Consolas, "Courier New", monospace;
  word-break: break-all;
}

.ghost-btn,
.primary-btn {
  min-width: 108px;
  min-height: 38px;
  padding: 0 14px;
  border: 1px solid #cad4e0;
  background: #ffffff;
  color: #334155;
  font-size: 13px;
  font-weight: 700;
  cursor: pointer;
}

.primary-btn {
  border-color: #0f3c7a;
  background: #0f3c7a;
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
  .account-layout,
  .security-panel {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 760px) {
  .hero-profile,
  .field-grid,
  .section-head {
    grid-template-columns: 1fr;
    flex-direction: column;
  }

  .hero-profile__headline h1 {
    font-size: 28px;
  }

  .info-panel,
  .security-panel,
  .side-fields,
  .log-list {
    padding: 14px;
  }

  .field-grid {
    grid-template-columns: 1fr;
  }
}
</style>
