-- Order Service Enhanced Database Schema
-- Comprehensive schema with business rules, constraints, and performance indexes
-- This schema enhances the existing basic orders and outbox tables

-- Enhanced Orders Table with Business Rules
CREATE TABLE IF NOT EXISTS orders (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' 
        CHECK (status IN ('PENDING', 'CONFIRMED', 'CANCELLED', 'SHIPPED', 'DELIVERED', 'REFUNDED')),
    total_amount DECIMAL(10,2) NOT NULL CHECK (total_amount >= 10.00 AND total_amount <= 10000.00),
    shipping_address JSONB NOT NULL, -- Structured address with validation
    billing_address JSONB NOT NULL, -- Structured address with validation
    payment_method VARCHAR(50) NOT NULL,
    payment_status VARCHAR(20) NOT NULL DEFAULT 'PENDING'
        CHECK (payment_status IN ('PENDING', 'AUTHORIZED', 'FAILED', 'REFUNDED')),
    idempotency_key VARCHAR(64) UNIQUE NOT NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'USD',
    tax_amount DECIMAL(10,2) NOT NULL DEFAULT 0.00 CHECK (tax_amount >= 0),
    shipping_amount DECIMAL(10,2) NOT NULL DEFAULT 0.00 CHECK (shipping_amount >= 0),
    discount_amount DECIMAL(10,2) NOT NULL DEFAULT 0.00 CHECK (discount_amount >= 0),
    subtotal_amount DECIMAL(10,2) NOT NULL CHECK (subtotal_amount > 0),
    notes TEXT,
    estimated_delivery_date DATE,
    actual_delivery_date DATE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP NOT NULL DEFAULT (CURRENT_TIMESTAMP + INTERVAL '30 minutes'),
    created_by UUID,
    updated_by UUID
);

-- Enhanced Order Items Table
CREATE TABLE IF NOT EXISTS order_items (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id UUID NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    product_id UUID NOT NULL,
    product_name VARCHAR(255) NOT NULL,
    product_sku VARCHAR(100),
    quantity INT NOT NULL CHECK (quantity > 0 AND quantity <= 50),
    unit_price DECIMAL(10,2) NOT NULL CHECK (unit_price > 0),
    total_price DECIMAL(10,2) NOT NULL CHECK (total_price > 0),
    tax_rate DECIMAL(5,4) NOT NULL DEFAULT 0.00 CHECK (tax_rate >= 0),
    tax_amount DECIMAL(10,2) NOT NULL DEFAULT 0.00 CHECK (tax_amount >= 0),
    discount_rate DECIMAL(5,4) NOT NULL DEFAULT 0.00 CHECK (discount_rate >= 0),
    discount_amount DECIMAL(10,2) NOT NULL DEFAULT 0.00 CHECK (discount_amount >= 0),
    weight_kg DECIMAL(5,2) CHECK (weight_kg >= 0),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Enhanced Outbox Events Table (replaces existing basic outbox)
CREATE TABLE IF NOT EXISTS outbox_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    aggregate_type VARCHAR(100) NOT NULL,
    aggregate_id VARCHAR(100) NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    payload TEXT NOT NULL,
    headers TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    sent_at TIMESTAMP,
    retry_count INT NOT NULL DEFAULT 0,
    max_retries INT NOT NULL DEFAULT 3,
    next_retry_at TIMESTAMP,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' 
        CHECK (status IN ('PENDING', 'SENT', 'FAILED', 'RETRY')),
    error_message TEXT,
    last_error_at TIMESTAMP,
    priority INT NOT NULL DEFAULT 0, -- Higher priority events processed first
    partition_key VARCHAR(100), -- For Kafka partitioning
    created_by UUID,
    updated_by UUID
);

-- Order Status History Table for Audit Trail
CREATE TABLE IF NOT EXISTS order_status_history (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id UUID NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    from_status VARCHAR(20),
    to_status VARCHAR(20) NOT NULL,
    reason TEXT,
    changed_by UUID,
    changed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    metadata JSONB -- Additional context about the status change
);

