-- Product Service Database Configuration
-- This file contains configuration specific to the Product Service

-- ============================================================================
-- DATABASE SECURITY CONFIGURATION
-- ============================================================================

-- Create dedicated database user for Product Service
DO $$
BEGIN
    IF NOT EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = 'product_service_user') THEN
        CREATE USER product_service_user WITH PASSWORD 'product_service_password';
    END IF;
END
$$;

-- Grant permissions for Product Service
GRANT CONNECT ON DATABASE products TO product_service_user;
GRANT USAGE ON SCHEMA public TO product_service_user;
GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA public TO product_service_user;
GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public TO product_service_user;
GRANT EXECUTE ON ALL FUNCTIONS IN SCHEMA public TO product_service_user;

-- ============================================================================
-- EXTENSIONS
-- ============================================================================

-- Enable required extensions for enhanced functionality
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";  -- UUID generation
CREATE EXTENSION IF NOT EXISTS "pg_trgm";    -- Trigram similarity for text search
CREATE EXTENSION IF NOT EXISTS "unaccent";   -- Accent-insensitive text search

-- ============================================================================
-- MAINTENANCE FUNCTIONS
-- ============================================================================

-- Function to analyze all tables for better query planning
CREATE OR REPLACE FUNCTION analyze_all_tables()
RETURNS void AS $$
DECLARE
    table_record RECORD;
BEGIN
    FOR table_record IN 
        SELECT tablename FROM pg_tables 
        WHERE schemaname = 'public'
    LOOP
        EXECUTE 'ANALYZE ' || quote_ident(table_record.tablename);
    END LOOP;
END;
$$ LANGUAGE plpgsql;

-- Function to vacuum all tables for maintenance
CREATE OR REPLACE FUNCTION vacuum_all_tables()
RETURNS void AS $$
DECLARE
    table_record RECORD;
BEGIN
    FOR table_record IN 
        SELECT tablename FROM pg_tables 
        WHERE schemaname = 'public'
    LOOP
        EXECUTE 'VACUUM ' || quote_ident(table_record.tablename);
    END LOOP;
END;
$$ LANGUAGE plpgsql;

-- ============================================================================
-- MONITORING FUNCTIONS
-- ============================================================================

-- Function to check database health
CREATE OR REPLACE FUNCTION check_database_health()
RETURNS TABLE(
    check_name TEXT,
    status TEXT,
    details TEXT
) AS $$
BEGIN
    -- Check active connections
    RETURN QUERY
    SELECT 
        'Active Connections'::TEXT as check_name,
        CASE 
            WHEN COUNT(*) < 100 THEN 'HEALTHY'::TEXT
            ELSE 'WARNING'::TEXT
        END as status,
        COUNT(*)::TEXT as details
    FROM pg_stat_activity;
    
    -- Check database size
    RETURN QUERY
    SELECT 
        'Database Size'::TEXT as check_name,
        CASE 
            WHEN pg_database_size(current_database()) < 1073741824 THEN 'HEALTHY'::TEXT
            ELSE 'WARNING'::TEXT
        END as status,
        pg_size_pretty(pg_database_size(current_database())) as details;
    
    -- Check table statistics
    RETURN QUERY
    SELECT 
        'Table Statistics'::TEXT as check_name,
        CASE 
            WHEN COUNT(*) > 0 THEN 'HEALTHY'::TEXT
            ELSE 'WARNING'::TEXT
        END as status,
        COUNT(*)::TEXT as details
    FROM pg_stat_user_tables;
END;
$$ LANGUAGE plpgsql;

-- Function to get table statistics
CREATE OR REPLACE FUNCTION get_table_statistics()
RETURNS TABLE(
    table_name TEXT,
    row_count BIGINT,
    table_size TEXT,
    index_size TEXT,
    total_size TEXT
) AS $$
BEGIN
    RETURN QUERY
    SELECT 
        schemaname||'.'||tablename as table_name,
        n_tup_ins + n_tup_upd + n_tup_del as row_count,
        pg_size_pretty(pg_total_relation_size(schemaname||'.'||tablename)) as table_size,
        pg_size_pretty(pg_indexes_size(schemaname||'.'||tablename)) as index_size,
        pg_size_pretty(pg_total_relation_size(schemaname||'.'||tablename)) as total_size
    FROM pg_stat_user_tables
    ORDER BY pg_total_relation_size(schemaname||'.'||tablename) DESC;
END;
$$ LANGUAGE plpgsql;

-- ============================================================================
-- FINAL CONFIGURATION
-- ============================================================================

-- Grant execute permissions on all functions to service user
GRANT EXECUTE ON ALL FUNCTIONS IN SCHEMA public TO product_service_user;

-- Log successful configuration
DO $$
BEGIN
    RAISE NOTICE 'Product Service database configuration completed successfully';
END;
$$;
