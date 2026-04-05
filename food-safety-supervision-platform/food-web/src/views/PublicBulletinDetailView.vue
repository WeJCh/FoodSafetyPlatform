<template>
  <div class="public-shell">
    <header class="public-topbar">
      <div class="brand">
        <span class="brand-mark">食安</span>
        <div>
          <strong>公告详情</strong>
          <span>查看监管公告具体内容</span>
        </div>
      </div>
      <div class="user-area">
        <button class="ghost" type="button" @click="$emit('back')">返回列表</button>
        <button class="ghost" type="button" @click="$emit('logout')">退出登录</button>
      </div>
    </header>

    <section class="detail-card">
      <div v-if="loading" class="status info">加载中...</div>
      <div v-else-if="!detail" class="status error">公告信息未找到</div>
      <template v-else>
        <div class="detail-head">
          <div>
            <div class="detail-badge">监管公告</div>
            <h1>{{ detail.title || "-" }}</h1>
            <div class="detail-meta">
              <span>发布时间：{{ formatTime(detail.publishedTime) }}</span>
              <span>发布人：{{ detail.publishedByName || "监管部门" }}</span>
            </div>
          </div>
        </div>

        <div class="detail-summary">
          <span>公告摘要</span>
          <strong>{{ detail.summary || "暂无摘要" }}</strong>
        </div>

        <article class="detail-content">{{ detail.content || "-" }}</article>
      </template>
    </section>
  </div>
</template>

<script setup>
import { onMounted, ref, watch } from "vue";
import { fetchPublicBulletinDetail } from "../api/regulation";

const props = defineProps({
  publicUser: {
    type: Object,
    required: true
  },
  publicToken: {
    type: String,
    required: true
  },
  bulletinId: {
    type: [String, Number],
    required: true
  }
});

defineEmits(["back", "logout"]);

const loading = ref(false);
const detail = ref(null);

function formatTime(value) {
  if (!value) return "-";
  return String(value).replace("T", " ").slice(0, 16);
}

async function loadDetail() {
  if (!props.bulletinId) {
    detail.value = null;
    return;
  }
  loading.value = true;
  try {
    detail.value = await fetchPublicBulletinDetail(props.publicToken, props.bulletinId);
  } catch {
    detail.value = null;
  } finally {
    loading.value = false;
  }
}

onMounted(loadDetail);
watch(() => props.bulletinId, loadDetail);
</script>

<style scoped>
.public-shell {
  min-height: 100vh;
  padding: 28px 48px 46px;
  background:
    radial-gradient(circle at 10% 10%, rgba(13, 94, 166, 0.16), transparent 45%),
    radial-gradient(circle at 85% 18%, rgba(15, 139, 141, 0.12), transparent 40%),
    var(--bg);
  display: grid;
  gap: 20px;
}

.public-topbar,
.detail-card {
  border-radius: 16px;
  border: 1px solid var(--stroke);
  background: #ffffff;
  box-shadow: var(--shadow);
}

.public-topbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 14px 18px;
}

.brand {
  display: flex;
  align-items: center;
  gap: 12px;
}

.brand-mark {
  width: 40px;
  height: 40px;
  border-radius: 12px;
  display: grid;
  place-items: center;
  background: var(--nav);
  color: #fff;
  font-weight: 700;
  letter-spacing: 0.08em;
}

.brand strong {
  display: block;
  font-size: 15px;
}

.brand span {
  font-size: 12px;
  color: var(--muted);
}

.user-area {
  display: flex;
  align-items: center;
  gap: 12px;
}

.detail-card {
  padding: 24px 28px;
}

.detail-head {
  padding-bottom: 18px;
  border-bottom: 1px solid var(--stroke);
}

.detail-badge {
  display: inline-flex;
  min-height: 28px;
  align-items: center;
  padding: 0 10px;
  border-radius: 999px;
  background: rgba(13, 94, 166, 0.12);
  color: #0d5ea6;
  font-size: 12px;
  font-weight: 600;
}

.detail-head h1 {
  margin: 10px 0 10px;
  font-size: 28px;
}

.detail-meta {
  display: flex;
  gap: 14px;
  flex-wrap: wrap;
  color: var(--muted);
  font-size: 12px;
}

.detail-summary {
  margin-top: 20px;
  display: grid;
  gap: 8px;
  padding: 16px 18px;
  border-radius: 14px;
  background: #f7fafc;
  border: 1px solid var(--stroke);
}

.detail-summary span {
  color: var(--muted);
  font-size: 12px;
}

.detail-summary strong {
  font-size: 14px;
}

.detail-content {
  margin-top: 20px;
  white-space: pre-wrap;
  line-height: 1.8;
  color: var(--ink);
}
</style>
