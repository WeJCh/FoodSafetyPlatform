<template>
    <PublicWorkspacePage
    page-class="public-home-page"
    active-key="home"
    :show-search="true"
    v-model:search-value="globalKeyword"
    search-placeholder="搜索企业、产品或公告关键词"
    :search-min-width="210"
    @search="handleGlobalSearch"
  >
    <main>
      <section class="public-home-page__hero">
        <div class="public-home-page__hero-bg" />
        <div class="public-home-page__hero-inner">
          <div class="public-home-page__hero-copy">
            <h1>食品安全公共服务门户</h1>
            <p>透明、权威、实时。我们致力于通过数据公开与社会共治，保障每一份餐桌食品安全。</p>
            <div class="public-home-page__hero-search">
              <label>
                <span class="material-symbols-outlined" aria-hidden="true">corporate_fare</span>
                <input v-model.trim="enterpriseKeyword" type="text" placeholder="输入企业信用代码或名称" />
              </label>
              <button type="button" class="primary" @click="onPublicSearch">
                <span class="material-symbols-outlined" aria-hidden="true">search</span>
                立即查询
              </button>
            </div>
          </div>
        </div>
      </section>

      <section class="public-home-page__entry-wrap">
        <div class="public-home-page__entry-grid">
          <button type="button" class="public-home-page__entry-card is-large" @click="goTo('public-bulletins')">
            <span class="material-symbols-outlined" aria-hidden="true">campaign</span>
            <h3>监管公告</h3>
            <p>获取最新国家、省、市级食品安全法规修订与监管动态。</p>
            <i>查看详情</i>
          </button>

          <div class="public-home-page__entry-middle">
            <button type="button" class="public-home-page__entry-card is-mini" @click="goTo('public-enterprises')">
              <span class="material-symbols-outlined" aria-hidden="true">verified</span>
              <div>
                <strong>企业公示</strong>
                <p>诚信等级与处罚记录</p>
              </div>
            </button>

            <button type="button" class="public-home-page__entry-card is-mini" @click="goTo('public-sampling-results')">
              <span class="material-symbols-outlined" aria-hidden="true">biotech</span>
              <div>
                <strong>抽检结果</strong>
                <p>权威质检报告实时发布</p>
              </div>
            </button>
          </div>

          <div class="public-home-page__rights-card">
            <span class="material-symbols-outlined" aria-hidden="true">support_agent</span>
            <h3>维权服务中心</h3>
            <p>发现食品安全隐患？我们将及时协助处理，维护公众合法权益。</p>
            <button type="button" @click="goTo('public-complaint-create')">我要投诉</button>
            <button type="button" class="is-outline" @click="goTo('public-complaints')">我的投诉</button>
          </div>
        </div>
      </section>

      <section class="public-home-page__news">
        <div class="public-home-page__news-main">
          <div class="public-home-page__section-head">
            <div>
              <h2>最新监管动态</h2>
              <div />
            </div>
            <button type="button" @click="goTo('public-bulletins')">更多资讯 →</button>
          </div>

          <div v-if="newsLoading" class="public-home-page__news-empty">公告加载中...</div>
          <div v-else-if="!latestNews.length" class="public-home-page__news-empty">暂无已发布公告</div>
          <article v-for="item in latestNews" :key="item.id" class="public-home-page__news-item" @click="viewBulletin(item)">
            <div class="public-home-page__news-date">
              <span>{{ item.day }}</span>
              <small>{{ item.month }}</small>
            </div>
            <div class="public-home-page__news-content">
              <div>
                <b>{{ item.tag }}</b>
                <span>{{ item.source }}</span>
              </div>
              <h4>{{ item.title }}</h4>
              <p>{{ item.description }}</p>
            </div>
          </article>
        </div>

        <aside class="public-home-page__news-side">
          <div class="public-home-page__quick-card">
            <h3>
              <span class="material-symbols-outlined" aria-hidden="true">dynamic_feed</span>
              常用查询
            </h3>
            <div>
              <button v-for="item in quickLinks" :key="item.key" type="button" @click="onQuickLink(item)">
                <span class="material-symbols-outlined" aria-hidden="true">{{ item.icon }}</span>
                <span>{{ item.label }}</span>
              </button>
            </div>
          </div>

          <div class="public-home-page__stats-card">
            <h3>公开数据概览</h3>
            <div>
              <p v-for="item in statsItems" :key="item.label">
                <span>{{ item.label }}</span>
                <strong>{{ item.value }}</strong>
              </p>
            </div>
          </div>
        </aside>
      </section>

      <section class="public-home-page__credibility">
        <article v-for="item in credibilityItems" :key="item.title">
          <span class="material-symbols-outlined" aria-hidden="true">{{ item.icon }}</span>
          <h4>{{ item.title }}</h4>
          <p>{{ item.desc }}</p>
        </article>
      </section>
    </main>

    <footer class="public-home-page__footer">
      <p>© 2024 食品安全监管公共服务门户 版权所有</p>
    </footer>
    </PublicWorkspacePage>
