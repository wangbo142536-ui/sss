# 交通艇服务大厅与班轮化拼船设计稿

日期：2026-07-09

## 背景

现有交通服务模块已经具备锚地字典、交通艇锚地报价、交通服务订单、交通服务商确认、履约信息、订单规划和路线执行能力。当前主要模式是“系统按锚地最低启用报价直接选择交通艇服务商并生成订单”，适合自动兜底，但不满足新的核心业务要求：

- 船代发布交通艇使用需求后，应推送给所有交通艇服务公司。
- 交通艇服务公司应在服务大厅领取需求并提交报价。
- 船代收到多家公司报价后可以选择成交，系统默认推荐最低报价。
- 交通艇服务公司也可以主动发布班轮化拼船运输服务，明确锚地、启动时间、返回时间和拼船价格。

本设计采用“需求 / 报价 / 成交订单”分层模型，并把班轮化拼船作为供给侧入口。两条业务路径成交后都生成现有 `traffic_service_order`，继续复用确认、履约和订单规划。

## 一期范围

包含：

- 新增船代侧交通艇使用需求发布、取消、查看报价和选择报价。
- 新增交通艇服务商侧服务大厅，支持查看可报价需求、提交或更新报价、撤回报价。
- 报价列表按成交金额默认升序，最低报价作为推荐项。
- 船代选择报价后生成或绑定现有交通服务订单，订单状态沿用现有 `PENDING_CONFIRM -> WAITING_SERVICE -> PLANNED/IN_TRANSIT/COMPLETED`。
- 新增交通艇服务商发布班轮化拼船服务，字段包含锚地、启动时间、返回时间、拼船价、容量和状态。
- 船代可查看可用拼船班次并预订，预订后同样生成现有交通服务订单。
- 路线规划继续只处理已成交的交通服务订单，不直接处理需求或报价。

不包含：

- 不做自动派单算法。
- 不接真实消息推送、短信、邮件或微信通知；本期以服务大厅列表可见代替推送。
- 不做在线支付、结算、发票。
- 不做真实地图、海图、AIS 或动态船位。
- 不做复杂容量优化，只做基础容量字段和展示。

## 业务流程

### 临时用艇竞价

1. 船代创建交通艇使用需求，填写海域、锚地、用艇时间、人员/货物类型、人数或货物明细、是否返程、是否允许拼船、备注。
2. 船代发布需求后，需求进入服务大厅，所有有交通艇服务权限的服务公司可见。
3. 服务公司提交报价，报价包含成交金额、可服务时间、可返程时间、交通艇、联系人和备注。
4. 船代查看报价列表，系统默认把最低有效报价标为推荐。
5. 船代选择某条报价后，需求变为已成交，选中报价变为已选中，其他有效报价变为未选中。
6. 系统生成 `traffic_service_order`，写入选中服务商、成交价格、锚地、时间、人员/货物和备注。
7. 服务商在交通艇服务管理中确认订单并进入履约；订单可继续加入订单规划。

### 班轮化拼船

1. 交通艇服务商发布拼船班次，填写海域、锚地、启动时间、返回时间、拼船价格、可用席位/载货容量和备注。
2. 船代在交通艇拼船服务列表中筛选锚地和时间，选择可用班次。
3. 船代预订班次，填写人数、货物摘要、联系人和备注。
4. 系统生成 `traffic_service_order`，服务商默认为班次发布方，价格为班次拼船价。
5. 后续确认、履约和订单规划沿用现有交通服务订单链路。

## 数据模型

### `traffic_service_request`

表示船代发布的交通艇使用需求。

建议字段：

- `id`
- `request_no`
- `requester_company_id`
- `demand_id`
- `purchase_order_id`
- `fee_type`
- `sea_area`
- `anchorage_code`
- `anchorage_name`
- `use_time`
- `service_type`
- `passenger_type`
- `passenger_count`
- `cargo_type`
- `return_trip`
- `allow_share`
- `remark`
- `status`
- `recommended_quote_id`
- `selected_quote_id`
- `selected_supplier_company_id`
- `traffic_service_order_id`
- `created_by`
- `created_at`
- `updated_at`

状态：

- `DRAFT`：草稿
- `PUBLISHED`：已发布，服务商可报价
- `QUOTING`：已有报价
- `AWARDED`：已选择报价
- `ORDER_CREATED`：已生成交通服务订单
- `CANCELLED`：已取消
- `EXPIRED`：已过期

