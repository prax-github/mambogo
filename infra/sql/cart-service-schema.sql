-- Cart Service Redis Schema Configuration
-- This file documents the Redis key patterns, data structures, and TTL strategy
-- for the Cart Service implementation

-- Redis Key Patterns and Data Structures
-- =====================================

-- 1. Cart Storage Pattern
-- Key: cart:{userId}
-- Value: JSON string containing cart data
-- TTL: 30 days (2592000 seconds)

-- Example Cart JSON Structure:
/*
{
  "userId": "uuid-string",
  "items": [
    {
      "productId": "uuid-string",
      "quantity": 2,
      "price": 29.99,
      "name": "Product Name",
      "imageUrl": "https://example.com/image.jpg",
      "addedAt": "2025-08-28T12:00:00Z",
      "updatedAt": "2025-08-28T12:00:00Z"
    }
  ],
  "totalItems": 2,
  "totalAmount": 59.98,
  "createdAt": "2025-08-28T12:00:00Z",
  "updatedAt": "2025-08-28T12:00:00Z",
  "expiresAt": "2025-09-27T12:00:00Z"
}
*/

-- 2. Cart Item Locks (for concurrency control)
-- Key: cart:lock:{userId}
-- Value: timestamp when lock was acquired
-- TTL: 30 seconds (to prevent deadlocks)

-- 3. Cart Analytics and Metrics
-- Key: cart:metrics:{userId}:{date}
-- Value: JSON string with cart activity metrics
-- TTL: 90 days (for analytics retention)

-- 4. Cart Expiration Tracking
-- Key: cart:expiry:{timestamp}
-- Value: Set of userIds with carts expiring at that time
-- TTL: 1 day (cleanup job processes these)

-- Redis Configuration Commands
-- ===========================

-- Set Redis configuration for cart service
-- These commands should be executed when setting up Redis

-- 1. Memory Policy Configuration
-- Set max memory policy to allkeys-lru for cart service
-- This ensures carts are evicted when memory is low
-- CONFIG SET maxmemory-policy allkeys-lru

-- 2. Persistence Configuration
-- Enable AOF persistence for cart data durability
-- CONFIG SET appendonly yes
-- CONFIG SET appendfsync everysec

-- 3. Connection Pool Configuration
-- Set max clients for cart service
-- CONFIG SET maxclients 1000

-- 4. TTL Configuration
-- Set default TTL for cart keys (30 days)
-- CONFIG SET timeout 2592000

-- Redis Lua Scripts for Cart Operations
-- =====================================

-- 1. Add Item to Cart (Atomic Operation)
-- Script: add_item_to_cart.lua
/*
local cartKey = KEYS[1]
local lockKey = KEYS[2]
local userId = ARGV[1]
local productId = ARGV[2]
local quantity = ARGV[3]
local price = ARGV[4]
local productName = ARGV[5]
local imageUrl = ARGV[6]

-- Check if cart is locked
if redis.call('EXISTS', lockKey) == 1 then
    return {err = "Cart is locked"}
end

-- Acquire lock
redis.call('SETEX', lockKey, 30, redis.call('TIME')[1])

-- Get existing cart
local cartJson = redis.call('GET', cartKey)
local cart = {}
if cartJson then
    cart = cjson.decode(cartJson)
else
    cart = {
        userId = userId,
        items = {},
        totalItems = 0,
        totalAmount = 0,
        createdAt = redis.call('TIME')[1],
        updatedAt = redis.call('TIME')[1]
    }
end

-- Add or update item
local itemFound = false
for i, item in ipairs(cart.items) do
    if item.productId == productId then
        item.quantity = item.quantity + quantity
        item.updatedAt = redis.call('TIME')[1]
        itemFound = true
        break
    end
end

if not itemFound then
    table.insert(cart.items, {
        productId = productId,
        quantity = quantity,
        price = price,
        name = productName,
        imageUrl = imageUrl,
        addedAt = redis.call('TIME')[1],
        updatedAt = redis.call('TIME')[1]
    })
end

-- Recalculate totals
cart.totalItems = 0
cart.totalAmount = 0
for _, item in ipairs(cart.items) do
    cart.totalItems = cart.totalItems + item.quantity
    cart.totalAmount = cart.totalAmount + (item.price * item.quantity)
end

cart.updatedAt = redis.call('TIME')[1]

-- Save cart with 30-day TTL
redis.call('SETEX', cartKey, 2592000, cjson.encode(cart))

-- Release lock
redis.call('DEL', lockKey)

return {ok = "Item added to cart"}
*/

