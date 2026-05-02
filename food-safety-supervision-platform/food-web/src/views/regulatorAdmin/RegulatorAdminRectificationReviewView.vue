<template>
  <RegulatorAdminWorkspacePage
    active-key="rectification"
    :username="regulatorUser.username"
    @navigate="handleSidebarNavigate"
    @logout="handleLogout"
  >
    <section class="rectification-page">
      <header class="page-head">
        <div>
          <h1>整改复核任务列表</h1>
          <p>统一跟踪整改进度，确保问题闭环。</p>
        </div>
      </header>

      <section class="summary-grid">
        <article class="summary-card summary-card--primary">
          <span>待复核总数</span>
          <strong>{{ total }}</strong>
          <p>当前页 {{ records.length }} 条</p>
        </article>
        <article class="summary-card summary-card--danger">
          <span>逾期未完成</span>
          <strong>{{ overdueCount }}</strong>
          <p>需要优先跟进</p>
        </article>
        <article class="summary-card summary-card--indigo">
          <span>整改进行中</span>
          <strong>{{ ongoingCount }}</strong>
          <p>等待企业提交</p>
        </article>
        <article class="summary-card summary-card--green">
          <span>已确认完成</span>
          <strong>{{ confirmedCount }}</strong>
          <p>闭环已完成</p>
        </article>
      </section>

      <section class="filter-card">
        <label>
          状态
          <select v-model="filters.status">
            <option value="">全部</option>
            <option value="ONGOING">整改中</option>
            <option value="SUBMITTED">待复核</option>
            <option value="REWORK">退回整改</option>
            <option value="CONFIRMED">已确认</option>
          </select>
        </label>
        <label>
          企业名称
          <input v-model.trim="filters.enterpriseName" placeholder="输入企业名称" />
        </label>
        <button class="primary" type="button" :disabled="loading" @click="handleSearch">
          {{ loading ? "查询中..." : "查询" }}
        </button>
      </section>

      <section class="table-card">
        <div class="table-wrap">
          <table>
            <thead>
              <tr>
                <th>企业名称</th>
                <th>问题描述</th>
                <th>状态</th>
                <th>整改截止时间</th>
                <th>时限状态</th>
                <th>操作</th>
              </tr>
            </thead>
            <tbody v-if="records.length">
              <tr v-for="item in records" :key="item.id">
                <td>
                  <div class="ent-name">{{ item.enterpriseName || "-" }}</div>
                  <div class="ent-sub">ID: {{ item.id }}</div>
                </td>
                <td class="desc" :title="item.rectificationDesc || '-'">{{ item.rectificationDesc || "-" }}</td>
                <td>
                  <div class="status-stack">
                    <AppStatusTag :label="formatRectificationStatus(item.status)" :tone="rectificationTone(item.status)" />
                    <AppStatusTag v-if="isRectificationOverdue(item)" label="已逾期" tone="danger" />
                  </div>
                </td>
                <td class="mono">{{ item.currentDeadline ? formatTime(item.currentDeadline) : "-" }}</td>
                <td>
                  <span :class="['sla-pill', `sla-pill--${rectificationSlaClass(item)}`]">
                    {{ formatRectificationSla(item) }}
                  </span>
                </td>
                <td>
                  <div class="action-row">
                    <button class="ghost action-btn" type="button" @click="goDetail(item.id)">详情</button>
                    <button
                      v-if="item.status === 'SUBMITTED'"
                      class="primary action-btn"
                      type="button"
                      :disabled="loading"
                      @click="handleReviewRectification(item, { action: 'CONFIRM' })"
                    >
                      复核通过
                    </button>
                  </div>
                </td>
              </tr>
            </tbody>
          </table>
          <AppEmptyState
            v-if="!records.length"
            :title="emptyTitle"
            :description="emptyDescription"
            class="rectification-page__empty-state"
          />
        </div>
        <div class="pager">
          <span>共 {{ total }} 条，{{ page }}/{{ pages }} 页</span>
          <div class="pager-actions">
            <button class="ghost" type="button" :disabled="page <= 1 || loading" @click="changePage(page - 1)">上一页</button>
            <button class="ghost" type="button" :disabled="page >= pages || loading" @click="changePage(page + 1)">下一页</button>
          </div>
        </div>
      </section>

      <AppStatusToast :message="status.message" :type="status.type" />
    </section>
  </RegulatorAdminWorkspacePage>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from "vue";
