<template>
  <RegulatorAdminWorkspacePage
    active-key="rectification"
    :username="regulatorUser.username"
    @navigate="handleSidebarNavigate"
    @logout="handleLogout"
  >
    <section class="rect-detail-page">
      <div v-if="loading" class="state-card">加载整改详情中...</div>
      <div v-else-if="!detail" class="state-card state-card--error">整改详情不存在或无权限查看</div>

      <template v-else>
        <header class="head">
          <div>
            <nav class="crumbs">
              <span>整改复核</span>
              <span class="sep">/</span>
              <span>整改详情</span>
            </nav>
            <div class="title-row">
              <h1>RC-{{ detail.id }}</h1>
              <span class="status-chip" :class="`is-${(detail.status || '').toLowerCase()}`">
                {{ formatRectificationStatus(detail.status) }}
              </span>
            </div>
          </div>
          <div class="head-actions">
            <button class="ghost" type="button" @click="goBack">返回列表</button>
            <button
              v-if="detail.status === 'SUBMITTED'"
              class="primary"
              type="button"
              :disabled="actionLoading"
              @click="handleReview({ action: 'CONFIRM' })"
            >
              确认流转
            </button>
          </div>
        </header>

        <div class="content-grid">
          <div class="left-col">
            <section class="panel panel-issue">
              <h4>原始违规事项 / Original Issue</h4>
              <div class="issue-meta">
                <div>
                  <span>企业名称</span>
                  <strong>{{ detail.enterpriseName || "-" }}</strong>
                </div>
                <div>
                  <span>检查日期</span>
                  <strong>{{ formatTime(detail.inspectionTime || detail.createTime) }}</strong>
                </div>
              </div>
              <div class="issue-desc">
                <label>违规描述</label>
                <p>{{ detail.rectificationDesc || "-" }}</p>
              </div>
              <div class="proof-grid" v-if="originalAttachments.length">
                <button
                  v-for="(url, index) in visibleOriginalAttachments"
                  :key="`orig-${url}-${index}`"
                  class="proof-thumb"
                  type="button"
                  @click="openImagePreview(originalAttachments, index)"
                >
                  <img :src="url" alt="原始违规证据" />
                </button>
              </div>
              <button
                v-if="originalHiddenCount > 0 && !showAllOriginalAttachments"
                class="proof-more-btn"
                type="button"
                @click="showAllOriginalAttachments = true"
              >
                查看更多（+{{ originalHiddenCount }}）
              </button>
              <button
                v-if="showAllOriginalAttachments && originalAttachments.length > 3"
                class="proof-more-btn proof-more-btn--muted"
                type="button"
                @click="showAllOriginalAttachments = false"
              >
                收起
              </button>
            </section>

            <section class="panel panel-submission">
              <div class="submission-head">
                <h4>企业整改报告 / Submission</h4>
                <span class="submit-time">
                  提交时间：{{ latestSubmitLog?.createTime ? formatTime(latestSubmitLog.createTime) : "-" }}
                </span>
              </div>
              <div class="issue-desc issue-desc--submission">
                <label>整改措施描述</label>
                <p>{{ latestSubmitLog?.actionComment || detail.progress || "企业暂未提交整改说明" }}</p>
              </div>
              <div class="proof-grid" v-if="submitAttachments.length">
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
                class="proof-more-btn"
                type="button"
                @click="showAllSubmitAttachments = true"
              >
                查看更多（+{{ submitHiddenCount }}）
              </button>
              <button
                v-if="showAllSubmitAttachments && submitAttachments.length > 3"
                class="proof-more-btn proof-more-btn--muted"
                type="button"
                @click="showAllSubmitAttachments = false"
              >
                收起
              </button>
              <div v-else class="proof-empty">暂无整改附件</div>
            </section>

            <section v-if="detail.status === 'SUBMITTED'" class="panel panel-review">
              <h4>监管复核判定 / Review Decision</h4>
              <p class="review-desc">请基于现场查验或在线材料比对，做出最终裁定。</p>
              <label class="form-label">
                审核批注（必填）
                <textarea
                  v-model.trim="reviewComment"
                  rows="4"
                  placeholder="请输入复核意见，如需重新整改请详细说明原因..."
                ></textarea>
              </label>
              <div class="action-stack">
                <button class="review-btn review-btn--confirm" type="button" :disabled="actionLoading" @click="handleReview({ action: 'CONFIRM' })">
                  确认整改通过 (CONFIRM)
                </button>
                <button class="review-btn review-btn--rework" type="button" :disabled="actionLoading" @click="handleReview({ action: 'REWORK' })">
                  驳回重新整改 (REWORK)
                </button>
              </div>
            </section>
          </div>

          <div class="right-col">
            <section class="panel panel-blue">
              <h4>预警与时限</h4>
              <div class="warn-row">
                <span>预警状态</span>
                <strong :class="`is-${rectificationSlaClass(detail)}`">{{ formatRectificationSla(detail) }}</strong>
              </div>
            </section>

            <section class="panel panel-large">
              <h4>流转日志 / Processing Log</h4>
              <div class="log-list">
                <article v-for="item in actionLogs" :key="item.id || item.key" class="log-item">
                  <span class="log-dot"></span>
                  <div class="log-main">
                    <strong>{{ item.actionName || resolveActionName(item.actionType) }}</strong>
                    <p>{{ item.actionComment || "无补充说明" }}</p>
                  </div>
                  <time>{{ formatTime(item.createTime) }}</time>
                </article>
                <div v-if="!actionLogs.length" class="log-empty">暂无流转日志</div>
              </div>
            </section>
          </div>
        </div>

        <div v-if="status.message" class="status" :class="status.type">{{ status.message }}</div>
      </template>
    </section>
  </RegulatorAdminWorkspacePage>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from "vue";
