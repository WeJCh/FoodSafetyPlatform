<template>
  <RegulatorAdminWorkspacePage
    active-key="sampling"
    :username="regulatorUser.username"
    @navigate="handleSidebarNavigate"
    @logout="handleLogout"
  >
    <section class="sd-page">
      <header class="sd-head">
        <div>
          <nav class="sd-crumbs" aria-label="面包屑">
            <button type="button" class="sd-crumb-link" @click="goList">SAMPLING</button>
            <span class="sd-crumb-sep">›</span>
            <span class="sd-crumb-mono">{{ task?.taskNo || "—" }}</span>
            <span class="sd-crumb-sep">›</span>
            <span class="sd-crumb-current">结果详情</span>
          </nav>
          <h1 class="sd-title">抽检结果与发布详情</h1>
        </div>
        <div class="sd-head-actions">
          <button type="button" class="sd-btn sd-btn--surface" @click="goList">返回抽检列表</button>
        </div>
      </header>

      <div v-if="pageLoading" class="sd-state">加载任务详情中…</div>
      <div v-else-if="loadError" class="sd-state sd-state--error">{{ loadError }}</div>
      <template v-else-if="task">
        <div v-if="task.samplingResult === 'FAIL'" class="sd-risk" role="alert">
          <div class="sd-risk__left">
            <div class="sd-risk__icon" aria-hidden="true">!</div>
            <div>
              <h2 class="sd-risk__title">高风险提示：检测结果不合格</h2>
              <p class="sd-risk__desc">任务「{{ task.taskTitle || task.productName || "—" }}」检出结论为不合格，请按流程启动核查与后续处置。</p>
            </div>
          </div>
          <div class="sd-risk__right">
            <div class="sd-risk__label">风险等级</div>
            <div class="sd-risk__level">重点监管</div>
          </div>
        </div>

        <div class="sd-bento">
          <div class="sd-col sd-col--aside">
            <section class="sd-card">
              <h3 class="sd-card__h"><span class="sd-card__icon">▣</span>任务与样品概览</h3>
              <div class="sd-cover">
                <div class="sd-cover__inner">
                  <span class="sd-cover__tag">{{ task.productCategory || "品类未填" }}</span>
                  <p class="sd-cover__name">{{ task.productName || "—" }}</p>
                </div>
              </div>
              <div class="sd-mini-grid">
                <div class="sd-mini"><span class="sd-mini__label">任务编号</span><span class="sd-mini__val mono">{{ task.taskNo || "—" }}</span></div>
                <div class="sd-mini"><span class="sd-mini__label">品类</span><span class="sd-mini__val">{{ task.productCategory || "—" }}</span></div>
              </div>
              <div class="sd-divider"></div>
              <dl class="sd-kv">
                <div class="sd-kv__row"><dt>受检企业</dt><dd>{{ enterprise?.enterpriseName || task.enterpriseName || "—" }}</dd></div>
                <div class="sd-kv__row sd-kv__row--time"><dt>采样 / 截止时间</dt><dd>{{ formatTime(task.sampledTime) }} · 截止 {{ formatTime(task.deadline) }}</dd></div>
                <div class="sd-kv__row"><dt>执行人员</dt><dd>{{ task.assignedToName || "—" }}</dd></div>
                <div class="sd-kv__row"><dt>所属区域</dt><dd>{{ regionLabel }}</dd></div>
              </dl>
            </section>

            <section class="sd-card">
              <h3 class="sd-card__h"><span class="sd-card__icon">⟲</span>审计追踪与历史</h3>
              <div v-if="timeline.length" class="sd-timeline">
                <div v-for="(ev, idx) in timeline" :key="ev.key || idx" class="sd-timeline__item">
                  <span class="sd-timeline__dot" :class="ev.dotClass" />
                  <div><div class="sd-timeline__title">{{ ev.title }}</div><div class="sd-timeline__sub">{{ ev.sub }}</div></div>
                </div>
              </div>
              <div v-else class="sd-timeline-empty">暂无操作日志</div>
            </section>
          </div>

          <div class="sd-col sd-col--main">
            <section class="sd-pub">
              <div class="sd-pub__text">
                <div class="sd-pub__badge"><span class="sd-pub__pulse" />公示状态：{{ formatSamplingPublicStatus(task.samplingPublicStatus) }}</div>
                <h3 class="sd-pub__title">公示管理控制台</h3>
                <p class="sd-pub__desc">当前检测结果对公众披露状态如上。可在下方查看实验室结论摘要，并决定是否发布或下线公示信息。</p>
              </div>
              <div class="sd-pub__actions">
                <button v-if="task.samplingResultId && task.samplingPublicStatus === 'PUBLISHED'" type="button" class="sd-btn sd-btn--on-dark sd-btn--ghost" :disabled="actionLoading" @click="handleOffline">下线公示</button>
                <button v-if="task.samplingResultId && task.samplingPublicStatus !== 'PUBLISHED'" type="button" class="sd-btn sd-btn--on-dark sd-btn--accent" :disabled="actionLoading" @click="handlePublish">立即发布</button>
                <p v-if="!task.samplingResultId" class="sd-pub__warn">抽检结果尚未生成，无法操作公示。</p>
              </div>
            </section>

            <section class="sd-card sd-card--table">
              <div class="sd-table-head">
                <h3 class="sd-card__h sd-card__h--inline"><span class="sd-card__icon">◈</span>检测结论摘要</h3>
                <div v-if="task.samplingResult" class="sd-table-flag" :class="{ 'is-fail': task.samplingResult === 'FAIL' }"><span class="sd-table-flag__dot" />{{ task.samplingResult === "FAIL" ? "检测到不合格项" : "未检出异常" }}</div>
                <div v-else class="sd-table-flag is-pending"><span class="sd-table-flag__dot" />待执法人员提交结果</div>
              </div>
              <div class="sd-table-wrap">
                <table class="sd-table">
                  <thead><tr><th>项目</th><th>结论 / 说明</th><th class="sd-col-result">结果</th></tr></thead>
                  <tbody>
                    <tr>
                      <td class="sd-em">综合判定</td>
                      <td class="mono muted">{{ task.samplingConclusion || "—" }}</td>
                      <td class="sd-col-result sd-col-result__cell"><span class="sd-pill" :class="resultPillClass(task.samplingResult)">{{ formatInspectionResult(task.samplingResult) }}</span></td>
                    </tr>
                  </tbody>
                </table>
              </div>
              <div class="sd-lab-notes"><div class="sd-note-block"><span class="sd-note-block__label">任务与产品说明</span><p class="sd-note-block__text">{{ task.taskDesc || "暂无补充说明。" }}</p></div></div>
            </section>

            <section class="sd-split">
              <div class="sd-card sd-card--accent2 sd-card--full">
                <h4 class="sd-subh">监管处置建议</h4>
                <ul class="sd-plan"><li v-for="(line, i) in planLines" :key="i">{{ line }}</li></ul>
              </div>
            </section>
          </div>
        </div>
      </template>
      <div v-if="status.message" class="sd-toast" :class="status.type">{{ status.message }}</div>
    </section>
  </RegulatorAdminWorkspacePage>
