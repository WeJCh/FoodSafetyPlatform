<template>
  <div class="app-shell detail-shell">
    <div class="hero-panel">
      <div class="hero-content">
        <span class="badge">企业详情</span>
        <h1>企业备案与产品档案</h1>
        <p>只读展示企业备案信息、附件材料与产品档案，不在此页面进行编辑。</p>
      </div>
    </div>

    <div class="form-panel">
      <div class="card">
        <div class="section-title">企业信息</div>
        <div v-if="loading" class="status info">加载中...</div>
        <div v-else-if="!detail" class="status error">未找到企业信息。</div>
        <div v-else class="detail-grid">
          <div>
            <span>企业名称</span>
            <strong>{{ detail.enterpriseName || "-" }}</strong>
          </div>
          <div>
            <span>食品经营许可证编号</span>
            <strong>{{ detail.licenseNo || "-" }}</strong>
          </div>
          <div>
            <span>统一社会信用代码</span>
            <strong>{{ detail.creditCode || "-" }}</strong>
          </div>
          <div>
            <span>法定代表人</span>
            <strong>{{ detail.legalRepresentative || "-" }}</strong>
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
            <span>更新时间</span>
            <strong>{{ formatTime(detail.updateTime) }}</strong>
          </div>
        </div>

        <div class="section-title section-title--sub">审核信息</div>
        <div class="detail-grid detail-grid--audit">
          <div>
            <span>审核人</span>
            <strong>{{ detail.approvedByName || "-" }}</strong>
          </div>
          <div>
            <span>审核时间</span>
            <strong>{{ formatTime(detail.approvedTime) }}</strong>
          </div>
          <div>
            <span>包保责任人</span>
            <strong>{{ detail.regulatorName || "-" }}</strong>
          </div>
          <div style="grid-column: span 2;">
            <span>审核意见</span>
            <strong>{{ detail.approvalComment || "-" }}</strong>
          </div>
        </div>

        <div v-if="detail" class="attachment-panel">
          <div class="section-title section-title--sub">备案附件</div>
          <div v-if="!detail.attachments || !detail.attachments.length" class="status info">
            当前企业暂无备案附件。
          </div>
          <div v-else class="attachment-list">
            <div
              v-for="(item, index) in detail.attachments"
              :key="`${item.type || 'attachment'}-${index}`"
              class="attachment-item"
            >
              <div class="attachment-item__head">
                <strong>{{ item.label || item.name || "备案附件" }}</strong>
                <span>{{ item.uploadedAt ? formatTime(item.uploadedAt) : "已上传" }}</span>
              </div>
              <p>{{ item.name || "未命名附件" }}</p>
              <a class="ghost inline-link" :href="item.url" target="_blank" rel="noreferrer">查看附件</a>
            </div>
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
              <p>{{ reason.reasonDetail || "已触发重点监管规则。" }}</p>
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
      regionName.value = Array.isArray(path) && path.length ? path.map((item) => item.name).join("/") : "";
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
  const routeNameMap = {
    enterprises: "regulator-enforcer-enterprises",
    tasks: "regulator-enforcer-tasks",
    sampling: "regulator-enforcer-sampling",
    inspections: "regulator-enforcer-inspections",
    complaints: "regulator-enforcer-complaints",
    rectification: "regulator-enforcer-rectifications",
    warnings: "regulator-enforcer-warnings",
    stats: "regulator-enforcer-stats"
  };
  const fallbackName = "regulator-enforcer-enterprises";
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
    CONSECUTIVE_INSPECTION_FAIL: "连续检查不合格",
    SAMPLING_FAIL: "抽检不合格",
    RECTIFICATION_OVERDUE: "整改逾期",
    WARNING_TRIGGERED: "预警触发",
    MANUAL_SET: "人工设定"
  };
  return map[value] || value || "-";
}

onMounted(loadDetail);
watch(() => route.params.enterpriseId, loadDetail);
</script>
