<template>
  <RegulatorEnforcerWorkspacePage
    active-key="complaints"
    :username="enforcerUser.username"
    @navigate="handleSidebarNavigate"
    @logout="handleLogout"
  >
    <section class="complaint-detail-page">
      <div v-if="loading" class="state-card">加载投诉详情中...</div>
      <div v-else-if="!complaint" class="state-card state-card--error">投诉信息未找到。</div>
      <template v-else>
        <header class="page-head">
          <div>
            <nav class="crumbs">
              <span>投诉处理</span>
              <span class="sep">/</span>
              <span>投诉详情</span>
            </nav>
            <div class="title-row">
              <h1>{{ complaint.complaintNo || "-" }}</h1>
              <span class="status-chip" :class="statusClass(complaint.status)">{{ formatComplaintStatus(complaint.status) }}</span>
            </div>
          </div>
          <div class="head-actions">
            <button class="ghost" type="button" @click="handleBack">返回列表</button>
            <button
              v-if="canStart"
              class="primary"
              type="button"
              :disabled="loadingAction"
              @click="handleStart"
            >
              开始处理
            </button>
          </div>
        </header>

        <div class="content-grid">
          <div class="left-col">
            <section class="panel">
              <h4>投诉内容</h4>
              <div class="content-box">{{ complaint.content || "-" }}</div>
              <div v-if="complaintImageList.length" class="image-grid">
                <button
                  v-for="(url, index) in complaintImageList"
                  :key="`${url}-${index}`"
                  class="image-thumb"
                  type="button"
                  @click="openImagePreview(complaintImageList, index)"
                >
                  <img :src="url" alt="投诉现场图片" />
                </button>
              </div>
              <div v-else class="muted-text">当前未上传现场图片。</div>
            </section>

            <section class="panel">
              <div class="panel-head">
                <h4>投诉操作日志</h4>
                <button class="mini-link" type="button" :disabled="auditLoading" @click="loadAuditLogs">
                  {{ auditLoading ? "加载中..." : "刷新日志" }}
                </button>
              </div>
              <div v-if="auditLoading" class="muted-text">正在加载投诉操作日志...</div>
              <div v-else-if="auditError" class="muted-text muted-text--error">{{ auditError }}</div>
              <div v-else-if="!auditLogs.length" class="muted-text">当前暂无投诉操作日志。</div>
              <div v-else class="logs">
                <article v-for="item in auditLogs" :key="item.id" class="log-item">
                  <span class="log-dot"></span>
                  <div class="log-main">
                    <strong>{{ item.title }}</strong>
                    <p>{{ item.desc }}</p>
                    <p class="log-meta">
                      <span>{{ item.operatorName }}</span>
                      <span v-if="item.remark">· {{ item.remark }}</span>
                    </p>
                  </div>
                  <time>{{ formatTime(item.createTime) }}</time>
                </article>
              </div>
            </section>
          </div>

          <div class="right-col">
            <section class="panel panel-blue">
              <h4>基础信息</h4>
              <dl class="summary-list">
                <div><dt>投诉状态</dt><dd>{{ formatComplaintStatus(complaint.status) }}</dd></div>
                <div><dt>投诉方式</dt><dd>{{ complaint.anonymous ? "匿名投诉" : "实名投诉" }}</dd></div>
                <div><dt>投诉人</dt><dd>{{ complaint.complainantName || "-" }}</dd></div>
                <div><dt>联系方式</dt><dd>{{ complainantContactDisplay }}</dd></div>
                <div><dt>投诉时间</dt><dd>{{ formatTime(complaint.createTime) }}</dd></div>
                <div><dt>处理时限</dt><dd>{{ formatTime(complaint.deadlineTime) }}</dd></div>
                <div><dt>投诉企业</dt><dd>{{ enterprise?.enterpriseName || complaint.enterpriseName || "-" }}</dd></div>
              </dl>
            </section>

            <section class="panel">
              <h4>处理反馈</h4>
              <div v-if="canStart" class="action-tip">该投诉已派发给你，开始处理后即可提交反馈。</div>
              <label v-if="canHandle" class="form-label">
                反馈摘要
                <textarea
                  v-model.trim="handleForm.feedbackSummary"
                  rows="4"
                  placeholder="请输入本次处理结果摘要"
                ></textarea>
              </label>
              <button
                v-if="canHandle"
                class="primary"
                type="button"
                :disabled="loadingAction"
                @click="handleSubmit"
              >
                提交反馈
              </button>
              <div v-else-if="latestHandle" class="result-box">
                <strong>最近处理结果</strong>
                <p>{{ latestHandle.handleResult || latestHandle.feedbackSummary || complaint.feedbackSummary || "-" }}</p>
              </div>
              <div v-else class="muted-text">当前暂无处理反馈。</div>
            </section>
          </div>
        </div>

        <div v-if="status.message" class="status" :class="status.type">{{ status.message }}</div>
      </template>
    </section>

    <div v-if="currentImagePreviewUrl" class="image-preview-mask" @click.self="closeImagePreview">
      <div class="image-preview-card">
        <img :src="currentImagePreviewUrl" alt="投诉现场大图预览" />
        <div class="image-preview-actions">
          <button class="ghost" type="button" :disabled="imagePreviewIndex <= 0" @click="showPrevImage">上一张</button>
          <span>{{ imagePreviewIndex + 1 }}/{{ imagePreviewUrls.length }}</span>
          <button class="ghost" type="button" :disabled="imagePreviewIndex >= imagePreviewUrls.length - 1" @click="showNextImage">下一张</button>
          <button class="primary" type="button" @click="closeImagePreview">关闭</button>
        </div>
      </div>
    </div>
  </RegulatorEnforcerWorkspacePage>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from "vue";
