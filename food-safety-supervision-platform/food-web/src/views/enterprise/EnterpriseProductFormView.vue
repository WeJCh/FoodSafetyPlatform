<template>
  <EnterpriseWorkspacePage
    active-key="products"
    :title="pageTitle"
    :subtitle="pageSubtitle"
    top-search-placeholder="搜索功能、档案或编号..."
    :username="enterpriseUser.username"
    :user-type="enterpriseUser.userType"
    :status-label="statusLabel"
    :status-tone="statusTone"
    @navigate="handleSidebarNavigate"
    @logout="handleLogout"
  >
    <div class="enterprise-workspace-card enterprise-product-form-page">
      <nav class="enterprise-page-hero__crumb" style="margin-bottom: 12px" aria-label="面包屑">
        <span>企业工作台</span>
        <span class="material-symbols-outlined" aria-hidden="true">chevron_right</span>
        <span>产品档案</span>
        <span class="material-symbols-outlined" aria-hidden="true">chevron_right</span>
        <span class="is-current">{{ isEdit ? "编辑产品" : "新增产品" }}</span>
      </nav>

      <header class="enterprise-page-hero" style="border-bottom: none; padding-bottom: 0; margin-bottom: 24px">
        <div style="min-width: 0">
          <h1 class="enterprise-page-hero__title" style="margin: 0 0 8px">{{ pageTitle }}</h1>
          <p class="enterprise-page-hero__desc" style="margin: 0">{{ pageSubtitle }}</p>
        </div>
        <div class="enterprise-product-form-header-actions" role="toolbar" aria-label="表单操作">
          <RouterLink class="enterprise-product-form-header-actions__cancel" :to="{ name: 'enterprise-products' }">
            取消
          </RouterLink>
          <button
            v-if="!isEdit"
            type="button"
            class="enterprise-product-form-header-actions__secondary"
            :disabled="loading || !canSubmit"
            @click="handleSaveAndAddAnother"
          >
            保存并继续添加
          </button>
          <button
            type="submit"
            class="primary enterprise-link-button enterprise-product-form-header-actions__submit"
            form="enterprise-product-form"
            :disabled="loading || !canSubmit"
          >
            {{ primarySubmitLabel }}
          </button>
        </div>
      </header>

      <div v-if="!profileLoaded" class="status info">请先完成企业备案后再维护产品档案。</div>
      <div v-else-if="approvalStatus !== 'APPROVED'" class="status info">当前企业备案尚未审核通过，暂不可维护产品档案。</div>
      <form v-else id="enterprise-product-form" class="enterprise-product-form-layout" @submit.prevent="handleSubmit">
        <div class="enterprise-product-form-main">
          <section class="enterprise-product-form-card">
            <div class="enterprise-product-form-card__head">
              <div class="enterprise-product-form-card__head-bar" aria-hidden="true" />
              <h3>基本信息</h3>
            </div>
            <div class="enterprise-product-form-grid">
              <label class="enterprise-product-form-field enterprise-product-form-grid__full">
                <span>产品名称</span>
                <input v-model.trim="form.productName" required placeholder="输入完整产品名称" autocomplete="off" />
              </label>
              <label class="enterprise-product-form-field enterprise-product-form-field--select">
                <span>产品分类</span>
                <select v-model="form.category" required>
                  <option value="" disabled>选择产品所属分类</option>
                  <option v-for="c in categoryOptions" :key="c" :value="c">{{ c }}</option>
                </select>
                <span class="material-symbols-outlined" aria-hidden="true">expand_more</span>
              </label>
              <label class="enterprise-product-form-field">
                <span>规格型号（重量 / 体积）</span>
                <input v-model.trim="form.specification" placeholder="例如：500g、1.5L" autocomplete="off" />
              </label>
              <label class="enterprise-product-form-field enterprise-product-form-grid__full">
                <span>产品备注</span>
                <textarea
                  v-model.trim="form.remark"
                  rows="4"
                  placeholder="补充产品配料、包装、贮存条件或其他说明（可选）"
                />
              </label>
            </div>
          </section>

          <section class="enterprise-product-form-card enterprise-product-form-card--muted">
            <div class="enterprise-product-form-status-head">
              <div class="enterprise-product-form-card__head" style="margin-bottom: 0">
                <div class="enterprise-product-form-card__head-bar" aria-hidden="true" />
                <h3>档案状态控制</h3>
              </div>
              <div class="enterprise-product-status-toggle" role="group" aria-label="档案状态">
                <button
                  type="button"
                  :class="{ 'is-active': form.status === 'ACTIVE' }"
                  @click="form.status = 'ACTIVE'"
                >
                  ACTIVE
                </button>
                <button
                  type="button"
                  :class="{ 'is-active': form.status === 'INACTIVE' }"
                  @click="form.status = 'INACTIVE'"
                >
                  INACTIVE
                </button>
              </div>
            </div>
            <div class="enterprise-product-form-callout">
              <div class="enterprise-product-form-callout__icon" aria-hidden="true">
                <span class="material-symbols-outlined">info</span>
              </div>
              <div>
                <h4>档案维护提示</h4>
                <p>
                  设置为 ACTIVE 后，该产品会进入企业正式产品档案，并可能用于监管抽检、检查或台账关联展示。请确保产品名称、分类与备案信息保持一致。
                </p>
              </div>
            </div>
          </section>
        </div>

        <aside class="enterprise-product-form-aside">
          <section class="enterprise-product-form-policy">
            <span class="material-symbols-outlined enterprise-product-form-policy__bg-icon" aria-hidden="true">policy</span>
            <h4>
              <span class="material-symbols-outlined" style="font-size: 20px" aria-hidden="true">verified_user</span>
              备案合规指引
            </h4>
            <ul>
              <li>
                <span class="enterprise-product-form-policy__bullet">•</span>
                <span>产品名称应与包装标识、备案资料中的法定名称保持一致。</span>
              </li>
              <li>
                <span class="enterprise-product-form-policy__bullet">•</span>
                <span>当前系统未接入产品图片上传，请在备注中补充包装、标签和关键识别信息。</span>
              </li>
              <li>
                <span class="enterprise-product-form-policy__bullet">•</span>
                <span>分类与企业许可范围不一致时，后续监管核对可能会提示异常。</span>
              </li>
            </ul>
            <div class="enterprise-product-form-policy__foot">
              <span>Regulatory checklist</span>
              <span class="material-symbols-outlined" style="font-size: 18px; color: rgba(255, 255, 255, 0.55)" aria-hidden="true">arrow_forward</span>
            </div>
          </section>
        </aside>
      </form>

      <div v-if="status.message" class="status" :class="status.type" style="margin-top: 16px">{{ status.message }}</div>
    </div>
  </EnterpriseWorkspacePage>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from "vue";
