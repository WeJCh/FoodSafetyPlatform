<template>
  <section class="warning-stats">
    <div class="warning-stats-head">
      <div>
        <div class="warning-stats-title">预警统计</div>
        <div class="warning-stats-subtitle">
          {{ mode === "enforcer" ? "默认按本人权限范围统计" : "默认按辖区权限范围统计" }}
        </div>
      </div>
      <form class="warning-stats-filters" @submit.prevent="handleSearch">
        <div class="range-group">
          <button class="range-chip" :class="{ active: filters.rangePreset === 'today' }" type="button" @click="useQuickRange('today')">
            今日
          </button>
          <button class="range-chip" :class="{ active: filters.rangePreset === '7d' }" type="button" @click="useQuickRange('7d')">
            近7天
          </button>
          <button class="range-chip" :class="{ active: filters.rangePreset === '30d' }" type="button" @click="useQuickRange('30d')">
            近30天
          </button>
          <button class="range-chip" :class="{ active: filters.rangePreset === 'custom' }" type="button" @click="useQuickRange('custom')">
            自定义
          </button>
        </div>
        <label>
          开始时间
          <input v-model="filters.startTime" type="datetime-local" @input="activateCustomRange" />
        </label>
        <label>
          结束时间
          <input v-model="filters.endTime" type="datetime-local" @input="activateCustomRange" />
        </label>
        <label>
          类型TopN
          <input v-model.number="filters.topN" type="number" min="3" max="20" />
        </label>
        <label>
          超时阈值(小时)
          <input v-model.number="filters.overdueHours" type="number" min="1" max="240" />
        </label>
        <button class="primary" type="submit" :disabled="loading">
          {{ loading ? "刷新中..." : "刷新统计" }}
        </button>
      </form>
    </div>

    <div class="overview-grid">
      <article v-for="card in cards" :key="card.key" class="overview-card" :class="`overview-card--${card.key}`">
        <span>{{ card.label }}</span>
        <strong>{{ card.value }}</strong>
      </article>
    </div>

    <div class="distribution-grid">
      <section class="panel-block distribution-block">
        <div class="panel-title">状态分布</div>
        <div class="distribution-list">
          <div v-for="item in statusDistribution" :key="item.key" class="distribution-item">
            <div class="distribution-head">
              <span>{{ item.label }}</span>
              <strong>{{ item.count }}</strong>
            </div>
            <div class="distribution-track">
              <div
                class="distribution-bar"
                :class="`distribution-bar--${statusBarClass(item.key)}`"
                :style="{ width: `${calcDistributionWidth(item.count, statusMax)}` }"
              ></div>
            </div>
          </div>
        </div>
      </section>

      <section class="panel-block distribution-block">
        <div class="panel-title">等级分布</div>
        <div class="level-grid">
          <article v-for="item in levelDistribution" :key="item.key" class="level-card" :class="`level-card--${item.key.toLowerCase()}`">
            <span>{{ item.label }}</span>
            <strong>{{ item.count }}</strong>
            <em>占比 {{ formatDistributionPercent(item.count, levelTotal) }}</em>
          </article>
        </div>
      </section>
    </div>

    <div class="warning-stats-body">
      <section class="panel-block trend-block">
        <div class="panel-title">趋势变化</div>
        <div v-if="!trend.length" class="panel-empty">暂无趋势数据</div>
        <div v-else class="trend-canvas">
          <div v-for="point in trend" :key="point.day" class="trend-column">
            <span class="trend-day">{{ formatDay(point.day) }}</span>
            <div class="trend-track">
              <div class="trend-bar" :style="{ height: `${calcTrendHeight(point.count)}%` }"></div>
            </div>
            <strong class="trend-count">{{ Number(point.count) || 0 }}</strong>
          </div>
        </div>
      </section>

      <section class="panel-block type-block">
        <div class="panel-title">类型 Top{{ filters.topN }}</div>
        <div v-if="!types.length" class="panel-empty">暂无类型统计</div>
        <div v-else class="type-list">
          <div v-for="(item, index) in types" :key="`${item.warningType}-${index}`" class="type-item">
            <span class="type-rank">#{{ index + 1 }}</span>
            <span class="type-name" :title="item.warningType || '-'">{{ formatWarningType(item.warningType) }}</span>
            <strong class="type-count">{{ Number(item.count) || 0 }}</strong>
          </div>
        </div>
      </section>

      <section class="panel-block efficiency-block">
        <div class="panel-title">处置效率</div>
        <div class="efficiency-grid">
          <article class="efficiency-card">
            <span>平均处置时长</span>
            <strong>{{ formatMinutes(efficiency.averageResolveMinutes) }}</strong>
            <em>已解决 {{ Number(efficiency.resolvedCount) || 0 }} 条</em>
          </article>
          <article class="efficiency-card">
            <span>超时待处理</span>
            <strong>{{ Number(efficiency.overduePendingCount) || 0 }} 条</strong>
            <em>阈值 {{ Number(efficiency.overdueHours) || filters.overdueHours }} 小时</em>
          </article>
        </div>
      </section>
    </div>

    <div v-if="errorMessage" class="stats-error">{{ errorMessage }}</div>
  </section>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from "vue";
