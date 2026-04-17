<template>
  <SystemAdminWorkspacePage
    active-key="list"
    :username="adminUser.username"
    search-placeholder="全局搜索指令..."
    @navigate="handleSidebarNavigate"
    @logout="handleLogout"
    @pending-feature="onPendingFeature"
  >
    <section class="sys-admin-list-page">
      <header class="sys-admin-list-page__header">
        <div>
          <nav class="sys-admin-breadcrumb">
            <span>系统管理</span>
            <span>/</span>
            <span class="is-current">监管人员列表</span>
          </nav>
          <h1>监管人员列表</h1>
        </div>
        <button type="button" class="sys-admin-create-btn" @click="handleSidebarNavigate('create')">
          <span class="material-symbols-outlined">add</span>
          <span>新建监管人员</span>
        </button>
      </header>

      <section class="sys-admin-filter-panel">
        <div class="sys-admin-filter-grid">
          <label>
            <span>姓名 / 账号</span>
            <input v-model.trim="filters.keyword" type="text" placeholder="输入关键字搜索" />
          </label>
          <label>
            <span>角色权限</span>
            <select v-model="filters.roleType">
              <option value="">全部角色</option>
              <option value="REGULATOR_ADMIN">区域管理员</option>
              <option value="REGULATOR_ENFORCER">执法人员</option>
            </select>
          </label>
          <label>
            <span>当前状态</span>
            <select v-model="filters.status">
              <option value="">全部状态</option>
              <option value="1">在岗</option>
              <option value="0">停用</option>
            </select>
          </label>
          <div class="sys-admin-filter-actions">
            <button type="button" class="btn-secondary" @click="resetFilters">重置</button>
            <button type="button" class="btn-primary" :disabled="loading" @click="loadRegulators">
              {{ loading ? "查询中..." : "查询" }}
            </button>
          </div>
        </div>
      </section>

      <section class="sys-admin-table-panel">
        <table>
          <thead>
            <tr>
              <th>姓名 / 账号</th>
              <th>角色</th>
              <th>所属辖区</th>
              <th>状态</th>
              <th class="is-right">操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-if="!displayRows.length">
              <td colspan="5" class="sys-admin-empty">暂无监管人员记录</td>
            </tr>
            <tr v-for="item in pagedRows" :key="item.id">
              <td>
                <div class="sys-admin-person-cell">
                  <div class="sys-admin-avatar">{{ getAvatarText(item.name) }}</div>
                  <div>
                    <div class="sys-admin-person-name">{{ item.name || "未命名监管员" }}</div>
                    <div class="sys-admin-person-id">ID: {{ item.id }}</div>
                  </div>
                </div>
              </td>
              <td>
                <span class="sys-admin-role-chip" :class="{ 'is-admin': item.roleType === 'REGULATOR_ADMIN' }">
                  {{ formatRoleType(item.roleType) }}
                </span>
              </td>
              <td>{{ item.regionText || "辖区信息待完善" }}</td>
              <td>
                <span class="sys-admin-status-chip" :class="{ 'is-disabled': Number(item.status) !== 1 }">
                  {{ Number(item.status) === 1 ? "在岗" : "停用" }}
                </span>
              </td>
              <td class="is-right">
                <div class="sys-admin-row-actions">
                  <button type="button" @click="goDetail(item)">详情</button>
                  <button type="button" @click="goEdit(item)">编辑</button>
                  <button
                    type="button"
                    :class="{ 'is-danger': Number(item.status) === 1 }"
                    @click="goStatusConfirm(item)"
                  >
                    {{ Number(item.status) === 1 ? "停用" : "启用" }}
                  </button>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
        <div class="sys-admin-pagination">
          <div class="sys-admin-pagination__meta">
            显示 {{ pageStart }} - {{ pageEnd }}，共 {{ totalCount }} 条记录
          </div>
          <div class="sys-admin-pagination__actions">
            <label>
              每页
              <select v-model.number="pagination.pageSize">
                <option :value="5">5</option>
                <option :value="10">10</option>
                <option :value="20">20</option>
              </select>
              条
            </label>
            <button type="button" :disabled="pagination.page <= 1" @click="goPrevPage">上一页</button>
            <button
              v-for="page in pageNumbers"
              :key="page"
              type="button"
              :class="{ 'is-active': page === pagination.page }"
              @click="goPage(page)"
            >
              {{ page }}
            </button>
            <button type="button" :disabled="pagination.page >= totalPages" @click="goNextPage">下一页</button>
          </div>
        </div>
      </section>

      <section class="sys-admin-bottom-grid">
        <article class="sys-admin-bottom-card is-gradient">
          <h4>今日在线统计</h4>
          <div class="sys-admin-bottom-value">{{ activeCount }}</div>
          <p>活跃监管员 / {{ displayRows.length }} 总计</p>
        </article>
        <article class="sys-admin-bottom-card sys-admin-log-card">
          <h4>最近操作日志</h4>
          <ul>
            <li v-for="item in opLogs" :key="item.id">
              <strong>{{ item.title }}</strong>
              <p>{{ item.desc }}</p>
              <small>{{ item.time }}</small>
            </li>
          </ul>
        </article>
      </section>

      <div v-if="status.message" class="sys-admin-status" :class="status.type">{{ status.message }}</div>
    </section>
  </SystemAdminWorkspacePage>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from "vue";
