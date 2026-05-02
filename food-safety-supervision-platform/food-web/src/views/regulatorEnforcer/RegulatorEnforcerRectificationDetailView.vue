<template>
  <RegulatorEnforcerPageShell
    active-key="rectifications"
    title="整改详情"
    subtitle="查看企业整改说明、时限状态与流转记录，并完成闭环确认。"
  >
    <section class="rect-detail-page">
      <div v-if="loading" class="state-card">整改详情加载中...</div>
      <div v-else-if="!detail" class="state-card state-card--error">整改详情不存在或当前账号无权查看。</div>

      <template v-else>
        <header class="head">
          <div>
            <nav class="crumbs">
              <button type="button" class="crumb-link" @click="goBack">整改跟进</button>
              <span class="sep">/</span>
              <span>整改详情</span>
            </nav>
            <div class="title-row">
              <h3>{{ detail.rectificationNo || `RECT-${detail.id}` }}</h3>
              <span class="status-chip" :class="`is-${String(detail.status || '').toLowerCase()}`">
                {{ formatRectificationStatus(detail.status) }}
              </span>
            </div>
            <p class="title-sub">
              {{ detail.enterpriseName || "-" }}
              <span>检查时间：{{ formatTime(detail.inspectionTime || detail.createTime) }}</span>
            </p>
          </div>
          <div class="head-actions">
            <button class="ghost" type="button" @click="goBack">返回列表</button>
            <button
              v-if="detail.status === 'SUBMITTED'"
              class="primary"
              type="button"
              :disabled="actionLoading"
              @click="handleConfirm"
            >
              {{ actionLoading ? "确认中..." : "确认闭环" }}
            </button>
          </div>
        </header>

        <div class="content-grid">
          <div class="left-col">
            <section class="panel">
              <div class="panel-head">
                <h4>原始整改事项</h4>
                <span class="panel-tag">{{ formatRectificationSla(detail) }}</span>
              </div>
              <div class="summary-grid">
                <article>
                  <span>企业名称</span>
                  <strong>{{ detail.enterpriseName || "-" }}</strong>
                </article>
                <article>
                  <span>当前截止日期</span>
                  <strong>{{ detail.currentDeadline ? formatTime(detail.currentDeadline) : "-" }}</strong>
                </article>
                <article>
                  <span>整改状态</span>
                  <strong>{{ formatRectificationStatus(detail.status) }}</strong>
                </article>
                <article>
                  <span>时限判断</span>
                  <strong :class="`tone-${rectificationSlaClass(detail)}`">{{ rectificationDeadlineLabel(detail) }}</strong>
                </article>
              </div>
              <div class="desc-block">
                <label>问题描述</label>
                <p>{{ detail.rectificationDesc || "-" }}</p>
              </div>
              <div v-if="originalAttachments.length" class="proof-wrap">
                <label>现场取证附件</label>
                <div class="proof-grid">
                  <button
                    v-for="(url, index) in visibleOriginalAttachments"
                    :key="`orig-${url}-${index}`"
                    class="proof-thumb"
                    type="button"
                    @click="openImagePreview(originalAttachments, index)"
                  >
                    <img :src="url" alt="现场取证附件" />
                  </button>
                </div>
                <button
                  v-if="originalHiddenCount > 0 && !showAllOriginalAttachments"
                  class="proof-toggle"
                  type="button"
                  @click="showAllOriginalAttachments = true"
                >
                  查看更多（{{ originalHiddenCount }}）
                </button>
                <button
                  v-if="showAllOriginalAttachments && originalAttachments.length > 3"
                  class="proof-toggle proof-toggle--muted"
                  type="button"
                  @click="showAllOriginalAttachments = false"
                >
                  收起附件
                </button>
              </div>
            </section>

            <section class="panel">
              <div class="panel-head">
                <h4>企业整改提交</h4>
                <span class="panel-tag panel-tag--soft">
                  提交时间：{{ latestSubmitLog?.createTime ? formatTime(latestSubmitLog.createTime) : "-" }}
                </span>
              </div>
              <div class="desc-block desc-block--soft">
                <label>整改说明</label>
                <p>{{ latestSubmitLog?.actionComment || detail.progress || "企业暂未提交整改说明。" }}</p>
              </div>
              <div v-if="submitAttachments.length" class="proof-wrap">
                <label>整改附件</label>
                <div class="proof-grid">
                  <button
                    v-for="(url, index) in visibleSubmitAttachments"
                    :key="`submit-${url}-${index}`"
                    class="proof-thumb"
                    type="button"
                    @click="openImagePreview(submitAttachments, index)"
                  >
                    <img :src="url" alt="整改附件" />
                  </button>
                </div>
                <button
                  v-if="submitHiddenCount > 0 && !showAllSubmitAttachments"
                  class="proof-toggle"
                  type="button"
                  @click="showAllSubmitAttachments = true"
                >
                  查看更多（{{ submitHiddenCount }}）
                </button>
                <button
                  v-if="showAllSubmitAttachments && submitAttachments.length > 3"
                  class="proof-toggle proof-toggle--muted"
                  type="button"
                  @click="showAllSubmitAttachments = false"
                >
                  收起附件
                </button>
              </div>
              <div v-else class="empty-box">企业暂未上传整改附件。</div>
            </section>
          </div>

          <div class="right-col">
            <section class="panel panel-accent">
              <h4>核验提示</h4>
              <div class="metric-list">
                <div>
                  <span>任务编号</span>
                  <strong>{{ detail.rectificationNo || `RECT-${detail.id}` }}</strong>
                </div>
                <div>
                  <span>进度摘要</span>
                  <strong>{{ detail.progress || "待企业提交整改说明" }}</strong>
                </div>
                <div>
                  <span>风险时效</span>
                  <strong :class="`tone-${rectificationSlaClass(detail)}`">{{ formatRectificationSla(detail) }}</strong>
                </div>
              </div>
            </section>

            <section class="panel">
              <h4>流转历史</h4>
              <div class="log-list">
                <article v-for="item in actionLogs" :key="item.id || item.key" class="log-item">
                  <span class="log-dot"></span>
                  <div class="log-main">
                    <strong>{{ resolveActionName(item.actionType) }}</strong>
                    <p>{{ item.actionComment || "无补充说明" }}</p>
                  </div>
                  <time>{{ formatTime(item.createTime) }}</time>
                </article>
                <div v-if="!actionLogs.length" class="empty-box empty-box--plain">暂无流转记录。</div>
              </div>
            </section>
          </div>
        </div>

        <div v-if="status.message" class="status-banner" :class="`is-${status.type}`">{{ status.message }}</div>
      </template>
    </section>
  </RegulatorEnforcerPageShell>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from "vue";
