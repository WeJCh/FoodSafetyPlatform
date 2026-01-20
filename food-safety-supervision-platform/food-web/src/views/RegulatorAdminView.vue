<template>
  <div class="app-shell regulator-shell">

    <div class="form-panel">
      <div class="card">
        <div class="section-title">监管人员</div>
        <div class="admin-info">
          <div>账号：{{ regulatorUser.username }}</div>
          <div>角色：{{ regulatorUser.userType }}</div>
          <div>职责：区域管理员</div>
        </div>

        <div class="sub-nav">
          <button :class="{ active: section === 'enterprises' }" @click="section = 'enterprises'">
            企业管理
          </button>
          <button :class="{ active: section === 'approvals' }" @click="section = 'approvals'; loadPending()">
            备案审核
          </button>
          <button :class="{ active: section === 'dispatch' }" @click="handleDispatchEnter">
            任务派发
          </button>
          <button :class="{ active: section === 'rectification' }" @click="section = 'rectification'">
            整改复核
          </button>
          <button :class="{ active: section === 'complaints' }" @click="section = 'complaints'">
            投诉流转
          </button>
        </div>

        <div v-if="section === 'enterprises'">
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
              <span>操作</span>
            </div>
            <div v-if="!records.length" class="list-empty">
              暂无企业数据
            </div>
            <div v-for="item in records" :key="item.id" class="list-row">
              <span>{{ item.enterpriseName }}</span>
              <span>{{ formatStatus(item.status) }}</span>
              <span>{{ formatApprovalStatus(item.approvalStatus) }}</span>
              <span>{{ item.principal || "-" }}</span>
              <span>{{ formatTime(item.updateTime) }}</span>
              <button class="ghost" type="button" @click="handleViewDetail(item)">查看详情</button>
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
        </div>

        <div v-else-if="section === 'approvals'">
          <div class="section-title">待审核企业</div>
          <div class="approval-toolbar">
            <label>
              审核人姓名
              <input v-model.trim="approvalForm.regulatorName" placeholder="可选填写" />
            </label>
            <label class="approval-comment">
              审批意见
              <input v-model.trim="approvalForm.comment" required placeholder="必填" />
            </label>
            <div class="approval-actions">
              <button class="primary" type="button" :disabled="approvalLoading" @click="handleApproveBatch">
                批量通过
              </button>
              <button class="ghost" type="button" :disabled="approvalLoading" @click="handleRejectBatch">
                批量驳回
              </button>
            </div>
          </div>

          <div class="list-table">
            <div class="list-row list-header approvals-header">
              <label class="checkbox-cell">
                <input type="checkbox" :checked="allSelected" @change="toggleSelectAll" />
                <span>全选</span>
              </label>
              <span>企业</span>
              <span>负责人</span>
              <span>地址</span>
              <span>操作</span>
            </div>
            <div v-if="!pendingRecords.length" class="list-empty">
              暂无待审核企业
            </div>
            <div v-for="item in pendingRecords" :key="item.id" class="list-row approvals-row">
              <label class="checkbox-cell">
                <input type="checkbox" :value="item.id" v-model="selectedIds" />
                <span>选择</span>
              </label>
              <div>
                <div class="primary-text">{{ item.enterpriseName }}</div>
                <div class="secondary-text">{{ item.licenseNo || "-" }}</div>
              </div>
              <div>
                <div class="primary-text">{{ item.principal || "-" }}</div>
                <div class="secondary-text">{{ item.principalPhone || "-" }}</div>
              </div>
              <div>
                <div class="primary-text">{{ item.addressDetail || "-" }}</div>
                <div class="secondary-text">区域：{{ formatRegionName(item.regionId) }}</div>
              </div>
              <div class="action-buttons">
                <button class="ghost" type="button" @click="handleViewDetail(item)">查看详情</button>
                <button class="primary" type="button" :disabled="approvalLoading" @click="handleApprove(item)">
                  通过
                </button>
                <button class="ghost" type="button" :disabled="approvalLoading" @click="handleReject(item)">
                  驳回
                </button>
              </div>
            </div>
          </div>
        </div>

        <div v-else-if="section === 'dispatch'">
          <div class="section-title">任务派发</div>
          <div class="dispatch-grid">
            <div class="dispatch-form">
              <div class="section-subtitle">创建检查任务</div>
              <form class="dispatch-form-grid" @submit.prevent="handleCreateTask">
                <label>
                  选择企业
                  <select v-model="dispatchForm.enterpriseId" :disabled="dispatchLoading">
                    <option value="">请选择企业</option>
                    <option v-for="item in dispatchEnterprises" :key="item.id" :value="item.id">
                      {{ item.enterpriseName }}
                    </option>
                  </select>
                </label>
                <label>
                  任务标题
                  <input v-model.trim="dispatchForm.taskTitle" required placeholder="例：季度检查" />
                </label>
                <label>
                  任务描述
                  <textarea v-model.trim="dispatchForm.taskDesc" rows="3" placeholder="任务要求说明"></textarea>
                </label>
                <label>
                  优先级
                  <select v-model="dispatchForm.priority">
                    <option value="MEDIUM">中</option>
                    <option value="LOW">低</option>
                    <option value="HIGH">高</option>
                  </select>
                </label>
                <label>
                  截止时间
                  <input v-model="dispatchForm.deadline" type="datetime-local" />
                </label>
                <button class="primary" type="submit" :disabled="dispatchLoading">
                  {{ dispatchLoading ? "创建中..." : "创建任务" }}
                </button>
              </form>
            </div>

            <div class="dispatch-list">
              <div class="section-subtitle">任务列表</div>
              <form class="filter-bar" @submit.prevent="handleDispatchSearch">
                <label>
                  企业名称
                  <input v-model.trim="dispatchFilters.enterpriseName" placeholder="输入企业名称" />
                </label>
                <label>
                  任务状态
                  <select v-model="dispatchFilters.status">
                    <option value="">全部</option>
                    <option value="CREATED">待派发</option>
                    <option value="ASSIGNED">已派发</option>
                    <option value="IN_PROGRESS">执行中</option>
                    <option value="COMPLETED">已完成</option>
                  </select>
                </label>
                <button class="primary" type="submit" :disabled="dispatchTaskLoading">
                  {{ dispatchTaskLoading ? "查询中..." : "查询" }}
                </button>
              </form>

              <div class="list-table task-table">
                <div class="list-row list-header task-header">
                  <span>任务号</span>
                  <span>企业</span>
                  <span>优先级</span>
                  <span>状态</span>
                  <span>负责人</span>
                  <span>截止时间</span>
                  <span>操作</span>
                </div>
                <div v-if="!dispatchTasks.length" class="list-empty">
                  暂无任务
                </div>
                <div v-for="task in dispatchTasks" :key="task.id" class="list-row task-row">
                  <span>{{ task.taskNo }}</span>
                  <span>{{ task.enterpriseName || "-" }}</span>
                  <span>{{ formatTaskPriority(task.priority) }}</span>
                  <span>{{ formatTaskStatus(task.status) }}</span>
                  <span>{{ task.assignedToName || "-" }}</span>
                  <span>{{ formatTime(task.deadline) }}</span>
                  <div class="action-buttons">
                    <select
                      v-if="isTaskAssignable(task)"
                      v-model="taskAssignments[task.id]"
                      :disabled="dispatchTaskLoading"
                    >
                      <option value="">选择执法人员</option>
                      <option
                        v-for="item in getEnforcers(task.regionId)"
                        :key="item.id"
                        :value="item.id"
                      >
                        {{ item.name }}
                      </option>
                    </select>
                    <button
                      v-if="isTaskAssignable(task)"
                      class="ghost"
                      type="button"
                      :disabled="dispatchTaskLoading"
                      @click="handleAssignTask(task)"
                    >
                      派发
                    </button>
                  </div>
                </div>
              </div>

              <div class="pager">
                <span>共 {{ dispatchTotal }} 条，{{ dispatchPage }}/{{ dispatchPages }} 页</span>
                <div class="pager-actions">
                  <button
                    class="ghost"
                    type="button"
                    :disabled="dispatchPage <= 1"
                    @click="changeDispatchPage(dispatchPage - 1)"
                  >
                    上一页
                  </button>
                  <button
                    class="ghost"
                    type="button"
                    :disabled="dispatchPage >= dispatchPages"
                    @click="changeDispatchPage(dispatchPage + 1)"
                  >
                    下一页
                  </button>
                </div>
              </div>
            </div>
          </div>
        </div>

        <div v-else class="placeholder">
          <strong>功能占位</strong>
          <p>{{ sectionLabel }} 将在后续版本实现。</p>
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
import { computed, onMounted, reactive, ref } from "vue";
import {
  approveEnterprise,
  approveEnterpriseBatch,
  assignInspectionTask,
  createInspectionTask,
  fetchEnterprises,
  fetchInspectionTasks,
  fetchPendingEnterprises,
  fetchRegulatorProfiles,
  fetchRegionPath,
  rejectEnterprise,
  rejectEnterpriseBatch
} from "../api/regulation";

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

