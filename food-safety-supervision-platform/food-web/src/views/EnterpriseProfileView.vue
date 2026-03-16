<template>
  <div class="admin-shell enterprise-shell">
    <aside class="admin-sidebar">
      <div class="admin-brand">企业中心</div>
      <div class="sidebar-meta">
        <span>账号：{{ enterpriseUser.username }}</span>
        <span>类型：{{ enterpriseUser.userType }}</span>
      </div>
      <nav class="admin-nav">
        <button :class="{ active: section === 'profile' }" @click="section = 'profile'">
          企业备案
        </button>
        <button :class="{ active: section === 'inspections' }" @click="section = 'inspections'">
          检查结果
        </button>
        <button :class="{ active: section === 'rectification' }" @click="handleRectificationEnter">
          整改任务
        </button>
      </nav>
      <button class="ghost sidebar-ghost" type="button" @click="handleLogout">退出登录</button>
    </aside>

    <div class="admin-main">
      <div class="dashboard-topbar">
        <div class="dashboard-title">
          <strong>企业用户工作台</strong>
          <span>备案维护、检查结果与整改跟进</span>
        </div>
        <div class="user-chip">
          <span>{{ enterpriseUser.username }}</span>
          <span>企业用户</span>
        </div>
      </div>

      <div class="dashboard-content">
        <div class="card dashboard-card">
          <div v-if="section === 'profile'">
            <div class="section-title">企业备案</div>

            <div class="status-banner" :class="statusTone">
              <div class="status-title">当前审核状态：{{ statusLabel }}</div>
              <div v-if="profile.approvalComment" class="status-note">
                审核意见：{{ profile.approvalComment }}
              </div>
              <div v-if="profile.approvedTime" class="status-note">
                审核时间：{{ profile.approvedTime }}
              </div>
              <div v-if="!profileLoaded" class="status-note">
                暂无备案记录，请先提交企业信息。
              </div>
            </div>

            <form @submit.prevent="handleSubmit">
              <label>
                企业名称
                <input v-model.trim="form.enterpriseName" required placeholder="请输入企业名称" />
              </label>
              <label>
                许可证编号
                <input v-model.trim="form.licenseNo" placeholder="请输入许可证编号" />
              </label>
              <label>
                省份
                <select v-model="regionSelection.provinceId" @change="handleProvinceChange">
                  <option value="">请选择省</option>
                  <option v-for="item in regionOptions.provinces" :key="item.id" :value="item.id">
                    {{ item.name }}
                  </option>
                </select>
              </label>
              <label>
                城市
                <select v-model="regionSelection.cityId" :disabled="!regionSelection.provinceId" @change="handleCityChange">
                  <option value="">请选择市</option>
                  <option v-for="item in regionOptions.cities" :key="item.id" :value="item.id">
                    {{ item.name }}
                  </option>
                </select>
              </label>
              <label>
                区县
                <select
                  v-model="regionSelection.countyId"
                  :disabled="!regionSelection.cityId"
                  @change="handleCountyChange"
                >
                  <option value="">请选择区县</option>
                  <option v-for="item in regionOptions.counties" :key="item.id" :value="item.id">
                    {{ item.name }}
                  </option>
                </select>
              </label>
              <label>
                街道
                <select v-model="regionSelection.streetId" :disabled="!regionSelection.countyId">
                  <option value="">请选择街道</option>
                  <option v-for="item in regionOptions.streets" :key="item.id" :value="item.id">
                    {{ item.name }}
                  </option>
                </select>
              </label>
              <div v-if="existingRegionText && !regionSelection.provinceId" class="hint">
                当前行政区：{{ existingRegionText }}
              </div>
              <label>
                详细地址
                <input v-model.trim="form.addressDetail" required placeholder="请输入详细地址" />
              </label>
              <label>
                负责人姓名
                <input v-model.trim="form.principal" placeholder="请输入负责人姓名" />
              </label>
              <label>
                负责人电话
                <input v-model.trim="form.principalPhone" placeholder="11 位手机号" />
              </label>
              <button class="primary" type="submit" :disabled="loading">
                {{ loading ? "提交中..." : submitLabel }}
              </button>
            </form>

            <div class="status" :class="status.type" v-if="status.message">
              {{ status.message }}
            </div>
          </div>

          <div v-else-if="section === 'rectification'">
            <div class="section-title">整改任务</div>
            <form class="filter-bar filter-bar--triple" @submit.prevent="handleRectificationSearch">
              <label>
                状态
                <select v-model="rectificationFilters.status">
                  <option value="">全部</option>
                  <option value="ONGOING">整改中</option>
                  <option value="SUBMITTED">待复核</option>
                  <option value="REWORK">打回重做</option>
                  <option value="CONFIRMED">已确认</option>
                </select>
              </label>
              <button class="primary" type="submit" :disabled="rectificationLoading">
                {{ rectificationLoading ? "查询中..." : "查询" }}
              </button>
            </form>

            <div class="list-table">
              <div class="list-row list-header rectification-header">
                <span>整改任务</span>
                <span>状态</span>
                <span>整改时限</span>
                <span>更新时间</span>
                <span>操作</span>
              </div>
              <div v-if="!rectificationRecords.length" class="list-empty">
                暂无整改任务
              </div>
              <div v-for="item in rectificationRecords" :key="item.id" class="list-row rectification-row">
                <div class="rectification-desc" :title="item.rectificationDesc || '-'">
                  {{ item.rectificationDesc || "-" }}
                </div>
                <div class="rectification-status-cell">
                  <span>{{ formatRectificationStatus(item.status) }}</span>
                  <button
                    v-if="rectificationHasReworkMap[item.id]"
                    class="rework-flag"
                    type="button"
                    @click="openLatestReworkDetail(item)"
                  >
                    有打回意见
                  </button>
                </div>
                <span :class="['rectification-sla', `rectification-sla--${rectificationSlaClass(item)}`]">
                  {{ formatRectificationSla(item) }}
                </span>
                <span>{{ formatTime(item.updateTime) }}</span>
                <div class="rectification-action">
                  <button class="ghost" type="button" @click="openRectificationDetail(item)">
                    查看详情
                  </button>
                  <template v-if="item.status === 'ONGOING' || item.status === 'REWORK'">
                    <div class="rectification-submit-inline">
                      <input
                        v-model.trim="rectificationDrafts[item.id]"
                        placeholder="请输入整改进展说明"
                        :disabled="rectificationLoading"
                      />
                      <label class="ghost rectification-upload-trigger">
                        上传凭证
                        <input
                          type="file"
                          accept="image/*"
                          multiple
                          :disabled="rectificationLoading || isRectificationUploading(item.id)"
                          @change="handleRectificationFileChange(item.id, $event)"
                        />
                      </label>
                      <button
                        class="primary"
                        type="button"
                        :disabled="rectificationLoading || isRectificationUploading(item.id)"
                        @click="handleSubmitRectification(item)"
                      >
                        提交整改
                      </button>
                    </div>
                    <div
                      v-if="getRectificationUploadItems(item.id).length"
                      class="rectification-upload-list"
                    >
                      <div
                        v-for="upload in getRectificationUploadItems(item.id)"
                        :key="upload.id"
                        class="rectification-upload-item"
                      >
                        <img :src="upload.previewUrl" :alt="upload.name" />
                        <div class="rectification-upload-meta" :class="{ error: upload.error }">
                          <span v-if="upload.uploading">上传中...</span>
                          <span v-else-if="upload.error">{{ upload.error }}</span>
                          <span v-else>已上传</span>
                          <button
                            v-if="upload.error"
                            class="ghost"
                            type="button"
                            @click="retryRectificationUpload(item.id, upload.id)"
                          >
                            重试
                          </button>
                          <button
                            class="ghost"
                            type="button"
                            @click="removeRectificationUpload(item.id, upload.id)"
                          >
                            删除
                          </button>
                        </div>
                      </div>
                    </div>
                  </template>
                  <span v-else class="secondary-text">无需操作</span>
                </div>
              </div>
            </div>

            <div class="pager">
              <span>共 {{ rectificationTotal }} 条，{{ rectificationPage }}/{{ rectificationPages }} 页</span>
              <div class="pager-actions">
                <button
                  class="ghost"
                  type="button"
                  :disabled="rectificationPage <= 1"
                  @click="changeRectificationPage(rectificationPage - 1)"
                >
                  上一页
                </button>
                <button
                  class="ghost"
                  type="button"
                  :disabled="rectificationPage >= rectificationPages"
                  @click="changeRectificationPage(rectificationPage + 1)"
                >
                  下一页
                </button>
              </div>
            </div>

            <RectificationDetailModal
              :visible="rectificationDetailVisible"
              :detail="rectificationDetail"
              :action-logs="rectificationActionLogs"
              :detail-loading="rectificationDetailLoading"
              :highlight-latest-rework="true"
              :focus-action-type="rectificationFocusActionType"
              :reviewable="false"
              :reviewing="false"
              @close="closeRectificationDetail"
            />
          </div>

          <div v-else class="placeholder">
            <strong>功能占位</strong>
            <p>{{ sectionLabel }} 将在后续版本实现。</p>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, reactive, ref } from "vue";
