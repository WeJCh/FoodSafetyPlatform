<template>
  <RegulatorEnforcerPageShell
    active-key="rectifications"
    title="整改跟进"
    subtitle="跟踪企业整改进度，并对已提交事项进行闭环确认。"
  >
    <section class="page-head">
      <div class="segmented">
        <button class="segment" :class="{ active: activePreset === 'all' }" type="button" @click="applyPreset('all')">全部任务</button>
        <button class="segment" :class="{ active: activePreset === 'review' }" type="button" @click="applyPreset('review')">待我确认</button>
        <button class="segment" :class="{ active: activePreset === 'overdue' }" type="button" @click="applyPreset('overdue')">已超期</button>
      </div>
      <button class="ghost" type="button" @click="openTrailDrawer">整改动态速览</button>
    </section>

    <section class="filter-grid">
      <label class="search-box">
        <span class="material-symbols-outlined">search</span>
        <input v-model.trim="filters.enterpriseName" placeholder="搜索企业名称或整改编号..." />
      </label>
      <label>
        <span>整改状态</span>
        <select v-model="filters.status">
          <option value="">所有整改状态</option>
          <option value="ONGOING">整改中</option>
          <option value="SUBMITTED">待复核</option>
          <option value="REWORK">打回重做</option>
          <option value="CONFIRMED">已确认</option>
        </select>
      </label>
      <div class="filter-actions">
        <button class="primary" type="button" :disabled="loading" @click="handleSearch">{{ loading ? "查询中..." : "查询" }}</button>
        <button class="ghost" type="button" :disabled="loading" @click="resetFilters">重置</button>
      </div>
    </section>

    <section class="content-grid">
      <div class="table-card">
        <div class="table-wrap">
          <table>
            <thead>
              <tr>
                <th>企业信息与整改摘要</th>
                <th>当前状态</th>
                <th>进度跟进</th>
                <th>截止日期</th>
                <th class="right">操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-if="!records.length && !loading">
                <td colspan="5" class="empty">暂无整改任务</td>
              </tr>
              <tr v-for="item in displayedRecords" :key="item.id">
                <td>
                  <div class="enterprise-cell">
                    <strong>{{ item.enterpriseName || "-" }}</strong>
                    <p :title="item.rectificationDesc || '-'">{{ item.rectificationDesc || "-" }}</p>
                  </div>
                </td>
                <td>
                  <span class="status-chip" :class="`is-${String(item.status || '').toLowerCase()}`">
                    {{ formatRectificationStatus(item.status) }}
                  </span>
                </td>
                <td>
                  <div class="progress-cell">
                    <div class="progress-meta">
                      <span :class="`sla sla--${rectificationSlaClass(item)}`">{{ formatRectificationSla(item) }}</span>
                      <span>{{ progressPercent(item) }}%</span>
                    </div>
                    <div class="progress-track">
                      <div class="progress-fill" :style="{ width: `${progressPercent(item)}%` }"></div>
                    </div>
                    <p class="progress-note">{{ item.progress || "企业暂未提交整改说明" }}</p>
                  </div>
                </td>
                <td>
                  <div class="deadline-cell">
                    <strong>{{ item.currentDeadline ? formatTime(item.currentDeadline).split(" ")[0] : "-" }}</strong>
                    <span>{{ rectificationDeadlineLabel(item) }}</span>
                  </div>
                </td>
                <td class="right">
                  <div class="row-actions">
                    <button class="ghost ghost--inline" type="button" @click="openDetail(item)">查看详情</button>
                    <button
                      v-if="item.status === 'SUBMITTED'"
                      class="primary primary--inline"
                      type="button"
                      :disabled="loading"
                      @click="handleConfirm(item)"
                    >
                      去核验
                    </button>
                  </div>
                </td>
              </tr>
            </tbody>
          </table>
        </div>

        <footer class="pager">
          <span>显示 {{ startIndex }} 到 {{ endIndex }} 项，共 {{ total }} 项整改任务</span>
          <div class="pager-actions">
            <button class="ghost" type="button" :disabled="page <= 1 || loading" @click="changePage(page - 1)">上一页</button>
            <button class="ghost" type="button" :disabled="page >= pages || loading" @click="changePage(page + 1)">下一页</button>
          </div>
        </footer>
      </div>
    </section>

    <transition name="trail-drawer">
      <div v-if="trailDrawerVisible" class="trail-overlay" @click.self="closeTrailDrawer">
        <aside class="trail-card trail-card--drawer">
          <div class="trail-card__head">
            <h3><span class="material-symbols-outlined">timeline</span>整改动态速览</h3>
            <button class="trail-close" type="button" @click="closeTrailDrawer" aria-label="关闭整改动态速览">
              <span class="material-symbols-outlined">close</span>
            </button>
          </div>
          <div class="trail-list">
            <article v-for="(item, index) in timelineRecords" :key="`${item.id || item.enterpriseName}-${index}`" class="trail-item">
              <span class="trail-dot" :class="`is-${trailTone(item)}`"></span>
              <div>
                <strong>{{ item.enterpriseName || "系统提醒" }}</strong>
                <p>{{ timelineText(item) }}</p>
                <small>{{ formatTime(item.updateTime || item.currentDeadline || item.createTime) }}</small>
              </div>
            </article>
            <div v-if="!timelineRecords.length" class="trail-empty">当前没有新的整改动态。</div>
          </div>
        </aside>
      </div>
    </transition>

    <div v-if="status.message" class="status-banner" :class="`is-${status.type}`">{{ status.message }}</div>
  </RegulatorEnforcerPageShell>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from "vue";