const emit = defineEmits(["logout", "view-enterprise"]);

const section = ref("enterprises");

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
const approvalLoading = ref(false);
const pendingRecords = ref([]);
const approvalForm = reactive({ regulatorName: "", comment: "" });
const selectedIds = ref([]);
const regionNameMap = reactive({});
const dispatchLoading = ref(false);
const dispatchTaskLoading = ref(false);
const dispatchEnterprises = ref([]);
const dispatchForm = reactive({
  enterpriseId: "",
  taskTitle: "",
  taskDesc: "",
  priority: "MEDIUM",
  deadline: ""
});
const dispatchFilters = reactive({
  enterpriseName: "",
  status: ""
});
const dispatchTasks = ref([]);
const dispatchPage = ref(1);
const dispatchSize = ref(8);
const dispatchTotal = ref(0);
const dispatchPages = ref(1);
const taskAssignments = reactive({});
const enforcerMap = reactive({});

function setStatus(message, type = "info") {
  status.message = message;
  status.type = type;
}

const sectionLabelMap = {
  approvals: "备案审核",
  dispatch: "任务派发",
  rectification: "整改复核",
  complaints: "投诉流转"
};

const sectionLabel = computed(() => sectionLabelMap[section.value] || "当前模块");

const statusMap = {
  NORMAL: "正常",
  KEY: "重点监管"
};

