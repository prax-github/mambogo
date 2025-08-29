CREATE TABLE orders (
  id UUID PRIMARY KEY,
  user_id UUID NOT NULL,
  status VARCHAR(40) NOT NULL,
  total_amount NUMERIC(12,2) NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT now(),
  updated_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE order_items (
  id UUID PRIMARY KEY,
  order_id UUID NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
  product_id UUID NOT NULL,
  qty INT NOT NULL,
  price NUMERIC(12,2) NOT NULL
);

CREATE TABLE idem_keys (
  request_id VARCHAR(64) PRIMARY KEY,
  order_id UUID NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE outbox (
  id BIGSERIAL PRIMARY KEY,
  aggregate_type VARCHAR(64) NOT NULL,
  aggregate_id UUID NOT NULL,
  event_type VARCHAR(64) NOT NULL,
  payload JSONB NOT NULL,
  headers JSONB,
  created_at TIMESTAMP NOT NULL DEFAULT now(),
  sent_at TIMESTAMP
);

CREATE INDEX idx_outbox_unsent ON outbox (sent_at) WHERE sent_at IS NULL;
