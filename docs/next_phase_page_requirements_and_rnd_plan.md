# 下一阶段页面需求定义与研发安排

> 状态：本文件为早期按页面推进的研发安排，部分内容已经与当前研发流程不匹配。后续新增研发以 `docs/module_layering_and_future_rnd_structure.md` 为准：平台按 12 项服务 + Dashboard + 基础管理共 14 个一级模块包推进；现有 Dashboard、物料采购链路和基础管理短期保持原状，不在本轮新分层中迁移。伙食采购专项以 `docs/food_procurement_rnd_plan.md` 为准。

## 1. 背景与目标

当前 Vue 正式前端已完成首页、伙食采购入口、物料采购入口、服务入口的基础页面。下一阶段目标是把船供海事服务平台从“入口展示与物料需求导入”推进到“标准库、供货商、报价比价、下单履约、运营看板”的完整业务链路。

一期仍以船代为中心，不做普通电商商城形态。页面应服务于一张船供订单从需求导入、标准库匹配、询价报价、比价、下单、送船、结算前状态跟踪的闭环。

## 2. 页面范围

本阶段定义并安排以下页面：

- 登录
- 注册
- 企业账号管理/成员管理
- IMPA 标准库
- 供货商信息
- 供货商产品
- 比价界面
- 报价界面
- 下单界面
- Dashboard

## 3. 总体信息架构

建议正式前端从当前入口页扩展为以下路由组：

| 模块 | 建议路由 | 页面 |
| --- | --- | --- |
| 认证 | `/login` | 登录 |
| 认证 | `/register` | 注册企业主账号/企业管理员 |
| 账号 | `/company/members` | 企业账号管理/成员管理 |
| 工作台 | `/dashboard` | Dashboard |
| 物料采购 | `/procurement/materials` | 物料采购入口，承接上传匹配、保存、报价、比价临时入口 |
| 物料采购 | `/inquiries` | 询价管理，当前先按静态/模拟列表展示 3 条数据 |
| 物料采购 | `/quotes` | 报价管理，当前先按静态/模拟列表展示 3 条数据 |
| 物料采购 | `/procurement/requests` 或 `/comparisons` | 比价管理，当前先按静态/模拟列表展示 3 条数据 |
| 物料采购 | `/procurement/requests/:requestId/compare` | 比价明细界面，不作为比价列表 |
| 物料采购 | `/orders` | 采购管理，当前先按静态/模拟列表展示 3 条数据 |
| 伙食采购管理 | `/procurement/food` | 伙食采购入口，已完成基础版 |
| 伙食采购管理 | `/food/inquiries` | 询价管理，当前先按静态/模拟列表展示 3 条数据 |
| 伙食采购管理 | `/food/quotes` | 报价管理，当前先按静态/模拟列表展示 3 条数据 |
| 伙食采购管理 | `/food/comparisons` | 比价管理，当前先按静态/模拟列表展示 3 条数据 |
| 伙食采购管理 | `/food/orders` | 采购管理，当前先按静态/模拟列表展示 3 条数据 |
| 采购 | `/services` | 服务入口，已完成基础版 |
| 标准库 | `/standard-library/impa` | IMPA 标准库 |
| 供货商 | `/suppliers` | 供货商信息 |
| 供货商 | `/suppliers/:supplierId/products` | 供货商产品 |
| 交易 | `/quotes`、`/quotes/:quoteId` | 报价界面，供货商报价和采购方查看 |
| 交易 | `/procurement/requests/:requestId/compare` | 比价界面 |
| 交易 | `/orders/new` | 下单界面 |
| 交易 | `/orders/:orderId` | 订单详情，后续承接 |

## 4. 统一设计原则

### 4.1 视觉方向

- 默认采用主页浅色系 + 蓝色主调，以浅色专业采购工作台为主，蓝色作为主强调色；不再使用青色作为主要状态色。
- 保留港口运营、靠泊窗口、送船状态、供应商响应等船供行业信号。
- 避免普通电商卡片堆叠，不做商品商城式首页。
- 避免传统 ERP 灰旧表格堆叠，表格需要密但清楚。
- 首页可保留日夜模式，业务工作台优先浅色。

### 4.2 页面结构

- 业务页面统一使用顶部导航 + 默认收起的左侧模块菜单 + 内容工作区。
- 左侧模块菜单默认收起为窄栏或图标栏，点击后以抽屉方式展开完整菜单；展开、收起需有轻量动效，并按角色过滤菜单。
- 复杂页面使用左侧筛选/列表 + 右侧详情/操作区，避免弹窗承载主要流程。
- 表格类页面必须保留筛选、状态、批量操作和详情入口。
- 关键业务状态必须可见：待匹配、待询价、待报价、待比价、待下单、待送船、已送船、已完成、异常。
- 首页和现有物料需求入口 Vue 页面直接复用，不纳入本轮后续页面重做；后续页面只需保证跳转、权限和状态衔接。
- 原“物料需求入口”正式命名为“物料采购入口”；原“需求单管理”在当前菜单文案中也统一改为“物料采购入口”。该页面可由 UI 在现有结构上继续做高级化视觉处理，但不得推翻已确认的业务结构和内容。
- 物料采购入口保存按钮当前按临时口径处理：点击后提示“保存成功”，停留当前界面，不跳需求单管理；真实保存/更新接口和后续需求单闭环仍按后端契约逐步补齐。
- 物料采购入口保存区域左侧新增“报”和“比”两个按钮：“报”点击提示“报价成功”后跳转询价管理；“比”点击直接进入比价明细界面，临时可跳 `/procurement/requests/RFQ-240604/compare`。
- 后续新页面必须对齐现有物料需求入口的浅色专业采购工作台风格：白色主背景、浅蓝灰页面底、蓝色主动作、细边框、轻阴影、紧凑表格和清晰工具栏。
- 每个业务界面都移除顶部独立介绍区，不展示模块小字、页面大标题和说明副标题；顶部导航下方直接进入实际业务内容、筛选工具栏、指标摘要或数据表格。
- 工作台采用满高布局，页面底部的主代码块、表格块、矩阵块或工作面板必须自适应补满剩余高度，不能在下方留下大片空白。
- 列表页不得常驻展示无意义固定船舶、港口、ETA、ETD；如果这些字段用于查询，必须按实际列表列设计为输入框、下拉框或日期时间范围筛选。
- 禁止在列表页继续展示 `M/V OCEAN EAST`、`Zhoushan`、`2026-06-04 14:30`、`2026-06-05 06:00` 这类演示值。
- 页面空间要有取舍：高频操作常驻，详情进抽屉，短确认进弹窗，大量编辑进放大工作区，低频辅助信息进折叠区或隐藏块，不把所有内容堆到主页面。

