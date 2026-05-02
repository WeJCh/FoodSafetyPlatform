<template>
  <div class="public-sampling-result-detail-page">
    <header class="public-sampling-result-detail-page__topbar">
      <div class="public-sampling-result-detail-page__topbar-inner">
        <div class="public-sampling-result-detail-page__brand-nav">
          <span class="public-sampling-result-detail-page__brand">食品安全监管平台</span>
          <nav class="public-sampling-result-detail-page__nav" aria-label="公众导航">
            <button
              v-for="item in topNavItems"
              :key="item.key"
              type="button"
              class="public-sampling-result-detail-page__nav-item"
              :class="{ 'is-active': item.key === 'sampling' }"
              @click="goTo(item.routeName)"
            >
              {{ item.label }}
            </button>
          </nav>
        </div>
        <div class="public-sampling-result-detail-page__toolbar">
          <label class="public-sampling-result-detail-page__search-box">
            <span class="material-symbols-outlined" aria-hidden="true">search</span>
            <input v-model.trim="searchKeyword" type="text" placeholder="搜索抽检企业" @keyup.enter="goToListWithSearch" />
          </label>
          <button type="button" class="ghost public-sampling-result-detail-page__logout" @click="handleLogout">退出登录</button>
        </div>
      </div>
    </header>

    <main class="public-sampling-result-detail-page__main">
      <AppStatusToast v-if="loading" message="详情加载中..." type="info" />
      <AppStatusToast v-else-if="!detail" message="未找到对应的抽检结果。" type="error" />
      <template v-else>
        <section class="public-sampling-result-detail-page__hero">
          <div class="public-sampling-result-detail-page__hero-bar">
            <button type="button" class="public-sampling-result-detail-page__hero-back" @click="goBack">
              <span class="material-symbols-outlined" aria-hidden="true">arrow_back</span>
              返回抽检结果列表
            </button>
            <AppStatusTag :label="formatResult(detail.result)" :tone="detail.result === 'FAIL' ? 'danger' : 'success'" />
          </div>
          <h1>{{ detail.productName || "-" }}</h1>
          <p class="public-sampling-result-detail-page__hero-en">Sampling Inspection Result</p>
          <p class="public-sampling-result-detail-page__hero-sub">被抽样单位：{{ detail.enterpriseName || "-" }}</p>
        </section>

        <section class="public-sampling-result-detail-page__content">
          <article class="public-sampling-result-detail-page__article-card">
            <div class="public-sampling-result-detail-page__meta-grid">
              <div>
                <span>任务编号</span>
                <strong>{{ detail.taskNo || "-" }}</strong>
              </div>
              <div>
                <span>抽检结果</span>
                <strong>{{ formatResult(detail.result) }}</strong>
              </div>
              <div>
                <span>采样时间</span>
                <strong>{{ formatTime(detail.sampledTime) }}</strong>
              </div>
              <div>
                <span>公示时间</span>
                <strong>{{ formatTime(detail.publishedTime || detail.updateTime) }}</strong>
              </div>
            </div>

            <div class="public-sampling-result-detail-page__block">
              <h3>产品与任务信息</h3>
              <div class="public-sampling-result-detail-page__field-grid">
                <div>
                  <span>产品类别</span>
                  <strong>{{ detail.productCategory || "-" }}</strong>
                </div>
                <div>
                  <span>规格型号</span>
                  <strong>{{ detail.productSpecification || "-" }}</strong>
                </div>
                <div>
                  <span>采样人员</span>
                  <strong>{{ detail.sampledByName || "-" }}</strong>
                </div>
                <div>
                  <span>公示状态</span>
                  <strong>{{ formatPublicStatus(detail.publicStatus) }}</strong>
                </div>
              </div>
            </div>

            <div class="public-sampling-result-detail-page__panel" :class="detail.result === 'FAIL' ? 'is-risk' : 'is-pass'">
              <h3>
                <span class="material-symbols-outlined" aria-hidden="true">fact_check</span>
                抽检结论
              </h3>
              <p>{{ detail.conclusion || "暂无结论说明。" }}</p>
            </div>

            <div class="public-sampling-result-detail-page__panel is-neutral">
              <h3>
                <span class="material-symbols-outlined" aria-hidden="true">policy</span>
                处置建议
              </h3>
              <p>{{ detail.disposalSuggestion || "暂无处置建议。" }}</p>
            </div>
          </article>

          <aside class="public-sampling-result-detail-page__side">
            <div class="public-sampling-result-detail-page__side-card">
              <h3>结果摘要</h3>
              <p><span>企业名称</span><strong>{{ detail.enterpriseName || "-" }}</strong></p>
              <p><span>产品名称</span><strong>{{ detail.productName || "-" }}</strong></p>
              <p><span>任务编号</span><strong>{{ detail.taskNo || "-" }}</strong></p>
              <p><span>记录编号</span><strong>SR-{{ String(detail.id || "").padStart(6, "0") }}</strong></p>
            </div>

            <div class="public-sampling-result-detail-page__side-card">
              <h3>操作</h3>
              <button type="button" @click="goBack">返回抽检结果列表</button>
            </div>
          </aside>
        </section>
      </template>
    </main>
  </div>
