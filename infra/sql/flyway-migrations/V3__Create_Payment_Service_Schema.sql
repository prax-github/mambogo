-- Flyway Migration V3: Create Payment Service Schema
-- This migration creates the complete Payment Service database schema

-- Core Payments Table
CREATE TABLE IF NOT EXISTS payments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id UUID NOT NULL,
    user_id UUID NOT NULL,
    amount DECIMAL(10,2) NOT NULL CHECK (amount > 0),
    currency VARCHAR(3) NOT NULL DEFAULT 'USD',
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING'
        CHECK (status IN ('PENDING', 'AUTHORIZED', 'CAPTURED', 'FAILED', 'REFUNDED', 'PARTIALLY_REFUNDED', 'CANCELLED')),
    payment_method VARCHAR(50) NOT NULL,
    payment_method_details JSONB, -- Flexible storage for different payment methods
    payment_reference VARCHAR(100) UNIQUE, -- External payment processor reference
    failure_reason TEXT,
    failure_code VARCHAR(50),
    processor_response JSONB, -- Raw response from payment processor
    metadata JSONB, -- Additional payment metadata
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    authorized_at TIMESTAMP,
    captured_at TIMESTAMP,
    failed_at TIMESTAMP,
    expires_at TIMESTAMP NOT NULL DEFAULT (CURRENT_TIMESTAMP + INTERVAL '30 minutes'),
    created_by UUID,
    updated_by UUID
);

-- Payment Methods Table for User Payment Preferences
CREATE TABLE IF NOT EXISTS payment_methods (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    payment_type VARCHAR(50) NOT NULL -- CREDIT_CARD, DEBIT_CARD, BANK_TRANSFER, etc.
        CHECK (payment_type IN ('CREDIT_CARD', 'DEBIT_CARD', 'BANK_TRANSFER', 'DIGITAL_WALLET', 'CRYPTO')),
    payment_provider VARCHAR(100) NOT NULL, -- Stripe, PayPal, etc.
    provider_account_id VARCHAR(100), -- External account identifier
    is_default BOOLEAN NOT NULL DEFAULT FALSE,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    last_used_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Payment Transactions Table for Detailed Transaction History
CREATE TABLE IF NOT EXISTS payment_transactions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    payment_id UUID NOT NULL REFERENCES payments(id) ON DELETE CASCADE,
    transaction_type VARCHAR(50) NOT NULL
        CHECK (transaction_type IN ('AUTHORIZATION', 'CAPTURE', 'REFUND', 'VOID', 'CHARGEBACK')),
    amount DECIMAL(10,2) NOT NULL CHECK (amount > 0),
    currency VARCHAR(3) NOT NULL DEFAULT 'USD',
    status VARCHAR(20) NOT NULL
        CHECK (status IN ('PENDING', 'SUCCESS', 'FAILED', 'CANCELLED')),
    processor_transaction_id VARCHAR(100), -- External transaction ID
    processor_response JSONB, -- Raw processor response
    error_code VARCHAR(50),
    error_message TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    processed_at TIMESTAMP,
    created_by UUID,
    updated_by UUID
);

-- Refunds Table for Refund Management
CREATE TABLE IF NOT EXISTS refunds (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    payment_id UUID NOT NULL REFERENCES payments(id) ON DELETE CASCADE,
    order_id UUID NOT NULL,
    amount DECIMAL(10,2) NOT NULL CHECK (amount > 0),
    currency VARCHAR(3) NOT NULL DEFAULT 'USD',
    reason VARCHAR(100) NOT NULL
        CHECK (reason IN ('CUSTOMER_REQUEST', 'DUPLICATE_CHARGE', 'FRAUD', 'SERVICE_NOT_RENDERED', 'DEFECTIVE_PRODUCT', 'OTHER')),
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING'
        CHECK (status IN ('PENDING', 'PROCESSING', 'COMPLETED', 'FAILED', 'CANCELLED')),
    refund_method VARCHAR(50) NOT NULL, -- How the refund was processed
    processor_refund_id VARCHAR(100), -- External refund ID
    notes TEXT,
    processed_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by UUID NOT NULL,
    updated_by UUID
);

