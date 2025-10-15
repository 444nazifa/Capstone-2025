# DailyMed API

A TypeScript API wrapper for accessing DailyMed medication data.

## Features

- Search medications by name
- Search medications by NDC number
- Get detailed medication information
- TypeScript support with full type definitions
- Express.js REST API endpoints

## Installation

```bash
npm install
```

## Development

```bash
npm run dev
```

## Build

```bash
npm run build
npm start
```

## API Endpoints

### Search by Medication Name
```
GET /api/v1/search/medication?query=aspirin&limit=10&offset=0
```

Returns: Array of medications with title, NDC, image, and metadata.

### Search by NDC Number
```
GET /api/v1/search/ndc?ndc=0378-0781-05
```

Returns: Medications matching the NDC number.

### Get Medication Details
```
GET /api/v1/medication/{setId}
```

Returns: Complete medication details including all sections, ingredients, images, and labeling information.

### Autocomplete
```
GET /api/v1/autocomplete?term=aspirin&limit=10
```

Returns: Autocomplete suggestions for medication names.

## Response Format

All endpoints return responses in this format:
```json
{
  "success": boolean,
  "data": any,
  "error": string (if success is false),
  "total": number (for search results)
}
```