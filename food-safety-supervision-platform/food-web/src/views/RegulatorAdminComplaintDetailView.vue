<template>
  <div class="app-shell detail-shell">
    <div class="hero-panel">
      <div class="hero-content">
        <span class="badge">投诉详情</span>
        <h1>投诉派发</h1>
        <p>聚焦派发与统筹，突出重点信息。</p>
      </div>
      <div class="hero-actions">
        <button class="ghost" type="button" @click="handleBack">返回列表</button>
      </div>
    </div>

    <div class="form-panel">
      <div class="card complaint-detail-card">
        <div class="section-title">核心信息</div>
        <div v-if="loading" class="status info">加载中...</div>
        <div v-else-if="!complaint" class="status error">投诉信息未找到</div>
        <template v-else>
          <div class="summary-grid summary-grid--compact">
            <div>
              <span>投诉号</span>
              <strong>{{ complaint.complaintNo || "-" }}</strong>
            </div>
            <div>
              <span>状态</span>
              <strong>{{ formatComplaintStatus(complaint.status) }}</strong>
            </div>
            <div>
              <span>企业</span>
              <strong>{{ enterprise?.enterpriseName || complaint.enterpriseName || "-" }}</strong>
            </div>
            <div>
              <span>更新时间</span>
              <strong>{{ formatTime(complaint.updateTime) }}</strong>
            </div>
          </div>

          <div class="detail-grid">
            <div class="detail-card">
              <div class="section-title">投诉内容</div>
              <div class="text-block">
                <p class="text-content" :class="{ clamped: shouldClampComplaintContent && !contentExpanded }">
                  {{ complaint.content || "-" }}
                </p>
                <button
                  v-if="shouldClampComplaintContent"
                  class="link-button"
                  type="button"
                  @click="toggleContent"
                >
                  {{ contentExpanded ? "收起" : "展开" }}
                </button>
              </div>

              <div class="section-title">现场图片</div>
              <div class="image-grid" v-if="complaintImageList.length">
                <button
                  v-for="(url, index) in complaintImageThumbs"
                  :key="`${url}-${index}`"
                  class="image-thumb"
                  type="button"
                  @click="openImagePreview(complaintImageList, index)"
                >
                  <img :src="url" alt="现场图片" />
                </button>
                <button
                  v-if="complaintImageRemain > 0"
                  class="image-thumb image-thumb--more"
                  type="button"
                  @click="openImagePreview(complaintImageList, 0)"
                >
                  +{{ complaintImageRemain }}
                </button>
              </div>
              <div v-else class="status info">未上传现场图片</div>

              <div class="section-title">企业信息</div>
              <div class="info-grid info-grid--compact">
                <div>
                  <span>企业名称</span>
                  <strong>{{ enterprise?.enterpriseName || complaint.enterpriseName || "-" }}</strong>
                </div>
                <div>
                  <span>详细地址</span>
                  <strong>{{ enterprise?.addressDetail || "-" }}</strong>
                </div>
              </div>
            </div>

            <div class="detail-card">
              <div class="section-title">流转信息</div>
              <div class="flow-list">
                <div class="flow-item">
                  <span>受理信息</span>
                  <strong>{{ complaint.acceptedByName || "暂无受理记录" }}</strong>
                  <em>{{ formatTime(complaint.acceptedTime) }}</em>
                </div>
                <div class="flow-item">
                  <span>派发信息</span>
                  <strong>{{ complaint.assignedToName || "暂无派发记录" }}</strong>
                  <em>{{ formatTime(complaint.assignedTime) }}</em>
                </div>
                <div class="flow-item">
                  <span>办理时限</span>
                  <strong>{{ formatTime(complaint.deadlineTime) }}</strong>
                  <em>投诉派发后公众端可查看办理时限</em>
                </div>
                <div class="flow-item">
                  <span>处理完成</span>
                  <strong>{{ complaint.processedByName || "暂无处理完成记录" }}</strong>
                  <em>{{ formatTime(complaint.processedTime) }}</em>
                </div>
              </div>

              <div v-if="complaint.feedbackSummary || complaint.rejectReason || latestHandle" class="section-title">
                结果信息
              </div>
              <div v-if="complaint.feedbackSummary || complaint.rejectReason || latestHandle" class="result-card">
                <p v-if="complaint.feedbackSummary">反馈摘要：{{ complaint.feedbackSummary }}</p>
                <p v-if="complaint.rejectReason">驳回原因：{{ complaint.rejectReason }}</p>
                <template v-if="latestHandle">
                  <div class="result-meta">
                    <strong>{{ latestHandle.handlerName || "-" }}</strong>
                    <span>{{ formatTime(latestHandle.handleTime) }}</span>
                  </div>
                  <p>{{ latestHandle.handleResult || "-" }}</p>
                </template>
              </div>

              <div class="section-title">派发处理</div>
              <div class="dispatch-panel">
                <div v-if="canAccept" class="dispatch-actions">
                  <span>该投诉尚未受理，请先受理。</span>
                  <button class="primary" type="button" :disabled="loadingAction" @click="handleAccept">
                    受理投诉
                  </button>
                </div>
                <div v-else-if="canAssign" class="dispatch-actions">
                  <label>
                    指派执法人员
                    <select v-model="assignForm.regulatorId">
                      <option value="">请选择</option>
                      <option v-for="item in enforcers" :key="item.id" :value="item.id">
                        {{ item.name }}
                      </option>
                    </select>
                  </label>
                  <label>
                    办理时限
                    <input v-model="assignForm.deadlineTime" type="datetime-local" />
                  </label>
                  <button class="primary" type="button" :disabled="loadingAction" @click="handleAssign">
                    确认派发
                  </button>
                </div>
                <div v-else class="status info">当前状态不可派发</div>
                <div v-if="canReject" class="dispatch-actions">
                  <label>
                    驳回原因
                    <textarea v-model.trim="rejectForm.reason" rows="3" placeholder="请输入驳回原因"></textarea>
                  </label>
                  <button class="ghost" type="button" :disabled="loadingAction" @click="handleReject">
                    驳回投诉
                  </button>
                </div>
              </div>
            </div>
          </div>

          <div v-if="status.message" class="status" :class="status.type">
            {{ status.message }}
          </div>
        </template>
      </div>
    </div>
  </div>

  <div v-if="currentImagePreviewUrl" class="image-preview-mask" @click.self="closeImagePreview">
    <div class="image-preview-card">
      <img :src="currentImagePreviewUrl" alt="现场图片大图" />
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
import { computed, onMounted, reactive, ref, watch } from "vue";
import {
  acceptComplaint,
  assignComplaint,
  fetchComplaintDetail,
  rejectComplaint
} from "../api/complaint";
import {
  fetchEligibleRegulators
} from "../api/regulation";

