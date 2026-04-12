<template>
  <div class="app-shell detail-shell">
    <div class="hero-panel">
      <div class="hero-content">
        <span class="badge">企业详情</span>
        <h1>企业备案与产品档案</h1>
        <p>只读展示企业备案详情与产品档案，监管人员不可在此修改。</p>
      </div>
    </div>

    <div class="form-panel">
      <div class="card">
        <div class="section-title">企业信息</div>
        <div v-if="loading" class="status info">加载中...</div>
        <div v-else-if="!detail" class="status error">企业信息未找到</div>
        <div v-else class="detail-grid">
          <div>
            <span>企业名称</span>
            <strong>{{ detail.enterpriseName }}</strong>
          </div>
          <div>
            <span>许可证编号</span>
            <strong>{{ detail.licenseNo || "-" }}</strong>
          </div>
          <div>
            <span>负责人</span>
            <strong>{{ detail.principal || "-" }}</strong>
          </div>
          <div>
            <span>负责人电话</span>
            <strong>{{ detail.principalPhone || "-" }}</strong>
          </div>
          <div>
            <span>所属区域</span>
            <strong>{{ regionName || "-" }}</strong>
          </div>
          <div>
            <span>详细地址</span>
            <strong>{{ detail.addressDetail || "-" }}</strong>
          </div>
          <div>
            <span>企业状态</span>
            <strong>{{ formatStatus(detail.status) }}</strong>
          </div>
          <div>
            <span>审核状态</span>
            <strong>{{ formatApprovalStatus(detail.approvalStatus) }}</strong>
          </div>
          <div>
            <span>审核意见</span>
            <strong>{{ detail.approvalComment || "-" }}</strong>
          </div>
          <div>
            <span>审核人</span>
            <strong>{{ detail.regulatorName || "-" }}</strong>
          </div>
          <div>
            <span>审核时间</span>
            <strong>{{ formatTime(detail.approvedTime) }}</strong>
          </div>
          <div>
            <span>更新时间</span>
            <strong>{{ formatTime(detail.updateTime) }}</strong>
          </div>
        </div>

        <div v-if="detail?.status === 'KEY'" class="key-reason-panel">
          <div class="section-title section-title--sub">重点监管原因</div>
          <div v-if="!detail.keyReasons || !detail.keyReasons.length" class="status info">
            当前企业已纳入重点监管，但历史原因记录暂未补齐。
          </div>
          <div v-else class="key-reason-list">
            <div
              v-for="(reason, index) in detail.keyReasons"
              :key="`${reason.reasonType || 'reason'}-${index}`"
              class="key-reason-item"
            >
              <div class="key-reason-head">
                <strong>{{ reason.reasonLabel || formatReasonType(reason.reasonType) }}</strong>
                <span>{{ formatTime(reason.createTime) }}</span>
              </div>
              <p>{{ reason.reasonDetail || "已触发重点监管规则" }}</p>
            </div>
          </div>
        </div>

        <div v-if="detail" class="product-panel">
          <div class="section-title section-title--sub">产品档案</div>
          <div v-if="productLoading" class="status info">产品档案加载中...</div>
          <div v-else-if="!productRecords.length" class="status info">
            当前企业暂无产品档案。
          </div>
          <div v-else class="product-list">
            <div v-for="item in productRecords" :key="item.id" class="product-item">
              <div class="product-item__head">
                <strong>{{ item.productName || "-" }}</strong>
                <span>{{ formatProductStatus(item.status) }}</span>
              </div>
              <div class="product-item__meta">
                <span>类别：{{ item.category || "-" }}</span>
                <span>规格：{{ item.specification || "-" }}</span>
                <span>更新时间：{{ formatTime(item.updateTime) }}</span>
              </div>
              <p>{{ item.remark || "暂无备注" }}</p>
            </div>
          </div>
        </div>

        <button class="ghost back-btn" type="button" @click="handleBack">返回列表</button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref, watch } from "vue";
import { useRoute, useRouter } from "vue-router";
import { fetchEnterpriseDetail, fetchEnterpriseProducts, fetchRegionPath } from "../api/regulation";
import { getActiveSession } from "../session/authRuntime";
import { formatByMap, formatTime } from "../utils/formatters";
import { approvalStatusMap, enterpriseStatusMap } from "../utils/statusMaps";

const router = useRouter();
const route = useRoute();
const token = computed(() => getActiveSession()?.token || "");

