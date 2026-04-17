<template>
  <RegulatorEnforcerWorkspacePage
    active-key="sampling"
    :username="enforcerUser.username || enforcerUser.realName || ''"
    @navigate="handleSidebarNavigate"
    @logout="handleLogout"
    @pending-feature="regulatorEnforcerFeaturePendingNotice"
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
          <button
            v-if="task?.enterpriseId"
            class="ghost-btn"
            type="button"
            @click="openEnterpriseDetail"
          >
            查看企业档案
          </button>
          <button
            v-if="showResultEntry"
            class="primary-btn"
            type="button"
            @click="openResultEntry"
          >
            {{ resultFormVisible ? "收起结果录入" : "开始采样 / 录入结果" }}
          </button>
          <button
            v-if="task?.status === 'COMPLETED'"
            class="primary-btn primary-btn--secondary"
            type="button"
            :disabled="actionLoading"
            @click="handleCloseTask"
          >
            归档任务
          </button>
        </div>
      </header>

      <div v-if="pageLoading" class="state-card">抽检任务详情加载中...</div>
      <div v-else-if="loadError" class="state-card state-card--error">{{ loadError }}</div>

      <template v-else-if="task">
        <div class="hero-grid">
          <section class="hero-card">
            <div class="hero-card__media">
              <div class="hero-card__tag">{{ task.productCategory || "抽检品类" }}</div>
              <div class="hero-card__placeholder">抽检样品</div>
            </div>
            <div class="hero-card__content">
              <div class="hero-card__top">
                <div>
                  <div class="hero-card__chips">
                    <span class="chip chip--primary">{{ formatSamplingTaskStatus(task.status) }}</span>
                    <span class="chip chip--plain">{{ task.taskNo || `#${task.id}` }}</span>
                  </div>
                  <h2>{{ task.productName || "待抽检样品" }}</h2>
                  <p>{{ task.productSpecification || "暂无规格信息" }}</p>
                </div>
                <div class="hero-batch">
                  <span>批次标识</span>
                  <strong>{{ task.productBatchNo || task.taskNo || "-" }}</strong>
                </div>
              </div>
              <div class="hero-metrics">
                <article>
                  <span>采样时间</span>
                  <strong>{{ formatTime(task.sampledTime) || "待录入" }}</strong>
                </article>
                <article>
                  <span>截止完成时间</span>
                  <strong>{{ formatTime(task.deadline) }}</strong>
                </article>
                <article>
                  <span>执行人员</span>
                  <strong>{{ task.assignedToName || enforcerUser.realName || enforcerUser.username || "-" }}</strong>
                </article>
              </div>
            </div>
          </section>

          <aside class="timeline-card">
            <div class="timeline-card__head">
              <h3>状态流转记录</h3>
              <span>共 {{ timeline.length }} 条记录</span>
            </div>
            <div class="timeline-list">
              <article v-for="(item, index) in timeline" :key="`${item.title}-${index}`" class="timeline-item">
                <span class="timeline-dot" :class="item.dotClass"></span>
                <div>
                  <strong>{{ item.title }}</strong>
                  <small>{{ item.time }}</small>
                  <p>{{ item.desc }}</p>
                </div>
              </article>
            </div>
            <div class="assign-card">
              <span>指派说明</span>
              <p>{{ task.taskDesc || "当前任务由专项抽检计划自动生成，请按要求完成采样与结果录入。" }}</p>
            </div>
          </aside>
        </div>

        <div class="content-grid">
          <div class="main-column">
            <section class="panel time-card">
              <div>
                <span>截止完成时间</span>
                <strong>{{ formatTime(task.deadline) }}</strong>
              </div>
              <div class="time-card__right">
                <span>剩余时间</span>
                <strong :class="{ danger: deadlineInfo.tone === 'danger', warning: deadlineInfo.tone === 'warning' }">
                  {{ deadlineInfo.label }}
                </strong>
              </div>
            </section>

            <section v-if="task.samplingResult || task.samplingConclusion" class="panel">
              <div class="panel-head">
                <h3>已录入结果</h3>
                <span>检测结论摘要</span>
              </div>
              <div class="result-summary">
                <article>
                  <span>抽检结论</span>
                  <strong>{{ formatInspectionResult(task.samplingResult) }}</strong>
                </article>
                <article class="is-wide">
                  <span>结果说明</span>
                  <strong>{{ task.samplingConclusion || "暂无说明" }}</strong>
                </article>
              </div>
            </section>

            <section v-if="resultFormVisible" class="panel panel--form">
              <div class="panel-head">
                <h3>抽检结果录入</h3>
                <span>录入后自动回写任务状态</span>
              </div>
              <form class="result-form" @submit.prevent="handleSubmitSamplingResult">
                <label>
                  采样时间
                  <input v-model="samplingForm.sampledTime" type="datetime-local" required />
                </label>
                <label>
                  抽检结论
                  <select v-model="samplingForm.result">
                    <option value="PASS">合格</option>
                    <option value="FAIL">不合格</option>
                  </select>
                </label>
                <label class="full">
                  结果说明
                  <textarea
                    v-model.trim="samplingForm.conclusion"
                    rows="4"
                    placeholder="请填写抽检结论、样品情况与现场说明"
                  ></textarea>
                </label>
                <label class="full">
                  处置建议
                  <textarea
                    v-model.trim="samplingForm.disposalSuggestion"
                    rows="4"
                    placeholder="请填写后续处置建议"
                  ></textarea>
                </label>
                <div class="form-actions">
                  <button class="primary-btn" type="submit" :disabled="actionLoading">提交结果</button>
                </div>
              </form>
            </section>
          </div>

          <aside class="side-column">
            <section class="panel panel--soft">
              <div class="panel-head">
                <h3>任务执行建议</h3>
                <span>执法视角提示</span>
              </div>
              <ul class="plan-list">
                <li v-for="(item, index) in planLines" :key="index">{{ item }}</li>
              </ul>
            </section>

            <section class="panel">
              <div class="panel-head">
                <h3>任务概览</h3>
                <span>当前信息快照</span>
              </div>
              <dl class="kv-list">
                <div><dt>任务标题</dt><dd>{{ task.taskTitle || "-" }}</dd></div>
                <div><dt>抽检产品</dt><dd>{{ task.productName || "-" }}</dd></div>
                <div><dt>产品分类</dt><dd>{{ task.productCategory || "-" }}</dd></div>
                <div><dt>规格型号</dt><dd>{{ task.productSpecification || "-" }}</dd></div>
                <div><dt>结果状态</dt><dd>{{ formatInspectionResult(task.samplingResult) }}</dd></div>
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
import {
  closeSamplingTask,
  findMySamplingTaskById,
  submitSamplingResult
} from "../../api/regulationOperation";
import RegulatorEnforcerWorkspacePage from "../../components/regulatorEnforcer/RegulatorEnforcerWorkspacePage.vue";
import { formatTime } from "../../utils/formatters";
import {
  regulatorEnforcerFeaturePendingNotice,
  useRegulatorEnforcerShellSession
} from "./regulatorEnforcerShared";

