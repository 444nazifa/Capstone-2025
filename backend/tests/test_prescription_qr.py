#!/usr/bin/env python3
"""
Test script for prescription QR code reader
Creates sample QR codes and tests parsing functionality
"""

import qrcode
import json
from prescription_qr_reader import PrescriptionQRReader

def create_test_qr_codes():
    """Create sample prescription QR codes for testing"""
    
    # Sample prescription data in JSON format
    prescription_data_json = {
        "patient_name": "John Smith",
        "patient_dob": "1985-03-15",
        "medication_name": "Lisinopril 10mg Tablets",
        "medication_strength": "10mg",
        "ndc_number": "0378-1805-01",
        "prescriber_name": "Dr. Sarah Johnson, MD",
        "pharmacy_name": "Main Street Pharmacy",
        "rx_number": "1234567",
        "date_filled": "2025-01-15",
        "directions": "Take one tablet by mouth once daily",
        "quantity": "30 tablets",
        "refills": "5"
    }
    
    # Sample prescription data in key-value format
    prescription_data_kv = """PATIENT: Jane Doe
DOB: 1990-07-22
MEDICATION: Metformin 500mg Tablets
STRENGTH: 500mg
NDC: 0093-1095-01
PRESCRIBER: Dr. Michael Chen, MD
PHARMACY: Westside Pharmacy
RX: 9876543
FILLED: 2025-01-14
DIRECTIONS: Take one tablet by mouth twice daily with meals
QTY: 60 tablets
REFILLS: 3"""
    
    # Create QR codes
    qr_json = qrcode.QRCode(version=1, box_size=10, border=5)
    qr_json.add_data(json.dumps(prescription_data_json))
    qr_json.make(fit=True)
    img_json = qr_json.make_image(fill_color="black", back_color="white")
    img_json.save("test_prescription_json.png")
    
    qr_kv = qrcode.QRCode(version=1, box_size=10, border=5)
    qr_kv.add_data(prescription_data_kv)
    qr_kv.make(fit=True)
    img_kv = qr_kv.make_image(fill_color="black", back_color="white")
    img_kv.save("test_prescription_kv.png")
    
    print("Test QR codes created:")
    print("- test_prescription_json.png (JSON format)")
    print("- test_prescription_kv.png (Key-Value format)")
    
    return json.dumps(prescription_data_json), prescription_data_kv

def test_parsing():
    """Test the prescription data parsing functionality"""
    reader = PrescriptionQRReader()
    
    json_data, kv_data = create_test_qr_codes()
    
    print("\n" + "="*60)
    print("TESTING JSON FORMAT PARSING")
    print("="*60)
    reader.process_qr_data(json_data)
    
    print("\n" + "="*60)
    print("TESTING KEY-VALUE FORMAT PARSING")
    print("="*60)
    reader.process_qr_data(kv_data)

if __name__ == "__main__":
    test_parsing()