<template>
  <RegulatorEnforcerPageShell
    active-key="overview"
    title="企业详情"
    subtitle="查看企业基础档案、产品信息与关联监管记录。"
  >
    <section class="enterprise-detail-page">
      <div v-if="loading" class="state-card">企业信息加载中...</div>
      <div v-else-if="!detail" class="state-card state-card--error">未找到企业信息或当前账号无查看权限。</div>

      <template v-else>
        <header class="hero">
          <div class="hero-main">
            <nav class="crumbs">
              <button class="crumb-link" type="button" @click="handleBack">企业列表</button>
              <span>/</span>
              <span>{{ detail.enterpriseName || "企业详情" }}</span>
            </nav>
            <div class="hero-title-row">
              <h3>{{ detail.enterpriseName || "企业详情" }}</h3>
              <span class="status-pill" :class="approvalBadgeClass(detail.approvalStatus)">
                {{ formatApprovalStatus(detail.approvalStatus) }}
              </span>
              <span class="status-pill" :class="statusBadgeClass(detail.status)">
                {{ formatEnterpriseStatus(detail.status) }}
              </span>
            </div>
            <p class="hero-desc">
              统一社会信用代码：{{ detail.creditCode || "-" }}；所属区域：{{ regionName || "-" }}
            </p>
          </div>
          <div class="hero-side">
            <button class="hero-back-button" type="button" @click="handleBack">返回列表</button>
            <article>
              <span>包保责任人</span>
              <strong>{{ detail.regulatorName || "-" }}</strong>
            </article>
            <article>
              <span>整改事项</span>
              <strong>{{ relatedRectifications.length }}</strong>
            </article>
            <article>
              <span>风险预警</span>
              <strong>{{ relatedWarnings.length }}</strong>
            </article>
          </div>
        </header>

        <div class="content-grid">
          <div class="main-col">
            <section class="panel">
              <div class="section-head">
                <h4>企业基础档案</h4>
                <span class="section-hint">执法视角总览</span>
              </div>
              <div class="summary-grid">
                <article>
                  <span>许可证编号</span>
                  <strong>{{ detail.licenseNo || "-" }}</strong>
                </article>
                <article>
                  <span>法定代表人</span>
                  <strong>{{ detail.legalRepresentative || "-" }}</strong>
                </article>
                <article>
                  <span>负责人</span>
                  <strong>{{ detail.principal || "-" }}</strong>
                </article>
                <article>
                  <span>联系电话</span>
                  <strong>{{ detail.principalPhone || "-" }}</strong>
                </article>
                <article>
                  <span>所属区域</span>
                  <strong>{{ regionName || "-" }}</strong>
                </article>
                <article>
                  <span>最近更新</span>
                  <strong>{{ formatTime(detail.updateTime) }}</strong>
                </article>
              </div>
              <div class="detail-block">
                <label>详细地址</label>
                <p>{{ detail.addressDetail || "-" }}</p>
              </div>
            </section>

            <section class="panel">
              <div class="section-head">
                <h4>审核信息</h4>
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
                <h4>产品档案</h4>
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

            <section class="panel">
              <div class="section-head">
                <h4>关联检查记录</h4>
                <span class="section-hint">最近 {{ relatedInspections.length }} 条</span>
              </div>
              <div v-if="inspectionLoading" class="empty-box">检查记录加载中...</div>
              <div v-else-if="!relatedInspections.length" class="empty-box">未检索到该企业的检查记录。</div>
              <div v-else class="timeline">
                <article v-for="item in relatedInspections" :key="item.id" class="timeline-item">
                  <span class="timeline-dot"></span>
                  <div class="timeline-main">
                    <div class="timeline-head">
                      <strong>{{ item.recordNo || item.taskNo || `#${item.id}` }}</strong>
                      <time>{{ formatTime(item.createTime || item.updateTime) }}</time>
                    </div>
                    <p>{{ item.enterpriseName || detail.enterpriseName || "-" }}</p>
                    <p class="timeline-meta">{{ formatInspectionResult(item.result) }}</p>
                  </div>
                </article>
              </div>
            </section>
          </div>

          <aside class="side-col">
            <section class="panel panel-accent">
              <h4>整改跟进</h4>
              <div v-if="rectificationLoading" class="empty-box empty-box--dark">整改任务加载中...</div>
              <div v-else-if="!relatedRectifications.length" class="empty-box empty-box--dark">当前暂无整改任务。</div>
              <div v-else class="mini-list">
                <article v-for="item in relatedRectifications" :key="item.id" class="mini-card mini-card--dark">
                  <strong>{{ item.rectificationNo || `#${item.id}` }}</strong>
                  <p>{{ item.rectificationDesc || "企业已进入整改流程。" }}</p>
                  <span>{{ formatRectificationStatus(item.status) }}</span>
                </article>
              </div>
            </section>

            <section class="panel">
              <div class="section-head">
                <h4>风险预警摘要</h4>
                <span class="section-hint">{{ relatedWarnings.length }} 条</span>
              </div>
              <div v-if="warningLoading" class="empty-box">风险预警加载中...</div>
              <div v-else-if="!relatedWarnings.length" class="empty-box">暂无相关风险预警。</div>
              <div v-else class="mini-list">
                <article v-for="item in relatedWarnings" :key="item.id" class="mini-card">
                  <strong>{{ item.title || item.warningType || `#${item.id}` }}</strong>
                  <p>{{ item.content || "已触发风险规则，请持续跟进。" }}</p>
                  <span>{{ formatWarningLevel(item.level) }} · {{ formatWarningStatus(item.status) }}</span>
                </article>
              </div>
            </section>

            <section class="panel">
              <div class="section-head">
                <h4>企业附件</h4>
                <span class="section-hint">补充资料</span>
              </div>
              <div v-if="!detail.attachments?.length" class="empty-box">当前企业暂无可查看附件。</div>
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
          </aside>
        </div>

        <div v-if="statusMessage" class="status-banner" :class="{ 'status-banner--error': statusType === 'error' }">
          {{ statusMessage }}
        </div>
      </template>
    </section>
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
  formatStatusLabel,
  inspectionResultMap,
  productStatusMap,
  rectificationStatusMap,
  warningLevelMap,
  warningStatusMap
} from "../../utils/statusMaps";
import { resolveErrorMessage } from "../../utils/uiFeedback";
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
const productLoadError = ref("");
const relatedInspections = ref([]);
const relatedRectifications = ref([]);
const relatedWarnings = ref([]);