-- Payment Disputes Table for Chargeback Management
CREATE TABLE IF NOT EXISTS payment_disputes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    payment_id UUID NOT NULL REFERENCES payments(id) ON DELETE CASCADE,
    dispute_type VARCHAR(50) NOT NULL
        CHECK (dispute_type IN ('FRAUD', 'DUPLICATE', 'PRODUCT_NOT_RECEIVED', 'PRODUCT_NOT_AS_DESCRIBED', 'CREDIT_NOT_PROCESSED', 'GENERAL')),
    status VARCHAR(20) NOT NULL DEFAULT 'OPEN'
        CHECK (status IN ('OPEN', 'UNDER_REVIEW', 'WON', 'LOST', 'CLOSED')),
    amount_disputed DECIMAL(10,2) NOT NULL CHECK (amount_disputed > 0),
    currency VARCHAR(3) NOT NULL DEFAULT 'USD',
    processor_dispute_id VARCHAR(100), -- External dispute ID
    evidence JSONB, -- Evidence submitted for the dispute
    response_deadline TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    resolved_at TIMESTAMP,
    created_by UUID,
    updated_by UUID
);

-- Payment Analytics Table for Business Intelligence
CREATE TABLE IF NOT EXISTS payment_analytics (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    date DATE NOT NULL,
    payment_method VARCHAR(50) NOT NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'USD',
    total_transactions BIGINT NOT NULL DEFAULT 0,
    successful_transactions BIGINT NOT NULL DEFAULT 0,
    failed_transactions BIGINT NOT NULL DEFAULT 0,
    total_amount DECIMAL(15,2) NOT NULL DEFAULT 0,
    successful_amount DECIMAL(15,2) NOT NULL DEFAULT 0,
    failed_amount DECIMAL(15,2) NOT NULL DEFAULT 0,
    average_transaction_amount DECIMAL(10,2),
    success_rate DECIMAL(5,2),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(date, payment_method, currency)
);

-- Create comprehensive indexes for performance
CREATE INDEX IF NOT EXISTS idx_payments_order_id ON payments(order_id);
CREATE INDEX IF NOT EXISTS idx_payments_user_id ON payments(user_id);
CREATE INDEX IF NOT EXISTS idx_payments_status ON payments(status);
CREATE INDEX IF NOT EXISTS idx_payments_payment_method ON payments(payment_method);
CREATE INDEX IF NOT EXISTS idx_payments_payment_reference ON payments(payment_reference);
CREATE INDEX IF NOT EXISTS idx_payments_created_at ON payments(created_at);
CREATE INDEX IF NOT EXISTS idx_payments_updated_at ON payments(updated_at);
CREATE INDEX IF NOT EXISTS idx_payments_expires_at ON payments(expires_at);
CREATE INDEX IF NOT EXISTS idx_payments_currency ON payments(currency);
CREATE INDEX IF NOT EXISTS idx_payments_amount ON payments(amount);

-- Composite indexes for common query patterns
CREATE INDEX IF NOT EXISTS idx_payments_user_status ON payments(user_id, status);
CREATE INDEX IF NOT EXISTS idx_payments_order_status ON payments(order_id, status);
CREATE INDEX IF NOT EXISTS idx_payments_method_status ON payments(payment_method, status);
CREATE INDEX IF NOT EXISTS idx_payments_status_created ON payments(status, created_at);
CREATE INDEX IF NOT EXISTS idx_payments_user_created ON payments(user_id, created_at);

-- Partial indexes for filtered queries
CREATE INDEX IF NOT EXISTS idx_payments_pending_expired ON payments(id) 
    WHERE status = 'PENDING' AND expires_at < CURRENT_TIMESTAMP;
CREATE INDEX IF NOT EXISTS idx_payments_failed_recent ON payments(id, created_at) 
    WHERE status = 'FAILED' AND created_at > CURRENT_TIMESTAMP - INTERVAL '7 days';

