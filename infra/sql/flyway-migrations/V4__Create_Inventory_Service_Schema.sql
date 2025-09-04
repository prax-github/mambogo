-- Flyway Migration V4: Create Inventory Service Schema
-- This migration creates the complete Inventory Service database schema

-- Core Inventory Table
CREATE TABLE IF NOT EXISTS inventory (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    product_id UUID NOT NULL UNIQUE,
    product_name VARCHAR(255) NOT NULL,
    product_sku VARCHAR(100) NOT NULL UNIQUE,
    available_quantity INT NOT NULL DEFAULT 0 CHECK (available_quantity >= 0),
    reserved_quantity INT NOT NULL DEFAULT 0 CHECK (reserved_quantity >= 0),
    total_quantity INT NOT NULL DEFAULT 0 CHECK (total_quantity >= 0),
    reorder_point INT NOT NULL DEFAULT 10 CHECK (reorder_point >= 0),
    reorder_quantity INT NOT NULL DEFAULT 50 CHECK (reorder_quantity > 0),
    max_stock_level INT CHECK (max_stock_level > 0),
    unit_cost DECIMAL(10,2) CHECK (unit_cost > 0),
    currency VARCHAR(3) NOT NULL DEFAULT 'USD',
    supplier_id UUID,
    supplier_name VARCHAR(255),
    last_restocked_at TIMESTAMP,
    next_restock_date DATE,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by UUID,
    updated_by UUID,
    -- Business rule: available + reserved cannot exceed total
    CONSTRAINT chk_inventory_quantities CHECK (available_quantity + reserved_quantity <= total_quantity)
);

-- Inventory Reservations Table for Order Processing
CREATE TABLE IF NOT EXISTS inventory_reservations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    product_id UUID NOT NULL REFERENCES inventory(product_id) ON DELETE CASCADE,
    order_id UUID NOT NULL,
    user_id UUID NOT NULL,
    quantity INT NOT NULL CHECK (quantity > 0),
    status VARCHAR(20) NOT NULL DEFAULT 'RESERVED'
        CHECK (status IN ('RESERVED', 'CONFIRMED', 'RELEASED', 'EXPIRED', 'CANCELLED')),
    reservation_type VARCHAR(50) NOT NULL DEFAULT 'ORDER'
        CHECK (reservation_type IN ('ORDER', 'HOLD', 'PRE_ORDER', 'BACKORDER')),
    expires_at TIMESTAMP NOT NULL DEFAULT (CURRENT_TIMESTAMP + INTERVAL '5 minutes'),
    confirmed_at TIMESTAMP,
    released_at TIMESTAMP,
    notes TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by UUID,
    updated_by UUID,
    -- Business rule: unique reservation per order-product combination
    UNIQUE(order_id, product_id)
);

-- Inventory Movements Table for Audit Trail
CREATE TABLE IF NOT EXISTS inventory_movements (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    product_id UUID NOT NULL REFERENCES inventory(product_id) ON DELETE CASCADE,
    movement_type VARCHAR(50) NOT NULL
        CHECK (movement_type IN ('RESTOCK', 'SALE', 'RESERVATION', 'RELEASE', 'ADJUSTMENT', 'DAMAGE', 'RETURN')),
    quantity INT NOT NULL, -- Positive for additions, negative for reductions
    previous_available INT NOT NULL,
    previous_reserved INT NOT NULL,
    previous_total INT NOT NULL,
    new_available INT NOT NULL,
    new_reserved INT NOT NULL,
    new_total INT NOT NULL,
    reference_id UUID, -- Order ID, reservation ID, etc.
    reference_type VARCHAR(50), -- ORDER, RESERVATION, etc.
    reason TEXT,
    unit_cost DECIMAL(10,2),
    currency VARCHAR(3) NOT NULL DEFAULT 'USD',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by UUID NOT NULL
);

