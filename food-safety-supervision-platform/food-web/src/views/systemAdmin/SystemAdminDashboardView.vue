<template>
  <SystemAdminWorkspacePage
    active-key="dashboard"
    :username="adminUser.username"
    @navigate="handleSidebarNavigate"
    @logout="handleLogout"
  >
    <section class="sys-admin-dashboard">
      <header class="sys-admin-dashboard__hero">
        <h2>系统管理员工作台</h2>
        <p>欢迎回来。这里展示监管人员总体情况，以及来自日志表的最新真实动态。</p>
      </header>

      <div class="sys-admin-bento">
        <section class="sys-admin-kpi-grid">
          <article class="sys-admin-kpi-card">
            <p>监管人员总量</p>
            <strong>{{ totalCount }}</strong>
            <small>更新时间：{{ lastUpdateText }}</small>
          </article>
          <article class="sys-admin-kpi-card is-green">
            <p>在岗人数</p>
            <strong>{{ activeCount }}</strong>
            <small>在岗率 {{ activeRatio }}%</small>
          </article>
          <article class="sys-admin-kpi-card is-red">
            <p>停用人数</p>
            <strong>{{ inactiveCount }}</strong>
            <small>需关注账号状态变化</small>
          </article>

          <article class="sys-admin-role-card">
            <h3>角色分布比例</h3>
            <div class="sys-admin-role-row">
              <span>监管管理员</span>
              <strong>{{ adminRoleCount }}</strong>
            </div>
            <div class="sys-admin-progress"><span :style="{ width: `${adminRatio}%` }"></span></div>
            <div class="sys-admin-role-row">
              <span>监管执法人员</span>
              <strong>{{ enforcerRoleCount }}</strong>
            </div>
            <div class="sys-admin-progress is-light"><span :style="{ width: `${enforcerRatio}%` }"></span></div>
          </article>
        </section>

        <aside class="sys-admin-side">
          <section class="sys-admin-quick-card">
            <h3>快捷操作</h3>
            <button type="button" @click="handleSidebarNavigate('create')">
              <span>新建监管人员</span><span class="material-symbols-outlined">chevron_right</span>
            </button>
            <button type="button" @click="handleSidebarNavigate('list')">
              <span>查看监管人员列表</span><span class="material-symbols-outlined">chevron_right</span>
            </button>
          </section>

          <section class="sys-admin-activity-card">
            <div class="sys-admin-activity-card__head">
              <h3>最近动态</h3>
            </div>
            <div v-if="!activities.length" class="sys-admin-empty">暂无动态数据。</div>
            <ul v-else class="sys-admin-activity-list">
              <li v-for="item in activities" :key="item.id">
                <button
                  type="button"
                  class="sys-admin-log-link"
                  :disabled="!item.targetUserId"
                  @click="goAuditTarget(item.targetUserId)"
                >
                  <strong>{{ item.title }}</strong>
                  <p>{{ item.desc }}</p>
                  <small>{{ item.time }}</small>
                </button>
              </li>
            </ul>
          </section>
        </aside>

        <section class="sys-admin-audit-card">
          <div class="sys-admin-audit-card__head">
            <h3>系统操作审计</h3>
          </div>
          <table>
            <thead>
              <tr>
                <th>时间</th>
                <th>操作人</th>
                <th>动作</th>
                <th>对象</th>
                <th>状态</th>
              </tr>
            </thead>
            <tbody>
              <tr v-if="!auditRows.length">
                <td colspan="5" class="sys-admin-empty">暂无审计日志。</td>
              </tr>
              <tr
                v-for="item in auditRows"
                :key="item.id"
                class="sys-admin-audit-row"
                :class="{ 'is-clickable': !!item.targetUserId }"
                @click="goAuditTarget(item.targetUserId)"
              >
                <td>{{ item.time }}</td>
                <td>{{ item.operator }}</td>
                <td>{{ item.action }}</td>
                <td>{{ item.target }}</td>
                <td><span class="sys-admin-chip" :class="item.tone">{{ item.status }}</span></td>
              </tr>
            </tbody>
          </table>
        </section>
      </div>
    </section>
  </SystemAdminWorkspacePage>
</template>

<script setup>
import { computed, onMounted, ref } from "vue";
import { useRouter } from "vue-router";
import { fetchRecentRegulatorAuditLogs, fetchRegulatorProfiles } from "../../api/regulation";
import SystemAdminWorkspacePage from "../../components/systemAdmin/SystemAdminWorkspacePage.vue";
import { useSystemAdminShellSession } from "./systemAdminShared";

const { adminUser, token, handleSidebarNavigate, handleLogout } = useSystemAdminShellSession();
const router = useRouter();

