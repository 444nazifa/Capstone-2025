# Tests and Demo Scripts

This directory contains test files and demonstration scripts for the QR Code Reader backend.

## Test Files

- **test_api.py** - Tests for the Flask API endpoints
- **test_nexium_format.py** - Tests for parsing Nexium prescription format
- **test_prescription_qr.py** - Tests for QR code reading functionality

## Demo Scripts

These scripts demonstrate various image processing techniques used for QR code detection:

- **gamma_comparison.py** - Compare different gamma correction values
- **simple_gamma_demo.py** - Simple demonstration of gamma correction
- **simple_side_by_side.py** - Side-by-side comparison of image preprocessing
- **simple_visual_demo.py** - Visual demonstration of QR detection process
- **visual_gamma_demo.py** - Visual gamma correction demonstration

## Running Tests

```bash
# Install dependencies first
pip install -r ../requirements.txt

# Install additional test dependencies
pip install pytest requests matplotlib

# Run specific test
python test_api.py

# Run all tests with pytest
pytest
```

## Running Demos

```bash
# Make sure you have matplotlib installed for visualizations
pip install matplotlib

# Run any demo script
python gamma_comparison.py
```

## Note

These files are excluded from deployment via `.vercelignore` as they require additional dependencies (matplotlib, requests) that are not needed for the production API.