const route = useRoute();
const router = useRouter();
const { enforcerUser, token, handleSidebarNavigate, handleLogout } = useRegulatorEnforcerShellSession();

const pageLoading = ref(true);
const loadError = ref("");
const actionLoading = ref(false);
const task = ref(null);
const enterprise = ref(null);
const regionLabel = ref("-");
const resultFormVisible = ref(false);
const status = reactive({ message: "", type: "info" });
const samplingForm = reactive({
  sampledTime: "",
  result: "PASS",
  conclusion: "",
  disposalSuggestion: ""
});

const samplingTaskStatusMap = {
  CREATED: "待派发",
  ASSIGNED: "待抽检",
  IN_PROGRESS: "抽检中",
  COMPLETED: "已完成",
  CLOSED: "已归档"
};
const inspectionResultMap = {
  PASS: "合格",
  FAIL: "不合格"
};

const showResultEntry = computed(() => {
  const value = String(task.value?.status || "").toUpperCase();
  return value === "ASSIGNED" || value === "IN_PROGRESS";
});

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

const timeline = computed(() => {
  const current = task.value;
  if (!current) return [];

  const rows = [
    {
      title: "任务已下达",
      time: formatTime(current.assignedTime || current.createTime),
      desc: current.taskDesc || "系统根据抽检任务计划生成当前任务。",
      dotClass: "is-on"
    }
  ];

  if (current.sampledTime) {
    rows.push({
      title: "采样完成",
      time: formatTime(current.sampledTime),
      desc: `${current.assignedToName || "当前执法人员"} 已录入采样时间。`,
      dotClass: "is-on"
    });
  } else {
    rows.push({
      title: "采样中",
      time: "待录入",
      desc: "当前尚未填写采样时间，请在现场完成后及时回填。",
      dotClass: "is-muted"
    });
  }

  if (current.samplingResult) {
    rows.push({
      title: `结果录入：${formatInspectionResult(current.samplingResult)}`,
      time: formatTime(current.updateTime),
      desc: current.samplingConclusion || "已提交抽检结论。",
      dotClass: current.samplingResult === "FAIL" ? "is-alert" : "is-on"
    });
  } else {
    rows.push({
      title: "结果录入",
      time: "待完成",
      desc: "抽检结论尚未提交。",
      dotClass: "is-muted"
    });
  }

  rows.push({
    title: current.status === "CLOSED" ? "任务已归档" : "任务归档",
    time: current.status === "CLOSED" ? formatTime(current.updateTime) : "待完成",
    desc: current.status === "CLOSED" ? "当前任务已归档，可供后续审计查询。" : "抽检完成后可归档收尾。",
    dotClass: current.status === "CLOSED" ? "is-on" : "is-muted"
  });

  return rows;
});

