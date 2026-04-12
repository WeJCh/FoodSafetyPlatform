<template>
  <div class="public-shell">
    <header class="public-topbar">
      <div class="brand">
        <span class="brand-mark">食安</span>
        <div>
          <strong>抽检结果详情</strong>
          <span>查看已公示抽检结果的完整信息</span>
        </div>
      </div>
      <div class="user-area">
        <button class="ghost" type="button" @click="goBack">返回列表</button>
        <button class="ghost" type="button" @click="handleLogout">退出登录</button>
      </div>
    </header>

    <section class="detail-card">
      <div v-if="loading" class="status info">加载中...</div>
      <div v-else-if="!detail" class="status error">抽检结果未找到</div>
      <template v-else>
        <div class="detail-head">
          <div>
            <div class="detail-badge">抽检公示</div>
            <h1>{{ detail.productName || "-" }}</h1>
            <p>{{ detail.enterpriseName || "-" }}</p>
          </div>
          <span :class="['status-chip', `status-chip--${resultClass(detail.result)}`]">
            {{ formatResult(detail.result) }}
          </span>
        </div>

        <div class="detail-grid">
          <div>
            <span>任务编号</span>
            <strong>{{ detail.taskNo || "-" }}</strong>
          </div>
          <div>
            <span>产品类别</span>
            <strong>{{ detail.productCategory || "-" }}</strong>
          </div>
          <div>
            <span>产品规格</span>
            <strong>{{ detail.productSpecification || "-" }}</strong>
          </div>
          <div>
            <span>采样人员</span>
            <strong>{{ detail.sampledByName || "-" }}</strong>
          </div>
          <div>
            <span>采样时间</span>
            <strong>{{ formatTime(detail.sampledTime) }}</strong>
          </div>
          <div>
            <span>公示时间</span>
            <strong>{{ formatTime(detail.publishedTime || detail.updateTime) }}</strong>
          </div>
          <div class="detail-grid__full">
            <span>抽检结论</span>
            <strong>{{ detail.conclusion || "-" }}</strong>
          </div>
          <div class="detail-grid__full">
            <span>处置建议</span>
            <strong>{{ detail.disposalSuggestion || "-" }}</strong>
          </div>
        </div>
      </template>
    </section>
  </div>
</template>

<script setup>
import { onMounted, ref, watch } from "vue";
import { useRoute, useRouter } from "vue-router";
import { fetchPublicSamplingResultDetail } from "../api/regulationOperation";
import { getActiveSession, performLogout } from "../session/authRuntime";
import { formatTime } from "../utils/formatters";

const router = useRouter();
const route = useRoute();
const publicToken = getActiveSession()?.token || "";
const loading = ref(false);
const detail = ref(null);

async function handleLogout() {
  await performLogout();
  router.replace({ name: "login" }).catch(() => {});
}

function formatResult(value) {
  if (value === "FAIL") return "不合格";
  if (value === "PASS") return "合格";
  return "-";
}

function resultClass(value) {
  return value === "FAIL" ? "warning" : "normal";
}

async function loadDetail() {
  const samplingResultId = route.params.samplingResultId;
  if (!samplingResultId) {
    detail.value = null;
    return;
  }
  loading.value = true;
  try {
    detail.value = await fetchPublicSamplingResultDetail(publicToken, samplingResultId);
  } catch {
    detail.value = null;
  } finally {
    loading.value = false;
  }
}

function goBack() {
  router.push({ name: "public-sampling-results" }).catch(() => {});
}

onMounted(loadDetail);
watch(() => route.params.samplingResultId, loadDetail);
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
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16px;
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
  margin: 12px 0 6px;
  font-size: 30px;
}

.detail-head p {
  margin: 0;
  color: var(--muted);
}

.detail-grid {
  margin-top: 20px;
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 18px 22px;
}

.detail-grid span {
  display: block;
  margin-bottom: 6px;
  font-size: 12px;
  color: var(--muted);
}

.detail-grid strong {
  font-size: 14px;
  color: var(--text);
  line-height: 1.7;
}

.detail-grid__full {
  grid-column: 1 / -1;
}

.status-chip {
  display: inline-flex;
  min-height: 34px;
  align-items: center;
  justify-content: center;
  padding: 0 14px;
  border-radius: 999px;
  font-size: 13px;
  font-weight: 700;
  border: 1px solid transparent;
}

.status-chip--normal {
  color: #1f6b4d;
  background: #ebf9f1;
  border-color: #c6e9d6;
}

.status-chip--warning {
  color: #9b3a0a;
  background: #fff4eb;
  border-color: #f8d5bf;
}

@media (max-width: 900px) {
  .public-shell {
    padding: 22px 20px 36px;
  }

  .detail-grid {
    grid-template-columns: 1fr;
  }
}
</style>
