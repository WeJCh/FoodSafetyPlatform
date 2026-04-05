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
        <p>当前版本聚焦投诉提交与进度查询，确保投诉可追踪、处理可反馈，未开放能力暂不对外展示。</p>
        <div class="hero-actions">
          <button class="ghost" type="button" @click="$emit('open-bulletins')">监管公告</button>
          <button class="ghost" type="button" @click="$emit('open-enterprises')">企业公示</button>
          <button class="ghost" type="button" @click="$emit('open-sampling-results')">抽检结果</button>
          <button class="primary" type="button" @click="$emit('open-complaint')">我要投诉</button>
          <button class="ghost" type="button" @click="$emit('open-track')">我的投诉</button>
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
            <strong>企业公示</strong>
            <span>备案通过企业可在线查看</span>
          </div>
          <div>
            <strong>抽检公示</strong>
            <span>已发布抽检结果可在线查看</span>
          </div>
          <div>
            <strong>办理留痕</strong>
            <span>处理结果可在线查看</span>
          </div>
        </div>
      </div>
      <div class="hero-panel">
        <div class="hero-panel-card">
          <h3>投诉流程</h3>
          <ol>
            <li>填写投诉信息并提交</li>
            <li>提交后在“我的投诉”查看进度</li>
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
          <span class="chip">信息公示</span>
          <h3>监管公告</h3>
          <p>已开放公告查询，可查看监管部门发布的食品安全提醒、阶段性通知和公众须知。</p>
        </div>
        <div class="card-actions">
          <button class="ghost" type="button" @click="$emit('open-bulletins')">查看公告</button>
        </div>
      </article>

      <article class="service-card">
        <div class="card-head">
          <span class="chip">信息公示</span>
          <h3>企业公示</h3>
          <p>已开放备案通过企业查询，可查看企业基础备案信息与当前监管状态。</p>
        </div>
        <div class="card-actions">
          <button class="ghost" type="button" @click="$emit('open-enterprises')">查看企业</button>
        </div>
      </article>

      <article class="service-card">
        <div class="card-head">
          <span class="chip">信息公示</span>
          <h3>抽检结果</h3>
          <p>已开放抽检结果公示，可查看已发布的企业产品抽检结论和处置建议。</p>
        </div>
        <div class="card-actions">
          <button class="ghost" type="button" @click="$emit('open-sampling-results')">查看结果</button>
        </div>
      </article>

      <article class="service-card">
        <div class="card-head">
          <span class="chip">提交建议</span>
          <h3>材料准备</h3>
          <p>建议提前准备涉事企业名称、问题描述和现场图片，便于监管部门快速核查。</p>
        </div>
      </article>
    </section>

    <section class="public-notice">
      <div class="notice-head">
        <h3>当前服务说明</h3>
        <span>P2</span>
      </div>
      <div class="notice-list">
        <div class="notice-item">
          <div>
            <strong>投诉提交已开放</strong>
            <p>支持问题描述填写和图片上传，提交后系统自动生成投诉编号。</p>
          </div>
          <span>已启用</span>
        </div>
        <div class="notice-item">
          <div>
            <strong>进度查询已开放</strong>
            <p>可在“我的投诉”中查看受理、派单、处理等节点状态。</p>
          </div>
          <span>已启用</span>
        </div>
        <div class="notice-item">
          <div>
            <strong>企业公示已开放</strong>
            <p>公众可查看已审核通过企业的备案信息与当前监管状态。</p>
          </div>
          <span>已启用</span>
        </div>
        <div class="notice-item">
          <div>
            <strong>监管公告已开放</strong>
            <p>公众可查看监管部门发布的食品安全公告与阶段性提示信息。</p>
          </div>
          <span>已启用</span>
        </div>
        <div class="notice-item">
          <div>
            <strong>抽检结果公示已开放</strong>
            <p>公众可查询已发布的抽检结果、抽检结论与处置建议。</p>
          </div>
          <span>已启用</span>
        </div>
      </div>
    </section>

    <section class="public-risk">
      <div>
        <h3>投诉提交建议</h3>
        <p>尽量写明时间、地点、涉事企业和问题表现，并上传清晰图片，便于后续研判和处理。</p>
      </div>
      <div class="risk-tags">
        <span>问题描述</span>
        <span>现场图片</span>
        <span>涉事企业</span>
        <span>联系方式</span>
      </div>
    </section>

    <footer class="public-footer">
      <div>食品安全监管热线：12315</div>
      <div>当前版本已开放监管公告、企业公示、抽检结果公示、投诉提交与进度查询</div>
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

defineEmits(["logout", "open-complaint", "open-track", "open-enterprises", "open-bulletins", "open-sampling-results"]);
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
  grid-template-columns: repeat(auto-fit, minmax(140px, 1fr));
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
