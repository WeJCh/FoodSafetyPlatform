<template>
  <SystemAdminWorkspacePage
    active-key="create"
    :username="adminUser.username"
    search-placeholder="全局搜索人员或辖区..."
    @navigate="handleSidebarNavigate"
    @logout="handleLogout"
  >
    <section class="sys-admin-create-page">
      <header class="sys-admin-create-page__header">
        <nav class="sys-admin-breadcrumb">
          <span>人员管理</span>
          <span class="material-symbols-outlined">chevron_right</span>
          <span class="is-current">新建监管人员</span>
        </nav>
        <h1>新建监管人员</h1>
        <p>创建新的监管账号，并补全基础档案信息。</p>
      </header>

      <section class="sys-admin-stepper">
        <div class="step is-active"><span>1</span><strong>填写基础信息</strong></div>
        <div class="line is-active"></div>
        <div class="step"><span>2</span><strong>确认提交信息</strong></div>
        <div class="line"></div>
        <div class="step"><span>3</span><strong>创建完成</strong></div>
      </section>

      <div class="sys-admin-create-layout">
        <div class="main-col">
          <form class="form-panel" @submit.prevent="goConfirm">
            <section class="form-group is-emphasis">
              <h2><span class="material-symbols-outlined">account_circle</span>账号信息</h2>
              <div class="grid2">
                <label>
                  <span>登录账号</span>
                  <input v-model.trim="regulatorForm.username" required type="text" placeholder="请输入登录账号" />
                </label>
                <label>
                  <span>初始密码</span>
                  <input v-model.trim="regulatorForm.password" required type="password" placeholder="请输入初始密码" />
                </label>
                <div class="full-row account-switch">
                  <div>
                    <strong>账号状态</strong>
                    <p>新建完成后默认启用，可直接登录系统继续工作。</p>
                  </div>
                  <span class="enabled-chip">默认启用</span>
                </div>
              </div>
            </section>

            <section class="form-group">
              <h2><span class="material-symbols-outlined">description</span>基础档案信息</h2>
              <div class="grid2">
                <label>
                  <span>姓名</span>
                  <input v-model.trim="regulatorForm.name" required type="text" placeholder="请输入真实姓名" />
                </label>
                <label>
                  <span>手机号</span>
                  <input
                    v-model.trim="regulatorForm.phone"
                    required
                    type="tel"
                    inputmode="numeric"
                    autocomplete="tel"
                    placeholder="请输入 11 位手机号"
                  />
                </label>
              </div>
            </section>

            <section class="form-group">
              <h2><span class="material-symbols-outlined">hub</span>角色与辖区</h2>
              <div class="role-grid">
                <label class="role-card" :class="{ active: regulatorForm.roleType === 'REGULATOR_ADMIN' }">
                  <input v-model="regulatorForm.roleType" type="radio" value="REGULATOR_ADMIN" />
                  <div>
                    <strong>监管管理员</strong>
                    <p>负责辖区内综合管理、任务协调与人员调度。</p>
                  </div>
                </label>
                <label class="role-card" :class="{ active: regulatorForm.roleType === 'REGULATOR_ENFORCER' }">
                  <input v-model="regulatorForm.roleType" type="radio" value="REGULATOR_ENFORCER" />
                  <div>
                    <strong>监管执法人员</strong>
                    <p>负责现场检查、抽检执行与证据采集等工作。</p>
                  </div>
                </label>
              </div>

              <div class="region-wrap">
                <div class="region-grid">
                  <select v-model="regulatorRegion.provinceId" @change="handleRegulatorProvinceChange">
                    <option value="">请选择省</option>
                    <option v-for="item in regulatorRegions.provinces" :key="item.id" :value="item.id">{{ item.name }}</option>
                  </select>
                  <select v-model="regulatorRegion.cityId" :disabled="!regulatorRegion.provinceId" @change="handleRegulatorCityChange">
                    <option value="">请选择市</option>
                    <option v-for="item in regulatorRegions.cities" :key="item.id" :value="item.id">{{ item.name }}</option>
                  </select>
                  <select v-model="regulatorRegion.countyId" :disabled="!regulatorRegion.cityId" @change="handleRegulatorCountyChange">
                    <option value="">请选择区县</option>
                    <option v-for="item in regulatorRegions.counties" :key="item.id" :value="item.id">{{ item.name }}</option>
                  </select>
                  <select v-model="regulatorRegion.streetId" :disabled="!regulatorRegion.countyId">
                    <option value="">请选择街道</option>
                    <option v-for="item in regulatorRegions.streets" :key="item.id" :value="item.id">{{ item.name }}</option>
                  </select>
                </div>
              </div>
            </section>

            <footer class="form-actions">
              <button type="button" class="btn-secondary" @click="handleSidebarNavigate('list')">取消</button>
              <button type="submit" class="btn-primary" :disabled="loading">
                {{ loading ? "处理中..." : "下一步：确认信息" }}
                <span class="material-symbols-outlined">arrow_forward</span>
              </button>
            </footer>
          </form>
        </div>

        <aside class="side-col">
          <section class="notice-card">
            <h3>创建说明</h3>
            <ul>
              <li>监管账号应与实际岗位和辖区保持一致。</li>
              <li>初始密码建议在首次登录后立即修改。</li>
              <li>角色与辖区一旦提交，后续应通过编辑流程调整。</li>
            </ul>
          </section>

          <section class="log-card">
            <h4>当前操作</h4>
            <div class="log-item">
              <strong>创建会话已开始</strong>
              <small>{{ nowText }}</small>
            </div>
            <div class="log-item muted">
              <strong>等待表单提交</strong>
            </div>
          </section>
        </aside>
      </div>

      <div v-if="status.message" class="sys-admin-status" :class="status.type">{{ status.message }}</div>
    </section>
  </SystemAdminWorkspacePage>
