<template>
  <EnterpriseWorkspacePage
    active-key="rectifications"
    title="整改任务详情"
    subtitle="查看整改要求、时限与监管流转记录。"
    top-search-placeholder="搜索任务或编号..."
    :username="enterpriseUser.username"
    :user-type="enterpriseUser.userType"
    status-label="整改详情"
    status-tone="warning"
    @navigate="handleSidebarNavigate"
    @logout="handleLogout"
  >
    <div class="enterprise-workspace-card enterprise-rectification-detail-page">
      <nav class="enterprise-page-hero__crumb enterprise-rectification-detail-page__crumb" aria-label="面包屑">
        <span>任务中心</span>
        <span class="material-symbols-outlined" aria-hidden="true">chevron_right</span>
        <span class="is-current">整改任务详情</span>
      </nav>

      <div v-if="loading" class="status info">加载中...</div>
      <div v-else-if="!detail" class="status error">未找到该整改任务。</div>
      <template v-else>
        <header class="enterprise-page-hero enterprise-rectification-detail-page__hero">
          <div>
            <div class="enterprise-rectification-detail-page__title-row">
              <span class="enterprise-rectification-detail-page__task-id-pill">TASK ID</span>
              <h1 class="enterprise-page-hero__title enterprise-rectification-detail-page__title">
                #{{ rectificationId }} · {{ detail.rectificationDesc || "整改任务" }}
              </h1>
            </div>
            <div class="enterprise-rectification-detail-page__meta-row">
              <EnterpriseStatusChip :label="formatRectificationStatus(detail.status)" :tone="statusChipTone" />
              <span class="enterprise-rectification-detail-page__inline-meta">
                <span class="material-symbols-outlined" aria-hidden="true">calendar_today</span>
                截止日期：<strong>{{ detail.currentDeadline || "-" }}</strong>
              </span>
              <span class="enterprise-rectification-detail-page__inline-meta">
                <span class="material-symbols-outlined" aria-hidden="true">schedule</span>
                {{ formatRectificationSla(detail) }}
              </span>
            </div>
          </div>
          <div class="enterprise-page-hero__actions enterprise-rectification-detail-page__hero-actions">
            <RouterLink class="ghost enterprise-inline-link enterprise-rectification-detail-page__hero-back" :to="{ name: 'enterprise-rectifications' }">
              返回列表
            </RouterLink>
            <RouterLink
              v-if="canSubmit"
              class="primary enterprise-link-button enterprise-rectification-detail-page__hero-submit"
              :to="{ name: 'enterprise-rectification-submit', params: { rectificationId } }"
            >
              <span class="material-symbols-outlined" aria-hidden="true">upload_file</span>
              提交整改
            </RouterLink>
          </div>
        </header>

        <div v-if="lastReworkLog" class="enterprise-alert-rework">
          <span class="material-symbols-outlined is-filled" aria-hidden="true">info</span>
          <div>
            <h4 class="enterprise-rectification-detail-page__rework-title">监管部门退回意见</h4>
            <p class="enterprise-rectification-detail-page__rework-body">{{ reworkAlertBody }}</p>
            <p v-if="reworkAlertTime" class="enterprise-rectification-detail-page__rework-time">{{ formatTime(reworkAlertTime) }}</p>
          </div>
        </div>

        <div class="enterprise-detail-layout">
          <div>
            <section class="enterprise-panel enterprise-panel--accent-top">
              <div class="enterprise-panel__head">
                <div class="enterprise-panel__head-bar" />
                <h3>整改详情与要求</h3>
              </div>
              <div class="enterprise-rectification-detail-page__block">
                <h5>问题描述</h5>
                <p>{{ detail.rectificationDesc || "暂无进一步说明。" }}</p>
              </div>
              <div class="enterprise-rectification-detail-page__block">
                <h5>整改要求</h5>
                <ul class="enterprise-rectification-detail-page__req-list">
                  <li v-for="(line, idx) in requirementItems" :key="idx">
                    <span class="material-symbols-outlined" aria-hidden="true">check_circle</span>
                    <span>{{ line }}</span>
                  </li>
                </ul>
              </div>
            </section>

            <section class="enterprise-panel">
              <div class="enterprise-panel__head">
                <div class="enterprise-panel__head-bar" />
                <h3>流转日志</h3>
              </div>
              <div v-if="!sortedLogs.length" class="secondary-text">暂无动作记录</div>
              <div v-else class="enterprise-audit-trail-v">
                <div v-for="(log, index) in sortedLogs" :key="log.id || `${index}-${log.actionType}`" class="enterprise-audit-node">
                  <span
                    class="enterprise-audit-node__dot"
                    :class="{
                      'is-error': String(log.actionType || '').toUpperCase() === 'REVIEW_REWORK',
                      'is-muted': index > 0
                    }"
                  />
                  <div class="enterprise-rectification-detail-page__log-head">
                    <strong>{{ formatRectificationActionLabel(log.actionType, log.actionName) }}</strong>
                    <span>{{ formatTime(rectificationLogTime(log)) }}</span>
                  </div>
                  <p v-if="logComment(log)" class="enterprise-rectification-detail-page__log-comment">{{ logComment(log) }}</p>
                  <div v-if="logImageUrls(log).length" class="enterprise-rectification-detail-page__log-images">
                    <button
                      v-for="(url, imageIndex) in logImageThumbs(logImageUrls(log))"
                      :key="`${log.id || index}-img-${imageIndex}`"
                      type="button"
                      class="enterprise-rectification-detail-page__log-image-thumb"
                      @click="openImagePreview(logImageUrls(log), imageIndex)"
                    >
                      <img :src="url" alt="整改日志附件" />
                    </button>
                    <button
                      v-if="logImageUrls(log).length > 4"
                      type="button"
                      class="enterprise-rectification-detail-page__log-image-thumb enterprise-rectification-detail-page__log-image-thumb--more"
                      @click="openImagePreview(logImageUrls(log), 0)"
                    >
                      +{{ logImageUrls(log).length - 4 }}
                    </button>
                  </div>
                  <p v-if="log.operatorName" class="enterprise-rectification-detail-page__log-operator">操作方：{{ log.operatorName }}</p>
                </div>
              </div>
            </section>
          </div>

          <aside class="enterprise-side-stack">
            <div class="enterprise-countdown-card enterprise-rectification-detail-page__countdown">
              <span class="material-symbols-outlined enterprise-countdown-card__bg" aria-hidden="true">schedule</span>
              <h4>距离截止日期还有</h4>
              <div class="enterprise-rectification-detail-page__countdown-value">
                <span class="enterprise-countdown-card__value">{{ countdownDisplay.primary }}</span>
                <span>{{ countdownDisplay.unit }}</span>
              </div>
              <p>{{ detail.currentDeadline ? `${detail.currentDeadline} 自动关闭提交通道` : "未设置明确截止时刻时，以监管通知为准。" }}</p>
            </div>

            <div class="enterprise-side-card">
              <div class="enterprise-side-card__head">后续操作</div>
              <div class="enterprise-side-card__body enterprise-rectification-detail-page__actions">
                <RouterLink v-if="canSubmit" class="primary enterprise-link-button" :to="{ name: 'enterprise-rectification-submit', params: { rectificationId } }">
                  <span class="material-symbols-outlined" aria-hidden="true">upload_file</span>
                  提交整改内容
                </RouterLink>
              </div>
            </div>
          </aside>
        </div>
      </template>

      <div v-if="status.message" class="status" :class="status.type">{{ status.message }}</div>
    </div>
  </EnterpriseWorkspacePage>

  <div v-if="currentImagePreviewUrl" class="enterprise-rectification-detail-page__image-preview-mask" @click.self="closeImagePreview">
    <div class="enterprise-rectification-detail-page__image-preview-card">
      <img :src="currentImagePreviewUrl" alt="整改日志附件大图" />
      <div class="enterprise-rectification-detail-page__image-preview-actions">
        <button class="ghost" type="button" :disabled="imagePreviewIndex <= 0" @click="showPrevImage">上一张</button>
        <span>{{ imagePreviewIndex + 1 }}/{{ imagePreviewUrls.length }}</span>
        <button class="ghost" type="button" :disabled="imagePreviewIndex >= imagePreviewUrls.length - 1" @click="showNextImage">下一张</button>
        <button class="primary" type="button" @click="closeImagePreview">关闭</button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, reactive, ref, watch } from "vue";
