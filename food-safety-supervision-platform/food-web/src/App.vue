<template>
  <AuthView
    v-if="view === 'auth'"
    @admin-login="handleAdminLogin"
    @enterprise-login="handleEnterpriseLogin"
  />
  <AdminView
    v-else-if="view === 'admin'"
    :admin-user="adminUser"
    :token="adminToken"
    @logout="handleLogout"
  />
  <EnterpriseProfileView
    v-else
    :enterprise-user="enterpriseUser"
    :token="enterpriseToken"
    @logout="handleLogout"
  />
</template>

<script setup>
import { reactive, ref } from "vue";
import AdminView from "./views/AdminView.vue";
import AuthView from "./views/AuthView.vue";
import EnterpriseProfileView from "./views/EnterpriseProfileView.vue";

const view = ref("auth");
const adminToken = ref("");
const adminUser = reactive({ username: "", userType: "" });
const enterpriseToken = ref("");
const enterpriseUser = reactive({ username: "", userType: "" });

function handleAdminLogin(payload) {
  adminToken.value = payload.token;
  adminUser.username = payload.username;
  adminUser.userType = payload.userType;
  view.value = "admin";
}

function handleEnterpriseLogin(payload) {
  enterpriseToken.value = payload.token;
  enterpriseUser.username = payload.username;
  enterpriseUser.userType = payload.userType;
  view.value = "enterprise";
}

function handleLogout() {
  adminToken.value = "";
  adminUser.username = "";
  adminUser.userType = "";
  enterpriseToken.value = "";
  enterpriseUser.username = "";
  enterpriseUser.userType = "";
  view.value = "auth";
}
</script>
