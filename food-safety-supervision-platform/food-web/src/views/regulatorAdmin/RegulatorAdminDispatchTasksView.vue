<template>
  <RegulatorAdminWorkspacePage
    active-key="dispatch"
    :username="regulatorUser.username"
    @navigate="handleSidebarNavigate"
    @logout="handleLogout"
    @pending-feature="onPendingFeature"
  >
    <section class="dispatch-page">
      <nav class="dispatch-subnav">
        <button type="button" class="active">任务列表</button>
        <button type="button" @click="goToDispatchRecords">检查记录</button>
      </nav>

      <header class="dispatch-page__head">
        <div>
          <h1>检查任务列表</h1>
          <p>发起并派发检查任务，跟踪执行进度与归档状态。</p>
        </div>
        <button class="primary head-create-btn" type="button" @click="goToDispatchCreate">新建检查任务</button>
      </header>

      <section class="filter-panel">
        <label>
          任务状态
          <select v-model="dispatchFilters.status">
            <option value="">全部状态</option>
            <option value="CREATED">已创建</option>
            <option value="ASSIGNED">已派发</option>
            <option value="IN_PROGRESS">执行中</option>
            <option value="COMPLETED">已完成</option>
            <option value="CLOSED">已关闭</option>
          </select>
        </label>
        <label>
          截止日期范围
          <div class="date-range">
            <input v-model="uiFilters.deadlineStart" type="date" />
            <span>至</span>
            <input v-model="uiFilters.deadlineEnd" type="date" />
          </div>
        </label>
        <label>
          指派人员
          <input v-model.trim="uiFilters.assigneeKeyword" placeholder="搜索人员姓名" />
        </label>
        <button class="ghost" type="button" :disabled="dispatchTaskLoading" @click="handleDispatchSearch">
          {{ dispatchTaskLoading ? "查询中..." : "查询" }}
        </button>
        <button class="ghost link-reset" type="button" :disabled="dispatchTaskLoading" @click="resetFilters">
          重置筛选
        </button>
      </section>

      <section class="stats-grid">
        <article class="stat-card stat-card--primary">
          <span>检查任务总数</span>
          <strong>{{ dispatchTotal }}</strong>
          <p>按当前筛选条件统计的任务总量</p>
        </article>
        <article class="stat-card">
          <span>执行中任务</span>
          <strong>{{ inProgressCount }}</strong>
          <p>基于筛选后全量任务统计</p>
        </article>
        <article class="stat-card">
          <span>逾期任务</span>
          <strong>{{ overdueCount }}</strong>
          <p>截止日期已到且尚未办结</p>
        </article>
        <article class="stat-card">
          <span>办结率</span>
          <strong>{{ completionRate }}%</strong>
          <p>已完成和已归档任务占比</p>
        </article>
      </section>

      <div class="dispatch-layout">
        <section class="panel list-panel">
          <div class="panel__title">任务列表</div>
          <div class="table-wrap">
            <table>
              <thead>
                <tr>
                  <th>任务号</th>
                  <th>任务名称</th>
                  <th>企业</th>
                  <th>指派人员</th>
                  <th>截止日期</th>
                  <th>状态</th>
                  <th>进度</th>
                  <th>操作</th>
                </tr>
              </thead>
              <tbody v-if="dispatchTasks.length">
                <tr v-for="task in dispatchTasks" :key="task.id">
                  <td class="mono">{{ task.taskNo || "-" }}</td>
                  <td class="task-title">{{ task.taskTitle || "-" }}</td>
                  <td>{{ task.enterpriseName || "-" }}</td>
                  <td>{{ task.assignedToName || "-" }}</td>
                  <td>{{ formatTime(task.deadline) }}</td>
                  <td>
                    <span class="status-pill" :class="statusClass(task.status)">
                      {{ formatTaskStatus(task.status) }}
                    </span>
                  </td>
                  <td>
                    <div class="progress-wrap">
                      <div class="progress-track">
                        <div class="progress-fill" :style="{ width: `${taskProgress(task)}%` }"></div>
                      </div>
                      <span>{{ taskProgress(task) }}%</span>
                    </div>
                  </td>
                  <td>
                    <div class="action-row">
                      <button class="op-btn op-btn--detail" type="button" @click="openTaskDetail(task)">详情</button>
                      <template v-if="isTaskAssignable(task) && !isTaskDeadlineExceeded(task.deadline)">
                        <select class="op-select" v-model="taskAssignments[task.id]" :disabled="dispatchTaskLoading">
                          <option value="">选择执法人员</option>
                          <option v-for="item in getEnforcers(task.regionId)" :key="item.id" :value="item.id">
                            {{ item.name }}
                          </option>
                        </select>
                        <button
                          class="op-btn op-btn--assign"
                          type="button"
                          :disabled="dispatchTaskLoading"
                          @click="handleAssignTask(task)"
                        >
                          派发
                        </button>
                      </template>
                      <span
                        v-else-if="isTaskAssignable(task) && isTaskDeadlineExceeded(task.deadline)"
                        class="op-expired-tip"
                      >
                        已截止
                      </span>
                      <button
                        v-if="isTaskClosable(task)"
                        class="op-btn op-btn--archive"
                        type="button"
                        :disabled="dispatchTaskLoading"
                        @click="handleCloseTask(task)"
                      >
                        归档
                      </button>
                    </div>
                  </td>
                </tr>
              </tbody>
            </table>
            <div v-if="!dispatchTasks.length" class="empty">暂无任务</div>
          </div>

          <div class="pager">
            <span>共 {{ dispatchTotal }} 条，{{ dispatchPage }}/{{ dispatchPages }} 页</span>
            <div class="pager-actions">
              <button class="ghost" type="button" :disabled="dispatchPage <= 1" @click="changeDispatchPage(dispatchPage - 1)">
                上一页
              </button>
              <button
                class="ghost"
                type="button"
                :disabled="dispatchPage >= dispatchPages"
                @click="changeDispatchPage(dispatchPage + 1)"
              >
                下一页
              </button>
            </div>
          </div>
        </section>
      </div>

      <div v-if="detailTask" class="modal-mask" @click.self="closeTaskDetail">
        <div class="modal-card">
          <div class="modal-title">任务详情</div>
          <div class="modal-grid">
            <article><span>任务号</span><strong>{{ detailTask.taskNo || "-" }}</strong></article>
            <article><span>任务标题</span><strong>{{ detailTask.taskTitle || "-" }}</strong></article>
            <article><span>状态</span><strong>{{ formatTaskStatus(detailTask.status) }}</strong></article>
            <article><span>优先级</span><strong>{{ formatTaskPriority(detailTask.priority) }}</strong></article>
            <article><span>负责人</span><strong>{{ detailTask.assignedToName || "-" }}</strong></article>
            <article><span>截止时间</span><strong>{{ formatTime(detailTask.deadline) }}</strong></article>
            <article class="span2"><span>任务描述</span><strong>{{ detailTask.taskDesc || "暂无任务描述" }}</strong></article>
            <article class="span2">
              <span>企业信息</span>
              <strong>
                {{ detailTaskEnterprise?.enterpriseName || detailTask.enterpriseName || "-" }}
                /
                {{ detailTaskRegionName || "-" }}
              </strong>
            </article>
          </div>
          <div class="modal-actions">
            <button class="ghost" type="button" @click="closeTaskDetail">关闭</button>
          </div>
        </div>
      </div>

      <div v-if="status.message" class="status" :class="status.type">{{ status.message }}</div>
    </section>
  </RegulatorAdminWorkspacePage>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from "vue";
