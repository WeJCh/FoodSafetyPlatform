<template>
  <RegulatorAdminWorkspacePage
    active-key="bulletins"
    :username="regulatorUser.username"
    @navigate="handleSidebarNavigate"
    @logout="handleLogout"
  >
    <section class="bulletin-page">
      <header class="page-head">
        <div>
          <p class="eyebrow">System Communication</p>
          <h1>公告管理列表</h1>
        </div>
        <div class="head-actions">
          <button class="ghost" type="button" @click="filterVisible = !filterVisible">
            {{ filterVisible ? "收起筛选" : "筛选条件" }}
          </button>
          <button class="primary" type="button" @click="openCreate">新建公告</button>
        </div>
      </header>

      <section class="stats-grid">
        <article class="stat-card stat-card--primary"><span>公告总数</span><strong>{{ total }}</strong></article>
        <article class="stat-card stat-card--green"><span>已发布</span><strong>{{ publishedCount }}</strong></article>
        <article class="stat-card stat-card--amber"><span>草稿</span><strong>{{ draftCount }}</strong></article>
        <article class="stat-card stat-card--slate"><span>已下线</span><strong>{{ offlineCount }}</strong></article>
      </section>

      <section v-if="filterVisible" class="filter-card">
        <label>
          关键词
          <input v-model.trim="filters.keyword" placeholder="搜索公告标题..." />
        </label>
        <label>
          公告类别
          <select v-model="filters.category">
            <option value="">全部类别</option>
            <option v-for="item in categoryOptions" :key="item.value" :value="item.value">{{ item.label }}</option>
          </select>
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
        <div class="filter-actions">
          <button class="primary" type="button" :disabled="loading" @click="handleSearch">
            {{ loading ? "查询中..." : "查询" }}
          </button>
          <button class="ghost" type="button" :disabled="loading" @click="resetFilters">重置</button>
        </div>
      </section>

      <section class="table-card">
        <div class="table-wrap">
          <table>
            <thead>
              <tr>
                <th class="col-check"><input type="checkbox" :checked="allChecked" @change="toggleAll($event)" /></th>
                <th>公告标题 / 类别</th>
                <th>状态</th>
                <th>发布日期</th>
                <th>最后操作时间</th>
                <th>创建人</th>
                <th class="right">操作</th>
              </tr>
            </thead>
            <tbody v-if="records.length">
              <tr v-for="item in records" :key="item.id">
                <td class="col-check">
                  <input type="checkbox" :checked="selectedIds.has(item.id)" @change="toggleOne(item.id, $event)" />
                </td>
                <td>
                  <div class="title-cell">
                    <strong>{{ item.title || "-" }}</strong>
                    <div class="subline">
                      <span class="category-chip">{{ formatCategory(item.category) }}</span>
                    </div>
                  </div>
                </td>
                <td>
                  <span :class="['status-pill', `status-pill--${statusClass(item.status)}`]">
                    {{ formatStatus(item.status) }}
                  </span>
                </td>
                <td class="mono">{{ formatTime(item.publishedTime) }}</td>
                <td class="mono">{{ formatLastOperateTime(item) }}</td>
                <td>{{ item.createdByName || item.publishedByName || "监管部门" }}</td>
                <td>
                  <div class="action-row">
                    <button class="icon-btn" type="button" :disabled="actionLoading" title="编辑" @click="openEdit(item)">
                      编辑
                    </button>
                    <button
                      v-if="item.status !== 'PUBLISHED'"
                      class="icon-btn icon-btn--ok"
                      type="button"
                      :disabled="actionLoading"
                      title="发布"
                      @click="handlePublish(item)"
                    >
                      发布
                    </button>
                    <button
                      v-else
                      class="icon-btn icon-btn--warn"
                      type="button"
                      :disabled="actionLoading"
                      title="下线"
                      @click="handleOffline(item)"
                    >
                      下线
                    </button>
                  </div>
                </td>
              </tr>
            </tbody>
          </table>
          <div v-if="!records.length" class="empty">{{ emptyText }}</div>
        </div>
        <div class="pager">
          <span>共 {{ total }} 条，{{ page }}/{{ pages }} 页</span>
          <div class="pager-actions">
            <button class="ghost" type="button" :disabled="page <= 1 || loading" @click="changePage(page - 1)">上一页</button>
            <button class="ghost" type="button" :disabled="page >= pages || loading" @click="changePage(page + 1)">下一页</button>
          </div>
        </div>
      </section>

      <div v-if="status.message" class="status" :class="status.type">{{ status.message }}</div>
    </section>
  </RegulatorAdminWorkspacePage>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from "vue";
import { useRouter } from "vue-router";
import {
  fetchBulletins,
  offlineBulletin,
  publishBulletin
} from "../../api/regulation";
import RegulatorAdminWorkspacePage from "../../components/regulatorAdmin/RegulatorAdminWorkspacePage.vue";
import { formatTime } from "../../utils/formatters";
import { bulletinStatusMap, formatStatusLabel } from "../../utils/statusMaps";
import { getEmptyStateText, resolveErrorMessage } from "../../utils/uiFeedback";
import { useRegulatorAdminShellSession } from "./regulatorAdminShared";

