# Google Sign-In Setup Instructions

To enable Google Sign-In in your app, follow these steps:

## 1. Enable Google Sign-In in Firebase Console

1. Go to [Firebase Console](https://console.firebase.google.com/)
2. Select your project: **magnum-cricket-club**
3. Navigate to **Authentication** → **Sign-in method**
4. Click on **Google** and enable it
5. Add your app's package name: `com.magnum.cricketclub`
6. Add your app's SHA-1 certificate fingerprint (for debug builds, get it using):
   ```bash
   keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android -keypass android
   ```
7. Save the configuration

## 2. Get the Web Client ID

1. In Firebase Console, go to **Project Settings** → **General**
2. Scroll down to **Your apps** section
3. Under **Web apps**, you'll see a **Web client ID** (it looks like: `xxxxx.apps.googleusercontent.com`)
4. Copy this Web Client ID

## 3. Update strings.xml

1. Open `app/src/main/res/values/strings.xml`
2. Find the line: `<string name="default_web_client_id">YOUR_WEB_CLIENT_ID</string>`
3. Replace `YOUR_WEB_CLIENT_ID` with the actual Web Client ID from step 2
4. Example: `<string name="default_web_client_id">123456789-abc123.apps.googleusercontent.com</string>`

## 4. Rebuild the App

After updating the Web Client ID, rebuild and run the app. The "Continue with Google" button will appear on the login screen.

## Notes

- The Google Sign-In button will be automatically hidden if the Web Client ID is not configured
- Users can still sign in/sign up using email and password
- Google Sign-In allows users to sign in with their Google account without entering a password