import { useRouter } from "vue-router";
import { fetchRegulatorProfiles, fetchRegionPath } from "../../api/regulation";
import SystemAdminWorkspacePage from "../../components/systemAdmin/SystemAdminWorkspacePage.vue";
import {
  systemAdminFeaturePendingNotice,
  useSystemAdminShellSession
} from "./systemAdminShared";

const { adminUser, token, handleSidebarNavigate, handleLogout } = useSystemAdminShellSession();
const router = useRouter();

const loading = ref(false);
const rows = ref([]);
const opLogs = ref([]);
const status = reactive({ message: "", type: "" });
const filters = reactive({ keyword: "", roleType: "", status: "" });
const pagination = reactive({ page: 1, pageSize: 10 });

const displayRows = computed(() => rows.value.filter((item) => {
  if (filters.roleType && item.roleType !== filters.roleType) return false;
  if (filters.status !== "" && String(item.status) !== String(filters.status)) return false;
  const keyword = filters.keyword.trim();
  if (!keyword) return true;
  return String(item.name || "").includes(keyword) || String(item.id || "").includes(keyword);
}));
const totalCount = computed(() => displayRows.value.length);
const totalPages = computed(() => Math.max(1, Math.ceil(totalCount.value / pagination.pageSize)));
const pagedRows = computed(() => {
  const start = (pagination.page - 1) * pagination.pageSize;
  const end = start + pagination.pageSize;
  return displayRows.value.slice(start, end);
});
const pageStart = computed(() => (totalCount.value ? (pagination.page - 1) * pagination.pageSize + 1 : 0));
const pageEnd = computed(() => Math.min(pagination.page * pagination.pageSize, totalCount.value));
const pageNumbers = computed(() => {
  const maxVisible = 5;
  const pages = [];
  let start = Math.max(1, pagination.page - 2);
  let end = Math.min(totalPages.value, start + maxVisible - 1);
  if (end - start < maxVisible - 1) {
    start = Math.max(1, end - maxVisible + 1);
  }
  for (let i = start; i <= end; i += 1) pages.push(i);
  return pages;
});

const activeCount = computed(() => displayRows.value.filter((item) => Number(item.status) === 1).length);

function setStatus(message, type = "info") {
  status.message = message;
  status.type = type;
}

function formatRoleType(roleType) {
  if (roleType === "REGULATOR_ADMIN") return "区域管理员";
  if (roleType === "REGULATOR_ENFORCER") return "执法人员";
  return "未知角色";
}

function getAvatarText(name) {
  return String(name || "监").trim().slice(0, 1);
}

function onPendingFeature(title) {
  // TODO: 详情页、编辑页、日志中心与筛选联动后续接入真实后端能力
  systemAdminFeaturePendingNotice(title);
}

function goDetail(item) {
  const uid = Number(item?.userId || 0) || null;
  if (!uid) {
    onPendingFeature("监管人员详情（缺少 userId）");
    return;
  }
  router.push({ name: "admin-regulator-detail", params: { userId: uid } });
}

function goEdit(item) {
  const uid = Number(item?.userId || 0) || null;
  if (!uid) {
    onPendingFeature("编辑监管人员（缺少 userId）");
    return;
  }
  router.push({ name: "admin-regulator-edit", params: { userId: uid } });
}

function goStatusConfirm(item) {
  const uid = Number(item?.userId || 0) || null;
  if (!uid) {
    onPendingFeature("状态切换（缺少 userId）");
    return;
  }
  const targetStatus = Number(item.status) === 1 ? 0 : 1;
  router.push({
    name: "admin-regulator-status-confirm",
    params: { userId: uid },
    query: { targetStatus: String(targetStatus), from: "list" }
  });
}

function buildNowTimeText() {
  return new Date().toLocaleString("zh-CN", { hour12: false });
}

function resetFilters() {
  filters.keyword = "";
  filters.roleType = "";
  filters.status = "";
  pagination.page = 1;
  loadRegulators();
}

function goPage(page) {
  const next = Math.min(Math.max(1, page), totalPages.value);
  pagination.page = next;
}

function goPrevPage() {
  goPage(pagination.page - 1);
}

function goNextPage() {
  goPage(pagination.page + 1);
}

