# Fix Google Sign-In Error 12501 - Step by Step

## ✅ What's Already Done
- SHA-1 fingerprint added to Firebase Console
- google-services.json file updated with certificate_hash

## ⚠️ Still Getting Error 12501?

This usually means the app needs to be completely rebuilt. Follow these steps:

### Step 1: Clean Build
In Android Studio:
1. Go to **Build** → **Clean Project**
2. Wait for it to complete

### Step 2: Invalidate Caches
1. Go to **File** → **Invalidate Caches / Restart**
2. Select **Invalidate and Restart**
3. Wait for Android Studio to restart

### Step 3: Uninstall the App from Device
**IMPORTANT:** You must uninstall the old version completely:
1. On your device/emulator, go to Settings → Apps
2. Find "Magnum Cricket Club" (or "com.magnum.cricketclub")
3. Tap **Uninstall**
4. Confirm uninstallation

### Step 4: Rebuild and Reinstall
1. In Android Studio, go to **Build** → **Rebuild Project**
2. Wait for the build to complete
3. Run the app again (this will install a fresh copy)

### Step 5: Test Google Sign-In
1. Open the app
2. Tap "Continue with Google"
3. It should work now!

## Why This Is Necessary

When you update `google-services.json`, the Google Services plugin processes it during build time. The old app still has the old configuration cached. A clean rebuild ensures:
- The new google-services.json is processed
- The SHA-1 fingerprint is properly linked
- All cached configurations are cleared

## Alternative: Quick Test

If you want to test quickly without full rebuild:
1. Uninstall the app from device
2. In Android Studio: **Build** → **Clean Project**
3. Run the app again

## Still Not Working?

If error 12501 persists after following all steps:

1. **Verify SHA-1 in Firebase Console:**
   - Go to Firebase Console → Project Settings → Your apps
   - Check that the SHA-1 fingerprint is listed
   - It should show: `BB:F1:E2:2D:4C:EF:31:B4:CC:41:3F:FD:E5:B9:CC:E5:D1:18:3B:6D`

2. **Verify google-services.json:**
   - Check that `app/google-services.json` has the `certificate_hash` field
   - It should match your SHA-1 (without colons): `bbf1e22d4cef31b4cc413ffde5b9cce5d1183b6d`

3. **Check Logcat:**
   - Look for any other error messages
   - Filter by "AuthActivity" or "GoogleSignIn"

4. **Wait a few minutes:**
   - Sometimes Firebase takes a few minutes to propagate changes
   - Try again after 5-10 minutes
