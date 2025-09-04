-- Database Configuration and Performance Tuning
-- This file contains comprehensive database configuration for all services

-- ============================================================================
-- DATABASE SECURITY CONFIGURATION
-- ============================================================================

-- Create dedicated database users for each service
-- This provides better security isolation

-- Product Service User
DO $$
BEGIN
    IF NOT EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = 'product_service_user') THEN
        CREATE USER product_service_user WITH PASSWORD 'product_service_password';
    END IF;
END
$$;
GRANT CONNECT ON DATABASE products TO product_service_user;
GRANT USAGE ON SCHEMA public TO product_service_user;
GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA public TO product_service_user;
GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public TO product_service_user;
GRANT EXECUTE ON ALL FUNCTIONS IN SCHEMA public TO product_service_user;

-- Order Service User
DO $$
BEGIN
    IF NOT EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = 'order_service_user') THEN
        CREATE USER order_service_user WITH PASSWORD 'order_service_password';
    END IF;
END
$$;
GRANT CONNECT ON DATABASE orders TO order_service_user;
GRANT USAGE ON SCHEMA public TO order_service_user;
GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA public TO order_service_user;
GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public TO order_service_user;
GRANT EXECUTE ON ALL FUNCTIONS IN SCHEMA public TO order_service_user;

-- Payment Service User
DO $$
BEGIN
    IF NOT EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = 'payment_service_user') THEN
        CREATE USER payment_service_user WITH PASSWORD 'payment_service_password';
    END IF;
END
$$;
GRANT CONNECT ON DATABASE payments TO payment_service_user;
GRANT USAGE ON SCHEMA public TO payment_service_user;
GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA public TO payment_service_user;
GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public TO payment_service_user;
GRANT EXECUTE ON ALL FUNCTIONS IN SCHEMA public TO payment_service_user;

-- Inventory Service User
DO $$
BEGIN
    IF NOT EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = 'inventory_service_user') THEN
        CREATE USER inventory_service_user WITH PASSWORD 'inventory_service_password';
    END IF;
END
$$;
GRANT CONNECT ON DATABASE inventory TO inventory_service_user;
GRANT USAGE ON SCHEMA public TO inventory_service_user;
GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA public TO inventory_service_user;
GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public TO inventory_service_user;
GRANT EXECUTE ON ALL FUNCTIONS IN SCHEMA public TO inventory_service_user;

-- ============================================================================
-- PERFORMANCE TUNING CONFIGURATION
-- ============================================================================

-- Set PostgreSQL configuration parameters for optimal performance
-- These should be configured in postgresql.conf

-- Memory Configuration
-- shared_buffers = 256MB                    -- 25% of available RAM
-- effective_cache_size = 1GB                -- 75% of available RAM
-- work_mem = 4MB                           -- Per connection memory
-- maintenance_work_mem = 64MB              -- Maintenance operations

-- WAL Configuration
-- wal_buffers = 16MB                       -- WAL buffer size
-- checkpoint_completion_target = 0.9       -- Checkpoint completion target
-- checkpoint_timeout = 5min                -- Checkpoint timeout

-- Query Planning
-- random_page_cost = 1.1                   -- SSD storage cost
-- effective_io_concurrency = 200           -- Concurrent I/O operations
-- default_statistics_target = 100          -- Statistics accuracy

-- Connection Settings
-- max_connections = 200                    -- Maximum concurrent connections
-- superuser_reserved_connections = 3       -- Reserved for superuser

-- Logging Configuration
-- log_statement = 'all'                    -- Log all statements
-- log_duration = on                        -- Log query duration
-- log_min_duration_statement = 1000        -- Log queries taking > 1 second

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

-- Function to cleanup old data based on retention policy
CREATE OR REPLACE FUNCTION cleanup_old_data()
RETURNS void AS $$
BEGIN
    -- Clean up old outbox events (older than 7 days)
    DELETE FROM outbox_events 
    WHERE created_at < CURRENT_TIMESTAMP - INTERVAL '7 days';
    
    -- Clean up expired idempotency keys (older than 1 day)
    DELETE FROM idempotency_keys 
    WHERE expires_at < CURRENT_TIMESTAMP;
    
    -- Clean up old order status history (older than 1 year)
    DELETE FROM order_status_history 
    WHERE created_at < CURRENT_TIMESTAMP - INTERVAL '1 year';
    
    -- Clean up old payment transactions (older than 1 year)
    DELETE FROM payment_transactions 
    WHERE created_at < CURRENT_TIMESTAMP - INTERVAL '1 year';
    
    -- Clean up old inventory movements (older than 1 year)
    DELETE FROM inventory_movements 
    WHERE created_at < CURRENT_TIMESTAMP - INTERVAL '1 year';
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

-- Function to get index usage statistics
CREATE OR REPLACE FUNCTION get_index_usage_statistics()
RETURNS TABLE(
    index_name TEXT,
    table_name TEXT,
    scans BIGINT,
    tuples_read BIGINT,
    tuples_fetched BIGINT
) AS $$
BEGIN
    RETURN QUERY
    SELECT 
        indexrelname as index_name,
        tablename as table_name,
        idx_scan as scans,
        idx_tup_read as tuples_read,
        idx_tup_fetch as tuples_fetched
    FROM pg_stat_user_indexes
    ORDER BY idx_scan DESC;
END;
$$ LANGUAGE plpgsql;

-- Function to get slow queries
CREATE OR REPLACE FUNCTION get_slow_queries()
RETURNS TABLE(
    query TEXT,
    calls BIGINT,
    total_time DOUBLE PRECISION,
    mean_time DOUBLE PRECISION
) AS $$
BEGIN
    RETURN QUERY
    SELECT 
        query as query,
        calls as calls,
        total_time as total_time,
        mean_time as mean_time
    FROM pg_stat_statements
    WHERE mean_time > 100  -- Queries taking more than 100ms
    ORDER BY mean_time DESC
    LIMIT 10;
