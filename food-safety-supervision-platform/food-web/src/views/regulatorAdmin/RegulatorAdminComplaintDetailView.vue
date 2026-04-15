<template>
  <RegulatorAdminWorkspacePage
    active-key="complaints"
    :username="regulatorUser.username"
    @navigate="handleSidebarNavigate"
    @logout="handleLogout"
  >
    <section class="complaint-detail-page">
      <div v-if="loading" class="state-card">加载投诉详情中...</div>
      <div v-else-if="!complaint" class="state-card state-card--error">投诉信息未找到</div>
      <template v-else>
        <header class="page-head">
          <div>
            <nav class="crumbs"><span>投诉流转</span><span class="sep">/</span><span>投诉详情</span></nav>
            <div class="title-row">
              <h1>{{ complaint.complaintNo || "-" }}</h1>
              <span class="status-chip">{{ formatComplaintStatus(complaint.status) }}</span>
            </div>
          </div>
          <div class="head-actions">
            <button class="ghost" type="button" @click="handleBack">返回列表</button>
            <button v-if="canReject" class="danger" type="button" :disabled="loadingAction" @click="handleRejectQuick">驳回投诉</button>
            <button v-if="canAssign" class="primary" type="button" :disabled="loadingAction" @click="scrollToActions">确认流转</button>
          </div>
        </header>

        <section class="timeline-card">
          <h3>投诉处理生命周期</h3>
          <div class="timeline">
            <div class="timeline-line timeline-line--all"></div>
            <div class="timeline-line timeline-line--done" :style="{ width: `${timelineProgress}%` }"></div>
            <article v-for="item in timelineItems" :key="item.key" class="timeline-node" :class="item.state">
              <span class="dot"></span><strong>{{ item.label }}</strong><em>{{ item.time }}</em>
            </article>
          </div>
        </section>

        <div class="content-grid">
          <div class="left-col">
            <div class="block-grid">
              <section class="panel">
                <h4>基本信息</h4>
                <dl class="kv-list">
                  <div><dt>投诉来源</dt><dd>公众投诉平台</dd></div>
                  <div><dt>投诉类别</dt><dd>{{ complaint.complaintType || "-" }}</dd></div>
                  <div><dt>投诉时间</dt><dd>{{ formatTime(complaint.createTime) }}</dd></div>
                  <div><dt>更新时间</dt><dd>{{ formatTime(complaint.updateTime) }}</dd></div>
                </dl>
              </section>
              <section class="panel">
                <h4>涉事企业</h4>
                <div class="enterprise-head">
                  <div class="enterprise-avatar">企</div>
                  <div>
                    <p class="name">{{ enterprise?.enterpriseName || complaint.enterpriseName || "-" }}</p>
                    <p class="sub">统一社会信用代码：{{ enterprise?.creditCode || "-" }}</p>
                  </div>
                </div>
                <div class="address-box"><span>经营地址</span><p>{{ enterprise?.addressDetail || "-" }}</p></div>
              </section>
            </div>
            <section class="panel panel-large">
              <h4>投诉详情描述</h4>
              <div class="content-box">{{ complaint.content || "-" }}</div>
              <div v-if="complaintImageList.length" class="image-grid">
                <button v-for="(url, index) in complaintImageList" :key="`${url}-${index}`" class="image-thumb" type="button" @click="openImagePreview(complaintImageList, index)">
                  <img :src="url" alt="投诉现场图片" />
                </button>
              </div>
              <div v-else class="muted-text">暂无现场图片</div>
            </section>
            <section class="panel panel-large">
              <h4>流转日志 / Processing Log</h4>
              <div class="logs">
                <article v-for="(entry, index) in processingLogs" :key="`${entry.title}-${index}`" class="log-item">
                  <span class="log-dot"></span>
                  <div class="log-main"><strong>{{ entry.title }}</strong><p>{{ entry.desc }}</p></div>
                  <time>{{ entry.time }}</time>
                </article>
              </div>
            </section>
          </div>

          <div class="right-col">
            <section class="panel panel-blue">
              <h4>当前执行人 / Enforcer</h4>
              <div class="enforcer-row">
                <div class="enforcer-avatar">{{ enforcerInitial }}</div>
                <div><p class="name">{{ complaint.assignedToName || "暂未指派" }}</p><p class="sub">指派时间：{{ formatTime(complaint.assignedTime) }}</p></div>
              </div>
              <dl class="enforcer-meta">
                <div><dt>联系电话</dt><dd>-</dd></div>
                <div><dt>办理时限</dt><dd>{{ formatTime(complaint.deadlineTime) }}</dd></div>
              </dl>
            </section>
            <section ref="actionPanelRef" class="panel">
              <h4>操作指令 / Actions</h4>
              <div class="action-stack">
                <div v-if="canAccept" class="action-tip">该投诉尚未受理，请先完成受理。<button class="primary" type="button" :disabled="loadingAction" @click="handleAccept">受理投诉</button></div>
                <template v-if="canAssign">
                  <label>指派执法人员<select v-model="assignForm.regulatorId"><option value="">请选择</option><option v-for="item in enforcers" :key="item.id" :value="item.id">{{ item.name }}</option></select></label>
                  <label>办理时限<input v-model="assignForm.deadlineTime" type="datetime-local" /></label>
                  <button class="primary" type="button" :disabled="loadingAction" @click="handleAssign">确认执行流转</button>
                </template>
                <template v-if="canReject">
                  <label>驳回原因<textarea v-model.trim="rejectForm.reason" rows="3" placeholder="请输入驳回原因"></textarea></label>
                  <button class="ghost" type="button" :disabled="loadingAction" @click="handleReject">提交驳回</button>
                </template>
              </div>
            </section>
          </div>
        </div>
        <div v-if="status.message" class="status" :class="status.type">{{ status.message }}</div>
      </template>
    </section>

    <div v-if="currentImagePreviewUrl" class="image-preview-mask" @click.self="closeImagePreview">
      <div class="image-preview-card">
        <img :src="currentImagePreviewUrl" alt="现场图片大图" />
        <div class="image-preview-actions">
          <button class="ghost" type="button" :disabled="imagePreviewIndex <= 0" @click="showPrevImage">上一张</button>
          <span>{{ imagePreviewIndex + 1 }}/{{ imagePreviewUrls.length }}</span>
          <button class="ghost" type="button" :disabled="imagePreviewIndex >= imagePreviewUrls.length - 1" @click="showNextImage">下一张</button>
          <button class="primary" type="button" @click="closeImagePreview">关闭</button>
        </div>
      </div>
    </div>
  </RegulatorAdminWorkspacePage>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from "vue";