</template>

<script setup>
import { computed, onMounted, ref } from "vue";
import { useRouter } from "vue-router";
import PublicWorkspacePage from "../../components/public/PublicWorkspacePage.vue";
import { fetchPublicBulletins, fetchPublicEnterprises } from "../../api/regulation";
import { fetchPublicSamplingResults } from "../../api/regulationOperation";
import { getActiveSession } from "../../session/authRuntime";
import { formatTime } from "../../utils/formatters";

const router = useRouter();
const publicUser = computed(() => getActiveSession() || {});
const publicToken = computed(() => getActiveSession()?.token || "");

const globalKeyword = ref("");
const enterpriseKeyword = ref("");
const newsLoading = ref(false);
const latestNews = ref([]);
const publicStats = ref({
  enterpriseTotal: null,
  bulletinTotal: null,
  samplingTotal: null
});

const quickLinks = [
  { key: "bulletins", icon: "campaign", label: "公告查询", routeName: "public-bulletins" },
  { key: "enterprises", icon: "apartment", label: "企业公示", routeName: "public-enterprises" },
  { key: "sampling", icon: "biotech", label: "抽检查询", routeName: "public-sampling-results" },
  { key: "complaints", icon: "track_changes", label: "投诉追踪", routeName: "public-complaints" }
];

const statsItems = computed(() => [
  { label: "已公示企业", value: formatOverviewStat(publicStats.value.enterpriseTotal) },
  { label: "已发布公告", value: formatOverviewStat(publicStats.value.bulletinTotal) },
  { label: "已公开抽检结果", value: formatOverviewStat(publicStats.value.samplingTotal) }
]);

const credibilityItems = [
  {
    icon: "shield_person",
    title: "全流程监管",
    desc: "从农田到餐桌，每一道工序都在数字化监管体系内，实现闭环管理。"
  },
  {
    icon: "database",
    title: "多方数据集成",
    desc: "融合工商、质检、气象等多维度数据，为食品安全预警提供科学依据。"
  },
  {
    icon: "groups",
    title: "全民共同参与",
    desc: "开放投诉举报通道，建立公众参与机制，打造食品安全社会共治共同体。"
  }
];

const bulletinCategoryMap = {
  POLICY: "政策法规",
  INSPECTION: "监督检查",
  NOTICE: "消费提示",
  OTHER: "其他公告"
};

function formatBulletinCategory(value) {
  return bulletinCategoryMap[String(value || "").toUpperCase()] || "公告";
}

function formatNewsDateParts(value) {
  const normalized = String(formatTime(value || "") || "");
  const dateText = normalized.length >= 10 ? normalized.slice(0, 10) : "";
  if (!dateText) {
    return { day: "--", month: "----.--" };
  }
  const [year, month, day] = dateText.split("-");
  return {
    day: day || "--",
    month: `${year || "----"}.${month || "--"}`
  };
}

