<template>
  <EnterpriseWorkspacePage
    active-key="profile"
    title="企业备案"
    subtitle="完善企业主体资料、附件材料与备案信息。"
    top-search-placeholder="搜索企业或档案..."
    :username="enterpriseUser.username"
    :user-type="enterpriseUser.userType"
    :status-label="statusLabel"
    :status-tone="statusTone"
    @navigate="handleSidebarNavigate"
    @logout="handleLogout"
  >
    <div class="enterprise-workspace-card">
      <nav class="enterprise-page-hero__crumb" style="margin-bottom: 18px" aria-label="面包屑">
        <span>企业工作台</span>
        <span class="material-symbols-outlined" aria-hidden="true">chevron_right</span>
        <span class="is-current">企业备案</span>
      </nav>

      <section class="enterprise-profile-registrar">
        <div class="enterprise-profile-registrar__gradient">
          <div style="display: flex; align-items: center; gap: 20px; flex: 1; min-width: 0">
            <div class="enterprise-profile-registrar__icon">
              <span class="material-symbols-outlined is-filled" aria-hidden="true">verified</span>
            </div>
            <div>
              <h1 class="enterprise-profile-registrar__title">企业备案详情</h1>
              <p class="enterprise-profile-registrar__meta">
                统一社会信用代码：{{ form.creditCode || "待填写" }}
              </p>
            </div>
          </div>
          <div class="enterprise-profile-registrar__status">
            <EnterpriseStatusChip :label="statusLabel" :tone="statusTone" />
            <p v-if="profile.approvedTime" class="enterprise-profile-registrar__audit-meta">
              上次审核日期 {{ formatTime(profile.approvedTime) }}
            </p>
            <RouterLink class="enterprise-profile-registrar__cta" :to="{ name: 'enterprise-profile-detail' }">只读详情页</RouterLink>
          </div>
        </div>
        </section>

        <section v-if="profileLoaded && (profile.approvedByName || profile.approvedTime || profile.regulatorName || profile.approvalComment)" class="enterprise-panel">
          <div class="enterprise-panel__head">
            <div class="enterprise-panel__head-bar" />
            <h3>审核信息</h3>
          </div>
          <div class="enterprise-detail-grid">
            <div class="enterprise-readonly-field">
              <span>审核人</span>
              <div>{{ profile.approvedByName || "-" }}</div>
            </div>
            <div class="enterprise-readonly-field">
              <span>审核时间</span>
              <div>{{ formatTime(profile.approvedTime) }}</div>
            </div>
            <div class="enterprise-readonly-field">
              <span>包保责任人</span>
              <div>{{ profile.regulatorName || "-" }}</div>
            </div>
            <div v-if="profile.approvalComment" class="enterprise-readonly-field enterprise-detail-item--full">
              <span>审核意见</span>
              <div>{{ profile.approvalComment }}</div>
            </div>
          </div>
        </section>

        <section class="enterprise-panel enterprise-key-supervision-panel">
          <div class="enterprise-panel__head">
            <div class="enterprise-panel__head-bar" />
            <h3>重点监管状态</h3>
          </div>
          <div class="enterprise-key-supervision-panel__summary">
            <div>
              <strong>{{ keySupervisionTitle }}</strong>
              <p>{{ keySupervisionDescription }}</p>
            </div>
            <span
              class="enterprise-key-supervision-panel__badge"
              :class="{ 'is-key': isKeyEnterprise, 'is-normal': !isKeyEnterprise }"
            >
              {{ isKeyEnterprise ? "KEY" : "NORMAL" }}
            </span>
          </div>
          <div v-if="keyReasonItems.length" class="enterprise-key-supervision-panel__list">
            <article
              v-for="(item, index) in keyReasonItems"
              :key="`${item.reasonType || 'reason'}-${index}`"
              class="enterprise-key-supervision-panel__item"
            >
              <div class="enterprise-key-supervision-panel__item-head">
                <strong>{{ item.reasonLabel || formatKeyReasonType(item.reasonType) }}</strong>
                <span>{{ formatTime(item.createTime) }}</span>
              </div>
              <p>{{ item.reasonDetail || "已触发重点监管规则。" }}</p>
            </article>
          </div>
          <div v-else class="enterprise-key-supervision-panel__empty">
            当前暂无已记录的重点监管原因；若后续触发风险规则或被监管确认，将在这里展示。
          </div>
        </section>

        <div class="enterprise-detail-layout">
        <form id="enterprise-profile-form-anchor" @submit.prevent="handleSubmit">
          <div class="enterprise-panel enterprise-panel--accent-top">
            <div class="enterprise-panel__head" style="justify-content: space-between">
              <div style="display: flex; align-items: center; gap: 10px">
                <div class="enterprise-panel__head-bar" />
                <h3>基础信息表单</h3>
              </div>
              <a class="ghost enterprise-inline-link" style="font-size: 12px" href="#enterprise-profile-form-anchor" @click.prevent="scrollToForm">在表单内修改</a>
            </div>
            <p v-if="profile.approvalComment" class="secondary-text" style="margin: 0 0 16px; font-size: 13px">
              审核意见：{{ profile.approvalComment }}
            </p>
            <p v-if="!profileLoaded" class="secondary-text" style="margin: 0 0 16px">
              当前还没有备案记录，请先提交企业资料。
            </p>
            <div class="enterprise-form-section" style="box-shadow: none; border: none; padding: 0; margin: 0">
              <div class="enterprise-detail-grid">
                <label>
                  企业名称
                  <input v-model.trim="form.enterpriseName" required placeholder="请输入企业名称" />
                </label>
                <label>
                  食品经营许可证编号
                  <input v-model.trim="form.licenseNo" placeholder="请输入食品经营许可证编号" />
                </label>
                <label>
                  统一社会信用代码
                  <input v-model.trim="form.creditCode" maxlength="18" placeholder="请输入 18 位统一社会信用代码" />
                </label>
                <label>
                  法定代表人
                  <input v-model.trim="stageB.legalRepresentative" placeholder="请输入法定代表人姓名" />
                </label>
                <label>
                  负责人姓名
                  <input v-model.trim="form.principal" placeholder="请输入负责人姓名" />
                </label>
                <label>
                  负责人电话
                  <input v-model.trim="form.principalPhone" placeholder="请输入负责人电话" />
                </label>
              </div>
            </div>
          </div>

          <div class="enterprise-panel">
            <div class="enterprise-panel__head">
              <div class="enterprise-panel__head-bar" />
              <h3>行政区与地址</h3>
            </div>
            <div class="enterprise-form-section" style="box-shadow: none; border: none; padding: 0; margin: 0">
              <div class="enterprise-detail-grid">
                <label>
                  省份
                  <select v-model="regionSelection.provinceId" @change="handleProvinceChange">
                    <option value="">请选择省份</option>
                    <option v-for="item in regionOptions.provinces" :key="item.id" :value="item.id">{{ item.name }}</option>
                  </select>
                </label>
                <label>
                  城市
                  <select v-model="regionSelection.cityId" :disabled="!regionSelection.provinceId" @change="handleCityChange">
                    <option value="">请选择城市</option>
                    <option v-for="item in regionOptions.cities" :key="item.id" :value="item.id">{{ item.name }}</option>
                  </select>
                </label>
                <label>
                  区县
                  <select v-model="regionSelection.countyId" :disabled="!regionSelection.cityId" @change="handleCountyChange">
                    <option value="">请选择区县</option>
                    <option v-for="item in regionOptions.counties" :key="item.id" :value="item.id">{{ item.name }}</option>
                  </select>
                </label>
                <label>
                  街道
                  <select v-model="regionSelection.streetId" :disabled="!regionSelection.countyId">
                    <option value="">请选择街道</option>
                    <option v-for="item in regionOptions.streets" :key="item.id" :value="item.id">{{ item.name }}</option>
                  </select>
                </label>
                <div v-if="existingRegionText && !regionSelection.provinceId" class="hint enterprise-detail-item--full">
                  当前行政区：{{ existingRegionText }}
                </div>
                <label class="enterprise-detail-item--full">
                  详细地址
                  <input v-model.trim="form.addressDetail" required placeholder="请输入详细地址" />
                </label>
              </div>
            </div>
          </div>

          <div class="enterprise-panel">
            <div class="enterprise-panel__head">
              <div class="enterprise-panel__head-bar" />
              <h3>附件上传区</h3>
            </div>
            <div class="enterprise-form-section" style="box-shadow: none; border: none; padding: 0; margin: 0">
              <p class="enterprise-attachment-intro">
                请上传备案所需材料。附件会在提交备案时一并发送到后端，未提交前只保留在当前页面会话中。
              </p>
              <div class="enterprise-attachment-uploader-grid">
                <div
                  v-for="field in attachmentFields"
                  :key="field.type"
                  class="enterprise-attachment-slot"
                  :class="{ 'enterprise-attachment-slot--filled': !!findAttachment(field.type) }"
                >
                  <div class="enterprise-attachment-slot__head">
                    <span class="material-symbols-outlined enterprise-attachment-slot__icon" aria-hidden="true">{{ attachmentTypeIcon[field.type] || "attach_file" }}</span>
                    <span class="enterprise-attachment-slot__title">{{ field.label }}</span>
                  </div>

                  <div v-if="findAttachment(field.type)" class="enterprise-attachment-file-card">
                    <span class="material-symbols-outlined enterprise-attachment-file-card__icon" aria-hidden="true">draft</span>
                    <div class="enterprise-attachment-file-card__body">
                      <span class="enterprise-attachment-file-card__name">{{ findAttachment(field.type)?.name }}</span>
                      <span class="enterprise-attachment-file-card__hint">已上传，提交备案时一并发送</span>
                    </div>
                    <div class="enterprise-attachment-file-card__actions">
                      <a class="enterprise-attachment-file-card__link" :href="findAttachment(field.type)?.url" target="_blank" rel="noreferrer">预览</a>
                      <button type="button" class="enterprise-attachment-file-card__link" @click="removeAttachment(field.type)">移除</button>
                      <label class="enterprise-attachment-file-card__replace">
                        替换
                        <input
                          type="file"
                          accept="image/jpeg,image/png,image/webp"
                          :disabled="loading || uploading"
                          @change="handleAttachmentSelect(field.type, field.label, $event)"
                        />
                      </label>
                    </div>
                  </div>

                  <label
                    v-else
                    class="enterprise-attachment-dropzone"
                    :class="{ 'is-dragover': dragOverType === field.type, 'is-disabled': loading || uploading }"
                    @dragenter.prevent="dragOverType = field.type"
                    @dragleave.prevent="handleAttachmentDragLeave($event, field.type)"
                    @dragover.prevent="dragOverType = field.type"
                    @drop.prevent="handleAttachmentDrop(field.type, field.label, $event)"
                  >
                    <input
                      type="file"
                      class="enterprise-attachment-dropzone__input"
                      accept="image/jpeg,image/png,image/webp"
                      :disabled="loading || uploading"
                      @change="handleAttachmentSelect(field.type, field.label, $event)"
                    />
                    <span class="material-symbols-outlined enterprise-attachment-dropzone__cloud" aria-hidden="true">cloud_upload</span>
                    <span class="enterprise-attachment-dropzone__title">点击选择或拖拽文件到此处</span>
                    <span class="enterprise-attachment-dropzone__hint">JPG、PNG、WebP，单文件不超过 5MB</span>
                  </label>
                </div>
              </div>
            </div>
          </div>

          <div class="product-form__actions">
            <button class="primary" type="submit" :disabled="loading || uploading">{{ loading ? "提交中..." : submitLabel }}</button>
          </div>
        </form>

        <aside class="enterprise-side-stack">
          <div class="enterprise-side-card enterprise-audit-summary-card">
            <div class="enterprise-side-card__head enterprise-audit-summary-card__head">
              <span class="material-symbols-outlined enterprise-audit-summary-card__head-icon" aria-hidden="true">timeline</span>
              审核轨迹摘要
            </div>
            <div class="enterprise-side-card__body enterprise-audit-summary-card__body">
              <ul v-if="timelineSummaryRows.length" class="enterprise-audit-summary" role="list">
                <li
                  v-for="(row, index) in timelineSummaryRows"
                  :key="row.key"
                  class="enterprise-audit-summary__item"
                  :class="[`is-${row.variant}`, { 'is-last': index === timelineSummaryRows.length - 1 }]"
                >
                  <div class="enterprise-audit-summary__rail" aria-hidden="true">
                    <span class="enterprise-audit-summary__marker">
                      <span v-if="row.variant === 'complete'" class="material-symbols-outlined enterprise-audit-summary__marker-icon is-filled" aria-hidden="true">check</span>
                      <span v-else-if="row.variant === 'active'" class="enterprise-audit-summary__marker-pulse" />
                      <span v-else class="enterprise-audit-summary__marker-hollow" />
                    </span>
                  </div>
                  <div class="enterprise-audit-summary__content">
                    <span class="enterprise-audit-summary__title">{{ row.label }}</span>
                    <span class="enterprise-audit-summary__meta">{{ row.metaLine }}</span>
                    <p v-if="row.noteLine" class="enterprise-audit-summary__note">{{ row.noteLine }}</p>
                  </div>
                </li>
              </ul>
              <p v-else class="enterprise-audit-summary__empty">暂无审核轨迹，提交备案后将在此显示。</p>
              <RouterLink class="primary enterprise-link-button enterprise-audit-summary__cta" :to="{ name: 'enterprise-profile-detail' }">
                <span class="material-symbols-outlined" aria-hidden="true" style="font-size: 18px">open_in_new</span>
                查看完整轨迹
              </RouterLink>
            </div>
          </div>

          <div class="enterprise-side-card">
            <div class="enterprise-side-card__head">提示</div>
            <div class="enterprise-side-card__body">
              <p class="secondary-text" style="margin: 0; font-size: 13px; line-height: 1.55">{{ statusDescription }}</p>
            </div>
          </div>
        </aside>
      </div>

      <div v-if="status.message" class="status" :class="status.type">{{ status.message }}</div>
    </div>
  </EnterpriseWorkspacePage>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from "vue";
