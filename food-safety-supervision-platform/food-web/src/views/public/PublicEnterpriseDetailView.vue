<template>
  <div class="public-enterprise-detail-page">
    <header class="public-enterprise-detail-page__topbar">
      <div class="public-enterprise-detail-page__topbar-inner">
        <div class="public-enterprise-detail-page__brand-nav">
          <span class="public-enterprise-detail-page__brand">食品安全监管平台</span>
          <nav class="public-enterprise-detail-page__nav" aria-label="公众导航">
            <button v-for="item in topNavItems" :key="item.key" type="button" class="public-enterprise-detail-page__nav-item" :class="{ 'is-active': item.key === 'enterprises' }" @click="goTo(item.routeName)">
              {{ item.label }}
            </button>
          </nav>
        </div>
        <div class="public-enterprise-detail-page__toolbar">
          <label class="public-enterprise-detail-page__search-box">
            <span class="material-symbols-outlined" aria-hidden="true">search</span>
            <input type="text" placeholder="搜索企业..." />
          </label>
          <button type="button" class="public-enterprise-detail-page__icon-btn">
            <span class="material-symbols-outlined" aria-hidden="true">notifications</span>
          </button>
          <button type="button" class="public-enterprise-detail-page__icon-btn">
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
        <div class="public-enterprise-detail-page__breadcrumb">
          <button type="button" @click="goBack">
            <span class="material-symbols-outlined" aria-hidden="true">arrow_back</span>
            返回企业公示列表
          </button>
        </div>
        <section class="public-enterprise-detail-page__hero">
          <h1>{{ detail.enterpriseName || "-" }}</h1>
          <p>统一社会信用代码：{{ detail.creditCode || "-" }}</p>
        </section>
        <section class="public-enterprise-detail-page__card">
          <h3>基本工商信息</h3>
          <div class="public-enterprise-detail-page__grid">
            <div><span>法定代表人</span><strong>{{ detail.legalRepresentative || "-" }}</strong></div>
            <div><span>负责人</span><strong>{{ detail.principal || "-" }}</strong></div>
            <div><span>联系电话</span><strong>{{ detail.principalPhoneMasked || "-" }}</strong></div>
            <div><span>包保责任人</span><strong>{{ detail.regulatorName || "-" }}</strong></div>
            <div class="is-full"><span>详细地址</span><strong>{{ fullAddress }}</strong></div>
          </div>
        </section>
        <section class="public-enterprise-detail-page__card">
          <h3>附件信息</h3>
          <div v-if="!attachmentList.length" class="status info">当前企业暂无备案附件。</div>
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
function goTo(name) {
  router.push({ name }).catch(() => {});
}
onMounted(loadDetail);
watch(() => route.params.enterpriseId, loadDetail);
</script>

<style scoped>
.public-enterprise-detail-page { min-height: 100vh; background: var(--surface); }
.public-enterprise-detail-page__topbar { position: sticky; top: 0; z-index: 40; border-bottom: 1px solid rgba(195,198,211,.4); background: rgba(248,250,253,.84); }
.public-enterprise-detail-page__topbar-inner { max-width: 1680px; margin: 0 auto; min-height: 56px; padding: 0 16px; display: flex; align-items: center; justify-content: space-between; gap: 24px; }
.public-enterprise-detail-page__brand-nav { display: flex; align-items: center; gap: 24px; }
.public-enterprise-detail-page__brand { font-family: var(--font-display); font-size: 25px; font-weight: 800; color: var(--primary); }
.public-enterprise-detail-page__nav { display: flex; gap: 18px; }
.public-enterprise-detail-page__nav-item { border: none; background: transparent; min-height: 56px; color: var(--on-surface-variant); font-size: 12px; font-weight: 700; border-bottom: 2px solid transparent; cursor: pointer; }
.public-enterprise-detail-page__nav-item.is-active { color: var(--primary); border-bottom-color: var(--primary); }
.public-enterprise-detail-page__toolbar { display: flex; align-items: center; gap: 10px; }
.public-enterprise-detail-page__search-box { display: inline-flex; align-items: center; gap: 6px; border-radius: 8px; border: 1px solid rgba(195,198,211,.44); background: rgba(255,255,255,.75); padding: 0 12px; min-height: 34px; }
.public-enterprise-detail-page__search-box input { border: none; background: transparent; font-size: 12px; min-width: 180px; }
.public-enterprise-detail-page__icon-btn { width: 34px; height: 34px; border-radius: 8px; border: 1px solid transparent; background: transparent; color: var(--on-surface-variant); cursor: pointer; }
.public-enterprise-detail-page__logout { min-height: 34px; margin: 0; }
.public-enterprise-detail-page__main { max-width: 1680px; margin: 0 auto; padding: 24px 16px 48px; }
.public-enterprise-detail-page__breadcrumb { margin: 0 0 12px; }
.public-enterprise-detail-page__breadcrumb button { border: 1px solid rgba(195,198,211,.45); background: #fff; border-radius: 8px; min-height: 34px; padding: 0 10px; cursor: pointer; display: inline-flex; align-items: center; gap: 4px; color: var(--on-surface-variant); }
.public-enterprise-detail-page__hero { border-left: 4px solid var(--primary); background: var(--surface-container-lowest); padding: 18px 20px; margin-bottom: 16px; }
.public-enterprise-detail-page__hero h1 { margin: 0 0 8px; font-family: var(--font-display); font-size: 34px; color: var(--primary); }
.public-enterprise-detail-page__hero p { margin: 0; font-size: 12px; color: var(--on-surface-variant); }
.public-enterprise-detail-page__card { border: 1px solid rgba(195,198,211,.3); background: var(--surface-container-lowest); border-radius: 10px; padding: 16px; }
.public-enterprise-detail-page__card h3 { margin: 0 0 12px; font-size: 13px; color: var(--primary); letter-spacing: .08em; text-transform: uppercase; }
.public-enterprise-detail-page__grid { display: grid; grid-template-columns: 1fr 1fr; gap: 12px; }
.public-enterprise-detail-page__grid div { display: grid; gap: 4px; padding-bottom: 8px; border-bottom: 1px dashed rgba(195,198,211,.42); }
.public-enterprise-detail-page__grid .is-full { grid-column: 1 / -1; }
.public-enterprise-detail-page__grid span { font-size: 11px; color: var(--on-surface-variant); }
.public-enterprise-detail-page__grid strong { font-size: 13px; }
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
  font-size: 13px;
  color: var(--on-surface);
}
.public-enterprise-detail-page__attachment-item p {
  margin: 0;
  font-size: 12px;
  color: var(--on-surface-variant);
  word-break: break-all;
}
.public-enterprise-detail-page__attachment-link {
  width: fit-content;
  text-decoration: none;
  min-height: 30px;
  padding: 0 10px;
  border-radius: 8px;
  display: inline-flex;
  align-items: center;
}
.public-enterprise-detail-page__attachment-empty {
  font-size: 12px;
  color: var(--on-surface-variant);
}
@media (max-width: 1100px) { .public-enterprise-detail-page__nav { display: none; } }
@media (max-width: 760px) { .public-enterprise-detail-page__toolbar { display: none; } }
</style>
