-- Add `name` column to users table in a migration separate from V1
-- Step 1: add nullable column
ALTER TABLE users ADD COLUMN name VARCHAR(255);

-- Step 2: populate from email local-part for existing users
UPDATE users SET name = split_part(email, '@', 1) WHERE name IS NULL;

-- Step 3: enforce NOT NULL constraint once populated
ALTER TABLE users ALTER COLUMN name SET NOT NULL;

-- Comment
COMMENT ON COLUMN users.name IS 'Full name provided at signup';
