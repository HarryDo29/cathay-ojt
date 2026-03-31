# API Gateway — Technical Specification

> **Audience:** Developer trong team  
> **Scope:** Tổng quan kiến trúc, filter chain, cấu hình  
> **Stack:** Java 21 · Spring Boot · Spring Cloud Gateway (WebFlux/Reactive) · Resilience4j · Caffeine

---

## 1. Tổng quan kiến trúc

API Gateway là entry point duy nhất của hệ thống. Mọi request từ client đều đi qua đây trước khi được forward tới upstream service.

```
Client
  │
  ▼
[CorsWebFilter]                    ← CORS (WebFlux filter, ngoài Gateway chain)
  │
  ▼
[IPBasedRateLimitGlobalFilter]     ← order -100 (global)
  │
  ▼
[ValidationGlobalGateway]          ← order -20 (global)
  │
  ▼
[Per-route filters — theo sort_order trong YAML]
  │  EmailBasedRateLimit
  │  Authentication (JWT)
  │  Authorization (RBAC)
  │  AccountBasedRateLimit
  │
  ▼
[CircuitBreaker filter]            ← bọc mỗi route, fallback → /fallback
  │
  ▼
[StripPrefix]                      ← tùy service
  │
  ▼
Upstream Service (identify / order / ...)
```

**Nguồn cấu hình:** Toàn bộ routes, endpoints, roles, rate limits, circuit breaker, CORS, header rules đều đọc từ `application.yml` thông qua các `@ConfigurationProperties` class trong package `data.config`. Không có database runtime — chỉ có R2DBC pool được khai báo nhưng chưa dùng để load config.

---

## 2. Luồng xử lý request

### 2.1. Request đến

1. **CORS check** — `CorsWebFilter` kiểm tra `Origin` header so với danh sách `app.cors.allowedOrigins`.
2. **IP Rate Limit** — `IPBasedRateLimitGlobalFilter` tra cứu IP (ưu tiên `X-Forwarded-For`), áp token bucket. Nếu vượt ngưỡng → `429`; nếu lạm dụng nhiều lần → IP bị blacklist.
3. **Validation** — `ValidationGlobalGateway` kiểm tra:
   - Endpoint có tồn tại và `enabled` không → `404` nếu không.
   - Method rule: body bắt buộc không, `Content-Type` hợp lệ không, body size.
   - Số lượng query params không vượt `app.limits.max_query_params`.
   - Header rules theo endpoint (required, pattern, max length, CRLF injection).
4. **Per-route filters** (thứ tự theo `sort_order`):
   - **EmailBasedRateLimit** — áp sliding window theo email (từ query param hoặc JSON body) cho các public auth path.
   - **Authentication** — verify JWT Bearer; inject `X-User-Id`, `X-User-Email`, `X-User-Role`, `X-Internal-API-Key` vào request tới upstream. Public endpoint bỏ qua JWT, chỉ inject `X-Internal-API-Key` + `Public-Endpoint: true`.
   - **Authorization** — so sánh `roleLevel` của user với `minRoleLevel` của endpoint → `403` nếu không đủ quyền.
   - **AccountBasedRateLimit** — sliding window theo `X-User-Id`, áp cho các path regex được cấu hình.
5. **Circuit Breaker** — Resilience4j bọc mỗi route. Nếu circuit open hoặc timeout → forward tới `/fallback` → `503`/`504`.
6. **StripPrefix** — cắt prefix path trước khi forward (tùy cấu hình từng service).

### 2.2. Response về

Upstream trả về response, Gateway forward thẳng về client. Không có response transformation.

---

## 3. Filter chain chi tiết

### 3.1. `IPBasedRateLimitGlobalFilter` — order `-100`

| Thuộc tính | Giá trị |
|------------|---------|
| Loại | `GlobalFilter` |
| Thuật toán | Token Bucket |
| Key | Client IP (`X-Forwarded-For` hoặc remote address) |
| Config nguồn | `app.ratelimit.rules` — `keyType: IP`, `type: TOKEN_BUCKET` |
| Cache | Caffeine `ipRateLimitCache` (max 10k, expire 2m access) |
| Blacklist | Caffeine `blackListCache` (expire 30m write) |
| Abuse counter | Caffeine `abuseCounterCache` (max 10k, expire 2m access) |

**Hành vi:**
- IP trong blacklist → `429` ngay.
- Token bucket hết token → tăng abuse counter → nếu vượt ngưỡng abuse → thêm vào blacklist → `429`.

### 3.2. `ValidationGlobalGateway` — order `-20`

**Endpoint lookup:** Dùng `PathTrie` (prefix trie hỗ trợ path param `{param}`) để match path. Nếu path không tồn tại → `404`. Nếu path tồn tại nhưng method không có → `405`.

**Method rules** (`app.methods.method_rules`):
- `require_body`: nếu `true` mà không có body → `400`.
- `require_content_type`: nếu `true` mà thiếu `Content-Type` → `415`.
- `max_body_size`: body vượt giới hạn → `413`.

