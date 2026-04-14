<template>
  <div class="admin-shell regulator-shell">
    <aside class="admin-sidebar">
      <div class="admin-brand">监管中心</div>
      <div class="sidebar-meta">
        <span>账号：{{ regulatorUser.username }}</span>
        <span>角色：{{ regulatorUser.userType }}</span>
        <span>职责：区域管理员</span>
      </div>
      <nav class="admin-nav">
        <button :class="{ active: section === 'sampling' }" @click="handleSamplingEnter">
          抽检任务
        </button>
        <button :class="{ active: section === 'complaints' }" @click="handleComplaintEnter">
          投诉流转
        </button>
        <button :class="{ active: section === 'inspections' }" @click="handleInspectionEnter">
          检查记录
        </button>
        <button :class="{ active: section === 'rectification' }" @click="handleRectificationEnter">
          整改复核
        </button>
        <button :class="{ active: section === 'warnings' }" @click="handleWarningEnter">
          风险预警
        </button>
        <button :class="{ active: section === 'bulletins' }" @click="section = 'bulletins'">
          公告发布
        </button>
        <button :class="{ active: section === 'stats' }" @click="section = 'stats'">
          数据统计
        </button>
      </nav>
      <button class="ghost sidebar-ghost" type="button" @click="handleLogout">退出登录</button>
    </aside>

    <div class="admin-main">
      <div class="dashboard-topbar">
        <div class="dashboard-title">
          <strong>区域管理员工作台</strong>
          <span>备案审核、任务派发、抽检协同与企业监管</span>
        </div>
        <div class="user-chip">
          <span>{{ regulatorUser.username }}</span>
          <span>区域管理员</span>
        </div>
      </div>

      <div class="dashboard-content">
        <div class="card dashboard-card">

        <div class="sub-nav">
          <button :class="{ active: section === 'sampling' }" @click="handleSamplingEnter">
            抽检任务
          </button>
          <button :class="{ active: section === 'inspections' }" @click="handleInspectionEnter">
            检查记录
          </button>
          <button :class="{ active: section === 'rectification' }" @click="handleRectificationEnter">
            整改复核
          </button>
          <button :class="{ active: section === 'complaints' }" @click="handleComplaintEnter">
            投诉流转
          </button>
          <button :class="{ active: section === 'warnings' }" @click="handleWarningEnter">
            风险预警
          </button>
          <button :class="{ active: section === 'bulletins' }" @click="section = 'bulletins'">
            公告发布
          </button>
          <button :class="{ active: section === 'stats' }" @click="section = 'stats'">
            数据统计
          </button>
        </div>

        <div v-if="section === 'sampling'">
          <div class="section-title">抽检任务</div>
          <div class="dispatch-grid">
            <div class="dispatch-form">
              <div class="section-subtitle">创建抽检任务</div>
              <form class="dispatch-form-grid" @submit.prevent="handleCreateSamplingTask">
                <label>
                  选择企业
                  <select v-model="samplingForm.enterpriseId" :disabled="samplingLoading" @change="handleSamplingEnterpriseChange">
                    <option value="">请选择企业</option>
                    <option v-for="item in samplingEnterprises" :key="item.id" :value="item.id">
                      {{ item.enterpriseName }}
                    </option>
                  </select>
                </label>
                <label>
                  选择产品
                  <select
                    v-model="samplingForm.productId"
                    :disabled="samplingLoading || samplingProductLoading || !samplingForm.enterpriseId"
                  >
                    <option value="">请选择产品</option>
                    <option v-for="item in samplingProducts" :key="item.id" :value="item.id">
                      {{ item.productName }}
                    </option>
                  </select>
                </label>
                <label>
                  任务标题
                  <input v-model.trim="samplingForm.taskTitle" required placeholder="例：乳制品例行抽检" />
                </label>
                <label class="span-all">
                  任务描述
                  <textarea v-model.trim="samplingForm.taskDesc" rows="3" placeholder="填写抽检要求说明"></textarea>
                </label>
                <label>
                  优先级
                  <select v-model="samplingForm.priority">
                    <option value="MEDIUM">中</option>
                    <option value="LOW">低</option>
                    <option value="HIGH">高</option>
                  </select>
                </label>
                <label>
                  截止时间
                  <input v-model="samplingForm.deadline" type="datetime-local" required />
                </label>
                <div class="secondary-text span-all" v-if="samplingForm.enterpriseId && !samplingProductLoading && !samplingProducts.length">
                  当前企业暂无可抽检的启用产品，请先补齐产品档案。
                </div>
                <button class="primary dispatch-submit span-all" type="submit" :disabled="samplingLoading">
                  {{ samplingLoading ? "创建中..." : "创建抽检任务" }}
                </button>
              </form>
            </div>

            <div class="dispatch-list">
              <div class="section-subtitle">抽检任务列表</div>
              <form class="filter-bar filter-bar--triple" @submit.prevent="handleSamplingSearch">
                <label>
                  企业名称
                  <input v-model.trim="samplingFilters.enterpriseName" placeholder="输入企业名称" />
                </label>
                <label>
                  任务状态
                  <select v-model="samplingFilters.status">
                    <option value="">全部</option>
                    <option value="CREATED">待派发</option>
                    <option value="ASSIGNED">已派发</option>
                    <option value="COMPLETED">已完成</option>
                    <option value="CLOSED">已归档</option>
                  </select>
                </label>
                <button class="primary" type="submit" :disabled="samplingTaskLoading">
                  {{ samplingTaskLoading ? "查询中..." : "查询" }}
                </button>
              </form>

              <div class="list-table task-table">
                <div class="list-row list-header sampling-header">
                  <span>任务号</span>
                  <span>企业</span>
                  <span>产品</span>
                  <span>优先级</span>
                  <span>状态</span>
                  <span>负责人</span>
                  <span>截止时间</span>
                  <span>操作</span>
                </div>
                <div v-if="!samplingTasks.length" class="list-empty">
                  暂无抽检任务
                </div>
                <div v-for="task in samplingTasks" :key="task.id" class="list-row sampling-row">
                  <span>{{ task.taskNo }}</span>
                  <span>{{ task.enterpriseName || "-" }}</span>
                  <div>
                    <div class="primary-text">{{ task.productName || "-" }}</div>
                    <div class="secondary-text">
                      {{ task.productSpecification || "暂无规格" }}
                      <template v-if="task.samplingResult">
                        · 结果：{{ formatInspectionResult(task.samplingResult) }}
                      </template>
                    </div>
                  </div>
                  <span>{{ formatTaskPriority(task.priority) }}</span>
                  <span>{{ formatSamplingTaskStatus(task.status) }}</span>
                  <span>{{ task.assignedToName || "-" }}</span>
                  <span>{{ formatTime(task.deadline) }}</span>
                  <div class="action-buttons">
                    <button class="ghost" type="button" @click="openSamplingTaskDetail(task)">
                      查看详情
                    </button>
                    <span v-if="task.samplingResultId" class="secondary-text">
                      {{ formatSamplingPublicStatus(task.samplingPublicStatus) }}
                    </span>
                    <select
                      v-if="isSamplingTaskAssignable(task)"
                      v-model="samplingAssignments[task.id]"
                      :disabled="samplingTaskLoading || isTaskDeadlineExceeded(task.deadline)"
                    >
                      <option value="">选择执法人员</option>
                      <option
                        v-for="item in getEnforcers(task.regionId)"
                        :key="item.id"
                        :value="item.id"
                      >
                        {{ item.name }}
                      </option>
                    </select>
                    <button
                      v-if="isSamplingTaskAssignable(task)"
                      class="ghost"
                      type="button"
                      :disabled="samplingTaskLoading || isTaskDeadlineExceeded(task.deadline)"
                      @click="handleAssignSamplingTask(task)"
                    >
                      派发
                    </button>
                    <button
                      v-if="task.samplingResultId && task.samplingPublicStatus !== 'PUBLISHED'"
                      class="primary"
                      type="button"
                      :disabled="samplingTaskLoading"
                      @click="handlePublishSamplingResult(task)"
                    >
                      公示
                    </button>
                    <button
                      v-if="task.samplingResultId && task.samplingPublicStatus === 'PUBLISHED'"
                      class="ghost"
                      type="button"
                      :disabled="samplingTaskLoading"
                      @click="handleOfflineSamplingResult(task)"
                    >
                      下线
                    </button>
                    <button
                      v-if="isSamplingTaskClosable(task)"
                      class="ghost"
                      type="button"
                      :disabled="samplingTaskLoading"
                      @click="handleCloseSamplingTask(task)"
                    >
                      归档
                    </button>
                  </div>
                </div>
              </div>

              <div class="pager">
                <span>共 {{ samplingTotal }} 条，{{ samplingPage }}/{{ samplingPages }} 页</span>
                <div class="pager-actions">
                  <button
                    class="ghost"
                    type="button"
                    :disabled="samplingPage <= 1"
                    @click="changeSamplingPage(samplingPage - 1)"
                  >
                    上一页
                  </button>
                  <button
                    class="ghost"
                    type="button"
                    :disabled="samplingPage >= samplingPages"
                    @click="changeSamplingPage(samplingPage + 1)"
                  >
                    下一页
                  </button>
                </div>
              </div>

              <div v-if="samplingDetailTask" class="modal-mask" @click.self="closeSamplingTaskDetail">
                <div class="modal-card task-detail-modal">
                  <div class="modal-title">抽检任务详情</div>
                  <div class="task-detail-header">
                    <span class="task-chip task-chip--status">{{ formatSamplingTaskStatus(samplingDetailTask.status) }}</span>
                    <span class="task-chip task-chip--priority">{{ formatTaskPriority(samplingDetailTask.priority) }}</span>
                    <span class="task-chip">{{ samplingDetailTask.taskNo || "-" }}</span>
                  </div>
                  <div class="task-detail-grid">
                    <section class="task-detail-section">
                      <div class="task-detail-section-title">企业信息</div>
                      <div v-if="samplingDetailLoading" class="task-detail-loading">加载企业信息中...</div>
                      <div v-else class="task-detail-fields">
                        <div class="task-detail-field">
                          <span>企业名称</span>
                          <strong>{{ samplingDetailEnterprise?.enterpriseName || samplingDetailTask.enterpriseName || "-" }}</strong>
                        </div>
                        <div class="task-detail-field">
                          <span>负责人姓名</span>
                          <strong>{{ samplingDetailEnterprise?.principal || "-" }}</strong>
                        </div>
                        <div class="task-detail-field">
                          <span>所属区域</span>
                          <strong>{{ samplingDetailRegionName || "-" }}</strong>
                        </div>
                        <div class="task-detail-field task-detail-field--full">
                          <span>详细地址</span>
                          <strong>{{ samplingDetailEnterprise?.addressDetail || "-" }}</strong>
                        </div>
                      </div>
                    </section>
                    <section class="task-detail-section">
                      <div class="task-detail-section-title">抽检信息</div>
                      <div class="task-detail-fields">
                        <div class="task-detail-field">
                          <span>任务标题</span>
                          <strong>{{ samplingDetailTask.taskTitle || "-" }}</strong>
                        </div>
                        <div class="task-detail-field">
                          <span>抽检产品</span>
                          <strong>{{ samplingDetailTask.productName || "-" }}</strong>
                        </div>
                        <div class="task-detail-field">
                          <span>产品类别</span>
                          <strong>{{ samplingDetailTask.productCategory || "-" }}</strong>
                        </div>
                        <div class="task-detail-field">
                          <span>产品规格</span>
                          <strong>{{ samplingDetailTask.productSpecification || "-" }}</strong>
                        </div>
                        <div class="task-detail-field">
                          <span>当前执行人</span>
                          <strong>{{ samplingDetailTask.assignedToName || "-" }}</strong>
                        </div>
                        <div class="task-detail-field">
                          <span>公示状态</span>
                          <strong>{{ formatSamplingPublicStatus(samplingDetailTask.samplingPublicStatus) }}</strong>
                        </div>
                        <div class="task-detail-field">
                          <span>截止时间</span>
                          <strong>{{ formatTime(samplingDetailTask.deadline) }}</strong>
                        </div>
                        <div class="task-detail-field">
                          <span>抽检结果</span>
                          <strong>{{ formatInspectionResult(samplingDetailTask.samplingResult) }}</strong>
                        </div>
                        <div class="task-detail-field task-detail-field--full">
                          <span>抽检结论</span>
                          <strong>{{ samplingDetailTask.samplingConclusion || "结果提交后显示" }}</strong>
                        </div>
                        <div class="task-detail-field task-detail-field--full">
                          <span>任务描述</span>
                          <strong>{{ samplingDetailTask.taskDesc || "暂无任务描述" }}</strong>
                        </div>
                      </div>
                    </section>
                  </div>
                  <div class="modal-actions">
                    <button class="ghost" type="button" @click="closeSamplingTaskDetail">关闭</button>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>

        <div v-else-if="section === 'inspections'">
          <div class="section-title">检查记录</div>
          <form class="filter-bar filter-bar--quad" @submit.prevent="handleInspectionSearch">
            <label>
              企业名称
              <input v-model.trim="inspectionFilters.enterpriseName" placeholder="输入企业名称" />
            </label>
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

          <div class="list-table inspection-table">
            <div class="list-row list-header inspection-header">
              <span>企业名称</span>
              <span>检查日期</span>
              <span>结果</span>
              <span>更新时间</span>
              <span>操作</span>
            </div>
            <div v-if="!inspectionRecords.length" class="list-empty">
              暂无检查记录
            </div>
            <div v-for="record in inspectionRecords" :key="record.id" class="list-row inspection-row">
              <span>{{ record.enterpriseName || "-" }}</span>
              <span>{{ record.inspectionDate || "-" }}</span>
              <span>{{ formatInspectionResult(record.result) }}</span>
              <span>{{ formatTime(record.updateTime) }}</span>
              <button class="ghost" type="button" @click="openInspectionDetail(record)">查看详情</button>
            </div>
          </div>

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
                    <div v-if="!inspectionDetail.items || !inspectionDetail.items.length" class="modal-empty">暂无检查明细</div>
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

        <div v-else-if="section === 'rectification'">
          <div class="section-title">整改复核</div>
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
            <label>
              企业名称
              <input v-model.trim="rectificationFilters.enterpriseName" placeholder="输入企业名称" />
            </label>
            <button class="primary" type="submit" :disabled="rectificationLoading">
              {{ rectificationLoading ? "查询中..." : "查询" }}
            </button>
          </form>

          <div class="list-table">
            <div class="list-row list-header rectification-header">
              <span>企业</span>
              <span>整改要求</span>
              <span>状态</span>
              <span>整改时限</span>
              <span>进展说明</span>
              <span>更新时间</span>
              <span>操作</span>
            </div>
            <div v-if="!rectificationRecords.length" class="list-empty">
              暂无整改任务
            </div>
            <div v-for="item in rectificationRecords" :key="item.id" class="list-row rectification-row">
              <span>{{ item.enterpriseName || "-" }}</span>
              <div class="rectification-text" :title="item.rectificationDesc || '-'">
                {{ item.rectificationDesc || "-" }}
              </div>
              <span>{{ formatRectificationStatus(item.status) }}</span>
              <span :class="['rectification-sla', `rectification-sla--${rectificationSlaClass(item)}`]">
                {{ formatRectificationSla(item) }}
              </span>
              <div class="rectification-text" :title="item.progress || '-'">
                {{ item.progress || "-" }}
              </div>
              <span>{{ formatTime(item.updateTime) }}</span>
              <div class="action-buttons">
                <button class="ghost" type="button" @click="openRectificationDetail(item)">
                  查看详情
                </button>
                <button
                  v-if="item.status === 'SUBMITTED'"
                  class="primary"
                  type="button"
                  :disabled="rectificationLoading"
                  @click="handleReviewRectification(item, { action: 'CONFIRM' })"
                >
                  快速通过
                </button>
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
            :highlight-latest-submit="true"
            :focus-action-type="rectificationFocusActionType"
            :reviewable="Boolean(rectificationDetail && rectificationDetail.status === 'SUBMITTED')"
            :reviewing="rectificationLoading"
            @close="closeRectificationDetail"
            @review="handleReviewRectification"
          />
        </div>

        <div v-else-if="section === 'complaints'">
          <div class="section-title">投诉流转</div>
          <form class="filter-bar filter-bar--quad" @submit.prevent="handleComplaintSearch">
            <label>
              状态
              <select v-model="complaintFilters.status">
                <option value="">全部</option>
                <option value="SUBMITTED">已提交</option>
                <option value="PENDING">已受理</option>
                <option value="ASSIGNED">已派发</option>
                <option value="PROCESSING">处理中</option>
                <option value="FEEDBACKED">已反馈</option>
                <option value="REJECTED">已驳回</option>
              </select>
            </label>
            <label>
              企业名称
              <input v-model.trim="complaintFilters.enterpriseName" placeholder="输入企业名称" />
            </label>
            <label>
              处理人
              <input v-model.trim="complaintFilters.assignedToName" placeholder="执法人员姓名" />
            </label>
            <label>
              指派人
              <input v-model.trim="complaintFilters.assignedByName" placeholder="管理员姓名" />
            </label>
            <button class="primary" type="submit" :disabled="complaintLoading">
              {{ complaintLoading ? "查询中..." : "查询" }}
            </button>
          </form>

          <div class="list-table complaint-table">
            <div class="list-row list-header complaint-header">
              <span>投诉号</span>
              <span>企业</span>
              <span>状态</span>
              <span>指派人</span>
              <span>处理人</span>
              <span>更新时间</span>
              <span>操作</span>
            </div>
            <div v-if="!complaintRecords.length" class="list-empty">
              暂无投诉
            </div>
            <div v-for="item in complaintRecords" :key="item.id" class="list-row complaint-row">
              <span>{{ item.complaintNo }}</span>
              <span>{{ item.enterpriseName || "-" }}</span>
              <span>{{ formatComplaintStatus(item.status) }}</span>
              <span>{{ item.assignedByName || "-" }}</span>
              <span>{{ item.assignedToName || "-" }}</span>
              <span>{{ formatTime(item.updateTime) }}</span>
              <div class="action-buttons">
                <button class="ghost" type="button" @click="handleViewComplaint(item)">
                  查看详情
                </button>
                <button
                  v-if="item.status === 'SUBMITTED'"
                  class="primary"
                  type="button"
                  :disabled="complaintLoading"
                  @click="handleAcceptComplaint(item)"
                >
                  受理
                </button>
              </div>
            </div>
          </div>

          <div class="pager">
            <span>共 {{ complaintTotal }} 条，{{ complaintPage }}/{{ complaintPages }} 页</span>
            <div class="pager-actions">
              <button
                class="ghost"
                type="button"
                :disabled="complaintPage <= 1"
                @click="changeComplaintPage(complaintPage - 1)"
              >
                上一页
              </button>
              <button
                class="ghost"
                type="button"
                :disabled="complaintPage >= complaintPages"
                @click="changeComplaintPage(complaintPage + 1)"
              >
                下一页
              </button>
            </div>
          </div>

        </div>

        <div v-else-if="section === 'warnings'">
          <div class="section-title">风险预警</div>
          <div class="warning-quick-tools">
            <button
              class="ghost warning-quick-toggle"
              :class="{ active: warningOnlyPending }"
              type="button"
              @click="toggleWarningOnlyPending"
            >
              仅看待处理：{{ warningOnlyPending ? "开启" : "关闭" }}
            </button>
            <span v-if="warningOnlyPending" class="warning-quick-tip">已固定筛选状态为“待处理”</span>
          </div>
          <form class="filter-bar filter-bar--five" @submit.prevent="handleWarningSearch">
            <label>
              状态
              <select v-model="warningFilters.status" :disabled="warningOnlyPending">
                <option value="">全部</option>
                <option value="OPEN">待处理</option>
                <option value="PROCESSING">处理中</option>
                <option value="RESOLVED">已解决</option>
                <option value="CLOSED">已归档</option>
              </select>
            </label>
            <label>
              等级
              <select v-model="warningFilters.level">
                <option value="">全部</option>
                <option value="L1">一级</option>
                <option value="L2">二级</option>
              </select>
            </label>
            <label>
              预警类型
              <input v-model.trim="warningFilters.warningType" placeholder="例：SLA_OVERDUE_SUBMIT" />
            </label>
            <label>
              业务类型
              <input v-model.trim="warningFilters.bizType" placeholder="例：RECTIFICATION" />
            </label>
            <label>
              关键词
              <input v-model.trim="warningFilters.keyword" placeholder="标题或内容关键词" />
            </label>
            <button class="primary" type="submit" :disabled="warningLoading || warningActionLoading">
              {{ warningLoading ? "查询中..." : "查询" }}
            </button>
          </form>

          <div class="list-table warning-table">
            <div class="list-row list-header warning-header">
              <span>预警编号</span>
              <span>预警标题</span>
              <span>等级</span>
              <span>状态</span>
              <span>触发次数</span>
              <span>最近触发</span>
              <span>操作</span>
            </div>
            <div v-if="!warningRecords.length" class="list-empty">
              暂无预警记录
            </div>
            <div v-for="item in warningRecords" :key="item.id" class="list-row warning-row">
              <span>{{ item.warningNo || "-" }}</span>
              <span class="warning-title-cell" :title="item.title || '-'">{{ item.title || "-" }}</span>
              <span>{{ formatWarningLevel(item.level) }}</span>
              <span :class="['warning-status-chip', `warning-status-chip--${warningStatusClass(item.status)}`]">
                {{ formatWarningStatus(item.status) }}
              </span>
              <span>{{ item.triggerCount || 0 }}</span>
              <span>{{ formatTime(item.lastOccurTime) }}</span>
              <div class="action-buttons">
                <button class="ghost" type="button" @click="openWarningDetail(item)">
                  查看详情
                </button>
                <button
                  v-if="warningQuickAction(item.status)"
                  class="primary"
                  type="button"
                  :disabled="warningActionLoading"
                  @click="handleWarningAction(item, warningQuickAction(item.status).actionType)"
                >
                  {{ warningQuickAction(item.status).label }}
                </button>
              </div>
            </div>
          </div>

          <div class="pager">
            <span>共 {{ warningTotal }} 条，{{ warningPage }}/{{ warningPages }} 页</span>
            <div class="pager-actions">
              <button
                class="ghost"
                type="button"
                :disabled="warningPage <= 1 || warningLoading"
                @click="changeWarningPage(warningPage - 1)"
              >
                上一页
              </button>
              <button
                class="ghost"
                type="button"
                :disabled="warningPage >= warningPages || warningLoading"
                @click="changeWarningPage(warningPage + 1)"
              >
                下一页
              </button>
            </div>
          </div>

          <div v-if="warningDetailVisible" class="modal-mask" @click.self="closeWarningDetail">
            <div class="modal-card warning-detail-modal">
              <div class="modal-title">预警详情</div>
              <div class="modal-body">
                <div v-if="warningDetailLoading" class="modal-empty">详情加载中...</div>
                <template v-else-if="warningDetail">
                  <div class="warning-summary-grid">
                    <div class="warning-summary-item">
                      <span>预警编号</span>
                      <strong>{{ warningDetail.warningNo || "-" }}</strong>
                    </div>
                    <div class="warning-summary-item">
                      <span>状态</span>
                      <strong>{{ formatWarningStatus(warningDetail.status) }}</strong>
                    </div>
                    <div class="warning-summary-item">
                      <span>等级</span>
                      <strong>{{ formatWarningLevel(warningDetail.level) }}</strong>
                    </div>
                    <div class="warning-summary-item">
                      <span>触发次数</span>
                      <strong>{{ warningDetail.triggerCount || 0 }}</strong>
                    </div>
                  </div>

                  <div class="modal-field">
                    <span>预警标题</span>
                    <strong>{{ warningDetail.title || "-" }}</strong>
                  </div>
                  <div class="modal-field">
                    <span>预警内容</span>
                    <strong>{{ warningDetail.content || "-" }}</strong>
                  </div>
                  <div class="modal-field">
                    <span>负载数据</span>
                    <pre class="warning-payload">{{ formatWarningPayload(warningDetail.payloadJson) }}</pre>
                  </div>
                  <div class="modal-field">
                    <span>处理记录</span>
                    <div class="warning-timeline-list">
                      <div
                        v-if="!warningDetail.processLogs || !warningDetail.processLogs.length"
                        class="modal-empty"
                      >
                        暂无处理记录
                      </div>
                      <div
                        v-for="log in warningDetail.processLogs || []"
                        :key="log.id"
                        class="warning-timeline-item"
                      >
                        <span
                          class="warning-timeline-dot"
                          :class="warningTimelineDotClass(log.actionType)"
                        ></span>
                        <div class="warning-timeline-content">
                          <div class="warning-timeline-header">
                            <div class="warning-timeline-name">{{ formatWarningAction(log.actionType) }}</div>
                            <div class="warning-timeline-time">{{ formatTime(log.createTime) }}</div>
                          </div>
                          <div class="warning-timeline-meta">操作人：{{ log.operatorName || "-" }}</div>
                          <div class="warning-timeline-desc">{{ log.actionComment || "无说明" }}</div>
                        </div>
                      </div>
                    </div>
                  </div>
                </template>
              </div>
                <div class="modal-actions warning-detail-actions">
                  <button
                    v-if="warningDetail && canJumpWarningComplaint(warningDetail)"
                    class="ghost"
                    type="button"
                    @click="jumpToWarningComplaint(warningDetail)"
                  >
                    跳转投诉详情
                  </button>
                  <button
                    v-if="warningDetail && canJumpWarningRectification(warningDetail)"
                    class="ghost"
                    type="button"
                    @click="jumpToWarningRectification(warningDetail)"
                  >
                    跳转整改详情
                  </button>
                  <button
                    v-if="warningDetail && warningQuickAction(warningDetail.status)"
                    class="primary"
                    type="button"
                    :disabled="warningActionLoading"
                  @click="handleWarningAction(warningDetail, warningQuickAction(warningDetail.status).actionType)"
                >
                  {{ warningQuickAction(warningDetail.status).label }}
                </button>
                <button class="ghost" type="button" @click="closeWarningDetail">关闭</button>
              </div>
            </div>
          </div>
        </div>

        <div v-else-if="section === 'stats'" class="stats-dashboard">
          <SupervisionOverviewPanel :token="token" mode="admin" />
          <WarningStatsPanel :token="token" mode="admin" />
        </div>

        <div v-else-if="section === 'bulletins'">
          <RegulatorBulletinManager :token="token" />
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
import { computed, onMounted, reactive, ref, watch } from "vue";
import { useRoute, useRouter } from "vue-router";
import { acceptComplaint, fetchComplaints } from "../api/complaint";
import { getActiveSession, performLogout } from "../session/authRuntime";
import {
  fetchEnterpriseDetail,
  fetchEnterpriseProducts,
  fetchEligibleRegulators,
  fetchEnterprises,
  fetchWarningRecordDetail,
  fetchWarningRecords,
  fetchRegionPath,
  processWarningRecord,
} from "../api/regulation";
import {
  assignSamplingTask,
  closeSamplingTask,
  createSamplingTask,
  fetchInspectionRecordDetail,
  fetchInspectionRecords,
  fetchSamplingTasks,
  offlineSamplingResult,
  publishSamplingResult,
  fetchRectificationActions,
  fetchRectificationDetail,
  fetchRectifications,
  reviewRectification
} from "../api/regulationOperation";
import RegulatorBulletinManager from "../components/RegulatorBulletinManager.vue";
import RectificationDetailModal from "../components/RectificationDetailModal.vue";
import SupervisionOverviewPanel from "../components/SupervisionOverviewPanel.vue";
import WarningStatsPanel from "../components/WarningStatsPanel.vue";
import { formatByMap, formatTime } from "../utils/formatters";
import {
  complaintStatusMap,
  warningActionMap,
  warningLevelMap,
  warningStatusMap
} from "../utils/statusMaps";

