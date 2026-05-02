<template>
  <RegulatorEnforcerPageShell
    active-key="warnings"
    title="我的风险预警"
    subtitle="查看分派给我的预警并执行处置动作。"
  >
    <section class="hero">
      <article>
        <span>当前预警</span>
        <strong>{{ total }}</strong>
      </article>
      <article>
        <span>待处理</span>
        <strong>{{ openCount }}</strong>
      </article>
      <article>
        <span>处理中</span>
        <strong>{{ processingCount }}</strong>
      </article>
    </section>

    <section class="filter-card">
      <div class="quick-tools">
        <button class="ghost" :class="{ active: onlyPending }" type="button" @click="toggleOnlyPending">
          仅看待处理：{{ onlyPending ? "开启" : "关闭" }}
        </button>
      </div>
      <form class="filter-grid" @submit.prevent="handleSearch">
        <label>
          预警状态
          <select v-model="filters.status" :disabled="onlyPending">
            <option value="">全部</option>
            <option value="OPEN">待处理</option>
            <option value="PROCESSING">处理中</option>
            <option value="RESOLVED">已解决</option>
            <option value="CLOSED">已归档</option>
          </select>
        </label>
        <label>
          预警级别
          <select v-model="filters.level">
            <option value="">全部</option>
            <option value="L1">一级</option>
            <option value="L2">二级</option>
          </select>
        </label>
        <label>
          预警类型
          <input v-model.trim="filters.warningType" placeholder="例：SLA_OVERDUE_SUBMIT" />
        </label>
        <label>
          关键词
          <input v-model.trim="filters.keyword" placeholder="标题或内容关键词" />
        </label>
        <button class="primary" type="submit" :disabled="loading || actionLoading">
          {{ loading ? "查询中..." : "查询" }}
        </button>
      </form>
    </section>

    <section class="table-card">
      <div class="table-wrap">
        <table>
          <thead>
            <tr>
              <th>预警编号</th>
              <th>预警标题</th>
              <th>等级</th>
              <th>状态</th>
              <th>业务对象</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-if="!records.length && !loading">
              <td colspan="6" class="empty">暂无预警记录</td>
            </tr>
            <tr v-for="item in records" :key="item.id">
              <td class="mono">{{ item.warningNo || `#${item.id}` }}</td>
              <td>
                <p class="strong">{{ item.title || "-" }}</p>
                <p class="sub">{{ item.content || "-" }}</p>
              </td>
              <td><span class="level-pill" :class="`is-${String(item.level || '').toLowerCase()}`">{{ formatWarningLevel(item.level) }}</span></td>
              <td><span class="status-pill" :class="`is-${warningStatusClass(item.status)}`">{{ formatWarningStatus(item.status) }}</span></td>
              <td>{{ item.bizName || item.bizType || "-" }}</td>
              <td>
                <div class="action-row">
                  <button class="ghost" type="button" @click="openDetail(item)">查看详情</button>
                  <button
                    v-if="warningQuickAction(item.status)"
                    class="primary"
                    type="button"
                    :disabled="actionLoading"
                    @click="handleWarningAction(item, warningQuickAction(item.status).actionType)"
                  >
                    {{ warningQuickAction(item.status).label }}
                  </button>
                </div>
              </td>
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

    <div v-if="status.message" class="status-banner" :class="`is-${status.type}`">{{ status.message }}</div>
  </RegulatorEnforcerPageShell>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from "vue";
import { useRouter } from "vue-router";
import { fetchMyWarningRecords, processMyWarning } from "../../api/regulation";
import RegulatorEnforcerPageShell from "./RegulatorEnforcerPageShell.vue";
import { formatByMap, formatTime } from "../../utils/formatters";
import { warningActionMap, warningLevelMap, warningStatusMap } from "../../utils/statusMaps";
import { resolveErrorMessage } from "../../utils/uiFeedback";
import { useRegulatorEnforcerShellSession } from "./regulatorEnforcerShared";

const router = useRouter();
const { token } = useRegulatorEnforcerShellSession();

const loading = ref(false);
const actionLoading = ref(false);
const records = ref([]);
const page = ref(1);
const size = ref(8);
const total = ref(0);
const pages = ref(1);
const onlyPending = ref(false);
const backupStatus = ref("");
const status = reactive({ message: "", type: "info" });
const filters = reactive({
  status: "",
  level: "",
  warningType: "",
  keyword: ""
});

const openCount = computed(() => records.value.filter((item) => item.status === "OPEN").length);
const processingCount = computed(() => records.value.filter((item) => item.status === "PROCESSING").length);

function setStatus(message = "", type = "info") {
  status.message = message;
  status.type = type;
}

function formatWarningStatus(value) {
  return formatByMap(value, warningStatusMap);
}

function formatWarningLevel(value) {
  return formatByMap(value, warningLevelMap);
}

function formatWarningAction(value) {
  return formatByMap(value, warningActionMap);
}

function warningStatusClass(value) {
  if (value === "OPEN") return "open";
  if (value === "PROCESSING") return "processing";
  if (value === "RESOLVED") return "resolved";
  if (value === "CLOSED") return "closed";
  return "unknown";
}

