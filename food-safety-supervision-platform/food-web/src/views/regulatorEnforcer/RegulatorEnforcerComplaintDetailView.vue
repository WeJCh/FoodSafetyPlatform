<template>
  <RegulatorEnforcerWorkspacePage
    active-key="complaints"
    :username="enforcerUser.username || enforcerUser.realName || ''"
    @navigate="handleSidebarNavigate"
    @logout="handleLogout"
    @pending-feature="regulatorEnforcerFeaturePendingNotice"
  >
    <section class="complaint-detail-page">
      <div v-if="loading" class="state-card">加载投诉详情中...</div>
      <div v-else-if="!complaint" class="state-card state-card--error">投诉信息未找到</div>
      <template v-else>
        <header class="page-head">
          <div>
            <div class="chips">
              <span class="chip chip-id">{{ complaint.complaintNo || `#${complaint.id}` }}</span>
              <span class="chip chip-status" :class="statusClass(complaint.status)">
                {{ formatComplaintStatus(complaint.status) }}
              </span>
            </div>
            <h1>投诉执法处理</h1>
            <p>{{ complaint.enterpriseName || enterprise?.enterpriseName || "-" }}</p>
          </div>
          <div class="head-actions">
            <button class="ghost" type="button" @click="handleBack">返回列表</button>
            <button v-if="canStart" class="primary" type="button" :disabled="loadingAction" @click="handleStart">开始处理</button>
            <button v-if="canHandle" class="primary" type="button" :disabled="loadingAction" @click="handleSubmit">提交处理</button>
          </div>
        </header>

        <div class="content-grid">
          <div class="left-col">
            <section class="panel">
              <h2>投诉内容</h2>
              <p class="content-text">{{ complaint.content || "-" }}</p>
            </section>

            <section class="panel">
              <h2>反馈处理</h2>
              <div v-if="canStart" class="note">该投诉已派发，开始处理后即可提交反馈。</div>
              <div v-if="complaint.deadlineTime" class="note">办理时限：{{ formatTime(complaint.deadlineTime) }}</div>
              <label v-if="canHandle" class="form-label">
                反馈摘要
                <textarea
                  v-model.trim="handleForm.feedbackSummary"
                  rows="5"
                  placeholder="请输入面向公众展示的处理反馈"
                ></textarea>
              </label>
              <article v-if="latestHandle || complaint.feedbackSummary" class="result-card">
                <strong>最近处理结果</strong>
                <p>{{ complaint.feedbackSummary || latestHandle?.handleResult || "-" }}</p>
                <span>{{ latestHandle?.handlerName || "-" }} · {{ formatTime(latestHandle?.handleTime || complaint.updateTime) }}</span>
              </article>
            </section>

            <section class="panel">
              <h2>现场图片</h2>
              <div v-if="complaintImageList.length" class="image-grid">
                <button
                  v-for="(url, index) in complaintImageList"
                  :key="`${url}-${index}`"
                  class="image-thumb"
                  type="button"
                  @click="openImagePreview(complaintImageList, index)"
                >
                  <img :src="url" alt="现场图片" />
                </button>
              </div>
              <div v-else class="note">暂无现场图片</div>
            </section>
          </div>

          <aside class="right-col">
            <section class="panel panel-blue">
              <h2>办理概览</h2>
              <dl class="summary-list">
                <div><dt>投诉状态</dt><dd>{{ formatComplaintStatus(complaint.status) }}</dd></div>
                <div><dt>投诉时间</dt><dd>{{ formatTime(complaint.createTime) }}</dd></div>
                <div><dt>更新时间</dt><dd>{{ formatTime(complaint.updateTime) }}</dd></div>
                <div><dt>办理时限</dt><dd>{{ formatTime(complaint.deadlineTime) }}</dd></div>
              </dl>
            </section>

            <section class="panel">
              <h2>企业信息</h2>
              <div class="info-list">
                <p><span>企业名称</span><strong>{{ enterprise?.enterpriseName || complaint.enterpriseName || "-" }}</strong></p>
                <p><span>负责人</span><strong>{{ enterprise?.principal || "-" }}</strong></p>
                <p><span>统一社会信用代码</span><strong>{{ enterprise?.creditCode || "-" }}</strong></p>
                <p><span>联系电话</span><strong>{{ enterprise?.principalPhone || "-" }}</strong></p>
              </div>
              <div class="address-box">
                <span>经营地址</span>
                <p>{{ enterprise?.addressDetail || "-" }}</p>
              </div>
            </section>

            <section class="panel">
              <h2>处理记录</h2>
              <div class="logs">
                <article v-for="(entry, index) in processingLogs" :key="`${entry.title}-${index}`" class="log-item">
                  <strong>{{ entry.title }}</strong>
                  <span>{{ entry.time }}</span>
                  <p>{{ entry.desc }}</p>
                </article>
              </div>
            </section>
          </aside>
        </div>

        <div v-if="status.message" class="status-banner" :class="`is-${status.type}`">{{ status.message }}</div>
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
  </RegulatorEnforcerWorkspacePage>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from "vue";
