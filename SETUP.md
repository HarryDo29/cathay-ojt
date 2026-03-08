# Hướng dẫn Build & Chạy Cathay (Docker)

Hướng dẫn này giúp người mới pull repo có thể build và chạy toàn bộ hệ thống bằng Docker để test.

## Yêu cầu

- **Docker** và **Docker Compose** đã cài đặt
- **Git** đã cài đặt

## Bước 1: Clone repository

```bash
git clone https://github.com/HarryDo29/cathay-ojt.git
cd cathay
```

## Bước 2: Tạo file cấu hình

Tạo 3 file sau bằng cách copy nội dung bên dưới vào file mới (hoặc dùng lệnh `cp` từ file `.example` nếu có).

### 2.1. `apps/apigateway/secret.properties`

```
# secret properties

access-token.secret = change-me-access-secret-in-production
access-token.expire = 15m

refresh-token.secret = change-me-refresh-secret-in-production
refresh-token.expire = 30d

postgres_db: postgres
postgres_user: apigateway_service
postgres_password: generated_password_for_fun
postgres_port: 5432
postgres_host: localhost

redis_host: localhost
redis_port: 6379
redis_password: mysecret


# Internal API Key - phải trùng với INTERNAL_GATEWAY_SECRET trong .env của identify-service và order-service
internal.api.key = cathay-internal-secret-key-2026-secure-communication
```

### 2.2. `apps/identify-service/.env`

```
DB_HOST=identity-postgres
DB_PORT=5432
DB_USERNAME=identity_service
DB_PASSWORD=generated_password_for_fun
DB_NAME=postgres

JWT_ACCESS_SECRET=change-me-access-secret-in-production
JWT_REFRESH_SECRET=change-me-refresh-secret-in-production

JWT_ACCESS_EXPIRATION=15m
JWT_REFRESH_EXPIRATION=30d

PORT=8081
HOST=0.0.0.0
INTERNAL_GATEWAY_SECRET=cathay-internal-secret-key-2026-secure-communication
```

### 2.3. `apps/order-service/.env`

```
DB_HOST=order-postgres
DB_PORT=5432
DB_USERNAME=order_service
DB_PASSWORD=generated_password_for_fun
DB_NAME=postgres

PORT=8082
NODE_ENV=development
INTERNAL_GATEWAY_SECRET=cathay-internal-secret-key-2026-secure-communication
```

> **Lưu ý:** `INTERNAL_GATEWAY_SECRET` trong `.env` phải trùng với `internal.api.key` trong `secret.properties`.

## Bước 3: Build và chạy Docker

Từ thư mục gốc của project (`cathay/`):

```bash
docker compose up --build -d
```

Lệnh này sẽ:

1. Build image cho **API Gateway** (Java/Spring), **Identify Service** (NestJS), **Order Service** (NestJS)
2. Khởi động **Redis**, **PostgreSQL** (2 instance), và các service
3. Chạy ở chế độ nền (`-d`)

Lần đầu build có thể mất **5–15 phút** tùy máy.

## Bước 4: Kiểm tra các service đã chạy

```bash
docker compose ps
```

Tất cả service phải ở trạng thái `Up` hoặc `running`.

## Bước 5: Truy cập và test

### Entry point (qua API Gateway)

| Mục        | URL                    |
|------------|------------------------|
| API Gateway | http://127.0.0.1:8080 |

### Test nhanh bằng curl

**1. Đăng nhập (lấy JWT):**

```bash
curl -X POST http://localhost:8080/api/v1/identify-service/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@example.com","password":"admin123"}'
```

**2. Lấy danh sách sản phẩm (public, không cần JWT):**

```bash
curl http://localhost:8080/api/v1/order-service/products
```

**3. Gọi API cần JWT (thay `YOUR_ACCESS_TOKEN` bằng token từ bước 1):**

```bash
curl http://localhost:8080/api/v1/order-service/orders \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN"
```

### Tài khoản seed (sau khi chạy lần đầu)

| Role  | Email             | Password |
|-------|-------------------|----------|
| Admin | admin@example.com | admin123 |
| User  | user@example.com  | user123  |

## Các lệnh hữu ích

| Lệnh | Mô tả |
|------|-------|
| `docker compose up -d` | Chạy các container |
| `docker compose down` | Dừng và xóa container |
| `docker compose logs -f` | Xem log real-time |
| `docker compose logs -f identity-service` | Xem log của 1 service |
| `docker compose down -v` | Dừng và xóa cả volumes (xóa data) |

## Xử lý lỗi thường gặp

### 1. Lỗi "Gateway secret is not configured"

- Kiểm tra `INTERNAL_GATEWAY_SECRET` trong `.env` của identify-service và order-service có trùng với `internal.api.key` trong `apps/apigateway/secret.properties`.

### 2. Lỗi kết nối database

- Đợi PostgreSQL khởi động xong (healthcheck). Có thể chạy lại: `docker compose up -d`.

### 3. Build thất bại

- Đảm bảo đã tạo file `.env` trong `apps/identify-service/` và `apps/order-service/` trước khi build.
- Xóa cache và build lại: `docker compose build --no-cache`.

### 4. Port đã được sử dụng

- Kiểm tra port 8080, 8081, 8082, 5433, 5434, 6379 có bị chiếm không.
- Có thể đổi port trong `docker-compose.yml` (ví dụ `"8085:8080"` thay cho `"8080:8080"`).
