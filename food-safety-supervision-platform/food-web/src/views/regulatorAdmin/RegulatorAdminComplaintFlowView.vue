<template>
  <RegulatorAdminWorkspacePage
    active-key="complaints"
    :username="regulatorUser.username"
    @navigate="handleSidebarNavigate"
    @logout="handleLogout"
    @pending-feature="onPendingFeature"
  >
    <section class="complaint-page">
      <header class="complaint-page__head">
        <div>
          <h1>投诉流转中心</h1>
          <p>受理、指派及反馈全流程数字化闭环管理。</p>
        </div>
      </header>

      <section class="filter-card">
        <div class="filter-toolbar">
          <div class="filter-grid">
            <label>
              投诉类型
              <select v-model="filters.complaintType">
                <option value="">全部类型</option>
                <option v-for="option in complaintTypeOptions" :key="option.value" :value="option.value">
                  {{ option.label }}
                </option>
              </select>
            </label>
            <label>
              当前状态
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
          </div>
          <div class="filter-actions">
            <button class="primary" type="button" :disabled="loading" @click="handleSearch">
              {{ loading ? "查询中..." : "执行筛选" }}
            </button>
            <button class="ghost" type="button" :disabled="loading" @click="resetFilters">重置</button>
          </div>
        </div>
      </section>

      <section class="table-card">
        <div class="table-wrap">
          <table>
            <thead>
              <tr>
                <th>投诉时间</th>
                <th>编号</th>
                <th>涉及企业</th>
                <th>投诉简述</th>
                <th>责任人</th>
                <th>状态</th>
                <th class="th-right">操作项</th>
              </tr>
            </thead>
            <tbody v-if="displayRecords.length">
              <tr v-for="item in displayRecords" :key="item.id">
                <td>{{ formatTime(item.createTime || item.updateTime) }}</td>
                <td class="mono">{{ item.complaintNo || "-" }}</td>
                <td class="strong">{{ item.enterpriseName || "-" }}</td>
                <td class="summary" :title="item.content || '-'">{{ summaryText(item.content) }}</td>
                <td>
                  <span v-if="item.assignedToName">{{ item.assignedToName }}</span>
                  <span v-else class="muted">未分配</span>
                </td>
                <td>
                  <span class="status-pill" :class="statusClass(item.status)">
                    {{ formatComplaintStatus(item.status) }}
                  </span>
                </td>
                <td class="td-right">
                  <div class="action-row">
                    <button class="ghost" type="button" @click="handleViewDetail(item)">详情</button>
                    <button
                      v-if="item.status === 'SUBMITTED'"
                      class="primary"
                      type="button"
                      :disabled="loading"
                      @click="handleAccept(item)"
                    >
                      受理
                    </button>
                  </div>
                </td>
              </tr>
            </tbody>
          </table>
          <div v-if="!records.length" class="empty">暂无投诉记录</div>
        </div>
        <div class="pager">
          <span>共 {{ total }} 条，{{ page }}/{{ pages }} 页</span>
          <div class="pager-actions">
            <button class="ghost" type="button" :disabled="page <= 1 || loading" @click="changePage(page - 1)">上一页</button>
            <button class="ghost" type="button" :disabled="page >= pages || loading" @click="changePage(page + 1)">下一页</button>
          </div>
        </div>
      </section>

      <section class="stats-bento">
        <article class="progress-card">
          <h3>流转处理进度分布</h3>
          <div class="flow-track">
            <div v-for="item in progressStats" :key="item.key" class="flow-node">
              <span class="dot">{{ item.count }}</span>
              <em>{{ item.label }}</em>
            </div>
          </div>
        </article>
      </section>

      <div v-if="status.message" class="status" :class="status.type">{{ status.message }}</div>
    </section>
  </RegulatorAdminWorkspacePage>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from "vue";
import { useRouter } from "vue-router";
import { acceptComplaint, fetchComplaints } from "../../api/complaint";
import RegulatorAdminWorkspacePage from "../../components/regulatorAdmin/RegulatorAdminWorkspacePage.vue";
import { formatTime } from "../../utils/formatters";
import { complaintStatusMap } from "../../utils/statusMaps";
import { regulatorFeaturePendingNotice, useRegulatorAdminShellSession } from "./regulatorAdminShared";

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
  complaintType: "",
  status: "",
});