const router = useRouter();
const route = useRoute();
const session = computed(() => getActiveSession() || {});
const regulatorUser = computed(() => session.value);
const token = computed(() => session.value.token || "");

function normalizeSection(value) {
  const sectionMap = new Set([
    "sampling",
    "inspections",
    "complaints",
    "rectification",
    "warnings",
    "bulletins",
    "stats"
  ]);
  return sectionMap.has(value) ? value : "sampling";
}

function getSectionRouteName(sectionValue) {
  const routeNameMap = {
    sampling: "regulator-admin-sampling",
    inspections: "regulator-admin-inspections",
    complaints: "regulator-admin-complaints",
    rectification: "regulator-admin-rectifications",
    warnings: "regulator-admin-warnings",
    bulletins: "regulator-admin-bulletins",
    stats: "regulator-admin-stats"
  };
  return routeNameMap[sectionValue] || "regulator-admin-sampling";
}

function resolveCurrentSection() {
  return normalizeSection(route.meta?.initialSection);
}

const section = ref(resolveCurrentSection());

const status = reactive({ message: "", type: "" });
const samplingLoading = ref(false);
const samplingTaskLoading = ref(false);
const samplingProductLoading = ref(false);
const samplingEnterprises = ref([]);
const samplingProducts = ref([]);
const samplingForm = reactive({
  enterpriseId: "",
  productId: "",
  taskTitle: "",
  taskDesc: "",
  priority: "MEDIUM",
  deadline: ""
});
const samplingFilters = reactive({
  enterpriseName: "",
  status: ""
});
const samplingTasks = ref([]);
const samplingPage = ref(1);
const samplingSize = ref(8);
const samplingTotal = ref(0);
const samplingPages = ref(1);
const samplingAssignments = reactive({});
const samplingDetailTask = ref(null);
const samplingDetailEnterprise = ref(null);
const samplingDetailRegionName = ref("-");
const samplingDetailLoading = ref(false);
const enforcerMap = reactive({});
const inspectionFilters = reactive({
  enterpriseName: "",
  result: "",
  startDate: "",
  endDate: ""
});
const inspectionRecords = ref([]);
const inspectionLoading = ref(false);
const inspectionPage = ref(1);
const inspectionSize = ref(8);
const inspectionTotal = ref(0);
const inspectionPages = ref(1);
const inspectionDetail = ref(null);
const complaintLoading = ref(false);
const complaintRecords = ref([]);
const complaintPage = ref(1);
const complaintSize = ref(8);
const complaintTotal = ref(0);
const complaintPages = ref(1);
const complaintFilters = reactive({
  status: "",
  enterpriseName: "",
  assignedToName: "",
  assignedByName: ""
});
const rectificationLoading = ref(false);
const rectificationRecords = ref([]);
const rectificationPage = ref(1);
const rectificationSize = ref(8);
const rectificationTotal = ref(0);
const rectificationPages = ref(1);
const rectificationFilters = reactive({
  status: "",
  enterpriseName: ""
});
const rectificationDetailVisible = ref(false);
const rectificationDetail = ref(null);
const rectificationActionLogs = ref([]);
const rectificationDetailLoading = ref(false);
const rectificationFocusActionType = ref("");
const warningLoading = ref(false);
const warningActionLoading = ref(false);
const warningDetailLoading = ref(false);
const warningRecords = ref([]);
const warningPage = ref(1);
const warningSize = ref(8);
const warningTotal = ref(0);
const warningPages = ref(1);
const warningFilters = reactive({
  status: "",
  level: "",
  warningType: "",
  bizType: "",
  keyword: ""
});
const warningOnlyPending = ref(false);
const warningStatusBackup = ref("");
const warningDetailVisible = ref(false);
const warningDetail = ref(null);

