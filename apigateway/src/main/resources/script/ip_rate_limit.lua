local RATE_LIMIT_KEY_PREFIX = "rate_limit:ip:"
local ABUSE_COUNTER_KEY_PREFIX = "abuse_counter:ip:"
local BLACKLIST_KEY_PREFIX = "blacklist:ip:"

local ip = KEYS[1]

local capacity = tonumber(ARGV[1])     -- số lượng token tối đa trong bucket
local refill_rate = tonumber(ARGV[2])  -- tốc độ refill (tokens/s)
local current_time = tonumber(ARGV[3]) -- thời gian hiện tại (ms)
local requested = tonumber(ARGV[4])    -- token cần cho request này (thường = 1)
local rate_limit_ttl = tonumber(ARGV[5]) -- time to live for the rate limit configuration

------------------------------------------------------------------------------
--                      Blacklist for IP Address
------------------------------------------------------------------------------

-- Check if IP is in blacklist: "blacklist:ip:{ip_address}"
local blacklist_key = BLACKLIST_KEY_PREFIX .. ip

if redis.call("EXISTS", blacklist_key) == 1 then
    return -1
end

------------------------------------------------------------------------------
--                      Rate Limiting for IP Address
------------------------------------------------------------------------------
-- IP Rate Limiting: key có thể là "rate_limit:ip:{ip_address}"
-- Lấy dữ liệu hiện tại
local rate_limit_key = RATE_LIMIT_KEY_PREFIX .. ip

local rate_limit_data = redis.call("HMGET", rate_limit_key, "tokens", "timestamp")

local tokens = tonumber(rate_limit_data[1])
local last_refill = tonumber(rate_limit_data[2])

-- Nếu bucket chưa tồn tại → khởi tạo mới
if tokens == nil and last_refill == nil then
    tokens = capacity
    last_refill = current_time
end

-- Tính thời gian trôi qua (ms → seconds)
local delta = math.max(0, current_time - last_refill) / 1000

-- Tính token refill, chỉ advance timestamp phần đã dùng để tránh mất phần dư
local refill_amount = math.floor(delta * refill_rate)
tokens = math.min(capacity, tokens + refill_amount)

if tokens >= requested then
    tokens = tokens - requested

    -- Lưu lại
    redis.call("HMSET", rate_limit_key,
            "tokens", tokens,
            "timestamp", current_time)


    -- Set TTL (ví dụ 2 phút)
    redis.call("PEXPIRE", rate_limit_key, rate_limit_ttl)

    return 1
end

------------------------------------------------------------------------------
--                      Abuse Counter for IP Address
------------------------------------------------------------------------------
-- If not allowed, check abuse counter: "abuse_counter:ip:{ip_address}"
if tokens < requested then
    local abuse_counter_key = ABUSE_COUNTER_KEY_PREFIX .. ip
    local abuse_count_data = redis.call("HMGET", abuse_counter_key, "abuse_counter")
    local abuse_counter = tonumber(abuse_count_data[1])

    if abuse_counter == nil then
        abuse_counter = 0
    end

    abuse_counter = abuse_counter + 1

    -- Nếu abuse count vượt ngưỡng (ví dụ 10), reject request với code 429
    if abuse_counter > 10 then
        -- Lưu lại
        redis.call("SET", blacklist_key, 1)
        redis.call("PEXPIRE", blacklist_key, 120000)
        return -1
    end


    -- Set TTL cho abuse counter (ví dụ 2 phút)
    if abuse_counter ~= 0 then
        -- Lưu lại
        redis.call("HMSET", abuse_counter_key,
            "abuse_counter", abuse_counter)
        -- Set TTL cho abuse counter (ví dụ 2 phút)
        redis.call("PEXPIRE", abuse_counter_key, 120000)
    end
    return 0
end

