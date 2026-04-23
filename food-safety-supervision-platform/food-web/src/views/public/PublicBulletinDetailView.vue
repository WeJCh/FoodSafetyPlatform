<template>
  <div class="public-bulletin-detail-page">
    <header class="public-bulletin-detail-page__topbar">
      <div class="public-bulletin-detail-page__topbar-inner">
        <div class="public-bulletin-detail-page__brand-nav">
          <span class="public-bulletin-detail-page__brand">食品安全监管平台</span>
          <nav class="public-bulletin-detail-page__nav" aria-label="公众导航">
            <button
              v-for="item in topNavItems"
              :key="item.key"
              type="button"
              class="public-bulletin-detail-page__nav-item"
              :class="{ 'is-active': item.key === 'bulletins' }"
              @click="goTo(item.routeName)"
            >
              {{ item.label }}
            </button>
          </nav>
        </div>
        <div class="public-bulletin-detail-page__toolbar">
          <label class="public-bulletin-detail-page__search-box">
            <span class="material-symbols-outlined" aria-hidden="true">search</span>
            <input v-model.trim="searchKeyword" type="text" placeholder="搜索公告标题或类别" @keyup.enter="goBackToList" />
          </label>
          <button type="button" class="public-bulletin-detail-page__icon-btn" @click="onFeaturePending('通知中心')">
            <span class="material-symbols-outlined" aria-hidden="true">notifications</span>
          </button>
          <button type="button" class="public-bulletin-detail-page__icon-btn" @click="onFeaturePending('个人中心')">
            <span class="material-symbols-outlined" aria-hidden="true">account_circle</span>
          </button>
          <button class="ghost public-bulletin-detail-page__logout" type="button" @click="handleLogout">退出登录</button>
        </div>
      </div>
    </header>

    <main class="public-bulletin-detail-page__main">
      <div v-if="loading" class="status info">加载中...</div>
      <div v-else-if="!detail" class="status error">公告信息未找到</div>
      <template v-else>
        <section class="public-bulletin-detail-page__hero">
          <button type="button" class="public-bulletin-detail-page__hero-back" @click="goBackToList">
            <span class="material-symbols-outlined" aria-hidden="true">arrow_back</span>
            返回公告列表
          </button>
          <h1>{{ detail.title || "-" }}</h1>
          <p>监管公告详情 · Regulatory Announcement Detail</p>
        </section>

        <section class="public-bulletin-detail-page__content">
          <article class="public-bulletin-detail-page__article-card">
            <div class="public-bulletin-detail-page__meta-grid">
              <div>
                <span>公告类别</span>
                <strong>{{ formatCategory(detail.category) }}</strong>
              </div>
              <div>
                <span>发布日期</span>
                <strong>{{ formatDateTime(detail.publishedTime) }}</strong>
              </div>
              <div>
                <span>发布机构</span>
                <strong>{{ detail.publishedByName || "监管部门" }}</strong>
              </div>
              <div>
                <span>公告编号</span>
                <strong>{{ bulletinCode }}</strong>
              </div>
            </div>

            <div class="public-bulletin-detail-page__article-body" v-html="sanitizedArticleHtml"></div>

            <div v-if="attachmentList.length" class="public-bulletin-detail-page__attachments">
              <h3>附件下载</h3>
              <button v-for="(item, index) in attachmentList" :key="`${item.name}-${index}`" type="button" @click="onFeaturePending('附件下载')">
                <span class="material-symbols-outlined" aria-hidden="true">description</span>
                <span>{{ item.name }}</span>
              </button>
            </div>
          </article>

          <aside class="public-bulletin-detail-page__side">
            <div class="public-bulletin-detail-page__side-card">
              <h3>公告信息</h3>
              <p><span>类别</span><strong>{{ formatCategory(detail.category) }}</strong></p>
              <p><span>时间</span><strong>{{ formatDate(detail.publishedTime) }}</strong></p>
              <p><span>发布人</span><strong>{{ detail.publishedByName || "监管部门" }}</strong></p>
            </div>

            <div class="public-bulletin-detail-page__side-card">
              <h3>操作</h3>
              <button type="button" @click="goBackToList">返回公告列表</button>
              <button type="button" @click="onFeaturePending('分享公告')">分享公告</button>
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
import { fetchPublicBulletinDetail } from "../../api/regulation";
import { getActiveSession, performLogout } from "../../session/authRuntime";
import { formatTime } from "../../utils/formatters";

