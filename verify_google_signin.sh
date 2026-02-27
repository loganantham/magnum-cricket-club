#!/bin/bash

# Google Sign-In Configuration Verification Script
# This script helps verify your Google Sign-In setup

echo "=========================================="
echo "Google Sign-In Configuration Verification"
echo "=========================================="
echo ""

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Expected values
EXPECTED_PACKAGE="com.magnum.cricketclub"
EXPECTED_SHA1="BB:F1:E2:2D:4C:EF:31:B4:CC:41:3F:FD:E5:B9:CC:E5:D1:18:3B:6D"
EXPECTED_WEB_CLIENT_ID="1076211377209-ride5umkasmacjqfjgh9836f1ttb9uls.apps.googleusercontent.com"
GOOGLE_SERVICES_FILE="app/google-services.json"

echo "1. Checking google-services.json file..."
if [ -f "$GOOGLE_SERVICES_FILE" ]; then
    echo -e "${GREEN}✓${NC} google-services.json exists"
    
    # Check package name
    if grep -q "$EXPECTED_PACKAGE" "$GOOGLE_SERVICES_FILE"; then
        echo -e "${GREEN}✓${NC} Package name found: $EXPECTED_PACKAGE"
    else
        echo -e "${RED}✗${NC} Package name not found or incorrect"
    fi
    
    # Check certificate_hash (SHA-1)
    SHA1_NO_COLONS=$(echo "$EXPECTED_SHA1" | tr -d ':')
    SHA1_LOWER=$(echo "$SHA1_NO_COLONS" | tr '[:upper:]' '[:lower:]')
    if grep -q "$SHA1_LOWER" "$GOOGLE_SERVICES_FILE"; then
        echo -e "${GREEN}✓${NC} SHA-1 certificate_hash found in google-services.json"
    else
        echo -e "${RED}✗${NC} SHA-1 certificate_hash NOT found in google-services.json"
        echo -e "${YELLOW}  Expected: $SHA1_LOWER${NC}"
    fi
    
    # Check Web Client ID
    if grep -q "$EXPECTED_WEB_CLIENT_ID" "$GOOGLE_SERVICES_FILE"; then
        echo -e "${GREEN}✓${NC} Web Client ID found: $EXPECTED_WEB_CLIENT_ID"
    else
        echo -e "${RED}✗${NC} Web Client ID not found or incorrect"
    fi
else
    echo -e "${RED}✗${NC} google-services.json NOT FOUND at $GOOGLE_SERVICES_FILE"
fi

echo ""
echo "2. Checking debug keystore SHA-1 fingerprint..."
DEBUG_KEYSTORE="$HOME/.android/debug.keystore"
if [ -f "$DEBUG_KEYSTORE" ]; then
    SHA1_OUTPUT=$(keytool -list -v -keystore "$DEBUG_KEYSTORE" -alias androiddebugkey -storepass android -keypass android 2>/dev/null | grep -i "SHA1:")
    if [ -n "$SHA1_OUTPUT" ]; then
        ACTUAL_SHA1=$(echo "$SHA1_OUTPUT" | awk '{print $2}')
        if [ "$ACTUAL_SHA1" = "$EXPECTED_SHA1" ]; then
            echo -e "${GREEN}✓${NC} SHA-1 fingerprint matches: $ACTUAL_SHA1"
        else
            echo -e "${YELLOW}⚠${NC} SHA-1 fingerprint mismatch!"
            echo -e "  Actual:   $ACTUAL_SHA1"
            echo -e "  Expected: $EXPECTED_SHA1"
        fi
    else
        echo -e "${RED}✗${NC} Could not read SHA-1 from debug keystore"
    fi
else
    echo -e "${YELLOW}⚠${NC} Debug keystore not found at $DEBUG_KEYSTORE"
fi

echo ""
echo "3. Checking AndroidManifest.xml..."
MANIFEST_FILE="app/src/main/AndroidManifest.xml"
if [ -f "$MANIFEST_FILE" ]; then
    if grep -q "$EXPECTED_PACKAGE" "$MANIFEST_FILE"; then
        echo -e "${GREEN}✓${NC} Package name found in AndroidManifest.xml"
    else
        echo -e "${RED}✗${NC} Package name not found in AndroidManifest.xml"
    fi
    
    if grep -q "INTERNET" "$MANIFEST_FILE"; then
        echo -e "${GREEN}✓${NC} INTERNET permission found"
    else
        echo -e "${RED}✗${NC} INTERNET permission missing"
    fi
else
    echo -e "${RED}✗${NC} AndroidManifest.xml not found"
fi

echo ""
echo "4. Checking build.gradle..."
BUILD_GRADLE="app/build.gradle"
if [ -f "$BUILD_GRADLE" ]; then
    if grep -q "google-services" "$BUILD_GRADLE"; then
        echo -e "${GREEN}✓${NC} Google Services plugin found"
    else
        echo -e "${YELLOW}⚠${NC} Google Services plugin not found (may be conditionally applied)"
    fi
    
    if grep -q "play-services-auth" "$BUILD_GRADLE"; then
        echo -e "${GREEN}✓${NC} Google Sign-In dependency found"
    else
        echo -e "${RED}✗${NC} Google Sign-In dependency missing"
    fi
else
    echo -e "${RED}✗${NC} build.gradle not found"
fi

echo ""
echo "=========================================="
echo "Verification Summary"
echo "=========================================="
echo ""
echo "If you're getting error 12501, check:"
echo ""
echo "1. ${YELLOW}OAuth Consent Screen${NC}"
echo "   → Go to Google Cloud Console → APIs & Services → OAuth consent screen"
echo "   → Configure and publish the consent screen"
echo ""
echo "2. ${YELLOW}SHA-1 in Firebase Console${NC}"
echo "   → Go to Firebase Console → Project Settings → Your apps"
echo "   → Verify SHA-1 fingerprint is added: $EXPECTED_SHA1"
echo ""
echo "3. ${YELLOW}Uninstall and Rebuild${NC}"
echo "   → Uninstall app: adb uninstall $EXPECTED_PACKAGE"
echo "   → Clean: ./gradlew clean"
echo "   → Rebuild: ./gradlew assembleDebug"
echo "   → Install: adb install app/build/outputs/apk/debug/app-debug.apk"
echo ""
echo "For detailed instructions, see: ERROR_12501_FIX.md"
echo ""
