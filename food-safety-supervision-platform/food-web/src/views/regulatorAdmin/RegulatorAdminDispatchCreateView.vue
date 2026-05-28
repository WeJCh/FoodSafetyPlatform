<template>
  <RegulatorAdminWorkspacePage
    active-key="dispatch"
    :username="regulatorUser.username"
    @navigate="handleSidebarNavigate"
    @logout="handleLogout"
  >
    <section class="create-page">
      <header class="create-page__head">
        <div>
          <nav class="crumbs">
            <span>检查任务</span>
            <span>/</span>
            <span class="is-current">发起新任务</span>
          </nav>
          <h1>发起检查任务</h1>
          <p>创建新的食品安全检查任务，并可在提交后直接完成执法人员分配。</p>
        </div>
        <div class="head-actions">
          <button class="ghost" type="button" @click="goBackToList">取消</button>
          <button class="primary" type="button" :disabled="loading" @click="handleSubmitTask">
            {{ loading ? "提交中..." : "提交并分派" }}
          </button>
        </div>
      </header>

      <div class="grid-layout">
        <div class="main-col">
          <section class="panel">
            <div class="panel-title">
              <i></i>
              <h2>任务基础信息</h2>
            </div>
            <div class="form-grid">
              <label class="span-all">
                任务名称
                <input v-model.trim="form.taskTitle" placeholder="例如：2026Q2 餐饮服务食品安全常规检查" />
              </label>
              <label>
                优先级
                <select v-model="form.priority">
                  <option value="LOW">低</option>
                  <option value="MEDIUM">中</option>
                  <option value="HIGH">高</option>
                </select>
              </label>
              <label>
                截止日期
                <input v-model="form.deadlineDate" type="date" />
              </label>
            </div>
          </section>

          <section class="panel">
            <div class="panel-title panel-title--between">
              <div class="panel-title__left">
                <i></i>
                <h2>目标企业选择</h2>
              </div>
            </div>
            <label class="search-input">
              <input
                v-model.trim="enterpriseKeyword"
                placeholder="输入企业名称、社会信用代码或法人姓名"
              />
            </label>
            <div class="enterprise-list">
              <button
                v-for="item in filteredEnterprises"
                :key="item.id"
                class="enterprise-item"
                :class="{ active: String(form.enterpriseId) === String(item.id) }"
                type="button"
                @click="form.enterpriseId = item.id"
              >
                <div>
                  <p class="enterprise-name">{{ item.enterpriseName || "-" }}</p>
                  <p class="enterprise-meta">
                    信用代码: {{ item.creditCode || "-" }} | 风险等级: {{ formatRisk(item.riskLevel) }}
                  </p>
                </div>
                <span class="check-dot">{{ String(form.enterpriseId) === String(item.id) ? "✓" : "" }}</span>
              </button>
              <div v-if="!filteredEnterprises.length" class="empty">暂无可选企业</div>
            </div>
          </section>
        </div>

        <aside class="side-col">
          <section class="panel">
            <div class="panel-title">
              <i></i>
              <h2>执法人员分配</h2>
            </div>
            <p class="side-tips">请先选择目标企业，仅展示该企业辖区内的执法人员；提交后将立即分配。</p>
            <div class="enforcer-list">
              <button
                v-for="item in enforcers"
                :key="item.id"
                class="enforcer-item"
                :class="{ active: String(form.regulatorId) === String(item.id) }"
                type="button"
                @click="form.regulatorId = item.id"
              >
                <div>
                  <p>{{ item.name || "未命名人员" }}</p>
                  <span>{{ formatRegulatorRegions(item.regionIds) }}</span>
                </div>
                <b>{{ String(form.regulatorId) === String(item.id) ? "已选" : "选择" }}</b>
              </button>
              <div v-if="!form.enterpriseId" class="empty">请先选择目标企业</div>
              <div v-else-if="!enforcers.length" class="empty">该辖区暂无可分配执法人员</div>
            </div>
            <label class="remark-box">
              任务备注
              <textarea v-model.trim="form.taskDesc" rows="4" placeholder="请输入具体的检查指令和注意事项" />
            </label>
          </section>
        </aside>

        <section class="panel span-all">
          <div class="panel-title">
            <i></i>
            <h2>执行链路</h2>
          </div>
          <div class="chain-wrap">
            <article class="chain-step is-current">
              <span class="dot"></span>
              <h3>任务创建</h3>
              <p>当前步骤</p>
            </article>
            <span class="chain-line"></span>
            <article class="chain-step">
              <span class="dot"></span>
              <h3>指派确认</h3>
              <p>等待中</p>
            </article>
            <span class="chain-line"></span>
            <article class="chain-step">
              <span class="dot"></span>
              <h3>现场检查</h3>
              <p>等待中</p>
            </article>
            <span class="chain-line"></span>
            <article class="chain-step">
              <span class="dot"></span>
              <h3>报告生成</h3>
              <p>等待中</p>
            </article>
          </div>
        </section>
      </div>

      <div v-if="status.message" class="status" :class="status.type">{{ status.message }}</div>
    </section>
  </RegulatorAdminWorkspacePage>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from "vue";
