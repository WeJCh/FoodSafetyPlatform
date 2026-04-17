<template>
  <SystemAdminWorkspacePage
    active-key="dashboard"
    :username="adminUser.username"
    @navigate="handleSidebarNavigate"
    @logout="handleLogout"
    @pending-feature="onPendingFeature"
  >
    <section class="sys-admin-dashboard">
      <header class="sys-admin-dashboard__hero">
        <h2>监管工作台</h2>
        <p>欢迎回来，系统当前运行状态良好。以下是实时监管数据汇总。</p>
      </header>

      <div class="sys-admin-bento">
        <section class="sys-admin-kpi-grid">
          <article class="sys-admin-kpi-card">
            <p>监管人员总量</p>
            <strong>{{ totalCount }}</strong>
            <small>更新时间: {{ lastUpdateText }}</small>
          </article>
          <article class="sys-admin-kpi-card is-green">
            <p>在岗人数 (Status 1)</p>
            <strong>{{ activeCount }}</strong>
            <small>在岗率 {{ activeRatio }}%</small>
          </article>
          <article class="sys-admin-kpi-card is-red">
            <p>停用人数 (Status 0)</p>
            <strong>{{ inactiveCount }}</strong>
            <small>需核查账号状态</small>
          </article>

          <article class="sys-admin-role-card">
            <h3>角色分布比例</h3>
            <div class="sys-admin-role-row">
              <span>区域管理员</span>
              <strong>{{ adminRoleCount }}</strong>
            </div>
            <div class="sys-admin-progress"><span :style="{ width: `${adminRatio}%` }"></span></div>
            <div class="sys-admin-role-row">
              <span>执法人员</span>
              <strong>{{ enforcerRoleCount }}</strong>
            </div>
            <div class="sys-admin-progress is-light"><span :style="{ width: `${enforcerRatio}%` }"></span></div>
          </article>
        </section>

        <aside class="sys-admin-side">
          <section class="sys-admin-quick-card">
            <h3>快速操作</h3>
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
              <button type="button" @click="onPendingFeature('查看全部动态')">查看全部</button>
            </div>
            <div v-if="!activities.length" class="sys-admin-empty">暂无动态数据。</div>
            <ul v-else class="sys-admin-activity-list">
              <li v-for="item in activities" :key="item.id">
                <strong>{{ item.title }}</strong>
                <p>{{ item.desc }}</p>
                <small>{{ item.time }}</small>
              </li>
            </ul>
          </section>
        </aside>

        <section class="sys-admin-audit-card">
          <div class="sys-admin-audit-card__head">
            <h3>系统操作审计</h3>
            <button type="button" @click="onPendingFeature('审计筛选器')">筛选器占位</button>
          </div>
          <table>
            <thead>
              <tr>
                <th>时间 / TIMESTAMP</th>
                <th>操作员 / OPERATOR</th>
                <th>动作 / ACTION</th>
                <th>对象 / TARGET</th>
                <th>状态 / STATUS</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="item in auditRows" :key="item.id">
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

      <footer class="sys-admin-footer">
        <div>© 2026 Sentinel Governance Ecosystem</div>
        <div class="sys-admin-footer__links">
          <button type="button" @click="onPendingFeature('Documentation')">Documentation</button>
          <button type="button" @click="onPendingFeature('API Status')">API Status</button>
          <button type="button" @click="onPendingFeature('Privacy Protocol')">Privacy Protocol</button>
        </div>
      </footer>

      <button class="sys-admin-fab" type="button" @click="onPendingFeature('快捷新建')">
        <span class="material-symbols-outlined">add</span>
      </button>
    </section>
  </SystemAdminWorkspacePage>
</template>

<script setup>
import { computed, onMounted, ref } from "vue";
import { fetchRegulatorProfiles } from "../../api/regulation";
import SystemAdminWorkspacePage from "../../components/systemAdmin/SystemAdminWorkspacePage.vue";
import {
  systemAdminFeaturePendingNotice,
  useSystemAdminShellSession
} from "./systemAdminShared";

const { adminUser, token, handleSidebarNavigate, handleLogout } = useSystemAdminShellSession();

const regulators = ref([]);
const lastUpdateText = ref("--");
const activities = ref([]);
const auditRows = ref([]);

