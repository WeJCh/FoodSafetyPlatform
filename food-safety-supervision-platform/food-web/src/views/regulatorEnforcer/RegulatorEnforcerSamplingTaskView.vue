<template>
  <RegulatorEnforcerPageShell
    active-key="sampling"
    title="我的抽检任务"
    subtitle="查看抽检任务，按企业、状态、结果快速筛选，并进入详情页核对样品与录入结果。"
  >
    <section class="filter-bento">
      <div class="filter-bento__main filter-panel">
        <div class="filter-panel__head">
          <span class="filter-panel__accent" aria-hidden="true"></span>
          <h2 class="filter-panel__title">列表筛选</h2>
        </div>
        <div class="filter-panel__body">
          <div class="filter-toolbar">
            <div class="filter-grid-3">
              <label>
                任务名称
                <input v-model.trim="uiFilters.taskKeyword" placeholder="输入任务或产品关键词..." />
              </label>
              <label>
                相关企业
                <input v-model.trim="filters.enterpriseName" placeholder="搜索企业名称..." />
              </label>
              <label>
                执行状态
                <select v-model="filters.status">
                  <option value="">全部状态</option>
                  <option value="ASSIGNED">待抽检</option>
                  <option value="COMPLETED">已完成</option>
                  <option value="CLOSED">已归档</option>
                </select>
              </label>
            </div>
            <div class="filter-actions">
              <button class="primary filter-btn" type="button" :disabled="loading" @click="handleSearch">
                {{ loading ? "查询中..." : "查询" }}
              </button>
              <button class="ghost filter-btn" type="button" :disabled="loading" @click="resetListFilters">重置</button>
            </div>
          </div>
        </div>
      </div>
      <div class="filter-bento__side">
        <div class="side-title">
          <span>结果与公示过滤</span>
          <span class="side-title-icon">筛</span>
        </div>
        <div class="side-block">
          <span class="side-label">抽检结果</span>
          <div class="pill-group">
            <button type="button" :class="{ active: uiFilters.resultFilter === 'PASS' }" @click="toggleResultFilter('PASS')">合格</button>
            <button type="button" :class="{ active: uiFilters.resultFilter === 'FAIL' }" @click="toggleResultFilter('FAIL')">不合格</button>
            <button type="button" :class="{ active: uiFilters.resultFilter === 'PENDING' }" @click="toggleResultFilter('PENDING')">待处理</button>
          </div>
        </div>
        <div class="side-block">
          <span class="side-label">公示状态</span>
          <div class="pill-group">
            <button type="button" :class="{ active: uiFilters.publicFilter === 'DRAFT' }" @click="togglePublicFilter('DRAFT')">草稿</button>
            <button type="button" :class="{ active: uiFilters.publicFilter === 'PUBLISHED' }" @click="togglePublicFilter('PUBLISHED')">已公示</button>
            <button type="button" :class="{ active: uiFilters.publicFilter === 'OFFLINE' }" @click="togglePublicFilter('OFFLINE')">已下线</button>
          </div>
        </div>
      </div>
    </section>

    <section class="table-card">
      <div class="table-wrap">
        <table>
          <thead>
            <tr>
              <th>任务编号</th>
              <th>任务 / 产品</th>
              <th>企业名称</th>
              <th class="th-center">状态</th>
              <th class="th-center">结果</th>
              <th class="th-center">公示</th>
              <th>截止时间</th>
              <th class="th-right">操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-if="!loading && !displayedTasks.length">
              <td colspan="8" class="empty">{{ tableEmptyText }}</td>
            </tr>
            <tr v-for="task in displayedTasks" :key="task.id">
              <td class="mono">{{ task.taskNo || `#${task.id}` }}</td>
              <td>
                <div class="task-cell">
                  <strong>{{ task.taskTitle || task.productName || "-" }}</strong>
                  <span>产品：{{ task.productName || "-" }}</span>
                  <small>规格：{{ task.productSpecification || "暂无规格" }}</small>
                </div>
              </td>
              <td class="cell-enterprise">{{ task.enterpriseName || "-" }}</td>
              <td class="td-center">
                <span class="pill pill--status" :class="statusPillClass(task.status)">
                  {{ formatSamplingTaskStatus(task.status) }}
                </span>
              </td>
              <td class="td-center">
                <span class="pill pill--result" :class="resultPillClass(task.samplingResult)">
                  {{ formatInspectionResult(task.samplingResult) }}
                </span>
              </td>
              <td class="td-center">
                <span class="pill pill--pub" :class="publicPillClass(task.samplingPublicStatus)">
                  {{ formatSamplingPublicStatus(task.samplingPublicStatus) }}
                </span>
              </td>
              <td>{{ formatTime(task.deadline) }}</td>
              <td class="td-right">
                <div class="action-row">
                  <button class="ghost op-btn" type="button" @click="openTaskDetail(task)">查看详情</button>
                  <button
                    v-if="task.status === 'ASSIGNED'"
                    class="primary op-btn"
                    type="button"
                    @click="openResultEntry(task)"
                  >
                    录入结果
                  </button>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <footer class="table-footer">
        <span class="footer-meta">
          当前页匹配 {{ displayedTasks.length }}/{{ records.length }} 条，已加载 {{ rangeLabel }}，共 {{ totalDisplay }} 条任务
        </span>
        <div class="pager-actions">
          <button class="ghost" type="button" :disabled="page <= 1 || loading" @click="changePage(page - 1)">上一页</button>
          <button class="ghost" type="button" :disabled="page >= pages || loading" @click="changePage(page + 1)">下一页</button>
        </div>
      </footer>
    </section>

    <div v-if="status.message" class="status-banner" :class="`is-${status.type}`">{{ status.message }}</div>
  </RegulatorEnforcerPageShell>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from "vue";
