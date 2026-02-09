
<template>
  <div class="admin-shell regulator-shell">
    <aside class="admin-sidebar">
      <div class="admin-brand">监管中心</div>
      <div class="sidebar-meta">
        <span>账号：{{ regulatorUser.username }}</span>
        <span>角色：{{ regulatorUser.userType }}</span>
        <span>职责：执法人员</span>
      </div>
      <nav class="admin-nav">
        <button :class="{ active: section === 'enterprises' }" @click="section = 'enterprises'">企业监管</button>
        <button :class="{ active: section === 'tasks' }" @click="handleTaskEnter">我的任务</button>
        <button :class="{ active: section === 'inspections' }" @click="handleInspectionEnter">检查记录</button>
        <button :class="{ active: section === 'rectification' }" @click="section = 'rectification'">整改跟进</button>
        <button :class="{ active: section === 'complaints' }" @click="handleComplaintEnter">投诉处理</button>
      </nav>
      <button class="ghost sidebar-ghost" type="button" @click="handleLogout">退出登录</button>
    </aside>

    <div class="admin-main">
      <div class="dashboard-topbar">
        <div class="dashboard-title">
          <strong>执法人员工作台</strong>
          <span>任务执行、检查记录与整改跟进</span>
        </div>
        <div class="user-chip">
          <span>{{ regulatorUser.username }}</span>
          <span>执法人员</span>
        </div>
      </div>

      <div class="dashboard-content">
        <div class="card dashboard-card">

          <div class="sub-nav">
            <button :class="{ active: section === 'enterprises' }" @click="section = 'enterprises'">企业列表</button>
            <button :class="{ active: section === 'tasks' }" @click="handleTaskEnter">我的任务</button>
            <button :class="{ active: section === 'inspections' }" @click="handleInspectionEnter">检查记录</button>
            <button :class="{ active: section === 'rectification' }" @click="section = 'rectification'">整改跟进</button>
          </div>

          <div v-if="section === 'enterprises'">
            <div class="section-title">企业监管</div>
            <form class="filter-bar filter-bar--triple" @submit.prevent="handleSearch">
              <label>企业名称<input v-model.trim="filters.enterpriseName" placeholder="输入企业名称" /></label>
              <label>企业状态
                <select v-model="filters.status">
                  <option value="">全部</option>
                  <option value="NORMAL">正常</option>
                  <option value="KEY">重点监管</option>
                </select>
              </label>
              <label>审核状态
                <select v-model="filters.approvalStatus">
                  <option value="">全部</option>
                  <option value="PENDING">待审核</option>
                  <option value="APPROVED">已通过</option>
                  <option value="REJECTED">已驳回</option>
                </select>
              </label>
              <button class="primary" type="submit" :disabled="loading">{{ loading ? "查询中..." : "查询" }}</button>
            </form>

            <div class="list-table">
              <div class="list-row list-header">
                <span>企业名称</span><span>状态</span><span>审核</span><span>负责人</span><span>更新时间</span><span>操作</span>
              </div>
              <div v-if="!records.length" class="list-empty">暂无企业数据</div>
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
              <span>共{{ total }} 条，{{ page }}/{{ pages }} 页</span>
              <div class="pager-actions">
                <button class="ghost" type="button" :disabled="page <= 1" @click="changePage(page - 1)">上一页</button>
                <button class="ghost" type="button" :disabled="page >= pages" @click="changePage(page + 1)">下一页</button>
            </div>
          </div>
        </div>
          <div v-else-if="section === 'tasks'">
            <div class="section-title">我的任务</div>
            <form class="filter-bar filter-bar--compact" @submit.prevent="handleTaskSearch">
              <label>任务状态
                <select v-model="taskFilters.status">
                  <option value="">全部</option>
                  <option value="ASSIGNED">待执行</option>
                  <option value="IN_PROGRESS">执行中</option>
                  <option value="COMPLETED">已完成</option>
                </select>
              </label>
              <button class="primary" type="submit" :disabled="taskLoading">{{ taskLoading ? "查询中..." : "查询" }}</button>
            </form>

            <div class="list-table task-table">
              <div class="list-row list-header task-header">
                <span>任务编号</span><span>企业</span><span>优先级</span><span>状态</span><span>截止时间</span><span>操作</span>
              </div>
              <div v-if="!taskRecords.length" class="list-empty">暂无任务</div>
              <div v-for="task in taskRecords" :key="task.id" class="list-row task-row">
                <span>{{ task.taskNo }}</span>
                <span>{{ task.enterpriseName || "-" }}</span>
                <span>{{ formatTaskPriority(task.priority) }}</span>
                <span>{{ formatTaskStatus(task.status) }}</span>
                <span>{{ formatTime(task.deadline) }}</span>
                <div class="action-buttons">
                  <button class="ghost" type="button" :disabled="taskLoading" @click="openTaskDetail(task)">查看详情</button>
                  <button v-if="task.status === 'ASSIGNED'" class="ghost" type="button" :disabled="taskLoading" @click="handleStartTask(task)">开始执行</button>
                  <button v-if="task.status === 'IN_PROGRESS'" class="primary" type="button" @click="handleSelectTask(task)">提交检查</button>
                </div>
              </div>
            </div>

            <div class="pager">
              <span>共{{ taskTotal }} 条，{{ taskPage }}/{{ taskPages }} 页</span>
              <div class="pager-actions">
                <button class="ghost" type="button" :disabled="taskPage <= 1" @click="changeTaskPage(taskPage - 1)">上一页</button>
                <button class="ghost" type="button" :disabled="taskPage >= taskPages" @click="changeTaskPage(taskPage + 1)">下一页</button>
              </div>
            </div>

            <div v-if="activeTask" class="task-submit">
              <div class="section-subtitle">检查结果填报</div>
              <div class="task-meta">
                <span>任务：{{ activeTask.taskNo }}</span>
                <span>企业：{{ activeTask.enterpriseName || "-" }}</span>
              </div>
              <form class="task-form" @submit.prevent="handleSubmitTask">
                <label>检查日期<input v-model="taskForm.inspectionDate" type="date" required /></label>
                <label>检查结果
                  <select v-model="taskForm.result">
                    <option value="PASS">合格</option>
                    <option value="FAIL">不合格</option>
                  </select>
                </label>
                <label>总体问题描述<textarea v-model.trim="taskForm.problemDesc" rows="3" placeholder="如有问题请填写"></textarea></label>
                <div class="task-items">
                  <div class="task-item" v-for="(item, index) in taskForm.items" :key="index">
                    <input v-model.trim="item.itemName" placeholder="检查项" />
                    <select v-model="item.itemResult">
                      <option value="PASS">合格</option>
                      <option value="FAIL">不合格</option>
                    </select>
                    <input v-model.trim="item.problemDesc" placeholder="问题描述（可选）" />
                    <button class="ghost" type="button" @click="removeItem(index)">删除</button>
                  </div>
                  <button class="ghost" type="button" @click="addItem">添加检查项</button>
                </div>
                <div class="task-actions">
                  <button class="primary" type="submit" :disabled="taskLoading">{{ taskLoading ? "提交中..." : "提交结果" }}</button>
                  <button class="ghost" type="button" @click="clearActiveTask">取消</button>
                </div>
              </form>
            </div>

            <div v-if="detailTask" class="modal-mask" @click.self="closeTaskDetail">
              <div class="modal-card">
                <div class="modal-title">任务详情</div>
                <div class="modal-body">
                  <div class="modal-field"><span>任务标题</span><strong>{{ detailTask.taskTitle || "-" }}</strong></div>
                  <div class="modal-field"><span>任务描述</span><strong>{{ detailTask.taskDesc || "-" }}</strong></div>
                </div>
                <div class="modal-actions"><button class="ghost" type="button" @click="closeTaskDetail">关闭</button></div>
              </div>
            </div>
          </div>

          <div v-else-if="section === 'inspections'">
            <div class="section-title">检查记录</div>
            <form class="filter-bar filter-bar--quad" @submit.prevent="handleInspectionSearch">
              <label>企业名称<input v-model.trim="inspectionFilters.enterpriseName" placeholder="输入企业名称" /></label>
              <label>检查结果
                <select v-model="inspectionFilters.result">
                  <option value="">全部</option>
                  <option value="PASS">合格</option>
                  <option value="FAIL">不合格</option>
                </select>
              </label>
              <label>起始日期<input v-model="inspectionFilters.startDate" type="date" /></label>
              <label>截止日期<input v-model="inspectionFilters.endDate" type="date" /></label>
              <button class="primary" type="submit" :disabled="inspectionLoading">{{ inspectionLoading ? "查询中..." : "查询" }}</button>
            </form>

            <div class="list-table inspection-table">
              <div class="list-row list-header inspection-header">
                <span>企业名称</span><span>检查日期</span><span>结果</span><span>更新时间</span><span>操作</span>
              </div>
              <div v-if="!inspectionRecords.length" class="list-empty">暂无检查记录</div>
              <div v-for="record in inspectionRecords" :key="record.id" class="list-row inspection-row">
                <span>{{ record.enterpriseName || "-" }}</span>
                <span>{{ record.inspectionDate || "-" }}</span>
                <span>{{ formatInspectionResult(record.result) }}</span>
                <span>{{ formatTime(record.updateTime) }}</span>
                <button class="ghost" type="button" @click="openInspectionDetail(record)">查看详情</button>
              </div>
            </div>

            <div class="pager">
              <span>共{{ inspectionTotal }} 条，{{ inspectionPage }}/{{ inspectionPages }} 页</span>
              <div class="pager-actions">
                <button class="ghost" type="button" :disabled="inspectionPage <= 1" @click="changeInspectionPage(inspectionPage - 1)">上一页</button>
                <button class="ghost" type="button" :disabled="inspectionPage >= inspectionPages" @click="changeInspectionPage(inspectionPage + 1)">下一页</button>
              </div>
            </div>

            <div v-if="inspectionDetail" class="modal-mask" @click.self="closeInspectionDetail">
              <div class="modal-card">
                <div class="modal-title">检查记录详情</div>
                <div class="modal-body">
                  <div class="modal-field"><span>企业名称</span><strong>{{ inspectionDetail.record.enterpriseName || "-" }}</strong></div>
                  <div class="modal-field"><span>检查日期</span><strong>{{ inspectionDetail.record.inspectionDate || "-" }}</strong></div>
                  <div class="modal-field"><span>检查结果</span><strong>{{ formatInspectionResult(inspectionDetail.record.result) }}</strong></div>
                  <div class="modal-field"><span>问题描述</span><strong>{{ inspectionDetail.record.problemDesc || "-" }}</strong></div>
                  <div class="modal-field">
                    <span>检查明细</span>
                    <div class="modal-list">
                      <div v-if="!inspectionDetail.items.length" class="modal-empty">暂无检查明细</div>
                      <div v-for="(item, index) in inspectionDetail.items" :key="index" class="modal-item">
                        <div class="modal-item-name">{{ item.itemName || "-" }}</div>
                        <div class="modal-item-meta">{{ formatInspectionResult(item.itemResult) }}</div>
                        <div class="modal-item-desc">{{ item.problemDesc || "-" }}</div>
                      </div>
                    </div>
                  </div>
                </div>
                <div class="modal-actions"><button class="ghost" type="button" @click="closeInspectionDetail">关闭</button></div>
              </div>
            </div>
          </div>

          <div v-else-if="section === 'complaints'">
            <div class="section-title">投诉处理</div>
            <form class="filter-bar filter-bar--double" @submit.prevent="handleComplaintSearch">
              <label>投诉状态
                <select v-model="complaintFilters.status">
                  <option value="">全部</option>
                  <option value="ASSIGNED">已派发</option>
                  <option value="PROCESSING">处理中</option>
                  <option value="FEEDBACKED">已反馈</option>
                </select>
              </label>
              <label>企业名称<input v-model.trim="complaintFilters.enterpriseName" placeholder="输入企业名称" /></label>
              <button class="primary" type="submit" :disabled="complaintLoading">{{ complaintLoading ? "查询中..." : "查询" }}</button>
            </form>

            <div class="list-table complaint-table">
              <div class="list-row list-header complaint-header">
                <span>投诉编号</span><span>企业名称</span><span>状态</span><span>处理人</span><span>更新时间</span><span>操作</span>
              </div>
              <div v-if="!complaintRecords.length" class="list-empty">暂无投诉</div>
              <div v-for="item in complaintRecords" :key="item.id" class="list-row complaint-row">
                <span>{{ item.complaintNo }}</span>
                <span>{{ item.enterpriseName || "-" }}</span>
                <span>{{ formatComplaintStatus(item.status) }}</span>
                <span>{{ item.assignedToName || "-" }}</span>
                <span>{{ formatTime(item.updateTime) }}</span>
                <div class="action-buttons">
                  <button class="ghost" type="button" @click="openComplaintDetail(item)">查看详情</button>
                  <button v-if="item.status === 'ASSIGNED'" class="primary" type="button" :disabled="complaintLoading" @click="handleStartComplaint(item)">开始处理</button>
                </div>
              </div>
            </div>

            <div class="pager">
              <span>共{{ complaintTotal }} 条，{{ complaintPage }}/{{ complaintPages }} 页</span>
              <div class="pager-actions">
                <button class="ghost" type="button" :disabled="complaintPage <= 1" @click="changeComplaintPage(complaintPage - 1)">上一页</button>
                <button class="ghost" type="button" :disabled="complaintPage >= complaintPages" @click="changeComplaintPage(complaintPage + 1)">下一页</button>
              </div>
            </div>

            <div v-if="complaintDetail" class="modal-mask" @click.self="closeComplaintDetail">
              <div class="modal-card modal-card--wide">
                <div class="modal-title">投诉详情</div>
                <div class="modal-body">
                  <div class="modal-field"><span>投诉编号</span><strong>{{ complaintDetail.complaint.complaintNo || "-" }}</strong></div>
                  <div class="modal-field"><span>状态</span><strong>{{ formatComplaintStatus(complaintDetail.complaint.status) }}</strong></div>
                  <div class="modal-field"><span>投诉内容</span><strong>{{ complaintDetail.complaint.content || "-" }}</strong></div>
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
                        <div class="modal-item-desc">负责人：{{ complaintDetail.enterprise?.principal || "-" }} {{ complaintDetail.enterprise?.principalPhone || "" }}</div>
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
                  <div v-if="complaintDetail.complaint.status === 'PROCESSING'" class="modal-field">
                    <span>处理结果</span>
                    <textarea v-model.trim="complaintHandleForm.handleResult" rows="3" placeholder="请输入处理结果"></textarea>
                  </div>
                </div>
                <div class="modal-actions">
                  <button v-if="complaintDetail.complaint.status === 'ASSIGNED'" class="primary" type="button" :disabled="complaintLoading" @click="handleStartComplaint(complaintDetail.complaint)">开始处理</button>
                  <button v-if="complaintDetail.complaint.status === 'PROCESSING'" class="primary" type="button" :disabled="complaintLoading" @click="handleSubmitComplaint">提交处理</button>
                  <button class="ghost" type="button" @click="closeComplaintDetail">关闭</button>
                </div>
              </div>
            </div>
          </div>

          <div v-else class="placeholder">
            <strong>功能建设中</strong>
            <p>{{ sectionLabel }} 相关能力正在完善，请稍后再试。</p>
          </div>

          <div class="status" :class="status.type" v-if="status.message">{{ status.message }}</div>
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
  fetchEnterprises,
  fetchComplaintDetail,
  fetchComplaints,
  fetchInspectionRecordDetail,
  fetchMyInspectionRecords,
  fetchMyInspectionTasks,
  handleComplaint,
  startComplaintProcess,
  startInspectionTask,
  submitInspectionTask
} from "../api/regulation";

