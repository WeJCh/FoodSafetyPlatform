<template>
  <div class="public-shell">
    <header class="public-topbar">
      <div class="brand">
        <span class="brand-mark">食安</span>
        <div>
          <strong>公众投诉服务</strong>
          <span>投诉提交与受理说明</span>
        </div>
      </div>
      <div class="user-area">
        <button class="ghost" type="button" @click="$emit('back')">返回首页</button>
        <button class="ghost" type="button" @click="$emit('logout')">退出登录</button>
      </div>
    </header>

    <section class="notice-card">
      <h3>投诉须知</h3>
      <ul>
        <li>请如实填写投诉内容，恶意虚假投诉将承担相应法律责任。</li>
        <li>平台严格保护个人隐私，支持匿名投诉。</li>
        <li>投诉提交后将生成编号，请妥善保存用于查询进度。</li>
      </ul>
    </section>

    <section class="form-card" v-if="!success">
      <h3>投诉信息填报</h3>
      <form class="complaint-form" @submit.prevent="handleSubmit">
        <div class="form-row">
          <label class="combo-field">
            投诉企业名称（搜索选择）
            <div class="combo">
              <input
                v-model.trim="enterpriseQuery"
                class="combo-input"
                placeholder="输入企业名称关键词"
                @focus="openEnterpriseDropdown"
                @input="handleEnterpriseInput"
                @blur="scheduleCloseEnterpriseDropdown"
              />
              <button class="ghost combo-toggle" type="button" @click="toggleEnterpriseDropdown">
                选择
              </button>
            </div>
            <div class="combo-panel" v-if="enterpriseDropdownOpen">
              <div class="combo-loading" v-if="enterpriseLoading">加载中...</div>
              <button
                v-for="item in enterpriseOptions"
                :key="item.id"
                class="combo-item"
                type="button"
                @mousedown.prevent="selectEnterprise(item)"
              >
                <div class="combo-title">{{ item.enterpriseName }}</div>
                <div class="combo-meta" v-if="item.regionPathText || item.addressDetail">
                  {{ [item.regionPathText, item.addressDetail].filter(Boolean).join(" · ") }}
                </div>
              </button>
              <div class="combo-empty" v-if="!enterpriseLoading && !enterpriseOptions.length">
                暂无匹配企业
              </div>
              <div class="combo-footer" v-if="enterpriseHasMore">
                <button class="ghost" type="button" @mousedown.prevent="loadMoreEnterprises">
                  加载更多
                </button>
              </div>
            </div>
          </label>
          <label>
            企业编号（自动填充）
            <input v-model="form.enterpriseId" placeholder="请选择企业后自动生成" readonly />
          </label>
        </div>
        <div class="form-row">
          <label>
            所在区域
            <input
              v-model.trim="form.region"
              placeholder="例：浙江省/杭州市/西湖区"
              @input="markRegionEdited"
            />
          </label>
          <label>
            详细地址
            <input
              v-model.trim="form.addressDetail"
              placeholder="街道、门牌号"
              @input="markAddressEdited"
            />
          </label>
        </div>
        <label>
          投诉类型
          <select v-model="form.complaintType">
            <option value="">请选择投诉类型</option>
            <option value="食品过期">食品过期</option>
            <option value="卫生不达标">卫生不达标</option>
            <option value="无证经营">无证经营</option>
            <option value="其他">其他</option>
          </select>
        </label>
        <label>
          问题描述
          <textarea v-model.trim="form.content" rows="5" placeholder="请详细描述投诉内容" required></textarea>
        </label>
        <div class="upload-panel">
          <div>
            <strong>现场图片</strong>
            <span>演示版本仅预览，不上传后端</span>
          </div>
          <input type="file" multiple accept="image/*" @change="handleFileChange" />
          <div class="preview-grid" v-if="previews.length">
            <div v-for="(src, index) in previews" :key="src" class="preview-item">
              <img :src="src" alt="预览" />
              <button type="button" class="ghost" @click="removeImage(index)">移除</button>
            </div>
          </div>
        </div>
        <div class="anonymous-row">
          <label class="checkbox">
            <input type="checkbox" v-model="form.anonymous" @change="handleAnonymousToggle" />
            匿名投诉（隐藏联系方式）
          </label>
        </div>
        <div class="form-row" v-if="!form.anonymous">
          <label>
            真实姓名
            <input v-model.trim="form.complainantName" placeholder="可选填写" />
          </label>
          <label>
            联系方式（手机号）
            <input v-model.trim="form.contact" placeholder="用于联系与查询" />
          </label>
        </div>
        <button class="primary" type="submit" :disabled="loading">
          {{ loading ? "提交中..." : "提交投诉" }}
        </button>
      </form>
    </section>

    <section class="success-card" v-else>
      <h3>投诉已提交</h3>
      <p>您的投诉已受理，请保存投诉编号用于查询进度。</p>
      <div class="code-box">
        <span>投诉编号</span>
        <strong>{{ success.complaintNo }}</strong>
        <button class="ghost" type="button" @click="copyComplaintNo">复制</button>
      </div>
      <div class="status-line">当前状态：{{ formatStatus(success.status) }}</div>
      <div class="actions">
        <button class="primary" type="button" @click="goTrack">去查询进度</button>
        <button class="ghost" type="button" @click="resetForm">继续提交</button>
      </div>
    </section>

    <div class="status" :class="status.type" v-if="status.message">{{ status.message }}</div>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref, watch } from "vue";
