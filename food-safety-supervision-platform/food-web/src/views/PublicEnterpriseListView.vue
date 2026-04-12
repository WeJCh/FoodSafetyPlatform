<template>
  <div class="public-shell">
    <header class="public-topbar">
      <div class="brand">
        <span class="brand-mark">食安</span>
        <div>
          <strong>企业公示</strong>
          <span>查看已审核通过企业的备案信息</span>
        </div>
      </div>
      <div class="user-area">
        <button class="ghost" type="button" @click="goBack">返回首页</button>
        <button class="ghost" type="button" @click="handleLogout">退出登录</button>
      </div>
    </header>

    <section class="form-card">
      <h3>企业查询</h3>
      <form class="track-form" @submit.prevent="handleSearch">
        <label>
          企业名称
          <input v-model.trim="filters.enterpriseName" placeholder="输入企业名称关键词" />
        </label>
        <button class="primary" type="submit" :disabled="loading">
          {{ loading ? "查询中..." : "查询" }}
        </button>
      </form>
    </section>

    <section class="result-card">
      <div class="result-header">
        <div>
          <strong>公示企业列表</strong>
          <span>共 {{ total }} 家</span>
        </div>
      </div>

      <div class="list-table">
        <div class="list-row list-header enterprise-header">
          <span>企业名称</span>
          <span>所属区域</span>
          <span>当前状态</span>
          <span>更新时间</span>
          <span>操作</span>
        </div>
        <div v-if="!records.length" class="list-empty">暂无公示企业</div>
        <div v-for="item in records" :key="item.id" class="list-row enterprise-row">
          <div class="primary-cell">
            <strong>{{ item.enterpriseName || "-" }}</strong>
            <span>{{ item.addressDetail || "-" }}</span>
          </div>
          <span>{{ item.regionPathText || "-" }}</span>
          <span :class="['status-chip', `status-chip--${statusClass(item.status)}`]">
            {{ formatStatus(item.status) }}
          </span>
          <span>{{ formatTime(item.updateTime) }}</span>
          <button class="ghost" type="button" @click="viewEnterprise(item)">查看详情</button>
        </div>
      </div>

      <div class="pager">
        <span>第 {{ page }} / {{ pages }} 页</span>
        <div class="pager-actions">
          <button class="ghost" type="button" :disabled="page <= 1" @click="changePage(page - 1)">上一页</button>
          <button class="ghost" type="button" :disabled="page >= pages" @click="changePage(page + 1)">下一页</button>
        </div>
      </div>
    </section>

    <div class="status" :class="status.type" v-if="status.message">{{ status.message }}</div>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from "vue";
import { useRouter } from "vue-router";
import { fetchPublicEnterprises } from "../api/regulation";
import { getActiveSession, performLogout } from "../session/authRuntime";
import { formatTime } from "../utils/formatters";

const router = useRouter();
const publicToken = getActiveSession()?.token || "";
const filters = reactive({ enterpriseName: "" });
const loading = ref(false);
const records = ref([]);
const page = ref(1);
const size = ref(8);
const total = ref(0);
const pages = ref(1);
const status = reactive({ message: "", type: "" });

async function handleLogout() {
  await performLogout();
  router.replace({ name: "login" }).catch(() => {});
}

function setStatus(message, type = "info") {
  status.message = message;
  status.type = type;
}

function formatStatus(value) {
  const map = {
    NORMAL: "正常监管",
    KEY: "重点监管"
  };
  return map[value] || value || "-";
}

function statusClass(value) {
  return value === "KEY" ? "warning" : "normal";
}

async function loadEnterprises() {
  loading.value = true;
  setStatus("");
  try {
    const data = await fetchPublicEnterprises(publicToken, {
      enterpriseName: filters.enterpriseName,
      page: page.value,
      size: size.value
    });
    records.value = data.records || [];
    total.value = data.total || 0;
    page.value = data.page || 1;
    size.value = data.size || size.value;
    pages.value = data.pages || 1;
  } catch (error) {
    records.value = [];
    setStatus(error.message || "加载公示企业失败", "error");
  } finally {
    loading.value = false;
  }
}

