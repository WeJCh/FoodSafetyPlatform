<template>
  <section class="bulletin-manager">
    <div class="bulletin-layout">
      <article class="bulletin-card bulletin-card--form">
        <div class="section-title">公告管理</div>
        <p class="section-tip">区域管理员可创建草稿、发布公告和下线历史公告，供公众端查看。</p>

        <form class="bulletin-form" @submit.prevent="handleSubmit">
          <label>
            公告标题
            <input v-model.trim="form.title" maxlength="120" placeholder="请输入公告标题" />
          </label>
          <label>
            公告摘要
            <input v-model.trim="form.summary" maxlength="255" placeholder="可选，不填时自动截取正文摘要" />
          </label>
          <label>
            公告正文
            <textarea
              v-model.trim="form.content"
              rows="10"
              maxlength="4000"
              placeholder="请输入公告正文，建议明确时间、范围和公众注意事项"
            ></textarea>
          </label>
          <div class="bulletin-form-actions">
            <button class="primary" type="submit" :disabled="saving">
              {{ saving ? "提交中..." : editingId ? "更新草稿" : "新建草稿" }}
            </button>
            <button class="ghost" type="button" :disabled="saving" @click="resetForm">清空</button>
          </div>
        </form>
      </article>

      <article class="bulletin-card">
        <div class="bulletin-toolbar">
          <div>
            <div class="section-title">公告列表</div>
            <p class="section-tip">只保留公告最小闭环，不扩展分类、置顶和附件。</p>
          </div>
          <form class="bulletin-filter" @submit.prevent="handleSearch">
            <label>
              关键词
              <input v-model.trim="filters.keyword" placeholder="标题或摘要" />
            </label>
            <label>
              状态
              <select v-model="filters.status">
                <option value="">全部</option>
                <option value="DRAFT">草稿</option>
                <option value="PUBLISHED">已发布</option>
                <option value="OFFLINE">已下线</option>
              </select>
            </label>
            <button class="ghost" type="submit" :disabled="loading">
              {{ loading ? "刷新中..." : "筛选" }}
            </button>
          </form>
        </div>

        <div class="list-table">
          <div class="list-row list-header bulletin-header">
            <span>标题</span>
            <span>状态</span>
            <span>发布时间</span>
            <span>发布人</span>
            <span>操作</span>
          </div>
          <div v-if="!records.length" class="list-empty">暂无公告</div>
          <div v-for="item in records" :key="item.id" class="list-row bulletin-row">
            <div class="bulletin-title-cell">
              <strong>{{ item.title || "-" }}</strong>
              <span>{{ item.summary || "未填写摘要" }}</span>
            </div>
            <span :class="['status-chip', `status-chip--${statusClass(item.status)}`]">
              {{ formatStatus(item.status) }}
            </span>
            <span>{{ formatTime(item.publishedTime) }}</span>
            <span>{{ item.publishedByName || "-" }}</span>
            <div class="action-buttons">
              <button class="ghost" type="button" :disabled="actionLoading" @click="handleEdit(item)">
                编辑
              </button>
              <button
                v-if="item.status !== 'PUBLISHED'"
                class="primary"
                type="button"
                :disabled="actionLoading"
                @click="handlePublish(item)"
              >
                发布
              </button>
              <button
                v-else
                class="ghost"
                type="button"
                :disabled="actionLoading"
                @click="handleOffline(item)"
              >
                下线
              </button>
            </div>
          </div>
        </div>

        <div class="pager">
          <span>共 {{ total }} 条，{{ page }}/{{ pages }} 页</span>
          <div class="pager-actions">
            <button class="ghost" type="button" :disabled="page <= 1 || loading" @click="changePage(page - 1)">
              上一页
            </button>
            <button class="ghost" type="button" :disabled="page >= pages || loading" @click="changePage(page + 1)">
              下一页
            </button>
          </div>
        </div>
      </article>
    </div>

    <div class="status" :class="status.type" v-if="status.message">{{ status.message }}</div>
  </section>
</template>

<script setup>
import { onMounted, reactive, ref } from "vue";
import {
  createBulletin,
  fetchBulletinDetail,
  fetchBulletins,
  offlineBulletin,
  publishBulletin,
  updateBulletin
} from "../api/regulation";

const props = defineProps({
  token: {
    type: String,
    required: true
  }
});

const filters = reactive({
  keyword: "",
  status: ""
});
const form = reactive({
  title: "",
  summary: "",
  content: ""
});
const status = reactive({
  message: "",
  type: ""
});
const loading = ref(false);
const saving = ref(false);
const actionLoading = ref(false);
const records = ref([]);
const page = ref(1);
const size = ref(8);
const total = ref(0);
const pages = ref(1);
const editingId = ref(null);

function setStatus(message, type = "info") {
  status.message = message;
  status.type = type;
}

function formatTime(value) {
  if (!value) return "-";
  return String(value).replace("T", " ").slice(0, 16);
}

function formatStatus(value) {
  const map = {
    DRAFT: "草稿",
    PUBLISHED: "已发布",
    OFFLINE: "已下线"
  };
  return map[value] || value || "-";
}

function statusClass(value) {
  if (value === "PUBLISHED") return "normal";
  if (value === "OFFLINE") return "warning";
  return "pending";
}

