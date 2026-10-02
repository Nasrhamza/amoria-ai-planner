# Amoria AI Planner v1.1.0 - Client Setup

## Package contents

- `SourceCode/`: clean Android Studio project.
- `APK/AmoriaAIPlanner-v1.1.0-demo.apk`: signed reference build.
- `Firebase/firestore.rules`: production security rules.
- `README_CLIENT.md`: this setup guide.

The reference APK was built before the client Firebase configuration was available.
For production, add the client `google-services.json` and rebuild the signed APK.

## Requirements

- Android Studio with Android SDK 35.
- JDK 17 or the Android Studio embedded JDK.
- A Firebase account and a new Firebase project owned by the client.
- Node.js / npm for Firebase CLI.
- LM Studio for local AI suggestions.

## 1. Firebase project

1. Create a Firebase project.
2. Add an Android application with this exact package name:

   `com.example.amoriaiaplanner`

3. Download `google-services.json`.
4. Put it at `SourceCode/app/google-services.json`.
5. In Firebase Authentication, enable:
   - Email/Password
   - Google, if Google sign-in is required
6. Create a Firestore database.
7. Add the release certificate fingerprints to the Firebase Android app:

   SHA-1:
   `83:18:07:58:51:24:DE:DE:7E:BF:C5:8B:4E:46:C9:C5:1A:02:03:9C`

   SHA-256:
   `EA:F4:71:85:42:60:28:4D:1A:BD:7D:64:70:D2:86:D5:79:F6:1A:E5:92:1E:35:5E:C3:18:FA:63:7F:8B:7E:EE`

## 2. Deploy Firestore rules

From `SourceCode`:

```powershell
npx firebase-tools login
npx firebase-tools deploy --only firestore:rules --project YOUR_FIREBASE_PROJECT_ID
```

This deployment is required before testing administrator account creation.

## 3. Configure LM Studio

The AI module uses the OpenAI-compatible LM Studio endpoint configured in:

`app/src/main/java/com/example/amoriaiaplanner/core/config/AiConfig.kt`

Start LM Studio, load:

`qwen/qwen3-4b-2507`

Then start the local server on port `1234`.

- Android Emulator on the same PC:
  `http://10.0.2.2:1234/v1/chat/completions`
- Physical Android device:
  use the PC LAN IP, keep the phone and PC on the same network, for example
  `http://192.168.1.20:1234/v1/chat/completions`

Update `AiConfig.BASE_URL` before the production build.

## 4. Create the first administrator

1. Register a normal account in the application and log in once.
2. In Firebase Console, open Firestore `users/{uid}`.
3. Set:

```text
role: "admin"
isBlocked: false
mustChangePassword: false
```

4. Log out and log in again.

The first administrator can then create other administrator accounts from
`Admin Dashboard > Users > Create user > Admin`.

New administrator accounts receive `mustChangePassword=true` and are forced
to replace the temporary password at first login.

## 5. Release signing

The signing key and credentials are delivered separately from the client ZIP.
Never commit them to source control or send them in a public channel.

Place the private files as follows:

```text
SourceCode/
  keystore.properties
  signing/
    amoria-release.jks
```

To use a new client-owned signing key instead, copy
`keystore.properties.example` to `keystore.properties` and update its values.

## 6. Build

Open `SourceCode` in Android Studio, let Gradle Sync finish, then run:

```powershell
$env:JAVA_HOME="C:\Program Files\Android\Android Studio\jbr"
.\gradlew.bat clean lintRelease assembleRelease
```

The signed production APK is generated at:

`app/build/outputs/apk/release/app-release.apk`

## 7. Administrator acceptance test

1. Log in with the first administrator.
2. Create another account and select the Admin role.
3. Log out.
4. Log in with the new email and temporary password.
5. Confirm that the forced password screen opens.
6. Change the password.
7. Confirm redirection to the real Admin Dashboard.
8. Test user blocking, role changes, room status changes, room deletion and feature controls.

## Security notes

- Passwords are managed by Firebase Authentication and are never stored in Firestore.
- Blocking uses `isBlocked` to reject application access.
- Disabling or deleting another Firebase Authentication identity requires a trusted
  server or Firebase Admin SDK, not the Android client.
- Keep `google-services.json`, signing files and Firebase ownership under the
  client's control for production.