import { useRoute, useRouter } from "vue-router";
import { fetchRectificationActions, fetchRectificationDetail, reviewRectification } from "../../api/regulationOperation";
import RegulatorAdminWorkspacePage from "../../components/regulatorAdmin/RegulatorAdminWorkspacePage.vue";
import { formatTime } from "../../utils/formatters";
import { formatStatusLabel, rectificationStatusMap } from "../../utils/statusMaps";
import { resolveErrorMessage } from "../../utils/uiFeedback";
import { useRegulatorAdminShellSession } from "./regulatorAdminShared";

const route = useRoute();
const router = useRouter();
const { regulatorUser, token, handleSidebarNavigate, handleLogout } = useRegulatorAdminShellSession();

const loading = ref(false);
const actionLoading = ref(false);
const detail = ref(null);
const actionLogs = ref([]);
const reviewComment = ref("");
const showAllOriginalAttachments = ref(false);
const showAllSubmitAttachments = ref(false);
const status = reactive({ message: "", type: "" });


const actionNameMap = {
  SYSTEM_CREATE: "系统创建整改任务",
  ENTERPRISE_SUBMIT: "企业提交整改",
  REVIEW_CONFIRM: "监管复核通过",
  REVIEW_REWORK: "监管打回重做"
};

const latestSubmitLog = computed(() =>
  actionLogs.value.find((item) => String(item.actionType || "").toUpperCase() === "ENTERPRISE_SUBMIT") || null
);
const submitAttachments = computed(() => {
  const urls = latestSubmitLog.value?.attachmentUrls;
  return Array.isArray(urls) ? urls : [];
});
const visibleSubmitAttachments = computed(() =>
  showAllSubmitAttachments.value ? submitAttachments.value : submitAttachments.value.slice(0, 3)
);
const submitHiddenCount = computed(() => Math.max(0, submitAttachments.value.length - 3));
const originalAttachments = computed(() => {
  const urls = detail.value?.attachmentUrls;
  if (Array.isArray(urls) && urls.length) return urls;
  const fallbackUrls = actionLogs.value[0]?.attachmentUrls;
  return Array.isArray(fallbackUrls) ? fallbackUrls : [];
});
const visibleOriginalAttachments = computed(() =>
  showAllOriginalAttachments.value ? originalAttachments.value : originalAttachments.value.slice(0, 3)
);
const originalHiddenCount = computed(() => Math.max(0, originalAttachments.value.length - 3));

function setStatus(message = "", type = "") {
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
  if (item.slaStatus === "OVERDUE") return "overdue";
  if (item.slaStatus === "DUE_SOON") return "warning";
  if (item.slaStatus === "NORMAL") return "normal";
  return "none";
}

