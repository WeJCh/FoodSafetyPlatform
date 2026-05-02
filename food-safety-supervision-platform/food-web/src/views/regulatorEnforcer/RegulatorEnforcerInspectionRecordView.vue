<template>
  <RegulatorEnforcerPageShell
    active-key="inspections"
    title="检查记录"
    subtitle="追踪并复盘历史检查记录，基于当前真实列表数据快速掌握检查结果与异常分布。"
  >
    <section class="stats-grid">
      <article class="stat-card stat-card--accent">
        <span>当前筛选结果</span>
        <strong>{{ total }}</strong>
        <p>接口返回的检查记录总数。</p>
      </article>
      <article class="stat-card">
        <span>当前页合格</span>
        <strong>{{ passCount }}</strong>
        <p>本页结果为“合格”的记录数量。</p>
      </article>
      <article class="stat-card stat-card--warn">
        <span>当前页异常</span>
        <strong>{{ failCount }}</strong>
        <p>本页结果为“不合格”的记录数量。</p>
      </article>
      <article class="stat-card">
        <span>当前页覆盖企业</span>
        <strong>{{ uniqueEnterpriseCount }}</strong>
        <p>本页去重后的企业数量。</p>
      </article>
    </section>

    <section class="filter-card">
      <form class="filter-grid" @submit.prevent="handleSearch">
        <label>
          企业名称
          <input v-model.trim="filters.enterpriseName" placeholder="输入企业名称" />
        </label>
        <label>
          检查结果
          <select v-model="filters.result">
            <option value="">全部</option>
            <option value="PASS">合格</option>
            <option value="FAIL">不合格</option>
          </select>
        </label>
        <label>
          起始日期
          <input v-model="filters.startDate" type="date" />
        </label>
        <label>
          截止日期
          <input v-model="filters.endDate" type="date" />
        </label>
        <div class="filter-actions">
          <button class="primary" type="submit" :disabled="loading">{{ loading ? "查询中..." : "查询" }}</button>
          <button class="ghost" type="button" :disabled="loading" @click="resetFilters">重置</button>
        </div>
      </form>
    </section>

    <section class="table-card">
      <div class="table-wrap">
        <table>
          <thead>
            <tr>
              <th>企业名称</th>
              <th>检查日期</th>
              <th>检查结果</th>
              <th>更新时间</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-if="!records.length && !loading">
              <td colspan="5" class="empty">暂无检查记录</td>
            </tr>
            <tr v-for="record in records" :key="record.id">
              <td class="strong">{{ record.enterpriseName || "-" }}</td>
              <td>{{ record.inspectionDate || "-" }}</td>
              <td>
                <span class="result-pill" :class="resultClass(record.result)">{{ formatInspectionResult(record.result) }}</span>
              </td>
              <td>{{ formatTime(record.updateTime) }}</td>
              <td><button class="ghost" type="button" @click="openDetail(record)">查看详情</button></td>
            </tr>
          </tbody>
        </table>
      </div>
      <footer class="pager">
        <span>共 {{ total }} 条，{{ page }}/{{ pages }} 页</span>
        <div class="pager-actions">
          <button class="ghost" type="button" :disabled="page <= 1 || loading" @click="changePage(page - 1)">上一页</button>
          <button class="ghost" type="button" :disabled="page >= pages || loading" @click="changePage(page + 1)">下一页</button>
        </div>
      </footer>
    </section>

    <section class="insight-grid">
      <article class="insight-card insight-card--trend">
        <div class="insight-head">
          <h3>检查结果趋势</h3>
          <span>{{ complianceRate }}%</span>
        </div>
        <p>当前页合格率按真实列表实时计算，用于快速判断最近检查稳定性。</p>
        <div class="progress-track">
          <div class="progress-fill" :style="{ width: `${complianceRate}%` }"></div>
        </div>
      </article>

      <article class="insight-card">
        <div class="insight-head">
          <h3>最近一次检查</h3>
          <span>{{ latestInspectionDate }}</span>
        </div>
        <p>{{ latestInspectionEnterprise }}</p>
      </article>

      <article class="insight-card">
        <div class="insight-head">
          <h3>异常记录提示</h3>
          <span>{{ failCount }}</span>
        </div>
        <p>{{ failCountHint }}</p>
      </article>
    </section>

    <div v-if="status.message" class="status-banner" :class="`is-${status.type}`">{{ status.message }}</div>
  </RegulatorEnforcerPageShell>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from "vue";