const approvalStatusMap = {
  PENDING: "待审核",
  APPROVED: "已通过",
  REJECTED: "已驳回"
};

const taskStatusMap = {
  CREATED: "待派发",
  ASSIGNED: "已派发",
  IN_PROGRESS: "执行中",
  COMPLETED: "已完成",
  CLOSED: "已关闭"
};

const taskPriorityMap = {
  LOW: "低",
  MEDIUM: "中",
  HIGH: "高"
};

function formatStatus(value) {
  return statusMap[value] || value || "-";
}

function formatApprovalStatus(value) {
  return approvalStatusMap[value] || value || "-";
}

function formatTaskStatus(value) {
  return taskStatusMap[value] || value || "-";
}

function formatTaskPriority(value) {
  return taskPriorityMap[value] || value || "-";
}

function formatRegionName(regionId) {
  if (!regionId) return "-";
  return regionNameMap[regionId] || `区域 ${regionId}`;
}

async function ensureRegionName(regionId) {
  if (!regionId || regionNameMap[regionId]) {
    return;
  }
  try {
    const path = await fetchRegionPath(props.token, regionId);
    regionNameMap[regionId] = Array.isArray(path) && path.length
      ? path.map((item) => item.name).join("/")
      : `区域 ${regionId}`;
  } catch {
    regionNameMap[regionId] = `区域 ${regionId}`;
  }
}

