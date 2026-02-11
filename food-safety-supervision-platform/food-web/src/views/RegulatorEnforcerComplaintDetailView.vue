<template>
  <div class="app-shell detail-shell">
    <div class="hero-panel">
      <div class="hero-content">
        <span class="badge">投诉详情</span>
        <h1>执法处理</h1>
        <p>聚焦事实与处理结果，减少非必要干扰。</p>
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

          <div class="detail-stack">
            <div>
              <div class="section-title">投诉内容</div>
              <p class="text-content">{{ complaint.content || "-" }}</p>
            </div>

            <div>
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
            </div>

            <div>
              <div class="section-title">企业地址</div>
              <div class="info-line">
                {{ enterprise?.addressDetail || "-" }}
              </div>
            </div>

            <div>
              <div class="section-title">处理操作</div>
              <div class="handle-panel">
                <div v-if="canStart" class="status info">该投诉已派发，点击开始处理。</div>
                <div v-if="canHandle" class="handle-form">
                  <label>
                    处理结果
                    <textarea
                      v-model.trim="handleForm.handleResult"
                      rows="4"
                      placeholder="请输入处理结果"
                    ></textarea>
                  </label>
                </div>
                <div class="handle-actions">
                  <button
                    v-if="canStart"
                    class="primary"
                    type="button"
                    :disabled="loadingAction"
                    @click="handleStart"
                  >
                    开始处理
                  </button>
                  <button
                    v-if="canHandle"
                    class="primary"
                    type="button"
                    :disabled="loadingAction"
                    @click="handleSubmit"
                  >
                    提交处理
                  </button>
                </div>
                <div v-if="latestHandle" class="result-card">
                  <div class="result-meta">
                    <strong>{{ latestHandle.handlerName || "-" }}</strong>
                    <span>{{ formatTime(latestHandle.handleTime) }}</span>
                  </div>
                  <p>{{ latestHandle.handleResult || "-" }}</p>
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
import { fetchComplaintDetail, handleComplaint, startComplaintProcess } from "../api/regulation";

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
const handleForm = reactive({ handleResult: "" });
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

const canStart = computed(() => complaint.value?.status === "ASSIGNED");
const canHandle = computed(() => complaint.value?.status === "PROCESSING");

const complaintStatusMap = {
  SUBMITTED: "已提交",
  PENDING: "已受理",
  ASSIGNED: "已派发",
  PROCESSING: "处理中",
  FEEDBACKED: "已反馈"
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
    handleForm.handleResult = "";
  } catch (error) {
    detail.value = null;
    setStatus(error.message || "加载投诉详情失败", "error");
  } finally {
    loading.value = false;
  }
}

async function handleStart() {
  if (!complaint.value?.id) return;
  loadingAction.value = true;
  setStatus("");
  try {
    await startComplaintProcess(props.token, complaint.value.id);
    setStatus("投诉已开始处理", "success");
    await loadDetail();
  } catch (error) {
    setStatus(error.message || "开始处理失败", "error");
  } finally {
    loadingAction.value = false;
  }
}

async function handleSubmit() {
  if (!complaint.value?.id) return;
  if (!handleForm.handleResult.trim()) {
    setStatus("请填写处理结果", "error");
    return;
  }
  loadingAction.value = true;
  setStatus("");
  try {
    await handleComplaint(props.token, complaint.value.id, {
      handleResult: handleForm.handleResult
    });
    setStatus("投诉处理已完成", "success");
    await loadDetail();
  } catch (error) {
    setStatus(error.message || "提交处理失败", "error");
  } finally {
    loadingAction.value = false;
  }
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

.summary-grid span {
  display: block;
  font-size: 12px;
  color: var(--muted);
  margin-bottom: 6px;
}

.summary-grid strong {
  font-size: 14px;
}

.text-content {
  margin: 0;
  white-space: pre-line;
  line-height: 1.6;
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

.detail-stack {
  display: grid;
  gap: 18px;
  margin-top: 18px;
}

.info-line {
  padding: 12px 14px;
  border-radius: 12px;
  border: 1px solid var(--stroke);
  background: var(--card-strong);
}

.handle-panel {
  display: grid;
  gap: 12px;
}

.handle-form label {
  display: grid;
  gap: 6px;
}

.handle-actions {
  display: flex;
  gap: 10px;
  justify-content: flex-end;
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

@media (max-width: 900px) {
  .detail-shell .hero-panel {
    padding: 32px 40px 20px;
  }

  .detail-shell .form-panel {
    padding: 10px 40px 50px;
  }

  .summary-grid {
    grid-template-columns: 1fr;
  }

  .handle-actions {
    justify-content: stretch;
    flex-direction: column;
  }
}
</style>
