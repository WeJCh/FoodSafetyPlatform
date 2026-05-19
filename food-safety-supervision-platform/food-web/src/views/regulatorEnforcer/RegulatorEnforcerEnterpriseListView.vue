<template>
  <RegulatorEnforcerWorkspacePage
    active-key="overview"
    :username="enforcerUser.username || enforcerUser.realName || ''"
    @navigate="handleSidebarNavigate"
    @logout="handleLogout"
  >
    <section class="enterprise-admin-page">
      <header class="enterprise-admin-page__head">
        <div>
          <h1>企业监管列表</h1>
          <p>查看辖区内企业档案、审核状态与监管分级信息。</p>
        </div>
      </header>

      <section class="enterprise-filter-grid">
        <div class="enterprise-filter-grid__name">
          <label>企业名称检索</label>
          <input v-model.trim="filters.enterpriseName" placeholder="输入企业名称" />
        </div>
        <div>
          <label>审核状态</label>
          <select v-model="filters.approvalStatus">
            <option value="">全部</option>
            <option value="PENDING">待审核</option>
            <option value="APPROVED">已通过</option>
            <option value="REJECTED">已驳回</option>
          </select>
        </div>
        <div>
          <label>监管分级</label>
          <select v-model="filters.status">
            <option value="">全部</option>
            <option value="KEY">重点监管</option>
            <option value="NORMAL">常规监管</option>
          </select>
        </div>
        <button type="button" class="primary enterprise-filter-grid__submit" :disabled="loading" @click="handleSearch">
          {{ loading ? "查询中..." : "查询" }}
        </button>
      </section>

      <section class="table-panel">
        <table>
          <thead>
            <tr>
              <th>企业名称 / 信用代码</th>
              <th>所属区域</th>
              <th>负责人</th>
              <th>监管分级</th>
              <th>审核状态</th>
              <th>最后更新时间</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody v-if="records.length">
            <tr v-for="item in records" :key="item.id">
              <td>
                <strong>{{ item.enterpriseName || "-" }}</strong>
                <p>{{ item.creditCode || "-" }}</p>
              </td>
              <td>{{ formatRegionName(item.regionId) }}</td>
              <td>{{ item.principal || "-" }}</td>
              <td>
                <span class="badge" :class="{ key: item.status === 'KEY' }">{{ formatStatus(item.status) }}</span>
              </td>
              <td>
                <span class="badge" :class="approvalBadgeClass(item.approvalStatus)">
                  {{ formatApprovalStatus(item.approvalStatus) }}
                </span>
              </td>
              <td>{{ formatTime(item.updateTime) }}</td>
              <td>
                <button type="button" class="link" @click="handleViewDetail(item)">查看详情</button>
              </td>
            </tr>
          </tbody>
        </table>
        <div v-if="!records.length" class="empty">{{ loading ? "正在加载企业数据..." : "暂无企业数据" }}</div>
      </section>

      <footer class="table-footer">
        <span>显示第 {{ pageStart }}-{{ pageEnd }} 条，共 {{ total }} 条</span>
        <div>
          <button type="button" :disabled="page <= 1 || loading" @click="changePage(page - 1)">上一页</button>
          <span>{{ page }} / {{ pages }}</span>
          <button type="button" :disabled="page >= pages || loading" @click="changePage(page + 1)">下一页</button>
        </div>
      </footer>

      <div class="stats-grid">
        <article><p>企业总数</p><strong>{{ total }}</strong></article>
        <article><p>重点监管</p><strong>{{ keyCount }}</strong></article>
        <article><p>待审核</p><strong>{{ pendingCount }}</strong></article>
        <article><p>已通过</p><strong>{{ approvedCount }}</strong></article>
      </div>

      <div v-if="status.message" class="status" :class="status.type">{{ status.message }}</div>
    </section>
  </RegulatorEnforcerWorkspacePage>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from "vue";
import { useRouter } from "vue-router";
import { fetchEnterprises, fetchRegionPath } from "../../api/regulation";
import RegulatorEnforcerWorkspacePage from "../../components/regulatorEnforcer/RegulatorEnforcerWorkspacePage.vue";
import { formatByMap, formatTime } from "../../utils/formatters";
import { resolveErrorMessage } from "../../utils/uiFeedback";
import { approvalStatusMap, enterpriseStatusMap } from "../../utils/statusMaps";
import { useRegulatorEnforcerShellSession } from "./regulatorEnforcerShared";

const router = useRouter();
const { enforcerUser, token, handleSidebarNavigate, handleLogout } = useRegulatorEnforcerShellSession();
const loading = ref(false);
const records = ref([]);
const page = ref(1);
const size = ref(10);
const total = ref(0);
const pages = ref(1);
const regionNameMap = reactive({});
const status = reactive({ message: "", type: "" });
const filters = reactive({
  enterpriseName: "",
  status: "",
  approvalStatus: ""
});

