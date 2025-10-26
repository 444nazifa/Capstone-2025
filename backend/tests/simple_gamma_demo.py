#!/usr/bin/env python3

import numpy as np

def adjust_gamma(pixel_value, gamma=1.0):
    """Apply gamma correction to a single pixel value (0-255)"""
    inv_gamma = 1.0 / gamma
    normalized = pixel_value / 255.0
    corrected = normalized ** inv_gamma
    return int(corrected * 255)

def demonstrate_gamma_correction():
    print("Gamma Correction Demonstration")
    print("=" * 50)
    print("Pixel Value: 0 (black) to 255 (white)")
    print()
    
    # Test different pixel values
    test_pixels = [50, 100, 150, 200]  # Dark to bright pixels
    gamma_values = [0.5, 1.0, 1.5, 2.0]
    
    print(f"{'Original':<10}", end="")
    for gamma in gamma_values:
        print(f"Gamma {gamma:<6}", end="")
    print()
    print("-" * 50)
    
    for pixel in test_pixels:
        print(f"{pixel:<10}", end="")
        for gamma in gamma_values:
            corrected = adjust_gamma(pixel, gamma)
            print(f"{corrected:<10}", end="")
        print()
    
    print("\nExplanation:")
    print("- Gamma 0.5: DARKENS image (inv_gamma=2.0, pixel^2)")
    print("- Gamma 1.0: No change (original)")  
    print("- Gamma 1.5: BRIGHTENS image (inv_gamma=0.67, pixel^0.67)")
    print("- Gamma 2.0: BRIGHTENS more (inv_gamma=0.5, pixel^0.5)")
    
    print("\nCORRECTED - QR Code Detection Benefits:")
    print("- Overexposed QR codes: Gamma 0.5 darkens, increases contrast")
    print("- Underexposed QR codes: Gamma 1.5-2.0 brightens, reveals details")
    print("- Your code tries [0.5, 1.5, 2.0] to handle bright→dark→bright range")

if __name__ == "__main__":
    demonstrate_gamma_correction()