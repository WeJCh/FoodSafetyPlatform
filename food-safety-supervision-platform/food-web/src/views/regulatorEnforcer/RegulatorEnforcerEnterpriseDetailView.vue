<template>
  <RegulatorEnforcerPageShell
    active-key="overview"
    :title="detail?.enterpriseName || '企业详情'"
    subtitle="企业档案、产品信息与关联检查任务在同一页面查看，便于执法跟进与闭环处置。"
  >
    <div v-if="loading" class="status-banner">企业信息加载中...</div>
    <div v-else-if="!detail" class="status-banner status-banner--error">未找到企业信息或当前账号无查看权限。</div>

    <template v-else>
      <section class="hero-card">
        <div>
          <div class="hero-badges">
            <span class="badge badge--id">ENT-{{ route.params.enterpriseId || "-" }}</span>
            <span class="badge" :class="statusBadgeClass(detail.status)">{{ formatEnterpriseStatus(detail.status) }}</span>
            <span class="badge" :class="approvalBadgeClass(detail.approvalStatus)">
              {{ formatApprovalStatus(detail.approvalStatus) }}
            </span>
          </div>
          <h3>{{ detail.enterpriseName || "企业详情" }}</h3>
          <p class="hero-copy">
            统一社会信用代码：{{ detail.creditCode || "-" }}，所属区域：{{ regionName || "-" }}
          </p>
        </div>
        <div class="hero-actions">
          <button class="ghost-btn" type="button" @click="handleBack">返回列表</button>
          <button class="primary-btn" type="button" @click="openStats">查看统计看板</button>
        </div>
      </section>

      <section class="summary-grid">
        <article>
          <span>近期待办</span>
          <strong>{{ relatedRectifications.length }}</strong>
          <em>整改跟进事项</em>
        </article>
        <article>
          <span>检查记录</span>
          <strong>{{ relatedInspections.length }}</strong>
          <em>近期待跟进检查</em>
        </article>
        <article>
          <span>风险预警</span>
          <strong>{{ relatedWarnings.length }}</strong>
          <em>当前检索到的预警摘要</em>
        </article>
        <article>
          <span>产品档案</span>
          <strong>{{ productRecords.length }}</strong>
          <em>已备案产品数量</em>
        </article>
      </section>

      <section class="detail-layout">
        <div class="main-column">
          <article class="panel">
            <div class="panel-head">
              <h4>企业基础档案</h4>
              <span>执法视角信息总览</span>
            </div>
            <div class="base-grid">
              <div><span>许可证编号</span><strong>{{ detail.licenseNo || "-" }}</strong></div>
              <div><span>法定代表人</span><strong>{{ detail.legalRepresentative || "-" }}</strong></div>
              <div><span>负责人</span><strong>{{ detail.principal || "-" }}</strong></div>
              <div><span>联系电话</span><strong>{{ detail.principalPhone || "-" }}</strong></div>
              <div><span>所属区域</span><strong>{{ regionName || "-" }}</strong></div>
              <div><span>最近更新</span><strong>{{ formatTime(detail.updateTime) }}</strong></div>
              <div class="is-wide"><span>详细地址</span><strong>{{ detail.addressDetail || "-" }}</strong></div>
              <div class="is-wide"><span>审核意见</span><strong>{{ detail.approvalComment || "-" }}</strong></div>
            </div>
          </article>

          <article class="panel">
            <div class="panel-head">
              <h4>产品档案</h4>
              <span>共 {{ productRecords.length }} 项</span>
            </div>
            <div v-if="productLoading" class="empty-box">产品档案加载中...</div>
            <div v-else-if="!productRecords.length" class="empty-box">当前企业暂无产品档案。</div>
            <div v-else class="product-list">
              <article v-for="item in productRecords" :key="item.id" class="product-item">
                <div class="product-item__head">
                  <strong>{{ item.productName || "-" }}</strong>
                  <span class="tag" :class="item.status === 'ACTIVE' ? 'tag--success' : 'tag--muted'">
                    {{ formatProductStatus(item.status) }}
                  </span>
                </div>
                <p>{{ item.category || "-" }} / {{ item.specification || "-" }}</p>
                <small>更新时间：{{ formatTime(item.updateTime) }}</small>
              </article>
            </div>
          </article>

          <article class="panel">
            <div class="panel-head">
              <h4>关联检查记录</h4>
              <span>最近 {{ relatedInspections.length }} 条</span>
            </div>
            <div v-if="inspectionLoading" class="empty-box">检查记录加载中...</div>
            <div v-else-if="!relatedInspections.length" class="empty-box">未检索到该企业的检查记录。</div>
            <div v-else class="record-list">
              <article v-for="item in relatedInspections" :key="item.id" class="record-item">
                <div class="record-item__head">
                  <strong>{{ item.recordNo || item.taskNo || `#${item.id}` }}</strong>
                  <span class="tag" :class="inspectionResultClass(item.result)">{{ formatInspectionResult(item.result) }}</span>
                </div>
                <p>{{ item.enterpriseName || detail.enterpriseName || "-" }}</p>
                <small>检查时间：{{ formatTime(item.createTime || item.updateTime) }}</small>
              </article>
            </div>
          </article>
        </div>

        <aside class="side-column">
          <article class="panel panel--accent">
            <div class="panel-head panel-head--light">
              <h4>整改跟进</h4>
              <span>整改动态速览</span>
            </div>
            <div v-if="rectificationLoading" class="empty-box empty-box--dark">整改任务加载中...</div>
            <div v-else-if="!relatedRectifications.length" class="empty-box empty-box--dark">当前暂无整改任务。</div>
            <div v-else class="timeline-list">
              <article v-for="item in relatedRectifications" :key="item.id" class="timeline-item timeline-item--light">
                <strong>{{ item.rectificationNo || `RECT-${item.id}` }}</strong>
                <p>{{ item.rectificationDesc || "企业已进入整改流程" }}</p>
                <div class="timeline-meta">
                  <span>{{ formatRectificationStatus(item.status) }}</span>
                  <span>{{ item.currentDeadline ? formatTime(item.currentDeadline) : "-" }}</span>
                </div>
              </article>
            </div>
          </article>

          <article class="panel">
            <div class="panel-head">
              <h4>风险预警摘要</h4>
              <span>与企业关键字关联</span>
            </div>
            <div v-if="warningLoading" class="empty-box">风险预警加载中...</div>
            <div v-else-if="!relatedWarnings.length" class="empty-box">暂无相关风险预警。</div>
            <div v-else class="timeline-list">
              <article v-for="item in relatedWarnings" :key="item.id" class="timeline-item">
                <strong>{{ item.title || item.warningNo || `WARN-${item.id}` }}</strong>
                <p>{{ item.content || "已触发风险规则，请持续跟进。" }}</p>
                <div class="timeline-meta">
                  <span>{{ formatWarningLevel(item.level) }}</span>
                  <span>{{ formatWarningStatus(item.status) }}</span>
                </div>
              </article>
            </div>
          </article>

          <article class="panel">
            <div class="panel-head">
              <h4>附件与档案提示</h4>
              <span>原型中的右侧信息补充位</span>
            </div>
            <div v-if="!detail.attachments?.length" class="empty-box">当前企业暂无可查看附件。</div>
            <div v-else class="attachment-list">
              <a
                v-for="(item, index) in detail.attachments"
                :key="`${item.type || 'attachment'}-${index}`"
                class="attachment-item"
                :href="item.url"
                target="_blank"
                rel="noreferrer"
              >
                <strong>{{ item.label || item.name || "备案附件" }}</strong>
                <span>{{ item.name || "未命名附件" }}</span>
              </a>
            </div>
          </article>
        </aside>
      </section>

      <div v-if="statusMessage" class="status-banner" :class="{ 'status-banner--error': statusType === 'error' }">
        {{ statusMessage }}
      </div>
    </template>
  </RegulatorEnforcerPageShell>
