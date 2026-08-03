# 模块化分层与后续研发结构确认稿

## 1. 文档目的

本文档作为后续新增研发的模块化目录、菜单、分层、UI 与工程约束口径。

当前阶段只明确 Markdown 规划，不创建代码目录、不迁移现有代码、不改现有业务逻辑。待用户确认后，再按本文档创建新的目录骨架。

## 2. 固定边界

以下已有能力保持原状，不迁移、不重构、不改变当前运行逻辑：

- Dashboard：现有 `/dashboard`、`/dashboard-government` 及其页面逻辑保持原位置。
- 采购服务中的物料采购部分：现有物料采购入口、询价、比价、采购订单、结算、评价、履约附件等保持原位置。
- 基础管理：现有权限、菜单、注册审核、数据字典、企业成员、IMPA 标准库、企业/供货商资料等保持原位置。

这些模块允许做缺陷修复和必要联调，但不作为新目录首批迁移对象。后续新增功能统一进入新模块目录；已有功能不因为新结构而被强制搬迁。

## 3. 14 个一级模块包

平台后续只固定 14 个一级模块包：12 项服务 + Dashboard + 基础管理。不在本文档里规划二级菜单。

| 序号 | 一级模块 | 前端包名 | 后端包名 | 当前策略 |
| --- | --- | --- | --- | --- |
| 1 | Dashboard | `dashboard` | `dashboard` | 已有实现不动；后续新能力才进新包 |
| 2 | 采购服务 | `procurement` | `procurement` | 物料采购不动；后续新增采购能力进新包 |
| 3 | 船员服务 | `crew` | `crew` | 后续新增 |
| 4 | 驳船服务 | `barge` | `barge` | 后续新增；新驳船能力在此研发 |
| 5 | 海关服务 | `customs` | `customs` | 后续新增 |
| 6 | 边检服务 | `borderInspection` | `borderinspection` | 后续新增 |
| 7 | 海事服务 | `maritime` | `maritime` | 后续新增 |
| 8 | 港航服务 | `portShipping` | `portshipping` | 后续新增 |
| 9 | 监管服务 | `regulatory` | `regulatory` | 后续新增 |
| 10 | 税务服务 | `tax` | `tax` | 后续新增 |
| 11 | 金融服务 | `finance` | `finance` | 后续新增 |
| 12 | 气象服务 | `weather` | `weather` | 后续新增 |
| 13 | 船舶动态服务 | `vesselDynamics` | `vesseldynamics` | 后续新增 |
| 14 | 基础管理 | `basicManagement` | `basicmanagement` | 已有实现不动；后续新能力才进新包 |

## 4. 一级菜单结构

只固定一级菜单，不预设二级菜单：

```text
Dashboard
采购服务
船员服务
驳船服务
海关服务
边检服务
海事服务
港航服务
监管服务
税务服务
金融服务
气象服务
船舶动态服务
基础管理
```

每个一级模块后续是否有二级入口，由具体模块设计稿和接口契约确认后再补，不提前在总目录里写死。

## 5. 前端目录结构

待确认后，新增模块按以下结构创建。已有文件和已有页面保持原位置。

```text
frontend/src/
  app/
    router/
      routes.ts
      routeGuards.ts
    shell/
      AppShell.vue
      ModuleLayout.vue
      ModuleMenu.vue
    providers/
      authProvider.ts
      permissionProvider.ts

  shared/
    components/
      attachments/
      cards/
      dialogs/
      drawers/
      filters/
      forms/
      status/
      tables/
    composables/
    services/
      apiClient.ts
      fileService.ts
    styles/
      tokens.css
      base.css
      layout.css
      motion.css
    types/
    utils/

  modules/
    dashboard/
    procurement/
    crew/
    barge/
    customs/
    borderInspection/
    maritime/
    portShipping/
    regulatory/
    tax/
    finance/
    weather/
    vesselDynamics/
    basicManagement/
```

每个 `modules/<module>/` 内部固定使用：

```text
pages/
components/
services/
types/
styles/
locales/
legacy-adapters/   # 仅 dashboard/procurement/basicManagement 等需要衔接旧实现的模块可有
```

## 6. 后端目录结构

后端现有包保持运行不动。后续新增能力按业务域进入新包。

```text
backend/src/main/java/com/zswy/shipsupply/
  common/
  dashboard/
  procurement/
  crew/
  barge/
  customs/
  borderinspection/
  maritime/
  portshipping/
  regulatory/
  tax/
  finance/
  weather/
  vesseldynamics/
  basicmanagement/
```

