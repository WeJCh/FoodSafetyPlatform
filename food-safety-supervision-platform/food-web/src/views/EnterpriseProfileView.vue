<template>
  <EnterpriseShell>
    <template #sidebar>
      <EnterpriseSidebar
        :active-key="section"
        :nav-items="sidebarNavItems"
        @navigate="handleSidebarNavigate"
        @logout="handleLogout"
      />
    </template>

    <template #topbar>
      <EnterpriseTopbar
        :search-placeholder="`在${currentSectionMeta.title}中搜索...`"
        :username="enterpriseUser.username"
        :role-label="enterpriseUser.userType || '企业用户'"
      >
        <template #actions>
          <EnterpriseStatusChip :label="statusLabel" :tone="statusChipTone" />
        </template>
      </EnterpriseTopbar>
    </template>

    <section class="enterprise-summary-grid">
      <EnterpriseSummaryCard
        v-for="card in summaryCards"
        :key="card.eyebrow"
        :eyebrow="card.eyebrow"
        :value="card.value"
        :title="card.title"
        :description="card.description"
        :tone="card.tone"
      >
        <template v-if="card.chipLabel" #meta>
          <EnterpriseStatusChip :label="card.chipLabel" :tone="card.chipTone" small />
        </template>
      </EnterpriseSummaryCard>
    </section>

    <div class="card dashboard-card enterprise-workspace-card">
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
                食品经营许可证编号
                <input v-model.trim="form.licenseNo" placeholder="请输入食品经营许可证编号" />
              </label>
              <label>
                统一社会信用代码
                <input v-model.trim="form.creditCode" maxlength="18" placeholder="18 位统一社会信用代码（选填）" />
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

          <div v-else-if="section === 'products'">
            <div class="section-title">产品档案</div>
            <div v-if="!profileLoaded" class="status info">
              请先完成企业备案后再维护产品档案。
            </div>
            <div v-else-if="profile.approvalStatus !== 'APPROVED'" class="status info">
              当前企业备案尚未审核通过，产品档案维护功能暂不可用。
            </div>
            <template v-else>
              <form class="product-form" @submit.prevent="handleProductSubmit">
                <label>
                  产品名称
                  <input v-model.trim="productForm.productName" required placeholder="请输入产品名称" />
                </label>
                <label>
                  产品类别
                  <input v-model.trim="productForm.category" required placeholder="如：乳制品、冷冻食品" />
                </label>
                <label>
                  规格
                  <input v-model.trim="productForm.specification" placeholder="如：250ml/盒、500g/袋" />
                </label>
                <label>
                  状态
                  <select v-model="productForm.status">
                    <option value="ACTIVE">启用</option>
                    <option value="INACTIVE">停用</option>
                  </select>
                </label>
                <label class="product-form__full">
                  备注
                  <input v-model.trim="productForm.remark" placeholder="可选填写产品说明" />
                </label>
                <div class="product-form__actions">
                  <button class="primary" type="submit" :disabled="productLoading">
                    {{ productLoading ? "提交中..." : productSubmitLabel }}
                  </button>
                  <button v-if="editingProductId" class="ghost" type="button" @click="resetProductForm()">
                    取消编辑
                  </button>
                </div>
              </form>

              <EnterpriseListTable
                class="product-table"
                :has-data="productRecords.length > 0"
                empty-title="暂无产品档案"
                empty-description="新增产品后会展示在这里。"
              >
                <template #header>
                  <div class="list-row list-header product-header">
                    <span>产品名称</span>
                    <span>类别</span>
                    <span>规格</span>
                    <span>状态</span>
                    <span>更新时间</span>
                    <span>操作</span>
                  </div>
                </template>
                <div v-for="item in productRecords" :key="item.id" class="list-row product-row">
                  <div>
                    <div class="primary-text">{{ item.productName || "-" }}</div>
                    <div class="secondary-text">{{ item.remark || "暂无备注" }}</div>
                  </div>
                  <span>{{ item.category || "-" }}</span>
                  <span>{{ item.specification || "-" }}</span>
                  <EnterpriseStatusChip
                    :label="formatProductStatus(item.status)"
                    :tone="item.status === 'ACTIVE' ? 'success' : 'neutral'"
                    small
                  />
                  <span>{{ formatTime(item.updateTime) }}</span>
                  <div class="action-buttons">
                    <button class="ghost" type="button" @click="handleEditProduct(item)">
                      编辑
                    </button>
                    <button class="ghost" type="button" :disabled="productLoading" @click="handleToggleProductStatus(item)">
                      {{ item.status === "ACTIVE" ? "停用" : "启用" }}
                    </button>
                  </div>
                </div>
              </EnterpriseListTable>
            </template>
          </div>

          <div v-else-if="section === 'inspections'">
            <div class="section-title">检查记录</div>
            <EnterpriseFilterBar
              title="检查记录筛选"
              description="按检查结果与日期范围快速定位历史记录。"
            >
              <form class="filter-bar filter-bar--triple" @submit.prevent="handleInspectionSearch">
                <label>
                  检查结果
                  <select v-model="inspectionFilters.result">
                    <option value="">全部</option>
                    <option value="PASS">合格</option>
                    <option value="FAIL">不合格</option>
                  </select>
                </label>
                <label>
                  起始日期
                  <input v-model="inspectionFilters.startDate" type="date" />
                </label>
                <label>
                  截止日期
                  <input v-model="inspectionFilters.endDate" type="date" />
                </label>
                <button class="primary" type="submit" :disabled="inspectionLoading">
                  {{ inspectionLoading ? "查询中..." : "查询" }}
                </button>
              </form>
            </EnterpriseFilterBar>

            <EnterpriseListTable
              class="inspection-table"
              :has-data="inspectionRecords.length > 0"
              empty-title="暂无检查记录"
              empty-description="检查记录同步后会展示在这里。"
            >
              <template #header>
                <div class="list-row list-header inspection-header">
                  <span>检查日期</span>
                  <span>检查结果</span>
                  <span>问题描述</span>
                  <span>更新时间</span>
                  <span>操作</span>
                </div>
              </template>
              <div v-for="record in inspectionRecords" :key="record.id" class="list-row inspection-row">
                <span>{{ record.inspectionDate || "-" }}</span>
                <EnterpriseStatusChip
                  :label="formatInspectionResult(record.result)"
                  :tone="record.result === 'FAIL' ? 'danger' : 'success'"
                  small
                />
                <div class="inspection-problem" :title="record.problemDesc || '-'">
                  {{ record.problemDesc || "-" }}
                </div>
                <span>{{ formatTime(record.updateTime) }}</span>
                <button class="ghost" type="button" @click="openInspectionDetail(record)">查看详情</button>
              </div>
            </EnterpriseListTable>

            <div class="pager">
              <span>共 {{ inspectionTotal }} 条，{{ inspectionPage }}/{{ inspectionPages }} 页</span>
              <div class="pager-actions">
                <button
                  class="ghost"
                  type="button"
                  :disabled="inspectionPage <= 1"
                  @click="changeInspectionPage(inspectionPage - 1)"
                >
                  上一页
                </button>
                <button
                  class="ghost"
                  type="button"
                  :disabled="inspectionPage >= inspectionPages"
                  @click="changeInspectionPage(inspectionPage + 1)"
                >
                  下一页
                </button>
              </div>
            </div>

            <div v-if="inspectionDetail" class="modal-mask" @click.self="closeInspectionDetail">
              <div class="modal-card">
                <div class="modal-title">检查记录详情</div>
                <div class="modal-body">
                  <div class="modal-field">
                    <span>企业名称</span>
                    <strong>{{ inspectionDetail.record.enterpriseName || "-" }}</strong>
                  </div>
                  <div class="modal-field">
                    <span>检查日期</span>
                    <strong>{{ inspectionDetail.record.inspectionDate || "-" }}</strong>
                  </div>
                  <div class="modal-field">
                    <span>检查结果</span>
                    <strong>{{ formatInspectionResult(inspectionDetail.record.result) }}</strong>
                  </div>
                  <div class="modal-field">
                    <span>问题描述</span>
                    <strong>{{ inspectionDetail.record.problemDesc || "-" }}</strong>
                  </div>
                  <div class="modal-field">
                    <span>检查明细</span>
                    <div class="modal-list">
                      <div v-if="!inspectionDetail.items || !inspectionDetail.items.length" class="modal-empty">
                        暂无检查明细
                      </div>
                      <div v-for="(item, index) in inspectionDetail.items || []" :key="index" class="modal-item">
                        <div class="modal-item-name">{{ item.itemName || "-" }}</div>
                        <div class="modal-item-meta">{{ formatInspectionResult(item.itemResult) }}</div>
                        <div class="modal-item-desc">{{ item.problemDesc || "-" }}</div>
                      </div>
                    </div>
                  </div>
                </div>
                <div class="modal-actions">
                  <button class="ghost" type="button" @click="closeInspectionDetail">关闭</button>
                </div>
              </div>
            </div>
          </div>

          <div v-else>
            <div class="section-title">整改任务</div>
            <EnterpriseFilterBar
              title="整改任务筛选"
              description="跟踪当前整改状态与临近时限任务。"
            >
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
            </EnterpriseFilterBar>

            <EnterpriseListTable
              :has-data="rectificationRecords.length > 0"
              empty-title="暂无整改任务"
              empty-description="监管侧派发后会在这里统一跟进。"
            >
              <template #header>
                <div class="list-row list-header rectification-header">
                  <span>整改任务</span>
                  <span>状态</span>
                  <span>整改时限</span>
                  <span>更新时间</span>
                  <span>操作</span>
                </div>
              </template>
              <div v-for="item in rectificationRecords" :key="item.id" class="list-row rectification-row">
                <div class="rectification-desc" :title="item.rectificationDesc || '-'">
                  {{ item.rectificationDesc || "-" }}
                </div>
                <div class="rectification-status-cell">
                  <EnterpriseStatusChip
                    :label="formatRectificationStatus(item.status)"
                    :tone="item.status === 'CONFIRMED' ? 'success' : item.status === 'REWORK' ? 'danger' : item.status === 'SUBMITTED' ? 'warning' : 'neutral'"
                    small
                  />
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
            </EnterpriseListTable>

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

    </div>
  </EnterpriseShell>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, reactive, ref, watch } from "vue";
