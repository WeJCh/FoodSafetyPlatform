<template>
  <SystemAdminWorkspacePage
    active-key="list"
    :username="adminUser.username"
    search-placeholder="搜索监管人员或辖区..."
    @navigate="handleSidebarNavigate"
    @logout="handleLogout"
  >
    <section class="sys-admin-detail-page">
      <header class="sys-admin-detail-header">
        <div class="sys-admin-detail-header__left">
          <button type="button" class="sys-admin-back-btn" @click="goBack">
            <span class="material-symbols-outlined">arrow_back</span>
          </button>
          <div>
            <h1>监管人员详情</h1>
            <p>查看监管人员基础档案、当前辖区、账户状态和真实操作日志。</p>
          </div>
        </div>
        <div class="sys-admin-detail-header__actions">
          <button type="button" class="sys-admin-action-btn" :disabled="loading || !profile" @click="goRegionAdjust">
            <span class="material-symbols-outlined">map</span>
            <span>调整辖区</span>
          </button>
          <button
            type="button"
            class="sys-admin-action-btn"
            :class="Number(profile?.status) === 1 ? 'is-danger' : 'is-success'"
            :disabled="loading || !profile"
            @click="goStatusConfirm"
          >
            <span class="material-symbols-outlined">{{ Number(profile?.status) === 1 ? "block" : "check_circle" }}</span>
            <span>{{ Number(profile?.status) === 1 ? "停用账号" : "启用账号" }}</span>
          </button>
          <button type="button" class="sys-admin-action-btn is-primary" :disabled="loading || !profile" @click="goEdit">
            <span class="material-symbols-outlined">edit</span>
            <span>编辑信息</span>
          </button>
        </div>
      </header>

      <div class="sys-admin-detail-grid">
        <aside class="sys-admin-detail-left">
          <section class="sys-admin-identity-card">
            <div class="sys-admin-identity-card__status">
              <span class="sys-admin-dot"></span>
              <span>{{ Number(profile?.status) === 1 ? "已启用" : "已停用" }}</span>
            </div>
            <div class="sys-admin-identity-card__main">
              <div class="sys-admin-portrait">
                <span>{{ getAvatarText(profile?.name) }}</span>
              </div>
              <h2>{{ profile?.name || "未命名监管人员" }}</h2>
              <p class="sys-admin-subtitle">{{ formatRoleType(profile?.roleType) }}</p>
            </div>
            <div class="sys-admin-identity-card__meta">
              <div class="sys-admin-meta-block">
                <div class="sys-admin-meta-label">账号标识</div>
                <div class="sys-admin-meta-value is-mono">
                  {{ profile?.userId ? `REGULATOR-${profile.userId}` : "-" }}
                </div>
              </div>
              <div class="sys-admin-kv">
                <span>用户 ID</span>
                <strong>{{ profile?.userId ?? "-" }}</strong>
              </div>
              <div class="sys-admin-kv">
                <span>档案 ID</span>
                <strong>{{ profile?.id ?? "-" }}</strong>
              </div>
            </div>
          </section>

          <section class="sys-admin-audit-card">
            <h3>操作日志</h3>
            <ol class="sys-admin-timeline">
              <li v-if="auditLoading">
                <div class="sys-admin-timeline__dot"></div>
                <div class="sys-admin-timeline__body">
                  <div class="sys-admin-timeline__title">日志加载中</div>
                  <div class="sys-admin-timeline__desc">正在读取该监管人员的真实操作记录。</div>
                </div>
              </li>
              <li v-else-if="auditError">
                <div class="sys-admin-timeline__dot"></div>
                <div class="sys-admin-timeline__body">
                  <div class="sys-admin-timeline__title">日志加载失败</div>
                  <div class="sys-admin-timeline__desc">{{ auditError }}</div>
                </div>
              </li>
              <li v-else-if="!auditLogs.length">
                <div class="sys-admin-timeline__dot"></div>
                <div class="sys-admin-timeline__body">
                  <div class="sys-admin-timeline__title">暂无操作日志</div>
                  <div class="sys-admin-timeline__desc">当前只展示后端真实返回的日志数据。</div>
                </div>
              </li>
              <li v-for="item in auditLogs" :key="item.id">
                <div class="sys-admin-timeline__dot" :class="{ 'is-active': item.active }"></div>
                <div class="sys-admin-timeline__body">
                  <div class="sys-admin-timeline__title">{{ item.title }}</div>
                  <div class="sys-admin-timeline__desc">{{ item.desc }}</div>
                  <div v-if="item.remark" class="sys-admin-timeline__desc is-remark">备注：{{ item.remark }}</div>
                  <div class="sys-admin-timeline__desc is-operator">操作人：{{ item.operatorName || "系统管理员" }}</div>
                  <div class="sys-admin-timeline__time">{{ item.time }}</div>
                </div>
              </li>
            </ol>
          </section>
        </aside>

        <main class="sys-admin-detail-right">
          <section class="sys-admin-panel">
            <header class="sys-admin-panel__head is-primary">
              <h3>基础档案</h3>
              <span class="material-symbols-outlined">badge</span>
            </header>
            <div class="sys-admin-panel__body">
              <div class="sys-admin-info-grid">
                <div class="sys-admin-info-item">
                  <div class="sys-admin-info-label">姓名</div>
                  <div class="sys-admin-info-value">{{ profile?.name || "-" }}</div>
                </div>
                <div class="sys-admin-info-item">
                  <div class="sys-admin-info-label">联系电话</div>
                  <div class="sys-admin-info-value">{{ formatPhone(profile?.phone) }}</div>
                </div>
                <div class="sys-admin-info-item">
                  <div class="sys-admin-info-label">角色</div>
                  <div class="sys-admin-info-value">{{ formatRoleType(profile?.roleType) }}</div>
                </div>
                <div class="sys-admin-info-item">
                  <div class="sys-admin-info-label">账号状态</div>
                  <div class="sys-admin-info-value">{{ Number(profile?.status) === 1 ? "已启用" : "已停用" }}</div>
                </div>
              </div>
            </div>
          </section>

          <section class="sys-admin-role-grid">
            <article class="sys-admin-role-card is-primary">
              <h4>角色编码</h4>
              <div class="sys-admin-role-card__row">
                <div class="sys-admin-role-icon is-primary">
                  <span class="material-symbols-outlined">policy</span>
                </div>
                <div>
                  <div class="sys-admin-role-code">{{ profile?.roleType || "-" }}</div>
                  <div class="sys-admin-role-desc">角色编码决定当前监管人员的权限范围和业务职责。</div>
                </div>
              </div>
            </article>
            <article class="sys-admin-role-card is-secondary">
              <h4>职责说明</h4>
              <div class="sys-admin-role-card__row">
                <div class="sys-admin-role-icon is-secondary">
                  <span class="material-symbols-outlined">shield_person</span>
                </div>
                <div>
                  <div class="sys-admin-role-code">{{ formatRoleType(profile?.roleType) }}</div>
                  <div class="sys-admin-role-desc">{{ getRoleDescription(profile?.roleType) }}</div>
                </div>
              </div>
            </article>
          </section>

          <section class="sys-admin-jurisdiction">
            <header class="sys-admin-jurisdiction__head">
              <span class="material-symbols-outlined">map</span>
              <h3>辖区信息</h3>
            </header>
            <div class="sys-admin-jurisdiction__body">
              <div class="sys-admin-jurisdiction__meta">
                <div class="sys-admin-juri-item">
                  <div class="sys-admin-juri-label">辖区路径</div>
                  <div class="sys-admin-juri-value">{{ regionText || "-" }}</div>
                </div>
                <div class="sys-admin-juri-item">
                  <div class="sys-admin-juri-label">辖区 ID</div>
                  <div class="sys-admin-juri-value is-mono">{{ primaryRegionId ?? "-" }}</div>
                </div>
              </div>
              <div class="sys-admin-jurisdiction__map">
                <div class="sys-admin-map-overlay">
                  <div class="sys-admin-map-chip">当前辖区范围</div>
                </div>
              </div>
            </div>
          </section>
        </main>
      </div>

      <div v-if="status.message" class="sys-admin-status" :class="status.type">{{ status.message }}</div>
    </section>
  </SystemAdminWorkspacePage>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from "vue";