const router = useRouter();
const route = useRoute();
const publicToken = getActiveSession()?.token || "";
const loading = ref(false);
const detail = ref(null);
const searchKeyword = ref("");

const topNavItems = [
  { key: "home", label: "首页", routeName: "public-home" },
  { key: "bulletins", label: "监管公告", routeName: "public-bulletins" },
  { key: "enterprises", label: "企业公示", routeName: "public-enterprises" },
  { key: "sampling", label: "抽检结果", routeName: "public-sampling-results" },
  { key: "complaint-create", label: "我要投诉", routeName: "public-complaint-create" },
  { key: "complaints", label: "我的投诉", routeName: "public-complaints" }
];

const categoryLabelMap = {
  POLICY: "政策法规",
  INSPECTION: "监督检查",
  NOTICE: "消费提示",
  OTHER: "其他公告"
};

const bulletinCode = computed(() => `GG-${String(detail.value?.id || "0000").padStart(4, "0")}`);
const sanitizedArticleHtml = computed(() => sanitizeBulletinHtml(detail.value?.content || ""));

const attachmentList = computed(() => {
  const raw = detail.value?.attachments;
  if (!Array.isArray(raw)) return [];
  return raw
    .map((item) => ({
      name: String(item?.name || item?.fileName || "").trim()
    }))
    .filter((item) => item.name);
});

function formatCategory(value) {
  return categoryLabelMap[String(value || "").toUpperCase()] || "未分类";
}

function formatDate(value) {
  return String(formatTime(value || "")).slice(0, 10) || "-";
}

function formatDateTime(value) {
  const text = String(formatTime(value || ""));
  return text && text !== "-" ? text : "-";
}

function sanitizeBulletinHtml(html) {
  const source = String(html || "").trim();
  if (!source) return "<p>-</p>";
  const parser = new DOMParser();
  const doc = parser.parseFromString(source, "text/html");
  const allowedTags = new Set(["P", "BR", "STRONG", "B", "EM", "I", "U", "H2", "H3", "UL", "OL", "LI", "A", "BLOCKQUOTE"]);
  const walkers = doc.body.querySelectorAll("*");
  walkers.forEach((el) => {
    const tag = el.tagName.toUpperCase();
    if (!allowedTags.has(tag)) {
      const text = doc.createTextNode(el.textContent || "");
      el.replaceWith(text);
      return;
    }
    [...el.attributes].forEach((attr) => {
      const name = attr.name.toLowerCase();
      if (tag === "A" && name === "href") return;
      el.removeAttribute(attr.name);
    });
    if (tag === "A") {
      const href = el.getAttribute("href") || "";
      if (!/^https?:\/\//i.test(href)) {
        el.replaceWith(doc.createTextNode(el.textContent || ""));
        return;
      }
      el.setAttribute("target", "_blank");
      el.setAttribute("rel", "noopener noreferrer");
    }
  });
  const result = doc.body.innerHTML.trim();
  return result || "<p>-</p>";
}

async function handleLogout() {
  await performLogout();
  router.replace({ name: "login" }).catch(() => {});
}

async function loadDetail() {
  const bulletinId = route.params.bulletinId;
  if (!bulletinId) {
    detail.value = null;
    return;
  }
  loading.value = true;
  try {
    detail.value = await fetchPublicBulletinDetail(publicToken, bulletinId);
  } catch {
    detail.value = null;
  } finally {
    loading.value = false;
  }
}

function goTo(name) {
  router.push({ name }).catch(() => {});
}