import { useRoute, useRouter } from "vue-router";
import { presignUpload } from "../api/file";
import { getActiveSession, performLogout } from "../session/authRuntime";
import {
  createProduct,
  fetchEnterpriseProfile,
  fetchMyProducts,
  fetchRegions,
  submitEnterpriseProfile,
  updateProduct
} from "../api/regulation";
import {
  fetchEnterpriseInspectionRecordDetail,
  fetchEnterpriseInspectionRecords,
  fetchMyRectifications,
  fetchRectificationActions,
  fetchRectificationDetail,
  submitMyRectification
} from "../api/regulationOperation";
import EnterpriseFilterBar from "../components/enterprise/EnterpriseFilterBar.vue";
import EnterpriseListTable from "../components/enterprise/EnterpriseListTable.vue";
import EnterpriseSidebar from "../components/enterprise/EnterpriseSidebar.vue";
import EnterpriseStatusChip from "../components/enterprise/EnterpriseStatusChip.vue";
import EnterpriseSummaryCard from "../components/enterprise/EnterpriseSummaryCard.vue";
import EnterpriseTopbar from "../components/enterprise/EnterpriseTopbar.vue";
import RectificationDetailModal from "../components/RectificationDetailModal.vue";
import EnterpriseShell from "../layouts/EnterpriseShell.vue";
import { formatTime } from "../utils/formatters";

