<template>
  <RegulatorAdminWorkspacePage
    :active-key="activeNavKey"
    :username="regulatorUser.username"
    @navigate="handleSidebarNavigate"
    @logout="handleLogout"
    @pending-feature="onPendingFeature"
  >
    <section class="enterprise-detail-page">
      <header class="detail-header">
        <div>
          <h1>{{ detail?.enterpriseName || "企业详情" }}</h1>
          <p>主档案 ID: ENT-{{ route.params.enterpriseId || "-" }}</p>
        </div>
        <div class="detail-header__chips">
          <div class="status-chip-group">
            <span class="status-chip-label">审核状态</span>
            <span class="chip" :class="approvalChipClass(detail?.approvalStatus)">
              {{ formatApprovalStatus(detail?.approvalStatus) }}
            </span>
          </div>
          <div class="status-chip-group">
            <span class="status-chip-label">监管状态</span>
            <span class="chip" :class="statusChipClass(detail?.status)">
              {{ formatStatus(detail?.status) }}
            </span>
          </div>
        </div>
        <div class="detail-header__actions">
          <button type="button" class="primary" @click="handleBack">返回列表</button>
        </div>
      </header>

      <div v-if="loading" class="status info">加载企业详情中...</div>
      <div v-else-if="!detail" class="status error">企业信息未找到</div>

      <template v-else>
        <div class="detail-layout">
          <section class="panel">
            <h2>基础信息</h2>
            <div class="base-grid">
              <article><span>统一社会信用代码</span><strong>{{ detail.creditCode || "-" }}</strong></article>
              <article><span>法定代表人</span><strong>{{ detail.legalRepresentative || "-" }}</strong></article>
              <article><span>食品经营许可证编号</span><strong>{{ detail.licenseNo || "-" }}</strong></article>
              <article><span>负责人</span><strong>{{ detail.principal || "-" }}</strong></article>
              <article><span>负责人电话</span><strong>{{ detail.principalPhone || "-" }}</strong></article>
              <article><span>所属区域</span><strong>{{ regionName || "-" }}</strong></article>
              <article class="span2"><span>详细地址</span><strong>{{ detail.addressDetail || "-" }}</strong></article>
              <article class="span2"><span>审核意见</span><strong>{{ detail.approvalComment || "-" }}</strong></article>
            </div>
            <div class="attachment-block">
              <div class="attachment-block__title">企业附件</div>
              <div v-if="!detail.attachments || !detail.attachments.length" class="status info">
                当前企业暂无可查看附件。
              </div>
              <div v-else class="attachment-list">
                <a
                  v-for="(item, index) in detail.attachments"
                  :key="`${item.type || 'attachment'}-${index}`"
                  class="attachment-item"
                  :href="item.url"
                  target="_blank"
                  rel="noreferrer"
                >
                  <div>
                    <strong>{{ item.label || item.name || "备案附件" }}</strong>
                    <p>{{ item.name || "未命名附件" }}</p>
                  </div>
                  <span>查看</span>
                </a>
              </div>
            </div>
          </section>

          <section class="panel panel--table">
            <div class="panel__head">
              <h2>产品档案概览</h2>
              <span>总计 {{ productRecords.length }} 项</span>
            </div>
            <div v-if="productLoading" class="status info">产品档案加载中...</div>
            <div v-else-if="!productRecords.length" class="status info">当前企业暂无产品档案。</div>
            <table v-else>
              <thead>
                <tr>
                  <th>产品名称</th>
                  <th>类别</th>
                  <th>规格型号</th>
                  <th>最后更新</th>
                  <th>状态</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="item in productRecords" :key="item.id">
                  <td>{{ item.productName || "-" }}</td>
                  <td>{{ item.category || "-" }}</td>
                  <td>{{ item.specification || "-" }}</td>
                  <td>{{ formatTime(item.updateTime) }}</td>
                  <td><span class="chip" :class="item.status === 'ACTIVE' ? 'chip--ok' : 'chip--off'">{{ formatProductStatus(item.status) }}</span></td>
                </tr>
              </tbody>
            </table>
          </section>
        </div>

        <div class="side-layout">
          <section class="panel panel--highlight">
            <h2>风险历史</h2>
            <div v-if="!riskHistory.length" class="status info">暂无风险历史，保持正常监管。</div>
            <ul v-else class="risk-list">
              <li v-for="(item, index) in riskHistory" :key="`${item.reasonType}-${index}`">
                <strong>{{ formatReasonType(item.reasonType) }}</strong>
                <span>{{ formatTime(item.createTime) }}</span>
                <p>{{ item.reasonDetail || "触发重点监管规则" }}</p>
              </li>
            </ul>
          </section>

          <section class="panel">
            <h2>审计日志</h2>
            <ul class="audit-list">
              <li>
                <strong>企业档案已加载</strong>
                <p>{{ formatTime(detail.updateTime) }} · 系统自动同步备案主档</p>
              </li>
              <li>
                <strong>产品数据同步</strong>
                <p>已加载 {{ productRecords.length }} 条产品记录</p>
              </li>
              <li>
                <strong>后续能力占位</strong>
                <p>TODO: 接入企业审计日志接口，替换当前前端占位。</p>
              </li>
            </ul>
          </section>
        </div>
      </template>
    </section>
  </RegulatorAdminWorkspacePage>