const pageStart = computed(() => (total.value ? (page.value - 1) * size.value + 1 : 0));
const pageEnd = computed(() => Math.min(page.value * size.value, total.value));
const keyCount = computed(() => records.value.filter((item) => item.status === "KEY").length);
const pendingCount = computed(() => records.value.filter((item) => item.approvalStatus === "PENDING").length);
const approvedCount = computed(() => records.value.filter((item) => item.approvalStatus === "APPROVED").length);

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

function approvalBadgeClass(value) {
  if (value === "APPROVED") return "ok";
  if (value === "PENDING") return "pending";
  if (value === "REJECTED") return "reject";
  return "";
}

function formatRegionName(regionId) {
  if (!regionId) return "-";
  return regionNameMap[regionId] || `区域 ${regionId}`;
}

async function ensureRegionName(regionId) {
  if (!regionId || regionNameMap[regionId]) return;
  try {
    const path = await fetchRegionPath(token.value, regionId);
    regionNameMap[regionId] = Array.isArray(path) && path.length ? path.map((item) => item.name).join(" / ") : `区域 ${regionId}`;
  } catch {
    regionNameMap[regionId] = `区域 ${regionId}`;
  }
}

async function load() {
  loading.value = true;
  setStatus("");
  try {
    const data = await fetchEnterprises(token.value, {
      ...filters,
      page: page.value,
      size: size.value
    });
    records.value = data.records || [];
    await Promise.all((records.value || []).map((item) => ensureRegionName(item.regionId)));
    total.value = data.total || 0;
    page.value = data.page || 1;
    size.value = data.size || size.value;
    pages.value = data.pages || 1;
  } catch (error) {
    setStatus(resolveErrorMessage(error, "加载企业列表失败"), "error");
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
.enterprise-admin-page { display: grid; gap: 16px; }
.enterprise-admin-page__head { display: flex; align-items: end; justify-content: space-between; gap: 12px; }
.enterprise-admin-page__head h1 { margin: 0; color: #002660; font-size: 30px; font-weight: 800; }
.enterprise-admin-page__head p { margin: 6px 0 0; color: #64748b; }
.enterprise-filter-grid { display: grid; grid-template-columns: 2fr 1fr 1fr auto; gap: 10px; background: #fff; padding: 14px; border-radius: 12px; }
.enterprise-filter-grid label { display: block; margin-bottom: 6px; color: #64748b; font-size: 12px; font-weight: 700; text-transform: uppercase; }
.enterprise-filter-grid input, .enterprise-filter-grid select { width: 100%; border: 0; background: #f1f5f9; border-radius: 8px; padding: 10px; }
.enterprise-filter-grid__submit, .table-footer button { border: 0; border-radius: 8px; padding: 9px 14px; cursor: pointer; font-size: 13px; }
.enterprise-filter-grid__submit { align-self: end; min-width: 96px; background: #002660; color: #fff; }
.table-footer button { background: #e2e8f0; color: #1e293b; }
.table-panel { background: #fff; border-radius: 12px; overflow: auto; border: 1px solid #e2e8f0; }
table { width: 100%; border-collapse: collapse; min-width: 1020px; }
th { background: #eef2f7; color: #64748b; font-size: 11px; font-weight: 800; text-transform: uppercase; letter-spacing: 0.08em; padding: 12px; text-align: left; }
td { border-top: 1px solid #edf2f7; padding: 12px; color: #1e293b; font-size: 13px; }
td strong { color: #002660; font-size: 14px; }
td p { margin: 3px 0 0; color: #94a3b8; font-size: 11px; }
.badge { display: inline-flex; padding: 3px 8px; border-radius: 6px; background: #e2e8f0; color: #334155; font-size: 11px; font-weight: 700; }
.badge.key, .badge.reject { background: #fee2e2; color: #991b1b; }
.badge.ok { background: #dcfce7; color: #166534; }
.badge.pending { background: #fef3c7; color: #92400e; }
.link { border: 0; background: transparent; color: #1d4ed8; font-weight: 700; cursor: pointer; }
.table-footer { display: flex; justify-content: space-between; align-items: center; background: #fff; border-radius: 10px; padding: 10px 12px; color: #475569; font-size: 13px; }
.table-footer div { display: flex; gap: 10px; align-items: center; }
.stats-grid { display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); gap: 10px; }
.stats-grid article { background: #fff; border-radius: 10px; padding: 12px; }
.stats-grid p { margin: 0; color: #64748b; font-size: 12px; text-transform: uppercase; }
.stats-grid strong { display: block; margin-top: 6px; font-size: 26px; color: #0f172a; }
.empty { padding: 20px; color: #64748b; font-size: 13px; }
.status { position: fixed; right: 18px; bottom: 18px; background: #0f172a; color: #fff; border-radius: 8px; padding: 10px 12px; font-size: 13px; }
.status.error { background: #b91c1c; }
@media (max-width: 1200px) {
  .enterprise-filter-grid { grid-template-columns: 1fr; }
  .stats-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); }
}
</style>
