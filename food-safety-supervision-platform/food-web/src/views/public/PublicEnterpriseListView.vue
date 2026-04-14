<template>
  <div class="public-enterprises-page">
    <header class="public-enterprises-page__topbar">
      <div class="public-enterprises-page__topbar-inner">
        <div class="public-enterprises-page__brand-nav">
          <span class="public-enterprises-page__brand">食品安全监管平台</span>
          <nav class="public-enterprises-page__nav" aria-label="公众导航">
            <button v-for="item in topNavItems" :key="item.key" type="button" class="public-enterprises-page__nav-item" :class="{ 'is-active': item.key === 'enterprises' }" @click="goTo(item.routeName)">
              {{ item.label }}
            </button>
          </nav>
        </div>
        <div class="public-enterprises-page__toolbar">
          <label class="public-enterprises-page__search-box">
            <span class="material-symbols-outlined" aria-hidden="true">search</span>
            <input v-model.trim="filters.enterpriseName" type="text" placeholder="搜索企业..." @keyup.enter="loadEnterprises" />
          </label>
          <button type="button" class="public-enterprises-page__icon-btn" @click="onFeaturePending('通知中心')">
            <span class="material-symbols-outlined" aria-hidden="true">notifications</span>
          </button>
          <button type="button" class="public-enterprises-page__icon-btn" @click="onFeaturePending('个人中心')">
            <span class="material-symbols-outlined" aria-hidden="true">account_circle</span>
          </button>
          <button type="button" class="ghost public-enterprises-page__logout" @click="handleLogout">退出登录</button>
        </div>
      </div>
    </header>
    <main class="public-enterprises-page__main">
      <section class="public-enterprises-page__head">
        <div>
          <div class="public-enterprises-page__crumb">政务公开 / 企业公示</div>
          <h1>企业公示</h1>
          <p>实时展示本行政区域内食品生产经营企业的基本信息及信用评价等级</p>
        </div>
        <div class="public-enterprises-page__filters">
          <label>
            <span>企业名称</span>
            <input
              v-model.trim="filters.enterpriseName"
              type="text"
              placeholder="请输入企业名称"
              @keyup.enter="loadEnterprises"
            />
          </label>
          <label>
            <span>监管状态</span>
            <select v-model="filters.status">
              <option value="">全部状态</option>
              <option value="normal">正常 (A级)</option>
              <option value="key">重点监管 (B级)</option>
            </select>
          </label>
          <div class="public-enterprises-page__filters-actions">
            <button type="button" @click="loadEnterprises">查询</button>
            <button type="button" class="ghost" @click="resetFilters">重置</button>
          </div>
        </div>
      </section>
      <section class="public-enterprises-page__table-card">
        <div class="public-enterprises-page__table-head">
          <span>企业名称 / 社会信用代码</span><span>经营地址 / 所在区域</span><span>监管状态</span><span>操作</span>
        </div>
        <div v-if="!filteredRecords.length" class="public-enterprises-page__empty">暂无符合条件的公示企业</div>
        <div v-for="item in filteredRecords" :key="item.id" class="public-enterprises-page__row">
          <div class="public-enterprises-page__col-main"><strong>{{ item.enterpriseName || "-" }}</strong><small>{{ item.creditCode || "-" }}</small></div>
          <div class="public-enterprises-page__col-sub"><p>{{ item.addressDetail || "-" }}</p><small>{{ item.regionPathText || "未标注区域" }}</small></div>
          <div class="public-enterprises-page__col-status"><i class="public-enterprises-page__status" :class="`is-${statusClass(item.status)}`">{{ formatStatus(item.status) }}</i></div>
          <div class="public-enterprises-page__col-action"><button type="button" @click="viewEnterprise(item)">查看详情</button></div>
        </div>
      </section>
    </main>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from "vue";
import { useRouter } from "vue-router";
import { fetchPublicEnterprises } from "../../api/regulation";
import { getActiveSession, performLogout } from "../../session/authRuntime";

const router = useRouter();
const publicToken = getActiveSession()?.token || "";
const records = ref([]);
const filters = reactive({ enterpriseName: "", status: "" });
const page = ref(1);
const size = ref(8);

const topNavItems = [
  { key: "home", label: "首页", routeName: "public-home" },
  { key: "bulletins", label: "监管公告", routeName: "public-bulletins" },
  { key: "enterprises", label: "企业公示", routeName: "public-enterprises" },
  { key: "sampling", label: "抽检结果", routeName: "public-sampling-results" },
  { key: "complaint-create", label: "我要投诉", routeName: "public-complaint-create" },
  { key: "complaints", label: "我的投诉", routeName: "public-complaints" }
];

