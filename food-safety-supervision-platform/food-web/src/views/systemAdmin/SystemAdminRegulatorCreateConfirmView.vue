<template>
  <SystemAdminWorkspacePage
    active-key="create"
    :username="adminUser.username"
    search-placeholder="全局搜索人员或辖区..."
    @navigate="handleSidebarNavigate"
    @logout="handleLogout"
    @pending-feature="onPendingFeature"
  >
    <section class="sys-admin-create-confirm-page">
      <header class="sys-admin-confirm-head">
        <nav class="sys-admin-breadcrumb">
          <span>人员管理</span>
          <span class="material-symbols-outlined">chevron_right</span>
          <span>新建监管人员</span>
          <span class="material-symbols-outlined">chevron_right</span>
          <span class="is-current">核对信息</span>
        </nav>
        <h1>核对人员信息</h1>
        <p>请在最终确认提交前核对以下信息是否准确。</p>
      </header>

      <section class="sys-admin-stepper">
        <div class="step is-done"><span>1</span><strong>基础信息录入</strong></div>
        <div class="line is-active"></div>
        <div class="step is-active"><span>2</span><strong>核对信息</strong></div>
        <div class="line"></div>
        <div class="step"><span>3</span><strong>完成</strong></div>
      </section>

      <div class="sys-admin-confirm-grid">
        <section class="sys-admin-confirm-main">
          <article class="sys-admin-card">
            <h2>账号信息</h2>
            <div class="sys-admin-kv-grid">
              <div><label>用户名</label><p class="is-mono">{{ draft.username }}</p></div>
              <div><label>初始密码</label><p>已设置（已脱敏）</p></div>
            </div>
          </article>

          <article class="sys-admin-card">
            <h2>基础档案信息</h2>
            <div class="sys-admin-kv-grid">
              <div><label>姓名</label><p>{{ draft.name }}</p></div>
              <div><label>手机号</label><p>{{ formatPhone(draft.phone) }}</p></div>
            </div>
          </article>

          <article class="sys-admin-card">
            <h2>角色与辖区</h2>
            <div class="sys-admin-kv-grid">
              <div><label>角色</label><p>{{ formatRoleType(draft.roleType) }}</p></div>
              <div><label>辖区ID</label><p class="is-mono">{{ draft.regionId }}</p></div>
            </div>
            <div class="sys-admin-path">
              <label>辖区路径</label>
              <p>{{ regionText || "辖区信息待完善" }}</p>
            </div>
          </article>
        </section>

        <aside class="sys-admin-confirm-side">
          <article class="sys-admin-risk-card">
            <h3>提交提示</h3>
            <ul>
              <li>确认后账号将立即激活并可登录系统。</li>
              <li>系统将自动记录本次创建操作日志。</li>
              <li>如信息有误，请返回上一步修改。</li>
            </ul>
          </article>
        </aside>
      </div>

      <footer class="sys-admin-confirm-actions">
        <button type="button" class="btn-secondary" :disabled="loading" @click="goBack">返回修改</button>
        <button type="button" class="btn-primary" :disabled="loading" @click="handleCreateConfirm">
          <span class="material-symbols-outlined">task_alt</span>
          <span>{{ loading ? "提交中..." : "最终确认并创建" }}</span>
        </button>
      </footer>

      <div v-if="status.message" class="sys-admin-status" :class="status.type">{{ status.message }}</div>
    </section>
  </SystemAdminWorkspacePage>
</template>

<script setup>
import { onMounted, reactive, ref } from "vue";
import { useRouter } from "vue-router";
import { createRegulator } from "../../api/auth";
import { createRegulatorProfile, fetchRegionPath } from "../../api/regulation";
import SystemAdminWorkspacePage from "../../components/systemAdmin/SystemAdminWorkspacePage.vue";
import { useSystemAdminShellSession } from "./systemAdminShared";