### 4.3 交互原则

- 主要动作放在页面右上角或当前工作区标题右侧。
- 页面可见操作按钮默认用图标代替文字，尤其是工具栏、表格行操作、抽屉头部、卡片动作和弹窗底部动作；低频动作进入“更多”菜单。
- 图标按钮必须有 `aria-label` 或 tooltip 语义，tooltip 文案必须来自国际化文件。
- 危险动作二次确认，普通筛选和查看动作不弹确认。
- 数据加载使用骨架屏、局部 loading 或工作区遮罩，不使用大面积空白。
- 保存、提交、批量处理和导入时需有遮罩或禁用态，防止重复点击。
- 大量表单、大型表格和复杂明细工作区必须提供放大/收起能力。
- 按钮点击需有 hover、focus、active、loading、disabled 反馈；Tab 切换需有 150 到 250ms 轻量过渡，不允许直接闪屏跳变。
- 每个界面在研发前必须列出按钮清单，明确动作语义、对应图标、权限点、可见条件、禁用原因、loading 态和中英文翻译 key。
- 弹窗、抽屉、放大工作区、折叠区都必须保留当前页面上下文，关闭后不丢筛选、输入、选中和滚动位置。

### 4.4 国际化原则

- 系统整体支持中文 `zh-CN` 和英文 `en-US`，默认中文。
- 国际化翻译单独建文件维护，建议放在 `frontend/src/locales/zh-CN.ts` 和 `frontend/src/locales/en-US.ts`，后续可按模块拆分。
- 菜单、按钮、Tab、表格列、表单标签、校验提示、空态、错误态、权限态、Toast、Dialog、Tooltip 均不得写死文案。
- 新增页面、新增按钮、新增状态时必须同步补齐中英文翻译。
- IMPA 标准库已有中英文字段时，语言切换优先展示对应语言数据；缺少英文时可回退中文，但开发环境需提示缺失。

### 4.5 物料与伙食采购差异

- 物料采购依赖 IMPA/MSG 标准编码、供货商 SKU 映射和每日价格导入。供货商可先上架自己的商品，维护标准编码映射，并按日导入或更新价格。
- 物料需求在标准编码匹配、供货商 SKU 覆盖和价格有效时，可进入基于日价的预比价或快速比价；价格过期、规格不明确、数量异常或交付风险较高时，仍应发起询价并以正式报价为准。
- 伙食采购需求与物料需求的页面链路相似，但伙食时效性强、价格波动大，不允许直接进入日价比价。
- 伙食每次需求必须先创建询价，供货商提交本次有效报价后，才能进入比价和下单。
- 伙食报价有效期应跟随本次询价和靠泊窗口，不使用长期参考价替代正式报价。
- 物料和伙食共用比价策略：先按有效报价或有效日价筛出供货商排名前五，再支持“最低报价混合采买”和“集中采购单一供货商”两种方案。

## 5. 角色与权限范围

本系统至少按四类角色设计权限：船代、供货商、驳船代理、平台管理员。权限要同时控制“菜单可见、数据范围、按钮动作、状态流转”四件事。

命名边界：系统超级管理员与企业内管理员必须区分。系统超级管理员是平台级账号，例如开发期 `admin / 123456`，可管理全平台企业、注册审核、权限种子和异常；公开注册/入驻审核通过后的企业主账号自动成为本公司企业管理员，默认拥有本公司全部菜单权限，只能管理本公司角色、账号和菜单授权。若界面文案沿用“平台管理员”，接口和代码仍应使用 `COMPANY_ADMIN` 或等价标识，避免与系统超级管理员混淆。

### 5.1 角色定义

| 角色 | 定位 | 默认首页 | 数据范围 |
| --- | --- | --- | --- |
| 船代 | 采购需求发起方、比价和下单决策方 | Dashboard 或物料需求入口 | 本企业创建/参与的需求、询价、比价、订单、送船任务 |
| 供货商 | 接收询价并报价、维护 SKU 的供货方 | 报价工作台或供货商产品 | 本企业资料、SKU、收到的询价、自己的报价、相关订单 |
| 驳船代理 | 承接送船/驳船履约任务的服务方 | 送船任务工作台 | 分配给本企业的送船任务、船舶靠泊和配送状态 |
| 平台管理员 | 平台运营、数据维护、审核和异常处理 | 平台 Dashboard | 全平台企业、标准库、供应商、订单、履约和异常数据 |

### 5.2 菜单可见范围

