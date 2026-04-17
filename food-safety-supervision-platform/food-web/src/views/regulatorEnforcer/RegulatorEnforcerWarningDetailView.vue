<template>
  <RegulatorEnforcerPageShell
    active-key="warnings"
    title="预警详情"
    subtitle="查看风险预警触发背景、处置状态与历史记录，并执行后续处置动作。"
  >
    <section class="warning-detail-page">
      <div v-if="loading" class="state-card">预警详情加载中...</div>
      <div v-else-if="!detail" class="state-card state-card--error">预警详情不存在或当前账号无权查看。</div>

      <template v-else>
        <header class="hero">
          <div class="hero-main">
            <nav class="crumbs">
              <button class="crumb-link" type="button" @click="goBack">我的风险预警</button>
              <span>/</span>
              <span>{{ detail.warningNo || `WRN-${detail.id}` }}</span>
            </nav>
            <div class="hero-title-row">
              <h3>{{ detail.title || "风险预警详情" }}</h3>
              <span class="status-pill" :class="`is-${warningStatusClass(detail.status)}`">
                {{ formatWarningStatus(detail.status) }}
              </span>
            </div>
            <p class="hero-desc">{{ detail.content || "暂无预警说明。" }}</p>
          </div>
          <div class="hero-side">
            <article>
              <span>预警编号</span>
              <strong>{{ detail.warningNo || `#${detail.id}` }}</strong>
            </article>
            <article>
              <span>预警等级</span>
              <strong>{{ formatWarningLevel(detail.level) }}</strong>
            </article>
            <article>
              <span>触发次数</span>
              <strong>{{ detail.triggerCount || 0 }}</strong>
            </article>
          </div>
        </header>

        <div class="content-grid">
          <div class="main-col">
            <section class="panel">
              <div class="section-head">
                <h4>预警概要</h4>
                <span class="level-pill" :class="`is-${String(detail.level || '').toLowerCase()}`">
                  {{ formatWarningLevel(detail.level) }}
                </span>
              </div>
              <div class="summary-grid">
                <article>
                  <span>当前状态</span>
                  <strong>{{ formatWarningStatus(detail.status) }}</strong>
                </article>
                <article>
                  <span>预警类型</span>
                  <strong>{{ detail.warningType || "-" }}</strong>
                </article>
                <article>
                  <span>业务对象</span>
                  <strong>{{ detail.bizName || "-" }}</strong>
                </article>
                <article>
                  <span>对象类型</span>
                  <strong>{{ detail.bizType || "-" }}</strong>
                </article>
              </div>
              <div class="detail-block">
                <label>预警内容说明</label>
                <p>{{ detail.content || "-" }}</p>
              </div>
            </section>

            <section class="panel">
              <div class="section-head">
                <h4>处置流转</h4>
                <span class="section-hint">共 {{ detail.processLogs?.length || 0 }} 条记录</span>
              </div>
              <div class="timeline">
                <article v-for="log in detail.processLogs || []" :key="log.id" class="timeline-item">
                  <span class="timeline-dot"></span>
                  <div class="timeline-main">
                    <div class="timeline-head">
                      <strong>{{ formatWarningAction(log.actionType) }}</strong>
                      <time>{{ formatTime(log.createTime) }}</time>
                    </div>
                    <p>操作人：{{ log.operatorName || "-" }}</p>
                    <p>{{ log.actionComment || "无补充说明" }}</p>
                  </div>
                </article>
                <div v-if="!detail.processLogs?.length" class="empty-box empty-box--plain">暂无处理记录。</div>
              </div>
            </section>
          </div>

          <aside class="side-col">
            <section class="panel panel-accent">
              <h4>处置动作</h4>
              <div class="action-stack">
                <button
                  v-if="warningQuickAction(detail.status)"
                  class="primary"
                  type="button"
                  :disabled="actionLoading"
                  @click="handleWarningAction(warningQuickAction(detail.status).actionType)"
                >
                  {{ actionLoading ? "处理中..." : warningQuickAction(detail.status).label }}
                </button>
                <button v-if="canJumpWarningComplaint(detail)" class="ghost ghost--light" type="button" @click="jumpToWarningComplaint">
                  跳转投诉详情
                </button>
                <button
                  v-if="canJumpWarningRectification(detail)"
                  class="ghost ghost--light"
                  type="button"
                  @click="jumpToWarningRectification"
                >
                  跳转整改详情
                </button>
                <button class="ghost ghost--light" type="button" @click="goBack">返回预警列表</button>
              </div>
            </section>

            <section class="panel">
              <h4>关联对象</h4>
              <div class="mini-list">
                <article>
                  <span>业务对象名称</span>
                  <strong>{{ detail.bizName || "-" }}</strong>
                </article>
                <article>
                  <span>业务对象类型</span>
                  <strong>{{ detail.bizType || "-" }}</strong>
                </article>
                <article>
                  <span>对象 ID</span>
                  <strong>{{ detail.bizId || "-" }}</strong>
                </article>
                <article>
                  <span>最近触发时间</span>
                  <strong>{{ formatTime(detail.lastOccurTime || detail.updateTime || detail.createTime) }}</strong>
                </article>
              </div>
            </section>
          </aside>
        </div>

        <div v-if="status.message" class="status-banner" :class="`is-${status.type}`">{{ status.message }}</div>
      </template>
    </section>
  </RegulatorEnforcerPageShell>