const router = useRouter();
const { adminUser, token, handleSidebarNavigate, handleLogout } = useSystemAdminShellSession();

const loading = ref(false);
const status = reactive({ message: "", type: "" });
const regionText = ref("");
const draft = reactive({
  username: "",
  password: "",
  name: "",
  phone: "",
  roleType: "REGULATOR_ENFORCER",
  regionId: null
});

function setStatus(message, type = "info") {
  status.message = message;
  status.type = type;
}

function onPendingFeature() {}

function formatRoleType(roleType) {
  if (roleType === "REGULATOR_ADMIN") return "区域管理员";
  if (roleType === "REGULATOR_ENFORCER") return "执法人员";
  return "未知角色";
}

function formatPhone(phone) {
  const raw = String(phone || "").replace(/\D/g, "");
  if (raw.length !== 11) return raw || "—";
  return `${raw.slice(0, 3)}-${raw.slice(3, 7)}-${raw.slice(7)}`;
}

function goBack() {
  router.push({ name: "admin-regulator-create" });
}

function loadDraft() {
  const raw = sessionStorage.getItem("sysAdminCreateRegulatorDraft");
  if (!raw) {
    setStatus("未找到待确认的创建信息，请先填写基础信息", "error");
    return false;
  }
  try {
    const parsed = JSON.parse(raw);
    draft.username = parsed.username || "";
    draft.password = parsed.password || "";
    draft.name = parsed.name || "";
    draft.phone = parsed.phone || "";
    draft.roleType = parsed.roleType || "REGULATOR_ENFORCER";
    draft.regionId = Number(parsed.regionId || 0) || null;
    if (!draft.username || !draft.password || !draft.name || !draft.phone || !draft.regionId) {
      throw new Error("invalid draft");
    }
    return true;
  } catch {
    setStatus("待确认信息已损坏，请重新填写", "error");
    return false;
  }
}

async function loadRegionText() {
  if (!draft.regionId) return;
  try {
    const pathList = await fetchRegionPath(token.value, draft.regionId);
    if (!Array.isArray(pathList) || !pathList.length) {
      regionText.value = `辖区ID: ${draft.regionId}`;
      return;
    }
    regionText.value = pathList.map((item) => item.name).join(" / ");
  } catch {
    regionText.value = `辖区ID: ${draft.regionId}`;
  }
}

async function handleCreateConfirm() {
  loading.value = true;
  setStatus("");
  try {
    const user = await createRegulator(
      {
        username: draft.username,
        password: draft.password,
        fullName: draft.name,
        phone: draft.phone,
        userType: "REGULATOR",
        roleType: draft.roleType
      },
      token.value
    );

    await createRegulatorProfile(token.value, {
      userId: user.id,
      name: draft.name,
      phone: draft.phone,
      roleType: draft.roleType,
      regionIds: [draft.regionId]
    });

    sessionStorage.removeItem("sysAdminCreateRegulatorDraft");
    setStatus("监管人员创建成功，档案已同步。", "success");
    setTimeout(() => {
      router.push({ name: "admin-regulator-list" });
    }, 450);
  } catch (error) {
    setStatus(error.message || "创建失败", "error");
  } finally {
    loading.value = false;
  }
}

onMounted(async () => {
  if (!loadDraft()) return;
  await loadRegionText();
});
</script>