</template>

<script setup>
import { onMounted, ref, watch } from "vue";
import { useRoute, useRouter } from "vue-router";
import { fetchEnterpriseDetail, fetchEnterpriseProducts, fetchMyWarningRecords, fetchRegionPath } from "../../api/regulation";
import { fetchMyInspectionRecords, fetchMyRegulatorRectifications } from "../../api/regulationOperation";
import RegulatorEnforcerPageShell from "./RegulatorEnforcerPageShell.vue";
import { formatByMap, formatTime } from "../../utils/formatters";
import {
  approvalStatusMap,
  enterpriseStatusMap,
  warningLevelMap,
  warningStatusMap
} from "../../utils/statusMaps";
import { useRegulatorEnforcerShellSession } from "./regulatorEnforcerShared";

const route = useRoute();
const router = useRouter();
const { token } = useRegulatorEnforcerShellSession();

const loading = ref(false);
const productLoading = ref(false);
const inspectionLoading = ref(false);
const rectificationLoading = ref(false);
const warningLoading = ref(false);

const detail = ref(null);
const regionName = ref("");
const productRecords = ref([]);
const relatedInspections = ref([]);
const relatedRectifications = ref([]);
const relatedWarnings = ref([]);

const statusMessage = ref("");
const statusType = ref("info");
const inspectionResultMap = {
  PASS: "合格",
  FAIL: "不合格"
};
const rectificationStatusMap = {
  ONGOING: "整改中",
  SUBMITTED: "待复核",
  REWORK: "打回重做",
  CONFIRMED: "已确认"
};

