<template>
  <SystemAdminWorkspacePage
    active-key="list"
    :username="adminUser.username"
    search-placeholder="搜索监管人员或辖区..."
    @navigate="handleSidebarNavigate"
    @logout="handleLogout"
  >
    <section class="region-adjust-page">
      <header class="region-adjust-page__header">
        <div class="region-adjust-page__title">
          <button type="button" class="back-btn" @click="goBack">
            <span class="material-symbols-outlined">arrow_back</span>
          </button>
          <div>
            <h1>调整监管人员辖区</h1>
            <p>为监管人员重新分配辖区，并将本次调整写入审计日志。</p>
          </div>
        </div>
        <button type="button" class="save-btn" :disabled="loading || saving || !profile" @click="handleSave">
          <span class="material-symbols-outlined">save</span>
          <span>{{ saving ? "保存中..." : "保存辖区调整" }}</span>
        </button>
      </header>

      <div class="region-adjust-layout">
        <section class="summary-card">
          <h2>监管人员摘要</h2>
          <div class="summary-grid">
            <article>
              <label>姓名</label>
              <p>{{ profile?.name || "-" }}</p>
            </article>
            <article>
              <label>角色</label>
              <p>{{ formatRoleType(profile?.roleType) }}</p>
            </article>
            <article>
              <label>当前辖区</label>
              <p>{{ currentRegionText || "-" }}</p>
            </article>
            <article>
              <label>目标层级</label>
              <p>{{ targetLevelText }}</p>
            </article>
          </div>
        </section>

        <section class="form-card">
          <h2>选择新辖区</h2>
          <div class="region-grid">
            <select v-model="region.provinceId" @change="handleProvinceChange">
              <option value="">请选择省份</option>
              <option v-for="item in regions.provinces" :key="item.id" :value="item.id">{{ item.name }}</option>
            </select>
            <select v-model="region.cityId" :disabled="!region.provinceId" @change="handleCityChange">
              <option value="">请选择城市</option>
              <option v-for="item in regions.cities" :key="item.id" :value="item.id">{{ item.name }}</option>
            </select>
            <select v-model="region.countyId" :disabled="!region.cityId" @change="handleCountyChange">
              <option value="">请选择区县</option>
              <option v-for="item in regions.counties" :key="item.id" :value="item.id">{{ item.name }}</option>
            </select>
            <select v-model="region.streetId" :disabled="!region.countyId">
              <option value="">请选择街道</option>
              <option v-for="item in regions.streets" :key="item.id" :value="item.id">{{ item.name }}</option>
            </select>
          </div>

          <div class="path-panel">
            <div>
              <label>当前辖区路径</label>
              <p>{{ currentRegionText || "-" }}</p>
            </div>
            <div>
              <label>调整后辖区路径</label>
              <p>{{ targetRegionText || "-" }}</p>
            </div>
          </div>

          <label class="remark-field">
            <span>调整备注</span>
            <textarea
              v-model.trim="remark"
              rows="4"
              maxlength="120"
              placeholder="请填写本次辖区调整原因"
            ></textarea>
          </label>
        </section>
      </div>

      <div v-if="status.message" class="status-toast" :class="status.type">{{ status.message }}</div>
    </section>
  </SystemAdminWorkspacePage>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from "vue";
import { useRoute, useRouter } from "vue-router";
import {
  adjustRegulatorRegions,
  fetchRegulatorProfileByUserId,
  fetchRegionPath,
  fetchRegions
} from "../../api/regulation";
import SystemAdminWorkspacePage from "../../components/systemAdmin/SystemAdminWorkspacePage.vue";
import { resolveErrorMessage } from "../../utils/uiFeedback";
import { useSystemAdminShellSession } from "./systemAdminShared";

const route = useRoute();
const router = useRouter();
const { adminUser, token, handleSidebarNavigate, handleLogout } = useSystemAdminShellSession();

const loading = ref(false);
const saving = ref(false);
const profile = ref(null);
const currentRegionText = ref("");
const targetRegionText = ref("");
const remark = ref("");
const status = reactive({ message: "", type: "" });
const regions = reactive({ provinces: [], cities: [], counties: [], streets: [] });
const region = reactive({ provinceId: "", cityId: "", countyId: "", streetId: "" });

const userId = computed(() => Number(route.params.userId || 0) || 0);
const targetLevelText = computed(() => (
  profile.value?.roleType === "REGULATOR_ADMIN" ? "区县级" : "街道级"
));