</template>

<script setup>
import { computed, onMounted, ref, watch } from "vue";
import { useRoute } from "vue-router";
import RegulatorAdminWorkspacePage from "../../components/regulatorAdmin/RegulatorAdminWorkspacePage.vue";
import { fetchEnterpriseDetail, fetchEnterpriseProducts, fetchRegionPath } from "../../api/regulation";
import { formatByMap, formatTime } from "../../utils/formatters";
import { approvalStatusMap, enterpriseStatusMap } from "../../utils/statusMaps";
import {
  regulatorFeaturePendingNotice,
  useRegulatorAdminShellSession
} from "./regulatorAdminShared";

const route = useRoute();
const { regulatorUser, token, handleSidebarNavigate, handleLogout } = useRegulatorAdminShellSession();

const loading = ref(false);
const detail = ref(null);
const regionName = ref("");
const productLoading = ref(false);
const productRecords = ref([]);

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
    "stats"
  ]);
  return allowed.has(from) ? from : "enterprises";
});

const riskHistory = computed(() => {
  if (!detail.value?.keyReasons?.length) return [];
  return detail.value.keyReasons.slice(0, 4);
});

function onPendingFeature(title) {
  // TODO: 待接入档案导出与深层审计能力
  regulatorFeaturePendingNotice(title);
}

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
  if (value === "APPROVED") return "chip--ok";
  if (value === "PENDING") return "chip--pending";
  if (value === "REJECTED") return "chip--reject";
  return "chip--neutral";
}

function statusChipClass(value) {
  if (value === "KEY") return "chip--key";
  if (value === "NORMAL") return "chip--normal";
  return "chip--neutral";
}

function formatProductStatus(value) {
  const map = { ACTIVE: "启用", INACTIVE: "停用" };
  return map[value] || value || "-";
}

function formatReasonType(value) {
  const map = {
    COMPLAINT_OVERFLOW: "投诉过多",
    CONSECUTIVE_FAIL: "连续不合格",
    SAMPLING_FAIL: "抽检不合格",
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
    return;
  }

  loading.value = true;
  try {
    detail.value = await fetchEnterpriseDetail(token.value, enterpriseId);
    regionName.value = "";
    if (detail.value?.regionId) {
      const path = await fetchRegionPath(token.value, detail.value.regionId).catch(() => []);
      regionName.value = Array.isArray(path) && path.length
        ? path.map((item) => item.name).join(" / ")
        : "";
    }
    productLoading.value = true;
    try {
      const products = await fetchEnterpriseProducts(token.value, enterpriseId);
      productRecords.value = Array.isArray(products) ? products : [];
    } catch {
      productRecords.value = [];
    } finally {
      productLoading.value = false;
    }
  } catch {
    detail.value = null;
    regionName.value = "";
    productRecords.value = [];
    productLoading.value = false;
  } finally {
    loading.value = false;
  }
}

onMounted(loadDetail);
watch(() => route.params.enterpriseId, loadDetail);
</script>