function setStatus(message, type = "info") {
  status.message = message;
  status.type = type;
}

const sectionLabelMap = {
  sampling: "抽检任务",
  inspections: "检查记录",
  rectification: "整改复核",
  complaints: "投诉流转",
  warnings: "风险预警",
  bulletins: "公告发布",
  stats: "数据统计"
};

const sectionLabel = computed(() => sectionLabelMap[section.value] || "当前模块");

const taskStatusMap = {
  CREATED: "待派发",
  ASSIGNED: "已派发",
  IN_PROGRESS: "执行中",
  COMPLETED: "已完成",
  CLOSED: "已归档"
};

const taskPriorityMap = {
  LOW: "低",
  MEDIUM: "中",
  HIGH: "高"
};
const samplingTaskStatusMap = {
  CREATED: "待派发",
  ASSIGNED: "已派发",
  COMPLETED: "已完成",
  CLOSED: "已归档"
};
const samplingPublicStatusMap = {
  DRAFT: "待公示",
  PUBLISHED: "已公示",
  OFFLINE: "已下线"
};

const inspectionResultMap = {
  PASS: "合格",
  FAIL: "不合格"
};
const rectificationStatusMap = {
  ONGOING: "整改中",
  SUBMITTED: "待复核",
  REWORK: "打回重做",
  CONFIRMED: "已确认"
};
function formatComplaintStatus(value) {
  return formatByMap(value, complaintStatusMap);
}

