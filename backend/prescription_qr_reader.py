#!/usr/bin/env python3

import cv2
import numpy as np
from pyzbar import pyzbar
from pyzbar.pyzbar import ZBarSymbol
import json
import re
from datetime import datetime
from typing import Dict, List, Optional, Tuple
import argparse
import sys
import xml.etree.ElementTree as ET
from xml.etree.ElementTree import ParseError
try:
    import pytesseract
    TESSERACT_AVAILABLE = True
except ImportError:
    TESSERACT_AVAILABLE = False
    print("Warning: pytesseract not available. NDC detection from text will be disabled.")


class PrescriptionQRReader:
    def __init__(self):
        self.cap = None

    def preprocess_image_for_qr(self, image: np.ndarray) -> List[np.ndarray]:
        processed_images = []

        # Original image
        processed_images.append(image.copy())

        gray = cv2.cvtColor(image, cv2.COLOR_BGR2GRAY)
        processed_images.append(cv2.cvtColor(gray, cv2.COLOR_GRAY2BGR))

        blurred = cv2.GaussianBlur(gray, (5, 5), 0)
        processed_images.append(cv2.cvtColor(blurred, cv2.COLOR_GRAY2BGR))

        adaptive_thresh = cv2.adaptiveThreshold(
            gray, 255, cv2.ADAPTIVE_THRESH_GAUSSIAN_C, cv2.THRESH_BINARY, 11, 2
        )
        processed_images.append(cv2.cvtColor(
            adaptive_thresh, cv2.COLOR_GRAY2BGR))

        _, otsu_thresh = cv2.threshold(
            gray, 0, 255, cv2.THRESH_BINARY + cv2.THRESH_OTSU)
        processed_images.append(cv2.cvtColor(otsu_thresh, cv2.COLOR_GRAY2BGR))

        equalized = cv2.equalizeHist(gray)
        processed_images.append(cv2.cvtColor(equalized, cv2.COLOR_GRAY2BGR))

        kernel = cv2.getStructuringElement(cv2.MORPH_RECT, (3, 3))
        morph = cv2.morphologyEx(gray, cv2.MORPH_CLOSE, kernel)
        processed_images.append(cv2.cvtColor(morph, cv2.COLOR_GRAY2BGR))

        edges = cv2.Canny(gray, 50, 150)
        processed_images.append(cv2.cvtColor(edges, cv2.COLOR_GRAY2BGR))

        for gamma in [0.5, 1.5, 2.0]:
            gamma_corrected = self.adjust_gamma(gray, gamma)
            processed_images.append(cv2.cvtColor(
                gamma_corrected, cv2.COLOR_GRAY2BGR))

        return processed_images

    def adjust_gamma(self, image: np.ndarray, gamma: float = 1.0) -> np.ndarray:
        inv_gamma = 1.0 / gamma
        table = np.array([((i / 255.0) ** inv_gamma) *
                         255 for i in np.arange(0, 256)]).astype("uint8")
        return cv2.LUT(image, table)

    def detect_qr_with_contours(self, image: np.ndarray) -> Optional[np.ndarray]:
        gray = cv2.cvtColor(image, cv2.COLOR_BGR2GRAY)

        blurred = cv2.GaussianBlur(gray, (5, 5), 0)
        _, thresh = cv2.threshold(
            blurred, 0, 255, cv2.THRESH_BINARY + cv2.THRESH_OTSU)

        contours, _ = cv2.findContours(
            thresh, cv2.RETR_EXTERNAL, cv2.CHAIN_APPROX_SIMPLE)

        for contour in contours:
            area = cv2.contourArea(contour)
            if area < 1000:
                continue

            epsilon = 0.02 * cv2.arcLength(contour, True)
            approx = cv2.approxPolyDP(contour, epsilon, True)

            if len(approx) == 4:
                x, y, w, h = cv2.boundingRect(contour)

                aspect_ratio = float(w) / h
                if 0.7 <= aspect_ratio <= 1.3:
                    # Extract the region
                    roi = image[y:y+h, x:x+w]
                    if roi.size > 0:
                        return roi

        return None

    def enhanced_qr_detection(self, image: np.ndarray) -> Optional[str]:
        decoded_objects = pyzbar.decode(image, symbols=[ZBarSymbol.QRCODE])
        if decoded_objects:
            return decoded_objects[0].data.decode('utf-8')

        processed_images = self.preprocess_image_for_qr(image)

        for processed_img in processed_images:
            decoded_objects = pyzbar.decode(
                processed_img, symbols=[ZBarSymbol.QRCODE])
            if decoded_objects:
                return decoded_objects[0].data.decode('utf-8')

        # Strategy 3: Try to detect QR region using contours first
        qr_region = self.detect_qr_with_contours(image)
        if qr_region is not None:
            # Try to decode the extracted region
            decoded_objects = pyzbar.decode(
                qr_region, symbols=[ZBarSymbol.QRCODE])
            if decoded_objects:
                return decoded_objects[0].data.decode('utf-8')

            # If direct decoding fails, preprocess the region
            processed_regions = self.preprocess_image_for_qr(qr_region)
            for processed_region in processed_regions:
                decoded_objects = pyzbar.decode(
                    processed_region, symbols=[ZBarSymbol.QRCODE])
                if decoded_objects:
                    return decoded_objects[0].data.decode('utf-8')

        for scale in [0.5, 1.5, 2.0]:
            height, width = image.shape[:2]
            new_width = int(width * scale)
            new_height = int(height * scale)

            if new_width > 50 and new_height > 50:  # Avoid too small images
                resized = cv2.resize(
                    image, (new_width, new_height), interpolation=cv2.INTER_CUBIC)
                decoded_objects = pyzbar.decode(
                    resized, symbols=[ZBarSymbol.QRCODE])
                if decoded_objects:
                    return decoded_objects[0].data.decode('utf-8')

        return None

    def detect_prescription_info_from_text(self, image: np.ndarray) -> Optional[Dict]:
        """
        Use OCR to detect NDC numbers and RX numbers from prescription label text as fallback
        NDC format: XXXXX-XXXX-XX or XXXX-XXXX-XX
        RX format: Various patterns like "Rx #123456", "Prescription: 123456", etc.
        Returns dict with 'ndc' and 'rx_number' keys, or None if nothing found
        """
        if not TESSERACT_AVAILABLE:
            return None
        
        try:
            # Resize large images for faster processing, but not too aggressively
            height, width = image.shape[:2]
            max_dimension = 2000  # Less aggressive resize - keep more detail for text
            
            if max(height, width) > max_dimension:
                # Calculate scaling factor to keep aspect ratio
                scale_factor = max_dimension / max(height, width)
                new_width = int(width * scale_factor)
                new_height = int(height * scale_factor)
                
                print(f"Resizing from {width}x{height} to {new_width}x{new_height} for faster processing...")
                # Use INTER_AREA for downscaling to preserve text quality
                image = cv2.resize(image, (new_width, new_height), interpolation=cv2.INTER_AREA)
            else:
                print(f"Image size {width}x{height} is reasonable for OCR, keeping original size")
            
            # Preprocess image for better OCR
            processed_images = []
            
            # Convert to grayscale
            gray = cv2.cvtColor(image, cv2.COLOR_BGR2GRAY)
            processed_images.append(gray)
            
            # Apply different preprocessing techniques
            # 1. Gaussian blur + threshold
            blurred = cv2.GaussianBlur(gray, (5, 5), 0)
            _, thresh1 = cv2.threshold(blurred, 0, 255, cv2.THRESH_BINARY + cv2.THRESH_OTSU)
            processed_images.append(thresh1)
            
            # 2. Adaptive threshold
            adaptive = cv2.adaptiveThreshold(gray, 255, cv2.ADAPTIVE_THRESH_GAUSSIAN_C, cv2.THRESH_BINARY, 11, 2)
            processed_images.append(adaptive)
            
            # 3. Morphological operations to clean up text
            kernel = cv2.getStructuringElement(cv2.MORPH_RECT, (2, 2))
            morph = cv2.morphologyEx(thresh1, cv2.MORPH_CLOSE, kernel)
            processed_images.append(morph)
            
            # 4. Add one upscaling option to help with small text
            # Only upscale if the image is reasonably sized after initial resize
            current_height, current_width = gray.shape
            if current_width < 2500:  # Only upscale if not already very large
                upscaled = cv2.resize(gray, (int(current_width * 1.5), int(current_height * 1.5)), interpolation=cv2.INTER_CUBIC)
                processed_images.append(upscaled)
            
            # 5. Rotation handling for rotated images (like 12.jpg)
            # Only try common rotations to balance speed vs accuracy
            for angle in [90, 270]:  # Skip 180 since it's usually less common
                height, width = gray.shape
                center = (width // 2, height // 2)
                rotation_matrix = cv2.getRotationMatrix2D(center, angle, 1.0)
                rotated = cv2.warpAffine(gray, rotation_matrix, (width, height))
                processed_images.append(rotated)
            
            # Try OCR on each processed image
            for processed_img in processed_images:
                try:
                    # Use fewer PSM modes for faster processing
                    psm_modes = [6, 8]  # Reduced from 4 to 2 most effective modes
                    found_info = {}
                    
                    for psm in psm_modes:
                        # Configure tesseract for better number detection (NDC focused)
                        config = f'--oem 3 --psm {psm} -c tessedit_char_whitelist=0123456789-'
                        text_numbers = pytesseract.image_to_string(processed_img, config=config)
                        
                        # Also get full text for RX number detection
                        config_full = f'--oem 3 --psm {psm}'
                        text_full = pytesseract.image_to_string(processed_img, config=config_full)
                        
                        # Look for NDC patterns in the number-focused extracted text
                        if 'ndc' not in found_info:
                            ndc_patterns = [
                                r'(\d{5}-\d{4}-\d{1,2})',  # XXXXX-XXXX-XX (removed word boundaries)
                                r'(\d{4}-\d{4}-\d{1,2})',  # XXXX-XXXX-XX
                                r'(\d{5}-\d{3}-\d{1,2})',  # XXXXX-XXX-XX (alternative format)
                                r'(\d{4}-\d{3}-\d{1,2})'   # XXXX-XXX-XX (alternative format)
                            ]
                            
                            for pattern in ndc_patterns:
                                matches = re.findall(pattern, text_numbers)
                                if matches:
                                    found_info['ndc'] = matches[0]
                                    break
                            
                            # Also try without requiring word boundaries (in case OCR adds spaces)
                            if 'ndc' not in found_info:
                                loose_patterns = [
                                    r'(\d{5})\s*-\s*(\d{4})\s*-\s*(\d{2})',
                                    r'(\d{4})\s*-\s*(\d{4})\s*-\s*(\d{2})',
                                    r'(\d{5})\s*-\s*(\d{3})\s*-\s*(\d{2})',
                                    r'(\d{4})\s*-\s*(\d{3})\s*-\s*(\d{2})'
                                ]
                                
                                for pattern in loose_patterns:
                                    matches = re.findall(pattern, text_numbers)
                                    if matches:
                                        # Reconstruct NDC by joining the groups
                                        found_info['ndc'] = '-'.join(matches[0])
                                        break
                        
                        # Look for RX number patterns in the full text
                        if 'rx_number' not in found_info:
                            rx_patterns = [
                                r'(?:Rx|RX)\s*#?\s*(\d+)',                           # Rx #123456 or RX 123456
                                r'(?:Prescription|PRESCRIPTION)\s*(?:Number|#)?\s*:?\s*(\d+)',  # Prescription Number: 123456
                                r'(?:Script|SCRIPT)\s*(?:ID|Number)\s*:?\s*(\d+)',   # Script ID: 123456
                                r'(?:Rx|RX)\s*(?:Number|No|NUM)\s*:?\s*(\d+)',       # Rx Number: 123456
                                r'(?:Prescription|PRESCRIPTION)\s*:?\s*(\d+)',        # Prescription: 123456
                                r'(?:RX|Rx)\s*(\d{6,})',                            # RX 123456 (6+ digits, space optional)
                                r'(?:RX|Rx)(\d{6,})',                               # RX123456 (no space, 6+ digits)
                                r'#\s*(\d{6,})'                                     # #123456 (standalone with 6+ digits)
                            ]
                            
                            for pattern in rx_patterns:
                                matches = re.findall(pattern, text_full, re.IGNORECASE)
                                if matches:
                                    found_info['rx_number'] = matches[0]
                                    break
                        
                        # If we found both or either, we can return
                        if found_info:
                            if len(found_info) == 2 or psm == psm_modes[-1]:  # Found both or last attempt
                                break
                                
                    # Return found info if any
                    if found_info:
                        return found_info
                        
                except Exception as e:
                    # Continue with next image/config if this one fails
                    continue
            
            # If no strict patterns found, try a more lenient approach for NDC
            # Look for any sequence that might be an NDC (only if we haven't found anything yet)
            if not found_info:
                for processed_img in processed_images[:2]:  # Only try the 2 best preprocessed images
                    try:
                        text = pytesseract.image_to_string(processed_img)
                        # Look for number sequences that could be NDCs
                        numbers = re.findall(r'\d+', text)
                        
                        # Try to find sequences that could form an NDC
                        for i in range(len(numbers) - 2):
                            part1, part2, part3 = numbers[i], numbers[i+1], numbers[i+2]
                            
                            # Check if this could be a valid NDC format
                            if ((len(part1) == 4 or len(part1) == 5) and 
                                (len(part2) == 3 or len(part2) == 4) and 
                                len(part3) == 2):
                                potential_ndc = f"{part1}-{part2}-{part3}"
                                found_info['ndc'] = potential_ndc
                                break
                                
                    except Exception:
                        continue
                    
                    if found_info.get('ndc'):
                        break
            
            # Return whatever we found (could be NDC, RX, both, or empty dict)
            return found_info if found_info else None
                    
        except Exception as e:
            print(f"Error in prescription info text detection: {e}")
        
        return None

    def parse_prescription_data(self, qr_data: str) -> Dict:
        """
        Parse prescription QR code data and extract relevant information
        Common prescription data fields include:
        - Patient name, DOB
        - Medication name, strength, NDC number
        - Prescriber information
        - Pharmacy information
        - Prescription number, date filled
        - Directions for use
        """
        parsed_data = {
            'raw_data': qr_data,
            'patient_name': None,
            'patient_dob': None,
            'medication_name': None,
            'medication_strength': None,
            'ndc_number': None,
            'prescriber_name': None,
            'pharmacy_name': None,
            'rx_number': None,
            'date_filled': None,
            'directions': None,
            'quantity': None,
            'refills': None,
            'detection_method': 'QR_CODE'  # Track detection method
        }

        # Check if this is prescription info data from text detection
        if qr_data.startswith('TEXT_INFO: '):
            # Parse the dictionary from the text
            import ast
            try:
                info_str = qr_data[11:]  # Remove 'TEXT_INFO: ' prefix
                info_dict = ast.literal_eval(info_str)
                
                if info_dict.get('ndc'):
                    parsed_data['ndc_number'] = info_dict['ndc']
                if info_dict.get('rx_number'):
                    parsed_data['rx_number'] = info_dict['rx_number']
                    
                parsed_data['detection_method'] = 'TEXT_OCR'
                return parsed_data
            except (ValueError, SyntaxError):
                # Fallback for old NDC-only format
                if qr_data.startswith('NDC: '):
                    ndc_value = qr_data[5:]  # Remove 'NDC: ' prefix
                    parsed_data['ndc_number'] = ndc_value
                    parsed_data['detection_method'] = 'TEXT_OCR'
                    return parsed_data

        try:
            if qr_data.strip().startswith('<') and qr_data.strip().endswith('>'):
                try:
                    root = ET.fromstring(qr_data)

                    xml_mappings = {
                        'patient_name': ['n', 'name', 'patient'],
                        'medication_name': ['dg', 'drug', 'medication', 'med'],
                        'prescriber_name': ['pm', 'prescriber', 'doctor'],
                        'date_filled': ['dt', 'date', 'dispensed'],
                        'directions': ['in', 'instructions', 'directions', 'sig'],
                        'rx_number': ['id', 'rx', 'prescription_id'],
                        'pharmacy_name': ['pharmacy', 'pharm']
                    }

                    for field, xml_tags in xml_mappings.items():
                        for tag in xml_tags:
                            element = root.find(tag)
                            if element is not None and element.text:
                                parsed_data[field] = element.text.strip()
                                break

                except ParseError:
                    # If XML parsing fails, try simple regex extraction
                    xml_patterns = {
                        'patient_name': r'<n[^>]*>([^<]+)</n>',
                        'medication_name': r'<dg[^>]*>([^<]+)</dg>',
                        'prescriber_name': r'<pm[^>]*>([^<]+)</pm>',
                        'date_filled': r'<dt[^>]*>([^<]+)</dt>',
                        'directions': r'<in[^>]*>([^<]+)</in>',
                        'rx_number': r'<id[^>]*>([^<]+)</id>'
                    }

                    for field, pattern in xml_patterns.items():
                        match = re.search(pattern, qr_data, re.IGNORECASE)
                        if match:
                            parsed_data[field] = match.group(1).strip()

            # Check for JSON format
            elif qr_data.startswith('{') and qr_data.endswith('}'):
                json_data = json.loads(qr_data)

                field_mappings = {
                    'patient_name': ['patient_name', 'patientName', 'name', 'pt_name'],
                    'patient_dob': ['patient_dob', 'patientDOB', 'dob', 'birth_date'],
                    'medication_name': ['medication_name', 'medicationName', 'drug_name', 'med_name'],
                    'medication_strength': ['medication_strength', 'strength', 'dose'],
                    'ndc_number': ['ndc_number', 'ndc', 'NDC'],
                    'prescriber_name': ['prescriber_name', 'prescriber', 'doctor', 'physician'],
                    'pharmacy_name': ['pharmacy_name', 'pharmacy', 'pharm_name'],
                    'rx_number': ['rx_number', 'prescription_number', 'rx_num', 'rx'],
                    'date_filled': ['date_filled', 'fill_date', 'dispensed_date'],
                    'directions': ['directions', 'sig', 'instructions'],
                    'quantity': ['quantity', 'qty', 'amount'],
                    'refills': ['refills', 'refills_remaining']
                }

                for field, possible_keys in field_mappings.items():
                    for key in possible_keys:
                        if key in json_data:
                            parsed_data[field] = json_data[key]
                            break

            else:
                lines = qr_data.split('\n')
                for line in lines:
                    line = line.strip()
                    if not line:
                        continue

                    if re.match(r'^[A-Z]{2,}:', line):
                        key, value = line.split(':', 1)
                        key = key.strip().lower()
                        value = value.strip()

                        if key in ['patient', 'name', 'pt']:
                            parsed_data['patient_name'] = value
                        elif key in ['dob', 'birth']:
                            parsed_data['patient_dob'] = value
                        elif key in ['drug', 'medication', 'med']:
                            parsed_data['medication_name'] = value
                        elif key in ['strength', 'dose']:
                            parsed_data['medication_strength'] = value
                        elif key == 'ndc':
                            parsed_data['ndc_number'] = value
                        elif key in ['prescriber', 'doctor', 'physician']:
                            parsed_data['prescriber_name'] = value
                        elif key in ['pharmacy', 'pharm']:
                            parsed_data['pharmacy_name'] = value
                        elif key in ['rx', 'prescription']:
                            parsed_data['rx_number'] = value
                        elif key in ['filled', 'date']:
                            parsed_data['date_filled'] = value
                        elif key in ['directions', 'sig']:
                            parsed_data['directions'] = value
                        elif key in ['qty', 'quantity']:
                            parsed_data['quantity'] = value
                        elif key == 'refills':
                            parsed_data['refills'] = value

                ndc_match = re.search(r'\b\d{4,5}-\d{3,4}-\d{2}\b', qr_data)
                if ndc_match and not parsed_data['ndc_number']:
                    parsed_data['ndc_number'] = ndc_match.group()

                rx_match = re.search(r'\bRx\s*#?\s*(\d+)\b',
                                     qr_data, re.IGNORECASE)
                if rx_match and not parsed_data['rx_number']:
                    parsed_data['rx_number'] = rx_match.group(1)

        except (json.JSONDecodeError, ValueError, KeyError) as e:
            print(f"Error parsing prescription data: {e}")

        return parsed_data

    def validate_prescription_data(self, parsed_data: Dict) -> Tuple[bool, List[str]]:
        """
        Validate parsed prescription data for completeness and format
        Returns tuple of (is_valid, list_of_issues)
        """
        issues = []

        if not parsed_data.get('medication_name'):
            issues.append("Missing medication name")

        if not parsed_data.get('patient_name'):
            issues.append("Missing patient name")

        ndc = parsed_data.get('ndc_number')
        if ndc and not re.match(r'^\d{4,5}-\d{3,4}-\d{1,2}$', ndc):
            issues.append("Invalid NDC number format")

        rx_num = parsed_data.get('rx_number')
        if rx_num:
            rx_clean = rx_num.strip()
            if not rx_clean or rx_clean.lower() in ['test', 'sample', 'demo']:
                issues.append(
                    "Invalid prescription number (appears to be test data)")

        date_filled = parsed_data.get('date_filled')
        if date_filled:
            date_formats = [
                '%Y-%m-%d',      # 2025-03-15
                '%m/%d/%Y',      # 03/15/2025
                '%d/%m/%Y',      # 15/03/2025 (common in many countries)
                '%Y/%m/%d',      # 2025/03/15
                '%d-%m-%Y',      # 15-03-2025
                '%m-%d-%Y',      # 03-15-2025
                '%d.%m.%Y',      # 15.03.2025
                '%m.%d.%Y'       # 03.15.2025
            ]

            date_valid = False
            for date_format in date_formats:
                try:
                    datetime.strptime(date_filled, date_format)
                    date_valid = True
                    break
                except ValueError:
                    continue

            if not date_valid:
                issues.append("Invalid date format")

        return len(issues) == 0, issues

    def format_prescription_output(self, parsed_data: Dict) -> str:
        output = []
        
        # Check detection method to customize output
        if parsed_data.get('detection_method') == 'TEXT_OCR':
            output.append("=" * 50)
            output.append("PRESCRIPTION INFORMATION (Text Detection)")
            output.append("=" * 50)
            output.append("")
            
            found_items = []
            if parsed_data.get('ndc_number'):
                output.append(f"NDC Number: {parsed_data['ndc_number']}")
                found_items.append("NDC")
            if parsed_data.get('rx_number'):
                output.append(f"RX Number: {parsed_data['rx_number']}")
                found_items.append("RX")
            
            output.append("")
            output.append(f"Note: {', '.join(found_items)} number(s) detected from text.")
            output.append("No additional prescription information available.")
            output.append("For complete prescription data, use a QR code.")
        else:
            output.append("=" * 50)
            output.append("PRESCRIPTION INFORMATION")
            output.append("=" * 50)

            if parsed_data.get('patient_name'):
                output.append(f"Patient: {parsed_data['patient_name']}")
            if parsed_data.get('patient_dob'):
                output.append(f"DOB: {parsed_data['patient_dob']}")

            output.append("")

            if parsed_data.get('medication_name'):
                output.append(f"Medication: {parsed_data['medication_name']}")
            if parsed_data.get('medication_strength'):
                output.append(f"Strength: {parsed_data['medication_strength']}")
            if parsed_data.get('ndc_number'):
                output.append(f"NDC: {parsed_data['ndc_number']}")

            output.append("")

            if parsed_data.get('prescriber_name'):
                output.append(f"Prescriber: {parsed_data['prescriber_name']}")
            if parsed_data.get('pharmacy_name'):
                output.append(f"Pharmacy: {parsed_data['pharmacy_name']}")

            output.append("")

            if parsed_data.get('rx_number'):
                output.append(f"Rx Number: {parsed_data['rx_number']}")
            if parsed_data.get('date_filled'):
                output.append(f"Date Filled: {parsed_data['date_filled']}")
            if parsed_data.get('quantity'):
                output.append(f"Quantity: {parsed_data['quantity']}")
            if parsed_data.get('refills'):
                output.append(f"Refills: {parsed_data['refills']}")

            output.append("")

            if parsed_data.get('directions'):
                output.append(f"Directions: {parsed_data['directions']}")

        return "\n".join(output)

    def read_from_camera(self) -> Optional[str]:
        try:
            self.cap = cv2.VideoCapture(0)

            if not self.cap.isOpened():
                print("Error: Could not open camera")
                return None

            print(
                "Camera opened. Point camera at QR code. Press 'q' to quit, 's' to save current frame.")
            print("Enhanced detection enabled - will try multiple processing techniques.")

            while True:
                ret, frame = self.cap.read()
                if not ret:
                    print("Error: Could not read frame")
                    break

                qr_data = self.enhanced_qr_detection(frame)

                if qr_data:
                    decoded_objects = pyzbar.decode(
                        frame, symbols=[ZBarSymbol.QRCODE])

                    if decoded_objects:
                        for obj in decoded_objects:
                            x, y, w, h = obj.rect
                            cv2.rectangle(
                                frame, (x, y), (x + w, y + h), (0, 255, 0), 2)

                            font = cv2.FONT_HERSHEY_SIMPLEX
                            cv2.putText(frame, "QR Code Detected",
                                        (x, y-10), font, 0.5, (0, 255, 0), 2)
                    else:
                        font = cv2.FONT_HERSHEY_SIMPLEX
                        cv2.putText(frame, "QR Code Found (Enhanced Detection)",
                                    (10, 30), font, 0.7, (0, 255, 0), 2)

                    cv2.imshow('Prescription QR Code Reader', frame)
                    cv2.waitKey(1000)  # Show detection for 1 second

                    self.cap.release()
                    cv2.destroyAllWindows()
                    return qr_data
                else:
                    # Try prescription info detection as fallback
                    prescription_info = self.detect_prescription_info_from_text(frame)
                    if prescription_info:
                        font = cv2.FONT_HERSHEY_SIMPLEX
                        display_text = []
                        if prescription_info.get('ndc'):
                            display_text.append(f"NDC: {prescription_info['ndc']}")
                        if prescription_info.get('rx_number'):
                            display_text.append(f"RX: {prescription_info['rx_number']}")
                        
                        for i, text in enumerate(display_text):
                            cv2.putText(frame, text, (10, 30 + i*30), font, 0.7, (0, 255, 255), 2)
                        
                        cv2.imshow('Prescription QR Code Reader', frame)
                        cv2.waitKey(2000)  # Show detection for 2 seconds

                        self.cap.release()
                        cv2.destroyAllWindows()
                        return f"TEXT_INFO: {prescription_info}"

                # Show scanning status
                font = cv2.FONT_HERSHEY_SIMPLEX
                cv2.putText(frame, "Scanning for QR Code...",
                            (10, 30), font, 0.7, (0, 0, 255), 2)
                cv2.putText(frame, "Press 'q' to quit, 's' to save frame",
                            (10, 60), font, 0.5, (255, 255, 255), 1)

                cv2.imshow('Prescription QR Code Reader', frame)

                key = cv2.waitKey(1) & 0xFF
                if key == ord('q'):
                    break
                elif key == ord('s'):
                    cv2.imwrite(
                        f'qr_frame_{datetime.now().strftime("%Y%m%d_%H%M%S")}.jpg', frame)
                    print("Frame saved")

        except Exception as e:
            print(f"Camera error: {e}")
        finally:
            if self.cap:
                self.cap.release()
            cv2.destroyAllWindows()

        return None

    def read_from_image(self, image_path: str) -> Optional[str]:
        try:
            image = cv2.imread(image_path)
            if image is None:
                print(f"Error: Could not load image {image_path}")
                return None

            print(f"Analyzing image: {image_path}")
            print(
                f"Image dimensions: {image.shape[1]}x{image.shape[0]} pixels")

            # First try QR code detection
            qr_data = self.enhanced_qr_detection(image)

            if qr_data:
                print("✓ QR code successfully detected and decoded")
                return qr_data
            else:
                print("✗ No QR code found in image after trying all detection methods")
                
                # Fallback to prescription info detection from text
                print("Attempting prescription info detection from text as fallback...")
                prescription_info = self.detect_prescription_info_from_text(image)
                
                if prescription_info:
                    found_items = []
                    if prescription_info.get('ndc'):
                        found_items.append(f"NDC: {prescription_info['ndc']}")
                    if prescription_info.get('rx_number'):
                        found_items.append(f"RX: {prescription_info['rx_number']}")
                    
                    print(f"✓ Prescription info detected: {', '.join(found_items)}")
                    # Return a format that can be parsed
                    return f"TEXT_INFO: {prescription_info}"
                else:
                    print("✗ No prescription info found in image text")
                    print("Tips:")
                    print("- Ensure the QR code or prescription label is clearly visible")
                    print("- Try better lighting conditions")
                    print("- Make sure the text isn't too small or blurry")
                    print("- Check if the image contains a valid QR code, NDC number, or RX number")
                    return None

        except Exception as e:
            print(f"Error reading image: {e}")
            return None

    def process_qr_data(self, qr_data: str) -> None:
        if not qr_data:
            print("No QR code data to process")
            return

        print("\nRaw QR Code Data:")
        print("-" * 30)
        print(qr_data)
        print()

        parsed_data = self.parse_prescription_data(qr_data)
        is_valid, issues = self.validate_prescription_data(parsed_data)

        print(self.format_prescription_output(parsed_data))

        if not is_valid:
            print("\nValidation Issues:")
            for issue in issues:
                print(f"- {issue}")
        else:
            print("\n✓ Prescription data appears valid")


def main():
    parser = argparse.ArgumentParser(description='Prescription QR Code Reader')
    parser.add_argument('-i', '--image', help='Read QR code from image file')
    parser.add_argument('-c', '--camera', action='store_true',
                        help='Read QR code from camera (default)')

    args = parser.parse_args()

    reader = PrescriptionQRReader()

    if args.image:
        qr_data = reader.read_from_image(args.image)
    else:
        qr_data = reader.read_from_camera()

    if qr_data:
        reader.process_qr_data(qr_data)
    else:
        print("No QR code detected or read")
        sys.exit(1)


if __name__ == "__main__":
    main()
