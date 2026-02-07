<template>
  <div class="public-shell">
    <header class="public-topbar">
      <div class="brand">
        <span class="brand-mark">食安</span>
        <div>
          <strong>食品安全公众服务平台</strong>
          <span>市场监督管理局公众窗口</span>
        </div>
      </div>
      <div class="user-area">
        <div class="user-chip">
          <span>{{ publicUser.username || "公众用户" }}</span>
          <span>已登录</span>
        </div>
        <button class="ghost" type="button" @click="$emit('logout')">退出登录</button>
      </div>
    </header>

    <section class="public-hero">
      <div class="hero-copy">
        <span class="badge badge--public">公众服务</span>
        <h1>投诉入口清晰可见，处理进度全程可追踪</h1>
        <p>面向社会公众的一站式食品安全服务入口，确保投诉可追踪、信息可查证、处理可反馈。</p>
        <div class="hero-actions">
          <button class="primary" type="button" @click="$emit('open-complaint')">我要投诉</button>
          <button class="ghost" type="button" @click="$emit('open-track')">投诉进度查询</button>
        </div>
        <div class="hero-tips">
          <div>
            <strong>一键投诉</strong>
            <span>提交后自动生成编号</span>
          </div>
          <div>
            <strong>进度透明</strong>
            <span>节点与处理部门可追踪</span>
          </div>
          <div>
            <strong>政务可信</strong>
            <span>官方公示与风险提示</span>
          </div>
        </div>
      </div>
      <div class="hero-panel">
        <div class="hero-panel-card">
          <h3>投诉流程</h3>
          <ol>
            <li>填写投诉信息并提交</li>
            <li>保存投诉编号</li>
            <li>随时查询处理进度</li>
          </ol>
          <button class="primary" type="button" @click="$emit('open-complaint')">立即提交</button>
        </div>
      </div>
    </section>

    <section class="public-grid">
      <article class="service-card highlight">
        <div class="card-head">
          <span class="chip">核心服务</span>
          <h3>投诉服务</h3>
          <p>支持匿名投诉、图片上传、结果反馈，办理全流程可追踪。</p>
        </div>
        <div class="card-actions">
          <button class="primary" type="button" @click="$emit('open-complaint')">提交投诉</button>
          <button class="ghost" type="button" @click="$emit('open-track')">查询进度</button>
        </div>
      </article>

      <article class="service-card">
        <div class="card-head">
          <span class="chip">公开信息</span>
          <h3>企业公示</h3>
          <p>查询企业状态、负责人信息与监管记录。</p>
        </div>
        <button class="ghost" type="button" @click="$emit('open-enterprise')">进入公示</button>
      </article>

      <article class="service-card">
        <div class="card-head">
          <span class="chip">风险提示</span>
          <h3>预警与公告</h3>
          <p>风险预警、抽检结果与食品召回信息集中展示。</p>
        </div>
        <button class="ghost" type="button" @click="$emit('open-bulletin')">查看公告</button>
      </article>
    </section>

    <section class="public-notice">
      <div class="notice-head">
        <h3>公告通知</h3>
        <span>演示数据</span>
      </div>
      <div class="notice-list">
        <div class="notice-item">
          <div>
            <strong>重点监管企业名单更新</strong>
            <p>重点监管名单按季度更新，当前版本已发布。</p>
          </div>
          <span>2026-02-04</span>
        </div>
        <div class="notice-item">
          <div>
            <strong>节日期间食品安全提示</strong>
            <p>倡导正规渠道采购，关注食品标签与保质期。</p>
          </div>
          <span>2026-02-02</span>
        </div>
        <div class="notice-item">
          <div>
            <strong>抽检不合格产品公示</strong>
            <p>已完成处置与召回公示，详情点击查看。</p>
          </div>
          <span>2026-01-28</span>
        </div>
      </div>
    </section>

    <section class="public-risk">
      <div>
        <h3>食品安全风险提示</h3>
        <p>关注高温季节食品储存、加工卫生与冷链运输风险。</p>
      </div>
      <div class="risk-tags">
        <span>生鲜冷链</span>
        <span>餐饮后厨</span>
        <span>校园周边</span>
        <span>网络外卖</span>
      </div>
    </section>

    <footer class="public-footer">
      <div>食品安全监管热线：12315</div>
      <div>本页面为演示版，数据仅供参考</div>
    </footer>
  </div>
</template>

<script setup>
defineProps({
  publicUser: {
    type: Object,
    required: true
  }
});

defineEmits(["logout", "open-complaint", "open-track", "open-enterprise", "open-bulletin"]);
</script>

<style scoped>
.public-shell {
  min-height: 100vh;
  padding: 28px 48px 46px;
  background:
    radial-gradient(circle at 10% 10%, rgba(13, 94, 166, 0.16), transparent 45%),
    radial-gradient(circle at 85% 18%, rgba(15, 139, 141, 0.12), transparent 40%),
    var(--bg);
  display: grid;
  gap: 24px;
}

