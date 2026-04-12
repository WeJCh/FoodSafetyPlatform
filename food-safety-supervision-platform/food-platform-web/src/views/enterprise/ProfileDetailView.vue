<template>
  <section class="enterprise-page">
    <nav class="enterprise-breadcrumbs">
      <RouterLink :to="{ name: 'enterprise-profile' }">企业备案</RouterLink>
      <span class="material-symbols-outlined">chevron_right</span>
      <span>企业备案详情</span>
    </nav>

    <div class="enterprise-grid enterprise-grid--detail">
      <form class="enterprise-card enterprise-form-card" @submit.prevent="handleSubmit">
        <div class="section-head section-head--column">
          <h3>基本信息</h3>
          <p>统一查看并维护企业备案主档信息。</p>
        </div>
        <div class="enterprise-form-grid">
          <label class="enterprise-field">
            <span>企业名称</span>
            <input v-model.trim="form.enterpriseName" type="text" required />
          </label>
          <label class="enterprise-field">
            <span>许可证编号</span>
            <input v-model.trim="form.licenseNo" type="text" />
          </label>
          <label class="enterprise-field enterprise-field--full">
            <span>行政区划</span>
            <input v-model.trim="form.regionText" type="text" />
          </label>
          <label class="enterprise-field enterprise-field--full">
            <span>详细地址</span>
            <input v-model.trim="form.addressDetail" type="text" required />
          </label>
        </div>

        <div class="section-head section-head--column section-head--subtle">
          <h3>联系人信息</h3>
          <p>与原型保持一致，统一企业负责人和对接联系人展示方式。</p>
        </div>
        <div class="enterprise-form-grid">
          <label class="enterprise-field">
            <span>负责人姓名</span>
            <input v-model.trim="form.principal" type="text" />
          </label>
          <label class="enterprise-field">
            <span>负责人电话</span>
            <input v-model.trim="form.principalPhone" type="tel" />
          </label>
          <label class="enterprise-field">
            <span>联系人姓名</span>
            <input v-model.trim="form.contactName" type="text" />
          </label>
          <label class="enterprise-field">
            <span>联系人电话</span>
            <input v-model.trim="form.contactPhone" type="tel" />
          </label>
          <label class="enterprise-field enterprise-field--full">
            <span>电子邮箱</span>
            <input v-model.trim="form.email" type="email" />
          </label>
        </div>

        <div class="form-actions">
          <button class="enterprise-primary-button" type="submit" :disabled="loading">{{ loading ? '保存中...' : '保存备案信息' }}</button>
          <RouterLink class="enterprise-ghost-button" :to="{ name: 'enterprise-products' }">前往维护产品档案</RouterLink>
        </div>
        <p v-if="status" class="inline-status inline-status--success">{{ status }}</p>
      </form>

      <aside class="enterprise-card">
        <div class="section-head">
          <h3>备案附件</h3>
          <span class="enterprise-chip enterprise-chip--neutral">{{ form.attachments?.length || 0 }} 份</span>
        </div>
        <div class="attachment-list">
          <div v-for="attachment in form.attachments || []" :key="attachment.id" class="attachment-item">
            <span class="material-symbols-outlined">folder_open</span>
            <div>
              <strong>{{ attachment.name }}</strong>
              <p>{{ attachment.type }}</p>
            </div>
          </div>
        </div>
        <ul class="detail-list detail-list--tight">
          <li>
            <strong>审核状态</strong>
            <span>{{ form.archiveStatus || '已核准' }}</span>
          </li>
          <li>
            <strong>审核意见</strong>
            <span>{{ form.approvalComment }}</span>
          </li>
          <li>
            <strong>审核时间</strong>
            <span>{{ form.approvedTime }}</span>
          </li>
        </ul>
      </aside>
    </div>
  </section>
</template>

<script setup>
import { onMounted, reactive, ref } from "vue";
import { loadEnterpriseProfile, saveEnterpriseProfile } from "../../services/enterpriseGateway";
import { getStoredSession } from "../../session/authSession";

const form = reactive({ attachments: [] });
const loading = ref(false);
const status = ref("");

onMounted(async () => {
  const session = getStoredSession();
  Object.assign(form, await loadEnterpriseProfile(session?.token || ""));
});

async function handleSubmit() {
  loading.value = true;
  status.value = "";
  try {
    const session = getStoredSession();
    await saveEnterpriseProfile(session?.token || "", form);
    status.value = "备案信息已保存。";
  } finally {
    loading.value = false;
  }
}
</script>

