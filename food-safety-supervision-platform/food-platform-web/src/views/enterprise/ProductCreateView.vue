<template>
  <section class="enterprise-page">
    <nav class="enterprise-breadcrumbs">
      <RouterLink :to="{ name: 'enterprise-products' }">产品档案</RouterLink>
      <span class="material-symbols-outlined">chevron_right</span>
      <span>新增产品</span>
    </nav>

    <form class="enterprise-card enterprise-form-card" @submit.prevent="handleSubmit">
      <div class="section-head section-head--column">
        <h3>新增产品档案</h3>
        <p>录入新的产品基础信息、规格和监管元数据。</p>
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
        <button class="enterprise-primary-button" type="submit" :disabled="loading">{{ loading ? '保存中...' : '保存产品' }}</button>
        <RouterLink class="enterprise-ghost-button" :to="{ name: 'enterprise-products' }">取消</RouterLink>
      </div>
      <p v-if="status" class="inline-status inline-status--success">{{ status }}</p>
    </form>
  </section>
</template>

<script setup>
import { reactive, ref } from "vue";
import { useRouter } from "vue-router";
import { saveProduct } from "../../services/enterpriseGateway";
import { getStoredSession } from "../../session/authSession";

const router = useRouter();
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

async function handleSubmit() {
  loading.value = true;
  status.value = "";
  try {
    const session = getStoredSession();
    const result = await saveProduct(session?.token || "", "", form);
    status.value = "产品档案已创建。";
    const productId = result?.id || `mock-${Date.now()}`;
    setTimeout(() => {
      router.push({ name: 'enterprise-product-detail', params: { id: productId } });
    }, 280);
  } finally {
    loading.value = false;
  }
}
</script>
