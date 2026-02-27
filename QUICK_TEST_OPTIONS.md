# Testing Without Full Google Cloud Console Setup

## Short Answer

**You cannot test Google Sign-In without Google Cloud Console**, but you have options:

1. **Quick 5-minute minimal setup** (recommended)
2. **Test with email/password auth instead** (works immediately)
3. **Skip authentication for development** (if you just want to test other features)

---

## Option 1: Quick 5-Minute Minimal Setup ⚡ (Recommended)

This is the fastest way to get Google Sign-In working. You only need to configure the OAuth consent screen:

### Steps:

1. **Go to Google Cloud Console:**
   - Visit: https://console.cloud.google.com/
   - Select project: **magnum-cricket-club** (or create one if needed)

2. **Configure OAuth Consent Screen (2 minutes):**
   - Navigate to: **APIs & Services** → **OAuth consent screen**
   - Select **External** user type → Click **Create**
   - Fill in:
     - **App name:** `Magnum Cricket Club`
     - **User support email:** Your email
     - **Developer contact:** Your email
   - Click **Save and Continue**
   - **Scopes:** Click **Save and Continue** (default scopes are fine)
   - **Test users:** Add your Google account email → Click **Save and Continue**
   - **Summary:** Click **Back to Dashboard**

3. **That's it!** The OAuth consent screen is now configured.

4. **Test:**
   ```bash
   adb uninstall com.magnum.cricketclub
   ./gradlew clean assembleDebug
   adb install app/build/outputs/apk/debug/app-debug.apk
   ```

**Note:** You don't need to "publish" the app - test mode is fine for development. Just add your email as a test user.

---

## Option 2: Test with Email/Password Auth Instead ✅

Your app already supports email/password authentication! You can test the entire flow without Google Sign-In:

### How to Use:

1. **The Google Sign-In button will be hidden** if not configured (your code already does this)
2. **Use email/password fields** to:
   - Sign up with a new account
   - Sign in with existing account
3. **All Firebase features work** (Firestore sync, etc.)

### To Hide Google Sign-In Button (Optional):

The button is already hidden when Google Sign-In isn't configured, but if you want to ensure it's always hidden during development, you can temporarily comment out the Google Sign-In initialization in `AuthActivity.kt`.

---

## Option 3: Skip Authentication for Development 🚀

If you just want to test other app features without authentication:

### Quick Modification:

Your `AuthActivity.kt` already has a fallback - if Firebase isn't configured, it skips to HomeActivity. You can temporarily modify it to always skip auth:

```kotlin
// In AuthActivity.onCreate(), add this at the top:
if (BuildConfig.DEBUG) {
    // Skip auth in debug mode for testing
    navigateToHome()
    return
}
```

Or create a debug flag:
```kotlin
private val SKIP_AUTH_FOR_TESTING = true // Set to false when ready

override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    
    if (SKIP_AUTH_FOR_TESTING) {
        navigateToHome()
        return
    }
    // ... rest of code
}
```

---

## Why Google Cloud Console is Required

Google Sign-In uses OAuth 2.0, which requires:
- **OAuth consent screen** - Google's security requirement
- **OAuth client IDs** - Already configured via Firebase (you have this)
- **SHA-1 fingerprint** - Already added (you have this)

The **only missing piece** is the OAuth consent screen, which takes ~5 minutes to set up.

---

## Recommendation

**For quick testing:** Use **Option 2** (email/password auth) - it works immediately and tests all your Firebase features.

**For Google Sign-In:** Use **Option 1** (5-minute setup) - it's the proper way and you'll need it eventually anyway.

---

## Quick Setup Checklist

If you choose Option 1, here's the minimal checklist:

- [ ] Go to Google Cloud Console
- [ ] Navigate to OAuth consent screen
- [ ] Fill in app name and your email
- [ ] Add your email as test user
- [ ] Save all steps
- [ ] Uninstall old app
- [ ] Rebuild and test

**Total time: ~5 minutes**

---

## Still Having Issues?

If you set up the OAuth consent screen and still get error 12501:

1. **Wait 5-10 minutes** - Changes can take time to propagate
2. **Verify test user** - Make sure your Google account email is added as a test user
3. **Check Logcat** - Look for more detailed error messages
4. **Run verification script:** `./verify_google_signin.sh`
