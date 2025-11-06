# Script to run Android app on physical device without Expo Go
# Make sure your Android device is connected via USB with USB debugging enabled

$WorkspacePath = "d:\new working without modules"

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Building React Native App for Physical Android Device" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Check if device is connected
Write-Host "Checking for connected Android devices..." -ForegroundColor Yellow
cd $WorkspacePath
adb devices

Write-Host ""
Write-Host "If you see your device listed above, press Enter to continue..." -ForegroundColor Green
Write-Host "If not, please:" -ForegroundColor Red
Write-Host "  1. Connect your Android device via USB" -ForegroundColor Red
Write-Host "  2. Enable USB Debugging in Developer Options" -ForegroundColor Red
Write-Host "  3. Accept the USB debugging prompt on your device" -ForegroundColor Red
Write-Host ""
pause

# Build and install the app directly on the device
Write-Host ""
Write-Host "Building and installing app on your device..." -ForegroundColor Green
Write-Host "This may take a few minutes on first build..." -ForegroundColor Yellow
Write-Host ""

npx expo run:android --device

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Build Complete!" -ForegroundColor Green
Write-Host "The app should now be installed on your device." -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Cyan
