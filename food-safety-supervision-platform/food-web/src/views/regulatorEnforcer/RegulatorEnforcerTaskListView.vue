<template>
  <RegulatorEnforcerPageShell
    active-key="tasks"
    title="我的检查任务"
    subtitle="管理并执行分配给您的日常监管与专项检查任务，执行中任务可直接进入检查结果提交页。"
  >
    <section class="filter-grid">
      <article class="filter-card filter-card--wide">
        <label>企业名称搜索</label>
        <div class="search-wrap">
          <span class="material-symbols-outlined">search</span>
          <input v-model.trim="filters.enterpriseName" placeholder="输入企业全称或社会信用代码..." />
        </div>
      </article>
      <article class="filter-card">
        <label>任务状态</label>
        <select v-model="filters.status">
          <option value="">全部状态</option>
          <option value="CREATED">待启动</option>
          <option value="ASSIGNED">已指派</option>
          <option value="IN_PROGRESS">执行中</option>
          <option value="COMPLETED">已完成</option>
          <option value="CLOSED">已归档</option>
        </select>
      </article>
      <article class="filter-card">
        <label>截止时间范围</label>
        <div class="date-range">
          <input v-model="filters.startDate" type="date" />
          <span>至</span>
          <input v-model="filters.endDate" type="date" />
        </div>
      </article>
    </section>

    <div class="toolbar">
      <button class="btn-primary" type="button" :disabled="loading" @click="handleSearch">
        {{ loading ? "查询中..." : "查询任务" }}
      </button>
      <button class="btn-muted" type="button" :disabled="loading" @click="resetFilters">重置筛选</button>
    </div>

    <section class="table-wrap">
      <div class="table-scroll">
        <table>
          <colgroup>
            <col class="col-id" />
            <col class="col-enterprise" />
            <col class="col-type" />
            <col class="col-status" />
            <col class="col-priority" />
            <col class="col-deadline" />
            <col class="col-actions" />
          </colgroup>
          <thead>
            <tr>
              <th>任务 ID</th>
              <th>企业名称</th>
              <th>任务类型</th>
              <th class="center">状态</th>
              <th>紧急程度</th>
              <th>截止时间</th>
              <th class="right">操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-if="!records.length && !loading">
              <td colspan="7" class="empty">暂无任务数据</td>
            </tr>
            <tr v-for="task in records" :key="task.id">
              <td class="mono">{{ task.taskNo || `#${task.id}` }}</td>
              <td>
                <p class="name">{{ task.enterpriseName || "-" }}</p>
                <p class="sub">{{ task.enterpriseCode || "-" }}</p>
              </td>
              <td><span class="type-pill">{{ task.taskType || task.taskTitle || "日常监督检查" }}</span></td>
              <td class="center">
                <span class="status-pill" :class="`is-${String(task.status || '').toLowerCase()}`">
                  {{ formatTaskStatus(task.status) }}
                </span>
              </td>
              <td>
                <div class="priority-line">
                  <i :class="`dot is-${String(task.priority || '').toLowerCase()}`"></i>
                  <span>{{ formatTaskPriority(task.priority) }}</span>
                </div>
              </td>
              <td>{{ formatTime(task.deadline) }}</td>
              <td class="right">
                <div class="row-actions">
                  <button type="button" class="link-btn" @click="openTaskDetailPage(task)">查看详情</button>
                  <button
                    v-if="task.status === 'ASSIGNED' || task.status === 'CREATED'"
                    type="button"
                    class="link-btn link-btn--primary"
                    :disabled="loading"
                    @click="handleStartTask(task)"
                  >
                    开始执行
                  </button>
                  <button
                    v-else-if="task.status === 'IN_PROGRESS'"
                    type="button"
                    class="link-btn link-btn--primary"
                    @click="openSubmitPage(task)"
                  >
                    继续执行
                  </button>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <footer class="table-footer">
        <p>显示 {{ (page - 1) * size + (records.length ? 1 : 0) }}-{{ (page - 1) * size + records.length }} 条记录，共 {{ total }} 条记录</p>
        <div class="page-actions">
          <button class="btn-page" :disabled="page <= 1 || loading" @click="changePage(page - 1)">上一页</button>
          <button class="btn-page is-current">{{ page }}</button>
          <button class="btn-page" :disabled="page >= pages || loading" @click="changePage(page + 1)">下一页</button>
        </div>
      </footer>
    </section>

    <section class="stats-grid">
      <article class="stat-card stat-card--primary">
        <p>当前处理中</p>
        <strong>{{ stats.inProgress }}</strong>
      </article>
      <article class="stat-card">
        <p>高风险待检</p>
        <strong class="danger">{{ stats.highPriority }}</strong>
      </article>
      <article class="stat-card">
        <p>今日已完成</p>
        <strong>{{ stats.completedToday }}</strong>
      </article>
    </section>

    <div v-if="status.message" class="status-banner" :class="`is-${status.type}`">{{ status.message }}</div>
  </RegulatorEnforcerPageShell>
