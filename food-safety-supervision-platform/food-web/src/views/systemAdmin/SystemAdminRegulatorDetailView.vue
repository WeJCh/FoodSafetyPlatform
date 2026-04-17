<template>
  <SystemAdminWorkspacePage
    active-key="list"
    :username="adminUser.username"
    search-placeholder="全局搜索人员或辖区..."
    @navigate="handleSidebarNavigate"
    @logout="handleLogout"
    @pending-feature="onPendingFeature"
  >
    <section class="sys-admin-detail-page">
      <header class="sys-admin-detail-header">
        <div class="sys-admin-detail-header__left">
          <button type="button" class="sys-admin-back-btn" @click="goBack">
            <span class="material-symbols-outlined">arrow_back</span>
          </button>
          <div>
            <h1>监管人员详情</h1>
            <p>查看与管理系统监管人员核心档案与访问权限</p>
          </div>
        </div>
        <div class="sys-admin-detail-header__actions">
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
          <button
            type="button"
            class="sys-admin-action-btn is-primary"
            :disabled="loading || !profile"
            @click="goEdit"
          >
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
              <span>{{ Number(profile?.status) === 1 ? "在岗" : "停用" }}</span>
            </div>
            <div class="sys-admin-identity-card__main">
              <div class="sys-admin-portrait">
                <span>{{ getAvatarText(profile?.name) }}</span>
              </div>
              <h2>{{ profile?.name || "未命名监管员" }}</h2>
              <p class="sys-admin-subtitle">{{ formatRoleType(profile?.roleType) }}</p>
            </div>
            <div class="sys-admin-identity-card__meta">
              <div class="sys-admin-meta-block">
                <div class="sys-admin-meta-label">账号标识</div>
                <div class="sys-admin-meta-value is-mono">
                  {{ profile?.userId ? `REGULATOR-${profile.userId}` : "—" }}
                </div>
              </div>
              <div class="sys-admin-kv">
                <span>用户ID</span>
                <strong>{{ profile?.userId ?? "—" }}</strong>
              </div>
              <div class="sys-admin-kv">
                <span>监管档案ID</span>
                <strong>{{ profile?.id ?? "—" }}</strong>
              </div>
            </div>
          </section>

          <section class="sys-admin-audit-card">
            <h3>合规操作日志</h3>
            <ol class="sys-admin-timeline">
              <li v-for="item in auditLogs" :key="item.id">
                <div class="sys-admin-timeline__dot" :class="{ 'is-active': item.active }"></div>
                <div class="sys-admin-timeline__body">
                  <div class="sys-admin-timeline__title">{{ item.title }}</div>
                  <div class="sys-admin-timeline__desc">{{ item.desc }}</div>
                  <div class="sys-admin-timeline__time">{{ item.time }}</div>
                </div>
              </li>
            </ol>
            <button type="button" class="sys-admin-link-btn" @click="onPendingFeature('查看完整审计日志')">
              查看完整日志
              <span class="material-symbols-outlined">arrow_forward</span>
            </button>
          </section>
        </aside>

        <main class="sys-admin-detail-right">
          <section class="sys-admin-panel">
            <header class="sys-admin-panel__head is-primary">
              <h3>基本档案与联系方式</h3>
              <span class="material-symbols-outlined">badge</span>
            </header>
            <div class="sys-admin-panel__body">
              <div class="sys-admin-info-grid">
                <div class="sys-admin-info-item">
                  <div class="sys-admin-info-label">全名</div>
                  <div class="sys-admin-info-value">{{ profile?.name || "—" }}</div>
                </div>
                <div class="sys-admin-info-item">
                  <div class="sys-admin-info-label">联系电话</div>
                  <div class="sys-admin-info-value">{{ formatPhone(profile?.phone) }}</div>
                </div>
                <div class="sys-admin-info-item">
                  <div class="sys-admin-info-label">系统角色</div>
                  <div class="sys-admin-info-value">{{ formatRoleType(profile?.roleType) }}</div>
                </div>
                <div class="sys-admin-info-item">
                  <div class="sys-admin-info-label">状态</div>
                  <div class="sys-admin-info-value">
                    {{ Number(profile?.status) === 1 ? "在岗" : "停用" }}
                  </div>
                </div>
              </div>
            </div>
          </section>

          <section class="sys-admin-role-grid">
            <article class="sys-admin-role-card is-primary">
              <h4>系统分配角色</h4>
              <div class="sys-admin-role-card__row">
                <div class="sys-admin-role-icon is-primary">
                  <span class="material-symbols-outlined">policy</span>
                </div>
                <div>
                  <div class="sys-admin-role-code">{{ profile?.roleType || "—" }}</div>
                  <div class="sys-admin-role-desc">角色用于决定可访问模块与数据范围</div>
                </div>
              </div>
            </article>
            <article class="sys-admin-role-card is-secondary">
              <h4>权限级别</h4>
              <div class="sys-admin-role-card__row">
                <div class="sys-admin-role-icon is-secondary">
                  <span class="material-symbols-outlined">shield_person</span>
                </div>
                <div>
                  <div class="sys-admin-role-code">TIER 1</div>
                  <div class="sys-admin-role-desc">TODO: 后端未提供权限等级字段，先保留 UI 占位</div>
                </div>
              </div>
            </article>
          </section>

          <section class="sys-admin-jurisdiction">
            <header class="sys-admin-jurisdiction__head">
              <span class="material-symbols-outlined">map</span>
              <h3>管辖辖区与分层系统</h3>
            </header>
            <div class="sys-admin-jurisdiction__body">
              <div class="sys-admin-jurisdiction__meta">
                <div class="sys-admin-juri-item">
                  <div class="sys-admin-juri-label">辖区路径</div>
                  <div class="sys-admin-juri-value">
                    {{ regionText || "辖区信息待完善" }}
                  </div>
                </div>
                <div class="sys-admin-juri-item">
                  <div class="sys-admin-juri-label">辖区ID</div>
                  <div class="sys-admin-juri-value is-mono">
                    {{ primaryRegionId ?? "—" }}
                  </div>
                </div>
              </div>
              <div class="sys-admin-jurisdiction__map" @click="onPendingFeature('查看交互式地图')">
                <div class="sys-admin-map-overlay">
                  <div class="sys-admin-map-chip">查看交互式地图</div>
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
import { computed, onMounted, reactive, ref } from "vue";
import { useRoute, useRouter } from "vue-router";
import {
  fetchRegulatorProfileByUserId,
  fetchRegionPath
} from "../../api/regulation";
import SystemAdminWorkspacePage from "../../components/systemAdmin/SystemAdminWorkspacePage.vue";
import { systemAdminFeaturePendingNotice, useSystemAdminShellSession } from "./systemAdminShared";

