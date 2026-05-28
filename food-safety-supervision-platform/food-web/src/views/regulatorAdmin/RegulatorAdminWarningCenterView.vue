<template>
  <RegulatorAdminWorkspacePage
    active-key="warnings"
    :username="regulatorUser.username || regulatorUser.realName || ''"
    @navigate="handleSidebarNavigate"
    @logout="handleLogout"
  >
    <section class="warning-page">
      <header class="hero">
        <div>
          <p class="hero-eyebrow">Risk Intelligence Center</p>
          <h1>风险预警中心</h1>
          <p class="hero-desc">统一查看辖区风险预警，进入详情页完成分派、改派与处置闭环。</p>
        </div>
        <div class="hero-metrics">
          <article>
            <span>当前活跃预警</span>
            <strong>{{ activeWarningCount }}</strong>
          </article>
          <article>
            <span>待处理</span>
            <strong>{{ pendingWarningCount }}</strong>
          </article>
        </div>
      </header>

      <section class="filter-card">
        <div class="filter-row">
          <div class="filter-block">
            <label>预警等级</label>
            <div class="chip-group">
              <button type="button" :class="['chip', { active: filters.level === '' }]" @click="setLevel('')">全部级别</button>
              <button type="button" :class="['chip', { active: filters.level === 'L1' }]" @click="setLevel('L1')">
                {{ getWarningLevelShortLabel("L1") }}
              </button>
              <button type="button" :class="['chip', { active: filters.level === 'L2' }]" @click="setLevel('L2')">
                {{ getWarningLevelShortLabel("L2") }}
              </button>
            </div>
          </div>
          <div class="filter-block">
            <label>预警类型</label>
            <select v-model="filters.warningType" @change="handleSearch">
              <option value="">全部类型</option>
              <option v-for="item in warningTypeOptions" :key="item" :value="item">{{ item }}</option>
            </select>
          </div>
          <div class="filter-block">
            <label>当前状态</label>
            <div class="chip-group">
              <button type="button" :class="['chip', { active: filters.status === '' }]" @click="setStatusFilter('')">全部</button>
              <button type="button" :class="['chip', { active: filters.status === 'OPEN' }]" @click="setStatusFilter('OPEN')">待处理</button>
              <button type="button" :class="['chip', { active: filters.status === 'PROCESSING' }]" @click="setStatusFilter('PROCESSING')">处理中</button>
              <button type="button" :class="['chip', { active: filters.status === 'RESOLVED' }]" @click="setStatusFilter('RESOLVED')">已解决</button>
            </div>
          </div>
        </div>
      </section>

      <section class="table-card">
        <div class="table-wrap">
          <table>
            <thead>
              <tr>
                <th>触发时间</th>
                <th class="center">等级</th>
                <th>关联对象</th>
                <th>预警摘要</th>
                <th>状态</th>
                <th>当前处理人</th>
                <th class="right">操作</th>
              </tr>
            </thead>
            <tbody v-if="records.length">
              <tr v-for="item in records" :key="item.id">
                <td class="mono">{{ formatTime(item.lastOccurTime || item.createTime) }}</td>
                <td class="center">
                  <AppStatusTag :label="formatWarningLevel(item.level)" :tone="getWarningLevelTone(item.level)" />
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
                  <AppStatusTag :label="formatWarningStatus(item.status)" :tone="warningTone(item.status)" />
                </td>
                <td>{{ item.assignedToName || "尚未指派" }}</td>
                <td>
                  <div class="action-row">
                    <button class="ghost action-btn" type="button" @click="openDetail(item)">详情</button>
                    <button
                      v-if="canAssignWarning(item.status)"
                      class="ghost action-btn"
                      type="button"
                      :disabled="actionLoading"
                      @click="openDetail(item, { action: 'assign' })"
                    >
                      {{ item.assignedTo ? "改派" : "分派" }}
                    </button>
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

          <AppEmptyState
            v-if="!records.length"
            :title="emptyTitle"
            :description="emptyDescription"
            class="warning-page__empty-state"
          />
        </div>

        <div class="pager">
          <span>共 {{ total }} 条，{{ page }}/{{ pages }} 页</span>
          <div class="pager-actions">
            <button class="ghost" type="button" :disabled="page <= 1 || loading" @click="changePage(page - 1)">上一页</button>
            <button class="ghost" type="button" :disabled="page >= pages || loading" @click="changePage(page + 1)">下一页</button>
          </div>
        </div>
      </section>

      <AppStatusToast :message="status.message" :type="status.type" />

      <div v-if="processConfirm.visible" class="modal-mask" @click.self="closeProcessConfirm">
        <div class="modal-card">
          <div class="modal-title">确认开始处理该预警？</div>
          <div class="modal-body">
            <p>确认后，该预警将从“待处理”变为“处理中”，系统会记录当前区域管理员的介入操作。</p>
            <p v-if="!processConfirm.assignedToName" class="modal-note">该预警当前尚未分派执法人员，建议尽快完成分派。</p>
            <div class="modal-summary">
              <div><span>预警标题</span><strong>{{ processConfirm.title || "-" }}</strong></div>
              <div><span>关联对象</span><strong>{{ processConfirm.bizName || "-" }}</strong></div>
            </div>
          </div>
          <div class="modal-actions">
            <button class="ghost" type="button" :disabled="actionLoading" @click="closeProcessConfirm">取消</button>
            <button class="primary" type="button" :disabled="actionLoading" @click="confirmProcessWarning">
              {{ actionLoading ? "处理中..." : "确认开始处理" }}
            </button>
          </div>
        </div>
      </div>

      <div v-if="resolveConfirm.visible" class="modal-mask" @click.self="closeResolveConfirm">
        <div class="modal-card">
          <div class="modal-title">确认标记该预警为已解决？</div>
          <div class="modal-body">
            <p>确认后，该预警将从“处理中”变为“已解决”，系统会记录当前区域管理员的处置结论。</p>
            <div class="modal-summary">
              <div><span>预警标题</span><strong>{{ resolveConfirm.title || "-" }}</strong></div>
              <div><span>关联对象</span><strong>{{ resolveConfirm.bizName || "-" }}</strong></div>
            </div>
          </div>
          <div class="modal-actions">
            <button class="ghost" type="button" :disabled="actionLoading" @click="closeResolveConfirm">取消</button>
            <button class="primary" type="button" :disabled="actionLoading" @click="confirmResolveWarning">
              {{ actionLoading ? "处理中..." : "确认标记解决" }}
            </button>
          </div>
        </div>
      </div>
    </section>
  </RegulatorAdminWorkspacePage>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from "vue";
