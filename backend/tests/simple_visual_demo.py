#!/usr/bin/env python3

import numpy as np

def adjust_gamma(image, gamma=1.0):
    """Apply gamma correction to image array"""
    inv_gamma = 1.0 / gamma
    table = np.array([((i / 255.0) ** inv_gamma) * 255 for i in np.arange(0, 256)]).astype("uint8")
    return table[image]

def create_qr_pattern():
    """Create a simple QR-like pattern"""
    pattern = np.ones((20, 20), dtype=np.uint8) * 255  # White background
    
    # Create finder patterns (black squares)
    pattern[2:5, 2:5] = 0     # Top-left
    pattern[2:5, 15:18] = 0   # Top-right  
    pattern[15:18, 2:5] = 0   # Bottom-left
    
    # Add some data modules
    pattern[6:8, 6:8] = 0
    pattern[10:12, 12:14] = 0
    
    return pattern

def print_ascii_image(image, title):
    """Print image as ASCII art"""
    print(f"\n{title}:")
    print("█" * (image.shape[1] + 2))
    for row in image:
        print("█", end="")
        for pixel in row:
            if pixel < 85:
                print("██", end="")  # Very dark -> solid block
            elif pixel < 170:
                print("▓▓", end="")  # Medium -> medium shade
            else:
                print("  ", end="")  # Light -> space
        print("█")
    print("█" * (image.shape[1] + 2))

def main():
    # Create QR pattern
    original_qr = create_qr_pattern()
    
    # Simulate dark image (underexposed)
    dark_qr = (original_qr * 0.3).astype(np.uint8)
    
    # Simulate bright image (overexposed)  
    bright_qr = np.clip(original_qr * 1.5 + 80, 0, 255).astype(np.uint8)
    
    print("GAMMA CORRECTION VISUAL DEMO")
    print("=" * 50)
    print("Legend: ██=black, ▓▓=gray, spaces=white")
    
    # Show dark image problem and solutions
    print_ascii_image(dark_qr, "PROBLEM: Dark/Underexposed QR Code")
    print_ascii_image(adjust_gamma(dark_qr, 1.5), "SOLUTION: Gamma 1.5 (brightens)")
    print_ascii_image(adjust_gamma(dark_qr, 2.0), "SOLUTION: Gamma 2.0 (brightens more)")
    
    print("\n" + "="*50)
    
    # Show bright image problem and solution
    print_ascii_image(bright_qr, "PROBLEM: Bright/Overexposed QR Code")
    print_ascii_image(adjust_gamma(bright_qr, 0.5), "SOLUTION: Gamma 0.5 (darkens)")
    
    print("\n" + "="*50)
    print("ANALYSIS:")
    print("- Dark images: Can't see black squares clearly → Use gamma 1.5-2.0 to brighten")
    print("- Bright images: Black squares become gray → Use gamma 0.5 to darken")
    print("- Your code tries [0.5, 1.5, 2.0] to handle all lighting conditions!")

if __name__ == "__main__":
    main()