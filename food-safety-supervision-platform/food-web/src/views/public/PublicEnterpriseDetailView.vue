<template>
  <div class="public-enterprise-detail-page">
    <header class="public-enterprise-detail-page__topbar">
      <div class="public-enterprise-detail-page__topbar-inner">
        <div class="public-enterprise-detail-page__brand-nav">
          <span class="public-enterprise-detail-page__brand">食品安全监管平台</span>
          <nav class="public-enterprise-detail-page__nav" aria-label="公众导航">
            <button
              v-for="item in topNavItems"
              :key="item.key"
              type="button"
              class="public-enterprise-detail-page__nav-item"
              :class="{ 'is-active': item.key === 'enterprises' }"
              @click="goTo(item.routeName)"
            >
              {{ item.label }}
            </button>
          </nav>
        </div>
        <div class="public-enterprise-detail-page__toolbar">
          <label class="public-enterprise-detail-page__search-box">
            <span class="material-symbols-outlined" aria-hidden="true">search</span>
            <input type="text" placeholder="搜索企业..." @keyup.enter="goToListWithSearch" />
          </label>
          <button type="button" class="public-enterprise-detail-page__icon-btn" @click="onFeaturePending('通知中心')">
            <span class="material-symbols-outlined" aria-hidden="true">notifications</span>
          </button>
          <button type="button" class="public-enterprise-detail-page__icon-btn" @click="onFeaturePending('个人中心')">
            <span class="material-symbols-outlined" aria-hidden="true">account_circle</span>
          </button>
          <button type="button" class="ghost public-enterprise-detail-page__logout" @click="handleLogout">退出登录</button>
        </div>
      </div>
    </header>

    <main class="public-enterprise-detail-page__main">
      <div v-if="loading" class="status info">加载中...</div>
      <div v-else-if="!detail" class="status error">企业公示信息未找到</div>
      <template v-else>
        <section class="public-enterprise-detail-page__hero">
          <div class="public-enterprise-detail-page__hero-bar">
            <button type="button" class="public-enterprise-detail-page__hero-back" @click="goBack">
              <span class="material-symbols-outlined" aria-hidden="true">arrow_back</span>
              返回企业公示列表
            </button>
            <span class="public-enterprise-detail-page__hero-badge">企业公示</span>
          </div>
          <h1>{{ detail.enterpriseName || "-" }}</h1>
          <p class="public-enterprise-detail-page__hero-en">企业公示详情 · Enterprise Public Disclosure</p>
          <p class="public-enterprise-detail-page__hero-sub">统一社会信用代码：{{ detail.creditCode || "-" }}</p>
        </section>

        <section class="public-enterprise-detail-page__content">
          <article class="public-enterprise-detail-page__article-card">
            <div class="public-enterprise-detail-page__meta-grid">
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
            </div>

            <div class="public-enterprise-detail-page__block">
              <h3>经营地址</h3>
              <div class="public-enterprise-detail-page__address-panel">
                <span class="material-symbols-outlined" aria-hidden="true">location_on</span>
                <p>{{ fullAddress }}</p>
              </div>
            </div>

            <div class="public-enterprise-detail-page__attachments-wrap">
              <h3>附件信息</h3>
              <div v-if="!attachmentList.length" class="public-enterprise-detail-page__empty-tip">当前企业暂无备案附件。</div>
              <div v-else class="public-enterprise-detail-page__attachments">
                <article
                  v-for="(item, index) in attachmentList"
                  :key="`${item.type || 'attachment'}-${index}`"
                  class="public-enterprise-detail-page__attachment-item"
                >
                  <div class="public-enterprise-detail-page__attachment-head">
                    <strong>{{ item.label || item.name || "备案附件" }}</strong>
                  </div>
                  <p>{{ item.name || "未命名附件" }}</p>
                  <a
                    v-if="item.url"
                    class="ghost public-enterprise-detail-page__attachment-link"
                    :href="item.url"
                    target="_blank"
                    rel="noreferrer"
                  >
                    查看附件
                  </a>
                  <span v-else class="public-enterprise-detail-page__attachment-empty">附件地址待补充</span>
                </article>
              </div>
            </div>
          </article>

          <aside class="public-enterprise-detail-page__side">
            <div class="public-enterprise-detail-page__side-card">
              <h3>企业概要</h3>
              <p><span>企业名称</span><strong>{{ detail.enterpriseName || "-" }}</strong></p>
              <p><span>信用代码</span><strong>{{ detail.creditCode || "-" }}</strong></p>
              <p><span>公示编号</span><strong>{{ publicCode }}</strong></p>
            </div>

            <div class="public-enterprise-detail-page__side-card">
              <h3>操作</h3>
              <button type="button" @click="goBack">返回企业公示列表</button>
              <button type="button" @click="onFeaturePending('分享企业信息')">分享</button>
            </div>
          </aside>
        </section>
      </template>
    </main>
  </div>
