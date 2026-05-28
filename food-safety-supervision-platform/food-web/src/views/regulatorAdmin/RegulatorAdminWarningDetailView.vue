<template>
  <RegulatorAdminWorkspacePage
    active-key="warnings"
    :username="regulatorUser.username || regulatorUser.realName || ''"
    @navigate="handleSidebarNavigate"
    @logout="handleLogout"
  >
    <section class="warning-detail-page">
      <div v-if="loading" class="state-card">正在加载预警详情...</div>
      <div v-else-if="!detail" class="state-card state-card--error">未找到该预警详情，请返回列表后重试。</div>

      <template v-else>
        <header class="hero">
          <div class="hero-main">
            <nav class="crumbs" aria-label="面包屑">
              <button class="crumb-link" type="button" @click="goBack">返回预警列表</button>
              <span>/</span>
              <span>{{ detail.warningNo || `WRN-${detail.id}` }}</span>
            </nav>
            <div class="hero-title-row">
              <h1>{{ detail.title || "风险预警详情" }}</h1>
              <span class="status-pill" :class="`is-${warningStatusClass(detail.status)}`">
                {{ formatWarningStatus(detail.status) }}
              </span>
            </div>
            <p class="hero-desc">{{ detail.content || "暂无预警说明。" }}</p>
          </div>
          <div class="hero-side">
            <article>
              <span>预警编号</span>
              <strong>{{ detail.warningNo || `#${detail.id}` }}</strong>
            </article>
            <article>
              <span>预警等级</span>
              <strong>{{ formatWarningLevel(detail.level) }}</strong>
            </article>
            <article>
              <span>触发次数</span>
              <strong>{{ detail.triggerCount || 0 }}</strong>
            </article>
          </div>
        </header>

        <div class="content-grid">
          <div class="main-col">
            <section class="panel">
              <div class="section-head">
                <h2>预警概览</h2>
                <span class="level-pill" :class="`is-${String(detail.level || '').toLowerCase()}`">
                  {{ formatWarningLevel(detail.level) }}
                </span>
              </div>
              <div class="summary-grid">
                <article>
                  <span>当前状态</span>
                  <strong>{{ formatWarningStatus(detail.status) }}</strong>
                </article>
                <article>
                  <span>预警类型</span>
                  <strong>{{ detail.warningType || "-" }}</strong>
                </article>
                <article>
                  <span>关联对象</span>
                  <strong>{{ detail.bizName || detail.title || "-" }}</strong>
                </article>
                <article>
                  <span>对象类型</span>
                  <strong>{{ detail.bizType || "-" }}</strong>
                </article>
                <article>
                  <span>所属辖区</span>
                  <strong>{{ detail.regionPathText || detail.regionName || "-" }}</strong>
                </article>
                <article>
                  <span>归属监管人</span>
                  <strong>{{ detail.ownerName || "-" }}</strong>
                </article>
                <article>
                  <span>当前处理人</span>
                  <strong>{{ detail.assignedToName || "尚未分派" }}</strong>
                </article>
                <article>
                  <span>最近触发时间</span>
                  <strong>{{ formatTime(detail.lastOccurTime || detail.updateTime || detail.createTime) }}</strong>
                </article>
              </div>
              <div class="detail-block">
                <label>预警内容</label>
                <p>{{ detail.content || "-" }}</p>
              </div>
            </section>

            <section class="panel">
              <div class="section-head">
                <h2>处理记录</h2>
                <span class="section-hint">共 {{ detail.processLogs?.length || 0 }} 条记录</span>
              </div>
              <div class="timeline">
                <article v-for="log in detail.processLogs || []" :key="log.id" class="timeline-item">
                  <span class="timeline-dot"></span>
                  <div class="timeline-main">
                    <div class="timeline-head">
                      <strong>{{ formatWarningAction(log.actionType) }}</strong>
                      <time>{{ formatTime(log.createTime) }}</time>
                    </div>
                    <p>操作人：{{ formatLogOperator(log) }}</p>
                    <p>{{ log.actionComment || "未填写处理说明。" }}</p>
                  </div>
                </article>
                <div v-if="!detail.processLogs?.length" class="empty-box empty-box--plain">暂无处理记录。</div>
              </div>
            </section>
          </div>

          <aside class="side-col">
            <section class="panel panel-accent">
              <h2>快捷操作</h2>
              <div class="action-stack">
                <button
                  v-if="warningQuickAction(detail.status)"
                  class="primary"
                  type="button"
                  :disabled="actionLoading"
                  @click="handleWarningAction(warningQuickAction(detail.status).actionType)"
                >
                  {{ actionLoading ? "处理中..." : warningQuickAction(detail.status).label }}
                </button>
                <button
                  v-if="canJumpWarningComplaint(detail)"
                  class="ghost ghost--light"
                  type="button"
                  @click="jumpToWarningComplaint"
                >
                  查看关联投诉
                </button>
                <button
                  v-if="canJumpWarningRectification(detail)"
                  class="ghost ghost--light"
                  type="button"
                  @click="jumpToWarningRectification"
                >
                  查看关联整改
                </button>
                <button class="ghost ghost--light" type="button" @click="goBack">返回上一页</button>
              </div>
            </section>

            <section v-if="canAssignWarning(detail.status)" ref="assignPanelRef" class="panel">
              <div class="section-head">
                <h2>{{ detail.assignedTo ? "改派执法人员" : "分派执法人员" }}</h2>
                <span class="section-hint">选择辖区内可处理该预警的执法人员</span>
              </div>
              <div class="assign-panel">
                <label>
                  指派人员
                  <select v-model="assignForm.assignedTo" :disabled="assignLoading || actionLoading">
                    <option value="">请选择执法人员</option>
                    <option v-for="item in assignOptions" :key="item.id" :value="String(item.id)">
                      {{ item.name || item.username || `执法人员 ${item.id}` }}
                    </option>
                  </select>
                </label>
                <label>
                  处理说明
                  <textarea
                    v-model.trim="assignForm.actionComment"
                    rows="4"
                    maxlength="200"
                    placeholder="可选填写本次分派或改派原因"
                  />
                </label>
                <div v-if="assignLoading" class="empty-box">正在加载可选执法人员...</div>
                <div v-else-if="!assignOptions.length" class="empty-box">当前辖区暂无可分派的执法人员。</div>
                <button
                  class="primary assign-submit"
                  type="button"
                  :disabled="actionLoading || assignLoading || !assignForm.assignedTo"
                  @click="handleAssignWarning"
                >
                  {{ detail.assignedTo ? "确认改派" : "确认分派" }}
                </button>
              </div>
            </section>

            <section class="panel">
              <h2>关联信息</h2>
              <div class="mini-list">
                <article>
                  <span>对象名称</span>
                  <strong>{{ detail.bizName || "-" }}</strong>
                </article>
                <article>
                  <span>对象类型</span>
                  <strong>{{ detail.bizType || "-" }}</strong>
                </article>
                <article>
                  <span>对象 ID</span>
                  <strong>{{ detail.bizId || "-" }}</strong>
                </article>
                <article>
                  <span>创建时间</span>
                  <strong>{{ formatTime(detail.createTime) }}</strong>
                </article>
              </div>
            </section>
          </aside>
        </div>

        <div v-if="status.message" class="status-banner" :class="`is-${status.type}`">{{ status.message }}</div>
      </template>

      <div v-if="processConfirmVisible" class="modal-mask" @click.self="closeProcessConfirm">
        <div class="modal-card">
          <div class="modal-title">确认开始处理该预警？</div>
          <div class="modal-body">
            <p>确认后，该预警将从“待处理”变为“处理中”，系统会记录当前区域管理员的介入操作。</p>
            <p v-if="!detail?.assignedToName" class="modal-note">该预警当前尚未分派执法人员，建议尽快完成分派。</p>
            <div class="modal-summary">
              <div><span>预警标题</span><strong>{{ detail?.title || "-" }}</strong></div>
              <div><span>关联对象</span><strong>{{ detail?.bizName || detail?.title || "-" }}</strong></div>
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

      <div v-if="resolveConfirmVisible" class="modal-mask" @click.self="closeResolveConfirm">
        <div class="modal-card">
          <div class="modal-title">确认标记该预警为已解决？</div>
          <div class="modal-body">
            <p>确认后，该预警将从“处理中”变为“已解决”，系统会记录当前区域管理员的处置结果。</p>
            <div class="modal-summary">
              <div><span>预警标题</span><strong>{{ detail?.title || "-" }}</strong></div>
              <div><span>关联对象</span><strong>{{ detail?.bizName || detail?.title || "-" }}</strong></div>
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
import { nextTick, onMounted, reactive, ref, watch } from "vue";
import { useRoute, useRouter } from "vue-router";
import { fetchEligibleRegulators, fetchWarningRecordDetail, processWarningRecord } from "../../api/regulation";
import RegulatorAdminWorkspacePage from "../../components/regulatorAdmin/RegulatorAdminWorkspacePage.vue";
import { formatByMap, formatTime } from "../../utils/formatters";
import { warningActionMap, warningLevelMap, warningStatusMap } from "../../utils/statusMaps";
import { resolveErrorMessage } from "../../utils/uiFeedback";
import { useRegulatorAdminShellSession } from "./regulatorAdminShared";