const route = useRoute();
const router = useRouter();
const { adminUser, token, handleSidebarNavigate, handleLogout } = useSystemAdminShellSession();

const loading = ref(false);
const status = reactive({ message: "", type: "" });
const profile = ref(null);
const regionText = ref("");

const auditLogs = ref([
  {
    id: "a1",
    title: "账号状态变更",
    desc: "管理员将监管账号设置为启用状态",
    time: "2小时前",
    active: true
  },
  {
    id: "a2",
    title: "账号状态变更",
    desc: "管理员将监管账号设置为停用状态",
    time: "3天前",
    active: false
  }
  // TODO: 接入系统审计日志 API（按人员 + 时间分页）
]);

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

function onPendingFeature(title) {
  systemAdminFeaturePendingNotice(title);
}

function goBack() {
  if (window.history.length > 1) router.back();
  else router.push({ name: "admin-regulator-list" });
}

function goEdit() {
  if (!userId.value) {
    setStatus("缺少监管人员 userId 参数", "error");
    return;
  }
  router.push({ name: "admin-regulator-edit", params: { userId: userId.value } });
}

function formatRoleType(roleType) {
  if (roleType === "REGULATOR_ADMIN") return "区域管理员";
  if (roleType === "REGULATOR_ENFORCER") return "执法人员";
  return roleType ? String(roleType) : "未知角色";
}

function getAvatarText(name) {
  const text = String(name || "").trim();
  if (!text) return "监管";
  return text.slice(-2);
}

function formatPhone(phone) {
  const raw = String(phone || "").replace(/\D/g, "");
  if (!raw) return "—";
  if (raw.length !== 11) return raw;
  return `${raw.slice(0, 3)}-${raw.slice(3, 7)}-${raw.slice(7)}`;
}