import { RouterLink } from "vue-router";
import { presignUpload } from "../../api/file";
import { fetchEnterpriseProfile, fetchRegions, submitEnterpriseProfile } from "../../api/regulation";
import EnterpriseStatusChip from "../../components/enterprise/EnterpriseStatusChip.vue";
import EnterpriseWorkspacePage from "../../components/enterprise/EnterpriseWorkspacePage.vue";
import { formatTime } from "../../utils/formatters";
import { resolveErrorMessage } from "../../utils/uiFeedback";
import { getApprovalStatusLabel, getApprovalStatusTone, useEnterpriseShellSession } from "./enterpriseShared";
import { buildApprovalTimeline, createEmptyStageBData, ENTERPRISE_ATTACHMENT_FIELDS } from "./enterpriseProfileStageB";

const { enterpriseUser, token, handleSidebarNavigate, handleLogout } = useEnterpriseShellSession();
const loading = ref(false);
const profileLoaded = ref(false);
const uploading = ref(false);
const dragOverType = ref(null);

const attachmentTypeIcon = {
  businessLicense: "badge",
  foodPermit: "restaurant",
  onsitePhoto: "photo_camera"
};

const status = reactive({ message: "", type: "" });
const existingRegionId = ref(null);
const existingRegionText = ref("");
const existingRegionPath = ref([]);
const profile = reactive({
  approvalStatus: "",
  approvalComment: "",
  approvedTime: "",
  approvedByName: "",
  regulatorName: "",
  status: "",
  keyReasons: []
});
const regionOptions = reactive({ provinces: [], cities: [], counties: [], streets: [] });
const regionSelection = reactive({ provinceId: "", cityId: "", countyId: "", streetId: "" });
const form = reactive({ enterpriseName: "", licenseNo: "", creditCode: "", addressDetail: "", principal: "", principalPhone: "" });
const stageB = reactive(createEmptyStageBData());