import {
  fetchWarningEfficiency,
  fetchWarningOverview,
  fetchWarningTrend,
  fetchWarningTypes
} from "../api/regulation";

const props = defineProps({
  token: {
    type: String,
    required: true
  },
  mode: {
    type: String,
    default: "admin"
  }
});

const loading = ref(false);
const errorMessage = ref("");
const overview = ref({});
const trend = ref([]);
const types = ref([]);
const efficiency = ref({});
const warningTypeLabelMap = {
  SLA_OVERDUE_SUBMIT: "企业提交整改超时",
  SLA_OVERDUE_REVIEW: "监管复核整改超时",
  SLA_DUE_SOON_SUBMIT: "企业提交整改即将超时",
  SLA_DUE_SOON_REVIEW: "监管复核整改即将超时"
};
const filters = reactive({
  rangePreset: "7d",
  trendDays: 7,
  startTime: "",
  endTime: "",
  topN: 5,
  overdueHours: 24
});

const cards = computed(() => ([
  { key: "total", label: "预警总数", value: Number(overview.value.totalCount) || 0 },
  { key: "open", label: "待处理", value: Number(overview.value.openCount) || 0 },
  { key: "processing", label: "处理中", value: Number(overview.value.processingCount) || 0 },
  { key: "completed", label: "已处理完成", value: Number(overview.value.completedCount) || 0 }
]));

const statusDistribution = computed(() => {
  const fallback = [
    { key: "OPEN", label: "待处理", count: Number(overview.value.openCount) || 0 },
    { key: "PROCESSING", label: "处理中", count: Number(overview.value.processingCount) || 0 },
    { key: "RESOLVED", label: "已解决", count: Number(overview.value.resolvedCount) || 0 },
    { key: "CLOSED", label: "已归档", count: Number(overview.value.closedCount) || 0 }
  ];
  const data = Array.isArray(overview.value.statusDistribution) ? overview.value.statusDistribution : [];
  if (!data.length) return fallback;
  return data.map((item) => ({
    key: String(item?.key || "").toUpperCase() || "UNKNOWN",
    label: item?.label || item?.key || "-",
    count: Number(item?.count) || 0
  }));
});

const levelDistribution = computed(() => {
  const fallback = [
    { key: "L1", label: "一级", count: 0 },
    { key: "L2", label: "二级", count: 0 }
  ];
  const data = Array.isArray(overview.value.levelDistribution) ? overview.value.levelDistribution : [];
  if (!data.length) return fallback;
  const mapped = data.map((item) => ({
    key: String(item?.key || "").toUpperCase() || "UNKNOWN",
    label: item?.label || item?.key || "-",
    count: Number(item?.count) || 0
  }));
  return [
    mapped.find((item) => item.key === "L1") || fallback[0],
    mapped.find((item) => item.key === "L2") || fallback[1]
  ];
});

const trendMax = computed(() => {
  const values = trend.value.map((item) => Number(item.count) || 0);
  return Math.max(1, ...values);
});

