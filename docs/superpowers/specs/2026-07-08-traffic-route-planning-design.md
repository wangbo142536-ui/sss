# 交通服务路线规划设计稿

日期：2026-07-08

## 背景

当前系统已经具备交通服务订单能力：物料采购下单时可以生成交通服务单，三方交通服务商可在“三方-交通服务”中查看和确认。下一步需要在“交通服务”下面增加“路线规划”，把同一天、同海域、时间相近、目的地相近的交通服务订单组织成一次出行路线，形成拼船能力。

目标不是做一个普通列表页，而是做一个调度视角的路线工作台：调度人员先看到当天待服务订单，再把订单分配到几条路线中，确认后形成一次完整交通艇出行。

## 一期范围

一期只做“人工规划 + 系统辅助提示”，不做自动最优路径算法。

包含：
- 交通服务下新增“路线规划”页面。
- 按日期、海域、路线状态、交通服务商、是否拼船、锚地筛选订单和路线。
- 展示当天待规划订单、已规划路线和路线站点。
- 支持创建路线、调整站点顺序、把订单加入路线、从路线移除订单。
- 确认路线后，路线进入“待出行”，关联交通服务单进入“已规划”。
- 路线执行过程支持状态流转：待出行、执行中、已完成、废弃。

不包含：
- 不接真实地图导航服务。
- 不做自动最短路径或自动派单算法。
- 不做真实海里数精算。
- 不做结算、付款、发票。
- 不做交通艇移动端。

## 页面结构

入口：
- 父级菜单：交通服务
- 子菜单：路线规划
- 路由建议：`/traffic-routes`

页面分为三块：

### 顶部筛选区

筛选条件：
- 日期
- 海域：北部海域、南部海域
- 路线状态：全部、草稿、待出行、执行中、已完成、废弃
- 交通服务商
- 是否拼船
- 锚地/目的地

操作：
- 刷新
- 新建路线
- 自动推荐分组（一期只生成建议，不自动保存）

### 中间地图/示意航线区

一期使用示意地图，不接真实地图 API。

展示内容：
- 未分配订单点位
- 已规划路线连线
- 路线颜色
- 起点、站点、终点
- 当前选中路线高亮

如果锚地暂无经纬度：
- 用海域分组 + 锚地顺序生成示意坐标。
- 后续可在数据字典或锚地表补充经纬度。

### 右侧路线与站点区

右侧上半部分为路线卡片列表。

路线卡片字段：
- 路线名称
- 交通服务商
- 交通艇
- 计划出发时间
- 预计结束时间
- 订单数
- 是否拼船
- 总收入
- 预计成本
- 预计利润
- 状态

点击路线后，右侧下半部分展示该路线的站点卡片。

站点卡片字段：
- 站点序号
- 锚地/目的地
- 计划服务时间
- 交通服务单号
- 采购单号
- 服务类型：人员接送、物品接送
- 联系人
- 联系电话
- 备注
- 状态

站点支持：
- 上移、下移
- 移出路线
- 查看交通服务单详情

## 数据状态

### 交通服务单状态

沿用现有状态，并新增规划相关状态：
- `PENDING_CONFIRM`：待确认
- `CONFIRMED`：已确认
- `PENDING_ROUTE`：待规划
- `PLANNED`：已规划
- `IN_TRANSIT`：运输中
- `COMPLETED`：已完成
- `DISCARDED`：废弃

说明：
- 三方确认后，交通服务单默认进入 `PENDING_ROUTE`。
- 被加入并确认到某条路线后，进入 `PLANNED`。
- 路线开始执行后，关联订单进入 `IN_TRANSIT`。
- 路线完成后，关联订单进入 `COMPLETED`。

### 路线状态

- `DRAFT`：草稿
- `READY`：待出行
- `IN_PROGRESS`：执行中
- `COMPLETED`：已完成
- `DISCARDED`：废弃

## 数据结构

### `traffic_route_plan`

路线主表。

建议字段：
- `id`
- `route_no`
- `route_name`
- `service_date`
- `sea_area`
- `supplier_company_id`
- `supplier_name`
- `traffic_vessel_id`
- `traffic_vessel_name`
- `planned_departure_time`
- `planned_finish_time`
- `allow_share`
- `order_count`
- `total_income`
- `estimated_cost`
- `estimated_profit`
- `status`
- `remark`
- `created_by`
- `created_at`
- `updated_at`

### `traffic_route_stop`

路线站点表。

建议字段：
- `id`
- `route_plan_id`
- `traffic_service_order_id`
- `stop_sequence`
- `anchorage_code`
- `anchorage_name`
- `planned_service_time`
- `service_type`
- `contact_name`
- `contact_phone`
- `status`
- `remark`
- `created_at`
- `updated_at`

