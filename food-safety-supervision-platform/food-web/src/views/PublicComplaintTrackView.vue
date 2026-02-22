<template>
  <div class="public-shell">
    <header class="public-topbar">
      <div class="brand">
        <span class="brand-mark">食安</span>
        <div>
          <strong>我的投诉</strong>
          <span>查看我提交的全部投诉进度</span>
        </div>
      </div>
      <div class="user-area">
        <button class="ghost" type="button" @click="$emit('back')">返回首页</button>
        <button class="ghost" type="button" @click="$emit('logout')">退出登录</button>
      </div>
    </header>

    <section class="form-card">
      <h3>投诉查询</h3>
      <form class="track-form" @submit.prevent="handleSearch">
        <label>
          状态筛选
          <select v-model="filters.status">
            <option value="">全部</option>
            <option value="SUBMITTED">已提交</option>
            <option value="PENDING">已受理</option>
            <option value="ASSIGNED">已派发</option>
            <option value="PROCESSING">处理中</option>
            <option value="FEEDBACKED">已反馈</option>
            <option value="REJECTED">已驳回</option>
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
          <strong>投诉列表</strong>
          <span>共 {{ total }} 条</span>
        </div>
      </div>

      <div class="list-table">
        <div class="list-row list-header">
          <span>投诉号</span>
          <span>企业</span>
          <span>状态</span>
          <span>提交时间</span>
          <span>更新时间</span>
          <span>操作</span>
        </div>
        <div v-if="!records.length" class="list-empty">暂无投诉记录</div>
        <div v-for="item in records" :key="item.id" class="list-row">
          <span>{{ item.complaintNo || "-" }}</span>
          <span>{{ item.enterpriseName || "-" }}</span>
          <span>{{ formatStatus(item.status) }}</span>
          <span>{{ formatTime(item.createTime) }}</span>
          <span>{{ formatTime(item.updateTime) }}</span>
          <button class="ghost" type="button" @click="selectComplaint(item)">查看详情</button>
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

    <div v-if="detailOpen && selected" class="modal-mask" @click.self="closeDetail">
      <div class="modal-card">
        <div class="modal-header">
          <strong>投诉详情</strong>
          <span class="status-pill">{{ formatStatus(selected.status) }}</span>
        </div>
        <div class="detail-grid">
          <div>
            <span>投诉编号</span>
            <strong>{{ selected.complaintNo || "-" }}</strong>
          </div>
          <div>
            <span>企业名称</span>
            <strong>{{ selected.enterpriseName || "-" }}</strong>
          </div>
          <div>
            <span>投诉类型</span>
            <strong>{{ selected.complaintType || "-" }}</strong>
          </div>
          <div>
            <span>提交时间</span>
            <strong>{{ formatTime(selected.createTime) }}</strong>
          </div>
        </div>
        <div class="detail-content">
          <span>投诉内容</span>
          <p>{{ selected.content || "-" }}</p>
        </div>
        <div v-if="selected.status === 'FEEDBACKED'" class="result-tip">
          <p>处理结果：{{ selected.handleResult || "暂无处理结果" }}</p>
        </div>
        <div v-if="selected.status === 'REJECTED'" class="rejected-tip">
          <p>该投诉已驳回。</p>
          <p v-if="selected.handleResult">驳回原因：{{ selected.handleResult }}</p>
          <p v-else>可能原因：信息不完整或不属于食品安全投诉。</p>
          <p>如需继续反馈，请补充材料后重新提交，或联系监管部门咨询。</p>
        </div>
        <div class="modal-actions">
          <button class="ghost" type="button" @click="closeDetail">关闭</button>
        </div>
      </div>
    </div>

    <div class="status" :class="status.type" v-if="status.message">{{ status.message }}</div>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from "vue";
import { fetchMyComplaints } from "../api/regulation";

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

defineEmits(["back", "logout"]);

const filters = reactive({ status: "" });
const loading = ref(false);
const records = ref([]);
const page = ref(1);
const size = ref(8);
const total = ref(0);
const pages = ref(1);
const selected = ref(null);
const detailOpen = ref(false);
const status = reactive({ message: "", type: "" });

function setStatus(message, type = "info") {
  status.message = message;
  status.type = type;
}

function formatStatus(value) {
  const map = {
    SUBMITTED: "已提交",
    PENDING: "已受理",
    ASSIGNED: "已派发",
    PROCESSING: "处理中",
    FEEDBACKED: "已反馈",
    REJECTED: "已驳回（无效投诉）"
  };
  return map[value] || value || "-";
}

function formatTime(value) {
  if (!value) return "-";
  return String(value).replace("T", " ").slice(0, 16);
}

function selectComplaint(item) {
  selected.value = item;
  detailOpen.value = true;
}

function closeDetail() {
  detailOpen.value = false;
}