import { useRoute, useRouter } from "vue-router";
import {
  confirmRectification,
  fetchRectificationActions,
  fetchRectificationDetail
} from "../../api/regulationOperation";
import { formatTime } from "../../utils/formatters";
import { formatStatusLabel, rectificationStatusMap } from "../../utils/statusMaps";
import { resolveErrorMessage } from "../../utils/uiFeedback";
import RegulatorEnforcerPageShell from "./RegulatorEnforcerPageShell.vue";
import { useRegulatorEnforcerShellSession } from "./regulatorEnforcerShared";

const route = useRoute();
const router = useRouter();
const { token } = useRegulatorEnforcerShellSession();

const loading = ref(false);
const actionLoading = ref(false);
const detail = ref(null);
const actionLogs = ref([]);
const showAllOriginalAttachments = ref(false);
const showAllSubmitAttachments = ref(false);
const status = reactive({ message: "", type: "info" });


const actionNameMap = {
  SYSTEM_CREATE: "系统创建整改任务",
  ENTERPRISE_SUBMIT: "企业提交整改",
  REVIEW_CONFIRM: "监管确认闭环",
  REVIEW_REWORK: "监管打回重做"
};

const latestSubmitLog = computed(() =>
  actionLogs.value.find((item) => String(item.actionType || "").toUpperCase() === "ENTERPRISE_SUBMIT") || null
);

const originalAttachments = computed(() => {
  const urls = detail.value?.attachmentUrls;
  if (Array.isArray(urls) && urls.length) return urls;
  const fallback = actionLogs.value[0]?.attachmentUrls;
  return Array.isArray(fallback) ? fallback : [];
});

const submitAttachments = computed(() => {
  const urls = latestSubmitLog.value?.attachmentUrls;
  return Array.isArray(urls) ? urls : [];
});

const visibleOriginalAttachments = computed(() =>
  showAllOriginalAttachments.value ? originalAttachments.value : originalAttachments.value.slice(0, 3)
);
const originalHiddenCount = computed(() => Math.max(0, originalAttachments.value.length - 3));

const visibleSubmitAttachments = computed(() =>
  showAllSubmitAttachments.value ? submitAttachments.value : submitAttachments.value.slice(0, 3)
);
const submitHiddenCount = computed(() => Math.max(0, submitAttachments.value.length - 3));

function setStatus(message = "", type = "info") {
  status.message = message;
  status.type = type;
}

function formatRectificationStatus(value) {
  return formatStatusLabel(value, rectificationStatusMap);
}

function resolveActionName(value) {
  return actionNameMap[String(value || "").toUpperCase()] || value || "未知动作";
}