| 页面/模块 | 船代 | 供货商 | 驳船代理 | 平台管理员 |
| --- | --- | --- | --- | --- |
| 首页 | 可见 | 可见 | 可见 | 可见 |
| Dashboard | 可见，本企业订单和待办 | 可见，报价和供货待办 | 可见，送船任务和船期待办 | 可见，全平台运营态势 |
| 物料需求入口 | 可见，可创建需求 | 不可创建，可查看被询价相关需求 | 不可见 | 可见，可协助处理 |
| 伙食采购入口 | 可见，可创建需求 | 不可创建，可查看被询价相关需求 | 不可见 | 可见 |
| 服务入口 | 可见，可发起服务需求 | 按服务商能力可见 | 可见送船/驳船相关服务 | 可见 |
| IMPA库 | 可查询 | 可查询并用于 SKU 映射 | 可查询但不维护 | 可查询和维护 |
| 供货商信息 | 可查看可询价供货商 | 仅查看/维护自身档案 | 不可见或仅合作方可见 | 全量管理 |
| 供货商产品 | 可查看可询价 SKU | 维护自身 SKU | 不可见 | 全量管理和审核 |
| 报价界面 | 查看报价结果 | 填写和提交报价 | 不可见 | 查看和协助处理 |
| 比价界面 | 可比价、定标、生成订单 | 不可见其他供应商报价 | 不可见 | 可查看和协助处理 |
| 下单界面 | 可下单、取消、提交确认 | 查看与确认自己的订单 | 查看关联送船任务 | 全量处理 |
| 送船/履约 | 查看状态和异常 | 查看供货出库/交付状态 | 更新驳船、到港、送达、签收状态 | 全量调度和异常处理 |

### 5.3 操作权限

船代可执行：

- 创建采购需求、上传清单、人工补录。
- 发起询价、选择供货商、查看报价。
- 比价、定标、生成订单、取消未确认订单。
- 查看送船状态、处理异常确认。

供货商可执行：

- 维护企业档案和供货商产品。
- 维护 SKU 与 IMPA 映射。
- 查看收到的询价。
- 填写报价、提交报价、拒绝报价、确认订单。
- 更新备货、出库等供货侧状态。

驳船代理可执行：

- 查看分配给自己的送船任务。
- 更新驳船计划、出发、到港、靠船、送达、异常状态。
- 填写驳船费用、异常说明和签收信息。
- 不能查看比价矩阵、供应商报价底价和非关联订单。

平台管理员可执行：

- 审核企业注册、启用/停用企业和账号。
- 维护 IMPA 标准库、供应商档案、供货商 SKU 映射。
- 查看全平台需求、询价、报价、订单、送船状态。
- 处理异常、重派任务、调整基础数据。

### 5.4 权限实现要求

- 后端必须做权限校验，前端隐藏按钮不能作为安全边界。
- 数据查询必须带企业隔离：船代只能看本企业数据，供货商只能看自己的报价和订单，驳船代理只能看分配给自己的送船任务。
- 平台管理员可以跨企业查看，但关键操作需要记录操作日志。
- 企业管理员只能在当前 `companyId` 下增删改查企业自定义角色、企业账号和用户菜单权限，不能跨企业。
- 成员列表默认查询启用用户，同时支持按状态筛选启用、停用或全部用户。
- 用户被停用后不得登录，登录失败提示固定为：`已经冻结，请联系公司管理员！`
- 前端路由需通过 `meta.roles` 或等价结构声明允许角色。
- 菜单、按钮、状态动作都应走统一权限判断，不在页面里零散硬编码。
- 登录后的 `GET /api/auth/me` 需要返回用户、企业、账号类型（企业主账号/子账号/平台账号）、审核状态、角色、权限点和默认首页。
- 登录后的 `GET /api/auth/menus` 必须只返回当前用户有效权限内的菜单；没有权限的菜单不返回，前端不显示。

### 5.5 权限数据模型建议

用户与企业：

- `sys_user.user_type`：`SHIP_AGENT`、`SUPPLIER`、`BARGE_AGENT`、`PLATFORM_ADMIN`
- `company.company_type`：`SHIP_AGENT`、`SUPPLIER`、`BARGE_AGENT`、`PLATFORM`
- `sys_user.company_id`：用户所属企业。
- `sys_user.status`：`PENDING`、`ACTIVE`、`DISABLED`

角色与权限：

- `sys_role.role_code`：角色编码，例如 `SHIP_AGENT_BUYER`、`SUPPLIER_QUOTER`、`BARGE_DISPATCHER`、`PLATFORM_ADMIN`
- `sys_permission.permission_code`：权限点编码，例如 `DEMAND_CREATE`、`QUOTE_SUBMIT`、`COMPARE_VIEW`、`ORDER_CREATE`、`DELIVERY_UPDATE`
- `sys_role_permission`：角色和权限点关系。
- `sys_user_role`：用户和角色关系。
- 企业内默认管理员角色建议使用 `COMPANY_ADMIN` 或按企业类型派生的管理员角色，作为企业主账号审核通过后的默认角色；该角色拥有本公司全部菜单权限。
- 企业自定义角色必须带 `company_id` 或等价租户范围，不能跨企业复用编辑。
- 保留角色权限作为模板，同时支持用户级菜单权限覆盖或补充；最终有效权限用于 `GET /api/auth/menus` 和按钮权限判断。
- 后续新增菜单或权限点时，必须同步补齐默认企业管理员角色和历史企业主账号的新权限；普通子账号不自动获得全量新菜单，除非其角色配置明确继承或企业管理员显式授权。

菜单权限：

- `sys_menu.menu_code`
- `sys_menu.route_path`
- `sys_menu.required_permission`
- `sys_menu.visible_roles`

数据权限：

- 船代按 `ship_agent_company_id` 隔离。
- 供货商按 `supplier_id/company_id` 隔离。
- 驳船代理按 `barge_agent_company_id` 或任务分配关系隔离。
- 平台管理员可跨企业访问，但关键操作必须写入 `operation_log`。

## 6. 页面需求定义

### 6.1 登录

目标用户：船代公司用户、平台运营人员、供货商用户。

核心任务：

- 使用账号密码登录平台。
- 根据角色进入对应默认页。
- 支持忘记密码入口。

主信息架构：

