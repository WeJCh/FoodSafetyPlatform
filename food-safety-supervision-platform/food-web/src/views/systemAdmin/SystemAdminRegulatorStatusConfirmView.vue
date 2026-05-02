<template>
  <SystemAdminWorkspacePage
    active-key="list"
    :username="adminUser.username"
    search-placeholder="全局搜索人员、证件或案件..."
    @navigate="handleSidebarNavigate"
    @logout="handleLogout"
  >
    <section class="sys-admin-status-confirm-page">
      <div class="sys-admin-status-confirm-card">
        <header class="sys-admin-status-confirm-card__head">
          <div class="sys-admin-title">
            <span class="material-symbols-outlined">manage_accounts</span>
            <h1>确认切换账号状态</h1>
          </div>
          <p>你正在修改监管人员的系统访问状态，该操作会写入操作日志，请确认后继续。</p>
        </header>

        <div class="sys-admin-status-confirm-card__body">
          <section>
            <h2>人员摘要</h2>
            <div class="sys-admin-summary-grid">
              <article>
                <label>姓名</label>
                <p>{{ profile?.name || "-" }}</p>
              </article>
              <article>
                <label>账号标识</label>
                <p>{{ profile?.userId ? `REGULATOR-${profile.userId}` : "-" }}</p>
              </article>
              <article>
                <label>当前角色</label>
                <p>{{ formatRoleType(profile?.roleType) }}</p>
              </article>
            </div>
          </section>

          <section>
            <h2>状态变更</h2>
            <div class="sys-admin-compare">
              <div class="sys-admin-compare-col">
                <span class="sys-admin-compare-label">当前状态</span>
                <div class="sys-admin-state-chip is-current">
                  <span class="material-symbols-outlined">
                    {{ currentStatus === 1 ? "check_circle" : "cancel" }}
                  </span>
                  <strong>{{ currentStatus === 1 ? "启用" : "停用" }}</strong>
                </div>
              </div>
              <div class="sys-admin-compare-arrow">
                <span class="material-symbols-outlined">trending_flat</span>
              </div>
              <div class="sys-admin-compare-col">
                <span class="sys-admin-compare-label is-target">目标状态</span>
                <div class="sys-admin-state-chip" :class="targetStatus === 1 ? 'is-enable' : 'is-disable'">
                  <span class="material-symbols-outlined">
                    {{ targetStatus === 1 ? "check_circle" : "cancel" }}
                  </span>
                  <strong>{{ targetStatus === 1 ? "启用" : "停用" }}</strong>
                </div>
              </div>
            </div>
          </section>

          <section class="sys-admin-risk-box">
            <div class="sys-admin-risk-title">
              <span class="material-symbols-outlined">warning</span>
              <h3>操作影响提示</h3>
            </div>
            <ul>
              <li>{{ targetStatus === 0 ? "停用后该人员将无法继续登录系统。" : "启用后该人员将恢复系统登录权限。" }}</li>
              <li>本次操作会写入审计日志，并保留变更痕迹。</li>
              <li>建议在业务低峰期执行，避免影响正在进行中的任务处理。</li>
            </ul>
          </section>
        </div>

        <footer class="sys-admin-status-confirm-card__foot">
          <button type="button" class="btn-cancel" :disabled="loading" @click="goBack">取消</button>
          <button
            type="button"
            class="btn-confirm"
            :class="targetStatus === 1 ? 'is-enable' : 'is-disable'"
            :disabled="loading || !profile"
            @click="handleConfirm"
          >
            <span class="material-symbols-outlined">{{ targetStatus === 1 ? "check_circle" : "block" }}</span>
            <span>{{ loading ? "提交中..." : (targetStatus === 1 ? "确认启用账号" : "确认停用账号") }}</span>
          </button>
        </footer>
      </div>

      <div v-if="status.message" class="sys-admin-status" :class="status.type">{{ status.message }}</div>
    </section>
  </SystemAdminWorkspacePage>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from "vue";
