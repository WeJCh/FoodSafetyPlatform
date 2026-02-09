<template>
  <div class="admin-shell regulator-shell">
    <aside class="admin-sidebar">
      <div class="admin-brand">监管中心</div>
      <div class="sidebar-meta">
        <span>账号：{{ regulatorUser.username }}</span>
        <span>角色：{{ regulatorUser.userType }}</span>
        <span>职责：区域管理员</span>
      </div>
      <nav class="admin-nav">
        <button :class="{ active: section === 'enterprises' }" @click="section = 'enterprises'">
          企业管理
        </button>
        <button :class="{ active: section === 'approvals' }" @click="section = 'approvals'; loadPending()">
          备案审核
        </button>
        <button :class="{ active: section === 'dispatch' }" @click="handleDispatchEnter">
          任务派发
        </button>
        <button :class="{ active: section === 'complaints' }" @click="handleComplaintEnter">
          投诉流转
        </button>
        <button :class="{ active: section === 'rectification' }" @click="section = 'rectification'">
          检查整改
        </button>
        <button :class="{ active: section === 'warning' }" @click="section = 'warning'">
          风险预警
        </button>
        <button :class="{ active: section === 'stats' }" @click="section = 'stats'">
          数据统计
        </button>
      </nav>
      <button class="ghost sidebar-ghost" type="button" @click="handleLogout">退出登录</button>
    </aside>

    <div class="admin-main">
      <div class="dashboard-topbar">
        <div class="dashboard-title">
          <strong>区域管理员工作台</strong>
          <span>备案审核、任务派发与企业监管</span>
        </div>
        <div class="user-chip">
          <span>{{ regulatorUser.username }}</span>
          <span>区域管理员</span>
        </div>
      </div>

      <div class="dashboard-content">
        <div class="card dashboard-card">

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
          <button :class="{ active: section === 'complaints' }" @click="handleComplaintEnter">
            投诉流转
          </button>
        </div>

        <div v-if="section === 'enterprises'">
          <div class="section-title">企业管理</div>
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
                <label class="span-all">
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
                <button class="primary dispatch-submit span-all" type="submit" :disabled="dispatchLoading">
                  {{ dispatchLoading ? "创建中..." : "创建任务" }}
                </button>
              </form>
            </div>

            <div class="dispatch-list">
              <div class="section-subtitle">任务列表</div>
              <form class="filter-bar filter-bar--triple" @submit.prevent="handleDispatchSearch">
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

        <div v-else-if="section === 'complaints'">
          <div class="section-title">投诉流转</div>
          <form class="filter-bar filter-bar--quad" @submit.prevent="handleComplaintSearch">
            <label>
              状态
              <select v-model="complaintFilters.status">
                <option value="">全部</option>
                <option value="SUBMITTED">已提交</option>
                <option value="PENDING">待受理</option>
                <option value="ASSIGNED">已派发</option>
                <option value="PROCESSING">处理中</option>
                <option value="FEEDBACKED">已反馈</option>
              </select>
            </label>
            <label>
              企业名称
              <input v-model.trim="complaintFilters.enterpriseName" placeholder="输入企业名称" />
            </label>
            <label>
              处理人
              <input v-model.trim="complaintFilters.assignedToName" placeholder="执法人员姓名" />
            </label>
            <label>
              指派人
              <input v-model.trim="complaintFilters.assignedByName" placeholder="管理员姓名" />
            </label>
            <button class="primary" type="submit" :disabled="complaintLoading">
              {{ complaintLoading ? "查询中..." : "查询" }}
            </button>
          </form>

          <div class="list-table complaint-table">
            <div class="list-row list-header complaint-header">
              <span>投诉号</span>
              <span>企业</span>
              <span>状态</span>
              <span>指派人</span>
              <span>处理人</span>
              <span>更新时间</span>
              <span>操作</span>
            </div>
            <div v-if="!complaintRecords.length" class="list-empty">
              暂无投诉
            </div>
            <div v-for="item in complaintRecords" :key="item.id" class="list-row complaint-row">
              <span>{{ item.complaintNo }}</span>
              <span>{{ item.enterpriseName || "-" }}</span>
              <span>{{ formatComplaintStatus(item.status) }}</span>
              <span>{{ item.assignedByName || "-" }}</span>
              <span>{{ item.assignedToName || "-" }}</span>
              <span>{{ formatTime(item.updateTime) }}</span>
              <div class="action-buttons">
                <button class="ghost" type="button" @click="openComplaintDetail(item)">
                  查看详情
                </button>
                <button
                  v-if="item.status === 'SUBMITTED'"
                  class="primary"
                  type="button"
                  :disabled="complaintLoading"
                  @click="handleAcceptComplaint(item)"
                >
                  受理
                </button>
              </div>
            </div>
          </div>

          <div class="pager">
            <span>共 {{ complaintTotal }} 条，{{ complaintPage }}/{{ complaintPages }} 页</span>
            <div class="pager-actions">
              <button
                class="ghost"
                type="button"
                :disabled="complaintPage <= 1"
                @click="changeComplaintPage(complaintPage - 1)"
              >
                上一页
              </button>
              <button
                class="ghost"
                type="button"
                :disabled="complaintPage >= complaintPages"
                @click="changeComplaintPage(complaintPage + 1)"
              >
                下一页
              </button>
            </div>
          </div>

          <div v-if="complaintDetail" class="modal-mask" @click.self="closeComplaintDetail">
            <div class="modal-card modal-card--wide">
              <div class="modal-title">投诉详情</div>
              <div class="modal-body">
                <div class="modal-field">
                  <span>投诉号</span>
                  <strong>{{ complaintDetail.complaint.complaintNo || "-" }}</strong>
                </div>
                <div class="modal-field">
                  <span>状态</span>
                  <strong>{{ formatComplaintStatus(complaintDetail.complaint.status) }}</strong>
                </div>
                <div class="modal-field">
                  <span>投诉内容</span>
                  <strong>{{ complaintDetail.complaint.content || "-" }}</strong>
                </div>
                <div class="modal-field">
                  <span>现场图片</span>
                  <div class="modal-gallery" v-if="complaintDetail.complaint?.imageUrls?.length">
                    <button
                      v-for="(url, index) in complaintDetail.complaint.imageUrls"
                      :key="`${url}-${index}`"
                      class="modal-image"
                      type="button"
                      @click="openImagePreview(complaintDetail.complaint.imageUrls, index)"
                    >
                      <img :src="url" alt="现场图片" />
                    </button>
                  </div>
                  <div v-else class="modal-empty">未上传现场图片</div>
                </div>

                <div class="modal-field">
                  <span>企业信息</span>
                  <div class="modal-list">
                    <div class="modal-item">
                      <div class="modal-item-name">{{ complaintDetail.enterprise?.enterpriseName || "-" }}</div>
                      <div class="modal-item-meta">{{ complaintDetail.enterprise?.addressDetail || "-" }}</div>
                      <div class="modal-item-desc">
                        负责人：{{ complaintDetail.enterprise?.principal || "-" }}
                        {{ complaintDetail.enterprise?.principalPhone || "" }}
                      </div>
                    </div>
                  </div>
                </div>

                <div class="modal-field">
                  <span>处理记录</span>
                  <div class="modal-list">
                    <div v-if="!complaintDetail.handles?.length" class="modal-empty">暂无处理记录</div>
                    <div v-for="(handle, index) in complaintDetail.handles" :key="index" class="modal-item">
                      <div class="modal-item-name">{{ handle.handlerName || "-" }}</div>
                      <div class="modal-item-meta">{{ formatTime(handle.handleTime) }}</div>
                      <div class="modal-item-desc">{{ handle.handleResult || "-" }}</div>
                    </div>
                  </div>
                </div>
              </div>

              <div v-if="isComplaintAssignable(complaintDetail.complaint)" class="modal-actions modal-actions--stack">
                <div class="modal-field">
                  <span>派发执法人员</span>
                  <select v-model="complaintAssign.regulatorId">
                    <option value="">请选择</option>
                    <option v-for="item in complaintEnforcers" :key="item.id" :value="item.id">
                      {{ item.name }}
                    </option>
                  </select>
                </div>
                <button class="primary" type="button" :disabled="complaintLoading" @click="handleAssignComplaint">
                  确认派发
                </button>
                <button class="ghost" type="button" @click="closeComplaintDetail">关闭</button>
              </div>
              <div v-else class="modal-actions">
                <button class="ghost" type="button" @click="closeComplaintDetail">关闭</button>
              </div>
            </div>
          </div>
        </div>

        <div v-else class="placeholder">
          <strong>功能占位</strong>
          <p>{{ sectionLabel }} 将在后续版本实现。</p>
        </div>

        <div class="status" :class="status.type" v-if="status.message">
          {{ status.message }}
        </div>
      </div>
      </div>
    </div>
    <div v-if="currentImagePreviewUrl" class="image-preview-mask" @click.self="closeImagePreview">
      <div class="image-preview-card">
        <img :src="currentImagePreviewUrl" alt="现场图片大图" />
        <div class="image-preview-actions">
          <button class="ghost" type="button" :disabled="imagePreviewIndex <= 0" @click="showPrevImage">
            上一张
          </button>
          <span class="image-preview-count">{{ imagePreviewIndex + 1 }}/{{ imagePreviewUrls.length }}</span>
          <button
            class="ghost"
            type="button"
            :disabled="imagePreviewIndex >= imagePreviewUrls.length - 1"
            @click="showNextImage"
          >
            下一张
          </button>
          <button class="ghost" type="button" @click="closeImagePreview">关闭</button>
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
  acceptComplaint,
  assignComplaint,
  assignInspectionTask,
  createInspectionTask,
  fetchComplaintDetail,
  fetchComplaints,
  fetchEligibleRegulators,
  fetchEnterprises,
  fetchInspectionTasks,
  fetchPendingEnterprises,
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
  },
  initialSection: {
    type: String,
    default: ""
  }
});