import { presignUpload } from "../api/file";
import {
  fetchEnterpriseProfile,
  fetchMyRectifications,
  fetchRectificationActions,
  fetchRectificationDetail,
  fetchRegions,
  submitEnterpriseProfile,
  submitMyRectification
} from "../api/regulation";
import RectificationDetailModal from "../components/RectificationDetailModal.vue";

const props = defineProps({
  token: {
    type: String,
    required: true
  },
  enterpriseUser: {
    type: Object,
    required: true
  }
});

const emit = defineEmits(["logout"]);

const section = ref("profile");
const loading = ref(false);
const profileLoaded = ref(false);
const status = reactive({ message: "", type: "" });
const existingRegionId = ref(null);
const existingRegionText = ref("");
const existingRegionPath = ref([]);
const profile = reactive({
  approvalStatus: "",
  approvalComment: "",
  approvedTime: ""
});

const regionOptions = reactive({
  provinces: [],
  cities: [],
  counties: [],
  streets: []
});

const regionSelection = reactive({
  provinceId: "",
  cityId: "",
  countyId: "",
  streetId: ""
});

const form = reactive({
  enterpriseName: "",
  licenseNo: "",
  addressDetail: "",
  principal: "",
  principalPhone: ""
});
const rectificationLoading = ref(false);
const rectificationRecords = ref([]);
const rectificationPage = ref(1);
const rectificationSize = ref(8);
const rectificationTotal = ref(0);
const rectificationPages = ref(1);
const rectificationFilters = reactive({
  status: ""
});
const rectificationDrafts = reactive({});
const rectificationDetailVisible = ref(false);
const rectificationDetail = ref(null);
const rectificationActionLogs = ref([]);
const rectificationDetailLoading = ref(false);
const rectificationHasReworkMap = reactive({});
const rectificationFocusActionType = ref("");
const rectificationUploadItems = reactive({});

