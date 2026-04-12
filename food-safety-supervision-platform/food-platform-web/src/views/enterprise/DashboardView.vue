<template>
  <section class="enterprise-page enterprise-dashboard-page">
    <div class="enterprise-grid enterprise-grid--dashboard">
      <article class="enterprise-card enterprise-card--hero">
        <div class="enterprise-card__accent"></div>
        <div class="hero-split">
          <div class="hero-split__copy">
            <p class="section-kicker">当前备案状态</p>
            <h2>{{ profile.archiveStatus || '已核准' }}</h2>
            <p class="hero-description">
              您的企业备案信息已于 {{ profile.approvedTime || '2023-10-24 09:18:00' }} 完成核验。
              下一次例行审查预计在 {{ profile.nextReviewAt || '2024-10-24' }} 前进行。
            </p>
            <div class="hero-actions">
              <RouterLink class="enterprise-primary-button" :to="{ name: 'enterprise-profile-detail' }">
                <span class="material-symbols-outlined">edit</span>
                <span>更新备案信息</span>
              </RouterLink>
              <button class="enterprise-ghost-button" type="button">下载电子证书</button>
            </div>
          </div>
          <div class="hero-split__certificate">
            <span class="material-symbols-outlined">gavel</span>
            <small>VALID UNTIL</small>
            <strong>{{ profile.nextReviewAt || '2024-10-24' }}</strong>
          </div>
        </div>
      </article>

      <article class="enterprise-stat-card">
        <div>
          <p>在册产品数量</p>
          <strong>{{ products.length }}</strong>
        </div>
        <span class="material-symbols-outlined">inventory_2</span>
      </article>

      <article class="enterprise-stat-card enterprise-stat-card--warning">
        <div>
          <p>待整改任务</p>
          <strong>{{ rectifications.length }}</strong>
        </div>
        <span class="material-symbols-outlined">warning</span>
      </article>

      <RouterLink class="enterprise-cta-card" :to="{ name: 'enterprise-product-create' }">
        <span>快速新增产品</span>
        <span class="material-symbols-outlined">add_circle</span>
      </RouterLink>

      <article class="enterprise-card">
        <div class="section-head">
          <h3>近期检查记录</h3>
          <RouterLink :to="{ name: 'enterprise-inspections' }">查看全部</RouterLink>
        </div>
        <div class="stack-list">
          <RouterLink v-for="record in inspections" :key="record.id" class="stack-list__item" :to="{ name: 'enterprise-inspection-detail', params: { id: record.id } }">
            <div>
              <strong>{{ record.title }}</strong>
              <p>{{ record.inspectionDate }} · 检查员：{{ record.inspector }}</p>
            </div>
            <span :class="['enterprise-chip', `enterprise-chip--${record.result === 'PASS' ? 'success' : 'danger'}`]">
              {{ record.result === 'PASS' ? '合格' : '待整改' }}
            </span>
          </RouterLink>
        </div>
      </article>

      <article class="enterprise-card">
        <div class="section-head">
          <h3>整改跟进</h3>
          <RouterLink :to="{ name: 'enterprise-rectifications' }">查看全部</RouterLink>
        </div>
        <div class="stack-list">
          <RouterLink v-for="item in rectifications" :key="item.id" class="stack-list__item" :to="{ name: 'enterprise-rectification-detail', params: { id: item.id } }">
            <div>
              <strong>{{ item.title }}</strong>
              <p>截止 {{ item.dueDate }} · {{ item.focusArea }}</p>
            </div>
            <span :class="['enterprise-chip', `enterprise-chip--${statusTone(item.status)}`]">{{ statusLabel(item.status) }}</span>
          </RouterLink>
        </div>
      </article>
    </div>
  </section>
</template>

<script setup>
import { onMounted, reactive, ref } from "vue";
import { loadDashboardSnapshot } from "../../services/enterpriseGateway";
import { getStoredSession } from "../../session/authSession";

const profile = reactive({});
const products = ref([]);
const inspections = ref([]);
const rectifications = ref([]);

onMounted(async () => {
  const session = getStoredSession();
  const snapshot = await loadDashboardSnapshot(session?.token || "");
  Object.assign(profile, snapshot.profile || {});
  products.value = snapshot.products || [];
  inspections.value = snapshot.inspections || [];
  rectifications.value = snapshot.rectifications || [];
});

function statusLabel(status) {
  return {
    ONGOING: "整改中",
    SUBMITTED: "待复核",
    REWORK: "打回重做",
    CONFIRMED: "已确认"
  }[status] || status || "待处理";
}

function statusTone(status) {
  return {
    ONGOING: "warning",
    SUBMITTED: "primary",
    REWORK: "danger",
    CONFIRMED: "success"
  }[status] || "neutral";
}
</script>