import { useRouter } from "vue-router";
import { fetchMySamplingTasks } from "../../api/regulationOperation";
import RegulatorEnforcerPageShell from "./RegulatorEnforcerPageShell.vue";
import { formatTime } from "../../utils/formatters";
import { formatStatusLabel, inspectionResultMap, samplingPublicStatusMap, samplingTaskStatusMap } from "../../utils/statusMaps";
import { getEmptyStateText, resolveErrorMessage } from "../../utils/uiFeedback";
import { useRegulatorEnforcerShellSession } from "./regulatorEnforcerShared";

const { token } = useRegulatorEnforcerShellSession();
const router = useRouter();

const loading = ref(false);
const records = ref([]);
const page = ref(1);
const size = ref(8);
const total = ref(0);
const pages = ref(1);
const status = reactive({ message: "", type: "info" });
const filters = reactive({ enterpriseName: "", status: "" });
const uiFilters = reactive({ taskKeyword: "", resultFilter: "", publicFilter: "" });
const baseEmptyText = getEmptyStateText("抽检任务");

function setStatus(message = "", type = "info") {
  status.message = message;
  status.type = type;
}

function formatSamplingTaskStatus(value) {
  return formatStatusLabel(value, samplingTaskStatusMap);
}

function formatSamplingPublicStatus(value) {
  return value ? formatStatusLabel(value, samplingPublicStatusMap) : "未生成";
}

function formatInspectionResult(value) {
  return value ? formatStatusLabel(value, inspectionResultMap) : "待处理";
}

function statusPillClass(value) {
  const normalized = String(value || "").toUpperCase();
  if (normalized === "ASSIGNED") return "is-assigned";
  if (normalized === "COMPLETED") return "is-completed";
  if (normalized === "CLOSED") return "is-closed";
  return "is-default";
}

function resultPillClass(value) {
  if (value === "PASS") return "is-pass";
  if (value === "FAIL") return "is-fail";
  return "is-pending";
}

function publicPillClass(value) {
  if (value === "PUBLISHED") return "is-pub";
  if (value === "OFFLINE") return "is-off";
  return "is-draft";
}

const displayedTasks = computed(() => {
  let list = records.value.slice();
  const keyword = uiFilters.taskKeyword.trim().toLowerCase();
  if (keyword) {
    list = list.filter((task) => `${task.taskTitle || ""} ${task.productName || ""}`.toLowerCase().includes(keyword));
  }
  if (uiFilters.resultFilter === "PASS") list = list.filter((task) => task.samplingResult === "PASS");
  if (uiFilters.resultFilter === "FAIL") list = list.filter((task) => task.samplingResult === "FAIL");
  if (uiFilters.resultFilter === "PENDING") list = list.filter((task) => !task.samplingResult);
  if (uiFilters.publicFilter) {
    if (uiFilters.publicFilter === "DRAFT") {
      list = list.filter((task) => !task.samplingPublicStatus || task.samplingPublicStatus === "DRAFT");
    } else {
      list = list.filter((task) => (task.samplingPublicStatus || "") === uiFilters.publicFilter);
    }
  }
  return list;
});

const totalDisplay = computed(() => {
  const totalCount = Number(total.value) || 0;
  if (!totalCount && records.value.length) return records.value.length;
  return totalCount;
});