function statusClass(value) {
  const key = String(value || "").toUpperCase();
  if (key === "KEY" || key === "B") return "key";
  if (key === "RISK" || key === "C") return "risk";
  return "normal";
}
function formatStatus(value) {
  const key = String(value || "").toUpperCase();
  if (key === "KEY" || key === "B") return "重点监管 (B级)";
  if (key === "RISK" || key === "C") return "失信惩戒 (C级)";
  return "正常 (A级)";
}
function normalizeStatus(value) {
  const key = String(value || "").toUpperCase();
  if (key === "KEY" || key === "B") return "key";
  if (key === "RISK" || key === "C") return "risk";
  return "normal";
}
const filteredRecords = computed(() => {
  const nameKeyword = filters.enterpriseName.trim().toLowerCase();
  const selectedStatus = filters.status;
  return records.value.filter((item) => {
    const nameMatched = !nameKeyword || String(item?.enterpriseName || "").toLowerCase().includes(nameKeyword);
    const statusMatched = !selectedStatus || normalizeStatus(item?.status) === selectedStatus;
    return nameMatched && statusMatched;
  });
});
function goTo(name) {
  router.push({ name }).catch(() => {});
}
async function handleLogout() {
  await performLogout();
  router.replace({ name: "login" }).catch(() => {});
}
function onFeaturePending(name) {
  window.alert(`${name} 功能待后续完善`);
}
function resetFilters() {
  filters.enterpriseName = "";
  filters.status = "";
  loadEnterprises();
}
function viewEnterprise(item) {
  if (!item?.id) return;
  router.push({ name: "public-enterprise-detail", params: { enterpriseId: item.id } }).catch(() => {});
}
async function loadEnterprises() {
  const data = await fetchPublicEnterprises(publicToken, { enterpriseName: filters.enterpriseName, page: page.value, size: size.value });
  records.value = data.records || [];
}
onMounted(loadEnterprises);
</script>