function formatDurationMinutes(minutes) {
  const totalMins = Math.max(0, Number(minutes) || 0);
  const days = Math.floor(totalMins / (24 * 60));
  const hours = Math.floor((totalMins % (24 * 60)) / 60);
  const mins = totalMins % 60;
  if (days > 0) return `${days}天${hours}小时`;
  if (hours > 0) return `${hours}小时${mins}分钟`;
  return `${mins}分钟`;
}

function rectificationSlaClass(item) {
  if (!item) return "none";
  if (item.slaStatus === "OVERDUE") return "error";
  if (item.slaStatus === "DUE_SOON") return "warning";
  if (item.slaStatus === "NORMAL") return "normal";
  return "muted";
}

function formatRectificationSla(item) {
  if (!item) return "-";
  const remaining = Number(item.remainingMinutes);
  if (item.slaStatus === "OVERDUE") return `已逾期 ${formatDurationMinutes(Math.abs(remaining))}`;
  if (item.slaStatus === "DUE_SOON") return `距截止仅剩 ${formatDurationMinutes(remaining)}`;
  if (item.slaStatus === "NORMAL") return `剩余整改时间 ${formatDurationMinutes(remaining)}`;
  if (item.currentDeadline) return `截止时间 ${formatTime(item.currentDeadline)}`;
  if (item.status === "CONFIRMED") return "已完成闭环";
  return "正常推进";
}

function rectificationDeadlineLabel(item) {
  if (!item) return "-";
  if (item.slaStatus === "OVERDUE") return "已超出整改时限";
  if (item.slaStatus === "DUE_SOON") return "临近整改截止时间";
  if (item.slaStatus === "NORMAL") return "当前按计划推进";
  if (item.status === "CONFIRMED") return "已完成闭环确认";
  return "-";
}

async function loadDetail() {
  const rectificationId = Number(route.params.rectificationId || 0);
  if (!rectificationId) {
    detail.value = null;
    actionLogs.value = [];
    return;
  }
  loading.value = true;
  setStatus("");
  try {
    const [detailData, actions] = await Promise.all([
      fetchRectificationDetail(token.value, rectificationId),
      fetchRectificationActions(token.value, rectificationId)
    ]);
    detail.value = detailData || null;
    actionLogs.value = Array.isArray(actions) ? actions : [];
    showAllOriginalAttachments.value = false;
    showAllSubmitAttachments.value = false;
  } catch (error) {
    detail.value = null;
    actionLogs.value = [];
    setStatus(resolveErrorMessage(error, "加载整改详情失败"), "error");
  } finally {
    loading.value = false;
  }
}

async function handleConfirm() {
  if (!detail.value?.id) return;
  actionLoading.value = true;
  setStatus("");
  try {
    await confirmRectification(token.value, detail.value.id);
    setStatus("整改任务已确认闭环", "success");
    await loadDetail();
  } catch (error) {
    setStatus(resolveErrorMessage(error, "整改确认失败"), "error");
  } finally {
    actionLoading.value = false;
  }
}

function openImagePreview(urls, index) {
  if (!Array.isArray(urls) || !urls.length) return;
  const targetUrl = urls[index];
  if (targetUrl) {
    window.open(targetUrl, "_blank", "noopener,noreferrer");
  }
}

function goBack() {
  router.push({ name: "regulator-enforcer-rectifications" }).catch(() => {});
}

onMounted(loadDetail);
watch(() => route.params.rectificationId, loadDetail);
</script>

