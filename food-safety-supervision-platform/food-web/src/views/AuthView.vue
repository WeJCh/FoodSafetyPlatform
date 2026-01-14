<template>
  <div class="app-shell">
    <div class="hero-panel">
      <div class="hero-content">
        <span class="badge">食品安全平台</span>
        <h1>用户中心登录与注册</h1>
        <p>
          已有账号请登录。注册时请选择公众或企业用户类型，企业注册信息与公众一致。
        </p>
        <div class="hero-highlights">
          <div>
            <strong>第一步</strong>
            <span>选择用户类型</span>
          </div>
          <div>
            <strong>第二步</strong>
            <span>填写基础资料</span>
          </div>
          <div>
            <strong>第三步</strong>
            <span>注册完成即可使用</span>
          </div>
        </div>
      </div>
    </div>

    <div class="form-panel">
      <div class="card">
        <div class="tabs">
          <button :class="{ active: view === 'login' }" @click="switchView('login')">登录</button>
          <button :class="{ active: view !== 'login' }" @click="switchView('select')">注册</button>
        </div>

        <form v-if="view === 'login'" @submit.prevent="handleLogin">
          <label>
            用户名
            <input v-model.trim="loginForm.username" required placeholder="请输入账号" />
          </label>
          <label>
            密码
            <input v-model.trim="loginForm.password" type="password" required placeholder="请输入密码" />
          </label>
          <button class="primary" type="submit" :disabled="loading">
            {{ loading ? "登录中..." : "登录" }}
          </button>
        </form>

        <div v-else-if="view === 'select'" class="type-select">
          <div class="type-card" @click="switchView('public')">
            <strong>公众用户</strong>
            <span>提交投诉与查询信息</span>
          </div>
          <div class="type-card" @click="switchView('enterprise')">
            <strong>企业用户</strong>
            <span>企业账号注册</span>
          </div>
          <div class="type-card note">
            <strong>监管人员</strong>
            <span>由系统管理员统一添加</span>
          </div>
        </div>

        <form v-else-if="view === 'public'" @submit.prevent="handlePublicRegister">
          <label>
            用户名
            <input v-model.trim="publicForm.username" required placeholder="请输入用户名" />
          </label>
          <label>
            密码
            <input v-model.trim="publicForm.password" type="password" required placeholder="请输入密码" />
          </label>
          <label>
            真实姓名
            <input v-model.trim="publicForm.realName" placeholder="请输入真实姓名" />
          </label>
          <label>
            手机号
            <input v-model.trim="publicForm.phone" placeholder="请输入手机号" />
          </label>
          <button class="primary" type="submit" :disabled="loading">
            {{ loading ? "创建中..." : "创建账号" }}
          </button>
          <button class="ghost" type="button" @click="switchView('select')">返回</button>
        </form>

        <form v-else-if="view === 'enterprise'" @submit.prevent="handleEnterpriseRegister">
          <label>
            用户名
            <input v-model.trim="enterpriseForm.username" required placeholder="请输入用户名" />
          </label>
          <label>
            密码
            <input v-model.trim="enterpriseForm.password" type="password" required placeholder="请输入密码" />
          </label>
          <label>
            真实姓名
            <input v-model.trim="enterpriseForm.realName" placeholder="请输入真实姓名" />
          </label>
          <label>
            手机号
            <input v-model.trim="enterpriseForm.phone" placeholder="请输入手机号" />
          </label>
          <button class="primary" type="submit" :disabled="loading">
            {{ loading ? "创建中..." : "创建账号" }}
          </button>
          <button class="ghost" type="button" @click="switchView('select')">返回</button>
        </form>

        <div class="status" :class="status.type" v-if="status.message">
          {{ status.message }}
        </div>

        <div class="token-box" v-if="token">
          <div class="token-header">
            <span>令牌</span>
            <button class="ghost" type="button" @click="handleVerify">验证</button>
          </div>
          <textarea readonly rows="3">{{ token }}</textarea>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref } from "vue";
import { login, register, registerEnterprise, verify } from "../api/auth";

const emit = defineEmits(["admin-login", "enterprise-login"]);

const view = ref("login");
const loading = ref(false);
const token = ref("");
const status = reactive({ message: "", type: "" });

const loginForm = reactive({
  username: "",
  password: ""
});

const publicForm = reactive({
  username: "",
  password: "",
  realName: "",
  phone: ""
});

const enterpriseForm = reactive({
  username: "",
  password: "",
  realName: "",
  phone: ""
});

function setStatus(message, type = "info") {
  status.message = message;
  status.type = type;
}

function switchView(target) {
  view.value = target;
  setStatus("");
}

async function handleLogin() {
  loading.value = true;
  setStatus("");
  try {
    const result = await login(loginForm);
    if (result.userType === "ADMIN") {
      emit("admin-login", {
        token: result.token,
        username: result.username,
        userType: result.userType
      });
      setStatus("");
    } else if (result.userType === "ENTERPRISE") {
      emit("enterprise-login", {
        token: result.token,
        username: result.username,
        userType: result.userType
      });
      setStatus("");
    } else {
      token.value = result.token;
      setStatus("登录成功，令牌已生成。", "success");
    }
  } catch (error) {
    setStatus(error.message || "登录失败", "error");
  } finally {
    loading.value = false;
  }
}

async function handlePublicRegister() {
  loading.value = true;
  setStatus("");
  try {
    await register(publicForm);
    setStatus("注册完成，现在可以登录。", "success");
    view.value = "login";
  } catch (error) {
    setStatus(error.message || "注册失败", "error");
  } finally {
    loading.value = false;
  }
}

async function handleEnterpriseRegister() {
  loading.value = true;
  setStatus("");
  try {
    await registerEnterprise(enterpriseForm);
    setStatus("注册完成，现在可以登录。", "success");
    view.value = "login";
  } catch (error) {
    setStatus(error.message || "注册失败", "error");
  } finally {
    loading.value = false;
  }
}

async function handleVerify() {
  if (!token.value) {
    setStatus("请先登录。", "error");
    return;
  }
  try {
    const result = await verify(token.value);
    setStatus(result.valid ? "令牌有效" : "令牌无效", result.valid ? "success" : "error");
  } catch (error) {
    setStatus(error.message || "验证失败", "error");
  }
}
</script>
