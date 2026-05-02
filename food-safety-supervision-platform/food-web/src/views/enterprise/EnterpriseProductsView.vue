<template>
  <EnterpriseWorkspacePage
    active-key="products"
    title="产品档案"
    subtitle="管理并维护企业注册产品的安全合规档案。"
    top-search-placeholder="搜索功能、档案或编号..."
    :username="enterpriseUser.username"
    :user-type="enterpriseUser.userType"
    :status-label="statusLabel"
    :status-tone="statusTone"
    @navigate="handleSidebarNavigate"
    @logout="handleLogout"
  >
    <div class="enterprise-workspace-card enterprise-products-page">
      <header class="enterprise-page-hero">
        <div>
          <nav class="enterprise-page-hero__crumb" aria-label="面包屑">
            <span>企业工作台</span>
            <span class="material-symbols-outlined" aria-hidden="true">chevron_right</span>
            <span class="is-current">产品档案库</span>
          </nav>
          <h1 class="enterprise-page-hero__title">产品档案库</h1>
          <p class="enterprise-page-hero__desc">管理并维护所有企业注册产品的安全合规档案。</p>
        </div>
        <RouterLink class="primary enterprise-link-button enterprise-products-page__cta" :to="{ name: 'enterprise-product-create' }">
          <span class="material-symbols-outlined" aria-hidden="true" style="font-size: 18px">add</span>
          新增产品档案
        </RouterLink>
      </header>

      <div v-if="!profileLoaded" class="status info">请先完成企业备案后再维护产品档案。</div>
      <div v-else-if="approvalStatus !== 'APPROVED'" class="status info">当前企业备案尚未审核通过，产品档案维护功能暂不可用。</div>
      <template v-else>
        <div class="enterprise-bento-row">
          <div class="enterprise-bento-panel enterprise-bento-panel--tight">
            <div style="flex: 1; min-width: 240px">
              <span class="enterprise-field-label">产品名称 / 编号</span>
              <input
                v-model.trim="listKeyword"
                class="enterprise-products-filter-input"
                type="search"
                placeholder="输入关键字搜索..."
                autocomplete="off"
              />
            </div>
            <label style="min-width: 160px">
              <span class="enterprise-field-label">分类</span>
              <select v-model="listCategory" class="enterprise-products-filter-select">
                <option value="">全部品类</option>
                <option v-for="c in categoryOptions" :key="c" :value="c">{{ c }}</option>
              </select>
            </label>
            <label style="min-width: 140px">
              <span class="enterprise-field-label">档案状态</span>
              <select v-model="listStatus" class="enterprise-products-filter-select">
                <option value="">所有状态</option>
                <option value="ACTIVE">ACTIVE</option>
                <option value="INACTIVE">INACTIVE</option>
              </select>
            </label>
            <button type="button" class="enterprise-toolbar-button" @click="resetListFilters">重置筛选</button>
          </div>
          <div class="enterprise-insights-tile">
            <div>
              <div class="enterprise-insights-tile__eyebrow">Total Assets</div>
              <p class="enterprise-insights-tile__value">{{ totalProductCount }}</p>
              <p v-if="listFiltersActive" class="enterprise-insights-tile__sub">当前筛选 {{ filteredCount }} 条</p>
              <p class="enterprise-insights-tile__note">
                <span class="material-symbols-outlined" aria-hidden="true" style="font-size: 14px">trending_up</span>
                ACTIVE {{ activeProductCount }} / INACTIVE {{ inactiveProductCount }}
              </p>
            </div>
            <span class="material-symbols-outlined enterprise-insights-tile__bg-icon" aria-hidden="true">inventory_2</span>
          </div>
        </div>

        <div class="enterprise-data-table-wrap">
          <template v-if="filteredProducts.length">
            <table class="enterprise-data-table enterprise-data-table--products">
              <thead>
                <tr>
                  <th>产品名称与编号</th>
                  <th>分类</th>
                  <th>规格说明</th>
                  <th>状态</th>
                  <th style="text-align: right">操作</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="item in filteredProducts" :key="item.id">
                  <td>
                    <div style="display: flex; align-items: center; gap: 14px">
                      <div class="enterprise-product-thumb" aria-hidden="true">
                        <span class="material-symbols-outlined">inventory_2</span>
                      </div>
                      <div>
                        <div class="enterprise-product-table__name">{{ item.productName || "-" }}</div>
                        <div class="enterprise-product-table__id">ID: {{ item.id }}</div>
                      </div>
                    </div>
                  </td>
                  <td>
                    <span class="enterprise-product-cat-pill">{{ item.category || "-" }}</span>
                  </td>
                  <td>
                    <div class="enterprise-product-table__spec">{{ item.specification || "-" }}</div>
                    <div v-if="item.remark" class="enterprise-product-table__remark">{{ item.remark }}</div>
                  </td>
                  <td>
                    <span
                      class="enterprise-product-status-badge"
                      :class="item.status === 'ACTIVE' ? 'enterprise-product-status-badge--active' : 'enterprise-product-status-badge--inactive'"
                    >
                      <span class="enterprise-product-status-badge__dot" aria-hidden="true" />
                      {{ item.status === "ACTIVE" ? "ACTIVE" : "INACTIVE" }}
                    </span>
                    <span class="enterprise-product-table__status-cn">{{ formatProductStatus(item.status) }}</span>
                  </td>
                  <td style="text-align: right">
                    <div class="enterprise-product-row-actions">
                      <RouterLink
                        class="enterprise-product-row-actions__btn"
                        :to="{ name: 'enterprise-product-detail', params: { productId: item.id } }"
                        title="查看详情"
                        aria-label="查看详情"
                      >
                        <span class="material-symbols-outlined" aria-hidden="true">visibility</span>
                      </RouterLink>
                      <RouterLink
                        class="enterprise-product-row-actions__btn"
                        :to="{ name: 'enterprise-product-edit', params: { productId: item.id } }"
                        title="编辑档案"
                        aria-label="编辑档案"
                      >
                        <span class="material-symbols-outlined" aria-hidden="true">edit_note</span>
                      </RouterLink>
                      <button
                        class="enterprise-product-row-actions__btn"
                        type="button"
                        :disabled="productLoading"
                        :title="item.status === 'ACTIVE' ? '停用' : '启用'"
                        :aria-label="item.status === 'ACTIVE' ? '停用产品' : '启用产品'"
                        @click="handleToggleProductStatus(item)"
                      >
                        <span class="material-symbols-outlined" aria-hidden="true">
                          {{ item.status === "ACTIVE" ? "toggle_on" : "toggle_off" }}
                        </span>
                      </button>
                    </div>
                  </td>
                </tr>
              </tbody>
            </table>
            <div class="enterprise-data-table-footer">
              <span>
                显示 {{ filteredCount }} 条{{ listFiltersActive ? "（已筛选）" : "" }}，共 {{ totalProductCount }} 条档案
              </span>
            </div>
          </template>
          <EnterpriseEmptyState v-else title="暂无符合条件的产品" description="可尝试调整筛选条件或新增产品档案。" />
        </div>
      </template>

      <div v-if="status.message" class="status" :class="status.type">{{ status.message }}</div>
    </div>
  </EnterpriseWorkspacePage>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from "vue";