const totalCount = computed(() => regulators.value.length);
const activeCount = computed(() => regulators.value.filter((item) => Number(item.status) === 1).length);
const inactiveCount = computed(() => Math.max(totalCount.value - activeCount.value, 0));
const adminRoleCount = computed(
  () => regulators.value.filter((item) => String(item.roleType) === "REGULATOR_ADMIN").length
);
const enforcerRoleCount = computed(
  () => regulators.value.filter((item) => String(item.roleType) === "REGULATOR_ENFORCER").length
);

const activeRatio = computed(() => (totalCount.value ? Math.round((activeCount.value / totalCount.value) * 100) : 0));
const adminRatio = computed(() => (totalCount.value ? Math.round((adminRoleCount.value / totalCount.value) * 100) : 0));
const enforcerRatio = computed(() =>
  totalCount.value ? Math.round((enforcerRoleCount.value / totalCount.value) * 100) : 0
);

function onPendingFeature(title) {
  // TODO: 通知中心、系统设置、帮助中心、审计筛选等功能待后端和交互流程完善
  systemAdminFeaturePendingNotice(title);
}

function formatRoleType(roleType) {
  if (roleType === "REGULATOR_ADMIN") return "区域管理员";
  if (roleType === "REGULATOR_ENFORCER") return "执法人员";
  return "未知角色";
}

function buildMockAuditRows() {
  // TODO: 接入系统管理员审计日志 API（按时间分页 + 条件筛选）
  auditRows.value = [
    { id: "a1", time: "2026-04-15 09:45:12", operator: "Admin_01", action: "UPDATE_ROLE", target: "User_8892", status: "SUCCESS", tone: "is-success" },
    { id: "a2", time: "2026-04-15 09:30:05", operator: "Admin_02", action: "CREATE_USER", target: "User_9102", status: "SUCCESS", tone: "is-success" },
    { id: "a3", time: "2026-04-15 09:12:44", operator: "System_Core", action: "ARCHIVE_RECORDS", target: "Batch_Apr_26", status: "COMPLETED", tone: "is-info" }
  ];
}

function buildActivities(records) {
  activities.value = records.slice(0, 3).map((item) => ({
    id: item.id,
    title: `${item.name || "监管人员"} (${formatRoleType(item.roleType)})`,
    desc: Number(item.status) === 1 ? "账号处于在岗状态，可继续执行监管任务。" : "账号处于停用状态，建议管理员复核。",
    time: "刚刚同步"
  }));
}

async function loadDashboard() {
  const records = await fetchRegulatorProfiles(token.value).catch(() => []);
  regulators.value = Array.isArray(records) ? records : [];
  buildActivities(regulators.value);
  buildMockAuditRows();
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
.sys-admin-activity-card__head button {
  border: 0;
  background: transparent;
  color: #003a8c;
  cursor: pointer;
  font-size: 11px;
  font-weight: 700;
}
.sys-admin-empty {
  padding: 18px 0;
  color: #64748b;
  font-size: 13px;
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
  padding: 12px;
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
.sys-admin-audit-card__head button {
  border: 0;
  background: #f2f4f7;
  color: #475569;
  border-radius: 2px;
  padding: 4px 8px;
  font-size: 10px;
  font-weight: 700;
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
.sys-admin-chip.is-info {
  background: #dbeafe;
  color: #1d4ed8;
}
.sys-admin-footer {
  margin-top: 6px;
  border-top: 1px solid #e2e8f0;
  padding: 18px 2px 0;
  display: flex;
  justify-content: space-between;
  align-items: center;
  color: #94a3b8;
  font-size: 10px;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.1em;
}
.sys-admin-footer__links {
  display: flex;
  gap: 10px;
}
.sys-admin-footer__links button {
  border: 0;
  background: transparent;
  color: #94a3b8;
  font-size: 10px;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.1em;
  cursor: pointer;
}
.sys-admin-footer__links button:hover {
  color: #002660;
}
.sys-admin-fab {
  position: fixed;
  right: 32px;
  bottom: 28px;
  width: 56px;
  height: 56px;
  border-radius: 10px;
  border: 0;
  background: #002660;
  color: #fff;
  display: grid;
  place-items: center;
  cursor: pointer;
  box-shadow: 0 14px 28px rgba(2, 38, 96, 0.28);
  z-index: 36;
  transition: transform 0.2s ease;
}
.sys-admin-fab:hover {
  transform: scale(1.05);
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