const props = defineProps({
  token: {
    type: String,
    required: true
  },
  complaintId: {
    type: [String, Number],
    required: true
  }
});

const emit = defineEmits(["back"]);

const loading = ref(false);
const loadingAction = ref(false);
const detail = ref(null);
const status = reactive({ message: "", type: "" });
const enforcers = ref([]);
const assignForm = reactive({ regulatorId: "", deadlineTime: "" });
const rejectForm = reactive({ reason: "" });
const contentExpanded = ref(false);
const imagePreviewUrls = ref([]);
const imagePreviewIndex = ref(0);

const complaint = computed(() => detail.value?.complaint || null);
const enterprise = computed(() => detail.value?.enterprise || null);
const latestHandle = computed(() => detail.value?.handles?.[0] || null);
const currentImagePreviewUrl = computed(
  () => imagePreviewUrls.value[imagePreviewIndex.value] || ""
);

const complaintImageList = computed(() => complaint.value?.imageUrls || []);
const complaintImageThumbs = computed(() => complaintImageList.value.slice(0, 3));
const complaintImageRemain = computed(() =>
  Math.max(complaintImageList.value.length - complaintImageThumbs.value.length, 0)
);

const shouldClampComplaintContent = computed(() => {
  const text = complaint.value?.content || "";
  return text.length > 120;
});

const canAccept = computed(() => complaint.value?.status === "SUBMITTED");
const canAssign = computed(() => ["PENDING", "ASSIGNED", "PROCESSING"].includes(complaint.value?.status || ""));
const canReject = computed(() => ["SUBMITTED", "PENDING"].includes(complaint.value?.status || ""));

const complaintStatusMap = {
  SUBMITTED: "已提交",
  PENDING: "已受理",
  ASSIGNED: "已派发",
  PROCESSING: "处理中",
  FEEDBACKED: "已反馈",
  REJECTED: "已驳回"
};

async function loadDetail() {
  if (!props.complaintId) {
    detail.value = null;
    return;
  }
  loading.value = true;
  setStatus("");
  try {
    detail.value = await fetchComplaintDetail(props.token, props.complaintId);
    assignForm.regulatorId = "";
    assignForm.deadlineTime = "";
    rejectForm.reason = "";
    contentExpanded.value = false;
    await loadEnforcers(detail.value?.enterprise?.regionId);
  } catch (error) {
    detail.value = null;
    setStatus(error.message || "加载投诉详情失败", "error");
  } finally {
    loading.value = false;
  }
}

async function loadEnforcers(regionId) {
  enforcers.value = [];
  if (!regionId) return;
  try {
    const data = await fetchEligibleRegulators(props.token, regionId);
    enforcers.value = Array.isArray(data) ? data : [];
  } catch (error) {
    enforcers.value = [];
    setStatus(error.message || "加载执法人员失败", "error");
  }
}

async function handleAccept() {
  if (!complaint.value?.id) return;
  loadingAction.value = true;
  setStatus("");
  try {
    await acceptComplaint(props.token, complaint.value.id);
    setStatus("投诉已受理", "success");
    await loadDetail();
  } catch (error) {
    setStatus(error.message || "投诉受理失败", "error");
  } finally {
    loadingAction.value = false;
  }
}

