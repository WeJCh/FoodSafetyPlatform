<template>
  <RegulatorEnforcerPageShell
    active-key="inspections"
    title="检查记录详情"
    subtitle="查看检查结论、企业信息与分项检查明细，按原型结构展示真实记录数据。"
  >
    <section class="detail-page">
      <div v-if="loading" class="state-card">检查记录详情加载中...</div>
      <div v-else-if="!detail" class="state-card state-card--error">未找到该检查记录或当前账号无权查看。</div>

      <template v-else>
        <header class="topbar">
          <div class="topbar-main">
            <button class="back-btn" type="button" @click="goBack">
              <span class="material-symbols-outlined">arrow_back</span>
            </button>
            <div>
              <h2>检查记录详情</h2>
              <p>ARCHIVE RECORD ID: #{{ detail.record.id || inspectionId }}</p>
            </div>
          </div>
          <button class="ghost" type="button" @click="goBack">返回列表</button>
        </header>

        <div class="hero-grid">
          <section class="hero-card">
            <div class="hero-card__head">
              <div>
                <span class="eyebrow">Final Determination</span>
                <h3>{{ detail.record.result === "FAIL" ? "检查不合格" : "检查合格" }}</h3>
                <p>
                  {{ detail.record.problemDesc || defaultSummary }}
                </p>
              </div>
              <div class="risk-badge" :class="detail.record.result === 'FAIL' ? 'is-risk' : 'is-safe'">
                <span class="material-symbols-outlined">
                  {{ detail.record.result === "FAIL" ? "report" : "verified" }}
                </span>
                <span>{{ detail.record.result === "FAIL" ? "HIGH RISK" : "ARCHIVED" }}</span>
              </div>
            </div>
          </section>

          <aside class="archive-card">
            <h4>归档状态</h4>
            <div class="meta-list">
              <div>
                <span>归档编号</span>
                <strong>REC-{{ detail.record.id || inspectionId }}</strong>
              </div>
              <div>
                <span>关联任务</span>
                <strong>{{ detail.record.taskNo || "-" }}</strong>
              </div>
              <div>
                <span>完整性校验</span>
                <strong class="ok">已校验</strong>
              </div>
            </div>
            <button
              v-if="detail.record.rectificationId"
              class="ghost ghost--full"
              type="button"
              @click="jumpToRectification"
            >
              查看关联整改详情
            </button>
          </aside>
        </div>

        <div class="info-grid">
          <section class="panel panel-wide">
            <div class="panel-head">
              <span class="bar"></span>
              <h4>基础信息</h4>
            </div>
            <div class="kv-grid">
              <div>
                <span>检查任务编号</span>
                <strong>{{ detail.record.taskNo || "-" }}</strong>
              </div>
              <div>
                <span>检查任务标题</span>
                <strong>{{ detail.record.taskTitle || "-" }}</strong>
              </div>
              <div>
                <span>计划日期</span>
                <strong>{{ detail.record.inspectionDate || "-" }}</strong>
              </div>
              <div>
                <span>实际更新时间</span>
                <strong>{{ formatTime(detail.record.updateTime) }}</strong>
              </div>
              <div class="kv-full">
                <span>检查结论</span>
                <strong>{{ formatInspectionResult(detail.record.result) }}</strong>
              </div>
            </div>
          </section>

          <section class="panel panel-wide">
            <div class="panel-head">
              <span class="bar"></span>
              <h4>企业信息</h4>
            </div>
            <div class="enterprise-card">
              <div class="enterprise-icon">
                <span class="material-symbols-outlined">domain</span>
              </div>
              <div class="enterprise-main">
                <h5>{{ detail.record.enterpriseName || "-" }}</h5>
                <p>统一社会信用代码：{{ detail.record.creditCode || "-" }}</p>
              </div>
            </div>
            <div class="kv-grid kv-grid--compact">
              <div>
                <span>地址</span>
                <strong>{{ detail.record.enterpriseAddress || "-" }}</strong>
              </div>
              <div>
                <span>整改状态</span>
                <strong>{{ formatRectificationStatus(detail.record.rectificationStatus) }}</strong>
              </div>
            </div>
          </section>
        </div>

        <div class="content-grid">
          <section class="panel panel-main">
            <div class="table-head">
              <h4>检查项明细</h4>
              <div class="table-head__meta">
                <span>共计 {{ itemCount }} 项</span>
                <span class="warn-chip">异常 {{ abnormalItemCount }} 项</span>
              </div>
            </div>
            <div class="detail-table-wrap">
              <table>
                <thead>
                  <tr>
                    <th>检查要点</th>
                    <th>结果</th>
                    <th>问题说明</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-if="!itemCount">
                    <td colspan="3" class="empty">暂无检查项明细</td>
                  </tr>
                  <tr v-for="(item, index) in detail.items || []" :key="index">
                    <td class="cell-main">{{ item.itemName || "-" }}</td>
                    <td>
                      <span class="result-pill" :class="resultClass(item.itemResult)">
                        {{ formatInspectionResult(item.itemResult) }}
                      </span>
                    </td>
                    <td class="cell-sub">{{ item.problemDesc || "无异常记录" }}</td>
                  </tr>
                </tbody>
              </table>
            </div>
          </section>

          <aside class="side-stack">
            <section class="panel">
              <h4>重点说明</h4>
              <p class="plain-text">{{ detail.record.problemDesc || "当前记录未填写额外问题说明。" }}</p>
            </section>
            <section class="panel">
              <h4>流程提示</h4>
              <p class="plain-text">
                检查记录由执法人员提交归档。如结果为不合格，可继续联动整改流程并回看企业整改状态。
              </p>
            </section>
          </aside>
        </div>

        <div v-if="status.message" class="status-banner" :class="`is-${status.type}`">{{ status.message }}</div>
      </template>
    </section>
  </RegulatorEnforcerPageShell>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from "vue";