const props = defineProps({
  token: { type: String, required: true },
  regulatorUser: { type: Object, required: true }
});

const emit = defineEmits(["logout", "view-enterprise"]);
const section = ref("enterprises");

const filters = reactive({ enterpriseName: "", status: "", approvalStatus: "" });
const status = reactive({ message: "", type: "" });
const loading = ref(false);
const records = ref([]);
const page = ref(1);
const size = ref(8);
const total = ref(0);
const pages = ref(1);

const taskLoading = ref(false);
const taskRecords = ref([]);
const taskPage = ref(1);
const taskSize = ref(8);
const taskTotal = ref(0);
const taskPages = ref(1);
const taskFilters = reactive({ status: "" });
const activeTask = ref(null);
const detailTask = ref(null);

const inspectionFilters = reactive({ enterpriseName: "", result: "", startDate: "", endDate: "" });
const inspectionRecords = ref([]);
const inspectionLoading = ref(false);
const inspectionPage = ref(1);
const inspectionSize = ref(8);
const inspectionTotal = ref(0);
const inspectionPages = ref(1);
const inspectionDetail = ref(null);

const complaintLoading = ref(false);
const complaintRecords = ref([]);
const complaintPage = ref(1);
const complaintSize = ref(8);
const complaintTotal = ref(0);
const complaintPages = ref(1);
const complaintFilters = reactive({ status: "", enterpriseName: "" });
const complaintDetail = ref(null);
const complaintHandleForm = reactive({ handleResult: "" });
const imagePreviewUrls = ref([]);
const imagePreviewIndex = ref(0);
const currentImagePreviewUrl = computed(
  () => imagePreviewUrls.value[imagePreviewIndex.value] || ""
);

