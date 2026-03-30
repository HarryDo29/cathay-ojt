# JMeter — API Gateway (non-GUI)

Chạy từ **thư mục gốc repo** (nơi có thư mục `test-apigateway-report`), JMeter đã cài và lệnh `jmeter` có trong `PATH`.

```bash
cd "$(git rev-parse --show-toplevel)"
```

Mỗi lần chạy với `-e -o`, thư mục báo cáo phải **trống hoặc chưa tồn tại**. Trước khi chạy lại có thể xóa report cũ:

```bash
rm -rf test-apigateway-report/Report/AccountRateLimit
rm -rf test-apigateway-report/Report/CircuitBreakerTest
rm -rf test-apigateway-report/Report/EmailRateLimit
rm -rf test-apigateway-report/Report/IPRateLimit
```

## 1. Account rate limit

File test: `test-apigateway-report/Test/AccountRateLimit.jmx`.

#### Mục tiêu

Chứng minh **rate limit gắn với từng tài khoản (user id)**, không phải chỉ với IP hay endpoint chung. Gateway đọc **JWT** ở `Authorization: Bearer …`, giải **`sub`** và gán vào header **`X-User-Id`** trong `AuthenticationGatewayFilterFactory` (khoảng dòng 107). Bộ lọc **AccountBasedRateLimit** dùng `X-User-Id` làm khóa — **không cần** (và không nên) tự gửi `X-User-Id` từ JMeter.

#### Endpoint và xác thực

Endpoint kiểm tra nên là **protected**, ví dụ **`GET /api/v1/identify-service/users/me`**, kèm header **`Authorization: Bearer <access_token>`** (token có `sub` trùng user trong DB). Endpoint **public** có `Public-Endpoint: true` thì account rate limit thường **bị bỏ qua**.

#### Kịch bản Thread Group

| Thread Group | Mô tả | Kỳ vọng |
|----------------|-------|---------|
| **A** | 1 thread; lặp **đủ số request GET** vẫn **dưới** ngưỡng account (ví dụ nếu gateway cho **100 GET / 60s** mỗi account thì A có thể là **100** lần `GET …/users/me` với token **user 1**) | **Không** (toàn bộ) **429** từ account limit; thường **HTTP 200** |
| **B** | Sau A (cùng user 1): **thêm ít nhất một** `GET …/users/me` với cùng loại token | **429 Too Many Requests** — chứng minh đã vượt quota **theo account** |
| **C** | Token **user 2** (`sub` khác); một `GET …/users/me` | **Không** bị 429 chỉ vì user 1 đã hết quota — thường **200**; phân tách bucket theo user |

#### Thứ tự chạy

Chạy **A → B → C** (trong Test Plan bật **chạy tuần tự các Thread Group** nếu cần), để B thực sự là “request tiếp theo” sau khi A đã dùng hết quota của user 1 trong cùng cửa sổ thời gian.

#### Lưu ý

Số vòng lặp và URL trong `.jmx` phải **khớp** rule **ACCOUNT_ID** / sliding window trong `apps/apigateway/.../application.yml` (ví dụ **GET/HEAD** và `limit` / `window`). Nếu đổi ngưỡng trên gateway, cập nhật lại JMeter cho khớp (ví dụ A = `limit`, B = thêm 1 request).

#### Lệnh chạy

```bash
jmeter -n \
  -t ./test-apigateway-report/Test/AccountRateLimit.jmx \
  -l ./test-apigateway-report/Result/AccountRateLimit.jtl \
  -e -o ./test-apigateway-report/Report/AccountRateLimit
```

## 2. Circuit breaker

File test: `test-apigateway-report/Test/CircuitBreakerTest.jmx`.

#### Mục tiêu

Chứng minh **Circuit breaker** trên Spring Cloud Gateway cho **order-service**: dùng **một Thread Group** gọi lần lượt ba endpoint smoke **`test` / `test1` / `test2`** để (1) xác nhận service đang sống, (2) tạo **slow call** đủ để breaker hướng tới **OPEN** (theo `slow_call_duration_threshold` / tỷ lệ slow call), (3) gọi endpoint **phản hồi nhanh** để sau khi chờ `wait_duration` / HALF_OPEN, breaker có thể về **CLOSED**. Khi breaker **OPEN**, gateway có thể trả **503** và body fallback (xử lý tại **`FallbackController.java`**, khoảng dòng 18).

#### Cấu hình JMeter (gợi ý)

- **Một Thread Group** (không tách A/B): trong đó xếp **3 HTTP Request** (hoặc lặp / timer tùy kịch bản) tới các path dưới đây theo **thứ tự logic** bạn cần (thường: `test` → gọi nhiều lần `test1` để đủ slow calls → **Test Action** pause ≥ `wait_duration_in_open_state` nếu cần → `test2`).

#### Ba endpoint (order-service)

Tất cả **GET**, **public** (không JWT), prefix gateway: `/api/v1/order-service/orders/…`.

| Endpoint | Vai trò trong bài test |
|----------|-------------------------|
| **`…/orders/test`** | Kiểm tra **order-service** (và đường forward gateway) **hoạt động bình thường** — kỳ vọng **200**. |
| **`…/orders/test1`** | Backend **trả lời chậm** (delay cố định trong `OrdersController`, ví dụ ~2s) để các lần gọi bị tính **slow call**, tích lũy đủ để **circuit breaker** của order-service trên gateway chuyển **OPEN** (kết hợp cấu hình `slow_call_rate_threshold`, cửa sổ, v.v.). |
| **`…/orders/test2`** | **Phản hồi nhanh** (delay ngắn hơn ngưỡng slow call — trong code có thể ~500ms hoặc chỉnh về gần 0ms) để sau khi breaker cho phép thử lại (**HALF_OPEN**), các request **thành công nhanh** giúp chuyển về **CLOSED**. |