import { useRouter } from "vue-router";
import { assignInspectionTask, createInspectionTask } from "../../api/regulationOperation";
import { fetchEligibleRegulators, fetchEnterprises, fetchRegionPath } from "../../api/regulation";
import RegulatorAdminWorkspacePage from "../../components/regulatorAdmin/RegulatorAdminWorkspacePage.vue";
import { resolveErrorMessage } from "../../utils/uiFeedback";
import { useRegulatorAdminShellSession } from "./regulatorAdminShared";

const router = useRouter();
const { regulatorUser, token, handleSidebarNavigate, handleLogout } = useRegulatorAdminShellSession();
const status = reactive({ message: "", type: "" });
const loading = ref(false);
const enterpriseKeyword = ref("");
const enterprises = ref([]);
const enforcers = ref([]);
const regionNameMap = reactive({});
const form = reactive({
  taskTitle: "",
  priority: "MEDIUM",
  enterpriseId: "",
  regulatorId: "",
  deadlineDate: "",
  taskDesc: ""
});

const filteredEnterprises = computed(() => {
  const key = enterpriseKeyword.value.trim().toLowerCase();
  if (!key) return enterprises.value;
  return enterprises.value.filter((item) => {
    const text = [item.enterpriseName, item.creditCode, item.legalRepresentative].filter(Boolean).join(" ").toLowerCase();
    return text.includes(key);
  });
});

function setStatus(message, type = "info") {
  status.message = message;
  status.type = type;
}

function formatRisk(value) {
  if (value === "HIGH") return "高";
  if (value === "LOW") return "低";
  if (value === "MEDIUM") return "中";
  return value || "-";
}

function formatRegionName(regionId) {
  if (!regionId) return "-";
  return regionNameMap[regionId] || `区域 ${regionId}`;
}

function formatRegulatorRegions(regionIds = []) {
  if (!Array.isArray(regionIds) || !regionIds.length) return "-";
  return regionIds.map((id) => formatRegionName(id)).filter(Boolean).join("、");
}

async function ensureRegionName(regionId) {
  if (!regionId || regionNameMap[regionId]) return;
  try {
    const path = await fetchRegionPath(token.value, regionId);
    regionNameMap[regionId] = Array.isArray(path) && path.length
      ? path.map((item) => item.name).join(" / ")
      : `区域 ${regionId}`;
  } catch {
    regionNameMap[regionId] = `区域 ${regionId}`;
  }
}

function findEnterpriseById(enterpriseId) {
  return enterprises.value.find((item) => String(item.id) === String(enterpriseId)) || null;
}

async function loadEnforcers(regionId) {
  if (!regionId) {
    enforcers.value = [];
    return;
  }
  try {
    const data = await fetchEligibleRegulators(token.value, regionId);
    const list = Array.isArray(data) ? data : [];
    enforcers.value = list;
    await Promise.all(
      list.flatMap((item) =>
        Array.isArray(item.regionIds) ? item.regionIds.map((region) => ensureRegionName(region)) : []
      )
    );
  } catch (error) {
    enforcers.value = [];
    setStatus(resolveErrorMessage(error, "执法人员列表加载失败"), "error");
  }
}