const planLines = computed(() => {
  const current = task.value;
  if (!current) return ["加载任务建议中..."];
  if (current.samplingResult === "FAIL") {
    return [
      `通知“${current.enterpriseName || enterprise.value?.enterpriseName || "企业"}”开展复核并准备后续整改。`,
      "保留现场采样、封样与流转证据，必要时同步风险预警与整改流程。",
      "在结果确认前复核样品信息、批次和现场影像材料。"
    ];
  }
  if (current.samplingResult === "PASS") {
    return [
      "整理抽检材料并归档，纳入该企业日常监管记录。",
      "无需额外处置时，完成归档后可回到任务列表继续后续任务。"
    ];
  }
  return [
    "核对样品信息、企业主体和批次信息后再录入抽检结果。",
    "如现场发现异常，请同步补充处置建议，便于后续联动。"
  ];
});

function setStatus(message = "", type = "info") {
  status.message = message;
  status.type = type;
}

function formatSamplingTaskStatus(value) {
  return samplingTaskStatusMap[value] || value || "-";
}

function formatInspectionResult(value) {
  return inspectionResultMap[value] || (value ? value : "待录入");
}

function normalizeDateTimeInput(value) {
  return value && value.length === 16 ? `${value}:00` : value;
}

function formatNowForDateTimeLocal() {
  const now = new Date();
  const offset = now.getTimezoneOffset() * 60 * 1000;
  return new Date(now.getTime() - offset).toISOString().slice(0, 16);
}

function toggleResultForm() {
  resultFormVisible.value = !resultFormVisible.value;
  if (resultFormVisible.value && !samplingForm.sampledTime) {
    samplingForm.sampledTime = formatNowForDateTimeLocal();
  }
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
  router.push({
    name: "regulator-enforcer-sampling-submit",
    params: { taskId: task.value.id }
  }).catch(() => {});
}

async function loadDetail() {
  pageLoading.value = true;
  loadError.value = "";
  task.value = null;
  enterprise.value = null;
  regionLabel.value = "-";
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
    samplingForm.sampledTime = row.sampledTime ? String(row.sampledTime).slice(0, 16) : "";
    samplingForm.result = row.samplingResult || "PASS";
    samplingForm.conclusion = row.samplingConclusion || "";
    samplingForm.disposalSuggestion = row.disposalSuggestion || "";

    if (route.query.action === "submit" && showResultEntry.value) {
      resultFormVisible.value = true;
      if (!samplingForm.sampledTime) {
        samplingForm.sampledTime = formatNowForDateTimeLocal();
      }
    } else {
      resultFormVisible.value = false;
    }

    if (route.query.submitted === "1") {
      setStatus("抽检结果已提交", "success");
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
    loadError.value = error?.message || "抽检任务详情加载失败";
  } finally {
    pageLoading.value = false;
  }
}