const route = useRoute();
const router = useRouter();
const { regulatorUser, token, handleSidebarNavigate, handleLogout } = useRegulatorAdminShellSession();

const loading = ref(false);
const actionLoading = ref(false);
const assignLoading = ref(false);
const detail = ref(null);
const assignOptions = ref([]);
const assignPanelRef = ref(null);
const processConfirmVisible = ref(false);
const resolveConfirmVisible = ref(false);
const status = reactive({ message: "", type: "info" });
const assignForm = reactive({
  assignedTo: "",
  actionComment: ""
});

function setStatus(message = "", type = "info") {
  status.message = message;
  status.type = type;
}

function normalizeWarningErrorMessage(error, fallback) {
  const resolved = resolveErrorMessage(error, fallback);
  if (String(resolved || "").trim().toLowerCase() === "warning not found") {
    return "未找到该预警详情，请返回列表后重试。";
  }
  return resolved;
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

function formatLogOperator(log) {
  return String(log?.operatorName || "").trim() || "系统";
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

function requiresProcessConfirm(actionType) {
  return String(actionType || "").toUpperCase() === "PROCESS";
}

function requiresResolveConfirm(actionType) {
  return String(actionType || "").toUpperCase() === "RESOLVE";
}

function canAssignWarning(statusValue) {
  return statusValue === "OPEN" || statusValue === "PROCESSING";
}

function canJumpWarningComplaint(warning) {
  return String(warning?.bizType || "").toUpperCase() === "COMPLAINT" && Number(warning?.bizId) > 0;
}

function canJumpWarningRectification(warning) {
  return String(warning?.bizType || "").toUpperCase() === "RECTIFICATION" && Number(warning?.bizId) > 0;
}

function prepareAssignForm() {
  assignForm.assignedTo = detail.value?.assignedTo ? String(detail.value.assignedTo) : "";
  assignForm.actionComment = "";
}

async function loadAssignOptions(regionId) {
  assignOptions.value = [];
  if (!regionId) return;
  assignLoading.value = true;
  try {
    const data = await fetchEligibleRegulators(token.value, regionId);
    assignOptions.value = Array.isArray(data) ? data : [];
  } catch {
    assignOptions.value = [];
  } finally {
    assignLoading.value = false;
  }
}

async function maybeScrollToAssign() {
  if (route.query.action !== "assign" || !canAssignWarning(detail.value?.status)) return;
  await nextTick();
  assignPanelRef.value?.scrollIntoView({ behavior: "smooth", block: "start" });
}

async function refreshDetailState(nextDetail) {
  detail.value = nextDetail;
  prepareAssignForm();
  if (canAssignWarning(detail.value?.status)) {
    await loadAssignOptions(detail.value?.regionId);
  } else {
    assignOptions.value = [];
  }
}

async function loadDetail() {
  const warningId = Number(route.params.warningId || 0);
  if (!warningId) {
    detail.value = null;
    return;
  }
  loading.value = true;
  setStatus("");
  try {
    const nextDetail = await fetchWarningRecordDetail(token.value, warningId);
    await refreshDetailState(nextDetail);
  } catch (error) {
    detail.value = null;
    assignOptions.value = [];
    setStatus(normalizeWarningErrorMessage(error, "加载预警详情失败"), "error");
  } finally {
    loading.value = false;
    await maybeScrollToAssign();
  }
}

async function handleWarningAction(actionType) {
  if (!detail.value?.id || !actionType) return;
  if (requiresProcessConfirm(actionType)) {
    processConfirmVisible.value = true;
    return;
  }
  if (requiresResolveConfirm(actionType)) {
    resolveConfirmVisible.value = true;
    return;
  }

  actionLoading.value = true;
  setStatus("");
  try {
    const nextDetail = await processWarningRecord(token.value, detail.value.id, { actionType });
    await refreshDetailState(nextDetail);
    setStatus(`预警已执行${formatWarningAction(actionType)}`, "success");
  } catch (error) {
    setStatus(normalizeWarningErrorMessage(error, "预警处理失败"), "error");
  } finally {
    actionLoading.value = false;
  }
}

function closeProcessConfirm() {
  processConfirmVisible.value = false;
}

function closeResolveConfirm() {
  resolveConfirmVisible.value = false;
}

async function confirmProcessWarning() {
  if (!detail.value?.id) return;
  actionLoading.value = true;
  setStatus("");
  try {
    const nextDetail = await processWarningRecord(token.value, detail.value.id, { actionType: "PROCESS" });
    await refreshDetailState(nextDetail);
    closeProcessConfirm();
    setStatus(`预警已执行${formatWarningAction("PROCESS")}`, "success");
  } catch (error) {
    setStatus(normalizeWarningErrorMessage(error, "预警处理失败"), "error");
  } finally {
    actionLoading.value = false;
  }
}

async function confirmResolveWarning() {
  if (!detail.value?.id) return;
  actionLoading.value = true;
  setStatus("");
  try {
    const nextDetail = await processWarningRecord(token.value, detail.value.id, { actionType: "RESOLVE" });
    await refreshDetailState(nextDetail);
    closeResolveConfirm();
    setStatus(`预警已执行${formatWarningAction("RESOLVE")}`, "success");
  } catch (error) {
    setStatus(normalizeWarningErrorMessage(error, "预警处理失败"), "error");
  } finally {
    actionLoading.value = false;
  }
}

async function handleAssignWarning() {
  if (!detail.value?.id) return;
  if (!assignForm.assignedTo) {
    setStatus("请选择执法人员", "error");
    return;
  }
  const hadAssignee = Boolean(detail.value?.assignedTo);
  actionLoading.value = true;
  setStatus("");
  try {
    const nextDetail = await processWarningRecord(token.value, detail.value.id, {
      actionType: "ASSIGN",
      assignedTo: Number(assignForm.assignedTo),
      actionComment: assignForm.actionComment || undefined
    });
    await refreshDetailState(nextDetail);
    setStatus(hadAssignee ? "预警改派成功" : "预警分派成功", "success");
  } catch (error) {
    setStatus(normalizeWarningErrorMessage(error, "预警分派失败"), "error");
  } finally {
    actionLoading.value = false;
  }
}

function jumpToWarningComplaint() {
  if (!canJumpWarningComplaint(detail.value)) return;
  router.push({
    name: "regulator-admin-complaint-detail",
    params: { complaintId: Number(detail.value.bizId) },
    query: { from: "warnings" }
  }).catch(() => {});
}

function jumpToWarningRectification() {
  if (!canJumpWarningRectification(detail.value)) return;
  router.push({
    name: "regulator-admin-rectification-detail",
    params: { rectificationId: Number(detail.value.bizId) },
    query: { from: "warnings" }
  }).catch(() => {});
}

function goBack() {
  if (route.query.from === "overview") {
    router.push({ name: "regulator-admin-overview" }).catch(() => {});
    return;
  }
  router.push({ name: "regulator-admin-warnings" }).catch(() => {});
}

onMounted(loadDetail);
watch(() => route.params.warningId, loadDetail);
watch(() => route.query.action, maybeScrollToAssign);
</script>

<style scoped>
.warning-detail-page { display: grid; gap: 16px; }
.state-card { padding: 18px 20px; border: 1px solid #dbe3ee; background: #fff; color: #64748b; border-radius: 12px; }
.state-card--error { color: #b91c1c; border-color: #fecaca; background: #fef2f2; }
.hero { display: grid; grid-template-columns: minmax(0, 1fr) 320px; gap: 16px; padding: 18px; background: linear-gradient(135deg, #f8fbff, #eef4ff); border: 1px solid #dbe3ee; border-radius: 12px; }
.crumbs { display: flex; gap: 6px; color: #64748b; font-size: 11px; font-weight: 700; }
.crumb-link { padding: 0; border: 0; background: transparent; color: #002660; cursor: pointer; font-size: inherit; font-weight: inherit; }
.hero-title-row { margin-top: 10px; display: flex; gap: 10px; align-items: center; flex-wrap: wrap; }
.hero-title-row h1 { margin: 0; color: #002660; font-size: 30px; font-weight: 900; }
.hero-desc { margin: 10px 0 0; color: #475569; font-size: 13px; line-height: 1.7; max-width: 760px; }
.hero-side { display: grid; gap: 10px; }
.hero-side article { padding: 12px 14px; background: #fff; border: 1px solid #dbe3ee; border-radius: 10px; }
.hero-side span, .summary-grid span, .detail-block label, .mini-list span { display: block; color: #64748b; font-size: 11px; font-weight: 700; }
.hero-side strong, .summary-grid strong, .mini-list strong { display: block; margin-top: 5px; color: #0f172a; font-size: 15px; font-weight: 800; line-height: 1.5; }
.content-grid { display: grid; grid-template-columns: minmax(0, 1fr) 340px; gap: 16px; align-items: start; }
.main-col, .side-col { display: grid; gap: 16px; }
.panel { border: 1px solid #dbe3ee; background: #fff; padding: 16px; border-radius: 12px; }
.panel-accent { background: linear-gradient(135deg, #002660, #003a8c); border-color: transparent; color: #fff; }
.panel h2 { margin: 0; color: #002660; font-size: 12px; font-weight: 900; text-transform: uppercase; letter-spacing: 0.08em; }
.panel-accent h2 { color: rgba(255, 255, 255, 0.78); }
.section-head { display: flex; justify-content: space-between; align-items: center; gap: 10px; margin-bottom: 14px; }
.section-hint { color: #94a3b8; font-size: 11px; font-weight: 700; }
.summary-grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 12px; }
.summary-grid article, .mini-list article { padding: 12px; background: #f8fafc; border: 1px solid #eef2f7; border-radius: 10px; }
.detail-block { margin-top: 14px; padding: 12px; background: #f8fafc; border-left: 3px solid #cbd5e1; }
.detail-block p { margin: 6px 0 0; color: #334155; font-size: 13px; line-height: 1.7; white-space: pre-line; }
.timeline { position: relative; display: grid; gap: 14px; }
.timeline::before { content: ""; position: absolute; left: 5px; top: 8px; bottom: 8px; width: 2px; background: #e2e8f0; }
.timeline-item { position: relative; padding-left: 22px; }
.timeline-dot { position: absolute; left: 0; top: 4px; width: 12px; height: 12px; border-radius: 999px; background: #002660; border: 2px solid #fff; box-shadow: 0 0 0 1px #cbd5e1; }
.timeline-head { display: flex; justify-content: space-between; gap: 10px; align-items: center; }
.timeline-head strong { color: #0f172a; font-size: 12px; }
.timeline-head time { color: #94a3b8; font-size: 10px; white-space: nowrap; }
.timeline-main p { margin: 6px 0 0; color: #64748b; font-size: 12px; line-height: 1.6; }
.action-stack { display: grid; gap: 10px; margin-top: 14px; }
.mini-list { display: grid; gap: 10px; margin-top: 14px; }
.assign-panel { display: grid; gap: 12px; }
.assign-panel label { display: grid; gap: 6px; color: #475569; font-size: 12px; font-weight: 700; }
.assign-panel select, .assign-panel textarea {
  width: 100%;
  min-height: 38px;
  border: 1px solid #cbd5e1;
  border-radius: 8px;
  padding: 0 12px;
  background: #fff;
  font-size: 12px;
}
.assign-panel textarea { min-height: 96px; padding: 10px 12px; resize: vertical; }
.assign-submit { width: 100%; }
.primary, .ghost { min-height: 36px; padding: 0 14px; border: 1px solid #cbd5e1; border-radius: 8px; font-size: 12px; font-weight: 700; cursor: pointer; }
.primary { background: #002660; border-color: #002660; color: #fff; }
.ghost { background: #fff; color: #334155; }
.ghost--light { background: rgba(255, 255, 255, 0.08); border-color: rgba(255, 255, 255, 0.24); color: #fff; }
.level-pill, .status-pill { display: inline-flex; min-height: 24px; align-items: center; justify-content: center; padding: 0 10px; border-radius: 999px; border: 1px solid transparent; font-size: 11px; font-weight: 800; }
.level-pill.is-l1 { background: #fff4eb; color: #9a3412; border-color: #fcd9b8; }
.level-pill.is-l2 { background: #fee2e2; color: #991b1b; border-color: #fecaca; }
.status-pill.is-open { background: #fff4eb; color: #9b3a0a; border-color: #f8d5bf; }
.status-pill.is-processing { background: #ecfeff; color: #155e75; border-color: #a5f3fc; }
.status-pill.is-resolved { background: #dcfce7; color: #166534; border-color: #86efac; }
.status-pill.is-closed { background: #f1f5f9; color: #475569; border-color: #dbe3ee; }
.status-pill.is-unknown { background: #f8fafc; color: #64748b; border-color: #dbe3ee; }
.empty-box { padding: 14px; border: 1px dashed #cbd5e1; background: #f8fafc; color: #64748b; font-size: 12px; border-radius: 10px; }
.empty-box--plain { margin-left: 22px; }
.status-banner { padding: 10px 12px; border: 1px solid #dbe3ee; background: #f8fafc; color: #334155; border-radius: 10px; }
.status-banner.is-error { border-color: #fecaca; background: #fef2f2; color: #b91c1c; }
.status-banner.is-success { border-color: #bbf7d0; background: #ecfdf5; color: #166534; }
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
@media (max-width: 1080px) {
  .hero, .content-grid { grid-template-columns: 1fr; }
}
@media (max-width: 760px) {
  .summary-grid { grid-template-columns: 1fr; }
  .hero-title-row h1 { font-size: 22px; }
  .timeline-head { flex-direction: column; align-items: flex-start; }
}
</style>