function normalizeDeadline(dateValue) {
  if (!dateValue) return "";
  const normalized = String(dateValue).trim();
  if (!normalized) return "";

  if (/^\d{4}-\d{2}-\d{2}$/.test(normalized)) {
    return `${normalized}T18:00:00`;
  }

  if (/^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}$/.test(normalized)) {
    return `${normalized}:00`;
  }

  if (/^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}$/.test(normalized)) {
    return normalized;
  }

  const date = new Date(normalized);
  if (Number.isNaN(date.getTime())) return "";
  const pad = (n) => String(n).padStart(2, "0");
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}T${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`;
}

async function loadEnterprises() {
  loading.value = true;
  try {
    const enterpriseData = await fetchEnterprises(token.value, { approvalStatus: "APPROVED", page: 1, size: 100 });
    enterprises.value = enterpriseData?.records || [];
  } catch (error) {
    setStatus(resolveErrorMessage(error, "加载企业列表失败"), "error");
  } finally {
    loading.value = false;
  }
}

watch(
  () => form.enterpriseId,
  async (enterpriseId) => {
    form.regulatorId = "";
    await loadEnforcers(findEnterpriseById(enterpriseId)?.regionId);
  }
);

function goBackToList() {
  router.push({ name: "regulator-admin-dispatch" });
}

async function handleSubmitTask() {
  if (!form.taskTitle.trim()) return setStatus("请填写任务名称", "error");
  if (!form.enterpriseId) return setStatus("请选择目标企业", "error");
  if (!form.deadlineDate) return setStatus("请填写截止日期", "error");

  loading.value = true;
  setStatus("");
  try {
    const payload = {
      enterpriseId: form.enterpriseId,
      taskTitle: form.taskTitle,
      taskDesc: form.taskDesc,
      priority: form.priority,
      deadline: normalizeDeadline(form.deadlineDate)
    };
    const createdTask = await createInspectionTask(token.value, payload);

    if (form.regulatorId && createdTask?.id) {
      try {
        await assignInspectionTask(token.value, createdTask.id, { regulatorId: form.regulatorId });
        setStatus("任务已创建，并完成执法人员分配", "success");
      } catch (assignError) {
        setStatus(resolveErrorMessage(assignError, "任务已创建，但执法人员分配失败，请到列表页重试"), "error");
      }
    } else {
      setStatus("任务已创建", "success");
    }

    setTimeout(() => {
      router.push({ name: "regulator-admin-dispatch" });
    }, 500);
  } catch (error) {
    setStatus(resolveErrorMessage(error, "创建任务失败"), "error");
  } finally {
    loading.value = false;
  }
}

onMounted(loadEnterprises);
</script>

<style scoped>
.create-page { display: grid; gap: 16px; }
.create-page__head { display: flex; justify-content: space-between; align-items: flex-end; gap: 12px; }
.crumbs { display: flex; align-items: center; gap: 6px; font-size: 12px; color: #64748b; }
.crumbs .is-current { color: #002660; font-weight: 700; }
h1 { margin: 6px 0 0; color: #002660; font-size: 30px; font-weight: 800; }
p { margin: 6px 0 0; color: #64748b; }
.head-actions { display: flex; gap: 8px; }
.primary, .ghost { border-radius: 6px; padding: 10px 14px; font-size: 12px; font-weight: 700; cursor: pointer; }
.primary { border: 0; background: #002660; color: #fff; box-shadow: 0 12px 24px rgba(0, 38, 96, 0.2); }
.ghost { border: 1px solid #d1d5db; background: #fff; color: #334155; }

.grid-layout { display: grid; grid-template-columns: 2fr 1fr; gap: 16px; align-items: start; }
.main-col, .side-col { display: grid; gap: 16px; }
.span-all { grid-column: 1 / -1; }
.panel { background: #fff; border: 1px solid #e2e8f0; border-radius: 8px; padding: 14px; }
.panel-title { display: flex; align-items: center; gap: 8px; margin-bottom: 10px; }
.panel-title--between { justify-content: space-between; }
.panel-title__left { display: flex; align-items: center; gap: 8px; }
.panel-title i { width: 4px; height: 18px; border-radius: 999px; background: #002660; display: block; }
.panel-title h2 { margin: 0; font-size: 16px; color: #002660; font-weight: 800; }

.form-grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 10px; }
label { display: grid; gap: 6px; font-size: 12px; color: #64748b; font-weight: 700; }
input, select, textarea { border: 0; background: #f1f5f9; border-radius: 6px; padding: 10px; color: #1e293b; }
textarea { resize: vertical; }

.search-input { margin-bottom: 10px; }
.enterprise-list { display: grid; gap: 8px; max-height: 320px; overflow: auto; padding-right: 2px; }
.enterprise-item { border: 1px solid #e2e8f0; background: #f8fafc; border-radius: 8px; padding: 12px; display: flex; justify-content: space-between; align-items: center; text-align: left; cursor: pointer; }
.enterprise-item.active { border-color: #002660; background: #eff6ff; }
.enterprise-name { margin: 0; color: #0f172a; font-size: 14px; font-weight: 700; }
.enterprise-meta { margin: 4px 0 0; color: #64748b; font-size: 11px; }
.check-dot { width: 20px; height: 20px; border-radius: 999px; display: grid; place-items: center; border: 1px solid #cbd5e1; color: #fff; font-size: 12px; background: #fff; }
.enterprise-item.active .check-dot { background: #002660; border-color: #002660; }

.side-tips { margin: 0 0 8px; font-size: 11px; color: #64748b; text-transform: uppercase; letter-spacing: 0.06em; }
.enforcer-list { display: grid; gap: 8px; }
.enforcer-item { border: 1px solid #e2e8f0; border-radius: 8px; background: #fff; padding: 10px; display: flex; justify-content: space-between; align-items: center; cursor: pointer; text-align: left; }
.enforcer-item.active { border-color: #002660; background: #eff6ff; }
.enforcer-item p { margin: 0; font-size: 13px; font-weight: 700; color: #0f172a; }
.enforcer-item span { font-size: 11px; color: #64748b; }
.enforcer-item b { font-size: 11px; color: #334155; }
.enforcer-item.active b { color: #002660; }
.remark-box { margin-top: 10px; }

.chain-wrap { display: flex; align-items: flex-start; justify-content: center; gap: 0; padding: 8px 0 2px; overflow-x: auto; }
.chain-step { min-width: 120px; display: grid; justify-items: center; gap: 6px; text-align: center; }
.chain-step .dot { width: 10px; height: 10px; border-radius: 999px; background: #cbd5e1; }
.chain-step h3 { margin: 0; font-size: 12px; font-weight: 700; color: #64748b; }
.chain-step p { margin: 0; font-size: 10px; color: #94a3b8; }
.chain-step.is-current .dot { width: 12px; height: 12px; background: #002660; box-shadow: 0 0 0 4px rgba(0, 38, 96, 0.16); }
.chain-step.is-current h3 { color: #0f172a; }
.chain-step.is-current p { color: #002660; font-weight: 700; }
.chain-line { width: 72px; height: 2px; margin-top: 5px; background: #dbe2ea; flex-shrink: 0; }

.empty { font-size: 12px; color: #64748b; padding: 10px 0; }
.status { position: fixed; right: 18px; bottom: 18px; border-radius: 8px; padding: 10px 12px; color: #fff; background: #0f172a; font-size: 13px; }
.status.error { background: #b91c1c; }
.status.success { background: #166534; }

@media (max-width: 1100px) {
  .grid-layout { grid-template-columns: 1fr; }
  .chain-wrap { justify-content: flex-start; }
}
@media (max-width: 900px) {
  .create-page__head { flex-direction: column; align-items: flex-start; }
  .head-actions { width: 100%; }
  .head-actions button { flex: 1; }
  .form-grid { grid-template-columns: 1fr; }
}
</style>
