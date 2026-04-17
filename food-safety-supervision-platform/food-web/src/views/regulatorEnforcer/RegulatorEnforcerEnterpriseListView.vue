<template>
  <RegulatorEnforcerPageShell
    active-key="overview"
    title="执法人员工作台"
    subtitle="企业监管总览、筛选查询与详情跳转。"
  >
    <section class="overview-cards">
      <article class="metric-card">
        <span>企业总数</span>
        <strong>{{ total }}</strong>
      </article>
      <article class="metric-card">
        <span>当前页企业</span>
        <strong>{{ records.length }}</strong>
      </article>
      <article class="metric-card">
        <span>重点监管</span>
        <strong>{{ keyCount }}</strong>
      </article>
      <article class="metric-card">
        <span>待审核</span>
        <strong>{{ pendingCount }}</strong>
      </article>
    </section>

    <form class="filter-bar" @submit.prevent="handleSearch">
      <label>
        企业名称
        <input v-model.trim="filters.enterpriseName" placeholder="输入企业名称" />
      </label>
      <label>
        企业状态
        <select v-model="filters.status">
          <option value="">全部</option>
          <option value="NORMAL">正常</option>
          <option value="KEY">重点监管</option>
        </select>
      </label>
      <label>
        审核状态
        <select v-model="filters.approvalStatus">
          <option value="">全部</option>
          <option value="PENDING">待审核</option>
          <option value="APPROVED">已通过</option>
          <option value="REJECTED">已驳回</option>
        </select>
      </label>
      <button class="primary-btn" type="submit" :disabled="loading">
        {{ loading ? "查询中..." : "查询" }}
      </button>
    </form>

    <div class="list-table">
      <div class="list-row list-header">
        <span>企业名称</span>
        <span>状态</span>
        <span>审核</span>
        <span>负责人</span>
        <span>更新时间</span>
        <span>操作</span>
      </div>
      <div v-if="!records.length && !loading" class="list-empty">暂无企业数据</div>
      <div v-for="item in records" :key="item.id" class="list-row">
        <span>{{ item.enterpriseName || "-" }}</span>
        <span>{{ formatStatus(item.status) }}</span>
        <span>{{ formatApprovalStatus(item.approvalStatus) }}</span>
        <span>{{ item.principal || "-" }}</span>
        <span>{{ formatTime(item.updateTime) }}</span>
        <button class="ghost-btn" type="button" @click="handleViewDetail(item)">查看详情</button>
      </div>
    </div>

    <footer class="pager">
      <span>共 {{ total }} 条，{{ page }}/{{ pages }} 页</span>
      <div class="pager-actions">
        <button class="ghost-btn" type="button" :disabled="loading || page <= 1" @click="changePage(page - 1)">上一页</button>
        <button class="ghost-btn" type="button" :disabled="loading || page >= pages" @click="changePage(page + 1)">下一页</button>
      </div>
    </footer>

    <div v-if="status.message" class="status-banner" :class="`is-${status.type}`">
      {{ status.message }}
    </div>
  </RegulatorEnforcerPageShell>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from "vue";
import { useRouter } from "vue-router";
import { fetchEnterprises } from "../../api/regulation";
import { formatByMap, formatTime } from "../../utils/formatters";
import { approvalStatusMap, enterpriseStatusMap } from "../../utils/statusMaps";
import RegulatorEnforcerPageShell from "./RegulatorEnforcerPageShell.vue";
import { useRegulatorEnforcerShellSession } from "./regulatorEnforcerShared";

const router = useRouter();
const { token } = useRegulatorEnforcerShellSession();

const filters = reactive({ enterpriseName: "", status: "", approvalStatus: "" });
const status = reactive({ message: "", type: "info" });
const loading = ref(false);
const records = ref([]);
const page = ref(1);
const size = ref(8);
const total = ref(0);
const pages = ref(1);

const keyCount = computed(() => records.value.filter((item) => item.status === "KEY").length);
const pendingCount = computed(() => records.value.filter((item) => item.approvalStatus === "PENDING").length);

function setStatus(message, type = "info") {
  status.message = message;
  status.type = type;
}

function formatStatus(value) {
  return formatByMap(value, enterpriseStatusMap);
}

function formatApprovalStatus(value) {
  return formatByMap(value, approvalStatusMap);
}

async function load() {
  loading.value = true;
  setStatus("");
  try {
    const data = await fetchEnterprises(token.value, { ...filters, page: page.value, size: size.value });
    records.value = data.records || [];
    total.value = data.total || 0;
    page.value = data.page || 1;
    size.value = data.size || size.value;
    pages.value = data.pages || 1;
  } catch (error) {
    setStatus(error.message || "加载企业列表失败", "error");
  } finally {
    loading.value = false;
  }
}

async function handleSearch() {
  page.value = 1;
  await load();
}

async function changePage(nextPage) {
  page.value = nextPage;
  await load();
}

function handleViewDetail(item) {
  if (!item?.id) return;
  router.push({
    name: "regulator-enforcer-enterprise-detail",
    params: { enterpriseId: item.id },
    query: { from: "enterprises" }
  }).catch(() => {});
}

onMounted(() => {
  load();
});
</script>

<style scoped>
.overview-cards {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
  margin-bottom: 16px;
}
.metric-card {
  border: 1px solid #dbe4ef;
  background: #f8fbff;
  padding: 12px;
}
.metric-card span {
  display: block;
  font-size: 12px;
  color: #64748b;
}
.metric-card strong {
  display: block;
  margin-top: 8px;
  color: #0f172a;
  font-size: 24px;
  font-weight: 800;
}
.filter-bar {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr)) auto;
  gap: 10px;
  margin-bottom: 14px;
}
.filter-bar label {
  display: grid;
  gap: 6px;
  font-size: 12px;
  color: #334155;
}
.filter-bar input,
.filter-bar select {
  height: 34px;
  border: 1px solid #cbd5e1;
  background: #fff;
  padding: 0 10px;
}
.primary-btn,
.ghost-btn {
  height: 34px;
  border: 1px solid #cbd5e1;
  background: #fff;
  color: #1e293b;
  padding: 0 14px;
  cursor: pointer;
}
.primary-btn {
  align-self: end;
  border-color: #002660;
  background: #002660;
  color: #fff;
}
.primary-btn:disabled,
.ghost-btn:disabled {
  opacity: 0.55;
  cursor: not-allowed;
}
.list-table {
  border: 1px solid #dbe4ef;
  background: #fff;
}
.list-row {
  display: grid;
  grid-template-columns: 1.6fr 0.8fr 0.8fr 1fr 1.2fr 0.8fr;
  gap: 10px;
  align-items: center;
  padding: 11px 12px;
  border-bottom: 1px solid #eef2f7;
  font-size: 13px;
}
.list-row:last-child {
  border-bottom: 0;
}
.list-header {
  background: #f8fafc;
  font-weight: 700;
  color: #334155;
}
.list-empty {
  padding: 24px 12px;
  text-align: center;
  color: #64748b;
}
.pager {
  margin-top: 12px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 13px;
  color: #475569;
}
.pager-actions {
  display: flex;
  gap: 8px;
}
.status-banner {
  margin-top: 12px;
  border: 1px solid #cbd5e1;
  background: #f8fafc;
  color: #334155;
  padding: 10px 12px;
  font-size: 13px;
}
.status-banner.is-error {
  border-color: #fecaca;
  background: #fef2f2;
  color: #b91c1c;
}
@media (max-width: 1200px) {
  .overview-cards {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
  .filter-bar {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
</style>

