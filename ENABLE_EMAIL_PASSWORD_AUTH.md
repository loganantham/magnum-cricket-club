# Enable Email/Password Authentication in Firebase

## Error Message

```
This operation is not allowed. This may be because the given sign-in provider is disabled for this Firebase project. Enable it in the Firebase console, under the sign-in method tab of the Auth section.
```

This means **Email/Password authentication is disabled** in your Firebase project.

---

## Quick Fix (2 minutes)

### Step 1: Go to Firebase Console

1. Visit: https://console.firebase.google.com/
2. Select your project: **magnum-cricket-club**

### Step 2: Enable Email/Password Authentication

1. In the left sidebar, click **Authentication**
2. Click on the **Sign-in method** tab (at the top)
3. You'll see a list of sign-in providers
4. Find **Email/Password** in the list
5. Click on **Email/Password**
6. Toggle **Enable** to **ON**
7. Click **Save**

### Step 3: Test Again

1. Go back to your app
2. Try signing up again with email/password
3. It should work now!

---

## Visual Guide

```
Firebase Console
├── Authentication (click this)
    ├── Sign-in method (click this tab)
        ├── Email/Password (click this)
            ├── Enable: ON (toggle this)
            └── Save (click this)
```

---

## Alternative: Enable via Firebase CLI

If you prefer command line:

```bash
# Install Firebase CLI (if not installed)
npm install -g firebase-tools

# Login
firebase login

# Enable email/password auth
firebase auth:export --project magnum-cricket-club
```

But the web console is easier!

---

## What This Enables

After enabling Email/Password:
- ✅ Users can sign up with email and password
- ✅ Users can sign in with email and password
- ✅ Password reset functionality (if you implement it)
- ✅ Email verification (if you enable it)

---

## Additional Settings (Optional)

While you're in the Email/Password settings, you can also:

1. **Enable Email link (passwordless sign-in)** - Optional
2. **Enable Email verification** - Recommended for production
   - Requires users to verify their email before signing in

For now, just enabling Email/Password is enough to test.

---

## Verification

After enabling, you should see:
- ✅ Email/Password shows as **Enabled** in the Sign-in method list
- ✅ Your app can now create accounts with email/password
- ✅ Your app can now sign in with email/password

---

## Still Not Working?

If you still get errors after enabling:

1. **Wait 1-2 minutes** - Changes can take a moment to propagate
2. **Check Firebase Console** - Verify Email/Password shows as "Enabled"
3. **Restart your app** - Close and reopen the app
4. **Check Logcat** - Look for any other error messages

---

## Quick Checklist

- [ ] Go to Firebase Console
- [ ] Navigate to Authentication → Sign-in method
- [ ] Click Email/Password
- [ ] Toggle Enable to ON
- [ ] Click Save
- [ ] Test sign up in your app

**Total time: ~2 minutes**