-- Payment methods indexes
CREATE INDEX IF NOT EXISTS idx_payment_methods_user_id ON payment_methods(user_id);
CREATE INDEX IF NOT EXISTS idx_payment_methods_type ON payment_methods(payment_type);
CREATE INDEX IF NOT EXISTS idx_payment_methods_provider ON payment_methods(payment_provider);
CREATE INDEX IF NOT EXISTS idx_payment_methods_active ON payment_methods(is_active);
CREATE INDEX IF NOT EXISTS idx_payment_methods_default ON payment_methods(is_default);

-- Payment transactions indexes
CREATE INDEX IF NOT EXISTS idx_transactions_payment_id ON payment_transactions(payment_id);
CREATE INDEX IF NOT EXISTS idx_transactions_type ON payment_transactions(transaction_type);
CREATE INDEX IF NOT EXISTS idx_transactions_status ON payment_transactions(status);
CREATE INDEX IF NOT EXISTS idx_transactions_created_at ON payment_transactions(created_at);
CREATE INDEX IF NOT EXISTS idx_transactions_processor_id ON payment_transactions(processor_transaction_id);

-- Refunds indexes
CREATE INDEX IF NOT EXISTS idx_refunds_payment_id ON refunds(payment_id);
CREATE INDEX IF NOT EXISTS idx_refunds_order_id ON refunds(order_id);
CREATE INDEX IF NOT EXISTS idx_refunds_status ON refunds(status);
CREATE INDEX IF NOT EXISTS idx_refunds_reason ON refunds(reason);
CREATE INDEX IF NOT EXISTS idx_refunds_created_at ON refunds(created_at);

-- Disputes indexes
CREATE INDEX IF NOT EXISTS idx_disputes_payment_id ON payment_disputes(payment_id);
CREATE INDEX IF NOT EXISTS idx_disputes_type ON payment_disputes(dispute_type);
CREATE INDEX IF NOT EXISTS idx_disputes_status ON payment_disputes(status);
CREATE INDEX IF NOT EXISTS idx_disputes_created_at ON payment_disputes(created_at);

-- Analytics indexes
CREATE INDEX IF NOT EXISTS idx_analytics_date ON payment_analytics(date);
CREATE INDEX IF NOT EXISTS idx_analytics_method ON payment_analytics(payment_method);
CREATE INDEX IF NOT EXISTS idx_analytics_currency ON payment_analytics(currency);
CREATE INDEX IF NOT EXISTS idx_analytics_date_method ON payment_analytics(date, payment_method);

-- Add comments for documentation
COMMENT ON TABLE payments IS 'Core payments table with comprehensive payment processing capabilities';
COMMENT ON TABLE payment_methods IS 'User payment method preferences and stored payment information';
COMMENT ON TABLE payment_transactions IS 'Detailed transaction history for audit and reconciliation';
COMMENT ON TABLE refunds IS 'Refund management with reason tracking and status management';
COMMENT ON TABLE payment_disputes IS 'Chargeback and dispute management for payment protection';
COMMENT ON TABLE payment_analytics IS 'Business intelligence data for payment performance analysis';

-- Create updated_at trigger function (if not exists)
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