const rangeLabel = computed(() => {
  if (!totalDisplay.value) return "0-0";
  const start = (page.value - 1) * size.value + 1;
  const end = Math.min(page.value * size.value, totalDisplay.value);
  return `${start}-${end}`;
});

const tableEmptyText = computed(() => {
  if (records.value.length) return "当前页筛选后暂无匹配抽检任务";
  if (filters.enterpriseName.trim() || filters.status || uiFilters.taskKeyword.trim() || uiFilters.resultFilter || uiFilters.publicFilter) {
    return "当前筛选条件下暂无匹配抽检任务";
  }
  return baseEmptyText;
});

async function loadSamplingTasks() {
  loading.value = true;
  setStatus("");
  try {
    const data = await fetchMySamplingTasks(token.value, {
      ...filters,
      page: page.value,
      size: size.value
    });
    records.value = data.records || [];
    total.value = data.total || 0;
    page.value = data.page || 1;
    size.value = data.size || size.value;
    pages.value = data.pages || 1;
  } catch (error) {
    setStatus(resolveErrorMessage(error, "加载抽检任务失败，请稍后重试。"), "error");
  } finally {
    loading.value = false;
  }
}

async function handleSearch() {
  page.value = 1;
  await loadSamplingTasks();
}

async function resetListFilters() {
  filters.enterpriseName = "";
  filters.status = "";
  uiFilters.taskKeyword = "";
  uiFilters.resultFilter = "";
  uiFilters.publicFilter = "";
  await handleSearch();
}

function toggleResultFilter(value) {
  uiFilters.resultFilter = uiFilters.resultFilter === value ? "" : value;
}

function togglePublicFilter(value) {
  uiFilters.publicFilter = uiFilters.publicFilter === value ? "" : value;
}

async function changePage(nextPage) {
  page.value = nextPage;
  await loadSamplingTasks();
}

function openTaskDetail(task) {
  if (!task?.id) return;
  router.push({
    name: "regulator-enforcer-sampling-detail",
    params: { taskId: task.id }
  }).catch(() => {});
}

function openResultEntry(task) {
  if (!task?.id) return;
  router.push({
    name: "regulator-enforcer-sampling-submit",
    params: { taskId: task.id }
  }).catch(() => {});
}

onMounted(loadSamplingTasks);
</script>

<style scoped>
.filter-bento {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(268px, 300px);
  gap: 14px;
  align-items: stretch;
  margin-bottom: 14px;
}