</template>

<script setup>
import { computed, onMounted, ref, watch } from "vue";
import { useRoute, useRouter } from "vue-router";
import { fetchPublicEnterpriseDetail } from "../../api/regulation";
import { getActiveSession, performLogout } from "../../session/authRuntime";

const router = useRouter();
const route = useRoute();
const publicToken = getActiveSession()?.token || "";
const loading = ref(false);
const detail = ref(null);

const publicCode = computed(() => `EP-${String(detail.value?.id || "").padStart(6, "0")}`);

const attachmentList = computed(() => {
  const raw = detail.value?.attachments;
  if (!Array.isArray(raw)) return [];
  return raw.map((item) => ({
    type: item?.type || "",
    label: item?.label || "",
    name: item?.name || item?.fileName || "",
    url: item?.url || item?.fileUrl || item?.attachmentUrl || ""
  }));
});

const fullAddress = computed(() => {
  const regionText = detail.value?.regionPathText || detail.value?.regionName || "";
  const address = detail.value?.addressDetail || "";
  const result = [regionText, address].filter(Boolean).join(" ");
  return result || "-";
});

const topNavItems = [
  { key: "home", label: "首页", routeName: "public-home" },
  { key: "bulletins", label: "监管公告", routeName: "public-bulletins" },
  { key: "enterprises", label: "企业公示", routeName: "public-enterprises" },
  { key: "sampling", label: "抽检结果", routeName: "public-sampling-results" },
  { key: "complaint-create", label: "我要投诉", routeName: "public-complaint-create" },
  { key: "complaints", label: "我的投诉", routeName: "public-complaints" }
];

function goTo(name) {
  router.push({ name }).catch(() => {});
}

function goToListWithSearch() {
  router.push({ name: "public-enterprises" }).catch(() => {});
}

function onFeaturePending(name) {
  window.alert(`${name} 功能待后续完善`);
}

