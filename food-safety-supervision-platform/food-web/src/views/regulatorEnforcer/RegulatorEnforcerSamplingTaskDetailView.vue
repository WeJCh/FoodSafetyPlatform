<template>
  <RegulatorEnforcerWorkspacePage
    active-key="sampling"
    :username="enforcerUser.username || enforcerUser.realName || ''"
    @navigate="handleSidebarNavigate"
    @logout="handleLogout"
  >
    <section class="sampling-detail-page">
      <header class="page-head">
        <div>
          <nav class="crumbs" aria-label="面包屑">
            <button type="button" class="crumb-link" @click="goList">我的抽检任务</button>
            <span class="crumb-sep">/</span>
            <span class="crumb-current">{{ task?.taskNo || "任务详情" }}</span>
          </nav>
          <h1>抽检任务详情</h1>
        </div>
        <div class="head-actions">
          <button class="ghost-btn" type="button" @click="goList">返回列表</button>
          <button v-if="task?.enterpriseId" class="ghost-btn" type="button" @click="openEnterpriseDetail">查看企业档案</button>
          <button v-if="showResultEntry" class="primary-btn" type="button" @click="openResultEntry">
            开始采样 / 录入结果
          </button>
        </div>
      </header>

      <div v-if="pageLoading" class="state-card">抽检任务详情加载中...</div>
      <div v-else-if="loadError" class="state-card state-card--error">{{ loadError }}</div>

      <template v-else-if="task">
        <section class="mission-board">
          <div class="mission-board__main">
            <div class="mission-board__chips">
              <span class="chip" :class="statusChipClass(task.status)">{{ formatSamplingTaskStatus(task.status) }}</span>
              <span v-if="task.priority" class="chip chip--plain">{{ formatTaskPriority(task.priority) }}</span>
              <span v-if="task.samplingResult" class="chip" :class="resultChipClass(task.samplingResult)">
                {{ formatInspectionResult(task.samplingResult) }}
              </span>
              <span class="chip chip--plain">{{ task.taskNo || `#${task.id}` }}</span>
            </div>
            <p class="mission-board__eyebrow">{{ currentEnterpriseName }}</p>
            <h2>{{ task.taskTitle || task.productName || "抽检任务" }}</h2>
            <p class="mission-board__product">
              {{ task.productName || "待确认抽检产品" }}
              <span v-if="task.productSpecification"> / {{ task.productSpecification }}</span>
            </p>
            <div class="mission-board__desc">
              <span>任务说明</span>
              <p>{{ task.taskDesc || "当前任务由抽检计划下发，请按要求完成现场采样与结果录入。" }}</p>
            </div>
          </div>

          <aside class="mission-board__side">
            <article class="mission-status">
              <span>剩余时间</span>
              <strong :class="{ warning: deadlineInfo.tone === 'warning', danger: deadlineInfo.tone === 'danger' }">
                {{ deadlineInfo.label }}
              </strong>
              <p>截止完成时间 {{ formatTime(task.deadline) || "-" }}</p>
            </article>
            <article class="mission-status mission-status--soft">
              <span>执行提示</span>
              <strong>{{ resultProgressTitle }}</strong>
              <p>{{ resultProgressNote }}</p>
            </article>
          </aside>
        </section>

        <div class="content-grid">
          <div class="main-column">
            <section class="snapshot-grid">
              <article v-for="card in taskSummaryCards" :key="card.label" class="snapshot-card">
                <span>{{ card.label }}</span>
                <strong>{{ card.value }}</strong>
                <small>{{ card.meta }}</small>
              </article>
            </section>

            <section v-if="task.samplingResult || task.samplingConclusion" class="panel">
              <div class="panel-head">
                <h3>已录入结果</h3>
                <span>检测结论与公示状态</span>
              </div>
              <div class="result-summary">
                <article>
                  <span>抽检结论</span>
                  <strong>{{ formatInspectionResult(task.samplingResult) }}</strong>
                </article>
                <article>
                  <span>公示状态</span>
                  <strong>{{ formatSamplingPublicStatus(task.samplingPublicStatus) }}</strong>
                </article>
                <article class="is-wide">
                  <span>结果说明</span>
                  <strong>{{ task.samplingConclusion || "暂无说明" }}</strong>
                </article>
              </div>
            </section>

            <section class="panel panel--soft">
              <div class="panel-head">
                <h3>任务执行建议</h3>
                <span>先核对主体，再处理现场动作</span>
              </div>
              <ul class="plan-list">
                <li v-for="(item, index) in planLines" :key="index">{{ item }}</li>
              </ul>
            </section>

            <section class="panel">
              <div class="panel-head">
                <h3>状态流转记录</h3>
                <span>共 {{ timeline.length }} 条关键记录</span>
              </div>
              <div v-if="timeline.length" class="timeline-list">
                <article v-for="(item, index) in timeline" :key="item.key || `${item.title}-${index}`" class="timeline-item">
                  <span class="timeline-dot" :class="item.dotClass"></span>
                  <div>
                    <strong>{{ item.title }}</strong>
                    <small>{{ item.meta }}</small>
                    <p>{{ item.desc }}</p>
                  </div>
                </article>
              </div>
              <div v-else class="timeline-empty">暂无操作日志</div>
            </section>
          </div>

          <aside class="side-column">
            <section class="panel panel--compact">
              <div class="panel-head">
                <h3>任务协同信息</h3>
                <span>指派、创建与完成状态</span>
              </div>
              <dl class="kv-list kv-list--compact">
                <div v-for="item in taskCoordinationItems" :key="item.label" :class="{ 'is-wide': item.wide }">
                  <dt>{{ item.label }}</dt>
                  <dd>{{ item.value }}</dd>
                  <small v-if="item.meta">{{ item.meta }}</small>
                </div>
              </dl>
            </section>

            <section class="panel panel--compact">
              <div class="panel-head">
                <h3>企业与区域信息</h3>
                <span>执行前核对主体信息</span>
              </div>
              <dl class="kv-list kv-list--compact">
                <div v-for="item in enterpriseInfoItems" :key="item.label" :class="{ 'is-wide': item.wide }">
                  <dt>{{ item.label }}</dt>
                  <dd>{{ item.value }}</dd>
                  <small v-if="item.meta">{{ item.meta }}</small>
                </div>
              </dl>
            </section>
          </aside>
        </div>

        <div v-if="status.message" class="status-banner" :class="`is-${status.type}`">{{ status.message }}</div>
      </template>
    </section>
  </RegulatorEnforcerWorkspacePage>