const statusMessage = ref("");
const statusType = ref("info");

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
  return formatStatusLabel(value, inspectionResultMap);
}

function formatRectificationStatus(value) {
  return formatStatusLabel(value, rectificationStatusMap);
}

function formatWarningLevel(value) {
  return formatByMap(value, warningLevelMap);
}

function formatWarningStatus(value) {
  return formatByMap(value, warningStatusMap);
}

function formatProductStatus(value) {
  return formatStatusLabel(value, productStatusMap);
}

function statusBadgeClass(value) {
  if (value === "KEY") return "is-danger";
  if (value === "NORMAL") return "is-neutral";
  return "is-neutral";
}

function approvalBadgeClass(value) {
  if (value === "APPROVED") return "is-success";
  if (value === "PENDING") return "is-warning";
  if (value === "REJECTED") return "is-danger";
  return "is-neutral";
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
  productLoadError.value = "";
  relatedInspections.value = [];
  relatedRectifications.value = [];
  relatedWarnings.value = [];

  try {
    const enterprise = await fetchEnterpriseDetail(token.value, enterpriseId);
    detail.value = enterprise || null;
    if (!enterprise) return;

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
      fetchEnterpriseProducts(token.value, enterpriseId).catch((error) => {
        const message = resolveErrorMessage(error, "产品档案加载失败");
        productLoadError.value = message.includes("forbidden enterprise scope")
          ? "当前账号暂无权限查看该企业的产品档案。"
          : message;
        return [];
      }),
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
    setStatus(resolveErrorMessage(error, "企业详情加载失败"), "error");
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
.enterprise-detail-page { display: grid; gap: 16px; }
.state-card { padding: 18px 20px; border: 1px solid #dbe3ee; background: #fff; color: #64748b; border-radius: 12px; }
.state-card--error { color: #b91c1c; border-color: #fecaca; background: #fef2f2; }
.hero { display: grid; grid-template-columns: minmax(0, 1fr) 280px; gap: 16px; padding: 18px; background: linear-gradient(135deg, #f8fbff, #eef4ff); border: 1px solid #dbe3ee; border-radius: 12px; }
.crumbs { display: flex; gap: 6px; color: #64748b; font-size: 11px; font-weight: 700; }
.crumb-link { padding: 0; border: 0; background: transparent; color: #002660; cursor: pointer; font-size: inherit; font-weight: inherit; }
.hero-title-row { margin-top: 10px; display: flex; gap: 10px; align-items: center; flex-wrap: wrap; }
.hero-title-row h3 { margin: 0; color: #002660; font-size: 30px; font-weight: 900; }
.hero-desc { margin: 10px 0 0; color: #475569; font-size: 13px; line-height: 1.7; max-width: 760px; }
.hero-side { display: grid; gap: 10px; }
.hero-back-button {
  min-height: 36px;
  border: 1px solid #cbd5e1;
  border-radius: 8px;
  background: #fff;
  color: #334155;
  font-size: 12px;
  font-weight: 700;
  cursor: pointer;
}
.hero-side article { padding: 12px 14px; background: #fff; border: 1px solid #dbe3ee; border-radius: 10px; }
.hero-side span,
.summary-grid span,
.detail-block label,
.timeline-meta,
.mini-card span,
.attachment-link span { display: block; color: #64748b; font-size: 11px; font-weight: 700; }
.hero-side strong,
.summary-grid strong,
.mini-card strong,
.attachment-link strong { display: block; margin-top: 5px; color: #0f172a; font-size: 15px; font-weight: 800; line-height: 1.5; }
.content-grid { display: grid; grid-template-columns: minmax(0, 1fr) 320px; gap: 16px; align-items: start; }
.main-col, .side-col { display: grid; gap: 16px; }
.panel { border: 1px solid #dbe3ee; background: #fff; padding: 16px; border-radius: 12px; }
.panel-accent { background: linear-gradient(135deg, #002660, #003a8c); border-color: transparent; color: #fff; }
.panel h4 { margin: 0; color: #002660; font-size: 12px; font-weight: 900; text-transform: uppercase; letter-spacing: 0.08em; }
.panel-accent h4 { color: rgba(255, 255, 255, 0.82); }
.section-head { display: flex; justify-content: space-between; align-items: center; gap: 10px; margin-bottom: 14px; }
.section-hint { color: #94a3b8; font-size: 11px; font-weight: 700; }
.summary-grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 12px; }
.summary-grid article { padding: 12px; background: #f8fafc; border: 1px solid #eef2f7; border-radius: 10px; }
.detail-block { margin-top: 14px; padding: 12px; background: #f8fafc; border-left: 3px solid #cbd5e1; border-radius: 8px; }
.detail-block p { margin: 6px 0 0; color: #334155; font-size: 13px; line-height: 1.7; white-space: pre-line; }
.table-wrap { overflow: auto; border: 1px solid #eef2f7; border-radius: 10px; }
table { width: 100%; border-collapse: collapse; min-width: 660px; }
th { padding: 12px; background: #f8fafc; color: #64748b; font-size: 11px; font-weight: 800; text-align: left; }
td { padding: 12px; border-top: 1px solid #eef2f7; color: #1e293b; font-size: 13px; }
.timeline { position: relative; display: grid; gap: 14px; }
.timeline::before { content: ""; position: absolute; left: 5px; top: 8px; bottom: 8px; width: 2px; background: #e2e8f0; }
.timeline-item { position: relative; padding-left: 22px; }
.timeline-dot { position: absolute; left: 0; top: 4px; width: 12px; height: 12px; border-radius: 999px; background: #002660; border: 2px solid #fff; box-shadow: 0 0 0 1px #cbd5e1; }
.timeline-head { display: flex; justify-content: space-between; gap: 10px; align-items: center; }
.timeline-head strong { color: #0f172a; font-size: 12px; }
.timeline-head time { color: #94a3b8; font-size: 10px; white-space: nowrap; }
.timeline-main p { margin: 6px 0 0; color: #64748b; font-size: 12px; line-height: 1.6; }
.mini-list { display: grid; gap: 10px; margin-top: 14px; }
.mini-card { padding: 12px; background: #f8fafc; border: 1px solid #eef2f7; border-radius: 10px; }
.mini-card p { margin: 6px 0 0; color: #475569; font-size: 12px; line-height: 1.6; }
.mini-card--dark { background: rgba(255, 255, 255, 0.08); border-color: rgba(255, 255, 255, 0.16); }
.mini-card--dark strong,
.mini-card--dark p,
.mini-card--dark span { color: #fff; }
.mini-card--dark p,
.mini-card--dark span { color: rgba(255, 255, 255, 0.82); }
.attachment-link { display: block; padding: 12px; background: #f8fafc; border: 1px solid #eef2f7; border-radius: 10px; color: inherit; text-decoration: none; }
.status-pill,
.inline-pill { display: inline-flex; min-height: 24px; align-items: center; justify-content: center; padding: 0 10px; border-radius: 999px; border: 1px solid transparent; font-size: 11px; font-weight: 800; }
.is-success { background: #dcfce7; color: #166534; border-color: #86efac; }
.is-warning { background: #fef3c7; color: #92400e; border-color: #fcd34d; }
.is-danger { background: #fee2e2; color: #991b1b; border-color: #fca5a5; }
.is-neutral { background: #f1f5f9; color: #475569; border-color: #dbe3ee; }
.empty-box { padding: 14px; border: 1px dashed #cbd5e1; background: #f8fafc; color: #64748b; font-size: 12px; border-radius: 10px; }
.empty-box--dark { color: rgba(255, 255, 255, 0.8); background: rgba(255, 255, 255, 0.08); border-color: rgba(255, 255, 255, 0.2); }
.status-banner { padding: 10px 12px; border: 1px solid #dbe3ee; background: #f8fafc; color: #334155; border-radius: 10px; }
.status-banner--error { border-color: #fecaca; background: #fef2f2; color: #b91c1c; }
@media (max-width: 1080px) {
  .hero, .content-grid { grid-template-columns: 1fr; }
}
@media (max-width: 760px) {
  .summary-grid { grid-template-columns: 1fr; }
  .hero-title-row h3 { font-size: 22px; }
  .timeline-head { flex-direction: column; align-items: flex-start; }
}
</style>
