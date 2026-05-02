<template>
  <div v-if="visible && detail" class="modal-mask" @click.self="handleClose">
    <div class="modal-card rectification-detail-modal">
      <div class="modal-title">整改详情</div>
      <div class="rectification-head">
        <AppStatusTag class="rectification-status" :label="formatRectificationStatus(detail.status)" :tone="statusTone" />
        <span class="rectification-id">整改 ID：{{ detail.id || "-" }}</span>
      </div>

      <div v-if="!detailLoading" class="rectification-section">
        <div class="rectification-field">
          <span>企业</span>
          <strong>{{ detail.enterpriseName || "-" }}</strong>
        </div>
        <div class="rectification-field">
          <span>检查记录 ID</span>
          <strong>{{ detail.inspectionId || "-" }}</strong>
        </div>
        <div class="rectification-field rectification-field--full">
          <span>整改要求</span>
          <strong>{{ detail.rectificationDesc || "-" }}</strong>
        </div>
        <div class="rectification-field rectification-field--full">
          <span>整改进展</span>
          <strong>{{ detail.progress || "企业暂未提交整改进展。" }}</strong>
        </div>
      </div>
      <div v-else class="detail-loading">整改详情加载中...</div>

      <div class="timeline-section">
        <div class="timeline-title">状态时间线</div>
        <div v-if="detailLoading" class="detail-loading">时间线加载中...</div>
        <div v-else ref="timelineRoot" class="timeline-list">
          <div
            v-for="item in timelineItems"
            :key="item.key"
            class="timeline-item"
            :class="{ 'timeline-item--focused': isFocusedItem(item) }"
            :data-timeline-key="item.key"
          >
            <span class="timeline-dot" :class="{ active: item.active, done: item.done }"></span>
            <div class="timeline-content">
              <div class="timeline-header">
                <div class="timeline-label">{{ item.label }}</div>
                <div class="timeline-time">{{ item.time ? formatTime(item.time) : "待处理" }}</div>
              </div>
              <div v-if="item.operatorName" class="timeline-meta">操作人：{{ item.operatorName }}</div>
              <div v-if="item.comment" class="timeline-comment">{{ item.comment }}</div>

              <div v-if="item.attachments.length" class="timeline-attachments">
                <button
                  v-for="(url, index) in toAttachmentThumbs(item.attachments)"
                  :key="`${item.key}-thumb-${index}`"
                  class="image-thumb"
                  type="button"
                  @click="openImagePreview(item.attachments, index)"
                >
                  <img :src="url" alt="整改附件" />
                </button>
                <button
                  v-if="item.attachments.length > 4"
                  class="image-thumb image-thumb--more"
                  type="button"
                  @click="openImagePreview(item.attachments, 0)"
                >
                  +{{ item.attachments.length - 4 }}
                </button>
              </div>
            </div>
          </div>
        </div>
      </div>

      <div v-if="highlightLatestRework && latestReworkLog" class="rework-highlight">
        <div class="rework-highlight-title">最近一次退回意见</div>
        <div class="rework-highlight-meta">
          <span>操作人：{{ latestReworkLog.operatorName || "-" }}</span>
          <span>时间：{{ formatTime(latestReworkLog.createTime) }}</span>
        </div>
        <div class="rework-highlight-content">{{ latestReworkLog.actionComment || "未填写退回意见。" }}</div>
      </div>

      <div v-if="highlightLatestSubmit && latestEnterpriseSubmitLog" class="submit-highlight">
        <div class="submit-highlight-title">最近一次企业提交说明</div>
        <div class="submit-highlight-meta">
          <span>提交方：{{ latestEnterpriseSubmitLog.operatorName || "-" }}</span>
          <span>时间：{{ formatTime(latestEnterpriseSubmitLog.createTime) }}</span>
        </div>
        <div class="submit-highlight-content">{{ latestEnterpriseSubmitLog.actionComment || "未填写整改说明。" }}</div>
      </div>

      <div v-if="reviewable" class="review-section">
        <label>
          复核意见（退回整改时必填）
          <textarea
            v-model.trim="reviewComment"
            rows="3"
            maxlength="1000"
            placeholder="请输入复核意见"
            :disabled="reviewing"
          ></textarea>
        </label>
        <div v-if="reviewError" class="review-error">{{ reviewError }}</div>
      </div>

      <div class="modal-actions rectification-modal-actions">
        <template v-if="reviewable">
          <button class="primary" type="button" :disabled="reviewing" @click="handleConfirm">
            {{ reviewing ? "处理中..." : "复核通过" }}
          </button>
          <button class="warning" type="button" :disabled="reviewing" @click="handleRework">
            {{ reviewing ? "处理中..." : "退回整改" }}
          </button>
        </template>
        <button class="ghost" type="button" @click="handleClose">关闭</button>
      </div>
    </div>
  </div>

  <div v-if="currentImagePreviewUrl" class="image-preview-mask" @click.self="closeImagePreview">
    <div class="image-preview-card">
      <img :src="currentImagePreviewUrl" alt="整改附件大图" />
      <div class="image-preview-actions">
        <button class="ghost" type="button" :disabled="imagePreviewIndex <= 0" @click="showPrevImage">
          上一张
        </button>
        <span class="image-preview-count">{{ imagePreviewIndex + 1 }}/{{ imagePreviewUrls.length }}</span>
        <button
          class="ghost"
          type="button"
          :disabled="imagePreviewIndex >= imagePreviewUrls.length - 1"
          @click="showNextImage"
        >
          下一张
        </button>
        <button class="primary" type="button" @click="closeImagePreview">关闭</button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, nextTick, ref, watch } from "vue";
