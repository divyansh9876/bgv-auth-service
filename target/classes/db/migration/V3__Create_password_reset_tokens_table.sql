-- Create password_reset_tokens table
CREATE TABLE password_reset_tokens (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    token VARCHAR(512) NOT NULL UNIQUE,
    user_id UUID NOT NULL,
    expiry TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    used BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_password_reset_token_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Create indexes for performance
CREATE INDEX idx_password_reset_tokens_token ON password_reset_tokens(token);
CREATE INDEX idx_password_reset_tokens_user_id ON password_reset_tokens(user_id);
CREATE INDEX idx_password_reset_tokens_expiry ON password_reset_tokens(expiry);
CREATE INDEX idx_password_reset_tokens_used ON password_reset_tokens(used);

-- Add comments
COMMENT ON TABLE password_reset_tokens IS 'Password reset tokens for forgot password functionality';
COMMENT ON COLUMN password_reset_tokens.token IS 'UUID-based reset token';
COMMENT ON COLUMN password_reset_tokens.expiry IS 'Token expiration timestamp (typically 1 hour)';
COMMENT ON COLUMN password_reset_tokens.used IS 'Whether the token has been used to reset password';


