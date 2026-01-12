# 🌐 API Gateway Service

![Spring Cloud Gateway](https://img.shields.io/badge/Spring_Cloud-Gateway-green)
![Security](https://img.shields.io/badge/Security-JWT_Filter-blue)
![Status](https://img.shields.io/badge/Status-Active-brightgreen)

> Đây là công vào duy nhất (Entry Point) của toàn hệ thống. Với nhiệm vụ điều hướng request , xác thực token (AuthenticateGatewayFilter) trước khi request đến các microservices phía sau.

## 📑 Mục lục
- [Kiến trúc định tuyến](#-kiến-trúc-định-tuyến)
- [Tính năng Gateway](#-tính-năng-gateway)
- [Cấu hình Routes](#-cấu-hình-routes)
- [Yêu cầu Request](#-yêu-cầu-request)
- [Cài đặt & Chạy](#-cài-đặt--chạy)


## Kiến trúc định tuyến

Hệ thống sử dụng **Spring Cloud Gateway** đóng vai trò là điểm truy cập duy nhất (Single Entry Point) cho toàn bộ hệ thống Microservices. Mọi yêu cầu từ Client đều được định tuyến qua Gateway để đảm bảo tính bảo mật và thống nhất.

### 1. Luồng xử lý Request (Workflow)

```mermaid
graph LR
    Client[🖥️ Client / Frontend] -->|Request| Gateway[🛡️ API Gateway :8080]
    
    subgraph "Internal Network (Microservices)"
        Gateway -->|/api/v1/identify/**| Identity[🔑 Identity Service :8081]
        Gateway -->|/api/v1/users/**| Users[👤 User Service :8082]
        Gateway -->|/api/v1/products/**| Product[📦 Product Service :8083]
    end

    Gateway -- Filter Chain --> Auth[🔐 Authentication Filter]
    Auth -->|Valid Token| Identity
    Auth -->|Valid Token| Users
    Auth -->|Invalid| Error[⛔ 401 Unauthorized]****
```

## Tính năng Gateway

## Cấu hình Routes

## Yêu cầu request

## Cài đặt & Chạy