</template>

<script setup>
import { onMounted, reactive, ref } from "vue";
import { useRouter } from "vue-router";
import { fetchRegions } from "../../api/regulation";
import SystemAdminWorkspacePage from "../../components/systemAdmin/SystemAdminWorkspacePage.vue";
import { resolveErrorMessage } from "../../utils/uiFeedback";
import { useSystemAdminShellSession } from "./systemAdminShared";

const { adminUser, token, handleSidebarNavigate, handleLogout } = useSystemAdminShellSession();
const router = useRouter();

const loading = ref(false);
const nowText = new Date().toLocaleString("zh-CN", { hour12: false });
const status = reactive({ message: "", type: "" });
const regulatorForm = reactive({
  username: "",
  password: "",
  name: "",
  phone: "",
  roleType: "REGULATOR_ENFORCER"
});
const regulatorRegions = reactive({ provinces: [], cities: [], counties: [], streets: [] });
const regulatorRegion = reactive({ provinceId: "", cityId: "", countyId: "", streetId: "" });

function setStatus(message, type = "info") {
  status.message = message;
  status.type = type;
}

async function loadRegulatorRegions(parentId, targetKey) {
  try {
    regulatorRegions[targetKey] = await fetchRegions(token.value, parentId);
  } catch (error) {
    setStatus(resolveErrorMessage(error, "加载行政区失败"), "error");
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

function resolveRegulatorRegionIdByRole() {
  if (regulatorForm.roleType === "REGULATOR_ADMIN") return Number(regulatorRegion.countyId || 0) || null;
  if (regulatorForm.roleType === "REGULATOR_ENFORCER") return Number(regulatorRegion.streetId || 0) || null;
  return null;
}

async function loadProvinces() {
  try {
    regulatorRegions.provinces = await fetchRegions(token.value, null);
  } catch (error) {
    setStatus(resolveErrorMessage(error, "加载行政区失败"), "error");
  }
}

async function goConfirm() {
  loading.value = true;
  setStatus("");
  try {
    const normalizedPhone = String(regulatorForm.phone || "").replace(/\D/g, "");
    if (!/^\d{11}$/.test(normalizedPhone)) {
      setStatus("手机号必须为 11 位数字", "error");
      return;
    }

    const regionId = resolveRegulatorRegionIdByRole();
    if (!regionId) {
      setStatus(
        regulatorForm.roleType === "REGULATOR_ADMIN" ? "监管管理员需选择区县级辖区" : "监管执法人员需选择街道级辖区",
        "error"
      );
      return;
    }

    const draft = {
      username: regulatorForm.username.trim(),
      password: regulatorForm.password,
      name: regulatorForm.name.trim(),
      phone: normalizedPhone,
      roleType: regulatorForm.roleType,
      regionId
    };
    sessionStorage.setItem("sysAdminCreateRegulatorDraft", JSON.stringify(draft));
    router.push({ name: "admin-regulator-create-confirm" });
  } catch (error) {
    setStatus(resolveErrorMessage(error, "信息校验失败"), "error");
  } finally {
    loading.value = false;
  }
}

onMounted(() => {
  loadProvinces();
});
</script>

<style scoped>
.sys-admin-create-page {
  display: grid;
  gap: 18px;
  width: 100%;
  margin: 0;
  padding: 0;
}
.sys-admin-create-page__header h1 { margin: 6px 0 0; color: #002660; font-size: 34px; font-weight: 900; }
.sys-admin-create-page__header p { margin: 6px 0 0; color: #64748b; font-size: 14px; }
.sys-admin-breadcrumb { display: flex; align-items: center; gap: 4px; color: #64748b; font-size: 10px; letter-spacing: .1em; text-transform: uppercase; }
.sys-admin-breadcrumb .material-symbols-outlined { font-size: 14px; }
.sys-admin-breadcrumb .is-current { color: #002660; font-weight: 700; }

.sys-admin-stepper { display: flex; align-items: center; width: 100%; }
.step { display: flex; align-items: center; gap: 8px; color: #64748b; }
.step span { width: 30px; height: 30px; border-radius: 4px; background: #e0e3e6; display: grid; place-items: center; font-weight: 700; }
.step strong { font-size: 13px; font-weight: 600; }
.step.is-active { color: #002660; }
.step.is-active span { background: #002660; color: #fff; }
.line { flex: 1; height: 2px; background: #e0e3e6; margin: 0 10px; }
.line.is-active { background: #003a8c; }

.sys-admin-create-layout {
  display: grid;
  grid-template-columns: repeat(12, minmax(0, 1fr));
  gap: 12px;
  align-items: start;
  width: 100%;
}
.main-col {
  grid-column: 1 / span 10;
  min-width: 0;
  width: 100%;
}
.form-panel {
  width: 100%;
  min-width: 0;
  max-width: none;
  justify-self: stretch;
  background: transparent;
  border: 0;
  box-shadow: none;
  border-radius: 0;
  padding: 0;
  display: grid;
  gap: 10px;
}
.form-panel > * {
  width: 100%;
  min-width: 0;
  max-width: none;
}
.form-group { background: #fff; border: 1px solid rgba(226, 232, 240, 0.6); box-shadow: 0 1px 2px rgba(15, 23, 42, 0.05); border-radius: 2px; padding: 14px 14px; }
.form-group.is-emphasis { border-left: 4px solid #002660; padding-left: 12px; }
.form-group h2 { margin: 0 0 12px; color: #191c1e; font-size: 18px; font-weight: 800; display: flex; align-items: center; gap: 8px; }
.form-group h2 .material-symbols-outlined { color: #002660; }
.grid2 { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 10px 12px; }
.full-row { grid-column: 1 / -1; }
.form-group label > span { display: block; margin-bottom: 5px; color: #475569; font-size: 10px; letter-spacing: .08em; text-transform: uppercase; font-weight: 700; }
.form-group input, .form-group select {
  width: 100%;
  border: 0;
  border-radius: 2px;
  background: #e0e3e6;
  padding: 12px 12px;
  font-size: 13px;
  transition: background-color .15s ease, outline-color .15s ease;
  outline: 1px solid rgba(195, 198, 211, 0.15);
  outline-offset: -1px;
}
.form-group input:focus, .form-group select:focus {
  outline: 1px solid rgba(0, 38, 96, 0.85);
  background: #ffffff;
}
.account-switch { background: #f2f4f7; padding: 8px 10px; border-radius: 2px; display: flex; align-items: center; justify-content: space-between; }
.account-switch strong { font-size: 13px; color: #0f172a; }
.account-switch p { margin: 3px 0 0; color: #64748b; font-size: 11px; }
.enabled-chip { background: #002660; color: #fff; border-radius: 99px; padding: 4px 10px; font-size: 10px; font-weight: 700; }

.role-grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 8px; margin-bottom: 10px; }
.role-card { border: 2px solid transparent; background: #f2f4f7; padding: 8px 10px; border-radius: 2px; display: flex; align-items: flex-start; gap: 8px; cursor: pointer; }
.role-card.active { border-color: #002660; background: #fff; }
.role-card input { margin-top: 2px; }
.role-card strong { font-size: 13px; color: #0f172a; }
.role-card p { margin: 4px 0 0; color: #64748b; font-size: 11px; }

.region-wrap { background: #f2f4f7; padding: 8px; border-radius: 2px; }
.region-grid { display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); gap: 8px; }
.region-grid select { background: #fff; }

.form-actions { margin-top: 2px; padding-top: 10px; border-top: 1px solid rgba(195, 198, 211, 0.3); display: flex; justify-content: flex-end; gap: 10px; }
.btn-secondary, .btn-primary { border: 0; border-radius: 2px; padding: 10px 14px; font-size: 12px; font-weight: 700; cursor: pointer; display: inline-flex; align-items: center; gap: 5px; }
.btn-secondary { background: #e6e8eb; color: #191c1e; }
.btn-primary { background: #002660; color: #fff; }
.btn-primary:disabled { opacity: .6; cursor: default; }

.side-col {
  grid-column: 11 / span 2;
  display: grid;
  gap: 10px;
  min-width: 0;
  width: 100%;
  justify-self: stretch;
}
.side-col > * { width: 100%; }
.notice-card { background: linear-gradient(135deg, #002660, #003a8c); color: #fff; border-radius: 2px; padding: 16px; }
.notice-card h3 { margin: 0 0 10px; font-size: 20px; }
.notice-card ul { margin: 0; padding-left: 16px; display: grid; gap: 8px; font-size: 12px; color: #d9e2ff; }
.log-card { background: #e6e8eb; border-radius: 2px; padding: 16px; }
.log-card h4 { margin: 0 0 10px; color: #475569; font-size: 10px; text-transform: uppercase; letter-spacing: .12em; }
.log-item { padding-left: 8px; border-left: 2px solid #002660; margin-bottom: 10px; }
.log-item strong { color: #0f172a; font-size: 12px; }
.log-item small { display: block; margin-top: 4px; color: #64748b; font-size: 10px; }
.log-item.muted { border-left-color: #94a3b8; margin-bottom: 0; }
.log-item.muted strong { color: #64748b; font-weight: 600; }

.sys-admin-status { position: fixed; right: 18px; bottom: 18px; color: #fff; border-radius: 8px; padding: 10px 12px; font-size: 12px; background: #0f172a; }
.sys-admin-status.error { background: #b91c1c; }
.sys-admin-status.success { background: #166534; }

@media (max-width: 1200px) {
  .sys-admin-create-layout { grid-template-columns: 1fr; }
  .main-col, .side-col { grid-column: auto; }
  .region-grid { grid-template-columns: 1fr 1fr; }
}
@media (max-width: 900px) {
  .grid2, .role-grid, .region-grid { grid-template-columns: 1fr; }
  .sys-admin-stepper { display: none; }
}
</style>