function buildBulletinSource(item) {
  const publisher = item?.publishedByName || item?.createdByName;
  return publisher ? `发布单位：${publisher}` : "发布单位：监管部门";
}

function formatOverviewStat(value) {
  if (typeof value !== "number" || Number.isNaN(value)) {
    return "--";
  }
  return value.toLocaleString("zh-CN");
}

async function loadLatestNews() {
  if (!publicToken.value) {
    latestNews.value = [];
    return;
  }
  newsLoading.value = true;
  try {
    const data = await fetchPublicBulletins(publicToken.value, {
      page: 1,
      size: 3
    });
    latestNews.value = (data.records || []).slice(0, 3).map((item) => {
      const publishedTime = item?.publishedTime || item?.updateTime || item?.createTime || "";
      const dateParts = formatNewsDateParts(publishedTime);
      return {
        id: item.id,
        bulletinId: item.id,
        day: dateParts.day,
        month: dateParts.month,
        tag: formatBulletinCategory(item?.category),
        source: buildBulletinSource(item),
        title: item?.title || "-",
        description: item?.contentSummary || item?.summary || "点击查看公告详情"
      };
    });
  } catch (error) {
    latestNews.value = [];
  } finally {
    newsLoading.value = false;
  }
}

async function loadPublicOverviewStats() {
  if (!publicToken.value) {
    publicStats.value = {
      enterpriseTotal: null,
      bulletinTotal: null,
      samplingTotal: null
    };
    return;
  }
  try {
    const [enterpriseData, bulletinData, samplingData] = await Promise.all([
      fetchPublicEnterprises(publicToken.value, { page: 1, size: 1 }),
      fetchPublicBulletins(publicToken.value, { page: 1, size: 1 }),
      fetchPublicSamplingResults(publicToken.value, { page: 1, size: 1 })
    ]);
    publicStats.value = {
      enterpriseTotal: Number(enterpriseData?.total ?? 0),
      bulletinTotal: Number(bulletinData?.total ?? 0),
      samplingTotal: Number(samplingData?.total ?? 0)
    };
  } catch (error) {
    publicStats.value = {
      enterpriseTotal: null,
      bulletinTotal: null,
      samplingTotal: null
    };
  }
}

function onPublicSearch() {
  const keyword = enterpriseKeyword.value.trim();
  router.push({
    name: "public-enterprises",
    query: keyword ? { keyword } : {}
  }).catch(() => {});
}

function onQuickLink(item) {
  if (!item?.routeName) return;
  router.push({ name: item.routeName }).catch(() => {});
}

function viewBulletin(item) {
  const bulletinId = Number(item?.bulletinId || item?.id || 0);
  if (!bulletinId) return;
  router.push({
    name: "public-bulletin-detail",
    params: { bulletinId }
  }).catch(() => {});
}

function handleGlobalSearch() {
  const keyword = globalKeyword.value.trim();
  if (!keyword) {
    router.push({ name: "public-enterprises" }).catch(() => {});
    return;
  }
  const bulletinHints = ["公告", "通知", "通告", "法规", "政策", "标准", "办法", "条例", "规范", "指引"];
  const routeName = bulletinHints.some((item) => keyword.includes(item)) ? "public-bulletins" : "public-enterprises";
  router.push({
    name: routeName,
    query: { keyword }
  }).catch(() => {});
}

function goTo(name) {
  if (name === "public-home") return;
  router.push({ name }).catch(() => {});
}

onMounted(() => {
  loadLatestNews();
  loadPublicOverviewStats();
});
</script>

<style scoped>
.public-home-page {
  min-height: 100vh;
  background: var(--surface);
}

.public-home-page__topbar {
  position: sticky;
  top: 0;
  z-index: 40;
  border-bottom: 1px solid rgba(195, 198, 211, 0.4);
  background: rgba(248, 250, 253, 0.84);
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
}