</template>

<script setup>
import { computed, reactive, ref, watch } from "vue";
import { useRoute, useRouter } from "vue-router";
import { fetchEnterpriseDetail, fetchRegionPath } from "../../api/regulation";
import { fetchOperationAuditLogs, findMySamplingTaskById } from "../../api/regulationOperation";
import RegulatorEnforcerWorkspacePage from "../../components/regulatorEnforcer/RegulatorEnforcerWorkspacePage.vue";
import { formatTime } from "../../utils/formatters";
import {
  formatStatusLabel,
  inspectionResultMap,
  samplingPublicStatusMap,
  samplingTaskStatusMap,
  taskPriorityMap
} from "../../utils/statusMaps";
import { resolveErrorMessage } from "../../utils/uiFeedback";
import { useRegulatorEnforcerShellSession } from "./regulatorEnforcerShared";

const route = useRoute();
const router = useRouter();
const { enforcerUser, token, handleSidebarNavigate, handleLogout } = useRegulatorEnforcerShellSession();

const pageLoading = ref(true);
const loadError = ref("");
const task = ref(null);
const enterprise = ref(null);
const regionLabel = ref("-");
const auditLogs = ref([]);
const status = reactive({ message: "", type: "info" });

const showResultEntry = computed(() => {
  const value = String(task.value?.status || "").toUpperCase();
  return value === "ASSIGNED";
});

const currentEnterpriseName = computed(() => task.value?.enterpriseName || enterprise.value?.enterpriseName || "-");
const currentAssigneeName = computed(() => task.value?.assignedToName || enforcerUser.realName || enforcerUser.username || "-");

const deadlineInfo = computed(() => {
  const deadline = task.value?.deadline ? new Date(task.value.deadline) : null;
  if (!deadline || Number.isNaN(deadline.getTime())) return { label: "暂无时限", tone: "normal" };
  const diff = deadline.getTime() - Date.now();
  if (diff <= 0) return { label: "已逾期", tone: "danger" };
  const hours = Math.floor(diff / (1000 * 60 * 60));
  const days = Math.floor(hours / 24);
  if (days > 0) return { label: `${days}天 ${hours % 24}小时`, tone: days <= 1 ? "warning" : "normal" };
  return { label: `${hours}小时内`, tone: hours <= 12 ? "danger" : "warning" };
});