const router = useRouter();
const route = useRoute();
const session = computed(() => getActiveSession() || {});
const enterpriseUser = computed(() => session.value);
const token = computed(() => session.value.token || "");

function normalizeSection(value) {
  if (value === "products" || value === "inspections" || value === "rectification") {
    return value;
  }
  return "profile";
}

function getSectionRouteName(sectionValue) {
  if (sectionValue === "products") return "enterprise-products";
  if (sectionValue === "inspections") return "enterprise-inspections";
  if (sectionValue === "rectification") return "enterprise-rectifications";
  return "enterprise-profile";
}

function resolveCurrentSection() {
  return normalizeSection(route.meta?.initialSection);
}

const section = ref(resolveCurrentSection());
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
  creditCode: "",
  addressDetail: "",
  principal: "",
  principalPhone: ""
});
const productLoading = ref(false);
const productRecords = ref([]);
const editingProductId = ref(null);
const productForm = reactive({
  productName: "",
  category: "",
  specification: "",
  status: "ACTIVE",
  remark: ""
});
const rectificationLoading = ref(false);
const rectificationRecords = ref([]);
const rectificationPage = ref(1);
const rectificationSize = ref(8);
const rectificationTotal = ref(0);
const rectificationPages = ref(1);
const inspectionLoading = ref(false);
const inspectionRecords = ref([]);
const inspectionPage = ref(1);
const inspectionSize = ref(8);
const inspectionTotal = ref(0);
const inspectionPages = ref(1);
const inspectionDetail = ref(null);
const inspectionFilters = reactive({
  result: "",
  startDate: "",
  endDate: ""
});
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