**Query param limit** (`app.limits.max_query_params`): vượt → `400`.

**Header rules** (`app.headers`):
- `required`: thiếu header → `400`.
- `pattern`: không khớp regex → `400`.
- `max_length`: vượt độ dài → `400`.
- CRLF injection detection (`\r`, `\n` trong value) → `400`.

> **Known issue:** Code có đoạn tạo `X-Request-ID` nếu thiếu nhưng không gán lại vào `exchange` — header này không thực sự được inject vào request forward.

### 3.3. `AuthenticationGatewayFilterFactory`

**Public endpoint** (`endpoint.isPublic() == true`):
- Inject `X-Internal-API-Key` (từ `internal.api.key` property).
- Inject `Public-Endpoint: true`.
- Không yêu cầu JWT.

**Protected endpoint:**
- Yêu cầu `Authorization: Bearer <token>`.
- Verify JWT (HS256, secret từ `access-token.secret`).
- Kiểm tra expiry.
- Strip `X-Internal-API-Key` từ client request (nếu có — bảo vệ internal key).
- Inject `X-User-Id`, `X-User-Email`, `X-User-Role`, `X-Internal-API-Key` vào request tới upstream.

**Lỗi:**
- Thiếu/sai token → `401`.
- Token hết hạn → `401`.

### 3.4. `AuthorizationGatewayFilterFactory`

- Public endpoint → pass through.
- Đọc `X-User-Role` (được set bởi Authentication filter).
- Tra `RoleService.getRoleLevel(role)` → lấy `roleEntity.level` (số nguyên, level cao = quyền cao hơn).
- So sánh với `endpoint.minRoleLevel`.
- Không đủ quyền → `403`.

### 3.5. `EmailBasedRateLimitGatewayFilterFactory`

| Thuộc tính | Giá trị |
|------------|---------|
| Thuật toán | Sliding Window |
| Key | Email (query param `email` hoặc JSON body field `email`) |
| Áp dụng khi | `endpoint.isPublic() == true` và path/method khớp rule |
| Config nguồn | `app.ratelimit.rules` — `keyType: EMAIL`, `type: SLIDING_WINDOW` |
| Cache | Caffeine `emailRateLimitCache` |

### 3.6. `AccountBasedRateLimitGatewayFilterFactory`

| Thuộc tính | Giá trị |
|------------|---------|
| Thuật toán | Sliding Window |
| Key | `X-User-Id` header |
| Áp dụng khi | `Public-Endpoint` header không phải `true` và path khớp `path_regex` trong rule |
| Config nguồn | `app.ratelimit.rules` — `keyType: ACCOUNT_ID`, `type: SLIDING_WINDOW` |
| Cache | Caffeine `accountRateLimitCache` |

---

## 4. Cấu hình

### 4.1. Routes & Services (`app.routes`)

```yaml
app:
  routes:
    services:
      - id: <uuid>
        name: identify-service
        path: /api/v1/identify-service/**
        url: http://identity-service:8081
        strip_prefix: true
        enabled: true
    filters:
      - id: <uuid>
        name: Authentication   # phải khớp tên bean filter factory
    service_filters:
      - service_id: <uuid>
        filter_id: <uuid>
        enabled: true
        sort_order: 1          # thứ tự áp filter cho service này
```

`GatewayConfig` build `RouteDefinitionLocator` từ dữ liệu này. Mỗi route có:
- `Path` predicate từ `service.path`
- Danh sách `FilterDefinition` theo `sort_order`
- `CircuitBreaker` filter (name = `defaultCircuitBreaker`, fallback = `forward:/fallback`)
- `StripPrefix` nếu `strip_prefix = true`

### 4.2. Endpoints (`app.gateway`)

```yaml
app:
  gateway:
    endpoints:
      - id: <uuid>
        path: /api/v1/identify-service/auth/login
        method: POST
        service_id: <uuid>
        enabled: true
        is_public: true
        min_role_level: 0
```

Được load vào `PathTrie` lúc startup (`@PostConstruct`). Dùng để lookup trong filter chain.

### 4.3. Circuit Breaker (`app.circuitbreaker`)

```yaml
app:
  circuitbreaker:
    rules:
      - id: <uuid>
        name: defaultCircuitBreaker
        service_id: <uuid>
        enabled: true
        failure_rate_threshold: 50       # %
        slow_call_rate_threshold: 80     # %
        slow_call_duration_threshold: 3  # giây
        sliding_window_size: 10
        minimum_number_of_calls: 5
        wait_duration_in_open_state: 30  # giây
        permitted_calls_in_half_open: 3
```

`GatewayCircuitBreakerConfig` cấu hình `ReactiveResilience4JCircuitBreakerFactory`. Nếu không tìm thấy config theo `service_id`, fallback về phần tử đầu tiên trong list.

> **Known issue:** Nếu list rỗng, `getFirst()` ném `NoSuchElementException` → gateway không start được.

### 4.4. CORS (`app.cors`)