import { RouterLink, useRoute, useRouter } from "vue-router";
import { createProduct, fetchEnterpriseProfile, fetchMyProducts, updateProduct } from "../../api/regulation";
import EnterpriseWorkspacePage from "../../components/enterprise/EnterpriseWorkspacePage.vue";
import { resolveErrorMessage } from "../../utils/uiFeedback";
import {
  ENTERPRISE_PRODUCT_CATEGORY_PRESETS,
  getApprovalStatusLabel,
  getApprovalStatusTone,
  useEnterpriseShellSession
} from "./enterpriseShared";

const route = useRoute();
const router = useRouter();
const { enterpriseUser, token, handleSidebarNavigate, handleLogout } = useEnterpriseShellSession();
const productId = computed(() => String(route.params.productId || ""));
const isEdit = computed(() => !!productId.value);
const profileLoaded = ref(false);
const approvalStatus = ref("");
const loading = ref(false);
const status = reactive({ message: "", type: "" });
const form = reactive({ productName: "", category: "", specification: "", status: "ACTIVE", remark: "" });

const statusLabel = computed(() => getApprovalStatusLabel(profileLoaded.value, approvalStatus.value));
const statusTone = computed(() => getApprovalStatusTone(profileLoaded.value, approvalStatus.value));
const pageTitle = computed(() => (isEdit.value ? "编辑产品信息" : "新增产品档案"));
const pageSubtitle = computed(() =>
  isEdit.value ? "更新产品档案中的监管信息与启用状态。" : "录入新的食品产品详细信息，建立正式产品档案。"
);
const primarySubmitLabel = computed(() => (isEdit.value ? "保存并返回详情" : "提交并返回列表"));

const categoryOptions = computed(() => [...ENTERPRISE_PRODUCT_CATEGORY_PRESETS]);

const canSubmit = computed(
  () => profileLoaded.value && approvalStatus.value === "APPROVED" && Boolean(form.productName.trim()) && Boolean(form.category)
);

function setStatus(message, type = "info") {
  status.message = message;
  status.type = type;
}

function applyForm(payload = {}) {
  form.productName = payload.productName || "";
  form.category = payload.category || "";
  form.specification = payload.specification || "";
  form.status = payload.status || "ACTIVE";
  form.remark = payload.remark || "";
}

async function loadProfileState() {
  try {
    const data = await fetchEnterpriseProfile(token.value);
    approvalStatus.value = data.approvalStatus || "";
    profileLoaded.value = true;
  } catch (error) {
    if (String(error?.message).includes("not found")) {
      profileLoaded.value = false;
      approvalStatus.value = "";
      return;
    }
    setStatus(resolveErrorMessage(error, "加载备案状态失败，请稍后重试。"), "error");
  }
}

async function loadExistingProduct() {
  if (!isEdit.value) return;
  try {
    const records = await fetchMyProducts(token.value);
    const matched = (records || []).find((item) => String(item.id) === productId.value);
    if (!matched) {
      setStatus("未找到要编辑的产品。", "error");
      return;
    }
    applyForm(matched);
  } catch (error) {
    setStatus(resolveErrorMessage(error, "加载产品信息失败，请稍后重试"), "error");
  }
}

async function handleSaveAndAddAnother() {
  if (isEdit.value || !canSubmit.value) return;
  loading.value = true;
  setStatus("");
  try {
    const payload = { ...form };
    await createProduct(token.value, payload);
    setStatus("已创建，可继续添加下一条。", "success");
    applyForm({ productName: "", category: "", specification: "", status: "ACTIVE", remark: "" });
  } catch (error) {
    setStatus(resolveErrorMessage(error, "创建产品失败，请稍后重试"), "error");
  } finally {
    loading.value = false;
  }
}

async function handleSubmit() {
  if (!profileLoaded.value || approvalStatus.value !== "APPROVED") {
    setStatus("企业备案审核通过后才能维护产品档案。", "error");
    return;
  }
  if (!canSubmit.value) {
    setStatus("请填写产品名称并选择分类。", "error");
    return;
  }
  loading.value = true;
  setStatus("");
  try {
    const payload = { ...form };
    if (isEdit.value) {
      await updateProduct(token.value, productId.value, payload);
      await router.replace({ name: "enterprise-product-detail", params: { productId: productId.value } });
    } else {
      await createProduct(token.value, payload);
      await router.replace({ name: "enterprise-products" });
    }
  } catch (error) {
    setStatus(resolveErrorMessage(error, "保存产品失败，请稍后重试"), "error");
  } finally {
    loading.value = false;
  }
}

onMounted(async () => {
  await loadProfileState();
  await loadExistingProduct();
});
</script>