- 左侧：平台品牌、港口作业图或运营态势摘要。
- 右侧：登录表单。
- 表单字段：账号、密码、记住登录。
- 辅助入口：注册、忘记密码。

关键状态：

- 默认、输入中、校验失败、登录中、账号停用、密码错误。

验收标准：

- 登录页视觉与首页品牌一致。
- 表单在桌面和移动端不溢出。
- 登录失败原因清晰，不泄露敏感信息。

### 6.2 注册

目标用户：新船代客户、新供货商的企业主账号/企业管理员。

核心任务：

- 创建企业主账号/企业管理员账号。
- 提交企业入驻资料，审核通过后再进入业务系统。
- 平台管理员不通过公开注册创建，驳船代理默认由平台管理员创建或邀请。

主信息架构：

- 基础账号信息：账号、密码、验证码。
- 企业主账号说明：该账号审核通过后可管理本企业成员。
- 企业信息：企业名称、统一社会信用代码、联系人、联系电话。
- 企业类型选择：船代、供货商；驳船代理按平台创建/邀请口径处理，平台管理员不开放自注册。
- 资质上传：营业执照、供货品类、服务港口。

关键状态：

- 草稿、待提交、待审核、审核驳回、审核通过。

验收标准：

- 注册不是普通个人商城注册，必须明确创建的是企业主账号/企业管理员账号。
- 供应商注册需要采集服务港口和主营品类。
- 审核通过前不能创建子账号；审核通过后才可进入企业账号管理/成员管理。

### 6.3 企业账号管理/成员管理

目标用户：企业主账号/企业管理员、具备成员管理权限的子账号、平台管理员。

核心任务：

- 在同一个 company 下创建、邀请、禁用和管理多个子账号。
- 为子账号分配岗位角色和权限，支持采购员、采购主管、财务、只读查看、供货商报价员、供货商管理员等企业内角色。
- 保证子账号只能访问本企业数据，不能跨企业查看需求、报价、订单、费用或成员。

主信息架构：

- 顶部筛选：关键词、角色、状态、最近登录时间。
- 成员列表：姓名、账号、手机号、邮箱、岗位/角色、状态、最近登录、创建来源。
- 成员详情：基础资料、所属企业、角色权限、数据范围、操作记录。
- 角色分配区：企业内可用角色、权限摘要、变更原因。

关键状态：

- 待邀请、已邀请、正常、已禁用、已离职、密码需重置、企业审核未通过。

主要操作：

- 新增成员、邀请成员、编辑成员、分配角色、启用/禁用、重置密码、重新发送邀请、查看日志。

接口依赖：

- `GET /api/company/members`
- `GET /api/company/members/{userId}`
- `POST /api/company/members`
- `POST /api/company/members/invitations`
- `PATCH /api/company/members/{userId}`
- `PATCH /api/company/members/{userId}/status`
- `PATCH /api/company/members/{userId}/roles`
- `POST /api/company/members/{userId}/reset-password`
- `GET /api/company/roles`

验收标准：

- 企业主账号/企业管理员只能管理本企业成员。
- 子账号必须绑定同一个 `companyId`，所有业务接口按 company 做数据隔离。
- 菜单、按钮动作、数据范围和状态流转必须受企业内角色控制。
- 禁用、重置密码、角色变更必须二次确认并记录操作日志。
- 企业未审核通过时，成员管理入口不可用并说明原因。
### 6.4 IMPA 标准库

目标用户：船代采购人员、运营人员、供货商维护人员。

核心任务：

- 查询 IMPA 编码和中文品名。
- 通过一级分类筛选物料。
- 查看物料规格、单位、英文名称、标准分类。
- 后续用于需求匹配、供货商 SKU 映射和报价。

主信息架构：

- 顶部搜索：支持 IMPA 编码、中文品名、规格关键词。
- 左侧一级分类：显示分类名称和该分类物料总数。
- 右侧物料明细：两列或表格展示 `impa_code description/specification`。
- 详情抽屉：编码、中文名、英文名、规格、单位、分类、关联供货商数量。

关键字段：

- `impaCode`
- `categoryCode`
- `nameCn`
- `nameEn`
- `specification`
- `unit`
- `supplierCount`

当前接口基础：

- `GET /api/standard-library/impa/categories`
- `GET /api/standard-library/impa/items?categoryCode=&segmentCode=&limit=`

后续接口建议：

- `GET /api/standard-library/impa/items?keyword=&categoryCode=&page=&size=`
- `GET /api/standard-library/impa/items/{impaCode}`

验收标准：

- 页面不展示二级码段按钮作为主信息。
- 物料列表直接展示具体编码和品名。
- 搜索编码能快速定位到物料。

### 6.5 供货商信息

目标用户：平台运营、船代采购人员。

核心任务：

- 查看供货商档案。
- 按港口、品类、资质、响应能力筛选供货商。
- 查看供应商是否可参与当前订单询价。

主信息架构：

- 顶部指标：供货商总数、活跃供货商、本港可服务、待审核。
- 筛选区：服务港口、主营品类、资质状态、合作状态。
- 列表：供货商名称、服务港口、主营品类、SKU 数量、报价响应率、最近报价时间、状态。
- 详情区：企业资料、联系人、资质、服务港口、历史履约、产品入口。

关键状态：

- 待审核、已启用、已停用、黑名单、资料待补充。

验收标准：

- 供货商列表不是商品店铺页，而是供应能力档案页。
- 必须能从供货商跳转到供货商产品。

### 6.6 店铺管理/供货商产品

目标用户：供货商维护人员、企业管理员、平台运营；船代采购人员仅在询价/比价场景查看可询价 SKU，不进入维护态。

核心任务：