async function handleDispatchEnter() {
  section.value = "dispatch";
  await loadDispatch();
}

async function loadDispatch() {
  await Promise.all([loadDispatchEnterprises(), loadDispatchTasks()]);
}

async function loadDispatchEnterprises() {
  dispatchLoading.value = true;
  try {
    const data = await fetchEnterprises(props.token, {
      approvalStatus: "APPROVED",
      page: 1,
      size: 100
    });
    dispatchEnterprises.value = data.records || [];
  } catch (error) {
    setStatus(error.message || "加载企业列表失败", "error");
  } finally {
    dispatchLoading.value = false;
  }
}

async function loadDispatchTasks() {
  dispatchTaskLoading.value = true;
  setStatus("");
  try {
    const data = await fetchInspectionTasks(props.token, {
      ...dispatchFilters,
      page: dispatchPage.value,
      size: dispatchSize.value
    });
    dispatchTasks.value = data.records || [];
    dispatchTotal.value = data.total || 0;
    dispatchPage.value = data.page || 1;
    dispatchSize.value = data.size || dispatchSize.value;
    dispatchPages.value = data.pages || 1;
    const regionIds = dispatchTasks.value
      .map((task) => task.regionId)
      .filter((value) => value);
    await Promise.all(regionIds.map((id) => ensureEnforcers(id)));
  } catch (error) {
    setStatus(error.message || "加载任务列表失败", "error");
  } finally {
    dispatchTaskLoading.value = false;
  }
}

async function handleDispatchSearch() {
  dispatchPage.value = 1;
  await loadDispatchTasks();
}

async function changeDispatchPage(nextPage) {
  dispatchPage.value = nextPage;
  await loadDispatchTasks();
}

async function handleCreateTask() {
  if (!dispatchForm.enterpriseId) {
    setStatus("请选择企业后再创建任务", "error");
    return;
  }
  if (!dispatchForm.taskTitle.trim()) {
    setStatus("请填写任务标题", "error");
    return;
  }
  dispatchLoading.value = true;
  setStatus("");
  try {
    await createInspectionTask(props.token, {
      enterpriseId: dispatchForm.enterpriseId,
      taskTitle: dispatchForm.taskTitle,
      taskDesc: dispatchForm.taskDesc,
      priority: dispatchForm.priority,
      deadline: normalizeDeadline(dispatchForm.deadline)
    });
    setStatus("任务已创建。", "success");
    dispatchForm.taskTitle = "";
    dispatchForm.taskDesc = "";
    dispatchForm.priority = "MEDIUM";
    dispatchForm.deadline = "";
    await loadDispatchTasks();
  } catch (error) {
    setStatus(error.message || "创建任务失败", "error");
  } finally {
    dispatchLoading.value = false;
  }
}

async function ensureEnforcers(regionId) {
  if (!regionId || enforcerMap[regionId]) {
    return;
  }
  try {
    const data = await fetchRegulatorProfiles(props.token, {
      roleType: "REGULATOR_ENFORCER",
      regionId
    });
    enforcerMap[regionId] = Array.isArray(data) ? data : [];
  } catch {
    enforcerMap[regionId] = [];
  }
}

function getEnforcers(regionId) {
  if (!regionId) return [];
  return enforcerMap[regionId] || [];
}

function isTaskAssignable(task) {
  return ["CREATED", "ASSIGNED"].includes(task.status);
}