import { useRoute, useRouter } from "vue-router";
import {
  fetchComplaintDetail,
  fetchComplaintLogs,
  handleComplaint,
  startComplaintProcess
} from "../../api/complaint";
import RegulatorEnforcerWorkspacePage from "../../components/regulatorEnforcer/RegulatorEnforcerWorkspacePage.vue";
import {
  formatComplaintAuditAction,
  formatComplaintAuditOperatorName,
  formatComplaintAuditSummary
} from "../../utils/complaintAudit";
import { formatByMap, formatTime } from "../../utils/formatters";
import { complaintStatusMap } from "../../utils/statusMaps";
import { resolveErrorMessage } from "../../utils/uiFeedback";
import { useRegulatorEnforcerShellSession } from "./regulatorEnforcerShared";

const route = useRoute();
const router = useRouter();
const { token, enforcerUser, handleSidebarNavigate, handleLogout } = useRegulatorEnforcerShellSession();

const loading = ref(false);
const loadingAction = ref(false);
const detail = ref(null);
const auditLogs = ref([]);
const auditLoading = ref(false);
const auditError = ref("");
const status = reactive({ message: "", type: "info" });
const handleForm = reactive({ feedbackSummary: "" });
const imagePreviewUrls = ref([]);
const imagePreviewIndex = ref(0);

const complaint = computed(() => detail.value?.complaint || null);
const enterprise = computed(() => detail.value?.enterprise || null);
const handles = computed(() => (Array.isArray(detail.value?.handles) ? detail.value.handles : []));
const latestHandle = computed(() => handles.value[0] || null);
const complaintImageList = computed(() => complaint.value?.imageUrls || []);
const currentImagePreviewUrl = computed(() => imagePreviewUrls.value[imagePreviewIndex.value] || "");
const complainantContactDisplay = computed(() =>
  complaint.value?.anonymous
    ? complaint.value?.contactMasked || complaint.value?.contact || "-"
    : complaint.value?.contact || "-"
);
const canStart = computed(() => complaint.value?.status === "ASSIGNED");
const canHandle = computed(() => complaint.value?.status === "PROCESSING");

function setStatus(message = "", type = "info") {
  status.message = message;
  status.type = type;
}

function formatComplaintStatus(value) {
  return formatByMap(value, complaintStatusMap);
}

function statusClass(value) {
  if (value === "PROCESSING") return "is-processing";
  if (value === "FEEDBACKED") return "is-feedbacked";
  if (value === "ASSIGNED") return "is-assigned";
  return "is-default";
}