import { fetchPublicEnterprises, submitPublicComplaint } from "../api/regulation";

const props = defineProps({
  publicUser: {
    type: Object,
    required: true
  },
  publicToken: {
    type: String,
    required: true
  }
});

const emit = defineEmits(["back", "logout", "open-track"]);

const form = reactive({
  enterpriseName: "",
  enterpriseId: "",
  region: "",
  addressDetail: "",
  complaintType: "",
  content: "",
  complainantName: "",
  contact: "",
  anonymous: false
});

const previews = ref([]);
const loading = ref(false);
const success = ref(null);
const status = reactive({ message: "", type: "" });
const enterpriseQuery = ref("");
const enterpriseOptions = ref([]);
const enterpriseLoading = ref(false);
const enterpriseDropdownOpen = ref(false);
const enterprisePage = ref(1);
const enterpriseSize = 10;
const enterpriseHasMore = ref(false);
const regionEdited = ref(false);
const addressEdited = ref(false);
let enterpriseSearchTimer = null;

function setStatus(message, type = "info") {
  status.message = message;
  status.type = type;
}

function handleAnonymousToggle() {
  if (form.anonymous) {
    form.complainantName = "";
    form.contact = "";
  }
}

function openEnterpriseDropdown() {
  enterpriseDropdownOpen.value = true;
  if (!enterpriseOptions.value.length) {
    loadEnterprises(true);
  }
}

function scheduleCloseEnterpriseDropdown() {
  window.setTimeout(() => {
    enterpriseDropdownOpen.value = false;
  }, 150);
}

function toggleEnterpriseDropdown() {
  enterpriseDropdownOpen.value = !enterpriseDropdownOpen.value;
  if (enterpriseDropdownOpen.value && !enterpriseOptions.value.length) {
    loadEnterprises(true);
  }
}

function handleEnterpriseInput() {
  enterpriseDropdownOpen.value = true;
  if (enterpriseQuery.value !== form.enterpriseName) {
    form.enterpriseId = "";
    form.enterpriseName = "";
  }
  if (enterpriseSearchTimer) {
    window.clearTimeout(enterpriseSearchTimer);
  }
  enterpriseSearchTimer = window.setTimeout(() => {
    loadEnterprises(true);
  }, 300);
}

async function loadEnterprises(reset) {
  if (enterpriseLoading.value) {
    return;
  }
  if (!props.publicToken) {
    setStatus("请先登录后再查询企业", "error");
    return;
  }
  enterpriseLoading.value = true;
  if (reset) {
    enterprisePage.value = 1;
  }
  try {
    const data = await fetchPublicEnterprises(props.publicToken, {
      enterpriseName: enterpriseQuery.value,
      page: enterprisePage.value,
      size: enterpriseSize
    });
    const records = data?.records || [];
    if (reset) {
      enterpriseOptions.value = records;
    } else {
      enterpriseOptions.value = [...enterpriseOptions.value, ...records];
    }
    const total = data?.total ?? enterpriseOptions.value.length;
    enterpriseHasMore.value = enterpriseOptions.value.length < total;
  } catch (error) {
    setStatus(error.message || "企业列表加载失败", "error");
  } finally {
    enterpriseLoading.value = false;
  }
}

function loadMoreEnterprises() {
  if (enterpriseLoading.value || !enterpriseHasMore.value) {
    return;
  }
  enterprisePage.value += 1;
  loadEnterprises(false);
}

function selectEnterprise(item) {
  form.enterpriseId = String(item.id || "");
  form.enterpriseName = item.enterpriseName || "";
  enterpriseQuery.value = form.enterpriseName;
  if (!regionEdited.value) {
    form.region = item.regionPathText || "";
  }
  if (!addressEdited.value) {
    form.addressDetail = item.addressDetail || "";
  }
  enterpriseDropdownOpen.value = false;
}

function handleFileChange(event) {
  const files = Array.from(event.target.files || []);
  previews.value = files.map((file) => URL.createObjectURL(file));
}

function removeImage(index) {
  previews.value.splice(index, 1);
}

function formatStatus(value) {
  const map = {
    SUBMITTED: "已提交",
    PENDING: "待受理",
    ASSIGNED: "已派发",
    PROCESSING: "处理中",
    FEEDBACKED: "已反馈"
  };
  return map[value] || value || "-";
}

