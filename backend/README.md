# Prescription QR Code Reader

A Python-based QR code reader specifically designed for USA prescription QR codes. Includes both a command-line interface and a REST API for mobile frontend integration.

## Features

- **QR Code Reading**: Supports both camera input and image files
- **Prescription Parsing**: Parses common prescription data formats (JSON and key-value)
- **Data Validation**: Validates prescription data for completeness and format
- **REST API**: Mobile-friendly API endpoints for image upload and processing
- **Multiple Input Formats**: File upload, base64 images, and direct QR text parsing

## Installation

1. **Clone or download the project files**

2. **Install system dependencies** (macOS):
```bash
brew install zbar
```

3. **Set up Python virtual environment**:
```bash
python3 -m venv venv
source venv/bin/activate
pip install -r requirements.txt
```

## Usage

### Command Line Interface

**Read QR code from camera:**
```bash
python prescription_qr_reader.py
```

**Read QR code from image file:**
```bash
python prescription_qr_reader.py -i path/to/qr_image.png
```

### REST API

**Start the API server:**
```bash
python prescription_api.py
```

The API will be available at `http://localhost:5000`

#### API Endpoints

1. **Health Check**
   - `GET /health`
   - Returns API status

2. **Scan QR Code from Image**
   - `POST /api/scan-qr`
   - Accepts file upload or base64 image data
   - Returns parsed prescription data

3. **Parse QR Text Directly**
   - `POST /api/parse-qr-text`
   - Accepts QR code text content
   - Returns parsed prescription data

4. **Validate Prescription Data**
   - `POST /api/validate-prescription`
   - Validates prescription data structure
   - Returns validation results

#### Example API Usage

**File Upload:**
```bash
curl -X POST -F "image=@test_prescription.png" http://localhost:5000/api/scan-qr
```

**Base64 Image:**
```bash
curl -X POST -H "Content-Type: application/json" \
  -d '{"image":"data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAA..."}' \
  http://localhost:5000/api/scan-qr
```

**QR Text Parsing:**
```bash
curl -X POST -H "Content-Type: application/json" \
  -d '{"qr_text":"PATIENT: John Doe\nMEDICATION: Aspirin 81mg"}' \
  http://localhost:5000/api/parse-qr-text
```

## Supported Prescription Data Fields

The system can parse and validate the following prescription information:

- **Patient Information**: Name, Date of Birth
- **Medication Details**: Name, Strength, NDC Number
- **Prescriber Information**: Name/Title
- **Pharmacy Information**: Name
- **Prescription Details**: RX Number, Date Filled, Quantity, Refills
- **Directions**: Usage instructions

## Data Formats

### JSON Format
```json
{
  "patient_name": "John Smith",
  "patient_dob": "1985-03-15",
  "medication_name": "Lisinopril 10mg Tablets",
  "ndc_number": "0378-1805-01",
  "prescriber_name": "Dr. Sarah Johnson, MD",
  "rx_number": "1234567",
  "directions": "Take one tablet by mouth once daily"
}
```

### Key-Value Format
```
PATIENT: John Smith
DOB: 1985-03-15
MEDICATION: Lisinopril 10mg Tablets
NDC: 0378-1805-01
PRESCRIBER: Dr. Sarah Johnson, MD
RX: 1234567
DIRECTIONS: Take one tablet by mouth once daily
```

## Testing

**Generate test QR codes and test parsing:**
```bash
python test_prescription_qr.py
```

**Test API endpoints:**
```bash
# Start API server in one terminal
python prescription_api.py

# Run tests in another terminal
python test_api.py
```

## File Structure

```
prescription-qr-reader/
├── prescription_qr_reader.py  # Main QR reader class
├── prescription_api.py        # REST API server
├── test_prescription_qr.py    # QR code generation and parsing tests
├── test_api.py               # API endpoint tests
├── requirements.txt          # Python dependencies
└── README.md                # This file
```

## Dependencies

- **opencv-python**: Computer vision and camera access
- **pyzbar**: QR/barcode decoding
- **numpy**: Array processing
- **qrcode[pil]**: QR code generation for testing
- **flask**: Web API framework
- **pillow**: Image processing
- **requests**: HTTP client for testing

## API Response Format

```json
{
  "success": true,
  "qr_detected": true,
  "image_source": "file_upload",
  "prescription_data": {
    "patient_name": "John Smith",
    "medication_name": "Lisinopril 10mg Tablets",
    "ndc_number": "0378-1805-01",
    "rx_number": "1234567"
  },
  "validation": {
    "is_valid": true,
    "issues": []
  },
  "raw_qr_data": "original QR code content"
}
```

## Error Handling

The API provides detailed error messages for:
- Invalid image formats
- File size limits (16MB max)
- Missing required data
- Validation failures
- Processing errors

## Security Considerations

- File uploads are validated for type and size
- Temporary files are automatically cleaned up
- No sensitive data is logged by default
- CORS can be configured for production use

## Production Deployment

For production use, consider:
- Adding authentication/authorization
- Implementing rate limiting
- Using a production WSGI server (e.g., Gunicorn)
- Adding SSL/TLS encryption
- Implementing proper logging and monitoring
- Adding database storage for prescription records

## License

This project is designed for healthcare and pharmacy applications. Ensure compliance with relevant healthcare data protection regulations (HIPAA, etc.) when handling prescription information.