.public-home-page__topbar-inner {
  max-width: 1680px;
  margin: 0 auto;
  min-height: var(--public-topbar-min-h);
  padding: 0 16px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 24px;
}

.public-home-page__brand-nav {
  display: flex;
  align-items: center;
  gap: 24px;
}

.public-home-page__brand {
  font-family: var(--font-display);
  font-size: var(--public-brand-size);
  font-weight: 800;
  letter-spacing: -0.03em;
  color: var(--primary);
}

.public-home-page__nav {
  display: flex;
  align-items: center;
  gap: 18px;
}

.public-home-page__nav-item {
  border: none;
  background: transparent;
  min-height: var(--public-topbar-min-h);
  padding: 0;
  color: var(--on-surface-variant);
  font-size: var(--public-nav-size);
  font-weight: 700;
  cursor: pointer;
  border-bottom: 2px solid transparent;
  transition: color 120ms ease, border-color 120ms ease;
}

.public-home-page__nav-item:hover {
  color: var(--primary);
}

.public-home-page__nav-item.is-active {
  color: var(--primary);
  border-bottom: 2px solid var(--primary);
}

.public-home-page__toolbar {
  display: flex;
  align-items: center;
  gap: 10px;
}

.public-home-page__search-box {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  border-radius: 8px;
  border: 1px solid rgba(195, 198, 211, 0.44);
  background: rgba(255, 255, 255, 0.75);
  padding: 0 14px;
  min-height: var(--public-toolbar-min-h);
}

.public-home-page__search-box input {
  border: none;
  background: transparent;
  font-size: var(--public-toolbar-input-size);
  min-width: 210px;
}

.public-home-page__icon-btn {
  width: var(--public-btn-compact-min-h);
  height: var(--public-btn-compact-min-h);
  border-radius: 8px;
  border: 1px solid transparent;
  background: transparent;
  cursor: pointer;
  color: var(--on-surface-variant);
}

.public-home-page__icon-btn:hover {
  background: rgba(255, 255, 255, 0.78);
  border-color: rgba(195, 198, 211, 0.48);
  color: var(--primary);
}

.public-home-page__logout {
  min-height: var(--public-toolbar-min-h);
  font-size: var(--public-logout-font-size);
  margin: 0;
  border-radius: 8px;
  border-color: rgba(195, 198, 211, 0.45);
  background: rgba(255, 255, 255, 0.74);
  color: var(--on-surface-variant);
}

.public-home-page__account {
  min-height: var(--public-toolbar-min-h);
  margin: 0;
  padding-inline: 12px;
}

.public-home-page__account .material-symbols-outlined {
  font-size: 22px;
}

.public-home-page__logout:hover {
  border-color: rgba(0, 38, 96, 0.22);
  background: #fff;
  color: var(--primary);
}

.public-home-page__hero {
  position: relative;
  min-height: 520px;
  overflow: hidden;
  background: var(--primary);
}

.public-home-page__hero-bg {
  position: absolute;
  inset: 0;
  background:
    linear-gradient(90deg, rgba(0, 38, 96, 0.92) 0%, rgba(0, 38, 96, 0.72) 42%, rgba(0, 38, 96, 0.08) 100%),
    radial-gradient(circle at 75% 38%, rgba(255, 255, 255, 0.2), transparent 45%),
    url("https://lh3.googleusercontent.com/aida-public/AB6AXuARQDr7ARy90-_xPaZBkXu4n1G86PeUWxDZAfZ81Udi-wOuPXZvR1I8M-zRjsiJu7OCH-H1mLPYodEMnU_-v79-F3PhdDc7q6ywSJqIybGpKwxdZdBIFp7lfDqo0ZqMJNxkwDJ-Y1nFvOqVyCa-tjPVX11omkaepd51Q31T1JvU57PTOiIjCGEFXkY9UHNJLNFiuLvGkWg2pJLNN3jWDP1OeSE0jXLbh2ZCUHf6UxRhW3jcnLU5euWhIDU-lrqspKZ_IguTN7bnwYo");
  background-size: auto, auto, cover;
  background-position: center, center, center;
}