async function resolveRegionText(regionIds = []) {
  const firstRegionId = Array.isArray(regionIds) ? Number(regionIds[0] || 0) : 0;
  if (!firstRegionId) return "辖区信息待完善";
  try {
    const pathList = await fetchRegionPath(token.value, firstRegionId);
    if (!Array.isArray(pathList) || !pathList.length) return `辖区ID: ${firstRegionId}`;
    return pathList.map((item) => item.name).join(" / ");
  } catch {
    return `辖区ID: ${firstRegionId}`;
  }
}

async function loadRegulators() {
  loading.value = true;
  setStatus("");
  try {
    const records = await fetchRegulatorProfiles(token.value).catch(() => []);
    const normalized = Array.isArray(records) ? records : [];
    const mapped = await Promise.all(
      normalized.map(async (item) => ({
        ...item,
        regionText: await resolveRegionText(item.regionIds)
      }))
    );
    rows.value = mapped;
    // TODO: 接入独立审计日志 API 后替换为真实日志流
    opLogs.value = mapped.slice(0, 6).map((item, index) => ({
      id: item.id || String(index),
      title: "人员状态变更",
      desc: `${item.name || "监管员"} 当前状态：${Number(item.status) === 1 ? "在岗" : "停用"}`,
      time: buildNowTimeText()
    }));
  } catch (error) {
    setStatus(error.message || "加载监管人员列表失败", "error");
  } finally {
    loading.value = false;
  }
}

onMounted(() => {
  loadRegulators();
});

watch([() => filters.keyword, () => filters.roleType, () => filters.status, () => pagination.pageSize], () => {
  pagination.page = 1;
});

watch(totalPages, (next) => {
  if (pagination.page > next) pagination.page = next;
});
</script>

<style scoped>
.sys-admin-list-page {
  position: relative;
  display: grid;
  gap: 16px;
}
.sys-admin-list-page__header {
  display: flex;
  align-items: end;
  justify-content: space-between;
}
.sys-admin-breadcrumb {
  display: flex;
  gap: 6px;
  color: #94a3b8;
  font-size: 10px;
  text-transform: uppercase;
  letter-spacing: 0.12em;
  font-weight: 700;
}
.sys-admin-breadcrumb .is-current {
  color: #002660;
}
.sys-admin-list-page__header h1 {
  margin: 6px 0 0;
  color: #002660;
  font-size: 30px;
  font-weight: 900;
  letter-spacing: -0.01em;
}
.sys-admin-create-btn {
  border: 0;
  background: #002660;
  color: #fff;
  border-radius: 2px;
  padding: 10px 14px;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  cursor: pointer;
}
.sys-admin-filter-panel,
.sys-admin-table-panel {
  background: #fff;
  border: 1px solid #f1f5f9;
  border-radius: 2px;
  box-shadow: 0 1px 2px rgba(15, 23, 42, 0.05);
}
.sys-admin-filter-panel {
  padding: 16px;
}
.sys-admin-filter-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
}
.sys-admin-filter-grid label span {
  display: block;
  margin-bottom: 5px;
  color: #434651;
  font-size: 10px;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.08em;
}
.sys-admin-filter-grid input,
.sys-admin-filter-grid select {
  width: 100%;
  border: 0;
  border-radius: 2px;
  background: #e0e3e6;
  padding: 10px 10px;
  font-size: 12px;
}
.sys-admin-filter-actions {
  display: flex;
  align-items: end;
  gap: 8px;
}
.btn-secondary,
.btn-primary {
  flex: 1;
  border: 0;
  border-radius: 2px;
  padding: 10px 0;
  font-size: 12px;
  font-weight: 700;
  cursor: pointer;
}
.btn-secondary {
  background: #e6e8eb;
  color: #191c1e;
}
.btn-primary {
  background: #002660;
  color: #fff;
}
.btn-primary:disabled {
  opacity: 0.6;
  cursor: default;
}
.sys-admin-table-panel {
  overflow: hidden;
}
.sys-admin-table-panel table {
  width: 100%;
  border-collapse: collapse;
  font-size: 12px;
}
.sys-admin-table-panel thead tr {
  background: #002660;
  color: #fff;
}
.sys-admin-table-panel th {
  padding: 14px 16px;
  text-align: left;
  font-size: 11px;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}