</template>

<script setup>
import { onMounted, ref, watch } from "vue";
import { useRoute, useRouter } from "vue-router";
import AppStatusTag from "../../components/common/AppStatusTag.vue";
import AppStatusToast from "../../components/common/AppStatusToast.vue";
import { fetchPublicSamplingResultDetail } from "../../api/regulationOperation";
import { getActiveSession, performLogout } from "../../session/authRuntime";
import { formatStatusLabel, inspectionResultMap, samplingPublicStatusMap } from "../../utils/statusMaps";
import { formatTime } from "../../utils/formatters";

const router = useRouter();
const route = useRoute();
const publicToken = getActiveSession()?.token || "";
const loading = ref(false);
const detail = ref(null);
const searchKeyword = ref(String(route.query.enterpriseName || ""));

const topNavItems = [
  { key: "home", label: "首页", routeName: "public-home" },
  { key: "bulletins", label: "监管公告", routeName: "public-bulletins" },
  { key: "enterprises", label: "企业公示", routeName: "public-enterprises" },
  { key: "sampling", label: "抽检结果", routeName: "public-sampling-results" },
  { key: "complaint-create", label: "我要投诉", routeName: "public-complaint-create" },
  { key: "complaints", label: "我的投诉", routeName: "public-complaints" }
];

function formatPublicStatus(value) {
  return formatStatusLabel(value, samplingPublicStatusMap);
}

function formatResult(value) {
  return formatStatusLabel(value, inspectionResultMap);
}

function goTo(name) {
  router.push({ name }).catch(() => {});
}

function goToListWithSearch() {
  const enterpriseName = searchKeyword.value.trim();
  const productName = typeof route.query.productName === "string" ? route.query.productName.trim() : "";
  const result = typeof route.query.result === "string" ? route.query.result.trim().toUpperCase() : "";
  const nextQuery = {};
  if (enterpriseName) nextQuery.enterpriseName = enterpriseName;
  if (productName) nextQuery.productName = productName;
  if (result === "PASS" || result === "FAIL") nextQuery.result = result;
  router.push({
    name: "public-sampling-results",
    query: nextQuery
  }).catch(() => {});
}

async function handleLogout() {
  await performLogout();
  router.replace({ name: "login" }).catch(() => {});
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
  const enterpriseName = typeof route.query.enterpriseName === "string" ? route.query.enterpriseName.trim() : "";
  const productName = typeof route.query.productName === "string" ? route.query.productName.trim() : "";
  const result = typeof route.query.result === "string" ? route.query.result.trim().toUpperCase() : "";
  const nextQuery = {};
  if (enterpriseName) nextQuery.enterpriseName = enterpriseName;
  if (productName) nextQuery.productName = productName;
  if (result === "PASS" || result === "FAIL") nextQuery.result = result;
  router.push({
    name: "public-sampling-results",
    query: nextQuery
  }).catch(() => {});
}

onMounted(loadDetail);
watch(() => route.params.samplingResultId, loadDetail);
</script>

