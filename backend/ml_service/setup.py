#!/usr/bin/env python3
"""
Setup script for ML Recommendation Service
"""

import subprocess
import sys
import os

def install_requirements():
    """Install Python requirements"""
    print("Installing Python requirements...")
    try:
        subprocess.check_call([sys.executable, "-m", "pip", "install", "-r", "requirements.txt"])
        print("✅ Requirements installed successfully")
    except subprocess.CalledProcessError as e:
        print(f"❌ Error installing requirements: {e}")
        return False
    return True

def run_tests():
    """Run basic tests"""
    print("Running basic tests...")
    try:
        # Test imports
        import flask
        import pandas
        import numpy
        import sklearn
        print("✅ All imports successful")
        return True
    except ImportError as e:
        print(f"❌ Import error: {e}")
        return False

def main():
    """Main setup function"""
    print("🚀 Setting up ML Recommendation Service...")
    
    # Install requirements
    if not install_requirements():
        sys.exit(1)
    
    # Run tests
    if not run_tests():
        sys.exit(1)
    
    print("✅ Setup completed successfully!")
    print("🎯 To start the ML service, run: python app.py")
    print("🌐 The service will be available at: http://localhost:5000")

if __name__ == "__main__":
    main() 