- 维护店铺 LOGO、店铺简介和基础资料。
- 通过导入方式把 SKU 导入店铺。
- 对 SKU 做增删改查、检索、筛选和异常处理。
- 将供货商 SKU 映射到 IMPA 标准库。
- 维护价格、单位、库存/供货能力、服务港口。
- 维护 SKU 的多属性、图片和报价相关参数，支撑后续询价微调和比价决策。

主信息架构：

- 店铺资料区：LOGO、店铺名称、店铺简介、主营品类、配送区域/服务港口、联系人、基础信息保存。
- SKU 管理工具栏：导入、新增、删除/停用、编辑、导出、检索、筛选、刷新、异常待处理。
- 筛选：商品类型、IMPA 一级分类/伙食品类、平台编码、商品名、规格、品牌、编码状态、异常状态、库存状态、配送区域、币种、上下架状态。
- 列表：缩略图、商品类型、分类、平台编码、供应商 SKU、供应商品名、规格摘要、库存、备货时长、配送区域、单价、币种、品牌、月销量、编码状态、上下架状态。
- 详情：SKU 原始信息、标准库映射、多个规格项、图片、属性组、替代品、历史报价、价格更新记录、导入来源。
- 图片：列表展示缩略图，点击后打开图片预览；无图时显示占位，不挤压表格行高。
- 属性：类似说明、品名、规格、包装、装箱数、尺寸、毛重、净重、条码、库存等均作为 SKU 属性维护，不建议全部平铺为 SKU 主表字段。

关键状态：

- 页面状态：空店铺、导入中、导入预览、导入成功、异常待处理。
- SKU 状态：未映射、已映射、待确认、已停用、价格待更新。
- 编码状态：已编码、候选、异常、暂不处理。

导入和异常规则：

- 导入时平台编码先核对 IMPA 库；命中后标注编码成功，并带出 IMPA 一级分类和标准物料名称。
- 平台编码为空、格式错误或未命中时，规格去标点、空格、大小写差异后，与 IMPA 库规格去标点结果一一比对。
- 规格比对能找到候选时进入候选确认；找不到或冲突时标记异常，等待人工处理。
- 异常处理支持人工补平台编码、选择候选、标记暂不处理；暂不处理 SKU 可保留但不参与自动比价推荐。

验收标准：

- SKU 必须和 IMPA 标准物料关联。
- 不能只做普通商品列表，需要展示标准库映射状态。
- SKU 属性必须支持一对多；询价微调时可选择需要的属性组合，不同属性组合可对应不同报价。
- 物料图片必须能以缩略图展示并点击放大预览。
- 店铺 LOGO、简介和基础信息保存后刷新可回显。
- 导入预览必须能区分编码成功、候选确认和异常待处理。
- 用 `wangbo` 验收时应能进入“店铺管理”，完成店铺资料保存、SKU 检索筛选、导入预览和异常处理主流程。

真实文件参考：

- 伙食需求单 `tmp/provision_analysis/provision.xlsx` 参考字段：`CODE`、`DESCRIPTION（品名）`、`REMARKS（备注）`、`Specification（规格）`、`QTTY（数量）`、`UNIT（单位）`、`Price（美元单价）`、`总价(TOTAL)`。
- 物料报价单 `tmp/provision_analysis/物料报价单.xlsx` 参考字段：`Pictures`、`Name of Commodity & Specification`、`Item No.`、`FOB WAREHOUSE(RMB)`、`Packing`、`Pcs/Inner box`、`Pcs/Ctns`、`L/cm`、`W/cm`、`H/cm`、`N/m³`、`GW/kgs`、`NW/kgs`、`Barcode`、`STOCK`。
- 上述物料字段中，`Name of Commodity & Specification`、包装、装箱、尺寸、重量、条码、库存等应优先作为 SKU 属性/库存/价格扩展数据处理；`Pictures` 应进入 SKU 图片数据。

### 6.7 报价界面

目标用户：供货商、平台运营。

核心任务：

- 供货商对询价清单逐项报价。
- 支持缺货、替代品、最小起订量、交付时间说明。
- 提交整单报价。

主信息架构：

- 顶部订单上下文：船名、港口、靠泊时间、报价截止时间。
- 左侧/主区报价明细：需求物料、请求规格、标准编码、数量、单位、报价单价、税率、交付说明、替代风险。
- 右侧报价摘要：合计金额、可供项、缺货项、替代项、预计送达。
- 操作：保存草稿、提交报价、导出报价单。

关键状态：

- 待报价、草稿、已提交、已过期、已撤回。
- 明细状态：可供、缺货、替代、需澄清。

验收标准：

- 供货商可以逐项填写报价。
- 船代侧能清楚看到报价是否完整。

### 6.8 比价界面

目标用户：船代采购人员、平台运营。

核心任务：

- 对多个供货商报价进行横向比较。
- 标记最优项、替代风险、缺货风险和履约风险。
- 选择推荐供应商或拆单组合。
- 支持物料和伙食共用的比价策略：最低报价混合采买、集中采购单一供货商。

主信息架构：

- 顶部订单上下文：船名、港口、计划靠泊、报价截止。
- 左侧物料明细列表：原始需求、匹配结果、数量、单位。
- 横向报价矩阵：供货商 A/B/C 的单价、总价、交付时间、替代说明。
- 属性选择：物料比价时必须展示本次询价选定的 SKU 属性，如规格、材质、包装、尺寸、重量、条码、库存和图片。
- 供货商排名：默认只纳入有效报价或有效日价的前五家供货商，支持查看排名依据。
- 策略切换：最低报价混合采买、集中采购单一供货商。
- 右侧决策摘要：最低价、集中采总价、混采总价、推荐方案、风险项、缺货项、预计合计。
- 操作：选择供应商、拆单、生成订单、退回询价、切换比价策略。

关键状态：

- 待报价、可比价、部分报价、已选供应商、已生成订单。

验收标准：

