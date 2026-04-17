<template>
  <RegulatorEnforcerPageShell
    active-key="complaints"
    title="投诉处理"
    subtitle="查看待处理投诉、进入详情并完成执法反馈。"
  >
    <section class="filter-card">
      <div class="filter-grid">
        <label>
          投诉状态
          <select v-model="filters.status">
            <option value="">全部状态</option>
            <option value="SUBMITTED">已提交</option>
            <option value="PENDING">已受理</option>
            <option value="ASSIGNED">已派发</option>
            <option value="PROCESSING">处理中</option>
            <option value="FEEDBACKED">已反馈</option>
            <option value="REJECTED">已驳回</option>
          </select>
        </label>
        <label>
          企业名称
          <input v-model.trim="filters.enterpriseName" placeholder="输入企业名称" />
        </label>
      </div>
      <div class="filter-actions">
        <button class="primary" type="button" :disabled="loading" @click="handleSearch">
          {{ loading ? "查询中..." : "查询" }}
        </button>
        <button class="ghost" type="button" :disabled="loading" @click="resetFilters">重置</button>
      </div>
    </section>

    <section class="table-card">
      <div class="table-wrap">
        <table>
          <thead>
            <tr>
              <th>投诉编号</th>
              <th>涉及企业</th>
              <th>投诉时间</th>
              <th>当前状态</th>
              <th>办理时限</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-if="!records.length && !loading">
              <td colspan="6" class="empty">暂无投诉记录</td>
            </tr>
            <tr v-for="item in records" :key="item.id">
              <td class="mono">{{ item.complaintNo || `#${item.id}` }}</td>
              <td>
                <p class="strong">{{ item.enterpriseName || "-" }}</p>
                <p class="sub">{{ summarize(item.content) }}</p>
              </td>
              <td>{{ formatTime(item.createTime || item.updateTime) }}</td>
              <td>
                <span class="status-pill" :class="statusClass(item.status)">
                  {{ formatComplaintStatus(item.status) }}
                </span>
              </td>
              <td>{{ formatTime(item.deadlineTime) }}</td>
              <td>
                <div class="action-row">
                  <button class="ghost" type="button" @click="openDetail(item)">查看详情</button>
                  <button
                    v-if="item.status === 'ASSIGNED'"
                    class="primary"
                    type="button"
                    :disabled="loading"
                    @click="handleStart(item)"
                  >
                    开始处理
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

    <section class="stats-grid">
      <article class="stat-card">
        <span>待开始</span>
        <strong>{{ assignedCount }}</strong>
      </article>
      <article class="stat-card">
        <span>处理中</span>
        <strong>{{ processingCount }}</strong>
      </article>
      <article class="stat-card is-accent">
        <span>已反馈</span>
        <strong>{{ feedbackedCount }}</strong>
      </article>
    </section>

    <div v-if="status.message" class="status-banner" :class="`is-${status.type}`">{{ status.message }}</div>
  </RegulatorEnforcerPageShell>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from "vue";
import { useRouter } from "vue-router";
import { fetchComplaints, startComplaintProcess } from "../../api/complaint";
import RegulatorEnforcerPageShell from "./RegulatorEnforcerPageShell.vue";
import { formatByMap, formatTime } from "../../utils/formatters";
import { complaintStatusMap } from "../../utils/statusMaps";
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
const filters = reactive({ status: "", enterpriseName: "" });

const assignedCount = computed(() => records.value.filter((item) => item.status === "ASSIGNED").length);
const processingCount = computed(() => records.value.filter((item) => item.status === "PROCESSING").length);
const feedbackedCount = computed(() => records.value.filter((item) => item.status === "FEEDBACKED").length);

function setStatus(message = "", type = "info") {
  status.message = message;
  status.type = type;
}

function formatComplaintStatus(value) {
  return formatByMap(value, complaintStatusMap);
}

