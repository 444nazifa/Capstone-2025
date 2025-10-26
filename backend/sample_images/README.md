# Sample Images

This directory contains sample prescription images and QR codes for testing the QR Code Reader API.

## Test QR Codes

- **test_prescription_json.png** - Sample QR code with JSON format prescription data
- **test_prescription_kv.png** - Sample QR code with key-value format prescription data
- **nexium.png** - Nexium prescription QR code

## Sample Prescription Photos

These images test various challenging conditions for QR code detection:

- **12.jpg** - Real prescription label photo (1.9 MB - large file)
- **realistic_prescription_photo.jpg** - Realistic prescription photo
- **prescription_photo_blurry.jpg** - Tests QR detection with blurred images
- **prescription_photo_dark.jpg** - Tests QR detection in low-light conditions
- **prescription_photo_rotated.jpg** - Tests QR detection with rotated images

## Demo Output

- **gamma_comparison.png** - Gamma correction comparison output from demos

## Usage

These images can be used with the test scripts in the `tests/` folder or for manual API testing:

```bash
# Test with curl
curl -X POST -F "image=@sample_images/test_prescription_json.png" \
  http://localhost:3002/api/scan-qr

# Test with Python
python tests/test_api.py
```

## Note

These files are excluded from Vercel deployment via `.vercelignore` to keep the deployment package small and fast.