async function loadAuditLogs() {
  if (!complaint.value?.id) {
    auditLogs.value = [];
    auditError.value = "";
    return;
  }
  auditLoading.value = true;
  auditError.value = "";
  try {
    const data = await fetchComplaintLogs(token.value, complaint.value.id, 12);
    auditLogs.value = (Array.isArray(data) ? data : []).map((item, index) => ({
      id: item.id || `complaint-log-${index}`,
      title: formatComplaintAuditAction(item.actionName || item.actionType),
      desc: formatComplaintAuditSummary(item),
      operatorName: formatComplaintAuditOperatorName(item.operatorName),
      remark: item.remark || "",
      createTime: item.createTime
    }));
  } catch (error) {
    auditLogs.value = [];
    auditError.value = resolveErrorMessage(error, "操作日志加载失败");
  } finally {
    auditLoading.value = false;
  }
}

async function loadDetail() {
  const complaintId = route.params.complaintId;
  if (!complaintId) {
    detail.value = null;
    auditLogs.value = [];
    auditError.value = "";
    return;
  }
  loading.value = true;
  setStatus("");
  try {
    detail.value = await fetchComplaintDetail(token.value, complaintId);
    handleForm.feedbackSummary = "";
    await loadAuditLogs();
  } catch (error) {
    detail.value = null;
    auditLogs.value = [];
    auditError.value = "";
    setStatus(resolveErrorMessage(error, "加载投诉详情失败"), "error");
  } finally {
    loading.value = false;
  }
}

async function handleStart() {
  if (!complaint.value?.id) return;
  loadingAction.value = true;
  setStatus("");
  try {
    await startComplaintProcess(token.value, complaint.value.id);
    setStatus("投诉已开始处理。", "success");
    await loadDetail();
  } catch (error) {
    setStatus(resolveErrorMessage(error, "开始处理失败"), "error");
  } finally {
    loadingAction.value = false;
  }
}

async function handleSubmit() {
  if (!complaint.value?.id) return;
  if (!handleForm.feedbackSummary.trim()) {
    setStatus("请填写处理反馈。", "error");
    return;
  }
  loadingAction.value = true;
  setStatus("");
  try {
    await handleComplaint(token.value, complaint.value.id, {
      feedbackSummary: handleForm.feedbackSummary,
      handleResult: handleForm.feedbackSummary
    });
    setStatus("投诉处理结果已反馈。", "success");
    await loadDetail();
  } catch (error) {
    setStatus(resolveErrorMessage(error, "提交反馈失败"), "error");
  } finally {
    loadingAction.value = false;
  }
}

function openImagePreview(urls, index) {
  if (!Array.isArray(urls) || !urls.length) return;
  imagePreviewUrls.value = urls;
  imagePreviewIndex.value = Math.min(Math.max(index || 0, 0), urls.length - 1);
}

function closeImagePreview() {
  imagePreviewUrls.value = [];
  imagePreviewIndex.value = 0;
}

function showPrevImage() {
  if (imagePreviewIndex.value <= 0) return;
  imagePreviewIndex.value -= 1;
}

function showNextImage() {
  if (imagePreviewIndex.value >= imagePreviewUrls.value.length - 1) return;
  imagePreviewIndex.value += 1;
}

