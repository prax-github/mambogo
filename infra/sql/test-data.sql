-- =====================================================
-- TEST DATA FOR DATABASE SCHEMA VALIDATION
-- =====================================================

-- Test data for Product Service
INSERT INTO products (id, name, description, price, stock_quantity, category_id, image_url, sku, weight_kg, dimensions_cm, tags, metadata, created_by, updated_by) VALUES
('550e8400-e29b-41d4-a716-446655440001', 'iPhone 15 Pro', 'Latest iPhone with advanced camera system', 999.99, 50, NULL, 'https://example.com/iphone15.jpg', 'IPHONE-15-PRO-001', 0.187, '146.7x71.5x8.25', ARRAY['smartphone', 'apple', '5g'], '{"color": "titanium", "storage": "256GB", "camera": "triple"}', '550e8400-e29b-41d4-a716-446655440099', '550e8400-e29b-41d4-a716-446655440099'),
('550e8400-e29b-41d4-a716-446655440002', 'Samsung Galaxy S24', 'Premium Android smartphone', 899.99, 45, NULL, 'https://example.com/galaxys24.jpg', 'SAMSUNG-S24-001', 0.168, '147.0x70.6x7.6', ARRAY['smartphone', 'android', '5g'], '{"color": "onyx", "storage": "512GB", "camera": "quad"}', '550e8400-e29b-41d4-a716-446655440099', '550e8400-e29b-41d4-a716-446655440099'),
('550e8400-e29b-41d4-a716-446655440003', 'MacBook Pro 16"', 'Professional laptop for developers', 2499.99, 25, NULL, 'https://example.com/macbook-pro.jpg', 'MACBOOK-PRO-16-001', 2.15, '357.9x248.1x16.8', ARRAY['laptop', 'apple', 'professional'], '{"processor": "M3 Pro", "memory": "32GB", "storage": "1TB"}', '550e8400-e29b-41d4-a716-446655440099', '550e8400-e29b-41d4-a716-446655440099');

-- Test data for Order Service
INSERT INTO orders (id, user_id, status, total_amount, shipping_address, billing_address, payment_method, payment_status, idempotency_key, currency, tax_amount, shipping_amount, discount_amount, subtotal_amount, notes, estimated_delivery_date, created_by, updated_by) VALUES
('550e8400-e29b-41d4-a716-446655440101', '550e8400-e29b-41d4-a716-446655440099', 'CONFIRMED', 1099.98, '{"street": "123 Main St", "city": "New York", "state": "NY", "zip": "10001", "country": "USA"}', '{"street": "123 Main St", "city": "New York", "state": "NY", "zip": "10001", "country": "USA"}', 'CREDIT_CARD', 'AUTHORIZED', 'order-001-2024-01-15', 'USD', 89.99, 9.99, 0.00, 1000.00, 'Please deliver to front door', '2024-01-20', '550e8400-e29b-41d4-a716-446655440099', '550e8400-e29b-41d4-a716-446655440099'),
('550e8400-e29b-41d4-a716-446655440102', '550e8400-e29b-41d4-a716-446655440099', 'PENDING', 2499.99, '{"street": "456 Oak Ave", "city": "Los Angeles", "state": "CA", "zip": "90210", "country": "USA"}', '{"street": "456 Oak Ave", "city": "Los Angeles", "state": "CA", "zip": "90210", "country": "USA"}', 'PAYPAL', 'PENDING', 'order-002-2024-01-15', 'USD', 204.99, 19.99, 100.00, 2175.01, 'Gift wrap requested', '2024-01-25', '550e8400-e29b-41d4-a716-446655440099', '550e8400-e29b-41d4-a716-446655440099');

