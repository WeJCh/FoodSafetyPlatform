<template>
  <section class="supervision-overview">
    <div class="overview-head">
      <div>
        <div class="eyebrow">监管概览</div>
        <h2>把企业、执法、投诉和预警收在同一张业务视图里</h2>
        <p>
          {{ mode === "enforcer" ? "默认按本人任务口径汇总" : "默认按辖区业务口径汇总" }}
        </p>
      </div>
      <button class="refresh-button" type="button" :disabled="loading" @click="loadOverview">
        {{ loading ? "刷新中..." : "刷新概览" }}
      </button>
    </div>

    <div class="hero-grid">
      <article class="hero-card hero-card--enterprise">
        <span>企业全景</span>
        <strong>{{ formatNumber(overview.enterpriseTotalCount) }}</strong>
        <em>纳管企业总数</em>
        <div class="hero-ratio">
          <div>
            <label>重点监管</label>
            <b>{{ formatPercent(overview.keyEnterpriseCount, overview.enterpriseTotalCount) }}</b>
          </div>
          <div>
            <label>备案通过</label>
            <b>{{ formatPercent(overview.approvedEnterpriseCount, overview.enterpriseTotalCount) }}</b>
          </div>
        </div>
      </article>

      <article class="hero-card hero-card--warning">
        <span>待处理预警</span>
        <strong>{{ formatNumber(overview.openWarningCount) }}</strong>
        <em>当前仍在监管链路中的风险提醒</em>
        <div class="warning-caption">
          这个数字复用了现有预警统计口径，用来把“业务执行”和“风险联动”放在同一页里看清楚。
        </div>
      </article>
    </div>

    <div class="metric-grid">
      <article v-for="card in summaryCards" :key="card.key" class="metric-card" :class="`metric-card--${card.key}`">
        <span>{{ card.label }}</span>
        <strong>{{ formatNumber(card.value) }}</strong>
        <em>{{ card.note }}</em>
      </article>
    </div>

    <div class="cluster-grid">
      <section class="cluster-card cluster-card--inspection">
        <div class="cluster-top">
          <span>检查执行</span>
          <strong>{{ formatNumber(overview.inspectionTotalCount) }}</strong>
        </div>
        <div class="cluster-body">
          <article>
            <label>检查总数</label>
            <b>{{ formatNumber(overview.inspectionTotalCount) }}</b>
          </article>
          <article>
            <label>不合格数</label>
            <b>{{ formatNumber(overview.inspectionFailCount) }}</b>
          </article>
          <article>
            <label>不合格占比</label>
            <b>{{ formatPercent(overview.inspectionFailCount, overview.inspectionTotalCount) }}</b>
          </article>
        </div>
      </section>

      <section class="cluster-card cluster-card--sampling">
        <div class="cluster-top">
          <span>抽检闭环</span>
          <strong>{{ formatNumber(overview.samplingTotalCount) }}</strong>
        </div>
        <div class="cluster-body">
          <article>
            <label>抽检总数</label>
            <b>{{ formatNumber(overview.samplingTotalCount) }}</b>
          </article>
          <article>
            <label>不合格数</label>
            <b>{{ formatNumber(overview.samplingFailCount) }}</b>
          </article>
          <article>
            <label>不合格占比</label>
            <b>{{ formatPercent(overview.samplingFailCount, overview.samplingTotalCount) }}</b>
          </article>
        </div>
      </section>

      <section class="cluster-card cluster-card--complaint">
        <div class="cluster-top">
          <span>投诉协同</span>
          <strong>{{ formatNumber(overview.complaintTotalCount) }}</strong>
        </div>
        <div class="cluster-body">
          <article>
            <label>投诉总数</label>
            <b>{{ formatNumber(overview.complaintTotalCount) }}</b>
          </article>
          <article>
            <label>已反馈数</label>
            <b>{{ formatNumber(overview.complaintFeedbackedCount) }}</b>
          </article>
          <article>
            <label>超时数</label>
            <b>{{ formatNumber(overview.complaintOverdueCount) }}</b>
          </article>
        </div>
      </section>
    </div>

    <div v-if="errorMessage" class="overview-error">
      {{ errorMessage }}
    </div>
  </section>