const { regulatorUser, token, handleSidebarNavigate, handleLogout } = useRegulatorAdminShellSession();
const router = useRouter();

const categoryOptions = [
  { value: "POLICY", label: "政策法规" },
  { value: "INSPECTION", label: "监督检查" },
  { value: "NOTICE", label: "消费提示" },
  { value: "OTHER", label: "其他公告" }
];
const categoryLabelMap = Object.fromEntries(categoryOptions.map((item) => [item.value, item.label]));

const filters = reactive({
  keyword: "",
  category: "",
  status: ""
});
const status = reactive({
  message: "",
  type: ""
});
const filterVisible = ref(true);
const loading = ref(false);
const actionLoading = ref(false);
const records = ref([]);
const page = ref(1);
const size = ref(8);
const total = ref(0);
const pages = ref(1);
const selectedIds = ref(new Set());

const publishedCount = computed(() => records.value.filter((item) => item.status === "PUBLISHED").length);
const draftCount = computed(() => records.value.filter((item) => item.status === "DRAFT").length);
const offlineCount = computed(() => records.value.filter((item) => item.status === "OFFLINE").length);
const allChecked = computed(() => records.value.length > 0 && records.value.every((item) => selectedIds.value.has(item.id)));
const emptyText = getEmptyStateText("公告");

function setStatus(message, type = "info") {
  status.message = message;
  status.type = type;
}

function formatStatus(value) {
  return formatStatusLabel(value, bulletinStatusMap);
}

function statusClass(value) {
  if (value === "PUBLISHED") return "published";
  if (value === "OFFLINE") return "offline";
  return "draft";
}

function formatCategory(value) {
  return categoryLabelMap[String(value || "").toUpperCase()] || "未分类";
}

function formatLastOperateTime(item) {
  return formatTime(item?.updateTime || item?.publishedTime || item?.createTime);
}

function toggleAll(event) {
  if (event.target.checked) {
    selectedIds.value = new Set(records.value.map((item) => item.id));
  } else {
    selectedIds.value = new Set();
  }
}

function toggleOne(id, event) {
  const next = new Set(selectedIds.value);
  if (event.target.checked) next.add(id);
  else next.delete(id);
  selectedIds.value = next;
}

function resetFilters() {
  filters.keyword = "";
  filters.category = "";
  filters.status = "";
  handleSearch();
}

function openCreate() {
  router.push({ name: "regulator-admin-bulletin-create" }).catch(() => {});
}

function openEdit(item) {
  const bulletinId = Number(item?.id || 0);
  if (!bulletinId) return;
  router.push({ name: "regulator-admin-bulletin-edit", params: { bulletinId } }).catch(() => {});
}

