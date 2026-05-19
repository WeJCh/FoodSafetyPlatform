<template>
  <EnterpriseWorkspacePage
    active-key="rectifications"
    title="整改任务"
    subtitle="查看并提交企业待处理的合规整改事项。"
    top-search-placeholder="搜索任务或编号..."
    :username="enterpriseUser.username"
    :user-type="enterpriseUser.userType"
    @navigate="handleSidebarNavigate"
    @logout="handleLogout"
  >
    <div class="enterprise-workspace-card enterprise-rectification-list-page">
      <header class="enterprise-page-hero">
        <div>
          <nav class="enterprise-page-hero__crumb" aria-label="面包屑">
            <span>监管平台</span>
            <span class="material-symbols-outlined" aria-hidden="true">chevron_right</span>
            <span class="is-current">整改任务</span>
          </nav>
          <h1 class="enterprise-page-hero__title">整改任务列表</h1>
          <p class="enterprise-page-hero__desc">查看并提交企业待处理的合规整改事项。</p>
        </div>
        <div class="enterprise-stat-pill-group">
          <div class="enterprise-stat-pill">
            <span class="enterprise-stat-pill__label">待处理总数</span>
            <span class="enterprise-stat-pill__value">{{ rectificationTotal }}</span>
          </div>
          <div class="enterprise-stat-pill enterprise-stat-pill--danger">
            <span class="enterprise-stat-pill__label">即将逾期</span>
            <span class="enterprise-stat-pill__value">{{ dueSoonOrOverdueTotal }}</span>
          </div>
        </div>
      </header>

      <div class="enterprise-chip-toolbar">
        <div class="enterprise-chip-row" role="toolbar" aria-label="状态筛选">
          <button
            type="button"
            class="enterprise-chip"
            :class="{ 'is-active': rectificationFilters.status === '' }"
            @click="setStatusFilter('')"
          >
            全部任务
          </button>
          <button
            type="button"
            class="enterprise-chip"
            :class="{ 'is-active': rectificationFilters.status === 'ONGOING' }"
            @click="setStatusFilter('ONGOING')"
          >
            待整改 (ONGOING)
          </button>
          <button
            type="button"
            class="enterprise-chip"
            :class="{ 'is-active': rectificationFilters.status === 'SUBMITTED' }"
            @click="setStatusFilter('SUBMITTED')"
          >
            已提交 (SUBMITTED)
          </button>
          <button
            type="button"
            class="enterprise-chip"
            :class="{ 'is-active': rectificationFilters.status === 'REWORK' }"
            @click="setStatusFilter('REWORK')"
          >
            需重提 (REWORK)
          </button>
          <button
            type="button"
            class="enterprise-chip"
            :class="{ 'is-active': rectificationFilters.status === 'CONFIRMED' }"
            @click="setStatusFilter('CONFIRMED')"
          >
            已确认 (CONFIRMED)
          </button>
          <button
            type="button"
            class="enterprise-chip"
            :class="{ 'is-active': rectificationFilters.slaFilter === 'OVERDUE' }"
            @click="setSlaFilter('OVERDUE')"
          >
            已逾期
          </button>
          <button
            type="button"
            class="enterprise-chip"
            :class="{ 'is-active': rectificationFilters.slaFilter === 'NOT_OVERDUE' }"
            @click="setSlaFilter('NOT_OVERDUE')"
          >
            未逾期
          </button>
        </div>
        <button type="button" class="enterprise-toolbar-button" @click="toggleAdvanced">
          <span class="material-symbols-outlined" aria-hidden="true">filter_list</span>
          {{ showAdvancedFilters ? "收起筛选" : "筛选条件" }}
        </button>
      </div>

      <div v-show="showAdvancedFilters" class="enterprise-rectification-list-page__advanced">
        <div class="enterprise-rectification-list-page__advanced-label">快速筛选</div>
        <button class="primary enterprise-rectification-list-page__advanced-apply" type="button" :disabled="rectificationLoading" @click="handleRectificationSearch">
          {{ rectificationLoading ? "查询中..." : "应用当前筛选" }}
        </button>
      </div>

      <div class="enterprise-data-table-wrap enterprise-rectification-list-page__table-wrap">
        <table v-if="rectificationRecords.length" class="enterprise-data-table enterprise-rectification-list-page__table">
          <thead>
            <tr>
              <th>任务编号</th>
              <th>整改项目内容</th>
              <th>截止日期</th>
              <th style="text-align: center">当前状态</th>
              <th style="text-align: right">操作行为</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="item in rectificationRecords" :key="item.id" class="enterprise-rectification-list-page__row">
              <td>
                <span class="enterprise-rectification-list-page__id">{{ rectificationTaskCode(item.id) }}</span>
              </td>
              <td>
                <RouterLink
                  class="enterprise-rectification-list-page__desc-link"
                  :to="{ name: 'enterprise-rectification-detail', params: { rectificationId: item.id } }"
                >
                  <div class="enterprise-rectification-list-page__desc-title">{{ item.rectificationDesc || "-" }}</div>
                  <div class="enterprise-rectification-list-page__desc-sub">{{ rectificationDescSub(item) }}</div>
                </RouterLink>
              </td>
              <td>
                <div :class="['enterprise-rectification-list-page__deadline', `is-${rectificationSlaClass(item)}`]">
                  <span class="material-symbols-outlined" v-if="rectificationSlaClass(item) === 'warning' || rectificationSlaClass(item) === 'overdue'" aria-hidden="true">
                    {{ rectificationSlaClass(item) === "overdue" ? "warning" : "error_outline" }}
                  </span>
                  <span>{{ rectificationDeadlineText(item) }}</span>
                  <i v-if="rectificationSlaTag(item)">{{ rectificationSlaTag(item) }}</i>
                </div>
              </td>
              <td style="text-align: center">
                <span :class="['enterprise-rectification-list-page__status-chip', `is-${rectificationStatusClass(item.status)}`]">
                  {{ rectificationStatusLabel(item.status) }}
                </span>
              </td>
              <td style="text-align: right">
                <RouterLink
                  v-if="item.status === 'ONGOING' || item.status === 'REWORK'"
                  class="enterprise-rectification-list-page__action-btn is-primary"
                  :to="{ name: 'enterprise-rectification-submit', params: { rectificationId: item.id } }"
                >
                  {{ item.status === "REWORK" ? "重新完善" : "立即提交" }}
                </RouterLink>
                <button v-else class="enterprise-rectification-list-page__action-btn is-muted" type="button" disabled>审核中...</button>
              </td>
            </tr>
          </tbody>
        </table>
        <EnterpriseEmptyState v-else title="暂无整改任务" description="当前筛选条件下暂无数据。" />
      </div>

      <div class="enterprise-rectification-list-page__pager">
        <span>共 {{ rectificationTotal }} 条，{{ rectificationPage }}/{{ rectificationPages }} 页</span>
        <div class="pager-actions">
          <button class="ghost" type="button" :disabled="rectificationPage <= 1" @click="changeRectificationPage(rectificationPage - 1)">上一页</button>
          <button class="ghost" type="button" :disabled="rectificationPage >= rectificationPages" @click="changeRectificationPage(rectificationPage + 1)">下一页</button>
        </div>
      </div>

      <div class="enterprise-rectification-list-page__guide">
        <div class="enterprise-rectification-list-page__guide-main">
          <h3>整改指南 (Regulatory Standards)</h3>
          <div class="enterprise-rectification-list-page__guide-item">
            <b>1</b>
            <p><strong>证据上传：</strong>所有整改项需提供清晰的现场照片及相关合规证明材料（PDF/JPG 格式）。</p>
          </div>
          <div class="enterprise-rectification-list-page__guide-item">
            <b>2</b>
            <p><strong>截止时间：</strong>逾期未整改事项将自动计入年度诚信扣分，请及时关注临近到期的任务。</p>
          </div>
        </div>
        <div class="enterprise-rectification-list-page__guide-support">
          <span>Support</span>
          <h4>按监管要求准备材料</h4>
          <p>如需线下补充说明，请按任务通知中的监管联系方式处理。</p>
        </div>
      </div>

      <div v-if="status.message" class="status" :class="status.type">{{ status.message }}</div>
    </div>
  </EnterpriseWorkspacePage>