.public-home-page__hero-inner {
  position: relative;
  z-index: 1;
  max-width: 1680px;
  margin: 0 auto;
  padding: 84px 16px 96px;
}

.public-home-page__hero-copy {
  max-width: 820px;
}

.public-home-page__hero-copy h1 {
  margin: 0;
  color: #fff;
  font-family: var(--font-display);
  font-size: clamp(var(--public-page-title-xs), 4vw, var(--public-page-title-sm));
  font-weight: 900;
}

.public-home-page__hero-copy p {
  margin: 14px 0 0;
  color: rgba(224, 234, 255, 0.95);
  line-height: 1.7;
  font-size: var(--public-lead);
}

.public-home-page__hero-search {
  margin-top: 28px;
  padding: 8px;
  border-radius: 12px;
  background: var(--surface-container-lowest);
  display: grid;
  grid-template-columns: 1fr auto;
  gap: 8px;
}

.public-home-page__hero-search label {
  min-height: 48px;
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 0 12px;
}

.public-home-page__hero-search label:first-child {
  border-right: 1px solid rgba(195, 198, 211, 0.5);
}

.public-home-page__hero-search input {
  border: none;
  background: transparent;
  width: 100%;
}

.public-home-page__hero-search .primary {
  margin: 0;
  min-height: 48px;
  padding: 0 20px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
}

.public-home-page__entry-wrap {
  max-width: 1680px;
  margin: -80px auto 0;
  padding: 0 16px 12px;
  position: relative;
  z-index: 2;
}

.public-home-page__entry-grid {
  display: grid;
  grid-template-columns: repeat(12, minmax(0, 1fr));
  gap: 14px;
}

.public-home-page__entry-card,
.public-home-page__rights-card {
  border: 1px solid rgba(195, 198, 211, 0.45);
  border-radius: 10px;
  background: var(--surface-container-lowest);
  box-shadow: 0 10px 22px -18px rgba(0, 38, 96, 0.35);
}

.public-home-page__entry-card {
  padding: 24px 22px;
  text-align: left;
  cursor: pointer;
}

.public-home-page__entry-card.is-large {
  grid-column: span 5;
  min-height: 238px;
}

.public-home-page__entry-middle {
  grid-column: span 3;
  display: grid;
  gap: 14px;
}

.public-home-page__entry-card.is-mini {
  display: flex;
  align-items: center;
  gap: 12px;
  min-height: 112px;
  padding: 0 18px;
}

.public-home-page__entry-card h3 {
  margin: 18px 0 8px;
  color: var(--primary);
  font-family: var(--font-display);
  font-size: var(--public-detail-title);
  letter-spacing: -0.04em;
}

.public-home-page__entry-card p {
  margin: 0;
  color: var(--on-surface-variant);
  font-size: var(--public-body-secondary);
  line-height: 1.6;
}

.public-home-page__entry-card i {
  margin-top: 36px;
  display: inline-block;
  color: var(--primary);
  font-style: normal;
  font-weight: 700;
}

.public-home-page__entry-card.is-large > .material-symbols-outlined {
  width: 44px;
  height: 44px;
  border-radius: 8px;
  display: grid;
  place-items: center;
  background: var(--surface-container-low);
  color: rgba(0, 38, 96, 0.72);
}

.public-home-page__entry-card.is-mini > .material-symbols-outlined {
  width: 34px;
  height: 34px;
  border-radius: 8px;
  display: grid;
  place-items: center;
  background: var(--surface-container-low);
  color: rgba(0, 38, 96, 0.78);
  font-size: var(--public-body-em);
}

