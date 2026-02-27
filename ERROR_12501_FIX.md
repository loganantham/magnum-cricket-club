# Fix Google Sign-In Error 12501 - Complete Guide

## What is Error 12501?

Error code **12501** means `DEVELOPER_ERROR`. This occurs when there's a configuration mismatch between your app and Google Cloud Console/Firebase.

## Your Current Configuration

✅ **SHA-1 Fingerprint:** `BB:F1:E2:2D:4C:EF:31:B4:CC:41:3F:FD:E5:B9:CC:E5:D1:18:3B:6D`  
✅ **Package Name:** `com.magnum.cricketclub`  
✅ **Web Client ID:** `1076211377209-ride5umkasmacjqfjgh9836f1ttb9uls.apps.googleusercontent.com`  
✅ **google-services.json:** Contains certificate_hash

## Most Common Causes (After SHA-1 is Added)

### 1. OAuth Consent Screen Not Configured ⚠️ **MOST LIKELY ISSUE**

The OAuth consent screen must be configured in Google Cloud Console.

**Steps to Fix:**
1. Go to [Google Cloud Console](https://console.cloud.google.com/)
2. Select your project: **magnum-cricket-club**
3. Navigate to **APIs & Services** → **OAuth consent screen**
4. Configure the consent screen:
   - **User Type:** Choose "External" (unless you have a Google Workspace)
   - **App name:** "Magnum Cricket Club"
   - **User support email:** Your email
   - **Developer contact information:** Your email
   - **Scopes:** Add `email` and `profile` (if not already added)
   - **Test users:** Add your Google account email (for testing)
5. Click **Save and Continue** through all steps
6. **Publish the app** (if in testing mode, add test users)

### 2. Web Client ID Not Linked to Android OAuth Client

The Web Client ID must be properly linked to your Android app.

**Steps to Verify:**
1. Go to [Google Cloud Console](https://console.cloud.google.com/)
2. Select project: **magnum-cricket-club**
3. Navigate to **APIs & Services** → **Credentials**
4. Find your **OAuth 2.0 Client IDs**:
   - You should see an **Android** client (client_type: 1)
   - You should see a **Web** client (client_type: 3)
5. Verify the Web Client ID matches: `1076211377209-ride5umkasmacjqfjgh9836f1ttb9uls.apps.googleusercontent.com`
6. If the Web client doesn't exist or is different:
   - Go to Firebase Console → Project Settings → Your apps
   - Make sure the Web Client ID is listed in `google-services.json`

### 3. App Not Completely Uninstalled and Rebuilt

The old app may have cached configuration.

**Steps to Fix:**
1. **Uninstall the app completely:**
   ```bash
   adb uninstall com.magnum.cricketclub
   ```
   Or manually: Settings → Apps → Magnum Cricket Club → Uninstall

2. **Clean the project:**
   ```bash
   cd /Users/loganantham.c/magnum-cricket-club
   ./gradlew clean
   ```

3. **Invalidate caches in Android Studio:**
   - File → Invalidate Caches / Restart
   - Select "Invalidate and Restart"

4. **Rebuild and reinstall:**
   ```bash
   ./gradlew assembleDebug
   adb install app/build/outputs/apk/debug/app-debug.apk
   ```

### 4. Firebase Project Settings Verification

**Verify in Firebase Console:**
1. Go to [Firebase Console](https://console.firebase.google.com/)
2. Select project: **magnum-cricket-club**
3. Go to **Project Settings** → **General** tab
4. Scroll to **Your apps** section
5. Verify:
   - ✅ Android app exists: `com.magnum.cricketclub`
   - ✅ SHA-1 fingerprint is listed: `BB:F1:E2:2D:4C:EF:31:B4:CC:41:3F:FD:E5:B9:CC:E5:D1:18:3B:6D`
   - ✅ `google-services.json` is downloaded and in `app/` directory

5. Go to **Authentication** → **Sign-in method**
   - ✅ **Google** is enabled
   - ✅ Support email is set

### 5. Google Cloud Console API Verification

**Verify APIs are enabled:**
1. Go to [Google Cloud Console](https://console.cloud.google.com/)
2. Select project: **magnum-cricket-club**
3. Navigate to **APIs & Services** → **Library**
4. Search and ensure these APIs are **ENABLED**:
   - ✅ Google Sign-In API
   - ✅ Identity Toolkit API
   - ✅ Firebase Authentication API

## Step-by-Step Fix Process

### Step 1: Configure OAuth Consent Screen (CRITICAL)
Follow the steps in section 1 above. This is the most common cause.

### Step 2: Verify Firebase Configuration
1. Download fresh `google-services.json` from Firebase Console
2. Replace `app/google-services.json`
3. Verify it contains the `certificate_hash` field

### Step 3: Clean and Rebuild
```bash
cd /Users/loganantham.c/magnum-cricket-club
./gradlew clean
./gradlew assembleDebug
```

### Step 4: Uninstall Old App
```bash
adb uninstall com.magnum.cricketclub
```

### Step 5: Install Fresh Build
```bash
adb install app/build/outputs/apk/debug/app-debug.apk
```

### Step 6: Test
1. Open the app
2. Tap "Continue with Google"
3. Check Logcat for any errors

## Verification Checklist

Before testing, verify:
- [ ] OAuth consent screen is configured and published
- [ ] SHA-1 fingerprint is in Firebase Console
- [ ] `google-services.json` has `certificate_hash` field
- [ ] Web Client ID matches in code and `google-services.json`
- [ ] Google Sign-In is enabled in Firebase Authentication
- [ ] Required APIs are enabled in Google Cloud Console
- [ ] App is completely uninstalled from device
- [ ] Project is cleaned and rebuilt
- [ ] Fresh APK is installed

## Still Not Working?

If error 12501 persists after all steps:

1. **Check Logcat for additional errors:**
   ```bash
   adb logcat | grep -i "google\|auth\|firebase"
   ```

2. **Verify OAuth client configuration:**
   - Go to Google Cloud Console → APIs & Services → Credentials
   - Check that Android OAuth client has the correct package name
   - Check that Web OAuth client exists

3. **Try creating a new OAuth client:**
   - In Firebase Console → Project Settings → Your apps
   - Click "Add fingerprint" again (even if it exists)
   - Download new `google-services.json`
   - Replace and rebuild

4. **Wait 5-10 minutes:**
   - Google Cloud changes can take time to propagate
   - Try again after waiting

5. **Check if you're using the correct Google account:**
   - Make sure the Google account you're testing with is added as a test user in OAuth consent screen

## Quick Test Command

To verify your SHA-1 fingerprint:
```bash
keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android -keypass android | grep SHA1
```

Expected output:
```
SHA1: BB:F1:E2:2D:4C:EF:31:B4:CC:41:3F:FD:E5:B9:CC:E5:D1:18:3B:6D
```

## Additional Resources

- [Google Sign-In Android Setup](https://developers.google.com/identity/sign-in/android/start-integrating)
- [Firebase Authentication Setup](https://firebase.google.com/docs/auth/android/google-signin)
- [OAuth Consent Screen Guide](https://support.google.com/cloud/answer/10311615)