<style scoped>
.public-enterprises-page { min-height: 100vh; background: var(--surface); }
.public-enterprises-page__topbar { position: sticky; top: 0; z-index: 40; border-bottom: 1px solid rgba(195,198,211,.4); background: rgba(248,250,253,.84); }
.public-enterprises-page__topbar-inner { max-width: 1680px; margin: 0 auto; min-height: 56px; padding: 0 16px; display: flex; align-items: center; justify-content: space-between; gap: 24px; }
.public-enterprises-page__brand-nav { display: flex; align-items: center; gap: 24px; }
.public-enterprises-page__brand { font-family: var(--font-display); font-size: 25px; font-weight: 800; color: var(--primary); }
.public-enterprises-page__nav { display: flex; gap: 18px; }
.public-enterprises-page__nav-item { border: none; background: transparent; min-height: 56px; color: var(--on-surface-variant); font-size: 12px; font-weight: 700; border-bottom: 2px solid transparent; cursor: pointer; }
.public-enterprises-page__nav-item.is-active { color: var(--primary); border-bottom-color: var(--primary); }
.public-enterprises-page__toolbar { display: flex; align-items: center; gap: 10px; }
.public-enterprises-page__search-box { display: inline-flex; align-items: center; gap: 6px; border-radius: 8px; border: 1px solid rgba(195,198,211,.44); background: rgba(255,255,255,.75); padding: 0 12px; min-height: 34px; }
.public-enterprises-page__search-box input { border: none; background: transparent; font-size: 12px; min-width: 180px; }
.public-enterprises-page__icon-btn { width: 34px; height: 34px; border-radius: 8px; border: 1px solid transparent; background: transparent; color: var(--on-surface-variant); cursor: pointer; }
.public-enterprises-page__logout { min-height: 34px; margin: 0; }
.public-enterprises-page__main { max-width: 1680px; margin: 0 auto; padding: 24px 16px 48px; }
.public-enterprises-page__crumb { font-size: 12px; color: var(--on-surface-variant); }
.public-enterprises-page__head {
  background: var(--surface-container-lowest);
  border: 1px solid rgba(195, 198, 211, 0.32);
  border-bottom: none;
  border-radius: 12px 12px 0 0;
  padding: 16px 18px 14px;
}
.public-enterprises-page__head h1 { margin: 4px 0 8px; color: var(--primary); font-family: var(--font-display); font-size: 42px; line-height: 1; }
.public-enterprises-page__head p { margin: 0; color: var(--on-surface-variant); font-size: 12px; line-height: 1.6; }
.public-enterprises-page__filters {
  margin-top: 10px;
  padding: 0;
  border: none;
  border-radius: 0;
  background: transparent;
  display: grid;
  grid-template-columns: 260px 180px auto;
  gap: 8px;
  align-items: end;
}
.public-enterprises-page__filters label { display: grid; gap: 4px; }
.public-enterprises-page__filters label span { font-size: 10px; color: #5e6880; font-weight: 500; }
.public-enterprises-page__filters input,
.public-enterprises-page__filters select {
  min-height: 28px;
  border-radius: 4px;
  border: 1px solid #d5dbea;
  background: #fff;
  padding: 0 8px;
  font-size: 12px;
  color: #243047;
}
.public-enterprises-page__filters input:focus,
.public-enterprises-page__filters select:focus {
  outline: none;
  border-color: #7393d7;
  box-shadow: none;
}
.public-enterprises-page__filters-actions { display: inline-flex; gap: 8px; }
.public-enterprises-page__filters-actions button { min-height: 28px; padding: 0 10px; border-radius: 4px; border: 1px solid transparent; font-size: 12px; cursor: pointer; font-weight: 600; }
.public-enterprises-page__filters-actions button:first-child { background: #0a3d86; color: #fff; }
.public-enterprises-page__filters-actions button:first-child:hover { background: #124898; }
.public-enterprises-page__filters-actions button.ghost { border-color: #d5dbea; background: #f7f8fb; color: #56607a; }
.public-enterprises-page__table-card { border-radius: 0 0 12px 12px; overflow: hidden; border: 1px solid rgba(195,198,211,.3); background: var(--surface-container-lowest); }
.public-enterprises-page__table-head { display: grid; grid-template-columns: 3fr 3fr 2fr 2fr; gap: 8px; padding: 13px 18px; background: linear-gradient(135deg,#003a8c 0%,#0b4f9f 100%); color: #fff; font-size: 11px; font-weight: 700; letter-spacing: .04em; }
.public-enterprises-page__row { display: grid; grid-template-columns: 3fr 3fr 2fr 2fr; gap: 8px; align-items: center; padding: 14px 18px; border-top: 1px solid rgba(195,198,211,.24); transition: background-color .2s ease; }
.public-enterprises-page__row:hover { background: rgba(70, 89, 231, 0.04); }
.public-enterprises-page__col-main strong { display: block; color: var(--primary); font-size: 16px; line-height: 1.2; }
.public-enterprises-page__col-main small,.public-enterprises-page__col-sub small { color: var(--on-surface-variant); font-size: 11px; }
.public-enterprises-page__col-sub p { margin: 0; font-size: 12px; line-height: 1.45; color: var(--on-surface); }
.public-enterprises-page__status { display: inline-flex; min-height: 24px; align-items: center; justify-content: center; padding: 0 9px; border-radius: 999px; font-size: 10px; font-weight: 800; border: 1px solid transparent; white-space: nowrap; }
.public-enterprises-page__status.is-normal { background: rgba(33,156,84,.1); color: #1f6e45; border-color: rgba(33,156,84,.2); }
.public-enterprises-page__status.is-key { background: rgba(210,122,0,.14); color: #9b5b00; border-color: rgba(210,122,0,.25); }
.public-enterprises-page__status.is-risk { background: rgba(186,26,26,.12); color: #93000a; border-color: rgba(186,26,26,.24); }
.public-enterprises-page__col-action button { border: 1px solid rgba(70, 89, 231, 0.24); background: rgba(70, 89, 231, 0.06); color: var(--primary); font-size: 12px; font-weight: 700; cursor: pointer; min-height: 28px; border-radius: 8px; padding: 0 10px; }
.public-enterprises-page__col-action button:hover { background: rgba(70, 89, 231, 0.12); }
.public-enterprises-page__empty { padding: 22px; text-align: center; color: var(--on-surface-variant); }
@media (max-width: 1100px) { .public-enterprises-page__nav { display: none; } }
@media (max-width: 900px) {
  .public-enterprises-page__head h1 { font-size: 34px; }
  .public-enterprises-page__filters { grid-template-columns: 1fr 1fr; }
  .public-enterprises-page__filters-actions { grid-column: 1 / -1; }
  .public-enterprises-page__table-head,
  .public-enterprises-page__row { grid-template-columns: 2.2fr 2fr 1.8fr 1.2fr; }
}
@media (max-width: 760px) {
  .public-enterprises-page__toolbar { display: none; }
  .public-enterprises-page__head { padding: 14px; }
  .public-enterprises-page__filters { grid-template-columns: 1fr; }
  .public-enterprises-page__table-head,
  .public-enterprises-page__row { padding-left: 12px; padding-right: 12px; }
}
</style>
