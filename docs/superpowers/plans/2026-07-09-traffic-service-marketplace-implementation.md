# Traffic Service Marketplace Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build the traffic boat demand marketplace and shuttle-sharing supply flow so agency users can publish traffic boat requests, service companies can quote, agencies can select a quote, and shuttle bookings can become normal traffic service orders.

**Architecture:** Add request/quote/shuttle tables and APIs beside the existing `transport` module. Keep `traffic_service_order` as the post-award fulfillment record, so existing traffic boat confirmation and route planning remain reusable.

**Tech Stack:** Java 17, Spring Boot 3.3, Spring JDBC, MySQL 8, Vue 3, TypeScript, existing `WorkbenchPage.vue` operational UI.

---

### Task 1: Database Contract

**Files:**
- Create: `db/mysql/045_traffic_service_marketplace.sql`

- [ ] **Step 1: Create migration**

Create tables `traffic_service_request`, `traffic_service_request_cargo`, `traffic_service_quote`, `traffic_service_request_event`, `traffic_shuttle_service`, and `traffic_shuttle_booking`.

- [ ] **Step 2: Verify SQL shape**

Run: `Get-Content -Raw db/mysql/045_traffic_service_marketplace.sql`

Expected: file contains UTF-8 SQL, idempotent `CREATE TABLE IF NOT EXISTS`, and no garbled Chinese.

### Task 2: Backend DTO And Repository

**Files:**
- Modify: `backend/src/main/java/com/zswy/shipsupply/transport/TrafficDtos.java`
- Modify: `backend/src/main/java/com/zswy/shipsupply/transport/TrafficServiceRepository.java`
- Test: `backend/src/test/java/com/zswy/shipsupply/transport/TrafficServiceMarketplaceRepositoryContractTest.java`

- [ ] **Step 1: Write failing contract test**

Add a lightweight repository contract test that verifies the new DTO names and repository methods exist through reflection:

```java
assertThat(TrafficServiceRepository.class.getDeclaredMethods())
    .extracting(Method::getName)
    .contains("createRequest", "listRequests", "getRequestDetail", "submitQuote", "selectQuote", "createShuttle", "bookShuttle");
```

- [ ] **Step 2: Run test to verify it fails**

Run: `cd backend; mvn -Dtest=TrafficServiceMarketplaceRepositoryContractTest test`

Expected: FAIL because new methods do not exist yet.

- [ ] **Step 3: Add DTO records and repository methods**

Add records for request, quote, events, shuttle service, booking, list responses, and payloads. Implement repository CRUD/list methods with Spring JDBC, following existing normalization and row-mapping style in `TrafficServiceRepository`.

- [ ] **Step 4: Run test to verify it passes**

Run: `cd backend; mvn -Dtest=TrafficServiceMarketplaceRepositoryContractTest test`

Expected: PASS.

### Task 3: Backend Service And Controller

**Files:**
- Modify: `backend/src/main/java/com/zswy/shipsupply/transport/TrafficServiceService.java`
- Modify: `backend/src/main/java/com/zswy/shipsupply/transport/TrafficServiceController.java`
- Test: `backend/src/test/java/com/zswy/shipsupply/transport/TrafficServiceMarketplaceControllerContractTest.java`

- [ ] **Step 1: Write failing controller contract test**

Add a reflection test that verifies the controller exposes:

```java
List.of(
    "/api/traffic/service-requests",
    "/api/supplier/traffic-service-requests",
    "/api/supplier/traffic-service-requests/{requestId}/quotes",
    "/api/traffic/service-requests/{requestId}/select-quote",
    "/api/supplier/traffic-shuttles",
    "/api/traffic/shuttles",
    "/api/traffic/shuttles/{shuttleId}/book"
)
```

- [ ] **Step 2: Run test to verify it fails**

Run: `cd backend; mvn -Dtest=TrafficServiceMarketplaceControllerContractTest test`

Expected: FAIL because endpoints do not exist yet.

- [ ] **Step 3: Add service methods and controller endpoints**

Implement agency-side request list/detail/create/publish/cancel/select quote, supplier-side request hall/quote/withdraw, supplier-side shuttle CRUD-lite, and agency-side shuttle list/detail/book endpoints.

- [ ] **Step 4: Run test to verify it passes**

Run: `cd backend; mvn -Dtest=TrafficServiceMarketplaceControllerContractTest test`

Expected: PASS.

### Task 4: Frontend Service Types

**Files:**
- Modify: `frontend/src/services/trafficService.ts`

- [ ] **Step 1: Extend TypeScript interfaces**

Add interfaces for `TrafficServiceRequest`, `TrafficServiceQuote`, `TrafficServiceRequestDetail`, `TrafficShuttleService`, `TrafficShuttleBooking`, and payloads.

- [ ] **Step 2: Add API wrappers**

Add functions mirroring the new backend endpoints, following the existing `requestTrafficJson` and normalizer style.

- [ ] **Step 3: Verify TypeScript build**

Run: `cd frontend; npm run build`

Expected: type check reaches the app with no missing exported symbols from `trafficService.ts`.

### Task 5: Frontend Pages In Workbench

**Files:**
- Modify: `frontend/src/router/index.ts`
- Modify: `frontend/src/views/WorkbenchPage.vue`
- Modify: `frontend/src/locales/zh-CN.ts`
- Modify: `frontend/src/locales/en-US.ts`
- Modify: `frontend/src/styles/workbench.css`

- [ ] **Step 1: Split `/transport/services` page key**

Change `/transport/services` from `delivery` to `transportServices`.

- [ ] **Step 2: Add agency request UI**

Add request list, create/publish form, detail panel with quote list, and select quote action.

- [ ] **Step 3: Add supplier hall UI**

Add `/traffic-boat` tabs for service hall and existing assigned service orders; add quote form and withdraw action.

- [ ] **Step 4: Add shuttle supply UI**

Extend the enterprise traffic service tab with shuttle list and create/edit form; expose agency-side shuttle booking in `/transport/services`.

- [ ] **Step 5: Verify frontend build**

Run: `cd frontend; npm run build`

Expected: PASS.

### Task 6: Integration Verification

**Files:**
- No new source files expected.

- [ ] **Step 1: Run backend tests**

Run: `cd backend; mvn test`

Expected: PASS or only pre-existing unrelated failures explicitly documented.

- [ ] **Step 2: Restart backend for real API verification**

Stop any process occupying port `8080`, start the backend, then verify a new traffic marketplace endpoint responds from the new code.

- [ ] **Step 3: Verify frontend route**

Use the running Vite app to open `/transport/services` and `/traffic-boat`; confirm no blank page and no obvious untranslated keys or `undefined/null`.

### Task 7: Durable Memory

**Files:**
- Modify: `docs/project_memory.md`

- [ ] **Step 1: Append implementation result**

Record only the durable result: new marketplace data model, endpoints, UI entry points, verification result, and any known residual risk.