function setStatus(message = "", type = "info") {
  statusMessage.value = message;
  statusType.value = type;
}

function formatEnterpriseStatus(value) {
  return formatByMap(value, enterpriseStatusMap);
}

function formatApprovalStatus(value) {
  return formatByMap(value, approvalStatusMap);
}

function formatInspectionResult(value) {
  return formatByMap(value, inspectionResultMap);
}

function formatRectificationStatus(value) {
  return formatByMap(value, rectificationStatusMap);
}

function formatWarningLevel(value) {
  return formatByMap(value, warningLevelMap);
}

function formatWarningStatus(value) {
  return formatByMap(value, warningStatusMap);
}

function formatProductStatus(value) {
  const map = { ACTIVE: "启用", INACTIVE: "停用" };
  return map[value] || value || "-";
}

function statusBadgeClass(value) {
  if (value === "KEY") return "badge--danger";
  if (value === "NORMAL") return "badge--neutral";
  return "badge--plain";
}

function approvalBadgeClass(value) {
  if (value === "APPROVED") return "badge--success";
  if (value === "PENDING") return "badge--warning";
  if (value === "REJECTED") return "badge--danger";
  return "badge--plain";
}

function inspectionResultClass(value) {
  if (value === "PASS") return "tag--success";
  if (value === "FAIL") return "tag--danger";
  return "tag--muted";
}

function handleBack() {
  const fromSection = typeof route.query.from === "string" ? route.query.from : "enterprises";
  const routeNameMap = {
    enterprises: "regulator-enforcer-enterprises",
    tasks: "regulator-enforcer-tasks",
    sampling: "regulator-enforcer-sampling",
    inspections: "regulator-enforcer-inspections",
    complaints: "regulator-enforcer-complaints",
    rectifications: "regulator-enforcer-rectifications",
    rectification: "regulator-enforcer-rectifications",
    warnings: "regulator-enforcer-warnings",
    stats: "regulator-enforcer-stats"
  };
  router.push({ name: routeNameMap[fromSection] || "regulator-enforcer-enterprises" }).catch(() => {});
}

function openStats() {
  router.push({ name: "regulator-enforcer-stats" }).catch(() => {});
}

async function loadDetail() {
  const enterpriseId = route.params.enterpriseId;
  if (!enterpriseId) {
    detail.value = null;
    return;
  }

  loading.value = true;
  setStatus("");
  detail.value = null;
  regionName.value = "";
  productRecords.value = [];
  relatedInspections.value = [];
  relatedRectifications.value = [];
  relatedWarnings.value = [];

  try {
    const enterprise = await fetchEnterpriseDetail(token.value, enterpriseId);
    detail.value = enterprise || null;

    if (!enterprise) {
      return;
    }

    if (enterprise.regionId) {
      const path = await fetchRegionPath(token.value, enterprise.regionId).catch(() => []);
      regionName.value = Array.isArray(path) && path.length ? path.map((item) => item.name).join(" / ") : "";
    }

    const enterpriseName = enterprise.enterpriseName || "";

    productLoading.value = true;
    inspectionLoading.value = true;
    rectificationLoading.value = true;
    warningLoading.value = true;

    const [products, inspections, rectifications, warnings] = await Promise.all([
      fetchEnterpriseProducts(token.value, enterpriseId).catch(() => []),
      fetchMyInspectionRecords(token.value, { enterpriseName, page: 1, size: 4 }).catch(() => ({ records: [] })),
      fetchMyRegulatorRectifications(token.value, { enterpriseName, page: 1, size: 4 }).catch(() => ({ records: [] })),
      fetchMyWarningRecords(token.value, { keyword: enterpriseName, page: 1, size: 4 }).catch(() => ({ records: [] }))
    ]);

    productRecords.value = Array.isArray(products) ? products : [];
    relatedInspections.value = Array.isArray(inspections?.records) ? inspections.records : [];
    relatedRectifications.value = Array.isArray(rectifications?.records) ? rectifications.records : [];
    relatedWarnings.value = Array.isArray(warnings?.records) ? warnings.records : [];
  } catch (error) {
    detail.value = null;
    setStatus(error.message || "企业详情加载失败", "error");
  } finally {
    loading.value = false;
    productLoading.value = false;
    inspectionLoading.value = false;
    rectificationLoading.value = false;
    warningLoading.value = false;
  }
}

onMounted(loadDetail);
watch(() => route.params.enterpriseId, loadDetail);
</script>

