<template>
  <section class="enterprise-page" v-if="loaded">
    <nav class="enterprise-breadcrumbs">
      <RouterLink :to="{ name: 'enterprise-products' }">产品档案</RouterLink>
      <span class="material-symbols-outlined">chevron_right</span>
      <RouterLink :to="{ name: 'enterprise-product-detail', params: { id: route.params.id } }">产品详情</RouterLink>
      <span class="material-symbols-outlined">chevron_right</span>
      <span>编辑产品</span>
    </nav>

    <form class="enterprise-card enterprise-form-card" @submit.prevent="handleSubmit">
      <div class="section-head section-head--column">
        <h3>编辑产品信息</h3>
        <p>更新产品档案中的监管数据和合规性状态。</p>
      </div>
      <div class="enterprise-form-grid">
        <label class="enterprise-field">
          <span>产品名称</span>
          <input v-model.trim="form.productName" type="text" required />
        </label>
        <label class="enterprise-field">
          <span>产品类别</span>
          <input v-model.trim="form.category" type="text" required />
        </label>
        <label class="enterprise-field">
          <span>规格型号</span>
          <input v-model.trim="form.specification" type="text" />
        </label>
        <label class="enterprise-field">
          <span>保质期</span>
          <input v-model.trim="form.shelfLife" type="text" />
        </label>
        <label class="enterprise-field">
          <span>监管状态</span>
          <select v-model="form.status">
            <option value="ACTIVE">启用</option>
            <option value="INACTIVE">停用</option>
          </select>
        </label>
        <label class="enterprise-field">
          <span>批文编号</span>
          <input v-model.trim="form.approvalNo" type="text" />
        </label>
        <label class="enterprise-field enterprise-field--full">
          <span>产品描述</span>
          <textarea v-model.trim="form.remark" rows="5"></textarea>
        </label>
      </div>
      <div class="form-actions">
        <button class="enterprise-primary-button" type="submit" :disabled="loading">{{ loading ? '保存中...' : '保存修改' }}</button>
        <RouterLink class="enterprise-ghost-button" :to="{ name: 'enterprise-product-detail', params: { id: route.params.id } }">取消</RouterLink>
      </div>
      <p v-if="status" class="inline-status inline-status--success">{{ status }}</p>
    </form>
  </section>
</template>

<script setup>
import { onMounted, reactive, ref } from "vue";
import { useRoute, useRouter } from "vue-router";
import { loadProductById, saveProduct } from "../../services/enterpriseGateway";
import { getStoredSession } from "../../session/authSession";

const route = useRoute();
const router = useRouter();
const loaded = ref(false);
const loading = ref(false);
const status = ref("");
const form = reactive({
  productName: "",
  category: "",
  specification: "",
  shelfLife: "",
  status: "ACTIVE",
  approvalNo: "",
  remark: ""
});

onMounted(async () => {
  const session = getStoredSession();
  const product = await loadProductById(session?.token || "", route.params.id);
  Object.assign(form, product || {});
  loaded.value = true;
});

async function handleSubmit() {
  loading.value = true;
  status.value = "";
  try {
    const session = getStoredSession();
    await saveProduct(session?.token || "", route.params.id, form);
    status.value = "产品信息已更新。";
    setTimeout(() => {
      router.push({ name: 'enterprise-product-detail', params: { id: route.params.id } });
    }, 280);
  } finally {
    loading.value = false;
  }
}
</script>
