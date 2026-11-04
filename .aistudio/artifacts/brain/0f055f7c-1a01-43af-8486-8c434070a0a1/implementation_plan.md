# Campus Lost & Found - Android Application

Build a modern, reliable, and secure Android app for university campuses that connects students and staff to report, browse, and recover lost or found belongings in real time.

### User Review & Critical Decisions

> [!IMPORTANT]
> The following architectural decisions were aligned during planning:

- **Confirmed Decision 1 (Firebase Setup)**: Provision backend resources automatically using AI Studio tooling (`show_aistudio_ui` and `ProvisionFirebase` RPC for Firestore Enterprise and Google Auth).
- **Confirmed Decision 2 (Visual Theme)**: Classic Collegiate Navy (`#1E3A8A`) and Amber (`#D97706`) palette with high-contrast status badges (Emerald `#059669` for FOUND, Coral/Crimson `#DC2626` for LOST) and clean M3 card surfaces.
- **Confirmed Decision 3 (Dependency Injection)**: Modern Dependency Injection architecture using Hilt modules (`FirebaseModule`, `RepositoryModule`) with `@HiltViewModel` and constructor injection.
- **Image Storage & Upload Handling**: Uses the Android zero-permission Photo Picker (`PickVisualMedia`) with an image processing pipeline that uploads to Firebase Storage with a real-time progress bar (and base64/data URL backup if Cloud Storage is unprovisioned in the developer's project).

---

### 1. Overview & Core Concept

- **What It Does**: A campus-wide community board where students and faculty can post lost or found items with photos, descriptions, and location markers; browse a real-time feed; filter by status; and contact item owners directly via email.
- **Target Audience**: University students, campus visitors, campus security, and facility staff.
- **Key Value**: Drastically reduces lost-property recovery times by replacing physical bulletin boards and scattered social media posts with a searchable, authenticated, real-time campus repository.

---

### 2. User Experience & Visual Design

#### Key User Flows
1. **Onboarding & Authentication**:
   - `SplashScreen` checks existing Firebase auth state.
   - If unauthenticated, navigates to `LoginScreen` offering both one-tap Google Sign-In (via Credential Manager) and standard student email/password login and registration.
2. **Browsing & Discovery (`FeedScreen`)**:
   - Real-time stream of all campus items with category filter chips (`All`, `Lost`, `Found`) and an instant search bar.
   - Each item displays thumbnail image, title, location pill, lost/found badge, and relative time elapsed.
   - Pull-to-refresh or live snapshot updates.
3. **Posting an Item (`PostItemScreen`)**:
   - Floating Action Button (+) opens the posting wizard.
   - Segmented toggle for `LOST` vs `FOUND`.
   - Fields: Title, Detailed Description, Campus Location (e.g. "Library 2nd Floor", "Student Union").
   - Integrated Photo Picker button with immediate preview and upload progress indicator.
4. **Item Detail & Contact (`ItemDetailScreen`)**:
   - Hero image display, full item metadata, owner name/email, and creation timestamp.
   - Prominent "Contact Owner" action triggering an Android `mailto:` intent with pre-filled subject and reference.
   - Contextual "Delete Post" button visible only to the authenticated owner.
5. **My Posts (`MyPostsScreen`)**:
   - Quick navigation tab/menu item filtering down to items authored by the signed-in student, allowing quick post management and deletion once an item is returned.

#### Visual Identity & Theme
- **Collegiate Navy & Warm Amber**:
  - Primary: `#1E3A8A` (Deep Collegiate Navy)
  - Primary Container: `#DBEAFE`
  - Secondary / Accent: `#D97706` (Warm Athletic Amber)
  - Surface & Background: `#F8FAFC` (Off-white / Warm Canvas)
  - Badges: Crimson `#DC2626` for `LOST`, Emerald `#059669` for `FOUND`.
- **Typography & Hierarchy**: Bold display headers for campus announcements, crisp sans-serif body typography, and prominent status chips.
- **Touch & Accessibility**: Minimum 48dp touch targets, edge-to-edge support with safe window insets, and clear empty-state illustrations with actionable prompts.

---

### 3. Key Product Decisions & Trade-Offs

- **Real-Time Data Layer (`callbackFlow` + Firestore `snapshots`)**:
  - *Chosen Approach*: Expose real-time `Flow<List<Item>>` in `ItemRepository` by bridging Firestore `addSnapshotListener` via `callbackFlow` with lifecycle-aware cleanup (`awaitClose { registration.remove() }`).
  - *Why*: Ensures items reported across campus appear instantly on all active student devices without manual refreshing.
- **Authentication Strategy (Credential Manager + Email/Password)**:
  - *Chosen Approach*: Google Sign-In with Jetpack `CredentialManager` (`GetSignInWithGoogleOption`) alongside Firebase Email/Password auth.
  - *Why*: Provides high security, seamless one-tap login for campus Google accounts, and email verification compatibility.
- **Zero-Trust Security Rules**:
  - *Chosen Approach*: Validate that reads are open to authenticated campus users, writes/updates/deletes are strictly restricted to `request.auth.uid == resource.data.ownerId`, and document payloads strictly conform to field type and string length constraints.
  - *Why*: Prevents tampering with lost-and-found listings or unauthorized deletions.

---

### 4. Technical Architecture & Data Strategy

#### Architecture Diagram

```
┌───────────────────────────────────────────────────────────────┐
│                     Jetpack Compose UI                        │
│  SplashScreen │ LoginScreen │ FeedScreen │ PostScreen │ Detail │
└───────────────────────────────┬───────────────────────────────┘
                                │ StateFlow / Events
┌───────────────────────────────▼───────────────────────────────┐
│                         ViewModels                            │
│  SplashViewModel │ LoginViewModel │ FeedViewModel │ PostViewModel│
└───────────────────────────────┬───────────────────────────────┘
                                │ Coroutines / Flow
┌───────────────────────────────▼───────────────────────────────┐
│                      Repository Layer                         │
│     AuthRepository    │   ItemRepository   │ StorageRepository │
└───────────┬───────────────────────┬───────────────────┬───────┘
            │                       │                   │
┌───────────▼───────────┐ ┌─────────▼─────────┐ ┌───────▼───────┐
│   Firebase Auth /     │ │   Cloud Firestore │ │FirebaseStorage│
│   CredentialManager   │ │  (Realtime Feed)  │ │ (Thumbnails)  │
└───────────────────────┘ └───────────────────┘ └───────────────┘
```

#### Firestore Data Model: `items/{itemId}`
```json
{
  "id": "doc_id_string",
  "ownerId": "auth_user_uid",
  "ownerEmail": "student@university.edu",
  "ownerName": "Alex Rivera",
  "type": "LOST", // or "FOUND"
  "title": "Blue Hydro Flask with stickers",
  "description": "Left near the lecture hall 101 front row.",
  "location": "Science Hall 101",
  "imageUrl": "https://storage.googleapis.com/...",
  "createdAt": "SERVER_TIMESTAMP"
}
```

#### Security Rules Outline (`firestore.rules`)
- `match /items/{itemId}`:
  - Read: `request.auth != null`
  - Create: `request.auth != null && request.resource.data.ownerId == request.auth.uid && isValidItem(request.resource.data)`
  - Update / Delete: `request.auth != null && resource.data.ownerId == request.auth.uid`

#### Verification Plan
1. Local JavaScript rules unit testing via `firestore.test.js` against the local Firestore emulator.
2. Robolectric repository tests for data parsing and authentication enforcement.
3. Android app compilation via `compile_applet`.