const RECTIFICATION_ALLOWED_TYPES = ["image/jpeg", "image/png", "image/webp"];
const RECTIFICATION_MAX_FILE_SIZE = 5 * 1024 * 1024;
const RECTIFICATION_MAX_FILE_COUNT = 6;

const sectionLabelMap = {
  inspections: "检查结果",
  rectification: "整改任务"
};

const sectionLabel = computed(() => sectionLabelMap[section.value] || "当前模块");
const rectificationStatusMap = {
  ONGOING: "整改中",
  SUBMITTED: "待复核",
  REWORK: "打回重做",
  CONFIRMED: "已确认"
};

const statusLabel = computed(() => {
  if (!profileLoaded.value) return "未提交";
  if (profile.approvalStatus === "APPROVED") return "已通过";
  if (profile.approvalStatus === "REJECTED") return "已驳回";
  if (profile.approvalStatus === "PENDING") return "待审核";
  return "未提交";
});

const statusTone = computed(() => {
  if (!profileLoaded.value) return "info";
  if (profile.approvalStatus === "APPROVED") return "success";
  if (profile.approvalStatus === "REJECTED") return "error";
  return "pending";
});

const submitLabel = computed(() => (profileLoaded.value ? "更新并重新提交" : "提交备案"));

function setStatus(message, type = "info") {
  status.message = message;
  status.type = type;
}

