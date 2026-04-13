<template>
  <EnterpriseWorkspacePage
    active-key="products"
    title="产品详情"
    subtitle="查看产品档案、备注与监管侧占位信息。"
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
              <h1>{{ product.productName || "—" }}</h1>
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
            <button type="button" class="enterprise-product-hero__danger-btn" @click="onDeleteProduct">删除</button>
            <RouterLink class="primary enterprise-link-button enterprise-product-hero__edit-btn" :to="{ name: 'enterprise-product-edit', params: { productId } }">
              <span class="material-symbols-outlined" aria-hidden="true">edit</span>
              编辑产品
            </RouterLink>
          </div>
        </header>

        <div class="enterprise-product-detail-grid">
          <div class="enterprise-product-detail-main">
            <section class="enterprise-panel">
              <h3 class="enterprise-product-detail-section-title">基本信息 / BASIC INFORMATION</h3>
              <div class="enterprise-detail-grid enterprise-product-detail-basic-grid">
                <div class="enterprise-readonly-field">
                  <span>产品类别</span>
                  <div>{{ product.category || "—" }}</div>
                </div>
                <div class="enterprise-readonly-field">
                  <span>规格型号</span>
                  <div>{{ product.specification || "—" }}</div>
                </div>
                <div class="enterprise-readonly-field">
                  <span>执行标准</span>
                  <div>待补充</div>
                </div>
                <div class="enterprise-readonly-field">
                  <span>保质期</span>
                  <div>待补充</div>
                </div>
              </div>
            </section>

            <section class="enterprise-panel">
              <h3 class="enterprise-product-detail-section-title">产品描述 / PRODUCT DESCRIPTION</h3>
              <p class="enterprise-product-detail-description">{{ product.remark || "暂无描述，建议补充产品原料、工艺、贮存条件等合规信息。" }}</p>
            </section>

            <section class="enterprise-panel">
              <div class="enterprise-product-detail-audit-head">
                <h3 class="enterprise-product-detail-section-title">备案历史 / FILING HISTORY</h3>
                <button type="button" class="enterprise-product-detail-audit-link" @click="onFullAuditLog">查看全部日志</button>
              </div>
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
              <p class="enterprise-product-detail-note">完整审计轨迹待后端提供专用查询接口。</p>
            </section>
          </div>

          <aside class="enterprise-side-stack">
            <div class="enterprise-product-preview-card">
              <div class="enterprise-product-preview-card__image" aria-hidden="true">
                <span class="material-symbols-outlined">inventory_2</span>
              </div>
              <div class="enterprise-product-preview-card__foot">
                <span>预览图 1/1</span>
                <span class="material-symbols-outlined" aria-hidden="true">zoom_in</span>
              </div>
            </div>

            <div class="enterprise-regulatory-gauge">
              <span class="material-symbols-outlined enterprise-regulatory-gauge__bg" aria-hidden="true">verified_user</span>
              <h4>监管状态概览</h4>
              <div class="enterprise-regulatory-gauge__stats">
                <div>
                  <span>合规评分</span>
                  <strong>— <em>/ 100</em></strong>
                </div>
                <div>
                  <span>近一年抽检次数</span>
                  <strong>—</strong>
                </div>
                <div>
                  <span>异常记录</span>
                  <b>待接入</b>
                </div>
              </div>
              <p>演示模块：与抽检、评分联动后展示真实数据。</p>
            </div>

            <div class="enterprise-side-card">
              <div class="enterprise-side-card__head">相关证书 / CERTIFICATES</div>
              <div class="enterprise-side-card__body">
                <div v-for="cert in certificatePlaceholders" :key="cert.name" class="enterprise-certificate-row">
                  <span class="material-symbols-outlined" aria-hidden="true">verified</span>
                  <div>
                    <div>{{ cert.name }}</div>
                    <div>{{ cert.validity }}</div>
                  </div>
                  <button type="button" class="enterprise-certificate-row__download" title="下载" @click="onCertDownload">
                    <span class="material-symbols-outlined" aria-hidden="true">file_download</span>
                  </button>
                </div>
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
import { computed, onMounted, reactive, ref } from "vue";
import { RouterLink, useRoute } from "vue-router";
import { fetchMyProducts } from "../../api/regulation";
import EnterpriseStatusChip from "../../components/enterprise/EnterpriseStatusChip.vue";
import EnterpriseWorkspacePage from "../../components/enterprise/EnterpriseWorkspacePage.vue";
import { formatTime } from "../../utils/formatters";
import { enterpriseFeaturePendingNotice, useEnterpriseShellSession } from "./enterpriseShared";

const route = useRoute();
const { enterpriseUser, token, handleSidebarNavigate, handleLogout } = useEnterpriseShellSession();
const productId = String(route.params.productId || "");
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
const certificatePlaceholders = [
  { name: "中国有机产品认证（占位）", validity: "有效期：待维护" },
  { name: "HACCP 质量管理体系（占位）", validity: "有效期：待维护" }
];

function onDeleteProduct() {
  // TODO: 接入删除产品档案接口并补充二次确认对话框
  enterpriseFeaturePendingNotice("删除产品档案");
}

function onFullAuditLog() {
  enterpriseFeaturePendingNotice("备案历史全量日志");
}

function onCertDownload() {
  enterpriseFeaturePendingNotice("证书下载");
}

async function loadProduct() {
  loading.value = true;
  try {
    const records = await fetchMyProducts(token.value);
    product.value = (records || []).find((item) => String(item.id) === productId) || null;
  } catch (error) {
    status.message = error.message || "加载产品详情失败";
    status.type = "error";
  } finally {
    loading.value = false;
  }
}

onMounted(() => {
  loadProduct();
});
</script>
