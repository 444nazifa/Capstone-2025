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
            'refills': None
        }

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
        if ndc and not re.match(r'^\d{4,5}-\d{3,4}-\d{2}$', ndc):
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

            qr_data = self.enhanced_qr_detection(image)

            if qr_data:
                print("✓ QR code successfully detected and decoded")
                return qr_data
            else:
                print("✗ No QR code found in image after trying all detection methods")
                print("Tips:")
                print("- Ensure the QR code is clearly visible")
                print("- Try better lighting conditions")
                print("- Make sure the QR code isn't too small or blurry")
                print("- Check if the image contains a valid QR code")
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
