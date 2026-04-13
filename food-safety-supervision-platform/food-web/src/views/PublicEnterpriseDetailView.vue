<template>
  <div class="public-shell">
    <header class="public-topbar">
      <div class="brand">
        <span class="brand-mark">食安</span>
        <div>
          <strong>企业公示详情</strong>
          <span>查看企业备案信息与当前监管状态</span>
        </div>
      </div>
      <div class="user-area">
        <button class="ghost" type="button" @click="goBack">返回列表</button>
        <button class="ghost" type="button" @click="handleLogout">退出登录</button>
      </div>
    </header>

    <section class="detail-card">
      <div v-if="loading" class="status info">加载中...</div>
      <div v-else-if="!detail" class="status error">企业公示信息未找到</div>
      <template v-else>
        <div class="detail-head">
          <div>
            <div class="detail-badge">企业公示</div>
            <h1>{{ detail.enterpriseName || "-" }}</h1>
            <p>{{ detail.regionPathText || "-" }}</p>
          </div>
          <span :class="['status-chip', `status-chip--${statusClass(detail.status)}`]">
            {{ formatStatus(detail.status) }}
          </span>
        </div>

        <div class="detail-grid">
          <div>
            <span>食品经营许可证编号</span>
            <strong>{{ detail.licenseNo || "-" }}</strong>
          </div>
          <div>
            <span>统一社会信用代码</span>
            <strong>{{ detail.creditCode || "-" }}</strong>
          </div>
          <div>
            <span>法定代表人</span>
            <strong>{{ detail.legalRepresentative || "-" }}</strong>
          </div>
          <div>
            <span>负责人</span>
            <strong>{{ detail.principal || "-" }}</strong>
          </div>
          <div>
            <span>联系电话</span>
            <strong>{{ detail.principalPhoneMasked || "-" }}</strong>
          </div>
          <div>
            <span>包保责任人</span>
            <strong>{{ detail.regulatorName || "-" }}</strong>
          </div>
          <div class="detail-grid__full">
            <span>详细地址</span>
            <strong>{{ detail.addressDetail || "-" }}</strong>
          </div>
          <div>
            <span>审核通过时间</span>
            <strong>{{ formatTime(detail.approvedTime) }}</strong>
          </div>
          <div>
            <span>最近更新时间</span>
            <strong>{{ formatTime(detail.updateTime) }}</strong>
          </div>
        </div>

        <div class="attachment-panel">
          <div class="attachment-panel__title">备案附件</div>
          <div v-if="!detail.attachments || !detail.attachments.length" class="status info">
            当前暂无备案附件。
          </div>
          <div v-else class="attachment-list">
            <article
              v-for="(item, index) in detail.attachments"
              :key="`${item.type || 'attachment'}-${index}`"
              class="attachment-item"
            >
              <div class="attachment-item__head">
                <strong>{{ item.label || item.name || "备案附件" }}</strong>
                <span>{{ item.uploadedAt ? formatTime(item.uploadedAt) : "已上传" }}</span>
              </div>
              <p>{{ item.name || "未命名附件" }}</p>
              <a :href="item.url" target="_blank" rel="noreferrer">查看附件</a>
            </article>
          </div>
        </div>

        <div v-if="detail.status === 'KEY'" class="reason-panel">
          <div class="reason-panel__title">重点监管原因</div>
          <div v-if="!detail.keyReasons || !detail.keyReasons.length" class="status info">
            当前企业已纳入重点监管，详细原因记录待补充。
          </div>
          <div v-else class="reason-list">
            <article
              v-for="(reason, index) in detail.keyReasons"
              :key="`${reason.reasonType || 'reason'}-${index}`"
              class="reason-item"
            >
              <div class="reason-item__head">
                <strong>{{ reason.reasonLabel || formatReasonType(reason.reasonType) }}</strong>
                <span>{{ formatTime(reason.createTime) }}</span>
              </div>
              <p>{{ reason.reasonDetail || "已触发重点监管规则" }}</p>
            </article>
          </div>
        </div>
      </template>
    </section>
  </div>
</template>

<script setup>
import { onMounted, ref, watch } from "vue";
import { useRoute, useRouter } from "vue-router";
import { fetchPublicEnterpriseDetail } from "../api/regulation";
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