const rectificationStatusMap = {
  ONGOING: "整改中",
  SUBMITTED: "待复核",
  REWORK: "打回重做",
  CONFIRMED: "已确认"
};
const inspectionResultMap = {
  PASS: "合格",
  FAIL: "不合格"
};
const productStatusMap = {
  ACTIVE: "启用",
  INACTIVE: "停用"
};
const sectionMetaMap = {
  profile: {
    title: "企业备案",
    subtitle: "维护主体备案信息、负责人资料与属地行政区信息。"
  },
  products: {
    title: "产品档案",
    subtitle: "统一维护企业产品目录、规格状态与基础说明。"
  },
  inspections: {
    title: "检查记录",
    subtitle: "按时间与结果快速查看历史检查情况与明细。"
  },
  rectification: {
    title: "整改任务",
    subtitle: "跟进整改时限、上传凭证，并完成复核前的闭环提交。"
  }
};
const sidebarNavItems = [
  {
    key: "profile",
    label: "企业备案",
    caption: "主体资料与审核状态"
  },
  {
    key: "products",
    label: "产品档案",
    caption: "产品目录与状态维护"
  },
  {
    key: "inspections",
    label: "检查记录",
    caption: "历史检查与明细回看"
  },
  {
    key: "rectification",
    label: "整改任务",
    caption: "整改提报与复核跟进"
  }
];

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
const currentSectionMeta = computed(() => sectionMetaMap[section.value] || sectionMetaMap.profile);
const statusChipTone = computed(() => {
  if (statusTone.value === "success") return "success";
  if (statusTone.value === "error") return "danger";
  if (statusTone.value === "pending") return "warning";
  return "neutral";
});
const summaryCards = computed(() => [
  {
    eyebrow: "Current Section",
    value: currentSectionMeta.value.title,
    title: "当前工作区域",
    description: currentSectionMeta.value.subtitle,
    tone: "highlight"
  },
  {
    eyebrow: "Profile Status",
    value: statusLabel.value,
    title: "备案审核状态",
    description: profileLoaded.value ? "企业主体资料已建立，可继续维护后续能力。" : "当前还没有提交企业备案记录。",
    tone: statusChipTone.value,
    chipLabel: statusLabel.value,
    chipTone: statusChipTone.value
  },
  {
    eyebrow: "Products",
    value: String(productRecords.value.length || 0),
    title: "产品档案数量",
    description: profile.approvalStatus === "APPROVED" ? "已接入当前档案记录。" : "备案通过后可继续维护产品档案。"
  },
  {
    eyebrow: "Rectifications",
    value: String(rectificationTotal.value || rectificationRecords.value.length || 0),
    title: "整改任务总数",
    description: "当前页会保留既有整改提交与附件上传逻辑。"
  }
]);

