<template>
  <div class="admin-shell">
    <aside class="admin-sidebar">
      <div class="admin-brand">管理员中心</div>
      <nav class="admin-nav">
        <button :class="{ active: section === 'regulators' }" @click="section = 'regulators'">
          监管人员管理
        </button>
        <button :class="{ active: section === 'enterprise-approval' }" @click="section = 'enterprise-approval'">
          企业审批
        </button>
        <button :class="{ active: section === 'users' }" @click="section = 'users'">
          用户管理
        </button>
        <button :class="{ active: section === 'logs' }" @click="section = 'logs'">
          操作日志
        </button>
      </nav>
      <button class="ghost" type="button" @click="('logout')">退出管理员页面</button>
    </aside>

    <div class="admin-main">
      <div class="hero-panel">
        <div class="hero-content">
          <span class="badge">管理员中心</span>
          <h1>系统管理员控制台</h1>
          <p>
            创建监管人员账号并分配角色类型。后续可在此扩展审核、账号管理等功能。
          </p>
          <div class="hero-highlights">
            <div>
              <strong>监管人员添加</strong>
              <span>创建执法人员或区域管理员</span>
            </div>
            <div>
              <strong>账号管理</strong>
              <span>后续扩展用户管理与审核</span>
            </div>
            <div>
              <strong>统一入口</strong>
              <span>管理员专属操作空间</span>
            </div>
          </div>
        </div>
      </div>

      <div class="form-panel">
        <div class="card">
          <div class="section-title">系统管理员</div>
          <div class="admin-info">
            <div>账号：{{ adminUser.username }}</div>
            <div>角色：{{ adminUser.userType }}</div>
          </div>

          <div v-if="section === 'regulators'" class="sub-nav">
            <button :class="{ active: subSection === 'create' }" @click="subSection = 'create'">
              添加监管人员
            </button>
            <button :class="{ active: subSection === 'list' }" @click="subSection = 'list'; loadRegulators()">
              监管人员列表
            </button>
            <button :class="{ active: subSection === 'roles' }" @click="subSection = 'roles'">
              角色调整
            </button>
            <button :class="{ active: subSection === 'transfer' }" @click="subSection = 'transfer'">
              区域交接
            </button>
            <button :class="{ active: subSection === 'disable' }" @click="subSection = 'disable'">
              启用/停用
            </button>
          </div>

          <form v-if="section === 'regulators' && subSection === 'create'" @submit.prevent="handleCreate">
            <label>
              监管人员账号
              <input v-model.trim="regulatorForm.username" required placeholder="请输入账号" />
            </label>
            <label>
              初始密码
              <input v-model.trim="regulatorForm.password" type="password" required placeholder="请输入密码" />
            </label>
            <label>
              姓名
              <input v-model.trim="regulatorForm.name" required placeholder="请输入姓名" />
            </label>
            <label>
              工作证件 URL（可选）
              <input v-model.trim="regulatorForm.workIdUrl" placeholder="证件链接" />
            </label>
            <label>
              联系电话
              <input v-model.trim="regulatorForm.phone" required placeholder="11 位手机号" />
            </label>
            <label>
              角色类型
              <select v-model="regulatorForm.roleType">
                <option value="REGULATOR_ENFORCER">执法人员</option>
                <option value="REGULATOR_ADMIN">区域管理员</option>
              </select>
            </label>
            <label>
              管辖省份
              <select v-model="regulatorRegion.provinceId" @change="handleRegulatorProvinceChange">
                <option value="">请选择省</option>
                <option v-for="item in regulatorRegions.provinces" :key="item.id" :value="item.id">
                  {{ item.name }}
                </option>
              </select>
            </label>
            <label>
              管辖城市
              <select
                v-model="regulatorRegion.cityId"
                :disabled="!regulatorRegion.provinceId"
                @change="handleRegulatorCityChange"
              >
                <option value="">请选择市</option>
                <option v-for="item in regulatorRegions.cities" :key="item.id" :value="item.id">
                  {{ item.name }}
                </option>
              </select>
            </label>
            <label>
              管辖区县
              <select
                v-model="regulatorRegion.countyId"
                :disabled="!regulatorRegion.cityId"
                @change="handleRegulatorCountyChange"
              >
                <option value="">请选择区县</option>
                <option v-for="item in regulatorRegions.counties" :key="item.id" :value="item.id">
                  {{ item.name }}
                </option>
              </select>
            </label>
            <label>
              管辖街道
              <select v-model="regulatorRegion.streetId" :disabled="!regulatorRegion.countyId">
                <option value="">请选择街道</option>
                <option v-for="item in regulatorRegions.streets" :key="item.id" :value="item.id">
                  {{ item.name }}
                </option>
              </select>
            </label>
            <button class="primary" type="submit" :disabled="loading">
              {{ loading ? "创建中..." : "创建监管人员" }}
            </button>
          </form>

          <div v-else-if="section === 'regulators' && subSection === 'list'">
            <div class="section-title">监管人员列表</div>
            <form class="filter-bar" @submit.prevent="handleSearch">
              <label>
                角色类型
                <select v-model="listQuery.roleType">
                  <option value="">全部</option>
                  <option value="REGULATOR_ENFORCER">执法人员</option>
                  <option value="REGULATOR_ADMIN">区域管理员</option>
                </select>
              </label>
              <label>
                省份
                <select v-model="filterRegion.provinceId" @change="handleFilterProvinceChange">
                  <option value="">请选择省</option>
                  <option v-for="item in filterRegions.provinces" :key="item.id" :value="item.id">
                    {{ item.name }}
                  </option>
                </select>
              </label>
              <label>
                城市
                <select
                  v-model="filterRegion.cityId"
                  :disabled="!filterRegion.provinceId"
                  @change="handleFilterCityChange"
                >
                  <option value="">请选择市</option>
                  <option v-for="item in filterRegions.cities" :key="item.id" :value="item.id">
                    {{ item.name }}
                  </option>
                </select>
              </label>
              <label>
                区县
                <select
                  v-model="filterRegion.countyId"
                  :disabled="!filterRegion.cityId"
                  @change="handleFilterCountyChange"
                >
                  <option value="">请选择区县</option>
                  <option v-for="item in filterRegions.counties" :key="item.id" :value="item.id">
                    {{ item.name }}
                  </option>
                </select>
              </label>
              <label>
                街道
                <select v-model="filterRegion.streetId" :disabled="!filterRegion.countyId">
                  <option value="">请选择街道</option>
                  <option v-for="item in filterRegions.streets" :key="item.id" :value="item.id">
                    {{ item.name }}
                  </option>
                </select>
              </label>
              <button class="primary" type="submit" :disabled="listLoading">
                {{ listLoading ? "查询中..." : "查询" }}
              </button>
            </form>

            <div class="list-table">
              <div class="list-row list-header">
                <span>姓名</span>
                <span>角色</span>
                <span>辖区ID</span>
                <span>状态</span>
                <span>操作</span>
              </div>
              <div v-if="!regulatorList.length" class="list-empty">
                暂无监管人员
              </div>
              <div v-for="item in regulatorList" :key="item.id" class="list-row">
                <span>{{ item.name }}</span>
                <span>{{ item.roleType }}</span>
                <span>{{ Array.isArray(item.regionIds) ? item.regionIds.join(",") : "-" }}</span>
                <span>{{ item.status === 1 ? "在岗" : "停用" }}</span>
                <button class="ghost" type="button" @click="handleToggle(item)">
                  {{ item.status === 1 ? "停用" : "启用" }}
                </button>
              </div>
            </div>
          </div>

          <div v-else-if="section === 'regulators'" class="placeholder">
            <strong>功能占位</strong>
            <p>{{ regulatorSubLabel }} 将在后续版本实现。</p>
          </div>

          <div v-else class="placeholder">
            <strong>功能占位</strong>
            <p>{{ sectionLabel }} 将在后续版本实现。</p>
          </div>

          <div class="status" :class="status.type" v-if="status.message">
            {{ status.message }}
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from "vue";
import { createRegulator } from "../api/auth";
import {
  createRegulatorProfile,
  fetchRegulatorProfiles,
  updateRegulatorStatus,
  fetchRegions
} from "../api/regulation";

