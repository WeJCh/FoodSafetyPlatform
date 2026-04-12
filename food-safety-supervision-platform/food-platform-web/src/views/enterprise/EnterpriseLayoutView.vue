<template>
  <EnterpriseShell
    :title="route.meta.title || '企业工作台'"
    :subtitle="route.meta.subtitle || defaultSubtitle"
    :eyebrow="route.meta.eyebrow || defaultEyebrow"
    :username="username"
    @logout="handleLogout"
  >
    <RouterView />
  </EnterpriseShell>
</template>

<script setup>
import { computed } from "vue";
import { useRoute, useRouter } from "vue-router";
import EnterpriseShell from "../../components/enterprise/EnterpriseShell.vue";
import { getStoredSession } from "../../session/authSession";
import { performLogout } from "../../session/authRuntime";

const route = useRoute();
const router = useRouter();
const defaultSubtitle = "聚合备案维护、产品档案、检查记录与整改跟进。";
const defaultEyebrow = "Digital Ledger";

const username = computed(() => getStoredSession()?.username || "企业用户");

async function handleLogout() {
  await performLogout();
  router.replace({ name: "login" });
}
</script>