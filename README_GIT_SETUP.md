# Git Setup - Quick Start Guide

## ✅ Pre-Flight Checklist

- [x] `google-services.json` is in `.gitignore` (line 63)
- [x] `google-services.json` file exists at `app/google-services.json`
- [x] Setup script created: `setup-git.sh`

## Step 1: Rename Folder (Do This First!)

**Using Terminal:**
```bash
cd /Users/loganantham.c
mv magnum-expenses magnum-cricket-club
cd magnum-cricket-club
```

**Or using Finder:**
1. Navigate to `/Users/loganantham.c/`
2. Rename `magnum-expenses` to `magnum-cricket-club`
3. Open Terminal and: `cd /Users/loganantham.c/magnum-cricket-club`

## Step 2: Run Setup Script

After renaming the folder, run:

```bash
cd /Users/loganantham.c/magnum-cricket-club
./setup-git.sh
```

This will:
- ✅ Initialize git repository
- ✅ Add all files (google-services.json will be automatically ignored)
- ✅ Create initial commit
- ✅ Set up remote repository
- ✅ Rename branch to 'main'

## Step 3: Push to GitHub

After the script completes successfully:

```bash
git push -u origin main
```

## Manual Setup (Alternative)

If you prefer to run commands manually:

```bash
# Navigate to project (after renaming)
cd /Users/loganantham.c/magnum-cricket-club

# Initialize git
git init

# Add files
git add .

# Verify google-services.json is NOT being tracked
git status | grep google-services.json
# (Should return nothing - if it shows files, they'll be ignored)

# Create commit
git commit -m "Initial commit: Magnum Cricket Club app"

# Add remote
git remote add origin https://github.com/loganantham/magnum-cricket-club.git

# Set branch to main
git branch -M main

# Push
git push -u origin main
```

## Verification

After pushing, verify on GitHub:
1. Go to https://github.com/loganantham/magnum-cricket-club
2. Check that `google-services.json` is NOT in the repository
3. Check that all your code files are there

## Troubleshooting

### "remote origin already exists"
```bash
git remote remove origin
git remote add origin https://github.com/loganantham/magnum-cricket-club.git
```

### "Authentication failed"
- Make sure you're logged into GitHub
- You may need a Personal Access Token instead of password
- Or set up SSH keys

### "google-services.json appears in git status"
This shouldn't happen, but if it does:
```bash
git rm --cached app/google-services.json
git commit -m "Remove google-services.json from tracking"
```

## Important Notes

1. **Never commit google-services.json** - It contains sensitive Firebase configuration
2. **After renaming folder**, close and reopen Android Studio
3. **The setup script** will verify everything is correct before proceeding
