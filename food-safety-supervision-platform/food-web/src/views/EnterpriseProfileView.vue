<template>
  <div class="app-shell legacy-enterprise-page">
    <div class="hero-panel">
      <div class="hero-content">
        <span class="badge">旧版入口</span>
        <h1>企业端已切换到新版工作台</h1>
        <p>当前页面仅保留兼容跳转能力，企业资料、产品档案、检查记录和整改任务都已迁移到新的闭环页面。</p>
      </div>
    </div>

    <div class="form-panel">
      <div class="card legacy-enterprise-page__card">
        <div class="section-title">正在跳转</div>
        <p class="legacy-enterprise-page__desc">
          系统会自动跳转到对应的新页面。如果没有跳转，可以使用下方入口继续。
        </p>

        <div class="legacy-enterprise-page__actions">
          <RouterLink class="primary" :to="{ name: 'enterprise-dashboard' }">进入企业工作台</RouterLink>
          <RouterLink class="ghost" :to="{ name: 'enterprise-profile' }">进入企业备案</RouterLink>
          <RouterLink class="ghost" :to="{ name: 'enterprise-products' }">进入产品档案</RouterLink>
          <RouterLink class="ghost" :to="{ name: 'enterprise-rectifications' }">进入整改任务</RouterLink>
        </div>

        <div v-if="status.message" class="status" :class="status.type">
          {{ status.message }}
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, reactive } from "vue";
import { RouterLink, useRoute, useRouter } from "vue-router";

const router = useRouter();
const route = useRoute();
const status = reactive({ message: "", type: "" });

function resolveTargetRoute() {
  const section = String(route.query.section || route.query.tab || "").toLowerCase();
  if (section === "profile") return { name: "enterprise-profile" };
  if (section === "products") return { name: "enterprise-products" };
  if (section === "inspections") return { name: "enterprise-inspections" };
  if (section === "rectification" || section === "rectifications") return { name: "enterprise-rectifications" };
  return { name: "enterprise-dashboard" };
}

onMounted(async () => {
  try {
    await router.replace(resolveTargetRoute());
  } catch {
    status.message = "自动跳转失败，请使用下方入口继续。";
    status.type = "info";
  }
});
</script>

<style scoped>
.legacy-enterprise-page {
  grid-template-columns: 1fr;
}

.legacy-enterprise-page__card {
  max-width: 760px;
  width: 100%;
}

.legacy-enterprise-page__desc {
  margin: 0 0 18px;
  color: var(--muted);
  line-height: 1.7;
}

.legacy-enterprise-page__actions {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

@media (max-width: 900px) {
  .legacy-enterprise-page__actions {
    flex-direction: column;
  }
}
</style>
