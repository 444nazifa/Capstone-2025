#!/usr/bin/env python3

import cv2
import numpy as np
import matplotlib.pyplot as plt
from prescription_qr_reader import PrescriptionQRReader

def create_sample_qr_image():
    """Create a sample QR-like pattern for testing"""
    # Create a simple black and white pattern similar to QR code
    image = np.ones((200, 200), dtype=np.uint8) * 255  # White background
    
    # Add some QR-like black squares
    image[50:70, 50:70] = 0    # Top-left finder pattern
    image[50:70, 130:150] = 0  # Top-right finder pattern  
    image[130:150, 50:70] = 0  # Bottom-left finder pattern
    
    # Add some data pattern
    image[80:100, 80:100] = 0
    image[110:130, 110:130] = 0
    
    return image

def adjust_gamma(image, gamma=1.0):
    """Apply gamma correction"""
    inv_gamma = 1.0 / gamma
    table = np.array([((i / 255.0) ** inv_gamma) * 255 for i in np.arange(0, 256)]).astype("uint8")
    return cv2.LUT(image, table)

def simulate_lighting_conditions(image):
    """Simulate different lighting conditions"""
    # Too dark (underexposed)
    dark_image = (image * 0.3).astype(np.uint8)
    
    # Too bright (overexposed) 
    bright_image = np.clip(image * 1.8 + 50, 0, 255).astype(np.uint8)
    
    # Uneven lighting (gradient)
    h, w = image.shape
    gradient = np.linspace(0.3, 1.0, w).reshape(1, -1)
    gradient = np.repeat(gradient, h, axis=0)
    uneven_image = (image * gradient).astype(np.uint8)
    
    return dark_image, bright_image, uneven_image

def main():
    # Create sample QR pattern
    original_qr = create_sample_qr_image()
    
    # Simulate different lighting problems
    dark_image, bright_image, uneven_image = simulate_lighting_conditions(original_qr)
    
    # Apply gamma corrections
    gamma_values = [0.5, 1.0, 1.5, 2.0]
    
    # Create comparison plot
    fig, axes = plt.subplots(4, 4, figsize=(15, 12))
    fig.suptitle('Gamma Correction Comparison for QR Code Detection', fontsize=16)
    
    test_images = [original_qr, dark_image, bright_image, uneven_image]
    test_names = ['Original', 'Dark (Underexposed)', 'Bright (Overexposed)', 'Uneven Lighting']
    
    for row, (test_img, test_name) in enumerate(zip(test_images, test_names)):
        for col, gamma in enumerate(gamma_values):
            corrected = adjust_gamma(test_img, gamma)
            
            axes[row, col].imshow(corrected, cmap='gray')
            axes[row, col].set_title(f'{test_name}\nGamma: {gamma}')
            axes[row, col].axis('off')
            
            # Add contrast info
            contrast = np.std(corrected)
            axes[row, col].text(10, 190, f'Contrast: {contrast:.1f}', 
                              bbox=dict(boxstyle="round,pad=0.3", facecolor="white", alpha=0.8),
                              fontsize=8)
    
    plt.tight_layout()
    plt.savefig('/Users/rem/projects/python/qr-code-reader/backend/gamma_comparison.png', dpi=150, bbox_inches='tight')
    print("Gamma comparison saved as 'gamma_comparison.png'")
    
    # Print analysis
    print("\nGamma Correction Analysis:")
    print("=" * 50)
    print("Gamma < 1.0 (e.g., 0.5): Brightens image, helps with dark/underexposed images")
    print("Gamma = 1.0: No change (original image)")  
    print("Gamma > 1.0 (e.g., 1.5, 2.0): Darkens image, helps with bright/overexposed images")
    print("\nFor QR detection:")
    print("- Dark images: Use gamma 0.5 to reveal hidden patterns")
    print("- Bright images: Use gamma 1.5-2.0 to reduce glare")
    print("- Uneven lighting: Try multiple gamma values to find best contrast")

if __name__ == "__main__":
    main()