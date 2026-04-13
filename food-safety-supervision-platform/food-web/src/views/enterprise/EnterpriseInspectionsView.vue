<template>
  <EnterpriseWorkspacePage
    active-key="inspections"
    title="检查记录"
    subtitle="查看企业历史检查记录、任务信息与整改联动状态。"
    top-search-placeholder="搜索任务编号、任务标题..."
    :username="enterpriseUser.username"
    :user-type="enterpriseUser.userType"
    status-label="检查台账"
    status-tone="neutral"
    @navigate="handleSidebarNavigate"
    @logout="handleLogout"
  >
    <div class="enterprise-workspace-card enterprise-inspections-page">
      <header class="enterprise-page-hero">
        <div>
          <nav class="enterprise-page-hero__crumb" aria-label="面包屑">
            <span>监管平台</span>
            <span class="material-symbols-outlined" aria-hidden="true">chevron_right</span>
            <span class="is-current">检查记录</span>
          </nav>
          <h1 class="enterprise-page-hero__title">检查记录列表</h1>
          <p class="enterprise-page-hero__desc">
            聚合查看检查日期、对应任务以及关联整改任务状态。
          </p>
        </div>
        <div class="enterprise-stat-pill-group">
          <div class="enterprise-stat-pill">
            <span class="enterprise-stat-pill__label">总记录数</span>
            <span class="enterprise-stat-pill__value">{{ inspectionTotal }}</span>
          </div>
          <div class="enterprise-stat-pill">
            <span class="enterprise-stat-pill__label">本页合格</span>
            <span class="enterprise-stat-pill__value">{{ passCount }}</span>
          </div>
          <div class="enterprise-stat-pill enterprise-stat-pill--danger">
            <span class="enterprise-stat-pill__label">本页不合格</span>
            <span class="enterprise-stat-pill__value">{{ failCount }}</span>
          </div>
        </div>
      </header>

      <div class="enterprise-chip-toolbar">
        <div class="enterprise-chip-row" role="toolbar" aria-label="检查结果筛选">
          <button type="button" class="enterprise-chip" :class="{ 'is-active': inspectionFilters.result === '' }" @click="setResultFilter('')">
            全部记录
          </button>
          <button type="button" class="enterprise-chip" :class="{ 'is-active': inspectionFilters.result === 'PASS' }" @click="setResultFilter('PASS')">
            合格
          </button>
          <button type="button" class="enterprise-chip" :class="{ 'is-active': inspectionFilters.result === 'FAIL' }" @click="setResultFilter('FAIL')">
            不合格
          </button>
        </div>
        <button type="button" class="enterprise-toolbar-button" @click="toggleDateFilters">
          <span class="material-symbols-outlined" aria-hidden="true">filter_list</span>
          {{ showDateFilters ? "收起日期筛选" : "日期筛选" }}
        </button>
      </div>

      <div v-show="showDateFilters" class="enterprise-inspections-page__filters">
        <label>
          起始日期
          <input v-model="inspectionFilters.startDate" type="date" />
        </label>
        <label>
          截止日期
          <input v-model="inspectionFilters.endDate" type="date" />
        </label>
        <button class="primary enterprise-inspections-page__filter-submit" type="button" :disabled="inspectionLoading" @click="handleInspectionSearch">
          {{ inspectionLoading ? "查询中..." : "应用筛选" }}
        </button>
      </div>

      <div class="enterprise-inspections-page__content">
        <section class="enterprise-inspections-page__list-panel">
          <div class="enterprise-inspections-page__list-head">
            <h3>
              <span class="material-symbols-outlined" aria-hidden="true">history</span>
              近期检查记录
            </h3>
            <span>第 {{ inspectionPage }}/{{ inspectionPages }} 页</span>
          </div>

          <div class="enterprise-data-table-wrap enterprise-inspections-table-wrap">
            <table v-if="inspectionRecords.length" class="enterprise-data-table enterprise-inspections-table">
              <thead>
                <tr>
                  <th>检查日期</th>
                  <th>任务编号 / 标题</th>
                  <th>检查结果</th>
                  <th>问题摘要</th>
                  <th>整改状态</th>
                  <th style="text-align: right">操作</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="record in inspectionRecords" :key="record.id" class="enterprise-inspections-table__row">
                  <td>
                    <div class="enterprise-inspections-table__date">
                      <span class="enterprise-inspections-table__date-main">{{ inspectionDateParts(record).date }}</span>
                      <span class="enterprise-inspections-table__date-sub">{{ inspectionDateParts(record).time }}</span>
                    </div>
                  </td>
                  <td>
                    <div class="enterprise-inspections-table__task">
                      <span class="enterprise-inspections-table__task-id">{{ inspectionTaskId(record) }}</span>
                      <span class="enterprise-inspections-table__task-type">{{ inspectionTaskType(record) }}</span>
                    </div>
                  </td>
                  <td>
                    <span class="enterprise-inspections-table__result" :class="`is-${inspectionResultClass(record)}`">
                      {{ inspectionResultLabel(record) }}
                    </span>
                  </td>
                  <td class="enterprise-inspections-table__summary">
                    <span :title="inspectionSummary(record)">{{ inspectionSummary(record) }}</span>
                  </td>
                  <td>
                    {{ rectificationStatusText(record) }}
                  </td>
                  <td style="text-align: right">
                    <RouterLink class="enterprise-inspections-table__action" :to="{ name: 'enterprise-inspection-detail', params: { inspectionId: record.id } }">
                      查看详情
                      <span class="material-symbols-outlined" aria-hidden="true">arrow_right_alt</span>
                    </RouterLink>
                  </td>
                </tr>
              </tbody>
            </table>
            <EnterpriseEmptyState v-else title="暂无检查记录" description="检查记录同步后会展示在这里。" />
          </div>

          <div class="enterprise-inspections-pagination">
            <span class="enterprise-inspections-pagination__range">{{ inspectionRangeText }}</span>
            <div class="enterprise-inspections-pagination__pages" role="navigation" aria-label="分页">
              <button type="button" class="enterprise-inspections-pagination__nav" :disabled="inspectionPage <= 1" @click="changeInspectionPage(inspectionPage - 1)" aria-label="上一页">
                <span class="material-symbols-outlined" aria-hidden="true">chevron_left</span>
              </button>
              <template v-for="(p, idx) in inspectionPageTokens" :key="`${p}-${idx}`">
                <span v-if="p === '...'" class="enterprise-inspections-pagination__ellipsis">...</span>
                <button
                  v-else
                  type="button"
                  class="enterprise-inspections-pagination__page"
                  :class="{ 'is-active': p === inspectionPage }"
                  @click="changeInspectionPage(p)"
                >
                  {{ p }}
                </button>
              </template>
              <button type="button" class="enterprise-inspections-pagination__nav" :disabled="inspectionPage >= inspectionPages" @click="changeInspectionPage(inspectionPage + 1)" aria-label="下一页">
                <span class="material-symbols-outlined" aria-hidden="true">chevron_right</span>
              </button>
            </div>
          </div>
        </section>
      </div>

      <div v-if="status.message" class="status" :class="status.type">{{ status.message }}</div>
    </div>
  </EnterpriseWorkspacePage>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from "vue";