const submitLabel = computed(() => (profileLoaded.value ? "更新并重新提交" : "提交备案"));
const productSubmitLabel = computed(() => (editingProductId.value ? "保存产品" : "新增产品"));

function setStatus(message, type = "info") {
  status.message = message;
  status.type = type;
}

function resetForm(payload = {}) {
  form.enterpriseName = payload.enterpriseName || "";
  form.licenseNo = payload.licenseNo || "";
  form.creditCode = payload.creditCode || "";
  form.addressDetail = payload.addressDetail || "";
  form.principal = payload.principal || "";
  form.principalPhone = payload.principalPhone || "";
}

function resetProductForm(payload = {}) {
  editingProductId.value = payload.id || null;
  productForm.productName = payload.productName || "";
  productForm.category = payload.category || "";
  productForm.specification = payload.specification || "";
  productForm.status = payload.status || "ACTIVE";
  productForm.remark = payload.remark || "";
}

async function loadProfile() {
  try {
    const data = await fetchEnterpriseProfile(token.value);
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
    const data = await submitEnterpriseProfile(token.value, { ...form, regionId });
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

async function handleLogout() {
  await performLogout();
  router.replace({ name: "login" }).catch(() => {});
}

async function handleSidebarNavigate(nextSection) {
  if (nextSection === "products") {
    await handleProductsEnter();
    return;
  }
  if (nextSection === "inspections") {
    await handleInspectionEnter();
    return;
  }
  if (nextSection === "rectification") {
    await handleRectificationEnter();
    return;
  }
  section.value = "profile";
}

function formatRectificationStatus(value) {
  return rectificationStatusMap[value] || value || "-";
}

function formatInspectionResult(value) {
  return inspectionResultMap[value] || value || "-";
}

function formatProductStatus(value) {
  return productStatusMap[value] || value || "-";
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
    const presign = await presignUpload(token.value, payload);
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
        const actions = await fetchRectificationActions(token.value, item.id);
        rectificationHasReworkMap[item.id] = Array.isArray(actions)
          && actions.some((log) => String(log?.actionType || "").toUpperCase() === "REVIEW_REWORK");
      } catch {
        // 拉取失败时降级为仅根据当前状态判断，避免阻断列表渲染。
        rectificationHasReworkMap[item.id] = item.status === "REWORK";
      }
    })
  );
}

async function handleProductsEnter() {
  section.value = "products";
  await loadProducts();
}

async function loadProducts() {
  if (!profileLoaded.value || profile.approvalStatus !== "APPROVED") {
    productRecords.value = [];
    resetProductForm();
    return;
  }
  productLoading.value = true;
  setStatus("");
  try {
    productRecords.value = await fetchMyProducts(token.value);
  } catch (error) {
    setStatus(error.message || "加载产品档案失败", "error");
  } finally {
    productLoading.value = false;
  }
}

async function handleProductSubmit() {
  if (!profileLoaded.value || profile.approvalStatus !== "APPROVED") {
    setStatus("企业备案审核通过后才能维护产品档案", "error");
    return;
  }
  productLoading.value = true;
  setStatus("");
  try {
    const payload = {
      productName: productForm.productName,
      category: productForm.category,
      specification: productForm.specification,
      status: productForm.status,
      remark: productForm.remark
    };
    if (editingProductId.value) {
      await updateProduct(token.value, editingProductId.value, payload);
      setStatus("产品档案更新成功", "success");
    } else {
      await createProduct(token.value, payload);
      setStatus("产品档案新增成功", "success");
    }
    resetProductForm();
    await loadProducts();
  } catch (error) {
    setStatus(error.message || "保存产品档案失败", "error");
  } finally {
    productLoading.value = false;
  }
}

function handleEditProduct(item) {
  resetProductForm(item || {});
}