</template>

<script setup>
import { onMounted, reactive, ref } from "vue";
import { useRouter } from "vue-router";
import { fetchMyInspectionTasks, startInspectionTask } from "../../api/regulationOperation";
import { formatTime } from "../../utils/formatters";
import { formatStatusLabel, inspectionTaskStatusMap, taskPriorityMap } from "../../utils/statusMaps";
import { resolveErrorMessage } from "../../utils/uiFeedback";
import RegulatorEnforcerPageShell from "./RegulatorEnforcerPageShell.vue";
import { useRegulatorEnforcerShellSession } from "./regulatorEnforcerShared";

const { token } = useRegulatorEnforcerShellSession();
const router = useRouter();

const loading = ref(false);
const records = ref([]);
const allRecords = ref([]);
const page = ref(1);
const size = ref(8);
const total = ref(0);
const pages = ref(1);
const status = reactive({ message: "", type: "info" });
const filters = reactive({ enterpriseName: "", status: "", startDate: "", endDate: "" });
const stats = reactive({ inProgress: 0, highPriority: 0, completedToday: 0 });

function setStatus(message = "", type = "info") {
  status.message = message;
  status.type = type;
}

function formatTaskStatus(value) {
  return formatStatusLabel(value, inspectionTaskStatusMap);
}

function formatTaskPriority(value) {
  return formatStatusLabel(value, taskPriorityMap);
}

async function loadTasks() {
  loading.value = true;
  setStatus("");
  try {
    allRecords.value = await fetchAllInspectionTasks();
    applyDisplayState();
    updateTaskStats(allRecords.value);
  } catch (error) {
    setStatus(resolveErrorMessage(error, "加载任务列表失败，请稍后重试。"), "error");
  } finally {
    loading.value = false;
  }
}

function isSameDay(dateLike, targetDate = new Date()) {
  if (!dateLike) return false;
  const value = new Date(dateLike);
  if (Number.isNaN(value.getTime())) return false;
  return value.toDateString() === targetDate.toDateString();
}

function resolveCompleteTime(task) {
  return task.completeTime || task.completedTime || task.finishTime || task.updateTime || "";
}

async function fetchAllInspectionTasks() {
  const firstPageData = await fetchMyInspectionTasks(token.value, {
    status: filters.status,
    page: 1,
    size: 100
  });
  const pagesCount = Number(firstPageData?.pages || 1);
  const merged = [...(firstPageData?.records || [])];

  for (let p = 2; p <= pagesCount && p <= 20; p += 1) {
    const pageData = await fetchMyInspectionTasks(token.value, {
      status: filters.status,
      page: p,
      size: 100
    });
    merged.push(...(pageData?.records || []));
  }

  return merged;
}

function updateTaskStats(all) {
  const today = new Date();
  stats.inProgress = all.filter((item) => item.status === "IN_PROGRESS").length;
  stats.highPriority = all.filter((item) => item.priority === "HIGH").length;
  stats.completedToday = all.filter((item) => item.status === "COMPLETED" && isSameDay(resolveCompleteTime(item), today)).length;
}

