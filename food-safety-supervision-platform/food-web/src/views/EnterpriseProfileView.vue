<template>
  <div class="admin-shell enterprise-shell">
    <aside class="admin-sidebar">
      <div class="admin-brand">企业中心</div>
      <div class="sidebar-meta">
        <span>账号：{{ enterpriseUser.username }}</span>
        <span>类型：{{ enterpriseUser.userType }}</span>
      </div>
      <nav class="admin-nav">
        <button :class="{ active: section === 'profile' }" @click="section = 'profile'">
          企业备案
        </button>
        <button :class="{ active: section === 'inspections' }" @click="section = 'inspections'">
          检查结果
        </button>
        <button :class="{ active: section === 'rectification' }" @click="section = 'rectification'">
          整改任务
        </button>
      </nav>
      <button class="ghost sidebar-ghost" type="button" @click="handleLogout">退出登录</button>
    </aside>

    <div class="admin-main">
      <div class="dashboard-topbar">
        <div class="dashboard-title">
          <strong>企业用户工作台</strong>
          <span>备案维护、检查结果与整改跟进</span>
        </div>
        <div class="user-chip">
          <span>{{ enterpriseUser.username }}</span>
          <span>企业用户</span>
        </div>
      </div>

      <div class="dashboard-content">
        <div class="card dashboard-card">
          <div v-if="section === 'profile'">
            <div class="section-title">企业备案</div>

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
                省份
                <select v-model="regionSelection.provinceId" @change="handleProvinceChange">
                  <option value="">请选择省</option>
                  <option v-for="item in regionOptions.provinces" :key="item.id" :value="item.id">
                    {{ item.name }}
                  </option>
                </select>
              </label>
              <label>
                城市
                <select v-model="regionSelection.cityId" :disabled="!regionSelection.provinceId" @change="handleCityChange">
                  <option value="">请选择市</option>
                  <option v-for="item in regionOptions.cities" :key="item.id" :value="item.id">
                    {{ item.name }}
                  </option>
                </select>
              </label>
              <label>
                区县
                <select
                  v-model="regionSelection.countyId"
                  :disabled="!regionSelection.cityId"
                  @change="handleCountyChange"
                >
                  <option value="">请选择区县</option>
                  <option v-for="item in regionOptions.counties" :key="item.id" :value="item.id">
                    {{ item.name }}
                  </option>
                </select>
              </label>
              <label>
                街道
                <select v-model="regionSelection.streetId" :disabled="!regionSelection.countyId">
                  <option value="">请选择街道</option>
                  <option v-for="item in regionOptions.streets" :key="item.id" :value="item.id">
                    {{ item.name }}
                  </option>
                </select>
              </label>
              <div v-if="existingRegionText && !regionSelection.provinceId" class="hint">
                当前行政区：{{ existingRegionText }}
              </div>
              <label>
                详细地址
                <input v-model.trim="form.addressDetail" required placeholder="请输入详细地址" />
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
            </form>

            <div class="status" :class="status.type" v-if="status.message">
              {{ status.message }}
            </div>
          </div>

          <div v-else class="placeholder">
            <strong>功能占位</strong>
            <p>{{ sectionLabel }} 将在后续版本实现。</p>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from "vue";
import { fetchEnterpriseProfile, fetchRegions, submitEnterpriseProfile } from "../api/regulation";

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

const section = ref("profile");
const loading = ref(false);
const profileLoaded = ref(false);
const status = reactive({ message: "", type: "" });
const existingRegionId = ref(null);
const existingRegionText = ref("");
const existingRegionPath = ref([]);
const profile = reactive({
  approvalStatus: "",
  approvalComment: "",
  approvedTime: ""
});

const regionOptions = reactive({
  provinces: [],
  cities: [],
  counties: [],
  streets: []
});

const regionSelection = reactive({
  provinceId: "",
  cityId: "",
  countyId: "",
  streetId: ""
});

const form = reactive({
  enterpriseName: "",
  licenseNo: "",
  addressDetail: "",
  principal: "",
  principalPhone: ""
});

const sectionLabelMap = {
  inspections: "检查结果",
  rectification: "整改任务"
};

const sectionLabel = computed(() => sectionLabelMap[section.value] || "当前模块");

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
  form.addressDetail = payload.addressDetail || "";
  form.principal = payload.principal || "";
  form.principalPhone = payload.principalPhone || "";
}