### `traffic_service_request_cargo`

表示需求侧货物明细。

建议字段：

- `id`
- `request_id`
- `cargo_name`
- `weight_kg`
- `volume_cbm`
- `created_at`

### `traffic_service_quote`

表示交通艇服务公司对需求提交的报价。

建议字段：

- `id`
- `request_id`
- `supplier_company_id`
- `supplier_company_name`
- `quote_amount`
- `base_price`
- `shared_price`
- `currency`
- `available_start_time`
- `available_return_time`
- `traffic_vessel_id`
- `traffic_vessel_name`
- `contact_name`
- `contact_phone`
- `message`
- `status`
- `created_by`
- `created_at`
- `updated_at`

约束：

- 一期建议限制同一需求下每家服务公司只有一条有效报价：`request_id + supplier_company_id` 唯一。

状态：

- `SUBMITTED`：已提交
- `UPDATED`：已更新
- `WITHDRAWN`：已撤回
- `SELECTED`：已选中
- `REJECTED`：未选中
- `EXPIRED`：已过期

### `traffic_service_request_event`

记录需求发布、报价、选择、取消和生成订单事件。

建议字段：

- `id`
- `request_id`
- `quote_id`
- `event_type`
- `event_message`
- `created_by`
- `created_at`

事件类型：

- `REQUEST_CREATED`
- `REQUEST_PUBLISHED`
- `QUOTE_SUBMITTED`
- `QUOTE_UPDATED`
- `QUOTE_WITHDRAWN`
- `QUOTE_SELECTED`
- `REQUEST_CANCELLED`
- `ORDER_CREATED`

### `traffic_shuttle_service`

表示交通艇服务商发布的班轮化拼船服务。

建议字段：

- `id`
- `shuttle_no`
- `supplier_company_id`
- `supplier_company_name`
- `sea_area`
- `anchorage_code`
- `anchorage_name`
- `start_time`
- `return_time`
- `shared_price`
- `passenger_capacity`
- `cargo_capacity_kg`
- `cargo_capacity_cbm`
- `booked_passenger_count`
- `booked_cargo_weight_kg`
- `booked_cargo_volume_cbm`
- `traffic_vessel_id`
- `traffic_vessel_name`
- `status`
- `remark`
- `created_by`
- `created_at`
- `updated_at`

状态：

- `PUBLISHED`：已发布
- `FULL`：已满
- `CLOSED`：已关闭
- `CANCELLED`：已取消
- `COMPLETED`：已完成

### `traffic_shuttle_booking`

表示船代对某个拼船班次的预订。

建议字段：

- `id`
- `booking_no`
- `shuttle_service_id`
- `requester_company_id`
- `request_id`
- `traffic_service_order_id`
- `passenger_count`
- `cargo_summary`
- `cargo_weight_kg`
- `cargo_volume_cbm`
- `amount`
- `contact_name`
- `contact_phone`
- `remark`
- `status`
- `created_by`
- `created_at`
- `updated_at`

状态：

- `BOOKED`：已预订
- `CONFIRMED`：已确认
- `CANCELLED`：已取消
- `COMPLETED`：已完成

## 接口设计

### 船代侧需求接口

`POST /api/traffic/service-requests`

- 创建交通艇使用需求，默认可直接发布，也可草稿保存。

`GET /api/traffic/service-requests`

- 查看当前船代企业发布的需求。
- 支持 `keyword/status/seaArea/anchorageCode/page/size`。

`GET /api/traffic/service-requests/{requestId}`

- 返回需求详情、货物明细、报价列表、推荐报价和事件。

`POST /api/traffic/service-requests/{requestId}/publish`

- 发布需求到服务大厅。

`POST /api/traffic/service-requests/{requestId}/select-quote`

- 请求字段：`quoteId`。
- 后端校验报价属于该需求且状态有效。
- 成功后生成或绑定 `traffic_service_order`。

`POST /api/traffic/service-requests/{requestId}/cancel`

- 取消尚未成交的需求。

### 服务商侧报价大厅接口

`GET /api/supplier/traffic-service-requests`

- 查看可报价需求和本公司已报价需求。
- 支持 `keyword/status/seaArea/anchorageCode/page/size`。

