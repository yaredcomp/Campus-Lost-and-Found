# Campus Lost & Found

A modern, real-time Android application built with Jetpack Compose, Material 3, and Firebase for university campuses to report, discover, and recover lost property.

---

## Accessing Your Firebase Project in Firebase Console

Your application is connected to the following provisioned Firebase project:

- **Project ID**: `gen-lang-client-0881186459`
- **Project Number**: `857919064292`
- **Firestore Database ID**: `ai-studio-android-campuslo-0f055f7c-1a01-43af-8486-8c434070a0a1`
- **Storage Bucket**: `gen-lang-client-0881186459.firebasestorage.app`
- **Android Package**: `com.aistudio.lostfound.cwqmpz`

### Direct Console Links

Sign in to [Firebase Console](https://console.firebase.google.com/) using the Google Account associated with your AI Studio session:

1. **Manage Users & Authentication**:
   - URL: [https://console.firebase.google.com/project/gen-lang-client-0881186459/authentication/users](https://console.firebase.google.com/project/gen-lang-client-0881186459/authentication/users)
   - View signed-in students, user UIDs, creation times, and disable/delete accounts if needed.

2. **View & Manage Lost/Found Listings (Cloud Firestore)**:
   - URL: [https://console.firebase.google.com/project/gen-lang-client-0881186459/firestore](https://console.firebase.google.com/project/gen-lang-client-0881186459/firestore)
   - **Important**: In the Firestore console, select the named database from the database dropdown at the top:
     `ai-studio-android-campuslo-0f055f7c-1a01-43af-8486-8c434070a0a1`
   - Browse the `items` collection to see all live listings in real-time.

3. **View Uploaded Photos (Cloud Storage)**:
   - URL: [https://console.firebase.google.com/project/gen-lang-client-0881186459/storage](https://console.firebase.google.com/project/gen-lang-client-0881186459/storage)
   - Bucket: `gen-lang-client-0881186459.firebasestorage.app`
   - Files are stored under:
     `items/{userUid}/{timestamp}.jpg`
   - *Note*: If Storage displays a "Get Started" prompt upon your first visit, click **Get Started** and keep default security rules to activate the storage browser for this bucket.

---

## Features
- **Real-Time Feed**: Instant Firestore observation via `callbackFlow` displaying lost and found items.
- **Search & Category Filters**: Filter by `All`, `Lost`, or `Found`, plus full-text keyword search across titles, locations, and descriptions.
- **Photo Picker & Image Upload**: Select images via Android Photo Picker with upload progress tracking to Firebase Storage.
- **Direct Owner Contact**: Tap "Contact Owner" to launch an email with pre-filled listing subject and details.
- **Zero-Trust Security**: Robust Firestore security rules enforcing authenticated reads and strict poster-only updates and deletions.
- **My Listings Management**: Easily track and remove resolved items.
- **Collegiate Navy & Amber Design**: Distinctive university design following Material 3 guidelines with high-contrast status badges.