.public-topbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 14px 18px;
  border-radius: 16px;
  border: 1px solid var(--stroke);
  background: #ffffff;
  box-shadow: var(--shadow);
}

.brand {
  display: flex;
  align-items: center;
  gap: 12px;
}

.brand-mark {
  width: 40px;
  height: 40px;
  border-radius: 12px;
  display: grid;
  place-items: center;
  background: var(--nav);
  color: #fff;
  font-weight: 700;
  letter-spacing: 0.08em;
}

.brand strong {
  display: block;
  font-size: 15px;
}

.brand span {
  font-size: 12px;
  color: var(--muted);
}

.user-area {
  display: flex;
  align-items: center;
  gap: 12px;
}

.public-hero {
  display: grid;
  grid-template-columns: minmax(0, 1.4fr) minmax(0, 0.9fr);
  gap: 24px;
  align-items: stretch;
  padding: 26px 30px;
  border-radius: 20px;
  background: linear-gradient(140deg, #ffffff 0%, #f2f7ff 100%);
  border: 1px solid var(--stroke);
  box-shadow: var(--shadow);
}

.hero-copy h1 {
  margin: 10px 0 10px;
  font-size: 28px;
}

.hero-copy p {
  margin: 0;
  color: var(--muted);
  line-height: 1.7;
}

.hero-actions {
  margin-top: 16px;
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
  align-items: center;
}

.hero-tips {
  margin-top: 18px;
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
}

.hero-tips strong {
  display: block;
  font-size: 13px;
}

.hero-tips span {
  font-size: 12px;
  color: var(--muted);
}

.hero-panel {
  display: flex;
  align-items: center;
}

.hero-panel-card {
  width: 100%;
  padding: 18px 20px;
  border-radius: 16px;
  border: 1px solid var(--stroke);
  background: #ffffff;
  display: grid;
  gap: 12px;
}

.hero-panel-card h3 {
  margin: 0;
}

.hero-panel-card ol {
  margin: 0;
  padding-left: 16px;
  color: var(--muted);
  font-size: 13px;
  display: grid;
  gap: 6px;
}

.public-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 18px;
}

.service-card {
  padding: 18px;
  border-radius: 16px;
  border: 1px solid var(--stroke);
  background: #ffffff;
  display: grid;
  gap: 14px;
  box-shadow: var(--shadow);
}

.service-card.highlight {
  background: linear-gradient(135deg, #ffffff 0%, #eef6ff 100%);
  border-color: rgba(13, 94, 166, 0.35);
}

.card-head h3 {
  margin: 8px 0;
  font-size: 18px;
}

.card-head p {
  margin: 0;
  color: var(--muted);
  line-height: 1.6;
  font-size: 13px;
}

.chip {
  display: inline-flex;
  padding: 4px 10px;
  border-radius: 999px;
  background: #e9f1f8;
  font-size: 11px;
  color: var(--muted);
}

.card-actions {
  display: flex;
  gap: 10px;
}

.public-notice {
  padding: 18px 22px;
  border-radius: 16px;
  border: 1px solid var(--stroke);
  background: #ffffff;
  display: grid;
  gap: 12px;
}

.notice-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 13px;
  color: var(--muted);
}

.notice-list {
  display: grid;
  gap: 10px;
}

.notice-item {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  padding: 12px 14px;
  border-radius: 12px;
  border: 1px dashed rgba(215, 226, 238, 0.8);
  background: #f9fbff;
  font-size: 13px;
}

.notice-item p {
  margin: 4px 0 0;
  color: var(--muted);
}

.public-risk {
  padding: 18px 22px;
  border-radius: 16px;
  border: 1px solid var(--stroke);
  background: #ffffff;
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 18px;
}

.risk-tags {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.risk-tags span {
  background: #fff3e8;
  color: #b15a00;
  padding: 4px 10px;
  border-radius: 999px;
  font-size: 12px;
}

.public-footer {
  display: flex;
  justify-content: space-between;
  color: var(--muted);
  font-size: 12px;
  padding: 0 4px;
}

@media (max-width: 1100px) {
  .public-hero {
    grid-template-columns: 1fr;
  }

  .public-grid {
    grid-template-columns: 1fr;
  }

  .hero-tips {
    grid-template-columns: 1fr;
  }

  .public-risk {
    flex-direction: column;
    align-items: flex-start;
  }

  .public-footer {
    flex-direction: column;
    gap: 6px;
  }
}

@media (max-width: 720px) {
  .public-shell {
    padding: 20px 18px 36px;
  }

  .hero-actions {
    flex-direction: column;
    align-items: stretch;
  }

  .card-actions {
    flex-direction: column;
  }
}
</style>