async function handleAssign() {
  if (!complaint.value?.id) return;
  if (!assignForm.regulatorId) {
    setStatus("请选择执法人员", "error");
    return;
  }
  loadingAction.value = true;
  setStatus("");
  try {
    await assignComplaint(props.token, complaint.value.id, {
      regulatorId: assignForm.regulatorId,
      deadlineTime: normalizeDateTime(assignForm.deadlineTime)
    });
    setStatus("派发成功", "success");
    await loadDetail();
  } catch (error) {
    setStatus(error.message || "派发失败", "error");
  } finally {
    loadingAction.value = false;
  }
}

async function handleReject() {
  if (!complaint.value?.id) return;
  if (!rejectForm.reason.trim()) {
    setStatus("请填写驳回原因", "error");
    return;
  }
  loadingAction.value = true;
  setStatus("");
  try {
    await rejectComplaint(props.token, complaint.value.id, { reason: rejectForm.reason });
    setStatus("投诉已驳回", "success");
    await loadDetail();
  } catch (error) {
    setStatus(error.message || "驳回失败", "error");
  } finally {
    loadingAction.value = false;
  }
}

function toggleContent() {
  contentExpanded.value = !contentExpanded.value;
}

function openImagePreview(urls, index) {
  if (!Array.isArray(urls) || !urls.length) return;
  imagePreviewUrls.value = urls;
  imagePreviewIndex.value = Math.min(Math.max(index || 0, 0), urls.length - 1);
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

function handleBack() {
  emit("back");
}

function formatTime(value) {
  if (!value) return "-";
  return String(value).replace("T", " ").slice(0, 16);
}

function normalizeDateTime(value) {
  if (!value) return undefined;
  return value.length === 16 ? `${value}:00` : value;
}

function formatComplaintStatus(value) {
  return complaintStatusMap[value] || value || "-";
}

function setStatus(message = "", type = "") {
  status.message = message;
  status.type = type;
}

onMounted(loadDetail);
watch(() => props.complaintId, loadDetail);
</script>

<style scoped>
.detail-shell {
  grid-template-columns: 1fr;
}

.detail-shell .hero-panel {
  padding: 40px 80px 24px;
}

.detail-shell .form-panel {
  padding: 10px 80px 60px;
  align-items: flex-start;
}

.detail-shell .card {
  max-width: 960px;
  width: 100%;
}

.hero-actions {
  margin-left: auto;
}

.summary-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px 24px;
  margin-top: 10px;
}

.summary-grid--compact {
  gap: 12px 20px;
}

.summary-grid span,
.info-grid span {
  display: block;
  font-size: 12px;
  color: var(--muted);
  margin-bottom: 6px;
}

.summary-grid strong,
.info-grid strong {
  font-size: 14px;
}

.text-block {
  display: grid;
  gap: 8px;
}

.text-content {
  margin: 0;
  white-space: pre-line;
  line-height: 1.6;
}

.text-content.clamped {
  overflow: hidden;
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
}

.link-button {
  background: transparent;
  border: none;
  color: var(--primary);
  padding: 0;
  font-size: 12px;
  cursor: pointer;
  text-align: left;
}

.image-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(120px, 1fr));
  gap: 10px;
}

.image-thumb {
  border-radius: 10px;
  overflow: hidden;
  border: 1px solid var(--stroke);
  padding: 0;
  background: var(--card-strong);
  cursor: pointer;
}

.image-thumb img {
  width: 100%;
  height: 96px;
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

.detail-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.1fr) minmax(0, 0.9fr);
  gap: 18px;
  margin-top: 18px;
}

.detail-card {
  border-radius: 14px;
  border: 1px solid var(--stroke);
  background: var(--card-strong);
  padding: 16px;
  display: grid;
  gap: 12px;
}

.info-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px 24px;
}

.info-grid--compact {
  grid-template-columns: 1fr;
  gap: 12px;
}

.flow-list {
  display: grid;
  gap: 12px;
}

.flow-item {
  padding: 12px 14px;
  border-radius: 12px;
  border: 1px solid var(--stroke);
  background: var(--card-strong);
  display: grid;
  gap: 4px;
}

.flow-item span {
  font-size: 12px;
  color: var(--muted);
}

.flow-item em {
  font-size: 12px;
  color: var(--muted);
}

.dispatch-panel {
  display: grid;
  gap: 10px;
}

.dispatch-actions {
  display: grid;
  gap: 10px;
}

.dispatch-actions label {
  display: grid;
  gap: 6px;
}

.dispatch-actions select {
  width: 100%;
}

.result-card {
  border-radius: 12px;
  border: 1px solid var(--stroke);
  background: var(--card-strong);
  padding: 12px 14px;
  display: grid;
  gap: 8px;
}

.result-card p {
  margin: 0;
  color: var(--ink);
}

.result-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  font-size: 12px;
  color: var(--muted);
}

.image-preview-mask {
  position: fixed;
  inset: 0;
  background: rgba(15, 23, 42, 0.55);
  display: grid;
  place-items: center;
  z-index: 9999;
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

@media (max-width: 1100px) {
  .detail-shell .hero-panel {
    padding: 32px 40px 20px;
  }

  .detail-shell .form-panel {
    padding: 10px 40px 50px;
  }

  .detail-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 720px) {
  .summary-grid {
    grid-template-columns: 1fr;
  }
}
</style>