const attachmentFields = ENTERPRISE_ATTACHMENT_FIELDS;
const statusLabel = computed(() => getApprovalStatusLabel(profileLoaded.value, profile.approvalStatus));
const statusTone = computed(() => getApprovalStatusTone(profileLoaded.value, profile.approvalStatus));
const isKeyEnterprise = computed(() => profileLoaded.value && profile.status === "KEY");
const keyReasonItems = computed(() => (
  Array.isArray(profile.keyReasons) ? profile.keyReasons.slice(0, 5) : []
));
const statusDescription = computed(() => {
  if (!profileLoaded.value) return "当前还没有提交企业备案记录。";
  if (profile.approvalStatus === "APPROVED") return "企业主体资料已审核通过，可以继续使用企业端功能。";
  if (profile.approvalStatus === "REJECTED") return "请根据审核意见修改后重新提交。";
  return "企业资料已提交，等待监管审核。";
});
const submitLabel = computed(() => (profileLoaded.value ? "更新并重新提交" : "提交备案"));
const approvalTimeline = computed(() =>
  buildApprovalTimeline({
    profileLoaded: profileLoaded.value,
    approvalStatus: profile.approvalStatus,
    approvalComment: profile.approvalComment,
    approvedTime: profile.approvedTime
  })
);

const timelineSummaryRows = computed(() => {
  const list = approvalTimeline.value.slice(0, 4);
  let activeAssigned = false;
  return list.map((item) => {
    const hasTime = Boolean(item.time && String(item.time).trim());
    let variant = "complete";
    if (hasTime) {
      variant = "complete";
    } else if (!activeAssigned) {
      variant = "active";
      activeAssigned = true;
    } else {
      variant = "upcoming";
    }
    const metaLine = hasTime ? formatTime(item.time) : variant === "active" ? "当前阶段 · 处理中" : "待开始";
    return {
      key: `${item.type}-${item.label}`,
      label: item.label,
      variant,
      metaLine,
      noteLine: variant === "active" && item.note ? item.note : ""
    };
  });
});