import { useRoute, useRouter } from "vue-router";
import { fetchComplaintDetail, handleComplaint, startComplaintProcess } from "../../api/complaint";
import RegulatorEnforcerWorkspacePage from "../../components/regulatorEnforcer/RegulatorEnforcerWorkspacePage.vue";
import { formatByMap, formatTime } from "../../utils/formatters";
import { complaintStatusMap } from "../../utils/statusMaps";
import {
  regulatorEnforcerFeaturePendingNotice,
  useRegulatorEnforcerShellSession
} from "./regulatorEnforcerShared";

const route = useRoute();
const router = useRouter();
const { token, enforcerUser, handleSidebarNavigate, handleLogout } = useRegulatorEnforcerShellSession();

const loading = ref(false);
const loadingAction = ref(false);
const detail = ref(null);
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
const canStart = computed(() => complaint.value?.status === "ASSIGNED");
const canHandle = computed(() => complaint.value?.status === "PROCESSING");

const processingLogs = computed(() => {
  if (!complaint.value) return [];
  const rows = [];
  rows.push({
    title: "投诉提交",
    time: formatTime(complaint.value.createTime),
    desc: "公众投诉已进入执法流转。"
  });
  if (complaint.value.assignedTime) {
    rows.push({
      title: "投诉派发",
      time: formatTime(complaint.value.assignedTime),
      desc: `已派发给 ${complaint.value.assignedToName || "当前执法人员"}`
    });
  }
  handles.value.forEach((item) => {
    rows.push({
      title: "处理反馈",
      time: formatTime(item.handleTime),
      desc: item.handleResult || item.feedbackSummary || "已提交处理记录"
    });
  });
  return rows.reverse();
});

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

async function loadDetail() {
  const complaintId = route.params.complaintId;
  if (!complaintId) {
    detail.value = null;
    return;
  }
  loading.value = true;
  setStatus("");
  try {
    detail.value = await fetchComplaintDetail(token.value, complaintId);
    handleForm.feedbackSummary = "";
  } catch (error) {
    detail.value = null;
    setStatus(error.message || "加载投诉详情失败", "error");
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
    setStatus("投诉已开始处理", "success");
    await loadDetail();
  } catch (error) {
    setStatus(error.message || "开始处理失败", "error");
  } finally {
    loadingAction.value = false;
  }
}

async function handleSubmit() {
  if (!complaint.value?.id) return;
  if (!handleForm.feedbackSummary.trim()) {
    setStatus("请填写反馈摘要", "error");
    return;
  }
  loadingAction.value = true;
  setStatus("");
  try {
    await handleComplaint(token.value, complaint.value.id, {
      feedbackSummary: handleForm.feedbackSummary
    });
    setStatus("投诉处理已完成", "success");
    await loadDetail();
  } catch (error) {
    setStatus(error.message || "提交处理失败", "error");
  } finally {
    loadingAction.value = false;
  }
}

