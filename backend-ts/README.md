# QR Reader Backend (TypeScript)

A TypeScript backend API for the QR code reader application with Supabase authentication.

## Features

- User registration and login
- JWT-based authentication
- Medication database search via DailyMed API
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

### Medication Search

#### GET /api/medication/search
Search for medications by name.

**Query parameters:**
- `query` (required): Medication name to search for
- `limit` (optional): Number of results to return (default: 20)
- `offset` (optional): Number of results to skip (default: 0)

**Example:**
```
GET /api/medication/search?query=aspirin&limit=5
```

**Response:**
```json
{
  "success": true,
  "data": [
    {
      "setId": "23b29044-6389-42ea-839a-5351557a6ce3",
      "title": "ASPIRIN 81 MG LOW DOSE (ASPIRIN) TABLET, DELAYED RELEASE [CVS WOONSOCKET PRESCRIPTION CENTER, INCORPORATED]",
      "ndc": [],
      "labeler": "Unknown",
      "published": "2024-01-15",
      "updated": "2024-01-15"
    }
  ],
  "total": 100
}
```

#### GET /api/medication/search/ndc
Search for medications by NDC number.

**Query parameters:**
- `ndc` (required): NDC number to search for

**Example:**
```
GET /api/medication/search/ndc?ndc=12345-678-90
```

#### GET /api/medication/details/:setId
Get detailed information about a specific medication.

**Parameters:**
- `setId` (required): The medication set ID

**Example:**
```
GET /api/medication/details/23b29044-6389-42ea-839a-5351557a6ce3
```

#### GET /api/medication/autocomplete
Get autocomplete suggestions for medication names.

**Query parameters:**
- `term` (required): Partial medication name
- `limit` (optional): Number of suggestions to return (default: 10)

**Example:**
```
GET /api/medication/autocomplete?term=asp&limit=5
```