const props = defineProps({
  adminUser: {
    type: Object,
    required: true
  },
  token: {
    type: String,
    required: true
  }
});

defineEmits(["logout"]);

const loading = ref(false);
const status = reactive({ message: "", type: "" });
const section = ref("regulators");
const subSection = ref("create");

const regulatorForm = reactive({
  username: "",
  password: "",
  name: "",
  workIdUrl: "",
  phone: "",
  roleType: "REGULATOR_ENFORCER"
});

const listQuery = reactive({
  roleType: "",
  regionId: ""
});

const regulatorRegions = reactive({
  provinces: [],
  cities: [],
  counties: [],
  streets: []
});

const regulatorRegion = reactive({
  provinceId: "",
  cityId: "",
  countyId: "",
  streetId: ""
});

const filterRegions = reactive({
  provinces: [],
  cities: [],
  counties: [],
  streets: []
});

const filterRegion = reactive({
  provinceId: "",
  cityId: "",
  countyId: "",
  streetId: ""
});

const regulatorList = ref([]);
const listLoading = ref(false);

async function loadRegulators() {
  listLoading.value = true;
  setStatus("");
  try {
    regulatorList.value = await fetchRegulatorProfiles(props.token, listQuery);
  } catch (error) {
    setStatus(error.message || "加载监管人员失败", "error");
  } finally {
    listLoading.value = false;
  }
}

