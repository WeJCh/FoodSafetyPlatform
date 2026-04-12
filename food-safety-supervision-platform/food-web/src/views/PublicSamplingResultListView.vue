<template>
  <div class="public-shell">
    <header class="public-topbar">
      <div class="brand">
        <span class="brand-mark">食安</span>
        <div>
          <strong>抽检结果公示</strong>
          <span>查看已对外发布的食品抽检结果</span>
        </div>
      </div>
      <div class="user-area">
        <button class="ghost" type="button" @click="goBack">返回首页</button>
        <button class="ghost" type="button" @click="handleLogout">退出登录</button>
      </div>
    </header>

    <section class="form-card">
      <h3>结果查询</h3>
      <form class="track-form" @submit.prevent="handleSearch">
        <label>
          企业名称
          <input v-model.trim="filters.enterpriseName" placeholder="输入企业名称关键词" />
        </label>
        <label>
          抽检结果
          <select v-model="filters.result">
            <option value="">全部</option>
            <option value="PASS">合格</option>
            <option value="FAIL">不合格</option>
          </select>
        </label>
        <button class="primary" type="submit" :disabled="loading">
          {{ loading ? "查询中..." : "查询" }}
        </button>
      </form>
    </section>

    <section class="result-card">
      <div class="result-header">
        <div>
          <strong>抽检结果列表</strong>
          <span>共 {{ total }} 条</span>
        </div>
      </div>

      <div class="list-table">
        <div class="list-row list-header">
          <span>企业</span>
          <span>产品</span>
          <span>抽检结果</span>
          <span>公示时间</span>
          <span>操作</span>
        </div>
        <div v-if="!records.length" class="list-empty">暂无已公示抽检结果</div>
        <div v-for="item in records" :key="item.id" class="list-row">
          <div class="primary-cell">
            <strong>{{ item.enterpriseName || "-" }}</strong>
            <span>{{ item.taskNo || "-" }}</span>
          </div>
          <div class="primary-cell">
            <strong>{{ item.productName || "-" }}</strong>
            <span>{{ item.productSpecification || "暂无规格" }}</span>
          </div>
          <span :class="['status-chip', `status-chip--${resultClass(item.result)}`]">
            {{ formatResult(item.result) }}
          </span>
          <span>{{ formatTime(item.publishedTime || item.updateTime) }}</span>
          <button class="ghost" type="button" @click="viewResult(item)">查看详情</button>
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
import { fetchPublicSamplingResults } from "../api/regulationOperation";
import { getActiveSession, performLogout } from "../session/authRuntime";
import { formatTime } from "../utils/formatters";

const router = useRouter();
const publicToken = getActiveSession()?.token || "";
const filters = reactive({ enterpriseName: "", result: "" });
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

function formatResult(value) {
  if (value === "FAIL") return "不合格";
  if (value === "PASS") return "合格";
  return "-";
}

function resultClass(value) {
  return value === "FAIL" ? "warning" : "normal";
}

async function loadResults() {
  loading.value = true;
  setStatus("");
  try {
    const data = await fetchPublicSamplingResults(publicToken, {
      enterpriseName: filters.enterpriseName,
      result: filters.result,
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
    setStatus(error.message || "加载抽检结果失败", "error");
  } finally {
    loading.value = false;
  }
}

function handleSearch() {
  page.value = 1;
  loadResults();
}

function changePage(nextPage) {
  page.value = nextPage;
  loadResults();
}

function goBack() {
  router.push({ name: "public-home" }).catch(() => {});
}

function viewResult(item) {
  if (!item?.id) {
    return;
  }
  router.push({
    name: "public-sampling-result-detail",
    params: { samplingResultId: item.id }
  }).catch(() => {});
}

onMounted(loadResults);
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
  min-width: 220px;
  font-size: 13px;
  color: var(--muted);
}

.track-form input,
.track-form select {
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
}

.result-header span {
  color: var(--muted);
  font-size: 13px;
}

.list-table {
  display: grid;
  gap: 10px;
}

.list-row {
  display: grid;
  grid-template-columns: 1.4fr 1.3fr 0.8fr 1fr 0.8fr;
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
  min-height: 30px;
  align-items: center;
  justify-content: center;
  padding: 0 10px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 600;
  border: 1px solid transparent;
}

.status-chip--normal {
  color: #1f6b4d;
  background: #ebf9f1;
  border-color: #c6e9d6;
}

.status-chip--warning {
  color: #9b3a0a;
  background: #fff4eb;
  border-color: #f8d5bf;
}

@media (max-width: 900px) {
  .public-shell {
    padding: 22px 20px 36px;
  }

  .list-row {
    grid-template-columns: 1fr;
  }
}
</style>