function resetForm(payload = {}) {
  form.enterpriseName = payload.enterpriseName || "";
  form.licenseNo = payload.licenseNo || "";
  form.addressDetail = payload.addressDetail || "";
  form.principal = payload.principal || "";
  form.principalPhone = payload.principalPhone || "";
}

async function loadProfile() {
  try {
    const data = await fetchEnterpriseProfile(props.token);
    profile.approvalStatus = data.approvalStatus || "";
    profile.approvalComment = data.approvalComment || "";
    profile.approvedTime = data.approvedTime || "";
    existingRegionId.value = data.regionId || null;
    existingRegionText.value = data.regionPathText || "";
    existingRegionPath.value = Array.isArray(data.regionPath) ? data.regionPath : [];
    resetForm(data);
    profileLoaded.value = true;
    if (existingRegionPath.value.length) {
      await applyRegionPath(existingRegionPath.value);
    }
  } catch (error) {
    if (String(error?.message).includes("not found")) {
      profileLoaded.value = false;
      existingRegionId.value = null;
      existingRegionText.value = "";
      existingRegionPath.value = [];
      resetForm();
      return;
    }
    setStatus(error.message || "加载失败", "error");
  }
}

async function handleSubmit() {
  loading.value = true;
  setStatus("");
  try {
    const regionId = resolveEnterpriseRegionId();
    if (!regionId) {
      setStatus("请选择所属行政区", "error");
      return;
    }
    const data = await submitEnterpriseProfile(props.token, { ...form, regionId });
    profile.approvalStatus = data.approvalStatus || "PENDING";
    profile.approvalComment = data.approvalComment || "";
    profile.approvedTime = data.approvedTime || "";
    existingRegionId.value = data.regionId || existingRegionId.value;
    existingRegionText.value = data.regionPathText || existingRegionText.value;
    existingRegionPath.value = Array.isArray(data.regionPath) ? data.regionPath : existingRegionPath.value;
    profileLoaded.value = true;
    setStatus("提交成功，已进入审核流程。", "success");
  } catch (error) {
    setStatus(error.message || "提交失败", "error");
  } finally {
    loading.value = false;
  }
}

function handleLogout() {
  emit("logout");
}

function formatRectificationStatus(value) {
  return rectificationStatusMap[value] || value || "-";
}

function formatTime(value) {
  if (!value) return "-";
  return String(value).replace("T", " ").slice(0, 16);
}

function formatDurationMinutes(minutes) {
  const total = Math.max(0, Number(minutes) || 0);
  const days = Math.floor(total / (24 * 60));
  const hours = Math.floor((total % (24 * 60)) / 60);
  const mins = total % 60;
  if (days > 0) return `${days}天${hours}小时`;
  if (hours > 0) return `${hours}小时${mins}分钟`;
  return `${mins}分钟`;
}

function rectificationSlaClass(item) {
  if (!item) return "none";
  if (item.slaStatus === "OVERDUE") return "overdue";
  if (item.slaStatus === "DUE_SOON") return "warning";
  if (item.slaStatus === "NORMAL") return "normal";
  return "none";
}

