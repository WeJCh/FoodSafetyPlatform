<template>
  <section class="enterprise-page" v-if="product">
    <nav class="enterprise-breadcrumbs">
      <RouterLink :to="{ name: 'enterprise-products' }">产品档案</RouterLink>
      <span class="material-symbols-outlined">chevron_right</span>
      <span>详情预览</span>
    </nav>

    <div class="enterprise-grid enterprise-grid--detail">
      <article class="enterprise-card enterprise-card--summary">
        <div class="section-head">
          <h3>{{ product.productName }}</h3>
          <span :class="['enterprise-chip', `enterprise-chip--${product.status === 'ACTIVE' ? 'success' : 'neutral'}`]">
            {{ product.status === 'ACTIVE' ? '启用中' : '已停用' }}
          </span>
        </div>
        <p class="product-card__desc">{{ product.remark }}</p>
        <dl class="data-pairs">
          <div><dt>产品类别</dt><dd>{{ product.category }}</dd></div>
          <div><dt>规格型号</dt><dd>{{ product.specification }}</dd></div>
          <div><dt>保质期</dt><dd>{{ product.shelfLife || '未填写' }}</dd></div>
          <div><dt>批文编号</dt><dd>{{ product.approvalNo || '未配置' }}</dd></div>
          <div><dt>最近更新</dt><dd>{{ product.updateTime }}</dd></div>
          <div><dt>监管状态</dt><dd>{{ product.status }}</dd></div>
        </dl>
        <div class="card-actions">
          <RouterLink class="enterprise-primary-button" :to="{ name: 'enterprise-product-edit', params: { id: product.id } }">编辑产品</RouterLink>
          <RouterLink class="enterprise-ghost-button" :to="{ name: 'enterprise-products' }">返回列表</RouterLink>
        </div>
      </article>

      <aside class="enterprise-card">
        <div class="section-head">
          <h3>修改日志</h3>
          <span>Audit Trail</span>
        </div>
        <div class="timeline-list">
          <div class="timeline-item">
            <strong>{{ product.updateTime }}</strong>
            <p>最新监管状态同步至数字账本。</p>
          </div>
          <div class="timeline-item">
            <strong>2026-03-18 10:12:00</strong>
            <p>完成基础信息校验并归档。</p>
          </div>
        </div>
      </aside>
    </div>
  </section>
</template>

<script setup>
import { onMounted, ref } from "vue";
import { useRoute } from "vue-router";
import { loadProductById } from "../../services/enterpriseGateway";
import { getStoredSession } from "../../session/authSession";

const route = useRoute();
const product = ref(null);

onMounted(async () => {
  const session = getStoredSession();
  product.value = await loadProductById(session?.token || "", route.params.id);
});
</script>
