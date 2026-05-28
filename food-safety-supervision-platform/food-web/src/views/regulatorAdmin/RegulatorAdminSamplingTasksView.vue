<template>
  <RegulatorAdminWorkspacePage
    active-key="sampling"
    :username="regulatorUser.username"
    @navigate="handleSidebarNavigate"
    @logout="handleLogout"
  >
    <section class="sampling-page">
      <header class="sampling-page__head">
        <div>
          <nav class="crumbs">
            <span>监管</span>
            <span class="crumbs-sep">/</span>
            <span class="is-current">抽检任务</span>
          </nav>
          <h1>抽检任务列表</h1>
          <p>查看、分派、公示并归档抽检任务。</p>
        </div>
        <button class="primary head-create-btn" type="button" @click="goToCreateSampling">新建抽检任务</button>
      </header>

      <div class="filter-bento">
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
                  <input v-model.trim="uiFilters.taskKeyword" placeholder="输入任务关键词..." />
                </label>
                <label>
                  相关企业
                  <input v-model.trim="samplingFilters.enterpriseName" placeholder="搜索企业名称..." />
                </label>
                <label>
                  执行状态
                  <select v-model="samplingFilters.status">
                    <option value="">全部状态</option>
                    <option value="CREATED">已创建</option>
                    <option value="ASSIGNED">已指派</option>
                    <option value="COMPLETED">已完成</option>
                    <option value="CLOSED">已关闭</option>
                  </select>
                </label>
              </div>
              <div class="filter-actions">
                <button class="primary filter-btn" type="button" :disabled="samplingTaskLoading" @click="handleSamplingSearch">
                  {{ samplingTaskLoading ? "查询中..." : "查询" }}
                </button>
                <button class="ghost filter-btn" type="button" @click="resetListFilters">重置</button>
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
      </div>

      <div class="table-wrap">
        <table>
          <thead>
            <tr>
              <th>任务 ID</th>
              <th>样品名称/项目</th>
              <th>所属企业</th>
              <th>执法人员</th>
              <th class="th-center">执行状态</th>
              <th class="th-center">检测结果</th>
              <th class="th-center">公示状态</th>
              <th class="th-right">操作</th>
            </tr>
          </thead>
          <tbody v-if="displayedTasks.length">
            <tr v-for="task in displayedTasks" :key="task.id">
              <td class="mono">{{ task.taskNo || "-" }}</td>
              <td>
                <div class="cell-title">{{ task.taskTitle || task.productName || "-" }}</div>
                <div class="cell-sub">规格: {{ task.productSpecification || "-" }}</div>
              </td>
              <td>
                <div class="cell-enterprise">
                  <span class="ent-icon">●</span>
                  <span>{{ task.enterpriseName || "-" }}</span>
                </div>
              </td>
              <td>{{ task.assignedToName || "-" }}</td>
              <td class="td-center">
                <span class="pill pill--status" :class="statusPillClass(task.status)">{{ formatSamplingTaskStatus(task.status) }}</span>
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
              <td class="td-right">
                <div class="action-row">
                  <button class="op-btn op-btn--detail" type="button" @click="goToSamplingDetail(task)">查看详情</button>
                  <template v-if="isSamplingTaskAssignable(task) && !isTaskDeadlineExceeded(task.deadline)">
                    <select class="op-select" v-model="samplingAssignments[task.id]" :disabled="samplingTaskLoading">
                      <option value="">选择执法人员</option>
                      <option v-for="item in getEnforcers(task.regionId)" :key="item.id" :value="item.id">
                        {{ item.name }}
                      </option>
                    </select>
                    <button class="op-btn op-btn--assign" type="button" :disabled="samplingTaskLoading" @click="handleAssignSamplingTask(task)">派发</button>
                  </template>
                  <span v-else-if="isSamplingTaskAssignable(task) && isTaskDeadlineExceeded(task.deadline)" class="op-expired-tip">已截止</span>
                  <button
                    v-if="task.samplingResultId && task.samplingPublicStatus !== 'PUBLISHED'"
                    class="op-btn op-btn--pub"
                    type="button"
                    :disabled="samplingTaskLoading"
                    @click="handlePublishSamplingResult(task)"
                  >
                    公示
                  </button>
                  <button
                    v-if="task.samplingResultId && task.samplingPublicStatus === 'PUBLISHED'"
                    class="op-btn op-btn--ghost"
                    type="button"
                    :disabled="samplingTaskLoading"
                    @click="handleOfflineSamplingResult(task)"
                  >
                    下线
                  </button>
                  <button v-if="isSamplingTaskClosable(task)" class="op-btn op-btn--archive" type="button" :disabled="samplingTaskLoading" @click="handleCloseSamplingTask(task)">归档</button>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
        <div v-if="!displayedTasks.length" class="empty">暂无抽检任务</div>
      </div>

      <div class="table-footer">
        <span class="footer-meta">显示 {{ rangeLabel }}，共 {{ totalDisplay }} 条记录</span>
        <div class="pager-actions">
          <button class="ghost" type="button" :disabled="samplingPage <= 1" @click="changeSamplingPage(samplingPage - 1)">上一页</button>
          <button class="ghost" type="button" :disabled="samplingPage >= samplingPages" @click="changeSamplingPage(samplingPage + 1)">下一页</button>
        </div>
      </div>

      <section class="stats-grid">
        <article class="stat-card stat-card--primary">
          <span>活跃任务</span>
          <strong>{{ statActive }}</strong>
          <p>当前页未关闭任务</p>
        </article>
        <article class="stat-card stat-card--green">
          <span>合格率</span>
          <strong>{{ statPassRate }}%</strong>
          <p>合格 / 已出结果</p>
        </article>
        <article class="stat-card stat-card--red">
          <span>不合格</span>
          <strong>{{ statFailCount }}</strong>
          <p>当前页检出</p>
        </article>
      </section>

      <div v-if="status.message" class="status" :class="status.type">{{ status.message }}</div>
    </section>
  </RegulatorAdminWorkspacePage>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from "vue";