import { useRoute, useRouter } from "vue-router";
import { fetchRegulatorProfileByUserId, updateRegulatorStatus } from "../../api/regulation";
import SystemAdminWorkspacePage from "../../components/systemAdmin/SystemAdminWorkspacePage.vue";
import { resolveErrorMessage } from "../../utils/uiFeedback";
import { useSystemAdminShellSession } from "./systemAdminShared";

const route = useRoute();
const router = useRouter();
const { adminUser, token, handleSidebarNavigate, handleLogout } = useSystemAdminShellSession();

const loading = ref(false);
const profile = ref(null);
const status = reactive({ message: "", type: "" });

const userId = computed(() => Number(route.params.userId || 0) || 0);
const currentStatus = computed(() => (Number(profile.value?.status) === 1 ? 1 : 0));
const targetStatus = computed(() => {
  const q = Number(route.query.targetStatus);
  if (q === 0 || q === 1) return q;
  return currentStatus.value === 1 ? 0 : 1;
});
const fromPage = computed(() => String(route.query.from || "detail"));

function setStatus(message, type = "info") {
  status.message = message;
  status.type = type;
}

function formatRoleType(roleType) {
  if (roleType === "REGULATOR_ADMIN") return "监管管理员";
  if (roleType === "REGULATOR_ENFORCER") return "监管执法人员";
  return roleType ? String(roleType) : "未知角色";
}

function goBack() {
  if (fromPage.value === "list") {
    router.push({ name: "admin-regulator-list" });
    return;
  }
  router.push({
    name: "admin-regulator-detail",
    params: { userId: userId.value },
    query: { refreshedAt: String(Date.now()) }
  });
}

function goAfterSubmit() {
  if (fromPage.value === "list") {
    router.replace({ name: "admin-regulator-list" });
    return;
  }
  router.replace({
    name: "admin-regulator-detail",
    params: { userId: userId.value },
    query: { refreshedAt: String(Date.now()) }
  });
}

async function loadDetail() {
  if (!userId.value) {
    setStatus("缺少监管人员 userId 参数", "error");
    return;
  }
  loading.value = true;
  setStatus("");
  try {
    profile.value = await fetchRegulatorProfileByUserId(token.value, userId.value);
  } catch (error) {
    setStatus(resolveErrorMessage(error, "加载监管人员信息失败"), "error");
  } finally {
    loading.value = false;
  }
}

async function handleConfirm() {
  if (!profile.value?.id) return;
  if (currentStatus.value === targetStatus.value) {
    setStatus("目标状态与当前状态一致，无需重复提交", "warning");
    return;
  }

  loading.value = true;
  setStatus("");
  try {
    await updateRegulatorStatus(token.value, profile.value.id, targetStatus.value);
    setStatus(targetStatus.value === 1 ? "账号已启用" : "账号已停用", targetStatus.value === 1 ? "success" : "warning");
    setTimeout(() => {
      goAfterSubmit();
    }, 450);
  } catch (error) {
    setStatus(resolveErrorMessage(error, "状态切换失败"), "error");
  } finally {
    loading.value = false;
  }
}

onMounted(() => {
  loadDetail();
});
</script>