```yaml
app:
  cors:
    allowedOrigins:
      - id: <uuid>
        origin: http://localhost:3000
        enabled: true
    allowedHeaders:
      - id: <uuid>
        header: Authorization
        enabled: true
```

Allowed methods được load từ `app.methods.method_rules`. Max-age cố định `3600s`. Credentials được phép (`allowCredentials: true`).

### 4.5. Rate Limit (`app.ratelimit`)

Rule được lưu dưới dạng JSON string trong field `rule`:

**Token Bucket (IP):**
```json
{ "capacity": 100, "refill_rate": 10, "refill_period_seconds": 1 }
```

**Sliding Window (Email / Account):**
```json
{ "max_requests": 5, "window_seconds": 60, "path_regex": "/api/v1/.*/auth/.*", "methods": ["POST"] }
```

### 4.6. Caching (Caffeine — in-process)

| Cache name | Key | Expire | Max size | Dùng cho |
|------------|-----|--------|----------|----------|
| `blackListCache` | IP string | 30m write | — | IP bị blacklist |
| `abuseCounterCache` | IP string | 2m access | 10k | Đếm số lần IP bị rate limit |
| `ipRateLimitCache` | IP string | 2m access | 10k | Token bucket state theo IP |
| `emailRateLimitCache` | Email string | 2m access | 10k | Sliding window state theo email |
| `accountRateLimitCache` | User ID string | 2m access | 10k | Sliding window state theo account |

> **Lưu ý:** Tất cả cache là **in-process** (không distributed). `RedisConfig` đã bị comment out. Khi chạy nhiều instance gateway, rate limit state **không được chia sẻ** giữa các instance.

---

## 5. Error handling

### 5.1. HTTP error codes

| Code | Tình huống |
|------|-----------|
| `400` | Thiếu/sai header, body, query param; CRLF injection |
| `401` | Thiếu JWT, JWT sai, JWT hết hạn |
| `403` | Role không đủ quyền |
| `404` | Endpoint không tồn tại hoặc `enabled = false` |
| `405` | Method không được phép trên endpoint này |
| `413` | Body vượt `max_body_size` |
| `415` | Thiếu `Content-Type` khi bắt buộc |
| `429` | Vượt rate limit (IP / email / account) |
| `503` | Circuit breaker open; upstream không kết nối được |
| `504` | Upstream timeout |
| `500` | Lỗi không xác định |

### 5.2. Response body lỗi

```json
{
  "status": 401,
  "error": "Unauthorized",
  "message": "JWT token is expired",
  "path": "/api/v1/identify-service/orders",
  "timestamp": "2026-03-30T10:00:00Z"
}
```

### 5.3. Fallback endpoint

`FallbackController` xử lý `GET /fallback` và `POST /fallback` — được Spring Cloud Gateway forward tới khi circuit breaker mở:

```json
{
  "status": 503,
  "error": "Service Unavailable",
  "message": "Service is temporarily unavailable. Please try again later.",
  "timestamp": "2026-03-30T10:00:00Z"
}
```

---

## 6. Internal header contract (Gateway ↔ Upstream)

| Header | Hướng | Mô tả |
|--------|-------|-------|
| `X-Internal-API-Key` | Gateway → Upstream | Shared secret, upstream phải verify để từ chối request không qua gateway |
| `X-User-Id` | Gateway → Upstream | UUID của user đã xác thực |
| `X-User-Email` | Gateway → Upstream | Email của user |
| `X-User-Role` | Gateway → Upstream | Role name của user |
| `Public-Endpoint: true` | Gateway → Upstream | Đánh dấu request từ public endpoint (không có JWT) |
| `X-Forwarded-For` | Client → Gateway | Dùng để lấy real IP cho rate limiting |

> `X-Internal-API-Key` từ client request bị **strip** trước khi forward — upstream không thể bị gọi trực tiếp bằng cách giả mạo header này từ client.

---

## 7. Startup & data loading

Tất cả config được load **một lần lúc startup** qua `@PostConstruct`:

```
ApiGatewayApplication starts
  └─ RouteRegistryService.init()         → load services vào Map<UUID, ServiceEntity>
  └─ ServiceFilterService.init()         → load filter order theo service
  └─ RouteFilterService.init()           → load filter definitions
  └─ CircuitBreakerService.init()        → load circuit breaker rules
  └─ RateLimitService.init()             → load rate limit rules
  └─ RoleService.init()                  → load roles vào Map<String, RoleEntity>
  └─ MethodRuleService.init()            → load method rules
  └─ HeaderRuleService.init()            → load header rules
  └─ EndpointHeaderRuleService.init()    → load endpoint-header rule mapping
  └─ AllowedOriginService.init()         → load CORS origins
  └─ AllowedHeaderService.init()         → load CORS headers
  └─ EndpointRegisterService.init()      → load endpoints vào PathTrie
  └─ ValidationGlobalGateway.init()      → cache header rules vào Map để lookup nhanh
```

**Không có hot reload.** Thay đổi config yêu cầu restart service.