Thứ tự và **số lần** gọi `test1` / khoảng **pause** giữa các bước phải **khớp** `application.yml` và `GatewayCircuitBreakerConfig` (ví dụ `slow_call_duration_threshold`, `wait_duration_in_open_state`, `minimum_number_of_calls`).

#### Lưu ý

Đảm bảo trong **`application.yml`** đã khai báo route GET cho **`/orders/test`**, **`/orders/test1`**, **`/orders/test2`** (trước route `…/orders/{orderId}`). Nếu vẫn dùng kịch bản **tắt hẳn order-service** để thấy 503/fallback, đó là bài kiểm tra **lỗi kết nối**, khác với bài **slow call** ở trên.

#### Lệnh chạy

```bash
jmeter -n \
  -t ./test-apigateway-report/Test/CircuitBreakerTest.jmx \
  -l ./test-apigateway-report/Result/CircuitBreakerTest.jtl \
  -e -o ./test-apigateway-report/Report/CircuitBreakerTest
```

## 3. Email rate limit

File test: `test-apigateway-report/Test/EmailRateLimit.jmx`.

#### Mục tiêu

Chứng minh **rate limit theo email** (khóa = địa chỉ email trong body) trên endpoint **public**. Gateway lấy email từ JSON (`EmailBasedRateLimitGatewayFilterFactory`); **không cần** `Authorization` / JWT.

#### Endpoint và request

- **Method / path:** `POST /api/v1/identify-service/auth/test`
- **Header:** `Content-Type: application/json`
- **Body:** `{"email":"ratelimit1@example.com"}` (hoặc email khác khi kiểm tra tách bucket)

#### Kịch bản

Gửi **32** lần **POST** cùng một body (`ratelimit1@example.com`), **không** timer delay giữa các request (hoặc tối thiểu).

- **30** request đầu: **không** bị chặn (thường **200**), vì vẫn trong quota **theo email** trên gateway.
- **2** request tiếp theo (tổng cộng request thứ **31** và **32**): bị **chặn** (**429**), vì đã vượt quota cùng key email trong cửa sổ thời gian.

Sau đó gửi **thêm 1** lần **POST** cùng endpoint nhưng **`email` khác** (ví dụ `ratelimit2@example.com`): request **lại bình thường** (**200**), vì bucket rate limit **tách** theo từng email — vi phạm của `ratelimit1@…` không “trừ” quota của email khác.

#### Lưu ý

Số **30** / **32** phải **khớp** `limit` / `window` của rule `key_type: EMAIL` trong `application.yml` (ví dụ `limit: 30` trong `60s` → 30 lần đầu OK, 2 lần sau cùng email bị 429). Đổi config gateway thì chỉnh lại số lần gọi trong JMeter cho đúng.

#### Lệnh chạy

```bash
jmeter -n \
  -t ./test-apigateway-report/Test/EmailRateLimit.jmx \
  -l ./test-apigateway-report/Result/EmailRateLimit.jtl \
  -e -o ./test-apigateway-report/Report/EmailRateLimit
```

## 4. IP rate limit

File test: `test-apigateway-report/Test/IPRateLimit.jmx`.

#### Mục tiêu

Chứng minh **rate limit theo IP** (gateway đọc IP client — thường qua **`X-Forwarded-For`** khi chạy sau proxy). Gửi **burst** nhiều request **cùng một IP giả lập** để vượt **burst / token bucket**; một phần request **200**, phần còn lại **429**.

#### Thứ tự chạy

Chạy **IP rate limit sau cùng** trong dãy Account / Circuit breaker / Email / IP, tránh ảnh hưởng tới các kịch bản khác và để **order-service** ở trạng thái ổn định.

#### Endpoint và request

- **Method / path:** `GET /api/v1/order-service/orders/test`
- **Header:** `X-Forwarded-For: 10.10.10.40` (mọi thread dùng **cùng** giá trị để gom một bucket IP)
- **Điều kiện:** Backend **healthy**, **circuit breaker CLOSED** (tránh 503 lẫn với 429).

#### Cấu hình Thread Group

| Tham số | Giá trị gợi ý |
|---------|----------------|
| Threads | **220** |
| Ramp-up | **1** s |
| Loop | **1** mỗi thread (tổng ~220 request gần đồng thời) |

Nếu có **Synchronizing Timer**, bật để **release cùng lúc** càng sát càng tốt — tăng xác suất “đập” vào giới hạn burst trong cùng một cửa sổ.

#### Kỳ vọng

Khoảng **200** response **đi qua** (200), phần còn lại **429** — con số chính xác phụ thuộc `replenish_rate`, `burst_capacity`, `ttl` của rule **key_type: IP** trong `application.yml`. Chỉnh JMeter hoặc config cho khớp.

#### Lưu ý

Cùng một giá trị `X-Forwarded-For` lặp lại nhiều lần có thể bị **blacklist** sau vài lần vi phạm — **đừng** tái sử dụng IP đó cho test khác. Muốn chạy lại: đổi header sang IP khác (ví dụ `10.10.10.41`).

#### Lệnh chạy

```bash
jmeter -n \
  -t ./test-apigateway-report/Test/IPRateLimit.jmx \
  -l ./test-apigateway-report/Result/IPRateLimit.jtl \
  -e -o ./test-apigateway-report/Report/IPRateLimit
```