function formatRectificationSla(item) {
  if (!item) return "-";
  const remaining = Number(item.remainingMinutes);
  if (item.slaStatus === "OVERDUE") {
    return `已超时 ${formatDurationMinutes(Math.abs(remaining))}`;
  }
  if (item.slaStatus === "DUE_SOON") {
    return `即将超时 ${formatDurationMinutes(remaining)}`;
  }
  if (item.slaStatus === "NORMAL") {
    return `剩余 ${formatDurationMinutes(remaining)}`;
  }
  if (item.currentDeadline) {
    return `截止 ${formatTime(item.currentDeadline)}`;
  }
  return "已完成";
}

function ensureRectificationUploadBucket(taskId) {
  if (!taskId) return;
  if (!Array.isArray(rectificationUploadItems[taskId])) {
    rectificationUploadItems[taskId] = [];
  }
}

function getRectificationUploadItems(taskId) {
  return Array.isArray(rectificationUploadItems[taskId]) ? rectificationUploadItems[taskId] : [];
}

function isRectificationUploading(taskId) {
  return getRectificationUploadItems(taskId).some((item) => item.uploading);
}

function validateRectificationFile(file) {
  if (!RECTIFICATION_ALLOWED_TYPES.includes(file.type)) {
    return "仅支持 JPG/PNG/WebP 图片";
  }
  if (file.size > RECTIFICATION_MAX_FILE_SIZE) {
    return "单张图片不能超过 5MB";
  }
  return "";
}

function createRectificationUploadItem(file) {
  return {
    id: `${Date.now()}-${Math.random().toString(16).slice(2)}`,
    name: file.name,
    file,
    previewUrl: URL.createObjectURL(file),
    fileUrl: "",
    uploading: true,
    error: ""
  };
}

async function handleRectificationFileChange(taskId, event) {
  if (!taskId) return;
  const files = Array.from(event?.target?.files || []);
  if (!files.length) return;
  ensureRectificationUploadBucket(taskId);
  const currentItems = getRectificationUploadItems(taskId);
  const remaining = RECTIFICATION_MAX_FILE_COUNT - currentItems.length;
  if (remaining <= 0) {
    setStatus(`最多上传 ${RECTIFICATION_MAX_FILE_COUNT} 张图片`, "error");
    event.target.value = "";
    return;
  }
  const selectedFiles = files.slice(0, remaining);
  selectedFiles.forEach((file) => {
    const error = validateRectificationFile(file);
    if (error) {
      setStatus(error, "error");
      return;
    }
    const uploadItem = createRectificationUploadItem(file);
    rectificationUploadItems[taskId] = [...getRectificationUploadItems(taskId), uploadItem];
    uploadRectificationFile(taskId, uploadItem);
  });
  event.target.value = "";
}

function removeRectificationUpload(taskId, uploadId) {
  const target = getRectificationUploadItems(taskId).find((item) => item.id === uploadId);
  if (target?.previewUrl) {
    URL.revokeObjectURL(target.previewUrl);
  }
  rectificationUploadItems[taskId] = getRectificationUploadItems(taskId).filter((item) => item.id !== uploadId);
}

function retryRectificationUpload(taskId, uploadId) {
  const target = getRectificationUploadItems(taskId).find((item) => item.id === uploadId);
  if (!target || !target.file) return;
  target.error = "";
  target.uploading = true;
  uploadRectificationFile(taskId, target);
}

async function uploadRectificationFile(taskId, uploadItem) {
  try {
    const payload = {
      filename: uploadItem.name,
      contentType: uploadItem.file.type || "application/octet-stream",
      size: uploadItem.file.size,
      bizType: "RECTIFICATION"
    };
    const presign = await presignUpload(props.token, payload);
    const response = await fetch(presign.uploadUrl, {
      method: "PUT",
      headers: {
        "Content-Type": payload.contentType
      },
      body: uploadItem.file
    });
    if (!response.ok) {
      throw new Error(`上传失败 (${response.status})`);
    }
    uploadItem.fileUrl = presign.fileUrl;
    uploadItem.uploading = false;
    uploadItem.error = "";
    rectificationUploadItems[taskId] = [...getRectificationUploadItems(taskId)];
  } catch (error) {
    uploadItem.uploading = false;
    uploadItem.error = error.message || "上传失败";
    rectificationUploadItems[taskId] = [...getRectificationUploadItems(taskId)];
  }
}