async function loadComplaints() {
  loading.value = true;
  setStatus("");
  try {
    const data = await fetchMyComplaints(props.publicToken, {
      status: filters.status,
      page: page.value,
      size: size.value
    });
    records.value = data.records || [];
    total.value = data.total || 0;
    page.value = data.page || 1;
    size.value = data.size || size.value;
    pages.value = data.pages || 1;
    selected.value = records.value[0] || null;
    detailOpen.value = false;
  } catch (error) {
    records.value = [];
    selected.value = null;
    setStatus(error.message || "加载投诉列表失败", "error");
  } finally {
    loading.value = false;
  }
}

function handleSearch() {
  page.value = 1;
  loadComplaints();
}

function changePage(next) {
  page.value = next;
  loadComplaints();
}

onMounted(loadComplaints);
</script>

<style scoped>
.public-shell {
  min-height: 100vh;
  padding: 28px 48px 46px;
  background: var(--bg);
  display: grid;
  gap: 20px;
}

.public-topbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 14px 18px;
  border-radius: 16px;
  border: 1px solid var(--stroke);
  background: #ffffff;
  box-shadow: var(--shadow);
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

.user-area {
  display: flex;
  gap: 12px;
}

.form-card,
.result-card {
  background: #ffffff;
  border: 1px solid var(--stroke);
  border-radius: 16px;
  padding: 18px 20px;
  box-shadow: var(--shadow);
}

.track-form {
  display: grid;
  gap: 14px;
}

.result-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
}

.result-header span {
  color: var(--muted);
  font-size: 12px;
}

.status-pill {
  padding: 4px 10px;
  border-radius: 999px;
  background: #e9f1f8;
  color: var(--primary);
  font-size: 12px;
  font-weight: 600;
}

.list-table {
  margin-top: 16px;
  border: 1px solid var(--stroke);
  border-radius: 14px;
  overflow: hidden;
  background: #ffffff;
}

.list-row {
  display: grid;
  grid-template-columns:
    minmax(220px, 1.6fr)
    minmax(120px, 1.1fr)
    minmax(96px, 0.9fr)
    minmax(140px, 1fr)
    minmax(140px, 1fr)
    minmax(120px, 0.8fr);
  align-items: center;
  gap: 10px;
  padding: 12px 16px;
  border-bottom: 1px solid var(--stroke);
  font-size: 13px;
}

.list-header {
  color: #395066;
  font-weight: 600;
  background: #eef4fb;
  border-bottom: 1px solid #d9e6f4;
}

.list-empty {
  padding: 18px 0;
  color: var(--muted);
  text-align: center;
}

.list-row:not(.list-header) {
  background: #f9fcff;
}

.list-row:not(.list-header):hover {
  background: #f1f7ff;
}

.list-row .ghost {
  justify-self: end;
  padding: 6px 14px;
  border-radius: 999px;
  background: #eef4fb;
  border: 1px solid #d9e6f4;
  color: #2b4a66;
}

.pager {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 12px;
  color: var(--muted);
  font-size: 12px;
}

.pager-actions {
  display: flex;
  gap: 10px;
}

.detail-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.detail-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px 18px;
  font-size: 13px;
}

.detail-grid span {
  display: block;
  color: var(--muted);
  font-size: 12px;
  margin-bottom: 6px;
}

.detail-content {
  margin-top: 14px;
}

.detail-content span {
  display: block;
  color: var(--muted);
  font-size: 12px;
  margin-bottom: 6px;
}

.detail-content p {
  margin: 0;
  line-height: 1.6;
}

.result-tip {
  margin-top: 14px;
  padding: 12px 14px;
  border-radius: 12px;
  background: rgba(21, 101, 192, 0.08);
  color: #1f4f7a;
  border: 1px solid rgba(21, 101, 192, 0.2);
  font-size: 13px;
  line-height: 1.6;
}

.result-tip p {
  margin: 0;
}

.rejected-tip {
  margin-top: 14px;
  padding: 12px 14px;
  border-radius: 12px;
  background: rgba(234, 68, 53, 0.08);
  color: #8a3b34;
  border: 1px solid rgba(234, 68, 53, 0.2);
  font-size: 13px;
  line-height: 1.6;
}

.rejected-tip p {
  margin: 0;
}

.rejected-tip p + p {
  margin-top: 6px;
}

.modal-mask {
  position: fixed;
  inset: 0;
  background: rgba(15, 23, 42, 0.55);
  display: grid;
  place-items: center;
  z-index: 9999;
  padding: 18px;
}

.modal-card {
  width: min(720px, 92vw);
  max-height: 88vh;
  overflow: auto;
  background: #ffffff;
  border-radius: 16px;
  border: 1px solid var(--stroke);
  padding: 18px 20px;
  box-shadow: 0 24px 60px rgba(15, 23, 42, 0.25);
  display: grid;
  gap: 12px;
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.modal-actions {
  display: flex;
  justify-content: flex-end;
}

@media (max-width: 1000px) {
  .public-shell {
    padding: 20px 18px 36px;
  }

  .list-row {
    grid-template-columns: 1fr;
    gap: 6px;
  }

  .list-row .ghost {
    justify-self: flex-start;
  }
}
</style>
