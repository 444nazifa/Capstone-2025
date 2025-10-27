#!/usr/bin/env python3

import cv2
import numpy as np
import matplotlib.pyplot as plt

def adjust_gamma(image, gamma=1.0):
    """Apply gamma correction"""
    inv_gamma = 1.0 / gamma
    table = np.array([((i / 255.0) ** inv_gamma) * 255 for i in np.arange(0, 256)]).astype("uint8")
    return cv2.LUT(image, table)

def create_realistic_qr():
    """Create a realistic QR code pattern"""
    # Start with a 200x200 white background
    qr = np.ones((200, 200), dtype=np.uint8) * 255
    
    # Add finder patterns (corner squares)
    # Top-left finder pattern
    qr[10:50, 10:50] = 0  # Outer black square
    qr[20:40, 20:40] = 255  # Inner white square  
    qr[25:35, 25:35] = 0  # Center black square
    
    # Top-right finder pattern
    qr[10:50, 150:190] = 0
    qr[20:40, 160:180] = 255
    qr[25:35, 165:175] = 0
    
    # Bottom-left finder pattern
    qr[150:190, 10:50] = 0
    qr[160:180, 20:40] = 255
    qr[165:175, 25:35] = 0
    
    # Add some realistic data modules (random pattern)
    np.random.seed(42)  # For consistent results
    for i in range(60, 140, 5):
        for j in range(60, 140, 5):
            if np.random.random() > 0.5:
                qr[i:i+3, j:j+3] = 0
    
    # Make it look more like a photo with some noise and lighting issues
    # Add slight blur
    qr = cv2.GaussianBlur(qr, (3, 3), 0)
    
    # Simulate uneven lighting (darker on one side)
    h, w = qr.shape
    gradient = np.linspace(0.7, 1.0, w).reshape(1, -1)
    gradient = np.repeat(gradient, h, axis=0)
    qr = (qr * gradient).astype(np.uint8)
    
    return qr

def create_simple_comparison(image, gamma_value=0.5):
    """Create a simple side-by-side comparison"""
    
    # Apply gamma correction
    gamma_corrected = adjust_gamma(image, gamma_value)
    
    # Create side-by-side comparison
    fig, (ax1, ax2) = plt.subplots(1, 2, figsize=(10, 5))
    
    # Original image
    ax1.imshow(image, cmap='gray')
    ax1.set_title('BEFORE (Original)', fontsize=14, fontweight='bold')
    ax1.axis('off')
    
    # Gamma corrected image  
    ax2.imshow(gamma_corrected, cmap='gray')
    ax2.set_title(f'AFTER (Gamma {gamma_value})', fontsize=14, fontweight='bold')
    ax2.axis('off')
    
    plt.suptitle('Gamma Correction Comparison', fontsize=16)
    plt.tight_layout()
    
    # Save the comparison
    output_path = 'gamma_comparison.png'
    plt.savefig(output_path, dpi=150, bbox_inches='tight')
    print(f"Comparison saved as '{output_path}'")
    
    return output_path

def main():
    print("Creating realistic QR code pattern...")
    qr_image = create_realistic_qr()
    
    print("Creating side-by-side comparison...")
    create_simple_comparison(qr_image, gamma_value=0.5)
    print("Done!")

if __name__ == "__main__":
    main()