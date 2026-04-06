-- =========================================================
-- CORS: allowed_origins & allowed_headers
-- =========================================================
CREATE TABLE IF NOT EXISTS allowed_origins (
    id UUID PRIMARY KEY,
    origin TEXT NOT NULL,
    enabled BOOLEAN NOT NULL
);

CREATE TABLE IF NOT EXISTS allowed_headers (
    id UUID PRIMARY KEY,
    header TEXT NOT NULL,
    enabled BOOLEAN NOT NULL
);

INSERT INTO allowed_origins (id, origin, enabled) VALUES
('dc2e8355-eaf4-45ed-a0e6-d042f26fc15e','http://localhost:5173', TRUE),
('43b6af5e-7476-4232-a1f6-df9825495caf','http://localhost:3000', TRUE);

INSERT INTO allowed_headers (id, header, enabled) VALUES
('1b3e0079-6d3f-472a-aca1-587f93a5c819','Content-Type', TRUE),
('95b6c166-c87a-4484-8640-6372e0388449','Authorization', TRUE),
('2818d6cb-392e-49b0-be74-108c35b14834','Accept', TRUE),
('acd5b0fb-e0e2-416b-8388-e941b6067056','Origin', TRUE),
('6a8c684e-75f8-431d-be2e-8be6e53f8f33','Access-Control-Request-Method', TRUE),
('fe4c7ac2-bee7-4ee1-8b04-edd57bb6fb1a','Access-Control-Request-Headers', TRUE),
('fe6b3233-bb56-4ba7-be11-ec93677aee8d','X-XSRF-Token', TRUE),
('892d5b15-7704-4f5f-bcc2-a0ff9a6a9b37','X-Request-Id', TRUE),
('885c49ad-1656-477a-a8c4-0efd60e7004c','X-Api-Key', TRUE);

-- =========================================================
-- SERVICES, FILTERS, SERVICE_FILTERS
-- =========================================================
CREATE TABLE IF NOT EXISTS services (
    id UUID PRIMARY KEY,
    name TEXT NOT NULL,
    url TEXT NOT NULL,
    path TEXT NOT NULL,
    strip_prefix INT NOT NULL,
    enabled BOOLEAN NOT NULL
);