</template>

<script setup>
import { onMounted, reactive, ref, watch } from "vue";
import { useRoute, useRouter } from "vue-router";
import { fetchMyWarningDetail, processMyWarning } from "../../api/regulation";
import RegulatorEnforcerPageShell from "./RegulatorEnforcerPageShell.vue";
import { formatByMap, formatTime } from "../../utils/formatters";
import { warningActionMap, warningLevelMap, warningStatusMap } from "../../utils/statusMaps";
import { useRegulatorEnforcerShellSession } from "./regulatorEnforcerShared";

const route = useRoute();
const router = useRouter();
const { token } = useRegulatorEnforcerShellSession();

const loading = ref(false);
const actionLoading = ref(false);
const detail = ref(null);
const status = reactive({ message: "", type: "info" });

function setStatus(message = "", type = "info") {
  status.message = message;
  status.type = type;
}

function formatWarningStatus(value) {
  return formatByMap(value, warningStatusMap);
}

function formatWarningLevel(value) {
  return formatByMap(value, warningLevelMap);
}

function formatWarningAction(value) {
  return formatByMap(value, warningActionMap);
}

function warningStatusClass(value) {
  if (value === "OPEN") return "open";
  if (value === "PROCESSING") return "processing";
  if (value === "RESOLVED") return "resolved";
  if (value === "CLOSED") return "closed";
  return "unknown";
}

function warningQuickAction(statusValue) {
  if (statusValue === "OPEN") return { actionType: "PROCESS", label: "开始处理" };
  if (statusValue === "PROCESSING") return { actionType: "RESOLVE", label: "标记解决" };
  return null;
}

function canJumpWarningComplaint(warning) {
  return String(warning?.bizType || "").toUpperCase() === "COMPLAINT" && Number(warning?.bizId) > 0;
}

function canJumpWarningRectification(warning) {
  return String(warning?.bizType || "").toUpperCase() === "RECTIFICATION" && Number(warning?.bizId) > 0;
}

function jumpToWarningComplaint() {
  if (!canJumpWarningComplaint(detail.value)) return;
  router.push({
    name: "regulator-enforcer-complaint-detail",
    params: { complaintId: Number(detail.value.bizId) },
    query: { from: "warnings" }
  }).catch(() => {});
}

function jumpToWarningRectification() {
  if (!canJumpWarningRectification(detail.value)) return;
  router.push({
    name: "regulator-enforcer-rectification-detail",
    params: { rectificationId: Number(detail.value.bizId) },
    query: { from: "warnings" }
  }).catch(() => {});
}

async function loadDetail() {
  const warningId = Number(route.params.warningId || 0);
  if (!warningId) {
    detail.value = null;
    return;
  }
  loading.value = true;
  setStatus("");
  try {
    detail.value = await fetchMyWarningDetail(token.value, warningId);
  } catch (error) {
    detail.value = null;
    setStatus(error.message || "加载预警详情失败", "error");
  } finally {
    loading.value = false;
  }
}

async function handleWarningAction(actionType) {
  if (!detail.value?.id || !actionType) return;
  actionLoading.value = true;
  setStatus("");
  try {
    detail.value = await processMyWarning(token.value, detail.value.id, { actionType });
    setStatus(`预警已执行${formatWarningAction(actionType)}`, "success");
  } catch (error) {
    setStatus(error.message || "预警处理失败", "error");
  } finally {
    actionLoading.value = false;
  }
}

function goBack() {
  router.push({ name: "regulator-enforcer-warnings" }).catch(() => {});
}

onMounted(loadDetail);
watch(() => route.params.warningId, loadDetail);
</script>

