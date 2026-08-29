import sys
import subprocess

try:
    from PIL import Image
    print("PIL is already installed.")
except ImportError:
    print("PIL not found. Installing Pillow...")
    try:
        subprocess.check_call([sys.executable, "-m", "pip", "install", "Pillow"])
        from PIL import Image
        print("Pillow installed successfully.")
    except Exception as e:
        print("Failed to install Pillow:", e)
        sys.exit(1)

src_path = r"c:\Users\Bruker\Android Studio\Maratontrener\app_icon.png"

try:
    img = Image.open(src_path)
    # Resize to exactly 512x512 with high-quality resampling
    resized_img = img.resize((512, 512), Image.Resampling.LANCZOS)
    
    # Ensure it's saved as a 32-bit PNG (RGBA format)
    rgba_img = resized_img.convert("RGBA")
    
    # Save back to app_icon.png as PNG format
    rgba_img.save(src_path, "PNG")
    print(f"Successfully converted and resized {src_path} to 512x512 32-bit PNG!")
    
    # Verify dimensions and format
    verify_img = Image.open(src_path)
    print(f"Verified dimensions: {verify_img.size[0]}x{verify_img.size[1]}")
    print(f"Verified format: {verify_img.format}")
    
except Exception as e:
    print("Error during image processing:", e)
