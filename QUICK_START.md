# Quick Start Guide

## Project Overview

This is a fully functional Android application for managing expenses with the following features:

✅ **Expense Management**: Add, edit, delete expenses with automatic balance calculation
✅ **Expense Types**: Create and manage custom expense categories
✅ **Automatic Calculations**: Balance updates automatically when expenses are added/updated
✅ **WhatsApp Integration**: Send expense updates to WhatsApp groups
✅ **Configuration Screen**: Configure WhatsApp settings and team name
✅ **Modern UI**: Material Design with intuitive user experience

## Missing Scenarios Addressed

Beyond the core requirements, the app includes:

1. **View Expense History**: See all expenses in a scrollable list
2. **Edit Expenses**: Modify existing expenses
3. **Delete Expenses**: Remove expenses with confirmation
4. **Balance Display**: Real-time total balance with color coding
5. **Date Tracking**: All expenses are timestamped
6. **Empty States**: Helpful messages when no data exists
7. **Input Validation**: Ensures data integrity
8. **Offline Support**: All data stored locally

## How to Build

1. **Open in Android Studio**
   ```
   File → Open → Select magnum-expenses folder
   ```

2. **Sync Gradle**
   - Android Studio will automatically sync
   - Wait for dependencies to download (first time may take a few minutes)

3. **Run the App**
   - Connect Android device (API 24+) or start emulator
   - Click Run button or press Shift+F10

## First Run Steps

1. **Add Expense Types First**
   - Menu (⋮) → Expense Types
   - Tap + button
   - Add types like: Equipment, Travel, Food, Ground Fees, etc.

2. **Configure WhatsApp (Optional)**
   - Menu → Settings
   - Enter WhatsApp group ID
   - Enable notifications toggle
   - Enter team name
   - Save

3. **Start Adding Expenses**
   - Tap + button on main screen
   - Select expense type
   - Enter amount
   - Choose Expense (subtraction) or Income (addition)
   - Save

## WhatsApp Setup

To get your WhatsApp group ID:
1. Open WhatsApp Web/Desktop
2. Open your group
3. Click group name → Group info
4. Find invite link (format: `chat.whatsapp.com/XXXXXXXXX`)
5. Copy the ID part (XXXXXXXXX) or full link
6. Paste in Settings screen

## Project Structure

```
magnum-expenses/
├── app/
│   ├── src/main/
│   │   ├── java/com/magnum/expenses/
│   │   │   ├── data/          # Database, DAOs, Models, Repositories
│   │   │   ├── ui/            # Activities, ViewModels, Adapters
│   │   │   └── utils/         # Helper classes
│   │   ├── res/               # Layouts, strings, colors, themes
│   │   └── AndroidManifest.xml
│   └── build.gradle
├── build.gradle
├── settings.gradle
└── README.md
```

## Key Technologies

- **Kotlin**: Modern Android development
- **Room Database**: Local data persistence
- **MVVM Architecture**: Clean separation of concerns
- **Coroutines & Flow**: Asynchronous operations
- **Material Design**: Modern UI components

## Troubleshooting

**Build Errors?**
- Ensure Android SDK is installed
- Check Gradle sync completed successfully
- Verify internet connection for dependency download

**WhatsApp Not Opening?**
- Ensure WhatsApp is installed
- Check group ID is correct
- Try using full invite link instead of just ID

**No Expense Types?**
- You must add at least one expense type before adding expenses
- Go to Menu → Expense Types → Add

## Notes

- Minimum Android version: 7.0 (API 24)
- All data is stored locally (no internet required for core features)
- WhatsApp integration requires WhatsApp app installed
- Balance calculation is automatic and real-time
