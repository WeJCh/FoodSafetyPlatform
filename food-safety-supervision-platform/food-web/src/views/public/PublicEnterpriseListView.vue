<template>
    <PublicWorkspacePage
    page-class="public-enterprises-page"
    active-key="enterprises"
    :show-search="true"
    v-model:search-value="filters.enterpriseName"
    search-placeholder="搜索企业名称"
    :search-min-width="220"
    @search="handleSearch"
  >
    <main class="public-enterprises-page__main">
      <section class="public-enterprises-page__head">
        <div>
          <div class="public-enterprises-page__crumb">政务公开 / 企业公示</div>
          <h1>企业公示</h1>
          <p>实时展示本行政区域内食品生产经营企业的基础信息与信用等级情况。</p>
        </div>
        <div class="public-enterprises-page__filters">
          <label>
            <span>企业名称</span>
            <input
              v-model.trim="filters.enterpriseName"
              type="text"
              placeholder="请输入企业名称"
              @keyup.enter="handleSearch"
            />
          </label>
          <label>
            <span>监管状态</span>
            <select v-model="filters.status">
              <option value="">全部状态</option>
              <option value="normal">正常监管</option>
              <option value="key">重点监管</option>
              <option value="risk">风险关注</option>
            </select>
          </label>
          <div class="public-enterprises-page__filters-actions">
            <button type="button" @click="handleSearch">查询</button>
            <button type="button" class="ghost" @click="resetFilters">重置</button>
          </div>
        </div>
      </section>
      <section class="public-enterprises-page__table-card">
        <div class="public-enterprises-page__table-head">
          <span>企业名称 / 社会信用代码</span><span>经营地址 / 所在区域</span><span>监管状态</span><span>操作</span>
        </div>
        <AppEmptyState
          v-if="!filteredRecords.length"
          :title="emptyTitle"
          :description="emptyDescription"
          class="public-enterprises-page__empty"
        />
        <div v-for="item in filteredRecords" :key="item.id" class="public-enterprises-page__row">
          <div class="public-enterprises-page__col-main"><strong>{{ item.enterpriseName || "-" }}</strong><small>{{ item.creditCode || "-" }}</small></div>
          <div class="public-enterprises-page__col-sub"><p>{{ item.addressDetail || "-" }}</p><small>{{ item.regionPathText || "未标注区域" }}</small></div>
          <div class="public-enterprises-page__col-status">
            <AppStatusTag :label="formatStatus(item.status)" :tone="statusTone(item.status)" />
          </div>
          <div class="public-enterprises-page__col-action"><button type="button" @click="viewEnterprise(item)">查看详情</button></div>
        </div>
      </section>
      <AppStatusToast :message="status.message" :type="status.type" />
    </main>
    </PublicWorkspacePage>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from "vue";
import { useRoute, useRouter } from "vue-router";
import PublicWorkspacePage from "../../components/public/PublicWorkspacePage.vue";
import { fetchPublicEnterprises } from "../../api/regulation";
import AppEmptyState from "../../components/common/AppEmptyState.vue";
import AppStatusTag from "../../components/common/AppStatusTag.vue";
import AppStatusToast from "../../components/common/AppStatusToast.vue";
import { getActiveSession } from "../../session/authRuntime";
import { enterpriseStatusMap, formatStatusLabel, getStatusTone } from "../../utils/statusMaps";
import { getEmptyStateText, resolveErrorMessage } from "../../utils/uiFeedback";

const route = useRoute();
const router = useRouter();
const publicToken = getActiveSession()?.token || "";
const records = ref([]);
const filters = reactive({ enterpriseName: "", status: "" });
const page = ref(1);
const size = ref(8);
const status = reactive({ message: "", type: "" });

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

const hasFilters = computed(() => Boolean(filters.enterpriseName.trim() || filters.status));
const emptyTitle = computed(() => getEmptyStateText("公示企业", hasFilters.value));
const emptyDescription = computed(() =>
  hasFilters.value ? "可以调整企业名称或监管状态后重新查询。" : "当前暂无可公示的企业信息。"
);

function formatStatus(value) {
  return formatStatusLabel(value, enterpriseStatusMap, "正常监管");
}

function statusTone(value) {
  return getStatusTone(value, "ENTERPRISE");
}

function setStatus(message, type = "info") {
  status.message = message;
  status.type = type;
}

function resetFilters() {
  filters.enterpriseName = "";
  filters.status = "";
  syncRouteQuery();
  loadEnterprises();
}

function handleSearch() {
  page.value = 1;
  syncRouteQuery();
  loadEnterprises();
}

function viewEnterprise(item) {
  if (!item?.id) return;
  router.push({ name: "public-enterprise-detail", params: { enterpriseId: item.id }, query: buildQuery() }).catch(() => {});
}