import { useRoute, useRouter } from "vue-router";
import AppEmptyState from "../../components/common/AppEmptyState.vue";
import AppStatusTag from "../../components/common/AppStatusTag.vue";
import AppStatusToast from "../../components/common/AppStatusToast.vue";
import {
  fetchWarningOverview,
  fetchWarningRecords,
  fetchWarningTypes,
  processWarningRecord
} from "../../api/regulation";
import RegulatorAdminWorkspacePage from "../../components/regulatorAdmin/RegulatorAdminWorkspacePage.vue";
import { formatByMap, formatTime } from "../../utils/formatters";
import {
  getStatusTone,
  getWarningLevelShortLabel,
  getWarningLevelTone,
  warningActionMap,
  warningLevelMap,
  warningStatusMap
} from "../../utils/statusMaps";
import { resolveErrorMessage } from "../../utils/uiFeedback";
import { useRegulatorAdminShellSession } from "./regulatorAdminShared";

const route = useRoute();
const router = useRouter();
const { regulatorUser, token, handleSidebarNavigate, handleLogout } = useRegulatorAdminShellSession();

const loading = ref(false);
const actionLoading = ref(false);
const records = ref([]);
const page = ref(1);
const size = ref(8);
const total = ref(0);
const pages = ref(1);
const status = reactive({ message: "", type: "" });
const summary = ref({});
const typeOptions = ref([]);
const processConfirm = reactive({
  visible: false,
  id: null,
  title: "",
  bizName: "",
  assignedToName: ""
});
const resolveConfirm = reactive({
  visible: false,
  id: null,
  title: "",
  bizName: ""
});
const filters = reactive({
  status: "",
  level: "",
  warningType: ""
});

const activeWarningCount = computed(() => (Number(summary.value?.openCount) || 0) + (Number(summary.value?.processingCount) || 0));
const pendingWarningCount = computed(() => Number(summary.value?.openCount) || 0);
const warningTypeOptions = computed(() => {
  const values = typeOptions.value
    .map((item) => {
      if (typeof item === "string") return item.trim();
      return String(item?.warningType || item?.type || item?.key || "").trim();
    })
    .filter(Boolean);
  if (filters.warningType && !values.includes(filters.warningType)) {
    values.unshift(filters.warningType);
  }
  return [...new Set(values)].sort();
});
const hasFilters = computed(() => Boolean(filters.status || filters.level || filters.warningType));
const emptyTitle = computed(() => (hasFilters.value ? "暂无符合条件的预警记录" : "暂无预警记录"));
const emptyDescription = computed(() => (hasFilters.value ? "可以调整筛选条件后再试。" : "新的风险预警会展示在这里。"));