import { useRoute, useRouter } from "vue-router";
import AppEmptyState from "../../components/common/AppEmptyState.vue";
import AppStatusTag from "../../components/common/AppStatusTag.vue";
import AppStatusToast from "../../components/common/AppStatusToast.vue";
import RegulatorAdminWorkspacePage from "../../components/regulatorAdmin/RegulatorAdminWorkspacePage.vue";
import { fetchRectifications, reviewRectification } from "../../api/regulationOperation";
import { formatTime } from "../../utils/formatters";
import { formatStatusLabel, getStatusTone, rectificationStatusMap } from "../../utils/statusMaps";
import { resolveErrorMessage } from "../../utils/uiFeedback";
import { useRegulatorAdminShellSession } from "./regulatorAdminShared";

const route = useRoute();
const router = useRouter();
const { regulatorUser, token, handleSidebarNavigate, handleLogout } = useRegulatorAdminShellSession();

const status = reactive({ message: "", type: "" });
const loading = ref(false);
const records = ref([]);
const page = ref(1);
const size = ref(8);
const total = ref(0);
const pages = ref(1);
const filters = reactive({
  status: "",
  enterpriseName: ""
});

const overdueCount = computed(() => records.value.filter((item) => item.slaStatus === "OVERDUE").length);
const ongoingCount = computed(() => records.value.filter((item) => item.status === "ONGOING").length);
const confirmedCount = computed(() => records.value.filter((item) => item.status === "CONFIRMED").length);
const hasFilters = computed(() => Boolean(filters.status || filters.enterpriseName.trim()));
const emptyTitle = computed(() => (hasFilters.value ? "暂无符合条件的整改任务" : "暂无整改任务"));
const emptyDescription = computed(() => (hasFilters.value ? "可以调整状态或企业名称筛选后再试。" : "待复核的整改任务会显示在这里。"));

function setStatus(message = "", type = "info") {
  status.message = message;
  status.type = type;
}

function formatRectificationStatus(value) {
  return formatStatusLabel(value, rectificationStatusMap);
}

function rectificationTone(value) {
  return getStatusTone(value, "RECTIFICATION");
}

function isRectificationOverdue(item) {
  return item?.slaStatus === "OVERDUE" && item?.status !== "CONFIRMED";
}

function formatDurationMinutes(minutes) {
  const totalMins = Math.max(0, Number(minutes) || 0);
  const days = Math.floor(totalMins / (24 * 60));
  const hours = Math.floor((totalMins % (24 * 60)) / 60);
  const mins = totalMins % 60;
  if (days > 0) return `${days}天${hours}小时`;
  if (hours > 0) return `${hours}小时${mins}分钟`;
  return `${mins}分钟`;
}

function rectificationSlaClass(item) {
  if (!item) return "none";
  if (item.slaStatus === "OVERDUE") return "overdue";
  if (item.slaStatus === "DUE_SOON") return "warning";
  if (item.slaStatus === "NORMAL") return "normal";
  return "none";
}

function formatRectificationSla(item) {
  if (!item) return "-";
  const remaining = Number(item.remainingMinutes);
  if (item.slaStatus === "OVERDUE") return `逾期 ${formatDurationMinutes(Math.abs(remaining))}`;
  if (item.slaStatus === "DUE_SOON") return `临期 ${formatDurationMinutes(remaining)}`;
  if (item.slaStatus === "NORMAL") return `剩余 ${formatDurationMinutes(remaining)}`;
  if (item.currentDeadline) return `截止 ${formatTime(item.currentDeadline)}`;
  return "正常推进";
}

async function loadRectifications() {
  loading.value = true;
  setStatus("");
  try {
    const data = await fetchRectifications(token.value, { ...filters, page: page.value, size: size.value });
    records.value = data.records || [];
    total.value = data.total || 0;
    page.value = data.page || 1;
    size.value = data.size || size.value;
    pages.value = data.pages || 1;
  } catch (error) {
    setStatus(resolveErrorMessage(error, "整改任务加载失败，请稍后重试"), "error");
  } finally {
    loading.value = false;
  }
}

async function handleSearch() {
  page.value = 1;
  await loadRectifications();
}

async function changePage(nextPage) {
  page.value = nextPage;
  await loadRectifications();
}

async function handleReviewRectification(item, payload) {
  if (!item?.id || !payload?.action) return;
  loading.value = true;
  setStatus("");
  try {
    await reviewRectification(token.value, item.id, payload);
    setStatus(payload.action === "REWORK" ? "整改任务已退回整改。" : "整改任务已确认通过。", "success");
    await loadRectifications();
  } catch (error) {
    setStatus(resolveErrorMessage(error, "整改复核失败，请稍后重试"), "error");
  } finally {
    loading.value = false;
  }
}