-- Inventory Alerts Table for Stock Notifications
CREATE TABLE IF NOT EXISTS inventory_alerts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    product_id UUID NOT NULL REFERENCES inventory(product_id) ON DELETE CASCADE,
    alert_type VARCHAR(50) NOT NULL
        CHECK (alert_type IN ('LOW_STOCK', 'OUT_OF_STOCK', 'OVERSTOCK', 'REORDER_DUE', 'EXPIRING_SOON')),
    severity VARCHAR(20) NOT NULL DEFAULT 'MEDIUM'
        CHECK (severity IN ('LOW', 'MEDIUM', 'HIGH', 'CRITICAL')),
    message TEXT NOT NULL,
    is_resolved BOOLEAN NOT NULL DEFAULT FALSE,
    resolved_at TIMESTAMP,
    resolved_by UUID,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by UUID
);

-- Inventory Suppliers Table
CREATE TABLE IF NOT EXISTS inventory_suppliers (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    contact_person VARCHAR(255),
    email VARCHAR(255),
    phone VARCHAR(50),
    address JSONB,
    payment_terms VARCHAR(100),
    lead_time_days INT CHECK (lead_time_days >= 0),
    minimum_order_quantity INT CHECK (minimum_order_quantity > 0),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    rating DECIMAL(3,2) CHECK (rating >= 0 AND rating <= 5),
    notes TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by UUID,
    updated_by UUID
);

-- Inventory Categories Table for Organization
CREATE TABLE IF NOT EXISTS inventory_categories (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(100) NOT NULL,
    description TEXT,
    parent_category_id UUID REFERENCES inventory_categories(id),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    display_order INT DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by UUID,
    updated_by UUID
);

-- Inventory Analytics Table for Business Intelligence
CREATE TABLE IF NOT EXISTS inventory_analytics (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    date DATE NOT NULL,
    product_id UUID NOT NULL REFERENCES inventory(product_id) ON DELETE CASCADE,
    category_id UUID REFERENCES inventory_categories(id),
    beginning_available INT NOT NULL DEFAULT 0,
    beginning_reserved INT NOT NULL DEFAULT 0,
    beginning_total INT NOT NULL DEFAULT 0,
    restocked_quantity INT NOT NULL DEFAULT 0,
    sold_quantity INT NOT NULL DEFAULT 0,
    reserved_quantity INT NOT NULL DEFAULT 0,
    released_quantity INT NOT NULL DEFAULT 0,
    ending_available INT NOT NULL DEFAULT 0,
    ending_reserved INT NOT NULL DEFAULT 0,
    ending_total INT NOT NULL DEFAULT 0,
    stock_turnover_rate DECIMAL(5,2),
    days_of_inventory DECIMAL(5,2),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(date, product_id)
);

-- Create comprehensive indexes for performance
CREATE INDEX IF NOT EXISTS idx_inventory_product_id ON inventory(product_id);
CREATE INDEX IF NOT EXISTS idx_inventory_sku ON inventory(product_sku);
CREATE INDEX IF NOT EXISTS idx_inventory_supplier ON inventory(supplier_id);
CREATE INDEX IF NOT EXISTS idx_inventory_active ON inventory(is_active);
CREATE INDEX IF NOT EXISTS idx_inventory_available ON inventory(available_quantity);
CREATE INDEX IF NOT EXISTS idx_inventory_reserved ON inventory(reserved_quantity);
CREATE INDEX IF NOT EXISTS idx_inventory_reorder_point ON inventory(reorder_point);
CREATE INDEX IF NOT EXISTS idx_inventory_last_restocked ON inventory(last_restocked_at);
CREATE INDEX IF NOT EXISTS idx_inventory_next_restock ON inventory(next_restock_date);

-- Composite indexes for common query patterns
CREATE INDEX IF NOT EXISTS idx_inventory_available_active ON inventory(available_quantity, is_active);
CREATE INDEX IF NOT EXISTS idx_inventory_supplier_active ON inventory(supplier_id, is_active);
CREATE INDEX IF NOT EXISTS idx_inventory_reorder_active ON inventory(reorder_point, available_quantity, is_active);

