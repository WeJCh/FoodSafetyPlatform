<template>
  <section class="enterprise-page enterprise-success-page" v-if="detail">
    <article class="enterprise-card success-scene">
      <div class="success-scene__badge">
        <span class="material-symbols-outlined">check_circle</span>
      </div>
      <p class="section-kicker">Status: Submitted</p>
      <h2>提交成功</h2>
      <p class="success-scene__copy">
        您的整改说明已提交，当前状态：<strong>已提交（SUBMITTED）</strong>。
        监管人员将在 24 小时内进行复核，请留意系统通知。
      </p>

      <div class="success-audit-card">
        <div>
          <small>Task Identification</small>
          <strong>{{ detail.taskNo }}</strong>
        </div>
        <div>
          <small>Target Area</small>
          <strong>{{ detail.focusArea }}</strong>
        </div>
        <div>
          <small>Risk Category</small>
          <strong>{{ detail.riskLevel }}</strong>
        </div>
      </div>

      <div class="hero-actions hero-actions--centered">
        <RouterLink class="enterprise-primary-button" :to="{ name: 'enterprise-rectification-detail', params: { id: detail.id } }">查看整改详情</RouterLink>
        <RouterLink class="enterprise-ghost-button" :to="{ name: 'enterprise-rectifications' }">返回任务列表</RouterLink>
      </div>
    </article>
  </section>
</template>

<script setup>
import { onMounted, ref } from "vue";
import { useRoute } from "vue-router";
import { loadRectificationBundle } from "../../services/enterpriseGateway";
import { getStoredSession } from "../../session/authSession";

const route = useRoute();
const detail = ref(null);

onMounted(async () => {
  const session = getStoredSession();
  const bundle = await loadRectificationBundle(session?.token || "", route.params.id);
  detail.value = bundle.detail;
});
</script>
