local RATE_LIMIT_KEY_PREFIX = "rate_limit:account:"
-- local ABUSE_COUNTER_KEY_PREFIX = "abuse_counter:ip:"

local account_id = KEYS[1]

local capacity = tonumber(ARGV[1])     -- số lượng token tối đa trong bucket
local refill_rate = tonumber(ARGV[2])  -- tốc độ refill (tokens/min)
local current_time = tonumber(ARGV[3]) -- thời gian hiện tại (ms)
local requested = tonumber(ARGV[4])    -- token cần cho request này (thường = 1)
local rate_limit_ttl = tonumber(ARGV[5]) -- time to live for the rate limit configuration

------------------------------------------------------------------------------
--                      Rate Limiting for Account ID
------------------------------------------------------------------------------
-- Account Rate Limiting: key có thể là "rate_limit:account:{account_id}"
-- Lấy dữ liệu hiện tại
local rate_limit_key = RATE_LIMIT_KEY_PREFIX .. account_id
local rate_limit_data = redis.call("HMGET", rate_limit_key, "tokens", "timestamp")

local tokens = tonumber(rate_limit_data[1])
local last_refill = tonumber(rate_limit_data[2])

-- Nếu bucket chưa tồn tại → khởi tạo mới
if tokens == nil then
    tokens = capacity
    last_refill = current_time
end

-- Tính thời gian trôi qua (ms → seconds)
local delta = math.max(0, current_time - last_refill) / (1000 * 60)

-- Tính token refill, chỉ advance timestamp phần đã dùng để tránh mất phần dư
local refill_amount = math.floor(delta * refill_rate)
tokens = math.min(capacity, tokens + refill_amount)

local allowed = tokens >= requested

if allowed then
    tokens = tokens - requested
end

-- Lưu lại
redis.call("HMSET", rate_limit_key,
    "tokens", tokens,
    "timestamp", current_time)

-- Set TTL (ví dụ 1 giờ)
redis.call("PEXPIRE", rate_limit_key, rate_limit_ttl)

return { allowed and 1 or 0}