INSERT INTO order_items (id, order_id, product_id, product_name, product_sku, quantity, unit_price, total_price, created_by, updated_by) VALUES
('550e8400-e29b-41d4-a716-446655440201', '550e8400-e29b-41d4-a716-446655440101', '550e8400-e29b-41d4-a716-446655440001', 'iPhone 15 Pro', 'IPHONE-15-PRO-001', 1, 999.99, 999.99, '550e8400-e29b-41d4-a716-446655440099', '550e8400-e29b-41d4-a716-446655440099'),
('550e8400-e29b-41d4-a716-446655440202', '550e8400-e29b-41d4-a716-446655440101', '550e8400-e29b-41d4-a716-446655440002', 'Samsung Galaxy S24', 'SAMSUNG-S24-001', 1, 899.99, 899.99, '550e8400-e29b-41d4-a716-446655440099', '550e8400-e29b-41d4-a716-446655440099'),
('550e8400-e29b-41d4-a716-446655440203', '550e8400-e29b-41d4-a716-446655440102', '550e8400-e29b-41d4-a716-446655440003', 'MacBook Pro 16"', 'MACBOOK-PRO-16-001', 1, 2499.99, 2499.99, '550e8400-e29b-41d4-a716-446655440099', '550e8400-e29b-41d4-a716-446655440099');

INSERT INTO outbox_events (id, aggregate_id, aggregate_type, event_type, event_data, status, created_at, created_by, updated_by) VALUES
('550e8400-e29b-41d4-a716-446655440301', '550e8400-e29b-41d4-a716-446655440101', 'ORDER', 'ORDER_CREATED', '{"orderId": "550e8400-e29b-41d4-a716-446655440101", "userId": "550e8400-e29b-41d4-a716-446655440099", "totalAmount": 1099.98}', 'PENDING', CURRENT_TIMESTAMP, '550e8400-e29b-41d4-a716-446655440099', '550e8400-e29b-41d4-a716-446655440099'),
('550e8400-e29b-41d4-a716-446655440302', '550e8400-e29b-41d4-a716-446655440102', 'ORDER', 'ORDER_CREATED', '{"orderId": "550e8400-e29b-41d4-a716-446655440102", "userId": "550e8400-e29b-41d4-a716-446655440099", "totalAmount": 2499.99}', 'PENDING', CURRENT_TIMESTAMP, '550e8400-e29b-41d4-a716-446655440099', '550e8400-e29b-41d4-a716-446655440099');

INSERT INTO idempotency_keys (id, key_value, service_name, request_hash, response_hash, expires_at, created_at, created_by, updated_by) VALUES
('550e8400-e29b-41d4-a716-446655440401', 'order-001-2024-01-15', 'order-service', 'abc123hash', 'def456hash', CURRENT_TIMESTAMP + INTERVAL '30 minutes', CURRENT_TIMESTAMP, '550e8400-e29b-41d4-a716-446655440099', '550e8400-e29b-41d4-a716-446655440099'),
('550e8400-e29b-41d4-a716-446655440402', 'order-002-2024-01-15', 'order-service', 'ghi789hash', 'jkl012hash', CURRENT_TIMESTAMP + INTERVAL '30 minutes', CURRENT_TIMESTAMP, '550e8400-e29b-41d4-a716-446655440099', '550e8400-e29b-41d4-a716-446655440099');

-- Test data for Payment Service
INSERT INTO payments (id, order_id, user_id, amount, currency, status, payment_method, payment_method_details, payment_reference, created_by, updated_by) VALUES
('550e8400-e29b-41d4-a716-446655440501', '550e8400-e29b-41d4-a716-446655440101', '550e8400-e29b-41d4-a716-446655440099', 1099.98, 'USD', 'AUTHORIZED', 'CREDIT_CARD', '{"cardType": "visa", "last4": "1234", "expiryMonth": 12, "expiryYear": 2026}', 'PAY-REF-001', '550e8400-e29b-41d4-a716-446655440099', '550e8400-e29b-41d4-a716-446655440099'),
('550e8400-e29b-41d4-a716-446655440502', '550e8400-e29b-41d4-a716-446655440102', '550e8400-e29b-41d4-a716-446655440099', 2499.99, 'USD', 'PENDING', 'PAYPAL', '{"paypalEmail": "user@example.com", "paypalAccountType": "personal"}', 'PAY-REF-002', '550e8400-e29b-41d4-a716-446655440099', '550e8400-e29b-41d4-a716-446655440099');