每个业务包内部固定使用：

```text
api/
application/
domain/
infrastructure/
legacy/            # 仅需衔接旧实现的模块可有
```

分层含义：

- `api`：Controller、请求/响应 DTO、权限入口。
- `application`：用例编排、事务边界、跨仓储协调。
- `domain`：业务状态、规则、领域对象和值对象。
- `infrastructure`：数据库访问、外部接口、文件存储、第三方服务。
- `legacy`：仅用于新旧结构衔接，不承载新业务主逻辑。

## 7. UI 固定方向

后续新界面必须延续当前已确认的工作台 UI 思路，不重新发明一套风格：

- 使用现有浅色专业海事工作台方向：白色主背景、浅蓝灰页面底、蓝色主动作、细边框、轻阴影、紧凑信息密度。
- 页面进入后直接展示实际业务内容，不再增加大段宣传头图、营销说明、空泛介绍区。
- 主内容区必须充分利用横向空间，右侧不能出现无意义大面积留白。
- 列表、表格、卡片和详情区必须根据容器自适应拉伸，内容不足时也要有合理的空态、占位和工作区高度。
- 查询输入框必须结构化展示：关键字、状态、时间范围、业务对象等按行内紧凑筛选区组织，不把固定船名、港口、日期写成常驻假数据。
- 查询区优先使用现有工作台里的紧凑输入框、下拉框、日期控件、图标按钮和筛选工具栏模式。
- 有线性流程、进度轨、路线、节点、时间轴或连接线时，必须增加轻量动态效果，例如线条推进、节点高亮、hover 反馈、状态过渡；不能是僵硬静态线框。
- 动效以 150-250ms 的轻量过渡为主，不影响操作效率，不做花哨装饰。
- 图标按钮、状态标签、抽屉、筛选条、数据表格、详情卡片优先从现有 UI 中提取样式和交互模式，再沉淀为 shared 组件。

## 8. CSS 与样式硬约束

后续新增 CSS 必须从现有 `workbench.css` 中提取可复用规范，沉淀到 shared 或模块样式，不继续无限追加全局大文件。

- `shared/styles/tokens.css`：颜色、字体、间距、阴影、圆角、层级、状态色等设计变量。
- `shared/styles/base.css`：全局 reset、基础排版、可访问性样式。
- `shared/styles/layout.css`：平台壳层、模块布局、工作区布局。
- `shared/styles/motion.css`：通用过渡、节点线条、进度轨、hover/focus 动效。
- `modules/<module>/styles/*.css`：模块专属样式。
- 单个 CSS 文件建议不超过 1200 行；超过必须按页面、组件或状态拆分。
- 禁止把新模块样式追加到 `frontend/src/styles/workbench.css`。
- 禁止复制整套相似卡片/表格/筛选样式；必须抽到 shared 组件或模块通用样式。
- 新界面做完后要检查移动端和桌面端：文字不能溢出按钮/卡片/表格，控件不能互相覆盖。

## 9. 后续研发硬约束

- 新增研发先进入对应 `modules/<module>/`，不得继续堆进 `WorkbenchPage.vue`。
- 新增页面不得直接依赖物料采购页面内部状态或内部方法；需要复用时通过 service、shared component 或 legacy adapter 衔接。
- 新增接口先明确路径、方法、请求字段、响应字段、状态码、权限点和验收数据。
- 接口未实现时，前端显示明确的“建设中/接口未接入/无权限/暂无数据”状态，不做静默 mock 伪装。
- 每个模块研发前先确认：一级模块、路由、权限、数据范围、接口、页面状态、验收点。
- 每次新增模块必须同步补齐中英文文案位置，文案放入模块 `locales/` 或统一 i18n 汇总，不把业务文案散落在组件里。
- 构建必须通过后才算当前研发完成；涉及真实接口时按后端启动规则验证新代码响应。

## 10. 代码规模、依赖与命名硬约束

以下限制只约束后续新增和重写代码，不要求为了达标立即迁移已有 Dashboard、物料采购和基础管理代码。

### 10.1 文件规模