function formatTaskStatus(value) {
  return taskStatusMap[value] || value || "-";
}

function formatTaskPriority(value) {
  return taskPriorityMap[value] || value || "-";
}

function formatSamplingTaskStatus(value) {
  return samplingTaskStatusMap[value] || value || "-";
}

function formatSamplingPublicStatus(value) {
  return samplingPublicStatusMap[value] || (value ? value : "未生成");
}

function formatInspectionResult(value) {
  return inspectionResultMap[value] || value || "-";
}

function formatRectificationStatus(value) {
  return rectificationStatusMap[value] || value || "-";
}

function formatWarningStatus(value) {
  return formatByMap(value, warningStatusMap);
}

function formatWarningLevel(value) {
  return formatByMap(value, warningLevelMap);
}

function formatWarningAction(value) {
  return formatByMap(value, warningActionMap);
}

function warningStatusClass(value) {
  if (value === "OPEN") return "open";
  if (value === "PROCESSING") return "processing";
  if (value === "RESOLVED") return "resolved";
  if (value === "CLOSED") return "closed";
  return "unknown";
}

function warningQuickAction(statusValue) {
  if (statusValue === "OPEN") return { actionType: "PROCESS", label: "开始处理" };
  if (statusValue === "PROCESSING") return { actionType: "RESOLVE", label: "标记解决" };
  return null;
}

