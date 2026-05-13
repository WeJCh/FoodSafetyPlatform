<template>
  <RegulatorAdminWorkspacePage
    :active-key="activeNavKey"
    :username="regulatorUser.username"
    @navigate="handleSidebarNavigate"
    @logout="handleLogout"
  >
    <section class="enterprise-detail-page">
      <div v-if="loading" class="state-card">企业详情加载中...</div>
      <div v-else-if="loadError" class="state-card state-card--error">{{ loadError }}</div>

      <template v-else-if="detail">
        <header class="hero">
          <div class="hero-topbar">
            <nav class="crumbs">
              <button class="crumb-link" type="button" @click="handleBack">企业名录管理</button>
              <span>/</span>
              <span>{{ detail.enterpriseName || "企业详情" }}</span>
            </nav>
            <button class="hero-back-button" type="button" @click="handleBack">返回列表</button>
          </div>

          <div class="hero-main">
            <div class="hero-title-row">
              <h1>{{ detail.enterpriseName || "企业详情" }}</h1>
              <span class="status-pill" :class="approvalChipClass(detail.approvalStatus)">
                {{ formatApprovalStatus(detail.approvalStatus) }}
              </span>
              <span class="status-pill" :class="statusChipClass(detail.status)">
                {{ formatStatus(detail.status) }}
              </span>
            </div>
            <p class="hero-desc">
              统一社会信用代码：{{ detail.creditCode || "-" }}；所属区域：{{ regionName || "-" }}
            </p>
            <div class="hero-meta-row">
              <article class="hero-meta-card">
                <span>企业编号</span>
                <strong>ENT-{{ route.params.enterpriseId || "-" }}</strong>
              </article>
              <article class="hero-meta-card">
                <span>包保责任人</span>
                <strong>{{ detail.regulatorName || "-" }}</strong>
              </article>
              <article class="hero-meta-card">
                <span>最后更新</span>
                <strong>{{ formatTime(detail.updateTime) }}</strong>
              </article>
            </div>
          </div>
        </header>

        <div class="content-grid">
          <div class="main-col">
            <section class="panel">
              <div class="section-head">
                <h2>企业基础信息</h2>
                <span class="section-hint">档案主体信息</span>
              </div>
              <div class="summary-grid">
                <article>
                  <span>法定代表人</span>
                  <strong>{{ detail.legalRepresentative || "-" }}</strong>
                </article>
                <article>
                  <span>负责人</span>
                  <strong>{{ detail.principal || "-" }}</strong>
                </article>
                <article>
                  <span>负责人电话</span>
                  <strong>{{ detail.principalPhone || "-" }}</strong>
                </article>
                <article>
                  <span>食品经营许可证号</span>
                  <strong>{{ detail.licenseNo || "-" }}</strong>
                </article>
                <article>
                  <span>所属区域</span>
                  <strong>{{ regionName || "-" }}</strong>
                </article>
                <article>
                  <span>监管分级</span>
                  <strong>{{ formatStatus(detail.status) }}</strong>
                </article>
              </div>
              <div class="detail-block">
                <label>详细地址</label>
                <p>{{ detail.addressDetail || "-" }}</p>
              </div>
            </section>

            <section class="panel">
              <div class="section-head">
                <h2>审核信息</h2>
                <span class="section-hint">准入与责任落实</span>
              </div>
              <div class="summary-grid">
                <article>
                  <span>审核人</span>
                  <strong>{{ detail.approvedByName || "-" }}</strong>
                </article>
                <article>
                  <span>审核时间</span>
                  <strong>{{ formatTime(detail.approvedTime) }}</strong>
                </article>
                <article>
                  <span>包保责任人</span>
                  <strong>{{ detail.regulatorName || "-" }}</strong>
                </article>
                <article>
                  <span>审核状态</span>
                  <strong>{{ formatApprovalStatus(detail.approvalStatus) }}</strong>
                </article>
              </div>
              <div class="detail-block">
                <label>审核意见</label>
                <p>{{ detail.approvalComment || "-" }}</p>
              </div>
            </section>

            <section class="panel">
              <div class="section-head">
                <h2>产品档案</h2>
                <span class="section-hint">{{ productRecords.length }} 项</span>
              </div>
              <div v-if="productLoading" class="empty-box">产品档案加载中...</div>
              <div v-else-if="productLoadError" class="empty-box">{{ productLoadError }}</div>
              <div v-else-if="!productRecords.length" class="empty-box">当前企业暂无产品档案。</div>
              <div v-else class="table-wrap">
                <table>
                  <thead>
                    <tr>
                      <th>产品名称</th>
                      <th>产品分类</th>
                      <th>规格型号</th>
                      <th>状态</th>
                      <th>最后更新</th>
                    </tr>
                  </thead>
                  <tbody>
                    <tr v-for="item in productRecords" :key="item.id">
                      <td>{{ item.productName || "-" }}</td>
                      <td>{{ item.category || "-" }}</td>
                      <td>{{ item.specification || "-" }}</td>
                      <td>
                        <span class="inline-pill" :class="item.status === 'ACTIVE' ? 'is-success' : 'is-neutral'">
                          {{ formatProductStatus(item.status) }}
                        </span>
                      </td>
                      <td>{{ formatTime(item.updateTime) }}</td>
                    </tr>
                  </tbody>
                </table>
              </div>
            </section>
          </div>

          <aside class="side-col">
            <section class="panel panel-accent">
              <h2>企业附件</h2>
              <div v-if="!detail.attachments || !detail.attachments.length" class="empty-box empty-box--dark">
                当前企业暂无可查看附件。
              </div>
              <div v-else class="mini-list">
                <a
                  v-for="(item, index) in detail.attachments"
                  :key="`${item.type || 'attachment'}-${index}`"
                  class="attachment-link"
                  :href="item.url"
                  target="_blank"
                  rel="noreferrer"
                >
                  <strong>{{ item.label || item.name || "备案附件" }}</strong>
                  <span>{{ item.name || "未命名附件" }}</span>
                </a>
              </div>
            </section>

            <section class="panel">
              <div class="section-head">
                <h2>重点监管原因</h2>
                <span class="section-hint">最近命中记录</span>
              </div>
              <div v-if="!riskHistory.length" class="empty-box">暂无风险历史。</div>
              <div v-else class="timeline">
                <article v-for="(item, index) in riskHistory" :key="`${item.reasonType}-${index}`" class="timeline-item">
                  <span class="timeline-dot"></span>
                  <div class="timeline-main">
                    <div class="timeline-head">
                      <strong>{{ formatReasonType(item.reasonType) }}</strong>
                      <time>{{ formatTime(item.createTime) }}</time>
                    </div>
                    <p>{{ item.reasonDetail || "命中重点监管规则" }}</p>
                  </div>
                </article>
              </div>
            </section>

            <section class="panel">
              <div class="section-head">
                <h2>企业审核日志</h2>
                <span class="section-hint">{{ auditLogs.length }} 条</span>
              </div>
              <div v-if="auditLogs.length" class="timeline">
                <article v-for="item in auditLogs" :key="item.id" class="timeline-item">
                  <span class="timeline-dot timeline-dot--muted"></span>
                  <div class="timeline-main">
                    <div class="timeline-head">
                      <strong>{{ item.title }}</strong>
                      <time>{{ item.time }}</time>
                    </div>
                    <p>{{ item.desc }}</p>
                    <p class="timeline-meta">{{ item.meta }}</p>
                  </div>
                </article>
              </div>
              <div v-else class="empty-box">当前暂无企业审核日志。</div>
            </section>
          </aside>
        </div>
      </template>
    </section>
  </RegulatorAdminWorkspacePage>
