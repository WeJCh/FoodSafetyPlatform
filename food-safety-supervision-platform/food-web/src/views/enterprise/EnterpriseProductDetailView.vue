<template>
  <EnterpriseWorkspacePage
    active-key="products"
    title="产品详情"
    subtitle="查看当前已接入的产品档案信息。"
    top-search-placeholder="搜索功能、档案或编号..."
    :username="enterpriseUser.username"
    :user-type="enterpriseUser.userType"
    status-label="产品详情"
    status-tone="neutral"
    @navigate="handleSidebarNavigate"
    @logout="handleLogout"
  >
    <div class="enterprise-workspace-card enterprise-product-detail-page">
      <nav class="enterprise-page-hero__crumb enterprise-product-detail-page__crumb" aria-label="面包屑">
        <span>产品档案</span>
        <span class="material-symbols-outlined" aria-hidden="true">chevron_right</span>
        <span class="is-current">详情预览</span>
      </nav>

      <div v-if="loading" class="status info">加载中...</div>
      <div v-else-if="!product" class="status error">未找到该产品档案。</div>
      <template v-else>
        <header class="enterprise-product-hero">
          <div class="enterprise-product-hero__main">
            <div class="enterprise-product-hero__title-row">
              <h1>{{ product.productName || "-" }}</h1>
              <EnterpriseStatusChip
                :label="`${product.status === 'ACTIVE' ? 'ACTIVE / 已上架' : 'INACTIVE / 已下架'}`"
                :tone="product.status === 'ACTIVE' ? 'success' : 'neutral'"
              />
            </div>
            <div class="enterprise-product-hero__meta">
              <span><span class="material-symbols-outlined" aria-hidden="true">business</span>企业：{{ enterpriseDisplayName }}</span>
              <span><span class="material-symbols-outlined" aria-hidden="true">schedule</span>最后更新：{{ formatTime(product.updateTime) }}</span>
              <span><span class="material-symbols-outlined" aria-hidden="true">tag</span>ID {{ product.id }}</span>
            </div>
          </div>
          <div class="enterprise-product-hero__actions" role="toolbar" aria-label="产品详情操作">
            <RouterLink class="enterprise-toolbar-button" :to="{ name: 'enterprise-products' }">返回列表</RouterLink>
            <RouterLink class="primary enterprise-link-button enterprise-product-hero__edit-btn" :to="{ name: 'enterprise-product-edit', params: { productId } }">
              <span class="material-symbols-outlined" aria-hidden="true">edit</span>
              编辑产品
            </RouterLink>
          </div>
        </header>

        <div class="enterprise-product-detail-grid">
          <div class="enterprise-product-detail-main">
            <section class="enterprise-panel">
              <h3 class="enterprise-product-detail-section-title">基础档案 / PRODUCT PROFILE</h3>
              <div class="enterprise-detail-grid enterprise-product-detail-basic-grid">
                <div class="enterprise-readonly-field">
                  <span>产品类别</span>
                  <div>{{ product.category || "-" }}</div>
                </div>
                <div class="enterprise-readonly-field">
                  <span>规格型号</span>
                  <div>{{ product.specification || "-" }}</div>
                </div>
                <div class="enterprise-readonly-field">
                  <span>产品状态</span>
                  <div>{{ product.status === "ACTIVE" ? "已上架" : "已下架" }}</div>
                </div>
                <div class="enterprise-readonly-field">
                  <span>档案编号</span>
                  <div>{{ product.id || "-" }}</div>
                </div>
              </div>
            </section>

            <section class="enterprise-panel">
              <h3 class="enterprise-product-detail-section-title">备注信息 / REMARK</h3>
              <p class="enterprise-product-detail-description">{{ product.remark || "暂无备注信息。" }}</p>
            </section>

            <section class="enterprise-panel">
              <h3 class="enterprise-product-detail-section-title">备案历史 / FILING HISTORY</h3>
              <div class="enterprise-audit-trail-v">
                <div v-for="item in filingHistoryPreview" :key="item.key" class="enterprise-audit-node">
                  <span class="enterprise-audit-node__dot" :class="{ 'is-muted': item.muted }" />
                  <div class="enterprise-product-detail-audit-item-head">
                    <strong>{{ item.title }}</strong>
                    <span>{{ item.time }}</span>
                  </div>
                  <p>{{ item.desc }}</p>
                </div>
              </div>
            </section>
          </div>

          <aside class="enterprise-side-stack">
            <div class="enterprise-product-preview-card">
              <div class="enterprise-product-preview-card__image" aria-hidden="true">
                <span class="material-symbols-outlined">inventory_2</span>
              </div>
              <div class="enterprise-product-preview-card__foot">
                <span>产品档案封面</span>
                <span class="secondary-text">当前未配置产品图片</span>
              </div>
            </div>
          </aside>
        </div>
      </template>

      <div v-if="status.message" class="status" :class="status.type">{{ status.message }}</div>
    </div>
  </EnterpriseWorkspacePage>
</template>

<script setup>
import { computed, reactive, ref, watch } from "vue";
import { RouterLink, useRoute } from "vue-router";
import { fetchMyProducts } from "../../api/regulation";
import { resolveErrorMessage } from "../../utils/uiFeedback";
import EnterpriseStatusChip from "../../components/enterprise/EnterpriseStatusChip.vue";
import EnterpriseWorkspacePage from "../../components/enterprise/EnterpriseWorkspacePage.vue";
import { formatTime } from "../../utils/formatters";
import { useEnterpriseShellSession } from "./enterpriseShared";

const route = useRoute();
const { enterpriseUser, token, handleSidebarNavigate, handleLogout } = useEnterpriseShellSession();
const productId = computed(() => String(route.params.productId || ""));
const loading = ref(false);
const product = ref(null);
const status = reactive({ message: "", type: "" });
const enterpriseDisplayName = computed(() => enterpriseUser.value?.enterpriseName || enterpriseUser.value?.username || "当前企业");
const filingHistoryPreview = computed(() => [
  {
    key: "latest",
    title: "产品信息更新",
    time: formatTime(product.value?.updateTime),
    desc: "已同步该产品的最新档案信息至企业工作台。",
    muted: false
  },
  {
    key: "created",
    title: "企业首次备案",
    time: formatTime(product.value?.createTime),
    desc: "该产品已在监管系统完成首次备案登记。",
    muted: true
  }
]);

async function loadProduct() {
  loading.value = true;
  try {
    const records = await fetchMyProducts(token.value);
    if (!productId.value) {
      product.value = null;
      return;
    }
    product.value = (records || []).find((item) => String(item.id) === productId.value) || null;
  } catch (error) {
    status.message = resolveErrorMessage(error, "加载产品详情失败");
    status.type = "error";
  } finally {
    loading.value = false;
  }
}

watch(
  () => route.params.productId,
  () => {
    loadProduct();
  },
  { immediate: true }
);
</script>