</template>

<script setup>
import { computed, reactive, ref, watch } from "vue";
import { useRoute, useRouter } from "vue-router";
import { fetchOperationAuditLogs, findSamplingTaskById, offlineSamplingResult, publishSamplingResult } from "../../api/regulationOperation";
import { fetchEnterpriseDetail, fetchRegionPath } from "../../api/regulation";
import RegulatorAdminWorkspacePage from "../../components/regulatorAdmin/RegulatorAdminWorkspacePage.vue";
import { formatTime } from "../../utils/formatters";
import { formatStatusLabel, inspectionResultMap, samplingPublicStatusMap } from "../../utils/statusMaps";
import { useRegulatorAdminShellSession } from "./regulatorAdminShared";

const route = useRoute();
const router = useRouter();
const { regulatorUser, token, handleSidebarNavigate, handleLogout } = useRegulatorAdminShellSession();
const status = reactive({ message: "", type: "" });
const pageLoading = ref(true);
const loadError = ref("");
const task = ref(null);
const enterprise = ref(null);
const regionLabel = ref("—");
const auditLogs = ref([]);
const actionLoading = ref(false);
function setStatus(message, type = "info") { status.message = message; status.type = type; }
function formatSamplingPublicStatus(value) { if (!value) return "草稿"; return formatStatusLabel(value, samplingPublicStatusMap); }
function formatInspectionResult(value) { if (!value) return "待处理"; return formatStatusLabel(value, inspectionResultMap, "—"); }
function resultPillClass(value) { if (value === "PASS") return "is-pass"; if (value === "FAIL") return "is-fail"; return "is-pending"; }
const timeline = computed(() => {
  const t = task.value; if (!t) return [];
  if (!auditLogs.value.length) return [];
  return auditLogs.value.map((item, index) => ({
    key: `${item.targetType || "target"}-${item.id || index}`,
    title: formatSamplingAuditTitle(item),
    sub: `${formatTime(item.createTime)}${item.operatorName ? ` · ${item.operatorName}` : ""}`,
    dotClass: samplingAuditDotClass(item)
  }));
});
const planLines = computed(() => {
  const t = task.value; if (!t) return ["加载中…"];
  if (t.samplingResult === "FAIL") return [`通知「${t.enterpriseName || enterprise.value?.enterpriseName || "企业"}」限期整改并留存书面记录。`, "视情况启动批次追溯与同类企业排查。", "公示前请复核结论与证据材料完整性。"];
  if (t.samplingResult === "PASS") return ["归档抽检材料，纳入日常监管信用记录。", "无额外强制处置项。"];
  return ["待执法人员提交抽检结果后生成处置建议。"];
});
async function loadDetail() {
  auditLogs.value = [];
  pageLoading.value = true; loadError.value = ""; task.value = null; enterprise.value = null; regionLabel.value = "—"; setStatus("");
  const id = route.params.taskId; if (!id) { loadError.value = "缺少任务 ID"; pageLoading.value = false; return; }
  try {
    const row = await findSamplingTaskById(token.value, id);
    if (!row) { loadError.value = "未找到该抽检任务，请从列表重新进入。"; return; }
    task.value = row;
    const [taskLogs, resultLogs] = await Promise.all([
      fetchOperationAuditLogs(token.value, "SAMPLING_TASK", row.id, 12).catch(() => []),
      row.samplingResultId
        ? fetchOperationAuditLogs(token.value, "SAMPLING_RESULT", row.samplingResultId, 12).catch(() => [])
        : Promise.resolve([])
    ]);
    auditLogs.value = [...taskLogs, ...resultLogs]
      .sort((a, b) => new Date(b.createTime || 0).getTime() - new Date(a.createTime || 0).getTime());
    if (row.enterpriseId) {
      try {
        const ent = await fetchEnterpriseDetail(token.value, row.enterpriseId);
        enterprise.value = ent || null;
        if (ent?.regionId) {
          const path = await fetchRegionPath(token.value, ent.regionId).catch(() => []);
          regionLabel.value = Array.isArray(path) && path.length ? path.map((p) => p.name).join(" / ") : "—";
        }
      } catch { enterprise.value = null; }
    }
  } catch (e) { loadError.value = e?.message || "加载失败"; } finally { pageLoading.value = false; }
}
async function handlePublish() {
  const t = task.value; if (!t?.samplingResultId) { setStatus("抽检结果未生成，无法公示", "error"); return; }
  actionLoading.value = true; setStatus("");
  try { await publishSamplingResult(token.value, t.samplingResultId); setStatus("抽检结果已公示", "success"); await loadDetail(); }
  catch (e) { setStatus(e?.message || "公示失败", "error"); } finally { actionLoading.value = false; }
}
async function handleOffline() {
  const t = task.value; if (!t?.samplingResultId) return;
  actionLoading.value = true; setStatus("");
  try { await offlineSamplingResult(token.value, t.samplingResultId); setStatus("抽检结果已下线", "success"); await loadDetail(); }
  catch (e) { setStatus(e?.message || "下线失败", "error"); } finally { actionLoading.value = false; }
}
function goList() { router.push({ name: "regulator-admin-sampling" }).catch(() => {}); }
function formatSamplingAuditTitle(item) {
  const actionType = String(item?.actionType || "").toUpperCase();
  if (actionType === "SAMPLING_ASSIGN") return "任务分派";
  if (actionType === "SAMPLING_RESULT_SUBMIT") return "提交抽检结果";
  if (actionType === "SAMPLING_RESULT_PUBLISH") return "结果公示";
  if (actionType === "SAMPLING_RESULT_OFFLINE") return "结果下线";
  return item?.actionName || item?.actionType || "抽检任务日志";
}
function samplingAuditDotClass(item) {
  const actionType = String(item?.actionType || "").toUpperCase();
  if (actionType === "SAMPLING_RESULT_OFFLINE") return "is-muted";
  if (String(item?.summary || "").includes("FAIL")) return "is-alert";
  return "is-on";
}
watch(() => route.params.taskId, () => loadDetail(), { immediate: true });
</script>

