#!/bin/bash

# Git Setup Script for Magnum Cricket Club
# Run this script after renaming the folder to magnum-cricket-club

echo "🚀 Setting up Git repository for Magnum Cricket Club..."

# Check if we're in the right directory
if [ ! -f "app/build.gradle" ]; then
    echo "❌ Error: Please run this script from the project root directory"
    exit 1
fi

# Verify google-services.json is in .gitignore
if grep -q "google-services.json" .gitignore; then
    echo "✅ google-services.json is already in .gitignore"
else
    echo "⚠️  Warning: google-services.json not found in .gitignore"
fi

# Initialize git repository
echo ""
echo "📦 Initializing git repository..."
git init

# Add all files
echo ""
echo "📝 Adding files to git..."
git add .

# Check if google-services.json would be added (it shouldn't)
if git status --porcelain | grep -q "google-services.json"; then
    echo "⚠️  Warning: google-services.json is being tracked! Removing it..."
    git rm --cached app/google-services.json 2>/dev/null || true
    git rm --cached google-services.json 2>/dev/null || true
fi

# Create initial commit
echo ""
echo "💾 Creating initial commit..."
git commit -m "Initial commit: Magnum Cricket Club app"

# Set up remote
echo ""
echo "🔗 Setting up remote repository..."
git remote add origin https://github.com/loganantham/magnum-cricket-club.git 2>/dev/null || {
    echo "⚠️  Remote 'origin' already exists. Removing and re-adding..."
    git remote remove origin
    git remote add origin https://github.com/loganantham/magnum-cricket-club.git
}

# Rename branch to main
echo ""
echo "🌿 Setting branch to 'main'..."
git branch -M main

# Show status
echo ""
echo "📊 Current git status:"
git status --short | head -20

echo ""
echo "✅ Git setup complete!"
echo ""
echo "📤 To push to GitHub, run:"
echo "   git push -u origin main"
echo ""
echo "⚠️  Note: Make sure you've renamed the folder to 'magnum-cricket-club' first!"
echo "⚠️  Note: Make sure google-services.json is NOT in the list above!"
