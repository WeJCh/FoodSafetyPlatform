<template>
  <section class="enterprise-page">
    <div class="section-banner">
      <div>
        <p class="section-kicker">产品档案库</p>
        <h2>产品档案库</h2>
        <p>查看并维护当前企业的产品档案、监管状态与合规信息。</p>
      </div>
      <RouterLink class="enterprise-primary-button" :to="{ name: 'enterprise-product-create' }">
        <span class="material-symbols-outlined">add</span>
        <span>新增产品档案</span>
      </RouterLink>
    </div>

    <div class="product-grid">
      <article v-for="item in products" :key="item.id" class="enterprise-card product-card">
        <div class="product-card__head">
          <div>
            <p class="section-kicker">{{ item.category }}</p>
            <h3>{{ item.productName }}</h3>
          </div>
          <span :class="['enterprise-chip', `enterprise-chip--${item.status === 'ACTIVE' ? 'success' : 'neutral'}`]">
            {{ item.status === 'ACTIVE' ? '启用中' : '已停用' }}
          </span>
        </div>
        <dl class="data-pairs data-pairs--compact">
          <div><dt>规格型号</dt><dd>{{ item.specification }}</dd></div>
          <div><dt>保质期</dt><dd>{{ item.shelfLife || '未填写' }}</dd></div>
          <div><dt>批文编号</dt><dd>{{ item.approvalNo || '未配置' }}</dd></div>
          <div><dt>最近更新</dt><dd>{{ item.updateTime }}</dd></div>
        </dl>
        <p class="product-card__desc">{{ item.remark }}</p>
        <div class="card-actions">
          <RouterLink class="enterprise-ghost-button" :to="{ name: 'enterprise-product-detail', params: { id: item.id } }">查看详情</RouterLink>
          <RouterLink class="enterprise-primary-button enterprise-primary-button--compact" :to="{ name: 'enterprise-product-edit', params: { id: item.id } }">编辑产品</RouterLink>
        </div>
      </article>
    </div>
  </section>
</template>

<script setup>
import { onMounted, ref } from "vue";
import { loadProducts } from "../../services/enterpriseGateway";
import { getStoredSession } from "../../session/authSession";

const products = ref([]);

onMounted(async () => {
  const session = getStoredSession();
  products.value = await loadProducts(session?.token || "");
});
</script>
