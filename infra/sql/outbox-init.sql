-- Outbox table for Order Service
-- This table implements the Outbox Pattern for reliable event publishing

CREATE TABLE IF NOT EXISTS outbox_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    aggregate_type VARCHAR(255) NOT NULL,
    aggregate_id VARCHAR(255) NOT NULL,
    event_type VARCHAR(255) NOT NULL,
    payload TEXT NOT NULL,
    headers TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    sent_at TIMESTAMP,
    retry_count INTEGER NOT NULL DEFAULT 0,
    max_retries INTEGER NOT NULL DEFAULT 3,
    next_retry_at TIMESTAMP,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    CONSTRAINT chk_status CHECK (status IN ('PENDING', 'SENT', 'FAILED', 'RETRY'))
);

-- Indexes for better performance
CREATE INDEX IF NOT EXISTS idx_outbox_status ON outbox_events(status);
CREATE INDEX IF NOT EXISTS idx_outbox_created_at ON outbox_events(created_at);
CREATE INDEX IF NOT EXISTS idx_outbox_aggregate ON outbox_events(aggregate_type, aggregate_id);
CREATE INDEX IF NOT EXISTS idx_outbox_event_type ON outbox_events(event_type);
CREATE INDEX IF NOT EXISTS idx_outbox_retry ON outbox_events(status, next_retry_at) WHERE status = 'RETRY';

-- Comments
COMMENT ON TABLE outbox_events IS 'Outbox table for reliable event publishing using the Outbox Pattern';
COMMENT ON COLUMN outbox_events.id IS 'Unique identifier for the outbox event';
COMMENT ON COLUMN outbox_events.aggregate_type IS 'Type of the aggregate (e.g., Order, Payment)';
COMMENT ON COLUMN outbox_events.aggregate_id IS 'ID of the aggregate that generated this event';
COMMENT ON COLUMN outbox_events.event_type IS 'Type of the event (e.g., OrderCreated, PaymentAuthorized)';
COMMENT ON COLUMN outbox_events.payload IS 'JSON payload of the event';
COMMENT ON COLUMN outbox_events.headers IS 'Optional headers for the event';
COMMENT ON COLUMN outbox_events.created_at IS 'Timestamp when the event was created';
COMMENT ON COLUMN outbox_events.sent_at IS 'Timestamp when the event was successfully sent';
COMMENT ON COLUMN outbox_events.retry_count IS 'Number of retry attempts made';
COMMENT ON COLUMN outbox_events.max_retries IS 'Maximum number of retry attempts allowed';
COMMENT ON COLUMN outbox_events.next_retry_at IS 'Timestamp for the next retry attempt';
COMMENT ON COLUMN outbox_events.status IS 'Current status of the event (PENDING, SENT, FAILED, RETRY)';