async function loadProfile() {
  try {
    const data = await fetchEnterpriseProfile(props.token);
    profile.approvalStatus = data.approvalStatus || "";
    profile.approvalComment = data.approvalComment || "";
    profile.approvedTime = data.approvedTime || "";
    existingRegionId.value = data.regionId || null;
    existingRegionText.value = data.regionPathText || "";
    existingRegionPath.value = Array.isArray(data.regionPath) ? data.regionPath : [];
    resetForm(data);
    profileLoaded.value = true;
    if (existingRegionPath.value.length) {
      await applyRegionPath(existingRegionPath.value);
    }
  } catch (error) {
    if (String(error?.message).includes("not found")) {
      profileLoaded.value = false;
      existingRegionId.value = null;
      existingRegionText.value = "";
      existingRegionPath.value = [];
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
    const regionId = resolveEnterpriseRegionId();
    if (!regionId) {
      setStatus("请选择所属行政区", "error");
      return;
    }
    const data = await submitEnterpriseProfile(props.token, { ...form, regionId });
    profile.approvalStatus = data.approvalStatus || "PENDING";
    profile.approvalComment = data.approvalComment || "";
    profile.approvedTime = data.approvedTime || "";
    existingRegionId.value = data.regionId || existingRegionId.value;
    existingRegionText.value = data.regionPathText || existingRegionText.value;
    existingRegionPath.value = Array.isArray(data.regionPath) ? data.regionPath : existingRegionPath.value;
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

async function loadRegions(parentId, targetKey) {
  try {
    regionOptions[targetKey] = await fetchRegions(props.token, parentId);
  } catch (error) {
    setStatus(error.message || "加载行政区失败", "error");
  }
}

async function applyRegionPath(path) {
  if (!path || !path.length) {
    return;
  }
  const province = path[0];
  regionSelection.provinceId = province?.id ? String(province.id) : "";
  resetRegion("province");
  if (province?.id) {
    await loadRegions(province.id, "cities");
  }
  const city = path[1];
  if (city?.id) {
    regionSelection.cityId = String(city.id);
    await loadRegions(city.id, "counties");
  }
  const county = path[2];
  if (county?.id) {
    regionSelection.countyId = String(county.id);
    await loadRegions(county.id, "streets");
  }
  const street = path[3];
  if (street?.id) {
    regionSelection.streetId = String(street.id);
  }
}

function resetRegion(level) {
  if (level === "province") {
    regionSelection.cityId = "";
    regionSelection.countyId = "";
    regionSelection.streetId = "";
    regionOptions.cities = [];
    regionOptions.counties = [];
    regionOptions.streets = [];
  } else if (level === "city") {
    regionSelection.countyId = "";
    regionSelection.streetId = "";
    regionOptions.counties = [];
    regionOptions.streets = [];
  } else if (level === "county") {
    regionSelection.streetId = "";
    regionOptions.streets = [];
  }
}

async function handleProvinceChange() {
  resetRegion("province");
  const provinceId = Number(regionSelection.provinceId || 0);
  if (!provinceId) return;
  await loadRegions(provinceId, "cities");
}

async function handleCityChange() {
  resetRegion("city");
  const cityId = Number(regionSelection.cityId || 0);
  if (!cityId) return;
  await loadRegions(cityId, "counties");
}

async function handleCountyChange() {
  resetRegion("county");
  const countyId = Number(regionSelection.countyId || 0);
  if (!countyId) return;
  await loadRegions(countyId, "streets");
}

function resolveEnterpriseRegionId() {
  if (regionOptions.streets.length) {
    return Number(regionSelection.streetId || 0) || null;
  }
  if (regionOptions.counties.length) {
    return Number(regionSelection.countyId || 0) || null;
  }
  if (regionOptions.cities.length) {
    return Number(regionSelection.cityId || 0) || null;
  }
  return Number(regionSelection.provinceId || 0) || existingRegionId.value || null;
}

onMounted(() => {
  const init = async () => {
    await loadRegions(null, "provinces");
    await loadProfile();
  };
  init();
});
</script>

<style scoped>
.status-banner {
  border-radius: 16px;
  padding: 14px 16px;
  margin-bottom: 18px;
  background: var(--card-strong);
  border: 1px solid var(--stroke);
  color: var(--ink);
  display: grid;
  gap: 6px;
}

.status-banner.success {
  background: rgba(26, 127, 90, 0.12);
  color: var(--success);
}

.status-banner.error {
  background: rgba(192, 57, 43, 0.12);
  color: var(--danger);
}

.status-banner.pending {
  background: rgba(209, 122, 0, 0.12);
  color: var(--warning);
}

.status-title {
  font-weight: 600;
  font-size: 14px;
}

.status-note {
  font-size: 12px;
  color: inherit;
}

.hint {
  font-size: 12px;
  color: var(--muted);
  margin-top: -6px;
}

.enterprise-shell .admin-info {
  margin-bottom: 16px;
}

.enterprise-shell {
  grid-template-columns: 260px 1fr;
}

@media (max-width: 960px) {
  .enterprise-shell {
    grid-template-columns: 1fr;
  }
}
</style>