function goBackToList() {
  router.push({ name: "public-bulletins" }).catch(() => {});
}

function onFeaturePending(name) {
  window.alert(`${name} 功能待后续完善`);
}

onMounted(loadDetail);
watch(() => route.params.bulletinId, loadDetail);
</script>

<style scoped>
.public-bulletin-detail-page { min-height: 100vh; background: var(--surface); }
.public-bulletin-detail-page__topbar { position: sticky; top: 0; z-index: 40; border-bottom: 1px solid rgba(195,198,211,.4); background: rgba(248,250,253,.84); backdrop-filter: blur(12px); -webkit-backdrop-filter: blur(12px); }
.public-bulletin-detail-page__topbar-inner { max-width: 1680px; margin: 0 auto; min-height: var(--public-topbar-min-h); padding: 0 16px; display: flex; align-items: center; justify-content: space-between; gap: 24px; }
.public-bulletin-detail-page__brand-nav { display: flex; align-items: center; gap: 24px; }
.public-bulletin-detail-page__brand { font-family: var(--font-display); font-size: var(--public-brand-size); font-weight: 800; letter-spacing: -.03em; color: var(--primary); }
.public-bulletin-detail-page__nav { display: flex; align-items: center; gap: 18px; }
.public-bulletin-detail-page__nav-item { border: none; background: transparent; min-height: var(--public-topbar-min-h); padding: 0; color: var(--on-surface-variant); font-size: var(--public-nav-size); font-weight: 700; cursor: pointer; border-bottom: 2px solid transparent; }
.public-bulletin-detail-page__nav-item.is-active { color: var(--primary); border-bottom-color: var(--primary); }
.public-bulletin-detail-page__toolbar { display: flex; align-items: center; gap: 10px; }
.public-bulletin-detail-page__search-box { display: inline-flex; align-items: center; gap: 6px; border-radius: 8px; border: 1px solid rgba(195,198,211,.44); background: rgba(255,255,255,.75); padding: 0 14px; min-height: var(--public-toolbar-min-h); }
.public-bulletin-detail-page__search-box input { border: none; background: transparent; font-size: var(--public-toolbar-input-size); min-width: var(--public-toolbar-input-min-w); }
.public-bulletin-detail-page__icon-btn { width: var(--public-btn-compact-min-h); height: var(--public-btn-compact-min-h); border-radius: 8px; border: 1px solid transparent; background: transparent; color: var(--on-surface-variant); cursor: pointer; }
.public-bulletin-detail-page__logout { min-height: var(--public-toolbar-min-h); font-size: var(--public-logout-font-size); margin: 0; }
.public-bulletin-detail-page__main { max-width: 1680px; margin: 0 auto; padding: 24px 16px 48px; }
.public-bulletin-detail-page__hero { margin-bottom: 24px; border-radius: 12px; padding: 24px 28px; color: #fff; background: linear-gradient(135deg, #002660 0%, #003a8c 100%); }
.public-bulletin-detail-page__hero-back { border: none; background: rgba(255,255,255,.14); color: #fff; min-height: 32px; padding: 0 10px; border-radius: 8px; display: inline-flex; align-items: center; gap: 4px; cursor: pointer; }
.public-bulletin-detail-page__hero h1 { margin: 14px 0 10px; font-family: var(--font-display); font-size: clamp(28px,3.2vw,40px); line-height: 1.15; }
.public-bulletin-detail-page__hero p { margin: 0; letter-spacing: .08em; font-size: var(--public-hero-subtitle-en); font-weight: 700; opacity: .88; text-transform: uppercase; }
.public-bulletin-detail-page__content { display: grid; grid-template-columns: minmax(0,1fr) 280px; gap: 24px; }
.public-bulletin-detail-page__article-card, .public-bulletin-detail-page__side-card { border-radius: 12px; border: 1px solid rgba(195,198,211,.36); background: var(--surface-container-lowest); }
.public-bulletin-detail-page__article-card { padding: 18px 20px 22px; }
.public-bulletin-detail-page__meta-grid { display: grid; grid-template-columns: repeat(4,minmax(0,1fr)); gap: 10px; padding: 12px; border-radius: 10px; background: rgba(242,244,247,.75); }
.public-bulletin-detail-page__meta-grid span { display: block; font-size: var(--public-table-head-overline); font-weight: 700; letter-spacing: .08em; color: var(--on-surface-variant); text-transform: uppercase; }
.public-bulletin-detail-page__meta-grid strong { margin-top: 4px; display: block; font-size: var(--public-meta-strong); color: var(--primary); }
.public-bulletin-detail-page__article-body { margin-top: 18px; line-height: 1.8; color: var(--on-surface); font-size: var(--public-article-body); }
.public-bulletin-detail-page__article-body :deep(p) { margin: 0 0 12px; text-indent: 2em; }
.public-bulletin-detail-page__article-body :deep(h2),
.public-bulletin-detail-page__article-body :deep(h3) { margin: 16px 0 10px; color: var(--primary); line-height: 1.4; text-indent: 0; }
.public-bulletin-detail-page__article-body :deep(ul),
.public-bulletin-detail-page__article-body :deep(ol) { margin: 10px 0 10px 24px; padding: 0; }
.public-bulletin-detail-page__article-body :deep(li) { margin: 4px 0; }
.public-bulletin-detail-page__article-body :deep(a) { color: #1d4ed8; text-decoration: underline; word-break: break-all; }
.public-bulletin-detail-page__article-body :deep(blockquote) { margin: 12px 0; padding: 8px 12px; border-left: 3px solid rgba(0,38,96,.35); background: rgba(242,244,247,.7); }
.public-bulletin-detail-page__attachments { margin-top: 18px; padding-top: 14px; border-top: 1px solid rgba(195,198,211,.35); display: grid; gap: 8px; }
.public-bulletin-detail-page__attachments h3 { margin: 0; font-size: var(--public-caption); color: var(--primary); }
.public-bulletin-detail-page__attachments button { border: 1px solid rgba(195,198,211,.5); background: var(--surface-container-low); min-height: 36px; padding: 0 10px; display: inline-flex; align-items: center; gap: 6px; justify-content: flex-start; cursor: pointer; }
.public-bulletin-detail-page__side { display: grid; gap: 12px; align-content: start; }
.public-bulletin-detail-page__side-card { padding: 16px; }
.public-bulletin-detail-page__side-card h3 { margin: 0 0 10px; color: var(--primary); font-size: var(--public-table-head-overline); letter-spacing: .08em; font-weight: 800; text-transform: uppercase; }
.public-bulletin-detail-page__side-card p { margin: 0; padding: 8px 0; border-bottom: 1px dashed rgba(195,198,211,.45); display: flex; justify-content: space-between; gap: 10px; }
.public-bulletin-detail-page__side-card p:last-of-type { border-bottom: none; }
.public-bulletin-detail-page__side-card p span { color: var(--on-surface-variant); font-size: var(--public-caption); }
.public-bulletin-detail-page__side-card p strong { color: var(--primary); font-size: var(--public-caption); }
.public-bulletin-detail-page__side-card button { width: 100%; min-height: 36px; margin-bottom: 8px; border-radius: 8px; border: 1px solid rgba(195,198,211,.45); background: var(--surface-container-low); color: var(--on-surface-variant); cursor: pointer; }
.public-bulletin-detail-page__side-card button:last-child { margin-bottom: 0; }
@media (max-width: 1100px) { .public-bulletin-detail-page__nav { display: none; } .public-bulletin-detail-page__content { grid-template-columns: 1fr; } }
@media (max-width: 760px) { .public-bulletin-detail-page__topbar-inner, .public-bulletin-detail-page__main { padding-left: 12px; padding-right: 12px; } .public-bulletin-detail-page__toolbar { display: none; } .public-bulletin-detail-page__hero { padding: 20px 16px; } .public-bulletin-detail-page__meta-grid { grid-template-columns: 1fr 1fr; } }
</style>