### `traffic_route_event`

路线事件表。

建议字段：
- `id`
- `route_plan_id`
- `traffic_service_order_id`
- `event_type`
- `event_message`
- `created_by`
- `created_at`

事件类型：
- `ROUTE_CREATED`
- `STOP_ADDED`
- `STOP_REMOVED`
- `ROUTE_CONFIRMED`
- `ROUTE_STARTED`
- `ROUTE_COMPLETED`
- `ROUTE_DISCARDED`

### `traffic_service_order` 补充字段

建议新增：
- `route_plan_id`
- `route_stop_id`
- `planned_sequence`
- `planned_service_time`

## 接口设计

### 路线列表

`GET /api/traffic/routes`

查询参数：
- `serviceDate`
- `seaArea`
- `status`
- `supplierCompanyId`
- `allowShare`
- `anchorageCode`
- `page`
- `size`

返回：
- 路线列表
- 每条路线的订单数、收入、成本、利润、状态

### 路线详情

`GET /api/traffic/routes/{routeId}`

返回：
- 路线主信息
- 站点列表
- 关联交通服务订单摘要
- 事件时间轴

### 新建路线

`POST /api/traffic/routes`

请求字段：
- `routeName`
- `serviceDate`
- `seaArea`
- `supplierCompanyId`
- `trafficVesselId`
- `plannedDepartureTime`
- `plannedFinishTime`
- `allowShare`
- `remark`

### 添加站点

`POST /api/traffic/routes/{routeId}/stops`

请求字段：
- `trafficServiceOrderId`
- `plannedServiceTime`
- `stopSequence`

后端校验：
- 交通服务单必须存在。
- 交通服务单不能是废弃或已完成。
- 交通服务单不能已绑定其它未废弃路线。
- 海域与路线海域不一致时返回明确错误。

### 调整站点顺序

`PUT /api/traffic/routes/{routeId}/stops/reorder`

请求字段：
- `stopIds[]`

### 移除站点

`DELETE /api/traffic/routes/{routeId}/stops/{stopId}`

效果：
- 移除路线站点。
- 交通服务单回到 `PENDING_ROUTE`。

### 确认路线

`POST /api/traffic/routes/{routeId}/confirm`

效果：
- 路线状态从 `DRAFT` 变为 `READY`。
- 关联交通服务单状态变为 `PLANNED`。

### 开始执行

`POST /api/traffic/routes/{routeId}/start`

效果：
- 路线状态变为 `IN_PROGRESS`。
- 关联交通服务单状态变为 `IN_TRANSIT`。

### 完成路线

`POST /api/traffic/routes/{routeId}/complete`

效果：
- 路线状态变为 `COMPLETED`。
- 关联交通服务单状态变为 `COMPLETED`。

### 废弃路线

`POST /api/traffic/routes/{routeId}/discard`

效果：
- 路线状态变为 `DISCARDED`。
- 未完成的交通服务单回到 `PENDING_ROUTE` 或按业务确认显示“待重新规划”。

## 推荐分组逻辑

一期不自动派单，只提供推荐分组。

推荐规则：
- 同一天
- 同海域
- 时间差在 2 小时内优先
- 同锚地或相邻锚地优先
- 同服务类型优先
- 允许拼船的订单优先合并

推荐结果展示为“推荐路线草案”，用户点击“采纳”后才创建路线。

## 权限

船代/平台：
- 可查看全部交通服务订单和路线。
- 可创建路线、调整站点、确认路线、废弃路线。

交通服务商：
- 可查看分配给自己的路线。
- 可查看路线内属于自己的服务订单。
- 可执行开始、完成等履约动作。

本期如权限未完全拆细，可先沿用现有交通服务权限，但接口层必须保留公司隔离。

## 页面验收点

- 交通服务下出现“路线规划”菜单。
- 路线规划页顶部筛选能按日期、海域、状态过滤。
- 未规划交通服务订单能展示在页面中。
- 可以创建路线并添加多个交通服务订单。
- 可以调整站点顺序。
- 确认路线后，路线状态为待出行，关联交通服务单状态为已规划。
- 开始路线后，关联交通服务单状态为运输中。
- 完成路线后，关联交通服务单状态为已完成。
- 交通服务商只能看到自己相关路线和订单。
- 页面不出现 `undefined/null/????/[object Object]`。

## 后续增强

- 接入真实地图或海图。
- 锚地维护经纬度。
- 自动路线优化。
- 根据历史速度、天气、船型估算服务时间。
- 路线成本核算与交通服务结算。
- 交通服务商移动端履约。