-- Partial indexes for filtered queries
CREATE INDEX IF NOT EXISTS idx_inventory_low_stock ON inventory(id, available_quantity) 
    WHERE available_quantity <= reorder_point AND is_active = TRUE;
CREATE INDEX IF NOT EXISTS idx_inventory_out_of_stock ON inventory(id) 
    WHERE available_quantity = 0 AND is_active = TRUE;
CREATE INDEX IF NOT EXISTS idx_inventory_overstock ON inventory(id, available_quantity) 
    WHERE available_quantity > max_stock_level AND max_stock_level IS NOT NULL;

-- Inventory reservations indexes
CREATE INDEX IF NOT EXISTS idx_reservations_product_id ON inventory_reservations(product_id);
CREATE INDEX IF NOT EXISTS idx_reservations_order_id ON inventory_reservations(order_id);
CREATE INDEX IF NOT EXISTS idx_reservations_user_id ON inventory_reservations(user_id);
CREATE INDEX IF NOT EXISTS idx_reservations_status ON inventory_reservations(status);
CREATE INDEX IF NOT EXISTS idx_reservations_expires_at ON inventory_reservations(expires_at);
CREATE INDEX IF NOT EXISTS idx_reservations_type ON inventory_reservations(reservation_type);
CREATE INDEX IF NOT EXISTS idx_reservations_created_at ON inventory_reservations(created_at);

-- Composite indexes for reservations
CREATE INDEX IF NOT EXISTS idx_reservations_product_status ON inventory_reservations(product_id, status);
CREATE INDEX IF NOT EXISTS idx_reservations_order_status ON inventory_reservations(order_id, status);
CREATE INDEX IF NOT EXISTS idx_reservations_expired ON inventory_reservations(status, expires_at) 
    WHERE status = 'RESERVED';

-- Inventory movements indexes
CREATE INDEX IF NOT EXISTS idx_movements_product_id ON inventory_movements(product_id);
CREATE INDEX IF NOT EXISTS idx_movements_type ON inventory_movements(movement_type);
CREATE INDEX IF NOT EXISTS idx_movements_reference ON inventory_movements(reference_id, reference_type);
CREATE INDEX IF NOT EXISTS idx_movements_created_at ON inventory_movements(created_at);
CREATE INDEX IF NOT EXISTS idx_movements_date_type ON inventory_movements(created_at, movement_type);

-- Inventory alerts indexes
CREATE INDEX IF NOT EXISTS idx_alerts_product_id ON inventory_alerts(product_id);
CREATE INDEX IF NOT EXISTS idx_alerts_type ON inventory_alerts(alert_type);
CREATE INDEX IF NOT EXISTS idx_alerts_severity ON inventory_alerts(severity);
CREATE INDEX IF NOT EXISTS idx_alerts_resolved ON inventory_alerts(is_resolved);
CREATE INDEX IF NOT EXISTS idx_alerts_created_at ON inventory_alerts(created_at);

-- Partial indexes for alerts
CREATE INDEX IF NOT EXISTS idx_alerts_unresolved ON inventory_alerts(id, severity) 
    WHERE is_resolved = FALSE;

-- Suppliers indexes
CREATE INDEX IF NOT EXISTS idx_suppliers_name ON inventory_suppliers(name);
CREATE INDEX IF NOT EXISTS idx_suppliers_active ON inventory_suppliers(is_active);
CREATE INDEX IF NOT EXISTS idx_suppliers_rating ON inventory_suppliers(rating);

-- Categories indexes
CREATE INDEX IF NOT EXISTS idx_categories_parent ON inventory_categories(parent_category_id);
CREATE INDEX IF NOT EXISTS idx_categories_active ON inventory_categories(is_active);
CREATE INDEX IF NOT EXISTS idx_categories_display_order ON inventory_categories(display_order);

