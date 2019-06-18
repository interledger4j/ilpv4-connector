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

-- The redis id of the `from` account, which is where funds will be subtracted from.
-- This id is of the form `accounts:{account_id}`
local from_account_id = KEYS[1]

-- The amount to subtract from the source account balance.
local from_amount = tonumber(ARGV[1])
if(isempty(from_amount)) then
    error("from_amount was nil!")
end

local min_balance = tonumber(ARGV[2])

local balance, prepaid_amount = unpack(redis.call('HMGET', from_account_id, 'balance', 'prepaid_amount'))

balance = numberOrZero(balance)
prepaid_amount = numberOrZero(prepaid_amount)

-- Check that the prepare wouldn't go under the from-account's minimum balance
if min_balance then
    if balance + prepaid_amount - from_amount < min_balance then
        error('Incoming prepare of ' .. from_amount .. ' would bring account ' .. from_account_id .. ' under its minimum balance. Current balance: ' .. balance .. ', min balance: ' .. min_balance)
    end
end

-- Deduct the from_amount from the prepaid_amount and/or the balance
if prepaid_amount >= from_amount then
    prepaid_amount = redis.call('HINCRBY', from_account_id, 'prepaid_amount', 0 - from_amount)
elseif prepaid_amount > 0 then
    local sub_from_balance = from_amount - prepaid_amount
    prepaid_amount = 0
    redis.call('HSET', from_account_id, 'prepaid_amount', 0)
    balance = redis.call('HINCRBY', from_account_id, 'balance', 0 - sub_from_balance)
else
    balance = redis.call('HINCRBY', from_account_id, 'balance', 0 - from_amount)
end

return balance + prepaid_amount