<template>
  <EnterpriseWorkspacePage
    active-key="rectifications"
    title="提交整改"
    subtitle="填写整改说明并上传凭证，提交后进入监管复核。"
    top-search-placeholder="搜索任务或编号..."
    :username="enterpriseUser.username"
    :user-type="enterpriseUser.userType"
    @navigate="handleSidebarNavigate"
    @logout="handleLogout"
  >
    <div class="enterprise-workspace-card enterprise-rectification-submit-page">
      <nav class="enterprise-page-hero__crumb enterprise-rectification-submit-page__crumb" aria-label="面包屑">
        <span>整改任务</span>
        <span class="material-symbols-outlined" aria-hidden="true">chevron_right</span>
        <span class="is-current">提交整改</span>
      </nav>

      <div v-if="loadingDetail" class="status info">加载中...</div>
      <div v-else-if="!detail" class="status error">未找到该整改任务。</div>
      <form v-else class="enterprise-submit-layout enterprise-rectification-submit-page__layout" @submit.prevent="handleSubmit">
        <div class="enterprise-rectification-submit-page__main">
          <section class="enterprise-panel enterprise-panel--accent-top">
            <label class="enterprise-panel__head enterprise-rectification-submit-page__head-label">
              <div class="enterprise-panel__head-bar" />
              <h3>整改情况说明</h3>
            </label>
            <textarea
              v-model.trim="progress"
              rows="10"
              maxlength="2000"
              class="enterprise-rectification-submit-page__textarea"
              placeholder="请详细描述针对发现问题采取的整改措施、完成情况，以及后续防范安排..."
            />
            <div class="enterprise-rectification-submit-page__counter">
              <span>{{ progress.length }} / 2000 字符</span>
            </div>
          </section>

          <section class="enterprise-panel">
            <div class="enterprise-panel__head">
              <div class="enterprise-panel__head-bar" />
              <h3>整改凭证 / 图片</h3>
            </div>
            <div class="enterprise-rectification-submit-page__upload-grid">
              <label class="enterprise-rectification-submit-page__upload-trigger">
                <span class="material-symbols-outlined" aria-hidden="true">add_a_photo</span>
                <span>添加图片/文件</span>
                <input type="file" accept="image/jpeg,image/png,image/webp" multiple :disabled="loading || uploading" @change="handleFileChange" />
              </label>
              <div v-for="upload in uploadItems" :key="upload.id" class="enterprise-rectification-submit-page__upload-item">
                <img :src="upload.previewUrl" :alt="upload.name" />
                <div class="enterprise-rectification-submit-page__upload-mask">
                  <button v-if="upload.error" class="ghost" type="button" @click="retryUpload(upload.id)">重试</button>
                  <button class="ghost" type="button" @click="removeUpload(upload.id)">删除</button>
                </div>
                <div class="enterprise-rectification-submit-page__upload-meta" :class="{ error: upload.error }">
                  <span v-if="upload.uploading">上传中...</span>
                  <span v-else-if="upload.error">{{ upload.error }}</span>
                  <span v-else>已上传</span>
                </div>
              </div>
            </div>
            <div v-if="uploadItems.length" class="enterprise-rectification-submit-page__upload-list">
              <div v-for="upload in uploadItems" :key="upload.id" class="rectification-upload-item">
                <div class="rectification-upload-meta" :class="{ error: upload.error }">
                  <span>{{ upload.name }}</span>
                </div>
              </div>
            </div>
            <div class="enterprise-rectification-submit-page__upload-tip">
              <span class="material-symbols-outlined" aria-hidden="true">info</span>
              <p>支持 JPG / PNG / WebP，单文件大小不超过 5MB。请确保图片清晰，能体现整改前后对比或关键佐证信息。</p>
            </div>
          </section>
        </div>

        <aside class="enterprise-side-stack enterprise-rectification-submit-page__aside">
          <div class="enterprise-regulatory-gauge enterprise-rectification-submit-page__task-card">
            <span class="material-symbols-outlined enterprise-regulatory-gauge__bg" aria-hidden="true">task_alt</span>
            <h4>待处理任务详情</h4>
            <div class="enterprise-rectification-submit-page__task-meta">
              <div>
                <p>任务编号</p>
                <p>#{{ rectificationId }}</p>
              </div>
              <div>
                <p>检查事项</p>
                <p>{{ detail.rectificationDesc || "-" }}</p>
              </div>
              <div>
                <p>整改期限</p>
                <p>{{ detail.currentDeadline || "以监管通知为准" }}</p>
              </div>
            </div>
            <div class="enterprise-rectification-submit-page__task-status">
              <EnterpriseStatusChip :label="formatRectificationStatus(detail.status)" :tone="detail.status === 'REWORK' ? 'danger' : 'warning'" />
            </div>
          </div>

          <div class="enterprise-side-card">
            <div class="enterprise-side-card__head">提交流程</div>
            <div class="enterprise-side-card__body">
              <div class="enterprise-flow-steps">
                <div class="enterprise-flow-step">
                  <p class="enterprise-rectification-submit-page__flow-title">1. 提交申请</p>
                  <p class="enterprise-rectification-submit-page__flow-desc">企业填写说明并上传凭证</p>
                </div>
                <div class="enterprise-flow-step is-muted">
                  <p class="enterprise-rectification-submit-page__flow-title">2. 监管复核</p>
                  <p class="enterprise-rectification-submit-page__flow-desc">执法人员在约定时限内审核</p>
                </div>
                <div class="enterprise-flow-step is-muted">
                  <p class="enterprise-rectification-submit-page__flow-title">3. 任务结案</p>
                  <p class="enterprise-rectification-submit-page__flow-desc">通过后关闭隐患闭环</p>
                </div>
              </div>
            </div>
          </div>
        </aside>

        <div class="enterprise-rectification-submit-page__footer">
          <div class="enterprise-rectification-submit-page__notice">
            <span class="material-symbols-outlined" aria-hidden="true">lock_open</span>
            <p>提交后状态将变更为“已提交”，内容会进入待审核阶段，请等待监管人员复核。</p>
          </div>
          <div class="enterprise-rectification-submit-page__actions">
            <button type="button" class="ghost" @click="onBackToList">返回整改列表</button>
            <button type="button" class="ghost" @click="onBackToDetail">返回任务详情</button>
            <button class="primary" type="submit" :disabled="loading || uploading">
              <span class="material-symbols-outlined" aria-hidden="true">send</span>
              {{ loading ? "提交中..." : "提交审核" }}
            </button>
          </div>
        </div>
      </form>

      <div v-if="status.message" class="status" :class="status.type">{{ status.message }}</div>
    </div>
  </EnterpriseWorkspacePage>