const sectionLabel = computed(() => {
  if (section.value === "enterprise-approval") return "企业审批";
  if (section.value === "users") return "用户管理";
  if (section.value === "logs") return "操作日志";
  return "监管人员管理";
});

const regulatorSubLabel = computed(() => {
  if (subSection.value === "list") return "监管人员列表";
  if (subSection.value === "roles") return "角色调整";
  if (subSection.value === "transfer") return "区域交接";
  if (subSection.value === "disable") return "启用/停用";
  return "添加监管人员";
});

function setStatus(message, type = "info") {
  status.message = message;
  status.type = type;
}

async function handleCreate() {
  loading.value = true;
  setStatus("");
  try {
    const regionId = resolveRegulatorRegionId();
    if (!regionId) {
      setStatus("请选择管辖区域", "error");
      return;
    }
    const user = await createRegulator(
      {
        username: regulatorForm.username,
        password: regulatorForm.password,
        fullName: regulatorForm.name,
        phone: regulatorForm.phone,
        userType: "REGULATOR",
        roleType: regulatorForm.roleType
      },
      props.token
    );
    await createRegulatorProfile(props.token, {
      userId: user.id,
      name: regulatorForm.name,
      phone: regulatorForm.phone,
      roleType: regulatorForm.roleType,
      regionIds: [regionId],
      workIdUrl: regulatorForm.workIdUrl
    });
    setStatus("监管人员创建成功，档案已同步。", "success");
    subSection.value = "list";
    await loadRegulators();
  } catch (error) {
    setStatus(error.message || "创建失败", "error");
  } finally {
    loading.value = false;
  }
}

async function loadRegulatorRegions(parentId, targetKey) {
  try {
    regulatorRegions[targetKey] = await fetchRegions(props.token, parentId);
  } catch (error) {
    setStatus(error.message || "加载行政区失败", "error");
  }
}

function resetRegulatorRegion(level) {
  if (level === "province") {
    regulatorRegion.cityId = "";
    regulatorRegion.countyId = "";
    regulatorRegion.streetId = "";
    regulatorRegions.cities = [];
    regulatorRegions.counties = [];
    regulatorRegions.streets = [];
  } else if (level === "city") {
    regulatorRegion.countyId = "";
    regulatorRegion.streetId = "";
    regulatorRegions.counties = [];
    regulatorRegions.streets = [];
  } else if (level === "county") {
    regulatorRegion.streetId = "";
    regulatorRegions.streets = [];
  }
}

async function handleRegulatorProvinceChange() {
  resetRegulatorRegion("province");
  const provinceId = Number(regulatorRegion.provinceId || 0);
  if (!provinceId) return;
  await loadRegulatorRegions(provinceId, "cities");
}

