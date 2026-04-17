<template>
  <RegulatorEnforcerWorkspacePage
    active-key="tasks"
    :username="enforcerUser.username || enforcerUser.realName || ''"
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
            <span
              v-if="inspectionRecord?.record?.id"
              class="chip chip-record"
            >
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
            v-if="inspectionRecord?.record?.id"
            class="btn-ghost"
            type="button"
            @click="openInspectionRecord"
          >
            查看检查记录
          </button>
          <button
            class="btn-primary"
            type="button"
            :disabled="!canSubmit"
            @click="goSubmit"
          >
            提交检查结果
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
                <label>执行人员</label>
                <strong>{{ enforcerUser.realName || enforcerUser.username || "执法人员" }}</strong>
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
              <h2>检查项清单概览</h2>
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
            <ul class="timeline">
              <li>
                <strong>任务状态：{{ formatTaskStatus(task.status) }}</strong>
                <span>{{ formatTime(task.updateTime || task.createTime) }}</span>
                <p>系统记录该任务最近一次状态变更。</p>
              </li>
              <li>
                <strong>任务下达</strong>
                <span>{{ formatTime(task.createTime) }}</span>
                <p>任务由派发中心下发至当前执法人员。</p>
              </li>
              <li>
                <strong>检查结果提交</strong>
                <span>{{ inspectionRecord?.record?.inspectionDate || (canSubmit ? "待处理" : formatTaskStatus(task.status)) }}</span>
                <p>
                  {{ inspectionRecord?.record?.id
                    ? `已生成检查记录 #${inspectionRecord.record.id}，检查结论为${formatInspectionResult(inspectionRecord.record.result)}。`
                    : "当前任务尚未提交检查结果，提交后会同步生成检查记录。"
                  }}
                </p>
              </li>
            </ul>
          </article>

          <article class="card">
            <h2>企业地理信息</h2>
            <div class="info-list">
              <p><span>详细地址</span><strong>{{ enterprise?.addressDetail || "-" }}</strong></p>
              <p><span>联系电话</span><strong>{{ enterprise?.principalPhone || "-" }}</strong></p>
              <p><span>所属区域</span><strong>{{ regionPathName || "-" }}</strong></p>
            </div>
            <button class="btn-ghost full" type="button" @click="openEnterprise">查看企业完整档案</button>
          </article>
        </aside>
      </section>

      <div v-if="statusMessage" class="status" :class="statusClass">{{ statusMessage }}</div>
    </div>
  </RegulatorEnforcerWorkspacePage>
</template>

<script setup>
import { computed, ref, watch } from "vue";
import { useRoute, useRouter } from "vue-router";
import RegulatorEnforcerWorkspacePage from "../../components/regulatorEnforcer/RegulatorEnforcerWorkspacePage.vue";
import { fetchEnterpriseDetail, fetchRegionPath } from "../../api/regulation";
import {
  fetchInspectionRecordDetail,
  findMyInspectionRecordByTaskId,
  findMyInspectionTaskById
} from "../../api/regulationOperation";
import { formatTime } from "../../utils/formatters";
import { useRegulatorEnforcerShellSession } from "./regulatorEnforcerShared";

const route = useRoute();
const router = useRouter();
const loading = ref(false);
const task = ref(null);
const enterprise = ref(null);
const regionPathName = ref("-");
const statusMessage = ref("");
const statusClass = ref("info");
const inspectionRecord = ref(null);
const taskId = computed(() => Number(route.params.taskId));

const { enforcerUser, token, handleSidebarNavigate, handleLogout } = useRegulatorEnforcerShellSession();

const taskStatusMap = {
  CREATED: "待启动",
  ASSIGNED: "已指派",
  IN_PROGRESS: "执行中",
  COMPLETED: "已完成",
  CLOSED: "已归档"
};

const taskPriorityMap = {
  LOW: "低优先级",
  MEDIUM: "中优先级",
  HIGH: "高优先级"
};