</template>

<script setup>
import { computed, onMounted, ref, watch } from "vue";
import { useRoute } from "vue-router";
import {
  fetchEnterpriseAuditLogs,
  fetchEnterpriseDetail,
  fetchEnterpriseProducts,
  fetchRegionPath
} from "../../api/regulation";
import RegulatorAdminWorkspacePage from "../../components/regulatorAdmin/RegulatorAdminWorkspacePage.vue";
import { formatByMap, formatTime } from "../../utils/formatters";
import { approvalStatusMap, enterpriseStatusMap } from "../../utils/statusMaps";
import { resolveErrorMessage } from "../../utils/uiFeedback";
import { useRegulatorAdminShellSession } from "./regulatorAdminShared";

const route = useRoute();
const { regulatorUser, token, handleSidebarNavigate, handleLogout } = useRegulatorAdminShellSession();

const loading = ref(false);
const detail = ref(null);
const regionName = ref("");
const productLoading = ref(false);
const productRecords = ref([]);
const productLoadError = ref("");
const loadError = ref("");
const auditLogs = ref([]);

const activeNavKey = computed(() => {
  const from = typeof route.query.from === "string" ? route.query.from : "enterprises";
  const allowed = new Set([
    "enterprises",
    "approvals",
    "dispatch",
    "sampling",
    "inspections",
    "complaints",
    "rectification",
    "warnings",
    "bulletins",
    "stats",
    "overview"
  ]);
  return allowed.has(from) ? from : "enterprises";
});