function onPendingFeature(title) {
  regulatorFeaturePendingNotice(title);
}

function setStatus(message, type = "info") {
  status.message = message;
  status.type = type;
}

function formatComplaintStatus(value) {
  return complaintStatusMap[value] || value || "-";
}

function summaryText(content) {
  const text = String(content || "").trim();
  if (!text) return "-";
  if (text.length <= 26) return text;
  return `${text.slice(0, 26)}...`;
}

function statusClass(value) {
  if (value === "SUBMITTED") return "is-submitted";
  if (value === "PENDING") return "is-pending";
  if (value === "ASSIGNED") return "is-assigned";
  if (value === "PROCESSING") return "is-processing";
  if (value === "FEEDBACKED") return "is-feedbacked";
  if (value === "REJECTED") return "is-rejected";
  return "";
}

const complaintTypeLabelMap = {
  FOOD_SAFETY: "食品安全",
  FOOD_SPOILAGE: "食品变质",
  FALSE_AD: "虚假宣传",
  HYGIENE: "卫生环境",
  PRICE: "价格违规",
  OTHER: "其他"
};

function formatComplaintType(value) {
  return complaintTypeLabelMap[value] || value || "其他";
}

const complaintTypeOptions = computed(() => {
  const set = new Set();
  records.value.forEach((item) => {
    const value = String(item.complaintType || "").trim();
    if (value) set.add(value);
  });
  return Array.from(set).map((value) => ({ value, label: formatComplaintType(value) }));
});

const displayRecords = computed(() => {
  const type = String(filters.complaintType || "").trim();
  if (!type) return records.value;
  return records.value.filter((item) => String(item.complaintType || "").trim() === type);
});

const progressStats = computed(() => {
  const stats = {
    SUBMITTED: 0,
    PENDING: 0,
    ASSIGNED: 0,
    PROCESSING: 0,
    FEEDBACKED: 0
  };
  records.value.forEach((item) => {
    const key = item.status;
    if (stats[key] !== undefined) stats[key] += 1;
  });
  return [
    { key: "SUBMITTED", label: "提交", count: stats.SUBMITTED },
    { key: "PENDING", label: "受理", count: stats.PENDING },
    { key: "ASSIGNED", label: "指派", count: stats.ASSIGNED },
    { key: "PROCESSING", label: "处理", count: stats.PROCESSING },
    { key: "FEEDBACKED", label: "反馈", count: stats.FEEDBACKED }
  ];
});