async function resolveRegionTextById(regionId) {
  if (!regionId) return "";
  try {
    const pathList = await fetchRegionPath(token.value, regionId);
    if (!Array.isArray(pathList) || !pathList.length) return `辖区ID: ${regionId}`;
    return pathList.map((item) => item.name).join(" / ");
  } catch {
    return `辖区ID: ${regionId}`;
  }
}

async function loadDetail() {
  if (!userId.value) {
    setStatus("缺少监管人员 userId 参数", "error");
    return;
  }

  loading.value = true;
  setStatus("");
  try {
    const data = await fetchRegulatorProfileByUserId(token.value, userId.value);
    profile.value = data;
    regionText.value = await resolveRegionTextById(primaryRegionId.value);
  } catch (error) {
    setStatus(error.message || "加载监管人员详情失败", "error");
  } finally {
    loading.value = false;
  }
}

function goStatusConfirm() {
  if (!profile.value?.userId) {
    setStatus("缺少监管人员 userId 参数", "error");
    return;
  }
  const targetStatus = Number(profile.value.status) === 1 ? 0 : 1;
  router.push({
    name: "admin-regulator-status-confirm",
    params: { userId: profile.value.userId },
    query: { targetStatus: String(targetStatus), from: "detail" }
  });
}

onMounted(() => {
  loadDetail();
});
</script>