async function loadDetail() {
  const enterpriseId = route.params.enterpriseId;
  if (!enterpriseId) return;
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

async function handleLogout() {
  await performLogout();
  router.replace({ name: "login" }).catch(() => {});
}

onMounted(loadDetail);
watch(() => route.params.enterpriseId, loadDetail);
</script>

<style scoped>
.public-enterprise-detail-page {
  min-height: 100vh;
  background: var(--surface);
}

.public-enterprise-detail-page__topbar {
  position: sticky;
  top: 0;
  z-index: 40;
  border-bottom: 1px solid rgba(195, 198, 211, 0.4);
  background: rgba(248, 250, 253, 0.84);
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
}

.public-enterprise-detail-page__topbar-inner {
  max-width: 1680px;
  margin: 0 auto;
  min-height: var(--public-topbar-min-h);
  padding: 0 16px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 24px;
}

.public-enterprise-detail-page__brand-nav {
  display: flex;
  align-items: center;
  gap: 24px;
}

.public-enterprise-detail-page__brand {
  font-family: var(--font-display);
  font-size: var(--public-brand-size);
  font-weight: 800;
  letter-spacing: -0.03em;
  color: var(--primary);
}

.public-enterprise-detail-page__nav {
  display: flex;
  align-items: center;
  gap: 18px;
}

.public-enterprise-detail-page__nav-item {
  border: none;
  background: transparent;
  min-height: var(--public-topbar-min-h);
  padding: 0;
  color: var(--on-surface-variant);
  font-size: var(--public-nav-size);
  font-weight: 700;
  border-bottom: 2px solid transparent;
  cursor: pointer;
}

.public-enterprise-detail-page__nav-item.is-active {
  color: var(--primary);
  border-bottom-color: var(--primary);
}

.public-enterprise-detail-page__toolbar {
  display: flex;
  align-items: center;
  gap: 10px;
}

.public-enterprise-detail-page__search-box {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  border-radius: 8px;
  border: 1px solid rgba(195, 198, 211, 0.44);
  background: rgba(255, 255, 255, 0.75);
  padding: 0 14px;
  min-height: var(--public-toolbar-min-h);
}

.public-enterprise-detail-page__search-box input {
  border: none;
  background: transparent;
  font-size: var(--public-toolbar-input-size);
  min-width: var(--public-toolbar-input-min-w);
}

.public-enterprise-detail-page__icon-btn {
  width: var(--public-btn-compact-min-h);
  height: var(--public-btn-compact-min-h);
  border-radius: 8px;
  border: 1px solid transparent;
  background: transparent;
  color: var(--on-surface-variant);
  cursor: pointer;
}

.public-enterprise-detail-page__logout {
  min-height: var(--public-toolbar-min-h);
  font-size: var(--public-logout-font-size);
  margin: 0;
}

.public-enterprise-detail-page__main {
  max-width: 1680px;
  margin: 0 auto;
  padding: 24px 16px 48px;
}

.public-enterprise-detail-page__hero {
  margin-bottom: 24px;
  border-radius: 12px;
  padding: 24px 28px;
  color: #fff;
  background: linear-gradient(135deg, #002660 0%, #003a8c 100%);
}

.public-enterprise-detail-page__hero-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
}

.public-enterprise-detail-page__hero-back {
  border: none;
  background: rgba(255, 255, 255, 0.14);
  color: #fff;
  min-height: 32px;
  padding: 0 10px;
  border-radius: 8px;
  display: inline-flex;
  align-items: center;
  gap: 4px;
  cursor: pointer;
  font-size: var(--public-caption);
  font-weight: 600;
}

.public-enterprise-detail-page__hero-back .material-symbols-outlined {
  font-size: var(--public-body-secondary);
}

.public-enterprise-detail-page__hero-badge {
  display: inline-flex;
  align-items: center;
  min-height: var(--public-badge-min-h);
  padding: 0 14px;
  border-radius: 999px;
  font-size: var(--public-caption);
  font-weight: 800;
  background: rgba(255, 255, 255, 0.18);
  border: 1px solid rgba(255, 255, 255, 0.35);
  color: #e8f0ff;
}

.public-enterprise-detail-page__hero h1 {
  margin: 14px 0 8px;
  font-family: var(--font-display);
  font-size: clamp(28px, 3.2vw, 40px);
  line-height: 1.15;
}

.public-enterprise-detail-page__hero-en {
  margin: 0;
  letter-spacing: 0.08em;
  font-size: var(--public-hero-subtitle-en);
  font-weight: 700;
  opacity: 0.88;
  text-transform: uppercase;
}

.public-enterprise-detail-page__hero-sub {
  margin: 12px 0 0;
  font-size: var(--public-body);
  line-height: 1.55;
  color: rgba(224, 234, 255, 0.95);
  word-break: break-all;
}

.public-enterprise-detail-page__content {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 280px;
  gap: 24px;
  align-items: start;
}

.public-enterprise-detail-page__article-card,
.public-enterprise-detail-page__side-card {
  border-radius: 12px;
  border: 1px solid rgba(195, 198, 211, 0.36);
  background: var(--surface-container-lowest);
}

.public-enterprise-detail-page__article-card {
  padding: 18px 20px 22px;
}

.public-enterprise-detail-page__meta-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 10px;
  padding: 12px;
  border-radius: 10px;
  background: rgba(242, 244, 247, 0.75);
}

.public-enterprise-detail-page__meta-grid span {
  display: block;
  font-size: var(--public-table-head-overline);
  font-weight: 700;
  letter-spacing: 0.08em;
  color: var(--on-surface-variant);
  text-transform: uppercase;
}

.public-enterprise-detail-page__meta-grid strong {
  margin-top: 4px;
  display: block;
  font-size: var(--public-meta-strong);
  color: var(--primary);
  word-break: break-word;
}

.public-enterprise-detail-page__block {
  margin-top: 20px;
}

.public-enterprise-detail-page__block > h3 {
  margin: 0 0 12px;
  font-size: var(--public-caption);
  font-weight: 800;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  color: var(--primary);
}

.public-enterprise-detail-page__address-panel {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  padding: 14px 16px;
  border-radius: 10px;
  border: 1px solid rgba(195, 198, 211, 0.45);
  background: rgba(242, 244, 247, 0.85);
}

.public-enterprise-detail-page__address-panel .material-symbols-outlined {
  font-size: var(--public-body-em);
  color: var(--primary);
  margin-top: 2px;
  flex-shrink: 0;
}

.public-enterprise-detail-page__address-panel p {
  margin: 0;
  font-size: var(--public-article-body);
  line-height: 1.75;
  color: var(--on-surface);
}

