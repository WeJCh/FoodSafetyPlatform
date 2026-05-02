<template>
  <RegulatorAdminWorkspacePage
    active-key="dispatch"
    :username="regulatorUser.username || regulatorUser.realName || ''"
    @navigate="handleSidebarNavigate"
    @logout="handleLogout"
  >
    <div class="task-detail-page">
      <header class="task-hero">
        <div>
          <div class="chips">
            <span class="chip chip-id">{{ task?.taskNo || `#TASK-${taskId || "-"}` }}</span>
            <span class="chip chip-status" :class="`is-${String(task?.status || '').toLowerCase()}`">
              {{ formatTaskStatus(task?.status) }}
            </span>
            <span v-if="inspectionRecord?.record?.id" class="chip chip-record">
              已生成检查记录
            </span>
          </div>
          <h1>{{ task?.taskTitle || "检查任务详情" }}</h1>
          <div class="hero-meta">
            <span>企业：{{ task?.enterpriseName || "-" }}</span>
            <span>检查类型：{{ task?.taskType || "日常监督检查" }}</span>
            <span class="deadline">截止时间：{{ formatTime(task?.deadline) }}</span>
          </div>
        </div>
        <div class="hero-actions">
          <button class="btn-ghost" type="button" @click="goBack">返回列表</button>
          <button
            v-if="task?.enterpriseId"
            class="btn-ghost"
            type="button"
            @click="openEnterprise"
          >
            查看企业档案
          </button>
          <button
            v-if="canClose"
            class="btn-primary"
            type="button"
            :disabled="actionLoading"
            @click="handleCloseTask"
          >
            归档任务
          </button>
        </div>
      </header>

      <div v-if="loading" class="status info">任务详情加载中...</div>
      <div v-else-if="!task" class="status error">未找到该任务或当前账号无权查看。</div>

      <section v-else class="detail-layout">
        <div class="left">
          <article class="card card-accent">
            <h2>任务要求说明</h2>
            <p>{{ task.taskDesc || "暂无任务描述" }}</p>
            <div class="meta-grid">
              <div>
                <label>负责人员</label>
                <strong>{{ task.assignedToName || "-" }}</strong>
              </div>
              <div>
                <label>任务等级</label>
                <strong>{{ formatTaskPriority(task.priority) }}</strong>
              </div>
              <div>
                <label>结果状态</label>
                <strong>{{ inspectionRecord ? formatInspectionResult(inspectionRecord.record?.result) : "待提交" }}</strong>
              </div>
              <div>
                <label>检查日期</label>
                <strong>{{ inspectionRecord?.record?.inspectionDate || "-" }}</strong>
              </div>
            </div>
          </article>

          <article class="card">
            <div class="card-head">
              <h2>检查项目清单概览</h2>
              <span>{{ inspectionOverviewHint }}</span>
            </div>
            <table class="check-table">
              <thead>
                <tr>
                  <th>检查科目</th>
                  <th>检查要点 / 问题说明</th>
                  <th>结果判定</th>
                </tr>
              </thead>
              <tbody>
                <tr v-if="!inspectionItems.length">
                  <td colspan="3" class="empty-cell">{{ inspectionEmptyText }}</td>
                </tr>
                <tr v-for="item in inspectionItems" :key="item.id || item.name">
                  <td>{{ item.name }}</td>
                  <td>{{ item.desc }}</td>
                  <td>
                    <span class="result-pill" :class="resultClass(item.rawResult)">
                      {{ item.result }}
                    </span>
                  </td>
                </tr>
              </tbody>
            </table>
            <div v-if="inspectionRecord?.record?.problemDesc" class="summary-note">
              <strong>总体问题描述：</strong>{{ inspectionRecord.record.problemDesc }}
            </div>
          </article>
        </div>

        <aside class="right">
          <article class="card">
            <h2>状态流转时间线</h2>
            <ul v-if="timelineItems.length" class="timeline">
              <li v-for="item in timelineItems" :key="item.key">
                <strong>{{ item.title }}</strong>
                <span>{{ item.time }}</span>
                <p>{{ item.desc }}</p>
              </li>
            </ul>
            <div v-else class="timeline-empty">暂无操作日志</div>
          </article>

          <article class="card">
            <h2>企业地理信息</h2>
            <div class="info-list">
              <p><span>详细地址</span><strong>{{ enterprise?.addressDetail || "-" }}</strong></p>
              <p><span>联系电话</span><strong>{{ enterprise?.principalPhone || "-" }}</strong></p>
              <p><span>所属区域</span><strong>{{ regionPathName || "-" }}</strong></p>
            </div>
          </article>
        </aside>
      </section>

      <div v-if="statusMessage" class="status" :class="statusClass">{{ statusMessage }}</div>
    </div>
  </RegulatorAdminWorkspacePage>