async function handleSubmitSamplingResult() {
  if (!task.value?.id) return;
  if (!samplingForm.sampledTime) {
    setStatus("请选择采样时间", "error");
    return;
  }

  actionLoading.value = true;
  setStatus("");
  try {
    await submitSamplingResult(token.value, task.value.id, {
      sampledTime: normalizeDateTimeInput(samplingForm.sampledTime),
      result: samplingForm.result,
      conclusion: samplingForm.conclusion,
      disposalSuggestion: samplingForm.disposalSuggestion
    });
    setStatus("抽检结果已提交", "success");
    await loadDetail();
  } catch (error) {
    setStatus(error?.message || "提交抽检结果失败", "error");
  } finally {
    actionLoading.value = false;
  }
}

async function handleCloseTask() {
  if (!task.value?.id) return;
  actionLoading.value = true;
  setStatus("");
  try {
    await closeSamplingTask(token.value, task.value.id);
    setStatus("抽检任务已归档", "success");
    await loadDetail();
  } catch (error) {
    setStatus(error?.message || "归档任务失败", "error");
  } finally {
    actionLoading.value = false;
  }
}

watch(() => route.params.taskId, loadDetail, { immediate: true });
</script>

<style scoped>
.sampling-detail-page {
  display: grid;
  gap: 18px;
}
.page-head {
  display: flex;
  justify-content: space-between;
  align-items: end;
  gap: 16px;
  flex-wrap: wrap;
}
.crumbs {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
  font-size: 11px;
  color: #64748b;
}
.crumb-link {
  border: 0;
  background: transparent;
  padding: 0;
  color: inherit;
  cursor: pointer;
}
.crumb-link:hover,
.crumb-current {
  color: #002660;
}
.crumb-sep {
  opacity: 0.5;
}
h1 {
  margin: 0;
  color: #002660;
  font-size: 30px;
  font-weight: 900;
}
.head-actions {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}
.primary-btn,
.ghost-btn {
  min-height: 38px;
  padding: 0 16px;
  border: 1px solid #cbd5e1;
  background: #fff;
  color: #334155;
  font-size: 12px;
  font-weight: 800;
  cursor: pointer;
}
.primary-btn {
  border-color: #002660;
  background: #002660;
  color: #fff;
}
.primary-btn--secondary {
  background: #0f4ea5;
  border-color: #0f4ea5;
}
.state-card {
  padding: 18px;
  border: 1px solid #dbe3ee;
  background: #fff;
  color: #64748b;
}
.state-card--error {
  border-color: #fecaca;
  background: #fef2f2;
  color: #b91c1c;
}
.hero-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.7fr) 360px;
  gap: 16px;
}
.hero-card,
.timeline-card,
.panel,
.time-card {
  border: 1px solid #dbe3ee;
  background: #fff;
}
.hero-card {
  display: grid;
  grid-template-columns: 220px minmax(0, 1fr);
  gap: 18px;
  padding: 20px;
}
.hero-card__media {
  position: relative;
  min-height: 220px;
  background: linear-gradient(135deg, #dfe8f5 0%, #f3f6fb 100%);
  overflow: hidden;
}
.hero-card__tag {
  position: absolute;
  top: 12px;
  left: 12px;
  padding: 4px 8px;
  background: rgba(0, 38, 96, 0.86);
  color: #fff;
  font-size: 10px;
  font-weight: 800;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}
.hero-card__placeholder {
  height: 100%;
  display: grid;
  place-items: center;
  color: #335bae;
  font-size: 22px;
  font-weight: 900;
  letter-spacing: 0.08em;
}
.hero-card__top {
  display: flex;
  justify-content: space-between;
  gap: 16px;
}
.hero-card__chips {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}
.chip {
  display: inline-flex;
  align-items: center;
  min-height: 24px;
  padding: 0 10px;
  border-radius: 999px;
  font-size: 11px;
  font-weight: 800;
}
.chip--primary {
  background: #dbeafe;
  color: #1e3a8a;
}
.chip--plain {
  background: #eef2f7;
  color: #475569;
}
.hero-card h2 {
  margin: 12px 0 0;
  color: #0f172a;
  font-size: 28px;
  font-weight: 900;
}
.hero-card p {
  margin: 6px 0 0;
  color: #64748b;
  font-size: 13px;
}
.hero-batch {
  text-align: right;
}
.hero-batch span,
.hero-metrics span,
.timeline-card__head span,
.panel-head span,
.detail-grid span,
.result-summary span,
.assign-card span,
.kv-list dt {
  display: block;
  color: #64748b;
  font-size: 11px;
  font-weight: 800;
}
.hero-batch strong {
  display: block;
  margin-top: 6px;
  color: #002660;
  font-size: 16px;
  font-family: ui-monospace, SFMono-Regular, Menlo, Consolas, monospace;
}
.hero-metrics {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
  margin-top: 18px;
  padding-top: 18px;
  border-top: 1px solid #e8edf3;
}
.hero-metrics article {
  padding: 12px;
  background: #f8fafc;
}
.hero-metrics strong {
  display: block;
  margin-top: 6px;
  color: #0f172a;
  font-size: 14px;
}
.timeline-card {
  padding: 18px;
}
.timeline-card__head,
.panel-head {
  display: flex;
  justify-content: space-between;
  gap: 10px;
  align-items: center;
  margin-bottom: 14px;
}
.timeline-card__head h3,
.panel-head h3 {
  margin: 0;
  color: #002660;
  font-size: 15px;
  font-weight: 900;
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
.timeline-dot.is-on {
  background: #002660;
}
.timeline-dot.is-alert {
  background: #ba1a1a;
}
.timeline-dot.is-muted {
  background: #cbd5e1;
}
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
  line-height: 1.6;
}
.assign-card {
  margin-top: 18px;
  padding: 12px;
  background: #f8fafc;
}
.assign-card p {
  margin: 6px 0 0;
  color: #334155;
  font-size: 12px;
  line-height: 1.6;
}
.content-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.6fr) 340px;
  gap: 16px;
}
.main-column,
.side-column {
  display: grid;
  gap: 16px;
}
.panel {
  padding: 16px;
}
.panel--soft {
  background: linear-gradient(180deg, #f5f9ff 0%, #edf4ff 100%);
}
.detail-grid,
.result-summary {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}
.detail-grid article,
.result-summary article {
  padding: 12px;
  background: #f8fafc;
}
.detail-grid .is-wide,
.result-summary .is-wide {
  grid-column: 1 / -1;
}
.detail-grid strong,
.result-summary strong,
.kv-list dd {
  display: block;
  margin-top: 6px;
  color: #0f172a;
  font-size: 13px;
}
.time-card {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  padding: 18px 16px;
  border-left: 5px solid #002660;
  background: #f6f9ff;
}
.time-card strong {
  display: block;
  margin-top: 6px;
  color: #002660;
  font-size: 22px;
  font-weight: 900;
}
.time-card__right {
  text-align: right;
}
.time-card strong.warning {
  color: #b45309;
}
.time-card strong.danger {
  color: #b91c1c;
}
.panel--form {
  border-color: #c9d7fe;
}
.result-form {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}
.result-form label {
  display: grid;
  gap: 6px;
  font-size: 12px;
  color: #475569;
  font-weight: 700;
}
.result-form input,
.result-form select,
.result-form textarea {
  min-height: 38px;
  border: 1px solid #d4dce8;
  background: #fff;
  padding: 0 10px;
}
.result-form textarea {
  min-height: 96px;
  padding: 10px;
  resize: vertical;
}
.result-form .full {
  grid-column: 1 / -1;
}
.form-actions {
  display: flex;
  justify-content: flex-end;
  grid-column: 1 / -1;
}
.plan-list {
  margin: 0;
  padding-left: 18px;
}
.kv-list {
  display: grid;
  gap: 10px;
  margin: 0;
}
.kv-list div {
  display: flex;
  justify-content: space-between;
  gap: 10px;
}
.kv-list dd {
  margin: 0;
  text-align: right;
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
  .hero-grid,
  .content-grid {
    grid-template-columns: 1fr;
  }
}
@media (max-width: 860px) {
  .hero-card,
  .hero-metrics,
  .detail-grid,
    .result-summary,
  .result-form {
    grid-template-columns: 1fr;
  }
}
</style>
