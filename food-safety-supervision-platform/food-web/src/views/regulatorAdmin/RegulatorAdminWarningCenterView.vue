<template>
  <RegulatorAdminWorkspacePage
    active-key="warnings"
    :username="regulatorUser.username"
    @navigate="handleSidebarNavigate"
    @logout="handleLogout"
  >
    <section class="warning-page">
      <header class="hero">
        <div>
          <p class="hero-eyebrow">Risk Intelligence Center</p>
          <h1>风险预警中心</h1>
          <p class="hero-desc">实时监控食品供应链风险信号，支持快速筛选、处置与溯源。</p>
        </div>
        <div class="hero-metrics">
          <article>
            <span>当前活跃预警</span>
            <strong>{{ total }}</strong>
          </article>
          <article>
            <span>待处理</span>
            <strong>{{ openCount }}</strong>
          </article>
        </div>
      </header>

      <section class="filter-card">
        <div class="filter-row">
          <div class="filter-block">
            <label>预警等级</label>
            <div class="chip-group">
              <button type="button" :class="['chip', { active: filters.level === '' }]" @click="setLevel('')">全部级别</button>
              <button type="button" :class="['chip', { active: filters.level === 'L1' }]" @click="setLevel('L1')">L1 高风险</button>
              <button type="button" :class="['chip', { active: filters.level === 'L2' }]" @click="setLevel('L2')">L2 一般风险</button>
            </div>
          </div>
          <div class="filter-block">
            <label>预警类型</label>
            <select v-model="filters.warningType">
              <option value="">全部类型</option>
              <option v-for="item in warningTypeOptions" :key="item" :value="item">{{ item }}</option>
            </select>
          </div>
          <div class="filter-block">
            <label>当前状态</label>
            <div class="chip-group">
              <button type="button" :class="['chip', { active: filters.status === '' }]" @click="setStatusFilter('')">全部</button>
              <button type="button" :class="['chip', { active: filters.status === 'OPEN' }]" @click="setStatusFilter('OPEN')">待处理</button>
              <button
                type="button"
                :class="['chip', { active: filters.status === 'PROCESSING' }]"
                @click="setStatusFilter('PROCESSING')"
              >
                处置中
              </button>
              <button type="button" :class="['chip', { active: filters.status === 'RESOLVED' }]" @click="setStatusFilter('RESOLVED')">已解决</button>
            </div>
          </div>
          <button class="advanced-btn" type="button" @click="advancedVisible = !advancedVisible">
            {{ advancedVisible ? "收起筛选" : "高级筛选" }}
          </button>
        </div>
        <div v-if="advancedVisible" class="advanced-row">
          <label>
            业务类型
            <input v-model.trim="filters.bizType" placeholder="例：RECTIFICATION" />
          </label>
          <label>
            关键词
            <input v-model.trim="filters.keyword" placeholder="标题或内容关键词" />
          </label>
          <div class="advanced-actions">
            <button class="primary" type="button" :disabled="loading || actionLoading" @click="handleSearch">
              {{ loading ? "查询中..." : "查询" }}
            </button>
            <button class="ghost" type="button" :disabled="loading || actionLoading" @click="resetFilters">重置</button>
          </div>
        </div>
      </section>

      <section class="table-card">
        <div class="table-wrap">
          <table>
            <thead>
              <tr>
                <th>预警触发时间</th>
                <th class="center">级别</th>
                <th>关联对象</th>
                <th>预警原因摘要</th>
                <th>当前状态</th>
                <th>处理人</th>
                <th class="right">操作项</th>
              </tr>
            </thead>
            <tbody v-if="records.length">
              <tr v-for="item in records" :key="item.id">
                <td class="mono">{{ formatTime(item.lastOccurTime || item.createTime) }}</td>
                <td class="center">
                  <span class="level-pill" :class="`is-${String(item.level || '').toLowerCase()}`">
                    {{ formatWarningLevel(item.level) }}
                  </span>
                </td>
                <td>
                  <p class="obj-main">{{ item.bizName || item.title || "-" }}</p>
                  <p class="obj-sub">{{ item.bizType || "-" }}</p>
                </td>
                <td class="reason">
                  <p class="reason-title">{{ item.title || "-" }}</p>
                  <p :title="item.content || '-'">{{ item.content || "-" }}</p>
                </td>
                <td>
                  <span class="status-pill" :class="`is-${warningStatusClass(item.status)}`">{{ formatWarningStatus(item.status) }}</span>
                </td>
                <td>{{ item.assignedToName || item.ownerName || "尚未指派" }}</td>
                <td>
                  <div class="action-row">
                    <button class="ghost action-btn" type="button" @click="openWarningDetail(item)">详情</button>
                    <button
                      v-if="warningQuickAction(item.status)"
                      class="primary action-btn"
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
          <div v-if="!records.length" class="empty">暂无预警记录</div>
        </div>

        <div class="pager">
          <span>共 {{ total }} 条，{{ page }}/{{ pages }} 页</span>
          <div class="pager-actions">
            <button class="ghost" type="button" :disabled="page <= 1 || loading" @click="changePage(page - 1)">上一页</button>
            <button class="ghost" type="button" :disabled="page >= pages || loading" @click="changePage(page + 1)">下一页</button>
          </div>
        </div>
      </section>

      <div v-if="warningDetailVisible" class="modal-mask" @click.self="closeWarningDetail">
        <div class="modal-card">
          <div class="modal-title">预警详情</div>
          <div class="modal-body">
            <div v-if="warningDetailLoading" class="modal-empty">详情加载中...</div>
            <template v-else-if="warningDetail">
              <div class="summary-grid">
                <div class="summary-item"><span>预警编号</span><strong>{{ warningDetail.warningNo || "-" }}</strong></div>
                <div class="summary-item"><span>状态</span><strong>{{ formatWarningStatus(warningDetail.status) }}</strong></div>
                <div class="summary-item"><span>等级</span><strong>{{ formatWarningLevel(warningDetail.level) }}</strong></div>
                <div class="summary-item"><span>触发次数</span><strong>{{ warningDetail.triggerCount || 0 }}</strong></div>
              </div>
              <div class="field"><span>预警标题</span><strong>{{ warningDetail.title || "-" }}</strong></div>
              <div class="field"><span>预警内容</span><strong>{{ warningDetail.content || "-" }}</strong></div>
              <div class="field">
                <span>处理记录</span>
                <div class="timeline-list">
                  <div v-if="!warningDetail.processLogs?.length" class="modal-empty">暂无处理记录</div>
                  <div v-for="log in warningDetail.processLogs || []" :key="log.id" class="timeline-item">
                    <div class="timeline-main">
                      <div class="timeline-head">
                        <strong>{{ formatWarningAction(log.actionType) }}</strong>
                        <time>{{ formatTime(log.createTime) }}</time>
                      </div>
                      <p>操作人：{{ log.operatorName || "-" }}</p>
                      <p>{{ log.actionComment || "无说明" }}</p>
                    </div>
                  </div>
                </div>
              </div>
            </template>
          </div>
          <div class="modal-actions">
            <button v-if="warningDetail && canJumpWarningComplaint(warningDetail)" class="ghost" type="button" @click="jumpToWarningComplaint(warningDetail)">
              跳转投诉详情
            </button>
            <button
              v-if="warningDetail && canJumpWarningRectification(warningDetail)"
              class="ghost"
              type="button"
              @click="jumpToWarningRectification(warningDetail)"
            >
              跳转整改详情
            </button>
            <button
              v-if="warningDetail && warningQuickAction(warningDetail.status)"
              class="primary"
              type="button"
              :disabled="actionLoading"
              @click="handleWarningAction(warningDetail, warningQuickAction(warningDetail.status).actionType)"
            >
              {{ warningQuickAction(warningDetail.status).label }}
            </button>
            <button class="ghost" type="button" @click="closeWarningDetail">关闭</button>
          </div>
        </div>
      </div>

      <div v-if="status.message" class="status" :class="status.type">{{ status.message }}</div>
    </section>
  </RegulatorAdminWorkspacePage>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from "vue";