<style scoped>
.sys-admin-create-confirm-page { display: grid; gap: 14px; }
.sys-admin-confirm-head h1 { margin: 6px 0 0; color: #002660; font-size: 30px; font-weight: 900; }
.sys-admin-confirm-head p { margin: 6px 0 0; color: #64748b; font-size: 13px; }
.sys-admin-breadcrumb { display: flex; align-items: center; gap: 4px; color: #64748b; font-size: 10px; letter-spacing: .1em; text-transform: uppercase; }
.sys-admin-breadcrumb .material-symbols-outlined { font-size: 14px; }
.sys-admin-breadcrumb .is-current { color: #002660; font-weight: 700; }

.sys-admin-stepper { display: flex; align-items: center; width: 100%; }
.step { display: flex; align-items: center; gap: 8px; color: #64748b; }
.step span { width: 30px; height: 30px; border-radius: 4px; background: #e0e3e6; display: grid; place-items: center; font-weight: 700; }
.step strong { font-size: 13px; font-weight: 600; }
.step.is-active { color: #002660; }
.step.is-active span { background: #002660; color: #fff; }
.step.is-done span { background: #dcfce7; color: #166534; }
.line { flex: 1; height: 2px; background: #e0e3e6; margin: 0 10px; }
.line.is-active { background: #003a8c; }

.sys-admin-confirm-grid { display: grid; grid-template-columns: minmax(0, 8fr) minmax(0, 4fr); gap: 12px; }
.sys-admin-confirm-main { display: grid; gap: 10px; }
.sys-admin-confirm-side { display: grid; gap: 10px; align-content: start; }
.sys-admin-card { background: #fff; border: 1px solid rgba(226, 232, 240, 0.6); border-radius: 2px; box-shadow: 0 1px 2px rgba(15, 23, 42, 0.05); padding: 14px; }
.sys-admin-card h2 { margin: 0 0 10px; color: #002660; font-size: 12px; letter-spacing: .12em; text-transform: uppercase; font-weight: 900; }
.sys-admin-kv-grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 10px 12px; }
.sys-admin-kv-grid label { display: block; margin-bottom: 5px; color: #64748b; font-size: 10px; letter-spacing: .08em; text-transform: uppercase; font-weight: 700; }
.sys-admin-kv-grid p { margin: 0; background: #f2f4f7; border-radius: 2px; padding: 10px; color: #0f172a; font-size: 13px; font-weight: 700; }
.sys-admin-kv-grid .is-mono { font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, "Liberation Mono", "Courier New", monospace; }
.sys-admin-path { margin-top: 10px; padding: 10px; background: #f8fafc; border: 1px solid #e2e8f0; border-radius: 2px; }
.sys-admin-path label { display: block; color: #64748b; font-size: 10px; letter-spacing: .08em; text-transform: uppercase; font-weight: 700; }
.sys-admin-path p { margin: 6px 0 0; color: #0f172a; font-size: 12px; font-weight: 700; }

.sys-admin-risk-card { background: #fffbeb; border: 1px solid #f59e0b33; border-left: 4px solid #b45309; border-radius: 2px; padding: 14px; }
.sys-admin-risk-card h3 { margin: 0 0 8px; color: #92400e; font-size: 13px; font-weight: 900; }
.sys-admin-risk-card ul { margin: 0; padding-left: 16px; display: grid; gap: 6px; color: #475569; font-size: 12px; }

.sys-admin-confirm-actions { display: flex; justify-content: flex-end; gap: 10px; }
.btn-secondary, .btn-primary { border: 0; border-radius: 2px; padding: 10px 14px; font-size: 12px; font-weight: 700; cursor: pointer; display: inline-flex; align-items: center; gap: 5px; }
.btn-secondary { background: #e6e8eb; color: #191c1e; }
.btn-primary { background: #002660; color: #fff; }
.btn-primary:disabled, .btn-secondary:disabled { opacity: .6; cursor: default; }

.sys-admin-status { position: fixed; right: 18px; bottom: 18px; color: #fff; border-radius: 8px; padding: 10px 12px; font-size: 12px; background: #0f172a; }
.sys-admin-status.error { background: #b91c1c; }
.sys-admin-status.success { background: #166534; }

@media (max-width: 1200px) {
  .sys-admin-confirm-grid { grid-template-columns: 1fr; }
}
@media (max-width: 900px) {
  .sys-admin-kv-grid { grid-template-columns: 1fr; }
  .sys-admin-stepper { display: none; }
}
</style>

