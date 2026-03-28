-- PostgreSQL Setup Script for ILoveMusic Backend
-- Run this script using: psql -U postgres -f setup.sql

-- Step 1: Create the database
CREATE DATABASE ilovemusic;

-- Step 2: Connect to the database
\c ilovemusic

-- Step 3: Create extensions (optional but recommended)
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pg_trgm";

-- Step 4: Verify database created
SELECT datname FROM pg_database WHERE datname = 'ilovemusic';

-- Step 5: Show confirmation
\echo 'Database "ilovemusic" created successfully!'
\echo 'You can now use: psql -U postgres -d ilovemusic'