</template>

<script setup>
import { onMounted, reactive, ref } from "vue";
import { RouterLink } from "vue-router";
import { fetchMyRectifications } from "../../api/regulationOperation";
import EnterpriseEmptyState from "../../components/enterprise/EnterpriseEmptyState.vue";
import EnterpriseWorkspacePage from "../../components/enterprise/EnterpriseWorkspacePage.vue";
import { formatTime } from "../../utils/formatters";
import { formatRectificationStatus, useEnterpriseShellSession } from "./enterpriseShared";

const { enterpriseUser, token, handleSidebarNavigate, handleLogout } = useEnterpriseShellSession();
const status = reactive({ message: "", type: "" });
const rectificationLoading = ref(false);
const rectificationRecords = ref([]);
const rectificationPage = ref(1);
const rectificationSize = ref(8);
const rectificationTotal = ref(0);
const rectificationPages = ref(1);
const dueSoonOrOverdueTotal = ref(0);
const rectificationFilters = reactive({ status: "", slaFilter: "" });
const showAdvancedFilters = ref(false);

function setStatusFilter(value) {
  rectificationFilters.status = value;
  handleRectificationSearch();
}

function setSlaFilter(value) {
  rectificationFilters.slaFilter = rectificationFilters.slaFilter === value ? "" : value;
  handleRectificationSearch();
}

