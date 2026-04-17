<template>
  <RegulatorEnforcerWorkspacePage
    active-key="sampling"
    :username="enforcerUser.username || enforcerUser.realName || ''"
    @navigate="handleSidebarNavigate"
    @logout="handleLogout"
    @pending-feature="regulatorEnforcerFeaturePendingNotice"
  >
    <section class="sampling-submit-page">
      <header class="page-head">
        <div>
          <nav class="crumbs" aria-label="面包屑">
            <button type="button" class="crumb-link" @click="goList">抽检任务列表</button>
            <span class="crumb-sep">/</span>
            <button v-if="task?.id" type="button" class="crumb-link" @click="goDetail">
              {{ task?.taskNo || "任务详情" }}
            </button>
            <template v-if="task?.id">
              <span class="crumb-sep">/</span>
            </template>
            <span class="crumb-current">结果录入</span>
          </nav>
          <h1>抽样检测结果提交</h1>
          <p>请准确录入实验室检测结论，提交后将进入审核流程。</p>
        </div>
        <div class="head-actions">
          <button class="ghost-btn" type="button" :disabled="actionLoading" @click="goDetail">
            取消
          </button>
          <button class="primary-btn" type="button" :disabled="actionLoading || pageLoading" @click="handleSubmit">
            {{ actionLoading ? "提交中..." : "确认并提交" }}
          </button>
        </div>
      </header>

      <div v-if="pageLoading" class="state-card">抽检结果提交页加载中...</div>
      <div v-else-if="loadError" class="state-card state-card--error">{{ loadError }}</div>

      <template v-else-if="task">
        <div class="content-grid">
          <div class="main-column">
            <section class="panel detail-panel">
              <div class="panel-head">
                <h2>抽样详情（只读）</h2>
              </div>
              <div class="detail-grid">
                <article>
                  <span>产品名称</span>
                  <strong>{{ task.productName || "-" }}</strong>
                </article>
                <article>
                  <span>抽样批次</span>
                  <strong>{{ task.productBatchNo || task.taskNo || "-" }}</strong>
                </article>
                <article>
                  <span>生产商</span>
                  <strong>{{ enterprise?.enterpriseName || task.enterpriseName || "-" }}</strong>
                </article>
                <article>
                  <span>采样日期</span>
                  <strong>{{ formatTime(task.sampledTime || task.assignedTime || task.createTime) }}</strong>
                </article>
                <article class="is-wide">
                  <span>采样地点</span>
                  <strong>{{ enterprise?.addressDetail || task.sampleLocation || "待补充现场采样地点信息" }}</strong>
                </article>
                <article class="is-wide">
                  <span>所属区域</span>
                  <strong>{{ regionLabel }}</strong>
                </article>
              </div>
            </section>

            <section class="panel">
              <div class="panel-head">
                <h2>检测结论录入</h2>
              </div>
              <form class="result-form" @submit.prevent="handleSubmit">
                <label class="full">
                  <span>采样时间</span>
                  <input v-model="samplingForm.sampledTime" type="datetime-local" required />
                </label>

                <div class="full">
                  <span class="field-label">结论判定</span>
                  <div class="result-choice-grid">
                    <button
                      type="button"
                      class="result-choice"
                      :class="{ 'is-active is-pass': samplingForm.result === 'PASS' }"
                      @click="samplingForm.result = 'PASS'"
                    >
                      <span class="result-choice__icon">✓</span>
                      <strong>合格 (PASS)</strong>
                    </button>
                    <button
                      type="button"
                      class="result-choice"
                      :class="{ 'is-active is-fail': samplingForm.result === 'FAIL' }"
                      @click="samplingForm.result = 'FAIL'"
                    >
                      <span class="result-choice__icon">×</span>
                      <strong>不合格 (FAIL)</strong>
                    </button>
                  </div>
                </div>

                <label class="full">
                  <span>抽检结论</span>
                  <textarea
                    v-model.trim="samplingForm.conclusion"
                    rows="6"
                    placeholder="请填写抽检结论，可补充关键指标、现场情况与实验室结论。"
                  ></textarea>
                </label>

                <label class="full">
                  <span>处置建议</span>
                  <textarea
                    v-model.trim="samplingForm.disposalSuggestion"
                    rows="7"
                    placeholder="请填写后续建议，例如复检申请、风险处置或后续跟进建议。"
                  ></textarea>
                </label>
              </form>
            </section>
          </div>

          <aside class="side-column">
            <section class="panel overview-panel">
              <div class="panel-head">
                <h2>任务快照</h2>
              </div>
              <dl class="overview-list">
                <div><dt>任务编号</dt><dd>{{ task.taskNo || `#${task.id}` }}</dd></div>
                <div><dt>任务状态</dt><dd>{{ formatSamplingTaskStatus(task.status) }}</dd></div>
                <div><dt>产品分类</dt><dd>{{ task.productCategory || "-" }}</dd></div>
                <div><dt>规格型号</dt><dd>{{ task.productSpecification || "-" }}</dd></div>
                <div><dt>执行人员</dt><dd>{{ task.assignedToName || enforcerUser.realName || enforcerUser.username || "-" }}</dd></div>
                <div><dt>截止时间</dt><dd>{{ formatTime(task.deadline) }}</dd></div>
              </dl>
            </section>

            <section class="panel timeline-panel">
              <div class="panel-head">
                <h2>任务流转历史</h2>
              </div>
              <div class="timeline-list">
                <article v-for="(item, index) in timeline" :key="`${item.title}-${index}`" class="timeline-item">
                  <span class="timeline-dot" :class="item.dotClass"></span>
                  <div class="timeline-body">
                    <div class="timeline-top">
                      <strong>{{ item.title }}</strong>
                      <small>{{ item.time }}</small>
                    </div>
                    <p>{{ item.desc }}</p>
                  </div>
                </article>
              </div>
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
import { findMySamplingTaskById, submitSamplingResult } from "../../api/regulationOperation";
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
const actionLoading = ref(false);
const loadError = ref("");
const task = ref(null);
const enterprise = ref(null);
const regionLabel = ref("-");
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

