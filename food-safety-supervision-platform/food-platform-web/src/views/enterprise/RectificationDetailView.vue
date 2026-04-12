<template>
  <section class="enterprise-page" v-if="detail">
    <nav class="enterprise-breadcrumbs">
      <RouterLink :to="{ name: 'enterprise-rectifications' }">整改任务</RouterLink>
      <span class="material-symbols-outlined">chevron_right</span>
      <span>任务详情</span>
    </nav>

    <div class="enterprise-grid enterprise-grid--detail">
      <article class="enterprise-card enterprise-card--summary">
        <div class="section-head">
          <h3>{{ detail.title }}</h3>
          <span :class="['enterprise-chip', `enterprise-chip--${statusTone(detail.status)}`]">{{ statusLabel(detail.status) }}</span>
        </div>
        <dl class="data-pairs">
          <div><dt>任务编号</dt><dd>{{ detail.taskNo }}</dd></div>
          <div><dt>风险等级</dt><dd>{{ detail.riskLevel }}</dd></div>
          <div><dt>重点区域</dt><dd>{{ detail.focusArea }}</dd></div>
          <div><dt>整改时限</dt><dd>{{ detail.dueDate }}</dd></div>
          <div><dt>任务说明</dt><dd>{{ detail.rectificationDesc }}</dd></div>
          <div><dt>进展摘要</dt><dd>{{ detail.progressSummary }}</dd></div>
        </dl>
        <div class="hero-actions">
          <RouterLink v-if="detail.status === 'ONGOING' || detail.status === 'REWORK'" class="enterprise-primary-button" :to="{ name: 'enterprise-rectification-submit', params: { id: detail.id } }">提交整改内容</RouterLink>
          <RouterLink class="enterprise-ghost-button" :to="{ name: 'enterprise-rectifications' }">返回任务列表</RouterLink>
        </div>
      </article>

      <aside class="enterprise-card">
        <div class="section-head">
          <h3>审计轨迹</h3>
          <span>{{ actions.length }} 条</span>
        </div>
        <div class="timeline-list">
          <div v-for="action in actions" :key="action.id" class="timeline-item">
            <strong>{{ action.actionTime }}</strong>
            <p>{{ action.actorName }} · {{ action.comment }}</p>
          </div>
        </div>
      </aside>
    </div>
  </section>
</template>

<script setup>
import { onMounted, ref } from "vue";
import { useRoute } from "vue-router";
import { loadRectificationBundle } from "../../services/enterpriseGateway";
import { getStoredSession } from "../../session/authSession";

const route = useRoute();
const detail = ref(null);
const actions = ref([]);

onMounted(async () => {
  const session = getStoredSession();
  const bundle = await loadRectificationBundle(session?.token || "", route.params.id);
  detail.value = bundle.detail;
  actions.value = bundle.actions || [];
});

function statusLabel(status) {
  return {
    ONGOING: "整改中",
    SUBMITTED: "待复核",
    REWORK: "打回重做",
    CONFIRMED: "已确认"
  }[status] || status;
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
