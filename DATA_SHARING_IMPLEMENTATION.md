# Data Sharing Implementation Guide

## Current Database

Your app currently uses:
- **Room Database** (Android's SQLite abstraction)
- **Local storage** on each device
- Database file: `cricket_expense_database`
- Location: `/data/data/com.magnum.expenses/databases/cricket_expense_database`

## Options for Data Sharing

### Option 1: Firebase Firestore (Recommended) ⭐
**Pros:**
- Real-time synchronization
- Offline support
- Built-in authentication
- Free tier (generous limits)
- Easy to implement
- Automatic conflict resolution

**Cons:**
- Requires Firebase account
- Data stored on Google servers
- Requires internet connection

### Option 2: Backend API + Cloud Database
**Pros:**
- Full control over data
- Can use any database (PostgreSQL, MySQL, MongoDB)
- Custom business logic
- Better for enterprise solutions

**Cons:**
- Requires server setup and maintenance
- More complex implementation
- Higher costs
- Need to handle security, scaling, etc.

### Option 3: Google Drive/Cloud Storage Sync
**Pros:**
- Simple file-based sync
- Uses existing cloud storage

**Cons:**
- Not real-time
- Manual sync required
- Limited query capabilities
- Conflict resolution is complex

## Recommended Implementation: Firebase Firestore

### Step 1: Setup Firebase Project

1. Go to [Firebase Console](https://console.firebase.google.com/)
2. Create a new project
3. Add Android app with package name: `com.magnum.expenses`
4. Download `google-services.json` and place it in `app/` directory

### Step 2: Add Firebase Dependencies

Add to `app/build.gradle`:
```gradle
plugins {
    // ... existing plugins
    id 'com.google.gms.google-services'
}

dependencies {
    // ... existing dependencies
    
    // Firebase
    implementation platform('com.google.firebase:firebase-bom:32.7.0')
    implementation 'com.google.firebase:firebase-firestore-ktx'
    implementation 'com.google.firebase:firebase-auth-ktx'
}
```

Add to `build.gradle` (project level):
```gradle
buildscript {
    dependencies {
        // ... existing
        classpath 'com.google.gms:google-services:4.4.0'
    }
}
```

### Step 3: Database Structure in Firestore

```
expenses/
  {expenseId}/
    - id: Long
    - expenseTypeId: Long?
    - incomeTypeId: Long?
    - amount: Double
    - description: String
    - date: Long
    - isIncome: Boolean
    - userId: String (for multi-user)
    - teamId: String (for team sharing)

expenseTypes/
  {typeId}/
    - id: Long
    - name: String
    - description: String
    - teamId: String

incomeTypes/
  {typeId}/
    - id: Long
    - name: String
    - description: String
    - teamId: String

teams/
  {teamId}/
    - id: String
    - name: String
    - members: [userId1, userId2, ...]
    - createdBy: String
```

### Step 4: Implementation Strategy

**Hybrid Approach (Recommended):**
- Keep Room database for offline-first experience
- Sync with Firestore in background
- Use Room as cache, Firestore as source of truth

**Benefits:**
- Works offline
- Fast local queries
- Automatic sync when online
- Conflict resolution

### Step 5: Sync Service Implementation

Create a sync service that:
1. Uploads local changes to Firestore
2. Downloads remote changes to Room
3. Handles conflicts (last-write-wins or merge strategy)
4. Syncs periodically or on app start

## Implementation Steps

### Phase 1: Add Firebase Support
1. Add Firebase dependencies
2. Initialize Firebase in Application class
3. Add authentication (optional but recommended)

### Phase 2: Create Sync Layer
1. Create `FirestoreRepository` for Firestore operations
2. Create `SyncService` to handle bidirectional sync
3. Add sync status indicators in UI

### Phase 3: Team/User Management
1. Add user authentication
2. Create team management
3. Add team member invitations
4. Implement permissions (read/write)

### Phase 4: Conflict Resolution
1. Implement conflict detection
2. Add merge strategies
3. Handle offline/online scenarios

## Quick Start Code Structure

```
data/
  ├── local/
  │   ├── AppDatabase.kt (existing Room DB)
  │   └── ...
  ├── remote/
  │   ├── FirestoreRepository.kt (new)
  │   └── FirestoreModels.kt (new)
  └── sync/
      ├── SyncService.kt (new)
      └── SyncManager.kt (new)
```

## Security Rules Example (Firestore)

```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    // Expenses - users can only access their team's expenses
    match /expenses/{expenseId} {
      allow read, write: if request.auth != null 
        && resource.data.teamId in get(/databases/$(database)/documents/users/$(request.auth.uid)).data.teams;
    }
    
    // Teams - users can read their teams, create new ones
    match /teams/{teamId} {
      allow read: if request.auth != null 
        && request.auth.uid in resource.data.members;
      allow create: if request.auth != null;
      allow update: if request.auth != null 
        && request.auth.uid == resource.data.createdBy;
    }
  }
}
```

## Migration Strategy

1. **Initial Setup:**
   - Export existing Room data
   - Upload to Firestore
   - Set up sync

2. **Ongoing:**
   - Local changes → Queue for sync
   - Remote changes → Update local DB
   - Conflict → Resolve based on strategy

## Cost Considerations

**Firebase Free Tier:**
- 50K reads/day
- 20K writes/day
- 20K deletes/day
- 1 GB storage

**Typical Usage:**
- Small team (5-10 users): Well within free tier
- Medium team (10-50 users): May need Blaze plan ($25/month)
- Large team (50+ users): Custom pricing

## Alternative: Simple REST API

If you prefer a custom backend:

1. **Backend Options:**
   - Node.js + Express + MongoDB
   - Python + Flask/Django + PostgreSQL
   - Java Spring Boot + MySQL

2. **API Endpoints Needed:**
   - `POST /api/expenses` - Create expense
   - `GET /api/expenses` - List expenses
   - `PUT /api/expenses/:id` - Update expense
   - `DELETE /api/expenses/:id` - Delete expense
   - Similar for types, teams, etc.

3. **Sync Strategy:**
   - Poll for changes every X minutes
   - Or use WebSockets for real-time updates

## Recommendation

**For your use case, I recommend Firebase Firestore because:**
1. ✅ Quick to implement (2-3 days)
2. ✅ Real-time sync out of the box
3. ✅ Handles offline scenarios
4. ✅ Built-in security
5. ✅ Free for small teams
6. ✅ No server maintenance

Would you like me to implement the Firebase Firestore integration for your app?
