<template>
  <div class="app-shell">
    <div class="hero-panel">
      <div class="hero-content">
        <span class="badge">监管工作台</span>
        <h1>监管企业列表</h1>
        <p>
          查看企业备案信息与审核状态，支持按状态快速筛选。更多监管任务将在后续模块展开。
        </p>
        <div class="hero-highlights">
          <div>
            <strong>企业档案</strong>
            <span>统一查看备案信息</span>
          </div>
          <div>
            <strong>审核状态</strong>
            <span>跟踪企业备案进度</span>
          </div>
          <div>
            <strong>监管入口</strong>
            <span>后续衔接检查与投诉</span>
          </div>
        </div>
      </div>
    </div>

    <div class="form-panel">
      <div class="card">
        <div class="section-title">监管人员</div>
        <div class="admin-info">
          <div>账号：{{ regulatorUser.username }}</div>
          <div>角色：{{ regulatorUser.userType }}</div>
        </div>

        <form class="filter-bar" @submit.prevent="handleSearch">
          <label>
            企业名称
            <input v-model.trim="filters.enterpriseName" placeholder="输入企业名称" />
          </label>
          <label>
            企业状态
            <select v-model="filters.status">
              <option value="">全部</option>
              <option value="NORMAL">正常</option>
              <option value="KEY">重点监管</option>
            </select>
          </label>
          <label>
            审核状态
            <select v-model="filters.approvalStatus">
              <option value="">全部</option>
              <option value="PENDING">待审核</option>
              <option value="APPROVED">已通过</option>
              <option value="REJECTED">已驳回</option>
            </select>
          </label>
          <button class="primary" type="submit" :disabled="loading">
            {{ loading ? "查询中..." : "查询" }}
          </button>
        </form>

        <div class="list-table">
          <div class="list-row list-header">
            <span>企业名称</span>
            <span>状态</span>
            <span>审核</span>
            <span>负责人</span>
            <span>更新时间</span>
          </div>
          <div v-if="!records.length" class="list-empty">
            暂无企业数据
          </div>
          <div v-for="item in records" :key="item.id" class="list-row">
            <span>{{ item.enterpriseName }}</span>
            <span>{{ item.status }}</span>
            <span>{{ item.approvalStatus }}</span>
            <span>{{ item.principal || "-" }}</span>
            <span>{{ formatTime(item.updateTime) }}</span>
          </div>
        </div>

        <div class="pager">
          <span>共 {{ total }} 条，{{ page }}/{{ pages }} 页</span>
          <div class="pager-actions">
            <button class="ghost" type="button" :disabled="page <= 1" @click="changePage(page - 1)">
              上一页
            </button>
            <button class="ghost" type="button" :disabled="page >= pages" @click="changePage(page + 1)">
              下一页
            </button>
          </div>
        </div>

        <button class="ghost" type="button" @click="handleLogout">退出登录</button>

        <div class="status" :class="status.type" v-if="status.message">
          {{ status.message }}
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from "vue";
import { fetchEnterprises } from "../api/regulation";

const props = defineProps({
  token: {
    type: String,
    required: true
  },
  regulatorUser: {
    type: Object,
    required: true
  }
});

const emit = defineEmits(["logout"]);

const filters = reactive({
  enterpriseName: "",
  status: "",
  approvalStatus: ""
});

const status = reactive({ message: "", type: "" });
const loading = ref(false);
const records = ref([]);
const page = ref(1);
const size = ref(8);
const total = ref(0);
const pages = ref(1);

function setStatus(message, type = "info") {
  status.message = message;
  status.type = type;
}

async function load() {
  loading.value = true;
  setStatus("");
  try {
    const data = await fetchEnterprises(props.token, {
      ...filters,
      page: page.value,
      size: size.value
    });
    records.value = data.records || [];
    total.value = data.total || 0;
    page.value = data.page || 1;
    size.value = data.size || size.value;
    pages.value = data.pages || 1;
  } catch (error) {
    setStatus(error.message || "加载企业列表失败", "error");
  } finally {
    loading.value = false;
  }
}

async function handleSearch() {
  page.value = 1;
  await load();
}

async function changePage(nextPage) {
  page.value = nextPage;
  await load();
}

function handleLogout() {
  emit("logout");
}

function formatTime(value) {
  if (!value) return "-";
  return String(value).replace("T", " ").slice(0, 16);
}

onMounted(() => {
  load();
});
</script>

<style scoped>
.filter-bar {
  display: grid;
  gap: 12px;
  margin-bottom: 16px;
}

.list-table {
  border-radius: 14px;
  border: 1px solid var(--stroke);
  background: #faf6f1;
  overflow: hidden;
}

.list-row {
  display: grid;
  grid-template-columns: 1.6fr 1fr 1fr 1fr 1.4fr;
  gap: 8px;
  padding: 12px 14px;
  align-items: center;
  font-size: 13px;
}

.list-header {
  font-weight: 600;
  background: #f1e6db;
}

.list-empty {
  padding: 16px;
  color: var(--muted);
  font-size: 13px;
}

.pager {
  margin: 16px 0;
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 12px;
  color: var(--muted);
}

.pager-actions {
  display: flex;
  gap: 8px;
}
</style>