<style scoped>
.rect-detail-page { display: grid; gap: 16px; }
.state-card { padding: 18px 20px; border: 1px solid #dbe3ee; background: #fff; color: #64748b; }
.state-card--error { color: #b91c1c; border-color: #fecaca; background: #fef2f2; }
.head { display: flex; justify-content: space-between; align-items: flex-end; gap: 16px; flex-wrap: wrap; }
.crumbs { display: flex; align-items: center; gap: 6px; color: #64748b; font-size: 11px; font-weight: 700; }
.crumb-link { padding: 0; border: 0; background: transparent; color: #002660; cursor: pointer; font-size: inherit; font-weight: inherit; }
.sep { opacity: 0.5; }
.title-row { margin-top: 8px; display: flex; align-items: center; gap: 10px; flex-wrap: wrap; }
.title-row h3 { margin: 0; color: #002660; font-size: 28px; font-weight: 900; letter-spacing: -0.02em; }
.title-sub { margin: 8px 0 0; display: flex; gap: 12px; flex-wrap: wrap; color: #64748b; font-size: 12px; }
.head-actions { display: flex; gap: 8px; }
.primary, .ghost { min-height: 36px; padding: 0 14px; border: 1px solid #cbd5e1; font-size: 12px; font-weight: 700; cursor: pointer; }
.primary { background: #002660; border-color: #002660; color: #fff; }
.ghost { background: #fff; color: #334155; }
.content-grid { display: grid; grid-template-columns: minmax(0, 1fr) 320px; gap: 16px; align-items: start; }
.left-col, .right-col { display: grid; gap: 16px; }
.panel { border: 1px solid #dbe3ee; background: #fff; padding: 16px; }
.panel-accent { background: linear-gradient(135deg, #002660, #003a8c); border-color: transparent; color: #fff; }
.panel-accent h4, .panel-accent span { color: rgba(255, 255, 255, 0.78); }
.panel h4 { margin: 0; color: #002660; font-size: 12px; font-weight: 900; letter-spacing: 0.08em; text-transform: uppercase; }
.panel-head { display: flex; justify-content: space-between; align-items: center; gap: 10px; margin-bottom: 14px; }
.panel-tag { display: inline-flex; align-items: center; min-height: 24px; padding: 0 10px; background: #eef4ff; color: #002660; font-size: 11px; font-weight: 700; }
.panel-tag--soft { background: #f8fafc; color: #475569; }
.summary-grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 12px; }
.summary-grid article, .metric-list div { display: grid; gap: 4px; }
.summary-grid span, .metric-list span, .proof-wrap label, .desc-block label { color: #64748b; font-size: 11px; font-weight: 700; }
.summary-grid strong, .metric-list strong { color: #0f172a; font-size: 15px; font-weight: 800; line-height: 1.5; }
.panel-accent strong { color: #fff; }
.desc-block { margin-top: 14px; padding: 12px; border-left: 3px solid #cbd5e1; background: #f8fafc; }
.desc-block--soft { border-left-color: #94a3b8; }
.desc-block p { margin: 6px 0 0; color: #334155; font-size: 13px; line-height: 1.7; white-space: pre-line; }
.proof-wrap { margin-top: 14px; display: grid; gap: 10px; }
.proof-grid { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 10px; }
.proof-thumb { min-height: 110px; border: 1px solid #dbe3ee; background: #fff; overflow: hidden; cursor: pointer; }
.proof-thumb img { width: 100%; height: 110px; display: block; object-fit: cover; }
.proof-toggle { justify-self: start; min-height: 32px; padding: 0 12px; border: 1px solid #cbd5e1; background: #fff; color: #334155; font-size: 12px; font-weight: 700; cursor: pointer; }
.proof-toggle--muted { color: #64748b; }
.metric-list { display: grid; gap: 12px; }
.log-list { position: relative; display: grid; gap: 14px; }
.log-list::before { content: ""; position: absolute; left: 5px; top: 8px; bottom: 8px; width: 2px; background: #e2e8f0; }
.log-item { position: relative; display: grid; grid-template-columns: minmax(0, 1fr) auto; gap: 8px 12px; padding-left: 20px; }
.log-dot { position: absolute; left: 0; top: 4px; width: 12px; height: 12px; border-radius: 999px; background: #002660; border: 2px solid #fff; box-shadow: 0 0 0 1px #cbd5e1; }
.log-main strong { color: #0f172a; font-size: 12px; }
.log-main p { margin: 4px 0 0; color: #64748b; font-size: 12px; line-height: 1.6; }
.log-item time { color: #94a3b8; font-size: 10px; white-space: nowrap; }
.status-chip { display: inline-flex; align-items: center; min-height: 24px; padding: 0 10px; font-size: 11px; font-weight: 800; }
.status-chip.is-ongoing { background: #dbeafe; color: #1e3a8a; }
.status-chip.is-submitted { background: #c9d7fe; color: #003a8c; }
.status-chip.is-rework { background: #fee2e2; color: #b91c1c; }
.status-chip.is-confirmed { background: #dcfce7; color: #166534; }
.tone-error { color: #b91c1c !important; }
.tone-warning { color: #b36b00 !important; }
.tone-normal { color: #0d4f9b !important; }
.tone-muted { color: #64748b !important; }
.empty-box { padding: 14px; border: 1px dashed #cbd5e1; background: #f8fafc; color: #64748b; font-size: 12px; }
.empty-box--plain { padding: 0 0 0 20px; border: 0; background: transparent; }
.status-banner { padding: 10px 12px; border: 1px solid #dbe3ee; background: #f8fafc; color: #334155; }
.status-banner.is-error { border-color: #fecaca; background: #fef2f2; color: #b91c1c; }
.status-banner.is-success { border-color: #bbf7d0; background: #ecfdf5; color: #166534; }
@media (max-width: 1080px) { .content-grid { grid-template-columns: 1fr; } }
@media (max-width: 760px) {
  .summary-grid, .proof-grid { grid-template-columns: 1fr; }
  .title-row h3 { font-size: 22px; }
  .head { align-items: stretch; }
}
</style>