`POST /api/supplier/traffic-service-requests/{requestId}/quotes`

- 提交或更新报价。
- 请求字段：报价金额、可服务时间、可返程时间、交通艇、联系人、备注。

`POST /api/supplier/traffic-service-quotes/{quoteId}/withdraw`

- 撤回本公司的有效报价。

### 班轮化拼船接口

服务商侧：

- `POST /api/supplier/traffic-shuttles`
- `GET /api/supplier/traffic-shuttles`
- `PUT /api/supplier/traffic-shuttles/{shuttleId}`
- `POST /api/supplier/traffic-shuttles/{shuttleId}/close`
- `POST /api/supplier/traffic-shuttles/{shuttleId}/cancel`

船代侧：

- `GET /api/traffic/shuttles`
- `GET /api/traffic/shuttles/{shuttleId}`
- `POST /api/traffic/shuttles/{shuttleId}/book`
- `POST /api/traffic/shuttle-bookings/{bookingId}/cancel`

## 页面设计

### `/transport/services` 船代侧

建议把现有 `delivery` 语义拆为独立 `transportServices` 页面键。

页面包含：

- 交通艇使用需求列表。
- 新增需求按钮。
- 需求详情抽屉或弹窗，展示报价列表。
- 报价列表默认按金额升序，最低价展示“推荐最低”。
- 选择报价动作。
- 已成交需求展示关联交通服务订单入口。

### `/traffic-boat` 服务商侧

页面包含两个视图：

- 服务大厅：可报价需求、我的报价、报价/撤回动作。
- 我的服务订单：已成交并分配给本公司的订单，继续沿用确认和履约表单。

### `/shop/products` 交通服务 Tab

保留现有锚地报价维护，并新增“班轮拼船服务”区：

- 班次列表。
- 新增/编辑班次。
- 关闭/取消班次。
- 显示预订数、容量和状态。

### `/traffic-routes` 订单规划

保持当前定位：

- 只接收已成交的 `traffic_service_order`。
- 不直接展示未成交需求或报价。
- 班轮预订生成的订单可与竞价成交订单一起进入路线规划。

## 采购联动

现有比价页“交通服务”入口可继续保留最低价查询能力，但后续真实交易应从“直接按最低价生成订单”调整为：

1. 保存交通服务信息到 `material_demand.traffic_service_json`。
2. 确认采购下单时生成 `traffic_service_request`。
3. 若业务要求自动推荐，可基于 `traffic_boat_price` 返回参考最低价，但不直接成交。
4. 船代在交通服务需求详情中选择报价后，再生成 `traffic_service_order`。

如果需要兼容当前自动生成订单流程，可保留一个后端开关或临时分支，但默认新口径应走需求竞价。

## 权限与隔离

- 船代只能查看本企业发布的需求、报价和预订。
- 交通艇服务商可查看已发布且未取消/未过期的需求，但只能修改本公司的报价。
- 交通艇服务商只能查看和维护本公司发布的拼船班次。
- 成交后生成的交通服务订单仍按现有公司隔离规则返回。
- 管理员或平台运营角色后续可增加全局查看能力，一期不强制实现。

## 验收点

- 船代能创建并发布交通艇使用需求。
- 交通艇服务商能在服务大厅看到已发布需求并提交报价。
- 船代能看到同一需求下多家公司报价。
- 报价列表默认推荐最低报价。
- 船代选择报价后，需求状态、报价状态和交通服务订单状态同步正确。
- 未选中报价不会生成订单。
- 服务商能发布班轮化拼船服务，包含锚地、启动时间、返回时间和拼船价格。
- 船代能预订拼船服务，预订后生成交通服务订单。
- 生成的交通服务订单能继续在 `/traffic-boat` 确认和在 `/traffic-routes` 规划。
- 页面不出现 `undefined/null/????/[object Object]`。

## 实施顺序建议

1. 新增需求和报价数据表、DTO、Repository、Service、Controller。
2. 接入船代侧需求列表、发布、详情和选择报价。
3. 接入服务商侧服务大厅和报价动作。
4. 调整成交后生成 `traffic_service_order` 的后端逻辑。
5. 新增班轮拼船数据表和接口。
6. 接入服务商发布班次与船代预订班次页面。
7. 联调采购下单生成交通需求的逻辑。
8. 回归交通艇确认、订单规划和采购详情运输服务展示。