<style scoped>
.enterprise-detail-page { display: grid; gap: 16px; }
.detail-header { background: #fff; border-radius: 6px; padding: 16px; display: grid; grid-template-columns: 1fr auto auto; gap: 12px; align-items: end; }
.detail-header h1 { margin: 0; color: #002660; font-size: 30px; font-weight: 800; }
.detail-header p { margin: 6px 0 0; color: #64748b; font-size: 13px; }
.detail-header__chips { display: flex; gap: 8px; align-items: center; }
.status-chip-group { display: grid; gap: 4px; justify-items: start; }
.status-chip-label { font-size: 10px; color: #64748b; font-weight: 700; letter-spacing: 0.05em; text-transform: uppercase; }
.detail-header__actions { display: flex; gap: 8px; }
.detail-header__actions button { border: 0; border-radius: 4px; padding: 8px 12px; font-size: 12px; font-weight: 700; cursor: pointer; }
.detail-header__actions .ghost { background: #e2e8f0; color: #334155; }
.detail-header__actions .primary { background: #002660; color: #fff; }
.chip { display: inline-flex; align-items: center; padding: 3px 8px; border-radius: 4px; background: #e2e8f0; color: #334155; font-size: 11px; font-weight: 700; }
.chip--ok { background: #dcfce7; color: #166534; }
.chip--pending { background: #fef3c7; color: #92400e; }
.chip--reject { background: #fee2e2; color: #991b1b; }
.chip--key { background: #fee2e2; color: #991b1b; }
.chip--normal { background: #e2e8f0; color: #334155; }
.chip--neutral { background: #e2e8f0; color: #334155; }
.chip--off { background: #f1f5f9; color: #64748b; }
.detail-layout { display: grid; grid-template-columns: 2fr 2fr; gap: 12px; }
.side-layout { display: grid; grid-template-columns: 1fr 1fr; gap: 12px; }
.panel { background: #fff; border-radius: 6px; padding: 14px; }
.panel h2 { margin: 0 0 10px; color: #0f172a; font-size: 16px; }
.base-grid { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 10px; }
.base-grid article { background: #f8fafc; border-radius: 4px; padding: 10px; display: grid; gap: 6px; }
.base-grid article.span2 { grid-column: span 2; }
.base-grid span { color: #64748b; font-size: 11px; text-transform: uppercase; font-weight: 700; }
.base-grid strong { color: #0f172a; font-size: 13px; }
.attachment-block { margin-top: 12px; border-top: 1px solid #e2e8f0; padding-top: 12px; }
.attachment-block__title { color: #0f172a; font-size: 13px; font-weight: 700; margin-bottom: 8px; }
.attachment-list { display: grid; gap: 8px; }
.attachment-item {
  text-decoration: none;
  color: inherit;
  border: 1px solid #e2e8f0;
  background: #f8fafc;
  border-radius: 4px;
  padding: 8px 10px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}
.attachment-item strong { font-size: 12px; color: #0f172a; }
.attachment-item p { margin: 2px 0 0; font-size: 12px; color: #64748b; }
.attachment-item span { font-size: 12px; color: #1d4ed8; font-weight: 700; }
.attachment-item:hover { background: #eef2ff; border-color: #c7d2fe; }
.panel__head { display: flex; justify-content: space-between; align-items: center; margin-bottom: 10px; }
.panel__head span { font-size: 12px; color: #64748b; }
table { width: 100%; border-collapse: collapse; }
th { background: #f8fafc; color: #64748b; text-align: left; font-size: 11px; text-transform: uppercase; padding: 10px; }
td { border-top: 1px solid #edf2f7; padding: 10px; font-size: 13px; color: #1e293b; }
.panel--highlight { background: linear-gradient(120deg, #003a8c, #124394); color: #fff; }
.panel--highlight h2 { color: #fff; }
.risk-list, .audit-list { margin: 0; padding: 0; list-style: none; display: grid; gap: 10px; }
.risk-list li, .audit-list li { background: rgba(255, 255, 255, 0.08); border-radius: 4px; padding: 10px; display: grid; gap: 4px; }
.panel:not(.panel--highlight) .audit-list li { background: #f8fafc; }
.risk-list strong, .audit-list strong { font-size: 13px; }
.risk-list span { font-size: 11px; opacity: 0.85; }
.risk-list p, .audit-list p { margin: 0; font-size: 12px; line-height: 1.5; }
.status { border-radius: 4px; padding: 12px; font-size: 13px; }
.status.info { background: #eff6ff; color: #1d4ed8; }
.status.error { background: #fee2e2; color: #991b1b; }
@media (max-width: 1200px) {
  .detail-header, .detail-layout, .side-layout { grid-template-columns: 1fr; }
  .base-grid { grid-template-columns: 1fr; }
  .base-grid article.span2 { grid-column: span 1; }
}
</style>
