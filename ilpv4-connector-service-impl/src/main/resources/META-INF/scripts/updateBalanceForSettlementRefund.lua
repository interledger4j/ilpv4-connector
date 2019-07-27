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

-- NOTE: This script is ONLY specific to unwinding a potential balance change that occurred in
-- updateBalanceForFulfill, and is called if a request to the settlement engine fails for any reason as a response to
-- that script. This is because the updateBalanceForFulfill script preemptively reduces the clearing_balance if a
-- settlement is necessary. This script unwinds that, if called.

-- The redis id of the account is of the form `accounts:{account_id}`, is where clearing_balance funds will be
-- subtracted from (since outgoing settlement payments should reduce the tracked debt owed).
local account_id = KEYS[1]
local settle_amount = numberOrZero(ARGV[1]) -- The actual amount settled, as reported by the SE.

local clearing_balance = redis.call('HINCRBY', account_id, 'clearing_balance', settle_amount)
return clearing_balance;