function formatStatus(value) {
  const map = {
    NORMAL: "正常监管",
    KEY: "重点监管"
  };
  return map[value] || value || "-";
}

function statusClass(value) {
  return value === "KEY" ? "warning" : "normal";
}

function formatReasonType(value) {
  const map = {
    COMPLAINT_OVERFLOW: "投诉过多",
    CONSECUTIVE_FAIL: "连续不合格",
    SAMPLING_FAIL: "抽检不合格",
    WARNING_TRIGGERED: "预警触发",
    MANUAL_SET: "人工设定"
  };
  return map[value] || value || "-";
}

async function loadDetail() {
  const enterpriseId = route.params.enterpriseId;
  if (!enterpriseId) {
    detail.value = null;
    return;
  }
  loading.value = true;
  try {
    detail.value = await fetchPublicEnterpriseDetail(publicToken, enterpriseId);
  } catch {
    detail.value = null;
  } finally {
    loading.value = false;
  }
}

function goBack() {
  router.push({ name: "public-enterprises" }).catch(() => {});
}

onMounted(loadDetail);
watch(() => route.params.enterpriseId, loadDetail);
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
  margin: 10px 0 6px;
  font-size: 28px;
}

.detail-head p {
  margin: 0;
  color: var(--muted);
}

.detail-grid {
  margin-top: 20px;
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 18px 24px;
}

.attachment-panel {
  margin-top: 24px;
  padding: 18px;
  border-radius: 14px;
  background: linear-gradient(135deg, rgba(15, 23, 42, 0.03), rgba(13, 94, 166, 0.06));
  border: 1px solid rgba(13, 94, 166, 0.12);
}

.attachment-panel__title {
  font-size: 14px;
  font-weight: 700;
  margin-bottom: 12px;
}

.attachment-list {
  display: grid;
  gap: 12px;
}

.attachment-item {
  padding: 14px 16px;
  border-radius: 12px;
  background: #fff;
  border: 1px solid var(--stroke);
}

.attachment-item__head {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: center;
}

.attachment-item__head span {
  font-size: 12px;
  color: var(--muted);
}

.attachment-item p {
  margin: 8px 0 10px;
  color: var(--on-surface-variant);
}

.attachment-item a {
  color: var(--primary);
  font-weight: 600;
  text-decoration: none;
}

.attachment-item a:hover {
  text-decoration: underline;
}

.detail-grid div {
  display: grid;
  gap: 6px;
}

.detail-grid span {
  font-size: 12px;
  color: var(--muted);
}

.detail-grid strong {
  font-size: 14px;
}

.detail-grid__full {
  grid-column: 1 / -1;
}

.reason-panel {
  margin-top: 24px;
  padding-top: 18px;
  border-top: 1px solid var(--stroke);
}

.reason-panel__title {
  font-size: 15px;
  font-weight: 700;
  margin-bottom: 12px;
}

.reason-list {
  display: grid;
  gap: 12px;
}

.reason-item {
  padding: 14px 16px;
  border-radius: 14px;
  border: 1px solid rgba(194, 118, 12, 0.18);
  background: linear-gradient(135deg, rgba(255, 246, 232, 0.95), rgba(255, 252, 247, 0.95));
}

.reason-item__head {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: center;
}

.reason-item__head span {
  font-size: 12px;
  color: var(--muted);
}

.reason-item p {
  margin: 8px 0 0;
  font-size: 13px;
  color: var(--text);
  line-height: 1.6;
}

.status-chip {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: 34px;
  padding: 0 12px;
  border-radius: 999px;
  font-size: 13px;
  font-weight: 600;
}

.status-chip--normal {
  background: rgba(33, 156, 84, 0.12);
  color: #1f6e45;
}

.status-chip--warning {
  background: rgba(210, 122, 0, 0.14);
  color: #9b5b00;
}

.status.info {
  background: var(--card-strong);
  color: var(--muted);
}

.status.error {
  background: rgba(190, 61, 61, 0.12);
  color: #9f2d2d;
}

@media (max-width: 900px) {
  .public-shell {
    padding: 20px;
  }

  .public-topbar,
  .detail-head {
    flex-direction: column;
    align-items: stretch;
  }

  .detail-grid {
    grid-template-columns: 1fr;
  }
}
</style>