const riskHistory = computed(() => {
  if (!detail.value?.keyReasons?.length) return [];
  return detail.value.keyReasons.slice(0, 6);
});

function handleBack() {
  handleSidebarNavigate(activeNavKey.value);
}

function formatStatus(value) {
  return formatByMap(value, enterpriseStatusMap);
}

function formatApprovalStatus(value) {
  return formatByMap(value, approvalStatusMap);
}

function approvalChipClass(value) {
  if (value === "APPROVED") return "is-success";
  if (value === "PENDING") return "is-warning";
  if (value === "REJECTED") return "is-danger";
  return "is-neutral";
}

function statusChipClass(value) {
  if (value === "KEY") return "is-danger";
  if (value === "NORMAL") return "is-neutral";
  return "is-neutral";
}

function formatProductStatus(value) {
  const map = { ACTIVE: "启用", INACTIVE: "停用" };
  return map[value] || value || "-";
}

function formatReasonType(value) {
  const map = {
    COMPLAINT_OVERFLOW: "投诉过量",
    CONSECUTIVE_INSPECTION_FAIL: "连续检查不合格",
    SAMPLING_FAIL: "抽检不合格",
    RECTIFICATION_OVERDUE: "整改逾期",
    WARNING_TRIGGERED: "预警触发",
    MANUAL_SET: "人工设定"
  };
  return map[value] || value || "-";
}

async function loadDetail() {
  const enterpriseId = route.params.enterpriseId;
  if (!enterpriseId) {
    detail.value = null;
    productRecords.value = [];
    productLoadError.value = "";
    auditLogs.value = [];
    loadError.value = "缺少企业参数";
    return;
  }

  loading.value = true;
  loadError.value = "";
  productLoadError.value = "";
  try {
    const [enterprise, logs] = await Promise.all([
      fetchEnterpriseDetail(token.value, enterpriseId),
      fetchEnterpriseAuditLogs(token.value, enterpriseId, 8).catch(() => [])
    ]);
    detail.value = enterprise;
    auditLogs.value = (Array.isArray(logs) ? logs : []).map((item, index) => ({
      id: item.id || `audit-${index}`,
      title: item.actionName || item.actionType || "企业审核日志",
      desc: item.summary || "暂无日志摘要",
      meta: `${item.operatorName || "系统"}${item.remark ? ` · ${item.remark}` : ""}`,
      time: formatTime(item.createTime)
    }));

    regionName.value = "";
    if (detail.value?.regionId) {
      const path = await fetchRegionPath(token.value, detail.value.regionId).catch(() => []);
      regionName.value = Array.isArray(path) && path.length ? path.map((item) => item.name).join(" / ") : "";
    }

    productLoading.value = true;
    try {
      const products = await fetchEnterpriseProducts(token.value, enterpriseId);
      productRecords.value = Array.isArray(products) ? products : [];
    } catch (error) {
      productRecords.value = [];
      const message = resolveErrorMessage(error, "产品档案加载失败");
      productLoadError.value = message.includes("forbidden enterprise scope")
        ? "当前账号暂无权限查看该企业的产品档案。"
        : message;
    } finally {
      productLoading.value = false;
    }
  } catch (error) {
    detail.value = null;
    regionName.value = "";
    productRecords.value = [];
    productLoadError.value = "";
    auditLogs.value = [];
    productLoading.value = false;
    loadError.value = resolveErrorMessage(error, "企业详情加载失败");
  } finally {
    loading.value = false;
  }
}

