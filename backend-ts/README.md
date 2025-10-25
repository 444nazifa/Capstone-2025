# QR Reader Backend (TypeScript)

A TypeScript backend API for the QR code reader application with Supabase authentication.

## Features

- User registration and login
- JWT-based authentication
- Input validation with Joi
- Rate limiting and security middleware
- TypeScript for type safety
- Supabase integration for database and auth

## Setup

1. Install dependencies:
```bash
npm install
```

2. Set up environment variables:
```bash
cp .env.example .env
```

3. Fill in your Supabase credentials in `.env`:
- `SUPABASE_URL`: Your Supabase project URL
- `SUPABASE_ANON_KEY`: Your Supabase anon key
- `SUPABASE_SERVICE_ROLE_KEY`: Your Supabase service role key
- `JWT_SECRET`: A secure random string for JWT signing
- `JWT_EXPIRES_IN`: JWT token expiration time (e.g., "7d", "24h", "1h")

## Development

```bash
npm run dev
```

## Production

```bash
npm run build
npm start
```

## API Endpoints

### Authentication

#### POST /api/auth/register
Register a new user.

**Request body:**
```json
{
  "name": "John Doe",
  "email": "john@example.com",
  "password": "SecurePass123!",
  "date_of_birth": "1990-01-01"
}
```

**Response:**
```json
{
  "success": true,
  "message": "User registered successfully",
  "user": {
    "id": "uuid",
    "email": "john@example.com",
    "name": "John Doe",
    "date_of_birth": "1990-01-01"
  },
  "token": "jwt_token"
}
```

#### POST /api/auth/login
Login with existing credentials.

**Request body:**
```json
{
  "email": "john@example.com",
  "password": "SecurePass123!"
}
```

**Response:**
```json
{
  "success": true,
  "message": "Login successful",
  "user": {
    "id": "uuid",
    "email": "john@example.com",
    "name": "John Doe",
    "date_of_birth": "1990-01-01"
  },
  "token": "jwt_token"
}
```

### Health Check

#### GET /health
Check if the server is running.

**Response:**
```json
{
  "success": true,
  "message": "Server is running",
  "timestamp": "2023-12-07T10:00:00.000Z"
}
```

## Password Requirements

- Minimum 8 characters
- At least one uppercase letter
- At least one lowercase letter
- At least one number
- At least one special character (@$!%*?&)