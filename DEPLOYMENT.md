# Deployment Guide for QR Code Reader APIs

This guide covers deploying both the TypeScript backend (medication API) and Python backend (QR code scanner) to Vercel.

## Prerequisites

1. **Vercel Account**: Sign up at [vercel.com](https://vercel.com)
2. **Vercel CLI**: Install globally: `npm install -g vercel`
3. **Git Repository**: Push your code to GitHub, GitLab, or Bitbucket

## TypeScript Backend (Medication API) Deployment

### 1. Environment Variables

Before deploying, set up these environment variables in Vercel dashboard:

```bash
# Supabase Configuration
SUPABASE_URL=your_supabase_project_url
SUPABASE_ANON_KEY=your_supabase_anon_key
SUPABASE_SERVICE_ROLE_KEY=your_supabase_service_role_key

# JWT Configuration
JWT_SECRET=your_secure_jwt_secret_key
JWT_EXPIRES_IN=7d

# Environment
NODE_ENV=production
```

### 2. Deploy via Vercel Dashboard

1. Go to [vercel.com/dashboard](https://vercel.com/dashboard)
2. Click "New Project"
3. Import your repository
4. Select the `backend-ts` folder as the root directory
5. Vercel will automatically detect it as a Node.js project
6. Add your environment variables in the "Environment Variables" section
7. Click "Deploy"

### 3. Deploy via CLI

```bash
cd backend-ts
vercel

# Follow the prompts:
# ? Set up and deploy "~/projects/python/qr-code-reader/backend-ts"? [Y/n] Y
# ? Which scope should contain your project? [Your Account]
# ? Link to existing project? [y/N] N
# ? What's your project's name? qr-reader-medication-api
# ? In which directory is your code located? ./

# Add environment variables
vercel env add SUPABASE_URL
vercel env add SUPABASE_ANON_KEY
vercel env add SUPABASE_SERVICE_ROLE_KEY
vercel env add JWT_SECRET
vercel env add JWT_EXPIRES_IN
vercel env add NODE_ENV

# Deploy to production
vercel --prod
```

### 4. API Endpoints (TypeScript)

Your deployed medication API will be available at:
- `https://your-deployment-url.vercel.app/health`
- `https://your-deployment-url.vercel.app/api/auth/register`
- `https://your-deployment-url.vercel.app/api/auth/login`
- `https://your-deployment-url.vercel.app/api/medication/search`
- `https://your-deployment-url.vercel.app/api/medication/details/:setId`
- `https://your-deployment-url.vercel.app/api/medication/autocomplete`

## Python Backend (QR Scanner API) Deployment

### Important Notes

**Dependencies:**
- The backend uses `opencv-python-headless` instead of `opencv-python` to reduce deployment size and avoid GUI dependencies
- This has been tested and verified to work correctly with all image processing features
- The API does not use camera functionality (which requires GUI libraries)

**QR Code Detection:**
- **Primary method**: OpenCV's built-in `cv2.QRCodeDetector()` (always available, works on all platforms including Vercel)
- **Fallback method**: pyzbar (optional, requires `zbar` system library - not available on Vercel)
- The API will automatically use the best available method
- Both methods are tested and working correctly
- **Note**: pyzbar is commented out in `requirements.txt` to avoid deployment issues on Vercel

**Text Detection (OCR for NDC/RX Numbers):**
- **Method**: pytesseract with tesseract OCR engine
- **Status on Vercel**: pytesseract package will install, but tesseract binary is not available
- **Behavior**:
  - **Local/VPS with tesseract installed**: Full OCR text detection works (extracts NDC and RX numbers from images)
  - **Vercel deployment**: Text detection gracefully disabled, QR detection still works
- The `/health` endpoint reports text detection availability
- If tesseract binary is missing, the API won't crash - it just won't extract text from images

**Files Excluded from Deployment:**
- `tests/` folder containing all test files and demo scripts
- Sample images and test data
- Development files and logs
- Virtual environment (venv/)
- Python cache files (__pycache__/)

### 1. Deploy via Vercel Dashboard

1. Go to [vercel.com/dashboard](https://vercel.com/dashboard)
2. Click "New Project"
3. Import your repository
4. Select the `backend` folder as the root directory
5. Vercel will automatically detect it as a Python project
6. Click "Deploy"

### 2. Deploy via CLI

```bash
cd backend
vercel

# Follow the prompts:
# ? Set up and deploy "~/projects/python/qr-code-reader/backend"? [Y/n] Y
# ? Which scope should contain your project? [Your Account]
# ? Link to existing project? [y/N] N
# ? What's your project's name? qr-reader-scanner-api
# ? In which directory is your code located? ./

# Deploy to production
vercel --prod
```

### 3. API Endpoints (Python)

Your deployed QR scanner API will be available at:
- `https://your-deployment-url.vercel.app/health`
- `https://your-deployment-url.vercel.app/api/scan-qr`
- `https://your-deployment-url.vercel.app/api/validate-prescription`
- `https://your-deployment-url.vercel.app/api/parse-qr-text`

## Configuration Files Created

### TypeScript Backend (`backend-ts/vercel.json`)
```json
{
  "version": 2,
  "builds": [
    {
      "src": "src/index.ts",
      "use": "@vercel/node"
    }
  ],
  "routes": [
    {
      "src": "/(.*)",
      "dest": "src/index.ts"
    }
  ],
  "env": {
    "NODE_ENV": "production"
  },
  "functions": {
    "src/index.ts": {
      "maxDuration": 30
    }
  }
}
```

### Python Backend (`backend/vercel.json`)
```json
{
  "version": 2,
  "builds": [
    {
      "src": "api/index.py",
      "use": "@vercel/python"
    }
  ],
  "routes": [
    {
      "src": "/(.*)",
      "dest": "api/index.py"
    }
  ],
  "functions": {
    "api/index.py": {
      "maxDuration": 60
    }
  }
}
```

### Python Entry Point (`backend/api/index.py`)
```python
#!/usr/bin/env python3

# Vercel-compatible entry point for the prescription API
import sys
import os

# Add the parent directory to the Python path so we can import our modules
sys.path.append(os.path.join(os.path.dirname(__file__), '..'))

from prescription_api import app

# For Vercel, we need to export the Flask app
# Vercel expects a WSGI-compatible application
if __name__ == "__main__":
    app.run()
else:
    # This is the WSGI application that Vercel will use
    application = app
```

## Testing Deployments

### TypeScript Backend Test
```bash
# Health check
curl https://your-medication-api.vercel.app/health

# Search medications
curl "https://your-medication-api.vercel.app/api/medication/search?query=aspirin&limit=1"
```

### Python Backend Test
```bash
# Health check - shows available capabilities
curl https://your-scanner-api.vercel.app/health

# Expected response on Vercel:
# {
#   "status": "healthy",
#   "capabilities": {
#     "qr_detection": true,
#     "qr_detection_method": "opencv",
#     "text_detection": false,
#     "text_detection_method": "unavailable",
#     "opencv_version": "4.12.0"
#   },
#   "features": {
#     "qr_code_scanning": "available",
#     "ndc_extraction": "qr_only",
#     "rx_number_extraction": "qr_only"
#   }
# }

# Test QR code scanning (works on Vercel)
curl -X POST https://your-scanner-api.vercel.app/api/scan-qr \
  -F "image=@prescription_qr.png"
```

## Troubleshooting

### Common Issues

1. **Build Failures**
   - Check that all dependencies are in `package.json` (TypeScript) or `requirements.txt` (Python)
   - Ensure environment variables are set correctly

2. **Function Timeouts**
   - Python functions have a 60-second timeout for complex image processing
   - TypeScript functions have a 30-second timeout for API calls

3. **Import Errors (Python)**
   - The `api/index.py` entry point handles Python path configuration
   - Ensure all modules are in the correct relative paths

4. **CORS Issues**
   - Both APIs include CORS middleware
   - Update origins if needed for production

### Environment Variables Security

- Never commit environment variables to your repository
- Use Vercel's environment variable management
- For Supabase keys, use the service role key for server-side operations
- Generate a strong, unique JWT secret

### Monitoring

- Use Vercel's built-in analytics and logs
- Monitor function execution times and errors
- Set up alerts for API failures

## Custom Domains (Optional)

1. Go to your project settings in Vercel
2. Add your custom domain
3. Configure DNS records as instructed
4. Vercel will automatically provision SSL certificates

## Scaling Considerations

- Vercel automatically scales based on demand
- Consider upgrading to Pro plan for production workloads
- Monitor usage and costs in the Vercel dashboard
- Both APIs are stateless and scale horizontally

## Support

- TypeScript API: Handles medication search, user authentication, and detailed prescribing information
- Python API: Handles QR code scanning, image processing, and prescription validation
- Both APIs can be deployed independently and used together or separately