const keySupervisionTitle = computed(() => (
  isKeyEnterprise.value ? "当前已纳入重点监管" : "当前未纳入重点监管"
));

const keySupervisionDescription = computed(() => {
  if (keyReasonItems.value.length) {
    const latest = keyReasonItems.value[0];
    const label = latest.reasonLabel || formatKeyReasonType(latest.reasonType);
    const timeText = latest.createTime ? formatTime(latest.createTime) : "-";
    return `最近一次触发原因：${label}，记录时间 ${timeText}。`;
  }
  return isKeyEnterprise.value
    ? "当前企业已被纳入重点监管，请持续关注整改、检查和投诉等监管要求。"
    : "当前暂无已记录的重点监管原因，请持续维护备案信息并配合日常监管。";
});

function formatKeyReasonType(value) {
  const map = {
    COMPLAINT_OVERFLOW: "投诉过量",
    CONSECUTIVE_INSPECTION_FAIL: "连续检查不合格",
    SAMPLING_FAIL: "抽检不合格",
    RECTIFICATION_OVERDUE: "整改逾期",
    WARNING_TRIGGERED: "预警触发",
    MANUAL_SET: "人工设定"
  };
  return map[value] || value || "-";
}

function setStatus(message, type = "info") {
  status.message = message;
  status.type = type;
}