import { useRouter } from "vue-router";
import { fetchWarningRecordDetail, fetchWarningRecords, processWarningRecord } from "../../api/regulation";
import RegulatorAdminWorkspacePage from "../../components/regulatorAdmin/RegulatorAdminWorkspacePage.vue";
import { formatByMap, formatTime } from "../../utils/formatters";
import { warningActionMap, warningLevelMap, warningStatusMap } from "../../utils/statusMaps";
import { useRegulatorAdminShellSession } from "./regulatorAdminShared";

const router = useRouter();
const { regulatorUser, token, handleSidebarNavigate, handleLogout } = useRegulatorAdminShellSession();

const loading = ref(false);
const actionLoading = ref(false);
const warningDetailLoading = ref(false);
const warningDetailVisible = ref(false);
const warningDetail = ref(null);
const records = ref([]);
const page = ref(1);
const size = ref(8);
const total = ref(0);
const pages = ref(1);
const advancedVisible = ref(false);
const status = reactive({ message: "", type: "" });
const filters = reactive({
  status: "",
  level: "",
  warningType: "",
  bizType: "",
  keyword: ""
});

const warningTypeOptions = computed(() => {
  const set = new Set(records.value.map((item) => String(item.warningType || "").trim()).filter(Boolean));
  return Array.from(set).sort();
});
const openCount = computed(() => records.value.filter((item) => item.status === "OPEN").length);

