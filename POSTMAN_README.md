# Postman Collection for BGV Auth Service

## Import Instructions

1. **Import Collection**: Import `BGV_Auth_Service.postman_collection.json` into Postman
2. **Import Environment**: Import `BGV_Auth_Service.postman_environment.json` into Postman
3. **Select Environment**: Make sure "BGV Auth Service - Local" environment is selected

## Environment Variables

- `baseUrl`: Base URL of the auth service (default: `http://localhost:8080`)
- `accessToken`: Automatically saved after successful login/register/google auth
- `refreshToken`: Automatically saved after successful login/register/google auth
- `googleIdToken`: Google ID token for Google authentication (set manually)
- `resetToken`: Password reset token from forgot password email (set manually)

## API Endpoints

### 1. Register
**POST** `/auth/register`

Register a new user with email and password (LOCAL auth only).

**Request Body:**
```json
{
  "email": "user@example.com",
  "password": "SecurePassword123!"
}
```

**Response:**
```json
{
  "success": true,
  "message": null,
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "refreshToken": "550e8400-e29b-41d4-a716-446655440000"
  }
}
```

### 2. Login
**POST** `/auth/login`

Login with email and password (LOCAL auth only).

**Request Body:**
```json
{
  "email": "user@example.com",
  "password": "SecurePassword123!"
}
```

**Response:**
```json
{
  "success": true,
  "message": null,
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "refreshToken": "550e8400-e29b-41d4-a716-446655440000"
  }
}
```

### 3. Google Auth
**POST** `/auth/google`

Authenticate with Google ID token (signup/login).

**Request Body:**
```json
{
  "idToken": "eyJhbGciOiJSUzI1NiIsImtpZCI6Ij..."
}
```

**Response:**
```json
{
  "success": true,
  "message": null,
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "refreshToken": "550e8400-e29b-41d4-a716-446655440000"
  }
}
```

**Note**: To get a Google ID token:
1. Use Google Sign-In JavaScript library on frontend
2. Or use Google OAuth 2.0 Playground: https://developers.google.com/oauthplayground/
3. Set the `googleIdToken` environment variable with the token

### 4. Refresh Token
**POST** `/auth/refresh`

Refresh access token using refresh token.

**Request Body:**
```json
{
  "refreshToken": "550e8400-e29b-41d4-a716-446655440000"
}
```

**Response:**
```json
{
  "success": true,
  "message": null,
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "refreshToken": "660e8400-e29b-41d4-a716-446655440001"
  }
}
```

### 5. Forgot Password
**POST** `/auth/forgot-password`

Request a password reset link. Only works for LOCAL users (not Google users).

**Request Body:**
```json
{
  "email": "user@example.com"
}
```

**Response:**
```json
{
  "success": true,
  "message": "If the email exists, a password reset link has been sent.",
  "data": null
}
```

**Note**: 
- In development, check application logs for the reset token
- The response is always the same to prevent email enumeration attacks
- Only LOCAL users can reset passwords (Google users cannot)

### 6. Reset Password
**POST** `/auth/reset-password`

Reset password using the token from the forgot password email.

**Request Body:**
```json
{
  "token": "550e8400-e29b-41d4-a716-446655440000",
  "newPassword": "NewSecurePassword123!"
}
```

**Response:**
```json
{
  "success": true,
  "message": "Password has been reset successfully",
  "data": null
}
```

**Note**: 
- Token expires after 1 hour
- Token can only be used once
- All refresh tokens are invalidated after password reset for security

### 7. Logout
**POST** `/auth/logout`

Logout from current device by invalidating the refresh token.

**Request Body:**
```json
{
  "refreshToken": "550e8400-e29b-41d4-a716-446655440000"
}
```

**Response:**
```json
{
  "success": true,
  "message": "Logged out successfully",
  "data": null
}
```

**Note**: 
- Deletes the refresh token from database
- Access token remains valid until expiry (stateless JWT)
- Client should delete tokens from local storage
- Access tokens cannot be immediately invalidated (they expire naturally)

### 8. Logout All Devices
**POST** `/auth/logout-all`

Logout from all devices by invalidating all refresh tokens for the user.

**Headers:**
```
Authorization: Bearer <accessToken>
```

**Request Body:** None

**Response:**
```json
{
  "success": true,
  "message": "Logged out from all devices successfully",
  "data": null
}
```

**Note**: 
- Requires authentication (JWT access token)
- Deletes all refresh tokens for the authenticated user
- Useful for security breaches, lost devices, or account compromise
- Access tokens remain valid until expiry

## Error Responses

All endpoints return errors in the following format:

```json
{
  "success": false,
  "message": "Error message here",
  "data": null
}
```

**Common Error Codes:**
- `400 Bad Request`: Validation errors (invalid email format, missing fields)
- `401 Unauthorized`: Authentication errors (invalid credentials, expired token)
- `500 Internal Server Error`: Server errors

## Testing Flow

1. **Register a new user** → Tokens are automatically saved to environment
2. **Use access token** → Add to Authorization header: `Bearer {{accessToken}}`
3. **When token expires** → Use Refresh Token endpoint
4. **For Google auth** → Set `googleIdToken` environment variable first
5. **Forgot password** → Request reset link, check logs for token, then reset password
6. **Logout** → Use Logout endpoint to invalidate refresh token
7. **Logout all devices** → Use Logout All endpoint (requires authentication)

## Security Notes

- Access tokens are short-lived (default: 1 hour)
- Refresh tokens are long-lived (30 days)
- Always use HTTPS in production
- Never expose refresh tokens in client-side code
- Store tokens securely
- **Logout behavior**: Refresh tokens are deleted, but access tokens remain valid until expiry (stateless JWT limitation)
- **Client responsibility**: Always delete tokens from local storage after logout

## Using Tokens in Other Requests

To use the access token in other API requests:

1. Add Authorization header:
   ```
   Authorization: Bearer {{accessToken}}
   ```

2. Or use Postman's Authorization tab:
   - Type: Bearer Token
   - Token: `{{accessToken}}`