function scrollToForm() {
  document.getElementById("enterprise-profile-form-anchor")?.scrollIntoView({ behavior: "smooth", block: "start" });
}

function resetForm(payload = {}) {
  form.enterpriseName = payload.enterpriseName || "";
  form.licenseNo = payload.licenseNo || "";
  form.creditCode = payload.creditCode || "";
  form.addressDetail = payload.addressDetail || "";
  form.principal = payload.principal || "";
  form.principalPhone = payload.principalPhone || "";
}

function applyStageB(payload = {}) {
  stageB.legalRepresentative = payload.legalRepresentative || "";
  stageB.attachments = Array.isArray(payload.attachments) ? payload.attachments : [];
}

async function loadProfile() {
  try {
    const data = await fetchEnterpriseProfile(token.value);
    profile.approvalStatus = data.approvalStatus || "";
    profile.approvalComment = data.approvalComment || "";
    profile.approvedTime = data.approvedTime || "";
    profile.approvedByName = data.approvedByName || "";
    profile.regulatorName = data.regulatorName || "";
    profile.status = data.status || "";
    profile.keyReasons = Array.isArray(data.keyReasons) ? data.keyReasons : [];
    existingRegionId.value = data.regionId || null;
    existingRegionText.value = data.regionPathText || "";
    existingRegionPath.value = Array.isArray(data.regionPath) ? data.regionPath : [];
    resetForm(data);
    applyStageB(data);
    profileLoaded.value = true;
    if (existingRegionPath.value.length) {
      await applyRegionPath(existingRegionPath.value);
    }
  } catch (error) {
    if (String(error?.message).includes("not found")) {
      profileLoaded.value = false;
      profile.status = "";
      profile.keyReasons = [];
      existingRegionId.value = null;
      existingRegionText.value = "";
      existingRegionPath.value = [];
      resetForm();
      applyStageB(createEmptyStageBData());
      return;
    }
    setStatus(resolveErrorMessage(error, "加载备案信息失败，请稍后重试"), "error");
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
    const payload = {
      ...form,
      regionId,
      legalRepresentative: stageB.legalRepresentative,
      attachments: stageB.attachments
    };
    const data = await submitEnterpriseProfile(token.value, payload);
    profile.approvalStatus = data.approvalStatus || "PENDING";
    profile.approvalComment = data.approvalComment || "";
    profile.approvedTime = data.approvedTime || "";
    profile.approvedByName = data.approvedByName || "";
    profile.regulatorName = data.regulatorName || "";
    profile.status = data.status || profile.status;
    profile.keyReasons = Array.isArray(data.keyReasons) ? data.keyReasons : profile.keyReasons;
    existingRegionId.value = data.regionId || existingRegionId.value;
    existingRegionText.value = data.regionPathText || existingRegionText.value;
    existingRegionPath.value = Array.isArray(data.regionPath) ? data.regionPath : existingRegionPath.value;
    applyStageB(data);
    profileLoaded.value = true;
    setStatus("提交成功，已进入审核流程。", "success");
  } catch (error) {
    setStatus(resolveErrorMessage(error, "提交备案失败，请稍后重试"), "error");
  } finally {
    loading.value = false;
  }
}