- 不只比较价格，必须同时比较交付时间、替代风险、供货完整度。
- 物料比价必须能看清楚价格对应的 SKU 属性，不能只看一个品名和价格。
- 有图片的物料必须在比价矩阵或详情抽屉中展示缩略图，并可点击预览大图。
- 能从物料需求入口的“比价”按钮进入。
- 伙食需求不能直接进入比价，必须先完成询价和报价回收。
- 比价页必须能展示前五家供货商排名，并在最低报价混采和集中采购之间切换。
- 最低报价混采按明细选择各项最低有效报价，可形成多供货商拆分订单。
- 集中采购按单一供货商整体报价、可供完整度、交付能力和风险选择一家供货商。

### 6.9 下单界面

目标用户：船代采购人员、平台运营。

核心任务：

- 基于比价结果生成订单。
- 确认送船信息、收货联系人、靠泊窗口、驳船/配送要求。
- 提交订单并进入履约跟踪。

主信息架构：

- 订单基础信息：船舶、港口、锚地/泊位、计划靠泊、预计离泊。
- 供应商与清单：供应商、物料明细、数量、价格、交付要求。
- 送船信息：送船方式、驳船需求、送达时间、联系人。
- 费用摘要：商品金额、服务费、驳船费、税费、合计。
- 操作：保存草稿、提交订单、返回比价。

关键状态：

- 草稿、待确认、已下单、供应商确认、待送船、送船中、已送达、异常、取消。

验收标准：

- 下单必须绑定船舶靠泊和送船信息。
- 能从比价界面带入选择结果。

### 6.10 Dashboard

目标用户：船代管理人员、平台运营人员。

核心任务：

- 查看订单整体状态。
- 查看船舶靠泊、预计离泊、送船进度。
- 识别超时、缺货、送船风险。

主信息架构：

- 顶部运营指标：今日订单、待报价、待送船、异常订单、在港船舶。
- 时间轴：船舶靠泊时间、计划送船时间、预计离泊时间。
- 订单状态列表：订单号、船名、港口、供应商、当前状态、下一步、风险。
- 送船状态：待装船、已出库、驳船中、已送达、签收异常。
- 风险提醒：报价超时、靠泊窗口临近、缺货、天气/驳船影响。

关键字段：

- `orderNo`
- `vesselName`
- `port`
- `berth`
- `eta`
- `etd`
- `deliveryStatus`
- `orderStatus`
- `supplierName`
- `riskLevel`
- `nextAction`

验收标准：

- Dashboard 必须体现“订单状态 + 船期 + 送船状态”，不是普通数据大屏。
- 风险项必须有下一步处理入口。

## 6. 后端模块规划

### 6.1 模块边界

- 认证与用户：账号、角色、企业、权限。
- 角色权限：船代、供货商、驳船代理、平台管理员的菜单、数据范围和动作权限。
- 标准库：IMPA 分类、IMPA 物料、搜索、详情。
- 供货商：供货商档案、服务港口、资质、联系人。
- 供应商产品：SKU、IMPA 映射、价格、单位、状态。
- 询价报价：询价单、询价明细、报价单、报价明细。
- 比价决策：报价矩阵、推荐方案、选中供应商。
- 订单履约：订单、订单明细、送船状态、靠泊窗口、费用。
- Dashboard：订单状态聚合、船期聚合、风险聚合。

### 6.2 核心状态流

```text
需求导入
  -> 标准库匹配
  -> 创建询价
  -> 供货商报价
  -> 比价决策
  -> 生成订单
  -> 供应商确认
  -> 备货/出库
  -> 送船中
  -> 已送达
  -> 完成/异常
```

### 6.3 建议接口清单

认证：

- `POST /api/auth/login`
- `POST /api/auth/register`
- `GET /api/auth/me`
- `POST /api/auth/logout`
- `GET /api/auth/menus`
- `GET /api/auth/permissions`

角色权限：

- `GET /api/admin/roles`
- `GET /api/admin/roles/{roleCode}/permissions`
- `PUT /api/admin/roles/{roleCode}/permissions`
- `GET /api/admin/users`
- `PATCH /api/admin/users/{userId}/roles`

企业内角色与账号：

- `GET /api/company/roles`
- `POST /api/company/roles`
- `PATCH /api/company/roles/{roleId}`
- `PATCH /api/company/roles/{roleId}/status` 或 `DELETE /api/company/roles/{roleId}`
- `GET /api/company/roles/{roleId}/permissions`
- `PUT /api/company/roles/{roleId}/permissions`
- `GET /api/company/members?status=ACTIVE|DISABLED|ALL`
- `POST /api/company/members`
- `PATCH /api/company/members/{userId}`
- `PATCH /api/company/members/{userId}/status`
- `POST /api/company/members/{userId}/reset-password`
- `PUT /api/company/members/{userId}/roles`
- `GET /api/company/members/{userId}/menu-permissions`
- `PUT /api/company/members/{userId}/menu-permissions`

物料需求单保存：

- `POST /api/procurement/material-demands/save`：保存或更新物料采购入口预览结果，若无 `demandId` 则新增，有 `demandId` 则更新。
- `GET /api/procurement/material-demands`：物料采购入口已保存需求数据列表，返回主信息和 SKU/匹配统计；当前菜单文案不再使用“需求单管理”。
- `GET /api/procurement/material-demands/{demandId}`：进入物料采购入口编辑时回填主信息和明细。

IMPA 标准库：

- `GET /api/standard-library/impa/categories`
- `GET /api/standard-library/impa/items`
- `GET /api/standard-library/impa/items/{impaCode}`

供货商：

- `GET /api/suppliers`
- `GET /api/suppliers/{supplierId}`
- `POST /api/suppliers`
- `PATCH /api/suppliers/{supplierId}/status`

供货商产品：

- `GET /api/suppliers/{supplierId}/products`
- `POST /api/suppliers/{supplierId}/products`
- `PATCH /api/supplier-products/{productId}`
- `POST /api/supplier-products/{productId}/map-impa`