import { useRoute, useRouter } from "vue-router";
import { fetchRegulatorAuditLogs, fetchRegulatorProfileByUserId, fetchRegionPath } from "../../api/regulation";
import { resolveErrorMessage } from "../../utils/uiFeedback";
import SystemAdminWorkspacePage from "../../components/systemAdmin/SystemAdminWorkspacePage.vue";
import { useSystemAdminShellSession } from "./systemAdminShared";

const route = useRoute();
const router = useRouter();
const { adminUser, token, handleSidebarNavigate, handleLogout } = useSystemAdminShellSession();

const loading = ref(false);
const auditLoading = ref(false);
const auditError = ref("");
const status = reactive({ message: "", type: "" });
const profile = ref(null);
const regionText = ref("");
const auditLogs = ref([]);

const userId = computed(() => Number(route.params.userId || 0) || 0);
const primaryRegionId = computed(() => {
  const ids = profile.value?.regionIds;
  const first = Array.isArray(ids) ? Number(ids[0] || 0) : 0;
  return first || null;
});

function setStatus(message, type = "info") {
  status.message = message;
  status.type = type;
}

function goBack() {
  router.push({ name: "admin-regulator-list" });
}

function goEdit() {
  if (!userId.value) {
    setStatus("缺少监管人员 userId", "error");
    return;
  }
  router.push({ name: "admin-regulator-edit", params: { userId: userId.value } });
}

