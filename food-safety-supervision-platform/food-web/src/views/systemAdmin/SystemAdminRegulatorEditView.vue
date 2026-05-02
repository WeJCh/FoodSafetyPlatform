<template>
  <SystemAdminWorkspacePage
    active-key="list"
    :username="adminUser.username"
    search-placeholder="全局搜索人员、证件或案件..."
    @navigate="handleSidebarNavigate"
    @logout="handleLogout"
  >
    <section class="sys-admin-edit-page">
      <nav class="sys-admin-breadcrumb">
        <button type="button" @click="goList">人员管理</button>
        <span class="material-symbols-outlined">chevron_right</span>
        <button type="button" @click="goList">监管人员列表</button>
        <span class="material-symbols-outlined">chevron_right</span>
        <span class="is-current">编辑人员信息</span>
      </nav>

      <header class="sys-admin-titlebar">
        <div>
          <h1>编辑监管人员档案</h1>
          <p>
            当前正在修改账号
            <span class="is-mono">{{ accountTag }}</span>
            的基础信息与辖区归属。
          </p>
        </div>
        <span class="sys-admin-status-flag" :class="{ 'is-disabled': Number(form.status) !== 1 }">
          状态：{{ Number(form.status) === 1 ? "启用" : "停用" }}
        </span>
      </header>

      <div class="sys-admin-editor-grid">
        <div class="sys-admin-main-col">
          <section class="sys-admin-card is-readonly">
            <div class="sys-admin-card__head">
              <span class="material-symbols-outlined">lock</span>
              <h2>账号信息（只读）</h2>
            </div>
            <div class="sys-admin-account-grid">
              <div>
                <label>登录账号</label>
                <div class="sys-admin-readonly-box">{{ readonlyAccount.username }}</div>
              </div>
              <div>
                <label>注册时间</label>
                <div class="sys-admin-readonly-box">{{ readonlyAccount.registeredAt }}</div>
              </div>
            </div>
          </section>

          <section class="sys-admin-card">
            <div class="sys-admin-card__head">
              <span class="material-symbols-outlined">badge</span>
              <h2>基础档案信息</h2>
            </div>
            <div class="sys-admin-form-grid">
              <label>
                <span>姓名</span>
                <input v-model.trim="form.name" type="text" placeholder="请输入真实姓名" />
              </label>
              <label>
                <span>手机号</span>
                <input
                  v-model.trim="form.phone"
                  type="tel"
                  inputmode="numeric"
                  autocomplete="tel"
                  placeholder="请输入 11 位手机号"
                />
              </label>
            </div>
          </section>

          <section class="sys-admin-card">
            <div class="sys-admin-card__head">
              <span class="material-symbols-outlined">admin_panel_settings</span>
              <h2>角色与权限</h2>
            </div>
            <div class="sys-admin-role-grid">
              <label class="sys-admin-role-option" :class="{ active: form.roleType === 'REGULATOR_ADMIN' }">
                <input v-model="form.roleType" type="radio" value="REGULATOR_ADMIN" />
                <strong>监管管理员</strong>
                <p>负责辖区管理、任务协调与人员调度。</p>
              </label>
              <label class="sys-admin-role-option" :class="{ active: form.roleType === 'REGULATOR_ENFORCER' }">
                <input v-model="form.roleType" type="radio" value="REGULATOR_ENFORCER" />
                <strong>监管执法人员</strong>
                <p>负责现场检查、执法流程处理与证据采集。</p>
              </label>
            </div>
          </section>
        </div>

        <aside class="sys-admin-side-col">
          <section class="sys-admin-juri-card">
            <div class="sys-admin-juri-card__head">
              <div class="sys-admin-juri-title">
                <span class="material-symbols-outlined">location_on</span>
                <h2>辖区配置</h2>
              </div>
            </div>
            <div class="sys-admin-region-grid">
              <select v-model="region.provinceId" @change="handleProvinceChange">
                <option value="">请选择省</option>
                <option v-for="item in regions.provinces" :key="item.id" :value="item.id">{{ item.name }}</option>
              </select>
              <select v-model="region.cityId" :disabled="!region.provinceId" @change="handleCityChange">
                <option value="">请选择市</option>
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
            <div class="sys-admin-juri-path">
              <label>当前辖区路径</label>
              <p>{{ regionPathText || "辖区信息待补全" }}</p>
            </div>
          </section>

          <section class="sys-admin-audit-card">
            <h3>编辑记录说明</h3>
            <ul>
              <li v-for="item in auditTrail" :key="item.id">
                <strong>{{ item.title }}</strong>
                <p>{{ item.desc }}</p>
              </li>
            </ul>
          </section>
        </aside>
      </div>

      <footer class="sys-admin-bottom-bar">
        <div class="sys-admin-bottom-note">
          <span class="material-symbols-outlined">info</span>
          <span>未保存的修改将会丢失，保存后会覆盖原有档案信息。</span>
        </div>
        <div class="sys-admin-bottom-actions">
          <button type="button" class="btn-plain" :disabled="loading" @click="goDetail">取消编辑</button>
          <button type="button" class="btn-primary" :disabled="loading" @click="handleSave">
            <span class="material-symbols-outlined">save</span>
            <span>{{ loading ? "保存中..." : "保存修改" }}</span>
          </button>
        </div>
      </footer>

      <div v-if="status.message" class="sys-admin-status" :class="status.type">{{ status.message }}</div>
    </section>
  </SystemAdminWorkspacePage>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from "vue";