async function handleRegulatorCityChange() {
  resetRegulatorRegion("city");
  const cityId = Number(regulatorRegion.cityId || 0);
  if (!cityId) return;
  await loadRegulatorRegions(cityId, "counties");
}

async function handleRegulatorCountyChange() {
  resetRegulatorRegion("county");
  const countyId = Number(regulatorRegion.countyId || 0);
  if (!countyId) return;
  await loadRegulatorRegions(countyId, "streets");
}

function resolveRegulatorRegionId() {
  if (regulatorRegions.streets.length) {
    return Number(regulatorRegion.streetId || 0) || null;
  }
  if (regulatorRegions.counties.length) {
    return Number(regulatorRegion.countyId || 0) || null;
  }
  if (regulatorRegions.cities.length) {
    return Number(regulatorRegion.cityId || 0) || null;
  }
  return Number(regulatorRegion.provinceId || 0) || null;
}

async function loadFilterRegions(parentId, targetKey) {
  try {
    filterRegions[targetKey] = await fetchRegions(props.token, parentId);
  } catch (error) {
    setStatus(error.message || "加载行政区失败", "error");
  }
}

function resetFilterRegion(level) {
  if (level === "province") {
    filterRegion.cityId = "";
    filterRegion.countyId = "";
    filterRegion.streetId = "";
    filterRegions.cities = [];
    filterRegions.counties = [];
    filterRegions.streets = [];
  } else if (level === "city") {
    filterRegion.countyId = "";
    filterRegion.streetId = "";
    filterRegions.counties = [];
    filterRegions.streets = [];
  } else if (level === "county") {
    filterRegion.streetId = "";
    filterRegions.streets = [];
  }
}

async function handleFilterProvinceChange() {
  resetFilterRegion("province");
  const provinceId = Number(filterRegion.provinceId || 0);
  if (!provinceId) return;
  await loadFilterRegions(provinceId, "cities");
}

async function handleFilterCityChange() {
  resetFilterRegion("city");
  const cityId = Number(filterRegion.cityId || 0);
  if (!cityId) return;
  await loadFilterRegions(cityId, "counties");
}

async function handleFilterCountyChange() {
  resetFilterRegion("county");
  const countyId = Number(filterRegion.countyId || 0);
  if (!countyId) return;
  await loadFilterRegions(countyId, "streets");
}

function resolveFilterRegionId() {
  if (filterRegion.streetId) return Number(filterRegion.streetId);
  if (filterRegion.countyId) return Number(filterRegion.countyId);
  if (filterRegion.cityId) return Number(filterRegion.cityId);
  if (filterRegion.provinceId) return Number(filterRegion.provinceId);
  return null;
}

async function loadProvinces() {
  try {
    const provinces = await fetchRegions(props.token, null);
    regulatorRegions.provinces = provinces;
    filterRegions.provinces = provinces;
  } catch (error) {
    setStatus(error.message || "加载行政区失败", "error");
  }
}

onMounted(async () => {
  await loadProvinces();
});

async function handleSearch() {
  const regionId = resolveFilterRegionId();
  listQuery.regionId = regionId ? String(regionId) : "";
  await loadRegulators();
}

async function handleToggle(regulator) {
  const nextStatus = regulator.status === 1 ? 0 : 1;
  try {
    await updateRegulatorStatus(props.token, regulator.id, nextStatus);
    regulator.status = nextStatus;
  } catch (error) {
    setStatus(error.message || "更新状态失败", "error");
  }
}
</script>

<style scoped>
.filter-bar {
  display: grid;
  gap: 12px;
  margin-bottom: 16px;
}

.list-table {
  border-radius: 14px;
  border: 1px solid var(--stroke);
  background: #faf6f1;
  overflow: hidden;
}

.list-row {
  display: grid;
  grid-template-columns: 1.4fr 1.2fr 1.6fr 0.8fr 0.8fr;
  gap: 8px;
  padding: 12px 14px;
  align-items: center;
  font-size: 13px;
}

.list-header {
  font-weight: 600;
  background: #f1e6db;
}

.list-empty {
  padding: 16px;
  color: var(--muted);
  font-size: 13px;
}
</style>
