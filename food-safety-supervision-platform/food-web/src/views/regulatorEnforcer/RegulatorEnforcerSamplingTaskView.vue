<template>
  <RegulatorEnforcerPageShell
    active-key="sampling"
    title="我的抽检任务"
    subtitle="查看抽检任务、进入详情页核对样品与企业信息，并在详情页完成结果录入。"
  >
    <section class="filter-card">
      <div class="filter-grid">
        <label>
          任务状态
          <select v-model="filters.status">
            <option value="">全部状态</option>
            <option value="ASSIGNED">待抽检</option>
            <option value="COMPLETED">已完成</option>
            <option value="CLOSED">已归档</option>
          </select>
        </label>
      </div>
      <div class="filter-actions">
        <button class="primary" type="button" :disabled="loading" @click="handleSearch">
          {{ loading ? "查询中..." : "查询" }}
        </button>
      </div>
    </section>

    <section class="table-card">
      <div class="table-wrap">
        <table>
          <thead>
            <tr>
              <th>任务编号</th>
              <th>企业名称</th>
              <th>抽检产品</th>
              <th>状态</th>
              <th>截止时间</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-if="!records.length && !loading">
              <td colspan="6" class="empty">暂无抽检任务</td>
            </tr>
            <tr v-for="task in records" :key="task.id">
              <td class="mono">{{ task.taskNo || `#${task.id}` }}</td>
              <td class="strong">{{ task.enterpriseName || "-" }}</td>
              <td>
                <div class="product-cell">
                  <strong>{{ task.productName || "-" }}</strong>
                  <span>{{ task.productSpecification || "暂无规格" }}</span>
                </div>
              </td>
              <td><span class="status-pill">{{ formatSamplingTaskStatus(task.status) }}</span></td>
              <td>{{ formatTime(task.deadline) }}</td>
              <td>
                <div class="action-row">
                  <button class="ghost" type="button" @click="openTaskDetail(task)">查看详情</button>
                  <button
                    v-if="task.status === 'ASSIGNED' || task.status === 'IN_PROGRESS'"
                    class="primary"
                    type="button"
                    @click="openResultEntry(task)"
                  >
                    录入结果
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
import { onMounted, reactive, ref } from "vue";
import { useRouter } from "vue-router";
import { fetchMySamplingTasks } from "../../api/regulationOperation";
import RegulatorEnforcerPageShell from "./RegulatorEnforcerPageShell.vue";
import { formatTime } from "../../utils/formatters";
import { useRegulatorEnforcerShellSession } from "./regulatorEnforcerShared";

const { token } = useRegulatorEnforcerShellSession();
const router = useRouter();

const loading = ref(false);
const records = ref([]);
const page = ref(1);
const size = ref(8);
const total = ref(0);
const pages = ref(1);
const status = reactive({ message: "", type: "info" });
const filters = reactive({ status: "" });

const samplingTaskStatusMap = {
  CREATED: "待派发",
  ASSIGNED: "待抽检",
  IN_PROGRESS: "抽检中",
  COMPLETED: "已完成",
  CLOSED: "已归档"
};

function setStatus(message = "", type = "info") {
  status.message = message;
  status.type = type;
}

function formatSamplingTaskStatus(value) {
  return samplingTaskStatusMap[value] || value || "-";
}

async function loadSamplingTasks() {
  loading.value = true;
  setStatus("");
  try {
    const data = await fetchMySamplingTasks(token.value, {
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
    setStatus(error.message || "加载抽检任务失败", "error");
  } finally {
    loading.value = false;
  }
}

async function handleSearch() {
  page.value = 1;
  await loadSamplingTasks();
}

async function changePage(nextPage) {
  page.value = nextPage;
  await loadSamplingTasks();
}

function openTaskDetail(task) {
  if (!task?.id) return;
  router.push({
    name: "regulator-enforcer-sampling-detail",
    params: { taskId: task.id }
  }).catch(() => {});
}

function openResultEntry(task) {
  if (!task?.id) return;
  router.push({
    name: "regulator-enforcer-sampling-submit",
    params: { taskId: task.id }
  }).catch(() => {});
}

onMounted(loadSamplingTasks);
</script>

<style scoped>
.filter-card,
.table-card {
  border: 1px solid #dbe3ee;
  background: #fff;
}
.filter-card {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: end;
  padding: 14px;
  margin-bottom: 14px;
}
.filter-grid {
  display: grid;
  grid-template-columns: 240px;
  gap: 10px;
}
.filter-grid label {
  display: grid;
  gap: 6px;
  font-size: 12px;
  color: #475569;
  font-weight: 700;
}
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
.table-wrap {
  overflow: auto;
}
table {
  width: 100%;
  min-width: 940px;
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
  color: #0f3a72;
  font-weight: 700;
}
.product-cell {
  display: grid;
  gap: 4px;
}
.product-cell strong {
  color: #0f172a;
}
.product-cell span {
  color: #64748b;
  font-size: 12px;
}
.status-pill {
  display: inline-flex;
  min-height: 24px;
  align-items: center;
  padding: 0 10px;
  border-radius: 999px;
  background: #f1f5f9;
  color: #475569;
  font-size: 11px;
  font-weight: 800;
}
.action-row,
.filter-actions,
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
@media (max-width: 760px) {
  .filter-card {
    display: grid;
  }
}
</style>