</template>

<script setup>
import { computed, onBeforeUnmount, reactive, ref, watch } from "vue";
import { useRoute, useRouter } from "vue-router";
import { presignUpload } from "../../api/file";
import { fetchRectificationDetail, submitMyRectification } from "../../api/regulationOperation";
import { resolveErrorMessage } from "../../utils/uiFeedback";
import EnterpriseStatusChip from "../../components/enterprise/EnterpriseStatusChip.vue";
import EnterpriseWorkspacePage from "../../components/enterprise/EnterpriseWorkspacePage.vue";
import { formatRectificationStatus, useEnterpriseShellSession } from "./enterpriseShared";

const route = useRoute();
const router = useRouter();
const { enterpriseUser, token, handleSidebarNavigate, handleLogout } = useEnterpriseShellSession();
const rectificationId = computed(() => String(route.params.rectificationId || ""));
const detail = ref(null);
const loadingDetail = ref(false);
const loading = ref(false);
const status = reactive({ message: "", type: "" });
const progress = ref("");
const uploadItems = ref([]);

const uploading = computed(() => uploadItems.value.some((item) => item.uploading));
const allowedTypes = ["image/jpeg", "image/png", "image/webp"];

function createUploadItem(file) {
  return {
    id: `${Date.now()}-${Math.random().toString(16).slice(2)}`,
    name: file.name,
    file,
    previewUrl: URL.createObjectURL(file),
    fileUrl: "",
    uploading: true,
    error: ""
  };
}