function applyDisplayState() {
  const filtered = applyTaskFilters(allRecords.value);
  total.value = filtered.length;
  pages.value = Math.max(1, Math.ceil(filtered.length / size.value));
  if (page.value > pages.value) {
    page.value = pages.value;
  }
  const start = (page.value - 1) * size.value;
  records.value = filtered.slice(start, start + size.value);
}

function normalizeDateOnly(value) {
  if (!value) return "";
  return String(value).slice(0, 10);
}

function applyTaskFilters(list) {
  const keyword = String(filters.enterpriseName || "").trim().toLowerCase();
  const start = normalizeDateOnly(filters.startDate);
  const end = normalizeDateOnly(filters.endDate);

  return list.filter((item) => {
    const enterpriseName = String(item.enterpriseName || "").toLowerCase();
    const deadline = normalizeDateOnly(item.deadline);

    if (keyword && !enterpriseName.includes(keyword)) return false;
    if (filters.status && item.status !== filters.status) return false;
    if (start && deadline && deadline < start) return false;
    if (end && deadline && deadline > end) return false;
    if ((start || end) && !deadline) return false;
    return true;
  });
}

async function handleSearch() {
  if (filters.startDate && filters.endDate && filters.startDate > filters.endDate) {
    setStatus("截止时间范围不合法：开始日期不能晚于结束日期。", "error");
    return;
  }
  page.value = 1;
  await loadTasks();
}

async function changePage(nextPage) {
  page.value = nextPage;
  applyDisplayState();
}

function resetFilters() {
  filters.enterpriseName = "";
  filters.status = "";
  filters.startDate = "";
  filters.endDate = "";
  handleSearch();
}

async function handleStartTask(task) {
  if (!task?.id) return;
  loading.value = true;
  setStatus("");
  try {
    await startInspectionTask(token.value, task.id);
    setStatus("任务已开始执行", "success");
    await loadTasks();
  } catch (error) {
    setStatus(resolveErrorMessage(error, "开始任务失败，请稍后重试。"), "error");
  } finally {
    loading.value = false;
  }
}

function openSubmitPage(task) {
  if (!task?.id) return;
  router.push({
    name: "regulator-enforcer-task-submit",
    params: { taskId: task.id }
  }).catch(() => {});
}

function openTaskDetailPage(task) {
  if (!task?.id) return;
  router.push({
    name: "regulator-enforcer-task-detail",
    params: { taskId: task.id },
    query: { from: "tasks" }
  }).catch(() => {});
}

onMounted(() => {
  loadTasks();
});
</script>