const regulators = ref([]);
const lastUpdateText = ref("--");
const activities = ref([]);
const auditRows = ref([]);

const totalCount = computed(() => regulators.value.length);
const activeCount = computed(() => regulators.value.filter((item) => Number(item.status) === 1).length);
const inactiveCount = computed(() => Math.max(totalCount.value - activeCount.value, 0));
const adminRoleCount = computed(() => regulators.value.filter((item) => String(item.roleType) === "REGULATOR_ADMIN").length);
const enforcerRoleCount = computed(() => regulators.value.filter((item) => String(item.roleType) === "REGULATOR_ENFORCER").length);

const activeRatio = computed(() => (totalCount.value ? Math.round((activeCount.value / totalCount.value) * 100) : 0));
const adminRatio = computed(() => (totalCount.value ? Math.round((adminRoleCount.value / totalCount.value) * 100) : 0));
const enforcerRatio = computed(() => (totalCount.value ? Math.round((enforcerRoleCount.value / totalCount.value) * 100) : 0));

function formatDateTime(value) {
  if (!value) return "--";
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return String(value);
  return date.toLocaleString("zh-CN", { hour12: false });
}

function goAuditTarget(targetUserId) {
  const userId = Number(targetUserId || 0);
  if (!userId) return;
  router.push({ name: "admin-regulator-detail", params: { userId } });
}

function buildActivities(logs) {
  activities.value = logs.slice(0, 3).map((item, index) => ({
    id: item.id || `activity-${index}`,
    title: item.actionName || item.actionType || "监管人员动态",
    desc: item.summary || `${item.targetName || "监管人员"} 发生了一条新的操作记录`,
    time: formatDateTime(item.createTime),
    targetUserId: item.targetUserId || null
  }));
}

function buildAuditRows(logs) {
  auditRows.value = logs.slice(0, 6).map((item, index) => ({
    id: item.id || `audit-${index}`,
    time: formatDateTime(item.createTime),
    operator: item.operatorName || "系统",
    action: item.actionName || item.actionType || "监管操作",
    target: item.targetName || "-",
    status: "已记录",
    tone: "is-success",
    targetUserId: item.targetUserId || null
  }));
}

async function loadDashboard() {
  const [records, logs] = await Promise.all([
    fetchRegulatorProfiles(token.value).catch(() => []),
    fetchRecentRegulatorAuditLogs(token.value, 6).catch(() => [])
  ]);
  regulators.value = Array.isArray(records) ? records : [];
  const auditLogs = Array.isArray(logs) ? logs : [];
  buildActivities(auditLogs);
  buildAuditRows(auditLogs);
  lastUpdateText.value = new Date().toLocaleString("zh-CN", { hour12: false });
}

onMounted(() => {
  loadDashboard();
});
</script>