import { useRoute, useRouter } from "vue-router";
import { acceptComplaint, assignComplaint, fetchComplaintDetail, rejectComplaint } from "../../api/complaint";
import { fetchEligibleRegulators } from "../../api/regulation";
import RegulatorAdminWorkspacePage from "../../components/regulatorAdmin/RegulatorAdminWorkspacePage.vue";
import { formatByMap, formatTime } from "../../utils/formatters";
import { complaintStatusMap } from "../../utils/statusMaps";
import { useRegulatorAdminShellSession } from "./regulatorAdminShared";

const route = useRoute();
const router = useRouter();
const { token, regulatorUser, handleSidebarNavigate, handleLogout } = useRegulatorAdminShellSession();
const loading = ref(false);
const loadingAction = ref(false);
const detail = ref(null);
const enforcers = ref([]);
const status = reactive({ message: "", type: "" });
const assignForm = reactive({ regulatorId: "", deadlineTime: "" });
const rejectForm = reactive({ reason: "" });
const imagePreviewUrls = ref([]);
const imagePreviewIndex = ref(0);
const actionPanelRef = ref(null);
const complaint = computed(() => detail.value?.complaint || null);
const enterprise = computed(() => detail.value?.enterprise || null);
const handles = computed(() => (Array.isArray(detail.value?.handles) ? detail.value.handles : []));
const currentImagePreviewUrl = computed(() => imagePreviewUrls.value[imagePreviewIndex.value] || "");
const complaintImageList = computed(() => complaint.value?.imageUrls || []);
const canAccept = computed(() => complaint.value?.status === "SUBMITTED");
const canAssign = computed(() => ["PENDING", "ASSIGNED", "PROCESSING"].includes(complaint.value?.status || ""));
const canReject = computed(() => ["SUBMITTED", "PENDING"].includes(complaint.value?.status || ""));
const enforcerInitial = computed(() => {
  const name = complaint.value?.assignedToName || "";
  return name ? name.slice(0, 1) : "执";
});
const timelineItems = computed(() => {
  const c = complaint.value;
  if (!c) return [];
  return [
    { key: "SUBMITTED", label: "已提交", time: formatTime(c.createTime), state: "done" },
    { key: "PENDING", label: "待审核", time: formatTime(c.acceptedTime), state: c.acceptedTime ? "done" : "waiting" },
    { key: "ASSIGNED", label: "已派发", time: formatTime(c.assignedTime), state: c.assignedTime ? "active" : "waiting" },
    { key: "PROCESSING", label: "处理中", time: formatTime(c.processedTime), state: c.status === "PROCESSING" ? "active" : c.processedTime ? "done" : "waiting" },
    { key: "FEEDBACKED", label: "已反馈", time: c.feedbackSummary ? formatTime(c.updateTime) : "--", state: c.status === "FEEDBACKED" ? "done" : "waiting" }
  ];
});
const timelineProgress = computed(() => {
  const nodes = timelineItems.value;
  if (!nodes.length) return 0;
  const done = nodes.filter((node) => node.state === "done" || node.state === "active").length;
  return ((Math.max(done - 1, 0)) / (nodes.length - 1)) * 100;
});
const processingLogs = computed(() => {
  const c = complaint.value;
  if (!c) return [];
  const rows = [];
  if (c.assignedTime) rows.push({ title: "投诉件已分配", desc: `系统分配至执法人员：${c.assignedToName || "-"}`, time: formatTime(c.assignedTime) });
  if (c.acceptedTime) rows.push({ title: "初审通过", desc: `受理人：${c.acceptedByName || "-"}`, time: formatTime(c.acceptedTime) });
  handles.value.forEach((item) => rows.push({ title: "处理反馈", desc: item.handleResult || "已提交处理记录", time: formatTime(item.handleTime) }));
  rows.push({ title: "系统接入手续", desc: "投诉平台同步数据完成。", time: formatTime(c.createTime) });
  return rows;
});
function formatComplaintStatus(value) { return formatByMap(value, complaintStatusMap); }
function setStatus(message = "", type = "") { status.message = message; status.type = type; }
function normalizeDateTime(value) { if (!value) return undefined; return value.length === 16 ? `${value}:00` : value; }
async function loadEnforcers(regionId) { enforcers.value = []; if (!regionId) return; try { const data = await fetchEligibleRegulators(token.value, regionId); enforcers.value = Array.isArray(data) ? data : []; } catch { enforcers.value = []; } }
async function loadDetail() {
  const complaintId = route.params.complaintId;
  if (!complaintId) { detail.value = null; return; }
  loading.value = true; setStatus("");
  try { detail.value = await fetchComplaintDetail(token.value, complaintId); assignForm.regulatorId = ""; assignForm.deadlineTime = ""; rejectForm.reason = ""; await loadEnforcers(detail.value?.enterprise?.regionId); }
  catch (error) { detail.value = null; setStatus(error.message || "加载投诉详情失败", "error"); }
  finally { loading.value = false; }
}
async function handleAccept() { if (!complaint.value?.id) return; loadingAction.value = true; setStatus(""); try { await acceptComplaint(token.value, complaint.value.id); setStatus("投诉已受理", "success"); await loadDetail(); } catch (error) { setStatus(error.message || "投诉受理失败", "error"); } finally { loadingAction.value = false; } }
async function handleAssign() { if (!complaint.value?.id) return; if (!assignForm.regulatorId) { setStatus("请选择执法人员", "error"); return; } loadingAction.value = true; setStatus(""); try { await assignComplaint(token.value, complaint.value.id, { regulatorId: assignForm.regulatorId, deadlineTime: normalizeDateTime(assignForm.deadlineTime) }); setStatus("派发成功", "success"); await loadDetail(); } catch (error) { setStatus(error.message || "派发失败", "error"); } finally { loadingAction.value = false; } }
async function handleReject() { if (!complaint.value?.id) return; if (!rejectForm.reason.trim()) { setStatus("请填写驳回原因", "error"); return; } loadingAction.value = true; setStatus(""); try { await rejectComplaint(token.value, complaint.value.id, { reason: rejectForm.reason }); setStatus("投诉已驳回", "success"); await loadDetail(); } catch (error) { setStatus(error.message || "驳回失败", "error"); } finally { loadingAction.value = false; } }
function handleRejectQuick() { actionPanelRef.value?.scrollIntoView({ behavior: "smooth", block: "center" }); }
function scrollToActions() { actionPanelRef.value?.scrollIntoView({ behavior: "smooth", block: "center" }); }
function openImagePreview(urls, index) { if (!Array.isArray(urls) || !urls.length) return; imagePreviewUrls.value = urls; imagePreviewIndex.value = Math.min(Math.max(index || 0, 0), urls.length - 1); }
function closeImagePreview() { imagePreviewUrls.value = []; imagePreviewIndex.value = 0; }
function showPrevImage() { if (imagePreviewIndex.value <= 0) return; imagePreviewIndex.value -= 1; }
function showNextImage() { if (imagePreviewIndex.value >= imagePreviewUrls.value.length - 1) return; imagePreviewIndex.value += 1; }
function handleBack() {
  const fromSection = typeof route.query.from === "string" ? route.query.from : "complaints";
  const routeNameMap = { enterprises: "regulator-admin-enterprises", approvals: "regulator-admin-approvals", dispatch: "regulator-admin-dispatch", sampling: "regulator-admin-sampling", inspections: "regulator-admin-dispatch-records", complaints: "regulator-admin-complaints", rectification: "regulator-admin-rectifications", warnings: "regulator-admin-warnings", bulletins: "regulator-admin-bulletins", stats: "regulator-admin-stats" };
  router.push({ name: routeNameMap[fromSection] || "regulator-admin-complaints" }).catch(() => {});
}
onMounted(loadDetail);
watch(() => route.params.complaintId, loadDetail);
</script>