function getRectificationAttachmentUrls(taskId) {
  return getRectificationUploadItems(taskId)
    .map((item) => item.fileUrl)
    .filter(Boolean);
}

function clearRectificationUploadBucket(taskId) {
  getRectificationUploadItems(taskId).forEach((item) => {
    if (item.previewUrl) {
      URL.revokeObjectURL(item.previewUrl);
    }
  });
  delete rectificationUploadItems[taskId];
}

function clearAllRectificationUploadBuckets() {
  Object.keys(rectificationUploadItems).forEach((key) => {
    clearRectificationUploadBucket(key);
  });
}

function resetRectificationReworkFlags() {
  Object.keys(rectificationHasReworkMap).forEach((key) => {
    delete rectificationHasReworkMap[key];
  });
}

async function loadRectificationReworkFlags(records) {
  resetRectificationReworkFlags();
  if (!Array.isArray(records) || !records.length) {
    return;
  }
  await Promise.all(
    records.map(async (item) => {
      if (!item?.id) return;
      try {
        const actions = await fetchRectificationActions(props.token, item.id);
        rectificationHasReworkMap[item.id] = Array.isArray(actions)
          && actions.some((log) => String(log?.actionType || "").toUpperCase() === "REVIEW_REWORK");
      } catch {
        // 拉取失败时降级为仅根据当前状态判断，避免阻断列表渲染。
        rectificationHasReworkMap[item.id] = item.status === "REWORK";
      }
    })
  );
}

async function handleRectificationEnter() {
  section.value = "rectification";
  await loadRectifications();
}

async function loadRectifications() {
  rectificationLoading.value = true;
  setStatus("");
  try {
    const data = await fetchMyRectifications(props.token, {
      ...rectificationFilters,
      page: rectificationPage.value,
      size: rectificationSize.value
    });
    rectificationRecords.value = data.records || [];
    rectificationTotal.value = data.total || 0;
    rectificationPage.value = data.page || 1;
    rectificationSize.value = data.size || rectificationSize.value;
    rectificationPages.value = data.pages || 1;
    await loadRectificationReworkFlags(rectificationRecords.value);
    // 弹窗打开时，同步刷新当前详情，确保动作时间线实时一致。
    if (rectificationDetailVisible.value && rectificationDetail.value?.id) {
      await loadRectificationDetail(rectificationDetail.value.id, true);
    }
  } catch (error) {
    setStatus(error.message || "加载整改任务失败", "error");
  } finally {
    rectificationLoading.value = false;
  }
}

async function loadRectificationDetail(id, silent = false) {
  if (!id) return;
  if (!silent) {
    rectificationDetailLoading.value = true;
  }
  try {
    const [detail, actions] = await Promise.all([
      fetchRectificationDetail(props.token, id),
      fetchRectificationActions(props.token, id)
    ]);
    rectificationDetail.value = detail || rectificationDetail.value;
    rectificationActionLogs.value = Array.isArray(actions) ? actions : [];
  } catch (error) {
    if (!silent) {
      setStatus(error.message || "加载整改详情失败", "error");
    }
  } finally {
    if (!silent) {
      rectificationDetailLoading.value = false;
    }
  }
}

async function handleRectificationSearch() {
  rectificationPage.value = 1;
  await loadRectifications();
}

async function changeRectificationPage(nextPage) {
  rectificationPage.value = nextPage;
  await loadRectifications();
}