import { RouterLink } from "vue-router";
import { fetchEnterpriseInspectionRecords } from "../../api/regulationOperation";
import EnterpriseEmptyState from "../../components/enterprise/EnterpriseEmptyState.vue";
import EnterpriseWorkspacePage from "../../components/enterprise/EnterpriseWorkspacePage.vue";
import { formatTime } from "../../utils/formatters";
import { formatInspectionResult, formatRectificationStatus, useEnterpriseShellSession } from "./enterpriseShared";

const { enterpriseUser, token, handleSidebarNavigate, handleLogout } = useEnterpriseShellSession();
const status = reactive({ message: "", type: "" });
const inspectionLoading = ref(false);
const inspectionRecords = ref([]);
const inspectionPage = ref(1);
const inspectionSize = ref(8);
const inspectionTotal = ref(0);
const inspectionPages = ref(1);
const inspectionFilters = reactive({ result: "", startDate: "", endDate: "" });
const showDateFilters = ref(false);

const passCount = computed(() => inspectionRecords.value.filter((item) => item.result === "PASS").length);
const failCount = computed(() => inspectionRecords.value.filter((item) => item.result === "FAIL").length);
const inspectionRangeText = computed(() => {
  const total = Number(inspectionTotal.value || 0);
  if (!total) return "显示 0 - 0 / 共 0 条记录";
  const start = (inspectionPage.value - 1) * inspectionSize.value + 1;
  const end = Math.min(inspectionPage.value * inspectionSize.value, total);
  return `显示 ${start} - ${end} / 共 ${total} 条记录`;
});