onMounted(loadDetail);
watch(() => route.params.enterpriseId, loadDetail);
</script>

<style scoped>
.enterprise-detail-page {
  display: grid;
  gap: 16px;
}

.state-card {
  padding: 18px 20px;
  border: 1px solid #dbe3ee;
  background: #fff;
  color: #64748b;
  border-radius: 12px;
}

.state-card--error {
  color: #b91c1c;
  border-color: #fecaca;
  background: #fef2f2;
}

.hero {
  display: grid;
  gap: 14px;
  padding: 18px;
  background: linear-gradient(135deg, #f8fbff, #eef4ff);
  border: 1px solid #dbe3ee;
  border-radius: 12px;
}

.hero-topbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.crumbs {
  display: flex;
  gap: 6px;
  color: #64748b;
  font-size: 11px;
  font-weight: 700;
}

.crumb-link {
  padding: 0;
  border: 0;
  background: transparent;
  color: #002660;
  cursor: pointer;
  font-size: inherit;
  font-weight: inherit;
}

.hero-main {
  display: grid;
  gap: 12px;
}

.hero-title-row {
  display: flex;
  gap: 10px;
  align-items: center;
  flex-wrap: wrap;
}

.hero-title-row h1 {
  margin: 0;
  color: #002660;
  font-size: 30px;
  font-weight: 900;
}

.hero-desc {
  margin: 0;
  color: #475569;
  font-size: 13px;
  line-height: 1.7;
}

.hero-meta-row {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
  max-width: 840px;
}

.hero-meta-card {
  padding: 12px 14px;
  background: rgba(255, 255, 255, 0.88);
  border: 1px solid #dbe3ee;
  border-radius: 10px;
}

.hero-meta-card span,
.summary-grid span,
.detail-block label,
.timeline-meta {
  display: block;
  color: #64748b;
  font-size: 11px;
  font-weight: 700;
}

.hero-meta-card strong,
.summary-grid strong {
  display: block;
  margin-top: 5px;
  color: #0f172a;
  font-size: 15px;
  font-weight: 800;
  line-height: 1.5;
}

.hero-back-button {
  min-height: 36px;
  padding: 0 14px;
  border: 1px solid #cbd5e1;
  border-radius: 8px;
  background: #fff;
  color: #334155;
  font-size: 12px;
  font-weight: 700;
  cursor: pointer;
  white-space: nowrap;
}

.content-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 340px;
  gap: 16px;
  align-items: start;
}

.main-col,
.side-col {
  display: grid;
  gap: 16px;
}

.panel {
  border: 1px solid #dbe3ee;
  background: #fff;
  padding: 16px;
  border-radius: 12px;
}

.panel-accent {
  background: linear-gradient(135deg, #002660, #003a8c);
  border-color: transparent;
  color: #fff;
}

.panel h2 {
  margin: 0;
  color: #002660;
  font-size: 12px;
  font-weight: 900;
  text-transform: uppercase;
  letter-spacing: 0.08em;
}

.panel-accent h2 {
  color: rgba(255, 255, 255, 0.82);
}

.section-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 10px;
  margin-bottom: 14px;
}

.section-hint {
  color: #94a3b8;
  font-size: 11px;
  font-weight: 700;
}

.summary-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.summary-grid article {
  padding: 12px;
  background: #f8fafc;
  border: 1px solid #eef2f7;
  border-radius: 10px;
}

