<template>
  <div v-if="visible && detail" class="modal-mask" @click.self="handleClose">
    <div class="modal-card rectification-detail-modal">
      <div class="modal-title">整改详情</div>
      <div class="rectification-head">
        <span class="rectification-status">{{ formatStatus(detail.status) }}</span>
        <span class="rectification-id">整改ID：{{ detail.id || "-" }}</span>
      </div>

      <div class="rectification-section">
        <div class="rectification-field">
          <span>企业</span>
          <strong>{{ detail.enterpriseName || "-" }}</strong>
        </div>
        <div class="rectification-field">
          <span>检查记录ID</span>
          <strong>{{ detail.inspectionId || "-" }}</strong>
        </div>
        <div class="rectification-field rectification-field--full">
          <span>整改要求</span>
          <strong>{{ detail.rectificationDesc || "-" }}</strong>
        </div>
        <div class="rectification-field rectification-field--full">
          <span>整改进展</span>
          <strong>{{ detail.progress || "企业暂未提交整改进展" }}</strong>
        </div>
      </div>

      <div class="timeline-section">
        <div class="timeline-title">状态时间线</div>
        <div class="timeline-list">
          <div v-for="item in timelineItems" :key="item.key" class="timeline-item">
            <span class="timeline-dot" :class="{ active: item.active, done: item.done }"></span>
            <div class="timeline-content">
              <div class="timeline-label">{{ item.label }}</div>
              <div class="timeline-time">{{ item.time ? formatTime(item.time) : "待到达" }}</div>
            </div>
          </div>
        </div>
      </div>

      <div v-if="reviewable" class="review-section">
        <label>
          复核意见（打回重做时必填）
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
            {{ reviewing ? "处理中..." : "打回重做" }}
          </button>
        </template>
        <button class="ghost" type="button" @click="handleClose">关闭</button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, ref, watch } from "vue";

const props = defineProps({
  visible: {
    type: Boolean,
    default: false
  },
  detail: {
    type: Object,
    default: null
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

const statusMap = {
  ONGOING: "整改中",
  SUBMITTED: "待复核",
  REWORK: "打回重做",
  CONFIRMED: "已确认"
};

watch(
  () => [props.visible, props.detail?.id],
  () => {
    reviewComment.value = "";
    reviewError.value = "";
  }
);

function formatStatus(value) {
  return statusMap[value] || value || "-";
}

function formatTime(value) {
  if (!value) return "-";
  return String(value).replace("T", " ").slice(0, 16);
}

const timelineItems = computed(() => {
  const status = props.detail?.status || "";
  const submittedDone = ["SUBMITTED", "REWORK", "CONFIRMED"].includes(status);
  const reworkDone = status === "REWORK";
  const confirmedDone = status === "CONFIRMED";
  return [
    {
      key: "created",
      label: "生成整改任务",
      time: props.detail?.createTime,
      done: Boolean(props.detail?.createTime),
      active: status === "ONGOING"
    },
    {
      key: "submitted",
      label: "企业提交整改",
      time: props.detail?.finishTime,
      done: submittedDone,
      active: status === "SUBMITTED"
    },
    {
      key: "rework",
      label: "监管打回重做",
      time: reworkDone ? props.detail?.updateTime : null,
      done: reworkDone,
      active: reworkDone
    },
    {
      key: "confirmed",
      label: "监管复核通过",
      time: props.detail?.confirmedTime,
      done: confirmedDone,
      active: confirmedDone
    }
  ];
});

function handleClose() {
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
    reviewError.value = "请填写打回原因";
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
  display: inline-flex;
  align-items: center;
  padding: 4px 10px;
  border-radius: 999px;
  background: rgba(13, 94, 166, 0.12);
  color: var(--primary);
  font-size: 12px;
  font-weight: 600;
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
}

.timeline-item {
  display: grid;
  grid-template-columns: 14px 1fr;
  gap: 10px;
  align-items: start;
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

.timeline-label {
  font-size: 13px;
  color: var(--ink);
}

.timeline-time {
  font-size: 12px;
  color: var(--muted);
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

@media (max-width: 720px) {
  .rectification-section {
    grid-template-columns: 1fr;
  }

  .rectification-modal-actions {
    justify-content: flex-start;
  }
}
</style>