function toggleAdvanced() {
  showAdvancedFilters.value = !showAdvancedFilters.value;
}

function setStatus(message, type = "info") {
  status.message = message;
  status.type = type;
}

function formatDurationMinutes(minutes) {
  const total = Math.max(0, Number(minutes) || 0);
  const days = Math.floor(total / (24 * 60));
  const hours = Math.floor((total % (24 * 60)) / 60);
  const mins = total % 60;
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
  if (item.slaStatus === "OVERDUE") return `已超时 ${formatDurationMinutes(Math.abs(remaining))}`;
  if (item.slaStatus === "DUE_SOON") return `即将超时 ${formatDurationMinutes(remaining)}`;
  if (item.slaStatus === "NORMAL") return `剩余 ${formatDurationMinutes(remaining)}`;
  if (item.currentDeadline) return `截止 ${formatTime(item.currentDeadline)}`;
  return "已完成";
}

function rectificationTaskCode(id) {
  const base = String(id || "0000");
  return `#RT-${new Date().getFullYear()}-${base.padStart(4, "0")}`;
}

function rectificationDescSub(item) {
  return item?.remark || item?.problemDesc || "请按监管要求补充整改说明及相关证明材料。";
}

function rectificationDeadlineText(item) {
  if (item?.currentDeadline) return String(formatTime(item.currentDeadline)).split(" ")[0];
  return formatRectificationSla(item);
}

function rectificationSlaTag(item) {
  if (item?.slaStatus === "OVERDUE") return "已逾期";
  if (item?.slaStatus === "DUE_SOON") return "即将超时";
  return "";
}

function rectificationStatusClass(status) {
  if (status === "ONGOING") return "ongoing";
  if (status === "SUBMITTED") return "submitted";
  if (status === "REWORK") return "rework";
  if (status === "CONFIRMED") return "confirmed";
  return "ongoing";
}

function rectificationStatusLabel(status) {
  const label = formatRectificationStatus(status);
  return label === "-" ? "整改中" : label;
}

async function loadRectifications() {
  rectificationLoading.value = true;
  setStatus("");
  try {
    const [data, riskData] = await Promise.all([
      fetchMyRectifications(token.value, {
        ...rectificationFilters,
        page: rectificationPage.value,
        size: rectificationSize.value
      }),
      fetchMyRectifications(token.value, {
        status: rectificationFilters.status,
        slaFilter: "AT_RISK",
        page: 1,
        size: 1
      })
    ]);
    rectificationRecords.value = data.records || [];
    rectificationTotal.value = data.total || 0;
    rectificationPage.value = data.page || 1;
    rectificationSize.value = data.size || rectificationSize.value;
    rectificationPages.value = data.pages || 1;
    dueSoonOrOverdueTotal.value = Number(riskData?.total || 0);
  } catch (error) {
    dueSoonOrOverdueTotal.value = 0;
    setStatus(error.message || "加载整改任务失败", "error");
  } finally {
    rectificationLoading.value = false;
  }
}

async function handleRectificationSearch() {
  rectificationPage.value = 1;
  await loadRectifications();
}

async function changeRectificationPage(nextPage) {
  rectificationPage.value = nextPage;
  await loadRectifications();
}

onMounted(() => {
  loadRectifications();
});
</script>