</template>

<script setup>
import { computed, onMounted, ref } from "vue";
import { fetchSupervisionOverview } from "../api/regulation";

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
const overview = ref({
  enterpriseTotalCount: 0,
  keyEnterpriseCount: 0,
  approvedEnterpriseCount: 0,
  inspectionTotalCount: 0,
  inspectionFailCount: 0,
  samplingTotalCount: 0,
  samplingFailCount: 0,
  complaintTotalCount: 0,
  complaintFeedbackedCount: 0,
  complaintOverdueCount: 0,
  openWarningCount: 0
});

const summaryCards = computed(() => ([
  {
    key: "enterprise",
    label: "重点监管企业",
    value: Number(overview.value.keyEnterpriseCount) || 0,
    note: "当前需要重点跟进的企业数量"
  },
  {
    key: "approved",
    label: "备案通过企业",
    value: Number(overview.value.approvedEnterpriseCount) || 0,
    note: "已经完成备案审核的企业数量"
  },
  {
    key: "feedback",
    label: "已反馈投诉",
    value: Number(overview.value.complaintFeedbackedCount) || 0,
    note: "已经形成反馈闭环的投诉数量"
  },
  {
    key: "overdue",
    label: "投诉超时",
    value: Number(overview.value.complaintOverdueCount) || 0,
    note: "已分配但超过截止时间的投诉数量"
  }
]));

async function loadOverview() {
  loading.value = true;
  errorMessage.value = "";
  try {
    const data = await fetchSupervisionOverview(props.token);
    overview.value = {
      ...overview.value,
      ...(data || {})
    };
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : "监管概览加载失败";
  } finally {
    loading.value = false;
  }
}

function formatNumber(value) {
  return new Intl.NumberFormat("zh-CN").format(Number(value) || 0);
}

function formatPercent(value, total) {
  const safeTotal = Number(total) || 0;
  if (!safeTotal) {
    return "0%";
  }
  return `${Math.round(((Number(value) || 0) / safeTotal) * 100)}%`;
}

onMounted(() => {
  loadOverview();
});
</script>

<style scoped>
.supervision-overview {
  --overview-paper: linear-gradient(180deg, #f8fbff 0%, #f3f8fe 100%);
  --overview-line: #d7e1ec;
  --overview-text: #0f172a;
  --overview-muted: #64748b;
  --overview-accent: #1d4f91;
  --overview-forest: #0f766e;
  --overview-rose: #c2410c;
  padding: 16px;
  border-radius: 4px;
  border: 1px solid var(--overview-line);
  background: var(--overview-paper);
  box-shadow: 0 2px 10px rgba(15, 23, 42, 0.04);
  color: var(--overview-text);
  overflow: hidden;
}

.overview-head {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: flex-start;
  margin-bottom: 14px;
}

.eyebrow {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 4px 10px;
  border-radius: 2px;
  background: #eaf2fd;
  color: var(--overview-accent);
  font-size: 11px;
  letter-spacing: 0.12em;
  text-transform: uppercase;
}

.overview-head h2 {
  margin: 10px 0 6px;
  font-size: 24px;
  line-height: 1.2;
}

.overview-head p {
  margin: 0;
  color: var(--overview-muted);
}

.refresh-button {
  border: 1px solid #cfdceb;
  border-radius: 4px;
  padding: 8px 14px;
  background: #ffffff;
  color: #1d4f91;
  cursor: pointer;
  transition: transform 0.2s ease, opacity 0.2s ease, background-color 0.2s ease;
}

.refresh-button:hover:not(:disabled) {
  background: #f1f6fd;
  transform: translateY(-1px);
}

.refresh-button:disabled {
  cursor: wait;
  opacity: 0.72;
}

.hero-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.35fr) minmax(280px, 0.95fr);
  gap: 12px;
  margin-bottom: 12px;
}