INSERT INTO payment_methods (id, user_id, method_type, method_details, is_default, is_active, created_at, created_by, updated_by) VALUES
('550e8400-e29b-41d4-a716-446655440601', '550e8400-e29b-41d4-a716-446655440099', 'CREDIT_CARD', '{"cardType": "visa", "last4": "1234", "expiryMonth": 12, "expiryYear": 2026, "cardholderName": "John Doe"}', true, true, CURRENT_TIMESTAMP, '550e8400-e29b-41d4-a716-446655440099', '550e8400-e29b-41d4-a716-446655440099'),
('550e8400-e29b-41d4-a716-446655440602', '550e8400-e29b-41d4-a716-446655440099', 'PAYPAL', '{"paypalEmail": "user@example.com", "paypalAccountType": "personal"}', false, true, CURRENT_TIMESTAMP, '550e8400-e29b-41d4-a716-446655440099', '550e8400-e29b-41d4-a716-446655440099');

-- Test data for Inventory Service
INSERT INTO inventory (id, product_id, product_name, product_sku, available_quantity, reserved_quantity, total_quantity, reorder_point, reorder_quantity, max_stock_level, unit_cost, currency, supplier_id, supplier_name, last_restocked_at, next_restock_date, created_by, updated_by) VALUES
('550e8400-e29b-41d4-a716-446655440701', '550e8400-e29b-41d4-a716-446655440001', 'iPhone 15 Pro', 'IPHONE-15-PRO-001', 45, 5, 50, 10, 50, 100, 799.99, 'USD', '550e8400-e29b-41d4-a716-446655440801', 'Apple Inc.', CURRENT_TIMESTAMP - INTERVAL '5 days', CURRENT_DATE + INTERVAL '30 days', '550e8400-e29b-41d4-a716-446655440099', '550e8400-e29b-41d4-a716-446655440099'),
('550e8400-e29b-41d4-a716-446655440702', '550e8400-e29b-41d4-a716-446655440002', 'Samsung Galaxy S24', 'SAMSUNG-S24-001', 40, 5, 45, 10, 50, 100, 719.99, 'USD', '550e8400-e29b-41d4-a716-446655440802', 'Samsung Electronics', CURRENT_TIMESTAMP - INTERVAL '7 days', CURRENT_DATE + INTERVAL '25 days', '550e8400-e29b-41d4-a716-446655440099', '550e8400-e29b-41d4-a716-446655440099'),
('550e8400-e29b-41d4-a716-446655440703', '550e8400-e29b-41d4-a716-446655440003', 'MacBook Pro 16"', 'MACBOOK-PRO-16-001', 20, 5, 25, 5, 25, 50, 1999.99, 'USD', '550e8400-e29b-41d4-a716-446655440801', 'Apple Inc.', CURRENT_TIMESTAMP - INTERVAL '10 days', CURRENT_DATE + INTERVAL '45 days', '550e8400-e29b-41d4-a716-446655440099', '550e8400-e29b-41d4-a716-446655440099');

INSERT INTO inventory_suppliers (id, name, contact_person, email, phone, address, is_active, created_at, created_by, updated_by) VALUES
('550e8400-e29b-41d4-a716-446655440801', 'Apple Inc.', 'John Smith', 'john.smith@apple.com', '+1-800-275-2273', '{"street": "1 Apple Park Way", "city": "Cupertino", "state": "CA", "zip": "95014", "country": "USA"}', true, CURRENT_TIMESTAMP, '550e8400-e29b-41d4-a716-446655440099', '550e8400-e29b-41d4-a716-446655440099'),
('550e8400-e29b-41d4-a716-446655440802', 'Samsung Electronics', 'Jane Doe', 'jane.doe@samsung.com', '+1-800-726-7864', '{"street": "85 Challenger Rd", "city": "Ridgefield Park", "state": "NJ", "zip": "07660", "country": "USA"}', true, CURRENT_TIMESTAMP, '550e8400-e29b-41d4-a716-446655440099', '550e8400-e29b-41d4-a716-446655440099');