-- Analytics indexes
CREATE INDEX IF NOT EXISTS idx_analytics_date ON inventory_analytics(date);
CREATE INDEX IF NOT EXISTS idx_analytics_product ON inventory_analytics(product_id);
CREATE INDEX IF NOT EXISTS idx_analytics_category ON inventory_analytics(category_id);
CREATE INDEX IF NOT EXISTS idx_analytics_date_product ON inventory_analytics(date, product_id);

-- Add comments for documentation
COMMENT ON TABLE inventory IS 'Core inventory table with stock levels and reorder management';
COMMENT ON TABLE inventory_reservations IS 'Inventory reservations for order processing and holds';
COMMENT ON TABLE inventory_movements IS 'Complete audit trail of inventory changes';
COMMENT ON TABLE inventory_alerts IS 'Stock alerts and notifications for inventory management';
COMMENT ON TABLE inventory_suppliers IS 'Supplier information and performance tracking';
COMMENT ON TABLE inventory_categories IS 'Inventory categorization for organization';
COMMENT ON TABLE inventory_analytics IS 'Business intelligence data for inventory performance';

-- Create updated_at trigger function (if not exists)
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

-- Create triggers for updated_at columns
CREATE TRIGGER update_inventory_updated_at BEFORE UPDATE ON inventory
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_reservations_updated_at BEFORE UPDATE ON inventory_reservations
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_alerts_updated_at BEFORE UPDATE ON inventory_alerts
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_suppliers_updated_at BEFORE UPDATE ON inventory_suppliers
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_categories_updated_at BEFORE UPDATE ON inventory_categories
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_analytics_updated_at BEFORE UPDATE ON inventory_analytics
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- Create function to reserve inventory
CREATE OR REPLACE FUNCTION reserve_inventory(
    p_product_id UUID,
    p_order_id UUID,
    p_user_id UUID,
    p_quantity INT,
    p_reservation_type VARCHAR(50) DEFAULT 'ORDER',
    p_expires_in_minutes INT DEFAULT 5
)
RETURNS BOOLEAN AS $$
DECLARE
    v_available_quantity INT;
    v_current_reserved INT;
    v_expires_at TIMESTAMP;
BEGIN
    -- Get current inventory levels
    SELECT available_quantity, reserved_quantity 
    INTO v_available_quantity, v_current_reserved
    FROM inventory 
    WHERE product_id = p_product_id AND is_active = TRUE;
    
    -- Check if product exists and has sufficient stock
    IF v_available_quantity IS NULL THEN
        RAISE EXCEPTION 'Product not found in inventory';
    END IF;
    
    IF v_available_quantity < p_quantity THEN
        RAISE EXCEPTION 'Insufficient available stock. Available: %, Requested: %', v_available_quantity, p_quantity;
    END IF;
    
    -- Check if user already has active reservations for this product
    IF (SELECT COUNT(*) FROM inventory_reservations 
        WHERE user_id = p_user_id AND product_id = p_product_id AND status = 'RESERVED') >= 5 THEN
        RAISE EXCEPTION 'User cannot have more than 5 active reservations for the same product';
    END IF;
    
    -- Calculate expiration time
    v_expires_at := CURRENT_TIMESTAMP + (p_expires_in_minutes || ' minutes')::INTERVAL;
    
    -- Create reservation
    INSERT INTO inventory_reservations (
        product_id, order_id, user_id, quantity, reservation_type, expires_at
    ) VALUES (
        p_product_id, p_order_id, p_user_id, p_quantity, p_reservation_type, v_expires_at
    );
    
    -- Update inventory levels
    UPDATE inventory 
    SET available_quantity = available_quantity - p_quantity,
        reserved_quantity = reserved_quantity + p_quantity,
        updated_at = CURRENT_TIMESTAMP
    WHERE product_id = p_product_id;
    
    -- Record movement
    INSERT INTO inventory_movements (
        product_id, movement_type, quantity, 
        previous_available, previous_reserved, previous_total,
        new_available, new_reserved, new_total,
        reference_id, reference_type, reason, created_by
    ) VALUES (
        p_product_id, 'RESERVATION', -p_quantity,
        v_available_quantity, v_current_reserved, v_available_quantity + v_current_reserved,
        v_available_quantity - p_quantity, v_current_reserved + p_quantity, v_available_quantity + v_current_reserved,
        p_order_id, 'ORDER', 'Inventory reserved for order', p_user_id
    );
    
    RETURN TRUE;