async function handleSubmitRectification(item) {
  if (!item?.id) return;
  const progress = String(rectificationDrafts[item.id] || "").trim();
  if (!progress) {
    setStatus("请先填写整改进展说明", "error");
    return;
  }
  const uploadItems = getRectificationUploadItems(item.id);
  if (uploadItems.some((upload) => upload.uploading)) {
    setStatus("整改凭证上传中，请稍后提交", "error");
    return;
  }
  if (uploadItems.some((upload) => upload.error)) {
    setStatus("存在上传失败的整改凭证，请处理后再提交", "error");
    return;
  }
  const attachmentUrls = getRectificationAttachmentUrls(item.id);
  rectificationLoading.value = true;
  setStatus("");
  try {
    await submitMyRectification(props.token, item.id, {
      progress,
      attachmentUrls: attachmentUrls.length ? attachmentUrls : undefined
    });
    setStatus("整改进展提交成功，等待监管复核", "success");
    rectificationDrafts[item.id] = "";
    clearRectificationUploadBucket(item.id);
    await loadRectifications();
  } catch (error) {
    setStatus(error.message || "整改提交失败", "error");
  } finally {
    rectificationLoading.value = false;
  }
}

async function openRectificationDetail(item, focusActionType = "") {
  if (!item) return;
  rectificationDetailLoading.value = true;
  rectificationDetail.value = item;
  rectificationActionLogs.value = [];
  rectificationFocusActionType.value = focusActionType;
  rectificationDetailVisible.value = true;
  await loadRectificationDetail(item.id);
}

function openLatestReworkDetail(item) {
  openRectificationDetail(item, "REVIEW_REWORK");
}

function closeRectificationDetail() {
  rectificationDetailVisible.value = false;
  rectificationDetail.value = null;
  rectificationActionLogs.value = [];
  rectificationDetailLoading.value = false;
  rectificationFocusActionType.value = "";
}

async function loadRegions(parentId, targetKey) {
  try {
    regionOptions[targetKey] = await fetchRegions(props.token, parentId);
  } catch (error) {
    setStatus(error.message || "加载行政区失败", "error");
  }
}

async function applyRegionPath(path) {
  if (!path || !path.length) {
    return;
  }
  const province = path[0];
  regionSelection.provinceId = province?.id ? String(province.id) : "";
  resetRegion("province");
  if (province?.id) {
    await loadRegions(province.id, "cities");
  }
  const city = path[1];
  if (city?.id) {
    regionSelection.cityId = String(city.id);
    await loadRegions(city.id, "counties");
  }
  const county = path[2];
  if (county?.id) {
    regionSelection.countyId = String(county.id);
    await loadRegions(county.id, "streets");
  }
  const street = path[3];
  if (street?.id) {
    regionSelection.streetId = String(street.id);
  }
}

function resetRegion(level) {
  if (level === "province") {
    regionSelection.cityId = "";
    regionSelection.countyId = "";
    regionSelection.streetId = "";
    regionOptions.cities = [];
    regionOptions.counties = [];
    regionOptions.streets = [];
  } else if (level === "city") {
    regionSelection.countyId = "";
    regionSelection.streetId = "";
    regionOptions.counties = [];
    regionOptions.streets = [];
  } else if (level === "county") {
    regionSelection.streetId = "";
    regionOptions.streets = [];
  }
}

async function handleProvinceChange() {
  resetRegion("province");
  const provinceId = Number(regionSelection.provinceId || 0);
  if (!provinceId) return;
  await loadRegions(provinceId, "cities");
}

async function handleCityChange() {
  resetRegion("city");
  const cityId = Number(regionSelection.cityId || 0);
  if (!cityId) return;
  await loadRegions(cityId, "counties");
}

async function handleCountyChange() {
  resetRegion("county");
  const countyId = Number(regionSelection.countyId || 0);
  if (!countyId) return;
  await loadRegions(countyId, "streets");
}

function resolveEnterpriseRegionId() {
  if (regionOptions.streets.length) {
    return Number(regionSelection.streetId || 0) || null;
  }
  if (regionOptions.counties.length) {
    return Number(regionSelection.countyId || 0) || null;
  }
  if (regionOptions.cities.length) {
    return Number(regionSelection.cityId || 0) || null;
  }
  return Number(regionSelection.provinceId || 0) || existingRegionId.value || null;
}