.public-home-page__entry-card.is-mini strong {
  display: block;
  color: var(--primary);
  font-size: var(--public-stat-number);
  letter-spacing: -0.04em;
}

.public-home-page__entry-card.is-mini p {
  margin-top: 4px;
  font-size: var(--public-caption);
}

.public-home-page__rights-card {
  grid-column: span 4;
  background: linear-gradient(160deg, var(--primary-container) 0%, var(--primary) 100%);
  padding: 22px 20px;
  color: #fff;
  min-height: 238px;
  position: relative;
  overflow: hidden;
}

.public-home-page__rights-card h3 {
  margin: 14px 0 10px;
  font-family: var(--font-display);
  font-size: var(--public-detail-title);
  letter-spacing: -0.04em;
}

.public-home-page__rights-card p {
  margin: 0 0 16px;
  font-size: var(--public-body-secondary);
  line-height: 1.55;
  opacity: 0.88;
}

.public-home-page__rights-card button {
  width: 100%;
  min-height: 46px;
  border-radius: 8px;
  border: 1px solid rgba(255, 255, 255, 0.35);
  background: #fff;
  color: var(--primary);
  font-weight: 700;
  cursor: pointer;
}

.public-home-page__rights-card button.is-outline {
  margin-top: 8px;
  color: #fff;
  background: transparent;
}

.public-home-page__rights-card > .material-symbols-outlined {
  position: absolute;
  right: -8px;
  bottom: -8px;
  font-size: var(--public-decorative-glyph);
  opacity: 0.14;
}

.public-home-page__news {
  max-width: 1680px;
  margin: 42px auto 0;
  padding: 0 16px 48px;
  display: grid;
  grid-template-columns: minmax(0, 2fr) minmax(320px, 1fr);
  gap: 24px;
}

.public-home-page__section-head {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  margin-bottom: 14px;
}

.public-home-page__section-head h2 {
  margin: 0;
  color: var(--primary);
  font-family: var(--font-display);
  font-size: var(--public-page-title-sm);
  line-height: 1.15;
}

.public-home-page__section-head div div {
  margin-top: 6px;
  width: 48px;
  height: 4px;
  background: var(--primary);
}

.public-home-page__section-head button {
  border: none;
  background: transparent;
  color: var(--primary);
  font-weight: 700;
  font-size: var(--public-caption);
  cursor: pointer;
}

.public-home-page__news-empty {
  min-height: 120px;
  border: 1px dashed rgba(195, 198, 211, 0.45);
  border-radius: 16px;
  display: grid;
  place-items: center;
  color: var(--on-surface-variant);
  background: rgba(255, 255, 255, 0.72);
}

.public-home-page__news-item {
  border-radius: 12px;
  border: 1px solid rgba(195, 198, 211, 0.28);
  background: var(--surface-container-lowest);
  padding: 16px;
  display: flex;
  gap: 14px;
  cursor: pointer;
  margin-bottom: 10px;
}

.public-home-page__news-date {
  width: 56px;
  flex-shrink: 0;
  text-align: center;
  border-radius: 8px;
  background: var(--surface-container-low);
  padding: 8px 0;
}

.public-home-page__news-date span {
  display: block;
  font-family: var(--font-display);
  font-size: var(--public-news-date-num);
  font-weight: 800;
  color: var(--primary);
  line-height: 1;
}

.public-home-page__news-date small {
  font-size: var(--public-overline);
  color: var(--on-surface-variant);
}

.public-home-page__news-content > div {
  display: flex;
  align-items: center;
  gap: 8px;
}

.public-home-page__news-content b {
  background: rgba(0, 38, 96, 0.1);
  color: var(--primary);
  padding: 2px 8px;
  border-radius: 999px;
  font-size: var(--public-table-head-overline);
}

.public-home-page__news-content span {
  font-size: var(--public-caption);
  color: var(--on-surface-variant);
}