import { useRoute, useRouter } from "vue-router";
import { fetchInspectionRecordDetail } from "../../api/regulationOperation";
import { formatTime } from "../../utils/formatters";
import { formatStatusLabel, inspectionResultMap, rectificationStatusMap } from "../../utils/statusMaps";
import { resolveErrorMessage } from "../../utils/uiFeedback";
import RegulatorEnforcerPageShell from "./RegulatorEnforcerPageShell.vue";
import { useRegulatorEnforcerShellSession } from "./regulatorEnforcerShared";

const route = useRoute();
const router = useRouter();
const { token } = useRegulatorEnforcerShellSession();

const inspectionId = computed(() => String(route.params.inspectionId || ""));
const loading = ref(false);
const detail = ref(null);
const status = reactive({ message: "", type: "info" });


const itemCount = computed(() => (Array.isArray(detail.value?.items) ? detail.value.items.length : 0));
const abnormalItemCount = computed(() =>
  (detail.value?.items || []).filter((item) => String(item.itemResult || "").toUpperCase() === "FAIL").length
);
const defaultSummary = computed(() =>
  detail.value?.record?.result === "FAIL"
    ? "经现场核查，当前企业存在异常项，已进入后续执法闭环。"
    : "当前记录未发现重大异常，检查结果已归档。"
);

function setStatus(message = "", type = "info") {
  status.message = message;
  status.type = type;
}

function formatInspectionResult(value) {
  return formatStatusLabel(value, inspectionResultMap);
}

function formatRectificationStatus(value) {
  return formatStatusLabel(value, rectificationStatusMap, "未触发");
}

function resultClass(value) {
  if (value === "PASS") return "is-pass";
  if (value === "FAIL") return "is-fail";
  return "is-default";
}

async function loadDetail() {
  if (!inspectionId.value) {
    detail.value = null;
    return;
  }
  loading.value = true;
  setStatus("");
  try {
    detail.value = await fetchInspectionRecordDetail(token.value, inspectionId.value);
  } catch (error) {
    detail.value = null;
    setStatus(resolveErrorMessage(error, "加载检查记录详情失败"), "error");
  } finally {
    loading.value = false;
  }
}

function goBack() {
  router.push({ name: "regulator-enforcer-inspections" }).catch(() => {});
}

function jumpToRectification() {
  if (!detail.value?.record?.rectificationId) return;
  router.push({
    name: "regulator-enforcer-rectification-detail",
    params: { rectificationId: detail.value.record.rectificationId },
    query: { from: "inspection-detail" }
  }).catch(() => {});
}

onMounted(loadDetail);
watch(() => route.params.inspectionId, loadDetail);
</script>