function setStatus(message = "", type = "info") {
  status.message = message;
  status.type = type;
}

function setLevel(level) {
  filters.level = level;
  handleSearch();
}

function setStatusFilter(value) {
  filters.status = value;
  handleSearch();
}

function resetFilters() {
  filters.status = "";
  filters.level = "";
  filters.warningType = "";
  filters.bizType = "";
  filters.keyword = "";
  handleSearch();
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
  if (statusValue === "OPEN") return { actionType: "PROCESS", label: "立即处理" };
  if (statusValue === "PROCESSING") return { actionType: "RESOLVE", label: "标记解决" };
  return null;
}

function canJumpWarningComplaint(warning) {
  return String(warning?.bizType || "").toUpperCase() === "COMPLAINT" && Number(warning?.bizId) > 0;
}

function canJumpWarningRectification(warning) {
  return String(warning?.bizType || "").toUpperCase() === "RECTIFICATION" && Number(warning?.bizId) > 0;
}

function jumpToWarningComplaint(warning) {
  if (!canJumpWarningComplaint(warning)) return;
  router.push({ name: "regulator-admin-complaint-detail", params: { complaintId: Number(warning.bizId) }, query: { from: "warnings" } }).catch(() => {});
  closeWarningDetail();
}

function jumpToWarningRectification(warning) {
  if (!canJumpWarningRectification(warning)) return;
  router.push({ name: "regulator-admin-rectification-detail", params: { rectificationId: Number(warning.bizId) }, query: { from: "warnings" } }).catch(() => {});
  closeWarningDetail();
}