import { useRouter } from "vue-router";
import {
  confirmRectification,
  fetchMyRegulatorRectifications
} from "../../api/regulationOperation";
import RegulatorEnforcerPageShell from "./RegulatorEnforcerPageShell.vue";
import { formatTime } from "../../utils/formatters";
import { formatStatusLabel, rectificationStatusMap } from "../../utils/statusMaps";
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
const activePreset = ref("all");
const trailDrawerVisible = ref(false);
const status = reactive({ message: "", type: "info" });
const filters = reactive({ status: "", enterpriseName: "" });

const displayedRecords = computed(() => {
  if (activePreset.value === "review") {
    return records.value.filter((item) => item.status === "SUBMITTED");
  }
  if (activePreset.value === "overdue") {
    return records.value.filter((item) => item.slaStatus === "OVERDUE" && item.status !== "CONFIRMED");
  }
  return records.value;
});

const timelineRecords = computed(() => {
  const list = displayedRecords.value.slice();
  return list
    .sort((a, b) => String(b.updateTime || b.currentDeadline || "").localeCompare(String(a.updateTime || a.currentDeadline || "")))
    .slice(0, 4);
});

const startIndex = computed(() => (total.value && displayedRecords.value.length ? (page.value - 1) * size.value + 1 : 0));
const endIndex = computed(() => (page.value - 1) * size.value + displayedRecords.value.length);


function setStatus(message = "", type = "info") {
  status.message = message;
  status.type = type;
}

function formatRectificationStatus(value) {
  return formatStatusLabel(value, rectificationStatusMap);
}

function formatDurationMinutes(minutes) {
  const totalMins = Math.max(0, Number(minutes) || 0);
  const days = Math.floor(totalMins / (24 * 60));
  const hours = Math.floor((totalMins % (24 * 60)) / 60);
  const mins = totalMins % 60;
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
  if (item.slaStatus === "OVERDUE") return `逾期 ${formatDurationMinutes(Math.abs(remaining))}`;
  if (item.slaStatus === "DUE_SOON") return `临期 ${formatDurationMinutes(remaining)}`;
  if (item.slaStatus === "NORMAL") return `剩余 ${formatDurationMinutes(remaining)}`;
  if (item.currentDeadline) return `截止 ${formatTime(item.currentDeadline)}`;
  return "正常推进";
}

function progressPercent(item) {
  if (!item) return 0;
  if (item.status === "CONFIRMED") return 100;
  if (item.status === "SUBMITTED") return 100;
  if (item.status === "REWORK") return 25;
  if (item.status === "ONGOING") return 60;
  return 0;
}