<style scoped>
.filter-grid {
  display: grid;
  grid-template-columns: 2fr 1fr 1fr;
  gap: 12px;
  margin-bottom: 12px;
}
.filter-card {
  background: #fff;
  border: 1px solid #dbe3ee;
  border-radius: 8px;
  padding: 12px;
}
.filter-card label {
  display: block;
  margin-bottom: 8px;
  font-size: 11px;
  font-weight: 800;
  color: #64748b;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}
.search-wrap {
  position: relative;
}
.search-wrap .material-symbols-outlined {
  position: absolute;
  left: 10px;
  top: 7px;
  color: #94a3b8;
  font-size: 18px;
}
.filter-card input,
.filter-card select {
  width: 100%;
  height: 34px;
  border: 1px solid #d4dce8;
  border-radius: 4px;
  padding: 0 10px;
  background: #f8fafc;
}
.search-wrap input {
  padding-left: 34px;
}
.date-range {
  display: grid;
  grid-template-columns: 1fr auto 1fr;
  align-items: center;
  gap: 8px;
}
.date-range span {
  color: #64748b;
  font-size: 12px;
}
.toolbar {
  display: flex;
  gap: 10px;
  margin-bottom: 14px;
}
.btn-primary,
.btn-muted,
.btn-page {
  min-height: 36px;
  padding: 0 14px;
  border-radius: 6px;
  border: 1px solid #cbd5e1;
  cursor: pointer;
  font-weight: 800;
}
.btn-primary {
  background: #002660;
  border-color: #002660;
  color: #fff;
}
.btn-muted,
.btn-page {
  background: #fff;
  color: #334155;
}
.table-wrap {
  border: 1px solid #dbe3ee;
  border-radius: 10px;
  background: #fff;
  overflow: hidden;
}
.table-scroll {
  overflow-x: auto;
}
table {
  width: 100%;
  border-collapse: collapse;
}
th,
td {
  padding: 14px 16px;
  border-bottom: 1px solid #e2e8f0;
  font-size: 13px;
  text-align: left;
  vertical-align: middle;
}
th {
  color: #64748b;
  font-size: 11px;
  font-weight: 800;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  background: #f8fafc;
}
.center {
  text-align: center;
}
.right {
  text-align: right;
}
.mono {
  font-family: Consolas, "Courier New", monospace;
}
.name {
  margin: 0;
  color: #0f172a;
  font-weight: 700;
}
.sub {
  margin: 4px 0 0;
  color: #64748b;
  font-size: 12px;
}
.type-pill {
  display: inline-flex;
  align-items: center;
  padding: 4px 8px;
  border-radius: 999px;
  background: #eef2ff;
  color: #3730a3;
  font-size: 12px;
  font-weight: 700;
}
.status-pill {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 76px;
  padding: 4px 8px;
  border-radius: 999px;
  background: #e2e8f0;
  color: #334155;
  font-size: 12px;
  font-weight: 800;
}
.status-pill.is-in_progress {
  background: #dbeafe;
  color: #1d4ed8;
}
.status-pill.is-completed,
.status-pill.is-closed {
  background: #dcfce7;
  color: #166534;
}
.priority-line {
  display: inline-flex;
  align-items: center;
  gap: 8px;
}
.dot {
  width: 8px;
  height: 8px;
  border-radius: 999px;
  background: #94a3b8;
}
.dot.is-high {
  background: #dc2626;
}
.dot.is-medium {
  background: #f59e0b;
}
.dot.is-low {
  background: #16a34a;
}
.row-actions {
  display: inline-flex;
  gap: 10px;
  flex-wrap: wrap;
  justify-content: flex-end;
}
.link-btn {
  border: 0;
  padding: 0;
  background: transparent;
  color: #475569;
  font-size: 13px;
  font-weight: 700;
  cursor: pointer;
}
.link-btn--primary {
  color: #003a8c;
}
.empty {
  text-align: center;
  color: #94a3b8;
}
.table-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  background: #f8fafc;
}
.table-footer p {
  margin: 0;
  color: #64748b;
  font-size: 12px;
}
.page-actions {
  display: flex;
  gap: 8px;
}
.btn-page.is-current {
  background: #002660;
  border-color: #002660;
  color: #fff;
}
.stats-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
  margin-top: 14px;
}
.stat-card {
  border: 1px solid #dbe3ee;
  border-radius: 10px;
  background: #fff;
  padding: 16px;
}
.stat-card p {
  margin: 0;
  color: #64748b;
  font-size: 12px;
}
.stat-card strong {
  display: block;
  margin-top: 8px;
  color: #0f172a;
  font-size: 28px;
  font-weight: 900;
}
.stat-card--primary {
  background: linear-gradient(135deg, #002660, #0f4c81);
  border-color: #002660;
}
.stat-card--primary p,
.stat-card--primary strong {
  color: #fff;
}
.danger {
  color: #b91c1c;
}
.status-banner {
  margin-top: 14px;
  border: 1px solid #bfdbfe;
  background: #eff6ff;
  color: #1d4ed8;
  border-radius: 10px;
  padding: 12px 14px;
  font-size: 14px;
}
.status-banner.is-success {
  border-color: #bbf7d0;
  background: #f0fdf4;
  color: #166534;
}
.status-banner.is-error {
  border-color: #fecaca;
  background: #fef2f2;
  color: #b91c1c;
}
@media (max-width: 1100px) {
  .filter-grid,
  .stats-grid {
    grid-template-columns: 1fr;
  }
  .table-footer {
    flex-direction: column;
    align-items: stretch;
  }
}
</style>
