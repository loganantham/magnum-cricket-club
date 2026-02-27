# Google Sign-In Troubleshooting Guide

## Common Issues and Solutions

### 1. Network Error ("There was a problem connecting to www.google.com")

**Possible Causes:**
- No internet connection
- Firewall blocking Google services
- VPN or proxy interfering

**Solutions:**
- Check your internet connection
- Disable VPN if active
- Try on a different network
- Check if other Google services work (Gmail, YouTube)

### 2. SHA-1 Certificate Fingerprint Not Configured (Error Code 12501)

**This is the cause of your current error!** Error code 12501 means DEVELOPER_ERROR, which almost always indicates the SHA-1 fingerprint is missing or incorrect in Firebase.

**Your Debug SHA-1 Fingerprint:**
```
BB:F1:E2:2D:4C:EF:31:B4:CC:41:3F:FD:E5:B9:CC:E5:D1:18:3B:6D
```

**To add SHA-1 to Firebase (REQUIRED):**
1. Go to [Firebase Console](https://console.firebase.google.com/)
2. Select your project: **magnum-cricket-club**
3. Go to **Project Settings** → **General** tab
4. Scroll down to **Your apps** section
5. Find your Android app (`com.magnum.cricketclub`)
6. Click **Add fingerprint** button
7. Paste this SHA-1: `BB:F1:E2:2D:4C:EF:31:B4:CC:41:3F:FD:E5:B9:CC:E5:D1:18:3B:6D`
8. Click **Save**
9. **IMPORTANT:** Download the updated `google-services.json` file
10. Replace `app/google-services.json` with the new file
11. **Rebuild your app completely** (Clean and Rebuild)

**For Release builds (when you publish):**
You'll need to add the release SHA-1 fingerprint as well:
```bash
keytool -list -v -keystore /path/to/your/keystore.jks -alias your-key-alias
```

### 3. Web Client ID Mismatch

The Web Client ID in the code should match the one in `google-services.json`.

**Current Web Client ID in code:**
`1076211377209-ride5umkasmacjqfjgh9836f1ttb9uls.apps.googleusercontent.com`

**To verify:**
1. Check `app/google-services.json`
2. Look for `oauth_client` array
3. Find entry with `client_type: 3` (Web client)
4. Verify the `client_id` matches the one above

### 4. Google Sign-In Not Enabled in Firebase

**To verify:**
1. Go to Firebase Console → **Authentication** → **Sign-in method**
2. Ensure **Google** is enabled
3. If not, enable it and configure

### 5. Check Logcat for Detailed Errors

When Google Sign-In fails, check Logcat for detailed error messages:

```bash
adb logcat | grep -i "AuthActivity\|GoogleSignIn\|FirebaseAuth"
```

Look for:
- Error codes (like `12500`, `10`, `8`)
- Network errors
- Authentication errors
- Configuration errors

## Testing Steps

1. **Verify Internet Connection:**
   - Open a browser and go to google.com
   - If it doesn't load, fix your network first

2. **Check Firebase Configuration:**
   - Verify `google-services.json` is in `app/` directory
   - Verify Google Sign-In is enabled in Firebase Console
   - Verify SHA-1 fingerprint is added

3. **Test Google Sign-In:**
   - Tap "Continue with Google" button
   - Check Logcat for any errors
   - Note the exact error message

4. **Common Error Codes:**
   - `12500`: Sign-in cancelled by user
   - `10`: Developer error (check SHA-1 and configuration)
   - `8`: Network error
   - `7`: API not connected (check Google Services)

## Quick Fix Checklist

- [ ] Internet connection is working
- [ ] SHA-1 fingerprint is added to Firebase Console
- [ ] Updated `google-services.json` is in `app/` directory
- [ ] Google Sign-In is enabled in Firebase Console
- [ ] App is rebuilt after updating `google-services.json`
- [ ] No VPN or proxy is interfering
