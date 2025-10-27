-- Migration: Add phone column to profiles table
-- Date: 2025-10-26
-- Description: Adds an optional phone field to store user phone numbers

-- Add phone column to profiles table
ALTER TABLE profiles
ADD COLUMN IF NOT EXISTS phone TEXT;

-- Add comment to document the column
COMMENT ON COLUMN profiles.phone IS 'User phone number (optional, US format)';

-- Optional: Add a check constraint to validate phone format (US phone numbers)
-- This matches the validation pattern used in the backend validation schema
ALTER TABLE profiles
ADD CONSTRAINT phone_format_check
CHECK (phone IS NULL OR phone ~ '^\+?1?[-.\s]?\(?\d{3}\)?[-.\s]?\d{3}[-.\s]?\d{4}$');