.public-home-page__news-content h4 {
  margin: 8px 0 6px;
  font-size: var(--public-subhead);
}

.public-home-page__news-content p {
  margin: 0;
  font-size: var(--public-body-secondary);
  color: var(--on-surface-variant);
  line-height: 1.55;
}

.public-home-page__news-side {
  display: grid;
  gap: 14px;
  align-content: start;
}

.public-home-page__quick-card,
.public-home-page__stats-card {
  border-radius: 12px;
  padding: 18px;
}

.public-home-page__quick-card {
  background: var(--surface-container-low);
}

.public-home-page__quick-card h3 {
  margin: 0 0 12px;
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: var(--public-text-md);
  font-weight: 800;
  color: var(--primary);
}

.public-home-page__quick-card > div {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 8px;
}

.public-home-page__quick-card button {
  border: 1px solid rgba(195, 198, 211, 0.35);
  border-radius: 8px;
  background: var(--surface-container-lowest);
  min-height: 76px;
  display: grid;
  place-items: center;
  gap: 4px;
  cursor: pointer;
  font-size: var(--public-caption);
  font-weight: 700;
}

.public-home-page__stats-card {
  background: linear-gradient(160deg, var(--primary-container) 0%, var(--primary) 100%);
  color: #fff;
}

.public-home-page__stats-card h3 {
  margin: 0 0 10px;
  font-size: var(--public-text-md);
  font-weight: 800;
}

.public-home-page__stats-card p {
  margin: 0;
  padding: 10px 0;
  border-bottom: 1px solid rgba(255, 255, 255, 0.14);
  display: flex;
  justify-content: space-between;
}

.public-home-page__stats-card p:last-child {
  border-bottom: none;
}

.public-home-page__stats-card p span {
  opacity: 0.82;
  font-size: var(--public-body-secondary);
}

.public-home-page__stats-card p strong {
  font-family: var(--font-display);
  font-size: var(--public-stat-highlight);
}

.public-home-page__credibility {
  background: var(--surface-container-low);
  padding: 46px 16px;
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 14px;
}

.public-home-page__credibility article {
  text-align: center;
  padding: 0 14px;
}

.public-home-page__credibility h4 {
  margin: 10px 0 8px;
  font-size: var(--public-subhead);
  color: var(--primary);
  font-weight: 800;
}

.public-home-page__credibility p {
  margin: 0;
  color: var(--on-surface-variant);
  font-size: var(--public-body-secondary);
  line-height: 1.65;
}

.public-home-page__footer {
  padding: 28px 16px;
  background: #f2f4f7;
  text-align: center;
}

.public-home-page__footer div {
  display: flex;
  gap: 14px;
  flex-wrap: wrap;
  justify-content: center;
  margin-bottom: 10px;
}

.public-home-page__footer button {
  border: none;
  background: transparent;
  color: var(--outline);
  font-size: var(--public-caption);
  cursor: pointer;
}

.public-home-page__footer p {
  margin: 0;
  font-size: var(--public-caption);
  color: var(--outline);
}

@media (max-width: 1180px) {
  .public-home-page__nav {
    display: none;
  }

  .public-home-page__hero-search {
    grid-template-columns: 1fr;
  }

  .public-home-page__hero-search label:first-child {
    border-right: none;
    border-bottom: 1px solid rgba(195, 198, 211, 0.45);
  }

  .public-home-page__entry-card.is-large,
  .public-home-page__entry-middle,
  .public-home-page__rights-card {
    grid-column: span 12;
  }

  .public-home-page__news {
    grid-template-columns: 1fr;
  }

  .public-home-page__credibility {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 760px) {
  .public-home-page__topbar-inner,
  .public-home-page__hero-inner,
  .public-home-page__entry-wrap,
  .public-home-page__news {
    padding-left: 16px;
    padding-right: 16px;
  }

  .public-home-page__toolbar {
    display: none;
  }
}
</style>