async function loadBulletins() {
  loading.value = true;
  setStatus("");
  try {
    const data = await fetchBulletins(token.value, {
      keyword: filters.keyword || undefined,
      category: filters.category || undefined,
      status: filters.status || undefined,
      page: page.value,
      size: size.value
    });
    records.value = data.records || [];
    total.value = data.total || 0;
    page.value = data.page || 1;
    size.value = data.size || size.value;
    pages.value = data.pages || 1;
    selectedIds.value = new Set();
  } catch (error) {
    records.value = [];
    setStatus(resolveErrorMessage(error, "加载公告列表失败，请稍后重试。"), "error");
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

async function handlePublish(item) {
  if (!item?.id) return;
  actionLoading.value = true;
  setStatus("");
  try {
    await publishBulletin(token.value, item.id);
    setStatus("公告已发布。", "success");
    await loadBulletins();
  } catch (error) {
    setStatus(resolveErrorMessage(error, "发布公告失败，请稍后重试。"), "error");
  } finally {
    actionLoading.value = false;
  }
}

async function handleOffline(item) {
  if (!item?.id) return;
  actionLoading.value = true;
  setStatus("");
  try {
    await offlineBulletin(token.value, item.id);
    setStatus("公告已下线。", "success");
    await loadBulletins();
  } catch (error) {
    setStatus(resolveErrorMessage(error, "下线公告失败，请稍后重试。"), "error");
  } finally {
    actionLoading.value = false;
  }
}

onMounted(loadBulletins);
</script>

<style scoped>
.bulletin-page { display: grid; gap: 16px; }
.page-head { display: flex; justify-content: space-between; gap: 12px; align-items: end; flex-wrap: wrap; }
.eyebrow { margin: 0; color: #002660; font-size: 11px; font-weight: 900; letter-spacing: 0.1em; text-transform: uppercase; }
.page-head h1 { margin: 6px 0 0; color: #002660; font-size: 32px; font-weight: 900; letter-spacing: -0.02em; }
.head-actions { display: flex; gap: 8px; }

.stats-grid { display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); gap: 10px; }
.stat-card { background: #fff; border: 1px solid #e2e8f0; border-left-width: 4px; border-radius: 4px; padding: 14px; }
.stat-card span { display: block; font-size: 10px; color: #64748b; font-weight: 900; letter-spacing: 0.08em; text-transform: uppercase; }
.stat-card strong { display: block; margin-top: 8px; font-size: 30px; line-height: 1; font-weight: 900; color: #0f172a; }
.stat-card--primary { border-left-color: #002660; }
.stat-card--green { border-left-color: #15803d; }
.stat-card--amber { border-left-color: #b45309; }
.stat-card--slate { border-left-color: #64748b; }

.filter-card {
  background: #eceef1;
  border: 1px solid #dbe2ea;
  border-radius: 4px;
  padding: 12px;
  display: grid;
  grid-template-columns: 1.4fr 0.8fr 0.8fr auto;
  gap: 10px;
  align-items: end;
}
.filter-card label { display: grid; gap: 6px; font-size: 12px; color: #475569; font-weight: 800; }
.filter-card input, .filter-card select {
  min-height: 34px;
  border: 1px solid #cdd5df;
  border-radius: 3px;
  background: #fff;
  font-size: 12px;
  padding: 0 10px;
}
.filter-actions { display: flex; gap: 8px; }

.table-card { background: #fff; border: 1px solid #e2e8f0; border-radius: 4px; overflow: hidden; }
.table-wrap { overflow: auto; }
table { width: 100%; min-width: 1180px; border-collapse: collapse; }
thead tr { background: #f2f4f7; border-bottom: 1px solid #dbe2ea; }
th { padding: 12px 14px; text-align: left; color: #64748b; font-size: 10px; font-weight: 900; letter-spacing: 0.08em; text-transform: uppercase; }
td { padding: 12px 14px; border-top: 1px solid #eef2f7; font-size: 13px; color: #1f2937; vertical-align: middle; }
tbody tr:nth-child(even) { background: #fafbfc; }
tbody tr:hover { background: #f4f7fb; }
.col-check { width: 42px; text-align: center; }
.mono { font-family: ui-monospace, SFMono-Regular, Menlo, Consolas, monospace; font-size: 12px; color: #64748b; white-space: nowrap; }
.right { text-align: right; }
.title-cell strong { display: block; color: #002660; font-size: 14px; }
.subline { margin-top: 6px; display: flex; gap: 6px; align-items: center; }
.category-chip {
  display: inline-flex;
  align-items: center;
  min-height: 20px;
  padding: 0 8px;
  border: 1px solid #dbe2ea;
  border-radius: 2px;
  font-size: 10px;
  font-weight: 800;
  color: #475569;
  background: #f8fafc;
}
.status-pill { display: inline-flex; min-height: 20px; align-items: center; padding: 0 8px; border-radius: 2px; font-size: 10px; font-weight: 900; }
.status-pill--published { background: #ecfdf3; color: #166534; border: 1px solid #bbf7d0; }
.status-pill--draft { background: #fffbeb; color: #92400e; border: 1px solid #fde68a; }
.status-pill--offline { background: #f1f5f9; color: #475569; border: 1px solid #dbe2ea; }

.action-row { display: flex; justify-content: flex-end; gap: 6px; }
.icon-btn {
  min-height: 28px;
  border: 1px solid #d1d5db;
  border-radius: 3px;
  background: #fff;
  color: #475569;
  font-size: 11px;
  font-weight: 800;
  padding: 0 10px;
  cursor: pointer;
}
.icon-btn--ok { color: #065f46; border-color: #86efac; background: #f0fdf4; }
.icon-btn--warn { color: #991b1b; border-color: #fecaca; background: #fff1f2; }

.empty { padding: 24px; text-align: center; font-size: 13px; color: #64748b; }
.pager { padding: 12px 14px; border-top: 1px solid #e2e8f0; display: flex; justify-content: space-between; align-items: center; gap: 10px; font-size: 12px; color: #64748b; font-weight: 700; }
.pager-actions { display: flex; gap: 8px; }

.primary, .ghost {
  min-height: 34px;
  border-radius: 3px;
  padding: 0 12px;
  font-size: 12px;
  font-weight: 800;
  cursor: pointer;
}
.primary { border: 0; background: #002660; color: #fff; }
.ghost { border: 1px solid #d1d5db; background: #fff; color: #334155; }

.status { position: fixed; right: 18px; bottom: 18px; border-radius: 3px; padding: 10px 12px; color: #fff; background: #0f172a; font-size: 13px; z-index: 1300; }
.status.error { background: #b91c1c; }
.status.success { background: #166534; }

@media (max-width: 1100px) {
  .stats-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); }
  .filter-card { grid-template-columns: 1fr 1fr; }
}

@media (max-width: 760px) {
  .stats-grid, .filter-card { grid-template-columns: 1fr; }
  .page-head h1 { font-size: 26px; }
}
</style>
