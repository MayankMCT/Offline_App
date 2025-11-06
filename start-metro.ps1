# Script to start Metro bundler with correct NODE_PATH in development build mode
$env:NODE_PATH = "D:\vs\test-offline-sync\node_modules"
Write-Host "Starting Metro bundler in development build mode..." -ForegroundColor Green
npx expo start --dev-client --clear