import { useRouter } from "vue-router";
import {
  assignInspectionTask,
  closeInspectionTask,
  fetchInspectionTasks
} from "../../api/regulationOperation";
import {
  fetchEligibleRegulators,
  fetchEnterpriseDetail,
  fetchRegionPath
} from "../../api/regulation";
import RegulatorAdminWorkspacePage from "../../components/regulatorAdmin/RegulatorAdminWorkspacePage.vue";
import { formatTime } from "../../utils/formatters";
import {
  regulatorFeaturePendingNotice,
  useRegulatorAdminShellSession
} from "./regulatorAdminShared";

const router = useRouter();
const { regulatorUser, token, handleSidebarNavigate, handleLogout } = useRegulatorAdminShellSession();
const status = reactive({ message: "", type: "" });

const dispatchTaskLoading = ref(false);
const dispatchTasks = ref([]);
const allDispatchTasks = ref([]);
const dispatchPage = ref(1);
const dispatchSize = ref(8);
const dispatchTotal = ref(0);
const dispatchPages = ref(1);
const dispatchFilters = reactive({ enterpriseName: "", status: "" });
const uiFilters = reactive({
  deadlineStart: "",
  deadlineEnd: "",
  assigneeKeyword: ""
});
const enforcerMap = reactive({});
const taskAssignments = reactive({});