import { RouterLink, useRoute } from "vue-router";
import { fetchRectificationActions, fetchRectificationDetail } from "../../api/regulationOperation";
import { resolveErrorMessage } from "../../utils/uiFeedback";
import EnterpriseStatusChip from "../../components/enterprise/EnterpriseStatusChip.vue";
import EnterpriseWorkspacePage from "../../components/enterprise/EnterpriseWorkspacePage.vue";
import { formatTime } from "../../utils/formatters";
import {
  formatRectificationActionLabel,
  formatRectificationStatus,
  rectificationLogTime,
  useEnterpriseShellSession
} from "./enterpriseShared";

const route = useRoute();
const { enterpriseUser, token, handleSidebarNavigate, handleLogout } = useEnterpriseShellSession();
const rectificationId = computed(() => String(route.params.rectificationId || ""));
const detail = ref(null);
const actionLogs = ref([]);
const loading = ref(false);
const status = reactive({ message: "", type: "" });
const imagePreviewUrls = ref([]);
const imagePreviewIndex = ref(0);

const currentImagePreviewUrl = computed(() => imagePreviewUrls.value[imagePreviewIndex.value] || "");

function logComment(log) {
  return log?.actionComment || log?.comment || log?.remark || "";
}

function normalizeLogUrls(raw) {
  if (!raw) return [];
  if (Array.isArray(raw)) return raw.filter(Boolean).map((value) => String(value));
  if (typeof raw === "string") return [raw];
  return [];
}