<style scoped>
.sys-admin-status-confirm-page { display: grid; gap: 14px; justify-items: center; padding: 8px 0 20px; }
.sys-admin-status-confirm-card { width: min(100%, 920px); background: #fff; border: 1px solid rgba(195, 198, 211, 0.25); border-radius: 2px; box-shadow: 0 1px 2px rgba(15, 23, 42, 0.08); overflow: hidden; }
.sys-admin-status-confirm-card__head { padding: 18px 18px; border-bottom: 1px solid #e6e8eb; }
.sys-admin-title { display: flex; align-items: center; gap: 8px; margin-bottom: 6px; }
.sys-admin-title .material-symbols-outlined { color: #002660; font-size: 24px; }
.sys-admin-title h1 { margin: 0; font-size: 24px; color: #002660; font-weight: 900; }
.sys-admin-status-confirm-card__head p { margin: 0; color: #64748b; font-size: 13px; }
.sys-admin-status-confirm-card__body { padding: 18px; display: grid; gap: 16px; }
.sys-admin-status-confirm-card__body h2 { margin: 0 0 8px; font-size: 11px; text-transform: uppercase; letter-spacing: 0.14em; color: #64748b; }
.sys-admin-summary-grid { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 10px; }
.sys-admin-summary-grid article { background: #f2f4f7; border-radius: 2px; padding: 10px; }
.sys-admin-summary-grid label { display: block; font-size: 10px; color: #64748b; text-transform: uppercase; font-weight: 900; letter-spacing: 0.08em; margin-bottom: 5px; }
.sys-admin-summary-grid p { margin: 0; color: #191c1e; font-size: 13px; font-weight: 800; }

.sys-admin-compare { display: flex; align-items: center; justify-content: space-between; gap: 8px; background: rgba(224, 227, 230, 0.35); border-radius: 2px; padding: 14px 10px; }
.sys-admin-compare-col { display: grid; justify-items: center; gap: 6px; flex: 1; }
.sys-admin-compare-label { font-size: 10px; color: #64748b; font-weight: 900; }
.sys-admin-compare-label.is-target { color: #93000a; }
.sys-admin-compare-arrow .material-symbols-outlined { font-size: 28px; color: #94a3b8; }
.sys-admin-state-chip { display: inline-flex; align-items: center; gap: 6px; border-radius: 2px; padding: 6px 10px; border: 1px solid transparent; }
.sys-admin-state-chip strong { font-size: 12px; font-weight: 900; }
.sys-admin-state-chip.is-current { background: rgba(18, 67, 148, 0.08); border-color: rgba(18, 67, 148, 0.2); color: #124394; }
.sys-admin-state-chip.is-enable { background: #dcfce7; border-color: rgba(22, 101, 52, 0.22); color: #166534; }
.sys-admin-state-chip.is-disable { background: #ffdad6; border-color: rgba(147, 0, 10, 0.24); color: #93000a; }

.sys-admin-risk-box { border-left: 4px solid #ba1a1a; background: rgba(255, 218, 214, 0.35); padding: 12px; }
.sys-admin-risk-title { display: flex; gap: 8px; align-items: center; margin-bottom: 6px; }
.sys-admin-risk-title .material-symbols-outlined { color: #ba1a1a; }
.sys-admin-risk-title h3 { margin: 0; font-size: 13px; color: #93000a; font-weight: 900; }
.sys-admin-risk-box ul { margin: 0; padding-left: 18px; display: grid; gap: 4px; color: #475569; font-size: 12px; }

.sys-admin-status-confirm-card__foot { padding: 14px 18px; background: #f2f4f7; display: flex; justify-content: flex-end; gap: 10px; }
.btn-cancel, .btn-confirm { border: 0; border-radius: 2px; padding: 10px 14px; font-size: 12px; font-weight: 900; display: inline-flex; align-items: center; gap: 6px; cursor: pointer; }
.btn-cancel { background: transparent; color: #0f172a; }
.btn-confirm.is-disable { background: #ba1a1a; color: #fff; }
.btn-confirm.is-enable { background: #166534; color: #fff; }
.btn-cancel:disabled, .btn-confirm:disabled { opacity: 0.6; cursor: default; }

.sys-admin-status { position: fixed; right: 18px; bottom: 18px; color: #fff; border-radius: 8px; padding: 10px 12px; font-size: 12px; background: #0f172a; z-index: 70; }
.sys-admin-status.error { background: #b91c1c; }
.sys-admin-status.success { background: #166534; }
.sys-admin-status.warning { background: #b45309; }

@media (max-width: 900px) {
  .sys-admin-summary-grid { grid-template-columns: 1fr; }
  .sys-admin-compare { flex-direction: column; align-items: stretch; }
  .sys-admin-compare-arrow { display: grid; place-items: center; transform: rotate(90deg); }
}
</style>