function warningTimelineDotClass(actionType) {
  const value = String(actionType || "").toUpperCase();
  if (value === "RESOLVE" || value === "AUTO_ARCHIVE") return "done";
  if (value === "PROCESS" || value === "ASSIGN" || value === "AUTO_LEVEL_UP") return "active";
  return "";
}

function formatWarningPayload(payloadJson) {
  if (!payloadJson) return "-";
  try {
    return JSON.stringify(JSON.parse(payloadJson), null, 2);
  } catch {
    return String(payloadJson);
  }
}

function canJumpWarningComplaint(warning) {
  return String(warning?.bizType || "").toUpperCase() === "COMPLAINT" && Number(warning?.bizId) > 0;
}

function canJumpWarningRectification(warning) {
  return String(warning?.bizType || "").toUpperCase() === "RECTIFICATION" && Number(warning?.bizId) > 0;
}

function jumpToWarningComplaint(warning) {
  if (!canJumpWarningComplaint(warning)) return;
  router.push({
    name: "regulator-admin-complaint-detail",
    params: { complaintId: Number(warning.bizId) },
    query: { from: "warnings" }
  }).catch(() => {});
  closeWarningDetail();
}

async function jumpToWarningRectification(warning) {
  if (!canJumpWarningRectification(warning)) return;
  const rectificationId = Number(warning.bizId);
  closeWarningDetail();
  section.value = "rectification";
  rectificationDetailVisible.value = true;
  rectificationDetailLoading.value = true;
  rectificationDetail.value = { id: rectificationId };
  rectificationActionLogs.value = [];
  rectificationFocusActionType.value = "";
  try {
    await loadRectificationDetail(rectificationId);
  } catch {
    // 具体错误提示由 loadRectificationDetail 内部处理。
  }
}

async function handleSamplingEnter() {
  section.value = "sampling";
  await loadSampling();
}

async function handleInspectionEnter() {
  section.value = "inspections";
  await loadInspections();
}

async function handleComplaintEnter() {
  section.value = "complaints";
  await loadComplaints();
}

async function handleRectificationEnter() {
  section.value = "rectification";
  await loadRectifications();
}

async function handleWarningEnter() {
  section.value = "warnings";
  await loadWarnings();
}

async function loadSampling() {
  await Promise.all([loadSamplingEnterprises(), loadSamplingTasks()]);
}

async function loadComplaints() {
  complaintLoading.value = true;
  setStatus("");
  try {
    const data = await fetchComplaints(token.value, {
      ...complaintFilters,
      page: complaintPage.value,
      size: complaintSize.value
    });
    complaintRecords.value = data.records || [];
    complaintTotal.value = data.total || 0;
    complaintPage.value = data.page || 1;
    complaintSize.value = data.size || complaintSize.value;
    complaintPages.value = data.pages || 1;
  } catch (error) {
    setStatus(error.message || "加载投诉列表失败", "error");
  } finally {
    complaintLoading.value = false;
  }
}

async function loadSamplingEnterprises() {
  samplingLoading.value = true;
  try {
    const data = await fetchEnterprises(token.value, {
      approvalStatus: "APPROVED",
      page: 1,
      size: 100
    });
    samplingEnterprises.value = data.records || [];
  } catch (error) {
    setStatus(error.message || "加载企业列表失败", "error");
  } finally {
    samplingLoading.value = false;
  }
}

async function loadSamplingProducts(enterpriseId) {
  if (!enterpriseId) {
    samplingProducts.value = [];
    return;
  }
  samplingProductLoading.value = true;
  try {
    const data = await fetchEnterpriseProducts(token.value, enterpriseId);
    samplingProducts.value = Array.isArray(data)
      ? data.filter((item) => item?.status === "ACTIVE")
      : [];
  } catch (error) {
    samplingProducts.value = [];
    setStatus(error.message || "加载企业产品失败", "error");
  } finally {
    samplingProductLoading.value = false;
  }
}

async function handleSamplingEnterpriseChange() {
  samplingForm.productId = "";
  await loadSamplingProducts(samplingForm.enterpriseId);
}