function setToast(message = "", type = "info") {
  status.message = message;
  status.type = type;
}

function buildListParams() {
  return {
    page: page.value,
    size: size.value,
    status: filters.status || undefined,
    level: filters.level || undefined,
    warningType: filters.warningType || undefined
  };
}

function buildTypeParams() {
  return {
    status: filters.status || undefined,
    level: filters.level || undefined
  };
}

function setLevel(level) {
  filters.level = level;
  handleSearch();
}

function setStatusFilter(value) {
  filters.status = value;
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

function warningTone(value) {
  return getStatusTone(value, "WARNING");
}

function warningQuickAction(statusValue) {
  if (statusValue === "OPEN") return { actionType: "PROCESS", label: "开始处理" };
  if (statusValue === "PROCESSING") return { actionType: "RESOLVE", label: "标记解决" };
  return null;
}

function canAssignWarning(statusValue) {
  return statusValue === "OPEN" || statusValue === "PROCESSING";
}

function requiresProcessConfirm(actionType) {
  return String(actionType || "").toUpperCase() === "PROCESS";
}

function requiresResolveConfirm(actionType) {
  return String(actionType || "").toUpperCase() === "RESOLVE";
}

function openProcessConfirm(target) {
  processConfirm.visible = true;
  processConfirm.id = target?.id || null;
  processConfirm.title = target?.title || "";
  processConfirm.bizName = target?.bizName || target?.title || "";
  processConfirm.assignedToName = target?.assignedToName || "";
}

function closeProcessConfirm() {
  processConfirm.visible = false;
  processConfirm.id = null;
  processConfirm.title = "";
  processConfirm.bizName = "";
  processConfirm.assignedToName = "";
}

function openResolveConfirm(target) {
  resolveConfirm.visible = true;
  resolveConfirm.id = target?.id || null;
  resolveConfirm.title = target?.title || "";
  resolveConfirm.bizName = target?.bizName || target?.title || "";
}

function closeResolveConfirm() {
  resolveConfirm.visible = false;
  resolveConfirm.id = null;
  resolveConfirm.title = "";
  resolveConfirm.bizName = "";
}

async function loadWarningSummary() {
  try {
    summary.value = (await fetchWarningOverview(token.value, {})) || {};
  } catch {
    summary.value = {};
  }
}

async function loadWarningTypeOptions() {
  try {
    const data = await fetchWarningTypes(token.value, buildTypeParams());
    typeOptions.value = Array.isArray(data) ? data : [];
  } catch {
    typeOptions.value = [];
  }
}

async function loadWarnings() {
  loading.value = true;
  setToast("");
  try {
    const data = await fetchWarningRecords(token.value, buildListParams());
    records.value = data.records || [];
    total.value = data.total || 0;
    page.value = data.page || 1;
    size.value = data.size || size.value;
    pages.value = data.pages || 1;
  } catch (error) {
    setToast(resolveErrorMessage(error, "预警列表加载失败，请稍后重试"), "error");
  } finally {
    loading.value = false;
  }
}

async function loadPageMeta() {
  await Promise.all([loadWarningSummary(), loadWarningTypeOptions()]);
}

async function handleSearch() {
  page.value = 1;
  await Promise.all([loadWarnings(), loadPageMeta()]);
}

async function changePage(nextPage) {
  page.value = nextPage;
  await loadWarnings();
}

function openDetail(item, extraQuery = {}) {
  if (!item?.id) return;
  router.push({
    name: "regulator-admin-warning-detail",
    params: { warningId: item.id },
    query: {
      from: route.query.from || "list",
      ...extraQuery
    }
  }).catch(() => {});
}

async function handleWarningAction(target, actionType) {
  const warningId = target?.id;
  if (!warningId || !actionType) return;
  if (requiresProcessConfirm(actionType)) {
    openProcessConfirm(target);
    return;
  }
  if (requiresResolveConfirm(actionType)) {
    openResolveConfirm(target);
    return;
  }
  actionLoading.value = true;
  setToast("");
  try {
    await processWarningRecord(token.value, warningId, { actionType });
    setToast(`预警已执行：${formatWarningAction(actionType)}`, "success");
    await Promise.all([loadWarnings(), loadWarningSummary()]);
  } catch (error) {
    setToast(resolveErrorMessage(error, "预警处理失败，请稍后重试"), "error");
  } finally {
    actionLoading.value = false;
  }
}

async function confirmResolveWarning() {
  if (!resolveConfirm.id) return;
  actionLoading.value = true;
  setToast("");
  try {
    await processWarningRecord(token.value, resolveConfirm.id, { actionType: "RESOLVE" });
    setToast(`预警已执行：${formatWarningAction("RESOLVE")}`, "success");
    closeResolveConfirm();
    await Promise.all([loadWarnings(), loadWarningSummary()]);
  } catch (error) {
    setToast(resolveErrorMessage(error, "预警处理失败，请稍后重试"), "error");
  } finally {
    actionLoading.value = false;
  }
}

async function confirmProcessWarning() {
  if (!processConfirm.id) return;
  actionLoading.value = true;
  setToast("");
  try {
    await processWarningRecord(token.value, processConfirm.id, { actionType: "PROCESS" });
    setToast(`预警已执行：${formatWarningAction("PROCESS")}`, "success");
    closeProcessConfirm();
    await Promise.all([loadWarnings(), loadWarningSummary()]);
  } catch (error) {
    setToast(resolveErrorMessage(error, "预警处理失败，请稍后重试"), "error");
  } finally {
    actionLoading.value = false;
  }
}

onMounted(async () => {
  if (Number(route.query.warningId || 0) > 0) {
    router.replace({
      name: "regulator-admin-warning-detail",
      params: { warningId: Number(route.query.warningId) },
      query: { from: route.query.from || "list" }
    }).catch(() => {});
    return;
  }
  await Promise.all([loadWarnings(), loadPageMeta()]);
});
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
.hero h1 { margin: 8px 0 0; font-size: 36px; line-height: 1.05; font-weight: 900; }
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
select {
  min-height: 34px;
  border: 1px solid #cdd5df;
  border-radius: 3px;
  padding: 0 10px;
  font-size: 12px;
  background: #e0e3e6;
}
.table-card { background: #fff; border: 1px solid #e2e8f0; border-radius: 4px; overflow: hidden; }
.table-wrap { overflow: auto; }
table { width: 100%; min-width: 1220px; border-collapse: collapse; }
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
.action-row { display: flex; justify-content: flex-end; gap: 8px; flex-wrap: wrap; }
.action-btn { min-height: 30px; font-size: 11px; font-weight: 800; padding: 0 10px; }
.warning-page__empty-state { margin: 16px; }

.pager { border-top: 1px solid #e2e8f0; padding: 12px 14px; display: flex; justify-content: space-between; gap: 10px; align-items: center; font-size: 12px; color: #64748b; font-weight: 700; }
.pager-actions { display: flex; gap: 8px; }

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
.modal-card { width: min(520px, 96vw); background: #fff; border-radius: 10px; overflow: hidden; box-shadow: 0 24px 60px rgba(15, 23, 42, 0.2); }
.modal-title { padding: 16px 18px; border-bottom: 1px solid #e2e8f0; font-size: 16px; font-weight: 900; color: #0f172a; }
.modal-body { padding: 16px 18px; display: grid; gap: 10px; color: #475569; font-size: 13px; line-height: 1.7; }
.modal-note { color: #9a3412; background: #fff7ed; border: 1px solid #fed7aa; padding: 10px 12px; border-radius: 8px; }
.modal-summary { display: grid; gap: 8px; }
.modal-summary div { padding: 10px 12px; background: #f8fafc; border: 1px solid #e2e8f0; border-radius: 8px; }
.modal-summary span { display: block; color: #64748b; font-size: 11px; font-weight: 800; margin-bottom: 4px; }
.modal-summary strong { color: #0f172a; font-size: 13px; }
.modal-actions { padding: 12px 18px 16px; display: flex; justify-content: flex-end; gap: 10px; border-top: 1px solid #e2e8f0; }

@media (max-width: 980px) {
  .hero { flex-direction: column; align-items: flex-start; }
  .hero-metrics article { text-align: left; }
}

@media (max-width: 720px) {
  .hero h1 { font-size: 30px; }
  .hero-metrics { width: 100%; justify-content: space-between; }
  .action-row { justify-content: flex-start; }
}
</style>