</template>

<script setup>
import { computed, ref, watch } from "vue";
import { useRoute, useRouter } from "vue-router";
import RegulatorAdminWorkspacePage from "../../components/regulatorAdmin/RegulatorAdminWorkspacePage.vue";
import { fetchEnterpriseDetail, fetchRegionPath } from "../../api/regulation";
import {
  closeInspectionTask,
  fetchInspectionRecordDetail,
  fetchOperationAuditLogs,
  findInspectionRecordByTaskId,
  findInspectionTaskById
} from "../../api/regulationOperation";
import { formatTime } from "../../utils/formatters";
import { formatStatusLabel, inspectionResultMap, inspectionTaskStatusMap, taskPriorityMap } from "../../utils/statusMaps";
import { resolveErrorMessage } from "../../utils/uiFeedback";
import { useRegulatorAdminShellSession } from "./regulatorAdminShared";

const route = useRoute();
const router = useRouter();
const loading = ref(false);
const actionLoading = ref(false);
const task = ref(null);
const enterprise = ref(null);
const regionPathName = ref("-");
const statusMessage = ref("");
const statusClass = ref("info");
const inspectionRecord = ref(null);
const auditLogs = ref([]);
const taskId = computed(() => Number(route.params.taskId));

const { regulatorUser, token, handleSidebarNavigate, handleLogout } = useRegulatorAdminShellSession();

const canClose = computed(() => task.value?.status === "COMPLETED");
const timelineItems = computed(() =>
  (auditLogs.value || []).map((item, index) => ({
    key: `${item.id || item.actionType || "inspection"}-${index}`,
    title: formatInspectionAuditTitle(item),
    time: formatTime(item.createTime) || "-",
    desc: item.summary || item.remark || item.actionName || item.actionType || "检查任务操作"
  }))
);

const inspectionItems = computed(() =>
  (inspectionRecord.value?.items || []).map((item, index) => ({
    id: item.id || `${item.itemName || "item"}-${index}`,
    name: item.itemName || "-",
    desc: item.problemDesc || "未记录异常问题",
    result: formatInspectionResult(item.itemResult),
    rawResult: String(item.itemResult || "").toUpperCase()
  }))
);

const inspectionOverviewHint = computed(() => {
  if (inspectionRecord.value?.record?.id) {
    return `当前展示的是已归档检查记录中的 ${inspectionItems.value.length} 项检查明细。`;
  }
  return "当前任务尚未生成检查记录，执法人员提交检查结果后这里会展示真实检查项。";
});

const inspectionEmptyText = computed(() => {
  if (inspectionRecord.value?.record?.id) {
    return "当前检查记录未回传检查项明细。";
  }
  return "当前任务还没有已提交的检查项。";
});

function formatTaskStatus(value) {
  return formatStatusLabel(value, inspectionTaskStatusMap);
}

function formatTaskPriority(value) {
  return formatStatusLabel(value, taskPriorityMap);
}

function formatInspectionResult(value) {
  return formatStatusLabel(String(value || "").toUpperCase(), inspectionResultMap);
}

function resultClass(value) {
  if (value === "PASS") return "is-pass";
  if (value === "FAIL") return "is-fail";
  return "is-default";
}

function setStatus(message = "", type = "info") {
  statusMessage.value = message;
  statusClass.value = type;
}

async function loadInspectionRecord(taskDetail) {
  inspectionRecord.value = null;
  if (!taskDetail?.id) return;

  const recordSummary = await findInspectionRecordByTaskId(token.value, taskDetail.id).catch(() => null);
  if (!recordSummary?.id) return;

  inspectionRecord.value = await fetchInspectionRecordDetail(token.value, recordSummary.id).catch(() => ({
    record: recordSummary,
    items: []
  }));
}