function setStatus(message, type = "info") {
  status.message = message;
  status.type = type;
}

function goBack() {
  router.push({
    name: "admin-regulator-detail",
    params: { userId: userId.value },
    query: { refreshedAt: String(Date.now()) }
  });
}

function goAfterSave() {
  router.replace({
    name: "admin-regulator-detail",
    params: { userId: userId.value },
    query: { refreshedAt: String(Date.now()) }
  });
}

function formatRoleType(roleType) {
  if (roleType === "REGULATOR_ADMIN") return "监管管理员";
  if (roleType === "REGULATOR_ENFORCER") return "监管执法人员";
  return roleType ? String(roleType) : "未知角色";
}

function resetRegion(level) {
  if (level === "province") {
    region.cityId = "";
    region.countyId = "";
    region.streetId = "";
    regions.cities = [];
    regions.counties = [];
    regions.streets = [];
  } else if (level === "city") {
    region.countyId = "";
    region.streetId = "";
    regions.counties = [];
    regions.streets = [];
  } else if (level === "county") {
    region.streetId = "";
    regions.streets = [];
  }
}

async function loadRegionOptions(parentId, targetKey) {
  regions[targetKey] = await fetchRegions(token.value, parentId);
}

async function handleProvinceChange() {
  resetRegion("province");
  const provinceId = Number(region.provinceId || 0);
  if (!provinceId) return;
  await loadRegionOptions(provinceId, "cities");
}

async function handleCityChange() {
  resetRegion("city");
  const cityId = Number(region.cityId || 0);
  if (!cityId) return;
  await loadRegionOptions(cityId, "counties");
}

async function handleCountyChange() {
  resetRegion("county");
  const countyId = Number(region.countyId || 0);
  if (!countyId) return;
  await loadRegionOptions(countyId, "streets");
}

function resolveTargetRegionId() {
  if (profile.value?.roleType === "REGULATOR_ADMIN") {
    return Number(region.countyId || 0) || null;
  }
  return Number(region.streetId || 0) || null;
}

async function syncTargetRegionText() {
  const regionId = resolveTargetRegionId();
  if (!regionId) {
    targetRegionText.value = "";
    return;
  }
  try {
    const pathList = await fetchRegionPath(token.value, regionId);
    targetRegionText.value = Array.isArray(pathList) ? pathList.map((item) => item.name).join(" / ") : "";
  } catch {
    targetRegionText.value = `辖区 ID: ${regionId}`;
  }
}

async function fillRegionByExisting(regionId) {
  if (!regionId) return;
  const pathList = await fetchRegionPath(token.value, regionId).catch(() => []);
  const ids = Array.isArray(pathList) ? pathList.map((item) => String(item.id)) : [];
  region.provinceId = ids[0] || "";
  if (region.provinceId) {
    await loadRegionOptions(Number(region.provinceId), "cities");
  }
  region.cityId = ids[1] || "";
  if (region.cityId) {
    await loadRegionOptions(Number(region.cityId), "counties");
  }
  region.countyId = ids[2] || "";
  if (region.countyId) {
    await loadRegionOptions(Number(region.countyId), "streets");
  }
  region.streetId = ids[3] || "";
}

async function loadPage() {
  if (!userId.value) {
    setStatus("缺少监管人员 userId 参数", "error");
    return;
  }
  loading.value = true;
  setStatus("");
  try {
    await loadRegionOptions(null, "provinces");
    const data = await fetchRegulatorProfileByUserId(token.value, userId.value);
    profile.value = data;
    const currentRegionId = Array.isArray(data?.regionIds) ? Number(data.regionIds[0] || 0) : 0;
    if (currentRegionId) {
      const pathList = await fetchRegionPath(token.value, currentRegionId).catch(() => []);
      currentRegionText.value = Array.isArray(pathList) ? pathList.map((item) => item.name).join(" / ") : `辖区 ID: ${currentRegionId}`;
      await fillRegionByExisting(currentRegionId);
      targetRegionText.value = currentRegionText.value;
    }
  } catch (error) {
    setStatus(resolveErrorMessage(error, "加载监管人员信息失败"), "error");
  } finally {
    loading.value = false;
  }
}