<style scoped>
.hero-card {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  gap: 16px;
  padding: 18px 20px;
  margin-bottom: 14px;
  border: 1px solid #dbe3ee;
  background: linear-gradient(135deg, #002a63 0%, #0f4ea5 100%);
  color: #fff;
}
.hero-badges,
.hero-actions,
.timeline-meta {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}
.badge,
.tag {
  display: inline-flex;
  align-items: center;
  min-height: 24px;
  padding: 0 10px;
  border-radius: 999px;
  font-size: 11px;
  font-weight: 800;
}
.badge {
  background: rgba(255, 255, 255, 0.14);
  color: #fff;
}
.badge--id { background: rgba(219, 234, 254, 0.24); }
.badge--success { background: rgba(34, 197, 94, 0.2); }
.badge--warning { background: rgba(251, 191, 36, 0.22); }
.badge--danger { background: rgba(248, 113, 113, 0.22); }
.badge--neutral { background: rgba(226, 232, 240, 0.18); }
.badge--plain { background: rgba(255, 255, 255, 0.14); }
.hero-card h3 {
  margin: 10px 0 0;
  font-size: 30px;
  font-weight: 900;
}
.hero-copy {
  margin: 8px 0 0;
  color: rgba(255, 255, 255, 0.84);
  font-size: 13px;
}
.primary-btn,
.ghost-btn {
  min-height: 36px;
  padding: 0 14px;
  border: 1px solid #cbd5e1;
  background: #fff;
  color: #1e293b;
  font-size: 12px;
  font-weight: 700;
  cursor: pointer;
}
.primary-btn {
  border-color: #002660;
  background: #002660;
  color: #fff;
}
.summary-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
  margin-bottom: 14px;
}
.summary-grid article {
  border: 1px solid #dbe3ee;
  background: #fff;
  padding: 14px;
}
.summary-grid span {
  display: block;
  color: #64748b;
  font-size: 12px;
}
.summary-grid strong {
  display: block;
  margin-top: 8px;
  color: #0f172a;
  font-size: 28px;
  font-weight: 900;
}
.summary-grid em {
  display: block;
  margin-top: 6px;
  color: #64748b;
  font-style: normal;
  font-size: 12px;
}
.detail-layout {
  display: grid;
  grid-template-columns: minmax(0, 1.7fr) 360px;
  gap: 14px;
}
.main-column,
.side-column,
.product-list,
.record-list,
.timeline-list,
.attachment-list {
  display: grid;
  gap: 14px;
}
.panel {
  border: 1px solid #dbe3ee;
  background: #fff;
  padding: 16px;
}
.panel--accent {
  background: linear-gradient(180deg, #f5f9ff 0%, #edf4ff 100%);
}
.panel-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 10px;
  margin-bottom: 12px;
}
.panel-head h4 {
  margin: 0;
  color: #002660;
  font-size: 15px;
  font-weight: 800;
}
.panel-head span {
  color: #64748b;
  font-size: 12px;
}
.panel-head--light h4,
.panel-head--light span {
  color: #0f3a72;
}
.base-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}
.base-grid div {
  border: 1px solid #edf2f7;
  background: #f8fafc;
  padding: 12px;
  display: grid;
  gap: 6px;
}
.base-grid .is-wide {
  grid-column: 1 / -1;
}
.base-grid span,
.timeline-item p,
.product-item p,
.product-item small,
.attachment-item span {
  color: #64748b;
  font-size: 12px;
}
.base-grid strong,
.timeline-item strong,
.product-item strong,
.attachment-item strong {
  color: #0f172a;
  font-size: 13px;
}
.product-item,
.record-item,
.timeline-item,
.attachment-item {
  border: 1px solid #e2e8f0;
  background: #f8fafc;
  padding: 12px;
  text-decoration: none;
}
.product-item__head,
.record-item__head {
  display: flex;
  justify-content: space-between;
  gap: 8px;
  align-items: center;
}
.record-item p,
.record-item small {
  margin: 6px 0 0;
  color: #64748b;
  font-size: 12px;
}
.timeline-item--light {
  background: rgba(255, 255, 255, 0.8);
}
.tag--success { background: #dcfce7; color: #166534; }
.tag--danger { background: #fee2e2; color: #b91c1c; }
.tag--muted { background: #e2e8f0; color: #475569; }
.empty-box,
.status-banner {
  border: 1px solid #dbe3ee;
  background: #f8fafc;
  color: #475569;
  padding: 12px;
  font-size: 13px;
}
.empty-box--dark {
  background: rgba(255, 255, 255, 0.7);
}
.status-banner--error {
  border-color: #fecaca;
  background: #fef2f2;
  color: #b91c1c;
}
@media (max-width: 1200px) {
  .detail-layout,
  .summary-grid {
    grid-template-columns: 1fr 1fr;
  }
  .side-column {
    grid-column: 1 / -1;
  }
}
@media (max-width: 860px) {
  .hero-card,
  .summary-grid,
  .detail-layout,
  .base-grid {
    grid-template-columns: 1fr;
  }
  .hero-card {
    display: grid;
  }
}
</style>