<style scoped>
.sys-admin-detail-page { display: grid; gap: 16px; }
.sys-admin-detail-header { display: flex; align-items: end; justify-content: space-between; gap: 12px; }
.sys-admin-detail-header__left { display: flex; align-items: center; gap: 12px; }
.sys-admin-back-btn { width: 36px; height: 36px; border-radius: 2px; border: 0; background: #e6e8eb; color: #0f172a; display: inline-flex; align-items: center; justify-content: center; cursor: pointer; }
.sys-admin-back-btn:hover { filter: brightness(0.98); }
.sys-admin-detail-header h1 { margin: 0; color: #002660; font-size: 26px; font-weight: 900; letter-spacing: -0.01em; }
.sys-admin-detail-header p { margin: 6px 0 0; color: #64748b; font-size: 13px; }
.sys-admin-detail-header__actions { display: flex; gap: 10px; flex-wrap: wrap; justify-content: flex-end; }
.sys-admin-action-btn { border: 0; border-radius: 2px; padding: 10px 14px; font-size: 12px; font-weight: 800; cursor: pointer; display: inline-flex; align-items: center; gap: 6px; }
.sys-admin-action-btn:disabled { opacity: 0.6; cursor: default; }
.sys-admin-action-btn.is-primary { background: #002660; color: #fff; }
.sys-admin-action-btn.is-danger { background: #fee2e2; color: #7f1d1d; }
.sys-admin-action-btn.is-success { background: #dcfce7; color: #166534; }
.sys-admin-action-btn.is-primary:hover { background: #003a8c; }
.sys-admin-action-btn.is-danger:hover { filter: brightness(0.98); }
.sys-admin-action-btn.is-success:hover { filter: brightness(0.98); }

.sys-admin-detail-grid { display: grid; grid-template-columns: minmax(0, 4fr) minmax(0, 8fr); gap: 12px; align-items: start; }
.sys-admin-detail-left { display: grid; gap: 12px; min-width: 0; }
.sys-admin-detail-right { display: grid; gap: 12px; min-width: 0; }

.sys-admin-identity-card { position: relative; background: #fff; border: 1px solid rgba(226, 232, 240, 0.6); box-shadow: 0 1px 2px rgba(15, 23, 42, 0.05); border-radius: 2px; padding: 16px; overflow: hidden; }
.sys-admin-identity-card__status { position: absolute; top: 12px; right: 12px; display: inline-flex; align-items: center; gap: 6px; padding: 4px 10px; border-radius: 2px; background: rgba(0, 38, 96, 0.08); color: #002660; font-size: 10px; font-weight: 900; letter-spacing: 0.08em; text-transform: uppercase; }
.sys-admin-dot { width: 8px; height: 8px; border-radius: 999px; background: #002660; }
.sys-admin-identity-card__main { display: grid; justify-items: center; text-align: center; gap: 6px; margin-top: 10px; }
.sys-admin-portrait { width: 76px; height: 76px; border-radius: 999px; border: 3px solid #f1f5f9; background: radial-gradient(circle at 20% 20%, rgba(201, 215, 254, 0.8), rgba(0, 58, 140, 0.12)); display: grid; place-items: center; box-shadow: 0 12px 28px rgba(0, 38, 96, 0.12); }
.sys-admin-portrait span { font-weight: 900; color: #002660; letter-spacing: -0.02em; }
.sys-admin-identity-card h2 { margin: 2px 0 0; color: #002660; font-size: 18px; font-weight: 900; }
.sys-admin-subtitle { margin: 0; color: #64748b; font-size: 12px; font-weight: 700; }
.sys-admin-identity-card__meta { margin-top: 14px; display: grid; gap: 10px; }
.sys-admin-meta-block { background: #f8fafc; border: 1px solid rgba(226, 232, 240, 0.7); border-radius: 2px; padding: 10px 12px; }
.sys-admin-meta-label { font-size: 10px; font-weight: 900; letter-spacing: 0.12em; text-transform: uppercase; color: #64748b; }
.sys-admin-meta-value { margin-top: 6px; font-weight: 900; color: #002660; }
.is-mono { font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, "Liberation Mono", "Courier New", monospace; }
.sys-admin-kv { display: flex; justify-content: space-between; align-items: center; padding: 0 2px; color: #0f172a; }
.sys-admin-kv span { color: #64748b; font-size: 12px; }
.sys-admin-kv strong { font-size: 12px; font-weight: 800; }

.sys-admin-audit-card { background: #fff; border: 1px solid rgba(226, 232, 240, 0.6); box-shadow: 0 1px 2px rgba(15, 23, 42, 0.05); border-radius: 2px; padding: 16px; }
.sys-admin-audit-card h3 { margin: 0 0 12px; font-size: 11px; font-weight: 900; color: #64748b; letter-spacing: 0.14em; text-transform: uppercase; }
.sys-admin-timeline { margin: 0; padding: 0 0 0 18px; list-style: none; display: grid; gap: 12px; position: relative; }
.sys-admin-timeline::before { content: ""; position: absolute; left: 4px; top: 6px; bottom: 6px; width: 2px; background: rgba(148, 163, 184, 0.35); }
.sys-admin-timeline li { position: relative; display: grid; grid-template-columns: 14px 1fr; gap: 10px; }
.sys-admin-timeline__dot { width: 8px; height: 8px; border-radius: 999px; background: rgba(148, 163, 184, 0.55); margin-top: 4px; box-shadow: 0 0 0 4px #fff; }
.sys-admin-timeline__dot.is-active { background: #002660; }
.sys-admin-timeline__title { font-size: 12px; font-weight: 900; color: #0f172a; }
.sys-admin-timeline__desc { margin-top: 2px; font-size: 11px; color: #64748b; line-height: 1.45; }
.sys-admin-timeline__time { margin-top: 4px; font-size: 10px; color: #94a3b8; font-style: italic; }
.sys-admin-link-btn { margin-top: 12px; border: 0; background: transparent; color: #002660; font-weight: 900; font-size: 12px; display: inline-flex; align-items: center; gap: 6px; cursor: pointer; }
.sys-admin-link-btn .material-symbols-outlined { font-size: 18px; }

.sys-admin-panel { background: #fff; border: 1px solid rgba(226, 232, 240, 0.6); box-shadow: 0 1px 2px rgba(15, 23, 42, 0.05); border-radius: 2px; overflow: hidden; }
.sys-admin-panel__head { padding: 12px 14px; display: flex; align-items: center; justify-content: space-between; }
.sys-admin-panel__head h3 { margin: 0; font-size: 13px; font-weight: 900; letter-spacing: 0.02em; }
.sys-admin-panel__head.is-primary { background: #002660; color: #fff; }
.sys-admin-panel__head .material-symbols-outlined { font-size: 18px; opacity: 0.95; }
.sys-admin-panel__body { padding: 16px 16px; }
.sys-admin-info-grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 14px 16px; }
.sys-admin-info-label { font-size: 10px; font-weight: 900; letter-spacing: 0.12em; text-transform: uppercase; color: #64748b; }
.sys-admin-info-value { margin-top: 6px; padding-bottom: 6px; border-bottom: 1px solid rgba(226, 232, 240, 0.9); font-size: 16px; font-weight: 800; color: #0f172a; }

.sys-admin-role-grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 12px; }
.sys-admin-role-card { background: #fff; border: 1px solid rgba(226, 232, 240, 0.6); box-shadow: 0 1px 2px rgba(15, 23, 42, 0.05); border-radius: 2px; padding: 14px 14px; }
.sys-admin-role-card.is-primary { border-left: 4px solid #002660; }
.sys-admin-role-card.is-secondary { border-left: 4px solid #475569; }
.sys-admin-role-card h4 { margin: 0 0 10px; font-size: 10px; font-weight: 900; color: #64748b; letter-spacing: 0.14em; text-transform: uppercase; }
.sys-admin-role-card__row { display: flex; align-items: center; gap: 10px; }
.sys-admin-role-icon { width: 44px; height: 44px; border-radius: 2px; display: grid; place-items: center; }
.sys-admin-role-icon.is-primary { background: rgba(0, 38, 96, 0.08); color: #002660; }
.sys-admin-role-icon.is-secondary { background: rgba(71, 85, 105, 0.12); color: #334155; }
.sys-admin-role-icon .material-symbols-outlined { font-size: 26px; }
.sys-admin-role-code { font-weight: 900; color: #002660; }
.sys-admin-role-desc { margin-top: 2px; font-size: 11px; color: #64748b; line-height: 1.45; }

.sys-admin-jurisdiction { background: #fff; border: 1px solid rgba(226, 232, 240, 0.6); box-shadow: 0 1px 2px rgba(15, 23, 42, 0.05); border-radius: 2px; overflow: hidden; }
.sys-admin-jurisdiction__head { padding: 12px 14px; display: flex; align-items: center; gap: 8px; border-bottom: 1px solid rgba(226, 232, 240, 0.7); color: #002660; }
.sys-admin-jurisdiction__head h3 { margin: 0; font-size: 13px; font-weight: 900; }
.sys-admin-jurisdiction__body { display: flex; height: 240px; }
.sys-admin-jurisdiction__meta { width: 34%; min-width: 240px; background: #f8fafc; padding: 14px; display: grid; gap: 12px; border-right: 1px solid rgba(226, 232, 240, 0.7); }
.sys-admin-juri-label { font-size: 10px; font-weight: 900; letter-spacing: 0.14em; text-transform: uppercase; color: #64748b; }
.sys-admin-juri-value { margin-top: 6px; font-size: 12px; font-weight: 800; color: #0f172a; line-height: 1.45; }
.sys-admin-jurisdiction__map { position: relative; flex: 1; background: radial-gradient(circle at 20% 20%, rgba(0, 58, 140, 0.22), transparent 55%), radial-gradient(circle at 80% 70%, rgba(201, 215, 254, 0.9), transparent 60%), linear-gradient(135deg, rgba(2, 38, 96, 0.16), rgba(15, 23, 42, 0.04)); cursor: pointer; }
.sys-admin-map-overlay { position: absolute; inset: 0; display: grid; place-items: center; background: rgba(0, 38, 96, 0.12); opacity: 0; transition: opacity 160ms ease; }
.sys-admin-jurisdiction__map:hover .sys-admin-map-overlay { opacity: 1; }
.sys-admin-map-chip { background: #fff; color: #002660; font-weight: 900; font-size: 12px; border-radius: 2px; padding: 8px 12px; box-shadow: 0 16px 40px rgba(0, 38, 96, 0.16); }

.sys-admin-status { position: fixed; right: 18px; bottom: 18px; color: #fff; border-radius: 8px; padding: 10px 12px; font-size: 12px; background: #0f172a; }
.sys-admin-status.error { background: #b91c1c; }
.sys-admin-status.success { background: #166534; }
.sys-admin-status.warning { background: #b45309; }

@media (max-width: 1200px) {
  .sys-admin-detail-grid { grid-template-columns: 1fr; }
  .sys-admin-jurisdiction__body { flex-direction: column; height: auto; }
  .sys-admin-jurisdiction__meta { width: 100%; min-width: 0; border-right: 0; border-bottom: 1px solid rgba(226, 232, 240, 0.7); }
  .sys-admin-jurisdiction__map { height: 220px; }
}

@media (max-width: 900px) {
  .sys-admin-info-grid { grid-template-columns: 1fr; }
  .sys-admin-role-grid { grid-template-columns: 1fr; }
  .sys-admin-detail-header { align-items: start; flex-direction: column; }
  .sys-admin-detail-header__actions { width: 100%; justify-content: flex-start; }
}
</style>