询价报价：

- `POST /api/inquiries`
- `GET /api/inquiries/{inquiryId}`
- `GET /api/inquiries/{inquiryId}/quotes`
- `POST /api/inquiries/{inquiryId}/quotes`
- `PATCH /api/quotes/{quoteId}`
- `POST /api/quotes/{quoteId}/submit`

比价：

- `GET /api/inquiries/{inquiryId}/compare`
- `POST /api/inquiries/{inquiryId}/selection`
- `POST /api/inquiries/{inquiryId}/create-order`
- 比价接口需支持 `strategy` 参数或等价字段，至少区分 `LOWEST_MIXED` 最低报价混合采买和 `SINGLE_SUPPLIER` 集中采购。
- 比价结果需返回前五家供货商排名、每个策略的总价、缺货项、替代风险、交付风险和推荐说明。

订单：

- `GET /api/orders`
- `GET /api/orders/{orderId}`
- `POST /api/orders`
- `PATCH /api/orders/{orderId}/status`
- `PATCH /api/orders/{orderId}/delivery`

Dashboard：

- `GET /api/dashboard/summary`
- `GET /api/dashboard/orders`
- `GET /api/dashboard/vessel-schedule`
- `GET /api/dashboard/delivery-status`
- `GET /api/dashboard/risks`

## 7. 前端研发拆分

### 7.1 页面组件

认证：

- `LoginView.vue`
- `RegisterView.vue`
- `AuthLayout.vue`

标准库：

- `ImpaLibraryView.vue`
- `ImpaItemList.vue`
- `ImpaItemDetailDrawer.vue`
- `StandardCategorySidebar.vue`

供货商：

- `SupplierListView.vue`
- `SupplierDetailPanel.vue`
- `SupplierProductView.vue`
- `SupplierProductTable.vue`
- `SkuImpaMappingPanel.vue`

交易：

- `QuoteView.vue`
- `QuoteLineTable.vue`
- `CompareView.vue`
- `QuoteMatrix.vue`
- `OrderCreateView.vue`
- `OrderSummaryPanel.vue`

Dashboard：

- `DashboardView.vue`
- `OrderStatusBoard.vue`
- `VesselScheduleTimeline.vue`
- `DeliveryStatusPanel.vue`
- `RiskAlertList.vue`

共享：

- `PlatformTopNav.vue`
- `CollapsibleSidebar.vue`
- `LanguageSwitch.vue`
- `PageToolbar.vue`
- `StatusBadge.vue`
- `MetricTile.vue`
- `DataTable.vue`
- `ExpandablePanel.vue`
- `LoadingOverlay.vue`
- `AnimatedTabs.vue`
- `EmptyState.vue`
- `ConfirmDialog.vue`

### 7.2 服务与类型

建议新增：

- `src/services/authService.ts`
- `src/services/impaLibraryService.ts`
- `src/services/supplierService.ts`
- `src/services/inquiryService.ts`
- `src/services/quoteService.ts`
- `src/services/orderService.ts`
- `src/services/dashboardService.ts`

建议新增类型：

- `src/types/auth.ts`
- `src/types/i18n.ts`
- `src/types/supplier.ts`
- `src/types/procurement.ts`
- `src/types/quote.ts`
- `src/types/order.ts`
- `src/types/dashboard.ts`

### 7.3 Mock 与真实接口策略

- 所有页面先以服务层函数取数，不在页面中直接写 `fetch`。
- 服务层优先请求真实接口，失败后可返回页面级 mock，并给出开发提示。
- 认证、下单、报价提交等写操作不做静默 mock，接口未就绪时显示“接口未接入”。
- 标准库和供货商列表可使用 mock 兜底，保证 UI 研发不断。

## 8. 研发顺序

### P0：基础壳与认证

目标：

- 完成登录、注册、会话状态、顶部导航基础。
- 首页登录/注册按钮接真实路由。

原因：

- 后续页面需要角色和登录态承接。

### P1：IMPA 标准库正式页

目标：

- 使用现有标准库接口做正式查询页。
- 修正当前 `MaterialCategoryPanel` 旧口径，正式页直接展示具体物料，不以二级码段作为主信息。

原因：

- 标准库是供货商 SKU、需求匹配、报价比价的底座。

### P2：供货商信息与供货商产品

目标：

- 建立供货商档案页面。
- 建立 SKU 与 IMPA 映射页面。

原因：

- 没有供应商产品数据，报价和比价只能是假流程。

### P3：报价与比价

目标：

- 报价界面支持供货商逐项报价。
- 比价界面支持多供应商报价矩阵、前五供货商排名、最低报价混采和集中采购两种策略。
- 伙食需求必须先询价、报价后比价；物料需求可基于有效 SKU 日价快速比价，也可转正式询价。

原因：

- 这是船供采购链路核心决策环节。

### P4：下单与 Dashboard

目标：

- 下单页承接比价结果。
- Dashboard 聚合订单、船期和送船状态。

原因：

- 下单和履约需要前面询价、报价、供应商数据支撑。

## 9. 验收口径

### 页面级验收

- 每个页面有清晰目标，不出现与业务无关的宣传区块。
- 后续新页面视觉与物料需求入口保持同一体系，不出现明显割裂。
- 顶部导航下方不出现独立页面介绍头，业务内容直接上顶。
- 页面底部主内容块自适应补满剩余高度，内容不足时容器延伸到底部，内容超出时容器内部滚动。
- 列表页不展示固定假的船舶、港口、ETA、ETD，查询条件来自实际列表主列。
- 页面可见操作默认用图标按钮，图标语义清楚，并有 tooltip、`aria-label` 和国际化 key。
- 桌面常用分辨率下不出现文字重叠和横向溢出。
- 移动端可以完成查看和基础操作，复杂表格允许横向滚动。