async function loadRegions(parentId, targetKey) {
  try {
    regionOptions[targetKey] = await fetchRegions(token.value, parentId);
  } catch (error) {
    setStatus(resolveErrorMessage(error, "加载行政区失败，请稍后重试。"), "error");
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

async function applyRegionPath(path) {
  if (!path?.length) return;
  const province = path[0];
  regionSelection.provinceId = province?.id ? String(province.id) : "";
  resetRegion("province");
  if (province?.id) await loadRegions(province.id, "cities");
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
  if (street?.id) regionSelection.streetId = String(street.id);
}

async function handleProvinceChange() {
  resetRegion("province");
  const provinceId = Number(regionSelection.provinceId || 0);
  if (provinceId) await loadRegions(provinceId, "cities");
}

async function handleCityChange() {
  resetRegion("city");
  const cityId = Number(regionSelection.cityId || 0);
  if (cityId) await loadRegions(cityId, "counties");
}

async function handleCountyChange() {
  resetRegion("county");
  const countyId = Number(regionSelection.countyId || 0);
  if (countyId) await loadRegions(countyId, "streets");
}

function resolveEnterpriseRegionId() {
  if (regionOptions.streets.length) return Number(regionSelection.streetId || 0) || null;
  if (regionOptions.counties.length) return Number(regionSelection.countyId || 0) || null;
  if (regionOptions.cities.length) return Number(regionSelection.cityId || 0) || null;
  return Number(regionSelection.provinceId || 0) || existingRegionId.value || null;
}

function findAttachment(type) {
  return stageB.attachments.find((item) => item.type === type) || null;
}

function isAllowedProfileAttachment(file) {
  if (!file?.name) return false;
  const contentType = (file.type || "").toLowerCase();
  if (["image/jpeg", "image/png", "image/webp"].includes(contentType)) return true;
  return /\.(jpe?g|png|webp)$/i.test(file.name);
}

function handleAttachmentDragLeave(event, type) {
  const related = event.relatedTarget;
  if (related && event.currentTarget.contains(related)) return;
  if (dragOverType.value === type) dragOverType.value = null;
}

async function uploadAttachment(file) {
  const payload = {
    filename: file.name,
    contentType: file.type || "application/octet-stream",
    size: file.size,
    bizType: "ENTERPRISE_PROFILE"
  };
  const presign = await presignUpload(token.value, payload);
  const response = await fetch(presign.uploadUrl, {
    method: "PUT",
    headers: { "Content-Type": payload.contentType },
    body: file
  });
  if (!response.ok) throw new Error(`上传失败 (${response.status})`);
  return presign.fileUrl;
}

async function processProfileAttachment(type, label, file) {
  if (!file) return;
  if (!isAllowedProfileAttachment(file)) {
    setStatus("仅支持 JPG、PNG、WebP 图片格式", "error");
    return;
  }
  if (file.size > 5 * 1024 * 1024) {
    setStatus("单文件请控制在 5MB 以内", "error");
    return;
  }
  uploading.value = true;
  setStatus("");
  try {
    const url = await uploadAttachment(file);
    stageB.attachments = [
      ...stageB.attachments.filter((item) => item.type !== type),
      { type, label, name: file.name, url, uploadedAt: new Date().toISOString() }
    ];
    setStatus(`${label}上传成功`, "success");
  } catch (error) {
    setStatus(resolveErrorMessage(error, `${label}上传失败，请稍后重试`), "error");
  } finally {
    uploading.value = false;
  }
}

async function handleAttachmentSelect(type, label, event) {
  const file = event?.target?.files?.[0];
  if (event?.target) event.target.value = "";
  await processProfileAttachment(type, label, file);
}

async function handleAttachmentDrop(type, label, event) {
  dragOverType.value = null;
  const file = event.dataTransfer?.files?.[0];
  await processProfileAttachment(type, label, file);
}

function removeAttachment(type) {
  stageB.attachments = stageB.attachments.filter((item) => item.type !== type);
}

onMounted(async () => {
  await loadRegions(null, "provinces");
  await loadProfile();
});
</script>

<style scoped>
.enterprise-key-supervision-panel {
  margin-bottom: 20px;
}

.enterprise-key-supervision-panel__summary {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16px;
}

.enterprise-key-supervision-panel__summary strong {
  display: block;
  font-size: 18px;
  line-height: 1.4;
  color: var(--on-surface);
}

.enterprise-key-supervision-panel__summary p {
  margin: 8px 0 0;
  font-size: 14px;
  line-height: 1.7;
  color: var(--on-surface-variant);
}

.enterprise-key-supervision-panel__badge {
  flex-shrink: 0;
  padding: 6px 12px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0;
}

.enterprise-key-supervision-panel__badge.is-key {
  color: #9f1239;
  background: rgba(244, 63, 94, 0.12);
}

.enterprise-key-supervision-panel__badge.is-normal {
  color: #166534;
  background: rgba(34, 197, 94, 0.12);
}

.enterprise-key-supervision-panel__list {
  display: grid;
  gap: 12px;
  margin-top: 16px;
}

.enterprise-key-supervision-panel__item {
  padding: 14px 16px;
  border-radius: 8px;
  background: rgba(15, 23, 42, 0.03);
  border: 1px solid rgba(148, 163, 184, 0.18);
}

.enterprise-key-supervision-panel__item-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
}

.enterprise-key-supervision-panel__item-head strong {
  font-size: 14px;
  color: var(--on-surface);
}

.enterprise-key-supervision-panel__item-head span {
  font-size: 12px;
  color: var(--muted);
  white-space: nowrap;
}

.enterprise-key-supervision-panel__item p,
.enterprise-key-supervision-panel__empty {
  margin: 8px 0 0;
  font-size: 13px;
  line-height: 1.7;
  color: var(--on-surface-variant);
}

.enterprise-key-supervision-panel__empty {
  padding: 14px 16px;
  border-radius: 8px;
  background: rgba(15, 23, 42, 0.03);
  border: 1px dashed rgba(148, 163, 184, 0.3);
}

@media (max-width: 720px) {
  .enterprise-key-supervision-panel__summary,
  .enterprise-key-supervision-panel__item-head {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