async function loadTaskDetail() {
  if (!taskId.value) {
    task.value = null;
    enterprise.value = null;
    regionPathName.value = "-";
    inspectionRecord.value = null;
    auditLogs.value = [];
    setStatus("任务参数无效。", "error");
    return;
  }

  loading.value = true;
  setStatus("");
  task.value = null;
  enterprise.value = null;
  regionPathName.value = "-";
  inspectionRecord.value = null;
  auditLogs.value = [];

  try {
    const detail = await findInspectionTaskById(token.value, taskId.value);
    task.value = detail;

    if (detail?.enterpriseId) {
      const enterpriseDetail = await fetchEnterpriseDetail(token.value, detail.enterpriseId).catch(() => null);
      enterprise.value = enterpriseDetail;
      if (enterpriseDetail?.regionId) {
        const path = await fetchRegionPath(token.value, enterpriseDetail.regionId).catch(() => []);
        regionPathName.value = Array.isArray(path) && path.length ? path.map((item) => item.name).join("/") : "-";
      }
    }

    await loadInspectionRecord(detail);
    if (detail?.id) {
      auditLogs.value = await fetchOperationAuditLogs(token.value, "INSPECTION_TASK", detail.id, 12).catch(() => []);
    }

    if (route.query.closed === "1") {
      setStatus("检查任务已归档。", "success");
    }
  } catch (error) {
    setStatus(resolveErrorMessage(error, "加载任务详情失败，请稍后重试。"), "error");
  } finally {
    loading.value = false;
  }
}

function goBack() {
  router.push({ name: "regulator-admin-dispatch" }).catch(() => {});
}

function openEnterprise() {
  if (!task.value?.enterpriseId) return;
  router.push({
    name: "regulator-admin-enterprise-detail",
    params: { enterpriseId: task.value.enterpriseId },
    query: { from: "dispatch" }
  }).catch(() => {});
}

async function handleCloseTask() {
  if (!task.value?.id || !canClose.value) return;
  actionLoading.value = true;
  setStatus("");
  try {
    await closeInspectionTask(token.value, task.value.id);
    await loadTaskDetail();
    router.replace({
      name: "regulator-admin-inspection-task-detail",
      params: { taskId: String(task.value.id) },
      query: { closed: "1" }
    }).catch(() => {});
  } catch (error) {
    setStatus(resolveErrorMessage(error, "归档任务失败"), "error");
  } finally {
    actionLoading.value = false;
  }
}

function formatInspectionAuditTitle(item) {
  const actionType = String(item?.actionType || "").toUpperCase();
  if (actionType === "INSPECTION_ASSIGN") return "任务分派";
  if (actionType === "INSPECTION_START") return "开始处理";
  if (actionType === "INSPECTION_SUBMIT") return "提交检查结果";
  if (actionType === "INSPECTION_RECTIFICATION_CREATE") return "触发整改任务";
  return item?.actionName || item?.actionType || "检查任务日志";
}

watch(
  () => [route.params.taskId, route.query.closed],
  () => {
    loadTaskDetail();
  },
  { immediate: true }
);
</script>

