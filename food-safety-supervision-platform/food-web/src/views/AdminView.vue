<template>
  <div class="admin-shell">
    <aside class="admin-sidebar">
      <div class="admin-brand">管理员中心</div>
      <nav class="admin-nav">
        <button :class="{ active: section === 'regulators' }" @click="section = 'regulators'">
          监管人员管理
        </button>
        <button :class="{ active: section === 'enterprise-approval' }" @click="section = 'enterprise-approval'">
          企业审批
        </button>
        <button :class="{ active: section === 'users' }" @click="section = 'users'">
          用户管理
        </button>
        <button :class="{ active: section === 'logs' }" @click="section = 'logs'">
          操作日志
        </button>
      </nav>
      <button class="ghost" type="button" @click="('logout')">退出管理员页面</button>
    </aside>

    <div class="admin-main">
      <div class="hero-panel">
        <div class="hero-content">
          <span class="badge">管理员中心</span>
          <h1>系统管理员控制台</h1>
          <p>
            创建监管人员账号并分配角色类型。后续可在此扩展审核、账号管理等功能。
          </p>
          <div class="hero-highlights">
            <div>
              <strong>监管人员添加</strong>
              <span>创建执法人员或区域管理员</span>
            </div>
            <div>
              <strong>账号管理</strong>
              <span>后续扩展用户管理与审核</span>
            </div>
            <div>
              <strong>统一入口</strong>
              <span>管理员专属操作空间</span>
            </div>
          </div>
        </div>
      </div>

      <div class="form-panel">
        <div class="card">
          <div class="section-title">系统管理员</div>
          <div class="admin-info">
            <div>账号：{{ adminUser.username }}</div>
            <div>角色：{{ adminUser.userType }}</div>
          </div>

          <div v-if="section === 'regulators'" class="sub-nav">
            <button :class="{ active: subSection === 'create' }" @click="subSection = 'create'">
              添加监管人员
            </button>
            <button :class="{ active: subSection === 'list' }" @click="subSection = 'list'">
              监管人员列表
            </button>
            <button :class="{ active: subSection === 'roles' }" @click="subSection = 'roles'">
              角色调整
            </button>
            <button :class="{ active: subSection === 'transfer' }" @click="subSection = 'transfer'">
              区域交接
            </button>
            <button :class="{ active: subSection === 'disable' }" @click="subSection = 'disable'">
              启用/停用
            </button>
          </div>

          <form v-if="section === 'regulators' && subSection === 'create'" @submit.prevent="handleCreate">
            <label>
              监管人员账号
              <input v-model.trim="regulatorForm.username" required placeholder="请输入账号" />
            </label>
            <label>
              初始密码
              <input v-model.trim="regulatorForm.password" type="password" required placeholder="请输入密码" />
            </label>
            <label>
              姓名
              <input v-model.trim="regulatorForm.fullName" required placeholder="请输入姓名" />
            </label>
            <label>
              身份证号
              <input v-model.trim="regulatorForm.idNumber" required placeholder="18 位身份证号" />
            </label>
            <label>
              工作证件 URL（可选）
              <input v-model.trim="regulatorForm.workIdUrl" placeholder="证件链接" />
            </label>
            <label>
              联系电话
              <input v-model.trim="regulatorForm.phone" required placeholder="11 位手机号" />
            </label>
            <label>
              角色类型
              <select v-model="regulatorForm.roleType">
                <option value="REGULATOR_ENFORCER">执法人员</option>
                <option value="REGULATOR_ADMIN">区域管理员</option>
              </select>
            </label>
            <label>
              管辖区域
              <input v-model.trim="regulatorForm.jurisdictionArea" required placeholder="省/市/区/街道" />
            </label>
            <button class="primary" type="submit" :disabled="loading">
              {{ loading ? "创建中..." : "创建监管人员" }}
            </button>
          </form>

          <div v-else-if="section === 'regulators'" class="placeholder">
            <strong>功能占位</strong>
            <p>{{ regulatorSubLabel }} 将在后续版本实现。</p>
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
  </div>
</template>

<script setup>
import { computed, reactive, ref } from "vue";
import { createRegulator } from "../api/auth";

const props = defineProps({
  adminUser: {
    type: Object,
    required: true
  },
  token: {
    type: String,
    required: true
  }
});

defineEmits(["logout"]);

const loading = ref(false);
const status = reactive({ message: "", type: "" });
const section = ref("regulators");
const subSection = ref("create");

const regulatorForm = reactive({
  username: "",
  password: "",
  fullName: "",
  idNumber: "",
  workIdUrl: "",
  phone: "",
  roleType: "REGULATOR_ENFORCER",
  jurisdictionArea: ""
});

const sectionLabel = computed(() => {
  if (section.value === "enterprise-approval") return "企业审批";
  if (section.value === "users") return "用户管理";
  if (section.value === "logs") return "操作日志";
  return "监管人员管理";
});

const regulatorSubLabel = computed(() => {
  if (subSection.value === "list") return "监管人员列表";
  if (subSection.value === "roles") return "角色调整";
  if (subSection.value === "transfer") return "区域交接";
  if (subSection.value === "disable") return "启用/停用";
  return "添加监管人员";
});

function setStatus(message, type = "info") {
  status.message = message;
  status.type = type;
}

async function handleCreate() {
  loading.value = true;
  setStatus("");
  try {
    await createRegulator(regulatorForm, props.token);
    setStatus("监管人员创建成功。", "success");
  } catch (error) {
    setStatus(error.message || "创建失败", "error");
  } finally {
    loading.value = false;
  }
}
</script>