function formatRectificationSla(item) {
  if (!item) return "-";
  const remaining = Number(item.remainingMinutes);
  if (item.slaStatus === "OVERDUE") return `逾期 ${formatDurationMinutes(Math.abs(remaining))}`;
  if (item.slaStatus === "DUE_SOON") return `临期 ${formatDurationMinutes(remaining)}`;
  if (item.slaStatus === "NORMAL") return `剩余 ${formatDurationMinutes(remaining)}`;
  if (item.currentDeadline) return `截止 ${formatTime(item.currentDeadline)}`;
  return "正常进度";
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
    reviewComment.value = "";
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

async function handleReview(payload) {
  if (!detail.value?.id) return;
  if (payload.action === "REWORK" && !reviewComment.value.trim()) {
    setStatus("打回重做必须填写复核意见", "error");
    return;
  }
  actionLoading.value = true;
  setStatus("");
  try {
    await reviewRectification(token.value, detail.value.id, {
      action: payload.action,
      reviewComment: reviewComment.value || undefined
    });
    setStatus(payload.action === "REWORK" ? "整改任务已打回重做" : "整改任务已确认复核", "success");
    await loadDetail();
  } catch (error) {
    setStatus(resolveErrorMessage(error, "整改复核失败"), "error");
  } finally {
    actionLoading.value = false;
  }
}

function goBack() {
  router.push({ name: "regulator-admin-rectifications" }).catch(() => {});
}

function openImagePreview(urls, index) {
  if (!Array.isArray(urls) || !urls.length) return;
  const targetUrl = urls[index];
  if (targetUrl) {
    window.open(targetUrl, "_blank", "noopener,noreferrer");
  }
}

onMounted(loadDetail);
watch(() => route.params.rectificationId, loadDetail);
</script>

<style scoped>
.rect-detail-page { display: grid; gap: 16px; }
.state-card { padding: 20px; border-radius: 4px; border: 1px solid #e2e8f0; background: #fff; color: #64748b; }
.state-card--error { color: #991b1b; background: #fef2f2; border-color: #fecaca; }

.head { display: flex; justify-content: space-between; align-items: flex-end; gap: 14px; flex-wrap: wrap; }
.crumbs { display: flex; gap: 6px; color: #64748b; font-size: 11px; font-weight: 700; }
.sep { opacity: 0.55; }
.title-row { margin-top: 8px; display: flex; align-items: center; gap: 10px; flex-wrap: wrap; }
.title-row h1 { margin: 0; font-size: 30px; font-weight: 900; color: #002660; letter-spacing: -0.02em; }
.status-chip { display: inline-flex; min-height: 22px; align-items: center; padding: 0 10px; border-radius: 2px; font-size: 10px; font-weight: 900; }
.status-chip.is-ongoing { background: #c9d7fe; color: #1e3a8a; }
.status-chip.is-submitted { background: #002660; color: #fff; }
.status-chip.is-rework { background: #ffdbce; color: #7c2d06; }
.status-chip.is-confirmed { background: #def7ec; color: #065f46; }
.head-actions { display: flex; gap: 8px; }

.primary, .ghost, .danger { border-radius: 3px; min-height: 38px; font-size: 12px; font-weight: 800; padding: 0 14px; cursor: pointer; }
.primary { border: 0; background: #002660; color: #fff; }
.ghost { border: 1px solid #d1d5db; background: #fff; color: #334155; }
.danger { border: 0; background: #ba1a1a; color: #fff; }

.content-grid { display: grid; grid-template-columns: minmax(0, 1fr) 320px; gap: 14px; align-items: start; }
.left-col, .right-col { display: grid; gap: 14px; }
.panel { background: #fff; border: 1px solid #e2e8f0; border-radius: 4px; padding: 14px; }
.panel-large { padding: 16px; }
.panel h4 { margin: 0 0 12px; color: #002660; font-size: 12px; font-weight: 900; text-transform: uppercase; letter-spacing: 0.08em; }

.panel-issue,
.panel-submission {
  background: #fff;
}
.issue-meta {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
  margin-bottom: 12px;
}
.issue-meta span {
  display: block;
  color: #64748b;
  font-size: 11px;
  font-weight: 700;
  margin-bottom: 4px;
}
.issue-meta strong {
  color: #0f172a;
  font-size: 24px;
  font-weight: 800;
}
.issue-desc {
  background: #f8fafc;
  border-radius: 2px;
  padding: 12px;
  border-left: 3px solid #cbd5e1;
}
.issue-desc label {
  display: block;
  margin-bottom: 4px;
  color: #64748b;
  font-size: 11px;
  font-weight: 800;
}
.issue-desc p {
  margin: 0;
  color: #334155;
  font-size: 14px;
  line-height: 1.7;
  white-space: pre-line;
}
.issue-desc--submission {
  border-left-color: #94a3b8;
}
.proof-grid {
  margin-top: 12px;
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
}
.proof-thumb {
  border: 1px solid #dbe2ea;
  border-radius: 2px;
  overflow: hidden;
  background: #fff;
  min-height: 120px;
  cursor: pointer;
}
.proof-thumb img {
  width: 100%;
  height: 118px;
  object-fit: cover;
  display: block;
}
.proof-thumb--more {
  color: #64748b;
  font-size: 13px;
  font-weight: 700;
  background: #f8fafc;
  border-style: dashed;
}
.proof-more-btn {
  margin-top: 8px;
  border: 1px solid #cdd5df;
  background: #fff;
  color: #334155;
  border-radius: 2px;
  min-height: 32px;
  padding: 0 12px;
  font-size: 12px;
  font-weight: 700;
  cursor: pointer;
}
.proof-more-btn--muted {
  color: #64748b;
}
.submission-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 10px;
  margin-bottom: 8px;
}
.submission-head h4 {
  margin: 0;
}
.submit-time {
  display: inline-flex;
  align-items: center;
  background: #002660;
  color: #fff;
  border-radius: 2px;
  padding: 4px 10px;
  font-size: 11px;
  font-weight: 700;
}
.proof-empty {
  margin-top: 12px;
  color: #94a3b8;
  font-size: 12px;
}

.log-list { position: relative; display: grid; gap: 12px; }
.log-list::before { content: ""; position: absolute; left: 5px; top: 8px; bottom: 8px; width: 2px; background: #e2e8f0; }
.log-item { position: relative; display: grid; grid-template-columns: 1fr auto; gap: 8px 12px; padding-left: 20px; }
.log-dot { position: absolute; left: 0; top: 4px; width: 12px; height: 12px; border-radius: 50%; background: #002660; border: 2px solid #fff; box-shadow: 0 0 0 1px #cbd5e1; }
.log-main strong { color: #0f172a; font-size: 12px; }
.log-main p { margin: 4px 0 0; color: #64748b; font-size: 12px; line-height: 1.5; }
.log-item time { color: #94a3b8; font-size: 10px; white-space: nowrap; }
.log-empty { color: #94a3b8; font-size: 12px; padding-left: 20px; }

.panel-blue { background: linear-gradient(135deg, #002660, #003a8c); border: 0; color: #fff; }
.panel-blue h4 { color: rgba(255, 255, 255, 0.75); }
.warn-row { display: flex; justify-content: space-between; align-items: center; font-size: 12px; }
.warn-row span { color: rgba(255, 255, 255, 0.7); }
.warn-row strong.is-overdue { color: #fecaca; }
.warn-row strong.is-warning { color: #fde68a; }
.warn-row strong.is-normal { color: #bbf7d0; }

.panel-review {
  background: #f1f5f9;
  border-color: #dbe2ea;
}
.panel-review h4 {
  margin-bottom: 4px;
}
.review-desc {
  margin: 0 0 12px;
  font-size: 12px;
  color: #64748b;
  font-weight: 600;
}
.form-label { display: grid; gap: 8px; color: #0f172a; font-size: 12px; font-weight: 800; }
textarea {
  width: 100%;
  box-sizing: border-box;
  border: 1px solid #cfd8e3;
  border-radius: 2px;
  padding: 12px 14px;
  font-size: 13px;
  color: #0f172a;
  resize: vertical;
  min-height: 110px;
  background: #fff;
}
textarea::placeholder { color: #94a3b8; }
.action-stack { margin-top: 12px; display: grid; grid-template-columns: 1fr 1fr; gap: 12px; }
.review-btn {
  min-height: 54px;
  border-radius: 2px;
  font-size: 16px;
  font-weight: 800;
  cursor: pointer;
  line-height: 1;
  letter-spacing: 0.01em;
  padding: 0 16px;
}
.review-btn--confirm {
  border: 0;
  background: #002660;
  color: #fff;
}
.review-btn--rework {
  border: 1px solid #b91c1c;
  background: #fff;
  color: #b91c1c;
}
.review-btn:disabled { opacity: 0.6; cursor: not-allowed; }

.status { position: fixed; right: 18px; bottom: 18px; border-radius: 3px; padding: 10px 12px; color: #fff; background: #0f172a; font-size: 13px; z-index: 1200; }
.status.error { background: #b91c1c; }
.status.success { background: #166534; }

@media (max-width: 1180px) {
  .content-grid { grid-template-columns: 1fr; }
}
@media (max-width: 760px) {
  .title-row h1 { font-size: 24px; }
  .issue-meta { grid-template-columns: 1fr; }
  .issue-meta strong { font-size: 18px; }
  .proof-grid { grid-template-columns: 1fr; }
  .action-stack { grid-template-columns: 1fr; }
}
</style>