const detailTask = ref(null);
const detailTaskEnterprise = ref(null);
const detailTaskRegionName = ref("-");

const taskStatusMap = {
  CREATED: "待派发",
  ASSIGNED: "已派发",
  IN_PROGRESS: "执行中",
  COMPLETED: "已完成",
  CLOSED: "已归档"
};

const taskPriorityMap = {
  LOW: "低",
  MEDIUM: "中",
  HIGH: "高"
};

const inProgressCount = computed(() =>
  allDispatchTasks.value.filter((item) => item.status === "IN_PROGRESS").length
);

const overdueCount = computed(() =>
  allDispatchTasks.value.filter(
    (item) =>
      isTaskDeadlineExceeded(item.deadline) &&
      item.status !== "COMPLETED" &&
      item.status !== "CLOSED"
  ).length
);

const completionRate = computed(() => {
  if (!dispatchTotal.value) return 0;
  const completed = allDispatchTasks.value.filter(
    (item) => item.status === "COMPLETED" || item.status === "CLOSED"
  ).length;
  return Math.round((completed / dispatchTotal.value) * 100);
});

function onPendingFeature(title) {
  regulatorFeaturePendingNotice(title);
}

function goToDispatchRecords() {
  router.push({ name: "regulator-admin-dispatch-records" });
}

function goToDispatchCreate() {
  router.push({ name: "regulator-admin-dispatch-create" });
}

function setStatus(message, type = "info") {
  status.message = message;
  status.type = type;
}

function formatTaskStatus(value) {
  return taskStatusMap[value] || value || "-";
}

function formatTaskPriority(value) {
  return taskPriorityMap[value] || value || "-";
}

function statusClass(value) {
  if (value === "IN_PROGRESS") return "is-danger";
  if (value === "ASSIGNED") return "is-assigned";
  if (value === "COMPLETED" || value === "CLOSED") return "is-success";
  return "is-default";
}

function taskProgress(task) {
  const map = {
    CREATED: 0,
    ASSIGNED: 5,
    IN_PROGRESS: 40,
    COMPLETED: 100,
    CLOSED: 100
  };
  return map[task?.status] ?? 0;
}

function isTaskAssignable(task) {
  return ["CREATED", "ASSIGNED"].includes(task.status);
}

function isTaskClosable(task) {
  return task.status === "COMPLETED";
}

function isTaskDeadlineExceeded(deadline) {
  if (!deadline) return false;
  const deadlineMs = new Date(deadline).getTime();
  if (Number.isNaN(deadlineMs)) return false;
  return deadlineMs <= Date.now();
}

function getEnforcers(regionId) {
  if (!regionId) return [];
  return enforcerMap[regionId] || [];
}

function normalizeName(value) {
  return String(value || "").trim().toLowerCase();
}

function parseFilterDate(value, endOfDay = false) {
  if (!value) return null;
  const suffix = endOfDay ? "T23:59:59.999" : "T00:00:00.000";
  const result = new Date(`${value}${suffix}`);
  return Number.isNaN(result.getTime()) ? null : result;
}

function taskMatchesUiFilters(task) {
  const assigneeKeyword = normalizeName(uiFilters.assigneeKeyword);
  if (assigneeKeyword) {
    const assigneeName = normalizeName(task?.assignedToName);
    if (!assigneeName.includes(assigneeKeyword)) {
      return false;
    }
  }

  const startDate = parseFilterDate(uiFilters.deadlineStart, false);
  const endDate = parseFilterDate(uiFilters.deadlineEnd, true);
  if (!startDate && !endDate) {
    return true;
  }

  if (!task?.deadline) {
    return false;
  }

  const deadline = new Date(task.deadline);
  if (Number.isNaN(deadline.getTime())) {
    return false;
  }

  if (startDate && deadline < startDate) {
    return false;
  }

  if (endDate && deadline > endDate) {
    return false;
  }

  return true;
}