import { useRoute, useRouter } from "vue-router";
import {
  createRegulatorProfile,
  fetchRegulatorProfileByUserId,
  fetchRegionPath,
  fetchRegions
} from "../../api/regulation";
import { fetchUserById } from "../../api/auth";
import SystemAdminWorkspacePage from "../../components/systemAdmin/SystemAdminWorkspacePage.vue";
import { resolveErrorMessage } from "../../utils/uiFeedback";
import { useSystemAdminShellSession } from "./systemAdminShared";

const route = useRoute();
const router = useRouter();
const { adminUser, token, handleSidebarNavigate, handleLogout } = useSystemAdminShellSession();

const loading = ref(false);
const status = reactive({ message: "", type: "" });
const sourceProfile = ref(null);
const sourceUser = ref(null);
const regionPathText = ref("");

const form = reactive({
  userId: null,
  regulatorId: null,
  name: "",
  phone: "",
  roleType: "REGULATOR_ENFORCER",
  status: 1
});

const regions = reactive({ provinces: [], cities: [], counties: [], streets: [] });
const region = reactive({ provinceId: "", cityId: "", countyId: "", streetId: "" });

const auditTrail = ref([
  { id: "a1", title: "账号档案", desc: "本页用于维护监管人员姓名、手机号、角色和辖区。" },
  { id: "a2", title: "保存结果", desc: "保存成功后将返回详情页，并展示最新档案内容。" }
]);

const userId = computed(() => Number(route.params.userId || 0) || 0);
const accountTag = computed(() => (form.userId ? `REGULATOR-${form.userId}` : "REGULATOR-UNKNOWN"));
const readonlyAccount = computed(() => ({
  username: sourceUser.value?.username || (form.userId ? `regulator_${form.userId}` : "-"),
  registeredAt: sourceProfile.value?.createTime ? formatDateTime(sourceProfile.value.createTime) : "-"
}));

function setStatus(message, type = "info") {
  status.message = message;
  status.type = type;
}

function formatDateTime(value) {
  if (!value) return "-";
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return String(value);
  return date.toLocaleString("zh-CN", { hour12: false });
}

function goList() {
  router.push({ name: "admin-regulator-list" });
}

