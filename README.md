# Magnum Expenses

A fully functional Android application for managing expenses with automatic balance calculation and WhatsApp integration.

## Features

### 1. Expense Management
- Add, edit, and delete expenses
- Support for both income (addition) and expense (subtraction)
- Automatic balance calculation
- View expense history with details

### 2. Expense Types
- Create and manage custom expense types
- Organize expenses by categories
- Edit and delete expense types

### 3. Automatic Balance Calculation
- Real-time balance updates
- Visual indication (green for positive, red for negative)
- Automatic addition/subtraction based on expense type

### 4. WhatsApp Integration
- Send expense update notifications to WhatsApp groups
- Configurable WhatsApp group ID
- Enable/disable WhatsApp notifications
- Automatic message formatting with expense details

### 5. Configuration Screen
- Configure WhatsApp group ID
- Enable/disable WhatsApp notifications
- Set team name
- All settings are persisted locally

## Technical Details

### Architecture
- **MVVM (Model-View-ViewModel)** architecture
- **Room Database** for local data persistence
- **Kotlin Coroutines** and **Flow** for asynchronous operations
- **Material Design** components for modern UI

### Dependencies
- AndroidX libraries
- Room Database
- Material Components
- Kotlin Coroutines
- Navigation Component

### Database Schema
- **Expenses Table**: Stores all expense entries
- **Expense Types Table**: Stores expense categories
- **App Config Table**: Stores application settings

## Setup Instructions

1. **Open in Android Studio**
   - Open Android Studio
   - Select "Open an existing project"
   - Navigate to the `magnum-expenses` folder

2. **Sync Gradle**
   - Android Studio will automatically sync Gradle files
   - Wait for dependencies to download

3. **Build and Run**
   - Connect an Android device or start an emulator
   - Click "Run" or press Shift+F10
   - The app will install and launch

## Usage Guide

### First Time Setup

1. **Add Expense Types**
   - Tap the menu (three dots) → "Expense Types"
   - Tap the "+" button
   - Enter expense type name (e.g., "Equipment", "Travel", "Food")
   - Optionally add a description
   - Tap "Save"

2. **Configure WhatsApp (Optional)**
   - Tap the menu → "Settings"
   - Enter your WhatsApp group ID or invite link
   - Toggle "Enable WhatsApp Notifications" if desired
   - Enter your team name
   - Tap "Save"

### Adding Expenses

1. Tap the "+" button on the main screen
2. Select an expense type from the dropdown
3. Enter the amount
4. Optionally add a description
5. Select "Expense" (subtraction) or "Income" (addition)
6. Tap "Save"
7. If WhatsApp is enabled, the app will open WhatsApp to send the notification

### Viewing Balance

- The total balance is displayed at the top of the main screen
- Green color indicates positive balance
- Red color indicates negative balance
- Balance updates automatically when expenses are added, edited, or deleted

## WhatsApp Group ID Setup

To get your WhatsApp group ID:
1. Open WhatsApp Web or Desktop
2. Open your group
3. Click on the group name
4. Scroll down to find the group invite link
5. Copy the group ID from the link (the part after `chat.whatsapp.com/`)
6. Paste it in the configuration screen

Alternatively, you can use the full invite link.

## Additional Features

- **Empty State Handling**: Shows helpful messages when no data is available
- **Input Validation**: Ensures all required fields are filled
- **Date Tracking**: All expenses are timestamped
- **Material Design**: Modern, intuitive user interface
- **Offline Support**: All data is stored locally, works without internet

## Project Structure

```
magnum-expenses/
├── app/src/main/java/com/magnum/expenses/
│   ├── data/              # Data models, DAOs, Database, Repositories
│   ├── ui/
│   │   ├── expense/       # Expense management screens
│   │   ├── expensetype/   # Expense type management screens
│   │   └── config/        # Configuration screen
│   └── utils/             # Utility classes (WhatsApp helper, Date utils)
└── app/src/main/res/      # Layouts, strings, colors, themes
```

## Requirements

- Android 7.0 (API level 24) or higher
- WhatsApp installed (for WhatsApp notifications)

## License

This project is created for cricket team expense management purposes.
