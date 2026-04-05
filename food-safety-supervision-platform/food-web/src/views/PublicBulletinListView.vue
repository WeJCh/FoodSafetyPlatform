<template>
  <div class="public-shell">
    <header class="public-topbar">
      <div class="brand">
        <span class="brand-mark">食安</span>
        <div>
          <strong>监管公告</strong>
          <span>查看监管部门对外发布的食品安全公告</span>
        </div>
      </div>
      <div class="user-area">
        <button class="ghost" type="button" @click="$emit('back')">返回首页</button>
        <button class="ghost" type="button" @click="$emit('logout')">退出登录</button>
      </div>
    </header>

    <section class="form-card">
      <h3>公告查询</h3>
      <form class="track-form" @submit.prevent="handleSearch">
        <label>
          关键词
          <input v-model.trim="filters.keyword" placeholder="输入公告标题或摘要关键词" />
        </label>
        <button class="primary" type="submit" :disabled="loading">
          {{ loading ? "查询中..." : "查询" }}
        </button>
      </form>
    </section>

    <section class="result-card">
      <div class="result-header">
        <div>
          <strong>公告列表</strong>
          <span>共 {{ total }} 条</span>
        </div>
      </div>

      <div class="bulletin-list">
        <article v-if="!records.length" class="bulletin-empty">暂无已发布公告</article>
        <article v-for="item in records" :key="item.id" class="bulletin-item">
          <div class="bulletin-meta">
            <span>{{ formatTime(item.publishedTime) }}</span>
            <span>{{ item.publishedByName || "监管部门" }}</span>
          </div>
          <h3>{{ item.title || "-" }}</h3>
          <p>{{ item.summary || "暂无摘要" }}</p>
          <div class="bulletin-actions">
            <button class="ghost" type="button" @click="$emit('view-bulletin', item)">查看详情</button>
          </div>
        </article>
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
import { fetchPublicBulletins } from "../api/regulation";

const props = defineProps({
  publicUser: {
    type: Object,
    required: true
  },
  publicToken: {
    type: String,
    required: true
  }
});

defineEmits(["back", "logout", "view-bulletin"]);

const filters = reactive({ keyword: "" });
const loading = ref(false);
const records = ref([]);
const page = ref(1);
const size = ref(8);
const total = ref(0);
const pages = ref(1);
const status = reactive({ message: "", type: "" });

function setStatus(message, type = "info") {
  status.message = message;
  status.type = type;
}

function formatTime(value) {
  if (!value) return "-";
  return String(value).replace("T", " ").slice(0, 16);
}

async function loadBulletins() {
  loading.value = true;
  setStatus("");
  try {
    const data = await fetchPublicBulletins(props.publicToken, {
      keyword: filters.keyword,
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
    setStatus(error.message || "加载公告列表失败", "error");
  } finally {
    loading.value = false;
  }
}

function handleSearch() {
  page.value = 1;
  loadBulletins();
}

function changePage(nextPage) {
  page.value = nextPage;
  loadBulletins();
}

onMounted(loadBulletins);
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
}

.result-header span {
  color: var(--muted);
  font-size: 13px;
}

.bulletin-list {
  margin-top: 18px;
  display: grid;
  gap: 14px;
}

.bulletin-item,
.bulletin-empty {
  border-radius: 16px;
  border: 1px solid var(--stroke);
  background: var(--card-strong);
  padding: 18px 20px;
}

.bulletin-item h3 {
  margin: 10px 0 8px;
  font-size: 18px;
}

.bulletin-item p {
  margin: 0;
  color: var(--muted);
  line-height: 1.7;
}

.bulletin-meta {
  display: flex;
  gap: 14px;
  flex-wrap: wrap;
  color: var(--muted);
  font-size: 12px;
}

.bulletin-actions {
  margin-top: 16px;
}
</style>