END;
$$ LANGUAGE plpgsql;

-- Create function to release inventory reservation
CREATE OR REPLACE FUNCTION release_inventory_reservation(
    p_reservation_id UUID,
    p_reason TEXT DEFAULT 'Manual release'
)
RETURNS BOOLEAN AS $$
DECLARE
    v_product_id UUID;
    v_quantity INT;
    v_current_available INT;
    v_current_reserved INT;
    v_current_total INT;
BEGIN
    -- Get reservation details
    SELECT product_id, quantity 
    INTO v_product_id, v_quantity
    FROM inventory_reservations 
    WHERE id = p_reservation_id AND status = 'RESERVED';
    
    IF v_product_id IS NULL THEN
        RAISE EXCEPTION 'Reservation not found or not in RESERVED status';
    END IF;
    
    -- Get current inventory levels
    SELECT available_quantity, reserved_quantity, total_quantity
    INTO v_current_available, v_current_reserved, v_current_total
    FROM inventory 
    WHERE product_id = v_product_id;
    
    -- Update reservation status
    UPDATE inventory_reservations 
    SET status = 'RELEASED',
        released_at = CURRENT_TIMESTAMP,
        notes = COALESCE(notes, '') || E'\n' || p_reason,
        updated_at = CURRENT_TIMESTAMP
    WHERE id = p_reservation_id;
    
    -- Update inventory levels
    UPDATE inventory 
    SET available_quantity = available_quantity + v_quantity,
        reserved_quantity = reserved_quantity - v_quantity,
        updated_at = CURRENT_TIMESTAMP
    WHERE product_id = v_product_id;
    
    -- Record movement
    INSERT INTO inventory_movements (
        product_id, movement_type, quantity, 
        previous_available, previous_reserved, previous_total,
        new_available, new_reserved, new_total,
        reference_id, reference_type, reason, created_by
    ) VALUES (
        v_product_id, 'RELEASE', v_quantity,
        v_current_available, v_current_reserved, v_current_total,
        v_current_available + v_quantity, v_current_reserved - v_quantity, v_current_total,
        p_reservation_id, 'RESERVATION', p_reason, '00000000-0000-0000-0000-000000000000'::UUID
    );
    
    RETURN TRUE;
END;
$$ LANGUAGE plpgsql;

-- Create function to confirm inventory reservation
CREATE OR REPLACE FUNCTION confirm_inventory_reservation(
    p_reservation_id UUID
)
RETURNS BOOLEAN AS $$
DECLARE
    v_product_id UUID;
    v_quantity INT;
    v_current_available INT;
    v_current_reserved INT;
    v_current_total INT;
BEGIN
    -- Get reservation details
    SELECT product_id, quantity 
    INTO v_product_id, v_quantity
    FROM inventory_reservations 
    WHERE id = p_reservation_id AND status = 'RESERVED';
    
    IF v_product_id IS NULL THEN
        RAISE EXCEPTION 'Reservation not found or not in RESERVED status';
    END IF;
    
    -- Get current inventory levels
    SELECT available_quantity, reserved_quantity, total_quantity
    INTO v_current_available, v_current_reserved, v_current_total
    FROM inventory 
    WHERE product_id = v_product_id;
    
    -- Update reservation status
    UPDATE inventory_reservations 
    SET status = 'CONFIRMED',
        confirmed_at = CURRENT_TIMESTAMP,
        updated_at = CURRENT_TIMESTAMP
    WHERE id = p_reservation_id;
    
    -- Record movement (no change in quantities, just status change)
    INSERT INTO inventory_movements (
        product_id, movement_type, quantity, 
        previous_available, previous_reserved, previous_total,
        new_available, new_reserved, new_total,
        reference_id, reference_type, reason, created_by
    ) VALUES (
        v_product_id, 'CONFIRMATION', 0,
        v_current_available, v_current_reserved, v_current_total,
        v_current_available, v_current_reserved, v_current_total,
        p_reservation_id, 'RESERVATION', 'Reservation confirmed', '00000000-0000-0000-0000-000000000000'::UUID
    );
    
    RETURN TRUE;