function clearUploads() {
  uploadItems.value.forEach((item) => {
    if (item.previewUrl) URL.revokeObjectURL(item.previewUrl);
  });
  uploadItems.value = [];
}

function onBackToDetail() {
  router.push({ name: "enterprise-rectification-detail", params: { rectificationId: rectificationId.value } }).catch(() => {});
}

function onBackToList() {
  router.push({ name: "enterprise-rectifications" }).catch(() => {});
}

async function loadDetail() {
  if (!rectificationId.value) {
    detail.value = null;
    return;
  }

  loadingDetail.value = true;
  status.message = "";
  status.type = "";

  try {
    detail.value = await fetchRectificationDetail(token.value, rectificationId.value);
  } catch (error) {
    detail.value = null;
    status.message = resolveErrorMessage(error, "加载整改任务失败");
    status.type = "error";
  } finally {
    loadingDetail.value = false;
  }
}

async function uploadFile(item) {
  try {
    const presign = await presignUpload(token.value, {
      filename: item.name,
      contentType: item.file.type || "application/octet-stream",
      size: item.file.size,
      bizType: "RECTIFICATION"
    });
    const response = await fetch(presign.uploadUrl, {
      method: "PUT",
      headers: { "Content-Type": item.file.type || "application/octet-stream" },
      body: item.file
    });
    if (!response.ok) throw new Error(`上传失败 (${response.status})`);
    item.fileUrl = presign.fileUrl;
    item.uploading = false;
    item.error = "";
    uploadItems.value = [...uploadItems.value];
  } catch (error) {
    item.uploading = false;
    item.error = resolveErrorMessage(error, "上传失败");
    uploadItems.value = [...uploadItems.value];
  }
}

function handleFileChange(event) {
  const files = Array.from(event?.target?.files || []);
  if (!files.length) return;

  const remaining = 6 - uploadItems.value.length;
  files.slice(0, remaining).forEach((file) => {
    if (!allowedTypes.includes(file.type)) {
      status.message = "仅支持 JPG / PNG / WebP 图片";
      status.type = "error";
      return;
    }
    if (file.size > 5 * 1024 * 1024) {
      status.message = "单张图片不能超过 5MB";
      status.type = "error";
      return;
    }
    const item = createUploadItem(file);
    uploadItems.value = [...uploadItems.value, item];
    uploadFile(item);
  });

  event.target.value = "";
}

function removeUpload(uploadId) {
  const target = uploadItems.value.find((item) => item.id === uploadId);
  if (target?.previewUrl) URL.revokeObjectURL(target.previewUrl);
  uploadItems.value = uploadItems.value.filter((item) => item.id !== uploadId);
}

function retryUpload(uploadId) {
  const target = uploadItems.value.find((item) => item.id === uploadId);
  if (!target) return;
  target.error = "";
  target.uploading = true;
  uploadFile(target);
}

async function handleSubmit() {
  if (!progress.value.trim()) {
    status.message = "请先填写整改进展说明";
    status.type = "error";
    return;
  }
  if (uploadItems.value.some((item) => item.uploading)) {
    status.message = "整改凭证仍在上传中，请稍后提交。";
    status.type = "error";
    return;
  }
  if (uploadItems.value.some((item) => item.error)) {
    status.message = "存在上传失败的整改凭证，请处理后再提交。";
    status.type = "error";
    return;
  }

  loading.value = true;
  status.message = "";
  status.type = "";

  try {
    await submitMyRectification(token.value, rectificationId.value, {
      progress: progress.value.trim(),
      attachmentUrls: uploadItems.value.map((item) => item.fileUrl).filter(Boolean)
    });
    await router.replace({
      name: "enterprise-rectification-submit-success",
      params: { rectificationId: rectificationId.value },
      query: { progress: progress.value.trim() }
    });
  } catch (error) {
    status.message = resolveErrorMessage(error, "整改提交失败");
    status.type = "error";
  } finally {
    loading.value = false;
  }
}

watch(
  () => route.params.rectificationId,
  () => {
    clearUploads();
    progress.value = "";
    status.message = "";
    status.type = "";
    loadDetail();
  },
  { immediate: true }
);

onBeforeUnmount(() => {
  clearUploads();
});
</script>