import { useRouter } from "vue-router";
import { fetchMyInspectionRecords } from "../../api/regulationOperation";
import RegulatorEnforcerPageShell from "./RegulatorEnforcerPageShell.vue";
import { formatTime } from "../../utils/formatters";
import { formatStatusLabel, inspectionResultMap } from "../../utils/statusMaps";
import { resolveErrorMessage } from "../../utils/uiFeedback";
import { useRegulatorEnforcerShellSession } from "./regulatorEnforcerShared";

const router = useRouter();
const { token } = useRegulatorEnforcerShellSession();

const loading = ref(false);
const records = ref([]);
const page = ref(1);
const size = ref(8);
const total = ref(0);
const pages = ref(1);
const status = reactive({ message: "", type: "info" });
const filters = reactive({ enterpriseName: "", result: "", startDate: "", endDate: "" });


const passCount = computed(() => records.value.filter((item) => item.result === "PASS").length);
const failCount = computed(() => records.value.filter((item) => item.result === "FAIL").length);
const uniqueEnterpriseCount = computed(() => {
  const names = records.value.map((item) => String(item.enterpriseName || "").trim()).filter(Boolean);
  return new Set(names).size;
});
const complianceRate = computed(() => {
  const count = records.value.length;
  if (!count) return 0;
  return Math.round((passCount.value / count) * 1000) / 10;
});
const latestInspectionRecord = computed(() => {
  const list = [...records.value];
  list.sort((a, b) => String(b.inspectionDate || b.updateTime || "").localeCompare(String(a.inspectionDate || a.updateTime || "")));
  return list[0] || null;
});
const latestInspectionDate = computed(() => latestInspectionRecord.value?.inspectionDate || "-");
const latestInspectionEnterprise = computed(() => {
  if (!latestInspectionRecord.value) return "当前页暂无最近检查记录。";
  return `${latestInspectionRecord.value.enterpriseName || "-"}，结果为${formatInspectionResult(latestInspectionRecord.value.result)}。`;
});
const failCountHint = computed(() => {
  if (!records.value.length) return "当前页没有可用于分析的检查记录。";
  if (!failCount.value) return "当前页没有不合格记录，整体检查结果较稳定。";
  return `当前页共有 ${failCount.value} 条不合格记录，建议优先复核相关企业。`;
});

function setStatus(message = "", type = "info") {
  status.message = message;
  status.type = type;
}

function formatInspectionResult(value) {
  return formatStatusLabel(value, inspectionResultMap);
}

function resultClass(value) {
  if (value === "PASS") return "is-pass";
  if (value === "FAIL") return "is-fail";
  return "is-default";
}

async function loadRecords() {
  loading.value = true;
  setStatus("");
  try {
    const data = await fetchMyInspectionRecords(token.value, {
      ...filters,
      page: page.value,
      size: size.value
    });
    const nextRecords = Array.isArray(data?.records) ? data.records : [];
    const nextTotal = Number(data?.total);
    const nextPage = Number(data?.page);
    const nextSize = Number(data?.size);
    const nextPages = Number(data?.pages);

    records.value = nextRecords;
    total.value = Number.isFinite(nextTotal) && nextTotal > 0 ? nextTotal : nextRecords.length;
    page.value = Number.isFinite(nextPage) && nextPage > 0 ? nextPage : 1;
    size.value = Number.isFinite(nextSize) && nextSize > 0 ? nextSize : size.value;
    pages.value = Number.isFinite(nextPages) && nextPages > 0
      ? nextPages
      : Math.max(1, Math.ceil(total.value / size.value));
  } catch (error) {
    setStatus(resolveErrorMessage(error, "加载检查记录失败"), "error");
  } finally {
    loading.value = false;
  }
}

async function handleSearch() {
  page.value = 1;
  await loadRecords();
}

async function resetFilters() {
  filters.enterpriseName = "";
  filters.result = "";
  filters.startDate = "";
  filters.endDate = "";
  page.value = 1;
  await loadRecords();
}

async function changePage(nextPage) {
  page.value = nextPage;
  await loadRecords();
}

function openDetail(record) {
  if (!record?.id) return;
  router.push({
    name: "regulator-enforcer-inspection-detail",
    params: { inspectionId: record.id }
  }).catch(() => {});
}

onMounted(loadRecords);
</script>

<style scoped>
.stats-grid,
.insight-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
  margin-bottom: 14px;
}

.insight-grid {
  grid-template-columns: 1.2fr 1fr 1fr;
  margin-top: 14px;
}

.stat-card,
.insight-card {
  border: 1px solid #dbe3ee;
  background: #fff;
  padding: 16px;
}