function handleBack() {
  const fromSection = typeof route.query.from === "string" ? route.query.from : "complaints";
  const routeNameMap = {
    enterprises: "regulator-enforcer-enterprises",
    complaints: "regulator-enforcer-complaints",
    warnings: "regulator-enforcer-warnings",
    stats: "regulator-enforcer-stats"
  };
  router.push({ name: routeNameMap[fromSection] || "regulator-enforcer-complaints" }).catch(() => {});
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
.title-row h1 { margin: 0; font-size: 30px; font-weight: 900; color: #002660; }
.status-chip { display: inline-flex; min-height: 22px; align-items: center; padding: 0 10px; border-radius: 999px; font-size: 10px; font-weight: 900; }
.status-chip.is-default { background: #e2e8f0; color: #334155; }
.status-chip.is-assigned { background: #dbeafe; color: #1e3a8a; }
.status-chip.is-processing { background: #fef3c7; color: #92400e; }
.status-chip.is-feedbacked { background: #dcfce7; color: #166534; }
.head-actions { display: flex; gap: 8px; }
.primary, .ghost { border-radius: 8px; min-height: 38px; font-size: 12px; font-weight: 800; padding: 0 14px; cursor: pointer; }
.primary { border: 0; background: #002660; color: #fff; }
.ghost { border: 1px solid #d1d5db; background: #fff; color: #334155; }
.content-grid { display: grid; grid-template-columns: minmax(0, 1fr) 320px; gap: 14px; align-items: start; }
.left-col, .right-col { display: grid; gap: 14px; }
.panel { background: #fff; border: 1px solid #e2e8f0; border-radius: 12px; padding: 14px; }
.panel h4 { margin: 0 0 12px; color: #002660; font-size: 12px; font-weight: 900; text-transform: uppercase; letter-spacing: 0.08em; }
.panel-head { display: flex; align-items: center; justify-content: space-between; gap: 12px; margin-bottom: 12px; }
.mini-link { border: 0; background: transparent; color: #1d4ed8; font-size: 12px; font-weight: 700; cursor: pointer; padding: 0; }
.content-box { background: #f8fafc; border-radius: 8px; padding: 12px; color: #1e293b; font-size: 13px; line-height: 1.7; white-space: pre-line; }
.image-grid { margin-top: 12px; display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); gap: 10px; }
.image-thumb { padding: 0; border: 1px solid #e2e8f0; border-radius: 8px; overflow: hidden; background: #fff; cursor: pointer; }
.image-thumb img { display: block; width: 100%; height: 96px; object-fit: cover; }
.muted-text { color: #94a3b8; font-size: 12px; }
.muted-text--error { color: #b91c1c; }
.logs { position: relative; display: grid; gap: 12px; }
.logs::before { content: ""; position: absolute; left: 5px; top: 8px; bottom: 8px; width: 2px; background: #e2e8f0; }
.log-item { position: relative; display: grid; grid-template-columns: 1fr auto; gap: 8px 12px; padding-left: 20px; }
.log-dot { position: absolute; left: 0; top: 4px; width: 12px; height: 12px; border-radius: 50%; background: #002660; border: 2px solid #fff; box-shadow: 0 0 0 1px #cbd5e1; }
.log-main strong { color: #0f172a; font-size: 12px; }
.log-main p { margin: 4px 0 0; color: #64748b; font-size: 12px; line-height: 1.5; }
.log-main .log-meta { color: #94a3b8; font-size: 11px; }
.log-item time { color: #94a3b8; font-size: 10px; white-space: nowrap; }
.panel-blue { background: linear-gradient(135deg, #002660, #003a8c); color: #fff; border: 0; box-shadow: 0 12px 24px rgba(0, 38, 96, 0.22); }
.panel-blue h4 { color: rgba(255, 255, 255, 0.76); }
.summary-list { margin: 0; display: grid; gap: 10px; }
.summary-list div { display: grid; gap: 4px; }
.summary-list dt { color: rgba(255, 255, 255, 0.68); font-size: 11px; }
.summary-list dd { margin: 0; color: #fff; font-size: 13px; font-weight: 700; }
.action-tip { color: #475569; font-size: 12px; line-height: 1.6; }
.form-label { display: grid; gap: 6px; color: #334155; font-size: 12px; font-weight: 700; }
.form-label textarea { width: 100%; box-sizing: border-box; min-height: 110px; border: 1px solid #dbe2ea; border-radius: 8px; padding: 10px; resize: vertical; font-size: 13px; }
.result-box { display: grid; gap: 6px; background: #f8fafc; border-radius: 8px; padding: 12px; }
.result-box strong { color: #0f172a; font-size: 12px; }
.result-box p { margin: 0; color: #475569; font-size: 12px; line-height: 1.6; white-space: pre-line; }
.status { position: fixed; right: 18px; bottom: 18px; border-radius: 8px; padding: 10px 12px; color: #fff; background: #0f172a; font-size: 13px; z-index: 1200; }
.status.error { background: #b91c1c; }
.status.success { background: #166534; }
.image-preview-mask { position: fixed; inset: 0; background: rgba(15, 23, 42, 0.55); display: grid; place-items: center; z-index: 9999; }
.image-preview-card { background: #fff; border-radius: 16px; padding: 16px; max-width: min(900px, 92vw); max-height: 88vh; display: grid; gap: 12px; }
.image-preview-card img { width: 100%; max-height: 70vh; object-fit: contain; border-radius: 12px; background: #f6f9ff; }
.image-preview-actions { display: flex; align-items: center; justify-content: center; gap: 10px; flex-wrap: wrap; }
@media (max-width: 1080px) { .content-grid { grid-template-columns: 1fr; } }
@media (max-width: 720px) { .image-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); } }
</style>
