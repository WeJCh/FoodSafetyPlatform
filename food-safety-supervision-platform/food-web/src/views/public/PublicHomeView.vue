<template>
  <div class="public-home-page">
    <header class="public-home-page__topbar">
      <div class="public-home-page__topbar-inner">
        <div class="public-home-page__brand-nav">
          <span class="public-home-page__brand">食品安全监管平台</span>
          <nav class="public-home-page__nav" aria-label="公众导航">
            <button
              v-for="item in topNavItems"
              :key="item.key"
              type="button"
              class="public-home-page__nav-item"
              :class="{ 'is-active': item.key === 'home' }"
              @click="goTo(item.routeName)"
            >
              {{ item.label }}
            </button>
          </nav>
        </div>

        <div class="public-home-page__toolbar">
          <label class="public-home-page__search-box">
            <span class="material-symbols-outlined" aria-hidden="true">search</span>
            <input v-model.trim="globalKeyword" type="text" placeholder="搜索企业、产品、法规..." />
          </label>
          <button type="button" class="public-home-page__icon-btn" @click="onFeaturePending('通知中心')">
            <span class="material-symbols-outlined" aria-hidden="true">notifications</span>
          </button>
          <button type="button" class="public-home-page__icon-btn" @click="onFeaturePending('个人中心')">
            <span class="material-symbols-outlined" aria-hidden="true">account_circle</span>
          </button>
          <button type="button" class="ghost public-home-page__logout" @click="handleLogout">退出登录</button>
        </div>
      </div>
    </header>

    <main>
      <section class="public-home-page__hero">
        <div class="public-home-page__hero-bg" />
        <div class="public-home-page__hero-inner">
          <div class="public-home-page__hero-copy">
            <h1>食品安全公共服务门户</h1>
            <p>透明、权威、实时。我们致力于通过数据透明与社会共治，保障每一份餐桌上的安全。</p>
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
            <div class="public-home-page__hotwords">
              <span>热词搜索:</span>
              <button v-for="word in hotWords" :key="word" type="button" @click="useHotWord(word)">{{ word }}</button>
            </div>
          </div>
        </div>
      </section>

      <section class="public-home-page__entry-wrap">
        <div class="public-home-page__entry-grid">
          <button type="button" class="public-home-page__entry-card is-large" @click="goTo('public-bulletins')">
            <span class="material-symbols-outlined" aria-hidden="true">campaign</span>
            <h3>监管公告</h3>
            <p>获取最新国家、省、市级食品安全法规修订与行政管理动态。</p>
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
            <p>发现食品安全隐患？我们将竭诚为您排忧解难，保护合法权益。</p>
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

          <article v-for="item in latestNews" :key="item.id" class="public-home-page__news-item" @click="goTo('public-bulletins')">
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
            <h3>今日公示数据</h3>
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
      <div>
        <button type="button" @click="onFeaturePending('关于我们')">关于我们</button>
        <button type="button" @click="onFeaturePending('隐私政策')">隐私政策</button>
        <button type="button" @click="onFeaturePending('操作指南')">操作指南</button>
        <button type="button" @click="onFeaturePending('友情链接')">友情链接</button>
      </div>
      <p>© 2024 食品安全监管公共服务门户 版权所有</p>
    </footer>
  </div>
</template>

<script setup>
import { computed, ref } from "vue";
import { useRouter } from "vue-router";
import { getActiveSession, performLogout } from "../../session/authRuntime";

const router = useRouter();
const publicUser = computed(() => getActiveSession() || {});

const globalKeyword = ref("");
const enterpriseKeyword = ref("");

const topNavItems = [
  { key: "home", label: "首页", routeName: "public-home" },
  { key: "bulletins", label: "监管公告", routeName: "public-bulletins" },
  { key: "enterprises", label: "企业公示", routeName: "public-enterprises" },
  { key: "sampling", label: "抽检结果", routeName: "public-sampling-results" },
  { key: "complaint-create", label: "我要投诉", routeName: "public-complaint-create" },
  { key: "complaints", label: "我的投诉", routeName: "public-complaints" }
];

const hotWords = ["乳制品抽检", "餐饮诚信榜", "预制菜标准"];

const latestNews = [
  {
    id: "n1",
    day: "24",
    month: "Oct",
    tag: "通告",
    source: "来源：市场监督管理局",
    title: "关于开展2024年秋季校园周边食品安全专项整治工作的通知",
    category: "监督检查",
    description: "为加强秋季开学期间校园及周边食品安全监管，决定开展为期一个月的专项整治行动。"
  },
  {
    id: "n2",
    day: "22",
    month: "Oct",
    tag: "警告",
    source: "来源：国家抽检公示系统",
    title: "近期某批次不合格“食用调和油”召回公告及消费提示",
    category: "消费提示",
    description: "抽检发现部分品牌食用调和油过氧化值超标，生产厂家已启动召回流程。"
  },
  {
    id: "n3",
    day: "18",
    month: "Oct",
    tag: "法规",
    source: "来源：国务院食安委",
    title: "《餐饮服务通用卫生规范》地方标准解读说明会举行",
    category: "政策法规",
    description: "围绕厨房通风、排水、消毒等重点条款开展线上解读，提升标准落地执行效果。"
  }
];

