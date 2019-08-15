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
local from_account_id = KEYS[1]
local from_amount = tonumber(ARGV[1])
if(isempty(from_amount)) then
    error("from_amount was nil!")
end

local clearing_balance = redis.call('HINCRBY', from_account_id, 'clearing_balance', from_amount)
local prepaid_amount = numberOrZero(redis.call('HGET', from_account_id, 'prepaid_amount'))

return clearing_balance + prepaid_amount
