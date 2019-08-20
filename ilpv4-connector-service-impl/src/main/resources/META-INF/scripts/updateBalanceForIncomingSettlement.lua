local function isempty(s)
    return s == nil or s == '' or s == false
end

local function numberOrZero(num)
    if(isempty(num)) then
        return 0
    else
        return tonumber(num)
    end
end

-- The redis id of the `from` account, of the form `accounts:{account_id}`, is where funds will be subtracted from
local account_id = KEYS[1]
local amount = numberOrZero(ARGV[1])
-- NOTE: The prefix `SETTLEMENT_IDEMPOTENCE:` is appended in the controller layer.
local idempotency_key = ARGV[2]

local clearing_balance, prepaid_amount = unpack(redis.call('HMGET', account_id, 'clearing_balance', 'prepaid_amount'))

clearing_balance = numberOrZero(clearing_balance)
prepaid_amount = numberOrZero(prepaid_amount)

-- If idempotency key has been used, then do not perform any operations
if redis.call('EXISTS', idempotency_key) == 1 then
    return clearing_balance + prepaid_amount
end

-- Otherwise, set it to true (there's no value to cache as a response)
-- and make it expire after 24h (86400 sec)
redis.call('SET', idempotency_key, 'true', 'EX', 86400)

-- Credit the incoming settlement to the clearing_balance and/or prepaid amount,
-- depending on whether that account_id currently owes money or not
if numberOrZero(clearing_balance) >= 0 then
    prepaid_amount = redis.call('HINCRBY', account_id, 'prepaid_amount', amount)
elseif math.abs(numberOrZero(clearing_balance)) >= amount then
    clearing_balance = redis.call('HINCRBY', account_id, 'clearing_balance', amount)
else
    prepaid_amount = redis.call('HINCRBY', account_id, 'prepaid_amount', amount + clearing_balance)
    clearing_balance = 0
    redis.call('HSET', account_id, 'clearing_balance', clearing_balance)
end

return clearing_balance + prepaid_amount