async function loadEnterprises() {
  setStatus("");
  try {
    const data = await fetchPublicEnterprises(publicToken, { enterpriseName: filters.enterpriseName, page: page.value, size: size.value });
    records.value = data.records || [];
  } catch (error) {
    records.value = [];
    setStatus(resolveErrorMessage(error, "企业公示列表加载失败，请稍后重试"), "error");
  }
}

function buildQuery() {
  const nextQuery = {};
  if (filters.enterpriseName.trim()) nextQuery.keyword = filters.enterpriseName.trim();
  if (filters.status) nextQuery.status = filters.status;
  return nextQuery;
}

function syncRouteQuery() {
  router.replace({ query: buildQuery() }).catch(() => {});
}

function applyRouteQuery() {
  filters.enterpriseName = typeof route.query.keyword === "string" ? route.query.keyword.trim() : "";
  filters.status = typeof route.query.status === "string" ? route.query.status : "";
}

onMounted(() => {
  applyRouteQuery();
  loadEnterprises();
});

watch(
  () => [route.query.keyword, route.query.status],
  () => {
    const nextKeyword = typeof route.query.keyword === "string" ? route.query.keyword.trim() : "";
    const nextStatus = typeof route.query.status === "string" ? route.query.status : "";
    if (nextKeyword === filters.enterpriseName && nextStatus === filters.status) return;
    applyRouteQuery();
    page.value = 1;
    loadEnterprises();
  }
);
</script>

