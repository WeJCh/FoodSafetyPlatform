<template>
  <div class="app-shell">
    <div class="hero-panel">
      <div class="hero-content">
        <span class="badge">企业备案</span>
        <h1>企业信息备案与审核状态</h1>
        <p>
          企业用户首次登录后，需要完善企业信息并提交审核。审核通过后才能使用其他监管业务功能。
        </p>
        <div class="hero-highlights">
          <div>
            <strong>填写企业信息</strong>
            <span>提交后进入待审核状态</span>
          </div>
          <div>
            <strong>等待审核</strong>
            <span>监管人员完成审批</span>
          </div>
          <div>
            <strong>审核通过</strong>
            <span>解锁全部业务功能</span>
          </div>
        </div>
      </div>
    </div>

    <div class="form-panel">
      <div class="card">
        <div class="section-title">企业备案</div>
        <div class="admin-info">
          <div>账号：{{ enterpriseUser.username }}</div>
          <div>类型：{{ enterpriseUser.userType }}</div>
        </div>

        <div class="status-banner" :class="statusTone">
          <div class="status-title">当前审核状态：{{ statusLabel }}</div>
          <div v-if="profile.approvalComment" class="status-note">
            审核意见：{{ profile.approvalComment }}
          </div>
          <div v-if="profile.approvedTime" class="status-note">
            审核时间：{{ profile.approvedTime }}
          </div>
          <div v-if="!profileLoaded" class="status-note">
            暂无备案记录，请先提交企业信息。
          </div>
        </div>

        <form @submit.prevent="handleSubmit">
          <label>
            企业名称
            <input v-model.trim="form.enterpriseName" required placeholder="请输入企业名称" />
          </label>
          <label>
            许可证编号
            <input v-model.trim="form.licenseNo" placeholder="请输入许可证编号" />
          </label>
          <label>
            企业地址
            <input v-model.trim="form.address" placeholder="请输入企业地址" />
          </label>
          <label>
            负责人姓名
            <input v-model.trim="form.principal" placeholder="请输入负责人姓名" />
          </label>
          <label>
            负责人电话
            <input v-model.trim="form.principalPhone" placeholder="11 位手机号" />
          </label>
          <button class="primary" type="submit" :disabled="loading">
            {{ loading ? "提交中..." : submitLabel }}
          </button>
          <button class="ghost" type="button" @click="handleLogout">退出登录</button>
        </form>

        <div class="status" :class="status.type" v-if="status.message">
          {{ status.message }}
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from "vue";
import { fetchEnterpriseProfile, submitEnterpriseProfile } from "../api/regulation";

const props = defineProps({
  token: {
    type: String,
    required: true
  },
  enterpriseUser: {
    type: Object,
    required: true
  }
});

const emit = defineEmits(["logout"]);

const loading = ref(false);
const profileLoaded = ref(false);
const status = reactive({ message: "", type: "" });
const profile = reactive({
  approvalStatus: "",
  approvalComment: "",
  approvedTime: ""
});

const form = reactive({
  enterpriseName: "",
  licenseNo: "",
  address: "",
  principal: "",
  principalPhone: ""
});

const statusLabel = computed(() => {
  if (!profileLoaded.value) return "未提交";
  if (profile.approvalStatus === "APPROVED") return "已通过";
  if (profile.approvalStatus === "REJECTED") return "已驳回";
  if (profile.approvalStatus === "PENDING") return "待审核";
  return "未提交";
});

const statusTone = computed(() => {
  if (!profileLoaded.value) return "info";
  if (profile.approvalStatus === "APPROVED") return "success";
  if (profile.approvalStatus === "REJECTED") return "error";
  return "pending";
});

const submitLabel = computed(() => (profileLoaded.value ? "更新并重新提交" : "提交备案"));

function setStatus(message, type = "info") {
  status.message = message;
  status.type = type;
}

function resetForm(payload = {}) {
  form.enterpriseName = payload.enterpriseName || "";
  form.licenseNo = payload.licenseNo || "";
  form.address = payload.address || "";
  form.principal = payload.principal || "";
  form.principalPhone = payload.principalPhone || "";
}

async function loadProfile() {
  try {
    const data = await fetchEnterpriseProfile(props.token);
    profile.approvalStatus = data.approvalStatus || "";
    profile.approvalComment = data.approvalComment || "";
    profile.approvedTime = data.approvedTime || "";
    resetForm(data);
    profileLoaded.value = true;
  } catch (error) {
    if (String(error?.message).includes("not found")) {
      profileLoaded.value = false;
      resetForm();
      return;
    }
    setStatus(error.message || "加载失败", "error");
  }
}

async function handleSubmit() {
  loading.value = true;
  setStatus("");
  try {
    const data = await submitEnterpriseProfile(props.token, { ...form });
    profile.approvalStatus = data.approvalStatus || "PENDING";
    profile.approvalComment = data.approvalComment || "";
    profile.approvedTime = data.approvedTime || "";
    profileLoaded.value = true;
    setStatus("提交成功，已进入审核流程。", "success");
  } catch (error) {
    setStatus(error.message || "提交失败", "error");
  } finally {
    loading.value = false;
  }
}

function handleLogout() {
  emit("logout");
}

onMounted(() => {
  loadProfile();
});
</script>

<style scoped>
.status-banner {
  border-radius: 16px;
  padding: 14px 16px;
  margin-bottom: 18px;
  background: #f7efe6;
  border: 1px solid var(--stroke);
  color: var(--ink);
  display: grid;
  gap: 6px;
}

.status-banner.success {
  background: #f1f8ea;
  color: #2b5c22;
}

.status-banner.error {
  background: #fff1f1;
  color: #7c1d1d;
}

.status-banner.pending {
  background: #fff5e8;
  color: #8a4b00;
}

.status-title {
  font-weight: 600;
  font-size: 14px;
}

.status-note {
  font-size: 12px;
  color: inherit;
}
</style>
