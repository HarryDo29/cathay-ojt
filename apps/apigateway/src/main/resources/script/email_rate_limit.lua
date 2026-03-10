local RATE_LIMIT_KEY_PREFIX = "rate_limit:email:"

local email = KEYS[1]

local limit = tonumber(ARGV[1])      -- Số lượng request được phép trong window
local window = tonumber(ARGV[2])     -- thời gian window (ms)
local now = tonumber(ARGV[3])        -- Thời gian hiện tại (ms)
local request_id = tonumber(ARGV[4]) -- reuqest_id để tránh duplicate request

local key = RATE_LIMIT_KEY_PREFIX .. email
--          rate_limit:email:{email}

-- remove old requests (out of range (window))
local removed = now - window
redis.call("ZREMRANGEBYSCORE", key, 0, removed)

-- current request count
local count = redis.call("ZCARD", key)

if count >= limit then
    return 0
end

-- add new request
redis.call("ZADD", key, now, request_id)

-- set expire
redis.call("EXPIRE", key, math.ceil(window / 1000))

return 1