const taskForm = reactive({
  inspectionDate: "",
  result: "PASS",
  problemDesc: "",
  items: [{ itemName: "", itemResult: "PASS", problemDesc: "" }]
});

function setStatus(message, type = "info") {
  status.message = message;
  status.type = type;
}

const sectionLabelMap = {
  tasks: "我的任务",
  inspections: "检查记录",
  rectification: "整改跟进",
  complaints: "投诉处理"
};

const sectionLabel = computed(() => sectionLabelMap[section.value] || "当前模块");

const statusMap = { NORMAL: "正常", KEY: "重点监管" };
const approvalStatusMap = { PENDING: "待审核", APPROVED: "已通过", REJECTED: "已驳回" };
const taskStatusMap = { CREATED: "待派发", ASSIGNED: "待执行", IN_PROGRESS: "执行中", COMPLETED: "已完成", CLOSED: "已关闭" };
const taskPriorityMap = { LOW: "低", MEDIUM: "中", HIGH: "高" };
const inspectionResultMap = { PASS: "合格", FAIL: "不合格" };
const complaintStatusMap = { SUBMITTED: "已提交", PENDING: "待受理", ASSIGNED: "已派发", PROCESSING: "处理中", FEEDBACKED: "已反馈" };

function formatStatus(value) { return statusMap[value] || value || "-"; }
function formatApprovalStatus(value) { return approvalStatusMap[value] || value || "-"; }
function formatTaskStatus(value) { return taskStatusMap[value] || value || "-"; }
function formatTaskPriority(value) { return taskPriorityMap[value] || value || "-"; }
function formatInspectionResult(value) { return inspectionResultMap[value] || value || "-"; }
function formatComplaintStatus(value) { return complaintStatusMap[value] || value || "-"; }