function handleBack() {
  const fromSection = typeof route.query.from === "string" ? route.query.from : "complaints";
  const routeNameMap = {
    enterprises: "regulator-enforcer-enterprises",
    tasks: "regulator-enforcer-tasks",
    sampling: "regulator-enforcer-sampling",
    inspections: "regulator-enforcer-inspections",
    complaints: "regulator-enforcer-complaints",
    rectifications: "regulator-enforcer-rectifications",
    warnings: "regulator-enforcer-warnings",
    stats: "regulator-enforcer-stats"
  };
  router.push({ name: routeNameMap[fromSection] || "regulator-enforcer-complaints" }).catch(() => {});
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

onMounted(loadDetail);
watch(() => route.params.complaintId, loadDetail);
</script>

<style scoped>
.complaint-detail-page {
  display: grid;
  gap: 14px;
}
.state-card {
  padding: 20px;
  border: 1px solid #dbe3ee;
  background: #fff;
  color: #64748b;
}
.state-card--error {
  color: #b91c1c;
  background: #fef2f2;
  border-color: #fecaca;
}
.page-head {
  display: flex;
  justify-content: space-between;
  gap: 14px;
  align-items: end;
  padding: 16px;
  border: 1px solid #dbe3ee;
  background: #fff;
}
.chips,
.head-actions {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}
.chip {
  display: inline-flex;
  align-items: center;
  min-height: 24px;
  padding: 0 10px;
  border-radius: 999px;
  font-size: 11px;
  font-weight: 800;
}
.chip-id {
  background: #dbeafe;
  color: #1e3a8a;
}
.chip-status.is-processing { background: #ecfeff; color: #155e75; }
.chip-status.is-feedbacked { background: #dcfce7; color: #166534; }
.chip-status.is-assigned { background: #ffedd5; color: #9a3412; }
.chip-status.is-default { background: #f1f5f9; color: #475569; }
h1 {
  margin: 10px 0 0;
  color: #002660;
  font-size: 28px;
}
.page-head p {
  margin: 6px 0 0;
  color: #64748b;
  font-size: 13px;
}
.primary,
.ghost {
  min-height: 36px;
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
.content-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.5fr) minmax(300px, 1fr);
  gap: 14px;
}
.left-col,
.right-col {
  display: grid;
  gap: 14px;
}
.panel {
  padding: 16px;
  border: 1px solid #dbe3ee;
  background: #fff;
}
.panel h2 {
  margin: 0 0 12px;
  color: #002660;
  font-size: 14px;
  font-weight: 800;
}
.panel-blue {
  background: linear-gradient(135deg, #002660, #003a8c);
  border-color: #003a8c;
  color: #fff;
}
.panel-blue h2 {
  color: #fff;
}
.content-text {
  margin: 0;
  white-space: pre-line;
  line-height: 1.7;
  color: #1e293b;
}
.note {
  margin-bottom: 10px;
  padding: 10px 12px;
  background: #f8fafc;
  border: 1px solid #dbe3ee;
  color: #475569;
  font-size: 12px;
}
.form-label {
  display: grid;
  gap: 8px;
  font-size: 12px;
  color: #334155;
  font-weight: 700;
}
textarea {
  width: 100%;
  box-sizing: border-box;
  border: 1px solid #dbe3ee;
  background: #fff;
  padding: 10px;
  resize: vertical;
}
.result-card {
  margin-top: 12px;
  padding: 12px;
  border: 1px solid #dbe3ee;
  background: #f8fafc;
}
.result-card strong {
  display: block;
  margin-bottom: 8px;
}
.result-card p {
  margin: 0;
  color: #1e293b;
}
.result-card span {
  display: block;
  margin-top: 8px;
  color: #64748b;
  font-size: 12px;
}
.image-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(120px, 1fr));
  gap: 10px;
}
.image-thumb {
  padding: 0;
  border: 1px solid #dbe3ee;
  background: #fff;
  cursor: pointer;
  overflow: hidden;
}
.image-thumb img {
  width: 100%;
  height: 96px;
  object-fit: cover;
  display: block;
}
.summary-list,
.logs {
  display: grid;
  gap: 10px;
}
.summary-list div {
  display: flex;
  justify-content: space-between;
  gap: 10px;
  font-size: 12px;
}
.summary-list dt {
  color: rgba(255, 255, 255, 0.72);
}
.summary-list dd {
  margin: 0;
  font-weight: 700;
}
.info-list p {
  margin: 0 0 10px;
  display: flex;
  justify-content: space-between;
  gap: 8px;
  font-size: 12px;
}
.info-list span {
  color: #64748b;
}
.info-list strong {
  text-align: right;
  color: #0f172a;
}
.address-box {
  margin-top: 10px;
  padding: 10px;
  background: #f8fafc;
}
.address-box span {
  display: block;
  margin-bottom: 4px;
  color: #64748b;
  font-size: 12px;
}
.address-box p {
  margin: 0;
  color: #1e293b;
  line-height: 1.6;
}
.log-item {
  padding-left: 12px;
  border-left: 2px solid #dbeafe;
}
.log-item strong,
.log-item span {
  display: block;
}
.log-item span {
  margin-top: 4px;
  color: #64748b;
  font-size: 12px;
}
.log-item p {
  margin: 6px 0 0;
  color: #1e293b;
  font-size: 12px;
  line-height: 1.6;
}
.status-banner {
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
.image-preview-mask {
  position: fixed;
  inset: 0;
  background: rgba(15, 23, 42, 0.55);
  display: grid;
  place-items: center;
  z-index: 9999;
}
.image-preview-card {
  background: #fff;
  padding: 16px;
  max-width: min(900px, 92vw);
  max-height: 88vh;
  display: grid;
  gap: 12px;
}
.image-preview-card img {
  width: 100%;
  max-height: 70vh;
  object-fit: contain;
  background: #f6f9ff;
}
.image-preview-actions {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  flex-wrap: wrap;
}
@media (max-width: 1100px) {
  .content-grid {
    grid-template-columns: 1fr;
  }
  .page-head {
    flex-direction: column;
    align-items: stretch;
  }
}
</style>