async function handleAssignTask(task) {
  const regulatorId = taskAssignments[task.id];
  if (!regulatorId) {
    setStatus("请选择执法人员后再派发", "error");
    return;
  }
  dispatchTaskLoading.value = true;
  setStatus("");
  try {
    await assignInspectionTask(props.token, task.id, { regulatorId });
    setStatus("任务已派发。", "success");
    await loadDispatchTasks();
  } catch (error) {
    setStatus(error.message || "任务派发失败", "error");
  } finally {
    dispatchTaskLoading.value = false;
  }
}

function normalizeDeadline(value) {
  if (!value) return null;
  return value.includes(":") && value.length === 16 ? `${value}:00` : value;
}

async function loadPending() {
  approvalLoading.value = true;
  setStatus("");
  try {
    const data = await fetchPendingEnterprises(props.token);
    pendingRecords.value = Array.isArray(data) ? data : [];
    selectedIds.value = [];
    const regionIds = pendingRecords.value
      .map((item) => item.regionId)
      .filter((value) => value);
    await Promise.all(regionIds.map((id) => ensureRegionName(id)));
  } catch (error) {
    setStatus(error.message || "加载待审核企业失败", "error");
  } finally {
    approvalLoading.value = false;
  }
}

async function handleApprove(item) {
  if (!approvalForm.comment.trim()) {
    setStatus("审批意见必填", "error");
    return;
  }
  approvalLoading.value = true;
  setStatus("");
  try {
    await approveEnterprise(props.token, item.id, approvalForm);
    setStatus("已通过企业备案。", "success");
    await loadPending();
  } catch (error) {
    setStatus(error.message || "审核通过失败", "error");
  } finally {
    approvalLoading.value = false;
  }
}

async function handleReject(item) {
  if (!approvalForm.comment.trim()) {
    setStatus("审批意见必填", "error");
    return;
  }
  approvalLoading.value = true;
  setStatus("");
  try {
    await rejectEnterprise(props.token, item.id, approvalForm);
    setStatus("已驳回企业备案。", "success");
    await loadPending();
  } catch (error) {
    setStatus(error.message || "驳回失败", "error");
  } finally {
    approvalLoading.value = false;
  }
}

async function handleApproveBatch() {
  if (!selectedIds.value.length) {
    setStatus("请选择需要审批的企业", "error");
    return;
  }
  if (!approvalForm.comment.trim()) {
    setStatus("审批意见必填", "error");
    return;
  }
  approvalLoading.value = true;
  setStatus("");
  try {
    await approveEnterpriseBatch(props.token, {
      ids: selectedIds.value,
      comment: approvalForm.comment,
      regulatorName: approvalForm.regulatorName
    });
    setStatus("批量通过成功。", "success");
    await loadPending();
  } catch (error) {
    setStatus(error.message || "批量通过失败", "error");
  } finally {
    approvalLoading.value = false;
  }
}

async function handleRejectBatch() {
  if (!selectedIds.value.length) {
    setStatus("请选择需要审批的企业", "error");
    return;
  }
  if (!approvalForm.comment.trim()) {
    setStatus("审批意见必填", "error");
    return;
  }
  approvalLoading.value = true;
  setStatus("");
  try {
    await rejectEnterpriseBatch(props.token, {
      ids: selectedIds.value,
      comment: approvalForm.comment,
      regulatorName: approvalForm.regulatorName
    });
    setStatus("批量驳回成功。", "success");
    await loadPending();
  } catch (error) {
    setStatus(error.message || "批量驳回失败", "error");
  } finally {
    approvalLoading.value = false;
  }
}

const allSelected = computed(() => {
  if (!pendingRecords.value.length) return false;
  return selectedIds.value.length === pendingRecords.value.length;
});

function toggleSelectAll(event) {
  if (event.target.checked) {
    selectedIds.value = pendingRecords.value.map((item) => item.id);
  } else {
    selectedIds.value = [];
  }
}

