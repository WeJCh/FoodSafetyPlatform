<template>
  <AuthView
    v-if="view === 'auth'"
    @admin-login="handleAdminLogin"
    @enterprise-login="handleEnterpriseLogin"
    @regulator-login="handleRegulatorLogin"
  />
  <AdminView
    v-else-if="view === 'admin'"
    :admin-user="adminUser"
    :token="adminToken"
    @logout="handleLogout"
  />
  <RegulatorView
    v-else-if="view === 'regulator'"
    :regulator-user="regulatorUser"
    :token="regulatorToken"
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
import RegulatorView from "./views/RegulatorView.vue";

const view = ref("auth");
const adminToken = ref("");
const adminUser = reactive({ username: "", userType: "" });
const enterpriseToken = ref("");
const enterpriseUser = reactive({ username: "", userType: "" });
const regulatorToken = ref("");
const regulatorUser = reactive({ username: "", userType: "" });

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

function handleRegulatorLogin(payload) {
  regulatorToken.value = payload.token;
  regulatorUser.username = payload.username;
  regulatorUser.userType = payload.userType;
  view.value = "regulator";
}

function handleLogout() {
  adminToken.value = "";
  adminUser.username = "";
  adminUser.userType = "";
  enterpriseToken.value = "";
  enterpriseUser.username = "";
  enterpriseUser.userType = "";
  regulatorToken.value = "";
  regulatorUser.username = "";
  regulatorUser.userType = "";
  view.value = "auth";
}
</script>