.sys-admin-table-panel tbody tr:nth-child(odd) {
  background: #fff;
}
.sys-admin-table-panel tbody tr:nth-child(even) {
  background: #f2f4f7;
}
.sys-admin-table-panel td {
  padding: 14px 16px;
  color: #434651;
}
.sys-admin-table-panel .is-right {
  text-align: right;
}
.sys-admin-empty {
  text-align: center;
  color: #64748b;
  padding: 28px 0 !important;
}
.sys-admin-person-cell {
  display: flex;
  align-items: center;
  gap: 10px;
}
.sys-admin-avatar {
  width: 30px;
  height: 30px;
  border-radius: 2px;
  background: rgba(0, 38, 96, 0.08);
  color: #002660;
  display: grid;
  place-items: center;
  font-weight: 800;
}
.sys-admin-person-name {
  color: #191c1e;
  font-weight: 700;
}
.sys-admin-person-id {
  color: #94a3b8;
  font-size: 10px;
}
.sys-admin-role-chip,
.sys-admin-status-chip {
  font-size: 10px;
  border-radius: 2px;
  padding: 3px 8px;
  font-weight: 700;
}
.sys-admin-role-chip {
  background: #f1f5f9;
  color: #475569;
}
.sys-admin-role-chip.is-admin {
  background: rgba(0, 38, 96, 0.08);
  color: #002660;
}
.sys-admin-status-chip {
  background: #dcfce7;
  color: #166534;
}
.sys-admin-status-chip.is-disabled {
  background: #ffdad6;
  color: #93000a;
}
.sys-admin-row-actions {
  display: inline-flex;
  gap: 8px;
}
.sys-admin-row-actions button {
  border: 0;
  background: transparent;
  color: #002660;
  font-size: 12px;
  font-weight: 700;
  cursor: pointer;
}
.sys-admin-row-actions button.is-danger {
  color: #ba1a1a;
}
.sys-admin-pagination {
  border-top: 1px solid #e2e8f0;
  background: #f2f4f7;
  padding: 12px 16px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}
.sys-admin-pagination__meta {
  color: #64748b;
  font-size: 11px;
  font-weight: 700;
}
.sys-admin-pagination__actions {
  display: flex;
  align-items: center;
  gap: 6px;
}
.sys-admin-pagination__actions label {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 11px;
  color: #475569;
}
.sys-admin-pagination__actions label select {
  border: 1px solid #cbd5e1;
  background: #fff;
  border-radius: 2px;
  padding: 2px 4px;
  font-size: 11px;
}
.sys-admin-pagination__actions button {
  border: 0;
  background: #fff;
  color: #334155;
  border-radius: 2px;
  min-width: 30px;
  padding: 6px 8px;
  font-size: 11px;
  font-weight: 700;
  cursor: pointer;
}
.sys-admin-pagination__actions button:disabled {
  opacity: 0.45;
  cursor: default;
}
.sys-admin-pagination__actions button.is-active {
  background: #002660;
  color: #fff;
}
.sys-admin-bottom-grid {
  display: grid;
  grid-template-columns: minmax(0, 4fr) minmax(0, 6fr);
  gap: 12px;
  margin-top: 14px;
}
.sys-admin-bottom-card {
  background: #fff;
  border: 1px solid #f1f5f9;
  border-radius: 2px;
  padding: 16px;
}
.sys-admin-bottom-card.is-gradient {
  color: #fff;
  border: 0;
  background: linear-gradient(135deg, #002660, #003a8c);
}
.sys-admin-bottom-card h4 {
  margin: 0;
  font-size: 10px;
  text-transform: uppercase;
  letter-spacing: 0.14em;
}
.sys-admin-bottom-value {
  margin-top: 8px;
  font-size: 36px;
  font-weight: 900;
}
.sys-admin-bottom-card p {
  margin: 6px 0 0;
  font-size: 11px;
}
.sys-admin-log-card h4 {
  margin: 0 0 8px;
  color: #002660;
  font-size: 10px;
  letter-spacing: 0.14em;
  text-transform: uppercase;
}
.sys-admin-log-card ul {
  list-style: none;
  margin: 0;
  padding: 0;
  display: grid;
  grid-template-columns: repeat(6, minmax(0, 1fr));
  gap: 10px;
}
.sys-admin-log-card li {
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 2px;
  padding: 8px 10px;
}
.sys-admin-log-card strong {
  color: #191c1e;
  font-size: 11px;
}
.sys-admin-log-card p {
  margin: 2px 0 0;
  color: #64748b;
  font-size: 10px;
}
.sys-admin-log-card small {
  color: #94a3b8;
  font-size: 9px;
}
.sys-admin-status {
  position: fixed;
  right: 18px;
  bottom: 18px;
  background: #0f172a;
  color: #fff;
  padding: 10px 12px;
  border-radius: 8px;
  font-size: 12px;
}
.sys-admin-status.error {
  background: #b91c1c;
}
@media (max-width: 1200px) {
  .sys-admin-filter-grid {
    grid-template-columns: 1fr 1fr;
  }
  .sys-admin-pagination {
    flex-direction: column;
    align-items: flex-start;
  }
  .sys-admin-bottom-grid {
    grid-template-columns: 1fr;
  }
  .sys-admin-log-card ul {
    grid-template-columns: 1fr;
  }
}
</style>