<style scoped>
/* 字体与控件尺寸与公众端“抽检结果”页对齐 */
.public-enterprises-page {
  min-height: 100vh;
  background: var(--surface);
}
.public-enterprises-page__topbar {
  position: sticky;
  top: 0;
  z-index: 40;
  border-bottom: 1px solid rgba(195, 198, 211, 0.4);
  background: rgba(248, 250, 253, 0.84);
}
.public-enterprises-page__topbar-inner {
  max-width: 1680px;
  margin: 0 auto;
  min-height: 56px;
  padding: 0 16px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 24px;
}
.public-enterprises-page__brand-nav {
  display: flex;
  align-items: center;
  gap: 24px;
}
.public-enterprises-page__brand {
  font-family: var(--font-display);
  font-size: var(--public-brand-size);
  font-weight: 800;
  color: var(--primary);
}
.public-enterprises-page__nav {
  display: flex;
  gap: 18px;
}
.public-enterprises-page__nav-item {
  border: none;
  background: transparent;
  min-height: 56px;
  color: var(--on-surface-variant);
  font-size: var(--public-text-md);
  font-weight: 700;
  border-bottom: 2px solid transparent;
  cursor: pointer;
}
.public-enterprises-page__nav-item.is-active {
  color: var(--primary);
  border-bottom-color: var(--primary);
}
.public-enterprises-page__toolbar {
  display: flex;
  align-items: center;
  gap: 10px;
}
.public-enterprises-page__search-box {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  border-radius: 8px;
  border: 1px solid rgba(195, 198, 211, 0.44);
  background: rgba(255, 255, 255, 0.75);
  padding: 0 14px;
  min-height: var(--public-toolbar-min-h);
}
.public-enterprises-page__search-box input {
  border: none;
  background: transparent;
  font-size: var(--public-text-md);
  min-width: var(--public-toolbar-input-min-w);
}
.public-enterprises-page__account {
  min-height: var(--public-toolbar-min-h);
  margin: 0;
  padding-inline: 12px;
}
.public-enterprises-page__account .material-symbols-outlined {
  font-size: 22px;
}
.public-enterprises-page__logout {
  min-height: var(--public-toolbar-min-h);
  font-size: var(--public-text-md);
  margin: 0;
}
.public-enterprises-page__main {
  max-width: 1680px;
  margin: 0 auto;
  padding: 24px 16px 48px;
}
.public-enterprises-page__crumb {
  font-size: var(--public-text-md);
  color: var(--on-surface-variant);
}
.public-enterprises-page__head {
  background: var(--surface-container-lowest);
  border: 1px solid rgba(195, 198, 211, 0.32);
  border-bottom: none;
  border-radius: 12px 12px 0 0;
  padding: 20px 22px 16px;
}
.public-enterprises-page__head h1 {
  margin: 4px 0 10px;
  color: var(--primary);
  font-family: var(--font-display);
  font-size: var(--public-page-title);
  line-height: 1.05;
}
.public-enterprises-page__head p {
  margin: 0;
  color: var(--on-surface-variant);
  font-size: var(--public-body);
  line-height: 1.65;
  max-width: 48rem;
}
.public-enterprises-page__filters {
  margin-top: 10px;
  padding: 0;
  border: none;
  border-radius: 0;
  background: transparent;
  display: grid;
  grid-template-columns: minmax(220px, 1fr) minmax(180px, 0.9fr) auto;
  gap: 12px 14px;
  align-items: end;
}
.public-enterprises-page__filters label {
  display: grid;
  gap: 6px;
}
.public-enterprises-page__filters label span {
  font-size: var(--public-caption);
  color: #5e6880;
  font-weight: 600;
}
.public-enterprises-page__filters input,
.public-enterprises-page__filters select {
  min-height: var(--public-control-min-h);
  border-radius: 6px;
  border: 1px solid #d5dbea;
  background: #fff;
  padding: 0 12px;
  font-size: var(--public-text-md);
  color: #243047;
}
.public-enterprises-page__filters input:focus,
.public-enterprises-page__filters select:focus {
  outline: none;
  border-color: #7393d7;
  box-shadow: none;
}
.public-enterprises-page__filters-actions {
  display: inline-flex;
  gap: 8px;
}
.public-enterprises-page__filters-actions button {
  min-height: var(--public-control-min-h);
  padding: 0 16px;
  border-radius: 6px;
  border: 1px solid transparent;
  font-size: var(--public-text-md);
  cursor: pointer;
  font-weight: 600;
}
.public-enterprises-page__filters-actions button:first-child {
  background: #0a3d86;
  color: #fff;
}
.public-enterprises-page__filters-actions button:first-child:hover {
  background: #124898;
}
.public-enterprises-page__filters-actions button.ghost {
  border-color: #d5dbea;
  background: #f7f8fb;
  color: #56607a;
}
.public-enterprises-page__table-card {
  border-radius: 0 0 12px 12px;
  overflow: hidden;
  border: 1px solid rgba(195, 198, 211, 0.3);
  background: var(--surface-container-lowest);
}
.public-enterprises-page__table-head {
  display: grid;
  grid-template-columns: 3fr 3fr 2fr 2fr;
  gap: 10px;
  padding: 15px 20px;
  background: linear-gradient(135deg, #003a8c 0%, #0b4f9f 100%);
  color: #fff;
  font-size: var(--public-body-secondary);
  font-weight: 700;
  letter-spacing: 0.03em;
}
.public-enterprises-page__row {
  display: grid;
  grid-template-columns: 3fr 3fr 2fr 2fr;
  gap: 10px;
  align-items: center;
  padding: 16px 20px;
  border-top: 1px solid rgba(195, 198, 211, 0.24);
  transition: background-color 0.2s ease;
}
.public-enterprises-page__row:hover {
  background: rgba(70, 89, 231, 0.04);
}
.public-enterprises-page__col-main strong {
  display: block;
  color: var(--primary);
  font-size: var(--public-body-em);
  line-height: 1.25;
}
.public-enterprises-page__col-main small,
.public-enterprises-page__col-sub small {
  color: var(--on-surface-variant);
  font-size: var(--public-body-secondary);
  line-height: 1.4;
}
.public-enterprises-page__col-sub p {
  margin: 0;
  font-size: var(--public-body);
  line-height: 1.5;
  color: var(--on-surface);
}
.public-enterprises-page__col-action button {
  border: 1px solid rgba(70, 89, 231, 0.24);
  background: rgba(70, 89, 231, 0.06);
  color: var(--primary);
  font-size: var(--public-text-md);
  font-weight: 700;
  cursor: pointer;
  min-height: var(--public-btn-action-min-h);
  border-radius: 8px;
  padding: 0 14px;
}
.public-enterprises-page__col-action button:hover {
  background: rgba(70, 89, 231, 0.12);
}
.public-enterprises-page__empty {
  margin: 22px;
}
@media (max-width: 1100px) {
  .public-enterprises-page__nav {
    display: none;
  }
}
@media (max-width: 900px) {
  .public-enterprises-page__head h1 {
    font-size: var(--public-page-title-sm);
  }
  .public-enterprises-page__filters {
    grid-template-columns: 1fr 1fr;
  }
  .public-enterprises-page__filters-actions {
    grid-column: 1 / -1;
  }
  .public-enterprises-page__table-head,
  .public-enterprises-page__row {
    grid-template-columns: 2.2fr 2fr 1.8fr 1.2fr;
  }
}
@media (max-width: 760px) {
  .public-enterprises-page__toolbar {
    display: none;
  }
  .public-enterprises-page__head {
    padding: 14px;
  }
  .public-enterprises-page__filters {
    grid-template-columns: 1fr;
  }
  .public-enterprises-page__table-head,
  .public-enterprises-page__row {
    padding-left: 12px;
    padding-right: 12px;
  }
}
</style>