.public-enterprise-detail-page__attachments-wrap {
  margin-top: 20px;
  padding-top: 18px;
  border-top: 1px solid rgba(195, 198, 211, 0.35);
}

.public-enterprise-detail-page__attachments-wrap > h3 {
  margin: 0 0 12px;
  font-size: var(--public-caption);
  font-weight: 800;
  color: var(--primary);
  letter-spacing: 0.06em;
  text-transform: uppercase;
}

.public-enterprise-detail-page__empty-tip {
  margin: 0;
  padding: 16px;
  text-align: center;
  color: var(--on-surface-variant);
  font-size: var(--public-caption);
  background: rgba(242, 244, 247, 0.55);
  border-radius: 10px;
  border: 1px dashed rgba(195, 198, 211, 0.55);
}

.public-enterprise-detail-page__attachments {
  display: grid;
  gap: 12px;
}

.public-enterprise-detail-page__attachment-item {
  border: 1px solid rgba(195, 198, 211, 0.42);
  border-radius: 10px;
  background: linear-gradient(180deg, rgba(248, 250, 253, 0.95), rgba(244, 246, 251, 0.9));
  padding: 12px 14px;
  display: grid;
  gap: 8px;
  transition: border-color 0.2s ease, box-shadow 0.2s ease, transform 0.2s ease;
}

.public-enterprise-detail-page__attachment-item:hover {
  border-color: rgba(70, 89, 231, 0.32);
  box-shadow: 0 8px 16px rgba(37, 56, 88, 0.08);
  transform: translateY(-1px);
}

.public-enterprise-detail-page__attachment-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.public-enterprise-detail-page__attachment-head strong {
  font-size: var(--public-body-secondary);
  color: var(--on-surface);
}

.public-enterprise-detail-page__attachment-item p {
  margin: 0;
  font-size: var(--public-caption);
  color: var(--on-surface-variant);
  word-break: break-all;
}

.public-enterprise-detail-page__attachment-link {
  width: fit-content;
  text-decoration: none;
  min-height: var(--public-chip-min-h);
  padding: 0 10px;
  border-radius: 8px;
  display: inline-flex;
  align-items: center;
  font-size: var(--public-caption);
  font-weight: 600;
}

.public-enterprise-detail-page__attachment-empty {
  font-size: var(--public-caption);
  color: var(--on-surface-variant);
}

.public-enterprise-detail-page__side {
  display: grid;
  gap: 12px;
  align-content: start;
}

.public-enterprise-detail-page__side-card {
  padding: 16px;
}

.public-enterprise-detail-page__side-card h3 {
  margin: 0 0 10px;
  color: var(--primary);
  font-size: var(--public-table-head-overline);
  letter-spacing: 0.08em;
  font-weight: 800;
  text-transform: uppercase;
}

.public-enterprise-detail-page__side-card p {
  margin: 0;
  padding: 8px 0;
  border-bottom: 1px dashed rgba(195, 198, 211, 0.45);
  display: flex;
  justify-content: space-between;
  gap: 10px;
}

.public-enterprise-detail-page__side-card p:last-of-type {
  border-bottom: none;
}

.public-enterprise-detail-page__side-card p span {
  color: var(--on-surface-variant);
  font-size: var(--public-caption);
  flex-shrink: 0;
}

.public-enterprise-detail-page__side-card p strong {
  color: var(--primary);
  font-size: var(--public-caption);
  text-align: right;
  font-weight: 700;
  word-break: break-word;
}

.public-enterprise-detail-page__side-card button {
  width: 100%;
  min-height: var(--public-btn-action-min-h);
  margin-bottom: 8px;
  border-radius: 8px;
  border: 1px solid rgba(195, 198, 211, 0.45);
  background: var(--surface-container-low);
  color: var(--on-surface-variant);
  font-size: var(--public-caption);
  font-weight: 600;
  cursor: pointer;
}

.public-enterprise-detail-page__side-card button:last-child {
  margin-bottom: 0;
}

@media (max-width: 1100px) {
  .public-enterprise-detail-page__nav {
    display: none;
  }

  .public-enterprise-detail-page__content {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 760px) {
  .public-enterprise-detail-page__topbar-inner,
  .public-enterprise-detail-page__main {
    padding-left: 12px;
    padding-right: 12px;
  }

  .public-enterprise-detail-page__toolbar {
    display: none;
  }

  .public-enterprise-detail-page__hero {
    padding: 20px 16px;
  }

  .public-enterprise-detail-page__meta-grid {
    grid-template-columns: 1fr 1fr;
  }
}
</style>