async function handleSave() {
  if (!profile.value?.id) return;
  const targetRegionId = resolveTargetRegionId();
  if (!targetRegionId) {
    setStatus(profile.value?.roleType === "REGULATOR_ADMIN" ? "请选择区县级辖区" : "请选择街道级辖区", "error");
    return;
  }
  const currentRegionId = Array.isArray(profile.value?.regionIds) ? Number(profile.value.regionIds[0] || 0) : 0;
  if (currentRegionId === targetRegionId) {
    setStatus("所选辖区与当前辖区一致，无需重复提交", "warning");
    return;
  }
  saving.value = true;
  setStatus("");
  try {
    await adjustRegulatorRegions(token.value, profile.value.id, {
      regionIds: [targetRegionId],
      remark: remark.value
    });
    setStatus("辖区调整成功", "success");
    setTimeout(() => {
      goAfterSave();
    }, 450);
  } catch (error) {
    setStatus(resolveErrorMessage(error, "辖区调整失败"), "error");
  } finally {
    saving.value = false;
  }
}

watch(
  () => [region.provinceId, region.cityId, region.countyId, region.streetId, profile.value?.roleType],
  () => {
    syncTargetRegionText();
  }
);

onMounted(() => {
  loadPage();
});
</script>

<style scoped>
.region-adjust-page { display: grid; gap: 16px; }
.region-adjust-page__header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}
.region-adjust-page__title {
  display: flex;
  align-items: center;
  gap: 12px;
}
.region-adjust-page__title h1 {
  margin: 0;
  color: #002660;
  font-size: 28px;
  font-weight: 900;
}
.region-adjust-page__title p {
  margin: 4px 0 0;
  color: #64748b;
  font-size: 13px;
}
.back-btn,
.save-btn {
  border: 0;
  border-radius: 2px;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  gap: 6px;
}
.back-btn {
  width: 40px;
  height: 40px;
  background: #e6e8eb;
  color: #002660;
  justify-content: center;
}
.save-btn {
  background: #002660;
  color: #fff;
  padding: 10px 14px;
  font-size: 12px;
  font-weight: 800;
}
.save-btn:disabled { opacity: 0.6; cursor: default; }
.region-adjust-layout {
  display: grid;
  grid-template-columns: minmax(0, 4fr) minmax(0, 6fr);
  gap: 14px;
}
.summary-card,
.form-card {
  background: #fff;
  border: 1px solid rgba(226, 232, 240, 0.9);
  border-radius: 2px;
  box-shadow: 0 1px 2px rgba(15, 23, 42, 0.05);
  padding: 16px;
}
.summary-card h2,
.form-card h2 {
  margin: 0 0 12px;
  color: #002660;
  font-size: 12px;
  letter-spacing: 0.12em;
  text-transform: uppercase;
  font-weight: 900;
}
.summary-grid {
  display: grid;
  gap: 10px;
}
.summary-grid article,
.path-panel,
.remark-field textarea {
  background: #f8fafc;
}
.summary-grid article {
  border: 1px solid #e2e8f0;
  border-radius: 2px;
  padding: 12px;
}
.summary-grid label,
.path-panel label,
.remark-field span {
  display: block;
  color: #64748b;
  font-size: 10px;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  font-weight: 800;
}
.summary-grid p,
.path-panel p {
  margin: 8px 0 0;
  color: #191c1e;
  font-size: 13px;
  font-weight: 700;
  line-height: 1.55;
}
.region-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}
.region-grid select,
.remark-field textarea {
  width: 100%;
  border: 0;
  border-radius: 2px;
  background: #e0e3e6;
  padding: 11px 12px;
  font-size: 13px;
  outline: 1px solid rgba(195, 198, 211, 0.15);
  outline-offset: -1px;
}
.path-panel {
  margin-top: 12px;
  border: 1px solid #e2e8f0;
  border-radius: 2px;
  padding: 12px;
  display: grid;
  gap: 12px;
}
.remark-field {
  display: grid;
  gap: 6px;
  margin-top: 12px;
}
.remark-field textarea {
  min-height: 96px;
  resize: vertical;
}
.status-toast {
  position: fixed;
  right: 18px;
  bottom: 18px;
  color: #fff;
  border-radius: 8px;
  padding: 10px 12px;
  font-size: 12px;
  background: #0f172a;
  z-index: 60;
}
.status-toast.error { background: #b91c1c; }
.status-toast.success { background: #166534; }
.status-toast.warning { background: #b45309; }

@media (max-width: 960px) {
  .region-adjust-page__header,
  .region-adjust-layout {
    grid-template-columns: 1fr;
    flex-direction: column;
  }
  .region-grid {
    grid-template-columns: 1fr;
  }
}
</style>