function warningQuickAction(statusValue) {
  if (statusValue === "OPEN") return { actionType: "PROCESS", label: "开始处理" };
  if (statusValue === "PROCESSING") return { actionType: "RESOLVE", label: "标记解决" };
  return null;
}

async function loadWarnings() {
  loading.value = true;
  setStatus("");
  try {
    const data = await fetchMyWarningRecords(token.value, {
      ...filters,
      page: page.value,
      size: size.value
    });
    records.value = data.records || [];
    total.value = data.total || 0;
    page.value = data.page || 1;
    size.value = data.size || size.value;
    pages.value = data.pages || 1;
  } catch (error) {
    setStatus(resolveErrorMessage(error, "加载预警列表失败"), "error");
  } finally {
    loading.value = false;
  }
}

async function handleSearch() {
  page.value = 1;
  await loadWarnings();
}

async function changePage(nextPage) {
  page.value = nextPage;
  await loadWarnings();
}

async function toggleOnlyPending() {
  onlyPending.value = !onlyPending.value;
  if (onlyPending.value) {
    backupStatus.value = filters.status;
    filters.status = "OPEN";
  } else {
    filters.status = backupStatus.value || "";
  }
  page.value = 1;
  await loadWarnings();
}

async function openDetail(item) {
  if (!item?.id) return;
  router.push({
    name: "regulator-enforcer-warning-detail",
    params: { warningId: item.id }
  }).catch(() => {});
}

async function handleWarningAction(target, actionType) {
  if (!target?.id || !actionType) return;
  actionLoading.value = true;
  setStatus("");
  try {
    await processMyWarning(token.value, target.id, { actionType });
    setStatus(`预警已执行${formatWarningAction(actionType)}`, "success");
    await loadWarnings();
  } catch (error) {
    setStatus(resolveErrorMessage(error, "预警处理失败"), "error");
  } finally {
    actionLoading.value = false;
  }
}

onMounted(loadWarnings);
</script>

<style scoped>
.hero {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
  margin-bottom: 14px;
}
.hero article {
  padding: 14px;
  background: linear-gradient(135deg, #002660, #003a8c);
  color: #fff;
}
.hero span {
  display: block;
  font-size: 12px;
  color: rgba(255, 255, 255, 0.8);
}
.hero strong {
  display: block;
  margin-top: 8px;
  font-size: 30px;
}
.filter-card,
.table-card {
  border: 1px solid #dbe3ee;
  background: #fff;
}
.filter-card {
  margin-bottom: 14px;
  padding: 14px;
}
.quick-tools {
  margin-bottom: 10px;
}
.filter-grid {
  display: grid;
  grid-template-columns: 1fr 1fr 1.2fr 1.2fr auto;
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
.ghost.active {
  background: #eef6ff;
  border-color: #bfdbfe;
  color: #1e3a8a;
}
.table-wrap {
  overflow: auto;
}
table {
  width: 100%;
  min-width: 1080px;
  border-collapse: collapse;
}
th,
td {
  padding: 12px 14px;
  border-bottom: 1px solid #eef2f7;
  font-size: 13px;
  text-align: left;
}
th {
  background: #f3f6fb;
  color: #64748b;
  font-size: 11px;
  font-weight: 800;
  text-transform: uppercase;
}
.mono {
  font-family: ui-monospace, SFMono-Regular, Menlo, Consolas, monospace;
  color: #1e3a8a;
  font-weight: 700;
}
.strong {
  margin: 0;
  color: #0f172a;
  font-weight: 700;
}
.sub {
  margin: 4px 0 0;
  color: #64748b;
  font-size: 12px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  max-width: 260px;
}
.level-pill,
.status-pill {
  display: inline-flex;
  min-height: 24px;
  align-items: center;
  justify-content: center;
  padding: 0 10px;
  border-radius: 999px;
  border: 1px solid transparent;
  font-size: 11px;
  font-weight: 800;
}
.level-pill.is-l1 { background: #fee2e2; color: #991b1b; }
.level-pill.is-l2 { background: #ffedd5; color: #9a3412; }
.status-pill.is-open { background: #fff4eb; color: #9b3a0a; border-color: #f8d5bf; }
.status-pill.is-processing { background: #ecfeff; color: #155e75; border-color: #a5f3fc; }
.status-pill.is-resolved { background: #dcfce7; color: #166534; border-color: #86efac; }
.status-pill.is-closed { background: #f1f5f9; color: #475569; border-color: #dbe3ee; }
.status-pill.is-unknown { background: #f8fafc; color: #64748b; border-color: #dbe3ee; }
.action-row,
.pager-actions {
  display: flex;
  gap: 8px;
}
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
.status-banner.is-success {
  border-color: #bbf7d0;
  background: #ecfdf5;
  color: #166534;
}
@media (max-width: 980px) {
  .hero { grid-template-columns: 1fr 1fr; }
  .filter-grid {
    grid-template-columns: 1fr 1fr;
  }
}
@media (max-width: 640px) {
  .hero, .filter-grid { grid-template-columns: 1fr; }
}
</style>