function resetForm() {
  editingId.value = null;
  form.title = "";
  form.summary = "";
  form.content = "";
}

async function loadBulletins() {
  loading.value = true;
  setStatus("");
  try {
    const data = await fetchBulletins(props.token, {
      keyword: filters.keyword,
      status: filters.status,
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

async function handleSearch() {
  page.value = 1;
  await loadBulletins();
}

async function changePage(nextPage) {
  page.value = nextPage;
  await loadBulletins();
}

async function handleEdit(item) {
  if (!item?.id) return;
  actionLoading.value = true;
  setStatus("");
  try {
    const detail = await fetchBulletinDetail(props.token, item.id);
    editingId.value = detail.id;
    form.title = detail.title || "";
    form.summary = detail.summary || "";
    form.content = detail.content || "";
    setStatus("已载入公告内容，可继续编辑", "info");
  } catch (error) {
    setStatus(error.message || "加载公告详情失败", "error");
  } finally {
    actionLoading.value = false;
  }
}

async function handleSubmit() {
  if (!form.title.trim()) {
    setStatus("公告标题不能为空", "error");
    return;
  }
  if (!form.content.trim()) {
    setStatus("公告正文不能为空", "error");
    return;
  }
  saving.value = true;
  setStatus("");
  const payload = {
    title: form.title,
    summary: form.summary,
    content: form.content
  };
  try {
    if (editingId.value) {
      await updateBulletin(props.token, editingId.value, payload);
      setStatus("公告草稿已更新", "success");
    } else {
      await createBulletin(props.token, payload);
      setStatus("公告草稿已创建", "success");
    }
    resetForm();
    await loadBulletins();
  } catch (error) {
    setStatus(error.message || "保存公告失败", "error");
  } finally {
    saving.value = false;
  }
}

async function handlePublish(item) {
  if (!item?.id) return;
  actionLoading.value = true;
  setStatus("");
  try {
    await publishBulletin(props.token, item.id);
    setStatus("公告已发布，公众端可见", "success");
    await loadBulletins();
  } catch (error) {
    setStatus(error.message || "发布公告失败", "error");
  } finally {
    actionLoading.value = false;
  }
}

async function handleOffline(item) {
  if (!item?.id) return;
  actionLoading.value = true;
  setStatus("");
  try {
    await offlineBulletin(props.token, item.id);
    setStatus("公告已下线", "success");
    await loadBulletins();
  } catch (error) {
    setStatus(error.message || "下线公告失败", "error");
  } finally {
    actionLoading.value = false;
  }
}

onMounted(loadBulletins);
</script>

<style scoped>
.bulletin-manager {
  display: grid;
  gap: 16px;
}

.bulletin-layout {
  display: grid;
  grid-template-columns: minmax(320px, 0.9fr) minmax(0, 1.3fr);
  gap: 18px;
  align-items: start;
}

.bulletin-card {
  border-radius: 16px;
  border: 1px solid var(--stroke);
  background: #fff;
  padding: 20px 22px;
}

.bulletin-card--form {
  background: linear-gradient(180deg, #f8fbff 0%, #eef5fc 100%);
}

.section-tip {
  margin: 8px 0 0;
  color: var(--muted);
  font-size: 13px;
  line-height: 1.6;
}

.bulletin-form,
.bulletin-filter {
  display: grid;
  gap: 14px;
}

.bulletin-form {
  margin-top: 18px;
}

.bulletin-form label,
.bulletin-filter label {
  display: grid;
  gap: 8px;
  font-size: 13px;
  color: var(--muted);
}

.bulletin-form input,
.bulletin-form textarea,
.bulletin-filter input,
.bulletin-filter select {
  width: 100%;
  border: 1px solid var(--stroke);
  border-radius: 12px;
  padding: 11px 14px;
  font: inherit;
  color: var(--ink);
  background: #fff;
}

.bulletin-form textarea {
  resize: vertical;
  min-height: 220px;
}

.bulletin-form-actions {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}

.bulletin-toolbar {
  display: flex;
  justify-content: space-between;
  gap: 18px;
  align-items: flex-start;
  margin-bottom: 16px;
}

.bulletin-filter {
  grid-template-columns: minmax(180px, 220px) 140px auto;
  align-items: end;
}

.bulletin-header,
.bulletin-row {
  --row-columns: 1.8fr 0.8fr 1fr 0.8fr 1fr;
}

.bulletin-title-cell {
  display: grid;
  gap: 6px;
}

.bulletin-title-cell strong {
  font-size: 14px;
}

.bulletin-title-cell span {
  font-size: 12px;
  color: var(--muted);
}

.status-chip {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: 34px;
  padding: 0 12px;
  border-radius: 999px;
  font-size: 13px;
  font-weight: 600;
}

.status-chip--pending {
  background: rgba(13, 94, 166, 0.12);
  color: #0d5ea6;
}

.status-chip--normal {
  background: rgba(33, 156, 84, 0.12);
  color: #1a7f5a;
}

.status-chip--warning {
  background: rgba(209, 122, 0, 0.12);
  color: #b56800;
}

@media (max-width: 1200px) {
  .bulletin-layout {
    grid-template-columns: 1fr;
  }

  .bulletin-filter {
    grid-template-columns: 1fr;
  }
}
</style>
