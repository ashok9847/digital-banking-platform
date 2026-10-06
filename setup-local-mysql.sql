-- ==============================================================================
-- Digital Banking Platform - Local MySQL Setup Script
-- Run this in MySQL Workbench or mysql CLI connected to your local MySQL server
-- ==============================================================================

-- 1. Create Isolated Databases for all Microservices
CREATE DATABASE IF NOT EXISTS auth_db 
  CHARACTER SET utf8mb4 
  COLLATE utf8mb4_unicode_ci;

CREATE DATABASE IF NOT EXISTS customer_db 
  CHARACTER SET utf8mb4 
  COLLATE utf8mb4_unicode_ci;

CREATE DATABASE IF NOT EXISTS account_db 
  CHARACTER SET utf8mb4 
  COLLATE utf8mb4_unicode_ci;

CREATE DATABASE IF NOT EXISTS transaction_db 
  CHARACTER SET utf8mb4 
  COLLATE utf8mb4_unicode_ci;

CREATE DATABASE IF NOT EXISTS notification_db 
  CHARACTER SET utf8mb4 
  COLLATE utf8mb4_unicode_ci;

CREATE DATABASE IF NOT EXISTS audit_db 
  CHARACTER SET utf8mb4 
  COLLATE utf8mb4_unicode_ci;

-- 2. Verify all 6 schemas exist
SHOW DATABASES LIKE '%_db';