<style scoped>
.sys-admin-dashboard {
  display: grid;
  gap: 24px;
  max-width: 1280px;
  margin: 0 auto;
  padding: 0 8px 88px;
}
.sys-admin-dashboard__hero h2 {
  margin: 0;
  color: #002660;
  font-size: 32px;
  font-weight: 900;
  letter-spacing: -0.02em;
}
.sys-admin-dashboard__hero p {
  margin: 6px 0 0;
  color: #434651;
  font-size: 14px;
}
.sys-admin-bento {
  display: grid;
  grid-template-columns: minmax(0, 8fr) minmax(0, 4fr);
  gap: 24px;
}
.sys-admin-kpi-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
}
.sys-admin-kpi-card,
.sys-admin-role-card,
.sys-admin-activity-card,
.sys-admin-audit-card {
  background: #fff;
  border-radius: 2px;
}
.sys-admin-kpi-card {
  border-left: 4px solid #002660;
  padding: 24px;
  box-shadow: 0 1px 2px rgba(15, 23, 42, 0.05);
}
.sys-admin-kpi-card p {
  margin: 0;
  color: #434651;
  font-size: 11px;
  text-transform: uppercase;
  letter-spacing: 0.08em;
  font-weight: 700;
}
.sys-admin-kpi-card strong {
  display: block;
  margin-top: 10px;
  color: #002660;
  font-size: 36px;
  font-weight: 900;
}
.sys-admin-kpi-card small {
  color: #94a3b8;
  font-size: 10px;
}
.sys-admin-kpi-card.is-green {
  border-left-color: #15803d;
}
.sys-admin-kpi-card.is-green strong {
  color: #166534;
}
.sys-admin-kpi-card.is-red {
  border-left-color: #ba1a1a;
}
.sys-admin-kpi-card.is-red strong {
  color: #b91c1c;
}
.sys-admin-role-card {
  grid-column: 1 / -1;
  padding: 22px 24px;
  box-shadow: 0 1px 2px rgba(15, 23, 42, 0.05);
}
.sys-admin-role-card h3 {
  margin: 0 0 16px;
  color: #002660;
  font-size: 14px;
}
.sys-admin-role-row {
  display: flex;
  justify-content: space-between;
  font-size: 12px;
}
.sys-admin-progress {
  background: #e0e3e6;
  height: 8px;
  border-radius: 999px;
  overflow: hidden;
  margin: 6px 0 12px;
}
.sys-admin-progress span {
  display: block;
  height: 100%;
  background: #002660;
}
.sys-admin-progress.is-light span {
  background: #003a8c;
}
.sys-admin-side {
  display: grid;
  gap: 16px;
}
.sys-admin-quick-card {
  background: #002660;
  color: #fff;
  border-radius: 2px;
  padding: 24px;
  display: grid;
  gap: 10px;
  box-shadow: 0 10px 24px rgba(2, 38, 96, 0.2);
}
.sys-admin-quick-card h3 {
  margin: 0 0 10px;
  font-size: 14px;
  letter-spacing: 0.02em;
}
.sys-admin-quick-card button {
  border: 1px solid rgba(255, 255, 255, 0.2);
  background: rgba(255, 255, 255, 0.08);
  color: inherit;
  border-radius: 2px;
  padding: 12px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  cursor: pointer;
  transition: 0.2s ease;
}
.sys-admin-quick-card button:hover {
  background: rgba(255, 255, 255, 0.18);
}
.sys-admin-activity-card {
  padding: 20px;
  box-shadow: 0 1px 2px rgba(15, 23, 42, 0.05);
}
.sys-admin-activity-card__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.sys-admin-activity-card__head h3 {
  margin: 0;
  color: #002660;
  font-size: 14px;
}
.sys-admin-empty {
  padding: 18px 0;
  color: #64748b;
  font-size: 13px;
  text-align: center;
}
.sys-admin-activity-list {
  list-style: none;
  margin: 12px 0 0;
  padding: 0;
  display: grid;
  gap: 12px;
}
.sys-admin-activity-list li {
  background: #f2f4f7;
  border-radius: 2px;
}
.sys-admin-log-link {
  width: 100%;
  border: 0;
  background: transparent;
  text-align: left;
  padding: 12px;
  cursor: pointer;
  outline: none;
  box-shadow: none;
  appearance: none;
}
.sys-admin-log-link:disabled {
  cursor: default;
}
.sys-admin-log-link:not(:disabled):hover {
  background: rgba(0, 38, 96, 0.04);
}
.sys-admin-log-link:not(:disabled):hover strong {
  color: #002660;
}
.sys-admin-log-link:focus,
.sys-admin-log-link:focus-visible {
  outline: none;
  box-shadow: none;
}
.sys-admin-activity-list strong {
  font-size: 12px;
  color: #0f172a;
}
.sys-admin-activity-list p {
  margin: 4px 0 0;
  color: #475569;
  font-size: 11px;
}
.sys-admin-activity-list small {
  color: #94a3b8;
  font-size: 10px;
}
.sys-admin-audit-card {
  grid-column: 1 / -1;
  overflow: hidden;
  box-shadow: 0 1px 2px rgba(15, 23, 42, 0.05);
}
.sys-admin-audit-card__head {
  border-bottom: 1px solid #eceef1;
  padding: 14px 20px;
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.sys-admin-audit-card__head h3 {
  margin: 0;
  color: #002660;
  font-size: 14px;
}
.sys-admin-audit-card table {
  width: 100%;
  border-collapse: collapse;
  font-size: 12px;
}
.sys-admin-audit-card th,
.sys-admin-audit-card td {
  padding: 12px 20px;
  text-align: left;
}
.sys-admin-audit-card thead tr {
  background: #f2f4f7;
  color: #434651;
}
.sys-admin-audit-card tbody tr:nth-child(odd) {
  background: #fff;
}
.sys-admin-audit-card tbody tr:nth-child(even) {
  background: #f2f4f7;
}
.sys-admin-audit-row.is-clickable {
  cursor: pointer;
}
.sys-admin-audit-row.is-clickable:hover {
  background: #eef4ff !important;
}
.sys-admin-chip {
  border-radius: 2px;
  padding: 2px 8px;
  font-size: 10px;
  font-weight: 700;
  background: #dbeafe;
  color: #1d4ed8;
}
.sys-admin-chip.is-success {
  background: #dcfce7;
  color: #166534;
}
@media (max-width: 1280px) {
  .sys-admin-bento {
    grid-template-columns: 1fr;
  }
}
@media (max-width: 900px) {
  .sys-admin-kpi-grid {
    grid-template-columns: 1fr;
  }
}
</style>
