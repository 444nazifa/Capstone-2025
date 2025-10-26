#!/usr/bin/env python3
"""
Test script for Nexium XML/HTML prescription format
"""

from prescription_qr_reader import PrescriptionQRReader

def test_nexium_format():
    """Test the Nexium prescription QR format"""
    
    # Your actual Nexium QR code data
    nexium_qr_data = "<p><n>Paul Smith</n><dg>Nexium Hp7 Pack 14+14+28</dg><in>utd</in><id>test </id><pm>test</pm><dt>16/03/2019</dt></p>"
    
    print("Testing Nexium Prescription QR Format")
    print("=" * 50)
    
    reader = PrescriptionQRReader()
    
    print("\nRaw QR Code Data:")
    print("-" * 30)
    print(nexium_qr_data)
    print()
    
    # Parse the prescription data
    parsed_data = reader.parse_prescription_data(nexium_qr_data)
    is_valid, issues = reader.validate_prescription_data(parsed_data)
    
    # Display formatted output
    print(reader.format_prescription_output(parsed_data))
    
    if not is_valid:
        print("\nValidation Issues:")
        for issue in issues:
            print(f"- {issue}")
    else:
        print("\n✓ Prescription data appears valid")
    
    # Test other XML variations
    print("\n" + "="*60)
    print("TESTING OTHER XML VARIATIONS")
    print("="*60)
    
    # Test with attributes
    xml_with_attrs = '<prescription><n type="patient">John Doe</n><dg class="medication">Amoxicillin 500mg</dg><dt format="dd/mm/yyyy">15/03/2025</dt></prescription>'
    
    print("\nTesting XML with attributes:")
    reader.process_qr_data(xml_with_attrs)
    
    # Test malformed XML (should fall back to regex)
    malformed_xml = '<p><n>Jane Smith</n><dg>Aspirin 100mg<in>Take daily</in><dt>20/03/2025</dt></p>'
    
    print("\nTesting malformed XML (regex fallback):")
    reader.process_qr_data(malformed_xml)

if __name__ == "__main__":
    test_nexium_format()