-- Order Notes Table for Customer Service
CREATE TABLE IF NOT EXISTS order_notes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id UUID NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    note_type VARCHAR(50) NOT NULL DEFAULT 'GENERAL'
        CHECK (note_type IN ('GENERAL', 'CUSTOMER_SERVICE', 'FULFILLMENT', 'PAYMENT', 'SYSTEM')),
    note TEXT NOT NULL,
    is_internal BOOLEAN NOT NULL DEFAULT FALSE, -- Internal notes not visible to customers
    created_by UUID NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Order Fulfillment Table for Shipping Information
CREATE TABLE IF NOT EXISTS order_fulfillment (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id UUID NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    tracking_number VARCHAR(100),
    carrier VARCHAR(100),
    service_level VARCHAR(50),
    shipped_at TIMESTAMP,
    delivered_at TIMESTAMP,
    shipping_label_url VARCHAR(500),
    package_weight_kg DECIMAL(5,2),
    package_dimensions_cm VARCHAR(50),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Enhanced Idempotency Keys Table
CREATE TABLE IF NOT EXISTS idempotency_keys (
    id VARCHAR(64) PRIMARY KEY,
    order_id UUID NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    user_id UUID NOT NULL,
    request_hash VARCHAR(64) NOT NULL, -- Hash of request body for validation
    response_hash VARCHAR(64), -- Hash of response for caching
    response_body TEXT, -- Cached response body
    expires_at TIMESTAMP NOT NULL DEFAULT (CURRENT_TIMESTAMP + INTERVAL '24 hours'),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_used_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    usage_count INT NOT NULL DEFAULT 1
);

-- Create comprehensive indexes for performance
CREATE INDEX IF NOT EXISTS idx_orders_user_id ON orders(user_id);
CREATE INDEX IF NOT EXISTS idx_orders_status ON orders(status);
CREATE INDEX IF NOT EXISTS idx_orders_payment_status ON orders(payment_status);
CREATE INDEX IF NOT EXISTS idx_orders_created_at ON orders(created_at);
CREATE INDEX IF NOT EXISTS idx_orders_updated_at ON orders(updated_at);
CREATE INDEX IF NOT EXISTS idx_orders_expires_at ON orders(expires_at);
CREATE INDEX IF NOT EXISTS idx_orders_idempotency_key ON orders(idempotency_key);
CREATE INDEX IF NOT EXISTS idx_orders_total_amount ON orders(total_amount);
CREATE INDEX IF NOT EXISTS idx_orders_currency ON orders(currency);

-- Composite indexes for common query patterns
CREATE INDEX IF NOT EXISTS idx_orders_user_status ON orders(user_id, status);
CREATE INDEX IF NOT EXISTS idx_orders_status_created ON orders(status, created_at);
CREATE INDEX IF NOT EXISTS idx_orders_payment_status_created ON orders(payment_status, created_at);
CREATE INDEX IF NOT EXISTS idx_orders_user_created ON orders(user_id, created_at);

-- Partial indexes for filtered queries
-- Note: Removed CURRENT_TIMESTAMP predicates as they are not IMMUTABLE in PostgreSQL
-- These indexes will be created without predicates for better compatibility
CREATE INDEX IF NOT EXISTS idx_orders_pending ON orders(id, status, expires_at) 
    WHERE status = 'PENDING';
CREATE INDEX IF NOT EXISTS idx_orders_confirmed ON orders(id, status, created_at) 
    WHERE status = 'CONFIRMED';

-- Order items indexes
CREATE INDEX IF NOT EXISTS idx_order_items_order_id ON order_items(order_id);
CREATE INDEX IF NOT EXISTS idx_order_items_product_id ON order_items(product_id);
CREATE INDEX IF NOT EXISTS idx_order_items_quantity ON order_items(quantity);
CREATE INDEX IF NOT EXISTS idx_order_items_unit_price ON order_items(unit_price);

-- Outbox events indexes
CREATE INDEX IF NOT EXISTS idx_outbox_status ON outbox_events(status);
CREATE INDEX IF NOT EXISTS idx_outbox_created_at ON outbox_events(created_at);
CREATE INDEX IF NOT EXISTS idx_outbox_aggregate ON outbox_events(aggregate_type, aggregate_id);
CREATE INDEX IF NOT EXISTS idx_outbox_event_type ON outbox_events(event_type);
CREATE INDEX IF NOT EXISTS idx_outbox_retry ON outbox_events(status, next_retry_at) WHERE status = 'RETRY';
CREATE INDEX IF NOT EXISTS idx_outbox_priority ON outbox_events(priority, created_at);
CREATE INDEX IF NOT EXISTS idx_outbox_partition ON outbox_events(partition_key, status);

-- Order status history indexes
CREATE INDEX IF NOT EXISTS idx_status_history_order_id ON order_status_history(order_id);
CREATE INDEX IF NOT EXISTS idx_status_history_changed_at ON order_status_history(changed_at);
CREATE INDEX IF NOT EXISTS idx_status_history_to_status ON order_status_history(to_status);

-- Order notes indexes
CREATE INDEX IF NOT EXISTS idx_order_notes_order_id ON order_notes(order_id);
CREATE INDEX IF NOT EXISTS idx_order_notes_type ON order_notes(note_type);
CREATE INDEX IF NOT EXISTS idx_order_notes_created_at ON order_notes(created_at);
CREATE INDEX IF NOT EXISTS idx_order_notes_internal ON order_notes(is_internal);

-- Order fulfillment indexes
CREATE INDEX IF NOT EXISTS idx_fulfillment_order_id ON order_fulfillment(order_id);
CREATE INDEX IF NOT EXISTS idx_fulfillment_tracking ON order_fulfillment(tracking_number);
CREATE INDEX IF NOT EXISTS idx_fulfillment_carrier ON order_fulfillment(carrier);

-- Idempotency keys indexes
CREATE INDEX IF NOT EXISTS idx_idempotency_user_id ON idempotency_keys(user_id);
CREATE INDEX IF NOT EXISTS idx_idempotency_expires_at ON idempotency_keys(expires_at);
CREATE INDEX IF NOT EXISTS idx_idempotency_request_hash ON idempotency_keys(request_hash);

-- Add comments for documentation
COMMENT ON TABLE orders IS 'Core orders table with comprehensive business rules and validation';
COMMENT ON TABLE order_items IS 'Order line items with product details and pricing';
COMMENT ON TABLE outbox_events IS 'Enhanced outbox pattern for reliable event publishing';
COMMENT ON TABLE order_status_history IS 'Complete audit trail of order status changes';
COMMENT ON TABLE order_notes IS 'Customer service and internal notes for orders';
COMMENT ON TABLE order_fulfillment IS 'Shipping and delivery information for orders';
COMMENT ON TABLE idempotency_keys IS 'Idempotency key management with request validation';

-- Create updated_at trigger function (if not exists)
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

-- Create triggers for updated_at columns
CREATE TRIGGER update_orders_updated_at BEFORE UPDATE ON orders
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_order_items_updated_at BEFORE UPDATE ON order_items
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_outbox_events_updated_at BEFORE UPDATE ON outbox_events
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_order_notes_updated_at BEFORE UPDATE ON order_notes
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_order_fulfillment_updated_at BEFORE UPDATE ON order_fulfillment
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- Create function to validate order business rules
CREATE OR REPLACE FUNCTION validate_order_business_rules(
    p_total_amount DECIMAL,
    p_item_count INT,
    p_user_id UUID
)
RETURNS BOOLEAN AS $$
BEGIN
    -- Check minimum order amount
    IF p_total_amount < 10.00 THEN
        RAISE EXCEPTION 'Order total must be at least $10.00';
    END IF;
    
    -- Check maximum order amount
    IF p_total_amount > 10000.00 THEN
        RAISE EXCEPTION 'Order total cannot exceed $10,000.00';
    END IF;
    
    -- Check maximum items per order
    IF p_item_count > 50 THEN
        RAISE EXCEPTION 'Order cannot contain more than 50 items';
    END IF;
    
    -- Check user order limit (max 5 pending orders)
    IF (SELECT COUNT(*) FROM orders WHERE user_id = p_user_id AND status = 'PENDING') >= 5 THEN
        RAISE EXCEPTION 'User cannot have more than 5 pending orders';
    END IF;
    
    RETURN TRUE;
END;
$$ LANGUAGE plpgsql;

-- Create function to get order with full details
CREATE OR REPLACE FUNCTION get_order_details(order_uuid UUID)
RETURNS TABLE (
    id UUID,
    user_id UUID,
    status VARCHAR(20),
    total_amount DECIMAL(10,2),
    shipping_address JSONB,
    billing_address JSONB,
    payment_method VARCHAR(50),
    payment_status VARCHAR(20),
    currency VARCHAR(3),
    tax_amount DECIMAL(10,2),
    shipping_amount DECIMAL(10,2),
    discount_amount DECIMAL(10,2),
    subtotal_amount DECIMAL(10,2),
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    expires_at TIMESTAMP,
    item_count BIGINT,
    tracking_number VARCHAR(100),
    carrier VARCHAR(100),
    estimated_delivery_date DATE
) AS $$
BEGIN
    RETURN QUERY
    SELECT 
        o.id,
        o.user_id,
        o.status,
        o.total_amount,
        o.shipping_address,
        o.billing_address,
        o.payment_method,
        o.payment_status,
        o.currency,
        o.tax_amount,
        o.shipping_amount,
        o.discount_amount,
        o.subtotal_amount,
        o.created_at,
        o.updated_at,
        o.expires_at,
        COUNT(oi.id) as item_count,
        of.tracking_number,
        of.carrier,
        of.estimated_delivery_date
    FROM orders o
    LEFT JOIN order_items oi ON o.id = oi.order_id
    LEFT JOIN order_fulfillment of ON o.id = of.order_id
    WHERE o.id = order_uuid
    GROUP BY o.id, o.user_id, o.status, o.total_amount, o.shipping_address, 
             o.billing_address, o.payment_method, o.payment_status, o.currency,
             o.tax_amount, o.shipping_amount, o.discount_amount, o.subtotal_amount,
             o.created_at, o.updated_at, o.expires_at, of.tracking_number, 
             of.carrier, of.estimated_delivery_date;
END;
$$ LANGUAGE plpgsql;

-- Create function to clean up expired orders
CREATE OR REPLACE FUNCTION cleanup_expired_orders()
RETURNS INT AS $$
DECLARE
    cleaned_count INT := 0;
BEGIN
    -- Cancel expired pending orders
    UPDATE orders 
    SET status = 'CANCELLED', 
        updated_at = CURRENT_TIMESTAMP,
        notes = COALESCE(notes, '') || E'\nOrder automatically cancelled due to payment timeout'
    WHERE status = 'PENDING' 
      AND expires_at < CURRENT_TIMESTAMP
      AND payment_status = 'PENDING';
    
    GET DIAGNOSTICS cleaned_count = ROW_COUNT;
    
    -- Log the cleanup
    INSERT INTO order_notes (order_id, note_type, note, is_internal, created_by)
    SELECT 
        id, 
        'SYSTEM', 
        'Order automatically cancelled due to payment timeout', 
        TRUE, 
        '00000000-0000-0000-0000-000000000000'::UUID
    FROM orders 
    WHERE status = 'CANCELLED' 
      AND updated_at = CURRENT_TIMESTAMP
      AND notes LIKE '%automatically cancelled%';
    
    RETURN cleaned_count;
END;
$$ LANGUAGE plpgsql;

-- Create function to get outbox events ready for processing
CREATE OR REPLACE FUNCTION get_outbox_events_for_processing(
    p_batch_size INT DEFAULT 100,
    p_max_priority INT DEFAULT 10
)
RETURNS TABLE (
    id UUID,
    aggregate_type VARCHAR(100),
    aggregate_id VARCHAR(100),
    event_type VARCHAR(100),
    payload TEXT,
    headers TEXT,
    priority INT,
    partition_key VARCHAR(100)
) AS $$
BEGIN
    RETURN QUERY
    SELECT 
        oe.id,
        oe.aggregate_type,
        oe.aggregate_id,
        oe.event_type,
        oe.payload,
        oe.headers,
        oe.priority,
        oe.partition_key
    FROM outbox_events oe
    WHERE oe.status = 'PENDING'
      AND oe.priority <= p_max_priority
      AND (oe.next_retry_at IS NULL OR oe.next_retry_at <= CURRENT_TIMESTAMP)
    ORDER BY oe.priority DESC, oe.created_at ASC
    LIMIT p_batch_size;
END;
$$ LANGUAGE plpgsql;
