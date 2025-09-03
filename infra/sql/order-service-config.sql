-- Order Service Database Configuration
-- This file contains configuration specific to the Order Service

-- ============================================================================
-- DATABASE SECURITY CONFIGURATION
-- ============================================================================

-- Create dedicated database user for Order Service
DO $$
BEGIN
    IF NOT EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = 'order_service_user') THEN
        CREATE USER order_service_user WITH PASSWORD 'order_service_password';
    END IF;
END
$$;

-- Grant permissions for Order Service
GRANT CONNECT ON DATABASE orders TO order_service_user;
GRANT USAGE ON SCHEMA public TO order_service_user;
GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA public TO order_service_user;
GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public TO order_service_user;
GRANT EXECUTE ON ALL FUNCTIONS IN SCHEMA public TO order_service_user;

-- ============================================================================
-- EXTENSIONS
-- ============================================================================

-- Enable required extensions for enhanced functionality
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";  -- UUID generation

-- ============================================================================
-- FINAL CONFIGURATION
-- ============================================================================

-- Grant execute permissions on all functions to service user
GRANT EXECUTE ON ALL FUNCTIONS IN SCHEMA public TO order_service_user;

-- Log successful configuration
DO $$
BEGIN
    RAISE NOTICE 'Order Service database configuration completed successfully';
END;
$$;
