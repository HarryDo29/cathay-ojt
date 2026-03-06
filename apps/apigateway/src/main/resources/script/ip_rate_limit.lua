local RATE_LIMIT_KEY_PREFIX = "rate_limit:ip:"
local ABUSE_COUNTER_KEY_PREFIX = "abuse_counter:ip:"
local BLACKLIST_KEY_PREFIX = "blacklist:ip:"

local ip = KEYS[1]

local capacity = tonumber(ARGV[1])         -- max tokens in bucket
local refill_rate = tonumber(ARGV[2])      -- tokens/s
local current_time = tonumber(ARGV[3])     -- current time (ms)
local requested = tonumber(ARGV[4])        -- tokens needed (usually 1)
local rate_limit_ttl = tonumber(ARGV[5])   -- TTL for rate limit key (ms)
local abuse_threshold = 10  -- max abuse violations before blacklist
local abuse_ttl = 2 * 60 * 1000        -- TTL for abuse counter key (ms)
local blacklist_ttl = 5 * 60 * 1000    -- TTL for blacklist key (ms)

------------------------------------------------------------------------------
--                      Phase 1: Blacklist Check
------------------------------------------------------------------------------

-- Check if IP is in blacklist: "blacklist:ip:{ip_address}"
local blacklist_key = BLACKLIST_KEY_PREFIX .. ip
if redis.call("EXISTS", blacklist_key) == 1 then
    redis.call("PEXPIRE", blacklist_key, blacklist_ttl)
    return -1
end

------------------------------------------------------------------------------
--                      Phase 2: Token Bucket Rate Limiting
------------------------------------------------------------------------------

-- IP Rate Limiting Key: "rate_limit:ip:{ip_address}"
-- Lấy dữ liệu hiện tại
local rate_limit_key = RATE_LIMIT_KEY_PREFIX .. ip
local rate_limit_data = redis.call("HMGET", rate_limit_key, "tokens", "timestamp")

local tokens = tonumber(rate_limit_data[1])
local last_refill = tonumber(rate_limit_data[2])

if tokens == nil then
    tokens = capacity
    last_refill = current_time
end

-- Tính thời gian trôi qua (ms → seconds)
local delta = math.max(0, current_time - last_refill) / 1000
-- Tính token refill, chỉ advance timestamp phần đã dùng để tránh mất phần dư
local refill_amount = math.floor(delta * refill_rate)
tokens = math.min(capacity, tokens + refill_amount)
-- last refill là thời điểm request tới lần này
last_refill = current_time 

if tokens >= requested then
    tokens = tokens - requested

    -- Lưu lại
    redis.call("HMSET", rate_limit_key,
            "tokens", tokens,
            "timestamp", last_refill)
    redis.call("PEXPIRE", rate_limit_key, rate_limit_ttl)

    return 1
end

-- Giữ nguyên mã thông báo và dấu thời gian ngay cả khi bị từ chối
redis.call("HMSET", rate_limit_key,
        "tokens", tokens,
        "timestamp", current_time)
redis.call("PEXPIRE", rate_limit_key, rate_limit_ttl)

------------------------------------------------------------------------------
--                      Phase 3: Abuse Detection & Auto-Blacklist
------------------------------------------------------------------------------
local abuse_counter_key = ABUSE_COUNTER_KEY_PREFIX .. ip
local abuse_counter_data = redis.call("GET", abuse_counter_key)

local abuse_counter = tonumber(abuse_counter_data)

if abuse_counter == nil then
    abuse_counter = 0
end

abuse_counter = abuse_counter + 1

if abuse_counter >= abuse_threshold then
    redis.call("SET", blacklist_key, 1)
    redis.call("PEXPIRE", blacklist_key, blacklist_ttl)
    redis.call("DEL", abuse_counter_key)
    return -1
end

redis.call("SET", abuse_counter_key, abuse_counter)
redis.call("PEXPIRE", abuse_counter_key, abuse_ttl)

return 0