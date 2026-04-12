<template>
  <section class="enterprise-page" v-if="detail">
    <nav class="enterprise-breadcrumbs">
      <RouterLink :to="{ name: 'enterprise-rectifications' }">整改任务</RouterLink>
      <span class="material-symbols-outlined">chevron_right</span>
      <RouterLink :to="{ name: 'enterprise-rectification-detail', params: { id: detail.id } }">任务详情</RouterLink>
      <span class="material-symbols-outlined">chevron_right</span>
      <span>提交整改</span>
    </nav>

    <div class="enterprise-grid enterprise-grid--detail">
      <form class="enterprise-card enterprise-form-card" @submit.prevent="handleSubmit">
        <div class="section-head section-head--column">
          <h3>提交整改</h3>
          <p>请结合任务要求填写整改说明，并附上佐证材料。</p>
        </div>

        <ul class="detail-list detail-list--tight">
          <li><strong>任务编号</strong><span>{{ detail.taskNo }}</span></li>
          <li><strong>整改范围</strong><span>{{ detail.focusArea }}</span></li>
          <li><strong>整改时限</strong><span>{{ detail.dueDate }}</span></li>
        </ul>

        <label class="enterprise-field enterprise-field--full">
          <span>整改说明</span>
          <textarea v-model.trim="progress" rows="8" placeholder="请输入已完成的整改动作、现场调整和复核准备情况。"></textarea>
        </label>

        <label class="enterprise-upload-dropzone">
          <input type="file" multiple accept="image/*" @change="handleFileChange" />
          <span class="material-symbols-outlined">upload</span>
          <strong>上传整改凭证</strong>
          <p>支持多张图片，作为前端调联版本会保留预览并在可用时直传后端。</p>
        </label>

        <div v-if="files.length" class="upload-preview-grid">
          <article v-for="item in files" :key="item.id" class="upload-preview-card">
            <img :src="item.previewUrl" :alt="item.name" />
            <strong>{{ item.name }}</strong>
            <p>{{ item.state }}</p>
          </article>
        </div>

        <div class="form-actions">
          <button class="enterprise-primary-button" type="submit" :disabled="loading">{{ loading ? '提交中...' : '提交整改内容' }}</button>
          <RouterLink class="enterprise-ghost-button" :to="{ name: 'enterprise-rectification-detail', params: { id: detail.id } }">返回详情</RouterLink>
        </div>
        <p v-if="status" class="inline-status inline-status--success">{{ status }}</p>
      </form>

      <aside class="enterprise-card">
        <div class="section-head">
          <h3>待处理任务详情</h3>
          <span>{{ detail.riskLevel }}</span>
        </div>
        <p class="product-card__desc">{{ detail.rectificationDesc }}</p>
        <div class="timeline-list">
          <div v-for="action in actions" :key="action.id" class="timeline-item">
            <strong>{{ action.actionTime }}</strong>
            <p>{{ action.comment }}</p>
          </div>
        </div>
      </aside>
    </div>
  </section>
</template>

<script setup>
import { onBeforeUnmount, onMounted, ref } from "vue";
import { useRoute, useRouter } from "vue-router";
import {
  loadRectificationBundle,
  submitRectificationProgress,
  uploadRectificationEvidence
} from "../../services/enterpriseGateway";
import { getStoredSession } from "../../session/authSession";

const route = useRoute();
const router = useRouter();
const detail = ref(null);
const actions = ref([]);
const progress = ref("");
const files = ref([]);
const loading = ref(false);
const status = ref("");

onMounted(async () => {
  const session = getStoredSession();
  const bundle = await loadRectificationBundle(session?.token || "", route.params.id);
  detail.value = bundle.detail;
  actions.value = bundle.actions || [];
});

onBeforeUnmount(() => {
  files.value.forEach((item) => URL.revokeObjectURL(item.previewUrl));
});

function handleFileChange(event) {
  const selected = Array.from(event?.target?.files || []);
  selected.forEach((file) => {
    files.value.push({
      id: `${file.name}-${Date.now()}`,
      name: file.name,
      file,
      previewUrl: URL.createObjectURL(file),
      state: "等待上传",
      fileUrl: ""
    });
  });
  event.target.value = "";
}

async function handleSubmit() {
  if (!progress.value.trim()) {
    status.value = "请先填写整改说明。";
    return;
  }
  loading.value = true;
  status.value = "";
  try {
    const session = getStoredSession();
    for (const item of files.value) {
      if (!item.fileUrl) {
        item.state = "上传中...";
        const uploadResult = await uploadRectificationEvidence(session?.token || "", item.file);
        item.fileUrl = uploadResult.fileUrl;
        item.state = "已上传";
      }
    }
    await submitRectificationProgress(session?.token || "", route.params.id, {
      progress: progress.value,
      attachmentUrls: files.value.map((item) => item.fileUrl).filter(Boolean)
    });
    status.value = "整改说明已提交。";
    router.push({ name: 'enterprise-rectification-success', params: { id: route.params.id } });
  } catch (error) {
    status.value = error.message || "整改提交失败。";
  } finally {
    loading.value = false;
  }
}
</script>