const inspectionPageTokens = computed(() => {
  const current = Number(inspectionPage.value || 1);
  const total = Number(inspectionPages.value || 1);
  if (total <= 5) return Array.from({ length: total }, (_, i) => i + 1);
  const tokens = [1];
  const left = Math.max(2, current - 1);
  const right = Math.min(total - 1, current + 1);
  if (left > 2) tokens.push("...");
  for (let p = left; p <= right; p += 1) tokens.push(p);
  if (right < total - 1) tokens.push("...");
  tokens.push(total);
  return tokens;
});

function setStatus(message, type = "info") {
  status.message = message;
  status.type = type;
}

function setResultFilter(value) {
  inspectionFilters.result = value;
  handleInspectionSearch();
}

function toggleDateFilters() {
  showDateFilters.value = !showDateFilters.value;
}

function inspectionDateParts(record) {
  const rawDate = record?.inspectionDate ? String(record.inspectionDate) : "";
  const time = record?.updateTime ? formatTime(record.updateTime).split(" ").slice(-1)[0] : "—";
  return { date: rawDate || "—", time: time || "—" };
}

function inspectionTaskId(record) {
  return String(record?.taskNo || "").trim() || `INS-${record?.id ?? "—"}`;
}

function inspectionTaskType(record) {
  return String(record?.taskTitle || "").trim() || "现场检查任务";
}

function inspectionSummary(record) {
  return String(record?.problemDesc || "").trim() || "—";
}

function rectificationStatusText(record) {
  return record?.rectificationStatus ? formatRectificationStatus(record.rectificationStatus) : "无";
}

function inspectionResultClass(record) {
  const result = String(record?.result || "").toUpperCase();
  if (result === "PASS") return "pass";
  if (result === "FAIL") return "fail";
  return "rectify";
}

function inspectionResultLabel(record) {
  const result = String(record?.result || "").toUpperCase();
  if (result === "PASS") return "合格 (PASS)";
  if (result === "FAIL") return "不合格 (FAIL)";
  return `${formatInspectionResult(record?.result) || "未知"} (${result || "UNKNOWN"})`;
}

async function loadInspections() {
  inspectionLoading.value = true;
  setStatus("");
  try {
    const data = await fetchEnterpriseInspectionRecords(token.value, {
      ...inspectionFilters,
      page: inspectionPage.value,
      size: inspectionSize.value
    });
    const records = Array.isArray(data.records) ? data.records : [];
    const total = Number(data.total);
    const size = Number(data.size) || inspectionSize.value;
    inspectionRecords.value = records;
    inspectionTotal.value = total > 0 ? total : records.length;
    inspectionPage.value = data.page || 1;
    inspectionSize.value = size;
    inspectionPages.value = Number(data.pages) > 0 ? Number(data.pages) : Math.max(1, Math.ceil(inspectionTotal.value / size));
  } catch (error) {
    setStatus(error.message || "加载检查记录失败", "error");
  } finally {
    inspectionLoading.value = false;
  }
}

async function handleInspectionSearch() {
  inspectionPage.value = 1;
  await loadInspections();
}

async function changeInspectionPage(nextPage) {
  if (!nextPage) return;
  const page = Math.max(1, Math.min(Number(nextPage), inspectionPages.value || 1));
  inspectionPage.value = page;
  await loadInspections();
}

onMounted(() => {
  loadInspections();
});
</script>
