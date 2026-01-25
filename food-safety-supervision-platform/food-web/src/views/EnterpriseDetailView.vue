<template>
  <div class="app-shell detail-shell">
    <div class="hero-panel">
      <div class="hero-content">
        <span class="badge">企业详情</span>
        <h1>企业备案信息</h1>
        <p>只读展示企业备案详情，监管人员不可在此修改。</p>
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

        <button class="ghost back-btn" type="button" @click="handleBack">返回列表</button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref, watch } from "vue";
import { fetchEnterpriseDetail, fetchRegionPath } from "../api/regulation";

const props = defineProps({
  token: {
    type: String,
    required: true
  },
  enterpriseId: {
    type: [String, Number],
    required: true
  }
});

const emit = defineEmits(["back"]);

const loading = ref(false);
const detail = ref(null);
const regionName = ref("");

const statusMap = {
  NORMAL: "正常",
  KEY: "重点监管"
};

const approvalStatusMap = {
  PENDING: "待审核",
  APPROVED: "已通过",
  REJECTED: "已驳回"
};

async function loadDetail() {
  if (!props.enterpriseId) {
    detail.value = null;
    return;
  }
  loading.value = true;
  try {
    detail.value = await fetchEnterpriseDetail(props.token, props.enterpriseId);
    regionName.value = "";
    if (detail.value?.regionId) {
      const path = await fetchRegionPath(props.token, detail.value.regionId).catch(() => []);
      regionName.value = Array.isArray(path) && path.length
        ? path.map((item) => item.name).join("/")
        : "";
    }
  } catch (error) {
    detail.value = null;
    regionName.value = "";
  } finally {
    loading.value = false;
  }
}

function handleBack() {
  emit("back");
}

function formatTime(value) {
  if (!value) return "-";
  return String(value).replace("T", " ").slice(0, 16);
}

function formatStatus(value) {
  return statusMap[value] || value || "-";
}

function formatApprovalStatus(value) {
  return approvalStatusMap[value] || value || "-";
}

onMounted(loadDetail);
watch(() => props.enterpriseId, loadDetail);
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