- 新增 Vue 页面建议控制在 800 行内，达到 800 行必须评估拆分，禁止超过 1200 行后继续追加业务逻辑。
- 新增普通 Vue 组件建议控制在 400 行内；页面状态、表格列、抽屉和复杂表单应拆为独立组件或 composable。
- 新增 CSS 文件建议控制在 800 行内，1200 行为硬上限；超过时按页面、组件、响应式和状态样式拆分。
- 前端 service 文件建议控制在 500 行内，800 行为硬上限；按资源或用例拆分，不建立万能 service。
- 后端 Controller 只负责入参、权限入口和响应转换，建议控制在 400 行内。
- 新增后端 Service、Repository 单文件不得超过 800 行；查询、写入、统计和导入等职责必须按边界拆分。
- 单个函数建议控制在 80 行内；超出时必须说明其事务或算法完整性，或者拆分为可命名、可测试的步骤。

### 10.2 前端依赖方向

- `pages` 负责页面编排，不直接拼接 HTTP 请求、不承载大段匹配算法和数据转换。
- `components` 通过 props、events 或明确的 store/composable 契约通信，不读取其他页面的内部状态。
- `services` 负责接口调用和 DTO 转换，不操作 DOM、不维护页面展示状态。
- `types` 负责模块契约；接口请求、响应、页面模型不得长期混用同一个宽泛类型。
- `shared` 只能放跨两个以上模块稳定复用的能力；单模块专属逻辑不得为了“看起来通用”提前放入 shared。
- 禁止模块之间直接引用对方的 `pages` 或页面私有组件；跨模块协作通过 shared 契约、service 或 adapter。

### 10.3 后端依赖方向

- `api` 只能依赖 `application` 和接口 DTO，不直接写 SQL 或拼装复杂领域规则。
- `application` 编排用例、权限、事务和跨仓储调用，不承载数据库实现细节。
- `domain` 不依赖 Controller、数据库框架和文件存储实现。
- `infrastructure` 实现仓储、文件和外部服务接口，不反向调用 Controller。
- 查询与写入职责明显不同或 SQL 复杂度较高时拆分 Repository；禁止继续形成单个大而全 Repository。

### 10.4 命名规则

- 前端模块目录使用小驼峰；Vue 页面使用 `XxxPage.vue`，业务组件使用 `XxxTable.vue`、`XxxCard.vue`、`XxxDrawer.vue`、`XxxDialog.vue` 等可识别后缀。
- 前端接口文件使用 `xxxService.ts`，组合逻辑使用 `useXxx.ts`，类型文件按业务对象命名，禁止长期使用 `utils2.ts`、`commonNew.ts`、`temp.ts` 等含糊名称。
- 后端使用 `XxxController`、`XxxApplicationService`、`XxxRepository`、`XxxRequest`、`XxxResponse`、`XxxEntity` 等明确职责名称。
- 数据库表、接口字段和状态枚举必须有稳定业务含义；禁止用页面文案直接充当状态值。

### 10.5 重复、Mock 与提交检查

- 同一结构或规则出现两次以上时先评估抽取；禁止整页复制后长期分别维护，仅允许在新模块初始迁移期通过 adapter 保持边界。
- 禁止静默 Mock、随机数据冒充真实接口结果；演示数据必须由用户显式触发，并保存明确的数据来源标记。
- 新接口必须先定义路径、权限、请求/响应、状态枚举、错误码、空态和验收数据。
- 提交前至少通过前端构建、后端构建和本次改动相关测试；涉及真实接口时必须验证运行中的新后端响应。
- 代码评审必须检查目录归属、文件规模、依赖方向、重复样式、权限、异常态、i18n 和数据库索引，不只检查页面是否能打开。

## 11. 已确认结论

以下内容已于 2026-07-16 确认，后续研发直接执行：

- 固定 14 个一级模块包，不在总规划中预设二级菜单。
- 已有 Dashboard、物料采购和基础管理保持原位置，不因新结构强制迁移。
- 后续新研发进入 `frontend/src/modules/<module>/` 和对应后端业务包。
- 新界面执行本文 UI、CSS、分层、文件规模、依赖方向和命名约束。
- 具体模块目录只在该模块方案确认后创建，不提前生成无业务内容的空目录。

## 12. 已落地模块

- 伙食采购已按新模块约束落地于 `frontend/src/modules/procurement/food/` 和后端 `procurement/food/` 包。
- 伙食采购使用独立 `food_*` 数据表、独立 API、独立路由和独立状态机；禁止后续为了复用而直接接入物料采购表或修改物料流程。
- 后续伙食研发继续在现有模块内按页面、服务、领域规则、基础设施和样式职责拆分，不回填到旧工作台大文件。