END;
$$ LANGUAGE plpgsql;

-- Function to get cache hit ratios
CREATE OR REPLACE FUNCTION get_cache_hit_ratios()
RETURNS TABLE(
    table_name TEXT,
    heap_hit_ratio NUMERIC,
    index_hit_ratio NUMERIC
) AS $$
BEGIN
    RETURN QUERY
    SELECT 
        schemaname||'.'||tablename as table_name,
        ROUND(
            CASE 
                WHEN heap_blks_hit + heap_blks_read = 0 THEN 0
                ELSE (heap_blks_hit::NUMERIC / (heap_blks_hit + heap_blks_read)) * 100
            END, 2
        ) as heap_hit_ratio,
        ROUND(
            CASE 
                WHEN idx_blks_hit + idx_blks_read = 0 THEN 0
                ELSE (idx_blks_hit::NUMERIC / (idx_blks_hit + idx_blks_read)) * 100
            END, 2
        ) as index_hit_ratio
    FROM pg_statio_user_tables
    ORDER BY heap_blks_hit + heap_blks_read DESC;
END;
$$ LANGUAGE plpgsql;

-- ============================================================================
-- BACKUP FUNCTIONS
-- ============================================================================

-- Function to generate backup script
CREATE OR REPLACE FUNCTION generate_backup_script()
RETURNS TEXT AS $$
DECLARE
    backup_script TEXT;
    db_name TEXT;
BEGIN
    db_name := current_database();
    backup_script := 'pg_dump -h localhost -U postgres -d ' || db_name || 
                     ' -f backup_' || db_name || '_' || 
                     to_char(CURRENT_TIMESTAMP, 'YYYY-MM-DD_HH24-MI-SS') || '.sql';
    
    RETURN backup_script;
END;
$$ LANGUAGE plpgsql;

-- ============================================================================
-- EXTENSIONS
-- ============================================================================

-- Enable required extensions for enhanced functionality
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";  -- UUID generation
CREATE EXTENSION IF NOT EXISTS "pg_trgm";    -- Trigram similarity for text search
CREATE EXTENSION IF NOT EXISTS "unaccent";   -- Accent-insensitive text search

-- ============================================================================
-- CONFIGURATION VALIDATION
-- ============================================================================

-- Function to validate database configuration
CREATE OR REPLACE FUNCTION validate_database_configuration()
RETURNS TABLE(
    setting_name TEXT,
    current_value TEXT,
    recommended_value TEXT,
    status TEXT
) AS $$
BEGIN
    -- Check shared_buffers
    RETURN QUERY
    SELECT 
        'shared_buffers'::TEXT as setting_name,
        setting as current_value,
        '256MB'::TEXT as recommended_value,
        CASE 
            WHEN setting::bigint >= 268435456 THEN 'OPTIMAL'::TEXT
            ELSE 'SUBOPTIMAL'::TEXT
        END as status
    FROM pg_settings WHERE name = 'shared_buffers';
    
    -- Check work_mem
    RETURN QUERY
    SELECT 
        'work_mem'::TEXT as setting_name,
        setting as current_value,
        '4MB'::TEXT as recommended_value,
        CASE 
            WHEN setting::bigint >= 4194304 THEN 'OPTIMAL'::TEXT
            ELSE 'SUBOPTIMAL'::TEXT
        END as status
    FROM pg_settings WHERE name = 'work_mem';
    
    -- Check max_connections
    RETURN QUERY
    SELECT 
        'max_connections'::TEXT as setting_name,
        setting as current_value,
        '200'::TEXT as recommended_value,
        CASE 
            WHEN setting::integer >= 200 THEN 'OPTIMAL'::TEXT
            ELSE 'SUBOPTIMAL'::TEXT
        END as status
    FROM pg_settings WHERE name = 'max_connections';
END;
$$ LANGUAGE plpgsql;

-- ============================================================================
-- SECURITY AUDIT FUNCTIONS
-- ============================================================================

-- Function to audit user permissions
CREATE OR REPLACE FUNCTION audit_user_permissions()
RETURNS TABLE(
    username TEXT,
    database_name TEXT,
    permissions TEXT[]
) AS $$
BEGIN
    RETURN QUERY
    SELECT 
        r.rolname::TEXT as username,
        d.datname::TEXT as database_name,
        ARRAY_AGG(DISTINCT p.privilege_type) as permissions
    FROM pg_roles r
    CROSS JOIN pg_database d
    LEFT JOIN information_schema.role_table_grants p ON p.grantee = r.rolname
    WHERE r.rolcanlogin = true
    GROUP BY r.rolname, d.datname
    ORDER BY r.rolname, d.datname;
END;
$$ LANGUAGE plpgsql;

-- ============================================================================
-- FINAL CONFIGURATION
-- ============================================================================

-- Grant execute permissions on all functions to service users
GRANT EXECUTE ON ALL FUNCTIONS IN SCHEMA public TO product_service_user;
GRANT EXECUTE ON ALL FUNCTIONS IN SCHEMA public TO order_service_user;
GRANT EXECUTE ON ALL FUNCTIONS IN SCHEMA public TO payment_service_user;
GRANT EXECUTE ON ALL FUNCTIONS IN SCHEMA public TO inventory_service_user;

-- Log successful configuration
DO $$
BEGIN
    RAISE NOTICE 'Database configuration completed successfully for %', current_database();
END;
$$;