import { useRouter } from "vue-router";
import { assignSamplingTask, closeSamplingTask, fetchSamplingTasks, offlineSamplingResult, publishSamplingResult } from "../../api/regulationOperation";
import { fetchEligibleRegulators } from "../../api/regulation";
import RegulatorAdminWorkspacePage from "../../components/regulatorAdmin/RegulatorAdminWorkspacePage.vue";
import { formatStatusLabel, inspectionResultMap, samplingPublicStatusMap, samplingTaskStatusMap } from "../../utils/statusMaps";
import { resolveErrorMessage } from "../../utils/uiFeedback";
import { useRegulatorAdminShellSession } from "./regulatorAdminShared";

const router = useRouter();
const { regulatorUser, token, handleSidebarNavigate, handleLogout } = useRegulatorAdminShellSession();
const status = reactive({ message: "", type: "" });

const samplingTaskLoading = ref(false);
const samplingFilters = reactive({ enterpriseName: "", status: "" });
const uiFilters = reactive({ taskKeyword: "", resultFilter: "", publicFilter: "" });
const samplingTasks = ref([]);
const samplingPage = ref(1);
const samplingSize = ref(8);
const samplingTotal = ref(0);
const samplingPages = ref(1);
const samplingAssignments = reactive({});
const enforcerMap = reactive({});

function setStatus(message, type = "info") {
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
  if (value === "COMPLETED") return "is-success";
  if (value === "ASSIGNED") return "is-warn";
  if (value === "CLOSED") return "is-muted";
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
  let list = samplingTasks.value.slice();
  const kw = uiFilters.taskKeyword.trim().toLowerCase();
  if (kw) list = list.filter((t) => (t.taskTitle || t.productName || "").toLowerCase().includes(kw));
  if (uiFilters.resultFilter === "PASS") list = list.filter((t) => t.samplingResult === "PASS");
  if (uiFilters.resultFilter === "FAIL") list = list.filter((t) => t.samplingResult === "FAIL");
  if (uiFilters.resultFilter === "PENDING") list = list.filter((t) => !t.samplingResult);
  if (uiFilters.publicFilter) {
    if (uiFilters.publicFilter === "DRAFT") list = list.filter((t) => !t.samplingPublicStatus || t.samplingPublicStatus === "DRAFT");
    else list = list.filter((t) => (t.samplingPublicStatus || "") === uiFilters.publicFilter);
  }
  return list;
});