const timeline = computed(() => {
  const current = task.value;
  if (!current) return [];

  return [
    {
      title: "任务已下达",
      time: formatTime(current.assignedTime || current.createTime),
      desc: current.taskDesc || "系统根据专项抽检计划生成当前抽检任务。",
      dotClass: "is-on"
    },
    {
      title: current.sampledTime ? "样品送达实验室" : "样品送检待完成",
      time: current.sampledTime ? formatTime(current.sampledTime) : "待录入",
      desc: current.sampledTime
        ? `${current.assignedToName || "当前执法人员"} 已录入采样时间，可继续填写检测结论。`
        : "尚未完成采样时间登记，请确认现场取样信息后再提交结果。",
      dotClass: current.sampledTime ? "is-on" : "is-muted"
    },
    {
      title: current.samplingResult ? `结果审核：${formatInspectionResult(current.samplingResult)}` : "结果审核（待处理）",
      time: current.samplingResult ? formatTime(current.updateTime) : "--",
      desc: current.samplingConclusion || "等待提交检测结论后进入后续审核流程。",
      dotClass: current.samplingResult ? (current.samplingResult === "FAIL" ? "is-alert" : "is-on") : "is-muted"
    }
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
  if (value === "PASS") return "合格";
  if (value === "FAIL") return "不合格";
  return "待录入";
}

function normalizeDateTimeInput(value) {
  return value && value.length === 16 ? `${value}:00` : value;
}

function formatNowForDateTimeLocal() {
  const now = new Date();
  const offset = now.getTimezoneOffset() * 60 * 1000;
  return new Date(now.getTime() - offset).toISOString().slice(0, 16);
}

function goList() {
  router.push({ name: "regulator-enforcer-sampling" }).catch(() => {});
}

function goDetail() {
  if (!task.value?.id) {
    goList();
    return;
  }
  router.push({
    name: "regulator-enforcer-sampling-detail",
    params: { taskId: task.value.id }
  }).catch(() => {});
}

async function loadPage() {
  pageLoading.value = true;
  loadError.value = "";
  setStatus("");
  task.value = null;
  enterprise.value = null;
  regionLabel.value = "-";

  const taskId = route.params.taskId;
  if (!taskId) {
    loadError.value = "缺少抽检任务参数";
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
    samplingForm.sampledTime = row.sampledTime ? String(row.sampledTime).slice(0, 16) : formatNowForDateTimeLocal();
    samplingForm.result = row.samplingResult || "PASS";
    samplingForm.conclusion = row.samplingConclusion || "";
    samplingForm.disposalSuggestion = row.disposalSuggestion || "";

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
    loadError.value = error?.message || "抽检结果提交页加载失败";
  } finally {
    pageLoading.value = false;
  }
}

async function handleSubmit() {
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
    await router.push({
      name: "regulator-enforcer-sampling-detail",
      params: { taskId: task.value.id },
      query: { submitted: "1" }
    });
  } catch (error) {
    setStatus(error?.message || "提交抽检结果失败", "error");
  } finally {
    actionLoading.value = false;
  }
}

watch(() => route.params.taskId, loadPage, { immediate: true });
</script>

<style scoped>
.sampling-submit-page {
  display: grid;
  gap: 18px;
}
.page-head {
  display: flex;
  justify-content: space-between;
  align-items: end;
  gap: 16px;
}
.crumbs {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 10px;
  color: #64748b;
  font-size: 12px;
}
.crumb-link,
.crumb-current,
.crumb-sep {
  font: inherit;
}
.crumb-link {
  border: 0;
  background: transparent;
  color: #64748b;
  cursor: pointer;
  padding: 0;
}
.crumb-current,
.crumb-link:hover,
.crumb-link:focus-visible {
  color: #002660;
}
.page-head h1 {
  margin: 0;
  color: #002660;
  font-size: 28px;
  font-weight: 900;
}
.page-head p {
  margin: 8px 0 0;
  color: #64748b;
  font-size: 14px;
}
.head-actions {
  display: flex;
  gap: 12px;
}
.ghost-btn,
.primary-btn,
.result-choice {
  border-radius: 10px;
}
.ghost-btn,
.primary-btn {
  min-height: 42px;
  padding: 0 18px;
  border: 1px solid #dbe3ee;
  font-size: 14px;
  font-weight: 800;
  cursor: pointer;
}
.ghost-btn {
  background: #eef2f7;
  color: #334155;
}
.primary-btn {
  background: #002660;
  border-color: #002660;
  color: #fff;
}
.ghost-btn:disabled,
.primary-btn:disabled {
  opacity: 0.65;
  cursor: not-allowed;
}
.state-card {
  padding: 16px 18px;
  border: 1px solid #dbe3ee;
  background: #fff;
  color: #334155;
}
.state-card--error {
  border-color: #fecaca;
  background: #fff4f4;
  color: #b91c1c;
}
.content-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.65fr) minmax(320px, 0.95fr);
  gap: 20px;
}
.main-column,
.side-column {
  display: grid;
  gap: 20px;
}
.panel {
  border: 1px solid #dbe3ee;
  background: #fff;
  padding: 22px;
  box-shadow: 0 16px 36px rgba(15, 23, 42, 0.04);
}
.panel-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  margin-bottom: 18px;
}
.panel-head h2 {
  margin: 0;
  color: #002660;
  font-size: 18px;
  font-weight: 900;
}
.detail-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 18px 24px;
}
.detail-grid article,
.overview-list div {
  display: grid;
  gap: 8px;
}
.detail-grid .is-wide {
  grid-column: 1 / -1;
  padding-top: 18px;
  border-top: 1px solid #e6ecf3;
}
.detail-grid span,
.overview-list dt,
.field-label {
  color: #64748b;
  font-size: 11px;
  font-weight: 800;
  letter-spacing: 0.04em;
  text-transform: uppercase;
}
.detail-grid strong,
.overview-list dd {
  margin: 0;
  color: #0f172a;
  font-size: 15px;
  font-weight: 700;
  line-height: 1.5;
}
.result-form {
  display: grid;
  gap: 20px;
}
.result-form label {
  display: grid;
  gap: 10px;
}
.result-form label span {
  color: #0f172a;
  font-size: 14px;
  font-weight: 800;
}
.result-form input,
.result-form textarea {
  width: 100%;
  border: 1px solid #d6deea;
  background: #f5f7fb;
  padding: 14px 16px;
  color: #0f172a;
  font-size: 14px;
  transition: border-color 0.2s ease, background 0.2s ease;
}
.result-form input:focus,
.result-form textarea:focus {
  outline: none;
  border-color: #2e5aac;
  background: #fff;
}
.full {
  grid-column: 1 / -1;
}
.result-choice-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
  margin-top: 10px;
}
.result-choice {
  display: grid;
  gap: 14px;
  justify-items: center;
  min-height: 124px;
  padding: 22px 16px;
  border: 2px solid #dbe3ee;
  background: #fff;
  color: #0f172a;
  cursor: pointer;
  transition: border-color 0.2s ease, background 0.2s ease, transform 0.2s ease;
}
.result-choice:hover {
  transform: translateY(-1px);
}
.result-choice.is-active.is-pass {
  border-color: #16a34a;
  background: #f3fbf6;
}
.result-choice.is-active.is-fail {
  border-color: #dc2626;
  background: #fff4f4;
}
.result-choice__icon {
  display: grid;
  place-items: center;
  width: 46px;
  height: 46px;
  border-radius: 999px;
  background: #e2e8f0;
  font-size: 26px;
  font-weight: 900;
}
.result-choice.is-pass .result-choice__icon {
  background: #16a34a;
  color: #fff;
}
.result-choice.is-fail .result-choice__icon {
  background: #dc2626;
  color: #fff;
}
.result-choice strong {
  font-size: 16px;
}
.overview-list {
  display: grid;
  gap: 14px;
}
.overview-list div {
  grid-template-columns: 94px minmax(0, 1fr);
  align-items: start;
}
.overview-list dd {
  text-align: right;
  font-size: 14px;
}
.timeline-list {
  display: grid;
  gap: 20px;
}
.timeline-item {
  position: relative;
  display: grid;
  grid-template-columns: 16px minmax(0, 1fr);
  gap: 14px;
}
.timeline-item:not(:last-child)::after {
  content: "";
  position: absolute;
  left: 7px;
  top: 18px;
  bottom: -18px;
  width: 2px;
  background: #d7dfec;
}
.timeline-dot {
  width: 16px;
  height: 16px;
  border-radius: 999px;
  margin-top: 2px;
  background: #cbd5e1;
}
.timeline-dot.is-on {
  background: #002660;
}
.timeline-dot.is-alert {
  background: #dc2626;
}
.timeline-body {
  display: grid;
  gap: 8px;
}
.timeline-top {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: baseline;
}
.timeline-top strong {
  color: #0f172a;
  font-size: 15px;
}
.timeline-top small {
  color: #64748b;
  font-size: 12px;
}
.timeline-body p {
  margin: 0;
  color: #475569;
  font-size: 13px;
  line-height: 1.7;
}
.status-banner {
  padding: 12px 16px;
  border: 1px solid #dbe3ee;
  background: #f8fafc;
  color: #334155;
  font-size: 13px;
}
.status-banner.is-error {
  border-color: #fecaca;
  background: #fff4f4;
  color: #b91c1c;
}
@media (max-width: 1180px) {
  .content-grid {
    grid-template-columns: 1fr;
  }
}
@media (max-width: 860px) {
  .page-head,
  .timeline-top,
  .head-actions {
    display: grid;
    grid-template-columns: 1fr;
  }
  .detail-grid,
  .result-choice-grid {
    grid-template-columns: 1fr;
  }
  .overview-list div {
    grid-template-columns: 1fr;
    gap: 6px;
  }
  .overview-list dd {
    text-align: left;
  }
}
</style>
