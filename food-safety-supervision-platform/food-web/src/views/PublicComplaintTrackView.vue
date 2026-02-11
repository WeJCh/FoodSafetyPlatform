<template>
  <div class="public-shell">
    <header class="public-topbar">
      <div class="brand">
        <span class="brand-mark">食安</span>
        <div>
          <strong>投诉进度查询</strong>
          <span>输入投诉编号查询处理进度</span>
        </div>
      </div>
      <div class="user-area">
        <button class="ghost" type="button" @click="$emit('back')">返回首页</button>
        <button class="ghost" type="button" @click="$emit('logout')">退出登录</button>
      </div>
    </header>

    <section class="form-card">
      <h3>查询投诉进度</h3>
      <form class="track-form" @submit.prevent="handleSearch">
        <label>
          投诉编号
          <input v-model.trim="form.complaintNo" placeholder="请输入投诉编号" required />
        </label>
        <label>
          联系方式（实名投诉）
          <input v-model.trim="form.contact" placeholder="手机号（可选）" />
        </label>
        <button class="primary" type="submit" :disabled="loading">
          {{ loading ? "查询中..." : "查询进度" }}
        </button>
      </form>
    </section>

    <section class="result-card" v-if="result">
      <div class="result-header">
        <div>
          <strong>投诉编号：{{ result.complaintNo }}</strong>
          <span>最近更新：{{ formatTime(result.updateTime) }}</span>
        </div>
        <span class="status-pill">{{ formatStatus(result.status) }}</span>
      </div>

      <div class="timeline">
        <div
          v-for="(step, index) in steps"
          :key="step.key"
          :class="['timeline-step', { active: index <= currentIndex }]"
        >
          <div class="dot"></div>
          <div class="content">
            <strong>{{ step.label }}</strong>
            <span v-if="index === currentIndex">处理时间：{{ formatTime(result.updateTime) }}</span>
            <span v-else>等待更新</span>
          </div>
        </div>
      </div>
    </section>

    <section class="empty-card" v-else>
      <p>请输入投诉编号查询进度。</p>
    </section>

    <div class="status" :class="status.type" v-if="status.message">{{ status.message }}</div>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from "vue";
import { trackComplaint } from "../api/regulation";

const props = defineProps({
  publicUser: {
    type: Object,
    required: true
  },
  initialComplaintNo: {
    type: String,
    default: ""
  },
  initialContact: {
    type: String,
    default: ""
  }
});

defineEmits(["back", "logout"]);

const form = reactive({ complaintNo: "", contact: "" });
const loading = ref(false);
const result = ref(null);
const status = reactive({ message: "", type: "" });

const steps = [
  { key: "SUBMITTED", label: "已提交" },
  { key: "PENDING", label: "已受理" },
  { key: "ASSIGNED", label: "已派发" },
  { key: "PROCESSING", label: "处理中" },
  { key: "FEEDBACKED", label: "已反馈" }
];

const currentIndex = computed(() => {
  if (!result.value?.status) return -1;
  return steps.findIndex((step) => step.key === result.value.status);
});

function setStatus(message, type = "info") {
  status.message = message;
  status.type = type;
}

function formatStatus(value) {
  const map = {
    SUBMITTED: "已提交",
    PENDING: "已受理",
    ASSIGNED: "已派发",
    PROCESSING: "处理中",
    FEEDBACKED: "已反馈"
  };
  return map[value] || value || "-";
}

function formatTime(value) {
  if (!value) return "-";
  return String(value).replace("T", " ").slice(0, 16);
}

async function handleSearch() {
  if (!form.complaintNo.trim()) {
    setStatus("请输入投诉编号", "error");
    return;
  }
  loading.value = true;
  setStatus("");
  try {
    result.value = await trackComplaint(form.complaintNo, form.contact);
    setStatus("查询成功", "success");
  } catch (error) {
    result.value = null;
    setStatus(error.message || "单号或联系方式不匹配", "error");
  } finally {
    loading.value = false;
  }
}

onMounted(() => {
  if (form.complaintNo) return;
  form.complaintNo = props.initialComplaintNo || "";
  form.contact = props.initialContact || "";
  if (form.complaintNo) {
    handleSearch();
  }
});
</script>

<style scoped>
.public-shell {
  min-height: 100vh;
  padding: 28px 48px 46px;
  background: var(--bg);
  display: grid;
  gap: 20px;
}

.public-topbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 14px 18px;
  border-radius: 16px;
  border: 1px solid var(--stroke);
  background: #ffffff;
  box-shadow: var(--shadow);
}

.brand {
  display: flex;
  align-items: center;
  gap: 12px;
}

.brand-mark {
  width: 40px;
  height: 40px;
  border-radius: 12px;
  display: grid;
  place-items: center;
  background: var(--nav);
  color: #fff;
  font-weight: 700;
  letter-spacing: 0.08em;
}

.user-area {
  display: flex;
  gap: 12px;
}

.form-card,
.result-card,
.empty-card {
  background: #ffffff;
  border: 1px solid var(--stroke);
  border-radius: 16px;
  padding: 18px 20px;
  box-shadow: var(--shadow);
}

.track-form {
  display: grid;
  gap: 14px;
}

.result-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
}

.result-header span {
  color: var(--muted);
  font-size: 12px;
}

.status-pill {
  padding: 4px 10px;
  border-radius: 999px;
  background: #e9f1f8;
  color: var(--primary);
  font-size: 12px;
  font-weight: 600;
}

.timeline {
  margin-top: 16px;
  display: grid;
  gap: 12px;
}

.timeline-step {
  display: flex;
  gap: 12px;
  align-items: flex-start;
}

.timeline-step .dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  margin-top: 6px;
  background: #d7e2ee;
}

.timeline-step.active .dot {
  background: var(--primary);
}

.timeline-step .content {
  display: grid;
  gap: 4px;
}

.timeline-step .content span {
  font-size: 12px;
  color: var(--muted);
}

.empty-card {
  color: var(--muted);
  text-align: center;
}

@media (max-width: 900px) {
  .public-shell {
    padding: 20px 18px 36px;
  }

  .result-header {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