const resultProgressTitle = computed(() => {
  const current = task.value;
  if (!current) return "-";
  if (current.samplingResult === "FAIL") return "已形成不合格结论";
  if (current.samplingResult === "PASS") return "已形成合格结论";
  if (current.samplingResultId) return "已提交抽检结果";
  if (showResultEntry.value) return "待执行现场采样";
  return "等待进入执行阶段";
});

const resultProgressNote = computed(() => {
  const current = task.value;
  if (!current) return "";
  if (current.samplingResult === "FAIL") {
    return "请保留现场采样、封样和流转证据，并关注后续联动处置。";
  }
  if (current.samplingResult === "PASS") {
    return "请复核抽检材料完整性，确认结论说明准确无误。";
  }
  if (current.samplingResultId) {
    return "结果已经生成，可回看结论摘要与操作记录。";
  }
  if (showResultEntry.value) {
    return "当前任务已可处理，完成现场采样后请尽快录入结果。";
  }
  return "任务尚未进入录入结果阶段。";
});

const taskSummaryCards = computed(() => {
  const current = task.value;
  if (!current) return [];
  return [
    {
      label: "受检企业",
      value: currentEnterpriseName.value,
      meta: regionLabel.value !== "-" ? regionLabel.value : "区域信息待补充"
    },
    {
      label: "抽检产品",
      value: current.productName || "-",
      meta: current.productCategory || "未标注产品分类"
    },
    {
      label: "规格型号",
      value: current.productSpecification || "-",
      meta: current.taskTitle || "任务标题待补充"
    },
    {
      label: "执行人员",
      value: currentAssigneeName.value,
      meta: current.assignedTime ? `指派于 ${formatTime(current.assignedTime)}` : "待确认指派时间"
    },
    {
      label: "截止完成时间",
      value: formatTime(current.deadline) || "-",
      meta: deadlineInfo.value.label === "暂无时限" ? "请关注任务更新时间" : `剩余 ${deadlineInfo.value.label}`
    },
    {
      label: "结果 / 公示",
      value: current.samplingResult ? formatInspectionResult(current.samplingResult) : "待录入",
      meta: `公示状态：${formatSamplingPublicStatus(current.samplingPublicStatus)}`
    }
  ];
});

const timeline = computed(() => {
  if (!task.value || !auditLogs.value.length) return [];
  return auditLogs.value.map((item, index) => ({
    key: `${item.targetType || "target"}-${item.id || index}`,
    title: formatSamplingAuditTitle(item),
    meta: formatSamplingAuditMeta(item),
    desc: item?.remark || item?.summary || item?.actionName || item?.actionType || "抽检任务操作",
    dotClass: samplingAuditDotClass(item)
  }));
});

const taskCoordinationItems = computed(() => {
  const current = task.value;
  if (!current) return [];
  return [
    {
      label: "任务编号",
      value: current.taskNo || `#${current.id}`,
      meta: `优先级：${formatTaskPriority(current.priority)}`
    },
    {
      label: "指派信息",
      value: current.assignedByName || current.createdByName || "-",
      meta: `指派时间：${formatTime(current.assignedTime) || "-"}`
    },
    {
      label: "创建信息",
      value: current.createdByName || "-",
      meta: `创建时间：${formatTime(current.createTime) || "-"}`
    },
    {
      label: "最近更新",
      value: formatTime(current.updateTime) || "-",
      meta: `完成时间：${formatTime(current.completedTime) || "未完成"}`
    }
  ];
});

const enterpriseInfoItems = computed(() => {
  if (!task.value) return [];
  const principalName = enterprise.value?.principal || "";
  const principalPhone = enterprise.value?.principalPhone || "";
  return [
    { label: "企业名称", value: currentEnterpriseName.value, wide: true },
    { label: "所属区域", value: regionLabel.value || "-" },
    {
      label: "联系信息",
      value: principalName || principalPhone || "-",
      meta: principalName && principalPhone ? principalPhone : principalName || principalPhone ? "" : "暂无联系电话"
    },
    { label: "详细地址", value: enterprise.value?.addressDetail || "-", wide: true }
  ];
});