async function ensureEnforcers(regionId) {
  if (!regionId || enforcerMap[regionId]) return;
  try {
    const data = await fetchEligibleRegulators(token.value, regionId);
    enforcerMap[regionId] = Array.isArray(data) ? data : [];
  } catch {
    enforcerMap[regionId] = [];
  }
}

async function fetchAllDispatchTasks() {
  const size = 100;
  let page = 1;
  let pages = 1;
  const records = [];

  do {
    const data = await fetchInspectionTasks(token.value, {
      enterpriseName: dispatchFilters.enterpriseName,
      status: dispatchFilters.status,
      page,
      size
    });
    const pageRecords = Array.isArray(data?.records) ? data.records : [];
    records.push(...pageRecords);
    pages = Number(data?.pages || 1);
    page += 1;
  } while (page <= pages);

  return records;
}

async function applyDispatchResult(tasks) {
  const filteredTasks = tasks.filter(taskMatchesUiFilters);
  allDispatchTasks.value = filteredTasks;
  dispatchTotal.value = filteredTasks.length;
  dispatchPages.value = Math.max(1, Math.ceil(filteredTasks.length / dispatchSize.value));
  if (dispatchPage.value > dispatchPages.value) {
    dispatchPage.value = dispatchPages.value;
  }
  const start = (dispatchPage.value - 1) * dispatchSize.value;
  const end = start + dispatchSize.value;
  dispatchTasks.value = filteredTasks.slice(start, end);

  const regionIds = [...new Set(dispatchTasks.value.map((task) => task.regionId).filter(Boolean))];
  await Promise.all(regionIds.map((id) => ensureEnforcers(id)));
}

async function loadDispatchTasks() {
  dispatchTaskLoading.value = true;
  setStatus("");
  try {
    const tasks = await fetchAllDispatchTasks();
    await applyDispatchResult(tasks);
  } catch (error) {
    setStatus(error.message || "加载任务列表失败", "error");
  } finally {
    dispatchTaskLoading.value = false;
  }
}

async function handleDispatchSearch() {
  dispatchPage.value = 1;
  await loadDispatchTasks();
}

async function resetFilters() {
  dispatchFilters.enterpriseName = "";
  dispatchFilters.status = "";
  uiFilters.deadlineStart = "";
  uiFilters.deadlineEnd = "";
  uiFilters.assigneeKeyword = "";
  dispatchPage.value = 1;
  await loadDispatchTasks();
}

async function changeDispatchPage(nextPage) {
  dispatchPage.value = nextPage;
  await applyDispatchResult(allDispatchTasks.value);
}

async function handleAssignTask(task) {
  if (isTaskDeadlineExceeded(task?.deadline)) {
    return setStatus("任务已超期，无法派发", "error");
  }
  const regulatorId = taskAssignments[task.id];
  if (!regulatorId) {
    return setStatus("请选择执法人员后再派发", "error");
  }
  dispatchTaskLoading.value = true;
  try {
    await assignInspectionTask(token.value, task.id, { regulatorId });
    setStatus("任务已派发", "success");
    await loadDispatchTasks();
  } catch (error) {
    setStatus(error.message || "任务派发失败", "error");
  } finally {
    dispatchTaskLoading.value = false;
  }
}

async function handleCloseTask(task) {
  if (!task?.id) return;
  dispatchTaskLoading.value = true;
  try {
    await closeInspectionTask(token.value, task.id);
    setStatus("任务已归档", "success");
    await loadDispatchTasks();
  } catch (error) {
    setStatus(error.message || "关闭任务失败", "error");
  } finally {
    dispatchTaskLoading.value = false;
  }
}

async function openTaskDetail(task) {
  if (!task) return;
  detailTask.value = task;
  detailTaskEnterprise.value = null;
  detailTaskRegionName.value = "-";
  if (!task.enterpriseId) return;
  try {
    const enterprise = await fetchEnterpriseDetail(token.value, task.enterpriseId);
    detailTaskEnterprise.value = enterprise || null;
    if (enterprise?.regionId) {
      const path = await fetchRegionPath(token.value, enterprise.regionId).catch(() => []);
      detailTaskRegionName.value = Array.isArray(path) && path.length
        ? path.map((item) => item.name).join("/")
        : "-";
    }
  } catch (error) {
    setStatus(error.message || "加载企业信息失败", "error");
  }
}

