# MediaVaultApp (or AndroidStorageVault)

An Android application designed to demonstrate and manage text summaries using various file storage approaches available on the Android platform. This project meticulously implements and showcases:

- **Internal Storage:** App-specific private storage for sensitive data.
- **App-Specific External Storage (Private External):** Storage on external devices that is private to the app and uninstalled with the app.
- **Shared/Public External Storage:** Interaction with user-accessible directories (specifically the `Downloads` directory) using Android's modern Scoped Storage APIs for devices running Android 10 (API 29) and above, and traditional file access for older Android versions.

## Features:

- **CRUD Operations:**
    - **Create:** Save new text summaries with a chosen storage type.
    - **Read:** List all saved summaries from internal, private external, and shared storage.
    - **Update:** Modify existing summaries.
    - **Delete:** Remove summaries.
- **Dynamic UI:** A Compose-based UI for seamless interaction.
- **Permission Handling:** Robust request and management of necessary storage permissions.
- **Scoped Storage Implementation:** Proper handling of `MediaStore` APIs for shared files on API 29+ and utilization of the Storage Access Framework (`ACTION_OPEN_DOCUMENT_TREE`) for user-selected directory access.

## Technologies Used:

- Kotlin
- Jetpack Compose
- Android Architecture Components (Lifecycle, Flow)
- MediaStore API (for Shared Storage on API 29+)
- Storage Access Framework (SAF) (`ACTION_OPEN_DOCUMENT_TREE`, `DocumentFile`)
- Accompanist Permissions Library

## How to Run:

1.  Clone this repository: `git clone https://github.com/YourUsername/MediaVaultApp.git`
2.  Open the project in Android Studio.
3.  Build and run on an Android emulator or physical device (API 23+ recommended for full testing of all storage types, specifically API 29+ for Scoped Storage testing).

## Permissions:

This app requests the following permissions:
- `READ_EXTERNAL_STORAGE` (maxSdkVersion 29/32, depending on Android version)
- `WRITE_EXTERNAL_STORAGE` (maxSdkVersion 29)

For Android 10 (API 29) and above, the app leverages Scoped Storage. For shared storage, it either utilizes `ACTION_OPEN_DOCUMENT_TREE` (to allow user selection of a directory) or interacts directly with the `Downloads` directory via `MediaStore`.

---