const emit = defineEmits(["logout", "view-enterprise"]);

const section = ref(props.initialSection || "enterprises");

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
const complaintLoading = ref(false);
const complaintRecords = ref([]);
const complaintPage = ref(1);
const complaintSize = ref(8);
const complaintTotal = ref(0);
const complaintPages = ref(1);
const complaintFilters = reactive({
  status: "",
  enterpriseName: "",
  assignedToName: "",
  assignedByName: ""
});
const complaintDetail = ref(null);
const complaintAssign = reactive({
  regulatorId: ""
});
const complaintEnforcers = ref([]);
const imagePreviewUrls = ref([]);
const imagePreviewIndex = ref(0);
const currentImagePreviewUrl = computed(
  () => imagePreviewUrls.value[imagePreviewIndex.value] || ""
);

function setStatus(message, type = "info") {
  status.message = message;
  status.type = type;
}

const sectionLabelMap = {
  approvals: "备案审核",
  dispatch: "任务派发",
  rectification: "整改复核",
  complaints: "投诉流转",
  warning: "风险预警",
  stats: "数据统计"
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

const complaintStatusMap = {
  SUBMITTED: "已提交",
  PENDING: "待受理",
  ASSIGNED: "已派发",
  PROCESSING: "处理中",
  FEEDBACKED: "已反馈"
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

function formatComplaintStatus(value) {
  return complaintStatusMap[value] || value || "-";
}

function formatTaskStatus(value) {
  return taskStatusMap[value] || value || "-";
}

function formatTaskPriority(value) {
  return taskPriorityMap[value] || value || "-";
}

function formatRegionName(regionId) {
  if (!regionId) return "-";
  return regionNameMap[regionId] || `鍖哄煙 ${regionId}`;
}

async function ensureRegionName(regionId) {
  if (!regionId || regionNameMap[regionId]) {
    return;
  }
  try {
    const path = await fetchRegionPath(props.token, regionId);
    regionNameMap[regionId] = Array.isArray(path) && path.length
      ? path.map((item) => item.name).join("/")
      : `鍖哄煙 ${regionId}`;
  } catch {
    regionNameMap[regionId] = `鍖哄煙 ${regionId}`;
  }
}

async function handleDispatchEnter() {
  section.value = "dispatch";
  await loadDispatch();
}

async function handleComplaintEnter() {
  section.value = "complaints";
  await loadComplaints();
}

async function loadDispatch() {
  await Promise.all([loadDispatchEnterprises(), loadDispatchTasks()]);
}

async function loadComplaints() {
  complaintLoading.value = true;
  setStatus("");
  try {
    const data = await fetchComplaints(props.token, {
      ...complaintFilters,
      page: complaintPage.value,
      size: complaintSize.value
    });
    complaintRecords.value = data.records || [];
    complaintTotal.value = data.total || 0;
    complaintPage.value = data.page || 1;
    complaintSize.value = data.size || complaintSize.value;
    complaintPages.value = data.pages || 1;
  } catch (error) {
    setStatus(error.message || "加载投诉列表失败", "error");
  } finally {
    complaintLoading.value = false;
  }
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

async function handleComplaintSearch() {
  complaintPage.value = 1;
  await loadComplaints();
}

async function changeComplaintPage(nextPage) {
  complaintPage.value = nextPage;
  await loadComplaints();
}

async function openComplaintDetail(item) {
  if (!item?.id) return;
  complaintLoading.value = true;
  setStatus("");
  try {
    complaintDetail.value = await fetchComplaintDetail(props.token, item.id);
    complaintAssign.regulatorId = "";
    await loadComplaintEnforcers(complaintDetail.value?.enterprise?.regionId);
  } catch (error) {
    setStatus(error.message || "加载投诉详情失败", "error");
  } finally {
    complaintLoading.value = false;
  }
}

function closeComplaintDetail() {
  complaintDetail.value = null;
  complaintEnforcers.value = [];
  complaintAssign.regulatorId = "";
  imagePreviewUrls.value = [];
  imagePreviewIndex.value = 0;
}

function openImagePreview(urls, index) {
  if (!Array.isArray(urls) || !urls.length) return;
  imagePreviewUrls.value = urls;
  imagePreviewIndex.value = Math.min(Math.max(index || 0, 0), urls.length - 1);
}

function closeImagePreview() {
  imagePreviewUrls.value = [];
  imagePreviewIndex.value = 0;
}

function showPrevImage() {
  if (imagePreviewIndex.value <= 0) return;
  imagePreviewIndex.value -= 1;
}

function showNextImage() {
  if (imagePreviewIndex.value >= imagePreviewUrls.value.length - 1) return;
  imagePreviewIndex.value += 1;
}

async function handleAcceptComplaint(item) {
  if (!item?.id) return;
  complaintLoading.value = true;
  setStatus("");
  try {
    await acceptComplaint(props.token, item.id);
    setStatus("投诉已受理", "success");
    await loadComplaints();
  } catch (error) {
    setStatus(error.message || "投诉受理失败", "error");
  } finally {
    complaintLoading.value = false;
  }
}

async function handleAssignComplaint() {
  if (!complaintDetail.value?.complaint?.id) return;
  if (!complaintAssign.regulatorId) {
    setStatus("请选择执法人员", "error");
    return;
  }
  complaintLoading.value = true;
  setStatus("");
  try {
    await assignComplaint(props.token, complaintDetail.value.complaint.id, {
      regulatorId: complaintAssign.regulatorId
    });
    setStatus("投诉已派发", "success");
    await openComplaintDetail(complaintDetail.value.complaint);
    await loadComplaints();
  } catch (error) {
    setStatus(error.message || "投诉派发失败", "error");
  } finally {
    complaintLoading.value = false;
  }
}

async function loadComplaintEnforcers(regionId) {
  if (!regionId) {
    complaintEnforcers.value = [];
    return;
  }
  try {
    const data = await fetchEligibleRegulators(props.token, regionId);
    complaintEnforcers.value = Array.isArray(data) ? data : [];
  } catch {
    complaintEnforcers.value = [];
  }
}

function isComplaintAssignable(complaint) {
  return ["PENDING", "ASSIGNED"].includes(complaint?.status);
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
    setStatus("任务已创建", "success");
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
    const data = await fetchEligibleRegulators(props.token, regionId);
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
    setStatus("任务已派发", "success");
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
    setStatus("已通过企业备案", "success");
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
    setStatus("已驳回企业备案", "success");
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
    setStatus("批量通过成功", "success");
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
    setStatus("批量驳回成功", "success");
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
  emit("view-enterprise", { id: item.id, fromSection: section.value });
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
  if (section.value === "approvals") {
    loadPending();
    return;
  }
  if (section.value === "dispatch") {
    handleDispatchEnter();
    return;
  }
  load();
});
</script>

<style scoped>
.regulator-shell {
  grid-template-columns: 260px 1fr;
}

.regulator-shell .sub-nav {
  display: none;
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
  display: block;
  width: 100%;
  padding: 24px 36px 60px;
  align-items: stretch;
  justify-content: flex-start;
}

.regulator-shell .card {
  max-width: 1280px;
  width: 100%;
  padding: 28px;
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
  background: var(--card-strong);
  color: var(--ink);
  cursor: pointer;
}

.sub-nav button.active {
  background: var(--nav);
  border-color: transparent;
  font-weight: 600;
  color: #fff;
}

.placeholder {
  border-radius: 12px;
  border: 1px dashed var(--stroke);
  padding: 16px;
  color: var(--muted);
  font-size: 13px;
}

.list-row {
  --row-columns: 1.6fr 0.9fr 0.9fr 1fr 1.2fr 0.8fr;
}

.approvals-header,
.approvals-row {
  --row-columns: 0.7fr 1.1fr 1.1fr 1.4fr 1.6fr;
}

.primary-text {
  font-weight: 600;
}

.secondary-text {
  font-size: 12px;
  color: var(--muted);
}

.dispatch-grid {
  display: grid;
  grid-template-columns: 1fr;
  gap: 18px;
  align-items: start;
}

.dispatch-form {
  border: 1px solid var(--stroke);
  border-radius: 14px;
  padding: 18px 20px;
  background: #eef6fb;
}

.dispatch-list {
  border-radius: 14px;
  border: 1px solid var(--stroke);
  padding: 18px 20px;
  background: var(--card-strong);
}

.dispatch-form-grid {
  display: grid;
  gap: 14px;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  align-items: end;
}

.dispatch-form-grid label {
  min-width: 0;
  display: grid;
  gap: 6px;
}

.dispatch-form-grid .span-all {
  grid-column: 1 / -1;
}

.dispatch-form-grid .span-all textarea {
  min-height: 96px;
  resize: vertical;
}

.dispatch-submit {
  justify-self: end;
  min-width: 140px;
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
  /* Use minmax to prevent long task numbers from overflowing into other columns. */
  grid-template-columns:
    minmax(200px, 1.6fr)
    minmax(140px, 1.3fr)
    minmax(72px, 0.7fr)
    minmax(88px, 0.8fr)
    minmax(96px, 0.9fr)
    minmax(140px, 1.1fr)
    minmax(200px, 1.6fr);
}

.task-row > span,
.task-row > div {
  min-width: 0;
}

.task-row > span {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.task-row > span:first-child {
  /* Task numbers are long and unbroken; allow wrapping to avoid overlap. */
  white-space: normal;
  word-break: break-all;
  line-height: 1.35;
}

.approval-toolbar {
  --approval-columns: 1fr 2fr auto;
}

.approval-comment input {
  width: 100%;
}

.complaint-header,
.complaint-row {
  --row-columns: 1.4fr 1.2fr 0.9fr 0.9fr 0.9fr 1.1fr 1.2fr;
}

.modal-card--wide {
  max-width: 760px;
}

.modal-actions--stack {
  flex-direction: column;
  align-items: stretch;
  gap: 10px;
}

.modal-gallery {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(120px, 1fr));
  gap: 10px;
  margin-top: 6px;
}

.modal-image {
  display: block;
  border-radius: 10px;
  overflow: hidden;
  border: 1px solid var(--stroke);
  background: #f6f9ff;
  padding: 0;
  cursor: pointer;
}

.modal-image img {
  width: 100%;
  height: 96px;
  object-fit: cover;
  display: block;
}

.image-preview-mask {
  position: fixed;
  inset: 0;
  background: rgba(15, 23, 42, 0.55);
  display: grid;
  place-items: center;
  z-index: 9999;
}

.image-preview-card {
  background: #fff;
  border-radius: 16px;
  padding: 16px;
  max-width: min(900px, 92vw);
  max-height: 88vh;
  display: grid;
  gap: 12px;
  box-shadow: 0 24px 60px rgba(15, 23, 42, 0.25);
}

.image-preview-card img {
  width: 100%;
  height: auto;
  max-height: 70vh;
  object-fit: contain;
  border-radius: 12px;
  background: #f6f9ff;
}

.image-preview-actions {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  flex-wrap: wrap;
}

.image-preview-count {
  font-size: 12px;
  color: var(--muted);
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

  .dispatch-form-grid {
    grid-template-columns: 1fr;
  }

  .dispatch-submit {
    justify-self: stretch;
    width: 100%;
  }

  .task-header,
  .task-row {
    grid-template-columns: 1fr;
  }

  .approvals-header,
  .approvals-row {
    --row-columns: 1fr;
  }

  .checkbox-cell span {
    display: none;
  }
}

@media (max-width: 1200px) {
  .dispatch-form-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
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

@media (max-width: 960px) {
  .regulator-shell {
    grid-template-columns: 1fr;
  }
}
</style>



