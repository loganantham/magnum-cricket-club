# Fix Token Permission Issue

## Problem
Getting 403 error when pushing - this means the token doesn't have the right permissions.

## Solution: Create New Token with Repo Permissions

### Step 1: Create New Token
1. Go to: https://github.com/settings/tokens/new
2. **Token name**: `magnum-cricket-club-push`
3. **Expiration**: Choose your preference (90 days recommended)
4. **Select scopes**: 
   - ✅ **repo** (Full control of private repositories) - **REQUIRED**
   - This includes:
     - repo:status
     - repo_deployment
     - public_repo
     - repo:invite
     - security_events
5. Click **Generate token**
6. **Copy the token immediately** (you won't see it again!)

### Step 2: Push with New Token

```bash
cd /Users/loganantham.c/magnum-cricket-club

# Set remote with new token
git remote set-url origin https://loganantham:YOUR_NEW_TOKEN@github.com/loganantham/magnum-cricket-club.git

# Push
git push -u origin main
```

### Alternative: Use SSH (More Secure)

If you have SSH keys set up:

```bash
cd /Users/loganantham.c/magnum-cricket-club

# Change to SSH URL
git remote set-url origin git@github.com:loganantham/magnum-cricket-club.git

# Push (will use SSH key)
git push -u origin main
```

## Current Status
- ✅ Repository exists on GitHub
- ✅ Local git is initialized
- ✅ All files committed
- ✅ google-services.json is ignored
- ⚠️ Need token with `repo` scope to push

## Quick Test Command

After getting new token, test it:
```bash
curl -H "Authorization: token YOUR_NEW_TOKEN" https://api.github.com/user
```

If it returns your user info, the token works!