INSERT INTO inventory_reservations (id, inventory_id, order_id, quantity, status, expires_at, created_at, created_by, updated_by) VALUES
('550e8400-e29b-41d4-a716-446655440901', '550e8400-e29b-41d4-a716-446655440701', '550e8400-e29b-41d4-a716-446655440101', 1, 'ACTIVE', CURRENT_TIMESTAMP + INTERVAL '30 minutes', CURRENT_TIMESTAMP, '550e8400-e29b-41d4-a716-446655440099', '550e8400-e29b-41d4-a716-446655440099'),
('550e8400-e29b-41d4-a716-446655440902', '550e8400-e29b-41d4-a716-446655440702', '550e8400-e29b-41d4-a716-446655440101', 1, 'ACTIVE', CURRENT_TIMESTAMP + INTERVAL '30 minutes', CURRENT_TIMESTAMP, '550e8400-e29b-41d4-a716-446655440099', '550e8400-e29b-41d4-a716-446655440099'),
('550e8400-e29b-41d4-a716-446655440903', '550e8400-e29b-41d4-a716-446655440703', '550e8400-e29b-41d4-a716-446655440102', 1, 'ACTIVE', CURRENT_TIMESTAMP + INTERVAL '30 minutes', CURRENT_TIMESTAMP, '550e8400-e29b-41d4-a716-446655440099', '550e8400-e29b-41d4-a716-446655440099');

-- =====================================================
-- VALIDATION QUERIES TO TEST SCHEMAS
-- =====================================================

-- Test 1: Verify all tables exist and have data
SELECT 'Product Service Tables' as service, COUNT(*) as table_count FROM information_schema.tables WHERE table_schema = 'public' AND table_name IN ('products', 'categories', 'product_reviews', 'product_images');

SELECT 'Order Service Tables' as service, COUNT(*) as table_count FROM information_schema.tables WHERE table_schema = 'public' AND table_name IN ('orders', 'order_items', 'outbox_events', 'order_status_history', 'order_notes', 'order_fulfillment', 'idempotency_keys');

SELECT 'Payment Service Tables' as service, COUNT(*) as table_count FROM information_schema.tables WHERE table_schema = 'public' AND table_name IN ('payments', 'payment_methods', 'payment_transactions', 'refunds', 'payment_disputes', 'payment_analytics');

SELECT 'Inventory Service Tables' as service, COUNT(*) as table_count FROM information_schema.tables WHERE table_schema = 'public' AND table_name IN ('inventory', 'inventory_reservations', 'inventory_movements', 'inventory_alerts', 'inventory_suppliers', 'inventory_categories', 'inventory_analytics');

-- Test 2: Verify data integrity constraints
SELECT 'Products with valid prices' as test, COUNT(*) as count FROM products WHERE price > 0;
SELECT 'Orders with valid amounts' as test, COUNT(*) as count FROM orders WHERE total_amount >= 10.00 AND total_amount <= 10000.00;
SELECT 'Inventory with valid quantities' as test, COUNT(*) as count FROM inventory WHERE available_quantity + reserved_quantity <= total_quantity;

-- Test 3: Verify foreign key relationships
SELECT 'Orders with valid user references' as test, COUNT(*) as count FROM orders o JOIN (SELECT '550e8400-e29b-41d4-a716-446655440099'::uuid as user_id) u ON o.user_id = u.user_id;
SELECT 'Order items with valid order references' as test, COUNT(*) as count FROM order_items oi JOIN orders o ON oi.order_id = o.id;
SELECT 'Payments with valid order references' as test, COUNT(*) as count FROM payments p JOIN orders o ON p.order_id = o.id;

-- Test 4: Verify business rules
SELECT 'Orders within business limits' as test, COUNT(*) as count FROM orders WHERE total_amount >= 10.00 AND total_amount <= 10000.00;
SELECT 'Active inventory items' as test, COUNT(*) as count FROM inventory WHERE is_active = true;
SELECT 'Valid payment statuses' as test, COUNT(*) as count FROM payments WHERE status IN ('PENDING', 'AUTHORIZED', 'CAPTURED', 'FAILED', 'REFUNDED', 'PARTIALLY_REFUNDED', 'CANCELLED');

-- Test 5: Verify audit fields
SELECT 'Products with audit fields' as test, COUNT(*) as count FROM products WHERE created_at IS NOT NULL AND updated_at IS NOT NULL;
SELECT 'Orders with audit fields' as test, COUNT(*) as count FROM orders WHERE created_at IS NOT NULL AND updated_at IS NOT NULL;
SELECT 'Payments with audit fields' as test, COUNT(*) as count FROM payments WHERE created_at IS NOT NULL AND updated_at IS NOT NULL;