const totalDisplay = computed(() => {
  const t = samplingTotal.value;
  const records = samplingTasks.value.length;
  if (!t && records) return records;
  return t;
});

const rangeLabel = computed(() => {
  const total = totalDisplay.value;
  if (!total) return "0 条";
  const start = (samplingPage.value - 1) * samplingSize.value + 1;
  const end = Math.min(samplingPage.value * samplingSize.value, total);
  return `${start}-${end} 条`;
});

const statActive = computed(() => displayedTasks.value.filter((t) => t.status !== "CLOSED").length);
const statFailCount = computed(() => displayedTasks.value.filter((t) => t.samplingResult === "FAIL").length);
const statPassRate = computed(() => {
  const withResult = displayedTasks.value.filter((t) => t.samplingResult === "PASS" || t.samplingResult === "FAIL");
  if (!withResult.length) return 0;
  const pass = withResult.filter((t) => t.samplingResult === "PASS").length;
  return Math.round((pass / withResult.length) * 100);
});

function isTaskDeadlineExceeded(deadline) {
  if (!deadline) return false;
  const deadlineMs = new Date(deadline).getTime();
  if (Number.isNaN(deadlineMs)) return false;
  return deadlineMs <= Date.now();
}

function isSamplingTaskAssignable(task) {
  return ["CREATED", "ASSIGNED"].includes(task.status);
}

function isSamplingTaskClosable(task) {
  return task.status === "COMPLETED";
}

