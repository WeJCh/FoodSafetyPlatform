<template>
  <RegulatorAdminWorkspacePage
    active-key="sampling"
    :username="regulatorUser.username"
    @navigate="handleSidebarNavigate"
    @logout="handleLogout"
  >
    <section class="create-page">
      <header class="create-page__head">
        <div>
          <nav class="crumbs">
            <span>抽检任务</span>
            <span>/</span>
            <span class="is-current">新建抽检任务</span>
          </nav>
          <h1>新建抽检任务</h1>
          <p>选择企业与抽检产品，填写任务要求与截止时间。</p>
        </div>
        <div class="head-actions">
          <button class="ghost" type="button" @click="goBackToList">取消</button>
          <button class="primary" type="button" :disabled="submitting" @click="handleSubmit">
            {{ submitting ? "创建中..." : "创建抽检任务" }}
          </button>
        </div>
      </header>

      <div class="grid-layout">
        <div class="main-col">
          <section class="panel">
            <div class="panel-title">
              <i></i>
              <h2>企业与产品</h2>
            </div>
            <div class="form-grid form-grid--2">
              <label>
                选择企业
                <select v-model="form.enterpriseId" :disabled="pageLoading" @change="handleEnterpriseChange">
                  <option value="">请选择企业</option>
                  <option v-for="item in enterprises" :key="item.id" :value="item.id">
                    {{ item.enterpriseName }}
                  </option>
                </select>
              </label>
              <label>
                选择产品
                <select v-model="form.productId" :disabled="pageLoading || submitting || productLoading || !form.enterpriseId">
                  <option value="">请选择产品</option>
                  <option v-for="item in products" :key="item.id" :value="item.id">
                    {{ item.productName }}
                  </option>
                </select>
              </label>
            </div>
            <p v-if="form.enterpriseId && !productLoading && !products.length" class="hint-warn">
              当前企业暂无可抽检的启用产品，请先补齐产品档案。
            </p>
          </section>

          <section class="panel">
            <div class="panel-title">
              <i></i>
              <h2>任务基础信息</h2>
            </div>
            <div class="form-grid">
              <label class="span-all">
                任务标题
                <input v-model.trim="form.taskTitle" :disabled="submitting" placeholder="例如：乳制品例行抽检" />
              </label>
              <label class="span-all">
                任务描述
                <textarea v-model.trim="form.taskDesc" :disabled="submitting" rows="3" placeholder="填写抽检要求说明"></textarea>
              </label>
              <label>
                优先级
                <select v-model="form.priority" :disabled="submitting">
                  <option value="MEDIUM">中</option>
                  <option value="LOW">低</option>
                  <option value="HIGH">高</option>
                </select>
              </label>
              <label>
                截止时间
                <input v-model="form.deadline" :disabled="submitting" type="datetime-local" required />
              </label>
            </div>
          </section>
        </div>
      </div>

      <div v-if="status.message" class="status" :class="status.type">{{ status.message }}</div>
    </section>
  </RegulatorAdminWorkspacePage>
</template>

<script setup>
import { onMounted, reactive, ref } from "vue";
import { useRouter } from "vue-router";
import { createSamplingTask } from "../../api/regulationOperation";
import { fetchEnterprises, fetchEnterpriseProducts } from "../../api/regulation";
import RegulatorAdminWorkspacePage from "../../components/regulatorAdmin/RegulatorAdminWorkspacePage.vue";
import { resolveErrorMessage } from "../../utils/uiFeedback";
import { useRegulatorAdminShellSession } from "./regulatorAdminShared";

const router = useRouter();
const { regulatorUser, token, handleSidebarNavigate, handleLogout } = useRegulatorAdminShellSession();
const status = reactive({ message: "", type: "" });
const pageLoading = ref(false);
const submitting = ref(false);
const productLoading = ref(false);
const enterprises = ref([]);
const products = ref([]);

const form = reactive({
  enterpriseId: "",
  productId: "",
  taskTitle: "",
  taskDesc: "",
  priority: "MEDIUM",
  deadline: ""
});

function setStatus(message, type = "info") {
  status.message = message;
  status.type = type;
}