async function loadSamplingTasks() {
  samplingTaskLoading.value = true;
  setStatus("");
  try {
    const data = await fetchSamplingTasks(token.value, {
      ...samplingFilters,
      page: samplingPage.value,
      size: samplingSize.value
    });
    samplingTasks.value = data.records || [];
    samplingTotal.value = data.total || 0;
    samplingPage.value = data.page || 1;
    samplingSize.value = data.size || samplingSize.value;
    samplingPages.value = data.pages || 1;
    const regionIds = samplingTasks.value
      .map((task) => task.regionId)
      .filter((value) => value);
    await Promise.all(regionIds.map((id) => ensureEnforcers(id)));
  } catch (error) {
    setStatus(error.message || "加载抽检任务失败", "error");
  } finally {
    samplingTaskLoading.value = false;
  }
}

async function loadInspections() {
  inspectionLoading.value = true;
  setStatus("");
  try {
    const data = await fetchInspectionRecords(token.value, {
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
    const data = await fetchRectifications(token.value, {
      ...rectificationFilters,
      page: rectificationPage.value,
      size: rectificationSize.value
    });
    rectificationRecords.value = data.records || [];
    rectificationTotal.value = data.total || 0;
    rectificationPage.value = data.page || 1;
    rectificationSize.value = data.size || rectificationSize.value;
    rectificationPages.value = data.pages || 1;
    // 列表刷新后同步详情弹窗数据，保证动作时间线最新。
    if (rectificationDetailVisible.value && rectificationDetail.value?.id) {
      await loadRectificationDetail(rectificationDetail.value.id, true);
    }
  } catch (error) {
    setStatus(error.message || "加载整改任务失败", "error");
  } finally {
    rectificationLoading.value = false;
  }
}

async function loadWarnings() {
  warningLoading.value = true;
  setStatus("");
  try {
    const data = await fetchWarningRecords(token.value, {
      ...warningFilters,
      page: warningPage.value,
      size: warningSize.value
    });
    warningRecords.value = data.records || [];
    warningTotal.value = data.total || 0;
    warningPage.value = data.page || 1;
    warningSize.value = data.size || warningSize.value;
    warningPages.value = data.pages || 1;
  } catch (error) {
    setStatus(error.message || "加载预警列表失败", "error");
  } finally {
    warningLoading.value = false;
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

async function handleComplaintSearch() {
  complaintPage.value = 1;
  await loadComplaints();
}

async function changeComplaintPage(nextPage) {
  complaintPage.value = nextPage;
  await loadComplaints();
}

async function handleInspectionSearch() {
  inspectionPage.value = 1;
  await loadInspections();
}

async function changeInspectionPage(nextPage) {
  inspectionPage.value = nextPage;
  await loadInspections();
}

async function handleRectificationSearch() {
  rectificationPage.value = 1;
  await loadRectifications();
}

async function changeRectificationPage(nextPage) {
  rectificationPage.value = nextPage;
  await loadRectifications();
}

async function handleWarningSearch() {
  warningPage.value = 1;
  await loadWarnings();
}

async function changeWarningPage(nextPage) {
  warningPage.value = nextPage;
  await loadWarnings();
}

async function toggleWarningOnlyPending() {
  warningOnlyPending.value = !warningOnlyPending.value;
  if (warningOnlyPending.value) {
    warningStatusBackup.value = warningFilters.status;
    warningFilters.status = "OPEN";
  } else {
    warningFilters.status = warningStatusBackup.value || "";
  }
  warningPage.value = 1;
  await loadWarnings();
}

async function openWarningDetail(item) {
  if (!item?.id) return;
  warningDetailVisible.value = true;
  warningDetailLoading.value = true;
  warningDetail.value = null;
  try {
    warningDetail.value = await fetchWarningRecordDetail(token.value, item.id);
  } catch (error) {
    setStatus(error.message || "加载预警详情失败", "error");
    warningDetailVisible.value = false;
  } finally {
    warningDetailLoading.value = false;
  }
}

function closeWarningDetail() {
  warningDetailVisible.value = false;
  warningDetail.value = null;
  warningDetailLoading.value = false;
}

async function handleWarningAction(target, actionType) {
  const warningId = target?.id;
  if (!warningId || !actionType) return;
  warningActionLoading.value = true;
  setStatus("");
  try {
    const detail = await processWarningRecord(token.value, warningId, { actionType });
    if (warningDetailVisible.value && warningDetail.value?.id === warningId) {
      warningDetail.value = detail;
    }
    setStatus(`预警已执行${formatWarningAction(actionType)}`, "success");
    await loadWarnings();
  } catch (error) {
    setStatus(error.message || "预警处理失败", "error");
  } finally {
    warningActionLoading.value = false;
  }
}

async function handleReviewRectification(target, payload) {
  const item = target?.id ? target : rectificationDetail.value;
  if (!item?.id) return;
  const reviewPayload = payload || target || {};
  if (!reviewPayload.action) {
    setStatus("缺少复核动作", "error");
    return;
  }
  rectificationLoading.value = true;
  setStatus("");
  try {
    await reviewRectification(token.value, item.id, reviewPayload);
    setStatus(reviewPayload.action === "REWORK" ? "整改任务已打回重做" : "整改任务已确认复核", "success");
    await loadRectifications();
    if (rectificationDetailVisible.value && rectificationDetail.value?.id === item.id) {
      closeRectificationDetail();
    }
  } catch (error) {
    setStatus(error.message || "整改复核失败", "error");
  } finally {
    rectificationLoading.value = false;
  }
}

async function openRectificationDetail(item) {
  if (!item) return;
  rectificationDetailLoading.value = true;
  rectificationDetail.value = item;
  rectificationActionLogs.value = [];
  // 打开详情后默认定位到最近一次企业提交整改动作。
  rectificationFocusActionType.value = "ENTERPRISE_SUBMIT";
  rectificationDetailVisible.value = true;
  await loadRectificationDetail(item.id);
}

function closeRectificationDetail() {
  rectificationDetailVisible.value = false;
  rectificationDetail.value = null;
  rectificationActionLogs.value = [];
  rectificationDetailLoading.value = false;
  rectificationFocusActionType.value = "";
}

function handleViewComplaint(item) {
  if (!item?.id) return;
  router.push({
    name: "regulator-admin-complaint-detail",
    params: { complaintId: item.id },
    query: { from: section.value }
  }).catch(() => {});
}

async function handleAcceptComplaint(item) {
  if (!item?.id) return;
  complaintLoading.value = true;
  setStatus("");
  try {
    await acceptComplaint(token.value, item.id);
    setStatus("投诉已受理", "success");
    await loadComplaints();
  } catch (error) {
    setStatus(error.message || "投诉受理失败", "error");
  } finally {
    complaintLoading.value = false;
  }
}

async function handleSamplingSearch() {
  samplingPage.value = 1;
  await loadSamplingTasks();
}

async function changeSamplingPage(nextPage) {
  samplingPage.value = nextPage;
  await loadSamplingTasks();
}

async function handleCreateSamplingTask() {
  if (!samplingForm.enterpriseId) {
    setStatus("请选择企业后再创建抽检任务", "error");
    return;
  }
  if (!samplingForm.productId) {
    setStatus("请选择产品后再创建抽检任务", "error");
    return;
  }
  if (!samplingForm.taskTitle.trim()) {
    setStatus("请填写抽检任务标题", "error");
    return;
  }
  if (!samplingForm.deadline) {
    setStatus("请填写截止时间", "error");
    return;
  }
  samplingLoading.value = true;
  setStatus("");
  try {
    await createSamplingTask(token.value, {
      enterpriseId: samplingForm.enterpriseId,
      productId: samplingForm.productId,
      taskTitle: samplingForm.taskTitle,
      taskDesc: samplingForm.taskDesc,
      priority: samplingForm.priority,
      deadline: normalizeDeadline(samplingForm.deadline)
    });
    setStatus("抽检任务已创建", "success");
    samplingForm.taskTitle = "";
    samplingForm.taskDesc = "";
    samplingForm.priority = "MEDIUM";
    samplingForm.deadline = "";
    await loadSamplingTasks();
  } catch (error) {
    setStatus(error.message || "创建抽检任务失败", "error");
  } finally {
    samplingLoading.value = false;
  }
}

async function ensureEnforcers(regionId) {
  if (!regionId || enforcerMap[regionId]) {
    return;
  }
  try {
    const data = await fetchEligibleRegulators(token.value, regionId);
    enforcerMap[regionId] = Array.isArray(data) ? data : [];
  } catch {
    enforcerMap[regionId] = [];
  }
}

function getEnforcers(regionId) {
  if (!regionId) return [];
  return enforcerMap[regionId] || [];
}

function isTaskDeadlineExceeded(deadline) {
  if (!deadline) return false;
  const deadlineMs = new Date(deadline).getTime();
  if (Number.isNaN(deadlineMs)) return false;
  return deadlineMs <= Date.now();
}

function isSamplingTaskAssignable(task) {
  return ["CREATED", "ASSIGNED"].includes(task.status);
}

function isSamplingTaskClosable(task) {
  return task.status === "COMPLETED";
}

async function handleCloseSamplingTask(task) {
  if (!task?.id) return;
  samplingTaskLoading.value = true;
  setStatus("", "info");
  try {
    await closeSamplingTask(token.value, task.id);
    setStatus("抽检任务已归档", "success");
    await loadSamplingTasks();
  } catch (error) {
    setStatus(error.message || "归档抽检任务失败", "error");
  } finally {
    samplingTaskLoading.value = false;
  }
}

async function handleAssignSamplingTask(task) {
  if (isTaskDeadlineExceeded(task?.deadline)) {
    setStatus("任务已超期，无法派发", "error");
    return;
  }
  const regulatorId = samplingAssignments[task.id];
  if (!regulatorId) {
    setStatus("请选择执法人员后再派发", "error");
    return;
  }
  samplingTaskLoading.value = true;
  setStatus("");
  try {
    await assignSamplingTask(token.value, task.id, { regulatorId });
    setStatus("抽检任务已派发", "success");
    await loadSamplingTasks();
  } catch (error) {
    setStatus(error.message || "抽检任务派发失败", "error");
  } finally {
    samplingTaskLoading.value = false;
  }
}

async function handlePublishSamplingResult(task) {
  if (!task?.samplingResultId) {
    setStatus("抽检结果未生成，无法公示", "error");
    return;
  }
  samplingTaskLoading.value = true;
  setStatus("");
  try {
    await publishSamplingResult(token.value, task.samplingResultId);
    setStatus("抽检结果已公示", "success");
    await loadSamplingTasks();
  } catch (error) {
    setStatus(error.message || "抽检结果公示失败", "error");
  } finally {
    samplingTaskLoading.value = false;
  }
}

async function handleOfflineSamplingResult(task) {
  if (!task?.samplingResultId) {
    setStatus("抽检结果未生成，无法下线", "error");
    return;
  }
  samplingTaskLoading.value = true;
  setStatus("");
  try {
    await offlineSamplingResult(token.value, task.samplingResultId);
    setStatus("抽检结果已下线", "success");
    await loadSamplingTasks();
  } catch (error) {
    setStatus(error.message || "抽检结果下线失败", "error");
  } finally {
    samplingTaskLoading.value = false;
  }
}

async function openSamplingTaskDetail(task) {
  if (!task) return;
  samplingDetailTask.value = task;
  samplingDetailEnterprise.value = null;
  samplingDetailRegionName.value = "-";
  if (!task.enterpriseId) return;

  samplingDetailLoading.value = true;
  try {
    const enterprise = await fetchEnterpriseDetail(token.value, task.enterpriseId);
    samplingDetailEnterprise.value = enterprise || null;
    if (enterprise?.regionId) {
      const path = await fetchRegionPath(token.value, enterprise.regionId).catch(() => []);
      samplingDetailRegionName.value = Array.isArray(path) && path.length
        ? path.map((item) => item.name).join("/")
        : "-";
    }
  } catch (error) {
    setStatus(error.message || "加载企业信息失败", "error");
  } finally {
    samplingDetailLoading.value = false;
  }
}

function closeSamplingTaskDetail() {
  samplingDetailTask.value = null;
  samplingDetailEnterprise.value = null;
  samplingDetailRegionName.value = "-";
  samplingDetailLoading.value = false;
}

async function openInspectionDetail(record) {
  if (!record?.id) return;
  inspectionLoading.value = true;
  try {
    inspectionDetail.value = await fetchInspectionRecordDetail(token.value, record.id);
  } catch (error) {
    setStatus(error.message || "加载检查记录失败", "error");
  } finally {
    inspectionLoading.value = false;
  }
}

function closeInspectionDetail() {
  inspectionDetail.value = null;
}

function normalizeDeadline(value) {
  if (!value) return null;
  return value.includes(":") && value.length === 16 ? `${value}:00` : value;
}

async function handleLogout() {
  await performLogout();
  router.replace({ name: "login" }).catch(() => {});
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

async function loadSection(sectionValue) {
  const normalized = normalizeSection(sectionValue);

  if (normalized === "sampling") {
    await loadSampling();
    return;
  }
  if (normalized === "inspections") {
    await loadInspections();
    return;
  }
  if (normalized === "rectification") {
    await loadRectifications();
    return;
  }
  if (normalized === "complaints") {
    await loadComplaints();
    return;
  }
  if (normalized === "warnings") {
    await loadWarnings();
    return;
  }
  if (normalized === "stats" || normalized === "bulletins") {
    return;
  }
}

onMounted(() => {
  loadSection(section.value);
});

watch(
  () => route.name,
  async () => {
    const normalized = resolveCurrentSection();
    if (section.value !== normalized) {
      section.value = normalized;
    }
    await loadSection(normalized);
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
.stats-dashboard {
  display: grid;
  gap: 22px;
}

.regulator-shell {
  grid-template-columns: 260px 1fr;
}

.regulator-shell .sub-nav {
  display: none;
}

.regulator-shell .hero-panel {
  padding: 48px 80px 32px;
}

.regulator-shell .hero-content h1 {
  font-size: 34px;
}

.regulator-shell .hero-content p {
  max-width: 720px;
  font-size: 16px;
}

.regulator-shell .hero-highlights {
  grid-template-columns: repeat(3, minmax(0, 1fr));
}

.regulator-shell .form-panel {
  display: block;
  width: 100%;
  padding: 24px 36px 60px;
  align-items: stretch;
  justify-content: flex-start;
}

.regulator-shell .card {
  width: 100%;
  padding: 28px;
}

.sub-nav {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-bottom: 16px;
}

.sub-nav button {
  padding: 8px 14px;
  border-radius: 999px;
  border: 1px solid var(--stroke);
  background: var(--card-strong);
  color: var(--ink);
  cursor: pointer;
}

.sub-nav button.active {
  background: var(--nav);
  border-color: transparent;
  font-weight: 600;
  color: #fff;
}

.placeholder {
  border-radius: 12px;
  border: 1px dashed var(--stroke);
  padding: 16px;
  color: var(--muted);
  font-size: 13px;
}

.list-row {
  --row-columns: 1.6fr 0.9fr 0.9fr 1fr 1.2fr 0.8fr;
}

.primary-text {
  font-weight: 600;
}

.secondary-text {
  font-size: 12px;
  color: var(--muted);
}

.dispatch-grid {
  display: grid;
  grid-template-columns: 1fr;
  gap: 18px;
  align-items: start;
}

.dispatch-form {
  border: 1px solid var(--stroke);
  border-radius: 14px;
  padding: 18px 20px;
  background: #eef6fb;
}

.dispatch-list {
  border-radius: 14px;
  border: 1px solid var(--stroke);
  padding: 18px 20px;
  background: var(--card-strong);
}

.dispatch-form-grid {
  display: grid;
  gap: 14px;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  align-items: end;
}

.dispatch-form-grid label {
  min-width: 0;
  display: grid;
  gap: 6px;
}

.dispatch-form-grid .span-all {
  grid-column: 1 / -1;
}

.dispatch-form-grid .span-all textarea {
  min-height: 96px;
  resize: vertical;
}

.dispatch-submit {
  justify-self: end;
  min-width: 140px;
}

.section-subtitle {
  font-weight: 600;
  margin-bottom: 10px;
}

.task-table {
  margin-top: 8px;
}

.sampling-header,
.sampling-row {
  grid-template-columns:
    minmax(180px, 1.5fr)
    minmax(140px, 1.2fr)
    minmax(160px, 1.4fr)
    minmax(72px, 0.7fr)
    minmax(88px, 0.8fr)
    minmax(96px, 0.9fr)
    minmax(140px, 1fr)
    minmax(200px, 1.6fr);
}

.sampling-row > span,
.sampling-row > div {
  min-width: 0;
}

.sampling-row > span {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.sampling-row > span:first-child {
  /* Task numbers are long and unbroken; allow wrapping to avoid overlap. */
  white-space: normal;
  word-break: break-all;
  line-height: 1.35;
}

.complaint-header,
.complaint-row {
  --row-columns: 1.4fr 1.2fr 0.9fr 0.9fr 0.9fr 1.1fr 1.2fr;
}

.warning-header,
.warning-row {
  --row-columns: minmax(180px, 1.4fr) minmax(200px, 1.8fr) 0.7fr 0.9fr 0.8fr 1fr 1.2fr;
}

.warning-quick-tools {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 12px;
  flex-wrap: wrap;
}

.warning-quick-toggle {
  height: 34px;
  padding: 0 14px;
}

.warning-quick-toggle.active {
  color: #1f4f89;
  border-color: #c7defc;
  background: #eef6ff;
  font-weight: 600;
}

.warning-quick-tip {
  font-size: 12px;
  color: var(--muted);
}

.warning-title-cell {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.warning-status-chip {
  display: inline-flex;
  justify-content: center;
  align-items: center;
  min-height: 30px;
  padding: 0 10px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 600;
  border: 1px solid transparent;
}

.warning-status-chip--open {
  color: #9b3a0a;
  background: #fff4eb;
  border-color: #f8d5bf;
}

.warning-status-chip--acked {
  color: #1f4f89;
  background: #eef6ff;
  border-color: #c7defc;
}

.warning-status-chip--processing {
  color: #245d62;
  background: #ecfbfb;
  border-color: #c4ebec;
}

.warning-status-chip--resolved {
  color: #1f6b4d;
  background: #ebf9f1;
  border-color: #c6e9d6;
}

.warning-status-chip--closed {
  color: #5a6b7f;
  background: #f0f4f8;
  border-color: #d8e1ea;
}

.warning-status-chip--unknown {
  color: var(--muted);
  background: #f5f8fb;
  border-color: var(--stroke);
}

.inspection-header,
.inspection-row {
  --row-columns: 1.6fr 1fr 0.8fr 1.2fr 0.8fr;
}

.rectification-header,
.rectification-row {
  --row-columns: 1fr 1.5fr 0.8fr 1.1fr 1.3fr 1fr 0.9fr;
}

.rectification-text {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
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

.task-detail-modal {
  width: min(760px, 94vw);
}

.warning-detail-modal {
  width: min(920px, 96vw);
}

.warning-summary-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 10px;
}

.warning-summary-item {
  border: 1px solid var(--stroke);
  border-radius: 12px;
  background: var(--card-strong);
  padding: 10px;
}

.warning-summary-item span {
  display: block;
  font-size: 12px;
  color: var(--muted);
  margin-bottom: 6px;
}

.warning-summary-item strong {
  font-size: 14px;
  color: var(--ink);
}

.warning-payload {
  margin: 0;
  max-height: 180px;
  overflow: auto;
  border: 1px solid var(--stroke);
  border-radius: 12px;
  background: var(--card-strong);
  padding: 10px;
  font-size: 12px;
  line-height: 1.5;
  color: var(--muted);
}

.warning-detail-actions {
  justify-content: flex-end;
  gap: 10px;
}

.warning-timeline-list {
  display: grid;
  gap: 8px;
  max-height: 280px;
  overflow: auto;
  padding: 10px;
  border: 1px solid var(--stroke);
  border-radius: 12px;
  background: var(--card-strong);
}

.warning-timeline-item {
  display: grid;
  grid-template-columns: 14px 1fr;
  gap: 10px;
  align-items: start;
  border-radius: 8px;
  padding: 4px 6px;
}

.warning-timeline-dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  margin-top: 5px;
  border: 2px solid #c9d6e5;
  background: #fff;
}

.warning-timeline-dot.done {
  border-color: rgba(31, 107, 77, 0.7);
  background: rgba(31, 107, 77, 0.2);
}

.warning-timeline-dot.active {
  border-color: var(--primary);
  background: var(--primary);
}

.warning-timeline-header {
  display: flex;
  justify-content: space-between;
  gap: 12px;
}

.warning-timeline-name {
  font-weight: 600;
  font-size: 13px;
  color: var(--ink);
}

.warning-timeline-time {
  font-size: 12px;
  color: var(--muted);
  white-space: nowrap;
}

.warning-timeline-meta {
  margin-top: 4px;
  font-size: 12px;
  color: var(--muted);
}

.warning-timeline-desc {
  margin-top: 6px;
  font-size: 13px;
  color: var(--ink);
  line-height: 1.5;
  word-break: break-word;
}

.task-detail-header {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 12px;
}

.task-chip {
  display: inline-flex;
  align-items: center;
  padding: 4px 10px;
  border-radius: 999px;
  border: 1px solid var(--stroke);
  background: var(--card-strong);
  font-size: 12px;
  color: var(--muted);
}

.task-chip--status {
  border-color: rgba(27, 99, 177, 0.25);
  background: rgba(27, 99, 177, 0.1);
  color: var(--nav);
}

.task-chip--priority {
  border-color: rgba(0, 132, 91, 0.2);
  background: rgba(0, 132, 91, 0.1);
  color: #0f6c53;
}

.task-detail-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}

.task-detail-section {
  border: 1px solid var(--stroke);
  border-radius: 12px;
  background: var(--card-strong);
  padding: 12px;
}

.task-detail-section-title {
  font-size: 13px;
  font-weight: 700;
  margin-bottom: 8px;
}

.task-detail-loading {
  font-size: 13px;
  color: var(--muted);
}

.task-detail-fields {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}

.task-detail-field span {
  display: block;
  font-size: 12px;
  color: var(--muted);
  margin-bottom: 4px;
}

.task-detail-field strong {
  display: block;
  font-size: 14px;
  color: var(--ink);
  line-height: 1.45;
  word-break: break-word;
}

.task-detail-field--full {
  grid-column: 1 / -1;
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

.modal-empty {
  font-size: 12px;
  color: var(--muted);
}

@media (max-width: 900px) {
  .dispatch-grid {
    grid-template-columns: 1fr;
  }

  .dispatch-form-grid {
    grid-template-columns: 1fr;
  }

  .dispatch-submit {
    justify-self: stretch;
    width: 100%;
  }

  .sampling-header,
  .sampling-row {
    grid-template-columns: 1fr;
  }

  .task-detail-grid {
    grid-template-columns: 1fr;
  }

  .task-detail-fields {
    grid-template-columns: 1fr;
  }

  .warning-summary-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

}

@media (max-width: 1200px) {
  .dispatch-form-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 640px) {
  .warning-summary-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 1024px) {
  .regulator-shell .hero-panel {
    padding: 36px 40px 24px;
  }

  .regulator-shell .form-panel {
    padding: 10px 40px 60px;
  }

  .regulator-shell .hero-highlights {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 960px) {
  .regulator-shell {
    grid-template-columns: 1fr;
  }
}
</style>