const inspectionResultMap = {
  PASS: "合格",
  FAIL: "不合格"
};

const canSubmit = computed(() => task.value?.status === "IN_PROGRESS");

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
  return "当前任务尚未生成检查记录，提交检查结果后这里会展示真实检查项。";
});

const inspectionEmptyText = computed(() => {
  if (inspectionRecord.value?.record?.id) {
    return "当前检查记录未回传检查项明细。";
  }
  return "当前任务还没有已提交的检查项。";
});

function formatTaskStatus(value) {
  return taskStatusMap[value] || value || "-";
}

function formatTaskPriority(value) {
  return taskPriorityMap[value] || value || "-";
}

function formatInspectionResult(value) {
  return inspectionResultMap[String(value || "").toUpperCase()] || value || "-";
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

  const recordSummary = await findMyInspectionRecordByTaskId(token.value, taskDetail.id).catch(() => null);
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
    setStatus("任务参数无效。", "error");
    return;
  }

  loading.value = true;
  setStatus("");
  task.value = null;
  enterprise.value = null;
  regionPathName.value = "-";
  inspectionRecord.value = null;

  try {
    const detail = await findMyInspectionTaskById(token.value, taskId.value);
    task.value = detail;

    if (detail?.enterpriseId) {
      const en = await fetchEnterpriseDetail(token.value, detail.enterpriseId).catch(() => null);
      enterprise.value = en;
      if (en?.regionId) {
        const path = await fetchRegionPath(token.value, en.regionId).catch(() => []);
        regionPathName.value = Array.isArray(path) && path.length ? path.map((item) => item.name).join("/") : "-";
      }
    }

    await loadInspectionRecord(detail);

    if (route.query.submitted === "1") {
      if (inspectionRecord.value?.record?.id) {
        setStatus("检查结果已提交，当前页面已同步回显最新检查项与检查结论。", "success");
      } else {
        setStatus("检查结果已提交，任务状态已更新；若检查记录尚未显示，请稍后刷新或进入检查记录页确认。", "info");
      }
    }
  } catch (error) {
    setStatus(error?.message || "加载任务详情失败", "error");
  } finally {
    loading.value = false;
  }
}

function goBack() {
  router.push({ name: "regulator-enforcer-tasks" }).catch(() => {});
}

function goSubmit() {
  if (!task.value?.id || !canSubmit.value) return;
  router.push({
    name: "regulator-enforcer-task-submit",
    params: { taskId: task.value.id }
  }).catch(() => {});
}

function openEnterprise() {
  if (!task.value?.enterpriseId) return;
  router.push({
    name: "regulator-enforcer-enterprise-detail",
    params: { enterpriseId: task.value.enterpriseId },
    query: { from: "tasks" }
  }).catch(() => {});
}

function openInspectionRecord() {
  if (!inspectionRecord.value?.record?.id) return;
  router.push({
    name: "regulator-enforcer-inspection-record-detail",
    params: { inspectionId: inspectionRecord.value.record.id }
  }).catch(() => {});
}

watch(
  () => [route.params.taskId, route.query.submitted],
  () => {
    loadTaskDetail();
  },
  { immediate: true }
);
</script>

