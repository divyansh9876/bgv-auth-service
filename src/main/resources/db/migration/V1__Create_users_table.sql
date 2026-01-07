-- Create users table
CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255),
    role VARCHAR(50) NOT NULL,
    status VARCHAR(50) NOT NULL,
    auth_provider VARCHAR(50) NOT NULL,
    provider_id VARCHAR(255) UNIQUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Create indexes for performance
CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_provider_id ON users(provider_id);
CREATE INDEX idx_users_status ON users(status);

-- Add comments
COMMENT ON TABLE users IS 'User accounts for authentication';
COMMENT ON COLUMN users.password_hash IS 'BCrypt hash, nullable for Google users';
COMMENT ON COLUMN users.provider_id IS 'Google sub claim, nullable for LOCAL users';
COMMENT ON COLUMN users.auth_provider IS 'LOCAL or GOOGLE';
COMMENT ON COLUMN users.status IS 'ACTIVE or BLOCKED';