function goRegionAdjust() {
  if (!userId.value) {
    setStatus("缺少监管人员 userId", "error");
    return;
  }
  router.push({ name: "admin-regulator-region-adjust", params: { userId: userId.value } });
}

function goStatusConfirm() {
  if (!profile.value?.userId) {
    setStatus("缺少监管人员 userId", "error");
    return;
  }
  const targetStatus = Number(profile.value.status) === 1 ? 0 : 1;
  router.push({
    name: "admin-regulator-status-confirm",
    params: { userId: profile.value.userId },
    query: { targetStatus: String(targetStatus), from: "detail" }
  });
}

function formatRoleType(roleType) {
  if (roleType === "REGULATOR_ADMIN") return "监管管理员";
  if (roleType === "REGULATOR_ENFORCER") return "监管执法人员";
  return roleType ? String(roleType) : "未知角色";
}

function getRoleDescription(roleType) {
  if (roleType === "REGULATOR_ADMIN") return "负责区县级统筹协调、任务分派与监管人员调度。";
  if (roleType === "REGULATOR_ENFORCER") return "负责现场检查、执法处置与证据采集等工作。";
  return "暂无职责说明。";
}

function getAvatarText(name) {
  const text = String(name || "").trim();
  if (!text) return "监管";
  return text.slice(0, 2);
}

function formatPhone(phone) {
  const raw = String(phone || "").replace(/\D/g, "");
  if (!raw) return "-";
  if (raw.length !== 11) return raw;
  return `${raw.slice(0, 3)}-${raw.slice(3, 7)}-${raw.slice(7)}`;
}

function formatDateTime(value) {
  if (!value) return "-";
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return String(value);
  return date.toLocaleString("zh-CN", { hour12: false });
}

async function resolveRegionTextById(regionId) {
  if (!regionId) return "";
  try {
    const pathList = await fetchRegionPath(token.value, regionId);
    if (!Array.isArray(pathList) || !pathList.length) return `辖区 ID：${regionId}`;
    return pathList.map((item) => item.name).join(" / ");
  } catch {
    return `辖区 ID：${regionId}`;
  }
}

async function loadAuditLogs(regulatorId) {
  const id = Number(regulatorId || 0);
  if (!id) {
    auditLogs.value = [];
    auditError.value = "";
    return;
  }

  auditLoading.value = true;
  auditError.value = "";
  try {
    const data = await fetchRegulatorAuditLogs(token.value, id, 8);
    auditLogs.value = (Array.isArray(data) ? data : []).map((item, index) => ({
      id: item.id || `audit-${index}`,
      title: item.actionName || item.actionType || "操作记录",
      desc: item.summary || "暂无详细说明",
      remark: item.remark || "",
      operatorName: item.operatorName || "",
      time: formatDateTime(item.createTime),
      active: index === 0
    }));
  } catch (error) {
    auditLogs.value = [];
    auditError.value = resolveErrorMessage(error, "操作日志加载失败");
  } finally {
    auditLoading.value = false;
  }
}

async function loadDetail() {
  if (!userId.value) {
    setStatus("缺少监管人员 userId", "error");
    return;
  }

  loading.value = true;
  setStatus("");
  try {
    const data = await fetchRegulatorProfileByUserId(token.value, userId.value);
    profile.value = data;
    regionText.value = await resolveRegionTextById(primaryRegionId.value);
    await loadAuditLogs(data?.id);
  } catch (error) {
    setStatus(resolveErrorMessage(error, "加载监管人员详情失败"), "error");
  } finally {
    loading.value = false;
  }
}

watch(
  () => route.fullPath,
  () => {
    loadDetail();
  }
);

onMounted(() => {
  loadDetail();
});
</script>

<style scoped>
.sys-admin-detail-page {
  display: grid;
  gap: 14px;
}