### 业务级验收

- 物料采购入口上传并匹配后，保存按钮点击提示“保存成功”，停留当前物料采购入口界面。
- 物料采购入口“报”按钮点击提示“报价成功”，随后跳转询价管理。
- 物料采购入口“比”按钮点击进入比价明细界面，例如 `/procurement/requests/RFQ-240604/compare`。
- 询价管理、报价管理、比价管理、采购管理当前先按同款列表形式展示，每个列表至少 3 条静态/模拟数据。
- 比价可以生成下单。
- 下单可以进入订单状态跟踪。
- Dashboard 可以看到订单状态、船舶靠泊时间、预计离泊时间、送船状态。
- 供货商产品必须能映射 IMPA 标准物料。

### 技术级验收

- `npm run build` 通过。
- 后端新增接口有最小测试或可重复验证 SQL。
- 前端所有新增路由可直接访问。
- 服务层有真实接口和必要兜底策略。

## 10. 暂定研发任务清单

1. 认证页面和路由接入：登录、注册、顶部按钮跳转。
2. IMPA 标准库正式页：搜索、一级分类、物料列表、详情抽屉。
3. 供货商信息页：列表、筛选、详情。
4. 供货商产品页：SKU 列表、IMPA 映射状态、详情。
5. 报价页：询价上下文、报价明细、报价摘要、提交动作。
6. 比价页：报价矩阵、风险提示、供应商选择、生成订单。
7. 下单页：订单确认、送船信息、费用摘要、提交订单。
8. Dashboard：订单状态、船期、送船状态、风险提醒。

## 11. 页面研发前置约束

### 11.1 前端基础设施先行

下一阶段正式做页面前，前端先完成以下基础设施，避免每个页面各自写一套请求、布局和状态。

- 新增 `apiClient.ts`：统一 `fetch`、`VITE_API_BASE_URL`、错误结构、认证头。
- 新增国际化基础设施：`frontend/src/locales/zh-CN.ts`、`frontend/src/locales/en-US.ts`、语言切换状态、缺失 key 开发提示。
- 新增 `PublicLayout.vue`：用于首页、登录、注册。
- 新增 `WorkbenchLayout.vue`：用于 Dashboard、标准库、供货商、报价、比价、下单等业务工作台，内置默认收起的左侧抽屉菜单。
- 新增共享组件：`CollapsibleSidebar`、`LanguageSwitch`、`StatusBadge`、`DataTable`、`SearchInput`、`ExpandablePanel`、`LoadingOverlay`、`AnimatedTabs`、`EmptyState`、`LoadingBlock`、`ErrorNotice`、`ConfirmDialog`。
- 新增统一类型：`ApiResponse<T>`、分页结构、状态枚举。
- 服务层采用真实接口优先；标准库、供货商列表可 mock 兜底，登录、下单、报价提交等写操作不能静默 mock。
- 已完成的主页和物料需求入口不重复研发，只做必要路由衔接和权限承接。

### 11.2 后端建模先行

后端不要把标准库、供货商 SKU、需求明细混成一张商品表。三者边界如下：

- 标准库：平台标准数据，维护 IMPA 分类和物料。
- 供货商 SKU：供应商自己的产品、规格、价格和服务港口，映射到标准库。
- 需求明细：船代导入的原始需求，匹配到标准库，再进入询价报价。
- 供货商 SKU 属性：SKU 的说明、规格、材质、包装、装箱数、尺寸、体积、重量、条码、库存等应作为 SKU 一对多属性或相关扩展表维护，用于询价微调和比价属性选择。
- 供货商 SKU 图片：物料报价单中的图片应进入 SKU 图片表，保存原图、缩略图、主图标记、排序和来源文件信息；前端列表使用缩略图，点击查看大图。

后端状态流转由服务端控制，前端只触发动作，不在前端自行改业务终态。

### 11.3 关键业务状态

需求单状态：

- `DRAFT` 草稿
- `IMPORTED` 已导入
- `MATCHED` 已匹配
- `INQUIRY_SENT` 询价中
- `QUOTE_COLLECTING` 报价回收中
- `COMPARING` 比价中
- `PLAN_READY` 报价方案已生成
- `ORDERED` 已下单
- `DELIVERING` 送船中
- `DELIVERED` 已交付
- `ARCHIVED` 已归档
- `CANCELLED` 已取消

报价状态：

- `PENDING` 待处理
- `VIEWED` 已查看
- `DRAFT` 草稿
- `SUBMITTED` 已提交
- `EXPIRED` 已过期
- `ACCEPTED` 已采纳
- `REJECTED` 已拒绝
- `DECLINED` 已放弃报价

订单和送船状态：

- 订单：`CREATED`、`SUPPLIER_CONFIRMED`、`PREPARING`、`READY_TO_DELIVER`、`DISPATCHED`、`DELIVERED`、`SIGNED`、`EXCEPTION`、`CLOSED`、`CANCELLED`
- 送船：`NOT_SCHEDULED`、`SCHEDULED`、`DEPARTED`、`ARRIVED_PORT`、`LOADED`、`ALONGSIDE`、`DELIVERED`、`SIGNED`、`EXCEPTION`

### 11.4 研发执行包

后续研发按以下包推进：

1. 基础设施包：`apiClient`、布局、共享组件、认证路由。
2. 标准库包：IMPA 搜索、详情、物料列表、供货商覆盖。
3. 供货商包：供货商档案、产品 SKU、IMPA 映射。
4. 需求询价包：需求单、需求明细、发起询价、供应商范围。
5. 报价比价包：供货商报价、报价矩阵、定标/推荐方案、对外报价方案。
6. 下单履约包：订单确认、费用、送船状态、异常处理。
7. Dashboard 包：订单状态、船期、送船状态、供应商响应和风险聚合。

