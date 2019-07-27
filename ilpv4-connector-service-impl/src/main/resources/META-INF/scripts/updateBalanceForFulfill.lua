local function isempty(s)
    return s == nil or s == '' or s == false
end

local function numberOrZero(num)
    if (isempty(num)) then
        return 0
    else
        return tonumber(num)
    end
end

-- The redis id of the `to` account, of the form `accounts:{account_id}`, is where IOUs will be subtracted from
local to_account_id = KEYS[1]
local amount = numberOrZero(ARGV[1])
if (isempty(amount)) then
    error("amount was nil!") -- should never happen!
end

local settle_threshold = tonumber(ARGV[2]) -- nil if not present.
local settle_to = numberOrZero(ARGV[3]) -- 0 if not present.

local clearing_balance = redis.call('HINCRBY', to_account_id, 'clearing_balance', amount)
local prepaid_amount = numberOrZero(redis.call('HGET', to_account_id, 'prepaid_amount'))

-- Check if we should send a settlement for this account
local settle_amount = 0
-- For context around the values in this if statement, see https://github.com/emschwartz/interledger-rs/pull/164
if settle_threshold and (clearing_balance > settle_threshold) and (clearing_balance > settle_to) then
    settle_amount = clearing_balance - settle_to

    -- Update the clearing_balance _before_ sending the settlement so that we don't accidentally send
    -- multiple settlements for the same balance. While there will be a small moment of time (the delta
    -- between this balance change and the moment that the settlement-engine accepts the request for
    -- settlement payment) where the actual balance in Redis is less than it should be, this is tolerable
    -- because this amount of time will always be small. This is because the design of the settlement
    -- engine API is asynchronous, meaning when a request is made to the settlement engine, it will
    -- accept the request and return (milliseconds) with a guarantee that the settlement payment will
    --  _eventually_ be completed. Because of this settlement_engine guarantee, the Connector can
    -- operate as-if the settlement engine has completed. Finally, if the request to the settlement-engine
    -- fails, this amount will be re-added back to balance.
    redis.call('HSET', to_account_id, 'clearing_balance', settle_to)
end

-- Rust returns the balance as a single thing, but we decompose the two values for usefulness. These will be put
-- together properly in `AccountBalance`.
return { clearing_balance, prepaid_amount, settle_amount }
