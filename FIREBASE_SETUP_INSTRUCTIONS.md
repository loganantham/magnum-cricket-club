# Firebase Setup Instructions

## Step 1: Create Firebase Project

1. Go to [Firebase Console](https://console.firebase.google.com/)
2. Click "Add project" or select an existing project
3. Follow the setup wizard:
   - Enter project name (e.g., "Magnum Cricket Club")
   - Enable/disable Google Analytics (optional)
   - Click "Create project"

## Step 2: Add Android App

1. In Firebase Console, click the Android icon (or "Add app")
2. Enter package name: `com.magnum.cricketclub`
3. Enter app nickname (optional): "Magnum Cricket Club"
4. Enter SHA-1 (optional, for Google Sign-In):
   - Run: `keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android -keypass android`
   - Copy the SHA-1 fingerprint
5. Click "Register app"

## Step 3: Download google-services.json

1. Download the `google-services.json` file
2. Place it in: `app/` directory (same level as `build.gradle`)
3. **Important**: Do NOT commit this file to public repositories

## Step 4: Enable Authentication

1. In Firebase Console, go to "Authentication"
2. Click "Get started"
3. Enable "Email/Password" sign-in method
4. Click "Save"

## Step 5: Enable Firestore Database

1. In Firebase Console, go to "Firestore Database"
2. Click "Create database"
3. Choose "Start in test mode" (for development)
   - **Note**: For production, set up proper security rules
4. Select a location (choose closest to your users)
5. Click "Enable"

## Step 6: Set Firestore Security Rules

1. In Firestore Database, go to "Rules" tab
2. Replace with these rules (for development):

```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    // Allow authenticated users to read/write their own data
    match /expenses/{expenseId} {
      allow read, write: if request.auth != null 
        && request.auth.uid == resource.data.userId;
    }
    
    match /expenseTypes/{typeId} {
      allow read, write: if request.auth != null;
    }
    
    match /incomeTypes/{typeId} {
      allow read, write: if request.auth != null;
    }
  }
}
```

3. Click "Publish"

## Step 7: Build and Run

1. Sync Gradle files in Android Studio
2. Build the project
3. Run the app
4. You should see the authentication screen

## Testing

1. **Sign Up**: Create a new account with email/password
2. **Sign In**: Use the same credentials
3. **Add Data**: Create expenses/income - they should sync automatically
4. **Check Firestore**: Go to Firebase Console → Firestore Database to see your data

## Production Security Rules

For production, use these more secure rules:

```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    // Helper function
    function isAuthenticated() {
      return request.auth != null;
    }
    
    function isOwner(userId) {
      return isAuthenticated() && request.auth.uid == userId;
    }
    
    // Expenses - users can only access their own
    match /expenses/{expenseId} {
      allow read: if isOwner(resource.data.userId);
      allow create: if isAuthenticated() && request.resource.data.userId == request.auth.uid;
      allow update, delete: if isOwner(resource.data.userId);
    }
    
    // Types - users can read all, write their own team's
    match /expenseTypes/{typeId} {
      allow read: if isAuthenticated();
      allow write: if isAuthenticated() && request.resource.data.teamId == request.auth.uid;
    }
    
    match /incomeTypes/{typeId} {
      allow read: if isAuthenticated();
      allow write: if isAuthenticated() && request.resource.data.teamId == request.auth.uid;
    }
  }
}
```

## Troubleshooting

### "google-services.json not found"
- Make sure the file is in `app/` directory
- Sync Gradle files
- Clean and rebuild project

### "Authentication failed"
- Check if Email/Password is enabled in Firebase Console
- Verify internet connection
- Check Firebase Console for error logs

### "Permission denied" in Firestore
- Check security rules
- Make sure user is authenticated
- Verify rules are published

### Data not syncing
- Check internet connection
- Verify user is signed in
- Check Firebase Console for errors
- Look at Logcat for sync errors

## Next Steps

1. **Team Management**: Implement team sharing (multiple users can share data)
2. **Offline Support**: Already implemented - works offline, syncs when online
3. **Real-time Updates**: Can be enhanced with Firestore listeners
4. **Backup/Restore**: Data is automatically backed up in Firestore

## Cost Estimation

**Free Tier (Spark Plan):**
- 50,000 reads/day
- 20,000 writes/day
- 20,000 deletes/day
- 1 GB storage

**Typical Usage:**
- Small team (5-10 users): Free tier is sufficient
- Medium team (10-50 users): May need Blaze plan ($25/month)
- Large team (50+ users): Custom pricing