.stat-card--accent {
  background: linear-gradient(135deg, #002660, #003a8c);
  border-color: transparent;
  color: #fff;
}

.stat-card--warn {
  background: linear-gradient(135deg, #fff7ed, #ffedd5);
  border-color: #fed7aa;
}

.stat-card span,
.insight-head h3 {
  display: block;
  color: #64748b;
  font-size: 11px;
  font-weight: 800;
  letter-spacing: 0.04em;
}

.stat-card--accent span,
.stat-card--accent p {
  color: rgba(255, 255, 255, 0.8);
}

.stat-card strong {
  display: block;
  margin-top: 8px;
  color: #0f172a;
  font-size: 28px;
  font-weight: 900;
}

.stat-card--accent strong {
  color: #fff;
}

.stat-card p,
.insight-card p {
  margin: 8px 0 0;
  color: #64748b;
  font-size: 12px;
  line-height: 1.6;
}

.filter-card {
  margin-bottom: 14px;
  padding: 14px;
  border: 1px solid #dbe3ee;
  background: #fff;
}

.filter-grid {
  display: grid;
  grid-template-columns: 1.4fr 1fr 1fr 1fr auto;
  gap: 10px;
  align-items: end;
}

.filter-grid label {
  display: grid;
  gap: 6px;
  font-size: 12px;
  color: #475569;
  font-weight: 700;
}

.filter-grid input,
.filter-grid select {
  min-height: 36px;
  border: 1px solid #d4dce8;
  background: #fff;
  padding: 0 10px;
}

.filter-actions,
.pager-actions {
  display: flex;
  gap: 8px;
}

.primary,
.ghost {
  min-height: 34px;
  padding: 0 14px;
  border: 1px solid #cbd5e1;
  font-size: 12px;
  font-weight: 700;
  cursor: pointer;
}

.primary {
  background: #002660;
  border-color: #002660;
  color: #fff;
}

.ghost {
  background: #fff;
  color: #334155;
}

.table-card {
  border: 1px solid #dbe3ee;
  background: #fff;
}

.table-wrap {
  overflow: auto;
}

table {
  width: 100%;
  min-width: 860px;
  border-collapse: collapse;
}

th,
td {
  padding: 12px 14px;
  border-bottom: 1px solid #eef2f7;
  font-size: 13px;
}

th {
  background: #f3f6fb;
  color: #64748b;
  font-size: 11px;
  font-weight: 800;
  text-transform: uppercase;
  text-align: left;
}

.strong {
  color: #0f3a72;
  font-weight: 700;
}

.result-pill {
  display: inline-flex;
  min-height: 24px;
  align-items: center;
  justify-content: center;
  padding: 0 10px;
  border-radius: 999px;
  border: 1px solid transparent;
  font-size: 11px;
  font-weight: 700;
}

.result-pill.is-pass { color: #166534; background: #dcfce7; border-color: #86efac; }
.result-pill.is-fail { color: #991b1b; background: #fee2e2; border-color: #fecaca; }
.result-pill.is-default { color: #334155; background: #e2e8f0; border-color: #cbd5e1; }

.empty {
  text-align: center;
  color: #64748b;
  padding: 28px 0;
}

.pager {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 14px;
  color: #64748b;
  font-size: 12px;
}

.insight-head {
  display: flex;
  justify-content: space-between;
  gap: 10px;
  align-items: center;
}

.insight-head span {
  color: #002660;
  font-size: 22px;
  font-weight: 900;
}

.insight-card--trend {
  background: linear-gradient(135deg, #f8fbff, #eef4ff);
}

.progress-track {
  margin-top: 14px;
  height: 8px;
  background: #dbeafe;
  overflow: hidden;
}

.progress-fill {
  height: 100%;
  background: linear-gradient(90deg, #002660, #2563eb);
}

.status-banner {
  margin-top: 12px;
  padding: 10px 12px;
  border: 1px solid #dbe3ee;
  background: #f8fafc;
  color: #334155;
}

.status-banner.is-error {
  border-color: #fecaca;
  background: #fef2f2;
  color: #b91c1c;
}

@media (max-width: 1100px) {
  .stats-grid,
  .insight-grid {
    grid-template-columns: 1fr 1fr;
  }

  .filter-grid {
    grid-template-columns: 1fr 1fr;
  }
}

@media (max-width: 640px) {
  .stats-grid,
  .insight-grid,
  .filter-grid {
    grid-template-columns: 1fr;
  }
}
</style>