function normalizeSamplingDeadline(value) {
  if (!value) return null;
  const v = String(value).trim();
  if (!v) return null;
  if (v.includes(":") && v.length === 16) return `${v}:00`;
  if (/^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}$/.test(v)) return v;
  const date = new Date(v);
  if (Number.isNaN(date.getTime())) return null;
  const pad = (n) => String(n).padStart(2, "0");
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}T${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`;
}

async function loadEnterprises() {
  pageLoading.value = true;
  try {
    const data = await fetchEnterprises(token.value, { approvalStatus: "APPROVED", page: 1, size: 100 });
    enterprises.value = data.records || [];
  } catch (error) {
    setStatus(resolveErrorMessage(error, "加载企业列表失败"), "error");
  } finally {
    pageLoading.value = false;
  }
}

async function loadProducts(enterpriseId) {
  if (!enterpriseId) {
    products.value = [];
    return;
  }
  productLoading.value = true;
  try {
    const data = await fetchEnterpriseProducts(token.value, enterpriseId);
    products.value = Array.isArray(data) ? data.filter((item) => item?.status === "ACTIVE") : [];
  } catch (error) {
    products.value = [];
    setStatus(resolveErrorMessage(error, "加载企业产品失败"), "error");
  } finally {
    productLoading.value = false;
  }
}

async function handleEnterpriseChange() {
  form.productId = "";
  await loadProducts(form.enterpriseId);
}

function goBackToList() {
  router.push({ name: "regulator-admin-sampling" });
}

async function handleSubmit() {
  if (!form.enterpriseId) return setStatus("请选择企业", "error");
  if (!form.productId) return setStatus("请选择产品", "error");
  if (!form.taskTitle.trim()) return setStatus("请填写任务标题", "error");
  if (!form.deadline) return setStatus("请填写截止时间", "error");

  const deadline = normalizeSamplingDeadline(form.deadline);
  if (!deadline) return setStatus("截止时间格式无效", "error");

  submitting.value = true;
  setStatus("");
  try {
    await createSamplingTask(token.value, {
      enterpriseId: form.enterpriseId,
      productId: form.productId,
      taskTitle: form.taskTitle,
      taskDesc: form.taskDesc,
      priority: form.priority,
      deadline
    });
    setStatus("抽检任务已创建", "success");
    setTimeout(() => {
      router.push({ name: "regulator-admin-sampling" });
    }, 400);
  } catch (error) {
    setStatus(resolveErrorMessage(error, "创建抽检任务失败"), "error");
  } finally {
    submitting.value = false;
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
.create-page__head > div > p { margin: 6px 0 0; color: #64748b; font-size: 14px; }
.head-actions { display: flex; gap: 8px; }
.primary, .ghost { border-radius: 6px; padding: 10px 14px; font-size: 12px; font-weight: 700; cursor: pointer; }
.primary { border: 0; background: #002660; color: #fff; box-shadow: 0 12px 24px rgba(0, 38, 96, 0.2); }
.primary:disabled { opacity: 0.65; cursor: not-allowed; }
.ghost { border: 1px solid #d1d5db; background: #fff; color: #334155; }
.grid-layout { display: grid; gap: 16px; }
.main-col { display: grid; gap: 16px; }
.panel { background: #fff; border: 1px solid #e2e8f0; border-radius: 8px; padding: 14px; }
.panel-title { display: flex; align-items: center; gap: 8px; margin-bottom: 10px; }
.panel-title i { width: 4px; height: 18px; border-radius: 999px; background: #002660; display: block; }
.panel-title h2 { margin: 0; font-size: 16px; color: #002660; font-weight: 800; }
.form-grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 10px; align-items: end; }
.form-grid--2 { grid-template-columns: repeat(2, minmax(0, 1fr)); }
label { display: grid; gap: 6px; font-size: 12px; color: #64748b; font-weight: 700; }
.form-grid .span-all { grid-column: 1 / -1; }
input, select, textarea { border: 0; background: #f1f5f9; border-radius: 6px; padding: 10px; color: #1e293b; }
textarea { resize: vertical; min-height: 88px; }
.hint-warn { margin: 8px 0 0; font-size: 12px; color: #b45309; }
.status { position: fixed; right: 18px; bottom: 18px; border-radius: 8px; padding: 10px 12px; color: #fff; background: #0f172a; font-size: 13px; }
.status.error { background: #b91c1c; }
.status.success { background: #166534; }
@media (max-width: 900px) {
  .create-page__head { flex-direction: column; align-items: flex-start; }
  .head-actions { width: 100%; }
  .head-actions button { flex: 1; }
  .form-grid { grid-template-columns: 1fr; }
}
</style>