<style scoped>
.sd-page { display: flex; flex-direction: column; gap: 24px; width: 100%; }
.sd-head { display: flex; justify-content: space-between; align-items: flex-end; gap: 16px; flex-wrap: wrap; }
.sd-crumbs { display: flex; align-items: center; gap: 8px; font-size: 10px; font-weight: 800; text-transform: uppercase; letter-spacing: 0.08em; color: #64748b; margin-bottom: 8px; }
.sd-crumb-link { border: 0; background: none; padding: 0; font: inherit; color: #64748b; cursor: pointer; }
.sd-crumb-link:hover { color: #002660; }
.sd-crumb-sep { opacity: 0.5; font-weight: 700; }
.sd-crumb-mono { font-family: ui-monospace, monospace; color: #475569; }
.sd-crumb-current { color: #002660; }
.sd-title { margin: 0; font-size: 28px; font-weight: 900; letter-spacing: -0.02em; color: #002660; }
.sd-head-actions { display: flex; align-items: center; gap: 10px; }
.sd-btn { border-radius: 8px; font-size: 12px; font-weight: 800; padding: 10px 20px; cursor: pointer; border: 0; min-height: 40px; transition: opacity 0.15s, background 0.15s; }
.sd-btn:disabled { opacity: 0.55; cursor: not-allowed; }
.sd-btn--surface { background: #e2e8f0; color: #0f172a; }
.sd-btn--surface:hover:not(:disabled) { background: #cbd5e1; }
.sd-btn--on-dark.sd-btn--ghost { background: rgba(255, 255, 255, 0.95); color: #002660; }
.sd-btn--on-dark.sd-btn--accent { background: #c2410c; color: #fff; }
.sd-btn--on-dark:hover:not(:disabled) { filter: brightness(1.05); }
.sd-state { padding: 24px; background: #fff; border: 1px solid #e2e8f0; border-radius: 10px; color: #64748b; font-size: 14px; }
.sd-state--error { color: #991b1b; border-color: #fecaca; background: #fef2f2; }
.sd-risk { display: flex; justify-content: space-between; align-items: center; gap: 16px; flex-wrap: wrap; padding: 20px 24px; background: rgba(254, 226, 226, 0.45); border-left: 8px solid #ba1a1a; border-radius: 8px; }
.sd-risk__left { display: flex; gap: 16px; align-items: flex-start; }
.sd-risk__icon { width: 44px; height: 44px; border-radius: 8px; background: #ba1a1a; color: #fff; font-weight: 900; font-size: 22px; display: grid; place-items: center; flex-shrink: 0; }
.sd-risk__title { margin: 0 0 6px; font-size: 16px; font-weight: 900; color: #7f1d1d; }
.sd-risk__desc { margin: 0; font-size: 13px; line-height: 1.5; color: #7f1d1d; max-width: 640px; }
.sd-risk__label { font-size: 10px; font-weight: 800; text-transform: uppercase; letter-spacing: 0.06em; color: rgba(127, 29, 29, 0.65); }
.sd-risk__level { font-size: 20px; font-weight: 900; color: #991b1b; }
.sd-bento { display: grid; grid-template-columns: 1fr; gap: 20px; }
@media (min-width: 1024px) { .sd-bento { grid-template-columns: minmax(380px, 420px) minmax(0, 1fr); align-items: start; } }
.sd-col { display: flex; flex-direction: column; gap: 20px; }
.sd-card { background: #fff; border: 1px solid #e8edf3; border-radius: 12px; padding: 20px 22px; box-shadow: 0 2px 12px rgba(15, 23, 42, 0.05); }
.sd-card__h { margin: 0 0 18px; font-size: 10px; font-weight: 900; text-transform: uppercase; letter-spacing: 0.12em; color: #64748b; display: flex; align-items: center; gap: 8px; }
.sd-card__h--inline { margin-bottom: 0; }
.sd-card__icon { opacity: 0.75; font-size: 12px; }
.sd-cover { aspect-ratio: 16 / 9; border-radius: 10px; overflow: hidden; background: linear-gradient(135deg, #e2e8f0, #f1f5f9); margin-bottom: 14px; }
.sd-cover__inner { height: 100%; padding: 16px; display: flex; flex-direction: column; justify-content: flex-end; background: linear-gradient(180deg, transparent 0%, rgba(0, 38, 96, 0.25) 100%); }
.sd-cover__tag { font-size: 10px; font-weight: 800; color: #fff; text-shadow: 0 1px 2px rgba(0, 0, 0, 0.35); }
.sd-cover__name { margin: 6px 0 0; font-size: 15px; font-weight: 800; color: #fff; text-shadow: 0 1px 3px rgba(0, 0, 0, 0.4); }
.sd-mini-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 10px; }
.sd-mini { padding: 10px 12px; background: #f1f5f9; border-radius: 8px; }
.sd-mini__label { display: block; font-size: 10px; font-weight: 800; color: #64748b; margin-bottom: 4px; }
.sd-mini__val { font-size: 13px; font-weight: 800; color: #0f172a; }
.sd-divider { height: 1px; background: #e8edf3; margin: 14px 0; }
.sd-kv { margin: 0; }
.sd-kv__row { display: flex; justify-content: space-between; gap: 12px; font-size: 12px; padding: 8px 0; border-bottom: 1px solid #f1f5f9; }
.sd-kv__row:last-child { border-bottom: 0; }
.sd-kv dt { color: #64748b; flex-shrink: 0; }
.sd-kv dd { margin: 0; text-align: right; font-weight: 600; color: #0f172a; }
.sd-kv__row--time dd { white-space: nowrap; font-size: 11px; }
.mono { font-family: ui-monospace, SFMono-Regular, Menlo, Consolas, monospace; }
.sd-timeline { position: relative; padding-left: 18px; }
.sd-timeline::before { content: ""; position: absolute; left: 5px; top: 6px; bottom: 6px; width: 1px; background: #cbd5e1; }
.sd-timeline__item { position: relative; padding-bottom: 18px; }
.sd-timeline__item:last-child { padding-bottom: 0; }
.sd-timeline__dot { position: absolute; left: -17px; top: 4px; width: 10px; height: 10px; border-radius: 50%; border: 3px solid #fff; box-shadow: 0 0 0 1px #cbd5e1; }
.sd-timeline__dot.is-on { background: #002660; }
.sd-timeline__dot.is-alert { background: #ba1a1a; }
.sd-timeline__dot.is-muted { background: #cbd5e1; }
.sd-timeline__title { font-size: 12px; font-weight: 800; color: #0f172a; }
.sd-timeline__sub { font-size: 10px; color: #64748b; margin-top: 4px; }
.sd-timeline-empty { color: #64748b; font-size: 12px; padding: 4px 0; }
.sd-pub { display: flex; justify-content: space-between; align-items: center; gap: 20px; flex-wrap: wrap; padding: 28px 32px; border-radius: 12px; background: linear-gradient(135deg, #002660 0%, #003a8c 100%); color: #fff; box-shadow: 0 12px 28px rgba(0, 38, 96, 0.28); }
.sd-pub__badge { display: inline-flex; align-items: center; gap: 8px; padding: 6px 10px; background: rgba(255, 255, 255, 0.12); border-radius: 6px; font-size: 10px; font-weight: 900; letter-spacing: 0.08em; text-transform: uppercase; margin-bottom: 12px; }
.sd-pub__pulse { width: 8px; height: 8px; border-radius: 50%; background: #fff; animation: sd-pulse 1.8s ease-in-out infinite; }
@keyframes sd-pulse { 0%, 100% { opacity: 1; } 50% { opacity: 0.35; } }
.sd-pub__title { margin: 0 0 8px; font-size: 22px; font-weight: 900; }
.sd-pub__desc { margin: 0; font-size: 13px; line-height: 1.55; color: rgba(255, 255, 255, 0.78); max-width: 520px; }
.sd-pub__actions { display: flex; gap: 10px; flex-wrap: wrap; align-items: center; }
.sd-pub__warn { margin: 0; font-size: 12px; color: rgba(255, 255, 255, 0.85); }
.sd-card--table { padding: 0; overflow: hidden; }
.sd-table-head { padding: 18px 22px; border-bottom: 1px solid #eef2f7; display: flex; justify-content: space-between; align-items: center; gap: 12px; flex-wrap: wrap; }
.sd-table-flag { display: inline-flex; align-items: center; gap: 8px; font-size: 10px; font-weight: 900; text-transform: uppercase; letter-spacing: 0.06em; color: #64748b; }
.sd-table-flag.is-fail { color: #991b1b; }
.sd-table-flag.is-pending { color: #b45309; }
.sd-table-flag__dot { width: 8px; height: 8px; border-radius: 50%; background: #16a34a; }
.sd-table-flag.is-fail .sd-table-flag__dot { background: #ba1a1a; }
.sd-table-flag.is-pending .sd-table-flag__dot { background: #eab308; }
.sd-table-wrap { overflow-x: auto; }
.sd-table { width: 100%; border-collapse: collapse; font-size: 13px; }
.sd-table thead tr { background: #e8edf3; }
.sd-table th { text-align: left; padding: 12px 20px; font-size: 10px; font-weight: 900; text-transform: uppercase; letter-spacing: 0.04em; color: #64748b; }
.sd-table td { padding: 14px 20px; border-bottom: 1px solid #f1f5f9; vertical-align: top; }
.sd-col-result { width: 140px; min-width: 140px; text-align: center; white-space: nowrap; }
.sd-col-result__cell { display: flex; justify-content: center; align-items: center; }
.sd-table tbody tr:nth-child(even) { background: #fafbfc; }
.sd-em { font-weight: 800; color: #0f172a; }
.muted { color: #475569; }
.sd-pill { display: inline-block; padding: 4px 8px; border-radius: 4px; font-size: 10px; font-weight: 900; text-transform: uppercase; }
.sd-pill.is-pass { background: #dbeafe; color: #1e40af; }
.sd-pill.is-fail { background: #fee2e2; color: #991b1b; }
.sd-pill.is-pending { background: #f1f5f9; color: #64748b; }
.sd-lab-notes { padding: 18px 22px; background: #f8fafc; display: block; }
.sd-note-block { padding: 14px; background: #fff; border: 1px dashed #cbd5e1; border-radius: 8px; }
.sd-note-block__label { display: block; font-size: 10px; font-weight: 800; color: #64748b; margin-bottom: 8px; }
.sd-note-block__text { margin: 0; font-size: 12px; line-height: 1.55; color: #334155; }
.sd-split { display: grid; grid-template-columns: 1fr; gap: 16px; }
@media (min-width: 800px) { .sd-split { grid-template-columns: 1fr 1fr; } }
.sd-card--full { grid-column: 1 / -1; }
.sd-card--accent2 { border-top: 4px solid #515e7f; }
.sd-subh { margin: 0 0 12px; font-size: 13px; font-weight: 900; color: #0f172a; display: flex; align-items: center; gap: 8px; }
.sd-plan { margin: 0; padding-left: 18px; font-size: 12px; line-height: 1.6; color: #334155; }
.sd-toast { position: fixed; right: 20px; bottom: 20px; padding: 12px 16px; border-radius: 8px; font-size: 13px; color: #fff; background: #0f172a; z-index: 1200; max-width: min(420px, 92vw); }
.sd-toast.error { background: #b91c1c; }
.sd-toast.success { background: #166534; }
</style>