-- Create triggers for updated_at columns
CREATE TRIGGER update_payments_updated_at BEFORE UPDATE ON payments
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_payment_methods_updated_at BEFORE UPDATE ON payment_methods
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_transactions_updated_at BEFORE UPDATE ON payment_transactions
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_refunds_updated_at BEFORE UPDATE ON refunds
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_disputes_updated_at BEFORE UPDATE ON payment_disputes
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_analytics_updated_at BEFORE UPDATE ON payment_analytics
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- Create function to process payment authorization
CREATE OR REPLACE FUNCTION process_payment_authorization(
    p_payment_id UUID,
    p_processor_transaction_id VARCHAR(100),
    p_processor_response JSONB,
    p_authorized_amount DECIMAL(10,2)
)
RETURNS BOOLEAN AS $$
BEGIN
    -- Update payment status
    UPDATE payments 
    SET status = 'AUTHORIZED',
        authorized_at = CURRENT_TIMESTAMP,
        updated_at = CURRENT_TIMESTAMP,
        processor_response = p_processor_response
    WHERE id = p_payment_id 
      AND status = 'PENDING';
    
    -- Create transaction record
    INSERT INTO payment_transactions (
        payment_id, 
        transaction_type, 
        amount, 
        currency, 
        status, 
        processor_transaction_id, 
        processor_response, 
        processed_at
    )
    SELECT 
        id,
        'AUTHORIZATION',
        p_authorized_amount,
        currency,
        'SUCCESS',
        p_processor_transaction_id,
        p_processor_response,
        CURRENT_TIMESTAMP
    FROM payments 
    WHERE id = p_payment_id;
    
    RETURN FOUND;
END;
$$ LANGUAGE plpgsql;

-- Create function to process payment capture
CREATE OR REPLACE FUNCTION process_payment_capture(
    p_payment_id UUID,
    p_captured_amount DECIMAL(10,2),
    p_processor_transaction_id VARCHAR(100),
    p_processor_response JSONB
)
RETURNS BOOLEAN AS $$
BEGIN
    -- Update payment status
    UPDATE payments 
    SET status = 'CAPTURED',
        captured_at = CURRENT_TIMESTAMP,
        updated_at = CURRENT_TIMESTAMP,
        processor_response = p_processor_response
    WHERE id = p_payment_id 
      AND status = 'AUTHORIZED';
    
    -- Create transaction record
    INSERT INTO payment_transactions (
        payment_id, 
        transaction_type, 
        amount, 
        currency, 
        status, 
        processor_transaction_id, 
        processor_response, 
        processed_at
    )
    SELECT 
        id,
        'CAPTURE',
        p_captured_amount,
        currency,
        'SUCCESS',
        p_processor_transaction_id,
        p_processor_response,
        CURRENT_TIMESTAMP
    FROM payments 
    WHERE id = p_payment_id;
    
    RETURN FOUND;
END;
$$ LANGUAGE plpgsql;

-- Create function to process payment failure
CREATE OR REPLACE FUNCTION process_payment_failure(
    p_payment_id UUID,
    p_failure_reason TEXT,
    p_failure_code VARCHAR(50),
    p_processor_response JSONB
)
RETURNS BOOLEAN AS $$
BEGIN
    -- Update payment status
    UPDATE payments 
    SET status = 'FAILED',
        failed_at = CURRENT_TIMESTAMP,
        updated_at = CURRENT_TIMESTAMP,
        failure_reason = p_failure_reason,
        failure_code = p_failure_code,
        processor_response = p_processor_response
    WHERE id = p_payment_id 
      AND status IN ('PENDING', 'AUTHORIZED');
    
    -- Create transaction record
    INSERT INTO payment_transactions (
        payment_id, 
        transaction_type, 
        amount, 
        currency, 
        status, 
        error_code, 
        error_message, 
        processor_response, 
        processed_at
    )
    SELECT 
        id,
        'AUTHORIZATION',
        amount,
        currency,
        'FAILED',
        p_failure_code,
        p_failure_reason,
        p_processor_response,
        CURRENT_TIMESTAMP
    FROM payments 
    WHERE id = p_payment_id;
    
    RETURN FOUND;
END;
$$ LANGUAGE plpgsql;

-- Create function to process refund
CREATE OR REPLACE FUNCTION process_refund(
    p_payment_id UUID,
    p_order_id UUID,
    p_refund_amount DECIMAL(10,2),
    p_reason VARCHAR(100),
    p_processor_refund_id VARCHAR(100),
    p_created_by UUID
)
RETURNS UUID AS $$
DECLARE
    v_refund_id UUID;
    v_payment_currency VARCHAR(3);