.filter-panel {
  background: linear-gradient(180deg, #fbfcfe 0%, #ffffff 40%);
  border: 1px solid #e2e8f0;
  border-radius: 10px;
  overflow: hidden;
  box-shadow: 0 4px 16px rgba(15, 23, 42, 0.06);
}

.filter-panel__head {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 16px;
  background: linear-gradient(90deg, #f1f5f9 0%, #f8fafc 100%);
  border-bottom: 1px solid #e8edf3;
}

.filter-panel__accent {
  width: 4px;
  height: 20px;
  border-radius: 2px;
  background: #002660;
  flex-shrink: 0;
}

.filter-panel__title {
  margin: 0;
  font-size: 15px;
  font-weight: 800;
  color: #0f172a;
}

.filter-panel__body {
  padding: 14px 16px 16px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.filter-toolbar {
  display: flex;
  align-items: flex-end;
  gap: 12px;
  flex-wrap: wrap;
}

.filter-grid-3 {
  flex: 1 1 0;
  min-width: 0;
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px 12px;
}

.filter-grid-3 label {
  display: grid;
  gap: 6px;
  font-size: 13px;
  font-weight: 700;
  color: #334155;
}

.filter-grid-3 input,
.filter-grid-3 select {
  min-height: 40px;
  border: 1px solid #dbe2ea;
  background: #fff;
  border-radius: 8px;
  padding: 10px 12px;
  font-size: 14px;
  color: #0f172a;
  box-sizing: border-box;
}

.filter-actions,
.pager-actions,
.action-row {
  display: flex;
  gap: 8px;
}

.filter-actions {
  flex-shrink: 0;
  align-items: center;
  padding-bottom: 1px;
}

.filter-bento__side {
  background: linear-gradient(135deg, #002660, #003a8c);
  color: #fff;
  border-radius: 10px;
  padding: 14px 16px;
  display: flex;
  flex-direction: column;
  gap: 12px;
  box-shadow: 0 12px 24px rgba(0, 38, 96, 0.2);
}

.side-title {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 13px;
  font-weight: 800;
  opacity: 0.95;
}

.side-title-icon {
  opacity: 0.65;
  font-size: 16px;
}

.side-block {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.side-label {
  font-size: 12px;
  font-weight: 700;
  opacity: 0.88;
}

.pill-group {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.pill-group button {
  min-height: 36px;
  border: 0;
  border-radius: 6px;
  padding: 8px 12px;
  background: rgba(255, 255, 255, 0.14);
  color: #fff;
  font-size: 12px;
  font-weight: 700;
  cursor: pointer;
}

.pill-group button.active {
  background: rgba(255, 255, 255, 0.38);
}

.table-card {
  display: grid;
  gap: 12px;
}

.table-wrap {
  overflow: auto;
  border: 1px solid #dbe3ee;
  background: #fff;
}

table {
  width: 100%;
  min-width: 1120px;
  border-collapse: collapse;
}

th,
td {
  padding: 12px 14px;
  border-bottom: 1px solid #eef2f7;
  font-size: 13px;
  text-align: left;
  vertical-align: middle;
}

th {
  background: #f3f6fb;
  color: #64748b;
  font-size: 11px;
  font-weight: 800;
  text-transform: uppercase;
}

.th-center,
.td-center {
  text-align: center;
}

.th-right,
.td-right {
  text-align: right;
}

.mono {
  font-family: ui-monospace, SFMono-Regular, Menlo, Consolas, monospace;
  color: #1e3a8a;
  font-weight: 700;
}

.task-cell {
  display: grid;
  gap: 4px;
}

.task-cell strong {
  color: #0f172a;
}

.task-cell span,
.task-cell small {
  color: #64748b;
  font-size: 12px;
}

.cell-enterprise {
  color: #0f3a72;
  font-weight: 700;
}

.pill {
  display: inline-flex;
  min-height: 24px;
  align-items: center;
  justify-content: center;
  padding: 0 10px;
  border-radius: 999px;
  border: 1px solid transparent;
  font-size: 11px;
  font-weight: 800;
}

.pill--status.is-default {
  background: #f1f5f9;
  border-color: #dbe3ee;
  color: #475569;
}

.pill--status.is-assigned {
  background: #dbeafe;
  border-color: #bfdbfe;
  color: #1e3a8a;
}

.pill--status.is-completed {
  background: #dcfce7;
  border-color: #bbf7d0;
  color: #166534;
}

.pill--status.is-closed {
  background: #fee2e2;
  border-color: #fecaca;
  color: #991b1b;
}

.pill--result.is-pass {
  background: #dcfce7;
  color: #166534;
}

.pill--result.is-fail {
  background: #fee2e2;
  color: #991b1b;
}

.pill--result.is-pending {
  background: #f1f5f9;
  color: #64748b;
}

.pill--pub.is-draft {
  background: #e2e8f0;
  color: #475569;
}

.pill--pub.is-pub {
  background: #c9d7fe;
  color: #1e3a8a;
}

.pill--pub.is-off {
  background: #e2e8f0;
  color: #64748b;
}

.primary,
.ghost,
.op-btn {
  min-height: 34px;
  padding: 0 14px;
  border: 1px solid #cbd5e1;
  font-size: 12px;
  font-weight: 700;
  cursor: pointer;
}

.primary {
  background: #002660;
  border-color: #002660;
  color: #fff;
}

.ghost {
  background: #fff;
  color: #334155;
}

.filter-btn.primary,
.filter-btn.ghost {
  min-height: 40px;
  border-radius: 8px;
  font-size: 14px;
}

.action-row {
  justify-content: flex-end;
}

.table-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  padding: 12px 14px;
  border: 1px solid #dbe3ee;
  background: #fff;
  color: #64748b;
  font-size: 12px;
}

.footer-meta {
  line-height: 1.5;
}

.empty {
  text-align: center;
  color: #64748b;
  padding: 28px 0;
}

.status-banner {
  margin-top: 12px;
  padding: 10px 12px;
  border: 1px solid #dbe3ee;
  background: #f8fafc;
  color: #334155;
}

.status-banner.is-error {
  border-color: #fecaca;
  background: #fef2f2;
  color: #b91c1c;
}

@media (max-width: 1080px) {
  .filter-bento {
    grid-template-columns: 1fr;
  }

  .table-footer {
    flex-direction: column;
    align-items: flex-start;
  }
}

@media (max-width: 760px) {
  .filter-toolbar {
    flex-direction: column;
    align-items: stretch;
  }

  .filter-grid-3 {
    grid-template-columns: 1fr;
  }

  .filter-actions {
    width: 100%;
  }

  .filter-btn {
    flex: 1 1 0;
  }
}
</style>