const loading = ref(false);
const detail = ref(null);
const regionName = ref("");
const productLoading = ref(false);
const productRecords = ref([]);

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
        ? path.map((item) => item.name).join("/")
        : "";
    }

    productLoading.value = true;
    try {
      productRecords.value = await fetchEnterpriseProducts(token.value, enterpriseId);
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

function handleBack() {
  const fromSection = typeof route.query.from === "string" ? route.query.from : "enterprises";
  const isAdminRoute = String(route.name || "").startsWith("regulator-admin");
  const routeNameMap = isAdminRoute
    ? {
        enterprises: "regulator-admin-enterprises",
        approvals: "regulator-admin-approvals",
        dispatch: "regulator-admin-dispatch",
        sampling: "regulator-admin-sampling",
        inspections: "regulator-admin-inspections",
        complaints: "regulator-admin-complaints",
        rectification: "regulator-admin-rectifications",
        warnings: "regulator-admin-warnings",
        bulletins: "regulator-admin-bulletins",
        stats: "regulator-admin-stats"
      }
    : {
        enterprises: "regulator-enforcer-enterprises",
        tasks: "regulator-enforcer-tasks",
        sampling: "regulator-enforcer-sampling",
        inspections: "regulator-enforcer-inspections",
        complaints: "regulator-enforcer-complaints",
        rectification: "regulator-enforcer-rectifications",
        warnings: "regulator-enforcer-warnings",
        stats: "regulator-enforcer-stats"
      };
  const fallbackName = isAdminRoute ? "regulator-admin-enterprises" : "regulator-enforcer-enterprises";
  router.push({ name: routeNameMap[fromSection] || fallbackName }).catch(() => {});
}

function formatStatus(value) {
  return formatByMap(value, enterpriseStatusMap);
}

function formatApprovalStatus(value) {
  return formatByMap(value, approvalStatusMap);
}

function formatProductStatus(value) {
  const map = {
    ACTIVE: "启用",
    INACTIVE: "停用"
  };
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

onMounted(loadDetail);
watch(() => route.params.enterpriseId, loadDetail);
</script>

<style scoped>
.detail-shell {
  grid-template-columns: 1fr;
}

.detail-shell .hero-panel {
  padding: 40px 80px 24px;
}

.detail-shell .form-panel {
  padding: 10px 80px 60px;
  align-items: flex-start;
}

.detail-shell .card {
  max-width: 900px;
  width: 100%;
}

.detail-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px 24px;
  margin-top: 10px;
}

.detail-grid span {
  display: block;
  font-size: 12px;
  color: var(--muted);
  margin-bottom: 6px;
}

.detail-grid strong {
  font-size: 14px;
}

.back-btn {
  margin-top: 20px;
}

.key-reason-panel {
  margin-top: 22px;
  padding-top: 18px;
  border-top: 1px solid var(--stroke);
}

.section-title--sub {
  margin-bottom: 12px;
}

.product-panel {
  margin-top: 22px;
  padding-top: 18px;
  border-top: 1px solid var(--stroke);
}

.product-list {
  display: grid;
  gap: 12px;
}

.product-item {
  padding: 14px 16px;
  border-radius: 14px;
  border: 1px solid var(--stroke);
  background: var(--card-strong);
  display: grid;
  gap: 8px;
}

.product-item__head,
.product-item__meta {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
}

.product-item__meta {
  font-size: 12px;
  color: var(--muted);
}

.product-item p {
  margin: 0;
  color: var(--text);
  font-size: 13px;
  line-height: 1.6;
}

.key-reason-list {
  display: grid;
  gap: 12px;
}

.key-reason-item {
  padding: 14px 16px;
  border-radius: 14px;
  border: 1px solid rgba(194, 118, 12, 0.18);
  background: rgba(255, 246, 232, 0.9);
}

.key-reason-head {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: center;
}

.key-reason-head span {
  margin-bottom: 0;
}

.key-reason-item p {
  margin: 8px 0 0;
  color: var(--text);
  font-size: 13px;
  line-height: 1.6;
}

.status.info {
  background: var(--card-strong);
  color: var(--muted);
}

@media (max-width: 900px) {
  .detail-shell .hero-panel {
    padding: 32px 40px 20px;
  }

  .detail-shell .form-panel {
    padding: 10px 40px 50px;
  }

  .detail-grid {
    grid-template-columns: 1fr;
  }
}
</style>