END;
$$ LANGUAGE plpgsql;

-- Create function to process inventory sale
CREATE OR REPLACE FUNCTION process_inventory_sale(
    p_product_id UUID,
    p_order_id UUID,
    p_quantity INT,
    p_unit_cost DECIMAL(10,2),
    p_created_by UUID
)
RETURNS BOOLEAN AS $$
DECLARE
    v_current_available INT;
    v_current_reserved INT;
    v_current_total INT;
BEGIN
    -- Get current inventory levels
    SELECT available_quantity, reserved_quantity, total_quantity
    INTO v_current_available, v_current_reserved, v_current_total
    FROM inventory 
    WHERE product_id = p_product_id AND is_active = TRUE;
    
    -- Check if product exists and has sufficient stock
    IF v_current_available IS NULL THEN
        RAISE EXCEPTION 'Product not found in inventory';
    END IF;
    
    IF v_current_available < p_quantity THEN
        RAISE EXCEPTION 'Insufficient available stock. Available: %, Requested: %', v_current_available, p_quantity;
    END IF;
    
    -- Update inventory levels
    UPDATE inventory 
    SET available_quantity = available_quantity - p_quantity,
        total_quantity = total_quantity - p_quantity,
        updated_at = CURRENT_TIMESTAMP
    WHERE product_id = p_product_id;
    
    -- Record movement
    INSERT INTO inventory_movements (
        product_id, movement_type, quantity, 
        previous_available, previous_reserved, previous_total,
        new_available, new_reserved, new_total,
        reference_id, reference_type, reason, unit_cost, created_by
    ) VALUES (
        p_product_id, 'SALE', -p_quantity,
        v_current_available, v_current_reserved, v_current_total,
        v_current_available - p_quantity, v_current_reserved, v_current_total - p_quantity,
        p_order_id, 'ORDER', 'Inventory sold', p_unit_cost, p_created_by
    );
    
    -- Check if reorder point reached
    IF (v_current_available - p_quantity) <= (SELECT reorder_point FROM inventory WHERE product_id = p_product_id) THEN
        INSERT INTO inventory_alerts (
            product_id, alert_type, severity, message, created_by
        ) VALUES (
            p_product_id, 'LOW_STOCK', 'HIGH', 
            'Product stock has reached reorder point', p_created_by
        );
    END IF;
    
    RETURN TRUE;
END;
$$ LANGUAGE plpgsql;

-- Create function to restock inventory
CREATE OR REPLACE FUNCTION restock_inventory(
    p_product_id UUID,
    p_quantity INT,
    p_unit_cost DECIMAL(10,2),
    p_supplier_id UUID,
    p_created_by UUID
)
RETURNS BOOLEAN AS $$
DECLARE
    v_current_available INT;
    v_current_reserved INT;
    v_current_total INT;