<style scoped>
.task-detail-page { display: grid; gap: 20px; width: 100%; }
.task-hero {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16px;
  padding: 24px;
  border-radius: 12px;
  background: linear-gradient(135deg, #052b67, #0d4d9f);
  color: #fff;
}
.chips { display: flex; flex-wrap: wrap; gap: 8px; margin-bottom: 12px; }
.chip {
  display: inline-flex;
  align-items: center;
  min-height: 28px;
  padding: 0 10px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.14);
  font-size: 12px;
  font-weight: 700;
}
.chip-status.is-completed,
.chip-status.is-closed { background: rgba(22, 163, 74, 0.22); }
.chip-status.is-in_progress { background: rgba(251, 191, 36, 0.22); }
.chip-status.is-assigned { background: rgba(59, 130, 246, 0.22); }
.chip-record { background: rgba(14, 165, 233, 0.22); }
.task-hero h1 { margin: 0; font-size: 30px; line-height: 1.2; }
.hero-meta { display: flex; flex-wrap: wrap; gap: 14px; margin-top: 10px; font-size: 13px; color: rgba(255, 255, 255, 0.88); }
.hero-actions { display: flex; flex-wrap: wrap; justify-content: flex-end; gap: 10px; }
.btn-ghost,
.btn-primary {
  min-height: 38px;
  border-radius: 8px;
  border: 0;
  padding: 0 14px;
  font-size: 13px;
  font-weight: 700;
  cursor: pointer;
}
.btn-ghost { background: rgba(255, 255, 255, 0.14); color: #fff; }
.btn-primary { background: #fff; color: #0b3f88; }
.btn-primary:disabled,
.btn-ghost:disabled { opacity: 0.6; cursor: not-allowed; }
.detail-layout {
  display: grid;
  width: 100%;
  grid-template-columns: minmax(0, 1.7fr) 360px;
  gap: 16px;
  align-items: start;
  justify-items: stretch;
}
.left,
.right {
  display: grid;
  gap: 16px;
  min-width: 0;
  align-content: start;
  justify-items: stretch;
}
.card {
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  background: #fff;
  padding: 18px;
  box-shadow: 0 10px 30px rgba(15, 23, 42, 0.05);
  width: 100%;
}
.card-accent {
  background: linear-gradient(180deg, #f8fbff, #ffffff);
  border-color: #cfe0f6;
}
.card h2 { margin: 0 0 12px; color: #0f172a; font-size: 18px; }
.card p { margin: 0; color: #334155; line-height: 1.7; }
.card-head { display: flex; justify-content: space-between; gap: 12px; align-items: flex-start; margin-bottom: 12px; }
.card-head span { color: #64748b; font-size: 12px; line-height: 1.6; }
.meta-grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 12px; margin-top: 16px; }
.meta-grid div {
  border-radius: 10px;
  background: rgba(255, 255, 255, 0.78);
  border: 1px solid #e2e8f0;
  padding: 12px;
}
.meta-grid label { display: block; color: #64748b; font-size: 12px; margin-bottom: 6px; }
.meta-grid strong { color: #0f172a; font-size: 14px; }
.check-table { width: 100%; border-collapse: collapse; table-layout: fixed; }
.check-table th:nth-child(1),
.check-table td:nth-child(1) { width: 26%; }
.check-table th:nth-child(2),
.check-table td:nth-child(2) { width: 50%; }
.check-table th:nth-child(3),
.check-table td:nth-child(3) { width: 24%; }
.check-table th,
.check-table td {
  padding: 12px 10px;
  border-top: 1px solid #e2e8f0;
  text-align: left;
  vertical-align: top;
  word-break: break-word;
}
.check-table th {
  padding: 12px 10px;
  border-top: 1px solid #e2e8f0;
  text-align: left;
  vertical-align: top;
}
.check-table thead th { border-top: 0; color: #64748b; font-size: 12px; font-weight: 800; background: #f8fafc; }
.empty-cell { text-align: center; color: #94a3b8; }
.result-pill {
  display: inline-flex;
  align-items: center;
  min-height: 26px;
  padding: 0 10px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 700;
}
.result-pill.is-pass { background: #dcfce7; color: #166534; }
.result-pill.is-fail { background: #fee2e2; color: #991b1b; }
.result-pill.is-default { background: #e2e8f0; color: #475569; }
.summary-note {
  margin-top: 14px;
  padding: 12px 14px;
  border-radius: 10px;
  background: #f8fafc;
  color: #334155;
  line-height: 1.7;
}
.timeline { list-style: none; padding: 0; margin: 0; display: grid; gap: 14px; }
.timeline li {
  position: relative;
  padding-left: 16px;
  border-left: 2px solid #dbeafe;
}
.timeline li::before {
  content: "";
  position: absolute;
  left: -6px;
  top: 4px;
  width: 10px;
  height: 10px;
  border-radius: 50%;
  background: #2563eb;
}
.timeline strong { display: block; color: #0f172a; font-size: 14px; }
.timeline span { display: block; margin-top: 4px; color: #64748b; font-size: 12px; }
.timeline p { margin-top: 6px; color: #334155; font-size: 13px; line-height: 1.7; }
.timeline-empty { color: #94a3b8; font-size: 13px; }
.info-list { display: grid; gap: 12px; }
.info-list p {
  display: grid;
  gap: 6px;
  padding: 12px;
  border-radius: 10px;
  background: #f8fafc;
}
.info-list span { color: #64748b; font-size: 12px; }
.info-list strong { color: #0f172a; font-size: 14px; line-height: 1.6; }
.status {
  padding: 12px 14px;
  border-radius: 10px;
  font-size: 13px;
  font-weight: 600;
}
.status.info { background: #eff6ff; color: #1d4ed8; }
.status.success { background: #ecfdf5; color: #047857; }
.status.error { background: #fef2f2; color: #b91c1c; }

@media (max-width: 1200px) {
  .detail-layout { grid-template-columns: 1fr; }
}

@media (max-width: 768px) {
  .task-hero { padding: 18px; flex-direction: column; }
  .task-hero h1 { font-size: 24px; }
  .hero-actions { width: 100%; justify-content: flex-start; }
  .meta-grid { grid-template-columns: 1fr; }
}
</style>
