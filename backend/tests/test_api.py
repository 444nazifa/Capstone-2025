#!/usr/bin/env python3
"""
Test script for the prescription QR code API
"""

import requests
import json
import base64
from pathlib import Path

def test_api_endpoints():
    """Test all API endpoints"""
    
    base_url = "http://localhost:5000"
    
    print("Testing Prescription QR Code Reader API")
    print("=" * 50)
    
    # Test health endpoint
    print("\n1. Testing health endpoint...")
    try:
        response = requests.get(f"{base_url}/health")
        print(f"Status: {response.status_code}")
        print(f"Response: {response.json()}")
    except requests.exceptions.ConnectionError:
        print("❌ API server not running. Start it with: python prescription_api.py")
        return
    
    # Test file upload endpoint
    print("\n2. Testing file upload endpoint...")
    if Path("test_prescription_json.png").exists():
        with open("test_prescription_json.png", "rb") as f:
            files = {"image": f}
            response = requests.post(f"{base_url}/api/scan-qr", files=files)
            print(f"Status: {response.status_code}")
            print(f"Response: {json.dumps(response.json(), indent=2)}")
    else:
        print("❌ Test QR code image not found. Run test_prescription_qr.py first.")
    
    # Test base64 image endpoint
    print("\n3. Testing base64 image endpoint...")
    if Path("test_prescription_kv.png").exists():
        with open("test_prescription_kv.png", "rb") as f:
            image_data = base64.b64encode(f.read()).decode('utf-8')
            
        payload = {
            "image": f"data:image/png;base64,{image_data}"
        }
        
        response = requests.post(
            f"{base_url}/api/scan-qr",
            json=payload,
            headers={"Content-Type": "application/json"}
        )
        print(f"Status: {response.status_code}")
        print(f"Response: {json.dumps(response.json(), indent=2)}")
    else:
        print("❌ Test QR code image not found. Run test_prescription_qr.py first.")
    
    # Test parse QR text endpoint
    print("\n4. Testing parse QR text endpoint...")
    test_qr_text = """PATIENT: Test Patient
DOB: 1980-01-01
MEDICATION: Test Medication 5mg
NDC: 1234-5678-90
RX: 555123"""
    
    payload = {"qr_text": test_qr_text}
    response = requests.post(
        f"{base_url}/api/parse-qr-text",
        json=payload,
        headers={"Content-Type": "application/json"}
    )
    print(f"Status: {response.status_code}")
    print(f"Response: {json.dumps(response.json(), indent=2)}")
    
    # Test validation endpoint
    print("\n5. Testing validation endpoint...")
    test_prescription = {
        "patient_name": "Test Patient",
        "medication_name": "Test Med",
        "ndc_number": "1234-567-89"  # Invalid format
    }
    
    response = requests.post(
        f"{base_url}/api/validate-prescription",
        json=test_prescription,
        headers={"Content-Type": "application/json"}
    )
    print(f"Status: {response.status_code}")
    print(f"Response: {json.dumps(response.json(), indent=2)}")
    
    print("\n✅ API testing completed!")

if __name__ == "__main__":
    test_api_endpoints()