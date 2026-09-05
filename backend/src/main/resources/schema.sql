-- ==============================================================================
-- Global Fintech Fest (GFF) - PostgreSQL DDL Schema
-- Database: visiting_card_db
-- ==============================================================================

-- 1. Visiting Cards & Upload Records Table
CREATE TABLE IF NOT EXISTS visiting_cards (
    id BIGSERIAL PRIMARY KEY,
    record_id VARCHAR(64) NOT NULL UNIQUE,
    
    -- Uploader Details
    uploader_name VARCHAR(255) NOT NULL,
    uploader_email VARCHAR(255) NOT NULL,
    uploader_mobile VARCHAR(32),
    uploader_role VARCHAR(32) NOT NULL,
    
    -- Media & Storage
    image_url TEXT,
    s3_key VARCHAR(512),
    s3_bucket VARCHAR(128) DEFAULT 'visiting-card-bkt',
    file_name VARCHAR(255),
    file_size VARCHAR(32),
    notes TEXT,
    
    -- Lifecycle & Sync
    status VARCHAR(32) NOT NULL DEFAULT 'PENDING_UPLOAD',
    is_offline BOOLEAN DEFAULT FALSE,
    retry_count INT DEFAULT 0,
    error_message TEXT,
    
    -- OCR Fields (for 6 PM IRIS Batch Job)
    ocr_status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    card_holder_name VARCHAR(255),
    company_name VARCHAR(255),
    designation VARCHAR(255),
    extracted_email VARCHAR(255),
    extracted_mobile VARCHAR(32),
    extracted_address TEXT,
    raw_ocr_text TEXT,
    ocr_processed_at TIMESTAMP,
    
    -- Audit Timestamps
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Indexes for optimal lookup and dashboard filtering
CREATE INDEX IF NOT EXISTS idx_vc_record_id ON visiting_cards(record_id);
CREATE INDEX IF NOT EXISTS idx_vc_uploader_email ON visiting_cards(uploader_email);
CREATE INDEX IF NOT EXISTS idx_vc_status ON visiting_cards(status);
CREATE INDEX IF NOT EXISTS idx_vc_ocr_status ON visiting_cards(ocr_status);

-- 2. Application Users Table (Field Users & Supervisors)
CREATE TABLE IF NOT EXISTS app_users (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    mobile VARCHAR(32),
    role VARCHAR(32) NOT NULL,
    avatar_url VARCHAR(512),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_users_email ON app_users(email);