function goDetail(rectificationId) {
  if (!rectificationId) return;
  router.push({ name: "regulator-admin-rectification-detail", params: { rectificationId } }).catch(() => {});
}

onMounted(async () => {
  await loadRectifications();
  const targetId = Number(route.query.rectificationId || 0);
  if (targetId > 0) goDetail(targetId);
});
</script>

<style scoped>
.rectification-page { display: grid; gap: 16px; }
.page-head h1 { margin: 0; color: #002660; font-size: 30px; font-weight: 900; letter-spacing: -0.02em; }
.page-head p { margin: 6px 0 0; color: #64748b; font-size: 14px; }
.summary-grid { display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); gap: 10px; }
.summary-card { background: #fff; border: 1px solid #e2e8f0; border-left-width: 4px; border-radius: 8px; padding: 12px; }
.summary-card span { font-size: 10px; color: #64748b; font-weight: 800; letter-spacing: 0.06em; text-transform: uppercase; }
.summary-card strong { display: block; margin-top: 6px; font-size: 28px; line-height: 1; color: #0f172a; font-weight: 900; }
.summary-card p { margin: 6px 0 0; font-size: 11px; color: #94a3b8; }
.summary-card--primary { border-left-color: #002660; }
.summary-card--danger { border-left-color: #dc2626; }
.summary-card--indigo { border-left-color: #475569; }
.summary-card--green { border-left-color: #00a873; }
.filter-card { background: #fff; border: 1px solid #e2e8f0; border-radius: 10px; padding: 12px; display: grid; grid-template-columns: 1fr 1fr auto; gap: 10px; align-items: end; }
.filter-card label { display: grid; gap: 6px; color: #475569; font-size: 12px; font-weight: 800; }
.filter-card input, .filter-card select { border: 1px solid #dbe2ea; border-radius: 8px; min-height: 38px; padding: 0 10px; font-size: 13px; }
.primary, .ghost { border-radius: 8px; min-height: 38px; font-size: 12px; font-weight: 800; padding: 0 14px; cursor: pointer; }
.primary { border: 0; background: #002660; color: #fff; }
.ghost { border: 1px solid #d1d5db; background: #fff; color: #334155; }
.table-card { background: #fff; border: 1px solid #e2e8f0; border-radius: 10px; overflow: hidden; }
.table-wrap { overflow: auto; }
table { width: 100%; min-width: 1080px; border-collapse: collapse; }
thead tr { background: #e6e8eb; border-bottom: 1px solid #d1d5db; }
th { text-align: left; padding: 12px 14px; font-size: 11px; color: #64748b; text-transform: uppercase; letter-spacing: 0.05em; font-weight: 900; }
td { padding: 12px 14px; border-top: 1px solid #f1f5f9; font-size: 13px; color: #1e293b; }
tbody tr:nth-child(even) { background: #f8fafc; }
tbody tr:hover { background: #f1f5f9; }
.ent-name { font-weight: 800; color: #1e3a8a; }
.ent-sub { margin-top: 2px; font-size: 10px; color: #94a3b8; }
.desc { max-width: 280px; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.mono { font-family: ui-monospace, SFMono-Regular, Menlo, Consolas, monospace; }
.status-stack { display: inline-flex; align-items: center; gap: 6px; flex-wrap: wrap; }
.sla-pill { font-size: 11px; font-weight: 700; }
.sla-pill--normal { color: #475569; }
.sla-pill--warning { color: #b45309; }
.sla-pill--overdue { color: #b91c1c; }
.sla-pill--none { color: #64748b; }
.action-row { display: flex; gap: 8px; align-items: center; }
.action-btn { min-height: 30px; padding: 0 10px; font-size: 11px; }
.rectification-page__empty-state { margin: 16px; }
.pager { display: flex; justify-content: space-between; align-items: center; gap: 10px; padding: 12px 14px; border-top: 1px solid #e2e8f0; font-size: 12px; color: #64748b; font-weight: 700; }
.pager-actions { display: flex; gap: 8px; }
@media (max-width: 1100px) {
  .summary-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); }
  .filter-card { grid-template-columns: 1fr 1fr; }
}
@media (max-width: 760px) {
  .summary-grid, .filter-card { grid-template-columns: 1fr; }
}
</style>