async function handleSubmit() {
  if (!form.enterpriseId.trim()) {
    setStatus("请填写企业编号", "error");
    return;
  }
  const enterpriseId = Number(form.enterpriseId);
  if (!Number.isFinite(enterpriseId)) {
    setStatus("企业编号格式不正确", "error");
    return;
  }
  if (!form.content.trim()) {
    setStatus("请填写投诉内容", "error");
    return;
  }
  if (!form.anonymous && !form.contact.trim()) {
    setStatus("请填写联系方式或选择匿名投诉", "error");
    return;
  }
  loading.value = true;
  setStatus("");
  try {
    const payload = {
      enterpriseId,
      complaintType: form.complaintType || undefined,
      content: form.content,
      contact: form.anonymous ? undefined : form.contact,
      complainantName: form.anonymous ? undefined : form.complainantName
    };
    success.value = await submitPublicComplaint(payload);
    setStatus("投诉提交成功", "success");
  } catch (error) {
    setStatus(error.message || "投诉提交失败", "error");
  } finally {
    loading.value = false;
  }
}

async function copyComplaintNo() {
  if (!success.value?.complaintNo) return;
  try {
    await navigator.clipboard.writeText(success.value.complaintNo);
    setStatus("投诉编号已复制", "success");
  } catch {
    setStatus("复制失败，请手动复制", "error");
  }
}

function goTrack() {
  if (!success.value?.complaintNo) return;
  const contact = form.anonymous ? "" : form.contact;
  emit("open-track", { complaintNo: success.value.complaintNo, contact });
}

function resetForm() {
  success.value = null;
  form.enterpriseName = "";
  form.enterpriseId = "";
  form.region = "";
  form.addressDetail = "";
  form.complaintType = "";
  form.content = "";
  form.complainantName = "";
  form.contact = "";
  form.anonymous = false;
  previews.value = [];
  regionEdited.value = false;
  addressEdited.value = false;
  setStatus("");
}

function markRegionEdited() {
  regionEdited.value = true;
}

function markAddressEdited() {
  addressEdited.value = true;
}

onMounted(() => {
  if (props.publicToken) {
    loadEnterprises(true);
  }
});

watch(
  () => props.publicToken,
  (value, prev) => {
    if (value && value !== prev) {
      loadEnterprises(true);
    }
  }
);
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

.notice-card,
.form-card,
.success-card {
  background: #ffffff;
  border: 1px solid var(--stroke);
  border-radius: 16px;
  padding: 18px 20px;
  box-shadow: var(--shadow);
}

.notice-card h3,
.form-card h3,
.success-card h3 {
  margin-top: 0;
}

.notice-card ul {
  margin: 0;
  padding-left: 18px;
  color: var(--muted);
  display: grid;
  gap: 6px;
}

.complaint-form {
  display: grid;
  gap: 14px;
}

.form-row {
  display: grid;
  gap: 12px;
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.combo-field {
  position: relative;
}

.combo {
  display: flex;
  align-items: center;
  gap: 8px;
}

.combo-input {
  flex: 1;
}

.combo-toggle {
  min-width: 72px;
  height: 40px;
}

.combo-panel {
  position: absolute;
  top: calc(100% + 6px);
  left: 0;
  right: 0;
  background: #fff;
  border: 1px solid var(--stroke);
  border-radius: 12px;
  box-shadow: var(--shadow);
  padding: 8px;
  display: grid;
  gap: 6px;
  z-index: 10;
  max-height: 260px;
  overflow: auto;
}

.combo-item {
  display: grid;
  gap: 4px;
  text-align: left;
  padding: 8px 10px;
  border-radius: 10px;
  border: 1px solid transparent;
  background: #f6f9ff;
  transition: 0.2s ease;
}

.combo-item:hover {
  border-color: var(--nav);
  background: #eef4ff;
}

.combo-title {
  font-weight: 600;
  color: var(--text);
}

.combo-meta {
  font-size: 12px;
  color: var(--muted);
}

.combo-loading,
.combo-empty {
  padding: 6px 8px;
  font-size: 12px;
  color: var(--muted);
}

.combo-footer {
  display: flex;
  justify-content: center;
}

.upload-panel {
  border: 1px dashed var(--stroke);
  border-radius: 12px;
  padding: 12px;
  display: grid;
  gap: 10px;
  color: var(--muted);
  font-size: 13px;
}

.preview-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(120px, 1fr));
  gap: 10px;
}

.preview-item {
  border: 1px solid var(--stroke);
  border-radius: 10px;
  padding: 8px;
  display: grid;
  gap: 6px;
  background: #f9fbff;
}

.preview-item img {
  width: 100%;
  height: 90px;
  object-fit: cover;
  border-radius: 8px;
}

.anonymous-row {
  display: flex;
  align-items: center;
}

.checkbox {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  color: var(--muted);
}

.code-box {
  display: flex;
  gap: 12px;
  align-items: center;
  background: #f0f6ff;
  border-radius: 12px;
  padding: 12px 14px;
  margin: 12px 0;
}

.code-box strong {
  font-size: 18px;
  letter-spacing: 0.08em;
}

.status-line {
  color: var(--muted);
  margin-bottom: 14px;
}

.actions {
  display: flex;
  gap: 12px;
}

@media (max-width: 900px) {
  .public-shell {
    padding: 20px 18px 36px;
  }

  .form-row {
    grid-template-columns: 1fr;
  }

  .actions {
    flex-direction: column;
  }
}
</style>