.hero-card,
.metric-card,
.cluster-card {
  position: relative;
  overflow: hidden;
  animation: overview-rise 0.45s ease both;
}

.hero-card {
  min-height: 152px;
  padding: 16px;
  border-radius: 4px;
  border: 1px solid #dbe5f1;
  box-shadow: none;
}

.hero-card--enterprise {
  background: linear-gradient(135deg, #f7fbff 0%, #ecf4ff 100%);
}

.hero-card--warning {
  background: linear-gradient(135deg, #f4f9ff 0%, #ebf3fe 100%);
}

.hero-card span,
.cluster-top span,
.metric-card span {
  display: block;
  font-size: 12px;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  color: #64748b;
}

.hero-card strong,
.cluster-top strong,
.metric-card strong {
  display: block;
  margin-top: 8px;
  font-size: 38px;
  font-weight: 800;
  line-height: 1;
}

.hero-card em,
.metric-card em {
  display: block;
  margin-top: 8px;
  font-style: normal;
  color: var(--overview-muted);
}

.hero-ratio {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
  margin-top: 14px;
}

.hero-ratio div,
.warning-caption {
  padding: 10px 12px;
  border-radius: 4px;
  background: #ffffff;
  border: 1px solid #dfe8f3;
}

.hero-ratio label {
  display: block;
  font-size: 12px;
  color: var(--overview-muted);
}

.hero-ratio b {
  display: block;
  margin-top: 4px;
  font-size: 20px;
}

.warning-caption {
  margin-top: 14px;
  line-height: 1.6;
  color: #48617b;
}

.metric-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 10px;
  margin-bottom: 12px;
}

.metric-card {
  padding: 12px;
  border-radius: 4px;
  border: 1px solid #dbe5f1;
  background: #ffffff;
  box-shadow: none;
}

.metric-card--enterprise strong {
  color: var(--overview-accent);
}

.metric-card--approved strong {
  color: var(--overview-forest);
}

.metric-card--feedback strong {
  color: #4979a5;
}

.metric-card--overdue strong {
  color: var(--overview-rose);
}

.cluster-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
}

.cluster-card {
  padding: 12px;
  border-radius: 4px;
  border: 1px solid #dbe5f1;
  background: #ffffff;
}

.cluster-card--inspection {
  background: linear-gradient(180deg, #ffffff 0%, #f4f9ff 100%);
}

.cluster-card--sampling {
  background: linear-gradient(180deg, #ffffff 0%, #f2faf8 100%);
}

.cluster-card--complaint {
  background: linear-gradient(180deg, #ffffff 0%, #f6f8fc 100%);
}

.cluster-body {
  display: grid;
  gap: 8px;
  margin-top: 10px;
}

.cluster-body article {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
  gap: 12px;
  padding: 10px 12px;
  border-radius: 4px;
  background: #f8fbff;
  border: 1px solid #e3ebf5;
}

.cluster-body label {
  color: var(--overview-muted);
}

.cluster-body b {
  font-size: 20px;
}

.overview-error {
  margin-top: 12px;
  padding: 10px 12px;
  border-radius: 4px;
  border: 1px solid #f4cccc;
  background: #fff5f5;
  color: #a53d3d;
}

@keyframes overview-rise {
  from {
    opacity: 0;
    transform: translateY(12px);
  }

  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@media (max-width: 1024px) {
  .hero-grid,
  .metric-grid,
  .cluster-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 720px) {
  .supervision-overview {
    padding: 12px;
  }

  .overview-head {
    flex-direction: column;
  }

  .overview-head h2 {
    font-size: 23px;
  }

  .hero-card strong,
  .cluster-top strong,
  .metric-card strong {
    font-size: 38px;
  }

  .hero-ratio {
    grid-template-columns: 1fr;
  }
}
</style>