async function loadWarnings() {
  loading.value = true;
  setStatus("");
  try {
    const data = await fetchWarningRecords(token.value, {
      page: page.value,
      size: size.value,
      status: filters.status || undefined,
      level: filters.level || undefined,
      warningType: filters.warningType || undefined,
      bizType: filters.bizType || undefined,
      keyword: filters.keyword || undefined
    });
    records.value = data.records || [];
    total.value = data.total || 0;
    page.value = data.page || 1;
    size.value = data.size || size.value;
    pages.value = data.pages || 1;
  } catch (error) {
    setStatus(error.message || "加载预警列表失败", "error");
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

async function openWarningDetail(item) {
  if (!item?.id) return;
  warningDetailVisible.value = true;
  warningDetailLoading.value = true;
  warningDetail.value = null;
  try {
    warningDetail.value = await fetchWarningRecordDetail(token.value, item.id);
  } catch (error) {
    setStatus(error.message || "加载预警详情失败", "error");
    warningDetailVisible.value = false;
  } finally {
    warningDetailLoading.value = false;
  }
}

function closeWarningDetail() {
  warningDetailVisible.value = false;
  warningDetailLoading.value = false;
  warningDetail.value = null;
}

async function handleWarningAction(target, actionType) {
  const warningId = target?.id;
  if (!warningId || !actionType) return;
  actionLoading.value = true;
  setStatus("");
  try {
    const detailData = await processWarningRecord(token.value, warningId, { actionType });
    if (warningDetailVisible.value && warningDetail.value?.id === warningId) {
      warningDetail.value = detailData;
    }
    setStatus(`预警已执行${formatWarningAction(actionType)}`, "success");
    await loadWarnings();
  } catch (error) {
    setStatus(error.message || "预警处理失败", "error");
  } finally {
    actionLoading.value = false;
  }
}

onMounted(loadWarnings);
</script>

<style scoped>
.warning-page { display: grid; gap: 16px; }
.hero {
  background: linear-gradient(135deg, #002660 0%, #003a8c 100%);
  color: #fff;
  border-radius: 4px;
  padding: 22px;
  display: flex;
  justify-content: space-between;
  align-items: end;
  gap: 20px;
}
.hero-eyebrow { margin: 0; font-size: 11px; color: #bfdbfe; letter-spacing: 0.12em; text-transform: uppercase; font-weight: 800; }
.hero h1 { margin: 8px 0 0; font-size: 36px; line-height: 1.05; font-weight: 900; letter-spacing: -0.02em; }
.hero-desc { margin: 10px 0 0; color: #dbeafe; font-size: 13px; max-width: 620px; }
.hero-metrics { display: flex; gap: 18px; }
.hero-metrics article { text-align: right; }
.hero-metrics span { display: block; color: #bfdbfe; font-size: 11px; font-weight: 800; letter-spacing: 0.08em; text-transform: uppercase; }
.hero-metrics strong { display: block; margin-top: 6px; font-size: 40px; line-height: 1; font-weight: 900; }

.filter-card { background: #eceef1; border: 1px solid #e2e8f0; border-radius: 4px; padding: 14px; display: grid; gap: 10px; }
.filter-row { display: flex; align-items: end; gap: 14px; flex-wrap: wrap; }
.filter-block { display: grid; gap: 6px; }
.filter-block label { font-size: 10px; color: #64748b; font-weight: 900; letter-spacing: 0.08em; text-transform: uppercase; }
.chip-group { display: flex; gap: 6px; flex-wrap: wrap; }
.chip {
  border: 1px solid #cdd5df;
  background: #e0e3e6;
  color: #475569;
  min-height: 34px;
  padding: 0 12px;
  border-radius: 3px;
  font-size: 12px;
  font-weight: 800;
  cursor: pointer;
}
.chip.active { background: #002660; color: #fff; border-color: #002660; }
select, input {
  min-height: 34px;
  border: 1px solid #cdd5df;
  border-radius: 3px;
  padding: 0 10px;
  font-size: 12px;
  background: #e0e3e6;
}
.advanced-btn {
  margin-left: auto;
  min-height: 34px;
  border: 1px solid #cdd5df;
  border-radius: 3px;
  padding: 0 14px;
  background: #e0e3e6;
  color: #002660;
  font-size: 12px;
  font-weight: 900;
  cursor: pointer;
}
.advanced-row { display: grid; grid-template-columns: 1fr 1fr auto; gap: 10px; align-items: end; }
.advanced-row label { display: grid; gap: 6px; color: #475569; font-size: 12px; font-weight: 800; }
.advanced-actions { display: flex; gap: 8px; }

.table-card { background: #fff; border: 1px solid #e2e8f0; border-radius: 4px; overflow: hidden; }
.table-wrap { overflow: auto; }
table { width: 100%; min-width: 1160px; border-collapse: collapse; }
thead tr { background: #e6e8eb; border-bottom: 1px solid #d1d5db; }
th { padding: 12px 14px; font-size: 10px; color: #64748b; font-weight: 900; letter-spacing: 0.08em; text-transform: uppercase; text-align: left; }
td { padding: 12px 14px; border-top: 1px solid #edf2f7; color: #1e293b; font-size: 13px; vertical-align: middle; }
tbody tr:nth-child(even) { background: #f8fafc; }
tbody tr:hover { background: #f1f5f9; }
.center { text-align: center; }
.right { text-align: right; }
.mono { font-family: ui-monospace, SFMono-Regular, Menlo, Consolas, monospace; font-size: 12px; color: #64748b; }
.obj-main { margin: 0; color: #002660; font-weight: 800; }
.obj-sub { margin: 4px 0 0; font-size: 11px; color: #94a3b8; }
.reason { max-width: 360px; }
.reason-title { margin: 0; color: #334155; font-weight: 800; }
.reason p:last-child { margin: 4px 0 0; color: #64748b; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }

.level-pill {
  display: inline-flex;
  min-height: 22px;
  align-items: center;
  justify-content: center;
  padding: 0 8px;
  border-radius: 2px;
  font-size: 10px;
  font-weight: 900;
  letter-spacing: 0.04em;
}
.level-pill.is-l1 { background: #ffdad6; color: #93000a; }
.level-pill.is-l2 { background: #ffdbce; color: #7c2d06; }
.status-pill { display: inline-flex; min-height: 22px; align-items: center; padding: 0 8px; border-radius: 2px; font-size: 10px; font-weight: 900; }
.status-pill.is-open { background: #fff4eb; color: #9b3a0a; border: 1px solid #f8d5bf; }
.status-pill.is-processing { background: #ecfbfb; color: #245d62; border: 1px solid #c4ebec; }
.status-pill.is-resolved { background: #ebf9f1; color: #1f6b4d; border: 1px solid #c6e9d6; }
.status-pill.is-closed { background: #f0f4f8; color: #5a6b7f; border: 1px solid #d8e1ea; }
.status-pill.is-unknown { background: #f5f8fb; color: #64748b; border: 1px solid #d8e1ea; }

.action-row { display: flex; justify-content: flex-end; gap: 8px; }
.action-btn { min-height: 30px; font-size: 11px; font-weight: 800; padding: 0 10px; }

.pager { border-top: 1px solid #e2e8f0; padding: 12px 14px; display: flex; justify-content: space-between; gap: 10px; align-items: center; font-size: 12px; color: #64748b; font-weight: 700; }
.pager-actions { display: flex; gap: 8px; }
.empty { padding: 24px; text-align: center; font-size: 13px; color: #64748b; }

.primary, .ghost {
  min-height: 34px;
  border-radius: 3px;
  padding: 0 12px;
  font-size: 12px;
  font-weight: 800;
  cursor: pointer;
}
.primary { border: 0; background: #002660; color: #fff; }
.ghost { border: 1px solid #d1d5db; background: #fff; color: #334155; }

.modal-mask { position: fixed; inset: 0; background: rgba(15, 23, 42, 0.42); display: flex; justify-content: center; align-items: center; z-index: 1200; padding: 16px; }
.modal-card { width: min(920px, 96vw); max-height: 88vh; display: grid; grid-template-rows: auto 1fr auto; background: #fff; border-radius: 4px; overflow: hidden; }
.modal-title { padding: 14px 16px; border-bottom: 1px solid #e2e8f0; font-size: 14px; font-weight: 900; color: #002660; }
.modal-body { overflow: auto; padding: 14px 16px; display: grid; gap: 10px; }
.summary-grid { display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); gap: 8px; }
.summary-item { border: 1px solid #e2e8f0; border-radius: 3px; background: #f8fafc; padding: 10px; }
.summary-item span { display: block; font-size: 11px; color: #64748b; margin-bottom: 4px; }
.summary-item strong { font-size: 13px; color: #0f172a; }
.field { display: grid; gap: 6px; }
.field > span { font-size: 12px; color: #64748b; font-weight: 800; }
.field > strong { color: #1e293b; font-size: 13px; }
.timeline-list { display: grid; gap: 8px; border: 1px solid #e2e8f0; border-radius: 3px; padding: 10px; max-height: 280px; overflow: auto; }
.timeline-item { border-bottom: 1px dashed #e2e8f0; padding-bottom: 8px; }
.timeline-item:last-child { border-bottom: 0; padding-bottom: 0; }
.timeline-head { display: flex; justify-content: space-between; gap: 10px; align-items: center; }
.timeline-head strong { font-size: 12px; color: #0f172a; }
.timeline-head time { font-size: 11px; color: #94a3b8; white-space: nowrap; }
.timeline-main p { margin: 4px 0 0; font-size: 12px; color: #64748b; }
.modal-empty { color: #94a3b8; font-size: 12px; }
.modal-actions { border-top: 1px solid #e2e8f0; padding: 10px 16px; display: flex; justify-content: flex-end; gap: 8px; flex-wrap: wrap; }

.status { position: fixed; right: 18px; bottom: 18px; border-radius: 3px; padding: 10px 12px; color: #fff; background: #0f172a; font-size: 13px; z-index: 1300; }
.status.error { background: #b91c1c; }
.status.success { background: #166534; }

@media (max-width: 980px) {
  .hero { flex-direction: column; align-items: flex-start; }
  .hero-metrics article { text-align: left; }
  .advanced-row { grid-template-columns: 1fr; }
  .summary-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); }
}

@media (max-width: 720px) {
  .hero h1 { font-size: 30px; }
  .hero-metrics { width: 100%; justify-content: space-between; }
  .summary-grid { grid-template-columns: 1fr; }
}
</style>