function summarize(value) {
  const text = String(value || "").trim();
  if (!text) return "暂无投诉摘要";
  if (text.length <= 28) return text;
  return `${text.slice(0, 28)}...`;
}

function statusClass(value) {
  if (value === "ASSIGNED") return "is-assigned";
  if (value === "PROCESSING") return "is-processing";
  if (value === "FEEDBACKED") return "is-feedbacked";
  if (value === "REJECTED") return "is-rejected";
  if (value === "PENDING") return "is-pending";
  return "is-default";
}

async function loadComplaints() {
  loading.value = true;
  setStatus("");
  try {
    const data = await fetchComplaints(token.value, {
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
    setStatus(error.message || "加载投诉列表失败", "error");
  } finally {
    loading.value = false;
  }
}

async function handleSearch() {
  page.value = 1;
  await loadComplaints();
}

function resetFilters() {
  filters.status = "";
  filters.enterpriseName = "";
  handleSearch();
}

async function changePage(nextPage) {
  page.value = nextPage;
  await loadComplaints();
}

function openDetail(item) {
  if (!item?.id) return;
  router.push({
    name: "regulator-enforcer-complaint-detail",
    params: { complaintId: item.id },
    query: { from: "complaints" }
  }).catch(() => {});
}

async function handleStart(item) {
  if (!item?.id) return;
  loading.value = true;
  setStatus("");
  try {
    await startComplaintProcess(token.value, item.id);
    setStatus("投诉已开始处理", "success");
    await loadComplaints();
  } catch (error) {
    setStatus(error.message || "开始处理失败", "error");
  } finally {
    loading.value = false;
  }
}

onMounted(loadComplaints);
</script>

<style scoped>
.filter-card {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: end;
  margin-bottom: 14px;
  padding: 14px;
  border: 1px solid #dbe3ee;
  background: #fff;
}
.filter-grid {
  flex: 1;
  display: grid;
  grid-template-columns: 1fr 1.4fr;
  gap: 12px;
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
  background: #f8fafc;
  padding: 0 10px;
}
.filter-actions,
.action-row,
.pager-actions {
  display: flex;
  gap: 8px;
}
.primary,
.ghost {
  min-height: 34px;
  padding: 0 14px;
  border: 1px solid #cbd5e1;
  cursor: pointer;
  font-size: 12px;
  font-weight: 700;
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
  min-width: 980px;
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
}
.status-pill {
  display: inline-flex;
  min-height: 24px;
  align-items: center;
  padding: 0 10px;
  border-radius: 999px;
  border: 1px solid transparent;
  font-size: 11px;
  font-weight: 800;
}
.status-pill.is-assigned { color: #1e3a8a; background: #dbeafe; border-color: #bfdbfe; }
.status-pill.is-processing { color: #155e75; background: #ecfeff; border-color: #a5f3fc; }
.status-pill.is-feedbacked { color: #166534; background: #dcfce7; border-color: #86efac; }
.status-pill.is-rejected { color: #991b1b; background: #fee2e2; border-color: #fecaca; }
.status-pill.is-pending { color: #9a3412; background: #ffedd5; border-color: #fdba74; }
.status-pill.is-default { color: #475569; background: #f1f5f9; border-color: #dbe3ee; }
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
.stats-grid {
  margin-top: 14px;
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
}
.stat-card {
  padding: 14px;
  border: 1px solid #dbe3ee;
  background: #fff;
}
.stat-card span {
  display: block;
  font-size: 12px;
  color: #64748b;
}
.stat-card strong {
  display: block;
  margin-top: 8px;
  font-size: 28px;
  color: #0f172a;
}
.stat-card.is-accent {
  background: #003b94;
  border-color: #003b94;
}
.stat-card.is-accent span,
.stat-card.is-accent strong {
  color: #fff;
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
@media (max-width: 900px) {
  .filter-card,
  .filter-grid,
  .stats-grid {
    grid-template-columns: 1fr;
    display: grid;
  }
}
</style>