const statusMax = computed(() => Math.max(1, ...statusDistribution.value.map((item) => Number(item.count) || 0)));

const levelTotal = computed(() => levelDistribution.value.reduce((sum, item) => sum + (Number(item.count) || 0), 0));

function normalizeDateTime(value) {
  if (!value) return "";
  return value.length === 16 ? `${value}:00` : value;
}

function buildQueryParams() {
  const params = {
    topN: Math.min(20, Math.max(3, Number(filters.topN) || 5)),
    overdueHours: Math.min(240, Math.max(1, Number(filters.overdueHours) || 24))
  };
  if (filters.rangePreset === "today") {
    const now = new Date();
    const startOfDay = new Date(now.getFullYear(), now.getMonth(), now.getDate(), 0, 0, 0);
    params.startTime = normalizeDateTime(toDateTimeLocal(startOfDay));
    params.endTime = normalizeDateTime(toDateTimeLocal(now));
  } else if (filters.rangePreset === "custom") {
    params.startTime = normalizeDateTime(filters.startTime);
    params.endTime = normalizeDateTime(filters.endTime);
  } else {
    params.trendDays = Math.min(60, Math.max(1, Number(filters.trendDays) || 7));
  }
  return params;
}

async function loadStats() {
  loading.value = true;
  errorMessage.value = "";
  try {
    const params = buildQueryParams();
    const [overviewData, trendData, typeData, efficiencyData] = await Promise.all([
      fetchWarningOverview(props.token, params),
      fetchWarningTrend(props.token, params),
      fetchWarningTypes(props.token, params),
      fetchWarningEfficiency(props.token, params)
    ]);
    overview.value = overviewData || {};
    trend.value = Array.isArray(trendData) ? trendData : [];
    types.value = Array.isArray(typeData) ? typeData : [];
    efficiency.value = efficiencyData || {};
  } catch (error) {
    errorMessage.value = error.message || "预警统计加载失败";
  } finally {
    loading.value = false;
  }
}

function handleSearch() {
  if (filters.rangePreset === "custom" && (!filters.startTime || !filters.endTime)) {
    errorMessage.value = "请选择完整的开始和结束时间";
    return;
  }
  loadStats();
}

function useQuickRange(preset) {
  filters.rangePreset = preset;
  if (preset === "today") {
    const now = new Date();
    const startOfDay = new Date(now.getFullYear(), now.getMonth(), now.getDate(), 0, 0, 0);
    filters.startTime = toDateTimeLocal(startOfDay);
    filters.endTime = toDateTimeLocal(now);
  } else if (preset === "7d") {
    filters.trendDays = 7;
    filters.startTime = "";
    filters.endTime = "";
  } else if (preset === "30d") {
    filters.trendDays = 30;
    filters.startTime = "";
    filters.endTime = "";
  } else {
    return;
  }
  loadStats();
}

function activateCustomRange() {
  filters.rangePreset = "custom";
}