-- Test 6: Verify JSONB fields
SELECT 'Orders with valid address format' as test, COUNT(*) as count FROM orders WHERE jsonb_typeof(shipping_address) = 'object' AND jsonb_typeof(billing_address) = 'object';
SELECT 'Products with valid metadata' as test, COUNT(*) as count FROM products WHERE metadata IS NOT NULL;

-- Test 7: Verify array fields
SELECT 'Products with tags' as test, COUNT(*) as count FROM products WHERE array_length(tags, 1) > 0;

-- Test 8: Verify UUID fields
SELECT 'Valid UUID formats' as test, COUNT(*) as count FROM products WHERE id ~ '^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$';

-- Test 9: Verify timestamps
SELECT 'Recent records' as test, COUNT(*) as count FROM products WHERE created_at > CURRENT_TIMESTAMP - INTERVAL '1 hour';
SELECT 'Updated records' as test, COUNT(*) as count FROM products WHERE updated_at > CURRENT_TIMESTAMP - INTERVAL '1 hour';

-- Test 10: Verify unique constraints
SELECT 'Unique SKUs' as test, COUNT(DISTINCT sku) as unique_count, COUNT(*) as total_count FROM products;
SELECT 'Unique order IDs' as test, COUNT(DISTINCT id) as unique_count, COUNT(*) as total_count FROM orders;
SELECT 'Unique payment references' as test, COUNT(DISTINCT payment_reference) as unique_count, COUNT(*) as total_count FROM payments WHERE payment_reference IS NOT NULL;

-- =====================================================
-- PERFORMANCE TESTING QUERIES
-- =====================================================

-- Test 11: Index performance
EXPLAIN (ANALYZE, BUFFERS) SELECT * FROM products WHERE sku = 'IPHONE-15-PRO-001';
EXPLAIN (ANALYZE, BUFFERS) SELECT * FROM orders WHERE user_id = '550e8400-e29b-41d4-a716-446655440099'::uuid;
EXPLAIN (ANALYZE, BUFFERS) SELECT * FROM inventory WHERE product_sku = 'IPHONE-15-PRO-001';

-- Test 12: Full-text search (if pg_trgm extension is available)
-- SELECT * FROM products WHERE name % 'iPhone' OR description % 'camera';

-- Test 13: JSONB query performance
EXPLAIN (ANALYZE, BUFFERS) SELECT * FROM orders WHERE shipping_address->>'city' = 'New York';
EXPLAIN (ANALYZE, BUFFERS) SELECT * FROM products WHERE metadata->>'color' = 'titanium';

-- =====================================================
-- BUSINESS LOGIC VALIDATION
-- =====================================================

-- Test 14: Verify order total calculation
SELECT 
    o.id as order_id,
    o.total_amount as order_total,
    COALESCE(SUM(oi.total_price), 0) as calculated_total,
    CASE 
        WHEN o.total_amount = COALESCE(SUM(oi.total_price), 0) THEN 'PASS'
        ELSE 'FAIL'
    END as validation_result
FROM orders o
LEFT JOIN order_items oi ON o.id = oi.order_id
GROUP BY o.id, o.total_amount;

-- Test 15: Verify inventory reservation logic
SELECT 
    i.product_sku,
    i.available_quantity,
    i.reserved_quantity,
    i.total_quantity,
    CASE 
        WHEN i.available_quantity + i.reserved_quantity <= i.total_quantity THEN 'PASS'
        ELSE 'FAIL'
    END as quantity_validation
FROM inventory i;

-- Test 16: Verify outbox pattern
SELECT 
    COUNT(*) as total_events,
    COUNT(CASE WHEN status = 'PENDING' THEN 1 END) as pending_events,
    COUNT(CASE WHEN status = 'PROCESSED' THEN 1 END) as processed_events
FROM outbox_events;

-- Test 17: Verify idempotency
SELECT 
    COUNT(*) as total_keys,
    COUNT(DISTINCT key_value) as unique_keys,
    CASE 
        WHEN COUNT(*) = COUNT(DISTINCT key_value) THEN 'PASS'
        ELSE 'FAIL'
    END as uniqueness_validation
FROM idempotency_keys;

-- =====================================================
-- FINAL VALIDATION SUMMARY
-- =====================================================

SELECT 'SCHEMA VALIDATION COMPLETE' as status, CURRENT_TIMESTAMP as validated_at;