function closeTaskDetail() {
  detailTask.value = null;
  detailTaskEnterprise.value = null;
  detailTaskRegionName.value = "-";
}

onMounted(async () => {
  await loadDispatchTasks();
});
</script>

<style scoped>
.dispatch-page { display: grid; gap: 16px; }
.dispatch-subnav { display: inline-flex; align-items: center; gap: 8px; }
.dispatch-subnav button {
  min-height: 30px;
  border-radius: 4px;
  border: 1px solid #d7e1ec;
  background: #fff;
  color: #516377;
  font-size: 12px;
  font-weight: 700;
  padding: 0 12px;
  cursor: pointer;
}
.dispatch-subnav button.active { border-color: #bfd2ea; background: #eaf2fd; color: #0f3a72; }
.dispatch-page__head { display: flex; justify-content: space-between; align-items: end; gap: 10px; }
.dispatch-page__head h1 { margin: 0; color: #002660; font-size: 30px; font-weight: 800; }
.dispatch-page__head p { margin: 6px 0 0; color: #64748b; }
.head-create-btn { min-height: 40px; padding: 0 16px; border-radius: 8px; white-space: nowrap; }
.filter-panel {
  background: #edf2f7;
  border-radius: 10px;
  padding: 12px;
  display: grid;
  grid-template-columns: 1fr 1.4fr 1fr auto auto;
  gap: 10px;
  align-items: end;
}
.filter-panel label { display: grid; gap: 6px; font-size: 12px; color: #64748b; font-weight: 700; }
.filter-panel input,
.filter-panel select { border: 0; background: #fff; border-radius: 8px; padding: 9px 10px; color: #1e293b; }
.date-range { display: grid; grid-template-columns: 1fr auto 1fr; gap: 8px; align-items: center; }
.date-range span { color: #64748b; font-size: 12px; }
.stats-grid { display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); gap: 10px; }
.stat-card { background: #fff; border: 1px solid #e2e8f0; border-radius: 10px; padding: 12px; }
.stat-card span { color: #64748b; font-size: 11px; text-transform: uppercase; font-weight: 700; }
.stat-card strong { display: block; margin-top: 8px; font-size: 30px; color: #0f172a; }
.stat-card p { margin: 6px 0 0; color: #64748b; font-size: 12px; }
.stat-card--primary { background: linear-gradient(120deg, #002660, #003a8c); border: 0; }
.stat-card--primary span,
.stat-card--primary strong,
.stat-card--primary p { color: #fff; }
.dispatch-layout { display: grid; gap: 16px; }
.panel { background: #fff; border: 1px solid #e2e8f0; border-radius: 8px; padding: 14px; }
.panel__title { font-size: 14px; font-weight: 800; color: #0f172a; margin-bottom: 10px; }
.primary {
  border: 0;
  background: #002660;
  color: #fff;
  border-radius: 6px;
  padding: 9px 12px;
  font-size: 12px;
  font-weight: 700;
  cursor: pointer;
}
.table-wrap { border: 1px solid #e2e8f0; border-radius: 8px; overflow: auto; }
table { width: 100%; min-width: 1080px; border-collapse: collapse; }
th { text-align: left; padding: 10px; background: #f8fafc; color: #64748b; font-size: 11px; text-transform: uppercase; }
td { padding: 10px; border-top: 1px solid #edf2f7; font-size: 13px; color: #1e293b; vertical-align: middle; }
.mono { font-family: ui-monospace, SFMono-Regular, Menlo, Consolas, monospace; }
.task-title { font-weight: 700; color: #002660; }
.status-pill { display: inline-flex; padding: 3px 8px; border-radius: 4px; font-size: 11px; font-weight: 700; }
.status-pill.is-default { background: #e2e8f0; color: #334155; }
.status-pill.is-assigned { background: #dbeafe; color: #1d4ed8; }
.status-pill.is-danger { background: #fee2e2; color: #991b1b; }
.status-pill.is-success { background: #dcfce7; color: #166534; }
.progress-wrap { display: flex; align-items: center; gap: 8px; }
.progress-track { width: 80px; height: 6px; background: #e2e8f0; border-radius: 999px; overflow: hidden; }
.progress-fill { height: 100%; background: #2563eb; }
.progress-wrap span { font-size: 11px; font-weight: 700; color: #334155; }
.action-row {
  display: flex;
  gap: 6px;
  align-items: center;
  flex-wrap: wrap;
  padding: 4px;
  border-radius: 8px;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
}
.op-btn {
  border: 1px solid #cbd5e1;
  background: #fff;
  color: #334155;
  border-radius: 6px;
  padding: 5px 10px;
  min-height: 28px;
  font-size: 12px;
  font-weight: 700;
  line-height: 1;
  cursor: pointer;
  transition: all 0.15s ease;
}
.op-btn:hover:not(:disabled) { transform: translateY(-1px); box-shadow: 0 4px 10px rgba(15, 23, 42, 0.08); }
.op-btn:disabled { cursor: not-allowed; opacity: 0.55; }
.op-btn--detail { color: #1e3a8a; border-color: #bfdbfe; background: #eff6ff; }
.op-btn--detail:hover:not(:disabled) { background: #dbeafe; }
.op-btn--assign { color: #fff; border-color: #1d4ed8; background: #1d4ed8; }
.op-btn--assign:hover:not(:disabled) { background: #1e40af; border-color: #1e40af; }
.op-btn--archive { color: #166534; border-color: #86efac; background: #f0fdf4; }
.op-btn--archive:hover:not(:disabled) { background: #dcfce7; }
.op-expired-tip {
  display: inline-flex;
  align-items: center;
  min-height: 28px;
  padding: 0 10px;
  border-radius: 6px;
  border: 1px solid #fecaca;
  background: #fef2f2;
  color: #b91c1c;
  font-size: 12px;
  font-weight: 700;
  white-space: nowrap;
}
.op-select {
  border: 1px solid #cbd5e1;
  background: #fff;
  border-radius: 6px;
  padding: 5px 8px;
  min-height: 28px;
  min-width: 118px;
  font-size: 12px;
  color: #334155;
}
.ghost {
  border: 1px solid #d1d5db;
  background: #fff;
  color: #334155;
  border-radius: 6px;
  padding: 6px 10px;
  font-size: 12px;
  cursor: pointer;
}
.link-reset {
  border-color: transparent;
  background: transparent;
  color: #475569;
  text-decoration: underline;
  text-underline-offset: 3px;
}
.pager { margin-top: 10px; display: flex; justify-content: space-between; align-items: center; font-size: 12px; color: #64748b; }
.pager-actions { display: flex; gap: 8px; }
.empty { padding: 14px; color: #64748b; font-size: 13px; }
.modal-mask { position: fixed; inset: 0; background: rgba(15, 23, 42, 0.4); display: grid; place-items: center; z-index: 1000; }
.modal-card { width: min(760px, 94vw); background: #fff; border-radius: 10px; padding: 14px; }
.modal-title { font-size: 16px; font-weight: 800; color: #0f172a; margin-bottom: 10px; }
.modal-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 10px; }
.modal-grid article { background: #f8fafc; border-radius: 6px; padding: 10px; display: grid; gap: 4px; }
.modal-grid article.span2 { grid-column: span 2; }
.modal-grid span { color: #64748b; font-size: 11px; text-transform: uppercase; font-weight: 700; }
.modal-grid strong { color: #1e293b; font-size: 13px; }
.modal-actions { margin-top: 10px; display: flex; justify-content: flex-end; }
.status { position: fixed; right: 18px; bottom: 18px; border-radius: 8px; padding: 10px 12px; color: #fff; background: #0f172a; font-size: 13px; }
.status.error { background: #b91c1c; }
.status.success { background: #166534; }

@media (max-width: 1200px) {
  .filter-panel { grid-template-columns: 1fr 1fr; }
  .stats-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); }
}

@media (max-width: 900px) {
  .dispatch-page__head,
  .filter-panel,
  .modal-grid {
    grid-template-columns: 1fr;
  }

  .modal-grid article.span2 {
    grid-column: span 1;
  }
}
</style>