const planLines = computed(() => {
  const current = task.value;
  if (!current) return ["加载任务建议中..."];
  if (current.samplingResult === "FAIL") {
    return [
      `通知“${currentEnterpriseName.value || "企业"}”配合复核，并准备后续处置材料。`,
      "保留现场采样、封样与流转证据，必要时同步上报风险预警。",
      "复核样品批次、规格和现场影像，确保不合格信息留痕完整。"
    ];
  }
  if (current.samplingResult === "PASS") {
    return [
      "复核抽检材料、现场记录和样品信息，确保结论说明完整准确。",
      "确认无补充事项后，可返回任务列表继续处理其他抽检任务。"
    ];
  }
  return [
    "先核对企业主体、样品信息和任务要求，再开展现场采样。",
    "采样完成后尽快录入结果，如发现异常请同步补充处置建议。"
  ];
});

function setStatus(message = "", type = "info") {
  status.message = message;
  status.type = type;
}

function formatSamplingTaskStatus(value) {
  return formatStatusLabel(value, samplingTaskStatusMap);
}

function formatTaskPriority(value) {
  return value ? formatStatusLabel(value, taskPriorityMap) : "常规";
}

function formatInspectionResult(value) {
  return value ? formatStatusLabel(value, inspectionResultMap) : "待录入";
}

function formatSamplingPublicStatus(value) {
  return value ? formatStatusLabel(value, samplingPublicStatusMap) : "未生成";
}

function formatLogOperatorName(value) {
  const text = String(value || "").trim();
  return text || "系统";
}

function normalizeSamplingAuditAction(value) {
  return String(value || "").trim().toUpperCase();
}

function formatSamplingAuditMeta(item) {
  const parts = [formatTime(item?.createTime) || "-"];
  const operatorName = formatLogOperatorName(item?.operatorName);
  if (operatorName) parts.push(operatorName);
  return parts.join(" · ");
}

function statusChipClass(value) {
  const normalized = String(value || "").toUpperCase();
  if (normalized === "COMPLETED") return "chip--status-completed";
  if (normalized === "ASSIGNED") return "chip--status-assigned";
  if (normalized === "CLOSED") return "chip--status-closed";
  return "chip--status-default";
}

function resultChipClass(value) {
  const normalized = String(value || "").toUpperCase();
  if (normalized === "PASS") return "chip--result-pass";
  if (normalized === "FAIL") return "chip--result-fail";
  return "chip--plain";
}

function goList() {
  router.push({ name: "regulator-enforcer-sampling" }).catch(() => {});
}

function openEnterpriseDetail() {
  if (!task.value?.enterpriseId) return;
  router.push({
    name: "regulator-enforcer-enterprise-detail",
    params: { enterpriseId: task.value.enterpriseId },
    query: { from: "sampling" }
  }).catch(() => {});
}

function openResultEntry() {
  if (!task.value?.id) return;
  router.push({ name: "regulator-enforcer-sampling-submit", params: { taskId: task.value.id } }).catch(() => {});
}

async function loadDetail() {
  pageLoading.value = true;
  loadError.value = "";
  task.value = null;
  enterprise.value = null;
  regionLabel.value = "-";
  auditLogs.value = [];
  setStatus("");

  const taskId = route.params.taskId;
  if (!taskId) {
    loadError.value = "缺少任务参数";
    pageLoading.value = false;
    return;
  }

  try {
    const row = await findMySamplingTaskById(token.value, taskId);
    if (!row) {
      loadError.value = "未找到该抽检任务，请从列表重新进入。";
      return;
    }

    task.value = row;
    const [taskLogs, resultLogs] = await Promise.all([
      fetchOperationAuditLogs(token.value, "SAMPLING_TASK", row.id, 12).catch(() => []),
      row.samplingResultId
        ? fetchOperationAuditLogs(token.value, "SAMPLING_RESULT", row.samplingResultId, 12).catch(() => [])
        : Promise.resolve([])
    ]);
    auditLogs.value = [...taskLogs, ...resultLogs]
      .sort((a, b) => new Date(b.createTime || 0).getTime() - new Date(a.createTime || 0).getTime());

    if (route.query.submitted === "1") {
      setStatus("抽检结果已提交。", "success");
    }

    if (row.enterpriseId) {
      try {
        const detail = await fetchEnterpriseDetail(token.value, row.enterpriseId);
        enterprise.value = detail || null;
        if (detail?.regionId) {
          const path = await fetchRegionPath(token.value, detail.regionId).catch(() => []);
          regionLabel.value = Array.isArray(path) && path.length ? path.map((item) => item.name).join(" / ") : "-";
        }
      } catch {
        enterprise.value = null;
      }
    }
  } catch (error) {
    loadError.value = resolveErrorMessage(error, "抽检任务详情加载失败");
  } finally {
    pageLoading.value = false;
  }
}

