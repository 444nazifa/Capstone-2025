#!/usr/bin/env python3

import numpy as np
import matplotlib.pyplot as plt

def adjust_gamma(image, gamma=1.0):
    """Apply gamma correction to image array"""
    inv_gamma = 1.0 / gamma
    table = np.array([((i / 255.0) ** inv_gamma) * 255 for i in np.arange(0, 256)]).astype("uint8")
    return table[image]

def create_qr_pattern():
    """Create a simple QR-like pattern"""
    pattern = np.ones((100, 100), dtype=np.uint8) * 255  # White background
    
    # Create finder patterns (black squares)
    pattern[10:25, 10:25] = 0    # Top-left
    pattern[10:25, 75:90] = 0    # Top-right  
    pattern[75:90, 10:25] = 0    # Bottom-left
    
    # Add some data modules
    pattern[30:35, 30:35] = 0
    pattern[40:45, 50:55] = 0
    pattern[60:65, 40:45] = 0
    pattern[50:55, 70:75] = 0
    
    return pattern

def simulate_lighting_problems(image):
    """Simulate common lighting issues"""
    # Too dark (underexposed)
    dark = (image * 0.3).astype(np.uint8)
    
    # Too bright (overexposed)
    bright = np.clip(image * 1.5 + 80, 0, 255).astype(np.uint8)
    
    # Very dark (severely underexposed)
    very_dark = (image * 0.15).astype(np.uint8)
    
    return dark, bright, very_dark

def main():
    # Create QR pattern
    original_qr = create_qr_pattern()
    
    # Create problematic versions
    dark_qr, bright_qr, very_dark_qr = simulate_lighting_problems(original_qr)
    
    # Test images and their problems
    test_cases = [
        (dark_qr, "Dark/Underexposed", [1.5, 2.0]),
        (bright_qr, "Bright/Overexposed", [0.5]),
        (very_dark_qr, "Very Dark", [2.0])
    ]
    
    fig, axes = plt.subplots(len(test_cases), 4, figsize=(16, 12))
    fig.suptitle('Before/After Gamma Correction for QR Detection', fontsize=16)
    
    for row, (problem_img, title, best_gammas) in enumerate(test_cases):
        # Show original problem
        axes[row, 0].imshow(problem_img, cmap='gray', vmin=0, vmax=255)
        axes[row, 0].set_title(f'{title}\n(Problem Image)')
        axes[row, 0].axis('off')
        
        # Show gamma corrections
        gamma_values = [0.5, 1.5, 2.0]
        for col, gamma in enumerate(gamma_values):
            corrected = adjust_gamma(problem_img, gamma)
            axes[row, col + 1].imshow(corrected, cmap='gray', vmin=0, vmax=255)
            
            # Highlight best gamma for this problem
            color = 'green' if gamma in best_gammas else 'black'
            weight = 'bold' if gamma in best_gammas else 'normal'
            
            axes[row, col + 1].set_title(f'Gamma {gamma}', color=color, weight=weight)
            axes[row, col + 1].axis('off')
            
            # Add contrast measurement
            contrast = np.std(corrected)
            axes[row, col + 1].text(5, 95, f'Contrast: {contrast:.1f}', 
                                  bbox=dict(boxstyle="round,pad=0.3", facecolor="white", alpha=0.8),
                                  fontsize=8)
    
    plt.tight_layout()
    plt.savefig('/Users/rem/projects/python/qr-code-reader/backend/gamma_before_after.png', 
                dpi=150, bbox_inches='tight')
    print("Visual comparison saved as 'gamma_before_after.png'")
    
    # Show the image
    plt.show()
    
    print("\nVisual Analysis:")
    print("=" * 50)
    print("Look for:")
    print("- BLACK squares should be clearly distinguishable from WHITE areas")
    print("- Higher contrast = better QR detection")
    print("- Green titles = recommended gamma for each problem type")

if __name__ == "__main__":
    main()