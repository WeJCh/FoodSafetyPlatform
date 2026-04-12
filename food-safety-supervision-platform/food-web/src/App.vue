<template>
  <RouterView />
</template>

<script setup>
import { onBeforeUnmount, onMounted } from "vue";
import { RouterView, useRoute, useRouter } from "vue-router";
import { UNAUTHORIZED_EVENT } from "./api/client";
import {
  dropResolvedSession,
  restoreResolvedSession
} from "./session/authRuntime";

const route = useRoute();
const router = useRouter();

function handleUnauthorized() {
  dropResolvedSession();

  if (!route.meta?.guestOnly) {
    router.replace({
      name: "login",
      query: {
        reason: "expired",
        redirect: route.fullPath
      }
    });
  }
}

onMounted(async () => {
  window.addEventListener(UNAUTHORIZED_EVENT, handleUnauthorized);
  await restoreResolvedSession();
});

onBeforeUnmount(() => {
  window.removeEventListener(UNAUTHORIZED_EVENT, handleUnauthorized);
});
</script>