.detail-block {
  margin-top: 14px;
  padding: 12px;
  background: #f8fafc;
  border-left: 3px solid #cbd5e1;
  border-radius: 8px;
}

.detail-block p {
  margin: 6px 0 0;
  color: #334155;
  font-size: 13px;
  line-height: 1.7;
  white-space: pre-line;
}

.table-wrap {
  overflow: auto;
  border: 1px solid #eef2f7;
  border-radius: 10px;
}

table {
  width: 100%;
  border-collapse: collapse;
  min-width: 660px;
}

th {
  padding: 12px;
  background: #f8fafc;
  color: #64748b;
  font-size: 11px;
  font-weight: 800;
  text-align: left;
}

td {
  padding: 12px;
  border-top: 1px solid #eef2f7;
  color: #1e293b;
  font-size: 13px;
}

.mini-list {
  display: grid;
  gap: 10px;
  margin-top: 14px;
}

.attachment-link {
  display: block;
  padding: 12px;
  border-radius: 10px;
  background: rgba(255, 255, 255, 0.08);
  color: #fff;
  text-decoration: none;
  border: 1px solid rgba(255, 255, 255, 0.16);
}

.attachment-link strong {
  display: block;
  font-size: 13px;
}

.attachment-link span {
  display: block;
  margin-top: 4px;
  color: rgba(255, 255, 255, 0.78);
  font-size: 12px;
}

.timeline {
  position: relative;
  display: grid;
  gap: 14px;
}

.timeline::before {
  content: "";
  position: absolute;
  left: 5px;
  top: 8px;
  bottom: 8px;
  width: 2px;
  background: #e2e8f0;
}

.timeline-item {
  position: relative;
  padding-left: 22px;
}

.timeline-dot {
  position: absolute;
  left: 0;
  top: 4px;
  width: 12px;
  height: 12px;
  border-radius: 999px;
  background: #002660;
  border: 2px solid #fff;
  box-shadow: 0 0 0 1px #cbd5e1;
}

.timeline-dot--muted {
  background: #64748b;
}

.timeline-head {
  display: flex;
  justify-content: space-between;
  gap: 10px;
  align-items: center;
}

.timeline-head strong {
  color: #0f172a;
  font-size: 12px;
}

.timeline-head time {
  color: #94a3b8;
  font-size: 10px;
  white-space: nowrap;
}

.timeline-main p {
  margin: 6px 0 0;
  color: #64748b;
  font-size: 12px;
  line-height: 1.6;
}

.status-pill,
.inline-pill {
  display: inline-flex;
  min-height: 24px;
  align-items: center;
  justify-content: center;
  padding: 0 10px;
  border-radius: 999px;
  border: 1px solid transparent;
  font-size: 11px;
  font-weight: 800;
}

.is-success {
  background: #dcfce7;
  color: #166534;
  border-color: #86efac;
}

.is-warning {
  background: #fef3c7;
  color: #92400e;
  border-color: #fcd34d;
}

.is-danger {
  background: #fee2e2;
  color: #991b1b;
  border-color: #fca5a5;
}

.is-neutral {
  background: #f1f5f9;
  color: #475569;
  border-color: #dbe3ee;
}

.empty-box {
  padding: 14px;
  border: 1px dashed #cbd5e1;
  background: #f8fafc;
  color: #64748b;
  font-size: 12px;
  border-radius: 10px;
}

.empty-box--dark {
  color: rgba(255, 255, 255, 0.8);
  background: rgba(255, 255, 255, 0.08);
  border-color: rgba(255, 255, 255, 0.2);
}

@media (max-width: 1080px) {
  .content-grid {
    grid-template-columns: 1fr;
  }

  .hero-meta-row {
    grid-template-columns: 1fr;
    max-width: none;
  }
}

@media (max-width: 760px) {
  .hero-topbar {
    flex-direction: column;
    align-items: flex-start;
  }

  .summary-grid {
    grid-template-columns: 1fr;
  }

  .hero-title-row h1 {
    font-size: 22px;
  }

  .timeline-head {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