CREATE TABLE IF NOT EXISTS filters (
    id UUID PRIMARY KEY,
    name TEXT NOT NULL,
    description TEXT,
    status TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS service_filters (
    id UUID PRIMARY KEY,
    service_id UUID NOT NULL,
    filter_id UUID NOT NULL,
    enabled BOOLEAN NOT NULL,
    sort_order INT NOT NULL,
    FOREIGN KEY (service_id) REFERENCES services(id),
    FOREIGN KEY (filter_id) REFERENCES filters(id)
);

INSERT INTO services (id, name, url, path, strip_prefix, enabled) VALUES
('fc222ee8-06b5-4c35-ad6a-46c079a8cf8e','identity-service','http://localhost:8081','/api/v1/identify-service/**',3,TRUE),
('1d07a7fe-0fbd-4e62-8f52-f6f3452e01dd','order-service',   'http://localhost:8082','/api/v1/order-service/**',   3,TRUE);

INSERT INTO filters (id, name, description, status) VALUES
('5ed4e50d-6b1e-492a-967a-25efd9361232','Authentication','Verify JWT token and set user context','ACTIVE'),
('517067c0-f87d-4cc6-8bc4-6ddae1e4cea2','Authorization','Check user permissions based on roles and endpoint access rules','ACTIVE'),
('074e0fd8-2f25-488f-b4b6-575b3d29ddce','AccountBasedRateLimit','Apply rate limits based on user account','ACTIVE'),
('e4436c68-1b15-443c-999b-b08687766376','EmailBasedRateLimit','Apply rate limits based on email domain for public endpoints needing extra protection','ACTIVE');

INSERT INTO service_filters (id, service_id, filter_id, enabled, sort_order) VALUES
('09f2b031-8750-4f05-a4b5-612575fbb0cd','fc222ee8-06b5-4c35-ad6a-46c079a8cf8e','e4436c68-1b15-443c-999b-b08687766376',TRUE,1),
('75d0b95a-ca78-45b3-8d92-886354197e44','fc222ee8-06b5-4c35-ad6a-46c079a8cf8e','5ed4e50d-6b1e-492a-967a-25efd9361232',TRUE,2),
('e9d76d8c-fe8a-4348-b1e2-6a1e3dac8a53','fc222ee8-06b5-4c35-ad6a-46c079a8cf8e','517067c0-f87d-4cc6-8bc4-6ddae1e4cea2',TRUE,3),
('2947ee25-07d2-4223-859b-779ebad2a28a','fc222ee8-06b5-4c35-ad6a-46c079a8cf8e','074e0fd8-2f25-488f-b4b6-575b3d29ddce',TRUE,4),
('5265c8b1-6c45-448e-be77-d03c83f6f95a','1d07a7fe-0fbd-4e62-8f52-f6f3452e01dd','5ed4e50d-6b1e-492a-967a-25efd9361232',TRUE,1),
('fc932792-4869-4cd3-943a-be0b400879e6','1d07a7fe-0fbd-4e62-8f52-f6f3452e01dd','517067c0-f87d-4cc6-8bc4-6ddae1e4cea2',TRUE,2),
('a747e93a-1fd2-4f8b-92f7-849d5cfc9726','1d07a7fe-0fbd-4e62-8f52-f6f3452e01dd','074e0fd8-2f25-488f-b4b6-575b3d29ddce',TRUE,3);

-- =========================================================
-- ENDPOINTS
-- =========================================================
CREATE TABLE IF NOT EXISTS endpoints (
    id UUID PRIMARY KEY,
    service_id UUID NOT NULL,
    method TEXT NOT NULL,
    path TEXT NOT NULL,
    is_public BOOLEAN NOT NULL,
    min_role_level INT,
    enabled BOOLEAN NOT NULL,
    FOREIGN KEY (service_id) REFERENCES services(id)
);

INSERT INTO endpoints (id, service_id, method, path, is_public, min_role_level, enabled) VALUES
-- IDENTIFY SERVICE: Auth
('17eb2517-7f4e-48d0-81b5-b293fbba7916','fc222ee8-06b5-4c35-ad6a-46c079a8cf8e','POST','/api/v1/identify-service/auth/login',    TRUE,  NULL, TRUE),
('d20e9148-a59a-4be8-ab99-863ceefa2b12','fc222ee8-06b5-4c35-ad6a-46c079a8cf8e','POST','/api/v1/identify-service/auth/register', TRUE,  NULL, TRUE),
('ca80402b-7df3-434b-b18e-32437bb05e65','fc222ee8-06b5-4c35-ad6a-46c079a8cf8e','POST','/api/v1/identify-service/auth/logout',   FALSE, 1,    TRUE),
('005d42d7-2b11-40e6-8f4a-c9bb0324f0d0','fc222ee8-06b5-4c35-ad6a-46c079a8cf8e','POST','/api/v1/identify-service/auth/refresh',  FALSE, 1,    TRUE),
-- IDENTIFY SERVICE: Users
('976aeb26-3f73-4b04-83c2-ea4464d7b239','fc222ee8-06b5-4c35-ad6a-46c079a8cf8e','PATCH','/api/v1/identify-service/users/me/password', FALSE,1,TRUE),
('09be6b41-15c2-4f53-b626-46cf62be95c5','fc222ee8-06b5-4c35-ad6a-46c079a8cf8e','GET',  '/api/v1/identify-service/users/me',          FALSE,1,TRUE),
('800b66bf-655b-4ec5-b1d0-751c583ee34f','fc222ee8-06b5-4c35-ad6a-46c079a8cf8e','PATCH','/api/v1/identify-service/users/me',          FALSE,1,TRUE),
('24f047ef-149d-4d2a-80ff-f79a0a9aa663','fc222ee8-06b5-4c35-ad6a-46c079a8cf8e','DELETE','/api/v1/identify-service/users/me',         FALSE,1,TRUE),
-- ORDER SERVICE
('2b9e0647-4374-4a04-9e31-a2b620fac50b','1d07a7fe-0fbd-4e62-8f52-f6f3452e01dd','POST',  '/api/v1/order-service/products',               FALSE,1,TRUE),
('5b3f989a-ac9d-479b-93cf-acb9fbe6ba08','1d07a7fe-0fbd-4e62-8f52-f6f3452e01dd','GET',   '/api/v1/order-service/products',               TRUE, 0,TRUE),
('d4d6bf35-e11f-461a-a83f-93c4d860156c','1d07a7fe-0fbd-4e62-8f52-f6f3452e01dd','GET',   '/api/v1/order-service/products/{productId}',   FALSE,1,TRUE),
('dcd1e992-8c05-4bdc-adca-57d20b648300','1d07a7fe-0fbd-4e62-8f52-f6f3452e01dd','PATCH', '/api/v1/order-service/products/{productId}',   FALSE,2,TRUE),
('3e7edabf-7996-4d53-8311-77d138169f28','1d07a7fe-0fbd-4e62-8f52-f6f3452e01dd','DELETE','/api/v1/order-service/products/{productId}',   FALSE,2,TRUE),
('10585bb9-af6a-43a6-a688-e77d7db17570','1d07a7fe-0fbd-4e62-8f52-f6f3452e01dd','POST',  '/api/v1/order-service/orders',                 FALSE,1,TRUE),
('c30030cd-16e6-425e-8339-5839cfe480c3','1d07a7fe-0fbd-4e62-8f52-f6f3452e01dd','GET',   '/api/v1/order-service/orders',                 FALSE,1,TRUE),
('67ad9112-4547-4432-b160-725f5cb2b62f','1d07a7fe-0fbd-4e62-8f52-f6f3452e01dd','GET',   '/api/v1/order-service/orders/{orderId}',       FALSE,1,TRUE),
('eb87d196-c367-4a35-9272-fd057e97c81f','1d07a7fe-0fbd-4e62-8f52-f6f3452e01dd','PATCH', '/api/v1/order-service/orders/{orderId}/status',FALSE,1,TRUE),
('59ebd01a-03f2-4baf-8d81-e87171c14cec','1d07a7fe-0fbd-4e62-8f52-f6f3452e01dd','DELETE','/api/v1/order-service/orders/{orderId}',       FALSE,1,TRUE);

-- =========================================================
-- ROLES
-- =========================================================
CREATE TABLE IF NOT EXISTS roles (
    id UUID PRIMARY KEY,
    name TEXT NOT NULL,
    description TEXT,
    level INT NOT NULL,
    enabled BOOLEAN NOT NULL
);

INSERT INTO roles (id, name, description, level, enabled) VALUES
('fa6adb4b-b33b-418f-bf03-a2f719ac6609','ADMIN',  'Administrator with full access',         4, TRUE),
('ca0c424c-e76f-4a85-a5a1-31cc0fac1e46','MANAGER','Manager with elevated permissions',     3, TRUE),
('a00d7585-9525-4dcb-9fa3-235bc2f7aca0','STAFF',  'Guest user with read-only access',      2, TRUE),
('197bd010-297b-4e81-a6a2-0211fb61513a','USER',   'Regular user with limited access',      1, TRUE);

-- =========================================================
-- METHOD RULES
-- =========================================================
CREATE TABLE IF NOT EXISTS method_rules (
    id UUID PRIMARY KEY,
    method TEXT NOT NULL,
    require_body BOOLEAN NOT NULL,
    require_content_type BOOLEAN NOT NULL,
    max_body_size BIGINT NOT NULL
);

INSERT INTO method_rules (id, method, require_body, require_content_type, max_body_size) VALUES
('4e4efc20-fb6f-4d4b-bad6-143ab0b55c26','POST',  TRUE,  TRUE,  10485760),
('0f06cdfb-52be-4be7-9449-b9b387eb9ffe','PUT',   TRUE,  TRUE,  10485760),
('f3a88138-87c6-4dde-b097-30e87be0ca72','PATCH', TRUE,  TRUE,  10485760),
('fe1d6dc9-baab-40a6-83ec-ee3730fd7bd8','GET',   FALSE, FALSE, 0),
('e829edd0-3277-4e20-ba2e-cda738e84f07','DELETE',FALSE, FALSE, 0);

-- =========================================================
-- HEADER RULES & ENDPOINT_HEADER_RULES
-- =========================================================
CREATE TABLE IF NOT EXISTS header_rules (
    id UUID PRIMARY KEY,
    name TEXT NOT NULL,
    max_length INT NOT NULL,
    pattern TEXT NOT NULL,
    description TEXT
);

CREATE TABLE IF NOT EXISTS endpoint_header_rules (
    id UUID PRIMARY KEY,
    endpoint_id UUID NOT NULL,
    header_rule_id UUID NOT NULL,
    required BOOLEAN NOT NULL,
    FOREIGN KEY (endpoint_id) REFERENCES endpoints(id),
    FOREIGN KEY (header_rule_id) REFERENCES header_rules(id)
);

-- Content-Length (id không dùng ở nơi khác, cho DB tự sinh)
INSERT INTO header_rules (id, name, max_length, pattern, description) VALUES
('2c019433-1cd0-4b60-95fb-09af9d019daa','Content-Length',20,'^[0-9]{1,20}$','Size of request body in bytes (numeric only)');

-- Các header còn lại dùng đúng id trong YAML
INSERT INTO header_rules (id, name, max_length, pattern, description) VALUES
('c1b84d8e-f74f-4db5-81b6-b3b6d03c8e7f','Host',           255,'^[a-zA-Z0-9]([a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?(\\.[a-zA-Z0-9]([a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?)*(:[0-9]{1,5})?$','Hostname of the server (e.g., api.example.com, localhost:8080)'),
('b8f85481-fbed-4ff4-bb51-423c48063b73','Accept',         255,'^[a-zA-Z0-9*+\\-./;=, ]+$','Media types that are acceptable for the response (e.g., application/json, text/html, */*)'),
('e1bfbd72-57c0-4042-835c-db5133216f74','Content-Type',   100,'^(application|text|multipart)/(json|xml|x-www-form-urlencoded|form-data|plain|html)(;\\s*charset=[a-zA-Z0-9-]+)?$','Media type of request body (application/json, multipart/form-data, etc.)'),
('6d949be9-0e5a-4c26-ac3a-d546a09ca5b2','User-Agent',     500,'^[a-zA-Z0-9._()/\\-;:, ]+$','User agent string (e.g., Mozilla/5.0, curl/7.68.0)'),
('3b9d4a32-647e-4643-ad15-586b9f099624','Accept-Encoding',100,'^[a-z0-9\\-*,; .=]+$','Compression algorithms (gzip, deflate, br, identity)'),
('0b9d669b-4310-4d44-a142-c662a1f47ac3','Accept-Language',100,'^[a-zA-Z]{2}(-[a-zA-Z]{2})?(,[a-zA-Z]{2}(-[a-zA-Z]{2})?)*$','Preferred languages (e.g., en-US, vi-VN, ja-JP)'),
('bd4769fd-830c-4bd6-8543-6aa1882cce04','Authorization', 4096,'^Bearer\\s+[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+$','Bearer JWT token or Basic auth credentials'),
('13b16182-a203-4785-a490-660f3643eaaf','X-API-Key',      255,'^[a-zA-Z0-9]{32,64}$','API key (32-64 alphanumeric characters)'),
('0bb6e744-41a4-4ac3-a21e-1198813a7608','X-CSRF-Token',   255,'^[a-zA-Z0-9\\-_=]{20,255}$','CSRF token (base64-like format)');

INSERT INTO endpoint_header_rules (id, endpoint_id, header_rule_id, required) VALUES
('1a9e4c2b-1b2e-4eac-9a62-0b6e8a8ea001','ca80402b-7df3-434b-b18e-32437bb05e65','bd4769fd-830c-4bd6-8543-6aa1882cce04', TRUE), -- POST /identify-service/auth/logout
('1a9e4c2b-1b2e-4eac-9a62-0b6e8a8ea002','005d42d7-2b11-40e6-8f4a-c9bb0324f0d0','bd4769fd-830c-4bd6-8543-6aa1882cce04', TRUE), -- POST /identify-service/auth/refresh
('1a9e4c2b-1b2e-4eac-9a62-0b6e8a8ea003','976aeb26-3f73-4b04-83c2-ea4464d7b239','bd4769fd-830c-4bd6-8543-6aa1882cce04', TRUE), -- PATCH /identify-service/users/me/password
('1a9e4c2b-1b2e-4eac-9a62-0b6e8a8ea004','09be6b41-15c2-4f53-b626-46cf62be95c5','bd4769fd-830c-4bd6-8543-6aa1882cce04', TRUE), -- GET /identify-service/users/me
('1a9e4c2b-1b2e-4eac-9a62-0b6e8a8ea005','800b66bf-655b-4ec5-b1d0-751c583ee34f','bd4769fd-830c-4bd6-8543-6aa1882cce04', TRUE), -- PATCH /identify-service/users/me
('1a9e4c2b-1b2e-4eac-9a62-0b6e8a8ea006','24f047ef-149d-4d2a-80ff-f79a0a9aa663','bd4769fd-830c-4bd6-8543-6aa1882cce04', TRUE), -- DELETE /identify-service/users/me
('1a9e4c2b-1b2e-4eac-9a62-0b6e8a8ea007','2b9e0647-4374-4a04-9e31-a2b620fac50b','bd4769fd-830c-4bd6-8543-6aa1882cce04', TRUE), -- POST /order-service/products
('1a9e4c2b-1b2e-4eac-9a62-0b6e8a8ea008','d4d6bf35-e11f-461a-a83f-93c4d860156c','bd4769fd-830c-4bd6-8543-6aa1882cce04', TRUE), -- GET /order-service/products/{productId}
('1a9e4c2b-1b2e-4eac-9a62-0b6e8a8ea009','dcd1e992-8c05-4bdc-adca-57d20b648300','bd4769fd-830c-4bd6-8543-6aa1882cce04', TRUE), -- PATCH /order-service/products/{productId}
('1a9e4c2b-1b2e-4eac-9a62-0b6e8a8ea00a','3e7edabf-7996-4d53-8311-77d138169f28','bd4769fd-830c-4bd6-8543-6aa1882cce04', TRUE), -- DELETE /order-service/products/{productId}
('1a9e4c2b-1b2e-4eac-9a62-0b6e8a8ea00b','10585bb9-af6a-43a6-a688-e77d7db17570','bd4769fd-830c-4bd6-8543-6aa1882cce04', TRUE), -- POST /order-service/orders
('1a9e4c2b-1b2e-4eac-9a62-0b6e8a8ea00c','c30030cd-16e6-425e-8339-5839cfe480c3','bd4769fd-830c-4bd6-8543-6aa1882cce04', TRUE), -- GET /order-service/orders
('1a9e4c2b-1b2e-4eac-9a62-0b6e8a8ea00d','67ad9112-4547-4432-b160-725f5cb2b62f','bd4769fd-830c-4bd6-8543-6aa1882cce04', TRUE), -- GET /order-service/orders/{orderId}
('1a9e4c2b-1b2e-4eac-9a62-0b6e8a8ea00e','eb87d196-c367-4a35-9272-fd057e97c81f','bd4769fd-830c-4bd6-8543-6aa1882cce04', TRUE), -- PATCH /order-service/orders/{orderId}/status
('1a9e4c2b-1b2e-4eac-9a62-0b6e8a8ea00f','59ebd01a-03f2-4baf-8d81-e87171c14cec','bd4769fd-830c-4bd6-8543-6aa1882cce04', TRUE); -- DELETE /order-service/orders/{orderId}

-- =========================================================
-- RATE LIMIT RULES
-- =========================================================
CREATE TABLE IF NOT EXISTS ratelimit_rules (
    id UUID PRIMARY KEY,
    type TEXT NOT NULL,
    key_type TEXT NOT NULL,
    rule TEXT NOT NULL,
    enabled BOOLEAN NOT NULL
);

INSERT INTO ratelimit_rules (id, type, key_type, rule, enabled) VALUES
('faf2473a-7310-4b59-88a6-580d45bc2ca3','TOKEN_BUCKET','IP',
 '{
   "replenish_rate": "10",
   "burst_capacity": "100",
   "ttl": "120"
 }', TRUE),

('28b73ad0-9f59-41a4-b7cb-65b1156d232f','SLIDING_WINDOW','EMAIL',
 '{
   "methods": ["POST", "PUT", "PATCH"],
   "path_regex": "^/api(?:/[^/]+)*/auth/(login|register|reset-password|forgot-password)(?:/.*)?$",
   "window": "60",
   "limit": "10",
   "priority": "1"
 }', TRUE),

('917c6f5d-c52b-47b5-890d-7a823ae6cd60','SLIDING_WINDOW','ACCOUNT_ID',
 '{
   "methods": ["GET", "POST"],
   "path_regex": "^/api/.*/(export|download|generate).*$",
   "window": "120",
   "limit": "2",
   "priority": "1"
 }', TRUE),

('8967e87e-ade9-413c-b0ec-d336c44a9eff','SLIDING_WINDOW','ACCOUNT_ID',
 '{
   "methods": ["POST", "PUT", "PATCH", "DELETE"],
   "path_regex": "*",
   "window": "60",
   "limit": "20",
   "priority": "2"
 }', TRUE),

('a0ffb6f4-bd1b-41ef-b1d4-159542194ba0','SLIDING_WINDOW','ACCOUNT_ID',
 '{
   "methods": ["GET", "HEAD"],
   "path_regex": "*",
   "window": "60",
   "limit": "100",
   "priority": "3"
 }', TRUE);

 -- =========================================================
 -- CIRCUIT BREAKER RULES
 -- =========================================================
 CREATE TABLE IF NOT EXISTS circuit_breaker_rules (
    id UUID PRIMARY KEY,
    service_id UUID NOT NULL,
    is_enabled BOOLEAN NOT NULL,
    failure_rate_threshold INT NOT NULL,
    slow_call_rate_threshold INT NOT NULL,
    slow_call_duration_threshold TEXT NOT NULL,
    permitted_number_of_calls_in_half_open_state INT NOT NULL,
    sliding_window_type TEXT NOT NULL,
    sliding_window_size INT NOT NULL,
    minimum_number_of_calls INT NOT NULL,
    wait_duration_in_open_state TEXT NOT NULL,
    name TEXT NOT NULL,
    FOREIGN KEY (service_id) REFERENCES services(id)
 );

 INSERT INTO circuit_breaker_rules (id, service_id, is_enabled, failure_rate_threshold, slow_call_rate_threshold, slow_call_duration_threshold, permitted_number_of_calls_in_half_open_state, sliding_window_type, sliding_window_size, minimum_number_of_calls, wait_duration_in_open_state, name) VALUES
 ('1c9e4d2b-1b2e-4eac-9a62-0b6e8a8ea010', '', TRUE, 35, 50, '10s', 2, 'COUNT_BASED', 50, 25, '20s', 'defaultCircuitBreaker'),
 ('2f5a6c7d-3e8a-4d21-9b10-7a1c2d3e4f50', '1d07a7fe-0fbd-4e62-8f52-f6f3452e01dd', TRUE, 50, 50, '8s', 5, 'COUNT_BASED', 50, 20, '30s', 'orderServiceCircuitBreaker');