onBeforeUnmount(() => {
  clearAllRectificationUploadBuckets();
});

onMounted(() => {
  const init = async () => {
    await loadRegions(null, "provinces");
    await loadProfile();
  };
  init();
});
</script>

<style scoped>
.status-banner {
  border-radius: 16px;
  padding: 14px 16px;
  margin-bottom: 18px;
  background: var(--card-strong);
  border: 1px solid var(--stroke);
  color: var(--ink);
  display: grid;
  gap: 6px;
}

.status-banner.success {
  background: rgba(26, 127, 90, 0.12);
  color: var(--success);
}

.status-banner.error {
  background: rgba(192, 57, 43, 0.12);
  color: var(--danger);
}

.status-banner.pending {
  background: rgba(209, 122, 0, 0.12);
  color: var(--warning);
}

.status-title {
  font-weight: 600;
  font-size: 14px;
}

.status-note {
  font-size: 12px;
  color: inherit;
}

.hint {
  font-size: 12px;
  color: var(--muted);
  margin-top: -6px;
}

.enterprise-shell .admin-info {
  margin-bottom: 16px;
}

.rectification-header,
.rectification-row {
  --row-columns: 1.8fr 0.8fr 1fr 1fr 2fr;
}

.rectification-desc {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.rectification-status-cell {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.rework-flag {
  display: inline-flex;
  align-items: center;
  width: fit-content;
  border: 1px solid rgba(204, 122, 0, 0.3);
  padding: 2px 8px;
  border-radius: 999px;
  font-size: 11px;
  color: #8a4f00;
  background: rgba(204, 122, 0, 0.14);
  cursor: pointer;
}

.rework-flag:hover {
  background: rgba(204, 122, 0, 0.22);
}

.rectification-sla {
  font-size: 12px;
  font-weight: 600;
}

.rectification-sla--normal {
  color: #0d4f9b;
}

.rectification-sla--warning {
  color: #b36b00;
}

.rectification-sla--overdue {
  color: var(--danger);
}

.rectification-sla--none {
  color: var(--muted);
}

.rectification-action {
  display: flex;
  flex-direction: column;
  gap: 8px;
  align-items: stretch;
}

.rectification-submit-inline {
  display: grid;
  grid-template-columns: 1fr auto auto;
  gap: 8px;
  align-items: center;
}

.rectification-submit-inline .primary {
  min-width: 100px;
  margin-top: 0;
}

.rectification-upload-trigger {
  position: relative;
  overflow: hidden;
  cursor: pointer;
}

.rectification-upload-trigger input {
  position: absolute;
  inset: 0;
  opacity: 0;
  cursor: pointer;
}

.rectification-upload-list {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(96px, 1fr));
  gap: 8px;
}

.rectification-upload-item {
  border: 1px solid var(--stroke);
  border-radius: 10px;
  padding: 6px;
  background: var(--card-strong);
}

.rectification-upload-item img {
  width: 100%;
  height: 72px;
  object-fit: cover;
  border-radius: 6px;
  display: block;
}

.rectification-upload-meta {
  margin-top: 6px;
  display: grid;
  gap: 4px;
  font-size: 12px;
  color: var(--muted);
}

.rectification-upload-meta.error {
  color: var(--danger);
}

.rectification-upload-meta .ghost {
  padding: 3px 8px;
  min-height: 24px;
  border-radius: 8px;
  width: 100%;
}

.enterprise-shell {
  grid-template-columns: 260px 1fr;
}

@media (max-width: 960px) {
  .rectification-header,
  .rectification-row {
    --row-columns: 1fr;
  }

  .rectification-action {
    align-items: stretch;
  }

  .rectification-submit-inline {
    grid-template-columns: 1fr;
  }

  .enterprise-shell {
    grid-template-columns: 1fr;
  }
}
</style>
