<template>
  <section class="enterprise-page" v-if="detail">
    <nav class="enterprise-breadcrumbs">
      <RouterLink :to="{ name: 'enterprise-inspections' }">检查记录</RouterLink>
      <span class="material-symbols-outlined">chevron_right</span>
      <span>详情</span>
    </nav>

    <div class="enterprise-grid enterprise-grid--detail">
      <article class="enterprise-card enterprise-card--summary">
        <div class="section-head">
          <h3>{{ detail.record.title }}</h3>
          <span :class="['enterprise-chip', `enterprise-chip--${detail.record.result === 'PASS' ? 'success' : 'danger'}`]">
            {{ detail.record.result === 'PASS' ? '合格' : '待整改' }}
          </span>
        </div>
        <dl class="data-pairs">
          <div><dt>检查日期</dt><dd>{{ detail.record.inspectionDate }}</dd></div>
          <div><dt>检查员</dt><dd>{{ detail.record.inspector }}</dd></div>
          <div><dt>问题描述</dt><dd>{{ detail.record.problemDesc }}</dd></div>
          <div><dt>账本备注</dt><dd>{{ detail.record.ledgerNote }}</dd></div>
        </dl>

        <div class="section-head section-head--column section-head--subtle">
          <h3>检查明细</h3>
          <p>按原型统一为独立详情页，不再使用弹窗承载。</p>
        </div>
        <div class="detail-list">
          <div v-for="(item, index) in detail.items" :key="index" class="detail-list__item">
            <strong>{{ item.itemName }}</strong>
            <span>{{ item.problemDesc }}</span>
            <em>{{ item.itemResult }}</em>
          </div>
        </div>
      </article>

      <aside class="enterprise-card">
        <div class="section-head">
          <h3>关联整改任务</h3>
          <span>{{ detail.record.rectificationId ? '已关联' : '无关联任务' }}</span>
        </div>
        <template v-if="detail.record.rectificationId">
          <p class="product-card__desc">该次检查已生成整改任务，可直接进入任务详情继续跟进。</p>
          <RouterLink class="enterprise-primary-button" :to="{ name: 'enterprise-rectification-detail', params: { id: detail.record.rectificationId } }">查看整改详情</RouterLink>
        </template>
        <template v-else>
          <p class="product-card__desc">当前检查无需整改，已完成归档。</p>
          <RouterLink class="enterprise-ghost-button" :to="{ name: 'enterprise-inspections' }">返回列表</RouterLink>
        </template>
      </aside>
    </div>
  </section>
</template>

<script setup>
import { onMounted, ref } from "vue";
import { useRoute } from "vue-router";
import { loadInspectionDetail } from "../../services/enterpriseGateway";
import { getStoredSession } from "../../session/authSession";

const route = useRoute();
const detail = ref(null);

onMounted(async () => {
  const session = getStoredSession();
  detail.value = await loadInspectionDetail(session?.token || "", route.params.id);
});
</script>