<style scoped>
.complaint-detail-page { display: grid; gap: 16px; }
.state-card { padding: 20px; border-radius: 10px; border: 1px solid #e2e8f0; background: #fff; color: #64748b; }
.state-card--error { color: #991b1b; background: #fef2f2; border-color: #fecaca; }
.page-head { display: flex; justify-content: space-between; align-items: flex-end; gap: 14px; flex-wrap: wrap; }
.crumbs { display: flex; gap: 6px; color: #64748b; font-size: 11px; font-weight: 700; }
.sep { opacity: 0.55; }
.title-row { margin-top: 8px; display: flex; align-items: center; gap: 10px; flex-wrap: wrap; }
.title-row h1 { margin: 0; font-size: 30px; font-weight: 900; color: #002660; letter-spacing: -0.02em; }
.status-chip { display: inline-flex; min-height: 22px; align-items: center; padding: 0 10px; border-radius: 999px; background: #dbeafe; color: #1e3a8a; font-size: 10px; font-weight: 900; text-transform: uppercase; }
.head-actions { display: flex; gap: 8px; }
.primary, .ghost, .danger { border-radius: 8px; min-height: 38px; font-size: 12px; font-weight: 800; padding: 0 14px; cursor: pointer; }
.primary { border: 0; background: #002660; color: #fff; }
.ghost { border: 1px solid #d1d5db; background: #fff; color: #334155; }
.danger { border: 0; background: #ba1a1a; color: #fff; }
.timeline-card { background: #fff; border: 1px solid #e2e8f0; border-radius: 12px; padding: 16px; }
.timeline-card h3 { margin: 0 0 14px; color: #64748b; font-size: 11px; text-transform: uppercase; letter-spacing: 0.08em; }
.timeline { position: relative; display: grid; grid-template-columns: repeat(5, minmax(0, 1fr)); gap: 8px; }
.timeline-line { position: absolute; left: 0; right: 0; top: 16px; height: 2px; }
.timeline-line--all { background: #e2e8f0; }
.timeline-line--done { background: #003a8c; }
.timeline-node { position: relative; z-index: 1; display: grid; justify-items: center; gap: 6px; }
.timeline-node .dot { width: 14px; height: 14px; border-radius: 50%; border: 2px solid #cbd5e1; background: #fff; }
.timeline-node.done .dot { background: #003a8c; border-color: #003a8c; }
.timeline-node.active .dot { border-color: #003a8c; box-shadow: 0 0 0 4px rgba(0, 58, 140, 0.15); }
.timeline-node strong { font-size: 12px; color: #0f172a; }
.timeline-node em { font-style: normal; font-size: 10px; color: #94a3b8; }
.content-grid { display: grid; grid-template-columns: minmax(0, 1fr) 340px; gap: 14px; align-items: start; }
.left-col, .right-col { display: grid; gap: 14px; }
.block-grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 14px; }
.panel { background: #fff; border: 1px solid #e2e8f0; border-radius: 12px; padding: 14px; }
.panel-large { padding: 16px; }
.panel h4 { margin: 0 0 12px; color: #002660; font-size: 12px; font-weight: 900; text-transform: uppercase; letter-spacing: 0.08em; }
.kv-list { margin: 0; display: grid; gap: 10px; }
.kv-list div { display: flex; justify-content: space-between; gap: 12px; padding-bottom: 8px; border-bottom: 1px solid #f1f5f9; }
.kv-list dt { color: #64748b; font-size: 12px; }
.kv-list dd { margin: 0; color: #0f172a; font-size: 12px; font-weight: 700; text-align: right; }
.enterprise-head { display: flex; gap: 10px; align-items: flex-start; margin-bottom: 12px; }
.enterprise-avatar { width: 38px; height: 38px; border-radius: 8px; background: #eff6ff; color: #1e3a8a; display: grid; place-items: center; font-weight: 900; }
.name { margin: 0; font-size: 14px; font-weight: 800; color: #0f172a; }
.sub { margin: 4px 0 0; color: #64748b; font-size: 11px; }
.address-box { background: #f8fafc; border-radius: 8px; padding: 10px; }
.address-box span { display: block; font-size: 10px; color: #64748b; text-transform: uppercase; margin-bottom: 4px; letter-spacing: 0.05em; }
.address-box p { margin: 0; font-size: 12px; color: #0f172a; line-height: 1.5; }
.content-box { background: #f8fafc; border-radius: 8px; padding: 12px; color: #1e293b; font-size: 13px; line-height: 1.7; white-space: pre-line; }
.image-grid { margin-top: 12px; display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); gap: 10px; }
.image-thumb { padding: 0; border: 1px solid #e2e8f0; border-radius: 8px; overflow: hidden; background: #fff; cursor: pointer; }
.image-thumb img { display: block; width: 100%; height: 96px; object-fit: cover; }
.muted-text { color: #94a3b8; font-size: 12px; }
.logs { position: relative; display: grid; gap: 12px; }
.logs::before { content: ""; position: absolute; left: 5px; top: 8px; bottom: 8px; width: 2px; background: #e2e8f0; }
.log-item { position: relative; display: grid; grid-template-columns: 1fr auto; gap: 8px 12px; padding-left: 20px; }
.log-dot { position: absolute; left: 0; top: 4px; width: 12px; height: 12px; border-radius: 50%; background: #002660; border: 2px solid #fff; box-shadow: 0 0 0 1px #cbd5e1; }
.log-main strong { color: #0f172a; font-size: 12px; }
.log-main p { margin: 4px 0 0; color: #64748b; font-size: 12px; line-height: 1.5; }
.log-item time { color: #94a3b8; font-size: 10px; white-space: nowrap; }
.panel-blue { background: linear-gradient(135deg, #002660, #003a8c); color: #fff; border: 0; box-shadow: 0 12px 24px rgba(0, 38, 96, 0.22); }
.panel-blue h4 { color: rgba(255, 255, 255, 0.76); }
.enforcer-row { display: flex; align-items: center; gap: 10px; }
.enforcer-avatar { width: 40px; height: 40px; border-radius: 8px; background: rgba(255, 255, 255, 0.18); display: grid; place-items: center; font-size: 14px; font-weight: 900; }
.panel-blue .name { color: #fff; }
.panel-blue .sub { color: rgba(255, 255, 255, 0.75); }
.enforcer-meta { margin: 12px 0 0; display: grid; gap: 8px; }
.enforcer-meta div { display: flex; justify-content: space-between; font-size: 12px; }
.enforcer-meta dt { color: rgba(255, 255, 255, 0.65); }
.enforcer-meta dd { margin: 0; font-weight: 700; }
.action-stack { display: grid; gap: 10px; }
.action-stack label { display: grid; gap: 6px; color: #334155; font-size: 12px; font-weight: 700; }
.action-stack select, .action-stack input, .action-stack textarea { width: 100%; box-sizing: border-box; border: 1px solid #dbe2ea; border-radius: 8px; min-height: 38px; padding: 8px 10px; font-size: 13px; color: #0f172a; background: #fff; }
.action-stack textarea { min-height: 90px; resize: vertical; }
.action-tip { display: grid; gap: 8px; color: #475569; font-size: 12px; }
.status { position: fixed; right: 18px; bottom: 18px; border-radius: 8px; padding: 10px 12px; color: #fff; background: #0f172a; font-size: 13px; z-index: 1200; }
.status.error { background: #b91c1c; }
.status.success { background: #166534; }
.image-preview-mask { position: fixed; inset: 0; background: rgba(15, 23, 42, 0.55); display: grid; place-items: center; z-index: 9999; }
.image-preview-card { background: #fff; border-radius: 16px; padding: 16px; max-width: min(900px, 92vw); max-height: 88vh; display: grid; gap: 12px; }
.image-preview-card img { width: 100%; max-height: 70vh; object-fit: contain; border-radius: 12px; background: #f6f9ff; }
.image-preview-actions { display: flex; align-items: center; justify-content: center; gap: 10px; flex-wrap: wrap; }
@media (max-width: 1220px) { .content-grid { grid-template-columns: 1fr; } }
@media (max-width: 860px) { .block-grid { grid-template-columns: 1fr; } .timeline { grid-template-columns: 1fr 1fr; row-gap: 12px; } .timeline-line { display: none; } }
@media (max-width: 640px) { .title-row h1 { font-size: 24px; } .head-actions { width: 100%; display: grid; grid-template-columns: 1fr 1fr 1fr; } }
</style>