import AppStatusTag from "./common/AppStatusTag.vue";
import { getStatusTone } from "../utils/statusMaps";
import { formatRectificationActionLabel, formatRectificationStatus } from "../views/enterprise/enterpriseShared";

const props = defineProps({
  visible: {
    type: Boolean,
    default: false
  },
  detail: {
    type: Object,
    default: null
  },
  actionLogs: {
    type: Array,
    default: () => []
  },
  detailLoading: {
    type: Boolean,
    default: false
  },
  highlightLatestRework: {
    type: Boolean,
    default: false
  },
  highlightLatestSubmit: {
    type: Boolean,
    default: false
  },
  focusActionType: {
    type: String,
    default: ""
  },
  reviewable: {
    type: Boolean,
    default: false
  },
  reviewing: {
    type: Boolean,
    default: false
  }
});

const emit = defineEmits(["close", "review"]);

const reviewComment = ref("");
const reviewError = ref("");
const timelineRoot = ref(null);
const imagePreviewUrls = ref([]);
const imagePreviewIndex = ref(0);

const currentImagePreviewUrl = computed(() => imagePreviewUrls.value[imagePreviewIndex.value] || "");
const statusTone = computed(() => getStatusTone(props.detail?.status, "RECTIFICATION"));

watch(
  () => [props.visible, props.detail?.id],
  () => {
    reviewComment.value = "";
    reviewError.value = "";
    closeImagePreview();
  }
);

function formatTime(value) {
  if (!value) return "-";
  return String(value).replace("T", " ").slice(0, 16);
}

function toAttachmentThumbs(urls) {
  if (!Array.isArray(urls)) return [];
  return urls.slice(0, 4);
}

const timelineItems = computed(() => {
  if (Array.isArray(props.actionLogs) && props.actionLogs.length) {
    return props.actionLogs.map((log, index) => ({
      key: log.id || `${log.actionType}-${index}`,
      actionType: String(log.actionType || "").toUpperCase(),
      label: formatRectificationActionLabel(log.actionType, log.actionName),
      time: log.createTime,
      operatorName: log.operatorName || null,
      comment: log.actionComment || "",
      attachments: Array.isArray(log.attachmentUrls) ? log.attachmentUrls : [],
      done: true,
      active: index === props.actionLogs.length - 1
    }));
  }

  const status = props.detail?.status || "";
  const submittedDone = ["SUBMITTED", "REWORK", "CONFIRMED"].includes(status);
  const reworkDone = status === "REWORK";
  const confirmedDone = status === "CONFIRMED";
  return [
    {
      key: "create",
      actionType: "SYSTEM_CREATE",
      label: "生成整改任务",
      time: props.detail?.createTime,
      operatorName: null,
      comment: "",
      attachments: [],
      done: Boolean(props.detail?.createTime),
      active: status === "ONGOING"
    },
    {
      key: "submit",
      actionType: "ENTERPRISE_SUBMIT",
      label: "企业提交整改",
      time: props.detail?.finishTime,
      operatorName: null,
      comment: "",
      attachments: [],
      done: submittedDone,
      active: status === "SUBMITTED"
    },
    {
      key: "rework",
      actionType: "REVIEW_REWORK",
      label: "监管退回整改",
      time: reworkDone ? props.detail?.updateTime : null,
      operatorName: null,
      comment: "",
      attachments: [],
      done: reworkDone,
      active: reworkDone
    },
    {
      key: "confirm",
      actionType: "REVIEW_CONFIRM",
      label: "监管复核通过",
      time: props.detail?.confirmedTime,
      operatorName: props.detail?.confirmedByName || null,
      comment: "",
      attachments: [],
      done: confirmedDone,
      active: confirmedDone
    }
  ];
});

