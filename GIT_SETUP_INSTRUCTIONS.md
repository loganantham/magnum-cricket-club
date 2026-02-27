# Git Setup and Folder Rename Instructions

## ✅ Already Done
- ✅ `google-services.json` is already in `.gitignore` (line 63)

## Step 1: Rename Project Folder

**Option A: Using Terminal (Recommended)**
```bash
cd /Users/loganantham.c
mv magnum-expenses magnum-cricket-club
cd magnum-cricket-club
```

**Option B: Using Finder**
1. Open Finder
2. Navigate to `/Users/loganantham.c/`
3. Right-click on `magnum-expenses` folder
4. Select "Rename"
5. Change to `magnum-cricket-club`
6. Press Enter

**After renaming:**
- Close Android Studio if it's open
- Reopen Android Studio and open the project from the new location: `/Users/loganantham.c/magnum-cricket-club`

## Step 2: Initialize Git Repository

Open Terminal and run these commands:

```bash
# Navigate to the project directory
cd /Users/loganantham.c/magnum-cricket-club

# Initialize git repository
git init

# Add all files (google-services.json will be ignored automatically)
git add .

# Create initial commit
git commit -m "Initial commit: Magnum Cricket Club app"
```

## Step 3: Set Up Remote and Push

```bash
# Add remote repository
git remote add origin https://github.com/loganantham/magnum-cricket-club.git

# Rename branch to main (if needed)
git branch -M main

# Push to remote
git push -u origin main
```

## Step 4: Verify google-services.json is Ignored

To verify that `google-services.json` won't be pushed:

```bash
# Check git status - google-services.json should NOT appear
git status

# If it appears, verify .gitignore
cat .gitignore | grep google-services
```

You should see: `google-services.json` in the output.

## Troubleshooting

### If you get "remote origin already exists":
```bash
git remote remove origin
git remote add origin https://github.com/loganantham/magnum-cricket-club.git
```

### If you get authentication error:
- Make sure you're logged into GitHub
- You may need to use a personal access token instead of password
- Or set up SSH keys

### If you need to update .gitignore:
The file is already configured, but if you need to verify:
- Open `.gitignore`
- Make sure line 63 has: `google-services.json`

## Important Notes

1. **google-services.json is already ignored** - You don't need to add it again
2. **After renaming folder**, make sure to:
   - Close and reopen Android Studio
   - Update any IDE bookmarks or shortcuts
3. **Before pushing**, make sure:
   - All your code changes are committed
   - You're happy with the current state
   - google-services.json is not in the staging area

## Quick Command Summary

```bash
# 1. Rename folder (if not done via Finder)
cd /Users/loganantham.c
mv magnum-expenses magnum-cricket-club
cd magnum-cricket-club

# 2. Initialize git
git init
git add .
git commit -m "Initial commit: Magnum Cricket Club app"

# 3. Set remote and push
git remote add origin https://github.com/loganantham/magnum-cricket-club.git
git branch -M main
git push -u origin main
```