-- 2. Remove Item from Cart (Atomic Operation)
-- Script: remove_item_from_cart.lua
/*
local cartKey = KEYS[1]
local lockKey = KEYS[2]
local userId = ARGV[1]
local productId = ARGV[2]

-- Check if cart is locked
if redis.call('EXISTS', lockKey) == 1 then
    return {err = "Cart is locked"}
end

-- Acquire lock
redis.call('SETEX', lockKey, 30, redis.call('TIME')[1])

-- Get existing cart
local cartJson = redis.call('GET', cartKey)
if not cartJson then
    redis.call('DEL', lockKey)
    return {err = "Cart not found"}
end

local cart = cjson.decode(cartJson)

-- Remove item
for i = #cart.items, 1, -1 do
    if cart.items[i].productId == productId then
        table.remove(cart.items, i)
        break
    end
end

-- Recalculate totals
cart.totalItems = 0
cart.totalAmount = 0
for _, item in ipairs(cart.items) do
    cart.totalItems = cart.totalItems + item.quantity
    cart.totalAmount = cart.totalAmount + (item.price * item.quantity)
end

cart.updatedAt = redis.call('TIME')[1]

-- Save cart with 30-day TTL
redis.call('SETEX', cartKey, 2592000, cjson.encode(cart))

-- Release lock
redis.call('DEL', lockKey)

return {ok = "Item removed from cart"}
*/

-- 3. Update Item Quantity (Atomic Operation)
-- Script: update_item_quantity.lua
/*
local cartKey = KEYS[1]
local lockKey = KEYS[2]
local userId = ARGV[1]
local productId = ARGV[2]
local newQuantity = ARGV[3]

-- Check if cart is locked
if redis.call('EXISTS', lockKey) == 1 then
    return {err = "Cart is locked"}
end

-- Acquire lock
redis.call('SETEX', lockKey, 30, redis.call('TIME')[1])

-- Get existing cart
local cartJson = redis.call('GET', cartKey)
if not cartJson then
    redis.call('DEL', lockKey)
    return {err = "Cart not found"}
end

local cart = cjson.decode(cartJson)

-- Update item quantity
local itemFound = false
for i, item in ipairs(cart.items) do
    if item.productId == productId then
        if newQuantity <= 0 then
            table.remove(cart.items, i)
        else
            item.quantity = newQuantity
            item.updatedAt = redis.call('TIME')[1]
        end
        itemFound = true
        break
    end
end

if not itemFound and newQuantity > 0 then
    redis.call('DEL', lockKey)
    return {err = "Product not found in cart"}
end

-- Recalculate totals
cart.totalItems = 0
cart.totalAmount = 0
for _, item in ipairs(cart.items) do
    cart.totalItems = cart.totalItems + item.quantity
    cart.totalAmount = cart.totalAmount + (item.price * item.quantity)
end

cart.updatedAt = redis.call('TIME')[1]

-- Save cart with 30-day TTL
redis.call('SETEX', cartKey, 2592000, cjson.encode(cart))

-- Release lock
redis.call('DEL', lockKey)

return {ok = "Cart updated"}
*/

-- 4. Cart Cleanup Job (Expired Carts)
-- Script: cleanup_expired_carts.lua
/*
-- This script should be run periodically to clean up expired carts
-- It finds carts that have exceeded their TTL and removes them

local pattern = "cart:*"
local keys = redis.call('KEYS', pattern)
local cleanedCount = 0

for i, key in ipairs(keys) do
    local ttl = redis.call('TTL', key)
    if ttl == -1 then
        -- Key has no TTL, set it to 30 days
        redis.call('EXPIRE', key, 2592000)
    elseif ttl == -2 then
        -- Key doesn't exist, skip
    elseif ttl == 0 then
        -- Key has expired, remove it
        redis.call('DEL', key)
        cleanedCount = cleanedCount + 1
    end
end

return {cleaned = cleanedCount}
*/

-- Redis Health Check Commands
-- ===========================

-- 1. Check Redis Status
-- PING

-- 2. Check Memory Usage
-- INFO memory

-- 3. Check Connected Clients
-- INFO clients

-- 4. Check Persistence Status
-- INFO persistence

-- 5. Check Replication Status
-- INFO replication

-- Cart Service Configuration Notes
-- ===============================

-- 1. Connection Pool Settings
-- - Max connections: 20
-- - Min idle connections: 5
-- - Connection timeout: 2000ms
-- - Idle timeout: 60000ms

-- 2. Serialization
-- - Use Jackson for JSON serialization/deserialization
-- - Enable compression for large cart objects
-- - Use UTF-8 encoding

-- 3. Error Handling
-- - Implement retry logic for Redis failures
-- - Use circuit breaker pattern for Redis operations
-- - Log all Redis errors with correlation IDs

-- 4. Monitoring
-- - Track cart operations per second
-- - Monitor Redis memory usage
-- - Alert on Redis connection failures
-- - Track cart expiration and cleanup metrics