const latestReworkLog = computed(() => {
  if (!Array.isArray(props.actionLogs) || !props.actionLogs.length) {
    return null;
  }
  for (let i = props.actionLogs.length - 1; i >= 0; i -= 1) {
    const log = props.actionLogs[i];
    if (String(log?.actionType || "").toUpperCase() === "REVIEW_REWORK") {
      return log;
    }
  }
  return null;
});

const latestEnterpriseSubmitLog = computed(() => {
  if (!Array.isArray(props.actionLogs) || !props.actionLogs.length) {
    return null;
  }
  for (let i = props.actionLogs.length - 1; i >= 0; i -= 1) {
    const log = props.actionLogs[i];
    if (String(log?.actionType || "").toUpperCase() === "ENTERPRISE_SUBMIT") {
      return log;
    }
  }
  return null;
});

const focusedTimelineKey = computed(() => {
  const focusType = String(props.focusActionType || "").toUpperCase();
  if (!focusType || !timelineItems.value.length) {
    return "";
  }
  for (let i = timelineItems.value.length - 1; i >= 0; i -= 1) {
    if (String(timelineItems.value[i].actionType || "").toUpperCase() === focusType) {
      return timelineItems.value[i].key;
    }
  }
  return "";
});

function isFocusedItem(item) {
  return Boolean(focusedTimelineKey.value) && item.key === focusedTimelineKey.value;
}

watch(
  () => [props.visible, props.focusActionType, timelineItems.value.length],
  async () => {
    if (!props.visible || !focusedTimelineKey.value) {
      return;
    }
    await nextTick();
    timelineRoot.value
      ?.querySelector(`[data-timeline-key='${focusedTimelineKey.value}']`)
      ?.scrollIntoView({ behavior: "smooth", block: "center" });
  }
);

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

function handleClose() {
  closeImagePreview();
  emit("close");
}

function handleConfirm() {
  reviewError.value = "";
  emit("review", {
    action: "CONFIRM",
    comment: reviewComment.value || ""
  });
}

function handleRework() {
  if (!reviewComment.value) {
    reviewError.value = "请填写退回原因。";
    return;
  }
  reviewError.value = "";
  emit("review", {
    action: "REWORK",
    comment: reviewComment.value
  });
}
</script>

<style scoped>
.rectification-detail-modal {
  width: min(720px, 95vw);
}

.rectification-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  margin-bottom: 14px;
}

.rectification-status {
  min-height: 28px;
}

.rectification-id {
  font-size: 12px;
  color: var(--muted);
}

.rectification-section {
  border: 1px solid var(--stroke);
  border-radius: 12px;
  background: var(--card-strong);
  padding: 12px;
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}

.detail-loading {
  margin-top: 12px;
  border: 1px dashed var(--stroke);
  border-radius: 10px;
  padding: 12px;
  font-size: 13px;
  color: var(--muted);
}

.rectification-field span {
  display: block;
  font-size: 12px;
  color: var(--muted);
  margin-bottom: 4px;
}

.rectification-field strong {
  display: block;
  font-size: 14px;
  line-height: 1.45;
  color: var(--ink);
  word-break: break-word;
}

.rectification-field--full {
  grid-column: 1 / -1;
}

.timeline-section {
  margin-top: 14px;
  border: 1px solid var(--stroke);
  border-radius: 12px;
  background: var(--card-strong);
  padding: 12px;
}

.timeline-title {
  font-size: 13px;
  font-weight: 700;
  margin-bottom: 10px;
}

.timeline-list {
  display: grid;
  gap: 8px;
  max-height: 320px;
  overflow: auto;
  padding-right: 4px;
}

.timeline-item {
  display: grid;
  grid-template-columns: 14px 1fr;
  gap: 10px;
  align-items: start;
  border: 1px solid transparent;
  border-radius: 8px;
  padding: 4px 6px;
}

.timeline-item--focused {
  border-color: rgba(204, 122, 0, 0.35);
  background: rgba(204, 122, 0, 0.08);
}

.timeline-dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  margin-top: 4px;
  border: 2px solid #c9d6e5;
  background: #fff;
}

.timeline-dot.done {
  border-color: rgba(13, 94, 166, 0.65);
  background: rgba(13, 94, 166, 0.2);
}