function handleSearch() {
  page.value = 1;
  loadEnterprises();
}

function changePage(next) {
  page.value = next;
  loadEnterprises();
}

function goBack() {
  router.push({ name: "public-home" }).catch(() => {});
}

function viewEnterprise(item) {
  if (!item?.id) {
    return;
  }
  router.push({
    name: "public-enterprise-detail",
    params: { enterpriseId: item.id }
  }).catch(() => {});
}

onMounted(loadEnterprises);
</script>

<style scoped>
.public-shell {
  min-height: 100vh;
  padding: 28px 48px 46px;
  background:
    radial-gradient(circle at 10% 10%, rgba(13, 94, 166, 0.16), transparent 45%),
    radial-gradient(circle at 85% 18%, rgba(15, 139, 141, 0.12), transparent 40%),
    var(--bg);
  display: grid;
  gap: 20px;
}

.public-topbar,
.form-card,
.result-card {
  border-radius: 16px;
  border: 1px solid var(--stroke);
  background: #ffffff;
  box-shadow: var(--shadow);
}

.public-topbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 14px 18px;
}

.brand {
  display: flex;
  align-items: center;
  gap: 12px;
}

.brand-mark {
  width: 40px;
  height: 40px;
  border-radius: 12px;
  display: grid;
  place-items: center;
  background: var(--nav);
  color: #fff;
  font-weight: 700;
  letter-spacing: 0.08em;
}

.brand strong {
  display: block;
  font-size: 15px;
}

.brand span {
  font-size: 12px;
  color: var(--muted);
}

.user-area {
  display: flex;
  align-items: center;
  gap: 12px;
}

.form-card,
.result-card {
  padding: 22px 24px;
}

.form-card h3,
.result-header strong {
  margin: 0;
}

.track-form {
  margin-top: 16px;
  display: flex;
  gap: 14px;
  align-items: flex-end;
  flex-wrap: wrap;
}

.track-form label {
  display: grid;
  gap: 8px;
  min-width: 280px;
  font-size: 13px;
  color: var(--muted);
}

.track-form input {
  height: 42px;
  border: 1px solid var(--stroke);
  border-radius: 12px;
  padding: 0 14px;
  font: inherit;
  color: var(--text);
  background: #fff;
}

.result-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 14px;
}

.result-header span {
  display: block;
  margin-top: 4px;
  font-size: 12px;
  color: var(--muted);
}

.list-table {
  display: grid;
  gap: 10px;
}

.list-row {
  display: grid;
  grid-template-columns: 1.5fr 1.2fr 0.8fr 0.9fr 0.7fr;
  gap: 12px;
  align-items: center;
  padding: 14px 16px;
  border-radius: 14px;
  background: #f8fbff;
}

.list-header {
  background: #f1f6fd;
  color: var(--muted);
  font-size: 12px;
  font-weight: 600;
}

.list-empty {
  padding: 26px 12px;
  text-align: center;
  color: var(--muted);
}

.primary-cell {
  display: grid;
  gap: 4px;
}

.primary-cell strong {
  font-size: 14px;
}

.primary-cell span {
  font-size: 12px;
  color: var(--muted);
}

.status-chip {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: 30px;
  padding: 0 10px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 600;
}

.status-chip--normal {
  background: rgba(33, 156, 84, 0.12);
  color: #1f6e45;
}

.status-chip--warning {
  background: rgba(210, 122, 0, 0.14);
  color: #9b5b00;
}

.pager {
  margin-top: 16px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
}

.pager span {
  font-size: 12px;
  color: var(--muted);
}

.pager-actions {
  display: flex;
  gap: 10px;
}

.status.error {
  background: rgba(190, 61, 61, 0.12);
  color: #9f2d2d;
}

@media (max-width: 980px) {
  .public-shell {
    padding: 20px;
  }

  .public-topbar,
  .pager {
    flex-direction: column;
    align-items: stretch;
  }

  .list-row {
    grid-template-columns: 1fr;
  }
}
</style>