<style scoped>
.task-detail-page { min-height: calc(100vh - 108px); }
.task-hero { display: flex; justify-content: space-between; gap: 16px; padding: 16px; border: 1px solid #dbe3ee; background: #fff; }
.chips { display: flex; gap: 8px; margin-bottom: 8px; flex-wrap: wrap; }
.chip { font-size: 11px; font-weight: 700; padding: 2px 8px; border-radius: 2px; }
.chip-id { background: #dbeafe; color: #1e3a8a; }
.chip-status { background: #e2e8f0; color: #334155; }
.chip-status.is-in_progress { background: #dbeafe; color: #1d4ed8; }
.chip-status.is-completed,
.chip-status.is-closed { background: #dcfce7; color: #166534; }
.chip-record { background: #eef2ff; color: #4338ca; }
h1 { margin: 0; color: #002660; font-size: 30px; font-weight: 800; }
.hero-meta { margin-top: 8px; display: flex; gap: 16px; font-size: 13px; color: #64748b; flex-wrap: wrap; }
.hero-meta .deadline { color: #b91c1c; font-weight: 600; }
.hero-actions { display: flex; align-items: flex-start; gap: 10px; flex-wrap: wrap; }
.btn-primary,.btn-ghost { height: 36px; padding: 0 14px; border: 1px solid #cbd5e1; border-radius: 2px; cursor: pointer; }
.btn-primary { background: #002660; color: #fff; border-color: #002660; font-weight: 700; }
.btn-primary:disabled { opacity: 0.45; cursor: not-allowed; }
.btn-ghost { background: #fff; color: #334155; }
.detail-layout { margin-top: 14px; display: grid; grid-template-columns: 2fr 1fr; gap: 14px; }
.left,.right { display: grid; gap: 14px; align-content: start; }
.card { background: #fff; border: 1px solid #dbe3ee; padding: 16px; }
.card-accent { border-left: 4px solid #003a8c; }
h2 { margin: 0 0 10px; font-size: 14px; color: #003a8c; font-weight: 800; }
.meta-grid { margin-top: 12px; display: grid; grid-template-columns: 1fr 1fr; gap: 10px; background: #f8fafc; border: 1px solid #e2e8f0; padding: 10px; }
.meta-grid label { font-size: 11px; color: #64748b; display: block; }
.meta-grid strong { font-size: 13px; color: #0f172a; }
.card-head { display: flex; justify-content: space-between; align-items: center; gap: 8px; }
.card-head span { font-size: 11px; color: #64748b; }
.check-table { width: 100%; border-collapse: collapse; font-size: 12px; }
.check-table th,.check-table td { border-bottom: 1px solid #e2e8f0; padding: 10px; text-align: left; vertical-align: top; }
.empty-cell { text-align: center; color: #64748b; padding: 18px 10px; }
.result-pill { display: inline-flex; align-items: center; justify-content: center; min-height: 24px; padding: 0 8px; border-radius: 2px; background: #f1f5f9; color: #334155; font-weight: 700; border: 1px solid transparent; }
.result-pill.is-pass { background: #f0fdf4; color: #166534; border-color: #86efac; }
.result-pill.is-fail { background: #fef2f2; color: #b91c1c; border-color: #fecaca; }
.summary-note { margin: 12px 0 0; padding: 10px 12px; background: #f8fafc; border: 1px solid #e2e8f0; color: #334155; font-size: 12px; line-height: 1.6; }
.timeline { margin: 0; padding-left: 16px; border-left: 2px solid #e2e8f0; display: grid; gap: 12px; }
.timeline li { list-style: none; position: relative; }
.timeline li::before { content: ""; position: absolute; left: -22px; top: 3px; width: 8px; height: 8px; border-radius: 999px; background: #1d4ed8; }
.timeline strong { display: block; font-size: 13px; color: #0f172a; }
.timeline span { font-size: 11px; color: #64748b; }
.timeline p { margin: 4px 0 0; font-size: 12px; color: #475569; line-height: 1.6; }
.info-list p { margin: 0 0 8px; display: flex; justify-content: space-between; gap: 8px; font-size: 12px; }
.info-list span { color: #64748b; }
.info-list strong { color: #0f172a; text-align: right; }
.full { width: 100%; margin-top: 8px; }
.status { margin-top: 12px; border: 1px solid #fecaca; background: #fef2f2; color: #b91c1c; padding: 10px 12px; }
.status.info { border-color: #bfdbfe; background: #eff6ff; color: #1d4ed8; }
.status.success { border-color: #bbf7d0; background: #f0fdf4; color: #166534; }
@media (max-width: 1200px) {
  .detail-layout { grid-template-columns: 1fr; }
  .task-hero { flex-direction: column; }
}
</style>