async function loadComplaints() {
  loading.value = true;
  setStatus("");
  try {
    const data = await fetchComplaints(token.value, {
      status: filters.status,
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
  filters.complaintType = "";
  filters.status = "";
  handleSearch();
}

async function changePage(nextPage) {
  page.value = nextPage;
  await loadComplaints();
}

function handleViewDetail(item) {
  if (!item?.id) return;
  router.push({ name: "regulator-admin-complaint-detail", params: { complaintId: item.id } }).catch(() => {});
}

async function handleAccept(item) {
  if (!item?.id) return;
  loading.value = true;
  setStatus("");
  try {
    await acceptComplaint(token.value, item.id);
    setStatus("投诉已受理", "success");
    await loadComplaints();
  } catch (error) {
    setStatus(error.message || "投诉受理失败", "error");
  } finally {
    loading.value = false;
  }
}

onMounted(loadComplaints);
</script>

<style scoped>
.complaint-page { display: grid; gap: 16px; }
.complaint-page__head h1 { margin: 0; color: #002660; font-size: 30px; font-weight: 900; letter-spacing: -0.02em; }
.complaint-page__head p { margin: 6px 0 0; color: #64748b; font-size: 14px; }

.filter-card {
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  padding: 14px;
}
.filter-toolbar {
  display: flex;
  align-items: stretch;
  gap: 14px;
  flex-wrap: wrap;
}
.filter-grid {
  flex: 1 1 540px;
  min-width: 0;
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px 14px;
  padding: 6px 2px;
}
.filter-grid label { display: grid; gap: 7px; font-size: 12px; font-weight: 800; color: #475569; }
.filter-grid input, .filter-grid select {
  border: 1px solid #d6dee8;
  background: #fff;
  border-radius: 8px;
  min-height: 38px;
  padding: 0 12px;
  font-size: 13px;
  color: #0f172a;
}
.filter-grid input:focus, .filter-grid select:focus {
  outline: none;
  border-color: #003a8c;
  box-shadow: 0 0 0 3px rgba(0, 58, 140, 0.1);
}
.filter-actions {
  flex: 0 0 auto;
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 2px;
}

.primary { border: 0; background: #002660; color: #fff; border-radius: 8px; padding: 10px 18px; font-size: 13px; font-weight: 700; cursor: pointer; }
.ghost { border: 1px solid #d1d5db; background: #fff; color: #334155; border-radius: 8px; padding: 10px 16px; font-size: 13px; cursor: pointer; }

.table-card { background: #fff; border: 1px solid #e2e8f0; border-radius: 12px; overflow: hidden; }
.table-wrap { overflow: auto; }
table { width: 100%; min-width: 1080px; border-collapse: collapse; }
thead tr { background: #e6e8eb; }
th { text-align: left; padding: 12px 14px; font-size: 11px; font-weight: 900; letter-spacing: 0.05em; text-transform: uppercase; color: #64748b; }
td { padding: 14px; border-top: 1px solid #f1f5f9; font-size: 13px; color: #1e293b; }
tbody tr:nth-child(even) { background: #f8fafc; }
tbody tr:hover { background: #f1f5f9; }
.mono { font-family: ui-monospace, SFMono-Regular, Menlo, Consolas, monospace; color: #1e3a8a; font-weight: 700; }
.strong { font-weight: 700; color: #0f172a; }
.summary { max-width: 280px; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.muted { color: #94a3b8; font-style: italic; }
.th-right, .td-right { text-align: right; }
.action-row { display: inline-flex; align-items: center; gap: 8px; }

.status-pill {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: 24px;
  padding: 0 10px;
  border-radius: 999px;
  font-size: 10px;
  font-weight: 800;
  border: 1px solid transparent;
}
.status-pill.is-submitted { color: #1e3a8a; background: #dbeafe; border-color: #bfdbfe; }
.status-pill.is-pending { color: #9a3412; background: #ffedd5; border-color: #fed7aa; }
.status-pill.is-assigned { color: #0f766e; background: #ccfbf1; border-color: #99f6e4; }
.status-pill.is-processing { color: #7c2d12; background: #ffedd5; border-color: #fed7aa; }
.status-pill.is-feedbacked { color: #166534; background: #dcfce7; border-color: #bbf7d0; }
.status-pill.is-rejected { color: #991b1b; background: #fee2e2; border-color: #fecaca; }

.empty { padding: 18px; text-align: center; color: #64748b; font-size: 13px; }
.pager {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 10px;
  padding: 12px 14px;
  border-top: 1px solid #e2e8f0;
  color: #64748b;
  font-size: 12px;
  font-weight: 700;
}
.pager-actions { display: flex; gap: 8px; }

.stats-bento { display: grid; grid-template-columns: minmax(0, 1fr); gap: 12px; }
.progress-card {
  background: #fff;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  padding: 16px;
}
.progress-card h3 { margin: 0 0 14px; font-size: 15px; color: #002660; }
.flow-track { display: flex; justify-content: space-between; gap: 8px; position: relative; padding: 12px 0 0; }
.flow-track::before {
  content: "";
  position: absolute;
  top: 27px;
  left: 0;
  right: 0;
  height: 2px;
  background: #e2e8f0;
}
.flow-node { position: relative; z-index: 1; display: grid; justify-items: center; gap: 6px; flex: 1; }
.dot {
  width: 34px;
  height: 34px;
  border-radius: 50%;
  background: #002660;
  color: #fff;
  font-size: 12px;
  font-weight: 800;
  display: grid;
  place-items: center;
}
.flow-node em { font-style: normal; font-size: 11px; color: #334155; font-weight: 700; }

.status { position: fixed; right: 18px; bottom: 18px; border-radius: 8px; padding: 10px 12px; color: #fff; background: #0f172a; font-size: 13px; z-index: 1200; }
.status.error { background: #b91c1c; }
.status.success { background: #166534; }

@media (max-width: 1100px) {
  .filter-toolbar { align-items: stretch; gap: 10px; }
  .filter-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); }
  .stats-bento { grid-template-columns: 1fr; }
}
@media (max-width: 760px) {
  .filter-grid { grid-template-columns: 1fr; }
  .filter-actions { justify-content: stretch; width: 100%; }
  .filter-actions button { flex: 1; }
}
</style>