const quickLinks = [
  { key: "bright-kitchen", icon: "restaurant", label: "明厨亮灶" },
  { key: "law-db", icon: "policy", label: "法律智库" },
  { key: "rank", icon: "stars", label: "红黑榜" },
  { key: "license", icon: "history_edu", label: "许可办理" }
];

const statsItems = [
  { label: "在营餐饮企业", value: "12,842" },
  { label: "本周抽检次数", value: "1,405" },
  { label: "合格率报告", value: "98.2%" },
  { label: "待办投诉处理", value: "14" }
];

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

function onFeaturePending(name) {
  // TODO: 接入公众端通知中心/个人中心/静态内容页能力
  window.alert(`${name} 功能待后续完善`);
}

function onPublicSearch() {
  // TODO: 接入公众门户统一查询接口（企业/追溯码/全文检索）
  window.alert("查询能力待后续完善，当前仅保留原型入口与交互形态");
}

function useHotWord(word) {
  globalKeyword.value = word;
  onPublicSearch();
}

function onQuickLink(item) {
  // TODO: 接入常用查询能力（明厨亮灶/法律智库/红黑榜/许可办理）
  window.alert(`${item.label} 功能待后续完善`);
}

async function handleLogout() {
  await performLogout();
  router.replace({ name: "login" }).catch(() => {});
}

function goTo(name) {
  if (name === "public-home") return;
  router.push({ name }).catch(() => {});
}
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
  min-height: 56px;
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
  font-size: 25px;
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
  min-height: 56px;
  padding: 0;
  color: var(--on-surface-variant);
  font-size: 12px;
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
  padding: 0 12px;
  min-height: 34px;
}

.public-home-page__search-box input {
  border: none;
  background: transparent;
  font-size: 12px;
  min-width: 210px;
}

.public-home-page__icon-btn {
  width: 34px;
  height: 34px;
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
  min-height: 34px;
  margin: 0;
  border-radius: 8px;
  border-color: rgba(195, 198, 211, 0.45);
  background: rgba(255, 255, 255, 0.74);
  color: var(--on-surface-variant);
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
  font-size: clamp(2rem, 4vw, 3rem);
  font-weight: 900;
}

.public-home-page__hero-copy p {
  margin: 14px 0 0;
  color: rgba(224, 234, 255, 0.95);
  line-height: 1.7;
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

.public-home-page__hotwords {
  margin-top: 14px;
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 10px;
}

.public-home-page__hotwords span {
  color: #fff;
  font-size: 12px;
  font-weight: 700;
}

.public-home-page__hotwords button {
  border: none;
  background: transparent;
  color: rgba(255, 255, 255, 0.86);
  text-decoration: underline;
  cursor: pointer;
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
  font-size: 38px;
  letter-spacing: -0.04em;
}

.public-home-page__entry-card p {
  margin: 0;
  color: var(--on-surface-variant);
  font-size: 13px;
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
  font-size: 18px;
}

.public-home-page__entry-card.is-mini strong {
  display: block;
  color: var(--primary);
  font-size: 30px;
  letter-spacing: -0.04em;
}

.public-home-page__entry-card.is-mini p {
  margin-top: 4px;
  font-size: 12px;
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
  font-size: 38px;
  letter-spacing: -0.04em;
}

.public-home-page__rights-card p {
  margin: 0 0 16px;
  font-size: 13px;
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
  font-size: 96px;
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
  cursor: pointer;
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
  font-size: 22px;
  font-weight: 800;
  color: var(--primary);
  line-height: 1;
}

.public-home-page__news-date small {
  font-size: 11px;
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
  font-size: 10px;
}

.public-home-page__news-content span {
  font-size: 12px;
  color: var(--on-surface-variant);
}

.public-home-page__news-content h4 {
  margin: 8px 0 6px;
  font-size: 16px;
}

.public-home-page__news-content p {
  margin: 0;
  font-size: 13px;
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
  font-size: 12px;
  font-weight: 700;
}

.public-home-page__stats-card {
  background: linear-gradient(160deg, var(--primary-container) 0%, var(--primary) 100%);
  color: #fff;
}

.public-home-page__stats-card h3 {
  margin: 0 0 10px;
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
  font-size: 13px;
}

.public-home-page__stats-card p strong {
  font-family: var(--font-display);
  font-size: 20px;
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
}

.public-home-page__credibility p {
  margin: 0;
  color: var(--on-surface-variant);
  font-size: 13px;
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
  cursor: pointer;
}

.public-home-page__footer p {
  margin: 0;
  font-size: 12px;
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