async function load() {
  loading.value = true; setStatus("");
  try {
    const data = await fetchEnterprises(props.token, { ...filters, page: page.value, size: size.value });
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

async function loadTasks() {
  taskLoading.value = true; setStatus("");
  try {
    const data = await fetchMyInspectionTasks(props.token, { ...taskFilters, page: taskPage.value, size: taskSize.value });
    taskRecords.value = data.records || [];
    taskTotal.value = data.total || 0;
    taskPage.value = data.page || 1;
    taskSize.value = data.size || taskSize.value;
    taskPages.value = data.pages || 1;
  } catch (error) {
    setStatus(error.message || "加载任务列表失败", "error");
  } finally {
    taskLoading.value = false;
  }
}

async function handleTaskSearch() { taskPage.value = 1; await loadTasks(); }
async function changeTaskPage(nextPage) { taskPage.value = nextPage; await loadTasks(); }
async function handleInspectionEnter() { section.value = "inspections"; await loadInspections(); }
async function handleComplaintEnter() { section.value = "complaints"; await loadComplaints(); }
async function handleInspectionSearch() { inspectionPage.value = 1; await loadInspections(); }
async function changeInspectionPage(nextPage) { inspectionPage.value = nextPage; await loadInspections(); }

async function loadInspections() {
  inspectionLoading.value = true; setStatus("");
  try {
    const data = await fetchMyInspectionRecords(props.token, { ...inspectionFilters, page: inspectionPage.value, size: inspectionSize.value });
    inspectionRecords.value = data.records || [];
    inspectionTotal.value = data.total || 0;
    inspectionPage.value = data.page || 1;
    inspectionSize.value = data.size || inspectionSize.value;
    inspectionPages.value = data.pages || 1;
  } catch (error) {
    setStatus(error.message || "加载检查记录失败", "error");
  } finally {
    inspectionLoading.value = false;
  }
}

async function loadComplaints() {
  complaintLoading.value = true; setStatus("");
  try {
    const data = await fetchComplaints(props.token, { ...complaintFilters, page: complaintPage.value, size: complaintSize.value });
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

async function handleComplaintSearch() { complaintPage.value = 1; await loadComplaints(); }
async function changeComplaintPage(nextPage) { complaintPage.value = nextPage; await loadComplaints(); }

async function openComplaintDetail(item) {
  if (!item?.id) return;
  complaintLoading.value = true; setStatus("");
  try {
    complaintDetail.value = await fetchComplaintDetail(props.token, item.id);
    complaintHandleForm.handleResult = "";
  } catch (error) {
    setStatus(error.message || "加载投诉详情失败", "error");
  } finally {
    complaintLoading.value = false;
  }
}

function closeComplaintDetail() {
  complaintDetail.value = null;
  complaintHandleForm.handleResult = "";
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

async function handleStartComplaint(item) {
  if (!item?.id) return;
  complaintLoading.value = true; setStatus("");
  try {
    await startComplaintProcess(props.token, item.id);
    setStatus("投诉已开始处理", "success");
    await loadComplaints();
  } catch (error) {
    setStatus(error.message || "开始处理失败", "error");
  } finally {
    complaintLoading.value = false;
  }
}

async function handleSubmitComplaint() {
  if (!complaintDetail.value?.complaint?.id) return;
  if (!complaintHandleForm.handleResult.trim()) { setStatus("请填写处理结果", "error"); return; }
  complaintLoading.value = true; setStatus("");
  try {
    await handleComplaint(props.token, complaintDetail.value.complaint.id, { handleResult: complaintHandleForm.handleResult });
    setStatus("投诉处理已完成", "success");
    await openComplaintDetail(complaintDetail.value.complaint);
    await loadComplaints();
  } catch (error) {
    setStatus(error.message || "处理投诉失败", "error");
  } finally {
    complaintLoading.value = false;
  }
}

async function handleStartTask(task) {
  taskLoading.value = true; setStatus("");
  try {
    await startInspectionTask(props.token, task.id);
    setStatus("任务已开始执行", "success");
    await loadTasks();
  } catch (error) {
    setStatus(error.message || "开始任务失败", "error");
  } finally {
    taskLoading.value = false;
  }
}

function handleSelectTask(task) {
  activeTask.value = task;
  taskForm.inspectionDate = new Date().toISOString().slice(0, 10);
  taskForm.result = "PASS";
  taskForm.problemDesc = "";
  taskForm.items = [{ itemName: "", itemResult: "PASS", problemDesc: "" }];
}

function clearActiveTask() { activeTask.value = null; }
function openTaskDetail(task) { detailTask.value = task; }
function closeTaskDetail() { detailTask.value = null; }

async function openInspectionDetail(record) {
  if (!record?.id) return;
  inspectionLoading.value = true;
  try {
    inspectionDetail.value = await fetchInspectionRecordDetail(props.token, record.id);
  } catch (error) {
    setStatus(error.message || "加载检查记录失败", "error");
  } finally {
    inspectionLoading.value = false;
  }
}

function closeInspectionDetail() { inspectionDetail.value = null; }

function addItem() { taskForm.items.push({ itemName: "", itemResult: "PASS", problemDesc: "" }); }
function removeItem(index) { if (taskForm.items.length <= 1) return; taskForm.items.splice(index, 1); }

async function handleSubmitTask() {
  if (!activeTask.value) return;
  if (!taskForm.inspectionDate) { setStatus("请选择检查日期", "error"); return; }
  taskLoading.value = true; setStatus("");
  try {
    const items = taskForm.items.filter((item) => item.itemName && item.itemName.trim()).map((item) => ({
      itemName: item.itemName, itemResult: item.itemResult, problemDesc: item.problemDesc
    }));
    await submitInspectionTask(props.token, activeTask.value.id, {
      inspectionDate: taskForm.inspectionDate,
      result: taskForm.result,
      problemDesc: taskForm.problemDesc,
      items
    });
    setStatus("检查结果已提交", "success");
    clearActiveTask();
    await loadTasks();
  } catch (error) {
    setStatus(error.message || "提交结果失败", "error");
  } finally {
    taskLoading.value = false;
  }
}

async function handleSearch() { page.value = 1; await load(); }
async function changePage(nextPage) { page.value = nextPage; await load(); }
async function handleTaskEnter() { section.value = "tasks"; await loadTasks(); }
function handleLogout() { emit("logout"); }
function handleViewDetail(item) { emit("view-enterprise", { id: item.id, fromSection: section.value }); }
function formatTime(value) { if (!value) return "-"; return String(value).replace("T", " ").slice(0, 16); }

onMounted(() => { load(); });
</script>

<style scoped>
.regulator-shell { grid-template-columns: 260px 1fr; }
.regulator-shell .sub-nav { display: none; }
.regulator-shell .hero-panel { padding: 48px 80px 32px; }
.regulator-shell .hero-content h1 { font-size: 34px; }
.regulator-shell .hero-content p { max-width: 720px; font-size: 16px; }
.regulator-shell .hero-highlights { grid-template-columns: repeat(3, minmax(0, 1fr)); }
.regulator-shell .form-panel { display: block; width: 100%; padding: 24px 36px 60px; align-items: stretch; justify-content: flex-start; }
.regulator-shell .card { max-width: 1280px; width: 100%; padding: 28px; }
.sub-nav { display: flex; flex-wrap: wrap; gap: 10px; margin-bottom: 16px; }
.sub-nav button { padding: 8px 14px; border-radius: 999px; border: 1px solid var(--stroke); background: var(--card-strong); color: var(--ink); cursor: pointer; }
.sub-nav button.active { background: var(--nav); border-color: transparent; font-weight: 600; color: #fff; }
.placeholder { border-radius: 12px; border: 1px dashed var(--stroke); padding: 16px; color: var(--muted); font-size: 13px; }
.list-row { --row-columns: 1.6fr 0.9fr 0.9fr 1fr 1.2fr 0.8fr; }
.task-header, .task-row { --row-columns: 1.2fr 1.6fr 0.8fr 0.9fr 1fr 1.2fr; }
.inspection-header, .inspection-row { --row-columns: 1.6fr 1fr 0.8fr 1.2fr 0.8fr; }
.complaint-header, .complaint-row { --row-columns: 1.4fr 1.4fr 0.9fr 0.9fr 1.1fr 1.2fr; }
.modal-card--wide { max-width: 760px; }
.section-subtitle { font-weight: 600; margin-bottom: 8px; }
.task-meta { display: flex; gap: 12px; flex-wrap: wrap; font-size: 12px; color: var(--muted); margin-bottom: 12px; }
.task-form { display: grid; gap: 12px; }
.task-items { display: grid; gap: 10px; }
.task-item { display: grid; grid-template-columns: 1.4fr 0.8fr 1.2fr auto; gap: 8px; align-items: center; }
.task-actions { display: flex; gap: 10px; justify-content: flex-end; }
.modal-list { display: grid; gap: 10px; }
.modal-item { padding: 10px 12px; border-radius: 12px; border: 1px solid var(--stroke); background: var(--card-strong); display: grid; gap: 4px; }
.modal-item-name { font-weight: 600; font-size: 14px; }
.modal-item-meta { font-size: 12px; color: var(--muted); }
.modal-item-desc { font-size: 13px; color: var(--ink); }
.modal-empty { font-size: 12px; color: var(--muted); }
.modal-gallery { display: grid; grid-template-columns: repeat(auto-fit, minmax(120px, 1fr)); gap: 10px; margin-top: 6px; }
.modal-image { display: block; border-radius: 10px; overflow: hidden; border: 1px solid var(--stroke); background: var(--card-strong); padding: 0; cursor: pointer; }
.modal-image img { width: 100%; height: 96px; object-fit: cover; display: block; }
.image-preview-mask { position: fixed; inset: 0; background: rgba(15, 23, 42, 0.55); display: grid; place-items: center; z-index: 9999; }
.image-preview-card { background: #fff; border-radius: 16px; padding: 16px; max-width: min(900px, 92vw); max-height: 88vh; display: grid; gap: 12px; box-shadow: 0 24px 60px rgba(15, 23, 42, 0.25); }
.image-preview-card img { width: 100%; height: auto; max-height: 70vh; object-fit: contain; border-radius: 12px; background: #f6f9ff; }
.image-preview-actions { display: flex; align-items: center; justify-content: center; gap: 10px; flex-wrap: wrap; }
.image-preview-count { font-size: 12px; color: var(--muted); }
@media (max-width: 1024px) { .regulator-shell .hero-panel { padding: 36px 40px 24px; } .regulator-shell .form-panel { padding: 10px 40px 60px; } .regulator-shell .hero-highlights { grid-template-columns: 1fr; } }
@media (max-width: 960px) { .regulator-shell { grid-template-columns: 1fr; } }
@media (max-width: 820px) { .task-item { grid-template-columns: 1fr; } }
</style>