function toDateTimeLocal(date) {
  const pad = (value) => String(value).padStart(2, "0");
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}T${pad(date.getHours())}:${pad(date.getMinutes())}`;
}

function formatDay(day) {
  if (!day) return "-";
  const text = String(day);
  return text.length > 5 ? text.slice(5) : text;
}

function calcTrendHeight(count) {
  const value = Number(count) || 0;
  if (!value) return 8;
  return Math.max(12, Math.round((value / trendMax.value) * 100));
}

function formatMinutes(value) {
  const minutes = Number(value);
  if (!Number.isFinite(minutes) || minutes <= 0) return "-";
  if (minutes < 60) return `${minutes} 分钟`;
  const hours = Math.floor(minutes / 60);
  const remain = minutes % 60;
  if (hours < 24) return `${hours} 小时 ${remain} 分钟`;
  const days = Math.floor(hours / 24);
  return `${days} 天 ${hours % 24} 小时`;
}

function formatWarningType(value) {
  const key = String(value || "").trim().toUpperCase();
  if (!key) return "-";
  return warningTypeLabelMap[key] || key;
}

function calcDistributionWidth(count, maxValue) {
  const value = Number(count) || 0;
  if (!value || !maxValue) return "8%";
  return `${Math.max(8, Math.round((value / maxValue) * 100))}%`;
}

function formatDistributionPercent(count, total) {
  const value = Number(count) || 0;
  const base = Number(total) || 0;
  if (!base) return "0%";
  return `${((value / base) * 100).toFixed(1)}%`;
}

function statusBarClass(key) {
  const value = String(key || "").toUpperCase();
  if (value === "OPEN") return "open";
  if (value === "PROCESSING") return "processing";
  if (value === "RESOLVED") return "resolved";
  if (value === "CLOSED") return "closed";
  return "unknown";
}

onMounted(() => {
  loadStats();
});
</script>

<style scoped>
.warning-stats {
  display: grid;
  gap: 14px;
}

.warning-stats-head {
  display: grid;
  gap: 12px;
}

.warning-stats-title {
  font-size: 20px;
  font-weight: 700;
  color: var(--ink);
}

.warning-stats-subtitle {
  margin-top: 4px;
  font-size: 12px;
  color: var(--muted);
}

.warning-stats-filters {
  display: grid;
  gap: 10px;
  grid-template-columns: 1.2fr repeat(4, minmax(120px, 1fr)) auto;
  align-items: end;
  border: 1px solid var(--stroke);
  border-radius: 12px;
  background: #f6fbff;
  padding: 12px;
}

.range-group {
  display: flex;
  align-items: center;
  gap: 8px;
  min-height: 32px;
}

.range-chip {
  height: 32px;
  border-radius: 999px;
  border: 1px solid var(--stroke);
  background: #fff;
  color: var(--muted);
  padding: 0 12px;
  cursor: pointer;
}

.range-chip.active {
  border-color: #b7d7fc;
  background: #eaf4ff;
  color: #205896;
  font-weight: 600;
}

.warning-stats-filters label {
  min-width: 0;
  display: grid;
  gap: 6px;
  font-size: 12px;
  color: var(--muted);
}

.warning-stats-filters .primary {
  min-width: 108px;
}

.overview-grid {
  display: grid;
  gap: 10px;
  grid-template-columns: repeat(4, minmax(0, 1fr));
}

.overview-card {
  border: 1px solid var(--stroke);
  border-radius: 12px;
  background: var(--card-strong);
  padding: 12px;
  display: grid;
  gap: 8px;
}

.overview-card span {
  font-size: 12px;
  color: var(--muted);
}

.overview-card strong {
  font-size: 24px;
  line-height: 1.1;
  color: var(--ink);
}

.overview-card--total {
  background: linear-gradient(135deg, #f7fbff 0%, #eef5ff 100%);
}

.overview-card--open {
  background: linear-gradient(135deg, #fff9f1 0%, #fff5e8 100%);
}

.overview-card--processing {
  background: linear-gradient(135deg, #f4fbff 0%, #e9f7ff 100%);
}

.overview-card--completed {
  background: linear-gradient(135deg, #f3fbf7 0%, #e7f6ef 100%);
}

.distribution-grid {
  display: grid;
  gap: 12px;
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.distribution-list {
  display: grid;
  gap: 10px;
}

.distribution-item {
  display: grid;
  gap: 6px;
}

.distribution-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  font-size: 12px;
  color: var(--muted);
}

.distribution-head strong {
  color: var(--ink);
  font-size: 13px;
}

.distribution-track {
  height: 10px;
  border-radius: 999px;
  background: #edf3fa;
  overflow: hidden;
  border: 1px solid #d7e3ef;
}

.distribution-bar {
  height: 100%;
  border-radius: inherit;
}

.distribution-bar--open {
  background: linear-gradient(90deg, #ebb07d 0%, #d87f3a 100%);
}

.distribution-bar--processing {
  background: linear-gradient(90deg, #61a5e8 0%, #2d75bf 100%);
}

.distribution-bar--resolved {
  background: linear-gradient(90deg, #6fb992 0%, #34885b 100%);
}

.distribution-bar--closed {
  background: linear-gradient(90deg, #a8b7c8 0%, #728398 100%);
}

.distribution-bar--unknown {
  background: linear-gradient(90deg, #d4dbe3 0%, #9aa9ba 100%);
}

.level-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}

.level-card {
  border: 1px solid var(--stroke);
  border-radius: 10px;
  padding: 10px;
  display: grid;
  gap: 6px;
}

.level-card span {
  font-size: 12px;
  color: var(--muted);
}

.level-card strong {
  font-size: 24px;
  line-height: 1.1;
  color: var(--ink);
}

.level-card em {
  font-style: normal;
  font-size: 12px;
  color: var(--muted);
}

.level-card--l1 {
  background: linear-gradient(135deg, #fff9f1 0%, #fff3e4 100%);
}

.level-card--l2 {
  background: linear-gradient(135deg, #fff2f2 0%, #ffe8e8 100%);
}

.warning-stats-body {
  display: grid;
  gap: 12px;
  grid-template-columns: 1.7fr 1fr 1fr;
  align-items: stretch;
}

.panel-block {
  border: 1px solid var(--stroke);
  border-radius: 12px;
  background: var(--card-strong);
  padding: 12px;
  display: grid;
  gap: 10px;
}

.panel-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--ink);
}

.panel-empty {
  min-height: 120px;
  border: 1px dashed var(--stroke);
  border-radius: 10px;
  display: grid;
  place-items: center;
  font-size: 12px;
  color: var(--muted);
}

.trend-canvas {
  min-height: 190px;
  display: grid;
  gap: 8px;
  grid-template-columns: repeat(auto-fit, minmax(56px, 1fr));
}

.trend-column {
  display: grid;
  grid-template-rows: auto 1fr auto;
  gap: 8px;
  align-items: end;
}

.trend-day {
  text-align: center;
  font-size: 11px;
  color: var(--muted);
}

.trend-track {
  height: 132px;
  border-radius: 8px;
  border: 1px solid #dce8f5;
  background: linear-gradient(180deg, #f9fcff 0%, #f2f8ff 100%);
  position: relative;
  overflow: hidden;
}

.trend-bar {
  position: absolute;
  left: 10px;
  right: 10px;
  bottom: 8px;
  max-height: calc(100% - 16px);
  border-radius: 6px;
  background: linear-gradient(180deg, #3f89d3 0%, #2368ae 100%);
}

.trend-count {
  text-align: center;
  font-size: 12px;
  color: var(--ink);
}

.type-list {
  display: grid;
  gap: 8px;
}

.type-item {
  display: grid;
  grid-template-columns: auto 1fr auto;
  gap: 10px;
  align-items: center;
  border: 1px solid var(--stroke);
  border-radius: 10px;
  padding: 8px 10px;
  background: #f8fbff;
}

.type-rank {
  min-width: 34px;
  text-align: center;
  font-size: 12px;
  color: #2f6ca9;
  border-radius: 999px;
  border: 1px solid #c8def5;
  background: #eaf4ff;
  padding: 2px 8px;
}

.type-name {
  font-size: 12px;
  color: var(--ink);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.type-count {
  font-size: 13px;
  color: var(--ink);
}

.efficiency-grid {
  display: grid;
  gap: 10px;
}

.efficiency-card {
  border: 1px solid var(--stroke);
  border-radius: 10px;
  background: #f7fbff;
  padding: 12px;
  display: grid;
  gap: 6px;
}

.efficiency-card span {
  font-size: 12px;
  color: var(--muted);
}

.efficiency-card strong {
  font-size: 22px;
  color: var(--ink);
  line-height: 1.2;
}

.efficiency-card em {
  font-style: normal;
  font-size: 12px;
  color: var(--muted);
}

.stats-error {
  border: 1px solid #f0c8c8;
  border-radius: 10px;
  background: #fff5f5;
  color: #a53d3d;
  padding: 10px 12px;
  font-size: 12px;
}

@media (max-width: 1280px) {
  .warning-stats-filters {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }

  .distribution-grid {
    grid-template-columns: 1fr;
  }

  .warning-stats-body {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 720px) {
  .warning-stats-filters {
    grid-template-columns: 1fr;
  }

  .overview-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
</style>
