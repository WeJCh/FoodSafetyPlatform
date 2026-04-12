export const enterpriseProfileSeed = {
  id: "EP-2023-00912",
  enterpriseName: "华源食品制造有限公司",
  licenseNo: "SC10431011200458",
  approvalStatus: "APPROVED",
  approvalComment: "备案材料齐全，信息核验通过。",
  approvedTime: "2023-10-24 09:18:00",
  regionText: "上海市 / 闵行区 / 浦江镇",
  addressDetail: "上海市闵行区联航路 188 弄 18 号",
  principal: "陈启明",
  principalPhone: "13800138000",
  contactName: "张若琳",
  contactPhone: "13910001000",
  email: "compliance@huayuanfoods.cn",
  archiveStatus: "已核准",
  nextReviewAt: "2024-10-24",
  attachments: [
    { id: "att-1", name: "食品生产许可证.pdf", type: "许可证" },
    { id: "att-2", name: "营业执照扫描件.pdf", type: "营业执照" }
  ]
};

export const enterpriseProductsSeed = [
  {
    id: "prod-olive-oil",
    productName: "有机特级初榨橄榄油",
    category: "调味品 / 食用油",
    specification: "500ml / 瓶",
    shelfLife: "18个月",
    status: "ACTIVE",
    updateTime: "2026-04-09 14:18:00",
    remark: "冷榨工艺，符合 GB/T 23347 质量标准。",
    approvalNo: "QS-OLIVE-2024-01"
  },
  {
    id: "prod-yogurt",
    productName: "高钙低糖常温酸奶",
    category: "乳制品",
    specification: "250ml / 盒",
    shelfLife: "6个月",
    status: "ACTIVE",
    updateTime: "2026-04-07 10:22:00",
    remark: "升级新版外包装，营养成分标签已复核。",
    approvalNo: "QS-YOGURT-2024-08"
  },
  {
    id: "prod-snack",
    productName: "烘焙谷物能量棒",
    category: "休闲食品",
    specification: "40g / 支",
    shelfLife: "12个月",
    status: "INACTIVE",
    updateTime: "2026-04-02 16:40:00",
    remark: "暂停售卖，等待新批次上线。",
    approvalNo: "QS-SNACK-2024-16"
  }
];

export const enterpriseInspectionsSeed = [
  {
    id: "insp-2026-001",
    title: "生产车间例行抽检",
    inspectionDate: "2026-04-05",
    result: "PASS",
    inspector: "李建国",
    problemDesc: "现场生产环境与台账记录一致。",
    updateTime: "2026-04-05 11:20:00",
    ledgerNote: "检查详情已录入账本。",
    rectificationId: ""
  },
  {
    id: "insp-2026-002",
    title: "包装材料专项核查",
    inspectionDate: "2026-03-29",
    result: "FAIL",
    inspector: "王晓云",
    problemDesc: "原料仓储温控记录存在缺口，需限期整改。",
    updateTime: "2026-03-29 15:12:00",
    ledgerNote: "判定结果为待整改。",
    rectificationId: "rect-2026-003"
  },
  {
    id: "insp-2026-003",
    title: "企业备案真实性核查",
    inspectionDate: "2026-03-15",
    result: "PASS",
    inspector: "周明浩",
    problemDesc: "备案附件完整，现场核验通过。",
    updateTime: "2026-03-15 17:48:00",
    ledgerNote: "备案核验流程已归档。",
    rectificationId: ""
  }
];

export const inspectionItemsSeed = {
  "insp-2026-001": [
    { itemName: "生产环境", itemResult: "PASS", problemDesc: "符合清洁规范" },
    { itemName: "追溯台账", itemResult: "PASS", problemDesc: "记录完整" }
  ],
  "insp-2026-002": [
    { itemName: "温控记录", itemResult: "FAIL", problemDesc: "3 月 27 日夜间缺少记录" },
    { itemName: "包材批次", itemResult: "PASS", problemDesc: "抽样批次一致" }
  ],
  "insp-2026-003": [
    { itemName: "备案附件", itemResult: "PASS", problemDesc: "原件与系统一致" }
  ]
};

export const enterpriseRectificationsSeed = [
  {
    id: "rect-2026-003",
    taskNo: "RT-2026-003",
    title: "原料仓储与温控系统复核",
    rectificationDesc: "补齐原料仓储温控记录，完善夜间交接制度。",
    status: "ONGOING",
    dueDate: "2026-04-18",
    updateTime: "2026-04-09 09:32:00",
    riskLevel: "中风险",
    focusArea: "原料仓储区",
    inspectionId: "insp-2026-002",
    progressSummary: "等待企业提交整改材料。"
  },
  {
    id: "rect-2026-001",
    taskNo: "RT-2026-001",
    title: "标签标识复核",
    rectificationDesc: "修正外包装营养成分标签的批次格式。",
    status: "SUBMITTED",
    dueDate: "2026-04-12",
    updateTime: "2026-04-08 18:20:00",
    riskLevel: "低风险",
    focusArea: "包装线",
    inspectionId: "",
    progressSummary: "已提交整改说明，等待复核。"
  },
  {
    id: "rect-2026-002",
    taskNo: "RT-2026-002",
    title: "冷链周转区标识优化",
    rectificationDesc: "补充冷链周转区风险标识并重做巡检记录。",
    status: "REWORK",
    dueDate: "2026-04-11",
    updateTime: "2026-04-07 12:08:00",
    riskLevel: "高风险",
    focusArea: "冷链周转区",
    inspectionId: "",
    progressSummary: "监管方已打回，需补充现场照片。"
  }
];

export const rectificationActionsSeed = {
  "rect-2026-003": [
    { id: "ra-1", actionType: "TASK_CREATED", actorName: "王晓云", actionTime: "2026-03-29 15:20:00", comment: "生成整改任务并发送给企业。" },
    { id: "ra-2", actionType: "REVIEW_PENDING", actorName: "系统", actionTime: "2026-04-09 09:32:00", comment: "等待企业提交整改说明。" }
  ],
  "rect-2026-001": [
    { id: "ra-3", actionType: "SUBMIT", actorName: "陈启明", actionTime: "2026-04-08 18:20:00", comment: "已完成标签修订并提交复核。" }
  ],
  "rect-2026-002": [
    { id: "ra-4", actionType: "SUBMIT", actorName: "陈启明", actionTime: "2026-04-06 19:08:00", comment: "首次提交整改说明。" },
    { id: "ra-5", actionType: "REVIEW_REWORK", actorName: "王晓云", actionTime: "2026-04-07 12:08:00", comment: "缺少冷链区现场补拍照片，请重新提交。" }
  ]
};

export function findSeedProduct(id) {
  return enterpriseProductsSeed.find((item) => item.id === id) || null;
}

export function findSeedInspection(id) {
  return enterpriseInspectionsSeed.find((item) => item.id === id) || null;
}

export function findSeedRectification(id) {
  return enterpriseRectificationsSeed.find((item) => item.id === id) || null;
}