.timeline-dot.active {
  border-color: var(--primary);
  background: var(--primary);
}

.timeline-header {
  display: flex;
  justify-content: space-between;
  gap: 12px;
}

.timeline-label {
  font-size: 13px;
  color: var(--ink);
}

.timeline-time {
  font-size: 12px;
  color: var(--muted);
  white-space: nowrap;
}

.timeline-meta {
  margin-top: 4px;
  font-size: 12px;
  color: var(--muted);
}

.timeline-comment {
  margin-top: 6px;
  font-size: 13px;
  color: var(--ink);
  line-height: 1.5;
  word-break: break-word;
}

.timeline-attachments {
  margin-top: 8px;
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(84px, 1fr));
  gap: 8px;
}

.image-thumb {
  border-radius: 10px;
  overflow: hidden;
  border: 1px solid var(--stroke);
  padding: 0;
  background: var(--card);
  cursor: pointer;
}

.image-thumb img {
  width: 100%;
  height: 76px;
  object-fit: cover;
  display: block;
}

.image-thumb--more {
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 600;
  color: var(--primary);
  background: rgba(15, 99, 190, 0.08);
  border-style: dashed;
}

.rework-highlight {
  margin-top: 14px;
  border: 1px solid rgba(204, 122, 0, 0.35);
  border-radius: 12px;
  background: rgba(204, 122, 0, 0.08);
  padding: 12px;
}

.rework-highlight-title {
  font-size: 13px;
  font-weight: 700;
  color: #8a4f00;
}

.rework-highlight-meta {
  margin-top: 6px;
  display: flex;
  flex-wrap: wrap;
  gap: 8px 12px;
  font-size: 12px;
  color: #8a4f00;
}

.rework-highlight-content {
  margin-top: 8px;
  font-size: 13px;
  color: var(--ink);
  line-height: 1.5;
  word-break: break-word;
}

.submit-highlight {
  margin-top: 14px;
  border: 1px solid rgba(13, 94, 166, 0.35);
  border-radius: 12px;
  background: rgba(13, 94, 166, 0.08);
  padding: 12px;
}

.submit-highlight-title {
  font-size: 13px;
  font-weight: 700;
  color: #0d4f9b;
}

.submit-highlight-meta {
  margin-top: 6px;
  display: flex;
  flex-wrap: wrap;
  gap: 8px 12px;
  font-size: 12px;
  color: #0d4f9b;
}

.submit-highlight-content {
  margin-top: 8px;
  font-size: 13px;
  color: var(--ink);
  line-height: 1.5;
  word-break: break-word;
}

.review-section {
  margin-top: 14px;
  border: 1px solid var(--stroke);
  border-radius: 12px;
  background: var(--card-strong);
  padding: 12px;
}

.review-section label {
  display: grid;
  gap: 8px;
  font-size: 13px;
  color: var(--muted);
}

.review-section textarea {
  width: 100%;
  border-radius: 10px;
  border: 1px solid var(--stroke);
  padding: 10px;
  resize: vertical;
  min-height: 88px;
  background: var(--card);
  color: var(--ink);
}

.review-error {
  margin-top: 6px;
  font-size: 12px;
  color: #c0392b;
}

.rectification-modal-actions {
  justify-content: space-between;
  gap: 10px;
  flex-wrap: wrap;
}

button.warning {
  background: #cc7a00;
  border-color: #cc7a00;
  color: #fff;
}

button.warning:hover {
  background: #b36b00;
}

.image-preview-mask {
  position: fixed;
  inset: 0;
  background: rgba(15, 23, 42, 0.55);
  display: grid;
  place-items: center;
  z-index: 10000;
}

.image-preview-card {
  background: #fff;
  border-radius: 16px;
  padding: 16px;
  max-width: min(900px, 92vw);
  max-height: 88vh;
  display: grid;
  gap: 12px;
  box-shadow: 0 24px 60px rgba(15, 23, 42, 0.25);
}

.image-preview-card img {
  width: 100%;
  height: auto;
  max-height: 70vh;
  object-fit: contain;
  border-radius: 12px;
  background: #f6f9ff;
}

.image-preview-actions {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  flex-wrap: wrap;
}

.image-preview-count {
  font-size: 12px;
  color: var(--muted);
}

@media (max-width: 720px) {
  .rectification-section {
    grid-template-columns: 1fr;
  }

  .timeline-header {
    flex-direction: column;
    gap: 2px;
  }

  .timeline-time {
    white-space: normal;
  }

  .timeline-attachments {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }

  .rectification-modal-actions {
    justify-content: flex-start;
  }
}
</style>
