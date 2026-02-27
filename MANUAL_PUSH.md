# Manual Push Instructions

The automated push is having authentication issues. Please follow these steps manually:

## Option 1: Use Token in URL (Recommended)

```bash
cd /Users/loganantham.c/magnum-cricket-club

# Remove token from URL for security (we'll add it temporarily)
git remote set-url origin https://github.com/loganantham/magnum-cricket-club.git

# Push with token in URL (one-time)
# NOTE: Replace YOUR_TOKEN_HERE with your actual GitHub Personal Access Token
git push https://loganantham:YOUR_TOKEN_HERE@github.com/loganantham/magnum-cricket-club.git main

# After successful push, remove token from remote URL
git remote set-url origin https://github.com/loganantham/magnum-cricket-club.git
```

## Option 2: Use GitHub CLI (if installed)

```bash
cd /Users/loganantham.c/magnum-cricket-club
# NOTE: Replace YOUR_TOKEN_HERE with your actual GitHub Personal Access Token
gh auth login --with-token <<< "YOUR_TOKEN_HERE"
git push -u origin main
```

## Option 3: Use Credential Helper

```bash
cd /Users/loganantham.c/magnum-cricket-club

# Configure credential helper to use token
# NOTE: Replace YOUR_TOKEN_HERE with your actual GitHub Personal Access Token
git config credential.helper store
echo "https://loganantham:YOUR_TOKEN_HERE@github.com" > ~/.git-credentials

# Push
git push -u origin main
```

## Option 4: Check Token Permissions

The 403 error might mean the token doesn't have `repo` scope. To fix:

1. Go to: https://github.com/settings/tokens
2. Find your token or create a new one
3. Make sure it has **`repo`** scope checked
4. Regenerate if needed
5. Use the new token

## Verify After Push

After successful push, check:
- https://github.com/loganantham/magnum-cricket-club
- Verify all files are there
- Verify `google-services.json` is NOT in the repository