function getEnforcers(regionId) {
  if (!regionId) return [];
  return enforcerMap[regionId] || [];
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

async function loadSamplingTasks() {
  samplingTaskLoading.value = true;
  setStatus("");
  try {
    const data = await fetchSamplingTasks(token.value, { ...samplingFilters, page: samplingPage.value, size: samplingSize.value });
    samplingTasks.value = data.records || [];
    samplingTotal.value = data.total || 0;
    samplingPage.value = data.page || 1;
    samplingSize.value = data.size || samplingSize.value;
    samplingPages.value = data.pages || 1;
    const regionIds = samplingTasks.value.map((task) => task.regionId).filter(Boolean);
    await Promise.all(regionIds.map((id) => ensureEnforcers(id)));
    for (const task of samplingTasks.value) {
      if (!isSamplingTaskAssignable(task)) continue;
      samplingAssignments[task.id] = task.assignedTo != null ? task.assignedTo : "";
    }
  } catch (error) {
    setStatus(resolveErrorMessage(error, "加载抽检任务失败"), "error");
  } finally {
    samplingTaskLoading.value = false;
  }
}

async function handleSamplingSearch() {
  samplingPage.value = 1;
  await loadSamplingTasks();
}

function resetListFilters() {
  samplingFilters.enterpriseName = "";
  samplingFilters.status = "";
  uiFilters.taskKeyword = "";
  uiFilters.resultFilter = "";
  uiFilters.publicFilter = "";
  handleSamplingSearch();
}

function toggleResultFilter(value) {
  uiFilters.resultFilter = uiFilters.resultFilter === value ? "" : value;
}

function togglePublicFilter(value) {
  uiFilters.publicFilter = uiFilters.publicFilter === value ? "" : value;
}

async function changeSamplingPage(nextPage) {
  samplingPage.value = nextPage;
  await loadSamplingTasks();
}

async function handleCloseSamplingTask(task) {
  if (!task?.id) return;
  samplingTaskLoading.value = true;
  setStatus("", "info");
  try {
    await closeSamplingTask(token.value, task.id);
    setStatus("抽检任务已归档", "success");
    await loadSamplingTasks();
  } catch (error) {
    setStatus(resolveErrorMessage(error, "归档抽检任务失败"), "error");
  } finally {
    samplingTaskLoading.value = false;
  }
}

async function handleAssignSamplingTask(task) {
  if (isTaskDeadlineExceeded(task?.deadline)) return setStatus("任务已超期，无法派发", "error");
  const regulatorId = samplingAssignments[task.id];
  if (!regulatorId) return setStatus("请选择执法人员后再派发", "error");
  samplingTaskLoading.value = true;
  setStatus("");
  try {
    await assignSamplingTask(token.value, task.id, { regulatorId });
    setStatus("抽检任务已派发", "success");
    await loadSamplingTasks();
  } catch (error) {
    setStatus(resolveErrorMessage(error, "抽检任务派发失败"), "error");
  } finally {
    samplingTaskLoading.value = false;
  }
}

async function handlePublishSamplingResult(task) {
  if (!task?.samplingResultId) return setStatus("抽检结果未生成，无法公示", "error");
  samplingTaskLoading.value = true;
  setStatus("");
  try {
    await publishSamplingResult(token.value, task.samplingResultId);
    setStatus("抽检结果已公示", "success");
    await loadSamplingTasks();
  } catch (error) {
    setStatus(resolveErrorMessage(error, "抽检结果公示失败"), "error");
  } finally {
    samplingTaskLoading.value = false;
  }
}

async function handleOfflineSamplingResult(task) {
  if (!task?.samplingResultId) return setStatus("抽检结果未生成，无法下线", "error");
  samplingTaskLoading.value = true;
  setStatus("");
  try {
    await offlineSamplingResult(token.value, task.samplingResultId);
    setStatus("抽检结果已下线", "success");
    await loadSamplingTasks();
  } catch (error) {
    setStatus(resolveErrorMessage(error, "抽检结果下线失败"), "error");
  } finally {
    samplingTaskLoading.value = false;
  }
}

function goToSamplingDetail(task) {
  if (!task?.id) return;
  router.push({ name: "regulator-admin-sampling-detail", params: { taskId: String(task.id) } }).catch(() => {});
}

function goToCreateSampling() {
  router.push({ name: "regulator-admin-sampling-create" });
}

onMounted(loadSamplingTasks);
</script>

<style scoped>
.sampling-page { display: grid; gap: 16px; }
.sampling-page__head { display: flex; justify-content: space-between; align-items: flex-end; gap: 12px; }
.crumbs { display: flex; align-items: center; gap: 6px; font-size: 10px; font-weight: 800; text-transform: uppercase; letter-spacing: 0.06em; color: #64748b; }
.crumbs-sep { opacity: 0.6; }
.crumbs .is-current { color: #002660; }
h1 { margin: 6px 0 0; color: #002660; font-size: 30px; font-weight: 800; }
.sampling-page__head p { margin: 6px 0 0; color: #64748b; font-size: 14px; }
.head-create-btn { min-height: 42px; padding: 10px 18px; border-radius: 8px; box-shadow: 0 12px 24px rgba(0, 38, 96, 0.2); }
.primary { border: 0; background: #002660; color: #fff; border-radius: 6px; padding: 9px 14px; font-size: 12px; font-weight: 700; cursor: pointer; }
.ghost { border: 1px solid #d1d5db; background: #fff; color: #334155; border-radius: 6px; padding: 8px 12px; font-size: 12px; cursor: pointer; }
.panel { background: #fff; border: 1px solid #e2e8f0; border-radius: 8px; padding: 14px; }
.filter-bento { display: grid; grid-template-columns: 1fr minmax(268px, 300px); gap: 14px; align-items: stretch; }
.filter-panel { background: linear-gradient(180deg, #fbfcfe 0%, #ffffff 40%); border: 1px solid #e2e8f0; border-radius: 10px; overflow: hidden; box-shadow: 0 4px 16px rgba(15, 23, 42, 0.06); }
.filter-panel__head { display: flex; align-items: center; gap: 10px; padding: 12px 16px; background: linear-gradient(90deg, #f1f5f9 0%, #f8fafc 100%); border-bottom: 1px solid #e8edf3; }
.filter-panel__accent { width: 4px; height: 20px; border-radius: 2px; background: #002660; flex-shrink: 0; }
.filter-panel__title { margin: 0; font-size: 15px; font-weight: 800; color: #0f172a; letter-spacing: 0.02em; }
.filter-panel__body { padding: 14px 16px 16px; display: flex; flex-direction: column; gap: 12px; }
.filter-toolbar { display: flex; align-items: flex-end; gap: 12px; flex-wrap: wrap; }
.filter-grid-3 { flex: 1 1 0; min-width: 0; display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 10px 12px; }
.filter-grid-3 label { display: grid; gap: 6px; font-size: 13px; font-weight: 700; color: #334155; }
.filter-grid-3 input, .filter-grid-3 select { border: 1px solid #dbe2ea; background: #fff; border-radius: 8px; padding: 10px 12px; font-size: 14px; color: #0f172a; min-height: 40px; box-sizing: border-box; }
.filter-actions { display: flex; gap: 8px; flex-shrink: 0; align-items: center; padding-bottom: 1px; }
.filter-btn.primary { padding: 10px 20px; font-size: 14px; min-height: 40px; border-radius: 8px; }
.filter-btn.ghost { padding: 10px 18px; font-size: 14px; min-height: 40px; border-radius: 8px; border-color: #cbd5e1; }
.filter-bento__side { background: linear-gradient(135deg, #002660, #003a8c); color: #fff; border-radius: 10px; padding: 14px 16px; display: flex; flex-direction: column; gap: 12px; box-shadow: 0 12px 24px rgba(0, 38, 96, 0.2); position: relative; overflow: hidden; }
.side-title { display: flex; justify-content: space-between; align-items: center; font-size: 13px; font-weight: 800; letter-spacing: 0.06em; opacity: 0.95; }
.side-title-icon { opacity: 0.65; font-size: 16px; }
.side-block { display: flex; flex-direction: column; gap: 8px; }
.side-label { font-size: 12px; font-weight: 700; opacity: 0.88; }
.pill-group { display: flex; flex-wrap: wrap; gap: 6px; }
.pill-group button { border: 0; background: rgba(255, 255, 255, 0.14); color: #fff; font-size: 12px; font-weight: 700; padding: 8px 12px; border-radius: 6px; cursor: pointer; min-height: 36px; }
.pill-group button.active { background: rgba(255, 255, 255, 0.38); }
.table-wrap { border: 1px solid #e2e8f0; border-radius: 8px; overflow: auto; background: #fff; }
table { width: 100%; min-width: 1080px; border-collapse: collapse; }
thead tr { background: #e2e8f0; border-bottom: 1px solid #cbd5e1; }
th { text-align: left; padding: 10px 12px; font-size: 10px; font-weight: 800; color: #64748b; text-transform: uppercase; letter-spacing: 0.02em; }
.th-center, .td-center { text-align: center; }
.th-right, .td-right { text-align: right; }
td { padding: 10px 12px; border-top: 1px solid #f1f5f9; font-size: 0.8125rem; color: #1e293b; vertical-align: middle; }
tbody tr:nth-child(even) { background: #f8fafc; }
tbody tr:hover { background: #f1f5f9; }
.mono { font-family: ui-monospace, SFMono-Regular, Menlo, Consolas, monospace; color: #64748b; }
.cell-title { font-weight: 700; color: #0f172a; }
.cell-sub { font-size: 10px; color: #64748b; margin-top: 2px; }
.cell-enterprise { display: flex; align-items: center; gap: 6px; }
.ent-icon { color: #3b82f6; font-size: 12px; line-height: 1; }
.pill { display: inline-flex; padding: 3px 8px; border-radius: 4px; font-size: 10px; font-weight: 800; text-transform: uppercase; }
.pill--status.is-default { background: #e2e8f0; color: #334155; }
.pill--status.is-warn { background: #fef3c7; color: #b45309; }
.pill--status.is-success { background: #dbeafe; color: #1d4ed8; }
.pill--status.is-muted { background: #fee2e2; color: #991b1b; }
.pill--result.is-pass { background: #dcfce7; color: #166534; }
.pill--result.is-fail { background: #fee2e2; color: #991b1b; }
.pill--result.is-pending { background: #f1f5f9; color: #64748b; }
.pill--pub.is-draft { background: #e2e8f0; color: #475569; }
.pill--pub.is-pub { background: #c9d7fe; color: #1e3a8a; }
.pill--pub.is-off { background: #e2e8f0; color: #64748b; }
.table-footer { display: flex; justify-content: space-between; align-items: center; padding: 10px 12px; background: #fff; border: 1px solid #e2e8f0; border-radius: 8px; font-size: 10px; font-weight: 700; color: #64748b; text-transform: uppercase; letter-spacing: 0.04em; }
.pager-actions { display: flex; gap: 8px; }
.stats-grid { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 10px; }
.stat-card { padding: 12px; border-radius: 8px; background: #f8fafc; border: 1px solid #e2e8f0; border-left-width: 4px; }
.stat-card span { font-size: 10px; font-weight: 700; color: #64748b; text-transform: uppercase; letter-spacing: 0.06em; }
.stat-card strong { display: block; margin-top: 6px; font-size: 24px; font-weight: 800; color: #0f172a; }
.stat-card p { margin: 4px 0 0; font-size: 11px; color: #94a3b8; }
.stat-card--primary { border-left-color: #002660; }
.stat-card--green { border-left-color: #16a34a; }
.stat-card--red { border-left-color: #dc2626; }
.action-row { display: flex; gap: 6px; align-items: center; flex-wrap: wrap; justify-content: flex-end; padding: 4px; border-radius: 8px; background: #f8fafc; border: 1px solid #e2e8f0; }
.op-btn { border: 1px solid #cbd5e1; background: #fff; color: #334155; border-radius: 6px; padding: 5px 10px; min-height: 28px; font-size: 12px; font-weight: 700; line-height: 1; cursor: pointer; transition: all 0.15s ease; }
.op-btn:hover:not(:disabled) { transform: translateY(-1px); box-shadow: 0 4px 10px rgba(15, 23, 42, 0.08); }
.op-btn:disabled { cursor: not-allowed; opacity: 0.55; }
.op-btn--detail { color: #1e3a8a; border-color: #bfdbfe; background: #eff6ff; }
.op-btn--assign { color: #fff; border-color: #1d4ed8; background: #1d4ed8; }
.op-btn--pub { color: #fff; border-color: #059669; background: #059669; }
.op-btn--ghost { color: #475569; border-color: #cbd5e1; background: #fff; }
.op-btn--archive { color: #166534; border-color: #86efac; background: #f0fdf4; }
.op-select { border: 1px solid #cbd5e1; background: #fff; border-radius: 6px; padding: 5px 8px; min-height: 28px; min-width: 118px; font-size: 12px; color: #334155; }
.op-expired-tip { display: inline-flex; align-items: center; min-height: 28px; padding: 0 10px; border-radius: 6px; border: 1px solid #fecaca; background: #fef2f2; color: #b91c1c; font-size: 12px; font-weight: 700; white-space: nowrap; }
.empty { padding: 16px; text-align: center; color: #64748b; font-size: 13px; }
.status { position: fixed; right: 18px; bottom: 18px; border-radius: 8px; padding: 10px 12px; color: #fff; background: #0f172a; font-size: 13px; }
.status.error { background: #b91c1c; }
.status.success { background: #166534; }
@media (max-width: 1100px) {
  .filter-toolbar { flex-direction: column; align-items: stretch; }
  .filter-actions { justify-content: flex-end; padding-bottom: 0; }
}
@media (max-width: 900px) {
  .filter-bento { grid-template-columns: 1fr; }
  .filter-grid-3 { grid-template-columns: 1fr; }
  .stats-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); }
}
@media (max-width: 640px) {
  .stats-grid { grid-template-columns: 1fr; }
}
</style>
