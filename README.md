# CycleGo - Bicycle Booking Android App

A complete, production-ready bicycle rental/booking system built with **Android (Java)** and **Firebase Backend-as-a-Service**.

## Features

### User Features
- ✅ Email/Password authentication via Firebase Auth
- ✅ View nearby bicycle stations on Google Maps
- ✅ Real-time station and bicycle availability
- ✅ Book a bicycle
- ✅ QR code scanner to unlock bicycle
- ✅ Live ride tracking with timer and fare estimation
- ✅ End ride with automatic fare calculation
- ✅ Ride history
- ✅ Profile management

### Admin Features
- ✅ Add/Edit/Delete stations
- ✅ Add/Toggle/Delete bicycles
- ✅ View all active rides in real-time
- ✅ Basic analytics

## Tech Stack

### Android
- **Language**: Java
- **Architecture**: MVVM (Model-View-ViewModel)
- **UI**: XML layouts with ViewBinding
- **Navigation**: Navigation Component with Safe Args
- **Async**: LiveData + ViewModel
- **Image Loading**: Glide
- **QR Scanner**: ZXing (Journey Apps)
- **Maps**: Google Maps SDK
- **Location**: Google Play Services Location

### Firebase
- **Authentication**: Email/Password
- **Database**: Cloud Firestore (NoSQL)
- **Storage**: Firebase Storage (optional)
- **Functions**: Cloud Functions for fare calculation
- **Messaging**: Firebase Cloud Messaging (push notifications)

## Project Structure

```
app/src/main/java/com/sanika/bicyclebookingapp/
├── model/              # Data models (User, Station, Bicycle, Booking, Payment)
├── repository/         # Firebase data access layer
├── viewmodel/          # ViewModels for UI state management
├── ui/
│   ├── auth/          # Login, Signup, ForgotPassword
│   ├── home/          # Map with stations
│   ├── station/       # Station detail + bicycle list
│   ├── booking/       # Booking confirmation
│   ├── qr/            # QR scanner
│   ├── ride/          # Active ride + ride complete
│   ├── history/       # Ride history
│   ├── profile/       # User profile
│   ├── admin/         # Admin panel
│   └── main/          # Main activity container
├── util/              # Utilities (validation, date formatting, Resource wrapper)
└── service/           # Firebase Cloud Messaging service
```

## Setup Instructions