async function handleToggleProductStatus(item) {
  if (!item?.id) {
    return;
  }
  productLoading.value = true;
  setStatus("");
  try {
    await updateProduct(token.value, item.id, {
      productName: item.productName,
      category: item.category,
      specification: item.specification,
      status: item.status === "ACTIVE" ? "INACTIVE" : "ACTIVE",
      remark: item.remark
    });
    setStatus("产品状态更新成功", "success");
    await loadProducts();
  } catch (error) {
    setStatus(error.message || "更新产品状态失败", "error");
  } finally {
    productLoading.value = false;
  }
}

async function handleRectificationEnter() {
  section.value = "rectification";
  await loadRectifications();
}

async function handleInspectionEnter() {
  section.value = "inspections";
  await loadInspections();
}

async function applySection(sectionValue) {
  const normalized = normalizeSection(sectionValue);
  if (section.value !== normalized) {
    section.value = normalized;
  }

  if (normalized === "products") {
    await loadProducts();
    return;
  }

  if (normalized === "inspections") {
    await loadInspections();
    return;
  }

  if (normalized === "rectification") {
    await loadRectifications();
  }
}

async function loadInspections() {
  inspectionLoading.value = true;
  setStatus("");
  try {
    const data = await fetchEnterpriseInspectionRecords(token.value, {
      ...inspectionFilters,
      page: inspectionPage.value,
      size: inspectionSize.value
    });
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

async function loadRectifications() {
  rectificationLoading.value = true;
  setStatus("");
  try {
    const data = await fetchMyRectifications(token.value, {
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
      fetchRectificationDetail(token.value, id),
      fetchRectificationActions(token.value, id)
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

async function handleInspectionSearch() {
  inspectionPage.value = 1;
  await loadInspections();
}

async function changeInspectionPage(nextPage) {
  inspectionPage.value = nextPage;
  await loadInspections();
}

async function openInspectionDetail(record) {
  if (!record?.id) return;
  inspectionLoading.value = true;
  try {
    inspectionDetail.value = await fetchEnterpriseInspectionRecordDetail(token.value, record.id);
  } catch (error) {
    setStatus(error.message || "加载检查记录失败", "error");
  } finally {
    inspectionLoading.value = false;
  }
}

function closeInspectionDetail() {
  inspectionDetail.value = null;
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
    await submitMyRectification(token.value, item.id, {
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
    regionOptions[targetKey] = await fetchRegions(token.value, parentId);
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
    await applySection(section.value);
  };
  init();
});

watch(
  () => route.name,
  async () => {
    await applySection(resolveCurrentSection());
  }
);

watch(section, (nextSection) => {
  const targetName = getSectionRouteName(nextSection);
  if (route.name !== targetName) {
    router.push({ name: targetName }).catch(() => {});
  }
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

.product-form {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
  margin-bottom: 18px;
}

.product-form__full {
  grid-column: 1 / -1;
}

.product-form__actions {
  grid-column: 1 / -1;
  display: flex;
  gap: 10px;
  align-items: center;
}

.product-header,
.product-row {
  --row-columns: 1.4fr 1fr 1fr 0.7fr 0.9fr 1fr;
}

.enterprise-shell .admin-info {
  margin-bottom: 16px;
}

.inspection-header,
.inspection-row {
  --row-columns: 0.9fr 0.8fr 1.8fr 1fr 0.8fr;
}

.inspection-problem {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
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

.modal-list {
  display: grid;
  gap: 10px;
}

.modal-item {
  padding: 10px 12px;
  border-radius: 12px;
  border: 1px solid var(--stroke);
  background: var(--card-strong);
  display: grid;
  gap: 4px;
}

.modal-item-name {
  font-weight: 600;
  font-size: 14px;
}

.modal-item-meta {
  font-size: 12px;
  color: var(--muted);
}

.modal-item-desc {
  font-size: 13px;
  color: var(--ink);
}

.enterprise-shell {
  grid-template-columns: 260px 1fr;
}

@media (max-width: 960px) {
  .inspection-header,
  .inspection-row,
  .product-header,
  .product-row,
  .rectification-header,
  .rectification-row {
    --row-columns: 1fr;
  }

  .product-form {
    grid-template-columns: 1fr;
  }

  .product-form__actions {
    flex-direction: column;
    align-items: stretch;
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