.sys-admin-detail-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.sys-admin-detail-header__left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.sys-admin-back-btn {
  width: 40px;
  height: 40px;
  border: 0;
  border-radius: 2px;
  background: #e6e8eb;
  color: #002660;
  display: grid;
  place-items: center;
  cursor: pointer;
}

.sys-admin-detail-header__left h1 {
  margin: 0;
  color: #002660;
  font-size: 30px;
  font-weight: 900;
}

.sys-admin-detail-header__left p {
  margin: 4px 0 0;
  color: #64748b;
  font-size: 13px;
}

.sys-admin-detail-header__actions {
  display: inline-flex;
  gap: 8px;
  flex-wrap: wrap;
}

.sys-admin-action-btn {
  border: 0;
  border-radius: 2px;
  padding: 10px 14px;
  font-size: 12px;
  font-weight: 800;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  cursor: pointer;
}

.sys-admin-action-btn.is-primary { background: #002660; color: #fff; }
.sys-admin-action-btn.is-danger { background: #ba1a1a; color: #fff; }
.sys-admin-action-btn.is-success { background: #166534; color: #fff; }
.sys-admin-action-btn:not(.is-primary):not(.is-danger):not(.is-success) { background: #e6e8eb; color: #0f172a; }
.sys-admin-action-btn:disabled { opacity: 0.6; cursor: default; }

.sys-admin-detail-grid {
  display: grid;
  grid-template-columns: minmax(280px, 360px) minmax(0, 1fr);
  gap: 14px;
  align-items: start;
}

.sys-admin-detail-left,
.sys-admin-detail-right {
  display: grid;
  gap: 14px;
}

.sys-admin-identity-card,
.sys-admin-audit-card,
.sys-admin-panel,
.sys-admin-role-card,
.sys-admin-jurisdiction {
  background: #fff;
  border: 1px solid rgba(226, 232, 240, 0.8);
  border-radius: 2px;
  box-shadow: 0 1px 2px rgba(15, 23, 42, 0.05);
}

.sys-admin-identity-card {
  padding: 18px;
  display: grid;
  gap: 16px;
}

.sys-admin-identity-card__status {
  justify-self: start;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  border-radius: 999px;
  background: rgba(0, 38, 96, 0.08);
  color: #002660;
  padding: 6px 10px;
  font-size: 11px;
  font-weight: 800;
}

.sys-admin-dot {
  width: 8px;
  height: 8px;
  border-radius: 999px;
  background: currentColor;
}

.sys-admin-identity-card__main {
  display: grid;
  justify-items: start;
  gap: 8px;
}

.sys-admin-portrait {
  width: 68px;
  height: 68px;
  border-radius: 2px;
  background: linear-gradient(135deg, #002660, #003a8c);
  color: #fff;
  display: grid;
  place-items: center;
  font-size: 24px;
  font-weight: 900;
}

.sys-admin-identity-card__main h2 {
  margin: 0;
  font-size: 24px;
  color: #191c1e;
  font-weight: 900;
}

.sys-admin-subtitle {
  margin: 0;
  color: #64748b;
  font-size: 13px;
}

.sys-admin-identity-card__meta {
  display: grid;
  gap: 10px;
}

.sys-admin-meta-block {
  padding: 12px;
  background: #f2f4f7;
  border-radius: 2px;
}

.sys-admin-meta-label {
  color: #64748b;
  font-size: 10px;
  letter-spacing: 0.12em;
  text-transform: uppercase;
  font-weight: 800;
}

.sys-admin-meta-value {
  margin-top: 6px;
  color: #0f172a;
  font-size: 14px;
  font-weight: 900;
}

.sys-admin-kv {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 10px;
  padding-bottom: 10px;
  border-bottom: 1px solid #e6e8eb;
}

.sys-admin-kv:last-child { border-bottom: 0; padding-bottom: 0; }
.sys-admin-kv span { color: #64748b; font-size: 12px; }
.sys-admin-kv strong { color: #191c1e; font-size: 13px; }
.is-mono { font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, "Liberation Mono", "Courier New", monospace; }

.sys-admin-audit-card {
  padding: 18px;
}

.sys-admin-audit-card h3 {
  margin: 0 0 12px;
  color: #64748b;
  font-size: 11px;
  font-weight: 900;
  letter-spacing: 0.14em;
  text-transform: uppercase;
}

.sys-admin-timeline {
  margin: 0;
  padding: 0;
  list-style: none;
  display: grid;
  gap: 12px;
}

.sys-admin-timeline li {
  display: grid;
  grid-template-columns: 14px minmax(0, 1fr);
  gap: 10px;
}

.sys-admin-timeline__dot {
  width: 10px;
  height: 10px;
  border-radius: 999px;
  background: #cbd5e1;
  margin-top: 6px;
}

.sys-admin-timeline__dot.is-active { background: #002660; }
.sys-admin-timeline__title { color: #0f172a; font-size: 13px; font-weight: 800; }
.sys-admin-timeline__desc { margin-top: 3px; color: #64748b; font-size: 12px; line-height: 1.5; }
.sys-admin-timeline__desc.is-remark,
.sys-admin-timeline__desc.is-operator { color: #475569; }
.sys-admin-timeline__time { margin-top: 4px; color: #94a3b8; font-size: 11px; }

.sys-admin-panel__head,
.sys-admin-jurisdiction__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  padding: 14px 16px;
  border-bottom: 1px solid #e6e8eb;
}

.sys-admin-panel__head.is-primary {
  color: #002660;
}

.sys-admin-panel__head h3,
.sys-admin-jurisdiction__head h3 {
  margin: 0;
  font-size: 14px;
  font-weight: 900;
}

.sys-admin-panel__body {
  padding: 16px;
}

.sys-admin-info-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.sys-admin-info-item {
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 2px;
  padding: 12px;
}

.sys-admin-info-label {
  color: #64748b;
  font-size: 10px;
  letter-spacing: 0.12em;
  text-transform: uppercase;
  font-weight: 800;
}

.sys-admin-info-value {
  margin-top: 8px;
  color: #191c1e;
  font-size: 14px;
  font-weight: 800;
}

.sys-admin-role-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
}

.sys-admin-role-card {
  padding: 16px;
}

.sys-admin-role-card h4 {
  margin: 0 0 12px;
  font-size: 12px;
  font-weight: 900;
  color: #64748b;
  letter-spacing: 0.12em;
  text-transform: uppercase;
}

.sys-admin-role-card__row {
  display: grid;
  grid-template-columns: 44px minmax(0, 1fr);
  gap: 12px;
  align-items: start;
}

.sys-admin-role-icon {
  width: 44px;
  height: 44px;
  border-radius: 2px;
  display: grid;
  place-items: center;
}

.sys-admin-role-icon.is-primary { background: rgba(0, 38, 96, 0.08); color: #002660; }
.sys-admin-role-icon.is-secondary { background: rgba(148, 163, 184, 0.15); color: #475569; }
.sys-admin-role-code { color: #0f172a; font-size: 14px; font-weight: 900; }
.sys-admin-role-desc { margin-top: 6px; color: #64748b; font-size: 12px; line-height: 1.55; }

.sys-admin-jurisdiction__body {
  padding: 16px;
  display: grid;
  grid-template-columns: minmax(0, 1fr) 240px;
  gap: 14px;
}

.sys-admin-jurisdiction__meta {
  display: grid;
  gap: 12px;
}

.sys-admin-juri-item {
  padding: 12px;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 2px;
}

.sys-admin-juri-label {
  color: #64748b;
  font-size: 10px;
  letter-spacing: 0.12em;
  text-transform: uppercase;
  font-weight: 800;
}

.sys-admin-juri-value {
  margin-top: 8px;
  color: #191c1e;
  font-size: 13px;
  font-weight: 800;
  line-height: 1.6;
}

.sys-admin-jurisdiction__map {
  min-height: 180px;
  border-radius: 2px;
  background:
    linear-gradient(rgba(255, 255, 255, 0.1), rgba(255, 255, 255, 0.1)),
    linear-gradient(135deg, #002660, #0b4ba7);
  position: relative;
  overflow: hidden;
}

.sys-admin-map-overlay {
  position: absolute;
  inset: 0;
  display: grid;
  place-items: center;
}

.sys-admin-map-chip {
  background: rgba(255, 255, 255, 0.14);
  border: 1px solid rgba(255, 255, 255, 0.22);
  color: #fff;
  padding: 8px 12px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 800;
}

.sys-admin-status {
  position: fixed;
  right: 18px;
  bottom: 18px;
  color: #fff;
  border-radius: 8px;
  padding: 10px 12px;
  font-size: 12px;
  background: #0f172a;
  z-index: 60;
}

.sys-admin-status.error { background: #b91c1c; }
.sys-admin-status.success { background: #166534; }

@media (max-width: 1100px) {
  .sys-admin-detail-grid,
  .sys-admin-role-grid,
  .sys-admin-jurisdiction__body {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 900px) {
  .sys-admin-detail-header {
    align-items: start;
    flex-direction: column;
  }

  .sys-admin-info-grid {
    grid-template-columns: 1fr;
  }
}
</style>