<style scoped>
.warning-detail-page { display: grid; gap: 16px; }
.state-card { padding: 18px 20px; border: 1px solid #dbe3ee; background: #fff; color: #64748b; }
.state-card--error { color: #b91c1c; border-color: #fecaca; background: #fef2f2; }
.hero { display: grid; grid-template-columns: minmax(0, 1fr) 320px; gap: 16px; padding: 18px; background: linear-gradient(135deg, #f8fbff, #eef4ff); border: 1px solid #dbe3ee; }
.crumbs { display: flex; gap: 6px; color: #64748b; font-size: 11px; font-weight: 700; }
.crumb-link { padding: 0; border: 0; background: transparent; color: #002660; cursor: pointer; font-size: inherit; font-weight: inherit; }
.hero-title-row { margin-top: 10px; display: flex; gap: 10px; align-items: center; flex-wrap: wrap; }
.hero-title-row h3 { margin: 0; color: #002660; font-size: 30px; font-weight: 900; letter-spacing: -0.02em; }
.hero-desc { margin: 10px 0 0; color: #475569; font-size: 13px; line-height: 1.7; max-width: 760px; }
.hero-side { display: grid; gap: 10px; }
.hero-side article { padding: 12px 14px; background: #fff; border: 1px solid #dbe3ee; }
.hero-side span, .summary-grid span, .detail-block label, .mini-list span { display: block; color: #64748b; font-size: 11px; font-weight: 700; }
.hero-side strong, .summary-grid strong, .mini-list strong { display: block; margin-top: 5px; color: #0f172a; font-size: 15px; font-weight: 800; line-height: 1.5; }
.content-grid { display: grid; grid-template-columns: minmax(0, 1fr) 320px; gap: 16px; align-items: start; }
.main-col, .side-col { display: grid; gap: 16px; }
.panel { border: 1px solid #dbe3ee; background: #fff; padding: 16px; }
.panel-accent { background: linear-gradient(135deg, #002660, #003a8c); border-color: transparent; color: #fff; }
.panel h4 { margin: 0; color: #002660; font-size: 12px; font-weight: 900; text-transform: uppercase; letter-spacing: 0.08em; }
.panel-accent h4 { color: rgba(255,255,255,0.78); }
.section-head { display: flex; justify-content: space-between; align-items: center; gap: 10px; margin-bottom: 14px; }
.section-hint { color: #94a3b8; font-size: 11px; font-weight: 700; }
.summary-grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 12px; }
.summary-grid article, .mini-list article { padding: 12px; background: #f8fafc; border: 1px solid #eef2f7; }
.detail-block { margin-top: 14px; padding: 12px; background: #f8fafc; border-left: 3px solid #cbd5e1; }
.detail-block p { margin: 6px 0 0; color: #334155; font-size: 13px; line-height: 1.7; white-space: pre-line; }
.timeline { position: relative; display: grid; gap: 14px; }
.timeline::before { content: ""; position: absolute; left: 5px; top: 8px; bottom: 8px; width: 2px; background: #e2e8f0; }
.timeline-item { position: relative; padding-left: 22px; }
.timeline-dot { position: absolute; left: 0; top: 4px; width: 12px; height: 12px; border-radius: 999px; background: #002660; border: 2px solid #fff; box-shadow: 0 0 0 1px #cbd5e1; }
.timeline-head { display: flex; justify-content: space-between; gap: 10px; align-items: center; }
.timeline-head strong { color: #0f172a; font-size: 12px; }
.timeline-head time { color: #94a3b8; font-size: 10px; white-space: nowrap; }
.timeline-main p { margin: 6px 0 0; color: #64748b; font-size: 12px; line-height: 1.6; }
.action-stack { display: grid; gap: 10px; margin-top: 14px; }
.mini-list { display: grid; gap: 10px; margin-top: 14px; }
.primary, .ghost { min-height: 36px; padding: 0 14px; border: 1px solid #cbd5e1; font-size: 12px; font-weight: 700; cursor: pointer; }
.primary { background: #002660; border-color: #002660; color: #fff; }
.ghost { background: #fff; color: #334155; }
.ghost--light { background: rgba(255,255,255,0.08); border-color: rgba(255,255,255,0.24); color: #fff; }
.level-pill, .status-pill { display: inline-flex; min-height: 24px; align-items: center; justify-content: center; padding: 0 10px; border-radius: 999px; border: 1px solid transparent; font-size: 11px; font-weight: 800; }
.level-pill.is-l1 { background: #fee2e2; color: #991b1b; }
.level-pill.is-l2 { background: #ffedd5; color: #9a3412; }
.status-pill.is-open { background: #fff4eb; color: #9b3a0a; border-color: #f8d5bf; }
.status-pill.is-processing { background: #ecfeff; color: #155e75; border-color: #a5f3fc; }
.status-pill.is-resolved { background: #dcfce7; color: #166534; border-color: #86efac; }
.status-pill.is-closed { background: #f1f5f9; color: #475569; border-color: #dbe3ee; }
.status-pill.is-unknown { background: #f8fafc; color: #64748b; border-color: #dbe3ee; }
.empty-box { padding: 14px; border: 1px dashed #cbd5e1; background: #f8fafc; color: #64748b; font-size: 12px; }
.empty-box--plain { margin-left: 22px; }
.status-banner { padding: 10px 12px; border: 1px solid #dbe3ee; background: #f8fafc; color: #334155; }
.status-banner.is-error { border-color: #fecaca; background: #fef2f2; color: #b91c1c; }
.status-banner.is-success { border-color: #bbf7d0; background: #ecfdf5; color: #166534; }
@media (max-width: 1080px) {
  .hero, .content-grid { grid-template-columns: 1fr; }
}
@media (max-width: 760px) {
  .summary-grid { grid-template-columns: 1fr; }
  .hero-title-row h3 { font-size: 22px; }
  .timeline-head { flex-direction: column; align-items: flex-start; }
}
</style>
