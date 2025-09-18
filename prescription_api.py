#!/usr/bin/env python3

from flask import Flask, request, jsonify
from werkzeug.utils import secure_filename
import os
import tempfile
import base64
import io
from PIL import Image
import cv2
import numpy as np
from pyzbar import pyzbar
from prescription_qr_reader import PrescriptionQRReader
import logging

app = Flask(__name__)
app.config['MAX_CONTENT_LENGTH'] = 16 * 1024 * 1024  # 16MB max file size

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

ALLOWED_EXTENSIONS = {'png', 'jpg', 'jpeg', 'gif', 'bmp', 'tiff', 'webp'}


def allowed_file(filename):
    """Check if uploaded file has allowed extension"""
    return '.' in filename and filename.rsplit('.', 1)[1].lower() in ALLOWED_EXTENSIONS


def decode_base64_image(base64_string):
    try:
        if base64_string.startswith('data:image'):
            base64_string = base64_string.split(',')[1]

        image_data = base64.b64decode(base64_string)
        image = Image.open(io.BytesIO(image_data))

        image_cv = cv2.cvtColor(np.array(image), cv2.COLOR_RGB2BGR)
        return image_cv
    except Exception as e:
        logger.error(f"Error decoding base64 image: {e}")
        return None


def read_qr_from_image_array(image_array):
    try:
        reader = PrescriptionQRReader()
        qr_data = reader.enhanced_qr_detection(image_array)
        return qr_data
    except Exception as e:
        logger.error(f"Error reading QR code: {e}")
        return None


@app.route('/health', methods=['GET'])
def health_check():
    return jsonify({
        'status': 'healthy',
        'service': 'Prescription QR Code Reader API',
        'version': '1.0.0'
    }), 200


@app.route('/api/scan-qr', methods=['POST'])
def scan_qr_code():
    try:
        qr_data = None
        image_source = None

        if 'image' in request.files:
            file = request.files['image']
            if file and file.filename and allowed_file(file.filename):
                # Save uploaded file temporarily
                with tempfile.NamedTemporaryFile(delete=False, suffix='.png') as tmp_file:
                    file.save(tmp_file.name)

                    reader = PrescriptionQRReader()
                    qr_data = reader.read_from_image(tmp_file.name)
                    image_source = "file_upload"

                    os.unlink(tmp_file.name)
            else:
                return jsonify({
                    'error': 'Invalid file type',
                    'message': 'Please upload a valid image file (PNG, JPG, JPEG, GIF, BMP, TIFF, WEBP)'
                }), 400

        elif request.is_json:
            data = request.get_json()
            if 'image' in data:
                image_array = decode_base64_image(data['image'])
                if image_array is not None:
                    qr_data = read_qr_from_image_array(image_array)
                    image_source = "base64"
                else:
                    return jsonify({
                        'error': 'Invalid base64 image',
                        'message': 'Could not decode base64 image data'
                    }), 400
            else:
                return jsonify({
                    'error': 'Missing image data',
                    'message': 'Please provide image data in base64 format'
                }), 400
        else:
            return jsonify({
                'error': 'No image provided',
                'message': 'Please provide an image file or base64 image data'
            }), 400

        if qr_data:
            reader = PrescriptionQRReader()
            parsed_data = reader.parse_prescription_data(qr_data)
            is_valid, issues = reader.validate_prescription_data(parsed_data)

            # Remove raw_data from response to keep it clean
            response_data = {k: v for k,
                             v in parsed_data.items() if k != 'raw_data'}

            return jsonify({
                'success': True,
                'qr_detected': True,
                'image_source': image_source,
                'prescription_data': response_data,
                'validation': {
                    'is_valid': is_valid,
                    'issues': issues
                },
                'raw_qr_data': qr_data
            }), 200
        else:
            return jsonify({
                'success': False,
                'qr_detected': False,
                'image_source': image_source,
                'message': 'No QR code detected in the provided image'
            }), 200

    except Exception as e:
        logger.error(f"Error processing QR code: {e}")
        return jsonify({
            'error': 'Processing error',
            'message': f'An error occurred while processing the image: {str(e)}'
        }), 500


@app.errorhandler(413)
def too_large(e):
    return jsonify({
        'error': 'File too large',
        'message': 'File size exceeds 16MB limit'
    }), 413


@app.errorhandler(404)
def not_found(e):
    return jsonify({
        'error': 'Endpoint not found',
        'message': 'The requested endpoint does not exist'
    }), 404


@app.errorhandler(500)
def internal_error(e):
    return jsonify({
        'error': 'Internal server error',
        'message': 'An unexpected error occurred'
    }), 500


if __name__ == '__main__':
    port = int(os.environ.get('PORT', 5000))
    debug = os.environ.get('DEBUG', 'False').lower() == 'true'

    print(f"Starting Prescription QR Code Reader API on port {port}")
    print("Available endpoints:")
    print("  GET  /health - Health check")
    print("  POST /api/scan-qr - Scan QR code from image")
    print("  POST /api/validate-prescription - Validate prescription data")
    print("  POST /api/parse-qr-text - Parse QR text directly")

    app.run(host='0.0.0.0', port=port, debug=debug)