<style scoped>
.detail-page { display: grid; gap: 16px; }
.state-card { padding: 18px 20px; border: 1px solid #dbe3ee; background: #fff; color: #64748b; }
.state-card--error { color: #b91c1c; border-color: #fecaca; background: #fef2f2; }
.topbar { display: flex; justify-content: space-between; gap: 12px; align-items: center; padding: 14px 18px; background: #f7f9fc; }
.topbar-main { display: flex; gap: 12px; align-items: center; }
.back-btn { width: 38px; height: 38px; border: 0; background: #e6e8eb; color: #002660; cursor: pointer; }
.topbar h2 { margin: 0; color: #002660; font-size: 24px; font-weight: 900; }
.topbar p { margin: 4px 0 0; color: #64748b; font-size: 11px; font-weight: 700; }
.hero-grid { display: grid; grid-template-columns: minmax(0, 1fr) 320px; gap: 16px; }
.hero-card { position: relative; overflow: hidden; padding: 28px; background: linear-gradient(135deg, #002660, #003a8c); color: #fff; }
.hero-card__head { display: flex; justify-content: space-between; gap: 12px; align-items: flex-start; }
.eyebrow { display: inline-block; padding: 4px 10px; background: rgba(255,255,255,0.12); color: rgba(255,255,255,0.82); font-size: 11px; font-weight: 800; letter-spacing: 0.08em; text-transform: uppercase; }
.hero-card h3 { margin: 14px 0 8px; font-size: 38px; font-weight: 900; letter-spacing: -0.03em; }
.hero-card p { margin: 0; max-width: 520px; color: rgba(255,255,255,0.88); line-height: 1.7; font-size: 14px; }
.risk-badge { min-width: 112px; padding: 16px 12px; display: grid; justify-items: center; gap: 6px; border-radius: 14px; color: #fff; font-size: 11px; font-weight: 900; }
.risk-badge.is-risk { background: #ba1a1a; }
.risk-badge.is-safe { background: #166534; }
.risk-badge .material-symbols-outlined { font-size: 38px; }
.archive-card { display: grid; gap: 14px; padding: 20px; background: #fff; border: 1px solid #dbe3ee; }
.archive-card h4,
.panel h4 { margin: 0; color: #002660; font-size: 12px; font-weight: 900; letter-spacing: 0.08em; text-transform: uppercase; }
.meta-list,
.kv-grid,
.side-stack { display: grid; gap: 12px; }
.meta-list div,
.kv-grid div { display: grid; gap: 4px; }
.meta-list span,
.kv-grid span { color: #64748b; font-size: 11px; font-weight: 700; }
.meta-list strong,
.kv-grid strong { color: #0f172a; font-size: 14px; font-weight: 800; line-height: 1.6; }
.ok { color: #166534 !important; }
.ghost,
.ghost--full { min-height: 36px; padding: 0 14px; border: 1px solid #cbd5e1; background: #fff; color: #334155; font-size: 12px; font-weight: 700; cursor: pointer; }
.ghost--full { width: 100%; }
.info-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 16px; }
.panel { padding: 20px; background: #fff; border: 1px solid #dbe3ee; }
.panel-wide { min-height: 100%; }
.panel-head { display: flex; align-items: center; gap: 8px; margin-bottom: 16px; }
.bar { width: 4px; height: 18px; background: #002660; }
.kv-grid { grid-template-columns: 1fr 1fr; gap: 14px 18px; }
.kv-full { grid-column: 1 / -1; }
.kv-grid--compact { margin-top: 14px; }
.enterprise-card { display: flex; gap: 14px; align-items: flex-start; }
.enterprise-icon { width: 52px; height: 52px; display: grid; place-items: center; background: #eef4ff; color: #002660; }
.enterprise-main h5 { margin: 0; color: #0f172a; font-size: 17px; font-weight: 800; }
.enterprise-main p { margin: 8px 0 0; color: #64748b; font-size: 12px; }
.content-grid { display: grid; grid-template-columns: minmax(0, 1fr) 300px; gap: 16px; align-items: start; }
.table-head { padding: 0 0 14px; display: flex; justify-content: space-between; align-items: center; gap: 10px; }
.table-head__meta { display: flex; gap: 8px; flex-wrap: wrap; }
.table-head__meta span { padding: 4px 8px; background: #f8fafc; border: 1px solid #dbe3ee; font-size: 11px; font-weight: 700; color: #475569; }
.warn-chip { background: #fff7ed !important; border-color: #fed7aa !important; color: #b45309 !important; }
.detail-table-wrap { overflow: auto; }
table { width: 100%; min-width: 720px; border-collapse: collapse; }
th, td { padding: 14px; border-bottom: 1px solid #eef2f7; font-size: 13px; text-align: left; vertical-align: top; }
th { background: #f3f6fb; color: #64748b; font-size: 11px; font-weight: 800; text-transform: uppercase; }
.cell-main { font-weight: 700; color: #0f172a; }
.cell-sub { color: #64748b; line-height: 1.6; }
.result-pill { display: inline-flex; min-height: 24px; align-items: center; justify-content: center; padding: 0 10px; border-radius: 2px; border: 1px solid transparent; font-size: 11px; font-weight: 800; }
.result-pill.is-pass { color: #166534; background: #dcfce7; border-color: #86efac; }
.result-pill.is-fail { color: #991b1b; background: #fee2e2; border-color: #fecaca; }
.result-pill.is-default { color: #334155; background: #e2e8f0; border-color: #cbd5e1; }
.plain-text { margin: 12px 0 0; color: #475569; line-height: 1.7; font-size: 13px; }
.empty { text-align: center; color: #64748b; padding: 28px 0; }
.status-banner { padding: 10px 12px; border: 1px solid #dbe3ee; background: #f8fafc; color: #334155; }
.status-banner.is-error { border-color: #fecaca; background: #fef2f2; color: #b91c1c; }
@media (max-width: 1100px) {
  .hero-grid,
  .info-grid,
  .content-grid { grid-template-columns: 1fr; }
}
@media (max-width: 760px) {
  .topbar { flex-direction: column; align-items: stretch; }
  .hero-card h3 { font-size: 28px; }
  .kv-grid { grid-template-columns: 1fr; }
  .hero-card__head { flex-direction: column; }
}
</style>