function handleViewDetail(item) {
  emit("view-enterprise", item.id);
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
    const regionIds = records.value
      .map((item) => item.regionId)
      .filter((value) => value);
    await Promise.all(regionIds.map((id) => ensureRegionName(id)));
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
.regulator-shell {
  grid-template-columns: 1fr;
}

.regulator-shell .hero-panel {
  padding: 48px 80px 32px;
}

.regulator-shell .hero-content h1 {
  font-size: 34px;
}

.regulator-shell .hero-content p {
  max-width: 720px;
  font-size: 16px;
}

.regulator-shell .hero-highlights {
  grid-template-columns: repeat(3, minmax(0, 1fr));
}

.regulator-shell .form-panel {
  padding: 20px 80px 70px;
  align-items: flex-start;
}

.regulator-shell .card {
  max-width: 980px;
  width: 100%;
}

.filter-bar {
  display: grid;
  gap: 12px;
  margin-bottom: 16px;
}

.sub-nav {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-bottom: 16px;
}

.sub-nav button {
  padding: 8px 14px;
  border-radius: 999px;
  border: 1px solid var(--stroke);
  background: transparent;
  color: var(--ink);
  cursor: pointer;
}

.sub-nav button.active {
  background: #efe2d3;
  border-color: transparent;
  font-weight: 600;
}

.placeholder {
  border-radius: 12px;
  border: 1px dashed var(--stroke);
  padding: 16px;
  color: var(--muted);
  font-size: 13px;
}

.list-table {
  border-radius: 14px;
  border: 1px solid var(--stroke);
  background: #faf6f1;
  overflow: hidden;
}

.list-row {
  display: grid;
  grid-template-columns: 1.6fr 0.9fr 0.9fr 1fr 1.2fr 0.8fr;
  gap: 8px;
  padding: 12px 14px;
  align-items: center;
  font-size: 13px;
}

.list-header {
  font-weight: 600;
  background: #f1e6db;
}

.approvals-header,
.approvals-row {
  grid-template-columns: 0.7fr 1.1fr 1.1fr 1.4fr 1.6fr;
}

.primary-text {
  font-weight: 600;
}

.secondary-text {
  font-size: 12px;
  color: var(--muted);
}

.action-buttons {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.dispatch-grid {
  display: grid;
  grid-template-columns: 1fr 2fr;
  gap: 20px;
}

.dispatch-form {
  border: 1px solid var(--stroke);
  border-radius: 14px;
  padding: 14px;
  background: #fff6ea;
}

.dispatch-list {
  border-radius: 14px;
  border: 1px solid var(--stroke);
  padding: 14px;
  background: #faf6f1;
}

.dispatch-form-grid {
  display: grid;
  gap: 12px;
}

.section-subtitle {
  font-weight: 600;
  margin-bottom: 10px;
}

.task-table {
  margin-top: 8px;
}

.task-header,
.task-row {
  grid-template-columns: 1.2fr 1.4fr 0.7fr 0.9fr 1fr 1fr 1.4fr;
}

.approval-toolbar {
  display: grid;
  gap: 12px;
  grid-template-columns: 1fr 2fr auto;
  align-items: end;
  margin-bottom: 16px;
}

.approval-comment input {
  width: 100%;
}

.approval-actions {
  display: flex;
  gap: 10px;
  justify-content: flex-end;
}

.checkbox-cell {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: var(--muted);
}

@media (max-width: 900px) {
  .approval-toolbar {
    grid-template-columns: 1fr;
  }

  .dispatch-grid {
    grid-template-columns: 1fr;
  }

  .task-header,
  .task-row {
    grid-template-columns: 1fr;
  }

  .approvals-header,
  .approvals-row {
    grid-template-columns: 1fr;
  }

  .checkbox-cell span {
    display: none;
  }
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

@media (max-width: 1024px) {
  .regulator-shell .hero-panel {
    padding: 36px 40px 24px;
  }

  .regulator-shell .form-panel {
    padding: 10px 40px 60px;
  }

  .regulator-shell .hero-highlights {
    grid-template-columns: 1fr;
  }
}
</style>