<style scoped>
.public-sampling-result-detail-page { min-height: 100vh; background: var(--surface); }
.public-sampling-result-detail-page__topbar { position: sticky; top: 0; z-index: 40; border-bottom: 1px solid rgba(195, 198, 211, 0.4); background: rgba(248, 250, 253, 0.84); backdrop-filter: blur(12px); -webkit-backdrop-filter: blur(12px); }
.public-sampling-result-detail-page__topbar-inner { max-width: 1680px; margin: 0 auto; min-height: var(--public-topbar-min-h); padding: 0 16px; display: flex; align-items: center; justify-content: space-between; gap: 24px; }
.public-sampling-result-detail-page__brand-nav { display: flex; align-items: center; gap: 24px; }
.public-sampling-result-detail-page__brand { font-family: var(--font-display); font-size: var(--public-brand-size); font-weight: 800; letter-spacing: -0.03em; color: var(--primary); }
.public-sampling-result-detail-page__nav { display: flex; align-items: center; gap: 18px; }
.public-sampling-result-detail-page__nav-item { border: none; background: transparent; min-height: var(--public-topbar-min-h); padding: 0; color: var(--on-surface-variant); font-size: var(--public-nav-size); font-weight: 700; border-bottom: 2px solid transparent; cursor: pointer; }
.public-sampling-result-detail-page__nav-item.is-active { color: var(--primary); border-bottom-color: var(--primary); }
.public-sampling-result-detail-page__toolbar { display: flex; align-items: center; gap: 10px; }
.public-sampling-result-detail-page__search-box { display: inline-flex; align-items: center; gap: 6px; border-radius: 8px; border: 1px solid rgba(195, 198, 211, 0.44); background: rgba(255, 255, 255, 0.75); padding: 0 14px; min-height: var(--public-toolbar-min-h); }
.public-sampling-result-detail-page__search-box input { border: none; background: transparent; font-size: var(--public-toolbar-input-size); min-width: var(--public-toolbar-input-min-w); }
.public-sampling-result-detail-page__logout { min-height: var(--public-toolbar-min-h); font-size: var(--public-logout-font-size); margin: 0; }
.public-sampling-result-detail-page__main { max-width: 1680px; margin: 0 auto; padding: 24px 16px 48px; }
.public-sampling-result-detail-page__hero { margin-bottom: 24px; border-radius: 12px; padding: 24px 28px; color: #fff; background: linear-gradient(135deg, #002660 0%, #003a8c 100%); }
.public-sampling-result-detail-page__hero-bar { display: flex; align-items: center; justify-content: space-between; gap: 12px; flex-wrap: wrap; }
.public-sampling-result-detail-page__hero-back { border: none; background: rgba(255, 255, 255, 0.14); color: #fff; min-height: 32px; padding: 0 10px; border-radius: 8px; display: inline-flex; align-items: center; gap: 4px; cursor: pointer; font-size: var(--public-caption); font-weight: 600; }
.public-sampling-result-detail-page__hero-back .material-symbols-outlined { font-size: var(--public-body-secondary); }
.public-sampling-result-detail-page__hero h1 { margin: 14px 0 8px; font-family: var(--font-display); font-size: clamp(28px, 3.2vw, 40px); line-height: 1.15; }
.public-sampling-result-detail-page__hero-en { margin: 0; letter-spacing: 0.08em; font-size: var(--public-hero-subtitle-en); font-weight: 700; opacity: 0.88; text-transform: uppercase; }
.public-sampling-result-detail-page__hero-sub { margin: 12px 0 0; font-size: var(--public-body); line-height: 1.55; color: rgba(224, 234, 255, 0.95); }
.public-sampling-result-detail-page__content { display: grid; grid-template-columns: minmax(0, 1fr) 280px; gap: 24px; align-items: start; }
.public-sampling-result-detail-page__article-card, .public-sampling-result-detail-page__side-card { border-radius: 12px; border: 1px solid rgba(195, 198, 211, 0.36); background: var(--surface-container-lowest); }
.public-sampling-result-detail-page__article-card { padding: 18px 20px 22px; }
.public-sampling-result-detail-page__meta-grid { display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); gap: 10px; padding: 12px; border-radius: 10px; background: rgba(242, 244, 247, 0.75); }
.public-sampling-result-detail-page__meta-grid span { display: block; font-size: var(--public-table-head-overline); font-weight: 700; letter-spacing: 0.08em; color: var(--on-surface-variant); text-transform: uppercase; }
.public-sampling-result-detail-page__meta-grid strong { margin-top: 4px; display: block; font-size: var(--public-meta-strong); color: var(--primary); }
.public-sampling-result-detail-page__block { margin-top: 20px; }
.public-sampling-result-detail-page__block h3 { margin: 0 0 12px; font-size: var(--public-caption); font-weight: 800; letter-spacing: 0.08em; text-transform: uppercase; color: var(--primary); }
.public-sampling-result-detail-page__field-grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 12px 16px; }
.public-sampling-result-detail-page__field-grid > div { padding: 10px 0; border-bottom: 1px dashed rgba(195, 198, 211, 0.42); }
.public-sampling-result-detail-page__field-grid span { display: block; font-size: var(--public-body-secondary); font-weight: 600; color: var(--on-surface-variant); margin-bottom: 4px; }
.public-sampling-result-detail-page__field-grid strong { font-size: var(--public-body); line-height: 1.55; font-weight: 600; }
.public-sampling-result-detail-page__panel { margin-top: 18px; padding: 14px 16px; border-radius: 10px; border: 1px solid rgba(195, 198, 211, 0.45); }
.public-sampling-result-detail-page__panel h3 { margin: 0 0 10px; display: flex; align-items: center; gap: 8px; font-size: var(--public-caption); font-weight: 800; color: var(--primary); }
.public-sampling-result-detail-page__panel h3 .material-symbols-outlined { font-size: var(--public-body-em); }
.public-sampling-result-detail-page__panel p { margin: 0; font-size: var(--public-article-body); line-height: 1.75; color: var(--on-surface); }
.public-sampling-result-detail-page__panel.is-pass { background: rgba(33, 156, 84, 0.06); border-color: rgba(33, 156, 84, 0.22); }
.public-sampling-result-detail-page__panel.is-risk { background: rgba(186, 26, 26, 0.06); border-color: rgba(186, 26, 26, 0.2); }
.public-sampling-result-detail-page__panel.is-neutral { background: rgba(242, 244, 247, 0.85); }
.public-sampling-result-detail-page__side { display: grid; gap: 12px; align-content: start; }
.public-sampling-result-detail-page__side-card { padding: 16px; }
.public-sampling-result-detail-page__side-card h3 { margin: 0 0 10px; color: var(--primary); font-size: var(--public-table-head-overline); letter-spacing: 0.08em; font-weight: 800; text-transform: uppercase; }
.public-sampling-result-detail-page__side-card p { margin: 0; padding: 8px 0; border-bottom: 1px dashed rgba(195, 198, 211, 0.45); display: flex; justify-content: space-between; gap: 10px; }
.public-sampling-result-detail-page__side-card p:last-of-type { border-bottom: none; }
.public-sampling-result-detail-page__side-card p span { color: var(--on-surface-variant); font-size: var(--public-caption); flex-shrink: 0; }
.public-sampling-result-detail-page__side-card p strong { color: var(--primary); font-size: var(--public-caption); text-align: right; font-weight: 700; word-break: break-word; }
.public-sampling-result-detail-page__side-card button { width: 100%; min-height: var(--public-btn-action-min-h); margin-bottom: 8px; border-radius: 8px; border: 1px solid rgba(195, 198, 211, 0.45); background: var(--surface-container-low); color: var(--on-surface-variant); font-size: var(--public-caption); font-weight: 600; cursor: pointer; }
.public-sampling-result-detail-page__side-card button:last-child { margin-bottom: 0; }
@media (max-width: 1100px) { .public-sampling-result-detail-page__nav { display: none; } .public-sampling-result-detail-page__content { grid-template-columns: 1fr; } }
@media (max-width: 760px) { .public-sampling-result-detail-page__topbar-inner, .public-sampling-result-detail-page__main { padding-left: 12px; padding-right: 12px; } .public-sampling-result-detail-page__toolbar { display: none; } .public-sampling-result-detail-page__hero { padding: 20px 16px; } .public-sampling-result-detail-page__meta-grid { grid-template-columns: 1fr 1fr; } .public-sampling-result-detail-page__field-grid { grid-template-columns: 1fr; } }
</style>