BEGIN
    -- Get current inventory levels
    SELECT available_quantity, reserved_quantity, total_quantity
    INTO v_current_available, v_current_reserved, v_current_total
    FROM inventory 
    WHERE product_id = p_product_id AND is_active = TRUE;
    
    -- Check if product exists
    IF v_current_available IS NULL THEN
        RAISE EXCEPTION 'Product not found in inventory';
    END IF;
    
    -- Update inventory levels
    UPDATE inventory 
    SET available_quantity = available_quantity + p_quantity,
        total_quantity = total_quantity + p_quantity,
        last_restocked_at = CURRENT_TIMESTAMP,
        unit_cost = p_unit_cost,
        supplier_id = p_supplier_id,
        updated_at = CURRENT_TIMESTAMP
    WHERE product_id = p_product_id;
    
    -- Record movement
    INSERT INTO inventory_movements (
        product_id, movement_type, quantity, 
        previous_available, previous_reserved, previous_total,
        new_available, new_reserved, new_total,
        reference_id, reference_type, reason, unit_cost, created_by
    ) VALUES (
        p_product_id, 'RESTOCK', p_quantity,
        v_current_available, v_current_reserved, v_current_total,
        v_current_available + p_quantity, v_current_reserved, v_current_total + p_quantity,
        p_supplier_id, 'SUPPLIER', 'Inventory restocked', p_unit_cost, p_created_by
    );
    
    -- Resolve low stock alerts
    UPDATE inventory_alerts 
    SET is_resolved = TRUE,
        resolved_at = CURRENT_TIMESTAMP,
        resolved_by = p_created_by
    WHERE product_id = p_product_id 
      AND alert_type IN ('LOW_STOCK', 'OUT_OF_STOCK')
      AND is_resolved = FALSE;
    
    RETURN TRUE;
END;
$$ LANGUAGE plpgsql;

-- Create function to clean up expired reservations
CREATE OR REPLACE FUNCTION cleanup_expired_reservations()
RETURNS INT AS $$
DECLARE
    v_reservation RECORD;
    v_cleaned_count INT := 0;
BEGIN
    -- Process expired reservations
    FOR v_reservation IN 
        SELECT id, product_id, quantity 
        FROM inventory_reservations 
        WHERE status = 'RESERVED' AND expires_at < CURRENT_TIMESTAMP
    LOOP
        -- Release the expired reservation
        PERFORM release_inventory_reservation(
            v_reservation.id, 
            'Reservation automatically expired and released'
        );
        v_cleaned_count := v_cleaned_count + 1;
    END LOOP;
    
    RETURN v_cleaned_count;
END;
$$ LANGUAGE plpgsql;

-- Create function to get inventory analytics for a date range
CREATE OR REPLACE FUNCTION get_inventory_analytics(
    p_start_date DATE,
    p_end_date DATE,
    p_category_id UUID DEFAULT NULL
)
RETURNS TABLE (
    date DATE,
    product_id UUID,
    product_name VARCHAR(255),
    category_name VARCHAR(100),
    beginning_available INT,
    beginning_reserved INT,
    beginning_total INT,
    restocked_quantity INT,
    sold_quantity INT,
    reserved_quantity INT,
    released_quantity INT,
    ending_available INT,
    ending_reserved INT,
    ending_total INT,
    stock_turnover_rate DECIMAL(5,2),
    days_of_inventory DECIMAL(5,2)
) AS $$
BEGIN
    RETURN QUERY
    SELECT 
        ia.date,
        ia.product_id,
        i.product_name,
        ic.name as category_name,
        ia.beginning_available,
        ia.beginning_reserved,
        ia.beginning_total,
        ia.restocked_quantity,
        ia.sold_quantity,
        ia.reserved_quantity,
        ia.released_quantity,
        ia.ending_available,
        ia.ending_reserved,
        ia.ending_total,
        ia.stock_turnover_rate,
        ia.days_of_inventory
    FROM inventory_analytics ia
    JOIN inventory i ON ia.product_id = i.id
    LEFT JOIN inventory_categories ic ON ia.category_id = ic.id
    WHERE ia.date BETWEEN p_start_date AND p_end_date
      AND (p_category_id IS NULL OR ia.category_id = p_category_id)
    ORDER BY ia.date DESC, i.product_name;
END;
$$ LANGUAGE plpgsql;