BEGIN
    -- Get payment currency
    SELECT currency INTO v_payment_currency FROM payments WHERE id = p_payment_id;
    
    -- Create refund record
    INSERT INTO refunds (
        payment_id,
        order_id,
        amount,
        currency,
        reason,
        processor_refund_id,
        created_by
    ) VALUES (
        p_payment_id,
        p_order_id,
        p_refund_amount,
        v_payment_currency,
        p_reason,
        p_processor_refund_id,
        p_created_by
    ) RETURNING id INTO v_refund_id;
    
    -- Create transaction record
    INSERT INTO payment_transactions (
        payment_id,
        transaction_type,
        amount,
        currency,
        status,
        processor_transaction_id,
        processed_at
    ) VALUES (
        p_payment_id,
        'REFUND',
        p_refund_amount,
        v_payment_currency,
        'SUCCESS',
        p_processor_refund_id,
        CURRENT_TIMESTAMP
    );
    
    -- Update payment status if full refund
    IF p_refund_amount >= (SELECT amount FROM payments WHERE id = p_payment_id) THEN
        UPDATE payments SET status = 'REFUNDED', updated_at = CURRENT_TIMESTAMP WHERE id = p_payment_id;
    ELSE
        UPDATE payments SET status = 'PARTIALLY_REFUNDED', updated_at = CURRENT_TIMESTAMP WHERE id = p_payment_id;
    END IF;
    
    RETURN v_refund_id;
END;
$$ LANGUAGE plpgsql;

-- Create function to get payment analytics for a date range
CREATE OR REPLACE FUNCTION get_payment_analytics(
    p_start_date DATE,
    p_end_date DATE,
    p_payment_method VARCHAR(50) DEFAULT NULL,
    p_currency VARCHAR(3) DEFAULT 'USD'
)
RETURNS TABLE (
    date DATE,
    payment_method VARCHAR(50),
    total_transactions BIGINT,
    successful_transactions BIGINT,
    failed_transactions BIGINT,
    total_amount DECIMAL(15,2),
    successful_amount DECIMAL(15,2),
    failed_amount DECIMAL(15,2),
    success_rate DECIMAL(5,2),
    average_transaction_amount DECIMAL(10,2)
) AS $$
BEGIN
    RETURN QUERY
    SELECT 
        pa.date,
        pa.payment_method,
        pa.total_transactions,
        pa.successful_transactions,
        pa.failed_transactions,
        pa.total_amount,
        pa.successful_amount,
        pa.failed_amount,
        pa.success_rate,
        pa.average_transaction_amount
    FROM payment_analytics pa
    WHERE pa.date BETWEEN p_start_date AND p_end_date
      AND (p_payment_method IS NULL OR pa.payment_method = p_payment_method)
      AND pa.currency = p_currency
    ORDER BY pa.date DESC, pa.payment_method;
END;
$$ LANGUAGE plpgsql;

-- Create function to clean up expired payments
CREATE OR REPLACE FUNCTION cleanup_expired_payments()
RETURNS INT AS $$
DECLARE
    cleaned_count INT := 0;
BEGIN
    -- Cancel expired pending payments
    UPDATE payments 
    SET status = 'CANCELLED', 
        updated_at = CURRENT_TIMESTAMP
    WHERE status = 'PENDING' 
      AND expires_at < CURRENT_TIMESTAMP;
    
    GET DIAGNOSTICS cleaned_count = ROW_COUNT;
    
    -- Create transaction records for cancelled payments
    INSERT INTO payment_transactions (
        payment_id, 
        transaction_type, 
        amount, 
        currency, 
        status, 
        error_message, 
        processed_at
    )
    SELECT 
        id,
        'AUTHORIZATION',
        amount,
        currency,
        'CANCELLED',
        'Payment expired and automatically cancelled',
        CURRENT_TIMESTAMP
    FROM payments 
    WHERE status = 'CANCELLED' 
      AND updated_at = CURRENT_TIMESTAMP;
    
    RETURN cleaned_count;
END;
$$ LANGUAGE plpgsql;