function logImageUrls(log) {
  const fromAttachmentUrls = normalizeLogUrls(log?.attachmentUrls);
  const fromImageUrls = normalizeLogUrls(log?.imageUrls);
  const fromAttachments = Array.isArray(log?.attachments)
    ? log.attachments
        .map((item) => item?.url || item?.fileUrl || item?.attachmentUrl)
        .filter(Boolean)
        .map((value) => String(value))
    : [];
  return [...new Set([...fromAttachmentUrls, ...fromImageUrls, ...fromAttachments])];
}

function logImageThumbs(urls) {
  return Array.isArray(urls) ? urls.slice(0, 4) : [];
}

function openImagePreview(urls, index = 0) {
  const validUrls = Array.isArray(urls) ? urls.filter(Boolean) : [];
  if (!validUrls.length) return;
  imagePreviewUrls.value = validUrls;
  imagePreviewIndex.value = Math.min(Math.max(index, 0), validUrls.length - 1);
}

function closeImagePreview() {
  imagePreviewUrls.value = [];
  imagePreviewIndex.value = 0;
}

function showPrevImage() {
  if (imagePreviewIndex.value <= 0) return;
  imagePreviewIndex.value -= 1;
}

function showNextImage() {
  if (imagePreviewIndex.value >= imagePreviewUrls.value.length - 1) return;
  imagePreviewIndex.value += 1;
}

const canSubmit = computed(() => detail.value && (detail.value.status === "ONGOING" || detail.value.status === "REWORK"));

const statusChipTone = computed(() => {
  const currentStatus = detail.value?.status;
  if (currentStatus === "CONFIRMED") return "success";
  if (currentStatus === "REWORK") return "danger";
  if (currentStatus === "SUBMITTED") return "warning";
  return "neutral";
});

const sortedLogs = computed(() => {
  const list = [...(actionLogs.value || [])];
  return list.sort((a, b) => {
    const timeA = new Date(rectificationLogTime(a) || 0).getTime();
    const timeB = new Date(rectificationLogTime(b) || 0).getTime();
    return timeB - timeA;
  });
});

const lastReworkLog = computed(() => sortedLogs.value.find((log) => String(log?.actionType || "").toUpperCase() === "REVIEW_REWORK"));
const reworkAlertBody = computed(() => (lastReworkLog.value ? logComment(lastReworkLog.value) : ""));
const reworkAlertTime = computed(() => (lastReworkLog.value ? rectificationLogTime(lastReworkLog.value) : ""));

const requirementItems = computed(() => {
  const text = (detail.value?.rectificationDesc || "").trim();
  if (!text) return ["请按监管要求补充整改措施，并提交佐证材料。"];
  const parts = text
    .split(/[\n;；。]+/)
    .map((item) => item.trim())
    .filter(Boolean);
  return parts.slice(0, 3);
});

const countdownDisplay = computed(() => {
  const currentDetail = detail.value;
  if (!currentDetail?.currentDeadline) return { primary: "-", unit: "" };
  const endTime = new Date(currentDetail.currentDeadline);
  if (Number.isNaN(endTime.getTime())) return { primary: "-", unit: "" };
  const days = Math.ceil((endTime.getTime() - Date.now()) / 86400000);
  if (currentDetail.slaStatus === "OVERDUE") return { primary: "已逾期", unit: "" };
  if (days >= 0) return { primary: String(Math.max(0, days)), unit: "天" };
  return { primary: "已逾期", unit: "" };
});

function formatDurationMinutes(minutes) {
  const total = Math.max(0, Number(minutes) || 0);
  const days = Math.floor(total / (24 * 60));
  const hours = Math.floor((total % (24 * 60)) / 60);
  const mins = total % 60;
  if (days > 0) return `${days}天${hours}小时`;
  if (hours > 0) return `${hours}小时${mins}分钟`;
  return `${mins}分钟`;
}

function formatRectificationSla(item) {
  if (!item) return "-";
  const remaining = Number(item.remainingMinutes);
  if (item.slaStatus === "OVERDUE") return `已超时 ${formatDurationMinutes(Math.abs(remaining))}`;
  if (item.slaStatus === "DUE_SOON") return `即将超时 ${formatDurationMinutes(remaining)}`;
  if (item.slaStatus === "NORMAL") return `剩余 ${formatDurationMinutes(remaining)}`;
  if (item.currentDeadline) return `截止 ${item.currentDeadline}`;
  return "-";
}

async function loadDetail() {
  if (!rectificationId.value) {
    detail.value = null;
    actionLogs.value = [];
    return;
  }

  loading.value = true;
  status.message = "";
  status.type = "";

  try {
    const [detailData, actionData] = await Promise.all([
      fetchRectificationDetail(token.value, rectificationId.value),
      fetchRectificationActions(token.value, rectificationId.value)
    ]);
    detail.value = detailData || null;
    actionLogs.value = Array.isArray(actionData) ? actionData : [];
  } catch (error) {
    detail.value = null;
    actionLogs.value = [];
    status.message = resolveErrorMessage(error, "加载整改详情失败");
    status.type = "error";
  } finally {
    loading.value = false;
  }
}

watch(
  () => route.params.rectificationId,
  () => {
    closeImagePreview();
    loadDetail();
  },
  { immediate: true }
);
</script>