function goDetail() {
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

function normalizePhone(phone) {
  return String(phone || "").replace(/\D/g, "");
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
  if (form.roleType === "REGULATOR_ADMIN") return Number(region.countyId || 0) || null;
  return Number(region.streetId || 0) || null;
}

async function syncRegionPathText() {
  const target = resolveTargetRegionId();
  if (!target) {
    regionPathText.value = "";
    return;
  }
  try {
    const pathList = await fetchRegionPath(token.value, target);
    regionPathText.value = Array.isArray(pathList) ? pathList.map((item) => item.name).join(" / ") : "";
  } catch {
    regionPathText.value = `辖区 ID: ${target}`;
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

async function loadDetail() {
  if (!userId.value) {
    setStatus("缺少监管人员 userId 参数", "error");
    return;
  }
  loading.value = true;
  setStatus("");
  try {
    await loadRegionOptions(null, "provinces");
    const data = await fetchRegulatorProfileByUserId(token.value, userId.value);
    sourceProfile.value = data;
    sourceUser.value = await fetchUserById(userId.value, token.value).catch(() => null);
    form.userId = data?.userId || userId.value;
    form.regulatorId = data?.id || null;
    form.name = data?.name || "";
    form.phone = data?.phone || "";
    form.roleType = data?.roleType || "REGULATOR_ENFORCER";
    form.status = Number(data?.status) === 1 ? 1 : 0;
    const firstRegionId = Array.isArray(data?.regionIds) ? Number(data.regionIds[0] || 0) : 0;
    if (firstRegionId) {
      await fillRegionByExisting(firstRegionId);
      await syncRegionPathText();
    }
  } catch (error) {
    setStatus(resolveErrorMessage(error, "加载监管人员信息失败"), "error");
  } finally {
    loading.value = false;
  }
}

async function handleSave() {
  const normalizedPhone = normalizePhone(form.phone);
  if (!form.name.trim()) {
    setStatus("姓名不能为空", "error");
    return;
  }
  if (!/^\d{11}$/.test(normalizedPhone)) {
    setStatus("手机号必须为 11 位数字", "error");
    return;
  }
  const targetRegionId = resolveTargetRegionId();
  if (!targetRegionId) {
    setStatus(form.roleType === "REGULATOR_ADMIN" ? "监管管理员需选择区县级辖区" : "监管执法人员需选择街道级辖区", "error");
    return;
  }

  loading.value = true;
  setStatus("");
  try {
    await createRegulatorProfile(token.value, {
      userId: form.userId,
      name: form.name.trim(),
      phone: normalizedPhone,
      roleType: form.roleType,
      regionIds: [targetRegionId],
      status: form.status
    });
    setStatus("保存成功", "success");
    setTimeout(() => {
      goAfterSave();
    }, 500);
  } catch (error) {
    setStatus(resolveErrorMessage(error, "保存失败"), "error");
  } finally {
    loading.value = false;
  }
}

onMounted(() => {
  loadDetail();
});
</script>

<style scoped>
.sys-admin-edit-page { display: grid; gap: 14px; padding-bottom: 88px; }
.sys-admin-breadcrumb { display: flex; align-items: center; gap: 4px; color: #64748b; font-size: 11px; font-weight: 600; }
.sys-admin-breadcrumb button { border: 0; background: transparent; color: inherit; cursor: pointer; padding: 0; }
.sys-admin-breadcrumb button:hover { color: #002660; }
.sys-admin-breadcrumb .material-symbols-outlined { font-size: 14px; color: #c3c6d3; }
.sys-admin-breadcrumb .is-current { color: #002660; font-weight: 800; }

.sys-admin-titlebar { display: flex; justify-content: space-between; align-items: end; gap: 12px; border-bottom: 1px solid #e6e8eb; padding-bottom: 10px; }
.sys-admin-titlebar h1 { margin: 0; color: #002660; font-size: 28px; font-weight: 900; letter-spacing: -0.01em; }
.sys-admin-titlebar p { margin: 4px 0 0; color: #64748b; font-size: 13px; }
.is-mono { font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, "Liberation Mono", "Courier New", monospace; font-weight: 800; color: #0f172a; }
.sys-admin-status-flag { border-radius: 2px; background: rgba(0, 38, 96, 0.1); color: #002660; padding: 5px 10px; font-size: 10px; font-weight: 900; letter-spacing: 0.08em; text-transform: uppercase; }
.sys-admin-status-flag.is-disabled { background: rgba(148, 163, 184, 0.2); color: #475569; }

.sys-admin-editor-grid { display: grid; grid-template-columns: minmax(0, 8fr) minmax(0, 4fr); gap: 12px; align-items: start; }
.sys-admin-main-col, .sys-admin-side-col { display: grid; gap: 12px; min-width: 0; }

.sys-admin-card { background: #fff; border: 1px solid rgba(226, 232, 240, 0.8); border-radius: 2px; box-shadow: 0 1px 2px rgba(15, 23, 42, 0.05); padding: 14px; }
.sys-admin-card.is-readonly { background: #f2f4f7; border-left: 4px solid rgba(0, 38, 96, 0.2); }
.sys-admin-card__head { display: flex; align-items: center; gap: 8px; margin-bottom: 10px; }
.sys-admin-card__head .material-symbols-outlined { color: #002660; font-size: 20px; }
.sys-admin-card__head h2 { margin: 0; color: #002660; font-size: 12px; letter-spacing: 0.12em; text-transform: uppercase; font-weight: 900; }

.sys-admin-account-grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 10px; }
.sys-admin-account-grid label { display: block; color: #64748b; font-size: 10px; font-weight: 900; letter-spacing: 0.12em; text-transform: uppercase; margin-bottom: 5px; }
.sys-admin-readonly-box { background: rgba(224, 227, 230, 0.7); border: 1px solid rgba(195, 198, 211, 0.3); border-radius: 2px; padding: 10px; color: #0f172a; font-size: 12px; font-weight: 700; }

.sys-admin-form-grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 10px 12px; }
.sys-admin-form-grid label > span { display: block; color: #64748b; font-size: 10px; font-weight: 900; letter-spacing: 0.12em; text-transform: uppercase; margin-bottom: 6px; }
.sys-admin-form-grid input,
.sys-admin-region-grid select { width: 100%; border: 0; border-radius: 2px; background: #e0e3e6; padding: 11px 12px; font-size: 13px; outline: 1px solid rgba(195, 198, 211, 0.15); outline-offset: -1px; transition: background-color .15s ease, outline-color .15s ease; }
.sys-admin-form-grid input:focus,
.sys-admin-region-grid select:focus { outline: 1px solid rgba(0, 38, 96, 0.85); background: #fff; }

.sys-admin-role-grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 10px; }
.sys-admin-role-option { position: relative; border: 2px solid transparent; background: #f2f4f7; border-radius: 2px; padding: 10px 12px; cursor: pointer; }
.sys-admin-role-option.active { border-color: #002660; background: rgba(217, 226, 255, 0.35); }
.sys-admin-role-option input { position: absolute; top: 10px; right: 10px; }
.sys-admin-role-option strong { display: block; color: #002660; font-size: 13px; font-weight: 900; }
.sys-admin-role-option p { margin: 5px 0 0; color: #64748b; font-size: 11px; line-height: 1.45; }

.sys-admin-juri-card { background: linear-gradient(135deg, #002660, #003a8c); border-radius: 2px; padding: 14px; color: #fff; box-shadow: 0 20px 40px rgba(0, 38, 96, 0.18); }
.sys-admin-juri-card__head { display: flex; align-items: center; justify-content: space-between; gap: 8px; margin-bottom: 12px; }
.sys-admin-juri-title { display: flex; align-items: center; gap: 6px; }
.sys-admin-juri-title .material-symbols-outlined { color: #bfdbfe; }
.sys-admin-juri-title h2 { margin: 0; font-size: 11px; letter-spacing: 0.14em; text-transform: uppercase; color: #dbeafe; font-weight: 900; }
.sys-admin-region-grid { display: grid; grid-template-columns: 1fr; gap: 8px; }
.sys-admin-region-grid select { background: rgba(255,255,255,.94); outline-color: rgba(255,255,255,.4); }
.sys-admin-juri-path { margin-top: 12px; padding-top: 12px; border-top: 1px solid rgba(255,255,255,.18); }
.sys-admin-juri-path label { color: #bfdbfe; font-size: 10px; font-weight: 900; letter-spacing: 0.12em; text-transform: uppercase; }
.sys-admin-juri-path p { margin: 6px 0 0; color: #eff6ff; font-size: 12px; font-weight: 700; line-height: 1.45; }

.sys-admin-audit-card { background: #fff; border: 1px solid rgba(195, 198, 211, 0.4); border-radius: 2px; padding: 14px; }
.sys-admin-audit-card h3 { margin: 0 0 10px; color: #64748b; font-size: 11px; font-weight: 900; letter-spacing: 0.14em; text-transform: uppercase; }
.sys-admin-audit-card ul { margin: 0; padding: 0 0 0 14px; display: grid; gap: 10px; }
.sys-admin-audit-card li strong { color: #0f172a; font-size: 12px; }
.sys-admin-audit-card li p { margin: 3px 0 0; color: #64748b; font-size: 11px; }

.sys-admin-bottom-bar { position: fixed; left: 256px; right: 0; bottom: 0; height: 72px; background: rgba(255, 255, 255, 0.92); backdrop-filter: blur(10px); border-top: 1px solid #e2e8f0; display: flex; align-items: center; justify-content: space-between; gap: 10px; padding: 0 14px; z-index: 40; }
.sys-admin-bottom-note { display: inline-flex; align-items: center; gap: 6px; color: #64748b; font-size: 12px; }
.sys-admin-bottom-note .material-symbols-outlined { font-size: 16px; }
.sys-admin-bottom-actions { display: inline-flex; align-items: center; gap: 8px; }
.btn-plain, .btn-primary { border: 0; border-radius: 2px; padding: 10px 14px; font-size: 12px; font-weight: 900; cursor: pointer; display: inline-flex; align-items: center; gap: 6px; }
.btn-plain { background: transparent; color: #0f172a; }
.btn-primary { background: #002660; color: #fff; box-shadow: 0 10px 22px rgba(0, 38, 96, 0.2); }
.btn-primary:disabled, .btn-plain:disabled { opacity: .6; cursor: default; }

.sys-admin-status { position: fixed; right: 18px; bottom: 86px; color: #fff; border-radius: 8px; padding: 10px 12px; font-size: 12px; background: #0f172a; z-index: 55; }
.sys-admin-status.error { background: #b91c1c; }
.sys-admin-status.success { background: #166534; }

@media (max-width: 1200px) {
  .sys-admin-editor-grid { grid-template-columns: 1fr; }
  .sys-admin-bottom-bar { left: 0; }
}

@media (max-width: 900px) {
  .sys-admin-titlebar { align-items: start; flex-direction: column; }
  .sys-admin-account-grid,
  .sys-admin-form-grid,
  .sys-admin-role-grid { grid-template-columns: 1fr; }
  .sys-admin-bottom-note { display: none; }
}
</style>