function rectificationDeadlineLabel(item) {
  if (!item) return "-";
  if (item.slaStatus === "OVERDUE") return "逾期强制预警已触发";
  if (item.slaStatus === "DUE_SOON") return "临近整改时限";
  if (item.slaStatus === "NORMAL") return "按计划推进";
  if (item.status === "CONFIRMED") return "已完成闭环";
  return "-";
}

function trailTone(item) {
  if (item?.slaStatus === "OVERDUE") return "error";
  if (item?.status === "SUBMITTED") return "primary";
  if (item?.status === "ONGOING") return "secondary";
  return "muted";
}

function timelineText(item) {
  if (item?.status === "SUBMITTED") return "正式提交了整改结果申请，等待您核验。";
  if (item?.slaStatus === "OVERDUE") return `${item.enterpriseName || "该企业"}整改期限到期，企业未按时完成。`;
  return item?.progress || "整改任务正在推进中。";
}

async function loadRectifications() {
  loading.value = true;
  setStatus("");
  try {
    const data = await fetchMyRegulatorRectifications(token.value, {
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
    setStatus(resolveErrorMessage(error, "加载整改任务失败"), "error");
  } finally {
    loading.value = false;
  }
}

async function handleSearch() {
  page.value = 1;
  await loadRectifications();
}

function resetFilters() {
  filters.status = "";
  filters.enterpriseName = "";
  activePreset.value = "all";
  handleSearch();
}

async function changePage(nextPage) {
  page.value = nextPage;
  await loadRectifications();
}

function applyPreset(preset) {
  activePreset.value = preset;
}

function openTrailDrawer() {
  trailDrawerVisible.value = true;
}

function closeTrailDrawer() {
  trailDrawerVisible.value = false;
}

async function openDetail(item) {
  if (!item?.id) return;
  router.push({
    name: "regulator-enforcer-rectification-detail",
    params: { rectificationId: item.id }
  }).catch(() => {});
}

async function handleConfirm(item) {
  if (!item?.id) return;
  loading.value = true;
  setStatus("");
  try {
    await confirmRectification(token.value, item.id);
    setStatus("整改任务已确认闭环", "success");
    await loadRectifications();
  } catch (error) {
    setStatus(resolveErrorMessage(error, "整改确认失败"), "error");
  } finally {
    loading.value = false;
  }
}

onMounted(loadRectifications);
</script>

<style scoped>
.page-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  margin-bottom: 14px;
}
.segmented {
  display: inline-flex;
  gap: 6px;
  padding: 4px;
  background: #eceef1;
}
.segment,
.primary,
.ghost {
  min-height: 34px;
  padding: 0 14px;
  border: 1px solid #cbd5e1;
  font-size: 12px;
  font-weight: 700;
  cursor: pointer;
}
.segment {
  background: transparent;
  color: #64748b;
  border-color: transparent;
}
.segment.active {
  background: #fff;
  color: #002660;
  border-color: #dbe3ee;
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
.ghost--inline,
.primary--inline {
  min-height: 30px;
  padding: 0 10px;
  font-size: 11px;
}
.filter-grid {
  display: grid;
  grid-template-columns: 1.4fr 240px auto;
  gap: 12px;
  align-items: end;
  margin-bottom: 14px;
}
.search-box {
  position: relative;
}
.search-box span {
  position: absolute;
  left: 10px;
  top: 33px;
  color: #64748b;
  font-size: 18px;
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
  min-height: 38px;
  border: 1px solid #d4dce8;
  background: #fff;
  padding: 0 10px;
}
.search-box input {
  padding-left: 36px;
}
.filter-actions,
.pager-actions,
.row-actions {
  display: flex;
  gap: 8px;
}
.content-grid {
  display: block;
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
  min-width: 920px;
  border-collapse: collapse;
}
th,
td {
  padding: 14px;
  border-bottom: 1px solid #eef2f7;
  font-size: 13px;
  vertical-align: middle;
}
th {
  background: #f3f6fb;
  color: #64748b;
  font-size: 11px;
  font-weight: 800;
  text-transform: uppercase;
  text-align: left;
}
.right {
  text-align: right;
}
.enterprise-cell strong {
  display: block;
  color: #002660;
}
.enterprise-cell p,
.progress-note {
  margin: 4px 0 0;
  color: #64748b;
  font-size: 12px;
  line-height: 1.5;
}
.status-chip {
  display: inline-flex;
  min-height: 24px;
  align-items: center;
  padding: 0 10px;
  border-radius: 2px;
  font-size: 11px;
  font-weight: 800;
}
.status-chip.is-ongoing { background: #dbeafe; color: #1e3a8a; }
.status-chip.is-submitted { background: #c9d7fe; color: #003a8c; }
.status-chip.is-rework { background: #fee2e2; color: #b91c1c; }
.status-chip.is-confirmed { background: #dcfce7; color: #166534; }
.progress-cell {
  min-width: 180px;
}
.progress-meta {
  display: flex;
  justify-content: space-between;
  gap: 8px;
  margin-bottom: 6px;
  font-size: 11px;
}
.sla {
  font-weight: 700;
}
.sla--normal { color: #0d4f9b; }
.sla--warning { color: #b36b00; }
.sla--overdue { color: #b91c1c; }
.sla--none { color: #64748b; }
.progress-track {
  height: 6px;
  background: #e2e8f0;
  overflow: hidden;
}
.progress-fill {
  height: 100%;
  background: #2563eb;
}
.deadline-cell strong {
  display: block;
  color: #0f172a;
}
.deadline-cell span {
  display: block;
  margin-top: 4px;
  color: #64748b;
  font-size: 12px;
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
.trail-overlay {
  position: fixed;
  inset: 0;
  z-index: 40;
  background: rgba(15, 23, 42, 0.18);
}
.trail-card {
  border: 1px solid #dbe3ee;
  background: #fff;
}
.trail-card--drawer {
  position: absolute;
  top: 0;
  right: 0;
  width: min(380px, 92vw);
  height: 100vh;
  padding: 20px 18px;
  display: grid;
  align-content: start;
  gap: 16px;
  box-shadow: -18px 0 40px rgba(15, 23, 42, 0.12);
  overflow-y: auto;
}
.trail-card__head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
}
.trail-card h3 {
  margin: 0;
  display: flex;
  align-items: center;
  gap: 8px;
  color: #002660;
  font-size: 15px;
}
.trail-close {
  width: 34px;
  height: 34px;
  border: 1px solid #dbe3ee;
  background: #fff;
  color: #334155;
  cursor: pointer;
}
.trail-list {
  display: grid;
  gap: 16px;
}
.trail-item {
  position: relative;
  padding-left: 18px;
}
.trail-dot {
  position: absolute;
  left: 0;
  top: 4px;
  width: 10px;
  height: 10px;
  border-radius: 999px;
}
.trail-dot.is-error { background: #ba1a1a; }
.trail-dot.is-primary { background: #003a8c; }
.trail-dot.is-secondary { background: #64748b; }
.trail-dot.is-muted { background: #cbd5e1; }
.trail-item strong {
  display: block;
  color: #0f172a;
  font-size: 13px;
}
.trail-item p {
  margin: 4px 0 0;
  color: #64748b;
  font-size: 12px;
  line-height: 1.6;
}
.trail-item small {
  display: block;
  margin-top: 6px;
  color: #94a3b8;
  font-size: 11px;
}
.trail-empty {
  color: #94a3b8;
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
.trail-drawer-enter-active,
.trail-drawer-leave-active {
  transition: opacity 0.24s ease;
}
.trail-drawer-enter-active .trail-card--drawer,
.trail-drawer-leave-active .trail-card--drawer {
  transition: transform 0.24s ease;
}
.trail-drawer-enter-from,
.trail-drawer-leave-to {
  opacity: 0;
}
.trail-drawer-enter-from .trail-card--drawer,
.trail-drawer-leave-to .trail-card--drawer {
  transform: translateX(100%);
}
@media (max-width: 860px) {
  .filter-grid {
    grid-template-columns: 1fr;
  }
  .page-head {
    flex-direction: column;
    align-items: stretch;
  }
}
</style>