### Prerequisites
1. **Android Studio** (latest version)
2. **Java 11** or higher
3. **Firebase account** ([console.firebase.google.com](https://console.firebase.google.com))
4. **Google Cloud account** (for Maps API)

### Step 1: Clone the Repository
```bash
git clone <your-repo-url>
cd BicycleBookingApp
```

### Step 2: Firebase Setup

#### 2.1 Create Firebase Project
1. Go to [Firebase Console](https://console.firebase.google.com)
2. Click "Add project"
3. Enter project name (e.g., "CycleGo")
4. Follow the wizard to create the project

#### 2.2 Register Android App
1. In Firebase Console → Project Overview → Add app → Android
2. **Android package name**: `com.sanika.bicyclebookingapp`
3. Download `google-services.json`
4. Place it in `app/` folder (replace the placeholder file)

#### 2.3 Enable Firebase Services
1. **Authentication**:
   - Go to Authentication → Sign-in method
   - Enable "Email/Password"

2. **Firestore Database**:
   - Go to Firestore Database → Create database
   - Choose **"Start in test mode"** (for development)
   - Select a region close to you

3. **Cloud Functions** (optional, for fare calculation):
   - Go to Functions → Get started
   - Upgrade to Blaze (pay-as-you-go) plan if needed

4. **Cloud Messaging** (optional, for notifications):
   - Already enabled by default

### Step 3: Google Maps API Key

#### 3.1 Enable Maps SDK
1. Go to [Google Cloud Console](https://console.cloud.google.com)
2. Select your Firebase project (they share the same project)
3. Go to **APIs & Services** → **Library**
4. Search for "Maps SDK for Android" → Enable it

#### 3.2 Create API Key
1. Go to **APIs & Services** → **Credentials**
2. Click **Create Credentials** → **API Key**
3. Copy the key

#### 3.3 Add Key to AndroidManifest.xml
Open `app/src/main/AndroidManifest.xml` and replace:
```xml
<meta-data
    android:name="com.google.android.geo.API_KEY"
    android:value="PASTE_YOUR_KEY_HERE" />
```

### Step 4: Build and Run
1. Open the project in Android Studio
2. Sync Gradle files (File → Sync Project with Gradle Files)
3. Connect an Android device or start an emulator
4. Click **Run** (green play button)

## Firestore Database Schema

### Collections

#### `users`
```
{
  uid: string (auto-generated)
  name: string
  email: string
  phone: string
  profileImageUrl: string (optional)
  isAdmin: boolean
  createdAt: timestamp
}
```

#### `stations`
```
{
  stationId: string (auto-generated)
  name: string
  address: string
  location: GeoPoint {latitude, longitude}
  totalBicycles: number
  availableBicycles: number
}
```

#### `bicycles`
```
{
  bicycleId: string (auto-generated)
  stationId: string (reference to stations)
  stationName: string
  available: boolean
  qrCode: string
  type: string (e.g., "Standard", "Electric")
  imageUrl: string (optional)
}
```

#### `bookings`
```
{
  bookingId: string (auto-generated)
  userId: string (reference to users)
  bicycleId: string (reference to bicycles)
  stationId: string (reference to stations)
  stationName: string
  status: string ("PENDING", "ACTIVE", "COMPLETED", "CANCELLED")
  createdAt: timestamp
  startTime: timestamp
  endTime: timestamp
  durationMinutes: number
  fare: number
}
```

#### `payments`
```
{
  paymentId: string (auto-generated)
  bookingId: string (reference to bookings)
  userId: string (reference to users)
  amount: number
  status: string ("PENDING", "SUCCESS", "FAILED")
  method: string (e.g., "SIMULATED", "RAZORPAY")
  paidAt: timestamp
}
```

## Firestore Security Rules

Deploy the rules from `firestore.rules`:
```bash
firebase deploy --only firestore:rules
```

Key rules:
- Users can only read/write their own data
- Admins have full access (check `isAdmin` field)
- Bookings are protected by user ownership
- Stations and bicycles are read-only for users, write-only for admins

## Firebase Cloud Functions (Optional)

The app includes a local fallback for fare calculation, so Cloud Functions are optional.

### Deploy Functions
```bash
cd functions
npm install
cd ..
firebase deploy --only functions
```

### Functions Included
- `calculateFare`: Calculates ride fare based on duration
- `sendBookingNotification`: Sends push notification on booking creation

## Testing the App

### Create Test Data

#### Add a Station (Admin)
1. Run the app
2. Sign up with a new account
3. In Firestore Console, manually set `isAdmin: true` for your user
4. Restart the app → Profile → Admin Panel
5. Add a station with coordinates (e.g., 28.6139, 77.2090 for Delhi)

#### Add Bicycles
1. In Admin Panel → Manage Bicycles
2. Add bicycles to the station

#### Book a Ride (User)
1. Logout from admin account
2. Sign up as a regular user
3. View stations on map
4. Select a station → Book a bicycle
5. Scan QR code (the QR code = bicycleId, so scan any QR with the bicycle ID)
6. Start ride → End ride

## Known Limitations

1. **QR Code**: For simplicity, the QR code is just the `bicycleId`. In production, use a secure token.
2. **Payment**: Currently simulated. Integrate Razorpay or Stripe for real payments.
3. **Bicycle Location**: Bicycles are tied to stations. In production, track GPS location of each bicycle.
4. **Offline Support**: No offline caching. Add Room database for offline-first architecture.

## Troubleshooting

### Build Errors
- **"google-services.json not found"**: Make sure you downloaded it from Firebase Console and placed it in `app/` folder
- **"Manifest merger failed"**: Check that your `applicationId` matches the Firebase package name
- **Safe Args errors**: Clean and rebuild (Build → Clean Project → Rebuild Project)

### Runtime Errors
- **"PERMISSION_DENIED" on Firestore**: Deploy security rules or use test mode
- **Map not showing**: Check that Maps API key is correct and Maps SDK is enabled
- **QR scanner crashes**: Grant camera permission in app settings

### Firestore Index Errors
When you see "The query requires an index" error:
1. Click the link in the error message (Logcat)
2. It opens Firebase Console with the index pre-filled
3. Click "Create Index"
4. Wait 2-5 minutes for the index to build

## License

This project is for educational purposes. Feel free to use and modify.

## Credits

Built with ❤️ using Android, Java, and Firebase.