function formatSamplingAuditTitle(item) {
  return item?.actionName || item?.actionType || "抽检任务日志";
}

function samplingAuditDotClass(item) {
  const actionType = normalizeSamplingAuditAction(item?.actionType);
  const detailText = `${item?.afterData || ""} ${item?.summary || ""} ${item?.remark || ""}`.toUpperCase();
  if (actionType === "SAMPLING_RESULT_OFFLINE") return "is-muted";
  if (detailText.includes("FAIL")) return "is-alert";
  return "is-on";
}

watch(() => route.params.taskId, loadDetail, { immediate: true });
</script>

<style scoped>
.sampling-detail-page { display: grid; gap: 18px; }
.page-head { display: flex; justify-content: space-between; align-items: end; gap: 16px; flex-wrap: wrap; }
.crumbs { display: flex; align-items: center; gap: 8px; margin-bottom: 8px; font-size: 11px; color: #64748b; }
.crumb-link { border: 0; background: transparent; padding: 0; color: inherit; cursor: pointer; }
.crumb-link:hover, .crumb-current { color: #002660; }
.crumb-sep { opacity: 0.5; }
h1 { margin: 0; color: #002660; font-size: 30px; font-weight: 900; }
.head-actions { display: flex; gap: 10px; flex-wrap: wrap; }
.primary-btn, .ghost-btn { min-height: 38px; padding: 0 16px; border: 1px solid #cbd5e1; background: #fff; color: #334155; font-size: 12px; font-weight: 800; cursor: pointer; }
.primary-btn { border-color: #002660; background: #002660; color: #fff; }
.state-card { padding: 18px; border: 1px solid #dbe3ee; background: #fff; color: #64748b; }
.state-card--error { border-color: #fecaca; background: #fef2f2; color: #b91c1c; }

.mission-board {
  display: grid;
  grid-template-columns: minmax(0, 1.45fr) 320px;
  gap: 16px;
  padding: 24px;
  border: 1px solid #dbe3ee;
  background: #fff;
}

.mission-board__main {
  display: grid;
  gap: 14px;
  min-width: 0;
}

.mission-board__chips {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.chip {
  display: inline-flex;
  align-items: center;
  min-height: 26px;
  padding: 0 10px;
  border-radius: 999px;
  font-size: 11px;
  font-weight: 800;
}

.chip--plain {
  background: #eef2f7;
  color: #475569;
}

.chip--status-default {
  background: #dbeafe;
  color: #1e3a8a;
}

.chip--status-assigned {
  background: #e0f2fe;
  color: #0369a1;
}

.chip--status-completed {
  background: #dcfce7;
  color: #166534;
}

.chip--status-closed {
  background: #e2e8f0;
  color: #475569;
}

.chip--result-pass {
  background: #dcfce7;
  color: #166534;
}

.chip--result-fail {
  background: #fee2e2;
  color: #991b1b;
}

.mission-board__eyebrow {
  margin: 0;
  color: #335bae;
  font-size: 12px;
  font-weight: 800;
  letter-spacing: 0.04em;
}

.mission-board h2 {
  margin: 0;
  color: #0f172a;
  font-size: 30px;
  font-weight: 900;
  line-height: 1.25;
}

.mission-board__product {
  margin: 0;
  color: #475569;
  font-size: 15px;
  line-height: 1.6;
}

.mission-board__desc {
  padding: 14px 16px;
  border: 1px solid #e8edf3;
  background: #f8fafc;
}

.mission-board__desc span,
.mission-status span,
.snapshot-card span,
.panel-head span,
.result-summary span,
.kv-list dt {
  display: block;
  color: #64748b;
  font-size: 11px;
  font-weight: 800;
}

.mission-board__desc p {
  margin: 8px 0 0;
  color: #334155;
  font-size: 13px;
  line-height: 1.7;
}

.mission-board__side {
  display: grid;
  gap: 12px;
  align-content: start;
}

.mission-status {
  padding: 16px;
  border: 1px solid #dbe3ee;
  background: #f8fbff;
}

.mission-status--soft {
  background: #fff;
}

.mission-status strong {
  display: block;
  margin-top: 8px;
  color: #002660;
  font-size: 24px;
  font-weight: 900;
  line-height: 1.25;
}

.mission-status strong.warning { color: #b45309; }
.mission-status strong.danger { color: #b91c1c; }

.mission-status p {
  margin: 8px 0 0;
  color: #475569;
  font-size: 12px;
  line-height: 1.7;
}

.snapshot-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
}

.snapshot-card {
  display: grid;
  gap: 8px;
  min-height: 116px;
  padding: 16px;
  border: 1px solid #dbe3ee;
  background: #fff;
}

.snapshot-card strong {
  color: #0f172a;
  font-size: 18px;
  line-height: 1.45;
}

.snapshot-card small {
  color: #475569;
  font-size: 12px;
  line-height: 1.6;
}

.content-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.55fr) 340px;
  gap: 16px;
  align-items: start;
}

.main-column, .side-column {
  display: grid;
  gap: 16px;
  align-content: start;
}

.panel {
  padding: 16px;
  border: 1px solid #dbe3ee;
  background: #fff;
}

.panel--compact .panel-head {
  margin-bottom: 12px;
}

.panel--soft {
  background: linear-gradient(180deg, #f5f9ff 0%, #edf4ff 100%);
}

.panel-head {
  display: flex;
  justify-content: space-between;
  gap: 10px;
  align-items: flex-start;
  margin-bottom: 14px;
}

.panel-head h3 {
  margin: 0;
  color: #002660;
  font-size: 15px;
  font-weight: 900;
}

.result-summary {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.result-summary article {
  padding: 12px;
  border: 1px solid #e8edf3;
  background: #f8fafc;
}

.result-summary .is-wide {
  grid-column: 1 / -1;
}

.result-summary strong {
  display: block;
  margin-top: 6px;
  color: #0f172a;
  font-size: 14px;
  line-height: 1.7;
}

.timeline-list {
  display: grid;
  gap: 16px;
}

.timeline-item {
  position: relative;
  padding-left: 20px;
}

.timeline-dot {
  position: absolute;
  left: 0;
  top: 5px;
  width: 10px;
  height: 10px;
  border-radius: 999px;
}

.timeline-dot.is-on { background: #002660; }
.timeline-dot.is-alert { background: #ba1a1a; }
.timeline-dot.is-muted { background: #cbd5e1; }

.timeline-item strong {
  display: block;
  color: #0f172a;
  font-size: 13px;
}

.timeline-item small {
  display: block;
  margin-top: 4px;
  color: #64748b;
  font-size: 11px;
}

.timeline-item p,
.plan-list li {
  margin: 6px 0 0;
  color: #475569;
  font-size: 12px;
  line-height: 1.7;
}

.timeline-empty {
  color: #64748b;
  font-size: 12px;
  padding: 4px 0;
}

.plan-list {
  display: grid;
  gap: 8px;
  margin: 0;
  padding-left: 18px;
}

.kv-list {
  display: grid;
  gap: 12px;
  margin: 0;
}

.kv-list div {
  display: grid;
  gap: 6px;
  padding: 12px;
  border: 1px solid #e8edf3;
  background: #f8fafc;
}

.kv-list dd {
  margin: 0;
  color: #0f172a;
  font-size: 13px;
  line-height: 1.65;
  word-break: break-word;
}

.kv-list small {
  color: #64748b;
  font-size: 11px;
  line-height: 1.5;
}

.kv-list--compact {
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}

.kv-list--compact div {
  gap: 4px;
  padding: 10px 12px;
  min-height: 84px;
  align-content: start;
}

.kv-list--compact div.is-wide {
  grid-column: 1 / -1;
  min-height: 0;
}

.kv-list--compact dd {
  font-size: 12px;
  line-height: 1.55;
}

.status-banner {
  padding: 10px 12px;
  border: 1px solid #dbe3ee;
  background: #f8fafc;
  color: #334155;
  font-size: 13px;
}

.status-banner.is-error {
  border-color: #fecaca;
  background: #fef2f2;
  color: #b91c1c;
}

.status-banner.is-success {
  border-color: #bbf7d0;
  background: #ecfdf5;
  color: #166534;
}

@media (max-width: 1280px) {
  .mission-board,
  .content-grid {
    grid-template-columns: 1fr;
  }

  .snapshot-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 720px) {
  h1 { font-size: 26px; }
  .mission-board { padding: 18px; }
  .mission-board h2 { font-size: 26px; }

  .snapshot-grid,
  .result-summary,
  .kv-list--compact {
    grid-template-columns: 1fr;
  }
}
</style>