import { RouterLink } from "vue-router";
import { fetchEnterpriseProfile, fetchMyProducts, updateProduct } from "../../api/regulation";
import EnterpriseEmptyState from "../../components/enterprise/EnterpriseEmptyState.vue";
import EnterpriseWorkspacePage from "../../components/enterprise/EnterpriseWorkspacePage.vue";
import { resolveErrorMessage } from "../../utils/uiFeedback";
import {
  ENTERPRISE_PRODUCT_CATEGORY_PRESETS,
  formatProductStatus,
  getApprovalStatusLabel,
  getApprovalStatusTone,
  useEnterpriseShellSession
} from "./enterpriseShared";

const { enterpriseUser, token, handleSidebarNavigate, handleLogout } = useEnterpriseShellSession();
const status = reactive({ message: "", type: "" });
const profileLoaded = ref(false);
const approvalStatus = ref("");
const productLoading = ref(false);
const productRecords = ref([]);
const listKeyword = ref("");
const listCategory = ref("");
const listStatus = ref("");

const statusLabel = computed(() => getApprovalStatusLabel(profileLoaded.value, approvalStatus.value));
const statusTone = computed(() => getApprovalStatusTone(profileLoaded.value, approvalStatus.value));

const categoryOptions = computed(() => {
  const fromRecords = new Set(productRecords.value.map((p) => p.category).filter(Boolean));
  const extras = Array.from(fromRecords)
    .filter((c) => !ENTERPRISE_PRODUCT_CATEGORY_PRESETS.includes(c))
    .sort((a, b) => a.localeCompare(b, "zh-CN"));
  return [...ENTERPRISE_PRODUCT_CATEGORY_PRESETS, ...extras];
});

const filteredProducts = computed(() => {
  let list = productRecords.value;
  const k = listKeyword.value.trim().toLowerCase();
  if (k) {
    list = list.filter(
      (p) => (p.productName || "").toLowerCase().includes(k) || String(p.id || "").includes(k) || (p.remark || "").toLowerCase().includes(k)
    );
  }
  if (listCategory.value) list = list.filter((p) => p.category === listCategory.value);
  if (listStatus.value) list = list.filter((p) => p.status === listStatus.value);
  return list;
});

const totalProductCount = computed(() => productRecords.value.length);
const activeProductCount = computed(() => productRecords.value.filter((item) => item.status === "ACTIVE").length);
const inactiveProductCount = computed(() => productRecords.value.filter((item) => item.status !== "ACTIVE").length);
const filteredCount = computed(() => filteredProducts.value.length);
const listFiltersActive = computed(() => Boolean(listKeyword.value.trim() || listCategory.value || listStatus.value));

function resetListFilters() {
  listKeyword.value = "";
  listCategory.value = "";
  listStatus.value = "";
}

function setStatus(message, type = "info") {
  status.message = message;
  status.type = type;
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

async function loadProducts() {
  if (!profileLoaded.value || approvalStatus.value !== "APPROVED") {
    productRecords.value = [];
    return;
  }
  productLoading.value = true;
  setStatus("");
  try {
    productRecords.value = await fetchMyProducts(token.value);
  } catch (error) {
    setStatus(resolveErrorMessage(error, "加载产品档案失败，请稍后重试"), "error");
  } finally {
    productLoading.value = false;
  }
}

async function handleToggleProductStatus(item) {
  if (!item?.id) return;
  productLoading.value = true;
  setStatus("");
  try {
    await updateProduct(token.value, item.id, {
      productName: item.productName,
      category: item.category,
      specification: item.specification,
      status: item.status === "ACTIVE" ? "INACTIVE" : "ACTIVE",
      remark: item.remark
    });
    setStatus("产品状态更新成功。", "success");
    await loadProducts();
  } catch (error) {
    setStatus(resolveErrorMessage(error, "更新产品状态失败，请稍后重试。"), "error");
  } finally {
    productLoading.value = false;
  }
}

onMounted(async () => {
  await loadProfileState();
  await loadProducts();
});
</script>
